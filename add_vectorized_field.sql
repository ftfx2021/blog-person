-- 给文章表添加是否向量化字段
ALTER TABLE article ADD COLUMN is_vectorized TINYINT(1) DEFAULT 0 COMMENT '是否已向量化(0:否,1:是)';
