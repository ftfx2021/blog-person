<template>
  <div class="about-container">


    <div class="about-content" v-loading="loading">
      <!-- 个人资料卡片 -->
      <div class="profile-section">
        <!-- 头像和关键词容器 -->
        <div class="avatar-keywords-container">
          <!-- 左侧关键词 -->
          <div class="keywords-side keywords-left" v-if="leftKeywords.length > 0">
            <div 
              v-for="(keyword, index) in leftKeywords" 
              :key="'left-' + index"
              class="keyword-item"
              :style="getKeywordOffset('left', index, leftKeywords.length)"
              @click="handleKeywordClick"
            >
              <span class="keyword-text">{{ keyword }}</span>
            </div>
          </div>
          
          <div class="avatar-wrapper">
            <el-avatar 
              :size="200" 
              :src="adminAvatar" 
              class="avatar"
            >
              {{ aboutInfo.about_name ? aboutInfo.about_name.charAt(0) : 'A' }}
            </el-avatar>
          </div>
          
          <!-- 右侧关键词 -->
          <div class="keywords-side keywords-right" v-if="rightKeywords.length > 0">
            <div 
              v-for="(keyword, index) in rightKeywords" 
              :key="'right-' + index"
              class="keyword-item"
              :style="getKeywordOffset('right', index, rightKeywords.length)"
              @click="handleKeywordClick"
            >
              <span class="keyword-text">{{ keyword }}</span>
            </div>
          </div>
        </div>
        
        <!-- 底部关键词（超出8个的部分） -->
        <div class="keywords-bottom-wrapper" v-if="bottomKeywords.length > 0">
          <div class="keywords-bottom">
            <div 
              v-for="(keyword, index) in bottomKeywords" 
              :key="'bottom-' + index"
              class="keyword-item"
              @click="handleKeywordClick"
            >
              <span class="keyword-text">{{ keyword }}</span>
            </div>
          </div>
        </div>
        
        <h2 class="about-title">关于本站</h2>

        
        <!-- 社交链接 -->
        <div class="social-links">
          <a v-if="aboutInfo.social_github" :href="aboutInfo.social_github" target="_blank" class="social-item">
            <i class="fab fa-github"></i>
            <span>GitHub</span>
          </a>
          <a v-if="aboutInfo.social_weibo" :href="aboutInfo.social_weibo" target="_blank" class="social-item">
            <i class="fab fa-weibo"></i>
            <span>微博</span>
          </a>
          <a v-if="aboutInfo.social_zhihu" :href="aboutInfo.social_zhihu" target="_blank" class="social-item">
            <i class="fab fa-zhihu"></i>
            <span>知乎</span>
          </a>
          <a v-if="aboutInfo.social_email" :href="`mailto:${aboutInfo.social_email}`" class="social-item">
            <i class="fas fa-envelope"></i>
            <span>Email</span>
          </a>
        </div>
      </div>
      
      <!-- 个人简介和发展历程并列 -->
      <div class="content-row">
        <!-- 详细介绍 -->
        <div class="detail-section">
          <div class="section-header">
            <h2 class="section-title">个人简介</h2>
          </div>
          <div class="markdown-content" v-html="htmlContent"></div>
        </div>
        
        <!-- 发展历程 -->
        <SiteTimeline />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAllBlogConfigs } from '@/api/BlogConfigApi'
import { getUsersByRole } from '@/api/user'
import { marked } from 'marked'
import confetti from 'canvas-confetti'
import SiteTimeline from '@/components/frontend/SiteTimeline.vue'

const loading = ref(false)
const aboutInfo = ref({})
const adminAvatar = ref('')
const adminName = ref('')
const authorKeywords = ref([])

// 获取关于我的信息
const fetchAboutInfo = async () => {
  loading.value = true
  try {
    getAllBlogConfigs({
      showDefaultMsg: false,
      onSuccess: (data) => {
        aboutInfo.value = data || {}
        // 处理关键词数据
        if (data.author_keyword) {
          authorKeywords.value = data.author_keyword.split(',').map(k => k.trim()).filter(k => k)
        }
      },
      onError: (error) => {
        console.error('获取关于我信息失败:', error)
      }
    })
  } catch (error) {
    console.error('获取关于我信息失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取管理员信息
const fetchAdminInfo = async () => {
  try {
    getUsersByRole('ADMIN', {
      showDefaultMsg: false,
      onSuccess: (data) => {
        if (data && data.length > 0) {
          const admin = data[0] // 获取第一个管理员
          adminAvatar.value = admin.avatar || ''
          adminName.value = admin.name || admin.username || ''
        }
      },
      onError: (error) => {
        console.error('获取管理员信息失败:', error)
      }
    })
  } catch (error) {
    console.error('获取管理员信息失败:', error)
  }
}




// 计算关键词分布 - 左右各最多4个，剩余显示在底部
const leftKeywords = computed(() => {
  // 左侧最多4个
  return authorKeywords.value.slice(0, Math.min(4, Math.ceil(Math.min(authorKeywords.value.length, 8) / 2)))
})

const rightKeywords = computed(() => {
  // 右侧最多4个
  const leftCount = leftKeywords.value.length
  const rightCount = Math.min(4, Math.min(authorKeywords.value.length, 8) - leftCount)
  return authorKeywords.value.slice(leftCount, leftCount + rightCount)
})

const bottomKeywords = computed(() => {
  // 超出8个的显示在底部
  return authorKeywords.value.slice(8)
})

// 计算关键词偏移量 - 沿圆弧排列，保持与头像距离固定
const getKeywordOffset = (side, index, total) => {
  // 头像半径约100px，关键词到头像边缘的固定距离
  const avatarRadius = 100
  const fixedGap = 40 // 关键词与头像边缘的固定间距
  const totalRadius = avatarRadius + fixedGap // 关键词到头像中心的距离
  
  // 计算垂直方向上的偏移（关键词在列表中的位置）
  // 垂直间距约为 12px（gap），每个关键词高度约36px
  const itemHeight = 36 + 12 // 高度 + 间距
  const centerIndex = (total - 1) / 2
  const verticalOffset = (index - centerIndex) * itemHeight
  
  // 使用勾股定理计算水平偏移
  // 圆的方程：x² + y² = r²
  // 所以 x = √(r² - y²)
  const ySquared = verticalOffset * verticalOffset
  const rSquared = totalRadius * totalRadius
  
  // 如果垂直偏移超出圆的范围，使用最小水平距离
  let horizontalOffset = 0
  if (ySquared < rSquared) {
    horizontalOffset = Math.sqrt(rSquared - ySquared)
  } else {
    horizontalOffset = fixedGap // 最小距离
  }
  
  // 计算相对于默认位置的偏移量
  // 默认位置是最大水平距离（中心位置），所以需要计算差值
  const maxHorizontal = totalRadius
  const offsetFromDefault = maxHorizontal - horizontalOffset
  
  // 左侧向右偏移（正值，靠近头像），右侧向左偏移（负值，靠近头像）
  const translateX = side === 'left' ? offsetFromDefault : -offsetFromDefault
  
  return {
    transform: `translateX(${translateX}px)`
  }
}

// 关键词点击事件
const handleKeywordClick = (event) => {
  // 获取点击位置
  const rect = event.target.getBoundingClientRect()
  const x = (rect.left + rect.width / 2) / window.innerWidth
  const y = (rect.top + rect.height / 2) / window.innerHeight

  // 触发烟花效果
  confetti({
    particleCount: 50,
    spread: 50,
    origin: { x, y },
    colors: ['#071af2', '#ffffff', '#f0f0f0', '#3498db'],
    zIndex: 9999
  })
}

// 将markdown转换为html
const htmlContent = computed(() => {
  if (!aboutInfo.value.about_content) return '<p>暂无内容</p>'
  return marked(aboutInfo.value.about_content)
})

// 生命周期钩子
onMounted(() => {
  fetchAboutInfo()
  fetchAdminInfo()
  document.title = '关于博主 - 个人博客'
})
</script>

<style scoped>
.about-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  font-family: 'dongqing', 'PingFangSC-Medium', 'Microsoft YaHei', sans-serif;
}








.about-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

/* 个人资料卡片样式 */
.profile-section {
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  position: relative;
  overflow: visible;
}

/* 头像和关键词容器 */
.avatar-keywords-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  margin-bottom: 20px;
}

/* 左右两侧关键词容器 */
.keywords-side {
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  gap: 12px;
  min-width: 120px;
}

.keywords-left {
  align-items: center;
}

.keywords-right {
  align-items: center;
}

/* 底部关键词容器外层 - 固定高度防止挤压 */
.keywords-bottom-wrapper {
  position: relative;
  height: 50px;
  margin-bottom: 20px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

/* 底部关键词容器 - 参考分类样式 */
.keywords-bottom {
  position: absolute;
  left: 0;
  right: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  padding: 8px 12px;
  border: 1.5px solid transparent;
  border-radius: 10px;
  background: transparent;
  max-height: 50px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 10;
}

.keywords-bottom:hover {
  max-height: 500px;
  padding: 6px 12px 12px 12px;
  border-color: rgb(7, 26, 242); 
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(3px);
  z-index: 100;
}

/* 底部关键词项  */
.keywords-bottom .keyword-item {
  padding: 4px 8px;
  font-weight: 700;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  /* font-size: 18px; */
  box-shadow: none;
}

.keywords-bottom .keyword-item:hover {
  background: rgb(7, 26, 242);
  color: #fff;

}

.avatar-wrapper {
  position: relative;
  z-index: 2;
}




.about-title {
  font-size: 36px;
  margin-bottom: 20px;
  font-weight: 600;
  color: #333;
}

.job {
  font-size: 18px;
  margin-bottom: 16px;
  color: #666;
  font-weight: 400;
}

.intro {
  max-width: 600px;
  margin: 0 auto 24px;
  line-height: 1.8;
  font-size: 16px;
  color: #555;
  padding: 0 20px;
}

/* 关键词标签样式 - 左右两侧 */
.keyword-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 8px;
  background: #fff;
  border-radius: 10px;
  color: #333;
  font-size: 15px;
  font-weight: 600;
  cursor: default;
  white-space: nowrap;
  border: 2px solid #f0f0f0;
  transition: all 0.3s ease;
 
  
}
.keyword-item:hover {
  background: rgb(7, 26, 242);
  color: #fff;
  border-radius: 10px;
  cursor: pointer;
}



.keyword-text {
  letter-spacing: 0.5px;
}

.social-links {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.social-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background-color: #fff;
  border-radius: 8px;
  color: #000;
  text-decoration: none;
  transition: all 0.3s ease;
    border: 2px solid #f0f0f0;
    font-size: 15px;
}

.social-item:hover {
  background-color: #e8f4fc;
  color: rgb(7, 26, 242);
  border-color: rgb(7, 26, 242);
}

/* 并列布局容器 */
.content-row {
  display: flex;
  gap: 30px;
}

/* 详细介绍样式 */
.detail-section {
  flex: 1;
  background-color: #fff;
  padding: 30px;
  border-radius: 8px;
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

:deep(.markdown-content) {
  line-height: 1.8;
  color: #333;
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3) {
  margin-top: 28px;
  margin-bottom: 20px;
  font-weight: 600;
  line-height: 1.4;
  color: #222;
}

:deep(.markdown-content h1) {
  font-size: 2em;
  border-bottom: 2px solid #eee;
  padding-bottom: 0.5em;
}

:deep(.markdown-content h2) {
  font-size: 1.5em;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.5em;
}

:deep(.markdown-content h3) {
  font-size: 1.25em;
}

:deep(.markdown-content p) {
  margin: 16px 0;
  line-height: 1.8;
}

:deep(.markdown-content a) {
  color: #3498db;
  text-decoration: none;
  border-bottom: 1px solid #3498db;
  transition: all 0.3s ease;
}

:deep(.markdown-content a:hover) {
  color: #2980b9;
  border-color: #2980b9;
}

:deep(.markdown-content img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 20px 0;
  border: 1px solid #eaeaea;
}

:deep(.markdown-content blockquote) {
  padding: 16px 24px;
  margin: 24px 0;
  background: #f8f9fa;
  border-left: 4px solid #3498db;
  border-radius: 4px;
}

:deep(.markdown-content code) {
  background: #f8f9fa;
  padding: 3px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  color: #333;
  font-family: 'JetBrains Mono', 'JetBrainsMono-Medium', 'Consolas', 'Monaco', monospace;
}

:deep(.markdown-content pre) {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 24px 0;
  border: 1px solid #eaeaea;
}

:deep(.markdown-content pre code) {
  background: none;
  padding: 0;
}

@media (max-width: 992px) {
  .content-row {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .about-content {
    padding: 0;
  }
  
  .profile-section {
    padding: 30px 15px;
  }
  
  /* 移动端头像和关键词容器 */
  .avatar-keywords-container {
    gap: 20px;
    margin-bottom: 15px;
  }
  
  .keywords-side {
    min-width: 80px;
    gap: 8px;
  }
  
  .avatar {
    width: 120px !important;
    height: 120px !important;
  }
  
  .keyword-item {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .keywords-bottom {
    gap: 8px;
    margin-bottom: 20px;
  }
  
  .detail-section {
    padding: 20px;
  }
  
  .name {
    font-size: 24px;
  }
  
  .job {
    font-size: 16px;
  }
  
  .social-links {
    gap: 10px;
  }
  
  .social-item {
    padding: 6px 12px;
    font-size: 14px;
  }
}

/* 超小屏幕适配 */
@media (max-width: 480px) {
  .avatar-keywords-container {
    flex-direction: column;
    gap: 15px;
  }
  
  .keywords-side {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    min-width: auto;
  }
  
  .keywords-left,
  .keywords-right {
    align-items: center;
  }
  
  .avatar {
    width: 100px !important;
    height: 100px !important;
  }
  
  .keyword-item {
    padding: 5px 10px;
    font-size: 12px;
  }
}
</style> 