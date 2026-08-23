// OCR 配置在主进程内使用；密钥只会短暂存在，绝不能被回传到渲染层。
export interface OcrSettings {
  provider: "baidu" | "tencent" | "aliyun" | "tesseract" | "disabled";
  baidu?: { apiKey: string; secretKey: string };
  tencent?: { secretId: string; secretKey: string; region: string };
  aliyun?: {
    accessKeyId: string;
    accessKeySecret: string;
    endpoint: string;
  };
}

export interface OcrResult {
  text: string;
  provider: "baidu" | "tencent" | "aliyun" | "tesseract";
}
