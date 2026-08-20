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

export function registerApplicationLifecycle(application: App): void {
  void application.whenReady().then(() => {
    installSecurityPolicy();
    registerP0Handlers();
    registerBackupHandlers();
    // 已保存配置时先恢复连接；连接失败不阻止窗口启动，页面会收到 DB_UNAVAILABLE。
    void loadConnection()
      .then(async (configuration) => {
        if (!configuration) return;
        // 启动时先检测并补齐迁移，再开放连接池；失败保持设置页可用且不让应用崩溃。
        await runMigrations(
          configuration,
          application.isPackaged
            ? join(process.resourcesPath, "migrations")
            : join(application.getAppPath(), "migrations"),
        );
        await configurePool(configuration);
      })
      .catch(() => undefined);
    createMainWindow();

    application.on("activate", () => {
      if (BrowserWindow.getAllWindows().length === 0) createMainWindow();
    });
  });

  application.on("window-all-closed", () => {
    if (process.platform !== "darwin") application.quit();
  });
}
