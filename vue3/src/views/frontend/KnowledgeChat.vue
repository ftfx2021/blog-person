<template>
  <div class="knowledge-chat-container">
    <!-- 装饰背景 -->
    <div class="decorative-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
    </div>

    <!-- 聊天主体 -->
    <div class="chat-wrapper glass-card">
      <!-- 头部 -->
      <div class="chat-header">
        <div class="header-left">
          <div class="header-icon">
            <i class="fa-solid fa-robot"></i>
          </div>
          <div class="header-info">
            <h2 class="chat-title">学习版智能体问答</h2>
            <p class="chat-subtitle">支持 PDF 上传与流式任务追踪</p>
          </div>
        </div>
        <div class="header-actions">
          <template v-if="isAdmin">
            <el-tag :type="statsLoaded ? 'success' : 'info'" size="small" class="status-tag">
              <i :class="statsLoaded ? 'fa-solid fa-circle-check' : 'fa-solid fa-circle-info'"></i>
              {{ statsLoaded ? `已加载 ${stats.loadedArticles || stats.totalFiles || 0} 篇文章` : '未加载' }}
            </el-tag>
            <el-button 
              class="reload-btn"
              :loading="loadingKnowledge"
              @click="handleLoadKnowledge"
            >
              <i class="fa-solid fa-rotate"></i>
              重新加载
            </el-button>
          </template>
          <el-button class="tika-test-btn" @click="openTikaDialog">
            <i class="fa-solid fa-file-circle-check"></i>
            临时Tika测试
          </el-button>
        </div>
      </div>

      <!-- 聊天内容区 -->
      <div class="chat-content" ref="chatContentRef">
        <!-- 欢迎消息 -->
        <div v-if="messages.length === 0" class="welcome-section">
          <div class="welcome-icon-wrapper">
            <div class="welcome-icon">
              <i class="fa-solid fa-comments"></i>
            </div>
            <div class="welcome-pulse"></div>
          </div>
          <h3 class="welcome-title">欢迎使用知识库问答</h3>
          <p class="welcome-desc">我可以结合博客知识与你上传的 PDF 进行智能体问答，快来试试吧！</p>
        </div>

        <!-- 消息列表 -->
        <div v-for="(msg, index) in messages" :key="index" class="message-item" :class="msg.role">
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="40" :src="userAvatar" class="user-avatar">
              <i class="fa-solid fa-user"></i>
            </el-avatar>
            <div v-else class="ai-avatar">
              <i class="fa-solid fa-robot"></i>
            </div>
          </div>
          <div class="message-bubble">
            <!-- AI回复使用Markdown渲染 -->
            <div v-if="msg.role === 'assistant'" class="message-text assistant-text">
              <ChatMarkdownRenderer :content="msg.content" />
              <span v-if="msg.streaming" class="cursor-blink">|</span>
            </div>
            <!-- 用户消息 -->
            <template v-else>
              <div class="message-text user-text">{{ msg.content }}</div>
              <div v-if="msg.attachments && msg.attachments.length > 0" class="message-attachments">
                <span
                  v-for="(attachment, idx) in msg.attachments"
                  :key="`att-${idx}`"
                  class="attachment-tag"
                >
                  <i class="fa-regular fa-file-pdf"></i>
                  {{ attachment.fileName }}
                </span>
              </div>
            </template>

            <!-- 任务追踪 -->
            <div
              v-if="msg.role === 'assistant' && ((msg.subAgents && msg.subAgents.length > 0) || (msg.steps && msg.steps.length > 0) || (msg.tools && msg.tools.length > 0))"
              class="trace-panel"
            >
              <div class="trace-title">
                <i class="fa-solid fa-list-check"></i>
                任务追踪
              </div>
              <div v-if="msg.subAgents && msg.subAgents.length > 0" class="trace-section">
                <div class="trace-subtitle">主AGENT决策</div>
                <div v-for="(subAgent, agentIndex) in msg.subAgents" :key="`agent-${agentIndex}`" class="trace-item">
                  <span class="trace-name">{{ subAgent.subAgentCode }}</span>
                  <span class="trace-status" :class="traceStatusClass(subAgent.status)">{{ subAgent.status }}</span>
                  <span class="trace-cost" v-if="subAgent.decision">决策: {{ subAgent.decision }}</span>
                  <span class="trace-cost" v-if="subAgent.reason">({{ subAgent.reason }})</span>
                </div>
              </div>
              <div v-if="msg.steps && msg.steps.length > 0" class="trace-section">
                <div class="trace-subtitle">步骤进度</div>
                <div v-for="(step, stepIndex) in msg.steps" :key="`step-${stepIndex}`" class="trace-item">
                  <span class="trace-name">{{ step.stepCode }}</span>
                  <span class="trace-status" :class="traceStatusClass(step.status)">{{ step.status }}</span>
                  <span class="trace-cost" v-if="step.costMs !== undefined && step.costMs !== null">{{ step.costMs }}ms</span>
                </div>
              </div>
              <div v-if="msg.tools && msg.tools.length > 0" class="trace-section">
                <div class="trace-subtitle">工具调用</div>
                <div v-for="(tool, toolIndex) in msg.tools" :key="`tool-${toolIndex}`" class="trace-item">
                  <span class="trace-name">{{ tool.toolName }}</span>
                  <span class="trace-status" :class="traceStatusClass(tool.status)">{{ tool.status }}</span>
                  <span class="trace-cost" v-if="tool.costMs !== undefined && tool.costMs !== null">{{ tool.costMs }}ms</span>
                </div>
              </div>
            </div>
            
            <!-- 来源文档 -->
            <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
              <div class="sources-header">
                <i class="fa-solid fa-book-open"></i>
                <span>参考来源 ({{ msg.sources.length }})</span>
              </div>
              <div class="sources-list">
                <span 
                  v-for="(source, idx) in msg.sources" 
                  :key="idx"
                  class="source-tag clickable"
                  @click="goToArticle(source.articleId)"
                >
                  <i class="fa-solid fa-file-lines"></i>
                  {{ source.source }}
                  <i class="fa-solid fa-arrow-up-right-from-square link-icon"></i>
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="loading && !streamingMessage" class="message-item assistant">
          <div class="message-avatar">
            <div class="ai-avatar thinking">
              <i class="fa-solid fa-robot"></i>
            </div>
          </div>
          <div class="message-bubble">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="upload-row">
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            :multiple="true"
            accept=".pdf,application/pdf"
            :limit="5"
            :on-change="handlePdfFileChange"
            :on-remove="handlePdfFileRemove"
            :file-list="selectedPdfFiles"
          >
            <el-button class="upload-btn" :disabled="loading">
              <i class="fa-regular fa-file-pdf"></i>
              选择 PDF
            </el-button>
          </el-upload>
      
        </div>

        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入你的问题，按 Enter 发送..."
            :disabled="loading"
            @keydown.enter.exact.prevent="handleSend"
            class="chat-input"
          />
          <button 
            class="send-btn"
            :class="{ disabled: !inputMessage.trim() || loading }"
            :disabled="!inputMessage.trim() || loading"
            @click="handleSend"
          >
            <i v-if="loading" class="fa-solid fa-spinner fa-spin"></i>
            <i v-else class="fa-solid fa-paper-plane"></i>
          </button>
          <button
            v-if="loading"
            class="stop-btn"
            @click="handleStop"
          >
            <i class="fa-solid fa-stop"></i>
          </button>
        </div>
        <p class="input-hint">
          <i class="fa-solid fa-circle-info"></i>
          AI 回答仅供参考，步骤与工具调用为实时追踪信息
        </p>
      </div>
    </div>

    <!-- 临时：Tika 文件解析测试 -->
    <el-dialog
      v-model="tikaDialogVisible"
      title="Tika 文件解析测试（临时）"
      width="760px"
      destroy-on-close
      class="tika-test-dialog"
    >
      <div class="tika-test-body">
        <p class="tika-tip">
          前端不限制文件类型，上传后交给后端 Tika 自动识别并提取文本。
        </p>

        <div class="tika-upload-row">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleTikaFileChange"
            :disabled="tikaParsing"
          >
            <el-button :disabled="tikaParsing">
              <i class="fa-solid fa-paperclip"></i>
              选择任意文件
            </el-button>
          </el-upload>
          <el-button
            type="primary"
            :loading="tikaParsing"
            :disabled="!tikaTestFile"
            @click="handleParseByTika"
          >
            <i class="fa-solid fa-play"></i>
            上传并解析
          </el-button>
          <el-button :disabled="tikaParsing" @click="resetTikaDialogData">
            清空
          </el-button>
        </div>

        <div v-if="tikaTestFile" class="tika-file-card">
          <div class="tika-file-name">
            <i class="fa-regular fa-file-lines"></i>
            {{ tikaTestFile.name }}
          </div>
          <div class="tika-file-meta">
            <span>大小：{{ formatFileSize(tikaTestFile.size) }}</span>
            <span>MIME：{{ tikaTestFile.type || '未提供' }}</span>
          </div>
        </div>

        <div v-if="tikaResult" class="tika-result-card">
          <el-alert
            :title="tikaResult.success ? '解析成功' : '解析失败'"
            :type="tikaResult.success ? 'success' : 'error'"
            :description="tikaResult.success ? '已返回结构化解析结果' : (tikaResult.errorMessage || '未知错误')"
            show-icon
            :closable="false"
          />

          <div class="tika-summary">
            <div class="summary-item">
              <div class="summary-label">检测 MIME</div>
              <div class="summary-value">{{ tikaResult.mimeType || '-' }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">文本长度</div>
              <div class="summary-value">{{ tikaResult.contentLength ?? 0 }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">元数据数量</div>
              <div class="summary-value">{{ tikaMetadataEntries.length }}</div>
            </div>
          </div>

          <div v-if="tikaMetadataEntries.length" class="tika-metadata">
            <h4>元数据</h4>
            <div class="tika-metadata-list">
              <div
                v-for="([key, value], index) in tikaMetadataEntries"
                :key="`meta-${index}`"
                class="metadata-item"
              >
                <span class="metadata-key">{{ key }}</span>
                <span class="metadata-value">{{ value }}</span>
              </div>
            </div>
          </div>

          <div class="tika-content">
            <h4>提取文本预览</h4>
            <el-input
              :model-value="tikaContentPreview"
              type="textarea"
              :rows="12"
              resize="none"
              readonly
            />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { streamAskLearnAgent, getKnowledgeStats, loadKnowledge, parseDocumentByTika } from '@/api/KnowledgeApi'
import { uploadToTemp } from '@/api/fileManagement'
import { useUserStore } from '@/store/user'
import ChatMarkdownRenderer from '@/components/frontend/ChatMarkdownRenderer.vue'

const router = useRouter()

const userStore = useUserStore()
const chatContentRef = ref(null)

// 状态
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const loadingKnowledge = ref(false)
const stats = ref({})
const statsLoaded = computed(() => stats.value.loaded === true)
const streamingMessage = ref(null)
const selectedPdfFiles = ref([])
const tikaDialogVisible = ref(false)
const tikaParsing = ref(false)
const tikaTestFile = ref(null)
const tikaResult = ref(null)
const pendingRemoveUids = new Set()
let streamController = null

// 用户头像
const userAvatar = computed(() => userStore.userInfo?.avatar || '')

// 判断是否为管理员
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const tikaMetadataEntries = computed(() => Object.entries(tikaResult.value?.metadata || {}))
const tikaContentPreview = computed(() => {
  const content = tikaResult.value?.content || ''
  const maxPreviewLength = 30000
  if (content.length <= maxPreviewLength) {
    return content
  }
  return `${content.slice(0, maxPreviewLength)}\n\n...（预览已截断，完整长度 ${content.length} 字符）`
})

const traceStatusClass = (status) => {
  const normalized = (status || '').toUpperCase()
  if (normalized === 'SUCCESS') return 'is-success'
  if (normalized === 'FAILED') return 'is-failed'
  if (normalized === 'RUNNING') return 'is-running'
  if (normalized === 'SKIPPED' || normalized === 'SKIP') return 'is-skipped'
  if (normalized === 'FALLBACK') return 'is-fallback'
  return 'is-default'
}

// 跳转到文章详情
const goToArticle = (articleId) => {
  if (articleId) {
    router.push(`/article/${articleId}`)
  }
}

// 获取知识库状态
const fetchStats = async () => {
  try {
    const data = await getKnowledgeStats()
    stats.value = data || {}
  } catch (error) {
    console.error('获取知识库状态失败:', error)
  }
}

// 加载知识库
const handleLoadKnowledge = async () => {
  loadingKnowledge.value = true
  try {
    await loadKnowledge()
    await fetchStats()
  } catch (error) {
    console.error('加载知识库失败:', error)
  } finally {
    loadingKnowledge.value = false
  }
}

const openTikaDialog = () => {
  tikaDialogVisible.value = true
}

const resetTikaDialogData = () => {
  if (tikaParsing.value) return
  tikaTestFile.value = null
  tikaResult.value = null
}

const handleTikaFileChange = (file) => {
  const rawFile = file?.raw || null
  if (!rawFile) return
  tikaTestFile.value = rawFile
  tikaResult.value = null
}

const formatFileSize = (size) => {
  const bytes = Number(size || 0)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

const handleParseByTika = async () => {
  if (!tikaTestFile.value || tikaParsing.value) {
    return
  }

  tikaParsing.value = true
  try {
    const result = await parseDocumentByTika(tikaTestFile.value, { showDefaultMsg: false })
    tikaResult.value = result || null
    if (result?.success) {
      ElMessage.success('Tika 解析成功')
    } else {
      ElMessage.warning(result?.errorMessage || '解析失败')
    }
  } catch (error) {
    console.error('Tika 解析失败:', error)
    tikaResult.value = {
      success: false,
      errorMessage: error?.message || '请求失败',
      metadata: {},
      content: '',
      contentLength: 0,
      mimeType: ''
    }
    ElMessage.error(error?.message || 'Tika 解析失败')
  } finally {
    tikaParsing.value = false
  }
}

const handlePdfFileChange = (file, fileList) => {
  if (!file?.name) return
  const fileName = file.name.toLowerCase()
  const mimeType = (file.raw?.type || file.type || '').toLowerCase()
  const isPdf = fileName.endsWith('.pdf') || mimeType.includes('pdf')

  if (!isPdf) {
    ElMessage.error('仅支持上传 PDF 文件')
    pendingRemoveUids.add(file.uid)
    selectedPdfFiles.value = fileList.filter(item => item.uid !== file.uid)
    return
  }

  selectedPdfFiles.value = fileList.filter(item => !pendingRemoveUids.has(item.uid))
}

const handlePdfFileRemove = (_file, fileList) => {
  selectedPdfFiles.value = fileList
}

const buildHistory = () => {
  const slices = messages.value.slice(-10)
  return slices
    .map(item => `${item.role === 'user' ? '用户' : '助手'}: ${item.content || ''}`)
    .filter(Boolean)
}

const uploadSelectedPdfs = async () => {
  if (!selectedPdfFiles.value.length) {
    return []
  }

  const attachments = []
  for (const fileItem of selectedPdfFiles.value) {
    const rawFile = fileItem.raw || fileItem
    if (!rawFile) continue
    const tempUrl = await uploadToTemp(rawFile, { showDefaultMsg: false })
    attachments.push({
      fileName: rawFile.name,
      fileUrl: tempUrl,
      mimeType: rawFile.type || 'application/pdf'
    })
  }

  return attachments
}

const updateStepStart = (message, data) => {
  const steps = message.steps || []
  const idx = steps.findIndex(item => item.stepCode === data.stepCode && item.status === 'RUNNING')
  if (idx === -1) {
    steps.push({
      stepCode: data.stepCode,
      status: 'RUNNING',
      costMs: null,
      inputSummary: data.inputSummary || '',
      outputSummary: ''
    })
  }
  message.steps = [...steps]
}

const updateStepFinish = (message, data) => {
  const steps = message.steps || []
  const idx = steps.findIndex(item => item.stepCode === data.stepCode && item.status === 'RUNNING')
  const stepData = {
    stepCode: data.stepCode,
    status: data.status || 'UNKNOWN',
    costMs: data.costMs,
    inputSummary: data.inputSummary || '',
    outputSummary: data.outputSummary || '',
    errorMessage: data.errorMessage || ''
  }
  if (idx >= 0) {
    steps[idx] = stepData
  } else {
    steps.push(stepData)
  }
  message.steps = [...steps]
}

const updateToolStart = (message, data) => {
  const tools = message.tools || []
  tools.push({
    toolName: data.toolName,
    callId: data.callId,
    status: 'RUNNING',
    costMs: null,
    argsPreview: data.argsPreview || '',
    outputPreview: ''
  })
  message.tools = [...tools]
}

const updateToolFinish = (message, data) => {
  const tools = message.tools || []
  const idx = tools.findIndex(item => item.callId === data.callId)
  const toolData = {
    toolName: data.toolName,
    callId: data.callId,
    status: data.status || 'UNKNOWN',
    costMs: data.costMs,
    argsPreview: idx >= 0 ? tools[idx].argsPreview : '',
    outputPreview: data.outputPreview || '',
    errorMessage: data.errorMessage || ''
  }
  if (idx >= 0) {
    tools[idx] = toolData
  } else {
    tools.push(toolData)
  }
  message.tools = [...tools]
}

const updateSubAgentDecision = (message, data) => {
  const subAgents = message.subAgents || []
  const idx = subAgents.findIndex(item => item.subAgentCode === data.subAgentCode)
  const value = {
    subAgentCode: data.subAgentCode,
    decision: data.decision || '',
    reason: data.reason || '',
    status: data.decision === 'SKIP' ? 'SKIPPED' : 'PENDING',
    costMs: null,
    errorMessage: ''
  }
  if (idx >= 0) {
    subAgents[idx] = { ...subAgents[idx], ...value }
  } else {
    subAgents.push(value)
  }
  message.subAgents = [...subAgents]
}

const updateSubAgentStart = (message, data) => {
  const subAgents = message.subAgents || []
  const idx = subAgents.findIndex(item => item.subAgentCode === data.subAgentCode)
  if (idx >= 0) {
    subAgents[idx] = { ...subAgents[idx], status: 'RUNNING' }
  } else {
    subAgents.push({
      subAgentCode: data.subAgentCode,
      decision: 'RUN',
      reason: '',
      status: 'RUNNING',
      costMs: null,
      errorMessage: ''
    })
  }
  message.subAgents = [...subAgents]
}

const updateSubAgentFinish = (message, data) => {
  const subAgents = message.subAgents || []
  const idx = subAgents.findIndex(item => item.subAgentCode === data.subAgentCode)
  const value = {
    subAgentCode: data.subAgentCode,
    status: data.status || 'UNKNOWN',
    costMs: data.costMs,
    errorMessage: data.errorMessage || ''
  }
  if (idx >= 0) {
    subAgents[idx] = { ...subAgents[idx], ...value }
  } else {
    subAgents.push({
      ...value,
      decision: 'RUN',
      reason: ''
    })
  }
  message.subAgents = [...subAgents]
}

const updateSubAgentSkip = (message, data) => {
  const subAgents = message.subAgents || []
  const idx = subAgents.findIndex(item => item.subAgentCode === data.subAgentCode)
  const value = {
    subAgentCode: data.subAgentCode,
    decision: 'SKIP',
    reason: data.reason || '',
    status: 'SKIPPED',
    costMs: 0,
    errorMessage: ''
  }
  if (idx >= 0) {
    subAgents[idx] = { ...subAgents[idx], ...value }
  } else {
    subAgents.push(value)
  }
  message.subAgents = [...subAgents]
}

const handleStop = (silent = false) => {
  if (streamController) {
    streamController.abort()
    streamController = null
    loading.value = false
    if (streamingMessage.value) {
      streamingMessage.value.streaming = false
      streamingMessage.value.content = streamingMessage.value.content || '已手动停止本次问答。'
    }
    streamingMessage.value = null
    if (!silent) {
      ElMessage.info('已停止当前问答')
    }
  }
}

// 发送消息（流式）
const handleSend = async () => {
  const question = inputMessage.value.trim()
  if (!question || loading.value) return

  messages.value.push({
    role: 'user',
    content: question,
    attachments: selectedPdfFiles.value.map(item => ({ fileName: item.name || item.raw?.name || 'PDF' }))
  })
  inputMessage.value = ''
  
  await nextTick()
  scrollToBottom()

  loading.value = true
  let attachments = []

  try {
    attachments = await uploadSelectedPdfs()
  } catch (error) {
    console.error('PDF上传失败:', error)
    loading.value = false
    ElMessage.error(error?.message || 'PDF 上传失败，请稍后重试')
    return
  }

  messages.value.push({
    role: 'assistant',
    content: '',
    sources: [],
    subAgents: [],
    steps: [],
    tools: [],
    streaming: true
  })
  const aiMessageIndex = messages.value.length - 1
  streamingMessage.value = messages.value[aiMessageIndex]
  
  streamController = streamAskLearnAgent(
    {
      question,
      topK: 3,
      history: buildHistory(),
      attachments
    },
    {
      onStepStart: (step) => {
        updateStepStart(messages.value[aiMessageIndex], step || {})
        scrollToBottom()
      },
      onSubAgentDecision: (decision) => {
        updateSubAgentDecision(messages.value[aiMessageIndex], decision || {})
        scrollToBottom()
      },
      onSubAgentStart: (subAgent) => {
        updateSubAgentStart(messages.value[aiMessageIndex], subAgent || {})
        scrollToBottom()
      },
      onSubAgentFinish: (subAgent) => {
        updateSubAgentFinish(messages.value[aiMessageIndex], subAgent || {})
        scrollToBottom()
      },
      onSubAgentSkip: (subAgent) => {
        updateSubAgentSkip(messages.value[aiMessageIndex], subAgent || {})
        scrollToBottom()
      },
      onStepFinish: (step) => {
        updateStepFinish(messages.value[aiMessageIndex], step || {})
        scrollToBottom()
      },
      onToolStart: (tool) => {
        updateToolStart(messages.value[aiMessageIndex], tool || {})
        scrollToBottom()
      },
      onToolFinish: (tool) => {
        updateToolFinish(messages.value[aiMessageIndex], tool || {})
        scrollToBottom()
      },
      onFinal: (result) => {
        const message = messages.value[aiMessageIndex]
        message.streaming = false
        message.content = result?.answer || '未生成有效回答'
        message.sources = result?.sources || []
        if (Array.isArray(result?.steps)) {
          message.steps = result.steps.map(step => ({
            stepCode: step.stepCode,
            status: step.status,
            costMs: step.costMs,
            inputSummary: step.inputSummary,
            outputSummary: step.outputSummary,
            errorMessage: step.errorMessage
          }))
        }
        const decisions = result?.executionContext?.orchestration?.decisions
        if (Array.isArray(decisions)) {
          message.subAgents = decisions.map(item => ({
            subAgentCode: item.subAgentCode,
            decision: item.decision,
            reason: item.reason,
            status: item.status,
            costMs: item.costMs,
            errorMessage: item.errorMessage
          }))
        }

        // 清空已上传附件选择（仅本轮生效）
        selectedPdfFiles.value = []
        scrollToBottom()
      },
      onError: (error) => {
        console.error('流式问答失败:', error)
        const message = messages.value[aiMessageIndex]
        message.streaming = false
        message.content = message.content || error?.message || '抱歉，问答服务暂时不可用，请稍后重试。'
        if (error?.partialResponse?.steps) {
          message.steps = error.partialResponse.steps
        }
        const decisions = error?.partialResponse?.executionContext?.orchestration?.decisions
        if (Array.isArray(decisions)) {
          message.subAgents = decisions.map(item => ({
            subAgentCode: item.subAgentCode,
            decision: item.decision,
            reason: item.reason,
            status: item.status,
            costMs: item.costMs,
            errorMessage: item.errorMessage
          }))
        }
        streamingMessage.value = null
        loading.value = false
        streamController = null
        scrollToBottom()
      },
      onDone: () => {
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].streaming = false
        }
        streamingMessage.value = null
        loading.value = false
        streamController = null
        scrollToBottom()
      }
    }
  )
}

// 滚动到底部
const scrollToBottom = () => {
  if (chatContentRef.value) {
    chatContentRef.value.scrollTop = chatContentRef.value.scrollHeight
  }
}

onUnmounted(() => {
  handleStop(true)
})

onMounted(() => {
  fetchStats()
  document.title = '学习版智能体问答 - 个人博客'
})
</script>


<style lang="scss" scoped>
// 核心颜色定义（与首页保持一致）
$primary-blue: rgb(7, 26, 242);
$primary-purple: #a855f7;
$bg-light: #f8fafc;
$glass-bg: rgba(255, 255, 255, 0.95);
$border-color: rgba(226, 232, 240);
$text-primary: #1f2937;
$text-secondary: #6b7280;

.knowledge-chat-container {
  min-height: calc(100vh - 80px);
  padding: 2rem 1rem;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

// 装饰背景
.decorative-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
  
  .blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(100px);
    opacity: 0.12;
    
    &.blob-1 {
      width: 500px;
      height: 500px;
      background: $primary-blue;
      top: -10%;
      left: 10%;
      animation: float 20s ease-in-out infinite;
    }
    
    &.blob-2 {
      width: 400px;
      height: 400px;
      background: $primary-purple;
      top: 30%;
      right: 5%;
      animation: float 25s ease-in-out infinite reverse;
    }
    
    &.blob-3 {
      width: 350px;
      height: 350px;
      background: #22c55e;
      bottom: 10%;
      left: 30%;
      animation: float 22s ease-in-out infinite;
    }
  }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(20px, 10px) scale(1.02); }
}

// 聊天主体卡片
.chat-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 900px;
  height: calc(100vh - 140px);
  min-height: 500px;
  display: flex;
  flex-direction: column;
  background: $glass-bg;
  border: 1px solid $border-color;
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

// 头部
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  background: linear-gradient(135deg, $primary-blue 0%, #4f46e5 100%);
  border-bottom: 1px solid $border-color;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.header-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  
  i {
    font-size: 1.5rem;
    color: #fff;
  }
}

.header-info {
  .chat-title {
    margin: 0;
    font-size: 1.25rem;
    font-weight: 700;
    color: #fff;
  }
  
  .chat-subtitle {
    margin: 0.25rem 0 0;
    font-size: 0.8rem;
    color: rgba(255, 255, 255, 0.8);
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  
  i {
    font-size: 0.7rem;
  }
}

.reload-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  color: #fff;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(255, 255, 255, 0.3);
  }
  
  i {
    font-size: 0.85rem;
  }
}

.tika-test-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  background: rgba(16, 185, 129, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 8px;
  color: #fff;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(16, 185, 129, 0.36);
  }
}

// 聊天内容区
.chat-content {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #ddd;
    border-radius: 3px;
    
    &:hover {
      background: #ccc;
    }
  }
}

// 欢迎区域
.welcome-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.welcome-icon-wrapper {
  position: relative;
  margin-bottom: 1.5rem;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, $primary-blue 0%, #4f46e5 100%);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
  
  i {
    font-size: 2.5rem;
    color: #fff;
  }
}

.welcome-pulse {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80px;
  height: 80px;
  background: $primary-blue;
  border-radius: 24px;
  opacity: 0.3;
  animation: pulse 2s ease-out infinite;
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.3;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.5);
    opacity: 0;
  }
}

.welcome-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 0.5rem;
}

.welcome-desc {
  font-size: 0.95rem;
  color: $text-secondary;
  margin: 0 0 2rem;
  max-width: 400px;
}

// 消息项
.message-item {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  animation: fadeInUp 0.3s ease;
  
  &.user {
    flex-direction: row-reverse;
    
    .message-bubble {
      background: linear-gradient(135deg, $primary-blue 0%, #4f46e5 100%);
      color: #fff;
      border-radius: 20px 20px 4px 20px;
    }
    
    .message-text {
      color: #fff;
    }
  }
  
  &.assistant {
    .message-bubble {
      background: #fff;
      border: 1px solid $border-color;
      border-radius: 20px 20px 20px 4px;
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  flex-shrink: 0;
}

.user-avatar {
  border: 2px solid $primary-blue;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, $primary-blue 0%, #4f46e5 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  
  i {
    font-size: 1.1rem;
    color: #fff;
  }
  
  &.thinking {
    animation: thinking 1s ease-in-out infinite;
  }
}

@keyframes thinking {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.message-bubble {
  max-width: 75%;
  padding: 1rem 1.25rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.message-text {
  font-size: 0.95rem;
  line-height: 1.7;
  
  &.user-text {
    white-space: pre-wrap;
  }
}

.message-attachments {
  margin-top: 0.75rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.attachment-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.28rem 0.6rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.26);
  font-size: 0.72rem;
}

.cursor-blink {
  animation: blink 1s infinite;
  font-weight: bold;
  color: $primary-blue;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

// 来源文档
.message-sources {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px dashed rgba(0, 0, 0, 0.1);
}

.trace-panel {
  margin-top: 0.9rem;
  border-top: 1px dashed rgba(148, 163, 184, 0.4);
  padding-top: 0.8rem;
}

.trace-title {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.78rem;
  color: $text-secondary;
  margin-bottom: 0.45rem;
}

.trace-section {
  margin-bottom: 0.45rem;
}

.trace-subtitle {
  font-size: 0.72rem;
  color: #64748b;
  margin-bottom: 0.35rem;
}

.trace-item {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.28rem;
  font-size: 0.72rem;
}

.trace-name {
  color: #334155;
  max-width: 45%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trace-status {
  padding: 0.08rem 0.35rem;
  border-radius: 4px;
  font-size: 0.68rem;
  border: 1px solid #d1d5db;
  color: #6b7280;

  &.is-success {
    color: #166534;
    border-color: #86efac;
    background: #dcfce7;
  }

  &.is-failed {
    color: #991b1b;
    border-color: #fca5a5;
    background: #fee2e2;
  }

  &.is-running {
    color: #1d4ed8;
    border-color: #93c5fd;
    background: #dbeafe;
  }

  &.is-skipped {
    color: #475569;
    border-color: #cbd5e1;
    background: #f1f5f9;
  }

  &.is-fallback {
    color: #854d0e;
    border-color: #fcd34d;
    background: #fef3c7;
  }
}

.trace-cost {
  color: #64748b;
}

.sources-header {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: $text-secondary;
  margin-bottom: 0.5rem;
  
  i {
    color: $primary-blue;
  }
}

.sources-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.source-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.6rem;
  background: rgba(7, 26, 242, 0.08);
  border-radius: 6px;
  font-size: 0.75rem;
  color: $primary-blue;
  
  i {
    font-size: 0.7rem;
  }
  
  .link-icon {
    font-size: 0.6rem;
    opacity: 0;
    transition: opacity 0.2s ease;
    margin-left: 0.2rem;
  }
  
  &.clickable {
    cursor: pointer;
    transition: all 0.2s ease;
    
    &:hover {
      background: rgba(7, 26, 242, 0.15);
      transform: translateY(-1px);
      
      .link-icon {
        opacity: 1;
      }
    }
  }
}

// 打字指示器
.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 0.5rem 0;
  
  span {
    width: 8px;
    height: 8px;
    background: $primary-blue;
    border-radius: 50%;
    animation: typing 1.4s infinite ease-in-out;
    
    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 60%, 100% { 
    transform: translateY(0); 
    opacity: 0.4; 
  }
  30% { 
    transform: translateY(-6px); 
    opacity: 1; 
  }
}

// 输入区域
.chat-input-area {
  padding: 1rem 1.5rem 1.25rem;
  background: #fff;
  border-top: 1px solid $border-color;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border-radius: 8px;
  font-size: 0.78rem;
}

.tika-test-body {
  .tika-tip {
    margin: 0;
    font-size: 0.86rem;
    color: $text-secondary;
    line-height: 1.6;
  }

  .tika-upload-row {
    margin-top: 0.9rem;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-wrap: wrap;
  }

  .tika-file-card {
    margin-top: 0.85rem;
    padding: 0.8rem 0.9rem;
    background: #f8fafc;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
  }

  .tika-file-name {
    font-size: 0.92rem;
    color: $text-primary;
    display: flex;
    align-items: center;
    gap: 0.35rem;
    font-weight: 600;
  }

  .tika-file-meta {
    margin-top: 0.5rem;
    display: flex;
    gap: 1rem;
    font-size: 0.8rem;
    color: $text-secondary;
    flex-wrap: wrap;
  }

  .tika-result-card {
    margin-top: 1rem;
    padding: 1rem;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    background: #fff;
  }

  .tika-summary {
    margin-top: 0.85rem;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
    gap: 0.75rem;
  }

  .summary-item {
    background: #f8fafc;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    padding: 0.65rem 0.75rem;
  }

  .summary-label {
    font-size: 0.76rem;
    color: $text-secondary;
    margin-bottom: 0.28rem;
  }

  .summary-value {
    font-size: 0.88rem;
    color: $text-primary;
    line-height: 1.5;
    word-break: break-word;
  }

  .tika-metadata,
  .tika-content {
    margin-top: 1rem;

    h4 {
      margin: 0 0 0.6rem;
      font-size: 0.92rem;
      color: $text-primary;
      font-weight: 600;
    }
  }

  .tika-metadata-list {
    display: flex;
    flex-direction: column;
    gap: 0.55rem;
    max-height: 220px;
    overflow-y: auto;
    padding-right: 0.25rem;
  }

  .metadata-item {
    display: grid;
    grid-template-columns: minmax(120px, 180px) 1fr;
    gap: 0.7rem;
    align-items: start;
    font-size: 0.8rem;
    border: 1px solid #f1f5f9;
    background: #fafafa;
    border-radius: 8px;
    padding: 0.55rem 0.65rem;
  }

  .metadata-key {
    color: #1e3a8a;
    font-weight: 600;
    word-break: break-all;
  }

  .metadata-value {
    color: #374151;
    word-break: break-word;
  }
}


.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
  background: #f8fafc;
  border: 1px solid $border-color;
  border-radius: 16px;
  padding: 0.5rem;
  transition: all 0.3s ease;
  
  &:focus-within {
    border-color: $primary-blue;
    box-shadow: 0 0 0 3px rgba(7, 26, 242, 0.1);
  }
}

.chat-input {
  flex: 1;
  
  :deep(.el-textarea__inner) {
    border: none;
    background: transparent;
    box-shadow: none;
    padding: 0.5rem 0.75rem;
    font-size: 0.95rem;
    resize: none;
    
    &:focus {
      box-shadow: none;
    }
    
    &::placeholder {
      color: #9ca3af;
    }
  }
}

.send-btn {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, $primary-blue 0%, #4f46e5 100%);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  
  &:hover:not(.disabled) {
    transform: scale(1.05);
    box-shadow: 0 4px 12px rgba(7, 26, 242, 0.3);
  }
  
  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.stop-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #fca5a5;
  background: #fff1f2;
  color: #be123c;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover {
    background: #ffe4e6;
  }
}

.input-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  margin: 0.75rem 0 0;
  font-size: 0.75rem;
  color: #9ca3af;
  
  i {
    font-size: 0.7rem;
  }
}

// 响应式
@media (max-width: 768px) {
  .knowledge-chat-container {
    padding: 1rem 0.5rem;
  }
  
  .chat-wrapper {
    height: calc(100vh - 100px);
    border-radius: 16px;
  }
  
  .chat-header {
    flex-direction: column;
    gap: 1rem;
    padding: 1rem;
  }
  
  .header-left {
    width: 100%;
  }
  
  .header-actions {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .tika-test-body {
    .metadata-item {
      grid-template-columns: 1fr;
      gap: 0.4rem;
    }
  }
  
  .message-bubble {
    max-width: 85%;
  }
  
  .welcome-section {
    padding: 2rem 1rem;
  }
  
  .welcome-icon {
    width: 64px;
    height: 64px;
    
    i {
      font-size: 2rem;
    }
  }
  
  .welcome-pulse {
    width: 64px;
    height: 64px;
  }
  
}
</style>
