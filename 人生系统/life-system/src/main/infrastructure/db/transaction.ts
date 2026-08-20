import type { PoolConnection } from "mysql2/promise";
import { requirePool } from "./pool.js";

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function inTransaction<T>(
  operation: (connection: PoolConnection) => Promise<T>,
): Promise<T> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const connection = await requirePool().getConnection();
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await connection.beginTransaction();
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const result = await operation(connection);
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await connection.commit();
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return result;
  } catch (error) {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await connection.rollback();
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    throw error;
  } finally {
    connection.release();
  }
}
