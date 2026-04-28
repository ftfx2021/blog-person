import request from "@/utils/request";

/**
 * @description 获取所有分类列表
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getAllCategories(callbacks = {}) {
  return request.get('/category', null, callbacks);
}

/**
 * @description 根据分类ID获取分类详情
 * @param {number} categoryId - 分类ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getCategoryById(categoryId, callbacks = {}) {
  return request.get(`/category/${categoryId}`, null, callbacks);
}

/**
 * @description 获取分类树形结构
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getCategoryTree(callbacks = {}) {
  return request.get('/category/tree', null, callbacks);
}

/**
 * @description 获取顶级分类（包含子分类文章数量累计）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getTopLevelCategories(callbacks = {}) {
  return request.get('/category/top-level', null, callbacks);
}

/**
 * @description 创建新分类
 * @param {object} categoryData - 分类创建数据
 * @param {string} categoryData.name - 分类名称
 * @param {string} [categoryData.description] - 分类描述
 * @param {number} [categoryData.parentId] - 父分类ID
 * @param {number} [categoryData.orderNum] - 排序号
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function createCategory(categoryData, callbacks = {}) {
  return request.post('/category', categoryData, callbacks);
}

/**
 * @description 更新分类信息
 * @param {number} categoryId - 分类ID
 * @param {object} categoryData - 分类更新数据
 * @param {string} categoryData.name - 分类名称
 * @param {string} [categoryData.description] - 分类描述
 * @param {number} [categoryData.parentId] - 父分类ID
 * @param {number} [categoryData.orderNum] - 排序号
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateCategory(categoryId, categoryData, callbacks = {}) {
  return request.put(`/category/${categoryId}`, categoryData, callbacks);
}

/**
 * @description 删除分类
 * @param {number} categoryId - 分类ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteCategory(categoryId, callbacks = {}) {
  return request.delete(`/category/${categoryId}`, callbacks);
}