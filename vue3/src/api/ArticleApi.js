import request from "@/utils/request";

/**
 * @description 获取文章详情
 * @param {string} articleId - 文章ID
 * @param {object} [params={}] - 请求参数
 * @param {string} [params.password] - 访问密码（可选，仅密码保护文章需要）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getArticleDetail(articleId, params = {}, callbacks = {}) {
  return request.get(`/article/${articleId}`, params, callbacks);
}

/**
 * @description 分页获取文章列表
 * @param {object} params - 查询参数
 * @param {number} [params.currentPage=1] - 当前页码
 * @param {number} [params.size=10] - 每页大小
 * @param {string} [params.title] - 文章标题（可选）
 * @param {number} [params.categoryId] - 分类ID（可选）
 * @param {number} [params.status] - 文章状态（可选）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getArticlesPage(params, callbacks = {}) {
  return request.get('/article/page', params, callbacks);
}

/**
 * @description 根据分类获取文章列表
 * @param {number} categoryId - 分类ID
 * @param {object} params - 查询参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.size - 每页大小
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getArticlesByCategory(categoryId, params, callbacks = {}) {
  return request.get(`/article/category/${categoryId}`, params, callbacks);
}

/**
 * @description 获取推荐文章
 * @param {object} params - 查询参数
 * @param {number} [params.limit=5] - 限制数量
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getRecommendArticles(params = {}, callbacks = {}) {
  // 转换参数格式以匹配后端期望的单个limit参数
  const queryParams = {
    limit: params.limit || 5
  };
  return request.get('/article/recommend', queryParams, callbacks);
}

/**
 * @description 获取热门文章
 * @param {object} params - 查询参数
 * @param {number} [params.limit=5] - 限制数量
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getHotArticles(params = {}, callbacks = {}) {
  // 转换参数格式以匹配后端期望的单个limit参数
  const queryParams = {
    limit: params.limit || 5
  };
  return request.get('/article/hot', queryParams, callbacks);
}

/**
 * @description 点赞或取消点赞文章
 * @param {string} articleId - 文章ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function toggleArticleLike(articleId, callbacks = {}) {
  return request.post(`/article/${articleId}/like`, {}, callbacks);
}

/**
 * @description 检查文章点赞状态
 * @param {string} articleId - 文章ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function checkArticleLikeStatus(articleId, callbacks = {}) {
  return request.get(`/article/${articleId}/like/status`, null, callbacks);
}

/**
 * @description 收藏或取消收藏文章
 * @param {string} articleId - 文章ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function toggleArticleCollect(articleId, callbacks = {}) {
  return request.post(`/article/${articleId}/collect`, {}, callbacks);
}

/**
 * @description 检查文章收藏状态
 * @param {string} articleId - 文章ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function checkArticleCollectStatus(articleId, callbacks = {}) {
  return request.get(`/article/${articleId}/collect/status`, null, callbacks);
}

/**
 * @description 获取用户点赞的文章ID列表
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUserLikedArticleIds(callbacks = {}) {
  return request.get('/article/user/likes', null, callbacks);
}

/**
 * @description 获取用户点赞的文章分页列表
 * @param {object} params - 查询参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.size - 每页大小
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUserLikedArticlesPage(params, callbacks = {}) {
  return request.get('/article/user/likes/page', params, callbacks);
}

/**
 * @description 获取文章的点赞用户列表
 * @param {string} articleId - 文章ID
 * @param {object} params - 查询参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.size - 每页大小
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getArticleLikeUsers(articleId, params, callbacks = {}) {
  return request.get(`/article/${articleId}/like/users`, params, callbacks);
}

/**
 * @description 获取文章的收藏用户列表
 * @param {string} articleId - 文章ID
 * @param {object} params - 查询参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.size - 每页大小
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getArticleCollectUsers(articleId, params, callbacks = {}) {
  return request.get(`/article/${articleId}/collect/users`, params, callbacks);
}

/**
 * @description 获取用户收藏的文章ID列表
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUserCollectedArticleIds(callbacks = {}) {
  return request.get('/article/user/collects', null, callbacks);
}

/**
 * @description 获取用户收藏的文章分页列表
 * @param {object} params - 查询参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.size - 每页大小
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getUserCollectedArticlesPage(params, callbacks = {}) {
  return request.get('/article/user/collects/page', params, callbacks);
}

/**
 * @description 创建新文章
 * @param {object} articleData - 文章数据
 * @param {string} articleData.title - 文章标题
 * @param {string} articleData.content - 文章内容
 * @param {string} [articleData.htmlContent] - HTML内容  
 * @param {string} [articleData.summary] - 文章摘要
 * @param {string} [articleData.outline] - 文章大纲(JSON格式)
 * @param {string} [articleData.coverImage] - 封面图片
 * @param {number} [articleData.categoryId] - 分类ID
 * @param {number} [articleData.status=0] - 文章状态
 * @param {number} [articleData.isTop=0] - 是否置顶
 * @param {number} [articleData.isRecommend=0] - 是否推荐
 * @param {array} [articleData.tags] - 标签列表
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function createArticle(articleData, callbacks = {}) {
  // 转换为ArticleCreateDTO格式
  const createDTO = {
    title: articleData.title,
    content: articleData.content,
    htmlContent: articleData.htmlContent,
    summary: articleData.summary,
    outline: articleData.outline, // 添加大纲字段
    coverImage: articleData.coverImage,
    mainColor: articleData.mainColor, // 添加主色调字段
    categoryId: articleData.categoryId,
    status: articleData.status !== undefined ? articleData.status : 0,
    isTop: articleData.isTop !== undefined ? articleData.isTop : 0,
    isRecommend: articleData.isRecommend !== undefined ? articleData.isRecommend : 0,
    tags: articleData.tags || []
  };
  return request.post('/article', createDTO, callbacks);
}

/**
 * @description 创建新文章（支持UUID）
 * @param {object} articleData - 文章数据，包含预生成的UUID
 * @param {string} articleData.id - 预生成的文章UUID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function createArticleWithUUID(articleData, callbacks = {}) {
  return request.post('/article', articleData, callbacks);
}

/**
 * @description 更新文章
 * @param {string} articleId - 文章ID
 * @param {object} articleData - 文章数据
 * @param {string} [articleData.title] - 文章标题
 * @param {string} [articleData.content] - 文章内容
 * @param {string} [articleData.htmlContent] - HTML内容  
 * @param {string} [articleData.summary] - 文章摘要
 * @param {string} [articleData.outline] - 文章大纲(JSON格式)
 * @param {string} [articleData.coverImage] - 封面图片
 * @param {number} [articleData.categoryId] - 分类ID
 * @param {number} [articleData.status] - 文章状态
 * @param {number} [articleData.isTop] - 是否置顶
 * @param {number} [articleData.isRecommend] - 是否推荐
 * @param {array} [articleData.tags] - 标签列表
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateArticle(articleId, articleData, callbacks = {}) {
  // 转换为ArticleUpdateDTO格式
  const updateDTO = {
    title: articleData.title,
    content: articleData.content,
    htmlContent: articleData.htmlContent,
    summary: articleData.summary,
    outline: articleData.outline, // 添加大纲字段
    coverImage: articleData.coverImage,
    mainColor: articleData.mainColor, // 添加主色调字段
    categoryId: articleData.categoryId,
    status: articleData.status,
    isTop: articleData.isTop,
    isRecommend: articleData.isRecommend,
    tags: articleData.tags
  };
  return request.put(`/article/${articleId}`, updateDTO, callbacks);
}

/**
 * @description 更新文章状态
 * @param {string} articleId - 文章ID
 * @param {number} status - 新状态
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function updateArticleStatus(articleId, status, callbacks = {}) {
  return request.put(`/article/${articleId}/status?status=${status}`, null, callbacks);
}

/**
 * @description 删除文章
 * @param {string} articleId - 文章ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function deleteArticle(articleId, callbacks = {}) {
  return request.delete(`/article/${articleId}`, callbacks);
}

/**
 * @description 获取最新文章
 * @param {object} params - 查询参数
 * @param {number} [params.currentPage=1] - 当前页码
 * @param {number} [params.size=10] - 每页条数
 * @param {number} [params.status=1] - 文章状态
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getLatestArticles(params, callbacks = {}) {
  return request.get('/article/page', params, callbacks);
}

/**
 * @description 搜索文章
 * @param {object} params - 搜索参数
 * @param {string} params.keyword - 搜索关键词
 * @param {number} [params.currentPage=1] - 当前页码
 * @param {number} [params.size=10] - 每页条数
 * @param {number} [params.categoryId] - 分类ID（可选）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function searchArticles(params, callbacks = {}) {
  // 转换参数格式以匹配后端期望的格式
  const queryParams = {
    keyword: params.keyword,
    currentPage: params.currentPage || 1,
    size: params.size || 10
  };
  return request.get('/article/search', queryParams, callbacks);
}

// ========== 文章密码保护相关API ==========

/**
 * @description 设置文章密码保护
 * @param {string} articleId - 文章ID
 * @param {object} params - 密码设置参数
 * @param {boolean} params.enablePassword - 是否开启密码保护
 * @param {string} [params.expireTime] - 密码过期时间（可选，ISO格式）
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function setArticlePassword(articleId, params, callbacks = {}) {
  return request.post(`/article/${articleId}/password`, params, callbacks);
}

/**
 * @description 检查文章是否需要密码访问
 * @param {string} articleId - 文章ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function checkPasswordRequired(articleId, callbacks = {}) {
  return request.get(`/article/${articleId}/password/required`, null, callbacks);
}

/**
 * @description 获取文章密码信息（仅管理员或文章作者）
 * @param {string} articleId - 文章ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getArticlePassword(articleId, callbacks = {}) {
  return request.get(`/article/${articleId}/password`, null, callbacks);
}

/**
 * @description 重新生成文章密码
 * @param {string} articleId - 文章ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function regenerateArticlePassword(articleId, callbacks = {}) {
  return request.post(`/article/${articleId}/password/regenerate`, null, callbacks);
}

// ========== 文章向量化相关API ==========

/**
 * @description 获取文章向量元数据
 * @param {string} articleId - 文章ID
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getArticleVectorMetadata(articleId, callbacks = {}) {
  return request.get(`/article/${articleId}/vector-metadata`, null, callbacks);
}

/**
 * @description 获取知识库统计信息
 * @param {object} [callbacks={}] - 回调函数
 * @returns {Promise} 由request工具返回的Promise
 */
export function getKnowledgeBaseStats(callbacks = {}) {
  return request.get('/article/knowledge-base/stats', null, callbacks);
}
