<script setup lang="ts">
// 根布局只维护导航与全局搜索状态，不在这一层承载任何实体数据或业务请求。
// 路由视图位于内容区，使侧栏在页面切换时保持稳定且可预测。
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Calendar,
  Aim,
  FolderOpened,
  CircleCheck,
  Refresh,
  Setting,
  Search,
  ChatDotRound,
  CollectionTag,
  Reading,
} from "@element-plus/icons-vue";
const route = useRoute();
const router = useRouter();
const keyword = ref("");
// 启动时尽早检查 preload 暴露面，避免页面点击后才出现难以定位的 undefined 异常。
if (!window.lifeSystem) {
  console.error("lifeSystem preload API 未加载，请检查 Electron preload 配置");
}
const nav = [
  ["/", "今天", Calendar],
  ["/goals", "目标", Aim],
  ["/projects", "项目", FolderOpened],
  ["/tasks", "待办", CircleCheck],
  ["/habits", "习惯", Refresh],
  ["/settings", "设置", Setting],
  ["/assistant", "AI 助手", ChatDotRound],
  ["/inbox", "收藏箱", CollectionTag],
  ["/knowledge", "知识库", Reading],
] as const;
// 详情路由归属一级模块，导航高亮按路径首段计算而不是要求逐条列出动态路由。
const active = computed(() =>
  route.path === "/" ? "/" : `/${route.path.split("/")[1]}`,
);
function search() {
  // 忽略纯空白搜索，提交时将关键词编码为路由查询参数以支持刷新和分享入口。
  if (keyword.value.trim())
    router.push({ path: "/search", query: { q: keyword.value.trim() } });
}
</script>
<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">生</span>
        <div><strong>人生系统</strong><small>目标与行动</small></div>
      </div>
      <nav>
        <button
          v-for="[path, label, icon] in nav"
          :key="path"
          :class="['nav-button', { active: active === path }]"
          @click="router.push(path)"
        >
          <el-icon><component :is="icon" /></el-icon><span>{{ label }}</span>
        </button>
      </nav>
      <div class="sidebar-foot"><span class="status-dot"></span>本机桌面端</div>
    </aside>
    <section class="workspace">
      <header class="topbar">
        <div class="global-search">
          <el-icon><Search /></el-icon
          ><input
            v-model="keyword"
            placeholder="搜索目标、项目、待办、习惯"
            @keyup.enter="search"
          />
        </div>
        <el-button
          :icon="Setting"
          circle
          title="设置"
          @click="router.push('/settings')"
        />
      </header>
      <main class="page"><router-view /></main>
    </section>
  </div>
</template>
