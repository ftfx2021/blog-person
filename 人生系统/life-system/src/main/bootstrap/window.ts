import { join } from "node:path";
import { BrowserWindow, shell } from "electron";

export function createMainWindow(): BrowserWindow {
  const window = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 1024,
    minHeight: 680,
    show: false,
    backgroundColor: "#f6f7f8",
    webPreferences: {
      preload: join(__dirname, "../preload/index.js"),
      contextIsolation: true,
      sandbox: true,
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

  window.once("ready-to-show", () => window.show());

  if (process.env.ELECTRON_RENDERER_URL) {
    void window.loadURL(process.env.ELECTRON_RENDERER_URL);
  } else {
    void window.loadFile(join(__dirname, "../renderer/index.html"));
  }

  return window;
}
