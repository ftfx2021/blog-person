<script setup lang="ts">
// 详情页的写操作均通过 API 完成，保证记录、里程碑和状态变更走同一校验链。
import { nextTick, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Back, Plus, Delete, Edit } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { toLocalInput, toUtc, useApi } from "../../shared/api";
const route = useRoute();
const router = useRouter();
const { call } = useApi();
const goal = ref<any>();
const loading = ref(true);
const error = ref("");
const recordDialog = ref(false);
const milestoneDialog = ref(false);
const editDialog = ref(false);
const recordFormRef = ref<any>();
const milestoneFormRef = ref<any>();
const editFormRef = ref<any>();
const recordSaving = ref(false);
const milestoneSaving = ref(false);
const editSaving = ref(false);
const actionBusy = ref(false);
const recordInitial = ref("");
const milestoneInitial = ref("");
const editInitial = ref("");
const editingGoalId = ref("");
const record = reactive({
  value: 0,
  note: "",
  recordedAt: toLocalInput(new Date().toISOString()),
});
const milestone = reactive({ title: "", sortOrder: 0 });
const editForm = reactive<any>({
  title: "",
  description: "",
  period: "quarterly",
  metricType: "numeric",
  unit: "",
  startValue: null,
  targetValue: null,
  dueDate: "",
  tags: [],
});
const editRules: any = {
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
        if (editForm.metricType !== "numeric") return callback();
        if (value == null || !Number.isFinite(Number(value)))
          return callback(new Error("请输入起点值"));
        if (Number(value) === Number(editForm.targetValue))
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
        if (editForm.metricType !== "numeric") return callback();
        if (value == null || !Number.isFinite(Number(value)))
          return callback(new Error("请输入目标值"));
        if (Number(value) === Number(editForm.startValue))
          return callback(new Error("目标值不能等于起点值"));
        callback();
      },
    },
  ],
};
const recordRules: any = {
  value: [
    {
      trigger: "change",
      validator: (
        _rule: any,
        value: number | null,
        callback: (error?: Error) => void,
      ) => {
        if (value == null || !Number.isFinite(Number(value)))
          return callback(new Error("请输入有效数值"));
        callback();
      },
    },
  ],
  note: [{ max: 1000, message: "备注不能超过 1000 个字符", trigger: "blur" }],
  recordedAt: [
    { required: true, message: "请选择记录时间", trigger: "change" },
  ],
};
const milestoneRules: any = {
  title: [
    { required: true, message: "请输入里程碑标题", trigger: "blur" },
    { max: 100, message: "标题不能超过 100 个字符", trigger: "blur" },
  ],
};
function disablePastDate(date: Date) {
  // 截止时间按本地日历日限制，今天可选，今天之前的日期不可选。
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date.getTime() < today.getTime();
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
function closeEditDialog() {
  if (JSON.stringify(editForm) === editInitial.value) {
    editDialog.value = false;
    return;
  }
  void confirmClose("当前修改尚未保存，确定关闭吗？", () => {
    editDialog.value = false;
  });
}
function beforeCloseEdit(done: () => void) {
  if (JSON.stringify(editForm) === editInitial.value) {
    done();
    return;
  }
  void confirmClose("当前修改尚未保存，确定关闭吗？", done);
}
function closeRecordDialog() {
  if (JSON.stringify(record) === recordInitial.value) {
    recordDialog.value = false;
    return;
  }
  void confirmClose("当前记录尚未保存，确定关闭吗？", () => {
    recordDialog.value = false;
  });
}
function beforeCloseRecord(done: () => void) {
  if (JSON.stringify(record) === recordInitial.value) {
    done();
    return;
  }
  void confirmClose("当前记录尚未保存，确定关闭吗？", done);
}
function closeMilestoneDialog() {
  if (JSON.stringify(milestone) === milestoneInitial.value) {
    milestoneDialog.value = false;
    return;
  }
  void confirmClose("当前里程碑尚未保存，确定关闭吗？", () => {
    milestoneDialog.value = false;
  });
}
function beforeCloseMilestone(done: () => void) {
  if (JSON.stringify(milestone) === milestoneInitial.value) {
    done();
    return;
  }
  void confirmClose("当前里程碑尚未保存，确定关闭吗？", done);
}
async function load() {
  // 详情与关联记录一并读取，减少页面出现半更新状态的时间窗口。
  loading.value = true;
  error.value = "";
  try {
    goal.value = await call(() =>
      window.lifeSystem.goals.get(String(route.params.id)),
    );
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function finish(status: "done" | "abandoned") {
  // 完成或放弃前由服务层校验状态机，页面只传递用户选择。
  try {
    await ElMessageBox.confirm(
      status === "done" ? "确认将目标标记为完成？" : "确认放弃这个目标？",
      "目标将进入只读状态",
    );
  } catch {
    return;
  }
  actionBusy.value = true;
  try {
    await call(() =>
      window.lifeSystem.goals.finish({ id: goal.value.id, status }),
    );
    ElMessage.success(status === "done" ? "目标已完成" : "目标已放弃");
    await load();
  } catch {
    // call 已展示错误；保留当前详情状态，避免失败时误更新页面快照。
  } finally {
    actionBusy.value = false;
  }
}
async function remove() {
  // 目标删除会级联移除数据点和里程碑，因此要求明确确认后才发起请求。
  try {
    await ElMessageBox.confirm(
      "删除后数据点与里程碑也会永久删除。",
      "确认删除",
      {
        type: "warning",
      },
    );
  } catch {
    return;
  }
  actionBusy.value = true;
  try {
    await call(() => window.lifeSystem.goals.remove(goal.value.id));
    ElMessage.success("已删除");
    await router.push("/goals");
  } catch {
    // 删除失败时保留详情页，用户仍可查看并重试。
  } finally {
    actionBusy.value = false;
  }
}
function edit() {
  // 回填完整目标快照，尤其保留既有标签，避免编辑标题时覆盖标签关联。
  const currentGoal = goal.value;
  if (!currentGoal?.id) {
    ElMessage.error("目标详情尚未加载完成，请稍后重试");
    return;
  }
  // 打开弹窗时固定实体 ID，异步保存期间不依赖可能更新的详情响应式对象。
  editingGoalId.value = currentGoal.id;
  Object.assign(editForm, {
    title: currentGoal.title,
    description: currentGoal.description || "",
    period: currentGoal.period,
    metricType: currentGoal.metricType,
    unit: currentGoal.unit || "",
    startValue: currentGoal.startValue,
    targetValue: currentGoal.targetValue,
    dueDate: toLocalInput(currentGoal.dueDate),
    tags: [...(currentGoal.tags || [])],
  });
  editInitial.value = JSON.stringify(editForm);
  editDialog.value = true;
  nextTick(() => editFormRef.value?.clearValidate());
}
async function saveEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (!editingGoalId.value) {
    ElMessage.error("未找到正在编辑的目标，请重新打开编辑窗口");
    return;
  }
  editSaving.value = true;
  const buildInput = (confirmRecalculate: boolean) => ({
    // Context bridge 不能克隆 Vue Proxy；编辑表单的 tags 必须复制为普通数组。
    id: editingGoalId.value,
    title: editForm.title,
    description: editForm.description || null,
    period: editForm.period,
    metricType: editForm.metricType,
    unit: editForm.unit || null,
    startValue:
      editForm.metricType === "numeric" ? Number(editForm.startValue) : null,
    targetValue:
      editForm.metricType === "numeric" ? Number(editForm.targetValue) : null,
    dueDate: toUtc(editForm.dueDate),
    tags: [...editForm.tags],
    confirmRecalculate,
  });
  const input = () => window.lifeSystem.goals.update(buildInput(false));
  try {
    try {
      await call(input);
    } catch (caught: any) {
      if (caught?.code !== "CONFLICT") throw caught;
      // 服务端只在存在历史数据且公式变化时返回冲突，二次确认后才允许重算。
      try {
        await ElMessageBox.confirm(
          "修改起点或目标值会重算历史进度，确认继续吗？",
          "需确认重算",
          { type: "warning" },
        );
      } catch {
        return;
      }
      await call(() => window.lifeSystem.goals.update(buildInput(true)));
    }
    ElMessage.success("已保存");
    editDialog.value = false;
    editingGoalId.value = "";
    await load();
  } catch {
    // call 已展示错误；保存失败时保留弹窗和全部输入，方便用户重试。
  } finally {
    editSaving.value = false;
  }
}
function openRecord() {
  Object.assign(record, {
    value: 0,
    note: "",
    recordedAt: toLocalInput(new Date().toISOString()),
  });
  recordInitial.value = JSON.stringify(record);
  recordDialog.value = true;
  nextTick(() => recordFormRef.value?.clearValidate());
}
async function addRecord() {
  // 记录时间转换为 ISO 再提交，保持主进程以 UTC 规则验证“不得晚于现在”。
  const valid = await recordFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  recordSaving.value = true;
  try {
    await call(() =>
      window.lifeSystem.goals.record({
        goalId: goal.value.id,
        value: Number(record.value),
        note: record.note || null,
        recordedAt: new Date(record.recordedAt).toISOString(),
      }),
    );
    ElMessage.success("数据已记录");
    recordDialog.value = false;
    await load();
  } catch {
    // call 已展示错误；失败时保留弹窗和记录输入。
  } finally {
    recordSaving.value = false;
  }
}
function openMilestone() {
  Object.assign(milestone, { title: "", sortOrder: 0 });
  milestoneInitial.value = JSON.stringify(milestone);
  milestoneDialog.value = true;
  nextTick(() => milestoneFormRef.value?.clearValidate());
}
async function addMilestone() {
  // 新里程碑默认排在现有项之后，排序权威仍由服务端持久化。
  const valid = await milestoneFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  milestoneSaving.value = true;
  try {
    await call(() =>
      window.lifeSystem.goals.createMilestone({
        goalId: goal.value.id,
        title: milestone.title,
        sortOrder: goal.value.milestones.length,
      }),
    );
    ElMessage.success("里程碑已添加");
    milestoneDialog.value = false;
    await load();
  } catch {
    // call 已展示错误；失败时保留弹窗和标题输入。
  } finally {
    milestoneSaving.value = false;
  }
}
async function toggle(item: any) {
  // 复选框只反转当前完成态；服务端同步维护完成时间与目标状态限制。
  actionBusy.value = true;
  try {
    await call(() =>
      window.lifeSystem.goals.toggleMilestone({
        id: item.id,
        isDone: !item.isDone,
      }),
    );
    ElMessage.success("里程碑状态已更新");
    await load();
  } catch {
    // call 已展示错误；失败时重新保持服务端快照，不乐观修改复选框。
  } finally {
    actionBusy.value = false;
  }
}
async function removeMilestone(id: string) {
  // 删除里程碑先确认，因为服务端会随即压实全部剩余排序序号。
  try {
    await ElMessageBox.confirm("确认删除这个里程碑？", "删除里程碑");
  } catch {
    return;
  }
  actionBusy.value = true;
  try {
    await call(() => window.lifeSystem.goals.removeMilestone(id));
    ElMessage.success("里程碑已删除");
    await load();
  } catch {
    // call 已展示错误；删除失败时保留当前详情状态。
  } finally {
    actionBusy.value = false;
  }
}
// 路由参数已确定后加载详情，失败由 PageState 提供可重试入口。
onMounted(load);
</script>
<template>
  <PageState :loading="loading" :error="error" :empty="!goal" @retry="load"
    ><template v-if="goal"
      ><div class="page-head">
        <div>
          <el-button link :icon="Back" @click="router.push('/goals')"
            >返回目标</el-button
          >
          <h1>{{ goal.title }}</h1>
          <p>{{ goal.description || "没有补充说明" }}</p>
        </div>
        <div class="row-actions">
          <el-button :icon="Edit" @click="edit">编辑</el-button>
          <template v-if="goal.status === 'active'">
            <el-button :disabled="actionBusy" @click="finish('abandoned')"
              >放弃</el-button
            ><el-button
              type="primary"
              :loading="actionBusy"
              @click="finish('done')"
              >标记完成</el-button
            >
          </template>
        </div>
      </div>
      <section class="section">
        <div class="stats">
          <div class="stat">
            <strong>{{
              goal.progress == null ? "--" : `${goal.progress}%`
            }}</strong
            ><span>{{
              goal.metricType === "status" ? "状态型目标" : "当前进度"
            }}</span>
          </div>
          <div class="stat">
            <strong>{{
              goal.status === "active"
                ? "进行中"
                : goal.status === "done"
                  ? "已完成"
                  : "已放弃"
            }}</strong
            ><span>目标状态</span>
          </div>
        </div>
        <el-progress
          v-if="goal.progress != null"
          :percentage="goal.progress"
          :stroke-width="10"
        />
      </section>
      <section v-if="goal.metricType === 'numeric'" class="section">
        <div class="page-head">
          <div>
            <h2 class="section-title">数据趋势</h2>
            <p>同一天多次记录时，以时间最新值计算当前进度。</p>
          </div>
          <el-button
            v-if="goal.status === 'active'"
            type="primary"
            :icon="Plus"
            :disabled="actionBusy"
            @click="openRecord"
            >记录数据</el-button
          >
        </div>
        <div v-if="goal.records.length" class="list">
          <div
            v-for="item in [...goal.records].reverse()"
            :key="item.id"
            class="list-row"
          >
            <div class="list-main">
              <div class="list-title">{{ item.value }} {{ goal.unit }}</div>
              <div class="list-meta">
                {{ new Date(item.recordedAt).toLocaleString() }} ·
                {{ item.note || "无备注" }}
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-inline">尚未记录真实数据。</div>
      </section>
      <section v-if="goal.metricType === 'milestone'" class="section">
        <div class="page-head">
          <div><h2 class="section-title">里程碑</h2></div>
          <el-button
            v-if="goal.status === 'active'"
            type="primary"
            :icon="Plus"
            :disabled="actionBusy"
            @click="openMilestone"
            >添加里程碑</el-button
          >
        </div>
        <div v-if="goal.milestones.length" class="list">
          <div v-for="item in goal.milestones" :key="item.id" class="list-row">
            <el-checkbox
              :model-value="Boolean(item.isDone)"
              :disabled="goal.status !== 'active' || actionBusy"
              @change="toggle(item)"
            />
            <div class="list-main">
              <div class="list-title">{{ item.title }}</div>
            </div>
            <el-button
              v-if="goal.status === 'active'"
              text
              type="danger"
              :icon="Delete"
              :disabled="actionBusy"
              title="删除"
              @click="removeMilestone(item.id)"
            />
          </div>
        </div>
        <div v-else class="empty-inline">还没有里程碑。</div>
      </section>
      <section class="section">
        <h2 class="section-title">支持行动</h2>
        <div
          v-if="
            goal.supportingActions.projects.length ||
            goal.supportingActions.tasks.length
          "
          class="list"
        >
          <div
            v-for="item in [
              ...goal.supportingActions.projects,
              ...goal.supportingActions.tasks,
            ]"
            :key="item.id"
            class="list-row"
          >
            <div class="list-main">
              <div class="list-title">{{ item.title }}</div>
              <div class="list-meta">{{ item.status }}</div>
            </div>
          </div>
        </div>
        <div v-else class="empty-inline">
          暂无关联项目或待办。行动数量不会改变目标进度。
        </div>
      </section>
      <div class="danger-zone">
        <el-button
          type="danger"
          plain
          :icon="Delete"
          :loading="actionBusy"
          @click="remove"
          >删除目标</el-button
        >
      </div></template
    ></PageState
  ><el-dialog
    v-model="editDialog"
    title="编辑目标"
    width="560px"
    :close-on-click-modal="false"
    :before-close="beforeCloseEdit"
    ><el-form
      ref="editFormRef"
      :model="editForm"
      :rules="editRules"
      label-position="top"
      class="dialog-form"
      ><el-alert
        v-if="goal?.status !== 'active'"
        title="已结束目标只能修改说明和标签"
        type="info"
        :closable="false"
        show-icon
        class="form-notice" /><el-form-item label="标题" prop="title"
        ><el-input
          v-model="editForm.title"
          maxlength="50"
          show-word-limit
          :disabled="goal?.status !== 'active'"
      /></el-form-item>
      <div class="inline-fields">
        <el-form-item label="周期"
          ><el-select
            v-model="editForm.period"
            :disabled="goal?.status !== 'active'"
            ><el-option label="年度" value="annual" /><el-option
              label="季度"
              value="quarterly" /><el-option
              label="月度"
              value="monthly" /></el-select></el-form-item
        ><el-form-item label="度量方式"
          ><el-select
            v-model="editForm.metricType"
            :disabled="goal?.status !== 'active'"
            ><el-option label="数值型" value="numeric" /><el-option
              label="里程碑型"
              value="milestone" /><el-option
              label="状态型"
              value="status" /></el-select
        ></el-form-item>
      </div>
      <template v-if="editForm.metricType === 'numeric'"
        ><el-form-item label="单位" prop="unit"
          ><el-input
            v-model="editForm.unit"
            maxlength="32"
            show-word-limit
            :disabled="goal?.status !== 'active'"
        /></el-form-item>
        <div class="inline-fields">
          <el-form-item label="起点值" prop="startValue"
            ><el-input-number
              v-model="editForm.startValue"
              controls-position="right"
              :disabled="goal?.status !== 'active'" /></el-form-item
          ><el-form-item label="目标值" prop="targetValue"
            ><el-input-number
              v-model="editForm.targetValue"
              controls-position="right"
              :disabled="goal?.status !== 'active'"
          /></el-form-item></div></template
      ><el-form-item label="截止时间"
        ><el-date-picker
          v-model="editForm.dueDate"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm"
          placeholder="可选"
          :disabled-date="disablePastDate"
          :disabled="goal?.status !== 'active'" /></el-form-item
      ><el-form-item label="说明"
        ><el-input
          v-model="editForm.description"
          type="textarea"
          :rows="3" /></el-form-item
      ><el-form-item label="标签" prop="tags"
        ><el-select
          v-model="editForm.tags"
          multiple
          filterable
          allow-create
          default-first-option /></el-form-item></el-form
    ><template #footer
      ><el-button @click="closeEditDialog">取消</el-button
      ><el-button type="primary" :loading="editSaving" @click="saveEdit"
        >保存</el-button
      ></template
    ></el-dialog
  ><el-dialog
    v-model="recordDialog"
    title="记录真实数据"
    width="420px"
    :close-on-click-modal="false"
    :before-close="beforeCloseRecord"
    ><el-form
      ref="recordFormRef"
      :model="record"
      :rules="recordRules"
      label-position="top"
      ><el-form-item label="数值" prop="value"
        ><el-input-number
          v-model="record.value"
          controls-position="right" /></el-form-item
      ><el-form-item label="记录时间" prop="recordedAt"
        ><el-date-picker
          v-model="record.recordedAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm" /></el-form-item
      ><el-form-item label="备注" prop="note"
        ><el-input
          v-model="record.note"
          type="textarea"
          maxlength="1000"
          show-word-limit
          :rows="3" /></el-form-item></el-form
    ><template #footer
      ><el-button @click="closeRecordDialog">取消</el-button
      ><el-button type="primary" :loading="recordSaving" @click="addRecord"
        >保存</el-button
      ></template
    ></el-dialog
  ><el-dialog
    v-model="milestoneDialog"
    title="添加里程碑"
    width="420px"
    :close-on-click-modal="false"
    :before-close="beforeCloseMilestone"
    ><el-form
      ref="milestoneFormRef"
      :model="milestone"
      :rules="milestoneRules"
      label-position="top"
      ><el-form-item label="标题" prop="title"
        ><el-input
          v-model="milestone.title"
          maxlength="100"
          show-word-limit
          placeholder="里程碑标题" /></el-form-item></el-form
    ><template #footer
      ><el-button @click="closeMilestoneDialog">取消</el-button
      ><el-button
        type="primary"
        :loading="milestoneSaving"
        @click="addMilestone"
        >添加</el-button
      ></template
    ></el-dialog
  >
</template>
