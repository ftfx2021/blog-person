<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessageBox } from "element-plus";
import {
  Plus,
  Delete,
  Check,
  RefreshLeft,
  Edit,
} from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { localToday, useApi } from "../../shared/api";

const { call } = useApi();
const rows = ref<any[]>([]);
const loading = ref(true);
const error = ref("");
const dialog = ref(false);
const historyDialog = ref(false);
const history = ref<any>();
const form = reactive<any>({
  name: "",
  note: "",
  frequencyType: "daily",
  weeklyTarget: null,
});

async function load(): Promise<void> {
  loading.value = true;
  try {
    rows.value = await call(() => window.lifeSystem.habits.list());
  } catch (caught: any) {
    error.value = caught.message;
  } finally {
    loading.value = false;
  }
}
async function save(): Promise<void> {
  await call(() =>
    window.lifeSystem.habits.create({
      ...form,
      weeklyTarget:
        form.frequencyType === "daily" ? null : Number(form.weeklyTarget),
    }),
  );
  dialog.value = false;
  await loadWithTodayStatus();
}
async function check(item: any): Promise<void> {
  const checkedOn = localToday();
  await call(() =>
    item.checkedToday
      ? window.lifeSystem.habits.undo({
          id: item.id,
          checkedOn,
          today: checkedOn,
        })
      : window.lifeSystem.habits.checkin({
          id: item.id,
          checkedOn,
          today: checkedOn,
        }),
  );
  await loadWithTodayStatus();
}
async function showHistory(id: string): Promise<void> {
  history.value = await call(() => window.lifeSystem.habits.history({ id }));
  historyDialog.value = true;
}
async function remove(id: string): Promise<void> {
  await ElMessageBox.confirm("习惯及全部打卡历史会永久删除。", "确认删除", {
    type: "warning",
  });
  await call(() => window.lifeSystem.habits.remove(id));
  await loadWithTodayStatus();
}
async function edit(item: any): Promise<void> {
  const result = await ElMessageBox.prompt("修改习惯名称", "编辑习惯", {
    inputValue: item.name,
    inputValidator: (value) =>
      value.trim().length > 0 ? true : "名称不能为空",
  });
  await call(() =>
    window.lifeSystem.habits.update({
      id: item.id,
      name: result.value,
      note: item.note,
      frequencyType: item.frequencyType,
      weeklyTarget: item.weeklyTarget,
    }),
  );
  await loadWithTodayStatus();
}
async function loadWithTodayStatus(): Promise<void> {
  await load();
  if (error.value) return;
  for (const item of rows.value) {
    const today = localToday();
    const data: any = await call(() =>
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
