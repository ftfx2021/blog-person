<template>
  <div class="markdown-editor-container">
    <MdEditor
      ref="mdEditorRef"
      v-model="localContent"
      :preview="true"
      :toolbars="toolbars"
      theme="light"
      previewTheme="github"
      codeTheme="github"
      :editor-id="editorId"
      language="zh-CN"
      :placeholder="placeholder"
      :tab-width="2"
      :scroll-auto="true"
      :no-html="true"
      @onUploadImg="handleUploadImg"
      @onChange="handleChange"
      @onSave="handleSave"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { ElMessage } from 'element-plus'

// Props
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  height: {
    type: [String, Number],
    default: '500px'
  },
  placeholder: {
    type: String,
    default: '开始写作...'
  },
  // 图片上传回调函数
  onUploadImg: {
    type: Function,
    default: null
  },
  // 内容变化回调
  onChange: {
    type: Function,
    default: null
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'change', 'save'])

// 编辑器引用
const mdEditorRef = ref()

// 本地内容
const localContent = ref(props.modelValue)

// 编辑器唯一ID
const editorId = ref(`md-editor-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`)

// 工具栏配置
const toolbars = [
  'bold',
  'underline',
  'italic',
  '-',
  'title',
  'strikeThrough',
  'quote',
  'unorderedList',
  'orderedList',
  'task',
  '-',
  'codeRow',
  'code',
  'link',
  'image',
  'table',
  '-',
  'revoke',
  'next',
  'save',
  '=',
  'pageFullscreen',
  'fullscreen',
  'preview',
  'catalog'
]

// 监听外部传入的内容变化
watch(() => props.modelValue, (newValue) => {
  if (newValue !== localContent.value) {
    localContent.value = newValue
  }
})

// 监听本地内容变化，向外发射
watch(localContent, (newValue) => {
  emit('update:modelValue', newValue)
})

// 处理图片上传
const handleUploadImg = async (files, callback) => {
  try {
    if (!props.onUploadImg) {
      ElMessage.error('图片上传功能未配置')
      return
    }

    const results = []
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const result = await props.onUploadImg(file)
      
      if (result && result.url) {
        results.push(result.url)
      } else if (typeof result === 'string') {
        results.push(result)
      } else {
        throw new Error('上传失败，返回格式错误')
      }
    }
    
    callback(results)
    
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败: ' + (error.message || '未知错误'))
  }
}

// 处理内容变化
const handleChange = (value, html) => {
  emit('change', {
    markdown: value || '',
    html: html || ''
  })
  
  if (props.onChange) {
    props.onChange(value, html)
  }
}

// 处理保存事件
const handleSave = (value, html) => {
  emit('save', {
    markdown: value || '',
    html: html || ''
  })
}

// 获取HTML内容
const getHtml = () => {
  return mdEditorRef.value?.getHtml() || ''
}

// 设置内容
const setContent = (content) => {
  localContent.value = content
}

// 插入内容
const insertContent = (content) => {
  if (mdEditorRef.value) {
    mdEditorRef.value.insert(() => content)
  }
}

// 聚焦编辑器
const focus = () => {
  if (mdEditorRef.value) {
    mdEditorRef.value.focus()
  }
}

// 暴露方法给父组件
defineExpose({
  getHtml,
  setContent,
  insertContent,
  focus
})
</script>

<style scoped>
.markdown-editor-container {
  width: 100%;
  height: 100%;
  position: relative;
}

/* md-editor-v3 样式优化 */
:deep(.md-editor) {
  height: 100% !important;
  border: none;
  border-radius: 0;
}

/* 工具栏样式 */
:deep(.md-editor-toolbar) {
  background: rgba(250, 250, 250, 0.95);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 6px 12px;
}

:deep(.md-editor-toolbar-item) {
  border-radius: 4px;
}

:deep(.md-editor-toolbar-item:hover) {
  background: rgba(7, 26, 242, 0.08);
}

/* 编辑区域样式 */
:deep(.md-editor-input-wrapper) {
  background: #fff;
}

:deep(.md-editor-input) {
  font-size: 14px;
  line-height: 1.8;
  padding: 20px 40px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
}

/* 预览区域样式 */
:deep(.md-editor-preview) {
  background: #fff;
  padding: 20px 40px;
  font-size: 15px;
  line-height: 1.8;
}

/* 标题样式 */
:deep(.md-editor-preview h1),
:deep(.md-editor-preview h2),
:deep(.md-editor-preview h3),
:deep(.md-editor-preview h4),
:deep(.md-editor-preview h5),
:deep(.md-editor-preview h6) {
  margin: 24px 0 16px 0;
  font-weight: 600;
  line-height: 1.4;
  color: #1a1a2e;
}

:deep(.md-editor-preview h1) {
  font-size: 2em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 10px;
}

:deep(.md-editor-preview h2) {
  font-size: 1.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 8px;
}

:deep(.md-editor-preview h3) {
  font-size: 1.25em;
}

/* 段落样式 */
:deep(.md-editor-preview p) {
  margin: 16px 0;
  line-height: 1.8;
}

/* 代码块样式 */
:deep(.md-editor-preview pre) {
  background: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  margin: 16px 0;
  overflow-x: auto;
}

:deep(.md-editor-preview code) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
  font-size: 14px;
}

:deep(.md-editor-preview code:not([class*="language-"])) {
  background: rgba(175, 184, 193, 0.2);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 85%;
}

/* 引用样式 */
:deep(.md-editor-preview blockquote) {
  padding: 12px 16px;
  margin: 16px 0;
  color: #6a737d;
  border-left: 4px solid rgb(7, 26, 242);
  background: rgba(7, 26, 242, 0.03);
  border-radius: 0 6px 6px 0;
}

/* 列表样式 */
:deep(.md-editor-preview ul),
:deep(.md-editor-preview ol) {
  margin: 16px 0;
  padding-left: 24px;
}

:deep(.md-editor-preview li) {
  margin: 6px 0;
  line-height: 1.7;
}

/* 链接样式 */
:deep(.md-editor-preview a) {
  color: rgb(7, 26, 242);
  text-decoration: none;
}

:deep(.md-editor-preview a:hover) {
  text-decoration: underline;
}

/* 表格样式 */
:deep(.md-editor-preview table) {
  border-collapse: collapse;
  margin: 16px 0;
  width: 100%;
}

:deep(.md-editor-preview table th),
:deep(.md-editor-preview table td) {
  border: 1px solid #dfe2e5;
  padding: 10px 14px;
  text-align: left;
}

:deep(.md-editor-preview table th) {
  background: #f6f8fa;
  font-weight: 600;
}

/* 图片样式 */
:deep(.md-editor-preview img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin: 16px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

/* 分割线 */
:deep(.md-editor-preview hr) {
  border: none;
  border-top: 1px solid #eaecef;
  margin: 24px 0;
}

/* 任务列表 */
:deep(.md-editor-preview .task-list-item) {
  list-style: none;
  margin-left: -20px;
}

:deep(.md-editor-preview .task-list-item input) {
  margin-right: 8px;
}

/* 滚动条样式 */
:deep(.md-editor-input-wrapper)::-webkit-scrollbar,
:deep(.md-editor-preview-wrapper)::-webkit-scrollbar {
  width: 6px;
}

:deep(.md-editor-input-wrapper)::-webkit-scrollbar-track,
:deep(.md-editor-preview-wrapper)::-webkit-scrollbar-track {
  background: transparent;
}

:deep(.md-editor-input-wrapper)::-webkit-scrollbar-thumb,
:deep(.md-editor-preview-wrapper)::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

:deep(.md-editor-input-wrapper)::-webkit-scrollbar-thumb:hover,
:deep(.md-editor-preview-wrapper)::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

</style>
