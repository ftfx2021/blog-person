<template>
  <div class="blog-config-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>博客基本设置</span>
          <el-button type="primary" @click="saveConfig" :loading="saving">保存设置</el-button>
        </div>
      </template>
      
      <el-form :model="configForm" label-width="120px" v-loading="loading">
        <el-form-item label="博客名称">
          <el-input v-model="configForm.blog_name" placeholder="请输入博客名称"></el-input>
        </el-form-item>
        
        <el-form-item label="博客描述">
          <el-input v-model="configForm.blog_description" type="textarea" placeholder="请输入博客描述"></el-input>
        </el-form-item>
        
        <el-form-item label="博客作者">
          <el-input v-model="configForm.blog_author" placeholder="请输入博客作者"></el-input>
        </el-form-item>
        
        <el-form-item label="页脚信息">
          <el-input v-model="configForm.blog_footer" type="textarea" placeholder="请输入页脚信息"></el-input>
        </el-form-item>
        
        <el-form-item label="ICP备案号">
          <el-input v-model="configForm.blog_icp" placeholder="请输入ICP备案号（选填）"></el-input>
        </el-form-item>
        
        <el-form-item label="公安备案号">
          <el-input v-model="configForm.blog_gongan" placeholder="请输入公安备案号（选填）"></el-input>
        </el-form-item>
        
        <el-form-item label="SEO关键词">
          <el-input v-model="configForm.blog_keywords" type="textarea" placeholder="请输入SEO关键词，多个关键词用英文逗号分隔"></el-input>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 高级设置卡片 -->
    <el-card class="box-card mt-20">
      <template #header>
        <div class="card-header">
          <span>高级设置</span>
          <el-button type="primary" @click="addConfigDialogVisible = true">添加配置</el-button>
        </div>
      </template>
      
      <el-table :data="advancedConfigList" border style="width: 100%">
        <el-table-column prop="key" label="配置键" width="180"></el-table-column>
        <el-table-column prop="value" label="配置值"></el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button 
              type="primary" 
              link
              @click="editConfig(scope.row)"
            >
              编辑
            </el-button>
            <el-button 
              type="danger" 
              link
              @click="confirmDeleteConfig(scope.row.key)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 添加配置对话框 -->
    <el-dialog v-model="addConfigDialogVisible" title="添加配置" width="500px">
      <el-form :model="newConfig" label-width="80px" :rules="configRules" ref="addConfigFormRef">
        <el-form-item label="配置键" prop="key">
          <el-input v-model="newConfig.key" placeholder="请输入配置键"></el-input>
        </el-form-item>
        <el-form-item label="配置值" prop="value">
          <el-input v-model="newConfig.value" type="textarea" placeholder="请输入配置值"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addConfigDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAddConfig" :loading="addConfigLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 编辑配置对话框 -->
    <el-dialog v-model="editConfigDialogVisible" title="编辑配置" width="500px">
      <el-form :model="editingConfig" label-width="80px" :rules="configRules" ref="editConfigFormRef">
        <el-form-item label="配置键" prop="key">
          <el-input v-model="editingConfig.key" placeholder="请输入配置键" disabled></el-input>
        </el-form-item>
        <el-form-item label="配置值" prop="value">
          <el-input v-model="editingConfig.value" type="textarea" placeholder="请输入配置值"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editConfigDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitEditConfig" :loading="editConfigLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getAllBlogConfigs,
  updateBlogConfigs,
  addBlogConfig,
  deleteBlogConfig
} from '@/api/BlogConfigApi'

// 状态变量
const loading = ref(false)
const saving = ref(false)
const addConfigDialogVisible = ref(false)
const editConfigDialogVisible = ref(false)
const addConfigLoading = ref(false)
const editConfigLoading = ref(false)
const addConfigFormRef = ref(null)
const editConfigFormRef = ref(null)

// 表单数据
const configForm = reactive({
  blog_name: '',
  blog_description: '',
  blog_author: '',
  blog_footer: '',
  blog_icp: '',
  blog_gongan: '',
  blog_keywords: ''
})

// 配置列表
const allConfigs = ref({})
const advancedConfigList = computed(() => {
  const basicKeys = ['blog_name', 'blog_description', 'blog_author', 'blog_footer', 'blog_icp', 'blog_gongan', 'blog_keywords']
  const result = []
  
  for (const key in allConfigs.value) {
    if (!basicKeys.includes(key)) {
      result.push({
        key,
        value: allConfigs.value[key]
      })
    }
  }
  
  return result
})

// 新配置
const newConfig = reactive({
  key: '',
  value: ''
})

// 编辑中的配置
const editingConfig = reactive({
  key: '',
  value: ''
})

// 验证规则
const configRules = {
  key: [
    { required: true, message: '请输入配置键', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  value: [
    { required: true, message: '请输入配置值', trigger: 'blur' }
  ]
}

// 获取所有配置
const fetchAllConfigs = async () => {
  loading.value = true
  try {
    getAllBlogConfigs({
      showDefaultMsg: false,
      onSuccess: (data) => {
        allConfigs.value = data
        
        // 填充表单
        for (const key in configForm) {
          if (data[key]) {
            configForm[key] = data[key]
          }
        }
      },
      onError: (error) => {
        console.error('获取配置失败:', error)
      }
    })
  } finally {
    loading.value = false
  }
}

// 保存配置
const saveConfig = async () => {
  saving.value = true
  try {
    // 按照后端DTO结构包装数据
    const configData = {
      configMap: { ...configForm }
    }
    
    updateBlogConfigs(configData, {
      successMsg: '保存配置成功',
      onSuccess: () => {
        fetchAllConfigs()
      },
      onError: (error) => {
        console.error('保存配置失败:', error)
      }
    })
  } finally {
    saving.value = false
  }
}

// 打开编辑配置对话框
const editConfig = (row) => {
  editingConfig.key = row.key
  editingConfig.value = row.value
  editConfigDialogVisible.value = true
}

// 提交编辑配置
const submitEditConfig = async () => {
  editConfigFormRef.value.validate(async (valid) => {
    if (valid) {
      editConfigLoading.value = true
      try {
        const configMap = {}
        configMap[editingConfig.key] = editingConfig.value
        
        // 按照后端DTO结构包装数据
        const configData = {
          configMap: configMap
        }
        
        updateBlogConfigs(configData, {
          successMsg: '更新配置成功',
          onSuccess: () => {
            editConfigDialogVisible.value = false
            fetchAllConfigs()
          },
          onError: (error) => {
            console.error('更新配置失败:', error)
          }
        })
      } finally {
        editConfigLoading.value = false
      }
    }
  })
}

// 提交添加配置
const submitAddConfig = async () => {
  addConfigFormRef.value.validate(async (valid) => {
    if (valid) {
      addConfigLoading.value = true
      try {
        addBlogConfig(newConfig.key, newConfig.value, {
          successMsg: '添加配置成功',
          onSuccess: () => {
            addConfigDialogVisible.value = false
            fetchAllConfigs()
            
            // 重置表单
            newConfig.key = ''
            newConfig.value = ''
            if (addConfigFormRef.value) {
              addConfigFormRef.value.resetFields()
            }
          },
          onError: (error) => {
            console.error('添加配置失败:', error)
          }
        })
      } finally {
        addConfigLoading.value = false
      }
    }
  })
}

// 确认删除配置
const confirmDeleteConfig = (key) => {
  ElMessageBox.confirm('确定要删除该配置吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteConfigItem(key)
  }).catch(() => {})
}

// 删除配置
const deleteConfigItem = async (key) => {
  try {
    deleteBlogConfig(key, {
      successMsg: '删除配置成功',
      onSuccess: () => {
        fetchAllConfigs()
      },
      onError: (error) => {
        console.error('删除配置失败:', error)
      }
    })
  } catch (error) {
    console.error('删除配置失败:', error)
  }
}

// 生命周期钩子
onMounted(() => {
  fetchAllConfigs()
})
</script>

<style scoped>
@import '@/assets/backend-common.css';

.blog-config-container {
  padding: 24px;
}

.box-card {
  margin-bottom: 24px;
  border-radius: 8px;
}

.mt-20 {
  margin-top: 24px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>