-- =====================================================
-- 报表权限细化
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-11
-- 描述: 将报表权限按业务类型细分，区分财务报表和业务报表
-- =====================================================

-- 添加新的报表权限菜单
-- menu_id 使用 750-759 范围避免冲突
INSERT INTO public.sys_menu VALUES (750, 200, '财务报表查看', NULL, NULL, NULL, NULL, 'BUTTON', 'report:finance:view', 10, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (751, 200, '业务报表查看', NULL, NULL, NULL, NULL, 'BUTTON', 'report:matter:view', 11, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;

-- 为管理员角色(1)分配所有报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 750, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 751, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为主任角色(2)分配所有报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 750, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 751, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为团队负责人角色(3)分配业务报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 3, 751, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为财务角色(5)分配财务报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 5, 750, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为律师角色(6)分配业务报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 6, 751, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 说明：
-- report:finance:view - 财务相关报表（收入统计、律师业绩排行）
--   - 管理员、主任、财务 可查看
-- report:matter:view - 业务相关报表（项目统计、客户统计）
--   - 管理员、主任、团队负责人、律师 可查看

