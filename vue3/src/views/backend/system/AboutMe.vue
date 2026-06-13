no<template>
  <div class="about-me-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>关于我页面设置</span>
          <el-button type="primary" @click="saveAboutMe" :loading="saving">保存设置</el-button>
        </div>
      </template>
      
      <div v-loading="loading">
        <!-- 个人信息卡片 -->
        <h3>个人基本信息</h3>
        <el-form :model="form" label-width="120px">
          <el-form-item label="个人头像">
            <el-upload
              class="avatar-uploader"
              action="#"
              :auto-upload="true"
              :show-file-list="false"
              :http-request="customUploadAvatar"
              :before-upload="beforeAvatarUpload"
            >
              <el-image 
                v-if="userStore.userInfo?.avatar" 
                :src="getFileUrl(userStore.userInfo?.avatar)" 
                class="avatar" 
              />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">点击上传头像，建议尺寸：200×200px</div>
          </el-form-item>
          
          <el-form-item label="昵称">
            <el-input v-model="form.about_name" placeholder="请输入昵称"></el-input>
          </el-form-item>
          
          <el-form-item label="职业">
            <el-input v-model="form.about_job" placeholder="请输入您的职业，如：全栈开发工程师"></el-input>
          </el-form-item>
          
          <el-form-item label="个人简介">
            <el-input 
              v-model="form.about_intro" 
              type="textarea" 
              :rows="3" 
              placeholder="请输入个人简介，简单介绍自己"
            ></el-input>
          </el-form-item>
          
          <el-form-item label="关键词标签">
            <el-input 
              v-model="form.author_keyword" 
              type="textarea" 
              :rows="2" 
              placeholder="请输入关键词标签，使用逗号分隔，如：数码科技爱好者,分享与热心帮助,智能家居小能手"
            ></el-input>
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>关键词将在"关于博主"页面以彩色标签形式展示，建议3-5个关键词</span>
            </div>
            <!-- 关键词预览 -->
            <div class="keywords-preview" v-if="keywordsList.length > 0">
              <div class="preview-label">预览效果：</div>
              <div class="preview-tags">
                <div 
                  v-for="(keyword, index) in keywordsList" 
                  :key="index"
                  class="keyword-tag-preview"
                >
                  {{ keyword }}
                </div>
              </div>
            </div>
          </el-form-item>
        </el-form>
        
        <!-- 内容编辑器 -->
        <h3>个人详细介绍</h3>
        <div class="editor-container">
          <v-md-editor v-model="form.about_content" height="500px"></v-md-editor>
        </div>
        
        <!-- 社交信息 -->
        <h3>社交链接</h3>
        <el-form :model="form" label-width="120px">
          <el-form-item label="GitHub">
            <el-input v-model="form.social_github" placeholder="请输入GitHub主页链接"></el-input>
          </el-form-item>
          
          <el-form-item label="微博">
            <el-input v-model="form.social_weibo" placeholder="请输入微博主页链接"></el-input>
          </el-form-item>
          
          <el-form-item label="知乎">
            <el-input v-model="form.social_zhihu" placeholder="请输入知乎主页链接"></el-input>
          </el-form-item>
          
          <el-form-item label="邮箱">
            <el-input v-model="form.social_email" placeholder="请输入邮箱地址"></el-input>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getAllBlogConfigs, updateBlogConfigs } from '@/api/BlogConfigApi'
import { updateUser } from '@/api/user'
import { uploadToTemp } from '@/api/fileManagement'
import { getFileUrl } from '@/utils/fileUtils'

// 状态变量
const loading = ref(false)
const saving = ref(false)
const userStore = useUserStore()

// 表单数据
const form = reactive({
  about_name: '',
  about_job: '',
  about_intro: '',
  about_content: '',
  author_keyword: '',
  social_github: '',
  social_weibo: '',
  social_zhihu: '',
  social_email: ''
})

// 获取关于我的配置
const fetchAboutMe = () => {
  loading.value = true
  getAllBlogConfigs({
    showDefaultMsg: false,
    onSuccess: (data) => {
      // 填充表单
      for (const key in form) {
        if (data[key]) {
          form[key] = data[key]
        }
      }
      loading.value = false
    },
    onError: (error) => {
      console.error('获取关于我配置失败:', error)
      loading.value = false
    }
  })
}

// 保存关于我配置
const saveAboutMe = () => {
  saving.value = true
  
  // 构造符合后端期望的数据格式：{ configMap: {...} }
  const configData = {
    configMap: {
      ...form
    }
  }
  
  updateBlogConfigs(configData, {
    successMsg: '保存配置成功',
    onSuccess: () => {
      fetchAboutMe()
      saving.value = false
    },
    onError: (error) => {
      console.error('保存关于我配置失败:', error)
      saving.value = false
    }
  })
}

// 上传头像前的校验
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('头像只能是JPG或PNG格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过2MB!')
    return false
  }
  return true
}

// 自定义上传头像
const customUploadAvatar = (options) => {
  const { file } = options
  const userId = userStore.userInfo.id
  
  // 新系统：上传到临时目录
  uploadToTemp(file, {
    successMsg: '头像上传成功',
    onSuccess: (tempUrl) => {
      // 直接使用临时URL更新用户头像
      updateUserAvatar(tempUrl).then(() => {
        options.onSuccess(tempUrl)
      }).catch((error) => {
        options.onError(error)
      })
    },
    onError: (error) => {
      options.onError(error)
    }
  })
}

// 更新用户头像
const updateUserAvatar = (avatarPath) => {
  return new Promise((resolve, reject) => {
    const userId = userStore.userInfo.id
    updateUser(userId, { avatar: avatarPath }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        // 更新本地用户信息
        const updatedUserInfo = { ...userStore.userInfo, avatar: avatarPath }
        userStore.updateUserInfo(updatedUserInfo)
        resolve(data)
      },
      onError: (error) => {
        console.error('头像信息保存失败', error)
        ElMessage.error('头像信息保存失败')
        reject(error)
      }
    })
  })
}

// 关键词列表 - 用于预览
const keywordsList = computed(() => {
  if (!form.author_keyword) return []
  return form.author_keyword.split(',').map(k => k.trim()).filter(k => k)
})

// 生命周期钩子
onMounted(() => {
  fetchAboutMe()
})
</script>

<style scoped>
@import '@/assets/backend-common.css';

.about-me-container {
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

:deep(.el-card__header) {
  padding: 0 20px;
}

h3 {
  margin-top: 28px;
  margin-bottom: 18px;
  font-weight: 500;
  color: #333;
  border-left: 3px solid #333;
  padding-left: 12px;
  font-size: 16px;
}

.editor-container {
  margin-bottom: 24px;
  border-radius: 4px;
  overflow: hidden;
}

:deep(.v-md-editor) {
  border-radius: 4px;
  border-color: #eaeaea !important;
}

:deep(.v-md-editor__toolbar) {
  border-bottom-color: #eaeaea !important;
}

/* 上传样式 */
.avatar-uploader {
  width: 150px;
  height: 150px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  display: inline-block;
  transition: all 0.3s;
}

.avatar-uploader:hover {
  border-color: #333;
  background-color: #f8f8f8;
}

.avatar {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #666;
  width: 150px;
  height: 150px;
  line-height: 150px;
  text-align: center;
}

.upload-tip {
  font-size: 12px;
  color: #666;
  margin-top: 8px;
}

/* 表单提示 */
.form-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.form-tip .el-icon {
  font-size: 14px;
  color: #409eff;
}

/* 关键词预览 */
.keywords-preview {
  margin-top: 16px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.preview-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  font-weight: 500;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.keyword-tag-preview {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
  cursor: default;
}

.keyword-tag-preview:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}
</style> 