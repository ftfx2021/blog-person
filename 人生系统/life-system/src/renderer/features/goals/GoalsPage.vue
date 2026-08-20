<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { Plus, ArrowRight } from "@element-plus/icons-vue";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { useRouter } from "vue-router";
import PageState from "../../shared/PageState.vue";
import { useApi, toUtc } from "../../shared/api";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
const router = useRouter();
const { call } = useApi();
// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const rows = ref<any[]>([]);
const loading = ref(true);
const error = ref("");
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
const dialog = ref(false);
const saving = ref(false);
const form = reactive<any>({
  title: "",
  description: "",
  period: "quarterly",
  metricType: "numeric",
  unit: "",
  startValue: 0,
  targetValue: 100,
  dueDate: "",
  tags: [],
});
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function load() {
  loading.value = true;
  error.value = "";
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    rows.value = await call(() => window.lifeSystem.goals.list({}));
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
function open() {
  Object.assign(form, {
    title: "",
    description: "",
    period: "quarterly",
    metricType: "numeric",
    unit: "",
    startValue: 0,
    targetValue: 100,
    dueDate: "",
    tags: [],
  });
  // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
  dialog.value = true;
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function save() {
  saving.value = true;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const result: any = await call(() =>
      // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
      window.lifeSystem.goals.create({
        ...form,
        unit: form.unit || null,
        startValue:
          form.metricType === "numeric" ? Number(form.startValue) : null,
        targetValue:
          form.metricType === "numeric" ? Number(form.targetValue) : null,
        dueDate: toUtc(form.dueDate),
      }),
    );
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    dialog.value = false;
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    await router.push(`/goals/${result.id}`);
  } finally {
    saving.value = false;
  }
}
const periodLabel: any = { annual: "年度", quarterly: "季度", monthly: "月度" };
const metricLabel: any = {
  numeric: "数值型",
  milestone: "里程碑型",
  status: "状态型",
};
const statusLabel: any = {
  active: "进行中",
  done: "已完成",
  abandoned: "已放弃",
};
onMounted(load);
</script>
<template>
  <div class="page-head">
    <div>
      <h1>目标</h1>
      <p>进度只由真实数据或里程碑完成情况计算。</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="open">新建目标</el-button>
  </div>
  <PageState
    :loading="loading"
    :error="error"
    :empty="rows.length === 0"
    empty-text="还没有目标，先记录一件真正想达成的事"
    @retry="load"
    ><div class="list">
      <div v-for="goal in rows" :key="goal.id" class="list-row">
        <div class="list-main">
          <div class="list-title">{{ goal.title }}</div>
          <div class="tag-line">
            <el-tag size="small" effect="plain">{{
              periodLabel[goal.period]
            }}</el-tag
            ><el-tag size="small" type="info" effect="plain">{{
              metricLabel[goal.metricType]
            }}</el-tag
            ><el-tag
              size="small"
              :type="goal.status === 'active' ? 'success' : 'info'"
              >{{ statusLabel[goal.status] }}</el-tag
            >
          </div>
        </div>
        <div class="progress-cell">
          <el-progress
            v-if="goal.progress != null"
            :percentage="goal.progress"
            :stroke-width="7"
          /><span v-else class="list-meta">{{
            goal.metricType === "status" ? "不设百分比" : "尚未记录"
          }}</span>
        </div>
        <el-button
          :icon="ArrowRight"
          circle
          title="查看详情"
          @click="router.push(`/goals/${goal.id}`)"
        />
      </div></div
  ></PageState>
  <el-dialog v-model="dialog" title="新建目标" width="560px"
    ><el-form label-position="top" class="dialog-form"
      ><el-form-item label="标题"
        ><el-input v-model="form.title" maxlength="50" show-word-limit
      /></el-form-item>
      <div class="inline-fields">
        <el-form-item label="周期"
          ><el-select v-model="form.period"
            ><el-option label="年度" value="annual" /><el-option
              label="季度"
              value="quarterly" /><el-option
              label="月度"
              value="monthly" /></el-select></el-form-item
        ><el-form-item label="度量方式"
          ><el-select v-model="form.metricType"
            ><el-option label="数值型" value="numeric" /><el-option
              label="里程碑型"
              value="milestone" /><el-option
              label="状态型"
              value="status" /></el-select
        ></el-form-item>
      </div>
      <template v-if="form.metricType === 'numeric'"
        ><el-form-item label="单位"
          ><el-input v-model="form.unit"
        /></el-form-item>
        <div class="inline-fields">
          <el-form-item label="起点值"
            ><el-input-number
              v-model="form.startValue"
              controls-position="right" /></el-form-item
          ><el-form-item label="目标值"
            ><el-input-number
              v-model="form.targetValue"
              controls-position="right"
          /></el-form-item></div></template
      ><el-form-item label="截止时间"
        ><el-date-picker
          v-model="form.dueDate"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm"
          placeholder="可选" /></el-form-item
      ><el-form-item label="说明"
        ><el-input
          v-model="form.description"
          type="textarea"
          :rows="3" /></el-form-item
      ><el-form-item label="标签"
        ><el-select
          v-model="form.tags"
          multiple
          filterable
          allow-create
          default-first-option /></el-form-item></el-form
    ><template #footer
      ><el-button @click="dialog = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save"
        >创建</el-button
      ></template
    ></el-dialog
  >
</template>
