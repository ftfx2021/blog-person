function dateValue(localDate: string): number {
  // 将 YYYY-MM-DD 固定解释为 UTC 零点，连续间隔计算不受浏览器本地时区和夏令时影响。
  return Date.parse(`${localDate}T00:00:00Z`);
}

function mondayOf(localDate: string): string {
  // 周频率的分组键采用 ISO 周一日期，避免同一自然周在不同调用点被分到不同桶。
  const date = new Date(`${localDate}T00:00:00Z`);
  // getUTCDay 的周日值为 0，改成 7 后才能统一向前回退到周一。
  const day = date.getUTCDay() || 7;
  date.setUTCDate(date.getUTCDate() - day + 1);
  // 周一日期作为自然周分组键，weekly_times 的连续周统计依赖这个稳定键。
  return date.toISOString().slice(0, 10);
}

export function calculateDailyStreak(checkins: string[]): number {
  // 先按日期去重并倒序排列，使重复打卡不会虚增 streak，最近一次打卡成为连续链起点。
  // 历史中的任何缺日都截断连续性，不从较早的连续段中挑选最长值来误导当前状态。
  const days = [...new Set(checkins)].sort((a, b) => b.localeCompare(a));
  if (days.length === 0) return 0;
  let streak = 1;
  for (let index = 1; index < days.length; index += 1) {
    // 只有相邻自然日才延长连续天数，出现缺口立即停止。
    if (dateValue(days[index - 1]!) - dateValue(days[index]!) !== 86_400_000)
      break;
    streak += 1;
  }
  return streak;
}

export function calculateWeeklyStreak(
  checkins: string[],
  weeklyTarget: number,
): number {
  // 周频率先统计每周次数，再仅保留达到 weeklyTarget 的周，防止零散打卡被计为连续周。
  // 和日 streak 一样从最近达标周向过去追溯，因此表达的是“当前连续”而非历史最高纪录。
  const counts = new Map<string, number>();
  for (const date of new Set(checkins)) {
    const week = mondayOf(date);
    counts.set(week, (counts.get(week) ?? 0) + 1);
  }
  const achievedWeeks = [...counts.entries()]
    .filter(([, count]) => count >= weeklyTarget)
    .map(([week]) => week)
    .sort((a, b) => b.localeCompare(a));
  if (achievedWeeks.length === 0) return 0;
  let streak = 1;
  for (let index = 1; index < achievedWeeks.length; index += 1) {
    // 连续达标周的周一必须正好相差七天，未达标周会中断 streak。
    if (
      dateValue(achievedWeeks[index - 1]!) -
        dateValue(achievedWeeks[index]!) !==
      7 * 86_400_000
    )
      break;
    streak += 1;
  }
  return streak;
}
