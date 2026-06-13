-- 文件模块重构 - 添加URL字段并删除旧字段
-- 执行时间：2025-01-22

-- ============================================
-- 商品表 (product) - 添加URL字段
-- ============================================

-- 添加封面图URL字段
ALTER TABLE product ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(500) COMMENT '封面图片URL';

-- 添加演示图片URL列表字段（JSON数组）
ALTER TABLE product ADD COLUMN IF NOT EXISTS demo_image_urls TEXT COMMENT '演示图片URL列表（JSON数组）';

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_product_cover_url ON product(cover_image_url(255));

-- ============================================
-- 删除旧字段
-- ============================================

-- 删除旧的文件ID字段
ALTER TABLE product DROP COLUMN IF EXISTS cover_image_id;
ALTER TABLE product DROP COLUMN IF EXISTS demo_image_ids;
