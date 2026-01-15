-- =====================================================
-- 审批权限细化
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-11
-- 描述: 将审批权限按业务类型细分
-- =====================================================

-- 添加按业务类型细分的审批权限
-- menu_id 使用 740-749 范围避免冲突
-- parent_id = 149（审批中心菜单）

INSERT INTO public.sys_menu VALUES (740, 149, '合同审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:contract:approve', 10, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (741, 149, '用印审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:seal:approve', 11, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (742, 149, '利冲审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:conflict:approve', 12, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (743, 149, '费用审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:expense:approve', 13, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (744, 149, '结案审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:matter-close:approve', 14, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;

-- 为管理员角色(1)分配所有审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 740, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 741, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 742, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 743, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 744, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为主任角色(2)分配所有审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 740, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 741, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 742, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 743, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 744, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为团队负责人角色(3)分配团队相关审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 3, 740, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 合同审批
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 3, 742, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 利冲审批
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 3, 744, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 结案审批

-- 为财务角色(5)分配财务相关审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 5, 743, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 费用审批

-- 为行政角色(8)分配行政相关审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 8, 741, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 用印审批

-- 说明：
-- approval:contract:approve - 合同审批权限
-- approval:seal:approve - 用印申请审批权限
-- approval:conflict:approve - 利冲检查审批权限
-- approval:expense:approve - 费用报销审批权限
-- approval:matter-close:approve - 项目结案审批权限
--
-- 注意：审批权限是「能否审批」的权限，不是「能否发起」的权限
-- 发起审批的权限由各业务模块自己的权限控制
--
-- 角色权限分配逻辑：
-- - 管理员、主任：所有审批权限
-- - 团队负责人：合同、利冲、结案（与业务相关）
-- - 财务：费用审批
-- - 行政：用印审批

