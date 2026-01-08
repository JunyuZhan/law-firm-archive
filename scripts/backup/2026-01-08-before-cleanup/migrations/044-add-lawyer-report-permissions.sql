-- 044-add-lawyer-report-permissions.sql
-- 为律师角色（LEAD_LAWYER、ASSOCIATE、TRAINEE）添加报表中心权限
-- 律师可以查看自己的业务统计（年度、月度同比环比数据）
-- 执行日期: 2026-01-07

-- 主办律师（LEAD_LAWYER，role_id=6）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 200, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 201, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 202, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 203, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 204, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (6, 205, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 协办律师（ASSOCIATE，role_id=7）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 200, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 201, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 202, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 203, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 204, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (7, 205, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 实习律师（TRAINEE，role_id=9）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 200, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 201, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 202, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 203, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 204, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (9, 205, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

