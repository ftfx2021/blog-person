<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import Vditor from "vditor";
import "vditor/dist/index.css";

const props = defineProps<{ content: string }>();
const element = ref<HTMLDivElement>();
let renderVersion = 0;

// 使用 Vditor 的只读预览 API，统一交给成熟库处理 Markdown、代码高亮和安全过滤。
async function render(): Promise<void> {
  if (!element.value) return;
  const version = ++renderVersion;
  element.value.replaceChildren();
  try {
    await Vditor.preview(element.value, props.content, {
      // 使用 renderer 同源的静态 dist 目录，禁止 Vditor 回退到 unpkg 等外部脚本。
      cdn: ".",
      mode: "light",
      hljs: { lineNumber: false, style: "github" },
    });
  } catch {
    // 本地资源异常时降级为纯文本，避免动态脚本加载失败造成未捕获 Promise。
    element.value.textContent = props.content;
  }
  // 旧异步渲染晚于新内容完成时，再渲染一次最新内容，避免旧结果覆盖最终消息。
  if (version !== renderVersion) void render();
}

onMounted(() => void render());
watch(() => props.content, () => void render());
onBeforeUnmount(() => {
  renderVersion += 1;
  element.value?.replaceChildren();
});
</script>

<template>
  <div ref="element" class="markdown-view" />
</template>

<style scoped>
/* 收紧 Vditor 预览内容的阅读节奏，并将代码区域统一为页面协调的 GitHub 浅色风格。 */
.markdown-view :deep(p) {
  margin: 4px 0;
  line-height: 1.65;
}

.markdown-view :deep(h1) {
  font-size: 20px;
  margin: 10px 0 6px;
  font-weight: 500;
}

.markdown-view :deep(h2) {
  font-size: 18px;
  margin: 10px 0 6px;
  font-weight: 500;
}

.markdown-view :deep(h3) {
  font-size: 16px;
  margin: 8px 0 4px;
  font-weight: 500;
}

.markdown-view :deep(h4) {
  font-size: 14px;
  margin: 8px 0 4px;
  font-weight: 500;
}

.markdown-view :deep(pre) {
  background: #f6f8fa;
  color: #24292f;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  padding: 10px 12px;
  margin: 8px 0;
  font-family: Consolas, "Fira Code", "JetBrains Mono", monospace;
  font-size: 13px;
  line-height: 1.55;
  overflow-x: auto;
  white-space: pre;
}

.markdown-view :deep(code) {
  font-family: Consolas, "Fira Code", "JetBrains Mono", monospace;
}

.markdown-view :deep(:not(pre) > code) {
  background: #eef1f4;
  color: #24292f;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 12.5px;
}

.markdown-view :deep(ul),
.markdown-view :deep(ol) {
  margin: 4px 0;
  padding-left: 22px;
}

.markdown-view :deep(li) {
  margin: 2px 0;
}

.markdown-view :deep(blockquote) {
  margin: 6px 0;
  padding: 2px 12px;
  border-left: 3px solid #d0d7de;
  color: #57606a;
}

.markdown-view :deep(a) {
  color: #185fa5;
}

.markdown-view :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
}

.markdown-view :deep(th),
.markdown-view :deep(td) {
  border: 1px solid #d0d7de;
  padding: 4px 8px;
}

.markdown-view :deep(hr) {
  margin: 8px 0;
  border: 0;
  border-top: 1px solid #e1e4e8;
}

.markdown-view :deep(.hljs-ln) {
  display: block;
}
</style>
