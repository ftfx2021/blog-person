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
  // 数据库存储的频率字段用于选择日连续或周连续算法。
  id: string;
  // 周频率的完成阈值可为空，仅 daily 习惯不参与周次数判断。
  frequency_type: string;
  // 连续算法只需要持久化层的最小字段，避免重算时依赖展示投影。
  weekly_target: number | null;
}
interface CheckinRow extends RowDataPacket {
  // mysql2 可能返回 Date 或字符串，后续统一截取为本地日期键。
  checkedOn: string | Date;
}
async function recalculate(
  connection: PoolConnection,
  habit: HabitPersistenceRow,
): Promise<void> {
  // 此内部函数只在已有事务连接内调用，不能单独提交部分 streak 更新。
  // 输入是锁定后的习惯持久化字段，频率变更可在同一事务中即时生效。
  // 它完整读取打卡历史而非做增量加减，以正确处理撤销、补卡与规则变更。
  // 查询按日期倒序，使最新打卡日期可直接成为 lastDoneOn。
  // 日期先降精度到 YYYY-MM-DD，消除 mysql2 返回 Date 或字符串的差异。
  // daily 与 weekly_times 调用不同领域算法，周目标只用于周频率。
  // streak、最近打卡日和更新时间同步落库，页面不会读取到半更新状态。
  // 任何读取或写入失败都冒泡给外层事务，保留旧的连续数据。
  // 每次打卡或频率调整后从完整历史重算，不能只递增，否则撤销会得到错误连续天数。
  const [rows] = await connection.query<CheckinRow[]>(
    // 倒序读取让 dates[0] 稳定对应最近一次打卡，用于更新 lastDoneOn。
    "SELECT checked_on AS checkedOn FROM habit_checkin WHERE habit_id=? ORDER BY checked_on DESC",
    [habit.id],
  );
  const dates = rows.map((row) => String(row.checkedOn).slice(0, 10));
  // 只保留 YYYY-MM-DD，连续性规则按自然日而非具体打卡时刻计算。
  const streak =
    // 日频率和周频率使用不同领域算法，不能用同一个“打卡数量”替代。
    habit.frequency_type === "daily"
      ? calculateDailyStreak(dates)
      : calculateWeeklyStreak(dates, Number(habit.weekly_target));
  await connection.query(
    // streak、lastDoneOn 与更新时间一次写入，避免页面读取到相互矛盾的派生状态。
    "UPDATE habit SET streak=?,last_done_on=?,updated_at=? WHERE id=?",
    [streak, dates[0] ?? null, utcNow(), habit.id],
  );
}
// 查询投影将 snake_case 持久化字段转换为前端契约的 camelCase。
const selectFields =
  "h.id,h.name,h.note,h.frequency_type AS frequencyType,h.weekly_target AS weeklyTarget,h.status,h.streak,h.last_done_on AS lastDoneOn,h.created_at AS createdAt,h.updated_at AS updatedAt";
const select = `SELECT ${selectFields} FROM habit h`;
export const habitService = {
  // 列表返回所有习惯定义和当前已持久化的 streak。
  // 它不把今日打卡状态写入习惯表，今天由调用页面按本地日期补齐。
  // 按更新时间倒序保证最近打卡或编辑的习惯首先出现。
  // 查询字段显式映射为 camelCase，避免渲染层依赖数据库命名。
  // 空列表是用户尚未创建习惯的正常状态，不产生错误。
  // 使用统一事务连接，确保连接在成功或失败后都被释放。
  // 本方法不运行连续算法，避免每次打开列表重新扫描所有历史。
  // 返回值只代表数据库事实，页面可在其上附加临时 checkedToday。
  // 返回习惯定义及已持久化的连续天数，今日是否完成由调用方按日期补充。
  list: (input: {
    status?: "active" | "paused" | "archived";
    includeArchived?: boolean;
    today?: string;
  }) =>
    inTransaction(async (connection) => {
      const where = input.status
        ? "h.status=?"
        : input.includeArchived
          ? "h.status IN ('active','paused','archived')"
          : "h.status IN ('active','paused')";
      const values: unknown[] = input.status ? [input.status] : [];
      const today = input.today ?? "";
      // 以一次聚合查询补齐今日和本周事实，列表页无需为每项再请求历史。
      const [rows] = await connection.query<RowDataPacket[]>(
        // 列表排序按最近修改，打卡和编辑后同一习惯会自然移到用户视野前方。
        `SELECT ${selectFields},
          EXISTS(SELECT 1 FROM habit_checkin hc WHERE hc.habit_id=h.id AND hc.checked_on=?) AS checkedToday,
          (SELECT COUNT(*) FROM habit_checkin hw WHERE hw.habit_id=h.id AND hw.checked_on>=DATE_SUB(?, INTERVAL WEEKDAY(?) DAY) AND hw.checked_on<=?) AS weeklyCheckinCount
         FROM habit h
         WHERE ${where} ORDER BY h.updated_at DESC`,
        [today, today, today, today, ...values],
      );
      return rows;
    }),
  // 单习惯读取仅接受 UUID，不开放名称等模糊查询以保持 IPC 边界窄。
  // 查询投影与列表一致，使编辑弹窗和详情不会面对不同字段结构。
  // 找不到行会返回 NOT_FOUND，不能把不存在习惯当作没有打卡的正常项目。
  // 方法不附带完整历史，避免普通详情读取带来无界数据量。
  // 历史需要使用 history 接口并可选指定日期范围。
  // 读取不修改 updatedAt，因此查看习惯不会改变列表排序。
  // 数据库连接由 inTransaction 保证提交空事务或错误时回收。
  // 返回的频率与 weeklyTarget 可直接用于解释 streak 单位。
  get: (id: string) =>
    // 详情查询先确认实体存在，把不存在与空字段明确区分为 NOT_FOUND。
    inTransaction(async (connection) => {
      const [rows] = await connection.query<RowDataPacket[]>(
        `${select} WHERE id=?`,
        [id],
      );
      if (!rows[0])
        // 空结果不表示空习惯内容，而是请求的实体不存在，须返回可识别错误码。
        throw Object.assign(new Error("习惯不存在"), { code: "NOT_FOUND" });
      return rows[0];
    }),
  // 创建输入通过 schema 保证每日习惯不会携带周目标。
  // 周频率输入必须给出一到七次的目标，避免连续算法没有阈值。
  // 主键与时间戳只由主进程生成，页面不能伪造创建历史。
  // 新习惯没有任何打卡历史，streak 固定初始化为零。
  // lastDoneOn 初始化为 null，明确区别“从未打卡”和具体历史日期。
  // 插入一条定义记录不隐式创建当天打卡，避免创建行为被误算为完成。
  // 失败时事务不会留下一半的习惯记录。
  // 成功仅返回 ID，调用方重新加载得到完整数据库投影。
  create: (input: HabitInput) =>
    // 新习惯从零连续天数开始，weeklyTarget 仅在周频率下作为算法的完成阈值使用。
    inTransaction(async (connection) => {
      const id = randomUUID();
      // 创建主键只在主进程产生，保持 IPC 输入不具备指定数据库标识的能力。
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
  // 更新先锁定习惯，确保频率定义与随后的历史回算是一个原子操作。
  // 名称、备注、频率和周目标由完整 schema 校验，不能传入部分无效规则。
  // 修改频率可能将同一历史解释为不同连续性，因此必须立即重算。
  // 重算使用新频率和新阈值，而不是数据库中尚未提交的旧字段。
  // streak 从不接受页面输入，防止客户端伪造连续记录。
  // 更新后写入 UTC 更新时间，使列表展示用户最近调整的习惯。
  // 任何重算失败都会回滚定义更新，避免规则和 streak 不一致。
  // 返回 ID 供页面重载今日状态和历史展示。
  update: (input: HabitInput & { id: string }) =>
    // 修改频率会改变连续规则，更新定义后必须按全部历史重新计算 streak。
    inTransaction(async (connection) => {
      const habit = await requireEntity(connection, "habit", input.id);
      // 先锁住原记录，频率变更和随后的历史重算必须处在同一个事务中。
      await connection.query(
        // 更新定义不直接写 streak，streak 永远由 checkin 历史推导而来。
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
        // 使用即将保存的新频率重算，避免在提交后保留旧规则计算的连续天数。
        ...habit,
        frequency_type: input.frequencyType,
        weekly_target: input.weeklyTarget,
      });
      return { id: input.id };
    }),
  // 状态变更不触碰打卡历史或连续值，恢复后仍沿用原来的历史统计。
  updateStatus: (input: {
    id: string;
    status: "active" | "paused" | "archived";
  }) =>
    inTransaction(async (connection) => {
      const habit = await requireEntity(connection, "habit", input.id);
      // 归档保留历史但只能恢复为 active；暂停与 active 之间可往返切换。
      if (habit.status === "archived" && input.status !== "active") {
        throw Object.assign(new Error("归档习惯只能恢复后再暂停或归档"), {
          code: "VALIDATION_ERROR",
        });
      }
      if (habit.status === input.status) return { id: input.id, status: input.status };
      await connection.query(
        "UPDATE habit SET status=?,updated_at=? WHERE id=?",
        [input.status, utcNow(), input.id],
      );
      return { id: input.id, status: input.status };
    }),
  // 删除前验证习惯存在，避免重复点击静默吞掉资源不存在的错误。
  // 数据库级关系会清理打卡历史，业务层不逐条删除以避免遗漏。
  // 删除不会影响其他习惯的 streak，因为历史按 habitId 隔离。
  // 操作在事务内执行，关系清理和主体删除同时成功或同时回滚。
  // 页面负责在调用前告知用户历史不可恢复的后果。
  // 不返回已删除记录，防止调用方继续使用过期 streak 数据。
  // 成功只返回 ID，列表刷新后由数据库反映最终状态。
  // 失败会转换为标准 ApiResult 错误而不留半删除数据。
  remove: (id: string) =>
    // 先校验目标习惯，避免删除不存在资源时伪装成成功响应。
    inTransaction(async (connection) => {
      await requireEntity(connection, "habit", id);
      await connection.query("DELETE FROM habit WHERE id=?", [id]);
      // 打卡历史由数据库关系清理，服务先确认实体以防误报删除成功。
      return { id };
    }),
  // 打卡输入包含 checkedOn 和调用方本地 today，服务负责拒绝未来日期。
  // 先锁定习惯，避免频率修改和本次重算穿插导致使用错误规则。
  // 数据库唯一关系使同一日期重复点击保持幂等而不增加多条记录。
  // upsert 不覆盖其他日期，只确认本次自然日已存在。
  // 写入成功后从完整历史回算，补卡也能正确改变当前 streak。
  // 时间戳以 UTC 写入 createdAt，而连续性只使用 checkedOn 自然日。
  // 未来日期错误使用 VALIDATION_ERROR，与格式错误保持可区分。
  // 成功返回实际打卡日，页面不用猜测日期格式化结果。
  checkin: (input: { id: string; checkedOn: string; today: string }) =>
    // 拒绝未来打卡；唯一约束配合 upsert 让同一天重复点击保持幂等。
    inTransaction(async (connection) => {
      const habit = await requireEntity(connection, "habit", input.id);
      // 读取并锁住习惯，打卡写入和重算期间不会被频率编辑并发穿插。
      if (input.checkedOn > input.today)
        // 以调用声明的今天为上界；schema 负责格式，服务负责业务时间边界。
        throw Object.assign(new Error("不能补未来日期的打卡"), {
          code: "VALIDATION_ERROR",
        });
      await connection.query(
        // 唯一键冲突时仅确认已有记录存在，不重复创建同一天打卡事实。
        "INSERT INTO habit_checkin (id,habit_id,checked_on,created_at) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE habit_id=VALUES(habit_id)",
        [randomUUID(), input.id, input.checkedOn, utcNow()],
      );
      await recalculate(connection, habit);
      return { id: input.id, checkedOn: input.checkedOn };
    }),
  // 撤销只删除指定习惯在指定自然日的一条打卡事实。
  // 即使该日没有记录，后续重算仍会给出历史真实的连续值。
  // 服务先锁定习惯，确保撤销与频率编辑不会交叉改变算法输入。
  // undo 不允许页面直接写 streak 或 lastDoneOn，二者始终从历史导出。
  // 删除和重算在一笔事务内完成，失败时原记录和连续值都保留。
  // today 保持与 checkin 相同契约，便于 IPC 接口的时间语义一致。
  // 成功返回影响的日期，日历可精确刷新单个单元格。
  // 不影响同周其他打卡，周频率会按剩余次数重新评估达标性。
  undo: (input: { id: string; checkedOn: string; today: string }) =>
    // 撤销指定日期后从历史回算，确保 lastDoneOn 与 streak 同步回退。
    inTransaction(async (connection) => {
      const habit = await requireEntity(connection, "habit", input.id);
      await connection.query(
        // 删除仅精确匹配习惯和自然日，不能因日期缺失影响其他历史记录。
        "DELETE FROM habit_checkin WHERE habit_id=? AND checked_on=?",
        [input.id, input.checkedOn],
      );
      await recalculate(connection, habit);
      return { id: input.id, checkedOn: input.checkedOn };
    }),
  // 历史查询先确认习惯存在，避免空数组掩盖错误的详情链接。
  // from/to 为可选包含边界，适合日历按月读取或页面读取完整历史。
  // 所有 where 片段固定在服务内，用户输入只作为参数值绑定。
  // 结果按日期倒序，最接近今天的记录优先展示。
  // 同时返回习惯快照，使调用方无需第二次请求频率和当前 streak。
  // 返回频率字段用于解释连续单位，daily 是天，weekly_times 是周。
  // 本方法不重算 streak，读取操作不能因为查看历史改变持久化状态。
  // 连接和失败处理保持统一事务与 IPC result 规则。
  history: (input: { id: string; from?: string; to?: string }) =>
    // 历史接口先验证习惯，再按可选日期边界过滤，供日历和连续天数解释共用。
    inTransaction(async (connection) => {
      const habit = await requireEntity(connection, "habit", input.id);
      const where = ["habit_id=?"];
      // 查询条件从固定首项开始，再可选追加范围，所有变量均使用参数绑定。
      const values: unknown[] = [input.id];
      if (input.from) {
        // from 是包含边界，日历选择起始日时应显示当天的打卡记录。
        where.push("checked_on>=?");
        values.push(input.from);
      }
      if (input.to) {
        // to 同样是包含边界，避免结束日的记录在范围视图中消失。
        where.push("checked_on<=?");
        values.push(input.to);
      }
      const [rows] = await connection.query<RowDataPacket[]>(
        // 历史按日期倒序返回，弹窗和日历可先展示用户最关心的最近记录。
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
