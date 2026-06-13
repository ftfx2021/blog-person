<template>
  <el-dialog
    v-model="dialogVisible"
    title="搜索"
    width="500px"
    :close-on-click-modal="true"
    :lock-scroll="false"
    class="search-dialog"
  >
    <div class="search-content">
      <el-input
        v-model="searchKeyword"
        placeholder="请输入搜索关键词，按 Enter 搜索..."
        size="large"
        clearable
        autofocus
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const searchKeyword = ref('')

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 监听对话框打开，清空搜索关键词
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    searchKeyword.value = ''
  }
})

// 执行搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  // 关闭对话框
  dialogVisible.value = false
  
  // 跳转到搜索结果页
  router.push({
    path: '/search',
    query: { keyword: searchKeyword.value }
  })
}
</script>

<style lang="scss" scoped>
:deep(.search-dialog) {
  .el-dialog__header {
    padding: 20px;
    
    .el-dialog__title {
      font-size: 16px;
      font-weight: 600;
      color: #2D3748;
    }
  }
  
  .el-dialog__body {
    padding: 0 20px 20px;
  }
}

.search-content {
  .el-input {
    :deep(.el-input__wrapper) {
      padding: 10px 15px;
    }
    
    :deep(.el-input__inner) {
      font-size: 15px;
    }
    
    :deep(.el-input__suffix) {
      margin-right: -8px;
    }
  }
}
</style>
