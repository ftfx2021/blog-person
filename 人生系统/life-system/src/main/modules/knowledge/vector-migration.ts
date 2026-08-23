import { connectMilvus, closeMilvus, createMilvusRepository, LEGACY_MILVUS_COLLECTION } from "../../infrastructure/milvus/index.js";
import { requirePool } from "../../infrastructure/db/pool.js";
import { settingsService } from "../settings/service.js";
import { DEFAULT_KB_ID } from "./kb.js";

// 将 v1 导出的向量补充归属后写入 v2，整个过程不生成新 embedding。
export async function migrateVectorsV1toV2(): Promise<{ migrated: number }> {
  const settings = await settingsService.getMilvusConnection();
  if (!settings) throw Object.assign(new Error("未配置 Milvus"), { code: "VECTOR_DB_UNAVAILABLE" });
  const storage = await settingsService.getKnowledgeStorage();
  const client = await connectMilvus(settings);
  try {
    const legacy: any[] = [];
    let cursor = "";
    while (true) {
      const result: any = await client.query({
        collection_name: LEGACY_MILVUS_COLLECTION,
        filter: cursor ? `chunk_id > "${cursor.replace(/"/g, '\\"')}"` : 'chunk_id != ""',
        output_fields: ["chunk_id", "document_id", "model_version", "embedding"],
        limit: 10000,
      });
      const page = result.data ?? [];
      if (!page.length) break;
      legacy.push(...page);
      cursor = String(page[page.length - 1].chunk_id);
      if (page.length < 10000) break;
    }
    if (!legacy.length) return { migrated: 0 };
    const ids = [...new Set(legacy.map((row) => String(row.document_id)))];
    const ownership = new Map<string, { kbId: string; folderId: string | null }>();
    for (let index = 0; index < ids.length; index += 500) {
      const batch = ids.slice(index, index + 500);
      const [rows] = await requirePool().query<any[]>(`SELECT id,kb_id AS kbId,folder_id AS folderId FROM document WHERE id IN (${batch.map(() => "?").join(",")})`, batch);
      rows.forEach((row) => ownership.set(row.id, { kbId: row.kbId ?? DEFAULT_KB_ID, folderId: row.folderId ?? null }));
    }
    const repository = createMilvusRepository(client, storage.collectionName);
    // 迁移只允许写入空的 v2 collection，避免重复执行把行数校验变得不可判定。
    const current = await repository.health();
    if (current.collection?.exists && (current.collection.rowCount ?? 0) > 0)
      throw Object.assign(new Error("目标向量库已有数据，不能重复执行存量迁移"), { code: "INVALID_STATE" });
    await repository.ensureCollection(Array.isArray(legacy[0].embedding) ? legacy[0].embedding.length : 0);
    await repository.importAll(legacy.map((row) => ({ chunkId: String(row.chunk_id), documentId: String(row.document_id), modelVersion: String(row.model_version), embedding: row.embedding.map(Number), ...(ownership.get(String(row.document_id)) ?? { kbId: DEFAULT_KB_ID, folderId: null }) })));
    const count = await repository.count();
    if (count !== legacy.length) throw new Error(`向量迁移数量校验失败：v1=${legacy.length}，v2=${count}`);
    return { migrated: count };
  } finally { closeMilvus(client); }
}
