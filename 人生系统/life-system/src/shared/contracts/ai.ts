import { z } from "zod";
import { confirmationSchema, entityIdSchema } from "./common.js";

// AI 对话输入限制长度与会话标识，防止空请求和超大上下文进入主进程。
export const chatStartSchema = z.object({
  sessionId: entityIdSchema,
  message: z.string().trim().min(1).max(8000),
  // 思考模式只控制展示与持久化，不向不同供应商传递不统一的模型参数。
  think: z.boolean().optional().default(false),
});

// 停止只允许操作合法 UUID，会话内容由主进程内存持有。
export const chatStopSchema = z.object({ sessionId: z.string().uuid() });

export type ChatStartInput = z.infer<typeof chatStartSchema>;
export type ChatStopInput = z.infer<typeof chatStopSchema>;

// 助手输入只包含本期明确开放的三项配置，系统提示词允许为空。
export const assistantInputSchema = z.object({
  name: z.string().trim().min(1).max(50),
  description: z.string().trim().max(200).optional().default(""),
  systemPrompt: z.string().max(4000).optional().default(""),
});

// 更新使用可选字段，服务层要求至少有一项实际变更。
export const assistantUpdateSchema = z.object({
  id: entityIdSchema,
  name: z.string().trim().min(1).max(50).optional(),
  description: z.string().trim().max(200).optional(),
  systemPrompt: z.string().max(4000).optional(),
});

export const assistantDeleteSchema = confirmationSchema;
export const sessionCreateSchema = z.object({ assistantId: entityIdSchema });
export const sessionRenameSchema = z.object({
  id: entityIdSchema,
  title: z.string().trim().min(1).max(100),
});
export const sessionListSchema = z.object({ assistantId: entityIdSchema });
export const sessionDeleteSchema = confirmationSchema;
export const sessionMessagesSchema = z.object({
  sessionId: entityIdSchema,
  limit: z.number().int().min(1).max(200).default(50),
  offset: z.number().int().min(0).default(0),
});

// 删除消息只允许携带会话归属，服务层还会限制只能删除最后一条消息。
export const messageDeleteSchema = z.object({
  id: entityIdSchema,
  sessionId: entityIdSchema,
  confirmed: z.literal(true),
});
export const sessionRegenerateSchema = z.object({ sessionId: entityIdSchema });
export const sessionClearSchema = z.object({
  id: entityIdSchema,
  confirmed: z.literal(true),
});
export const sessionPinSchema = z.object({
  id: entityIdSchema,
  pinned: z.boolean(),
});
export const sessionExportSchema = z.object({ id: entityIdSchema });

export type AssistantInput = z.infer<typeof assistantInputSchema>;
export type AssistantUpdate = z.infer<typeof assistantUpdateSchema>;
