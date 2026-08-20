import { mkdir } from "node:fs/promises";
import { join } from "node:path";
import { app } from "electron";

export async function applicationPaths(): Promise<{
  data: string;
  backups: string;
  exports: string;
}> {
  const root = app.getPath("userData");
  const paths = {
    data: join(root, "data"),
    backups: join(root, "backups"),
    exports: join(root, "data", "exports"),
  };
  await Promise.all(
    Object.values(paths).map((path) => mkdir(path, { recursive: true })),
  );
  return paths;
}
