/**
 * 将渲染层数据收敛为 Electron IPC 可克隆的普通 JSON 值。
 * contextBridge 会在进入 preload 前克隆参数，所以调用方必须在边界之前完成转换。
 */
export function toIpcPayload<T>(value: T): T {
  // IPC 契约只使用 JSON 类型；往返转换会移除 Vue Proxy、Ref 和其他运行时包装对象。
  const json = JSON.stringify(value);
  // undefined 没有可传输的业务值，统一为 null，避免 JSON.parse(undefined) 抛错。
  return (json === undefined ? null : JSON.parse(json)) as T;
}
