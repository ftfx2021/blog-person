import { contextBridge } from "electron";
import { lifeSystemApi } from "./api/index.js";

contextBridge.exposeInMainWorld("lifeSystem", lifeSystemApi);
// Preload 只暴露经过校验的领域方法，不泄露 ipcRenderer 或任意通道。
