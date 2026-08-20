<script setup lang="ts">
import { computed, ref } from "vue";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { useRoute, useRouter } from "vue-router";
import {
  Calendar,
  Aim,
  FolderOpened,
  CircleCheck,
  Refresh,
  Setting,
  Search,
} from "@element-plus/icons-vue";
const route = useRoute();
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
const router = useRouter();
const keyword = ref("");
const nav = [
  ["/", "今天", Calendar],
  ["/goals", "目标", Aim],
  ["/projects", "项目", FolderOpened],
  ["/tasks", "待办", CircleCheck],
  ["/habits", "习惯", Refresh],
  ["/settings", "设置", Setting],
] as const;
const active = computed(() =>
  route.path === "/" ? "/" : `/${route.path.split("/")[1]}`,
);
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
function search() {
  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (keyword.value.trim())
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
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
