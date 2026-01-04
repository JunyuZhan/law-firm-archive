-- ============================================
-- 添加合同管理菜单 - 数据库迁移脚本
-- 执行日期：2026-01-XX
-- ============================================

-- 1. 添加合同管理菜单到项目管理下（如果不存在）
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
VALUES (45, 4, '合同管理', '/matter/contract', 'matter/contract/index', 'matter:contract:list', 'FileProtectOutlined', 'MENU', 3, TRUE, 'ENABLED')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    permission = EXCLUDED.permission,
    icon = EXCLUDED.icon,
    menu_type = EXCLUDED.menu_type,
    sort_order = EXCLUDED.sort_order,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status;

-- 2. 更新财务模块合同菜单（改为合同查看）
UPDATE sys_menu 
SET 
    name = '合同查看',
    permission = 'contract:view',
    visible = TRUE,
    status = 'ENABLED'
WHERE id = 51 AND path = '/finance/contract';

-- 3. 确保所有菜单的 visible 和 status 字段已设置
UPDATE sys_menu 
SET 
    visible = COALESCE(visible, TRUE),
    status = COALESCE(status, 'ENABLED')
WHERE visible IS NULL OR status IS NULL;

-- 4. 为管理员角色分配合同管理菜单权限（如果还没有）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 45
FROM sys_role r
WHERE r.role_code = 'ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = 45
);

-- 5. 为律所主任分配合同管理菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 45
FROM sys_role r
WHERE r.role_code = 'DIRECTOR'
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = 45
);

-- 6. 为合伙人分配合同管理菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 45
FROM sys_role r
WHERE r.role_code = 'PARTNER'
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = 45
);

-- 7. 为主办律师分配合同管理菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 45
FROM sys_role r
WHERE r.role_code = 'LEAD_LAWYER'
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = 45
);

-- 8. 为财务主管分配合同查看菜单权限（如果还没有）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 51
FROM sys_role r
WHERE r.role_code IN ('FINANCE_MANAGER', 'FINANCE')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = 51
);

-- ============================================
-- 说明：
-- 1. 添加合同管理菜单（ID: 45）到项目管理下
-- 2. 更新财务模块合同菜单为"合同查看"
-- 3. 确保所有菜单的 visible 和 status 字段已设置
-- 4. 为相关角色分配菜单权限
-- ============================================

