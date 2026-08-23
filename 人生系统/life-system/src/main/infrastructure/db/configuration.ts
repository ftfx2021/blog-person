import { z } from "zod";

export const mysqlConnectionSchema = z.object({
  host: z.string().trim().min(1),
  port: z.number().int().min(1).max(65535).default(3306),
  user: z.string().trim().min(1),
  password: z.string(),
  database: z
    .string()
    .trim()
    .regex(/^[a-zA-Z0-9_]+$/, "数据库名只能包含字母、数字和下划线"),
  connectTimeout: z.number().int().min(1000).max(30000).default(5000),
});

export type MysqlConnectionConfiguration = z.infer<
  typeof mysqlConnectionSchema
>;

export function parseMysqlUrl(url: string): MysqlConnectionConfiguration {
  // URL 解析只接受 mysql 协议，阻止把任意 URI 当成驱动连接串解释。
  // 最终仍交给 Zod schema 校验端口、库名与必填字段，避免解析结果绕过配置规则。
  const parsed = new URL(url);
  if (parsed.protocol !== "mysql:")
    throw new Error("连接地址必须使用 mysql:// 协议");
  return mysqlConnectionSchema.parse({
    host: parsed.hostname,
    port: parsed.port ? Number(parsed.port) : 3306,
    user: decodeURIComponent(parsed.username),
    password: decodeURIComponent(parsed.password),
    database: parsed.pathname.replace(/^\//, ""),
    connectTimeout: 5000,
  });
}
// MySQL 连接配置由 Zod 统一校验，避免非法主机、端口或库名进入驱动。
