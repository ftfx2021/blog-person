package org.example.springboot.chat;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.tika.parser.mp3.MP3Frame;
import org.example.springboot.config.AIModelProperties;
import org.example.springboot.dto.StreamCallback;
import org.example.springboot.dto.StreamCancellationHandle;
import org.example.springboot.emuns.ModelCapability;
import org.example.springboot.emuns.ModelProvider;
import org.example.springboot.framework.ChatRequest;
import org.example.springboot.model.ModelRoutingExecutor;
import org.example.springboot.model.ModelTarget;

import java.util.concurrent.Executor;
@Slf4j
public class OllamaChatClient extends AbstractOpenAIStyleChatClient{


    protected OllamaChatClient(OkHttpClient httpClient, Executor modelStreamExecutor) {
        super(httpClient, modelStreamExecutor);
    }

    @Override
    public String provider() {
        return ModelProvider.OLLAMA.getId();
    }

    @Override
    public String chat(ChatRequest request, ModelTarget target) {
        return doChat(request, target);
    }

    @Override
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback, ModelTarget target) {
        return doStreamChat(request, callback, target);
    }
}
