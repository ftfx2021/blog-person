import request, { aiRequest } from "@/utils/request";
import { createAiRequestWrapper } from "@/utils/aiRequestUtils";

/**
 * 知识库问答（原始版本）
 */
function _askKnowledge(params, callbacks = {}) {
  return aiRequest.post('/knowledge/ask', params, callbacks);
}

/**
 * 知识库问答（增强版本，带超时处理和重试）
 * @param {object} params - 请求参数
 * @param {string} params.question - 用户问题
 * @param {number} [params.topK=5] - 返回相关文档数量
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export const askKnowledge = createAiRequestWrapper(_askKnowledge);

/**
 * 流式知识库问答（SSE）- 使用原生 fetch 实现
 * @param {object} params - 请求参数
 * @param {string} params.question - 用户问题
 * @param {number} [params.topK=5] - 返回相关文档数量
 * @param {object} callbacks - 回调函数
 * @param {function} callbacks.onContent - 收到内容片段时的回调
 * @param {function} callbacks.onSources - 收到来源信息时的回调
 * @param {function} callbacks.onDone - 完成时的回调
 * @param {function} callbacks.onError - 错误时的回调
 * @returns {object} 包含 abort 方法的对象
 */
export function streamAskKnowledge(params, callbacks = {}) {
  return streamSsePost('/knowledge/ask/stream', params, callbacks, (payload) => {
    if (payload.type === 'content' && payload.data && callbacks.onContent) {
      callbacks.onContent(payload.data)
    } else if (payload.type === 'source' && payload.data && callbacks.onSources) {
      callbacks.onSources([payload.data])
    } else if (payload.type === 'done' && callbacks.onDone) {
      callbacks.onDone()
    } else if (payload.type === 'error' && callbacks.onError) {
      callbacks.onError(new Error(payload?.data?.message || payload.data || '流式问答失败'))
    }
  })
}

/**
 * 学习版智能体流式问答（步骤 + 工具调用追踪）
 * @param {object} params - 请求参数
 * @param {string} params.question - 用户问题
 * @param {number} [params.topK=5] - 检索数量
 * @param {string[]} [params.history=[]] - 历史对话
 * @param {Array<{fileName:string,fileUrl:string,mimeType:string}>} [params.attachments=[]] - PDF附件
 * @param {object} callbacks - 回调
 */
export function streamAskLearnAgent(params, callbacks = {}) {
  return streamSsePost('/knowledge/agent/ask/stream', params, callbacks, (payload) => {
    const type = payload?.type
    const data = payload?.data

    if (type === 'step_start' && callbacks.onStepStart) {
      callbacks.onStepStart(data)
      return
    }
    if (type === 'step_finish' && callbacks.onStepFinish) {
      callbacks.onStepFinish(data)
      return
    }
    if (type === 'tool_start' && callbacks.onToolStart) {
      callbacks.onToolStart(data)
      return
    }
    if (type === 'tool_finish' && callbacks.onToolFinish) {
      callbacks.onToolFinish(data)
      return
    }
    if (type === 'subagent_decision' && callbacks.onSubAgentDecision) {
      callbacks.onSubAgentDecision(data)
      return
    }
    if (type === 'subagent_start' && callbacks.onSubAgentStart) {
      callbacks.onSubAgentStart(data)
      return
    }
    if (type === 'subagent_finish' && callbacks.onSubAgentFinish) {
      callbacks.onSubAgentFinish(data)
      return
    }
    if (type === 'subagent_skip' && callbacks.onSubAgentSkip) {
      callbacks.onSubAgentSkip(data)
      return
    }
    if (type === 'final' && callbacks.onFinal) {
      callbacks.onFinal(data)
      return
    }
    if (type === 'error' && callbacks.onError) {
      const message = data?.message || data || '智能体执行失败'
      const error = new Error(message)
      error.partialResponse = data?.partialResponse
      callbacks.onError(error)
      return
    }
    if (type === 'done' && callbacks.onDone) {
      callbacks.onDone()
    }
  })
}

function streamSsePost(path, params, callbacks, eventHandler) {
  const baseUrl = import.meta.env.VITE_APP_BASE_API || '/api'
  const token = localStorage.getItem('token')
  const controller = new AbortController()

  fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      token: token || ''
    },
    body: JSON.stringify(params),
    signal: controller.signal
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      if (!response.body) {
        throw new Error('当前浏览器不支持流式响应')
      }
      return parseSseBody(response.body, eventHandler, callbacks)
    })
    .catch(error => {
      if (error.name !== 'AbortError' && callbacks.onError) {
        callbacks.onError(error)
      }
    })

  return {
    abort: () => controller.abort()
  }
}

async function parseSseBody(body, eventHandler, callbacks = {}) {
  const reader = body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      buffer = consumeSseBuffer(buffer, eventHandler)
    }

    if (buffer.trim()) {
      buffer = consumeSseBuffer(buffer, eventHandler, true)
    }

    if (buffer.trim()) {
      // 兜底：某些情况下服务端会直接输出纯 JSON（无 data: 前缀）
      try {
        eventHandler(JSON.parse(buffer.trim()))
      } catch (e) {
        console.warn('SSE尾包无法解析:', buffer, e)
      }
    }
  } catch (error) {
    if (error.name !== 'AbortError' && callbacks.onError) {
      callbacks.onError(error)
    }
  }
}

function consumeSseBuffer(rawBuffer, eventHandler, flushTail = false) {
  let cursor = 0
  const buffer = rawBuffer || ''

  while (cursor < buffer.length) {
    const dataIdx = buffer.indexOf('data:', cursor)
    if (dataIdx === -1) {
      break
    }

    let jsonStart = dataIdx + 5
    while (jsonStart < buffer.length && /\s/.test(buffer[jsonStart])) {
      jsonStart++
    }

    // 仅处理 JSON 事件体；非 JSON 行跳过到下一行
    if (buffer[jsonStart] !== '{') {
      const nextLine = buffer.indexOf('\n', jsonStart)
      if (nextLine === -1) {
        break
      }
      cursor = nextLine + 1
      continue
    }

    const { complete, endIndex } = findJsonObjectEnd(buffer, jsonStart)
    if (!complete) {
      break
    }

    const jsonText = buffer.slice(jsonStart, endIndex + 1)
    try {
      eventHandler(JSON.parse(jsonText))
    } catch (e) {
      console.warn('SSE事件JSON解析失败:', jsonText, e)
    }

    cursor = endIndex + 1
    while (cursor < buffer.length && /[\r\n]/.test(buffer[cursor])) {
      cursor++
    }
  }

  const remain = buffer.slice(cursor)
  if (flushTail && remain.trim()) {
    // flush 模式下如果尾部正好是一个完整的 data:{...} 也尝试解析
    const idx = remain.indexOf('data:')
    if (idx >= 0) {
      const tail = remain.slice(idx + 5).trim()
      try {
        eventHandler(JSON.parse(tail))
        return ''
      } catch {
        // ignore
      }
    }
  }
  return remain
}

function findJsonObjectEnd(text, startIndex) {
  let depth = 0
  let inString = false
  let escaped = false

  for (let i = startIndex; i < text.length; i++) {
    const ch = text[i]

    if (inString) {
      if (escaped) {
        escaped = false
        continue
      }
      if (ch === '\\') {
        escaped = true
        continue
      }
      if (ch === '"') {
        inString = false
      }
      continue
    }

    if (ch === '"') {
      inString = true
      continue
    }

    if (ch === '{') {
      depth++
      continue
    }

    if (ch === '}') {
      depth--
      if (depth === 0) {
        return { complete: true, endIndex: i }
      }
    }
  }

  return { complete: false, endIndex: -1 }
}

/**
 * 搜索知识库
 * @param {string} question - 搜索问题
 * @param {number} [topK=5] - 返回结果数量
 * @returns {Promise}
 */
export function searchKnowledge(question, topK = 5) {
  return request.get('/knowledge/search', { question, topK });
}

/**
 * 加载知识库
 * @returns {Promise}
 */
export function loadKnowledge() {
  return request.post('/knowledge/load', {}, {
    successMsg: '知识库加载成功',
    errorMsg: '知识库加载失败'
  });
}

/**
 * 获取知识库状态
 * @returns {Promise}
 */
export function getKnowledgeStats() {
  return request.get('/knowledge/stats', {}, {
    showDefaultMsg: false
  });
}

/**
 * 临时接口：调用 Tika 文档解析
 * @param {File} file - 任意文件（由后端 Tika 自动识别类型）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise<object>} ParseResult
 */
export function parseDocumentByTika(file, callbacks = {}) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/doc/parse', formData, {
    showDefaultMsg: false,
    ...callbacks
  })
}

/**
 * 向量化单篇文章
 * @param {string} articleId - 文章ID
 * @param {object} callbacks - 回调函数
 * @returns {Promise}
 */
export function vectorizeArticle(articleId, callbacks = {}) {
  return request.post(`/knowledge/vectorize/${articleId}`, {}, {
    successMsg: '向量化成功',
    errorMsg: '向量化失败',
    ...callbacks
  });
}
