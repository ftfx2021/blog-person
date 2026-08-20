import { z } from "zod";
import {
  confirmationSchema,
  entityIdSchema,
} from "../../shared/contracts/common.js";
import {
  dashboardInputSchema,
  emptySchema,
  mysqlSettingsSchema,
  reminderSettingsSchema,
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
import { registerHandler } from "./register.js";

const id = z.object({ id: entityIdSchema });
const finish = z.object({
  id: entityIdSchema,
  status: z.enum(["done", "abandoned"]),
});
export function registerP0Handlers(): void {
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
  registerHandler("habits:list", emptySchema, habitService.list);
  registerHandler("habits:get", id, ({ id }) => habitService.get(id));
  registerHandler("habits:create", habitInputSchema, habitService.create);
  registerHandler("habits:update", habitUpdateSchema, habitService.update);
  registerHandler("habits:delete", confirmationSchema, ({ id }) =>
    habitService.remove(id),
  );
  registerHandler("habits:checkin", habitCheckinSchema, habitService.checkin);
  registerHandler("habits:undo", habitCheckinSchema, habitService.undo);
  registerHandler("habits:history", habitHistorySchema, habitService.history);
  registerHandler("dashboard:get", dashboardInputSchema, ({ today }) =>
    dashboardService.get(today),
  );
  registerHandler("search:query", searchInputSchema, searchService.search);
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
    "settings:milvus:status",
    emptySchema,
    settingsService.milvusStatus,
  );
}
