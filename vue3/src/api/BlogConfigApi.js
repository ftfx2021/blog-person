import request from "@/utils/request";

/**
 * @description 获取所有博客配置
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getAllBlogConfigs(callbacks = {}) {
  return request.get('/config', null, callbacks);
}

/**
 * @description 根据配置键获取配置值
 * @param {string} key - 配置键
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getBlogConfigByKey(key, callbacks = {}) {
  return request.get(`/config/${key}`, null, callbacks);
}

/**
 * @description 批量更新博客配置
 * @param {object} configData - 配置数据对象，必须包含 configMap 字段
 * @param {object} configData.configMap - 配置键值对映射，如 { about_name: "张三", about_job: "工程师" }
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateBlogConfigs(configData, callbacks = {}) {
  return request.put('/config/admin', configData, callbacks);
}

/**
 * @description 添加新的博客配置
 * @param {string} configKey - 配置键
 * @param {string} configValue - 配置值
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function addBlogConfig(configKey, configValue, callbacks = {}) {
  return request.post('/config/admin', { 
    configKey, 
    configValue 
  }, callbacks);
}

/**
 * @description 删除博客配置
 * @param {string} key - 配置键
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteBlogConfig(key, callbacks = {}) {
  return request.delete(`/config/admin/${key}`, callbacks);
}
