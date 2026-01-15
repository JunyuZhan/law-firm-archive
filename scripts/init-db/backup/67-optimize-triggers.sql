-- =====================================================
-- 数据库优化脚本 - 自动更新触发器
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-15
-- 描述: 自动更新 updated_at 字段的触发器
-- 优先级: P2 (中优先级)
-- 说明: 确保所有表的 updated_at 字段在数据修改时自动更新
-- =====================================================

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

DROP TRIGGER IF EXISTS trg_finance_payment_amendment_updated_at ON public.fin_payment_amendment;
CREATE TRIGGER trg_finance_payment_amendment_updated_at
    BEFORE UPDATE ON public.fin_payment_amendment
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
DROP TRIGGER IF EXISTS trg_admin_seal_updated_at ON public.admin_seal;
CREATE TRIGGER trg_admin_seal_updated_at
    BEFORE UPDATE ON public.admin_seal
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_admin_seal_application_updated_at ON public.admin_seal_application;
CREATE TRIGGER trg_admin_seal_application_updated_at
    BEFORE UPDATE ON public.admin_seal_application
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_admin_meeting_room_updated_at ON public.admin_meeting_room;
CREATE TRIGGER trg_admin_meeting_room_updated_at
    BEFORE UPDATE ON public.admin_meeting_room
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_admin_meeting_room_reservation_updated_at ON public.admin_meeting_room_reservation;
CREATE TRIGGER trg_admin_meeting_room_reservation_updated_at
    BEFORE UPDATE ON public.admin_meeting_room_reservation
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

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
