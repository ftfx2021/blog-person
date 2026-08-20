import { describe, expect, it } from "vitest";
import {
  calculateMilestoneProgress,
  calculateNumericProgress,
} from "../../src/shared/domain/progress.js";
import {
  nextTaskStatus,
  transitionGoal,
  transitionProject,
} from "../../src/shared/domain/state-machines.js";
import {
  calculateDailyStreak,
  calculateWeeklyStreak,
} from "../../src/shared/domain/streak.js";

describe("目标进度", () => {
  it("支持递减目标并夹在 0 到 100", () => {
    expect(calculateNumericProgress(70, 60, 65)).toBe(50);
    expect(calculateNumericProgress(70, 60, 55)).toBe(100);
    expect(calculateNumericProgress(70, 60)).toBeNull();
  });
  it("里程碑为空时不显示伪进度", () => {
    expect(calculateMilestoneProgress(0, 0)).toBeNull();
    expect(calculateMilestoneProgress(2, 4)).toBe(50);
  });
});

describe("状态机", () => {
  it("待办只按顺序推进且完成可撤销", () => {
    expect(nextTaskStatus("todo", "advance")).toBe("doing");
    expect(nextTaskStatus("doing", "advance")).toBe("done");
    expect(nextTaskStatus("done", "undo")).toBe("todo");
    expect(() => nextTaskStatus("done", "advance")).toThrow();
  });
  it("目标和项目终态不可恢复", () => {
    expect(() => transitionGoal("done", "active")).toThrow();
    expect(() => transitionProject("done", "active")).toThrow();
  });
});

describe("习惯连续周期", () => {
  it("按相邻自然日计算 daily streak", () =>
    expect(
      calculateDailyStreak([
        "2026-08-20",
        "2026-08-19",
        "2026-08-18",
        "2026-08-16",
      ]),
    ).toBe(3));
  it("按连续达标周计算 weekly streak", () =>
    expect(
      calculateWeeklyStreak(
        ["2026-08-18", "2026-08-20", "2026-08-11", "2026-08-12"],
        2,
      ),
    ).toBe(2));
});
