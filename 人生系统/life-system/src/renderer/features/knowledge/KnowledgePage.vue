<script setup lang="ts">
// 页面保留当前库/文件夹作为所有导入与新建文档的默认归属，避免文档落入错误范围。
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { DocumentAdd, Edit, FolderAdd, Link, MoreFilled, Plus, Upload } from "@element-plus/icons-vue";
import { useApi } from "../../shared/api";
import MarkdownEditor from "./MarkdownEditor.vue";

const { call } = useApi();
const router = useRouter();
const route = useRoute();
const bases = ref<any[]>([]);
const folders = ref<any[]>([]);
const rows = ref<any[]>([]);
const loading = ref(false);
const loadError = ref("");
const keyword = ref("");
const docType = ref("");
const recycle = ref(false);
const selectedKbId = ref(localStorage.getItem("knowledge.lastKbId") || "");
const selectedFolderId = ref<string | null>(localStorage.getItem("knowledge.lastFolderId"));
const selectedFolder = ref<any>();
const urlDialog = ref(false);
const noteDialog = ref(false);
const moveDialog = ref(false);
const baseDialog = ref(false);
const folderDialog = ref(false);
const movingRow = ref<any>();
const url = ref("");
const note = ref<any>({ title: "", rawText: "", docType: "note" });
const noteEditor = ref<{ getMarkdown: () => string }>();
const baseForm = ref<any>({ name: "", description: "", color: "" });
const folderForm = ref<any>({ name: "", parentId: null });
const moveTarget = ref<any>({ kbId: "", folderId: null });
const moveFolders = ref<any[]>([]);
const docTypes = ["webpage", "pdf", "docx", "markdown", "txt", "html", "note", "skill", "prompt"];

function flatten(items: any[], depth = 0): any[] {
  return items.flatMap((item) => [{ ...item, depth }, ...flatten(item.children || [], depth + 1)]);
}
function findFolder(items: any[], id: string | null): any | undefined {
  if (!id) return undefined;
  for (const item of items) {
    if (item.id === id) return item;
    const found = findFolder(item.children || [], id);
    if (found) return found;
  }
  return undefined;
}
const folderOptions = computed(() => flatten(folders.value));
const selectedBase = computed(() => bases.value.find((item) => item.id === selectedKbId.value));
const breadcrumb = computed(() => {
  if (!selectedFolderId.value) return [];
  const path: any[] = [];
  const walk = (items: any[]): boolean => items.some((item) => {
    if (item.id === selectedFolderId.value) { path.push(item); return true; }
    if (walk(item.children || [])) { path.unshift(item); return true; }
    return false;
  });
  walk(folders.value);
  return path;
});

async function loadFolders(): Promise<void> {
  if (!selectedKbId.value) { folders.value = []; return; }
  folders.value = await call(() => window.lifeSystem.knowledge.folder.list(selectedKbId.value));
}
async function load(): Promise<void> {
  loading.value = true;
  loadError.value = "";
  try {
    const result = await call<{ items: any[] }>(() => window.lifeSystem.knowledge.list({
      keyword: keyword.value || undefined, docType: docType.value || undefined,
      tag: typeof route.query.tag === "string" ? route.query.tag : undefined,
      kbId: selectedKbId.value || undefined, folderId: selectedFolderId.value || undefined,
      includeDeleted: recycle.value,
    }));
    rows.value = Array.isArray(result.items) ? result.items : [];
  } catch (error: any) {
    rows.value = [];
    loadError.value = error?.message || "文档列表加载失败";
  } finally { loading.value = false; }
}
async function loadBases(): Promise<void> {
  try {
    bases.value = await call(() => window.lifeSystem.knowledge.kb.list());
    if (!bases.value.some((item) => item.id === selectedKbId.value)) selectedKbId.value = bases.value[0]?.id || "";
    if (selectedKbId.value) localStorage.setItem("knowledge.lastKbId", selectedKbId.value);
    else localStorage.removeItem("knowledge.lastKbId");
    // 标签管理页跳转回来时切换为全局标签视图，避免当前库范围隐藏关联文档。
    if (typeof route.query.tag === "string") {
      selectedKbId.value = "";
      selectedFolderId.value = null;
      selectedFolder.value = undefined;
      localStorage.removeItem("knowledge.lastFolderId");
    }
    await loadFolders();
    // localStorage 可能保留已删除文件夹的 UUID，校验后清理才能避免把无效筛选传给主进程。
    if (selectedFolderId.value && !findFolder(folders.value, selectedFolderId.value)) {
      selectedFolderId.value = null;
      selectedFolder.value = undefined;
      localStorage.removeItem("knowledge.lastFolderId");
    } else if (selectedFolderId.value) {
      selectedFolder.value = findFolder(folders.value, selectedFolderId.value);
    }
    await load();
  } catch (error: any) {
    rows.value = [];
    loadError.value = error?.message || "知识库加载失败";
  }
}
async function selectBase(id: string): Promise<void> {
  selectedKbId.value = id; selectedFolderId.value = null; selectedFolder.value = undefined;
  localStorage.setItem("knowledge.lastKbId", id); localStorage.removeItem("knowledge.lastFolderId");
  await loadFolders(); await load();
}
async function selectFolder(node: any): Promise<void> {
  if (!findFolder(folders.value, node.id)) {
    await clearFolder();
    return;
  }
  selectedFolderId.value = node.id; selectedFolder.value = node;
  localStorage.setItem("knowledge.lastFolderId", node.id); await load();
}
async function clearFolder(): Promise<void> {
  selectedFolderId.value = null; selectedFolder.value = undefined;
  localStorage.removeItem("knowledge.lastFolderId"); await load();
}
async function createBase(): Promise<void> {
  const created: any = await call(() => window.lifeSystem.knowledge.kb.create(baseForm.value));
  baseDialog.value = false; baseForm.value = { name: "", description: "", color: "" };
  await loadBases(); await selectBase(created.id);
}
async function editBase(): Promise<void> {
  if (!selectedBase.value) return;
  const { value } = await ElMessageBox.prompt("知识库名称", "编辑知识库", { inputValue: selectedBase.value.name, inputPattern: /\S+/, inputErrorMessage: "请输入名称" });
  await call(() => window.lifeSystem.knowledge.kb.update({ id: selectedBase.value.id, name: value })); await loadBases();
}
async function removeBase(): Promise<void> {
  if (!selectedBase.value) return;
  await ElMessageBox.confirm("删除前请先移动或删除库内文档。", "删除知识库", { type: "warning" });
  await call(() => window.lifeSystem.knowledge.kb.remove(selectedBase.value.id)); await loadBases();
}
async function createFolder(): Promise<void> {
  await call(() => window.lifeSystem.knowledge.folder.create({ kbId: selectedKbId.value, ...folderForm.value }));
  folderDialog.value = false; folderForm.value = { name: "", parentId: selectedFolderId.value }; await loadFolders();
}
async function renameFolder(): Promise<void> {
  if (!selectedFolder.value) return;
  const { value } = await ElMessageBox.prompt("文件夹名称", "重命名文件夹", { inputValue: selectedFolder.value.name, inputPattern: /\S+/, inputErrorMessage: "请输入名称" });
  await call(() => window.lifeSystem.knowledge.folder.update({ id: selectedFolder.value.id, name: value })); await loadFolders();
}
async function removeFolder(): Promise<void> {
  if (!selectedFolder.value) return;
  await ElMessageBox.confirm("非空文件夹无法删除。", "删除文件夹", { type: "warning" });
  await call(() => window.lifeSystem.knowledge.folder.remove(selectedFolder.value.id)); await clearFolder(); await loadFolders();
}
async function createNote(): Promise<void> {
  const rawText = noteEditor.value?.getMarkdown() ?? "";
  const result = await call<any>(() => window.lifeSystem.knowledge.createNote({ ...note.value, rawText, kbId: selectedKbId.value || undefined, folderId: selectedFolderId.value }));
  noteDialog.value = false; await router.push(`/knowledge/${result.id}`);
}
async function importUrl(): Promise<void> {
  const result = await call<any>(() => window.lifeSystem.knowledge.importUrl({ url: url.value, kbId: selectedKbId.value, folderId: selectedFolderId.value }));
  urlDialog.value = false; await router.push(`/knowledge/${result.documentId}`);
}
async function importFile(): Promise<void> {
  const result = await call<any>(() => window.lifeSystem.knowledge.importFile({ kbId: selectedKbId.value, folderId: selectedFolderId.value }));
  await router.push(`/knowledge/${result.documentId}`);
}
async function loadMoveFolders(kbId: string): Promise<void> {
  moveFolders.value = await call(() => window.lifeSystem.knowledge.folder.list(kbId));
}
async function openMove(row: any): Promise<void> {
  movingRow.value = row; moveTarget.value = { kbId: row.kbId || selectedKbId.value, folderId: row.folderId || null };
  await loadMoveFolders(moveTarget.value.kbId); moveDialog.value = true;
}
async function changeMoveBase(kbId: string): Promise<void> { moveTarget.value.folderId = null; await loadMoveFolders(kbId); }
async function moveDocument(): Promise<void> {
  await call(() => window.lifeSystem.knowledge.moveDocument({ id: movingRow.value.id, ...moveTarget.value }));
  moveDialog.value = false; ElMessage.success("文档已移动"); await loadBases();
}
async function remove(id: string): Promise<void> { await ElMessageBox.confirm("删除后可在回收站恢复，恢复窗口为 30 天。", "删除文档", { type: "warning" }); await call(() => window.lifeSystem.knowledge.remove(id)); await loadBases(); }
async function restore(id: string): Promise<void> { await call(() => window.lifeSystem.knowledge.restore(id)); await loadBases(); }
onMounted(loadBases);
</script>

<template>
  <section class="knowledge-workspace">
    <aside class="knowledge-sidebar">
      <div class="sidebar-title"><h2>知识库</h2><el-button :icon="Plus" circle size="small" title="新建知识库" @click="baseDialog = true" /></div>
      <button v-for="base in bases" :key="base.id" :class="['base-item', { active: selectedKbId === base.id }]" @click="selectBase(base.id)"><span class="base-color" :style="{ backgroundColor: base.color || '#4d8a75' }"></span><span>{{ base.name }}</span><small>{{ base.documentCount }}</small></button>
      <div class="folder-head"><span>文件夹</span><div><el-button :icon="FolderAdd" circle size="small" title="新建文件夹" :disabled="!selectedKbId" @click="folderForm = { name: '', parentId: selectedFolderId }; folderDialog = true" /><el-button :icon="MoreFilled" circle size="small" title="知识库操作" /></div></div>
      <button :class="['folder-root', { active: !selectedFolderId }]" @click="clearFolder">全部文档</button>
      <el-tree :data="folders" node-key="id" :expand-on-click-node="false" :highlight-current="true" :current-node-key="selectedFolderId || undefined" @node-click="selectFolder"><template #default="{ data }"><span class="tree-label">{{ data.name }}</span></template></el-tree>
      <div v-if="selectedFolder" class="folder-actions"><el-button link :icon="Edit" @click="renameFolder">重命名</el-button><el-button link type="danger" @click="removeFolder">删除</el-button></div>
      <div v-if="selectedBase" class="base-actions"><el-button link @click="editBase">编辑知识库</el-button><el-button link type="danger" @click="removeBase">删除</el-button></div>
    </aside>
    <main class="knowledge-main">
      <div class="breadcrumb"><button @click="clearFolder">{{ selectedBase?.name || '知识库' }}</button><template v-for="item in breadcrumb" :key="item.id"><span>/</span><button @click="selectFolder(item)">{{ item.name }}</button></template></div>
      <header class="knowledge-header"><div><h1>{{ selectedFolder?.name || selectedBase?.name || '知识库' }}</h1><p>{{ selectedFolder ? '当前文件夹中的文档' : '当前知识库中的全部文档' }}</p></div><div class="row-actions"><el-button @click="router.push('/knowledge/tags')">标签管理</el-button><el-button :icon="Upload" @click="importFile">导入文件</el-button><el-button :icon="Link" @click="urlDialog = true">导入网页</el-button><el-button type="primary" :icon="DocumentAdd" @click="noteDialog = true">新建文档</el-button></div></header>
      <div class="knowledge-filters"><el-input v-model="keyword" clearable placeholder="在当前范围搜索" @keyup.enter="load" /><el-select v-model="docType" clearable placeholder="全部类型" @change="load"><el-option v-for="type in docTypes" :key="type" :label="type" :value="type" /></el-select><el-checkbox v-model="recycle" @change="load">回收站</el-checkbox></div>
      <div v-if="loadError" class="knowledge-error"><el-alert type="error" :closable="false" :title="loadError" /><el-button type="primary" @click="load">重试</el-button></div>
      <el-table v-else v-loading="loading" :data="rows" empty-text="当前范围暂无文档" @row-click="(row: any) => router.push(`/knowledge/${row.id}`)"><el-table-column prop="title" label="文档" min-width="260" /><el-table-column prop="docType" label="类型" width="110" /><el-table-column label="标签" min-width="150"><template #default="{ row }"><el-tag v-for="tag in row.tags" :key="tag" size="small">{{ tag }}</el-tag></template></el-table-column><el-table-column label="状态" width="180"><template #default="{ row }"><el-tag :type="row.parseStatus === 'ready' ? 'success' : 'danger'">解析 {{ row.parseStatus }}</el-tag><el-tag type="warning">索引 {{ row.indexStatus }}</el-tag></template></el-table-column><el-table-column label="操作" width="160"><template #default="{ row }"><el-button v-if="recycle" link type="primary" @click.stop="restore(row.id)">恢复</el-button><template v-else><el-button link @click.stop="openMove(row)">移动</el-button><el-button link type="danger" @click.stop="remove(row.id)">删除</el-button></template></template></el-table-column></el-table>
    </main>
  </section>
  <el-dialog v-model="baseDialog" title="新建知识库" width="460"><el-form label-position="top"><el-form-item label="名称"><el-input v-model="baseForm.name" /></el-form-item><el-form-item label="说明"><el-input v-model="baseForm.description" type="textarea" :rows="3" /></el-form-item><el-form-item label="标识色"><el-input v-model="baseForm.color" placeholder="#4d8a75" /></el-form-item></el-form><template #footer><el-button @click="baseDialog = false">取消</el-button><el-button type="primary" @click="createBase">创建</el-button></template></el-dialog>
  <el-dialog v-model="folderDialog" title="新建文件夹" width="460"><el-form label-position="top"><el-form-item label="名称"><el-input v-model="folderForm.name" /></el-form-item><el-form-item label="父文件夹"><el-select v-model="folderForm.parentId" clearable placeholder="知识库根目录" style="width: 100%"><el-option v-for="item in folderOptions" :key="item.id" :label="`${'  '.repeat(item.depth)}${item.name}`" :value="item.id" /></el-select></el-form-item></el-form><template #footer><el-button @click="folderDialog = false">取消</el-button><el-button type="primary" @click="createFolder">创建</el-button></template></el-dialog>
  <el-dialog v-model="urlDialog" title="导入网页链接" width="460"><el-input v-model="url" placeholder="https://" /><template #footer><el-button @click="urlDialog = false">取消</el-button><el-button type="primary" @click="importUrl">导入</el-button></template></el-dialog>
  <el-dialog v-model="noteDialog" title="新建手写文档" width="620"><el-form label-position="top"><el-form-item label="标题"><el-input v-model="note.title" /></el-form-item><el-form-item label="类型"><el-select v-model="note.docType"><el-option label="手写文档" value="note" /><el-option label="技能" value="skill" /><el-option label="Prompt" value="prompt" /></el-select></el-form-item><el-form-item label="正文"><MarkdownEditor v-if="noteDialog" ref="noteEditor" :initial-value="note.rawText" /></el-form-item></el-form><template #footer><el-button @click="noteDialog = false">取消</el-button><el-button type="primary" @click="createNote">创建</el-button></template></el-dialog>
  <el-dialog v-model="moveDialog" title="移动文档" width="460"><el-form label-position="top"><el-form-item label="目标知识库"><el-select v-model="moveTarget.kbId" style="width: 100%" @change="changeMoveBase"><el-option v-for="item in bases" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="目标文件夹"><el-select v-model="moveTarget.folderId" clearable placeholder="知识库根目录" style="width: 100%"><el-option v-for="item in flatten(moveFolders)" :key="item.id" :label="`${'  '.repeat(item.depth)}${item.name}`" :value="item.id" /></el-select></el-form-item></el-form><template #footer><el-button @click="moveDialog = false">取消</el-button><el-button type="primary" @click="moveDocument">移动</el-button></template></el-dialog>
</template>

<style scoped>
.knowledge-workspace{display:grid;grid-template-columns:236px minmax(0,1fr);min-height:620px;border:1px solid #dce5df;background:#fff}.knowledge-sidebar{padding:16px 12px;border-right:1px solid #dce5df;background:#f7faf8}.sidebar-title,.folder-head,.knowledge-header,.row-actions{display:flex;align-items:center;justify-content:space-between;gap:8px}.sidebar-title h2{margin:0;font-size:16px}.base-item,.folder-root{display:flex;align-items:center;width:100%;gap:8px;padding:8px;border:0;border-radius:5px;background:transparent;text-align:left;color:#34433c;cursor:pointer}.base-item:hover,.base-item.active,.folder-root.active{background:#e3f0e9;color:#226448}.base-item small{margin-left:auto;color:#708079}.base-color{width:8px;height:8px;border-radius:50%;flex:none}.folder-head{margin:22px 4px 7px;color:#69786f;font-size:12px;font-weight:700}.tree-label{font-size:13px}.folder-actions,.base-actions{display:flex;gap:6px;margin:8px 2px}.base-actions{margin-top:20px}.knowledge-main{min-width:0;padding:24px}.knowledge-error{display:flex;align-items:center;gap:12px;margin-top:16px}.breadcrumb{display:flex;gap:8px;margin-bottom:14px;color:#718078;font-size:13px}.breadcrumb button{padding:0;border:0;background:none;color:#4e7664;cursor:pointer}.knowledge-header{margin-bottom:20px}.knowledge-header h1{margin:0;font-size:24px}.knowledge-header p{margin:5px 0 0;color:#718078;font-size:13px}.knowledge-filters{display:grid;grid-template-columns:minmax(180px,1fr) 150px auto;gap:12px;margin-bottom:16px}.el-tag+.el-tag{margin-left:5px}@media(max-width:800px){.knowledge-workspace{grid-template-columns:1fr}.knowledge-sidebar{border-right:0;border-bottom:1px solid #dce5df}.knowledge-main{padding:16px}.knowledge-header{align-items:flex-start;flex-direction:column}.knowledge-filters{grid-template-columns:1fr}.base-actions{margin-top:10px}}
</style>
