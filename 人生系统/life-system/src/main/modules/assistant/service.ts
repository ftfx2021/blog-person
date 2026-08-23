import { randomUUID } from "node:crypto";
import { writeFile } from "node:fs/promises";
import { join } from "node:path";
import type { RowDataPacket } from "mysql2/promise";
import type {
  AssistantInput,
  AssistantUpdate,
} from "../../../shared/contracts/ai.js";
import {
  assistantInputSchema,
  assistantUpdateSchema,
} from "../../../shared/contracts/ai.js";
import type { ChatMessage } from "../../infrastructure/llm/types.js";
import { createLlmProvider } from "../../infrastructure/llm/index.js";
import { requirePool } from "../../infrastructure/db/pool.js";
import { utcNow } from "../common/database.js";
import { applicationPaths } from "../../infrastructure/filesystem/paths.js";
import { settingsService } from "../settings/service.js";

export interface AssistantRow extends RowDataPacket {
  id: string;
  name: string;
  description: string | null;
  systemPrompt: string;
  modelConfigJson: unknown;
  knowledgeBaseId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SessionRow extends RowDataPacket {
  id: string;
  assistantId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  pinned: boolean;
}

export interface MessageRow extends RowDataPacket {
  id: string;
  sessionId: string;
  role: "user" | "assistant";
  content: string;
  reasoning: string | null;
  createdAt: string;
}

const assistantSelect = `SELECT id,name,description,system_prompt AS systemPrompt,
  model_config_json AS modelConfigJson,knowledge_base_id AS knowledgeBaseId,
  created_at AS createdAt,updated_at AS updatedAt FROM ai_assistant`;
const sessionSelect = `SELECT id,assistant_id AS assistantId,title,
  created_at AS createdAt,updated_at AS updatedAt,CAST(pinned AS UNSIGNED) AS pinned FROM ai_session`;
const messageSelect = `SELECT id,session_id AS sessionId,role,content,
  reasoning,created_at AS createdAt FROM ai_message`;

function notFound(message: string): never {
  throw Object.assign(new Error(message), { code: "NOT_FOUND" });
}

async function requireAssistant(id: string): Promise<AssistantRow> {
  const [rows] = await requirePool().query<AssistantRow[]>(
    `${assistantSelect} WHERE id=?`,
    [id],
  );
  if (!rows[0]) notFound("助手不存在");
  return rows[0];
}

async function requireSession(id: string): Promise<SessionRow> {
  const [rows] = await requirePool().query<SessionRow[]>(
    `${sessionSelect} WHERE id=?`,
    [id],
  );
  if (!rows[0]) notFound("会话不存在");
  return rows[0];
}

export const assistantService = {
  // 助手列表按最近编辑时间排序，保证刚维护的助手优先出现在管理入口。
  list: async (): Promise<AssistantRow[]> => {
    const [rows] = await requirePool().query<AssistantRow[]>(
      `${assistantSelect} ORDER BY updated_at DESC,created_at DESC`,
    );
    return rows;
  },

  // 创建助手只写本期开放字段，预留字段明确置 NULL，避免误启用未来能力。
  create: async (input: AssistantInput): Promise<AssistantRow> => {
    const parsed = assistantInputSchema.parse(input);
    const id = randomUUID();
    const now = utcNow();
    await requirePool().query(
      "INSERT INTO ai_assistant (id,name,description,system_prompt,model_config_json,knowledge_base_id,created_at,updated_at) VALUES (?,?,?,?,NULL,NULL,?,?)",
      [id, parsed.name, parsed.description || null, parsed.systemPrompt, now, now],
    );
    return requireAssistant(id);
  },

  // 更新只拼接固定白名单列；没有实际字段时返回 CONFLICT 而不是写入空更新。
  update: async (input: AssistantUpdate): Promise<AssistantRow> => {
    const parsed = assistantUpdateSchema.parse(input);
    await requireAssistant(parsed.id);
    const fields: string[] = [];
    const values: unknown[] = [];
    if (parsed.name !== undefined) {
      fields.push("name=?");
      values.push(parsed.name);
    }
    if (parsed.description !== undefined) {
      fields.push("description=?");
      values.push(parsed.description || null);
    }
    if (parsed.systemPrompt !== undefined) {
      fields.push("system_prompt=?");
      values.push(parsed.systemPrompt);
    }
    if (!fields.length)
      throw Object.assign(new Error("至少修改一项助手配置"), {
        code: "CONFLICT",
      });
    fields.push("updated_at=?");
    values.push(utcNow(), parsed.id);
    await requirePool().query(
      `UPDATE ai_assistant SET ${fields.join(",")} WHERE id=?`,
      values,
    );
    return requireAssistant(parsed.id);
  },

  // 删除助手前显式确认存在；数据库外键负责级联删除会话与消息。
  remove: async (id: string): Promise<{ removed: true }> => {
    await requireAssistant(id);
    await requirePool().query("DELETE FROM ai_assistant WHERE id=?", [id]);
    return { removed: true };
  },

  // 会话列表只接受已存在助手，按活跃时间倒序供左侧栏切换。
  sessions: async (assistantId: string): Promise<SessionRow[]> => {
    await requireAssistant(assistantId);
    const [rows] = await requirePool().query<SessionRow[]>(
      `${sessionSelect} WHERE assistant_id=? ORDER BY pinned DESC,updated_at DESC,created_at DESC`,
      [assistantId],
    );
    return rows;
  },

  // 新建会话不提前写消息，首条用户消息到达后再自动命名。
  createSession: async (assistantId: string): Promise<SessionRow> => {
    await requireAssistant(assistantId);
    const id = randomUUID();
    const now = utcNow();
    await requirePool().query(
      "INSERT INTO ai_session (id,assistant_id,title,created_at,updated_at) VALUES (?,?,?, ?,?)",
      [id, assistantId, "新会话", now, now],
    );
    return requireSession(id);
  },

  // 重命名和更新时间在同一条更新中完成，保证会话排序能反映用户最近操作。
  renameSession: async (id: string, title: string): Promise<SessionRow> => {
    await requireSession(id);
    await requirePool().query(
      "UPDATE ai_session SET title=?,updated_at=? WHERE id=?",
      [title, utcNow(), id],
    );
    return requireSession(id);
  },

  // 删除会话使用级联约束清理消息，删除语义不保留回收站或软删除记录。
  removeSession: async (id: string): Promise<{ removed: true }> => {
    await requireSession(id);
    await requirePool().query("DELETE FROM ai_session WHERE id=?", [id]);
    return { removed: true };
  },

  // 消息按“最近 offset 条倒序取出再反转”，页面始终得到旧消息在前的新消息在后。
  messages: async (
    sessionId: string,
    limit: number,
    offset: number,
  ): Promise<{ messages: MessageRow[]; total: number }> => {
    await requireSession(sessionId);
    const [rows] = await requirePool().query<MessageRow[]>(
      `${messageSelect} WHERE session_id=? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?`,
      [sessionId, limit, offset],
    );
    const [countRows] = await requirePool().query<RowDataPacket[]>(
      "SELECT COUNT(*) AS total FROM ai_message WHERE session_id=?",
      [sessionId],
    );
    return {
      messages: rows.reverse(),
      total: Number(countRows[0]?.total ?? 0),
    };
  },

  // 用户或助手消息只负责落库并推进会话更新时间，标题由首轮完成后的后台任务生成。
  appendMessage: async (
    sessionId: string,
    role: "user" | "assistant",
    content: string,
    reasoning?: string,
  ): Promise<MessageRow> => {
    await requireSession(sessionId);
    const id = randomUUID();
    const now = utcNow();
    await requirePool().query(
      "INSERT INTO ai_message (id,session_id,role,content,reasoning,created_at) VALUES (?,?,?,?,?,?)",
      [id, sessionId, role, content, reasoning || null, now],
    );
    await requirePool().query(
      "UPDATE ai_session SET updated_at=? WHERE id=?",
      [now, sessionId],
    );
    const [rows] = await requirePool().query<MessageRow[]>(
      `${messageSelect} WHERE id=?`,
      [id],
    );
    return rows[0]!;
  },

  // 用首轮用户问题和助手回答生成简短标题；失败静默，避免标题任务影响对话主流程。
  generateSessionTitle: async (sessionId: string): Promise<SessionRow | null> => {
    try {
      const session = await requireSession(sessionId);
      if (session.title !== "新会话") return null;
      const [rows] = await requirePool().query<MessageRow[]>(
        `${messageSelect} WHERE session_id=? ORDER BY created_at ASC,id ASC LIMIT 200`,
        [sessionId],
      );
      const userIndex = rows.map((message) => message.role).lastIndexOf("user");
      if (userIndex < 0) return null;
      const userMessage = rows[userIndex]!;
      const assistantMessage = rows.slice(userIndex + 1).find((message) => message.role === "assistant");
      if (!assistantMessage) return null;
      const config = await settingsService.getActiveChatConfig();
      if (!config) return null;
      const prompt = [
        "根据下面的第一轮对话，生成一个不超过 20 个字的会话标题。",
        "只输出标题本身，不要引号、标点或任何解释。",
        `用户：${userMessage.content.slice(0, 2000)}`,
        `助手：${assistantMessage.content.slice(0, 2000)}`,
      ].join("\n");
      const result = await createLlmProvider(config).chat({
        messages: [{ role: "user", content: prompt }],
        model: config.model,
        temperature: 0.3,
        maxTokens: 50,
      });
      const title = result.content
        .replace(/[\r\n"'“”‘’`]/g, "")
        .replace(/\s+/g, " ")
        .trim()
        .slice(0, 20);
      if (!title) return null;
      const now = utcNow();
      const [update] = await requirePool().query(
        "UPDATE ai_session SET title=?,updated_at=? WHERE id=? AND title='新会话'",
        [title, now, sessionId],
      );
      if ((update as { affectedRows?: number }).affectedRows !== 1) return null;
      return requireSession(sessionId);
    } catch {
      return null;
    }
  },

  // 失败回滚按本轮生成的消息 ID 删除，避免内容重复时误删历史用户消息。
  removeMessage: async (id: string, sessionId: string): Promise<void> => {
    await requirePool().query(
      "DELETE FROM ai_message WHERE id=? AND session_id=? AND role='user'",
      [id, sessionId],
    );
  },

  // 重新生成前只删除会话末条助手消息，保留最后一条用户消息作为新的输入上下文。
  removeLastAssistantMessage: async (sessionId: string): Promise<string> => {
    await requireSession(sessionId);
    const [rows] = await requirePool().query<MessageRow[]>(
      `${messageSelect} WHERE session_id=? ORDER BY created_at DESC,id DESC LIMIT 1`,
      [sessionId],
    );
    if (!rows[0] || rows[0].role !== "assistant")
      throw Object.assign(new Error("当前会话没有可重新生成的回答"), {
        code: "VALIDATION_ERROR",
      });
    await requirePool().query("DELETE FROM ai_message WHERE id=? AND session_id=?", [rows[0].id, sessionId]);
    const [users] = await requirePool().query<MessageRow[]>(
      `${messageSelect} WHERE session_id=? AND role='user' ORDER BY created_at DESC,id DESC LIMIT 1`,
      [sessionId],
    );
    if (!users[0])
      throw Object.assign(new Error("当前会话没有可重新生成的提问"), {
        code: "VALIDATION_ERROR",
      });
    return users[0].content;
  },

  // 删除单条消息只允许末条，避免删除中间上下文后产生难以解释的历史断裂。
  removeLastMessage: async (id: string, sessionId: string): Promise<{ removed: true }> => {
    await requireSession(sessionId);
    const [rows] = await requirePool().query<MessageRow[]>(
      `${messageSelect} WHERE session_id=? ORDER BY created_at DESC,id DESC LIMIT 1`,
      [sessionId],
    );
    if (!rows[0] || rows[0].id !== id)
      throw Object.assign(new Error("只能删除会话最后一条消息"), { code: "CONFLICT" });
    await requirePool().query("DELETE FROM ai_message WHERE id=? AND session_id=?", [id, sessionId]);
    return { removed: true };
  },

  // 清空只删除消息，保留会话本身，便于用户继续沿用当前助手配置。
  clearSession: async (sessionId: string): Promise<{ cleared: true }> => {
    await requireSession(sessionId);
    await requirePool().query("DELETE FROM ai_message WHERE session_id=?", [sessionId]);
    await requirePool().query("UPDATE ai_session SET title='新会话',updated_at=? WHERE id=?", [utcNow(), sessionId]);
    return { cleared: true };
  },

  // 置顶状态只更新会话自身，不修改更新时间，避免置顶操作扰乱活跃排序。
  pinSession: async (id: string, pinned: boolean): Promise<SessionRow> => {
    await requireSession(id);
    await requirePool().query("UPDATE ai_session SET pinned=? WHERE id=?", [pinned ? 1 : 0, id]);
    return requireSession(id);
  },

  // 导出由主进程写入受控 exports 目录，推理文本以引用块附带且不改变数据库内容。
  exportSessionMarkdown: async (id: string): Promise<{ path: string }> => {
    const session = await requireSession(id);
    const result = await assistantService.messages(id, 200, 0);
    const paths = await applicationPaths();
    const safeTitle = session.title.replace(/[\\/:*?"<>|]/g, "_").slice(0, 80) || "会话";
    const stamp = new Date().toISOString().replace(/[-:.TZ]/g, "").slice(0, 14);
    const lines = [`# ${session.title}`, ""];
    for (const message of result.messages) {
      lines.push(`## ${message.role === "user" ? "用户" : "助手"}`, "", message.content, "");
      if (message.reasoning) {
        lines.push("> 深度思考", ...message.reasoning.split(/\r?\n/).map((line) => `> ${line}`), "");
      }
    }
    const path = join(paths.exports, `${safeTitle}-${stamp}.md`);
    await writeFile(path, lines.join("\n"), "utf8");
    return { path };
  },

  // ChatService 只通过该上下文入口读取助手提示词与最近历史，绝不查询情绪数据。
  context: async (
    sessionId: string,
    limit: number,
  ): Promise<{ session: SessionRow; assistant: AssistantRow; messages: ChatMessage[] }> => {
    const session = await requireSession(sessionId);
    const assistant = await requireAssistant(session.assistantId);
    const history = await assistantService.messages(sessionId, limit, 0);
    return {
      session,
      assistant,
      messages: history.messages.map((message) => ({
        role: message.role,
        content: message.content,
      })),
    };
  },
};
