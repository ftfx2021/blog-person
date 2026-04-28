import request from "@/utils/request";

/**
 * @description 获取用户已购商品列表
 * @param {object} params - 请求参数
 * @param {number} params.current - 当前页码
 * @param {number} params.size - 每页大小
 * @param {string} [params.productName] - 商品名称
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getPurchasedProducts(params, callbacks = {}) {
  return request.get('/download/purchased', params, callbacks);
}

/**
 * @description 记录下载行为
 * @param {string} productId - 商品ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function recordDownload(productId, callbacks = {}) {
  return request.post(`/download/record/${productId}`, null, callbacks);
}

/**
 * @description 获取用户下载记录
 * @param {object} params - 请求参数
 * @param {number} params.current - 当前页码
 * @param {number} params.size - 每页大小
 * @param {string} [params.productName] - 商品名称
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getDownloadRecords(params, callbacks = {}) {
  return request.get('/download/records', params, callbacks);
}

/**
 * @description 获取商品下载链接
 * @param {string} productId - 商品ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getDownloadLink(productId, callbacks = {}) {
  return request.get(`/download/link/${productId}`, null, callbacks);
}
