package org.example.springboot.chat;

import lombok.extern.slf4j.Slf4j;
import org.example.springboot.emuns.ModelCapability;
import org.example.springboot.enums.BaseErrorCode;
import org.example.springboot.exception.RemoteException;
import org.example.springboot.framework.ChatRequest;
import org.example.springboot.model.ModelHealthStore;
import org.example.springboot.model.ModelRoutingExecutor;
import org.example.springboot.model.ModelSelector;
import org.example.springboot.model.ModelTarget;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoutingLLMService implements LLMService {
    private static final int FIRST_PACKET_TIMEOUT_SECONDS = 60;
    private static final String STREAM_INTERRUPTED_MESSAGE = "流式请求被中断";
    private static final String STREAM_NO_PROVIDER_MESSAGE = "无可用大模型提供者";
    private static final String STREAM_START_FAILED_MESSAGE = "流式请求启动失败";
    private static final String STREAM_TIMEOUT_MESSAGE = "流式首包超时";
    private static final String STREAM_NO_CONTENT_MESSAGE = "流式请求未返回内容";
    private static final String STREAM_ALL_FAILED_MESSAGE = "大模型调用失败，请稍后再试...";
    private final ModelRoutingExecutor executor;
    private final ModelSelector selector;
    private final Map<String, ChatClient> clientsByProvider;
    private final ModelHealthStore healthStore;

    public RoutingLLMService(ModelRoutingExecutor executor, ModelSelector selector, List<ChatClient> clients, ModelHealthStore healthStore) {
        this.executor = executor;
        this.selector = selector;
        this.clientsByProvider = clients.stream()
                .collect(
                        Collectors.toMap(ChatClient::provider,//拿到当前对象的provider，把provider当成value
                                Function.identity())); //把自己当成value
        
        this.healthStore = healthStore;
    }

    @Override
    public String chat(ChatRequest request) {
        return executor.executeWithFallback(
                ModelCapability.CHAT,
                selector.selectChatModel(Boolean.TRUE.equals(request.getThinking())),
                this::resolveClient,
                (client, target) -> client.chat(request, target)


        );
    }

    /**
     * 流式chat不走executeWithFallback
     * @param request 请求
     * @param callback 回调
     * @return
     */
    @Override
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback) {
        //找到完整的可用模型信息
        List<ModelTarget> modelTargets = selector.selectChatModel(Boolean.TRUE.equals(request.getThinking()));
        if (modelTargets.isEmpty()) {
            throw new RuntimeException(STREAM_NO_PROVIDER_MESSAGE);
        }
        String displayName = ModelCapability.CHAT.getDisplayName();
        Throwable lastError = null;

        //尝试每个可用调用模型目标
        for (ModelTarget modelTarget : modelTargets) {
            //
            ChatClient chatClient = resolveClient(modelTarget);
            if (chatClient == null) {
                continue;
            }
            if(!healthStore.allowCall(modelTarget.id())){
                continue;
            }


            FirstPacketAwaiter awaiter = new FirstPacketAwaiter();
            ProbeBufferingCallback wrapper = new ProbeBufferingCallback(callback, awaiter);
            StreamCancellationHandle handle;
            try{
                //发起调用（立即返回，不阻塞）
                handle = chatClient.streamChat(request, wrapper, modelTarget);
                
            }catch(Exception e){
                healthStore.markFailure(modelTarget.id());
                lastError = e;
                log.info("{}流式请求失败，切换下一个模型。modelId:{},provider:{}",displayName,modelTarget.id(),modelTarget.provider());
                continue;
            }
            //等待首包（阻塞）
            FirstPacketAwaiter.Result result = awaitFirstPacket(awaiter, handle, callback);
            if(result.isSuccess()){
                wrapper.commit();
                healthStore.markSuccess(modelTarget.id());
                return handle;
            }
            healthStore.markFailure(modelTarget.id());
            handle.cancel();

            lastError = buildLastErrorAndLog(result,modelTarget,displayName);


        }
        //所有模型均尝试失败，通知客户端错误
        throw notifyAllFailed(callback,lastError);
    }

    /**
     * 解析、提取客户端
     * @param target 模型目标
     * @return 客户端信息
     */
    private ChatClient resolveClient(ModelTarget target) {
        ChatClient client = clientsByProvider.get(target.candidate().getProvider());
        if (client == null) {
            log.warn("{} 提供商客户端缺失: provider：{}，modelId：{}",
                    ModelCapability.CHAT.getDisplayName(), target.candidate().getProvider(), target.id());
        }
        return client;
    }

    /**
     * awaitFirstPacket()
     *     │
     *     ├── 正常情况
     *     │   ├── await() 超时        → return TIMEOUT
     *     │   ├── await() 收到首包    → return SUCCESS
     *     │   └── await() 无内容结束  → return NO_CONTENT
     *     │
     *     └── 被中断 (InterruptedException)
     *         ├── Thread.interrupt()      恢复中断标志
     *         ├── handle.cancel()         取消网络请求
     *         ├── callback.onError()      通知下游
     *         └── throw exception         抛出异常
     */
    private FirstPacketAwaiter.Result awaitFirstPacket(FirstPacketAwaiter awaiter,
                                                       StreamCancellationHandle handle,
                                                       StreamCallback callback) {
        try {
            //正常等待
           return awaiter.await(FIRST_PACKET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) { //异常处理（被中断）
            //什么时候会触发？ 其他线程调用了 Thread.interrupt() 中断了主线程：// 比如服务器要关闭，管理器中断了所有线程
            // ① 恢复中断标志，为什么要手动恢复？Java 的设计规定：catch(InterruptedException) 会清除中断标志。
            // 恢复标志是为了让上层调用者也能感知到"这个线程被中断过"。
            //// ✅ 标准写法
            //try {
            //    someBlockingMethod();  // 可能抛 InterruptedException
            //} catch (InterruptedException e) {
            //    Thread.currentThread().interrupt();  // ← 恢复中断标志
            //    // ... 清理资源
            //}
            Thread.currentThread().interrupt();
            //不取消的话：
            //  - 网络连接泄漏
            //  - 线程泄漏
            //  - 资源浪费
            handle.cancel();
            //通知下游：出错了
            //不通知下游会怎样？
            //下游在傻等数据，永远收不到任何消息（既没有数据，也没有错误通知）
            RemoteException interruptedException = new RemoteException(STREAM_INTERRUPTED_MESSAGE, e, BaseErrorCode.REMOTE_ERROR);
            callback.onError(interruptedException);
            //// ④ 向上抛出异常
            throw  interruptedException;
        }

    }

    private Throwable buildLastErrorAndLog(FirstPacketAwaiter.Result result, ModelTarget target, String label) {
        switch (result.getType()) {
            case ERROR -> {
                Throwable error = result.getError() != null
                        ? result.getError()
                        : new RemoteException("流式请求失败", BaseErrorCode.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求失败，切换下一个模型",
                        label, target.id(), target.candidate().getProvider(), error);
                return error;
            }
            case TIMEOUT -> {
                RemoteException timeout = new RemoteException(STREAM_TIMEOUT_MESSAGE, BaseErrorCode.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求超时，切换下一个模型",
                        label, target.id(), target.candidate().getProvider());
                return timeout;
            }
            case NO_CONTENT -> {
                RemoteException noContent = new RemoteException(STREAM_NO_CONTENT_MESSAGE, BaseErrorCode.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求无内容完成，切换下一个模型",
                        label, target.id(), target.candidate().getProvider());
                return noContent;
            }
            default -> {
                RemoteException unknown = new RemoteException("流式请求失败", BaseErrorCode.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求失败（未知类型），切换下一个模型",
                        label, target.id(), target.candidate().getProvider());
                return unknown;
            }
        }
    }

    private RemoteException notifyAllFailed(StreamCallback callback, Throwable lastError) {
        //，throw RemoteException 让调用链上层知道失败了（比如 Controller 层的全局异常处理器）
        RemoteException finalException = new RemoteException(
                STREAM_ALL_FAILED_MESSAGE,
                lastError,
                BaseErrorCode.REMOTE_ERROR
        );
        //callback.onError() 让前端知道失败了
        callback.onError(finalException);
        return finalException;
    }


}

