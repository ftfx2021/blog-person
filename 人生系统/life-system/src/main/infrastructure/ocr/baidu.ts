import { createHash } from "node:crypto";

let cachedToken: { value: string; expiresAt: number } | undefined;

// 百度 OCR 使用 client_credentials 换取短期 access token，缓存可减少令牌请求。
async function accessToken(apiKey: string, secretKey: string): Promise<string> {
  if (cachedToken && cachedToken.expiresAt > Date.now() + 60_000)
    return cachedToken.value;
  const params = new URLSearchParams({
    grant_type: "client_credentials",
    client_id: apiKey,
    client_secret: secretKey,
  });
  const response = await fetch(
    `https://aip.baidubce.com/oauth/2.0/token?${params.toString()}`,
    { method: "POST", signal: AbortSignal.timeout(30_000) },
  );
  const payload = await response.json() as { access_token?: string; expires_in?: number; error_description?: string };
  if (!response.ok || !payload.access_token)
    throw new Error(`百度 OCR 鉴权失败: ${payload.error_description ?? response.status}`);
  cachedToken = {
    value: payload.access_token,
    expiresAt: Date.now() + (payload.expires_in ?? 2_592_000) * 1000,
  };
  return cachedToken.value;
}

// 调用通用文字识别高精度版；每页独立请求使失败可被调度器安全降级。
export async function recognizeBaiduPage(
  image: Buffer,
  credentials: { apiKey: string; secretKey: string },
): Promise<string> {
  const token = await accessToken(credentials.apiKey, credentials.secretKey);
  const body = new URLSearchParams({ image: image.toString("base64") });
  const response = await fetch(
    `https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token=${encodeURIComponent(token)}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
      signal: AbortSignal.timeout(30_000),
    },
  );
  const payload = await response.json() as {
    words_result?: Array<{ words?: string }>;
    error_msg?: string;
  };
  if (!response.ok || payload.error_msg)
    throw new Error(`百度 OCR 请求失败: ${payload.error_msg ?? response.status}`);
  const text = (payload.words_result ?? []).map((line) => line.words ?? "").join("\n").trim();
  if (!text) throw new Error("百度 OCR 未识别到文字");
  // 哈希仅用于确保 image 已经在请求体中使用，绝不记录正文或凭据。
  void createHash("sha256").update(image).digest("hex");
  return text;
}
