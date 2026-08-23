import { z } from "zod";
import { entityIdSchema } from "./common.js";

// 收藏类型限制在三种轻量暂存形态，避免页面传入无法落库的自由字符串。
export const inboxKindSchema = z.enum(["link", "snippet", "read_later"]);
// 收藏状态由主进程服务驱动，页面只能通过动作通道改变状态。
export const inboxStatusSchema = z.enum([
  "pending",
  "clipped",
  "bookmarked",
  "discarded",
]);
// 新增收藏只做字段形状校验，URL 协议与跨字段规则由服务层再次确认。
export const inboxCreateSchema = z.object({
  kind: inboxKindSchema,
  title: z.string().trim().min(1).max(200),
  url: z.string().trim().optional(),
  note: z.string().trim().max(2000).optional(),
});
// 编辑收藏要求稳定 ID，其余字段可按需修改但至少由服务层检查最终状态。
export const inboxUpdateSchema = z.object({
  id: entityIdSchema,
  kind: inboxKindSchema.optional(),
  title: z.string().trim().min(1).max(200).optional(),
  url: z.string().trim().optional(),
  note: z.string().trim().max(2000).optional(),
});
// 列表筛选只允许类型和状态两个白名单字段，避免暴露任意 SQL 条件。
export const inboxListSchema = z.object({
  kind: inboxKindSchema.optional(),
  status: inboxStatusSchema.optional(),
});
// 四种收藏动作共用同一个 UUID 负载契约。
export const inboxActionSchema = z.object({ id: entityIdSchema });
