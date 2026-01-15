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
