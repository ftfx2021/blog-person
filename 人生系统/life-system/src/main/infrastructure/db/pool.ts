import mysql, { type Pool } from "mysql2/promise";
import type { MysqlConnectionConfiguration } from "./configuration.js";

let activePool: Pool | undefined;

export async function configurePool(
  configuration: MysqlConnectionConfiguration,
): Promise<Pool> {
  // 重配前关闭旧连接池，确保设置页切换数据库后没有请求继续命中旧配置。
  // 连接池固定 UTC 与 utf8mb4，保证日期、中文标签和排序在不同 MySQL 默认设置下稳定。
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
  // 业务服务通过此守卫取得连接池，未配置数据库时返回可识别的 DB_UNAVAILABLE 错误。
  // 不在每个服务内重复判断 activePool，避免错误码和提示语漂移。
  if (!activePool)
    throw Object.assign(new Error("请先在设置页配置 MySQL 连接"), {
      code: "DB_UNAVAILABLE",
    });
  return activePool;
}

export async function closePool(): Promise<void> {
  // 应用退出或重配时显式释放所有连接，防止 Electron 主进程被数据库 socket 持有。
  // 释放后清空引用，后续误用会被 requirePool 及时发现而非操作已结束的池。
  if (activePool) await activePool.end();
  activePool = undefined;
}
