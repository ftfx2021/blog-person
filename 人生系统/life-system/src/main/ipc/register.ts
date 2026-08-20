import { ipcMain } from "electron";
import type { ZodType } from "zod";
import { toResult } from "./result.js";

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function registerHandler<T>(
  channel: string,
  schema: ZodType<T>,
  handler: (input: T) => Promise<unknown>,
): void {
  ipcMain.handle(channel, async (_event, payload) => {
    // 每个白名单 handler 在进入业务服务前统一执行 Zod parse，拒绝任意形状参数。
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return toResult(() => handler(schema.parse(payload)));
  });
}
