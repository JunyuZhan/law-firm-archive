-- 043-add-team-leader-business-permissions.sql
-- 为团队负责人（TEAM_LEADER，role_id=3）添加业务模块权限
-- 团队负责人是纯业务角色，应拥有全部业务模块权限，数据范围限于本团队
-- 执行日期: 2026-01-07

-- 文书管理模块（不含63印章管理，64用印申请已有）
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 6, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 61, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 62, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 档案管理模块
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 8, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 81, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 82, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 83, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 知识库模块
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 11, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 111, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 112, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 113, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 报表中心模块
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 200, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 201, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 202, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 203, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 204, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) VALUES (3, 205, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 验证结果
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 3;

