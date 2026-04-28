/**
 * 颜色处理工具类
 * 提供颜色转换、亮度计算、文字颜色自动适配等功能
 */

/**
 * 将十六进制颜色转换为RGB对象
 * @param {string} hex - 十六进制颜色值（如 #3498db 或 3498db）
 * @returns {object} RGB对象 { r, g, b }
 */
export const hexToRgb = (hex) => {
  if (!hex) return { r: 52, g: 152, b: 219 } // 默认蓝色
  
  // 移除#号
  hex = hex.replace('#', '')
  
  // 处理3位十六进制（如 abc -> aabbcc）
  if (hex.length === 3) {
    hex = hex.split('').map(char => char + char).join('')
  }
  
  // 验证并解析十六进制
  const result = /^([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : { r: 52, g: 152, b: 219 }
}

/**
 * 计算颜色的相对亮度（使用WCAG标准）
 * @param {number} r - 红色分量 (0-255)
 * @param {number} g - 绿色分量 (0-255)
 * @param {number} b - 蓝色分量 (0-255)
 * @returns {number} 相对亮度值 (0-1)
 */
export const calculateLuminance = (r, g, b) => {
  // 将RGB值转换为0-1范围并应用gamma校正
  const [rs, gs, bs] = [r, g, b].map(c => {
    c = c / 255
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
  })
  
  // 使用WCAG公式计算相对亮度
  return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs
}

/**
 * 根据背景颜色亮度自动选择合适的文字颜色
 * @param {string} backgroundColor - 背景颜色（十六进制）
 * @param {object} options - 配置选项
 * @param {number} options.threshold - 亮度阈值，默认0.5
 * @param {number} options.lightOpacity - 浅色文字透明度，默认0.85
 * @param {number} options.darkOpacity - 深色文字透明度，默认0.85
 * @returns {string} 文字颜色（rgba格式）
 */
export const getContrastTextColor = (backgroundColor, options = {}) => {
  const {
    threshold = 0.5,
    lightOpacity = 0.9,
    darkOpacity = 0.9
  } = options
  
  if (!backgroundColor) return `rgba(255, 255, 255, ${lightOpacity})`
  
  const { r, g, b } = hexToRgb(backgroundColor)
  const luminance = calculateLuminance(r, g, b)
  
  // 亮度大于阈值使用深色文字，否则使用浅色文字
  return luminance > threshold 
    ? `rgba(0, 0, 0, ${darkOpacity})` 
    : `rgba(255, 255, 255, ${lightOpacity})`
}

/**
 * 根据背景颜色亮度生成合适的文字阴影
 * @param {string} backgroundColor - 背景颜色（十六进制）
 * @param {object} options - 配置选项
 * @param {number} options.threshold - 亮度阈值，默认0.5
 * @param {string} options.lightShadow - 浅色背景阴影，默认'rgba(0, 0, 0, 0.2)'
 * @param {string} options.darkShadow - 深色背景阴影，默认'rgba(255, 255, 255, 0.3)'
 * @param {string} options.offset - 阴影偏移，默认'0 2px 4px'
 * @returns {string} 文字阴影CSS值
 */
export const getContrastTextShadow = (backgroundColor, options = {}) => {
  const {
    threshold = 0.5,
    lightShadow = 'rgba(0, 0, 0, 0.2)',
    darkShadow = 'rgba(255, 255, 255, 0.3)',
    offset = '0 2px 4px'
  } = options
  
  if (!backgroundColor) return `${offset} ${darkShadow}`
  
  const { r, g, b } = hexToRgb(backgroundColor)
  const luminance = calculateLuminance(r, g, b)
  
  const shadowColor = luminance > threshold ? lightShadow : darkShadow
  return `${offset} ${shadowColor}`
}

/**
 * 生成基于主色调的渐变背景
 * @param {string} baseColor - 基础颜色（十六进制）
 * @param {string} direction - 渐变方向，默认'to right'
 * @param {number} opacity1 - 起始透明度，默认1
 * @param {number} opacity2 - 中间透明度，默认0.5
 * @param {number} opacity3 - 结束透明度，默认0
 * @param {number} stop1 - 第一个色标位置，默认0
 * @param {number} stop2 - 第二个色标位置，默认30
 * @param {number} stop3 - 第三个色标位置，默认100
 * @returns {string} CSS渐变字符串
 */
export const generateGradient = (baseColor, direction = 'to right', opacity1 = 1, opacity2 = 0.5, opacity3 = 0, stop1 = 0, stop2 = 30, stop3 = 100) => {
  if (!baseColor) return `linear-gradient(${direction}, #3498db, rgba(52, 152, 219, 0.5) 30%, rgba(255, 255, 255, 0))`
  
  const { r, g, b } = hexToRgb(baseColor)
  return `linear-gradient(${direction}, rgba(${r}, ${g}, ${b}, ${opacity1}) ${stop1}%, rgba(${r}, ${g}, ${b}, ${opacity2}) ${stop2}%, rgba(255, 255, 255, ${opacity3}) ${stop3}%)`
}

/**
 * 根据背景亮度生成合适的元信息样式
 * @param {string} backgroundColor - 背景颜色（十六进制）
 * @param {object} options - 配置选项
 * @returns {object} 包含文字颜色、背景色、边框色的样式对象
 */
export const getMetaStyles = (backgroundColor, options = {}) => {
  const {
    threshold = 0.5,
    lightTextOpacity = 0.75,
    darkTextOpacity = 0.9,
    backgroundOpacity = 0.1,
    borderOpacity = 0.2
  } = options
  
  if (!backgroundColor) {
    return {
      textColor: `rgba(255, 255, 255, ${darkTextOpacity})`,
      backgroundColor: `rgba(255, 255, 255, ${backgroundOpacity})`,
      borderColor: `rgba(255, 255, 255, ${borderOpacity})`
    }
  }
  
  const { r, g, b } = hexToRgb(backgroundColor)
  const luminance = calculateLuminance(r, g, b)
  
  if (luminance > threshold) {
    // 浅色背景
    return {
      textColor: `rgba(0, 0, 0, ${lightTextOpacity})`,
      backgroundColor: `rgba(0, 0, 0, ${backgroundOpacity})`,
      borderColor: `rgba(0, 0, 0, ${borderOpacity})`
    }
  } else {
    // 深色背景
    return {
      textColor: `rgba(255, 255, 255, ${darkTextOpacity})`,
      backgroundColor: `rgba(255, 255, 255, ${backgroundOpacity})`,
      borderColor: `rgba(255, 255, 255, ${borderOpacity})`
    }
  }
}

/**
 * 生成完整的封面样式对象
 * @param {string} baseColor - 基础颜色（十六进制）
 * @param {object} options - 配置选项
 * @returns {object} 完整的样式对象
 */
export const generateCoverStyles = (baseColor, options = {}) => {
  const { r, g, b } = hexToRgb(baseColor)
  const textColor = getContrastTextColor(baseColor, options)
  const textShadow = getContrastTextShadow(baseColor, options)
  const leftGradient = generateGradient(baseColor, 'to right')
  const rightGradient = generateGradient(baseColor, 'to left')
  const metaStyles = getMetaStyles(baseColor, options)
  
  return {
    // 主容器样式
    heroStyle: {
      backgroundColor: `rgb(${r}, ${g}, ${b})`,
      backgroundImage: `linear-gradient(135deg, rgba(${r}, ${g}, ${b}, 0.1) 0%, rgba(255, 255, 255, 0.5) 50%, rgba(${r}, ${g}, ${b}, 0.1) 100%)`,
      '--base-color': `${r} ${g} ${b}`,
      '--text-color': textColor,
      '--text-shadow-color': textShadow
    },
    
    // 文字样式
    textColor,
    textShadow,
    
    // 渐变样式
    leftGradient,
    rightGradient,
    
    // 元信息样式
    metaTextColor: metaStyles.textColor,
    metaBackground: metaStyles.backgroundColor,
    metaBorderColor: metaStyles.borderColor,
    
    // RGB值
    rgb: { r, g, b }
  }
}

/**
 * 颜色工具类的默认导出
 */
export default {
  hexToRgb,
  calculateLuminance,
  getContrastTextColor,
  getContrastTextShadow,
  generateGradient,
  getMetaStyles,
  generateCoverStyles
}
