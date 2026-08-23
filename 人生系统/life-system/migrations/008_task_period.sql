-- 待办时间范围只做用户归类，不影响截止时间语义或既有状态机。
ALTER TABLE task
  ADD COLUMN period VARCHAR(16) NOT NULL DEFAULT 'other' COMMENT '时间范围归类（day/week/month/semester/other）',
  ADD CONSTRAINT ck_task_period CHECK (period IN ('day', 'week', 'month', 'semester', 'other'));
