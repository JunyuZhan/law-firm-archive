-- 权限冲突修复脚本
-- 2026-01-07

-- =============================================
-- 1. 移除团队负责人的人力资源权限
-- =============================================
DELETE FROM public.sys_role_menu WHERE role_id = 3 AND menu_id IN (
    10,   -- 人力资源目录
    101,  -- 培训管理
    102,  -- 绩效考核
    103,  -- 晋升管理
    104,  -- 发展计划
    105,  -- 转正管理
    106,  -- 离职管理
    109   -- 员工档案
);

-- =============================================
-- 2. 移除团队负责人的系统管理权限
-- =============================================
DELETE FROM public.sys_role_menu WHERE role_id = 3 AND menu_id IN (
    2,    -- 系统管理目录
    21,   -- 用户管理
    152, 153, 154, 155, 156  -- 期限管理相关按钮
);

-- =============================================
-- 3. 档案销毁权限只保留管理员
-- =============================================
-- 移除主任的档案销毁权限
DELETE FROM public.sys_role_menu WHERE role_id = 2 AND menu_id = 83;
-- 移除团队负责人的档案销毁权限
DELETE FROM public.sys_role_menu WHERE role_id = 3 AND menu_id = 83;
-- 移除律师的档案销毁权限
DELETE FROM public.sys_role_menu WHERE role_id = 6 AND menu_id = 83;
-- 移除行政的档案销毁权限
DELETE FROM public.sys_role_menu WHERE role_id = 8 AND menu_id = 83;
-- 移除实习律师的档案销毁权限
DELETE FROM public.sys_role_menu WHERE role_id = 9 AND menu_id = 83;
-- 移除财务的档案销毁权限（如果有）
DELETE FROM public.sys_role_menu WHERE role_id = 5 AND menu_id = 83;

-- =============================================
-- 4. 确保律师有费用报销权限
-- =============================================
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) 
VALUES (6, 59, NOW())
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 确保实习律师也有费用报销权限
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) 
VALUES (9, 59, NOW())
ON CONFLICT (role_id, menu_id) DO NOTHING;

