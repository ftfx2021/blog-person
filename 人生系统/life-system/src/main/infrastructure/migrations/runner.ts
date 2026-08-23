import { readdir, readFile } from "node:fs/promises";
import { join } from "node:path";
import mysql, { type RowDataPacket } from "mysql2/promise";
import type { MysqlConnectionConfiguration } from "../db/configuration.js";

export interface MigrationResult {
  applied: string[];
  skipped: string[];
}

export async function runMigrations(
  configuration: MysqlConnectionConfiguration,
  directory: string,
): Promise<MigrationResult> {
  // 迁移使用独立短连接，避免在应用连接池尚未完全建立时混入业务事务。
  // 返回 applied/skipped 清单，使设置页能区分首次建库和重复执行的结果。
  const connection = await mysql.createConnection({
    ...configuration,
    charset: "utf8mb4",
    timezone: "Z",
    multipleStatements: true,
  });

  try {
    await connection.query(
      "SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED",
    );
    await connection.query("SET time_zone = '+00:00'");
    await connection.query(`
      CREATE TABLE IF NOT EXISTS schema_migrations (
        id VARCHAR(128) NOT NULL PRIMARY KEY,
        applied_at DATETIME(3) NOT NULL
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    `);

    const [rows] = await connection.query<
      Array<RowDataPacket & { id: string }>
    >("SELECT id FROM schema_migrations");
    const appliedIds = new Set(rows.map((row) => row.id));
    const files = (await readdir(directory))
      .filter((file) => /^\d+.*\.sql$/.test(file))
      .sort();
    const result: MigrationResult = { applied: [], skipped: [] };

    for (const file of files) {
      if (appliedIds.has(file)) {
        result.skipped.push(file);
        continue;
      }

      const sql = await readFile(join(directory, file), "utf8");
      try {
        // MySQL DDL 会隐式提交；迁移文件先经过完整读取，失败时绝不写版本记录，便于修复后增量续跑。
        await connection.beginTransaction();
        await connection.query(sql);
        await connection.query(
          "INSERT INTO schema_migrations (id, applied_at) VALUES (?, UTC_TIMESTAMP(3))",
          [file],
        );
        await connection.commit();
        result.applied.push(file);
      } catch (error) {
        await connection.rollback();
        throw new Error(
          `迁移 ${file} 失败：${error instanceof Error ? error.message : String(error)}`,
        );
      }
    }
    return result;
  } finally {
    await connection.end();
  }
}
