function dateValue(localDate: string): number {
  return Date.parse(`${localDate}T00:00:00Z`);
}

function mondayOf(localDate: string): string {
  const date = new Date(`${localDate}T00:00:00Z`);
  const day = date.getUTCDay() || 7;
  date.setUTCDate(date.getUTCDate() - day + 1);
  return date.toISOString().slice(0, 10);
}

export function calculateDailyStreak(checkins: string[]): number {
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
