-- =============================================
-- 052-fix-admin-module-permissions.sql
-- 修复行政管理模块权限问题
-- 1. 给所有角色分配员工自助功能菜单权限
-- 2. 给律师角色分配出函管理菜单权限
-- 3. 统一权限码命名
-- =============================================

-- 角色ID对照：
-- 1=ADMIN(管理员), 2=DIRECTOR(主任), 3=TEAM_LEADER(团队负责人)
-- 5=FINANCE(财务), 6=LAWYER(律师), 8=ADMIN_STAFF(行政), 9=TRAINEE(实习律师)

-- =============================================
-- 1. 员工自助功能 - 给所有业务角色分配菜单权限
-- =============================================

-- 1.1 考勤管理 (menu_id=91) - 所有员工都需要签到签退和查看自己考勤
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 91, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 团队负责人
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (5, 91, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 财务
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 91, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 律师
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 91, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 实习律师

-- 1.2 请假管理 (menu_id=92) - 所有员工都需要申请请假
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 92, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 团队负责人
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (5, 92, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 财务
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 92, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 律师
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 92, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 实习律师

-- 1.3 会议室预约 (menu_id=93) - 所有员工都需要预约会议室
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 93, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 团队负责人
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (5, 93, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 财务
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 93, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 律师
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 93, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 实习律师

-- 1.4 加班管理 (menu_id=97) - 所有员工都需要申请加班
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 97, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 团队负责人
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 97, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 律师
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 97, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 实习律师

-- 1.5 外出管理 (menu_id=99) - 所有员工都需要登记外出
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 99, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 团队负责人
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 99, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 律师
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 99, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 实习律师

-- =============================================
-- 2. 出函管理 - 给律师和实习律师分配菜单权限
-- =============================================

-- 出函管理 (menu_id=120) - 律师是主要使用者
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 120, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 律师
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 120, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;  -- 实习律师

-- =============================================
-- 3. 统一权限码命名
-- =============================================

-- 将菜单权限码 admin:go-out:list 改为 admin:goout:list
UPDATE sys_menu SET permission = 'admin:goout:list' WHERE id = 99 AND permission = 'admin:go-out:list';

-- =============================================
-- 4. 添加按钮级权限菜单
-- =============================================

-- 4.1 加班管理按钮权限
INSERT INTO sys_menu (parent_id, name, path, component, permission, menu_type, sort_order, visible, status, created_at, updated_at, deleted)
SELECT 97, '申请加班', '', '', 'admin:overtime:apply', 'BUTTON', 1, true, 'ENABLED', NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'admin:overtime:apply' AND menu_type = 'BUTTON');

INSERT INTO sys_menu (parent_id, name, path, component, permission, menu_type, sort_order, visible, status, created_at, updated_at, deleted)
SELECT 97, '审批加班', '', '', 'admin:overtime:approve', 'BUTTON', 2, true, 'ENABLED', NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'admin:overtime:approve' AND menu_type = 'BUTTON');

-- 4.2 外出管理按钮权限
INSERT INTO sys_menu (parent_id, name, path, component, permission, menu_type, sort_order, visible, status, created_at, updated_at, deleted)
SELECT 99, '外出登记', '', '', 'admin:goout:register', 'BUTTON', 1, true, 'ENABLED', NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'admin:goout:register' AND menu_type = 'BUTTON');

-- 4.3 请假管理按钮权限
INSERT INTO sys_menu (parent_id, name, path, component, permission, menu_type, sort_order, visible, status, created_at, updated_at, deleted)
SELECT 92, '审批请假', '', '', 'admin:leave:approve', 'BUTTON', 1, true, 'ENABLED', NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'admin:leave:approve' AND menu_type = 'BUTTON');

-- 4.4 出函管理按钮权限
INSERT INTO sys_menu (parent_id, name, path, component, permission, menu_type, sort_order, visible, status, created_at, updated_at, deleted)
SELECT 120, '出函审批', '', '', 'admin:letter:approve', 'BUTTON', 1, true, 'ENABLED', NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'admin:letter:approve' AND menu_type = 'BUTTON');

INSERT INTO sys_menu (parent_id, name, path, component, permission, menu_type, sort_order, visible, status, created_at, updated_at, deleted)
SELECT 120, '出函打印', '', '', 'admin:letter:print', 'BUTTON', 2, true, 'ENABLED', NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'admin:letter:print' AND menu_type = 'BUTTON');

-- =============================================
-- 5. 分配按钮级权限给相应角色
-- =============================================

-- 获取新增按钮权限的ID并分配
DO $$
DECLARE
    v_overtime_apply_id BIGINT;
    v_overtime_approve_id BIGINT;
    v_goout_register_id BIGINT;
    v_leave_approve_id BIGINT;
    v_letter_approve_id BIGINT;
    v_letter_print_id BIGINT;
BEGIN
    -- 获取按钮菜单ID
    SELECT id INTO v_overtime_apply_id FROM sys_menu WHERE permission = 'admin:overtime:apply' AND menu_type = 'BUTTON';
    SELECT id INTO v_overtime_approve_id FROM sys_menu WHERE permission = 'admin:overtime:approve' AND menu_type = 'BUTTON';
    SELECT id INTO v_goout_register_id FROM sys_menu WHERE permission = 'admin:goout:register' AND menu_type = 'BUTTON';
    SELECT id INTO v_leave_approve_id FROM sys_menu WHERE permission = 'admin:leave:approve' AND menu_type = 'BUTTON';
    SELECT id INTO v_letter_approve_id FROM sys_menu WHERE permission = 'admin:letter:approve' AND menu_type = 'BUTTON';
    SELECT id INTO v_letter_print_id FROM sys_menu WHERE permission = 'admin:letter:print' AND menu_type = 'BUTTON';
    
    -- 加班申请权限 - 所有角色（除管理员外都需要申请）
    IF v_overtime_apply_id IS NOT NULL THEN
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, v_overtime_apply_id, NOW()) ON CONFLICT DO NOTHING;  -- 主任
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, v_overtime_apply_id, NOW()) ON CONFLICT DO NOTHING;  -- 团队负责人
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (5, v_overtime_apply_id, NOW()) ON CONFLICT DO NOTHING;  -- 财务
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, v_overtime_apply_id, NOW()) ON CONFLICT DO NOTHING;  -- 律师
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, v_overtime_apply_id, NOW()) ON CONFLICT DO NOTHING;  -- 行政
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, v_overtime_apply_id, NOW()) ON CONFLICT DO NOTHING;  -- 实习律师
    END IF;
    
    -- 加班审批权限 - 管理员、主任、团队负责人
    IF v_overtime_approve_id IS NOT NULL THEN
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, v_overtime_approve_id, NOW()) ON CONFLICT DO NOTHING;  -- 管理员
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, v_overtime_approve_id, NOW()) ON CONFLICT DO NOTHING;  -- 主任
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, v_overtime_approve_id, NOW()) ON CONFLICT DO NOTHING;  -- 团队负责人
    END IF;
    
    -- 外出登记权限 - 所有角色
    IF v_goout_register_id IS NOT NULL THEN
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, v_goout_register_id, NOW()) ON CONFLICT DO NOTHING;  -- 管理员
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, v_goout_register_id, NOW()) ON CONFLICT DO NOTHING;  -- 主任
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, v_goout_register_id, NOW()) ON CONFLICT DO NOTHING;  -- 团队负责人
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (5, v_goout_register_id, NOW()) ON CONFLICT DO NOTHING;  -- 财务
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, v_goout_register_id, NOW()) ON CONFLICT DO NOTHING;  -- 律师
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, v_goout_register_id, NOW()) ON CONFLICT DO NOTHING;  -- 行政
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, v_goout_register_id, NOW()) ON CONFLICT DO NOTHING;  -- 实习律师
    END IF;
    
    -- 请假审批权限 - 管理员、主任、团队负责人
    IF v_leave_approve_id IS NOT NULL THEN
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, v_leave_approve_id, NOW()) ON CONFLICT DO NOTHING;  -- 管理员
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, v_leave_approve_id, NOW()) ON CONFLICT DO NOTHING;  -- 主任
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, v_leave_approve_id, NOW()) ON CONFLICT DO NOTHING;  -- 团队负责人
    END IF;
    
    -- 出函审批权限 - 管理员、主任、团队负责人
    IF v_letter_approve_id IS NOT NULL THEN
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, v_letter_approve_id, NOW()) ON CONFLICT DO NOTHING;  -- 管理员
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, v_letter_approve_id, NOW()) ON CONFLICT DO NOTHING;  -- 主任
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, v_letter_approve_id, NOW()) ON CONFLICT DO NOTHING;  -- 团队负责人
    END IF;
    
    -- 出函打印权限 - 管理员、主任、行政
    IF v_letter_print_id IS NOT NULL THEN
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, v_letter_print_id, NOW()) ON CONFLICT DO NOTHING;  -- 管理员
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, v_letter_print_id, NOW()) ON CONFLICT DO NOTHING;  -- 主任
        INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, v_letter_print_id, NOW()) ON CONFLICT DO NOTHING;  -- 行政
    END IF;
END $$;

-- =============================================
-- 验证结果
-- =============================================
-- SELECT m.name, m.permission, string_agg(r.role_name, ', ' ORDER BY r.id) AS roles
-- FROM sys_menu m
-- LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id
-- LEFT JOIN sys_role r ON rm.role_id = r.id
-- WHERE m.parent_id = 9 OR m.id = 9
-- GROUP BY m.id, m.name, m.permission, m.sort_order
-- ORDER BY m.sort_order;

