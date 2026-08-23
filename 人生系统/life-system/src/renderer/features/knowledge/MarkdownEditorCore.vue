<script setup lang="ts">
// 编辑器实例只在组件挂载期间存在，父级通过 expose 读取 Markdown，避免把 ProseMirror 对象泄漏到页面状态。
import { Milkdown, useEditor, useInstance } from "@milkdown/vue";
import { Editor, defaultValueCtx, rootCtx } from "@milkdown/kit/core";
import { commonmark } from "@milkdown/kit/preset/commonmark";
import { gfm } from "@milkdown/kit/preset/gfm";
import { getMarkdown as serializeMarkdown } from "@milkdown/kit/utils";

const props = defineProps<{ initialValue: string }>();
const [loading, getEditor] = useInstance();
useEditor((root) => Editor.make()
  .config((ctx) => {
    ctx.set(rootCtx, root);
    ctx.set(defaultValueCtx, props.initialValue);
  })
  .use(commonmark)
  .use(gfm));

function getMarkdown(): string {
  if (loading.value) return props.initialValue;
  const editor = getEditor();
  return editor ? editor.action(serializeMarkdown()) : props.initialValue;
}
defineExpose({ getMarkdown });
</script>

<template><Milkdown /></template>

<style scoped>
:deep(.milkdown) { min-height: 220px; padding: 14px 16px; border: 1px solid #d8e2dc; border-radius: 6px; background: #fff; color: #26352f; line-height: 1.7; }
:deep(.milkdown:focus-within) { border-color: #4d8a75; box-shadow: 0 0 0 2px rgb(77 138 117 / 12%); }
:deep(.milkdown .editor) { outline: none; min-height: 190px; }
:deep(.milkdown h1), :deep(.milkdown h2), :deep(.milkdown h3) { margin: 10px 0 6px; }
:deep(.milkdown pre) { padding: 10px 12px; overflow-x: auto; background: #f5f7f6; border-radius: 4px; }
</style>
