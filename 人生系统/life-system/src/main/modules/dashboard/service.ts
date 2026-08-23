import type { RowDataPacket } from "mysql2/promise";
import { inTransaction } from "../../infrastructure/db/transaction.js";
import {
  calculateMilestoneProgress,
  calculateNumericProgress,
} from "../../../shared/domain/progress.js";

export const dashboardService = {
  get: (today: string) =>
    // 首屏聚合固定以调用方传入的 UTC 日期计算，避免客户端时区改变“今天”的统计口径。
    inTransaction(async (connection) => {
      // 今日行动包含全部未完成待办中的今日到期项，以及所有习惯的今日状态；两种实体不混表。
      const [tasks] = await connection.query<RowDataPacket[]>(
        `SELECT id,title,status,due_date AS dueDate FROM task WHERE status<>'done' AND (due_date IS NULL OR DATE(due_date)<=?) ORDER BY due_date IS NULL,due_date,created_at DESC`,
        [today],
      );
      const [habits] = await connection.query<RowDataPacket[]>(
        `SELECT h.id,h.name,h.frequency_type AS frequencyType,h.weekly_target AS weeklyTarget,h.streak,EXISTS(SELECT 1 FROM habit_checkin hc WHERE hc.habit_id=h.id AND hc.checked_on=?) AS checkedToday FROM habit h ORDER BY h.updated_at DESC`,
        [today],
      );
      const [goalRows] = await connection.query<RowDataPacket[]>(
        `SELECT id,title,metric_type AS metricType,start_value AS startValue,target_value AS targetValue,status,due_date AS dueDate FROM goal WHERE status='active' ORDER BY due_date IS NULL,due_date,updated_at DESC LIMIT 6`,
      );
      const goals = [];
      for (const goal of goalRows) {
        let progress: number | null = null;
        if (goal.metricType === "numeric") {
          const [latest] = await connection.query<RowDataPacket[]>(
            "SELECT value FROM goal_record WHERE goal_id=? ORDER BY recorded_at DESC,created_at DESC LIMIT 1",
            [goal.id],
          );
          progress = calculateNumericProgress(
            Number(goal.startValue),
            Number(goal.targetValue),
            latest[0]?.value == null ? undefined : Number(latest[0].value),
          );
        }
        if (goal.metricType === "milestone") {
          const [counts] = await connection.query<RowDataPacket[]>(
            "SELECT COUNT(*) AS total,SUM(is_done) AS done FROM milestone WHERE goal_id=?",
            [goal.id],
          );
          progress = calculateMilestoneProgress(
            Number(counts[0]!.done ?? 0),
            Number(counts[0]!.total),
          );
        }
        goals.push({ ...goal, progress });
      }
      // P0 提醒在查询时聚合，不额外持久化；关闭关键提醒时立即返回空列表。
      const [settings] = await connection.query<RowDataPacket[]>(
        "SELECT value_json AS valueJson FROM app_setting WHERE `key`='notify.preferences'",
      );
      const preferences =
        typeof settings[0]?.valueJson === "string"
          ? JSON.parse(settings[0].valueJson)
          : settings[0]?.valueJson;
      const reminders: any[] = [];
      if (preferences?.criticalEnabled !== false) {
        for (const goal of goalRows) {
          if (goal.dueDate) {
            const days = Math.ceil(
              (new Date(goal.dueDate).getTime() -
                new Date(`${today}T00:00:00Z`).getTime()) /
                86400000,
            );
            if (days >= 0 && days <= 3)
              reminders.push({
                type: "goal_due",
                entityId: goal.id,
                title: `${goal.title}将在 ${days === 0 ? "今天" : `${days} 天后`}到期`,
              });
          }
          if (goal.metricType === "numeric") {
            const [last] = await connection.query<RowDataPacket[]>(
              "SELECT recorded_at AS recordedAt FROM goal_record WHERE goal_id=? ORDER BY recorded_at DESC LIMIT 1",
              [goal.id],
            );
            if (
              !last[0] ||
              Date.now() - new Date(last[0].recordedAt).getTime() > 7 * 86400000
            )
              reminders.push({
                type: "goal_record",
                entityId: goal.id,
                title: `${goal.title}该记录一次真实数据了`,
              });
          }
        }
        for (const habit of habits.filter((item) => !item.checkedToday))
          reminders.push({
            type: "habit",
            entityId: habit.id,
            title: `${habit.name}今天还未打卡`,
          });
      }
      return {
        tasks,
        habits,
        goals,
        reminders,
        summary: {
          taskCount: tasks.length,
          habitDone: habits.filter((item) => Boolean(item.checkedToday)).length,
          habitCount: habits.length,
        },
      };
    }),
};
