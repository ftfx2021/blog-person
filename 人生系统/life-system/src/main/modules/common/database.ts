import { randomUUID } from "node:crypto";
import type { PoolConnection, RowDataPacket } from "mysql2/promise";

export const utcNow = (): string =>
  new Date().toISOString().slice(0, 23).replace("T", " ");
export const toMysqlDateTime = (
  value: string | null | undefined,
): string | null =>
  value ? new Date(value).toISOString().slice(0, 23).replace("T", " ") : null;

export async function requireEntity(
  connection: PoolConnection,
  table: "goal" | "project" | "task" | "habit",
  id: string,
): Promise<any> {
  // 表名来自受控联合类型，绝不接受渲染层传入任意 SQL 标识符。
  const [rows] = await connection.query<RowDataPacket[]>(
    `SELECT * FROM ${table} WHERE id = ? FOR UPDATE`,
    [id],
  );
  if (!rows[0])
    throw Object.assign(new Error("记录不存在"), { code: "NOT_FOUND" });
  return rows[0];
}

export async function replaceTags(
  connection: PoolConnection,
  entityType: "goal" | "project" | "task" | "habit",
  entityId: string,
  names: string[],
): Promise<void> {
  await connection.query(
    "DELETE FROM entity_tag WHERE entity_type = ? AND entity_id = ?",
    [entityType, entityId],
  );
  // 标签去重使用数据库大小写不敏感唯一索引，逐项 upsert 后读取稳定 ID。
  for (const rawName of [
    ...new Set(names.map((name) => name.trim()).filter(Boolean)),
  ]) {
    const tagId = randomUUID();
    await connection.query(
      "INSERT INTO tag (id, name, created_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name)",
      [tagId, rawName, utcNow()],
    );
    const [tagRows] = await connection.query<RowDataPacket[]>(
      "SELECT id FROM tag WHERE name = ?",
      [rawName],
    );
    await connection.query(
      "INSERT INTO entity_tag (entity_type, entity_id, tag_id) VALUES (?, ?, ?)",
      [entityType, entityId, tagRows[0]!.id],
    );
  }
}

export async function cleanupEntityLinks(
  connection: PoolConnection,
  entityType: string,
  entityId: string,
): Promise<void> {
  // entity_link 是多态关联且无外键，因此删除实体时必须由应用层清理两端引用。
  await connection.query(
    "DELETE FROM entity_link WHERE (from_type = ? AND from_id = ?) OR (to_type = ? AND to_id = ?)",
    [entityType, entityId, entityType, entityId],
  );
}
