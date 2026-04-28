package org.example.springboot.chat;

import org.example.springboot.dto.StreamCallback;
import org.example.springboot.dto.StreamCancellationHandle;
import org.example.springboot.dto.ChatMessage;
import org.example.springboot.dto.ChatRequest;

import java.util.List;

//llm对话接口
//LLMService 支持同步调用 chat() 和流式调用 streamChat()。流式调用返回一个 StreamCancellationHandle，
// 业务层可以随时通过 handle.cancel() 取消正在进行的生成。
public interface LLMService {

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
