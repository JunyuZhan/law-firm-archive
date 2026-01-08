--
-- 角色变更历史表
-- 创建时间：2026-01-06
--

CREATE TABLE IF NOT EXISTS public.sys_role_change_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    username VARCHAR(50) NOT NULL,
    old_role_ids TEXT,  -- JSON格式：旧角色ID列表
    old_role_codes TEXT,  -- JSON格式：旧角色代码列表
    new_role_ids TEXT,  -- JSON格式：新角色ID列表
    new_role_codes TEXT,  -- JSON格式：新角色代码列表
    change_type VARCHAR(20) NOT NULL,  -- UPGRADE, DOWNGRADE, TRANSFER
    change_reason TEXT,
    pending_business_count INT DEFAULT 0,
    changed_by BIGINT REFERENCES sys_user(id),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE public.sys_role_change_log IS '角色变更历史表';
COMMENT ON COLUMN public.sys_role_change_log.user_id IS '用户ID';
COMMENT ON COLUMN public.sys_role_change_log.username IS '用户名';
COMMENT ON COLUMN public.sys_role_change_log.old_role_ids IS '旧角色ID列表（JSON格式）';
COMMENT ON COLUMN public.sys_role_change_log.old_role_codes IS '旧角色代码列表（JSON格式）';
COMMENT ON COLUMN public.sys_role_change_log.new_role_ids IS '新角色ID列表（JSON格式）';
COMMENT ON COLUMN public.sys_role_change_log.new_role_codes IS '新角色代码列表（JSON格式）';
COMMENT ON COLUMN public.sys_role_change_log.change_type IS '变更类型：UPGRADE-权限扩大, DOWNGRADE-权限缩小, TRANSFER-跨部门/跨角色';
COMMENT ON COLUMN public.sys_role_change_log.change_reason IS '变更原因';
COMMENT ON COLUMN public.sys_role_change_log.pending_business_count IS '待处理业务数量';
COMMENT ON COLUMN public.sys_role_change_log.changed_by IS '变更人ID';
COMMENT ON COLUMN public.sys_role_change_log.changed_at IS '变更时间';

CREATE INDEX IF NOT EXISTS idx_role_change_user ON public.sys_role_change_log(user_id);
CREATE INDEX IF NOT EXISTS idx_role_change_time ON public.sys_role_change_log(changed_at);
CREATE INDEX IF NOT EXISTS idx_role_change_type ON public.sys_role_change_log(change_type);

