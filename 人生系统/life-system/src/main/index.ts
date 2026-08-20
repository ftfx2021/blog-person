import { app } from "electron";
import { registerApplicationLifecycle } from "./bootstrap/lifecycle.js";

registerApplicationLifecycle(app);
// 主进程入口只注册生命周期，具体业务由模块服务负责。
