import { app } from "electron";
import { existsSync } from "node:fs";
import { copyFile, mkdir } from "node:fs/promises";
import { join } from "node:path";
import { createWorker } from "tesseract.js";

// 本地语言包必须来自应用资源或依赖目录，不允许在用户运行时下载。
async function localLanguagePath(): Promise<string> {
  // 语言数据无论开发或打包都复制到同一受控目录，Tesseract 因而只会读取本地文件。
  const development = join(app.getPath("userData"), "data", "ocr-lang");
  const sourceRoot = app.isPackaged
    ? join(process.resourcesPath, "ocr-data")
    : join(app.getAppPath(), "node_modules", "@tesseract.js-data");
  const chiSource = join(sourceRoot, "chi_sim", "4.0.0", "chi_sim.traineddata.gz");
  const engSource = join(sourceRoot, "eng", "4.0.0", "eng.traineddata.gz");
  if (!existsSync(chiSource) || !existsSync(engSource))
    throw new Error("未找到离线 OCR 语言包（chi_sim、eng），请重新安装依赖");
  await mkdir(development, { recursive: true });
  const chiTarget = join(development, "chi_sim.traineddata.gz");
  const engTarget = join(development, "eng.traineddata.gz");
  if (!existsSync(chiTarget)) await copyFile(chiSource, chiTarget);
  if (!existsSync(engTarget)) await copyFile(engSource, engTarget);
  return development;
}

// 使用一个 worker 串行处理所有页，避免多份 WASM 核心并发占用大量内存。
export async function recognizeTesseractPages(images: Buffer[]): Promise<string> {
  const worker = await createWorker(["chi_sim", "eng"], 1, {
    langPath: await localLanguagePath(),
    cacheMethod: "none",
  });
  try {
    const pages: string[] = [];
    for (const image of images) {
      const result = await worker.recognize(image);
      if (result.data.text.trim()) pages.push(result.data.text.trim());
    }
    const text = pages.join("\n\n").trim();
    if (!text) throw new Error("本地 OCR 未识别到文字");
    return text;
  } finally {
    // 无论识别成功或失败都终止 worker，防止 Electron 主进程保留 WASM 线程。
    await worker.terminate();
  }
}
