import type {
  ChatChunk,
  ChatRequest,
  ChatResult,
  LlmProvider,
  LlmProviderConfig,
} from "./types.js";

const CONNECT_TIMEOUT_MS = 10_000;
const STREAM_IDLE_TIMEOUT_MS = 300_000;
const MAX_ATTEMPTS = 4;

// 创建带错误码的内部异常，供 IPC 层把鉴权失败和服务不可用分开呈现。
function providerError(
  code: "AI_UNAVAILABLE" | "AI_AUTH_ERROR" | "AI_TIMEOUT",
  message: string,
  noRetry = false,
): Error & { code: string; noRetry?: boolean } {
  return Object.assign(new Error(message), { code, noRetry });
}

// 将 Web Response 的 SSE 数据逐事件解析，兼容两种协议都使用的 data: JSON 格式。
async function* readSse(
  response: Response,
): AsyncGenerator<Record<string, unknown>> {
  if (!response.body)
    throw providerError("AI_UNAVAILABLE", "AI 服务未返回流式数据");
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let dataLines: string[] = [];
  let receivedEvent = false;
  try {
    while (true) {
      // 模型首字前可持续思考，不施加总等待上限；首个事件后才保护异常空闲连接。
      const timeoutMs = receivedEvent ? STREAM_IDLE_TIMEOUT_MS : undefined;
      let timer: ReturnType<typeof setTimeout> | undefined;
      const next = timeoutMs
        ? await Promise.race([
            reader.read(),
            new Promise<never>((_, reject) => {
              timer = setTimeout(() => reject(providerError("AI_TIMEOUT", "AI 流式响应空闲超时")), timeoutMs);
            }),
          ]).finally(() => {
            if (timer) clearTimeout(timer);
          })
        : await reader.read();
      buffer += decoder.decode(next.value ?? new Uint8Array(), {
        stream: !next.done,
      });
      const lines = buffer.split(/\r?\n/);
      buffer = lines.pop() ?? "";
      if (next.done && buffer) {
        lines.push(buffer);
        buffer = "";
      }
      for (const line of lines) {
        // SSE 以空行提交一个事件；data 允许分成多行，按协议拼接后再解析 JSON。
        if (line === "") {
          const data = dataLines.join("\n").trim();
          dataLines = [];
          if (!data || data === "[DONE]") continue;
          try {
            receivedEvent = true;
            yield JSON.parse(data) as Record<string, unknown>;
          } catch {
            throw providerError("AI_UNAVAILABLE", "AI 流式响应格式无法识别");
          }
          continue;
        }
        if (line.startsWith("data:")) dataLines.push(line.slice(5).trimStart());
      }
      if (next.done) break;
    }
    // 服务端偶尔不发送结尾空行，仍尝试处理最后一条完整 data 事件。
    const lastData = dataLines.join("\n").trim();
    if (lastData && lastData !== "[DONE]") {
      try {
        receivedEvent = true;
        yield JSON.parse(lastData) as Record<string, unknown>;
      } catch {
        throw providerError("AI_UNAVAILABLE", "AI 流式响应格式无法识别");
      }
    }
  } finally {
    // 生成器提前结束时主动释放网络 reader，避免连接泄漏。
    await reader.cancel();
  }
}

export class OpenAiCompatibleProvider implements LlmProvider {
  private readonly endpoint: string;

  // 保存连接配置并预先计算协议端点，避免每次请求重复拼接路径。
  constructor(private readonly config: LlmProviderConfig) {
    const path =
      config.protocol === "responses"
        ? "/v1/responses"
        : "/v1/chat/completions";
    this.endpoint = `${config.baseURL.replace(/\/+$/, "")}${path}`;
  }

  // 根据协议构造请求；stream=true 时聚合流式事件，实时消费请调用 chatStream。
  async chat(request: ChatRequest): Promise<ChatResult> {
    if (request.stream) {
      let content = "";
      let reasoning = "";
      let usage: ChatResult["usage"];
      for await (const chunk of this.chatStream(request)) {
        if (chunk.type === "content") content += chunk.delta;
        else reasoning += chunk.delta;
        if (chunk.usage) usage = chunk.usage;
      }
      return {
        content,
        reasoning: reasoning || undefined,
        model: request.model || this.config.model,
        usage,
      };
    }
    const response = await this.request(this.buildBody(request, false), request.signal);
    const payload = (await response.json()) as Record<string, unknown>;
    return this.config.protocol === "responses"
      ? this.parseResponsesResult(payload, request.model || this.config.model)
      : this.parseChatCompletionsResult(
          payload,
          request.model || this.config.model,
        );
  }

  // 通过 SSE 逐个产出回答或推理增量，调用方可以在每个 chunk 到达时更新 UI。
  async *chatStream(request: ChatRequest): AsyncGenerator<ChatChunk> {
    const response = await this.request(this.buildBody(request, true), request.signal);
    for await (const event of readSse(response)) {
      if (this.config.protocol === "responses") {
        const type = typeof event.type === "string" ? event.type : "";
        const delta = typeof event.delta === "string" ? event.delta : "";
        if (delta && type.includes("reasoning"))
          yield { type: "reasoning", delta };
        else if (delta && type.includes("output_text"))
          yield { type: "content", delta };
        if (type === "error")
          throw providerError("AI_UNAVAILABLE", "AI 流式请求失败");
        const usage = this.parseUsage(event.usage ?? (event.response as Record<string, unknown> | undefined)?.usage, "responses");
        if (usage) yield { type: "content", delta: "", usage, model: typeof event.model === "string" ? event.model : undefined };
      } else {
        const choices = Array.isArray(event.choices) ? event.choices : [];
        const delta = (
          choices[0] as { delta?: Record<string, unknown> } | undefined
        )?.delta;
        const content = typeof delta?.content === "string" ? delta.content : "";
        const reasoning =
          typeof delta?.reasoning_content === "string"
            ? delta.reasoning_content
            : "";
        if (content) yield { type: "content", delta: content };
        if (reasoning) yield { type: "reasoning", delta: reasoning };
        const usage = this.parseUsage(event.usage, "chat");
        if (usage) yield { type: "content", delta: "", usage, model: typeof event.model === "string" ? event.model : undefined };
      }
    }
  }

  // 用一条极短消息验证当前模型、协议端点和鉴权是否能完成完整调用链。
  async healthCheck(): Promise<{ ok: boolean; detail: string }> {
    try {
      await this.chat({
        model: this.config.model,
        messages: [{ role: "user", content: "ping" }],
        maxTokens: 8,
      });
      return { ok: true, detail: "连接正常" };
    } catch (error) {
      return { ok: false, detail: (error as Error).message || "AI 服务不可用" };
    }
  }

  // 按协议映射请求体；Responses API 使用 input message content 数组而非 messages 字段。
  private buildBody(
    request: ChatRequest,
    stream: boolean,
  ): Record<string, unknown> {
    const common = {
      model: request.model || this.config.model,
      temperature: request.temperature ?? 0.7,
      stream,
    };
    if (this.config.protocol === "responses") {
      return {
        ...common,
        input: request.messages.map((message) => ({
          role: message.role,
          content: [{ type: "input_text", text: message.content }],
        })),
        max_output_tokens: request.maxTokens ?? 2048,
      };
    }
    return {
      ...common,
      messages: request.messages,
      max_tokens: request.maxTokens ?? 2048,
      ...(stream ? { stream_options: { include_usage: true } } : {}),
    };
  }

  // 解析 Chat Completions 的文本、推理和用量字段，未知供应商字段一律忽略。
  private parseChatCompletionsResult(
    payload: Record<string, unknown>,
    fallbackModel: string,
  ): ChatResult {
    const choices = Array.isArray(payload.choices) ? payload.choices : [];
    const message = (
      choices[0] as { message?: Record<string, unknown> } | undefined
    )?.message;
    const content = typeof message?.content === "string" ? message.content : "";
    const reasoning =
      typeof message?.reasoning_content === "string"
        ? message.reasoning_content
        : undefined;
    if (!content && !reasoning)
      throw providerError("AI_UNAVAILABLE", "AI 服务返回了无法识别的内容");
    const usage = this.parseUsage(payload.usage, "chat");
    return {
      content,
      reasoning,
      model: typeof payload.model === "string" ? payload.model : fallbackModel,
      usage,
    };
  }

  // 解析 Responses API 的 output_text、reasoning 摘要和 usage，兼容顶层 output_text 缺失情况。
  private parseResponsesResult(
    payload: Record<string, unknown>,
    fallbackModel: string,
  ): ChatResult {
    const output = Array.isArray(payload.output) ? payload.output : [];
    let content =
      typeof payload.output_text === "string" ? payload.output_text : "";
    let reasoning = "";
    for (const item of output) {
      const record = item as {
        type?: unknown;
        content?: Array<Record<string, unknown>>;
        summary?: Array<Record<string, unknown>>;
      };
      const fragments = [...(record.content ?? []), ...(record.summary ?? [])];
      for (const fragment of fragments) {
        if (typeof fragment.text !== "string") continue;
        if (record.type === "reasoning") reasoning += fragment.text;
        else if (!content) content += fragment.text;
      }
    }
    if (!content && !reasoning)
      throw providerError("AI_UNAVAILABLE", "AI 服务返回了无法识别的内容");
    const usage = this.parseUsage(payload.usage, "responses");
    return {
      content,
      reasoning: reasoning || undefined,
      model: typeof payload.model === "string" ? payload.model : fallbackModel,
      usage,
    };
  }

  // 执行一次带超时的 HTTP 请求，并仅对网络错误或 5xx 做指数退避重试。
  private async request(body: unknown, externalSignal?: AbortSignal): Promise<Response> {
    let lastError: unknown;
    for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt += 1) {
      // 每次尝试都使用独立 AbortController，避免上一次超时信号污染后续重试。
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort("connect-timeout"), CONNECT_TIMEOUT_MS);
      const onExternalAbort = () => controller.abort("external-abort");
      externalSignal?.addEventListener("abort", onExternalAbort, { once: true });
      try {
        if (externalSignal?.aborted) throw Object.assign(new Error("请求已停止"), { name: "AbortError" });
        const headers: Record<string, string> = {
          "Content-Type": "application/json",
          Accept: "text/event-stream",
        };
        if (this.config.apiKey)
          headers.Authorization = `Bearer ${this.config.apiKey}`;
        const response = await fetch(this.endpoint, {
          method: "POST",
          headers,
          body: JSON.stringify(body),
          signal: controller.signal,
        });
        if (response.ok) return response;
        if (response.status === 401 || response.status === 403)
          throw providerError("AI_AUTH_ERROR", "AI 鉴权失败，请检查 API Key");
        if (response.status < 500)
          throw providerError(
            "AI_UNAVAILABLE",
            `AI 请求失败（HTTP ${response.status}）`,
            true,
          );
          lastError = providerError("AI_UNAVAILABLE", "AI 服务暂时不可用");
      } catch (error) {
        const candidate = error as Error & { code?: string; noRetry?: boolean };
        if (externalSignal?.aborted) throw candidate;
        if (controller.signal.aborted) throw providerError("AI_TIMEOUT", "AI 连接超时", true);
        if (candidate.code === "AI_AUTH_ERROR" || candidate.noRetry)
          throw candidate;
        lastError =
          candidate.code === "AI_UNAVAILABLE"
            ? candidate
            : providerError("AI_UNAVAILABLE", "AI 服务不可用");
      } finally {
        // 无论成功或失败都清理定时器，避免请求结束后仍触发 abort 回调。
        clearTimeout(timeout);
        externalSignal?.removeEventListener("abort", onExternalAbort);
      }
      if (attempt < MAX_ATTEMPTS - 1)
        await new Promise((resolve) =>
          setTimeout(resolve, 1000 * 2 ** attempt),
        );
    }
    throw lastError instanceof Error
      ? lastError
      : providerError("AI_UNAVAILABLE", "AI 服务不可用");
  }

  // 兼容 Chat Completions 与 Responses 两套 usage 字段，并保留推理 token 细分。
  private parseUsage(value: unknown, protocol: "chat" | "responses" = "chat"): ChatResult["usage"] | undefined {
    if (!value || typeof value !== "object") return undefined;
    const usage = value as Record<string, unknown>;
    const prompt = protocol === "responses" ? usage.input_tokens : usage.prompt_tokens;
    const completion = protocol === "responses" ? usage.output_tokens : usage.completion_tokens;
    const details = usage.completion_tokens_details as Record<string, unknown> | undefined;
    const reasoning = usage.reasoning_tokens ?? details?.reasoning_tokens;
    if (prompt === undefined && completion === undefined && reasoning === undefined) return undefined;
    return {
      promptTokens: Number(prompt ?? 0),
      completionTokens: Number(completion ?? 0),
      ...(reasoning === undefined ? {} : { reasoningTokens: Number(reasoning) }),
    };
  }
}
