-- 多知识库基础结构：库负责检索边界，文件夹仅负责库内组织。
CREATE TABLE knowledge_base (
  id CHAR(36) NOT NULL COMMENT '知识库 UUID',
  name VARCHAR(50) NOT NULL COMMENT '知识库名称',
  description TEXT NULL COMMENT '知识库说明',
  color VARCHAR(16) NULL COMMENT '界面图标色',
  sort INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_base_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库';

CREATE TABLE knowledge_folder (
  id CHAR(36) NOT NULL COMMENT '文件夹 UUID',
  kb_id CHAR(36) NOT NULL COMMENT '所属知识库 UUID',
  parent_id CHAR(36) NULL COMMENT '父文件夹 UUID',
  name VARCHAR(50) NOT NULL COMMENT '文件夹名称',
  sort INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_folder_kb_parent_name (kb_id, parent_id, name),
  CONSTRAINT fk_knowledge_folder_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
  CONSTRAINT fk_knowledge_folder_parent FOREIGN KEY (parent_id) REFERENCES knowledge_folder(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文件夹';

-- document 原有数据保留不变，只增加可空归属字段以保持回滚兼容。
ALTER TABLE document ADD COLUMN kb_id CHAR(36) NULL COMMENT '所属知识库 UUID' AFTER id;
ALTER TABLE document ADD COLUMN folder_id CHAR(36) NULL COMMENT '所属文件夹 UUID' AFTER kb_id;
ALTER TABLE document ADD CONSTRAINT fk_document_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base(id) ON DELETE SET NULL;
ALTER TABLE document ADD CONSTRAINT fk_document_folder FOREIGN KEY (folder_id) REFERENCES knowledge_folder(id) ON DELETE SET NULL;
CREATE INDEX ix_document_kb_folder_updated ON document(kb_id, folder_id, updated_at DESC);

-- 稳定 UUID 让所有版本都可确定默认归属，包含软删除文档以保证恢复视图一致。
INSERT INTO knowledge_base (id, name, description, color, sort, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', '默认知识库', '自动创建的默认知识库', NULL, 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE id=id;
UPDATE document SET kb_id='00000000-0000-0000-0000-000000000001' WHERE kb_id IS NULL;
