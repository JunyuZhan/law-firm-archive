-- 修复任务管理和工时管理权限
-- Author: System
-- Date: 2026-01-06
-- 问题：菜单权限码与API所需权限码不匹配

-- 1. 添加任务管理相关权限按钮
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (161, 44, '任务列表', NULL, NULL, NULL, NULL, 'BUTTON', 'task:list', 1, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (162, 44, '任务查看', NULL, NULL, NULL, NULL, 'BUTTON', 'task:view', 2, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

-- 2. 添加工时管理相关权限按钮
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (163, 43, '工时列表', NULL, NULL, NULL, NULL, 'BUTTON', 'timesheet:list', 1, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (164, 43, '工时查看', NULL, NULL, NULL, NULL, 'BUTTON', 'timesheet:view', 3, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) 
VALUES (165, 43, '工时审批', NULL, NULL, NULL, NULL, 'BUTTON', 'timesheet:approve', 4, true, 'ENABLED', false, true, NOW(), NOW(), NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

-- 3. 给所有需要的角色分配这些权限

-- ADMIN (1) - 完整权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 161, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 162, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 163, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 164, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (1, 165, NOW()) ON CONFLICT DO NOTHING;

-- DIRECTOR (2) - 完整权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 161, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 162, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 163, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 164, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (2, 165, NOW()) ON CONFLICT DO NOTHING;

-- PARTNER (3) - 完整权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 161, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 162, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 163, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 164, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 165, NOW()) ON CONFLICT DO NOTHING;

-- LAWYER (4) - 完整权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 161, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 162, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 163, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 164, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (4, 165, NOW()) ON CONFLICT DO NOTHING;

-- PARALEGAL (5) - 完整权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 161, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 162, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 163, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 164, NOW()) ON CONFLICT DO NOTHING;
-- PARALEGAL 不需要工时审批权限

-- INTERN (6) - 基础权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 161, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 162, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 163, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 164, NOW()) ON CONFLICT DO NOTHING;
-- INTERN 不需要工时审批权限

-- FINANCE (7) - 工时相关权限（财务需要查看工时统计）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 163, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 164, NOW()) ON CONFLICT DO NOTHING;

-- HR (8) - 任务和工时权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 161, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 162, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 163, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 164, NOW()) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (8, 165, NOW()) ON CONFLICT DO NOTHING;

-- 4. 更新序列值
SELECT pg_catalog.setval('public.sys_menu_id_seq', 170, true);

