// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function transitionGoal(current: string, target: string): void {
  // 目标只有 active 可以进入结束态；done/abandoned 是只读终态。
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (current !== "active" || !["done", "abandoned"].includes(target))
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    throw Object.assign(new Error("目标已结束或状态转换不合法"), {
      code: "INVALID_STATE",
    });
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function transitionProject(current: string, target: string): void {
  // active 与 paused 可互转，二者都可结束；done 不允许恢复。
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  const allowed: Record<string, string[]> = {
    active: ["paused", "done"],
    paused: ["active", "done"],
    done: [],
  };
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (!allowed[current]?.includes(target))
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    throw Object.assign(new Error("项目状态转换不合法"), {
      code: "INVALID_STATE",
    });
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function nextTaskStatus(
  current: string,
  action: "advance" | "undo",
): "todo" | "doing" | "done" {
  // 撤销只允许 done 显式回 todo，避免编辑接口暗中恢复完成项。
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (action === "undo") {
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (current !== "done")
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      throw Object.assign(new Error("只有已完成待办可以撤销"), {
        code: "INVALID_STATE",
      });
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return "todo";
  }
  // 推进严格按 todo -> doing -> done，不允许跨级或继续推进终态。
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (current === "todo") return "doing";
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (current === "doing") return "done";
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  throw Object.assign(new Error("已完成待办不可再推进"), {
    code: "INVALID_STATE",
  });
}
