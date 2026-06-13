<template>
  <div class="stats-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>知识库统计</span>
          <el-button type="primary" :icon="Refresh" @click="fetchStats" :loading="loading">刷新</el-button>
        </div>
      </template>
      
      <!-- 统计卡片行 -->
      <el-row :gutter="20" class="stats-cards">
        <el-col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
          <el-card class="stat-card blue-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon :size="32"><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalPublishedArticles || 0 }}</div>
                <div class="stat-label">已发布文章</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
          <el-card class="stat-card green-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon :size="32"><CircleCheck /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.successfullyVectorized || 0 }}</div>
                <div class="stat-label">成功向量化</div>
                <div class="stat-percentage">{{ successPercentage }}%</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
          <el-card class="stat-card yellow-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon :size="32"><Loading /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.inProgressVectorization || 0 }}</div>
                <div class="stat-label">处理中</div>
                <div class="stat-percentage">{{ inProgressPercentage }}%</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
          <el-card class="stat-card red-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon :size="32"><CircleClose /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.failedVectorization || 0 }}</div>
                <div class="stat-label">失败</div>
                <div class="stat-percentage">{{ failedPercentage }}%</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
          <el-card class="stat-card gray-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon :size="32"><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.pendingVectorization || 0 }}</div>
                <div class="stat-label">待向量化</div>
                <div class="stat-percentage">{{ pendingPercentage }}%</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 文章状态表格 -->
      <div class="table-section">
        <div class="table-header">
          <h3>文章向量化状态</h3>
          <el-select
            v-model="filterStatus"
            placeholder="筛选状态"
            clearable
            style="width: 160px"
            @change="handleFilterChange"
          >
            <el-option :value="2" label="成功" />
            <el-option :value="1" label="处理中" />
            <el-option :value="-1" label="失败" />
            <el-option :value="0" label="待向量化" />
          </el-select>
        </div>
        
        <el-table
          :data="filteredArticleList"
          style="width: 100%"
          v-loading="loading"
          border
          :default-sort="{ prop: 'lastVectorizedSuccessTime', order: 'descending' }"
        >
          <el-table-column prop="title" label="文章标题" show-overflow-tooltip min-width="200" />
          
          <el-table-column label="向量化状态" width="140" align="center">
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
              <!-- Failed (-1): Red badge with error icon -->
              <el-tag v-else-if="scope.row.vectorizedStatus === -1" type="danger" size="small">
                <el-icon><CircleClose /></el-icon>
                <span style="margin-left: 4px;">失败</span>
              </el-tag>
              <!-- Not Vectorized (0 or null): Gray badge -->
              <el-tag v-else type="info" size="small">
                <span>待向量化</span>
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="vectorizedErrorReason" label="错误原因" show-overflow-tooltip min-width="200">
            <template #default="scope">
              <span v-if="scope.row.vectorizedErrorReason" class="error-reason">
                {{ scope.row.vectorizedErrorReason }}
              </span>
              <span v-else class="no-error">-</span>
            </template>
          </el-table-column>
          
          <el-table-column
            prop="lastVectorizedSuccessTime"
            label="最后成功时间"
            width="180"
            sortable
            :sort-method="sortByTime"
          >
            <template #default="scope">
              <span v-if="scope.row.lastVectorizedSuccessTime">
                {{ formatDateTime(scope.row.lastVectorizedSuccessTime) }}
              </span>
              <span v-else class="no-time">-</span>
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
            :total="filteredTotal"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, CircleCheck, Loading, CircleClose, Clock, Refresh } from '@element-plus/icons-vue'
import { getKnowledgeBaseStats } from '@/api/ArticleApi'

// 加载状态
const loading = ref(false)

// 统计数据
const stats = ref({
  totalPublishedArticles: 0,
  successfullyVectorized: 0,
  inProgressVectorization: 0,
  failedVectorization: 0,
  pendingVectorization: 0,
  articleStatuses: []
})

// 筛选状态
const filterStatus = ref(null)

// 分页信息
const currentPage = ref(1)
const pageSize = ref(10)

// 计算百分比
const successPercentage = computed(() => {
  if (stats.value.totalPublishedArticles === 0) return 0
  return ((stats.value.successfullyVectorized / stats.value.totalPublishedArticles) * 100).toFixed(1)
})

const inProgressPercentage = computed(() => {
  if (stats.value.totalPublishedArticles === 0) return 0
  return ((stats.value.inProgressVectorization / stats.value.totalPublishedArticles) * 100).toFixed(1)
})

const failedPercentage = computed(() => {
  if (stats.value.totalPublishedArticles === 0) return 0
  return ((stats.value.failedVectorization / stats.value.totalPublishedArticles) * 100).toFixed(1)
})

const pendingPercentage = computed(() => {
  if (stats.value.totalPublishedArticles === 0) return 0
  return ((stats.value.pendingVectorization / stats.value.totalPublishedArticles) * 100).toFixed(1)
})

// 筛选后的文章列表
const filteredArticles = computed(() => {
  if (filterStatus.value === null) {
    return stats.value.articleStatuses || []
  }
  return (stats.value.articleStatuses || []).filter(
    article => article.vectorizedStatus === filterStatus.value
  )
})

// 分页后的文章列表
const filteredArticleList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredArticles.value.slice(start, end)
})

// 筛选后的总数
const filteredTotal = computed(() => {
  return filteredArticles.value.length
})

// 初始化
onMounted(() => {
  fetchStats()
})

// 获取统计数据
const fetchStats = () => {
  loading.value = true
  getKnowledgeBaseStats({
    showDefaultMsg: false,
    onSuccess: (data) => {
      stats.value = data || {
        totalPublishedArticles: 0,
        successfullyVectorized: 0,
        inProgressVectorization: 0,
        failedVectorization: 0,
        pendingVectorization: 0,
        articleStatuses: []
      }
      loading.value = false
    },
    onError: (error) => {
      console.error('获取统计信息失败:', error)
      ElMessage.error('获取统计信息失败')
      loading.value = false
    }
  })
}

// 处理筛选变化
const handleFilterChange = () => {
  currentPage.value = 1
}

// 处理每页显示数量变化
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
}

// 时间排序方法
const sortByTime = (a, b) => {
  const timeA = a.lastVectorizedSuccessTime ? new Date(a.lastVectorizedSuccessTime).getTime() : 0
  const timeB = b.lastVectorizedSuccessTime ? new Date(b.lastVectorizedSuccessTime).getTime() : 0
  return timeA - timeB
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}
</script>

<style scoped>
@import '@/assets/backend-common.css';

.stats-container {
  padding: 24px;
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

/* 统计卡片样式 */
.stats-cards {
  margin-bottom: 32px;
}

.stat-card {
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background-color: rgba(255, 255, 255, 0.2);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 2px;
}

.stat-percentage {
  font-size: 12px;
  opacity: 0.8;
}

/* 卡片颜色主题 */
.blue-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.green-card {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
  border: none;
}

.yellow-card {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  border: none;
}

.red-card {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white;
  border: none;
}

.gray-card {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #333;
  border: none;
}

/* 表格部分样式 */
.table-section {
  margin-top: 24px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-header h3 {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.error-reason {
  color: #f56c6c;
}

.no-error,
.no-time {
  color: #909399;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-cards {
    margin-bottom: 24px;
  }
  
  .stat-content {
    flex-direction: column;
    text-align: center;
  }
  
  .stat-icon {
    width: 48px;
    height: 48px;
  }
  
  .stat-value {
    font-size: 24px;
  }
}
</style>
