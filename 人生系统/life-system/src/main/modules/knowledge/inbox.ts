import { randomUUID } from "node:crypto";
import type { PoolConnection, RowDataPacket } from "mysql2/promise";
import { requirePool } from "../../infrastructure/db/pool.js";
import { inTransaction } from "../../infrastructure/db/transaction.js";
import { utcNow } from "../common/database.js";
import { knowledgeService } from "./index.js";

type InboxKind = "link" | "snippet" | "read_later";
type InboxStatus = "pending" | "clipped" | "bookmarked" | "discarded";

export interface InboxItemRow extends RowDataPacket {
  id: string;
  kind: InboxKind;
  url: string | null;
  title: string;
  note: string | null;
  status: InboxStatus;
  document_id: string | null;
  created_at: string;
  updated_at: string;
}

interface InboxInput {
  kind: InboxKind;
  title: string;
  url?: string;
  note?: string;
}

interface InboxUpdateInput {
  id: string;
  kind?: InboxKind;
  title?: string;
  url?: string;
  note?: string;
}

interface InboxListInput {
  kind?: InboxKind;
  status?: InboxStatus;
}

function validation(message: string): never {
  // 业务层把跨字段约束转换成统一错误码，IPC 才能给出稳定表单提示。
  throw Object.assign(new Error(message), { code: "VALIDATION_ERROR" });
}

function ensureUrl(kind: InboxKind, url: string | null): void {
  // 链接和稍后读必须是真实 HTTP(S) 地址，snippet 则允许没有 URL。
  if (kind === "snippet") return;
  if (!url) validation("链接和稍后读必须填写 URL");
  try {
    const parsed = new URL(url);
    if (parsed.protocol !== "http:" && parsed.protocol !== "https:")
      validation("URL 仅支持 http 或 https 协议");
  } catch {
    validation("请输入合法的 http/https URL");
  }
}

function validateFields(
  kind: InboxKind,
  title: string,
  url: string | null,
  note: string | null,
): void {
  // 服务层独立校验长度，防止绕过 preload 的调用方直接触发数据库写入。
  if (!title.trim() || title.trim().length > 200)
    validation("标题不能为空且不能超过 200 字");
  if (note && note.length > 2000) validation("备注不能超过 2000 字");
  ensureUrl(kind, url);
  // 片段没有来源 URL 时必须保留正文，否则剪藏后没有可入库内容。
  if (kind === "snippet" && !url && !note)
    validation("片段没有 URL 时必须填写备注");
}

async function findById(
  connection: PoolConnection,
  id: string,
  lock = false,
): Promise<InboxItemRow> {
  // 状态动作使用行锁读取，保证判断和更新不会被并发请求穿透。
  const suffix = lock ? " FOR UPDATE" : "";
  const [rows] = await connection.query<InboxItemRow[]>(
    `SELECT id, kind, url, title, note, status, document_id, created_at, updated_at FROM inbox_item WHERE id = ?${suffix}`,
    [id],
  );
  if (!rows[0])
    throw Object.assign(new Error("收藏不存在"), { code: "NOT_FOUND" });
  return rows[0];
}

export const inboxService = {
  async list(input: InboxListInput): Promise<InboxItemRow[]> {
    // 列表查询只拼接受控筛选片段，并按创建时间倒序呈现最近收藏。
    const conditions: string[] = [];
    const values: string[] = [];
    if (input.kind) {
      conditions.push("kind = ?");
      values.push(input.kind);
    }
    if (input.status) {
      conditions.push("status = ?");
      values.push(input.status);
    }
    const where = conditions.length ? ` WHERE ${conditions.join(" AND ")}` : "";
    const [rows] = await requirePool().query<InboxItemRow[]>(
      `SELECT id, kind, url, title, note, status, document_id, created_at, updated_at FROM inbox_item${where} ORDER BY created_at DESC`,
      values,
    );
    return rows;
  },

  async create(input: InboxInput): Promise<InboxItemRow> {
    // 统一清理可选文本，数据库用 NULL 表示未填写而不是空字符串。
    const title = input.title.trim();
    const url = input.url?.trim() || null;
    const note = input.note?.trim() || null;
    validateFields(input.kind, title, url, note);
    const id = randomUUID();
    const now = utcNow();
    return inTransaction(async (connection) => {
      // 插入与回读放在同一事务中，调用方只会收到已经确认写入的完整 DTO。
      await connection.query(
        "INSERT INTO inbox_item (id, kind, url, title, note, status, document_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'pending', NULL, ?, ?)",
        [id, input.kind, url, title, note, now, now],
      );
      return findById(connection, id);
    });
  },

  async get(id: string): Promise<InboxItemRow> {
    // 单条读取复用统一查询逻辑，找不到时返回稳定 NOT_FOUND 错误。
    return inTransaction((connection) => findById(connection, id));
  },

  async update(input: InboxUpdateInput): Promise<InboxItemRow> {
    return inTransaction(async (connection) => {
      // 先锁住原记录，再把部分更新字段合并成最终值进行完整校验。
      const current = await findById(connection, input.id, true);
      if (
        input.kind === undefined &&
        input.title === undefined &&
        input.url === undefined &&
        input.note === undefined
      )
        validation("至少填写一个要修改的字段");
      const kind = input.kind ?? current.kind;
      const title =
        input.title === undefined ? current.title : input.title.trim();
      const url =
        input.url === undefined ? current.url : input.url.trim() || null;
      const note =
        input.note === undefined ? current.note : input.note.trim() || null;
      validateFields(kind, title, url, note);
      // 编辑只更新收藏自身字段，保留 clipped 状态和既有 document_id 关联。
      await connection.query(
        "UPDATE inbox_item SET kind=?, url=?, title=?, note=?, updated_at=? WHERE id=?",
        [kind, url, title, note, utcNow(), input.id],
      );
      return findById(connection, input.id);
    });
  },

  async clip(input: { id: string }): Promise<{ documentId: string }> {
    return inTransaction(async (connection) => {
      // 剪藏前锁定状态，允许 pending/bookmarked 重试，已入库记录明确返回冲突。
      const item = await findById(connection, input.id, true);
      if (item.status === "clipped")
        throw Object.assign(new Error("收藏已经剪藏入库"), {
          code: "CONFLICT",
        });
      if (item.status === "discarded")
        throw Object.assign(new Error("已丢弃收藏不能直接剪藏"), {
          code: "INVALID_STATE",
        });
      const document = await knowledgeService.clipToDocument(
        item.kind === "snippet"
          ? {
              kind: "snippet",
              title: item.title,
              note: item.note ?? undefined,
              content: item.note || item.title,
            }
          : {
              kind: item.kind,
              url: item.url ?? undefined,
              title: item.title,
              note: item.note ?? undefined,
            },
      );
      // 只有端口成功返回文档 ID 才提交 clipped 状态，失败会由事务回滚并保留 pending/bookmarked。
      await connection.query(
        "UPDATE inbox_item SET status='clipped', document_id=?, updated_at=? WHERE id=?",
        [document.documentId, utcNow(), input.id],
      );
      return document;
    });
  },

  async keep(input: { id: string }): Promise<InboxItemRow> {
    return inTransaction(async (connection) => {
      // 仅链接允许标记为 bookmarked，片段没有可单独保留的 URL。
      const item = await findById(connection, input.id, true);
      if (item.kind === "snippet") validation("片段不能仅保留链接");
      if (item.status === "clipped" || item.status === "discarded")
        throw Object.assign(new Error("当前状态不能仅保留链接"), {
          code: "INVALID_STATE",
        });
      await connection.query(
        "UPDATE inbox_item SET status='bookmarked', updated_at=? WHERE id=?",
        [utcNow(), input.id],
      );
      return findById(connection, input.id);
    });
  },

  async discard(input: { id: string }): Promise<InboxItemRow> {
    return inTransaction(async (connection) => {
      // 丢弃只是隐藏收藏，不删除记录或其可能存在的文档关联。
      const item = await findById(connection, input.id, true);
      if (item.status === "clipped")
        throw Object.assign(new Error("已入库收藏不能丢弃，请先处理关联文档"), {
          code: "INVALID_STATE",
        });
      if (item.status !== "pending" && item.status !== "bookmarked")
        throw Object.assign(new Error("当前状态不能丢弃"), {
          code: "INVALID_STATE",
        });
      await connection.query(
        "UPDATE inbox_item SET status='discarded', updated_at=? WHERE id=?",
        [utcNow(), input.id],
      );
      return findById(connection, input.id);
    });
  },

  async restore(input: { id: string }): Promise<InboxItemRow> {
    return inTransaction(async (connection) => {
      // 恢复只接受 discarded，且不触碰已有 document_id，保证历史关联仍可追溯。
      const item = await findById(connection, input.id, true);
      if (item.status !== "discarded")
        throw Object.assign(new Error("只有已丢弃收藏可以恢复"), {
          code: "INVALID_STATE",
        });
      await connection.query(
        "UPDATE inbox_item SET status='pending', updated_at=? WHERE id=?",
        [utcNow(), input.id],
      );
      return findById(connection, input.id);
    });
  },
};
