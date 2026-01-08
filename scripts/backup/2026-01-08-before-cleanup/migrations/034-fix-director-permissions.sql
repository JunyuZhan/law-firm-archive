-- =====================================================
-- 修复律所主任(DIRECTOR)权限问题
-- 1. 添加系统管理目录权限（否则子菜单无法显示）
-- 2. 添加合同变更操作权限（同步变更、忽略变更）
-- =====================================================

-- 先删除可能存在的重复记录
DELETE FROM sys_role_menu WHERE role_id = 2 AND menu_id IN (2, 124, 125);

-- 为DIRECTOR角色(role_id=2)添加缺失的权限
-- 1. 系统管理目录 (menu_id=2) - 使子菜单可以正确显示
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 2, NOW());

-- 2. 合同变更操作权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 124, NOW());  -- 同步变更 (finance:contract:amendment:sync)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 125, NOW());  -- 忽略变更 (finance:contract:amendment:ignore)

-- 验证：查询DIRECTOR角色权限数量
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2;
-- 应该是 130 个菜单权限

