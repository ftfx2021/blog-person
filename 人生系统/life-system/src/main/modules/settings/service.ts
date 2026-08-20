import { readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { app, safeStorage } from "electron";
import mysql from "mysql2/promise";
import type {
  MysqlSettings,
  ReminderSettings,
} from "../../../shared/contracts/system.js";
import { configurePool, requirePool } from "../../infrastructure/db/pool.js";
import { runMigrations } from "../../infrastructure/migrations/runner.js";
import { applicationPaths } from "../../infrastructure/filesystem/paths.js";
import { utcNow } from "../common/database.js";

interface PersistedConnection extends Omit<MysqlSettings, "password"> {
  encryptedPassword: string;
}
const configurationPath = () =>
  join(app.getPath("userData"), "data", "mysql-connection.json");

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function persistConnection(input: MysqlSettings): Promise<void> {
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (!safeStorage.isEncryptionAvailable())
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    throw Object.assign(new Error("系统安全存储当前不可用，不能保存密码"), {
      code: "FILESYSTEM_ERROR",
    });
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await applicationPaths();
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  const persisted: PersistedConnection = {
    ...input,
    encryptedPassword: safeStorage
      .encryptString(input.password)
      .toString("base64"),
  };
  delete (persisted as any).password;
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await writeFile(
    configurationPath(),
    JSON.stringify(persisted, null, 2),
    "utf8",
  );
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function loadConnection(): Promise<MysqlSettings | null> {
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    const persisted = JSON.parse(
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await readFile(configurationPath(), "utf8"),
    ) as PersistedConnection;
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return {
      ...persisted,
      password: safeStorage.decryptString(
        Buffer.from(persisted.encryptedPassword, "base64"),
      ),
    };
  } catch {
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return null;
  }
}
export const settingsService = {
  saveMysql: async (input: MysqlSettings) => {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await persistConnection(input);
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const pool = await configurePool(input);
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const migrations = await runMigrations(
      input,
      app.isPackaged
        ? join(process.resourcesPath, "migrations")
        : join(app.getAppPath(), "migrations"),
    );
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await pool.query(
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      "INSERT INTO app_setting (`key`,value_json,updated_at) VALUES ('mysql.connection',?,?) ON DUPLICATE KEY UPDATE value_json=VALUES(value_json),updated_at=VALUES(updated_at)",
      [
        JSON.stringify({
          host: input.host,
          port: input.port,
          user: input.user,
          database: input.database,
          connectTimeout: input.connectTimeout,
          passwordEncrypted: true,
        }),
        utcNow(),
      ],
    );
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return { saved: true, migrations };
  },
  getMysql: async () => {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const value = await loadConnection();
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return value
      ? { ...value, password: value.password ? "********" : "" }
      : null;
  },
  testMysql: async (input: MysqlSettings) => {
    const started = Date.now();
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const connection = await mysql.createConnection({
      ...input,
      timezone: "Z",
    });
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    try {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<any[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT VERSION() AS version,1 AS healthy",
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return {
        healthy: true,
        latencyMs: Date.now() - started,
        version: rows[0]!.version,
      };
    } finally {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.end();
    }
  },
  health: async () => {
    const started = Date.now();
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const [rows] = await requirePool().query<any[]>(
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      "SELECT VERSION() AS version",
    );
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return {
      healthy: true,
      latencyMs: Date.now() - started,
      version: rows[0]!.version,
    };
  },
  saveReminders: async (input: ReminderSettings) => {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await requirePool().query(
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      "INSERT INTO app_setting (`key`,value_json,updated_at) VALUES ('notify.preferences',?,?) ON DUPLICATE KEY UPDATE value_json=VALUES(value_json),updated_at=VALUES(updated_at)",
      [JSON.stringify(input), utcNow()],
    );
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return input;
  },
  getReminders: async () => {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const [rows] = await requirePool().query<any[]>(
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      "SELECT value_json AS valueJson FROM app_setting WHERE `key`='notify.preferences'",
    );
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return (
      rows[0]?.valueJson ?? {
        criticalEnabled: true,
        periodicEnabled: true,
        recommendationEnabled: false,
        frequency: "realtime",
        aggregationMinutes: 30,
        readRetentionDays: 30,
        recommendationRequiresConfirmation: true,
      }
    );
  },
  milvusStatus: async () => ({
    enabled: false,
    status: "P1_DISABLED",
    message: "未启用（P1）",
  }),
};
