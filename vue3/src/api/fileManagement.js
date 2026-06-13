import request from "@/utils/request";

/**
 * 文件管理API - 全新重构版本
 * 
 * 支持三种文件上传模式：
 * 1. OneToOne：单个业务对象对应单个文件（如用户头像）
 * 2. OneToMany：单个业务对象对应多个文件（如商品图片列表）
 * 3. 富文本：非结构化数据中的文件引用（如文章内容中的图片）
 */

/**
 * 上传文件到临时目录（第一步）
 * @param {File} file - 文件对象
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise<string>} 临时文件URL
 */
export function uploadToTemp(file, callbacks = {}) {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/file/upload/temp', formData, callbacks);
}

/**
 * 批量上传文件到临时目录
 * @param {File[]} files - 文件数组
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise<string[]>} 临时文件URL数组
 */
export function uploadMultipleToTemp(files, callbacks = {}) {
  const formData = new FormData();
  files.forEach(file => {
    formData.append('files', file);
  });
  return request.post('/file/upload/temp/batch', formData, callbacks);
}

/**
 * 移动临时文件到正式目录（OneToOne场景）
 * @param {string} tempUrl - 临时文件URL
 * @param {string} businessType - 业务类型
 * @param {string} businessId - 业务ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise<string>} 正式文件路径
 */
export function moveTempToFormal(tempUrl, businessType, businessId, callbacks = {}) {
  const params = new URLSearchParams();
  params.append('tempUrl', tempUrl);
  params.append('businessType', businessType);
  params.append('businessId', businessId);
  
  return request.post('/file/move/formal', params, callbacks);
}

/**
 * 批量移动临时文件到正式目录（OneToMany场景）
 * @param {string[]} tempUrls - 临时文件URL数组
 * @param {string} businessType - 业务类型
 * @param {string} businessId - 业务ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise<string[]>} 正式文件路径数组
 */
export function moveTempToFormalBatch(tempUrls, businessType, businessId, callbacks = {}) {
  return request.post(
    `/file/move/formal/batch?businessType=${businessType}&businessId=${businessId}`,
    tempUrls,
    callbacks
  );
}

/**
 * 处理富文本中的临时文件
 * @param {string} htmlContent - 富文本内容
 * @param {string} businessType - 业务类型
 * @param {string} businessId - 业务ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise<string>} 处理后的富文本内容
 */
export function processRichTextFiles(htmlContent, businessType, businessId, callbacks = {}) {
  return request.post(
    `/file/process/richtext?businessType=${businessType}&businessId=${businessId}`,
    htmlContent,
    {
      ...callbacks,
      headers: {
        'Content-Type': 'text/plain'
      }
    }
  );
}

/**
 * 删除业务文件
 * @param {string} businessType - 业务类型
 * @param {string} businessId - 业务ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function deleteBusinessFiles(businessType, businessId, callbacks = {}) {
  return request.delete(`/file/business/${businessType}/${businessId}`, callbacks);
}

/**
 * 清理过期临时文件（管理员接口）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise<number>} 清理的文件数量
 */
export function cleanupExpiredTempFiles(callbacks = {}) {
  return request.post('/file/cleanup/temp', null, callbacks);
}

// 业务类型常量
export const BUSINESS_TYPES = {
  // 用户相关
  USER_AVATAR: 'user_avatar',
  
  // 文章相关
  ARTICLE_COVER: 'article_cover',
  ARTICLE_CONTENT: 'article_content',
  
  // 商品相关
  PRODUCT_COVER: 'product_cover',
  PRODUCT_IMAGE: 'product_image',
  
  // 分类相关
  CATEGORY_ICON: 'category_icon',
  
  // 友链相关
  FRIEND_LINK_LOGO: 'friend_link_logo'
};

/**
 * 文件上传辅助函数
 */

/**
 * OneToOne模式上传辅助函数
 * 先上传到临时目录，返回临时URL，业务保存时再移动
 * 
 * @param {File} file - 文件对象
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise<string>} 临时文件URL
 * 
 * @example
 * // 1. 上传文件
 * const tempUrl = await uploadOneToOne(file);
 * 
 * // 2. 保存业务时，后端会自动处理临时文件
 * await saveUser({ avatar: tempUrl });
 */
export function uploadOneToOne(file, callbacks = {}) {
  return uploadToTemp(file, callbacks);
}

/**
 * OneToMany模式上传辅助函数
 * 批量上传到临时目录，返回临时URL数组
 * 
 * @param {File[]} files - 文件数组
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise<string[]>} 临时文件URL数组
 * 
 * @example
 * // 1. 批量上传文件
 * const tempUrls = await uploadOneToMany(files);
 * 
 * // 2. 保存业务时，后端会自动处理临时文件
 * await saveProduct({ images: JSON.stringify(tempUrls) });
 */
export function uploadOneToMany(files, callbacks = {}) {
  return uploadMultipleToTemp(files, callbacks);
}

/**
 * 富文本编辑器图片上传辅助函数
 * 用于配置富文本编辑器的图片上传
 * 
 * @param {File} file - 图片文件
 * @returns {Promise<{url: string}>} 返回临时图片URL
 * 
 * @example
 * // 配置编辑器
 * const editorConfig = {
 *   uploadImage: uploadForRichText
 * };
 */
export async function uploadForRichText(file) {
  try {
    const tempUrl = await uploadToTemp(file, { showDefaultMsg: false });
    return { url: tempUrl };
  } catch (error) {
    console.error('富文本图片上传失败:', error);
    throw error;
  }
}
