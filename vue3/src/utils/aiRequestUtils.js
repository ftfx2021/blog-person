/**
 * AI请求超时和轮询处理工具
 * 专门用于处理AI生成请求的超时、重试和轮询机制
 */

import { ElMessage, ElNotification } from 'element-plus'

/**
 * AI请求配置
 */
const AI_REQUEST_CONFIG = {
  // 基础超时时间（秒）
  BASE_TIMEOUT: 30,
  // 根据内容长度动态调整的超时时间
  DYNAMIC_TIMEOUT: {
    SHORT: 30,    // 短内容（<500字）
    MEDIUM: 60,   // 中等内容（500-2000字）
    LONG: 120,    // 长内容（>2000字）
    VERY_LONG: 180 // 超长内容（>5000字）
  },
  // 重试配置
  RETRY: {
    MAX_ATTEMPTS: 3,        // 最大重试次数
    INITIAL_DELAY: 2000,    // 初始重试延迟（毫秒）
    BACKOFF_MULTIPLIER: 1.5 // 退避乘数
  },
  // 轮询配置
  POLLING: {
    INTERVAL: 2000,         // 轮询间隔（毫秒）
    MAX_DURATION: 300000,   // 最大轮询时长（5分钟）
    PROGRESS_MESSAGES: [    // 进度提示消息
      '正在分析内容...',
      '正在生成结果...',
      '正在优化输出...',
      '即将完成...'
    ]
  }
}

/**
 * 根据内容长度计算超时时间
 * @param {string} content - 文章内容
 * @returns {number} 超时时间（秒）
 */
export function calculateTimeout(content = '') {
  const length = content.length
  
  if (length < 500) {
    return AI_REQUEST_CONFIG.DYNAMIC_TIMEOUT.SHORT
  } else if (length < 2000) {
    return AI_REQUEST_CONFIG.DYNAMIC_TIMEOUT.MEDIUM
  } else if (length < 5000) {
    return AI_REQUEST_CONFIG.DYNAMIC_TIMEOUT.LONG
  } else {
    return AI_REQUEST_CONFIG.DYNAMIC_TIMEOUT.VERY_LONG
  }
}

/**
 * 获取建议的超时时间配置
 * @param {string} content - 文章内容
 * @returns {object} 包含超时时间和相关信息的配置对象
 */
export function getSuggestedTimeout(content = '') {
  const timeout = calculateTimeout(content)
  const contentLength = content.length
  
  return {
    timeout: timeout * 1000, // 转换为毫秒
    timeoutSeconds: timeout,
    contentLength,
    level: contentLength < 500 ? 'short' : 
           contentLength < 2000 ? 'medium' : 
           contentLength < 5000 ? 'long' : 'very_long',
    message: `内容长度: ${contentLength}字，预计处理时间: ${timeout}秒`
  }
}

/**
 * 检查内容长度是否合适进行AI处理
 * @param {string} content - 文章内容
 * @param {number} minLength - 最小长度要求
 * @returns {object} 检查结果
 */
export function checkContentLength(content = '', minLength = 20) {
  const length = content.length
  
  return {
    isValid: length >= minLength,
    length,
    minLength,
    message: length < minLength ? 
      `内容太短，至少需要${minLength}字，当前${length}字` : 
      `内容长度合适（${length}字）`
  }
}

/**
 * AI请求重试机制
 * @param {Function} requestFn - 请求函数
 * @param {object} params - 请求参数
 * @param {object} options - 重试选项
 * @returns {Promise} 请求结果
 */
export async function retryAiRequest(requestFn, params, options = {}) {
  const {
    maxAttempts = AI_REQUEST_CONFIG.RETRY.MAX_ATTEMPTS,
    initialDelay = AI_REQUEST_CONFIG.RETRY.INITIAL_DELAY,
    backoffMultiplier = AI_REQUEST_CONFIG.RETRY.BACKOFF_MULTIPLIER,
    onRetry = () => {}
  } = options

  let lastError = null
  let delay = initialDelay

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      console.log(`AI请求尝试 ${attempt}/${maxAttempts}`)
      
      const result = await requestFn(params)
      
      if (attempt > 1) {
        ElMessage.success(`第${attempt}次尝试成功！`)
      }
      
      return result
      
    } catch (error) {
      lastError = error
      
      console.warn(`AI请求第${attempt}次失败:`, error.message)
      
      // 如果是最后一次尝试，直接抛出错误
      if (attempt === maxAttempts) {
        break
      }
      
      // 显示重试提示
      ElMessage.warning(`第${attempt}次尝试失败，${delay/1000}秒后重试...`)
      onRetry(attempt, error)
      
      // 等待后重试
      await new Promise(resolve => setTimeout(resolve, delay))
      delay *= backoffMultiplier
    }
  }
  
  // 所有尝试都失败了
  ElMessage.error(`AI请求失败，已重试${maxAttempts}次`)
  throw lastError
}

/**
 * 带进度提示的AI请求
 * @param {Function} requestFn - 请求函数
 * @param {object} params - 请求参数
 * @param {object} options - 选项
 * @returns {Promise} 请求结果
 */
export async function aiRequestWithProgress(requestFn, params, options = {}) {
  const {
    timeout = calculateTimeout(params.content) * 1000,
    showProgress = true,
    progressTitle = 'AI正在处理中',
    enableRetry = true
  } = options

  let progressNotification = null
  let progressMessageIndex = 0
  let progressInterval = null

  try {
    // 显示进度通知
    if (showProgress) {
      progressNotification = ElNotification({
        title: progressTitle,
        message: AI_REQUEST_CONFIG.POLLING.PROGRESS_MESSAGES[0],
        type: 'info',
        duration: 0, // 不自动关闭
        showClose: false
      })

      // 定期更新进度消息
      progressInterval = setInterval(() => {
        progressMessageIndex = (progressMessageIndex + 1) % AI_REQUEST_CONFIG.POLLING.PROGRESS_MESSAGES.length
        if (progressNotification) {
          progressNotification.message = AI_REQUEST_CONFIG.POLLING.PROGRESS_MESSAGES[progressMessageIndex]
        }
      }, 3000)
    }

    // 执行请求（带重试机制）
    const executeRequest = enableRetry ? 
      () => retryAiRequest(requestFn, params, { timeout }) :
      () => requestFn(params)

    const result = await Promise.race([
      executeRequest(),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error('请求超时')), timeout)
      )
    ])

    // 成功完成
    if (progressNotification) {
      progressNotification.close()
      ElNotification({
        title: '处理完成',
        message: 'AI处理成功完成！',
        type: 'success',
        duration: 3000
      })
    }

    return result

  } catch (error) {
    // 处理失败
    if (progressNotification) {
      progressNotification.close()
      ElNotification({
        title: '处理失败',
        message: error.message || 'AI处理失败，请稍后重试',
        type: 'error',
        duration: 5000
      })
    }
    throw error

  } finally {
    // 清理资源
    if (progressInterval) {
      clearInterval(progressInterval)
    }
  }
}

/**
 * 轮询检查AI任务状态（适用于异步任务）
 * @param {string} taskId - 任务ID
 * @param {Function} checkStatusFn - 检查状态的函数
 * @param {object} options - 轮询选项
 * @returns {Promise} 最终结果
 */
export async function pollAiTaskStatus(taskId, checkStatusFn, options = {}) {
  const {
    interval = AI_REQUEST_CONFIG.POLLING.INTERVAL,
    maxDuration = AI_REQUEST_CONFIG.POLLING.MAX_DURATION,
    onProgress = () => {},
    showProgress = true
  } = options

  const startTime = Date.now()
  let progressNotification = null

  if (showProgress) {
    progressNotification = ElNotification({
      title: '任务处理中',
      message: '正在查询任务状态...',
      type: 'info',
      duration: 0,
      showClose: false
    })
  }

  try {
    while (Date.now() - startTime < maxDuration) {
      const status = await checkStatusFn(taskId)
      
      onProgress(status)
      
      // 任务完成
      if (status.completed) {
        if (progressNotification) {
          progressNotification.close()
          ElNotification({
            title: '任务完成',
            message: 'AI任务处理完成！',
            type: 'success',
            duration: 3000
          })
        }
        return status.result
      }
      
      // 任务失败
      if (status.failed) {
        throw new Error(status.error || '任务执行失败')
      }
      
      // 更新进度消息
      if (progressNotification && status.message) {
        progressNotification.message = status.message
      }
      
      // 等待下次轮询
      await new Promise(resolve => setTimeout(resolve, interval))
    }
    
    // 轮询超时
    throw new Error('任务处理超时')
    
  } catch (error) {
    if (progressNotification) {
      progressNotification.close()
      ElNotification({
        title: '任务失败',
        message: error.message || '任务处理失败',
        type: 'error',
        duration: 5000
      })
    }
    throw error
  }
}

/**
 * 创建带超时和重试的AI请求包装器
 * @param {Function} originalRequestFn - 原始请求函数
 * @returns {Function} 增强后的请求函数
 */
export function createAiRequestWrapper(originalRequestFn) {
  return async (params, callbacks = {}) => {
    const timeoutConfig = getSuggestedTimeout(params.content)
    
    // 合并回调函数
    const enhancedCallbacks = {
      ...callbacks,
      onSuccess: (data) => {
        console.log('AI请求成功:', data)
        if (callbacks.onSuccess) {
          callbacks.onSuccess(data)
        }
      },
      onError: (error) => {
        console.error('AI请求失败:', error)
        if (callbacks.onError) {
          callbacks.onError(error)
        }
      }
    }

    // 使用增强的请求处理
    return aiRequestWithProgress(
      (requestParams) => originalRequestFn(requestParams, enhancedCallbacks),
      params,
      {
        timeout: timeoutConfig.timeout,
        progressTitle: '正在处理AI请求',
        enableRetry: true
      }
    )
  }
}

/**
 * 批量AI请求处理（避免并发过多）
 * @param {Array} requests - 请求列表
 * @param {object} options - 选项
 * @returns {Promise} 所有请求的结果
 */
export async function batchAiRequests(requests, options = {}) {
  const {
    concurrency = 2, // 并发数量
    delayBetweenBatches = 1000 // 批次间延迟
  } = options

  const results = []
  const errors = []

  for (let i = 0; i < requests.length; i += concurrency) {
    const batch = requests.slice(i, i + concurrency)
    
    try {
      const batchResults = await Promise.allSettled(
        batch.map(request => request())
      )
      
      batchResults.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          results[i + index] = result.value
        } else {
          errors[i + index] = result.reason
        }
      })
      
      // 批次间延迟
      if (i + concurrency < requests.length) {
        await new Promise(resolve => setTimeout(resolve, delayBetweenBatches))
      }
      
    } catch (error) {
      console.error(`批次 ${Math.floor(i/concurrency) + 1} 处理失败:`, error)
    }
  }

  return { results, errors }
}

export default {
  calculateTimeout,
  getSuggestedTimeout,
  checkContentLength,
  retryAiRequest,
  aiRequestWithProgress,
  pollAiTaskStatus,
  createAiRequestWrapper,
  batchAiRequests,
  AI_REQUEST_CONFIG
}

