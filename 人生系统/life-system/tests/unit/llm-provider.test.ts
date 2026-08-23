import { describe, expect, it, vi } from "vitest";
import { OpenAiCompatibleProvider } from "../../src/main/infrastructure/llm/openai-compatible.js";

const request = {
  messages: [{ role: "user" as const, content: "你好" }],
  model: "test-model",
};

describe("LLM provider protocols", () => {
  it("解析 Chat Completions 的回答与推理字段", async () => {
    // 用 fetch mock 隔离真实网络，验证协议字段映射和 reasoning_content 解析。
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              model: "chat-model",
              choices: [
                { message: { content: "回答", reasoning_content: "推理" } },
              ],
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          ),
      ),
    );
    const provider = new OpenAiCompatibleProvider({
      baseURL: "http://127.0.0.1:11434",
      apiKey: "",
      model: "test-model",
    });
    await expect(provider.chat(request)).resolves.toMatchObject({
      content: "回答",
      reasoning: "推理",
    });
  });

  it("解析 OpenAI Responses 的 output_text 与 reasoning 摘要", async () => {
    // Responses API 的 reasoning 位于 output.reasoning.summary，回答位于 output_text。
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              model: "responses-model",
              output_text: "回答",
              output: [{ type: "reasoning", summary: [{ text: "推理" }] }],
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          ),
      ),
    );
    const provider = new OpenAiCompatibleProvider({
      baseURL: "https://api.openai.com",
      apiKey: "test-key",
      model: "test-model",
      protocol: "responses",
    });
    await expect(provider.chat(request)).resolves.toMatchObject({
      content: "回答",
      reasoning: "推理",
    });
  });

  it("按 SSE 增量产出回答和推理", async () => {
    // ReadableStream 模拟真实 SSE，确保 chatStream 不必等待完整响应即可产出 chunk。
    const encoder = new TextEncoder();
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            new ReadableStream({
              start(controller) {
                controller.enqueue(
                  encoder.encode(
                    'data: {"choices":[{"delta":{"reasoning_content":"推理"}}]}\n\n',
                  ),
                );
                controller.enqueue(
                  encoder.encode(
                    'data: {"choices":[{"delta":{"content":"回答"}}]}\n\n',
                  ),
                );
                controller.enqueue(encoder.encode("data: [DONE]\n\n"));
                controller.close();
              },
            }),
            { status: 200, headers: { "Content-Type": "text/event-stream" } },
          ),
      ),
    );
    const provider = new OpenAiCompatibleProvider({
      baseURL: "http://127.0.0.1:11434",
      apiKey: "",
      model: "test-model",
    });
    const chunks: string[] = [];
    for await (const chunk of provider.chatStream({
      ...request,
      stream: true,
    })) {
      chunks.push(`${chunk.type}:${chunk.delta}`);
    }
    expect(chunks).toEqual(["reasoning:推理", "content:回答"]);
  });

  it("解析 Responses API 的 SSE 事件", async () => {
    // Responses 使用事件类型区分 reasoning_summary_text.delta 和 output_text.delta。
    const encoder = new TextEncoder();
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            new ReadableStream({
              start(controller) {
                controller.enqueue(
                  encoder.encode(
                    'data: {"type":"response.reasoning_summary_text.delta","delta":"推理"}\n\n',
                  ),
                );
                controller.enqueue(
                  encoder.encode(
                    'data: {"type":"response.output_text.delta","delta":"回答"}\n\n',
                  ),
                );
                controller.enqueue(encoder.encode("data: [DONE]\n\n"));
                controller.close();
              },
            }),
            { status: 200, headers: { "Content-Type": "text/event-stream" } },
          ),
      ),
    );
    const provider = new OpenAiCompatibleProvider({
      baseURL: "https://api.openai.com",
      apiKey: "test-key",
      model: "test-model",
      protocol: "responses",
    });
    const chunks: string[] = [];
    for await (const chunk of provider.chatStream({
      ...request,
      stream: true,
    })) {
      chunks.push(`${chunk.type}:${chunk.delta}`);
    }
    expect(chunks).toEqual(["reasoning:推理", "content:回答"]);
  });
});
