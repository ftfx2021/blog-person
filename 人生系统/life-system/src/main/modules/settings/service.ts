import { readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { app, safeStorage } from "electron";
import mysql from "mysql2/promise";
import type {
  EmbeddingSettings,
  LlmSettings,
  MysqlSettings,
  MilvusSettings,
  ReminderSettings,
  KnowledgeImportSettings,
  OcrSettings,
  KnowledgeStorageSettings,
} from "../../../shared/contracts/system.js";
import {
  embeddingSettingsSchema,
  llmSettingsSchema,
  milvusHealthSchema,
  milvusSettingsSchema,
  knowledgeImportSettingsSchema,
  ocrSettingsSchema,
  knowledgeStorageSettingsSchema,
} from "../../../shared/contracts/system.js";
import { createEmbeddingProvider } from "../../infrastructure/embedding/index.js";
import { createLlmProvider } from "../../infrastructure/llm/index.js";
import type { LlmProviderConfig } from "../../infrastructure/llm/types.js";
import { configurePool, requirePool } from "../../infrastructure/db/pool.js";
import { runMigrations } from "../../infrastructure/migrations/runner.js";
import { applicationPaths } from "../../infrastructure/filesystem/paths.js";
import { utcNow } from "../common/database.js";
import {
  closeMilvus,
  connectMilvus,
  createMilvusRepository,
} from "../../infrastructure/milvus/index.js";

interface PersistedConnection extends Omit<MysqlSettings, "password"> {
  encryptedPassword: string;
}
// 连接配置固定存放在 userData，避免开发目录切换或打包后丢失用户设置。
const configurationPath = () =>
  join(app.getPath("userData"), "data", "mysql-connection.json");

// 将 MySQL 密码加密后写入用户目录，安全存储不可用时拒绝落盘明文。
async function persistConnection(input: MysqlSettings): Promise<void> {
  // 密码只写入 Electron 安全存储的密文；安全存储不可用时宁可拒绝保存也不落明文。
  if (!safeStorage.isEncryptionAvailable())
    throw Object.assign(new Error("系统安全存储当前不可用，不能保存密码"), {
      code: "FILESYSTEM_ERROR",
    });
  await applicationPaths();
  const persisted: PersistedConnection = {
    ...input,
    encryptedPassword: safeStorage
      .encryptString(input.password)
      .toString("base64"),
  };
  delete (persisted as any).password;
  await writeFile(
    configurationPath(),
    JSON.stringify(persisted, null, 2),
    "utf8",
  );
}

// 从 userData 读取 MySQL 配置并解密密码；文件缺失或密文失效按未配置处理。
export async function loadConnection(): Promise<MysqlSettings | null> {
  // 读取或解密失败统一视为“尚未配置”，让首次启动流程可自然接管。
  try {
    const persisted = JSON.parse(
      await readFile(configurationPath(), "utf8"),
    ) as PersistedConnection;
    // 解密后只返回 MysqlSettings；encryptedPassword 仅属于磁盘格式，不能继续传给 mysql2。
    const { encryptedPassword, ...connection } = persisted;
    return {
      ...connection,
      password: safeStorage.decryptString(
        Buffer.from(encryptedPassword, "base64"),
      ),
    };
  } catch {
    return null;
  }
}

const llmProviderPresets = [
  { value: "openai", label: "OpenAI", baseURL: "https://api.openai.com" },
  { value: "deepseek", label: "DeepSeek", baseURL: "https://api.deepseek.com" },
  {
    value: "qwen",
    label: "通义千问",
    baseURL: "https://dashscope.aliyuncs.com/compatible-mode",
  },
  { value: "moonshot", label: "Kimi", baseURL: "https://api.moonshot.cn" },
  {
    value: "zhipu",
    label: "智谱",
    baseURL: "https://open.bigmodel.cn/api/paas",
  },
  { value: "ollama", label: "Ollama", baseURL: "http://127.0.0.1:11434" },
  { value: "custom", label: "自定义", baseURL: "" },
] as const;

// 将数据库驱动返回的 JSON 字段统一转换成普通对象，兼容字符串和已解析对象两种形态。
function parseSetting(value: unknown): Record<string, unknown> {
  // MySQL 驱动可能已经解析 JSON，也可能返回字符串；统一在主进程边界归一化。
  if (typeof value === "string") {
    try {
      return JSON.parse(value) as Record<string, unknown>;
    } catch {
      return {};
    }
  }
  return value && typeof value === "object"
    ? (value as Record<string, unknown>)
    : {};
}

// 对 API Key 做不可逆展示掩码，确保渲染层只能确认首尾而不能恢复密钥。
function maskApiKey(value: string): string {
  // 短密钥全部遮蔽，长密钥仅保留首尾四位以便用户确认当前配置。
  if (value.length <= 8) return value ? "********" : "";
  return `${value.slice(0, 4)}****${value.slice(-4)}`;
}

// 按受控设置键读取 app_setting，禁止调用方传入 SQL 片段或任意表名。
async function readAiSetting(
  key: string,
): Promise<Record<string, unknown> | null> {
  const [rows] = await requirePool().query<any[]>(
    "SELECT value_json AS valueJson FROM app_setting WHERE `key`=?",
    [key],
  );
  return rows[0] ? parseSetting(rows[0].valueJson) : null;
}

// 以单个 JSON 设置项原子写入 AI 配置，更新时同步记录 UTC 时间。
async function writeAiSetting(
  key: string,
  value: Record<string, unknown>,
): Promise<void> {
  await requirePool().query(
    "INSERT INTO app_setting (`key`,value_json,updated_at) VALUES (?,?,?) ON DUPLICATE KEY UPDATE value_json=VALUES(value_json),updated_at=VALUES(updated_at)",
    [key, JSON.stringify(value), utcNow()],
  );
}

// OCR 密钥与 LLM 密钥同样使用系统安全存储，数据库只保存不可读密文。
function encryptOcrValue(value: string): string {
  if (!value) return "";
  if (!safeStorage.isEncryptionAvailable())
    throw Object.assign(new Error("系统安全存储当前不可用，不能保存 OCR 密钥"), {
      code: "FILESYSTEM_ERROR",
    });
  return safeStorage.encryptString(value).toString("base64");
}

function decryptOcrValue(value: unknown): string {
  if (typeof value !== "string" || !value) return "";
  try {
    return safeStorage.decryptString(Buffer.from(value, "base64"));
  } catch {
    return "";
  }
}

function isMasked(value: string): boolean {
  return value.includes("****");
}

// 从磁盘格式恢复仅供 OCR 服务消费的明文配置，绝不跨 IPC 返回。
async function getStoredOcrConfig(): Promise<OcrSettings> {
  const stored = await readAiSetting("knowledge.ocr");
  if (!stored) return ocrSettingsSchema.parse({});
  return ocrSettingsSchema.parse({
    provider: stored.provider ?? "disabled",
    baidu: stored.baidu
      ? {
          apiKey: decryptOcrValue((stored.baidu as any).apiKeyEncrypted),
          secretKey: decryptOcrValue((stored.baidu as any).secretKeyEncrypted),
        }
      : undefined,
    tencent: stored.tencent
      ? {
          secretId: decryptOcrValue((stored.tencent as any).secretIdEncrypted),
          secretKey: decryptOcrValue((stored.tencent as any).secretKeyEncrypted),
          region: (stored.tencent as any).region ?? "ap-guangzhou",
        }
      : undefined,
    aliyun: stored.aliyun
      ? {
          accessKeyId: decryptOcrValue((stored.aliyun as any).accessKeyIdEncrypted),
          accessKeySecret: decryptOcrValue((stored.aliyun as any).accessKeySecretEncrypted),
          endpoint: (stored.aliyun as any).endpoint ?? "ocr-api.cn-hangzhou.aliyuncs.com",
        }
      : undefined,
  });
}

// Milvus 密码与 LLM 密钥一样只以 safeStorage 密文写入 app_setting。
async function readMilvusSetting(): Promise<Record<string, unknown> | null> {
  return readAiSetting("milvus.connection");
}
async function storedMilvusConfig(): Promise<MilvusSettings | null> {
  const stored = await readMilvusSetting();
  if (!stored || typeof stored.address !== "string") return null;
  let password = "";
  if (
    typeof stored.passwordEncrypted === "string" &&
    stored.passwordEncrypted
  ) {
    try {
      password = safeStorage.decryptString(
        Buffer.from(stored.passwordEncrypted, "base64"),
      );
    } catch {
      password = "";
    }
  }
  return milvusSettingsSchema.parse({
    address: stored.address,
    username: stored.username ?? "",
    password,
    ssl: stored.ssl ?? false,
    connectTimeout: stored.connectTimeout ?? 10000,
  });
}
async function milvusHealth(): Promise<any> {
  const config = await storedMilvusConfig();
  if (!config)
    return milvusHealthSchema.parse({
      ok: false,
      detail: "未配置",
      collection: null,
    });
  const client = await connectMilvus(config);
  try {
    const embedding = await getStoredEmbeddingConfig();
    let expectedDim: number | undefined;
    if (embedding) {
      const result = await createEmbeddingProvider(embedding).healthCheck();
      expectedDim = result.dim;
    }
    const storage = await settingsService.getKnowledgeStorage();
    const repository = createMilvusRepository(client, storage.collectionName);
    const result = await repository.health();
    if (result.collection && expectedDim)
      result.collection.expectedDim = expectedDim;
    if (
      result.collection?.dim &&
      expectedDim &&
      result.collection.dim !== expectedDim
    ) {
      result.ok = false;
      result.detail = "Embedding 模型维度与向量库不匹配";
    }
    return milvusHealthSchema.parse(result);
  } finally {
    closeMilvus(client);
  }
}

// 读取并解密 LLM 配置，仅供主进程健康检查使用，不直接返回给渲染层。
async function getStoredLlmConfig(): Promise<LlmSettings | null> {
  const stored = await readAiSetting("llm.chat");
  if (
    !stored ||
    typeof stored.provider !== "string" ||
    typeof stored.baseURL !== "string" ||
    typeof stored.model !== "string"
  )
    return null;
  let apiKey = "";
  if (typeof stored.apiKeyEncrypted === "string" && stored.apiKeyEncrypted) {
    try {
      // 密文只在主进程短暂解密为 provider 所需字符串，不写日志或回传页面。
      apiKey = safeStorage.decryptString(
        Buffer.from(stored.apiKeyEncrypted, "base64"),
      );
    } catch {
      apiKey = "";
    }
  }
  return llmSettingsSchema.parse({
    provider: stored.provider,
    baseURL: stored.baseURL,
    model: stored.model,
    apiKey,
    protocol: stored.protocol ?? "chat-completions",
  });
}

// 读取 embedding 配置并通过 schema 复核持久化数据，避免旧版本脏字段进入 provider。
async function getStoredEmbeddingConfig(): Promise<EmbeddingSettings | null> {
  const stored = await readAiSetting("llm.embedding");
  if (!stored) return null;
  return embeddingSettingsSchema.parse(stored);
}

// 分别检查 LLM 与 embedding，任何一侧未配置或失败都保留另一侧的健康结果。
async function aiHealth(): Promise<any> {
  // 两个 provider 并行检查，避免远端 LLM 超时时阻塞本地 Ollama 状态展示。
  const llm = await getStoredLlmConfig();
  const embedding = await getStoredEmbeddingConfig();
  // 每个检查都单独兜底，保证一个 provider 的构造或网络异常不丢失另一侧结果。
  const chatPromise = llm
    ? createLlmProvider({
        baseURL: llm.baseURL,
        apiKey: llm.apiKey,
        model: llm.model,
        protocol: llm.protocol,
      })
        .healthCheck()
        .then((result) => ({
          ...result,
          provider: llm.provider,
          model: llm.model,
        }))
        .catch(() => ({
          ok: false,
          detail: "AI 服务不可用",
          provider: llm.provider,
          model: llm.model,
        }))
    : Promise.resolve({ ok: false, detail: "未配置" });
  const embeddingPromise = embedding
    ? createEmbeddingProvider(embedding)
        .healthCheck()
        .then((result) => ({ ...result, model: embedding.model }))
        .catch(() => ({
          ok: false,
          detail: "Embedding 服务不可用",
          model: embedding.model,
        }))
    : Promise.resolve({ ok: false, detail: "未配置" });
  const [chat, embeddingHealth] = await Promise.all([
    chatPromise,
    embeddingPromise,
  ]);
  return { chat, embedding: embeddingHealth };
}
export const settingsService = {
  // collection 名是受控设置，Milvus 读写端据此保持同一个 v2 集合。
  getKnowledgeStorage: async (): Promise<KnowledgeStorageSettings> => {
    const stored = await readAiSetting("knowledge.storage");
    return knowledgeStorageSettingsSchema.parse(stored ?? {});
  },
  saveKnowledgeStorage: async (input: KnowledgeStorageSettings) => {
    const parsed = knowledgeStorageSettingsSchema.parse(input);
    await writeAiSetting("knowledge.storage", parsed);
    return parsed;
  },
  // 入库管线读取该方法取得短暂明文，渲染层永远不会看到密钥。
  getOcr: getStoredOcrConfig,
  // OCR 设置仅回显掩码，供用户确认配置存在而不能恢复凭据。
  getOcrDisplay: async () => {
    const config = await getStoredOcrConfig();
    return {
      provider: config.provider,
      // 返回完整嵌套对象，设置页切换 provider 时不会把 v-model 绑定到 undefined。
      baidu: { apiKey: maskApiKey(config.baidu?.apiKey ?? ""), secretKey: maskApiKey(config.baidu?.secretKey ?? "") },
      tencent: { secretId: maskApiKey(config.tencent?.secretId ?? ""), secretKey: maskApiKey(config.tencent?.secretKey ?? ""), region: config.tencent?.region ?? "ap-guangzhou" },
      aliyun: { accessKeyId: maskApiKey(config.aliyun?.accessKeyId ?? ""), accessKeySecret: maskApiKey(config.aliyun?.accessKeySecret ?? ""), endpoint: config.aliyun?.endpoint ?? "ocr-api.cn-hangzhou.aliyuncs.com" },
    };
  },
  // 保存时保留页面未编辑的掩码字段，只把真正的新密钥安全加密后落库。
  saveOcr: async (input: OcrSettings) => {
    const parsed = ocrSettingsSchema.parse(input);
    const previous = await getStoredOcrConfig();
    const choose = (value: string, oldValue: string) => isMasked(value) ? oldValue : value;
    const baidu = parsed.baidu ? { apiKey: choose(parsed.baidu.apiKey, previous.baidu?.apiKey ?? ""), secretKey: choose(parsed.baidu.secretKey, previous.baidu?.secretKey ?? "") } : undefined;
    const tencent = parsed.tencent ? { secretId: choose(parsed.tencent.secretId, previous.tencent?.secretId ?? ""), secretKey: choose(parsed.tencent.secretKey, previous.tencent?.secretKey ?? ""), region: parsed.tencent.region } : undefined;
    const aliyun = parsed.aliyun ? { accessKeyId: choose(parsed.aliyun.accessKeyId, previous.aliyun?.accessKeyId ?? ""), accessKeySecret: choose(parsed.aliyun.accessKeySecret, previous.aliyun?.accessKeySecret ?? ""), endpoint: parsed.aliyun.endpoint } : undefined;
    await writeAiSetting("knowledge.ocr", {
      provider: parsed.provider,
      baidu: baidu ? { apiKeyEncrypted: encryptOcrValue(baidu.apiKey), secretKeyEncrypted: encryptOcrValue(baidu.secretKey) } : undefined,
      tencent: tencent ? { secretIdEncrypted: encryptOcrValue(tencent.secretId), secretKeyEncrypted: encryptOcrValue(tencent.secretKey), region: tencent.region } : undefined,
      aliyun: aliyun ? { accessKeyIdEncrypted: encryptOcrValue(aliyun.accessKeyId), accessKeySecretEncrypted: encryptOcrValue(aliyun.accessKeySecret), endpoint: aliyun.endpoint } : undefined,
    });
    return { saved: true };
  },
  // 测试只确认候选配置的完整性，不发起识别以免用户密钥未保存时上传文档内容。
  testOcr: async (input: OcrSettings) => {
    const parsed = ocrSettingsSchema.parse(input);
    const credentialsPresent = parsed.provider === "tesseract" || parsed.provider === "disabled" || (parsed.provider === "baidu" && Boolean(parsed.baidu?.apiKey && parsed.baidu.secretKey)) || (parsed.provider === "tencent" && Boolean(parsed.tencent?.secretId && parsed.tencent.secretKey)) || (parsed.provider === "aliyun" && Boolean(parsed.aliyun?.accessKeyId && parsed.aliyun.accessKeySecret));
    return { ok: credentialsPresent, detail: credentialsPresent ? "OCR 配置格式有效，导入扫描件时将执行识别" : "当前服务缺少必填密钥" };
  },
  // 读取知识导入设置，旧库无记录时返回 schema 默认值。
  getKnowledgeImport: async (): Promise<KnowledgeImportSettings> => {
    const stored = await readAiSetting("knowledge.import");
    return knowledgeImportSettingsSchema.parse(stored ?? {});
  },
  // 保存前重跑 schema，防止内部调用绕过 IPC 时写入不安全的超时或文件上限。
  saveKnowledgeImport: async (input: KnowledgeImportSettings) => {
    const parsed = knowledgeImportSettingsSchema.parse(input);
    await writeAiSetting("knowledge.import", parsed);
    return parsed;
  },
  // 对话服务只读获取当前聊天模型配置；密钥仅在主进程内解密并立即交给 Provider。
  getActiveChatConfig: async (): Promise<LlmProviderConfig | null> => {
    const stored = await getStoredLlmConfig();
    if (!stored) return null;
    return {
      baseURL: stored.baseURL,
      apiKey: stored.apiKey,
      model: stored.model,
      protocol: stored.protocol,
    };
  },
  // 保存 MySQL 配置、执行迁移并初始化连接池；这是 P0 数据源的原有入口。
  saveMysql: async (input: MysqlSettings) => {
    // 保存后立即建立连接并执行迁移，防止页面显示已保存但数据库尚不可用。
    await persistConnection(input);
    const pool = await configurePool(input);
    const migrations = await runMigrations(
      input,
      app.isPackaged
        ? join(process.resourcesPath, "migrations")
        : join(app.getAppPath(), "migrations"),
    );
    await pool.query(
      "INSERT INTO app_setting (`key`,value_json,updated_at) VALUES ('mysql.connection',?,?) ON DUPLICATE KEY UPDATE value_json=VALUES(value_json),updated_at=VALUES(updated_at)",
      [
        JSON.stringify({
          host: input.host,
          port: input.port,
          user: input.user,
          database: input.database,
          connectTimeout: input.connectTimeout,
          passwordEncrypted: true,
        }),
        utcNow(),
      ],
    );
    return { saved: true, migrations };
  },
  // 读取 MySQL 连接回显，始终把密码替换成固定掩码。
  getMysql: async () => {
    // 回显配置时掩码密码，防止渲染进程和界面日志接触明文凭据。
    const value = await loadConnection();
    return value
      ? { ...value, password: value.password ? "********" : "" }
      : null;
  },
  // 使用临时连接测试候选 MySQL 参数，不改变当前运行连接池。
  testMysql: async (input: MysqlSettings) => {
    // 测试使用临时连接而不污染运行连接池，并返回耗时供设置页诊断网络问题。
    const started = Date.now();
    const connection = await mysql.createConnection({
      ...input,
      timezone: "Z",
    });
    try {
      const [rows] = await connection.query<any[]>(
        "SELECT VERSION() AS version,1 AS healthy",
      );
      return {
        healthy: true,
        latencyMs: Date.now() - started,
        version: rows[0]!.version,
      };
    } finally {
      await connection.end();
    }
  },
  // 检查已配置连接池的实时状态，供设置页区分“已保存”和“当前可用”。
  health: async () => {
    // 健康检查只验证当前已配置的池连接，区分“配置可连”与“运行时仍可用”。
    const started = Date.now();
    const [rows] = await requirePool().query<any[]>(
      "SELECT VERSION() AS version",
    );
    return {
      healthy: true,
      latencyMs: Date.now() - started,
      version: rows[0]!.version,
    };
  },
  // 原子保存提醒偏好，保持提醒配置与 AI 配置互不覆盖。
  saveReminders: async (input: ReminderSettings) => {
    // 提醒偏好以单个 JSON 设置项原子覆盖，避免多列配置产生部分更新。
    await requirePool().query(
      "INSERT INTO app_setting (`key`,value_json,updated_at) VALUES ('notify.preferences',?,?) ON DUPLICATE KEY UPDATE value_json=VALUES(value_json),updated_at=VALUES(updated_at)",
      [JSON.stringify(input), utcNow()],
    );
    return input;
  },
  // 读取提醒偏好；旧库没有记录时返回完整默认对象。
  getReminders: async () => {
    // 无持久化偏好时返回完整默认值，保证旧库升级后界面字段不缺失。
    const [rows] = await requirePool().query<any[]>(
      "SELECT value_json AS valueJson FROM app_setting WHERE `key`='notify.preferences'",
    );
    return (
      rows[0]?.valueJson ?? {
        criticalEnabled: true,
        periodicEnabled: true,
        recommendationEnabled: false,
        frequency: "realtime",
        aggregationMinutes: 30,
        readRetentionDays: 30,
        recommendationRequiresConfirmation: true,
      }
    );
  },
  // 返回 AI 连接配置和供应商预设，API Key 仅以掩码形式跨 IPC 输出。
  getAiConfigs: async () => {
    // 读取配置时永远只返回掩码密钥，渲染层无法通过 IPC 取得明文。
    const llm = await readAiSetting("llm.chat");
    const embedding = await readAiSetting("llm.embedding");
    let maskedKey = "";
    if (llm?.apiKeyEncrypted && typeof llm.apiKeyEncrypted === "string") {
      try {
        // 只有主进程需要解密后才能生成首尾掩码，明文不会进入返回对象。
        maskedKey = maskApiKey(
          safeStorage.decryptString(Buffer.from(llm.apiKeyEncrypted, "base64")),
        );
      } catch {
        maskedKey = "********";
      }
    }
    return {
      llm: llm
        ? {
            provider: llm.provider,
            baseURL: llm.baseURL,
            model: llm.model,
            apiKey: maskedKey,
            protocol: llm.protocol,
          }
        : null,
      embedding: embedding
        ? { baseURL: embedding.baseURL, model: embedding.model }
        : null,
      providers: llmProviderPresets,
    };
  },
  // 校验、加密并保存 LLM 配置；保存动作不访问远端，连接测试由用户主动触发。
  saveLlmConfig: async (input: LlmSettings) => {
    // schema parse 在服务层再次执行，防止绕过 preload 的内部调用传入脏数据。
    const parsed = llmSettingsSchema.parse(input);
    // 只有存在密钥时才要求系统安全存储，Ollama 空密钥可以在无安全存储环境运行。
    let apiKeyEncrypted = "";
    if (parsed.apiKey && !parsed.apiKey.includes("****")) {
      if (!safeStorage.isEncryptionAvailable())
        throw Object.assign(
          new Error("系统安全存储当前不可用，不能保存 API Key"),
          { code: "AI_UNAVAILABLE" },
        );
      apiKeyEncrypted = safeStorage
        .encryptString(parsed.apiKey)
        .toString("base64");
    } else if (parsed.apiKey.includes("****")) {
      // 页面回显的掩码表示用户未修改密钥，此时沿用原密文而不是把掩码再次加密。
      const previous = await readAiSetting("llm.chat");
      apiKeyEncrypted =
        typeof previous?.apiKeyEncrypted === "string"
          ? previous.apiKeyEncrypted
          : "";
    }
    // 只写 provider、地址、模型和密文，绝不把明文 API Key 放入 JSON。
    await writeAiSetting("llm.chat", {
      provider: parsed.provider,
      baseURL: parsed.baseURL,
      model: parsed.model,
      protocol: parsed.protocol,
      apiKeyEncrypted,
    });
    return { saved: true, model: parsed.model };
  },
  // 保存 Ollama embedding 配置；保存动作不访问远端，连接测试由用户主动触发。
  saveEmbeddingConfig: async (input: EmbeddingSettings) => {
    const parsed = embeddingSettingsSchema.parse(input);
    await writeAiSetting("llm.embedding", parsed);
    return { saved: true, model: parsed.model };
  },
  // 用调用方提供的明文候选配置测试 LLM，测试过程不写入数据库。
  testLlmConnection: async (input: LlmSettings) => {
    const parsed = llmSettingsSchema.parse(input);
    const result = await createLlmProvider({
      baseURL: parsed.baseURL,
      apiKey: parsed.apiKey,
      model: parsed.model,
      protocol: parsed.protocol,
    }).healthCheck();
    return { ...result, model: parsed.model };
  },
  // 用候选 Ollama 地址和模型测试 embedding，并返回实际向量维度。
  testEmbeddingConnection: async (input: EmbeddingSettings) => {
    const parsed = embeddingSettingsSchema.parse(input);
    const result = await createEmbeddingProvider(parsed).healthCheck();
    return { ...result, model: parsed.model };
  },
  // 读取已保存配置并分别探测两个 provider，供设置页刷新健康徽标。
  getAiHealth: aiHealth,
  // 回显 Milvus 配置时固定掩码密码，主进程之外永远不可见明文。
  getMilvus: async () => {
    const value = await storedMilvusConfig();
    return value
      ? { ...value, password: value.password ? "********" : "" }
      : null;
  },
  // 用候选配置测试连接，不落盘，并返回版本、延迟和 collection 列表。
  testMilvus: async (input: MilvusSettings) => {
    const parsed = milvusSettingsSchema.parse(input);
    const started = Date.now();
    const client = await connectMilvus(parsed);
    try {
      const health = await client.checkHealth();
      if (health.isHealthy === false)
        throw Object.assign(new Error("Milvus 健康检查未通过"), {
          code: "VECTOR_DB_UNAVAILABLE",
        });
      let version: string | undefined;
      try {
        version = String(((await client.getVersion()) as any).version);
      } catch {
        /* 版本获取失败可省略。 */
      }
      const listed: any = await client.listCollections();
      const collections = (listed.data ?? [])
        .map((item: any) => (typeof item === "string" ? item : item.name))
        .filter(Boolean);
      return {
        ok: true,
        detail: "连接正常",
        version,
        latencyMs: Date.now() - started,
        collections,
      };
    } catch (error) {
      if ((error as { code?: string }).code === "VECTOR_DB_UNAVAILABLE")
        throw error;
      throw Object.assign(
        new Error("无法读取 Milvus 服务状态：请检查连接配置"),
        { code: "VECTOR_DB_UNAVAILABLE" },
      );
    } finally {
      closeMilvus(client);
    }
  },
  // 保存后立即测试；若 Embedding 可用则按实际维度创建唯一 collection。
  saveMilvus: async (input: MilvusSettings) => {
    const parsed = milvusSettingsSchema.parse(input);
    if (parsed.password && !safeStorage.isEncryptionAvailable())
      throw Object.assign(
        new Error("系统安全存储当前不可用，不能保存 Milvus 密码"),
        { code: "VECTOR_DB_UNAVAILABLE" },
      );
    const previous = await readMilvusSetting();
    let passwordEncrypted = "";
    if (parsed.password === "********" && previous?.passwordEncrypted)
      passwordEncrypted = String(previous.passwordEncrypted);
    else if (parsed.password)
      passwordEncrypted = safeStorage
        .encryptString(parsed.password)
        .toString("base64");
    await writeAiSetting("milvus.connection", {
      address: parsed.address,
      username: parsed.username,
      passwordEncrypted,
      ssl: parsed.ssl,
      connectTimeout: parsed.connectTimeout,
    });
    const runtime =
      parsed.password === "********" ? await storedMilvusConfig() : parsed;
    const client = await connectMilvus(runtime ?? parsed);
    try {
      const embedding = await getStoredEmbeddingConfig();
      if (embedding) {
        const check = await createEmbeddingProvider(embedding).healthCheck();
        if (check.ok && check.dim) {
          const storage = await settingsService.getKnowledgeStorage();
          await createMilvusRepository(client, storage.collectionName).ensureCollection(check.dim);
        }
      }
    } finally {
      closeMilvus(client);
    }
    return milvusHealth();
  },
  // 读取已保存配置并检查服务、collection、维度和向量数量。
  getMilvusHealth: milvusHealth,
  // 备份服务在主进程内读取解密配置，绝不通过 IPC 暴露该方法。
  getMilvusConnection: storedMilvusConfig,
};
