-- P1-8 体验修复：AI 消息增加可选推理内容，思考模式开启时保存。
ALTER TABLE ai_message
  ADD COLUMN reasoning MEDIUMTEXT NULL COMMENT '推理内容（思考模式开启时）' AFTER content;
