-- ============================================
-- 任务评论表结构更新（M3-057~M3-059）
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 更新任务评论表，添加附件和@提醒字段
ALTER TABLE task_comment
ADD COLUMN IF NOT EXISTS attachments JSONB,
ADD COLUMN IF NOT EXISTS mentioned_user_ids JSONB;

COMMENT ON COLUMN task_comment.attachments IS '附件列表（JSON格式，存储文件URL列表）';
COMMENT ON COLUMN task_comment.mentioned_user_ids IS '@提醒的用户ID列表（JSON格式）';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_task_comment_task_id ON task_comment(task_id);
CREATE INDEX IF NOT EXISTS idx_task_comment_created_at ON task_comment(created_at);

