<script setup lang="ts">
// 搜索结果只展示主进程返回的统一摘要，避免渲染层拼接 SQL 条件。
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
  // 搜索先裁剪空白并阻止空关键词，避免无意义的全文或 LIKE 查询。
  // 类型筛选通过 schema 传递，服务层仍会再次使用实体白名单。
  // 搜索结果按实体分组保存，模板只负责展示，不拼接 SQL 或解析数据库字段。
  // loading/error 由页面统一控制，失败可用 PageState 重试同一条件。
  // 成功后 groups computed 自动过滤空分组，保持结果区域紧凑。
  // 空关键词不发起查询，避免无意义的全表扫描。
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
  // 结果跳转按实体类型映射到现有工作区，目标详情支持精确 ID 路由。
  // 项目、待办和习惯当前跳转列表页，避免伪造尚未实现的详情路由。
  // 文档结果没有 P0 详情页面，因此模板不显示打开按钮。
  // 任何未知类型都不执行跳转，避免路由被搜索数据驱动到任意路径。
  // 搜索结果按实体类型跳转到对应工作区；项目、待办和习惯当前使用各自列表页承接。
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
// 直接带 q 参数进入时立即搜索，使导航栏搜索与本页输入框行为一致。
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
