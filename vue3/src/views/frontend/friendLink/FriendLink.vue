<template>
  <div class="friend-link-container">
    <!-- 标题区域 -->
    <div class="title-container">
      <div class="title-decorations">
        <span class="decoration-left">&#123; &#125;</span>
        <span class="decoration-right">&#60;/&#62;</span>
      </div>
      <h1 class="page-title">友情链接</h1>
      <div class="title-wave"></div>
    </div>
    
    <div class="link-list" v-loading="loading">
      <el-empty v-if="friendLinks.length === 0" description="暂无友情链接" />
      <div class="link-grid" v-else>
        <a v-for="link in friendLinks" :key="link.id" 
          :href="link.url" 
          target="_blank" 
          class="link-item"
        >
          <div class="link-content">
            <div class="link-logo">
              <el-image 
                v-if="link.logo" 
                :src="link.logo" 
                fit="cover"
                :alt="link.name"
              />
              <div v-else class="link-avatar">
                {{ link.name.charAt(0) }}
              </div>
            </div>
            <div class="link-info">
              <div class="link-name">{{ link.name }}</div>
              <div class="link-desc">{{ link.description || '暂无描述' }}</div>
            </div>
          </div>
        </a>
      </div>
    </div>
    
    <div class="apply-section">
      <div class="section-header">
        <h2 class="section-title">申请友链</h2>
      </div>
      <p class="apply-desc">如果您想与本站交换友情链接，请按以下格式提供信息并发送邮件：</p>
      <div class="apply-info">
        <p><strong>网站名称：</strong>您的网站名称</p>
        <p><strong>网站链接：</strong>您的网站链接(以http://或https://开头)</p>
        <p><strong>网站Logo：</strong>您的网站Logo(可选)</p>
        <p><strong>网站描述：</strong>一句话描述您的网站</p>
      </div>
      <div class="my-info">
        <div class="section-header">
          <h3>我的友链信息</h3>
        </div>
        <div class="my-info-content">
          <p><strong>网站名称：</strong>{{ blogName }}</p>
          <p><strong>网站链接：</strong>{{ currentUrl }}</p>
          <p><strong>网站描述：</strong>{{ blogDesc }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getVisibleFriendLinks } from '@/api/FriendLinkApi'
import { getAllBlogConfigs } from '@/api/BlogConfigApi'

const loading = ref(false)
const friendLinks = ref([])
const blogConfig = ref({})

// 获取友情链接
const fetchFriendLinks = () => {
  loading.value = true
  getVisibleFriendLinks({
    showDefaultMsg: false,
    onSuccess: (data) => {
      friendLinks.value = data || []
      loading.value = false
    },
    onError: (error) => {
      console.error('获取友情链接失败:', error)
      loading.value = false
    }
  })
}

// 获取博客配置
const fetchBlogConfig = () => {
  getAllBlogConfigs({
    showDefaultMsg: false,
    onSuccess: (data) => {
      blogConfig.value = data || {}
    },
    onError: (error) => {
      console.error('获取博客配置失败:', error)
    }
  })
}



// 博客名称
const blogName = computed(() => {
  return blogConfig.value.blog_name || '个人博客'
})

// 博客描述
const blogDesc = computed(() => {
  return blogConfig.value.blog_description || '分享技术，记录生活'
})

// 当前URL
const currentUrl = computed(() => {
  return window.location.origin
})

// 生命周期钩子
onMounted(() => {
  fetchFriendLinks()
  fetchBlogConfig()
  document.title = '友情链接 - 个人博客'
})
</script>

<style scoped>
.friend-link-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;

}

/* 标题区域样式 */
.title-container {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
  padding: 30px 0;
}

.page-title {
  font-size: 36px;
  font-weight: 500;
  color: #3498db;
  margin: 0;
  letter-spacing: 2px;
  font-family: 'JetBrains Mono', 'JetBrainsMono-Medium', 'Consolas', 'Monaco', monospace;
  position: relative;
  display: inline-block;
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

/* 链接列表样式 */
.link-list {
  margin-bottom: 40px;
}

.link-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.link-item {
  text-decoration: none;
  color: inherit;
  border-radius: 8px;
  background-color: #fff;
  border: 2px solid #ecf0f1;
  transition: all 0.3s ease;
  overflow: hidden;
  position: relative;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.link-item::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(to right, #3498db, #2ecc71);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}

.link-item:hover {
  border-color: #3498db;
  background-color: #f9f9f9;
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.link-item:hover::before {
  transform: scaleX(1);
}

.link-content {
  display: flex;
  padding: 20px;
  align-items: center;
}

.link-logo {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  margin-right: 15px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ecf0f1;
  border: 2px solid #e0e6e8;
  transition: all 0.3s ease;
}

.link-item:hover .link-logo {
  border-color: #3498db;
  box-shadow: 0 0 10px rgba(52, 152, 219, 0.3);
}

.link-logo .el-image {
  width: 100%;
  height: 100%;
}

.link-avatar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  color: #fff;
  background-color: #3498db;
}

.link-info {
  flex: 1;
  overflow: hidden;
}

.link-name {
  font-size: 18px;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.3s ease;
}

.link-item:hover .link-name {
  color: #3498db;
}

.link-desc {
  font-size: 14px;
  color: #7f8c8d;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

/* 申请友链区域样式 */
.apply-section {
  background-color: #fff;
  padding: 30px;
  border-radius: 8px;
  margin-top: 40px;
  border: 1px solid #eaeaea;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.section-title {
  margin: 0;
  font-size: 24px;
  color: #333;
  font-weight: 600;
  position: relative;
  padding-left: 15px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 5px;
  height: 70%;
  width: 4px;
  background: linear-gradient(to bottom, #3498db, #2ecc71);
  border-radius: 2px;
}

.apply-desc {
  color: #555;
  margin-bottom: 20px;
  font-size: 16px;
  line-height: 1.6;
}

.apply-info {
  background-color: #f8f9fa;
  padding: 20px 25px;
  border-radius: 8px;
  margin: 20px 0;
}

.apply-info p {
  margin: 10px 0;
  color: #34495e;
  line-height: 1.8;
}

.apply-info strong {
  color: #2c3e50;
  margin-right: 5px;
}

.my-info {
  margin-top: 30px;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
}

.my-info h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
  font-weight: 500;
}

.my-info-content {
  padding: 15px;
  background-color: #fff;
  border-radius: 6px;
  margin-top: 15px;
  border: 1px solid #eaeaea;
}

.my-info-content p {
  margin: 10px 0;
  color: #34495e;
}

.my-info-content strong {
  color: #2c3e50;
}

@media (max-width: 768px) {
  .link-grid {
    grid-template-columns: 1fr;
  }
  
  .link-content {
    padding: 15px;
  }
  
  .link-logo {
    width: 50px;
    height: 50px;
  }
  
  .apply-section {
    padding: 20px;
  }
  
  .page-title {
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