-- 添加任务验收相关字段
ALTER TABLE task ADD COLUMN IF NOT EXISTS review_status VARCHAR(20);
ALTER TABLE task ADD COLUMN IF NOT EXISTS review_comment TEXT;
ALTER TABLE task ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE task ADD COLUMN IF NOT EXISTS reviewed_by BIGINT;

-- 添加字段注释
COMMENT ON COLUMN task.review_status IS '验收状态: PENDING_REVIEW-待验收, APPROVED-已通过, REJECTED-已退回';
COMMENT ON COLUMN task.review_comment IS '验收意见（退回时填写）';
COMMENT ON COLUMN task.reviewed_at IS '验收时间';
COMMENT ON COLUMN task.reviewed_by IS '验收人ID';

