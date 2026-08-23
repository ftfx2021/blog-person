<script setup lang="ts">
// Provider 与编辑器实例分层，确保 useEditor 能拿到 Milkdown 上下文且弹窗关闭时完整销毁。
import { MilkdownProvider } from "@milkdown/vue";
import MarkdownEditorCore from "./MarkdownEditorCore.vue";
import { ref } from "vue";

defineProps<{ initialValue: string }>();
const editor = ref<InstanceType<typeof MarkdownEditorCore>>();
defineExpose({ getMarkdown: () => editor.value?.getMarkdown() ?? "" });
</script>

<template>
  <MilkdownProvider>
    <MarkdownEditorCore ref="editor" :initial-value="initialValue" />
  </MilkdownProvider>
</template>
