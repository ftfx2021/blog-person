import type { MilvusSettings } from "../../../shared/contracts/system.js";

// chunk 向量记录与 MySQL chunk 表通过 UUID 主键关联，严禁混入情绪等业务字段。
export interface MilvusChunkRecord {
  chunkId: string;
  documentId: string;
  kbId: string;
  folderId: string | null;
  modelVersion: string;
  embedding: number[];
}

// 健康结果只携带面向用户的摘要，避免把服务端堆栈跨进程暴露。
export interface MilvusHealthResult {
  ok: boolean;
  detail: string;
  version?: string;
  latencyMs?: number;
  collection: {
    exists: boolean;
    dim?: number;
    rowCount?: number;
    expectedDim?: number;
  } | null;
}

export interface MilvusRepository {
  ensureCollection(dim: number): Promise<{ created: boolean; dim: number }>;
  upsertChunks(records: MilvusChunkRecord[]): Promise<void>;
  queryByDocumentId(documentId: string): Promise<MilvusChunkRecord[]>;
  search(
    vector: number[],
    topK: number, filter?: string,
  ): Promise<Array<{ chunkId: string; score: number }>>;
  count(): Promise<number>;
  deleteByDocumentId(documentId: string): Promise<void>;
  exportAll(): Promise<MilvusChunkRecord[]>;
  importAll(records: MilvusChunkRecord[]): Promise<void>;
  dropAndRecreate(dim: number): Promise<void>;
  health(): Promise<MilvusHealthResult>;
}

export type { MilvusSettings };
