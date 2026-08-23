import { copyFile, stat, writeFile } from "node:fs/promises";
import { basename, join } from "node:path";
import { randomUUID } from "node:crypto";
import { BrowserWindow, session } from "electron";
import { requirePool } from "../../infrastructure/db/pool.js";
import { inTransaction } from "../../infrastructure/db/transaction.js";
import { applicationPaths } from "../../infrastructure/filesystem/paths.js";
import { createParser, docTypeFromPath } from "../../infrastructure/parser/index.js";
import { utcNow } from "../common/database.js";
import { chunkDocument } from "./chunk.js";
import { normalizeDocument } from "./normalize.js";
import { settingsService } from "../settings/service.js";
import { recognizePdf } from "../../infrastructure/ocr/index.js";
import { validateOwnership } from "./kb.js";

const docTypes = ["webpage", "pdf", "docx", "markdown", "txt", "html", "note", "skill", "prompt", "promoted"] as const;
export type KnowledgeDocType = (typeof docTypes)[number];
export interface ClipToDocumentInput { kind: "link" | "read_later" | "snippet"; url?: string; title: string; note?: string; content?: string; kbId?: string; folderId?: string | null; }
export interface IngestInput { title: string; docType: KnowledgeDocType; sourceUrl?: string; sourcePath?: string; storedPath?: string; rawText?: string; content?: string; tags?: string[]; documentId?: string; kbId?: string; folderId?: string | null; promotedFromTimelineId?: string; }
export interface IngestResult { documentId: string; chunkCount: number; parseStatus: "ready"; indexStatus: "pending"; }
function message(error: unknown): string { return error instanceof Error ? error.message.slice(0, 500) : "文档解析失败"; }
function assertInput(input: IngestInput): void { if (!docTypes.includes(input.docType) || !input.title.trim()) throw Object.assign(new Error("文档类型或标题无效"), { code: "VALIDATION_ERROR" }); if (!input.rawText && !input.content && !input.storedPath && !input.sourcePath && !input.sourceUrl) throw Object.assign(new Error("文档必须包含正文、文件或网页来源"), { code: "VALIDATION_ERROR" }); }
async function fetchWebpageChromium(
  url: string,
  timeoutSeconds: number,
): Promise<string> {
  // URL 白名单在创建窗口前执行，阻止 file/data 等本地协议被加载。
  const parsed = new URL(url);
  if (parsed.protocol !== "http:" && parsed.protocol !== "https:")
    throw Object.assign(new Error("URL 仅支持 http 或 https 协议"), { code: "VALIDATION_ERROR" });
  // 每次使用隔离 partition，第三方页面无法读写应用默认会话或其他抓取的 cookie。
  const isolatedSession = session.fromPartition(`webpage-fetch-${randomUUID()}`);
  const window = new BrowserWindow({
    show: false,
    webPreferences: {
      session: isolatedSession,
      sandbox: true,
      contextIsolation: true,
      nodeIntegration: false,
    },
  });
  window.webContents.setWindowOpenHandler(() => ({ action: "deny" }));
  // 下载事件属于 session 而非 WebContents，统一取消所有由该临时会话发起的下载。
  isolatedSession.on("will-download", (event) => event.preventDefault());
  try {
    // Electron 的 loadURL 类型未提供 timeout，使用 Promise.race 实现同等取消语义。
    await Promise.race([
      window.loadURL(url),
      new Promise<never>((_, reject) => setTimeout(() => reject(new Error("网页加载超时")), timeoutSeconds * 1000)),
    ]);
    // 页面 load 后短暂轮询正文，兼容简单的客户端渲染页面而不开放自定义等待脚本。
    const started = Date.now();
    let html = "";
    while (Date.now() - started < 5000) {
      html = await window.webContents.executeJavaScript(
        "document.body && document.body.innerText.trim() ? document.documentElement.outerHTML : ''",
      );
      if (html.trim().length >= 50) break;
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
    if (!html || html.trim().length < 50)
      throw new Error("页面未渲染出内容");
    if (/\b(403|access denied|forbidden|拒绝访问)\b/i.test(html))
      throw new Error("网站拒绝访问");
    return html;
  } catch (error) {
    throw Object.assign(
      new Error(`网页抓取失败：${message(error)}；网站拒绝访问时可保存网页后导入文件`),
      { code: "FILESYSTEM_ERROR" },
    );
  } finally {
    // 先销毁窗口再清理临时 session，避免隐藏窗口和 cookie 在主进程常驻。
    if (!window.isDestroyed()) window.destroy();
    await isolatedSession.clearStorageData().catch(() => undefined);
  }
}
async function updateTags(documentId: string, tags: string[]): Promise<void> { const names = [...new Set(tags.map((tag) => tag.trim()).filter(Boolean))].slice(0, 30); await inTransaction(async (connection) => { await connection.query("DELETE FROM entity_tag WHERE entity_type='document' AND entity_id=?", [documentId]); for (const name of names) { const id = randomUUID(); await connection.query("INSERT INTO tag (id,name,created_at) VALUES (?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name)", [id, name, utcNow()]); const [rows] = await connection.query<any[]>("SELECT id FROM tag WHERE name=?", [name]); await connection.query("INSERT IGNORE INTO entity_tag (entity_type,entity_id,tag_id) VALUES ('document',?,?)", [documentId, rows[0].id]); } }); }

export const knowledgeService = {
  async ingest(input: IngestInput): Promise<IngestResult> {
    // 唯一入库管线先完成采集，再创建 pending 壳，失败时始终保留来源与原件。
    assertInput(input);
    const documentId = input.documentId ?? randomUUID();
    const editing = Boolean(input.documentId);
    const ownership = await validateOwnership(input.kbId, input.folderId);
    const importSettings = await settingsService.getKnowledgeImport();
    let storedPath = input.storedPath;
    try {
      if (input.sourcePath && !storedPath) { const paths = await applicationPaths(); const source = await stat(input.sourcePath); if (source.size > importSettings.maxFileSizeMb * 1024 * 1024) throw new Error(`文件超过当前 ${importSettings.maxFileSizeMb}MB 导入上限`); storedPath = join(paths.documents, `${documentId}-${basename(input.sourcePath)}`); await copyFile(input.sourcePath, storedPath); }
    } catch (error) { throw Object.assign(new Error(message(error)), { code: "FILESYSTEM_ERROR" }); }
    if (!editing) await requirePool().query("INSERT INTO document (id,kb_id,folder_id,title,doc_type,source_url,source_path,stored_path,raw_text,parse_status,index_status,promoted_from_timeline_id,deleted_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,'','pending','pending',?,NULL,?,?)", [documentId, ownership.kbId, ownership.folderId, input.title.trim(), input.docType, input.sourceUrl ?? null, input.sourcePath ?? null, storedPath ?? null, input.promotedFromTimelineId ?? null, utcNow(), utcNow()]);
    try {
      if (input.sourceUrl && !input.rawText && !input.content && !storedPath) { const paths = await applicationPaths(); storedPath = join(paths.documents, `${documentId}.html`); await writeFile(storedPath, await fetchWebpageChromium(input.sourceUrl, importSettings.parseTimeoutSeconds), "utf8"); }
      let rawText = input.rawText ?? input.content;
      let parsed: any;
      if (!rawText) {
        if (!storedPath) throw new Error("未找到可解析的原件");
        try {
          parsed = await createParser(input.docType).parse(storedPath);
          rawText = parsed.rawText;
        } catch (error: any) {
          // 仅扫描 PDF 的明确错误触发 OCR，其它解析失败仍沿用原失败路径。
          if (error?.code !== "OCR_REQUIRED" || input.docType !== "pdf") throw error;
          const ocr = await recognizePdf(storedPath, await settingsService.getOcr());
          rawText = ocr.text;
        }
      }
      const normalized = normalizeDocument(rawText!);
      const chunks = chunkDocument(normalized.rawText, { documentTitle: input.title.trim(), docType: input.docType, sourceUrl: input.sourceUrl, sourcePath: input.sourcePath, headingPath: parsed?.headingPath, pageLocations: parsed?.pageLocations });
      await inTransaction(async (connection) => { if (editing) await connection.query("DELETE FROM chunk WHERE document_id=?", [documentId]); await connection.query("UPDATE document SET kb_id=?,folder_id=?,title=?,doc_type=?,source_url=?,source_path=?,stored_path=?,raw_text=?,parse_status='ready',index_status='pending',updated_at=? WHERE id=?", [ownership.kbId, ownership.folderId, input.title.trim(), input.docType, input.sourceUrl ?? null, input.sourcePath ?? null, storedPath ?? null, normalized.rawText, utcNow(), documentId]); for (const chunk of chunks) await connection.query("INSERT INTO chunk (id,document_id,content,seq_no,token_count,metadata_json,content_hash,created_at) VALUES (?,?,?,?,?,?,?,?)", [randomUUID(), documentId, chunk.content, chunk.seqNo, chunk.tokenCount, JSON.stringify(chunk.metadata), chunk.contentHash, utcNow()]); });
      if (input.tags) await updateTags(documentId, input.tags);
      // TODO(P1-5): 索引任务在事务提交后调度；本期保持 pending 且不写 Milvus。
      return { documentId, chunkCount: chunks.length, parseStatus: "ready", indexStatus: "pending" };
    } catch (error) { if (!editing) await requirePool().query("UPDATE document SET parse_status='failed',updated_at=? WHERE id=?", [utcNow(), documentId]); throw Object.assign(new Error(message(error)), { code: "PARSE_FAILED", documentId }); }
  },
  async clipToDocument(input: ClipToDocumentInput): Promise<{ documentId: string }> { const result = await knowledgeService.ingest(input.kind === "snippet" ? { title: input.title, docType: "note", rawText: input.content ?? input.note ?? input.title, kbId: input.kbId, folderId: input.folderId } : { title: input.title, docType: "webpage", sourceUrl: input.url, kbId: input.kbId, folderId: input.folderId }); return { documentId: result.documentId }; },
  // 文件选择由 IPC 完成，归属参数只在这里补进既有统一入库管线。
  importFile: async (
    sourcePath: string,
    ownership: Pick<IngestInput, "kbId" | "folderId"> = {},
  ): Promise<IngestResult> => knowledgeService.ingest({
    title: basename(sourcePath),
    docType: docTypeFromPath(sourcePath) as KnowledgeDocType,
    sourcePath,
    ...ownership,
  }),
};
