package org.example.springboot.client;



// Rerank 供应商接口
public interface RerankClient {
   // 返回供应商标识（比如 "bailian"、"siliconflow"、"ollama"）
    String provider();
    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN, ModelTarget target);
}
