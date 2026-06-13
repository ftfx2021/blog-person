import request from "@/utils/request";

/**
 * @description 获取前台显示的友情链接
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getVisibleFriendLinks(callbacks = {}) {
  return request.get('/friendLinks/visible', null, callbacks);
}

/**
 * @description 管理员获取所有友情链接
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getAllFriendLinks(callbacks = {}) {
  return request.get('/friendLinks/admin/all', null, callbacks);
}

/**
 * @description 管理员分页获取友情链接
 * @param {object} params - 查询参数
 * @param {string} [params.name] - 链接名称
 * @param {number} [params.status] - 状态(0:隐藏,1:显示)
 * @param {number} [params.currentPage=1] - 当前页码
 * @param {number} [params.size=10] - 每页条数
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getFriendLinksByPage(params, callbacks = {}) {
  return request.get('/friendLinks/admin/page', params, callbacks);
}

/**
 * @description 根据ID获取友情链接
 * @param {number} id - 友链ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getFriendLinkById(id, callbacks = {}) {
  return request.get(`/friendLinks/admin/${id}`, null, callbacks);
}

/**
 * @description 管理员新增友情链接
 * @param {object} params - 友链信息
 * @param {string} params.name - 链接名称
 * @param {string} params.url - 链接地址
 * @param {string} [params.logo] - 链接logo
 * @param {string} [params.description] - 链接描述
 * @param {number} [params.orderNum=0] - 排序号
 * @param {number} [params.status=1] - 状态(0:隐藏,1:显示)
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function createFriendLink(params, callbacks = {}) {
  return request.post('/friendLinks/admin', params, callbacks);
}

/**
 * @description 管理员更新友情链接
 * @param {number} id - 友链ID
 * @param {object} params - 友链信息
 * @param {string} params.name - 链接名称
 * @param {string} params.url - 链接地址
 * @param {string} [params.logo] - 链接logo
 * @param {string} [params.description] - 链接描述
 * @param {number} [params.orderNum] - 排序号
 * @param {number} params.status - 状态(0:隐藏,1:显示)
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateFriendLink(id, params, callbacks = {}) {
  return request.put(`/friendLinks/admin/${id}`, params, callbacks);
}

/**
 * @description 管理员删除友情链接
 * @param {number} id - 友链ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteFriendLink(id, callbacks = {}) {
  return request.delete(`/friendLinks/admin/${id}`, callbacks);
}

/**
 * @description 管理员更新友情链接状态
 * @param {number} id - 友链ID
 * @param {object} params - 状态信息
 * @param {number} params.status - 状态(0:隐藏,1:显示)
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateFriendLinkStatus(id, params, callbacks = {}) {
  return request.put(`/friendLinks/admin/${id}/status`, params, callbacks);
}


