import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "./shared/styles.css";
import App from "./app/App.vue";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { router } from "./app/router";

// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
createApp(App).use(createPinia()).use(router).use(ElementPlus).mount("#app");
