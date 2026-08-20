import { createRouter, createWebHashHistory } from "vue-router";
import DashboardPage from "../features/dashboard/DashboardPage.vue";
import GoalsPage from "../features/goals/GoalsPage.vue";
import GoalDetailPage from "../features/goals/GoalDetailPage.vue";
import ProjectsPage from "../features/projects/ProjectsPage.vue";
import TasksPage from "../features/tasks/TasksPage.vue";
import HabitsPage from "../features/habits/HabitsPage.vue";
import SettingsPage from "../features/settings/SettingsPage.vue";
import SearchPage from "../features/search/SearchPage.vue";

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
  ],
});
