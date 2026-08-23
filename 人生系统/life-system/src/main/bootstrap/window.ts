import { join } from "node:path";
import { app, BrowserWindow, shell } from "electron";

export function createMainWindow(): BrowserWindow {
  const window = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 1024,
    minHeight: 680,
    show: false,
    backgroundColor: "#f6f7f8",
    webPreferences: {
      // electron-vite 的 ESM preload 产物为 index.mjs；路径错误会导致整个 lifeSystem API 消失。
      preload: join(__dirname, "../preload/index.mjs"),
      contextIsolation: true,
      // electron-vite 生成 ESM preload；Electron 沙箱渲染器可能拒绝加载 ESM preload。
      // 继续保留 contextIsolation 和 nodeIntegration=false，页面仍不能直接访问主进程能力。
      sandbox: false,
      nodeIntegration: false,
      webSecurity: true,
    },
  });

  // 禁止页面创建额外窗口；外部链接只交给系统浏览器。
  window.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith("https://")) void shell.openExternal(url);
    return { action: "deny" };
  });

  window.webContents.on("will-navigate", (event, url) => {
    const developmentUrl = process.env.ELECTRON_RENDERER_URL;
    if (!developmentUrl || !url.startsWith(developmentUrl))
      event.preventDefault();
  });

  // preload 加载失败时记录明确路径和异常，避免渲染层只看到 window.lifeSystem 未定义。
  window.webContents.on("preload-error", (_event, preloadPath, error) => {
    console.error(`Preload 加载失败：${preloadPath}`, error);
  });

  if (!app.isPackaged) {
    // 开发环境直接探测 bridge，确认 Electron 窗口不是误以浏览器页面方式加载。
    window.webContents.once("did-finish-load", () => {
      void window.webContents
        .executeJavaScript("typeof window.lifeSystem")
        .then((value) => console.info(`Preload API 状态：${value}`));
    });
  }

  window.once("ready-to-show", () => window.show());

  if (process.env.ELECTRON_RENDERER_URL) {
    void window.loadURL(process.env.ELECTRON_RENDERER_URL);
  } else {
    void window.loadFile(join(__dirname, "../renderer/index.html"));
  }

  return window;
}
