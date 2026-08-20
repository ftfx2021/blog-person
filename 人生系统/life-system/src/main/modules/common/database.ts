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
  // 小写归一与 utf8mb4_0900_ai_ci 唯一索引语义一致，避免 Coding/coding 重复关联。
  const normalizedNames = new Map<string, string>();
  for (const name of names) {
    const trimmed = name.trim();
    if (trimmed) normalizedNames.set(trimmed.toLowerCase(), trimmed);
  }
  // 先查找已有标签再插入，复用同一个 ID 并让关联表保持一条记录。
  for (const rawName of normalizedNames.values()) {
    const [tagRows] = await connection.query<RowDataPacket[]>(
      "SELECT id FROM tag WHERE LOWER(name) = LOWER(?) LIMIT 1",
      [rawName],
    );
    // 只有数据库中确实不存在时才创建标签，避免唯一索引冲突。
    let tagId = tagRows[0]?.id as string | undefined;
    if (!tagId) {
      tagId = randomUUID();
      await connection.query(
        "INSERT INTO tag (id, name, created_at) VALUES (?, ?, ?)",
        [tagId, rawName, utcNow()],
      );
    }
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
