<template>
  <div class="category-articles-container">
    <!-- 分类标题区域 -->
    <div class="category-header" v-if="category">
      <div class="title-decorations">
        <span class="decoration-left">&#60;&#47;</span>
        <span class="decoration-right">&#62;</span>
      </div>
      <h1 class="category-title">
        分类：{{ category.name }}
      </h1>
      <p class="category-description" v-if="category.description">{{ category.description }}</p>
      <div class="title-wave"></div>
    </div>

    <div class="article-list" v-loading="loading">
      <div class="section-header">
        <h2 class="section-title">分类文章</h2>
        <div class="article-count" v-if="total > 0">共 {{ total }} 篇文章</div>
      </div>
      
      <el-empty v-if="articles.length === 0" description="该分类下暂无文章" />
      
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
            <span><el-icon><Calendar /></el-icon> {{ formatDate(article.createTime) }}</span>
            <span><el-icon><View /></el-icon> {{ article.viewCount }} 阅读</span>
          </div>
          <div class="article-tags">
            <DynamicTag
              v-for="tag in article.tags"
              :key="tag.id"
              :label="tag.name"
              :bgColor="tag.color"
              :textColor="tag.textColor"
              size="small"
              class="tag-item"
              @click.stop="goToTag(tag.id)"
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
import { User, Calendar, View } from '@element-plus/icons-vue'
import DynamicTag from '@/components/common/DynamicTag.vue'
import { getCategoryById } from '@/api/CategoryApi'
import { getArticlesByCategory } from '@/api/ArticleApi'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()


// 加载状态
const loading = ref(false)
// 分类信息
const category = ref(null)
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
    fetchCategory(newId)
    fetchArticles(newId, 1)
  }
})

// 初始化
onMounted(() => {
  const categoryId = route.params.id
  if (categoryId) {
    fetchCategory(categoryId)
    fetchArticles(categoryId, 1)
  }
})

// 获取分类信息
const fetchCategory = (id) => {
  getCategoryById(id, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      category.value = data
      document.title = `${data.name} - 分类文章 - 个人博客`
    },
    onError: (error) => {
      console.error('获取分类信息失败:', error)
      ElMessage.error('获取分类信息失败')
    }
  })
}

// 获取分类下的文章
const fetchArticles = (id, page) => {
  loading.value = true
  getArticlesByCategory(id, {
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
      console.error('获取分类文章失败:', error)
      ElMessage.error('获取分类文章失败')
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
  router.push(`/tag/${id}`)
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString()
}

</script>

<style scoped>
.category-articles-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;

}

/* 分类标题区域样式 */
.category-header {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
  padding: 30px 0;
  border-radius: 8px;
  border: none;
  background-color: transparent;
}

.category-title {
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

.category-title:after {
  display: none;
}

.typewriter-text {
  display: none;
}

.cursor {
  display: none;
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

.category-description {
  color: #606266;
  margin: 15px auto 0;
  font-size: 15px;
  line-height: 1.6;
  max-width: 600px;
}

/* 文章列表样式 */
.article-list {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
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
  line-clamp: 2;
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
  
  .category-title {
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