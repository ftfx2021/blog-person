<template>
  <div class="tag-articles-container">
    <!-- 标签标题区域 -->
    <div class="tag-header" v-if="tag">
      <div class="title-decorations">
        <span class="decoration-left">&#123;</span>
        <span class="decoration-right">&#125;</span>
      </div>
      <h1 class="tag-title">标签：{{ tag.name }}</h1>
      <div class="tag-info">
        <DynamicTag
          :label="tag.name"
          :bgColor="tag.color"
          :textColor="tag.textColor"
          size="small"
          :clickable="false"
        />
        <span class="article-count">共 {{ tag.articleCount || 0 }} 篇文章</span>
      </div>
      <div class="title-wave"></div>
    </div>

    <div class="article-list" v-loading="loading">
      <div class="section-header">
        <h2 class="section-title">标签文章</h2>
        <div class="article-count" v-if="total > 0">共 {{ total }} 篇文章</div>
      </div>
      
      <el-empty v-if="articles.length === 0" description="该标签下暂无文章" />
      
      <div 
        v-else
        v-for="article in articles" 
        :key="article.id" 
        class="article-item"
        @click="goToArticle(article.id)"
      >
        <el-image 
          v-if="article.coverImage" 
          :src="article.coverImage" 
          fit="cover" 
          class="article-cover"
        />
        <div class="article-info">
          <h3 class="article-title">{{ article.title }}</h3>
          <p class="article-summary">{{ article.summary }}</p>
          <div class="article-meta">
            <span><el-icon><User /></el-icon> {{ article.authorName }}</span>
            <span @click.stop="goToCategory(article.categoryId)"><el-icon><Folder /></el-icon> {{ article.categoryName }}</span>
            <span><el-icon><Calendar /></el-icon> {{ formatDate(article.createTime) }}</span>
            <span><el-icon><View /></el-icon> {{ article.viewCount }} 阅读</span>
          </div>
          <div class="article-tags">
            <DynamicTag
              v-for="tagItem in article.tags" 
              :key="tagItem.id" 
              :label="tagItem.name"
              :bgColor="tagItem.color"
              :textColor="tagItem.textColor"
              size="small"
              class="tag-item"
              @click="goToTag(tagItem.id)"
            />
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          background
          layout="prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Folder, Calendar, View } from '@element-plus/icons-vue'
import { getTagById, getArticlesByTag } from '@/api/TagApi'
import { ElMessage } from 'element-plus'
import DynamicTag from '@/components/common/DynamicTag.vue'

const route = useRoute()
const router = useRouter()


// 加载状态
const loading = ref(false)
// 标签信息
const tag = ref(null)
// 文章列表
const articles = ref([])
// 分页信息
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 监听路由参数变化
watch(() => route.params.id, (newId) => {
  if (newId) {
    currentPage.value = 1
    fetchTag(newId)
    fetchArticles(newId, 1)
  }
})

// 初始化
onMounted(() => {
  const tagId = route.params.id
  if (tagId) {
    fetchTag(tagId)
    fetchArticles(tagId, 1)
  }
})

// 获取标签信息
const fetchTag = async (id) => {
  getTagById(id, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      tag.value = data
      document.title = `${data.name} - 标签文章 - 个人博客`
    },
    onError: (error) => {
      console.error('获取标签信息失败:', error)
      ElMessage.error('获取标签信息失败')
    }
  })
}

// 获取标签下的文章
const fetchArticles = async (id, page) => {
  loading.value = true
  
  getArticlesByTag(id, {
    currentPage: page,
    size: pageSize.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      articles.value = data.records || []
      total.value = data.total || 0
      loading.value = false
    },
    onError: (error) => {
      console.error('获取标签文章失败:', error)
      ElMessage.error('获取标签文章失败')
      loading.value = false
    }
  })
}

// 处理页码变化
const handlePageChange = (page) => {
  currentPage.value = page
  fetchArticles(route.params.id, page)
  // 滚动到页面顶部
  window.scrollTo(0, 0)
}

// 跳转到文章详情
const goToArticle = (id) => {
  router.push(`/article/${id}`)
}

// 跳转到标签页
const goToTag = (id) => {
  if (id !== Number(route.params.id)) {
    router.push(`/tag/${id}`)
  }
}

// 跳转到分类页
const goToCategory = (id) => {
  router.push(`/category/${id}`)
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString()
}


</script>

<style scoped>
.tag-articles-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
 
}

/* 标签标题区域样式 */
.tag-header {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
  padding: 30px 0;
  border-radius: 8px;
  background-color: transparent;
}

.tag-title {
  font-size: 36px;
  font-weight: 500;
  color: #3498db;
  margin: 0;
  letter-spacing: 2px;
  font-family: 'JetBrains Mono', 'JetBrainsMono-Medium', 'Consolas', 'Monaco', monospace;
  position: relative;
  display: inline-block;
  padding-bottom: 0;
}

.tag-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 15px;
}

.title-decorations {
  position: relative;
  margin-bottom: 15px;
}

.decoration-left, .decoration-right {
  font-size: 24px;
  color: #2ecc71;
  position: relative;
  display: inline-block;
  margin: 0 20px;
  font-family: 'JetBrains Mono', 'JetBrainsMono-Medium', 'Consolas', 'Monaco', monospace;
}

.decoration-left::before, .decoration-right::before {
  content: '';
  height: 1px;
  width: 60px;
  background: linear-gradient(to right, transparent, #2ecc71, transparent);
  position: absolute;
  top: 50%;
}

.decoration-left::before {
  right: 30px;
}

.decoration-right::before {
  left: 30px;
}

.title-wave {
  height: 15px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 30" preserveAspectRatio="none"><path d="M0,0 C150,40 350,0 500,20 C650,40 750,0 900,10 C1050,20 1150,40 1200,10 L1200,30 L0,30 Z" style="fill: %23f9f9f9;"/></svg>') no-repeat;
  background-size: 100% 100%;
  position: absolute;
  bottom: 0;
  width: 100%;
  opacity: 0.8;
}

.main-tag {
  font-size: 16px;
  padding: 6px 12px;
  border-radius: 4px;
}

/* 文章列表样式 */
.article-list {
  background-color: #fff;
  border-radius: 8px;
  padding: 25px;
  border: 1px solid #eaeaea;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.section-title {
  margin: 0;
  font-size: 24px;
  color: #333;
  font-weight: 600;
}

.article-count {
  font-size: 14px;
  color: #888;
}

.article-item {
  display: flex;
  padding: 25px 15px;
  border-bottom: 1px solid #e0e0e0;
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 10px;
  border: 1px solid transparent;
}

.article-item:hover {
  transform: none;
  border: 1px solid rgba(64, 158, 255, 0.6);
  background-color: rgba(64, 158, 255, 0.02);
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.08);
}

.article-item:not(:last-child)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 10%;
  width: 80%;
  height: 1px;
  background: linear-gradient(to right, transparent, #d0d0d0, transparent);
}

.article-item:last-child {
  border-bottom: 1px solid #e0e0e0;
}

.article-cover {
  width: 220px;
  height: 140px;
  border-radius: 6px;
  margin-right: 25px;
  flex-shrink: 0;
  object-fit: cover;
  transition: all 0.3s ease;
  border: none;
}

.article-item:hover .article-cover {
  transform: none;
}

.article-info {
  flex: 1;
}

.article-title {
  margin: 0 0 12px;
  font-size: 20px;
  color: #333;
  text-align: left;
  font-weight: 600;
  transition: color 0.3s;
}

.article-item:hover .article-title {
  color: #409EFF;
}

.article-summary {
  color: #666;
  text-align: left;
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 15px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  text-align: left;
  color: #888;
  font-size: 13px;
  margin-bottom: 12px;
}

.article-meta span {
  margin-right: 18px;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  transition: color 0.2s;
}

.article-meta span:hover {
  color: #409EFF;
}

.article-meta .el-icon {
  margin-right: 5px;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
}

.tag-item {
  margin-right: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  transition: transform 0.2s;
}

.tag-item:hover {
  transform: scale(1.05);
}

.pagination-container {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .article-item {
    flex-direction: column;
  }
  
  .article-cover {
    width: 100%;
    height: 180px;
    margin-right: 0;
    margin-bottom: 15px;
  }
  
  .tag-title {
    font-size: 28px;
  }
  
  .decoration-left, .decoration-right {
    font-size: 20px;
    margin: 0 10px;
  }
  
  .decoration-left::before, .decoration-right::before {
    width: 40px;
  }
  
  .decoration-left::before {
    right: 25px;
  }
  
  .decoration-right::before {
    left: 25px;
  }
}
</style> 