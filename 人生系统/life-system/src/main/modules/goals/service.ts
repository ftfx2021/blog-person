import { randomUUID } from "node:crypto";
import type { PoolConnection, RowDataPacket } from "mysql2/promise";
import type { GoalInput } from "../../../shared/contracts/entities.js";
import {
  calculateMilestoneProgress,
  calculateNumericProgress,
} from "../../../shared/domain/progress.js";
import { transitionGoal } from "../../../shared/domain/state-machines.js";
import { inTransaction } from "../../infrastructure/db/transaction.js";
import {
  cleanupEntityLinks,
  replaceTags,
  requireEntity,
  toMysqlDateTime,
  utcNow,
} from "../common/database.js";

interface GoalRow extends RowDataPacket {
  // 目标主键用于关联标签、数据点、里程碑与支持行动查询。
  id: string;
  // 度量类型决定 decorateGoal 需要加载哪一种进度来源。
  metricType: "numeric" | "milestone" | "status";
  // 数值进度的起点可为空，仅 numeric 类型在 schema 中要求完整公式参数。
  startValue: number | null;
  // 目标值与起点共同定义比例方向，可支持递增和递减的真实指标。
  targetValue: number | null;
}

interface MilestoneRow extends RowDataPacket {
  // MySQL TINYINT 以数字返回，进度计算显式只认 1 为已完成。
  isDone: number;
}

async function decorateGoal(
  connection: PoolConnection,
  goal: GoalRow,
): Promise<Record<string, unknown>> {
  // 组装目标详情的所有派生信息；进度在主进程计算，避免不同页面使用不同公式。
  // 详情查询一次性组装标签、原始记录和里程碑，避免列表与详情各自维护一套进度口径。
  const [tagRows] = await connection.query<RowDataPacket[]>(
    // 标签按名称排序，详情与列表展示不会因关联写入顺序不同而抖动。
    "SELECT t.name FROM tag t JOIN entity_tag et ON et.tag_id=t.id WHERE et.entity_type='goal' AND et.entity_id=? ORDER BY t.name",
    [goal.id],
  );
  const [records] = await connection.query<RowDataPacket[]>(
    // 数据点按记录时间再按创建时间稳定排序，最后一项才是进度的权威观测。
    "SELECT id, value, note, recorded_at AS recordedAt, created_at AS createdAt FROM goal_record WHERE goal_id=? ORDER BY recorded_at ASC, created_at ASC",
    [goal.id],
  );
  const [milestones] = await connection.query<MilestoneRow[]>(
    // 里程碑按显式 sortOrder 排列，创建时间仅用于处理同序的历史数据。
    "SELECT id, title, is_done AS isDone, done_at AS doneAt, sort_order AS sortOrder, created_at AS createdAt, updated_at AS updatedAt FROM milestone WHERE goal_id=? ORDER BY sort_order, created_at",
    [goal.id],
  );
  let progress: number | null = null;
  // 状态型目标没有百分比概念，保留 null 让界面明确显示其非数值性质。
  // 数值型目标只取最新数据点计算进度；任务数量不会参与这个公式。
  if (goal.metricType === "numeric")
    progress = calculateNumericProgress(
      Number(goal.startValue),
      Number(goal.targetValue),
      records.at(-1)?.value == null ? undefined : Number(records.at(-1)!.value),
    );
  // 里程碑型目标使用已完成数量除以总数量，空列表保持“尚未开始”。
  if (goal.metricType === "milestone")
    progress = calculateMilestoneProgress(
      milestones.filter((item) => item.isDone === 1).length,
      milestones.length,
    );
  return {
    ...goal,
    tags: tagRows.map((row) => row.name),
    records,
    milestones,
    progress,
  };
}

const goalSelect = `SELECT id, title, description, period, metric_type AS metricType, unit, start_value AS startValue,
  target_value AS targetValue, status, due_date AS dueDate, created_at AS createdAt, updated_at AS updatedAt FROM goal`;

export const goalService = {
  // 列表筛选只支持状态和标题关键词，避免在 P0 中形成不可审计的自由查询。
  // 关键词以参数绑定传入 LIKE，不能改变 SQL 结构。
  // 到期日排序优先展示有期限的目标，无期限目标随后按更新时间排列。
  // 每行都调用 decorateGoal，数值/里程碑进度不会由不同页面重复计算。
  // 状态型目标保持 null 进度，界面据此显示非百分比目标。
  // 目标标签、记录和里程碑的读取使用同一事务连接快照。
  // 空集合是合法查询结果，NOT_FOUND 只属于按 ID 读取。
  // 读取失败由事务和 IPC 统一处理，不在列表中吞掉数据库异常。
  // 按状态与关键词筛选目标，并逐项补齐标签、记录、里程碑和统一进度。
  list: (filter: { status?: string; keyword?: string }) =>
    inTransaction(async (connection) => {
      const where: string[] = [];
      const values: unknown[] = [];
      if (filter.status) {
        // 状态筛选以占位符绑定，不能将调用方字符串拼进 WHERE 结构。
        where.push("status=?");
        values.push(filter.status);
      }
      if (filter.keyword) {
        // 关键词仅匹配标题，P0 不把备注、标签或文档内容混进目标列表语义。
        where.push("title LIKE ?");
        values.push(`%${filter.keyword}%`);
      }
      const [rows] = await connection.query<RowDataPacket[]>(
        // 到期目标优先且无到期日后置，方便用户先处理时间敏感的长期目标。
        `${goalSelect} ${where.length ? `WHERE ${where.join(" AND ")}` : ""} ORDER BY due_date IS NULL, due_date, updated_at DESC`,
        values,
      );
      return Promise.all(
        // 同一事务连接装饰所有行，避免标签或里程碑在列表内出现跨快照差异。
        rows.map((row) => decorateGoal(connection, row as GoalRow)),
      );
    }),
  // 详情读取先确认目标存在，再聚合目标自身的所有派生数据。
  // 项目和待办以 supportingActions 返回，明确它们支持目标但不决定进度。
  // 关联项目按最近更新排序，用户优先看到刚有变化的项目。
  // 关联待办按截止时间排序，未设截止日期项后置。
  // 详情不修改状态、记录或里程碑，纯读取保持可重复。
  // 一次性返回目标、标签、记录、里程碑和支持行动，避免页面发起 N+1 请求。
  // 无效 ID 返回 NOT_FOUND，页面可显示明确错误而非空详情。
  // 所有数据在同一连接上下文读取，减少跨查询视图不一致。
  get: (id: string) =>
    // 详情除目标本身外返回支持行动，便于用户判断项目和待办是否支撑该目标。
    inTransaction(async (connection) => {
      const [rows] = await connection.query<RowDataPacket[]>(
        `${goalSelect} WHERE id=?`,
        [id],
      );
      if (!rows[0])
        // 详情不存在必须抛 NOT_FOUND，页面才能显示错误状态而非空白详情。
        throw Object.assign(new Error("目标不存在"), { code: "NOT_FOUND" });
      const goal = await decorateGoal(connection, rows[0] as GoalRow);
      // 已装饰的目标作为基础，再补项目和待办两类支持行动而不把它们计入进度。
      const [projects] = await connection.query<RowDataPacket[]>(
        "SELECT id,title,status FROM project WHERE goal_id=? ORDER BY updated_at DESC",
        [id],
      );
      const [tasks] = await connection.query<RowDataPacket[]>(
        "SELECT id,title,status,due_date AS dueDate FROM task WHERE goal_id=? ORDER BY due_date IS NULL,due_date",
        [id],
      );
      return { ...goal, supportingActions: { projects, tasks } };
    }),
  // 创建输入已验证度量类型与数值公式是否匹配。
  // 新目标始终以 active 状态创建，结束态只能经 finish 状态机进入。
  // 主键、创建时间和更新时间由主进程生成，页面无法伪造业务历史。
  // 可选截止时间统一转换成 UTC MySQL DATETIME。
  // 数值字段仅按 schema 对 numeric 类型开放，其他类型写空值。
  // 标签替换与插入在同一事务中，失败不会留下半创建目标。
  // 创建不隐式生成数据点或里程碑，用户需要明确记录真实进展。
  // 成功仅返回 ID，后续页面跳转会重新加载数据库权威详情。
  create: (input: GoalInput) =>
    // 新目标与标签在同一事务写入，避免创建成功后留下没有标签或标签悬空的状态。
    inTransaction(async (connection) => {
      const id = randomUUID();
      // 目标 ID 由主进程生成，使页面只能描述业务字段而不能伪造资源身份。
      const now = utcNow();
      // 创建与更新时间共享同一 UTC 时刻，首次保存可按两者一致识别新记录。
      await connection.query(
        // 新目标始终从 active 开始，结束状态只能经过 finish 和领域状态机进入。
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
      await replaceTags(connection, "goal", id, input.tags);
      // 标签替换与目标插入处于同一事务，失败不会留下无主关联或半完成目标。
      return { id };
    }),
  // 更新先锁定目标，历史记录数量和字段判断在一次事务中保持一致。
  // 已结束目标锁定标题、周期与度量类型，防止完成后重写业务事实。
  // 已有数值记录时修改起点或目标值必须显式确认重算。
  // 该确认防止用户无意改变比例公式后让整个历史进度看起来失真。
  // 已有数值历史时禁止切换 metricType，避免记录失去可解释的归属。
  // 描述、标签等允许字段仍可更新，以补充结束目标的回顾信息。
  // 状态不在更新接口写入，完成/放弃只能通过 finish。
  // 更新与标签替换要么一起提交，要么因异常整体回滚。
  update: (input: GoalInput & { id: string }) =>
    // 更新保留已结束目标与历史记录的不可变约束，防止倒改后让过往进度失真。
    inTransaction(async (connection) => {
      const goal = await requireEntity(connection, "goal", input.id);
      const [recordCount] = await connection.query<RowDataPacket[]>(
        "SELECT COUNT(*) AS count FROM goal_record WHERE goal_id=?",
        [input.id],
      );
      // 已结束目标只允许更新说明和标签，核心事实保持只读。
      if (
        goal.status !== "active" &&
        (goal.title !== input.title ||
          goal.metric_type !== input.metricType ||
          goal.period !== input.period)
      )
        throw Object.assign(new Error("已结束目标只能修改说明和标签"), {
          code: "INVALID_STATE",
        });
      // 有数值历史时修改起终点必须显式确认，避免用户未察觉地重算全部历史进度。
      if (
        Number(recordCount[0]!.count) > 0 &&
        (Number(goal.start_value) !== input.startValue ||
          Number(goal.target_value) !== input.targetValue) &&
        !input.confirmRecalculate
      )
        throw Object.assign(
          new Error("修改起点或目标值会重算历史进度，请确认"),
          { code: "CONFLICT" },
        );
      if (
        goal.metric_type !== input.metricType &&
        Number(recordCount[0]!.count) > 0
      )
        throw Object.assign(new Error("已有数据点时不能修改度量类型"), {
          code: "INVALID_STATE",
        });
      await connection.query(
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
      await replaceTags(connection, "goal", input.id, input.tags);
      return { id: input.id };
    }),
  // 删除前验证并锁定目标，避免并发操作中重复清理关系。
  // 多态实体链接先清理，防止其他模块保留指向已删目标的引用。
  // 关联数据点和里程碑由数据库关系规则一并清理。
  // 删除不会删除支持该目标的项目或待办本体。
  // 清理与主体删除处于同一事务，不会提交半删除状态。
  // 该操作不可撤销，页面必须在 IPC 调用前完成用户确认。
  // 返回 ID 而非旧对象，调用端应重新读取列表确认最终状态。
  // 失败由统一错误映射保留具体 NOT_FOUND 或数据库原因。
  remove: (id: string) =>
    // 删除前清理多态关联，确保标签和实体链接不会继续指向已删除目标。
    inTransaction(async (connection) => {
      await requireEntity(connection, "goal", id);
      await cleanupEntityLinks(connection, "goal", id);
      await connection.query("DELETE FROM goal WHERE id=?", [id]);
      return { id };
    }),
  // finish 仅允许目标进入 done 或 abandoned 两种结束态。
  // 当前状态先经 transitionGoal 验证，结束态不可二次变更。
  // 行锁确保状态检查和 UPDATE 之间不会被另一完成请求抢先写入。
  // 完成不会伪造数值记录或自动勾选里程碑，进度事实保持独立。
  // 服务端写 UTC 更新时间，列表和审计能反映实际结束操作。
  // 非法转换返回 INVALID_STATE，而不是悄悄覆盖已有终态。
  // 成功返回最终状态，页面刷新后可显示只读控制。
  // 整个流转由事务保护，状态不会在错误时停留在中间值。
  finish: (id: string, status: "done" | "abandoned") =>
    // 完成/放弃必须经领域状态机验证，禁止把已结束目标重新改成另一种结束态。
    inTransaction(async (connection) => {
      const goal = await requireEntity(connection, "goal", id);
      transitionGoal(goal.status, status);
      await connection.query(
        "UPDATE goal SET status=?,updated_at=? WHERE id=?",
        [status, utcNow(), id],
      );
      return { id, status };
    }),
  record: (input: {
    goalId: string;
    value: number;
    note: string | null;
    recordedAt: string;
  }) =>
    // 真实数据记录只允许活跃的数值型目标使用。
    // 服务读取目标后判断类型和状态，不能只信任页面隐藏了按钮。
    // recordedAt 不能晚于当前时刻，避免提前填报未来业绩。
    // 记录值可正可负，比例方向由目标起点和目标值决定。
    // 数据点使用独立 UUID，允许同一天保留多次真实观测。
    // 进度读取时取最新记录，不在写入时缓存百分比。
    // 插入失败不会修改目标主体或其他历史记录。
    // 成功返回记录 ID，页面重新加载可得到统一进度。
    // 只允许活跃数值型目标新增真实数据点，并拒绝未来时间以保证时间线可信。
    inTransaction(async (connection) => {
      const goal = await requireEntity(connection, "goal", input.goalId);
      if (goal.metric_type !== "numeric" || goal.status !== "active")
        throw Object.assign(new Error("只有进行中的数值型目标可以记录数据"), {
          code: "INVALID_STATE",
        });
      if (new Date(input.recordedAt).getTime() > Date.now())
        throw Object.assign(new Error("记录时间不能晚于当前时间"), {
          code: "VALIDATION_ERROR",
        });
      const id = randomUUID();
      await connection.query(
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
      return { id };
    }),
  addMilestone: (input: {
    goalId: string;
    title: string;
    sortOrder?: number;
  }) =>
    // 里程碑只可添加到活跃的 milestone 类型目标。
    // 服务验证父目标类型，页面不能借接口给数值目标混入里程碑。
    // 未指定 sortOrder 时取当前最大序号后的下一个位置。
    // 排序从零开始且在同一目标范围内独立维护。
    // 新里程碑初始化为未完成，不带完成时间。
    // UUID 和时间由主进程生成，确保排序/审计字段可信。
    // 父目标检查和子项插入同事务完成，避免孤儿里程碑。
    // 返回 ID，调用页面重新读详情获得排序后的列表。
    // 里程碑只属于活跃的里程碑型目标；未指定排序时追加到现有列表末尾。
    inTransaction(async (connection) => {
      const goal = await requireEntity(connection, "goal", input.goalId);
      if (goal.metric_type !== "milestone" || goal.status !== "active")
        throw Object.assign(new Error("只有进行中的里程碑目标可以添加子项"), {
          code: "INVALID_STATE",
        });
      const [rows] = await connection.query<RowDataPacket[]>(
        "SELECT COALESCE(MAX(sort_order),-1)+1 AS nextOrder FROM milestone WHERE goal_id=?",
        [input.goalId],
      );
      const id = randomUUID();
      const now = utcNow();
      await connection.query(
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
      return { id };
    }),
  // 里程碑更新联表锁定父目标，确保结束目标不能在并发窗口被修改。
  // 标题和排序号均由 schema 验证，负序号无法进入数据库。
  // 查询不到子项时返回 NOT_FOUND，不把空更新视为成功。
  // 父目标不是 active 时返回 INVALID_STATE，保留结束快照。
  // 更新不改变 isDone 或 doneAt，完成事实只能由 toggleMilestone 写入。
  // 更新时间由服务端统一写入 UTC。
  // 操作在事务内，锁和 UPDATE 的原子性避免拖拽排序竞争。
  // 成功只返回 ID，详情刷新后读取最终排列。
  updateMilestone: (input: { id: string; title: string; sortOrder: number }) =>
    // 通过 FOR UPDATE 锁住子项与父目标，避免并发拖拽排序覆盖结束态限制。
    inTransaction(async (connection) => {
      const [rows] = await connection.query<RowDataPacket[]>(
        "SELECT m.*,g.status AS goalStatus FROM milestone m JOIN goal g ON g.id=m.goal_id WHERE m.id=? FOR UPDATE",
        [input.id],
      );
      if (!rows[0])
        throw Object.assign(new Error("里程碑不存在"), { code: "NOT_FOUND" });
      if (rows[0].goalStatus !== "active")
        throw Object.assign(new Error("目标已结束"), { code: "INVALID_STATE" });
      await connection.query(
        "UPDATE milestone SET title=?,sort_order=?,updated_at=? WHERE id=?",
        [input.title, input.sortOrder, utcNow(), input.id],
      );
      return { id: input.id };
    }),
  // 开关操作锁定里程碑及父目标，防止结束目标在并发中被误勾选。
  // 同时验证父目标类型必须仍是 milestone，阻止失效数据模型写入。
  // isDone 由 schema 限定为布尔值，不能用任意数值绕过 UI。
  // 勾选时写入服务端 UTC doneAt，保留完成发生时间。
  // 取消勾选时清空 doneAt，避免未完成子项仍带完成审计。
  // 目标百分比并不缓存，详情下次读取按子项事实重新计算。
  // 非法状态使用 INVALID_STATE 返回，页面可禁用或提示用户。
  // 成功返回当前开关值，调用端无需猜测数据库转换。
  toggleMilestone: (input: { id: string; isDone: boolean }) =>
    // 切换开关同步维护 doneAt，完成时间只在实际完成时存在，撤销后清空。
    inTransaction(async (connection) => {
      const [rows] = await connection.query<RowDataPacket[]>(
        "SELECT m.*,g.status AS goalStatus,g.metric_type AS metricType FROM milestone m JOIN goal g ON g.id=m.goal_id WHERE m.id=? FOR UPDATE",
        [input.id],
      );
      if (!rows[0])
        throw Object.assign(new Error("里程碑不存在"), { code: "NOT_FOUND" });
      if (rows[0].goalStatus !== "active" || rows[0].metricType !== "milestone")
        throw Object.assign(new Error("目标已结束或类型不正确"), {
          code: "INVALID_STATE",
        });
      await connection.query(
        "UPDATE milestone SET is_done=?,done_at=?,updated_at=? WHERE id=?",
        [
          input.isDone ? 1 : 0,
          input.isDone ? utcNow() : null,
          utcNow(),
          input.id,
        ],
      );
      return { id: input.id, isDone: input.isDone };
    }),
  // 删除前联表检查里程碑和父目标，空 ID 或结束目标均不能继续操作。
  // 删除后读取剩余项并压实 sortOrder，保证之后拖拽和追加不遇到排序空洞。
  // 重排使用同一事务与行锁，避免两个删除请求得到相同序号。
  // 删除不会自动改变父目标状态，里程碑数量变化只影响下次进度读取。
  // 每一项重排更新时间使用服务端 UTC，审计顺序保持明确。
  // 子项不存在返回 NOT_FOUND，不能将用户操作误报成功。
  // 父目标结束返回 INVALID_STATE，完成后列表作为历史快照保留。
  // 成功只返回被删除 ID，页面需要重新加载获得压实后的列表。
  removeMilestone: (id: string) =>
    // 删除后压实排序序号，防止后续插入和拖拽建立在有空洞的顺序上。
    inTransaction(async (connection) => {
      const [rows] = await connection.query<RowDataPacket[]>(
        "SELECT m.goal_id AS goalId,g.status FROM milestone m JOIN goal g ON g.id=m.goal_id WHERE m.id=? FOR UPDATE",
        [id],
      );
      if (!rows[0])
        throw Object.assign(new Error("里程碑不存在"), { code: "NOT_FOUND" });
      if (rows[0].status !== "active")
        throw Object.assign(new Error("目标已结束"), { code: "INVALID_STATE" });
      await connection.query("DELETE FROM milestone WHERE id=?", [id]);
      const [remaining] = await connection.query<RowDataPacket[]>(
        "SELECT id FROM milestone WHERE goal_id=? ORDER BY sort_order,created_at",
        [rows[0].goalId],
      );
      for (const [index, item] of remaining.entries())
        await connection.query(
          "UPDATE milestone SET sort_order=?,updated_at=? WHERE id=?",
          [index, utcNow(), item.id],
        );
      return { id };
    }),
};
