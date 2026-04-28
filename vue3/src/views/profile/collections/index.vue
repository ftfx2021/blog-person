<template>
  <div class="my-collections">
    <h2>我的收藏</h2>
    
    <div v-loading="loading">
      <el-empty v-if="!loading && collections.length === 0" description="暂无收藏内容" />
      
      <div v-else class="collection-list">
        <el-card v-for="item in collections" :key="item.id" class="collection-item">
          <div class="article-info">
            <div class="article-cover" v-if="item.coverImage">
              <el-image :src="item.coverImage" fit="cover" />
            </div>
            <div class="article-content">
              <h3 class="article-title">
                <router-link :to="`/article/${item.id}`">{{ item.title }}</router-link>
              </h3>
              <div class="article-summary" v-if="item.summary">{{ item.summary }}</div>
              <div class="article-meta">
                <span><el-icon><User /></el-icon> {{ item.authorName }}</span>
                <span><el-icon><Calendar /></el-icon> {{ formatDate(item.createTime) }}</span>
                <span><el-icon><View /></el-icon> {{ item.viewCount }} 阅读</span>
                <span><el-icon><Star /></el-icon> {{ item.likeCount }} 点赞</span>
                <span><el-icon><ChatDotRound /></el-icon> {{ item.commentCount }} 评论</span>
              </div>
            </div>
          </div>
          <div class="article-actions">
            <span class="collect-time">收藏于：{{ formatDate(item.collectTime) }}</span>
            <el-button 
              size="small" 
              type="danger" 
              @click="cancelCollect(item.id)"
              :loading="canceling === item.id"
            >
              取消收藏
            </el-button>
          </div>
        </el-card>
      </div>
      
      <!-- 分页 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="fetchCollections"
          @current-change="fetchCollections"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserCollectedArticlesPage, toggleArticleCollect } from '@/api/ArticleApi'
import DateUtils from '@/utils/dateUtils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Calendar, View, Star, ChatDotRound } from '@element-plus/icons-vue'

// 使用DateUtils格式化日期
const formatDate = (date) => {
  return DateUtils.formatDateTime(date);
}

const collections = ref([])
const loading = ref(false)
const canceling = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)


// 获取用户收藏列表
const fetchCollections = () => {
  loading.value = true
  getUserCollectedArticlesPage({
    currentPage: currentPage.value,
    size: pageSize.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      collections.value = data.records || []
      total.value = data.total || 0
      loading.value = false
    },
    onError: (error) => {
      console.error('获取收藏列表失败:', error)
      loading.value = false
    }
  })
}

// 取消收藏
const cancelCollect = (articleId) => {
  ElMessageBox.confirm('确定要取消收藏该文章吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    canceling.value = articleId
    toggleArticleCollect(articleId, {
      successMsg: '已取消收藏',
      onSuccess: () => {
        // 从列表中移除该文章
        collections.value = collections.value.filter(item => item.id !== articleId)
        // 如果当前页没有数据且不是第一页，则跳转到上一页
        if (collections.value.length === 0 && currentPage.value > 1) {
          currentPage.value--
          fetchCollections()
        } else {
          // 更新总数
          total.value = Math.max(0, total.value - 1)
        }
        canceling.value = null
      },
      onError: (error) => {
        console.error('取消收藏失败:', error)
        canceling.value = null
      }
    })
  }).catch(() => {
    // 用户取消操作
  })
}



onMounted(() => {
  fetchCollections()
})
</script>

<style scoped>
.my-collections {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: 500;
}

.collection-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.collection-item {
  transition: all 0.3s;
}

.collection-item:hover {
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.article-info {
  display: flex;
  gap: 15px;
}

.article-cover {
  width: 180px;
  height: 120px;
  overflow: hidden;
  border-radius: 4px;
  flex-shrink: 0;
}

.article-cover .el-image {
  width: 100%;
  height: 100%;
}

.article-content {
  flex: 1;
  overflow: hidden;
}

.article-title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: bold;
}

.article-title a {
  color: #303133;
  text-decoration: none;
}

.article-title a:hover {
  color: #409eff;
}

.article-summary {
  color: #606266;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  color: #909399;
  font-size: 13px;
}

.article-meta span {
  display: flex;
  align-items: center;
}

.article-meta .el-icon {
  margin-right: 4px;
}

.article-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.collect-time {
  color: #909399;
  font-size: 13px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style> 