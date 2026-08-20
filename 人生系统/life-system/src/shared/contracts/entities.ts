import { z } from "zod";
import {
  entityIdSchema,
  localDateSchema,
  nullableEntityIdSchema,
  utcDateTimeSchema,
} from "./common.js";

export const goalPeriodSchema = z.enum(["annual", "quarterly", "monthly"]);
export const goalMetricTypeSchema = z.enum(["numeric", "milestone", "status"]);
export const goalStatusSchema = z.enum(["active", "done", "abandoned"]);
export const projectStatusSchema = z.enum(["active", "done", "paused"]);
export const taskStatusSchema = z.enum(["todo", "doing", "done"]);
export const habitFrequencySchema = z.enum(["daily", "weekly_times"]);

const optionalText = z
  .string()
  .trim()
  .nullish()
  .transform((value) => value || null);
const optionalDateTime = utcDateTimeSchema
  .nullish()
  .transform((value) => value || null);

export const goalInputSchema = z
  .object({
    title: z.string().trim().min(1).max(50),
    description: optionalText,
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
    tags: z.array(z.string().trim().min(1).max(100)).max(20).default([]),
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
export const goalRecordInputSchema = z.object({
  goalId: entityIdSchema,
  value: z.number().finite(),
  note: optionalText,
  recordedAt: utcDateTimeSchema,
});
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

export const taskInputSchema = z.object({
  title: z.string().trim().min(1).max(100),
  note: optionalText,
  dueDate: optionalDateTime,
  goalId: nullableEntityIdSchema,
  projectId: nullableEntityIdSchema,
});
export const taskUpdateSchema = taskInputSchema.and(
  z.object({ id: entityIdSchema }),
);
export const taskListSchema = z
  .object({
    status: taskStatusSchema.optional(),
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
