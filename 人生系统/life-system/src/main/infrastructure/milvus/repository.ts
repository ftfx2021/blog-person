import type { MilvusClient } from "@zilliz/milvus2-sdk-node";
import { MetricType } from "@zilliz/milvus2-sdk-node";
import { closeMilvus, vectorUnavailable } from "./client.js";
import {
  describeCollection,
  ensureCollection,
  MILVUS_COLLECTION,
} from "./collection.js";
import type {
  MilvusChunkRecord,
  MilvusHealthResult,
  MilvusRepository,
} from "./types.js";

function mapRow(row: any): MilvusChunkRecord {
  return {
    chunkId: String(row.chunk_id),
    documentId: String(row.document_id),
    kbId: String(row.kb_id ?? "00000000-0000-0000-0000-000000000001"),
    folderId: row.folder_id ? String(row.folder_id) : null,
    modelVersion: String(row.model_version),
    embedding: Array.isArray(row.embedding) ? row.embedding.map(Number) : [],
  };
}
function quote(value: string): string {
  return `"${value.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
}

// 将固定 collection 的 CRUD 收敛到主进程唯一端口，后续备份和检索复用同一套映射。
export function createMilvusRepository(client: MilvusClient, collectionName = MILVUS_COLLECTION): MilvusRepository {
  const run = async <T>(operation: () => Promise<T>): Promise<T> => {
    try {
      return await operation();
    } catch (error) {
      throw vectorUnavailable(
        error instanceof Error
          ? "Milvus 操作失败，请检查服务状态"
          : "Milvus 操作失败，请检查服务状态",
      );
    }
  };
  const upsert = async (records: MilvusChunkRecord[]) => {
    for (let index = 0; index < records.length; index += 500) {
      const batch = records.slice(index, index + 500);
      await run(() =>
        client.upsert({
          collection_name: collectionName,
          data: batch.map((item) => ({
            chunk_id: item.chunkId,
            document_id: item.documentId,
            kb_id: item.kbId,
            folder_id: item.folderId ?? "",
            model_version: item.modelVersion,
            embedding: item.embedding,
          })),
        }),
      );
    }
  };
  return {
    ensureCollection: (dim) => run(() => ensureCollection(client, dim, collectionName)),
    upsertChunks: upsert,
    queryByDocumentId: (documentId) =>
      run(async () => {
        const result: any = await client.query({
          collection_name: collectionName,
          filter: `document_id == ${quote(documentId)}`,
          output_fields: [
            "chunk_id",
            "document_id",
            "kb_id",
            "folder_id",
            "model_version",
            "embedding",
          ],
          limit: 16384,
        });
        return (result.data ?? []).map(mapRow);
      }),
    search: (vector, topK, filter) =>
      run(async () => {
        const result: any = await client.search({
          collection_name: collectionName,
          data: [vector],
          anns_field: "embedding",
          limit: topK,
          output_fields: ["chunk_id"],
          search_params: { metric_type: MetricType.COSINE, params: { ef: 64 } },
          ...(filter ? { expr: filter } : {}),
        });
        const rows = result.results ?? result.data ?? [];
        return rows.map((row: any) => ({
          chunkId: String(row.chunk_id ?? row.id),
          score: Number(row.score ?? row.distance ?? row["distance"] ?? 0),
        }));
      }),
    count: () =>
      run(async () =>
        Number(
          ((await client.count({ collection_name: collectionName })) as any)
            .data ?? 0,
        ),
      ),
    deleteByDocumentId: (documentId) =>
      run(async () => {
        await client.delete({
          collection_name: collectionName,
          filter: `document_id == ${quote(documentId)}`,
        });
      }),
    exportAll: () =>
      run(async () => {
        const all: MilvusChunkRecord[] = [];
        let cursor = "";
        while (true) {
          const filter = cursor
            ? `chunk_id > ${quote(cursor)}`
            : 'chunk_id != ""';
          const result: any = await client.query({
            collection_name: collectionName,
            filter,
            output_fields: [
              "chunk_id",
              "document_id",
              "kb_id",
              "folder_id",
              "model_version",
              "embedding",
            ],
            limit: 10000,
          });
          const page = (result.data ?? []).map(mapRow);
          if (!page.length) break;
          all.push(...page);
          cursor = page[page.length - 1]!.chunkId;
          if (page.length < 10000) break;
        }
        return all;
      }),
    importAll: upsert,
    dropAndRecreate: (dim) =>
      run(async () => {
        try {
          await client.releaseCollection({
            collection_name: collectionName,
          });
        } catch {
          /* 未加载时无需释放。 */
        }
        try {
          await client.dropCollection({ collection_name: collectionName });
        } catch {
          /* 不存在时直接创建。 */
        }
        await ensureCollection(client, dim, collectionName);
      }),
    health: () =>
      run(async (): Promise<MilvusHealthResult> => {
        const started = Date.now();
        const health: any = await client.checkHealth();
        const collection = await describeCollection(client, collectionName);
        let version: string | undefined;
        try {
          version = String(((await client.getVersion()) as any).version);
        } catch {
          /* 版本接口失败不影响健康结果。 */
        }
        return {
          ok: health.isHealthy !== false,
          detail:
            health.isHealthy === false ? "Milvus 健康检查未通过" : "连接正常",
          version,
          latencyMs: Date.now() - started,
          collection,
        };
      }),
  };
}

export { closeMilvus };
