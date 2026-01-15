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
-- 2. 会议室预约
-- =====================================================
-- 预约状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝, CANCELLED-已取消

INSERT INTO admin_meeting_room_booking (id, room_id, user_id, booking_date, start_time, end_time,
  purpose, attendees, status, approved_by, approved_at, created_at, updated_at, deleted)
VALUES
  -- 已批准的预约
  (101, 101, 2, '2026-01-20', '08:30:00', '09:30:00',
   '诉讼部周例会', '诉讼部全体成员', 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (102, 104, 4, '2026-01-21', '14:30:00', '16:00:00',
   '杭州电商公司来访接待', '李团长,新客户代表', 'APPROVED', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (103, 105, 2, '2026-01-23', '14:00:00', '17:00:00',
   '新民事诉讼法培训', '全体律师', 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (104, 101, 2, '2026-01-31', '16:00:00', '17:30:00',
   '月度全员大会', '全体员工', 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 待审批的预约
  (105, 102, 3, '2026-01-22', '10:00:00', '11:30:00',
   '案件讨论会', '张律师,陈实习', 'PENDING', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (106, 103, 7, '2026-01-24', '14:00:00', '15:00:00',
   '法律文书写作讨论', '陈实习,张律师', 'PENDING', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('admin_meeting_room_booking_id_seq', COALESCE((SELECT MAX(id) FROM admin_meeting_room_booking), 1));

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

INSERT INTO admin_seal_application (id, application_no, applicant_id, seal_id,
  document_name, document_type, seal_count, purpose, matter_id,
  status, approved_by, approved_at, sealed_by, sealed_at,
  remark, created_at, updated_at, deleted)
VALUES
  -- 已完成的用印
  (101, 'SA2026010001', 3, 102,
   '北京科技创新公司法律顾问服务合同', '合同', 2, '客户合同签署', 101,
   'COMPLETED', 2, '2026-01-08 10:00:00', 5, '2026-01-08 14:00:00',
   '一式两份', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (102, 'SA2026010002', 4, 102,
   '上海金融集团并购项目服务合同', '合同', 3, '客户合同签署', 102,
   'COMPLETED', 2, '2026-01-10 09:30:00', 5, '2026-01-10 11:00:00',
   '一式三份，客户、律所、存档各一份', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (103, 'SA2026010003', 3, 101,
   '律师函（催告履约）', '函件', 1, '发送律师函', 101,
   'COMPLETED', 2, '2026-01-12 14:00:00', 2, '2026-01-12 15:30:00',
   NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 已批准待用印
  (104, 'SA2026010004', 4, 101,
   '专利侵权警告函', '函件', 1, '发送警告函', 104,
   'APPROVED', 2, '2026-01-14 09:00:00', NULL, NULL,
   '需要尽快用印发出', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  -- 待审批
  (105, 'SA2026010005', 3, 102,
   '李建军劳动争议代理合同', '合同', 2, '客户合同签署', 105,
   'PENDING', NULL, NULL, NULL, NULL,
   NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (106, 'SA2026010006', 3, 102,
   '王美丽婚姻家事代理合同', '合同', 2, '客户合同签署', 106,
   'PENDING', NULL, NULL, NULL, NULL,
   NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('admin_seal_application_id_seq', COALESCE((SELECT MAX(id) FROM admin_seal_application), 1));

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
  RAISE NOTICE '  - 会议室预约: 6条（4个已通过，2个待审批）';
  RAISE NOTICE '  - 印章: 5个（公章/合同章/财务章/发票章/法代章）';
  RAISE NOTICE '  - 用印申请: 6条（3个已完成，1个已批准，2个待审批）';
  RAISE NOTICE '====================================================';
END $$;
