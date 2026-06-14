package org.example.springboot.chat;

import org.example.springboot.framework.ChatRequest;
import org.example.springboot.model.ModelTarget;

/**
 * 供应商级别的 ChatClient
 */
public interface ChatClient {
    /**
     * @return 供应商标识
     */
    String provider();

    /**
     *
     * @param request 请求
     * @param target 一次请求所需要的完整模型信息
     * @return 模型返回的文本
     */
    String chat(ChatRequest request, ModelTarget target);

    StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback, ModelTarget target);
}
