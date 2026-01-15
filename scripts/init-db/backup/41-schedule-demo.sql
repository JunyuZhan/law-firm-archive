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

INSERT INTO schedule_event (id, title, description, event_type, start_time, end_time, 
  all_day, location, matter_id, user_id, reminder_minutes, reminder_sent, 
  status, created_at, updated_at, deleted)
VALUES
  -- 庭审日程
  (101, '广州制造业劳动争议案开庭', 
   '广州制造业公司劳动争议案第一次开庭审理，需提前准备答辩材料和证据',
   'COURT_HEARING', '2026-01-20 09:30:00', '2026-01-20 12:00:00',
   false, '广州市中级人民法院第三法庭', 103, 3, 60, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (102, '专利侵权案证据交换', 
   '深圳互联网公司专利侵权案证据交换，准备证据副本3份',
   'COURT_HEARING', '2026-01-22 14:00:00', '2026-01-22 16:00:00',
   false, '深圳市知识产权法庭', 104, 4, 120, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (103, '离婚诉讼案调解', 
   '王美丽离婚案调解，法院主持调解',
   'COURT_HEARING', '2026-01-25 10:00:00', '2026-01-25 11:30:00',
   false, '北京市朝阳区人民法院调解室', 106, 3, 60, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 客户会议
  (104, '北京科技公司季度法律顾问会议', 
   '2026年Q1法律顾问工作汇报，讨论合规风险及下阶段工作计划',
   'MEETING', '2026-01-18 14:00:00', '2026-01-18 16:00:00',
   false, '北京科技创新公司会议室', 101, 3, 30, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (105, '上海金融集团并购项目进度汇报', 
   '并购项目阶段性汇报，介绍尽职调查发现及后续建议',
   'MEETING', '2026-01-19 10:00:00', '2026-01-19 12:00:00',
   false, '上海金融中心大厦28楼会议室', 102, 4, 60, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 客户会见
  (106, '李建军案情沟通', 
   '与当事人李建军沟通仲裁裁决情况及后续安排',
   'CLIENT_VISIT', '2026-01-17 15:00:00', '2026-01-17 16:00:00',
   false, '律所接待室A', 105, 3, 30, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (107, '新客户咨询接待', 
   '杭州电商公司法务总监来所咨询合作事宜',
   'CLIENT_VISIT', '2026-01-21 14:30:00', '2026-01-21 16:00:00',
   false, '律所贵宾接待室', NULL, 4, 30, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 内部会议
  (108, '诉讼部周例会', 
   '本周案件进展汇报，下周工作安排',
   'MEETING', '2026-01-20 08:30:00', '2026-01-20 09:30:00',
   false, '律所大会议室', NULL, 3, 15, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (109, '律所全员月度大会', 
   '1月份工作总结，2月份工作部署',
   'MEETING', '2026-01-31 16:00:00', '2026-01-31 17:30:00',
   false, '律所多功能厅', NULL, 2, 60, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 培训
  (110, '新民事诉讼法解读培训', 
   '邀请法官讲解新民事诉讼法重点修改内容',
   'TRAINING', '2026-01-23 14:00:00', '2026-01-23 17:00:00',
   false, '律所培训教室', NULL, 7, 30, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (111, '知识产权诉讼实务分享', 
   '李团长分享知识产权诉讼经验',
   'TRAINING', '2026-01-28 15:00:00', '2026-01-28 16:30:00',
   false, '律所小会议室', NULL, 4, 30, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 截止日期提醒
  (112, '劳动仲裁答辩状提交截止', 
   '广州制造业劳动争议案答辩状提交截止日',
   'DEADLINE', '2026-01-18 00:00:00', '2026-01-18 23:59:59',
   true, NULL, 103, 3, 1440, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (113, '专利侵权起诉状提交截止', 
   '深圳互联网公司专利侵权案起诉状提交截止',
   'DEADLINE', '2026-01-25 00:00:00', '2026-01-25 23:59:59',
   true, NULL, 104, 4, 1440, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

  -- 其他事项
  (114, '律师证年检', 
   '完成律师执业证年度检验',
   'OTHER', '2026-01-30 00:00:00', '2026-01-30 23:59:59',
   true, '司法局', NULL, 3, 1440, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
  
  (115, '案例库更新', 
   '整理2025年度典型案例入库',
   'OTHER', '2026-01-27 09:00:00', '2026-01-27 18:00:00',
   false, '办公室', NULL, 7, 60, false,
   'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

SELECT setval('schedule_event_id_seq', COALESCE((SELECT MAX(id) FROM schedule_event), 1));

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
