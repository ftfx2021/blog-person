<template>
  <div class="article-edit-container">
    <!-- AI智能助手 -->
    <ArticleAiAssistant
      :content="currentContent"
      :title="articleForm.title"
      @update:title="updateTitle"
      @update:summary="updateSummary"
      @update:outline="updateOutline"
      @update:tags="updateTags"
      @update:categoryId="updateCategoryId"
    />
    
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑文章' : '写文章' }}</span>
          <div class="action-buttons">
            <el-button @click="$router.push('/back/article')">返回列表</el-button>
            <el-button type="success" @click="triggerMarkdownImport">
              <el-icon><Upload /></el-icon>
              导入MD文件
            </el-button>
            <el-button type="info" @click="saveArticle(0)">保存草稿</el-button>
            <el-button type="primary" @click="saveArticle(1)">发布文章</el-button>
          </div>
        </div>
      </template>
      
      <!-- 文章表单 -->
      <el-form
        ref="articleFormRef"
        :model="articleForm"
        :rules="articleRules"
        label-width="100px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="articleForm.title" placeholder="请输入文章标题" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="articleForm.categoryId" placeholder="请选择文章分类" style="width: 100%">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签">
              <el-select
                v-model="articleForm.tagIds"
                multiple
                placeholder="请选择文章标签"
                style="width: 100%"
              >
                <el-option
                  v-for="item in tagOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                >
                  <div class="tag-option">
                    <div class="color-dot" :style="{ backgroundColor: item.color }"></div>
                    <span>{{ item.name }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="18">
            <el-form-item label="封面图片">
              <el-upload
                class="cover-uploader"
                action="#"
                :auto-upload="true"
                :show-file-list="false"
                :http-request="uploadCoverImage"
                :before-upload="beforeCoverUpload"
              >
                <el-image
                  v-if="coverImage"
                  :src="coverImage"
                  fit="cover"
                  class="cover-image"
                />
                <div v-else class="upload-placeholder">
                  <el-icon class="upload-icon"><Plus /></el-icon>
                  <div class="upload-text">点击上传封面</div>
                </div>
              </el-upload>
              <div class="tip">建议尺寸: 900x500px, 大小不超过2MB</div>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="主色调">
              <el-color-picker 
                v-model="articleForm.mainColor" 
                color-format="hex" 
                :predefine="predefineColors" 
              />
              <div class="tip">文章卡片展示颜色</div>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="articleForm.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入文章摘要，如不填写将自动提取正文前150字"
          />
        </el-form-item>
        
        <el-form-item label="文章大纲">
          <div class="outline-editor">
            <!-- 大纲编辑器（优先显示） -->
            <div v-if="showOutlineEditor" class="outline-display">
              <div class="outline-header">
                <span class="outline-title">{{ parsedOutline.length > 0 ? '编辑大纲' : '创建大纲' }}</span>
                <div class="outline-actions">
                  <el-button size="small" @click="showOutlineEditor = false">
                    {{ parsedOutline.length > 0 ? '返回预览' : '取消创建' }}
                  </el-button>
                  <el-button v-if="parsedOutline.length > 0" size="small" type="danger" @click="clearOutline">清空大纲</el-button>
                </div>
              </div>
              
              <!-- 大纲编辑器 -->
              <div class="outline-edit">
                <el-input
                  v-model="outlineText"
                  type="textarea"
                  :rows="12"
                  placeholder="请输入大纲内容，格式示例：
# 一级标题（主要章节）
## 二级标题（子章节）
### 三级标题（细分要点）
#### 四级标题（详细内容）
##### 五级标题（补充说明）
###### 六级标题（备注）

注意：
- 支持一级到六级标题（# ## ### #### ##### ######）
- 标题长度建议不超过25个字符
- 保持标题简洁明了

您也可以使用JSON格式：
[
  {
    &quot;level&quot;: 1,
    &quot;title&quot;: &quot;概述&quot;,
    &quot;anchorId&quot;: &quot;heading-1&quot;
  },
  {
    &quot;level&quot;: 2,
    &quot;title&quot;: &quot;核心要点&quot;,
    &quot;anchorId&quot;: &quot;heading-2&quot;
  },
  {
    &quot;level&quot;: 3,
    &quot;title&quot;: &quot;基础定义&quot;,
    &quot;anchorId&quot;: &quot;heading-3&quot;
  },
  {
    &quot;level&quot;: 4,
    &quot;title&quot;: &quot;详细内容&quot;,
    &quot;anchorId&quot;: &quot;heading-4&quot;
  }
]"
                  @blur="updateOutlineFromText"
                />
                <div class="outline-edit-actions">
                  <el-button size="small" @click="parseMarkdownOutline">解析Markdown格式</el-button>
                  <el-button size="small" @click="formatOutlineJson">格式化JSON</el-button>
                  <el-button size="small" type="success" @click="saveOutlineChanges">保存修改</el-button>
                  <el-button size="small" @click="cancelOutlineEdit">取消编辑</el-button>
                </div>
              </div>
            </div>
            
            <!-- 大纲显示区域 -->
            <div v-else-if="parsedOutline.length > 0" class="outline-display">
              <div class="outline-header">
                <span class="outline-title">当前大纲</span>
                <div class="outline-actions">
                  <el-button size="small" type="primary" @click="createNewOutline">
                    编辑大纲
                  </el-button>
                  <el-button size="small" type="danger" @click="clearOutline">清空大纲</el-button>
                </div>
              </div>
              
              <!-- 大纲预览（简化版） -->
              <div class="outline-preview">
                <div 
                  v-for="(item, index) in parsedOutline" 
                  :key="index"
                  class="outline-item" 
                  :style="{ marginLeft: (item.level - 1) * 12 + 'px' }"
                >
                  <div class="outline-header-item">
                    <el-tag :type="getTagType(item.level)" size="small">{{ item.number }}</el-tag>
                    <span class="outline-item-title">{{ item.title }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 空状态 -->
            <div v-else class="outline-empty">
              <el-empty description="暂无大纲">
                <el-button type="primary" @click="createNewOutline">手动创建大纲</el-button>
              </el-empty>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="正文" prop="content" class="content-item">
          <!-- Markdown 编辑器 -->
          <MarkdownEditor
            ref="markdownEditorRef"
            v-model="articleForm.content"
            :height="'500px'"
            placeholder="请输入文章内容（支持 Markdown 语法）..."
            :onUploadImg="handleMarkdownImageUpload"
            @change="handleMarkdownChange"
            @save="handleMarkdownSave"
          />
        </el-form-item>
        
        <el-form-item>
          <el-row :gutter="50">
            <el-col :span="8">
              <el-checkbox v-model="articleForm.isTop">置顶文章</el-checkbox>
            </el-col>
            <el-col :span="8">
              <el-checkbox v-model="articleForm.isRecommend">推荐文章</el-checkbox>
            </el-col>
          </el-row>
        </el-form-item>
        
        <!-- 密码保护设置 -->
        <el-form-item label="密码保护">
          <div class="password-protection-section">
            <el-row :gutter="20" align="middle">
              <el-col :span="6">
                <el-switch
                  v-model="articleForm.isPasswordProtected"
                  :active-value="1"
                  :inactive-value="0"
                  :disabled="!isEdit || isSettingPassword"
                  :loading="isSettingPassword"
                  active-text="开启"
                  inactive-text="关闭"
                  @change="handlePasswordProtectedChange"
                />
              </el-col>
              <el-col :span="18" v-if="articleForm.isPasswordProtected === 1">
                <div class="password-settings">
                  <el-form-item label="有效期" label-width="60px" style="margin-bottom: 0;">
                    <el-date-picker
                      v-model="articleForm.passwordExpireTime"
                      type="datetime"
                      placeholder="选择过期时间（可选，不选则永不过期）"
                      format="YYYY-MM-DD HH:mm:ss"
                      value-format="YYYY-MM-DDTHH:mm:ss"
                      :shortcuts="passwordExpireShortcuts"
                      style="width: 280px;"
                    />
                  </el-form-item>
                </div>
              </el-col>
            </el-row>
            
            <!-- 新建模式提示 -->
            <div class="password-tip" v-if="!isEdit && articleForm.isPasswordProtected === 1">
              <el-icon><InfoFilled /></el-icon>
              <span>保存文章后将自动生成密码</span>
            </div>
            <div class="password-tip" v-if="!isEdit && articleForm.isPasswordProtected !== 1">
              <el-icon><InfoFilled /></el-icon>
              <span>请先保存文章后再开启密码保护</span>
            </div>
            
            <!-- 编辑模式下显示密码 -->
            <div class="password-display-section" v-if="isEdit && articleForm.isPasswordProtected === 1">
              <!-- 有明文密码时显示 -->
              <div v-if="generatedPassword" class="password-display">
                <span class="password-label">访问密码：</span>
                <el-input 
                  :model-value="generatedPassword" 
                  readonly 
                  class="password-input"
                  style="width: 200px;"
                >
                  <template #append>
                    <el-button @click="copyPassword">
                      <el-icon><CopyDocument /></el-icon>
                    </el-button>
                  </template>
                </el-input>
                <el-button type="warning" link @click="handleRegeneratePassword" :loading="isSettingPassword">
                  <el-icon><Refresh /></el-icon>
                  重新生成
                </el-button>
              </div>
              <!-- 密码已存在但无明文时显示 -->
              <div v-else-if="passwordExists" class="password-placeholder">
                <el-tag type="warning" size="small">
                  <el-icon><Lock /></el-icon>
                  密码已设置
                </el-tag>
                <el-button type="primary" size="small" @click="handleRegeneratePassword" :loading="isSettingPassword">
                  <el-icon><Refresh /></el-icon>
                  重新生成密码
                </el-button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 隐藏的文件输入 -->
    <input
      ref="markdownFileInput"
      type="file"
      accept=".md,.markdown,.txt"
      style="display: none"
      @change="handleMarkdownFileImport"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, onBeforeUnmount, shallowRef, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { getAllTags } from '@/api/TagApi'
import { getAllCategories } from '@/api/CategoryApi'
import { createArticleWithUUID, updateArticle, getArticleDetail, regenerateArticlePassword, getArticlePassword, setArticlePassword } from '@/api/ArticleApi'
import { uploadToTemp } from '@/api/fileManagement'
import { generateUUID } from '@/utils/uuidUtils'
import { getFileUrl } from '@/utils/fileUtils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, InfoFilled, Lock, CopyDocument, Refresh } from '@element-plus/icons-vue'
import MarkdownEditor from '@/components/backend/MarkdownEditor.vue'
import ArticleAiAssistant from '@/components/backend/ArticleAiAssistant.vue'
import { markdownToHtml, htmlToText, extractSummary } from '@/utils/markdownUtils'

const route = useRoute()
const router = useRouter()

// 判断是否为编辑模式
const isEdit = computed(() => !!route.params.id)

// 封面图片显示（处理完整URL）
const coverImage = computed(() => getFileUrl(articleForm.coverImage))

// 当前内容（用于AI助手）
const currentContent = computed(() => {
  // 直接使用 Markdown 内容
  if (articleForm.content && articleForm.content.trim()) {
    return articleForm.content
  }
  return ''
})

// 生成数字编号
const generateOutlineNumbers = (outline) => {
  const counters = [0, 0, 0, 0, 0, 0] // 支持六级标题
  
  return outline.map(item => {
    const level = item.level - 1 // 转换为数组索引
    
    // 重置下级计数器
    for (let i = level + 1; i < counters.length; i++) {
      counters[i] = 0
    }
    
    // 增加当前级别计数器
    counters[level]++
    
    // 生成编号字符串
    let number = ''
    for (let i = 0; i <= level; i++) {
      if (counters[i] > 0) {
        number += (number ? '.' : '') + counters[i]
      }
    }
    
    return {
      ...item,
      number: number
    }
  })
}

// 解析大纲数据
const parsedOutline = computed(() => {
  if (!articleForm.outline) return []
  
  try {
    const outline = JSON.parse(articleForm.outline)
    if (Array.isArray(outline)) {
      return generateOutlineNumbers(outline)
    }
    return []
  } catch (error) {
    console.warn('大纲数据解析失败:', error)
    return []
  }
})

// 大纲标签类型
const getTagType = (level) => {
  const types = ['', 'danger', 'warning', 'success', 'info', 'primary', '']
  return types[level] || 'info'
}



// 表单引用
const articleFormRef = ref(null)
// 分类选项
const categoryOptions = ref([])
// 标签选项
const tagOptions = ref([])

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

// 文章表单 - 必须在使用之前定义
const articleForm = reactive({
  id: null,
  title: '',
  content: '',
  htmlContent: '',
  summary: '',
  outline: '', // 文章大纲(JSON格式)
  coverImage: '',
  mainColor: '#409EFF', // 文章主色调
  categoryId: null,
  tagIds: [], // 存储标签ID而不是整个对象
  tags: [], // 保留兼容性
  isTop: false,
  isRecommend: false,
  isPasswordProtected: 0, // 是否开启密码保护
  passwordExpireTime: null, // 密码过期时间
  status: 0 // 0:草稿, 1:已发布
})

// 当前密码信息（编辑模式下使用）
const currentPasswordInfo = reactive({
  enabled: false,
  expireTime: null
})

// 密码显示相关
const generatedPassword = ref('') // 生成的密码
const passwordExists = ref(false) // 密码是否已存在
const isSettingPassword = ref(false) // 是否正在设置密码

// 密码过期时间快捷选项
const passwordExpireShortcuts = [
  {
    text: '1小时后',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() + 3600 * 1000)
      return date
    }
  },
  {
    text: '1天后',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() + 3600 * 1000 * 24)
      return date
    }
  },
  {
    text: '7天后',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() + 3600 * 1000 * 24 * 7)
      return date
    }
  },
  {
    text: '30天后',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() + 3600 * 1000 * 24 * 30)
      return date
    }
  }
]

// 预生成的业务UUID（仅在新增模式下使用）
const businessId = ref(null)

// 大纲编辑相关状态
const showOutlineEditor = ref(false)
const outlineText = ref('')

// 初始化业务ID
if (!isEdit.value) {
  businessId.value = generateUUID()
  articleForm.id = businessId.value
}

// Markdown 编辑器引用
const markdownEditorRef = ref()

// 文件输入引用
const markdownFileInput = ref()

// 监听 Markdown 内容变化
watch(() => articleForm.content, (newValue) => {
  // 当 Markdown 内容变化时，自动生成 HTML 内容
  if (newValue) {
    articleForm.htmlContent = markdownToHtml(newValue)
    
    // 如果摘要为空，自动生成摘要
    if (!articleForm.summary || articleForm.summary.trim() === '') {
      articleForm.summary = extractSummary(newValue, 200)
    }
  } else {
    articleForm.htmlContent = ''
  }
})

// 监听tagIds变化，确保UI更新
watch(() => articleForm.tagIds, (newIds) => {
  // 可以在这里处理tagIds变化的逻辑
  console.log('标签选择变化:', newIds)
}, { deep: true })

// 监听分类变化，自动设置主色调
let isInitialLoad = true // 标记是否为初始加载
watch(() => articleForm.categoryId, (newCategoryId, oldCategoryId) => {
  if (newCategoryId && categoryOptions.value.length > 0) {
    const selectedCategory = categoryOptions.value.find(cat => cat.id === newCategoryId)
    if (selectedCategory && selectedCategory.mainColor) {
      // 只在以下情况自动设置主色调：
      // 1. 新建文章（非编辑模式）
      // 2. 编辑模式下主色调为空或默认值，且不是初始加载
      // 3. 用户主动切换分类（oldCategoryId存在且不同）
      const shouldAutoSet = !isEdit.value || 
                           (!articleForm.mainColor || articleForm.mainColor === '#409EFF') && !isInitialLoad ||
                           (oldCategoryId && oldCategoryId !== newCategoryId && !isInitialLoad)
      
      if (shouldAutoSet) {
        articleForm.mainColor = selectedCategory.mainColor
        console.log('分类变化，自动设置主色调:', selectedCategory.mainColor)
      }
    }
  }
  
  // 首次加载后，标记为非初始加载
  if (isInitialLoad) {
    isInitialLoad = false
  }
})

// Markdown 编辑器图片上传处理
const handleMarkdownImageUpload = async (file) => {
  return new Promise((resolve, reject) => {
    // 新系统：统一上传到临时目录
    uploadToTemp(file, {
      showDefaultMsg: false,
      onSuccess: (tempUrl) => {
        console.log('图片已上传到临时目录:', tempUrl)
        resolve({ url: tempUrl })
      },
      onError: (error) => {
        console.error('Markdown编辑器图片上传失败:', error)
        reject(error)
      }
    })
  })
}

// Markdown 内容变化处理
const handleMarkdownChange = (data) => {
  // data 包含：{ markdown, html, version }
  // 已经通过 watch 自动处理了内容同步
  if (data && data.markdown) {
    console.log('Markdown 内容变化:', data.markdown.length, '字符')
  } else {
    console.log('Markdown 内容变化: 内容为空或undefined')
  }
}

// Markdown 保存处理
const handleMarkdownSave = (data) => {
  // 用户按了 Ctrl+S 或点击了保存按钮
  if (data && data.markdown) {
    console.log('用户请求保存文章，内容长度:', data.markdown.length)
  } else {
    console.log('用户请求保存文章')
  }
  // 可以在这里自动保存草稿
  // saveArticle(0) // 保存为草稿
}

// 表单验证规则
const articleRules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择文章分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入文章内容', trigger: 'blur' }
  ]
}



// 初始化
onMounted(() => {
  // 加载分类和标签列表
  fetchCategoryList()
  fetchTagList()
  
  // 如果是编辑模式，获取文章详情
  if (isEdit.value) {
    fetchArticleDetail(route.params.id)
  }
})

// 组件销毁时的清理工作
onBeforeUnmount(() => {
  // Markdown 编辑器不需要手动销毁
  console.log('文章编辑组件销毁')
})

// 获取 Markdown 编辑器实例
const getMarkdownEditor = () => {
  return markdownEditorRef.value
  
  // 如果有内容，设置到编辑器中
  if (articleForm.htmlContent) {
    valueHtml.value = articleForm.htmlContent
  }
}

// 获取分类列表
const fetchCategoryList = () => {
  getAllCategories({
    showDefaultMsg: false,
    onSuccess: (data) => {
      categoryOptions.value = data
    },
    onError: (error) => {
      console.error('获取分类列表失败:', error)
    }
  })
}

// 获取标签列表
const fetchTagList = () => {
  getAllTags({
    showDefaultMsg: false,
    onSuccess: (data) => {
      tagOptions.value = data
    },
    onError: (error) => {
      console.error('获取标签列表失败:', error)
    }
  })
}

// 获取文章详情
const fetchArticleDetail = (id) => {
  getArticleDetail(id, {}, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      // 填充表单数据
      articleForm.id = data.id
      articleForm.title = data.title
      
      // 处理 Markdown 内容：优先使用 content，如果为空则尝试从 HTML 转换
      if (data.content && data.content.trim()) {
        articleForm.content = data.content
      } else if (data.htmlContent) {
        // 如果只有 HTML 内容，尝试转换为 Markdown（基础功能）
        articleForm.content = htmlToText(data.htmlContent) // 暂时使用纯文本，后续可以增强
      } else {
        articleForm.content = ''
      }
      
      articleForm.htmlContent = data.htmlContent || ''
      articleForm.summary = data.summary
      articleForm.outline = data.outline || ''
      articleForm.coverImage = data.coverImage
      articleForm.mainColor = data.mainColor || '#409EFF'
      articleForm.categoryId = data.categoryId
      
      // 设置标签ID数组
      if (data.tags && Array.isArray(data.tags)) {
        articleForm.tags = data.tags
        articleForm.tagIds = data.tags.map(tag => tag.id)
      }
      
      articleForm.isTop = data.isTop === 1
      articleForm.isRecommend = data.isRecommend === 1
      articleForm.status = data.status
      
      // 加载密码保护信息
      articleForm.isPasswordProtected = data.isPasswordProtected || 0
      articleForm.passwordExpireTime = data.passwordExpireTime || null
      currentPasswordInfo.enabled = data.isPasswordProtected === 1
      currentPasswordInfo.expireTime = data.passwordExpireTime
      
      // 如果开启了密码保护，获取密码状态
      if (data.isPasswordProtected === 1) {
        fetchArticlePassword(data.id)
      } else {
        generatedPassword.value = ''
        passwordExists.value = false
      }
      
      // Markdown 编辑器会通过 v-model 自动显示内容
      console.log('文章详情加载完成:', {
        title: data.title,
        contentLength: articleForm.content.length,
        hasHtml: !!data.htmlContent,
        isPasswordProtected: data.isPasswordProtected
      })
    },
    onError: (error) => {
      console.error('获取文章详情失败:', error)
      ElMessage.error('获取文章详情失败')
      router.push('/back/article')
    }
  })
}

// 封面图片上传前的校验
const beforeCoverUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  
  if (!isImage) {
    ElMessage.error('封面只能是图片格式!')
    return false
  }
  
  if (!isLt2M) {
    ElMessage.error('封面大小不能超过 2MB!')
    return false
  }
  
  return true
}

// 上传封面图片
const uploadCoverImage = (options) => {
  const { file } = options
  
  // 新系统：统一上传到临时目录
  uploadToTemp(file, {
    successMsg: '封面上传成功',
    onSuccess: (tempUrl) => {
      articleForm.coverImage = tempUrl
      console.log('封面已上传到临时目录:', tempUrl)
      options.onSuccess(tempUrl)
    },
    onError: (error) => {
      console.error('封面上传失败:', error)
      options.onError(error)
    }
  })
}

// 保存文章（采用策略B：UUID预生成）
const saveArticle = (status) => {
  // 确保 HTML 内容是最新的（从 Markdown 生成）
  if (articleForm.content && articleForm.content.trim()) {
    articleForm.htmlContent = markdownToHtml(articleForm.content)
    console.log('从 Markdown 生成 HTML，长度:', articleForm.htmlContent.length)
  } else {
    articleForm.htmlContent = ''
    console.log('Markdown 内容为空，清空 HTML')
  }
  
  // 如果摘要为空，自动从 Markdown 内容提取
  if (!articleForm.summary && articleForm.content) {
    articleForm.summary = extractSummary(articleForm.content, 200)
    console.log('自动生成摘要:', articleForm.summary)
  }
  
  // 保存前确保大纲编辑器的内容被正确同步
  if (showOutlineEditor.value) {
    console.log('大纲编辑器处于打开状态，同步大纲内容')
    console.log('当前编辑器内容:', outlineText.value)
    console.log('当前表单大纲:', articleForm.outline)
    
    if (outlineText.value.trim()) {
      updateOutlineFromText()
      console.log('同步后的表单大纲:', articleForm.outline)
    } else {
      // 如果编辑器为空，清空大纲
      articleForm.outline = ''
      console.log('编辑器为空，已清空大纲')
    }
  }
  
  // 从articleForm.tagIds构建tags数组，保持后端兼容性
  const tagsData = articleForm.tagIds.map(id => {
    const tagObj = tagOptions.value.find(tag => tag.id === id);
    return { id: id, ...tagObj };
  });
  
  // 转换布尔值为数字
  const formData = {
    ...articleForm,
    isTop: articleForm.isTop ? 1 : 0,
    isRecommend: articleForm.isRecommend ? 1 : 0,
    status,
    mainColor: articleForm.mainColor, // 确保包含主色调字段
    tags: tagsData // 发送标签对象数组，与后端ArticleService.saveArticleTags方法兼容
  }
  
  // 调试信息：检查大纲数据
  console.log('保存文章时的完整formData:', formData)
  console.log('保存文章时的大纲数据:', formData.outline)
  console.log('大纲数据类型:', typeof formData.outline)
  console.log('大纲数据长度:', formData.outline ? formData.outline.length : 'undefined')
  
  articleFormRef.value.validate((valid) => {
    if (valid) {
      if (isEdit.value) {
        // 更新文章 - 使用传统方式
        updateArticle(articleForm.id, formData, {
          successMsg: status === 1 ? '文章发布成功' : '文章已保存为草稿',
          onSuccess: () => {
            router.push('/back/article')
          },
          onError: (error) => {
            console.error('更新文章失败:', error)
          }
        })
      } else {
        // 新增文章 - 使用UUID预生成策略
        createArticleWithUUID(formData, {
          successMsg: status === 1 ? '文章发布成功' : '文章已保存为草稿',
          onSuccess: () => {
            router.push('/back/article')
          },
          onError: (error) => {
            console.error('创建文章失败:', error)
            // 如果文章创建失败，文件已上传，这里可以考虑清理策略
            // 但根据策略B的设计，文件已经与UUID关联，即使业务创建失败也有明确标识
          }
        })
      }
    }
  })
}

// ========== AI助手事件处理方法 ==========

// 更新标题
const updateTitle = (title) => {
  articleForm.title = title
}

// 更新摘要
const updateSummary = (summary) => {
  articleForm.summary = summary
}

// 更新大纲
const updateOutline = (outline) => {
  articleForm.outline = outline
}

// 更新标签
const updateTags = (tagNames) => {
  // 根据标签名称找到对应的标签ID
  const matchedTagIds = []
  tagNames.forEach(tagName => {
    const existingTag = tagOptions.value.find(tag => tag.name === tagName)
    if (existingTag) {
      matchedTagIds.push(existingTag.id)
    }
  })
  articleForm.tagIds = matchedTagIds
}

// 更新分类
const updateCategoryId = (categoryName) => {
  // 根据分类名称找到对应的分类ID
  const category = categoryOptions.value.find(cat => cat.name === categoryName)
  if (category) {
    articleForm.categoryId = category.id
  }
}

// ========== 大纲编辑方法 ==========

// 创建新大纲
const createNewOutline = () => {
  showOutlineEditor.value = true
  // 如果已有大纲，载入编辑；如果没有，提供示例模板
  if (articleForm.outline) {
    try {
      const parsed = JSON.parse(articleForm.outline)
      outlineText.value = JSON.stringify(parsed, null, 2)
    } catch (error) {
      outlineText.value = articleForm.outline
    }
  } else {
    outlineText.value = `# 概述
## 背景介绍
## 主要内容
### 核心要点
### 关键特性
# 详细分析
## 实现方法
### 技术方案
### 操作步骤
# 总结与展望`
  }
}

// 清空大纲
const clearOutline = () => {
  articleForm.outline = ''
  outlineText.value = ''
  showOutlineEditor.value = false
  ElMessage.success('大纲已清空')
}

// 从文本更新大纲
const updateOutlineFromText = () => {
  if (!outlineText.value.trim()) {
    articleForm.outline = '' // 如果文本为空，清空大纲
    console.log('大纲文本为空，已清空大纲数据')
    return
  }
  
  try {
    // 尝试解析JSON格式
    const parsed = JSON.parse(outlineText.value)
    if (Array.isArray(parsed)) {
      articleForm.outline = JSON.stringify(parsed)
      console.log('JSON格式大纲更新成功:', articleForm.outline)
      return
    }
  } catch (error) {
    // 不是JSON格式，继续尝试Markdown解析
    console.log('不是JSON格式，尝试Markdown解析')
  }
  
  // 尝试解析Markdown格式
  parseMarkdownOutline()
}

// 解析Markdown格式大纲（简化版）
const parseMarkdownOutline = () => {
  if (!outlineText.value.trim()) {
    ElMessage.warning('请输入大纲内容')
    return
  }
  
  try {
    const lines = outlineText.value.split('\n').filter(line => line.trim())
    const outline = []
    let currentItem = null
    
    for (const line of lines) {
      const trimmedLine = line.trim()
      if (!trimmedLine) continue
      
      // 匹配标题格式 (# ## ### #### ##### ######)，支持最多6级
      const headerMatch = trimmedLine.match(/^(#{1,6})\s+(.+)$/)
      if (headerMatch) {
        // 如果有之前的项目，先保存
        if (currentItem) {
          outline.push(currentItem)
        }
        
        // 创建新项目
        const level = headerMatch[1].length
        let title = headerMatch[2].trim()
        
        // 限制标题长度不超过25个字符
        if (title.length > 25) {
          title = title.substring(0, 25) + '...'
        }
        
        currentItem = {
          level: Math.min(level, 6), // 支持最多6级标题
          title: title,
          anchorId: `heading-${outline.length + 1}` // 自动生成锚点ID
        }
      }
    }
    
    // 保存最后一个项目
    if (currentItem) {
      outline.push(currentItem)
    }
    
    if (outline.length > 0) {
      articleForm.outline = JSON.stringify(outline, null, 2)
      console.log('Markdown大纲解析成功:', articleForm.outline)
      ElMessage.success('Markdown大纲解析成功')
    } else {
      ElMessage.warning('未识别到有效的大纲结构')
    }
  } catch (error) {
    console.error('Markdown解析失败:', error)
    ElMessage.error('大纲解析失败，请检查格式')
  }
}

// 格式化JSON大纲
const formatOutlineJson = () => {
  if (!outlineText.value.trim()) {
    ElMessage.warning('请输入大纲内容')
    return
  }
  
  try {
    const parsed = JSON.parse(outlineText.value)
    outlineText.value = JSON.stringify(parsed, null, 2)
    ElMessage.success('JSON格式化成功')
  } catch (error) {
    ElMessage.error('JSON格式错误，无法格式化')
  }
}

// 保存大纲修改
const saveOutlineChanges = () => {
  updateOutlineFromText()
  showOutlineEditor.value = false
  ElMessage.success('大纲修改已保存')
}

// 取消大纲编辑
const cancelOutlineEdit = () => {
  // 恢复原始大纲文本
  if (articleForm.outline) {
    try {
      const parsed = JSON.parse(articleForm.outline)
      outlineText.value = JSON.stringify(parsed, null, 2)
    } catch (error) {
      outlineText.value = articleForm.outline
    }
  } else {
    outlineText.value = ''
  }
  showOutlineEditor.value = false
}

// 监听大纲变化，同步到编辑器（仅在非编辑状态下同步）
watch(() => articleForm.outline, (newOutline) => {
  // 仅在打开编辑器时且不是用户手动编辑时同步
  if (newOutline && showOutlineEditor.value && !outlineText.value) {
    try {
      const parsed = JSON.parse(newOutline)
      outlineText.value = JSON.stringify(parsed, null, 2)
    } catch (error) {
      outlineText.value = newOutline
    }
  }
}, { immediate: false })

// ========== 密码保护相关方法 ==========

// 获取文章密码信息
const fetchArticlePassword = (articleId) => {
  getArticlePassword(articleId, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      if (res.enabled && res.password) {
        generatedPassword.value = res.password
        passwordExists.value = false
      } else if (res.enabled) {
        generatedPassword.value = ''
        passwordExists.value = true
      } else {
        generatedPassword.value = ''
        passwordExists.value = false
      }
    },
    onError: (error) => {
      console.error('获取密码信息失败:', error)
      generatedPassword.value = ''
    }
  })
}

// 重新生成密码
const handleRegeneratePassword = () => {
  if (!articleForm.id) {
    ElMessage.warning('请先保存文章')
    return
  }
  
  ElMessageBox.confirm(
    '确定要重新生成密码吗？旧密码将失效。',
    '重新生成密码',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    isSettingPassword.value = true
    regenerateArticlePassword(articleForm.id, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        isSettingPassword.value = false
        generatedPassword.value = data.password || ''
        passwordExists.value = false
        ElMessage.success('密码已重新生成')
      },
      onError: (error) => {
        isSettingPassword.value = false
        console.error('重新生成密码失败:', error)
        ElMessage.error('重新生成密码失败')
      }
    })
  }).catch(() => {
    // 用户取消
  })
}

// 复制密码到剪贴板
const copyPassword = () => {
  if (generatedPassword.value) {
    navigator.clipboard.writeText(generatedPassword.value).then(() => {
      ElMessage.success('密码已复制到剪贴板')
    }).catch(() => {
      ElMessage.error('复制失败，请手动复制')
    })
  }
}

// 处理密码保护开关变化
const handlePasswordProtectedChange = (enabled) => {
  if (!isEdit.value) {
    // 新建文章，只记录状态，保存时处理
    return
  }
  
  if (enabled === 1) {
    // 开启密码保护
    isSettingPassword.value = true
    setArticlePassword(articleForm.id, {
      enablePassword: true,
      expireTime: articleForm.passwordExpireTime
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        isSettingPassword.value = false
        generatedPassword.value = res.password || ''
        passwordExists.value = false
        currentPasswordInfo.enabled = true
        ElMessage.success('密码保护已开启')
      },
      onError: (error) => {
        isSettingPassword.value = false
        articleForm.isPasswordProtected = 0
        console.error('设置密码保护失败:', error)
        ElMessage.error('设置密码保护失败')
      }
    })
  } else {
    // 关闭密码保护
    isSettingPassword.value = true
    setArticlePassword(articleForm.id, {
      enablePassword: false
    }, {
      showDefaultMsg: false,
      onSuccess: () => {
        isSettingPassword.value = false
        generatedPassword.value = ''
        passwordExists.value = false
        articleForm.passwordExpireTime = null
        currentPasswordInfo.enabled = false
        ElMessage.success('密码保护已关闭')
      },
      onError: (error) => {
        isSettingPassword.value = false
        articleForm.isPasswordProtected = 1
        console.error('关闭密码保护失败:', error)
        ElMessage.error('关闭密码保护失败')
      }
    })
  }
}

// ========== Markdown 文件导入功能 ==========

// 触发文件选择
const triggerMarkdownImport = () => {
  markdownFileInput.value?.click()
}

// 处理文件导入
const handleMarkdownFileImport = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  // 验证文件类型
  const validExtensions = ['.md', '.markdown', '.txt']
  const fileName = file.name.toLowerCase()
  const isValidFile = validExtensions.some(ext => fileName.endsWith(ext))
  
  if (!isValidFile) {
    ElMessage.error('请选择 Markdown 文件 (.md, .markdown, .txt)')
    return
  }
  
  // 验证文件大小（限制10MB）
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    return
  }
  
  try {
    const content = await readFileAsText(file)
    await parseAndFillMarkdownContent(content, file.name)
    ElMessage.success('Markdown 文件导入成功')
  } catch (error) {
    console.error('导入 Markdown 文件失败:', error)
    ElMessage.error('导入失败: ' + (error.message || '文件读取错误'))
  } finally {
    // 清空文件输入，允许重复导入同一文件
    event.target.value = ''
  }
}

// 读取文件内容
const readFileAsText = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsText(file, 'UTF-8')
  })
}

// 解析 Front Matter（YAML 格式）
const parseFrontMatter = (content) => {
  const frontMatterRegex = /^---\s*\n([\s\S]*?)\n---\s*\n?([\s\S]*)$/
  const match = content.match(frontMatterRegex)
  
  if (!match) {
    return { metadata: {}, content: content }
  }
  
  const yamlStr = match[1]
  const markdownContent = match[2]
  
  try {
    // 简单的 YAML 解析（支持基本的 key: value 格式）
    const metadata = parseSimpleYaml(yamlStr)
    return { metadata, content: markdownContent }
  } catch (error) {
    console.warn('Front Matter 解析失败，使用原始内容:', error)
    return { metadata: {}, content: content }
  }
}

// 简单的 YAML 解析器（支持基本格式）
const parseSimpleYaml = (yamlStr) => {
  const result = {}
  const lines = yamlStr.split('\n')
  
  for (const line of lines) {
    const trimmedLine = line.trim()
    if (!trimmedLine || trimmedLine.startsWith('#')) continue
    
    const colonIndex = trimmedLine.indexOf(':')
    if (colonIndex === -1) continue
    
    const key = trimmedLine.substring(0, colonIndex).trim()
    let value = trimmedLine.substring(colonIndex + 1).trim()
    
    // 处理引号
    if ((value.startsWith('"') && value.endsWith('"')) || 
        (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1)
    }
    
    // 处理数组（简单格式：[item1, item2]）
    if (value.startsWith('[') && value.endsWith(']')) {
      const arrayContent = value.slice(1, -1)
      value = arrayContent.split(',').map(item => item.trim().replace(/['"]/g, ''))
    }
    
    // 处理布尔值
    if (value === 'true') value = true
    else if (value === 'false') value = false
    // 处理数字
    else if (!isNaN(value) && value !== '') value = Number(value)
    
    result[key] = value
  }
  
  return result
}

// 解析并填充 Markdown 内容
const parseAndFillMarkdownContent = async (content, fileName) => {
  const { metadata, content: markdownContent } = parseFrontMatter(content)
  
  // 确认是否覆盖现有内容
  if (articleForm.content && articleForm.content.trim() || articleForm.title && articleForm.title.trim()) {
    try {
      await ElMessageBox.confirm(
        '导入文件将覆盖当前内容，是否继续？',
        '确认导入',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch (e) {
      // 用户点击取消
      return
    }
  }
  
  // 填充表单内容
  fillFormFromMarkdown(metadata, markdownContent, fileName)
  
  console.log('Markdown 文件导入完成:', {
    title: articleForm.title,
    contentLength: articleForm.content.length,
    summary: articleForm.summary
  })
}

// 从 Markdown 填充表单数据
const fillFormFromMarkdown = (metadata, content, fileName) => {
  // 1. 填充标题
  if (metadata.title) {
    articleForm.title = metadata.title
  } else {
    // 如果没有标题，尝试从文件名生成
    const nameWithoutExt = fileName.replace(/\.(md|markdown|txt)$/i, '')
    articleForm.title = nameWithoutExt
  }
  
  // 2. 填充内容 - 使用 nextTick 确保响应式更新
  const trimmedContent = content.trim()
  articleForm.content = ''  // 先清空
  nextTick(() => {
    articleForm.content = trimmedContent  // 再设置新值，触发响应式更新
    console.log('内容已填充到编辑器，长度:', trimmedContent.length)
  })
  
  // 3. 填充摘要
  if (metadata.summary || metadata.description || metadata.excerpt) {
    articleForm.summary = metadata.summary || metadata.description || metadata.excerpt
  } else {
    // 自动从内容提取摘要
    articleForm.summary = extractSummary(content, 200)
  }
  
  // 4. 处理分类
  if (metadata.category) {
    const category = categoryOptions.value.find(cat => 
      cat.name === metadata.category || 
      cat.name.toLowerCase() === metadata.category.toLowerCase()
    )
    if (category) {
      articleForm.categoryId = category.id
    }
  }
  
  // 5. 处理标签
  if (metadata.tags && Array.isArray(metadata.tags)) {
    const matchedTagIds = []
    metadata.tags.forEach(tagName => {
      const existingTag = tagOptions.value.find(tag => 
        tag.name === tagName || 
        tag.name.toLowerCase() === tagName.toLowerCase()
      )
      if (existingTag) {
        matchedTagIds.push(existingTag.id)
      }
    })
    articleForm.tagIds = matchedTagIds
  }
  
  // 6. 处理文章状态
  if (metadata.published !== undefined) {
    articleForm.status = metadata.published ? 1 : 0
  }
  
  if (metadata.draft !== undefined) {
    articleForm.status = metadata.draft ? 0 : 1
  }
  
  // 7. 处理置顶和推荐
  if (metadata.top !== undefined || metadata.sticky !== undefined) {
    articleForm.isTop = metadata.top || metadata.sticky || false
  }
  
  if (metadata.recommend !== undefined || metadata.featured !== undefined) {
    articleForm.isRecommend = metadata.recommend || metadata.featured || false
  }
  
  // 8. 自动生成或处理大纲
  if (metadata.outline) {
    // 如果有预定义大纲
    try {
      if (typeof metadata.outline === 'string') {
        articleForm.outline = metadata.outline
      } else {
        articleForm.outline = JSON.stringify(metadata.outline)
      }
    } catch (error) {
      console.warn('大纲数据处理失败:', error)
    }
  } else {
    // 从内容自动提取大纲
    const autoOutline = extractOutlineFromContent(content)
    if (autoOutline.length > 0) {
      articleForm.outline = JSON.stringify(autoOutline)
    }
  }
  
  console.log('Markdown 导入完成:', {
    title: articleForm.title,
    contentLength: articleForm.content.length,
    summaryLength: articleForm.summary.length,
    tags: articleForm.tagIds.length,
    category: articleForm.categoryId,
    outline: articleForm.outline ? 'yes' : 'no'
  })
}

// 从内容自动提取大纲
const extractOutlineFromContent = (content) => {
  const lines = content.split('\n')
  const outline = []
  
  for (const line of lines) {
    const trimmedLine = line.trim()
    if (!trimmedLine) continue
    
    // 匹配标题格式 (# ## ### #### ##### ######)
    const headerMatch = trimmedLine.match(/^(#{1,6})\s+(.+)$/)
    if (headerMatch) {
      const level = headerMatch[1].length
      let title = headerMatch[2].trim()
      
      // 移除可能的标题ID语法 {#id}
      title = title.replace(/\s*\{#[^}]+\}\s*$/, '')
      
      // 限制标题长度
      if (title.length > 30) {
        title = title.substring(0, 30) + '...'
      }
      
      outline.push({
        level: Math.min(level, 6),
        title: title,
        anchorId: `heading-${outline.length + 1}`
      })
    }
  }
  
  return outline
}
</script>

<style>
/* 全局富文本编辑器样式 */
.w-e-toolbar {
  z-index: 2001 !important;
}

.w-e-menu {
  z-index: 2002 !important;
}

.w-e-text-container {
  z-index: 2000 !important;
  text-align: left !important;
}

.w-e-dropdown-menu {
  z-index: 2003 !important;
}

/* 确保编辑器内容正确显示 */
.w-e-text-container .w-e-text p {
  margin: 10px 0;
}

.w-e-text-container .w-e-text h1,
.w-e-text-container .w-e-text h2,
.w-e-text-container .w-e-text h3,
.w-e-text-container .w-e-text h4,
.w-e-text-container .w-e-text h5 {
  margin: 10px 0;
  font-weight: bold;
}

.w-e-text-container .w-e-text ul,
.w-e-text-container .w-e-text ol {
  margin: 10px 0 10px 20px;
}

.w-e-text-container .w-e-text img {
  max-width: 100%;
  height: auto;
}

.w-e-text-container .w-e-text code {
  background-color: #f6f6f6;
  padding: 2px 4px;
  border-radius: 3px;
}

.w-e-text-container .w-e-text pre {
  background-color: #f6f6f6;
  padding: 10px;
  border-radius: 5px;
  overflow: auto;
}

.w-e-text-container .w-e-text blockquote {
  border-left: 3px solid #d0d0d0;
  padding-left: 10px;
  color: #666;
  margin: 10px 0;
}
</style>

<style scoped>
@import '@/assets/backend-common.css';

.article-edit-container {
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

.action-buttons {
  display: flex;
  gap: 12px;
}

.action-buttons .el-button {
  border-radius: 4px;
  font-weight: 500;
}

.content-item {
  margin-bottom: 24px;
}

.cover-uploader {
  width: 360px;
}

.cover-image {
  width: 360px;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #eaeaea;
}

.upload-placeholder {
  width: 360px;
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  background-color: #fafafa;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder:hover {
  border-color: #333;
  background-color: #f0f0f0;
}

.upload-icon {
  font-size: 28px;
  color: #666;
  margin-bottom: 8px;
}

.upload-text {
  color: #666;
  font-size: 14px;
}

.tip {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

.tag-option {
  display: flex;
  align-items: center;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
}

/* 编辑器样式优化 */
:deep(.w-e-toolbar) {
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
  border-color: #eaeaea !important;
}

:deep(.w-e-text-container) {
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
  border-color: #eaeaea !important;
}

/* 大纲编辑器样式 */
.outline-editor {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fafafa;
}

.outline-display {
  padding: 16px;
}

.outline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.outline-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.outline-actions {
  display: flex;
  gap: 8px;
}

.outline-preview {
  background-color: white;
  border-radius: 4px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  max-height: 400px;
  overflow-y: auto;
}

.outline-item {
  margin-bottom: 6px;
  padding: 8px 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  transition: background-color 0.2s;
}

.outline-item:hover {
  background-color: #ecf5ff;
}

.outline-header-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.outline-item-title {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
  flex: 1;
}

.outline-item-content {
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
  margin-left: 16px;
  padding: 4px 8px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 3px;
  border-left: 2px solid #d9ecff;
}

.outline-edit {
  padding: 16px;
  background-color: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.outline-edit-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  justify-content: flex-end;
}

.outline-empty {
  padding: 40px;
  text-align: center;
}

/* 大纲编辑器文本域样式 */
.outline-edit :deep(.el-textarea__inner) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  border-radius: 4px;
  border-color: #e4e7ed;
}

.outline-edit :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
}

/* Markdown 导入按钮样式 */
.action-buttons .el-button[type="success"] {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border: none;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-buttons .el-button[type="success"]:hover {
  background: linear-gradient(135deg, #5daf34 0%, #7bc143 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.action-buttons .el-button[type="success"] .el-icon {
  margin-right: 6px;
}

/* 密码保护设置样式 */
.password-protection-section {
  width: 100%;
}

.password-settings {
  display: flex;
  align-items: center;
  gap: 16px;
}

.password-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  background-color: #fdf6ec;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 12px;
}

.password-tip .el-icon {
  font-size: 14px;
}

.password-status {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding: 8px 12px;
  background-color: #fef0f0;
  border-radius: 4px;
}

.password-status .el-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 密码显示区域 */
.password-display-section {
  margin-top: 12px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.password-display {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.password-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.password-input {
  font-family: 'Consolas', 'Monaco', monospace;
}

.password-input :deep(.el-input__inner) {
  font-weight: 600;
  color: #e6a23c;
  letter-spacing: 2px;
}

.password-placeholder {
  display: flex;
  align-items: center;
  gap: 12px;
}

.password-placeholder .el-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style> 