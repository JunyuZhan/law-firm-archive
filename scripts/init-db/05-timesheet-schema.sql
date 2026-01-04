-- ============================================
-- 智慧律所管理系统 - 工时管理模块数据库脚本
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 工时记录表
CREATE TABLE IF NOT EXISTS timesheet (
    id BIGSERIAL PRIMARY KEY,
    timesheet_no VARCHAR(50) NOT NULL UNIQUE,
    
    -- 关联信息
    matter_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    -- 工时信息
    work_date DATE NOT NULL,
    hours DECIMAL(5, 2) NOT NULL,
    
    -- 工作内容
    work_type VARCHAR(50),
    work_content TEXT NOT NULL,
    
    -- 计费信息
    billable BOOLEAN DEFAULT TRUE,
    hourly_rate DECIMAL(10, 2),
    amount DECIMAL(15, 2),
    
    -- 审批状态
    status VARCHAR(20) DEFAULT 'DRAFT',
    
    -- 审批信息
    submitted_at TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    approval_comment TEXT,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE timesheet IS '工时记录表';
COMMENT ON COLUMN timesheet.work_type IS '工作类型: RESEARCH-法律研究, DRAFTING-文书起草, MEETING-会议, COURT-出庭, NEGOTIATION-谈判, COMMUNICATION-沟通, TRAVEL-差旅, OTHER-其他';
COMMENT ON COLUMN timesheet.status IS '状态: DRAFT-草稿, SUBMITTED-已提交, APPROVED-已批准, REJECTED-已拒绝';

-- 工时汇总表（按月统计）
CREATE TABLE IF NOT EXISTS timesheet_summary (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    matter_id BIGINT,
    
    -- 统计周期
    year INT NOT NULL,
    month INT NOT NULL,
    
    -- 汇总数据
    total_hours DECIMAL(10, 2) DEFAULT 0,
    billable_hours DECIMAL(10, 2) DEFAULT 0,
    non_billable_hours DECIMAL(10, 2) DEFAULT 0,
    total_amount DECIMAL(15, 2) DEFAULT 0,
    
    -- 审批状态统计
    draft_count INT DEFAULT 0,
    submitted_count INT DEFAULT 0,
    approved_count INT DEFAULT 0,
    rejected_count INT DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, matter_id, year, month)
);

COMMENT ON TABLE timesheet_summary IS '工时汇总表';

-- 小时费率表
CREATE TABLE IF NOT EXISTS hourly_rate (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- 费率信息
    rate DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'CNY',
    
    -- 生效时间
    effective_date DATE NOT NULL,
    expiry_date DATE,
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE hourly_rate IS '小时费率表';

-- 索引
CREATE INDEX IF NOT EXISTS idx_timesheet_matter ON timesheet(matter_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_user ON timesheet(user_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_date ON timesheet(work_date);
CREATE INDEX IF NOT EXISTS idx_timesheet_status ON timesheet(status);
CREATE INDEX IF NOT EXISTS idx_summary_user ON timesheet_summary(user_id);
CREATE INDEX IF NOT EXISTS idx_summary_period ON timesheet_summary(year, month);
CREATE INDEX IF NOT EXISTS idx_hourly_rate_user ON hourly_rate(user_id);
