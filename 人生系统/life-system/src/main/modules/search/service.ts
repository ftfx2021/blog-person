import type { RowDataPacket } from "mysql2/promise";
// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
import { inTransaction } from "../../infrastructure/db/transaction.js";

// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const allowedTypes = ["goal", "project", "task", "habit", "document"] as const;
export const searchService = {
  search: (input: {
    keyword: string;
    types: string[];
    tags: string[];
    status?: string;
  }) =>
    inTransaction(async (connection) => {
      const selected = input.types.length
        ? allowedTypes.filter((type) => input.types.includes(type))
        : [...allowedTypes];
      // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
      const results: Record<string, unknown[]> = {};
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      for (const type of selected) {
        // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
        const titleColumn = type === "habit" ? "name" : "title";
        const clauses = [`${titleColumn} LIKE ?`];
        const values: unknown[] = [`%${input.keyword}%`];
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        if (input.status && ["goal", "project", "task"].includes(type)) {
          clauses.push("status=?");
          values.push(input.status);
        }
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        if (input.tags.length && type !== "document") {
          clauses.push(
            // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
            `EXISTS (SELECT 1 FROM entity_tag et JOIN tag tg ON tg.id=et.tag_id WHERE et.entity_type=? AND et.entity_id=${type}.id AND tg.name IN (${input.tags.map(() => "?").join(",")}))`,
          );
          values.push(type, ...input.tags);
        }
        // document 使用 ngram FULLTEXT；P0 其他实体按标题 LIKE，并明确不查询 mood_record。
        const sql =
          type === "document"
            ? // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
              "SELECT id,title,doc_type AS subtype,updated_at AS updatedAt FROM document WHERE deleted_at IS NULL AND MATCH(title,raw_text) AGAINST (? IN NATURAL LANGUAGE MODE) ORDER BY updated_at DESC LIMIT 50"
            : // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
              `SELECT id,${titleColumn} AS title${["goal", "project", "task"].includes(type) ? ",status" : ""},updated_at AS updatedAt FROM ${type} WHERE ${clauses.join(" AND ")} ORDER BY updated_at DESC LIMIT 50`;
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        const [rows] = await connection.query<RowDataPacket[]>(
          sql,
          type === "document" ? [input.keyword] : values,
        );
        results[type] = rows;
      }
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return results;
    }),
};
