<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ChatDotRound,
  CopyDocument,
  Delete,
  Download,
  Edit,
  Plus,
  Promotion,
  RefreshLeft,
  Setting,
  Top,
  VideoPause,
} from "@element-plus/icons-vue";
import { useApi, toIpcPayload } from "../../shared/api";
import MarkdownView from "../../shared/MarkdownView.vue";

interface Assistant {
  id: string;
  name: string;
  description: string | null;
  systemPrompt: string;
}

interface Session {
  id: string;
  assistantId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  pinned?: boolean;
}

interface Message {
  id?: string;
  role: "user" | "assistant";
  content: string;
  reasoning?: string | null;
  usage?: { promptTokens: number; completionTokens: number; reasoningTokens?: number };
  model?: string;
  error?: boolean;
  retryable?: boolean;
  reasoningExpanded?: boolean;
}

const router = useRouter();
const { call } = useApi();
const assistants = ref<Assistant[]>([]);
const sessions = ref<Session[]>([]);
const messages = ref<Message[]>([]);
const currentAssistantId = ref<string>();
const currentSessionId = ref<string>();
const activeSessionId = ref<string>();
const input = ref("");
const think = ref(false);
const sessionSearch = ref("");
const unavailable = ref("");
const loading = ref(true);
const loadingMessages = ref(false);
const sending = ref(false);
const busy = ref("");
const totalMessages = ref(0);
const messageOffset = ref(0);
const waiting = ref(false);
const firstTokenMs = ref<number>();
const timeoutNotice = ref(false);
const firstTokenStartedAt = ref<number>();
const firstTokenTimer = ref<ReturnType<typeof setTimeout>>();
const firstTokenTimeoutTimer = ref<ReturnType<typeof setTimeout>>();
const pendingDelta = ref("");
const pendingReasoning = ref("");
let deltaFrame = 0;
let reasoningFrame = 0;
const list = ref<HTMLElement>();
const stopSubscriptions: Array<() => void> = [];
const assistantDialogVisible = ref(false);
const editingAssistantId = ref<string>();
const assistantForm = reactive({ name: "", description: "", systemPrompt: "" });

const currentAssistant = computed(() =>
  assistants.value.find((item) => item.id === currentAssistantId.value),
);
const currentSession = computed(() =>
  sessions.value.find((item) => item.id === currentSessionId.value),
);
const hasEarlierMessages = computed(
  () => messages.value.length < totalMessages.value,
);
const filteredSessions = computed(() => {
  const keyword = sessionSearch.value.trim().toLowerCase();
  return keyword
    ? sessions.value.filter((session) => session.title.toLowerCase().includes(keyword))
    : sessions.value;
});
// 超过 100 条时只把靠近底部的 100 条交给 DOM，历史仍保存在 messages 中供分页加载。
const renderedMessages = computed(() => {
  const start = Math.max(0, messages.value.length - 100);
  return messages.value.slice(start).map((message, offset) => ({ message, index: start + offset }));
});

function markFirstToken() {
  if (!waiting.value) return;
  firstTokenMs.value = Math.round(performance.now() - (firstTokenStartedAt.value || performance.now()));
  waiting.value = false;
  if (firstTokenTimer.value) clearTimeout(firstTokenTimer.value);
  if (firstTokenTimeoutTimer.value) clearTimeout(firstTokenTimeoutTimer.value);
}

function clearFirstTokenTimers() {
  if (firstTokenTimer.value) clearTimeout(firstTokenTimer.value);
  if (firstTokenTimeoutTimer.value) clearTimeout(firstTokenTimeoutTimer.value);
  firstTokenTimer.value = undefined;
  firstTokenTimeoutTimer.value = undefined;
  waiting.value = false;
}

function flushDelta() {
  deltaFrame = 0;
  const value = pendingDelta.value;
  pendingDelta.value = "";
  const last = messages.value.at(-1);
  if (last?.role === "assistant" && value) last.content += value;
  scrollToBottom();
}

function flushReasoning() {
  reasoningFrame = 0;
  const value = pendingReasoning.value;
  pendingReasoning.value = "";
  const last = messages.value.at(-1);
  if (last?.role === "assistant" && value) last.reasoning = `${last.reasoning || ""}${value}`;
  scrollToBottom();
}

function scrollToBottom() {
  void nextTick(() => {
    if (list.value) list.value.scrollTop = list.value.scrollHeight;
  });
}

function handleDelta(payload: { sessionId: string; delta: string }) {
  if (payload.sessionId !== activeSessionId.value) return;
  markFirstToken();
  pendingDelta.value += payload.delta;
  if (!deltaFrame) deltaFrame = requestAnimationFrame(flushDelta);
}

function handleReasoning(payload: { sessionId: string; delta: string }) {
  if (payload.sessionId !== activeSessionId.value) return;
  markFirstToken();
  pendingReasoning.value += payload.delta;
  if (!reasoningFrame) reasoningFrame = requestAnimationFrame(flushReasoning);
}

function handleDone(payload: { sessionId: string; fullText: string; reasoning?: string; model?: string; usage?: Message["usage"] }) {
  if (payload.sessionId !== activeSessionId.value) return;
  if (deltaFrame) cancelAnimationFrame(deltaFrame);
  if (reasoningFrame) cancelAnimationFrame(reasoningFrame);
  flushDelta();
  flushReasoning();
  const last = messages.value.at(-1);
  if (last?.role === "assistant") {
    last.content = payload.fullText;
    last.reasoning = payload.reasoning || last.reasoning;
    last.model = payload.model;
    last.usage = payload.usage;
  }
  clearFirstTokenTimers();
  sending.value = false;
  activeSessionId.value = undefined;
  void refreshSessions();
  scrollToBottom();
}

function handleError(payload: { sessionId: string; code: string; message: string }) {
  if (payload.sessionId !== activeSessionId.value) return;
  const last = messages.value.at(-1);
  if (last?.role === "assistant") {
    last.error = true;
    last.content =
      payload.code === "AI_AUTH_ERROR"
        ? "AI 鉴权失败，请前往设置检查 API Key。"
        : payload.code === "AI_TIMEOUT"
          ? "AI 响应超时：模型思考或网络延迟过长，请重试。"
        : payload.code === "AI_UNAVAILABLE"
          ? `AI 不可用：${payload.message}`
          : payload.message;
  }
  sending.value = false;
  clearFirstTokenTimers();
  if (last?.role === "assistant" && payload.code !== "AI_AUTH_ERROR" && payload.code !== "AI_TIMEOUT")
    last.retryable = true;
  activeSessionId.value = undefined;
  scrollToBottom();
}

async function refreshSessions() {
  if (!currentAssistantId.value) {
    sessions.value = [];
    return;
  }
  const result: any = await call(() =>
    window.lifeSystem.sessions.list({ assistantId: currentAssistantId.value }),
  );
  sessions.value = result.sessions;
}

async function loadSessionMessages(sessionId: string) {
  await stopCurrentStream();
  currentSessionId.value = sessionId;
  messageOffset.value = 0;
  totalMessages.value = 0;
  messages.value = [];
  loadingMessages.value = true;
  try {
    const result: any = await call(() =>
      window.lifeSystem.sessions.messages({ sessionId, limit: 50, offset: 0 }),
    );
    messages.value = result.messages;
    totalMessages.value = result.total;
    scrollToBottom();
  } finally {
    loadingMessages.value = false;
  }
}

async function loadEarlierMessages() {
  if (!currentSessionId.value || !hasEarlierMessages.value) return;
  const nextOffset = messageOffset.value + 50;
  const result: any = await call(() =>
    window.lifeSystem.sessions.messages({
      sessionId: currentSessionId.value,
      limit: 50,
      offset: nextOffset,
    }),
  );
  messages.value = [...result.messages, ...messages.value];
  messageOffset.value = nextOffset;
  totalMessages.value = result.total;
}

async function createSession() {
  if (!currentAssistantId.value) return;
  const session: Session = await call(() =>
    window.lifeSystem.sessions.create({ assistantId: currentAssistantId.value }),
  );
  sessions.value.unshift(session);
  await loadSessionMessages(session.id);
}

async function refreshAssistants(preferredId?: string) {
  const result: any = await call(() => window.lifeSystem.assistant.list());
  assistants.value = result.assistants;
  if (!assistants.value.length) {
    currentAssistantId.value = undefined;
    currentSessionId.value = undefined;
    sessions.value = [];
    messages.value = [];
    return;
  }
  const nextId = preferredId && assistants.value.some((item) => item.id === preferredId)
    ? preferredId
    : currentAssistantId.value && assistants.value.some((item) => item.id === currentAssistantId.value)
      ? currentAssistantId.value
      : assistants.value[0]!.id;
  if (nextId !== currentAssistantId.value) {
    currentAssistantId.value = nextId;
    currentSessionId.value = undefined;
  }
  await refreshSessions();
  if (!sessions.value.length) await createSession();
  else if (!currentSessionId.value || !sessions.value.some((item) => item.id === currentSessionId.value))
    await loadSessionMessages(sessions.value[0]!.id);
}

async function loadPage() {
  loading.value = true;
  try {
    await refreshAssistants();
  } catch (error: any) {
    // 数据库未连接时保留助手页空状态，并用降级提示引导用户先修复设置。
    unavailable.value = error?.message || "数据库尚未连接，无法读取助手历史";
    assistants.value = [];
    sessions.value = [];
    messages.value = [];
  } finally {
    // 助手/会话数据已经决定页面结构，页面进入时不主动探测远端 AI 服务。
    loading.value = false;
  }
}

async function send() {
  const message = input.value.trim();
  if (!message || sending.value || unavailable.value || !currentSessionId.value) return;
  input.value = "";
  messages.value.push({ role: "user", content: message });
  await sendMessage(message);
  scrollToBottom();
}

async function stopCurrentStream() {
  if (!activeSessionId.value) return;
  await call(
    () => window.lifeSystem.ai.stop({ sessionId: activeSessionId.value }),
    { silent: true },
  ).catch(() => undefined);
  // 保留 activeSessionId 等待主进程 done，确保中止后的部分内容仍能正确收尾。
}

async function deleteCurrentSession() {
  if (!currentSession.value) return;
  try {
    await ElMessageBox.confirm("删除后该会话的全部消息都会清除，是否继续？", "确认删除", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  await stopCurrentStream();
  await call(() => window.lifeSystem.sessions.remove(currentSession.value!.id));
  sessions.value = sessions.value.filter((item) => item.id !== currentSession.value!.id);
  currentSessionId.value = undefined;
  messages.value = [];
  if (sessions.value[0]) await loadSessionMessages(sessions.value[0].id);
  else await createSession();
}

async function renameCurrentSession() {
  if (!currentSession.value) return;
  try {
    const result = await ElMessageBox.prompt("输入新的会话标题", "重命名会话", {
      inputValue: currentSession.value.title,
      inputValidator: (value) => (value.trim() ? true : "标题不能为空"),
      confirmButtonText: "保存",
      cancelButtonText: "取消",
    });
    const session: Session = await call(() =>
      window.lifeSystem.sessions.rename({ id: currentSession.value!.id, title: result.value }),
    );
    const index = sessions.value.findIndex((item) => item.id === session.id);
    if (index >= 0) sessions.value[index] = session;
  } catch {
    // 用户取消重命名时不提示错误。
  }
}

async function copyMessage(message: Message) {
  await navigator.clipboard.writeText(message.content);
  ElMessage.success("消息已复制");
}

function toggleReasoning(message: Message) {
  message.reasoningExpanded = !message.reasoningExpanded;
}

function handleTitleUpdated(payload: { id: string; title: string }) {
  const session = sessions.value.find((item) => item.id === payload.id);
  if (!session) return;
  session.title = payload.title;
}

async function retryMessage(message: Message) {
  if (!currentSessionId.value || sending.value) return;
  const index = messages.value.indexOf(message);
  const user = index > 0 ? messages.value[index - 1] : undefined;
  if (!user || user.role !== "user") {
    ElMessage.warning("会话状态已变化，请重新发送");
    return;
  }
  const result: any = await call(() => window.lifeSystem.sessions.messages({ sessionId: currentSessionId.value, limit: 50, offset: 0 }), { silent: true });
  if (!result.messages.some((item: Message) => item.content === user.content && item.role === "user")) {
    ElMessage.warning("会话状态已变化，请重新发送");
    return;
  }
  messages.value.splice(index, 1);
  await sendMessage(user.content);
}

async function sendMessage(message: string) {
  if (!currentSessionId.value || sending.value) return;
  messages.value.push({ role: "assistant", content: "", reasoning: "" });
  sending.value = true;
  waiting.value = true;
  timeoutNotice.value = false;
  firstTokenMs.value = undefined;
  firstTokenStartedAt.value = performance.now();
  firstTokenTimeoutTimer.value = setTimeout(() => {
    if (waiting.value) timeoutNotice.value = true;
  }, 30_000);
  activeSessionId.value = currentSessionId.value;
  try {
    await call(() => window.lifeSystem.ai.start(toIpcPayload({ sessionId: currentSessionId.value, message, think: think.value })));
  } catch (error: any) {
    const last = messages.value.at(-1);
    if (last?.role === "assistant") {
      last.error = true;
      last.retryable = true;
      last.content = error?.code === "AI_AUTH_ERROR" ? "AI 鉴权失败，请前往设置检查 API Key。" : error?.code === "AI_TIMEOUT" ? "AI 响应超时：模型思考或网络延迟过长，请重试。" : "AI 暂时不可用，请检查设置或网络后重试。";
    }
    sending.value = false;
    clearFirstTokenTimers();
    activeSessionId.value = undefined;
  }
}

async function regenerate() {
  if (!currentSessionId.value || sending.value) return;
  const result: any = await call(() => window.lifeSystem.sessions.regenerate({ sessionId: currentSessionId.value }));
  messages.value = messages.value.slice(0, -1);
  await sendMessage(result.message);
}

async function removeMessage(message: Message) {
  if (!message.id || !currentSessionId.value) return;
  try {
    await ElMessageBox.confirm("只能删除会话最后一条消息，是否继续？", "确认删除", { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" });
  } catch {
    return;
  }
  await call(() => window.lifeSystem.sessions.removeMessage({ id: message.id, sessionId: currentSessionId.value, confirmed: true }));
  await loadSessionMessages(currentSessionId.value);
}

async function clearCurrentSession() {
  if (!currentSessionId.value) return;
  try {
    await ElMessageBox.confirm("将清空当前会话的全部消息，是否继续？", "确认清空", { type: "warning", confirmButtonText: "清空", cancelButtonText: "取消" });
  } catch {
    return;
  }
  await stopCurrentStream();
  await call(() => window.lifeSystem.sessions.clear(currentSessionId.value!));
  messages.value = [];
  await refreshSessions();
}

async function togglePinned(session: Session) {
  const updated: Session = await call(() => window.lifeSystem.sessions.pin({ id: session.id, pinned: !session.pinned }));
  const index = sessions.value.findIndex((item) => item.id === updated.id);
  if (index >= 0) sessions.value[index] = updated;
  sessions.value.sort((left, right) => Number(Boolean(right.pinned)) - Number(Boolean(left.pinned)));
}

async function exportCurrentSession() {
  if (!currentSessionId.value) return;
  const result: any = await call(() => window.lifeSystem.sessions.export(currentSessionId.value!));
  ElMessage.success(`已导出：${result.path}`);
}

function openCreateAssistant() {
  editingAssistantId.value = undefined;
  Object.assign(assistantForm, { name: "", description: "", systemPrompt: "" });
  assistantDialogVisible.value = true;
}

function openManageAssistants() {
  // 管理入口始终打开助手列表，避免上次编辑状态残留在下一次弹窗中。
  editingAssistantId.value = undefined;
  assistantDialogVisible.value = true;
}

function openEditAssistant(assistant: Assistant) {
  editingAssistantId.value = assistant.id;
  Object.assign(assistantForm, {
    name: assistant.name,
    description: assistant.description || "",
    systemPrompt: assistant.systemPrompt,
  });
  assistantDialogVisible.value = true;
}

async function saveAssistant() {
  if (!assistantForm.name.trim()) {
    ElMessage.warning("助手名称不能为空");
    return;
  }
  busy.value = "assistant-save";
  try {
    const input = toIpcPayload(assistantForm);
    const assistant: Assistant = editingAssistantId.value
      ? await call(() => window.lifeSystem.assistant.update({ id: editingAssistantId.value, ...input }))
      : await call(() => window.lifeSystem.assistant.create(input));
    assistantDialogVisible.value = false;
    await refreshAssistants(assistant.id);
    if (!sessions.value.length) await createSession();
  } finally {
    busy.value = "";
  }
}

async function deleteAssistant(assistant: Assistant) {
  try {
    await ElMessageBox.confirm("将同时删除该助手的所有会话与消息，是否继续？", "确认删除", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  await stopCurrentStream();
  await call(() => window.lifeSystem.assistant.remove(assistant.id));
  await refreshAssistants();
}

async function selectAssistant(id: string) {
  if (id === currentAssistantId.value) return;
  await stopCurrentStream();
  currentAssistantId.value = id;
  currentSessionId.value = undefined;
  messages.value = [];
  await refreshSessions();
  if (sessions.value[0]) await loadSessionMessages(sessions.value[0].id);
  else await createSession();
}

onMounted(() => {
  // 页面建立后立即订阅固定事件，sessionId 已由左侧数据库会话提供。
  stopSubscriptions.push(window.lifeSystem.ai.onDelta(handleDelta));
  stopSubscriptions.push(window.lifeSystem.ai.onReasoning(handleReasoning));
  stopSubscriptions.push(window.lifeSystem.ai.onDone(handleDone));
  stopSubscriptions.push(window.lifeSystem.ai.onError(handleError));
  stopSubscriptions.push(window.lifeSystem.sessions.onTitleUpdated(handleTitleUpdated));
  void loadPage();
});

onBeforeUnmount(() => {
  // 页面切换时解除事件订阅并停止当前流，避免旧页面继续收到新会话增量。
  stopSubscriptions.splice(0).forEach((cancel) => cancel());
  void stopCurrentStream();
});
</script>

<template>
  <div class="assistant-page">
    <div class="page-head">
      <div><h1>AI 助手</h1><p>助手、会话与历史消息均保存在本机数据库。</p></div>
      <el-button :icon="Setting" @click="router.push('/settings')">设置</el-button>
    </div>
    <el-alert
      v-if="unavailable"
      type="warning"
      :closable="false"
      class="assistant-alert"
      :title="`AI 不可用：${unavailable}`"
    ><template #default><el-button link type="primary" @click="router.push('/settings')">前往设置配置 LLM</el-button></template></el-alert>
    <div v-if="loading" class="assistant-state"><el-icon class="is-loading"><ChatDotRound /></el-icon>正在加载助手与会话…</div>
    <div v-else-if="!assistants.length" class="assistant-no-assistant">
      <el-icon><ChatDotRound /></el-icon><strong>还没有助手</strong><span>创建一个助手并设置它的系统提示词。</span>
      <el-button type="primary" :icon="Plus" @click="openCreateAssistant">新建助手</el-button>
    </div>
    <div v-else class="assistant-layout">
      <aside class="assistant-sidebar">
        <div class="assistant-sidebar-head"><el-select :model-value="currentAssistantId" @change="selectAssistant"><el-option v-for="assistant in assistants" :key="assistant.id" :label="assistant.name" :value="assistant.id" /></el-select><el-button :icon="Edit" circle title="管理助手" @click="openManageAssistants" /></div>
        <div class="assistant-sidebar-actions"><el-button type="primary" plain :icon="Plus" @click="createSession">新建会话</el-button><el-button link @click="openManageAssistants">管理助手</el-button></div>
        <el-input v-model="sessionSearch" class="session-search" clearable placeholder="搜索会话" />
        <div class="assistant-session-title">会话</div>
        <div class="assistant-sessions">
          <button v-for="session in filteredSessions" :key="session.id" :class="['assistant-session-item', { active: session.id === currentSessionId }]" @click="loadSessionMessages(session.id)"><el-icon><Top v-if="session.pinned" /><ChatDotRound v-else /></el-icon><span>{{ session.title }}</span><el-button link :icon="Top" class="session-pin" :title="session.pinned ? '取消置顶' : '置顶'" @click.stop="togglePinned(session)" /></button>
        </div>
      </aside>
      <section class="assistant-main">
        <header class="assistant-conversation-head"><div><strong>{{ currentSession?.title || "新会话" }}</strong><small>{{ currentAssistant?.name }}</small></div><div class="row-actions"><el-button link :icon="RefreshLeft" @click="regenerate">重新生成</el-button><el-button link :icon="Download" @click="exportCurrentSession">导出</el-button><el-button link :icon="Edit" @click="renameCurrentSession">重命名</el-button><el-button link type="warning" @click="clearCurrentSession">清空</el-button><el-button link type="danger" :icon="Delete" @click="deleteCurrentSession">删除会话</el-button></div></header>
        <section ref="list" class="assistant-panel">
          <div v-if="loadingMessages" class="assistant-state">正在加载历史消息…</div>
          <template v-else>
            <el-button v-if="hasEarlierMessages" link class="earlier-button" @click="loadEarlierMessages">加载更早消息</el-button>
            <div v-if="!messages.length" class="assistant-empty"><strong>问我任何问题</strong><span>当前会话还没有消息。</span></div>
            <div v-for="entry in renderedMessages" :key="entry.message.id || entry.index" :class="['assistant-message', entry.message.role]">
              <div v-if="entry.message" class="assistant-message-content">
                <details v-if="entry.message.reasoning" :open="sending && entry.index === messages.length - 1" class="reasoning-panel"><summary>深度思考<span v-if="entry.message.usage?.reasoningTokens"> · {{ entry.message.usage.reasoningTokens }} tokens</span></summary><pre>{{ sending && entry.index === messages.length - 1 || entry.message.reasoningExpanded ? entry.message.reasoning : entry.message.reasoning.slice(0, 200) }}<template v-if="!sending && !entry.message.reasoningExpanded && entry.message.reasoning.length > 200">…</template></pre><el-button v-if="!sending && entry.message.reasoning.length > 200" link size="small" @click="toggleReasoning(entry.message)">{{ entry.message.reasoningExpanded ? "收起完整推理" : "展开完整推理" }}</el-button></details>
                <div class="assistant-bubble" :class="{ error: entry.message.error, streaming: sending && entry.index === messages.length - 1 }">
                  <template v-if="sending && entry.index === messages.length - 1"><span>{{ entry.message.content || (waiting ? "正在等待模型响应…" : "") }}</span><span v-if="!waiting" class="typing-cursor">▍</span></template>
                  <template v-else-if="entry.message.role === 'assistant' && !entry.message.content"><span class="message-meta">（回答未完成，已中断）</span></template>
                  <template v-else-if="entry.message.role === 'assistant'"><MarkdownView :key="entry.message.id || entry.index" :content="entry.message.content" /></template>
                  <template v-else>{{ entry.message.content }}</template>
                </div>
                <div v-if="sending && waiting && timeoutNotice && entry.index === messages.length - 1" class="wait-notice">已等待 30 秒无响应，请检查网络或点击停止</div>
                <div v-if="entry.message.model || entry.message.usage || (firstTokenMs && entry.index === messages.length - 1)" class="message-meta"><span v-if="entry.message.model">{{ entry.message.model }}</span><span v-if="entry.message.usage">↑{{ entry.message.usage.promptTokens }} ↓{{ entry.message.usage.completionTokens }}<template v-if="entry.message.usage.reasoningTokens"> · 思考 {{ entry.message.usage.reasoningTokens }}</template></span><span v-if="firstTokenMs && entry.index === messages.length - 1">首字 {{ (firstTokenMs / 1000).toFixed(1) }}s</span></div>
                <div class="message-actions"><el-button link :icon="CopyDocument" @click="copyMessage(entry.message)">复制</el-button><el-button v-if="entry.message.id && entry.index === messages.length - 1" link :icon="Delete" @click="removeMessage(entry.message)">删除</el-button><el-button v-if="entry.message.error && entry.message.retryable" link :icon="RefreshLeft" @click="retryMessage(entry.message)">重试</el-button></div>
              </div>
            </div>
          </template>
        </section>
        <section class="assistant-compose"><el-input v-model="input" type="textarea" :rows="3" resize="none" maxlength="8000" show-word-limit :disabled="sending || !!unavailable" placeholder="输入问题，Enter 发送，Shift+Enter 换行" @keydown.enter.exact.prevent="send" /><div class="assistant-compose-foot"><el-switch v-model="think" inline-prompt active-text="深度思考" inactive-text="普通回答" /><span>上下文按 6000 token 预算截断。</span><div class="row-actions"><el-button v-if="sending" :icon="VideoPause" @click="stopCurrentStream">停止</el-button><el-button type="primary" :icon="Promotion" :disabled="sending || !!unavailable || !input.trim()" @click="send">发送</el-button></div></div></section>
      </section>
    </div>
    <el-dialog v-model="assistantDialogVisible" :title="editingAssistantId ? '编辑助手' : '管理助手'" width="620px">
      <div v-if="!editingAssistantId" class="assistant-dialog-list"><div v-for="assistant in assistants" :key="assistant.id" class="assistant-dialog-row"><div><strong>{{ assistant.name }}</strong><small>{{ assistant.description || "暂无简介" }}</small></div><div class="row-actions"><el-button link :icon="Edit" @click="openEditAssistant(assistant)">编辑</el-button><el-button link type="danger" :icon="Delete" @click="deleteAssistant(assistant)">删除</el-button></div></div><div v-if="assistants.length" class="assistant-dialog-divider" /></div>
      <el-form label-position="top"><el-form-item label="名称"><el-input v-model="assistantForm.name" maxlength="50" /></el-form-item><el-form-item label="简介"><el-input v-model="assistantForm.description" maxlength="200" /></el-form-item><el-form-item label="系统提示词"><el-input v-model="assistantForm.systemPrompt" type="textarea" :rows="6" maxlength="4000" placeholder="仅对本助手的会话生效的系统指令" /></el-form-item></el-form>
      <template #footer><el-button @click="assistantDialogVisible = false">取消</el-button><el-button type="primary" :loading="busy === 'assistant-save'" @click="saveAssistant">保存助手</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.assistant-page { min-height: calc(100vh - 120px); display: flex; flex-direction: column; }
.assistant-alert { margin-bottom: 14px; }
.assistant-layout { min-height: 620px; flex: 1; display: grid; grid-template-columns: 260px minmax(0, 1fr); gap: 14px; }
.assistant-sidebar, .assistant-main, .assistant-compose, .assistant-panel { background: #fff; border: 1px solid #e1e6e3; border-radius: 7px; }
.assistant-sidebar { padding: 14px; display: flex; flex-direction: column; min-height: 620px; }
.assistant-sidebar-head { display: flex; gap: 8px; align-items: center; }
.assistant-sidebar-head .el-select { flex: 1; }
.assistant-sidebar-actions { display: flex; align-items: center; justify-content: space-between; margin: 16px 0; }
.session-search { margin-bottom: 10px; }
.assistant-session-title { color: #7a8682; font-size: 12px; margin-bottom: 8px; }
.assistant-sessions { overflow-y: auto; display: grid; gap: 4px; }
.assistant-session-item { min-width: 0; border: 0; background: transparent; color: #50625c; padding: 10px; border-radius: 6px; display: flex; align-items: center; gap: 8px; text-align: left; cursor: pointer; }
.assistant-session-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.session-pin { opacity: 0; flex: none; }
.assistant-session-item:hover .session-pin, .assistant-session-item.active .session-pin { opacity: 1; }
.assistant-session-item:hover, .assistant-session-item.active { background: #edf3f0; color: #24312d; }
.assistant-main { min-width: 0; min-height: 620px; border: 0; background: transparent; display: flex; flex-direction: column; gap: 14px; }
.assistant-conversation-head { min-height: 58px; padding: 12px 16px; background: #fff; border: 1px solid #e1e6e3; border-radius: 7px; display: flex; align-items: center; justify-content: space-between; }
.assistant-conversation-head strong, .assistant-conversation-head small { display: block; }
.assistant-conversation-head small { color: #7a8682; font-size: 12px; margin-top: 3px; }
.assistant-panel { flex: 1; min-height: 400px; max-height: calc(100vh - 350px); overflow-y: auto; padding: 24px; }
.assistant-empty, .assistant-state { min-height: 320px; display: grid; place-content: center; text-align: center; gap: 8px; color: #7a8682; }
.assistant-empty strong { color: #31463f; font-size: 20px; }
.assistant-message { display: flex; margin: 0 0 16px; }
.assistant-message.user { justify-content: flex-end; }
.assistant-message-content { max-width: 82%; }
.assistant-bubble { max-width: 100%; padding: 11px 14px; white-space: pre-wrap; line-height: 1.65; background: #edf3f0; border-radius: 7px; color: #24312d; overflow-wrap: anywhere; }
.assistant-message.user .assistant-bubble { background: #31463f; color: #fff; }
.assistant-bubble.error { background: #fff1f0; color: #b42318; }
.assistant-bubble :deep(pre) { max-width: 100%; overflow-x: auto; padding: 10px; background: #1f2926; color: #eef4f1; border-radius: 5px; }
.assistant-bubble :deep(code) { font-family: Consolas, monospace; }
.reasoning-panel { margin-bottom: 8px; padding: 8px 10px; background: #f6f7f5; border-left: 3px solid #aebbb5; color: #617069; border-radius: 4px; }
.reasoning-panel summary { cursor: pointer; font-size: 12px; font-style: italic; }
.reasoning-panel pre { margin: 8px 0 0; white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; font-size: 12px; line-height: 1.55; }
.typing-cursor { display: inline-block; margin-left: 2px; animation: assistant-cursor 1s step-end infinite; }
.message-meta, .message-actions, .wait-notice { color: #84918b; font-size: 11px; margin-top: 4px; }
.message-meta, .message-actions { display: flex; gap: 8px; align-items: center; }
.message-actions { opacity: 0; }
.assistant-message-content:hover .message-actions { opacity: 1; }
.wait-notice { color: #b7791f; }
@keyframes assistant-cursor { 50% { opacity: 0; } }
.assistant-compose { padding: 14px; }
.assistant-compose-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; color: #7a8682; font-size: 12px; }
.assistant-no-assistant { min-height: 520px; display: grid; place-content: center; justify-items: center; gap: 10px; color: #7a8682; }
.assistant-no-assistant .el-icon { font-size: 34px; color: #e69645; }
.assistant-no-assistant strong { color: #31463f; font-size: 20px; }
.earlier-button { display: block; margin: -8px auto 18px; }
.assistant-dialog-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #edf0ee; }
.assistant-dialog-row strong, .assistant-dialog-row small { display: block; }
.assistant-dialog-row small { color: #7a8682; font-size: 12px; margin-top: 4px; }
.assistant-dialog-divider { margin: 14px 0; border-top: 1px solid #dfe5e2; }
@media (max-width: 900px) { .assistant-layout { grid-template-columns: 220px minmax(0, 1fr); } .assistant-compose-foot { align-items: flex-end; gap: 10px; flex-direction: column; } }
</style>
