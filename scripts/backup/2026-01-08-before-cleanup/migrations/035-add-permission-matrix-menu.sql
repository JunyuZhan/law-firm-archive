-- 添加权限矩阵管理菜单
-- 创建时间: 2026-01-06

-- 插入权限矩阵管理菜单（放在角色管理之后，sort_order = 2.5，但PostgreSQL需要整数，所以设为3）
-- 需要将部门管理及之后的菜单sort_order都往后移1位
INSERT INTO sys_menu (
    id, parent_id, name, path, component, redirect, icon, menu_type, permission, 
    sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted
) VALUES (
    198, 2, '权限矩阵', '/system/permission-matrix', 'system/permission-matrix/index', 
    NULL, 'TableOutlined', 'MENU', 'sys:role:list', 3, true, 'ENABLED', false, true, 
    NOW(), NOW(), false
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    icon = EXCLUDED.icon,
    permission = EXCLUDED.permission,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

-- 将部门管理及之后的菜单sort_order都往后移1位
UPDATE sys_menu 
SET sort_order = sort_order + 1, updated_at = NOW()
WHERE parent_id = 2 AND sort_order >= 3 AND id != 198;

-- 更新序列
SELECT setval('sys_menu_id_seq', 200, true);

-- 为ADMIN角色分配权限矩阵菜单权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 1, 198, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 198
);

-- 注意：DIRECTOR角色不需要权限矩阵管理权限（只有ADMIN可以查看）

