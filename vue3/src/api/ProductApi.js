import request from "@/utils/request";

/**
 * @description 分页查询商品列表
 * @param {number} current - 当前页码
 * @param {number} size - 每页大小
 * @param {string} [productName] - 商品名称（可选）
 * @param {number} [categoryId] - 分类ID（可选）
 * @param {number} [status] - 状态（可选）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getProductPage(current, size, productName, categoryId, status, callbacks = {}) {
  const params = { current, size };
  if (productName) params.productName = productName;
  if (categoryId !== null && categoryId !== undefined) params.categoryId = categoryId;
  if (status !== null && status !== undefined) params.status = status;
  
  return request.get('/product/page', params, callbacks);
}

/**
 * @description 根据ID获取商品详情
 * @param {string} productId - 商品ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getProductById(productId, callbacks = {}) {
  return request.get(`/product/${productId}`, null, callbacks);
}

/**
 * @description 创建商品
 * @param {object} productData - 商品数据
 * @param {string} productData.id - 商品ID（UUID）
 * @param {string} productData.productName - 商品名称
 * @param {string} [productData.productDesc] - 商品简介
 * @param {string} [productData.productDetail] - 商品详情
 * @param {number} productData.categoryId - 分类ID
 * @param {number} [productData.coverImageId] - 封面图文件ID
 * @param {number} productData.price - 价格
 * @param {string} [productData.downloadLink] - 下载链接
 * @param {number} [productData.status] - 状态（0:下架, 1:上架）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function createProduct(productData, callbacks = {}) {
  return request.post('/product', productData, callbacks);
}

/**
 * @description 更新商品
 * @param {string} productId - 商品ID
 * @param {object} productData - 商品数据
 * @param {string} productData.productName - 商品名称
 * @param {string} [productData.productDesc] - 商品简介
 * @param {string} [productData.productDetail] - 商品详情
 * @param {number} productData.categoryId - 分类ID
 * @param {number} [productData.coverImageId] - 封面图文件ID
 * @param {number} productData.price - 价格
 * @param {string} [productData.downloadLink] - 下载链接
 * @param {number} [productData.status] - 状态（0:下架, 1:上架）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateProduct(productId, productData, callbacks = {}) {
  return request.put(`/product/${productId}`, productData, callbacks);
}

/**
 * @description 删除商品
 * @param {string} productId - 商品ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteProduct(productId, callbacks = {}) {
  return request.delete(`/product/${productId}`, callbacks);
}

/**
 * @description 更新商品状态
 * @param {string} productId - 商品ID
 * @param {number} status - 状态（0:下架, 1:上架）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateProductStatus(productId, status, callbacks = {}) {
  return request.put(`/product/${productId}/status`, null, {
    ...callbacks,
    params: { status }
  });
}

/**
 * @description 增加商品浏览次数
 * @param {string} productId - 商品ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function incrementViewCount(productId, callbacks = {}) {
  return request.post(`/product/${productId}/view`, null, callbacks);
}
