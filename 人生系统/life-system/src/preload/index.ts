import { contextBridge } from "electron";
import { lifeSystemApi } from "./api/index.js";

contextBridge.exposeInMainWorld("lifeSystem", lifeSystemApi);
