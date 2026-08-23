import { describe, expect, it, vi } from "vitest";
import { OllamaEmbeddingProvider } from "../../src/main/infrastructure/embedding/ollama.js";

describe("Ollama embedding provider", () => {
  it("解析向量并返回维度", async () => {
    // 使用兼容端点的最小响应验证输入数组、模型名和维度映射。
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              model: "qwen3-embedding:0.6b",
              data: [
                { embedding: [0.1, 0.2, 0.3] },
                { embedding: [0.4, 0.5, 0.6] },
              ],
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          ),
      ),
    );
    const provider = new OllamaEmbeddingProvider({
      baseURL: "http://127.0.0.1:11434",
      model: "qwen3-embedding:0.6b",
    });
    await expect(provider.embed(["第一条", "第二条"])).resolves.toEqual({
      vectors: [
        [0.1, 0.2, 0.3],
        [0.4, 0.5, 0.6],
      ],
      model: "qwen3-embedding:0.6b",
      dim: 3,
    });
  });

  it("空输入直接返回 VALIDATION_ERROR", async () => {
    // 空批次没有维度且不应触发网络调用，错误在 provider 本地完成校验。
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    const provider = new OllamaEmbeddingProvider({
      baseURL: "http://127.0.0.1:11434",
      model: "qwen3-embedding:0.6b",
    });
    await expect(provider.embed([])).rejects.toMatchObject({
      code: "VALIDATION_ERROR",
    });
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
