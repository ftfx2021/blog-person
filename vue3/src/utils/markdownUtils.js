import { marked } from 'marked'

/**
 * Markdown 工具类
 * 提供 Markdown 与 HTML 之间的转换功能
 */

// 配置 marked 选项
marked.setOptions({
  // 启用 GitHub 风格的 Markdown
  gfm: true,
  // 启用表格支持
  tables: true,
  // 启用换行符转换
  breaks: true,
  // 启用删除线
  smartypants: false
})

// 自定义渲染器，保留 HTML 标签
const renderer = new marked.Renderer()

// 重写 html 方法，直接返回原始 HTML
renderer.html = function(html) {
  return html
}

// 应用自定义渲染器
marked.use({ renderer })

/**
 * @description 将 Markdown 文本转换为 HTML
 * @param {string} markdown - Markdown 文本
 * @returns {string} 转换后的 HTML 文本
 */
export function markdownToHtml(markdown) {
  if (!markdown || typeof markdown !== 'string') {
    return ''
  }
  
  try {
    return marked(markdown)
  } catch (error) {
    console.error('Markdown 转 HTML 失败:', error)
    return markdown // 失败时返回原始文本
  }
}

/**
 * @description 从 HTML 中提取纯文本内容
 * @param {string} html - HTML 文本
 * @returns {string} 提取的纯文本
 */
export function htmlToText(html) {
  if (!html || typeof html !== 'string') {
    return ''
  }
  
  try {
    // 创建一个临时 DOM 元素来解析 HTML
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = html
    
    // 获取纯文本内容
    const text = tempDiv.textContent || tempDiv.innerText || ''
    
    // 清理多余的空白字符
    return text.replace(/\s+/g, ' ').trim()
  } catch (error) {
    console.error('HTML 转文本失败:', error)
    // 简单的正则表达式清理 HTML 标签
    return html.replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim()
  }
}

/**
 * @description 简单的 HTML 转 Markdown（基础功能）
 * @param {string} html - HTML 文本
 * @returns {string} 转换后的 Markdown 文本
 */
export function htmlToMarkdown(html) {
  if (!html || typeof html !== 'string') {
    return ''
  }
  
  try {
    let markdown = html
    
    // 基础的 HTML 到 Markdown 转换规则
    const conversions = [
      // 标题
      { from: /<h1[^>]*>(.*?)<\/h1>/gi, to: '# $1\n\n' },
      { from: /<h2[^>]*>(.*?)<\/h2>/gi, to: '## $1\n\n' },
      { from: /<h3[^>]*>(.*?)<\/h3>/gi, to: '### $1\n\n' },
      { from: /<h4[^>]*>(.*?)<\/h4>/gi, to: '#### $1\n\n' },
      { from: /<h5[^>]*>(.*?)<\/h5>/gi, to: '##### $1\n\n' },
      { from: /<h6[^>]*>(.*?)<\/h6>/gi, to: '###### $1\n\n' },
      
      // 粗体和斜体
      { from: /<strong[^>]*>(.*?)<\/strong>/gi, to: '**$1**' },
      { from: /<b[^>]*>(.*?)<\/b>/gi, to: '**$1**' },
      { from: /<em[^>]*>(.*?)<\/em>/gi, to: '*$1*' },
      { from: /<i[^>]*>(.*?)<\/i>/gi, to: '*$1*' },
      
      // 链接
      { from: /<a[^>]*href="([^"]*)"[^>]*>(.*?)<\/a>/gi, to: '[$2]($1)' },
      
      // 图片
      { from: /<img[^>]*src="([^"]*)"[^>]*alt="([^"]*)"[^>]*>/gi, to: '![$2]($1)' },
      { from: /<img[^>]*src="([^"]*)"[^>]*>/gi, to: '![]($1)' },
      
      // 代码
      { from: /<code[^>]*>(.*?)<\/code>/gi, to: '`$1`' },
      { from: /<pre[^>]*><code[^>]*>(.*?)<\/code><\/pre>/gi, to: '```\n$1\n```\n\n' },
      
      // 段落
      { from: /<p[^>]*>(.*?)<\/p>/gi, to: '$1\n\n' },
      
      // 换行
      { from: /<br[^>]*\/?>/gi, to: '\n' },
      
      // 列表
      { from: /<ul[^>]*>(.*?)<\/ul>/gi, to: '$1\n' },
      { from: /<ol[^>]*>(.*?)<\/ol>/gi, to: '$1\n' },
      { from: /<li[^>]*>(.*?)<\/li>/gi, to: '- $1\n' },
      
      // 引用
      { from: /<blockquote[^>]*>(.*?)<\/blockquote>/gi, to: '> $1\n\n' }
    ]
    
    // 应用转换规则
    conversions.forEach(conversion => {
      markdown = markdown.replace(conversion.from, conversion.to)
    })
    
    // 清理剩余的 HTML 标签
    markdown = markdown.replace(/<[^>]*>/g, '')
    
    // 清理多余的空行
    markdown = markdown.replace(/\n{3,}/g, '\n\n')
    
    return markdown.trim()
  } catch (error) {
    console.error('HTML 转 Markdown 失败:', error)
    return htmlToText(html) // 失败时返回纯文本
  }
}

/**
 * @description 从 Markdown 内容中提取摘要
 * @param {string} markdown - Markdown 文本
 * @param {number} maxLength - 摘要最大长度，默认 200 字符
 * @returns {string} 提取的摘要
 */
export function extractSummary(markdown, maxLength = 200) {
  if (!markdown || typeof markdown !== 'string') {
    return ''
  }
  
  try {
    // 移除 Markdown 语法标记
    let summary = markdown
      // 移除标题标记
      .replace(/^#{1,6}\s+/gm, '')
      // 移除粗体和斜体标记
      .replace(/\*\*(.*?)\*\*/g, '$1')
      .replace(/\*(.*?)\*/g, '$1')
      // 移除链接
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      // 移除图片
      .replace(/!\[([^\]]*)\]\([^)]+\)/g, '')
      // 移除代码标记
      .replace(/`([^`]+)`/g, '$1')
      // 移除引用标记
      .replace(/^>\s+/gm, '')
      // 移除列表标记
      .replace(/^[-*+]\s+/gm, '')
      .replace(/^\d+\.\s+/gm, '')
    
    // 清理多余的空白字符和换行
    summary = summary.replace(/\s+/g, ' ').trim()
    
    // 截取指定长度
    if (summary.length > maxLength) {
      summary = summary.substring(0, maxLength).trim()
      // 确保不在单词中间截断
      const lastSpace = summary.lastIndexOf(' ')
      if (lastSpace > maxLength * 0.8) {
        summary = summary.substring(0, lastSpace)
      }
      summary += '...'
    }
    
    return summary
  } catch (error) {
    console.error('提取摘要失败:', error)
    return ''
  }
}

/**
 * @description 验证 Markdown 内容是否有效
 * @param {string} markdown - Markdown 文本
 * @returns {boolean} 是否为有效的 Markdown
 */
export function isValidMarkdown(markdown) {
  if (!markdown || typeof markdown !== 'string') {
    return false
  }
  
  try {
    // 尝试转换为 HTML
    marked(markdown)
    return true
  } catch (error) {
    return false
  }
}

/**
 * @description 清理和标准化 Markdown 内容
 * @param {string} markdown - Markdown 文本
 * @returns {string} 清理后的 Markdown 文本
 */
export function cleanMarkdown(markdown) {
  if (!markdown || typeof markdown !== 'string') {
    return ''
  }
  
  try {
    let cleaned = markdown
    
    // 标准化换行符
    cleaned = cleaned.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
    
    // 清理多余的空行（保留最多两个连续换行）
    cleaned = cleaned.replace(/\n{3,}/g, '\n\n')
    
    // 清理行尾空格
    cleaned = cleaned.replace(/ +$/gm, '')
    
    // 标准化列表格式
    cleaned = cleaned.replace(/^[\s]*[-*+][\s]+/gm, '- ')
    
    return cleaned.trim()
  } catch (error) {
    console.error('清理 Markdown 失败:', error)
    return markdown
  }
}
