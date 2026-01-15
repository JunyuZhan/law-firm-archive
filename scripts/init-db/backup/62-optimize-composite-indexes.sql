-- =====================================================
-- 数据库优化脚本 - 复合索引优化
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-15
-- 描述: 为常见查询模式添加复合索引
-- 优先级: P1 (高优先级)
-- 说明: 复合索引可以显著提高多条件查询性能
-- =====================================================

-- =====================================================
-- 1. 系统管理模块复合索引
-- =====================================================

-- 用户查询：按部门+状态查询
CREATE INDEX IF NOT EXISTS idx_user_dept_status ON public.sys_user (department_id, status, created_at DESC)
    WHERE deleted = false;

-- 用户查询：按角色+状态查询 (通过关联)
CREATE INDEX IF NOT EXISTS idx_user_role_status ON public.sys_user_role (role_id)
    INCLUDE (user_id);

-- 菜单查询：按角色+可见性查询
CREATE INDEX IF NOT EXISTS idx_menu_role_visible ON public.sys_role_menu (role_id)
    INCLUDE (menu_id);

-- 操作日志：按用户+模块+时间查询
CREATE INDEX IF NOT EXISTS idx_operation_log_user_module_time
    ON public.sys_operation_log (user_id, module, created_at DESC)
    WHERE created_at > CURRENT_DATE - INTERVAL '90 days';

-- 操作日志：按模块+操作类型+时间查询
CREATE INDEX IF NOT EXISTS idx_operation_log_module_type_time
    ON public.sys_operation_log (module, operation_type, created_at DESC)
    WHERE created_at > CURRENT_DATE - INTERVAL '90 days';

-- 登录日志：按用户+状态+时间查询
CREATE INDEX IF NOT EXISTS idx_login_log_user_status_time
    ON public.sys_login_log (user_id, status, login_time DESC);

-- 通知查询：按接收人+类型+已读状态查询
CREATE INDEX IF NOT EXISTS idx_notification_receiver_type_read
    ON public.sys_notification (receiver_id, type, is_read, created_at DESC)
    WHERE deleted = false;

-- =====================================================
-- 2. 客户管理模块复合索引
-- =====================================================

-- 客户查询：按状态+类型+创建时间查询
CREATE INDEX IF NOT EXISTS idx_client_status_type_time
    ON public.crm_client (status, client_type, created_at DESC)
    WHERE deleted = false;

-- 客户查询：按负责律师+状态查询
CREATE INDEX IF NOT EXISTS idx_client_lawyer_status
    ON public.crm_client (responsible_lawyer_id, status, updated_at DESC)
    WHERE deleted = false;

-- 客户查询：按级别+状态查询
CREATE INDEX IF NOT EXISTS idx_client_level_status
    ON public.crm_client (level, status, created_at DESC)
    WHERE deleted = false;

-- 客户查询：按案源人+状态查询
CREATE INDEX IF NOT EXISTS idx_client_originator_status
    ON public.crm_client (originator_id, status, created_at DESC)
    WHERE deleted = false;

-- 联系人记录：按客户+日期查询
CREATE INDEX IF NOT EXISTS idx_contact_record_client_date
    ON public.crm_client_contact_record (client_id, contact_date DESC)
    WHERE deleted = false;

-- 联系人记录：按下次跟进日期查询
CREATE INDEX IF NOT EXISTS idx_contact_record_followup
    ON public.crm_client_contact_record (next_follow_up_date, follow_up_reminder)
    WHERE deleted = false AND next_follow_up_date IS NOT NULL;

-- 案源查询：按负责人+状态+优先级查询
CREATE INDEX IF NOT EXISTS idx_lead_user_status_priority
    ON public.crm_lead (responsible_user_id, status, priority, created_at DESC)
    WHERE deleted = false;

-- 案源查询：按来源渠道+状态查询
CREATE INDEX IF NOT EXISTS idx_lead_source_status
    ON public.crm_lead (source_channel, status, created_at DESC)
    WHERE deleted = false;

-- 利冲检查：按客户+状态查询
CREATE INDEX IF NOT EXISTS idx_conflict_check_client_status
    ON public.crm_conflict_check (client_id, status, created_at DESC)
    WHERE deleted = false;

-- 利冲检查：按项目+状态查询
CREATE INDEX IF NOT EXISTS idx_conflict_check_matter_status
    ON public.crm_conflict_check (matter_id, status, created_at DESC)
    WHERE deleted = false;

-- =====================================================
-- 3. 项目管理模块复合索引
-- =====================================================

-- 项目查询：按客户+状态查询
CREATE INDEX IF NOT EXISTS idx_matter_client_status
    ON public.matter (client_id, matter_status, created_at DESC)
    WHERE deleted = false;

-- 项目查询：按负责律师+状态+类型查询
CREATE INDEX IF NOT EXISTS idx_matter_lawyer_status_type
    ON public.matter (responsible_lawyer_id, matter_status, matter_type, updated_at DESC)
    WHERE deleted = false;

-- 项目查询：按类型+状态+创建时间查询
CREATE INDEX IF NOT EXISTS idx_matter_type_status_time
    ON public.matter (matter_type, matter_status, created_at DESC)
    WHERE deleted = false;

-- 项目参与人：按项目+角色查询
CREATE INDEX IF NOT EXISTS idx_participant_matter_role
    ON public.matter_participant (matter_id, participant_role)
    WHERE deleted = false;

-- 项目参与人：按用户+项目查询
CREATE INDEX IF NOT EXISTS idx_participant_user_matter
    ON public.matter_participant (user_id, matter_id)
    WHERE deleted = false;

-- 项目期限：按项目+完成状态+到期日期查询
CREATE INDEX IF NOT EXISTS idx_deadline_matter_completed_date
    ON public.matter_deadline (matter_id, is_completed, deadline_date)
    WHERE deleted = false;

-- 项目客户关联：按客户查询所有项目
CREATE INDEX IF NOT EXISTS idx_matter_client_client_matter
    ON public.matter_client (client_id, matter_id)
    WHERE deleted = false;

-- =====================================================
-- 4. 财务管理模块复合索引
-- =====================================================

-- 合同查询：按客户+状态查询
CREATE INDEX IF NOT EXISTS idx_contract_client_status
    ON public.finance_contract (client_id, contract_status, signed_date DESC)
    WHERE deleted = false;

-- 合同查询：按项目+状态查询
CREATE INDEX IF NOT EXISTS idx_contract_matter_status
    ON public.finance_contract (matter_id, contract_status, signed_date DESC)
    WHERE deleted = false;

-- 合同查询：按类型+状态查询
CREATE INDEX IF NOT EXISTS idx_contract_type_status
    ON public.finance_contract (contract_type, contract_status, signed_date DESC)
    WHERE deleted = false;

-- 收款查询：按合同+状态查询
CREATE INDEX IF NOT EXISTS idx_payment_contract_status
    ON public.finance_payment (contract_id, payment_status, payment_date DESC)
    WHERE deleted = false;

-- 收款查询：按客户+状态查询
CREATE INDEX IF NOT EXISTS idx_payment_client_status
    ON public.finance_payment (client_id, payment_status, payment_date DESC)
    WHERE deleted = false;

-- 收款查询：按收款方式+状态+日期查询
CREATE INDEX IF NOT EXISTS idx_payment_method_status_date
    ON public.finance_payment (payment_method, payment_status, payment_date DESC)
    WHERE deleted = false;

-- 提成查询：按案源人+状态查询
CREATE INDEX IF NOT EXISTS idx_commission_originator_status
    ON public.finance_commission (originator_id, status, created_at DESC)
    WHERE deleted = false;

-- 提成查询：按合同+状态查询
CREATE INDEX IF NOT EXISTS idx_commission_contract_status
    ON public.finance_commission (contract_id, status, created_at DESC)
    WHERE deleted = false;

-- 发票查询：按客户+状态查询
CREATE INDEX IF NOT EXISTS idx_invoice_client_status
    ON public.finance_invoice (client_id, invoice_status, issue_date DESC)
    WHERE deleted = false;

-- 发票查询：按收款+状态查询
CREATE INDEX IF NOT EXISTS idx_invoice_payment_status
    ON public.finance_invoice (payment_id, invoice_status, issue_date DESC)
    WHERE deleted = false;

-- =====================================================
-- 5. 行政管理模块复合索引
-- =====================================================

-- 用印申请：按申请人+状态查询
CREATE INDEX IF NOT EXISTS idx_seal_applicant_status
    ON public.admin_seal_application (applicant_id, application_status, created_at DESC)
    WHERE deleted = false;

-- 用印申请：按类型+状态查询
CREATE INDEX IF NOT EXISTS idx_seal_type_status
    ON public.admin_seal_application (seal_type, application_status, created_at DESC)
    WHERE deleted = false;

-- 会议室预约：按房间+时间段查询
CREATE INDEX IF NOT EXISTS idx_meeting_room_time
    ON public.admin_meeting_room_reservation (room_id, start_time, end_time)
    WHERE deleted = false AND start_time >= CURRENT_DATE;

-- =====================================================
-- 6. 工时管理模块复合索引
-- =====================================================

-- 工时查询：按用户+日期范围查询
CREATE INDEX IF NOT EXISTS idx_timesheet_user_date
    ON public.timesheet (user_id, work_date DESC)
    WHERE deleted = false AND work_date > CURRENT_DATE - INTERVAL '180 days';

-- 工时查询：按项目+日期查询
CREATE INDEX IF NOT EXISTS idx_timesheet_matter_date
    ON public.timesheet (matter_id, work_date DESC)
    WHERE deleted = false;

-- 工时汇总：按用户+年月查询
CREATE INDEX IF NOT EXISTS idx_timesheet_user_ym
    ON public.timesheet (user_id, work_year, work_month)
    WHERE deleted = false;

-- =====================================================
-- 7. 任务管理模块复合索引
-- =====================================================

-- 任务查询：按分配人+状态+截止日期查询
CREATE INDEX IF NOT EXISTS idx_task_assignee_status_due
    ON public.task (assignee_id, status, due_date)
    WHERE deleted = false AND status IN ('PENDING', 'IN_PROGRESS');

-- 任务查询：按项目+状态查询
CREATE INDEX IF NOT EXISTS idx_task_matter_status
    ON public.task (matter_id, status, due_date)
    WHERE deleted = false;

-- 任务查询：按创建人+状态查询
CREATE INDEX IF NOT EXISTS idx_task_creator_status
    ON public.task (created_by, status, created_at DESC)
    WHERE deleted = false;

-- =====================================================
-- 8. 人力资源模块复合索引
-- =====================================================

-- 考勤查询：按员工+日期范围查询
CREATE INDEX IF NOT EXISTS idx_attendance_employee_date
    ON public.hr_attendance (employee_id, attendance_date DESC)
    WHERE deleted = false AND attendance_date > CURRENT_DATE - INTERVAL '365 days';

-- 考勤查询：按部门+日期查询
CREATE INDEX IF NOT EXISTS idx_attendance_dept_date
    ON public.hr_attendance (department_id, attendance_date DESC)
    WHERE deleted = false AND attendance_date > CURRENT_DATE - INTERVAL '90 days';

-- 薪酬查询：按员工+期间查询
CREATE INDEX IF NOT EXISTS idx_payroll_employee_period
    ON public.hr_payroll (employee_id, pay_period DESC)
    WHERE deleted = false;

-- 绩效查询：按员工+考核周期查询
CREATE INDEX IF NOT EXISTS idx_performance_employee_cycle
    ON public.hr_performance (employee_id, assessment_cycle DESC)
    WHERE deleted = false;

-- =====================================================
-- 9. 档案管理模块复合索引
-- =====================================================

-- 档案查询：按项目+状态查询
CREATE INDEX IF NOT EXISTS idx_archive_matter_status
    ON public.archive (matter_id, archive_status, archived_date DESC)
    WHERE deleted = false;

-- 档案查询：按存放位置+状态查询
CREATE INDEX IF NOT EXISTS idx_archive_location_status
    ON public.archive (storage_location_id, archive_status)
    WHERE deleted = false;

-- 借阅查询：按档案+状态查询
CREATE INDEX IF NOT EXISTS idx_borrow_archive_status
    ON public.archive_borrow (archive_id, return_date)
    WHERE deleted = false AND return_date IS NULL;

-- 借阅查询：按借阅人+状态查询
CREATE INDEX IF NOT EXISTS idx_borrow_borrower_status
    ON public.archive_borrow (borrower_id, return_date)
    WHERE deleted = false AND return_date IS NULL;

-- =====================================================
-- 10. 文档管理模块复合索引
-- =====================================================

-- 文档查询：按项目+类型+创建时间查询
CREATE INDEX IF NOT EXISTS idx_doc_matter_type_time
    ON public.doc_file (matter_id, doc_type, created_at DESC)
    WHERE deleted = false;

-- 文档查询：按创建人+创建时间查询
CREATE INDEX IF NOT EXISTS idx_doc_creator_time
    ON public.doc_file (created_by, created_at DESC)
    WHERE deleted = false;

-- 文档版本：按文档+版本号查询
CREATE INDEX IF NOT EXISTS idx_doc_version_file_number
    ON public.doc_version (file_id, version_number DESC)
    WHERE deleted = false;

-- =====================================================
-- 11. 知识库模块复合索引
-- =====================================================

-- 文章查询：按分类+状态+发布时间查询
CREATE INDEX IF NOT EXISTS idx_article_category_status_time
    ON public.kb_article (category_id, status, published_at DESC)
    WHERE deleted = false AND status = 'PUBLISHED';

-- 文章查询：按标签+状态查询 (通过关联)
CREATE INDEX IF NOT EXISTS idx_article_tag_article
    ON public.kb_article_tag_relation (tag_id, article_id)
    WHERE deleted = false;

-- =====================================================
-- 12. 质量管理模块复合索引
-- =====================================================

-- 质量检查：按项目+状态查询
CREATE INDEX IF NOT EXISTS idx_quality_check_matter_status
    ON public.quality_check (matter_id, check_status, check_date DESC)
    WHERE deleted = false;

-- 质量检查：按检查人+状态查询
CREATE INDEX IF NOT EXISTS idx_quality_check_checker_status
    ON public.quality_check (checked_by, check_status, check_date DESC)
    WHERE deleted = false;

-- 风险预警：按级别+状态查询
CREATE INDEX IF NOT EXISTS idx_risk_level_status
    ON public.quality_risk (risk_level, status, created_at DESC)
    WHERE deleted = false AND status = 'OPEN';

-- =====================================================
-- 13. 工作台模块复合索引
-- =====================================================

-- 审批查询：按申请人+状态查询
CREATE INDEX IF NOT EXISTS idx_approval_applicant_status
    ON public.wb_approval (applicant_id, approval_status, created_at DESC)
    WHERE deleted = false AND approval_status IN ('PENDING', 'IN_PROGRESS');

-- 审批查询：按审批人+状态查询
CREATE INDEX IF NOT EXISTS idx_approval_approver_status
    ON public.wb_approval_approver (approver_id, approval_status, created_at DESC)
    WHERE deleted = false AND approval_status IN ('PENDING', 'IN_PROGRESS');

-- 日程查询：按参与人+时间范围查询
CREATE INDEX IF NOT EXISTS idx_schedule_participant_time
    ON public.wb_schedule_participant (user_id, start_time)
    WHERE deleted = false AND start_time >= CURRENT_DATE;

-- 日程查询：按创建人+时间查询
CREATE INDEX IF NOT EXISTS idx_schedule_creator_time
    ON public.wb_schedule (created_by, start_time)
    WHERE deleted = false AND start_time >= CURRENT_DATE;

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
    AND indexname LIKE 'idx_%';

    RAISE NOTICE '========================================';
    RAISE NOTICE '复合索引优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已创建/检查复合索引';
    RAISE NOTICE '';
    RAISE NOTICE '复合索引使用建议:';
    RAISE NOTICE '  1. 索引列顺序应遵循查询模式';
    RAISE NOTICE '  2. 等值查询列放在前面，范围查询列放在后面';
    RAISE NOTICE '  3. 使用 INCLUDE 添加非键列(覆盖索引)';
    RAISE NOTICE '  4. 定期使用 EXPLAIN ANALYZE 检查查询计划';
END $$;
