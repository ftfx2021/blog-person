<template>
  <div class="home-container">
    <!-- 装饰背景 -->
    <div class="decorative-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
    </div>

    <!-- HERO 区域 -->
    <header class="hero-section">
      <h1 class="hero-title" v-html="heroTitleHtml"></h1>
      
      <!-- 打字机效果 -->
      <div class="typewriter-container">
        <span class="typewriter-prefix">I write about</span>
        <span class="typewriter-text">{{ displayText }}</span>
        <span class="cursor"></span>
      </div>

      <div class="hero-actions">
        <a href="#posts" class="btn-primary">
          <i class="fa-solid fa-arrow-down"></i> 开始阅读
        </a>
    
      </div>
    </header>

    <!-- 主要内容区 -->
    <main id="posts" class="main-content">
      <div class="content-grid">
        
        <!-- 左侧：文章列表 -->
        <div class="articles-column">
          <div class="section-header">
            <h2 class="section-title">
              <i class="fa-solid fa-newspaper"></i> 最新文章
            </h2>
            <button class="view-more-btn" @click="$router.push('/articles')">
              查看更多 <i class="fa-solid fa-arrow-right"></i>
            </button>
          </div>

          <el-empty v-if="latestArticles.length === 0" description="暂无文章" />

          <div v-else class="articles-list">
            <article 
              v-for="article in latestArticles" 
              :key="article.id" 
              class="article-card glass-card"
              :style="getArticleCardStyle(article)"
              @click="goToArticle(article.id)"
            >
              <!-- 文章封面 -->
              <div class="article-cover">
                <img v-if="article.coverImage" :src="article.coverImage" :alt="article.title" class="cover-image" />
                <DefaultCover 
                  v-else
                  :color="article.mainColor || '#409EFF'"
                  :text="article.title"
                  class="cover-image"
                />
                <span class="category-badge" :style="getCategoryBadgeStyle(article)">{{ article.categoryName || '未分类' }}</span>
              </div>
              
              <!-- 文章内容 -->
              <div class="article-body">
                <div class="article-meta">
                  <span><i class="fa-regular fa-calendar"></i> {{ formatDate(article.createTime) }}</span>
                  <span><i class="fa-regular fa-eye"></i> {{ article.viewCount }} 阅读</span>
                </div>
                <h3 class="article-title">{{ article.title }}</h3>
                <p class="article-summary">{{ article.summary }}</p>
                <div class="article-footer">
                  <div class="article-tags">
                    <span 
                      v-for="tag in (article.tags || []).slice(0, 3)" 
                      :key="tag.id" 
                      class="tag-item"
                      @click.stop="goToTag(tag.id)"
                    >
                      #{{ tag.name }}
                    </span>
                  </div>
                  <span class="read-more">
                    阅读全文 <i class="fa-solid fa-arrow-right"></i>
                  </span>
                </div>
              </div>
            </article>
          </div>
        </div>

        <!-- 右侧：吸顶侧边栏 -->
        <aside class="sidebar-column">
          <div class="sidebar-sticky">
            <!-- 博主信息卡片 -->
            <div class="sidebar-card author-card">
              <!-- 欢迎语遮罩层 -->
              <div class="welcome-overlay">
                <div class="welcome-icon">
                  <i class="fa fa-hand-peace"></i>
                </div>
                <h2 class="welcome-text">Hi, Friend</h2>
                <p class="welcome-sub">很高兴见到你</p>
                <div class="welcome-line"></div>
              </div>
              <p class="author-title">
                <transition name="fade" mode="out-in">
                  <span :key="currentAuthorKeyword">{{ currentAuthorKeyword }}</span>
                </transition>
              </p>
              <div class="author-avatar">
                <el-avatar :size="100" :src="adminAvatar">
                  {{ adminName ? adminName.charAt(0) : 'A' }}
                </el-avatar>
              </div>
              <h3 class="author-name">{{ aboutInfo.about_name || adminName || 'John Doe' }}</h3>
        
              <p class="author-intro">{{ aboutInfo.personal_signature || '热爱代码，热爱生活。专注于 Web 开发技术分享。' }}</p>
      
            </div>

            <!-- 3D 标签云 -->
            <div class="sidebar-card glass-card tags-cloud-card">
              <h3 class="sidebar-title">
                <i class="fa-solid fa-tags"></i> 标签云
              </h3>
              <TagsCloud3D 
                :data="tagsCloudData" 
                :boxWidth="280" 
                :speed="400"
                :randomColor="true"
                @tag-click="handleTagClick"
              />
            </div>

            <!-- 热门文章 -->
            <div class="sidebar-card glass-card">
              <h3 class="sidebar-title">
                <i class="fa-solid fa-fire"></i> 热门阅读
              </h3>
              <ul class="hot-articles-list">
                <li 
                  v-for="(article, index) in hotArticles.slice(0, 5)" 
                  :key="article.id"
                  @click="goToArticle(article.id)"
                >
                  <span class="rank-number">{{ String(index + 1).padStart(2, '0') }}</span>
                  <div class="hot-article-info">
                    <h4>{{ article.title }}</h4>
                    <span class="view-count">{{ article.viewCount }} views</span>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getTopLevelCategories } from '@/api/CategoryApi'
import { getAllTags } from '@/api/TagApi'
import { getLatestArticles, getHotArticles } from '@/api/ArticleApi'
import { getAllBlogConfigs, getBlogConfigByKey } from '@/api/BlogConfigApi'
import { getUsersByRole } from '@/api/user'
import { getRandomHomepageSentence } from '@/api/SentenceApi'
import DateUtils from '@/utils/dateUtils'
import TagsCloud3D from '@/components/frontend/TagsCloud3D.vue'
import DefaultCover from '@/components/frontend/DefaultCover.vue'

const router = useRouter()

// 最新文章
const latestArticles = ref([])
// 热门文章
const hotArticles = ref([])
// 分类列表
const categories = ref([])
// 标签列表
const tags = ref([])
// 博主信息
const aboutInfo = ref({})
const adminAvatar = ref('')
const adminName = ref('')

// 博主关键词轮播
const defaultAuthorKeywords = ['Full Stack Developer']
const authorKeywords = ref([...defaultAuthorKeywords])
const currentAuthorKeywordIndex = ref(0)
const currentAuthorKeyword = computed(() => {
  const keywords = authorKeywords.value
  if (keywords.length === 0) return 'Full Stack Developer'
  return keywords[currentAuthorKeywordIndex.value % keywords.length]
})
let authorKeywordTimer = null

// 首页句子
const heroSentence = ref(null)
const defaultSentence = {
  sentenceContent: '探索技术的无限可能',
  keywords: '无限可能'
}

// 计算Hero标题HTML（高亮关键词）
const heroTitleHtml = computed(() => {
  const sentence = heroSentence.value || defaultSentence
  let content = sentence.sentenceContent || defaultSentence.sentenceContent
  const keywords = sentence.keywords
  
  if (!keywords) {
    return content
  }
  
  // 将关键词用高亮标签包裹
  const keywordArray = keywords.split(',')
  keywordArray.forEach(keyword => {
    const trimmed = keyword.trim()
    if (trimmed && content.includes(trimmed)) {
      // 使用正则替换，避免重复替换
      const regex = new RegExp(`(${escapeRegExp(trimmed)})`, 'g')
      content = content.replace(regex, '<span class="highlight">$1</span>')
    }
  })
  
  return content
})

// 转义正则特殊字符
const escapeRegExp = (string) => {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

// 打字机效果相关
const defaultTypewriterWords = ['Clean Code', 'Web Development', 'Vue.js', 'Spring Boot', 'UI Design']
const typewriterWords = ref([...defaultTypewriterWords])
const displayText = ref('')
let wordIndex = 0
let charIndex = 0
let isDeleting = false

const typeWriter = () => {
  const words = typewriterWords.value
  if (words.length === 0) return
  
  const currentWord = words[wordIndex % words.length]
  
  if (!isDeleting) {
    // 打字
    displayText.value = currentWord.substring(0, charIndex + 1)
    charIndex++
    
    if (charIndex === currentWord.length) {
      isDeleting = true
      setTimeout(typeWriter, 2000) // 停顿2秒后开始删除
      return
    }
  } else {
    // 删除
    displayText.value = currentWord.substring(0, charIndex - 1)
    charIndex--
    
    if (charIndex === 0) {
      isDeleting = false
      wordIndex = (wordIndex + 1) % typewriterWords.value.length
    }
  }
  
  const delay = isDeleting ? 50 : 100
  setTimeout(typeWriter, delay)
}

// 获取分类徽章样式（使用文章主色调）
const getCategoryBadgeStyle = (article) => {
  const mainColor = article.mainColor || '#409EFF'
  return {
    background: mainColor.includes('rgb') 
      ? mainColor.replace(')', ', 0.9)').replace('rgb', 'rgba')
      : mainColor
  }
}

// 获取文章卡片边框样式（使用文章主色调）
const getArticleCardStyle = (article) => {
  return {
    '--article-main-color': article.mainColor || '#409EFF'
  }
}

// 获取博主信息
const fetchAboutInfo = () => {
  getAllBlogConfigs({
    showDefaultMsg: false,
    onSuccess: (data) => {
      aboutInfo.value = data || {}
    },
    onError: (error) => {
      console.error('获取博客配置失败:', error)
    }
  })
}

// 获取管理员信息（头像）
const fetchAdminInfo = () => {
  getUsersByRole('ADMIN', {
    showDefaultMsg: false,
    onSuccess: (data) => {
      if (data && data.length > 0) {
        const admin = data[0]
        adminAvatar.value = admin.avatar || ''
        adminName.value = admin.name || admin.username || ''
      }
    },
    onError: (error) => {
      console.error('获取管理员信息失败:', error)
    }
  })
}

// 初始化
onMounted(() => {
  fetchTypewriterWords() // 先获取打字机文本，然后启动打字效果
  fetchLatestArticles()
  fetchHotArticles()
  fetchCategories()
  fetchTags()
  fetchAboutInfo()
  fetchAdminInfo()
  fetchHeroSentence()
  fetchAuthorKeywords()
})

// 获取博主关键词配置
const fetchAuthorKeywords = () => {
  getBlogConfigByKey('author_keyword', {
    showDefaultMsg: false,
    onSuccess: (data) => {
      if (data) {
        const keywords = data.split(',').map(w => w.trim()).filter(w => w)
        if (keywords.length > 0) {
          authorKeywords.value = keywords
          // 如果有多个关键词，启动定时切换
          if (keywords.length > 1) {
            startAuthorKeywordRotation()
          }
        }
      }
    },
    onError: (error) => {
      console.error('获取博主关键词失败:', error)
    }
  })
}

// 启动博主关键词轮播
const startAuthorKeywordRotation = () => {
  if (authorKeywordTimer) {
    clearInterval(authorKeywordTimer)
  }
  authorKeywordTimer = setInterval(() => {
    currentAuthorKeywordIndex.value = (currentAuthorKeywordIndex.value + 1) % authorKeywords.value.length
  }, 3000) // 每3秒切换一次
}

// 获取打字机显示文本配置
const fetchTypewriterWords = () => {
  getBlogConfigByKey('home_display_text', {
    showDefaultMsg: false,
    onSuccess: (data) => {
      // 后端直接返回字符串类型的配置值
      if (data) {
        // 按英文逗号分割，并去除空白
        const words = data.split(',').map(w => w.trim()).filter(w => w)
        if (words.length > 0) {
          typewriterWords.value = words
        }
      }
      typeWriter() // 启动打字效果
    },
    onError: (error) => {
      console.error('获取打字机配置失败:', error)
      typeWriter() // 失败时使用默认文本启动
    }
  })
}

// 获取首页句子
const fetchHeroSentence = () => {
  getRandomHomepageSentence({
    showDefaultMsg: false,
    onSuccess: (data) => {
      if (data) {
        heroSentence.value = data
      }
    },
    onError: (error) => {
      console.error('获取首页句子失败:', error)
      // 失败时使用默认句子，不影响页面展示
    }
  })
}

// 获取最新文章
const fetchLatestArticles = () => {
  getLatestArticles({
    currentPage: 1,
    size:10,
    status: 1 // 已发布的文章
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      latestArticles.value = data.records || []
    },
    onError: (error) => {
      console.error('获取最新文章失败:', error)
    }
  })
}

// 获取热门文章
const fetchHotArticles = () => {
  getHotArticles({ limit: 10 }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      hotArticles.value = data || []
    },
    onError: (error) => {
      console.error('获取热门文章失败:', error)
    }
  })
}

// 获取顶级分类列表（包含子分类文章数量累计）
const fetchCategories = () => {
  getTopLevelCategories({
    showDefaultMsg: false,
    onSuccess: (data) => {
      categories.value = data
    },
    onError: (error) => {
      console.error('获取顶级分类列表失败:', error)
    }
  })
}

// 获取标签列表
const fetchTags = () => {
  getAllTags({
    showDefaultMsg: false,
    onSuccess: (data) => {
      tags.value = data
    },
    onError: (error) => {
      console.error('获取标签列表失败:', error)
    }
  })
}

// 格式化日期
const formatDate = (dateString) => {
  return DateUtils.formatDate(dateString);
}

// 跳转到文章详情页
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

// 3D标签云数据格式转换
const tagsCloudData = computed(() => {
  return tags.value.map(tag => ({
    name: tag.name,
    id: tag.id,
    count: tag.articleCount
  }))
})

// 3D标签云点击处理
const handleTagClick = (tag) => {
  goToTag(tag.id)
}

</script>

<style lang="scss" scoped>
// 核心颜色定义
$primary-blue: rgb(7, 26, 242);
$bg-light: #e8f4fc;
$glass-bg: rgba(255, 255, 255, 0.3);
$border-color: #f0f0f0;

.home-container {
  min-height: 100vh;
  // background-color: $bg-light;
  color: #333;
  position: relative;
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
}

// 装饰背景
.decorative-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
  
  .blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.15;
    
    &.blob-1 {
      width: 400px;
      height: 400px;
      background: $primary-blue;
      top: 10%;
      left: 20%;
    }
    
    &.blob-2 {
      width: 350px;
      height: 350px;
      background: #a855f7;
      top: 25%;
      right: 20%;
    }
  }
}

// HERO 区域
.hero-section {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 120px 20px 80px;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 800;
  color: #1f2937;
  margin-bottom: 1.5rem;
  letter-spacing: -0.02em;
  line-height: 1.3;
  max-width: 1200px;
  
  // 使用 :deep() 穿透 scoped 样式，因为 v-html 动态插入的内容
  :deep(.highlight) {
    color: $primary-blue;
  }
}

.typewriter-container {
  font-size: 1.5rem;
  color: #6b7280;
  margin-bottom: 2.5rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  
  .typewriter-prefix {
    color: #6b7280;
  }
  
  .typewriter-text {
    font-weight: 700;
    color: $primary-blue;
  }
  
  .cursor {
    display: inline-block;
    width: 3px;
    height: 1.5rem;
    background-color: $primary-blue;
    animation: blink 1s infinite;
  }
}

.hero-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background-color: transparent;
  color: $primary-blue;
  border: 2px solid $primary-blue;
  border-radius: 8px;
  padding: 0.75rem 1.5rem;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  
  &:hover {
    background-color: $primary-blue;
    color: white;
    box-shadow: 0 0 20px rgba(7, 26, 242, 0.3);
  }
}



// 主要内容区
.main-content {
  position: relative;
  z-index: 1;
  max-width: 1280px;
  margin: 0 auto;
  padding: 3rem 1rem;
  scroll-margin-top: 80px; // 补偿固定导航栏高度
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2rem;
  
  @media (min-width: 1024px) {
    grid-template-columns: 2fr 1fr;
  }
}

// 卡片基础样式
.glass-card {
  background-color: #fff;
  border: 1px solid rgba(226, 232, 240);
  border-radius: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02);
  transition: all 0.3s ease;
  min-width: 0;
}

// 文章列表区域
.articles-column {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-left: 0.75rem;
  border-left: 4px solid $primary-blue;
  
  i {
    color: $primary-blue;
  }
}

.view-more-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: transparent;
  border: none;
  border-radius: 8px;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: $primary-blue;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background: $primary-blue;
    color: white;
  }
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

// 文章卡片
.article-card {
  display: flex;
  flex-direction: column;
  padding: 0;
  cursor: pointer;
  overflow: hidden;
  --article-main-color: #409EFF; // 默认值，会被内联样式覆盖
  
  &:hover {
    border-color: var(--article-main-color);
  }
  
  @media (min-width: 768px) {
    flex-direction: row;
  }
}

.article-cover {
  position: relative;
  width: 100%;
  padding-bottom: 50%; // 2:1 比例
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  
  @media (min-width: 768px) {
    width: 280px;
    padding-bottom: 0;

  }
  
  .cover-image {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s ease;
  }
  
  .category-badge {
    position: absolute;
    top: 12px;
    left: 12px;
    background: var(--article-main-color);
    color: white;
    font-size: 12px;
    padding: 6px 12px;
    border-radius: 6px;
    font-weight: 500;
    backdrop-filter: blur(4px);
    z-index: 10;
    transition: all 0.3s ease;
  }
  
  .article-card:hover & .category-badge {
    transform: translateY(-2px);
  }
  
  .article-card:hover & .cover-image {
    transform: scale(1.05);
  }
}

.article-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: $glass-bg;
  backdrop-filter: blur(10px);
}

.article-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.75rem;
  
  i {
    margin-right: 0.25rem;
  }
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.75rem;
  line-height: 1.4;
  transition: color 0.3s ease;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-card:hover .article-title {
  color:$primary-blue;
}

.article-summary {
  color: #6b7280;
  font-size: 0.9rem;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 1rem;
  flex: 1;
}

.article-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.article-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  
  .tag-item {
    font-size: 0.75rem;
    color: $primary-blue;
    background: rgba(7, 26, 242, 0.05);
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      background: rgba(7, 26, 242, 0.1);
    }
  }
}

.read-more {
  font-size: 0.875rem;
  font-weight: 600;
  color: $primary-blue;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  
  i {
    transition: transform 0.3s ease;
  }
  
  &:hover i {
    transform: translateX(4px);
  }
}

.sidebar-sticky {
  position: -webkit-sticky; // Safari 兼容
  position: sticky;
  top: 80px; // 导航栏高度 + 间距
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sidebar-card {
  padding: 1.5rem;
  text-align: center;
  // 博主信息卡片特殊样式
  &.author-card {
    text-align: center;
    background: #425AEF;
    border: 1px solid rgba(226, 232, 240);
    border-radius: 20px;
    padding: 2rem 1.5rem;
    position: relative;
    overflow: hidden;
    
    &:hover .welcome-overlay {
      opacity: 1;
      visibility: visible;
    }
  }
  
  // 3D标签云卡片特殊样式
  &.tags-cloud-card {
    padding-bottom: 1rem;
    overflow: visible;
  }
}

// 欢迎语遮罩层样式
.welcome-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #425AEF;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 2rem;
  opacity: 0;
  visibility: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 10;
  border-radius: 20px;
  
  .welcome-icon {
    font-size: 3rem;
    color: #fff;
    margin-bottom: 1rem;
  }
  
  .welcome-text {
    font-size: 3rem;
    font-weight: 700;
    color: #fff;
    margin: 0 0 0.6rem;
    letter-spacing: 1px;
  }
  
  .welcome-sub {
    font-size: 2rem;
    color: rgba(255, 255, 255, 0.8);
    margin: 0;
  }
  
  .welcome-line {
    width: 50px;
    height: 3px;
    background: rgba(255, 255, 255, 0.5);
    margin-top: 1.2rem;
    border-radius: 2px;
  }
}

// 博主信息卡片样式
.author-avatar {
  margin-bottom: 1rem;
  
  .el-avatar {
    border: 4px solid #fff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.author-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 0.5rem;
}

.author-title {
  display: inline-block;
  font-size: 0.875rem;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  padding: 5px 20px;
  border-radius: 20px;
  margin: 0 0 1rem;
  width: 12em; 
  text-align: center;
}

.author-intro {
  font-size: 0.875rem;
  color: #fff;
  line-height: 1.6;
  margin: 0 0 1.25rem;
}



.sidebar-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  
  i {
    color: $primary-blue;
    
    &.fa-fire {
      color: #ef4444;
    }
  }
}

// 标签云
.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: white;
  color: #555;
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 0.25rem 0.75rem;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.3s ease;
  
  .tag-count {
    font-size: 0.75rem;
    color: #9ca3af;
  }
  
  &:hover {
    color: $primary-blue;
    background: rgba(7, 26, 242, 0.05);
  }
}

// 热门文章列表
.hot-articles-list {
  list-style: none;
  padding: 0;
  margin: 0;
  
  li {
    display: flex;
    gap: 0.75rem;
    padding: 0.75rem 0;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:not(:last-child) {
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    }
    
    &:hover {
      .rank-number {
        color: $primary-blue;
      }
      
      h4 {
        color: $primary-blue;
      }
    }
  }
  
  .rank-number {
    font-size: 1.5rem;
    font-weight: 700;
    color: #e5e7eb;
    transition: color 0.3s ease;
    min-width: 2rem;
  }
  
  .hot-article-info {
    flex: 1;
    
    h4 {
      font-size: 0.875rem;
      font-weight: 600;
      color: #1f2937;
      margin-bottom: 0.25rem;
      line-height: 1.4;
      transition: color 0.3s ease;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    
    .view-count {
      font-size: 0.75rem;
      color: #9ca3af;
    }
  }
}

// 分类列表
.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
  
  li {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem 0;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:not(:last-child) {
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    }
    
    &:hover {
      .category-name {
        color: $primary-blue;
      }
    }
  }
  
  .category-name {
    font-size: 0.9rem;
    color: #1f2937;
    font-weight: 500;
    transition: color 0.3s ease;
  }
  
  .category-count {
    font-size: 0.75rem;
    color: white;
    background: $primary-blue;
    padding: 0.125rem 0.5rem;
    border-radius: 999px;
    min-width: 1.5rem;
    text-align: center;
  }
}

// 动画
@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

// 关键词切换过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

// 响应式
@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }
  
  .typewriter-container {
    font-size: 1.25rem;
    flex-direction: column;
    height: auto;
    gap: 0.25rem;
  }
  
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .view-more-btn {
    align-self: flex-end;
  }
}
</style>



