package org.example.springboot.embedding;

import okhttp3.OkHttpClient;
import org.example.springboot.emuns.ModelProvider;
import org.springframework.stereotype.Service;

@Service
public class BaiLianEmbeddingClient extends AbstractOpenAIStyleEmbeddingClient {
    protected BaiLianEmbeddingClient(OkHttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public String provider() {
        return ModelProvider.BAI_LIAN.getId();
    }
}
