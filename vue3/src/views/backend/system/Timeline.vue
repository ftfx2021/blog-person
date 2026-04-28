<template>
  <div class="timeline-container">
    <div class="page-header">
      <h2>发展历程管理</h2>
      <el-button type="primary" @click="handleAdd">新增事件</el-button>
    </div>
    
    <!-- 搜索表单 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" ref="searchFormRef">
        <el-form-item label="事件标题">
          <el-input v-model="searchForm.title" placeholder="请输入事件标题" clearable></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="显示" :value="1"></el-option>
            <el-option label="隐藏" :value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 时间线列表 -->
    <el-card class="list-card">
      <el-table :data="timelineList" border style="width: 100%" v-loading="loading">
        <el-table-column type="index" width="50" label="#"></el-table-column>
        <el-table-column prop="title" label="事件标题" min-width="180" show-overflow-tooltip></el-table-column>
        <el-table-column prop="content" label="事件描述" min-width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="eventDate" label="事件日期" width="120">
          <template #default="scope">
            {{ formatDate(scope.row.eventDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="80">
          <template #default="scope">
            <div class="icon-preview" :style="{ backgroundColor: scope.row.color || '#409EFF' }">
              <i :class="['fas', scope.row.icon || 'fa-star']"></i>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="color" label="颜色" width="80">
          <template #default="scope">
            <div class="color-preview" :style="{ backgroundColor: scope.row.color || '#409EFF' }"></div>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="80"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button 
              type="primary" 
              link
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button 
              type="primary" 
              link
              @click="toggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? '隐藏' : '显示' }}
            </el-button>
            <el-button 
              type="danger" 
              link
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        ></el-pagination>
      </div>
    </el-card>
    
    <!-- 编辑/新增对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑发展历程' : '新增发展历程'" 
      width="600px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="事件标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入事件标题"></el-input>
        </el-form-item>
        <el-form-item label="事件日期" prop="eventDate">
          <el-date-picker
            v-model="form.eventDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="事件描述" prop="content">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="4"
            placeholder="请输入事件详细描述"
          ></el-input>
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-select v-model="form.icon" placeholder="请选择图标" style="width: 100%">
            <el-option 
              v-for="icon in iconOptions" 
              :key="icon.value" 
              :label="icon.label" 
              :value="icon.value"
            >
              <span style="display: flex; align-items: center; gap: 10px;">
                <i :class="['fas', icon.value]" style="width: 20px;"></i>
                <span>{{ icon.label }}</span>
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="节点颜色" prop="color">
          <el-color-picker v-model="form.color" show-alpha></el-color-picker>
          <span class="color-tip">{{ form.color || '#409EFF' }}</span>
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" :max="999"></el-input-number>
          <span class="sort-tip">数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">显示</el-radio>
            <el-radio :label="0">隐藏</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getTimelinesByPage, 
  createTimeline, 
  updateTimeline, 
  deleteTimeline, 
  updateTimelineStatus 
} from '@/api/SiteTimelineApi'

// 状态变量
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const searchFormRef = ref(null)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const timelineList = ref([])

// 搜索表单
const searchForm = reactive({
  title: '',
  status: null
})

// 编辑表单
const form = reactive({
  id: null,
  title: '',
  content: '',
  eventDate: '',
  icon: 'fa-star',
  color: '#409EFF',
  orderNum: 0,
  status: 1
})

// 图标选项
const iconOptions = [
  { label: '星星', value: 'fa-star' },
  { label: '火箭', value: 'fa-rocket' },
  { label: '文档', value: 'fa-file-alt' },
  { label: '评论', value: 'fa-comments' },
  { label: '购物袋', value: 'fa-shopping-bag' },
  { label: '爱心', value: 'fa-heart' },
  { label: '代码', value: 'fa-code' },
  { label: '奖杯', value: 'fa-trophy' },
  { label: '灯泡', value: 'fa-lightbulb' },
  { label: '旗帜', value: 'fa-flag' },
  { label: '齿轮', value: 'fa-cog' },
  { label: '用户', value: 'fa-user' },
  { label: '图片', value: 'fa-image' },
  { label: '链接', value: 'fa-link' },
  { label: '书籍', value: 'fa-book' },
  { label: '日历', value: 'fa-calendar' },
  { label: '闪电', value: 'fa-bolt' },
  { label: '钻石', value: 'fa-gem' }
]

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入事件标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  eventDate: [
    { required: true, message: '请选择事件日期', trigger: 'change' }
  ],
  orderNum: [
    { required: true, message: '请输入排序号', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 获取所有时间线
const fetchTimelines = () => {
  loading.value = true
  getTimelinesByPage({
    title: searchForm.title,
    status: searchForm.status,
    current: currentPage.value,
    size: pageSize.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      timelineList.value = data.records || []
      total.value = data.total || 0
      loading.value = false
    },
    onError: (error) => {
      console.error('获取发展历程失败:', error)
      loading.value = false
    }
  })
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchTimelines()
}

// 重置搜索
const resetSearch = () => {
  searchForm.title = ''
  searchForm.status = null
  currentPage.value = 1
  fetchTimelines()
}

// 分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchTimelines()
}

// 页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchTimelines()
}

// 添加时间线
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
  nextTick(() => {
    resetForm()
  })
}

// 编辑时间线
const handleEdit = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  nextTick(() => {
    resetForm()
    Object.assign(form, {
      id: row.id,
      title: row.title,
      content: row.content,
      eventDate: row.eventDate,
      icon: row.icon || 'fa-star',
      color: row.color || '#409EFF',
      orderNum: row.orderNum,
      status: row.status
    })
  })
}

// 删除时间线
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除事件 "${row.title}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteTimeline(row.id, {
      successMsg: '删除成功',
      onSuccess: () => {
        fetchTimelines()
      },
      onError: (error) => {
        console.error('删除事件失败:', error)
      }
    })
  }).catch(() => {})
}

// 切换状态
const toggleStatus = (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  updateTimelineStatus(row.id, {
    status: newStatus
  }, {
    successMsg: '更新状态成功',
    onSuccess: () => {
      row.status = newStatus
    },
    onError: (error) => {
      console.error('更新状态失败:', error)
    }
  })
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.title = ''
  form.content = ''
  form.eventDate = ''
  form.icon = 'fa-star'
  form.color = '#409EFF'
  form.orderNum = 0
  form.status = 1
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true
      
      const handleSuccess = () => {
        dialogVisible.value = false
        fetchTimelines()
        submitting.value = false
      }
      
      const handleError = (error) => {
        console.error('提交事件失败:', error)
        submitting.value = false
      }
      
      if (isEdit.value) {
        updateTimeline(form.id, form, {
          successMsg: '更新事件成功',
          onSuccess: handleSuccess,
          onError: handleError
        })
      } else {
        createTimeline(form, {
          successMsg: '新增事件成功',
          onSuccess: handleSuccess,
          onError: handleError
        })
      }
    }
  })
}

// 生命周期钩子
onMounted(() => {
  fetchTimelines()
})
</script>

<style scoped>
@import '@/assets/backend-common.css';

.timeline-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 图标预览 */
.icon-preview {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
}

/* 颜色预览 */
.color-preview {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 1px solid #ddd;
}

/* 颜色提示 */
.color-tip {
  margin-left: 12px;
  color: #666;
  font-size: 13px;
}

/* 排序提示 */
.sort-tip {
  margin-left: 12px;
  color: #999;
  font-size: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
