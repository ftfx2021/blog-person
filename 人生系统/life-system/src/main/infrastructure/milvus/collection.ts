import {
  DataType,
  MetricType,
  type MilvusClient,
} from "@zilliz/milvus2-sdk-node";
import { vectorUnavailable } from "./client.js";

export const MILVUS_COLLECTION = "knowledge_chunk_v2";
export const LEGACY_MILVUS_COLLECTION = "knowledge_chunk_v1";

// 读取 embedding 字段的维度，兼容 SDK 返回 type_params 数组或扁平 dim 字段。
export function collectionDim(description: any): number | undefined {
  const field =
    description?.schema?.fields?.find(
      (item: any) => item.name === "embedding",
    ) ?? description?.fields?.find((item: any) => item.name === "embedding");
  const params = field?.type_params ?? field?.typeParams;
  const pair = Array.isArray(params)
    ? params.find((item: any) => item.key === "dim")
    : undefined;
  const dim =
    field?.dim ??
    pair?.value ??
    (params && !Array.isArray(params) ? (params as any).dim : undefined);
  return dim === undefined ? undefined : Number(dim);
}

// 确保固定 collection 存在并加载；维度不一致时只返回实际值，绝不自动重建。
export async function ensureCollection(
  client: MilvusClient,
  dim: number,
  collectionName = MILVUS_COLLECTION,
): Promise<{ created: boolean; dim: number }> {
  try {
    const described = await client.describeCollection({
      collection_name: collectionName,
    });
    const actual = collectionDim(described);
    if (actual) {
      // 已存在的集合也要加载，否则后续 search/query 在 Milvus 服务端会被拒绝。
      await client.loadCollection({ collection_name: collectionName });
      return { created: false, dim: actual };
    }
  } catch {
    // describe 对不存在 collection 通常返回错误，下面尝试创建并由 SDK 决定最终状态。
  }
  try {
    await client.createCollection({
      collection_name: collectionName,
      fields: [
        {
          name: "chunk_id",
          data_type: DataType.VarChar,
          max_length: 36,
          is_primary_key: true,
        },
        { name: "document_id", data_type: DataType.VarChar, max_length: 36 },
        { name: "kb_id", data_type: DataType.VarChar, max_length: 36 },
        { name: "folder_id", data_type: DataType.VarChar, max_length: 36 },
        { name: "model_version", data_type: DataType.VarChar, max_length: 128 },
        { name: "embedding", data_type: DataType.FloatVector, dim },
      ],
      enable_dynamic_field: false,
    } as any);
    await client.createIndex({
      collection_name: collectionName,
      field_name: "embedding",
      index_type: "HNSW",
      metric_type: MetricType.COSINE,
      params: { M: 16, efConstruction: 200 },
    } as any);
    await client.loadCollection({ collection_name: collectionName });
    return { created: true, dim };
  } catch (error) {
    throw vectorUnavailable(
      error instanceof Error
        ? "无法创建 Milvus 向量集合"
        : "无法创建 Milvus 向量集合",
    );
  }
}

export async function describeCollection(
  client: MilvusClient,
  collectionName = MILVUS_COLLECTION,
): Promise<{ exists: boolean; dim?: number; rowCount?: number }> {
  try {
    const described = await client.describeCollection({
      collection_name: collectionName,
    });
    const dim = collectionDim(described);
    const count = await client.count({ collection_name: collectionName });
    return { exists: true, dim, rowCount: Number((count as any).data ?? 0) };
  } catch {
    return { exists: false };
  }
}
