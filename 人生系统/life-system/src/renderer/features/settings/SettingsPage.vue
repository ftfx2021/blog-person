<script setup lang="ts">
// 设置页面协调本机连接配置与备份任务；真正的凭据和数据库操作始终留在主进程。
import { onMounted, reactive, ref } from "vue";
import {
  Connection,
  Download,
  Upload,
  Document,
  Loading,
} from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { toIpcPayload, useApi } from "../../shared/api";
const { call } = useApi();
const mysql = reactive<any>({
  host: "127.0.0.1",
  port: 3306,
  user: "root",
  password: "",
  database: "life_system",
  connectTimeout: 5000,
});
const reminders = reactive<any>({
  criticalEnabled: true,
  periodicEnabled: true,
  recommendationEnabled: false,
  frequency: "realtime",
  aggregationMinutes: 30,
  readRetentionDays: 30,
  recommendationRequiresConfirmation: true,
});
const knowledgeImport = reactive<any>({
  maxFileSizeMb: 20,
  parseTimeoutSeconds: 60,
});
const knowledgeStorage = reactive<any>({ collectionName: "knowledge_chunk_v2" });
const ocr = reactive<any>({
  provider: "disabled",
  baidu: { apiKey: "", secretKey: "" },
  tencent: { secretId: "", secretKey: "", region: "ap-guangzhou" },
  aliyun: {
    accessKeyId: "",
    accessKeySecret: "",
    endpoint: "ocr-api.cn-hangzhou.aliyuncs.com",
  },
});
const ocrTest = ref<any>();
const health = ref<any>(),
  milvus = ref<any>(),
  milvusTest = ref<any>(),
  aiHealth = ref<any>(),
  aiConfigs = ref<any>(),
  llmTest = ref<any>(),
  embeddingTest = ref<any>(),
  tasks = ref<any[]>([]),
  busy = ref("");
const llm = reactive<any>({
  provider: "openai",
  baseURL: "https://api.openai.com",
  apiKey: "",
  model: "gpt-4o-mini",
  protocol: "chat-completions",
});
const embedding = reactive<any>({
  baseURL: "http://127.0.0.1:11434",
  // 当前本机已安装该模型，首次打开即可直接测试；用户仍可替换为 bge-m3 等模型。
  model: "qwen3-embedding:0.6b",
});
const milvusConfig = reactive<any>({
  address: "127.0.0.1:19530",
  username: "",
  password: "",
  ssl: false,
  connectTimeout: 10000,
});
const defaultProviderPresets = [
  { value: "openai", label: "OpenAI", baseURL: "https://api.openai.com" },
  { value: "deepseek", label: "DeepSeek", baseURL: "https://api.deepseek.com" },
  {
    value: "qwen",
    label: "通义千问",
    baseURL: "https://dashscope.aliyuncs.com/compatible-mode",
  },
  { value: "moonshot", label: "Kimi", baseURL: "https://api.moonshot.cn" },
  {
    value: "zhipu",
    label: "智谱",
    baseURL: "https://open.bigmodel.cn/api/paas",
  },
  { value: "ollama", label: "Ollama", baseURL: "http://127.0.0.1:11434" },
  { value: "custom", label: "自定义", baseURL: "" },
];
async function load() {
  // 页面初始化先读取脱敏 MySQL 配置，避免把明文密码写入渲染状态。
  // AI/Milvus 状态与数据库偏好分别加载，单项失败不会阻断其他设置展示。
  // 任务列表仅在数据库可用时读取，未配置连接时保留空列表并等待用户修复。
  // saved 为空表示首次启动，表单保留默认连接参数供用户填写。
  // 该方法不修改任何设置，只建立页面当前展示快照。
  // 失败信息写入控制台而不打断设置页面，方便首次配置流程继续。
  // 先回显脱敏配置，再尽力读取依赖数据库的偏好和任务，未连接数据库不阻断设置页。
  const saved: any = await call(() => window.lifeSystem.settings.getMysql());
  if (saved) Object.assign(mysql, saved, { password: "" });
  try {
    const savedMilvus = await call(
      () => window.lifeSystem.settings.milvus.get(),
      { silent: true },
    );
    if (savedMilvus)
      Object.assign(milvusConfig, savedMilvus, {
        password: savedMilvus.password || "",
      });
    milvus.value = await call(
      () => window.lifeSystem.settings.milvus.health(),
      { silent: true },
    );
  } catch (caught) {
    milvus.value = {
      ok: false,
      detail: (caught as any)?.message || "向量数据库不可用",
      collection: null,
    };
  }
  // MySQL 尚未配置时 app_setting 不可读，仍保留默认表单让用户先完成数据源配置。
  try {
    aiConfigs.value = await call(
      () => window.lifeSystem.settings.getAiConfigs(),
      { silent: true },
    );
    if (aiConfigs.value?.llm) Object.assign(llm, aiConfigs.value.llm);
    if (aiConfigs.value?.embedding)
      Object.assign(embedding, aiConfigs.value.embedding);
    aiHealth.value = await call(
      () => window.lifeSystem.settings.getAiHealth(),
      { silent: true },
    );
  } catch (caught) {
    console.warn("AI 配置将在 MySQL 连接后加载", caught);
    aiConfigs.value = { providers: defaultProviderPresets };
  }
  try {
    Object.assign(
      reminders,
      await call(() => window.lifeSystem.settings.getReminders(), {
        silent: true,
      }),
    );
    Object.assign(
      knowledgeImport,
      await call(() => window.lifeSystem.settings.knowledgeImport.get(), {
        silent: true,
      }),
    );
    Object.assign(
      knowledgeStorage,
      await call(() => window.lifeSystem.settings.knowledgeStorage.get(), {
        silent: true,
      }),
    );
    Object.assign(
      ocr,
      await call(() => window.lifeSystem.settings.ocr.get(), { silent: true }),
    );
    tasks.value = await call(() => window.lifeSystem.backup.tasks(), {
      silent: true,
    });
  } catch (caught) {
    console.warn("数据库未连接，系统任务将在连接后加载", caught);
  }
}
// 测试当前 Milvus 草稿配置，不写入 app_setting。
async function testMilvus() {
  busy.value = "milvus-test";
  milvusTest.value = undefined;
  try {
    milvusTest.value = await call(() =>
      window.lifeSystem.settings.milvus.test(toIpcPayload(milvusConfig)),
    );
  } catch (caught: any) {
    milvusTest.value = {
      ok: false,
      detail: caught?.message || "Milvus 连接测试失败",
    };
  } finally {
    busy.value = "";
  }
}
// 保存 Milvus 配置并刷新 collection 健康状态。
async function saveMilvus() {
  busy.value = "milvus-save";
  milvusTest.value = undefined;
  try {
    milvus.value = await call(() =>
      window.lifeSystem.settings.milvus.save(toIpcPayload(milvusConfig)),
    );
  } catch (caught: any) {
    milvus.value = {
      ok: false,
      detail: caught?.message || "Milvus 配置保存失败",
      collection: null,
    };
  } finally {
    busy.value = "";
  }
}
// 根据供应商预设更新 baseURL；自定义选项保留用户手动输入的地址。
function selectProvider(value: string) {
  // 预设只自动填充地址，模型和密钥由用户按账号实际可用值维护。
  const preset = aiConfigs.value?.providers?.find(
    (item: any) => item.value === value,
  );
  if (preset) llm.baseURL = preset.baseURL;
}
// 测试当前 LLM 草稿配置，不写入数据库，只更新页面上的测试结果。
async function testLlm() {
  busy.value = "llm-test";
  // 清除上一轮结果，避免测试中仍展示过期健康状态。
  llmTest.value = undefined;
  try {
    llmTest.value = await call(() =>
      window.lifeSystem.settings.testLlmConnection(toIpcPayload(llm)),
    );
  } finally {
    busy.value = "";
  }
}
// 保存 LLM 配置但不发起远端请求，连接测试由用户主动点击测试按钮触发。
async function saveLlm() {
  busy.value = "llm-save";
  // 保存期间改由 loading 状态承载反馈，避免把保存误认为连接测试。
  llmTest.value = undefined;
  try {
    await call(() =>
      window.lifeSystem.settings.saveLlmConfig(toIpcPayload(llm)),
    );
    llmTest.value = {
      ok: true,
      detail: "LLM 配置已保存，需手动测试连接",
      model: llm.model,
    };
  } finally {
    busy.value = "";
  }
}
// 测试当前 Ollama embedding 草稿配置，并显示服务返回的向量维度。
async function testEmbedding() {
  busy.value = "embedding-test";
  // 清除上一轮结果，避免测试中仍展示过期维度或失败原因。
  embeddingTest.value = undefined;
  try {
    embeddingTest.value = await call(() =>
      window.lifeSystem.settings.testEmbeddingConnection(
        toIpcPayload(embedding),
      ),
    );
  } catch (caught: any) {
    // preload 缺失或 IPC 异常也要落到卡片提示，不能只留在控制台。
    embeddingTest.value = {
      ok: false,
      detail: caught?.message || "Embedding 连接测试失败",
    };
  } finally {
    busy.value = "";
  }
}
// 保存 embedding 配置但不发起远端请求，连接测试由用户主动点击测试按钮触发。
async function saveEmbedding() {
  busy.value = "embedding-save";
  // 保存期间清除旧测试结果，避免旧状态掩盖当前保存动作。
  embeddingTest.value = undefined;
  try {
    await call(() =>
      window.lifeSystem.settings.saveEmbeddingConfig(toIpcPayload(embedding)),
    );
    embeddingTest.value = {
      ok: true,
      detail: "Embedding 配置已保存，需手动测试连接",
      model: embedding.model,
    };
  } catch (caught: any) {
    // 数据库未配置或 IPC 失败时保留明确原因，用户可据此先修复 MySQL 或 Ollama。
    embeddingTest.value = {
      ok: false,
      detail: caught?.message || "Embedding 配置保存失败",
    };
  } finally {
    busy.value = "";
  }
}
async function test() {
  // 使用当前表单值测试候选连接，不先保存也不替换运行中的连接池。
  // busy 标记阻止重复点击导致多个临时连接同时建立。
  // 成功返回版本和延迟，失败由 useApi 统一显示错误。
  // finally 清除 busy，确保成功和异常都能恢复按钮状态。
  // 测试使用当前尚未保存的表单值，让用户能在落盘前验证主机、账号和网络。
  busy.value = "test";
  try {
    health.value = await call(() =>
      window.lifeSystem.settings.testMysql(toIpcPayload(mysql)),
    );
  } finally {
    busy.value = "";
  }
}
async function save() {
  // 保存动作将密码交给主进程安全存储，页面不负责加密或写文件。
  // 主进程成功完成连接初始化和迁移后，再执行健康检查确认可用。
  // busy 期间禁止重复保存，避免两个迁移流程交叉运行。
  // 失败不清空用户表单，便于修正配置后重试。
  // finally 恢复交互状态，保证错误路径也不会永久禁用按钮。
  // 保存连接后立即查询健康状态，避免“保存成功”掩盖迁移或连接初始化失败。
  busy.value = "save";
  try {
    await call(() => window.lifeSystem.settings.saveMysql(toIpcPayload(mysql)));
    health.value = await call(() => window.lifeSystem.settings.health());
  } finally {
    busy.value = "";
  }
}
async function saveReminders() {
  // 提醒偏好单独提交，避免调整通知时再次发送数据库密码。
  // 输入 schema 在 preload 和主进程双重校验，页面只负责收集控件值。
  // 保存成功后主进程下次仪表盘聚合立即使用新配置。
  // 失败由统一 API 提示，不在页面维护第二份本地持久化副本。
  // 提醒设置独立保存，避免用户调整偏好时意外重新提交数据库密码。
  await call(() =>
    window.lifeSystem.settings.saveReminders(toIpcPayload(reminders)),
  );
}
async function saveKnowledgeImport() {
  // 导入限制独立保存，文件复制和网页抓取会读取该受控设置。
  await call(() =>
    window.lifeSystem.settings.knowledgeImport.save(
      toIpcPayload(knowledgeImport),
    ),
  );
  ElMessage.success("知识库导入设置已保存");
}
async function saveKnowledgeStorage() {
  busy.value = "knowledge-storage-save";
  try {
    await call(() => window.lifeSystem.settings.knowledgeStorage.save(toIpcPayload(knowledgeStorage)));
    milvus.value = await call(() => window.lifeSystem.settings.milvus.health());
    ElMessage.success("知识库存储设置已保存");
  } finally { busy.value = ""; }
}
async function migrateKnowledgeVectors() {
  await ElMessageBox.confirm("将把 knowledge_chunk_v1 中的存量向量复制到当前 collection。目标 collection 必须为空。", "执行存量向量迁移", { type: "warning" });
  busy.value = "knowledge-storage-migrate";
  try {
    const result: any = await call(() => window.lifeSystem.settings.knowledgeStorage.migrate());
    milvus.value = await call(() => window.lifeSystem.settings.milvus.health());
    ElMessage.success(`存量向量迁移完成，共 ${result.migrated} 条`);
  } finally { busy.value = ""; }
}
async function saveOcr() {
  // 保存前通过 preload 把普通表单对象转为纯 DTO，密钥加密仅发生在主进程。
  await call(() => window.lifeSystem.settings.ocr.save(toIpcPayload(ocr)));
  ElMessage.success("OCR 设置已保存");
}
async function testOcr() {
  // 测试候选配置不落库，也不会上传任何用户文档内容。
  ocrTest.value = await call(() =>
    window.lifeSystem.settings.ocr.test(toIpcPayload(ocr)),
  );
}
async function backup() {
  // 备份按钮只发起任务，不在渲染层直接访问文件系统或数据库。
  // 主进程通过进度广播更新任务阶段，完成后再主动刷新任务列表。
  // busy 标记避免用户重复发起多个大体量 mysqldump。
  // 失败由主进程保留旧备份并把具体原因反馈给用户。
  // finally 清除忙碌状态，允许用户在失败后重新尝试。
  // 备份完成后重新拉取任务列表，使进度和最终目录来自主进程权威状态。
  busy.value = "backup";
  try {
    await call(() => window.lifeSystem.backup.create());
    tasks.value = await call(() => window.lifeSystem.backup.tasks());
  } finally {
    busy.value = "";
  }
}
async function restore() {
  // 恢复是覆盖数据库的不可逆操作，必须先显示明确的二次确认。
  // 用户取消或关闭确认框时直接返回，绝不向主进程发起 restore IPC。
  // confirmation 短语由确认结果驱动，不能再使用固定“恢复”绕过 UI 选择。
  // 主进程收到 SELECT 后负责原生文件选择、SHA-256 校验和安全点建立。
  // 成功提示包含安全点路径，便于用户在恢复后仍保留人工回滚依据。
  // 失败提示保留主进程的原因和安全点信息，任务列表刷新也尽力执行。
  // finally 统一释放 busy，取消、成功和失败都能恢复页面交互。
  // 恢复会覆盖当前数据库，先由用户明确确认，取消时不发送 IPC。
  let action: string;
  try {
    action = await ElMessageBox.confirm(
      "将用所选备份覆盖当前数据库，此操作不可撤销。是否继续？",
      "确认恢复",
      {
        type: "warning",
        confirmButtonText: "继续恢复",
        cancelButtonText: "取消",
      },
    );
  } catch {
    return;
  }
  // 只有确认结果为 confirm 时，才填写 restoreSchema 要求的确认文本。
  if (action !== "confirm") return;
  busy.value = "restore";
  try {
    const result: any = await call(() =>
      window.lifeSystem.backup.restore({
        // SELECT 是主进程约定的哨兵值，用来打开原生 manifest 文件选择框。
        manifestPath: "SELECT",
        confirmation: action === "confirm" ? "恢复" : "",
      }),
    );
    tasks.value = await call(() => window.lifeSystem.backup.tasks());
    // 成功反馈包含主进程返回的安全点，便于用户留存恢复凭据。
    ElMessage.success(
      `恢复完成${result?.safetyPoint ? `，安全点：${result.safetyPoint}` : ""}`,
    );
  } catch (caught: any) {
    // 失败提示保留安全点路径和主进程失败原因，方便人工回滚或排查。
    ElMessage.error(`恢复失败：${caught?.message ?? "未知错误"}`);
    tasks.value = await call(() => window.lifeSystem.backup.tasks()).catch(
      () => tasks.value,
    );
  } finally {
    busy.value = "";
  }
}
async function exportData(format: "json" | "markdown" | "txt") {
  // 导出格式来自模板白名单，页面不允许用户指定任意文件扩展名或路径。
  // 主进程写盘并重新计算 SHA-256，页面只展示任务结果而不复制文件逻辑。
  // 每种格式使用独立 busy 标识，避免导出过程中重复触发同一按钮。
  // 失败状态仍由主进程任务保留，用户可查看阶段和错误信息。
  // 完成后刷新任务列表，确保展示的是实际落盘结果。
  // 以格式名作为忙碌标识，让多个导出按钮只禁用当前正在运行的操作。
  busy.value = format;
  try {
    await call(() => window.lifeSystem.backup.export({ format }));
    tasks.value = await call(() => window.lifeSystem.backup.tasks());
  } finally {
    busy.value = "";
  }
}
// 组件进入后载入本机设置和已有任务；调用失败在 load 内按是否已连接处理。
onMounted(load);
</script>
<template>
  <div class="page-head">
    <div>
      <h1>设置</h1>
      <p>数据源、提醒与本机数据维护。</p>
    </div>
  </div>
  <div class="settings-grid">
    <section class="settings-section">
      <h2>MySQL 数据源</h2>
      <el-form label-position="top"
        ><div class="inline-fields">
          <el-form-item label="主机"
            ><el-input v-model="mysql.host" /></el-form-item
          ><el-form-item label="端口"
            ><el-input-number v-model="mysql.port" :controls="false"
          /></el-form-item>
        </div>
        <div class="inline-fields">
          <el-form-item label="用户名"
            ><el-input v-model="mysql.user" /></el-form-item
          ><el-form-item label="数据库"
            ><el-input v-model="mysql.database"
          /></el-form-item>
        </div>
        <el-form-item label="密码"
          ><el-input v-model="mysql.password" type="password" show-password
        /></el-form-item>
        <div class="row-actions">
          <el-button :icon="Connection" :loading="busy === 'test'" @click="test"
            >测试连接</el-button
          ><el-button type="primary" :loading="busy === 'save'" @click="save"
            >保存并迁移</el-button
          >
        </div>
        <el-alert
          v-if="health"
          style="margin-top: 16px"
          type="success"
          :closable="false"
          :title="`连接正常 · MySQL ${health.version} · ${health.latencyMs} ms`"
      /></el-form>
    </section>
    <section class="settings-section">
      <h2>Milvus 数据源（知识向量库）</h2>
      <el-form label-position="top">
        <el-form-item label="地址"
          ><el-input
            v-model="milvusConfig.address"
            placeholder="127.0.0.1:19530"
        /></el-form-item>
        <div class="form-grid">
          <el-form-item label="用户名"
            ><el-input v-model="milvusConfig.username"
          /></el-form-item>
          <el-form-item label="密码"
            ><el-input
              v-model="milvusConfig.password"
              type="password"
              show-password
              placeholder="已保存密码显示为掩码"
          /></el-form-item>
        </div>
        <el-form-item label="启用 TLS"
          ><el-switch v-model="milvusConfig.ssl"
        /></el-form-item>
        <div class="row-actions">
          <el-button :loading="busy === 'milvus-test'" @click="testMilvus"
            >测试连接</el-button
          >
          <el-button
            type="primary"
            :loading="busy === 'milvus-save'"
            @click="saveMilvus"
            >保存</el-button
          >
        </div>
        <el-alert
          v-if="milvusTest"
          :type="milvusTest.ok ? 'success' : 'error'"
          :closable="false"
          :title="milvusTest.detail"
          style="margin-top: 16px"
        >
          <template #default
            ><span v-if="milvusTest.version"
              >版本：{{ milvusTest.version }} ·
              {{ milvusTest.latencyMs }} ms</span
            ><span v-if="milvusTest.collections?.length">
              · Collection：{{ milvusTest.collections.join("、") }}</span
            ></template
          >
        </el-alert>
        <el-alert
          v-if="milvus"
          :type="milvus.ok ? 'success' : 'warning'"
          :closable="false"
          :title="
            milvus.ok
              ? `连接正常${milvus.version ? ` · ${milvus.version}` : ''}`
              : milvus.detail || '向量检索不可用，全文搜索与核心功能不受影响'
          "
          style="margin-top: 16px"
        />
        <div
          v-if="milvus?.collection"
          class="list-meta"
          style="margin-top: 10px"
        >
          {{ knowledgeStorage.collectionName }}：{{ milvus.collection.exists ? "已创建" : "未创建"
          }}<span v-if="milvus.collection.dim">
            · 维度 {{ milvus.collection.dim }}</span
          ><span v-if="milvus.collection.rowCount !== undefined">
            · 向量 {{ milvus.collection.rowCount }}</span
          >
        </div>
        <el-alert
          v-if="milvus?.collection?.exists === false && aiHealth?.embedding?.ok"
          type="info"
          :closable="false"
          title="配置 Embedding 后自动创建向量库"
          style="margin-top: 10px"
        />
        <el-alert
          v-if="
            milvus?.collection?.expectedDim &&
            milvus.collection.dim &&
            milvus.collection.expectedDim !== milvus.collection.dim
          "
          type="warning"
          :closable="false"
          title="Embedding 模型维度与向量库不匹配，请更换模型或重建向量库"
          style="margin-top: 10px"
        />
        <el-alert
          v-if="milvus && !milvus.ok"
          type="info"
          :closable="false"
          title="向量检索不可用，全文搜索与核心功能不受影响"
          style="margin-top: 10px"
        />
      </el-form>
    </section>
    <section class="settings-section">
      <h2>AI 配置 · LLM 连接</h2>
      <el-form label-position="top">
        <el-form-item label="供应商预设">
          <el-select
            v-model="llm.provider"
            @change="selectProvider"
            style="width: 100%"
          >
            <el-option
              v-for="item in aiConfigs?.providers || defaultProviderPresets"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL"
          ><el-input v-model="llm.baseURL"
        /></el-form-item>
        <el-form-item label="API Key"
          ><el-input v-model="llm.apiKey" type="password" show-password
        /></el-form-item>
        <el-form-item label="模型"
          ><el-input v-model="llm.model"
        /></el-form-item>
        <el-form-item label="协议">
          <el-select v-model="llm.protocol" style="width: 100%">
            <el-option
              label="OpenAI Chat Completions"
              value="chat-completions"
            />
            <el-option label="OpenAI Responses" value="responses" />
          </el-select>
        </el-form-item>
        <div class="row-actions">
          <el-button :loading="busy === 'llm-test'" @click="testLlm"
            >测试连接</el-button
          >
          <el-button
            type="primary"
            :loading="busy === 'llm-save'"
            @click="saveLlm"
            >保存</el-button
          >
        </div>
        <el-alert
          v-if="busy === 'llm-test' || busy === 'llm-save'"
          type="info"
          :closable="false"
          style="margin-top: 16px"
        >
          <template #title>
            <span class="ai-testing-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              {{
                busy === "llm-test"
                  ? "正在测试 LLM 连接…"
                  : "正在保存 LLM 配置…"
              }}
            </span>
          </template>
        </el-alert>
        <el-alert
          v-if="llmTest"
          :type="llmTest.ok ? 'success' : 'error'"
          :closable="false"
          :title="llmTest.detail"
          style="margin-top: 16px"
        />
        <el-alert
          v-if="aiHealth?.chat && !aiHealth.chat.ok"
          type="warning"
          :closable="false"
          title="AI 功能不可用，核心功能不受影响"
          style="margin-top: 10px"
        />
      </el-form>
    </section>
    <section class="settings-section">
      <h2>AI 配置 · Embedding 连接</h2>
      <el-form label-position="top">
        <el-form-item label="Ollama 地址"
          ><el-input v-model="embedding.baseURL"
        /></el-form-item>
        <el-form-item label="模型"
          ><el-input v-model="embedding.model" />
          <div class="list-meta">
            建议中文效果较好的模型，如 bge-m3、qwen3-embedding；nomic-embed-text
            中文一般。
          </div></el-form-item
        >
        <div class="row-actions">
          <el-button :loading="busy === 'embedding-test'" @click="testEmbedding"
            >测试连接</el-button
          >
          <el-button
            type="primary"
            :loading="busy === 'embedding-save'"
            @click="saveEmbedding"
            >保存</el-button
          >
        </div>
        <el-alert
          v-if="busy === 'embedding-test' || busy === 'embedding-save'"
          type="info"
          :closable="false"
          style="margin-top: 16px"
        >
          <template #title>
            <span class="ai-testing-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              {{
                busy === "embedding-test"
                  ? "正在测试 Ollama 模型与向量维度…"
                  : "正在保存 Embedding 配置…"
              }}
            </span>
          </template>
        </el-alert>
        <el-alert
          v-if="embeddingTest"
          :type="embeddingTest.ok ? 'success' : 'error'"
          :closable="false"
          :title="`${embeddingTest.detail}${embeddingTest.dim ? ` · 维度 ${embeddingTest.dim}` : ''}`"
          style="margin-top: 16px"
        />
        <el-alert
          v-if="aiHealth?.embedding && !aiHealth.embedding.ok"
          type="warning"
          :closable="false"
          title="AI 功能不可用，核心功能不受影响"
          style="margin-top: 10px"
        />
      </el-form>
    </section>
    <section class="settings-section">
      <h2>知识库</h2>
      <el-form label-position="left" label-width="150px">
        <el-form-item label="向量 Collection">
          <el-input v-model="knowledgeStorage.collectionName" />
        </el-form-item>
        <div class="row-actions">
          <el-button :loading="busy === 'knowledge-storage-save'" @click="saveKnowledgeStorage">保存存储设置</el-button>
          <el-button type="primary" :loading="busy === 'knowledge-storage-migrate'" @click="migrateKnowledgeVectors">执行存量迁移</el-button>
        </div>
        <el-divider />
        <el-form-item label="单文件上限（MB）">
          <el-input-number
            v-model="knowledgeImport.maxFileSizeMb"
            :min="1"
            :max="500"
          />
        </el-form-item>
        <el-form-item label="解析超时（秒）">
          <el-input-number
            v-model="knowledgeImport.parseTimeoutSeconds"
            :min="10"
            :max="600"
          />
        </el-form-item>
        <el-button type="primary" @click="saveKnowledgeImport">
          保存知识库设置
        </el-button>
      </el-form>
      <el-divider />
      <h3 class="section-title">扫描件 OCR</h3>
      <el-form label-position="left" label-width="150px">
        <el-form-item label="识别服务">
          <el-select v-model="ocr.provider">
            <el-option label="关闭 OCR" value="disabled" />
            <el-option label="百度 OCR" value="baidu" />
            <el-option label="腾讯云 OCR" value="tencent" />
            <el-option label="阿里云 OCR" value="aliyun" />
            <el-option label="本地 Tesseract" value="tesseract" />
          </el-select>
        </el-form-item>
        <template v-if="ocr.provider === 'baidu'">
          <el-form-item label="API Key"><el-input v-model="ocr.baidu.apiKey" /></el-form-item>
          <el-form-item label="Secret Key"><el-input v-model="ocr.baidu.secretKey" type="password" show-password /></el-form-item>
        </template>
        <template v-else-if="ocr.provider === 'tencent'">
          <el-form-item label="Secret ID"><el-input v-model="ocr.tencent.secretId" /></el-form-item>
          <el-form-item label="Secret Key"><el-input v-model="ocr.tencent.secretKey" type="password" show-password /></el-form-item>
          <el-form-item label="区域"><el-input v-model="ocr.tencent.region" /></el-form-item>
        </template>
        <template v-else-if="ocr.provider === 'aliyun'">
          <el-form-item label="Access Key ID"><el-input v-model="ocr.aliyun.accessKeyId" /></el-form-item>
          <el-form-item label="Access Key Secret"><el-input v-model="ocr.aliyun.accessKeySecret" type="password" show-password /></el-form-item>
          <el-form-item label="Endpoint"><el-input v-model="ocr.aliyun.endpoint" /></el-form-item>
        </template>
        <div class="row-actions">
          <el-button @click="testOcr">测试配置</el-button>
          <el-button type="primary" @click="saveOcr">保存 OCR 设置</el-button>
        </div>
        <el-alert v-if="ocrTest" :type="ocrTest.ok ? 'success' : 'error'" :closable="false" :title="ocrTest.detail" style="margin-top: 12px" />
      </el-form>
    </section>
    <section class="settings-section">
      <h2>站内提醒与降噪</h2>
      <el-form label-position="left" label-width="150px"
        ><el-form-item label="关键提醒"
          ><el-switch v-model="reminders.criticalEnabled" /></el-form-item
        ><el-form-item label="周期报告"
          ><el-switch v-model="reminders.periodicEnabled" /></el-form-item
        ><el-form-item label="主动推荐"
          ><el-switch v-model="reminders.recommendationEnabled" /><span
            class="list-meta"
            style="margin-left: 10px"
            >默认关闭</span
          ></el-form-item
        ><el-form-item label="聚合窗口（分钟）"
          ><el-input-number
            v-model="reminders.aggregationMinutes"
            :min="0"
            :max="1440" /></el-form-item
        ><el-form-item label="已读保留（天）"
          ><el-input-number
            v-model="reminders.readRetentionDays"
            :min="1"
            :max="365" /></el-form-item
        ><el-button type="primary" @click="saveReminders"
          >保存提醒设置</el-button
        ></el-form
      >
    </section>
    <section class="settings-section">
      <h2>备份、恢复与导出</h2>
      <div class="row-actions" style="flex-wrap: wrap">
        <el-button :icon="Download" :loading="busy === 'backup'" @click="backup"
          >一键备份</el-button
        ><el-button
          :icon="Upload"
          :loading="busy === 'restore'"
          @click="restore"
          >一键恢复</el-button
        ><el-button
          :icon="Document"
          :loading="busy === 'json'"
          @click="exportData('json')"
          >导出 JSON</el-button
        ><el-button
          :icon="Document"
          :loading="busy === 'markdown'"
          @click="exportData('markdown')"
          >导出 Markdown</el-button
        ><el-button
          :icon="Document"
          :loading="busy === 'txt'"
          @click="exportData('txt')"
          >导出 TXT</el-button
        >
      </div>
      <el-divider />
      <h3 class="section-title">系统任务状态</h3>
      <div v-if="tasks.length" class="list">
        <div v-for="task in tasks" :key="task.id" class="list-row">
          <div class="list-main">
            <div class="list-title">{{ task.type }} · {{ task.stage }}</div>
            <div class="list-meta">
              {{ new Date(task.startedAt).toLocaleString()
              }}<template v-if="task.error"> · {{ task.error }}</template>
            </div>
          </div>
          <el-progress
            type="circle"
            :width="42"
            :percentage="task.progress"
            :status="
              task.status === 'failed'
                ? 'exception'
                : task.status === 'success'
                  ? 'success'
                  : undefined
            "
          />
        </div>
      </div>
      <div v-else class="empty-inline">暂无系统任务。</div>
    </section>
  </div>
</template>
