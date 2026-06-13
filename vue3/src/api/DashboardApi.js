import request from "@/utils/request";

/**
 * @description 获取仪表盘统计数据
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getDashboardStats(callbacks = {}) {
  return request.get('/dashboard/stats', null, callbacks);
}

/**
 * @description 获取文章发布趋势数据
 * @param {object} params - 查询参数
 * @param {number} [params.days=7] - 查询天数
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getArticleTrend(params = { days: 7 }, callbacks = {}) {
  return request.get('/dashboard/article/trend', params, callbacks);
}

/**
 * @description 获取访问量趋势数据
 * @param {object} params - 查询参数
 * @param {number} [params.days=7] - 查询天数
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getViewTrend(params = { days: 7 }, callbacks = {}) {
  return request.get('/dashboard/view/trend', params, callbacks);
}

/**
 * @description 获取最近发布的文章
 * @param {object} params - 查询参数
 * @param {number} [params.size=5] - 返回数量
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getRecentArticles(params = { size: 5 }, callbacks = {}) {
  return request.get('/article/recent', params, callbacks);
}

/**
 * @description 获取最近评论
 * @param {object} params - 查询参数
 * @param {number} [params.size=5] - 返回数量
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getRecentComments(params = { size: 5 }, callbacks = {}) {
  return request.get('/comment/recent', params, callbacks);
}
