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
  "AI_UNAVAILABLE",
  "INGEST_UNAVAILABLE",
  "VECTOR_DB_UNAVAILABLE",
  "AI_AUTH_ERROR",
  "AI_TIMEOUT",
]);

export interface AppError {
  code: z.infer<typeof errorCodeSchema>;
  message: string;
  details?: unknown;
}

export type ApiResult<T> =
  { ok: true; data: T } | { ok: false; error: AppError };

export function success<T>(data: T): ApiResult<T> {
  return { ok: true, data };
}

export function failure<T>(
  code: AppError["code"],
  message: string,
  details?: unknown,
): ApiResult<T> {
  return { ok: false, error: { code, message, details } };
}
// 公共错误码和结果包装让 DB_UNAVAILABLE 等故障在所有页面表现一致。
