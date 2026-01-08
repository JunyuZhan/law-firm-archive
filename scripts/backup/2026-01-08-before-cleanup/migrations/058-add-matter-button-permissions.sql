-- 项目详情页按钮权限
-- 2026-01-08
--
-- 权限设计：
-- - 管理员、主任、团队负责人：全部操作权限
-- - 律师：查看、创建、编辑、申请结案（无审批权）
-- - 实习律师：查看、编辑（无创建、结案、审批权）

-- 1. 添加项目管理按钮权限到 sys_menu 表
-- 父菜单ID: 41 (项目列表)

-- 项目查看（详情、报告、时间线）
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (220, 41, '项目查看', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:view', 1, true, 'ENABLED', false, true, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 项目创建
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (221, 41, '项目创建', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:create', 2, true, 'ENABLED', false, true, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 项目编辑（含修改状态、管理成员）
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (222, 41, '项目编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:update', 3, true, 'ENABLED', false, true, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 申请结案
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (223, 41, '申请结案', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:close', 4, true, 'ENABLED', false, true, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 审批结案
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (224, 41, '审批结案', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:approve', 5, true, 'ENABLED', false, true, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 2. 为各角色分配按钮权限

-- 管理员 (role_id=1): 全部权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 220, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 221, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 222, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 223, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 224, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 主任 (role_id=2): 全部权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 220, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 221, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 222, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 223, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 224, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 团队负责人 (role_id=3): 全部权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 220, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 221, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 222, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 223, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 224, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 律师 (role_id=6): 查看、创建、编辑、申请结案（无审批权）
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 220, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 221, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 222, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 223, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- 律师无 matter:approve 权限

-- 实习律师 (role_id=9): 查看、编辑（无创建、结案、审批权）
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 220, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- 实习律师无 matter:create 权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 222, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- 实习律师无 matter:close 权限
-- 实习律师无 matter:approve 权限


