import { createHash, randomUUID } from "node:crypto";
import { execFile, spawn } from "node:child_process";
import { readFile, rename, rm, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { promisify } from "node:util";
import { BrowserWindow } from "electron";
import type { RowDataPacket } from "mysql2/promise";
import { requirePool } from "../../infrastructure/db/pool.js";
import { applicationPaths } from "../../infrastructure/filesystem/paths.js";
import { loadConnection } from "../settings/service.js";

const executeFile = promisify(execFile);
type TaskStatus = {
  id: string;
  type: "backup" | "restore" | "export";
  status: "loading" | "success" | "failed";
  progress: number;
  stage: string;
  error?: string;
  startedAt: string;
  finishedAt?: string;
};
const tasks: TaskStatus[] = [];
function update(task: TaskStatus, patch: Partial<TaskStatus>): void {
  Object.assign(task, patch);
  for (const window of BrowserWindow.getAllWindows())
    window.webContents.send("backup:progress", task);
}
const stamp = () => new Date().toISOString().replace(/[:.]/g, "-");
const hash = (value: Buffer | string) =>
  createHash("sha256").update(value).digest("hex");
async function connectionArgs(): Promise<{
  command: string;
  args: string[];
  environment: NodeJS.ProcessEnv;
  database: string;
}> {
  const config = await loadConnection();
  if (!config)
    throw Object.assign(new Error("请先配置 MySQL"), {
      code: "DB_UNAVAILABLE",
    });
  return {
    command: "mysqldump",
    args: [
      "--host",
      config.host,
      "--port",
      String(config.port),
      "--user",
      config.user,
      "--single-transaction",
      "--routines",
      "--triggers",
      "--default-character-set=utf8mb4",
      config.database,
    ],
    environment: { ...process.env, MYSQL_PWD: config.password },
    database: config.database,
  };
}
async function tableCounts(): Promise<Record<string, number>> {
  const tables = [
    "goal",
    "project",
    "task",
    "habit",
    "goal_record",
    "milestone",
    "habit_checkin",
  ];
  const counts: Record<string, number> = {};
  for (const table of tables) {
    const [rows] = await requirePool().query<RowDataPacket[]>(
      `SELECT COUNT(*) AS count FROM ${table}`,
    );
    counts[table] = Number(rows[0]!.count);
  }
  return counts;
}
async function createDump(target: string): Promise<any> {
  const config = await connectionArgs();
  const { stdout } = await executeFile(config.command, config.args, {
    env: config.environment,
    maxBuffer: 1024 * 1024 * 1024,
    encoding: "buffer",
  } as any);
  await writeFile(target, stdout);
  return config;
}

async function importDump(
  dump: Buffer,
  args: string[],
  environment: NodeJS.ProcessEnv,
): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    // 使用 spawn 的 stdin 管道导入，避免把完整 dump 拼进 shell 命令或依赖 execFile 不支持的 input 选项。
    const child = spawn("mysql", args, {
      env: environment,
      stdio: ["pipe", "ignore", "pipe"],
    });
    const errors: Buffer[] = [];
    child.stderr.on("data", (chunk: Buffer) => errors.push(chunk));
    child.on("error", reject);
    child.on("close", (code) =>
      code === 0
        ? resolve()
        : reject(
            new Error(
              Buffer.concat(errors).toString("utf8") || `mysql 退出码 ${code}`,
            ),
          ),
    );
    child.stdin.end(dump);
  });
}
async function exportEntities(): Promise<Record<string, unknown[]>> {
  const result: Record<string, unknown[]> = {};
  for (const table of [
    "goal",
    "goal_record",
    "milestone",
    "project",
    "task",
    "habit",
    "habit_checkin",
    "tag",
    "entity_tag",
  ]) {
    const [rows] = await requirePool().query<RowDataPacket[]>(
      `SELECT * FROM ${table}`,
    );
    result[table] = rows;
  }
  return result;
}
export const backupService = {
  tasks: async () => tasks,
  create: async () => {
    const paths = await applicationPaths();
    const directory = join(paths.backups, stamp());
    const temporary = `${directory}.tmp`;
    const task: TaskStatus = {
      id: randomUUID(),
      type: "backup",
      status: "loading",
      progress: 5,
      stage: "准备备份",
      startedAt: new Date().toISOString(),
    };
    tasks.unshift(task);
    try {
      await import("node:fs/promises").then((fs) =>
        fs.mkdir(temporary, { recursive: true }),
      );
      const dumpPath = join(temporary, "database.sql");
      update(task, { progress: 25, stage: "导出 MySQL" });
      await createDump(dumpPath);
      const content = await readFile(dumpPath);
      const counts = await tableCounts();
      const [versions] = await requirePool().query<RowDataPacket[]>(
        "SELECT id FROM schema_migrations ORDER BY id",
      );
      const manifest = {
        formatVersion: 1,
        appVersion: "0.1.0",
        schemaVersions: versions.map((row) => row.id),
        createdAt: new Date().toISOString(),
        dumpFile: "database.sql",
        sha256: hash(content),
        tableCounts: counts,
      };
      await writeFile(
        join(temporary, "manifest.json"),
        JSON.stringify(manifest, null, 2),
        "utf8",
      );
      await rename(temporary, directory);
      update(task, {
        status: "success",
        progress: 100,
        stage: "备份完成",
        finishedAt: new Date().toISOString(),
      });
      return { directory, manifest };
    } catch (error) {
      await rm(temporary, { recursive: true, force: true });
      update(task, {
        status: "failed",
        stage: "备份失败",
        error: error instanceof Error ? error.message : String(error),
        finishedAt: new Date().toISOString(),
      });
      throw Object.assign(new Error("备份失败，旧备份未受影响"), {
        code: "BACKUP_FAILED",
      });
    }
  },
  restore: async (manifestPath: string) => {
    const task: TaskStatus = {
      id: randomUUID(),
      type: "restore",
      status: "loading",
      progress: 5,
      stage: "校验备份",
      startedAt: new Date().toISOString(),
    };
    tasks.unshift(task);
    const paths = await applicationPaths();
    const safety = join(paths.backups, `restore-safety-${stamp()}.sql`);
    try {
      const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
      const dumpPath = join(dirname(manifestPath), manifest.dumpFile);
      const dump = await readFile(dumpPath);
      // 先校验 dump 的 SHA-256，再允许任何覆盖动作，避免导入被篡改的备份。
      if (hash(dump) !== manifest.sha256)
        throw new Error("备份 SHA-256 校验失败");
      update(task, { progress: 25, stage: "建立恢复前安全点" });
      const config = await createDump(safety);
      const connection = await loadConnection();
      if (!connection) throw new Error("MySQL 配置不存在");
      update(task, { progress: 50, stage: "恢复数据库" });
      const mysqlArgs = [
        "--host",
        connection.host,
        "--port",
        String(connection.port),
        "--user",
        connection.user,
        "--default-character-set=utf8mb4",
        config.database,
      ];
      await importDump(dump, mysqlArgs, config.environment);
      const counts = await tableCounts();
      for (const [name, count] of Object.entries(
        manifest.tableCounts as Record<string, number>,
      ))
        if (counts[name] !== count)
          throw new Error(`表 ${name} 记录数校验失败`);
      update(task, {
        status: "success",
        progress: 100,
        stage: "恢复完成",
        finishedAt: new Date().toISOString(),
      });
      return { counts, safetyPoint: safety };
    } catch (error) {
      // 导入或校验失败时尝试使用恢复前安全点回滚，回滚失败仍保留安全点路径供人工处理。
      try {
        const connection = await loadConnection();
        if (connection) {
          const rollbackConfig = await connectionArgs();
          const rollbackDump = await readFile(safety);
          await importDump(
            rollbackDump,
            [
              "--host",
              connection.host,
              "--port",
              String(connection.port),
              "--user",
              connection.user,
              "--default-character-set=utf8mb4",
              rollbackConfig.database,
            ],
            rollbackConfig.environment,
          );
        }
      } catch (rollbackError) {
        console.error("恢复失败后的安全点回滚也失败", rollbackError);
      }
      update(task, {
        status: "failed",
        stage: "恢复失败，正在保留安全点",
        error: error instanceof Error ? error.message : String(error),
        finishedAt: new Date().toISOString(),
      });
      throw Object.assign(
        new Error(
          `恢复失败：${error instanceof Error ? error.message : String(error)}；恢复前安全点：${safety}`,
        ),
        { code: "RESTORE_FAILED" },
      );
    }
  },
  export: async (format: "json" | "markdown" | "txt") => {
    const paths = await applicationPaths();
    const task: TaskStatus = {
      id: randomUUID(),
      type: "export",
      status: "loading",
      progress: 10,
      stage: "读取业务数据",
      startedAt: new Date().toISOString(),
    };
    tasks.unshift(task);
    try {
      const entities = await exportEntities();
      const counts = Object.fromEntries(
        Object.entries(entities).map(([key, value]) => [key, value.length]),
      );
      let content: string;
      if (format === "json") {
        const payload = {
          formatVersion: 1,
          exportedAt: new Date().toISOString(),
          counts,
          entities,
        };
        const body = JSON.stringify(payload, null, 2);
        content = JSON.stringify({ ...payload, sha256: hash(body) }, null, 2);
      } else {
        const heading = (value: string) =>
          format === "markdown" ? `# ${value}` : value;
        const line = (label: string, value: unknown) =>
          format === "markdown"
            ? `- **${label}**：${value ?? ""}`
            : `${label}：${value ?? ""}`;
        content = ["goal", "project", "task", "habit"]
          .flatMap((type) => [
            heading(type),
            ...(entities[type] ?? []).flatMap((item: any) => [
              "",
              format === "markdown"
                ? `## ${item.title ?? item.name}`
                : `[${item.title ?? item.name}]`,
              line("状态", item.status ?? item.frequency_type),
              line("说明", item.description ?? item.note ?? ""),
              line("创建时间", item.created_at),
            ]),
          ])
          .join("\n");
      }
      const extension = format === "markdown" ? "md" : format;
      const target = join(paths.exports, `life-system-${stamp()}.${extension}`);
      await writeFile(target, content, "utf8");
      const saved = await readFile(target);
      update(task, {
        status: "success",
        progress: 100,
        stage: "导出完成",
        finishedAt: new Date().toISOString(),
      });
      return { path: target, format, count: counts, sha256: hash(saved) };
    } catch (error) {
      update(task, {
        status: "failed",
        stage: "导出失败",
        error: error instanceof Error ? error.message : String(error),
        finishedAt: new Date().toISOString(),
      });
      throw error;
    }
  },
};
