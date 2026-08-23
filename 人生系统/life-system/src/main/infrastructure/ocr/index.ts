import { recognizeAliyunPage } from "./aliyun.js";
import { recognizeBaiduPage } from "./baidu.js";
import { renderPdfPages } from "./pdf-images.js";
import { recognizeTencentPage } from "./tencent.js";
import { recognizeTesseractPages } from "./tesseract.js";
import type { OcrResult, OcrSettings } from "./types.js";

export type { OcrResult, OcrSettings } from "./types.js";

// 在线服务按锁定顺序尝试；某个服务不可用时继续降级，不暴露凭据到错误文本。
export async function recognizePdf(pdfPath: string, settings: OcrSettings): Promise<OcrResult> {
  if (settings.provider === "disabled")
    throw new Error("扫描件需启用 OCR；已尝试 OCR：disabled");
  const images = await renderPdfPages(pdfPath);
  const attempts: Array<{ provider: OcrResult["provider"]; run?: () => Promise<string> }> = [
    settings.baidu ? { provider: "baidu", run: async () => (await Promise.all(images.map((image) => recognizeBaiduPage(image, settings.baidu!)))).join("\n\n") } : { provider: "baidu" },
    settings.tencent ? { provider: "tencent", run: async () => (await Promise.all(images.map((image) => recognizeTencentPage(image, settings.tencent!)))).join("\n\n") } : { provider: "tencent" },
    settings.aliyun ? { provider: "aliyun", run: async () => (await Promise.all(images.map((image) => recognizeAliyunPage(image, settings.aliyun!)))).join("\n\n") } : { provider: "aliyun" },
    { provider: "tesseract", run: () => recognizeTesseractPages(images) },
  ];
  let lastError = "未配置可用 OCR 服务";
  for (const attempt of attempts) {
    if (!attempt.run) continue;
    try {
      const text = await attempt.run();
      if (text.trim()) return { text, provider: attempt.provider };
    } catch (error) {
      // 只记录服务名称和可读错误，避免将密钥、请求 body 或识别内容写入日志。
      lastError = `${attempt.provider}: ${error instanceof Error ? error.message : "请求失败"}`;
    }
  }
  throw new Error(`已尝试 OCR：${lastError}`);
}
