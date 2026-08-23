-- 人生系统 P1-8：AI 助手体系（助手/会话/消息），沿用 InnoDB、utf8mb4 与 UTC DATETIME(3)。
CREATE TABLE ai_assistant (
  id CHAR(36) NOT NULL COMMENT '助手 UUID',
  name VARCHAR(50) NOT NULL COMMENT '助手名称',
  description VARCHAR(200) NULL COMMENT '助手简介',
  system_prompt TEXT NOT NULL COMMENT '系统提示词（本期唯一模型配置）',
  model_config_json JSON NULL COMMENT '未来：每助手模型设置，本期不启用',
  knowledge_base_id CHAR(36) NULL COMMENT '未来：关联知识库，本期不启用',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 助手';

CREATE TABLE ai_session (
  id CHAR(36) NOT NULL COMMENT '会话 UUID',
  assistant_id CHAR(36) NOT NULL COMMENT '所属助手',
  title VARCHAR(100) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  KEY ix_ai_session_assistant (assistant_id, updated_at DESC),
  CONSTRAINT fk_ai_session_assistant FOREIGN KEY (assistant_id) REFERENCES ai_assistant(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话';

CREATE TABLE ai_message (
  id CHAR(36) NOT NULL COMMENT '消息 UUID',
  session_id CHAR(36) NOT NULL COMMENT '所属会话',
  role VARCHAR(16) NOT NULL COMMENT '消息角色：user/assistant',
  content MEDIUMTEXT NOT NULL COMMENT '消息内容',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  PRIMARY KEY (id),
  KEY ix_ai_message_session (session_id, created_at),
  CONSTRAINT fk_ai_message_session FOREIGN KEY (session_id) REFERENCES ai_session(id) ON DELETE CASCADE,
  CONSTRAINT ck_ai_message_role CHECK (role IN ('user', 'assistant'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话消息';
