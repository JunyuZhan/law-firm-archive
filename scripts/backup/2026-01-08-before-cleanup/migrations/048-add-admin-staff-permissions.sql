-- 048-add-admin-staff-permissions.sql
-- 为行政添加缺失的核心权限
-- 执行日期: 2026-01-07

-- 出函模板
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 27, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 人力资源模块（101-106）
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 10, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 101, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 102, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 103, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 104, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 105, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 106, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 审批中心
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 701, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 130, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 131, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 资产操作按钮（180-186）
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 180, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 181, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 182, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 183, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 184, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 185, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 186, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 采购操作按钮（187-191）
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 187, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 188, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 189, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 190, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 191, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 供应商操作按钮（192-195）
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 192, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 193, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 194, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 195, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

