import { createHash, randomUUID } from "node:crypto";
import { access, copyFile, writeFile } from "node:fs/promises";
import { basename, join } from "node:path";
import { clipboard, dialog, shell } from "electron";
import { requirePool } from "../../infrastructure/db/pool.js";
import { inTransaction } from "../../infrastructure/db/transaction.js";
import { applicationPaths } from "../../infrastructure/filesystem/paths.js";
import { utcNow } from "../common/database.js";
import { knowledgeService, type KnowledgeDocType } from "./index.js";
import { validateOwnership } from "./kb.js";
import { closeMilvus, connectMilvus, createMilvusRepository } from "../../infrastructure/milvus/index.js";
import { settingsService } from "../settings/service.js";

function notFound(): never { throw Object.assign(new Error("文档不存在"), { code: "NOT_FOUND" }); }
async function getRow(id: string): Promise<any> { const [rows] = await requirePool().query<any[]>("SELECT id,title,doc_type AS docType,kb_id AS kbId,folder_id AS folderId,source_url AS sourceUrl,source_path AS sourcePath,stored_path AS storedPath,raw_text AS rawText,parse_status AS parseStatus,index_status AS indexStatus,deleted_at AS deletedAt,created_at AS createdAt,updated_at AS updatedAt FROM document WHERE id=?", [id]); if (!rows[0]) notFound(); return rows[0]; }
async function tagsFor(id: string): Promise<string[]> {
  // 兼容运行中的旧数据库；重启后 005 迁移会创建表并恢复完整标签能力。
  try {
    const [rows] = await requirePool().query<any[]>(
      "SELECT t.name FROM tag t JOIN entity_tag et ON et.tag_id=t.id WHERE et.entity_type='document' AND et.entity_id=? ORDER BY t.name",
      [id],
    );
    return rows.map((row) => row.name);
  } catch (error: any) {
    if (error?.code === "ER_NO_SUCH_TABLE") return [];
    throw error;
  }
}

export const documentsService = {
  async list(input: { docType?: KnowledgeDocType; tag?: string; keyword?: string; kbId?: string; folderId?: string; includeDeleted?: boolean } = {}) {
    const conditions = [input.includeDeleted ? "d.deleted_at IS NOT NULL" : "d.deleted_at IS NULL"];
    const values: string[] = [];
    if (input.docType) { conditions.push("d.doc_type=?"); values.push(input.docType); }
    if (input.keyword) { conditions.push("(d.title LIKE ? OR d.raw_text LIKE ?)"); values.push(`%${input.keyword}%`, `%${input.keyword}%`); }
    if (input.tag) { conditions.push("EXISTS (SELECT 1 FROM entity_tag et JOIN tag t ON t.id=et.tag_id WHERE et.entity_type='document' AND et.entity_id=d.id AND t.name=?)"); values.push(input.tag); }
    if (input.folderId) {
      // 只传文件夹时由文件夹反查所属知识库；同时传入时仍由服务校验一致性。
      if (input.kbId) {
        const folder = await validateOwnership(input.kbId, input.folderId);
        conditions.push("d.kb_id=? AND d.folder_id=?");
        values.push(folder.kbId, folder.folderId!);
      } else {
        const [folders] = await requirePool().query<any[]>("SELECT kb_id AS kbId FROM knowledge_folder WHERE id=?", [input.folderId]);
        if (!folders[0]) throw Object.assign(new Error("文件夹不存在"), { code: "INVALID_STATE" });
        conditions.push("d.kb_id=? AND d.folder_id=?");
        values.push(folders[0].kbId, input.folderId);
      }
    } else if (input.kbId) { conditions.push("d.kb_id=?"); values.push(input.kbId); }
    const [rows] = await requirePool().query<any[]>(`SELECT d.id,d.title,d.doc_type AS docType,d.kb_id AS kbId,d.folder_id AS folderId,d.parse_status AS parseStatus,d.index_status AS indexStatus,d.deleted_at AS deletedAt,d.updated_at AS updatedAt FROM document d WHERE ${conditions.join(" AND ")} ORDER BY d.updated_at DESC`, values);
    return Promise.all(rows.map(async (row) => ({ ...row, tags: await tagsFor(row.id) })));
  },
  async get(id: string) { const row = await getRow(id); return { ...row, tags: await tagsFor(id) }; },
  async createNote(input: { title: string; rawText: string; docType: "note" | "skill" | "prompt"; tags?: string[]; kbId?: string; folderId?: string | null }) { const result = await knowledgeService.ingest(input); return this.get(result.documentId); },
  async update(id: string, input: { title?: string; rawText?: string; docType?: KnowledgeDocType; tags?: string[] }) { const row = await getRow(id); if (row.deletedAt) throw Object.assign(new Error("已删除文档不能编辑"), { code: "INVALID_STATE" }); if (input.rawText !== undefined || input.docType !== undefined) await knowledgeService.ingest({ documentId: id, title: input.title ?? row.title, rawText: input.rawText ?? row.rawText, docType: input.docType ?? row.docType, tags: input.tags ?? await tagsFor(id), sourceUrl: row.sourceUrl ?? undefined, sourcePath: row.sourcePath ?? undefined, storedPath: row.storedPath ?? undefined, kbId: row.kbId ?? undefined, folderId: row.folderId ?? null }); else if (input.title) await requirePool().query("UPDATE document SET title=?,updated_at=? WHERE id=?", [input.title.trim(), utcNow(), id]); return this.get(id); },
  async remove(id: string) { await getRow(id); await requirePool().query("UPDATE document SET deleted_at=?,updated_at=? WHERE id=?", [utcNow(), utcNow(), id]); return { removed: true }; },
  async restore(id: string) { await getRow(id); await requirePool().query("UPDATE document SET deleted_at=NULL,index_status='stale',updated_at=? WHERE id=?", [utcNow(), id]); return this.get(id); },
  tags: tagsFor,
  async setTags(id: string, tags: string[]) { const row = await getRow(id); return this.update(id, { title: row.title, tags }); },
  async export(id: string, format: "markdown" | "txt" | "pdf") { const row = await getRow(id); if (!row.rawText) throw new Error("空文档不能导出"); const paths = await applicationPaths(); const extension = format === "pdf" ? "html" : format === "markdown" ? "md" : "txt"; const content = format === "pdf" ? `<html><body><pre>${row.rawText.replace(/[<&>]/g, (value: string) => ({ "<": "&lt;", ">": "&gt;", "&": "&amp;" })[value]!)}</pre></body></html>` : row.rawText; const path = join(paths.exports, `${row.title.replace(/[\\/:*?"<>|]/g, "_").slice(0, 80) || randomUUID()}.${extension}`); await writeFile(path, content, "utf8"); return { path, hash: createHash("sha256").update(content).digest("hex") }; },
  async copyPrompt(id: string) { const row = await getRow(id); if (row.docType !== "prompt") throw Object.assign(new Error("只有 Prompt 文档可以复制"), { code: "VALIDATION_ERROR" }); clipboard.writeText(row.rawText); return { copied: true }; },
  async retryParse(id: string) { const row = await getRow(id); const result = await knowledgeService.ingest({ documentId: id, title: row.title, docType: row.docType, sourceUrl: row.sourceUrl ?? undefined, sourcePath: row.sourcePath ?? undefined, storedPath: row.storedPath ?? undefined, kbId: row.kbId ?? undefined, folderId: row.folderId ?? null }); return this.get(result.documentId); },
  async retryIndex(id: string) { await getRow(id); await requirePool().query("UPDATE document SET index_status='pending',updated_at=? WHERE id=?", [utcNow(), id]); return this.get(id); },
  async moveDocument(input: { id: string; kbId: string; folderId?: string | null }) {
    // MySQL 事务先更新权威归属；Milvus 失败时可安全重试同一目标移动。
    const target = await validateOwnership(input.kbId, input.folderId);
    const row = await getRow(input.id);
    await inTransaction(async (connection) => {
      await connection.query("UPDATE document SET kb_id=?,folder_id=?,updated_at=? WHERE id=?", [target.kbId, target.folderId, utcNow(), input.id]);
    });
    const settings = await settingsService.getMilvusConnection();
    if (settings) {
      const client = await connectMilvus(settings);
      try {
        const storage = await settingsService.getKnowledgeStorage();
        const repository = createMilvusRepository(client, storage.collectionName);
        const records = await repository.queryByDocumentId(input.id);
        if (records.length) {
          await repository.deleteByDocumentId(input.id);
          await repository.upsertChunks(records.map((item) => ({ ...item, kbId: target.kbId, folderId: target.folderId })));
        }
      } finally { closeMilvus(client); }
    }
    return { id: row.id, kbId: target.kbId, folderId: target.folderId };
  },
  async openOriginal(id: string): Promise<{ opened: boolean }> {
    // 原件路径只从数据库读取，渲染层永远不能指定系统路径。
    const row = await getRow(id);
    if (!row.storedPath) throw Object.assign(new Error("该文档没有原件"), { code: "NOT_FOUND" });
    try { await access(row.storedPath); } catch { throw Object.assign(new Error("原件文件不存在"), { code: "NOT_FOUND" }); }
    const error = await shell.openPath(row.storedPath);
    if (error) throw Object.assign(new Error(`无法打开原件: ${error}`), { code: "FILESYSTEM_ERROR" });
    return { opened: true };
  },
  async saveOriginal(id: string): Promise<{ path: string } | null> {
    // 另存先让系统对话框选择目标，取消操作不被视为失败。
    const row = await getRow(id);
    if (!row.storedPath) throw Object.assign(new Error("该文档没有原件"), { code: "NOT_FOUND" });
    try { await access(row.storedPath); } catch { throw Object.assign(new Error("原件文件不存在"), { code: "NOT_FOUND" }); }
    const result = await dialog.showSaveDialog({ defaultPath: basename(row.storedPath), filters: [{ name: "全部文件", extensions: ["*"] }] });
    if (result.canceled || !result.filePath) return null;
    await copyFile(row.storedPath, result.filePath);
    return { path: result.filePath };
  },
};
