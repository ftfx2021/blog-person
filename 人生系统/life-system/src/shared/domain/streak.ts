// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
function dateValue(localDate: string): number {
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return Date.parse(`${localDate}T00:00:00Z`);
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
function mondayOf(localDate: string): string {
  const date = new Date(`${localDate}T00:00:00Z`);
  const day = date.getUTCDay() || 7;
  date.setUTCDate(date.getUTCDate() - day + 1);
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return date.toISOString().slice(0, 10);
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function calculateDailyStreak(checkins: string[]): number {
  const days = [...new Set(checkins)].sort((a, b) => b.localeCompare(a));
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (days.length === 0) return 0;
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  let streak = 1;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  for (let index = 1; index < days.length; index += 1) {
    // 只有相邻自然日才延长连续天数，出现缺口立即停止。
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (dateValue(days[index - 1]!) - dateValue(days[index]!) !== 86_400_000)
      break;
    streak += 1;
  }
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return streak;
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function calculateWeeklyStreak(
  checkins: string[],
  weeklyTarget: number,
): number {
  const counts = new Map<string, number>();
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  for (const date of new Set(checkins)) {
    const week = mondayOf(date);
    counts.set(week, (counts.get(week) ?? 0) + 1);
  }
  const achievedWeeks = [...counts.entries()]
    .filter(([, count]) => count >= weeklyTarget)
    .map(([week]) => week)
    .sort((a, b) => b.localeCompare(a));
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (achievedWeeks.length === 0) return 0;
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  let streak = 1;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  for (let index = 1; index < achievedWeeks.length; index += 1) {
    // 连续达标周的周一必须正好相差七天，未达标周会中断 streak。
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (
      dateValue(achievedWeeks[index - 1]!) -
        dateValue(achievedWeeks[index]!) !==
      7 * 86_400_000
    )
      break;
    streak += 1;
  }
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return streak;
}
