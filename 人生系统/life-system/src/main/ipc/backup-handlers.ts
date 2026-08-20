// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { BrowserWindow, dialog } from "electron";
import {
  emptySchema,
  exportSchema,
  restoreSchema,
} from "../../shared/contracts/system.js";
import { backupService } from "../modules/backup/service.js";
import { applicationPaths } from "../infrastructure/filesystem/paths.js";
import { registerHandler } from "./register.js";
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function registerBackupHandlers(): void {
  registerHandler("backup:create", emptySchema, backupService.create);
  registerHandler("backup:tasks", emptySchema, backupService.tasks);
  registerHandler("backup:export", exportSchema, ({ format }) =>
    backupService.export(format),
  );
  registerHandler(
    "backup:restore",
    restoreSchema,
    async ({ manifestPath, confirmation }) => {
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (confirmation !== "恢复")
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw Object.assign(new Error("请输入“恢复”确认"), {
          code: "VALIDATION_ERROR",
        });
      // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
      let selected = manifestPath;
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (manifestPath === "SELECT") {
        // 默认打开备份目录，并将对话框挂到当前窗口，减少误选和失焦操作。
        const paths = await applicationPaths();
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        const result = await dialog.showOpenDialog(
          // Electron 类型要求 BaseWindow；运行时允许 undefined，因此用窄断言保留无父窗口场景。
          (BrowserWindow.getFocusedWindow() ?? undefined) as any,
          {
            defaultPath: paths.backups,
            title: "选择备份 manifest",
            properties: ["openFile"],
            filters: [{ name: "备份清单", extensions: ["json"] }],
          },
        );
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        if (result.canceled || !result.filePaths[0])
          // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
          throw Object.assign(new Error("未选择备份"), {
            code: "VALIDATION_ERROR",
          });
        selected = result.filePaths[0];
      }
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return backupService.restore(selected);
    },
  );
}
