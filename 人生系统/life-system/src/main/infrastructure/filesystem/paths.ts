import { mkdir } from "node:fs/promises";
import { join } from "node:path";
import { app } from "electron";

// 集中计算并创建持久化目录，避免数据散落到工作目录。
export async function applicationPaths(): Promise<{
  data: string;
  backups: string;
  exports: string;
  documents: string;
}> {
  const root = app.getPath("userData");
  const paths = {
    data: join(root, "data"),
    backups: join(root, "backups"),
    exports: join(root, "data", "exports"),
    documents: join(root, "data", "documents"),
  };
  await Promise.all(
    Object.values(paths).map((path) => mkdir(path, { recursive: true })),
  );
  return paths;
}
