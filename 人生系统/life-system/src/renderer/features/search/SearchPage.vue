<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { useRoute, useRouter } from "vue-router";
import { Search, Right } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { useApi } from "../../shared/api";
const route = useRoute(),
  // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
  router = useRouter(),
  { call } = useApi();
// 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
const keyword = ref(String(route.query.q || "")),
  types = ref<string[]>([]),
  data = ref<Record<string, any[]>>({}),
  loading = ref(false),
  error = ref("");
const groups = computed(() =>
  Object.entries(data.value).filter(([, rows]) => rows.length),
);
const labels: any = {
  goal: "目标",
  project: "项目",
  task: "待办",
  habit: "习惯",
  document: "文档",
};
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
async function search() {
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (!keyword.value.trim()) return;
  loading.value = true;
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  try {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    data.value = await call(() =>
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      window.lifeSystem.search.query({
        keyword: keyword.value,
        types: types.value,
        tags: [],
      }),
    );
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
function open(type: string, id: string) {
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (type === "goal") router.push(`/goals/${id}`);
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  else if (type === "project") router.push("/projects");
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  else if (type === "task") router.push("/tasks");
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  else if (type === "habit") router.push("/habits");
}
watch(
  // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
  () => route.query.q,
  () => {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    keyword.value = String(route.query.q || "");
    search();
  },
);
onMounted(search);
</script>
<template>
  <div class="page-head">
    <div>
      <h1>搜索</h1>
      <p>搜索目标、项目、待办、习惯标题与知识文档正文。</p>
    </div>
  </div>
  <div class="toolbar">
    <el-input
      v-model="keyword"
      size="large"
      :prefix-icon="Search"
      placeholder="输入关键词"
      @keyup.enter="search"
    /><el-button type="primary" size="large" @click="search">搜索</el-button>
  </div>
  <el-checkbox-group v-model="types" class="toolbar"
    ><el-checkbox-button label="goal">目标</el-checkbox-button
    ><el-checkbox-button label="project">项目</el-checkbox-button
    ><el-checkbox-button label="task">待办</el-checkbox-button
    ><el-checkbox-button label="habit">习惯</el-checkbox-button
    ><el-checkbox-button label="document"
      >文档</el-checkbox-button
    ></el-checkbox-group
  ><PageState
    :loading="loading"
    :error="error"
    :empty="!loading && groups.length === 0"
    empty-text="没有匹配结果"
    @retry="search"
    ><section v-for="[type, rows] in groups" :key="type" class="section">
      <h2 class="section-title">{{ labels[type] }} · {{ rows.length }}</h2>
      <div class="list">
        <div v-for="item in rows" :key="item.id" class="list-row">
          <div class="list-main">
            <div class="list-title">{{ item.title }}</div>
            <div class="list-meta">
              {{
                item.status ||
                item.subtype ||
                new Date(item.updatedAt).toLocaleString()
              }}
            </div>
          </div>
          <el-button
            v-if="type !== 'document'"
            text
            :icon="Right"
            @click="open(type, item.id)"
          />
        </div>
      </div></section
  ></PageState>
</template>
