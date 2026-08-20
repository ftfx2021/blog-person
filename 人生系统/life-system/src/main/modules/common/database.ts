import { randomUUID } from "node:crypto";
import type { PoolConnection, RowDataPacket } from "mysql2/promise";

export const utcNow = (): string =>
  new Date().toISOString().slice(0, 23).replace("T", " ");
export const toMysqlDateTime = (
  value: string | null | undefined,
): string | null =>
  value ? new Date(value).toISOString().slice(0, 23).replace("T", " ") : null;

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function requireEntity(
  connection: PoolConnection,
  table: "goal" | "project" | "task" | "habit",
  id: string,
): Promise<any> {
  // 表名来自受控联合类型，绝不接受渲染层传入任意 SQL 标识符。
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const [rows] = await connection.query<RowDataPacket[]>(
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    `SELECT * FROM ${table} WHERE id = ? FOR UPDATE`,
    [id],
  );
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (!rows[0])
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    throw Object.assign(new Error("记录不存在"), { code: "NOT_FOUND" });
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return rows[0];
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function replaceTags(
  connection: PoolConnection,
  entityType: "goal" | "project" | "task" | "habit",
  entityId: string,
  names: string[],
): Promise<void> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await connection.query(
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
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
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const [tagRows] = await connection.query<RowDataPacket[]>(
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
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
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await connection.query(
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      "INSERT INTO entity_tag (entity_type, entity_id, tag_id) VALUES (?, ?, ?)",
      [entityType, entityId, tagRows[0]!.id],
    );
  }
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function cleanupEntityLinks(
  connection: PoolConnection,
  entityType: string,
  entityId: string,
): Promise<void> {
  // entity_link 是多态关联且无外键，因此删除实体时必须由应用层清理两端引用。
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await connection.query(
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    "DELETE FROM entity_link WHERE (from_type = ? AND from_id = ?) OR (to_type = ? AND to_id = ?)",
    [entityType, entityId, entityType, entityId],
  );
}
