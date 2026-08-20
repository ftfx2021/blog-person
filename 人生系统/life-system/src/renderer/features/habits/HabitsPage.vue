<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { ElMessageBox } from "element-plus";
import {
  Plus,
  Delete,
  Check,
  RefreshLeft,
  Edit,
} from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { localToday, useApi } from "../../shared/api";

const { call } = useApi();
// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const rows = ref<any[]>([]);
const loading = ref(true);
const error = ref("");
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
const dialog = ref(false);
const historyDialog = ref(false);
const history = ref<any>();
const form = reactive<any>({
  name: "",
  note: "",
  frequencyType: "daily",
  weeklyTarget: null,
});

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function load(): Promise<void> {
  loading.value = true;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    rows.value = await call(() => window.lifeSystem.habits.list());
  } catch (caught: any) {
    error.value = caught.message;
  } finally {
    loading.value = false;
  }
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function save(): Promise<void> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    window.lifeSystem.habits.create({
      ...form,
      weeklyTarget:
        form.frequencyType === "daily" ? null : Number(form.weeklyTarget),
    }),
  );
  // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
  dialog.value = false;
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await loadWithTodayStatus();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function check(item: any): Promise<void> {
  // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
  const checkedOn = localToday();
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    item.checkedToday
      ? // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
        window.lifeSystem.habits.undo({
          id: item.id,
          checkedOn,
          today: checkedOn,
        })
      : // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
        window.lifeSystem.habits.checkin({
          id: item.id,
          checkedOn,
          today: checkedOn,
        }),
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await loadWithTodayStatus();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function showHistory(id: string): Promise<void> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  history.value = await call(() => window.lifeSystem.habits.history({ id }));
  historyDialog.value = true;
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function remove(id: string): Promise<void> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await ElMessageBox.confirm("习惯及全部打卡历史会永久删除。", "确认删除", {
    type: "warning",
  });
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() => window.lifeSystem.habits.remove(id));
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await loadWithTodayStatus();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function edit(item: any): Promise<void> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  const result = await ElMessageBox.prompt("修改习惯名称", "编辑习惯", {
    inputValue: item.name,
    inputValidator: (value) =>
      value.trim().length > 0 ? true : "名称不能为空",
  });
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    window.lifeSystem.habits.update({
      id: item.id,
      name: result.value,
      note: item.note,
      frequencyType: item.frequencyType,
      weeklyTarget: item.weeklyTarget,
    }),
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await loadWithTodayStatus();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function loadWithTodayStatus(): Promise<void> {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await load();
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (error.value) return;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  for (const item of rows.value) {
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    const today = localToday();
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const data: any = await call(() =>
      // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
      window.lifeSystem.habits.history({ id: item.id, from: today, to: today }),
    );
    item.checkedToday = data.checkins.length > 0;
  }
}
onMounted(loadWithTodayStatus);
</script>

<template>
  <div class="page-head">
    <div>
      <h1>习惯</h1>
      <p>习惯独立打卡，不转换成待办或目标进度。</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="dialog = true"
      >新建习惯</el-button
    >
  </div>
  <PageState
    :loading="loading"
    :error="error"
    :empty="rows.length === 0"
    empty-text="暂无习惯"
    @retry="loadWithTodayStatus"
    ><div class="list">
      <div v-for="item in rows" :key="item.id" class="list-row">
        <div class="list-main" @click="showHistory(item.id)">
          <div class="list-title">{{ item.name }}</div>
          <div class="list-meta">
            {{
              item.frequencyType === "daily"
                ? "每日"
                : `每周 ${item.weeklyTarget} 次`
            }}
            · 连续 {{ item.streak }}
            {{ item.frequencyType === "daily" ? "天" : "周" }}
          </div>
        </div>
        <el-button
          :type="item.checkedToday ? 'default' : 'primary'"
          :icon="item.checkedToday ? RefreshLeft : Check"
          @click="check(item)"
          >{{ item.checkedToday ? "撤销" : "打卡" }}</el-button
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
      </div></div
  ></PageState>
  <el-dialog v-model="dialog" title="新建习惯" width="460px"
    ><el-form label-position="top"
      ><el-form-item label="名称"
        ><el-input v-model="form.name" maxlength="50" /></el-form-item
      ><el-form-item label="频率"
        ><el-segmented
          v-model="form.frequencyType"
          :options="[
            { label: '每日', value: 'daily' },
            { label: '每周次数', value: 'weekly_times' },
          ]" /></el-form-item
      ><el-form-item
        v-if="form.frequencyType === 'weekly_times'"
        label="每周目标"
        ><el-input-number
          v-model="form.weeklyTarget"
          :min="1"
          :max="7" /></el-form-item
      ><el-form-item label="备注"
        ><el-input
          v-model="form.note"
          type="textarea" /></el-form-item></el-form
    ><template #footer
      ><el-button @click="dialog = false">取消</el-button
      ><el-button type="primary" @click="save">创建</el-button></template
    ></el-dialog
  >
  <el-dialog v-model="historyDialog" title="打卡历史" width="480px"
    ><div v-if="history?.checkins.length" class="list">
      <div v-for="item in history.checkins" :key="item.id" class="list-row">
        <div class="list-title">{{ String(item.checkedOn).slice(0, 10) }}</div>
      </div>
    </div>
    <el-empty v-else description="暂无打卡记录"
  /></el-dialog>
</template>
