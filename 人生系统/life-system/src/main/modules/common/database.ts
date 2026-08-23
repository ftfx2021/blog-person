import { randomUUID } from "node:crypto";
import type { PoolConnection, RowDataPacket } from "mysql2/promise";

// 统一生成 UTC MySQL DATETIME 字符串，避免桌面端本地时区改变历史排序结果。
export const utcNow = (): string =>
  // 所有创建、更新时间使用 UTC，避免桌面端时区变化导致排序和审计时间漂移。
  new Date().toISOString().slice(0, 23).replace("T", " ");
export const toMysqlDateTime = (
  value: string | null | undefined,
): string | null =>
  // 输入 ISO 时间先转 UTC，再去掉 T 和时区标记以匹配 MySQL DATETIME(3)。
  // 空值保留 null，区别于一个真实的午夜时间，供可选截止日期使用。
  // 用户输入 ISO 时间先归一到 UTC；空值保持 null 以匹配可空日期列。
  value ? new Date(value).toISOString().slice(0, 23).replace("T", " ") : null;

export async function requireEntity(
  connection: PoolConnection,
  table: "goal" | "project" | "task" | "habit",
  id: string,
): Promise<any> {
  // 读取时加行锁，让随后同一事务内的状态判断和更新不会被并发请求穿透。
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
  // 替换采用“先清后建”，调用方必须在同一事务内执行以避免短暂的空标签状态被提交。
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
      // 新建标签时查询结果为空，必须使用上方查询或创建后都已确定的 tagId。
      [entityType, entityId, tagId],
    );
  }
}

export async function cleanupEntityLinks(
  connection: PoolConnection,
  entityType: string,
  entityId: string,
): Promise<void> {
  // 删除实体前移除作为起点或终点的链接，避免多态关联缺少外键时遗留脏数据。
  // entity_link 是多态关联且无外键，因此删除实体时必须由应用层清理两端引用。
  await connection.query(
    "DELETE FROM entity_link WHERE (from_type = ? AND from_id = ?) OR (to_type = ? AND to_id = ?)",
    [entityType, entityId, entityType, entityId],
  );
}
