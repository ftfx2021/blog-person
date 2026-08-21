<script setup lang="ts">
// 项目页集中处理列表、筛选和编辑弹窗，避免模板直接操作 IPC。
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Edit, Plus } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { toUtc, useApi } from "../../shared/api";

const { call } = useApi();
const rows = ref<any[]>([]);
const goals = ref<any[]>([]);
const loading = ref(true);
const error = ref("");
const dialog = ref(false);
const saving = ref(false);
const editingId = ref<string | null>(null);
const editingStatus = ref("");
const formRef = ref<any>();
const statusFilter = ref("");
const goalFilter = ref("");
const transitioningIds = reactive(new Set<string>());

const form = reactive<any>({
  title: "",
  description: "",
  goalId: null,
  startAt: "",
  endAt: "",
  tags: [],
});

const isEditing = computed(() => editingId.value !== null);
const isDone = computed(() => editingStatus.value === "done");
const hasFilter = computed(() => Boolean(statusFilter.value || goalFilter.value));
const dialogTitle = computed(() =>
  isEditing.value ? "编辑生活项目" : "新建生活项目",
);

const rules: any = {
  title: [
    { required: true, message: "请输入标题", trigger: "blur" },
    { max: 50, message: "标题不能超过 50 个字符", trigger: "blur" },
  ],
  endAt: [
    {
      trigger: "change",
      validator: (
        _rule: any,
        value: string,
        callback: (error?: Error) => void,
      ) => {
        // 只有两个时间都填写时比较，保持与服务端 refine 的可选字段语义一致。
        if (value && form.startAt && value < form.startAt)
          return callback(new Error("结束时间不能早于开始时间"));
        callback();
      },
    },
  ],
  tags: [
    {
      type: "array",
      max: 20,
      message: "标签不能超过 20 个",
      trigger: "change",
    },
  ],
};

function resetForm() {
  form.title = "";
  form.description = "";
  form.goalId = null;
  form.startAt = "";
  form.endAt = "";
  form.tags = [];
  editingId.value = null;
  editingStatus.value = "";
}

function openCreate() {
  resetForm();
  dialog.value = true;
}

function toLocalDateTimeInput(value: string | null | undefined) {
  if (!value) return "";
  const date = new Date(value);
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function openEdit(item: any) {
  editingId.value = item.id;
  editingStatus.value = item.status;
  form.title = item.title ?? "";
  form.description = item.description ?? "";
  form.goalId = item.goalId ?? null;
  form.startAt = toLocalDateTimeInput(item.startAt);
  form.endAt = toLocalDateTimeInput(item.endAt);
  // 列表返回真实标签集合，复制一份供编辑器增删，避免直接改动列表行。
  form.tags = Array.isArray(item.tags) ? [...item.tags] : [];
  dialog.value = true;
}

function validateEndAt() {
  void formRef.value?.validateField("endAt");
}

async function load() {
  // 筛选值为空时不传参数，清除筛选即可恢复服务端全量列表。
  loading.value = true;
  error.value = "";
  try {
    const filter: { status?: string; goalId?: string } = {};
    if (statusFilter.value) filter.status = statusFilter.value;
    if (goalFilter.value) filter.goalId = goalFilter.value;
    [rows.value, goals.value] = await Promise.all([
      call(() => window.lifeSystem.projects.list(filter)),
      call(() => window.lifeSystem.goals.list({})),
    ]);
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

async function save() {
  try {
    const valid = await formRef.value?.validate();
    if (!valid) return;
    saving.value = true;
    const input = {
      ...form,
      startAt: toUtc(form.startAt),
      endAt: toUtc(form.endAt),
      tags: [...form.tags],
    };
    if (editingId.value) {
      await call(() =>
        window.lifeSystem.projects.update({ id: editingId.value, ...input }),
      );
      ElMessage.success("已保存");
    } else {
      await call(() => window.lifeSystem.projects.create(input));
      ElMessage.success("项目已创建");
    }
    dialog.value = false;
    resetForm();
    await load();
  } catch {
    // call 已统一提示失败原因；保留弹窗与表单输入，方便修正后重试。
  } finally {
    saving.value = false;
  }
}

async function changeStatus(item: any, value: string) {
  if (transitioningIds.has(item.id)) return;
  transitioningIds.add(item.id);
  try {
    await call(() =>
      window.lifeSystem.projects.updateStatus({ id: item.id, status: value }),
    );
    ElMessage.success(
      value === "done"
        ? "项目已完成"
        : value === "paused"
          ? "项目已暂停"
          : "项目已恢复",
    );
    await load();
  } catch {
    // 失败时保留服务端旧状态，避免页面看似变更但实际未保存。
  } finally {
    transitioningIds.delete(item.id);
  }
}

async function remove(id: string) {
  try {
    await ElMessageBox.confirm(
      "删除项目后，关联待办会保留但取消项目归属。",
      "确认删除",
      { type: "warning" },
    );
  } catch {
    // 用户取消确认时静默返回，避免产生未处理的 Promise rejection。
    return;
  }
  try {
    await call(() => window.lifeSystem.projects.remove(id));
    ElMessage.success("已删除");
    await load();
  } catch {
    // call 已提示失败原因，列表保持原状以便用户重试。
  }
}

function formatDate(value: string | null | undefined) {
  return value ? new Date(value).toLocaleString() : "—";
}

function deadlineState(item: any) {
  if (item.status !== "active" || !item.endAt) return "";
  const remaining = new Date(item.endAt).getTime() - Date.now();
  if (remaining < 0) return "expired";
  if (remaining <= 7 * 24 * 60 * 60 * 1000) return "soon";
  return "";
}

onMounted(load);
</script>

<template>
  <div class="page-head">
    <div>
      <h1>生活项目</h1>
      <p>组织一组行动，可选支持某个目标。</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="openCreate">
      新建项目
    </el-button>
  </div>

  <div class="toolbar">
    <el-select
      v-model="statusFilter"
      clearable
      placeholder="全部状态"
      style="width: 150px"
      @change="load"
    >
      <el-option label="进行中" value="active" />
      <el-option label="暂停" value="paused" />
      <el-option label="已完成" value="done" />
    </el-select>
    <el-select
      v-model="goalFilter"
      clearable
      placeholder="按目标"
      style="width: 220px"
      @change="load"
    >
      <el-option
        v-for="goal in goals"
        :key="goal.id"
        :label="goal.title"
        :value="goal.id"
      />
    </el-select>
  </div>

  <PageState
    :loading="loading"
    :error="error"
    :empty="rows.length === 0"
    :empty-text="hasFilter ? '当前筛选下没有项目' : '还没有生活项目'"
    @retry="load"
  >
    <div class="list">
      <div v-for="item in rows" :key="item.id" class="list-row">
        <div class="list-main">
          <div class="list-title">{{ item.title }}</div>
          <div class="list-meta">
            {{ item.goalTitle ? `支持目标：${item.goalTitle}` : "独立项目" }} ·
            {{ item.description || "无说明" }}
          </div>
          <div class="list-meta">
            时间：{{ formatDate(item.startAt) }} 至 {{ formatDate(item.endAt) }}
            <el-tag
              v-if="deadlineState(item) === 'expired'"
              type="danger"
              size="small"
            >
              已过期
            </el-tag>
            <el-tag
              v-else-if="deadlineState(item) === 'soon'"
              type="warning"
              size="small"
            >
              即将到期
            </el-tag>
          </div>
          <div v-if="item.tags?.length" class="tag-line">
            <el-tag v-for="tag in item.tags" :key="tag" size="small">
              {{ tag }}
            </el-tag>
          </div>
        </div>
        <el-select
          :model-value="item.status"
          size="small"
          style="width: 105px"
          :disabled="item.status === 'done' || transitioningIds.has(item.id)"
          :loading="transitioningIds.has(item.id)"
          @change="changeStatus(item, $event)"
        >
          <el-option label="进行中" value="active" />
          <el-option label="暂停" value="paused" />
          <el-option label="已完成" value="done" />
        </el-select>
        <div class="row-actions">
          <el-button text :icon="Edit" title="编辑" @click="openEdit(item)" />
          <el-button
            text
            type="danger"
            :icon="Delete"
            title="删除"
            @click="remove(item.id)"
          />
        </div>
      </div>
    </div>
  </PageState>

  <el-dialog
    v-model="dialog"
    :title="dialogTitle"
    width="520px"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="dialog-form"
    >
      <el-alert
        v-if="isDone"
        title="已完成项目只能修改说明和标签"
        type="info"
        :closable="false"
        show-icon
        class="form-notice"
      />
      <el-form-item label="标题" prop="title" required>
        <el-input
          v-model="form.title"
          maxlength="50"
          show-word-limit
          :disabled="isDone"
        />
      </el-form-item>
      <el-form-item label="支持目标">
        <el-select
          v-model="form.goalId"
          clearable
          style="width: 100%"
          :disabled="isDone"
        >
          <el-option
            v-for="goal in goals"
            :key="goal.id"
            :label="goal.title"
            :value="goal.id"
          />
        </el-select>
      </el-form-item>
      <div class="inline-fields">
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm"
            placeholder="可选"
            :disabled="isDone"
            @change="validateEndAt"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endAt">
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm"
            placeholder="可选"
            :disabled="isDone"
          />
        </el-form-item>
      </div>
      <el-form-item label="说明">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="标签" prop="tags">
        <el-select
          v-model="form.tags"
          multiple
          filterable
          allow-create
          default-first-option
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="dialog = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">
        {{ isEditing ? "保存" : "创建" }}
      </el-button>
    </template>
  </el-dialog>
</template>
