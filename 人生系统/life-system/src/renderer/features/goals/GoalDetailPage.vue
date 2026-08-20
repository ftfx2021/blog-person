<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { useRoute, useRouter } from "vue-router";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { ElMessageBox } from "element-plus";
import { Back, Plus, Delete, Edit } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { useApi } from "../../shared/api";
const route = useRoute();
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
const router = useRouter();
const { call } = useApi();
// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const goal = ref<any>();
const loading = ref(true);
const error = ref("");
// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const recordDialog = ref(false);
// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const milestoneDialog = ref(false);
// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const record = reactive({
  value: 0,
  note: "",
  recordedAt: new Date().toISOString().slice(0, 16),
});
// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const milestone = reactive({ title: "", sortOrder: 0 });
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function load() {
  loading.value = true;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    goal.value = await call(() =>
      // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
      window.lifeSystem.goals.get(String(route.params.id)),
    );
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function finish(status: "done" | "abandoned") {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await ElMessageBox.confirm(
    status === "done" ? "确认将目标标记为完成？" : "确认放弃这个目标？",
    "目标将进入只读状态",
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    window.lifeSystem.goals.finish({ id: goal.value.id, status }),
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await load();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function remove() {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await ElMessageBox.confirm("删除后数据点与里程碑也会永久删除。", "确认删除", {
    type: "warning",
  });
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() => window.lifeSystem.goals.remove(goal.value.id));
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await router.push("/goals");
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function edit() {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const result = await ElMessageBox.prompt("修改目标标题", "编辑目标", {
    inputValue: goal.value.title,
    inputValidator: (value) =>
      value.trim().length > 0 ? true : "标题不能为空",
  });
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    window.lifeSystem.goals.update({
      id: goal.value.id,
      title: result.value,
      description: goal.value.description,
      period: goal.value.period,
      metricType: goal.value.metricType,
      unit: goal.value.unit,
      startValue: goal.value.startValue,
      targetValue: goal.value.targetValue,
      dueDate: goal.value.dueDate,
      tags: [],
      confirmRecalculate: false,
    }),
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await load();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function addRecord() {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    window.lifeSystem.goals.record({
      goalId: goal.value.id,
      value: Number(record.value),
      note: record.note || null,
      recordedAt: new Date(record.recordedAt).toISOString(),
    }),
  );
  recordDialog.value = false;
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await load();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function addMilestone() {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    window.lifeSystem.goals.createMilestone({
      goalId: goal.value.id,
      title: milestone.title,
      sortOrder: goal.value.milestones.length,
    }),
  );
  milestoneDialog.value = false;
  milestone.title = "";
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await load();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function toggle(item: any) {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    window.lifeSystem.goals.toggleMilestone({
      id: item.id,
      isDone: !item.isDone,
    }),
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await load();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function removeMilestone(id: string) {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await ElMessageBox.confirm("确认删除这个里程碑？", "删除里程碑");
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() => window.lifeSystem.goals.removeMilestone(id));
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await load();
}
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
            <el-button @click="finish('abandoned')">放弃</el-button
            ><el-button type="primary" @click="finish('done')"
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
            @click="recordDialog = true"
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
            @click="milestoneDialog = true"
            >添加里程碑</el-button
          >
        </div>
        <div v-if="goal.milestones.length" class="list">
          <div v-for="item in goal.milestones" :key="item.id" class="list-row">
            <el-checkbox
              :model-value="Boolean(item.isDone)"
              :disabled="goal.status !== 'active'"
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
        <el-button type="danger" plain :icon="Delete" @click="remove"
          >删除目标</el-button
        >
      </div></template
    ></PageState
  ><el-dialog v-model="recordDialog" title="记录真实数据" width="420px"
    ><el-form label-position="top"
      ><el-form-item label="数值"
        ><el-input-number
          v-model="record.value"
          controls-position="right" /></el-form-item
      ><el-form-item label="记录时间"
        ><el-date-picker
          v-model="record.recordedAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm" /></el-form-item
      ><el-form-item label="备注"
        ><el-input v-model="record.note" /></el-form-item></el-form
    ><template #footer
      ><el-button @click="recordDialog = false">取消</el-button
      ><el-button type="primary" @click="addRecord">保存</el-button></template
    ></el-dialog
  ><el-dialog v-model="milestoneDialog" title="添加里程碑" width="420px"
    ><el-input
      v-model="milestone.title"
      maxlength="100"
      placeholder="里程碑标题"
    /><template #footer
      ><el-button @click="milestoneDialog = false">取消</el-button
      ><el-button type="primary" @click="addMilestone"
        >添加</el-button
      ></template
    ></el-dialog
  >
</template>
