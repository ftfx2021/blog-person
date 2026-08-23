import type { ApiResult, AppError } from "../../shared/contracts/common.js";
import { failure, success } from "../../shared/contracts/common.js";
import { ZodError } from "zod";

// Electron IPC 只接受结构化可克隆值；统一把服务返回 DTO 转成纯 JSON，隔离驱动对象、Proxy 和特殊类实例。
function serializeIpcData<T>(value: T): T {
  const json = JSON.stringify(value);
  // 未定义返回值在 IPC 中等价于 null，避免 JSON.parse(undefined) 再次生成内部错误。
  return (json === undefined ? null : JSON.parse(json)) as T;
}

export async function toResult<T>(
  operation: () => Promise<T>,
): Promise<ApiResult<T>> {
  // 所有 IPC handler 共享这一层，成功值和异常都转换成可结构化克隆的 ApiResult。
  // ZodError 单独映射为 VALIDATION_ERROR，并保留 issues 供表单定位错误字段。
  // 已知业务错误码原样保留，页面可以据此导航设置或提示状态限制。
  // MySQL 驱动连接错误统一映射 DB_UNAVAILABLE，避免把底层驱动码暴露给用户。
  // 未知异常只携带安全的 message，不把堆栈、连接对象或原始异常跨进程传输。
  // 错误没有 code 时使用 INTERNAL_ERROR，调用方永远收到稳定的失败结构。
  // success 只包装 operation 返回的数据，不改变领域结果。
  // IPC 统一把异常映射为可序列化错误，避免跨进程传递原始异常对象。
  try {
    // 服务层可能返回数据库驱动对象或响应式代理，返回前强制收敛为 JSON DTO。
    return success(serializeIpcData(await operation()));
  } catch (error) {
    if (error instanceof ZodError)
      return failure("VALIDATION_ERROR", "输入内容不符合要求", error.issues);
    const candidate = error as Error & { code?: string };
    const knownCodes: AppError["code"][] = [
      "DB_UNAVAILABLE",
      "NOT_FOUND",
      "INVALID_STATE",
      "CONFLICT",
      "FILESYSTEM_ERROR",
      "BACKUP_FAILED",
      "RESTORE_FAILED",
      "AI_UNAVAILABLE",
      "INGEST_UNAVAILABLE",
      "AI_AUTH_ERROR",
      "AI_TIMEOUT",
      "VECTOR_DB_UNAVAILABLE",
    ];
    if (knownCodes.includes(candidate.code as AppError["code"]))
      return failure(candidate.code as AppError["code"], candidate.message);
    const databaseCodes = [
      "ECONNREFUSED",
      "ER_ACCESS_DENIED_ERROR",
      "ER_BAD_DB_ERROR",
      "PROTOCOL_CONNECTION_LOST",
      "ETIMEDOUT",
    ];
    if (candidate.code && databaseCodes.includes(candidate.code))
      return failure("DB_UNAVAILABLE", "无法连接 MySQL，请检查设置与服务状态");
    return failure("INTERNAL_ERROR", candidate.message || "系统操作失败");
  }
}
