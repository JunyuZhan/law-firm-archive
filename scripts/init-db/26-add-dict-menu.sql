-- ============================================
-- 添加字典管理菜单
-- ============================================
-- 说明：在系统管理模块下添加字典管理菜单项
-- 执行时间：2026-01-10
-- ============================================

-- 插入字典管理菜单（系统管理下，ID=30，排序号=5，放在菜单管理之后）
INSERT INTO public.sys_menu (
    id, 
    parent_id, 
    name, 
    path, 
    component, 
    redirect, 
    icon, 
    menu_type, 
    permission, 
    sort_order, 
    visible, 
    status, 
    is_external, 
    is_cache, 
    created_at, 
    updated_at, 
    created_by, 
    updated_by, 
    deleted
) VALUES (
    30, 
    2, 
    '字典管理', 
    '/system/dict', 
    'system/dict/index', 
    NULL, 
    'BookOutlined', 
    'MENU', 
    'sys:dict:list', 
    5, 
    true, 
    'ENABLED', 
    false, 
    true, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP, 
    NULL, 
    NULL, 
    false
) ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    name = EXCLUDED.name,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    icon = EXCLUDED.icon,
    menu_type = EXCLUDED.menu_type,
    permission = EXCLUDED.permission,
    sort_order = EXCLUDED.sort_order,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

-- 为管理员角色分配字典管理菜单权限
-- 注意：role_id=1 是管理员角色（ADMIN），role_id=2 是律所主任（DIRECTOR）
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT r.id, 30, CURRENT_TIMESTAMP
FROM sys_role r
WHERE r.role_code IN ('ADMIN', 'DIRECTOR')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = 30
);

-- 更新菜单序列（如果需要）
SELECT setval('sys_menu_id_seq', GREATEST((SELECT MAX(id) FROM sys_menu), 30));

