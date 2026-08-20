import { randomUUID } from "node:crypto";
import type { PoolConnection, RowDataPacket } from "mysql2/promise";
import type { GoalInput } from "../../../shared/contracts/entities.js";
import {
  calculateMilestoneProgress,
  calculateNumericProgress,
} from "../../../shared/domain/progress.js";
import { transitionGoal } from "../../../shared/domain/state-machines.js";
// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
import { inTransaction } from "../../infrastructure/db/transaction.js";
import {
  cleanupEntityLinks,
  replaceTags,
  requireEntity,
  toMysqlDateTime,
  utcNow,
} from "../common/database.js";

interface GoalRow extends RowDataPacket {
  id: string;
  metricType: "numeric" | "milestone" | "status";
  startValue: number | null;
  targetValue: number | null;
}

interface MilestoneRow extends RowDataPacket {
  isDone: number;
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function decorateGoal(
  connection: PoolConnection,
  goal: GoalRow,
): Promise<Record<string, unknown>> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const [tagRows] = await connection.query<RowDataPacket[]>(
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    "SELECT t.name FROM tag t JOIN entity_tag et ON et.tag_id=t.id WHERE et.entity_type='goal' AND et.entity_id=? ORDER BY t.name",
    [goal.id],
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const [records] = await connection.query<RowDataPacket[]>(
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    "SELECT id, value, note, recorded_at AS recordedAt, created_at AS createdAt FROM goal_record WHERE goal_id=? ORDER BY recorded_at ASC, created_at ASC",
    [goal.id],
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const [milestones] = await connection.query<MilestoneRow[]>(
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    "SELECT id, title, is_done AS isDone, done_at AS doneAt, sort_order AS sortOrder, created_at AS createdAt, updated_at AS updatedAt FROM milestone WHERE goal_id=? ORDER BY sort_order, created_at",
    [goal.id],
  );
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  let progress: number | null = null;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (goal.metricType === "numeric")
    progress = calculateNumericProgress(
      Number(goal.startValue),
      Number(goal.targetValue),
      records.at(-1)?.value == null ? undefined : Number(records.at(-1)!.value),
    );
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (goal.metricType === "milestone")
    progress = calculateMilestoneProgress(
      milestones.filter((item) => item.isDone === 1).length,
      milestones.length,
    );
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return {
    ...goal,
    tags: tagRows.map((row) => row.name),
    records,
    milestones,
    progress,
  };
}

// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
const goalSelect = `SELECT id, title, description, period, metric_type AS metricType, unit, start_value AS startValue,
  target_value AS targetValue, status, due_date AS dueDate, created_at AS createdAt, updated_at AS updatedAt FROM goal`;

export const goalService = {
  list: (filter: { status?: string; keyword?: string }) =>
    inTransaction(async (connection) => {
      const where: string[] = [];
      const values: unknown[] = [];
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (filter.status) {
        where.push("status=?");
        values.push(filter.status);
      }
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (filter.keyword) {
        where.push("title LIKE ?");
        values.push(`%${filter.keyword}%`);
      }
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        `${goalSelect} ${where.length ? `WHERE ${where.join(" AND ")}` : ""} ORDER BY due_date IS NULL, due_date, updated_at DESC`,
        values,
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return Promise.all(
        rows.map((row) => decorateGoal(connection, row as GoalRow)),
      );
    }),
  get: (id: string) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        `${goalSelect} WHERE id=?`,
        [id],
      );
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (!rows[0])
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("目标不存在"), { code: "NOT_FOUND" });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const goal = await decorateGoal(connection, rows[0] as GoalRow);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [projects] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT id,title,status FROM project WHERE goal_id=? ORDER BY updated_at DESC",
        [id],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [tasks] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT id,title,status,due_date AS dueDate FROM task WHERE goal_id=? ORDER BY due_date IS NULL,due_date",
        [id],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { ...goal, supportingActions: { projects, tasks } };
    }),
  create: (input: GoalInput) =>
    inTransaction(async (connection) => {
      const id = randomUUID();
      const now = utcNow();
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "INSERT INTO goal (id,title,description,period,metric_type,unit,start_value,target_value,status,due_date,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,'active',?,?,?)",
        [
          id,
          input.title,
          input.description,
          input.period,
          input.metricType,
          input.unit,
          input.startValue ?? null,
          input.targetValue ?? null,
          toMysqlDateTime(input.dueDate),
          now,
          now,
        ],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await replaceTags(connection, "goal", id, input.tags);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  update: (input: GoalInput & { id: string }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const goal = await requireEntity(connection, "goal", input.id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [recordCount] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT COUNT(*) AS count FROM goal_record WHERE goal_id=?",
        [input.id],
      );
      // 已结束目标只允许更新说明和标签，核心事实保持只读。
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (
        goal.status !== "active" &&
        (goal.title !== input.title ||
          goal.metric_type !== input.metricType ||
          goal.period !== input.period)
      )
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("已结束目标只能修改说明和标签"), {
          code: "INVALID_STATE",
        });
      // 有数值历史时修改起终点必须显式确认，避免用户未察觉地重算全部历史进度。
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (
        Number(recordCount[0]!.count) > 0 &&
        (Number(goal.start_value) !== input.startValue ||
          Number(goal.target_value) !== input.targetValue) &&
        !input.confirmRecalculate
      )
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(
          new Error("修改起点或目标值会重算历史进度，请确认"),
          { code: "CONFLICT" },
        );
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (
        goal.metric_type !== input.metricType &&
        Number(recordCount[0]!.count) > 0
      )
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("已有数据点时不能修改度量类型"), {
          code: "INVALID_STATE",
        });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "UPDATE goal SET title=?,description=?,period=?,metric_type=?,unit=?,start_value=?,target_value=?,due_date=?,updated_at=? WHERE id=?",
        [
          input.title,
          input.description,
          input.period,
          input.metricType,
          input.unit,
          input.startValue ?? null,
          input.targetValue ?? null,
          toMysqlDateTime(input.dueDate),
          utcNow(),
          input.id,
        ],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await replaceTags(connection, "goal", input.id, input.tags);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id: input.id };
    }),
  remove: (id: string) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await requireEntity(connection, "goal", id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await cleanupEntityLinks(connection, "goal", id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query("DELETE FROM goal WHERE id=?", [id]);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  finish: (id: string, status: "done" | "abandoned") =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const goal = await requireEntity(connection, "goal", id);
      transitionGoal(goal.status, status);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "UPDATE goal SET status=?,updated_at=? WHERE id=?",
        [status, utcNow(), id],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id, status };
    }),
  record: (input: {
    goalId: string;
    value: number;
    note: string | null;
    recordedAt: string;
  }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const goal = await requireEntity(connection, "goal", input.goalId);
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (goal.metric_type !== "numeric" || goal.status !== "active")
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("只有进行中的数值型目标可以记录数据"), {
          code: "INVALID_STATE",
        });
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (new Date(input.recordedAt).getTime() > Date.now())
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("记录时间不能晚于当前时间"), {
          code: "VALIDATION_ERROR",
        });
      const id = randomUUID();
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "INSERT INTO goal_record (id,goal_id,value,note,recorded_at,created_at) VALUES (?,?,?,?,?,?)",
        [
          id,
          input.goalId,
          input.value,
          input.note,
          toMysqlDateTime(input.recordedAt),
          utcNow(),
        ],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  addMilestone: (input: {
    goalId: string;
    title: string;
    sortOrder?: number;
  }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const goal = await requireEntity(connection, "goal", input.goalId);
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (goal.metric_type !== "milestone" || goal.status !== "active")
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("只有进行中的里程碑目标可以添加子项"), {
          code: "INVALID_STATE",
        });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT COALESCE(MAX(sort_order),-1)+1 AS nextOrder FROM milestone WHERE goal_id=?",
        [input.goalId],
      );
      const id = randomUUID();
      const now = utcNow();
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "INSERT INTO milestone (id,goal_id,title,is_done,done_at,sort_order,created_at,updated_at) VALUES (?,?,?,0,NULL,?,?,?)",
        [
          id,
          input.goalId,
          input.title,
          input.sortOrder ?? rows[0]!.nextOrder,
          now,
          now,
        ],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  updateMilestone: (input: { id: string; title: string; sortOrder: number }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT m.*,g.status AS goalStatus FROM milestone m JOIN goal g ON g.id=m.goal_id WHERE m.id=? FOR UPDATE",
        [input.id],
      );
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (!rows[0])
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("里程碑不存在"), { code: "NOT_FOUND" });
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (rows[0].goalStatus !== "active")
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("目标已结束"), { code: "INVALID_STATE" });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "UPDATE milestone SET title=?,sort_order=?,updated_at=? WHERE id=?",
        [input.title, input.sortOrder, utcNow(), input.id],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id: input.id };
    }),
  toggleMilestone: (input: { id: string; isDone: boolean }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT m.*,g.status AS goalStatus,g.metric_type AS metricType FROM milestone m JOIN goal g ON g.id=m.goal_id WHERE m.id=? FOR UPDATE",
        [input.id],
      );
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (!rows[0])
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("里程碑不存在"), { code: "NOT_FOUND" });
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (rows[0].goalStatus !== "active" || rows[0].metricType !== "milestone")
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("目标已结束或类型不正确"), {
          code: "INVALID_STATE",
        });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "UPDATE milestone SET is_done=?,done_at=?,updated_at=? WHERE id=?",
        [
          input.isDone ? 1 : 0,
          input.isDone ? utcNow() : null,
          utcNow(),
          input.id,
        ],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id: input.id, isDone: input.isDone };
    }),
  removeMilestone: (id: string) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT m.goal_id AS goalId,g.status FROM milestone m JOIN goal g ON g.id=m.goal_id WHERE m.id=? FOR UPDATE",
        [id],
      );
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (!rows[0])
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("里程碑不存在"), { code: "NOT_FOUND" });
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (rows[0].status !== "active")
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("目标已结束"), { code: "INVALID_STATE" });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query("DELETE FROM milestone WHERE id=?", [id]);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [remaining] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT id FROM milestone WHERE goal_id=? ORDER BY sort_order,created_at",
        [rows[0].goalId],
      );
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      for (const [index, item] of remaining.entries())
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await connection.query(
          // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
          "UPDATE milestone SET sort_order=?,updated_at=? WHERE id=?",
          [index, utcNow(), item.id],
        );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
};
