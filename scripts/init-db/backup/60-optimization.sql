-- =====================================================
-- 数据库性能优化脚本（整合版）
-- =====================================================
-- 版本: 2.0.0
-- 日期: 2026-01-15
-- 描述: 整合所有数据库优化功能
-- 优先级: P0+P1+P2
-- 说明: 此脚本包含以下优化：
--   - P0: 外键约束、部分索引
--   - P1: 复合索引、检查约束、日志分区
--   - P2: 全文搜索、物化视图、触发器
-- =====================================================


-- =====================================================
-- 合并自: 60-optimize-foreign-keys.sql
-- =====================================================

-- =====================================================

-- crm_client 表外键
ALTER TABLE public.crm_client
    ADD CONSTRAINT fk_client_originator
    FOREIGN KEY (originator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_client_responsible_lawyer
    FOREIGN KEY (responsible_lawyer_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_client_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_client_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

COMMENT ON CONSTRAINT fk_client_originator ON public.crm_client IS '案源人关联';
COMMENT ON CONSTRAINT fk_client_responsible_lawyer ON public.crm_client IS '负责律师关联';

-- crm_client_change_history 表外键
ALTER TABLE public.crm_client_change_history
    ADD CONSTRAINT fk_client_change_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_client_change_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_client_change_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_client_contact_record 表外键
ALTER TABLE public.crm_client_contact_record
    ADD CONSTRAINT fk_contact_record_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_contact_record_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_client_related_company 表外键
ALTER TABLE public.crm_client_related_company
    ADD CONSTRAINT fk_related_company_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE;

-- crm_client_shareholder 表外键
ALTER TABLE public.crm_client_shareholder
    ADD CONSTRAINT fk_shareholder_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE;

-- crm_client_tag_relation 表外键
ALTER TABLE public.crm_client_tag_relation
    ADD CONSTRAINT fk_client_tag_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_client_tag_tag
    FOREIGN KEY (tag_id) REFERENCES public.crm_client_tag(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_client_tag_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_contact 表外键
ALTER TABLE public.crm_contact
    ADD CONSTRAINT fk_contact_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_contact_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_contact_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_conflict_check 表外键
ALTER TABLE public.crm_conflict_check
    ADD CONSTRAINT fk_conflict_check_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_conflict_check_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_conflict_check_applicant
    FOREIGN KEY (applicant_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_conflict_check_reviewer
    FOREIGN KEY (reviewer_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_conflict_check_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_conflict_check_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_conflict_check_item 表外键
ALTER TABLE public.crm_conflict_check_item
    ADD CONSTRAINT fk_conflict_item_check
    FOREIGN KEY (check_id) REFERENCES public.crm_conflict_check(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_conflict_item_matter
    FOREIGN KEY (related_matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_conflict_item_client
    FOREIGN KEY (related_client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_conflict_item_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_conflict_item_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_lead 表外键
ALTER TABLE public.crm_lead
    ADD CONSTRAINT fk_lead_originator
    FOREIGN KEY (originator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_lead_responsible_user
    FOREIGN KEY (responsible_user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_lead_converted_client
    FOREIGN KEY (converted_to_client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_lead_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_lead_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

COMMENT ON CONSTRAINT fk_lead_originator ON public.crm_lead IS '案源人关联';
COMMENT ON CONSTRAINT fk_lead_responsible_user ON public.crm_lead IS '负责人关联';

-- crm_lead_follow_up 表外键
ALTER TABLE public.crm_lead_follow_up
    ADD CONSTRAINT fk_lead_follow_up_lead
    FOREIGN KEY (lead_id) REFERENCES public.crm_lead(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_lead_follow_up_user
    FOREIGN KEY (follow_user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_lead_follow_up_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 2. 项目管理模块 (Matter) 外键
-- =====================================================

-- matter 表外键
ALTER TABLE public.matter
    ADD CONSTRAINT fk_matter_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_matter_responsible_lawyer
    FOREIGN KEY (responsible_lawyer_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_matter_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_matter_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

COMMENT ON CONSTRAINT fk_matter_responsible_lawyer ON public.matter IS '负责律师关联';

-- matter_participant 表外键
ALTER TABLE public.matter_participant
    ADD CONSTRAINT fk_participant_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_participant_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- matter_deadline 表外键
ALTER TABLE public.matter_deadline
    ADD CONSTRAINT fk_deadline_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_deadline_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- matter_client 表外键
ALTER TABLE public.matter_client
    ADD CONSTRAINT fk_matter_client_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_matter_client_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE;

-- =====================================================
-- 3. 财务管理模块 (Finance) 外键
-- =====================================================

-- finance_contract 表外键
ALTER TABLE public.finance_contract
    ADD CONSTRAINT fk_contract_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_contract_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_contract_originator
    FOREIGN KEY (originator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_contract_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_contract_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_payment 表外键
ALTER TABLE public.finance_payment
    ADD CONSTRAINT fk_payment_contract
    FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_payment_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_payment_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_payment_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_payment_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_payment_amendment 表外键
ALTER TABLE public.finance_payment_amendment
    ADD CONSTRAINT fk_payment_amendment_payment
    FOREIGN KEY (payment_id) REFERENCES public.finance_payment(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_payment_amendment_requested_by
    FOREIGN KEY (requested_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_payment_amendment_approved_by
    FOREIGN KEY (approved_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_commission 表外键
ALTER TABLE public.finance_commission
    ADD CONSTRAINT fk_commission_payment
    FOREIGN KEY (payment_id) REFERENCES public.finance_payment(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_fee
    FOREIGN KEY (fee_id) REFERENCES public.finance_fee(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_contract
    FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_rule
    FOREIGN KEY (rule_id) REFERENCES public.hr_commission_rule(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_originator
    FOREIGN KEY (originator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_approved_by
    FOREIGN KEY (approved_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_paid_by
    FOREIGN KEY (paid_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_commission_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_commission_detail 表外键
ALTER TABLE public.finance_commission_detail
    ADD CONSTRAINT fk_commission_detail_commission
    FOREIGN KEY (commission_id) REFERENCES public.finance_commission(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_commission_detail_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_invoice 表外键
ALTER TABLE public.finance_invoice
    ADD CONSTRAINT fk_invoice_contract
    FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_invoice_payment
    FOREIGN KEY (payment_id) REFERENCES public.finance_payment(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_invoice_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_invoice_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_invoice_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 4. 行政管理模块 (Admin) 外键
-- =====================================================

-- seal_application 表外键
ALTER TABLE public.seal_application
    ADD CONSTRAINT fk_seal_applicant
    FOREIGN KEY (applicant_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_seal_approver
    FOREIGN KEY (approved_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_seal_seal
    FOREIGN KEY (seal_id) REFERENCES public.admin_seal(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_seal_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_seal_used_by
    FOREIGN KEY (used_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- 注：会议室预约功能暂未实现，需要创建 meeting_room_reservation 表
-- ALTER TABLE public.meeting_room_reservation
--     ADD CONSTRAINT fk_meeting_room
--     FOREIGN KEY (room_id) REFERENCES public.meeting_room(id) ON DELETE CASCADE,
--     ADD CONSTRAINT fk_meeting_organizer
--     FOREIGN KEY (organizer_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 5. 工时管理模块 (Timesheet) 外键
-- =====================================================

-- timesheet 表外键
ALTER TABLE public.timesheet
    ADD CONSTRAINT fk_timesheet_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_timesheet_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_timesheet_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- timer_session 表外键
ALTER TABLE public.timer_session
    ADD CONSTRAINT fk_timer_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_timer_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL;

-- =====================================================
-- 6. 任务管理模块 (Task) 外键
-- =====================================================

-- task 表外键
ALTER TABLE public.task
    ADD CONSTRAINT fk_task_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_task_assignee
    FOREIGN KEY (assignee_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_task_creator
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- task_comment 表外键
ALTER TABLE public.task_comment
    ADD CONSTRAINT fk_task_comment_task
    FOREIGN KEY (task_id) REFERENCES public.task(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_task_comment_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 7. 人力资源模块 (HR) 外键
-- =====================================================

-- hr_employee 表外键
ALTER TABLE public.hr_employee
    ADD CONSTRAINT fk_employee_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_employee_department
    FOREIGN KEY (department_id) REFERENCES public.sys_department(id) ON DELETE SET NULL;

-- hr_attendance 表外键
ALTER TABLE public.hr_attendance
    ADD CONSTRAINT fk_attendance_employee
    FOREIGN KEY (employee_id) REFERENCES public.hr_employee(id) ON DELETE CASCADE;

-- hr_payroll 表外键
ALTER TABLE public.hr_payroll
    ADD CONSTRAINT fk_payroll_employee
    FOREIGN KEY (employee_id) REFERENCES public.hr_employee(id) ON DELETE CASCADE;

-- hr_training 表外键
ALTER TABLE public.hr_training
    ADD CONSTRAINT fk_training_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 8. 档案管理模块 (Archive) 外键
-- =====================================================

-- archive 表外键
ALTER TABLE public.archive
    ADD CONSTRAINT fk_archive_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_archive_creator
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 9. 文档管理模块 (Document) 外键
-- =====================================================

-- doc_file 表外键
ALTER TABLE public.doc_file
    ADD CONSTRAINT fk_doc_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_doc_creator
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- doc_version 表外键
ALTER TABLE public.doc_version
    ADD CONSTRAINT fk_doc_version_file
    FOREIGN KEY (file_id) REFERENCES public.doc_file(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_doc_version_creator
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 完成标记
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '外键约束优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已添加的外键约束涵盖以下模块:';
    RAISE NOTICE '  1. 客户管理 (CRM)';
    RAISE NOTICE '  2. 项目管理 (Matter)';
    RAISE NOTICE '  3. 财务管理 (Finance)';
    RAISE NOTICE '  4. 行政管理 (Admin)';
    RAISE NOTICE '  5. 工时管理 (Timesheet)';
    RAISE NOTICE '  6. 任务管理 (Task)';
    RAISE NOTICE '  7. 人力资源 (HR)';
    RAISE NOTICE '  8. 档案管理 (Archive)';
    RAISE NOTICE '  9. 文档管理 (Document)';
    RAISE NOTICE '';
    RAISE NOTICE '注意: 使用 IF NOT EXISTS 确保脚本可重复执行';
END $$;

-- =====================================================
-- 合并自: 61-optimize-partial-indexes.sql
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

-- seal_application 表：只索引待审批的用印申请
CREATE INDEX IF NOT EXISTS idx_seal_pending ON public.seal_application (status, created_at)
    WHERE deleted = false AND status = 'PENDING';

-- 注：会议室预约功能暂未实现
-- CREATE INDEX IF NOT EXISTS idx_meeting_upcoming ON public.meeting_room_reservation (room_id, start_time)
--     WHERE deleted = false AND start_time >= CURRENT_TIMESTAMP;

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

-- =====================================================
-- 合并自: 62-optimize-composite-indexes.sql
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
    ON public.seal_application (applicant_id, status, created_at DESC)
    WHERE deleted = false;

-- 用印申请：按印章+状态查询
CREATE INDEX IF NOT EXISTS idx_seal_seal_status
    ON public.seal_application (seal_id, status, created_at DESC)
    WHERE deleted = false;

-- 注：会议室预约功能暂未实现
-- CREATE INDEX IF NOT EXISTS idx_meeting_room_time
--     ON public.meeting_room_reservation (room_id, start_time, end_time)
--     WHERE deleted = false AND start_time >= CURRENT_DATE;

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

-- =====================================================
-- 合并自: 63-optimize-check-constraints.sql
-- =====================================================

-- 1. 系统管理模块检查约束
-- =====================================================

-- sys_user 表：邮箱格式检查
ALTER TABLE public.sys_user
    ADD CONSTRAINT chk_user_email_format
    CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- sys_user 表：手机号格式检查（中国手机号）
ALTER TABLE public.sys_user
    ADD CONSTRAINT chk_user_phone_format
    CHECK (phone IS NULL OR phone ~* '^1[3-9][0-9]{9}$');

-- sys_user 表：状态值检查
ALTER TABLE public.sys_user
    ADD CONSTRAINT chk_user_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

-- sys_user 表：薪酬模式检查
ALTER TABLE public.sys_user
    ADD CONSTRAINT chk_user_compensation_type
    CHECK (compensation_type IN ('COMMISSION', 'SALARIED', 'HYBRID'));

-- sys_role 表：数据范围检查
ALTER TABLE public.sys_role
    ADD CONSTRAINT chk_role_data_scope
    CHECK (data_scope IN ('ALL', 'DEPT', 'DEPT_AND_CHILD', 'SELF', 'CUSTOM'));

-- sys_role 表：状态检查
ALTER TABLE public.sys_role
    ADD CONSTRAINT chk_role_status
    CHECK (status IN ('ACTIVE', 'INACTIVE'));

-- sys_department 表：状态检查
ALTER TABLE public.sys_department
    ADD CONSTRAINT chk_dept_status
    CHECK (status IN ('ACTIVE', 'INACTIVE'));

-- sys_menu 表：菜单类型检查
ALTER TABLE public.sys_menu
    ADD CONSTRAINT chk_menu_type
    CHECK (menu_type IN ('DIRECTORY', 'MENU', 'BUTTON'));

-- sys_menu 表：状态检查
ALTER TABLE public.sys_menu
    ADD CONSTRAINT chk_menu_status
    CHECK (status IN ('ENABLED', 'DISABLED'));

-- sys_config 表：配置类型检查
ALTER TABLE public.sys_config
    ADD CONSTRAINT chk_config_type
    CHECK (config_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON', 'TEXT'));

-- sys_dict_type 表：状态检查
ALTER TABLE public.sys_dict_type
    ADD CONSTRAINT chk_dict_type_status
    CHECK (status IN ('ENABLED', 'DISABLED'));

-- sys_dict_item 表：状态检查
ALTER TABLE public.sys_dict_item
    ADD CONSTRAINT chk_dict_item_status
    CHECK (status IN ('ENABLED', 'DISABLED'));

-- sys_login_log 表：状态检查
ALTER TABLE public.sys_login_log
    ADD CONSTRAINT chk_login_log_status
    CHECK (status IN ('SUCCESS', 'FAILURE'));

-- sys_login_log 表：设备类型检查
ALTER TABLE public.sys_login_log
    ADD CONSTRAINT chk_login_log_device
    CHECK (device_type IN ('PC', 'MOBILE', 'TABLET', 'UNKNOWN'));

-- sys_notification 表：类型检查
ALTER TABLE public.sys_notification
    ADD CONSTRAINT chk_notification_type
    CHECK (type IN ('SYSTEM', 'APPROVAL', 'TASK', 'REMINDER', 'ANNOUNCEMENT'));

-- sys_user_session 表：状态检查
ALTER TABLE public.sys_user_session
    ADD CONSTRAINT chk_session_status
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'));

-- sys_data_handover 表：交接类型检查
ALTER TABLE public.sys_data_handover
    ADD CONSTRAINT chk_handover_type
    CHECK (handover_type IN ('RESIGNATION', 'PROJECT', 'CLIENT', 'LEAD', 'TASK'));

-- sys_data_handover 表：状态检查
ALTER TABLE public.sys_data_handover
    ADD CONSTRAINT chk_handover_status
    CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CONFIRMED', 'CANCELLED'));

-- sys_operation_log 表：状态检查
ALTER TABLE public.sys_operation_log
    ADD CONSTRAINT chk_operation_status
    CHECK (status IN ('SUCCESS', 'FAILURE', 'PARTIAL'));

-- =====================================================
-- 2. 客户管理模块检查约束
-- =====================================================

-- crm_client 表：客户类型检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT chk_client_type
    CHECK (client_type IN ('INDIVIDUAL', 'ENTERPRISE', 'GOVERNMENT', 'OTHER'));

-- crm_client 表：客户级别检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT chk_client_level
    CHECK (level IN ('A', 'B', 'C'));

-- crm_client 表：客户分类检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT chk_client_category
    CHECK (category IN ('VIP', 'NORMAL', 'POTENTIAL'));

-- crm_client 表：客户状态检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT chk_client_status
    CHECK (status IN ('POTENTIAL', 'ACTIVE', 'INACTIVE', 'BLACKLIST'));

-- crm_client 表：身份证格式检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT chk_client_id_card
    CHECK (id_card IS NULL OR id_card ~* '^[1-9][0-9]{16}[0-9Xx]$');

-- crm_client 表：信用代码格式检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT chk_client_credit_code
    CHECK (credit_code IS NULL OR credit_code ~* '^[0-9A-Z]{18}$');

-- crm_client 表：手机号格式检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT chk_client_phone
    CHECK (contact_phone IS NULL OR contact_phone ~* '^1[3-9][0-9]{9}$');

-- crm_client 表：邮箱格式检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT chk_client_email
    CHECK (contact_email IS NULL OR contact_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- crm_contact 表：邮箱格式检查
ALTER TABLE public.crm_contact
    ADD CONSTRAINT chk_contact_email
    CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- crm_contact 表：手机号格式检查
ALTER TABLE public.crm_contact
    ADD CONSTRAINT chk_contact_phone
    CHECK (mobile_phone IS NULL OR mobile_phone ~* '^1[3-9][0-9]{9}$');

-- crm_client_change_history 表：变更类型检查
ALTER TABLE public.crm_client_change_history
    ADD CONSTRAINT chk_change_type
    CHECK (change_type IN ('NAME', 'REGISTERED_CAPITAL', 'LEGAL_REPRESENTATIVE',
                          'ADDRESS', 'BUSINESS_SCOPE', 'SHAREHOLDER', 'OTHER'));

-- crm_client_related_company 表：关联类型检查
ALTER TABLE public.crm_client_related_company
    ADD CONSTRAINT chk_related_company_type
    CHECK (related_company_type IN ('PARENT', 'SUBSIDIARY', 'AFFILIATE'));

-- crm_client_shareholder 表：股东类型检查
ALTER TABLE public.crm_client_shareholder
    ADD CONSTRAINT chk_shareholder_type
    CHECK (shareholder_type IN ('INDIVIDUAL', 'ENTERPRISE'));

-- crm_client_shareholder 表：持股比例范围检查
ALTER TABLE public.crm_client_shareholder
    ADD CONSTRAINT chk_shareholding_ratio
    CHECK (shareholding_ratio IS NULL OR (shareholding_ratio >= 0 AND shareholding_ratio <= 100));

-- crm_lead 表：案源类型检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT chk_lead_type
    CHECK (lead_type IN ('INDIVIDUAL', 'ENTERPRISE', 'GOVERNMENT'));

-- crm_lead 表：状态检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT chk_lead_status
    CHECK (status IN ('PENDING', 'FOLLOWING', 'CONVERTED', 'ABANDONED'));

-- crm_lead 表：优先级检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT chk_lead_priority
    CHECK (priority IN ('HIGH', 'NORMAL', 'LOW'));

-- crm_lead 表：手机号格式检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT chk_lead_phone
    CHECK (contact_phone IS NULL OR contact_phone ~* '^1[3-9][0-9]{9}$');

-- crm_lead 表：邮箱格式检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT chk_lead_email
    CHECK (contact_email IS NULL OR contact_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- crm_lead_follow_up 表：跟进方式检查
ALTER TABLE public.crm_lead_follow_up
    ADD CONSTRAINT chk_follow_type
    CHECK (follow_type IN ('PHONE', 'EMAIL', 'VISIT', 'MEETING', 'OTHER'));

-- crm_lead_follow_up 表：跟进结果检查
ALTER TABLE public.crm_lead_follow_up
    ADD CONSTRAINT chk_follow_result
    CHECK (follow_result IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE'));

-- crm_conflict_check 表：检查类型检查
ALTER TABLE public.crm_conflict_check
    ADD CONSTRAINT chk_conflict_check_type
    CHECK (check_type IN ('NEW_CLIENT', 'NEW_MATTER', 'MANUAL', 'PERIODIC'));

-- crm_conflict_check 表：状态检查
ALTER TABLE public.crm_conflict_check
    ADD CONSTRAINT chk_conflict_check_status
    CHECK (status IN ('PENDING', 'CHECKING', 'PASSED', 'CONFLICT', 'EXEMPTION_PENDING',
                      'WAIVED', 'REJECTED'));

-- crm_conflict_check_item 表：当事人类型检查
ALTER TABLE public.crm_conflict_check_item
    ADD CONSTRAINT chk_party_type
    CHECK (party_type IN ('CLIENT', 'OPPOSING', 'RELATED', 'THIRD_PARTY'));

-- =====================================================
-- 3. 项目管理模块检查约束
-- =====================================================

-- matter 表：项目类型检查
ALTER TABLE public.matter
    ADD CONSTRAINT chk_matter_type
    CHECK (matter_type IN ('LITIGATION', 'NON_LITIGATION', 'LEGAL_ADVICE', 'DRAFTING',
                          'CONSULTING', 'OTHER'));

-- matter 表：项目状态检查
ALTER TABLE public.matter
    ADD CONSTRAINT chk_matter_status
    CHECK (matter_status IN ('PENDING', 'IN_PROGRESS', 'SUSPENDED', 'COMPLETED', 'CANCELLED'));

-- matter 表：案件性质检查
ALTER TABLE public.matter
    ADD CONSTRAINT chk_case_nature
    CHECK (case_nature IN ('CIVIL', 'ADMINISTRATIVE', 'CRIMINAL', 'STATE_COMPENSATION',
                            'ENFORCEMENT', 'OTHER'));

-- matter 表：审理程序检查
ALTER TABLE public.matter
    ADD CONSTRAINT chk_trial_procedure
    CHECK (trial_procedure IN ('FIRST_INSTANCE', 'SECOND_INSTANCE', 'RETRIAL',
                               'SUMMARY', 'SUPERVISION', 'EXECUTION'));

-- matter 表：项目等级检查
ALTER TABLE public.matter
    ADD CONSTRAINT chk_matter_level
    CHECK (matter_level IN ('MAJOR', 'IMPORTANT', 'NORMAL', 'MINOR'));

-- matter 表：紧急程度检查
ALTER TABLE public.matter
    ADD CONSTRAINT chk_urgency
    CHECK (urgency IN ('URGENT', 'NORMAL', 'LOW'));

-- matter_participant 表：参与人角色检查
ALTER TABLE public.matter_participant
    ADD CONSTRAINT chk_participant_role
    CHECK (participant_role IN ('PLAINTIFF', 'DEFENDANT', 'THIRD_PARTY',
                                'PROSECUTOR', 'DEFENSE_COUNSEL', 'PLAINTIFF_COUNSEL',
                                'JUDGE', 'CLERK', 'OTHER'));

-- matter_deadline 表：期限类型检查
ALTER TABLE public.matter_deadline
    ADD CONSTRAINT chk_deadline_type
    CHECK (deadline_type IN ('COURT_HEARING', 'EVIDENCE_SUBMISSION', 'STATEMENT_SUBMISSION',
                             'APPEAL_DEADLINE', 'EXECUTION_DEADLINE', 'OTHER'));

-- =====================================================
-- 4. 财务管理模块检查约束
-- =====================================================

-- finance_contract 表：合同类型检查
ALTER TABLE public.finance_contract
    ADD CONSTRAINT chk_contract_type
    CHECK (contract_type IN ('FIXED_FEE', 'HOURLY', 'CONTINGENCY', 'HYBRID', 'RETAINER',
                             'LEGAL_AID', 'PRO_BONO', 'OTHER'));

-- finance_contract 表：合同状态检查
ALTER TABLE public.finance_contract
    ADD CONSTRAINT chk_contract_status
    CHECK (contract_status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SIGNED',
                                'EXECUTING', 'COMPLETED', 'TERMINATED', 'CANCELLED'));

-- finance_contract 表：金额检查
ALTER TABLE public.finance_contract
    ADD CONSTRAINT chk_contract_amount_positive
    CHECK (contract_amount IS NULL OR contract_amount >= 0);

-- finance_payment 表：收款状态检查
ALTER TABLE public.finance_payment
    ADD CONSTRAINT chk_payment_status
    CHECK (payment_status IN ('PENDING', 'PARTIAL', 'COMPLETED', 'CANCELLED'));

-- finance_payment 表：收款方式检查
ALTER TABLE public.finance_payment
    ADD CONSTRAINT chk_payment_method
    CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'CHECK', 'ALIPAY', 'WECHAT_PAY',
                               'POS', 'OTHER'));

-- finance_payment 表：金额检查
ALTER TABLE public.finance_payment
    ADD CONSTRAINT chk_payment_amount_positive
    CHECK (amount > 0);

-- finance_payment 表：已收金额检查
ALTER TABLE public.finance_payment
    ADD CONSTRAINT chk_received_amount_valid
    CHECK (received_amount >= 0 AND received_amount <= amount);

-- finance_commission 表：提成状态检查
ALTER TABLE public.finance_commission
    ADD CONSTRAINT chk_commission_status
    CHECK (status IN ('PENDING', 'APPROVED', 'PAID', 'CANCELLED'));

-- finance_commission 表：薪酬模式检查
ALTER TABLE public.finance_commission
    ADD CONSTRAINT chk_commission_compensation_type
    CHECK (compensation_type IN ('COMMISSION', 'SALARIED', 'HYBRID'));

-- finance_commission 表：分配比例范围检查
ALTER TABLE public.finance_commission
    ADD CONSTRAINT chk_distribution_ratio
    CHECK (distribution_ratio IS NULL OR (distribution_ratio >= 0 AND distribution_ratio <= 100));

-- finance_commission 表：提成比例范围检查
ALTER TABLE public.finance_commission
    ADD CONSTRAINT chk_commission_rate
    CHECK (commission_rate IS NULL OR (commission_rate >= 0 AND commission_rate <= 100));

-- finance_commission_detail 表：分配比例范围检查
ALTER TABLE public.finance_commission_detail
    ADD CONSTRAINT chk_allocation_rate_positive
    CHECK (allocation_rate > 0 AND allocation_rate <= 100);

-- finance_invoice 表：发票类型检查
ALTER TABLE public.finance_invoice
    ADD CONSTRAINT chk_invoice_type
    CHECK (invoice_type IN ('VAT_SPECIAL', 'VAT_GENERAL', 'COMMON', 'PROFORMA', 'OTHER'));

-- finance_invoice 表：发票状态检查
ALTER TABLE public.finance_invoice
    ADD CONSTRAINT chk_invoice_status
    CHECK (invoice_status IN ('PENDING', 'PROCESSING', 'ISSUED', 'VOID', 'CANCELLED'));

-- =====================================================
-- 5. 行政管理模块检查约束
-- =====================================================

-- admin_seal 表：印章状态检查
ALTER TABLE public.admin_seal
    ADD CONSTRAINT chk_seal_status
    CHECK (seal_status IN ('ACTIVE', 'INACTIVE', 'LOST', 'DAMAGED'));

-- seal_application 表：申请状态检查
ALTER TABLE public.seal_application
    ADD CONSTRAINT chk_seal_app_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'USED', 'CANCELLED'));

-- meeting_room 表：会议室状态检查
ALTER TABLE public.meeting_room
    ADD CONSTRAINT chk_meeting_room_status
    CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'DISABLED'));

-- 注：会议室预约功能暂未实现
-- ALTER TABLE public.meeting_room_reservation
--     ADD CONSTRAINT chk_meeting_res_status
--     CHECK (reservation_status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED'));

-- =====================================================
-- 6. 工时管理模块检查约束
-- =====================================================

-- timesheet 表：工作类型检查
ALTER TABLE public.timesheet
    ADD CONSTRAINT chk_work_type
    CHECK (work_type IN ('BILLABLE', 'NON_BILLABLE', 'ADMIN', 'PRO_BONO', 'OTHER'));

-- timesheet 表：工时范围检查
ALTER TABLE public.timesheet
    ADD CONSTRAINT chk_work_hours_positive
    CHECK (work_hours > 0 AND work_hours <= 24);

-- timesheet 表：年份范围检查
ALTER TABLE public.timesheet
    ADD CONSTRAINT chk_work_year_valid
    CHECK (work_year >= 2020 AND work_year <= 2100);

-- timesheet 表：月份范围检查
ALTER TABLE public.timesheet
    ADD CONSTRAINT chk_work_month_valid
    CHECK (work_month >= 1 AND work_month <= 12);

-- =====================================================
-- 7. 任务管理模块检查约束
-- =====================================================

-- task 表：任务状态检查
ALTER TABLE public.task
    ADD CONSTRAINT chk_task_status
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'BLOCKED'));

-- task 表：优先级检查
ALTER TABLE public.task
    ADD CONSTRAINT chk_task_priority
    CHECK (priority IN ('URGENT', 'HIGH', 'NORMAL', 'LOW'));

-- =====================================================
-- 8. 人力资源模块检查约束
-- =====================================================

-- hr_employee 表：员工状态检查
ALTER TABLE public.hr_employee
    ADD CONSTRAINT chk_employee_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'RESIGNED', 'TERMINATED'));

-- hr_employee 表：性别检查
ALTER TABLE public.hr_employee
    ADD CONSTRAINT chk_employee_gender
    CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));

-- hr_attendance 表：考勤状态检查
ALTER TABLE public.hr_attendance
    ADD CONSTRAINT chk_attendance_status
    CHECK (attendance_status IN ('PRESENT', 'ABSENT', 'LATE', 'EARLY_LEAVE', 'LEAVE', 'BUSINESS_TRIP'));

-- hr_payroll 表：薪酬状态检查
ALTER TABLE public.hr_payroll
    ADD CONSTRAINT chk_payroll_status
    CHECK (payroll_status IN ('DRAFT', 'PENDING', 'APPROVED', 'PAID', 'CANCELLED'));

-- hr_performance 表：考核等级检查
ALTER TABLE public.hr_performance
    ADD CONSTRAINT chk_performance_rating
    CHECK (rating IN ('S', 'A', 'B', 'C', 'D'));

-- =====================================================
-- 9. 档案管理模块检查约束
-- =====================================================

-- archive 表：档案状态检查
ALTER TABLE public.archive
    ADD CONSTRAINT chk_archive_status
    CHECK (archive_status IN ('PENDING', 'STORED', 'BORROWED', 'DESTROYED'));

-- archive 表：保管期限检查
ALTER TABLE public.archive
    ADD CONSTRAINT chk_retention_period
    CHECK (retention_period IN ('PERMANENT', 'LONG_TERM', 'SHORT_TERM', 'TEMPORARY'));

-- archive_borrow 表：借阅状态检查
ALTER TABLE public.archive_borrow
    ADD CONSTRAINT chk_borrow_status
    CHECK (borrow_status IN ('PENDING', 'APPROVED', 'BORROWED', 'RETURNED', 'OVERDUE'));

-- =====================================================
-- 10. 文档管理模块检查约束
-- =====================================================

-- doc_file 表：文档类型检查
ALTER TABLE public.doc_file
    ADD CONSTRAINT chk_doc_type
    CHECK (doc_type IN ('CONTRACT', 'EVIDENCE', 'PLEADING', 'CORRESPONDENCE',
                         'COURT_DOCUMENT', 'OPINION', 'RESEARCH', 'ADMIN', 'OTHER'));

-- doc_file 表：文档状态检查
ALTER TABLE public.doc_file
    ADD CONSTRAINT chk_doc_status
    CHECK (doc_status IN ('DRAFT', 'REVIEW', 'APPROVED', 'FINAL', 'ARCHIVED'));

-- doc_version 表：版本类型检查
ALTER TABLE public.doc_version
    ADD CONSTRAINT chk_version_type
    CHECK (version_type IN ('DRAFT', 'MINOR', 'MAJOR', 'FINAL'));

-- =====================================================
-- 11. 质量管理模块检查约束
-- =====================================================

-- quality_check 表：检查状态检查
ALTER TABLE public.quality_check
    ADD CONSTRAINT chk_quality_check_status
    CHECK (check_status IN ('PENDING', 'IN_PROGRESS', 'PASSED', 'FAILED', 'SKIPPED'));

-- quality_check 表：检查类型检查
ALTER TABLE public.quality_check
    ADD CONSTRAINT chk_quality_check_type
    CHECK (check_type IN ('FILE_REVIEW', 'MATTER_REVIEW', 'CONTRACT_REVIEW',
                          'COURT_DOCUMENT_REVIEW', 'OTHER'));

-- quality_risk 表：风险等级检查
ALTER TABLE public.quality_risk
    ADD CONSTRAINT chk_risk_level
    CHECK (risk_level IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW'));

-- quality_risk 表：风险状态检查
ALTER TABLE public.quality_risk
    ADD CONSTRAINT chk_risk_status
    CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'IGNORED'));

-- =====================================================
-- 12. 工作台模块检查约束
-- =====================================================

-- wb_approval 表：审批状态检查
ALTER TABLE public.wb_approval
    ADD CONSTRAINT chk_approval_status
    CHECK (approval_status IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'CANCELLED'));

-- wb_approval 表：审批类型检查
ALTER TABLE public.wb_approval
    ADD CONSTRAINT chk_approval_type
    CHECK (approval_type IN ('CONTRACT', 'SEAL', 'LEAVE', 'REIMBURSEMENT', 'PURCHASE',
                             'CONFLICT_CHECK', 'MATTER_CLOSE', 'OTHER'));

-- wb_schedule 表：日程类型检查
ALTER TABLE public.wb_schedule
    ADD CONSTRAINT chk_schedule_type
    CHECK (schedule_type IN ('COURT_HEARING', 'MEETING', 'CLIENT_VISIT', 'DEPOSITION',
                             'MEDITATION', 'ARBITRATION', 'TRAINING', 'OTHER'));

-- wb_schedule 表：提醒类型检查
ALTER TABLE public.wb_schedule
    ADD CONSTRAINT chk_reminder_type
    CHECK (reminder_type IN ('NONE', 'EMAIL', 'SMS', 'SYSTEM', 'ALL'));

-- =====================================================
-- 完成标记
-- =====================================================

DO $$
DECLARE
    constraint_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO constraint_count
    FROM pg_constraint
    WHERE conname LIKE 'chk_%';

    RAISE NOTICE '========================================';
    RAISE NOTICE '检查约束优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已添加 % 个检查约束', constraint_count;
    RAISE NOTICE '';
    RAISE NOTICE '检查约束的作用:';
    RAISE NOTICE '  1. 确保数据格式正确(邮箱、手机号等)';
    RAISE NOTICE '  2. 限制枚举值范围(状态、类型等)';
    RAISE NOTICE '  3. 防止不合理数据(负数金额等)';
    RAISE NOTICE '  4. 在数据库层面保证数据完整性';
END $$;

-- =====================================================
-- 合并自: 64-optimize-log-partitioning.sql
-- =====================================================

-- 注意事项
-- =====================================================
-- 1. 此脚本会删除现有的非分区表，请确保已有数据已备份
-- 2. 分区表不支持外键约束，需要移除相关外键
-- 3. 分区键必须是主键的一部分
-- 4. 建议在低峰期执行
-- =====================================================

-- =====================================================
-- 1. 创建分区表函数
-- =====================================================

-- 创建操作日志分区表
CREATE TABLE IF NOT EXISTS public.sys_operation_log_partitioned (
    id bigint NOT NULL,
    user_id bigint,
    user_name character varying(50),
    module character varying(50),
    operation_type character varying(50),
    description character varying(500),
    method character varying(200),
    request_url character varying(500),
    request_method character varying(10),
    request_params text,
    response_result text,
    ip_address character varying(50),
    user_agent character varying(2000),
    execution_time bigint,
    status character varying(20) DEFAULT 'SUCCESS'::character varying,
    error_message text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

COMMENT ON TABLE public.sys_operation_log_partitioned IS '操作日志表（按月分区）';

-- 创建登录日志分区表
CREATE TABLE IF NOT EXISTS public.sys_login_log_partitioned (
    id bigint NOT NULL,
    user_id bigint,
    username character varying(50),
    real_name character varying(50),
    login_ip character varying(50),
    login_location character varying(200),
    user_agent character varying(2000),
    browser character varying(100),
    os character varying(100),
    device_type character varying(20),
    status character varying(20) NOT NULL,
    message character varying(2000),
    login_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    logout_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    version integer DEFAULT 0,
    PRIMARY KEY (id, login_time)
) PARTITION BY RANGE (login_time);

COMMENT ON TABLE public.sys_login_log_partitioned IS '登录日志表（按月分区）';

-- =====================================================
-- 2. 创建初始分区（当前季度）
-- =====================================================

-- 获取当前月份并创建分区
DO $$
DECLARE
    current_month TEXT := TO_CHAR(CURRENT_DATE, 'YYYY_MM');
    partition_name TEXT;
    start_date TEXT;
    end_date TEXT;
    i INTEGER;
BEGIN
    -- 创建当前月和接下来2个月的分区
    FOR i IN 0..2 LOOP
        partition_name := 'sys_operation_log_' || TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY_MM');
        start_date := TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY-MM') || '-01';
        end_date := TO_CHAR((CURRENT_DATE + ((i + 1) || ' months')::INTERVAL), 'YYYY-MM') || '-01';

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS public.%I
            PARTITION OF public.sys_operation_log_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);
    END LOOP;

    -- 创建登录日志分区
    FOR i IN 0..2 LOOP
        partition_name := 'sys_login_log_' || TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY_MM');
        start_date := TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY-MM') || '-01';
        end_date := TO_CHAR((CURRENT_DATE + ((i + 1) || ' months')::INTERVAL), 'YYYY-MM') || '-01';

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS public.%I
            PARTITION OF public.sys_login_log_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);
    END LOOP;

    RAISE NOTICE '已创建当前季度及下月的分区';
END $$;

-- =====================================================
-- 3. 创建索引（分区表专用）
-- =====================================================

-- 操作日志分区表索引
CREATE INDEX IF NOT EXISTS idx_operation_log_partition_user
    ON public.sys_operation_log_partitioned (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operation_log_partition_module
    ON public.sys_operation_log_partitioned (module, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operation_log_partition_status
    ON public.sys_operation_log_partitioned (status, created_at DESC);

-- 登录日志分区表索引
CREATE INDEX IF NOT EXISTS idx_login_log_partition_user
    ON public.sys_login_log_partitioned (user_id, login_time DESC);

CREATE INDEX IF NOT EXISTS idx_login_log_partition_status
    ON public.sys_login_log_partitioned (status, login_time DESC);

CREATE INDEX IF NOT EXISTS idx_login_log_partition_username
    ON public.sys_login_log_partitioned (username, login_time DESC);

-- =====================================================
-- 4. 创建自动维护函数
-- =====================================================

-- 创建自动创建未来分区的函数
CREATE OR REPLACE FUNCTION public.create_monthly_partitions()
RETURNS void AS $$
DECLARE
    partition_name TEXT;
    start_date TEXT;
    end_date TEXT;
    table_name TEXT;
    date_column TEXT;
    i INTEGER;
BEGIN
    -- 为操作日志表创建未来3个月的分区
    FOR i IN 3..5 LOOP
        partition_name := 'sys_operation_log_' || TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY_MM');
        start_date := TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY-MM') || '-01';
        end_date := TO_CHAR((CURRENT_DATE + ((i + 1) || ' months')::INTERVAL), 'YYYY-MM') || '-01';

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS public.%I
            PARTITION OF public.sys_operation_log_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);

        RAISE NOTICE '已创建操作日志分区: %', partition_name;
    END LOOP;

    -- 为登录日志表创建未来3个月的分区
    FOR i IN 3..5 LOOP
        partition_name := 'sys_login_log_' || TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY_MM');
        start_date := TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY-MM') || '-01';
        end_date := TO_CHAR((CURRENT_DATE + ((i + 1) || ' months')::INTERVAL), 'YYYY-MM') || '-01';

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS public.%I
            PARTITION OF public.sys_login_log_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);

        RAISE NOTICE '已创建登录日志分区: %', partition_name;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.create_monthly_partitions() IS '自动创建未来月份的分区';

-- 创建删除旧分区的函数
CREATE OR REPLACE FUNCTION public.drop_old_partitions(months_to_keep INTEGER DEFAULT 12)
RETURNS void AS $$
DECLARE
    partition_name TEXT;
    cutoff_date TIMESTAMP := (CURRENT_DATE - (months_to_keep || ' months')::INTERVAL);
BEGIN
    -- 删除旧的操作日志分区
    FOR partition_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
        AND tablename LIKE 'sys_operation_log_%'
        AND substring(tablename FROM 19 FOR 7) < TO_CHAR(cutoff_date, 'YYYY_MM')
    LOOP
        EXECUTE 'DROP TABLE IF EXISTS public.' || quote_ident(partition_name);
        RAISE NOTICE '已删除操作日志分区: %', partition_name;
    END LOOP;

    -- 删除旧的登录日志分区
    FOR partition_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
        AND tablename LIKE 'sys_login_log_%'
        AND substring(tablename FROM 16 FOR 7) < TO_CHAR(cutoff_date, 'YYYY_MM')
    LOOP
        EXECUTE 'DROP TABLE IF EXISTS public.' || quote_ident(partition_name);
        RAISE NOTICE '已删除登录日志分区: %', partition_name;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.drop_old_partitions(integer) IS '删除指定月份之前的旧分区，默认保留12个月';

-- =====================================================
-- 5. 数据迁移函数（可选）
-- =====================================================

-- 创建从旧表迁移数据到分区表的函数
CREATE OR REPLACE FUNCTION public.migrate_to_partitioned_tables(batch_size INTEGER DEFAULT 10000)
RETURNS void AS $$
DECLARE
    migrated_rows INTEGER;
BEGIN
    -- 迁移操作日志
    INSERT INTO public.sys_operation_log_partitioned
    SELECT * FROM public.sys_operation_log
    ON CONFLICT (id, created_at) DO NOTHING;

    GET DIAGNOSTICS migrated_rows = ROW_COUNT;
    RAISE NOTICE '已迁移 % 条操作日志记录', migrated_rows;

    -- 迁移登录日志
    INSERT INTO public.sys_login_log_partitioned
    SELECT * FROM public.sys_login_log
    ON CONFLICT (id, login_time) DO NOTHING;

    GET DIAGNOSTICS migrated_rows = ROW_COUNT;
    RAISE NOTICE '已迁移 % 条登录日志记录', migrated_rows;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.migrate_to_partitioned_tables(integer) IS '将旧表数据迁移到分区表';

-- =====================================================
-- 6. 定时任务（需要 pg_cron 扩展）
-- =====================================================

-- 创建每月维护分区定时任务
-- SELECT cron.schedule('maintain-log-partitions', '0 0 1 * *', 'SELECT public.create_monthly_partitions();');

-- =====================================================
-- 7. 使用说明
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '日志表分区脚本已执行';
    RAISE NOTICE '========================================';
    RAISE NOTICE '';
    RAISE NOTICE '使用说明:';
    RAISE NOTICE '1. 分区表已创建:';
    RAISE NOTICE '   - sys_operation_log_partitioned';
    RAISE NOTICE '   - sys_login_log_partitioned';
    RAISE NOTICE '';
    RAISE NOTICE '2. 迁移数据:';
    RAISE NOTICE '   SELECT public.migrate_to_partitioned_tables();';
    RAISE NOTICE '';
    RAISE NOTICE '3. 验证迁移后，替换原表:';
    RAISE NOTICE '   ALTER TABLE sys_operation_log RENAME TO sys_operation_log_old;';
    RAISE NOTICE '   ALTER TABLE sys_operation_log_partitioned RENAME TO sys_operation_log;';
    RAISE NOTICE '   -- 确认无误后删除旧表';
    RAISE NOTICE '   DROP TABLE sys_operation_log_old;';
    RAISE NOTICE '';
    RAISE NOTICE '4. 定期维护:';
    RAISE NOTICE '   -- 创建未来月份分区';
    RAISE NOTICE '   SELECT public.create_monthly_partitions();';
    RAISE NOTICE '   -- 删除旧分区(保留12个月)';
    RAISE NOTICE '   SELECT public.drop_old_partitions(12);';
    RAISE NOTICE '';
    RAISE NOTICE '5. 设置自动维护(可选):';
    RAISE NOTICE '   需要先安装 pg_cron 扩展';
END $$;

-- =====================================================
-- 合并自: 65-optimize-fulltext-search.sql
-- =====================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =====================================================
-- 1. 系统管理模块全文搜索
-- =====================================================

-- 用户表：姓名和用户名搜索
CREATE INDEX IF NOT EXISTS idx_user_name_trgm
    ON public.sys_user USING gin (real_name gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_user_username_trgm
    ON public.sys_user USING gin (username gin_trgm_ops)
    WHERE deleted = false;

-- 菜单表：菜单名称搜索
CREATE INDEX IF NOT EXISTS idx_menu_name_trgm
    ON public.sys_menu USING gin (name gin_trgm_ops)
    WHERE deleted = false;

-- 配置表：配置名称搜索
CREATE INDEX IF NOT EXISTS idx_config_name_trgm
    ON public.sys_config USING gin (config_name gin_trgm_ops)
    WHERE deleted = false;

-- 通知表：标题和内容搜索
CREATE INDEX IF NOT EXISTS idx_notification_title_trgm
    ON public.sys_notification USING gin (title gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_notification_content_trgm
    ON public.sys_notification USING gin (content gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 2. 客户管理模块全文搜索
-- =====================================================

-- 客户表：客户名称搜索
CREATE INDEX IF NOT EXISTS idx_client_name_trgm
    ON public.crm_client USING gin (name gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_client_name_cn_trgm
    ON public.crm_client USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_client_legal_rep_trgm
    ON public.crm_client USING gin (legal_representative gin_trgm_ops)
    WHERE deleted = false;

-- 客户表：注册地址搜索
CREATE INDEX IF NOT EXISTS idx_client_address_trgm
    ON public.crm_client USING gin (registered_address gin_trgm_ops)
    WHERE deleted = false;

-- 联系人表：联系人姓名和手机号搜索
CREATE INDEX IF NOT EXISTS idx_contact_name_trgm
    ON public.crm_contact USING gin (contact_name gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_contact_phone_trgm
    ON public.crm_contact USING gin (mobile_phone gin_trgm_ops)
    WHERE deleted = false;

-- 案源表：案源名称搜索
CREATE INDEX IF NOT EXISTS idx_lead_name_trgm
    ON public.crm_lead USING gin (lead_name gin_trgm_ops)
    WHERE deleted = false;

-- 案源表：联系人信息搜索
CREATE INDEX IF NOT EXISTS idx_lead_contact_name_trgm
    ON public.crm_lead USING gin (contact_name gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_lead_phone_trgm
    ON public.crm_lead USING gin (contact_phone gin_trgm_ops)
    WHERE deleted = false;

-- 利冲检查表：当事人名称搜索
CREATE INDEX IF NOT EXISTS idx_conflict_party_name_trgm
    ON public.crm_conflict_check_item USING gin (party_name gin_trgm_ops)
    WHERE deleted = false;

-- 客户变更历史：变更内容搜索
CREATE INDEX IF NOT EXISTS idx_client_change_desc_trgm
    ON public.crm_client_change_history USING gin (change_description gin_trgm_ops)
    WHERE deleted = false;

-- 客户标签：标签名称搜索
CREATE INDEX IF NOT EXISTS idx_client_tag_name_trgm
    ON public.crm_client_tag USING gin (tag_name gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 3. 项目管理模块全文搜索
-- =====================================================

-- 项目表：项目名称搜索
CREATE INDEX IF NOT EXISTS idx_matter_title_trgm
    ON public.matter USING gin (title gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_matter_title_cn_trgm
    ON public.matter USING gin (title gin_trgm_ops);

-- 项目表：案号搜索
CREATE INDEX IF NOT EXISTS idx_matter_case_no_trgm
    ON public.matter USING gin (case_no gin_trgm_ops)
    WHERE deleted = false;

-- 项目表：案件名称搜索
CREATE INDEX IF NOT EXISTS idx_matter_court_name_trgm
    ON public.matter USING gin (court_name gin_trgm_ops)
    WHERE deleted = false;

-- 项目表：对方当事人搜索
CREATE INDEX IF NOT EXISTS idx_matter_opposing_party_trgm
    ON public.matter USING gin (opposing_party gin_trgm_ops)
    WHERE deleted = false;

-- 项目表：审理法院搜索
CREATE INDEX IF NOT EXISTS idx_matter_court_trgm
    ON public.matter USING gin (trial_court gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 4. 财务管理模块全文搜索
-- =====================================================

-- 合同表：合同名称搜索
CREATE INDEX IF NOT EXISTS idx_contract_name_trgm
    ON public.finance_contract USING gin (contract_name gin_trgm_ops)
    WHERE deleted = false;

-- 合同表：合同编号搜索
CREATE INDEX IF NOT EXISTS idx_contract_no_trgm
    ON public.finance_contract USING gin (contract_no gin_trgm_ops)
    WHERE deleted = false;

-- 收款表：备注搜索
CREATE INDEX IF NOT EXISTS idx_payment_remark_trgm
    ON public.finance_payment USING gin (remark gin_trgm_ops)
    WHERE deleted = false;

-- 发票表：发票号码搜索
CREATE INDEX IF NOT EXISTS idx_invoice_no_trgm
    ON public.finance_invoice USING gin (invoice_no gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 5. 行政管理模块全文搜索
-- =====================================================

-- 会议室：位置搜索
CREATE INDEX IF NOT EXISTS idx_meeting_location_trgm
    ON public.meeting_room USING gin (location gin_trgm_ops)
    WHERE deleted = false;

-- 印章申请：申请事由搜索
CREATE INDEX IF NOT EXISTS idx_seal_reason_trgm
    ON public.seal_application USING gin (use_purpose gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 6. 任务管理模块全文搜索
-- =====================================================

-- 任务表：任务标题搜索
CREATE INDEX IF NOT EXISTS idx_task_title_trgm
    ON public.task USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- 任务表：任务描述搜索
CREATE INDEX IF NOT EXISTS idx_task_desc_trgm
    ON public.task USING gin (description gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 7. 人力资源模块全文搜索
-- =====================================================

-- 员工表：姓名搜索
CREATE INDEX IF NOT EXISTS idx_employee_name_trgm
    ON public.hr_employee USING gin (name gin_trgm_ops)
    WHERE deleted = false;

-- 培训表：培训主题搜索
CREATE INDEX IF NOT EXISTS idx_training_topic_trgm
    ON public.hr_training USING gin (topic gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 8. 档案管理模块全文搜索
-- =====================================================

-- 档案表：档案编号搜索
CREATE INDEX IF NOT EXISTS idx_archive_no_trgm
    ON public.archive USING gin (archive_no gin_trgm_ops)
    WHERE deleted = false;

-- 档案表：案卷名称搜索
CREATE INDEX IF NOT EXISTS idx_archive_name_trgm
    ON public.archive USING gin (dossier_name gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 9. 文档管理模块全文搜索
-- =====================================================

-- 文档表：文件名搜索
CREATE INDEX IF NOT EXISTS idx_doc_filename_trgm
    ON public.doc_file USING gin (file_name gin_trgm_ops)
    WHERE deleted = false;

-- 文档表：文档标题搜索
CREATE INDEX IF NOT EXISTS idx_doc_title_trgm
    ON public.doc_file USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 10. 知识库模块全文搜索
-- =====================================================

-- 文章表：标题搜索
CREATE INDEX IF NOT EXISTS idx_article_title_trgm
    ON public.kb_article USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- 文章表：内容搜索（使用 tsvector）
CREATE INDEX IF NOT EXISTS idx_article_content_fts
    ON public.kb_article USING gin (to_tsvector('chinese', coalesce(title, '') || ' ' || coalesce(content, '')))
    WHERE deleted = false;

-- 文章表：标签搜索
CREATE INDEX IF NOT EXISTS idx_article_tags_trgm
    ON public.kb_article USING gin (tags gin_trgm_ops)
    WHERE deleted = false;

-- 案例表：案例名称搜索
CREATE INDEX IF NOT EXISTS idx_kb_case_name_trgm
    ON public.kb_case USING gin (case_name gin_trgm_ops)
    WHERE deleted = false;

-- 案例表：案由搜索
CREATE INDEX IF NOT EXISTS idx_kb_case_cause_trgm
    ON public.kb_case USING gin (cause_of_action gin_trgm_ops)
    WHERE deleted = false;

-- 法规表：法规名称搜索
CREATE INDEX IF NOT EXISTS idx_regulation_name_trgm
    ON public.kb_regulation USING gin (regulation_name gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 11. 质量管理模块全文搜索
-- =====================================================

-- 风险预警表：风险描述搜索
CREATE INDEX IF NOT EXISTS idx_risk_desc_trgm
    ON public.quality_risk USING gin (risk_description gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 12. 工作台模块全文搜索
-- =====================================================

-- 审批表：审批事项搜索
CREATE INDEX IF NOT EXISTS idx_approval_title_trgm
    ON public.wb_approval USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- 日程表：日程内容搜索
CREATE INDEX IF NOT EXISTS idx_schedule_content_trgm
    ON public.wb_schedule USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 13. 创建全文搜索辅助函数
-- =====================================================

-- 创建中文全文搜索配置（如果 zhparser 可用）
-- CREATE TEXT SEARCH CONFIGURATION chinese (COPY = simple);
-- ALTER TEXT SEARCH CONFIGURATION chinese ALTER MAPPING FOR n,v,a,i,e,l WITH zhparser;

-- 创建通用搜索函数
CREATE OR REPLACE FUNCTION public.fulltext_search(query_text TEXT, table_name TEXT, search_columns TEXT[])
RETURNS TABLE (id bigint, rank REAL) AS $$
DECLARE
    sql_query TEXT;
    search_col TEXT;
BEGIN
    sql_query := 'SELECT id, ts_rank_cd(to_tsvector('' || quote_literal('chinese') || '', ';

    FOREACH search_col IN ARRAY search_columns LOOP
        sql_query := sql_query || 'coalesce(' || quote_ident(search_col) || ', '''') || '' '' || ';
    END LOOP;

    sql_query := sql_query || ''''), query) AS rank FROM ' || quote_ident(table_name);
    sql_query := sql_query || ' WHERE deleted = false AND ';
    sql_query := sql_query || 'to_tsvector('' || quote_literal('chinese') || '', ';

    FOREACH search_col IN ARRAY search_columns LOOP
        sql_query := sql_query || 'coalesce(' || quote_ident(search_col) || ', '''') || '' '' || ';
    END LOOP;

    sql_query := sql_query || ''') @@ to_tsquery('' || quote_literal('chinese') || ', replace(' || quote_literal(query_text) || ', '' '', '' | ''))';
    sql_query := sql_query || ' ORDER BY rank DESC LIMIT 100;';

    RETURN QUERY EXECUTE sql_query;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.fulltext_search(text, text, text[]) IS '通用全文搜索函数';

-- =====================================================
-- 14. 创建搜索使用示例函数
-- =====================================================

-- 客户模糊搜索函数
CREATE OR REPLACE FUNCTION public.search_clients(search_term TEXT, limit_count INTEGER DEFAULT 20)
RETURNS TABLE (
    id bigint,
    name character varying,
    client_type character varying,
    status character varying,
    match_rank REAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.name,
        c.client_type,
        c.status,
        CASE
            WHEN c.name % search_term THEN 0.9 * similarity(c.name, search_term)
            WHEN c.name ILIKE ('%' || search_term || '%') THEN 0.5
            ELSE 0.1
        END AS match_rank
    FROM public.crm_client c
    WHERE c.deleted = false
      AND (
          c.name % search_term
          OR c.name ILIKE ('%' || search_term || '%')
          OR c.legal_representative ILIKE ('%' || search_term || '%')
          OR c.credit_code ILIKE ('%' || search_term || '%')
      )
    ORDER BY match_rank DESC, c.name
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.search_clients(text, integer) IS '客户模糊搜索函数';

-- 项目模糊搜索函数
CREATE OR REPLACE FUNCTION public.search_matters(search_term TEXT, limit_count INTEGER DEFAULT 20)
RETURNS TABLE (
    id bigint,
    title character varying,
    matter_type character varying,
    matter_status character varying,
    match_rank REAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        m.id,
        m.title,
        m.matter_type,
        m.matter_status,
        CASE
            WHEN m.title % search_term THEN 0.9 * similarity(m.title, search_term)
            WHEN m.title ILIKE ('%' || search_term || '%') THEN 0.5
            ELSE 0.1
        END AS match_rank
    FROM public.matter m
    WHERE m.deleted = false
      AND (
          m.title % search_term
          OR m.title ILIKE ('%' || search_term || '%')
          OR m.case_no ILIKE ('%' || search_term || '%')
          OR m.opposing_party ILIKE ('%' || search_term || '%')
      )
    ORDER BY match_rank DESC, m.title
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.search_matters(text, integer) IS '项目模糊搜索函数';

-- =====================================================
-- 完成标记
-- =====================================================

DO $$
DECLARE
    trgm_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO trgm_count
    FROM pg_indexes
    WHERE schemaname = 'public'
    AND indexdef LIKE '%USING gin%gin_trgm_ops%';

    RAISE NOTICE '========================================';
    RAISE NOTICE '全文搜索索引优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已创建 % 个三元组/全文搜索索引', trgm_count;
    RAISE NOTICE '';
    RAISE NOTICE '使用示例:';
    RAISE NOTICE '  -- 客户搜索';
    RAISE NOTICE '  SELECT * FROM search_clients(''贵州公司'');';
    RAISE NOTICE '';
    RAISE NOTICE '  -- 项目搜索';
    RAISE NOTICE '  SELECT * FROM search_matters(''合同纠纷'');';
    RAISE NOTICE '';
    RAISE NOTICE '三元组索引支持:';
    RAISE NOTICE '  1. 模糊匹配 (LIKE ''%term%'')';
    RAISE NOTICE '  2. 相似度搜索 (name % ''term'')';
    RAISE NOTICE '  3. 拼音搜索 (需要额外配置)';
END $$;

-- =====================================================
-- 合并自: 66-optimize-materialized-views.sql
-- =====================================================

-- 1. 工作台统计视图
-- =====================================================

-- 工作台概览统计
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_workbench_overview AS
SELECT
    'workbench' AS category,
    (SELECT COUNT(*) FROM public.matter WHERE deleted = false AND matter_status = 'IN_PROGRESS') AS matters_in_progress,
    (SELECT COUNT(*) FROM public.task WHERE deleted = false AND status IN ('PENDING', 'IN_PROGRESS')) AS pending_tasks,
    (SELECT COUNT(*) FROM public.wb_approval WHERE deleted = false AND approval_status = 'PENDING') AS pending_approvals,
    (SELECT COUNT(*) FROM public.crm_conflict_check WHERE deleted = false AND status = 'PENDING') AS pending_conflict_checks,
    (SELECT COUNT(*) FROM public.sys_notification WHERE deleted = false AND is_read = false) AS unread_notifications,
    CURRENT_TIMESTAMP AS last_updated;

COMMENT ON MATERIALIZED VIEW public.mv_workbench_overview IS '工作台概览统计';

-- 用户工作台统计
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_user_workbench_stats AS
SELECT
    u.id AS user_id,
    u.real_name AS user_name,
    u.department_id,
    d.name AS department_name,
    (SELECT COUNT(*) FROM public.matter m WHERE m.responsible_lawyer_id = u.id AND m.deleted = false AND m.matter_status = 'IN_PROGRESS') AS matters_count,
    (SELECT COUNT(*) FROM public.task t WHERE t.assignee_id = u.id AND t.deleted = false AND t.status IN ('PENDING', 'IN_PROGRESS')) AS tasks_count,
    (SELECT COUNT(*) FROM public.wb_approval a JOIN public.wb_approval_approver aa ON a.id = aa.approval_id WHERE aa.approver_id = u.id AND a.deleted = false AND a.approval_status = 'PENDING') AS approvals_count,
    (SELECT COUNT(*) FROM public.sys_notification n WHERE n.receiver_id = u.id AND n.deleted = false AND n.is_read = false) AS unread_count,
    CURRENT_TIMESTAMP AS last_updated
FROM public.sys_user u
LEFT JOIN public.sys_department d ON u.department_id = d.id
WHERE u.deleted = false AND u.status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_user_workbench_user ON public.mv_user_workbench_stats (user_id);
COMMENT ON MATERIALIZED VIEW public.mv_user_workbench_stats IS '用户工作台统计';

-- =====================================================
-- 2. 客户统计视图
-- =====================================================

-- 客户汇总统计
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_client_summary AS
SELECT
    c.status,
    c.client_type,
    c.category,
    COUNT(*) AS client_count,
    COUNT(DISTINCT m.id) AS matter_count,
    COALESCE(SUM(fc.contract_amount), 0) AS total_contract_amount,
    COALESCE(SUM(fp.amount), 0) AS total_payment_amount,
    COUNT(DISTINCT c.originator_id) AS originator_count,
    COUNT(DISTINCT c.responsible_lawyer_id) AS lawyer_count
FROM public.crm_client c
LEFT JOIN public.matter m ON m.client_id = c.id AND m.deleted = false
LEFT JOIN public.finance_contract fc ON fc.client_id = c.id AND fc.deleted = false AND fc.contract_status = 'SIGNED'
LEFT JOIN public.finance_payment fp ON fp.client_id = c.id AND fp.deleted = false AND fp.payment_status = 'COMPLETED'
WHERE c.deleted = false
GROUP BY c.status, c.client_type, c.category;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_client_summary_key ON public.mv_client_summary (status, client_type, category);
COMMENT ON MATERIALIZED VIEW public.mv_client_summary IS '客户汇总统计（按状态、类型、分类）';

-- 按月份统计新增客户
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_client_monthly_stats AS
SELECT
    TO_CHAR(c.created_at, 'YYYY-MM') AS month,
    c.client_type,
    COUNT(*) AS new_client_count,
    COUNT(DISTINCT c.originator_id) AS originator_count,
    COALESCE(SUM(fc.contract_amount), 0) AS total_contract_value
FROM public.crm_client c
LEFT JOIN public.finance_contract fc ON fc.client_id = c.id AND fc.deleted = false
WHERE c.deleted = false
  AND c.created_at >= DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '2 years'
GROUP BY TO_CHAR(c.created_at, 'YYYY-MM'), c.client_type
ORDER BY month DESC, c.client_type;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_client_monthly_key ON public.mv_client_monthly_stats (month, client_type);
COMMENT ON MATERIALIZED VIEW public.mv_client_monthly_stats IS '按月统计新增客户';

-- =====================================================
-- 3. 项目统计视图
-- =====================================================

-- 项目汇总统计
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_summary AS
SELECT
    m.matter_type,
    m.matter_status,
    COUNT(*) AS matter_count,
    COALESCE(SUM(fc.contract_amount), 0) AS total_contract_value,
    COALESCE(AVG(fc.contract_amount), 0) AS avg_contract_value,
    COUNT(DISTINCT m.client_id) AS client_count,
    COUNT(DISTINCT m.responsible_lawyer_id) AS lawyer_count
FROM public.matter m
LEFT JOIN public.finance_contract fc ON fc.matter_id = m.id AND fc.deleted = false
WHERE m.deleted = false
GROUP BY m.matter_type, m.matter_status;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_matter_summary_key ON public.mv_matter_summary (matter_type, matter_status);
COMMENT ON MATERIALIZED VIEW public.mv_matter_summary IS '项目汇总统计（按类型、状态）';

-- 项目期限统计
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_deadline_summary AS
SELECT
    COUNT(*) FILTER (WHERE is_completed = false AND deadline_date < CURRENT_DATE) AS overdue_count,
    COUNT(*) FILTER (WHERE is_completed = false AND deadline_date >= CURRENT_DATE AND deadline_date <= CURRENT_DATE + INTERVAL '7 days') AS due_within_week,
    COUNT(*) FILTER (WHERE is_completed = false AND deadline_date >= CURRENT_DATE AND deadline_date <= CURRENT_DATE + INTERVAL '30 days') AS due_within_month,
    COUNT(*) FILTER (WHERE is_completed = false) AS pending_count,
    COUNT(*) FILTER (WHERE is_completed = true) AS completed_count
FROM public.matter_deadline
WHERE deleted = false;

COMMENT ON MATERIALIZED VIEW public.mv_deadline_summary IS '项目期限统计概览';

-- 按律师统计项目
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_lawyer_matter_stats AS
SELECT
    m.responsible_lawyer_id AS lawyer_id,
    u.real_name AS lawyer_name,
    u.department_id,
    d.name AS department_name,
    COUNT(*) FILTER (WHERE m.matter_status = 'IN_PROGRESS') AS matters_in_progress,
    COUNT(*) FILTER (WHERE m.matter_status = 'COMPLETED') AS matters_completed,
    COUNT(*) FILTER (WHERE m.matter_status IN ('PENDING', 'IN_PROGRESS')) AS matters_pending,
    COALESCE(SUM(fc.contract_amount) FILTER (WHERE m.matter_status = 'COMPLETED'), 0) AS completed_contract_value,
    COALESCE(SUM(fc.contract_amount) FILTER (WHERE m.matter_status = 'IN_PROGRESS'), 0) AS in_progress_contract_value,
    CURRENT_TIMESTAMP AS last_updated
FROM public.matter m
JOIN public.sys_user u ON u.id = m.responsible_lawyer_id
LEFT JOIN public.sys_department d ON d.id = u.department_id
LEFT JOIN public.finance_contract fc ON fc.matter_id = m.id AND fc.deleted = false
WHERE m.deleted = false AND u.deleted = false
GROUP BY m.responsible_lawyer_id, u.real_name, u.department_id, d.name;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_lawyer_matter_key ON public.mv_lawyer_matter_stats (lawyer_id);
COMMENT ON MATERIALIZED VIEW public.mv_lawyer_matter_stats IS '按律师统计项目';

-- =====================================================
-- 4. 财务统计视图
-- =====================================================

-- 收款统计（按月）
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_payment_monthly_stats AS
SELECT
    TO_CHAR(fp.payment_date, 'YYYY-MM') AS month,
    fp.payment_method,
    COUNT(*) AS payment_count,
    SUM(fp.amount) AS total_amount,
    AVG(fp.amount) AS avg_amount,
    COUNT(DISTINCT fp.client_id) AS client_count,
    COUNT(DISTINCT fp.contract_id) AS contract_count
FROM public.finance_payment fp
WHERE fp.deleted = false
  AND fp.payment_status = 'COMPLETED'
  AND fp.payment_date >= DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '2 years'
GROUP BY TO_CHAR(fp.payment_date, 'YYYY-MM'), fp.payment_method
ORDER BY month DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_payment_monthly_key ON public.mv_payment_monthly_stats (month, payment_method);
COMMENT ON MATERIALIZED VIEW public.mv_payment_monthly_stats IS '按月统计收款';

-- 提成统计（按月）
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_commission_monthly_stats AS
SELECT
    TO_CHAR(fcom.created_at, 'YYYY-MM') AS month,
    fcom.compensation_type,
    COUNT(*) AS commission_count,
    SUM(fcom.commission_amount) AS total_commission,
    AVG(fcom.commission_amount) AS avg_commission,
    COUNT(DISTINCT fcom.originator_id) AS originator_count,
    COUNT(*) FILTER (WHERE fcom.status = 'PAID') AS paid_count,
    SUM(fcom.commission_amount) FILTER (WHERE fcom.status = 'PAID') AS paid_amount
FROM public.finance_commission fcom
WHERE fcom.deleted = false
  AND fcom.created_at >= DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '2 years'
GROUP BY TO_CHAR(fcom.created_at, 'YYYY-MM'), fcom.compensation_type
ORDER BY month DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_commission_monthly_key ON public.mv_commission_monthly_stats (month, compensation_type);
COMMENT ON MATERIALIZED VIEW public.mv_commission_monthly_stats IS '按月统计提成';

-- 发票统计（按月）
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_invoice_monthly_stats AS
SELECT
    TO_CHAR(fi.issue_date, 'YYYY-MM') AS month,
    fi.invoice_type,
    COUNT(*) AS invoice_count,
    SUM(fi.tax_exclusive_amount) AS total_tax_exclusive_amount,
    SUM(fi.tax_amount) AS total_tax_amount,
    SUM(fi.total_amount) AS total_amount
FROM public.finance_invoice fi
WHERE fi.deleted = false
  AND fi.invoice_status IN ('ISSUED', 'TAX_FILED')
  AND fi.issue_date >= DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '2 years'
GROUP BY TO_CHAR(fi.issue_date, 'YYYY-MM'), fi.invoice_type
ORDER BY month DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_invoice_monthly_key ON public.mv_invoice_monthly_stats (month, invoice_type);
COMMENT ON MATERIALIZED VIEW public.mv_invoice_monthly_stats IS '按月统计发票';

-- =====================================================
-- 5. 人力资源统计视图
-- =====================================================

-- 员工统计概览
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_employee_summary AS
SELECT
    d.id AS department_id,
    d.name AS department_name,
    COUNT(*) FILTER (WHERE e.status = 'ACTIVE') AS active_count,
    COUNT(*) FILTER (WHERE e.status = 'ACTIVE' AND e.gender = 'MALE') AS male_count,
    COUNT(*) FILTER (WHERE e.status = 'ACTIVE' AND e.gender = 'FEMALE') AS female_count,
    COUNT(*) FILTER (WHERE e.position LIKE '%律师%' AND e.status = 'ACTIVE') AS lawyer_count,
    COUNT(*) FILTER (WHERE e.position LIKE '%实习%' AND e.status = 'ACTIVE') AS intern_count,
    CURRENT_TIMESTAMP AS last_updated
FROM public.hr_employee e
JOIN public.sys_department d ON d.id = e.department_id
WHERE e.deleted = false AND d.deleted = false
GROUP BY d.id, d.name;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_employee_summary_key ON public.mv_employee_summary (department_id);
COMMENT ON MATERIALIZED VIEW public.mv_employee_summary IS '员工统计概览（按部门）';

-- 考勤统计（按月）
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_attendance_monthly_stats AS
SELECT
    TO_CHAR(a.attendance_date, 'YYYY-MM') AS month,
    a.department_id,
    d.name AS department_name,
    COUNT(*) FILTER (WHERE a.attendance_status = 'PRESENT') AS present_days,
    COUNT(*) FILTER (WHERE a.attendance_status = 'ABSENT') AS absent_days,
    COUNT(*) FILTER (WHERE a.attendance_status = 'LATE') AS late_days,
    COUNT(*) FILTER (WHERE a.attendance_status = 'LEAVE') AS leave_days,
    COUNT(*) FILTER (WHERE a.attendance_status = 'BUSINESS_TRIP') AS business_trip_days,
    COUNT(*) AS total_days
FROM public.hr_attendance a
JOIN public.sys_department d ON d.id = a.department_id
WHERE a.deleted = false
  AND a.attendance_date >= DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '1 year'
GROUP BY TO_CHAR(a.attendance_date, 'YYYY-MM'), a.department_id, d.name;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_attendance_monthly_key ON public.mv_attendance_monthly_stats (month, department_id);
COMMENT ON MATERIALIZED VIEW public.mv_attendance_monthly_stats IS '按月统计考勤（按部门）';

-- =====================================================
-- 6. 行政统计视图
-- =====================================================

-- 印章使用统计（按月）
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_seal_usage_monthly_stats AS
SELECT
    TO_CHAR(sa.used_at, 'YYYY-MM') AS month,
    si.seal_type,
    COUNT(*) AS usage_count,
    COUNT(*) FILTER (WHERE sa.status = 'USED') AS used_count,
    COUNT(DISTINCT sa.applicant_id) AS applicant_count
FROM public.seal_application sa
JOIN public.seal_info si ON si.id = sa.seal_id
WHERE sa.deleted = false
  AND sa.status = 'USED'
  AND sa.used_at >= DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '1 year'
GROUP BY TO_CHAR(sa.used_at, 'YYYY-MM'), si.seal_type
ORDER BY month DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_seal_usage_key ON public.mv_seal_usage_monthly_stats (month, seal_type);
COMMENT ON MATERIALIZED VIEW public.mv_seal_usage_monthly_stats IS '按月统计印章使用';

-- 注：会议室使用统计功能暂未实现
-- CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_meeting_room_usage_stats AS
-- SELECT
--     TO_CHAR(r.start_time, 'YYYY-MM') AS month,
--     r.room_id,
--     mr.room_name,
--     COUNT(*) AS reservation_count,
--     SUM(EXTRACT(EPOCH FROM (r.end_time - r.start_time))/3600) AS total_hours,
--     COUNT(*) FILTER (WHERE r.reservation_status = 'COMPLETED') AS completed_count,
--     COUNT(*) FILTER (WHERE r.reservation_status = 'CANCELLED') AS cancelled_count
-- FROM public.meeting_room_reservation r
-- JOIN public.meeting_room mr ON mr.id = r.room_id
-- WHERE r.deleted = false
--   AND r.start_time >= DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '1 year'
-- GROUP BY TO_CHAR(r.start_time, 'YYYY-MM'), r.room_id, mr.room_name
-- ORDER BY month DESC;

-- =====================================================
-- 7. 创建物化视图刷新函数
-- =====================================================

-- 刷新所有物化视图的函数
CREATE OR REPLACE FUNCTION public.refresh_all_materialized_views()
RETURNS void AS $$
DECLARE
    mv_name TEXT;
    mv_count INTEGER := 0;
BEGIN
    -- 刷新可以并发刷新的物化视图
    FOR mv_name IN
        SELECT matviewname
        FROM pg_matviews
        WHERE schemaname = 'public'
        AND matviewname LIKE 'mv_%'
    LOOP
        BEGIN
            EXECUTE 'REFRESH MATERIALIZED VIEW CONCURRENTLY public.' || quote_ident(mv_name);
            mv_count := mv_count + 1;
            RAISE NOTICE '已刷新物化视图: %', mv_name;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE '无法并发刷新 %，尝试普通刷新', mv_name;
            EXECUTE 'REFRESH MATERIALIZED VIEW public.' || quote_ident(mv_name);
            mv_count := mv_count + 1;
        END;
    END LOOP;

    RAISE NOTICE '========================================';
    RAISE NOTICE '物化视图刷新完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '共刷新 % 个物化视图', mv_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.refresh_all_materialized_views() IS '刷新所有物化视图';

-- 按需刷新指定物化视图的函数
CREATE OR REPLACE FUNCTION public.refresh_materialized_view(view_name TEXT)
RETURNS void AS $$
BEGIN
    EXECUTE format('REFRESH MATERIALIZED VIEW CONCURRENTLY public.%I', view_name);
    RAISE NOTICE '已刷新物化视图: %', view_name;
EXCEPTION
    WHEN OTHERS THEN
        EXECUTE format('REFRESH MATERIALIZED VIEW public.%I', view_name);
        RAISE NOTICE '已刷新物化视图(非并发): %', view_name;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.refresh_materialized_view(text) IS '刷新指定物化视图';

-- =====================================================
-- 完成标记
-- =====================================================

DO $$
DECLARE
    mv_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO mv_count
    FROM pg_matviews
    WHERE schemaname = 'public'
    AND matviewname LIKE 'mv_%';

    RAISE NOTICE '========================================';
    RAISE NOTICE '物化视图优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已创建 % 个物化视图', mv_count;
    RAISE NOTICE '';
    RAISE NOTICE '刷新物化视图:';
    RAISE NOTICE '  SELECT public.refresh_all_materialized_views();';
    RAISE NOTICE '  -- 或刷新单个视图';
    RAISE NOTICE '  SELECT public.refresh_materialized_view(''mv_client_summary'');';
    RAISE NOTICE '';
    RAISE NOTICE '定时刷新(需安装 pg_cron):';
    RAISE NOTICE '  SELECT cron.schedule(''refresh-mvs'', ''0 2 * * *'', ';
    RAISE NOTICE '    ''SELECT public.refresh_all_materialized_views();'');';
END $$;

-- =====================================================
-- 合并自: 67-optimize-triggers.sql
-- =====================================================

-- 1. 创建通用更新时间戳函数
-- =====================================================

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.update_updated_at_column() IS '通用更新时间戳触发器函数';

-- =====================================================
-- 2. 为所有包含 updated_at 字段的表添加触发器
-- =====================================================

-- 系统管理模块触发器
DROP TRIGGER IF EXISTS trg_sys_user_updated_at ON public.sys_user;
CREATE TRIGGER trg_sys_user_updated_at
    BEFORE UPDATE ON public.sys_user
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_sys_role_updated_at ON public.sys_role;
CREATE TRIGGER trg_sys_role_updated_at
    BEFORE UPDATE ON public.sys_role
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_sys_menu_updated_at ON public.sys_menu;
CREATE TRIGGER trg_sys_menu_updated_at
    BEFORE UPDATE ON public.sys_menu
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_sys_department_updated_at ON public.sys_department;
CREATE TRIGGER trg_sys_department_updated_at
    BEFORE UPDATE ON public.sys_department
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_sys_config_updated_at ON public.sys_config;
CREATE TRIGGER trg_sys_config_updated_at
    BEFORE UPDATE ON public.sys_config
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_sys_dict_type_updated_at ON public.sys_dict_type;
CREATE TRIGGER trg_sys_dict_type_updated_at
    BEFORE UPDATE ON public.sys_dict_type
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_sys_dict_item_updated_at ON public.sys_dict_item;
CREATE TRIGGER trg_sys_dict_item_updated_at
    BEFORE UPDATE ON public.sys_dict_item
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_sys_notification_updated_at ON public.sys_notification;
CREATE TRIGGER trg_sys_notification_updated_at
    BEFORE UPDATE ON public.sys_notification
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_sys_login_log_updated_at ON public.sys_login_log;
CREATE TRIGGER trg_sys_login_log_updated_at
    BEFORE UPDATE ON public.sys_login_log
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_sys_operation_log_updated_at ON public.sys_operation_log;
CREATE TRIGGER trg_sys_operation_log_updated_at
    BEFORE UPDATE ON public.sys_operation_log
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 客户管理模块触发器
DROP TRIGGER IF EXISTS trg_crm_client_updated_at ON public.crm_client;
CREATE TRIGGER trg_crm_client_updated_at
    BEFORE UPDATE ON public.crm_client
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_client_change_history_updated_at ON public.crm_client_change_history;
CREATE TRIGGER trg_crm_client_change_history_updated_at
    BEFORE UPDATE ON public.crm_client_change_history
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_client_contact_record_updated_at ON public.crm_client_contact_record;
CREATE TRIGGER trg_crm_client_contact_record_updated_at
    BEFORE UPDATE ON public.crm_client_contact_record
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_client_related_company_updated_at ON public.crm_client_related_company;
CREATE TRIGGER trg_crm_client_related_company_updated_at
    BEFORE UPDATE ON public.crm_client_related_company
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_client_shareholder_updated_at ON public.crm_client_shareholder;
CREATE TRIGGER trg_crm_client_shareholder_updated_at
    BEFORE UPDATE ON public.crm_client_shareholder
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_client_tag_updated_at ON public.crm_client_tag;
CREATE TRIGGER trg_crm_client_tag_updated_at
    BEFORE UPDATE ON public.crm_client_tag
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_contact_updated_at ON public.crm_contact;
CREATE TRIGGER trg_crm_contact_updated_at
    BEFORE UPDATE ON public.crm_contact
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_conflict_check_updated_at ON public.crm_conflict_check;
CREATE TRIGGER trg_crm_conflict_check_updated_at
    BEFORE UPDATE ON public.crm_conflict_check
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_conflict_check_item_updated_at ON public.crm_conflict_check_item;
CREATE TRIGGER trg_crm_conflict_check_item_updated_at
    BEFORE UPDATE ON public.crm_conflict_check_item
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_lead_updated_at ON public.crm_lead;
CREATE TRIGGER trg_crm_lead_updated_at
    BEFORE UPDATE ON public.crm_lead
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_crm_lead_follow_up_updated_at ON public.crm_lead_follow_up;
CREATE TRIGGER trg_crm_lead_follow_up_updated_at
    BEFORE UPDATE ON public.crm_lead_follow_up
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 项目管理模块触发器
DROP TRIGGER IF EXISTS trg_matter_updated_at ON public.matter;
CREATE TRIGGER trg_matter_updated_at
    BEFORE UPDATE ON public.matter
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_matter_deadline_updated_at ON public.matter_deadline;
CREATE TRIGGER trg_matter_deadline_updated_at
    BEFORE UPDATE ON public.matter_deadline
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_matter_participant_updated_at ON public.matter_participant;
CREATE TRIGGER trg_matter_participant_updated_at
    BEFORE UPDATE ON public.matter_participant
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 财务管理模块触发器
DROP TRIGGER IF EXISTS trg_finance_contract_updated_at ON public.finance_contract;
CREATE TRIGGER trg_finance_contract_updated_at
    BEFORE UPDATE ON public.finance_contract
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_finance_payment_updated_at ON public.finance_payment;
CREATE TRIGGER trg_finance_payment_updated_at
    BEFORE UPDATE ON public.finance_payment
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_finance_payment_amendment_updated_at ON public.finance_payment_amendment;
CREATE TRIGGER trg_finance_payment_amendment_updated_at
    BEFORE UPDATE ON public.finance_payment_amendment
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_finance_commission_updated_at ON public.finance_commission;
CREATE TRIGGER trg_finance_commission_updated_at
    BEFORE UPDATE ON public.finance_commission
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_finance_commission_detail_updated_at ON public.finance_commission_detail;
CREATE TRIGGER trg_finance_commission_detail_updated_at
    BEFORE UPDATE ON public.finance_commission_detail
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_finance_invoice_updated_at ON public.finance_invoice;
CREATE TRIGGER trg_finance_invoice_updated_at
    BEFORE UPDATE ON public.finance_invoice
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 文档管理模块触发器
DROP TRIGGER IF EXISTS trg_doc_file_updated_at ON public.doc_file;
CREATE TRIGGER trg_doc_file_updated_at
    BEFORE UPDATE ON public.doc_file
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_doc_version_updated_at ON public.doc_version;
CREATE TRIGGER trg_doc_version_updated_at
    BEFORE UPDATE ON public.doc_version
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 证据管理模块触发器
DROP TRIGGER IF EXISTS trg_evidence_updated_at ON public.evidence;
CREATE TRIGGER trg_evidence_updated_at
    BEFORE UPDATE ON public.evidence
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_evidence_list_updated_at ON public.evidence_list;
CREATE TRIGGER trg_evidence_list_updated_at
    BEFORE UPDATE ON public.evidence_list
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 档案管理模块触发器
DROP TRIGGER IF EXISTS trg_archive_updated_at ON public.archive;
CREATE TRIGGER trg_archive_updated_at
    BEFORE UPDATE ON public.archive
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_archive_borrow_updated_at ON public.archive_borrow;
CREATE TRIGGER trg_archive_borrow_updated_at
    BEFORE UPDATE ON public.archive_borrow
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 工时管理模块触发器
DROP TRIGGER IF EXISTS trg_timesheet_updated_at ON public.timesheet;
CREATE TRIGGER trg_timesheet_updated_at
    BEFORE UPDATE ON public.timesheet
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_timer_session_updated_at ON public.timer_session;
CREATE TRIGGER trg_timer_session_updated_at
    BEFORE UPDATE ON public.timer_session
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 任务管理模块触发器
DROP TRIGGER IF EXISTS trg_task_updated_at ON public.task;
CREATE TRIGGER trg_task_updated_at
    BEFORE UPDATE ON public.task
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_task_comment_updated_at ON public.task_comment;
CREATE TRIGGER trg_task_comment_updated_at
    BEFORE UPDATE ON public.task_comment
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 行政管理模块触发器
DROP TRIGGER IF EXISTS trg_seal_info_updated_at ON public.seal_info;
CREATE TRIGGER trg_seal_info_updated_at
    BEFORE UPDATE ON public.seal_info
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_seal_application_updated_at ON public.seal_application;
CREATE TRIGGER trg_seal_application_updated_at
    BEFORE UPDATE ON public.seal_application
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_meeting_room_updated_at ON public.meeting_room;
CREATE TRIGGER trg_meeting_room_updated_at
    BEFORE UPDATE ON public.meeting_room
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 注：会议室预约功能暂未实现
-- DROP TRIGGER IF EXISTS trg_meeting_room_reservation_updated_at ON public.meeting_room_reservation;
-- CREATE TRIGGER trg_meeting_room_reservation_updated_at
--     BEFORE UPDATE ON public.meeting_room_reservation
--     FOR EACH ROW
--     EXECUTE FUNCTION public.update_updated_at_column();

-- 资产管理模块触发器
DROP TRIGGER IF EXISTS trg_asset_inventory_updated_at ON public.asset_inventory;
CREATE TRIGGER trg_asset_inventory_updated_at
    BEFORE UPDATE ON public.asset_inventory
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 知识库模块触发器
DROP TRIGGER IF EXISTS trg_kb_article_updated_at ON public.kb_article;
CREATE TRIGGER trg_kb_article_updated_at
    BEFORE UPDATE ON public.kb_article
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_kb_category_updated_at ON public.kb_category;
CREATE TRIGGER trg_kb_category_updated_at
    BEFORE UPDATE ON public.kb_category
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_kb_case_updated_at ON public.kb_case;
CREATE TRIGGER trg_kb_case_updated_at
    BEFORE UPDATE ON public.kb_case
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 人力资源模块触发器
DROP TRIGGER IF EXISTS trg_hr_employee_updated_at ON public.hr_employee;
CREATE TRIGGER trg_hr_employee_updated_at
    BEFORE UPDATE ON public.hr_employee
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_hr_attendance_updated_at ON public.hr_attendance;
CREATE TRIGGER trg_hr_attendance_updated_at
    BEFORE UPDATE ON public.hr_attendance
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_hr_payroll_updated_at ON public.hr_payroll;
CREATE TRIGGER trg_hr_payroll_updated_at
    BEFORE UPDATE ON public.hr_payroll
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_hr_performance_updated_at ON public.hr_performance;
CREATE TRIGGER trg_hr_performance_updated_at
    BEFORE UPDATE ON public.hr_performance
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_hr_training_updated_at ON public.hr_training;
CREATE TRIGGER trg_hr_training_updated_at
    BEFORE UPDATE ON public.hr_training
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 质量管理模块触发器
DROP TRIGGER IF EXISTS trg_quality_check_updated_at ON public.quality_check;
CREATE TRIGGER trg_quality_check_updated_at
    BEFORE UPDATE ON public.quality_check
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_quality_risk_updated_at ON public.quality_risk;
CREATE TRIGGER trg_quality_risk_updated_at
    BEFORE UPDATE ON public.quality_risk
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 工作台模块触发器
DROP TRIGGER IF EXISTS trg_wb_approval_updated_at ON public.wb_approval;
CREATE TRIGGER trg_wb_approval_updated_at
    BEFORE UPDATE ON public.wb_approval
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_wb_schedule_updated_at ON public.wb_schedule;
CREATE TRIGGER trg_wb_schedule_updated_at
    BEFORE UPDATE ON public.wb_schedule
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 合同模板模块触发器
DROP TRIGGER IF EXISTS trg_contract_template_updated_at ON public.contract_template;
CREATE TRIGGER trg_contract_template_updated_at
    BEFORE UPDATE ON public.contract_template
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 外部集成触发器
DROP TRIGGER IF EXISTS trg_sys_external_integration_updated_at ON public.sys_external_integration;
CREATE TRIGGER trg_sys_external_integration_updated_at
    BEFORE UPDATE ON public.sys_external_integration
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- 开放API触发器
DROP TRIGGER IF EXISTS trg_openapi_client_file_updated_at ON public.openapi_client_file;
CREATE TRIGGER trg_openapi_client_file_updated_at
    BEFORE UPDATE ON public.openapi_client_file
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- =====================================================
-- 3. 创建软删除级联触发器（可选）
-- =====================================================

-- 用户软删除时级联处理关联数据
CREATE OR REPLACE FUNCTION public.cascade_soft_delete_user()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted = true AND (OLD.deleted = false OR OLD.deleted IS NULL) THEN
        -- 标记用户角色关联为已删除
        UPDATE public.sys_user_role SET deleted = true WHERE user_id = NEW.id;

        -- 标记用户会话为已删除
        UPDATE public.sys_user_session SET deleted = true WHERE user_id = NEW.id;

        -- 取消该用户作为负责律师的项目
        UPDATE public.matter SET responsible_lawyer_id = NULL
            WHERE responsible_lawyer_id = NEW.id;

        -- 取消该用户的任务
        UPDATE public.task SET assignee_id = NULL
            WHERE assignee_id = NEW.id AND status = 'PENDING';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_user_soft_delete ON public.sys_user;
CREATE TRIGGER trg_user_soft_delete
    BEFORE UPDATE ON public.sys_user
    FOR EACH ROW
    EXECUTE FUNCTION public.cascade_soft_delete_user();

COMMENT ON FUNCTION public.cascade_soft_delete_user() IS '用户软删除时级联处理关联数据';

-- 客户软删除时级联处理关联数据
CREATE OR REPLACE FUNCTION public.cascade_soft_delete_client()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted = true AND (OLD.deleted = false OR OLD.deleted IS NULL) THEN
        -- 标记联系人为已删除
        UPDATE public.crm_contact SET deleted = true WHERE client_id = NEW.id;

        -- 标记客户联系记录为已删除
        UPDATE public.crm_client_contact_record SET deleted = true WHERE client_id = NEW.id;

        -- 标记股东信息为已删除
        UPDATE public.crm_client_shareholder SET deleted = true WHERE client_id = NEW.id;

        -- 标记关联企业为已删除
        UPDATE public.crm_client_related_company SET deleted = true WHERE client_id = NEW.id;

        -- 标记变更历史为已删除
        UPDATE public.crm_client_change_history SET deleted = true WHERE client_id = NEW.id;

        -- 标记客户标签关联为已删除
        UPDATE public.crm_client_tag_relation SET deleted = true WHERE client_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_client_soft_delete ON public.crm_client;
CREATE TRIGGER trg_client_soft_delete
    BEFORE UPDATE ON public.crm_client
    FOR EACH ROW
    EXECUTE FUNCTION public.cascade_soft_delete_client();

COMMENT ON FUNCTION public.cascade_soft_delete_client() IS '客户软删除时级联处理关联数据';

-- =====================================================
-- 4. 创建版本号自动增加触发器（乐观锁）
-- =====================================================

CREATE OR REPLACE FUNCTION public.increment_version()
RETURNS TRIGGER AS $$
BEGIN
    -- 只在更新时增加版本号
    IF TG_OP = 'UPDATE' THEN
        NEW.version = COALESCE(OLD.version, 0) + 1;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.increment_version() IS '版本号自动增加触发器函数';

-- 为需要乐观锁的表添加版本号触发器
DROP TRIGGER IF EXISTS trg_sys_user_version ON public.sys_user;
CREATE TRIGGER trg_sys_user_version
    BEFORE UPDATE ON public.sys_user
    FOR EACH ROW
    EXECUTE FUNCTION public.increment_version();

DROP TRIGGER IF EXISTS trg_sys_login_log_version ON public.sys_login_log;
CREATE TRIGGER trg_sys_login_log_version
    BEFORE UPDATE ON public.sys_login_log
    FOR EACH ROW
    EXECUTE FUNCTION public.increment_version();

DROP TRIGGER IF EXISTS trg_sys_user_session_version ON public.sys_user_session;
CREATE TRIGGER trg_sys_user_session_version
    BEFORE UPDATE ON public.sys_user_session
    FOR EACH ROW
    EXECUTE FUNCTION public.increment_version();

DROP TRIGGER IF EXISTS trg_crm_client_tag_version ON public.crm_client_tag;
CREATE TRIGGER trg_crm_client_tag_version
    BEFORE UPDATE ON public.crm_client_tag
    FOR EACH ROW
    EXECUTE FUNCTION public.increment_version();

DROP TRIGGER IF EXISTS trg_sys_role_change_log_version ON public.sys_role_change_log;
CREATE TRIGGER trg_sys_role_change_log_version
    BEFORE UPDATE ON public.sys_role_change_log
    FOR EACH ROW
    EXECUTE FUNCTION public.increment_version();

-- =====================================================
-- 完成标记
-- =====================================================

DO $$
DECLARE
    trigger_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO trigger_count
    FROM pg_trigger
    WHERE tgname LIKE 'trg_%_updated_at'
    OR tgname LIKE 'trg_%_version';

    RAISE NOTICE '========================================';
    RAISE NOTICE '触发器优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已创建 % 个触发器', trigger_count;
    RAISE NOTICE '';
    RAISE NOTICE '触发器类型:';
    RAISE NOTICE '  1. updated_at 自动更新触发器';
    RAISE NOTICE '  2. version 自动增加触发器(乐观锁)';
    RAISE NOTICE '  3. 软删除级联触发器';
END $$;

-- =====================================================
-- 完成标记
-- =====================================================

DO $$
DECLARE
    fk_count INTEGER;
    idx_count INTEGER;
    mv_count INTEGER;
    trg_count INTEGER;
BEGIN
    -- 统计外键数量
    SELECT COUNT(*) INTO fk_count
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
    WHERE tc.constraint_schema = 'public'
    AND tc.constraint_type = 'FOREIGN KEY';

    -- 统计索引数量
    SELECT COUNT(*) INTO idx_count
    FROM pg_indexes
    WHERE schemaname = 'public';

    -- 统计物化视图数量
    SELECT COUNT(*) INTO mv_count
    FROM pg_matviews
    WHERE schemaname = 'public';

    -- 统计触发器数量
    SELECT COUNT(*) INTO trg_count
    FROM pg_trigger
    WHERE tgname LIKE 'trg_%';

    RAISE NOTICE '========================================';
    RAISE NOTICE '数据库优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '外键约束: % 个', fk_count;
    RAISE NOTICE '索引: % 个', idx_count;
    RAISE NOTICE '物化视图: % 个', mv_count;
    RAISE NOTICE '触发器: % 个', trg_count;
END $$;


-- =====================================================
-- 额外性能优化（v2.1 新增）
-- =====================================================
-- 说明：针对运行期间发现的性能问题，添加额外优化
-- 包含：缺失索引、物化视图、中文全文搜索、监控配置
-- =====================================================

-- =====================================================
-- 1. 补充缺失的关键索引
-- =====================================================

-- 客户表：高价值客户筛选（级别+分类+状态）
CREATE INDEX IF NOT EXISTS idx_client_level_category_status
    ON public.crm_client (level, category, status)
    WHERE deleted = false;

-- 客户表：按负责律师+级别+状态筛选（用于律师客户列表）
CREATE INDEX IF NOT EXISTS idx_client_lawyer_level_status
    ON public.crm_client (responsible_lawyer_id, level, status, created_at DESC)
    WHERE deleted = false;

-- 项目表：按负责律师+状态+更新时间（用于律师工作台）
CREATE INDEX IF NOT EXISTS idx_matter_lawyer_status_updated
    ON public.matter (responsible_lawyer_id, matter_status, updated_at DESC)
    WHERE deleted = false;

-- 文档表：按项目+状态+类型（用于项目文档列表）
CREATE INDEX IF NOT EXISTS idx_doc_matter_status_type
    ON public.doc_document (matter_id, doc_status, doc_type, created_at DESC)
    WHERE deleted = false;

-- 任务表：按分配人+优先级+状态（用于任务优先级排序）
CREATE INDEX IF NOT EXISTS idx_task_assignee_priority_status
    ON public.task (assignee_id, priority, status, due_date)
    WHERE deleted = false AND status IN ('PENDING', 'IN_PROGRESS');

-- 工时表：按用户+年月汇总（用于月度工时统计）
CREATE INDEX IF NOT EXISTS idx_timesheet_user_year_month
    ON public.timesheet (user_id, work_year, work_month, work_date DESC)
    WHERE deleted = false;

-- 收款表：按客户+状态+收款日期（用于客户应收款查询）
CREATE INDEX IF NOT EXISTS idx_payment_client_status_date
    ON public.finance_payment (client_id, payment_status, payment_date DESC)
    WHERE deleted = false;

-- 费用表：按项目+状态+创建时间（用于项目费用统计）
CREATE INDEX IF NOT EXISTS idx_expense_matter_status_date
    ON public.finance_expense (matter_id, expense_status, created_at DESC)
    WHERE deleted = false;

-- 质量检查表：按项目+检查日期（用于项目质量检查历史）
CREATE INDEX IF NOT EXISTS idx_quality_matter_date
    ON public.quality_check (matter_id, check_date DESC)
    WHERE deleted = false;

-- =====================================================
-- 2. 中文全文搜索索引（补充）
-- =====================================================

-- 客户表：客户名称中文搜索
CREATE INDEX IF NOT EXISTS idx_client_name_cn_trgm
    ON public.crm_client USING gin (name gin_trgm_ops)
    WHERE deleted = false;

-- 客户表：企业名称中文搜索
CREATE INDEX IF NOT EXISTS idx_client_company_name_cn_trgm
    ON public.crm_client USING gin (company_name gin_trgm_ops)
    WHERE deleted = false;

-- 项目表：项目名称中文搜索
CREATE INDEX IF NOT EXISTS idx_matter_name_cn_trgm
    ON public.matter USING gin (name gin_trgm_ops)
    WHERE deleted = false;

-- 项目表：案件编号模糊搜索
CREATE INDEX IF NOT EXISTS idx_matter_no_trgm
    ON public.matter USING gin (matter_no gin_trgm_ops)
    WHERE deleted = false;

-- 项目表：案号模糊搜索
CREATE INDEX IF NOT EXISTS idx_matter_case_no_trgm
    ON public.matter USING gin (case_no gin_trgm_ops)
    WHERE deleted = false;

-- 合同表：合同编号模糊搜索
CREATE INDEX IF NOT EXISTS idx_contract_no_trgm
    ON public.finance_contract USING gin (contract_no gin_trgm_ops)
    WHERE deleted = false;

-- 文档表：文档名称模糊搜索
CREATE INDEX IF NOT EXISTS idx_doc_name_trgm
    ON public.doc_document USING gin (doc_name gin_trgm_ops)
    WHERE deleted = false;

-- 任务表：任务标题和描述模糊搜索
CREATE INDEX IF NOT EXISTS idx_task_title_desc_trgm
    ON public.task USING gin ((title || ' ' || description) gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 3. 物化视图（工作台统计）
-- =====================================================

-- 工作台统计视图（用于快速加载工作台数据）
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_workbench_stats AS
SELECT
    'my_matters' AS stat_type,
    u.id AS user_id,
    u.real_name AS user_name,
    COUNT(*) FILTER (WHERE m.matter_status IN ('FILING', 'IN_PROGRESS', 'SUSPENDED')) AS my_matter_count,
    COUNT(*) FILTER (WHERE m.matter_status = 'IN_PROGRESS') AS in_progress_count,
    COUNT(*) FILTER (WHERE t.status IN ('PENDING', 'IN_PROGRESS') AND t.due_date < CURRENT_DATE + INTERVAL '7 days') AS urgent_task_count
FROM sys_user u
LEFT JOIN matter m ON u.id = m.responsible_lawyer_id AND m.deleted = false
LEFT JOIN task t ON u.id = t.assignee_id AND t.deleted = false
WHERE u.deleted = false AND u.status = 'ACTIVE'
GROUP BY u.id, u.real_name;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_workbench_stats_key ON public.mv_workbench_stats (user_id, stat_type);

-- 律师业绩统计视图（月度）
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_lawyer_performance_monthly AS
SELECT
    TO_CHAR(t.work_date, 'YYYY-MM') AS month,
    t.user_id,
    u.real_name AS user_name,
    u.department_id,
    d.name AS department_name,
    COUNT(*) AS work_days,
    SUM(t.work_hours) AS total_hours,
    SUM(t.work_hours) FILTER (WHERE t.work_type = 'BILLABLE') AS billable_hours,
    COUNT(DISTINCT t.matter_id) AS matter_count
FROM timesheet t
JOIN sys_user u ON t.user_id = u.id AND u.deleted = false
LEFT JOIN sys_department d ON u.department_id = d.id AND d.deleted = false
WHERE t.deleted = false
  AND t.work_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '6 months'
GROUP BY TO_CHAR(t.work_date, 'YYYY-MM'), t.user_id, u.real_name, u.department_id, d.name
ORDER BY month DESC, user_id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_lawyer_performance_key ON public.mv_lawyer_performance_monthly (month, user_id);

-- 客户统计视图
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_client_stats AS
SELECT
    'total' AS stat_type,
    COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active_client_count,
    COUNT(*) FILTER (WHERE status = 'POTENTIAL') AS potential_client_count,
    COUNT(*) FILTER (WHERE level = 'A') AS vip_client_count,
    COUNT(*) FILTER (WHERE client_type = 'ENTERPRISE') AS enterprise_count,
    COUNT(*) FILTER (WHERE client_type = 'INDIVIDUAL') AS individual_count
FROM crm_client
WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_client_stats_key ON public.mv_client_stats (stat_type);

-- =====================================================
-- 4. 自动刷新物化视图函数
-- =====================================================

-- 创建定时刷新物化视图的函数
CREATE OR REPLACE FUNCTION public.refresh_stats_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY public.mv_workbench_stats;
    REFRESH MATERIALIZED VIEW CONCURRENTLY public.mv_lawyer_performance_monthly;
    REFRESH MATERIALIZED VIEW CONCURRENTLY public.mv_client_stats;
    RAISE NOTICE '物化视图刷新完成';
END $$ LANGUAGE plpgsql;

-- =====================================================
-- 5. 慢查询监控配置
-- =====================================================

-- 启用 pg_stat_statements 扩展（需要 postgresql.conf 配置）
-- 加载方式：shared_preload_libraries = 'pg_stat_statements'
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- 创建慢查询日志函数（用于查询执行时间超过1秒的语句）
CREATE OR REPLACE FUNCTION public.get_slow_queries(min_duration_ms INTEGER DEFAULT 1000)
RETURNS TABLE (
    query_id bigint,
    query text,
    calls bigint,
    total_time numeric,
    mean_time numeric,
    rows bigint
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        sq.queryid,
        LEFT(sq.query, 200) AS query,
        sq.calls,
        ROUND(sq.total_exec_time::numeric, 2) AS total_time,
        ROUND(sq.mean_exec_time::numeric, 2) AS mean_time,
        sq.rows
    FROM pg_stat_statements sq
    WHERE sq.mean_exec_time > min_duration_ms
    ORDER BY sq.mean_exec_time DESC
    LIMIT 50;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 6. 数据保留策略（注释说明）
-- =====================================================

-- 日志表数据保留策略：
-- 1. sys_operation_log: 保留 90 天，超过的迁移到归档表或删除
-- 2. sys_login_log: 保留 180 天，超过的迁移到归档表或删除
-- 3. timesheet: 永久保留，但可考虑对3年前数据进行归档
-- 4. doc_document: 根据业务需求归档已关闭项目1年以上的文档
--
-- 建议创建定时任务（如 pg_cron 扩展）定期清理：
--
-- -- 示例：删除90天前的操作日志
-- DELETE FROM sys_operation_log WHERE created_at < CURRENT_DATE - INTERVAL '90 days';
--
-- -- 示例：归档旧数据
-- INSERT INTO sys_operation_log_archive SELECT * FROM sys_operation_log WHERE created_at < CURRENT_DATE - INTERVAL '1 year';
-- DELETE FROM sys_operation_log WHERE created_at < CURRENT_DATE - INTERVAL '1 year';

-- =====================================================
-- 7. 大表分区建议（注释说明）
-- =====================================================

-- 对于快速增长的大表，建议使用分区表提升性能：
--
-- sys_operation_log 分区表示例（需要在 Schema 脚本中创建）：
-- CREATE TABLE sys_operation_log (
--     id bigint,
--     user_id bigint,
--     module varchar(50),
--     operation_type varchar(50),
--     created_at timestamp,
--     deleted boolean
-- ) PARTITION BY RANGE (created_at);
--
-- 创建月度分区：
-- CREATE TABLE sys_operation_log_2026_01 PARTITION OF sys_operation_log
--     FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
--
-- 创建自动创建分区的函数：
-- CREATE OR REPLACE FUNCTION create_monthly_partition()
-- RETURNS void AS $$
-- DECLARE
--     partition_name text;
--     start_date text;
--     end_date text;
-- BEGIN
--     partition_name := 'sys_operation_log_' || TO_CHAR(CURRENT_DATE + INTERVAL '1 month', 'YYYY_MM');
--     start_date := TO_CHAR(CURRENT_DATE + INTERVAL '1 month', 'YYYY-MM') || '-01';
--     end_date := TO_CHAR(CURRENT_DATE + INTERVAL '2 months', 'YYYY-MM') || '-01';
--
--     EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF sys_operation_log
--         FOR VALUES FROM (%L) TO (%L)', partition_name, start_date, end_date);
-- END $$;
-- =====================================================

-- =====================================================
-- 8. 索引维护建议
-- =====================================================

-- 定期维护索引的 SQL 命令（建议每月执行一次）：
--
-- -- 分析表统计信息
-- ANALYZE sys_operation_log;
-- ANALYZE timesheet;
-- ANALYZE matter;
-- ANALYZE crm_client;
--
-- -- 清理死索引和无效索引
-- REINDEX TABLE CONCURRENTLY sys_operation_log;
-- REINDEX TABLE CONCURRENTLY timesheet;
--
-- -- 查看索引使用情况
-- SELECT
--     schemaname,
--     tablename,
--     indexname,
--     idx_scan as index_scans,
--     pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
-- ORDER BY idx_scan ASC;

-- =====================================================
-- 9. 完成标记
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '额外性能优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已添加:';
    RAISE NOTICE '  - 补充缺失的关键索引';
    RAISE NOTICE '  - 中文全文搜索索引';
    RAISE NOTICE '  - 工作台统计物化视图';
    RAISE NOTICE '  - 律师业绩统计物化视图';
    RAISE NOTICE '  - 客户统计物化视图';
    RAISE NOTICE '  - 慢查询监控函数';
    RAISE NOTICE '';
    RAISE NOTICE '定期维护建议:';
    RAISE NOTICE '  - 每月: REFRESH MATERIALIZED VIEW CONCURRENTLY';
    RAISE NOTICE '  - 每月: ANALYZE 表统计信息';
    RAISE NOTICE '  - 每月: 清理过期日志数据';
END $$;

