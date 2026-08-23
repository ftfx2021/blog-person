import { OllamaEmbeddingProvider } from "./ollama.js";
import type { EmbeddingProviderConfig } from "./types.js";

// 工厂统一创建 Ollama provider，供设置服务和后续索引 worker 复用。
// 工厂不缓存实例，确保用户修改地址或模型后下一次测试使用最新配置。
export function createEmbeddingProvider(
  config: EmbeddingProviderConfig,
): OllamaEmbeddingProvider {
  return new OllamaEmbeddingProvider(config);
}

export * from "./types.js";
