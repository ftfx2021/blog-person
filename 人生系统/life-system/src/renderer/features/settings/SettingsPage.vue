<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import {
  Connection,
  Download,
  Upload,
  Document,
} from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useApi } from "../../shared/api";
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
const health = ref<any>(),
  milvus = ref<any>(),
  tasks = ref<any[]>([]),
  busy = ref("");
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function load() {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const saved: any = await call(() => window.lifeSystem.settings.getMysql());
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (saved) Object.assign(mysql, saved, { password: "" });
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  milvus.value = await call(() => window.lifeSystem.settings.milvusStatus());
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    Object.assign(
      reminders,
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await call(() => window.lifeSystem.settings.getReminders()),
    );
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    tasks.value = await call(() => window.lifeSystem.backup.tasks());
  } catch (caught) {
    console.warn("数据库未连接，系统任务将在连接后加载", caught);
  }
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function test() {
  busy.value = "test";
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    health.value = await call(() =>
      // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
      window.lifeSystem.settings.testMysql(mysql),
    );
  } finally {
    busy.value = "";
  }
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function save() {
  busy.value = "save";
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await call(() => window.lifeSystem.settings.saveMysql(mysql));
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    health.value = await call(() => window.lifeSystem.settings.health());
  } finally {
    busy.value = "";
  }
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function saveReminders() {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() => window.lifeSystem.settings.saveReminders(reminders));
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function backup() {
  busy.value = "backup";
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await call(() => window.lifeSystem.backup.create());
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    tasks.value = await call(() => window.lifeSystem.backup.tasks());
  } finally {
    busy.value = "";
  }
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function restore() {
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
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const result: any = await call(() =>
      // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
      window.lifeSystem.backup.restore({
        // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
        manifestPath: "SELECT",
        confirmation: action === "confirm" ? "恢复" : "",
      }),
    );
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
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
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function exportData(format: "json" | "markdown" | "txt") {
  busy.value = format;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await call(() => window.lifeSystem.backup.export({ format }));
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    tasks.value = await call(() => window.lifeSystem.backup.tasks());
  } finally {
    busy.value = "";
  }
}
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
      <h2>Milvus</h2>
      <el-alert
        type="info"
        :closable="false"
        :title="milvus?.message || '未启用（P1）'"
        description="P0 使用 MySQL 全文检索；此数据源不会影响目标和行动功能。"
      />
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
