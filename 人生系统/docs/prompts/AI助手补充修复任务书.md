# AI 助手补充修复任务书（Prompt）· 渲染换现成库 + 会话标题异步生成 · 给搭建智能体

> 用法：把**整篇**内容作为对话/prompt 发给搭建智能体（全栈开发方向）。工作目录=工程根 `D:\javaweb\个人博客\人生系统\life-system`（注意：**不是**项目根）。
> 前置：**P1-8 体验修复已交付并验收**（F-1~F-17 已实现）。本任务是对其中两处做**补充修正**，独立于 `P1-8修复任务书.md`（该任务书保持原样不动）。
> **检查结论（2026-08-21，下发前已核对）**：超时阈值放宽——原拟修正项 **已满足，不执行**：`openai-compatible.ts` 当前 `CONNECT_TIMEOUT_MS=10s`（建连兜底）、**首字不设超时**（`readSse` 中 `receivedEvent ? STREAM_IDLE_TIMEOUT_MS : undefined`，首事件前直接 `reader.read()` 无限等待）、`STREAM_IDLE_TIMEOUT_MS=300s`。慢模型/深度思考误杀问题已解决，**不要改动超时相关代码**。
> 本任务书**自包含**；若能读文件，用法见文末。

---

## 一、角色

你是全栈工程师，专长 Electron + Vue3 + TypeScript + Node。任务是为「人生系统」AI 助手做**两处补充修正**：① Markdown 渲染改用**现成完整库**（替代手搓渲染封装）；② 会话标题改为**根据第一轮对话内容异步生成**（LLM 生成，替代截取式）。**不要问需求**；无法裁决记入 `life-system/docs/decisions/` 并继续。

## 二、任务总览（两项修正）

1. **Markdown 渲染换现成库**：当前 `src/renderer/shared/markdown.ts` 用 `markdown-it` + `dompurify` 手搓封装（含自写 LRU 缓存与样式），改为**现成完整渲染库 Vditor**（内置代码高亮、XSS 过滤、样式），删除手搓封装；流式期间仍纯文本、完成后整条渲染的策略不变
2. **会话标题异步生成**：当前标题是首条用户消息**截取前 20 字**（`appendMessage` 内同步完成），体验差（如「RAG 检索增强生成入门 RAG 检」这类截断）。改为：第一轮对话完成（用户首问 + AI 首答都落库）后，**后台异步调用 LLM 生成简短标题**（≤20 字），生成成功即更新会话标题并广播到 UI；失败静默保留「新会话」，不阻塞、不打扰

**核心思想**：渲染用成熟库，不自己造轮子；标题交给 LLM 理解对话后生成，不用字符串截断。

## 三、必读材料

| # | 材料 | 作用 |
|---|---|---|
| 1 | `src/main/modules/assistant/service.ts` | `appendMessage`（**移除截取式自动命名**）、`messages`/`renameSession`；新增 `generateSessionTitle` |
| 2 | `src/main/modules/chat/service.ts` | `run` 完成处（触发标题生成）；**超时相关代码不要动** |
| 3 | `src/main/infrastructure/llm/types.ts` | `ChatRequest`、`LlmProvider.chat`（标题生成复用） |
| 4 | `src/renderer/features/assistant/AssistantPage.vue` | `renderMarkdown` 调用点（换库后替换）；`refreshSessions`/会话列表（订阅标题更新事件） |
| 5 | `src/renderer/shared/markdown.ts` | 当前手搓封装（删除，由组件替代） |
| 6 | `src/preload/api/index.ts` | 新增标题更新事件订阅（如 `sessions.onTitleUpdated`） |
| 7 | `package.json` | 移除 `markdown-it`/`dompurify`，新增 `vditor` |

## 四、范围（只做这两项，一个不多）

| 编号 | 内容 | 说明 |
|---|---|---|
| T-1 | Markdown 渲染换 Vditor（现成库），删除手搓封装 | §5.1 |
| T-2 | 会话标题根据第一轮对话内容异步生成（LLM） | §5.2 |

**明确不做**：不改错误码体系；不改流式/思考区/首字探测等其余 F-1~F-17 逻辑；**不改超时相关代码**（已检查满足）；不做代码高亮库单独集成（Vditor 已内置）；不做标题生成的 UI 编辑入口（沿用既有重命名按钮）；不动 `migrations`；标题生成不引入独立配置项（复用全局 LLM 配置）。

## 五、实现要求

### 5.1 Markdown 渲染换现成库 Vditor（T-1）

**选型理由**：Vditor 是现成完整 Markdown 渲染/编辑库，内置代码高亮、XSS 过滤（sanitize）与样式，一条 API 完成渲染，无需自己拼 markdown-it + dompurify + 手写缓存/样式。与架构文档选型（Milkdown 用于**编辑**）不冲突——本任务管**只读渲染**，未来手写文档编辑器（K02-F06）再引入 Milkdown。

**依赖变更**：
- 新增：`vditor`（最新稳定版）
- 移除：`markdown-it`、`dompurify` 及 `@types/markdown-it`、`@types/dompurify`（不再需要）
- 样式：渲染层入口引入 `vditor/dist/index.css`（或按需），不手写 markdown 样式

**实现**：
- **删除** `src/renderer/shared/markdown.ts`（手搓封装不再使用）
- 新建 `src/renderer/shared/MarkdownView.vue`（轻组件，只做库调用封装，**不手写解析/样式**）：

```vue
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import Vditor from "vditor";

const props = defineProps<{ content: string }>();
const element = ref<HTMLElement>();
let preview: Vditor | undefined;

// 每次 content 变化调用现成预览 API，不自行解析 Markdown。
function render() {
  if (!element.value) return;
  preview?.destroy();
  preview = new Vditor(element.value, {
    mode: "ir",          // 即时渲染模式：按需预览
    preview: { theme: "light", hljs: { lineNumber: true, style: "github" } },
    cache: { enable: false },   // 只读渲染不需要本地缓存
    toolbar: [],               // 只读，不显示工具栏
  });
  // 或使用 Vditor.preview(element, content, options) 静态方法按内容渲染
}
onMounted(render);
watch(() => props.content, render);
onBeforeUnmount(() => preview?.destroy());
</script>
<template><div ref="element" class="markdown-view" /></template>
```

> 注：以上为最小集成示意。以 Vditor 当前版本文档为准（`Vditor.preview` 静态方法或 `mode: "ir"` 实例二选一），**目标是开箱即用的现成渲染**；禁止手写正则/解析器、禁止自写 CSS 覆盖库样式（可少量微调间距）。

- `AssistantPage.vue`：
  - 删除 `renderMarkdown` 导入与 `v-html` 用法
  - 已完成（非流式）的 assistant 消息气泡：用 `<MarkdownView :content="message.content" />` 替代
  - 流式中的消息：保持纯文本（现有逻辑不变）；`handleDone` 后 content 更新触发 MarkdownView 重渲染
  - 思考区（`reasoning`）：**保持纯文本 pre 渲染**（思考过程不需要 markdown；不改）
  - 消息复用 `MarkdownView` 的 `:key` 用 `message.id`，历史消息切换时正常渲染

### 5.2 会话标题异步生成（T-2）

**问题**：当前 `appendMessage` 内把首条用户消息截取前 20 字当标题，产生「RAG 检索增强生成入门 RAG 检」这类截断标题，观感差。

**设计**：第一轮对话完成（用户首问 + AI 首答都落库）后，后台异步调用 LLM 生成简短标题；失败静默保留「新会话」，**不阻塞、不打扰、不重试打扰用户**。

**改动**：

`src/main/modules/assistant/service.ts`：
- **移除** `appendMessage` 内的截取式自动命名（删掉 `title='新会话'` 时 `content.slice(0, 20)` 的逻辑；`appendMessage` 只负责落库 + 更新 `updated_at`）
- 新增 `generateSessionTitle(sessionId): Promise<SessionRow | null>`：
  1. 读会话，**仅当 `title='新会话'` 时继续**（用户已重命名则跳过）
  2. 读该会话**第一轮**消息：最新一条 user 消息 + 其后的第一条 assistant 消息（各截断 ≤2000 字符，防请求超长）
  3. 复用 `settingsService.getActiveChatConfig()` 取全局 LLM 配置；无配置 → 返回 null（静默）
  4. `createLlmProvider(config).chat({ messages: [{ role: "user", content: 标题生成提示词 }], model, maxTokens: 50, temperature: 0.3 })`（**非流式**，短请求）
  5. 结果清洗：去引号/换行/多余空白，截断 ≤30 字；非空才 `UPDATE ai_session SET title=?, updated_at=?`；空结果返回 null
  6. 失败/超时：**静默返回 null**（不抛错、不重试）
- 标题生成提示词（内嵌，直接使用）：

```
根据下面的第一轮对话，生成一个不超过 20 个字的会话标题。
只输出标题本身，不要引号、标点或任何解释。
用户：{首问}
助手：{首答}
```

`src/main/modules/chat/service.ts`——`run` 完成后（`sendDone` 之后、`finally` 之前）触发：

```ts
// 标题生成与用户操作完全解耦：fire-and-forget，失败静默不影响主流程。
if (!active.controller.signal.aborted && active.text)
  void assistantService.generateSessionTitle(sessionId).catch(() => undefined);
```

- 生成成功后的 **UI 刷新**（广播事件，不轮询）：
  - 主进程标题更新后：`BrowserWindow.getAllWindows().forEach(w => w.webContents.send("sessions:title-updated", { id: sessionId, title }))`
  - Preload `sessions.onTitleUpdated(callback)`：订阅 `sessions:title-updated`，返回取消函数（沿用 `subscribe` 模式）
  - `AssistantPage.vue`：订阅该事件，命中当前 `assistantId` 的会话列表时原地更新该项 title（不整表刷新、不跳转）

**注意**：生成时机依赖「第一轮」定义——本实现取**最新一轮 user+assistant**（对首轮即满足；若首轮标题未生成成功而用户又发了第二轮，会以第二轮内容补生成一次——可接受，任务书按此实现即可）。

## 六、硬约束（违反即返工）

1. 渲染层禁入 mysql2/Milvus SDK/fs/electron 主进程 API；所有数据访问经 preload
2. 所有 IPC channel 必须有 Zod schema；标题更新用**主进程广播事件**（`webContents.send`），**不新增**请求-响应 IPC 通道
3. **依赖边界**：允许新增 `vditor`；**必须移除** `markdown-it`/`dompurify` 及其类型包；仍严禁 LangChain/LlamaIndex/ai-sdk/openai 及独立高亮库；标题生成**零新增依赖**（复用既有 `createLlmProvider`）
4. **不改超时相关代码**（已检查满足：首字不设超时、流空闲 300s、connect 10s）；不新增 `timeouts` 字段
5. **行级注释**：新代码逐逻辑块中文注释，写「做什么+为什么」；禁止模板句灌水
6. **禁止压缩代码**：`build.minify:false` 保持
7. 中文界面与注释；不引入商业/交付黑话
8. **不动已验收逻辑**：F-1~F-17 一律不碰（本任务只碰渲染调用点、自动命名逻辑）；`ai_message` 表结构与迁移不动
9. 标题生成**只读对话内容**用于 LLM 请求，情绪数据绝不进入标题生成输入；失败静默不弹错

## 七、明确不做

- 不改错误码、流式、思考区、首字探测、token 预算、超时等其余逻辑
- 不引入 Milkdown（未来手写文档编辑器用）；不用 `marked`/`react-markdown` 等其它库
- 不做 Markdown 编辑（只读渲染）
- 不改 `migrations` / DTO；不新增请求-响应 IPC（标题更新仅广播事件）
- 不做标题生成的设置项/开关；不做失败重试队列

## 八、验收标准（全部满足才算完成）

- [ ] `npm run dev` 启动正常；AI 助手页消息气泡正常显示（标题/列表/代码块/链接渲染正确，代码有高亮）
- [ ] **恶意输入安全**：构造含 `<script>`/`onerror` 的测试消息 → 不执行、被清洗（Vditor sanitize 生效）
- [ ] **流式行为不变**：生成中纯文本实时显示；完成后整条切换为 Vditor 渲染；无闪烁/无重复解析卡顿
- [ ] 思考区仍为纯文本折叠面板，行为与渲染均不变
- [ ] **标题异步生成**：新会话首轮对话完成后，左侧会话列表标题在数秒内变为 LLM 生成的简短标题（不再是消息前 20 字的截断）；对话过程不卡顿
- [ ] **标题幂等**：用户手动重命名后，后续对话**不覆盖**该标题；LLM 未配置/失败时标题保持「新会话」且无错误提示、无重试风暴
- [ ] **UI 刷新**：标题更新后当前助手会话列表原地更新，不整表刷新、不跳转
- [ ] `npm ls` 确认 `markdown-it`/`dompurify` 已移除、`vditor` 已安装；无 langchain 等框架
- [ ] 代码抽查：`appendMessage` 已无截取式自动命名；`markdown.ts` 已删除；`AssistantPage.vue` 无 `v-html`/`renderMarkdown` 残留；**超时相关文件（`openai-compatible.ts`）未被改动**
- [ ] `npm run typecheck` ✅；`npm run lint` 0 error ✅；`npm run test` 全绿 ✅

## 九、技术决策（已锁定，直接执行）

| 项 | 决策 |
|---|---|
| 超时（已满足，不动） | 现状：connect 10s / 首字无限等待 / 流空闲 300s；**本任务不改** |
| Markdown 渲染 | **Vditor**（现成完整库，内置高亮+sanitize+样式）；删除 markdown-it/dompurify 手搓封装 |
| 流式策略 | 流式纯文本 → 完成后整条 Vditor 渲染（不变） |
| 思考区 | 纯文本 pre（不渲染 markdown，不变） |
| 标题生成 | 第一轮 user+assistant 内容 → LLM 非流式短请求（maxTokens 50）→ ≤30 字标题；仅 `title='新会话'` 时生成；失败静默不重试 |
| 标题刷新 | 主进程广播 `sessions:title-updated` 事件；preload 订阅；UI 原地更新列表项（不轮询） |

## 十、工作方式

1. 先通读任务书 + 既有代码（`assistant/service.ts`、`chat/service.ts`、`preload/api/index.ts`、`AssistantPage.vue`、`markdown.ts`、`package.json`）再动笔
2. 顺序：npm 依赖变更（装 vditor、卸 markdown-it/dompurify）→ 标题生成（assistant service → chat service 触发 → 广播事件 → preload → 页面订阅）→ MarkdownView.vue → AssistantPage 替换 → 自测
3. 每完成一个模块提交一次 git，commit 信息中文；接口变更记入 `life-system/docs/decisions/`
4. 所有 TODO 用 `// TODO(P1-x): ...` 标注

---

## 附录：改动文件清单

| 文件 | 改动 |
|---|---|
| `src/main/modules/assistant/service.ts` | **移除**截取式自动命名；新增 `generateSessionTitle`（读首轮 → LLM → 更新标题，仅 `title='新会话'` 时） |
| `src/main/modules/chat/service.ts` | `run` 完成后 fire-and-forget 触发 `generateSessionTitle`；标题更新后广播 `sessions:title-updated`（**不改超时/流式逻辑**） |
| `src/preload/api/index.ts` | 新增 `sessions.onTitleUpdated(callback)` 事件订阅（返回取消函数） |
| `src/renderer/shared/markdown.ts` | **删除**（手搓封装移除） |
| `src/renderer/shared/MarkdownView.vue` | 新增：Vditor 只读渲染组件（现成库调用） |
| `src/renderer/features/assistant/AssistantPage.vue` | 移除 `renderMarkdown`/`v-html`，assistant 消息用 `MarkdownView`；订阅 `onTitleUpdated` 原地更新会话列表项 |
| `package.json` | 新增 `vditor`；移除 `markdown-it`/`dompurify`（含类型包） |
