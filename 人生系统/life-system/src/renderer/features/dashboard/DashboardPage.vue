<script setup lang="ts">
import { onMounted, ref } from "vue";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { useRouter } from "vue-router";
import { Plus, Right, Check } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { localToday, useApi } from "../../shared/api";
const { call } = useApi();
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
const router = useRouter();
// 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
const data = ref<any>(),
  loading = ref(true),
  error = ref("");
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function load() {
  loading.value = true;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    data.value = await call(() =>
      // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
      window.lifeSystem.dashboard.get({ today: localToday() }),
    );
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function task(item: any) {
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    window.lifeSystem.tasks.transition({ id: item.id, action: "advance" }),
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await load();
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function habit(item: any) {
  // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
  const today = localToday();
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await call(() =>
    // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
    window.lifeSystem.habits.checkin({ id: item.id, checkedOn: today, today }),
  );
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  await load();
}
onMounted(load);
</script>
<template>
  <div class="page-head">
    <div>
      <h1>今天</h1>
      <p>
        {{
          new Date().toLocaleDateString("zh-CN", {
            year: "numeric",
            month: "long",
            day: "numeric",
            weekday: "long",
          })
        }}
      </p>
    </div>
    <el-button type="primary" :icon="Plus" @click="router.push('/tasks')"
      >新建待办</el-button
    >
  </div>
  <PageState :loading="loading" :error="error" :empty="!data" @retry="load"
    ><div v-if="data" class="dashboard-grid">
      <div>
        <div class="stats">
          <div class="stat">
            <strong>{{ data.summary.taskCount }}</strong
            ><span>今日行动</span>
          </div>
          <div class="stat">
            <strong
              >{{ data.summary.habitDone }} /
              {{ data.summary.habitCount }}</strong
            ><span>习惯打卡</span>
          </div>
        </div>
        <section class="section">
          <h2 class="section-title">待办行动</h2>
          <div v-if="data.tasks.length" class="list">
            <div v-for="item in data.tasks" :key="item.id" class="list-row">
              <div class="list-main">
                <div class="list-title">{{ item.title }}</div>
                <div class="list-meta">
                  {{ item.status === "todo" ? "待处理" : "进行中" }} ·
                  {{
                    item.dueDate
                      ? new Date(item.dueDate).toLocaleString()
                      : "无截止时间"
                  }}
                </div>
              </div>
              <el-button size="small" :icon="Right" @click="task(item)">{{
                item.status === "todo" ? "开始" : "完成"
              }}</el-button>
            </div>
          </div>
          <div v-else class="empty-inline">
            今天没有待办，可以留一点空间给自己。
          </div>
        </section>
        <section class="section">
          <h2 class="section-title">习惯</h2>
          <div v-if="data.habits.length" class="list">
            <div v-for="item in data.habits" :key="item.id" class="list-row">
              <div class="list-main">
                <div class="list-title">{{ item.name }}</div>
                <div class="list-meta">
                  连续 {{ item.streak }}
                  {{ item.frequencyType === "daily" ? "天" : "周" }}
                </div>
              </div>
              <el-button
                size="small"
                :type="item.checkedToday ? 'success' : 'primary'"
                :icon="Check"
                :disabled="Boolean(item.checkedToday)"
                @click="habit(item)"
                >{{ item.checkedToday ? "已打卡" : "打卡" }}</el-button
              >
            </div>
          </div>
          <div v-else class="empty-inline">还没有习惯。</div>
        </section>
      </div>
      <aside>
        <div class="panel">
          <h2>目标摘要</h2>
          <div v-if="data.goals.length">
            <div
              v-for="goal in data.goals"
              :key="goal.id"
              class="list-row"
              style="padding-left: 0; padding-right: 0"
            >
              <div class="list-main">
                <div class="list-title">{{ goal.title }}</div>
                <el-progress
                  v-if="goal.progress != null"
                  :percentage="goal.progress"
                  :show-text="false"
                  :stroke-width="5"
                />
                <div v-else class="list-meta">
                  {{ goal.metricType === "status" ? "状态型" : "尚未记录" }}
                </div>
              </div>
              <el-button
                text
                :icon="Right"
                @click="router.push(`/goals/${goal.id}`)"
              />
            </div>
          </div>
          <div v-else class="empty-inline">暂无进行中目标。</div>
        </div>
        <div class="panel" style="margin-top: 18px">
          <h2>站内提醒</h2>
          <div v-if="data.reminders.length">
            <div
              v-for="item in data.reminders"
              :key="`${item.type}-${item.entityId}`"
              class="list-row"
              style="padding-left: 0; padding-right: 0"
            >
              <div class="list-main">
                <div class="list-title">{{ item.title }}</div>
              </div>
            </div>
          </div>
          <div v-else class="empty-inline">暂无提醒。</div>
        </div>
      </aside>
    </div></PageState
  >
</template>
