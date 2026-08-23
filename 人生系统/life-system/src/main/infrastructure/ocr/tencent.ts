import { createHash, createHmac } from "node:crypto";

function hmac(key: string | Buffer, value: string): Buffer {
  return createHmac("sha256", key).update(value).digest();
}

// 腾讯云 OCR 的 TC3-HMAC-SHA256 签名严格按官方四步构造请求。
export async function recognizeTencentPage(
  image: Buffer,
  credentials: { secretId: string; secretKey: string; region: string },
): Promise<string> {
  const host = "ocr.tencentcloudapi.com";
  const service = "ocr";
  const version = "2018-11-19";
  const timestamp = Math.floor(Date.now() / 1000);
  const date = new Date(timestamp * 1000).toISOString().slice(0, 10);
  const payload = JSON.stringify({ ImageBase64: image.toString("base64") });
  const payloadHash = createHash("sha256").update(payload).digest("hex");
  const canonicalHeaders = `content-type:application/json; charset=utf-8\nhost:${host}\n`;
  const signedHeaders = "content-type;host";
  const canonicalRequest = `POST\n/\n\n${canonicalHeaders}\n${signedHeaders}\n${payloadHash}`;
  const scope = `${date}/${service}/tc3_request`;
  const stringToSign = `TC3-HMAC-SHA256\n${timestamp}\n${scope}\n${createHash("sha256").update(canonicalRequest).digest("hex")}`;
  const signingKey = hmac(hmac(hmac(`TC3${credentials.secretKey}`, date), service), "tc3_request");
  const signature = createHmac("sha256", signingKey).update(stringToSign).digest("hex");
  const authorization = `TC3-HMAC-SHA256 Credential=${credentials.secretId}/${scope}, SignedHeaders=${signedHeaders}, Signature=${signature}`;
  const response = await fetch(`https://${host}/`, {
    method: "POST",
    headers: {
      Authorization: authorization,
      "Content-Type": "application/json; charset=utf-8",
      Host: host,
      "X-TC-Action": "TextRecognize",
      "X-TC-Version": version,
      "X-TC-Timestamp": String(timestamp),
      "X-TC-Region": credentials.region,
    },
    body: payload,
    signal: AbortSignal.timeout(30_000),
  });
  const data = await response.json() as { Response?: { TextDetections?: Array<{ DetectedText?: string }>; Error?: { Message?: string } } };
  const error = data.Response?.Error?.Message;
  if (!response.ok || error) throw new Error(`腾讯 OCR 请求失败: ${error ?? response.status}`);
  const text = (data.Response?.TextDetections ?? []).map((line) => line.DetectedText ?? "").join("\n").trim();
  if (!text) throw new Error("腾讯 OCR 未识别到文字");
  return text;
}
