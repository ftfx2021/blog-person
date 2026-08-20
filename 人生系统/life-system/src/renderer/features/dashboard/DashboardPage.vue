<script setup lang="ts">
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
async function load() {
  loading.value = true;
  try {
    data.value = await call(() =>
      window.lifeSystem.dashboard.get({ today: localToday() }),
    );
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function task(item: any) {
  await call(() =>
    window.lifeSystem.tasks.transition({ id: item.id, action: "advance" }),
  );
  await load();
}
async function habit(item: any) {
  const today = localToday();
  await call(() =>
    window.lifeSystem.habits.checkin({ id: item.id, checkedOn: today, today }),
  );
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
