--
-- 工资管理自动确认功能
-- 创建时间：2026-01-06
--

-- 添加工资表自动确认截止时间字段
ALTER TABLE public.hr_payroll_sheet 
ADD COLUMN IF NOT EXISTS auto_confirm_deadline TIMESTAMP;

COMMENT ON COLUMN public.hr_payroll_sheet.auto_confirm_deadline IS '自动确认截止时间（超过此时间未确认的工资明细将自动确认）';

-- 添加工资明细拒绝理由字段（已有confirm_comment字段，但需要明确用途）
COMMENT ON COLUMN public.hr_payroll_item.confirm_comment IS '确认意见（确认时可为空，拒绝时必须填写理由）';

