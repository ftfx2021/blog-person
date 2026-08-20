// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function calculateNumericProgress(
  startValue: number,
  targetValue: number,
  latestValue?: number,
): number | null {
  // 没有真实数据点时返回 null，界面应显示“尚未记录”，不能伪造零进度。
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (latestValue == null) return null;
  // 公式天然支持递增和递减目标，再将越界结果限制为 0 到 100。
  const ratio = (latestValue - startValue) / (targetValue - startValue);
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return Math.min(100, Math.max(0, Math.round(ratio * 10000) / 100));
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function calculateMilestoneProgress(
  doneCount: number,
  totalCount: number,
): number | null {
  // 没有里程碑时无法计算，不能用 0% 掩盖数据缺口。
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (totalCount === 0) return null;
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return Math.round((doneCount / totalCount) * 10000) / 100;
}
