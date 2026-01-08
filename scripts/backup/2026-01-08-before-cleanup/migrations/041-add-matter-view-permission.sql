-- 添加 matter:view 权限
-- 创建日期: 2026-01-07
-- 说明: /timeline 和 /close/report 接口需要 matter:view 权限，但该权限未在数据库中定义
-- 解决方案: 创建 matter:view 权限菜单项，并分配给所有有 matter:list 权限的角色

-- 1. 创建 matter:view 权限菜单项（BUTTON类型，挂载在项目列表菜单下）
INSERT INTO sys_menu (
    id, parent_id, name, path, component, redirect, icon, menu_type, permission, 
    sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted
) VALUES (
    217, 41, '查看项目详情', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:view', 
    1, true, 'ENABLED', false, false, NOW(), NOW(), false
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    permission = EXCLUDED.permission,
    updated_at = NOW();

-- 2. 为所有有 matter:list 权限的角色分配 matter:view 权限
-- 这些角色包括：ADMIN(1), DIRECTOR(2), PARTNER(3), FINANCE_MANAGER(4), FINANCE(5), 
--              LEAD_LAWYER(6), ASSOCIATE(7), ADMIN_STAFF(8), TRAINEE(9)
-- 使用 INSERT ... ON CONFLICT 避免重复插入

INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT role_id, 217, NOW()
FROM (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9)) AS roles(role_id)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = roles.role_id AND rm.menu_id = 217
)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 更新序列
SELECT setval('sys_menu_id_seq', 217, true);

