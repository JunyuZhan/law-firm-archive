-- =====================================================
-- 律师事务所管理系统 - 示例数据初始化
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-11
-- 描述: 部署后的示例数据，用于演示和测试
-- 
-- 数据依赖关系：
-- 1. sys_department (部门) - 基础数据
-- 2. sys_user (用户) - 基础数据
-- 3. crm_client (客户) - 依赖用户（案源人、负责律师）
-- 4. finance_contract (合同) - 依赖客户、用户
-- 5. matter (项目) - 依赖客户、合同、用户
-- 6. matter_participant (项目成员) - 依赖项目、用户
-- 7. task (任务) - 依赖项目、用户
-- =====================================================

-- =====================================================
-- 1. 补充部门数据
-- =====================================================
-- 现有部门：诉讼部(1)→第一组(4)/第二组(5), 财务部(2), 行政部(3)
-- 新增：非诉部(6)→知产组(7)

INSERT INTO sys_department (id, name, parent_id, leader_id, sort_order, status, created_at, updated_at)
VALUES 
  (6, '非诉部', 0, 2, 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7, '知产组', 6, 4, 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval('sys_department_id_seq', COALESCE((SELECT MAX(id) FROM sys_department), 1));

-- =====================================================
-- 2. 示例客户数据
-- =====================================================
-- 客户类型：INDIVIDUAL-个人, ENTERPRISE-企业
-- 客户状态：POTENTIAL-潜在, ACTIVE-正式
-- 客户级别：A-重要, B-普通, C-一般

INSERT INTO crm_client (id, client_no, name, client_type, credit_code, id_card, legal_representative, 
  registered_address, contact_person, contact_phone, contact_email, industry, source, level, category, 
  status, originator_id, responsible_lawyer_id, first_cooperation_date, remark, created_at, updated_at, created_by, updated_by, deleted)
VALUES
  -- 企业客户
  (101, 'C2026010001', '北京科技创新有限公司', 'ENTERPRISE', '91110108MA01XXXXX1', NULL, '李明', 
   '北京市海淀区中关村大街1号', '王秘书', '13800138001', 'wang@bjtech.com', '信息技术', '老客户介绍', 'A', 'VIP',
   'ACTIVE', 3, 3, '2025-06-15', '重要战略客户，常年法律顾问', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  (102, 'C2026010002', '上海金融投资集团', 'ENTERPRISE', '91310000MA01XXXXX2', NULL, '张华', 
   '上海市浦东新区陆家嘴金融中心', '刘助理', '13800138002', 'liu@shfinance.com', '金融', '主动拜访', 'A', 'VIP',
   'ACTIVE', 2, 4, '2025-08-20', '金融领域重点客户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  (103, 'C2026010003', '广州制造业有限公司', 'ENTERPRISE', '91440100MA01XXXXX3', NULL, '陈建国', 
   '广州市天河区科技园区', '赵经理', '13800138003', 'zhao@gzmanuf.com', '制造业', '网络推广', 'B', 'NORMAL',
   'ACTIVE', 3, 3, '2025-10-01', '制造业纠纷案件客户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  (104, 'C2026010004', '深圳互联网科技公司', 'ENTERPRISE', '91440300MA01XXXXX4', NULL, '周伟', 
   '深圳市南山区科技园', '孙总监', '13800138004', 'sun@szinternet.com', '互联网', '同行介绍', 'B', 'NORMAL',
   'ACTIVE', 4, 4, '2025-11-15', '知识产权案件客户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 个人客户
  (105, 'C2026010005', '李建军', 'INDIVIDUAL', NULL, '110101198501010011', NULL, 
   NULL, '李建军', '13900139001', 'lijj@email.com', NULL, '朋友介绍', 'B', 'NORMAL',
   'ACTIVE', 3, 3, '2025-12-01', '劳动争议案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  (106, 'C2026010006', '王美丽', 'INDIVIDUAL', NULL, '110101199002020022', NULL, 
   NULL, '王美丽', '13900139002', 'wangml@email.com', NULL, '老客户介绍', 'C', 'NORMAL',
   'ACTIVE', 7, 3, '2026-01-05', '婚姻家事案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 潜在客户
  (107, 'C2026010007', '杭州电商有限公司', 'ENTERPRISE', '91330100MA01XXXXX7', NULL, '吴强', 
   '杭州市余杭区梦想小镇', '钱经理', '13800138007', 'qian@hzec.com', '电子商务', '展会认识', 'B', 'POTENTIAL',
   'POTENTIAL', 4, 4, NULL, '正在洽谈中', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('crm_client_id_seq', COALESCE((SELECT MAX(id) FROM crm_client), 1));

-- =====================================================
-- 3. 示例合同数据
-- =====================================================
-- 合同状态：DRAFT-草稿, PENDING-待审批, ACTIVE-生效中, REJECTED-已拒绝, TERMINATED-已终止, COMPLETED-已完成, EXPIRED-已过期
-- 收费类型：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理

INSERT INTO finance_contract (id, contract_no, name, client_id, contract_type, fee_type, total_amount, currency,
  sign_date, effective_date, expiry_date, status, signer_id, department_id, 
  content, remark, created_at, updated_at, created_by, updated_by, deleted)
VALUES
  -- 北京科技创新公司 - 常年法律顾问合同
  (101, '2025民代字第0001号', '常年法律顾问服务合同', 101, 'SERVICE', 'FIXED', 120000.00, 'CNY',
   '2025-06-20', '2025-07-01', '2026-06-30', 'ACTIVE', 3, 1,
   '为北京科技创新公司提供常年法律顾问服务', '分期付款，每季度3万', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 上海金融投资集团 - 股权并购项目
  (102, '2025民代字第0002号', '股权并购法律服务合同', 102, 'SERVICE', 'FIXED', 500000.00, 'CNY',
   '2025-08-25', '2025-09-01', '2026-03-31', 'ACTIVE', 4, 1,
   '上海金融投资集团并购项目法律服务', '重大项目', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 广州制造业公司 - 劳动仲裁代理
  (103, '2025民代字第0003号', '劳动争议代理合同', 103, 'LITIGATION', 'CONTINGENCY', 30000.00, 'CNY',
   '2025-10-10', '2025-10-10', '2026-04-10', 'ACTIVE', 3, 4,
   '代理广州制造业公司劳动争议仲裁案件', '风险代理，胜诉后收费', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 深圳互联网公司 - 知识产权维权
  (104, '2025知代字第0001号', '知识产权维权代理合同', 104, 'LITIGATION', 'CONTINGENCY', 80000.00, 'CNY',
   '2025-11-20', '2025-11-20', '2026-05-20', 'ACTIVE', 4, 4,
   '代理深圳互联网公司专利侵权维权', '按件收费', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 个人客户 - 劳动争议
  (105, '2025民代字第0004号', '劳动争议代理合同', 105, 'LITIGATION', 'FIXED', 15000.00, 'CNY',
   '2025-12-05', '2025-12-05', '2026-06-05', 'ACTIVE', 3, 4,
   '代理李建军劳动仲裁案件', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 个人客户 - 离婚诉讼
  (106, '2026民代字第0001号', '婚姻家事代理合同', 106, 'LITIGATION', 'FIXED', 20000.00, 'CNY',
   '2026-01-08', '2026-01-08', '2026-07-08', 'ACTIVE', 3, 4,
   '代理王美丽离婚诉讼案件', '刚签约', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('finance_contract_id_seq', COALESCE((SELECT MAX(id) FROM finance_contract), 1));

-- =====================================================
-- 4. 示例项目（案件）数据
-- =====================================================
-- 项目状态：ACTIVE-进行中, PENDING_CLOSE-待结案, CLOSED-已结案, ARCHIVED-已归档
-- 项目类型：LITIGATION-诉讼, NON_LITIGATION-非诉, ADVISORY-咨询顾问
-- 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政

INSERT INTO matter (id, matter_no, name, matter_type, case_type, cause_of_action, client_id, 
  opposing_party, description, status, originator_id, lead_lawyer_id, department_id,
  fee_type, estimated_fee, filing_date, expected_end_date, contract_id,
  remark, created_at, updated_at, created_by, updated_by, deleted)
VALUES
  -- 北京科技创新公司 - 法律顾问项目
  (101, 'M2025001', '北京科技创新公司法律顾问', 'ADVISORY', NULL, NULL, 101,
   NULL, '为北京科技创新公司提供日常法律咨询、合同审查等服务', 'ACTIVE', 3, 3, 1,
   'FIXED', 120000.00, '2025-07-01', '2026-06-30', 101,
   '常年顾问项目', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 上海金融投资集团 - 并购项目
  (102, 'M2025002', '上海金融集团并购项目', 'NON_LITIGATION', NULL, NULL, 102,
   '目标公司', '上海金融投资集团收购目标公司股权项目，涉及尽职调查、交易文件起草等', 'ACTIVE', 2, 4, 1,
   'FIXED', 500000.00, '2025-09-01', '2026-03-31', 102,
   '重大非诉项目', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 广州制造业公司 - 劳动仲裁
  (103, 'M2025003', '广州制造业公司劳动争议案', 'LITIGATION', 'CIVIL', '劳动争议', 103,
   '张某等10人', '代理广州制造业公司应对10名员工的劳动仲裁案件', 'ACTIVE', 3, 3, 4,
   'CONTINGENCY', 30000.00, '2025-10-15', '2026-04-15', 103,
   '群体性劳动争议', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 深圳互联网公司 - 专利维权
  (104, 'M2025004', '深圳互联网公司专利侵权案', 'LITIGATION', 'CIVIL', '侵害发明专利权纠纷', 104,
   '某科技公司', '代理深圳互联网公司起诉某科技公司侵害发明专利权', 'ACTIVE', 4, 4, 4,
   'CONTINGENCY', 80000.00, '2025-11-25', '2026-05-25', 104,
   '知识产权诉讼', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 李建军 - 劳动仲裁
  (105, 'M2025005', '李建军劳动争议案', 'LITIGATION', 'CIVIL', '劳动争议', 105,
   '某公司', '代理李建军与某公司的劳动仲裁案件，主张违法解除赔偿', 'ACTIVE', 3, 3, 4,
   'FIXED', 15000.00, '2025-12-10', '2026-03-10', 105,
   NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 王美丽 - 离婚诉讼
  (106, 'M2026001', '王美丽离婚诉讼案', 'LITIGATION', 'CIVIL', '离婚纠纷', 106,
   '赵某', '代理王美丽离婚诉讼，涉及财产分割、子女抚养权', 'ACTIVE', 7, 3, 4,
   'FIXED', 20000.00, '2026-01-10', '2026-04-10', 106,
   '婚姻家事案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('matter_id_seq', COALESCE((SELECT MAX(id) FROM matter), 1));

-- =====================================================
-- 5. 项目客户关联（多客户支持）
-- =====================================================

INSERT INTO matter_client (id, matter_id, client_id, client_role, is_primary, created_at, updated_at)
VALUES
  (101, 101, 101, 'CLIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (102, 102, 102, 'CLIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (103, 103, 103, 'CLIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (104, 104, 104, 'CLIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (105, 105, 105, 'CLIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (106, 106, 106, 'CLIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval('matter_client_id_seq', COALESCE((SELECT MAX(id) FROM matter_client), 1));

-- =====================================================
-- 6. 项目成员数据
-- =====================================================
-- 角色：LEAD-主办律师, ASSISTANT-协办律师, TRAINEE-实习律师

INSERT INTO matter_participant (id, matter_id, user_id, role, join_date, created_at, updated_at)
VALUES
  -- 项目101: 北京科技创新公司法律顾问
  (101, 101, 3, 'LEAD', '2025-07-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (102, 101, 7, 'TRAINEE', '2025-07-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  
  -- 项目102: 上海金融集团并购项目
  (103, 102, 4, 'LEAD', '2025-09-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (104, 102, 3, 'ASSISTANT', '2025-09-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (105, 102, 7, 'TRAINEE', '2025-09-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  
  -- 项目103: 广州制造业公司劳动争议案
  (106, 103, 3, 'LEAD', '2025-10-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  
  -- 项目104: 深圳互联网公司专利侵权案
  (107, 104, 4, 'LEAD', '2025-11-25', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  
  -- 项目105: 李建军劳动争议案
  (108, 105, 3, 'LEAD', '2025-12-10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  
  -- 项目106: 王美丽离婚诉讼案
  (109, 106, 3, 'LEAD', '2026-01-10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (110, 106, 7, 'TRAINEE', '2026-01-10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval('matter_participant_id_seq', COALESCE((SELECT MAX(id) FROM matter_participant), 1));

-- =====================================================
-- 7. 示例任务数据
-- =====================================================
-- 任务状态：TODO-待处理, IN_PROGRESS-进行中, DONE-已完成, CANCELLED-已取消
-- 优先级：HIGH-高, MEDIUM-中, LOW-低

INSERT INTO task (id, task_no, title, description, matter_id, assignee_id,
  priority, status, due_date, start_date, completed_at, created_by, created_at, updated_at, deleted)
VALUES
  -- 项目101任务
  (101, 'T2025001', '审查供应商合同', '审查北京科技创新公司与供应商签订的采购合同', 101, 3,
   'MEDIUM', 'DONE', '2025-08-15', '2025-08-01', '2025-08-10 10:00:00', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (102, 'T2025002', '起草保密协议模板', '为北京科技创新公司起草员工保密协议模板', 101, 7,
   'LOW', 'DONE', '2025-09-01', '2025-08-20', '2025-08-28 14:00:00', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (103, 'T2026001', '劳动合同审查', '审查北京科技创新公司新版劳动合同', 101, 3,
   'MEDIUM', 'IN_PROGRESS', '2026-01-20', '2026-01-10', NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 项目102任务
  (104, 'T2025003', '目标公司尽职调查', '对目标公司进行法律尽职调查', 102, 4,
   'HIGH', 'DONE', '2025-10-15', '2025-09-10', '2025-10-12 16:00:00', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (105, 'T2025004', '起草股权转让协议', '起草股权转让协议初稿', 102, 4,
   'HIGH', 'IN_PROGRESS', '2026-01-31', '2026-01-05', NULL, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (106, 'T2025005', '整理交割文件清单', '整理交割所需全部法律文件清单', 102, 7,
   'MEDIUM', 'TODO', '2026-02-15', NULL, NULL, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 项目103任务
  (107, 'T2025006', '准备仲裁答辩材料', '准备劳动仲裁答辩状及证据', 103, 3,
   'HIGH', 'DONE', '2025-11-01', '2025-10-20', '2025-10-30 09:00:00', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (108, 'T2025007', '出庭参加仲裁庭审', '代理公司出庭参加劳动仲裁庭审', 103, 3,
   'HIGH', 'IN_PROGRESS', '2026-01-15', '2026-01-10', NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 项目104任务
  (109, 'T2025008', '专利侵权分析报告', '对被控侵权产品进行专利侵权分析', 104, 4,
   'HIGH', 'DONE', '2025-12-15', '2025-12-01', '2025-12-13 11:00:00', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (110, 'T2025009', '起草起诉状', '起草专利侵权诉讼起诉状', 104, 4,
   'HIGH', 'IN_PROGRESS', '2026-01-25', '2026-01-15', NULL, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 项目105任务
  (111, 'T2025010', '收集劳动关系证据', '收集证明劳动关系存在的证据材料', 105, 3,
   'HIGH', 'DONE', '2025-12-20', '2025-12-12', '2025-12-18 15:00:00', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 项目106任务
  (112, 'T2026002', '调查婚姻财产状况', '调查双方婚姻存续期间的共同财产', 106, 3,
   'HIGH', 'TODO', '2026-01-25', NULL, NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (113, 'T2026003', '起草离婚诉讼起诉状', '起草离婚诉讼起诉状', 106, 7,
   'MEDIUM', 'TODO', '2026-02-01', NULL, NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('task_id_seq', COALESCE((SELECT MAX(id) FROM task), 1));

-- =====================================================
-- 8. 已归档卷宗数据
-- =====================================================
-- 档案状态：PENDING-待入库, PENDING_APPROVAL-待审批, STORED-已入库, BORROWED-已借出, DESTROYED-已销毁

-- 先添加档案库位
INSERT INTO archive_location (id, location_code, location_name, room, cabinet, shelf, total_capacity, used_capacity, status, created_at, updated_at)
VALUES
  (1, 'LOC-A-001', '档案室A区1号柜', 'A区', '1号柜', '第1层', 100, 3, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'LOC-A-002', '档案室A区2号柜', 'A区', '2号柜', '第1层', 100, 0, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval('archive_location_id_seq', COALESCE((SELECT MAX(id) FROM archive_location), 1));

-- 已入库的档案
INSERT INTO archive (id, archive_no, matter_id, archive_name, archive_type, matter_no, matter_name, client_name, 
  main_lawyer_name, case_close_date, volume_count, page_count, location_id, box_no, retention_period, 
  has_electronic, status, stored_by, stored_at, remarks, created_by, created_at, updated_at, deleted)
VALUES
  -- 已完成的法律顾问项目档案
  (101, 'AR2025-0001', 101, '北京科技创新公司2025年度法律顾问档案', 'ADVISORY', 'M2025001', 
   '北京科技创新公司法律顾问', '北京科技创新有限公司', '张律师', '2025-12-31', 
   2, 150, 1, 'BOX-2025-001', '10_YEARS', true, 'STORED', 1, '2025-12-31 10:00:00',
   '2025年度常年法律顾问项目档案', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 已结案的劳动争议案档案
  (102, 'AR2025-0002', 103, '广州制造业劳动争议案档案', 'LITIGATION', 'M2025003',
   '广州制造业公司劳动争议案', '广州制造业有限公司', '张律师', '2025-11-15',
   3, 280, 1, 'BOX-2025-002', '30_YEARS', true, 'STORED', 1, '2025-11-20 14:30:00',
   '群体性劳动争议案件，已调解结案', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 已结案的个人劳动仲裁案档案
  (103, 'AR2025-0003', 105, '李建军劳动争议案档案', 'LITIGATION', 'M2025005',
   '李建军劳动争议案', '李建军', '张律师', '2025-12-20',
   1, 45, 1, 'BOX-2025-003', '10_YEARS', true, 'STORED', 1, '2025-12-25 09:00:00',
   '个人劳动仲裁案件，胜诉结案', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('archive_id_seq', COALESCE((SELECT MAX(id) FROM archive), 1));

-- =====================================================
-- 完成提示
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '====================================================';
  RAISE NOTICE '示例数据初始化完成！';
  RAISE NOTICE '----------------------------------------------------';
  RAISE NOTICE '新增数据统计：';
  RAISE NOTICE '  - 部门: 2个（非诉部、知产组）';
  RAISE NOTICE '  - 客户: 7个（5企业 + 2个人）';
  RAISE NOTICE '  - 合同: 6个';
  RAISE NOTICE '  - 项目: 6个（4诉讼 + 1非诉 + 1顾问）';
  RAISE NOTICE '  - 项目成员: 10条';
  RAISE NOTICE '  - 任务: 13个';
  RAISE NOTICE '  - 档案库位: 2个';
  RAISE NOTICE '  - 已归档卷宗: 3个（已入库）';
  RAISE NOTICE '----------------------------------------------------';
  RAISE NOTICE '数据关系说明：';
  RAISE NOTICE '  - 张律师(lawyer1, id=3): 主办4个项目';
  RAISE NOTICE '  - 李团长(leader, id=4): 主办2个项目';
  RAISE NOTICE '  - 陈实习(trainee, id=7): 参与4个项目';
  RAISE NOTICE '====================================================';
END $$;
