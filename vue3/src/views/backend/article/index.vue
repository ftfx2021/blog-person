<template>
  <div class="article-container">
    <!-- 知识库统计卡片 -->
    <el-card class="stats-card" v-loading="statsLoading">
      <template #header>
        <div class="card-header">
          <span>知识库统计</span>
          <el-button 
            type="primary" 
            size="small" 
            :icon="Refresh" 
            :loading="statsLoading"
            @click="fetchKnowledgeStats"
          >
            刷新
          </el-button>
        </div>
      </template>
      <div class="stats-content">
        <div class="stat-item">
          <div class="stat-label">已发布文章</div>
          <div class="stat-value">{{ globalVectorizationStats.totalPublishedArticles || 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">已向量化</div>
          <div class="stat-value success">{{ globalVectorizationStats.successfullyVectorized || 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">处理中</div>
          <div class="stat-value processing">{{ globalVectorizationStats.inProgressVectorization || 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">失败</div>
          <div class="stat-value failed">{{ globalVectorizationStats.failedVectorization || 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">待向量化</div>
          <div class="stat-value">{{ globalVectorizationStats.pendingVectorization || 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">向量化率</div>
          <div class="stat-value" :class="vectorizationRateClass">
            {{ vectorizationRate }}
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>文章管理</span>
          <el-button type="primary" @click="$router.push('/back/article/create')">写文章</el-button>
        </div>
      </template>
      
      <!-- 搜索表单 -->
      <div class="search-form">
        <el-form :inline="true" :model="searchForm">
          <el-form-item>
            <el-input
              v-model="searchForm.title"
              placeholder="文章标题"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="searchForm.categoryId"
              placeholder="文章分类"
              clearable
              style="width: 160px"
            >
              <el-option
                v-for="item in categoryOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="searchForm.status"
              placeholder="文章状态"
              clearable
              style="width: 160px"
            >
              <el-option :value="0" label="草稿" />
              <el-option :value="1" label="已发布" />
              <el-option :value="2" label="已删除" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="searchForm.isVectorized"
              placeholder="向量化状态"
              clearable
              style="width: 160px"
            >
              <el-option :value="0" label="未向量化" />
              <el-option :value="1" label="已向量化" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchArticleList(1)">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 文章列表 -->
      <el-table :data="articleList" style="width: 100%" v-loading="loading" border>
        
        <el-table-column label="封面" width="100">
          <template #default="scope">
            <el-image
              v-if="scope.row.coverImage"
           
                :src="scope.row.coverImage"
              :preview-src-list="[scope.row.coverImage]"
              fit="cover"
              :preview-teleported="true"
              class="article-cover"
            />
            <div v-else class="no-image">无封面</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="标签" width="180">
          <template #default="scope">
            <el-tag
              v-for="tag in scope.row.tags"
              :key="tag.id"
              :color="tag.color"
              effect="dark"
              class="tag-item"
            >
              {{ tag.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning">草稿</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success">已发布</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="danger">已删除</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="向量化状态" width="140">
          <template #default="scope">
            <!-- Success (2): Green badge with checkmark -->
            <el-tag v-if="scope.row.vectorizedStatus === 2" type="success" size="small">
              <el-icon><CircleCheck /></el-icon>
              <span style="margin-left: 4px;">成功</span>
            </el-tag>
            <!-- In Progress (1): Blue badge with loading icon -->
            <el-tag v-else-if="scope.row.vectorizedStatus === 1" type="primary" size="small">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span style="margin-left: 4px;">处理中</span>
            </el-tag>
            <!-- Failed (-1): Red badge with error icon and tooltip -->
            <el-tooltip 
              v-else-if="scope.row.vectorizedStatus === -1" 
              :content="scope.row.vectorizedErrorReason || '向量化失败'" 
              placement="top"
            >
              <el-tag type="danger" size="small">
                <el-icon><CircleClose /></el-icon>
                <span style="margin-left: 4px;">失败</span>
              </el-tag>
            </el-tooltip>
            <!-- Not Vectorized (0 or null): Gray badge -->
            <el-tag v-else type="info" size="small">
              <span>未向量化</span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="向量化详情" width="200">
          <template #default="scope">
            <div v-if="scope.row.vectorizedStatus" style="font-size: 12px; color: #666;">
              <div v-if="scope.row.vectorizedRetryCount > 0">
                重试: {{ scope.row.vectorizedRetryCount }}次
              </div>
              <div v-if="scope.row.lastVectorizedSuccessTime" style="margin-top: 2px;">
                成功: {{ formatDateTime(scope.row.lastVectorizedSuccessTime) }}
              </div>
              <div v-if="scope.row.vectorizedProcessAt && scope.row.vectorizedStatus === 1" style="margin-top: 2px;">
                开始: {{ formatDateTime(scope.row.vectorizedProcessAt) }}
              </div>
              <el-tooltip 
                v-if="scope.row.vectorizedErrorReason" 
                :content="scope.row.vectorizedErrorReason" 
                placement="top"
              >
                <div style="margin-top: 2px; color: #f56c6c; cursor: pointer;">
                  错误原因 <el-icon><InfoFilled /></el-icon>
                </div>
              </el-tooltip>
            </div>
            <span v-else style="font-size: 12px; color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="统计" width="180">
          <template #default="scope">
            <div class="article-stats">
              <span><i class="el-icon-view"></i> {{ scope.row.viewCount }}</span>
              <span><i class="el-icon-star-on"></i> {{ scope.row.likeCount }}</span>
              <span><i class="el-icon-chat-line-square"></i> {{ scope.row.commentCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="450" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status !== 2"
              type="primary"
              size="small"
              @click="$router.push(`/back/article/edit/${scope.row.id}`)"
            >
              编辑
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              type="success"
              size="small"
              @click="updateArticleStatusHandler(scope.row.id, 1)"
            >
              发布
            </el-button>
            <el-button
              v-if="scope.row.status === 1"
              type="warning"
              size="small"
              @click="updateArticleStatusHandler(scope.row.id, 0)"
            >
              撤回
            </el-button>
            <el-button
              v-if="scope.row.status === 1 && scope.row.isVectorized !== 1"
              type="info"
              size="small"
              :loading="vectorizingId === scope.row.id"
              @click="handleVectorize(scope.row.id)"
            >
              向量化
            </el-button>
            <el-button
              v-if="scope.row.vectorizedStatus === 2"
              type="primary"
              plain
              size="small"
              @click="showVectorMetadata(scope.row)"
            >
              查看向量
            </el-button>
            <el-button
              v-if="scope.row.likeCount > 0"
              type="primary"
            
              size="small"
              @click="showLikeUsers(scope.row)"
            >
              点赞用户
            </el-button>
            <el-button
              v-if="scope.row.collectCount > 0"
              type="success"
              plain
              size="small"
              @click="showCollectUsers(scope.row)"
            >
              收藏用户
            </el-button>
            <el-popconfirm
              v-if="scope.row.status !== 2"
              title="确定删除该文章吗？"
              @confirm="updateArticleStatusHandler(scope.row.id, 2)"
              width="220"
            >
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
            <el-button
              v-if="scope.row.status === 2"
              type="info"
              size="small"
              @click="updateArticleStatusHandler(scope.row.id, 0)"
            >
              恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 点赞用户对话框 -->
    <el-dialog
      v-model="likeDialogVisible"
      :title="`《${currentArticle?.title || ''}》的点赞用户`"
      width="700px"
    >
      <el-table :data="likeUserList" border style="width: 100%" v-loading="likeLoading">
        <el-table-column type="index" width="50" label="#"></el-table-column>
        <el-table-column prop="username" label="用户名" width="120"></el-table-column>
        <el-table-column prop="name" label="姓名" width="120"></el-table-column>
        <el-table-column label="头像" width="80">
          <template #default="scope">
            <el-avatar 
              :size="40" 
              :src="scope.row.avatar" 
            ></el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱"></el-table-column>
        <el-table-column prop="likeTime" label="点赞时间" width="180"></el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          :current-page="likeCurrentPage"
          :page-size="likePageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="likeTotal"
          @size-change="handleLikeSizeChange"
          @current-change="handleLikeCurrentChange"
        />
      </div>
    </el-dialog>
    
    <!-- 收藏用户对话框 -->
    <el-dialog
      v-model="collectDialogVisible"
      :title="`《${currentArticle?.title || ''}》的收藏用户`"
      width="700px"
    >
      <el-table :data="collectUserList" border style="width: 100%" v-loading="collectLoading">
        <el-table-column type="index" width="50" label="#"></el-table-column>
        <el-table-column prop="username" label="用户名" width="120"></el-table-column>
        <el-table-column prop="name" label="姓名" width="120"></el-table-column>
        <el-table-column label="头像" width="80">
          <template #default="scope">
            <el-avatar 
              :size="40" 
              :src="scope.row.avatar" 
            ></el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱"></el-table-column>
        <el-table-column prop="collectTime" label="收藏时间" width="180"></el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          :current-page="collectCurrentPage"
          :page-size="collectPageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="collectTotal"
          @size-change="handleCollectSizeChange"
          @current-change="handleCollectCurrentChange"
        />
      </div>
    </el-dialog>
    
    <!-- 向量元数据对话框 -->
    <el-dialog
      v-model="vectorMetadataVisible"
      :title="`《${currentArticle?.title || ''}》的向量元数据`"
      width="900px"
    >
      <div v-if="vectorMetadataList.length > 0" style="margin-bottom: 16px;">
        <el-tag type="info">总分片数: {{ vectorMetadataList[0]?.totalChunks || 0 }}</el-tag>
      </div>
      <el-table :data="vectorMetadataList" border style="width: 100%" v-loading="vectorMetadataLoading">
        <el-table-column prop="chunkIndex" label="分片索引" width="100" sortable></el-table-column>
        <el-table-column prop="chunkId" label="分片ID" width="280" show-overflow-tooltip></el-table-column>
        <el-table-column prop="contentPreview" label="内容预览" show-overflow-tooltip></el-table-column>
      </el-table>
      <el-empty v-if="!vectorMetadataLoading && vectorMetadataList.length === 0" description="该文章尚未向量化" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Loading, CircleClose, InfoFilled, Refresh } from '@element-plus/icons-vue'
import { getArticlesPage, updateArticleStatus, getArticleLikeUsers, getArticleCollectUsers, getArticleVectorMetadata, getKnowledgeBaseStats } from '@/api/ArticleApi'
import { getAllCategories } from '@/api/CategoryApi'
import { vectorizeArticle } from '@/api/KnowledgeApi'



// 加载状态
const loading = ref(false)
// 文章列表
const articleList = ref([])
// 分类选项
const categoryOptions = ref([])
// 分页信息
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
// 搜索表单
const searchForm = reactive({
  title: '',
  categoryId: '',
  status: '',
  isVectorized: ''
})

// 点赞用户对话框相关
const likeDialogVisible = ref(false)
const likeLoading = ref(false)
const likeUserList = ref([])
const likeCurrentPage = ref(1)
const likePageSize = ref(10)
const likeTotal = ref(0)
const currentArticle = ref(null)

// 收藏用户对话框相关
const collectDialogVisible = ref(false)
const collectLoading = ref(false)
const collectUserList = ref([])
const collectCurrentPage = ref(1)
const collectPageSize = ref(10)
const collectTotal = ref(0)

// 向量化相关
const vectorizingId = ref(null)

// 向量元数据对话框相关
const vectorMetadataVisible = ref(false)
const vectorMetadataLoading = ref(false)
const vectorMetadataList = ref([])

// 知识库统计相关
const statsLoading = ref(false)
const globalVectorizationStats = ref({
  totalPublishedArticles: 0,
  successfullyVectorized: 0,
  inProgressVectorization: 0,
  failedVectorization: 0,
  pendingVectorization: 0
})
const autoRefreshEnabled = ref(true)
let refreshTimer = null

// 计算向量化率
const vectorizationRate = computed(() => {
  const totalCount = globalVectorizationStats.value.totalPublishedArticles
  if (totalCount === 0) return '0%'
  const successCount = globalVectorizationStats.value.successfullyVectorized || 0
  const rate = ((successCount / totalCount) * 100).toFixed(1)
  return `${rate}%`
})

// 向量化率样式
const vectorizationRateClass = computed(() => {
  const rate = parseFloat(vectorizationRate.value)
  if (rate >= 80) return 'success'
  if (rate >= 50) return 'processing'
  return 'failed'
})

// 初始化
onMounted(() => {
  fetchCategoryList()
  fetchArticleList(1)
  fetchKnowledgeStats()
  startAutoRefresh()
})

// 组件卸载时清理定时器
onUnmounted(() => {
  stopAutoRefresh()
})

// 获取分类列表
const fetchCategoryList = () => {
  getAllCategories({
    showDefaultMsg: false,
    onSuccess: (data) => {
      categoryOptions.value = data
    },
    onError: (error) => {
      console.error('获取分类列表失败:', error)
    }
  })
}

// 获取文章列表
const fetchArticleList = (page) => {
  if (page) {
    currentPage.value = page
  }
  
  loading.value = true
  const queryParams = {
    currentPage: currentPage.value,
    size: pageSize.value,
    title: searchForm.title || undefined,
    categoryId: searchForm.categoryId || undefined,
    status: searchForm.status !== '' ? searchForm.status : undefined,
    isVectorized: searchForm.isVectorized !== '' ? searchForm.isVectorized : undefined
  }
  
  getArticlesPage(queryParams, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      articleList.value = data.records || []
      total.value = data.total || 0
      loading.value = false
    },
    onError: (error) => {
      console.error('获取文章列表失败:', error)
      loading.value = false
    }
  })
}

// 处理每页显示数量变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchArticleList(1)
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchArticleList()
}

// 重置搜索条件
const resetSearch = () => {
  searchForm.title = ''
  searchForm.categoryId = ''
  searchForm.status = ''
  searchForm.isVectorized = ''
  fetchArticleList(1)
}

// 更新文章状态
const updateArticleStatusHandler = (id, status) => {
  updateArticleStatus(id, status, {
    successMsg: '操作成功',
    onSuccess: () => {
      fetchArticleList()
    },
    onError: (error) => {
      console.error('更新文章状态失败:', error)
    }
  })
}

// 向量化文章
const handleVectorize = (articleId) => {
  vectorizingId.value = articleId
  vectorizeArticle(articleId, {
    onSuccess: () => {
      fetchArticleList()
      vectorizingId.value = null
    },
    onError: (error) => {
      console.error('向量化失败:', error)
      vectorizingId.value = null
    }
  })
}

// 显示点赞用户
const showLikeUsers = (article) => {
  currentArticle.value = article
  likeDialogVisible.value = true
  likeCurrentPage.value = 1
  // 重置数据
  likeUserList.value = []
  likeTotal.value = 0
  // 打开对话框后立即获取数据
  fetchLikeUsers()
}

// 获取点赞用户列表
const fetchLikeUsers = () => {
  if (!currentArticle.value) return
  
  likeLoading.value = true
  getArticleLikeUsers(currentArticle.value.id, {
    currentPage: likeCurrentPage.value,
    size: likePageSize.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      likeUserList.value = data.records || []
      likeTotal.value = data.total || 0
      likeLoading.value = false
    },
    onError: (error) => {
      console.error('获取点赞用户列表失败:', error)
      likeLoading.value = false
    }
  })
}

// 点赞用户分页大小变化
const handleLikeSizeChange = (val) => {
  likePageSize.value = val
  fetchLikeUsers()
}

// 点赞用户页码变化
const handleLikeCurrentChange = (val) => {
  likeCurrentPage.value = val
  fetchLikeUsers()
}

// 显示收藏用户
const showCollectUsers = (article) => {
  currentArticle.value = article
  collectDialogVisible.value = true
  collectCurrentPage.value = 1
  // 重置数据
  collectUserList.value = []
  collectTotal.value = 0
  // 打开对话框后立即获取数据
  fetchCollectUsers()
}

// 获取收藏用户列表
const fetchCollectUsers = () => {
  if (!currentArticle.value) return
  
  collectLoading.value = true
  getArticleCollectUsers(currentArticle.value.id, {
    currentPage: collectCurrentPage.value,
    size: collectPageSize.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      collectUserList.value = data.records || []
      collectTotal.value = data.total || 0
      collectLoading.value = false
    },
    onError: (error) => {
      console.error('获取收藏用户列表失败:', error)
      collectLoading.value = false
    }
  })
}

// 收藏用户分页大小变化
const handleCollectSizeChange = (val) => {
  collectPageSize.value = val
  fetchCollectUsers()
}

// 收藏用户页码变化
const handleCollectCurrentChange = (val) => {
  collectCurrentPage.value = val
  fetchCollectUsers()
}

// 显示向量元数据
const showVectorMetadata = (article) => {
  currentArticle.value = article
  vectorMetadataVisible.value = true
  vectorMetadataList.value = []
  
  vectorMetadataLoading.value = true
  getArticleVectorMetadata(article.id, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      vectorMetadataList.value = data || []
      vectorMetadataLoading.value = false
    },
    onError: (error) => {
      console.error('获取向量元数据失败:', error)
      ElMessage.error('获取向量元数据失败')
      vectorMetadataLoading.value = false
    }
  })
}

// 获取知识库统计（全局统计）
const fetchKnowledgeStats = async () => {
  statsLoading.value = true
  try {
    await getKnowledgeBaseStats({
      showDefaultMsg: false,
      onSuccess: (data) => {
        globalVectorizationStats.value = {
          totalPublishedArticles: data.totalPublishedArticles || 0,
          successfullyVectorized: data.successfullyVectorized || 0,
          inProgressVectorization: data.inProgressVectorization || 0,
          failedVectorization: data.failedVectorization || 0,
          pendingVectorization: data.pendingVectorization || 0
        }
      },
      onError: (error) => {
        console.error('获取知识库统计失败:', error)
      }
    })
  } finally {
    statsLoading.value = false
  }
}

// 计算向量化状态统计（已废弃，改用全局统计）
const calculateVectorizationStats = () => {
  // 此方法已不再使用，保留以防兼容性问题
}

// 启动自动刷新
const startAutoRefresh = () => {
  if (refreshTimer) return
  
  autoRefreshEnabled.value = true
  refreshTimer = setInterval(() => {
    // 静默刷新，不显示loading
    const currentLoading = loading.value
    const currentStatsLoading = statsLoading.value
    loading.value = false
    statsLoading.value = false
    fetchArticleList()
    fetchKnowledgeStats()
    loading.value = currentLoading
    statsLoading.value = currentStatsLoading
  }, 5000) // 每5秒刷新一次
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
    autoRefreshEnabled.value = false
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return dateTime.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
@import '@/assets/backend-common.css';

.article-container {
  padding: 24px;
}

.stats-card {
  margin-bottom: 24px;
  border-radius: 8px;
  border: 1px solid #eaeaea;
  box-shadow: none;
}

.stats-content {
  display: flex;
  gap: 32px;
  padding: 16px 0;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

.stat-value.processing {
  color: #409eff;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.failed {
  color: #f56c6c;
}

.box-card {
  border-radius: 8px;
  border: 1px solid #eaeaea;
  box-shadow: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #eaeaea;
}

.card-header span {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.search-form {
  margin-bottom: 24px;
  padding-top: 8px;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.article-cover {
  width: 60px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid #eaeaea;
  object-fit: cover;
}

.no-image {
  width: 60px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
  color: #666;
  font-size: 12px;
  border-radius: 4px;
  border: 1px solid #eaeaea;
}

.article-stats {
  display: flex;
  gap: 12px;
  color: #666;
  font-size: 13px;
}

.tag-item {
  margin-right: 5px;
  margin-bottom: 5px;
  border: none;
}
</style> 