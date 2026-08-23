import { ipcMain } from "electron";
import type { IpcMainInvokeEvent } from "electron";
import type { ZodType } from "zod";
import { toResult } from "./result.js";

export function registerHandler<T>(
  channel: string,
  schema: ZodType<T>,
  handler: (input: T, event?: IpcMainInvokeEvent) => Promise<unknown>,
): void {
  // channel 在主进程注册表中固定，渲染层只能调用预先暴露的窄 API。
  // schema.parse 在业务函数之前执行，任何格式错误都不会触及数据库或文件系统。
  // toResult 统一包装成功值和异常，保证 renderer 不需要处理原生 Error 对象。
  // handler 只接收解析后的 T，服务层可以依赖契约而不重复猜测输入形状。
  // ipcMain.handle 的返回 Promise 会被 Electron 安全序列化回 preload。
  // 注册器是 IPC 唯一入口：先 parse，再将服务异常标准化为 ApiResult 返回渲染层。
  ipcMain.handle(channel, async (event, payload) => {
    // payload 来自不可信渲染进程，必须在这里而不是仅在页面模板中验证。
    // 每个白名单 handler 在进入业务服务前统一执行 Zod parse，拒绝任意形状参数。
    // 记录通道名称而不是敏感 payload，便于定位 IPC 错误来源且不泄露 API Key。
    try {
      return await toResult(() => handler(schema.parse(payload), event));
    } catch (error) {
      console.error(`IPC 通道 ${channel} 返回值无法序列化`, error);
      throw error;
    }
  });
}
