import { z } from "zod";

export const entityIdSchema = z.string().uuid();
export const nullableEntityIdSchema = entityIdSchema.nullable().optional();
export const localDateSchema = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "日期格式必须为 YYYY-MM-DD");
export const utcDateTimeSchema = z.string().datetime({ offset: true });
export const confirmationSchema = z.object({
  id: entityIdSchema,
  confirmed: z.literal(true),
});

export const errorCodeSchema = z.enum([
  "VALIDATION_ERROR",
  "DB_UNAVAILABLE",
  "NOT_FOUND",
  "INVALID_STATE",
  "CONFLICT",
  "FILESYSTEM_ERROR",
  "BACKUP_FAILED",
  "RESTORE_FAILED",
  "INTERNAL_ERROR",
]);

export interface AppError {
  code: z.infer<typeof errorCodeSchema>;
  message: string;
  details?: unknown;
}

export type ApiResult<T> =
  { ok: true; data: T } | { ok: false; error: AppError };

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function success<T>(data: T): ApiResult<T> {
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return { ok: true, data };
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function failure<T>(
  code: AppError["code"],
  message: string,
  details?: unknown,
): ApiResult<T> {
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return { ok: false, error: { code, message, details } };
}
// 公共错误码和结果包装让 DB_UNAVAILABLE 等故障在所有页面表现一致。
