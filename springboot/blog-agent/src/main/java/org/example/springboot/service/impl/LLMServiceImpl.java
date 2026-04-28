package org.example.springboot.service.impl;

import org.example.springboot.dto.ChatRequest;
import org.example.springboot.chat.LLMService;
import org.example.springboot.dto.StreamCallback;
import org.example.springboot.dto.StreamCancellationHandle;

public class LLMServiceImpl implements LLMService {
    @Override
    public String chat(ChatRequest request) {
        return "";
    }

    @Override
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback) {
        return null;
    }
}
