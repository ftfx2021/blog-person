import request from "@/utils/request";

/**
 * @description 获取所有商品分类列表
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getAllProductCategories(callbacks = {}) {
  return request.get('/product-category', null, callbacks);
}

/**
 * @description 获取启用状态的商品分类列表
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getEnabledProductCategories(callbacks = {}) {
  return request.get('/product-category/enabled', null, callbacks);
}

/**
 * @description 根据ID获取商品分类详情
 * @param {number} categoryId - 分类ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getProductCategoryById(categoryId, callbacks = {}) {
  return request.get(`/product-category/${categoryId}`, null, callbacks);
}

/**
 * @description 创建商品分类
 * @param {object} categoryData - 分类数据
 * @param {string} categoryData.categoryName - 分类名称
 * @param {number} [categoryData.sortOrder] - 排序号
 * @param {number} [categoryData.status] - 状态（0:禁用, 1:启用）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function createProductCategory(categoryData, callbacks = {}) {
  return request.post('/product-category', categoryData, callbacks);
}

/**
 * @description 更新商品分类
 * @param {number} categoryId - 分类ID
 * @param {object} categoryData - 分类数据
 * @param {string} categoryData.categoryName - 分类名称
 * @param {number} [categoryData.sortOrder] - 排序号
 * @param {number} [categoryData.status] - 状态（0:禁用, 1:启用）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateProductCategory(categoryId, categoryData, callbacks = {}) {
  return request.put(`/product-category/${categoryId}`, categoryData, callbacks);
}

/**
 * @description 删除商品分类
 * @param {number} categoryId - 分类ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteProductCategory(categoryId, callbacks = {}) {
  return request.delete(`/product-category/${categoryId}`, callbacks);
}
