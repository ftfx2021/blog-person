<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessageBox } from "element-plus";
import { Plus, Delete, Edit } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { toUtc, useApi } from "../../shared/api";
const { call } = useApi();
const rows = ref<any[]>([]),
  goals = ref<any[]>([]),
  loading = ref(true),
  error = ref(""),
  dialog = ref(false);
const form = reactive<any>({
  title: "",
  description: "",
  goalId: null,
  startAt: "",
  endAt: "",
  tags: [],
});
async function load() {
  loading.value = true;
  try {
    [rows.value, goals.value] = await Promise.all([
      call(() => window.lifeSystem.projects.list({})),
      call(() => window.lifeSystem.goals.list({})),
    ]);
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function save() {
  await call(() =>
    window.lifeSystem.projects.create({
      ...form,
      startAt: toUtc(form.startAt),
      endAt: toUtc(form.endAt),
    }),
  );
  dialog.value = false;
  await load();
}
async function status(item: any, value: string) {
  await call(() =>
    window.lifeSystem.projects.updateStatus({ id: item.id, status: value }),
  );
  await load();
}
async function edit(item: any) {
  const result = await ElMessageBox.prompt("修改项目标题", "编辑生活项目", {
    inputValue: item.title,
    inputValidator: (value) =>
      value.trim().length > 0 ? true : "标题不能为空",
  });
  await call(() =>
    window.lifeSystem.projects.update({
      id: item.id,
      title: result.value,
      description: item.description,
      goalId: item.goalId,
      startAt: item.startAt,
      endAt: item.endAt,
      tags: [],
    }),
  );
  await load();
}
async function remove(id: string) {
  await ElMessageBox.confirm(
    "删除项目后，关联待办会保留但取消项目归属。",
    "确认删除",
    { type: "warning" },
  );
  await call(() => window.lifeSystem.projects.remove(id));
  await load();
}
onMounted(load);
</script>
<template>
  <div class="page-head">
    <div>
      <h1>生活项目</h1>
      <p>组织一组行动，可选支持某个目标。</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="dialog = true"
      >新建项目</el-button
    >
  </div>
  <PageState
    :loading="loading"
    :error="error"
    :empty="rows.length === 0"
    empty-text="暂无生活项目"
    @retry="load"
    ><div class="list">
      <div v-for="item in rows" :key="item.id" class="list-row">
        <div class="list-main">
          <div class="list-title">{{ item.title }}</div>
          <div class="list-meta">
            {{ item.goalTitle ? `支持目标：${item.goalTitle}` : "独立项目" }} ·
            {{ item.description || "无说明" }}
          </div>
        </div>
        <el-select
          :model-value="item.status"
          size="small"
          style="width: 105px"
          :disabled="item.status === 'done'"
          @change="status(item, $event)"
          ><el-option label="进行中" value="active" /><el-option
            label="暂停"
            value="paused" /><el-option
            label="已完成"
            value="done" /></el-select
        ><el-button
          text
          :icon="Edit"
          title="编辑"
          @click="edit(item)"
        /><el-button
          text
          type="danger"
          :icon="Delete"
          title="删除"
          @click="remove(item.id)"
        />
      </div></div></PageState
  ><el-dialog v-model="dialog" title="新建生活项目" width="520px"
    ><el-form label-position="top"
      ><el-form-item label="标题"
        ><el-input v-model="form.title" maxlength="50" /></el-form-item
      ><el-form-item label="支持目标"
        ><el-select v-model="form.goalId" clearable style="width: 100%"
          ><el-option
            v-for="goal in goals"
            :key="goal.id"
            :label="goal.title"
            :value="goal.id" /></el-select
      ></el-form-item>
      <div class="inline-fields">
        <el-form-item label="开始时间"
          ><el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm" /></el-form-item
        ><el-form-item label="结束时间"
          ><el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm"
        /></el-form-item>
      </div>
      <el-form-item label="说明"
        ><el-input v-model="form.description" type="textarea" /></el-form-item
      ><el-form-item label="标签"
        ><el-select
          v-model="form.tags"
          multiple
          filterable
          allow-create /></el-form-item></el-form
    ><template #footer
      ><el-button @click="dialog = false">取消</el-button
      ><el-button type="primary" @click="save">创建</el-button></template
    ></el-dialog
  >
</template>
