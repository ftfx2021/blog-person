/**
 * 文件URL处理工具
 */

/**
 * 获取完整的文件URL
 * @param {string} path - 文件路径（如：/files/temp/xxx.jpg）
 * @returns {string} 完整的文件URL
 */
export function getFileUrl(path) {
  if (!path) return ''
  
  // 如果已经是完整URL，直接返回
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  
  // 如果是相对路径，拼接后端服务器地址
  const baseUrl = ''
  
  // 确保路径以 / 开头
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  
  return `${baseUrl}${normalizedPath}`
}

/**
 * 获取图片URL（getFileUrl的别名）
 * @param {string} path - 图片路径
 * @returns {string} 完整的图片URL
 */
export function getImageUrl(path) {
  return getFileUrl(path)
}

/**
 * 批量获取文件URL
 * @param {string[]} paths - 文件路径数组
 * @returns {string[]} 完整的文件URL数组
 */
export function getFileUrls(paths) {
  if (!Array.isArray(paths)) return []
  return paths.map(path => getFileUrl(path))
}
