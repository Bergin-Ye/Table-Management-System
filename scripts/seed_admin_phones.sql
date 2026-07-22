-- ============================================
-- 手机验证码登录 — 初始化管理员手机号
-- 执行后旧密码登录失效，只能用短信验证码登录
-- ============================================

-- 1. 删除所有非管理员用户（之前的测试用户）
DELETE FROM sys_user WHERE role != 'admin';

-- 2. 更新管理员手机号（username = 手机号，password 清空）
--    已有 admin 账号改为手机号登录
UPDATE sys_user SET username = '18720647482', password = '' WHERE id = 1;

-- 3. 创建第二个管理员（如果不存在）
INSERT IGNORE INTO sys_user (username, password, real_name, role)
VALUES ('15919958627', '', '管理员2', 'admin');
