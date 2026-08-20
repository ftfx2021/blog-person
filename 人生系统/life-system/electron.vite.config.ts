import { resolve } from "node:path";
import { defineConfig, externalizeDepsPlugin } from "electron-vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  main: {
    plugins: [externalizeDepsPlugin()],
    build: {
      minify: false,
      rollupOptions: {
        input: resolve(__dirname, "src/main/index.ts"),
      },
    },
  },
  preload: {
    plugins: [externalizeDepsPlugin()],
    build: {
      minify: false,
      rollupOptions: {
        input: resolve(__dirname, "src/preload/index.ts"),
      },
    },
  },
  renderer: {
    resolve: {
      alias: {
        "@renderer": resolve(__dirname, "src/renderer"),
        "@shared": resolve(__dirname, "src/shared"),
      },
    },
    plugins: [vue()],
    build: {
      minify: false,
      rollupOptions: {
        input: resolve(__dirname, "src/renderer/index.html"),
      },
    },
  },
});
