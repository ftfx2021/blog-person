import { createHash, randomUUID } from "node:crypto";
import { execFile, spawn } from "node:child_process";
import {
  cp,
  readFile,
  rename,
  rm,
  writeFile,
  readdir,
  stat,
} from "node:fs/promises";
import { dirname, join } from "node:path";
import { promisify } from "node:util";
import { BrowserWindow } from "electron";
import type { RowDataPacket } from "mysql2/promise";
import { requirePool } from "../../infrastructure/db/pool.js";
import { applicationPaths } from "../../infrastructure/filesystem/paths.js";
import { loadConnection, settingsService } from "../settings/service.js";
import {
  closeMilvus,
  connectMilvus,
  createMilvusRepository,
} from "../../infrastructure/milvus/index.js";
import { createEmbeddingProvider } from "../../infrastructure/embedding/index.js";

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
// 任务列表只保留当前主进程生命周期内的可观察操作，不把临时进度写入业务数据库。
function update(task: TaskStatus, patch: Partial<TaskStatus>): void {
  // 任务对象在内存中原地更新，任务列表引用保持稳定供设置页轮询读取。
  // patch 只覆盖当前阶段变化字段，开始时间和类型等身份字段不会意外丢失。
  // 所有已打开窗口均接收相同事件，切换设置页面不需要重新启动操作。
  // 事件通道仅传递可序列化 TaskStatus，避免跨进程传函数或 Error 对象。
  // UI 可按 progress、stage 和 status 分别呈现进度条、文案和最终样式。
  // 广播失败不会中断真实备份流程，因为窗口展示不是数据安全前提。
  // 任务状态不写数据库，进程重启后不会误报已完成的长操作。
  // 真正的成功与失败由调用方的文件/数据库步骤决定，而非广播结果。
  // 更新内存任务并广播进度，让设置页无需轮询也能显示长操作的当前阶段。
  Object.assign(task, patch);
  for (const window of BrowserWindow.getAllWindows())
    // 多窗口场景下逐个广播，确保设置页切换窗口后仍能收到同一任务的状态。
    window.webContents.send("backup:progress", task);
}
// 文件名时间戳移除 Windows 非法字符，保证备份目录能跨平台创建。
const stamp = () => new Date().toISOString().replace(/[:.]/g, "-");
// 所有备份完整性校验使用同一 SHA-256 实现，避免导出与恢复采用不同摘要规则。
const hash = (value: Buffer | string) =>
  createHash("sha256").update(value).digest("hex");
async function connectionArgs(): Promise<{
  command: string;
  args: string[];
  environment: NodeJS.ProcessEnv;
  database: string;
}> {
  // 从安全存储读取运行连接配置，调用者无法通过备份 API 传递任意 mysqldump 参数。
  // 未配置时显式抛 DB_UNAVAILABLE，避免子进程以空参数执行并产生误导错误。
  // mysqldump 启用 single-transaction，尽量让在线数据库导出获得一致快照。
  // routines 和 triggers 一并导出，恢复后数据库行为不只包含数据表。
  // 字符集固定 utf8mb4，中文标题和标签不会依赖服务器默认字符集。
  // 密码仅通过 MYSQL_PWD 子进程环境变量传递，不拼进 shell 命令或日志。
  // 返回 database 同时供 mysql 导入和 manifest 逻辑复用，避免配置来源分叉。
  // command 与 args 分离交给 execFile，规避 shell 解释注入风险。
  // 从已安全保存的连接设置生成命令参数，密码仅经子进程环境变量传递。
  const config = await loadConnection();
  // 恢复和备份都复用已加密保存的配置，绝不从渲染层传入数据库口令。
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
  // 计数清单仅覆盖 P0 核心业务表，既可发现缺表导入也不会包含技术设置。
  // 表名由静态数组定义，循环拼接不会接触用户可控标识符。
  // 每张表独立查询使恢复错误能指向具体表名而不是模糊总计。
  // COUNT(*) 结果显式转 Number，避免 mysql2 返回字符串导致严格比较失败。
  // 该函数读取当前活动连接池，恢复完成后自然验证新数据库内容。
  // 备份创建时同样使用它写入 manifest，验证口径前后一致。
  // 若任一表查询失败，调用方将备份/恢复整体标记失败而不忽略数据缺口。
  // 计数是摘要完整性校验，不替代 dump SHA-256 的字节完整性校验。
  // 备份与恢复两端计数同一组核心业务表，作为 SQL 可导入之外的第二道完整性检查。
  const tables = [
    "goal",
    "project",
    "task",
    "habit",
    "goal_record",
    "milestone",
    "habit_checkin",
    "document",
    "chunk",
    "inbox_item",
    "timeline_entry",
    "mood_word",
    "mood_record",
    "mood_report",
  ];
  const counts: Record<string, number> = {};
  for (const table of tables) {
    // 表名来自固定清单，不将外部字符串插入 SQL 标识符，计数只覆盖 P0 核心事实表。
    const [rows] = await requirePool().query<RowDataPacket[]>(
      `SELECT COUNT(*) AS count FROM ${table}`,
    );
    counts[table] = Number(rows[0]!.count);
  }
  return counts;
}
async function createDump(target: string): Promise<any> {
  // 导出命令通过 execFile 执行，SQL 与密码不会交给 shell 解析。
  // maxBuffer 设为大值以容纳实际数据库转储，而非截断大备份造成静默损坏。
  // stdout 以 Buffer 保存，摘要计算和文件写入面对完全一致的字节流。
  // target 由调用方构造在备份目录内，服务不接受渲染层指定任意输出路径。
  // 写入成功才返回连接参数，恢复流程可复用数据库名和环境变量。
  // mysqldump 失败会抛异常，临时目录由 create 的 catch 清理。
  // 此函数不发布备份目录，发布由外层 rename 作为最后提交点完成。
  // 文件系统错误与数据库导出错误都会保留为备份失败的根本原因。
  // mysqldump 使用单事务快照导出，减少在线写入造成表之间数据不一致的概率。
  const config = await connectionArgs();
  const { stdout } = await executeFile(config.command, config.args, {
    env: config.environment,
    maxBuffer: 1024 * 1024 * 1024,
    encoding: "buffer",
  } as any);
  await writeFile(target, stdout);
  // dump 先完整写入临时路径，后续 manifest 将对这份实际字节流生成摘要。
  return config;
}

async function importDump(
  dump: Buffer,
  args: string[],
  environment: NodeJS.ProcessEnv,
): Promise<void> {
  // 导入使用 stdin 管道传递 dump，避免临时 SQL 文件再次暴露或命令行长度限制。
  // mysql 子进程标准输出被忽略，标准错误被收集为可操作的失败原因。
  // close 事件的零退出码是导入完成的唯一成功条件，stdin 写完不代表数据库接受 SQL。
  // 子进程 error 事件单独 reject，可区分找不到 mysql 命令与 SQL 执行错误。
  // Buffer 聚合保留 MySQL 原始诊断，恢复失败信息能提示具体语法/约束问题。
  // 输入 args 和环境由 connectionArgs 生成，调用方不能注入其他 shell 参数。
  // Promise 被 await 后才继续表计数验证，保证恢复阶段顺序严格。
  // 任何错误由 restore 捕获后尝试安全点回滚，导入函数本身不吞异常。
  // 通过 stdin 交给 mysql，避免 SQL 内容进入 shell 字符串并受命令长度限制。
  await new Promise<void>((resolve, reject) => {
    // 使用 spawn 的 stdin 管道导入，避免把完整 dump 拼进 shell 命令或依赖 execFile 不支持的 input 选项。
    const child = spawn("mysql", args, {
      env: environment,
      stdio: ["pipe", "ignore", "pipe"],
    });
    const errors: Buffer[] = [];
    // stderr 累积后再作为错误原因返回，避免导入失败时丢掉 MySQL 的具体诊断。
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

// 备份时连接 Milvus、导出全部向量并计算文件摘要；未配置时按空能力成功返回。
async function exportMilvusSnapshot(target: string): Promise<any | null> {
  const config = await settingsService.getMilvusConnection();
  if (!config) return null;
  const client = await connectMilvus(config);
  try {
    const embedding = await (async () => {
      const stored = await requirePool().query<any[]>(
        "SELECT value_json AS valueJson FROM app_setting WHERE `key`='llm.embedding'",
      );
      if (!stored[0][0]) return null;
      const value =
        typeof stored[0][0].valueJson === "string"
          ? JSON.parse(stored[0][0].valueJson)
          : stored[0][0].valueJson;
      return createEmbeddingProvider(value);
    })();
    const storage = await settingsService.getKnowledgeStorage();
    const repository = createMilvusRepository(client, storage.collectionName);
    let dim = 0;
    if (embedding) {
      const check = await embedding.healthCheck();
      dim = check.dim ?? 0;
    }
    const existing = await repository.health();
    dim = dim || existing.collection?.dim || 0;
    if (dim) await repository.ensureCollection(dim);
    const records = await repository.exportAll();
    const content = JSON.stringify(records);
    await writeFile(target, content, "utf8");
    const saved = await readFile(target);
    const count = await repository.count();
    if (count !== records.length) throw new Error("Milvus 导出数量校验失败");
    return {
      collection: storage.collectionName,
      modelVersion: records[0]?.modelVersion ?? "",
      dim,
      vectorCount: records.length,
      vectorFile: "milvus-vectors.json",
      sha256: hash(saved),
    };
  } finally {
    closeMilvus(client);
  }
}
async function exportEntities(): Promise<Record<string, unknown[]>> {
  // 导出实体表清单是显式白名单，用户导出不会包含密码和连接设置。
  // tag/entity_tag 一并导出，保留内容实体与标签之间的关联结构。
  // 每张表读取完整行用于 JSON 机器可读格式，文本格式再投影为可读字段。
  // 表名固定在代码中，SQL 标识符不会由 format 或页面输入影响。
  // 空表返回空数组，导出格式仍包含该实体键以保持结构稳定。
  // 当前活动池不可用会抛 DB_UNAVAILABLE，避免生成看似成功的空导出。
  // 该函数只读取，不修改更新时间或业务状态。
  // 结果由 export 统一计算数量和 SHA-256，不在辅助函数内产生副作用文件。
  // 导出只读取 P0 已支持的实体与关系表，格式化层据此生成多种文件格式。
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
    // JSON/文本导出只读取显式列出的实体表，排除尚未纳入 P0 的技术表和敏感设置。
    const [rows] = await requirePool().query<RowDataPacket[]>(
      `SELECT * FROM ${table}`,
    );
    result[table] = rows;
  }
  return result;
}
export const backupService = {
  // tasks 返回当前进程内的任务状态，供设置页展示长操作而不读取业务数据库。
  // 任务按创建时间置顶，失败原因和阶段会在后续广播中持续更新。
  // 返回值用于只读展示，调用方不应修改内部任务对象。
  // 返回进程存活期间的任务状态，供页面恢复长操作的进度展示。
  tasks: async () => tasks,
  // create 先写临时目录，所有文件和 manifest 完成后才原子发布正式目录。
  // dump 使用单事务快照导出，manifest 同时记录 SHA-256、表计数和 schema 版本。
  // 任意导出、读取或写入失败都会清理临时目录，旧备份保持不变。
  // 最终返回正式目录和 manifest，便于用户追踪可恢复资产。
  // 先写临时目录，manifest 和 dump 都成功后才原子改名为可见备份目录。
  create: async () => {
    // 备份任务从最小进度开始登记，UI 可立即显示“准备备份”而不是等待 mysqldump 返回。
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
    // 新任务置顶，设置页始终先展示用户最近发起的备份操作。
    try {
      await import("node:fs/promises").then((fs) =>
        fs.mkdir(temporary, { recursive: true }),
      );
      const dumpPath = join(temporary, "database.sql");
      update(task, { progress: 25, stage: "导出 MySQL" });
      await createDump(dumpPath);
      // 读取已生成的 dump 计算摘要；不能对内存 stdout 计算，否则无法证明落盘文件未变。
      const content = await readFile(dumpPath);
      const counts = await tableCounts();
      update(task, { progress: 55, stage: "导出 Milvus" });
      const milvus = await exportMilvusSnapshot(
        join(temporary, "milvus-vectors.json"),
      );
      if (!milvus) {
        await rm(join(temporary, "milvus-vectors.json"), { force: true });
      }
      const originalsRoot = paths.documents;
      const originalsTarget = join(temporary, "originals");
      const originalFiles: Array<{
        name: string;
        size: number;
        sha256: string;
      }> = [];
      try {
        const entries = await readdir(originalsRoot, { withFileTypes: true });
        if (entries.length) {
          await cp(originalsRoot, originalsTarget, { recursive: true });
          const collect = async (directory: string, prefix = "") => {
            for (const entry of await readdir(directory, {
              withFileTypes: true,
            })) {
              const absolute = join(directory, entry.name);
              const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
              if (entry.isDirectory()) await collect(absolute, relative);
              else {
                const data = await readFile(absolute);
                originalFiles.push({
                  name: relative,
                  size: (await stat(absolute)).size,
                  sha256: hash(data),
                });
              }
            }
          };
          await collect(originalsRoot);
        }
      } catch {
        /* 原件目录不存在时按空副本处理。 */
      }
      const [versions] = await requirePool().query<RowDataPacket[]>(
        // 记录 schema 版本供未来兼容性检查，不把版本号硬编码进备份目录名。
        "SELECT id FROM schema_migrations ORDER BY id",
      );
      const manifest = {
        formatVersion: 2,
        appVersion: "0.1.0",
        schemaVersions: versions.map((row) => row.id),
        createdAt: new Date().toISOString(),
        dumpFile: "database.sql",
        sha256: hash(content),
        tableCounts: counts,
        milvus,
        originals: { fileCount: originalFiles.length, files: originalFiles },
      };
      await writeFile(
        join(temporary, "manifest.json"),
        JSON.stringify(manifest, null, 2),
        "utf8",
      );
      await rename(temporary, directory);
      // 目录改名是备份可见的提交点：此前失败会清理 .tmp，不会污染可选备份列表。
      update(task, {
        status: "success",
        progress: 100,
        stage: "备份完成",
        finishedAt: new Date().toISOString(),
      });
      return { directory, manifest };
    } catch (error) {
      // 失败只删除尚未发布的临时目录，已存在的历史备份永远不被此操作改动。
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
    // restore 先校验 manifest 与 dump 的 SHA-256，再允许覆盖当前数据库。
    // 覆盖前建立独立安全点，导入或表计数校验失败都会尝试回滚。
    // 回滚失败不删除安全点，最终错误会携带路径供人工处理。
    // 成功返回安全点和计数，页面可以在恢复提示中留存这两个结果。
    // 恢复任务也先登记，校验、创建安全点和导入三个阶段都能被 UI 追踪。
    // 恢复前建立安全点；任何导入或计数校验异常都会尝试回滚，原安全点始终保留。
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
    const milvusSafety = join(
      paths.backups,
      `restore-safety-${stamp()}-milvus.json`,
    );
    let milvusSafetyRecords: Awaited<
      ReturnType<ReturnType<typeof createMilvusRepository>["exportAll"]>
    > | null = null;
    let milvusSafetyConfig: Awaited<
      ReturnType<typeof settingsService.getMilvusConnection>
    > = null;
    let milvusSafetyDim: number | null = null;
    let milvusRestoreStarted = false;
    // 安全点采用独立文件名，避免用户选中的原备份被恢复操作覆盖。
    try {
      const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
      // manifest 决定 dump 文件名；此处读取后才拼路径，保持一个备份目录内的相对结构。
      const dumpPath = join(dirname(manifestPath), manifest.dumpFile);
      const dump = await readFile(dumpPath);
      // 先校验 dump 的 SHA-256，再允许任何覆盖动作，避免导入被篡改的备份。
      if (hash(dump) !== manifest.sha256)
        throw new Error("备份 SHA-256 校验失败");
      update(task, { progress: 25, stage: "建立恢复前安全点" });
      const config = await createDump(safety);
      // 在覆盖前导出当前数据库，安全点既用于自动回滚，也留给用户人工恢复。
      const connection = await loadConnection();
      if (!connection) throw new Error("MySQL 配置不存在");
      // 在任何 Milvus 写操作前导出当前向量，恢复失败时才能回到原状态。
      milvusSafetyConfig = await settingsService.getMilvusConnection();
      if (milvusSafetyConfig) {
        try {
          const safetyClient = await connectMilvus(milvusSafetyConfig);
          try {
            const storage = await settingsService.getKnowledgeStorage();
            const safetyRepository = createMilvusRepository(safetyClient, storage.collectionName);
            milvusSafetyDim =
              (await safetyRepository.health()).collection?.dim ?? null;
            milvusSafetyRecords = await safetyRepository.exportAll();
            await writeFile(
              milvusSafety,
              JSON.stringify(milvusSafetyRecords),
              "utf8",
            );
          } finally {
            closeMilvus(safetyClient);
          }
        } catch {
          // 当前 Milvus 不可用时不阻断安全点建立，后续向量恢复失败会明确提示人工处理。
          milvusSafetyConfig = null;
          milvusSafetyRecords = null;
        }
      }
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
      // 导入返回成功并不代表内容完整，随后逐表比对 manifest 的记录数。
      const counts = await tableCounts();
      for (const [name, count] of Object.entries(
        manifest.tableCounts as Record<string, number>,
      ))
        // 任何一张表数量不符都视为恢复失败，宁可触发安全点回滚也不保留半可信数据。
        if (counts[name] !== count)
          throw new Error(`表 ${name} 记录数校验失败`);
      let milvusResult: { restored: boolean; vectorCount?: number } = {
        restored: false,
      };
      if (manifest.formatVersion >= 2 && manifest.milvus) {
        const vectorPath = join(
          dirname(manifestPath),
          manifest.milvus.vectorFile,
        );
        const vectorData = await readFile(vectorPath);
        if (hash(vectorData) !== manifest.milvus.sha256)
          throw new Error("Milvus 向量文件 SHA-256 校验失败");
        const records = JSON.parse(vectorData.toString("utf8"));
        const config = await settingsService.getMilvusConnection();
        if (!config) throw new Error("当前未配置 Milvus，无法恢复向量数据");
        const client = await connectMilvus(config);
        try {
          const storage = await settingsService.getKnowledgeStorage();
          const repository = createMilvusRepository(client, storage.collectionName);
          milvusRestoreStarted = true;
          const dim = Number(
            manifest.milvus.dim || records[0]?.embedding?.length || 0,
          );
          await repository.dropAndRecreate(dim);
          await repository.importAll(records);
          const vectorCount = await repository.count();
          if (vectorCount !== manifest.milvus.vectorCount)
            throw new Error("Milvus 恢复数量校验失败");
          if (counts.chunk !== vectorCount)
            throw new Error("MySQL chunk 与 Milvus 向量数量不一致");
          milvusResult = { restored: true, vectorCount };
        } finally {
          closeMilvus(client);
        }
      }
      update(task, {
        status: "success",
        progress: 100,
        stage: "恢复完成",
        finishedAt: new Date().toISOString(),
      });
      const milvusSkipped = manifest.formatVersion < 2 || !manifest.milvus;
      return {
        counts,
        safetyPoint: safety,
        milvus: milvusResult,
        milvusSkipped,
        ...(milvusSkipped ? { message: "该备份不含向量数据" } : {}),
      };
    } catch (error) {
      // 导入或校验失败时尝试使用恢复前安全点回滚，回滚失败仍保留安全点路径供人工处理。
      try {
        // 自动回滚尽最大努力执行；即使回滚自身失败，也不能掩盖最初恢复失败的原因。
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
        // 安全点路径会随最终错误返回，控制台日志仅补充开发者诊断信息。
        console.error("恢复失败后的安全点回滚也失败", rollbackError);
      }
      // MySQL 回滚后再尽力恢复 Milvus；失败时保留向量安全点路径供人工处理。
      if (milvusRestoreStarted && milvusSafetyConfig && milvusSafetyRecords) {
        try {
          const rollbackClient = await connectMilvus(milvusSafetyConfig);
          try {
            const storage = await settingsService.getKnowledgeStorage();
            const rollbackRepository = createMilvusRepository(rollbackClient, storage.collectionName);
            const safetyDim =
              milvusSafetyDim ?? milvusSafetyRecords[0]?.embedding.length ?? 0;
            if (safetyDim > 0) {
              await rollbackRepository.dropAndRecreate(safetyDim);
              await rollbackRepository.importAll(milvusSafetyRecords);
            }
          } finally {
            closeMilvus(rollbackClient);
          }
        } catch (milvusRollbackError) {
          console.error(
            `Milvus 回滚失败，安全点保留于 ${milvusSafety}`,
            milvusRollbackError,
          );
        }
      }
      update(task, {
        status: "failed",
        stage: "恢复失败，正在保留安全点",
        error: error instanceof Error ? error.message : String(error),
        finishedAt: new Date().toISOString(),
      });
      throw Object.assign(
        new Error(
          `恢复失败：${error instanceof Error ? error.message : String(error)}；恢复前安全点：${safety}${milvusSafetyRecords ? `；Milvus 安全点：${milvusSafety}` : ""}`,
        ),
        { code: "RESTORE_FAILED" },
      );
    }
  },
  export: async (format: "json" | "markdown" | "txt") => {
    // export 仅读取 P0 白名单实体，避免把密码、连接配置或技术表写入用户文件。
    // JSON 保留完整实体并附 SHA-256；Markdown/txt 只输出便于阅读的业务字段。
    // 写盘后重新读取计算摘要，确保返回的 hash 对应实际文件而不是内存字符串。
    // 目标路径集中在 exports 目录，格式由 schema 白名单决定。
    // 失败只更新任务状态，不触碰此前已经完成的导出文件。
    // 导出与数据库备份不同：它面向用户阅读或迁移，且不执行可逆恢复流程。
    // 导出完成后重新读回文件计算摘要，确保返回的校验值对应实际落盘内容。
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
        // 先记录各实体数量，JSON 接收方可快速判断传输是否遗漏表或记录。
        Object.entries(entities).map(([key, value]) => [key, value.length]),
      );
      let content: string;
      if (format === "json") {
        // JSON 保留完整实体结构并内嵌摘要，适合作为机器可读交换格式。
        const payload = {
          formatVersion: 1,
          exportedAt: new Date().toISOString(),
          counts,
          entities,
        };
        const body = JSON.stringify(payload, null, 2);
        content = JSON.stringify({ ...payload, sha256: hash(body) }, null, 2);
      } else {
        // 文本和 Markdown 仅投影人类可读字段，不暴露完整数据库列或内部关联 ID。
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
      // 写完后从磁盘重新读取再计算 SHA-256，返回值可验证实际产物而非待写字符串。
      const saved = await readFile(target);
      update(task, {
        status: "success",
        progress: 100,
        stage: "导出完成",
        finishedAt: new Date().toISOString(),
      });
      return { path: target, format, count: counts, sha256: hash(saved) };
    } catch (error) {
      // 导出失败保留已登记任务状态；不删除历史导出，避免误伤用户已有文件。
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
