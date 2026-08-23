export function transitionGoal(current: string, target: string): void {
  // 目标状态机只有一个可编辑起点，结束态不可逆，保护目标达成/放弃的审计含义。
  // 函数不返回目标状态，因为调用方已持有 target；唯一职责是拒绝非法意图。
  // 目标只有 active 可以进入结束态；done/abandoned 是只读终态。
  if (current !== "active" || !["done", "abandoned"].includes(target))
    throw Object.assign(new Error("目标已结束或状态转换不合法"), {
      code: "INVALID_STATE",
    });
}

export function transitionProject(current: string, target: string): void {
  // 项目允许暂停后恢复，但完成态不能恢复，避免把完成项目再次纳入日常行动。
  // 映射表集中记录合法边，新增状态时必须显式补全而非依赖隐式条件判断。
  // active 与 paused 可互转，二者都可结束；done 不允许恢复。
  const allowed: Record<string, string[]> = {
    active: ["paused", "done"],
    paused: ["active", "done"],
    done: [],
  };
  // 不在 allowed 映射中的状态转换一律视为越权，交给 IPC 层转成 INVALID_STATE。
  if (!allowed[current]?.includes(target))
    throw Object.assign(new Error("项目状态转换不合法"), {
      code: "INVALID_STATE",
    });
}

export function nextTaskStatus(
  current: string,
  action: "advance" | "undo",
): "todo" | "doing" | "done" {
  // 待办仅通过动作推进，调用方不能直接指定任意结果状态来绕过工作流。
  // undo 固定回 todo 而非 doing，明确表示“重新安排”而不是继续未完成的执行。
  // 撤销只允许 done 显式回 todo，避免编辑接口暗中恢复完成项。
  if (action === "undo") {
    if (current !== "done")
      throw Object.assign(new Error("只有已完成待办可以撤销"), {
        code: "INVALID_STATE",
      });
    return "todo";
  }
  // 推进严格按 todo -> doing -> done，不允许跨级或继续推进终态。
  if (current === "todo") return "doing";
  if (current === "doing") return "done";
  throw Object.assign(new Error("已完成待办不可再推进"), {
    code: "INVALID_STATE",
  });
}
