import { readFile } from "node:fs/promises";
import { unified } from "unified";
import remarkParse from "remark-parse";
import * as cheerio from "cheerio";
import TurndownService from "turndown";
import mammoth from "mammoth";
import { getDocument } from "pdfjs-dist/legacy/build/pdf.mjs";


export interface ParseResult { rawText: string; pageLocations?: { pageStart?: number; pageEnd?: number }[]; headingPath?: string[]; }
export interface DocumentParser { parse(storedPath: string): Promise<ParseResult>; }

// 统一 TXT 解码，显式支持 UTF-8、UTF-16LE 和 UTF-16BE BOM。
function decodeText(buffer: Buffer): string {
  if (buffer[0] === 0xff && buffer[1] === 0xfe) return buffer.subarray(2).toString("utf16le");
  if (buffer[0] === 0xfe && buffer[1] === 0xff) {
    const swapped = Buffer.from(buffer.subarray(2));
    for (let index = 0; index + 1 < swapped.length; index += 2) {
      // UTF-16BE 需要交换两个字节后才能复用 Node 的 utf16le 解码器。
      const value = swapped[index]!;
      swapped[index] = swapped[index + 1]!;
      swapped[index + 1] = value;
    }
    return swapped.toString("utf16le");
  }
  return buffer.subarray(buffer[0] === 0xef && buffer[1] === 0xbb && buffer[2] === 0xbf ? 3 : 0).toString("utf8");
}
class TxtParser implements DocumentParser { async parse(path: string): Promise<ParseResult> { const rawText = decodeText(await readFile(path)); if (!rawText.trim()) throw new Error("文本正文为空"); return { rawText }; } }
class MarkdownParser implements DocumentParser { async parse(path: string): Promise<ParseResult> { const rawText = decodeText(await readFile(path)); const tree = unified().use(remarkParse).parse(rawText) as any; const headingPath = (tree.children ?? []).filter((node: any) => node.type === "heading").map((node: any) => node.children.map((child: any) => child.value ?? "").join("").trim()).filter(Boolean); if (!rawText.trim()) throw new Error("Markdown 正文为空"); return { rawText, headingPath }; } }
class HtmlParser implements DocumentParser { async parse(path: string): Promise<ParseResult> { const $ = cheerio.load(decodeText(await readFile(path))); $("script,style,nav,header,footer,aside,form,[aria-hidden='true']").remove(); const rawText = new TurndownService({ headingStyle: "atx", codeBlockStyle: "fenced" }).turndown($("body").html() || $.html()); if (!rawText.trim()) throw new Error("HTML 正文为空"); return { rawText, headingPath: $("h1,h2,h3,h4,h5,h6").toArray().map((item) => $(item).text().trim()).filter(Boolean) }; } }
class DocxParser implements DocumentParser { async parse(path: string): Promise<ParseResult> { const result = await mammoth.convertToHtml({ path }); const $ = cheerio.load(result.value); const rawText = new TurndownService({ headingStyle: "atx", codeBlockStyle: "fenced" }).turndown(result.value); if (!rawText.trim()) throw new Error("DOCX 正文为空"); return { rawText, headingPath: $("h1,h2,h3,h4,h5,h6").toArray().map((item) => $(item).text().trim()).filter(Boolean) }; } }
class PdfParser implements DocumentParser { async parse(path: string): Promise<ParseResult> { const file = await readFile(path); // pdfjs-dist 只接受 Uint8Array，不能把 Node Buffer 直接传入。
  const data = new Uint8Array(file.buffer.slice(file.byteOffset, file.byteOffset + file.byteLength));
  const document = await getDocument({ data }).promise; const pages: string[] = []; const pageLocations: { pageStart?: number; pageEnd?: number }[] = []; for (let pageNumber = 1; pageNumber <= document.numPages; pageNumber += 1) { const page = await document.getPage(pageNumber); const content = await page.getTextContent(); const text = content.items.map((item: any) => item.str ?? "").join(" ").trim(); if (text) { pages.push(text); pageLocations.push({ pageStart: pageNumber, pageEnd: pageNumber }); } } if (!pages.length) throw Object.assign(new Error("扫描件需要 OCR"), { code: "OCR_REQUIRED" }); return { rawText: pages.join("\n\n"), pageLocations }; } }
export function createParser(docType: string): DocumentParser { if (docType === "pdf") return new PdfParser(); if (docType === "docx") return new DocxParser(); if (docType === "markdown") return new MarkdownParser(); if (docType === "html" || docType === "webpage") return new HtmlParser(); if (docType === "txt") return new TxtParser(); throw Object.assign(new Error(`不支持的文档类型: ${docType}`), { code: "VALIDATION_ERROR" }); }
export function docTypeFromPath(path: string): string { const extension = path.toLowerCase().slice(path.lastIndexOf(".")); const value: Record<string, string> = { ".pdf": "pdf", ".docx": "docx", ".md": "markdown", ".markdown": "markdown", ".html": "html", ".htm": "html", ".txt": "txt" }; if (!value[extension]) throw Object.assign(new Error("仅支持 PDF、DOCX、Markdown、HTML 和 TXT 文件"), { code: "VALIDATION_ERROR" }); return value[extension]; }
