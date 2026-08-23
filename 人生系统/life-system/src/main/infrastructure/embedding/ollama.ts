import type { EmbeddingProvider, EmbeddingProviderConfig } from "./types.js";

const REQUEST_TIMEOUT_MS = 30_000;
const MAX_ATTEMPTS = 4;

// 统一构造 embedding 错误，网络层和服务层均使用 AI_UNAVAILABLE 语义。
function embeddingError(
  message: string,
  noRetry = false,
): Error & { code: string; noRetry?: boolean } {
  return Object.assign(new Error(message), { code: "AI_UNAVAILABLE", noRetry });
}

export class OllamaEmbeddingProvider implements EmbeddingProvider {
  private readonly endpoint: string;

  // 规范化 Ollama 地址并固定到 OpenAI 兼容 embedding 路径。
  constructor(private readonly config: EmbeddingProviderConfig) {
    // Ollama 暴露 OpenAI 兼容路径，因此 embedding 不需要厂商专用协议。
    this.endpoint = `${config.baseURL.replace(/\/+$/, "")}/v1/embeddings`;
  }

  // 批量请求文本向量，校验返回顺序、数量和每个向量的数值类型。
  async embed(
    texts: string[],
  ): Promise<{ vectors: number[][]; model: string; dim: number }> {
    // 空数组没有可推断的维度，直接在发请求前拒绝无效输入。
    if (texts.length === 0)
      throw Object.assign(new Error("Embedding 输入不能为空"), {
        code: "VALIDATION_ERROR",
      });
    // 传数组保持输入与返回 data 的一一对应，便于后续 chunk 批量入库。
    const response = await this.request({
      model: this.config.model,
      input: texts,
    });
    const payload = (await response.json()) as {
      data?: Array<{ embedding?: unknown }>;
      model?: unknown;
    };
    // 仅保留完整的数字数组，屏蔽 Ollama 可能附带的其他响应字段。
    const vectors = payload.data
      ?.map((item) => item.embedding)
      .filter(
        (item): item is number[] =>
          Array.isArray(item) &&
          item.every((value) => typeof value === "number"),
      );
    if (!vectors || vectors.length !== texts.length || !vectors[0]?.length)
      throw embeddingError("Embedding 服务返回了无法识别的向量");
    // 首个向量长度即本批次维度，调用方据此配置向量索引。
    return {
      vectors,
      model:
        typeof payload.model === "string" ? payload.model : this.config.model,
      dim: vectors[0].length,
    };
  }

  // 用 ping 文本取得真实维度；失败时返回可展示原因而不让设置页崩溃。
  async healthCheck(): Promise<{ ok: boolean; detail: string; dim?: number }> {
    try {
      const result = await this.embed(["ping"]);
      return { ok: true, detail: "连接正常", dim: result.dim };
    } catch (error) {
      return {
        ok: false,
        detail: (error as Error).message || "Embedding 服务不可用",
      };
    }
  }

  // 发送 embedding 请求并复用与 LLM 相同的超时和退避策略。
  private async request(body: unknown): Promise<Response> {
    let lastError: unknown;
    for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt += 1) {
      // 每次重试重新创建取消控制器，保证定时器生命周期清晰可控。
      const controller = new AbortController();
      // 30 秒上限避免本地模型加载或服务停止时阻塞整个设置流程。
      const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
      try {
        // Ollama 本机无需密钥，只发送协议要求的 JSON 请求头和负载。
        const response = await fetch(this.endpoint, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
          signal: controller.signal,
        });
        // 成功响应交由 embed 读取 data，request 不消费响应体。
        if (response.ok) return response;
        // Ollama 对模型不存在和路径不支持通常都返回 404，直接给出可操作的提示。
        if (response.status === 404)
          throw embeddingError(
            "Embedding 模型或端点不存在，请检查 Ollama 地址和模型名",
            true,
          );
        if (response.status < 500)
          throw embeddingError(
            `Embedding 请求失败（HTTP ${response.status}）`,
            true,
          );
        lastError = embeddingError("Embedding 服务暂时不可用");
      } catch (error) {
        if ((error as { noRetry?: boolean }).noRetry) throw error;
        lastError =
          error instanceof Error
            ? error
            : embeddingError("Embedding 服务不可用");
      } finally {
        // 清理本轮计时器，防止完成请求后仍触发 AbortController。
        clearTimeout(timeout);
      }
      if (attempt < MAX_ATTEMPTS - 1)
        await new Promise((resolve) =>
          setTimeout(resolve, 1000 * 2 ** attempt),
        );
    }
    throw lastError instanceof Error
      ? lastError
      : embeddingError("Embedding 服务不可用");
  }
}
