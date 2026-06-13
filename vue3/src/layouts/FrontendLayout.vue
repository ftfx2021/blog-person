<template>
    <div class="frontend-layout">
      <!-- 搜索对话框 -->
      <SearchDialog v-model="showSearchDialog" />
      
      <!-- 二次元风格背景 -->
      <div class="anime-bg">
      </div>
      
      <!-- 顶部导航栏 -->
      <Navbar 
        :blog-name="blogName" 
        :is-floating="shouldNavbarFloat" 
        :background-color="articleMainColor"
      />
  
      <!-- 主要内容区域 -->
      <main class="main-content">
        <router-view />
      </main>
      
  
      <!-- 页脚 -->
      <footer class="footer">
        <div class="footer-decoration">
          <!-- 添加页脚装饰元素 -->
          <div class="footer-flower left"></div>
          <div class="footer-flower right"></div>
        </div>
        <div class="footer-content">
          <div class="footer-logo">
            <span class="logo-icon">✒️</span>
            <span class="logo-text">{{ blogName }}</span>
          </div>
          <p class="footer-text">{{ footerText }}</p>
          <p v-if="icpInfo" class="icp-info">{{ icpInfo }}</p>
          <p v-if="gonganInfo" class="gongan-info">{{ gonganInfo }}</p>
        </div>
      </footer>
    </div>
  </template>
  
  <script setup>
  import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
  import { useUserStore } from '@/store/user'
  import { useRouter, useRoute } from 'vue-router'
  import { Setting } from '@element-plus/icons-vue'
  import { getAllBlogConfigs } from '@/api/BlogConfigApi'
  import { getArticleDetail } from '@/api/ArticleApi'
  import Navbar from '@/components/frontend/Navbar.vue'
  import SearchDialog from '@/components/frontend/SearchDialog.vue'

  const userStore = useUserStore()
  const router = useRouter()
  const route = useRoute()
  const blogConfig = ref({})
  const showSearchDialog = ref(false)
  const articleMainColor = ref(null) // 文章主色调

  // 判断是否为后台用户
  const isBackendUser = computed(() => userStore.isLoggedIn && !userStore.isUser)
  
  // 判断是否为文章详情页
  const isArticleDetailPage = computed(() => route.path.startsWith('/article/') && route.params.id)
  
  // 判断导航栏是否应该悬浮（只有首页和文章详情页开启）
  const shouldNavbarFloat = computed(() => {
    return route.path === '/' || isArticleDetailPage.value
  })
  
  // 博客名称
  const blogName = computed(() => {
    return blogConfig.value.blog_name || '响の博客'
  })
  
  // 页脚文本
  const footerText = computed(() => {
    return blogConfig.value.blog_footer || ' 2025 个人博客 All Rights Reserved'
  })
  
  // ICP备案信息
  const icpInfo = computed(() => {
    return blogConfig.value.blog_icp || ''
  })
  
  // 公安备案信息
  const gonganInfo = computed(() => {
    return blogConfig.value.blog_gongan || ''
  })
  
  // 获取文章主色调
  const fetchArticleMainColor = async (articleId) => {
    console.log('[FrontendLayout] 开始获取文章主色调, articleId:', articleId)
    try {
      getArticleDetail(articleId, {}, {
        showDefaultMsg: false,
        onSuccess: (data) => {
          articleMainColor.value = data.mainColor || null
          console.log('[FrontendLayout] 获取到文章主色调:', articleMainColor.value)
        },
        onError: (error) => {
          console.error('获取文章主色调失败:', error)
          articleMainColor.value = null
        }
      })
    } catch (error) {
      console.error('获取文章主色调失败:', error)
      articleMainColor.value = null
    }
  }
  
  // 监听路由变化，获取文章主色调
  watch(
    () => route.params.id,
    (newId) => {
      if (isArticleDetailPage.value && newId) {
        fetchArticleMainColor(newId)
      } else {
        articleMainColor.value = null
      }
    },
    { immediate: true }
  )
  
  // 获取博客配置
  const fetchBlogConfig = async () => {
    try {
      getAllBlogConfigs({
        showDefaultMsg: false,
        onSuccess: (data) => {
          blogConfig.value = data || {}
        },
        onError: (error) => {
          console.error('获取博客配置失败:', error)
        }
      })
    } catch (error) {
      console.error('获取博客配置失败:', error)
    }
  }
  

  // 监听搜索点击事件
  const handleSearchClick = () => {
    showSearchDialog.value = true
  }
  
  // 生命周期钩子
  onMounted(() => {
    fetchBlogConfig()
    // 监听 navbar 的搜索点击事件
    window.addEventListener('search-click', handleSearchClick)
    
    // 如果当前就在文章详情页，立即获取主色调
    if (isArticleDetailPage.value && route.params.id) {
      fetchArticleMainColor(route.params.id)
    }
  })
  
  onUnmounted(() => {
    // 移除搜索点击事件监听
    window.removeEventListener('search-click', handleSearchClick)
  })
  </script>
  
  <style lang="scss" scoped>

 
  .frontend-layout {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    position: relative;
    /* overflow-x: hidden; 移除此行，会导致子元素sticky失效 */
 
    background-color: #f7f9fe;
  }
  
  /* 二次元风格背景 */
  .anime-bg {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 0;
    pointer-events: none;
    overflow: hidden;
  }
  

  
  
  /* 主要内容区域 */
  .main-content {
    flex: 1;
    position: relative;
    z-index: 1;
    padding: 0;
    width: 100%;
  }
  

  

  
  /* 页脚 */
  .footer {
    background: linear-gradient(135deg, rgba(45, 55, 72, 0.95) 0%, rgba(26, 32, 44, 0.95) 100%);
    color: #FFFFFF;
    padding: 60px 40px 40px;
    text-align: center;
    position: relative;
    z-index: 1;
    border-top: 1px solid rgba(246, 173, 85, 0.2);
    width: 100%;
    backdrop-filter: blur(10px);
  }

  .footer-decoration {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 60px;
    overflow: hidden;
  }

  .footer-decoration::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(246, 173, 85, 0.5), rgba(104, 211, 145, 0.5), transparent);
  }

  .footer-flower {
    position: absolute;
    width: 60px;
    height: 60px;
    background: linear-gradient(135deg, rgba(246, 173, 85, 0.1), rgba(104, 211, 145, 0.1));
    border-radius: 50%;
    opacity: 0.3;
  }

  .footer-flower.left {
    top: -15px;
    left: 10%;
    transform: rotate(-15deg);
    background: radial-gradient(circle, rgba(246, 173, 85, 0.15) 0%, transparent 70%);
  }

  .footer-flower.right {
    top: -15px;
    right: 10%;
    transform: rotate(15deg);
    background: radial-gradient(circle, rgba(104, 211, 145, 0.15) 0%, transparent 70%);
  }

  .footer-content {
    width: 100%;
    max-width: 800px;
    margin: 0 auto;
  }

  .footer-logo {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;
    
    .logo-icon {
      font-size: 24px;
      margin-right: 10px;
      filter: drop-shadow(0 2px 4px rgba(246, 173, 85, 0.4));
    }
    
    .logo-text {
   
      font-weight: 600;
      font-size: 18px;
      color: #F6AD55;
    }
  }

  .footer-text {
  
    margin-bottom: 20px;
    color: rgba(255, 255, 255, 0.8);
    font-size: 14px;
    font-weight: 400;
    line-height: 1.6;
  }

  .icp-info, .gongan-info {
    margin-top: 10px;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.5);

    letter-spacing: 0.025em;
    
    &:hover {
      color: rgba(246, 173, 85, 0.8);
      transition: color 0.3s ease;
    }
  }
  
  @media (max-width: 768px) {
    .footer {
      padding: 40px 20px 30px;
    }
    
    .footer-content {
      padding: 0 10px;
    }
    
    .footer-logo .logo-text {
      font-size: 16px;
    }
    
    .footer-text {
      font-size: 13px;
    }
  }
  </style>