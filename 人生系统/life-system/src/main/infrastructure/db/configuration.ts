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

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function parseMysqlUrl(url: string): MysqlConnectionConfiguration {
  const parsed = new URL(url);
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (parsed.protocol !== "mysql:")
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    throw new Error("连接地址必须使用 mysql:// 协议");
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
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
