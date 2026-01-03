-- ============================================
-- 在线计时器会话表（M3-044）
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 计时器会话表
CREATE TABLE IF NOT EXISTS timer_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    matter_id BIGINT NOT NULL,
    work_type VARCHAR(50),
    work_content TEXT,
    billable BOOLEAN DEFAULT TRUE,
    
    -- 计时信息
    start_time TIMESTAMP NOT NULL,
    pause_time TIMESTAMP,
    resume_time TIMESTAMP,
    elapsed_seconds BIGINT DEFAULT 0, -- 已累计的秒数
    status VARCHAR(20) DEFAULT 'RUNNING', -- RUNNING, PAUSED, STOPPED
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE timer_session IS '计时器会话表';
COMMENT ON COLUMN timer_session.user_id IS '用户ID';
COMMENT ON COLUMN timer_session.matter_id IS '案件ID';
COMMENT ON COLUMN timer_session.work_type IS '工作类型';
COMMENT ON COLUMN timer_session.work_content IS '工作内容';
COMMENT ON COLUMN timer_session.billable IS '是否计费';
COMMENT ON COLUMN timer_session.start_time IS '开始时间';
COMMENT ON COLUMN timer_session.pause_time IS '暂停时间';
COMMENT ON COLUMN timer_session.resume_time IS '恢复时间';
COMMENT ON COLUMN timer_session.elapsed_seconds IS '已累计的秒数';
COMMENT ON COLUMN timer_session.status IS '状态: RUNNING-运行中, PAUSED-已暂停, STOPPED-已停止';

-- 创建索引
CREATE INDEX idx_timer_session_user_id ON timer_session(user_id);
CREATE INDEX idx_timer_session_matter_id ON timer_session(matter_id);
CREATE INDEX idx_timer_session_status ON timer_session(status);

