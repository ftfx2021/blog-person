import type { RowDataPacket } from "mysql2/promise";
// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
import { inTransaction } from "../../infrastructure/db/transaction.js";
import {
  calculateMilestoneProgress,
  calculateNumericProgress,
} from "../../../shared/domain/progress.js";

export const dashboardService = {
  get: (today: string) =>
    inTransaction(async (connection) => {
      // 今日行动包含全部未完成待办中的今日到期项，以及所有习惯的今日状态；两种实体不混表。
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [tasks] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        `SELECT id,title,status,due_date AS dueDate FROM task WHERE status<>'done' AND (due_date IS NULL OR DATE(due_date)<=?) ORDER BY due_date IS NULL,due_date,created_at DESC`,
        [today],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [habits] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        `SELECT h.id,h.name,h.frequency_type AS frequencyType,h.weekly_target AS weeklyTarget,h.streak,EXISTS(SELECT 1 FROM habit_checkin hc WHERE hc.habit_id=h.id AND hc.checked_on=?) AS checkedToday FROM habit h ORDER BY h.updated_at DESC`,
        [today],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [goalRows] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        `SELECT id,title,metric_type AS metricType,start_value AS startValue,target_value AS targetValue,status,due_date AS dueDate FROM goal WHERE status='active' ORDER BY due_date IS NULL,due_date,updated_at DESC LIMIT 6`,
      );
      // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
      const goals = [];
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      for (const goal of goalRows) {
        // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
        let progress: number | null = null;
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        if (goal.metricType === "numeric") {
          // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
          const [latest] = await connection.query<RowDataPacket[]>(
            // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
            "SELECT value FROM goal_record WHERE goal_id=? ORDER BY recorded_at DESC,created_at DESC LIMIT 1",
            [goal.id],
          );
          progress = calculateNumericProgress(
            Number(goal.startValue),
            Number(goal.targetValue),
            latest[0]?.value == null ? undefined : Number(latest[0].value),
          );
        }
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        if (goal.metricType === "milestone") {
          // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
          const [counts] = await connection.query<RowDataPacket[]>(
            // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
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
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [settings] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT value_json AS valueJson FROM app_setting WHERE `key`='notify.preferences'",
      );
      const preferences =
        typeof settings[0]?.valueJson === "string"
          ? JSON.parse(settings[0].valueJson)
          : settings[0]?.valueJson;
      const reminders: any[] = [];
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (preferences?.criticalEnabled !== false) {
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        for (const goal of goalRows) {
          // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
          if (goal.dueDate) {
            const days = Math.ceil(
              (new Date(goal.dueDate).getTime() -
                new Date(`${today}T00:00:00Z`).getTime()) /
                86400000,
            );
            // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
            if (days >= 0 && days <= 3)
              reminders.push({
                type: "goal_due",
                entityId: goal.id,
                title: `${goal.title}将在 ${days === 0 ? "今天" : `${days} 天后`}到期`,
              });
          }
          // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
          if (goal.metricType === "numeric") {
            // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
            const [last] = await connection.query<RowDataPacket[]>(
              // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
              "SELECT recorded_at AS recordedAt FROM goal_record WHERE goal_id=? ORDER BY recorded_at DESC LIMIT 1",
              [goal.id],
            );
            // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
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
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        for (const habit of habits.filter((item) => !item.checkedToday))
          reminders.push({
            type: "habit",
            entityId: habit.id,
            title: `${habit.name}今天还未打卡`,
          });
      }
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
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
