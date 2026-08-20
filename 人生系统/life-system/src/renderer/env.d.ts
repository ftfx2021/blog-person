import type { LifeSystemApi } from "../preload/api/index.js";

declare global {
  interface Window {
    lifeSystem: LifeSystemApi;
  }
}

// P1 空壳只保留目录边界，避免未实现能力被 P0 页面误用。
export {};
