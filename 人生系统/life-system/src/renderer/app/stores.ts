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
