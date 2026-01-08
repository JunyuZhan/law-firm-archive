-- 修复行政管理模块权限问题
-- 后端API使用的权限码与菜单权限码不匹配，需要添加缺失的权限

-- 获取角色ID
-- ADMIN = 1, DIRECTOR = 2, PARTNER = 3, LAWYER = 4, ASSISTANT = 5, INTERN = 6, FINANCE = 7, HR = 8

-- 添加行政管理模块缺失的权限菜单项

-- 外出管理权限 (菜单权限是 admin:go-out:list，但API使用 admin:goout:view, admin:goout:register)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(168, 99, '外出查看', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:goout:view', 1, true, 'ENABLED', false, false, NOW(), NOW(), false),
(169, 99, '外出登记', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:goout:register', 2, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 会议记录权限 (菜单权限是 admin:meeting-record:list，但API使用 admin:meeting:view)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(170, 100, '会议记录查看', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:meeting:view', 1, true, 'ENABLED', false, false, NOW(), NOW(), false),
(171, 100, '会议记录管理', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:meeting:record', 2, true, 'ENABLED', false, false, NOW(), NOW(), false),
(172, 100, '会议通知', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:meeting:notice', 3, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 资产盘点权限 (菜单权限是 admin:asset-inventory:list，但API使用 admin:asset:inventory)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(173, 98, '资产盘点操作', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:inventory', 1, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 会议室预约权限 (菜单权限是 admin:meeting:list，但API使用 admin:meeting:manage)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(174, 93, '会议室管理', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:meeting:manage', 1, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 加班管理权限 (菜单权限是 admin:overtime:list，但API使用 admin:overtime:view, admin:overtime:apply, admin:overtime:approve)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(175, 97, '加班查看', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:overtime:view', 1, true, 'ENABLED', false, false, NOW(), NOW(), false),
(176, 97, '加班申请', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:overtime:apply', 2, true, 'ENABLED', false, false, NOW(), NOW(), false),
(177, 97, '加班审批', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:overtime:approve', 3, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 请假管理权限 (菜单权限是 admin:leave:list，但API使用 admin:leave:approve, admin:leave:manage)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(178, 92, '请假审批', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:leave:approve', 1, true, 'ENABLED', false, false, NOW(), NOW(), false),
(179, 92, '假期管理', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:leave:manage', 2, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 资产管理权限 (菜单权限是 admin:asset:list，但API使用 admin:asset:detail, admin:asset:create, admin:asset:edit, admin:asset:delete, admin:asset:receive, admin:asset:return, admin:asset:scrap)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(180, 94, '资产详情', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:detail', 1, true, 'ENABLED', false, false, NOW(), NOW(), false),
(181, 94, '资产创建', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:create', 2, true, 'ENABLED', false, false, NOW(), NOW(), false),
(182, 94, '资产编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:edit', 3, true, 'ENABLED', false, false, NOW(), NOW(), false),
(183, 94, '资产删除', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:delete', 4, true, 'ENABLED', false, false, NOW(), NOW(), false),
(184, 94, '资产领用', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:receive', 5, true, 'ENABLED', false, false, NOW(), NOW(), false),
(185, 94, '资产归还', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:return', 6, true, 'ENABLED', false, false, NOW(), NOW(), false),
(186, 94, '资产报废', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:scrap', 7, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 采购管理权限 (菜单权限是 admin:purchase:list，但API使用更多细化权限)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(187, 95, '采购详情', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:detail', 1, true, 'ENABLED', false, false, NOW(), NOW(), false),
(188, 95, '采购创建', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:create', 2, true, 'ENABLED', false, false, NOW(), NOW(), false),
(189, 95, '采购编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:edit', 3, true, 'ENABLED', false, false, NOW(), NOW(), false),
(190, 95, '采购审批', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:approve', 4, true, 'ENABLED', false, false, NOW(), NOW(), false),
(191, 95, '采购入库', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:receive', 5, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 供应商管理权限 (菜单权限是 admin:supplier:list，但API使用更多细化权限)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(192, 96, '供应商详情', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:supplier:detail', 1, true, 'ENABLED', false, false, NOW(), NOW(), false),
(193, 96, '供应商创建', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:supplier:create', 2, true, 'ENABLED', false, false, NOW(), NOW(), false),
(194, 96, '供应商编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:supplier:edit', 3, true, 'ENABLED', false, false, NOW(), NOW(), false),
(195, 96, '供应商删除', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:supplier:delete', 4, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 合同查询导出权限
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(196, 121, '合同导出', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:contract:export', 1, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 考勤管理权限
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
(197, 91, '考勤记录', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:attendance:record', 1, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- 更新序列
SELECT setval('sys_menu_id_seq', 200, true);

-- 为各角色分配行政管理权限
-- ADMIN(1) - 所有权限
-- DIRECTOR(2) - 所有权限
-- PARTNER(3) - 大部分权限
-- LAWYER(4) - 基本权限
-- ASSISTANT(5) - 基本权限
-- INTERN(6) - 查看权限
-- FINANCE(7) - 采购、资产相关
-- HR(8) - 考勤、请假、加班相关

-- 删除可能存在的旧权限关联，避免重复
DELETE FROM sys_role_menu WHERE menu_id IN (168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197);

-- ADMIN(1) - 全部权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM sys_menu WHERE id IN (168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197);

-- DIRECTOR(2) - 全部权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 2, id, NOW() FROM sys_menu WHERE id IN (168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197);

-- PARTNER(3) - 大部分权限(排除管理和删除)
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 3, id, NOW() FROM sys_menu WHERE id IN (168, 169, 170, 171, 172, 173, 175, 176, 177, 178, 180, 181, 182, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 196);

-- LAWYER(4) - 基本操作权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 4, id, NOW() FROM sys_menu WHERE id IN (168, 169, 170, 175, 176, 180, 184, 185, 187, 188, 192);

-- ASSISTANT(5) - 基本操作权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 5, id, NOW() FROM sys_menu WHERE id IN (168, 169, 170, 175, 176, 180, 184, 185, 187, 188, 192);

-- INTERN(6) - 只读权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 6, id, NOW() FROM sys_menu WHERE id IN (168, 170, 175, 180, 187, 192);

-- FINANCE(7) - 财务相关权限(采购、资产)
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 7, id, NOW() FROM sys_menu WHERE id IN (168, 170, 173, 175, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196);

-- HR(8) - 人事相关权限(考勤、请假、加班)
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 8, id, NOW() FROM sys_menu WHERE id IN (168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 197);

-- 更新角色菜单关联序列
SELECT setval('sys_role_menu_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_role_menu), true);


