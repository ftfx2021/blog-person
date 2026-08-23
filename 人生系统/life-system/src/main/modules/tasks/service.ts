import { randomUUID } from "node:crypto";
import type { RowDataPacket } from "mysql2/promise";
import type { TaskInput } from "../../../shared/contracts/entities.js";
import { nextTaskStatus } from "../../../shared/domain/state-machines.js";
import { inTransaction } from "../../infrastructure/db/transaction.js";
import {
  cleanupEntityLinks,
  requireEntity,
  toMysqlDateTime,
  utcNow,
} from "../common/database.js";

const select = `SELECT t.id,t.goal_id AS goalId,t.project_id AS projectId,t.period AS period,t.title,t.note,t.due_date AS dueDate,t.status,t.completed_at AS completedAt,t.created_at AS createdAt,t.updated_at AS updatedAt,g.title AS goalTitle,p.title AS projectTitle FROM task t LEFT JOIN goal g ON g.id=t.goal_id LEFT JOIN project p ON p.id=t.project_id`;
// 列表和详情复用同一投影，字段别名严格对齐渲染层契约，避免页面再解释下划线列名。
export const taskService = {
  // 列表读取的输入是已由 IPC schema 规范化过的筛选器。
  // status、goalId 与 projectId 是可叠加的 AND 条件。
  // period 也是可叠加的分类筛选，只影响列表分组，不改变状态机结果。
  // dateFrom/dateTo 只约束截止时间，不改变无截止日期的保存语义。
  // sort 只从 schema 白名单取得，不能成为自由 SQL 排序表达式。
  // 返回结果含关联标题，页面无需再进行 N+1 查询。
  // 整个读取放在事务连接中，以便关联字段共享同一读快照。
  // 空结果是合法业务结果，不等同于实体不存在。
  // 数据库异常由事务包装器回滚并交给 IPC 转成统一错误。
  // 列表查询在同一事务快照中完成，保证关联目标与项目名称一致。
  list: (filter: {
    status?: string;
    period?: string;
    goalId?: string;
    projectId?: string;
    dateFrom?: string;
    dateTo?: string;
    sort: string;
  }) =>
    inTransaction(async (connection) => {
      const where: string[] = [];
      const values: unknown[] = [];
      for (const [field, value] of [
        ["t.status", filter.status],
        ["t.period", filter.period],
        ["t.goal_id", filter.goalId],
        ["t.project_id", filter.projectId],
      ] as const)
        // 字段名来自本地常量而不是请求参数，动态部分仅以占位符绑定，保证筛选安全。
        if (value) {
          where.push(`${field}=?`);
          values.push(value);
        }
      if (filter.dateFrom) {
        // 日期下界经 UTC 归一后再比较，避免用户时区导致截止待办落在错误日期段。
        where.push("t.due_date>=?");
        values.push(toMysqlDateTime(filter.dateFrom));
      }
      if (filter.dateTo) {
        // 日期上界与下界使用相同格式，确保范围筛选不会把 DATETIME 当字符串混比。
        where.push("t.due_date<=?");
        values.push(toMysqlDateTime(filter.dateTo));
      }
      const order =
        // created_desc 供审计最近创建；默认将无截止日期放最后以优先呈现需行动项目。
        filter.sort === "created_desc"
          ? "t.created_at DESC"
          : "t.due_date IS NULL,t.due_date,t.created_at DESC";
      const [rows] = await connection.query<RowDataPacket[]>(
        // 查询始终使用参数数组绑定 values，关键词和值不会被拼进 SQL 文本。
        `${select} ${where.length ? `WHERE ${where.join(" AND ")}` : ""} ORDER BY ${order}`,
        values,
      );
      return rows;
    }),
  // 详情读取只接受受控 UUID，调用者不能指定额外字段或筛选条件。
  // 使用与列表相同的投影，保证页面切换详情后字段名称和格式不变。
  // 查询不存在的 ID 会产生 NOT_FOUND，而不是返回 undefined 给页面猜测。
  // 详情读取本身不修改任何数据，仍使用统一事务保证连接释放。
  // 所有关联名称都由 SQL LEFT JOIN 一次返回，关联缺失时仍保留待办主体。
  // 该方法的返回值是持久化层真实数据，不缓存旧列表结果。
  // 不在此处计算状态，状态只由 transition 处理。
  // 错误会由 IPC result 层映射为可序列化 ApiResult。
  get: (id: string) =>
    // 单条读取复用关联查询，避免渲染层重复请求关联实体。
    inTransaction(async (connection) => {
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} WHERE t.id=?`,
        [id],
      );
      if (!rows[0])
        // 读取不存在的 UUID 必须显式失败，调用方不能把空数组误认为有效待办。
        throw Object.assign(new Error("待办不存在"), { code: "NOT_FOUND" });
      return rows[0];
    }),
  // 创建输入已校验标题长度、日期格式以及可空外键 ID。
  // period 是用户归类字段，默认 other，不和 dueDate 绑定。
  // 目标和项目分别做存在性检查，不能只相信 schema 的 UUID 格式。
  // 主键由主进程生成，避免客户端伪造或重放已有资源 ID。
  // 新待办固定从 todo 开始，不能在创建接口预置为完成。
  // completedAt 固定为空，完成事实只能由状态机写入。
  // 输入日期转换为 UTC MySQL DATETIME，数据库始终使用同一时区口径。
  // 插入成功才返回 ID；若关联或写入失败，事务撤销全部中间步骤。
  // 标签不属于待办 P0 创建表单，故不在此接口隐式创建关联。
  create: (input: TaskInput) =>
    // 写入前校验外键实体，避免产生无法追溯的孤儿待办。
    inTransaction(async (connection) => {
      if (input.goalId) await requireEntity(connection, "goal", input.goalId);
      // 项目同样需要存在性与行锁校验，防止并发删除后插入失效关联。
      if (input.projectId)
        await requireEntity(connection, "project", input.projectId);
      const id = randomUUID();
      // 标识符由主进程生成，避免渲染层控制主键或覆盖已有待办。
      const now = utcNow();
      await connection.query(
        "INSERT INTO task (id,goal_id,project_id,period,title,note,due_date,status,completed_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,'todo',NULL,?,?)",
        [
          id,
          input.goalId ?? null,
          input.projectId ?? null,
          input.period,
          input.title,
          input.note,
          toMysqlDateTime(input.dueDate),
          now,
          now,
        ],
      );
      return { id };
    }),
  // 更新先锁住现有待办，确保完成态判断与后续写入不可被并发请求穿插。
  // 已完成待办的内容被保护，用户必须先明确撤销完成才能重新编辑。
  // 可选目标和项目在写入前各自验证，避免把 UUID 指向已删除实体。
  // period 更新与 dueDate 解耦，编辑时只保存用户当前选择，不做派生推导。
  // 更新不会修改 status 或 completedAt，编辑动作和状态动作有清晰边界。
  // 不传关联时持久化 null，表示主动解除归属而不是保留旧值。
  // 更新时间由服务端写入 UTC 当前时刻，页面不能伪造修改历史。
  // 所有字段更新在同一事务提交，失败时旧待办保持不变。
  // 返回 ID 而非整行，调用页面通过 list/get 获得数据库权威投影。
  update: (input: TaskInput & { id: string }) =>
    // 完成态禁止直接编辑，防止完成时间与内容语义脱节。
    inTransaction(async (connection) => {
      const task = await requireEntity(connection, "task", input.id);
      // 行锁覆盖接下来的完成态判断和更新，防止两个编辑请求交叉写入。
      if (task.status === "done")
        // 完成记录携带 completedAt 这一业务事实，必须先撤销完成才能重新编辑内容。
        throw Object.assign(new Error("已完成待办请先撤销完成再编辑"), {
          code: "INVALID_STATE",
        });
      if (input.goalId) await requireEntity(connection, "goal", input.goalId);
      if (input.projectId)
        await requireEntity(connection, "project", input.projectId);
      await connection.query(
        // 更新不改变 status/completedAt，编辑与状态流转保持两个明确的领域入口。
        "UPDATE task SET goal_id=?,project_id=?,period=?,title=?,note=?,due_date=?,updated_at=? WHERE id=?",
        [
          input.goalId ?? null,
          input.projectId ?? null,
          input.period,
          input.title,
          input.note,
          toMysqlDateTime(input.dueDate),
          utcNow(),
          input.id,
        ],
      );
      return { id: input.id };
    }),
  // 删除前先锁定并验证实体存在，避免重复删除被静默认定成功。
  // 通用实体链接必须和待办删除处于同一事务，防止产生悬挂双向关系。
  // 标签等其他关系由各自约束或清理策略维护，不在页面端直接操作。
  // 删除不会影响关联目标或项目本体，待办只是支持行动。
  // 删除失败时事务回滚，关联清理也不会半完成。
  // 删除成功仅返回 ID，调用者可以可靠地从本地列表移除该项。
  // 数据库的外键/关系规则仍是最后防线，服务层负责业务顺序。
  // 该操作不可撤销，因此页面必须在调用前完成用户确认。
  remove: (id: string) =>
    // 删除是原子操作：实体存在性、多态关联清理和行删除任一步失败都会整体回滚。
    inTransaction(async (connection) => {
      await requireEntity(connection, "task", id);
      await cleanupEntityLinks(connection, "task", id);
      // 标签关系由其他通用清理策略处理；此处先处理双向实体链接防止悬挂引用。
      await connection.query("DELETE FROM task WHERE id=?", [id]);
      return { id };
    }),
  // 状态变更接收动作而非目标状态，避免调用方绕过待办工作流。
  // advance 和 undo 的合法性完全由共享领域状态机定义。
  // 读取行锁覆盖计算下一状态和更新，防止双击造成状态跳跃。
  // 进入 done 时记录服务端 UTC 完成时刻，作为完成事实的唯一来源。
  // undo 清空完成时刻，避免状态已恢复但历史统计仍显示完成的矛盾。
  // 更新同时写 updatedAt，列表排序和审计视图可正确反映状态变化。
  // 非法动作抛 INVALID_STATE，IPC 将其显示为用户可理解的错误。
  // 成功返回新状态，让快捷操作可以立即刷新而无需推测结果。
  transition: (input: { id: string; action: "advance" | "undo" }) =>
    // 状态变更交给领域状态机判定，数据库只保存合法结果。
    inTransaction(async (connection) => {
      const task = await requireEntity(connection, "task", input.id);
      const status = nextTaskStatus(task.status, input.action);
      // 状态机返回唯一合法下一状态，服务层不在此复制 todo/doing/done 的转移表。
      const completedAt = status === "done" ? utcNow() : null;
      // 只有进入 done 写完成时刻；撤销完成必须清空它，避免统计使用过期时间。
      await connection.query(
        "UPDATE task SET status=?,completed_at=?,updated_at=? WHERE id=?",
        [status, completedAt, utcNow(), input.id],
      );
      return { id: input.id, status };
    }),
};
