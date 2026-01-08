-- 049-refactor-document-template-menu.sql
-- 重构文书模板菜单：分离"文书制作"和"模板管理"
-- 执行日期: 2026-01-08

-- 目标结构：
-- 卷宗管理 (id=6)
--   ├── 卷宗列表 (id=61) - 查看文书列表
--   ├── 文书制作 (id=62) - 使用模板制作文书（重命名）
--   └── 模板管理 (id=713) - 维护模板（新增，仅管理层可见）

-- 1. 重命名"文书模板"为"文书制作"
UPDATE sys_menu SET 
    name = '文书制作',
    path = '/document/compose',
    component = 'document/compose/index',
    permission = 'doc:compose',
    updated_at = NOW()
WHERE id = 62;

-- 2. 保持"卷宗列表"名称不变（注释掉）
-- UPDATE sys_menu SET 
--     name = '卷宗列表',
--     updated_at = NOW()
-- WHERE id = 61;

-- 3. 新增"模板管理"菜单（仅管理员/主任/行政可见）
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (713, 6, '模板管理', '/document/template', 'document/template/index', NULL, 'SettingOutlined', 'MENU', 'doc:template:manage', 3, true, 'ENABLED', false, true, NOW(), NOW(), false)
ON CONFLICT (id) DO UPDATE SET
    name = '模板管理',
    path = '/document/template',
    component = 'document/template/index',
    permission = 'doc:template:manage',
    sort_order = 3,
    updated_at = NOW();

-- 4. 更新按钮权限的parent_id（原来挂在62下，现在需要调整）
-- 文书制作相关按钮（挂在id=62下）
-- 生成文档按钮
UPDATE sys_menu SET parent_id = 62 WHERE id = 711 AND permission = 'doc:template:generate';
-- 预览模板按钮（制作时预览）
UPDATE sys_menu SET parent_id = 62 WHERE id = 712 AND permission = 'doc:template:view';
-- 使用模板按钮
UPDATE sys_menu SET parent_id = 62 WHERE id = 710 AND permission = 'doc:template:use';

-- 模板管理相关按钮（挂在id=713下）
-- 查看详情
UPDATE sys_menu SET parent_id = 713 WHERE id = 708 AND permission = 'doc:template:detail';
-- 管理模板（创建/编辑/删除）
UPDATE sys_menu SET parent_id = 713 WHERE id = 709 AND permission = 'doc:template:manage';

-- 5. 调整权限分配

-- 5.1 删除原有"文书模板"(id=62)的角色权限，重新分配"文书制作"权限
DELETE FROM sys_role_menu WHERE menu_id = 62;

-- 5.2 为"文书制作"(id=62)添加权限：所有业务人员可用
-- ADMIN (role_id=1)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 62, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- DIRECTOR (role_id=2)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 62, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- TEAM_LEADER (role_id=3)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 62, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- LAWYER (role_id=6)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 62, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- ADMIN_STAFF (role_id=8)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 62, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
-- TRAINEE (role_id=9)
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 62, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 5.3 为"模板管理"(id=713)添加权限：仅管理员/主任/行政
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 713, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 713, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 713, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 5.4 调整按钮权限分配
-- 文书制作相关按钮：所有业务人员
-- 生成文档(711)、预览(712)、使用模板(710)
DELETE FROM sys_role_menu WHERE menu_id IN (710, 711, 712);
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (3, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (6, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 710, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 711, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (9, 712, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 模板管理相关按钮：仅管理员/主任/行政
-- 查看详情(708)、管理模板(709)
DELETE FROM sys_role_menu WHERE menu_id IN (708, 709);
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 708, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 709, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 708, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 709, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 708, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (8, 709, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 6. 验证结果
DO $$
DECLARE
    menu_record RECORD;
BEGIN
    RAISE NOTICE '=== 菜单结构 ===';
    FOR menu_record IN 
        SELECT m.id, m.name, m.path, m.permission, 
               (SELECT string_agg(r.role_name, ', ') FROM sys_role_menu rm JOIN sys_role r ON rm.role_id = r.id WHERE rm.menu_id = m.id) as roles
        FROM sys_menu m 
        WHERE m.parent_id = 6 OR m.id = 6
        ORDER BY m.id
    LOOP
        RAISE NOTICE 'ID: %, 名称: %, 路径: %, 权限: %, 角色: %', 
            menu_record.id, menu_record.name, menu_record.path, menu_record.permission, menu_record.roles;
    END LOOP;
END $$;

