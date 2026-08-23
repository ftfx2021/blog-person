export interface EmbeddingProvider {
  // 批量把文本转换为向量，并返回模型名与向量维度供索引层建表。
  embed(
    texts: string[],
  ): Promise<{ vectors: number[][]; model: string; dim: number }>;
  // 通过单条 ping 文本验证 Ollama 服务和当前 embedding 模型。
  healthCheck(): Promise<{ ok: boolean; detail: string; dim?: number }>;
}

export interface EmbeddingProviderConfig {
  baseURL: string;
  model: string;
}
