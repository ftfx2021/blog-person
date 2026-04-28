<template>
  <div class="sentence-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>句子管理</span>
          <el-button type="primary" @click="handleAdd">
            <i class="fa-solid fa-plus"></i> 新增句子
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索句子内容或关键词"
          clearable
          style="width: 300px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <i class="fa-solid fa-search"></i>
          </template>
        </el-input>
        <el-select v-model="searchHomepage" placeholder="首页展示" clearable style="width: 150px">
          <el-option label="全部" :value="null" />
          <el-option label="首页展示" :value="1" />
          <el-option label="不展示" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="sentenceList" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="句子内容" min-width="300" show-overflow-tooltip>
          <template #default="scope">
            <div class="sentence-content" v-html="highlightKeywords(scope.row.sentenceContent, scope.row.keywords)"></div>
          </template>
        </el-table-column>
        <el-table-column prop="keywords" label="关键词" width="200" show-overflow-tooltip>
          <template #default="scope">
            <div class="keywords-tags" v-if="scope.row.keywords">
              <el-tag 
                v-for="(keyword, index) in scope.row.keywords.split(',')" 
                :key="index"
                size="small"
                type="info"
                style="margin-right: 4px; margin-bottom: 4px;"
              >
                {{ keyword.trim() }}
              </el-tag>
            </div>
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="首页展示" width="120" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.isHomepage"
              :active-value="1"
              :inactive-value="0"
              @change="handleToggleHomepage(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-popconfirm
              title="确定删除该句子吗？"
              @confirm="handleDelete(scope.row)"
              width="220"
            >
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 句子表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增句子' : '编辑句子'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="句子内容" prop="sentenceContent">
          <el-input
            v-model="formData.sentenceContent"
            type="textarea"
            :rows="4"
            placeholder="请输入句子内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input
            v-model="formData.keywords"
            placeholder="请输入关键词，多个用逗号隔开（关键词必须包含在句子中）"
          />
          <div class="form-tip">
            <i class="fa-solid fa-circle-info"></i>
            关键词将在首页高亮显示，必须是句子内容的一部分
          </div>
        </el-form-item>
        <el-form-item label="首页展示" prop="isHomepage">
          <el-switch
            v-model="formData.isHomepage"
            :active-value="1"
            :inactive-value="0"
          />
          <span class="switch-label">{{ formData.isHomepage === 1 ? '展示' : '不展示' }}</span>
        </el-form-item>

        <!-- 预览区域 -->
        <el-form-item label="效果预览">
          <div class="preview-box">
            <div class="preview-sentence" v-html="previewHtml"></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { 
  getSentencePage, 
  createSentence, 
  updateSentence, 
  deleteSentence, 
  toggleSentenceHomepage 
} from '@/api/SentenceApi'
import DateUtils from '@/utils/dateUtils'

// 加载状态
const loading = ref(false)
const submitting = ref(false)

// 列表数据
const sentenceList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索条件
const searchKeyword = ref('')
const searchHomepage = ref(null)

// 对话框
const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)

// 表单数据
const formData = reactive({
  id: null,
  sentenceContent: '',
  keywords: '',
  isHomepage: 0
})

// 表单验证规则
const formRules = {
  sentenceContent: [
    { required: true, message: '请输入句子内容', trigger: 'blur' },
    { min: 1, max: 500, message: '句子长度不能超过500个字符', trigger: 'blur' }
  ]
}

// 预览HTML
const previewHtml = computed(() => {
  if (!formData.sentenceContent) {
    return '<span class="text-muted">请输入句子内容</span>'
  }
  return highlightKeywords(formData.sentenceContent, formData.keywords)
})

// 初始化
onMounted(() => {
  fetchList()
})

// 获取列表
const fetchList = () => {
  loading.value = true
  
  getSentencePage({
    current: currentPage.value,
    size: pageSize.value,
    keyword: searchKeyword.value || undefined,
    isHomepage: searchHomepage.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      sentenceList.value = data.records || []
      total.value = data.total || 0
      loading.value = false
    },
    onError: (error) => {
      console.error('获取句子列表失败:', error)
      loading.value = false
    }
  })
}

// 高亮关键词
const highlightKeywords = (content, keywords) => {
  if (!content) return ''
  if (!keywords) return content

  let result = content
  const keywordArray = keywords.split(',')
  
  keywordArray.forEach(keyword => {
    const trimmed = keyword.trim()
    if (trimmed) {
      // 使用正则替换，避免重复替换
      const regex = new RegExp(`(${escapeRegExp(trimmed)})`, 'g')
      result = result.replace(regex, '<span class="highlight">$1</span>')
    }
  })
  
  return result
}

// 转义正则特殊字符
const escapeRegExp = (string) => {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

// 格式化日期
const formatDate = (dateString) => {
  return DateUtils.formatDateTime(dateString)
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchList()
}

// 重置搜索
const handleReset = () => {
  searchKeyword.value = ''
  searchHomepage.value = null
  currentPage.value = 1
  fetchList()
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  fetchList()
}

// 页码改变
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchList()
}

// 重置表单
const resetForm = () => {
  formData.id = null
  formData.sentenceContent = ''
  formData.keywords = ''
  formData.isHomepage = 0
  
  nextTick(() => {
    if (formRef.value) {
      formRef.value.resetFields()
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
}

// 新增
const handleAdd = () => {
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogType.value = 'edit'
  formData.id = row.id
  formData.sentenceContent = row.sentenceContent
  formData.keywords = row.keywords || ''
  formData.isHomepage = row.isHomepage
  dialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  deleteSentence(row.id, {
    successMsg: '删除成功',
    onSuccess: () => {
      fetchList()
    },
    onError: (error) => {
      console.error('删除句子失败:', error)
    }
  })
}

// 切换首页展示状态
const handleToggleHomepage = (row) => {
  toggleSentenceHomepage(row.id, row.isHomepage, {
    successMsg: row.isHomepage === 1 ? '已设为首页展示' : '已取消首页展示',
    onSuccess: () => {
      // 状态已通过v-model更新，无需额外操作
    },
    onError: (error) => {
      console.error('切换首页展示状态失败:', error)
      // 恢复原状态
      row.isHomepage = row.isHomepage === 1 ? 0 : 1
    }
  })
}

// 提交表单
const submitForm = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true
      
      const payload = {
        sentenceContent: formData.sentenceContent,
        keywords: formData.keywords || null,
        isHomepage: formData.isHomepage
      }
      
      if (dialogType.value === 'add') {
        createSentence(payload, {
          successMsg: '添加成功',
          onSuccess: () => {
            dialogVisible.value = false
            resetForm()
            fetchList()
            submitting.value = false
          },
          onError: (error) => {
            console.error('添加句子失败:', error)
            submitting.value = false
          }
        })
      } else {
        updateSentence(formData.id, payload, {
          successMsg: '更新成功',
          onSuccess: () => {
            dialogVisible.value = false
            resetForm()
            fetchList()
            submitting.value = false
          },
          onError: (error) => {
            console.error('更新句子失败:', error)
            submitting.value = false
          }
        })
      }
    }
  })
}
</script>

<style scoped>
@import '@/assets/backend-common.css';

.sentence-container {
  padding: 24px;
}

.box-card {
  margin-bottom: 24px;
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

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.sentence-content {
  line-height: 1.6;
}

.sentence-content :deep(.highlight) {
  color: rgb(7, 26, 242);
  font-weight: 600;
}

.keywords-tags {
  display: flex;
  flex-wrap: wrap;
}

.text-muted {
  color: #999;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.form-tip i {
  margin-right: 4px;
}

.switch-label {
  margin-left: 10px;
  color: #666;
  font-size: 14px;
}

.preview-box {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  padding: 24px;
  min-height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-sentence {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  text-align: center;
  line-height: 1.6;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.preview-sentence :deep(.highlight) {
  color: #ffd700;
  font-weight: 700;
}

.preview-sentence :deep(.text-muted) {
  color: rgba(255, 255, 255, 0.6);
  font-weight: 400;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
