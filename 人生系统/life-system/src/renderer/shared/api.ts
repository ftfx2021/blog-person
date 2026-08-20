import { ElMessage } from "element-plus";
// 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
import { useRouter } from "vue-router";

// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function useApi() {
  // 所有界面动作经窄 API 或原生对话框执行，避免渲染层越过进程边界。
  const router = useRouter();
  // 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
  async function call<T = any>(operation: () => Promise<any>): Promise<T> {
    // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
    const result = await operation();
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (result.ok) return result.data as T;
    // 显式处理当前状态分支，非法路径必须返回可操作的结构化错误。
    if (result.error.code === "DB_UNAVAILABLE") {
      ElMessage.error(result.error.message);
      // 这里执行持久化或事务步骤，确保数据库事实与界面状态保持一致。
      await router.push("/settings");
    } else ElMessage.error(result.error.message);
    // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
    throw result.error;
  }
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return { call };
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function localToday(): string {
  const now = new Date();
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function toUtc(value: string | null | undefined): string | null {
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return value ? new Date(value).toISOString() : null;
}
// 封装这个业务步骤，保证规则在主进程集中执行并可由单元测试覆盖。
export function toLocalInput(value: string | null | undefined): string {
  // 将结果交给上层或抛出带错误码的失败，避免调用方误把失败当成功。
  return value ? new Date(value).toISOString().slice(0, 16) : "";
}
