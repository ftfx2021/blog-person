package org.example.springboot.chat;

import lombok.NoArgsConstructor;
import okhttp3.Call;
import org.example.springboot.dto.StreamCallback;
import org.example.springboot.dto.StreamCancellationHandle;
import org.example.springboot.dto.StreamCancellationHandles;
import org.example.springboot.http.ModelClientErrorType;
import org.example.springboot.http.ModelClientException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 异步提交,一个无状态的工具类，只有一个 submit 方法，职责单一：把阻塞式的流式读取任务提交到专用线程池异步执行，并构建取消句柄。
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
//
public final class StreamAsyncExecutor {
    /**
     * 如果线程池满了（所有线程都在处理其他流式请求），CompletableFuture.runAsync 会抛出 RejectedExecutionException。这时的降级逻辑：
     * 1.call.cancel()——取消 OkHttp 请求，释放底层连接。虽然 call.execute() 还没被调用（任务没提交成功），但 cancel 一个未执行的 Call 是安全的，确保不会有悬挂的连接
     * 2. callback.onError(new ModelClientException(...))——通过回调通知调用方线程池繁忙。
     * 注意这里不是抛异常，而是调 callback.onError()——因为 doStreamChat 的返回类型是 StreamCancellationHandle，回调模式下错误通过 onError 传递，不通过异常
     * 3.return StreamCancellationHandles.noop()——返回一个空操作的取消句柄。因为流式任务实际没有启动，没有什么可取消的，noop 句柄的 cancel() 什么都不做
     */
    private static final String STREAM_BUSY_MESSAGE = "流式线程池繁忙";

    /**
     *  把阻塞式的流式读取任务提交到专用线程池异步执行，并构建取消句柄
     * @param executor 专用流式线程池,“线程池”的管理者
     * @param call OkHttp 的 Call 对象
     * @param callback 流式回调，线程池拒绝时用于通知错误
     * @param streamTask 实际的流式读取逻辑（就是 doStream 方法）
     */
    static StreamCancellationHandle submit(
            Executor executor,
            Call call,
            StreamCallback callback,
            Consumer<AtomicBoolean> streamTask

    ){
        //创建取消信号
        AtomicBoolean cancelled = new AtomicBoolean(false);
        try {
            //提交一部分
            //CompletableFuture.runAsync：异步启动器
            //它的标准写法是：
            //CompletableFuture.runAsync(任务逻辑, 线程池);runAsync：意思是“去后台跑吧，别管我，我（主线程）要继续往下走”。
            //注意这里的try-catch没法捕获streamTask（异步线程）的异常，它只能捕获和它同步执行的异常（如分配线程去后台干活，捕获线程池满的异常）
            CompletableFuture.runAsync(() -> streamTask.accept(cancelled), executor);
        }catch (RejectedExecutionException e){
            //如果线程池满了（所有线程都在处理其他流式请求）。CompletableFuture.runAsync 会抛出 RejectedExecutionException。
            //构建取消句柄并返回
            call.cancel();
            callback.onError(
                    new ModelClientException(
                            STREAM_BUSY_MESSAGE, ModelClientErrorType.SERVER_ERROR,null,e
                    )
            );
            return StreamCancellationHandles.noop();
        }
        return StreamCancellationHandles.fromOkHttp(call, cancelled);
    }
}
