-- =============================================
-- 053-add-letter-approver-field.sql
-- 出函申请添加指定审批人字段
-- =============================================

-- 添加指定审批人字段
ALTER TABLE letter_application ADD COLUMN IF NOT EXISTS assigned_approver_id BIGINT;

-- 添加注释
COMMENT ON COLUMN letter_application.assigned_approver_id IS '指定审批人ID（申请时选择）';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_letter_application_assigned_approver ON letter_application(assigned_approver_id);

