-- =====================================================
-- 添加新菜单项
-- 用于在现有数据库中添加新创建的页面菜单
-- =====================================================

-- 检查并添加行政管理新菜单
-- 供应商管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
SELECT 96, 9, '供应商管理', '/admin/supplier', 'admin/supplier/index', 'admin:supplier:list', 'ShopOutlined', 'MENU', 6, TRUE, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/supplier');

-- 加班管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
SELECT 97, 9, '加班管理', '/admin/overtime', 'admin/overtime/index', 'admin:overtime:list', 'ClockCircleOutlined', 'MENU', 7, TRUE, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/overtime');

-- 资产盘点
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
SELECT 98, 9, '资产盘点', '/admin/asset-inventory', 'admin/asset-inventory/index', 'admin:asset-inventory:list', 'FileSearchOutlined', 'MENU', 8, TRUE, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/asset-inventory');

-- 外出管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
SELECT 99, 9, '外出管理', '/admin/go-out', 'admin/go-out/index', 'admin:go-out:list', 'ExportOutlined', 'MENU', 9, TRUE, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/go-out');

-- 会议记录
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
SELECT 100, 9, '会议记录', '/admin/meeting-record', 'admin/meeting-record/index', 'admin:meeting-record:list', 'FileTextOutlined', 'MENU', 10, TRUE, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/meeting-record');

-- 出函管理（已融合到02-init-data.sql，ID=120）
-- INSERT INTO sys_menu ... 已移除

-- 检查并添加人力资源新菜单
-- 转正管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
SELECT 105, 10, '转正管理', '/hr/regularization', 'hr/regularization/index', 'hr:regularization:list', 'CheckSquareOutlined', 'MENU', 5, TRUE, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/hr/regularization');

-- 离职管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
SELECT 106, 10, '离职管理', '/hr/resignation', 'hr/resignation/index', 'hr:resignation:list', 'ExportOutlined', 'MENU', 6, TRUE, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/hr/resignation');

-- 为管理员角色分配新菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id 
FROM sys_role r, sys_menu m 
WHERE r.role_code = 'ADMIN' 
AND m.path IN (
    '/admin/supplier',
    '/admin/overtime',
    '/admin/asset-inventory',
    '/admin/go-out',
    '/admin/meeting-record',
    '/hr/regularization',
    '/hr/resignation'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

-- 为行政人员角色分配行政管理新菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id 
FROM sys_role r, sys_menu m 
WHERE r.role_code = 'ADMIN_STAFF' 
AND m.path IN (
    '/admin/supplier',
    '/admin/overtime',
    '/admin/asset-inventory',
    '/admin/go-out',
    '/admin/meeting-record'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

-- 为律所主任和合伙人分配人力资源新菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id 
FROM sys_role r, sys_menu m 
WHERE r.role_code IN ('DIRECTOR', 'PARTNER')
AND m.path IN (
    '/hr/regularization',
    '/hr/resignation'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

-- 重置序列，确保后续插入ID正确
SELECT setval('sys_menu_id_seq', (SELECT MAX(id) FROM sys_menu));

COMMIT;

