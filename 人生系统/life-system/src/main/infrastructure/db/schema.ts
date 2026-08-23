import {
  char,
  date,
  datetime,
  double,
  int,
  json,
  mysqlTable,
  mediumtext,
  primaryKey,
  text,
  tinyint,
  uniqueIndex,
  varchar,
} from "drizzle-orm/mysql-core";

// Drizzle schema 用于类型安全查询；特殊 CHECK、FULLTEXT 与补挂外键由手写迁移负责。
export const goals = mysqlTable("goal", {
  id: char("id", { length: 36 }).primaryKey(),
  title: varchar("title", { length: 50 }).notNull(),
  description: text("description"),
  period: varchar("period", { length: 16 }).notNull(),
  metricType: varchar("metric_type", { length: 16 }).notNull(),
  unit: varchar("unit", { length: 32 }),
  startValue: double("start_value"),
  targetValue: double("target_value"),
  status: varchar("status", { length: 16 }).notNull(),
  dueDate: datetime("due_date", { mode: "string", fsp: 3 }),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
});
export const goalRecords = mysqlTable("goal_record", {
  id: char("id", { length: 36 }).primaryKey(),
  goalId: char("goal_id", { length: 36 }).notNull(),
  value: double("value").notNull(),
  note: text("note"),
  recordedAt: datetime("recorded_at", { mode: "string", fsp: 3 }).notNull(),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
});
export const milestones = mysqlTable("milestone", {
  id: char("id", { length: 36 }).primaryKey(),
  goalId: char("goal_id", { length: 36 }).notNull(),
  title: varchar("title", { length: 100 }).notNull(),
  isDone: tinyint("is_done").notNull(),
  doneAt: datetime("done_at", { mode: "string", fsp: 3 }),
  sortOrder: int("sort_order").notNull(),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
});
export const projects = mysqlTable("project", {
  id: char("id", { length: 36 }).primaryKey(),
  goalId: char("goal_id", { length: 36 }),
  title: varchar("title", { length: 50 }).notNull(),
  description: text("description"),
  status: varchar("status", { length: 16 }).notNull(),
  startAt: datetime("start_at", { mode: "string", fsp: 3 }),
  endAt: datetime("end_at", { mode: "string", fsp: 3 }),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
});
export const tasks = mysqlTable("task", {
  id: char("id", { length: 36 }).primaryKey(),
  goalId: char("goal_id", { length: 36 }),
  projectId: char("project_id", { length: 36 }),
  period: varchar("period", { length: 16 }).notNull().default("other"),
  title: varchar("title", { length: 100 }).notNull(),
  note: text("note"),
  dueDate: datetime("due_date", { mode: "string", fsp: 3 }),
  status: varchar("status", { length: 16 }).notNull(),
  completedAt: datetime("completed_at", { mode: "string", fsp: 3 }),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
});
export const habits = mysqlTable("habit", {
  id: char("id", { length: 36 }).primaryKey(),
  name: varchar("name", { length: 50 }).notNull(),
  note: text("note"),
  frequencyType: varchar("frequency_type", { length: 20 }).notNull(),
  weeklyTarget: int("weekly_target"),
  streak: int("streak").notNull(),
  lastDoneOn: date("last_done_on", { mode: "string" }),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
});
export const habitCheckins = mysqlTable(
  "habit_checkin",
  {
    id: char("id", { length: 36 }).primaryKey(),
    habitId: char("habit_id", { length: 36 }).notNull(),
    checkedOn: date("checked_on", { mode: "string" }).notNull(),
    createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  },
  (table) => [
    uniqueIndex("uk_habit_checkin_habit_day").on(
      table.habitId,
      table.checkedOn,
    ),
  ],
);
export const tags = mysqlTable(
  "tag",
  {
    id: char("id", { length: 36 }).primaryKey(),
    name: varchar("name", { length: 100 }).notNull(),
    createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  },
  (table) => [uniqueIndex("uk_tag_name").on(table.name)],
);
export const entityTags = mysqlTable(
  "entity_tag",
  {
    entityType: varchar("entity_type", { length: 32 }).notNull(),
    entityId: char("entity_id", { length: 36 }).notNull(),
    tagId: char("tag_id", { length: 36 }).notNull(),
  },
  (table) => [
    primaryKey({ columns: [table.entityType, table.entityId, table.tagId] }),
  ],
);
// 多知识库结构由迁移 006 建立，Drizzle 同步声明核心字段供后续类型安全查询使用。
export const knowledgeBases = mysqlTable("knowledge_base", {
  id: char("id", { length: 36 }).primaryKey(),
  name: varchar("name", { length: 50 }).notNull(),
  description: text("description"),
  color: varchar("color", { length: 16 }),
  sort: int("sort").notNull(),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
});
export const knowledgeFolders = mysqlTable("knowledge_folder", {
  id: char("id", { length: 36 }).primaryKey(),
  kbId: char("kb_id", { length: 36 }).notNull(),
  parentId: char("parent_id", { length: 36 }),
  name: varchar("name", { length: 50 }).notNull(),
  sort: int("sort").notNull(),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
});
export const appSettings = mysqlTable("app_setting", {
  key: varchar("key", { length: 128 }).primaryKey(),
  valueJson: json("value_json").notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
});
// AI 助手仅启用名称、简介和系统提示词，模型与知识库字段保留给未来路线。
export const aiAssistants = mysqlTable("ai_assistant", {
  id: char("id", { length: 36 }).primaryKey(),
  name: varchar("name", { length: 50 }).notNull(),
  description: varchar("description", { length: 200 }),
  systemPrompt: text("system_prompt").notNull(),
  modelConfigJson: json("model_config_json"),
  knowledgeBaseId: char("knowledge_base_id", { length: 36 }),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
});
// AI 会话属于一个助手；删除助手由数据库级联清理会话和消息。
export const aiSessions = mysqlTable("ai_session", {
  id: char("id", { length: 36 }).primaryKey(),
  assistantId: char("assistant_id", { length: 36 }).notNull(),
  title: varchar("title", { length: 100 }).notNull().default("新会话"),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
  updatedAt: datetime("updated_at", { mode: "string", fsp: 3 }).notNull(),
  pinned: tinyint("pinned").notNull().default(0),
});
// AI 消息内容使用 MEDIUMTEXT，以支持较长回答；角色 CHECK 由迁移保证。
export const aiMessages = mysqlTable("ai_message", {
  id: char("id", { length: 36 }).primaryKey(),
  sessionId: char("session_id", { length: 36 }).notNull(),
  role: varchar("role", { length: 16 }).notNull(),
  content: mediumtext("content").notNull(),
  reasoning: mediumtext("reasoning"),
  createdAt: datetime("created_at", { mode: "string", fsp: 3 }).notNull(),
});
// Drizzle 类型定义与手写迁移共享同一字段事实源，特殊约束仍由 SQL 保证。
