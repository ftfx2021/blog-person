import { dialog } from "electron";
import {
  emptySchema,
  exportSchema,
  restoreSchema,
} from "../../shared/contracts/system.js";
import { backupService } from "../modules/backup/service.js";
import { registerHandler } from "./register.js";
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
      if (confirmation !== "恢复")
        throw Object.assign(new Error("请输入“恢复”确认"), {
          code: "VALIDATION_ERROR",
        });
      let selected = manifestPath;
      if (manifestPath === "SELECT") {
        const result = await dialog.showOpenDialog({
          title: "选择备份 manifest",
          properties: ["openFile"],
          filters: [{ name: "备份清单", extensions: ["json"] }],
        });
        if (result.canceled || !result.filePaths[0])
          throw Object.assign(new Error("未选择备份"), {
            code: "VALIDATION_ERROR",
          });
        selected = result.filePaths[0];
      }
      return backupService.restore(selected);
    },
  );
}
