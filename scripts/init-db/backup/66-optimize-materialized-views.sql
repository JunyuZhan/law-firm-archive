-- =====================================================
-- 数据库优化脚本 - 汇总物化视图
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-15
-- 描述: 为常用统计查询创建物化视图
-- 优先级: P2 (中优先级)
-- 说明: 物化视图可以显著提高复杂查询的性能
-- =====================================================

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
    TO_CHAR(sa.approved_at, 'YYYY-MM') AS month,
    sa.seal_type,
    COUNT(*) AS usage_count,
    COUNT(*) FILTER (WHERE sa.application_status = 'APPROVED') AS approved_count,
    COUNT(DISTINCT sa.applicant_id) AS applicant_count
FROM public.admin_seal_application sa
WHERE sa.deleted = false
  AND sa.application_status = 'APPROVED'
  AND sa.approved_at >= DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '1 year'
GROUP BY TO_CHAR(sa.approved_at, 'YYYY-MM'), sa.seal_type
ORDER BY month DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_seal_usage_key ON public.mv_seal_usage_monthly_stats (month, seal_type);
COMMENT ON MATERIALIZED VIEW public.mv_seal_usage_monthly_stats IS '按月统计印章使用';

-- 会议室使用统计（按月）
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_meeting_room_usage_stats AS
SELECT
    TO_CHAR(r.start_time, 'YYYY-MM') AS month,
    r.room_id,
    mr.room_name,
    COUNT(*) AS reservation_count,
    SUM(EXTRACT(EPOCH FROM (r.end_time - r.start_time))/3600) AS total_hours,
    COUNT(*) FILTER (WHERE r.reservation_status = 'COMPLETED') AS completed_count,
    COUNT(*) FILTER (WHERE r.reservation_status = 'CANCELLED') AS cancelled_count
FROM public.admin_meeting_room_reservation r
JOIN public.admin_meeting_room mr ON mr.id = r.room_id
WHERE r.deleted = false
  AND r.start_time >= DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '1 year'
GROUP BY TO_CHAR(r.start_time, 'YYYY-MM'), r.room_id, mr.room_name
ORDER BY month DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_meeting_usage_key ON public.mv_meeting_room_usage_stats (month, room_id);
COMMENT ON MATERIALIZED VIEW public.mv_meeting_room_usage_stats IS '会议室使用统计';

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
