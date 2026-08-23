import type { PoolConnection } from "mysql2/promise";
import { requirePool } from "./pool.js";

export async function inTransaction<T>(
  operation: (connection: PoolConnection) => Promise<T>,
): Promise<T> {
  // 每个业务调用独占一个连接，避免不同请求共享事务上下文。
  // beginTransaction 标记原子边界，服务内部的多次 SQL 要么全部生效要么全部撤销。
  // operation 只描述业务步骤，基础设施统一负责提交、回滚和连接生命周期。
  // commit 仅在 operation 成功返回后执行，避免部分写入被错误提交。
  // rollback 在异常路径执行，覆盖外键校验、状态机拒绝和 SQL 失败等情况。
  // finally 无论提交还是回滚都会 release，防止 Electron 长时间运行耗尽连接池。
  // 原始错误继续向上抛出，由 IPC result 映射为用户可理解的错误码。
  // 读取事务也沿用同一机制，保证连接配置和释放策略不分叉。
  // 统一托管 begin/commit/rollback，让上层服务保持原子性。
  const connection = await requirePool().getConnection();
  try {
    await connection.beginTransaction();
    const result = await operation(connection);
    await connection.commit();
    return result;
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}
