import { z } from "zod";
import { localDateSchema } from "./common.js";

// MySQL 配置的完整校验在 IPC 前后复用，避免不受控端口和库名传给驱动。
export const mysqlSettingsSchema = z.object({
  host: z.string().trim().min(1),
  port: z.number().int().min(1).max(65535).default(3306),
  user: z.string().trim().min(1),
  password: z.string(),
  database: z
    .string()
    .trim()
    .regex(/^[a-zA-Z0-9_]+$/),
  connectTimeout: z.number().int().min(1000).max(30000).default(5000),
});
// 提醒偏好是独立 JSON 设置项，默认值保证旧数据库没有配置时界面也完整可用。
export const reminderSettingsSchema = z.object({
  criticalEnabled: z.boolean().default(true),
  periodicEnabled: z.boolean().default(true),
  recommendationEnabled: z.boolean().default(false),
  frequency: z.enum(["realtime", "daily"]).default("realtime"),
  aggregationMinutes: z.number().int().min(0).max(1440).default(30),
  readRetentionDays: z.number().int().min(1).max(365).default(30),
  recommendationRequiresConfirmation: z.literal(true).default(true),
});
// 搜索条件限制关键词、类型和标签数量，实际可查询表仍由服务层白名单决定。
export const searchInputSchema = z.object({
  keyword: z.string().trim().min(1).max(100),
  types: z
    .array(z.enum(["goal", "project", "task", "habit", "document"]))
    .default([]),
  tags: z.array(z.string().trim().min(1)).default([]),
  status: z.string().trim().optional(),
});
// 仪表盘日期必须显式传入，以便统计和测试在同一个自然日口径下可复现。
export const dashboardInputSchema = z.object({ today: localDateSchema });
// 仅支持明确的三种文本导出格式，不让文件扩展名成为自由输入。
export const exportSchema = z.object({
  format: z.enum(["json", "markdown", "txt"]),
});
// 恢复请求必须带固定确认短语；页面确认与 IPC schema 构成两道独立防线。
export const restoreSchema = z.object({
  manifestPath: z.string().min(1),
  confirmation: z.literal("恢复"),
});
// 无参数通道严格拒绝多余字段，避免调用方借“空操作”偷偷传递未定义输入。
export const emptySchema = z.object({}).strict();
// 知识库导入设置固定在受控键 knowledge.import，避免页面写任意 app_setting。
export const knowledgeImportSettingsSchema = z.object({
  maxFileSizeMb: z.number().int().min(1).max(500).default(20),
  parseTimeoutSeconds: z.number().int().min(10).max(600).default(60),
});
// OCR 只允许四种受控服务或关闭；密钥字段保持字符串以便主进程执行加密处理。
export const ocrSettingsSchema = z.object({
  provider: z
    .enum(["baidu", "tencent", "aliyun", "tesseract", "disabled"])
    .default("disabled"),
  baidu: z.object({ apiKey: z.string(), secretKey: z.string() }).optional(),
  tencent: z
    .object({
      secretId: z.string(),
      secretKey: z.string(),
      region: z.string().default("ap-guangzhou"),
    })
    .optional(),
  aliyun: z
    .object({
      accessKeyId: z.string(),
      accessKeySecret: z.string(),
      endpoint: z.string().default("ocr-api.cn-hangzhou.aliyuncs.com"),
    })
    .optional(),
});
// collection 名仅接受 Milvus 合法标识符，禁止页面把表达式或路径写入设置。
export const knowledgeStorageSettingsSchema = z.object({
  collectionName: z.string().regex(/^[A-Za-z_][A-Za-z0-9_]{0,127}$/).default("knowledge_chunk_v2"),
});

// LLM 配置只描述连接所需字段，apiKey 允许为空以兼容本机 Ollama。
export const llmSettingsSchema = z.object({
  provider: z.enum([
    "openai",
    "deepseek",
    "qwen",
    "moonshot",
    "zhipu",
    "ollama",
    "custom",
  ]),
  baseURL: z.string().trim().min(1),
  apiKey: z.string(),
  model: z.string().trim().min(1),
  // 默认保持既有 Chat Completions 行为，只有明确选择时才调用 Responses API。
  protocol: z
    .enum(["chat-completions", "responses"])
    .default("chat-completions"),
});
// Embedding 固定使用 Ollama OpenAI 兼容端点，不携带 API 密钥。
export const embeddingSettingsSchema = z.object({
  baseURL: z.string().trim().min(1).default("http://127.0.0.1:11434"),
  model: z.string().trim().min(1),
});
// 健康状态保持可序列化，detail 只包含面向用户的原因摘要。
export const aiHealthSchema = z.object({
  chat: z.object({
    ok: z.boolean(),
    detail: z.string(),
    provider: z.string().optional(),
    model: z.string().optional(),
  }),
  embedding: z.object({
    ok: z.boolean(),
    detail: z.string(),
    model: z.string().optional(),
    dim: z.number().optional(),
  }),
});
export const llmTestResultSchema = z.object({
  ok: z.boolean(),
  detail: z.string(),
  model: z.string().optional(),
});
export const embeddingTestResultSchema = z.object({
  ok: z.boolean(),
  detail: z.string(),
  model: z.string().optional(),
  dim: z.number().optional(),
});

// Milvus 连接配置允许本地免鉴权部署，密码只在主进程短暂存在并由 safeStorage 加密。
export const milvusSettingsSchema = z.object({
  address: z.string().trim().min(1).default("127.0.0.1:19530"),
  username: z.string().trim().default(""),
  password: z.string().default(""),
  ssl: z.boolean().default(false),
  connectTimeout: z.number().int().min(1000).max(60000).default(10000),
});
// 测试连接只返回用户可读诊断，不包含服务端原始响应或凭据。
export const milvusTestResultSchema = z.object({
  ok: z.boolean(),
  detail: z.string(),
  version: z.string().optional(),
  latencyMs: z.number().optional(),
  collections: z.array(z.string()).optional(),
});
// 健康结果携带固定 collection 的维度和行数，支持设置页展示不匹配警告。
export const milvusHealthSchema = z.object({
  ok: z.boolean(),
  detail: z.string(),
  version: z.string().optional(),
  latencyMs: z.number().optional(),
  collection: z
    .object({
      exists: z.boolean(),
      dim: z.number().optional(),
      rowCount: z.number().optional(),
      expectedDim: z.number().optional(),
    })
    .nullable(),
});

export type MysqlSettings = z.infer<typeof mysqlSettingsSchema>;
export type ReminderSettings = z.infer<typeof reminderSettingsSchema>;
export type LlmSettings = z.infer<typeof llmSettingsSchema>;
export type EmbeddingSettings = z.infer<typeof embeddingSettingsSchema>;
export type MilvusSettings = z.infer<typeof milvusSettingsSchema>;
export type KnowledgeImportSettings = z.infer<typeof knowledgeImportSettingsSchema>;
export type OcrSettings = z.infer<typeof ocrSettingsSchema>;
export type KnowledgeStorageSettings = z.infer<typeof knowledgeStorageSettingsSchema>;
// 系统 IPC DTO 固化设置、搜索、备份参数，防止页面传入任意路径或配置键。
