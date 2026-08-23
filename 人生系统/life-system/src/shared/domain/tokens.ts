// 共享 token 估算，聊天上下文和知识库分块必须使用同一套口径。
export function estimateTokens(text: string): number {
  const cjk = (text.match(/[\u3400-\u9fff]/g) ?? []).length;
  const other = text.replace(/[\u3400-\u9fff]/g, "").length;
  return Math.max(1, cjk + Math.ceil(other / 4));
}

// 在预算内截断，并优先落在段落或空白边界，避免切断单词。
export function truncateToTokens(text: string, budget: number): string {
  if (estimateTokens(text) <= budget) return text;
  const candidate = text.slice(0, Math.max(1, budget * 4));
  const boundary = Math.max(candidate.lastIndexOf("\n"), candidate.lastIndexOf(" "));
  return candidate.slice(0, boundary > candidate.length * 0.6 ? boundary : candidate.length).trimEnd();
}
