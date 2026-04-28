<template>
  <div class="my-likes">
    <h2>我的点赞</h2>
    
    <el-table
      v-loading="loading"
      :data="likedArticles"
      style="width: 100%"
      empty-text="暂无点赞内容"
    >
      <el-table-column prop="title" label="文章标题">
        <template #default="{ row }">
          <router-link :to="`/article/${row.id}`">{{ row.title }}</router-link>
        </template>
      </el-table-column>
      
      <el-table-column prop="authorName" label="作者"></el-table-column>
      
      <el-table-column prop="createTime" label="点赞时间">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button 
            size="small" 
            type="danger" 
            @click="cancelLike(row.id)"
            :loading="canceling === row.id"
          >
            取消点赞
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 30, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="fetchLikedArticles"
        @current-change="fetchLikedArticles"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DateUtils from '@/utils/dateUtils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserLikedArticlesPage, toggleArticleLike } from '@/api/ArticleApi'

// 使用DateUtils格式化日期
const formatDate = (date) => {
  return DateUtils.formatDateTime(date);
}

const likedArticles = ref([])
const loading = ref(false)
const canceling = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 获取用户点赞的文章列表
const fetchLikedArticles = () => {
  loading.value = true
  getUserLikedArticlesPage({
    currentPage: currentPage.value,
    size: pageSize.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      likedArticles.value = data.records
      total.value = data.total
      loading.value = false
    },
    onError: (error) => {
      console.error('获取点赞文章列表失败:', error)
      loading.value = false
    }
  })
}

// 取消点赞
const cancelLike = (articleId) => {
  ElMessageBox.confirm('确定要取消点赞该文章吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    canceling.value = articleId
    toggleArticleLike(articleId, {
      successMsg: '已取消点赞',
      onSuccess: () => {
        // 从列表中移除该文章
        likedArticles.value = likedArticles.value.filter(item => item.id !== articleId)
        // 如果当前页没有数据且不是第一页，则跳转到上一页
        if (likedArticles.value.length === 0 && currentPage.value > 1) {
          currentPage.value--
          fetchLikedArticles()
        }
        // 更新总数
        total.value--
        canceling.value = null
      },
      onError: (error) => {
        console.error('取消点赞失败:', error)
        canceling.value = null
      }
    })
  }).catch(() => {
    // 用户取消操作
  })
}

onMounted(() => {
  fetchLikedArticles()
})
</script>

<style scoped>
.my-likes {
  padding: 20px;

}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: 500;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style> 