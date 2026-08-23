import { createHash } from "node:crypto";
import { estimateTokens } from "../../../shared/domain/tokens.js";

export interface ChunkOutput {
  seqNo: number;
  content: string;
  tokenCount: number;
  metadata: {
    documentTitle: string;
    docType: string;
    sourceUrl: string | null;
    sourcePath: string | null;
    headingPath: string[];
    pageStart: number | null;
    pageEnd: number | null;
    charStart: number;
    charEnd: number;
  };
  contentHash: string;
}

interface Paragraph {
  content: string;
  charStart: number;
}

// 将原文按标题和空行切为结构单元，绝不以固定字符位置切词。
function paragraphsOf(rawText: string): Paragraph[] {
  const paragraphs: Paragraph[] = [];
  let cursor = 0;
  for (const part of rawText.split(/\n{2,}/)) {
    const content = part.trim();
    const charStart = rawText.indexOf(content, cursor);
    cursor = Math.max(cursor, charStart + content.length);
    if (content) paragraphs.push({ content, charStart });
  }
  return paragraphs;
}

// 以段落为粒度生成 400-700 token 块；不可分结构允许单块超限。
export function chunkDocument(
  rawText: string,
  input: {
    documentTitle: string;
    docType: string;
    sourceUrl?: string | null;
    sourcePath?: string | null;
    headingPath?: string[];
    pageLocations?: { pageStart?: number; pageEnd?: number }[];
  },
): ChunkOutput[] {
  const outputs: ChunkOutput[] = [];
  const paragraphs = paragraphsOf(rawText);
  let bucket: Paragraph[] = [];
  let bucketTokens = 0;
  let sequence = 0;
  const flush = () => {
    if (!bucket.length) return;
    const content = bucket.map((part) => part.content).join("\n\n");
    const charStart = bucket[0]!.charStart;
    const charEnd = charStart + content.length;
    const metadata = {
      documentTitle: input.documentTitle,
      docType: input.docType,
      sourceUrl: input.sourceUrl ?? null,
      sourcePath: input.sourcePath ?? null,
      headingPath: input.headingPath ?? [],
      pageStart: input.pageLocations?.[sequence]?.pageStart ?? null,
      pageEnd: input.pageLocations?.[sequence]?.pageEnd ?? null,
      charStart,
      charEnd,
    };
    outputs.push({
      seqNo: sequence,
      content,
      tokenCount: estimateTokens(content),
      metadata,
      contentHash: createHash("sha256")
        .update(`${content}|${charStart}|${charEnd}|${JSON.stringify(metadata.headingPath)}`)
        .digest("hex"),
    });
    sequence += 1;
    bucket = [];
    bucketTokens = 0;
  };
  for (const paragraph of paragraphs) {
    const tokenCount = estimateTokens(paragraph.content);
    // 标题开始新节，防止相邻章节拼入一个上下文块。
    if (/^#{1,6}\s/.test(paragraph.content) && bucket.length) flush();
    // 引用、表格和代码块不在结构内部截断。
    const isAtomicStructure = /^(>|\|.*\||```)/.test(paragraph.content);
    if (bucket.length && bucketTokens + tokenCount > 700) flush();
    bucket.push(paragraph);
    bucketTokens += tokenCount;
    if (bucketTokens >= 400 && !isAtomicStructure) flush();
  }
  flush();
  if (!outputs.length) throw new Error("正文无法生成有效分块");
  return outputs;
}
