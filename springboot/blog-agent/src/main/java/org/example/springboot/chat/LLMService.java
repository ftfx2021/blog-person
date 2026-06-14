package org.example.springboot.chat;

import org.example.springboot.framework.ChatMessage;
import org.example.springboot.framework.ChatRequest;
import org.springframework.stereotype.Service;

import java.util.List;



/**
 * llm对话接口
 *  LLMService 支持同步调用 chat() 和流式调用 streamChat()。流式调用返回一个 StreamCancellationHandle，
 * 业务层可以随时通过 handle.cancel() 取消正在进行的生成。
 */

public interface LLMService {
    //用 default 定义多个容易调用的便捷接口（高层抽象），然后在内部将其转化为结构化的请求
    default String chat(String prompt) {
        ChatRequest req = ChatRequest.builder()
                .messages(List.of(ChatMessage.user(prompt)))
                .build();
        return chat(req);
    }

    String chat(ChatRequest request);

    default String chat(ChatRequest request, String modelId) {
        return chat(request);
    }

    default StreamCancellationHandle streamChat(String prompt, StreamCallback callback) {
        ChatRequest req = ChatRequest.builder()
                .messages(List.of(ChatMessage.user(prompt)))
                .build();
        return streamChat(req, callback);
    }

    StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback);

}
