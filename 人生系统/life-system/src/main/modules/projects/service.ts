import { randomUUID } from "node:crypto";
import type { RowDataPacket } from "mysql2/promise";
import type { ProjectInput } from "../../../shared/contracts/entities.js";
import { transitionProject } from "../../../shared/domain/state-machines.js";
// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
import { inTransaction } from "../../infrastructure/db/transaction.js";
import {
  cleanupEntityLinks,
  replaceTags,
  requireEntity,
  toMysqlDateTime,
  utcNow,
} from "../common/database.js";

// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
const select = `SELECT p.id,p.goal_id AS goalId,p.title,p.description,p.status,p.start_at AS startAt,p.end_at AS endAt,p.created_at AS createdAt,p.updated_at AS updatedAt,g.title AS goalTitle FROM project p LEFT JOIN goal g ON g.id=p.goal_id`;
export const projectService = {
  list: (filter: { status?: string; goalId?: string }) =>
    inTransaction(async (connection) => {
      const where: string[] = [];
      const values: unknown[] = [];
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (filter.status) {
        where.push("p.status=?");
        values.push(filter.status);
      }
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (filter.goalId) {
        where.push("p.goal_id=?");
        values.push(filter.goalId);
      }
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} ${where.length ? `WHERE ${where.join(" AND ")}` : ""} ORDER BY p.updated_at DESC`,
        values,
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return rows;
    }),
  get: (id: string) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} WHERE p.id=?`,
        [id],
      );
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (!rows[0])
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("项目不存在"), { code: "NOT_FOUND" });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [tasks] = await connection.query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT id,title,status,due_date AS dueDate FROM task WHERE project_id=? ORDER BY due_date IS NULL,due_date",
        [id],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { ...rows[0], tasks };
    }),
  create: (input: ProjectInput) =>
    inTransaction(async (connection) => {
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (input.goalId) await requireEntity(connection, "goal", input.goalId);
      const id = randomUUID();
      const now = utcNow();
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "INSERT INTO project (id,goal_id,title,description,status,start_at,end_at,created_at,updated_at) VALUES (?,?,?,?,'active',?,?,?,?)",
        [
          id,
          input.goalId ?? null,
          input.title,
          input.description,
          toMysqlDateTime(input.startAt),
          toMysqlDateTime(input.endAt),
          now,
          now,
        ],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await replaceTags(connection, "project", id, input.tags);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  update: (input: ProjectInput & { id: string }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const project = await requireEntity(connection, "project", input.id);
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (input.goalId) await requireEntity(connection, "goal", input.goalId);
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (
        project.status === "done" &&
        (project.title !== input.title ||
          project.goal_id !== (input.goalId ?? null) ||
          String(project.start_at ?? "") !==
            String(toMysqlDateTime(input.startAt) ?? ""))
      )
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("已完成项目只能修改说明和标签"), {
          code: "INVALID_STATE",
        });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "UPDATE project SET title=?,description=?,goal_id=?,start_at=?,end_at=?,updated_at=? WHERE id=?",
        [
          input.title,
          input.description,
          input.goalId ?? null,
          toMysqlDateTime(input.startAt),
          toMysqlDateTime(input.endAt),
          utcNow(),
          input.id,
        ],
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await replaceTags(connection, "project", input.id, input.tags);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id: input.id };
    }),
  remove: (id: string) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await requireEntity(connection, "project", id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await cleanupEntityLinks(connection, "project", id);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query("DELETE FROM project WHERE id=?", [id]);
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { id };
    }),
  updateStatus: (input: { id: string; status: "active" | "paused" | "done" }) =>
    inTransaction(async (connection) => {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const project = await requireEntity(connection, "project", input.id);
      transitionProject(project.status, input.status);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await connection.query(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "UPDATE project SET status=?,end_at=CASE WHEN ?='done' AND end_at IS NULL THEN ? ELSE end_at END,updated_at=? WHERE id=?",
        [input.status, input.status, utcNow(), utcNow(), input.id],
      );
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return input;
    }),
};
