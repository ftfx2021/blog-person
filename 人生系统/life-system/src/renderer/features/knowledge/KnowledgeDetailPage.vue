<script setup lang="ts">
// 详情页保存正文时统一调用 knowledge.update，主进程负责完整分块替换。
import { onMounted, ref, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Download, FolderOpened } from "@element-plus/icons-vue";
import { useApi } from "../../shared/api";
import MarkdownEditor from "./MarkdownEditor.vue";
import MarkdownView from "../../shared/MarkdownView.vue";
const { call } = useApi(); const route = useRoute(); const router = useRouter(); const document = ref<any>(); const editing = ref(false); const content = ref(""); const editor = ref<{ getMarkdown: () => string }>();
async function load(): Promise<void> { document.value = await call(() => window.lifeSystem.knowledge.get(String(route.params.id))); content.value = document.value.rawText; }
async function startEditing(): Promise<void> { content.value = document.value?.rawText ?? ""; editing.value = true; await nextTick(); }
function cancelEditing(): void { editing.value = false; content.value = document.value?.rawText ?? ""; }
async function save(): Promise<void> { const rawText = editor.value?.getMarkdown() ?? content.value; document.value = await call(() => window.lifeSystem.knowledge.update({ id: document.value.id, rawText })); content.value = rawText; editing.value = false; ElMessage.success("已保存并重新分块"); }
async function exportDocument(format: "pdf" | "markdown" | "txt"): Promise<void> { const result = await call(() => window.lifeSystem.knowledge.export({ id: document.value.id, format })); ElMessage.success(`已导出到 ${result.path}`); }
async function copyPrompt(): Promise<void> { await call(() => window.lifeSystem.knowledge.copyPrompt(document.value.id)); ElMessage.success("已复制到剪贴板"); }
async function openOriginal(): Promise<void> { await call(() => window.lifeSystem.knowledge.openOriginal(document.value.id)); }
async function saveOriginal(): Promise<void> { const result = await call(() => window.lifeSystem.knowledge.saveOriginal(document.value.id)); if (result?.path) ElMessage.success(`已另存至 ${result.path}`); }
onMounted(load);
</script>
<template><section v-if="document" class="detail-page"><header class="detail-header"><div><el-button link @click="router.push('/knowledge')">返回知识库</el-button><h1>{{ document.title }}</h1><div class="status-line">解析 {{ document.parseStatus }} · 索引 {{ document.indexStatus }}</div></div><div class="row-actions"><el-tooltip content="无原件" :disabled="Boolean(document.storedPath)"><el-button :icon="FolderOpened" :disabled="!document.storedPath" @click="openOriginal">打开原件</el-button></el-tooltip><el-tooltip content="无原件" :disabled="Boolean(document.storedPath)"><el-button :icon="Download" :disabled="!document.storedPath" @click="saveOriginal">另存原件</el-button></el-tooltip><el-button v-if="!editing" @click="startEditing">编辑</el-button><el-button v-else @click="cancelEditing">取消编辑</el-button><el-button @click="exportDocument('markdown')">导出 MD</el-button><el-button @click="exportDocument('txt')">导出 TXT</el-button><el-button v-if="document.docType === 'prompt'" type="primary" @click="copyPrompt">复制</el-button></div></header><MarkdownEditor v-if="editing" ref="editor" :initial-value="content" /><MarkdownView v-else :content="document.rawText || ''" /><div v-if="editing" class="row-actions save-bar"><el-button type="primary" @click="save">保存</el-button></div></section></template>
<style scoped>.detail-header{display:flex;justify-content:space-between;gap:24px;margin-bottom:24px}.detail-header h1{margin:8px 0}.status-line{color:#78837f;font-size:13px}.document-content{margin:0;white-space:pre-wrap;font:14px/1.8 ui-monospace,SFMono-Regular,Consolas,monospace;color:#26352f}.save-bar{margin-top:16px}@media(max-width:760px){.detail-header{flex-direction:column}}</style>
