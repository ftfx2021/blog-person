import { defineStore } from "pinia";

export const useSessionStore = defineStore("session", {
  state: () => ({
    globalLoading: false,
    lastError: null as null | { code: string; message: string },
  }),
  actions: {
    setError(error: any) {
      this.lastError = error;
    },
    clearError() {
      this.lastError = null;
    },
  },
});
// Pinia 只保存 UI 会话状态，MySQL 仍是业务事实源。
