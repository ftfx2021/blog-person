<script setup lang="ts">
// 页面只负责表单状态、分组视图和筛选参数，period 与截止时间仍由共享契约校验。
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Edit, Plus, RefreshLeft, Right } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { toUtc, useApi } from "../../shared/api";

const periodOrder = ["day", "week", "month", "semester", "other"] as const;
type TaskPeriod = (typeof periodOrder)[number];
type TaskRow = {
  id: string;
  title: string;
  note?: string | null;
  dueDate?: string | null;
  goalId?: string | null;
  projectId?: string | null;
  status: "todo" | "doing" | "done";
  period?: TaskPeriod | string | null;
  goalTitle?: string | null;
  projectTitle?: string | null;
};
type TaskGroup = {
  period: TaskPeriod;
  title: string;
  count: number;
  items: TaskRow[];
};

const periodSelectLabels: Record<TaskPeriod, string> = {
  day: "日",
  week: "周",
  month: "月",
  semester: "学期",
  other: "其它",
};
const periodGroupLabels: Record<TaskPeriod, string> = {
  day: "今日",
  week: "本周",
  month: "本月",
  semester: "本学期",
  other: "其它",
};

const { call } = useApi();

const rows = ref<TaskRow[]>([]);
const goals = ref<any[]>([]);
const projects = ref<any[]>([]);
const loading = ref(true);
const error = ref("");
const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<any>();
const statusFilter = ref("");
const goalFilter = ref<string | null>("");
const projectFilter = ref<string | null>("");
const dateRange = ref<[string, string] | null>(null);
const editingTaskId = ref<string | null>(null);
const deletingId = ref<string | null>(null);
const transitioningIds = reactive<Record<string, boolean>>({});
const dueDateTouched = ref(false);
const dueDateSyncLock = ref(false);

const form = reactive({
  title: "",
  note: "",
  dueDate: "",
  period: "other" as TaskPeriod,
  goalId: null as string | null,
  projectId: null as string | null,
});

const formRules = {
  // 标题先在前端拦住空白字符串，避免无意义请求打到主进程后才失败。
  title: [
    {
      trigger: "blur",
      validator: (_rule: any, value: string, callback: (error?: Error) => void) =>
        value && value.trim().length > 0
          ? callback()
          : callback(new Error("请输入标题")),
    },
  ],
  // 备注长度与共享合同保持一致，避免提交时才发现文本超长。
  note: [{ max: 2000, message: "备注不能超过 2000 个字符", trigger: "blur" }],
};

const hasFilters = computed(
  () =>
    Boolean(
      statusFilter.value ||
        goalFilter.value ||
        projectFilter.value ||
        (dateRange.value && dateRange.value.length === 2),
    ),
);
const emptyText = computed(() =>
  hasFilters.value ? "当前筛选下没有待办" : "还没有待办",
);
const groupedRows = computed<TaskGroup[]>(() =>
  periodOrder
    .map((period) => {
      const items = rows.value.filter((item) => normalizePeriod(item.period) === period);
      return items.length
        ? {
            period,
            title: periodGroupLabels[period],
            count: items.length,
            items,
          }
        : null;
    })
    .filter(Boolean) as TaskGroup[],
);

const labels: Record<string, string> = {
  todo: "待处理",
  doing: "进行中",
  done: "已完成",
};

const periodOptions = periodOrder.map((value) => ({
  label: periodSelectLabels[value],
  value,
}));

function normalizePeriod(value: unknown): TaskPeriod {
  return periodOrder.includes(value as TaskPeriod) ? (value as TaskPeriod) : "other";
}

function resetForm() {
  Object.assign(form, {
    title: "",
    note: "",
    dueDate: "",
    period: "other",
    goalId: null,
    projectId: null,
  });
  dueDateTouched.value = false;
  dueDateSyncLock.value = false;
}

function closeDialog() {
  dialogVisible.value = false;
  editingTaskId.value = null;
  resetForm();
}

function formatLocalInput(value: string | null | undefined): string {
  if (!value) return "";
  const date = new Date(value);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hour}:${minute}`;
}

function formatDueDate(value: string | null | undefined): string {
  return value ? new Date(value).toLocaleString() : "无截止时间";
}

function endOfLocalDay(base = new Date()) {
  const date = new Date(base);
  date.setHours(23, 59, 0, 0);
  return date;
}

function formatInputFromDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hour}:${minute}`;
}

function suggestDueDate(period: TaskPeriod): string {
  const today = new Date();
  if (period === "day") return formatInputFromDate(endOfLocalDay(today));
  if (period === "week") {
    const date = endOfLocalDay(today);
    const offset = (7 - date.getDay()) % 7;
    date.setDate(date.getDate() + offset);
    return formatInputFromDate(date);
  }
  if (period === "month") {
    const date = new Date(today.getFullYear(), today.getMonth() + 1, 0, 23, 59, 0, 0);
    return formatInputFromDate(date);
  }
  return "";
}

function maybeApplyPeriodSuggestion(period: TaskPeriod) {
  const suggestion = suggestDueDate(period);
  if (!suggestion) return;
  if (dueDateTouched.value && form.dueDate) return;
  // 选中短周期时给一个本地截止时间建议，既省输入也不强制绑定到 period。
  dueDateSyncLock.value = true;
  form.dueDate = suggestion;
  dueDateTouched.value = false;
  void nextTick(() => {
    dueDateSyncLock.value = false;
  });
}

function openCreate() {
  editingTaskId.value = null;
  resetForm();
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

function openEdit(item: TaskRow) {
  editingTaskId.value = item.id;
  Object.assign(form, {
    title: item.title || "",
    note: item.note || "",
    dueDate: formatLocalInput(item.dueDate),
    period: normalizePeriod(item.period),
    goalId: item.goalId ?? null,
    projectId: item.projectId ?? null,
  });
  dueDateTouched.value = false;
  dueDateSyncLock.value = false;
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

function buildListFilter() {
  const filter: Record<string, string> = { sort: "due_asc" };
  if (statusFilter.value) filter.status = statusFilter.value;
  if (goalFilter.value) filter.goalId = goalFilter.value;
  if (projectFilter.value) filter.projectId = projectFilter.value;
  if (dateRange.value?.[0] && dateRange.value?.[1]) {
    // 日期范围按本地日历日转 UTC 边界，和服务端的 DATETIME 口径保持一致。
    const dateFrom = toUtc(`${dateRange.value[0]}T00:00:00`);
    const dateTo = toUtc(`${dateRange.value[1]}T23:59:59.999`);
    if (dateFrom) filter.dateFrom = dateFrom;
    if (dateTo) filter.dateTo = dateTo;
  }
  return filter;
}

function isOverdue(item: TaskRow) {
  return (
    item.status !== "done" &&
    Boolean(item.dueDate) &&
    new Date(item.dueDate as string).getTime() < Date.now()
  );
}

function setTransitionBusy(id: string, busy: boolean) {
  if (busy) transitioningIds[id] = true;
  else delete transitioningIds[id];
}

async function loadLookups() {
  const [goalResult, projectResult] = await Promise.allSettled([
    call(() => window.lifeSystem.goals.list({}), { silent: true }),
    call(() => window.lifeSystem.projects.list({}), { silent: true }),
  ]);
  if (goalResult.status === "fulfilled") goals.value = goalResult.value;
  if (projectResult.status === "fulfilled") projects.value = projectResult.value;
}

async function loadRows() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await call(() => window.lifeSystem.tasks.list(buildListFilter()));
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

async function save() {
  const valid = formRef.value ? await formRef.value.validate().catch(() => false) : false;
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      title: form.title.trim(),
      note: form.note || null,
      dueDate: toUtc(form.dueDate),
      period: form.period,
      goalId: form.goalId ?? null,
      projectId: form.projectId ?? null,
    };
    if (editingTaskId.value) {
      await call(() =>
        window.lifeSystem.tasks.update({
          id: editingTaskId.value,
          ...payload,
        }),
      );
      ElMessage.success("已保存");
    } else {
      await call(() => window.lifeSystem.tasks.create(payload));
      ElMessage.success("待办已创建");
    }
    closeDialog();
    await loadRows();
  } catch {
    // call 已经弹出错误提示，这里保留弹窗和输入，方便用户修正后重试。
  } finally {
    saving.value = false;
  }
}

async function transition(item: TaskRow, action: "advance" | "undo") {
  if (transitioningIds[item.id]) return;
  setTransitionBusy(item.id, true);
  try {
    await call(() => window.lifeSystem.tasks.transition({ id: item.id, action }));
    ElMessage.success(action === "undo" ? "已撤销完成" : item.status === "todo" ? "已开始" : "已完成");
    await loadRows();
  } catch {
    // 单行 loading 复位后继续保留当前列表快照，避免连点时并发写入。
  } finally {
    setTransitionBusy(item.id, false);
  }
}

async function remove(item: TaskRow) {
  try {
    await ElMessageBox.confirm("待办删除后不可恢复。", "确认删除", {
      type: "warning",
    });
  } catch {
    return;
  }
  deletingId.value = item.id;
  try {
    await call(() => window.lifeSystem.tasks.remove(item.id));
    ElMessage.success("已删除");
    await loadRows();
  } catch {
    // 删除失败交给 call 统一提示，这里只保持页面状态不丢。
  } finally {
    deletingId.value = null;
  }
}

function onPeriodChange(value: TaskPeriod) {
  maybeApplyPeriodSuggestion(value);
}

function onDueDateChange(value: string | null | undefined) {
  if (dueDateSyncLock.value) return;
  dueDateTouched.value = Boolean(value);
}

onMounted(() => {
  void loadLookups();
  void loadRows();
});
</script>

<template>
  <div class="page-head">
    <div>
      <h1>待办</h1>
      <p>按截止时间排序，不设置重要或紧急等级。</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="openCreate">新建待办</el-button>
  </div>

  <div class="toolbar task-toolbar">
    <el-segmented
      v-model="statusFilter"
      :options="[
        { label: '全部', value: '' },
        { label: '待处理', value: 'todo' },
        { label: '进行中', value: 'doing' },
        { label: '已完成', value: 'done' },
      ]"
      @change="loadRows"
    />
    <div class="task-filters">
      <el-select
        v-model="goalFilter"
        clearable
        filterable
        placeholder="按目标"
        class="task-filter"
        @change="loadRows"
      >
        <el-option
          v-for="item in goals"
          :key="item.id"
          :label="item.title"
          :value="item.id"
        />
      </el-select>
      <el-select
        v-model="projectFilter"
        clearable
        filterable
        placeholder="按项目"
        class="task-filter"
        @change="loadRows"
      >
        <el-option
          v-for="item in projects"
          :key="item.id"
          :label="item.title"
          :value="item.id"
        />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        clearable
        class="task-range"
        @change="loadRows"
        @clear="loadRows"
      />
    </div>
  </div>

  <PageState
    :loading="loading"
    :error="error"
    :empty="!loading && rows.length === 0"
    :empty-text="emptyText"
    @retry="loadRows"
  >
    <div v-if="groupedRows.length" class="task-groups">
      <section v-for="group in groupedRows" :key="group.period" class="task-group">
        <header class="task-group-head">
          <div>
            <h2>{{ group.title }}</h2>
          </div>
          <el-tag size="small" effect="plain">{{ group.count }}</el-tag>
        </header>
        <div class="list task-group-list">
          <div
            v-for="item in group.items"
            :key="item.id"
            class="list-row task-row"
            :class="{ 'task-row--overdue': isOverdue(item) }"
          >
            <div class="list-main">
              <div class="task-title-line">
                <el-tag v-if="isOverdue(item)" type="danger" size="small">已逾期</el-tag>
                <div class="list-title task-title" :class="{ 'task-title--done': item.status === 'done' }">
                  {{ item.title }}
                </div>
              </div>
              <div class="task-meta">
                <div class="list-meta">
                  {{ labels[item.status] }} ·
                  <span :class="{ 'task-meta--overdue': isOverdue(item) }">
                    {{ formatDueDate(item.dueDate) }}
                  </span>
                </div>
                <div class="list-meta">
                  <span>目标：{{ item.goalTitle || "未关联" }}</span>
                  <span>·</span>
                  <span>项目：{{ item.projectTitle || "未关联" }}</span>
                </div>
              </div>
            </div>
            <div class="row-actions">
              <el-button
                v-if="item.status !== 'done'"
                size="small"
                :icon="Right"
                :loading="Boolean(transitioningIds[item.id])"
                :disabled="Boolean(transitioningIds[item.id])"
                @click="transition(item, 'advance')"
              >
                {{ item.status === "todo" ? "开始" : "完成" }}
              </el-button>
              <el-button
                v-if="item.status !== 'done'"
                text
                :icon="Edit"
                title="编辑"
                :disabled="Boolean(transitioningIds[item.id])"
                @click="openEdit(item)"
              />
              <el-button
                v-else
                size="small"
                :icon="RefreshLeft"
                :loading="Boolean(transitioningIds[item.id])"
                :disabled="Boolean(transitioningIds[item.id])"
                @click="transition(item, 'undo')"
              >
                撤销完成
              </el-button>
              <el-button
                text
                type="danger"
                :icon="Delete"
                title="删除"
                :loading="deletingId === item.id"
                :disabled="Boolean(transitioningIds[item.id]) || deletingId === item.id"
                @click="remove(item)"
              />
            </div>
          </div>
        </div>
      </section>
    </div>
  </PageState>

  <el-dialog
    v-model="dialogVisible"
    :title="editingTaskId ? '编辑待办' : '新建待办'"
    width="560px"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      label-position="top"
      class="task-form"
    >
      <el-form-item label="标题" prop="title" required>
        <el-input
          v-model="form.title"
          maxlength="100"
          show-word-limit
          placeholder="请输入标题"
        />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-select
          v-model="form.period"
          placeholder="选择范围"
          class="task-full"
          @change="onPeriodChange"
        >
          <el-option
            v-for="item in periodOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="截止时间">
        <el-date-picker
          v-model="form.dueDate"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm"
          placeholder="可选"
          class="task-full"
          @change="onDueDateChange"
        />
      </el-form-item>
      <el-form-item label="备注" prop="note">
        <el-input
          v-model="form.note"
          type="textarea"
          :rows="4"
          maxlength="2000"
          show-word-limit
          placeholder="可选"
        />
      </el-form-item>
      <div class="task-inline-fields">
        <el-form-item label="关联目标">
          <el-select
            v-model="form.goalId"
            clearable
            filterable
            placeholder="选择目标"
          >
            <el-option
              v-for="item in goals"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联项目">
          <el-select
            v-model="form.projectId"
            clearable
            filterable
            placeholder="选择项目"
          >
            <el-option
              v-for="item in projects"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">
        {{ editingTaskId ? "保存" : "创建" }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.task-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.task-filters {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.task-filter {
  min-width: 180px;
}

.task-range {
  min-width: 260px;
}

.task-groups {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.task-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 2px;
}

.task-group-head h2 {
  margin: 0;
  font-size: 16px;
  line-height: 1.2;
}

.task-group-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-row {
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.task-row--overdue {
  background: rgba(245, 108, 108, 0.06);
  border-color: rgba(245, 108, 108, 0.28);
}

.task-title-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.task-title {
  flex: 1;
  min-width: 0;
}

.task-title--done {
  text-decoration: line-through;
  color: #8a9591;
}

.task-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 4px;
}

.task-meta--overdue {
  color: #f56c6c;
  font-weight: 600;
}

.task-inline-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.task-inline-fields :deep(.el-select),
.task-inline-fields :deep(.el-date-editor) {
  width: 100%;
}

.task-full {
  width: 100%;
}

@media (max-width: 720px) {
  .task-inline-fields {
    grid-template-columns: 1fr;
  }

  .task-filter,
  .task-range {
    min-width: 100%;
  }
}
</style>
