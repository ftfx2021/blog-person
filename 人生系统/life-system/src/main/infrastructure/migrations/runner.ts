import { readdir, readFile } from "node:fs/promises";
import { join } from "node:path";
import mysql, { type RowDataPacket } from "mysql2/promise";
import type { MysqlConnectionConfiguration } from "../db/configuration.js";

export interface MigrationResult {
  applied: string[];
  skipped: string[];
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function runMigrations(
  configuration: MysqlConnectionConfiguration,
  directory: string,
): Promise<MigrationResult> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const connection = await mysql.createConnection({
    ...configuration,
    charset: "utf8mb4",
    timezone: "Z",
    multipleStatements: true,
  });

  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await connection.query(
      "SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED",
    );
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await connection.query("SET time_zone = '+00:00'");
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await connection.query(`
      CREATE TABLE IF NOT EXISTS schema_migrations (
        id VARCHAR(128) NOT NULL PRIMARY KEY,
        applied_at DATETIME(3) NOT NULL
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    `);

    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const [rows] = await connection.query<
      Array<RowDataPacket & { id: string }>
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    >("SELECT id FROM schema_migrations");
    // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
    const appliedIds = new Set(rows.map((row) => row.id));
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const files = (await readdir(directory))
      .filter((file) => /^\d+.*\.sql$/.test(file))
      .sort();
    // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
    const result: MigrationResult = { applied: [], skipped: [] };

    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    for (const file of files) {
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (appliedIds.has(file)) {
        result.skipped.push(file);
        continue;
      }

      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const sql = await readFile(join(directory, file), "utf8");
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      try {
        // MySQL DDL 会隐式提交；迁移文件先经过完整读取，失败时绝不写版本记录，便于修复后增量续跑。
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await connection.beginTransaction();
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await connection.query(sql);
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await connection.query(
          // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
          "INSERT INTO schema_migrations (id, applied_at) VALUES (?, UTC_TIMESTAMP(3))",
          [file],
        );
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await connection.commit();
        result.applied.push(file);
      } catch (error) {
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await connection.rollback();
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw new Error(
          `迁移 ${file} 失败：${error instanceof Error ? error.message : String(error)}`,
        );
      }
    }
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return result;
  } finally {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await connection.end();
  }
}
