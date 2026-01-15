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
