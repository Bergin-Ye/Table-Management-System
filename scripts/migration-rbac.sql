-- ============================================
-- RBAC 权限迁移脚本
-- 执行日期: 2026-07-16
-- ============================================

USE metal_system;

-- 1. sys_user 添加 role 字段
ALTER TABLE `sys_user`
    ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin / user'
    AFTER `real_name`;

-- 2. 设置第一个用户为管理员 (id=1 的 admin 用户)
UPDATE `sys_user` SET `role` = 'admin' WHERE `id` = 1;

-- 3. material 表添加 created_by / updated_by
ALTER TABLE `material`
    ADD COLUMN `created_by` VARCHAR(50) COMMENT '创建人'
    AFTER `material_code`,
    ADD COLUMN `updated_by` VARCHAR(50) COMMENT '最后修改人'
    AFTER `created_by`;

-- 4. delivery_stats 表添加 created_by / updated_by
ALTER TABLE `delivery_stats`
    ADD COLUMN `created_by` VARCHAR(50) COMMENT '创建人'
    AFTER `year_month`,
    ADD COLUMN `updated_by` VARCHAR(50) COMMENT '最后修改人'
    AFTER `created_by`;

-- 5. settlement_machine 表添加 created_by / updated_by
ALTER TABLE `settlement_machine`
    ADD COLUMN `created_by` VARCHAR(50) COMMENT '创建人'
    AFTER `settlement_machine_count`,
    ADD COLUMN `updated_by` VARCHAR(50) COMMENT '最后修改人'
    AFTER `created_by`;

-- 6. machine_detail 表添加 created_by / updated_by
ALTER TABLE `machine_detail`
    ADD COLUMN `created_by` VARCHAR(50) COMMENT '创建人'
    AFTER `machine_brand`,
    ADD COLUMN `updated_by` VARCHAR(50) COMMENT '最后修改人'
    AFTER `created_by`;

-- 7. machine_count 表添加 created_by / updated_by
ALTER TABLE `machine_count`
    ADD COLUMN `created_by` VARCHAR(50) COMMENT '创建人'
    AFTER `remark`,
    ADD COLUMN `updated_by` VARCHAR(50) COMMENT '最后修改人'
    AFTER `created_by`;
