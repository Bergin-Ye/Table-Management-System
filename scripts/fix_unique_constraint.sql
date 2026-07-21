-- 修复156项表唯一约束：从单列(material_code)改为联合(material_code, company_id)
-- 这样不同公司可以有相同的料号

ALTER TABLE base_material_156 DROP INDEX idx_b156_mcode;
ALTER TABLE base_material_156 ADD UNIQUE KEY idx_b156_mcode_company (material_code, company_id);

-- 验证
SHOW CREATE TABLE base_material_156;
