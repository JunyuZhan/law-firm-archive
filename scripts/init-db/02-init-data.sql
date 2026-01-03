-- ============================================
-- 智慧律所管理系统 - 初始化数据
-- 版本: 1.0.0
-- ============================================

-- ============================================
-- 一、初始化角色
-- ============================================

INSERT INTO sys_role (role_code, role_name, description, data_scope, sort_order) VALUES
('ADMIN', '超级管理员', '系统最高权限，可管理所有功能和数据', 'ALL', 1),
('DIRECTOR', '律所主任', '律所管理层，可查看全所数据，审批重要事项', 'ALL', 2),
('PARTNER', '合伙人', '律所合伙人，可查看部门数据，参与重大决策', 'DEPT_AND_CHILD', 3),
('FINANCE_MANAGER', '财务主管', '财务管理负责人，管理全所财务数据', 'ALL', 4),
('FINANCE', '财务人员', '财务操作人员，处理日常财务工作', 'DEPT', 5),
('LEAD_LAWYER', '主办律师', '案件主办人，管理自己主办的项目', 'SELF', 6),
('ASSOCIATE', '协办律师', '案件协办人，参与项目协作', 'SELF', 7),
('ADMIN_STAFF', '行政人员', '行政后勤人员，管理行政事务', 'DEPT', 8),
('TRAINEE', '实习律师', '实习人员，有限权限', 'SELF', 9);

-- ============================================
-- 二、初始化部门
-- ============================================

INSERT INTO sys_department (name, parent_id, sort_order) VALUES
('律师事务所', 0, 1),
('诉讼业务部', 1, 1),
('非诉业务部', 1, 2),
('知识产权部', 1, 3),
('财务部', 1, 4),
('行政部', 1, 5);

-- ============================================
-- 三、初始化管理员用户
-- 密码: admin123 (BCrypt加密)
-- ============================================

INSERT INTO sys_user (username, password, real_name, email, phone, department_id, position, compensation_type, status) VALUES
('admin', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '系统管理员', 'admin@lawfirm.com', '13800138000', 1, '系统管理员', 'SALARIED', 'ACTIVE');

-- 分配管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'admin' AND r.role_code = 'ADMIN';

-- ============================================
-- 四、初始化菜单权限
-- ============================================

-- 使用固定ID插入一级菜单，确保子菜单parent_id正确
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, menu_type, sort_order) VALUES
(1, 0, '工作台', '/dashboard', 'dashboard/index/index', 'DashboardOutlined', 'MENU', 1),
(2, 0, '系统管理', '/system', 'LAYOUT', 'SettingOutlined', 'DIRECTORY', 2),
(3, 0, '客户管理', '/crm', 'LAYOUT', 'TeamOutlined', 'DIRECTORY', 3),
(4, 0, '项目管理', '/matter', 'LAYOUT', 'FolderOutlined', 'DIRECTORY', 4),
(5, 0, '财务管理', '/finance', 'LAYOUT', 'MoneyCollectOutlined', 'DIRECTORY', 5),
(6, 0, '文书管理', '/document', 'LAYOUT', 'FileTextOutlined', 'DIRECTORY', 6),
(7, 0, '证据管理', '/evidence', 'LAYOUT', 'AuditOutlined', 'DIRECTORY', 7),
(8, 0, '档案管理', '/archive', 'LAYOUT', 'DatabaseOutlined', 'DIRECTORY', 8),
(9, 0, '行政管理', '/admin', 'LAYOUT', 'BankOutlined', 'DIRECTORY', 9),
(10, 0, '人力资源', '/hr', 'LAYOUT', 'IdcardOutlined', 'DIRECTORY', 10),
(11, 0, '知识库', '/knowledge', 'LAYOUT', 'BookOutlined', 'DIRECTORY', 11);

-- 系统管理子菜单 (parent_id = 2)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(21, 2, '用户管理', '/system/user', 'system/user/index', 'sys:user:list', 'UserOutlined', 'MENU', 1),
(22, 2, '角色管理', '/system/role', 'system/role/index', 'sys:role:list', 'SafetyCertificateOutlined', 'MENU', 2),
(23, 2, '部门管理', '/system/dept', 'system/dept/index', 'sys:dept:list', 'ApartmentOutlined', 'MENU', 3),
(24, 2, '菜单管理', '/system/menu', 'system/menu/index', 'sys:menu:list', 'MenuOutlined', 'MENU', 4),
(25, 2, '系统配置', '/system/config', 'system/config/index', 'sys:config:list', 'ToolOutlined', 'MENU', 5),
(26, 2, '操作日志', '/system/log', 'system/log/index', 'sys:log:list', 'FileSearchOutlined', 'MENU', 6);

-- 客户管理子菜单 (parent_id = 3)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(31, 3, '客户列表', '/crm/client', 'crm/client/index', 'client:list', 'ContactsOutlined', 'MENU', 1),
(32, 3, '利冲审查', '/crm/conflict', 'crm/conflict/index', 'conflict:apply', 'SecurityScanOutlined', 'MENU', 2),
(33, 3, '案源管理', '/crm/lead', 'crm/lead/index', 'lead:list', 'FunnelPlotOutlined', 'MENU', 3);

-- 项目管理子菜单 (parent_id = 4)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(41, 4, '项目列表', '/matter/list', 'matter/list/index', 'matter:list', 'ProjectOutlined', 'MENU', 1),
(42, 4, '我的项目', '/matter/my', 'matter/my/index', 'matter:list', 'SolutionOutlined', 'MENU', 2),
(43, 4, '工时管理', '/matter/timesheet', 'matter/timesheet/index', 'timesheet:record', 'ClockCircleOutlined', 'MENU', 3),
(44, 4, '任务管理', '/matter/task', 'matter/task/index', 'task:manage', 'CheckSquareOutlined', 'MENU', 4);

-- 财务管理子菜单 (parent_id = 5)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(51, 5, '收费合同', '/finance/contract', 'finance/contract/index', 'finance:contract:list', 'FileProtectOutlined', 'MENU', 1),
(52, 5, '收款管理', '/finance/payment', 'finance/payment/index', 'finance:payment:list', 'PayCircleOutlined', 'MENU', 2),
(53, 5, '提成管理', '/finance/commission', 'finance/commission/index', 'finance:commission:list', 'PercentageOutlined', 'MENU', 3),
(54, 5, '发票管理', '/finance/invoice', 'finance/invoice/index', 'finance:invoice:apply', 'ProfileOutlined', 'MENU', 4),
(55, 5, '财务报表', '/finance/report', 'finance/report/index', 'finance:report:view', 'BarChartOutlined', 'MENU', 5);

-- 文书管理子菜单 (parent_id = 6)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(61, 6, '文书列表', '/document/list', 'document/list/index', 'doc:list', 'FileOutlined', 'MENU', 1),
(62, 6, '文书模板', '/document/template', 'document/template/index', 'doc:template:list', 'FileAddOutlined', 'MENU', 2),
(63, 6, '印章管理', '/document/seal', 'document/seal/index', 'doc:seal:list', 'SafetyOutlined', 'MENU', 3),
(64, 6, '用印申请', '/document/seal-apply', 'document/seal-apply/index', 'doc:seal:apply', 'FormOutlined', 'MENU', 4);

-- 证据管理子菜单 (parent_id = 7)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(71, 7, '证据列表', '/evidence/list', 'evidence/list/index', 'evidence:list', 'FileSearchOutlined', 'MENU', 1),
(72, 7, '证据入库', '/evidence/create', 'evidence/create/index', 'evidence:create', 'PlusCircleOutlined', 'MENU', 2);

-- 档案管理子菜单 (parent_id = 8)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(81, 8, '档案列表', '/archive/list', 'archive/list/index', 'archive:list', 'FolderOpenOutlined', 'MENU', 1),
(82, 8, '档案借阅', '/archive/borrow', 'archive/borrow/index', 'archive:borrow', 'ExportOutlined', 'MENU', 2),
(83, 8, '档案销毁', '/archive/destroy', 'archive/destroy/index', 'archive:destroy', 'DeleteOutlined', 'MENU', 3);

-- 行政管理子菜单 (parent_id = 9)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(91, 9, '考勤管理', '/admin/attendance', 'admin/attendance/index', 'admin:attendance:list', 'ClockCircleOutlined', 'MENU', 1),
(92, 9, '请假管理', '/admin/leave', 'admin/leave/index', 'admin:leave:list', 'CalendarOutlined', 'MENU', 2),
(93, 9, '会议室预约', '/admin/meeting-room', 'admin/meeting-room/index', 'admin:meeting:list', 'ScheduleOutlined', 'MENU', 3),
(94, 9, '资产管理', '/admin/asset', 'admin/asset/index', 'admin:asset:list', 'ShopOutlined', 'MENU', 4),
(95, 9, '采购管理', '/admin/purchase', 'admin/purchase/index', 'admin:purchase:list', 'ShoppingCartOutlined', 'MENU', 5);

-- 人力资源子菜单 (parent_id = 10)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(101, 10, '培训管理', '/hr/training', 'hr/training/index', 'hr:training:list', 'ReadOutlined', 'MENU', 1),
(102, 10, '绩效考核', '/hr/performance', 'hr/performance/index', 'hr:performance:list', 'TrophyOutlined', 'MENU', 2),
(103, 10, '晋升管理', '/hr/promotion', 'hr/promotion/index', 'hr:promotion:list', 'RiseOutlined', 'MENU', 3),
(104, 10, '发展计划', '/hr/development', 'hr/development/index', 'hr:development:list', 'AimOutlined', 'MENU', 4);

-- 知识库子菜单 (parent_id = 11)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(111, 11, '知识文章', '/knowledge/article', 'knowledge/article/index', 'knowledge:article:list', 'FileTextOutlined', 'MENU', 1),
(112, 11, '案例库', '/knowledge/case', 'knowledge/case/index', 'knowledge:case:list', 'ContainerOutlined', 'MENU', 2),
(113, 11, '法规库', '/knowledge/law', 'knowledge/law/index', 'knowledge:law:list', 'BookOutlined', 'MENU', 3);

-- 重置序列，确保后续插入ID正确
SELECT setval('sys_menu_id_seq', (SELECT MAX(id) FROM sys_menu));

-- ============================================
-- 五、初始化提成规则
-- ============================================

INSERT INTO finance_commission_rule (rule_code, rule_name, firm_retention_rate, originator_rate, tax_rate, management_fee_rate, rate_tiers, is_default, active) VALUES
('DEFAULT', '默认提成规则', 0.30, 0.20, 0.0672, 0.15, 
'[{"minAmount": 0, "maxAmount": 100000, "rate": 0.30},
  {"minAmount": 100000, "maxAmount": 500000, "rate": 0.35},
  {"minAmount": 500000, "maxAmount": 1000000, "rate": 0.40},
  {"minAmount": 1000000, "maxAmount": null, "rate": 0.45}]'::jsonb,
TRUE, TRUE);

-- ============================================
-- 六、角色权限分配（管理员拥有所有权限）
-- ============================================

-- 为管理员角色分配所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m WHERE r.role_code = 'ADMIN';

-- 为律所主任分配核心权限（系统管理、客户管理、项目管理、财务管理及其子菜单）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m 
WHERE r.role_code = 'DIRECTOR' 
AND (m.parent_id = 0 OR m.parent_id IN (2, 3, 4, 5));

-- 为合伙人分配业务权限（客户管理、项目管理、财务管理及其子菜单）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m 
WHERE r.role_code = 'PARTNER' 
AND (m.id IN (1, 3, 4, 5) OR m.parent_id IN (3, 4, 5));

-- 为财务主管分配财务相关权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m 
WHERE r.role_code = 'FINANCE_MANAGER' 
AND (m.id IN (1, 5) OR m.parent_id = 5);

-- 为主办律师分配业务权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m 
WHERE r.role_code = 'LEAD_LAWYER' 
AND (m.id IN (1, 3, 4, 6, 7, 8, 11) OR m.parent_id IN (3, 4, 6, 7, 8, 11));

-- 为行政人员分配行政权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m 
WHERE r.role_code = 'ADMIN_STAFF' 
AND (m.id IN (1, 9) OR m.parent_id = 9);

COMMIT;

