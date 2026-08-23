export interface ChatMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

export interface ChatRequest {
  messages: ChatMessage[];
  model: string;
  temperature?: number;
  maxTokens?: number;
  // 为 true 时由 chatStream 提供增量事件；chat 会聚合后返回完整结果。
  stream?: boolean;
  // 外部取消信号由聊天服务传入，停止操作时立即中断底层 fetch。
  signal?: AbortSignal;
}

export interface ChatResult {
  content: string;
  // 推理模型可能返回独立思考摘要；普通模型没有该字段。
  reasoning?: string;
  usage?: {
    promptTokens: number;
    completionTokens: number;
    reasoningTokens?: number;
  };
  model: string;
}

export interface ChatChunk {
  // content 是可直接展示的回答增量，reasoning 是可选的推理摘要增量。
  type: "content" | "reasoning";
  delta: string;
  usage?: ChatResult["usage"];
  model?: string;
}

export interface LlmProvider {
  // 发送一次非流式对话请求，统一返回文本和可选用量信息。
  chat(request: ChatRequest): Promise<ChatResult>;
  // 以 AsyncGenerator 暴露 SSE 增量，调用方可按需渲染而不必等待完整回答。
  chatStream(request: ChatRequest): AsyncGenerator<ChatChunk>;
  // 发起最小 ping 请求，确认端点、模型和鉴权均可用。
  healthCheck(): Promise<{ ok: boolean; detail: string }>;
}

export interface LlmProviderConfig {
  baseURL: string;
  apiKey: string;
  model: string;
  // chat-completions 保持主流兼容协议，responses 对接 OpenAI Responses API。
  protocol?: "chat-completions" | "responses";
}
