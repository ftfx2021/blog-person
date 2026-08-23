import { createHash } from "node:crypto";

// 规范化只清理运输层噪声，不摘要、改写或补写用户原文。
export function normalizeDocument(rawText: string): {
  rawText: string;
  contentHash: string;
} {
  // 统一换行和不可见控制字符，确保相同原文重复入库得到相同哈希。
  const withoutControls = [...rawText]
    .filter((character) => {
      // 保留换行和制表以维持 Markdown 结构，其余 C0 控制字符没有正文语义。
      const code = character.charCodeAt(0);
      return code === 9 || code === 10 || code >= 32;
    })
    .join("");
  const normalized = withoutControls
    .replace(/\r\n?/g, "\n")
    .split("\n")
    .map((line) => line.replace(/[ \t]+$/g, ""))
    .join("\n")
    .replace(/\n{4,}/g, "\n\n\n")
    .trim();
  // 空文本没有索引价值，必须作为失败保留元数据壳。
  if (!normalized) throw new Error("规范化后正文为空");
  return {
    rawText: normalized,
    contentHash: createHash("sha256").update(normalized).digest("hex"),
  };
}
