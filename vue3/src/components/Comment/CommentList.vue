<template>
  <div class="comment-list-container">
    <h3 class="section-title">评论 ({{ total }})</h3>
    
    <!-- 评论表单 -->
    <div class="comment-form">
      <el-input
        v-model="commentContent"
        type="textarea"
        :rows="4"
        placeholder="发表您的评论..."
        class="comment-textarea"
      />
      <div class="form-actions">
        <el-button type="primary" @click="submitComment" :loading="submitting" class="submit-btn">发表评论</el-button>
      </div>
    </div>
    
    <!-- 评论列表 -->
    <div class="comment-list" v-loading="loading">
      <el-empty v-if="!comments || comments.length === 0" description="暂无评论" />
      
      <div v-else class="comment-item" v-for="comment in comments" :key="comment.id">
        <div class="comment-avatar">
          <el-avatar :src="comment.avatar" :size="40"></el-avatar>
        </div>
        <div class="comment-content">
          <div class="comment-header">
            <span class="comment-author">{{ comment.name || comment.username }}</span>
            <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
          </div>
          <div class="comment-text">{{ comment.content }}</div>
          <div class="comment-actions">
            <span class="action-btn" @click="showReplyForm(comment.id, comment.userId, comment.name || comment.username)">
              <el-icon><ChatDotRound /></el-icon> 回复
            </span>
            <span class="action-btn" v-if="canDelete(comment.userId)" @click="deleteCommentHandler(comment.id)">
              <el-icon><Delete /></el-icon> 删除
            </span>
          </div>
          
          <!-- 回复列表 -->
          <div class="replies-list" v-if="comment.replies && Array.isArray(comment.replies) && comment.replies.length > 0">
            <div class="reply-item" v-for="reply in comment.replies" :key="reply.id">
              <div class="reply-avatar">
                <el-avatar :src="reply.avatar" :size="32"></el-avatar>
              </div>
              <div class="reply-content">
                <div class="reply-header">
                  <span class="reply-author">{{ reply.name || reply.username }}</span>
                  <template v-if="reply.toUserId">
                    <span class="reply-to">回复</span>
                    <span class="reply-to-author">{{ reply.toName || reply.toUsername }}</span>
                  </template>
                  <span class="reply-time">{{ formatDate(reply.createTime) }}</span>
                </div>
                <div class="reply-text">{{ reply.content }}</div>
                <div class="reply-actions">
                  <span class="action-btn" @click="showReplyForm(comment.id, reply.userId, reply.name || reply.username)">
                    <el-icon><ChatDotRound /></el-icon> 回复
                  </span>
                  <span class="action-btn" v-if="canDelete(reply.userId)" @click="deleteCommentHandler(reply.id)">
                    <el-icon><Delete /></el-icon> 删除
                  </span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 回复表单 -->
          <div class="reply-form" v-if="replyInfo.parentId === comment.id">
            <el-input
              v-model="replyInfo.content"
              type="textarea"
              :rows="2"
              :placeholder="`回复 ${replyInfo.toUsername}...`"
              class="reply-textarea"
            />
            <div class="form-actions">
              <el-button size="small" @click="cancelReply" class="cancel-btn">取消</el-button>
              <el-button size="small" type="primary" @click="submitReply" :loading="submitting" class="reply-btn">回复</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 分页 -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, defineProps, defineEmits } from 'vue'
import { useUserStore } from '@/store/user'
import { createComment, getArticleComments, deleteComment } from '@/api/CommentApi'
import DateUtils from '@/utils/dateUtils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  articleId: {
    type: [Number, String],
    required: true
  }
})

const emit = defineEmits(['comment-added', 'comment-deleted'])

const userStore = useUserStore()


// 状态变量
const comments = ref([])
const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const commentContent = ref('')

// 回复相关状态
const replyInfo = ref({
  parentId: null,
  toUserId: null,
  toUsername: '',
  content: ''
})

// 使用DateUtils格式化日期
const formatDate = (date) => {
  return DateUtils.formatDateTime(date);
}

// 获取评论列表
const fetchComments = async () => {
  loading.value = true
  try {
    await getArticleComments(props.articleId, {
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        // 确保data.records存在，否则初始化为空数组
        comments.value = data && data.records ? data.records : []
        total.value = data && data.total ? data.total : 0
      }
    })
  } catch (error) {
    console.error('获取评论失败:', error)
    ElMessage.error('获取评论失败')
    // 出错时初始化为空数组
    comments.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 提交评论
const submitComment = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录再评论')
    return
  }
  
  if (!commentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  
  submitting.value = true
  try {
    const comment = {
      articleId: props.articleId,
      content: commentContent.value,
      parentId: 0,
      toUserId: 0
    }
    
    await createComment(comment, {
      successMsg: '评论提交成功，等待审核',
      onSuccess: () => {
        commentContent.value = ''
        // 刷新评论列表
        fetchComments()
        // 通知父组件评论已添加
        emit('comment-added')
      }
    })
  } catch (error) {
    console.error('提交评论失败:', error)
  } finally {
    submitting.value = false
  }
}

// 显示回复表单
const showReplyForm = (parentId, toUserId, toUsername) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录再回复')
    return
  }
  
  replyInfo.value = {
    parentId,
    toUserId,
    toUsername,
    content: ''
  }
}

// 取消回复
const cancelReply = () => {
  replyInfo.value = {
    parentId: null,
    toUserId: null,
    toUsername: '',
    content: ''
  }
}

// 提交回复
const submitReply = async () => {
  if (!replyInfo.value.content.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  
  submitting.value = true
  try {
    const comment = {
      articleId: props.articleId,
      content: replyInfo.value.content,
      parentId: replyInfo.value.parentId,
      toUserId: replyInfo.value.toUserId
    }
    
    await createComment(comment, {
      successMsg: '回复提交成功，等待审核',
      onSuccess: () => {
        cancelReply()
        // 刷新评论列表
        fetchComments()
        // 通知父组件评论已添加
        emit('comment-added')
      }
    })
  } catch (error) {
    console.error('提交回复失败:', error)
  } finally {
    submitting.value = false
  }
}

// 删除评论
const deleteCommentHandler = async (commentId) => {
  ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteComment(commentId, {
        successMsg: '评论删除成功',
        onSuccess: () => {
          // 刷新评论列表
          fetchComments()
          // 通知父组件评论已删除
          emit('comment-deleted')
        }
      })
    } catch (error) {
      console.error('删除评论失败:', error)
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 处理分页变化
const handlePageChange = (page) => {
  currentPage.value = page
  fetchComments()
}




// 判断当前用户是否可以删除评论
const canDelete = (commentUserId) => {
  if (!userStore.isLoggedIn) return false
  // 管理员或评论作者可以删除
  return userStore.isAdmin || userStore.userInfo.id === commentUserId
}

onMounted(() => {
  fetchComments()
})
</script>

<style scoped>
.comment-list-container {
  margin-top: 30px;
}

.section-title {
  font-size: 22px;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
  color: #333;
  font-weight: 600;
}

.comment-form {
  margin-bottom: 40px;
}

.comment-textarea {
  border-radius: 6px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 15px;
}

.submit-btn {
  padding: 10px 25px;
  font-size: 14px;
  border-radius: 25px;
  transition: all 0.3s;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.comment-list {
  margin-top: 20px;
}

.comment-item {
  display: flex;
  margin-bottom: 30px;
  padding-bottom: 30px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  margin-right: 15px;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
  overflow: hidden;
  text-align: left;
}

.comment-header {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}

.comment-author {
  font-weight: 600;
  margin-right: 10px;
  color: #333;
  font-size: 15px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-text {
  line-height: 1.6;
  margin-bottom: 12px;
  word-break: break-all;
  color: #444;
  font-size: 15px;
}

.comment-actions {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.action-btn {
  font-size: 13px;
  color: #888;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  transition: color 0.2s;
}

.action-btn:hover {
  color: #409eff;
}

.action-btn .el-icon {
  margin-right: 5px;
}

.replies-list {
  margin-left: 20px;
  padding: 15px 0 0 15px;
  border-left: 2px solid #f0f0f0;
}

.reply-item {
  display: flex;
  margin-bottom: 20px;
  padding-bottom: 20px;
}

.reply-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
}

.reply-avatar {
  margin-right: 12px;
  flex-shrink: 0;
}

.reply-content {
  flex: 1;
}

.reply-header {
  margin-bottom: 8px;
  font-size: 13px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.reply-author {
  font-weight: 600;
  color: #333;
}

.reply-to {
  margin: 0 5px;
  color: #999;
}

.reply-to-author {
  font-weight: 600;
  color: #333;
}

.reply-time {
  font-size: 12px;
  color: #999;
  margin-left: 10px;
}

.reply-text {
  line-height: 1.5;
  margin-bottom: 10px;
  font-size: 14px;
  word-break: break-all;
  color: #444;
}

.reply-actions {
  display: flex;
  gap: 15px;
}

.reply-form {
  margin: 15px 0;
  padding: 15px;
  border-left: 2px solid #409eff;
  background-color: #f9f9f9;
  border-radius: 0 6px 6px 0;
}

.reply-textarea {
  border-radius: 6px;
}

.cancel-btn, .reply-btn {
  border-radius: 20px;
  transition: all 0.2s;
}

.reply-btn:hover {
  transform: translateY(-2px);
}

.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .comment-item {
    flex-direction: column;
  }
  
  .comment-avatar {
    margin-bottom: 10px;
  }
  
  .replies-list {
    margin-left: 10px;
    padding-left: 10px;
  }
}
</style> 