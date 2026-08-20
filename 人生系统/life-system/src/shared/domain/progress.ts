export function calculateNumericProgress(startValue: number, targetValue: number, latestValue?: number): number | null {
  // 没有真实数据点时返回 null，界面应显示“尚未记录”，不能伪造零进度。
  if (latestValue == null) return null
  // 公式天然支持递增和递减目标，再将越界结果限制为 0 到 100。
  const ratio = (latestValue - startValue) / (targetValue - startValue)
  return Math.min(100, Math.max(0, Math.round(ratio * 10000) / 100))
}

export function calculateMilestoneProgress(doneCount: number, totalCount: number): number | null {
  // 没有里程碑时无法计算，不能用 0% 掩盖数据缺口。
  if (totalCount === 0) return null
  return Math.round((doneCount / totalCount) * 10000) / 100
}
