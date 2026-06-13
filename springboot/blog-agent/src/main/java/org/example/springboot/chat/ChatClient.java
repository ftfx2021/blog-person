package org.example.springboot.chat;

import org.example.springboot.framework.ChatRequest;
import org.example.springboot.dto.StreamCallback;
import org.example.springboot.dto.StreamCancellationHandle;
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
     * @param request
     * @param target
     * @return
     */
    String chat(ChatRequest request, ModelTarget target);
    StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback, ModelTarget target);
}
