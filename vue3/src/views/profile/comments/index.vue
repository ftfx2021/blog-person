<template>
  <div class="my-comments">
    <h2>我的评论</h2>
    
    <el-table
      v-loading="loading"
      :data="comments"
      style="width: 100%"
      empty-text="暂无评论内容"
    >
      <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="comment-content">{{ row.content }}</div>
        </template>
      </el-table-column>
      
      <el-table-column prop="articleTitle" label="文章标题" min-width="200">
        <template #default="{ row }">
          <router-link :to="`/article/${row.articleId}`">{{ row.articleTitle }}</router-link>
        </template>
      </el-table-column>
      
      <el-table-column prop="createTime" label="评论时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getCommentStatusTagType(row.status)">
            {{ getCommentStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button 
            size="small" 
            type="danger" 
            @click="deleteCommentHandler(row.id)"
            :loading="deleting === row.id"
          >
            删除
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
        @size-change="fetchComments"
        @current-change="fetchComments"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserComments, deleteComment } from '@/api/CommentApi'
import { getCommentStatusText, getCommentStatusTagType } from '@/utils/commentConstants'
import DateUtils from '@/utils/dateUtils'
import { ElMessage, ElMessageBox } from 'element-plus'

// 使用DateUtils格式化日期
const formatDate = (date) => {
  return DateUtils.formatDateTime(date);
}

const comments = ref([])
const loading = ref(false)
const deleting = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 获取用户评论列表
const fetchComments = async () => {
  loading.value = true
  try {
    await getUserComments({
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        comments.value = data.records
        total.value = data.total
      }
    })
  } catch (error) {
    console.error('获取评论列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 删除评论
const deleteCommentHandler = async (commentId) => {
  ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    deleting.value = commentId
    try {
      await deleteComment(commentId, {
        successMsg: '评论删除成功',
        onSuccess: () => {
          // 从列表中移除该评论
          comments.value = comments.value.filter(item => item.id !== commentId)
          // 如果当前页没有数据且不是第一页，则跳转到上一页
          if (comments.value.length === 0 && currentPage.value > 1) {
            currentPage.value--
            fetchComments()
          }
          // 更新总数
          total.value--
        }
      })
    } catch (error) {
      console.error('删除评论失败:', error)
    } finally {
      deleting.value = null
    }
  }).catch(() => {
    // 用户取消操作
  })
}


onMounted(() => {
  fetchComments()
})
</script>

<style scoped>
.my-comments {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: 500;
}

.comment-content {
  white-space: normal;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style> 