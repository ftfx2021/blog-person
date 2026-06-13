import request, { aiRequest } from "@/utils/request";
import { createAiRequestWrapper } from "@/utils/aiRequestUtils";

/**
 * AI智能生成文章标题（原始版本）
 */
function _generateArticleTitle(params, callbacks = {}) {
  return aiRequest.post('/article/ai/generate-title', params, callbacks);
}

/**
 * AI智能生成文章标题（增强版本，带超时处理和重试）
 * @param {object} params - 请求参数
 * @param {string} params.content - 文章内容
 * @param {string} [params.currentTitle] - 当前标题
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export const generateArticleTitle = createAiRequestWrapper(_generateArticleTitle);

/**
 * AI智能生成文章摘要（原始版本）
 */
function _generateArticleSummary(params, callbacks = {}) {
  return aiRequest.post('/article/ai/generate-summary', params, callbacks);
}

/**
 * AI智能生成文章摘要（增强版本，带超时处理和重试）
 * @param {object} params - 请求参数
 * @param {string} params.content - 文章内容
 * @param {number} [params.targetSummaryLength] - 目标摘要长度
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export const generateArticleSummary = createAiRequestWrapper(_generateArticleSummary);

/**
 * AI智能生成文章大纲（原始版本）
 */
function _generateArticleOutline(params, callbacks = {}) {
  return aiRequest.post('/article/ai/generate-outline', params, callbacks);
}

/**
 * AI智能生成文章大纲（增强版本，带超时处理和重试）
 * @param {object} params - 请求参数
 * @param {string} params.content - 文章内容
 * @param {boolean} [params.includeReadingTimeEstimation] - 是否包含阅读时间估算
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export const generateArticleOutline = createAiRequestWrapper(_generateArticleOutline);

/**
 * AI智能推荐文章标签（原始版本）
 */
function _recommendArticleTags(params, callbacks = {}) {
  return aiRequest.post('/article/ai/recommend-tags', params, callbacks);
}

/**
 * AI智能推荐文章标签（增强版本，带超时处理和重试）
 * @param {object} params - 请求参数
 * @param {string} params.content - 文章内容
 * @param {string} [params.currentTitle] - 当前标题
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export const recommendArticleTags = createAiRequestWrapper(_recommendArticleTags);

/**
 * AI智能推荐文章分类（原始版本）
 */
function _recommendArticleCategory(params, callbacks = {}) {
  return aiRequest.post('/article/ai/recommend-category', params, callbacks);
}

/**
 * AI智能推荐文章分类（增强版本，带超时处理和重试）
 * @param {object} params - 请求参数
 * @param {string} params.content - 文章内容
 * @param {string} [params.currentTitle] - 当前标题
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export const recommendArticleCategory = createAiRequestWrapper(_recommendArticleCategory);


