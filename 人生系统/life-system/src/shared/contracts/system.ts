import { z } from "zod";
import { localDateSchema } from "./common.js";

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
export const reminderSettingsSchema = z.object({
  criticalEnabled: z.boolean().default(true),
  periodicEnabled: z.boolean().default(true),
  recommendationEnabled: z.boolean().default(false),
  frequency: z.enum(["realtime", "daily"]).default("realtime"),
  aggregationMinutes: z.number().int().min(0).max(1440).default(30),
  readRetentionDays: z.number().int().min(1).max(365).default(30),
  recommendationRequiresConfirmation: z.literal(true).default(true),
});
export const searchInputSchema = z.object({
  keyword: z.string().trim().min(1).max(100),
  types: z
    .array(z.enum(["goal", "project", "task", "habit", "document"]))
    .default([]),
  tags: z.array(z.string().trim().min(1)).default([]),
  status: z.string().trim().optional(),
});
export const dashboardInputSchema = z.object({ today: localDateSchema });
export const exportSchema = z.object({
  format: z.enum(["json", "markdown", "txt"]),
});
export const restoreSchema = z.object({
  manifestPath: z.string().min(1),
  confirmation: z.literal("恢复"),
});
export const emptySchema = z.object({}).strict();

export type MysqlSettings = z.infer<typeof mysqlSettingsSchema>;
export type ReminderSettings = z.infer<typeof reminderSettingsSchema>;
