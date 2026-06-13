<template>
  <div class="article-detail-container">
    <!-- 密码验证弹窗 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="文章访问验证"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      center
    >
      <div class="password-dialog-content">
        <div class="password-icon">
          <el-icon :size="48" color="#E6A23C"><Lock /></el-icon>
        </div>
        <p class="password-hint">此文章已开启密码保护，请输入访问密码</p>
        <el-input
          v-model="inputPassword"
          type="password"
          placeholder="请输入访问密码"
          show-password
          size="large"
          @keyup.enter="handleVerifyPassword"
        />
        <p class="password-error" v-if="passwordError">{{ passwordError }}</p>
      </div>
      <template #footer>
        <div class="password-dialog-footer">
          <el-button @click="goBack">返回</el-button>
          <el-button type="primary" @click="handleVerifyPassword" :loading="verifyLoading">
            验证
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 文章操作对话框 -->
    <ArticleActionDialog 
      v-model="showActionDialog" 
      :article-id="route.params.id"
    />

    <div class="article-layout" v-loading="loading" :class="{ 'with-outline-open': outlineVisible && parsedOutline.length > 0 }">
      <template v-if="article && !needPassword">
        <!-- HEO风格的文章头部背景区域 -->
        <div class="heo-article-hero" :style="heroStyle">
          <!-- 左边渐变模糊层 -->
          <div class="gradient-blur-left" :style="{ background: leftGradient }"></div>
          
          <!-- 右边渐变模糊层 -->
          <div class="gradient-blur-right" :style="{ background: rightGradient }"></div>
          
          <div class="hero-content" :style="{ color: textColor }">
          
            <h1 class="hero-title" :style="{ color: textColor }">{{ article.title }}</h1>
            <div class="hero-meta">
              <div class="meta-item" :style="{ color: metaTextColor, background: metaBackground, borderColor: metaBorderColor }">
                <i class="fa fa-user"></i>
                <span>{{ article.authorName }}</span>
              </div>
              <div class="meta-item" :style="{ color: metaTextColor, background: metaBackground, borderColor: metaBorderColor }">
                <i class="fa fa-calendar"></i>
                <span>{{ formatDate(article.createTime) }}</span>
              </div>
              <div class="meta-item" :style="{ color: metaTextColor, background: metaBackground, borderColor: metaBorderColor }">
                <i class="fa fa-folder"></i>
                <span>{{ article.categoryName }}</span>
              </div>
              <div class="meta-item" :style="{ color: metaTextColor, background: metaBackground, borderColor: metaBorderColor }">
                <i class="fa fa-eye"></i>
                <span>{{ article.viewCount }}</span>
              </div>
              <div class="meta-item" :style="{ color: metaTextColor, background: metaBackground, borderColor: metaBorderColor }">
                <i class="fa fa-comment"></i>
                <span>{{ article.commentCount }}</span>
              </div>
            </div>
            <div class="hero-tags">
              <DynamicTag
                v-for="tag in article.tags"
                :key="tag.id"
                class="hero-tag-item"
                :label="tag.name"
                :bgColor="tag.color"
                :textColor="tag.textColor"
                size="small"
                @click="goToTag(tag.id)"
              />
            </div>
          </div>
        </div>

        <!-- 主内容区域 - 分为左右两栏 -->
        <main class="article-main">
          <!-- 左侧侧边栏区域 -->
          <aside 
            ref="sidebarElement"
            class="article-sidebar" 
            v-if="parsedOutline.length > 0 || relatedArticles.length > 0"
          >
            <!-- 文章大纲 -->
            <div class="outline-container" v-if="parsedOutline.length > 0">
              <div class="outline-header">
                <h3 class="outline-title">
                  <el-icon class="title-icon"><List /></el-icon>
                  文章大纲
                </h3>
              </div>
              <nav class="outline-nav">
                <div 
                  v-for="(item, index) in parsedOutline" 
                  :key="index"
                  class="outline-nav-item" 
                  :class="[
                    `outline-level-${item.level}`,
                    { 'outline-active': activeOutlineIndex === index }
                  ]"
                  @click="handleOutlineClick(item.anchorId || item.title, index)"
                >
                  <div class="outline-nav-content">
                    <span class="outline-nav-title">{{ item.title }}</span>
                  </div>
                </div>
              </nav>
            </div>

            <!-- 相关推荐 -->
            <div class="related-container" v-if="relatedArticles.length > 0">
              <div class="related-header">
                <h3 class="related-title">
                  <el-icon class="title-icon"><Star /></el-icon>
                  相关推荐
                </h3>
              </div>
              <div class="related-list-sidebar">
                <div 
                  v-for="article in relatedArticles" 
                  :key="article.id" 
                  class="related-item-sidebar"
                  @click="goToArticle(article.id)"
                >
                  <el-image 
                    v-if="article.coverImage" 
                    :src="article.coverImage" 
                    fit="cover" 
                    class="related-cover-sidebar"
                  />
                  <div class="related-info-sidebar">
                    <h4 class="related-title-sidebar">{{ article.title }}</h4>
                    <div class="related-meta-sidebar">
                      <span><i class="fa fa-eye"></i> {{ article.viewCount }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </aside>

          <!-- 右侧文章内容区域 -->
          <div 
            class="article-content-wrapper" 
            :class="{ 'full-width': parsedOutline.length === 0 }"
          >
            <!-- 文章内容 -->
            <div class="article-body rich-text-content" v-html="article.htmlContent"></div>

            <!-- 评论区 -->
            <div class="comments-section">
              <comment-list 
                :article-id="route.params.id" 
                @comment-added="handleCommentAdded"
                @comment-deleted="handleCommentDeleted"
              />
            </div>
          </div>
        </main>
      </template>

      <el-empty v-else description="文章不存在或已被删除" />
    </div>

    <!-- 右下角悬浮操作按钮 -->
    <div class="floating-actions" v-if="article && !needPassword">
      <button 
        class="float-btn like-btn" 
        :class="{ 'active': isLiked }"
        @click="handleLike"
        :disabled="likeLoading"
        :title="isLiked ? '取消点赞' : '点赞'"
      >
        <i :class="isLiked ? 'fa fa-heart' : 'far fa-heart'"></i>
        <span class="float-count" v-if="article.likeCount > 0">{{ article.likeCount }}</span>
      </button>
      
      <button 
        class="float-btn collect-btn" 
        :class="{ 'active': isCollected }"
        @click="handleCollect"
        :disabled="collectLoading"
        :title="isCollected ? '取消收藏' : '收藏'"
      >
        <i :class="isCollected ? 'fa fa-bookmark' : 'far fa-bookmark'"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { List, Close, Star, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import DateUtils from '@/utils/dateUtils'
import CommentList from '@/components/Comment/CommentList.vue'
import ArticleActionDialog from '@/components/frontend/ArticleActionDialog.vue'
import { 
  getArticleDetail, 
  getArticlesByCategory, 
  checkArticleLikeStatus, 
  checkArticleCollectStatus,
  toggleArticleLike,
  toggleArticleCollect
} from '@/api/ArticleApi'
import DynamicTag from '@/components/common/DynamicTag.vue'
// 导入 highlight.js 核心库和样式
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css' // GitHub 亮色主题
// 导入颜色工具
import { generateCoverStyles } from '@/utils/colorUtils'

// 使用DateUtils格式化日期
const formatDate = (date) => {
  return DateUtils.formatDateTime(date);
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()


// 加载状态
const loading = ref(false)
// 文章信息
const article = ref(null)
// 相关文章
const relatedArticles = ref([])
// 删除未使用的变量
// 是否已点赞
const isLiked = ref(false)
// 是否显示操作对话框
const showActionDialog = ref(false)
// 是否已收藏
const isCollected = ref(false)
const collectLoading = ref(false)
const likeLoading = ref(false)

// 密码保护相关状态
const needPassword = ref(false)
const showPasswordDialog = ref(false)
const inputPassword = ref('')
const passwordError = ref('')
const verifyLoading = ref(false)
// 当前激活的大纲项索引
const activeOutlineIndex = ref(-1)
// 滚动锁定状态，用于防止点击大纲项时的闪烁
const scrollLock = ref(false)
const outlineVisible = ref(true)
// 侧边栏引用
const sidebarElement = ref(null)
const scrollListener = ref(null)

// 解析大纲数据 - 直接从文章内容中自动提取
const parsedOutline = computed(() => {
  return extractOutlineFromContent()
})

// 使用颜色工具生成所有样式
const colorStyles = computed(() => {
  const mainColor = article.value?.mainColor || '#3498db'
  return generateCoverStyles(mainColor,{
    lightOpacity: 1,
    darkOpacity: 1
  })
})

// 提取各个样式属性
const heroStyle = computed(() => colorStyles.value.heroStyle)
const textColor = computed(() => colorStyles.value.textColor)
const textShadow = computed(() => colorStyles.value.textShadow)
const leftGradient = computed(() => colorStyles.value.leftGradient)
const rightGradient = computed(() => colorStyles.value.rightGradient)
const metaTextColor = computed(() => colorStyles.value.metaTextColor)
const metaBackground = computed(() => colorStyles.value.metaBackground)
const metaBorderColor = computed(() => colorStyles.value.metaBorderColor)

// 从文章内容中自动提取大纲（只提取h1、h2、h3）
const extractOutlineFromContent = () => {
  if (!article.value?.htmlContent) return []
  
  try {
    // 创建临时DOM来解析HTML内容
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = article.value.htmlContent
    
    // 只查找h1、h2、h3标题元素
    const headings = tempDiv.querySelectorAll('h1, h2, h3')
    
    if (headings.length === 0) return []
    
    const outline = []
    headings.forEach((heading, index) => {
      const level = parseInt(heading.tagName.substring(1)) // h1 -> 1, h2 -> 2, h3 -> 3
      const title = heading.textContent.trim()
      
      // 为标题生成或获取ID
      let anchorId = heading.id
      if (!anchorId) {
        anchorId = `heading-${index + 1}`
        // 同时为实际DOM中的标题元素添加ID（在渲染后）
      }
      
      outline.push({
        level: level,
        title: title,
        anchorId: anchorId
      })
    })
    
    return outline
  } catch (error) {
    console.error('自动提取大纲失败:', error)
    return []
  }
}

// 大纲标签类型
const getOutlineTagType = (level) => {
  const types = ['', 'danger', 'warning', 'success', 'info', 'primary', '']
  return types[level] || 'info'
}

// 切换大纲显示状态
const toggleOutline = () => {
  outlineVisible.value = !outlineVisible.value
}



// 生成数字编号
const generateOutlineNumbers = (outline) => {
  const counters = [0, 0, 0, 0, 0, 0] // 支持六级标题
  
  return outline.map(item => {
    const level = item.level - 1 // 转换为数组索引
    
    // 重置下级计数器
    for (let i = level + 1; i < counters.length; i++) {
      counters[i] = 0
    }
    
    // 增加当前级别计数器
    counters[level]++
    
    // 生成编号字符串
    let number = ''
    for (let i = 0; i <= level; i++) {
      if (counters[i] > 0) {
        number += (number ? '.' : '') + counters[i]
      }
    }
    
    return {
      ...item,
      number: number
    }
  })
}

// 处理大纲点击事件
const handleOutlineClick = (anchorIdOrTitle, index) => {
  // 先手动设置激活的大纲项，避免滚动过程中的闪烁
  activeOutlineIndex.value = index
  
  // 临时禁用滚动监听，避免滚动过程中重新计算激活项
  scrollLock.value = true
  
  // 使用统一的滚动函数定位到目标元素
  scrollToElement(anchorIdOrTitle, index)
  
  // 1秒后允许滚动监听重新计算
  setTimeout(() => {
    scrollLock.value = false
  }, 1000)
}

// 移除复杂的定位逻辑，使用纯CSS sticky

// 组件卸载时移除监听
onUnmounted(() => {
  if (scrollListener.value) {
    window.removeEventListener('scroll', scrollListener.value)
  }
  // 移除文章操作点击事件监听
  window.removeEventListener('article-action-click', handleArticleActionClick)
})


// 滚动监听，更新活动的大纲项
const updateActiveOutline = () => {
  // 如果大纲为空或处于锁定状态，则不更新
  if (parsedOutline.value.length === 0 || scrollLock.value) return
  
  const articleBody = document.querySelector('.rich-text-content')
  if (!articleBody) return
  

  
  const headings = articleBody.querySelectorAll('h1, h2, h3, h4, h5, h6')
  if (headings.length === 0) return
  
  // 获取视口信息
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  
  // 获取导航栏高度（统一选择器，包含 .site-header）
  const navbar = document.querySelector('.navbar, .header, nav, .site-header')
  const headerOffset = navbar ? navbar.offsetHeight + 20 : 80
  
  // 创建标题ID到大纲索引的映射
  const headingMap = new Map()
  parsedOutline.value.forEach((item, index) => {
    headingMap.set(item.anchorId, { index, title: item.title })
  })
  
  // 找到当前在视口内或刚刚离开视口上方的标题
  let bestHeading = null
  let bestScore = -Infinity
  
  for (const heading of headings) {
    const rect = heading.getBoundingClientRect()
    
    // 计算标题位置得分
    // 1. 如果标题在视口顶部附近，得分最高
    // 2. 如果标题在视口内，得分次之
    // 3. 如果标题刚离开视口上方，得分再次
    // 4. 如果标题在视口下方，得分最低
    let score = 0
    
    if (rect.top >= 0 && rect.top <= headerOffset + 50) {
      // 标题在视口顶部附近（考虑导航栏高度），最理想的位置
      score = 1000 - Math.abs(rect.top - headerOffset)
    } else if (rect.top > headerOffset && rect.top < windowHeight * 0.8) {
      // 标题在视口内，但不在顶部
      score = 500 - rect.top
    } else if (rect.top <= 0 && rect.bottom > 0) {
      // 标题部分可见（顶部已滚出视口）
      score = 200 + rect.bottom
    } else if (rect.top <= 0) {
      // 标题已完全滚出视口上方
      score = 100 - Math.abs(rect.top)
    } else {
      // 标题在视口下方
      score = -rect.top
    }
    
    if (score > bestScore) {
      bestScore = score
      bestHeading = heading
    }
  }
  
  // 如果找到了最佳标题，尝试更新活动索引
  if (bestHeading) {
    // 首先尝试通过ID匹配
    const headingId = bestHeading.id
    if (headingId && headingMap.has(headingId)) {
      activeOutlineIndex.value = headingMap.get(headingId).index
      return
    }
    
    // 如果没有ID匹配，尝试通过标题文本匹配
    const headingText = bestHeading.textContent.trim()
    
    // 寻找标题文本最匹配的大纲项
    let bestMatchIndex = -1
    let bestMatchScore = 0
    
    parsedOutline.value.forEach((item, index) => {
      if (!item.title) return
      
      const outlineTitle = item.title.trim()
      let matchScore = 0
      
      // 完全匹配得分最高
      if (headingText === outlineTitle) {
        matchScore = 100
      } 
      // 包含关系得分次之
      else if (headingText.includes(outlineTitle)) {
        matchScore = 50 + (outlineTitle.length / headingText.length) * 50
      }
      else if (outlineTitle.includes(headingText)) {
        matchScore = 50 + (headingText.length / outlineTitle.length) * 50
      }
      // 部分单词匹配得分再次
      else {
        const headingWords = headingText.toLowerCase().split(/\s+/)
        const outlineWords = outlineTitle.toLowerCase().split(/\s+/)
        const commonWords = headingWords.filter(word => outlineWords.includes(word))
        if (commonWords.length > 0) {
          matchScore = (commonWords.length / Math.max(headingWords.length, outlineWords.length)) * 40
        }
      }
      
      if (matchScore > bestMatchScore) {
        bestMatchScore = matchScore
        bestMatchIndex = index
      }
    })
    
    if (bestMatchIndex >= 0) {
      activeOutlineIndex.value = bestMatchIndex
    }
  }
}

// 监听 navbar 的点击事件
const handleArticleActionClick = () => {
  showActionDialog.value = true
}

// 初始化
onMounted(() => {
  const articleId = route.params.id
  if (articleId) {
    // 直接尝试获取文章，后端会判断是否需要密码（管理员可直接访问）
    tryFetchArticle(articleId)
  }
  
  // 监听 navbar 的文章操作点击事件
  window.addEventListener('article-action-click', handleArticleActionClick)
  
  // 创建节流函数 - 只更新大纲高亮
  const throttledUpdate = throttle(() => {
    updateActiveOutline()
  }, 100, {
    leading: true,
    trailing: true
  })
  scrollListener.value = throttledUpdate
  
  // 添加滚动监听
  window.addEventListener('scroll', scrollListener.value)
  
  // 初始执行一次，确保页面加载时就有正确的大纲高亮
  setTimeout(() => {
    updateActiveOutline()
    
    // 如果URL中有锚点，滚动到对应位置
    if (location.hash) {
      const anchorId = location.hash.substring(1)
      const targetElement = document.getElementById(anchorId)
      if (targetElement) {
        scrollToElement(targetElement)
      }
    }
  }, 800)
  
})

// 移除watch，不再需要动态初始化

// 优化的节流函数，带有立即执行和结束执行的选项
const throttle = (func, delay, options = {}) => {
  let timeoutId = null
  let lastExecTime = 0
  const { leading = true, trailing = true } = options
  
  return function (...args) {
    const currentTime = Date.now()
    const remaining = delay - (currentTime - lastExecTime)
    
    // 如果是第一次调用或者已经过了节流时间，并且允许立即执行
    if ((lastExecTime === 0 || remaining <= 0) && leading) {
      if (timeoutId) {
        clearTimeout(timeoutId)
        timeoutId = null
      }
      func.apply(this, args)
      lastExecTime = currentTime
    } else if (!timeoutId && trailing) {
      // 如果没有待执行的定时器，并且允许结束执行，则设置定时器
      timeoutId = setTimeout(() => {
        // 如果leading为false，并且这是第一次调用，lastExecTime保持为0
        // 否则更新lastExecTime
        lastExecTime = leading ? Date.now() : 0
        timeoutId = null
        func.apply(this, args)
      }, remaining > 0 ? remaining : delay)
    }
  }
}

// 尝试获取文章（后端会判断权限，管理员可直接访问加密文章）
const tryFetchArticle = (id) => {
  loading.value = true
  getArticleDetail(id, {}, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      // 成功获取文章（管理员或无密码文章）
      needPassword.value = false
      article.value = data
      document.title = `${data.title} - 个人博客`
      
      // 获取相关文章
      fetchRelatedArticles(data.categoryId)
      loading.value = false
      
      // 在下一个DOM更新周期后应用代码高亮和添加标题ID
      nextTick(() => {
        applyCodeHighlight()
        addHeadingIds()
      })
      
      // 检查用户交互状态
      checkUserInteractions()
    },
    onError: (error) => {
      loading.value = false
      // 检查是否是需要密码的错误
      if (error && error.message) {
        if (error.message.includes('密码访问') || error.message.includes('请输入密码')) {
          // 需要密码验证
          needPassword.value = true
          showPasswordDialog.value = true
        } else {
          // 其他错误
          console.error('获取文章失败:', error)
          ElMessage.error(error.message || '获取文章失败')
        }
      } else {
        console.error('获取文章失败:', error)
        ElMessage.error('获取文章失败')
      }
    }
  })
}

// 验证密码（通过获取文章详情接口验证）
const handleVerifyPassword = () => {
  if (!inputPassword.value.trim()) {
    passwordError.value = '请输入密码'
    return
  }
  
  verifyLoading.value = true
  passwordError.value = ''
  
  // 直接调用获取文章详情接口，传入密码进行验证
  getArticleDetail(route.params.id, { password: inputPassword.value }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      verifyLoading.value = false
      // 密码正确，文章加载成功
      needPassword.value = false
      showPasswordDialog.value = false
      inputPassword.value = ''
      
      // 设置文章数据
      article.value = data
      document.title = `${data.title} - 个人博客`
      
      // 获取相关文章
      fetchRelatedArticles(data.categoryId)
      loading.value = false
      
      // 在下一个DOM更新周期后应用代码高亮和添加标题ID
      nextTick(() => {
        applyCodeHighlight()
        addHeadingIds()
      })
      
      // 检查用户交互状态
      checkUserInteractions()
    },
    onError: (error) => {
      verifyLoading.value = false
      // 根据错误信息判断是密码错误还是其他错误
      if (error && error.message) {
        if (error.message.includes('密码错误')) {
          passwordError.value = '密码错误，请重新输入'
        } else if (error.message.includes('密码访问')) {
          passwordError.value = '请输入正确的访问密码'
        } else {
          passwordError.value = error.message || '验证失败，请重试'
        }
      } else {
        passwordError.value = '验证失败，请重试'
      }
      console.error('密码验证失败:', error)
    }
  })
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 获取文章详情
const fetchArticleDetail = (id) => {
  loading.value = true
  getArticleDetail(id, {}, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      article.value = data
      document.title = `${data.title} - 个人博客`
      
      // 获取相关文章
      fetchRelatedArticles(data.categoryId)
      loading.value = false
      
      // 在下一个DOM更新周期后应用代码高亮和添加标题ID
      nextTick(() => {
        applyCodeHighlight()
        addHeadingIds()
      })
    },
    onError: (error) => {
      console.error('获取文章详情失败:', error)
      ElMessage.error('获取文章详情失败')
      loading.value = false
    }
  })
}

// 应用代码高亮
const applyCodeHighlight = () => {
  // 查找文章内容中的所有代码块
  const articleBody = document.querySelector('.article-body')
  if (!articleBody) return
  
  const codeBlocks = articleBody.querySelectorAll('pre code')
  codeBlocks.forEach((block) => {
    // 应用高亮
    hljs.highlightElement(block)
    
    // 为每个代码块添加复制按钮
    const pre = block.parentElement
    if (pre && !pre.querySelector('.code-copy-btn')) {
      addCopyButton(pre, block)
    }
  })
}

// 为文章标题添加ID，以便大纲定位
const addHeadingIds = () => {
  const articleBody = document.querySelector('.rich-text-content')
  if (!articleBody) return
  
  const headings = articleBody.querySelectorAll('h1, h2, h3, h4, h5, h6')
  headings.forEach((heading, index) => {
    // 如果标题已有ID，保持不变；否则添加ID
    if (!heading.id) {
      heading.id = `heading-${index + 1}`
    }
  })
}

// 为代码块添加复制按钮
const addCopyButton = (pre, codeBlock) => {
  // 创建复制按钮容器
  const copyBtn = document.createElement('button')
  copyBtn.className = 'code-copy-btn'
  copyBtn.innerHTML = '<i class="fa fa-copy"></i>'
  copyBtn.title = '复制代码'
  
  // 点击复制
  copyBtn.addEventListener('click', async () => {
    try {
      const code = codeBlock.textContent
      
      // 优先使用现代 Clipboard API
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(code)
      } else {
        // 降级方案：使用传统的 document.execCommand
        const textarea = document.createElement('textarea')
        textarea.value = code
        textarea.style.position = 'fixed'
        textarea.style.opacity = '0'
        document.body.appendChild(textarea)
        textarea.select()
        document.execCommand('copy')
        document.body.removeChild(textarea)
      }
      
      // 显示复制成功状态
      copyBtn.innerHTML = '<i class="fa fa-check"></i>'
      copyBtn.classList.add('copied')
      ElMessage.success('代码已复制到剪贴板')
      
      // 2秒后恢复按钮状态
      setTimeout(() => {
        copyBtn.innerHTML = '<i class="fa fa-copy"></i>'
        copyBtn.classList.remove('copied')
      }, 2000)
    } catch (err) {
      console.error('复制失败:', err)
      ElMessage.error('复制失败，请手动复制')
    }
  })
  
  // 将按钮添加到 pre 标签
  pre.style.position = 'relative'
  pre.appendChild(copyBtn)
}

// 获取相关文章
const fetchRelatedArticles = (categoryId) => {
  if (!categoryId) return
  
  getArticlesByCategory(categoryId, {
    currentPage: 1,
    size: 5
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      // 过滤掉当前文章
      relatedArticles.value = data.records.filter(item => item.id !== article.value.id).slice(0, 4)
    },
    onError: (error) => {
      console.error('获取相关文章失败:', error)
    }
  })
}

// 检查当前用户是否已点赞和收藏文章
const checkUserInteractions = () => {
  if (!userStore.isLoggedIn) return
  
  // 检查点赞状态
  checkArticleLikeStatus(route.params.id, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      isLiked.value = data.liked
    },
    onError: (error) => {
      console.error('获取点赞状态失败:', error)
    }
  })
  
  // 检查收藏状态
  checkArticleCollectStatus(route.params.id, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      isCollected.value = data.collected
    },
    onError: (error) => {
      console.error('获取收藏状态失败:', error)
    }
  })
}

// 点赞或取消点赞
const handleLike = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  likeLoading.value = true
  toggleArticleLike(route.params.id, {
    successMsg: isLiked.value ? '已取消点赞' : '点赞成功',
    onSuccess: (data) => {
      isLiked.value = data.liked
      // 重新获取文章详情以更新统计数据
      fetchArticleDetail(route.params.id)
      likeLoading.value = false
    },
    onError: (error) => {
      console.error('点赞操作失败:', error)
      likeLoading.value = false
    }
  })
}

// 收藏或取消收藏
const handleCollect = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  collectLoading.value = true
  toggleArticleCollect(route.params.id, {
    successMsg: isCollected.value ? '已取消收藏' : '收藏成功',
    onSuccess: (data) => {
      isCollected.value = data.collected
      collectLoading.value = false
    },
    onError: (error) => {
      console.error('收藏操作失败:', error)
      collectLoading.value = false
    }
  })
}

// 移除冗余的评论提交方法，已由CommentList组件处理

// 跳转到标签页
const goToTag = (id) => {
  router.push(`/tag/${id}`)
}

// 跳转到文章详情
const goToArticle = (id) => {
  router.push(`/article/${id}`)
  location.reload() // 刷新页面获取新文章内容
}



// 处理评论添加事件
const handleCommentAdded = () => {
  // 评论计数已在后端更新，这里可以重新获取文章信息来刷新评论数
  fetchArticleDetail(route.params.id)
}

// 处理评论删除事件
const handleCommentDeleted = () => {
  // 评论计数已在后端更新，这里可以重新获取文章信息来刷新评论数
  fetchArticleDetail(route.params.id)
}

// 统一的滚动定位函数，处理锚点和标题
const scrollToElement = (target, outlineIndex = -1) => {
  nextTick(() => {
    try {
      let targetElement = null;
      
      // 确定目标元素
      if (typeof target === 'string') {
        // 如果是锚点ID
        if (target.startsWith('heading-')) {
          targetElement = document.getElementById(target);
        } else {
          // 如果是标题文本，尝试查找匹配的标题
          const articleBody = document.querySelector('.rich-text-content');
          if (articleBody) {
            const headings = articleBody.querySelectorAll('h1, h2, h3, h4, h5, h6');
            
            // 标题文本预处理
            const normalizeText = (text) => text.trim().replace(/\s+/g, ' ');
            const targetTitle = normalizeText(target);
            
            // 尝试多种匹配方式
            for (const heading of headings) {
              const headingText = normalizeText(heading.textContent);
              if (headingText === targetTitle || 
                  headingText.toLowerCase().includes(targetTitle.toLowerCase()) ||
                  targetTitle.toLowerCase().includes(headingText.toLowerCase())) {
                targetElement = heading;
                break;
              }
            }
          }
        }
      } else if (target instanceof HTMLElement) {
        // 如果直接传入了DOM元素
        targetElement = target;
      }
      
      // 如果找到目标元素，执行滚动
      if (targetElement) {
        // 获取导航栏高度（统一选择器，包含 .site-header）
        const navbar = document.querySelector('.navbar, .header, nav, .site-header');
        const headerOffset = navbar ? navbar.offsetHeight + 20 : 80;
        
        // 计算滚动位置
        const elementPosition = targetElement.getBoundingClientRect().top;
        const offsetPosition = elementPosition + window.pageYOffset - headerOffset;
        
        // 平滑滚动
        window.scrollTo({
          top: Math.max(0, offsetPosition),
          behavior: 'smooth'
        });
        
        // 使用静态标记而不是动画效果
        if (document.querySelector('.current-reading-heading')) {
          document.querySelector('.current-reading-heading').classList.remove('current-reading-heading');
        }
        
        // 添加静态类名标记当前阅读位置
        targetElement.classList.add('current-reading-heading');
        
        // 更新大纲激活项
        if (outlineIndex >= 0) {
          activeOutlineIndex.value = outlineIndex;
        }
        
        return true;
      } else {
        console.warn(`未找到目标元素: ${target}`);
        return false;
      }
    } catch (error) {
      console.error('滚动定位失败:', error);
      return false;
    }
  });
}
</script>

<style scoped>
.article-detail-container {
  position: relative;

}

/* HEO风格的文章英雄区域 */
.heo-article-hero {
  width: 100%;
  position: relative;
  min-height: 50vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden; /* 防止模糊层溢出 */
  margin-bottom: 0;
  padding-top: 70px; /* 为悬浮导航栏预留空间 */
  padding-bottom: 20px; /* 减少底部间距 */
  background-size: 200% 200%;

}

/* 底部圆角效果 */
.heo-article-hero::after {
  content: '';
  position: absolute;
  bottom: -1px; /* 稍微向下偏移确保完全覆盖 */
  left: 0;
  right: 0;
  height: 21px; /* 稍微增加高度确保完全覆盖 */
  background: #f7f9fe;
  border-radius: 20px 20px 0 0;
  z-index: 15; /* 提高z-index确保在所有内容之上 */
}

/* 左边渐变模糊层 */
.gradient-blur-left {
  position: absolute;
  left: 0;
  top: 0;
  width: 50%;
  height: 100%;
  filter: blur(40px);
  opacity: 0.6;
  z-index: 1;
}

/* 右边渐变模糊层 */
.gradient-blur-right {
  position: absolute;
  right: 0;
  top: 0;
  width: 50%;
  height: 100%;
  filter: blur(40px);
  opacity: 0.6;
  z-index: 1;
}



.hero-content {
  position: relative;
  z-index: 10;
  max-width: 1200px;
  width: 100%;
  padding: 60px 20px;
  text-align: center;
  box-sizing: border-box;
}



/* 英雄标题 */
.hero-title {
  font-size: 3.5rem;
  font-weight: 800;
  line-height: 1.3;
  margin: 0 0 30px 0;
  letter-spacing: -0.5px;
  font-family: 'dingding', 'JetBrains Mono', 'Microsoft YaHei', '微软雅黑', 'SimHei', '黑体', sans-serif;
  transition: all 0.3s ease;
  /* 不省略文字，允许换行 */
  white-space: normal;
  word-break: break-word;
  overflow-wrap: break-word;
}

/* 元信息 */
.hero-meta {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 25px;
  margin-bottom: 25px;
  font-size: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  backdrop-filter: blur(10px);
  border: 1px solid;
  transition: all 0.3s ease;
}

.meta-item:hover {

  opacity: 0.9;
}

.meta-item i {
  font-size: 14px;
  opacity: 0.8;
}

/* 标签区域 */
.hero-tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-tag-item {
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.hero-tag-item:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .hero-title {
    font-size: 3rem;
  }
}

@media (max-width: 768px) {
  .heo-article-hero {
    min-height: 40vh;
    padding-top: 60px;
  }
  
  .hero-content {
    padding: 40px 15px;
  }
  
  .hero-title {
    font-size: 2.5rem;
    margin-bottom: 20px;
    letter-spacing: -0.3px;
  }
  
  .hero-meta {
    gap: 15px;
    font-size: 14px;
  }
  
  .meta-item {
    padding: 6px 12px;
  }
}

@media (max-width: 480px) {
  .heo-article-hero {
    min-height: 35vh;
    padding-top: 50px;
  }
  
  .hero-content {
    padding: 30px 10px;
  }
  
  .hero-title {
    font-size: 2rem;
    margin-bottom: 15px;
    letter-spacing: 0;
  }
  
  .hero-meta {
    gap: 10px;
    font-size: 13px;
  }
  
  .meta-item {
    padding: 5px 10px;
    font-size: 12px;
  }
  
  .breadcrumb {
    font-size: 12px;
    margin-bottom: 20px;
  }
}

/* 移除了原有的抽屉式大纲相关样式 */

/* 主色调适配样式 */
.heo-article-hero {
  /* 使用CSS变量支持动态颜色 */
  color: var(--text-color, rgba(255, 255, 255, 0.85));
}

/* 渐变动画键帧 */
@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

/* 大纲导航样式 */
.outline-nav {
  margin: 0;
  padding: 0;
}

.outline-nav-item {
  margin-bottom: 6px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  position: relative;
  background-color: rgba(255, 255, 255, 0.6);
}

.outline-nav-item:hover {
  background-color: rgba(240, 247, 255, 0.95);
  border-color: rgba(225, 239, 254, 0.8);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.outline-nav-item:active {
  background-color: rgba(230, 242, 255, 0.95) !important;
  border-color: rgba(208, 231, 255, 0.8) !important;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2) !important;
}

.outline-nav-item:last-child {
  margin-bottom: 0;
}

.outline-nav-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.outline-nav-tag {
  border-radius: 10px;
  font-size: 10px;
  padding: 2px 6px;
  font-weight: 600;
  flex-shrink: 0;
}

.outline-nav-title {
  font-size: 13px;
  color: #495057;
  font-weight: 500;
  line-height: 1.4;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 不同层级的大纲项样式 */
.outline-nav-item.outline-level-1 {
  margin-left: 0;
  background-color: rgba(64, 158, 255, 0.05);
}

.outline-nav-item.outline-level-1 .outline-nav-title {
  font-weight: 600;
  font-size: 14px;
  color: #2c3e50;
}

.outline-nav-item.outline-level-2 {
  margin-left: 12px;
}

.outline-nav-item.outline-level-3 {
  margin-left: 24px;
}

.outline-nav-item.outline-level-3 .outline-nav-title {
  font-size: 12px;
  color: #6c757d;
}

.outline-nav-item.outline-level-4 {
  margin-left: 36px;
}

.outline-nav-item.outline-level-5 {
  margin-left: 48px;
}

.outline-nav-item.outline-level-6 {
  margin-left: 60px;
}

.outline-nav-item.outline-level-4 .outline-nav-title,
.outline-nav-item.outline-level-5 .outline-nav-title,
.outline-nav-item.outline-level-6 .outline-nav-title {
  font-size: 12px;
  color: #6c757d;
  opacity: 0.9;
}

/* 活动状态的大纲项 - 简洁蓝色加粗样式 */
.outline-nav-item.outline-active .outline-nav-title {
  font-weight: 600;
  font-size: 15px;
  color: #409EFF;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: unset;
  display: block;
}

/* 主内容区 - 左右分栏布局 */
.article-main {
  width: 1400px;
  max-width: 100%;
  display: flex;
  flex-direction: row;
  margin: 0 auto;
  padding: 20px 10px 40px; /* 减少顶部间距 */
 

  gap: 30px;
  align-items: flex-start;
}

/* 左侧侧边栏区域 - 包含大纲和推荐 */
.article-sidebar {
  width: 300px;
  flex-shrink: 0;
  position: sticky;
  top: 90px; 
  left: 0;
  /* 不限制高度，让内容自然显示 */
  z-index: 10;
  align-self: flex-start;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 移除多余的类，使用纯CSS sticky */

.outline-container {
  background: #fff ;
  border-radius: 20px;
  padding: 20px;
  border: 1px solid rgba(226, 232, 240);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02);
}

.outline-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}

.outline-title .title-icon {
  margin-right: 8px;
  color: #409EFF;
}

/* 右侧文章内容区域 */
.article-content-wrapper {
 background-color: #fff;
  flex: 1;
  padding: 10px 35px;
  border-radius: 20px ;
  border: 1px solid rgba(226, 232, 240);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02);
  min-width: 0;

}

.article-content-wrapper.full-width {
  width: 100%;
}


.article-body {
  line-height: 1.8;
  color: #333;
  font-size: 16px;
  word-break: break-word;
  padding: 10px 0;
  max-width: 100%;
  width: 100%;
}

.article-footer {
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid rgba(226, 232, 240, 0.8);
}

.article-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

/* 现代简洁风格的操作按钮 */
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 50px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;
}

.action-btn:hover:not(:disabled) {
  background: #fff;
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.action-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn.loading {
  pointer-events: none;
}

.action-btn .btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-size: 16px;
  transition: transform 0.3s ease;
}

.action-btn .btn-text {
  font-size: 14px;
  letter-spacing: 0.3px;
}

.action-btn .btn-count {
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 20px;
  min-width: 24px;
  text-align: center;
}

/* 点赞按钮样式 */
.like-btn .btn-icon {
  color: #94a3b8;
}

.like-btn:hover:not(:disabled) .btn-icon {
  color: #f43f5e;
  transform: scale(1.15);
}

.like-btn.active {
  color: #f43f5e;
  background: linear-gradient(135deg, #fff5f7 0%, #ffe4e8 100%);
  border-color: #fecdd3;
}

.like-btn.active .btn-icon {
  color: #f43f5e;
}

.like-btn.active .btn-count {
  background: rgba(244, 63, 94, 0.12);
  color: #e11d48;
}

.like-btn.active:hover:not(:disabled) {
  background: linear-gradient(135deg, #fff0f3 0%, #ffdce2 100%);
  border-color: #fda4af;
  box-shadow: 0 4px 16px rgba(244, 63, 94, 0.2);
}

.like-btn.active:hover:not(:disabled) .btn-icon {
  animation: heartBeat 0.6s ease-in-out;
}

/* 收藏按钮样式 */
.collect-btn .btn-icon {
  color: #94a3b8;
}

.collect-btn:hover:not(:disabled) .btn-icon {
  color: #f59e0b;
  transform: scale(1.15);
}

.collect-btn.active {
  color: #f59e0b;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-color: #fde68a;
}

.collect-btn.active .btn-icon {
  color: #f59e0b;
}

.collect-btn.active:hover:not(:disabled) {
  background: linear-gradient(135deg, #fef9e7 0%, #fde68a 100%);
  border-color: #fcd34d;
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.2);
}

.collect-btn.active:hover:not(:disabled) .btn-icon {
  animation: bookmarkPop 0.4s ease;
}

/* 右下角悬浮操作按钮 */
.floating-actions {
  position: fixed;
  right: 24px;
  bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 100;
}

.float-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #fff;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  color: #333;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.float-btn:hover:not(:disabled) {
  background: rgb(7, 26, 242);
  color: #fff;
  border-color: rgb(7, 26, 242);
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(7, 26, 242, 0.3);
}

.float-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 点赞按钮激活态 */
.float-btn.like-btn.active {
  background: #fff0f3;
  border-color: #f43f5e;
  color: #f43f5e;
}

.float-btn.like-btn.active:hover:not(:disabled) {
  background: #f43f5e;
  color: #fff;
  border-color: #f43f5e;
}

/* 收藏按钮激活态 */
.float-btn.collect-btn.active {
  background: #fffbeb;
  border-color: #f59e0b;
  color: #f59e0b;
}

.float-btn.collect-btn.active:hover:not(:disabled) {
  background: #f59e0b;
  color: #fff;
  border-color: #f59e0b;
}

/* 点赞数量角标 */
.float-count {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: #f43f5e;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .floating-actions {
    right: 16px;
    bottom: 16px;
  }
  
  .float-btn {
    width: 44px;
    height: 44px;
    font-size: 16px;
  }
}

.section-title {
  font-size: 22px;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
  color: #333;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.title-icon {
  margin-right: 10px;
  color: #409EFF;
}

/* 侧边栏推荐区域 */
.related-container {
  background: #fff;
  border-radius: 20px;
  padding: 20px;
  margin-top: 20px;
  border: 1px solid rgba(226, 232, 240);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02);
}

.related-header {
  margin-bottom: 16px;
}

.related-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  display: flex;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}

.related-list-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.related-item-sidebar {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: rgba(255, 255, 255, 0.6);
  border: 1px solid transparent;
}

.related-item-sidebar:hover {
  background-color: rgba(240, 247, 255, 0.95);
  border-color: rgba(225, 239, 254, 0.8);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateX(3px);
}

.related-cover-sidebar {
  width: 80px;
  height: 60px;
  border-radius: 8px;
  flex-shrink: 0;
  object-fit: cover;
}

.related-info-sidebar {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.related-title-sidebar {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 500;
  color: #495057;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.2s;
}

.related-item-sidebar:hover .related-title-sidebar {
  color: #409EFF;
}

.related-meta-sidebar {
  font-size: 12px;
  color: #6c757d;
  display: flex;
  align-items: center;
  gap: 4px;
}

.related-meta-sidebar i {
  font-size: 11px;
}

.comments-section {
  margin-bottom: 30px;
}

/* 富文本编辑器内容样式 */
:deep(.rich-text-content) {

  font-size: 17px;
  line-height: 1.50;
  color: #1f2937;
  letter-spacing: 0.15px;
 
  text-align: left;
}
:deep(.rich-text-content li),
:deep(.rich-text-content p){
font-family: "SimSun", "宋体", serif !important;;
} 
:deep(.rich-text-content img) {
  max-width: 100%;
  height: auto !important;
  box-sizing: content-box;
  background-color: #fff;
  border-radius: 6px;
  margin: 10px 0;
}

:deep(.rich-text-content pre) {
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: #f6f8fa;
  border-radius: 6px;
  margin: 16px 0;
  border: 1px solid #ccc;
  white-space: pre-wrap;
}

:deep(.rich-text-content code) {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(175, 184, 193, 0.2);
  border-radius: 6px;
  font-family: 'JetBrains Mono', 'JetBrainsMono-Medium', Consolas, Monaco, 'Andale Mono', monospace;
}

:deep(.rich-text-content pre code) {
  padding: 0;
  background-color: transparent;
}

/* 代码块复制按钮样式 */
:deep(.code-copy-btn) {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 6px 12px;
  background-color: rgba(255, 255, 255, 0.9);
  border: 1px solid #d0d7de;
  border-radius: 6px;
  color: #57606a;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 10;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

:deep(.code-copy-btn:hover) {
  background-color: #f6f8fa;
  border-color: #409EFF;
  color: #409EFF;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2);
}

:deep(.code-copy-btn:active) {
  transform: translateY(0);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

:deep(.code-copy-btn.copied) {
  background-color: #67c23a;
  border-color: #67c23a;
  color: white;
}

:deep(.code-copy-btn i) {
  font-size: 14px;
}

:deep(.rich-text-content blockquote) {
  padding: 0.5em 1em;
  color: #57606a;

  font-style: italic;
  margin: 16px 0;
  background-color: #f8f9fa;
}
:deep(.rich-text-content blockquote p){
  font-family:'Trebuchet MS', 'Lucida Sans Unicode', 'Lucida Grande', 'Lucida Sans', Arial, sans-serif!important;
}

:deep(.rich-text-content table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  overflow-x: auto;
  display: block;
}

:deep(.rich-text-content table th),
:deep(.rich-text-content table td) {
  padding: 8px 13px;
  border: 1px solid #d0d7de;
}

:deep(.rich-text-content table th) {
  background-color: #f1f3f5;
  font-weight: 600;
}

:deep(.rich-text-content table tr) {
  background-color: #fff;
  border-top: 1px solid #d0d7de;
}

:deep(.rich-text-content table tr:nth-child(2n)) {
  background-color: #f6f8fa;
}

:deep(.rich-text-content a) {
  color: #409EFF;
  text-decoration: none;
}

:deep(.rich-text-content a:hover) {
  text-decoration: underline;
}

:deep(.rich-text-content ul),
:deep(.rich-text-content ol) {
  padding-left: 2em;
  margin: 8px 0;
}

:deep(.rich-text-content li) {
  margin-bottom: 3px;
}

:deep(.rich-text-content p) {
  margin: 16px 0;
  text-align: left;
}

:deep(.rich-text-content hr) {
  height: 1px;
  background-color: #eee;
  border: none;
  margin: 24px 0;
}

/* 当前阅读位置标记 - 简洁蓝色加粗样式 */
:deep(.current-reading-heading) {
  color: #409EFF !important;
  font-weight: 700 !important;
}

/* 标题排版（全新样式：提高可读性与层级区分） */
@font-face {
  font-family: 'PingFangSC-Med';
  src: url('../../../assets/font/PingFangSC-Medium.woff2') format('woff2');
  font-weight: 600;
  font-style: normal;
  font-display: swap;
}

::deep(.rich-text-content h1),
::deep(.rich-text-content h2),
::deep(.rich-text-content h3),
::deep(.rich-text-content h4),
::deep(.rich-text-content h5),
::deep(.rich-text-content h6) {
  font-family: 'Microsoft YaHei', 'PingFang SC', system-ui, -apple-system, 'Segoe UI', Roboto, 'Noto Sans CJK SC', sans-serif !important;
  color: #1f2937;
  letter-spacing: 0.2px;
  background: none !important;
  border: 0 !important;
  padding-left: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* H1 */
::deep(.rich-text-content h1) {
  font-family: 'Microsoft YaHei', 'PingFang SC', system-ui, -apple-system, 'Segoe UI', Roboto, 'Noto Sans CJK SC', sans-serif !important;
  font-size: 2.2em;
  font-weight: 800;
  line-height: 1.35;
  margin-top: 2.8em;
  margin-bottom: 1em;
}
::deep(.rich-text-content h1)::before {
  content: '';
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #111827;
}

/* H2 */
::deep(.rich-text-content h2) {
  font-family: 'Microsoft YaHei', 'PingFang SC', system-ui, -apple-system, 'Segoe UI', Roboto, 'Noto Sans CJK SC', sans-serif !important;
  font-size: 1.7em;
  font-weight: 700;
  line-height: 1.4;
  margin-top: 2.5em;
  margin-bottom: 0.8em;
}
::deep(.rich-text-content h2)::before {
  content: '';
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #374151;
  opacity: 0.9;
}

/* H3 */
::deep(.rich-text-content h3) {
  font-family: 'Microsoft YaHei', 'PingFang SC', system-ui, -apple-system, 'Segoe UI', Roboto, 'Noto Sans CJK SC', sans-serif !important;
  font-size: 1.35em;
  font-weight: 700;
  line-height: 1.45;
  margin-top: 2em;
  margin-bottom: 0.7em;
}
::deep(.rich-text-content h3)::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4b5563;
  opacity: 0.85;
}

/* H4 */
::deep(.rich-text-content h4) {
  font-family: 'Microsoft YaHei', 'PingFang SC', system-ui, -apple-system, 'Segoe UI', Roboto, 'Noto Sans CJK SC', sans-serif !important;
  font-size: 1.15em;
  font-weight: 600;
  line-height: 1.5;
  margin-top: 1.4em;
  margin-bottom: 0.4em;
}
::deep(.rich-text-content h4)::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 2px solid #9ca3af;
  background: transparent;
}

/* H5 */
::deep(.rich-text-content h5) {
  font-size: 1.05em;
  font-weight: 600;
  line-height: 1.55;
  margin-top: 1.2em;
  margin-bottom: 0.35em;
  color: #374151;
}
::deep(.rich-text-content h5)::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  border: 2px solid #c7cdd6;
  background: transparent;
}

/* H6 */
::deep(.rich-text-content h6) {
  font-size: 0.95em;
  font-weight: 600;
  line-height: 1.6;
  margin-top: 1.1em;
  margin-bottom: 0.3em;
  color: #4b5563;
}
::deep(.rich-text-content h6)::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 2px;
  background: #e5e7eb;
  border: 1px solid #d1d5db;
}

@media (max-width: 768px) {
  /* HEO风格移动端适配 */
  .heo-article-hero {
    min-height: 50vh;
  }
  
  .hero-content {
    padding: 40px 15px;
  }
  
  .hero-title {
    font-size: 2.2rem;
    margin-bottom: 20px;
  }
  
  .hero-meta {
    gap: 15px;
    font-size: 14px;
  }
  
  .meta-item {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .breadcrumb {
    font-size: 12px;
    margin-bottom: 20px;
  }
  
  /* 移动端改为垂直布局 */
  .article-main {
    display: flex;
    flex-direction: column;
    padding: 30px 15px;
    margin-top: -20px;
    border-radius: 15px 15px 0 0;
    gap: 20px;
  }
  
  .article-outline-sidebar {
    width: 100%;
    position: static;
    max-height: none;
    order: 2; /* 移动端大纲放在内容后面 */
  }
  
  .article-content-wrapper {
    order: 1; /* 移动端内容放在前面 */
  }
  
  .outline-container {
    padding: 15px;
  }
  
  .outline-title {
    font-size: 14px;
  }
  
  .related-list {
    grid-template-columns: 1fr;
  }
  
  /* 移动端大纲样式优化 */
  .outline-nav-item.outline-level-2 {
    margin-left: 8px;
  }
  
  .outline-nav-item.outline-level-3 {
    margin-left: 16px;
  }
  
  .outline-nav-item.outline-level-4 {
    margin-left: 24px;
  }
  
  .outline-nav-item.outline-level-5,
  .outline-nav-item.outline-level-6 {
    margin-left: 32px;
  }
  
  .outline-nav-title {
    font-size: 12px;
  }
}

/* 密码验证弹窗样式 */
.password-dialog-content {
  text-align: center;
  padding: 20px 0;
}

.password-icon {
  margin-bottom: 20px;
}

.password-hint {
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
}

.password-error {
  color: #F56C6C;
  font-size: 12px;
  margin-top: 10px;
}

.password-dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
}

:deep(.el-dialog__header) {
  text-align: center;
}

:deep(.el-dialog__title) {
  font-weight: 600;
  color: #303133;
}
</style> 