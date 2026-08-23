// 本模块把 preload 的 ApiResult 解包为 Promise，使页面只处理成功数据或可读异常。
// 窄包装层避免 Vue 组件重复了解 code/message/data 的跨进程返回结构。
// 日期函数统一本地“今天”与 UTC 转换，避免各页面自行拼接时间字符串。
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
export { toIpcPayload } from "../../shared/contracts/ipc-payload.js";

interface ApiCallOptions {
  /** 初始化探测失败时不弹出重复提示；用户主动操作仍使用默认 false。 */
  silent?: boolean;
}

export function useApi() {
  // call 统一显示失败提示；数据库不可用时导航到设置页，让用户能直接修复连接。
  const router = useRouter();
  async function call<T = any>(
    operation: () => Promise<any>,
    options: ApiCallOptions = {},
  ): Promise<T> {
    // 只解包主进程的标准 ApiResult，禁止组件把未知 IPC 返回值当业务数据使用。
    // 普通浏览器直接打开 Vite 地址时没有 preload bridge，先给出可读提示而不是抛 undefined。
    if (!window.lifeSystem) {
      const error = {
        code: "INTERNAL_ERROR",
        message:
          "当前页面未连接 Electron 主进程，请使用 npm run dev 启动桌面应用",
      };
      // 浏览器预览模式的初始化同样支持静默，避免设置页加载时重复打断用户。
      if (!options.silent) ElMessage.error(error.message);
      throw error;
    }
    const result = await operation();
    if (result.ok) return result.data as T;
    if (result.error.code === "DB_UNAVAILABLE") {
      // 静默初始化仍然跳转设置页，但不连续弹出相同错误打断首屏加载。
      if (!options.silent) ElMessage.error(result.error.message);
      await router.push("/settings");
    } else if (result.error.code === "AI_UNAVAILABLE") {
      if (!options.silent)
        ElMessage.error("AI 服务不可用：请检查连接配置或网络");
    } else if (result.error.code === "INGEST_UNAVAILABLE") {
      if (!options.silent) ElMessage.error("文档入库管线未就绪，请稍后重试");
    } else if (result.error.code === "AI_AUTH_ERROR") {
      if (!options.silent) ElMessage.error("AI 鉴权失败：请检查 API Key");
    } else if (result.error.code === "AI_TIMEOUT") {
      if (!options.silent) ElMessage.error("AI 响应超时：模型思考或网络延迟过长，请重试");
    } else if (result.error.code === "VECTOR_DB_UNAVAILABLE") {
      if (!options.silent)
        ElMessage.error("向量数据库不可用：请检查 Milvus 连接配置");
    } else if (!options.silent) ElMessage.error(result.error.message);
    throw result.error;
  }
  return { call };
}
export function localToday(): string {
  // 按本地日历日生成打卡日期，避免 UTC 零点让东八区用户打到前一天。
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}
export function toUtc(value: string | null | undefined): string | null {
  // 空日期保持 null；非空输入转换 ISO，交由主进程再转 MySQL 的 UTC 格式。
  return value ? new Date(value).toISOString() : null;
}
export function toLocalInput(value: string | null | undefined): string {
  // 回填 datetime 输入只保留到分钟，匹配页面控件 value-format 的精度。
  return value ? new Date(value).toISOString().slice(0, 16) : "";
}
