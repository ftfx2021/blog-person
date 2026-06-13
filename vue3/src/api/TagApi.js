import request from "@/utils/request";

/**
 * 获取所有标签列表
 * @param {object} callbacks - 回调函数配置
 * @returns {Promise}
 */
export function getAllTags(callbacks = {}) {
  return request.get('/tag', null, callbacks);
}

/**
 * 根据ID获取标签详情
 * @param {number} id - 标签ID
 * @param {object} callbacks - 回调函数配置
 * @returns {Promise}
 */
export function getTagById(id, callbacks = {}) {
  return request.get(`/tag/${id}`, null, callbacks);
}

/**
 * 创建新标签
 * @param {object} params - 标签创建参数
 * @param {string} params.name - 标签名称
 * @param {string} params.color - 标签背景颜色（十六进制）
 * @param {string} params.textColor - 标签文字颜色（十六进制）
 * @param {object} callbacks - 回调函数配置
 * @returns {Promise}
 */
export function createTag(params, callbacks = {}) {
  return request.post('/tag', params, callbacks);
}

/**
 * 更新标签信息
 * @param {number} id - 标签ID
 * @param {object} params - 标签更新参数
 * @param {string} params.name - 标签名称
 * @param {string} params.color - 标签背景颜色（十六进制）
 * @param {string} params.textColor - 标签文字颜色（十六进制）
 * @param {object} callbacks - 回调函数配置
 * @returns {Promise}
 */
export function updateTag(id, params, callbacks = {}) {
  return request.put(`/tag/${id}`, params, callbacks);
}

/**
 * 删除标签
 * @param {number} id - 标签ID
 * @param {object} callbacks - 回调函数配置
 * @returns {Promise}
 */
export function deleteTag(id, callbacks = {}) {
  return request.delete(`/tag/${id}`, callbacks);
}

/**
 * 根据标签ID获取文章列表
 * @param {number} tagId - 标签ID
 * @param {object} params - 查询参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.size - 每页大小
 * @param {object} callbacks - 回调函数配置
 * @returns {Promise}
 */
export function getArticlesByTag(tagId, params, callbacks = {}) {
  return request.get(`/article/tag/${tagId}`, params, callbacks);
}

