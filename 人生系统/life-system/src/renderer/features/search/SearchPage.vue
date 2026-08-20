<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Search, Right } from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { useApi } from "../../shared/api";
const route = useRoute(),
  router = useRouter(),
  { call } = useApi();
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
async function search() {
  if (!keyword.value.trim()) return;
  loading.value = true;
  try {
    data.value = await call(() =>
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
function open(type: string, id: string) {
  if (type === "goal") router.push(`/goals/${id}`);
  else if (type === "project") router.push("/projects");
  else if (type === "task") router.push("/tasks");
  else if (type === "habit") router.push("/habits");
}
watch(
  () => route.query.q,
  () => {
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
