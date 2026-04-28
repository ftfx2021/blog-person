package org.example.springboot.client;

import java.util.List;

public interface EmbeddingClient {
    // 返回供应商标识（比如 "bailian"、"siliconflow"、"ollama"）
    String provider();
    List<Float> embed(String text, ModelTarget target);
    List<List<Float>> embedBatch(List<String> texts, ModelTarget target);
}
