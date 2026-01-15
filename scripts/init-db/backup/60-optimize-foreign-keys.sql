-- =====================================================
-- 数据库优化脚本 - 添加缺失的外键约束
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-15
-- 描述: 补全系统中缺失的外键约束，确保数据一致性
-- 优先级: P0 (紧急)
-- =====================================================

-- =====================================================
-- 1. 客户管理模块 (CRM) 外键
-- =====================================================

-- crm_client 表外键
ALTER TABLE public.crm_client
    ADD CONSTRAINT IF NOT EXISTS fk_client_originator
    FOREIGN KEY (originator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_client_responsible_lawyer
    FOREIGN KEY (responsible_lawyer_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_client_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_client_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

COMMENT ON CONSTRAINT fk_client_originator ON public.crm_client IS '案源人关联';
COMMENT ON CONSTRAINT fk_client_responsible_lawyer ON public.crm_client IS '负责律师关联';

-- crm_client_change_history 表外键
ALTER TABLE public.crm_client_change_history
    ADD CONSTRAINT IF NOT EXISTS fk_client_change_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_client_change_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_client_change_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_client_contact_record 表外键
ALTER TABLE public.crm_client_contact_record
    ADD CONSTRAINT IF NOT EXISTS fk_contact_record_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_contact_record_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_client_related_company 表外键
ALTER TABLE public.crm_client_related_company
    ADD CONSTRAINT IF NOT EXISTS fk_related_company_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE;

-- crm_client_shareholder 表外键
ALTER TABLE public.crm_client_shareholder
    ADD CONSTRAINT IF NOT EXISTS fk_shareholder_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE;

-- crm_client_tag_relation 表外键
ALTER TABLE public.crm_client_tag_relation
    ADD CONSTRAINT IF NOT EXISTS fk_client_tag_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_client_tag_tag
    FOREIGN KEY (tag_id) REFERENCES public.crm_client_tag(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_client_tag_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_contact 表外键
ALTER TABLE public.crm_contact
    ADD CONSTRAINT IF NOT EXISTS fk_contact_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_contact_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_contact_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_conflict_check 表外键
ALTER TABLE public.crm_conflict_check
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_check_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_check_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_check_applicant
    FOREIGN KEY (applicant_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_check_reviewer
    FOREIGN KEY (reviewer_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_check_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_check_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_conflict_check_item 表外键
ALTER TABLE public.crm_conflict_check_item
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_item_check
    FOREIGN KEY (check_id) REFERENCES public.crm_conflict_check(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_item_matter
    FOREIGN KEY (related_matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_item_client
    FOREIGN KEY (related_client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_item_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_conflict_item_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- crm_lead 表外键
ALTER TABLE public.crm_lead
    ADD CONSTRAINT IF NOT EXISTS fk_lead_originator
    FOREIGN KEY (originator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_lead_responsible_user
    FOREIGN KEY (responsible_user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_lead_converted_client
    FOREIGN KEY (converted_to_client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_lead_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_lead_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

COMMENT ON CONSTRAINT fk_lead_originator ON public.crm_lead IS '案源人关联';
COMMENT ON CONSTRAINT fk_lead_responsible_user ON public.crm_lead IS '负责人关联';

-- crm_lead_follow_up 表外键
ALTER TABLE public.crm_lead_follow_up
    ADD CONSTRAINT IF NOT EXISTS fk_lead_follow_up_lead
    FOREIGN KEY (lead_id) REFERENCES public.crm_lead(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_lead_follow_up_user
    FOREIGN KEY (follow_user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_lead_follow_up_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 2. 项目管理模块 (Matter) 外键
-- =====================================================

-- matter 表外键
ALTER TABLE public.matter
    ADD CONSTRAINT IF NOT EXISTS fk_matter_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE RESTRICT,
    ADD CONSTRAINT IF NOT EXISTS fk_matter_responsible_lawyer
    FOREIGN KEY (responsible_lawyer_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_matter_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_matter_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

COMMENT ON CONSTRAINT fk_matter_responsible_lawyer ON public.matter IS '负责律师关联';

-- matter_participant 表外键
ALTER TABLE public.matter_participant
    ADD CONSTRAINT IF NOT EXISTS fk_participant_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_participant_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- matter_deadline 表外键
ALTER TABLE public.matter_deadline
    ADD CONSTRAINT IF NOT EXISTS fk_deadline_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_deadline_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- matter_client 表外键
ALTER TABLE public.matter_client
    ADD CONSTRAINT IF NOT EXISTS fk_matter_client_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_matter_client_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE CASCADE;

-- =====================================================
-- 3. 财务管理模块 (Finance) 外键
-- =====================================================

-- finance_contract 表外键
ALTER TABLE public.finance_contract
    ADD CONSTRAINT IF NOT EXISTS fk_contract_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE RESTRICT,
    ADD CONSTRAINT IF NOT EXISTS fk_contract_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_contract_originator
    FOREIGN KEY (originator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_contract_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_contract_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_payment 表外键
ALTER TABLE public.finance_payment
    ADD CONSTRAINT IF NOT EXISTS fk_payment_contract
    FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_payment_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_payment_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_payment_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_payment_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_payment_amendment 表外键
ALTER TABLE public.fin_payment_amendment
    ADD CONSTRAINT IF NOT EXISTS fk_payment_amendment_payment
    FOREIGN KEY (payment_id) REFERENCES public.finance_payment(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_payment_amendment_requested_by
    FOREIGN KEY (requested_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_payment_amendment_approved_by
    FOREIGN KEY (approved_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_commission 表外键
ALTER TABLE public.finance_commission
    ADD CONSTRAINT IF NOT EXISTS fk_commission_payment
    FOREIGN KEY (payment_id) REFERENCES public.finance_payment(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_fee
    FOREIGN KEY (fee_id) REFERENCES public.finance_fee(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_contract
    FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_rule
    FOREIGN KEY (rule_id) REFERENCES public.hr_commission_rule(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_originator
    FOREIGN KEY (originator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_approved_by
    FOREIGN KEY (approved_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_paid_by
    FOREIGN KEY (paid_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_updated_by
    FOREIGN KEY (updated_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_commission_detail 表外键
ALTER TABLE public.finance_commission_detail
    ADD CONSTRAINT IF NOT EXISTS fk_commission_detail_commission
    FOREIGN KEY (commission_id) REFERENCES public.finance_commission(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_commission_detail_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- finance_invoice 表外键
ALTER TABLE public.finance_invoice
    ADD CONSTRAINT IF NOT EXISTS fk_invoice_contract
    FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_invoice_payment
    FOREIGN KEY (payment_id) REFERENCES public.finance_payment(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_invoice_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_invoice_client
    FOREIGN KEY (client_id) REFERENCES public.crm_client(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_invoice_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 4. 行政管理模块 (Admin) 外键
-- =====================================================

-- admin_seal_application 表外键
ALTER TABLE public.admin_seal_application
    ADD CONSTRAINT IF NOT EXISTS fk_seal_applicant
    FOREIGN KEY (applicant_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_seal_approver
    FOREIGN KEY (approver_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- admin_meeting_room_reservation 表外键
ALTER TABLE public.admin_meeting_room_reservation
    ADD CONSTRAINT IF NOT EXISTS fk_meeting_room
    FOREIGN KEY (room_id) REFERENCES public.admin_meeting_room(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_meeting_organizer
    FOREIGN KEY (organizer_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 5. 工时管理模块 (Timesheet) 外键
-- =====================================================

-- timesheet 表外键
ALTER TABLE public.timesheet
    ADD CONSTRAINT IF NOT EXISTS fk_timesheet_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_timesheet_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_timesheet_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- timer_session 表外键
ALTER TABLE public.timer_session
    ADD CONSTRAINT IF NOT EXISTS fk_timer_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_timer_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL;

-- =====================================================
-- 6. 任务管理模块 (Task) 外键
-- =====================================================

-- task 表外键
ALTER TABLE public.task
    ADD CONSTRAINT IF NOT EXISTS fk_task_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_task_assignee
    FOREIGN KEY (assignee_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_task_creator
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- task_comment 表外键
ALTER TABLE public.task_comment
    ADD CONSTRAINT IF NOT EXISTS fk_task_comment_task
    FOREIGN KEY (task_id) REFERENCES public.task(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_task_comment_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 7. 人力资源模块 (HR) 外键
-- =====================================================

-- hr_employee 表外键
ALTER TABLE public.hr_employee
    ADD CONSTRAINT IF NOT EXISTS fk_employee_user
    FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_employee_department
    FOREIGN KEY (department_id) REFERENCES public.sys_department(id) ON DELETE SET NULL;

-- hr_attendance 表外键
ALTER TABLE public.hr_attendance
    ADD CONSTRAINT IF NOT EXISTS fk_attendance_employee
    FOREIGN KEY (employee_id) REFERENCES public.hr_employee(id) ON DELETE CASCADE;

-- hr_payroll 表外键
ALTER TABLE public.hr_payroll
    ADD CONSTRAINT IF NOT EXISTS fk_payroll_employee
    FOREIGN KEY (employee_id) REFERENCES public.hr_employee(id) ON DELETE CASCADE;

-- hr_training 表外键
ALTER TABLE public.hr_training
    ADD CONSTRAINT IF NOT EXISTS fk_training_created_by
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 8. 档案管理模块 (Archive) 外键
-- =====================================================

-- archive 表外键
ALTER TABLE public.archive
    ADD CONSTRAINT IF NOT EXISTS fk_archive_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_archive_creator
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- =====================================================
-- 9. 文档管理模块 (Document) 外键
-- =====================================================

-- doc_file 表外键
ALTER TABLE public.doc_file
    ADD CONSTRAINT IF NOT EXISTS fk_doc_matter
    FOREIGN KEY (matter_id) REFERENCES public.matter(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_doc_creator
    FOREIGN KEY (created_by) REFERENCES public.sys_user(id) ON DELETE SET NULL;

-- doc_version 表外键
ALTER TABLE public.doc_version
    ADD CONSTRAINT IF NOT EXISTS fk_doc_version_file
    FOREIGN KEY (file_id) REFERENCES public.doc_file(id) ON DELETE CASCADE,
    ADD CONSTRAINT IF NOT EXISTS fk_doc_version_creator
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
