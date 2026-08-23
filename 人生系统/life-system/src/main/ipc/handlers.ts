import { z } from "zod";
import { dialog } from "electron";
import {
  confirmationSchema,
  entityIdSchema,
} from "../../shared/contracts/common.js";
import {
  dashboardInputSchema,
  embeddingSettingsSchema,
  milvusSettingsSchema,
  emptySchema,
  llmSettingsSchema,
  mysqlSettingsSchema,
  reminderSettingsSchema,
  knowledgeImportSettingsSchema,
  knowledgeStorageSettingsSchema,
  ocrSettingsSchema,
  searchInputSchema,
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
import { goalService } from "../modules/goals/service.js";
import { projectService } from "../modules/projects/service.js";
import { taskService } from "../modules/tasks/service.js";
import { habitService } from "../modules/habits/service.js";
import { dashboardService } from "../modules/dashboard/service.js";
import { searchService } from "../modules/search/service.js";
import { settingsService } from "../modules/settings/service.js";
// P0 业务通道集中声明，使 preload 暴露面与主进程服务入口可审计。
// 每个通道绑定输入 schema，禁止渲染层绕过校验直接调用业务服务。
// handler 只做请求解构和路由，不承载业务规则或数据库访问。
import { registerHandler } from "./register.js";
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
import { chatService } from "../modules/chat/service.js";
import { assistantService } from "../modules/assistant/service.js";
import {
  inboxActionSchema,
  inboxCreateSchema,
  inboxListSchema,
  inboxUpdateSchema,
} from "../../shared/contracts/inbox.js";
import { inboxService } from "../modules/knowledge/inbox.js";
import { knowledgeService } from "../modules/knowledge/index.js";
import { documentsService } from "../modules/knowledge/documents.js";
import { knowledgeBaseCreateSchema, knowledgeBaseUpdateSchema, knowledgeExportSchema, knowledgeFolderCreateSchema, knowledgeFolderListSchema, knowledgeFolderUpdateSchema, knowledgeImportFileSchema, knowledgeImportUrlSchema, knowledgeListSchema, knowledgeMoveSchema, knowledgeNoteSchema, knowledgeTagsSchema, knowledgeUpdateSchema, tagRenameSchema } from "../../shared/contracts/knowledge.js";
import { knowledgeBaseService } from "../modules/knowledge/kb.js";
import { tagService } from "../modules/knowledge/tags.js";
import { migrateVectorsV1toV2 } from "../modules/knowledge/vector-migration.js";

const id = z.object({ id: entityIdSchema });
const finish = z.object({
  id: entityIdSchema,
  status: z.enum(["done", "abandoned"]),
});
export function registerP0Handlers(): void {
  // 知识库所有写入进入 documentsService 或 knowledgeService，IPC 不直接访问数据库。
  registerHandler("knowledge:list", knowledgeListSchema, async (input) => ({ items: await documentsService.list(input) }));
  registerHandler("knowledge:get", id, ({ id }) => documentsService.get(id));
  registerHandler("knowledge:createNote", knowledgeNoteSchema, documentsService.createNote);
  registerHandler("knowledge:update", knowledgeUpdateSchema, ({ id, ...input }) => documentsService.update(id, input));
  registerHandler("knowledge:remove", confirmationSchema, ({ id }) => documentsService.remove(id));
  registerHandler("knowledge:restore", id, ({ id }) => documentsService.restore(id));
  registerHandler("knowledge:setTags", knowledgeTagsSchema, ({ id, tags }) => documentsService.setTags(id, tags));
  registerHandler("knowledge:export", knowledgeExportSchema, ({ id, format }) => documentsService.export(id, format));
  registerHandler("knowledge:copyPrompt", id, ({ id }) => documentsService.copyPrompt(id));
  registerHandler("knowledge:retryParse", id, ({ id }) => documentsService.retryParse(id));
  registerHandler("knowledge:retryIndex", id, ({ id }) => documentsService.retryIndex(id));
  registerHandler("knowledge:openOriginal", id, ({ id }) => documentsService.openOriginal(id));
  registerHandler("knowledge:saveOriginal", id, ({ id }) => documentsService.saveOriginal(id));
  registerHandler("knowledge:move", knowledgeMoveSchema, documentsService.moveDocument);
  // 知识库和目录操作都在主进程服务层执行，避免页面直接拼接树形 SQL。
  registerHandler("knowledge:kb:list", emptySchema, knowledgeBaseService.list);
  registerHandler("knowledge:kb:create", knowledgeBaseCreateSchema, knowledgeBaseService.create);
  registerHandler("knowledge:kb:update", knowledgeBaseUpdateSchema, knowledgeBaseService.update);
  registerHandler("knowledge:kb:remove", id, ({ id }) => knowledgeBaseService.remove(id));
  registerHandler("knowledge:folder:list", knowledgeFolderListSchema, ({ kbId }) => knowledgeBaseService.folders(kbId));
  registerHandler("knowledge:folder:create", knowledgeFolderCreateSchema, knowledgeBaseService.createFolder);
  registerHandler("knowledge:folder:update", knowledgeFolderUpdateSchema, knowledgeBaseService.updateFolder);
  registerHandler("knowledge:folder:remove", id, ({ id }) => knowledgeBaseService.removeFolder(id));
  registerHandler("tag:list", emptySchema, tagService.list);
  registerHandler("tag:rename", tagRenameSchema, ({ id, name }) => tagService.rename(id, name));
  registerHandler("tag:remove", id, ({ id }) => tagService.remove(id));
  registerHandler("knowledge:importFile", knowledgeImportFileSchema, async ({ sourcePath, kbId, folderId }, event) => {
    // 文件对话框只能在主进程打开，渲染层只得到最终受控路径或取消结果。
    const selectedPath = sourcePath ?? (event ? (await dialog.showOpenDialog({
      properties: ["openFile"],
      filters: [{ name: "支持的文档", extensions: ["pdf", "docx", "md", "markdown", "html", "htm", "txt"] }],
    })).filePaths[0] : undefined);
    if (!selectedPath) throw Object.assign(new Error("未选择导入文件"), { code: "VALIDATION_ERROR" });
    return knowledgeService.importFile(selectedPath, { kbId, folderId });
  });
  registerHandler("knowledge:importUrl", knowledgeImportUrlSchema, ({ url, title, kbId, folderId }) => knowledgeService.ingest({ title: title ?? url, docType: "webpage", sourceUrl: url, kbId, folderId }));
  registerHandler("settings:saveKnowledgeImport", knowledgeImportSettingsSchema, settingsService.saveKnowledgeImport);
  registerHandler("settings:getKnowledgeImport", emptySchema, settingsService.getKnowledgeImport);
  registerHandler("settings:knowledge-storage:get", emptySchema, settingsService.getKnowledgeStorage);
  registerHandler("settings:knowledge-storage:save", knowledgeStorageSettingsSchema, settingsService.saveKnowledgeStorage);
  registerHandler("settings:knowledge-storage:migrate", emptySchema, migrateVectorsV1toV2);
  registerHandler("settings:ocr:get", emptySchema, settingsService.getOcrDisplay);
  registerHandler("settings:ocr:save", ocrSettingsSchema, settingsService.saveOcr);
  registerHandler("settings:ocr:test", ocrSettingsSchema, settingsService.testOcr);
  // 收藏箱通道只接收共享 Zod 契约，页面不能以裸 IPC 形式传入任意数据库字段。
  registerHandler("inbox:list", inboxListSchema, async (input) => ({
    items: await inboxService.list(input),
  }));
  registerHandler("inbox:create", inboxCreateSchema, inboxService.create);
  registerHandler("inbox:update", inboxUpdateSchema, inboxService.update);
  registerHandler("inbox:clip", inboxActionSchema, inboxService.clip);
  registerHandler("inbox:keep", inboxActionSchema, inboxService.keep);
  registerHandler("inbox:discard", inboxActionSchema, inboxService.discard);
  registerHandler("inbox:restore", inboxActionSchema, inboxService.restore);
  // 注册目标 CRUD、状态结束、真实数据记录和里程碑操作；各项仍由对应服务验证状态。
  registerHandler("goals:list", goalListSchema, goalService.list);
  registerHandler("goals:get", id, ({ id }) => goalService.get(id));
  registerHandler("goals:create", goalInputSchema, goalService.create);
  registerHandler("goals:update", goalUpdateSchema, goalService.update);
  registerHandler("goals:delete", confirmationSchema, ({ id }) =>
    goalService.remove(id),
  );
  registerHandler("goals:finish", finish, ({ id, status }) =>
    goalService.finish(id, status),
  );
  registerHandler("goals:record", goalRecordInputSchema, goalService.record);
  registerHandler(
    "goals:milestone:create",
    milestoneInputSchema,
    goalService.addMilestone,
  );
  registerHandler(
    "goals:milestone:update",
    milestoneUpdateSchema,
    goalService.updateMilestone,
  );
  registerHandler(
    "goals:milestone:toggle",
    milestoneToggleSchema,
    goalService.toggleMilestone,
  );
  registerHandler("goals:milestone:delete", confirmationSchema, ({ id }) =>
    goalService.removeMilestone(id),
  );
  registerHandler("projects:list", projectListSchema, projectService.list);
  // 注册项目读取、写入、删除和状态流转，防止页面直接访问项目服务。
  registerHandler("projects:get", id, ({ id }) => projectService.get(id));
  registerHandler("projects:create", projectInputSchema, projectService.create);
  registerHandler(
    "projects:update",
    projectUpdateSchema,
    projectService.update,
  );
  registerHandler("projects:delete", confirmationSchema, ({ id }) =>
    projectService.remove(id),
  );
  registerHandler(
    "projects:status",
    projectStatusUpdateSchema,
    projectService.updateStatus,
  );
  registerHandler("tasks:list", taskListSchema, taskService.list);
  // 注册待办操作；推进和撤销统一以 action 形式交给任务状态机。
  registerHandler("tasks:get", id, ({ id }) => taskService.get(id));
  registerHandler("tasks:create", taskInputSchema, taskService.create);
  registerHandler("tasks:update", taskUpdateSchema, taskService.update);
  registerHandler("tasks:delete", confirmationSchema, ({ id }) =>
    taskService.remove(id),
  );
  registerHandler(
    "tasks:transition",
    taskTransitionSchema,
    taskService.transition,
  );
  registerHandler("habits:list", habitListSchema, habitService.list);
  // 注册习惯定义、日期打卡和历史读取；日期边界由服务层执行校验。
  registerHandler("habits:get", id, ({ id }) => habitService.get(id));
  registerHandler("habits:create", habitInputSchema, habitService.create);
  registerHandler("habits:update", habitUpdateSchema, habitService.update);
  registerHandler(
    "habits:status",
    habitStatusUpdateSchema,
    habitService.updateStatus,
  );
  registerHandler("habits:delete", confirmationSchema, ({ id }) =>
    habitService.remove(id),
  );
  registerHandler("habits:checkin", habitCheckinSchema, habitService.checkin);
  registerHandler("habits:undo", habitCheckinSchema, habitService.undo);
  registerHandler("habits:history", habitHistorySchema, habitService.history);
  registerHandler("dashboard:get", dashboardInputSchema, ({ today }) =>
    // 仪表盘只接受显式 today，保证主进程聚合使用可复现的日期口径。
    dashboardService.get(today),
  );
  registerHandler("search:query", searchInputSchema, searchService.search);
  // 设置通道分别处理连接、提醒和可选能力状态，敏感密码不会直接回传。
  registerHandler("settings:mysql:get", emptySchema, settingsService.getMysql);
  registerHandler(
    "settings:mysql:save",
    mysqlSettingsSchema,
    settingsService.saveMysql,
  );
  registerHandler(
    "settings:mysql:test",
    mysqlSettingsSchema,
    settingsService.testMysql,
  );
  registerHandler("settings:mysql:health", emptySchema, settingsService.health);
  registerHandler(
    "settings:reminders:get",
    emptySchema,
    settingsService.getReminders,
  );
  registerHandler(
    "settings:reminders:save",
    reminderSettingsSchema,
    settingsService.saveReminders,
  );
  registerHandler(
    "settings:milvus:get",
    emptySchema,
    settingsService.getMilvus,
  );
  registerHandler(
    "settings:milvus:save",
    milvusSettingsSchema,
    settingsService.saveMilvus,
  );
  registerHandler(
    "settings:milvus:test",
    milvusSettingsSchema,
    settingsService.testMilvus,
  );
  registerHandler(
    "settings:milvus:health",
    emptySchema,
    settingsService.getMilvusHealth,
  );
  registerHandler(
    "settings:getAiConfigs",
    emptySchema,
    settingsService.getAiConfigs,
  );
  registerHandler(
    "settings:saveLlmConfig",
    llmSettingsSchema,
    settingsService.saveLlmConfig,
  );
  registerHandler(
    "settings:saveEmbeddingConfig",
    embeddingSettingsSchema,
    settingsService.saveEmbeddingConfig,
  );
  registerHandler(
    "settings:testLlmConnection",
    llmSettingsSchema,
    settingsService.testLlmConnection,
  );
  registerHandler(
    "settings:testEmbeddingConnection",
    embeddingSettingsSchema,
    settingsService.testEmbeddingConnection,
  );
  registerHandler(
    "settings:getAiHealth",
    emptySchema,
    settingsService.getAiHealth,
  );
  // 对话请求绑定 event.sender，只向发起请求的窗口推送增量，避免跨窗口泄露内容。
  registerHandler("ai:chat:start", chatStartSchema, (input, event) => {
    if (!event) throw new Error("IPC 窗口上下文缺失");
    return chatService.start(input, event.sender);
  });
  registerHandler("ai:chat:stop", chatStopSchema, ({ sessionId }) =>
    Promise.resolve(chatService.stop(sessionId)),
  );
  // 助手管理通道只返回业务 DTO，敏感配置和未来预留字段不会由页面自行写入。
  registerHandler("assistant:list", emptySchema, async () => ({
    assistants: await assistantService.list(),
  }));
  registerHandler(
    "assistant:create",
    assistantInputSchema,
    assistantService.create,
  );
  registerHandler(
    "assistant:update",
    assistantUpdateSchema,
    assistantService.update,
  );
  registerHandler("assistant:remove", assistantDeleteSchema, ({ id }) =>
    assistantService.remove(id),
  );
  // 会话通道按助手隔离，服务层会在查询前确认父助手存在。
  registerHandler(
    "session:list",
    sessionListSchema,
    async ({ assistantId }) => ({
      sessions: await assistantService.sessions(assistantId),
    }),
  );
  registerHandler("session:create", sessionCreateSchema, ({ assistantId }) =>
    assistantService.createSession(assistantId),
  );
  registerHandler("session:rename", sessionRenameSchema, ({ id, title }) =>
    assistantService.renameSession(id, title),
  );
  registerHandler("session:remove", sessionDeleteSchema, ({ id }) =>
    assistantService.removeSession(id),
  );
  registerHandler(
    "session:messages",
    sessionMessagesSchema,
    ({ sessionId, limit, offset }) =>
      assistantService.messages(sessionId, limit, offset),
  );
  registerHandler("messages:remove", messageDeleteSchema, ({ id, sessionId }) =>
    assistantService.removeLastMessage(id, sessionId),
  );
  registerHandler(
    "sessions:regenerate",
    sessionRegenerateSchema,
    ({ sessionId }) =>
      assistantService
        .removeLastAssistantMessage(sessionId)
        .then((message) => ({ message })),
  );
  registerHandler("sessions:clear", sessionClearSchema, ({ id }) =>
    assistantService.clearSession(id),
  );
  registerHandler("sessions:pin", sessionPinSchema, ({ id, pinned }) =>
    assistantService.pinSession(id, pinned),
  );
  registerHandler("sessions:export", sessionExportSchema, ({ id }) =>
    assistantService.exportSessionMarkdown(id),
  );
}
