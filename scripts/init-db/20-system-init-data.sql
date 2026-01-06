--
-- PostgreSQL database dump
--

\restrict zfSazfVoubL5nP9VwQfOeF5z0FOroDaYwwqhnzTxLkf1NH65LR5iS3qunPbP2V8

-- Dumped from database version 15.15
-- Dumped by pg_dump version 15.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: sys_department; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_department (id, name, parent_id, sort_order, leader_id, status, created_at, updated_at, created_by, updated_by, deleted) VALUES (1, '律师事务所', 0, 1, NULL, 'ACTIVE', '2026-01-04 16:14:15.893589', '2026-01-04 16:14:15.893589', NULL, NULL, false);
INSERT INTO public.sys_department (id, name, parent_id, sort_order, leader_id, status, created_at, updated_at, created_by, updated_by, deleted) VALUES (2, '诉讼业务部', 1, 1, NULL, 'ACTIVE', '2026-01-04 16:14:15.893589', '2026-01-04 16:14:15.893589', NULL, NULL, false);
INSERT INTO public.sys_department (id, name, parent_id, sort_order, leader_id, status, created_at, updated_at, created_by, updated_by, deleted) VALUES (3, '非诉业务部', 1, 2, NULL, 'ACTIVE', '2026-01-04 16:14:15.893589', '2026-01-04 16:14:15.893589', NULL, NULL, false);
INSERT INTO public.sys_department (id, name, parent_id, sort_order, leader_id, status, created_at, updated_at, created_by, updated_by, deleted) VALUES (4, '知识产权部', 1, 3, NULL, 'ACTIVE', '2026-01-04 16:14:15.893589', '2026-01-04 16:14:15.893589', NULL, NULL, false);
INSERT INTO public.sys_department (id, name, parent_id, sort_order, leader_id, status, created_at, updated_at, created_by, updated_by, deleted) VALUES (5, '财务部', 1, 4, NULL, 'ACTIVE', '2026-01-04 16:14:15.893589', '2026-01-04 16:14:15.893589', NULL, NULL, false);
INSERT INTO public.sys_department (id, name, parent_id, sort_order, leader_id, status, created_at, updated_at, created_by, updated_by, deleted) VALUES (6, '行政部', 1, 5, NULL, 'ACTIVE', '2026-01-04 16:14:15.893589', '2026-01-04 16:14:15.893589', NULL, NULL, false);


--
-- Data for Name: sys_menu; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (1, 0, '工作台', '/dashboard/workspace', 'dashboard/workspace/index', NULL, 'DashboardOutlined', 'MENU', NULL, 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (2, 0, '系统管理', '/system', 'LAYOUT', NULL, 'SettingOutlined', 'DIRECTORY', NULL, 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (3, 0, '客户管理', '/crm', 'LAYOUT', NULL, 'TeamOutlined', 'DIRECTORY', NULL, 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (4, 0, '项目管理', '/matter', 'LAYOUT', NULL, 'FolderOutlined', 'DIRECTORY', NULL, 4, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (5, 0, '财务管理', '/finance', 'LAYOUT', NULL, 'MoneyCollectOutlined', 'DIRECTORY', NULL, 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (6, 0, '文书管理', '/document', 'LAYOUT', NULL, 'FileTextOutlined', 'DIRECTORY', NULL, 6, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (8, 0, '档案管理', '/archive', 'LAYOUT', NULL, 'DatabaseOutlined', 'DIRECTORY', NULL, 7, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (9, 0, '行政管理', '/admin', 'LAYOUT', NULL, 'BankOutlined', 'DIRECTORY', NULL, 8, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (10, 0, '人力资源', '/hr', 'LAYOUT', NULL, 'IdcardOutlined', 'DIRECTORY', NULL, 9, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (11, 0, '知识库', '/knowledge', 'LAYOUT', NULL, 'BookOutlined', 'DIRECTORY', NULL, 10, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (21, 2, '用户管理', '/system/user', 'system/user/index', NULL, 'UserOutlined', 'MENU', 'sys:user:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (22, 2, '角色管理', '/system/role', 'system/role/index', NULL, 'SafetyCertificateOutlined', 'MENU', 'sys:role:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (23, 2, '部门管理', '/system/dept', 'system/dept/index', NULL, 'ApartmentOutlined', 'MENU', 'sys:dept:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (24, 2, '菜单管理', '/system/menu', 'system/menu/index', NULL, 'MenuOutlined', 'MENU', 'sys:menu:list', 4, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (25, 2, '系统配置', '/system/config', 'system/config/index', NULL, 'ToolOutlined', 'MENU', 'sys:config:list', 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (26, 2, '操作日志', '/system/log', 'system/log/index', NULL, 'FileSearchOutlined', 'MENU', 'sys:log:list', 6, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (27, 2, '出函模板', '/system/letter-template', 'system/letter-template/index', NULL, 'MailOutlined', 'MENU', 'sys:letter-template:list', 7, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (28, 2, '合同模板', '/system/contract-template', 'system/contract-template/index', NULL, 'FileProtectOutlined', 'MENU', 'sys:contract-template:list', 8, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (31, 3, '客户列表', '/crm/client', 'crm/client/index', NULL, 'ContactsOutlined', 'MENU', 'client:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896673', '2026-01-04 16:14:15.896673', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (32, 3, '利冲审查', '/crm/conflict', 'crm/conflict/index', NULL, 'SecurityScanOutlined', 'MENU', 'conflict:apply', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896673', '2026-01-04 16:14:15.896673', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (33, 3, '案源管理', '/crm/lead', 'crm/lead/index', NULL, 'FunnelPlotOutlined', 'MENU', 'lead:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896673', '2026-01-04 16:14:15.896673', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (41, 4, '项目列表', '/matter/list', 'matter/list/index', NULL, 'ProjectOutlined', 'MENU', 'matter:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (42, 4, '我的项目', '/matter/my', 'matter/my/index', NULL, 'SolutionOutlined', 'MENU', 'matter:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (45, 4, '合同管理', '/matter/contract', 'matter/contract/index', NULL, 'FileProtectOutlined', 'MENU', 'matter:contract:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (148, 4, '我的合同', '/matter/my-contract', 'matter/my-contract/index', NULL, 'FileTextOutlined', 'MENU', 'matter:contract:view', 4, true, 'ENABLED', false, true, '2026-01-06 09:30:00', '2026-01-06 09:30:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (43, 4, '工时管理', '/matter/timesheet', 'matter/timesheet/index', NULL, 'ClockCircleOutlined', 'MENU', 'timesheet:record', 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (44, 4, '任务管理', '/matter/task', 'matter/task/index', NULL, 'CheckSquareOutlined', 'MENU', 'task:manage', 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
-- 律师可见的财务菜单（排序靠前）
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (57, 5, '我的收款', '/finance/my-payment', 'finance/my-payment/index', NULL, 'WalletOutlined', 'MENU', 'finance:my:payment', 1, true, 'ENABLED', false, true, '2026-01-05 16:00:00', '2026-01-05 16:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (58, 5, '我的提成', '/finance/my-commission', 'finance/my-commission/index', NULL, 'DollarOutlined', 'MENU', 'finance:my:commission', 2, true, 'ENABLED', false, true, '2026-01-05 16:00:00', '2026-01-05 16:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (59, 5, '费用报销', '/finance/expense', 'finance/expense/index', NULL, 'AccountBookOutlined', 'MENU', 'finance:expense:apply', 3, true, 'ENABLED', false, true, '2026-01-05 16:00:00', '2026-01-05 16:00:00', NULL, NULL, false);
-- 财务人员可见的菜单（排序靠后）
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (51, 5, '合同收款概览', '/finance/contract', 'finance/contract/index', NULL, 'FileProtectOutlined', 'MENU', 'finance:contract:view', 10, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (52, 5, '收款管理', '/finance/payment', 'finance/payment/index', NULL, 'PayCircleOutlined', 'MENU', 'finance:payment:manage', 11, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (53, 5, '提成管理', '/finance/commission', 'finance/commission/index', NULL, 'PercentageOutlined', 'MENU', 'finance:commission:manage', 12, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (54, 5, '发票管理', '/finance/invoice', 'finance/invoice/index', NULL, 'ProfileOutlined', 'MENU', 'finance:invoice:manage', 13, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (55, 5, '财务报表', '/finance/report', 'finance/report/index', NULL, 'BarChartOutlined', 'MENU', 'finance:report:view', 14, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (61, 6, '文书列表', '/document/list', 'document/list/index', NULL, 'FileOutlined', 'MENU', 'doc:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89735', '2026-01-04 16:14:15.89735', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (62, 6, '文书模板', '/document/template', 'document/template/index', NULL, 'FileAddOutlined', 'MENU', 'doc:template:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89735', '2026-01-04 16:14:15.89735', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (63, 6, '印章管理', '/document/seal', 'document/seal/index', NULL, 'SafetyOutlined', 'MENU', 'doc:seal:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89735', '2026-01-04 16:14:15.89735', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (64, 6, '用印申请', '/document/seal-apply', 'document/seal-apply/index', NULL, 'FormOutlined', 'MENU', 'doc:seal:apply', 4, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89735', '2026-01-04 16:14:15.89735', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (81, 8, '档案列表', '/archive/list', 'archive/list/index', NULL, 'FolderOpenOutlined', 'MENU', 'archive:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89755', '2026-01-04 16:14:15.89755', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (82, 8, '档案借阅', '/archive/borrow', 'archive/borrow/index', NULL, 'ExportOutlined', 'MENU', 'archive:borrow', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89755', '2026-01-04 16:14:15.89755', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (83, 8, '档案销毁', '/archive/destroy', 'archive/destroy/index', NULL, 'DeleteOutlined', 'MENU', 'archive:destroy', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89755', '2026-01-04 16:14:15.89755', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (91, 9, '考勤管理', '/admin/attendance', 'admin/attendance/index', NULL, 'ClockCircleOutlined', 'MENU', 'admin:attendance:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (92, 9, '请假管理', '/admin/leave', 'admin/leave/index', NULL, 'CalendarOutlined', 'MENU', 'admin:leave:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (93, 9, '会议室预约', '/admin/meeting-room', 'admin/meeting-room/index', NULL, 'ScheduleOutlined', 'MENU', 'admin:meeting:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (94, 9, '资产管理', '/admin/asset', 'admin/asset/index', NULL, 'ShopOutlined', 'MENU', 'admin:asset:list', 4, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (95, 9, '采购管理', '/admin/purchase', 'admin/purchase/index', NULL, 'ShoppingCartOutlined', 'MENU', 'admin:purchase:list', 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (96, 9, '供应商管理', '/admin/supplier', 'admin/supplier/index', NULL, 'ShopOutlined', 'MENU', 'admin:supplier:list', 6, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (97, 9, '加班管理', '/admin/overtime', 'admin/overtime/index', NULL, 'ClockCircleOutlined', 'MENU', 'admin:overtime:list', 7, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (98, 9, '资产盘点', '/admin/asset-inventory', 'admin/asset-inventory/index', NULL, 'FileSearchOutlined', 'MENU', 'admin:asset-inventory:list', 8, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (99, 9, '外出管理', '/admin/go-out', 'admin/go-out/index', NULL, 'ExportOutlined', 'MENU', 'admin:go-out:list', 9, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (100, 9, '会议记录', '/admin/meeting-record', 'admin/meeting-record/index', NULL, 'FileTextOutlined', 'MENU', 'admin:meeting-record:list', 10, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (120, 9, '出函管理', '/admin/letter', 'admin/letter/index', NULL, 'MailOutlined', 'MENU', 'admin:letter:list', 11, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (101, 10, '培训管理', '/hr/training', 'hr/training/index', NULL, 'ReadOutlined', 'MENU', 'hr:training:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (102, 10, '绩效考核', '/hr/performance', 'hr/performance/index', NULL, 'TrophyOutlined', 'MENU', 'hr:performance:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (103, 10, '晋升管理', '/hr/promotion', 'hr/promotion/index', NULL, 'RiseOutlined', 'MENU', 'hr:promotion:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (104, 10, '发展计划', '/hr/development', 'hr/development/index', NULL, 'AimOutlined', 'MENU', 'hr:development:list', 4, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (105, 10, '转正管理', '/hr/regularization', 'hr/regularization/index', NULL, 'CheckSquareOutlined', 'MENU', 'hr:regularization:list', 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (106, 10, '离职管理', '/hr/resignation', 'hr/resignation/index', NULL, 'ExportOutlined', 'MENU', 'hr:resignation:list', 6, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (111, 11, '知识文章', '/knowledge/article', 'knowledge/article/index', NULL, 'FileTextOutlined', 'MENU', 'knowledge:article:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898559', '2026-01-04 16:14:15.898559', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (112, 11, '案例库', '/knowledge/case', 'knowledge/case/index', NULL, 'ContainerOutlined', 'MENU', 'knowledge:case:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898559', '2026-01-04 16:14:15.898559', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (113, 11, '法规库', '/knowledge/law', 'knowledge/law/index', NULL, 'BookOutlined', 'MENU', 'knowledge:law:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898559', '2026-01-04 16:14:15.898559', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (56, 5, '收款变更审批', '/finance/payment-amendment', 'finance/payment-amendment/index', NULL, 'AuditOutlined', 'MENU', 'fee:amendment:list', 6, true, 'ENABLED', false, true, '2026-01-05 02:19:31.876174', '2026-01-05 02:19:31.876174', NULL, NULL, false);
-- 审批中心权限菜单
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (130, 1, '审批列表', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:list', 1, true, 'ENABLED', false, true, '2026-01-05 10:00:00', '2026-01-05 10:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (131, 1, '审批操作', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:approve', 2, true, 'ENABLED', false, true, '2026-01-05 10:00:00', '2026-01-05 10:00:00', NULL, NULL, false);
-- 合同审批权限
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (132, 45, '合同审批', NULL, NULL, NULL, NULL, 'BUTTON', 'contract:approve', 1, true, 'ENABLED', false, true, '2026-01-05 10:00:00', '2026-01-05 10:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (121, 9, '合同查询', '/admin/contract', 'admin/contract/index', NULL, 'FileSearchOutlined', 'MENU', 'admin:contract:list', 12, true, 'ENABLED', false, true, '2026-01-05 02:19:57.79315', '2026-01-05 02:19:57.79315', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (123, 5, '合同变更处理', '/finance/contract-amendment', '/finance/contract-amendment/index', NULL, 'ant-design:diff-outlined', 'MENU', 'finance:contract:amendment:view', 60, true, 'ENABLED', false, true, '2026-01-05 03:55:33.410601', '2026-01-05 03:55:33.410601', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (124, 123, '同步变更', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:contract:amendment:sync', 1, true, 'ENABLED', false, true, '2026-01-05 03:55:33.410601', '2026-01-05 03:55:33.410601', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (125, 123, '忽略变更', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:contract:amendment:ignore', 2, true, 'ENABLED', false, true, '2026-01-05 03:55:33.410601', '2026-01-05 03:55:33.410601', NULL, NULL, false);
-- 提成规则配置（系统管理模块，仅admin/director）
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (140, 2, '提成规则配置', '/system/commission-config', 'system/commission-config/index', NULL, 'ant-design:percentage-outlined', 'MENU', 'system:config:list', 50, true, 'ENABLED', false, true, '2026-01-05 18:00:00', '2026-01-05 18:00:00', NULL, NULL, false);
-- 提成查看和操作权限（财务角色可用）
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (145, 53, '查看提成', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:commission:view', 5, true, 'ENABLED', false, true, '2026-01-05 18:00:00', '2026-01-05 18:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (146, 53, '提成审批', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:commission:approve', 6, true, 'ENABLED', false, true, '2026-01-05 18:00:00', '2026-01-05 18:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (147, 53, '提成发放', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:commission:issue', 7, true, 'ENABLED', false, true, '2026-01-05 18:00:00', '2026-01-05 18:00:00', NULL, NULL, false);

-- 日程管理菜单和权限
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (149, 0, '日程管理', '/workbench/schedule', 'workbench/schedule/index', NULL, 'CalendarOutlined', 'MENU', 'schedule:list', 2, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (150, 149, '日程查看', NULL, NULL, NULL, NULL, 'BUTTON', 'schedule:view', 1, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (151, 149, '日程管理', NULL, NULL, NULL, NULL, 'BUTTON', 'schedule:manage', 2, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);

-- 期限提醒权限
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (152, 2, '期限列表', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:list', 10, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (153, 2, '期限查看', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:view', 11, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (154, 2, '期限创建', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:create', 12, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (155, 2, '期限编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:edit', 13, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (156, 2, '期限删除', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:delete', 14, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);

-- 任务管理权限
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (161, 44, '任务列表', NULL, NULL, NULL, NULL, 'BUTTON', 'task:list', 1, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (162, 44, '任务查看', NULL, NULL, NULL, NULL, 'BUTTON', 'task:view', 2, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);

-- 工时管理权限
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (163, 43, '工时列表', NULL, NULL, NULL, NULL, 'BUTTON', 'timesheet:list', 1, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (164, 43, '工时查看', NULL, NULL, NULL, NULL, 'BUTTON', 'timesheet:view', 3, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) VALUES (165, 43, '工时审批', NULL, NULL, NULL, NULL, 'BUTTON', 'timesheet:approve', 4, true, 'ENABLED', false, true, '2026-01-06 12:00:00', '2026-01-06 12:00:00', NULL, NULL, false);


--
-- Data for Name: sys_role; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (1, 'ADMIN', '超级管理员', '系统最高权限，可管理所有功能和数据', 'ALL', 'ACTIVE', 1, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (2, 'DIRECTOR', '律所主任', '律所管理层，可查看全所数据，审批重要事项', 'ALL', 'ACTIVE', 2, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (3, 'PARTNER', '合伙人', '律所合伙人，可查看部门数据，参与重大决策', 'DEPT_AND_CHILD', 'ACTIVE', 3, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (4, 'FINANCE_MANAGER', '财务主管', '财务管理负责人，管理全所财务数据', 'ALL', 'ACTIVE', 4, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (5, 'FINANCE', '财务人员', '财务操作人员，处理日常财务工作', 'DEPT', 'ACTIVE', 5, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (6, 'LEAD_LAWYER', '主办律师', '案件主办人，管理自己主办的项目', 'SELF', 'ACTIVE', 6, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (7, 'ASSOCIATE', '协办律师', '案件协办人，参与项目协作', 'SELF', 'ACTIVE', 7, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (8, 'ADMIN_STAFF', '行政人员', '行政后勤人员，管理行政事务', 'DEPT', 'ACTIVE', 8, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (9, 'TRAINEE', '实习律师', '实习人员，有限权限', 'SELF', 'ACTIVE', 9, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);


--
-- Data for Name: sys_role_menu; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (1, 1, 1, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (2, 1, 2, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (3, 1, 3, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (4, 1, 4, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (5, 1, 5, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (6, 1, 6, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (7, 1, 8, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (8, 1, 9, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (9, 1, 10, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (10, 1, 11, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (11, 1, 21, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (12, 1, 22, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (13, 1, 23, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (14, 1, 24, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (15, 1, 25, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (16, 1, 26, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (17, 1, 27, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (18, 1, 28, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (19, 1, 31, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (20, 1, 32, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (21, 1, 33, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (22, 1, 41, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (23, 1, 42, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (24, 1, 45, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (25, 1, 43, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (26, 1, 44, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (27, 1, 51, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (28, 1, 52, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (29, 1, 53, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (30, 1, 54, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (31, 1, 55, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (32, 1, 61, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (33, 1, 62, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (34, 1, 63, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (35, 1, 64, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (36, 1, 81, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (37, 1, 82, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (38, 1, 83, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (39, 1, 91, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (40, 1, 92, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (41, 1, 93, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (42, 1, 94, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (43, 1, 95, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (44, 1, 96, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (45, 1, 97, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (46, 1, 98, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (47, 1, 99, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (48, 1, 100, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (49, 1, 120, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (50, 1, 101, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (51, 1, 102, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (52, 1, 103, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (53, 1, 104, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (54, 1, 105, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (55, 1, 106, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (56, 1, 111, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (57, 1, 112, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (58, 1, 113, '2026-01-04 16:14:15.8992');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (59, 2, 1, '2026-01-04 16:14:15.900075');
-- DIRECTOR不需要系统管理目录，只需要查看用户和部门
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (61, 2, 3, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (62, 2, 4, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (63, 2, 5, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (64, 2, 6, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (65, 2, 8, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (66, 2, 9, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (67, 2, 10, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (68, 2, 11, '2026-01-04 16:14:15.900075');
-- DIRECTOR保留用户查看和部门查看（只读），移除角色管理(22)、菜单管理(24)、系统配置(25)
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (69, 2, 21, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (71, 2, 23, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (74, 2, 26, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (75, 2, 27, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (76, 2, 28, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (77, 2, 31, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (78, 2, 32, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (79, 2, 33, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (80, 2, 41, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (81, 2, 42, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (82, 2, 45, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (83, 2, 43, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (84, 2, 44, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (85, 2, 51, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (86, 2, 52, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (87, 2, 53, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (88, 2, 54, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (89, 2, 55, '2026-01-04 16:14:15.900075');
-- DIRECTOR需要文书管理
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (316, 2, 61, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (317, 2, 62, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (318, 2, 63, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (319, 2, 64, '2026-01-05 17:00:00');
-- DIRECTOR需要档案管理
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (320, 2, 81, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (321, 2, 82, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (322, 2, 83, '2026-01-05 17:00:00');
-- DIRECTOR需要行政管理全部
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (323, 2, 91, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (324, 2, 92, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (325, 2, 93, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (326, 2, 94, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (327, 2, 95, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (328, 2, 96, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (329, 2, 97, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (330, 2, 98, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (331, 2, 99, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (332, 2, 100, '2026-01-05 17:00:00');
-- DIRECTOR需要人力资源全部
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (333, 2, 101, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (334, 2, 102, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (335, 2, 103, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (336, 2, 104, '2026-01-05 17:00:00');
-- DIRECTOR需要知识库
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (337, 2, 111, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (338, 2, 112, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (339, 2, 113, '2026-01-05 17:00:00');
-- DIRECTOR需要合同变更处理
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (340, 2, 123, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (90, 3, 1, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (91, 3, 3, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (92, 3, 4, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (93, 3, 5, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (94, 3, 31, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (95, 3, 32, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (96, 3, 33, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (97, 3, 41, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (98, 3, 42, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (99, 3, 45, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (100, 3, 43, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (101, 3, 44, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (102, 3, 51, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (103, 3, 52, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (104, 3, 53, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (105, 3, 54, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (106, 3, 55, '2026-01-04 16:14:15.900502');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (107, 4, 1, '2026-01-04 16:14:15.900745');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (108, 4, 5, '2026-01-04 16:14:15.900745');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (109, 4, 51, '2026-01-04 16:14:15.900745');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (110, 4, 52, '2026-01-04 16:14:15.900745');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (111, 4, 53, '2026-01-04 16:14:15.900745');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (112, 4, 54, '2026-01-04 16:14:15.900745');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (113, 4, 55, '2026-01-04 16:14:15.900745');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (114, 6, 1, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (115, 6, 3, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (116, 6, 4, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (117, 6, 6, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (118, 6, 8, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (119, 6, 11, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (120, 6, 31, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (121, 6, 32, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (122, 6, 33, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (123, 6, 41, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (124, 6, 42, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (125, 6, 45, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (126, 6, 43, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (127, 6, 44, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (128, 6, 61, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (129, 6, 62, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (130, 6, 63, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (131, 6, 64, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (132, 6, 81, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (133, 6, 82, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (134, 6, 83, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (135, 6, 111, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (136, 6, 112, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (137, 6, 113, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (138, 8, 1, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (139, 8, 9, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (140, 8, 91, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (141, 8, 92, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (142, 8, 93, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (143, 8, 94, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (144, 8, 95, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (145, 8, 96, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (146, 8, 97, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (147, 8, 98, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (148, 8, 99, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (149, 8, 100, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (150, 8, 120, '2026-01-04 16:14:15.902804');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (151, 2, 105, '2026-01-04 16:14:18.541755');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (152, 2, 106, '2026-01-04 16:14:18.541755');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (153, 3, 105, '2026-01-04 16:14:18.541755');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (154, 3, 106, '2026-01-04 16:14:18.541755');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (155, 1, 56, '2026-01-05 02:19:31.879342');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (156, 4, 56, '2026-01-05 02:19:31.880239');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (157, 2, 120, '2026-01-05 02:19:31.880543');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (158, 2, 56, '2026-01-05 02:19:31.880861');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (159, 3, 120, '2026-01-05 02:19:31.881299');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (160, 3, 56, '2026-01-05 02:19:31.881604');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (161, 1, 121, '2026-01-05 02:19:57.796406');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (162, 8, 121, '2026-01-05 02:19:57.797797');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (163, 2, 121, '2026-01-05 02:19:57.79819');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (164, 3, 121, '2026-01-05 02:19:57.798474');
-- 审批权限分配给 DIRECTOR(2), PARTNER(3) - 业务审批由业务管理层负责，ADMIN不参与
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (168, 2, 130, '2026-01-05 10:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (169, 2, 131, '2026-01-05 10:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (170, 2, 132, '2026-01-05 10:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (171, 3, 130, '2026-01-05 10:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (172, 3, 131, '2026-01-05 10:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (173, 3, 132, '2026-01-05 10:00:00');
-- 律师财务菜单权限分配（我的收款、我的提成、费用报销）
-- LEAD_LAWYER(6), ASSOCIATE(7), TRAINEE(9) 可以看到律师视角的财务菜单
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (174, 6, 5, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (175, 6, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (176, 6, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (177, 6, 59, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (178, 7, 5, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (179, 7, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (180, 7, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (181, 7, 59, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (182, 9, 5, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (183, 9, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (184, 9, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (185, 9, 59, '2026-01-05 16:00:00');
-- 财务人员(5)也可以看到律师视角菜单（方便查看）
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (186, 5, 1, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (187, 5, 5, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (188, 5, 51, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (189, 5, 52, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (190, 5, 53, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (191, 5, 54, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (192, 5, 55, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (193, 5, 56, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (194, 5, 123, '2026-01-05 16:00:00');
-- 财务人员(5)需要基础业务模块权限（客户、项目、合同、文书、档案、知识库）
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (246, 5, 3, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (247, 5, 4, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (248, 5, 6, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (249, 5, 8, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (250, 5, 11, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (251, 5, 31, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (252, 5, 32, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (253, 5, 33, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (254, 5, 41, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (255, 5, 42, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (256, 5, 45, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (257, 5, 43, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (258, 5, 44, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (259, 5, 61, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (260, 5, 62, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (261, 5, 63, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (262, 5, 64, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (263, 5, 81, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (264, 5, 82, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (265, 5, 111, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (266, 5, 112, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (267, 5, 113, '2026-01-05 17:00:00');
-- 行政人员(8)需要基础业务模块权限（客户、项目、合同、文书、档案、知识库、财务-律师视角）
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (268, 8, 3, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (269, 8, 4, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (270, 8, 5, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (271, 8, 6, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (272, 8, 8, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (273, 8, 11, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (274, 8, 31, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (275, 8, 32, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (276, 8, 33, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (277, 8, 41, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (278, 8, 42, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (279, 8, 45, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (280, 8, 43, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (281, 8, 44, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (282, 8, 57, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (283, 8, 58, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (284, 8, 59, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (285, 8, 61, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (286, 8, 62, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (287, 8, 63, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (288, 8, 64, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (289, 8, 81, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (290, 8, 82, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (291, 8, 111, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (292, 8, 112, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (293, 8, 113, '2026-01-05 17:00:00');
-- 财务主管(4)也需要基础业务模块权限
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (294, 4, 3, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (295, 4, 4, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (296, 4, 6, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (297, 4, 8, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (298, 4, 11, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (299, 4, 31, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (300, 4, 32, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (301, 4, 33, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (302, 4, 41, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (303, 4, 42, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (304, 4, 45, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (305, 4, 43, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (306, 4, 44, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (307, 4, 61, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (308, 4, 62, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (309, 4, 63, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (310, 4, 64, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (311, 4, 81, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (312, 4, 82, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (313, 4, 111, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (314, 4, 112, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (315, 4, 113, '2026-01-05 17:00:00');
-- ADMIN(1)也需要新菜单权限
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (195, 1, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (196, 1, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (197, 1, 59, '2026-01-05 16:00:00');
-- DIRECTOR(2), PARTNER(3) 也需要新菜单权限
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (198, 2, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (199, 2, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (200, 2, 59, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (201, 3, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (202, 3, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (203, 3, 59, '2026-01-05 16:00:00');
-- 财务主管(4)也需要新菜单权限
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (204, 4, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (205, 4, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (206, 4, 59, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (207, 4, 123, '2026-01-05 16:00:00');
-- ASSOCIATE(7) 协办律师基本菜单权限
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (208, 7, 1, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (209, 7, 3, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (210, 7, 4, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (211, 7, 6, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (212, 7, 8, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (213, 7, 11, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (214, 7, 31, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (215, 7, 32, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (216, 7, 33, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (217, 7, 41, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (218, 7, 42, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (219, 7, 45, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (220, 7, 43, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (221, 7, 44, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (222, 7, 61, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (223, 7, 62, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (224, 7, 63, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (225, 7, 64, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (226, 7, 81, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (227, 7, 82, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (228, 7, 111, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (229, 7, 112, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (230, 7, 113, '2026-01-05 17:00:00');
-- TRAINEE(9) 实习律师基本菜单权限（与协办律师类似，但权限更少）
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (231, 9, 1, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (232, 9, 3, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (233, 9, 4, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (234, 9, 6, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (235, 9, 11, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (236, 9, 31, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (237, 9, 32, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (238, 9, 42, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (239, 9, 43, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (240, 9, 44, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (241, 9, 61, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (242, 9, 62, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (243, 9, 111, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (244, 9, 112, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (245, 9, 113, '2026-01-05 17:00:00');
-- 提成规则配置权限分配（仅ADMIN和DIRECTOR）
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (341, 1, 140, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (346, 1, 145, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (347, 1, 146, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (348, 1, 147, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (349, 2, 140, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (354, 2, 145, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (355, 2, 146, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (356, 2, 147, '2026-01-05 18:00:00');
-- 提成查看权限分配给其他财务角色
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (357, 3, 145, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (358, 4, 145, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (359, 4, 147, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (360, 5, 145, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (361, 5, 147, '2026-01-05 18:00:00');
-- 合同变更处理权限分配给ADMIN（role_id=1）
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (362, 1, 123, '2026-01-06 00:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (363, 1, 124, '2026-01-06 00:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (364, 1, 125, '2026-01-06 00:00:00');
-- 我的合同菜单权限分配
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (365, 1, 148, '2026-01-06 09:30:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (366, 2, 148, '2026-01-06 09:30:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (367, 3, 148, '2026-01-06 09:30:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (368, 4, 148, '2026-01-06 09:30:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (369, 5, 148, '2026-01-06 09:30:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (370, 6, 148, '2026-01-06 09:30:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (371, 7, 148, '2026-01-06 09:30:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (372, 8, 148, '2026-01-06 09:30:00');

-- 日程管理权限分配
-- ADMIN (1)
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (373, 1, 149, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (374, 1, 150, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (375, 1, 151, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (376, 1, 152, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (377, 1, 153, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (378, 1, 154, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (379, 1, 155, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (380, 1, 156, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (381, 1, 161, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (382, 1, 162, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (383, 1, 163, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (384, 1, 164, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (385, 1, 165, '2026-01-06 12:00:00');

-- DIRECTOR (2)
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (386, 2, 149, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (387, 2, 150, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (388, 2, 151, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (389, 2, 152, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (390, 2, 153, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (391, 2, 154, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (392, 2, 155, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (393, 2, 156, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (394, 2, 161, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (395, 2, 162, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (396, 2, 163, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (397, 2, 164, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (398, 2, 165, '2026-01-06 12:00:00');

-- PARTNER (3)
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (399, 3, 149, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (400, 3, 150, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (401, 3, 151, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (402, 3, 152, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (403, 3, 153, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (404, 3, 154, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (405, 3, 155, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (406, 3, 156, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (407, 3, 161, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (408, 3, 162, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (409, 3, 163, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (410, 3, 164, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (411, 3, 165, '2026-01-06 12:00:00');

-- LAWYER (4)
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (412, 4, 149, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (413, 4, 150, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (414, 4, 151, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (415, 4, 152, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (416, 4, 153, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (417, 4, 154, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (418, 4, 155, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (419, 4, 156, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (420, 4, 161, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (421, 4, 162, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (422, 4, 163, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (423, 4, 164, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (424, 4, 165, '2026-01-06 12:00:00');

-- PARALEGAL (5)
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (425, 5, 149, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (426, 5, 150, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (427, 5, 151, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (428, 5, 152, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (429, 5, 153, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (430, 5, 154, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (431, 5, 155, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (432, 5, 156, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (433, 5, 161, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (434, 5, 162, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (435, 5, 163, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (436, 5, 164, '2026-01-06 12:00:00');

-- INTERN (6)
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (437, 6, 149, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (438, 6, 150, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (439, 6, 152, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (440, 6, 153, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (441, 6, 161, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (442, 6, 162, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (443, 6, 163, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (444, 6, 164, '2026-01-06 12:00:00');

-- FINANCE (7)
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (445, 7, 149, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (446, 7, 150, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (447, 7, 151, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (448, 7, 163, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (449, 7, 164, '2026-01-06 12:00:00');

-- HR (8)
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (450, 8, 149, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (451, 8, 150, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (452, 8, 151, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (453, 8, 161, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (454, 8, 162, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (455, 8, 163, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (456, 8, 164, '2026-01-06 12:00:00');
INSERT INTO public.sys_role_menu (id, role_id, menu_id, created_at) VALUES (457, 8, 165, '2026-01-06 12:00:00');


--
-- Data for Name: sys_user; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) VALUES (2, 'lawyer1', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '张律师', 'lawyer1@lawfirm.com', '13800138001', NULL, 2, '律师', 'EMP002', 'L20240001', NULL, 'COMMISSION', true, 'ACTIVE', NULL, NULL, '2026-01-05 03:13:37.697736', '2026-01-05 03:13:37.697736', NULL, NULL, false);
INSERT INTO public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) VALUES (1, 'admin', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '系统管理员', 'admin@lawfirm.com', '13800138000', NULL, 1, '系统管理员', NULL, NULL, NULL, 'HYBRID', true, 'ACTIVE', '2026-01-05 02:57:04.449217', '0:0:0:0:0:0:0:1', '2026-01-04 16:14:15.893998', '2026-01-04 16:14:15.893998', NULL, NULL, false);
-- 添加主任用户（用于审批）
INSERT INTO public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) VALUES (3, 'director', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '李主任', 'director@lawfirm.com', '13800138002', NULL, 1, '主任', 'EMP003', 'L20240002', NULL, 'SALARIED', true, 'ACTIVE', NULL, NULL, '2026-01-05 10:00:00', '2026-01-05 10:00:00', NULL, NULL, false);
-- 添加合伙人用户（用于审批）
INSERT INTO public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) VALUES (4, 'partner', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '王合伙人', 'partner@lawfirm.com', '13800138003', NULL, 2, '合伙人', 'EMP004', 'L20240003', NULL, 'HYBRID', true, 'ACTIVE', NULL, NULL, '2026-01-05 10:00:00', '2026-01-05 10:00:00', NULL, NULL, false);
-- 添加财务人员用户
INSERT INTO public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) VALUES (5, 'finance', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '赵财务', 'finance@lawfirm.com', '13800138004', NULL, 5, '财务', 'EMP005', NULL, NULL, 'SALARIED', false, 'ACTIVE', NULL, NULL, '2026-01-05 16:00:00', '2026-01-05 16:00:00', NULL, NULL, false);
-- 添加财务主管用户
INSERT INTO public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) VALUES (6, 'finmgr', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '钱财务主管', 'finmgr@lawfirm.com', '13800138005', NULL, 5, '财务主管', 'EMP006', NULL, NULL, 'SALARIED', false, 'ACTIVE', NULL, NULL, '2026-01-05 17:00:00', '2026-01-05 17:00:00', NULL, NULL, false);
-- 添加协办律师用户
INSERT INTO public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) VALUES (7, 'associate', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '孙协办', 'associate@lawfirm.com', '13800138006', NULL, 2, '协办律师', 'EMP007', 'L20240004', NULL, 'COMMISSION', false, 'ACTIVE', NULL, NULL, '2026-01-05 17:00:00', '2026-01-05 17:00:00', NULL, NULL, false);
-- 添加行政人员用户
INSERT INTO public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) VALUES (8, 'staff', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '周行政', 'staff@lawfirm.com', '13800138007', NULL, 6, '行政专员', 'EMP008', NULL, NULL, 'SALARIED', false, 'ACTIVE', NULL, NULL, '2026-01-05 17:00:00', '2026-01-05 17:00:00', NULL, NULL, false);
-- 添加实习律师用户
INSERT INTO public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) VALUES (9, 'trainee', '$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S', '吴实习', 'trainee@lawfirm.com', '13800138008', NULL, 2, '实习律师', 'EMP009', NULL, NULL, 'SALARIED', false, 'ACTIVE', NULL, NULL, '2026-01-05 17:00:00', '2026-01-05 17:00:00', NULL, NULL, false);


--
-- Data for Name: sys_user_role; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_user_role (id, user_id, role_id, created_at) VALUES (1, 1, 1, '2026-01-04 16:14:15.894473');
INSERT INTO public.sys_user_role (id, user_id, role_id, created_at) VALUES (2, 2, 6, '2026-01-05 03:13:59.896251');
-- 主任用户分配 DIRECTOR 角色
INSERT INTO public.sys_user_role (id, user_id, role_id, created_at) VALUES (3, 3, 2, '2026-01-05 10:00:00');
-- 合伙人用户分配 PARTNER 角色
INSERT INTO public.sys_user_role (id, user_id, role_id, created_at) VALUES (4, 4, 3, '2026-01-05 10:00:00');
-- 财务人员分配 FINANCE 角色
INSERT INTO public.sys_user_role (id, user_id, role_id, created_at) VALUES (5, 5, 5, '2026-01-05 16:00:00');
-- 财务主管分配 FINANCE_MANAGER 角色
INSERT INTO public.sys_user_role (id, user_id, role_id, created_at) VALUES (6, 6, 4, '2026-01-05 17:00:00');
-- 协办律师分配 ASSOCIATE 角色
INSERT INTO public.sys_user_role (id, user_id, role_id, created_at) VALUES (7, 7, 7, '2026-01-05 17:00:00');
-- 行政人员分配 ADMIN_STAFF 角色
INSERT INTO public.sys_user_role (id, user_id, role_id, created_at) VALUES (8, 8, 8, '2026-01-05 17:00:00');
-- 实习律师分配 TRAINEE 角色
INSERT INTO public.sys_user_role (id, user_id, role_id, created_at) VALUES (9, 9, 9, '2026-01-05 17:00:00');


--
-- Name: sys_department_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_department_id_seq', 6, true);


--
-- Name: sys_menu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_menu_id_seq', 170, true);


--
-- Name: sys_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_role_id_seq', 9, true);


--
-- Name: sys_role_menu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_role_menu_id_seq', 460, true);


--
-- Name: sys_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_user_id_seq', 9, true);


--
-- Name: sys_user_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_user_role_id_seq', 9, true);


--
-- Data for Name: sys_config; Type: TABLE DATA; Schema: public; Owner: -
--

-- 合同编号配置
-- 支持的变量：
-- {PREFIX}      - 前缀（配置值）
-- {YEAR}        - 年份(YYYY)，如 2026
-- {YEAR_SHORT}  - 年份(YY)，如 26
-- {MONTH}       - 月份(MM)
-- {DAY}         - 日期(DD)
-- {DATE}        - 日期(YYMMDD)
-- {DATE_FULL}   - 完整日期(YYYYMMDD)
-- {CASE_TYPE}   - 案件类型中文简称（民/刑/行/知/仲/执/顾/非/破）
-- {CASE_TYPE_CODE} - 案件类型代码（MS/XS/XZ/ZS/ZC/ZX/GW/ZX/PC）
-- {FEE_TYPE}    - 收费类型（固/时/风/混）
-- {SEQUENCE}    - 日序号（当日第N份合同）
-- {SEQUENCE_YEAR} - 年度序号（本年度第N份合同）
-- {RANDOM}      - 随机字符
-- {RANDOM_NUM}  - 随机数字
--
-- 示例规则：
-- {YEAR}{CASE_TYPE}代字第{SEQUENCE_YEAR}号  -> 2026民代字第0001号
-- {PREFIX}{DATE}{RANDOM}                     -> HT260105ABCD
-- {YEAR}{CASE_TYPE_CODE}-{SEQUENCE_YEAR}    -> 2026MS-0001

INSERT INTO public.sys_config (id, config_key, config_value, config_name, config_type, description, is_system, created_at, updated_at, created_by, updated_by, deleted) VALUES 
(nextval('public.sys_config_id_seq'), 'contract.number.prefix', 'HT', '合同编号前缀', 'STRING', '合同编号的前缀，用于 {PREFIX} 变量', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
(nextval('public.sys_config_id_seq'), 'contract.number.pattern', '{YEAR}{CASE_TYPE}代字第{SEQUENCE_YEAR}号', '合同编号规则', 'STRING', '合同编号生成规则，支持多种变量组合。示例：2026民代字第0001号', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
(nextval('public.sys_config_id_seq'), 'contract.number.sequence.length', '4', '序号长度', 'STRING', '序号部分的长度(1-10)，不足位数前面补0', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false);



--
-- PostgreSQL database dump complete
--

\unrestrict zfSazfVoubL5nP9VwQfOeF5z0FOroDaYwwqhnzTxLkf1NH65LR5iS3qunPbP2V8

