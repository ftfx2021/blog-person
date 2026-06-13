package org.example.springboot.embedding;

import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import org.example.springboot.emuns.ModelProvider;
import org.example.springboot.model.ModelTarget;

public class OllamaEmbeddingClient extends AbstractOpenAIStyleEmbeddingClient
{
    public OllamaEmbeddingClient(OkHttpClient httpClient)
    {
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
