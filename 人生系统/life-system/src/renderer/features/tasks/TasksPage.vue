<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessageBox } from "element-plus";
import {
  Plus,
  Delete,
  Right,
  RefreshLeft,
  Edit,
} from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { toUtc, useApi } from "../../shared/api";
const { call } = useApi();
const rows = ref<any[]>([]),
  goals = ref<any[]>([]),
  projects = ref<any[]>([]),
  loading = ref(true),
  error = ref(""),
  dialog = ref(false),
  statusFilter = ref("");
const form = reactive<any>({
  title: "",
  note: "",
  dueDate: "",
  goalId: null,
  projectId: null,
});
async function load() {
  loading.value = true;
  try {
    [rows.value, goals.value, projects.value] = await Promise.all([
      call(() =>
        window.lifeSystem.tasks.list(
          statusFilter.value
            ? { status: statusFilter.value }
            : { sort: "due_asc" },
        ),
      ),
      call(() => window.lifeSystem.goals.list({})),
      call(() => window.lifeSystem.projects.list({})),
    ]);
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function save() {
  await call(() =>
    window.lifeSystem.tasks.create({ ...form, dueDate: toUtc(form.dueDate) }),
  );
  dialog.value = false;
  await load();
}
async function transition(id: string, action: "advance" | "undo") {
  await call(() => window.lifeSystem.tasks.transition({ id, action }));
  await load();
}
async function edit(item: any) {
  const result = await ElMessageBox.prompt("修改待办标题", "编辑待办", {
    inputValue: item.title,
    inputValidator: (value) =>
      value.trim().length > 0 ? true : "标题不能为空",
  });
  await call(() =>
    window.lifeSystem.tasks.update({
      id: item.id,
      title: result.value,
      note: item.note,
      dueDate: item.dueDate,
      goalId: item.goalId,
      projectId: item.projectId,
    }),
  );
  await load();
}
async function remove(id: string) {
  await ElMessageBox.confirm("待办删除后不可恢复。", "确认删除", {
    type: "warning",
  });
  await call(() => window.lifeSystem.tasks.remove(id));
  await load();
}
onMounted(load);
const labels: any = { todo: "待处理", doing: "进行中", done: "已完成" };
</script>
<template>
  <div class="page-head">
    <div>
      <h1>待办</h1>
      <p>按截止时间排序，不设置重要或紧急等级。</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="dialog = true"
      >新建待办</el-button
    >
  </div>
  <div class="toolbar">
    <el-segmented
      v-model="statusFilter"
      :options="[
        { label: '全部', value: '' },
        { label: '待处理', value: 'todo' },
        { label: '进行中', value: 'doing' },
        { label: '已完成', value: 'done' },
      ]"
      @change="load"
    />
  </div>
  <PageState
    :loading="loading"
    :error="error"
    :empty="rows.length === 0"
    empty-text="当前筛选下没有待办"
    @retry="load"
    ><div class="list">
      <div v-for="item in rows" :key="item.id" class="list-row">
        <div class="list-main">
          <div
            class="list-title"
            :style="
              item.status === 'done'
                ? 'text-decoration:line-through;color:#8a9591'
                : ''
            "
          >
            {{ item.title }}
          </div>
          <div class="list-meta">
            {{ labels[item.status] }} ·
            {{
              item.dueDate
                ? new Date(item.dueDate).toLocaleString()
                : "无截止时间"
            }}<template v-if="item.goalTitle">
              · 目标：{{ item.goalTitle }}</template
            ><template v-if="item.projectTitle">
              · 项目：{{ item.projectTitle }}</template
            >
          </div>
        </div>
        <div class="row-actions">
          <el-button
            v-if="item.status !== 'done'"
            size="small"
            :icon="Right"
            @click="transition(item.id, 'advance')"
            >{{ item.status === "todo" ? "开始" : "完成" }}</el-button
          ><el-button
            v-if="item.status !== 'done'"
            text
            :icon="Edit"
            title="编辑"
            @click="edit(item)"
          /><el-button
            v-else
            size="small"
            :icon="RefreshLeft"
            @click="transition(item.id, 'undo')"
            >撤销完成</el-button
          ><el-button
            text
            type="danger"
            :icon="Delete"
            title="删除"
            @click="remove(item.id)"
          />
        </div>
      </div></div></PageState
  ><el-dialog v-model="dialog" title="新建待办" width="520px"
    ><el-form label-position="top"
      ><el-form-item label="标题"
        ><el-input v-model="form.title" maxlength="100" /></el-form-item
      ><el-form-item label="截止时间"
        ><el-date-picker
          v-model="form.dueDate"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm" /></el-form-item
      ><el-form-item label="备注"
        ><el-input v-model="form.note" type="textarea" /></el-form-item
      ><el-collapse
        ><el-collapse-item title="关联目标或项目"
          ><el-form-item label="目标"
            ><el-select v-model="form.goalId" clearable style="width: 100%"
              ><el-option
                v-for="item in goals"
                :key="item.id"
                :label="item.title"
                :value="item.id" /></el-select></el-form-item
          ><el-form-item label="生活项目"
            ><el-select v-model="form.projectId" clearable style="width: 100%"
              ><el-option
                v-for="item in projects"
                :key="item.id"
                :label="item.title"
                :value="
                  item.id
                " /></el-select></el-form-item></el-collapse-item></el-collapse></el-form
    ><template #footer
      ><el-button @click="dialog = false">取消</el-button
      ><el-button type="primary" @click="save">创建</el-button></template
    ></el-dialog
  >
</template>
