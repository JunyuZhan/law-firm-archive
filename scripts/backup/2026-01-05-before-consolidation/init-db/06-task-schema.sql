-- ============================================
-- 智慧律所管理系统 - 任务管理模块数据库脚本
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 任务表
CREATE TABLE IF NOT EXISTS task (
    id BIGSERIAL PRIMARY KEY,
    task_no VARCHAR(50) NOT NULL UNIQUE,
    
    -- 关联信息
    matter_id BIGINT NOT NULL,
    parent_id BIGINT,
    
    -- 任务信息
    title VARCHAR(500) NOT NULL,
    description TEXT,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    
    -- 执行人
    assignee_id BIGINT,
    assignee_name VARCHAR(50),
    
    -- 时间
    start_date DATE,
    due_date DATE,
    completed_at TIMESTAMP,
    
    -- 状态
    status VARCHAR(20) DEFAULT 'TODO',
    progress INT DEFAULT 0,
    
    -- 提醒
    reminder_date TIMESTAMP,
    reminder_sent BOOLEAN DEFAULT FALSE,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE task IS '任务表';
COMMENT ON COLUMN task.priority IS '优先级: HIGH-高, MEDIUM-中, LOW-低';
COMMENT ON COLUMN task.status IS '状态: TODO-待办, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消';

-- 任务评论表
CREATE TABLE IF NOT EXISTS task_comment (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    
    content TEXT NOT NULL,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE task_comment IS '任务评论表';

-- 日程表
CREATE TABLE IF NOT EXISTS schedule (
    id BIGSERIAL PRIMARY KEY,
    
    -- 关联信息
    matter_id BIGINT,
    user_id BIGINT NOT NULL,
    
    -- 日程信息
    title VARCHAR(500) NOT NULL,
    description TEXT,
    location VARCHAR(500),
    
    -- 日程类型
    schedule_type VARCHAR(50) NOT NULL,
    
    -- 时间
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    all_day BOOLEAN DEFAULT FALSE,
    
    -- 提醒
    reminder_minutes INT,
    reminder_sent BOOLEAN DEFAULT FALSE,
    
    -- 重复
    recurrence_rule VARCHAR(200),
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE schedule IS '日程表';
COMMENT ON COLUMN schedule.schedule_type IS '日程类型: COURT-开庭, MEETING-会议, DEADLINE-期限, APPOINTMENT-约见, OTHER-其他';

-- 索引
CREATE INDEX IF NOT EXISTS idx_task_matter ON task(matter_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee ON task(assignee_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON task(status);
CREATE INDEX IF NOT EXISTS idx_task_due_date ON task(due_date);
CREATE INDEX IF NOT EXISTS idx_task_comment_task ON task_comment(task_id);
CREATE INDEX IF NOT EXISTS idx_schedule_user ON schedule(user_id);
CREATE INDEX IF NOT EXISTS idx_schedule_matter ON schedule(matter_id);
CREATE INDEX IF NOT EXISTS idx_schedule_time ON schedule(start_time, end_time);
