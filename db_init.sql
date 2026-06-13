-- =============================================
-- 计算机项目销售平台数据库初始化脚本
-- 版本: v1.0
-- 创建日期: 2025-10-15
-- 数据库: MySQL 8.0+
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `project_sales_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `project_sales_platform`;

-- =============================================
-- 1. 用户表 (user)
-- 说明: 用户基础信息表，支持角色区分
-- =============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `role_code` VARCHAR(50) DEFAULT 'USER' COMMENT '角色代码（USER:普通用户, ADMIN:管理员）',
    `name` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(200) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0:禁用, 1:正常）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_role_code` (`role_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 2. 商品分类表 (product_category)
-- 说明: 商品分类管理（支持两级树形结构）
-- =============================================
CREATE TABLE IF NOT EXISTS `product_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID（0表示一级分类）',
    `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `level` TINYINT DEFAULT 1 COMMENT '分类层级（1:一级, 2:二级）',
    `sort_order` INT DEFAULT 0 COMMENT '排序（数字越小越靠前）',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0:禁用, 1:启用）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- =============================================
-- 3. 文件信息表 (sys_file_info)
-- 说明: 统一文件管理表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_file_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型（MIME类型）',
    `business_type` VARCHAR(50) DEFAULT NULL COMMENT '业务类型（USER_AVATAR, PRODUCT_COVER, PRODUCT_IMAGE, PRODUCT_FILE）',
    `business_id` BIGINT DEFAULT NULL COMMENT '业务ID（关联的业务实体ID）',
    `business_field` VARCHAR(50) DEFAULT NULL COMMENT '业务字段（如demo_images）',
    `sort_order` INT DEFAULT 0 COMMENT '排序（用于多图片排序）',
    `is_temp` TINYINT DEFAULT 0 COMMENT '是否临时文件（0:否, 1:是）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_business` (`business_type`, `business_id`),
    KEY `idx_is_temp` (`is_temp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';

-- =============================================
-- 4. 商品表 (product)
-- 说明: 商品信息表
-- =============================================
CREATE TABLE IF NOT EXISTS `product` (
    `id` VARCHAR(36) NOT NULL COMMENT '商品ID（UUID）',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `product_desc` VARCHAR(500) DEFAULT NULL COMMENT '商品简介',
    `product_detail` TEXT DEFAULT NULL COMMENT '商品详情（富文本）',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `cover_image_id` BIGINT DEFAULT NULL COMMENT '封面图文件ID（关联sys_file_info）',
    `demo_image_ids` VARCHAR(500) DEFAULT NULL COMMENT '演示图片ID列表（逗号分隔，如：1,2,3）',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格（元）',
    `download_link` VARCHAR(500) DEFAULT NULL COMMENT '下载链接',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `sale_count` INT DEFAULT 0 COMMENT '销售数量',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0:下架, 1:上架）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_cover_image_id` (`cover_image_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `product_category` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_product_cover` FOREIGN KEY (`cover_image_id`) REFERENCES `sys_file_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- =============================================
-- 5. 订单表 (orders)
-- 说明: 订单信息表
-- =============================================
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号（唯一）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` VARCHAR(36) NOT NULL COMMENT '商品ID（UUID）',
    `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称（冗余字段）',
    `order_amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额（元）',
    `pay_type` TINYINT DEFAULT NULL COMMENT '支付方式（1:支付宝, 2:微信）',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `order_status` TINYINT DEFAULT 1 COMMENT '订单状态（1:待支付, 2:已支付, 3:已完成）',
    `third_pay_no` VARCHAR(64) DEFAULT NULL COMMENT '第三方支付流水号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_order_status` (`order_status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =============================================
-- 6. 下载记录表 (download_record)
-- 说明: 用户下载记录表
-- =============================================
CREATE TABLE IF NOT EXISTS `download_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` VARCHAR(36) NOT NULL COMMENT '商品ID（UUID）',
    `download_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_download_time` (`download_time`),
    CONSTRAINT `fk_download_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='下载记录表';

-- =============================================
-- 初始化数据
-- =============================================

-- 插入默认管理员账号（密码: admin123，需要BCrypt加密）
-- 注意: 实际使用时需要用BCrypt加密后的密码替换
INSERT INTO `user` (`username`, `password`, `email`, `role_code`, `name`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', 'ADMIN', '系统管理员', 1);
-- 密码: admin123

-- 插入默认商品分类
INSERT INTO `product_category` (`category_name`, `sort_order`, `status`) VALUES
('Web开发', 1, 1),
('移动应用', 2, 1),
('桌面应用', 3, 1),
('小程序', 4, 1),
('数据分析', 5, 1),
('人工智能', 6, 1),
('其他', 99, 1);

-- =============================================
-- 常用查询SQL示例
-- =============================================

-- 查询商品及其封面图
-- SELECT p.*, f.file_path as cover_image_path 
-- FROM product p
-- LEFT JOIN sys_file_info f ON p.cover_image_id = f.id
-- WHERE p.status = 1;

-- 查询商品的所有演示图片
-- SELECT * FROM sys_file_info 
-- WHERE business_type = 'PRODUCT_IMAGE' 
-- AND business_id = ? 
-- ORDER BY sort_order, create_time;

-- 查询用户已购买的商品
-- SELECT DISTINCT p.*, p.download_link 
-- FROM product p
-- INNER JOIN orders o ON p.id = o.product_id
-- WHERE o.user_id = ? AND o.order_status IN (2, 3)
-- ORDER BY o.pay_time DESC;

-- 查询用户的订单列表
-- SELECT o.*, p.product_name, p.cover_image_id
-- FROM orders o
-- LEFT JOIN product p ON o.product_id = p.id
-- WHERE o.user_id = ?
-- ORDER BY o.create_time DESC;

-- =============================================
-- 数据库初始化完成
-- =============================================
