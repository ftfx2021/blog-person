<template>
  <div class="category-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>分类管理</span>
          <el-button type="primary" @click="handleAdd">新增分类</el-button>
        </div>
      </template>
      
      <el-table :data="categoryList" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="description" label="分类描述" show-overflow-tooltip />
        <el-table-column label="主色调" width="100">
          <template #default="scope">
            <div class="color-display">
              <div class="color-dot" :style="{ backgroundColor: scope.row.mainColor || '#409EFF' }"></div>
              <span class="color-text">{{ scope.row.mainColor || '#409EFF' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="parentId" label="父分类ID" width="100" />
        <el-table-column prop="orderNum" label="排序" width="80" />
        <el-table-column prop="articleCount" label="文章数量" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-popconfirm
              title="确定删除该分类吗？"
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
    
    <!-- 分类表单对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogType === 'add' ? '新增分类' : '编辑分类'"
      width="500px"
    >
      <el-form 
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        label-width="80px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="categoryForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
          />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="categoryForm.parentId" placeholder="请选择父分类" style="width: 100%">
            <el-option :value="0" label="无（顶级分类）" />
            <el-option 
              v-for="item in parentOptions" 
              :key="item.id" 
              :label="item.name" 
              :value="item.id"
              :disabled="dialogType === 'edit' && categoryForm.id === item.id"
            />
          </el-select>
          <div class="form-tip">
            <el-text type="info" size="small">
              <el-icon><InfoFilled /></el-icon>
              系统最多支持2级分类，只能选择一级分类作为父分类
            </el-text>
          </div>
        </el-form-item>
        <el-form-item label="主色调">
          <el-color-picker 
            v-model="categoryForm.mainColor" 
            color-format="hex" 
            :predefine="predefineColors" 
          />
          <div class="form-tip">
            <el-text type="info" size="small">
              <el-icon><InfoFilled /></el-icon>
              设置分类的主色调，新文章将默认使用此颜色
            </el-text>
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="categoryForm.orderNum" :min="0" :max="99" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue'
import { 
  getAllCategories, 
  createCategory, 
  updateCategory, 
  deleteCategory 
} from '@/api/CategoryApi'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'

// 加载状态
const loading = ref(false)
// 分类列表
const categoryList = ref([])
// 对话框可见性
const dialogVisible = ref(false)
// 对话框类型（add / edit）
const dialogType = ref('add')
// 表单引用
const categoryFormRef = ref(null)
// 预定义颜色
const predefineColors = [
  '#409EFF', // Element Plus 蓝色
  '#67C23A', // 绿色
  '#E6A23C', // 橙色
  '#F56C6C', // 红色
  '#909399', // 灰色
  '#FF6B6B', // 亮红
  '#4ECDC4', // 青色
  '#45B7D1', // 天蓝
  '#FFA07A', // 浅橙
  '#98D8C8', // 薄荷绿
  '#F7DC6F', // 黄色
  '#BB8FCE', // 紫色
]

// 表单数据
const categoryForm = reactive({
  id: null,
  name: '',
  description: '',
  mainColor: '#409EFF',
  parentId: 0,
  orderNum: 0
})
// 表单验证规则
const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' },
    { pattern: /^[^\s].*[^\s]$|^[^\s]$/, message: '分类名称不能以空格开头或结尾', trigger: 'blur' }
  ],
  description: [
    { max: 200, message: '长度不能超过 200 个字符', trigger: 'blur' }
  ],
  orderNum: [
    { required: true, message: '请输入排序号', trigger: 'blur' },
    { type: 'number', min: 0, max: 99, message: '排序号必须在 0 到 99 之间', trigger: 'blur' }
  ]
}

// 父分类选项，只显示一级分类（parentId为0的分类），并排除当前编辑的分类
const parentOptions = computed(() => {
  // 只显示一级分类作为父分类选项
  let options = categoryList.value.filter(item => item.parentId === 0)
  
  if (dialogType.value === 'edit') {
    // 编辑时排除当前分类本身
    options = options.filter(item => item.id !== categoryForm.id)
  }
  
  return options
})

// 监听对话框状态，确保表单正确重置
watch(dialogVisible, (newVal) => {
  if (newVal && dialogType.value === 'add') {
    // 当新增对话框打开时，确保表单被正确重置
    nextTick(() => {
      if (categoryFormRef.value) {
        categoryFormRef.value.clearValidate()
      }
    })
  }
})

// 初始化
onMounted(() => {
  fetchCategoryList()
})

// 获取分类列表
const fetchCategoryList = () => {
  loading.value = true
  getAllCategories({
    showDefaultMsg: false,
    onSuccess: (data) => {
      categoryList.value = data
      loading.value = false
    },
    onError: (error) => {
      console.error('获取分类列表失败:', error)
      loading.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  // 先重置表单验证状态
  if (categoryFormRef.value) {
    categoryFormRef.value.resetFields()
  }
  
  // 使用 Object.assign 重置 reactive 对象到初始状态
  Object.assign(categoryForm, {
    id: null,
    name: '',
    description: '',
    mainColor: '#409EFF',
    parentId: 0,
    orderNum: 0
  })
}

// 处理添加分类
const handleAdd = () => {
  dialogType.value = 'add'
  dialogVisible.value = true
  
  // 在下一个 tick 中重置表单，确保对话框已经打开且表单已渲染
  nextTick(() => {
    resetForm()
  })
}

// 处理编辑分类
const handleEdit = (row) => {
  dialogType.value = 'edit'
  dialogVisible.value = true
  
  // 在下一个 tick 中处理表单数据，确保对话框已经打开
  nextTick(() => {
    resetForm()
    
    // 填充表单数据
    categoryForm.id = row.id
    categoryForm.name = row.name
    categoryForm.description = row.description
    categoryForm.mainColor = row.mainColor || '#409EFF'
    categoryForm.parentId = row.parentId
    categoryForm.orderNum = row.orderNum
  })
}

// 处理删除分类
const handleDelete = (row) => {
  deleteCategory(row.id, {
    successMsg: '删除成功',
    onSuccess: () => {
      fetchCategoryList()
    },
    onError: (error) => {
      console.error('删除分类失败:', error)
    }
  })
}

// 验证分类层级限制
const validateCategoryLevel = () => {
  // 如果选择了父分类（不是顶级分类）
  if (categoryForm.parentId && categoryForm.parentId !== 0) {
    // 检查选择的父分类是否已经是二级分类
    const selectedParent = categoryList.value.find(item => item.id === categoryForm.parentId)
    if (selectedParent && selectedParent.parentId !== 0) {
      ElMessage.error('不能在二级分类下创建子分类，系统最多支持2级分类')
      return false
    }
  }
  return true
}

// 提交表单
const submitForm = () => {
  categoryFormRef.value.validate((valid) => {
    if (valid) {
      // 验证分类层级限制
      if (!validateCategoryLevel()) {
        return
      }
      
      if (dialogType.value === 'add') {
        // 新增分类
        const { id, ...createData } = categoryForm
        createCategory(createData, {
          successMsg: '添加成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchCategoryList()
          },
          onError: (error) => {
            console.error('保存分类失败:', error)
          }
        })
      } else {
        // 更新分类
        const { id, ...updateData } = categoryForm
        updateCategory(categoryForm.id, updateData, {
          successMsg: '更新成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchCategoryList()
          },
          onError: (error) => {
            console.error('保存分类失败:', error)
          }
        })
      }
    }
  })
}
</script>

<style scoped>
@import '@/assets/backend-common.css';

.category-container {
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

:deep(.el-table) {
  margin-top: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-tip {
  margin-top: 4px;
}

.form-tip .el-text {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 颜色显示样式 */
.color-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  flex-shrink: 0;
}

.color-text {
  font-size: 12px;
  color: #606266;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}
</style> 