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
// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const tasks: TaskStatus[] = [];
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
function update(task: TaskStatus, patch: Partial<TaskStatus>): void {
  Object.assign(task, patch);
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  for (const window of BrowserWindow.getAllWindows())
    window.webContents.send("backup:progress", task);
}
const stamp = () => new Date().toISOString().replace(/[:.]/g, "-");
const hash = (value: Buffer | string) =>
  createHash("sha256").update(value).digest("hex");
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function connectionArgs(): Promise<{
  command: string;
  args: string[];
  environment: NodeJS.ProcessEnv;
  database: string;
}> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const config = await loadConnection();
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (!config)
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    throw Object.assign(new Error("请先配置 MySQL"), {
      code: "DB_UNAVAILABLE",
    });
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return {
    command: "mysqldump",
    args: [
      "--host",
      config.host,
      "--port",
      String(config.port),
      "--user",
      config.user,
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
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
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
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
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  const counts: Record<string, number> = {};
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  for (const table of tables) {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const [rows] = await requirePool().query<RowDataPacket[]>(
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      `SELECT COUNT(*) AS count FROM ${table}`,
    );
    counts[table] = Number(rows[0]!.count);
  }
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return counts;
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function createDump(target: string): Promise<any> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const config = await connectionArgs();
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const { stdout } = await executeFile(config.command, config.args, {
    env: config.environment,
    maxBuffer: 1024 * 1024 * 1024,
    encoding: "buffer",
  } as any);
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await writeFile(target, stdout);
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return config;
}

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function importDump(
  dump: Buffer,
  args: string[],
  environment: NodeJS.ProcessEnv,
): Promise<void> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
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
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function exportEntities(): Promise<Record<string, unknown[]>> {
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
  const result: Record<string, unknown[]> = {};
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
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
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const [rows] = await requirePool().query<RowDataPacket[]>(
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      `SELECT * FROM ${table}`,
    );
    result[table] = rows;
  }
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return result;
}
export const backupService = {
  tasks: async () => tasks,
  create: async () => {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const paths = await applicationPaths();
    // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
    const directory = join(paths.backups, stamp());
    const temporary = `${directory}.tmp`;
    // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
    const task: TaskStatus = {
      id: randomUUID(),
      type: "backup",
      status: "loading",
      progress: 5,
      stage: "准备备份",
      startedAt: new Date().toISOString(),
    };
    tasks.unshift(task);
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    try {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await import("node:fs/promises").then((fs) =>
        fs.mkdir(temporary, { recursive: true }),
      );
      // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
      const dumpPath = join(temporary, "database.sql");
      update(task, { progress: 25, stage: "导出 MySQL" });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await createDump(dumpPath);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const content = await readFile(dumpPath);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const counts = await tableCounts();
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const [versions] = await requirePool().query<RowDataPacket[]>(
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        "SELECT id FROM schema_migrations ORDER BY id",
      );
      // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
      const manifest = {
        formatVersion: 1,
        appVersion: "0.1.0",
        schemaVersions: versions.map((row) => row.id),
        createdAt: new Date().toISOString(),
        dumpFile: "database.sql",
        sha256: hash(content),
        tableCounts: counts,
      };
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await writeFile(
        join(temporary, "manifest.json"),
        JSON.stringify(manifest, null, 2),
        "utf8",
      );
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await rename(temporary, directory);
      update(task, {
        status: "success",
        progress: 100,
        stage: "备份完成",
        finishedAt: new Date().toISOString(),
      });
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { directory, manifest };
    } catch (error) {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await rm(temporary, { recursive: true, force: true });
      update(task, {
        status: "failed",
        stage: "备份失败",
        error: error instanceof Error ? error.message : String(error),
        finishedAt: new Date().toISOString(),
      });
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      throw Object.assign(new Error("备份失败，旧备份未受影响"), {
        code: "BACKUP_FAILED",
      });
    }
  },
  restore: async (manifestPath: string) => {
    // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
    const task: TaskStatus = {
      id: randomUUID(),
      type: "restore",
      status: "loading",
      progress: 5,
      stage: "校验备份",
      startedAt: new Date().toISOString(),
    };
    tasks.unshift(task);
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const paths = await applicationPaths();
    // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
    const safety = join(paths.backups, `restore-safety-${stamp()}.sql`);
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    try {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
      // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
      const dumpPath = join(dirname(manifestPath), manifest.dumpFile);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const dump = await readFile(dumpPath);
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      if (hash(dump) !== manifest.sha256)
        // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
        throw new Error("备份 SHA-256 校验失败");
      update(task, { progress: 25, stage: "建立恢复前安全点" });
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const config = await createDump(safety);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const connection = await loadConnection();
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
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
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await importDump(dump, mysqlArgs, config.environment);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const counts = await tableCounts();
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      for (const [name, count] of Object.entries(
        manifest.tableCounts as Record<string, number>,
      ))
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        if (counts[name] !== count)
          // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
          throw new Error(`表 ${name} 记录数校验失败`);
      update(task, {
        status: "success",
        progress: 100,
        stage: "恢复完成",
        finishedAt: new Date().toISOString(),
      });
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { counts, safetyPoint: safety };
    } catch (error) {
      // 导入或校验失败时尝试使用恢复前安全点回滚，回滚失败仍保留安全点路径供人工处理。
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
      try {
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        const connection = await loadConnection();
        // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
        if (connection) {
          // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
          const rollbackConfig = await connectionArgs();
          // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
          const rollbackDump = await readFile(safety);
          // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
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
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      throw Object.assign(
        new Error(
          `恢复失败：${error instanceof Error ? error.message : String(error)}；恢复前安全点：${safety}`,
        ),
        { code: "RESTORE_FAILED" },
      );
    }
  },
  export: async (format: "json" | "markdown" | "txt") => {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const paths = await applicationPaths();
    // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
    const task: TaskStatus = {
      id: randomUUID(),
      type: "export",
      status: "loading",
      progress: 10,
      stage: "读取业务数据",
      startedAt: new Date().toISOString(),
    };
    tasks.unshift(task);
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    try {
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const entities = await exportEntities();
      const counts = Object.fromEntries(
        Object.entries(entities).map(([key, value]) => [key, value.length]),
      );
      let content: string;
      // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
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
      // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
      const target = join(paths.exports, `life-system-${stamp()}.${extension}`);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await writeFile(target, content, "utf8");
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      const saved = await readFile(target);
      update(task, {
        status: "success",
        progress: 100,
        stage: "导出完成",
        finishedAt: new Date().toISOString(),
      });
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      return { path: target, format, count: counts, sha256: hash(saved) };
    } catch (error) {
      update(task, {
        status: "failed",
        stage: "导出失败",
        error: error instanceof Error ? error.message : String(error),
        finishedAt: new Date().toISOString(),
      });
      // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
      throw error;
    }
  },
};
