-- P1-8 体验修复：会话支持置顶，置顶会话在列表中优先展示。
ALTER TABLE ai_session
  ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶' AFTER updated_at;
