-- 用户会话表
CREATE TABLE IF NOT EXISTS sys_user_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    token VARCHAR(2000) NOT NULL, -- JWT Token
    refresh_token VARCHAR(2000), -- 刷新Token
    ip_address VARCHAR(50), -- 登录IP
    user_agent VARCHAR(2000), -- 用户代理
    device_type VARCHAR(50), -- 设备类型：PC, MOBILE, TABLET
    browser VARCHAR(100), -- 浏览器
    os VARCHAR(100), -- 操作系统
    location VARCHAR(200), -- 登录地点
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 登录时间
    last_access_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 最后访问时间
    expire_time TIMESTAMP NOT NULL, -- 过期时间
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 状态：ACTIVE-活跃, EXPIRED-已过期, FORCED_LOGOUT-强制下线, LOGGED_OUT-已登出
    is_current BOOLEAN DEFAULT TRUE, -- 是否为当前会话
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_user_session IS '用户会话表';
COMMENT ON COLUMN sys_user_session.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_session.token IS 'JWT访问令牌';
COMMENT ON COLUMN sys_user_session.refresh_token IS '刷新令牌';
COMMENT ON COLUMN sys_user_session.ip_address IS '登录IP地址';
COMMENT ON COLUMN sys_user_session.user_agent IS '用户代理';
COMMENT ON COLUMN sys_user_session.device_type IS '设备类型';
COMMENT ON COLUMN sys_user_session.status IS '会话状态';
COMMENT ON COLUMN sys_user_session.is_current IS '是否为当前会话';
COMMENT ON COLUMN sys_user_session.expire_time IS '过期时间';

CREATE INDEX idx_sys_user_session_user_id ON sys_user_session(user_id);
CREATE INDEX idx_sys_user_session_token ON sys_user_session(token);
CREATE INDEX idx_sys_user_session_status ON sys_user_session(status);
CREATE INDEX idx_sys_user_session_expire_time ON sys_user_session(expire_time);

