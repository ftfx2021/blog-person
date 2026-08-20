import { randomUUID } from "node:crypto";
import type { PoolConnection, RowDataPacket } from "mysql2/promise";
import type { HabitInput } from "../../../shared/contracts/entities.js";
import {
  calculateDailyStreak,
  calculateWeeklyStreak,
} from "../../../shared/domain/streak.js";
// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
import { inTransaction } from "../../infrastructure/db/transaction.js";
import { requireEntity, utcNow } from "../common/database.js";

interface HabitPersistenceRow {
  id: string;
  frequency_type: string;
  weekly_target: number | null;
}
interface CheckinRow extends RowDataPacket {
  checkedOn: string | Date;
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function recalculate(
  connection: PoolConnection,
  habit: HabitPersistenceRow,
): Promise<void> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const [rows] = await connection.query<CheckinRow[]>(
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    "SELECT checked_on AS checkedOn FROM habit_checkin WHERE habit_id=? ORDER BY checked_on DESC",
    [habit.id],
  );
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  const dates = rows.map((row) => String(row.checkedOn).slice(0, 10));
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  const streak =
    habit.frequency_type === "daily"
      ? calculateDailyStreak(dates)
      : calculateWeeklyStreak(dates, Number(habit.weekly_target));
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await connection.query(
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    "UPDATE habit SET streak=?,last_done_on=?,updated_at=? WHERE id=?",
    [streak, dates[0] ?? null, utcNow(), habit.id],
  );
}
const select =
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  "SELECT id,name,note,frequency_type AS frequencyType,weekly_target AS weeklyTarget,streak,last_done_on AS lastDoneOn,created_at AS createdAt,updated_at AS updatedAt FROM habit";
export const habitService = {
  list: () =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} ORDER BY updated_at DESC`,
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return rows;
    }),
  get: (id: string) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} WHERE id=?`,
        [id],
      );
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (!rows[0])
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("习惯不存在"), { code: "NOT_FOUND" });
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return rows[0];
    }),
  create: (input: HabitInput) =>
    inTransaction(async (connection) => {
      const id = randomUUID();
      const now = utcNow();
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "INSERT INTO habit (id,name,note,frequency_type,weekly_target,streak,last_done_on,created_at,updated_at) VALUES (?,?,?,?,?,0,NULL,?,?)",
        [
          id,
          input.name,
          input.note,
          input.frequencyType,
          input.weeklyTarget ?? null,
          now,
          now,
        ],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  update: (input: HabitInput & { id: string }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const habit = await requireEntity(connection, "habit", input.id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "UPDATE habit SET name=?,note=?,frequency_type=?,weekly_target=?,updated_at=? WHERE id=?",
        [
          input.name,
          input.note,
          input.frequencyType,
          input.weeklyTarget ?? null,
          utcNow(),
          input.id,
        ],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await recalculate(connection, {
        ...habit,
        frequency_type: input.frequencyType,
        weekly_target: input.weeklyTarget,
      });
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id: input.id };
    }),
  remove: (id: string) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await requireEntity(connection, "habit", id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query("DELETE FROM habit WHERE id=?", [id]);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  checkin: (input: { id: string; checkedOn: string; today: string }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const habit = await requireEntity(connection, "habit", input.id);
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (input.checkedOn > input.today)
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("不能补未来日期的打卡"), {
          code: "VALIDATION_ERROR",
        });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "INSERT INTO habit_checkin (id,habit_id,checked_on,created_at) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE habit_id=VALUES(habit_id)",
        [randomUUID(), input.id, input.checkedOn, utcNow()],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await recalculate(connection, habit);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id: input.id, checkedOn: input.checkedOn };
    }),
  undo: (input: { id: string; checkedOn: string; today: string }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const habit = await requireEntity(connection, "habit", input.id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "DELETE FROM habit_checkin WHERE habit_id=? AND checked_on=?",
        [input.id, input.checkedOn],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await recalculate(connection, habit);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id: input.id, checkedOn: input.checkedOn };
    }),
  history: (input: { id: string; from?: string; to?: string }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const habit = await requireEntity(connection, "habit", input.id);
      // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
      const where = ["habit_id=?"];
      const values: unknown[] = [input.id];
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (input.from) {
        where.push("checked_on>=?");
        values.push(input.from);
      }
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (input.to) {
        where.push("checked_on<=?");
        values.push(input.to);
      }
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        `SELECT id,checked_on AS checkedOn,created_at AS createdAt FROM habit_checkin WHERE ${where.join(" AND ")} ORDER BY checked_on DESC`,
        values,
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return {
        habit: {
          id: habit.id,
          name: habit.name,
          frequencyType: habit.frequency_type,
          weeklyTarget: habit.weekly_target,
          streak: habit.streak,
          lastDoneOn: habit.last_done_on,
        },
        checkins: rows,
      };
    }),
};
