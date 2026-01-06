-- ============================================
-- 修复登录日志表结构（添加缺失的字段）
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 为 sys_login_log 表添加缺失的字段
ALTER TABLE sys_login_log
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by BIGINT,
ADD COLUMN IF NOT EXISTS updated_by BIGINT,
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN sys_login_log.updated_at IS '更新时间';
COMMENT ON COLUMN sys_login_log.created_by IS '创建人ID';
COMMENT ON COLUMN sys_login_log.updated_by IS '更新人ID';
COMMENT ON COLUMN sys_login_log.deleted IS '是否删除';

