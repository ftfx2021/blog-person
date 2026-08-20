import type { ApiResult, AppError } from "../../shared/contracts/common.js";
import { failure, success } from "../../shared/contracts/common.js";
import { ZodError } from "zod";

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function toResult<T>(
  operation: () => Promise<T>,
): Promise<ApiResult<T>> {
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return success(await operation());
  } catch (error) {
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (error instanceof ZodError)
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
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
    ];
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (knownCodes.includes(candidate.code as AppError["code"]))
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return failure(candidate.code as AppError["code"], candidate.message);
    // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
    const databaseCodes = [
      "ECONNREFUSED",
      "ER_ACCESS_DENIED_ERROR",
      "ER_BAD_DB_ERROR",
      "PROTOCOL_CONNECTION_LOST",
      "ETIMEDOUT",
    ];
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (candidate.code && databaseCodes.includes(candidate.code))
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return failure("DB_UNAVAILABLE", "无法连接 MySQL，请检查设置与服务状态");
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return failure("INTERNAL_ERROR", candidate.message || "系统操作失败");
  }
}
