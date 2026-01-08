-- 系统管理模块权限修复脚本
-- 2026-01-07
-- 
-- 设计说明：
-- - 管理员：全部权限
-- - 主任：部门管理、出函模板、合同模板、提成规则配置
-- - 财务：合同模板、提成规则配置
-- - 行政：出函模板、合同模板
-- - 团队负责人、律师、实习律师：无权限

-- 1. 清理主任的多余权限
DELETE FROM sys_role_menu WHERE role_id = 2 AND menu_id IN (21, 26, 152, 153, 154, 155, 156);

-- 2. 清理律师的全部系统管理权限
DELETE FROM sys_role_menu WHERE role_id = 6 AND menu_id IN (2, 21, 152, 153);

-- 3. 清理实习律师的全部系统管理权限
DELETE FROM sys_role_menu WHERE role_id = 9 AND menu_id IN (2, 21, 152, 153);

-- 4. 清理团队负责人的全部系统管理权限
DELETE FROM sys_role_menu WHERE role_id = 3 AND menu_id IN (2, 21, 22, 23, 24, 25, 26, 27, 28, 140, 152, 153, 154, 155, 156, 198);

-- 5. 调整行政权限：移除用户管理，添加合同模板
DELETE FROM sys_role_menu WHERE role_id = 8 AND menu_id = 21;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 28, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 6. 添加财务的系统管理权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES 
(5, 2, NOW()),   -- 系统管理目录
(5, 28, NOW()),  -- 合同模板
(5, 140, NOW())  -- 提成规则配置
ON CONFLICT (role_id, menu_id) DO NOTHING;

