<script setup lang="ts">
defineProps<{
  loading: boolean;
  error?: string | null;
  empty?: boolean;
  emptyText?: string;
}>();
defineEmits<{ retry: [] }>();
</script>
<template>
  <div v-if="loading" class="state"><el-skeleton :rows="5" animated /></div>
  <el-result v-else-if="error" icon="error" title="加载失败" :sub-title="error"
    ><template #extra
      ><el-button @click="$emit('retry')">重试</el-button
      ><el-button type="primary" @click="$router.push('/settings')"
        >前往设置</el-button
      ></template
    ></el-result
  ><el-empty v-else-if="empty" :description="emptyText || '暂无内容'" /><slot
    v-else
  />
</template>
