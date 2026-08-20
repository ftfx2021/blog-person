import mysql, { type Pool } from "mysql2/promise";
import type { MysqlConnectionConfiguration } from "./configuration.js";

let activePool: Pool | undefined;

export async function configurePool(
  configuration: MysqlConnectionConfiguration,
): Promise<Pool> {
  if (activePool) await activePool.end();
  activePool = mysql.createPool({
    ...configuration,
    charset: "utf8mb4",
    timezone: "Z",
    connectionLimit: 8,
    waitForConnections: true,
    queueLimit: 0,
    multipleStatements: false,
  });
  // 每次取连接时显式使用 READ COMMITTED，避免依赖服务器全局默认值。
  activePool.on("connection", (connection) => {
    connection.query("SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED");
    connection.query("SET time_zone = '+00:00'");
  });
  return activePool;
}

export function requirePool(): Pool {
  if (!activePool)
    throw Object.assign(new Error("请先在设置页配置 MySQL 连接"), {
      code: "DB_UNAVAILABLE",
    });
  return activePool;
}

export async function closePool(): Promise<void> {
  if (activePool) await activePool.end();
  activePool = undefined;
}
