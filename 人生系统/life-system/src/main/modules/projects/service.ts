import { randomUUID } from "node:crypto";
import type { RowDataPacket } from "mysql2/promise";
import type { ProjectInput } from "../../../shared/contracts/entities.js";
import { transitionProject } from "../../../shared/domain/state-machines.js";
import { inTransaction } from "../../infrastructure/db/transaction.js";
import {
  cleanupEntityLinks,
  replaceTags,
  requireEntity,
  toMysqlDateTime,
  utcNow,
} from "../common/database.js";

const select = `SELECT p.id,p.goal_id AS goalId,p.title,p.description,p.status,p.start_at AS startAt,p.end_at AS endAt,p.created_at AS createdAt,p.updated_at AS updatedAt,g.title AS goalTitle FROM project p LEFT JOIN goal g ON g.id=p.goal_id`;
export const projectService = {
  list: (filter: { status?: string; goalId?: string }) =>
    inTransaction(async (connection) => {
      const where: string[] = [];
      const values: unknown[] = [];
      if (filter.status) {
        where.push("p.status=?");
        values.push(filter.status);
      }
      if (filter.goalId) {
        where.push("p.goal_id=?");
        values.push(filter.goalId);
      }
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} ${where.length ? `WHERE ${where.join(" AND ")}` : ""} ORDER BY p.updated_at DESC`,
        values,
      );
      return rows;
    }),
  get: (id: string) =>
    inTransaction(async (connection) => {
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} WHERE p.id=?`,
        [id],
      );
      if (!rows[0])
        throw Object.assign(new Error("项目不存在"), { code: "NOT_FOUND" });
      const [tasks] = await connection.query<RowDataPacket[]>(
        "SELECT id,title,status,due_date AS dueDate FROM task WHERE project_id=? ORDER BY due_date IS NULL,due_date",
        [id],
      );
      return { ...rows[0], tasks };
    }),
  create: (input: ProjectInput) =>
    inTransaction(async (connection) => {
      if (input.goalId) await requireEntity(connection, "goal", input.goalId);
      const id = randomUUID();
      const now = utcNow();
      await connection.query(
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
      await replaceTags(connection, "project", id, input.tags);
      return { id };
    }),
  update: (input: ProjectInput & { id: string }) =>
    inTransaction(async (connection) => {
      const project = await requireEntity(connection, "project", input.id);
      if (input.goalId) await requireEntity(connection, "goal", input.goalId);
      if (
        project.status === "done" &&
        (project.title !== input.title ||
          project.goal_id !== (input.goalId ?? null) ||
          String(project.start_at ?? "") !==
            String(toMysqlDateTime(input.startAt) ?? ""))
      )
        throw Object.assign(new Error("已完成项目只能修改说明和标签"), {
          code: "INVALID_STATE",
        });
      await connection.query(
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
      await replaceTags(connection, "project", input.id, input.tags);
      return { id: input.id };
    }),
  remove: (id: string) =>
    inTransaction(async (connection) => {
      await requireEntity(connection, "project", id);
      await cleanupEntityLinks(connection, "project", id);
      await connection.query("DELETE FROM project WHERE id=?", [id]);
      return { id };
    }),
  updateStatus: (input: { id: string; status: "active" | "paused" | "done" }) =>
    inTransaction(async (connection) => {
      const project = await requireEntity(connection, "project", input.id);
      transitionProject(project.status, input.status);
      await connection.query(
        "UPDATE project SET status=?,end_at=CASE WHEN ?='done' AND end_at IS NULL THEN ? ELSE end_at END,updated_at=? WHERE id=?",
        [input.status, input.status, utcNow(), utcNow(), input.id],
      );
      return input;
    }),
};
