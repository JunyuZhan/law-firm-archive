--
-- 添加工资明细确认截止时间字段
-- 创建时间：2026-01-06
--

-- 添加工资明细确认截止时间字段
ALTER TABLE public.hr_payroll_item 
ADD COLUMN IF NOT EXISTS confirm_deadline TIMESTAMP;

COMMENT ON COLUMN public.hr_payroll_item.confirm_deadline IS '确认截止时间（超过此时间未确认将自动确认，如果为空则使用工资表的autoConfirmDeadline）';

