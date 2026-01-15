-- =====================================================
-- 数据库优化脚本 - 部分索引优化
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-15
-- 描述: 为软删除表添加部分索引，仅索引未删除的记录
-- 优先级: P0 (紧急)
-- 说明: 使用 WHERE deleted = false 可以显著减小索引大小
-- =====================================================

-- =====================================================
-- 1. 系统管理模块部分索引
-- =====================================================

-- sys_user 表：只索引活跃用户
CREATE INDEX IF NOT EXISTS idx_user_active_id ON public.sys_user (id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_user_active_status ON public.sys_user (status) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_user_active_dept ON public.sys_user (department_id) WHERE deleted = false;

-- sys_role 表：只索引未删除的角色
CREATE INDEX IF NOT EXISTS idx_role_active ON public.sys_role (id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_role_active_status ON public.sys_role (status) WHERE deleted = false;

-- sys_menu 表：只索引可见且启用的菜单
CREATE INDEX IF NOT EXISTS idx_menu_visible ON public.sys_menu (parent_id, sort_order)
    WHERE visible = true AND status = 'ENABLED' AND deleted = false;

-- sys_department 表：只索引活跃部门
CREATE INDEX IF NOT EXISTS idx_dept_active ON public.sys_department (id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_dept_active_status ON public.sys_department (status) WHERE deleted = false;

-- sys_config 表：只索引未删除的配置
CREATE INDEX IF NOT EXISTS idx_config_active ON public.sys_config (id) WHERE deleted = false;

-- sys_dict_type 表：只索引启用的字典类型
CREATE INDEX IF NOT EXISTS idx_dict_type_active ON public.sys_dict_type (id) WHERE deleted = false AND status = 'ENABLED';

-- sys_dict_item 表：只索引启用的字典项
CREATE INDEX IF NOT EXISTS idx_dict_item_active ON public.sys_dict_item (id) WHERE deleted = false AND status = 'ENABLED';

-- sys_notification 表：只索引未读通知
CREATE INDEX IF NOT EXISTS idx_notification_unread ON public.sys_notification (receiver_id, created_at DESC)
    WHERE is_read = false AND deleted = false;

-- sys_operation_log 表：按时间分区索引 (最近30天)
CREATE INDEX IF NOT EXISTS idx_operation_log_recent ON public.sys_operation_log (created_at DESC)
    WHERE created_at > CURRENT_DATE - INTERVAL '30 days';

-- sys_login_log 表：按时间分区索引 (最近30天)
CREATE INDEX IF NOT EXISTS idx_login_log_recent ON public.sys_login_log (login_time DESC)
    WHERE login_time > CURRENT_DATE - INTERVAL '30 days';

-- sys_user_session 表：只索引活跃会话
CREATE INDEX IF NOT EXISTS idx_session_active ON public.sys_user_session (user_id, expire_time)
    WHERE status = 'ACTIVE' AND deleted = false;

-- =====================================================
-- 2. 客户管理模块部分索引
-- =====================================================

-- crm_client 表：只索引活跃客户
CREATE INDEX IF NOT EXISTS idx_client_active_id ON public.crm_client (id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_client_active_status ON public.crm_client (status, client_type) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_client_active_lawyer ON public.crm_client (responsible_lawyer_id, status)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_client_potential ON public.crm_client (status, created_at DESC)
    WHERE deleted = false AND status = 'POTENTIAL';

-- crm_contact 表：只索引未删除的联系人和主要联系人
CREATE INDEX IF NOT EXISTS idx_contact_primary ON public.crm_contact (client_id)
    WHERE deleted = false AND is_primary = true;
CREATE INDEX IF NOT EXISTS idx_contact_active ON public.crm_contact (client_id) WHERE deleted = false;

-- crm_lead 表：只索引未删除且未转化的线索
CREATE INDEX IF NOT EXISTS idx_lead_pending ON public.crm_lead (status, created_at DESC)
    WHERE deleted = false AND status IN ('PENDING', 'FOLLOWING');
CREATE INDEX IF NOT EXISTS idx_lead_active_user ON public.crm_lead (responsible_user_id, status)
    WHERE deleted = false;

-- crm_conflict_check 表：只索引待处理的检查
CREATE INDEX IF NOT EXISTS idx_conflict_pending ON public.crm_conflict_check (status, created_at)
    WHERE deleted = false AND status IN ('PENDING', 'CHECKING');

-- crm_client_tag 表：只索引未删除的标签
CREATE INDEX IF NOT EXISTS idx_client_tag_active ON public.crm_client_tag (id) WHERE deleted = false;

-- =====================================================
-- 3. 项目管理模块部分索引
-- =====================================================

-- matter 表：只索引未删除的项目
CREATE INDEX IF NOT EXISTS idx_matter_active_id ON public.matter (id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_matter_active_status ON public.matter (matter_status, matter_type)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_matter_active_lawyer ON public.matter (responsible_lawyer_id, matter_status)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_matter_ongoing ON public.matter (matter_status, updated_at DESC)
    WHERE deleted = false AND matter_status IN ('IN_PROGRESS', 'PENDING');

-- matter_deadline 表：只索引未完成的期限
CREATE INDEX IF NOT EXISTS idx_deadline_pending ON public.matter_deadline (deadline_date, matter_id)
    WHERE deleted = false AND is_completed = false;
CREATE INDEX IF NOT EXISTS idx_deadline_upcoming ON public.matter_deadline (deadline_date)
    WHERE deleted = false AND is_completed = false AND deadline_date >= CURRENT_DATE;

-- matter_participant 表：只索引未删除的参与人
CREATE INDEX IF NOT EXISTS idx_participant_active ON public.matter_participant (matter_id) WHERE deleted = false;

-- =====================================================
-- 4. 财务管理模块部分索引
-- =====================================================

-- finance_contract 表：只索引有效的合同
CREATE INDEX IF NOT EXISTS idx_contract_active_id ON public.finance_contract (id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_contract_active_status ON public.finance_contract (contract_status, signed_date DESC)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_contract_unsigned ON public.finance_contract (contract_status)
    WHERE deleted = false AND contract_status IN ('DRAFT', 'PENDING_APPROVAL');

-- finance_payment 表：只索引未删除的收款
CREATE INDEX IF NOT EXISTS idx_payment_active_id ON public.finance_payment (id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_payment_pending ON public.finance_payment (payment_status, payment_date)
    WHERE deleted = false AND payment_status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_payment_recent ON public.finance_payment (payment_date DESC)
    WHERE deleted = false AND payment_date > CURRENT_DATE - INTERVAL '90 days';

-- finance_commission 表：只索引待审批和待发放的提成
CREATE INDEX IF NOT EXISTS idx_commission_pending ON public.finance_commission (status, created_at)
    WHERE deleted = false AND status IN ('PENDING', 'APPROVED');

-- finance_invoice 表：只索引未开具的发票
CREATE INDEX IF NOT EXISTS idx_invoice_pending ON public.finance_invoice (invoice_status)
    WHERE deleted = false AND invoice_status IN ('PENDING', 'PROCESSING');

-- =====================================================
-- 5. 行政管理模块部分索引
-- =====================================================

-- admin_seal_application 表：只索引待审批的用印申请
CREATE INDEX IF NOT EXISTS idx_seal_pending ON public.admin_seal_application (application_status, created_at)
    WHERE deleted = false AND application_status = 'PENDING';

-- admin_meeting_room_reservation 表：只索引未来的预约
CREATE INDEX IF NOT EXISTS idx_meeting_upcoming ON public.admin_meeting_room_reservation (room_id, start_time)
    WHERE deleted = false AND start_time >= CURRENT_TIMESTAMP;

-- =====================================================
-- 6. 工时管理模块部分索引
-- =====================================================

-- timesheet 表：只索引最近3个月的工时
CREATE INDEX IF NOT EXISTS idx_timesheet_recent ON public.timesheet (work_date DESC, user_id)
    WHERE deleted = false AND work_date > CURRENT_DATE - INTERVAL '90 days';

-- timer_session 表：只索引活跃的计时会话
CREATE INDEX IF NOT EXISTS idx_timer_active ON public.timer_session (user_id, start_time DESC)
    WHERE deleted = false AND end_time IS NULL;

-- =====================================================
-- 7. 任务管理模块部分索引
-- =====================================================

-- task 表：只索引未完成的任务
CREATE INDEX IF NOT EXISTS idx_task_pending ON public.task (status, due_date)
    WHERE deleted = false AND status IN ('PENDING', 'IN_PROGRESS');
CREATE INDEX IF NOT EXISTS idx_task_active_user ON public.task (assignee_id, status)
    WHERE deleted = false AND status IN ('PENDING', 'IN_PROGRESS');

-- =====================================================
-- 8. 人力资源模块部分索引
-- =====================================================

-- hr_employee 表：只索引在职员工
CREATE INDEX IF NOT EXISTS idx_employee_active ON public.hr_employee (id) WHERE deleted = false AND status = 'ACTIVE';

-- hr_attendance 表：只索引最近6个月的考勤
CREATE INDEX IF NOT EXISTS idx_attendance_recent ON public.hr_attendance (attendance_date DESC, employee_id)
    WHERE deleted = false AND attendance_date > CURRENT_DATE - INTERVAL '180 days';

-- hr_payroll 表：只索引最近12个月的薪酬
CREATE INDEX IF NOT EXISTS idx_payroll_recent ON public.hr_payroll (pay_period DESC, employee_id)
    WHERE deleted = false AND pay_period > CURRENT_DATE - INTERVAL '365 days';

-- =====================================================
-- 9. 档案管理模块部分索引
-- =====================================================

-- archive 表：只索引未借出的档案
CREATE INDEX IF NOT EXISTS idx_archive_available ON public.archive (archive_status, archive_no)
    WHERE deleted = false AND archive_status = 'STORED';

-- archive_borrow 表：只索引未归还的借阅记录
CREATE INDEX IF NOT EXISTS idx_borrow_active ON public.archive_borrow (archive_id)
    WHERE deleted = false AND return_date IS NULL;

-- =====================================================
-- 10. 文档管理模块部分索引
-- =====================================================

-- doc_file 表：只索引未删除的文档
CREATE INDEX IF NOT EXISTS idx_doc_active_id ON public.doc_file (id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_doc_active_matter ON public.doc_file (matter_id, created_at DESC) WHERE deleted = false;

-- doc_version 表：只索引最新版本
CREATE INDEX IF NOT EXISTS idx_doc_version_latest ON public.doc_version (file_id, version_number DESC)
    WHERE deleted = false AND is_latest = true;

-- =====================================================
-- 11. 知识库模块部分索引
-- =====================================================

-- kb_article 表：只索引已发布的文章
CREATE INDEX IF NOT EXISTS idx_article_published ON public.kb_article (category_id, published_at DESC)
    WHERE deleted = false AND status = 'PUBLISHED';

-- =====================================================
-- 12. 质量管理模块部分索引
-- =====================================================

-- quality_check 表：只索引待处理的检查
CREATE INDEX IF NOT EXISTS idx_quality_pending ON public.quality_check (check_status, check_date)
    WHERE deleted = false AND check_status IN ('PENDING', 'IN_PROGRESS');

-- quality_risk 表：只索引未消除的风险
CREATE INDEX IF NOT EXISTS idx_risk_active ON public.quality_risk (risk_level, status)
    WHERE deleted = false AND status = 'OPEN';

-- =====================================================
-- 13. 工作台模块部分索引
-- =====================================================

-- wb_approval 表：只索引待审批和进行中的审批
CREATE INDEX IF NOT EXISTS idx_approval_pending ON public.wb_approval (approval_status, created_at)
    WHERE deleted = false AND approval_status IN ('PENDING', 'IN_PROGRESS');

-- wb_schedule 表：只索引未来的日程
CREATE INDEX IF NOT EXISTS idx_schedule_upcoming ON public.wb_schedule (start_time)
    WHERE deleted = false AND start_time >= CURRENT_DATE;

-- =====================================================
-- 14. 合同模板模块部分索引
-- =====================================================

-- contract_template 表：只索引启用的模板
CREATE INDEX IF NOT EXISTS idx_template_active ON public.contract_template (id)
    WHERE deleted = false AND status = 'ACTIVE';

-- =====================================================
-- 完成标记
-- =====================================================

DO $$
DECLARE
    idx_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO idx_count
    FROM pg_indexes
    WHERE schemaname = 'public'
    AND indexname LIKE 'idx_%_active'
    OR indexname LIKE 'idx_%_pending'
    OR indexname LIKE 'idx_%_upcoming'
    OR indexname LIKE 'idx_%_recent';

    RAISE NOTICE '========================================';
    RAISE NOTICE '部分索引优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已创建 % 个部分索引', idx_count;
    RAISE NOTICE '';
    RAISE NOTICE '部分索引的优点:';
    RAISE NOTICE '  1. 减小索引大小，节省存储空间';
    RAISE NOTICE '  2. 提高查询性能，扫描更少的索引页';
    RAISE NOTICE '  3. 减少索引维护开销';
    RAISE NOTICE '';
    RAISE NOTICE '注意: 查询必须包含 WHERE 条件才能使用部分索引';
END $$;
