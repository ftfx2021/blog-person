<script setup lang="ts">
// 收藏箱页面仅管理展示和交互，所有持久化、校验和状态机仍通过 preload 调用主进程。
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Delete,
  DocumentAdd,
  Edit,
  Link,
  RefreshLeft,
  Star,
} from "@element-plus/icons-vue";
import PageState from "../../shared/PageState.vue";
import { useApi } from "../../shared/api";

type InboxKind = "link" | "snippet" | "read_later";
type InboxStatus = "pending" | "clipped" | "bookmarked" | "discarded";

interface InboxItem {
  id: string;
  kind: InboxKind;
  url: string | null;
  title: string;
  note: string | null;
  status: InboxStatus;
  document_id: string | null;
  created_at: string;
  updated_at: string;
}

const { call } = useApi();
const rows = ref<InboxItem[]>([]);
const discardedRows = ref<InboxItem[]>([]);
const loading = ref(true);
const error = ref("");
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const kindFilter = ref<InboxKind | "">("");
const statusFilter = ref<Exclude<InboxStatus, "discarded">>("pending");
const discardedOpen = ref<string[]>([]);
const form = reactive({
  kind: "link" as InboxKind,
  title: "",
  url: "",
  note: "",
});

const kindOptions = [
  { label: "全部", value: "" },
  { label: "链接", value: "link" },
  { label: "片段", value: "snippet" },
  { label: "稍后读", value: "read_later" },
];
const statusOptions = [
  { label: "待处理", value: "pending" },
  { label: "仅保留", value: "bookmarked" },
  { label: "已入库", value: "clipped" },
];
const dialogTitle = computed(() =>
  editingId.value ? "编辑收藏" : "收藏新内容",
);
const showsUrl = computed(() => form.kind !== "snippet");

function resetForm(): void {
  // 关闭或新建时清空所有字段，避免上次编辑内容意外带入下一条收藏。
  editingId.value = null;
  form.kind = "link";
  form.title = "";
  form.url = "";
  form.note = "";
}

function openCreate(): void {
  // 新建入口总是从空表单开始，降低轻量暂存的操作成本。
  resetForm();
  dialogVisible.value = true;
}

function openEdit(item: InboxItem): void {
  // 编辑只回填收藏字段，页面从不展示或改写 document_id。
  editingId.value = item.id;
  form.kind = item.kind;
  form.title = item.title;
  form.url = item.url ?? "";
  form.note = item.note ?? "";
  dialogVisible.value = true;
}

function isHttpUrl(value: string): boolean {
  // 本地预检只提升表单反馈速度，主进程仍会重新验证 URL 协议和格式。
  try {
    const parsed = new URL(value);
    return parsed.protocol === "http:" || parsed.protocol === "https:";
  } catch {
    return false;
  }
}

function validateForm(): boolean {
  // 表单校验覆盖可在浏览器端立即发现的必填、长度和跨字段问题。
  if (!form.title.trim()) {
    ElMessage.error("请填写标题");
    return false;
  }
  if (form.title.trim().length > 200 || form.note.trim().length > 2000) {
    ElMessage.error("标题不能超过 200 字，备注不能超过 2000 字");
    return false;
  }
  if (showsUrl.value && !isHttpUrl(form.url.trim())) {
    ElMessage.error("请填写合法的 http/https URL");
    return false;
  }
  if (form.kind === "snippet" && !form.url.trim() && !form.note.trim()) {
    ElMessage.error("片段没有 URL 时必须填写备注");
    return false;
  }
  return true;
}

async function load(): Promise<void> {
  // 当前主列表由类型 Tab 和状态筛选共同决定，默认只加载 pending 收藏。
  loading.value = true;
  error.value = "";
  try {
    const [mainResult, discardedResult] = await Promise.all([
      call<{ items: InboxItem[] }>(() =>
        window.lifeSystem.inbox.list({
          kind: kindFilter.value || undefined,
          status: statusFilter.value,
        }),
      ),
      call<{ items: InboxItem[] }>(
        () =>
          window.lifeSystem.inbox.list({
            kind: kindFilter.value || undefined,
            status: "discarded",
          }),
        { silent: true },
      ),
    ]);
    rows.value = mainResult.items;
    discardedRows.value = discardedResult.items;
  } catch (cause: any) {
    error.value = cause.message || "收藏箱加载失败";
  } finally {
    loading.value = false;
  }
}

async function save(): Promise<void> {
  // 保存前先做本地预检，成功后从数据库刷新，保证状态与时间字段来自主进程。
  if (!validateForm()) return;
  const input = {
    kind: form.kind,
    title: form.title.trim(),
    // 片段表单不展示 URL，显式发送空串以支持编辑时清除原有链接。
    url: showsUrl.value ? form.url.trim() || undefined : "",
    note: form.note.trim() || undefined,
  };
  if (editingId.value) {
    await call(() =>
      window.lifeSystem.inbox.update({ id: editingId.value!, ...input }),
    );
    ElMessage.success("收藏已更新");
  } else {
    await call(() => window.lifeSystem.inbox.create(input));
    ElMessage.success("已加入收藏箱");
  }
  dialogVisible.value = false;
  await load();
}

async function clip(item: InboxItem): Promise<void> {
  // 剪藏失败时专门识别未就绪端口，明确说明收藏仍保留并允许稍后重试。
  try {
    await call(() => window.lifeSystem.inbox.clip(item.id), { silent: true });
    ElMessage.success("已入库，正在排队索引");
    await load();
  } catch (cause: any) {
    if (cause.code === "INGEST_UNAVAILABLE") {
      ElMessage.warning("文档入库管线未就绪，收藏已保留，可稍后重试");
      return;
    }
    ElMessage.error(cause.message || "剪藏入库失败");
  }
}

async function keep(item: InboxItem): Promise<void> {
  // 仅保留链接不触发抓取或入库，完成后条目从默认 pending 列表移出。
  await call(() => window.lifeSystem.inbox.keep(item.id));
  ElMessage.success("已仅保留链接");
  await load();
}

async function discard(item: InboxItem): Promise<void> {
  // 丢弃是可恢复的隐藏操作，确认后仍通过状态机而不是删除记录实现。
  await ElMessageBox.confirm("丢弃后可在已丢弃区域恢复。", "确认丢弃", {
    type: "warning",
  });
  await call(() => window.lifeSystem.inbox.discard(item.id));
  ElMessage.success("已移入已丢弃");
  await load();
}

async function restore(item: InboxItem): Promise<void> {
  // 恢复把收藏送回 pending，原有文档关联不会由页面删除。
  await call(() => window.lifeSystem.inbox.restore(item.id));
  ElMessage.success("已恢复到待处理");
  await load();
}

function sourceLabel(item: InboxItem): string {
  // 来源只展示域名以保持列表紧凑，解析失败时保留原始 URL 作为降级信息。
  if (!item.url) return "自存";
  try {
    return new URL(item.url).hostname;
  } catch {
    return item.url;
  }
}

const kindLabels: Record<InboxKind, string> = {
  link: "链接",
  snippet: "片段",
  read_later: "稍后读",
};

// 页面首次进入时加载列表；筛选变化由控件显式调用 load，避免重复请求。
onMounted(load);
</script>

<template>
  <div class="page-head">
    <div>
      <h1>收藏箱</h1>
      <p>链接、片段、稍后读先轻量暂存；值得的剪藏入库，其余丢弃或仅留链接</p>
    </div>
    <el-button type="primary" :icon="DocumentAdd" @click="openCreate"
      >收藏新内容</el-button
    >
  </div>

  <div class="inbox-toolbar">
    <el-segmented v-model="kindFilter" :options="kindOptions" @change="load" />
    <el-select v-model="statusFilter" aria-label="状态筛选" @change="load">
      <el-option
        v-for="option in statusOptions"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
  </div>

  <div class="inbox-layout">
    <section>
      <PageState
        :loading="loading"
        :error="error"
        :empty="rows.length === 0"
        empty-text="当前筛选下没有收藏"
        @retry="load"
      >
        <div class="list inbox-list">
          <article
            v-for="item in rows"
            :key="item.id"
            class="list-row inbox-row"
          >
            <div class="list-main">
              <div class="inbox-title-line">
                <el-tag size="small" effect="plain">{{
                  kindLabels[item.kind]
                }}</el-tag>
                <span class="list-title">{{ item.title }}</span>
              </div>
              <div class="list-meta">
                {{ sourceLabel(item) }} ·
                {{
                  item.status === "bookmarked"
                    ? "仅保留链接"
                    : item.status === "clipped"
                      ? "已入库"
                      : "待处理"
                }}
              </div>
              <p v-if="item.note" class="inbox-note">{{ item.note }}</p>
            </div>
            <div class="row-actions inbox-actions">
              <el-button
                v-if="item.status !== 'clipped'"
                size="small"
                type="primary"
                :icon="DocumentAdd"
                @click="clip(item)"
                >剪藏入库</el-button
              >
              <el-button
                v-if="item.kind !== 'snippet'"
                size="small"
                :icon="Star"
                @click="keep(item)"
                >仅保留链接</el-button
              >
              <el-button
                v-if="item.status !== 'clipped'"
                text
                :icon="Edit"
                title="编辑"
                @click="openEdit(item)"
              />
              <el-button
                text
                type="danger"
                :icon="Delete"
                title="丢弃"
                @click="discard(item)"
              />
            </div>
          </article>
        </div>
      </PageState>

      <el-collapse v-model="discardedOpen" class="discarded-section">
        <el-collapse-item name="discarded">
          <template #title>已丢弃（{{ discardedRows.length }}）</template>
          <div v-if="discardedRows.length" class="list inbox-list">
            <article
              v-for="item in discardedRows"
              :key="item.id"
              class="list-row inbox-row"
            >
              <div class="list-main">
                <div class="inbox-title-line">
                  <el-tag size="small" type="info">{{
                    kindLabels[item.kind]
                  }}</el-tag
                  ><span class="list-title">{{ item.title }}</span>
                </div>
                <div class="list-meta">{{ sourceLabel(item) }}</div>
              </div>
              <el-button size="small" :icon="RefreshLeft" @click="restore(item)"
                >恢复</el-button
              >
            </article>
          </div>
          <p v-else class="empty-inline">暂无已丢弃收藏</p>
        </el-collapse-item>
      </el-collapse>
    </section>

    <aside class="inbox-rules">
      <h2>处理规则</h2>
      <dl>
        <div>
          <dt>剪藏入库</dt>
          <dd>提交到知识库，并在后续参与检索问答。</dd>
        </div>
        <div>
          <dt>仅保留链接</dt>
          <dd>只保存 URL，不抓取、不进入知识库。</dd>
        </div>
        <div>
          <dt>丢弃</dt>
          <dd>移出主列表；需要时仍可恢复。</dd>
        </div>
      </dl>
    </aside>
  </div>

  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    @closed="resetForm"
  >
    <el-form label-position="top" class="dialog-form">
      <el-form-item label="类型">
        <el-radio-group v-model="form.kind">
          <el-radio-button value="link">链接</el-radio-button>
          <el-radio-button value="snippet">片段</el-radio-button>
          <el-radio-button value="read_later">稍后读</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="标题" required
        ><el-input v-model="form.title" maxlength="200" show-word-limit
      /></el-form-item>
      <el-form-item v-if="showsUrl" label="URL" required
        ><el-input
          v-model="form.url"
          placeholder="https://"
          :prefix-icon="Link"
      /></el-form-item>
      <el-form-item
        :label="form.kind === 'snippet' ? '片段内容' : '备注'"
        :required="form.kind === 'snippet' && !form.url.trim()"
      >
        <el-input
          v-model="form.note"
          type="textarea"
          :rows="5"
          maxlength="2000"
          show-word-limit
          placeholder="可记录摘要、想法或片段正文"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.inbox-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.inbox-toolbar .el-select {
  width: 128px;
}
.inbox-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 24px;
  align-items: start;
}
.inbox-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.inbox-note {
  margin: 7px 0 0;
  color: #5f6d68;
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-wrap;
}
.inbox-row {
  align-items: flex-start;
}
.inbox-actions {
  padding-top: 2px;
}
.inbox-rules {
  border-left: 3px solid #e69645;
  padding: 4px 0 4px 16px;
}
.inbox-rules h2 {
  margin: 0 0 14px;
  font-size: 15px;
}
.inbox-rules dl {
  margin: 0;
  display: grid;
  gap: 14px;
}
.inbox-rules dt {
  font-size: 13px;
  font-weight: 650;
  color: #2b3a34;
}
.inbox-rules dd {
  margin: 4px 0 0;
  color: #77827e;
  font-size: 12px;
  line-height: 1.55;
}
.discarded-section {
  margin-top: 20px;
}
@media (max-width: 1100px) {
  .inbox-layout {
    grid-template-columns: 1fr;
  }
  .inbox-rules {
    border-left: 0;
    border-top: 1px solid #dfe5e2;
    padding: 18px 0 0;
  }
}
</style>
