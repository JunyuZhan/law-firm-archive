-- ============================================
-- 字典管理初始化数据
-- ============================================
-- 说明：根据系统137张表的字段需求，提供完整的字典数据
-- 执行时间：2026-01-10
-- ============================================

-- ==================== 第一部分：通用字典 ====================

-- 1. 性别
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (1, '性别', 'gender', '用户性别', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(1, 1, '男', 'MALE', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(2, 1, '女', 'FEMALE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(3, 1, '未知', 'UNKNOWN', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 2. 学历
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (2, '学历', 'education', '员工学历', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(10, 2, '博士', 'PHD', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(11, 2, '硕士', 'MASTER', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(12, 2, '本科', 'BACHELOR', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(13, 2, '大专', 'COLLEGE', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(14, 2, '其他', 'OTHER', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 3. 证件类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (3, '证件类型', 'id_type', '身份证件类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(20, 3, '身份证', 'ID_CARD', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(21, 3, '护照', 'PASSPORT', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(22, 3, '港澳通行证', 'HK_MACAO_PASS', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(23, 3, '台湾通行证', 'TAIWAN_PASS', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(24, 3, '统一社会信用代码', 'USCC', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(25, 3, '营业执照', 'BUSINESS_LICENSE', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 4. 紧急程度
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (4, '紧急程度', 'priority', '任务紧急程度', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(30, 4, '紧急', 'URGENT', 1, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(31, 4, '高', 'HIGH', 2, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(32, 4, '中', 'MEDIUM', 3, 'ENABLED', 'primary', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(33, 4, '低', 'LOW', 4, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 5. 行业分类
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (5, '行业分类', 'industry', '客户所属行业', 'ENABLED', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(40, 5, '金融业', 'FINANCE', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(41, 5, '房地产', 'REAL_ESTATE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(42, 5, '制造业', 'MANUFACTURING', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(43, 5, '互联网/IT', 'IT', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(44, 5, '医疗健康', 'HEALTHCARE', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(45, 5, '教育培训', 'EDUCATION', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(46, 5, '建筑工程', 'CONSTRUCTION', 7, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(47, 5, '零售/电商', 'RETAIL', 8, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(48, 5, '物流运输', 'LOGISTICS', 9, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(49, 5, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- ==================== 第二部分：系统管理字典 ====================

-- 6. 用户状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (6, '用户状态', 'user_status', '系统用户状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(50, 6, '正常', 'ACTIVE', 1, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(51, 6, '禁用', 'INACTIVE', 2, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(52, 6, '锁定', 'LOCKED', 3, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 7. 设备类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (7, '设备类型', 'device_type', '登录设备类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(60, 7, '电脑', 'PC', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(61, 7, '手机', 'MOBILE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(62, 7, '平板', 'TABLET', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 8. 通知类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (8, '通知类型', 'notification_type', '系统通知类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(70, 8, '系统通知', 'SYSTEM', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(71, 8, '审批通知', 'APPROVAL', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(72, 8, '任务通知', 'TASK', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(73, 8, '提醒通知', 'REMINDER', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 9. 菜单类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (9, '菜单类型', 'menu_type', '系统菜单类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(80, 9, '目录', 'DIRECTORY', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(81, 9, '菜单', 'MENU', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(82, 9, '按钮', 'BUTTON', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 10. 备份类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (10, '备份类型', 'backup_type', '系统备份类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(90, 10, '全量备份', 'FULL', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(91, 10, '增量备份', 'INCREMENTAL', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(92, 10, '数据库备份', 'DATABASE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(93, 10, '文件备份', 'FILE', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 11. 备份状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (11, '备份状态', 'backup_status', '备份执行状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(100, 11, '进行中', 'PENDING', 1, 'ENABLED', 'processing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, 11, '成功', 'SUCCESS', 2, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, 11, '失败', 'FAILED', 3, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 12. 数据交接类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (12, '数据交接类型', 'handover_type', '数据交接类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(110, 12, '离职交接', 'RESIGNATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(111, 12, '项目移交', 'PROJECT', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(112, 12, '客户移交', 'CLIENT', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(113, 12, '案源移交', 'LEAD', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 13. 外部集成类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (13, '外部集成类型', 'integration_type', '外部系统集成类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(120, 13, '档案系统', 'ARCHIVE', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(121, 13, 'AI大模型', 'AI', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(122, 13, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 14. 认证方式
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (14, '认证方式', 'auth_type', '外部系统认证方式', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(130, 14, 'API Key', 'API_KEY', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(131, 14, 'Bearer Token', 'BEARER_TOKEN', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(132, 14, 'Basic Auth', 'BASIC', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(133, 14, 'OAuth2', 'OAUTH2', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 15. 薪酬模式
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (15, '薪酬模式', 'compensation_type', '律师薪酬模式', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(140, 15, '提成制', 'COMMISSION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(141, 15, '授薪制', 'SALARIED', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(142, 15, '混合制', 'HYBRID', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- ==================== 第三部分：案件管理字典 ====================

-- 16. 案件类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (16, '案件类型', 'matter_type', '法律案件类型分类', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(150, 16, '民事诉讼', 'CIVIL', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(151, 16, '刑事诉讼', 'CRIMINAL', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(152, 16, '行政诉讼', 'ADMINISTRATIVE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(153, 16, '破产案件', 'BANKRUPTCY', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(154, 16, '知识产权', 'IP', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(155, 16, '仲裁案件', 'ARBITRATION', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(156, 16, '执行案件', 'ENFORCEMENT', 7, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(157, 16, '法律顾问', 'LEGAL_COUNSEL', 8, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(158, 16, '专项服务', 'SPECIAL_SERVICE', 9, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 17. 案件状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (17, '案件状态', 'matter_status', '案件进展状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(160, 17, '立案中', 'FILING', 1, 'ENABLED', 'processing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(161, 17, '进行中', 'ONGOING', 2, 'ENABLED', 'primary', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(162, 17, '已暂停', 'SUSPENDED', 3, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(163, 17, '已结案', 'CLOSED', 4, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(164, 17, '已归档', 'ARCHIVED', 5, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- ==================== 第四部分：客户管理字典 ====================

-- 18. 客户类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (18, '客户类型', 'client_type', '客户类型分类', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(170, 18, '个人', 'INDIVIDUAL', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(171, 18, '企业', 'ENTERPRISE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(172, 18, '政府机关', 'GOVERNMENT', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(173, 18, '事业单位', 'INSTITUTION', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(174, 18, '社会团体', 'ORGANIZATION', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 19. 客户来源
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (19, '客户来源', 'client_source', '客户获取渠道', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(180, 19, '老客户介绍', 'REFERRAL', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(181, 19, '网络推广', 'ONLINE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(182, 19, '电话咨询', 'PHONE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(183, 19, '上门咨询', 'WALK_IN', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(184, 19, '行业展会', 'EXHIBITION', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(185, 19, '合作伙伴', 'PARTNER', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(186, 19, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- ==================== 第五部分：财务管理字典 ====================

-- 20. 收费方式
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (20, '收费方式', 'fee_type', '律师费收费方式', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(190, 20, '固定收费', 'FIXED', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(191, 20, '计时收费', 'HOURLY', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(192, 20, '风险代理', 'CONTINGENCY', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(193, 20, '混合收费', 'MIXED', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 21. 合同类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (21, '合同类型', 'contract_type', '财务合同类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(200, 21, '服务合同', 'SERVICE', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(201, 21, '常年法顾', 'RETAINER', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(202, 21, '诉讼代理', 'LITIGATION', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(203, 21, '非诉项目', 'NON_LITIGATION', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 22. 合同状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (22, '合同状态', 'contract_status', '财务合同状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(210, 22, '草稿', 'DRAFT', 1, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(211, 22, '待审批', 'PENDING', 2, 'ENABLED', 'processing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(212, 22, '生效中', 'ACTIVE', 3, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(213, 22, '已拒绝', 'REJECTED', 4, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(214, 22, '已终止', 'TERMINATED', 5, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(215, 22, '已完成', 'COMPLETED', 6, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(216, 22, '已过期', 'EXPIRED', 7, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 23. 发票类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (23, '发票类型', 'invoice_type', '发票类型分类', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(220, 23, '增值税专用发票', 'VAT_SPECIAL', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(221, 23, '增值税普通发票', 'VAT_NORMAL', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(222, 23, '电子发票', 'ELECTRONIC', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 24. 支付方式
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (24, '支付方式', 'payment_method', '客户支付方式', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(230, 24, '银行转账', 'BANK_TRANSFER', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(231, 24, '现金', 'CASH', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(232, 24, '支票', 'CHECK', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(233, 24, '微信支付', 'WECHAT', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(234, 24, '支付宝', 'ALIPAY', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 25. 提成状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (25, '提成状态', 'commission_status', '提成发放状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(240, 25, '待审批', 'PENDING', 1, 'ENABLED', 'processing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(241, 25, '已审批', 'APPROVED', 2, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(242, 25, '已发放', 'PAID', 3, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(243, 25, '已取消', 'CANCELLED', 4, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 26. 利冲审查状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (26, '利冲审查状态', 'conflict_check_status', '利益冲突审查状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(250, 26, '待审查', 'PENDING', 1, 'ENABLED', 'processing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(251, 26, '已通过', 'PASSED', 2, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(252, 26, '未通过', 'FAILED', 3, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(253, 26, '无需审查', 'NOT_REQUIRED', 4, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 27. 合同变更类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (27, '合同变更类型', 'amendment_type', '合同变更类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(260, 27, '金额变更', 'AMOUNT', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(261, 27, '参与人变更', 'PARTICIPANT', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(262, 27, '付款计划变更', 'SCHEDULE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(263, 27, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- ==================== 第六部分：人力资源字典 ====================

-- 28. 员工状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (28, '员工状态', 'work_status', '员工工作状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(270, 28, '在职', 'ACTIVE', 1, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(271, 28, '试用', 'PROBATION', 2, 'ENABLED', 'processing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(272, 28, '离职', 'RESIGNED', 3, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(273, 28, '退休', 'RETIRED', 4, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 29. 劳动合同类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (29, '劳动合同类型', 'hr_contract_type', '员工劳动合同类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(280, 29, '固定期限', 'FIXED', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(281, 29, '无固定期限', 'UNFIXED', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(282, 29, '项目合同', 'PROJECT', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(283, 29, '实习', 'INTERN', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 30. 职称
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (30, '职称', 'title', '律师职称', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(290, 30, '高级合伙人', 'SENIOR_PARTNER', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(291, 30, '合伙人', 'PARTNER', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(292, 30, '资深律师', 'SENIOR_LAWYER', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(293, 30, '执业律师', 'LAWYER', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(294, 30, '实习律师', 'TRAINEE', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(295, 30, '律师助理', 'ASSISTANT', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 31. 考勤状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (31, '考勤状态', 'attendance_status', '员工考勤状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(300, 31, '正常', 'NORMAL', 1, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(301, 31, '迟到', 'LATE', 2, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(302, 31, '早退', 'EARLY', 3, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(303, 31, '缺勤', 'ABSENT', 4, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(304, 31, '请假', 'LEAVE', 5, 'ENABLED', 'processing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 32. 离职类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (32, '离职类型', 'resignation_type', '员工离职类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(310, 32, '主动离职', 'VOLUNTARY', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(311, 32, '辞退', 'DISMISSED', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(312, 32, '退休', 'RETIREMENT', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(313, 32, '合同到期', 'CONTRACT_EXPIRED', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 33. 培训类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (33, '培训类型', 'training_type', '员工培训类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(320, 33, '内部培训', 'INTERNAL', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(321, 33, '外部培训', 'EXTERNAL', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(322, 33, '在线培训', 'ONLINE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 34. 培训分类
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (34, '培训分类', 'training_category', '培训内容分类', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(330, 34, '法律知识', 'LAW', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(331, 34, '业务技能', 'SKILL', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(332, 34, '管理能力', 'MANAGEMENT', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(333, 34, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- ==================== 第七部分：文档管理字典 ====================

-- 35. 文档状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (35, '文档状态', 'doc_status', '文档状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(340, 35, '正常', 'ACTIVE', 1, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(341, 35, '已归档', 'ARCHIVED', 2, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(342, 35, '已删除', 'DELETED', 3, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 36. 文件分类
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (36, '文件分类', 'file_category', '文档文件分类', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(350, 36, '证据材料', 'EVIDENCE', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(351, 36, '诉讼文书', 'PLEADING', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(352, 36, '合同文件', 'CONTRACT', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(353, 36, '往来函件', 'CORRESPONDENCE', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(354, 36, '会见记录', 'MEETING', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(355, 36, '裁判文书', 'JUDGMENT', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(356, 36, '工作日志', 'WORKLOG', 7, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(357, 36, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 37. 文档模板类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (37, '文档模板类型', 'template_type', '文档模板类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(360, 37, '合同', 'CONTRACT', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(361, 37, '法律意见书', 'LEGAL_OPINION', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(362, 37, '授权委托书', 'POWER_OF_ATTORNEY', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(363, 37, '起诉状', 'COMPLAINT', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(364, 37, '答辩状', 'DEFENSE', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(365, 37, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- ==================== 第八部分：工时管理字典 ====================

-- 38. 工作类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (38, '工作类型', 'work_type', '工时记录工作类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(370, 38, '法律研究', 'RESEARCH', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(371, 38, '文书起草', 'DRAFTING', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(372, 38, '会议', 'MEETING', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(373, 38, '出庭', 'COURT', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(374, 38, '谈判', 'NEGOTIATION', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(375, 38, '沟通', 'COMMUNICATION', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(376, 38, '差旅', 'TRAVEL', 7, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(377, 38, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 39. 工时状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (39, '工时状态', 'timesheet_status', '工时记录状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(380, 39, '草稿', 'DRAFT', 1, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(381, 39, '已提交', 'SUBMITTED', 2, 'ENABLED', 'processing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(382, 39, '已批准', 'APPROVED', 3, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(383, 39, '已拒绝', 'REJECTED', 4, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- ==================== 第九部分：行政管理字典 ====================

-- 40. 资产类别
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (40, '资产类别', 'asset_category', '行政资产类别', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(390, 40, '办公设备', 'OFFICE', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(391, 40, 'IT设备', 'IT', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(392, 40, '家具', 'FURNITURE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(393, 40, '车辆', 'VEHICLE', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(394, 40, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 41. 资产状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (41, '资产状态', 'asset_status', '资产使用状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(400, 41, '闲置', 'IDLE', 1, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(401, 41, '使用中', 'IN_USE', 2, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(402, 41, '维修中', 'MAINTENANCE', 3, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(403, 41, '已报废', 'SCRAPPED', 4, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 42. 印章类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (42, '印章类型', 'seal_type', '印章类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(410, 42, '公章', 'OFFICIAL', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(411, 42, '合同章', 'CONTRACT', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(412, 42, '财务章', 'FINANCE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(413, 42, '法人章', 'LEGAL', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(414, 42, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 43. 印章状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (43, '印章状态', 'seal_status', '印章状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(420, 43, '在用', 'ACTIVE', 1, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(421, 43, '停用', 'DISABLED', 2, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(422, 43, '遗失', 'LOST', 3, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(423, 43, '销毁', 'DESTROYED', 4, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 44. 函件类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (44, '函件类型', 'letter_type', '出函类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(430, 44, '介绍信', 'INTRODUCTION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(431, 44, '会见函', 'MEETING', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(432, 44, '调查函', 'INVESTIGATION', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(433, 44, '阅卷函', 'FILE_REVIEW', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(434, 44, '法律意见函', 'LEGAL_OPINION', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(435, 44, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 45. 会议室状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (45, '会议室状态', 'meeting_room_status', '会议室状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(440, 45, '可用', 'AVAILABLE', 1, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(441, 45, '占用', 'OCCUPIED', 2, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(442, 45, '维护中', 'MAINTENANCE', 3, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- ==================== 第十部分：知识库字典 ====================

-- 46. 知识文章状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (46, '知识文章状态', 'article_status', '知识库文章状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(450, 46, '草稿', 'DRAFT', 1, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(451, 46, '已发布', 'PUBLISHED', 2, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(452, 46, '已归档', 'ARCHIVED', 3, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 47. 法规状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (47, '法规状态', 'law_status', '法律法规状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(460, 47, '有效', 'EFFECTIVE', 1, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(461, 47, '已修订', 'AMENDED', 2, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(462, 47, '已废止', 'REPEALED', 3, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- ==================== 第十一部分：通用审批状态字典 ====================

-- 48. 审批状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (48, '审批状态', 'approval_status', '通用审批状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(470, 48, '待审批', 'PENDING', 1, 'ENABLED', 'processing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(471, 48, '审批中', 'PROCESSING', 2, 'ENABLED', 'primary', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(472, 48, '已通过', 'APPROVED', 3, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(473, 48, '已拒绝', 'REJECTED', 4, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(474, 48, '已撤回', 'WITHDRAWN', 5, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 49. 归档状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (49, '归档状态', 'archive_status', '案件归档状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(480, 49, '未归档', 'NOT_ARCHIVED', 1, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(481, 49, '已归档', 'ARCHIVED', 2, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(482, 49, '已销毁', 'DESTROYED', 3, 'ENABLED', 'danger', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- 50. 启用禁用状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (50, '启用状态', 'enable_status', '通用启用禁用状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted)
VALUES 
(490, 50, '启用', 'ENABLED', 1, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(491, 50, '禁用', 'DISABLED', 2, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order, css_class = EXCLUDED.css_class;

-- ==================== 第十二部分：代理阶段字典 ====================

-- 51. 民事案件代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (51, '民事案件代理阶段', 'litigation_stage_civil', '民事案件的诉讼代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(510, 51, '一审', 'FIRST_INSTANCE', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(511, 51, '二审', 'SECOND_INSTANCE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(512, 51, '再审', 'RETRIAL', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(513, 51, '执行', 'EXECUTION', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 52. 刑事案件代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (52, '刑事案件代理阶段', 'litigation_stage_criminal', '刑事案件的诉讼代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(520, 52, '侦查阶段', 'INVESTIGATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(521, 52, '审查起诉', 'PROSECUTION_REVIEW', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(522, 52, '一审', 'FIRST_INSTANCE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(523, 52, '二审', 'SECOND_INSTANCE', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(524, 52, '死刑复核', 'DEATH_PENALTY_REVIEW', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(525, 52, '再审', 'RETRIAL', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 53. 行政案件代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (53, '行政案件代理阶段', 'litigation_stage_administrative', '行政案件的诉讼代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(530, 53, '行政复议', 'ADMINISTRATIVE_RECONSIDERATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(531, 53, '一审', 'FIRST_INSTANCE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(532, 53, '二审', 'SECOND_INSTANCE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(533, 53, '再审', 'RETRIAL', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 54. 劳动仲裁代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (54, '劳动仲裁代理阶段', 'litigation_stage_labor_arbitration', '劳动仲裁的代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(540, 54, '仲裁阶段', 'ARBITRATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(541, 54, '一审', 'FIRST_INSTANCE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(542, 54, '二审', 'SECOND_INSTANCE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(543, 54, '再审', 'RETRIAL', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(544, 54, '执行', 'EXECUTION', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 55. 商事仲裁代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (55, '商事仲裁代理阶段', 'litigation_stage_commercial_arbitration', '商事仲裁的代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(550, 55, '仲裁阶段', 'ARBITRATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(551, 55, '执行', 'EXECUTION', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 56. 执行案件代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (56, '执行案件代理阶段', 'litigation_stage_enforcement', '执行案件的代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(560, 56, '执行阶段', 'EXECUTION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(561, 56, '执行异议', 'EXECUTION_OBJECTION', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(562, 56, '执行复议', 'EXECUTION_REVIEW', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 57. 非诉项目服务阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (57, '非诉项目服务阶段', 'litigation_stage_default', '非诉项目的服务阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(570, 57, '非诉服务', 'NON_LITIGATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- ==================== 第十三部分：国家赔偿相关字典 ====================

-- 61. 行政国家赔偿代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (61, '行政国家赔偿代理阶段', 'litigation_stage_state_comp_admin', '行政国家赔偿案件的代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(610, 61, '赔偿申请', 'COMPENSATION_APPLICATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(611, 61, '赔偿决定', 'COMPENSATION_DECISION', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(612, 61, '行政复议', 'ADMIN_RECONSIDERATION', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(613, 61, '行政赔偿诉讼', 'ADMIN_LITIGATION', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(614, 61, '一审', 'FIRST_INSTANCE', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(615, 61, '二审', 'SECOND_INSTANCE', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(616, 61, '执行', 'ENFORCEMENT', 7, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 62. 刑事国家赔偿代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (62, '刑事国家赔偿代理阶段', 'litigation_stage_state_comp_criminal', '刑事国家赔偿案件的代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(620, 62, '刑事诉讼终结确认', 'CRIMINAL_TERMINATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(621, 62, '赔偿申请', 'COMPENSATION_APPLICATION', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(622, 62, '赔偿决定', 'COMPENSATION_DECISION', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(623, 62, '刑事赔偿复议', 'CRIMINAL_REVIEW', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(624, 62, '赔偿委员会', 'COMPENSATION_COMMITTEE', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(625, 62, '上级赔偿委员会', 'COMMITTEE_REVIEW', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(626, 62, '支付赔偿金', 'PAYMENT', 7, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 63. 赔偿义务机关类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (63, '赔偿义务机关类型', 'obligor_org_type', '国家赔偿案件中的赔偿义务机关类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(630, 63, '公安机关', 'PUBLIC_SECURITY', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(631, 63, '检察机关', 'PROCURATORATE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(632, 63, '审判机关', 'COURT', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(633, 63, '监狱管理机关', 'PRISON', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(634, 63, '行政机关', 'ADMIN_ORGAN', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(635, 63, '其他', 'OTHER', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 64. 致损行为类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (64, '致损行为类型', 'damage_cause_type', '国家赔偿案件中的致损行为类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(640, 64, '违法拘留', 'ILLEGAL_DETENTION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(641, 64, '违法采取强制措施', 'ILLEGAL_COERCIVE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(642, 64, '违法使用武器警械', 'ILLEGAL_WEAPON', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(643, 64, '违法搜查', 'ILLEGAL_SEARCH', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(644, 64, '错误判决/错判', 'WRONGFUL_CONVICT', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(645, 64, '违法查封/扣押/冻结财产', 'ILLEGAL_DETENTION_PROPERTY', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(646, 64, '违法行政处罚', 'ILLEGAL_ADMIN_PUNISHMENT', 7, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(647, 64, '其他', 'OTHER', 8, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 65. 赔偿决定结果
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (65, '赔偿决定结果', 'compensation_decision_result', '国家赔偿案件的决定结果类型', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(650, 65, '予以赔偿', 'GRANTED', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(651, 65, '不予赔偿', 'DENIED', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(652, 65, '部分赔偿', 'PARTIAL_GRANTED', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 66. 赔偿支付状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (66, '赔偿支付状态', 'compensation_payment_status', '国家赔偿金的支付状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(660, 66, '未支付', 'UNPAID', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(661, 66, '已支付', 'PAID', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(662, 66, '部分支付', 'PARTIAL_PAID', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- ==================== 更新序列 ====================

SELECT setval('sys_dict_type_id_seq', GREATEST((SELECT MAX(id) FROM sys_dict_type), 70));
SELECT setval('sys_dict_item_id_seq', GREATEST((SELECT MAX(id) FROM sys_dict_item), 700));

-- ============================================
-- 统计信息
-- ============================================
-- 字典类型：66 种（+9 国家赔偿相关）
-- 字典项：约 270 个（+40 国家赔偿相关）
-- 覆盖模块：系统管理、案件管理、客户管理、财务管理、
--           人力资源、文档管理、工时管理、行政管理、知识库、代理阶段、国家赔偿
-- ============================================
-- 字典编码与案件类型对照表
-- ============================================
-- 案件类型                 | 字典编码
-- ------------------------|-----------------------------------
-- CIVIL                   | litigation_stage_civil
-- CRIMINAL                | litigation_stage_criminal
-- ADMINISTRATIVE          | litigation_stage_administrative
-- LABOR_ARBITRATION       | litigation_stage_labor_arbitration
-- COMMERCIAL_ARBITRATION  | litigation_stage_commercial_arbitration
-- ENFORCEMENT             | litigation_stage_enforcement
-- STATE_COMP_ADMIN        | litigation_stage_state_comp_admin
-- STATE_COMP_CRIMINAL     | litigation_stage_state_comp_criminal
-- 其他（非诉）            | litigation_stage_default
-- ============================================
