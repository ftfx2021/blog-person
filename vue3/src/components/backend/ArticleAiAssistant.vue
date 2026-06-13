<template>
  <div class="ai-assistant">
    <el-card class="ai-card">
      <template #header>
        <div class="card-header">
          <el-icon class="ai-icon"><MagicStick /></el-icon>
          <span>AI 智能助手</span>
        </div>
      </template>
      
      <div class="ai-actions">
        <!-- 智能生成标题 -->
        <el-button 
          type="primary" 
          :icon="Edit" 
          size="small" 
          :loading="loading.title"
          @click="generateTitle"
          :disabled="!hasContent"
        >
          生成标题
        </el-button>
        
        <!-- 智能生成摘要 -->
        <el-button 
          type="success" 
          :icon="Document" 
          size="small"
          :loading="loading.summary"
          @click="generateSummary"
          :disabled="!hasContent"
        >
          生成摘要
        </el-button>
        
        <!-- 智能生成大纲 -->
        <el-button 
          type="info" 
          :icon="List" 
          size="small"
          :loading="loading.outline"
          @click="generateOutline"
          :disabled="!hasContent"
        >
          生成大纲
        </el-button>
        
        <!-- 推荐标签 -->
        <el-button 
          type="warning" 
          :icon="PriceTag" 
          size="small"
          :loading="loading.tags"
          @click="recommendTags"
          :disabled="!hasContent"
        >
          推荐标签
        </el-button>
        
        <!-- 推荐分类 -->
        <el-button 
          type="danger" 
          :icon="FolderOpened" 
          size="small"
          :loading="loading.category"
          @click="recommendCategory"
          :disabled="!hasContent"
        >
          推荐分类
        </el-button>
        
      </div>
      
      <!-- 内容长度和超时提示 -->
      <div class="content-info" v-if="contentLength > 0">
        <el-tag size="small" type="info">
          内容长度: {{ contentLength }} 字
        </el-tag>
        <el-tag size="small" type="warning" v-if="timeoutConfig.timeoutSeconds > 60">
          预计处理时间: {{ timeoutConfig.timeoutSeconds }}秒
        </el-tag>
      </div>
      
      <!-- 没有内容时的提示 -->
      <div class="no-content-tip" v-if="!hasContent">
        <el-alert
          :title="`请先输入文章内容（至少20字，当前${contentLength}字）`"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
    </el-card>
    
    <!-- 标题选择对话框 -->
    <el-dialog 
      v-model="titleDialog.visible" 
      title="选择生成的标题" 
      width="600px"
    >
      <div class="title-list">
        <div 
          v-for="(title, index) in titleDialog.titles" 
          :key="index"
          class="title-option"
          :class="{ 'recommended': index === titleDialog.recommendedIndex }"
          @click="selectTitle(title)"
        >
          <div class="title-text">{{ title }}</div>
          <el-tag v-if="index === titleDialog.recommendedIndex" type="success" size="small">
            推荐
          </el-tag>
        </div>
      </div>
      <div class="recommendation-reason" v-if="titleDialog.reason">
        <el-text type="info">推荐理由: {{ titleDialog.reason }}</el-text>
      </div>
    </el-dialog>
    
    <!-- 摘要预览对话框 -->
    <el-dialog 
      v-model="summaryDialog.visible" 
      title="生成的文章摘要" 
      width="700px"
    >
      <div class="summary-content">
        <el-input
          v-model="summaryDialog.summary"
          type="textarea"
          :rows="4"
          placeholder="生成的摘要"
        />
        
        <div class="summary-info">
          <el-tag type="info" size="small">字数: {{ summaryDialog.wordCount }}</el-tag>
        </div>
        
        <div class="key-points" v-if="summaryDialog.keyPoints.length > 0">
          <h4>关键要点:</h4>
          <ul>
            <li v-for="(point, index) in summaryDialog.keyPoints" :key="index">
              {{ point }}
            </li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button @click="summaryDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="applySummary">应用摘要</el-button>
      </template>
    </el-dialog>
    
    <!-- 大纲预览对话框 -->
    <el-dialog 
      v-model="outlineDialog.visible" 
      title="生成的文章大纲" 
      width="800px"
    >
      <div class="outline-dialog-content">
        <div class="outline-info">
          <el-tag type="info" size="small">总章节: {{ outlineDialog.totalSections }}</el-tag>
          <el-tag type="success" size="small">预计阅读: {{ outlineDialog.estimatedReadingTime }}分钟</el-tag>
        </div>
        
        <div class="outline-tree">
          <div 
            v-for="(item, index) in outlineDialog.outline" 
            :key="index"
            class="outline-item" 
            :style="{ marginLeft: (item.level - 1) * 20 + 'px' }"
          >
            <div class="outline-header">
              <el-tag :type="getTagType(item.level)" size="small">H{{ item.level }}</el-tag>
              <span class="outline-title">{{ item.title }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="outlineDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="applyOutline">应用大纲</el-button>
      </template>
    </el-dialog>
    
    <!-- 标签推荐对话框 -->
    <el-dialog 
      v-model="tagsDialog.visible" 
      title="推荐的文章标签" 
      width="600px"
    >
      <div class="tags-content">
        <div class="tags-info">
          <el-tag type="info" size="small">置信度: {{ Math.round(tagsDialog.confidence * 100) }}%</el-tag>
          <el-tag type="success" size="small">主要类别: {{ tagsDialog.category }}</el-tag>
        </div>
        
        <div class="tags-list">
          <el-tag 
            v-for="tag in tagsDialog.tags" 
            :key="tag"
            size="medium"
            class="tag-item"
            type="primary"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>
      <template #footer>
        <el-button @click="tagsDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="applyTags">应用标签</el-button>
      </template>
    </el-dialog>
    
    <!-- 分类推荐对话框 -->
    <el-dialog 
      v-model="categoryDialog.visible" 
      title="推荐的文章分类" 
      width="600px"
    >
      <div class="category-content">
        <div class="category-info">
          <el-tag type="info" size="small">置信度: {{ Math.round(categoryDialog.confidence * 100) }}%</el-tag>
          <el-tag type="success" size="small">推荐: {{ categoryDialog.primaryCategory }}</el-tag>
        </div>
        
        <div class="category-list">
          <div 
            v-for="category in categoryDialog.categories" 
            :key="category.name"
            class="category-option"
            @click="selectCategory(category)"
          >
            <div class="category-name">{{ category.name }}</div>
            <div class="category-score">
              <el-progress 
                :percentage="Math.round(category.score * 100)" 
                :stroke-width="6"
                :show-text="false"
              />
              <span class="score-text">{{ Math.round(category.score * 100) }}%</span>
            </div>
            <div class="category-reason">{{ category.reason }}</div>
          </div>
        </div>
        
        <div class="recommendation-reason" v-if="categoryDialog.reason">
          <el-text type="info">推荐理由: {{ categoryDialog.reason }}</el-text>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Edit, 
  Document, 
  List, 
  PriceTag, 
  FolderOpened 
} from '@element-plus/icons-vue'
import {
  generateArticleTitle,
  generateArticleSummary,
  generateArticleOutline,
  recommendArticleTags,
  recommendArticleCategory
} from '@/api/ArticleAiApi'
import { 
  checkContentLength, 
  getSuggestedTimeout,
  calculateTimeout 
} from '@/utils/aiRequestUtils'

// 大纲渲染工具函数
const getTagType = (level) => {
  const types = ['', 'danger', 'warning', 'success', 'info', 'primary']
  return types[level] || 'info'
}

// Props定义
const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  }
})

// Emits定义
const emit = defineEmits([
  'update:title',
  'update:summary', 
  'update:outline',
  'update:tags',
  'update:categoryId'
])

// 计算属性
const hasContent = computed(() => {
  const check = checkContentLength(props.content, 20)
  return check.isValid
})

const contentLength = computed(() => props.content ? props.content.length : 0)

// 超时配置
const timeoutConfig = computed(() => {
  return getSuggestedTimeout(props.content)
})

// 加载状态
const loading = reactive({
  title: false,
  summary: false,
  outline: false,
  tags: false,
  category: false
})

// 对话框状态
const titleDialog = reactive({
  visible: false,
  titles: [],
  recommendedIndex: 0,
  reason: ''
})

const summaryDialog = reactive({
  visible: false,
  summary: '',
  keyPoints: [],
  wordCount: 0
})

const outlineDialog = reactive({
  visible: false,
  outline: [],
  totalSections: 0,
  estimatedReadingTime: 0
})

const tagsDialog = reactive({
  visible: false,
  tags: [],
  confidence: 0,
  category: ''
})

const categoryDialog = reactive({
  visible: false,
  categories: [],
  primaryCategory: '',
  confidence: 0,
  reason: ''
})

// 生成标题
const generateTitle = () => {
  if (!hasContent.value) {
    ElMessage.warning('请先输入文章内容')
    return
  }
  
  loading.title = true
  generateArticleTitle({
    content: props.content,
    currentTitle: props.title
  }, {
    onSuccess: (data) => {
      titleDialog.titles = data.titles || []
      titleDialog.recommendedIndex = data.recommendedIndex || 0
      titleDialog.reason = data.reason || ''
      titleDialog.visible = true
    },
    onError: (error) => {
      ElMessage.error('标题生成失败: ' + error.message)
    }
  }).finally(() => {
    loading.title = false
  })
}

// 选择标题
const selectTitle = (title) => {
  emit('update:title', title)
  titleDialog.visible = false
  ElMessage.success('标题已应用')
}

// 生成摘要
const generateSummary = () => {
  if (!hasContent.value) {
    ElMessage.warning('请先输入文章内容')
    return
  }
  
  loading.summary = true
  generateArticleSummary({
    content: props.content
  }, {
    onSuccess: (data) => {
      summaryDialog.summary = data.summary || ''
      summaryDialog.keyPoints = data.keyPoints || []
      summaryDialog.wordCount = data.wordCount || 0
      summaryDialog.visible = true
    },
    onError: (error) => {
      ElMessage.error('摘要生成失败: ' + error.message)
    }
  }).finally(() => {
    loading.summary = false
  })
}

// 应用摘要
const applySummary = () => {
  emit('update:summary', summaryDialog.summary)
  summaryDialog.visible = false
  ElMessage.success('摘要已应用')
}

// 生成大纲
const generateOutline = () => {
  if (!hasContent.value) {
    ElMessage.warning('请先输入文章内容')
    return
  }
  
  loading.outline = true
  generateArticleOutline({
    content: props.content,
    includeReadingTimeEstimation: true
  }, {
    onSuccess: (data) => {
      console.log('大纲生成成功，数据结构：', data)
      outlineDialog.outline = data.outline || []
      outlineDialog.totalSections = data.totalSections || 0
      outlineDialog.estimatedReadingTime = data.estimatedReadingTime || 0
      console.log('设置的大纲数据：', outlineDialog.outline)
      outlineDialog.visible = true
    },
    onError: (error) => {
      ElMessage.error('大纲生成失败: ' + error.message)
    }
  }).finally(() => {
    loading.outline = false
  })
}

// 应用大纲
const applyOutline = () => {
  const outlineJson = JSON.stringify(outlineDialog.outline)
  emit('update:outline', outlineJson)
  outlineDialog.visible = false
  ElMessage.success('大纲已应用')
}

// 推荐标签
const recommendTags = () => {
  if (!hasContent.value) {
    ElMessage.warning('请先输入文章内容')
    return
  }
  
  loading.tags = true
  recommendArticleTags({
    content: props.content,
    currentTitle: props.title
  }, {
    onSuccess: (data) => {
      tagsDialog.tags = data.tags || []
      tagsDialog.confidence = data.confidence || 0
      tagsDialog.category = data.category || ''
      tagsDialog.visible = true
    },
    onError: (error) => {
      ElMessage.error('标签推荐失败: ' + error.message)
    }
  }).finally(() => {
    loading.tags = false
  })
}

// 应用标签
const applyTags = () => {
  emit('update:tags', tagsDialog.tags)
  tagsDialog.visible = false
  ElMessage.success('标签已应用')
}

// 推荐分类
const recommendCategory = () => {
  if (!hasContent.value) {
    ElMessage.warning('请先输入文章内容')
    return
  }
  
  loading.category = true
  recommendArticleCategory({
    content: props.content,
    currentTitle: props.title
  }, {
    onSuccess: (data) => {
      categoryDialog.categories = data.categories || []
      categoryDialog.primaryCategory = data.primaryCategory || ''
      categoryDialog.confidence = data.confidence || 0
      categoryDialog.reason = data.reason || ''
      categoryDialog.visible = true
    },
    onError: (error) => {
      ElMessage.error('分类推荐失败: ' + error.message)
    }
  }).finally(() => {
    loading.category = false
  })
}

// 选择分类
const selectCategory = (category) => {
  // 这里需要根据分类名称找到对应的ID
  emit('update:categoryId', category.name)
  categoryDialog.visible = false
  ElMessage.success(`已选择分类: ${category.name}`)
}

</script>

<style scoped>
.ai-assistant {
  margin-bottom: 20px;
}

.ai-card {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.card-header {
  display: flex;
  align-items: center;
  font-weight: bold;
}

.ai-icon {
  margin-right: 8px;
  color: #409eff;
}

.ai-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 15px;
}

.content-info {
  text-align: center;
  margin-top: 10px;
}

.no-content-tip {
  margin-top: 10px;
}

/* 大纲样式 */
.outline-dialog-content {
  max-height: 500px;
  overflow-y: auto;
}

.outline-info {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.outline-tree {
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fafafa;
}

.outline-item {
  margin-bottom: 8px;
  padding: 8px;
  background-color: white;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.outline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.outline-title {
  font-weight: 500;
  color: #303133;
}

.outline-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
  margin-left: 32px;
}

/* 对话框样式 */
.title-list {
  max-height: 400px;
  overflow-y: auto;
}

.title-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.title-option:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.title-option.recommended {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

.title-text {
  font-weight: 500;
  flex: 1;
}

.recommendation-reason {
  margin-top: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.summary-content {
  margin-bottom: 20px;
}

.summary-info {
  margin: 10px 0;
}

.key-points {
  margin-top: 15px;
}

.key-points h4 {
  margin-bottom: 10px;
  color: #303133;
}

.key-points ul {
  padding-left: 20px;
}

.key-points li {
  margin-bottom: 5px;
  line-height: 1.5;
}

.outline-content {
  max-height: 500px;
  overflow-y: auto;
}

.outline-info {
  margin-bottom: 15px;
  display: flex;
  gap: 10px;
}

.outline-item {
  margin-bottom: 10px;
}

.outline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}

.outline-title {
  font-weight: 500;
}

.outline-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  margin-left: 40px;
}

.tags-content, .category-content {
  margin-bottom: 20px;
}

.tags-info, .category-info {
  margin-bottom: 15px;
  display: flex;
  gap: 10px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  margin: 2px;
}

.category-list {
  max-height: 300px;
  overflow-y: auto;
}

.category-option {
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.category-option:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.category-name {
  font-weight: 500;
  margin-bottom: 8px;
}

.category-score {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.score-text {
  font-size: 12px;
  color: #909399;
}

.category-reason {
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}
</style>
