/*
 多智能体写作工作流 - 数据层初始化脚本

 使用说明：
 1. 在业务库（如 blog / personal_blog）中执行本脚本。
 2. 仅创建工作流相关三张表，不影响现有业务表。
 3. 已按要求创建索引：task_id、status、created_at。
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_workflow_task
-- ----------------------------
DROP TABLE IF EXISTS `ai_workflow_task`;
CREATE TABLE `ai_workflow_task`  (
  `task_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务ID（业务主键）',
  `article_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联文章ID',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/RUNNING/PARTIAL_SUCCESS/FAILED/COMPLETED',
  `input_payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '任务输入快照(JSON)',
  `final_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '任务结果快照(JSON)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '失败原因',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建人ID',
  `started_at` datetime NULL DEFAULT NULL COMMENT '开始执行时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`) USING BTREE,
  INDEX `idx_ai_workflow_task_status`(`status`) USING BTREE,
  INDEX `idx_ai_workflow_task_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI写作工作流任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_workflow_step
-- ----------------------------
DROP TABLE IF EXISTS `ai_workflow_step`;
CREATE TABLE `ai_workflow_step`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '步骤记录ID',
  `task_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务ID',
  `step_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '步骤编码',
  `step_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '步骤名称',
  `step_order` int(11) NOT NULL DEFAULT 0 COMMENT '步骤顺序',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '步骤状态：PENDING/RUNNING/SUCCESS/FAILED/SKIPPED',
  `input_snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '步骤输入快照(JSON)',
  `output_snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '步骤输出快照(JSON)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '步骤失败原因',
  `retry_count` int(11) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `started_at` datetime NULL DEFAULT NULL COMMENT '开始执行时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_workflow_step_task_id`(`task_id`) USING BTREE,
  INDEX `idx_ai_workflow_step_status`(`status`) USING BTREE,
  INDEX `idx_ai_workflow_step_create_time`(`create_time`) USING BTREE,
  UNIQUE INDEX `uk_ai_workflow_step_task_order`(`task_id`, `step_order`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI写作工作流步骤表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_workflow_event
-- ----------------------------
DROP TABLE IF EXISTS `ai_workflow_event`;
CREATE TABLE `ai_workflow_event`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '事件记录ID',
  `task_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务ID',
  `step_id` bigint(20) NULL DEFAULT NULL COMMENT '步骤ID',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型：agent_started/tool_called/agent_completed/fallback_used',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '事件发生时状态快照',
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '事件描述',
  `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工具名',
  `tool_input_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '工具输入摘要',
  `tool_output_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '工具输出摘要',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_workflow_event_task_id`(`task_id`) USING BTREE,
  INDEX `idx_ai_workflow_event_status`(`status`) USING BTREE,
  INDEX `idx_ai_workflow_event_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI写作工作流事件表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;

