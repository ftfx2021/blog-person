<template>
  <el-dialog
    v-model="dialogVisible"
    title="文章操作"
    width="360px"
    :close-on-click-modal="true"
    :lock-scroll="false"
    class="action-dialog"
  >
    <div class="action-list">
      <div class="action-item" @click="handleEdit">
        <el-icon class="action-icon edit-icon"><Edit /></el-icon>
        <div class="action-title">编辑</div>
      </div>
      
      <div class="action-item" @click="handleToggleVisibility" :class="{ 'loading': visibilityLoading }">
        <template v-if="!visibilityLoading">
          <el-icon class="action-icon hide-icon" v-if="articleVisible"><Hide /></el-icon>
          <el-icon class="action-icon show-icon" v-else><View /></el-icon>
        </template>
        <el-icon class="action-icon hide-icon el-icon-loading" v-else><Loading /></el-icon>
        <div class="action-title">{{ articleVisible ? '隐藏' : '显示' }}</div>
      </div>
      
      <div class="action-item" @click="handleShare">
        <el-icon class="action-icon share-icon"><Share /></el-icon>
        <div class="action-title">分享</div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Edit, Hide, Share, Loading, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { updateArticleStatus, getArticleDetail } from '@/api/ArticleApi'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  articleId: {
    type: [String, Number],
    required: true
  }
})

const emit = defineEmits(['update:modelValue'])

const route = useRoute()
const router = useRouter()

// 文章可见性状态
const articleVisible = ref(true)
// 加载状态
const visibilityLoading = ref(false)
// 文章数据
const articleData = ref(null)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 监听对话框打开，获取文章状态
watch(() => props.modelValue, (newVal) => {
  if (newVal && props.articleId) {
    fetchArticleStatus()
  }
})

// 获取文章状态
const fetchArticleStatus = () => {
  getArticleDetail(props.articleId, {}, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      // 根据实际后端字段调整，假设 status=1 表示已发布/可见
      articleVisible.value = data.status === 1
      // 保存文章数据，用于分享功能
      articleData.value = data
    },
    onError: (error) => {
      console.error('获取文章状态失败:', error)
    }
  })
}

// 处理编辑操作
const handleEdit = () => {
  dialogVisible.value = false
  router.push(`/back/article/edit/${props.articleId}`)
}

// 处理隐藏/显示操作
const handleToggleVisibility = () => {
  if (visibilityLoading.value) return
  
  visibilityLoading.value = true
  
  // status: 0=草稿/隐藏, 1=已发布/显示
  const newStatus = articleVisible.value ? 0 : 1
  
  updateArticleStatus(props.articleId, newStatus, {
    successMsg: articleVisible.value ? '文章已隐藏' : '文章已显示',
    onSuccess: () => {
      // 更新本地状态
      articleVisible.value = !articleVisible.value
      visibilityLoading.value = false
      // 不关闭对话框，让用户可以继续操作
    },
    onError: (error) => {
      console.error('切换可见性失败:', error)
      visibilityLoading.value = false
    }
  })
}

// 处理分享操作
const handleShare = async () => {
  // 生成文章链接
  const articleUrl = `${window.location.origin}/article/${props.articleId}`
  
  // 构建分享内容
  let shareContent = `文章链接：${articleUrl}`
  
  // 如果文章有密码保护，添加密码信息
  if (articleData.value?.isPasswordProtected === 1 && articleData.value?.accessPassword) {
    shareContent += `\n访问密码：${articleData.value.accessPassword}`
  }
  
  try {
    // 优先使用现代 Clipboard API
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(shareContent)
    } else {
      // 降级方案：使用传统的 document.execCommand
      const textarea = document.createElement('textarea')
      textarea.value = shareContent
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    
    // 显示成功提示
    if (articleData.value?.isPasswordProtected === 1) {
      ElMessage.success('文章链接和密码已复制到剪贴板')
    } else {
      ElMessage.success('文章链接已复制到剪贴板')
    }
  } catch (err) {
    console.error('复制失败:', err)
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style lang="scss" scoped>
// 操作对话框样式
:deep(.action-dialog) {
  .el-dialog__header {
    padding: 20px 20px 15px;
    border-bottom: 1px solid #f0f0f0;
    
    .el-dialog__title {
      font-size: 16px;
      font-weight: 600;
      color: #2D3748;
    }
  }
  
  .el-dialog__body {
    padding: 20px;
  }
}

.action-list {
  display: flex;
  justify-content: space-around;
  gap: 16px;
  
  .action-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 20px 12px;
    cursor: pointer;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.6);
    border: 1px solid rgba(0, 0, 0, 0.06);
    
    &:hover:not(.loading) {
      background: rgba(64, 158, 255, 0.05);
      border-color: rgba(64, 158, 255, 0.2);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }
    
    &:active:not(.loading) {
      transform: translateY(0);
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
    }
    
    &.loading {
      pointer-events: none;
    }
    
    .action-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      flex-shrink: 0;
      transition: all 0.3s ease;
      
      &.edit-icon {
        background: rgba(246, 173, 85, 0.15);
        color: #F6AD55;
      }
      
      &.hide-icon {
        background: rgba(245, 101, 101, 0.15);
        color: #F56565;
      }
      
      &.show-icon {
        background: rgba(72, 187, 120, 0.15);
        color: #48BB78;
      }
      
      &.share-icon {
        background: rgba(66, 153, 225, 0.15);
        color: #4299E1;
      }
      
      &.el-icon-loading {
        animation: rotating 1s linear infinite;
      }
    }
    
    .action-title {
      font-size: 14px;
      font-weight: 500;
      color: #2D3748;
      text-align: center;
    }
  }
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
