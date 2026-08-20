export function transitionGoal(current: string, target: string): void {
  // 目标只有 active 可以进入结束态；done/abandoned 是只读终态。
  if (current !== "active" || !["done", "abandoned"].includes(target))
    throw Object.assign(new Error("目标已结束或状态转换不合法"), {
      code: "INVALID_STATE",
    });
}

export function transitionProject(current: string, target: string): void {
  // active 与 paused 可互转，二者都可结束；done 不允许恢复。
  const allowed: Record<string, string[]> = {
    active: ["paused", "done"],
    paused: ["active", "done"],
    done: [],
  };
  if (!allowed[current]?.includes(target))
    throw Object.assign(new Error("项目状态转换不合法"), {
      code: "INVALID_STATE",
    });
}

export function nextTaskStatus(
  current: string,
  action: "advance" | "undo",
): "todo" | "doing" | "done" {
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
