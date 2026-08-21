<script setup lang="ts">
// 页面只负责表单状态和筛选参数，写入校验仍交给主进程和共享 schema。
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Edit, Plus, RefreshLeft, Right } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { toLocalInput, toUtc, useApi } from "../../shared/api";

const { call } = useApi();

const rows = ref<any[]>([]);
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

const form = reactive({
  title: "",
  note: "",
  dueDate: "",
  goalId: null as string | null,
  projectId: null as string | null,
});

const formRules = {
  // 标题必须先在前端拦住空白字符串，避免空提交来回占用一次 IPC 往返。
  title: [
    {
      trigger: "blur",
      validator: (_rule: any, value: string, callback: (error?: Error) => void) =>
        value && value.trim().length > 0
          ? callback()
          : callback(new Error("请输入标题")),
    },
  ],
  // 备注长度与服务端 optionalText 保持一致，防止超长内容到主进程才失败。
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

const labels: Record<string, string> = {
  todo: "待处理",
  doing: "进行中",
  done: "已完成",
};

function resetForm() {
  Object.assign(form, {
    title: "",
    note: "",
    dueDate: "",
    goalId: null,
    projectId: null,
  });
}

function closeDialog() {
  dialogVisible.value = false;
  editingTaskId.value = null;
  resetForm();
}

function openCreate() {
  editingTaskId.value = null;
  resetForm();
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

function openEdit(item: any) {
  editingTaskId.value = item.id;
  Object.assign(form, {
    title: item.title || "",
    note: item.note || "",
    dueDate: item.dueDate ? toLocalInput(item.dueDate) : "",
    goalId: item.goalId ?? null,
    projectId: item.projectId ?? null,
  });
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

function buildListFilter() {
  const filter: Record<string, string> = { sort: "due_asc" };
  if (statusFilter.value) filter.status = statusFilter.value;
  if (goalFilter.value) filter.goalId = goalFilter.value;
  if (projectFilter.value) filter.projectId = projectFilter.value;
  if (dateRange.value?.[0] && dateRange.value?.[1]) {
    // 日期范围按本地日历日取 UTC 边界，和服务端的 datetime 过滤保持同一口径。
    filter.dateFrom = toUtcDayStart(dateRange.value[0]);
    filter.dateTo = toUtcDayEnd(dateRange.value[1]);
  }
  return filter;
}

function toUtcDayStart(value: string) {
  return new Date(`${value}T00:00:00`).toISOString();
}

function toUtcDayEnd(value: string) {
  return new Date(`${value}T23:59:59.999`).toISOString();
}

function isOverdue(item: any) {
  return (
    item.status !== "done" &&
    Boolean(item.dueDate) &&
    new Date(item.dueDate).getTime() < Date.now()
  );
}

function formatDueDate(item: any) {
  return item.dueDate ? new Date(item.dueDate).toLocaleString() : "无截止时间";
}

function transitionLabel(item: any) {
  return item.status === "todo" ? "开始" : "完成";
}

function transitionSuccessText(item: any) {
  return item.status === "todo" ? "已开始" : "已完成";
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
    // call 已经弹出错误提示，这里只保留弹窗与输入，方便用户修正后重试。
  } finally {
    saving.value = false;
  }
}

async function transition(item: any, action: "advance" | "undo") {
  if (transitioningIds[item.id]) return;
  setTransitionBusy(item.id, true);
  try {
    await call(() => window.lifeSystem.tasks.transition({ id: item.id, action }));
    ElMessage.success(action === "undo" ? "已撤销完成" : transitionSuccessText(item));
    await loadRows();
  } catch {
    // 单行 loading 复位后继续保留当前列表快照，避免连点时并发写入。
  } finally {
    setTransitionBusy(item.id, false);
  }
}

async function remove(item: any) {
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
    // 取消之外的失败交给 call 提示，这里不关闭页面状态，方便再次重试。
  } finally {
    deletingId.value = null;
  }
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
    <div class="list">
      <div
        v-for="item in rows"
        :key="item.id"
        class="list-row task-row"
        :class="{ 'task-row--overdue': isOverdue(item) }"
      >
        <div class="list-main">
          <div class="task-title-line">
            <el-tag v-if="isOverdue(item)" type="danger" size="small"
              >已逾期</el-tag
            >
            <div
              class="list-title task-title"
              :class="{ 'task-title--done': item.status === 'done' }"
            >
              {{ item.title }}
            </div>
          </div>
          <div class="task-meta">
            <div class="list-meta">
              {{ labels[item.status] }} ·
              <span :class="{ 'task-meta--overdue': isOverdue(item) }">
                {{ formatDueDate(item) }}
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
            >{{ transitionLabel(item) }}</el-button
          >
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
            >撤销完成</el-button
          >
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
      <el-form-item label="截止时间">
        <el-date-picker
          v-model="form.dueDate"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm"
          placeholder="可选"
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
