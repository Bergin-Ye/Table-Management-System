-- ============================================
-- 金属厂数据管理系统 - 156项 + 结算机台数改造 迁移脚本
-- 日期: 2026-07-18
-- ============================================

USE metal_system;

-- 新增156项表
CREATE TABLE IF NOT EXISTS `base_material_156` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_id` BIGINT COMMENT '公司ID',
    `category` VARCHAR(100) COMMENT '类别',
    `material_code` VARCHAR(100) NOT NULL COMMENT '料号',
    `system_name` VARCHAR(200) COMMENT '系统名称',
    `part_name` VARCHAR(200) COMMENT '配件名称',
    `unit_usage` DECIMAL(10,4) COMMENT '单台机用量',
    `ratio` DECIMAL(10,4) COMMENT '比例',
    `unit_price_with_tax` DECIMAL(12,4) COMMENT '含税单价',
    `created_by` VARCHAR(50),
    `updated_by` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX `idx_b156_mcode` (`material_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- settlement_machine 新增统计月份字段
ALTER TABLE `settlement_machine` ADD COLUMN `stat_month` VARCHAR(7) COMMENT '统计月份 格式yyyy-MM';
