import { OpenAiCompatibleProvider } from "./openai-compatible.js";
import type { LlmProviderConfig } from "./types.js";

// 工厂固定返回 OpenAI 兼容实现，供应商差异全部由 baseURL 和配置承担。
// 调用方只依赖 LlmProvider 契约，后续替换实现不会扩散到业务模块。
export function createLlmProvider(
  config: LlmProviderConfig,
): OpenAiCompatibleProvider {
  return new OpenAiCompatibleProvider(config);
}

export * from "./types.js";
