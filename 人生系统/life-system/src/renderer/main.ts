import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "./shared/styles.css";
import App from "./app/App.vue";
import { router } from "./app/router";

// 挂载前注册状态、路由和组件库，保证首屏组件依赖完整。
createApp(App).use(createPinia()).use(router).use(ElementPlus).mount("#app");
