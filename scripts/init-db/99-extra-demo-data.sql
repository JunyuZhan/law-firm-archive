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
INSERT INTO finance_fee (id, fee_no, name, client_id, contract_id, matter_id, fee_type, amount, 
  due_date, status, description, created_by, created_at, updated_at, deleted)
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
  target_unit, target_contact, target_phone, target_address, 
  status, created_at, updated_at, deleted)
VALUES
  (201, 'LT2026010001', 1, 101, 3, '某供应商公司', '李经理', '13800001111', '北京市朝阳区某路100号',
   'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (202, 'LT2026010002', 1, 103, 3, '广州某公司人事部', '人事经理', '13800002222', '广州市天河区某路200号',
   'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  (203, 'LT2026010003', 1, 104, 4, '某科技公司', '法务部', '13800003333', '深圳市南山区某路300号',
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
