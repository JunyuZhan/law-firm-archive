-- 047-add-approval-permissions-for-all-roles.sql
-- 为所有角色添加审批中心基础权限
-- 所有用户都可能发起审批（如费用报销、合同审批等），需要能查看自己发起的审批状态
-- 执行日期: 2026-01-08

-- 审批权限说明：
-- menu_id=130: approval:list (审批列表 - 查看审批)
-- menu_id=131: approval:approve (审批操作 - 执行审批)
-- 
-- 权限分配原则：
-- - 所有角色都需要 approval:list 权限（查看自己发起的审批状态）
-- - 只有管理层角色才需要 approval:approve 权限（执行审批操作）
-- - 后端通过数据权限过滤，确保用户只能看到自己相关的审批

-- FINANCE (role_id=5) - 财务人员需要处理费用报销审批
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) 
VALUES (5, 130, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) 
VALUES (5, 131, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- LAWYER (role_id=6) - 律师需要查看自己发起的合同审批状态
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) 
VALUES (6, 130, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- 律师不需要 approval:approve 权限（不参与审批）

-- ADMIN_STAFF (role_id=8) - 行政需要查看用印申请等审批状态
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) 
VALUES (8, 130, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- 行政不需要 approval:approve 权限（不参与业务审批）

-- TRAINEE (role_id=9) - 实习律师需要查看自己发起的审批状态
INSERT INTO public.sys_role_menu (role_id, menu_id, created_at) 
VALUES (9, 130, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- 实习律师不需要 approval:approve 权限

-- 验证权限分配
DO $$
DECLARE
    role_count INTEGER;
BEGIN
    SELECT COUNT(DISTINCT role_id) INTO role_count
    FROM public.sys_role_menu
    WHERE menu_id = 130;
    
    RAISE NOTICE '审批列表权限(menu_id=130)已分配给 % 个角色', role_count;
END $$;

