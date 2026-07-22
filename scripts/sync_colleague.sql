-- ============================================================
-- 同事数据库同步脚本（在 metal_system 库执行）
-- MySQL 8.0+ 均可执行，已存在的会报错但不影响
-- ============================================================

-- 1. 156项唯一约束改为 (material_code, company_id) 联合
ALTER TABLE base_material_156 DROP INDEX idx_b156_mcode;
ALTER TABLE base_material_156 DROP INDEX idx_b156_mcode_company;
ALTER TABLE base_material_156 ADD UNIQUE KEY idx_b156_mcode_company (material_code, company_id);

-- 2. 原始记录加"单据号"列
ALTER TABLE original_record ADD COLUMN document_no VARCHAR(100) DEFAULT NULL COMMENT '单据号' AFTER delivery_record_ref;

-- 3. OCR调用日志表
CREATE TABLE IF NOT EXISTS ocr_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_type VARCHAR(50) NOT NULL,
    user_id BIGINT DEFAULT NULL,
    username VARCHAR(100) DEFAULT NULL,
    image_size BIGINT DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. 用户在线状态表
CREATE TABLE IF NOT EXISTS user_online (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    last_active_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
