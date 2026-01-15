-- =====================================================
-- 数据库优化脚本 - 添加检查约束
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-15
-- 描述: 为关键字段添加数据有效性检查约束
-- 优先级: P1 (高优先级)
-- 说明: 检查约束在数据库层面确保数据有效性
-- =====================================================

-- =====================================================
-- 1. 系统管理模块检查约束
-- =====================================================

-- sys_user 表：邮箱格式检查
ALTER TABLE public.sys_user
    ADD CONSTRAINT IF NOT EXISTS chk_user_email_format
    CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- sys_user 表：手机号格式检查（中国手机号）
ALTER TABLE public.sys_user
    ADD CONSTRAINT IF NOT EXISTS chk_user_phone_format
    CHECK (phone IS NULL OR phone ~* '^1[3-9][0-9]{9}$');

-- sys_user 表：状态值检查
ALTER TABLE public.sys_user
    ADD CONSTRAINT IF NOT EXISTS chk_user_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

-- sys_user 表：薪酬模式检查
ALTER TABLE public.sys_user
    ADD CONSTRAINT IF NOT EXISTS chk_user_compensation_type
    CHECK (compensation_type IN ('COMMISSION', 'SALARIED', 'HYBRID'));

-- sys_role 表：数据范围检查
ALTER TABLE public.sys_role
    ADD CONSTRAINT IF NOT EXISTS chk_role_data_scope
    CHECK (data_scope IN ('ALL', 'DEPT', 'DEPT_AND_CHILD', 'SELF', 'CUSTOM'));

-- sys_role 表：状态检查
ALTER TABLE public.sys_role
    ADD CONSTRAINT IF NOT EXISTS chk_role_status
    CHECK (status IN ('ACTIVE', 'INACTIVE'));

-- sys_department 表：状态检查
ALTER TABLE public.sys_department
    ADD CONSTRAINT IF NOT EXISTS chk_dept_status
    CHECK (status IN ('ACTIVE', 'INACTIVE'));

-- sys_menu 表：菜单类型检查
ALTER TABLE public.sys_menu
    ADD CONSTRAINT IF NOT EXISTS chk_menu_type
    CHECK (menu_type IN ('DIRECTORY', 'MENU', 'BUTTON'));

-- sys_menu 表：状态检查
ALTER TABLE public.sys_menu
    ADD CONSTRAINT IF NOT EXISTS chk_menu_status
    CHECK (status IN ('ENABLED', 'DISABLED'));

-- sys_config 表：配置类型检查
ALTER TABLE public.sys_config
    ADD CONSTRAINT IF NOT EXISTS chk_config_type
    CHECK (config_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON', 'TEXT'));

-- sys_dict_type 表：状态检查
ALTER TABLE public.sys_dict_type
    ADD CONSTRAINT IF NOT EXISTS chk_dict_type_status
    CHECK (status IN ('ENABLED', 'DISABLED'));

-- sys_dict_item 表：状态检查
ALTER TABLE public.sys_dict_item
    ADD CONSTRAINT IF NOT EXISTS chk_dict_item_status
    CHECK (status IN ('ENABLED', 'DISABLED'));

-- sys_login_log 表：状态检查
ALTER TABLE public.sys_login_log
    ADD CONSTRAINT IF NOT EXISTS chk_login_log_status
    CHECK (status IN ('SUCCESS', 'FAILURE'));

-- sys_login_log 表：设备类型检查
ALTER TABLE public.sys_login_log
    ADD CONSTRAINT IF NOT EXISTS chk_login_log_device
    CHECK (device_type IN ('PC', 'MOBILE', 'TABLET', 'UNKNOWN'));

-- sys_notification 表：类型检查
ALTER TABLE public.sys_notification
    ADD CONSTRAINT IF NOT EXISTS chk_notification_type
    CHECK (type IN ('SYSTEM', 'APPROVAL', 'TASK', 'REMINDER', 'ANNOUNCEMENT'));

-- sys_user_session 表：状态检查
ALTER TABLE public.sys_user_session
    ADD CONSTRAINT IF NOT EXISTS chk_session_status
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'));

-- sys_data_handover 表：交接类型检查
ALTER TABLE public.sys_data_handover
    ADD CONSTRAINT IF NOT EXISTS chk_handover_type
    CHECK (handover_type IN ('RESIGNATION', 'PROJECT', 'CLIENT', 'LEAD', 'TASK'));

-- sys_data_handover 表：状态检查
ALTER TABLE public.sys_data_handover
    ADD CONSTRAINT IF NOT EXISTS chk_handover_status
    CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CONFIRMED', 'CANCELLED'));

-- sys_operation_log 表：状态检查
ALTER TABLE public.sys_operation_log
    ADD CONSTRAINT IF NOT EXISTS chk_operation_status
    CHECK (status IN ('SUCCESS', 'FAILURE', 'PARTIAL'));

-- =====================================================
-- 2. 客户管理模块检查约束
-- =====================================================

-- crm_client 表：客户类型检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT IF NOT EXISTS chk_client_type
    CHECK (client_type IN ('INDIVIDUAL', 'ENTERPRISE', 'GOVERNMENT', 'OTHER'));

-- crm_client 表：客户级别检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT IF NOT EXISTS chk_client_level
    CHECK (level IN ('A', 'B', 'C'));

-- crm_client 表：客户分类检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT IF NOT EXISTS chk_client_category
    CHECK (category IN ('VIP', 'NORMAL', 'POTENTIAL'));

-- crm_client 表：客户状态检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT IF NOT EXISTS chk_client_status
    CHECK (status IN ('POTENTIAL', 'ACTIVE', 'INACTIVE', 'BLACKLIST'));

-- crm_client 表：身份证格式检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT IF NOT EXISTS chk_client_id_card
    CHECK (id_card IS NULL OR id_card ~* '^[1-9][0-9]{16}[0-9Xx]$');

-- crm_client 表：信用代码格式检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT IF NOT EXISTS chk_client_credit_code
    CHECK (credit_code IS NULL OR credit_code ~* '^[0-9A-Z]{18}$');

-- crm_client 表：手机号格式检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT IF NOT EXISTS chk_client_phone
    CHECK (contact_phone IS NULL OR contact_phone ~* '^1[3-9][0-9]{9}$');

-- crm_client 表：邮箱格式检查
ALTER TABLE public.crm_client
    ADD CONSTRAINT IF NOT EXISTS chk_client_email
    CHECK (contact_email IS NULL OR contact_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- crm_contact 表：邮箱格式检查
ALTER TABLE public.crm_contact
    ADD CONSTRAINT IF NOT EXISTS chk_contact_email
    CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- crm_contact 表：手机号格式检查
ALTER TABLE public.crm_contact
    ADD CONSTRAINT IF NOT EXISTS chk_contact_phone
    CHECK (mobile_phone IS NULL OR mobile_phone ~* '^1[3-9][0-9]{9}$');

-- crm_client_change_history 表：变更类型检查
ALTER TABLE public.crm_client_change_history
    ADD CONSTRAINT IF NOT EXISTS chk_change_type
    CHECK (change_type IN ('NAME', 'REGISTERED_CAPITAL', 'LEGAL_REPRESENTATIVE',
                          'ADDRESS', 'BUSINESS_SCOPE', 'SHAREHOLDER', 'OTHER'));

-- crm_client_related_company 表：关联类型检查
ALTER TABLE public.crm_client_related_company
    ADD CONSTRAINT IF NOT EXISTS chk_related_company_type
    CHECK (related_company_type IN ('PARENT', 'SUBSIDIARY', 'AFFILIATE'));

-- crm_client_shareholder 表：股东类型检查
ALTER TABLE public.crm_client_shareholder
    ADD CONSTRAINT IF NOT EXISTS chk_shareholder_type
    CHECK (shareholder_type IN ('INDIVIDUAL', 'ENTERPRISE'));

-- crm_client_shareholder 表：持股比例范围检查
ALTER TABLE public.crm_client_shareholder
    ADD CONSTRAINT IF NOT EXISTS chk_shareholding_ratio
    CHECK (shareholding_ratio IS NULL OR (shareholding_ratio >= 0 AND shareholding_ratio <= 100));

-- crm_lead 表：案源类型检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT IF NOT EXISTS chk_lead_type
    CHECK (lead_type IN ('INDIVIDUAL', 'ENTERPRISE', 'GOVERNMENT'));

-- crm_lead 表：状态检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT IF NOT EXISTS chk_lead_status
    CHECK (status IN ('PENDING', 'FOLLOWING', 'CONVERTED', 'ABANDONED'));

-- crm_lead 表：优先级检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT IF NOT EXISTS chk_lead_priority
    CHECK (priority IN ('HIGH', 'NORMAL', 'LOW'));

-- crm_lead 表：手机号格式检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT IF NOT EXISTS chk_lead_phone
    CHECK (contact_phone IS NULL OR contact_phone ~* '^1[3-9][0-9]{9}$');

-- crm_lead 表：邮箱格式检查
ALTER TABLE public.crm_lead
    ADD CONSTRAINT IF NOT EXISTS chk_lead_email
    CHECK (contact_email IS NULL OR contact_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- crm_lead_follow_up 表：跟进方式检查
ALTER TABLE public.crm_lead_follow_up
    ADD CONSTRAINT IF NOT EXISTS chk_follow_type
    CHECK (follow_type IN ('PHONE', 'EMAIL', 'VISIT', 'MEETING', 'OTHER'));

-- crm_lead_follow_up 表：跟进结果检查
ALTER TABLE public.crm_lead_follow_up
    ADD CONSTRAINT IF NOT EXISTS chk_follow_result
    CHECK (follow_result IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE'));

-- crm_conflict_check 表：检查类型检查
ALTER TABLE public.crm_conflict_check
    ADD CONSTRAINT IF NOT EXISTS chk_conflict_check_type
    CHECK (check_type IN ('NEW_CLIENT', 'NEW_MATTER', 'MANUAL', 'PERIODIC'));

-- crm_conflict_check 表：状态检查
ALTER TABLE public.crm_conflict_check
    ADD CONSTRAINT IF NOT EXISTS chk_conflict_check_status
    CHECK (status IN ('PENDING', 'CHECKING', 'PASSED', 'CONFLICT', 'EXEMPTION_PENDING',
                      'WAIVED', 'REJECTED'));

-- crm_conflict_check_item 表：当事人类型检查
ALTER TABLE public.crm_conflict_check_item
    ADD CONSTRAINT IF NOT EXISTS chk_party_type
    CHECK (party_type IN ('CLIENT', 'OPPOSING', 'RELATED', 'THIRD_PARTY'));

-- =====================================================
-- 3. 项目管理模块检查约束
-- =====================================================

-- matter 表：项目类型检查
ALTER TABLE public.matter
    ADD CONSTRAINT IF NOT EXISTS chk_matter_type
    CHECK (matter_type IN ('LITIGATION', 'NON_LITIGATION', 'LEGAL_ADVICE', 'DRAFTING',
                          'CONSULTING', 'OTHER'));

-- matter 表：项目状态检查
ALTER TABLE public.matter
    ADD CONSTRAINT IF NOT EXISTS chk_matter_status
    CHECK (matter_status IN ('PENDING', 'IN_PROGRESS', 'SUSPENDED', 'COMPLETED', 'CANCELLED'));

-- matter 表：案件性质检查
ALTER TABLE public.matter
    ADD CONSTRAINT IF NOT EXISTS chk_case_nature
    CHECK (case_nature IN ('CIVIL', 'ADMINISTRATIVE', 'CRIMINAL', 'STATE_COMPENSATION',
                            'ENFORCEMENT', 'OTHER'));

-- matter 表：审理程序检查
ALTER TABLE public.matter
    ADD CONSTRAINT IF NOT EXISTS chk_trial_procedure
    CHECK (trial_procedure IN ('FIRST_INSTANCE', 'SECOND_INSTANCE', 'RETRIAL',
                               'SUMMARY', 'SUPERVISION', 'EXECUTION'));

-- matter 表：项目等级检查
ALTER TABLE public.matter
    ADD CONSTRAINT IF NOT EXISTS chk_matter_level
    CHECK (matter_level IN ('MAJOR', 'IMPORTANT', 'NORMAL', 'MINOR'));

-- matter 表：紧急程度检查
ALTER TABLE public.matter
    ADD CONSTRAINT IF NOT EXISTS chk_urgency
    CHECK (urgency IN ('URGENT', 'NORMAL', 'LOW'));

-- matter_participant 表：参与人角色检查
ALTER TABLE public.matter_participant
    ADD CONSTRAINT IF NOT EXISTS chk_participant_role
    CHECK (participant_role IN ('PLAINTIFF', 'DEFENDANT', 'THIRD_PARTY',
                                'PROSECUTOR', 'DEFENSE_COUNSEL', 'PLAINTIFF_COUNSEL',
                                'JUDGE', 'CLERK', 'OTHER'));

-- matter_deadline 表：期限类型检查
ALTER TABLE public.matter_deadline
    ADD CONSTRAINT IF NOT EXISTS chk_deadline_type
    CHECK (deadline_type IN ('COURT_HEARING', 'EVIDENCE_SUBMISSION', 'STATEMENT_SUBMISSION',
                             'APPEAL_DEADLINE', 'EXECUTION_DEADLINE', 'OTHER'));

-- =====================================================
-- 4. 财务管理模块检查约束
-- =====================================================

-- finance_contract 表：合同类型检查
ALTER TABLE public.finance_contract
    ADD CONSTRAINT IF NOT EXISTS chk_contract_type
    CHECK (contract_type IN ('FIXED_FEE', 'HOURLY', 'CONTINGENCY', 'HYBRID', 'RETAINER',
                             'LEGAL_AID', 'PRO_BONO', 'OTHER'));

-- finance_contract 表：合同状态检查
ALTER TABLE public.finance_contract
    ADD CONSTRAINT IF NOT EXISTS chk_contract_status
    CHECK (contract_status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SIGNED',
                                'EXECUTING', 'COMPLETED', 'TERMINATED', 'CANCELLED'));

-- finance_contract 表：金额检查
ALTER TABLE public.finance_contract
    ADD CONSTRAINT IF NOT EXISTS chk_contract_amount_positive
    CHECK (contract_amount IS NULL OR contract_amount >= 0);

-- finance_payment 表：收款状态检查
ALTER TABLE public.finance_payment
    ADD CONSTRAINT IF NOT EXISTS chk_payment_status
    CHECK (payment_status IN ('PENDING', 'PARTIAL', 'COMPLETED', 'CANCELLED'));

-- finance_payment 表：收款方式检查
ALTER TABLE public.finance_payment
    ADD CONSTRAINT IF NOT EXISTS chk_payment_method
    CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'CHECK', 'ALIPAY', 'WECHAT_PAY',
                               'POS', 'OTHER'));

-- finance_payment 表：金额检查
ALTER TABLE public.finance_payment
    ADD CONSTRAINT IF NOT EXISTS chk_payment_amount_positive
    CHECK (amount > 0);

-- finance_payment 表：已收金额检查
ALTER TABLE public.finance_payment
    ADD CONSTRAINT IF NOT EXISTS chk_received_amount_valid
    CHECK (received_amount >= 0 AND received_amount <= amount);

-- finance_commission 表：提成状态检查
ALTER TABLE public.finance_commission
    ADD CONSTRAINT IF NOT EXISTS chk_commission_status
    CHECK (status IN ('PENDING', 'APPROVED', 'PAID', 'CANCELLED'));

-- finance_commission 表：薪酬模式检查
ALTER TABLE public.finance_commission
    ADD CONSTRAINT IF NOT EXISTS chk_commission_compensation_type
    CHECK (compensation_type IN ('COMMISSION', 'SALARIED', 'HYBRID'));

-- finance_commission 表：分配比例范围检查
ALTER TABLE public.finance_commission
    ADD CONSTRAINT IF NOT EXISTS chk_distribution_ratio
    CHECK (distribution_ratio IS NULL OR (distribution_ratio >= 0 AND distribution_ratio <= 100));

-- finance_commission 表：提成比例范围检查
ALTER TABLE public.finance_commission
    ADD CONSTRAINT IF NOT EXISTS chk_commission_rate
    CHECK (commission_rate IS NULL OR (commission_rate >= 0 AND commission_rate <= 100));

-- finance_commission_detail 表：分配比例范围检查
ALTER TABLE public.finance_commission_detail
    ADD CONSTRAINT IF NOT EXISTS chk_allocation_rate_positive
    CHECK (allocation_rate > 0 AND allocation_rate <= 100);

-- finance_invoice 表：发票类型检查
ALTER TABLE public.finance_invoice
    ADD CONSTRAINT IF NOT EXISTS chk_invoice_type
    CHECK (invoice_type IN ('VAT_SPECIAL', 'VAT_GENERAL', 'COMMON', 'PROFORMA', 'OTHER'));

-- finance_invoice 表：发票状态检查
ALTER TABLE public.finance_invoice
    ADD CONSTRAINT IF NOT EXISTS chk_invoice_status
    CHECK (invoice_status IN ('PENDING', 'PROCESSING', 'ISSUED', 'VOID', 'CANCELLED'));

-- =====================================================
-- 5. 行政管理模块检查约束
-- =====================================================

-- admin_seal 表：印章状态检查
ALTER TABLE public.admin_seal
    ADD CONSTRAINT IF NOT EXISTS chk_seal_status
    CHECK (seal_status IN ('ACTIVE', 'INACTIVE', 'LOST', 'DAMAGED'));

-- admin_seal_application 表：申请状态检查
ALTER TABLE public.admin_seal_application
    ADD CONSTRAINT IF NOT EXISTS chk_seal_app_status
    CHECK (application_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));

-- admin_seal_application 表：印章类型检查
ALTER TABLE public.admin_seal_application
    ADD CONSTRAINT IF NOT EXISTS chk_seal_type
    CHECK (seal_type IN ('OFFICIAL', 'CONTRACT', 'LEGAL', 'FINANCE', 'ADMIN', 'OTHER'));

-- admin_meeting_room 表：会议室状态检查
ALTER TABLE public.admin_meeting_room
    ADD CONSTRAINT IF NOT EXISTS chk_meeting_room_status
    CHECK (room_status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'DISABLED'));

-- admin_meeting_room_reservation 表：预约状态检查
ALTER TABLE public.admin_meeting_room_reservation
    ADD CONSTRAINT IF NOT EXISTS chk_meeting_res_status
    CHECK (reservation_status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED'));

-- =====================================================
-- 6. 工时管理模块检查约束
-- =====================================================

-- timesheet 表：工作类型检查
ALTER TABLE public.timesheet
    ADD CONSTRAINT IF NOT EXISTS chk_work_type
    CHECK (work_type IN ('BILLABLE', 'NON_BILLABLE', 'ADMIN', 'PRO_BONO', 'OTHER'));

-- timesheet 表：工时范围检查
ALTER TABLE public.timesheet
    ADD CONSTRAINT IF NOT EXISTS chk_work_hours_positive
    CHECK (work_hours > 0 AND work_hours <= 24);

-- timesheet 表：年份范围检查
ALTER TABLE public.timesheet
    ADD CONSTRAINT IF NOT EXISTS chk_work_year_valid
    CHECK (work_year >= 2020 AND work_year <= 2100);

-- timesheet 表：月份范围检查
ALTER TABLE public.timesheet
    ADD CONSTRAINT IF NOT EXISTS chk_work_month_valid
    CHECK (work_month >= 1 AND work_month <= 12);

-- =====================================================
-- 7. 任务管理模块检查约束
-- =====================================================

-- task 表：任务状态检查
ALTER TABLE public.task
    ADD CONSTRAINT IF NOT EXISTS chk_task_status
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'BLOCKED'));

-- task 表：优先级检查
ALTER TABLE public.task
    ADD CONSTRAINT IF NOT EXISTS chk_task_priority
    CHECK (priority IN ('URGENT', 'HIGH', 'NORMAL', 'LOW'));

-- =====================================================
-- 8. 人力资源模块检查约束
-- =====================================================

-- hr_employee 表：员工状态检查
ALTER TABLE public.hr_employee
    ADD CONSTRAINT IF NOT EXISTS chk_employee_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'RESIGNED', 'TERMINATED'));

-- hr_employee 表：性别检查
ALTER TABLE public.hr_employee
    ADD CONSTRAINT IF NOT EXISTS chk_employee_gender
    CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));

-- hr_attendance 表：考勤状态检查
ALTER TABLE public.hr_attendance
    ADD CONSTRAINT IF NOT EXISTS chk_attendance_status
    CHECK (attendance_status IN ('PRESENT', 'ABSENT', 'LATE', 'EARLY_LEAVE', 'LEAVE', 'BUSINESS_TRIP'));

-- hr_payroll 表：薪酬状态检查
ALTER TABLE public.hr_payroll
    ADD CONSTRAINT IF NOT EXISTS chk_payroll_status
    CHECK (payroll_status IN ('DRAFT', 'PENDING', 'APPROVED', 'PAID', 'CANCELLED'));

-- hr_performance 表：考核等级检查
ALTER TABLE public.hr_performance
    ADD CONSTRAINT IF NOT EXISTS chk_performance_rating
    CHECK (rating IN ('S', 'A', 'B', 'C', 'D'));

-- =====================================================
-- 9. 档案管理模块检查约束
-- =====================================================

-- archive 表：档案状态检查
ALTER TABLE public.archive
    ADD CONSTRAINT IF NOT EXISTS chk_archive_status
    CHECK (archive_status IN ('PENDING', 'STORED', 'BORROWED', 'DESTROYED'));

-- archive 表：保管期限检查
ALTER TABLE public.archive
    ADD CONSTRAINT IF NOT EXISTS chk_retention_period
    CHECK (retention_period IN ('PERMANENT', 'LONG_TERM', 'SHORT_TERM', 'TEMPORARY'));

-- archive_borrow 表：借阅状态检查
ALTER TABLE public.archive_borrow
    ADD CONSTRAINT IF NOT EXISTS chk_borrow_status
    CHECK (borrow_status IN ('PENDING', 'APPROVED', 'BORROWED', 'RETURNED', 'OVERDUE'));

-- =====================================================
-- 10. 文档管理模块检查约束
-- =====================================================

-- doc_file 表：文档类型检查
ALTER TABLE public.doc_file
    ADD CONSTRAINT IF NOT EXISTS chk_doc_type
    CHECK (doc_type IN ('CONTRACT', 'EVIDENCE', 'PLEADING', 'CORRESPONDENCE',
                         'COURT_DOCUMENT', 'OPINION', 'RESEARCH', 'ADMIN', 'OTHER'));

-- doc_file 表：文档状态检查
ALTER TABLE public.doc_file
    ADD CONSTRAINT IF NOT EXISTS chk_doc_status
    CHECK (doc_status IN ('DRAFT', 'REVIEW', 'APPROVED', 'FINAL', 'ARCHIVED'));

-- doc_version 表：版本类型检查
ALTER TABLE public.doc_version
    ADD CONSTRAINT IF NOT EXISTS chk_version_type
    CHECK (version_type IN ('DRAFT', 'MINOR', 'MAJOR', 'FINAL'));

-- =====================================================
-- 11. 质量管理模块检查约束
-- =====================================================

-- quality_check 表：检查状态检查
ALTER TABLE public.quality_check
    ADD CONSTRAINT IF NOT EXISTS chk_quality_check_status
    CHECK (check_status IN ('PENDING', 'IN_PROGRESS', 'PASSED', 'FAILED', 'SKIPPED'));

-- quality_check 表：检查类型检查
ALTER TABLE public.quality_check
    ADD CONSTRAINT IF NOT EXISTS chk_quality_check_type
    CHECK (check_type IN ('FILE_REVIEW', 'MATTER_REVIEW', 'CONTRACT_REVIEW',
                          'COURT_DOCUMENT_REVIEW', 'OTHER'));

-- quality_risk 表：风险等级检查
ALTER TABLE public.quality_risk
    ADD CONSTRAINT IF NOT EXISTS chk_risk_level
    CHECK (risk_level IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW'));

-- quality_risk 表：风险状态检查
ALTER TABLE public.quality_risk
    ADD CONSTRAINT IF NOT EXISTS chk_risk_status
    CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'IGNORED'));

-- =====================================================
-- 12. 工作台模块检查约束
-- =====================================================

-- wb_approval 表：审批状态检查
ALTER TABLE public.wb_approval
    ADD CONSTRAINT IF NOT EXISTS chk_approval_status
    CHECK (approval_status IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'CANCELLED'));

-- wb_approval 表：审批类型检查
ALTER TABLE public.wb_approval
    ADD CONSTRAINT IF NOT EXISTS chk_approval_type
    CHECK (approval_type IN ('CONTRACT', 'SEAL', 'LEAVE', 'REIMBURSEMENT', 'PURCHASE',
                             'CONFLICT_CHECK', 'MATTER_CLOSE', 'OTHER'));

-- wb_schedule 表：日程类型检查
ALTER TABLE public.wb_schedule
    ADD CONSTRAINT IF NOT EXISTS chk_schedule_type
    CHECK (schedule_type IN ('COURT_HEARING', 'MEETING', 'CLIENT_VISIT', 'DEPOSITION',
                             'MEDITATION', 'ARBITRATION', 'TRAINING', 'OTHER'));

-- wb_schedule 表：提醒类型检查
ALTER TABLE public.wb_schedule
    ADD CONSTRAINT IF NOT EXISTS chk_reminder_type
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
