import { createHmac, randomUUID } from "node:crypto";

function encode(value: string): string {
  return encodeURIComponent(value).replace(/[!'()*]/g, (character) => `%${character.charCodeAt(0).toString(16).toUpperCase()}`);
}

// 阿里云 RPC 请求通过排序参数和 HMAC-SHA1 生成签名，不引入任何云 SDK。
export async function recognizeAliyunPage(
  image: Buffer,
  credentials: { accessKeyId: string; accessKeySecret: string; endpoint: string },
): Promise<string> {
  const timestamp = new Date().toISOString().replace(/\.\d{3}Z$/, "Z");
  const parameters: Record<string, string> = {
    Action: "RecognizeGeneral",
    Version: "2021-07-07",
    Format: "JSON",
    AccessKeyId: credentials.accessKeyId,
    SignatureMethod: "HMAC-SHA1",
    Timestamp: timestamp,
    SignatureVersion: "1.0",
    SignatureNonce: randomUUID(),
  };
  const canonicalized = Object.entries(parameters).sort(([left], [right]) => left.localeCompare(right)).map(([key, value]) => `${encode(key)}=${encode(value)}`).join("&");
  const stringToSign = `POST&%2F&${encode(canonicalized)}`;
  parameters.Signature = createHmac("sha1", `${credentials.accessKeySecret}&`).update(stringToSign).digest("base64");
  const response = await fetch(`https://${credentials.endpoint}/?${new URLSearchParams(parameters).toString()}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ Body: image.toString("base64") }),
    signal: AbortSignal.timeout(30_000),
  });
  const data = await response.json() as { Data?: Array<{ text?: string }> | { text?: string }; Message?: string };
  if (!response.ok || data.Message) throw new Error(`阿里 OCR 请求失败: ${data.Message ?? response.status}`);
  const entries = Array.isArray(data.Data) ? data.Data : data.Data ? [data.Data] : [];
  const text = entries.map((entry) => entry.text ?? "").join("\n").trim();
  if (!text) throw new Error("阿里 OCR 未识别到文字");
  return text;
}
