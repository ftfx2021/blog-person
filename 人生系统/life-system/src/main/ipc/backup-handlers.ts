import { BrowserWindow, dialog } from "electron";
import {
  emptySchema,
  exportSchema,
  restoreSchema,
} from "../../shared/contracts/system.js";
import { backupService } from "../modules/backup/service.js";
import { applicationPaths } from "../infrastructure/filesystem/paths.js";
import { registerHandler } from "./register.js";
export function registerBackupHandlers(): void {
  // 备份通道只暴露 create、tasks、export、restore 四类 P0 操作。
  // 每个通道先由 registerHandler 执行 Zod parse，再进入备份服务。
  // 这样渲染层无法绕过 confirmation 或构造任意格式的导出参数。
  // 处理器只负责窗口选择和参数路由，不重复实现 SHA-256 或回滚逻辑。
  // backup:progress 由服务广播，处理器返回值仍通过统一 ApiResult 传回页面。
  // 备份通道集中注册，所有来自渲染层的参数都会先经过对应 Zod schema。
  registerHandler("backup:create", emptySchema, backupService.create);
  registerHandler("backup:tasks", emptySchema, backupService.tasks);
  registerHandler("backup:export", exportSchema, ({ format }) =>
    backupService.export(format),
  );
  registerHandler(
    "backup:restore",
    restoreSchema,
    async ({ manifestPath, confirmation }) => {
      // confirmation 是 IPC 安全边界上的第二次校验，不能只依赖页面弹窗。
      // manifestPath 的 SELECT 哨兵只允许打开主进程原生对话框。
      // 用户提供的实际文件路径仍需由 restore 服务解析 manifest 和校验摘要。
      // restoreSchema 的确认短语在 IPC 边界再次校验，不能仅信任页面确认弹窗。
      if (confirmation !== "恢复")
        throw Object.assign(new Error("请输入“恢复”确认"), {
          code: "VALIDATION_ERROR",
        });
      let selected = manifestPath;
      if (manifestPath === "SELECT") {
        // SELECT 是页面约定的文件选择哨兵值，实际路径只在主进程对话框返回后取得。
        // 默认打开备份目录，并将对话框挂到当前窗口，减少误选和失焦操作。
        const paths = await applicationPaths();
        const result = await dialog.showOpenDialog(
          // Electron 类型要求 BaseWindow；运行时允许 undefined，因此用窄断言保留无父窗口场景。
          (BrowserWindow.getFocusedWindow() ?? undefined) as any,
          {
            // parent 绑定当前焦点窗口，defaultPath 引导用户进入受控备份目录。
            defaultPath: paths.backups,
            title: "选择备份 manifest",
            properties: ["openFile"],
            filters: [{ name: "备份清单", extensions: ["json"] }],
          },
        );
        if (result.canceled || !result.filePaths[0])
          // 取消选择属于用户中止，不允许把空路径交给恢复服务继续处理。
          throw Object.assign(new Error("未选择备份"), {
            code: "VALIDATION_ERROR",
          });
        selected = result.filePaths[0];
      }
      return backupService.restore(selected);
    },
  );
}
