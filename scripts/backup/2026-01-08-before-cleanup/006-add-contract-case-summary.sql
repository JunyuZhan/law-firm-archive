-- 添加案情摘要字段到合同表
-- 用于审批表中的案情摘要，与合同内容区分

ALTER TABLE finance_contract ADD COLUMN IF NOT EXISTS case_summary TEXT;

COMMENT ON COLUMN finance_contract.case_summary IS '案情摘要（用于审批表）';

