import { BrowserWindow, Notification } from "electron";
import type { WebContents } from "electron";
import { createLlmProvider } from "../../infrastructure/llm/index.js";
import type { ChatMessage, ChatResult } from "../../infrastructure/llm/types.js";
import { assistantService } from "../assistant/service.js";
import type { MessageRow } from "../assistant/service.js";
import { settingsService } from "../settings/service.js";
import { estimateTokens, truncateToTokens } from "../../../shared/domain/tokens.js";

// 预算是轻量估算值，不引入 tiktoken；同时保留 40 条上限防止极端小文本撑满请求。
const CONTEXT_TOKEN_BUDGET = 6000;
const MAX_CONTEXT_MESSAGES = 40;
const MAX_CONCURRENT_STREAMS = 2;

interface ActiveChat {
  controller: AbortController;
  sender: WebContents;
  userMessageId: string;
  think: boolean;
  text: string;
  reasoning: string;
  usage?: ChatResult["usage"];
  model?: string;
}

type ChatError = Error & { code?: string };


export class ChatService {
  // active 只保存正在生成的流；历史消息始终以数据库为准。
  private readonly active = new Map<string, ActiveChat>();
  // 用户消息落库期间也占用会话，防止两个快速 start 穿过异步写入窗口。
  private readonly starting = new Set<string>();
  private running = 0;

  async start(input: { sessionId: string; message: string; think?: boolean }, sender: WebContents): Promise<{ sessionId: string }> {
    if (this.active.has(input.sessionId) || this.starting.has(input.sessionId))
      throw Object.assign(new Error("该会话正在生成回答"), { code: "CONFLICT" });
    if (this.running >= MAX_CONCURRENT_STREAMS)
      throw Object.assign(new Error("已有 2 个会话正在生成，请等待完成"), { code: "CONFLICT" });
    this.starting.add(input.sessionId);
    let userMessage: MessageRow;
    try {
      userMessage = await assistantService.appendMessage(input.sessionId, "user", input.message);
    } catch (error) {
      this.starting.delete(input.sessionId);
      throw error;
    }
    const active: ActiveChat = {
      controller: new AbortController(),
      sender,
      userMessageId: userMessage.id,
      think: input.think ?? false,
      text: "",
      reasoning: "",
      model: undefined,
    };
    this.starting.delete(input.sessionId);
    this.active.set(input.sessionId, active);
    this.running += 1;
    // 异步启动让 IPC 先返回，页面可以立即建立事件归属和停止按钮状态。
    setTimeout(() => void this.run(input.sessionId, active), 0);
    return { sessionId: input.sessionId };
  }

  // abort 通过 provider signal 直达 fetch，停止后不等待下一个 SSE chunk。
  stop(sessionId: string): { stopped: boolean } {
    const current = this.active.get(sessionId);
    if (!current) return { stopped: false };
    current.controller.abort();
    return { stopped: true };
  }

  private async run(sessionId: string, active: ActiveChat): Promise<void> {
    try {
      if (active.controller.signal.aborted) {
        await this.rollbackUserMessage(sessionId, active.userMessageId);
        this.sendDone(sessionId, active, true);
        return;
      }
      const config = await settingsService.getActiveChatConfig();
      if (!config) {
        await this.rollbackUserMessage(sessionId, active.userMessageId);
        this.sendError(sessionId, active.sender, Object.assign(new Error("未配置 LLM"), { code: "AI_UNAVAILABLE" }));
        return;
      }
      active.model = config.model;
      const context = await assistantService.context(sessionId, MAX_CONTEXT_MESSAGES);
      const messages = this.buildContext(context.assistant.systemPrompt, context.messages);
      let lastError: unknown;
      // 非用户中止的断流静默整轮重试一次；超时不重试，避免长请求被重复放大。
      for (let attempt = 0; attempt < 2; attempt += 1) {
        try {
          await this.consumeStream(sessionId, active, config, messages);
          lastError = undefined;
          break;
        } catch (error) {
          lastError = error;
          const code = (error as ChatError).code;
          if (active.controller.signal.aborted || attempt === 1 || code === "AI_TIMEOUT") break;
          active.text = "";
          active.reasoning = "";
          active.usage = undefined;
        }
      }
      if (lastError) throw lastError;
      // 推理必须依附已输出的正文保存，避免思考中断时留下只有推理的空消息。
      if (active.text)
        await assistantService.appendMessage(sessionId, "assistant", active.text, active.think ? active.reasoning : undefined);
      this.sendDone(sessionId, active, active.controller.signal.aborted);
      // 标题生成与对话完成解耦，后台失败静默，不让额外的短请求阻塞用户操作。
      if (!active.controller.signal.aborted && active.text) {
        void assistantService.generateSessionTitle(sessionId).then((session) => {
          if (!session) return;
          BrowserWindow.getAllWindows().forEach((window) => {
            if (!window.isDestroyed())
              window.webContents.send("sessions:title-updated", {
                id: session.id,
                title: session.title,
              });
          });
        }).catch(() => undefined);
      }
      if (active.text && !active.controller.signal.aborted) this.notifyWhenBackground();
    } catch (error) {
      if (!active.controller.signal.aborted) {
        await this.rollbackUserMessage(sessionId, active.userMessageId);
        this.sendError(sessionId, active.sender, error as ChatError);
      } else {
        // 用户中止时仅保存已输出的正文；正文尚未开始则不持久化无意义的推理片段。
        if (active.text)
          await assistantService.appendMessage(sessionId, "assistant", active.text, active.think ? active.reasoning : undefined);
        this.sendDone(sessionId, active, true);
      }
    } finally {
      this.active.delete(sessionId);
      this.running = Math.max(0, this.running - 1);
    }
  }

  private buildContext(systemPrompt: string, history: ChatMessage[]): ChatMessage[] {
    const result: ChatMessage[] = [];
    let remaining = CONTEXT_TOKEN_BUDGET;
    if (systemPrompt) {
      const prompt = truncateToTokens(systemPrompt, remaining);
      result.push({ role: "system", content: prompt });
      remaining = Math.max(1, remaining - estimateTokens(prompt));
    }
    const selected: ChatMessage[] = [];
    for (let index = history.length - 1; index >= 0; index -= 1) {
      const message = history[index]!;
      if (remaining <= 0 && selected.length > 0) break;
      const cost = estimateTokens(message.content);
      const content = cost > remaining ? truncateToTokens(message.content, remaining) : message.content;
      selected.unshift({ role: message.role, content });
      remaining -= estimateTokens(content);
    }
    result.push(...selected);
    return result;
  }

  private async consumeStream(sessionId: string, active: ActiveChat, config: { baseURL: string; apiKey: string; model: string; protocol?: "chat-completions" | "responses" }, messages: ChatMessage[]): Promise<void> {
    const provider = createLlmProvider(config);
    for await (const chunk of provider.chatStream({ messages, model: config.model, temperature: 0.7, stream: true, signal: active.controller.signal })) {
      if (active.controller.signal.aborted) break;
      if (chunk.usage) active.usage = chunk.usage;
      if (chunk.model) active.model = chunk.model;
      if (chunk.type === "content") {
        if (!chunk.delta) continue;
        active.text += chunk.delta;
        this.send(active.sender, "ai:chat:delta", { sessionId, delta: chunk.delta });
      } else if (active.think) {
        active.reasoning += chunk.delta;
        this.send(active.sender, "ai:chat:reasoning", { sessionId, delta: chunk.delta });
      }
    }
  }

  private sendDone(sessionId: string, active: ActiveChat, aborted: boolean): void {
    this.send(active.sender, "ai:chat:done", { sessionId, fullText: active.text, reasoning: active.think ? active.reasoning : undefined, model: active.model, usage: active.usage, aborted });
  }

  private async rollbackUserMessage(sessionId: string, messageId: string): Promise<void> {
    try {
      await assistantService.removeMessage(messageId, sessionId);
    } catch {
      // 原始错误优先反馈页面，回滚失败不能覆盖主错误。
    }
  }

  private sendError(sessionId: string, sender: WebContents, error: ChatError): void {
    const code = error.code === "AI_AUTH_ERROR" ? "AI_AUTH_ERROR" : error.code === "AI_TIMEOUT" ? "AI_TIMEOUT" : error.code === "AI_UNAVAILABLE" || error.code === "DB_UNAVAILABLE" ? "AI_UNAVAILABLE" : "INTERNAL_ERROR";
    this.send(sender, "ai:chat:error", { sessionId, code, message: error.message || "AI 服务不可用" });
  }

  private notifyWhenBackground(): void {
    if (!Notification.isSupported()) return;
    const windows = BrowserWindow.getAllWindows();
    if (windows.some((window) => !window.isDestroyed() && window.isFocused())) return;
    new Notification({ title: "AI 助手", body: "回答已生成" }).show();
  }

  private send(sender: WebContents, channel: string, payload: unknown): void {
    try {
      if (!sender.isDestroyed()) sender.send(channel, payload);
    } catch {
      // 页面销毁后停止推送，但不影响数据库收尾。
    }
  }
}

export const chatService = new ChatService();
