package org.example.springboot.chat;

import lombok.extern.slf4j.Slf4j;
import org.example.springboot.emuns.ModelProvider;
import org.example.springboot.framework.ChatRequest;
import org.example.springboot.model.ModelTarget;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class SiliconFlowChatClient extends  AbstractOpenAIStyleChatClient {
    @Override
    public String provider() {
        return ModelProvider.SILICONFLOW.getId();
    }

    @Override
    public String chat(ChatRequest request, ModelTarget target) {
        return doChat(request,target);
    }

    @Override
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback, ModelTarget target) {
        return doStreamChat(request,callback,target);
    }
}
