-- 兼容早期数据库：001 已记录但标签表被遗漏或人工删除时，补齐知识库标签依赖。
CREATE TABLE IF NOT EXISTS tag (
  id CHAR(36) NOT NULL COMMENT '标签 UUID',
  name VARCHAR(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签';

-- entity_tag 可能同样在早期库中遗漏；独立补齐以支持文档多标签关联。
CREATE TABLE IF NOT EXISTS entity_tag (
  entity_type VARCHAR(32) NOT NULL COMMENT '实体类型',
  entity_id CHAR(36) NOT NULL COMMENT '实体 UUID',
  tag_id CHAR(36) NOT NULL COMMENT '标签 UUID',
  PRIMARY KEY (entity_type, entity_id, tag_id),
  CONSTRAINT fk_entity_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实体标签关联';
