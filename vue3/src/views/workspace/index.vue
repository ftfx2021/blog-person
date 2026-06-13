<template>
  <div class="article-workspace" :style="workspaceStyle">
    <!-- 左侧分类树面板 -->
    <div class="sidebar-panel" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <span v-if="!sidebarCollapsed" class="sidebar-title">文章管理</span>
        <el-button 
          :icon="sidebarCollapsed ? 'Expand' : 'Fold'" 
          link 
          @click="toggleSidebar"
          class="collapse-btn"
        />
      </div>
      
      <div v-if="!sidebarCollapsed" class="sidebar-content">
        <!-- 搜索框 -->
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文章..."
            :prefix-icon="Search"
            clearable
            size="small"
          />
        </div>
        
        <!-- 分类树 -->
        <div class="category-tree">
          <el-tree
            ref="categoryTreeRef"
            :data="filteredCategoryTree"
            :props="treeProps"
            node-key="id"
            :expand-on-click-node="false"
            :default-expand-all="true"
            :highlight-current="true"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node" :class="{ 'is-article': data.type === 'article' }">
                <div class="node-content">
                  <el-icon class="node-icon">
                    <Folder v-if="data.type === 'category'" />
                    <Document v-else />
                  </el-icon>
                  <span class="node-label" :title="node.label">{{ node.label }}</span>
                  <el-tag 
                    v-if="data.type === 'article' && data.status === 0" 
                    size="small" 
                    type="info"
                    class="status-tag"
                  >
                    草稿
                  </el-tag>
                </div>
                
                <div v-if="data.type === 'category'" class="node-actions">
                  <el-dropdown trigger="click" @command="(cmd) => handleNodeAction(cmd, data)" @click.stop>
                    <el-button :icon="Plus" link size="small" @click.stop />
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="article">
                          <el-icon><Document /></el-icon>
                          <span>新建文章</span>
                        </el-dropdown-item>
                        <!-- 只有一级分类才能新建子分类（最多二级） -->
                        <el-dropdown-item v-if="!data.isSubCategory" command="category">
                          <el-icon><Folder /></el-icon>
                          <span>新建子分类</span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </template>
          </el-tree>
        </div>
        
        <!-- 快速新增按钮 -->
        <div class="quick-add">
          <el-button type="primary" :icon="Plus" style="width: 100%" @click="openNewCategoryDialog(0)">
            新增分类
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 右侧编辑区域 -->
    <div class="editor-panel">
      <div v-if="!currentArticle" class="empty-state">
        <el-empty description="选择一篇文章开始编辑，或创建新文章">
          <el-button type="primary" @click="handleQuickAdd">创建新文章</el-button>
        </el-empty>
      </div>
      
      <div v-else class="editor-container">
        <!-- 编辑器头部 -->
        <div class="editor-header">
          <div class="header-left">
            <el-input
              v-model="articleForm.title"
              placeholder="请输入文章标题"
              class="title-input"
              maxlength="100"
              show-word-limit
            />
          </div>
          <div class="header-right">
            <span v-if="autoSaveTime" class="auto-save-tip">
              <el-icon><Clock /></el-icon>
              {{ autoSaveTime }}
            </span>
            <el-button @click="openSettingsDrawer">
              <el-icon><Setting /></el-icon>
              设置
            </el-button>
            <el-button v-if="isEditMode" type="danger" @click="handleDeleteArticle">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
            <el-button type="info" @click="saveArticle(0)">保存草稿</el-button>
            <el-button type="primary" @click="saveArticle(1)">发布文章</el-button>
          </div>
        </div>
        
        <!-- Markdown 编辑器 -->
        <div class="editor-body">
          <MarkdownEditor
            ref="markdownEditorRef"
            v-model="articleForm.content"
            :height="editorHeight"
            placeholder="开始写作..."
            :onUploadImg="handleMarkdownImageUpload"
            @change="handleContentChange"
            @save="handleEditorSave"
          />
        </div>
      </div>
    </div>
    
    <!-- 文章设置抽屉 -->
    <el-drawer
      v-model="settingsDrawerVisible"
      title="文章设置"
      direction="rtl"
      size="400px"
    >
      <el-form :model="articleForm" label-width="80px" label-position="top">
        <el-form-item label="文章分类">
          <el-select v-model="articleForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="item in flatCategoryList"
              :key="item.id"
              :label="item.displayName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="文章标签">
          <el-select
            v-model="articleForm.tagIds"
            multiple
            placeholder="请选择标签"
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
              v-if="articleForm.coverImage"
              :src="getFileUrl(articleForm.coverImage)"
              fit="cover"
              class="cover-preview"
            />
            <div v-else class="upload-placeholder">
              <el-icon class="upload-icon"><Plus /></el-icon>
              <div class="upload-text">点击上传封面</div>
            </div>
          </el-upload>
          <div class="form-tip">建议尺寸: 900x500px</div>
        </el-form-item>
        
        <el-form-item label="主色调">
          <el-color-picker 
            v-model="articleForm.mainColor" 
            color-format="hex" 
            :predefine="predefineColors" 
          />
        </el-form-item>
        
        <el-form-item label="文章摘要">
          <el-input
            v-model="articleForm.summary"
            type="textarea"
            :rows="3"
            placeholder="如不填写将自动提取正文前150字"
          />
        </el-form-item>
        
        <el-form-item>
          <el-checkbox v-model="articleForm.isTop">置顶文章</el-checkbox>
          <el-checkbox v-model="articleForm.isRecommend">推荐文章</el-checkbox>
        </el-form-item>
        
        <el-divider content-position="left">密码保护</el-divider>
        
        <el-form-item label="开启密码保护">
          <el-switch 
            v-model="articleForm.isPasswordProtected" 
            :disabled="!isArticleSaved || isSettingPassword"
            :loading="isSettingPassword"
            @change="handlePasswordProtectedChange"
          />
          <div v-if="!isArticleSaved" class="form-tip form-tip-disabled">
            <el-icon><Warning /></el-icon>
            请先保存文章后再开启密码保护
          </div>
          <div v-else class="form-tip">开启后，访客需输入密码才能查看文章内容</div>
        </el-form-item>
        
        <!-- 密码显示区域 -->
        <el-form-item v-if="articleForm.isPasswordProtected" label="访问密码">
          <!-- 有明文密码时显示 -->
          <div v-if="generatedPassword" class="password-display">
            <el-input 
              :model-value="generatedPassword" 
              readonly 
              class="password-input"
            >
              <template #append>
                <el-button @click="copyPassword">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </template>
            </el-input>
            <div class="password-actions">
              <el-button type="warning" link @click="handleRegeneratePassword" :loading="isSettingPassword">
                <el-icon><Refresh /></el-icon>
                重新生成
              </el-button>
            </div>
          </div>
          <!-- 密码已存在但无明文时显示 -->
          <div v-else-if="passwordExists" class="password-placeholder">
            <div class="password-hint">
              <el-icon><Lock /></el-icon>
              <span>密码已设置（出于安全考虑不显示）</span>
            </div>
            <el-button type="primary" @click="handleRegeneratePassword" :loading="isSettingPassword">
              <el-icon><Refresh /></el-icon>
              重新生成密码
            </el-button>
          </div>
          <div class="form-tip">重新生成后原密码将失效</div>
        </el-form-item>
        
        <el-form-item v-if="articleForm.isPasswordProtected" label="密码过期时间">
          <el-date-picker
            v-model="articleForm.passwordExpireTime"
            type="datetime"
            placeholder="选择过期时间（可选）"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :shortcuts="passwordExpireShortcuts"
            :disabled="!isArticleSaved"
            @change="handlePasswordExpireTimeChange"
          />
          <div class="form-tip">不设置则永不过期</div>
        </el-form-item>
      </el-form>
    </el-drawer>
    
    <!-- 选择分类对话框（新建文章时选择分类） -->
    <el-dialog v-model="categoryDialogVisible" title="选择分类" width="400px">
      <el-form label-width="80px">
        <el-form-item label="文章分类">
          <el-select v-model="newArticleCategoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="item in flatCategoryList"
              :key="item.id"
              :label="item.displayName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreateArticle">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 新建分类对话框 -->
    <el-dialog v-model="newCategoryDialogVisible" title="新建分类" width="450px">
      <el-form ref="newCategoryFormRef" :model="newCategoryForm" :rules="categoryRules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="newCategoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="newCategoryForm.parentId" placeholder="请选择父分类" style="width: 100%">
            <el-option :value="0" label="无（顶级分类）" />
            <el-option
              v-for="item in topLevelCategories"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分类描述">
          <el-input v-model="newCategoryForm.description" type="textarea" :rows="2" placeholder="请输入分类描述" />
        </el-form-item>
        <el-form-item label="主色调">
          <el-color-picker v-model="newCategoryForm.mainColor" color-format="hex" :predefine="predefineColors" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newCategoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreateCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Folder, Document, Plus, Clock, Setting, Warning, CopyDocument, Refresh, Lock, Delete } from '@element-plus/icons-vue'
import { getCategoryTree, createCategory } from '@/api/CategoryApi'
import { getAllTags } from '@/api/TagApi'
import { 
  getArticleDetail, 
  getArticlesPage, 
  createArticleWithUUID, 
  updateArticle,
  deleteArticle,
  setArticlePassword,
  getArticlePassword,
  regenerateArticlePassword 
} from '@/api/ArticleApi'
import { uploadToTemp } from '@/api/fileManagement'
import { generateUUID } from '@/utils/uuidUtils'
import { markdownToHtml, extractSummary } from '@/utils/markdownUtils'
import { hexToRgb } from '@/utils/colorUtils'
import { getFileUrl } from '@/utils/fileUtils'
import MarkdownEditor from '@/components/backend/MarkdownEditor.vue'

const route = useRoute()
const router = useRouter()

// ========== 状态定义 ==========
const sidebarCollapsed = ref(false)
const searchKeyword = ref('')
const categoryTreeData = ref([])
const categoryTreeRef = ref(null)
const flatCategoryList = ref([])
const tagOptions = ref([])
const currentArticle = ref(null)
const markdownEditorRef = ref(null)
const isEditMode = ref(false)

const articleForm = reactive({
  id: null,
  title: '',
  content: '',
  htmlContent: '',
  summary: '',
  coverImage: '',
  mainColor: '#409EFF',
  categoryId: null,
  tagIds: [],
  isTop: false,
  isRecommend: false,
  status: 0,
  isPasswordProtected: false,
  passwordExpireTime: null
})

const businessId = ref(null)
const settingsDrawerVisible = ref(false)
const categoryDialogVisible = ref(false)
const newArticleCategoryId = ref(null)
const autoSaveTime = ref('')
const autoSaveTimer = ref(null)
const hasUnsavedChanges = ref(false)
const isLoadingArticle = ref(false) // 标记是否正在加载文章，避免触发未保存提示

// 密码保护相关
const generatedPassword = ref('') // 生成的密码
const isSettingPassword = ref(false) // 是否正在设置密码
const passwordExists = ref(false) // 密码是否已存在（但未显示明文）

// 新建分类相关
const newCategoryDialogVisible = ref(false)
const newCategoryFormRef = ref(null)
const newCategoryForm = reactive({
  name: '',
  description: '',
  mainColor: '#409EFF',
  parentId: 0
})
const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ]
}

const editorHeight = computed(() => '100%')

// 根据文章主色调计算背景样式
const workspaceStyle = computed(() => {
  const mainColor = articleForm.mainColor || '#409EFF'
  const { r, g, b } = hexToRgb(mainColor)
  return {
    background: `linear-gradient(135deg, rgba(${r}, ${g}, ${b}, 0.08) 0%, rgba(${r}, ${g}, ${b}, 0.04) 50%, rgba(${r}, ${g}, ${b}, 0.08) 100%)`
  }
})

// 顶级分类列表（用于新建分类时选择父分类）
const topLevelCategories = computed(() => {
  return flatCategoryList.value.filter(cat => {
    // 找出没有 "/" 的分类，即顶级分类
    return !cat.displayName.includes(' / ')
  })
})

const treeProps = {
  children: 'children',
  label: 'label'
}

const predefineColors = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8',
  '#F7DC6F', '#BB8FCE'
]

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

// ========== 计算属性 ==========
// 判断文章是否已保存到数据库（编辑模式 = 已保存）
const isArticleSaved = computed(() => {
  return isEditMode.value
})

const filteredCategoryTree = computed(() => {
  if (!searchKeyword.value) return categoryTreeData.value
  
  const keyword = searchKeyword.value.toLowerCase()
  
  const filterTree = (nodes) => {
    return nodes.reduce((acc, node) => {
      const matchLabel = node.label.toLowerCase().includes(keyword)
      const filteredChildren = node.children ? filterTree(node.children) : []
      
      if (matchLabel || filteredChildren.length > 0) {
        acc.push({
          ...node,
          children: filteredChildren.length > 0 ? filteredChildren : node.children
        })
      }
      return acc
    }, [])
  }
  
  return filterTree(categoryTreeData.value)
})

// ========== 生命周期 ==========
onMounted(() => {
  fetchCategoryTree()
  fetchTagList()
  
  if (route.query.id) {
    loadArticle(route.query.id)
  }
  
  // 启动自动保存定时器
  startAutoSave()
})

onBeforeUnmount(() => {
  if (autoSaveTimer.value) {
    clearInterval(autoSaveTimer.value)
  }
})

// ========== 数据获取 ==========
const fetchCategoryTree = async () => {
  getCategoryTree({
    showDefaultMsg: false,
    onSuccess: async (categories) => {
      getArticlesPage({ currentPage: 1, size: 1000 }, {
        showDefaultMsg: false,
        onSuccess: (articlesData) => {
          const articles = articlesData.records || []
          categoryTreeData.value = buildCategoryTreeWithArticles(categories, articles)
          flatCategoryList.value = flattenCategories(categories)
        },
        onError: (error) => {
          console.error('获取文章列表失败:', error)
          categoryTreeData.value = buildCategoryTreeWithArticles(categories, [])
          flatCategoryList.value = flattenCategories(categories)
        }
      })
    },
    onError: (error) => {
      console.error('获取分类树失败:', error)
    }
  })
}

const buildCategoryTreeWithArticles = (categories, articles) => {
  const buildNode = (category, isSubCategory = false) => {
    const categoryArticles = articles
      .filter(a => a.categoryId === category.id)
      .map(a => ({
        id: `article-${a.id}`,
        articleId: a.id,
        label: a.title,
        type: 'article',
        status: a.status,
        categoryId: a.categoryId
      }))
    
    // 子分类标记为 isSubCategory = true，不能再创建子分类
    const childCategories = (category.children || []).map(child => buildNode(child, true))
    
    return {
      id: `category-${category.id}`,
      categoryId: category.id,
      label: category.name,
      type: 'category',
      mainColor: category.mainColor,
      isSubCategory: isSubCategory, // 标记是否为二级分类
      children: [...childCategories, ...categoryArticles]
    }
  }
  
  // 顶级分类 isSubCategory = false
  return categories.map(cat => buildNode(cat, false))
}

const flattenCategories = (categories, prefix = '') => {
  const result = []
  categories.forEach(cat => {
    const displayName = prefix ? `${prefix} / ${cat.name}` : cat.name
    result.push({
      id: cat.id,
      name: cat.name,
      displayName: displayName,
      mainColor: cat.mainColor
    })
    if (cat.children && cat.children.length > 0) {
      result.push(...flattenCategories(cat.children, displayName))
    }
  })
  return result
}

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

// ========== 文章操作 ==========
const loadArticle = (articleId) => {
  isLoadingArticle.value = true // 开始加载，禁用变化检测
  
  getArticleDetail(articleId, {}, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      currentArticle.value = data
      isEditMode.value = true
      
      articleForm.id = data.id
      articleForm.title = data.title
      articleForm.content = data.content || ''
      articleForm.htmlContent = data.htmlContent || ''
      articleForm.summary = data.summary || ''
      articleForm.coverImage = data.coverImage || ''
      articleForm.mainColor = data.mainColor || '#409EFF'
      articleForm.categoryId = data.categoryId
      articleForm.tagIds = data.tags ? data.tags.map(t => t.id) : []
      articleForm.isTop = data.isTop === 1
      articleForm.isRecommend = data.isRecommend === 1
      articleForm.status = data.status
      articleForm.isPasswordProtected = data.isPasswordProtected === 1
      articleForm.passwordExpireTime = data.passwordExpireTime || null
      
      // 如果开启了密码保护，获取密码信息
      if (data.isPasswordProtected === 1) {
        fetchArticlePassword(data.id)
      } else {
        generatedPassword.value = ''
      }
      
      // 使用 nextTick 确保所有响应式更新完成后再重置标志
      nextTick(() => {
        hasUnsavedChanges.value = false
        isLoadingArticle.value = false // 加载完成，恢复变化检测
      })
    },
    onError: (error) => {
      console.error('获取文章详情失败:', error)
      ElMessage.error('获取文章详情失败')
      isLoadingArticle.value = false
    }
  })
}

const createNewArticle = (categoryId, mainColor) => {
  // 检查是否有未保存的更改
  if (hasUnsavedChanges.value) {
    ElMessageBox.confirm('当前文章有未保存的更改，是否保存？', '提示', {
      confirmButtonText: '保存',
      cancelButtonText: '不保存',
      type: 'warning',
      distinguishCancelAndClose: true
    }).then(() => {
      saveArticle(articleForm.status || 0)
      doCreateNewArticle(categoryId, mainColor)
    }).catch((action) => {
      if (action === 'cancel') {
        doCreateNewArticle(categoryId, mainColor)
      }
    })
  } else {
    doCreateNewArticle(categoryId, mainColor)
  }
}

const doCreateNewArticle = (categoryId, mainColor) => {
  businessId.value = generateUUID()
  isEditMode.value = false
  currentArticle.value = { isNew: true }
  
  Object.assign(articleForm, {
    id: businessId.value,
    title: '',
    content: '',
    htmlContent: '',
    summary: '',
    coverImage: '',
    mainColor: mainColor || '#409EFF',
    categoryId: categoryId,
    tagIds: [],
    isTop: false,
    isRecommend: false,
    status: 0,
    isPasswordProtected: false,
    passwordExpireTime: null
  })
  
  // 清空密码相关状态
  generatedPassword.value = ''
  passwordExists.value = false
  
  hasUnsavedChanges.value = false
  autoSaveTime.value = ''
}

const saveArticle = (status) => {
  if (!articleForm.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  
  if (!articleForm.categoryId) {
    ElMessage.warning('请选择文章分类')
    settingsDrawerVisible.value = true
    return
  }
  
  // 生成HTML内容
  if (articleForm.content && articleForm.content.trim()) {
    articleForm.htmlContent = markdownToHtml(articleForm.content)
  }
  
  // 自动生成摘要
  if (!articleForm.summary && articleForm.content) {
    articleForm.summary = extractSummary(articleForm.content, 200)
  }
  
  const tagsData = articleForm.tagIds.map(id => {
    const tagObj = tagOptions.value.find(tag => tag.id === id)
    return { id: id, ...tagObj }
  })
  
  const formData = {
    ...articleForm,
    isTop: articleForm.isTop ? 1 : 0,
    isRecommend: articleForm.isRecommend ? 1 : 0,
    isPasswordProtected: articleForm.isPasswordProtected ? 1 : 0,
    passwordExpireTime: articleForm.isPasswordProtected ? articleForm.passwordExpireTime : null,
    status,
    tags: tagsData
  }
  
  if (isEditMode.value) {
    updateArticle(articleForm.id, formData, {
      successMsg: status === 1 ? '文章发布成功' : '草稿保存成功',
      onSuccess: () => {
        hasUnsavedChanges.value = false
        autoSaveTime.value = formatTime(new Date()) + ' 已保存'
        fetchCategoryTree()
      },
      onError: (error) => {
        console.error('更新文章失败:', error)
      }
    })
  } else {
    createArticleWithUUID(formData, {
      successMsg: status === 1 ? '文章发布成功' : '草稿保存成功',
      onSuccess: (data) => {
        isEditMode.value = true
        hasUnsavedChanges.value = false
        autoSaveTime.value = formatTime(new Date()) + ' 已保存'
        fetchCategoryTree()
      },
      onError: (error) => {
        console.error('创建文章失败:', error)
      }
    })
  }
}

// 删除文章
const handleDeleteArticle = () => {
  if (!isEditMode.value || !articleForm.id) {
    ElMessage.warning('当前文章尚未保存，无需删除')
    return
  }
  
  ElMessageBox.confirm(
    `确定要删除文章"${articleForm.title}"吗？此操作不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(() => {
    deleteArticle(articleForm.id, {
      successMsg: '文章已删除',
      onSuccess: () => {
        // 清空当前编辑状态
        currentArticle.value = null
        isEditMode.value = false
        hasUnsavedChanges.value = false
        autoSaveTime.value = ''
        generatedPassword.value = ''
        passwordExists.value = false
        
        // 重置表单
        Object.assign(articleForm, {
          id: null,
          title: '',
          content: '',
          htmlContent: '',
          summary: '',
          coverImage: '',
          mainColor: '#409EFF',
          categoryId: null,
          tagIds: [],
          isTop: false,
          isRecommend: false,
          status: 0,
          isPasswordProtected: false,
          passwordExpireTime: null
        })
        
        // 刷新分类树
        fetchCategoryTree()
      },
      onError: (error) => {
        console.error('删除文章失败:', error)
      }
    })
  }).catch(() => {
    // 用户取消
  })
}

// ========== 密码保护处理 ==========
const handlePasswordProtectedChange = (enabled) => {
  if (!isArticleSaved.value) {
    // 未保存的文章不能开启密码保护
    articleForm.isPasswordProtected = false
    return
  }
  
  if (enabled) {
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
        ElMessage.success('密码保护已开启')
      },
      onError: (error) => {
        isSettingPassword.value = false
        articleForm.isPasswordProtected = false
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
        articleForm.passwordExpireTime = null
        ElMessage.success('密码保护已关闭')
      },
      onError: (error) => {
        isSettingPassword.value = false
        articleForm.isPasswordProtected = true
        console.error('关闭密码保护失败:', error)
        ElMessage.error('关闭密码保护失败')
      }
    })
  }
}

// 处理密码过期时间变化
const handlePasswordExpireTimeChange = (expireTime) => {
  if (!isArticleSaved.value || !articleForm.isPasswordProtected) return
  
  setArticlePassword(articleForm.id, {
    enablePassword: true,
    expireTime: expireTime
  }, {
    showDefaultMsg: false,
    onSuccess: () => {
      ElMessage.success('过期时间已更新')
    },
    onError: (error) => {
      console.error('更新过期时间失败:', error)
    }
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

// 获取文章密码信息
const fetchArticlePassword = (articleId) => {
  getArticlePassword(articleId, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      if (res.enabled && res.password) {
        // 如果返回了明文密码（刚设置时）
        generatedPassword.value = res.password
      } else if (res.enabled) {
        // 密码已设置但不返回明文，显示占位符提示用户可以重新生成
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
  if (!isArticleSaved.value) return
  
  ElMessageBox.confirm('重新生成密码后，原密码将失效，确定要继续吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    isSettingPassword.value = true
    regenerateArticlePassword(articleForm.id, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        isSettingPassword.value = false
        generatedPassword.value = res.password || ''
        passwordExists.value = false
        ElMessage.success('密码已重新生成')
      },
      onError: (error) => {
        isSettingPassword.value = false
        console.error('重新生成密码失败:', error)
        ElMessage.error('重新生成密码失败')
      }
    })
  }).catch(() => {})
}

// ========== 事件处理 ==========
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const handleNodeClick = (data) => {
  if (data.type === 'article') {
    if (hasUnsavedChanges.value) {
      ElMessageBox.confirm('当前文章有未保存的更改，是否保存？', '提示', {
        confirmButtonText: '保存',
        cancelButtonText: '不保存',
        type: 'warning',
        distinguishCancelAndClose: true
      }).then(() => {
        saveArticle(articleForm.status || 0)
        loadArticle(data.articleId)
      }).catch((action) => {
        if (action === 'cancel') {
          loadArticle(data.articleId)
        }
      })
    } else {
      loadArticle(data.articleId)
    }
  }
}

const handleAddArticle = (categoryData) => {
  createNewArticle(categoryData.categoryId, categoryData.mainColor)
}

// 处理节点操作（新建文章或新建子分类）
const handleNodeAction = (command, categoryData) => {
  if (command === 'article') {
    createNewArticle(categoryData.categoryId, categoryData.mainColor)
  } else if (command === 'category') {
    openNewCategoryDialog(categoryData.categoryId)
  }
}

// 处理快速新增操作
const handleQuickAction = (command) => {
  if (command === 'article') {
    handleQuickAdd()
  } else if (command === 'category') {
    openNewCategoryDialog(0)
  }
}

// 打开新建分类对话框
const openNewCategoryDialog = (parentId = 0) => {
  Object.assign(newCategoryForm, {
    name: '',
    description: '',
    mainColor: '#409EFF',
    parentId: parentId
  })
  newCategoryDialogVisible.value = true
}

// 确认创建分类
const confirmCreateCategory = () => {
  newCategoryFormRef.value.validate((valid) => {
    if (valid) {
      createCategory(newCategoryForm, {
        successMsg: '分类创建成功',
        onSuccess: () => {
          newCategoryDialogVisible.value = false
          fetchCategoryTree() // 刷新分类树
        },
        onError: (error) => {
          console.error('创建分类失败:', error)
        }
      })
    }
  })
}

const handleQuickAdd = () => {
  if (flatCategoryList.value.length === 0) {
    ElMessage.warning('请先创建分类')
    return
  }
  
  if (flatCategoryList.value.length === 1) {
    const cat = flatCategoryList.value[0]
    createNewArticle(cat.id, cat.mainColor)
  } else {
    newArticleCategoryId.value = flatCategoryList.value[0]?.id
    categoryDialogVisible.value = true
  }
}

const confirmCreateArticle = () => {
  if (!newArticleCategoryId.value) {
    ElMessage.warning('请选择分类')
    return
  }
  
  const selectedCat = flatCategoryList.value.find(c => c.id === newArticleCategoryId.value)
  categoryDialogVisible.value = false
  createNewArticle(newArticleCategoryId.value, selectedCat?.mainColor)
}

const openSettingsDrawer = () => {
  settingsDrawerVisible.value = true
}

const handleContentChange = () => {
  // 加载文章时不触发未保存提示
  if (!isLoadingArticle.value) {
    hasUnsavedChanges.value = true
  }
}

const handleEditorSave = () => {
  saveArticle(articleForm.status || 0)
}

// ========== 图片上传 ==========
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
        console.error('图片上传失败:', error)
        reject(error)
      }
    })
  })
}

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

// ========== 自动保存 ==========
const startAutoSave = () => {
  autoSaveTimer.value = setInterval(() => {
    if (hasUnsavedChanges.value && currentArticle.value && articleForm.title.trim() && articleForm.categoryId) {
      saveArticle(0)
    }
  }, 60000) // 每分钟自动保存
}

const formatTime = (date) => {
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

// 监听标题变化
watch(() => articleForm.title, () => {
  // 加载文章时不触发未保存提示
  if (currentArticle.value && !isLoadingArticle.value) {
    hasUnsavedChanges.value = true
  }
})
</script>

<style scoped>
/* ========== 浅色系 + 玻璃拟态设计 ========== */
.article-workspace {
  display: flex;
  height: 100vh;
  /* 背景色由JS动态计算，基于文章主色调 */
  overflow: hidden;
  padding: 16px;
  gap: 16px;
  transition: background 0.3s ease;
}

/* ========== 左侧边栏 - 毛玻璃效果 ========== */
.sidebar-panel {
  width: 280px;
  min-width: 280px;
  /* 毛玻璃效果 */
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(7, 26, 242, 0.08);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  box-shadow: 0 4px 24px rgba(7, 26, 242, 0.08);
}

.sidebar-panel.collapsed {
  width: 56px;
  min-width: 56px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid rgba(7, 26, 242, 0.06);
  min-height: 56px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.collapse-btn {
  color: #64748b;
}

.collapse-btn:hover {
  color: rgb(7, 26, 242);
  background: rgba(7, 26, 242, 0.08);
}

.sidebar-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-box {
  padding: 12px 16px;
}

.search-box :deep(.el-input__wrapper) {
  background: rgba(7, 26, 242, 0.03);
  border: 1px solid rgba(7, 26, 242, 0.1);
  box-shadow: none;
}

.search-box :deep(.el-input__wrapper:hover) {
  border-color: rgba(7, 26, 242, 0.2);
}

.search-box :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(7, 26, 242, 0.5);
  box-shadow: 0 0 0 2px rgba(7, 26, 242, 0.1);
}

.search-box :deep(.el-input__inner) {
  color: #1a1a2e;
}

.search-box :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.search-box :deep(.el-input__prefix) {
  color: #64748b;
}

.category-tree {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

/* 自定义滚动条 */
.category-tree::-webkit-scrollbar {
  width: 6px;
}

.category-tree::-webkit-scrollbar-track {
  background: transparent;
}

.category-tree::-webkit-scrollbar-thumb {
  background: rgba(7, 26, 242, 0.15);
  border-radius: 3px;
}

.category-tree::-webkit-scrollbar-thumb:hover {
  background: rgba(7, 26, 242, 0.25);
}

/* 树节点样式 */
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 6px 10px;
  border-radius: 8px;
  transition: background-color 0.3s ease;
}

.tree-node:hover {
  background: rgba(7, 26, 242, 0.06);
}

.tree-node.is-article {
  padding-left: 10px;
}

.node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.node-icon {
  font-size: 16px;
  color: #64748b;
  flex-shrink: 0;
}

.tree-node.is-article .node-icon {
  color: rgb(7, 26, 242);
}

.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: #1a1a2e;
}

.status-tag {
  flex-shrink: 0;
  margin-left: 4px;
  background: rgba(7, 26, 242, 0.08);
  border: none;
  color: #64748b;
}

.node-actions {
  opacity: 0;
  transition: opacity 0.3s ease;
}

.tree-node:hover .node-actions {
  opacity: 1;
}

.node-actions :deep(.el-button) {
  color: #64748b;
}

.node-actions :deep(.el-button:hover) {
  color: rgb(7, 26, 242);
  background: rgba(7, 26, 242, 0.08);
}

.quick-add {
  padding: 16px;
  border-top: 1px solid rgba(7, 26, 242, 0.06);
}

.quick-add .el-button {
  width: 100%;
  background: rgb(7, 26, 242);
  border: none;
  color: #fff;
}

.quick-add .el-button:hover {
  background: rgb(6, 22, 200);
}

/* ========== 右侧编辑区域 - 毛玻璃效果 ========== */
.editor-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 毛玻璃效果 */
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(7, 26, 242, 0.08);
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(7, 26, 242, 0.08);
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.empty-state :deep(.el-empty__description) {
  color: #64748b;
}

.empty-state :deep(.el-button) {
  background: rgb(7, 26, 242);
  border: none;
}

.empty-state :deep(.el-button:hover) {
  background: rgb(6, 22, 200);
}

.editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
  flex-shrink: 0;
}

.header-left {
  flex: 1;
  max-width: 600px;
}

.title-input {
  font-size: 18px;
}

.title-input :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  border: none;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.3s ease;
}

.title-input :deep(.el-input__wrapper:hover) {
  background: rgba(100, 116, 139, 0.06);
}

.title-input :deep(.el-input__wrapper.is-focus) {
  background: rgba(100, 116, 139, 0.1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: none;
}

.title-input :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 500;
  color: #1e293b;
}

.title-input :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.title-input :deep(.el-input__count) {
  background: transparent;
}

.title-input :deep(.el-input__count-inner) {
  background: transparent;
  color: #94a3b8;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right :deep(.el-button) {
  border-radius: 8px;
}

.header-right :deep(.el-button--primary) {
  background: rgb(7, 26, 242);
  border: none;
}

.header-right :deep(.el-button--primary:hover) {
  background: rgb(6, 22, 200);
}

.header-right :deep(.el-button--info) {
  background: rgba(7, 26, 242, 0.06);
  border: 1px solid rgba(7, 26, 242, 0.15);
  color: #64748b;
}

.header-right :deep(.el-button--info:hover) {
  background: rgba(7, 26, 242, 0.1);
  border-color: rgba(7, 26, 242, 0.25);
}

.auto-save-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.editor-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
  border-radius: 0 0 16px 16px;
}

.editor-body :deep(.markdown-editor-container) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.editor-body :deep(.md-editor) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: none;
}

.editor-body :deep(.md-editor-content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* 编辑区和预览区滚动修复 */
.editor-body :deep(.md-editor-input-wrapper),
.editor-body :deep(.md-editor-preview-wrapper) {
  overflow-y: auto !important;
  height: 100% !important;
}

.editor-body :deep(.md-editor-preview) {
  height: auto !important;
  min-height: 100%;
}

/* ========== 设置抽屉样式 ========== */
.cover-uploader {
  width: 100%;
}

.cover-preview {
  width: 100%;
  height: 150px;
  border-radius: 8px;
}

.upload-placeholder {
  width: 100%;
  height: 150px;
  border: 1px dashed rgba(100, 116, 139, 0.3);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.3s ease, background-color 0.3s ease;
  background: rgba(100, 116, 139, 0.03);
}

.upload-placeholder:hover {
  border-color: rgba(7, 26, 242, 0.4);
  background: rgba(7, 26, 242, 0.03);
}

.upload-icon {
  font-size: 28px;
  color: #94a3b8;
}

.upload-text {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.form-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.tag-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* ========== Element Plus 树组件样式覆盖 ========== */
:deep(.el-tree) {
  background: transparent;
  --el-tree-node-hover-bg-color: transparent;
}

:deep(.el-tree-node__content) {
  height: auto;
  padding: 2px 0;
  background: transparent;
}

:deep(.el-tree-node__expand-icon) {
  padding: 6px;
  color: #64748b;
}

:deep(.el-tree-node__expand-icon.is-leaf) {
  color: transparent;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(7, 26, 242, 0.1);
  border-radius: 8px;
}

:deep(.el-tree-node__content:hover) {
  background: transparent;
}

/* ========== 对话框样式优化 ========== */
:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 16px 20px;
  margin: 0;
}

:deep(.el-dialog__title) {
  font-weight: 600;
  color: #1e293b;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding: 12px 20px;
}

/* ========== 抽屉样式优化 ========== */
:deep(.el-drawer) {
  background: rgba(255, 255, 255, 0.98);
}

:deep(.el-drawer__header) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 16px 20px;
  margin-bottom: 0;
}

:deep(.el-drawer__title) {
  font-weight: 600;
  color: #1e293b;
}

:deep(.el-drawer__body) {
  padding: 20px;
}

/* ========== 表单样式优化 ========== */
:deep(.el-form-item__label) {
  color: #475569;
  font-weight: 500;
}

:deep(.el-select .el-input__wrapper),
:deep(.el-input .el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
}

/* ========== 下拉菜单样式 ========== */
:deep(.el-dropdown-menu) {
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

:deep(.el-dropdown-menu__item) {
  padding: 8px 16px;
  transition: background-color 0.3s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(7, 26, 242, 0.08);
  color: rgb(7, 26, 242);
}

/* ========== 密码保护样式 ========== */
.form-tip-disabled {
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.form-tip-disabled .el-icon {
  font-size: 14px;
}

.password-display {
  width: 100%;
}

.password-input {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 16px;
  letter-spacing: 2px;
}

.password-input :deep(.el-input__inner) {
  font-weight: 600;
  color: #e6a23c;
}

.password-input :deep(.el-input-group__append) {
  padding: 0;
}

.password-input :deep(.el-input-group__append .el-button) {
  border: none;
  margin: 0;
  padding: 0 16px;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 4px;
}

.password-actions {
  margin-top: 8px;
}

.password-placeholder {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  width: 100%;
}

.password-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.password-hint .el-icon {
  font-size: 18px;
  color: #e6a23c;
}
</style>
