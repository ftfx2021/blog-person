import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

/**
 * 增强的 axios 请求工具
 *
 * 新增特性：
 * 1. 与用户状态管理集成
 * 2. 智能错误处理和重试机制
 * 3. 请求去重和缓存
 * 4. 详细的错误日志
 * 5. 请求取消支持
 *
 * 配置选项：
 * @param {boolean} showDefaultMsg - 是否显示默认提示，默认 true
 * @param {string} successMsg - 自定义成功提示
 * @param {string} errorMsg - 自定义错误提示
 * @param {Function} onSuccess - 成功回调
 * @param {Function} onError - 错误回调
 * @param {boolean} enableRetry - 是否启用重试，默认 false
 * @param {number} retryCount - 重试次数，默认 3
 * @param {boolean} enableCache - 是否启用缓存，默认 false
 * @param {number} cacheTime - 缓存时间(ms)，默认 5分钟

 */

// 请求缓存
const requestCache = new Map()
// 请求计数器（用于生成唯一ID）
let requestId = 0

// 生成缓存key
function generateCacheKey(config) {
  const { method, url, params, data } = config
  return `${method}:${url}:${JSON.stringify(params)}:${JSON.stringify(data)}`
}

// 错误类型枚举
const ErrorTypes = {
  NETWORK: 'network',
  BUSINESS: 'business',
  HTTP: 'http',
  TIMEOUT: 'timeout',
  CANCEL: 'cancel'
}

// 创建 axios 实例
const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// 创建AI专用的 axios 实例（更长的超时时间）
const aiService = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
  timeout: 120000, // AI请求2分钟超时
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// 工具函数：安全获取用户store
function getUserStore() {
  try {
    return useUserStore()
  } catch (error) {
    console.warn('无法获取用户store，可能在setup外部调用')
    return null
  }
}

// 工具函数：获取token
function getAuthToken() {
  const userStore = getUserStore()
  if (userStore && userStore.isLoggedIn) {
    return userStore.token
  }
  // 降级方案：直接从localStorage获取
  return localStorage.getItem('token')
}

// 防止重复弹窗的标志
let isHandlingTokenExpired = false

// 工具函数：处理token过期
function handleTokenExpired() {
  // 防止重复处理
  if (isHandlingTokenExpired) {
    return
  }
  isHandlingTokenExpired = true

  console.warn('🔒 Token已失效，清除用户信息并跳转到登录页')

  // 清除用户信息
  const userStore = getUserStore()
  if (userStore) {
    userStore.clearUserInfo()
  } else {
    // 降级方案：直接清理localStorage
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('role')
    localStorage.removeItem('menus')
  }

  // 保存当前路径，登录后可以跳转回来
  const currentPath = window.location.pathname + window.location.search
  if (currentPath !== '/login') {
    sessionStorage.setItem('redirectPath', currentPath)
  }

  // 显示提示消息（不阻塞跳转）
  ElMessage.warning('登录已过期，请重新登录')

  // 延迟500ms后跳转，让用户看到提示
  setTimeout(() => {
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    // 重置标志
    isHandlingTokenExpired = false
  }, 500)
}

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 生成请求ID
    config.requestId = ++requestId
    config.requestTime = Date.now()


    // 处理文件上传：如果data是FormData，删除Content-Type让浏览器自动设置
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
      console.log('📤 检测到FormData，自动设置multipart/form-data')
    }

    // 添加认证token
    const token = getAuthToken()
    if (token) {
      config.headers['token'] = token
    }

    console.log(`📤 发送请求 [${config.requestId}]:`, {
      method: config.method?.toUpperCase(),
      url: config.url,
      isFormData: config.data instanceof FormData
    })

    return config
  },
  error => {
    console.error('请求拦截器错误:', error)
    return Promise.reject({
      type: ErrorTypes.NETWORK,
      message: '请求配置错误',
      originalError: error
    })
  }
)

// 统一的响应处理函数
function handleResponse(data, config) {
  const requestTime = Date.now() - (config.requestTime || 0)

  console.log(`📥 收到响应 [${config.requestId}]:`, {
    method: config.method?.toUpperCase(),
    url: config.url,
    code: data.code,
    time: `${requestTime}ms`
  })

  // 缓存GET请求的成功响应
  if (config.enableCache && config.method?.toLowerCase() === 'get' && data.code === "200") {
    const cacheKey = generateCacheKey(config)
    requestCache.set(cacheKey, {
      data: data.data,
      timestamp: Date.now()
    })
  }

  if (data.code === "200") {
    // 成功处理
    try {
      if (config.successMsg) {
        ElMessage.success(config.successMsg)
      } else if (config.showDefaultMsg !== false && config.method?.toLowerCase() !== 'get') {
        ElMessage.success('操作成功')
      }

      if (typeof config.onSuccess === 'function') {
        config.onSuccess(data.data)
      }

      return data.data
    } catch (err) {
      console.error('成功回调执行错误:', err)
      return data.data
    }
  } else {
    // 业务错误处理
    const errorInfo = {
      type: ErrorTypes.BUSINESS,
      code: data.code,
      message: data.msg || '请求失败',
      data: data.data,
      requestId: config.requestId
    }

    // 特殊错误码处理
    if (data.code === "401") {
      // 只有非登录接口的401才认为是token过期
      if (!config.url?.includes('/login')) {
        handleTokenExpired()
        errorInfo.message = '登录已过期，请重新登录'
      }
      // 登录接口的401保持原始错误消息
    }

    try {
      if (config.errorMsg) {
        ElMessage.error(config.errorMsg)
      } else if (config.showDefaultMsg !== false) {
        ElMessage.error(errorInfo.message)
      }

      if (typeof config.onError === 'function') {
        config.onError(errorInfo)
      }
    } catch (err) {
      console.error('错误回调执行错误:', err)
    }

    return Promise.reject(errorInfo)
  }
}

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 处理真实响应
    return handleResponse(response.data, response.config)
  },
  error => {
    const config = error.config || {}

    console.error(`请求失败 [${config.requestId}]:`, {
      method: config.method?.toUpperCase(),
      url: config.url,
      error: error.message
    })

    // 构建错误信息
    let errorInfo = {
      type: ErrorTypes.HTTP,
      requestId: config.requestId,
      originalError: error
    }

    if (error.response) {
      // HTTP错误
      const status = error.response.status
      errorInfo.code = status
      errorInfo.data = error.response.data

      // 根据状态码设置错误消息
      const statusMessages = {
        400: '请求参数错误',
        401: '未授权，请重新登录',
        403: '拒绝访问',
        404: '请求的资源不存在',
        408: '请求超时',
        500: '服务器内部错误',
        502: '网关错误',
        503: '服务不可用',
        504: '网关超时'
      }

      errorInfo.message = statusMessages[status] || error.response.data?.msg || `请求失败(${status})`

      // 401错误特殊处理
      if (status === 401 && !config.url?.includes('/login')) {
        handleTokenExpired()
      }
    } else if (error.code === 'ECONNABORTED') {
      errorInfo.type = ErrorTypes.TIMEOUT
      errorInfo.message = '请求超时，请检查网络连接'
    } else if (error.message?.includes('Network Error')) {
      errorInfo.type = ErrorTypes.NETWORK
      errorInfo.message = '网络连接失败，请检查网络设置'
    } else {
      errorInfo.message = error.message || '未知错误'
    }

    // 显示错误提示
    try {
      if (config.errorMsg) {
        ElMessage.error(config.errorMsg)
      } else if (config.showDefaultMsg !== false) {
        ElMessage({
          message: errorInfo.message,
          type: 'error',
          duration: 5000,
          showClose: true
        })
      }

      if (typeof config.onError === 'function') {
        config.onError(errorInfo)
      }
    } catch (err) {
      console.error('错误处理回调执行失败:', err)
    }

    return Promise.reject(errorInfo)
  }
)

// 扩展请求方法
const request = {
  get(url, params, config = {}) {
    return service.get(url, {
      params,
      enableCache: true, // GET请求默认启用缓存
      ...config
    })
  },

  post(url, data, config = {}) {
    return service.post(url, data, config)
  },

  put(url, data, config = {}) {
    return service.put(url, data, config)
  },

  delete(url, config = {}) {
    return service.delete(url, config)
  },

  // 新增：带重试的请求
  retry(url, options = {}) {
    const { method = 'get', data, params, retryCount = 3, ...config } = options
    return service({
      method,
      url,
      data,
      params,
      enableRetry: true,
      retryCount,
      ...config
    })
  },

  // 新增：可取消的请求
  cancelable(url, options = {}) {
    const source = axios.CancelToken.source()
    const { method = 'get', data, params, ...config } = options

    const promise = service({
      method,
      url,
      data,
      params,
      cancelToken: source.token,
      ...config
    })

    promise.cancel = source.cancel
    return promise
  },

  // 新增：清理缓存
  clearCache(pattern) {
    if (pattern) {
      // 清理匹配模式的缓存
      for (const [key] of requestCache) {
        if (key.includes(pattern)) {
          requestCache.delete(key)
        }
      }
      console.log(`🗑️ 清理缓存: ${pattern}`)
    } else {
      // 清理所有缓存
      requestCache.clear()
      console.log('🗑️ 清理所有缓存')
    }
  },

  // 新增：获取缓存状态
  getCacheInfo() {
    return {
      size: requestCache.size,
      keys: Array.from(requestCache.keys())
    }
  }
}

/**
 * 增强版请求方法使用示例：
 *
 * 1. 基础请求（自动缓存GET请求）：
 * request.get('/api/users', { page: 1 })
 * request.post('/api/users', { name: 'Tom', age: 20 })
 * request.put('/api/users/1', { name: 'Tom' })
 * request.delete('/api/users/1')
 *
 * 2. 自定义配置：
 * request.post('/api/users', data, {
 *   successMsg: '添加用户成功！',
 *   errorMsg: '添加用户失败，请重试',
 *   showDefaultMsg: true,
 *   enableCache: false,

 * })
 *
 * 3. 使用回调函数：
 * request.post('/api/users', data, {
 *   onSuccess: (data) => {
 *     console.log('请求成功：', data)
 *   },
 *   onError: (error) => {
 *     console.log('请求失败：', error)
 *     console.log('错误类型：', error.type)
 *     console.log('请求ID：', error.requestId)
 *   }
 * })
 *
 * 4. 带重试的请求：
 * request.retry('/api/users', {
 *   method: 'post',
 *   data: userData,
 *   retryCount: 3
 * })
 *
 * 5. 可取消的请求：
 * const cancelableRequest = request.cancelable('/api/users')
 * // 取消请求
 * cancelableRequest.cancel('用户取消')
 *
 * 6. 缓存管理：
 * request.clearCache() // 清理所有缓存
 * request.clearCache('/api/users') // 清理特定接口缓存
 *
 * 7. 完整示例：
 * request.post('/api/users', data, {
 *   successMsg: '添加成功',
 *   errorMsg: '添加失败',
 *   enableCache: false,

 *   onSuccess: (data) => {
 *     // 处理成功逻辑
 *   },
 *   onError: (error) => {
 *     // 根据错误类型处理
 *     switch(error.type) {
 *       case 'business':
 *         // 业务错误
 *         break
 *       case 'network':
 *         // 网络错误
 *         break
 *       case 'timeout':
 *         // 超时错误
 *         break
 *     }
 *   }
 * })
 */

// 为AI服务添加相同的拦截器
aiService.interceptors.request.use(
  config => {
    // 复用相同的请求拦截逻辑
    const token = getAuthToken()
    if (token) {
      config.headers['token'] = token
    }
    
    // 生成请求ID
    config.requestId = ++requestId
    config.requestTime = Date.now()
    
    console.log(`AI请求开始 [${config.requestId}]:`, {
      method: config.method?.toUpperCase(),
      url: config.url,
      timeout: config.timeout
    })
    
    return config
  },
  error => {
    console.error('AI请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

aiService.interceptors.response.use(
  response => {
    return handleResponse(response.data, response.config)
  },
  error => {
    const config = error.config || {}
    
    console.error(`AI请求失败 [${config.requestId}]:`, {
      method: config.method?.toUpperCase(),
      url: config.url,
      error: error.message,
      timeout: config.timeout
    })

    // 构建错误信息（AI请求特殊处理）
    let errorInfo = {
      type: ErrorTypes.HTTP,
      requestId: config.requestId,
      originalError: error
    }

    if (error.response) {
      const status = error.response.status
      errorInfo.code = status
      errorInfo.data = error.response.data

      const statusMessages = {
        400: '请求参数错误',
        401: '未授权，请重新登录',
        403: '拒绝访问',
        404: '请求的资源不存在',
        408: 'AI请求超时，内容可能过长',
        500: 'AI服务暂时不可用',
        502: 'AI服务网关错误',
        503: 'AI服务繁忙，请稍后重试',
        504: 'AI服务响应超时'
      }

      errorInfo.message = statusMessages[status] || error.response.data?.msg || `AI请求失败(${status})`

      if (status === 401 && !config.url?.includes('/login')) {
        handleTokenExpired()
      }
    } else if (error.code === 'ECONNABORTED') {
      errorInfo.type = ErrorTypes.TIMEOUT
      errorInfo.message = 'AI请求超时，请尝试缩短内容长度或稍后重试'
    } else if (error.message?.includes('Network Error')) {
      errorInfo.type = ErrorTypes.NETWORK
      errorInfo.message = '网络连接失败，请检查网络设置'
    } else {
      errorInfo.message = error.message || 'AI处理失败'
    }

    // AI请求错误不显示默认消息（由AI工具类处理）
    if (config.showDefaultMsg !== false && !config.suppressErrorMessage) {
      ElMessage({
        message: errorInfo.message,
        type: 'error',
        duration: 8000, // AI错误消息显示更久
        showClose: true
      })
    }

    if (typeof config.onError === 'function') {
      config.onError(errorInfo)
    }

    return Promise.reject(errorInfo)
  }
)

// 扩展AI请求方法
const aiRequest = {
  get(url, params, config = {}) {
    return aiService.get(url, {
      params,
      suppressErrorMessage: true, // 默认不显示错误消息
      ...config
    })
  },

  post(url, data, config = {}) {
    return aiService.post(url, data, {
      suppressErrorMessage: true,
      ...config
    })
  },

  put(url, data, config = {}) {
    return aiService.put(url, data, {
      suppressErrorMessage: true,
      ...config
    })
  },

  delete(url, config = {}) {
    return aiService.delete(url, {
      suppressErrorMessage: true,
      ...config
    })
  }
}

export { aiRequest }
export default request 