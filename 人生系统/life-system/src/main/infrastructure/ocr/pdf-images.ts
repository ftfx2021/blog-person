import { readFile } from "node:fs/promises";
import { Canvas } from "@napi-rs/canvas";
import { getDocument } from "pdfjs-dist/legacy/build/pdf.mjs";

// 将 PDF 逐页渲染为 PNG，OCR 与普通 PDF 文本解析共享 pdfjs 的安全解析能力。
export async function renderPdfPages(pdfPath: string): Promise<Buffer[]> {
  const file = await readFile(pdfPath);
  // pdfjs-dist 不接受 Node Buffer，复制为独立 Uint8Array 避免底层转移原始内存。
  const data = new Uint8Array(
    file.buffer.slice(file.byteOffset, file.byteOffset + file.byteLength),
  );
  const document = await getDocument({ data }).promise;
  const pages: Buffer[] = [];
  try {
    for (let pageNumber = 1; pageNumber <= document.numPages; pageNumber += 1) {
      // scale=2 在文字可识别性和在线服务的图片大小限制之间取得平衡。
      const page = await document.getPage(pageNumber);
      const viewport = page.getViewport({ scale: 2 });
      const canvas = new Canvas(Math.ceil(viewport.width), Math.ceil(viewport.height));
      const context = canvas.getContext("2d") as unknown as CanvasRenderingContext2D;
      // pdfjs 6 的类型要求同时提供 canvas 与 context，实际渲染仍在 napi canvas 上执行。
      await page.render({ canvas: canvas as any, canvasContext: context, viewport }).promise;
      pages.push(canvas.toBuffer("image/png"));
    }
  } finally {
    // 不保留 PDF 文档实例，避免批量 OCR 时累积页面资源。
    document.cleanup();
  }
  if (!pages.length) throw new Error("PDF 没有可供 OCR 的页面");
  return pages;
}
