import request from "@/utils/request";

/**
 * @description 用户登录
 * @param {object} params - 登录参数
 * @param {string} params.username - 用户名
 * @param {string} params.password - 密码
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function loginUser(params, callbacks = {}) {
  return request.post('/user/login', params, callbacks);
}

/**
 * @description 用户注册
 * @param {object} params - 注册参数
 * @param {string} params.username - 用户名
 * @param {string} params.password - 密码
 * @param {string} params.email - 邮箱
 * @param {string} [params.phone] - 手机号
 * @param {string} [params.name] - 姓名
 * @param {string} [params.sex] - 性别
 * @param {string} [params.roleCode] - 角色编码
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function registerUser(params, callbacks = {}) {
  return request.post('/user/add', params, callbacks);
}

/**
 * @description 忘记密码
 * @param {object} params - 重置密码参数
 * @param {string} params.email - 邮箱
 * @param {string} params.newPassword - 新密码
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function forgetPassword(params, callbacks = {}) {
  return request.post('/user/forget', params, callbacks);
}

/**
 * @description 获取当前登录用户信息
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getCurrentUser(callbacks = {}) {
  return request.get('/user/current', null, callbacks);
}

/**
 * @description 根据ID获取用户信息
 * @param {number} userId - 用户ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUserById(userId, callbacks = {}) {
  return request.get(`/user/${userId}`, null, callbacks);
}

/**
 * @description 根据用户名获取用户信息
 * @param {string} username - 用户名
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUserByUsername(username, callbacks = {}) {
  return request.get(`/user/username/${username}`, null, callbacks);
}

/**
 * @description 更新用户信息
 * @param {number} userId - 用户ID
 * @param {object} params - 更新参数
 * @param {string} [params.name] - 姓名
 * @param {string} [params.email] - 邮箱
 * @param {string} [params.phone] - 手机号
 * @param {string} [params.sex] - 性别
 * @param {string} [params.avatar] - 头像
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateUser(userId, params, callbacks = {}) {
  return request.put(`/user/${userId}`, params, callbacks);
}

/**
 * @description 修改密码
 * @param {number} userId - 用户ID
 * @param {object} params - 密码修改参数
 * @param {string} params.oldPassword - 旧密码
 * @param {string} params.newPassword - 新密码
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updatePassword(userId, params, callbacks = {}) {
  return request.put(`/user/password/${userId}`, params, callbacks);
}

/**
 * @description 根据角色获取用户列表
 * @param {string} roleCode - 角色编码
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUsersByRole(roleCode, callbacks = {}) {
  return request.get(`/user/role/${roleCode}`, null, callbacks);
}

/**
 * @description 分页查询用户
 * @param {object} params - 查询参数
 * @param {number} [params.currentPage] - 当前页码
 * @param {number} [params.size] - 每页大小
 * @param {string} [params.username] - 用户名模糊查询
 * @param {string} [params.email] - 邮箱模糊查询
 * @param {string} [params.roleCode] - 角色编码
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUsersPage(params, callbacks = {}) {
  return request.get('/user/page', params, callbacks);
}

/**
 * @description 获取所有用户列表
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getAllUsers(callbacks = {}) {
  return request.get('/user/all', null, callbacks);
}


/**
 * @description 删除用户
 * @param {number} userId - 用户ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteUser(userId, callbacks = {}) {
  return request.delete(`/user/delete/${userId}`, callbacks);
}

/**
 * @description 批量删除用户
 * @param {object} params - 批量删除参数
 * @param {number[]} params.ids - 用户ID数组
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function batchDeleteUsers(params, callbacks = {}) {
  return request.delete('/user/deleteBatch', params, callbacks);
}

/**
 * @description 修改用户状态
 * @param {number} userId - 用户ID
 * @param {object} params - 状态修改参数
 * @param {number} params.status - 用户状态
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateUserStatus(userId, params, callbacks = {}) {
  return request.put(`/user/status/${userId}`, params, callbacks);
}

/**
 * @description 获取用户统计数据
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUserStats(callbacks = {}) {
  return request.get('/user/stats', null, callbacks);
}

/**
 * @description 获取用户评论列表
 * @param {object} params - 查询参数
 * @param {number} [params.currentPage=1] - 当前页码
 * @param {number} [params.size=10] - 每页条数
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUserComments(params, callbacks = {}) {
  return request.get('/comment/user', params, callbacks);
}

/**
 * @description 删除用户评论
 * @param {number} commentId - 评论ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteUserComment(commentId, callbacks = {}) {
  return request.delete(`/comment/${commentId}`, callbacks);
}
