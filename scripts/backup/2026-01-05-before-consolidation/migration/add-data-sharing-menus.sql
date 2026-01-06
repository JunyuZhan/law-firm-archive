-- 数据共享增强功能菜单
-- 添加行政合同查询菜单和收款变更审批菜单

-- 1. 添加行政合同查询菜单到行政管理下 (parent_id = 9)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
SELECT 120, 9, '合同查询', '/admin/contract', 'admin/contract/index', 'admin:contract:list', 'FileSearchOutlined', 'MENU', 11, TRUE, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/contract');

-- 2. 添加收款变更审批菜单到财务管理下 (parent_id = 5)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status)
SELECT 56, 5, '收款变更审批', '/finance/payment-amendment', 'finance/payment-amendment/index', 'fee:amendment:list', 'AuditOutlined', 'MENU', 6, TRUE, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/finance/payment-amendment');

-- 3. 为管理员角色分配新菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 120
FROM sys_role r
WHERE r.role_code = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = r.id AND menu_id = 120);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 56
FROM sys_role r
WHERE r.role_code = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = r.id AND menu_id = 56);

-- 4. 为行政人员分配合同查询菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 120
FROM sys_role r
WHERE r.role_code = 'ADMIN_STAFF'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = r.id AND menu_id = 120);

-- 5. 为财务主管分配收款变更审批菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 56
FROM sys_role r
WHERE r.role_code = 'FINANCE_MANAGER'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = r.id AND menu_id = 56);

-- 6. 为律所主任分配新菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 120
FROM sys_role r
WHERE r.role_code = 'DIRECTOR'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = r.id AND menu_id = 120);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 56
FROM sys_role r
WHERE r.role_code = 'DIRECTOR'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = r.id AND menu_id = 56);

-- 7. 为合伙人分配新菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 120
FROM sys_role r
WHERE r.role_code = 'PARTNER'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = r.id AND menu_id = 120);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 56
FROM sys_role r
WHERE r.role_code = 'PARTNER'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = r.id AND menu_id = 56);

-- 验证插入结果
SELECT id, parent_id, name, path, permission, visible, status 
FROM sys_menu 
WHERE id IN (120, 56)
ORDER BY id;
