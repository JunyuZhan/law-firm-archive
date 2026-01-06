-- 添加日程管理和期限提醒菜单及权限
-- Author: System
-- Date: 2026-01-06

-- 1. 添加日程管理菜单（作为工作台子菜单）
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (149, 0, '日程管理', '/workbench/schedule', 'workbench/schedule/index', NULL, 'CalendarOutlined', 'MENU', 'schedule:list', 2, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

-- 2. 添加日程管理权限按钮
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (150, 149, '日程查看', NULL, NULL, NULL, NULL, 'BUTTON', 'schedule:view', 1, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (151, 149, '日程管理', NULL, NULL, NULL, NULL, 'BUTTON', 'schedule:manage', 2, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

-- 3. 添加期限提醒权限按钮（作为项目管理的子权限）
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (152, 2, '期限列表', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:list', 10, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (153, 2, '期限查看', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:view', 11, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (154, 2, '期限创建', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:create', 12, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (155, 2, '期限编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:edit', 13, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (156, 2, '期限删除', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:delete', 14, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

-- 4. 给所有角色分配日程管理权限
-- ADMIN (1)
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 149, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 150, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 151, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 152, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 153, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 154, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 155, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 156, NOW()) ON CONFLICT DO NOTHING;

-- DIRECTOR (2)
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 149, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 150, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 151, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 152, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 153, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 154, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 155, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 156, NOW()) ON CONFLICT DO NOTHING;

-- PARTNER (3)
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 149, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 150, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 151, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 152, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 153, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 154, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 155, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 156, NOW()) ON CONFLICT DO NOTHING;

-- LAWYER (4)
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 149, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 150, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 151, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 152, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 153, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 154, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 155, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 156, NOW()) ON CONFLICT DO NOTHING;

-- PARALEGAL (5)
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 149, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 150, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 151, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 152, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 153, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 154, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 155, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 156, NOW()) ON CONFLICT DO NOTHING;

-- INTERN (6)
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 149, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 150, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 152, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 153, NOW()) ON CONFLICT DO NOTHING;

-- FINANCE (7)
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 149, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 150, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 151, NOW()) ON CONFLICT DO NOTHING;

-- HR (8)
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 149, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 150, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 151, NOW()) ON CONFLICT DO NOTHING;

-- 5. 更新序列值
SELECT pg_catalog.setval('public.sys_menu_id_seq', 160, true);

