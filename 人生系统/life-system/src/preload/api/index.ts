import { ipcRenderer } from "electron";
import { z, type ZodType } from "zod";
import {
  confirmationSchema,
  entityIdSchema,
} from "../../shared/contracts/common.js";
import {
  dashboardInputSchema,
  embeddingSettingsSchema,
  milvusSettingsSchema,
  emptySchema,
  exportSchema,
  mysqlSettingsSchema,
  llmSettingsSchema,
  reminderSettingsSchema,
  restoreSchema,
  searchInputSchema,
  knowledgeImportSettingsSchema,
  knowledgeStorageSettingsSchema,
  ocrSettingsSchema,
} from "../../shared/contracts/system.js";
import {
  goalInputSchema,
  goalListSchema,
  goalRecordInputSchema,
  goalUpdateSchema,
  habitCheckinSchema,
  habitHistorySchema,
  habitInputSchema,
  habitListSchema,
  habitStatusUpdateSchema,
  habitUpdateSchema,
  milestoneInputSchema,
  milestoneToggleSchema,
  milestoneUpdateSchema,
  projectInputSchema,
  projectListSchema,
  projectStatusUpdateSchema,
  projectUpdateSchema,
  taskInputSchema,
  taskListSchema,
  taskTransitionSchema,
  taskUpdateSchema,
} from "../../shared/contracts/entities.js";
import {
  assistantDeleteSchema,
  assistantInputSchema,
  assistantUpdateSchema,
  chatStartSchema,
  chatStopSchema,
  messageDeleteSchema,
  sessionClearSchema,
  sessionCreateSchema,
  sessionDeleteSchema,
  sessionExportSchema,
  sessionListSchema,
  sessionMessagesSchema,
  sessionPinSchema,
  sessionRegenerateSchema,
  sessionRenameSchema,
} from "../../shared/contracts/ai.js";
import {
  inboxActionSchema,
  inboxCreateSchema,
  inboxListSchema,
  inboxUpdateSchema,
} from "../../shared/contracts/inbox.js";
import { knowledgeBaseCreateSchema, knowledgeBaseUpdateSchema, knowledgeExportSchema, knowledgeFolderCreateSchema, knowledgeFolderListSchema, knowledgeFolderUpdateSchema, knowledgeImportFileSchema, knowledgeImportUrlSchema, knowledgeListSchema, knowledgeMoveSchema, knowledgeNoteSchema, knowledgeTagsSchema, knowledgeUpdateSchema, tagRenameSchema } from "../../shared/contracts/knowledge.js";

const idSchema = z.object({ id: entityIdSchema });
const finishSchema = z.object({
  id: entityIdSchema,
  status: z.enum(["done", "abandoned"]),
});

function invoke<T>(
  channel: string,
  schema: ZodType<T>,
  payload: T,
): Promise<any> {
  // channel 来自本文件的字面量而非渲染层参数，preload 因此仍保持最小权限边界。
  // Preload 在跨进程前再次校验参数；channel 只由闭包固定，渲染层不能构造任意通道。
  const validated = schema.parse(payload);
  // Vue reactive 对象是 Proxy，Electron structured clone 无法直接复制；JSON round-trip 生成纯 DTO。
  // 当前 IPC schema 只包含字符串、数字、布尔值、数组和普通对象，不会损失业务字段或日期类型。
  const serializable = JSON.parse(JSON.stringify(validated)) as T;
  return ipcRenderer.invoke(channel, serializable);
}

// 订阅函数只接收固定事件的 payload，并返回取消函数，避免渲染层接触 ipcRenderer 或 event 对象。
function subscribe<T>(
  channel: string,
  callback: (payload: T) => void,
): () => void {
  const handler = (_event: Electron.IpcRendererEvent, payload: T) =>
    callback(payload);
  ipcRenderer.on(channel, handler);
  return () => ipcRenderer.removeListener(channel, handler);
}

export const lifeSystemApi = {
  knowledge: {
    // 知识库方法为固定通道，渲染层无法访问文件系统或数据库连接。
    list: (input: unknown = {}) => invoke("knowledge:list", knowledgeListSchema, input as any),
    get: (id: string) => invoke("knowledge:get", idSchema, { id }),
    createNote: (input: unknown) => invoke("knowledge:createNote", knowledgeNoteSchema, input),
    update: (input: unknown) => invoke("knowledge:update", knowledgeUpdateSchema, input),
    remove: (id: string) => invoke("knowledge:remove", confirmationSchema, { id, confirmed: true }),
    restore: (id: string) => invoke("knowledge:restore", idSchema, { id }),
    setTags: (input: unknown) => invoke("knowledge:setTags", knowledgeTagsSchema, input),
    export: (input: unknown) => invoke("knowledge:export", knowledgeExportSchema, input),
    copyPrompt: (id: string) => invoke("knowledge:copyPrompt", idSchema, { id }),
    retryParse: (id: string) => invoke("knowledge:retryParse", idSchema, { id }),
    retryIndex: (id: string) => invoke("knowledge:retryIndex", idSchema, { id }),
    openOriginal: (id: string) => invoke("knowledge:openOriginal", idSchema, { id }),
    saveOriginal: (id: string) => invoke("knowledge:saveOriginal", idSchema, { id }),
    importFile: (input: unknown = {}) => invoke("knowledge:importFile", knowledgeImportFileSchema, typeof input === "string" ? { sourcePath: input } : input as any),
    importUrl: (input: unknown) => invoke("knowledge:importUrl", knowledgeImportUrlSchema, input),
    moveDocument: (input: unknown) => invoke("knowledge:move", knowledgeMoveSchema, input),
    kb: {
      list: () => invoke("knowledge:kb:list", emptySchema, {}),
      create: (input: unknown) => invoke("knowledge:kb:create", knowledgeBaseCreateSchema, input),
      update: (input: unknown) => invoke("knowledge:kb:update", knowledgeBaseUpdateSchema, input),
      remove: (id: string) => invoke("knowledge:kb:remove", idSchema, { id }),
    },
    folder: {
      list: (kbId: string) => invoke("knowledge:folder:list", knowledgeFolderListSchema, { kbId }),
      create: (input: unknown) => invoke("knowledge:folder:create", knowledgeFolderCreateSchema, input),
      update: (input: unknown) => invoke("knowledge:folder:update", knowledgeFolderUpdateSchema, input),
      remove: (id: string) => invoke("knowledge:folder:remove", idSchema, { id }),
    },
  },
  tags: {
    list: () => invoke("tag:list", emptySchema, {}),
    rename: (input: unknown) => invoke("tag:rename", tagRenameSchema, input),
    remove: (id: string) => invoke("tag:remove", idSchema, { id }),
  },
  inbox: {
    // 收藏箱入口固定在 preload，渲染层只能调用已校验的七个受控动作。
    list: (input: unknown = {}) =>
      invoke("inbox:list", inboxListSchema, input as any),
    create: (input: unknown) =>
      invoke("inbox:create", inboxCreateSchema, input),
    update: (input: unknown) =>
      invoke("inbox:update", inboxUpdateSchema, input),
    clip: (id: string) => invoke("inbox:clip", inboxActionSchema, { id }),
    keep: (id: string) => invoke("inbox:keep", inboxActionSchema, { id }),
    discard: (id: string) => invoke("inbox:discard", inboxActionSchema, { id }),
    restore: (id: string) => invoke("inbox:restore", inboxActionSchema, { id }),
  },
  goals: {
    // 目标列表接受可选筛选器，schema 为缺省值补全默认查询条件。
    list: (input: unknown = {}) =>
      invoke("goals:list", goalListSchema, input as any),
    // 单目标读取只透传 UUID，避免页面将任意 where 条件带进主进程。
    get: (id: string) => invoke("goals:get", idSchema, { id }),
    // 创建、更新分别使用输入和带 id 的契约，防止把更新误发到创建通道。
    // 创建目标的 schema 校验度量类型与数值字段的组合，避免无效公式进入服务层。
    create: (input: unknown) => invoke("goals:create", goalInputSchema, input),
    // 更新必须含实体 ID，历史重算确认标志也由该契约传递给服务层判断。
    update: (input: unknown) => invoke("goals:update", goalUpdateSchema, input),
    // 删除通道在 preload 固定 confirmed=true；真正的交互确认由页面完成。
    remove: (id: string) =>
      invoke("goals:delete", confirmationSchema, { id, confirmed: true }),
    // 完成动作仅允许 done 或 abandoned，不能通过 API 直接恢复结束目标。
    finish: (input: unknown) => invoke("goals:finish", finishSchema, input),
    // 记录和里程碑操作使用各自 schema，保持三类进度行为边界明确。
    // 数值记录要求带时区时间，主进程会进一步拒绝未来记录和非活跃目标。
    record: (input: unknown) =>
      invoke("goals:record", goalRecordInputSchema, input),
    // 创建里程碑受标题与可选排序号约束，防止不稳定顺序进入详情页。
    createMilestone: (input: unknown) =>
      invoke("goals:milestone:create", milestoneInputSchema, input),
    // 更新里程碑使用单独契约，避免错误复用目标更新字段。
    updateMilestone: (input: unknown) =>
      invoke("goals:milestone:update", milestoneUpdateSchema, input),
    // 复选开关只允许 boolean 完成态，完成时间由服务端生成而非页面指定。
    toggleMilestone: (input: unknown) =>
      invoke("goals:milestone:toggle", milestoneToggleSchema, input),
    // 删除里程碑同样固定确认字段，用户确认弹窗不被 preload 的调用细节绕过。
    removeMilestone: (id: string) =>
      invoke("goals:milestone:delete", confirmationSchema, {
        id,
        confirmed: true,
      }),
  },
  projects: {
    // 项目 API 暴露独立的状态更新通道，避免更新表单可以任意改写状态。
    list: (input: unknown = {}) =>
      invoke("projects:list", projectListSchema, input as any),
    // 项目详情只按受控 UUID 查询，不暴露原始 SQL 或数据库连接。
    get: (id: string) => invoke("projects:get", idSchema, { id }),
    // 项目创建会验证可选目标与时间范围，preload 不允许裸对象直接跨进程。
    create: (input: unknown) =>
      invoke("projects:create", projectInputSchema, input),
    // 项目更新的 ID 与业务字段在同一 schema 中解析，防止调用方错配实体和负载。
    update: (input: unknown) =>
      invoke("projects:update", projectUpdateSchema, input),
    // 项目删除只接收 UUID，清理关联的实际规则由主进程服务保持唯一实现。
    remove: (id: string) =>
      invoke("projects:delete", confirmationSchema, { id, confirmed: true }),
    // 状态更新不复用项目表单，明确要求 active、paused、done 枚举值。
    updateStatus: (input: unknown) =>
      invoke("projects:status", projectStatusUpdateSchema, input),
  },
  tasks: {
    // 待办 API 将“推进/撤销”建模为 action，不允许前端直接指定最终状态。
    list: (input: unknown = {}) =>
      invoke("tasks:list", taskListSchema, input as any),
    // 获取单条待办的输入契约与其他实体统一，避免 ID 格式漂移。
    get: (id: string) => invoke("tasks:get", idSchema, { id }),
    // 创建待办允许独立存在，也允许关联受控 UUID 的目标或项目。
    create: (input: unknown) => invoke("tasks:create", taskInputSchema, input),
    // 更新待办仍需走完整表单 schema，不能因快速编辑漏掉日期格式校验。
    update: (input: unknown) => invoke("tasks:update", taskUpdateSchema, input),
    // 删除待办固定为明确确认请求，主进程负责清除其多态关联。
    remove: (id: string) =>
      invoke("tasks:delete", confirmationSchema, { id, confirmed: true }),
    // transition 只接受 advance/undo；状态机拒绝从错误起点发起的动作。
    transition: (input: unknown) =>
      invoke("tasks:transition", taskTransitionSchema, input),
  },
  habits: {
    // 习惯打卡、撤销和历史读取分成三个受控通道，连续算法仍只在主进程执行。
    // 列表携带页面本地今天，服务端可一次投影今日状态而无需逐项查历史。
    list: (input: unknown = {}) =>
      invoke("habits:list", habitListSchema, input),
    // 单习惯读取统一传 UUID，不允许列表页面构造额外查询条件。
    get: (id: string) => invoke("habits:get", idSchema, { id }),
    // 创建习惯校验 daily 与 weeklyTarget 的互斥/依赖关系。
    create: (input: unknown) =>
      invoke("habits:create", habitInputSchema, input),
    // 修改频率后服务层重算历史 streak，页面不可自行写入连续天数。
    update: (input: unknown) =>
      invoke("habits:update", habitUpdateSchema, input),
    // 生命周期状态单独变更，避免编辑习惯时意外改变暂停或归档状态。
    updateStatus: (input: unknown) =>
      invoke("habits:status", habitStatusUpdateSchema, input),
    // 删除习惯仅交给服务层，数据库会同时处理对应的打卡历史。
    remove: (id: string) =>
      invoke("habits:delete", confirmationSchema, { id, confirmed: true }),
    // 打卡参数携带本地自然日，服务端将其与 today 比较以阻断未来补卡。
    checkin: (input: unknown) =>
      invoke("habits:checkin", habitCheckinSchema, input),
    // 撤销使用相同日期契约，服务端删除记录后从完整历史回算 streak。
    undo: (input: unknown) => invoke("habits:undo", habitCheckinSchema, input),
    // 历史查询允许可选日期范围，供日历展示而不开放任意 SQL 过滤表达式。
    history: (input: unknown) =>
      invoke("habits:history", habitHistorySchema, input),
  },
  dashboard: {
    // 仪表盘只接收标准化日期，主进程据此建立任务、习惯和提醒的同日快照。
    get: (input: unknown) =>
      invoke("dashboard:get", dashboardInputSchema, input),
  },
  search: {
    // 搜索传递结构化条件；表名和全文检索策略不暴露给渲染层。
    query: (input: unknown) => invoke("search:query", searchInputSchema, input),
  },
  settings: {
    knowledgeImport: {
      get: () => invoke("settings:getKnowledgeImport", emptySchema, {}),
      save: (input: unknown) => invoke("settings:saveKnowledgeImport", knowledgeImportSettingsSchema, input),
    },
    knowledgeStorage: {
      get: () => invoke("settings:knowledge-storage:get", emptySchema, {}),
      save: (input: unknown) => invoke("settings:knowledge-storage:save", knowledgeStorageSettingsSchema, input),
      migrate: () => invoke("settings:knowledge-storage:migrate", emptySchema, {}),
    },
    ocr: {
      get: () => invoke("settings:ocr:get", emptySchema, {}),
      save: (input: unknown) => invoke("settings:ocr:save", ocrSettingsSchema, input),
      test: (input: unknown) => invoke("settings:ocr:test", ocrSettingsSchema, input),
    },
    // 设置通道只返回脱敏连接信息，密码写入与加密由主进程安全存储负责。
    // 获取连接设置只返回脱敏密码，避免 API 成为明文凭据读取能力。
    getMysql: () => invoke("settings:mysql:get", emptySchema, {}),
    // 保存连接会触发主进程迁移和连接池重建，不能跳过 MySQL schema 校验。
    saveMysql: (input: unknown) =>
      invoke("settings:mysql:save", mysqlSettingsSchema, input),
    // 测试使用临时连接，允许用户验证未保存的表单输入。
    testMysql: (input: unknown) =>
      invoke("settings:mysql:test", mysqlSettingsSchema, input),
    // 健康检查针对当前运行连接池，和测试候选配置的用途不同。
    health: () => invoke("settings:mysql:health", emptySchema, {}),
    // 获取提醒偏好没有输入，严格空 schema 禁止调用方夹带未知设置键。
    getReminders: () => invoke("settings:reminders:get", emptySchema, {}),
    // 提醒偏好使用独立 schema，保存时不会携带数据库或 AI 配置。
    saveReminders: (input: unknown) =>
      invoke("settings:reminders:save", reminderSettingsSchema, input),
    // Milvus 四个窄方法分别覆盖脱敏读取、保存、候选测试和实时健康检查。
    milvus: {
      get: () => invoke("settings:milvus:get", emptySchema, {}),
      save: (input: unknown) =>
        invoke("settings:milvus:save", milvusSettingsSchema, input),
      test: (input: unknown) =>
        invoke("settings:milvus:test", milvusSettingsSchema, input),
      health: () => invoke("settings:milvus:health", emptySchema, {}),
    },
    // 读取 AI 配置使用空参数，避免渲染层选择任意配置文件或密钥来源。
    getAiConfigs: () => invoke("settings:getAiConfigs", emptySchema, {}),
    // 保存聊天模型配置前校验 provider、端点和模型名，密钥由设置服务自行保护。
    saveLlmConfig: (input: unknown) =>
      invoke("settings:saveLlmConfig", llmSettingsSchema, input),
    // Embedding 配置与聊天配置分离，避免一项保存覆盖另一项的运行参数。
    saveEmbeddingConfig: (input: unknown) =>
      invoke("settings:saveEmbeddingConfig", embeddingSettingsSchema, input),
    // 聊天连接测试针对候选值运行，不以当前健康状态替代真实连通性验证。
    testLlmConnection: (input: unknown) =>
      invoke("settings:testLlmConnection", llmSettingsSchema, input),
    // 向量连接测试采用专用 schema，确保不误把聊天模型字段传给 embedding 服务。
    testEmbeddingConnection: (input: unknown) =>
      invoke(
        "settings:testEmbeddingConnection",
        embeddingSettingsSchema,
        input,
      ),
    // 健康状态只读汇总，供设置界面展示当前运行依赖而不是配置草稿。
    getAiHealth: () => invoke("settings:getAiHealth", emptySchema, {}),
  },
  ai: {
    // 发起聊天只传输已校验的会话标识和文本，主进程负责上下文与密钥。
    start: (input: unknown) => invoke("ai:chat:start", chatStartSchema, input),
    // 停止请求只操作当前会话，不暴露 AbortController 或原生 IPC。
    stop: (input: unknown) => invoke("ai:chat:stop", chatStopSchema, input),
    // 固定事件白名单；组件卸载时调用返回值解除监听，防止重复订阅和内存泄漏。
    onDelta: (
      callback: (payload: { sessionId: string; delta: string }) => void,
    ) => subscribe("ai:chat:delta", callback),
    onReasoning: (
      callback: (payload: { sessionId: string; delta: string }) => void,
    ) => subscribe("ai:chat:reasoning", callback),
    onDone: (
      callback: (payload: {
        sessionId: string;
        fullText: string;
        model?: string;
        reasoning?: string;
        usage?: {
          promptTokens: number;
          completionTokens: number;
          reasoningTokens?: number;
        };
        aborted?: boolean;
      }) => void,
    ) => subscribe("ai:chat:done", callback),
    onError: (
      callback: (payload: {
        sessionId: string;
        code: string;
        message: string;
      }) => void,
    ) => subscribe("ai:chat:error", callback),
  },
  assistant: {
    // 助手 CRUD 通过固定 channel 暴露，渲染层不能访问数据库或任意设置键。
    list: () => invoke("assistant:list", emptySchema, {}),
    create: (input: unknown) =>
      invoke("assistant:create", assistantInputSchema, input),
    update: (input: unknown) =>
      invoke("assistant:update", assistantUpdateSchema, input),
    remove: (id: string) =>
      invoke("assistant:remove", assistantDeleteSchema, {
        id,
        confirmed: true,
      }),
  },
  sessions: {
    // 会话始终绑定助手，切换助手时页面需重新加载对应会话列表。
    list: (input: unknown) => invoke("session:list", sessionListSchema, input),
    create: (input: unknown) =>
      invoke("session:create", sessionCreateSchema, input),
    rename: (input: unknown) =>
      invoke("session:rename", sessionRenameSchema, input),
    remove: (id: string) =>
      invoke("session:remove", sessionDeleteSchema, { id, confirmed: true }),
    messages: (input: unknown) =>
      invoke("session:messages", sessionMessagesSchema, input),
    removeMessage: (input: unknown) =>
      invoke("messages:remove", messageDeleteSchema, input),
    regenerate: (input: unknown) =>
      invoke("sessions:regenerate", sessionRegenerateSchema, input),
    clear: (id: string) =>
      invoke("sessions:clear", sessionClearSchema, { id, confirmed: true }),
    pin: (input: unknown) => invoke("sessions:pin", sessionPinSchema, input),
    export: (id: string) =>
      invoke("sessions:export", sessionExportSchema, { id }),
    // 标题由主进程后台生成后广播，页面只订阅固定 DTO，不发起轮询。
    onTitleUpdated: (callback: (payload: { id: string; title: string }) => void) =>
      subscribe("sessions:title-updated", callback),
  },
  backup: {
    // 备份 API 的恢复输入必须通过 confirmation schema，文件路径选择也由主进程所有。
    // 创建备份没有业务参数，目录、命名和完整性摘要均由主进程决定。
    create: () => invoke("backup:create", emptySchema, {}),
    // 恢复必须通过 manifest 路径和固定确认短语两项校验。
    restore: (input: unknown) => invoke("backup:restore", restoreSchema, input),
    // 导出格式限于白名单，防止渲染层指定任意文件扩展名或路径。
    export: (input: unknown) => invoke("backup:export", exportSchema, input),
    // 任务列表只读，用于呈现主进程广播的备份、恢复和导出进度。
    tasks: () => invoke("backup:tasks", emptySchema, {}),
  },
};

export type LifeSystemApi = typeof lifeSystemApi;
