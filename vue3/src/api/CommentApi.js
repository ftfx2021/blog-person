import request from "@/utils/request";

/**
 * @description 添加评论
 * @param {object} params - 评论创建参数
 * @param {string} params.articleId - 文章ID
 * @param {string} params.content - 评论内容
 * @param {number} [params.parentId] - 父评论ID（0表示顶级评论）
 * @param {number} [params.toUserId] - 回复用户ID（0表示不是回复）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function createComment(params, callbacks = {}) {
  return request.post('/comment', params, callbacks);
}

/**
 * @description 获取文章评论列表（分页）
 * @param {string} articleId - 文章ID
 * @param {object} params - 分页参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.size - 每页大小
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getArticleComments(articleId, params, callbacks = {}) {
  return request.get(`/comment/article/${articleId}`, params, callbacks);
}

/**
 * @description 获取用户评论列表（分页）
 * @param {object} params - 分页参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.size - 每页大小
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUserComments(params, callbacks = {}) {
  return request.get('/comment/user', params, callbacks);
}

/**
 * @description 删除评论
 * @param {number} commentId - 评论ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteComment(commentId, callbacks = {}) {
  return request.delete(`/comment/${commentId}`, callbacks);
}

/**
 * @description 管理员获取评论分页列表
 * @param {object} params - 查询参数
 * @param {string} [params.articleTitle] - 文章标题（模糊查询）
 * @param {number} [params.status] - 评论状态（0:待审核,1:已通过,2:已拒绝）
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.size - 每页大小
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getAdminComments(params, callbacks = {}) {
  return request.get('/comment/admin/page', params, callbacks);
}

/**
 * @description 审核评论
 * @param {number} commentId - 评论ID
 * @param {object} params - 审核参数
 * @param {number} params.status - 评论状态（1:通过,2:拒绝）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function auditComment(commentId, params, callbacks = {}) {
  return request.put(`/comment/audit/${commentId}`, params, callbacks);
}

/**
 * @description 获取最近评论
 * @param {object} params - 查询参数
 * @param {number} [params.size] - 获取数量，默认5条
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getRecentComments(params = {}, callbacks = {}) {
  return request.get('/comment/recent', params, callbacks);
}
