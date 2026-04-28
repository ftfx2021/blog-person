/**
 * 评论状态常量
 */
export const COMMENT_STATUS = {
  PENDING: 0,     // 待审核
  APPROVED: 1,    // 已通过  
  REJECTED: 2     // 已拒绝
}

/**
 * 评论状态文本映射
 */
export const COMMENT_STATUS_TEXT = {
  [COMMENT_STATUS.PENDING]: '待审核',
  [COMMENT_STATUS.APPROVED]: '已通过', 
  [COMMENT_STATUS.REJECTED]: '已拒绝'
}

/**
 * 评论状态标签类型映射（Element Plus Tag组件）
 */
export const COMMENT_STATUS_TAG_TYPE = {
  [COMMENT_STATUS.PENDING]: 'warning',
  [COMMENT_STATUS.APPROVED]: 'success',
  [COMMENT_STATUS.REJECTED]: 'danger'
}

/**
 * 获取评论状态文本
 * @param {number} status - 状态值
 * @returns {string} 状态文本
 */
export function getCommentStatusText(status) {
  return COMMENT_STATUS_TEXT[status] || '未知';
}

/**
 * 获取评论状态标签类型
 * @param {number} status - 状态值
 * @returns {string} 标签类型
 */
export function getCommentStatusTagType(status) {
  return COMMENT_STATUS_TAG_TYPE[status] || 'info';
}
