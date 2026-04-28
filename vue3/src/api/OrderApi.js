import request from "@/utils/request";

/**
 * @description 创建订单
 * @param {object} params - 请求参数
 * @param {string} params.productId - 商品ID
 * @param {number} params.payType - 支付方式（1:支付宝, 2:微信）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function createOrder(params, callbacks = {}) {
  return request.post('/order', params, callbacks);
}

/**
 * @description 分页查询订单列表
 * @param {object} params - 请求参数
 * @param {number} params.current - 当前页码
 * @param {number} params.size - 每页大小
 * @param {number} [params.userId] - 用户ID（管理员可查询所有用户订单）
 * @param {string} [params.productName] - 商品名称
 * @param {number} [params.orderStatus] - 订单状态
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getOrderPage(params, callbacks = {}) {
  return request.get('/order/page', params, callbacks);
}

/**
 * @description 根据ID获取订单详情
 * @param {number} id - 订单ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getOrderById(id, callbacks = {}) {
  return request.get(`/order/${id}`, null, callbacks);
}

/**
 * @description 根据订单号获取订单详情
 * @param {string} orderNo - 订单号
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getOrderByOrderNo(orderNo, callbacks = {}) {
  return request.get(`/order/no/${orderNo}`, null, callbacks);
}

/**
 * @description 更新订单
 * @param {number} id - 订单ID
 * @param {object} params - 请求参数
 * @param {number} [params.orderStatus] - 订单状态
 * @param {number} [params.payType] - 支付方式
 * @param {string} [params.payTime] - 支付时间
 * @param {string} [params.thirdPayNo] - 第三方支付流水号
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function updateOrder(id, params, callbacks = {}) {
  return request.put(`/order/${id}`, params, callbacks);
}

/**
 * @description 更新订单状态
 * @param {number} id - 订单ID
 * @param {number} orderStatus - 订单状态（1:待支付, 2:已支付, 3:已完成）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function updateOrderStatus(id, orderStatus, callbacks = {}) {
  return request.put(`/order/${id}/status`, { orderStatus }, callbacks);
}

/**
 * @description 支付订单
 * @param {number} id - 订单ID
 * @param {object} params - 请求参数
 * @param {number} params.payType - 支付方式（1:支付宝, 2:微信）
 * @param {string} [params.thirdPayNo] - 第三方支付流水号
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function payOrder(id, params, callbacks = {}) {
  return request.post(`/order/${id}/pay`, params, callbacks);
}

/**
 * @description 检查用户是否已购买商品
 * @param {string} productId - 商品ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function checkPurchased(productId, callbacks = {}) {
  return request.get('/order/check-purchased', { productId }, callbacks);
}

/**
 * @description 获取用户已购买的商品ID列表
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getPurchasedProducts(callbacks = {}) {
  return request.get('/order/purchased-products', null, callbacks);
}
