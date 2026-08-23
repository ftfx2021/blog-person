import { createRouter, createWebHashHistory } from "vue-router";
import DashboardPage from "../features/dashboard/DashboardPage.vue";
import GoalsPage from "../features/goals/GoalsPage.vue";
import GoalDetailPage from "../features/goals/GoalDetailPage.vue";
import ProjectsPage from "../features/projects/ProjectsPage.vue";
import TasksPage from "../features/tasks/TasksPage.vue";
import HabitsPage from "../features/habits/HabitsPage.vue";
import SettingsPage from "../features/settings/SettingsPage.vue";
import SearchPage from "../features/search/SearchPage.vue";
import AssistantPage from "../features/assistant/AssistantPage.vue";

// 收藏箱按 feature 懒加载，避免未访问知识管理时占用首屏脚本解析时间。
const InboxPage = () => import("../features/inbox/InboxPage.vue");
const KnowledgePage = () => import("../features/knowledge/KnowledgePage.vue");
const KnowledgeDetailPage = () => import("../features/knowledge/KnowledgeDetailPage.vue");
const KnowledgeTagsPage = () => import("../features/knowledge/KnowledgeTagsPage.vue");

// hash 历史兼容 Electron file 协议，刷新时不依赖后端路由回退。
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", name: "dashboard", component: DashboardPage },
    { path: "/goals", name: "goals", component: GoalsPage },
    { path: "/goals/:id", name: "goal-detail", component: GoalDetailPage },
    { path: "/projects", name: "projects", component: ProjectsPage },
    { path: "/tasks", name: "tasks", component: TasksPage },
    { path: "/habits", name: "habits", component: HabitsPage },
    { path: "/settings", name: "settings", component: SettingsPage },
    { path: "/search", name: "search", component: SearchPage },
    { path: "/assistant", name: "assistant", component: AssistantPage },
    { path: "/inbox", name: "inbox", component: InboxPage },
    { path: "/knowledge", name: "knowledge", component: KnowledgePage },
    { path: "/knowledge/tags", name: "knowledge-tags", component: KnowledgeTagsPage },
    { path: "/knowledge/:id", name: "knowledge-detail", component: KnowledgeDetailPage },
  ],
});
