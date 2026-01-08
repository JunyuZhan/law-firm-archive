-- 045-add-finance-report-permissions.sql
-- 为财务人员添加报表中心权限（查看财务报表）
-- 执行日期: 2026-01-07

-- 财务人员（FINANCE，role_id=5）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 200, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 201, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 202, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 203, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 204, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (5, 205, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

