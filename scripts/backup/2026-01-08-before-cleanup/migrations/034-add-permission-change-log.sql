--
-- 权限变更历史表
-- 创建时间：2026-01-06
--

CREATE TABLE IF NOT EXISTS public.sys_permission_change_log (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    role_code VARCHAR(50) NOT NULL,
    change_type VARCHAR(20) NOT NULL,  -- ADD, REMOVE
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100),
    change_reason TEXT,
    changed_by BIGINT REFERENCES sys_user(id),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE public.sys_permission_change_log IS '权限变更历史表';
COMMENT ON COLUMN public.sys_permission_change_log.role_id IS '角色ID';
COMMENT ON COLUMN public.sys_permission_change_log.role_code IS '角色代码';
COMMENT ON COLUMN public.sys_permission_change_log.change_type IS '变更类型：ADD-新增权限, REMOVE-移除权限';
COMMENT ON COLUMN public.sys_permission_change_log.permission_code IS '权限代码';
COMMENT ON COLUMN public.sys_permission_change_log.permission_name IS '权限名称';
COMMENT ON COLUMN public.sys_permission_change_log.change_reason IS '变更原因';
COMMENT ON COLUMN public.sys_permission_change_log.changed_by IS '变更人ID';
COMMENT ON COLUMN public.sys_permission_change_log.changed_at IS '变更时间';

CREATE INDEX IF NOT EXISTS idx_permission_log_role ON public.sys_permission_change_log(role_id);
CREATE INDEX IF NOT EXISTS idx_permission_log_time ON public.sys_permission_change_log(changed_at);
CREATE INDEX IF NOT EXISTS idx_permission_log_type ON public.sys_permission_change_log(change_type);
CREATE INDEX IF NOT EXISTS idx_permission_log_code ON public.sys_permission_change_log(permission_code);

