-- =====================================================
-- 修复超级管理员(ADMIN)权限：补充缺失的菜单权限
-- 确保ADMIN角色拥有系统所有菜单和按钮权限
-- =====================================================

-- 缺失的权限：
-- 107 - 工资管理 (payroll:list)
-- 108 - 我的工资 (payroll:my:view)
-- 130 - 审批列表 (approval:list)
-- 131 - 审批操作 (approval:approve)
-- 132 - 合同审批 (contract:approve)

-- 先删除可能存在的重复记录
DELETE FROM sys_role_menu WHERE role_id = 1 AND menu_id IN (107, 108, 130, 131, 132);

-- 为ADMIN角色(role_id=1)添加缺失的菜单权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 107, NOW());  -- 工资管理
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 108, NOW());  -- 我的工资
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 130, NOW());  -- 审批列表
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 131, NOW());  -- 审批操作
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 132, NOW());  -- 合同审批

-- 验证：查询ADMIN角色权限数量
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1;
-- 应该等于 sys_menu 表中的总菜单数量

