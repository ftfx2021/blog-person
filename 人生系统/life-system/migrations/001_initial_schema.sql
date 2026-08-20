-- 人生系统 P0 全量基线：所有表使用 InnoDB、utf8mb4，并按 UTC DATETIME(3) 存储时间。

-- 迁移版本表：runner 以此保证重复执行时只应用未执行版本。
CREATE TABLE IF NOT EXISTS schema_migrations (
  id VARCHAR(128) NOT NULL COMMENT '迁移版本标识',
  applied_at DATETIME(3) NOT NULL COMMENT '迁移应用时间（UTC）',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库迁移版本';

-- 目标表：只保存目标事实，进度始终在查询时计算，避免行动数量污染结果。
CREATE TABLE goal (
  id CHAR(36) NOT NULL COMMENT '目标 UUID',
  title VARCHAR(50) NOT NULL COMMENT '目标标题',
  description TEXT NULL COMMENT '目标说明',
  period VARCHAR(16) NOT NULL DEFAULT 'quarterly' COMMENT '目标周期',
  metric_type VARCHAR(16) NOT NULL COMMENT '度量方式',
  unit VARCHAR(32) NULL COMMENT '数值单位',
  start_value DOUBLE NULL COMMENT '数值型起点',
  target_value DOUBLE NULL COMMENT '数值型终点',
  status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '目标状态',
  due_date DATETIME(3) NULL COMMENT '截止时间（UTC）',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  -- 周期 CHECK：严格限制为 FSD 枚举，拒绝无法解释的周期值。
  CONSTRAINT ck_goal_period CHECK (period IN ('annual', 'quarterly', 'monthly')),
  -- 度量 CHECK：三种度量方式与产品规则保持一致。
  CONSTRAINT ck_goal_metric_type CHECK (metric_type IN ('numeric', 'milestone', 'status')),
  -- 状态 CHECK：结束态不可由任意字符串绕过状态机。
  CONSTRAINT ck_goal_status CHECK (status IN ('active', 'done', 'abandoned')),
  -- 数值组合 CHECK：数值目标必须有不同起终点，其他目标不得偷存数值。
  CONSTRAINT ck_goal_numeric_values CHECK (
    (metric_type = 'numeric' AND start_value IS NOT NULL AND target_value IS NOT NULL AND target_value <> start_value)
    OR (metric_type <> 'numeric' AND start_value IS NULL AND target_value IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人生目标';

-- 目标数据点：记录真实测量结果，不在 goal 表写入派生进度。
CREATE TABLE goal_record (
  id CHAR(36) NOT NULL COMMENT '数据点 UUID',
  goal_id CHAR(36) NOT NULL COMMENT '所属目标 UUID',
  value DOUBLE NOT NULL COMMENT '真实记录值',
  note TEXT NULL COMMENT '记录备注',
  recorded_at DATETIME(3) NOT NULL COMMENT '记录发生时间（UTC）',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  PRIMARY KEY (id),
  CONSTRAINT fk_goal_record_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标真实数据点';

-- 里程碑：仅用于 milestone 目标，完成比例由已完成数量查询计算。
CREATE TABLE milestone (
  id CHAR(36) NOT NULL COMMENT '里程碑 UUID',
  goal_id CHAR(36) NOT NULL COMMENT '所属目标 UUID',
  title VARCHAR(100) NOT NULL COMMENT '里程碑标题',
  is_done TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成',
  done_at DATETIME(3) NULL COMMENT '完成时间（UTC）',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '同目标内排序',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  -- 完成标志 CHECK：布尔列只允许 0/1。
  CONSTRAINT ck_milestone_is_done CHECK (is_done IN (0, 1)),
  CONSTRAINT fk_milestone_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标里程碑';

-- 生活项目：与开发项目分离，可选挂接一个目标。
CREATE TABLE project (
  id CHAR(36) NOT NULL COMMENT '生活项目 UUID',
  goal_id CHAR(36) NULL COMMENT '可选目标 UUID',
  title VARCHAR(50) NOT NULL COMMENT '项目标题',
  description TEXT NULL COMMENT '项目说明',
  status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '项目状态',
  start_at DATETIME(3) NULL COMMENT '开始时间（UTC）',
  end_at DATETIME(3) NULL COMMENT '结束时间（UTC）',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  -- 项目状态 CHECK：只允许 active/paused/done 状态机值。
  CONSTRAINT ck_project_status CHECK (status IN ('active', 'done', 'paused')),
  CONSTRAINT fk_project_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生活项目';

-- 待办：目标与生活项目均为松耦合可空外键，不设优先级字段。
CREATE TABLE task (
  id CHAR(36) NOT NULL COMMENT '待办 UUID',
  goal_id CHAR(36) NULL COMMENT '可选目标 UUID',
  project_id CHAR(36) NULL COMMENT '可选生活项目 UUID',
  title VARCHAR(100) NOT NULL COMMENT '待办标题',
  note TEXT NULL COMMENT '待办备注',
  due_date DATETIME(3) NULL COMMENT '截止时间（UTC）',
  status VARCHAR(16) NOT NULL DEFAULT 'todo' COMMENT '待办状态',
  completed_at DATETIME(3) NULL COMMENT '完成时间（UTC）',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  -- 待办状态 CHECK：状态推进仅能使用 todo/doing/done。
  CONSTRAINT ck_task_status CHECK (status IN ('todo', 'doing', 'done')),
  CONSTRAINT fk_task_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE SET NULL,
  CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='待办';

-- 习惯：streak 是根据打卡历史回算的读取优化值，不参与目标进度。
CREATE TABLE habit (
  id CHAR(36) NOT NULL COMMENT '习惯 UUID',
  name VARCHAR(50) NOT NULL COMMENT '习惯名称',
  note TEXT NULL COMMENT '习惯备注',
  frequency_type VARCHAR(20) NOT NULL DEFAULT 'daily' COMMENT '频率类型',
  weekly_target INT NULL COMMENT '每周目标次数',
  streak INT NOT NULL DEFAULT 0 COMMENT '当前连续周期',
  last_done_on DATE NULL COMMENT '最近打卡的本地自然日',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  -- 频率 CHECK：只允许每日或每周次数。
  CONSTRAINT ck_habit_frequency_type CHECK (frequency_type IN ('daily', 'weekly_times')),
  -- 周目标 CHECK：weekly_target 存在时只能为 1 到 7。
  CONSTRAINT ck_habit_weekly_target CHECK (weekly_target BETWEEN 1 AND 7),
  -- 互斥 CHECK：daily 不得带周目标，weekly_times 必须带周目标。
  CONSTRAINT ck_habit_frequency_values CHECK (
    (frequency_type = 'daily' AND weekly_target IS NULL)
    OR (frequency_type = 'weekly_times' AND weekly_target IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='习惯';

-- 习惯打卡：唯一键保证同一习惯同一天幂等。
CREATE TABLE habit_checkin (
  id CHAR(36) NOT NULL COMMENT '打卡 UUID',
  habit_id CHAR(36) NOT NULL COMMENT '习惯 UUID',
  checked_on DATE NOT NULL COMMENT '用户本地打卡自然日',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_habit_checkin_habit_day (habit_id, checked_on),
  CONSTRAINT fk_habit_checkin_habit FOREIGN KEY (habit_id) REFERENCES habit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='习惯打卡';

-- 收藏箱：P1 业务预留，本次只建立架构定义的数据结构。
CREATE TABLE inbox_item (
  id CHAR(36) NOT NULL COMMENT '收藏 UUID', kind VARCHAR(20) NOT NULL COMMENT '收藏类型',
  url TEXT NULL COMMENT '来源网址', title TEXT NOT NULL COMMENT '标题', note TEXT NULL COMMENT '备注',
  status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态', document_id CHAR(36) NULL COMMENT '入库文档 UUID',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  -- 收藏类型 CHECK：严格对应 FSD 枚举。
  CONSTRAINT ck_inbox_item_kind CHECK (kind IN ('link', 'snippet', 'read_later')),
  -- 收藏状态 CHECK：严格对应 FSD 枚举。
  CONSTRAINT ck_inbox_item_status CHECK (status IN ('pending', 'clipped', 'bookmarked', 'discarded'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏箱';

-- 文档：P1 入库事实表，P0 建好中文全文索引能力。
CREATE TABLE document (
  id CHAR(36) NOT NULL COMMENT '文档 UUID', title TEXT NOT NULL COMMENT '标题',
  doc_type VARCHAR(20) NOT NULL COMMENT '文档类型', source_url TEXT NULL COMMENT '来源网址', source_path TEXT NULL COMMENT '来源路径',
  stored_path TEXT NULL COMMENT '应用内文件路径', raw_text LONGTEXT NOT NULL COMMENT '规范化正文',
  parse_status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '解析状态', index_status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '索引状态',
  promoted_from_timeline_id CHAR(36) NULL COMMENT '升格来源时间线 UUID', deleted_at DATETIME(3) NULL COMMENT '软删除时间（UTC）',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  -- 文档类型 CHECK：包含架构定义的全部长期语料类型。
  CONSTRAINT ck_document_doc_type CHECK (doc_type IN ('webpage', 'pdf', 'docx', 'markdown', 'txt', 'html', 'note', 'skill', 'prompt', 'promoted')),
  -- 解析状态 CHECK：导入开始为 pending，成功后才允许 ready。
  CONSTRAINT ck_document_parse_status CHECK (parse_status IN ('pending', 'ready', 'failed')),
  -- 索引状态 CHECK：覆盖初始、进行中、完成、失败与失效。
  CONSTRAINT ck_document_index_status CHECK (index_status IN ('pending', 'indexing', 'ready', 'failed', 'stale'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识文档';

-- 文档分块：P1 向量索引的稳定事实源，序号在文档内唯一。
CREATE TABLE chunk (
  id CHAR(36) NOT NULL COMMENT '分块 UUID', document_id CHAR(36) NOT NULL COMMENT '文档 UUID', content LONGTEXT NOT NULL COMMENT '分块正文',
  seq_no INT NOT NULL COMMENT '文档内序号', token_count INT NOT NULL COMMENT '令牌数量', metadata_json JSON NOT NULL COMMENT '结构元数据',
  content_hash VARCHAR(64) NOT NULL COMMENT '正文 SHA-256', created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  PRIMARY KEY (id), UNIQUE KEY uk_chunk_document_seq (document_id, seq_no),
  CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档分块';

-- 时间线：P1 沉淀数据，未升格内容不得进入 P0 搜索。
CREATE TABLE timeline_entry (
  id CHAR(36) NOT NULL COMMENT '时间线 UUID', type VARCHAR(16) NOT NULL COMMENT '条目类型', title TEXT NULL COMMENT '标题', content LONGTEXT NOT NULL COMMENT '内容',
  document_id CHAR(36) NULL COMMENT '升格后的文档 UUID', created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  -- 时间线类型 CHECK：严格对应 FSD 枚举。
  CONSTRAINT ck_timeline_entry_type CHECK (type IN ('diary', 'idea', 'decision', 'reading', 'review')),
  CONSTRAINT fk_timeline_document FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='沉淀时间线';

-- 情绪词：P1 独立心理健康模块，不参与搜索与 RAG。
CREATE TABLE mood_word (
  id CHAR(36) NOT NULL COMMENT '情绪词 UUID', name VARCHAR(50) NOT NULL COMMENT '情绪词', is_builtin TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内置',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', PRIMARY KEY (id), UNIQUE KEY uk_mood_word_name (name),
  -- 内置标志 CHECK：布尔列只允许 0/1。
  CONSTRAINT ck_mood_word_is_builtin CHECK (is_builtin IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='情绪词';

-- 情绪记录：P1 私密事实表，不得被 P0 搜索服务引用。
CREATE TABLE mood_record (
  id CHAR(36) NOT NULL COMMENT '情绪记录 UUID', mood_word_id CHAR(36) NULL COMMENT '情绪词 UUID', custom_mood_word VARCHAR(50) NULL COMMENT '自定义情绪词',
  intensity INT NOT NULL COMMENT '强度 1 到 5', event TEXT NOT NULL COMMENT '事件', context TEXT NULL COMMENT '情境', need TEXT NULL COMMENT '当下需求',
  recorded_at DATETIME(3) NOT NULL COMMENT '记录时间（UTC）', created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', PRIMARY KEY (id),
  -- 强度 CHECK：仅允许五级量表。
  CONSTRAINT ck_mood_record_intensity CHECK (intensity BETWEEN 1 AND 5),
  -- 情绪词来源 CHECK：内置或自定义至少存在一个。
  CONSTRAINT ck_mood_record_word CHECK (mood_word_id IS NOT NULL OR custom_mood_word IS NOT NULL),
  CONSTRAINT fk_mood_record_word FOREIGN KEY (mood_word_id) REFERENCES mood_word(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='情绪记录';

-- 情绪报告：P1 周期报告，同一周期只保留一份事实记录。
CREATE TABLE mood_report (
  id CHAR(36) NOT NULL COMMENT '报告 UUID', period_start DATETIME(3) NOT NULL COMMENT '周期开始（UTC）', period_end DATETIME(3) NOT NULL COMMENT '周期结束（UTC）',
  model_name VARCHAR(128) NULL COMMENT '模型名称', input_snapshot_json JSON NOT NULL COMMENT '输入快照', content LONGTEXT NOT NULL COMMENT '报告正文',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', PRIMARY KEY (id), UNIQUE KEY uk_mood_report_period (period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='情绪周期报告';

-- 开发项目：P2 交付模块，与生活 project 表严格分离。
CREATE TABLE dev_project (
  id CHAR(36) NOT NULL COMMENT '开发项目 UUID', name VARCHAR(100) NOT NULL COMMENT '项目名称', description TEXT NULL COMMENT '说明', repo_url TEXT NULL COMMENT '仓库网址',
  status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '项目状态', current_stage_id CHAR(36) NULL COMMENT '当前阶段 UUID',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）', PRIMARY KEY (id),
  -- 开发项目状态 CHECK：严格对应 FSD 枚举。
  CONSTRAINT ck_dev_project_status CHECK (status IN ('active', 'done', 'paused', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开发项目';

-- 开发阶段：P2 自定义状态流，同项目名称与顺序都唯一。
CREATE TABLE dev_project_stage (
  id CHAR(36) NOT NULL COMMENT '阶段 UUID', dev_project_id CHAR(36) NOT NULL COMMENT '开发项目 UUID', name VARCHAR(100) NOT NULL COMMENT '阶段名称',
  sort_order INT NOT NULL COMMENT '阶段顺序', is_terminal TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否终止阶段', PRIMARY KEY (id),
  UNIQUE KEY uk_dev_project_stage_order (dev_project_id, sort_order), UNIQUE KEY uk_dev_project_stage_name (dev_project_id, name),
  -- 终止阶段 CHECK：布尔列只允许 0/1。
  CONSTRAINT ck_dev_project_stage_terminal CHECK (is_terminal IN (0, 1)),
  CONSTRAINT fk_dev_project_stage_project FOREIGN KEY (dev_project_id) REFERENCES dev_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开发项目阶段';

-- 项目想法：P2 创意漏斗，可选立项为开发项目。
CREATE TABLE project_idea (
  id CHAR(36) NOT NULL COMMENT '想法 UUID', title VARCHAR(100) NOT NULL COMMENT '标题', description TEXT NULL COMMENT '说明', status VARCHAR(16) NOT NULL DEFAULT 'inbox' COMMENT '漏斗状态',
  value_score INT NULL COMMENT '价值评分', feasibility_score INT NULL COMMENT '可行性评分', evaluation_note TEXT NULL COMMENT '评估说明',
  launched_to_project_id CHAR(36) NULL COMMENT '立项后的开发项目 UUID', created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）', PRIMARY KEY (id),
  -- 想法状态 CHECK：严格对应 FSD 漏斗枚举。
  CONSTRAINT ck_project_idea_status CHECK (status IN ('inbox', 'evaluating', 'approved', 'discarded', 'launched')),
  -- 价值评分 CHECK：存在时限制为 1 到 5。
  CONSTRAINT ck_project_idea_value CHECK (value_score BETWEEN 1 AND 5),
  -- 可行性评分 CHECK：存在时限制为 1 到 5。
  CONSTRAINT ck_project_idea_feasibility CHECK (feasibility_score BETWEEN 1 AND 5),
  CONSTRAINT fk_project_idea_project FOREIGN KEY (launched_to_project_id) REFERENCES dev_project(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开发项目想法';

-- 标签：使用大小写和重音不敏感排序规则落实名称唯一性。
CREATE TABLE tag (
  id CHAR(36) NOT NULL COMMENT '标签 UUID', name VARCHAR(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', PRIMARY KEY (id), UNIQUE KEY uk_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签';

-- 实体标签：多态实体端由应用层校验，标签端保留数据库外键。
CREATE TABLE entity_tag (
  entity_type VARCHAR(32) NOT NULL COMMENT '实体类型', entity_id CHAR(36) NOT NULL COMMENT '实体 UUID', tag_id CHAR(36) NOT NULL COMMENT '标签 UUID',
  PRIMARY KEY (entity_type, entity_id, tag_id), CONSTRAINT fk_entity_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实体标签关联';

-- 实体关联：两端均为多态引用，按架构要求不设置数据库外键。
CREATE TABLE entity_link (
  id CHAR(36) NOT NULL COMMENT '关联 UUID', from_type VARCHAR(32) NOT NULL COMMENT '来源实体类型', from_id CHAR(36) NOT NULL COMMENT '来源实体 UUID',
  to_type VARCHAR(32) NOT NULL COMMENT '目标实体类型', to_id CHAR(36) NOT NULL COMMENT '目标实体 UUID', relation VARCHAR(64) NULL COMMENT '关系语义',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）', PRIMARY KEY (id), UNIQUE KEY uk_entity_link_relation (from_type, from_id, to_type, to_id, relation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多态实体关联';

-- 应用设置：受控 key 与 Zod schema 在应用层校验，JSON 不允许任意页面直写。
CREATE TABLE app_setting (
  `key` VARCHAR(128) NOT NULL COMMENT '受控设置键', value_json JSON NOT NULL COMMENT '版本化设置值', updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用设置';

-- 以下索引逐项对应架构文档 4.2，用于 P0 高频筛选与稳定排序。
CREATE INDEX ix_goal_status_due ON goal(status, due_date);
CREATE INDEX ix_goal_record_goal_time ON goal_record(goal_id, recorded_at DESC);
CREATE INDEX ix_milestone_goal_order ON milestone(goal_id, sort_order);
CREATE INDEX ix_project_goal ON project(goal_id);
CREATE INDEX ix_task_status_due ON task(status, due_date);
CREATE INDEX ix_task_goal ON task(goal_id);
CREATE INDEX ix_task_project ON task(project_id);
CREATE INDEX ix_habit_checkin_habit_day ON habit_checkin(habit_id, checked_on DESC);
CREATE INDEX ix_document_type_updated ON document(doc_type, updated_at DESC);
CREATE INDEX ix_chunk_document_seq ON chunk(document_id, seq_no);
CREATE INDEX ix_timeline_created ON timeline_entry(created_at DESC);
CREATE INDEX ix_mood_record_time ON mood_record(recorded_at DESC);
CREATE INDEX ix_dev_project_status ON dev_project(status, updated_at DESC);
CREATE INDEX ix_entity_tag_tag ON entity_tag(tag_id, entity_type);
CREATE INDEX ix_entity_link_from ON entity_link(from_type, from_id);
CREATE INDEX ix_entity_link_to ON entity_link(to_type, to_id);

-- 中文 FULLTEXT：ngram parser 支持标题与正文的自然语言模式检索。
ALTER TABLE document ADD FULLTEXT INDEX ft_document_content (title, raw_text) WITH PARSER ngram;

-- 补挂收藏到文档外键：延后执行用于解决 inbox_item 与 document 的建表顺序。
ALTER TABLE inbox_item ADD CONSTRAINT fk_inbox_item_document FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE SET NULL;
-- 补挂升格来源外键：延后执行用于解决 document 与 timeline_entry 的循环依赖。
ALTER TABLE document ADD CONSTRAINT fk_document_timeline FOREIGN KEY (promoted_from_timeline_id) REFERENCES timeline_entry(id) ON DELETE SET NULL;
-- 补挂开发项目当前阶段外键：阶段表建立后才可安全引用。
ALTER TABLE dev_project ADD CONSTRAINT fk_dev_project_current_stage FOREIGN KEY (current_stage_id) REFERENCES dev_project_stage(id) ON DELETE SET NULL;
