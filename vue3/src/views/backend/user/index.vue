<template>
  <div class="user-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd">添加用户</el-button>
        </div>
      </template>
      
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable></el-input>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable></el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.roleCode" placeholder="请选择角色" clearable>
            <el-option label="管理员" value="ADMIN"></el-option>
            <el-option label="普通用户" value="USER"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchUsers">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 用户表格 -->
      <el-table v-loading="loading" :data="tableData" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="username" label="用户名" width="120"></el-table-column>
        <el-table-column prop="name" label="姓名" width="120"></el-table-column>
        <el-table-column prop="email" label="邮箱"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="120"></el-table-column>
        <el-table-column prop="roleCode" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.roleCode === 'ADMIN' ? 'danger' : 'success'">
              {{ scope.row.roleCode === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sex" label="性别" width="80"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        ></el-pagination>
      </div>
      
      <!-- 用户表单对话框 -->
      <el-dialog v-model="dialogVisible" :title="dialogType === 'add' ? '添加用户' : '编辑用户'">
        <el-form ref="userFormRef" :model="userForm" :rules="rules" label-width="100px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userForm.username" :disabled="dialogType === 'edit'"></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password" v-if="dialogType === 'add'">
            <el-input v-model="userForm.password" type="password"></el-input>
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="userForm.name"></el-input>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userForm.email"></el-input>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userForm.phone"></el-input>
          </el-form-item>
          <el-form-item label="性别" prop="sex">
            <el-radio-group v-model="userForm.sex">
              <el-radio label="男">男</el-radio>
              <el-radio label="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="角色" prop="roleCode">
            <el-select v-model="userForm.roleCode" placeholder="请选择角色">
              <el-option label="管理员" value="ADMIN"></el-option>
              <el-option label="普通用户" value="USER"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0"></el-switch>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitForm">确认</el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getUsersPage, 
  registerUser, 
  updateUser, 
  deleteUser, 
  updateUserStatus 
} from '@/api/user'

// 搜索表单
const searchForm = reactive({
  username: '',
  name: '',
  roleCode: ''
})

// 表格数据
const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 对话框相关
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const userFormRef = ref(null)

// 用户表单
const userForm = reactive({
  id: '',
  username: '',
  password: '',
  name: '',
  email: '',
  phone: '',
  sex: '男',
  roleCode: 'USER',
  status: 1
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在3到50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度必须在6到100个字符之间', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 初始化
onMounted(() => {
  fetchUsers()
})

// 获取用户列表
const fetchUsers = () => {
  loading.value = true
  getUsersPage({
    currentPage: currentPage.value,
    size: pageSize.value,
    ...searchForm
  }, {
    onSuccess: (res) => {
      tableData.value = res.records || []
      total.value = res.total || 0
      loading.value = false
    },
    onError: (error) => {
      console.error('获取用户列表失败:', error)
      loading.value = false
    }
  })
}

// 重置搜索条件
const resetSearch = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  fetchUsers()
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchUsers()
}

// 处理每页数量变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchUsers()
}

// 添加用户
const handleAdd = () => {
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  dialogType.value = 'edit'
  Object.keys(userForm).forEach(key => {
    if (key in row) {
      userForm[key] = row[key]
    }
  })
  dialogVisible.value = true
}

// 删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteUser(row.id, {
      successMsg: '删除成功',
      onSuccess: () => {
        fetchUsers()
      },
      onError: (error) => {
        console.error('删除用户失败:', error)
      }
    })
  }).catch(() => {})
}

// 修改用户状态
const handleStatusChange = (row) => {
  updateUserStatus(row.id, { status: row.status }, {
    successMsg: `用户${row.status === 1 ? '启用' : '禁用'}成功`,
    onSuccess: () => {
      // 状态更新成功
    },
    onError: (error) => {
      console.error('修改用户状态失败:', error)
      // 状态修改失败，恢复原状态
      row.status = row.status === 1 ? 0 : 1
    }
  })
}

// 重置表单
const resetForm = () => {
  userForm.id = ''
  userForm.username = ''
  userForm.password = ''
  userForm.name = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.sex = '男'
  userForm.roleCode = 'USER'
  userForm.status = 1
  
  // 重置表单验证
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
}

// 提交表单
const submitForm = () => {
  userFormRef.value.validate((valid) => {
    if (valid) {
      if (dialogType.value === 'add') {
        // 添加用户
        registerUser(userForm, {
          successMsg: '添加成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchUsers()
          },
          onError: (error) => {
            console.error('添加用户失败:', error)
          }
        })
      } else {
        // 编辑用户
        updateUser(userForm.id, userForm, {
          successMsg: '更新成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchUsers()
          },
          onError: (error) => {
            console.error('更新用户失败:', error)
          }
        })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.user-management {
  padding: 24px;
  
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
  
  .box-card {
    margin-bottom: 24px;
    border-radius: 8px;
    border: 1px solid #eaeaea;
    box-shadow: none;
  }
  
  .search-form {
    margin-bottom: 24px;
  }
  
  .pagination-container {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
  }
}

:deep(.el-card__header) {
  padding: 0 20px;
}

:deep(.el-card__body) {
  padding: 24px;
}

:deep(.el-form-item__label) {
  color: #333;
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  border-radius: 4px;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #333;
}

:deep(.el-select .el-input__wrapper) {
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1);
}

:deep(.el-select .el-input__wrapper:hover),
:deep(.el-select .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #333;
}

:deep(.el-button) {
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.3s;
}

:deep(.el-button--primary) {
  background-color: #333;
  border-color: #333;
}

:deep(.el-button--primary:hover),
:deep(.el-button--primary:focus) {
  background-color: #000;
  border-color: #000;
}

:deep(.el-button--danger) {
  background-color: #f56c6c;
  border-color: #f56c6c;
}

:deep(.el-button--danger:hover),
:deep(.el-button--danger:focus) {
  background-color: #d33030;
  border-color: #d33030;
}

:deep(.el-table) {
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #eaeaea;
}

:deep(.el-table th) {
  background-color: #f8f8f8;
  font-weight: 500;
  color: #333;
  border-bottom: 1px solid #eaeaea;
}

:deep(.el-table td) {
  border-bottom: 1px solid #eaeaea;
}

:deep(.el-table--border .el-table__cell) {
  border-right: 1px solid #eaeaea;
}

:deep(.el-table__row:hover td) {
  background-color: #f8f9fa;
}

:deep(.el-pagination .el-pagination__total) {
  color: #666;
}

:deep(.el-pagination .el-pagination__jump) {
  color: #666;
}

:deep(.el-pagination .el-input__wrapper) {
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1);
}

:deep(.el-pagination .el-input__wrapper:hover),
:deep(.el-pagination .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #333;
}

:deep(.el-pagination .el-pagination__sizes .el-input .el-input__wrapper) {
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1);
}

:deep(.el-pagination .el-pagination__sizes .el-input .el-input__wrapper:hover),
:deep(.el-pagination .el-pagination__sizes .el-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #333;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  background-color: transparent;
  color: #666;
}

:deep(.el-pagination .el-pager li) {
  background-color: transparent;
  color: #666;
  border-radius: 4px;
  font-weight: 400;
}

:deep(.el-pagination .el-pager li.is-active) {
  background-color: #333;
  color: #fff;
  font-weight: 500;
}

:deep(.el-pagination .el-pager li:hover) {
  color: #333;
}

:deep(.el-tag) {
  border: none;
  border-radius: 4px;
}

:deep(.el-tag--danger) {
  background-color: rgba(245, 108, 108, 0.1);
  border-color: transparent;
  color: #f56c6c;
}

:deep(.el-tag--success) {
  background-color: rgba(103, 194, 58, 0.1);
  border-color: transparent;
  color: #67c23a;
}

:deep(.el-switch__core) {
  border-color: rgba(0, 0, 0, 0.1);
}

:deep(.el-switch.is-checked .el-switch__core) {
  background-color: #333;
  border-color: #333;
}

/* 对话框样式 */
:deep(.el-dialog) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: none;
  border: 1px solid #eaeaea;
}

:deep(.el-dialog__header) {
  padding: 16px 20px;
  margin: 0;
  background-color: #f8f8f8;
  border-bottom: 1px solid #eaeaea;
}

:deep(.el-dialog__title) {
  color: #333;
  font-weight: 500;
  font-size: 16px;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  padding: 16px 20px;
  border-top: 1px solid #eaeaea;
}

:deep(.el-dialog__close) {
  color: #666;
}

:deep(.el-dialog__close:hover) {
  color: #333;
  background-color: #f0f0f0;
  border-radius: 50%;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: #333;
  border-color: #333;
}

:deep(.el-radio__input.is-focus .el-radio__inner) {
  border-color: #333;
}

:deep(.el-radio__inner:hover) {
  border-color: #333;
}

:deep(.el-radio__label) {
  color: #666;
}

:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #333;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style> 