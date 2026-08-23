-- 习惯状态独立于打卡历史，暂停和归档均不会删除既有记录。
ALTER TABLE habit
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '习惯状态',
  ADD CONSTRAINT ck_habit_status CHECK (status IN ('active', 'paused', 'archived'));
