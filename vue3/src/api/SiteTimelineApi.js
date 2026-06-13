import request from "@/utils/request";

/**
 * @description 获取前台显示的网站发展历程
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getVisibleTimelines(callbacks = {}) {
  return request.get('/timeline/visible', null, callbacks);
}

/**
 * @description 管理员获取所有发展历程
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getAllTimelines(callbacks = {}) {
  return request.get('/timeline/admin/all', null, callbacks);
}

/**
 * @description 管理员分页获取发展历程
 * @param {object} params - 查询参数
 * @param {string} [params.title] - 事件标题
 * @param {number} [params.status] - 状态(0:隐藏,1:显示)
 * @param {number} [params.current=1] - 当前页码
 * @param {number} [params.size=10] - 每页条数
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getTimelinesByPage(params, callbacks = {}) {
  return request.get('/timeline/admin/page', params, callbacks);
}

/**
 * @description 根据ID获取发展历程
 * @param {number} id - 事件ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getTimelineById(id, callbacks = {}) {
  return request.get(`/timeline/admin/${id}`, null, callbacks);
}

/**
 * @description 管理员新增发展历程
 * @param {object} params - 事件信息
 * @param {string} params.title - 事件标题
 * @param {string} [params.content] - 事件详细描述
 * @param {string} params.eventDate - 事件日期(YYYY-MM-DD)
 * @param {string} [params.icon='fa-star'] - 图标类名(Font Awesome)
 * @param {string} [params.color='#409EFF'] - 时间线节点颜色
 * @param {number} [params.orderNum=0] - 排序号
 * @param {number} [params.status=1] - 状态(0:隐藏,1:显示)
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function createTimeline(params, callbacks = {}) {
  return request.post('/timeline/admin', params, callbacks);
}

/**
 * @description 管理员更新发展历程
 * @param {number} id - 事件ID
 * @param {object} params - 事件信息
 * @param {string} params.title - 事件标题
 * @param {string} [params.content] - 事件详细描述
 * @param {string} params.eventDate - 事件日期(YYYY-MM-DD)
 * @param {string} [params.icon] - 图标类名(Font Awesome)
 * @param {string} [params.color] - 时间线节点颜色
 * @param {number} [params.orderNum] - 排序号
 * @param {number} params.status - 状态(0:隐藏,1:显示)
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateTimeline(id, params, callbacks = {}) {
  return request.put(`/timeline/admin/${id}`, params, callbacks);
}

/**
 * @description 管理员删除发展历程
 * @param {number} id - 事件ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteTimeline(id, callbacks = {}) {
  return request.delete(`/timeline/admin/${id}`, callbacks);
}

/**
 * @description 管理员更新发展历程状态
 * @param {number} id - 事件ID
 * @param {object} params - 状态信息
 * @param {number} params.status - 状态(0:隐藏,1:显示)
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateTimelineStatus(id, params, callbacks = {}) {
  return request.put(`/timeline/admin/${id}/status`, params, callbacks);
}
