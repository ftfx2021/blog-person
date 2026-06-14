package org.example.springboot.embedding;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.example.springboot.emuns.ModelProvider;
import org.example.springboot.model.ModelTarget;
import org.springframework.stereotype.Service;

@Slf4j
@Service

public class OllamaEmbeddingClient extends AbstractOpenAIStyleEmbeddingClient
{


    protected OllamaEmbeddingClient(OkHttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public String provider() {
        return ModelProvider.OLLAMA.getId();
    }

    @Override
    protected void customizeRequestBody(JsonObject body, ModelTarget target) {

    }

    @Override
    protected boolean requiresApiKey() {
        return false;
    }
}
