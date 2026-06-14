package org.example.springboot.embedding;

import okhttp3.OkHttpClient;
import org.example.springboot.emuns.ModelProvider;
import org.springframework.stereotype.Service;

@Service
public class SiliconFlowEmbeddingClient extends AbstractOpenAIStyleEmbeddingClient{
    protected SiliconFlowEmbeddingClient(OkHttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public String provider() {
        return ModelProvider.SILICONFLOW.getId();
    }

    @Override
    /**
     * SiliconFlow 的 Embedding API 对单次请求的 input 数量有上限，最多 32 条。
     */
    protected int maxBatchSize() {
        return 32;
    }
}
