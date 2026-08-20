import mysql, { type Pool } from "mysql2/promise";
import type { MysqlConnectionConfiguration } from "./configuration.js";

let activePool: Pool | undefined;

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function configurePool(
  configuration: MysqlConnectionConfiguration,
): Promise<Pool> {
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
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
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    connection.query("SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED");
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    connection.query("SET time_zone = '+00:00'");
  });
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return activePool;
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function requirePool(): Pool {
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (!activePool)
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    throw Object.assign(new Error("请先在设置页配置 MySQL 连接"), {
      code: "DB_UNAVAILABLE",
    });
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return activePool;
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function closePool(): Promise<void> {
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (activePool) await activePool.end();
  activePool = undefined;
}
