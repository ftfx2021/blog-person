package org.example.springboot.chat;

import cn.hutool.core.collection.CollUtil;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import okio.BufferedSource;
import org.example.springboot.config.AIModelProperties;
import org.example.springboot.framework.ChatMessage;
import org.example.springboot.framework.ChatRequest;
import org.example.springboot.emuns.ModelCapability;

import org.example.springboot.http.*;
import org.example.springboot.model.ModelTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 模板方法
 * 如果每个供应商都从头实现一遍 HTTP 请求构建、JSON 序列化、响应解析、错误处理，90% 的代码是重复的。
 * openai请求规范 <a href="https://openai.apifox.cn/doc-2187082">...</a> （Chat Completions API）
 */
@Slf4j
@Component
public abstract class   AbstractOpenAIStyleChatClient implements ChatClient {

    @Autowired
    private   OkHttpClient syncHttpClient;
    @Autowired
    private   OkHttpClient streamHttpClient;
    @Autowired
    private    Executor modelStreamExecutor;

    protected final Gson gson  = new Gson();


    /**
     * 控制是否校验和携带 API Key。默认返回 true
     * Ollama 是本地部署的推理服务，没有云端认证机制，所以 OllamaChatClient 覆写为 false
     * 覆写后，doChat 会跳过 API Key 校验,newAuthorizedRequest 也不会添加 Authorization 头
     * @return 否校验和携带 API Key
     */
    protected boolean requiresApiKey(){
        return true;
    }

    /**
     * 向请求体添加供应商特有字段
     * 如果某个供应商有自己独特的字段（比如特殊的采样参数），子类可以覆写这个方法添加
     * @param body 请求体
     * @param request 发送的请求
     */
    protected void customizeRequestBody(JsonObject body, ChatRequest request){
        //把确定非空的对象放在 equals() 左边，避免 NullPointerException。
        if(Boolean.TRUE.equals(request.getThinking())){
            body.addProperty("enable_thinking",request.getThinking());
        }
    }

    /**
     * 流式调用时是否解析 reasoning_content 字段
     * @param request 发送的请求
     * @return 是否解析 reasoning_content 字段
     */
    protected boolean isReasoningEnabledForStream(ChatRequest request){
        return Boolean.TRUE.equals(request.getThinking());
    }

    /**
     *  流式聊天入口
     * @param request 请求
     * @param callback
     * @param target
     * @return
     */
    protected StreamCancellationHandle doStreamChat(ChatRequest request, StreamCallback callback, ModelTarget target){
        // 确保 ModelTarget 里有供应商配置
        AIModelProperties.ProviderConfig provider = HttpResponseHelper.requireProvider(target, provider());
        if (requiresApiKey()) {
            HttpResponseHelper.requireApiKey(provider, provider());
        }
        //封装请求和请求头
        JsonObject reqBody = buildRequestBody(request, target, true);
        Request streamRequest  = newAuthorizedRequest(provider, target)
                .post(RequestBody.create(reqBody.toString(),HttpMediaTypes.JSON))
                .addHeader("Accept","text/event-stream")
                .build();
        //发起请求(准备执行的请求任务对象)
        Call call = streamHttpClient.newCall(streamRequest);
        boolean reasoningEnabled = isReasoningEnabledForStream(request);
        //流式调用把 Call 对象交给异步执行器，自己立即返回一个取消句柄(异步执行)
        return StreamAsyncExecutor.submit(
                modelStreamExecutor,
                call,
                callback,
                new Consumer<AtomicBoolean>() {
                    @Override
                    public void accept(AtomicBoolean cancelled) {
                        doStream(call,callback,cancelled,reasoningEnabled);
                    }
                }

        );
//        cancelled->doStream(call,callback,cancelled,reasoningEnabled)
    }

    private void doStream(Call call, StreamCallback callback,
                          AtomicBoolean cancelled, boolean reasoningEnabled){
        //客户端向服务端发起连接，并发送请求头。
        try(Response response = call.execute()){
            if(!response.isSuccessful()){
                String body = HttpResponseHelper.readBody(response.body());
                throw new ModelClientException(
                        provider() + " 流式请求失败: HTTP " + response.code() + " - " + body,
                        ModelClientErrorType.fromHttpStatus(response.code()),
                        response.code()
                );
            }
            ResponseBody body = response.body();
            if(body == null){
                throw new ModelClientException(
                        provider() + " 流式响应为空",
                        ModelClientErrorType.INVALID_RESPONSE, null);
            }
            //获取BufferedSource ：response.body().source() 拿到 OkHttp 的缓冲数据源，后续通过它逐行读取 SSE 数据。

            //专门为“流”而生（实时性）； 极其简单的“按行读取”（SSE 协议匹配）；性能更高，内存占用更小；完美处理“网络碎片”
            //这一步的唯一目的，是让你的代码拿到了控制这个缓冲区的句柄（指针）。此时，网络底层的数据接收工作已经托管给了 OkHttp 的后台线程，它只要从网络收到字节（Chunk），就会默默往这个 source 缓冲区里丢。
            BufferedSource source = body.source();
            boolean completed = false;
            while (!cancelled.get()) {

                String line = source.readUtf8Line();
                if(line == null){
                    break;
                }
                if(line.isBlank()){
                    continue;
                }
                try{
                    //解析流式输出的每一行
                    OpenAIStyleSseParser.ParsedEvent event = OpenAIStyleSseParser.parseLine(line,gson,reasoningEnabled);
                    if(event.hasReasoning()){
                        callback.onThinking(event.reasoning());
                    }
                    if(event.hasContent()){
                        callback.onContent(event.content());
                    }
                    if(event.completed()){
                        callback.onComplete();
                        completed = true;
                        break;
                    }
                }catch (Exception parseEx) {
                    log.warn("{} 流式响应解析失败: line={}", provider(), line, parseEx);
                }

            }
            if(cancelled.get()){
                log.info("{}流式响应结束",provider());
                return;
            }
            if(!completed){
                throw new ModelClientException(
                        provider() + " 流式响应异常结束",
                        ModelClientErrorType.INVALID_RESPONSE, null);
            }
        }catch (Exception e) {
            if (!cancelled.get()) {
                callback.onError(e);
            } else {
                log.info("{} 流式响应取消期间产生异常（可忽略）: {}", provider(), e.getMessage());
            }
        }
    }

    /**
     * 同步调用的核心，定义了从校验到返回结果的完整骨架
     * @param request 发送的请求
     * @param target 模型信息
     * @return 响应结果
     */
    protected String doChat(ChatRequest request, ModelTarget target)  {
        // 确保 ModelTarget 里有供应商配置
        AIModelProperties.ProviderConfig provider = HttpResponseHelper.requireProvider(target, provider());
        //如果 requiresApiKey() 为 true，检查供应商配置中的 apiKey 字段非空。Ollama 跳过这步。
        if(requiresApiKey()){
            HttpResponseHelper.requireApiKey(provider,provider());
        }
        //buildRequestBody 把 ChatRequest 转换为 OpenAI 格式的 JSON
        JsonObject requestBody = buildRequestBody(request, target, false);
        //构建 HTTP 请求ewAuthorizedRequest 解析 URL 并添加 Authorization 头，然后设置 POST body。
        Request requestHttp = newAuthorizedRequest(provider, target)
                .post(RequestBody.create(requestBody.toString(), HttpMediaTypes.JSON))
                .build();


        JsonObject respJson;
        //httpClient.newCall(requestHttp).execute() 同步发送 HTTP 请求。用 try-with-resources 确保响应体关闭
        try(Response response = syncHttpClient.newCall(requestHttp).execute()){
            if(!response.isSuccessful()){
                String body = HttpResponseHelper.readBody(response.body());
                log.info("{} 同步请求失败：status={}, body={}, ",provider(),response.code(),   body );
                throw new ModelClientException(
                        provider()+" 同步请求失败：HTTP "+ response.code(),
                        ModelClientErrorType.fromHttpStatus(response.code()),
                        response.code()
                );
            }
            //parseJson 把响应体解析为 JsonObject。
            respJson = HttpResponseHelper.parseJson(response.body(), provider());
        } catch (IOException e) {
            throw new ModelClientException(
                    provider()+" 同步请求失败：HTTP "+ e.getMessage(),
                    ModelClientErrorType.NETWORK_ERROR,null,e
            );
        }
        //extractChatContent 从 choices[0].message.content 提取模型回答。
        return extractChatContent(respJson);

    }

    /**
     * 解析响应
     * 1 {
     * 2    "id":"chatcmpl-abc123",
     * 3    "object":"chat.completion",
     * 4    "created":1677858242,
     * 5    "model":"gpt-3.5-turbo-0301",
     * 6    "usage":{
     * 7       "prompt_tokens":13,
     * 8       "completion_tokens":7,
     * 9       "total_tokens":20
     * 10    },
     * 11    "choices":[
     * 12       {
     * 13          "message":{
     * 14             "role":"assistant",
     * 15             "content":"\n\nThis is a test!"
     * 16          },
     * 17          "finish_reason":"stop",
     * 18          "index":0
     * 19       }
     * 20    ]
     * 21 }
     * @param responseJSON
     * @return
     */
    private String extractChatContent(JsonObject responseJSON){
        if(responseJSON==null || !responseJSON.has("choices")){
            throw new ModelClientException(
                    provider() + " 响应缺少 choices", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        JsonArray choices = responseJSON.getAsJsonArray("choices");
        if(choices==null || choices.isEmpty()){
            throw new ModelClientException(
                    provider() + " 响应 choices 为空", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        JsonObject choice0 = choices.get(0).getAsJsonObject();
        if(choice0==null||!choice0.has("message")){
            throw new ModelClientException(
                    provider() + " 响应缺少 message", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        JsonObject message = choice0.getAsJsonObject("message");
        if(message==null||!message.has("content")||message.get("content").isJsonNull()){
            throw new ModelClientException(
                    provider() + " 响应缺少 content", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        return message.get("content").getAsString();

    }

    /**
     *  请求体构建
     * @param request 聊天请求
     * @param target 模型信息
     * @param stream 是否流式
     * @return 完整的请求体
     */
    protected JsonObject buildRequestBody(ChatRequest request, ModelTarget target,Boolean stream){
        JsonObject body = new JsonObject();
        body.addProperty("model",HttpResponseHelper.requireModel(target,provider()));
        if(stream){
            body.addProperty("stream",true);
        }
        //JsonObject 的 add 和 addProperty 的主要区别在于你存入的数据类型：是简单的“基本值”，还是复杂的“JSON 结构”。
        body.add("messages",buildMessages(request));

        if(request.getTemperature() != null){
            body.addProperty("temperature",request.getTemperature());
        }
        if(request.getTopK() != null){
            body.addProperty("top_k",request.getTopK());
        }
        if(request.getTopP()!=null){
            body.addProperty("top_p",request.getTopP());
        }
        if(request.getMaxTokens() != null){
            body.addProperty("max_tokens",request.getMaxTokens());
        }
        customizeRequestBody(body,request);
        return body;
    }

    /**
     * URL 构建请求信息的url和请求头
     * @param provider 供应商
     * @param target 模型信息
     * @return 请求Builder
     */
    protected Request.Builder newAuthorizedRequest(AIModelProperties.ProviderConfig provider, ModelTarget target){
        Request.Builder builder = new Request.Builder()
                .url(ModelUrlResolver.resolveUrl(provider, target.candidate(), ModelCapability.CHAT)); //构建模型URL
        if(requiresApiKey()){
            //构建请求头
            builder.addHeader("Authorization", "Bearer " + provider.getApiKey());
        }
        return builder;
    }

    /**
     * 构建请求message
     * @param request 请求
     * @return
     */
    private JsonArray buildMessages(ChatRequest request){
        JsonArray arr = new JsonArray();
        List<ChatMessage> messages = request.getMessages();
        if(CollUtil.isNotEmpty(messages)){
            messages.forEach(message -> {
                JsonObject msg = new JsonObject();
                msg.addProperty("role",toOpenAiRole(message.getRole()));
                msg.addProperty("content",message.getContent());
                arr.add(msg);
            });
        }
        return arr;
    }

    /**
     * 兼容OPEN_AI
     * @param role 原始的角色编码
     * @return open ai型的
     */
    private String toOpenAiRole(ChatMessage.Role role) {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
        };
    }
}
