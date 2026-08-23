<script setup lang="ts">
// 标签在所有知识库间共享，本页只管理标签本身，不改变文档归属。
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { useApi } from "../../shared/api";

const { call } = useApi();
const router = useRouter();
const tags = ref<any[]>([]);
async function load(): Promise<void> { tags.value = await call(() => window.lifeSystem.tags.list()); }
async function rename(tag: any): Promise<void> {
  const { value } = await ElMessageBox.prompt("标签名称", "重命名标签", { inputValue: tag.name, inputPattern: /\S+/, inputErrorMessage: "请输入标签名称" });
  const result: any = await call(() => window.lifeSystem.tags.rename({ id: tag.id, name: value }));
  ElMessage.success(result.merged ? "标签已合并" : "标签已重命名"); await load();
}
async function remove(tag: any): Promise<void> {
  await ElMessageBox.confirm(`删除标签“${tag.name}”只会解除关联，不会删除文档。`, "删除标签", { type: "warning" });
  await call(() => window.lifeSystem.tags.remove(tag.id)); await load();
}
function browse(tag: any): void { router.push({ path: "/knowledge", query: { tag: tag.name } }); }
onMounted(load);
</script>
<template>
  <section class="tag-page">
    <header><div><el-button link @click="router.push('/knowledge')">返回知识库</el-button><h1>标签管理</h1><p>标签跨知识库关联文档。</p></div></header>
    <el-table :data="tags" empty-text="暂无标签"><el-table-column prop="name" label="标签" min-width="240"><template #default="{ row }"><el-tag>{{ row.name }}</el-tag></template></el-table-column><el-table-column prop="documentCount" label="文档数" width="120" /><el-table-column label="操作" width="200"><template #default="{ row }"><el-button link type="primary" @click="browse(row)">查看文档</el-button><el-button link @click="rename(row)">重命名</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column></el-table>
  </section>
</template>
<style scoped>.tag-page{max-width:900px}.tag-page header{margin-bottom:22px}.tag-page h1{margin:6px 0;font-size:25px}.tag-page p{margin:0;color:#718078;font-size:13px}</style>
