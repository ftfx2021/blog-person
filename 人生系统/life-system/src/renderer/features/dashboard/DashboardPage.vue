<script setup lang="ts">
// 仪表盘仅编排聚合数据和快捷操作，统计口径由 dashboard 服务统一维护。
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Plus, Right, Check } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { localToday, useApi } from "../../shared/api";
const { call } = useApi();
const router = useRouter();
const data = ref<any>(),
  loading = ref(true),
  error = ref("");
async function load(silent = false) {
  // 仪表盘使用一个聚合接口，保证任务、习惯、目标和提醒来自同一日期快照。
  // localToday 按本地日历生成，避免 UTC 转换让凌晨用户看到错误的今日行动。
  // loading/error 状态交给 PageState，页面函数不复制一套空态渲染逻辑。
  // 失败保留 error 供用户重试，而不是用空数组掩盖数据库不可用。
  // finally 无论结果如何都结束加载状态，避免界面永久显示骨架屏。
  // 聚合接口一次返回首屏所需数据，减少多个请求造成的闪烁。
  loading.value = true;
  try {
    data.value = await call(() =>
      window.lifeSystem.dashboard.get({ today: localToday() }),
      { silent },
    );
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function task(item: any) {
  // 快捷任务按钮提交推进动作，不在客户端直接修改 status。
  // 服务端状态机决定 todo->doing 或 doing->done，非法状态会返回错误。
  // 操作成功后重新加载聚合数据，任务计数和提醒同时更新。
  // 失败时不修改本地 item，避免界面显示与数据库不一致。
  // 快捷按钮始终请求“推进”而非本地改状态，确保仍受待办状态机约束。
  await call(() =>
    window.lifeSystem.tasks.transition({ id: item.id, action: "advance" }),
  );
  await load();
}
async function habit(item: any) {
  // 仪表盘打卡只针对尚未完成的今日习惯，按钮禁用状态仅是体验保护。
  // checkedOn 与 today 使用同一本地日期，服务端仍会再次拒绝未来日期。
  // 成功后刷新聚合数据，使 streak、完成数量和提醒同步变化。
  // 失败不篡改 checkedToday，用户可修复数据库后重试。
  // 仪表盘打卡使用同一个本地日期作为 checkedOn 与 today，阻止跨日界面的歧义提交。
  const today = localToday();
  await call(() =>
    window.lifeSystem.habits.checkin({ id: item.id, checkedOn: today, today }),
  );
  await load();
}
// 首屏挂载时拉取单一聚合接口，避免多个卡片分别完成造成数据时间不一致。
// 首屏数据库未配置时静默跳转设置页，避免 Dashboard 与设置页各自弹出重复错误。
onMounted(() => load(true));
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
  <PageState :loading="loading" :error="error" :empty="!data" @retry="() => load()"
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
