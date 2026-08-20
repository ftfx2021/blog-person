import { randomUUID } from "node:crypto";
import type { RowDataPacket } from "mysql2/promise";
import type { TaskInput } from "../../../shared/contracts/entities.js";
import { nextTaskStatus } from "../../../shared/domain/state-machines.js";
// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
import { inTransaction } from "../../infrastructure/db/transaction.js";
import {
  cleanupEntityLinks,
  requireEntity,
  toMysqlDateTime,
  utcNow,
} from "../common/database.js";

// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
const select = `SELECT t.id,t.goal_id AS goalId,t.project_id AS projectId,t.title,t.note,t.due_date AS dueDate,t.status,t.completed_at AS completedAt,t.created_at AS createdAt,t.updated_at AS updatedAt,g.title AS goalTitle,p.title AS projectTitle FROM task t LEFT JOIN goal g ON g.id=t.goal_id LEFT JOIN project p ON p.id=t.project_id`;
export const taskService = {
  list: (filter: {
    status?: string;
    goalId?: string;
    projectId?: string;
    dateFrom?: string;
    dateTo?: string;
    sort: string;
  }) =>
    inTransaction(async (connection) => {
      const where: string[] = [];
      const values: unknown[] = [];
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      for (const [field, value] of [
        ["t.status", filter.status],
        ["t.goal_id", filter.goalId],
        ["t.project_id", filter.projectId],
      ] as const)
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        if (value) {
          where.push(`${field}=?`);
          values.push(value);
        }
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (filter.dateFrom) {
        where.push("t.due_date>=?");
        values.push(toMysqlDateTime(filter.dateFrom));
      }
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (filter.dateTo) {
        where.push("t.due_date<=?");
        values.push(toMysqlDateTime(filter.dateTo));
      }
      const order =
        filter.sort === "created_desc"
          ? "t.created_at DESC"
          : "t.due_date IS NULL,t.due_date,t.created_at DESC";
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} ${where.length ? `WHERE ${where.join(" AND ")}` : ""} ORDER BY ${order}`,
        values,
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return rows;
    }),
  get: (id: string) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} WHERE t.id=?`,
        [id],
      );
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (!rows[0])
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("待办不存在"), { code: "NOT_FOUND" });
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return rows[0];
    }),
  create: (input: TaskInput) =>
    inTransaction(async (connection) => {
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (input.goalId) await requireEntity(connection, "goal", input.goalId);
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (input.projectId)
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await requireEntity(connection, "project", input.projectId);
      const id = randomUUID();
      const now = utcNow();
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "INSERT INTO task (id,goal_id,project_id,title,note,due_date,status,completed_at,created_at,updated_at) VALUES (?,?,?,?,?,?,'todo',NULL,?,?)",
        [
          id,
          input.goalId ?? null,
          input.projectId ?? null,
          input.title,
          input.note,
          toMysqlDateTime(input.dueDate),
          now,
          now,
        ],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  update: (input: TaskInput & { id: string }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const task = await requireEntity(connection, "task", input.id);
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (task.status === "done")
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("已完成待办请先撤销完成再编辑"), {
          code: "INVALID_STATE",
        });
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (input.goalId) await requireEntity(connection, "goal", input.goalId);
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (input.projectId)
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await requireEntity(connection, "project", input.projectId);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "UPDATE task SET goal_id=?,project_id=?,title=?,note=?,due_date=?,updated_at=? WHERE id=?",
        [
          input.goalId ?? null,
          input.projectId ?? null,
          input.title,
          input.note,
          toMysqlDateTime(input.dueDate),
          utcNow(),
          input.id,
        ],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id: input.id };
    }),
  remove: (id: string) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await requireEntity(connection, "task", id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await cleanupEntityLinks(connection, "task", id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query("DELETE FROM task WHERE id=?", [id]);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  transition: (input: { id: string; action: "advance" | "undo" }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const task = await requireEntity(connection, "task", input.id);
      // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
      const status = nextTaskStatus(task.status, input.action);
      const completedAt = status === "done" ? utcNow() : null;
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "UPDATE task SET status=?,completed_at=?,updated_at=? WHERE id=?",
        [status, completedAt, utcNow(), input.id],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id: input.id, status };
    }),
};
