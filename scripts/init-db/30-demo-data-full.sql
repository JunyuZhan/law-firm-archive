-- =====================================================
-- 完整演示数据脚本（整合版）
-- =====================================================
-- 版本: 2.0.0
-- 日期: 2026-01-15
-- 描述: 整合所有模块的演示数据
-- 说明: 此脚本包含以下演示数据：
--   - 基础数据：客户、合同、项目、任务、归档
--   - 知识库：分类、文章
--   - 日程：庭审、会议、培训
--   - 人力资源：考勤、培训记录
--   - 行政管理：会议室、印章、用印申请
--   - 财务：收费、付款、发票、工时
-- =====================================================


-- =====================================================
-- 合并自: 30-demo-data.sql
-- =====================================================

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
  
  -- 广州制造业公司 - 劳动仲裁（已结案）
  (103, 'M2025003', '广州制造业公司劳动争议案', 'LITIGATION', 'CIVIL', '劳动争议', 103,
   '张某等10人', '代理广州制造业公司应对10名员工的劳动仲裁案件', 'CLOSED', 3, 3, 4,
   'CONTINGENCY', 30000.00, '2025-10-15', '2026-04-15', 103,
   '群体性劳动争议，已调解结案', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 深圳互联网公司 - 专利维权
  (104, 'M2025004', '深圳互联网公司专利侵权案', 'LITIGATION', 'CIVIL', '侵害发明专利权纠纷', 104,
   '某科技公司', '代理深圳互联网公司起诉某科技公司侵害发明专利权', 'ACTIVE', 4, 4, 4,
   'CONTINGENCY', 80000.00, '2025-11-25', '2026-05-25', 104,
   '知识产权诉讼', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  -- 李建军 - 劳动仲裁（已结案）
  (105, 'M2025005', '李建军劳动争议案', 'LITIGATION', 'CIVIL', '劳动争议', 105,
   '某公司', '代理李建军与某公司的劳动仲裁案件，主张违法解除赔偿', 'CLOSED', 3, 3, 4,
   'FIXED', 15000.00, '2025-12-10', '2026-03-10', 105,
   '个人劳动仲裁案件，胜诉结案', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
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
  RAISE NOTICE '  - 项目: 6个（2已结案 + 4进行中）';
  RAISE NOTICE '  - 项目成员: 10条';
  RAISE NOTICE '  - 任务: 13个';
  RAISE NOTICE '  - 档案库位: 2个';
  RAISE NOTICE '  - 已归档卷宗: 3个（已入库）';
  RAISE NOTICE '----------------------------------------------------';
  RAISE NOTICE '数据关系说明：';
  RAISE NOTICE '  - 张律师(lawyer1, id=3): 主办4个项目';
  RAISE NOTICE '  - 李团长(leader, id=4): 主办2个项目';
  RAISE NOTICE '  - 陈实习(trainee, id=7): 参与4个项目';
  RAISE NOTICE '  - 项目103(广州制造业)、105(李建军)已结案';
  RAISE NOTICE '====================================================';
END $$;

-- =====================================================
-- 合并自: 40-knowledge-demo.sql
-- =====================================================

-- =====================================================
-- 律师事务所管理系统 - 知识库示例数据
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-14
-- 描述: 知识库文章、案例、法规示例数据
-- =====================================================

-- =====================================================
-- 1. 知识库分类
-- =====================================================

INSERT INTO kb_category (id, name, parent_id, sort_order, description, created_at, updated_at)
VALUES
  (101, '合同法律知识', 0, 1, '合同起草、审查、纠纷相关知识', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (102, '劳动法律知识', 0, 2, '劳动关系、劳动争议相关知识', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (103, '知识产权', 0, 3, '专利、商标、著作权相关知识', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (104, '公司法务', 0, 4, '公司治理、股权、并购相关知识', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (105, '诉讼实务', 0, 5, '诉讼技巧、庭审策略相关知识', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (106, '合同审查要点', 101, 1, '各类合同审查要点汇总', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (107, '劳动仲裁实务', 102, 1, '劳动仲裁程序与技巧', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (108, '专利诉讼', 103, 1, '专利侵权诉讼实务', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval('kb_category_id_seq', COALESCE((SELECT MAX(id) FROM kb_category), 1));

-- =====================================================
-- 2. 知识库文章
-- =====================================================

INSERT INTO kb_article (id, title, content, category_id, article_type, author_id, status, 
  view_count, like_count, tags, created_at, updated_at, published_at, deleted)
VALUES
  -- 合同法律知识
  (101, '买卖合同审查要点清单', 
   E'## 买卖合同审查要点\n\n### 一、主体资格审查\n1. 审查出卖人是否有权处分标的物\n2. 核实双方营业执照、授权委托书\n3. 确认签约代表身份及权限\n\n### 二、标的物条款\n1. 品名、规格、型号描述清晰\n2. 数量、质量标准明确\n3. 交付方式、地点、时间约定\n\n### 三、价款条款\n1. 单价、总价计算准确\n2. 付款方式、时间节点\n3. 发票类型及税率\n\n### 四、违约责任\n1. 逾期交货违约金\n2. 质量问题处理\n3. 逾期付款利息\n\n### 五、争议解决\n1. 协商条款\n2. 仲裁或诉讼选择\n3. 管辖法院约定',
   106, 'ARTICLE', 3, 'PUBLISHED', 156, 23, '合同审查,买卖合同,实务指南', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (102, '服务合同常见风险及防范', 
   E'## 服务合同风险防范指南\n\n### 常见风险点\n\n#### 1. 服务范围不清\n- 问题：服务边界模糊，易产生争议\n- 防范：详细列明服务内容清单，明确"包含"与"不包含"\n\n#### 2. 验收标准缺失\n- 问题：无法判断服务是否合格\n- 防范：制定量化的验收标准和验收流程\n\n#### 3. 知识产权归属\n- 问题：服务成果归属不明\n- 防范：明确约定知识产权归属及使用许可\n\n#### 4. 保密条款\n- 问题：商业秘密泄露风险\n- 防范：完善保密条款，约定违约责任\n\n### 示例条款模板\n\n```\n第X条 服务内容\n甲方委托乙方提供以下服务：\n1. ...\n2. ...\n以下事项不在本合同服务范围内：\n1. ...\n```',
   106, 'ARTICLE', 3, 'PUBLISHED', 98, 15, '服务合同,风险防范,合同条款', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 劳动法律知识
  (103, '劳动合同解除的法律风险', 
   E'## 劳动合同解除法律风险分析\n\n### 一、用人单位单方解除\n\n#### 1. 试用期解除（第39条第1项）\n- 条件：不符合录用条件\n- 风险点：录用条件需事先告知并有书面确认\n- 建议：入职时签署录用条件确认书\n\n#### 2. 严重违纪解除（第39条第2项）\n- 条件：严重违反规章制度\n- 风险点：规章制度需经民主程序制定并公示\n- 建议：完善规章制度制定程序\n\n#### 3. 无过失性解除（第40条）\n- 条件：医疗期满、不胜任工作、客观情况变化\n- 风险点：需提前30日通知或支付代通知金\n- 建议：做好书面通知及证据保留\n\n### 二、经济补偿计算\n\n| 情形 | 标准 |\n|------|------|\n| N | 工作年限×月工资 |\n| N+1 | N + 代通知金 |\n| 2N | 违法解除赔偿 |\n\n### 三、风险防控建议\n\n1. 建立完善的劳动规章制度\n2. 做好日常管理证据保留\n3. 解除前进行法律风险评估',
   102, 'ARTICLE', 3, 'PUBLISHED', 234, 45, '劳动合同,解除,经济补偿', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (104, '劳动仲裁案件代理要点', 
   E'## 劳动仲裁案件代理实务\n\n### 一、仲裁时效\n\n1. **一般时效**：1年，从知道或应当知道权利被侵害之日起算\n2. **特殊情形**：\n   - 劳动关系存续期间因拖欠劳动报酬发生争议，不受1年限制\n   - 劳动关系终止的，自终止之日起1年内\n\n### 二、证据收集\n\n#### 必备证据\n1. 劳动合同或事实劳动关系证明\n2. 工资支付记录\n3. 社保缴纳记录\n4. 考勤记录\n5. 离职相关文件\n\n#### 举证责任分配\n| 争议类型 | 举证责任方 |\n|---------|------------|\n| 工资支付 | 用人单位 |\n| 加班事实 | 劳动者（初步） |\n| 解除原因 | 用人单位 |\n\n### 三、庭审技巧\n\n1. 陈述简洁，突出诉求要点\n2. 质证时关注证据三性\n3. 善用发问技巧还原事实\n4. 辩论阶段法律适用准确',
   107, 'ARTICLE', 3, 'PUBLISHED', 189, 38, '劳动仲裁,证据,诉讼技巧', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 知识产权
  (105, '专利侵权判定方法', 
   E'## 专利侵权判定实务指南\n\n### 一、侵权判定原则\n\n#### 1. 全面覆盖原则\n被诉侵权技术方案包含与权利要求记载的全部技术特征相同或等同的技术特征。\n\n#### 2. 等同原则\n以基本相同的手段，实现基本相同的功能，达到基本相同的效果。\n\n#### 3. 禁止反悔原则\n专利权人在专利授权或无效程序中放弃的技术方案，不能再主张。\n\n### 二、侵权分析步骤\n\n```\n1. 确定权利要求保护范围\n      ↓\n2. 分解权利要求技术特征\n      ↓\n3. 分析被诉产品技术特征\n      ↓\n4. 逐一比对技术特征\n      ↓\n5. 综合判断是否构成侵权\n```\n\n### 三、侵权抗辩\n\n1. **现有技术抗辩**\n2. **先用权抗辩**\n3. **合法来源抗辩**\n4. **专利权用尽抗辩**',
   108, 'ARTICLE', 4, 'PUBLISHED', 145, 28, '专利侵权,知识产权,侵权判定', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (106, '商标侵权案件处理流程', 
   E'## 商标侵权案件处理指南\n\n### 一、侵权调查\n\n1. **侵权事实固定**\n   - 公证购买侵权产品\n   - 网页公证保全\n   - 工商查询企业信息\n\n2. **损害证据收集**\n   - 侵权产品销售规模\n   - 权利人损失证明\n   - 市场混淆证据\n\n### 二、维权途径\n\n| 途径 | 优点 | 缺点 |\n|------|------|------|\n| 工商投诉 | 快速、免费 | 仅能责令停止 |\n| 民事诉讼 | 可获赔偿 | 周期长 |\n| 刑事报案 | 威慑力强 | 门槛高 |\n\n### 三、赔偿计算\n\n**法定赔偿顺序**：\n1. 实际损失\n2. 侵权获利\n3. 许可使用费合理倍数\n4. 法定赔偿（500万元以下）\n5. 惩罚性赔偿（故意侵权，1-5倍）\n\n### 四、诉讼策略\n\n1. 选择有利管辖法院\n2. 申请诉前禁令\n3. 充分举证商标知名度\n4. 合理主张赔偿数额',
   103, 'ARTICLE', 4, 'PUBLISHED', 112, 19, '商标侵权,知识产权,诉讼策略', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 公司法务
  (107, '股权转让协议审查要点', 
   E'## 股权转让协议审查清单\n\n### 一、股权状况核查\n\n- [ ] 出让方持股比例及出资证明\n- [ ] 股权是否存在质押、冻结\n- [ ] 其他股东是否放弃优先购买权\n- [ ] 公司章程是否有限制性规定\n\n### 二、核心条款审查\n\n#### 1. 转让价款\n- 定价依据（评估、净资产、协商）\n- 支付方式与时间\n- 价款调整机制\n\n#### 2. 交割安排\n- 股权交割时点\n- 工商变更登记\n- 股东名册变更\n\n#### 3. 陈述与保证\n- 出让方资格\n- 目标公司状况\n- 历史问题披露\n\n#### 4. 违约责任\n- 陈述保证不实的责任\n- 逾期履行的违约金\n- 解除权约定\n\n### 三、特别关注事项\n\n1. **对赌条款**：业绩承诺、股权回购\n2. **竞业限制**：原股东竞业禁止\n3. **保密条款**：交易信息保密\n4. **税务安排**：税费承担约定',
   104, 'ARTICLE', 4, 'PUBLISHED', 178, 32, '股权转让,公司法,尽职调查', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 诉讼实务
  (108, '民事诉讼证据规则要点', 
   E'## 民事诉讼证据规则实务\n\n### 一、举证责任分配\n\n#### 一般规则\n"谁主张，谁举证"\n\n#### 举证责任倒置\n- 医疗损害责任纠纷（因果关系、过错）\n- 环境污染责任纠纷（因果关系）\n- 产品责任纠纷（产品缺陷）\n\n### 二、证据类型\n\n| 类型 | 证明力 | 注意事项 |\n|------|--------|----------|\n| 书证 | 较高 | 原件优先 |\n| 电子证据 | 中等 | 需完整保全 |\n| 证人证言 | 较低 | 需当庭作证 |\n| 鉴定意见 | 较高 | 可申请重新鉴定 |\n\n### 三、质证技巧\n\n1. **真实性质疑**\n   - 审查证据来源\n   - 核对原件\n   - 关注矛盾之处\n\n2. **合法性审查**\n   - 取证手段是否合法\n   - 证据形式是否符合要求\n\n3. **关联性分析**\n   - 证据与待证事实的关系\n   - 证明力强弱评估\n\n### 四、证据保全\n\n- 诉前证据保全\n- 诉中证据保全\n- 公证保全',
   105, 'ARTICLE', 3, 'PUBLISHED', 267, 51, '民事诉讼,证据规则,诉讼技巧', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (109, '二审改判的关键因素分析', 
   E'## 二审改判成功案例分析\n\n### 一、二审改判的主要情形\n\n1. **事实认定错误**\n   - 证据采信不当\n   - 事实认定遗漏\n   - 证据证明力判断错误\n\n2. **法律适用错误**\n   - 法律条文理解错误\n   - 法律关系定性错误\n   - 责任分配不当\n\n3. **程序违法**\n   - 剥夺当事人诉讼权利\n   - 审判组织组成不合法\n   - 应当回避未回避\n\n### 二、二审代理策略\n\n#### 上诉方策略\n1. 精准提炼上诉理由\n2. 围绕争议焦点补充证据\n3. 强化法律论证\n4. 申请二审开庭审理\n\n#### 被上诉方策略\n1. 巩固一审判决理由\n2. 反驳上诉理由\n3. 补充新证据强化论证\n\n### 三、二审新证据\n\n根据《民事诉讼法解释》第100条：\n- 一审后新发现的证据\n- 一审因客观原因未能收集的证据\n- 一审未质证的证据',
   105, 'ARTICLE', 4, 'PUBLISHED', 156, 29, '二审,上诉,改判', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (110, '执行难问题的应对策略', 
   E'## 执行案件代理实务\n\n### 一、执行前准备\n\n#### 1. 财产调查\n- 不动产查询（房产、土地）\n- 车辆信息查询\n- 银行账户查询\n- 股权信息查询\n- 网络资产调查\n\n#### 2. 财产保全\n- 诉前保全\n- 诉中保全\n- 执行前保全\n\n### 二、执行措施\n\n| 措施 | 适用对象 | 效果 |\n|------|----------|------|\n| 查封、扣押 | 动产、不动产 | 控制财产 |\n| 冻结 | 银行存款、股权 | 禁止处分 |\n| 拍卖、变卖 | 已查封财产 | 变现清偿 |\n| 限制消费 | 被执行人 | 间接强制 |\n| 失信名单 | 被执行人 | 信用惩戒 |\n\n### 三、执行难破解\n\n1. **被执行人下落不明**\n   - 申请公告送达\n   - 向公安机关报案\n\n2. **财产转移隐匿**\n   - 申请审计查账\n   - 撤销权诉讼\n   - 追加被执行人\n\n3. **无可供执行财产**\n   - 终结本次执行程序\n   - 持续跟踪财产线索\n   - 申请恢复执行',
   105, 'ARTICLE', 3, 'PUBLISHED', 198, 42, '强制执行,执行难,财产保全', 
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('kb_article_id_seq', COALESCE((SELECT MAX(id) FROM kb_article), 1));

-- =====================================================
-- 完成提示
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '====================================================';
  RAISE NOTICE '知识库示例数据初始化完成！';
  RAISE NOTICE '----------------------------------------------------';
  RAISE NOTICE '新增数据：';
  RAISE NOTICE '  - 知识库分类: 8个';
  RAISE NOTICE '  - 知识库文章: 10篇';
  RAISE NOTICE '    - 合同法律知识: 2篇';
  RAISE NOTICE '    - 劳动法律知识: 2篇';
  RAISE NOTICE '    - 知识产权: 2篇';
  RAISE NOTICE '    - 公司法务: 1篇';
  RAISE NOTICE '    - 诉讼实务: 3篇';
  RAISE NOTICE '====================================================';
END $$;

-- =====================================================
-- 合并自: 41-schedule-demo.sql
-- =====================================================

-- =====================================================
-- 律师事务所管理系统 - 日程示例数据
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-14
-- 描述: 日程事件、待办提醒示例数据
-- =====================================================

-- =====================================================
-- 1. 日程事件
-- =====================================================
-- 事件类型：COURT_HEARING-庭审, MEETING-会议, DEADLINE-截止日期, 
--           CLIENT_VISIT-客户会见, TRAINING-培训, OTHER-其他

INSERT INTO schedule (id, title, description, schedule_type, start_time, end_time,
  all_day, location, matter_id, user_id, reminder_minutes, reminder_sent,
  created_by, created_at, updated_at, deleted)
VALUES
  -- 庭审日程
  (101, '广州制造业劳动争议案开庭',
   '广州制造业公司劳动争议案第一次开庭审理，需提前准备答辩材料和证据',
   'COURT', '2026-01-20 09:30:00', '2026-01-20 12:00:00',
   false, '广州市中级人民法院第三法庭', 103, 3, 60, false,
   3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (102, '专利侵权案证据交换',
   '深圳互联网公司专利侵权案证据交换，准备证据副本3份',
   'COURT', '2026-01-22 14:00:00', '2026-01-22 16:00:00',
   false, '深圳市知识产权法庭', 104, 4, 120, false,
   4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (103, '离婚诉讼案调解',
   '王美丽离婚案调解，法院主持调解',
   'COURT', '2026-01-25 10:00:00', '2026-01-25 11:30:00',
   false, '北京市朝阳区人民法院调解室', 106, 3, 60, false,
   3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 客户会议
  (104, '北京科技公司季度法律顾问会议',
   '2026年Q1法律顾问工作汇报，讨论合规风险及下阶段工作计划',
   'MEETING', '2026-01-18 14:00:00', '2026-01-18 16:00:00',
   false, '北京科技创新公司会议室', 101, 3, 30, false,
   3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (105, '上海金融集团并购项目进度汇报',
   '并购项目阶段性汇报，介绍尽职调查发现及后续建议',
   'MEETING', '2026-01-19 10:00:00', '2026-01-19 12:00:00',
   false, '上海金融中心大厦28楼会议室', 102, 4, 60, false,
   4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 客户会见
  (106, '李建军案情沟通',
   '与当事人李建军沟通仲裁裁决情况及后续安排',
   'APPOINTMENT', '2026-01-17 15:00:00', '2026-01-17 16:00:00',
   false, '律所接待室A', 105, 3, 30, false,
   3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (107, '新客户咨询接待',
   '杭州电商公司法务总监来所咨询合作事宜',
   'APPOINTMENT', '2026-01-21 14:30:00', '2026-01-21 16:00:00',
   false, '律所贵宾接待室', NULL, 4, 30, false,
   4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 内部会议
  (108, '诉讼部周例会',
   '本周案件进展汇报，下周工作安排',
   'MEETING', '2026-01-20 08:30:00', '2026-01-20 09:30:00',
   false, '律所大会议室', NULL, 3, 15, false,
   3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (109, '律所全员月度大会',
   '1月份工作总结，2月份工作部署',
   'MEETING', '2026-01-31 16:00:00', '2026-01-31 17:30:00',
   false, '律所多功能厅', NULL, 2, 60, false,
   2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 培训
  (110, '新民事诉讼法解读培训',
   '邀请法官讲解新民事诉讼法重点修改内容',
   'OTHER', '2026-01-23 14:00:00', '2026-01-23 17:00:00',
   false, '律所培训教室', NULL, 7, 30, false,
   7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (111, '知识产权诉讼实务分享',
   '李团长分享知识产权诉讼经验',
   'OTHER', '2026-01-28 15:00:00', '2026-01-28 16:30:00',
   false, '律所小会议室', NULL, 4, 30, false,
   4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 截止日期提醒
  (112, '劳动仲裁答辩状提交截止',
   '广州制造业劳动争议案答辩状提交截止日',
   'DEADLINE', '2026-01-18 00:00:00', '2026-01-18 23:59:59',
   true, NULL, 103, 3, 1440, false,
   3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (113, '专利侵权起诉状提交截止',
   '深圳互联网公司专利侵权案起诉状提交截止',
   'DEADLINE', '2026-01-25 00:00:00', '2026-01-25 23:59:59',
   true, NULL, 104, 4, 1440, false,
   4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 其他事项
  (114, '律师证年检',
   '完成律师执业证年度检验',
   'OTHER', '2026-01-30 00:00:00', '2026-01-30 23:59:59',
   true, '司法局', NULL, 3, 1440, false,
   3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (115, '案例库更新',
   '整理2025年度典型案例入库',
   'OTHER', '2026-01-27 09:00:00', '2026-01-27 18:00:00',
   false, '办公室', NULL, 7, 60, false,
   7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('schedule_id_seq', COALESCE((SELECT MAX(id) FROM schedule), 1));

-- =====================================================
-- 2. 日程共享（如果有共享表的话）
-- =====================================================
-- 注：如果存在 schedule_event_share 表，可以在此添加共享记录

-- =====================================================
-- 完成提示
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '====================================================';
  RAISE NOTICE '日程示例数据初始化完成！';
  RAISE NOTICE '----------------------------------------------------';
  RAISE NOTICE '新增数据：';
  RAISE NOTICE '  - 日程事件: 15条';
  RAISE NOTICE '    - 庭审: 3条';
  RAISE NOTICE '    - 会议: 4条';
  RAISE NOTICE '    - 客户会见: 2条';
  RAISE NOTICE '    - 培训: 2条';
  RAISE NOTICE '    - 截止日期: 2条';
  RAISE NOTICE '    - 其他: 2条';
  RAISE NOTICE '====================================================';
END $$;

-- =====================================================
-- 合并自: 42-hr-demo.sql
-- =====================================================

-- =====================================================
-- 律师事务所管理系统 - 人力资源示例数据
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-14
-- 描述: 考勤记录、培训记录示例数据
-- =====================================================

-- =====================================================
-- 1. 考勤记录 (最近一周)
-- =====================================================
-- 考勤状态：NORMAL-正常, LATE-迟到, EARLY_LEAVE-早退, ABSENT-缺勤, LEAVE-请假

INSERT INTO hr_attendance (id, user_id, attendance_date, check_in_time, check_out_time, 
  work_hours, status, late_minutes, early_leave_minutes, remark, created_at, updated_at, deleted)
VALUES
  -- 2026-01-13 周一
  (101, 3, '2026-01-13', '08:55:00', '18:30:00', 9.5, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (102, 4, '2026-01-13', '09:05:00', '19:00:00', 10.0, 'LATE', 5, 0, '路上堵车', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (103, 5, '2026-01-13', '08:50:00', '18:05:00', 9.25, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (104, 6, '2026-01-13', '08:45:00', '17:45:00', 9.0, 'EARLY_LEAVE', 0, 15, '家中有事提前离开', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (105, 7, '2026-01-13', '09:00:00', '18:00:00', 9.0, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 2026-01-14 周二
  (106, 3, '2026-01-14', '08:50:00', '20:30:00', 11.5, 'NORMAL', 0, 0, '加班处理案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (107, 4, '2026-01-14', '08:58:00', '18:15:00', 9.3, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (108, 5, '2026-01-14', '09:00:00', '18:00:00', 9.0, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (109, 6, '2026-01-14', '08:55:00', '18:10:00', 9.25, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (110, 7, '2026-01-14', NULL, NULL, 0, 'LEAVE', 0, 0, '年假', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 2026-01-10 上周五
  (111, 3, '2026-01-10', '08:45:00', '18:00:00', 9.25, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (112, 4, '2026-01-10', '08:55:00', '21:00:00', 12.0, 'NORMAL', 0, 0, '并购项目加班', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (113, 5, '2026-01-10', '08:50:00', '18:05:00', 9.25, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (114, 6, '2026-01-10', '09:00:00', '18:00:00', 9.0, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (115, 7, '2026-01-10', '08:55:00', '18:15:00', 9.3, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 2026-01-09 上周四
  (116, 3, '2026-01-09', '08:40:00', '18:20:00', 9.5, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (117, 4, '2026-01-09', '08:50:00', '18:30:00', 9.5, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (118, 5, '2026-01-09', '09:15:00', '18:00:00', 8.75, 'LATE', 15, 0, '地铁故障', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (119, 6, '2026-01-09', '08:55:00', '18:05:00', 9.15, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (120, 7, '2026-01-09', '08:58:00', '18:00:00', 9.0, 'NORMAL', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('hr_attendance_id_seq', COALESCE((SELECT MAX(id) FROM hr_attendance), 1));

-- =====================================================
-- 2. 培训记录
-- =====================================================
-- 培训类型：INTERNAL-内部培训, EXTERNAL-外部培训, ONLINE-在线培训
-- 培训状态：PLANNED-计划中, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消

INSERT INTO hr_training (id, training_name, training_type, description, trainer, 
  start_date, end_date, duration_hours, location, max_participants, 
  status, created_by, created_at, updated_at, deleted)
VALUES
  (101, '新民事诉讼法重点解读', 'INTERNAL', 
   '邀请退休法官讲解2025年民事诉讼法修改要点，包括电子送达、小额诉讼程序等内容',
   '张法官（退休）', '2026-01-23', '2026-01-23', 3, '律所培训教室', 30,
   'PLANNED', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (102, '知识产权诉讼实务分享', 'INTERNAL',
   '李团长分享近年知识产权诉讼经验，包括专利、商标、著作权案件代理技巧',
   '李团长', '2026-01-28', '2026-01-28', 2, '律所小会议室', 20,
   'PLANNED', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (103, '企业合规实务培训', 'EXTERNAL',
   '参加律师协会组织的企业合规专题培训',
   '协会讲师团', '2026-02-15', '2026-02-16', 12, '律师协会培训中心', 50,
   'PLANNED', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (104, '法律AI工具使用培训', 'ONLINE',
   '学习法律AI辅助工具的使用方法，提高工作效率',
   '技术部', '2026-01-20', '2026-01-20', 2, '线上会议', 100,
   'PLANNED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (105, '青年律师执业技能培训', 'INTERNAL',
   '面向实习律师和初级律师的执业技能培训，包括会见技巧、法律文书写作',
   '张律师', '2026-02-10', '2026-02-12', 18, '律所培训教室', 15,
   'PLANNED', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 已完成的培训
  (106, '律所信息化系统使用培训', 'INTERNAL',
   '新系统上线培训，包括案件管理、工时记录、文档管理等模块使用说明',
   '系统管理员', '2026-01-05', '2026-01-05', 4, '律所大会议室', 50,
   'COMPLETED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (107, '年度职业道德与执业规范', 'EXTERNAL',
   '司法局要求的年度必修课程',
   '司法局培训处', '2025-12-20', '2025-12-20', 6, '司法局培训中心', 200,
   'COMPLETED', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('hr_training_id_seq', COALESCE((SELECT MAX(id) FROM hr_training), 1));

-- =====================================================
-- 3. 培训参与记录
-- =====================================================

INSERT INTO hr_training_participant (id, training_id, user_id, status, 
  attendance_status, score, feedback, created_at, updated_at)
VALUES
  -- 信息化系统培训参与者
  (101, 106, 2, 'ENROLLED', 'ATTENDED', NULL, '培训内容很实用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (102, 106, 3, 'ENROLLED', 'ATTENDED', NULL, '系统操作简单易学', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (103, 106, 4, 'ENROLLED', 'ATTENDED', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (104, 106, 5, 'ENROLLED', 'ATTENDED', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (105, 106, 6, 'ENROLLED', 'ATTENDED', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (106, 106, 7, 'ENROLLED', 'ATTENDED', NULL, '希望能有更多操作练习', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  
  -- 职业道德培训参与者
  (107, 107, 3, 'ENROLLED', 'ATTENDED', 92, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (108, 107, 4, 'ENROLLED', 'ATTENDED', 88, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (109, 107, 7, 'ENROLLED', 'ATTENDED', 95, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  
  -- 新民事诉讼法培训报名
  (110, 101, 3, 'ENROLLED', NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (111, 101, 4, 'ENROLLED', NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (112, 101, 7, 'ENROLLED', NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  
  -- 知识产权培训报名
  (113, 102, 3, 'ENROLLED', NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (114, 102, 7, 'ENROLLED', NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval('hr_training_participant_id_seq', COALESCE((SELECT MAX(id) FROM hr_training_participant), 1));

-- =====================================================
-- 完成提示
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '====================================================';
  RAISE NOTICE '人力资源示例数据初始化完成！';
  RAISE NOTICE '----------------------------------------------------';
  RAISE NOTICE '新增数据：';
  RAISE NOTICE '  - 考勤记录: 20条（覆盖4个工作日）';
  RAISE NOTICE '  - 培训计划: 7个（5个计划中，2个已完成）';
  RAISE NOTICE '  - 培训参与记录: 14条';
  RAISE NOTICE '====================================================';
END $$;

-- =====================================================
-- 合并自: 43-admin-demo.sql
-- =====================================================

-- =====================================================
-- 律师事务所管理系统 - 行政管理示例数据
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-14
-- 描述: 会议室、印章、用印申请示例数据
-- =====================================================

-- =====================================================
-- 1. 会议室
-- =====================================================
-- 会议室状态：AVAILABLE-可用, OCCUPIED-使用中, MAINTENANCE-维护中

INSERT INTO admin_meeting_room (id, room_name, room_code, location, capacity, 
  equipment, description, status, created_at, updated_at, deleted)
VALUES
  (101, '大会议室', 'MR-001', '12楼东侧', 30, 
   '投影仪,视频会议系统,白板,音响', '可容纳30人，适合全员会议、培训',
   'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (102, '小会议室A', 'MR-002', '12楼西侧', 8,
   '电视屏幕,白板', '可容纳8人，适合小型会议',
   'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (103, '小会议室B', 'MR-003', '12楼西侧', 6,
   '电视屏幕', '可容纳6人，适合小型讨论',
   'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (104, '贵宾接待室', 'MR-004', '12楼中央', 12,
   '投影仪,茶水服务', '高端装修，用于接待重要客户',
   'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (105, '培训教室', 'TR-001', '11楼', 50,
   '投影仪,音响,录播设备,白板', '培训专用，配备录播设备',
   'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('admin_meeting_room_id_seq', COALESCE((SELECT MAX(id) FROM admin_meeting_room), 1));

-- =====================================================
-- 注：会议室预约功能需要先创建 meeting_room_booking 表
-- =====================================================

-- =====================================================
-- 3. 印章管理
-- =====================================================
-- 印章类型：OFFICIAL-公章, CONTRACT-合同章, FINANCE-财务章, LEGAL_REP-法定代表人章, INVOICE-发票章
-- 印章状态：NORMAL-正常, LOCKED-锁定, DESTROYED-已销毁

INSERT INTO admin_seal (id, seal_name, seal_type, seal_no, keeper_id, 
  location, description, status, created_at, updated_at, deleted)
VALUES
  (101, '律所公章', 'OFFICIAL', 'SEAL-2020-001', 2,
   '主任办公室保险柜', '律所官方印章，用于重要文件盖章',
   'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (102, '合同专用章', 'CONTRACT', 'SEAL-2020-002', 5,
   '财务室保险柜', '合同签署专用',
   'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (103, '财务专用章', 'FINANCE', 'SEAL-2020-003', 5,
   '财务室保险柜', '财务收支专用',
   'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (104, '发票专用章', 'INVOICE', 'SEAL-2020-004', 5,
   '财务室保险柜', '开具发票专用',
   'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (105, '法定代表人章', 'LEGAL_REP', 'SEAL-2020-005', 2,
   '主任办公室保险柜', '法定代表人签章',
   'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('admin_seal_id_seq', COALESCE((SELECT MAX(id) FROM admin_seal), 1));

-- =====================================================
-- 4. 用印申请
-- =====================================================
-- 申请状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝, COMPLETED-已用印, CANCELLED-已取消

INSERT INTO seal_application (id, application_no, applicant_id, seal_id,
  document_name, document_type, copies, use_purpose, matter_id,
  status, approved_by, approved_at, used_by, used_at, use_remark,
  created_by, created_at, updated_at, deleted)
VALUES
  -- 已完成的用印
  (101, 'SA2026010001', 3, 102,
   '北京科技创新公司法律顾问服务合同', '合同', 2, '客户合同签署', 101,
   'USED', 2, '2026-01-08 10:00:00', 5, '2026-01-08 14:00:00',
   '一式两份', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (102, 'SA2026010002', 4, 102,
   '上海金融集团并购项目服务合同', '合同', 3, '客户合同签署', 102,
   'USED', 2, '2026-01-10 09:30:00', 5, '2026-01-10 11:00:00',
   '一式三份，客户、律所、存档各一份', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (103, 'SA2026010003', 3, 101,
   '律师函（催告履约）', '函件', 1, '发送律师函', 101,
   'USED', 2, '2026-01-12 14:00:00', 2, '2026-01-12 15:30:00',
   NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 已批准待用印
  (104, 'SA2026010004', 4, 101,
   '专利侵权警告函', '函件', 1, '发送警告函', 104,
   'APPROVED', 2, '2026-01-14 09:00:00', NULL, NULL,
   '需要尽快用印发出', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 待审批
  (105, 'SA2026010005', 3, 102,
   '李建军劳动争议代理合同', '合同', 2, '客户合同签署', 105,
   'PENDING', NULL, NULL, NULL, NULL,
   NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  (106, 'SA2026010006', 3, 102,
   '王美丽婚姻家事代理合同', '合同', 2, '客户合同签署', 106,
   'PENDING', NULL, NULL, NULL, NULL,
   NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('seal_application_id_seq', COALESCE((SELECT MAX(id) FROM seal_application), 1));

-- =====================================================
-- 5. 办公用品/资产 (如果存在相关表)
-- =====================================================
-- 注：如果存在 admin_asset 表，可以在此添加办公设备数据

-- =====================================================
-- 完成提示
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '====================================================';
  RAISE NOTICE '行政管理示例数据初始化完成！';
  RAISE NOTICE '----------------------------------------------------';
  RAISE NOTICE '新增数据：';
  RAISE NOTICE '  - 会议室: 5个';
  RAISE NOTICE '  - 印章: 5个（公章/合同章/财务章/发票章/法代章）';
  RAISE NOTICE '  - 用印申请: 6条（3个已用印，1个已批准，2个待审批）';
  RAISE NOTICE '  注：会议室预约功能需要 meeting_room_booking 表';
  RAISE NOTICE '====================================================';
END $$;

-- =====================================================
-- 合并自: 44-finance-demo.sql
-- =====================================================

-- =====================================================
-- 律师事务所管理系统 - 财务示例数据
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-14
-- 描述: 收费记录、付款记录、发票、工时记录示例数据
-- =====================================================

-- =====================================================
-- 1. 收费记录 (补充)
-- =====================================================
-- 收费状态：PENDING-待收款, PARTIAL-部分收款, PAID-已收款, OVERDUE-逾期, CANCELLED-已取消

INSERT INTO finance_fee (id, fee_no, fee_name, client_id, contract_id, matter_id, fee_type, amount, 
  received_amount, planned_date, actual_date, status, remark, created_by, created_at, updated_at, deleted)
VALUES
  -- 北京科技创新公司 - 法律顾问费
  (101, 'FEE2025Q3-001', '2025年Q3法律顾问服务费', 101, 101, 101, 'SERVICE_FEE', 30000.00, 
   30000.00, '2025-10-15', '2025-10-12', 'PAID', '第三季度服务费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (102, 'FEE2025Q4-001', '2025年Q4法律顾问服务费', 101, 101, 101, 'SERVICE_FEE', 30000.00, 
   30000.00, '2026-01-15', '2025-12-28', 'PAID', '第四季度服务费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (103, 'FEE2026Q1-001', '2026年Q1法律顾问服务费', 101, 101, 101, 'SERVICE_FEE', 30000.00, 
   0, '2026-04-15', NULL, 'PENDING', '第一季度服务费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 上海金融集团 - 并购项目费
  (104, 'FEE2025-M102-01', '并购项目首期费用', 102, 102, 102, 'SERVICE_FEE', 150000.00, 
   150000.00, '2025-09-15', '2025-09-10', 'PAID', '签约后支付', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (105, 'FEE2025-M102-02', '并购项目第二期费用', 102, 102, 102, 'SERVICE_FEE', 150000.00, 
   150000.00, '2025-12-15', '2025-12-20', 'PAID', '尽调完成后支付', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (106, 'FEE2026-M102-03', '并购项目第三期费用', 102, 102, 102, 'SERVICE_FEE', 200000.00, 
   0, '2026-03-15', NULL, 'PENDING', '交割完成后支付', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 广州制造业公司 - 劳动仲裁
  (107, 'FEE2025-M103-01', '劳动仲裁代理费预付', 103, 103, 103, 'LITIGATION_FEE', 10000.00, 
   10000.00, '2025-10-20', '2025-10-18', 'PAID', '预付款', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 深圳互联网公司 - 专利维权
  (108, 'FEE2025-M104-01', '专利维权首期费用', 104, 104, 104, 'LITIGATION_FEE', 30000.00, 
   30000.00, '2025-12-01', '2025-11-28', 'PAID', '启动费用', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 李建军 - 个人劳动仲裁
  (109, 'FEE2025-M105-01', '劳动仲裁代理费', 105, 105, 105, 'LITIGATION_FEE', 15000.00, 
   7500.00, '2025-12-10', '2025-12-08', 'PARTIAL', '先支付50%', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 王美丽 - 离婚诉讼
  (110, 'FEE2026-M106-01', '离婚诉讼代理费', 106, 106, 106, 'LITIGATION_FEE', 20000.00, 
   10000.00, '2026-01-15', '2026-01-10', 'PARTIAL', '先支付50%', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('finance_fee_id_seq', COALESCE((SELECT MAX(id) FROM finance_fee), 1));

-- =====================================================
-- 2. 付款记录
-- =====================================================

INSERT INTO finance_payment (id, payment_no, fee_id, amount, payment_method, payment_date,
  payer_name, payer_account, receipt_no, remark, received_by, created_at, updated_at, deleted)
VALUES
  -- 北京科技创新公司付款
  (101, 'PAY2025-001', 101, 30000.00, 'BANK_TRANSFER', '2025-10-12',
   '北京科技创新有限公司', '6222***1234', 'REC2025-001', 'Q3顾问费', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (102, 'PAY2025-002', 102, 30000.00, 'BANK_TRANSFER', '2025-12-28',
   '北京科技创新有限公司', '6222***1234', 'REC2025-002', 'Q4顾问费', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 上海金融集团付款
  (103, 'PAY2025-003', 104, 150000.00, 'BANK_TRANSFER', '2025-09-10',
   '上海金融投资集团', '3100***5678', 'REC2025-003', '并购项目首期', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (104, 'PAY2025-004', 105, 150000.00, 'BANK_TRANSFER', '2025-12-20',
   '上海金融投资集团', '3100***5678', 'REC2025-004', '并购项目二期', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 广州制造业公司付款
  (105, 'PAY2025-005', 107, 10000.00, 'BANK_TRANSFER', '2025-10-18',
   '广州制造业有限公司', '4400***9012', 'REC2025-005', '劳动仲裁预付', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 深圳互联网公司付款
  (106, 'PAY2025-006', 108, 30000.00, 'BANK_TRANSFER', '2025-11-28',
   '深圳互联网科技公司', '4403***3456', 'REC2025-006', '专利维权首期', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 个人客户付款
  (107, 'PAY2025-007', 109, 7500.00, 'ALIPAY', '2025-12-08',
   '李建军', NULL, 'REC2025-007', '劳动仲裁预付50%', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (108, 'PAY2026-001', 110, 10000.00, 'WECHAT', '2026-01-10',
   '王美丽', NULL, 'REC2026-001', '离婚诉讼预付50%', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('finance_payment_id_seq', COALESCE((SELECT MAX(id) FROM finance_payment), 1));

-- =====================================================
-- 3. 发票记录
-- =====================================================
-- 发票状态：PENDING-待开票, ISSUED-已开票, CANCELLED-已作废, RED_LETTER-红冲

INSERT INTO finance_invoice (id, invoice_no, invoice_type, fee_id, client_id, amount, tax_rate, tax_amount,
  title, tax_no, address, bank_info, status, issued_by, issued_at, 
  remark, created_at, updated_at, deleted)
VALUES
  -- 北京科技创新公司发票
  (101, 'FP2025-001', 'VAT_SPECIAL', 101, 101, 30000.00, 6.0, 1698.11,
   '北京科技创新有限公司', '91110108MA01XXXXX1', '北京市海淀区中关村大街1号', '工商银行 0200001234567',
   'ISSUED', 5, '2025-10-15', 'Q3法律顾问服务费', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (102, 'FP2025-002', 'VAT_SPECIAL', 102, 101, 30000.00, 6.0, 1698.11,
   '北京科技创新有限公司', '91110108MA01XXXXX1', '北京市海淀区中关村大街1号', '工商银行 0200001234567',
   'ISSUED', 5, '2025-12-30', 'Q4法律顾问服务费', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 上海金融集团发票
  (103, 'FP2025-003', 'VAT_SPECIAL', 104, 102, 150000.00, 6.0, 8490.57,
   '上海金融投资集团', '91310000MA01XXXXX2', '上海市浦东新区陆家嘴金融中心', '建设银行 3100001234567',
   'ISSUED', 5, '2025-09-12', '并购项目首期服务费', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (104, 'FP2025-004', 'VAT_SPECIAL', 105, 102, 150000.00, 6.0, 8490.57,
   '上海金融投资集团', '91310000MA01XXXXX2', '上海市浦东新区陆家嘴金融中心', '建设银行 3100001234567',
   'ISSUED', 5, '2025-12-22', '并购项目二期服务费', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 待开票
  (105, NULL, 'VAT_SPECIAL', 107, 103, 10000.00, 6.0, 566.04,
   '广州制造业有限公司', '91440100MA01XXXXX3', '广州市天河区科技园区', '农业银行 4400001234567',
   'PENDING', NULL, NULL, '劳动仲裁代理费', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (106, NULL, 'VAT_SPECIAL', 108, 104, 30000.00, 6.0, 1698.11,
   '深圳互联网科技公司', '91440300MA01XXXXX4', '深圳市南山区科技园', '招商银行 7559001234567',
   'PENDING', NULL, NULL, '专利维权首期费用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('finance_invoice_id_seq', COALESCE((SELECT MAX(id) FROM finance_invoice), 1));

-- =====================================================
-- 4. 工时记录 (补充)
-- =====================================================
-- 工作类型：CONSULTATION-咨询, CONTRACT_REVIEW-合同审查, RESEARCH-法律研究, 
--           DOCUMENT_DRAFTING-文书起草, COURT_APPEARANCE-出庭, MEETING-会议, OTHER-其他

INSERT INTO timesheet (id, timesheet_no, matter_id, user_id, work_date, hours, work_type, work_content,
  billable, hourly_rate, amount, status, approved_by, approved_at, created_by, created_at, updated_at, deleted)
VALUES
  -- 北京科技创新公司顾问项目
  (101, 'TS2025-001', 101, 3, '2025-10-08', 2.0, 'CONTRACT_REVIEW', '审查供应商合同', 
   true, 500.00, 1000.00, 'APPROVED', 2, '2025-10-09', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (102, 'TS2025-002', 101, 3, '2025-10-15', 1.5, 'CONSULTATION', '电话法律咨询', 
   true, 500.00, 750.00, 'APPROVED', 2, '2025-10-16', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (103, 'TS2025-003', 101, 7, '2025-10-20', 3.0, 'RESEARCH', '劳动法合规研究', 
   true, 200.00, 600.00, 'APPROVED', 3, '2025-10-21', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (104, 'TS2025-004', 101, 3, '2025-11-05', 2.5, 'CONTRACT_REVIEW', '审查合作协议', 
   true, 500.00, 1250.00, 'APPROVED', 2, '2025-11-06', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 上海金融集团并购项目
  (105, 'TS2025-005', 102, 4, '2025-09-15', 8.0, 'RESEARCH', '目标公司尽职调查', 
   true, 600.00, 4800.00, 'APPROVED', 2, '2025-09-16', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (106, 'TS2025-006', 102, 4, '2025-09-16', 6.0, 'RESEARCH', '目标公司尽职调查', 
   true, 600.00, 3600.00, 'APPROVED', 2, '2025-09-17', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (107, 'TS2025-007', 102, 3, '2025-09-20', 4.0, 'DOCUMENT_DRAFTING', '尽调报告撰写', 
   true, 500.00, 2000.00, 'APPROVED', 2, '2025-09-21', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (108, 'TS2025-008', 102, 7, '2025-09-22', 6.0, 'RESEARCH', '历史交易资料整理', 
   true, 200.00, 1200.00, 'APPROVED', 4, '2025-09-23', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (109, 'TS2025-009', 102, 4, '2025-12-10', 5.0, 'DOCUMENT_DRAFTING', '起草股权转让协议', 
   true, 600.00, 3000.00, 'APPROVED', 2, '2025-12-11', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 广州制造业劳动争议案
  (110, 'TS2025-010', 103, 3, '2025-10-18', 4.0, 'DOCUMENT_DRAFTING', '准备仲裁答辩材料', 
   true, 500.00, 2000.00, 'APPROVED', 2, '2025-10-19', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (111, 'TS2025-011', 103, 3, '2025-11-10', 3.0, 'COURT_APPEARANCE', '出庭参加仲裁庭审', 
   true, 500.00, 1500.00, 'APPROVED', 2, '2025-11-11', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 深圳互联网专利侵权案
  (112, 'TS2025-012', 104, 4, '2025-12-01', 6.0, 'RESEARCH', '专利侵权分析', 
   true, 600.00, 3600.00, 'APPROVED', 2, '2025-12-02', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (113, 'TS2025-013', 104, 4, '2025-12-05', 4.0, 'DOCUMENT_DRAFTING', '起草侵权分析报告', 
   true, 600.00, 2400.00, 'APPROVED', 2, '2025-12-06', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 近期待审批工时
  (114, 'TS2026-001', 101, 3, '2026-01-10', 2.0, 'CONTRACT_REVIEW', '审查新版劳动合同模板', 
   true, 500.00, 1000.00, 'PENDING', NULL, NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (115, 'TS2026-002', 102, 4, '2026-01-12', 3.0, 'MEETING', '并购项目进度汇报会议', 
   true, 600.00, 1800.00, 'PENDING', NULL, NULL, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (116, 'TS2026-003', 106, 3, '2026-01-13', 2.5, 'CONSULTATION', '离婚案件初次会见', 
   true, 500.00, 1250.00, 'PENDING', NULL, NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (117, 'TS2026-004', 106, 7, '2026-01-14', 3.0, 'RESEARCH', '婚姻财产法律研究', 
   true, 200.00, 600.00, 'PENDING', NULL, NULL, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('timesheet_id_seq', COALESCE((SELECT MAX(id) FROM timesheet), 1));

-- =====================================================
-- 5. 费用支出记录 (如果存在 finance_expense 表)
-- =====================================================

INSERT INTO finance_expense (id, expense_no, expense_type, matter_id, applicant_id, amount,
  expense_date, description, receipt_count, status, approved_by, approved_at,
  reimbursed, reimbursed_at, created_at, updated_at, deleted)
VALUES
  -- 案件相关支出
  (101, 'EXP2025-001', 'LITIGATION_FEE', 103, 3, 500.00,
   '2025-10-15', '劳动仲裁案件受理费', 1, 'REIMBURSED', 2, '2025-10-16',
   true, '2025-10-20', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (102, 'EXP2025-002', 'TRAVEL', 103, 3, 800.00,
   '2025-11-10', '广州出差交通费', 2, 'REIMBURSED', 2, '2025-11-11',
   true, '2025-11-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (103, 'EXP2025-003', 'TRAVEL', 102, 4, 1500.00,
   '2025-09-15', '上海尽调出差差旅费', 3, 'REIMBURSED', 2, '2025-09-16',
   true, '2025-09-20', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (104, 'EXP2025-004', 'NOTARY', 104, 4, 2000.00,
   '2025-12-02', '专利侵权证据公证费', 1, 'REIMBURSED', 2, '2025-12-03',
   true, '2025-12-08', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 待审批支出
  (105, 'EXP2026-001', 'LITIGATION_FEE', 104, 4, 8000.00,
   '2026-01-10', '专利侵权诉讼案件受理费', 1, 'PENDING', NULL, NULL,
   false, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (106, 'EXP2026-002', 'DOCUMENT', 106, 3, 200.00,
   '2026-01-12', '离婚案件房产调查费', 2, 'PENDING', NULL, NULL,
   false, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('finance_expense_id_seq', COALESCE((SELECT MAX(id) FROM finance_expense), 1));

-- =====================================================
-- 完成提示
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '====================================================';
  RAISE NOTICE '财务示例数据初始化完成！';
  RAISE NOTICE '----------------------------------------------------';
  RAISE NOTICE '新增数据：';
  RAISE NOTICE '  - 收费记录: 10条';
  RAISE NOTICE '  - 付款记录: 8条';
  RAISE NOTICE '  - 发票记录: 6条（4已开票，2待开票）';
  RAISE NOTICE '  - 工时记录: 17条（13已审批，4待审批）';
  RAISE NOTICE '  - 费用支出: 6条（4已报销，2待审批）';
  RAISE NOTICE '====================================================';
END $$;

-- =====================================================
-- 合并自: 99-extra-demo-data.sql
-- =====================================================

-- =====================================================
-- 律师事务所管理系统 - 增量示例数据
-- =====================================================
-- 用途：在已有数据基础上增加更多示例数据
-- 使用：docker exec -i law-firm-postgres psql -U law_admin -d law_firm < scripts/init-db/99-extra-demo-data.sql
-- =====================================================

-- =====================================================
-- 1. 额外客户数据
-- =====================================================
INSERT INTO crm_client (id, client_no, name, client_type, credit_code, id_card, legal_representative, 
  registered_address, contact_person, contact_phone, contact_email, industry, source, level, category, 
  status, originator_id, responsible_lawyer_id, first_cooperation_date, remark, created_at, updated_at, created_by, updated_by, deleted)
VALUES
  (201, 'C2026020001', '成都软件开发有限公司', 'ENTERPRISE', '91510100MA99DEMO01', NULL, '刘伟', 
   '成都市高新区天府软件园', '周秘书', '13800201001', 'zhou@cdsoft.com', '软件开发', 'API测试', 'A', 'VIP',
   'ACTIVE', 3, 3, '2026-01-10', '软件著作权相关客户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  (202, 'C2026020002', '武汉医药科技公司', 'ENTERPRISE', '91420100MA99DEMO02', NULL, '陈强', 
   '武汉市东湖高新区光谷生物城', '杨助理', '13800201002', 'yang@whmed.com', '医药', 'API测试', 'B', 'NORMAL',
   'ACTIVE', 4, 4, '2026-01-11', '医药合规咨询客户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  (203, 'C2026020003', '张明华', 'INDIVIDUAL', NULL, '420101198803030033', NULL, 
   NULL, '张明华', '13900201003', 'zhangmh@email.com', NULL, 'API测试', 'B', 'NORMAL',
   'ACTIVE', 3, 3, '2026-01-12', '房产纠纷案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('crm_client_id_seq', GREATEST((SELECT MAX(id) FROM crm_client), 203));

-- =====================================================
-- 2. 额外合同数据
-- =====================================================
INSERT INTO finance_contract (id, contract_no, name, client_id, contract_type, fee_type, total_amount, currency,
  sign_date, effective_date, expiry_date, status, signer_id, department_id, 
  content, remark, created_at, updated_at, created_by, updated_by, deleted)
VALUES
  (201, '2026民代字第0101号', '软件著作权维权代理合同', 201, 'LITIGATION', 'CONTINGENCY', 150000.00, 'CNY',
   '2026-01-10', '2026-01-10', '2026-07-10', 'ACTIVE', 3, 1,
   '代理成都软件开发公司著作权侵权维权', '风险代理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  (202, '2026民代字第0102号', '医药合规咨询合同', 202, 'SERVICE', 'FIXED', 80000.00, 'CNY',
   '2026-01-11', '2026-01-11', '2026-12-31', 'ACTIVE', 4, 1,
   '为武汉医药科技公司提供合规咨询服务', '按季度付款', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false),
  
  (203, '2026民代字第0103号', '房产纠纷代理合同', 203, 'LITIGATION', 'FIXED', 25000.00, 'CNY',
   '2026-01-12', '2026-01-12', '2026-06-12', 'ACTIVE', 3, 4,
   '代理张明华房产纠纷诉讼', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('finance_contract_id_seq', GREATEST((SELECT MAX(id) FROM finance_contract), 203));

-- =====================================================
-- 3. 额外工时记录
-- =====================================================
INSERT INTO timesheet (id, timesheet_no, matter_id, user_id, work_date, hours, work_type, work_content,
  billable, hourly_rate, amount, status, created_by, created_at, updated_at, deleted)
VALUES
  (201, 'TS2026010001', 101, 3, '2026-01-08', 2.0, 'CONTRACT_REVIEW', '审查供应商合同修订版', 
   true, 500.00, 1000.00, 'DRAFT', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (202, 'TS2026010002', 101, 3, '2026-01-09', 1.5, 'CONSULTATION', '电话会议讨论合同条款', 
   true, 500.00, 750.00, 'DRAFT', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (203, 'TS2026010003', 102, 4, '2026-01-10', 4.0, 'RESEARCH', '并购项目法律研究', 
   true, 600.00, 2400.00, 'DRAFT', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (204, 'TS2026010004', 103, 3, '2026-01-11', 3.0, 'COURT_APPEARANCE', '出庭参加仲裁庭审', 
   true, 500.00, 1500.00, 'DRAFT', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (205, 'TS2026010005', 104, 4, '2026-01-12', 2.5, 'DOCUMENT_DRAFTING', '起草专利侵权起诉状', 
   true, 600.00, 1500.00, 'DRAFT', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('timesheet_id_seq', GREATEST((SELECT MAX(id) FROM timesheet), 205));

-- =====================================================
-- 4. 额外收费记录
-- =====================================================
INSERT INTO finance_fee (id, fee_no, fee_name, client_id, contract_id, matter_id, fee_type, amount, 
  planned_date, status, remark, created_by, created_at, updated_at, deleted)
VALUES
  (201, 'FEE2026010001', '法律顾问Q1服务费', 101, 101, 101, 'SERVICE_FEE', 30000.00, 
   '2026-01-15', 'PENDING', '2026年第一季度法律顾问服务费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (202, 'FEE2026010002', '并购项目首期费用', 102, 102, 102, 'SERVICE_FEE', 100000.00, 
   '2026-01-20', 'PENDING', '并购项目首期服务费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (203, 'FEE2026010003', '劳动仲裁代理费', 103, 103, 103, 'LITIGATION_FEE', 30000.00, 
   '2026-01-25', 'PENDING', '劳动争议案件代理费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (204, 'FEE2026010004', '专利维权首期费用', 104, 104, 104, 'LITIGATION_FEE', 40000.00, 
   '2026-01-30', 'PENDING', '专利侵权维权首期费用', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('finance_fee_id_seq', GREATEST((SELECT MAX(id) FROM finance_fee), 204));

-- =====================================================
-- 5. 额外出函申请
-- =====================================================
INSERT INTO letter_application (id, application_no, template_id, matter_id, applicant_id, 
  letter_type, target_unit, target_contact, target_phone, target_address, purpose,
  status, created_at, updated_at, deleted)
VALUES
  (201, 'LT2026010001', 1, 101, 3, 'LAWYER_LETTER', '某供应商公司', '李经理', '13800001111', '北京市朝阳区某路100号', '催告供应商履行合同义务',
   'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (202, 'LT2026010002', 1, 103, 3, 'LAWYER_LETTER', '广州某公司人事部', '人事经理', '13800002222', '广州市天河区某路200号', '要求提供劳动关系相关证明材料',
   'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (203, 'LT2026010003', 1, 104, 4, 'LAWYER_LETTER', '某科技公司', '法务部', '13800003333', '深圳市南山区某路300号', '专利侵权警告函',
   'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('letter_application_id_seq', GREATEST((SELECT MAX(id) FROM letter_application), 203));

-- =====================================================
-- 6. 额外档案借阅记录
-- =====================================================
INSERT INTO archive_borrow (id, borrow_no, archive_id, borrower_id, borrower_name, department,
  borrow_reason, borrow_date, expected_return_date, status, created_by, created_at, updated_at, deleted)
VALUES
  (201, 'BR2026010001', 101, 3, '张律师', '诉讼部', 
   '案件复查需要查阅原始档案', '2026-01-10', '2026-01-17', 'BORROWED', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (202, 'BR2026010002', 102, 4, '李团长', '诉讼部', 
   '类案参考', '2026-01-11', '2026-01-18', 'APPROVED', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('archive_borrow_id_seq', GREATEST((SELECT MAX(id) FROM archive_borrow), 202));

-- =====================================================
-- 完成提示
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '====================================================';
  RAISE NOTICE '增量示例数据添加完成！';
  RAISE NOTICE '----------------------------------------------------';
  RAISE NOTICE '新增数据：';
  RAISE NOTICE '  - 客户: 3个';
  RAISE NOTICE '  - 合同: 3个';
  RAISE NOTICE '  - 工时记录: 5条';
  RAISE NOTICE '  - 收费记录: 4条';
  RAISE NOTICE '  - 出函申请: 3个';
  RAISE NOTICE '  - 档案借阅: 2条';
  RAISE NOTICE '====================================================';
END $$;

-- =====================================================
-- 完成标记
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '演示数据加载完成';
    RAISE NOTICE '========================================';
END $$;

