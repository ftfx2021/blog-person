import { join } from "node:path";
import { BrowserWindow, shell } from "electron";

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function createMainWindow(): BrowserWindow {
  // 保存当前业务事实或派生值，后续逻辑据此完成校验和状态更新。
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
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (url.startsWith("https://")) void shell.openExternal(url);
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    return { action: "deny" };
  });

  window.webContents.on("will-navigate", (event, url) => {
    const developmentUrl = process.env.ELECTRON_RENDERER_URL;
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (!developmentUrl || !url.startsWith(developmentUrl))
      event.preventDefault();
  });

  window.once("ready-to-show", () => window.show());

  // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
  if (process.env.ELECTRON_RENDERER_URL) {
    void window.loadURL(process.env.ELECTRON_RENDERER_URL);
  } else {
    void window.loadFile(join(__dirname, "../renderer/index.html"));
  }

  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return window;
}
