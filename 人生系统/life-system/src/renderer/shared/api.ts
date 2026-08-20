import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";

export function useApi() {
  const router = useRouter();
  async function call<T = any>(operation: () => Promise<any>): Promise<T> {
    const result = await operation();
    if (result.ok) return result.data as T;
    if (result.error.code === "DB_UNAVAILABLE") {
      ElMessage.error(result.error.message);
      await router.push("/settings");
    } else ElMessage.error(result.error.message);
    throw result.error;
  }
  return { call };
}
export function localToday(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}
export function toUtc(value: string | null | undefined): string | null {
  return value ? new Date(value).toISOString() : null;
}
export function toLocalInput(value: string | null | undefined): string {
  return value ? new Date(value).toISOString().slice(0, 16) : "";
}
