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

// 项目投影固定转换外键、时间和关联目标标题，列表与详情共享同一字段契约。
const select = `SELECT p.id,p.goal_id AS goalId,p.title,p.description,p.status,p.start_at AS startAt,p.end_at AS endAt,p.created_at AS createdAt,p.updated_at AS updatedAt,g.title AS goalTitle FROM project p LEFT JOIN goal g ON g.id=p.goal_id`;
export const projectService = {
  // 列表可按状态和关联目标组合筛选，所有条件都经参数绑定。
  // 关联目标不存在不会消失项目，因为 LEFT JOIN 保留独立项目。
  // 默认按更新时间倒序，用户刚创建或变更状态的项目可立即出现。
  // 筛选器已由 schema 校验，服务仍只把受控字段名放进 SQL。
  // 空列表是正常结果，不表示数据库或筛选器发生错误。
  // 查询保持只读，不会因为浏览项目改变任何审计时间。
  // inTransaction 负责连接归还和异常回滚，服务只描述业务读取。
  // 返回记录字段完全匹配页面列表和项目选择器的使用方式。
  // 按状态和所属目标筛选项目；查询事务保证项目与关联目标的展示数据来自同一读取快照。
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
        `SELECT p.id,p.goal_id AS goalId,p.title,p.description,p.status,p.start_at AS startAt,p.end_at AS endAt,p.created_at AS createdAt,p.updated_at AS updatedAt,g.title AS goalTitle,
          (SELECT JSON_ARRAYAGG(t.name) FROM tag t JOIN entity_tag et ON et.tag_id=t.id WHERE et.entity_type='project' AND et.entity_id=p.id) AS tags
          FROM project p LEFT JOIN goal g ON g.id=p.goal_id
          ${where.length ? `WHERE ${where.join(" AND ")}` : ""} ORDER BY p.updated_at DESC`,
        values,
      );
      // mysql2 可能把 JSON 聚合结果作为字符串返回，统一成页面可直接回填的标签数组。
      return rows.map((row) => {
        const parsedTags =
          typeof row.tags === "string" ? JSON.parse(row.tags) : row.tags;
        return { ...row, tags: Array.isArray(parsedTags) ? parsedTags : [] };
      });
    }),
  // 详情读取接收唯一项目 ID，并使用项目投影保持字段与列表一致。
  // 找不到项目抛 NOT_FOUND，不用空 tasks 数组掩盖无效链接。
  // 同时加载项目待办，详情页因此不需要第二轮业务请求。
  // 待办按截止时间排序，未设置截止时间的项放在最后。
  // 不对待办状态做修改，读取项目不应产生行动副作用。
  // 关联目标的名称已由 SELECT 加载，避免查询中的 N+1。
  // 操作在同一连接快照中完成，主体和待办不会出现跨时刻组合。
  // 返回对象包含项目字段和 tasks 聚合，边界清晰且可序列化。
  get: (id: string) =>
    // 读取项目详情时同时带回待办清单，使详情页不必拼接两次独立的业务读取。
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
  // 创建输入包含标题、描述、可选目标、计划时间和标签。
  // 若指定目标，先使用锁定查询验证目标仍存在。
  // 项目状态固定初始化为 active，不能由创建表单伪造已完成项目。
  // 时间字段统一转换为 UTC，数据库不会依赖客户端本地时区。
  // 主键与创建/更新时间均由主进程生成以保持审计可信。
  // 标签替换在插入后同事务执行，关联失败会回滚项目主体。
  // 未关联目标时写入 null，独立项目是合法业务模型。
  // 成功仅返回 ID，页面应重新读取列表得到关联投影。
  create: (input: ProjectInput) =>
    // 新项目先验证可选目标存在，再与标签替换一起提交，避免半成品关联留在数据库。
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
  // 更新前锁定项目并验证可选目标，避免并发删除产生失效外键。
  // 已完成项目的计划事实被保护，只允许调整说明和标签。
  // 该限制阻止完成后偷偷修改标题、归属目标或起始时间重写历史含义。
  // 结束时间与说明可随业务需要更新，仍受输入 schema 的时间范围约束。
  // 状态不在本接口更新，必须使用 updateStatus 经过项目状态机。
  // 标签始终采用 replaceTags，确保集合语义而不是追加旧标签。
  // 所有字段和标签关系一起提交，失败不留下半更新项目。
  // 返回 ID，调用者重新加载得到数据库权威状态。
  update: (input: ProjectInput & { id: string }) =>
    // 完成项目只允许改说明和标签，锁住计划本身以维护完成时的业务事实。
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
  // 删除先确认项目存在，重复请求会显式失败而不是伪装成功。
  // 清理多态 entity_link 防止其他实体继续引用已删除项目。
  // 项目删除不删除关联待办，待办会保留为独立行动。
  // 标签、链接清理和主体删除在同一事务，保证关系不悬挂。
  // 页面在调用前提示待办保留规则，服务层负责真正的数据一致性。
  // 数据库错误会导致事务回滚，已清理的链接不会单独提交。
  // 成功返回 ID，列表可以安全剔除该项目。
  // 此操作没有撤销接口，因此不能绕过用户确认直接从模板调用。
  remove: (id: string) =>
    // 删除前清理通用关联表，避免 entity_tag 等多态关系引用已删除项目。
    inTransaction(async (connection) => {
      await requireEntity(connection, "project", id);
      await cleanupEntityLinks(connection, "project", id);
      await connection.query("DELETE FROM project WHERE id=?", [id]);
      return { id };
    }),
  // 状态更新使用独立输入而非项目表单，避免可编辑字段混入流转动作。
  // 读取时加锁，合法性判断和最终 UPDATE 在同一事务串行进行。
  // transitionProject 是状态转换唯一权威，服务不复制 active/paused/done 规则。
  // 首次到 done 时写结束时间，已有计划结束时间则保留用户原意。
  // 从暂停恢复不会擦除结束时间，因为 done 状态机根本不允许恢复。
  // 更新统一记录 UTC 更新时间，用于列表排序和审计。
  // 非法转换返回 INVALID_STATE，IPC 可给页面明确错误反馈。
  // 成功返回输入状态，调用页面可以重新拉取列表确认最终数据。
  updateStatus: (input: { id: string; status: "active" | "paused" | "done" }) =>
    // 先由状态机拒绝非法流转；首次完成时补结束时间，保留用户已填写的计划结束日期。
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
