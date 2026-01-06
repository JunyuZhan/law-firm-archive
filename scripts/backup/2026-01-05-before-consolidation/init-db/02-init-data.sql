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
(8, 0, '档案管理', '/archive', 'LAYOUT', 'DatabaseOutlined', 'DIRECTORY', 7),
(9, 0, '行政管理', '/admin', 'LAYOUT', 'BankOutlined', 'DIRECTORY', 8),
(10, 0, '人力资源', '/hr', 'LAYOUT', 'IdcardOutlined', 'DIRECTORY', 9),
(11, 0, '知识库', '/knowledge', 'LAYOUT', 'BookOutlined', 'DIRECTORY', 10);

-- 系统管理子菜单 (parent_id = 2)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(21, 2, '用户管理', '/system/user', 'system/user/index', 'sys:user:list', 'UserOutlined', 'MENU', 1),
(22, 2, '角色管理', '/system/role', 'system/role/index', 'sys:role:list', 'SafetyCertificateOutlined', 'MENU', 2),
(23, 2, '部门管理', '/system/dept', 'system/dept/index', 'sys:dept:list', 'ApartmentOutlined', 'MENU', 3),
(24, 2, '菜单管理', '/system/menu', 'system/menu/index', 'sys:menu:list', 'MenuOutlined', 'MENU', 4),
(25, 2, '系统配置', '/system/config', 'system/config/index', 'sys:config:list', 'ToolOutlined', 'MENU', 5),
(26, 2, '操作日志', '/system/log', 'system/log/index', 'sys:log:list', 'FileSearchOutlined', 'MENU', 6),
(27, 2, '出函模板', '/system/letter-template', 'system/letter-template/index', 'sys:letter-template:list', 'MailOutlined', 'MENU', 7),
(28, 2, '合同模板', '/system/contract-template', 'system/contract-template/index', 'sys:contract-template:list', 'FileProtectOutlined', 'MENU', 8);

-- 客户管理子菜单 (parent_id = 3)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(31, 3, '客户列表', '/crm/client', 'crm/client/index', 'client:list', 'ContactsOutlined', 'MENU', 1),
(32, 3, '利冲审查', '/crm/conflict', 'crm/conflict/index', 'conflict:apply', 'SecurityScanOutlined', 'MENU', 2),
(33, 3, '案源管理', '/crm/lead', 'crm/lead/index', 'lead:list', 'FunnelPlotOutlined', 'MENU', 3);

-- 项目管理子菜单 (parent_id = 4)
-- 注：证据管理已移至项目详情页的Tab中，不再作为独立菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status) VALUES
(41, 4, '项目列表', '/matter/list', 'matter/list/index', 'matter:list', 'ProjectOutlined', 'MENU', 1, TRUE, 'ENABLED'),
(42, 4, '我的项目', '/matter/my', 'matter/my/index', 'matter:list', 'SolutionOutlined', 'MENU', 2, TRUE, 'ENABLED'),
(45, 4, '合同管理', '/matter/contract', 'matter/contract/index', 'matter:contract:list', 'FileProtectOutlined', 'MENU', 3, TRUE, 'ENABLED'),
(43, 4, '工时管理', '/matter/timesheet', 'matter/timesheet/index', 'timesheet:record', 'ClockCircleOutlined', 'MENU', 4, TRUE, 'ENABLED'),
(44, 4, '任务管理', '/matter/task', 'matter/task/index', 'task:manage', 'CheckSquareOutlined', 'MENU', 5, TRUE, 'ENABLED');

-- 财务管理子菜单 (parent_id = 5) - 合同查看（只读）
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order, visible, status) VALUES
(51, 5, '合同查看', '/finance/contract', 'finance/contract/index', 'contract:view', 'FileProtectOutlined', 'MENU', 1, TRUE, 'ENABLED'),
(52, 5, '收款管理', '/finance/payment', 'finance/payment/index', 'finance:payment:list', 'PayCircleOutlined', 'MENU', 2, TRUE, 'ENABLED'),
(53, 5, '提成管理', '/finance/commission', 'finance/commission/index', 'finance:commission:list', 'PercentageOutlined', 'MENU', 3, TRUE, 'ENABLED'),
(54, 5, '发票管理', '/finance/invoice', 'finance/invoice/index', 'finance:invoice:apply', 'ProfileOutlined', 'MENU', 4, TRUE, 'ENABLED'),
(55, 5, '财务报表', '/finance/report', 'finance/report/index', 'finance:report:view', 'BarChartOutlined', 'MENU', 5, TRUE, 'ENABLED');

-- 文书管理子菜单 (parent_id = 6)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(61, 6, '文书列表', '/document/list', 'document/list/index', 'doc:list', 'FileOutlined', 'MENU', 1),
(62, 6, '文书模板', '/document/template', 'document/template/index', 'doc:template:list', 'FileAddOutlined', 'MENU', 2),
(63, 6, '印章管理', '/document/seal', 'document/seal/index', 'doc:seal:list', 'SafetyOutlined', 'MENU', 3),
(64, 6, '用印申请', '/document/seal-apply', 'document/seal-apply/index', 'doc:seal:apply', 'FormOutlined', 'MENU', 4);

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
(95, 9, '采购管理', '/admin/purchase', 'admin/purchase/index', 'admin:purchase:list', 'ShoppingCartOutlined', 'MENU', 5),
(96, 9, '供应商管理', '/admin/supplier', 'admin/supplier/index', 'admin:supplier:list', 'ShopOutlined', 'MENU', 6),
(97, 9, '加班管理', '/admin/overtime', 'admin/overtime/index', 'admin:overtime:list', 'ClockCircleOutlined', 'MENU', 7),
(98, 9, '资产盘点', '/admin/asset-inventory', 'admin/asset-inventory/index', 'admin:asset-inventory:list', 'FileSearchOutlined', 'MENU', 8),
(99, 9, '外出管理', '/admin/go-out', 'admin/go-out/index', 'admin:go-out:list', 'ExportOutlined', 'MENU', 9),
(100, 9, '会议记录', '/admin/meeting-record', 'admin/meeting-record/index', 'admin:meeting-record:list', 'FileTextOutlined', 'MENU', 10),
(120, 9, '出函管理', '/admin/letter', 'admin/letter/index', 'admin:letter:list', 'MailOutlined', 'MENU', 11);

-- 人力资源子菜单 (parent_id = 10)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(101, 10, '培训管理', '/hr/training', 'hr/training/index', 'hr:training:list', 'ReadOutlined', 'MENU', 1),
(102, 10, '绩效考核', '/hr/performance', 'hr/performance/index', 'hr:performance:list', 'TrophyOutlined', 'MENU', 2),
(103, 10, '晋升管理', '/hr/promotion', 'hr/promotion/index', 'hr:promotion:list', 'RiseOutlined', 'MENU', 3),
(104, 10, '发展计划', '/hr/development', 'hr/development/index', 'hr:development:list', 'AimOutlined', 'MENU', 4),
(105, 10, '转正管理', '/hr/regularization', 'hr/regularization/index', 'hr:regularization:list', 'CheckSquareOutlined', 'MENU', 5),
(106, 10, '离职管理', '/hr/resignation', 'hr/resignation/index', 'hr:resignation:list', 'ExportOutlined', 'MENU', 6);

-- 知识库子菜单 (parent_id = 11)
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, icon, menu_type, sort_order) VALUES
(111, 11, '知识文章', '/knowledge/article', 'knowledge/article/index', 'knowledge:article:list', 'FileTextOutlined', 'MENU', 1),
(112, 11, '案例库', '/knowledge/case', 'knowledge/case/index', 'knowledge:case:list', 'ContainerOutlined', 'MENU', 2),
(113, 11, '法规库', '/knowledge/law', 'knowledge/law/index', 'knowledge:law:list', 'BookOutlined', 'MENU', 3);

-- 重置序列，确保后续插入ID正确
SELECT setval('sys_menu_id_seq', (SELECT MAX(id) FROM sys_menu));

-- ============================================
-- 五、初始化提成规则（将在15-commission-schema.sql之后执行）
-- ============================================
-- 注意：提成规则数据插入已移至30-commission-init-data.sql

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

-- 为财务主管分配财务相关权限（包括合同查看权限）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m 
WHERE r.role_code = 'FINANCE_MANAGER' 
AND (m.id IN (1, 5) OR m.parent_id = 5);

-- 为主办律师分配业务权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m 
WHERE r.role_code = 'LEAD_LAWYER' 
AND (m.id IN (1, 3, 4, 6, 8, 11) OR m.parent_id IN (3, 4, 6, 8, 11));

-- 为行政人员分配行政权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m 
WHERE r.role_code = 'ADMIN_STAFF' 
AND (m.id IN (1, 9) OR m.parent_id = 9);

-- ============================================
-- 七、初始化合同模板
-- ============================================

INSERT INTO contract_template (template_no, name, contract_type, fee_type, content, clauses, description, status, sort_order) VALUES
('CT-001', '标准服务合同', 'SERVICE', 'FIXED', 
'委托代理合同

甲方（委托人）：${clientName}
乙方（受托人）：${firmName}

鉴于甲方因${matterDescription}事宜，委托乙方提供法律服务，经双方协商一致，订立本合同。

第一条 委托事项
甲方委托乙方就${matterName}事项提供法律服务。

第二条 服务内容
1. 提供法律咨询；
2. 代理参与诉讼/仲裁/调解；
3. 起草、审查法律文书；
4. 其他双方约定的法律服务。

第三条 服务费用
本合同项下法律服务费用为人民币${totalAmount}元整。

第四条 付款方式
${paymentTerms}

第五条 合同期限
本合同自${effectiveDate}起至${expiryDate}止。

第六条 双方权利义务
（一）甲方权利义务
1. 如实向乙方陈述案件事实，提供相关证据材料；
2. 按约定支付法律服务费用；
3. 配合乙方开展法律服务工作。

（二）乙方权利义务
1. 依法维护甲方合法权益；
2. 保守甲方商业秘密和个人隐私；
3. 及时向甲方通报案件进展情况。

第七条 违约责任
任何一方违反本合同约定，应承担相应违约责任。

第八条 争议解决
本合同履行过程中发生争议，双方协商解决；协商不成的，提交乙方所在地人民法院诉讼解决。

甲方（签章）：                    乙方（签章）：
日期：${signDate}                 日期：${signDate}',
'[{"title":"服务范围","content":"提供法律咨询、代理诉讼、起草审查法律文书等"},{"title":"保密条款","content":"乙方对甲方的商业秘密和个人隐私负有保密义务"},{"title":"免责条款","content":"因甲方提供虚假信息导致的损失由甲方自行承担"}]',
'适用于一般法律服务的标准合同模板', 'ACTIVE', 1),

('CT-002', '常年法律顾问合同', 'RETAINER', 'FIXED',
'常年法律顾问合同

甲方（委托人）：${clientName}
乙方（受托人）：${firmName}

第一条 服务期限
自${effectiveDate}起至${expiryDate}止，为期一年。

第二条 服务内容
1. 日常法律咨询；
2. 合同审查与起草；
3. 法律风险评估；
4. 参与重大商务谈判；
5. 代理诉讼/仲裁（另行收费）。

第三条 服务费用
年度顾问费人民币${totalAmount}元整。

第四条 付款方式
${paymentTerms}

第五条 服务方式
1. 电话、邮件咨询不限次数；
2. 每月提供不少于${serviceHours}小时现场服务；
3. 紧急事项24小时响应。

甲方（签章）：                    乙方（签章）：
日期：${signDate}                 日期：${signDate}',
'[{"title":"服务响应","content":"常规咨询24小时内响应，紧急事项即时响应"},{"title":"续约条款","content":"合同到期前30日，双方可协商续约事宜"}]',
'适用于企业常年法律顾问服务', 'ACTIVE', 2),

('CT-003', '民事/行政委托代理合同', 'LITIGATION', 'FIXED',
'委 托 代 理 合 同

（${contractYear}）${contractNo}代字第${contractSeq}号

委托人：${clientName}
住所地：${clientAddress}                     电话：${clientPhone}
受托人：${firmName}
法定代表人（负责人）：${firmLegalRep}        职务：主任
住所地：${firmAddress}，电话：${firmPhone}。

委托人因与${opposingParty}${causeOfAction}纠纷一案，委托人与受托人就委托代理事宜，经平等协商，自愿订立本合同，以供双方共同遵守执行。

一、受托人接受委托后，指派律师${lawyerNames}、律师助理${assistantNames}为委托人的${trialStage}代理人。

二、代理权限为：${authorizationType}（一般代理或特别代理）。
一般代理的权限为：拟写诉讼（仲裁）文书、起诉（立案）、应诉、调查取证、参加庭审、签收诉讼（仲裁）法律文书等诉讼活动。
特别代理的权限（根据相应的代理阶段据实确定）为：承认、变更、放弃诉讼（仲裁）请求，决定是否调解、和解并签订调解、和解协议，提起反诉或上诉，代为申请执行并收付执行款物，以及行使与案件有关的其他诉讼权利。

三、根据《律师服务收费管理办法》及相关规定，委托人应当向受托人支付的代理费大写${totalAmountCN}（¥：${totalAmount}），定于${paymentDeadline}前付清。
委托人应当以直接汇入受托人单位银行账户或支付给持有受托人单位签章票据（收据或发票）的人员的方式付费，不得私自向受托人单位的律师、律师助理或其他工作人员支付任何费用。否则，由此产生的法律后果与受托人单位无关。

四、委托人必须如实全面地向受托人陈述案情，提供证据及证据线索，并不得有下列行为：
1、隐瞒案情；2、伪造证据；3、虚假陈述；4、利用受托人提供法律服务谋取非法利益的其他行为。
受托人发现委托人有前列情形之一时，有权单方终止本合同的履行，所收委托人的一切费用概不予退还。

五、受托人必须尽职尽责，依法维护委托人利益，并不得有下列行为：
1、与委托人的对方当事人及其代理人恶意串通，损害委托人的利益；2、接受委托人的对方当事人及其代理人的吃请或其他贿赂；3、指派其他律师担任同一案件中对方当事人的代理人；4、泄露委托人的商业秘密或个人隐私；5、其他损害委托人合法利益的行为。

六、委托人不得以下列理由解除本合同：
1、与对方当事人自行协商、和解；2、法院主持调解结案；3、委托人自己撤诉或放弃权利；4、对方当事人撤诉或主动履行义务；5、未经法院判决应当解除合同的其他理由。

七、受托人在履行本合同过程中，如出现下列情形，委托人可以解除本合同并要求受托人退费：
1、经人民法院判决确认，受托人实施了损害委托人合法权益的行为；2、受托人无正当理由明确表示不履行代理义务；3、受托人代理义务未履行完毕之前，丧失执业资格。

八、委托人不得以下列理由要求受托人退费：
1、案件败诉；2、裁判文书确定的利益未实现或未能达到委托人的预期目的；3、案件出现本合同第六条规定的情形；4、未经法院判决受托人应当退费的其他理由。

九、因履行本合同过程中引起的或本合同有关的任何争议，不能协商解决的，双方共同选择以下第${disputeResolution}种方式解决：
1.由受托人住所地法院管辖。2.提请${arbitrationCommittee}仲裁委员会按该会仲裁规则进行仲裁。

十、本合同自双方签字和盖章之日起生效。

十一、本合同未尽事宜，由双方本着平等、自愿、合法的原则，协商解决。

十二、本合同一式二份，双方各持一份。

十三、特别约定：${specialTerms}

委托人签章：                            受托人签章：
法定代表人（负责人）：                   负责人签章：
                           ${signDate}',
'[{"title":"代理权限说明","content":"一般代理：拟写诉讼文书、起诉、应诉、调查取证、参加庭审、签收法律文书。特别代理：承认、变更、放弃诉讼请求，调解和解，提起反诉或上诉，申请执行并收付执行款物"},{"title":"委托人义务","content":"如实陈述案情，提供证据，不得隐瞒案情、伪造证据、虚假陈述"},{"title":"受托人义务","content":"尽职尽责，依法维护委托人利益，保守商业秘密和个人隐私"},{"title":"费用支付","content":"应直接汇入受托人单位银行账户或支付给持有受托人单位签章票据的人员"}]',
'适用于民事案件和行政案件的委托代理合同，符合律师协会规范', 'ACTIVE', 3),

('CT-004', '非诉项目合同', 'NON_LITIGATION', 'HOURLY',
'非诉讼法律服务合同

甲方（委托人）：${clientName}
乙方（受托人）：${firmName}

第一条 项目名称
${matterName}

第二条 服务内容
${matterDescription}

第三条 收费方式
本项目采用计时收费方式：
1. 合伙人：人民币${partnerRate}元/小时；
2. 资深律师：人民币${seniorRate}元/小时；
3. 律师助理：人民币${assistantRate}元/小时。

第四条 费用预估
预估总费用人民币${totalAmount}元（实际以工时记录为准）。

第五条 付款方式
${paymentTerms}

甲方（签章）：                    乙方（签章）：
日期：${signDate}                 日期：${signDate}',
'[{"title":"工时记录","content":"乙方应如实记录工作时间，定期向甲方提供工时报告"},{"title":"费用上限","content":"如实际费用超出预估20%，乙方应提前告知甲方"}]',
'适用于非诉讼项目，按工时计费', 'ACTIVE', 4),

('CT-005', '刑事案件委托代理合同', 'LITIGATION', 'FIXED',
'委 托 代 理 合 同

（${contractYear}）刑辩字第${contractSeq}号

委托人：${clientName}                           电话：${clientPhone}
受托人：${firmName}
法定代表人（负责人）：${firmLegalRep}        职务：主任
住所地：${firmAddress}，电话：${firmPhone}。

因${defendantName}涉嫌${criminalCharge}一案，委托人与受托人就委托辩护（代理）事宜，经平等协商，自愿订立本合同，以供双方共同遵守。

一、受托人接受委托后，指派律师${lawyerNames}担任被告人（犯罪嫌疑人、受害人）${defendantName}的${defenseStage}。
A、侦查阶段的律师；B、审查起诉阶段的辩护人；C、一审辩护人；D、二审辩护人；E、死刑复核阶段的辩护人；F、再审辩护人；

二、根据《律师服务收费管理办法》及相关规定，委托人应当向受托人支付辩护费（代理）${totalAmountCN}元（￥${totalAmount}），定于${paymentDeadline}前付清。
委托人应当以直接汇入受托人单位银行账户或支付给持有受托人单位签章票据（收据或发票）的人员的方式付费，不得私自向受托人单位的律师、律师助理或其他工作人员支付任何费用。否则，由此产生的法律后果与受托人单位无关。

三、受托人必须尽职尽责，依法维护被告人（犯罪嫌疑人、受害人）的合法权益，并及时与委托方沟通联系。

四、如出现被告人（犯罪嫌疑人）拒绝受托人指派的律师为其辩护（代理）的情况，除非受托人实施了损害被告人（犯罪嫌疑人）的合法权益的行为，委托人不得要求受托人退还所交费用。受托人是否实施了损害被告人合法权益的行为，须由人民法院判决确认。

五、委托人不得以下列理由要求受托人退还费用：
1、侦查部门撤销案件；2、审查起诉部门决定对犯罪嫌疑人不起诉；3、公诉机关撤回对案件的起诉；4、被告人（犯罪嫌疑人）被取保候审；5、案件的处理结果未达到或未能完全达到委托人的预期目的；6、委托人单方解除本合同；7、被告人（犯罪嫌疑人）拒绝委托人指派的律师为其辩护（代理）。8、其他非经法院判决确认退费的理由。

六、因履行本合同所发生的不能协商解决的争议，由受托人住所地法院管辖。

七、本合同自双方签字、盖章之日起生效。

八、本合同未尽事宜，由双方本着平等、自愿、合法的原则，协商解决。

九、本合同一式二份，双方各持一份。

十、特别约定：${specialTerms}

委托人签章：                                受托人签章：
法定代表人（负责人）：                       负责人签章：
${signDate}',
'[{"title":"辩护阶段说明","content":"A-侦查阶段律师、B-审查起诉阶段辩护人、C-一审辩护人、D-二审辩护人、E-死刑复核阶段辩护人、F-再审辩护人"},{"title":"受托人义务","content":"尽职尽责，依法维护被告人（犯罪嫌疑人、受害人）的合法权益，及时与委托方沟通联系"},{"title":"不予退费情形","content":"侦查部门撤销案件、不起诉、撤回起诉、取保候审、结果未达预期、委托人单方解除、被告人拒绝辩护等"},{"title":"费用支付","content":"应直接汇入受托人单位银行账户或支付给持有受托人单位签章票据的人员"}]',
'适用于刑事案件的辩护代理合同，包含侦查、审查起诉、一审、二审、死刑复核、再审等阶段', 'ACTIVE', 5);

COMMIT;

