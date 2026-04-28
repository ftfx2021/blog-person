package org.example.springboot.client;

import org.example.springboot.dto.ChatRequest;
import org.example.springboot.dto.StreamCallback;
import org.example.springboot.dto.StreamCancellationHandle;
import org.example.springboot.model.ModelTarget;

public interface ChatClient {
    // 返回供应商标识（比如 "bailian"、"siliconflow"、"ollama"）
    String provider();
    String chat(ChatRequest request, ModelTarget target);
    StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback, ModelTarget target);
}
