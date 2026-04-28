import request from "@/utils/request";

/**
 * @description 临时业务文件上传（策略A第一阶段）
 * @param {File} file - 文件对象
 * @param {string} businessType - 业务类型，必须是后端FileBusinessTypeEnum中定义的值
 * @param {string} [businessField] - 业务字段
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function uploadTempBusinessFile(file, businessType, businessField, callbacks = {}) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('businessType', businessType);
  if (businessField) {
    formData.append('businessField', businessField);
  }
  return request.post('/file/upload/temp-business', formData, callbacks);
}

/**
 * @description 确认临时文件（策略A第二阶段）
 * @param {number} tempFileId - 临时文件ID
 * @param {object} uploadDTO - 文件上传信息
 * @param {string} uploadDTO.businessType - 业务类型
 * @param {string} uploadDTO.businessId - 业务实体ID
 * @param {string} uploadDTO.businessField - 业务字段
 * @param {boolean} uploadDTO.isTemp - 是否临时文件
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function confirmTempFile(tempFileId, uploadDTO, callbacks = {}) {
  return request.put(`/file/confirm/${tempFileId}`, uploadDTO, callbacks);
}

/**
 * @description 策略B：前端UUID预生成业务文件上传
 * @param {File} file - 文件对象
 * @param {object} businessInfo - 业务信息
 * @param {string} businessInfo.businessType - 业务类型
 * @param {string} businessInfo.businessId - 预生成的业务UUID
 * @param {string} businessInfo.businessField - 业务字段
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function uploadBusinessFileWithUUID(file, businessInfo, callbacks = {}) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('businessType', businessInfo.businessType);
  formData.append('businessId', businessInfo.businessId);
  formData.append('businessField', businessInfo.businessField);
  return request.post('/file/upload', formData, callbacks);
}

/**
 * @description 直接业务文件上传（策略C：用于更新已存在业务实体的文件）
 * @param {File} file - 文件对象
 * @param {object} businessInfo - 业务信息
 * @param {string} businessInfo.businessType - 业务类型
 * @param {string} businessInfo.businessId - 业务实体ID
 * @param {string} businessInfo.businessField - 业务字段
 * @param {boolean} [replaceOld=false] - 是否替换旧文件
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function uploadBusinessFile(file, businessInfo, replaceOld = false, callbacks = {}) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('businessType', businessInfo.businessType);
  formData.append('businessId', businessInfo.businessId);
  formData.append('businessField', businessInfo.businessField);
  formData.append('replaceOld', replaceOld.toString());
  return request.post('/file/upload', formData, callbacks);
}

/**
 * @description 简单图片上传（不保存在数据库）
 * @param {File} file - 图片文件
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function uploadSimpleImage(file, callbacks = {}) {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/file/simple/upload/image', formData, callbacks);
}

/**
 * @description 简单文件上传（不保存在数据库）
 * @param {File} file - 文件对象
 * @param {string} [fileType='COMMON'] - 文件类型
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function uploadSimpleFile(file, fileType = 'COMMON', callbacks = {}) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', fileType);
  return request.post('/file/simple/upload', formData, callbacks);
}

/**
 * @description 获取业务文件列表
 * @param {string} businessType - 业务类型
 * @param {string} businessId - 业务实体ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getFilesByBusiness(businessType, businessId, callbacks = {}) {
  return request.get(`/file/business/${businessType}/${businessId}`, null, callbacks);
}

/**
 * @description 根据文件ID删除文件
 * @param {number} fileId - 文件ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteFileById(fileId, callbacks = {}) {
  return request.delete(`/file/delete/${fileId}`, callbacks);
}

/**
 * @description 批量删除业务文件
 * @param {string} businessType - 业务类型
 * @param {string} businessId - 业务实体ID
 * @param {string} [businessField] - 业务字段
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteFilesByBusiness(businessType, businessId, businessField, callbacks = {}) {
  const params = businessField ? { businessField } : null;
  return request.delete(`/file/business/${businessType}/${businessId}`, callbacks, params);
}

/**
 * @description 获取文件上传配置
 * @param {string} businessType - 业务类型
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUploadConfig(businessType, callbacks = {}) {
  return request.get('/file/upload/config', { businessType }, callbacks);
}

// 文件业务类型常量 - 与后端FileBusinessTypeEnum保持一致
export const FILE_BUSINESS_TYPES = {
  // 用户相关
  USER_AVATAR: 'USER_AVATAR',
  USER_BACKGROUND: 'USER_BACKGROUND',
  
  // 心情日记相关
  MOOD_DIARY: 'MOOD_DIARY',
  MOOD_IMAGE: 'MOOD_IMAGE',
  
  // 帖子相关
  POST_CONTENT: 'POST_CONTENT',
  POST_COVER: 'POST_COVER',
  
  // 文章相关
  ARTICLE: 'ARTICLE',
  ARTICLE_COVER: 'ARTICLE_COVER',
  ARTICLE_CONTENT: 'ARTICLE_CONTENT',
  
  // 评论相关
  COMMENT_IMAGE: 'COMMENT_IMAGE',
  
  // 友情链接相关
  FRIEND_LINK_LOGO: 'FRIEND_LINK_LOGO',
  
  // 系统相关
  TEMP_FILE: 'TEMP_FILE',
  SYSTEM_NOTICE: 'SYSTEM_NOTICE'
};
