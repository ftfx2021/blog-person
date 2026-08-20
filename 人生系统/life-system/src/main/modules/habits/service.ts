import { randomUUID } from "node:crypto";
import type { PoolConnection, RowDataPacket } from "mysql2/promise";
import type { HabitInput } from "../../../shared/contracts/entities.js";
import {
  calculateDailyStreak,
  calculateWeeklyStreak,
} from "../../../shared/domain/streak.js";
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
async function recalculate(
  connection: PoolConnection,
  habit: HabitPersistenceRow,
): Promise<void> {
  const [rows] = await connection.query<CheckinRow[]>(
    "SELECT checked_on AS checkedOn FROM habit_checkin WHERE habit_id=? ORDER BY checked_on DESC",
    [habit.id],
  );
  const dates = rows.map((row) => String(row.checkedOn).slice(0, 10));
  const streak =
    habit.frequency_type === "daily"
      ? calculateDailyStreak(dates)
      : calculateWeeklyStreak(dates, Number(habit.weekly_target));
  await connection.query(
    "UPDATE habit SET streak=?,last_done_on=?,updated_at=? WHERE id=?",
    [streak, dates[0] ?? null, utcNow(), habit.id],
  );
}
const select =
  "SELECT id,name,note,frequency_type AS frequencyType,weekly_target AS weeklyTarget,streak,last_done_on AS lastDoneOn,created_at AS createdAt,updated_at AS updatedAt FROM habit";
export const habitService = {
  list: () =>
    inTransaction(async (connection) => {
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} ORDER BY updated_at DESC`,
      );
      return rows;
    }),
  get: (id: string) =>
    inTransaction(async (connection) => {
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} WHERE id=?`,
        [id],
      );
      if (!rows[0])
        throw Object.assign(new Error("习惯不存在"), { code: "NOT_FOUND" });
      return rows[0];
    }),
  create: (input: HabitInput) =>
    inTransaction(async (connection) => {
      const id = randomUUID();
      const now = utcNow();
      await connection.query(
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
      return { id };
    }),
  update: (input: HabitInput & { id: string }) =>
    inTransaction(async (connection) => {
      const habit = await requireEntity(connection, "habit", input.id);
      await connection.query(
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
      await recalculate(connection, {
        ...habit,
        frequency_type: input.frequencyType,
        weekly_target: input.weeklyTarget,
      });
      return { id: input.id };
    }),
  remove: (id: string) =>
    inTransaction(async (connection) => {
      await requireEntity(connection, "habit", id);
      await connection.query("DELETE FROM habit WHERE id=?", [id]);
      return { id };
    }),
  checkin: (input: { id: string; checkedOn: string; today: string }) =>
    inTransaction(async (connection) => {
      const habit = await requireEntity(connection, "habit", input.id);
      if (input.checkedOn > input.today)
        throw Object.assign(new Error("不能补未来日期的打卡"), {
          code: "VALIDATION_ERROR",
        });
      await connection.query(
        "INSERT INTO habit_checkin (id,habit_id,checked_on,created_at) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE habit_id=VALUES(habit_id)",
        [randomUUID(), input.id, input.checkedOn, utcNow()],
      );
      await recalculate(connection, habit);
      return { id: input.id, checkedOn: input.checkedOn };
    }),
  undo: (input: { id: string; checkedOn: string; today: string }) =>
    inTransaction(async (connection) => {
      const habit = await requireEntity(connection, "habit", input.id);
      await connection.query(
        "DELETE FROM habit_checkin WHERE habit_id=? AND checked_on=?",
        [input.id, input.checkedOn],
      );
      await recalculate(connection, habit);
      return { id: input.id, checkedOn: input.checkedOn };
    }),
  history: (input: { id: string; from?: string; to?: string }) =>
    inTransaction(async (connection) => {
      const habit = await requireEntity(connection, "habit", input.id);
      const where = ["habit_id=?"];
      const values: unknown[] = [input.id];
      if (input.from) {
        where.push("checked_on>=?");
        values.push(input.from);
      }
      if (input.to) {
        where.push("checked_on<=?");
        values.push(input.to);
      }
      const [rows] = await connection.query<RowDataPacket[]>(
        `SELECT id,checked_on AS checkedOn,created_at AS createdAt FROM habit_checkin WHERE ${where.join(" AND ")} ORDER BY checked_on DESC`,
        values,
      );
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
