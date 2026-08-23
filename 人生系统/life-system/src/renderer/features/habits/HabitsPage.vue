<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage, ElMessageBox } from "element-plus";
import { Box, Check, Delete, Edit, Plus, RefreshLeft, VideoPause, VideoPlay } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { localToday, useApi } from "../../shared/api";

type Habit = { id: string; name: string; note: string | null; frequencyType: "daily" | "weekly_times"; weeklyTarget: number | null; status: "active" | "paused" | "archived"; streak: number; lastDoneOn: string | null; createdAt: string; updatedAt: string; checkedToday: boolean; weeklyCheckinCount: number };
const { call } = useApi();
const rows = ref<Habit[]>([]); const loading = ref(true); const error = ref("");
const dialog = ref(false); const historyDialog = ref(false); const history = ref<any>();
const formRef = ref<FormInstance>(); const saving = ref(false); const editingId = ref<string | null>(null);
const checkingIds = ref(new Set<string>()); const statusIds = ref(new Set<string>()); const historySaving = ref(false);
const includeArchived = ref(false); const sortBy = ref("recent"); const calendarDate = ref(new Date());
const form = reactive({ name: "", note: "", frequencyType: "daily" as "daily" | "weekly_times", weeklyTarget: null as number | null });
const rules: FormRules = {
  // 名称规则与服务端 min(1).max(50) 一致，前端提前阻止空白提交。
  name: [{ required: true, message: "请输入习惯名称", trigger: "blur" }, { max: 50, message: "习惯名称不能超过 50 个字符", trigger: "blur" }],
  // 周目标仅在周频率下校验，避免 daily 习惯携带与契约冲突的阈值。
  weeklyTarget: [{ validator: (_rule, value, callback) => { if (form.frequencyType !== "weekly_times") return callback(); if (!Number.isInteger(value) || value < 1 || value > 7) return callback(new Error("每周目标需为 1 到 7 次")); callback(); }, trigger: "change" }],
};
const visibleRows = computed(() => {
  const copied = [...rows.value];
  if (sortBy.value === "createdDesc") return copied.sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)));
  if (sortBy.value === "createdAsc") return copied.sort((a, b) => String(a.createdAt).localeCompare(String(b.createdAt)));
  return copied.sort((a, b) => String(b.lastDoneOn ?? "").localeCompare(String(a.lastDoneOn ?? "")));
});
function resetForm(): void { editingId.value = null; Object.assign(form, { name: "", note: "", frequencyType: "daily", weeklyTarget: null }); formRef.value?.clearValidate(); }
function openCreate(): void { resetForm(); dialog.value = true; }
function openEdit(item: Habit): void { editingId.value = item.id; Object.assign(form, { name: item.name, note: item.note ?? "", frequencyType: item.frequencyType, weeklyTarget: item.weeklyTarget }); dialog.value = true; formRef.value?.clearValidate(); }
function toDateKey(date: Date): string { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`; }
function startOfWeek(today: string): string { const date = new Date(`${today}T00:00:00`); date.setDate(date.getDate() - (date.getDay() || 7) + 1); return toDateKey(date); }
function statusText(status: Habit["status"]): string { return status === "paused" ? "已暂停" : status === "archived" ? "已归档" : "进行中"; }
function weeklyMeta(item: Habit): string { const target = item.weeklyTarget ?? 0; return item.weeklyCheckinCount >= target ? `本周 ${item.weeklyCheckinCount}/${target} 次，已达标` : `本周 ${item.weeklyCheckinCount}/${target} 次，还差 ${target - item.weeklyCheckinCount} 次`; }
function isWeekDone(item: Habit): boolean { return item.frequencyType === "weekly_times" && item.weeklyCheckinCount >= Number(item.weeklyTarget); }
async function load(): Promise<void> { loading.value = true; error.value = ""; try { // 一次列表请求同时取得今日状态与周进度，避免逐条历史查询造成 N+1。
  rows.value = await call(() => window.lifeSystem.habits.list({ includeArchived: includeArchived.value, today: localToday() }));
} catch (caught: any) { error.value = caught.message; } finally { loading.value = false; } }
async function save(): Promise<void> { try { // 校验失败时保持弹窗和输入不变，让用户直接修正。
  await formRef.value?.validate(); saving.value = true;
  const payload = { name: form.name, note: form.note, frequencyType: form.frequencyType, weeklyTarget: form.frequencyType === "daily" ? null : Number(form.weeklyTarget) };
  const original = rows.value.find((item) => item.id === editingId.value);
  if (original && (original.frequencyType !== payload.frequencyType || original.weeklyTarget !== payload.weeklyTarget)) { try { await ElMessageBox.confirm("修改频率会按全部打卡历史重新计算连续天数/周数，确定继续？", "确认修改频率", { type: "warning" }); } catch { return; } }
  if (editingId.value) { await call(() => window.lifeSystem.habits.update({ id: editingId.value!, ...payload })); ElMessage.success("已保存"); } else { await call(() => window.lifeSystem.habits.create(payload)); ElMessage.success("习惯已创建"); }
  dialog.value = false; await load();
} catch { // call 已显示失败信息；保留弹窗和输入供用户重试。
} finally { saving.value = false; } }
async function check(item: Habit): Promise<void> { if (item.status !== "active" || checkingIds.value.has(item.id)) return; checkingIds.value = new Set(checkingIds.value).add(item.id); const checkedOn = localToday(); try { await call(() => item.checkedToday ? window.lifeSystem.habits.undo({ id: item.id, checkedOn, today: checkedOn }) : window.lifeSystem.habits.checkin({ id: item.id, checkedOn, today: checkedOn })); ElMessage.success(item.checkedToday ? "已撤销今日打卡" : "打卡成功"); await load(); } catch { // 失败不乐观更新，避免本地状态与服务端事实不一致。
} finally { const next = new Set(checkingIds.value); next.delete(item.id); checkingIds.value = next; } }
async function remove(id: string): Promise<void> { try { await ElMessageBox.confirm("习惯及全部打卡历史会永久删除。", "确认删除", { type: "warning" }); } catch { return; } try { await call(() => window.lifeSystem.habits.remove(id)); ElMessage.success("已删除"); await load(); } catch { // call 已提示错误，保持当前列表。
} }
async function updateStatus(item: Habit, status: Habit["status"]): Promise<void> { if (statusIds.value.has(item.id)) return; if (status === "archived") { try { await ElMessageBox.confirm("归档后将从主列表隐藏，历史记录会保留。", "确认归档", { type: "warning" }); } catch { return; } } statusIds.value = new Set(statusIds.value).add(item.id); try { await call(() => window.lifeSystem.habits.updateStatus({ id: item.id, status })); ElMessage.success(status === "paused" ? "已暂停" : status === "archived" ? "已归档" : "已恢复"); await load(); } catch { // 失败不预先切换标签，展示仍以数据库状态为准。
} finally { const next = new Set(statusIds.value); next.delete(item.id); statusIds.value = next; } }
async function showHistory(item: Habit): Promise<void> { try { // 读取完整历史后才打开，日历换月不再产生额外请求。
  history.value = await call(() => window.lifeSystem.habits.history({ id: item.id })); calendarDate.value = new Date(`${localToday()}T00:00:00`); historyDialog.value = true;
} catch { // 失败时不打开可能残留旧数据的弹窗。
} }
const historyDays = computed(() => new Set((history.value?.checkins ?? []).map((item: any) => String(item.checkedOn).slice(0, 10))));
const historyWeekCount = computed(() => [...historyDays.value].filter((day) => String(day) >= startOfWeek(localToday()) && String(day) <= localToday()).length);
function isFuture(day: string): boolean { return day > localToday(); }
async function toggleHistoryDay(day: string): Promise<void> { if (!history.value || isFuture(day) || historySaving.value) return; const checked = historyDays.value.has(day); try { await ElMessageBox.confirm(checked ? "撤销该日打卡？" : "补记该日打卡？", checked ? "确认撤销" : "确认补卡", { type: "warning" }); } catch { return; } historySaving.value = true; try { await call(() => checked ? window.lifeSystem.habits.undo({ id: history.value.habit.id, checkedOn: day, today: localToday() }) : window.lifeSystem.habits.checkin({ id: history.value.habit.id, checkedOn: day, today: localToday() })); ElMessage.success(checked ? "已撤销该日打卡" : "补卡成功"); history.value = await call(() => window.lifeSystem.habits.history({ id: history.value.habit.id })); await load(); } catch { // 刷新只在写入成功后进行，失败不清空当前日历标记。
} finally { historySaving.value = false; } }
onMounted(load);
</script>

<template>
  <div class="page-head"><div><h1>习惯</h1><p>习惯独立打卡，不转换成待办或目标进度。</p></div><div class="habit-toolbar"><el-select v-model="sortBy" aria-label="列表排序" style="width: 142px"><el-option label="最近打卡" value="recent" /><el-option label="创建时间（新）" value="createdDesc" /><el-option label="创建时间（旧）" value="createdAsc" /></el-select><el-switch v-model="includeArchived" active-text="查看归档" @change="load" /><el-button type="primary" :icon="Plus" @click="openCreate">新建习惯</el-button></div></div>
  <PageState :loading="loading" :error="error" :empty="rows.length === 0" empty-text="暂无习惯" @retry="load"><div class="list"><div v-for="item in visibleRows" :key="item.id" class="list-row"><div class="list-main" @click="showHistory(item)"><div class="list-title">{{ item.name }} <el-tag v-if="item.status !== 'active'" size="small" :type="item.status === 'archived' ? 'info' : 'warning'">{{ statusText(item.status) }}</el-tag></div><div class="list-meta">{{ item.frequencyType === "daily" ? "每日" : `每周 ${item.weeklyTarget} 次` }} · 连续 {{ item.streak }}{{ item.frequencyType === "daily" ? "天" : "周" }}<template v-if="item.frequencyType === 'weekly_times'"> · <span :class="{ 'week-done': isWeekDone(item) }">{{ weeklyMeta(item) }}</span></template></div></div><div class="habit-actions"><el-button :type="item.checkedToday ? 'default' : 'primary'" :icon="item.checkedToday ? RefreshLeft : Check" :loading="checkingIds.has(item.id)" :disabled="item.status !== 'active'" @click="check(item)">{{ item.checkedToday ? "撤销" : "打卡" }}</el-button><el-button v-if="item.status === 'active'" text :icon="VideoPause" title="暂停" :loading="statusIds.has(item.id)" @click="updateStatus(item, 'paused')" /><el-button v-else-if="item.status === 'paused' || item.status === 'archived'" text :icon="VideoPlay" title="恢复" :loading="statusIds.has(item.id)" @click="updateStatus(item, 'active')" /><el-button v-if="item.status !== 'archived'" text :icon="Box" title="归档" :loading="statusIds.has(item.id)" @click="updateStatus(item, 'archived')" /><el-button text :icon="Edit" title="编辑" @click="openEdit(item)" /><el-button text type="danger" :icon="Delete" title="删除" @click="remove(item.id)" /></div></div></div></PageState>
  <el-dialog v-model="dialog" :title="editingId ? '编辑习惯' : '新建习惯'" width="460px" @closed="resetForm"><el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="save"><el-form-item label="名称" prop="name" required><el-input v-model="form.name" maxlength="50" show-word-limit /></el-form-item><el-form-item label="频率" prop="frequencyType"><el-segmented v-model="form.frequencyType" :options="[{ label: '每日', value: 'daily' }, { label: '每周次数', value: 'weekly_times' }]" @change="form.weeklyTarget = form.frequencyType === 'daily' ? null : form.weeklyTarget" /></el-form-item><el-form-item v-if="form.frequencyType === 'weekly_times'" label="每周目标" prop="weeklyTarget"><el-input-number v-model="form.weeklyTarget" :min="1" :max="7" /></el-form-item><el-form-item label="备注"><el-input v-model="form.note" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">{{ editingId ? "保存" : "创建" }}</el-button></template></el-dialog>
  <el-dialog v-model="historyDialog" :title="history ? `${history.habit.name}的打卡历史` : '打卡历史'" width="660px"><template v-if="history"><div class="habit-stats"><span>累计打卡 {{ history.checkins.length }} 天</span><span>当前连续 {{ history.habit.streak }}{{ history.habit.frequencyType === 'daily' ? ' 天' : ' 周' }}</span><span v-if="history.habit.frequencyType === 'weekly_times'">本周 {{ historyWeekCount }}/{{ history.habit.weeklyTarget }} 次{{ historyWeekCount >= history.habit.weeklyTarget ? '，已达标' : '' }}</span></div><el-calendar v-model="calendarDate" class="habit-calendar"><template #date-cell="{ data }"><button class="calendar-day" :class="{ checked: historyDays.has(data.day), future: isFuture(data.day) }" :disabled="isFuture(data.day) || historySaving" @click="toggleHistoryDay(data.day)"><span>{{ data.day.slice(-2) }}</span><Check v-if="historyDays.has(data.day)" /></button></template></el-calendar></template></el-dialog>
</template>

<style scoped>
.habit-toolbar, .habit-actions, .habit-stats { display: flex; align-items: center; gap: 10px; }
.habit-actions { flex-shrink: 0; }.week-done { color: var(--el-color-success); }.habit-stats { flex-wrap: wrap; padding: 0 4px 14px; color: var(--el-text-color-secondary); font-size: 13px; }.habit-calendar :deep(.el-calendar-day) { height: 62px; padding: 4px; }.calendar-day { display: flex; width: 100%; height: 100%; align-items: flex-start; justify-content: space-between; border: 0; border-radius: 4px; background: transparent; color: inherit; cursor: pointer; padding: 6px; }.calendar-day:hover:not(:disabled) { background: var(--el-fill-color-light); }.calendar-day.checked { background: var(--el-color-primary-light-8); color: var(--el-color-primary); }.calendar-day.future { color: var(--el-text-color-disabled); cursor: not-allowed; }@media (max-width: 720px) { .page-head, .habit-toolbar, .habit-actions { align-items: flex-start; flex-wrap: wrap; }.list-row { align-items: flex-start; }.habit-calendar :deep(.el-calendar-day) { height: 48px; } }
</style>
