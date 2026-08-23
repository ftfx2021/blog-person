<script setup lang="ts">
// 目标列表只维护查询和编辑状态，进度计算由共享领域规则提供。
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { Plus, ArrowRight } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import PageState from "../../shared/PageState.vue";
import { useApi, toUtc } from "../../shared/api";
const router = useRouter();
const { call } = useApi();
const rows = ref<any[]>([]);
const loading = ref(true);
const error = ref("");
const dialog = ref(false);
const saving = ref(false);
const formRef = ref<any>();
const keyword = ref("");
const status = ref("");
let searchTimer: ReturnType<typeof setTimeout> | undefined;
const createDefaults = {
  title: "",
  description: "",
  period: "quarterly",
  metricType: "numeric",
  unit: "",
  startValue: 0,
  targetValue: 100,
  dueDate: "",
  tags: [] as string[],
};
const form = reactive<any>({
  ...createDefaults,
});
const rules: any = {
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  unit: [{ max: 32, message: "单位长度不能超过 32 个字符", trigger: "blur" }],
  tags: [
    {
      type: "array",
      max: 20,
      message: "标签不能超过 20 个",
      trigger: "change",
    },
  ],
  startValue: [
    {
      trigger: "change",
      validator: (
        _rule: any,
        value: number | null,
        callback: (error?: Error) => void,
      ) => {
        if (form.metricType !== "numeric") return callback();
        if (value == null || !Number.isFinite(Number(value)))
          return callback(new Error("请输入起点值"));
        if (Number(value) === Number(form.targetValue))
          return callback(new Error("起点值不能等于目标值"));
        callback();
      },
    },
  ],
  targetValue: [
    {
      trigger: "change",
      validator: (
        _rule: any,
        value: number | null,
        callback: (error?: Error) => void,
      ) => {
        if (form.metricType !== "numeric") return callback();
        if (value == null || !Number.isFinite(Number(value)))
          return callback(new Error("请输入目标值"));
        if (Number(value) === Number(form.startValue))
          return callback(new Error("目标值不能等于起点值"));
        callback();
      },
    },
  ],
};
function changeMetricType(value: string) {
  // 切换度量类型时清掉不适用的数值，避免回切后把旧公式误提交到服务端。
  if (value === "numeric") {
    form.startValue = 0;
    form.targetValue = 100;
  } else {
    form.startValue = null;
    form.targetValue = null;
  }
  nextTick(() => formRef.value?.clearValidate(["startValue", "targetValue"]));
}
function disablePastDate(date: Date) {
  // 截止时间按本地日历日限制，今天可选，今天之前的日期不可选。
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date.getTime() < today.getTime();
}
function isCreateDirty() {
  return Object.keys(createDefaults).some((key) => {
    const value = form[key];
    const initial = createDefaults[key as keyof typeof createDefaults];
    return Array.isArray(value)
      ? JSON.stringify(value) !== JSON.stringify(initial)
      : value !== initial;
  });
}
async function confirmClose(message: string, close: () => void) {
  try {
    await ElMessageBox.confirm(message, "确认关闭", {
      type: "warning",
      confirmButtonText: "关闭",
      cancelButtonText: "继续编辑",
    });
    close();
  } catch {
    // 取消关闭时保留当前输入，不产生未处理的 Promise rejection。
  }
}
function closeCreateDialog() {
  if (!isCreateDirty()) {
    dialog.value = false;
    return;
  }
  void confirmClose("当前填写内容尚未保存，确定关闭吗？", () => {
    dialog.value = false;
  });
}
function beforeCloseCreate(done: () => void) {
  if (!isCreateDirty()) {
    done();
    return;
  }
  void confirmClose("当前填写内容尚未保存，确定关闭吗？", done);
}
async function load(filters: { status?: string; keyword?: string } = {}) {
  // 重新读取目标可得到最新进度与里程碑派生值。
  loading.value = true;
  error.value = "";
  try {
    rows.value = await call(() => window.lifeSystem.goals.list(filters));
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
function open() {
  // 每次打开创建弹窗都重置表单，避免上次取消的输入被误作为新目标提交。
  Object.assign(form, createDefaults, { tags: [] });
  dialog.value = true;
  // 清除上一次校验状态，避免新建弹窗刚打开就显示旧的错误提示。
  nextTick(() => formRef.value?.clearValidate());
}
async function save() {
  saving.value = true;
  try {
    // 把校验放进 try，防止校验实例异常时点击处理器静默结束。
    if (!formRef.value || typeof formRef.value.validate !== "function") {
      ElMessage.warning("表单正在加载，请稍后再试");
      return;
    }
    // Element Plus 校验失败会 reject 字段错误对象，这里转换为布尔值而不是当成创建异常。
    const valid = await formRef.value.validate().then(
      () => true,
      () => false,
    );
    if (!valid) {
      ElMessage.warning("请检查表单中的红色提示");
      return;
    }
    // Context bridge 不能克隆 Vue Proxy；显式生成纯 DTO 后再调用 preload API。
    const input = {
      title: form.title,
      description: form.description || null,
      period: form.period,
      metricType: form.metricType,
      unit: form.unit || null,
      startValue:
        form.metricType === "numeric" ? Number(form.startValue) : null,
      targetValue:
        form.metricType === "numeric" ? Number(form.targetValue) : null,
      dueDate: toUtc(form.dueDate),
      tags: [...form.tags],
      confirmRecalculate: false,
    };
    const result: any = await call(() => window.lifeSystem.goals.create(input));
    ElMessage.success("目标已创建");
    dialog.value = false;
    await router.push(`/goals/${result.id}`);
  } catch (caught: any) {
    // 保留输入并把异常显示出来，避免 IPC/schema 失败时只表现为按钮无反应。
    console.error("创建目标失败", caught);
    // call 对标准业务错误已经提示过；这里只补充校验、序列化等前端异常提示。
    if (!caught?.code)
      ElMessage.error(caught?.message || "目标创建失败，请重试");
  } finally {
    saving.value = false;
  }
}
function currentFilters() {
  const filters: { status?: string; keyword?: string } = {};
  if (status.value) filters.status = status.value;
  if (keyword.value.trim()) filters.keyword = keyword.value.trim();
  return filters;
}
function loadWithFilters() {
  void load(currentFilters());
}
watch(keyword, () => {
  // 搜索输入防抖 300ms，避免每个字符都触发一次 IPC 查询。
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(loadWithFilters, 300);
});
watch(status, loadWithFilters);
const hasFilters = computed(() =>
  Boolean(keyword.value.trim() || status.value),
);
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
// 目标列表挂载后读取一次，展示服务端计算的最新进度。
onMounted(load);
onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer);
});
</script>
<template>
  <div class="page-head">
    <div>
      <h1>目标</h1>
      <p>进度只由真实数据或里程碑完成情况计算。</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="open">新建目标</el-button>
  </div>
  <div
    class="list-toolbar"
    style="display: flex; gap: 10px; margin-bottom: 20px"
  >
    <el-input
      v-model="keyword"
      clearable
      placeholder="搜索目标标题"
      style="max-width: 320px"
    />
    <el-select
      v-model="status"
      clearable
      placeholder="全部状态"
      style="width: 150px"
    >
      <el-option label="进行中" value="active" />
      <el-option label="已完成" value="done" />
      <el-option label="已放弃" value="abandoned" />
    </el-select>
  </div>
  <PageState
    :loading="loading"
    :error="error"
    :empty="rows.length === 0"
    :empty-text="
      hasFilters ? '筛选无结果' : '还没有目标，先记录一件真正想达成的事'
    "
    @retry="loadWithFilters"
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
            <el-tag
              v-for="tag in goal.tags || []"
              :key="tag"
              size="small"
              effect="plain"
              >{{ tag }}</el-tag
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
  <el-dialog
    v-model="dialog"
    title="新建目标"
    width="560px"
    :close-on-click-modal="false"
    :before-close="beforeCloseCreate"
    ><el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="dialog-form"
      ><el-form-item label="标题" prop="title"
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
          ><el-select v-model="form.metricType" @change="changeMetricType"
            ><el-option label="数值型" value="numeric" /><el-option
              label="里程碑型"
              value="milestone" /><el-option
              label="状态型"
              value="status" /></el-select
        ></el-form-item>
      </div>
      <template v-if="form.metricType === 'numeric'"
        ><el-form-item label="单位" prop="unit"
          ><el-input v-model="form.unit" maxlength="32" show-word-limit
        /></el-form-item>
        <div class="inline-fields">
          <el-form-item label="起点值" prop="startValue"
            ><el-input-number
              v-model="form.startValue"
              controls-position="right" /></el-form-item
          ><el-form-item label="目标值" prop="targetValue"
            ><el-input-number
              v-model="form.targetValue"
              controls-position="right"
          /></el-form-item></div></template
      ><el-form-item label="截止时间"
        ><el-date-picker
          v-model="form.dueDate"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm"
          :disabled-date="disablePastDate"
          placeholder="可选" /></el-form-item
      ><el-form-item label="说明"
        ><el-input
          v-model="form.description"
          type="textarea"
          :rows="3" /></el-form-item
      ><el-form-item label="标签" prop="tags"
        ><el-select
          v-model="form.tags"
          multiple
          filterable
          allow-create
          default-first-option /></el-form-item></el-form
    ><template #footer
      ><el-button @click="closeCreateDialog">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save"
        >创建</el-button
      ></template
    ></el-dialog
  >
</template>
