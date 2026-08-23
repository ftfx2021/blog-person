import { z } from "zod";
import {
  entityIdSchema,
  localDateSchema,
  nullableEntityIdSchema,
  utcDateTimeSchema,
} from "./common.js";

// 目标周期仅描述规划视角，不参与进度公式或到期日期的推算。
export const goalPeriodSchema = z.enum(["annual", "quarterly", "monthly"]);
// 度量类型决定目标进度的唯一来源，数值、里程碑和状态不能混用。
export const goalMetricTypeSchema = z.enum(["numeric", "milestone", "status"]);
// 目标结束态不可逆，服务层用该枚举配合状态机验证流转。
export const goalStatusSchema = z.enum(["active", "done", "abandoned"]);
// 项目允许暂停和恢复，完成后由领域规则阻止回到执行状态。
export const projectStatusSchema = z.enum(["active", "done", "paused"]);
// 待办状态只能按 todo、doing、done 的流程推进，不能直接任意赋值。
export const taskStatusSchema = z.enum(["todo", "doing", "done"]);
// 时间范围只是用户归类，不强制绑定到截止日期或状态流转。
export const taskPeriodSchema = z.enum([
  "day",
  "week",
  "month",
  "semester",
  "other",
]);
// 习惯频率决定 streak 按自然日还是按达到次数的自然周计算。
export const habitFrequencySchema = z.enum(["daily", "weekly_times"]);
export const habitStatusSchema = z.enum(["active", "paused", "archived"]);

// 可选文本在边界处统一 trim 并转 null，数据库无需再区分空串与未填写。
const optionalText = z
  .string()
  .trim()
  .nullish()
  .transform((value) => value || null);
// 可选时间必须先是带 offset 的 ISO 值，再归一为 null 或交给主进程转 UTC DATETIME。
const optionalDateTime = utcDateTimeSchema
  .nullish()
  .transform((value) => value || null);

// 目标输入契约同时适用于创建与更新，superRefine 负责跨字段的度量一致性。
export const goalInputSchema = z
  .object({
    // 标题是目标的稳定人类标识，空白和过长内容在 IPC 边界拒绝。
    title: z.string().trim().min(1).max(50),
    description: optionalText,
    // 未选择周期时默认季度，避免创建记录缺少筛选和展示维度。
    period: goalPeriodSchema.default("quarterly"),
    metricType: goalMetricTypeSchema,
    unit: z
      .string()
      .trim()
      .max(32)
      .nullish()
      .transform((value) => value || null),
    startValue: z.number().finite().nullable().optional(),
    targetValue: z.number().finite().nullable().optional(),
    dueDate: optionalDateTime,
    // 标签数量设上限，防止把自由输入意外提交成超大关联集合。
    tags: z.array(z.string().trim().min(1).max(100)).max(20).default([]),
    // 改变已有数值目标的公式参数时，服务层依赖此显式确认防止误重算。
    confirmRecalculate: z.boolean().default(false),
  })
  .superRefine((value, context) => {
    const numeric = value.metricType === "numeric";
    if (
      numeric &&
      (value.startValue == null ||
        value.targetValue == null ||
        value.startValue === value.targetValue)
    ) {
      context.addIssue({
        code: "custom",
        message: "数值型目标必须填写不同的起点值和目标值",
      });
    }
    if (!numeric && (value.startValue != null || value.targetValue != null)) {
      context.addIssue({
        code: "custom",
        message: "非数值型目标不能填写起点值和目标值",
      });
    }
  });
export const goalUpdateSchema = goalInputSchema.and(
  z.object({ id: entityIdSchema }),
);
export const goalListSchema = z
  .object({
    status: goalStatusSchema.optional(),
    keyword: z.string().trim().max(100).optional(),
  })
  .default({});
// 真实数据点独立于目标本体保存，进度永远取时间线中的最新有效记录。
export const goalRecordInputSchema = z.object({
  goalId: entityIdSchema,
  value: z.number().finite(),
  note: optionalText,
  recordedAt: utcDateTimeSchema,
});
// 里程碑创建允许省略排序，服务端会以同一目标内的末尾顺序补齐。
export const milestoneInputSchema = z.object({
  goalId: entityIdSchema,
  title: z.string().trim().min(1).max(100),
  sortOrder: z.number().int().min(0).optional(),
});
export const milestoneUpdateSchema = z.object({
  id: entityIdSchema,
  title: z.string().trim().min(1).max(100),
  sortOrder: z.number().int().min(0),
});
export const milestoneToggleSchema = z.object({
  id: entityIdSchema,
  isDone: z.boolean(),
});

// 项目输入可选地支持目标，用于组织行动而不把项目本身误当成目标进度。
export const projectInputSchema = z
  .object({
    title: z.string().trim().min(1).max(50),
    description: optionalText,
    goalId: nullableEntityIdSchema,
    startAt: optionalDateTime,
    endAt: optionalDateTime,
    tags: z.array(z.string().trim().min(1).max(100)).max(20).default([]),
  })
  .refine(
    (value) => !value.startAt || !value.endAt || value.endAt >= value.startAt,
    { message: "结束时间不能早于开始时间" },
  );
export const projectUpdateSchema = projectInputSchema.and(
  z.object({ id: entityIdSchema }),
);
export const projectListSchema = z
  .object({
    status: projectStatusSchema.optional(),
    goalId: entityIdSchema.optional(),
  })
  .default({});
export const projectStatusUpdateSchema = z.object({
  id: entityIdSchema,
  status: projectStatusSchema,
});

// 待办可关联目标或项目，但二者均可为空，保持独立行动的使用场景。
export const taskInputSchema = z.object({
  title: z.string().trim().min(1).max(100),
  note: optionalText,
  dueDate: optionalDateTime,
  period: taskPeriodSchema.default("other"),
  goalId: nullableEntityIdSchema,
  projectId: nullableEntityIdSchema,
});
export const taskUpdateSchema = taskInputSchema.and(
  z.object({ id: entityIdSchema }),
);
export const taskListSchema = z
  .object({
    status: taskStatusSchema.optional(),
    period: taskPeriodSchema.optional(),
    goalId: entityIdSchema.optional(),
    projectId: entityIdSchema.optional(),
    dateFrom: utcDateTimeSchema.optional(),
    dateTo: utcDateTimeSchema.optional(),
    sort: z.enum(["due_asc", "created_desc"]).default("due_asc"),
  })
  .default({ sort: "due_asc" });
export const taskTransitionSchema = z.object({
  id: entityIdSchema,
  action: z.enum(["advance", "undo"]),
});

// 习惯输入的频率与周目标必须成对合法，避免连续算法收到无意义的阈值。
export const habitInputSchema = z
  .object({
    name: z.string().trim().min(1).max(50),
    note: optionalText,
    frequencyType: habitFrequencySchema.default("daily"),
    weeklyTarget: z.number().int().min(1).max(7).nullable().optional(),
  })
  .superRefine((value, context) => {
    if (value.frequencyType === "daily" && value.weeklyTarget != null)
      context.addIssue({ code: "custom", message: "每日习惯不能设置每周次数" });
    if (value.frequencyType === "weekly_times" && value.weeklyTarget == null)
      context.addIssue({ code: "custom", message: "每周习惯必须设置目标次数" });
  });
export const habitUpdateSchema = habitInputSchema.and(
  z.object({ id: entityIdSchema }),
);
export const habitListSchema = z
  .object({
    status: habitStatusSchema.optional(),
    includeArchived: z.boolean().default(false),
    today: localDateSchema.optional(),
  })
  .default({ includeArchived: false });
export const habitStatusUpdateSchema = z.object({
  id: entityIdSchema,
  status: habitStatusSchema,
});
// 打卡请求携带 checkedOn 与今天，服务端据此拒绝未来日期而不信任客户端时钟以外的意图。
export const habitCheckinSchema = z.object({
  id: entityIdSchema,
  checkedOn: localDateSchema,
  today: localDateSchema,
});
export const habitHistorySchema = z.object({
  id: entityIdSchema,
  from: localDateSchema.optional(),
  to: localDateSchema.optional(),
});

export type GoalInput = z.infer<typeof goalInputSchema>;
export type ProjectInput = z.infer<typeof projectInputSchema>;
export type TaskInput = z.infer<typeof taskInputSchema>;
export type HabitInput = z.infer<typeof habitInputSchema>;
// 四类 P0 实体的输入 schema 同时承担边界校验和前后端契约说明。
