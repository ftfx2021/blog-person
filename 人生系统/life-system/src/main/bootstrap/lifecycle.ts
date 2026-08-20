import type { App } from "electron";
import { BrowserWindow } from "electron";
import { installSecurityPolicy } from "./security.js";
import { createMainWindow } from "./window.js";
import { registerP0Handlers } from "../ipc/handlers.js";
import { configurePool } from "../infrastructure/db/pool.js";
import { loadConnection } from "../modules/settings/service.js";
import { runMigrations } from "../infrastructure/migrations/runner.js";
import { join } from "node:path";
import { registerBackupHandlers } from "../ipc/backup-handlers.js";

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function registerApplicationLifecycle(application: App): void {
  void application.whenReady().then(() => {
    installSecurityPolicy();
    registerP0Handlers();
    registerBackupHandlers();
    // 已保存配置时先恢复连接；连接失败不阻止窗口启动，页面会收到 DB_UNAVAILABLE。
    void loadConnection()
      .then(async (configuration) => {
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        if (!configuration) return;
        // 启动时先检测并补齐迁移，再开放连接池；失败保持设置页可用且不让应用崩溃。
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await runMigrations(
          configuration,
          application.isPackaged
            ? join(process.resourcesPath, "migrations")
            : join(application.getAppPath(), "migrations"),
        );
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        await configurePool(configuration);
      })
      .catch(() => undefined);
    createMainWindow();

    application.on("activate", () => {
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (BrowserWindow.getAllWindows().length === 0) createMainWindow();
    });
  });

  application.on("window-all-closed", () => {
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (process.platform !== "darwin") application.quit();
  });
}
