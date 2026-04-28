import request from '@/utils/request';

/**
 * 文章全文搜索
 * @param {object} params - 搜索参数
 * @param {string} params.keyword - 搜索关键词(必填)
 * @param {number} params.current - 当前页码,默认1
 * @param {number} params.size - 每页大小,默认10
 * @param {object} callbacks - 回调函数
 * @param {function} callbacks.onSuccess - 成功回调,参数为响应data(Page对象)
 * @param {function} callbacks.onError - 失败回调,参数为错误对象
 * @returns {Promise} 请求Promise
 */
export function searchArticles(params, callbacks = {}) {
  return request.get('/search/article/query', params, callbacks);
}

/**
 * 文章高级搜索
 * @param {object} params - 搜索参数
 * @param {string} [params.keyword] - 搜索关键词(可选)
 * @param {string} [params.categoryName] - 分类名称(可选)
 * @param {Array<string>} [params.tags] - 标签列表(可选)
 * @param {number} params.current - 当前页码,默认1
 * @param {number} params.size - 每页大小,默认10
 * @param {object} callbacks - 回调函数
 * @returns {Promise} 请求Promise
 */
export function advancedSearchArticles(params, callbacks = {}) {
  return request.get('/search/article/advanced', params, callbacks);
}

/**
 * 同步数据到ES
 * @param {object} callbacks - 回调函数
 * @returns {Promise} 请求Promise
 */
export function syncToES(callbacks = {}) {
  return request.post('/search/article/sync', null, callbacks);
}

/**
 * 重建ES索引
 * @param {object} callbacks - 回调函数
 * @returns {Promise} 请求Promise
 */
export function rebuildIndex(callbacks = {}) {
  return request.post('/search/article/rebuild', null, callbacks);
}

/**
 * 创建ES索引
 * @param {object} callbacks - 回调函数
 * @returns {Promise} 请求Promise
 */
export function createIndex(callbacks = {}) {
  return request.post('/search/article/index/create', null, callbacks);
}

/**
 * 删除ES索引
 * @param {object} callbacks - 回调函数
 * @returns {Promise} 请求Promise
 */
export function deleteIndex(callbacks = {}) {
  return request.delete('/search/article/index/delete', callbacks);
}

/**
 * 检查ES索引是否存在
 * @param {object} callbacks - 回调函数
 * @returns {Promise} 请求Promise
 */
export function checkIndexExists(callbacks = {}) {
  return request.get('/search/article/index/exist', null, callbacks);
}
