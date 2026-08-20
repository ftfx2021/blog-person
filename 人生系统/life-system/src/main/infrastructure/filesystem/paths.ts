import { mkdir } from "node:fs/promises";
import { join } from "node:path";
import { app } from "electron";

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export async function applicationPaths(): Promise<{
  data: string;
  backups: string;
  exports: string;
}> {
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  const root = app.getPath("userData");
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  const paths = {
    data: join(root, "data"),
    backups: join(root, "backups"),
    exports: join(root, "data", "exports"),
  };
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await Promise.all(
    Object.values(paths).map((path) => mkdir(path, { recursive: true })),
  );
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return paths;
}
