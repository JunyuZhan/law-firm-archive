-- =====================================================
-- 律师事务所管理系统 - 演示数据（完整版）
-- =====================================================
-- 版本: 2.0.0
-- 日期: 2026-01-17
-- 描述: 包含完整的演示数据，用于系统演示和测试
-- 说明: 此脚本使用 ON CONFLICT 避免重复插入
-- =====================================================

-- =====================================================
-- 1. 客户标签
-- =====================================================
INSERT INTO crm_client_tag (id, tag_name, tag_color, description, sort_order, created_by, created_at, updated_at, deleted)
VALUES 
(100, 'VIP客户', '#ff4d4f', '重要客户，需优先处理', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, '长期合作', '#52c41a', '长期合作客户', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, '新客户', '#1890ff', '首次合作客户', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, '国企', '#722ed1', '国有企业客户', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, '民企', '#fa8c16', '民营企业客户', 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 2. 客户数据
-- =====================================================
INSERT INTO crm_client (id, client_no, name, client_type, credit_code, id_card, legal_representative, registered_address, contact_person, contact_phone, contact_email, industry, source, level, category, status, originator_id, responsible_lawyer_id, first_cooperation_date, remark, created_at, updated_at, created_by, deleted)
VALUES
-- 企业客户
(100, 'KH2026010001', '贵州茂盛科技有限公司', 'ENTERPRISE', '91520100MA6DEMO001X', NULL, '张志强', '贵州省贵阳市云岩区中华中路168号', '李明', '13800138001', 'liming@maosheng.com', '信息技术', 'REFERRAL', 'A', 'NORMAL', 'ACTIVE', 1, 2, '2025-03-15', '科技公司，主营软件开发', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(101, 'KH2026010002', '贵阳鼎盛建筑工程有限公司', 'ENTERPRISE', '91520100MA6DEMO002X', NULL, '王建华', '贵州省贵阳市观山湖区金融城1号楼', '赵丽', '13800138002', 'zhaoli@dingsheng.com', '建筑业', 'WEBSITE', 'A', 'NORMAL', 'ACTIVE', 1, 3, '2025-05-20', '建筑公司，承接政府项目较多', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(102, 'KH2026010003', '贵州盛世矿业集团有限公司', 'ENTERPRISE', '91520100MA6DEMO003X', NULL, '刘强', '贵州省六盘水市钟山区人民路88号', '周伟', '13800138003', 'zhouwei@shengshi.com', '采矿业', 'REFERRAL', 'S', 'VIP', 'ACTIVE', 2, 2, '2024-08-10', 'VIP客户，大型矿业集团', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(103, 'KH2026010004', '贵阳市第一人民医院', 'ENTERPRISE', '12520100MB1DEMO004Y', NULL, '陈院长', '贵州省贵阳市南明区瑞金南路1号', '张护士长', '13800138004', 'yygl@gyrmyy.com', '医疗卫生', 'GOVERNMENT', 'A', 'NORMAL', 'ACTIVE', 3, 3, '2025-01-08', '公立医院，法律顾问服务', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(104, 'KH2026010005', '贵州恒通物流有限公司', 'ENTERPRISE', '91520100MA6DEMO005X', NULL, '孙海涛', '贵州省贵阳市白云区麦架镇物流园区', '钱芳', '13800138005', 'qianfang@hengtong.com', '物流运输', 'COLD_CALL', 'B', 'NORMAL', 'ACTIVE', 2, 4, '2025-06-12', '物流公司，运输合同纠纷处理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(105, 'KH2026010006', '贵州茅台镇酒业有限公司', 'ENTERPRISE', '91520300MA6DEMO006X', NULL, '吴国强', '贵州省遵义市仁怀市茅台镇', '郑经理', '13800138006', 'zheng@mtjy.com', '食品饮料', 'REFERRAL', 'S', 'VIP', 'ACTIVE', 1, 2, '2024-11-20', 'VIP客户，知名酒企', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(106, 'KH2026010007', '贵阳银河商贸有限公司', 'ENTERPRISE', '91520100MA6DEMO007X', NULL, '冯丽娟', '贵州省贵阳市云岩区延安中路88号', '吴助理', '13800138007', 'wu@yinhe.com', '批发零售', 'WEBSITE', 'B', 'NORMAL', 'POTENTIAL', 1, 4, NULL, '潜在客户，商贸公司', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
-- 个人客户
(107, 'KH2026010008', '张三', 'INDIVIDUAL', NULL, '520102198501150012', NULL, '贵州省贵阳市南明区花果园C区', '张三', '13800138008', 'zhangsan@qq.com', NULL, 'REFERRAL', 'B', 'NORMAL', 'ACTIVE', 3, 3, '2025-09-05', '个人客户，劳动争议案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(108, 'KH2026010009', '李四', 'INDIVIDUAL', NULL, '520103199003220035', NULL, '贵州省贵阳市云岩区未来方舟', '李四', '13800138009', 'lisi@163.com', NULL, 'WEBSITE', 'C', 'NORMAL', 'ACTIVE', 2, 4, '2025-10-18', '个人客户，交通事故案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(109, 'KH2026010010', '王五', 'INDIVIDUAL', NULL, '520111198712080048', NULL, '贵州省贵阳市花溪区大学城', '王五', '13800138010', 'wangwu@gmail.com', NULL, 'COLD_CALL', 'C', 'NORMAL', 'POTENTIAL', 4, 5, NULL, '潜在客户，咨询离婚事宜', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 3. 合同数据
-- =====================================================
INSERT INTO finance_contract (id, contract_no, name, client_id, contract_type, total_amount, status, sign_date, effective_date, expiry_date, fee_type, risk_ratio, payment_terms, department_id, created_by, created_at, updated_at, deleted)
VALUES
(100, '2026民代字第0001号', '茂盛科技法律顾问服务合同', 100, 'RETAINER', 120000.00, 'ACTIVE', '2025-03-15', '2025-03-15', '2026-03-14', 'FIXED', NULL, '常年法律顾问服务，每季度支付30000元', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, '2026民代字第0002号', '鼎盛建筑工程纠纷代理合同', 101, 'LITIGATION', 80000.00, 'ACTIVE', '2025-05-20', '2025-05-20', '2026-05-19', 'CONTINGENCY', 15.00, '建设工程合同纠纷代理，风险代理15%', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, '2026民代字第0003号', '盛世矿业股权并购法律服务', 102, 'NON_LITIGATION', 500000.00, 'ACTIVE', '2024-08-10', '2024-08-10', '2025-08-09', 'FIXED', NULL, '重大股权并购项目法律服务', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, '2026民代字第0004号', '第一人民医院法律顾问合同', 103, 'RETAINER', 80000.00, 'ACTIVE', '2025-01-08', '2025-01-08', '2026-01-07', 'FIXED', NULL, '医院常年法律顾问服务', 2, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, '2026民代字第0005号', '恒通物流合同纠纷代理', 104, 'LITIGATION', 35000.00, 'COMPLETED', '2025-06-12', '2025-06-12', '2025-12-30', 'FIXED', NULL, '运输合同纠纷代理案件', 2, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(105, '2026民代字第0006号', '茅台镇酒业知识产权顾问', 105, 'RETAINER', 200000.00, 'ACTIVE', '2024-11-20', '2024-11-20', '2025-11-19', 'FIXED', NULL, '知识产权法律顾问服务', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(106, '2026民代字第0007号', '张三劳动仲裁代理合同', 107, 'LITIGATION', 15000.00, 'ACTIVE', '2025-09-05', '2025-09-05', '2026-03-05', 'FIXED', NULL, '劳动争议仲裁代理', 2, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(107, '2026民代字第0008号', '李四交通事故代理合同', 108, 'LITIGATION', 20000.00, 'ACTIVE', '2025-10-18', '2025-10-18', '2026-04-18', 'CONTINGENCY', 20.00, '交通事故损害赔偿代理，风险代理20%', 2, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(108, '2026刑代字第0001号', '刘某盗窃案辩护合同', 107, 'LITIGATION', 25000.00, 'DRAFT', '2026-01-10', '2026-01-10', '2026-07-10', 'FIXED', NULL, '刑事辩护案件', 2, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 4. 案件/项目数据
-- =====================================================
INSERT INTO matter (id, matter_no, name, matter_type, case_type, cause_of_action, client_id, opposing_party, description, status, originator_id, lead_lawyer_id, department_id, fee_type, estimated_fee, filing_date, expected_end_date, contract_id, remark, created_at, updated_at, created_by, deleted)
VALUES
(100, 'MS2026010001', '茂盛科技常年法律顾问', 'NON_LITIGATION', 'CORPORATE_ADVISORY', NULL, 100, NULL, '为茂盛科技提供常年法律顾问服务，包括合同审核、法律咨询等', 'ACTIVE', 1, 2, 1, 'FIXED', 120000.00, '2025-03-15', '2026-03-14', 100, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(101, 'MS2026010002', '鼎盛建筑诉某房地产公司工程款纠纷', 'CIVIL_LITIGATION', 'CIVIL', '建设工程合同纠纷', 101, '贵阳某房地产开发有限公司', '我方代理鼎盛建筑公司，起诉某房地产公司拖欠工程款500万元', 'ACTIVE', 2, 3, 1, 'CONTINGENCY', 80000.00, '2025-05-20', '2026-05-19', 101, '已立案，等待开庭', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(102, 'MS2026010003', '盛世矿业收购贵州金矿股权项目', 'NON_LITIGATION', 'INVESTMENT', NULL, 102, NULL, '协助盛世矿业收购贵州某金矿60%股权，涉及金额3亿元', 'ACTIVE', 2, 2, 1, 'FIXED', 500000.00, '2024-08-10', '2025-08-09', 102, 'VIP项目，需重点跟进', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(103, 'MS2026010004', '第一人民医院常年法律顾问', 'NON_LITIGATION', 'CORPORATE_ADVISORY', NULL, 103, NULL, '为医院提供常年法律顾问服务，包括医疗纠纷处理、合同审核等', 'ACTIVE', 3, 3, 2, 'FIXED', 80000.00, '2025-01-08', '2026-01-07', 103, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(104, 'MS2026010005', '恒通物流诉某运输公司货损赔偿', 'CIVIL_LITIGATION', 'CIVIL', '运输合同纠纷', 104, '贵州某运输有限公司', '代理恒通物流起诉运输公司货物损坏赔偿50万元', 'CLOSED', 2, 4, 2, 'FIXED', 35000.00, '2025-06-12', '2025-12-30', 104, '案件已结案，胜诉', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(105, 'MS2026010006', '茅台镇酒业商标侵权维权', 'CIVIL_LITIGATION', 'CIVIL', '侵害商标权纠纷', 105, '某酒业公司', '代理茅台镇酒业对某酒业公司侵害商标权进行维权', 'ACTIVE', 1, 2, 1, 'FIXED', 100000.00, '2024-11-20', '2025-11-19', 105, 'VIP客户重点案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(106, 'MS2026010007', '张三劳动仲裁案', 'CIVIL_LITIGATION', 'CIVIL', '劳动争议', 107, '贵阳某科技公司', '代理张三申请劳动仲裁，追讨拖欠工资及经济补偿', 'ACTIVE', 3, 3, 2, 'FIXED', 15000.00, '2025-09-05', '2026-03-05', 106, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(107, 'MS2026010008', '李四交通事故损害赔偿案', 'CIVIL_LITIGATION', 'CIVIL', '机动车交通事故责任纠纷', 108, '某保险公司', '代理李四向保险公司索赔交通事故损失', 'ACTIVE', 2, 4, 2, 'CONTINGENCY', 20000.00, '2025-10-18', '2026-04-18', 107, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(108, 'XS2026010001', '刘某盗窃案刑事辩护', 'CRIMINAL', 'CRIMINAL', '盗窃罪', 107, '贵阳市云岩区人民检察院', '为刘某涉嫌盗窃罪提供刑事辩护', 'DRAFT', 2, 5, 2, 'FIXED', 25000.00, '2026-01-10', '2026-07-10', 108, '刑事案件，需谨慎处理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 5. 案件参与人（团队成员）
-- =====================================================
INSERT INTO matter_participant (id, matter_id, user_id, role, commission_rate, remark, created_at, updated_at, deleted)
VALUES
(100, 100, 2, 'LEAD', 40.00, '主办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, 100, 4, 'CO_COUNSEL', 20.00, '协办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, 101, 3, 'LEAD', 40.00, '主办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, 101, 4, 'CO_COUNSEL', 20.00, '协办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, 102, 2, 'LEAD', 50.00, '主办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(105, 102, 3, 'CO_COUNSEL', 25.00, '协办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(106, 103, 3, 'LEAD', 40.00, '主办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(107, 104, 4, 'LEAD', 40.00, '主办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(108, 105, 2, 'LEAD', 40.00, '主办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(109, 106, 3, 'LEAD', 40.00, '主办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(110, 107, 4, 'LEAD', 40.00, '主办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(111, 108, 5, 'LEAD', 40.00, '主办律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 6. 任务数据
-- =====================================================
INSERT INTO task (id, task_no, matter_id, title, description, priority, status, assignee_id, created_by, due_date, completed_at, created_at, updated_at, deleted)
VALUES
-- 茂盛科技法律顾问任务
(100, 'TASK202501001', 100, '审核供应商合同', '审核与新供应商签订的采购合同', 'HIGH', 'COMPLETED', 2, 1, '2025-04-01', '2025-03-28', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, 'TASK202501002', 100, '起草员工手册', '起草公司员工手册法律条款部分', 'MEDIUM', 'IN_PROGRESS', 4, 2, '2026-01-30', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
-- 鼎盛建筑案件任务
(102, 'TASK202501003', 101, '准备起诉材料', '整理工程款纠纷起诉所需证据材料', 'HIGH', 'COMPLETED', 3, 2, '2025-05-25', '2025-05-24', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, 'TASK202501004', 101, '提交立案申请', '向法院提交立案申请', 'HIGH', 'COMPLETED', 3, 2, '2025-05-30', '2025-05-28', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, 'TASK202501005', 101, '准备开庭材料', '准备一审开庭所需代理词、证据目录等', 'HIGH', 'IN_PROGRESS', 3, 2, '2026-02-15', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
-- 盛世矿业股权项目任务
(105, 'TASK202501006', 102, '尽职调查', '对目标矿业公司进行法律尽职调查', 'HIGH', 'COMPLETED', 2, 1, '2024-09-15', '2024-09-10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(106, 'TASK202501007', 102, '起草股权转让协议', '起草60%股权转让协议', 'HIGH', 'COMPLETED', 2, 1, '2024-10-20', '2024-10-18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(107, 'TASK202501008', 102, '办理工商变更', '协助办理股权工商变更登记', 'MEDIUM', 'IN_PROGRESS', 3, 2, '2025-03-01', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
-- 医院法律顾问任务
(108, 'TASK202501009', 103, '医疗纠纷法律意见书', '针对某患者投诉出具法律意见书', 'HIGH', 'TODO', 3, 3, '2026-01-25', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(109, 'TASK202501010', 103, '审核医疗器械采购合同', '审核进口医疗器械采购合同', 'MEDIUM', 'TODO', 3, 3, '2026-02-10', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
-- 恒通物流案件（已结案）
(110, 'TASK202501011', 104, '准备起诉材料', '整理货损赔偿证据', 'HIGH', 'COMPLETED', 4, 2, '2025-06-20', '2025-06-18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(111, 'TASK202501012', 104, '出庭代理', '代理原告出庭', 'HIGH', 'COMPLETED', 4, 2, '2025-10-15', '2025-10-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(112, 'TASK202501013', 104, '申请执行', '案件胜诉后申请强制执行', 'HIGH', 'COMPLETED', 4, 2, '2025-12-20', '2025-12-18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 7. 期限提醒数据
-- =====================================================
INSERT INTO matter_deadline (id, matter_id, deadline_name, deadline_type, base_date, deadline_date, reminder_days, status, description, created_at, updated_at, created_by, deleted)
VALUES
(100, 101, '举证期限', 'EVIDENCE_SUBMISSION', '2026-01-15', '2026-02-01', 7, 'ACTIVE', '鼎盛建筑案一审举证期限', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(101, 101, '开庭日期', 'COURT_HEARING', '2026-01-20', '2026-02-20', 3, 'ACTIVE', '鼎盛建筑案一审开庭', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(102, 105, '答辩期限', 'DEFENSE_SUBMISSION', '2024-12-25', '2025-01-10', 5, 'ACTIVE', '商标侵权案答辩期限', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(103, 106, '仲裁开庭', 'ARBITRATION', '2026-01-10', '2026-01-28', 3, 'ACTIVE', '张三劳动仲裁开庭', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(104, 107, '保险理赔截止', 'OTHER', '2025-12-28', '2026-02-28', 14, 'ACTIVE', '交通事故保险理赔截止日期', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),
(105, 108, '审查起诉期限', 'OTHER', '2026-01-10', '2026-02-10', 7, 'ACTIVE', '刘某案审查起诉期限', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 8. 文档分类
-- =====================================================
INSERT INTO doc_category (id, name, parent_id, description, sort_order, created_at, updated_at, deleted)
VALUES
(100, '合同文书', 0, '各类合同文书', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, '诉讼文书', 0, '诉讼相关文书', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, '法律意见', 0, '法律意见书', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, '证据材料', 0, '证据材料', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, '客户资料', 0, '客户相关资料', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(105, '起诉状', 101, '起诉状模板', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(106, '答辩状', 101, '答辩状模板', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(107, '代理词', 101, '代理词模板', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(108, '上诉状', 101, '上诉状模板', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 9. 档案存放位置
-- =====================================================
INSERT INTO archive_location (id, location_code, location_name, room, cabinet, shelf, total_capacity, used_capacity, status, remarks, created_at, updated_at, deleted)
VALUES
(100, 'A-01-01-01', 'A区1号柜1层1格', 'A区档案室', 'A-01柜', '1层', 50, 10, 'AVAILABLE', '主档案存放区域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, 'A-01-01-02', 'A区1号柜1层2格', 'A区档案室', 'A-01柜', '1层', 50, 20, 'AVAILABLE', 'A区1号档案柜', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, 'A-01-02-01', 'A区1号柜2层1格', 'A区档案室', 'A-01柜', '2层', 50, 15, 'AVAILABLE', 'A区1号档案柜', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, 'B-01-01-01', 'B区1号柜1层1格', 'B区档案室', 'B-01柜', '1层', 30, 5, 'AVAILABLE', '备用档案存放区域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, 'B-01-01-02', 'B区1号柜1层2格', 'B区档案室', 'B-01柜', '1层', 30, 8, 'AVAILABLE', 'B区1号档案柜', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 10. 归档记录
-- =====================================================
INSERT INTO archive (id, archive_no, archive_name, matter_id, archive_type, status, location_id, volume_count, page_count, stored_by, stored_at, retention_period, retention_expire_date, remarks, created_at, updated_at, created_by, deleted)
VALUES
(100, 'DA2025060001', '恒通物流运输合同纠纷案卷宗', 104, 'LITIGATION', 'STORED', 101, 2, 156, 4, '2025-12-30', '10_YEARS', '2035-12-30', '已结案归档', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4, false),
(101, 'DA2024120001', '2024年常年法律顾问工作档案', 100, 'NON_LITIGATION', 'STORED', 102, 1, 45, 2, '2024-12-31', '5_YEARS', '2029-12-31', '年度工作档案', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, false),
(102, 'DA2025010001', '茂盛科技合同审核档案', 100, 'NON_LITIGATION', 'PENDING', 102, 1, 30, NULL, NULL, '10_YEARS', NULL, '待归档', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 11. 会议室
-- =====================================================
INSERT INTO meeting_room (id, name, code, location, capacity, equipment, status, description, created_at, updated_at, deleted)
VALUES
(100, '大会议室', 'MR-001', '5楼东侧', 30, '投影仪,视频会议系统,白板,空调', 'AVAILABLE', '可容纳30人的大型会议室', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, '中会议室A', 'MR-002', '5楼西侧', 15, '投影仪,白板,空调', 'AVAILABLE', '可容纳15人的中型会议室', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, '小会议室', 'MR-003', '4楼', 8, '电视,白板,空调', 'AVAILABLE', '可容纳8人的小型会议室', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 12. 印章管理
-- =====================================================
INSERT INTO seal_info (id, name, seal_type, seal_no, keeper_id, status, description, created_at, updated_at, deleted)
VALUES
(100, '公司公章', 'OFFICIAL', 'GZ2020001', 1, 'ACTIVE', '律所公章，用于重要文件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, '合同专用章', 'CONTRACT', 'HT2020001', 6, 'ACTIVE', '合同专用章，用于签订合同', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, '财务专用章', 'FINANCE', 'CW2020001', 6, 'ACTIVE', '财务专用章，用于财务文件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 13. 法规分类
-- =====================================================
INSERT INTO law_category (id, name, parent_id, level, sort_order, description, created_at, updated_at, deleted)
VALUES
(100, '法律法规', 0, 1, 1, '法律法规汇编', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, '民事法规', 100, 2, 1, '民事相关法律法规', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, '刑事法规', 100, 2, 2, '刑事相关法律法规', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, '行政法规', 100, 2, 3, '行政相关法律法规', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 14. 知识文章
-- =====================================================
INSERT INTO knowledge_article (id, title, category, content, summary, author_id, status, tags, view_count, like_count, created_at, updated_at, deleted)
VALUES
(100, '建设工程合同纠纷案件办理指南', 'GUIDE', '一、案件类型概述\n建设工程合同纠纷是指...\n\n二、证据收集要点\n1. 合同文件\n2. 施工记录\n3. 验收文件\n...', '建设工程合同纠纷案件办理要点总结', 2, 'PUBLISHED', '建设工程,合同纠纷,办案指南', 156, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, '劳动争议案件仲裁流程详解', 'GUIDE', '一、劳动仲裁申请条件\n...\n\n二、仲裁流程\n1. 申请受理\n2. 开庭审理\n3. 裁决作出\n...', '劳动争议仲裁流程全面解析', 3, 'PUBLISHED', '劳动争议,仲裁,流程', 89, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, '最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释', 'LAW', '为正确审理建设工程施工合同纠纷案件...', '建设工程施工合同司法解释', 2, 'PUBLISHED', '司法解释,建设工程', 234, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, '民事起诉状模板', 'TEMPLATE', '民事起诉状\n\n原告：\n被告：\n\n诉讼请求：\n一、...\n二、...\n\n事实与理由：\n...', '民事起诉状标准格式模板', 3, 'PUBLISHED', '起诉状,模板', 567, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, '刑事辩护意见书模板', 'TEMPLATE', '辩护意见书\n\n某某人民法院：\n\n根据《中华人民共和国刑事诉讼法》的规定...', '刑事辩护意见书标准格式', 5, 'PUBLISHED', '刑事辩护,模板', 123, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(105, '商标侵权案件证据收集指南', 'GUIDE', '一、商标权属证据\n1. 商标注册证\n2. 续展证明\n...\n\n二、侵权证据\n1. 侵权商品\n2. 销售记录\n...', '商标侵权案件证据收集方法', 2, 'PUBLISHED', '商标侵权,证据,知识产权', 78, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 15. 收费记录
-- =====================================================
INSERT INTO finance_fee (id, fee_no, fee_name, contract_id, matter_id, client_id, fee_type, amount, paid_amount, planned_date, actual_date, status, remark, created_by, created_at, updated_at, deleted)
VALUES
(100, 'FEE202503001', '第一季度顾问费', 100, 100, 100, 'RETAINER', 30000.00, 30000.00, '2025-03-15', '2025-03-20', 'PAID', '第一季度顾问费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, 'FEE202506001', '第二季度顾问费', 100, 100, 100, 'RETAINER', 30000.00, 30000.00, '2025-06-15', '2025-06-18', 'PAID', '第二季度顾问费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, 'FEE202509001', '第三季度顾问费', 100, 100, 100, 'RETAINER', 30000.00, 30000.00, '2025-09-15', '2025-09-22', 'PAID', '第三季度顾问费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, 'FEE202512001', '第四季度顾问费', 100, 100, 100, 'RETAINER', 30000.00, 0.00, '2025-12-15', NULL, 'PENDING', '第四季度顾问费', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, 'FEE202505001', '前期代理费', 101, 101, 101, 'PROGRESS', 40000.00, 40000.00, '2025-05-20', '2025-05-25', 'PAID', '前期代理费', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(105, 'FEE202506002', '代理费', 104, 104, 104, 'FINAL', 35000.00, 35000.00, '2025-06-12', '2025-06-15', 'PAID', '代理费', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(106, 'FEE202509002', '代理费首付', 106, 106, 107, 'RETAINER', 15000.00, 7500.00, '2025-09-05', '2025-09-08', 'PARTIAL', '代理费首付50%', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 16. 工时记录
-- =====================================================
INSERT INTO timesheet (id, timesheet_no, user_id, matter_id, work_date, hours, hourly_rate, work_content, billable, status, created_at, updated_at, deleted)
VALUES
(100, 'TS202503001', 2, 100, '2025-03-20', 2.50, 500.00, '审核供应商合同3份', true, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, 'TS202504001', 2, 100, '2025-04-15', 1.50, 500.00, '电话咨询劳动法问题', true, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, 'TS202505001', 3, 101, '2025-05-22', 4.00, 450.00, '整理证据材料', true, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, 'TS202505002', 3, 101, '2025-05-24', 3.00, 450.00, '起草起诉状', true, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, 'TS202409001', 2, 102, '2024-09-05', 8.00, 500.00, '尽职调查现场工作', true, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(105, 'TS202409002', 2, 102, '2024-09-08', 6.00, 500.00, '尽职调查报告撰写', true, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(106, 'TS202506001', 4, 104, '2025-06-18', 3.50, 400.00, '准备起诉材料', true, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(107, 'TS202510001', 4, 104, '2025-10-15', 5.00, 400.00, '出庭代理', true, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(108, 'TS202509001', 3, 106, '2025-09-10', 2.00, 450.00, '会见当事人', true, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(109, 'TS202509002', 3, 106, '2025-09-15', 3.00, 450.00, '准备仲裁申请材料', true, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 17. 日程
-- =====================================================
INSERT INTO schedule (id, title, description, schedule_type, start_time, end_time, all_day, location, reminder_minutes, user_id, matter_id, status, created_at, updated_at, deleted)
VALUES
(100, '鼎盛建筑案一审开庭', '贵阳市中级人民法院第三法庭', 'COURT', '2026-02-20 09:00:00', '2026-02-20 12:00:00', false, '贵阳市中级人民法院', 1440, 3, 101, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(101, '张三劳动仲裁开庭', '贵阳市云岩区劳动人事争议仲裁委', 'OTHER', '2026-01-28 14:00:00', '2026-01-28 17:00:00', false, '云岩区仲裁委', 1440, 3, 106, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(102, '茂盛科技季度工作汇报', '向客户汇报本季度法律顾问工作', 'MEETING', '2026-01-25 10:00:00', '2026-01-25 11:30:00', false, '茂盛科技会议室', 60, 2, 100, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(103, '盛世矿业股权项目推进会', '讨论股权变更登记事宜', 'MEETING', '2026-01-22 15:00:00', '2026-01-22 17:00:00', false, '律所大会议室', 60, 2, 102, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(104, '周一部门例会', '本周工作安排', 'MEETING', '2026-01-20 09:00:00', '2026-01-20 10:00:00', false, '律所中会议室', 30, 1, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(105, '李四案保险公司协商', '与保险公司协商赔偿事宜', 'APPOINTMENT', '2026-01-23 14:00:00', '2026-01-23 16:00:00', false, '保险公司办公室', 60, 4, 107, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 18. 更新序列值（确保新增数据不会冲突）
-- =====================================================
SELECT setval('crm_client_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM crm_client), 1), 200));
SELECT setval('crm_client_tag_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM crm_client_tag), 1), 200));
SELECT setval('finance_contract_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM finance_contract), 1), 200));
SELECT setval('matter_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM matter), 1), 200));
SELECT setval('matter_participant_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM matter_participant), 1), 200));
SELECT setval('matter_deadline_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM matter_deadline), 1), 200));
SELECT setval('task_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM task), 1), 200));
SELECT setval('doc_category_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM doc_category), 1), 200));
SELECT setval('archive_location_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM archive_location), 1), 200));
SELECT setval('archive_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM archive), 1), 200));
SELECT setval('meeting_room_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM meeting_room), 1), 200));
SELECT setval('seal_info_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM seal_info), 1), 200));
SELECT setval('law_category_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM law_category), 1), 200));
SELECT setval('knowledge_article_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM knowledge_article), 1), 200));
SELECT setval('finance_fee_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM finance_fee), 1), 200));
SELECT setval('timesheet_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM timesheet), 1), 200));
SELECT setval('schedule_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM schedule), 1), 200));

-- =====================================================
-- 完成提示
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '================================';
  RAISE NOTICE '演示数据初始化完成！';
  RAISE NOTICE '================================';
  RAISE NOTICE '已创建数据：';
  RAISE NOTICE '  - 客户: 10个';
  RAISE NOTICE '  - 合同: 9份';
  RAISE NOTICE '  - 案件/项目: 9个';
  RAISE NOTICE '  - 任务: 13个';
  RAISE NOTICE '  - 期限提醒: 6个';
  RAISE NOTICE '  - 档案: 3个';
  RAISE NOTICE '  - 知识文章: 6篇';
  RAISE NOTICE '  - 收费记录: 7条';
  RAISE NOTICE '  - 工时记录: 10条';
  RAISE NOTICE '  - 日程: 6个';
  RAISE NOTICE '================================';
END $$;
