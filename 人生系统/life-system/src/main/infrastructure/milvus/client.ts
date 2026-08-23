import { MilvusClient } from "@zilliz/milvus2-sdk-node";
import type { MilvusSettings } from "../../../shared/contracts/system.js";

// 将所有 SDK 异常转换成稳定错误码，渲染层只看到可操作的连接提示。
export function vectorUnavailable(message: string): Error & { code: string } {
  return Object.assign(new Error(message), { code: "VECTOR_DB_UNAVAILABLE" });
}

// 每次调用创建短生命周期客户端，避免设置、备份之间共享失效连接。
export async function connectMilvus(
  settings: MilvusSettings,
): Promise<MilvusClient> {
  let client: MilvusClient | undefined;
  try {
    client = new MilvusClient({
      address: settings.address,
      username: settings.username,
      password: settings.password,
      ssl: settings.ssl,
      timeout: settings.connectTimeout,
      maxRetries: 3,
    });
    await client.connectPromise;
    const health = await client.checkHealth();
    if (health.isHealthy === false) throw new Error("服务健康检查未通过");
    return client;
  } catch (error) {
    try {
      client?.closeConnection();
    } catch {
      // 连接构造失败时没有可清理资源，忽略关闭异常。
    }
    const raw = error instanceof Error ? error.message.toLowerCase() : "";
    if (
      raw.includes("auth") ||
      raw.includes("credential") ||
      raw.includes("permission")
    )
      throw vectorUnavailable("鉴权失败：请检查用户名/密码");
    throw vectorUnavailable("无法连接 Milvus：请检查地址与端口");
  }
}

export function closeMilvus(client: MilvusClient): void {
  try {
    client.closeConnection();
  } catch {
    // 关闭是尽力操作，不覆盖已经完成的业务结果。
  }
}

// 对外提供任务书约定的客户端工厂名称；调用方仍负责在本次操作结束后关闭连接。
export const getClient = connectMilvus;
export const close = closeMilvus;
