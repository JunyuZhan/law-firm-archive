-- 048-add-document-template-permissions.sql
-- 添加文书模板的按钮级权限
-- 执行日期: 2026-01-08

-- 文书模板菜单ID: 62, parent_id: 6 (文书管理)

-- 1. 添加按钮级权限菜单
-- 查看详情按钮
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (708, 62, '查看详情', '', '', NULL, '', 'BUTTON', 'doc:template:detail', 1, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 管理模板按钮（创建/更新/删除）
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (709, 62, '管理模板', '', '', NULL, '', 'BUTTON', 'doc:template:manage', 2, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 使用模板按钮
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (710, 62, '使用模板', '', '', NULL, '', 'BUTTON', 'doc:template:use', 3, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 生成文档按钮
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (711, 62, '生成文档', '', '', NULL, '', 'BUTTON', 'doc:template:generate', 4, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 预览模板按钮
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (712, 62, '预览模板', '', '', NULL, '', 'BUTTON', 'doc:template:view', 5, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 2. 分配权限给角色
-- 角色ID: 1=ADMIN, 2=DIRECTOR, 3=TEAM_LEADER, 5=FINANCE, 6=LAWYER, 8=ADMIN_STAFF, 9=TRAINEE

-- ADMIN (role_id=1): 全部权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 708, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 709, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- DIRECTOR (role_id=2): 全部权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 708, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 709, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ADMIN_STAFF (role_id=8): 全部权限（行政负责模板管理）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 708, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 709, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- TEAM_LEADER (role_id=3): 查看+使用+生成+预览（无管理权限）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 708, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- LAWYER (role_id=6): 查看+使用+生成+预览（无管理权限）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 708, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- TRAINEE (role_id=9): 查看+使用+生成+预览（无管理权限）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 708, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 注意：FINANCE (role_id=5) 没有文书模板菜单权限，所以不需要添加按钮权限

-- 3. 验证结果
DO $$
DECLARE
    menu_count INT;
    perm_count INT;
BEGIN
    SELECT COUNT(*) INTO menu_count FROM sys_menu WHERE permission LIKE 'doc:template:%';
    RAISE NOTICE '文书模板相关权限菜单数量: %', menu_count;
    
    SELECT COUNT(*) INTO perm_count FROM sys_role_menu rm 
    JOIN sys_menu m ON rm.menu_id = m.id 
    WHERE m.permission LIKE 'doc:template:%';
    RAISE NOTICE '文书模板权限分配记录数量: %', perm_count;
END $$;

