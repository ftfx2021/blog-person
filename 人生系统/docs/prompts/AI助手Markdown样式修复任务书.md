# AI 助手 Markdown 渲染样式修复任务书（Prompt）· 段落间距 + 代码块样式 · 给搭建智能体

> 用法：把**整篇**内容作为对话/prompt 发给搭建智能体（全栈开发方向）。工作目录=工程根 `D:\javaweb\个人博客\人生系统\life-system`（注意：**不是**项目根）。
> 前置：补充修复已交付（Vditor 渲染 `src/renderer/shared/MarkdownView.vue`）。本任务是**纯样式调整**，只改 1 个文件。
> 本任务书**自包含**；若能读文件，用法见文末。

---

## 一、角色

你是 Vue3 前端工程师。任务是为 AI 助手消息的 Markdown 渲染**调整样式**：① 段落间距过大；② 代码块太丑（Vditor light 主题默认深底、带行号）。**不要问需求**；只改 `src/renderer/shared/MarkdownView.vue` 一个文件，其余一律不碰。

## 二、问题与目标

- 当前 `MarkdownView.vue` 用 `Vditor.preview`（mode: "light"、`hljs: { lineNumber: true, style: "github" }`），Vditor 默认样式导致：段落 `<p>` 上下 margin ~16px 过大、代码块深底+行号视觉割裂。
- 目标（已与用户确认）：**1A 代码块浅底深字（GitHub 风）；2B 关闭行号**；段落间距收紧。

## 三、实现要求（只改 `MarkdownView.vue`）

### 3.1 关闭行号

`render()` 中 `hljs` 配置：`{ lineNumber: false, style: "github" }`（`style: "github"` 即浅底深字高亮主题，与 1A 一致）。

### 3.2 添加 scoped 样式覆盖 Vditor 渲染类

在 `<script setup>` 后加 `<style scoped>`，用 `:deep()` 覆盖 Vditor 在 `.markdown-view` 内生成的 DOM 样式。**关键规则（参数已拍板，按此实现，可微调 ±2px）**：

```css
.markdown-view :deep(p) { margin: 4px 0; line-height: 1.65; }
.markdown-view :deep(h1) { font-size: 20px; margin: 10px 0 6px; font-weight: 500; }
.markdown-view :deep(h2) { font-size: 18px; margin: 10px 0 6px; font-weight: 500; }
.markdown-view :deep(h3) { font-size: 16px; margin: 8px 0 4px; font-weight: 500; }
.markdown-view :deep(h4) { font-size: 14px; margin: 8px 0 4px; font-weight: 500; }
.markdown-view :deep(pre) {
  background: #f6f8fa; color: #24292f;            /* 浅底深字（GitHub 风） */
  border: 1px solid #e1e4e8; border-radius: 6px;
  padding: 10px 12px; margin: 8px 0;
  font-family: Consolas, "Fira Code", "JetBrains Mono", monospace;
  font-size: 13px; line-height: 1.55;
  overflow-x: auto; white-space: pre;
}
.markdown-view :deep(code) { font-family: Consolas, "Fira Code", "JetBrains Mono", monospace; }
.markdown-view :deep(:not(pre) > code) {   /* 行内代码 */
  background: #eef1f4; color: #24292f;
  padding: 1px 5px; border-radius: 4px; font-size: 12.5px;
}
.markdown-view :deep(ul), .markdown-view :deep(ol) { margin: 4px 0; padding-left: 22px; }
.markdown-view :deep(li) { margin: 2px 0; }
.markdown-view :deep(blockquote) { margin: 6px 0; padding: 2px 12px; border-left: 3px solid #d0d7de; color: #57606a; }
.markdown-view :deep(a) { color: #185fa5; }
.markdown-view :deep(table) { border-collapse: collapse; margin: 8px 0; }
.markdown-view :deep(th), .markdown-view :deep(td) { border: 1px solid #d0d7de; padding: 4px 8px; }
.markdown-view :deep(hr) { margin: 8px 0; border: 0; border-top: 1px solid #e1e4e8; }
```

**注意**：
- 必须用 `:deep()`（Vditor 生成的 DOM 在组件内部但非模板节点）；选择器带 `.markdown-view` 前缀限定作用域，不影响页面其它区域
- 关掉行号后，若 Vditor 仍输出 `.hljs-ln` 相关容器，确认其不影响布局（可一并隐藏）
- 不引入新依赖；不修改 Vditor 的 `index.css` 引入（保留库样式，仅覆盖）

## 四、硬约束

1. **只改** `src/renderer/shared/MarkdownView.vue`；`AssistantPage.vue`、库文件、其它页面一律不碰
2. 不引入新依赖；不改 Vditor 调用结构（仅 `hljs` 参数与新增样式）
3. 颜色与工程浅色主题协调（正文深字 #24312d 系、页面白/浅灰卡片）
4. 代码块**必须**浅底深字、**无行号**
5. **行级注释**：样式块顶部写「做什么+为什么」一句话；禁止模板句灌水
6. **禁止压缩代码**：`build.minify:false` 保持

## 五、验收标准

- [ ] 消息正文段落间距明显收紧（`p` 上下 4px），阅读不再松散
- [ ] 代码块为**浅底深字**（#f6f8fa / #24292f）、无行号、圆角 6px、等宽字体、横向溢出可滚动
- [ ] 行内代码为浅灰底小圆角，与代码块可区分
- [ ] 标题/列表/引用/表格/分隔线样式协调，不与页面其它区域样式冲突
- [ ] 流式生成中纯文本、完成后 Markdown 渲染的行为不变；思考区样式不变
- [ ] `npm run typecheck` ✅；`npm run lint` 0 error ✅；`npm run test` 全绿 ✅

## 六、工作方式

1. 通读 `MarkdownView.vue` 后直接改（单文件，无需大改）
2. 提交一次 git，commit 信息中文（如「修复 AI 助手 Markdown 渲染样式」）

## 用法说明（发给智能体前看）

- 本任务书**自包含**，直接发送全文即可；只动 `src/renderer/shared/MarkdownView.vue`。
- 验收重点：段落紧凑、代码块浅底无行号、与浅色主题协调。
