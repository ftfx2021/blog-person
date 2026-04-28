<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card">
      <template #header>
        <div class="welcome-header">
          <el-avatar :size="64" :src="userInfo?.avatar">
            {{ userInfo?.name?.charAt(0) }}
          </el-avatar>
          <div class="welcome-info">
            <h2>欢迎回来, {{ userInfo?.name || userInfo?.username }}</h2>
            <p>{{ currentTime }}</p>
          </div>
        </div>
      </template>
      <div class="role-info">
        <el-tag>{{ roleLabel }}</el-tag>
      </div>
    </el-card>

    <!-- 数据统计卡片 -->
    <div class="stat-cards">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-icon article-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.articleCount }}</div>
              <div class="stat-label">文章总数</div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-icon comment-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.commentCount }}</div>
              <div class="stat-label">评论总数</div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-icon view-icon">
              <el-icon><View /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.viewCount }}</div>
              <div class="stat-label">总访问量</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 数据统计图表 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="chart-card" v-loading="loading.articleChart">
          <template #header>
            <div class="card-header">
              <h3>文章发布趋势</h3>
            </div>
          </template>
          <div class="chart-container">
            <div ref="articleChartRef" class="chart"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card" v-loading="loading.viewChart">
          <template #header>
            <div class="card-header">
              <h3>访问量趋势</h3>
            </div>
          </template>
          <div class="chart-container">
            <div ref="viewChartRef" class="chart"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近文章统计 -->
    <el-card class="recent-articles-card" v-loading="loading.recentArticles">
      <template #header>
        <div class="card-header">
          <h3>最近发布的文章</h3>
          <el-button type="primary" text @click="goToArticleManagement">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentArticles" style="width: 100%" v-if="recentArticles.length > 0">
        <el-table-column prop="title" label="标题" show-overflow-tooltip>
          <template #default="scope">
            <router-link :to="`/article/${scope.row.id}`" target="_blank" class="article-link">
              {{ scope.row.title }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读量" width="100" />
        <el-table-column prop="commentCount" label="评论数" width="100" />
        <el-table-column prop="createTime" label="发布时间" width="180" />
      </el-table>
      <el-empty v-else description="暂无文章数据"></el-empty>
    </el-card>

    <!-- 最近评论统计 -->
    <el-card class="recent-comments-card" v-loading="loading.recentComments">
      <template #header>
        <div class="card-header">
          <h3>最近评论</h3>
          <el-button type="primary" text @click="goToCommentManagement">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentComments" style="width: 100%" v-if="recentComments.length > 0">
        <el-table-column label="评论用户" width="150">
          <template #default="scope">
            <div class="user-info">
              <el-avatar :size="24" :src="scope.row.userAvatar">
                {{ scope.row.userName?.charAt(0) || scope.row.username?.charAt(0) }}
              </el-avatar>
              <span>{{ scope.row.userName || scope.row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" show-overflow-tooltip />
        <el-table-column prop="articleTitle" label="文章标题" width="200" show-overflow-tooltip>
          <template #default="scope">
            <router-link :to="`/article/${scope.row.articleId}`" target="_blank" class="article-link">
              {{ scope.row.articleTitle }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" width="180" />
      </el-table>
      <el-empty v-else description="暂无评论数据"></el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Document, ChatDotRound, User, View } from '@element-plus/icons-vue'
import { 
  getDashboardStats, 
  getArticleTrend, 
  getViewTrend, 
  getRecentArticles, 
  getRecentComments 
} from '@/api/DashboardApi'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)


// 图表引用
const articleChartRef = ref(null)
const viewChartRef = ref(null)
let articleChart = null
let viewChart = null

// 角色标签
const roleLabel = computed(() => {
  const roleMap = {
    'ADMIN': '系统管理员',
    'USER': '普通用户'
  }
  return roleMap[userInfo.value?.roleCode] || '未知角色'
})



// 当前时间
const currentTime = ref('')
let timeInterval = null // 保存定时器引用

const updateTime = () => {
  const now = new Date()
  const options = { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric', 
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit'
  }
  currentTime.value = now.toLocaleDateString('zh-CN', options)
}

// 统计数据
const stats = reactive({
  articleCount: 0,
  commentCount: 0,
  userCount: 0,
  viewCount: 0
})

// 加载状态
const loading = reactive({
  stats: false,
  recentArticles: false,
  recentComments: false,
  articleChart: false,
  viewChart: false
})

// 最近文章和评论
const recentArticles = ref([])
const recentComments = ref([])

// 图表数据
const chartData = reactive({
  articleTrend: [],
  viewTrend: []
})

// 获取统计数据
const fetchStats = async () => {
  loading.stats = true
  try {
    getDashboardStats({
      showDefaultMsg: false,
      onSuccess: (data) => {
        stats.articleCount = data.articleCount || 0
        stats.commentCount = data.commentCount || 0
        stats.userCount = data.userCount || 0
        stats.viewCount = data.viewCount || 0
      },
      onError: (error) => {
        console.error('获取统计数据失败:', error)
      }
    })
  } finally {
    loading.stats = false
  }
}

// 获取最近文章
const fetchRecentArticles = async () => {
  loading.recentArticles = true
  try {
    getRecentArticles({ size: 5 }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        recentArticles.value = data || []
      },
      onError: (error) => {
        console.error('获取最近文章失败:', error)
      }
    })
  } finally {
    loading.recentArticles = false
  }
}

// 获取最近评论
const fetchRecentComments = async () => {
  loading.recentComments = true
  try {
    getRecentComments({ size: 5 }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        recentComments.value = data || []
      },
      onError: (error) => {
        console.error('获取最近评论失败:', error)
      }
    })
  } finally {
    loading.recentComments = false
  }
}

// 获取文章发布趋势数据
const fetchArticleTrend = async () => {
  loading.articleChart = true
  try {
    getArticleTrend({ days: 7 }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        chartData.articleTrend = data || []
        initArticleChart()
      },
      onError: (error) => {
        console.error('获取文章发布趋势失败:', error)
      }
    })
  } finally {
    loading.articleChart = false
  }
}

// 获取访问量趋势数据
const fetchViewTrend = async () => {
  loading.viewChart = true
  try {
    getViewTrend({ days: 7 }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        chartData.viewTrend = data || []
        initViewChart()
      },
      onError: (error) => {
        console.error('获取访问量趋势失败:', error)
      }
    })
  } finally {
    loading.viewChart = false
  }
}

// 初始化文章发布趋势图表
const initArticleChart = () => {
  if (!articleChartRef.value) return
  
  // 销毁旧实例
  if (articleChart) {
    articleChart.dispose()
  }
  
  // 创建图表实例
  articleChart = echarts.init(articleChartRef.value)
  
  // 日期和数量数组
  const dates = chartData.articleTrend.map(item => item.date)
  const counts = chartData.articleTrend.map(item => item.count)
  
  // 配置图表选项
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '文章数量',
        type: 'bar',
        data: counts,
        itemStyle: {
          color: '#409EFF'
        }
      }
    ]
  }
  
  // 设置图表选项
  articleChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    articleChart && articleChart.resize()
  })
}

// 初始化访问量趋势图表
const initViewChart = () => {
  if (!viewChartRef.value) return
  
  // 销毁旧实例
  if (viewChart) {
    viewChart.dispose()
  }
  
  // 创建图表实例
  viewChart = echarts.init(viewChartRef.value)
  
  // 日期和数量数组
  const dates = chartData.viewTrend.map(item => item.date)
  const counts = chartData.viewTrend.map(item => item.count)
  
  // 配置图表选项
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: {
        rotate: 45
      }
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '访问量',
        type: 'line',
        data: counts,
        areaStyle: {
          opacity: 0.3,
          color: '#F56C6C'
        },
        lineStyle: {
          width: 2,
          color: '#F56C6C'
        },
        itemStyle: {
          color: '#F56C6C'
        },
        smooth: true
      }
    ]
  }
  
  // 设置图表选项
  viewChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    viewChart && viewChart.resize()
  })
}

// 跳转到文章管理
const goToArticleManagement = () => {
  router.push('/back/article')
}

// 跳转到评论管理
const goToCommentManagement = () => {
  router.push('/back/comment')
}



onMounted(() => {
  updateTime()
  // 每分钟更新一次时间
  timeInterval = setInterval(updateTime, 60000)
  
  // 获取统计数据
  fetchStats()
  fetchRecentArticles()
  fetchRecentComments()
  
  // 获取图表数据
  fetchArticleTrend()
  fetchViewTrend()
})

onUnmounted(() => {
  // 清除定时器
  if (timeInterval) {
    clearInterval(timeInterval)
    timeInterval = null
  }
  
  // 销毁图表实例
  if (articleChart) {
    articleChart.dispose()
    articleChart = null
  }
  
  if (viewChart) {
    viewChart.dispose()
    viewChart = null
  }
  
  // 移除事件监听
  window.removeEventListener('resize', () => {})
})
</script>

<style lang="scss" scoped>
@import '@/assets/backend-common.css';

.dashboard {
  .welcome-card {
    margin-bottom: 20px;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
    
    .welcome-header {
      display: flex;
      align-items: center;
      gap: 20px;
      
      .el-avatar {
        transition: transform 0.3s ease;
        
        &:hover {
          transform: scale(1.1);
        }
      }
      
      .welcome-info {
        h2 {
          margin: 0 0 8px 0;
          font-size: 24px;
          background: linear-gradient(to right, #409eff, #67c23a);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }
        p {
          margin: 0;
          color: #666;
        }
      }
    }
    
    .role-info {
      margin-top: 16px;
    }
  }
  
  .stat-cards {
    margin-bottom: 20px;
    
    .stat-card {
      // height: 120px;
      display: flex;
      align-items: center;
      padding: 20px;
      margin-bottom: 20px;
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-5px);
      }
      
      .stat-icon {
        width: 64px;
        height: 64px;
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 20px;
        
        .el-icon {
          font-size: 32px;
          color: white;
        }
      }
      
      .article-icon {
        background-color: #409eff;
      }
      
      .comment-icon {
        background-color: #67c23a;
      }
      
      .user-icon {
        background-color: #e6a23c;
      }
      
      .view-icon {
        background-color: #f56c6c;
      }
      
      .stat-info {
        .stat-value {
          font-size: 28px;
          font-weight: bold;
          color: #303133;
          margin-bottom: 5px;
        }
        
        .stat-label {
          font-size: 14px;
          color: #909399;
        }
      }
    }
  }
  
  .chart-card {
    margin-bottom: 20px;
    
    .chart-container {
      height: 300px;
      width: 100%;
      
      .chart {
        height: 100%;
        width: 100%;
      }
    }
  }
  
  .recent-articles-card, .recent-comments-card {
    margin-bottom: 20px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 500;
      }
    }
    
    .article-link {
      color: #333;
      text-decoration: none;
      transition: color 0.3s;
      
      &:hover {
        color: #409eff;
      }
    }
    
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}
</style> 