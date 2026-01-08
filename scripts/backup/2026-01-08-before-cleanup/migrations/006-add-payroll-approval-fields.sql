--
-- 工资表审批功能
-- 创建时间：2026-01-06
--

-- 添加审批相关字段到工资表
ALTER TABLE public.hr_payroll_sheet 
ADD COLUMN IF NOT EXISTS approver_id BIGINT,
ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS approved_by BIGINT,
ADD COLUMN IF NOT EXISTS approval_comment TEXT;

COMMENT ON COLUMN public.hr_payroll_sheet.approver_id IS '审批人ID（主任或合伙人）';
COMMENT ON COLUMN public.hr_payroll_sheet.approved_at IS '审批时间';
COMMENT ON COLUMN public.hr_payroll_sheet.approved_by IS '审批人ID';
COMMENT ON COLUMN public.hr_payroll_sheet.approval_comment IS '审批意见';

-- 更新状态枚举注释
COMMENT ON COLUMN public.hr_payroll_sheet.status IS '状态：DRAFT-草稿, PENDING_CONFIRM-待确认, CONFIRMED-已确认, PENDING_APPROVAL-待审批, APPROVED-已审批, REJECTED-已拒绝, ISSUED-已发放';

