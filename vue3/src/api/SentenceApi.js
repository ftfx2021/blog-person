import request from "@/utils/request";

/**
 * @description 分页查询句子列表
 * @param {object} params - 查询参数
 * @param {number} params.current - 当前页码
 * @param {number} params.size - 每页大小
 * @param {string} [params.keyword] - 关键词搜索
 * @param {number} [params.isHomepage] - 是否首页展示：0-否，1-是
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getSentencePage(params, callbacks = {}) {
  return request.get('/sentence/page', params, callbacks);
}

/**
 * @description 获取句子详情
 * @param {number} id - 句子ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getSentenceById(id, callbacks = {}) {
  return request.get(`/sentence/${id}`, null, callbacks);
}

/**
 * @description 创建句子
 * @param {object} data - 句子数据
 * @param {string} data.sentenceContent - 句子内容
 * @param {string} [data.keywords] - 关键词，多个用逗号隔开
 * @param {number} [data.isHomepage] - 是否首页展示：0-否，1-是
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function createSentence(data, callbacks = {}) {
  return request.post('/sentence', data, callbacks);
}

/**
 * @description 更新句子
 * @param {number} id - 句子ID
 * @param {object} data - 句子数据
 * @param {string} data.sentenceContent - 句子内容
 * @param {string} [data.keywords] - 关键词，多个用逗号隔开
 * @param {number} [data.isHomepage] - 是否首页展示：0-否，1-是
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function updateSentence(id, data, callbacks = {}) {
  return request.put(`/sentence/${id}`, data, callbacks);
}

/**
 * @description 删除句子
 * @param {number} id - 句子ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function deleteSentence(id, callbacks = {}) {
  return request.delete(`/sentence/${id}`, callbacks);
}

/**
 * @description 切换首页展示状态
 * @param {number} id - 句子ID
 * @param {number} isHomepage - 是否首页展示：0-否，1-是
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function toggleSentenceHomepage(id, isHomepage, callbacks = {}) {
  return request.put(`/sentence/${id}/homepage`, null, { 
    params: { isHomepage },
    ...callbacks 
  });
}

/**
 * @description 获取随机首页句子
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise}
 */
export function getRandomHomepageSentence(callbacks = {}) {
  return request.get('/sentence/random/homepage', null, callbacks);
}
