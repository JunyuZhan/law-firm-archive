-- 050-add-my-documents-menu.sql
-- 新增"我的文书"导航菜单
-- 执行日期: 2026-01-08

-- 目标结构：
-- 卷宗管理 (id=6)
--   ├── 卷宗列表 (id=61) - 按项目查看卷宗
--   ├── 我的文书 (id=714) - 查看用户创建的所有文书（新增）
--   ├── 文书制作 (id=62) - 使用模板制作文书
--   └── 模板管理 (id=713) - 维护模板（仅管理层可见）

-- 1. 新增"我的文书"菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (714, 6, '我的文书', '/document/my', 'document/my/index', NULL, 'FileTextOutlined', 'MENU', 'doc:my:list', 2, true, 'ENABLED', false, true, NOW(), NOW(), false)
ON CONFLICT (id) DO UPDATE SET
    name = '我的文书',
    path = '/document/my',
    component = 'document/my/index',
    permission = 'doc:my:list',
    sort_order = 2,
    updated_at = NOW();

-- 2. 调整其他菜单排序
-- 文书制作排在我的文书后面
UPDATE sys_menu SET sort_order = 3 WHERE id = 62;
-- 模板管理排在最后
UPDATE sys_menu SET sort_order = 4 WHERE id = 713;

-- 3. 为所有业务角色添加"我的文书"菜单权限
-- ADMIN (role_id=1)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 714, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- DIRECTOR (role_id=2)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 714, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- TEAM_LEADER (role_id=3)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 714, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- FINANCE (role_id=5) - 财务人员也可能需要创建报销相关文书
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (5, 714, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- LAWYER (role_id=6)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 714, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- ADMIN_STAFF (role_id=8)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 714, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- TRAINEE (role_id=9)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 714, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 4. 验证结果
DO $$
DECLARE
    menu_record RECORD;
BEGIN
    RAISE NOTICE '=== 卷宗管理菜单结构 ===';
    FOR menu_record IN 
        SELECT m.id, m.name, m.path, m.permission, m.sort_order,
               (SELECT string_agg(r.role_name, ', ') FROM sys_role_menu rm JOIN sys_role r ON rm.role_id = r.id WHERE rm.menu_id = m.id) as roles
        FROM sys_menu m 
        WHERE m.parent_id = 6 OR m.id = 6
        ORDER BY CASE WHEN m.parent_id IS NULL OR m.id = 6 THEN 0 ELSE 1 END, m.sort_order
    LOOP
        RAISE NOTICE 'ID: %, 名称: %, 路径: %, 权限: %, 排序: %, 角色: %', 
            menu_record.id, menu_record.name, menu_record.path, menu_record.permission, menu_record.sort_order, menu_record.roles;
    END LOOP;
END $$;

