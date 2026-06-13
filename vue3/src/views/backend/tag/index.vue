<template>
  <div class="tag-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>标签管理</span>
          <el-button type="primary" @click="handleAdd">新增标签</el-button>
        </div>
      </template>
      
      <el-table :data="tagList" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" />
        <el-table-column label="标签颜色" width="200">
          <template #default="scope">
            <div class="color-display">
              <DynamicTag
                :label="scope.row.name"
                :bgColor="scope.row.color"
                :textColor="scope.row.textColor"
                :clickable="false"
                size="small"
              />
      
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="articleCount" label="文章数量" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-popconfirm
              title="确定删除该标签吗？"
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
    </el-card>
    
    <!-- 标签表单对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogType === 'add' ? '新增标签' : '编辑标签'"
      width="500px"
    >
      <el-form 
        ref="tagFormRef"
        :model="tagForm"
        :rules="tagRules"
        label-width="80px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="tagForm.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="背景颜色" prop="color">
          <el-color-picker v-model="tagForm.color" color-format="hex" />
        </el-form-item>
        <el-form-item label="文字颜色" prop="textColor">
          <div class="text-color-selector">
            <el-color-picker v-model="tagForm.textColor" color-format="hex" />
            <el-button 
              type="text" 
              size="small" 
              @click="autoSetTextColor"
              style="margin-left: 10px;"
            >
              自动设置
            </el-button>
          </div>
          <div style="margin-top: 8px;">
            <DynamicTag
              :label="tagForm.name || '标签预览'"
              :bgColor="tagForm.color"
              :textColor="tagForm.textColor"
              :clickable="false"
              size="small"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { getAllTags, createTag, updateTag, deleteTag } from '@/api/TagApi'
import { ElMessage } from 'element-plus'
import DynamicTag from '@/components/common/DynamicTag.vue'

// 加载状态
const loading = ref(false)
// 标签列表
const tagList = ref([])
// 对话框可见性
const dialogVisible = ref(false)
// 对话框类型（add / edit）
const dialogType = ref('add')
// 表单引用
const tagFormRef = ref(null)
// 表单数据
const tagForm = reactive({
  id: null,
  name: '',
  color: '#409EFF',
  textColor: '#FFFFFF'
})
// 表单验证规则
const tagRules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  color: [
    { required: true, message: '请选择标签背景颜色', trigger: 'change' }
  ],
  textColor: [
    { required: true, message: '请选择标签文字颜色', trigger: 'change' }
  ]
}

// 初始化
onMounted(() => {
  fetchTagList()
})

// 获取标签列表
const fetchTagList = async () => {
  loading.value = true
  
  getAllTags({
    showDefaultMsg: false,
    onSuccess: (data) => {
      tagList.value = data
      loading.value = false
    },
    onError: (error) => {
      console.error('获取标签列表失败:', error)
      loading.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  tagForm.id = null
  tagForm.name = ''
  tagForm.color = '#409EFF'
  tagForm.textColor = '#FFFFFF'
  
  // 等待下一个tick后清除验证状态，确保DOM已更新
  nextTick(() => {
    if (tagFormRef.value) {
      tagFormRef.value.resetFields()
    }
  })
}

// 规范化为标准六位十六进制颜色（#RRGGBB）
const normalizeHex = (value) => {
  if (!value) return ''
  let v = String(value).trim()
  // 仅保留十六进制，移除#
  if (v.startsWith('#')) v = v.slice(1)
  // 3位转6位
  if (/^[0-9a-fA-F]{3}$/.test(v)) {
    v = v.split('').map(c => c + c).join('')
  }
  // 截断为6位并大写
  if (/^[0-9a-fA-F]{6}$/.test(v)) {
    return '#' + v.toUpperCase()
  }
  // 非法输入兜底：尝试取前6位合法字符，不足补0
  v = (v.match(/[0-9a-fA-F]/g) || []).join('').slice(0, 6).padEnd(6, '0')
  return '#' + v.toUpperCase()
}

// 处理取消操作
const handleCancel = () => {
  dialogVisible.value = false
  resetForm()
}

// 处理添加标签
const handleAdd = () => {
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 处理编辑标签
const handleEdit = (row) => {
  dialogType.value = 'edit'
  
  // 填充表单数据
  tagForm.id = row.id
  tagForm.name = row.name
  tagForm.color = row.color
  tagForm.textColor = row.textColor || '#FFFFFF'
  
  dialogVisible.value = true
}

// 处理删除标签
const handleDelete = async (row) => {
  deleteTag(row.id, {
    successMsg: '删除成功',
    onSuccess: () => {
      fetchTagList()
    },
    onError: (error) => {
      console.error('删除标签失败:', error)
    }
  })
}

// 自动设置文字颜色
const autoSetTextColor = () => {
  if (!tagForm.color) {
    tagForm.textColor = '#FFFFFF'
    return
  }

  // 解析十六进制颜色
  let color = tagForm.color.replace('#', '')
  if (color.length === 3) {
    // 将3位颜色转换为6位
    color = color.charAt(0) + color.charAt(0) +
            color.charAt(1) + color.charAt(1) +
            color.charAt(2) + color.charAt(2)
  }

  try {
    // 计算亮度
    const r = parseInt(color.substring(0, 2), 16)
    const g = parseInt(color.substring(2, 4), 16)
    const b = parseInt(color.substring(4, 6), 16)

    // 使用相对亮度公式计算
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255

    // 亮度大于0.5使用深色文字，否则使用白色文字
    tagForm.textColor = luminance > 0.5 ? '#2D3748' : '#FFFFFF'
  } catch (e) {
    // 解析失败时设置默认深色文字
    tagForm.textColor = '#2D3748'
  }
}

// 提交表单
const submitForm = () => {
  tagFormRef.value.validate(async (valid) => {
    if (valid) {
      if (dialogType.value === 'add') {
        // 新增标签
        const payload = {
          name: tagForm.name,
          color: normalizeHex(tagForm.color),
          textColor: normalizeHex(tagForm.textColor)
        }
        createTag(payload, {
          successMsg: '添加成功',
          onSuccess: () => {
            dialogVisible.value = false
            resetForm() // 重置表单
            fetchTagList()
          },
          onError: (error) => {
            console.error('添加标签失败:', error)
          }
        })
      } else {
        // 更新标签
        const payload = {
          name: tagForm.name,
          color: normalizeHex(tagForm.color),
          textColor: normalizeHex(tagForm.textColor)
        }
        updateTag(tagForm.id, payload, {
          successMsg: '更新成功',
          onSuccess: () => {
            dialogVisible.value = false
            resetForm() // 重置表单
            fetchTagList()
          },
          onError: (error) => {
            console.error('更新标签失败:', error)
          }
        })
      }
    }
  })
}
</script>

<style scoped>
@import '@/assets/backend-common.css';

.tag-container {
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

.color-display {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 12px;
}

.color-preview {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  min-width: 60px;
  text-align: center;
}



.text-color-selector {
  display: flex;
  align-items: center;
}

.color-preview-demo {
  font-weight: 500;
  text-align: center;
  transition: all 0.2s ease;
}

.color-preview {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  margin-right: 8px;
  vertical-align: middle;
  border: 1px solid rgba(0, 0, 0, 0.1);
}


:deep(.el-color-picker__trigger) {
  border-color: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

:deep(.el-color-picker__trigger:hover) {
  border-color: #333;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style> 