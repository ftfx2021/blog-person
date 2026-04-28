<template>
  <div class="article-list-container">
    <!-- 浪漫标题区域 -->
    <div class="romantic-title-container">
      <div class="title-decorations">
        <span class="decoration-left">&#60;&#47;</span>
        <span class="decoration-right">&#62;</span>
      </div>
      <h1 class="romantic-title">
        <span class="typewriter-text"></span>
        <span class="cursor">|</span>
      </h1>
    
      <div class="title-wave"></div>
    </div>


    <!-- 分类筛选区域（水平布局） -->
    <div class="category-filter-horizontal">
      <div class="category-scroll-container" :class="{ 'popup-open': hoveredCategory }">
        <!-- 全部标签 -->
        <div
          class="top-category-item"
          :class="{ active: selectedCategoryId === null }"
          @click="selectCategory(null)"
        >
          <span class="category-name">全部</span>
          <span class="category-count">{{ totalArticleCount }}</span>
        </div>
        
        <!-- 顶级分类列表 -->
        <div
          v-for="category in categories"
          :key="category.id"
          class="top-category-item"
          :class="{ 
            active: isTopCategoryActive(category.id),
            'has-children': category.children && category.children.length > 0
          }"
          :data-category-id="category.id"
          @click="handleTopCategoryClick(category)"
          @mouseenter="handleCategoryHover(category)"
          @mouseleave="handleCategoryLeave"
        >
          <span class="category-name">{{ category.name }}</span>
          <span class="category-count">{{ category.articleCount || 0 }}</span>
        </div>
      </div>
      
      <!-- 子分类胶囊容器（毛玻璃效果） - 独立层级 -->
      <transition name="fade-slide-down">
        <div 
          v-if="hoveredCategory && hoveredCategory.children && hoveredCategory.children.length > 0"
          class="sub-category-popup"
          :style="popupStyle"
          @mouseenter="keepPopupOpen"
          @mouseleave="handlePopupLeave"
        >
          <SubCategoryCarousel
            :sub-categories="hoveredCategory.children"
            :selected-category-id="selectedCategoryId"
            @select="selectCategory"
          />
        </div>
      </transition>
    </div>

    <div class="content-layout">
      <section class="content">
        <div class="article-section" v-loading="loading">
      <div class="section-header">
        <h2 class="section-title">
          <template v-if="selectedCategoryId !== null">{{ selectedCategoryName }}</template>
          <template v-else>全部文章</template>
        </h2>
        <div class="article-count" v-if="total > 0">共 {{ total }} 篇文章</div>
      </div>

      <el-empty v-if="articles.length === 0" description="暂无文章" />
      
      <div v-else class="articles-grid">
        <ArticleCard 
          v-for="article in articles" 
          :key="article.id" 
          :article="article"
          @article-click="goToArticle"
          @tag-click="goToTag"
          @category-click="goToCategory"
        />
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
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Folder, Calendar, View } from '@element-plus/icons-vue'
import { getCategoryTree } from '@/api/CategoryApi'
import { getLatestArticles } from '@/api/ArticleApi'
import { ElMessage } from 'element-plus'
import DateUtils from '@/utils/dateUtils'
import DynamicTag from '@/components/common/DynamicTag.vue'
import SubCategoryCarousel from '@/components/frontend/SubCategoryCarousel.vue'
import ArticleCard from '@/components/frontend/ArticleCard.vue'

const route = useRoute()
const router = useRouter()


// 打字机效果
const titleText = "Hello, Beautiful World"
let typingTimer = null

const startTypingAnimation = () => {
  const typewriterElement = document.querySelector('.typewriter-text')
  if (!typewriterElement) return
  
  let index = 0
  typewriterElement.textContent = ''
  
  clearInterval(typingTimer)
  typingTimer = setInterval(() => {
    if (index < titleText.length) {
      typewriterElement.textContent += titleText.charAt(index)
      index++
    } else {
      clearInterval(typingTimer)
    }
  }, 150)
}

// 加载状态
const loading = ref(false)
// 文章列表
const articles = ref([])
// 分页信息
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
// 分类相关
const categories = ref([])
const selectedCategoryId = ref(null)
const selectedCategoryName = ref('')
const hoveredCategory = ref(null)
const totalArticleCount = ref(0)
const popupStyle = ref({})
let hideTimer = null

// 递归查找分类名称（支持子分类）
const findCategoryNameById = (id) => {
  if (id === null || id === undefined) return ''
  const dfs = (nodes) => {
    if (!Array.isArray(nodes)) return ''
    for (const node of nodes) {
      if (Number(node.id) === Number(id)) return node.name || ''
      if (node.children && node.children.length) {
        const res = dfs(node.children)
        if (res) return res
      }
    }
    return ''
  }
  return dfs(categories.value)
}


// 获取所有分类
const fetchCategories = () => {
  getCategoryTree({
    showDefaultMsg: false,
    onSuccess: (data) => {
      categories.value = data || []
      // 计算总文章数
      totalArticleCount.value = categories.value.reduce((sum, cat) => sum + (cat.articleCount || 0), 0)
    },
    onError: (error) => {
      console.error('获取分类列表失败:', error)
    }
  })
}

// 获取文章列表
const fetchArticles = (page) => {
  loading.value = true
  const params = {
    currentPage: page,
    size: pageSize.value,
    status: 1 // 已发布的文章
  }
  
  // 如果选择了分类，添加分类ID参数
  if (selectedCategoryId.value !== null) {
    params.categoryId = selectedCategoryId.value
  }
  
  getLatestArticles(params, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      articles.value = data.records || []
      total.value = data.total || 0
      loading.value = false
    },
    onError: (error) => {
      console.error('获取文章列表失败:', error)
      ElMessage.error('获取文章列表失败')
      loading.value = false
    }
  })
}


// 选择分类
const selectCategory = (categoryId) => {
  if (selectedCategoryId.value === categoryId) return
  
  selectedCategoryId.value = categoryId
  
  // 设置选中分类名称
  if (categoryId === null) {
    selectedCategoryName.value = ''
  } else {
    selectedCategoryName.value = findCategoryNameById(categoryId)
  }
  
  // 更新路由
  const query = {...route.query}
  if (categoryId === null) {
    delete query.categoryId
  } else {
    query.categoryId = categoryId
  }
  
  router.push({
    path: route.path,
    query
  })
  
  // 重置页码并获取文章
  currentPage.value = 1
  fetchArticles(1)
}

// 顶级分类点击
const handleTopCategoryClick = (category) => {
  // 如果没有子分类,直接选择该分类
  if (!category.children || category.children.length === 0) {
    selectCategory(category.id)
  } else {
    // 如果有子分类,选择该顶级分类(显示该分类下所有文章)
    selectCategory(category.id)
  }
}

// 判断顶级分类是否激活
const isTopCategoryActive = (topCategoryId) => {
  if (selectedCategoryId.value === topCategoryId) return true
  
  // 检查是否选中了该顶级分类的子分类
  const topCategory = categories.value.find(cat => cat.id === topCategoryId)
  if (topCategory?.children) {
    return topCategory.children.some(child => child.id === selectedCategoryId.value)
  }
  
  return false
}

// 分类悬浮
const handleCategoryHover = (category) => {
  // 清除之前的隐藏定时器
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
  
  if (category.children && category.children.length > 0) {
    hoveredCategory.value = category
    
    // 计算弹窗位置
    setTimeout(() => {
      const categoryElement = document.querySelector(`[data-category-id="${category.id}"]`)
      if (categoryElement) {
        const rect = categoryElement.getBoundingClientRect()
        const containerRect = categoryElement.closest('.category-filter-horizontal').getBoundingClientRect()
        
        popupStyle.value = {
          left: `${rect.left - containerRect.left + rect.width / 2}px`,
          top: `${rect.bottom - containerRect.top + 12}px`
        }
      }
    }, 0)
  }
}

// 分类离开
const handleCategoryLeave = () => {
  // 设置延迟隐藏,给用户时间移动鼠标到弹窗
  hideTimer = setTimeout(() => {
    hoveredCategory.value = null
    popupStyle.value = {}
  }, 300)
}

// 保持弹窗打开
const keepPopupOpen = () => {
  // 鼠标进入弹窗时,清除隐藏定时器,保持弹窗和父级分类容器打开
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
}

// 弹窗离开
const handlePopupLeave = () => {
  // 从弹窗离开时,才真正隐藏
  hideTimer = setTimeout(() => {
    hoveredCategory.value = null
    popupStyle.value = {}
  }, 300)
}

// 监听路由查询参数变化
watch(() => route.query, (query) => {
  // 处理分类ID
  if (query.categoryId) {
    const categoryId = parseInt(query.categoryId)
    if (selectedCategoryId.value !== categoryId) {
      selectedCategoryId.value = categoryId
      // 设置选中分类名称
      selectedCategoryName.value = findCategoryNameById(categoryId)
    }
  } else {
    selectedCategoryId.value = null
    selectedCategoryName.value = ''
  }
  
  fetchArticles(1)
}, { immediate: true })

// 初始化
onMounted(async () => {
  // 先获取分类列表
  await fetchCategories()
  
  // 处理路由参数
  if (route.query.categoryId) {
    const categoryId = parseInt(route.query.categoryId)
    selectedCategoryId.value = categoryId
    // 设置选中分类名称
    selectedCategoryName.value = findCategoryNameById(categoryId)
  }
  
  fetchArticles(1)
  
  document.title = '文章列表 - 个人博客'
  
  // 启动打字机动画
  setTimeout(() => {
    startTypingAnimation()
  }, 500)
})


// 处理页码变化
const handlePageChange = (page) => {
  currentPage.value = page
  fetchArticles(page)
  // 滚动到页面顶部
  window.scrollTo(0, 0)
}

// 跳转到文章详情
const goToArticle = (id) => {
  router.push(`/article/${id}`)
}

// 跳转到分类页面
const goToCategory = (id) => {
  router.push(`/category/${id}`)
}

// 跳转到标签页面
const goToTag = (id) => {
  router.push(`/tag/${id}`)
}

// 格式化日期
const formatDate = (dateString) => {
  return DateUtils.formatDate(dateString);
}


</script>

<style scoped>
.article-list-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  
}

.content-layout {
  max-width: 1400px;
  margin: 0 auto;
}

.content {
  width: 100%;
}

/* 浪漫标题样式 */
.romantic-title-container {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
  padding: 30px 0;
}

.romantic-title {
  font-size: 36px;
  font-weight: 500;
  color: #3498db;
  margin: 0;
  letter-spacing: 2px;
  font-family: 'JetBrains Mono', 'JetBrainsMono-Medium', 'Consolas', 'Monaco', monospace;
  position: relative;
  display: inline-block;
  min-height: 45px;
}

.romantic-subtitle {
  font-size: 16px;
  color: #777;
  margin-top: 10px;
  font-weight: 300;
  letter-spacing: 1px;
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


/* 分类筛选样式 - 水平布局 */
.category-filter-horizontal {
  position: relative;
  min-height: 62px;

}

.category-scroll-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 6px 8px;
  border: 1.5px solid #f3f3f3;
  border-radius: 10px;
  background: #fff;
  max-height: 50px;
  overflow: hidden;
  z-index: 10;
  transform-origin: top center;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.category-scroll-container:hover,
.category-scroll-container.popup-open {
  max-height: 500px;
  padding: 6px 8px 12px 8px;
  background:transparent;
  backdrop-filter: blur(5px);
  z-index: 100;
}

/* 顶级分类项 - 水平布局 */
.top-category-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 2px 12px;
   font-weight: 700;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 18px;
  white-space: nowrap;
  flex-shrink: 0;
}

.top-category-item:hover {
   background: rgb(7, 26, 242);
  color: #fff;
  border-radius: 10px;


}

.top-category-item.active {
  background: rgb(7, 26, 242);
  color: #fff;
 border-radius: 10px;

}

.category-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  
  padding: 0 0px;
  background: #333;
  color: #fff;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
}

.top-category-item:hover .category-count,
.top-category-item.active .category-count {
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
}

/* 子分类弹窗容器 - 仅定位 */
.sub-category-popup {
  position: absolute;
  transform: translateX(-50%);
  z-index: 1000;
}

/* 过渡动画 - 从上往下 */
.fade-slide-down-enter-active,
.fade-slide-down-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-down-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px) scale(0.95);
}

.fade-slide-down-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px) scale(0.95);
}

.article-section {
  min-height: 400px;
  border-radius: 8px;
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
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

/* 文章网格布局 - 约3:2比例 */
.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

/* 响应式网格 */
@media (max-width: 1400px) {
  .articles-grid {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .articles-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .articles-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}



.pagination-container {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .category-filter-horizontal {
    padding: 0 10px;
  }
  
  .category-scroll-container {
    gap: 8px;
  }
  
  .top-category-item {
    padding: 8px 16px;
    font-size: 13px;
  }
  
  .sub-category-popup {
    min-width: 300px;
    max-width: 90vw;
  }
}

.typewriter-text {
  display: inline-block;
}

.cursor {
  display: inline-block;
  margin-left: 2px;
  font-weight: 100;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style> 