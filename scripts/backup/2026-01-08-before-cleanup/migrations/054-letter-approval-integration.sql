-- 054-letter-approval-integration.sql
-- 出函审批接入审批中心

-- 1. 在 letter_application 表中添加 approval_id 字段
-- 用于关联审批中心的审批记录
ALTER TABLE letter_application ADD COLUMN IF NOT EXISTS approval_id BIGINT;

COMMENT ON COLUMN letter_application.approval_id IS '审批中心审批记录ID（关联 workbench_approval 表）';

-- 2. 创建索引，用于快速查询
CREATE INDEX IF NOT EXISTS idx_letter_application_approval_id ON letter_application (approval_id);

-- 3. 确认修改成功
DO $$
BEGIN
    RAISE NOTICE '出函审批接入审批中心迁移完成';
    RAISE NOTICE '- 添加 letter_application.approval_id 字段';
    RAISE NOTICE '- 创建 idx_letter_application_approval_id 索引';
END $$;


