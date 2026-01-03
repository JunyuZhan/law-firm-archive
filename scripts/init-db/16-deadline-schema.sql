-- 期限提醒表
CREATE TABLE IF NOT EXISTS matter_deadline (
    id BIGSERIAL PRIMARY KEY,
    matter_id BIGINT NOT NULL,
    deadline_type VARCHAR(50) NOT NULL, -- 期限类型：EVIDENCE_SUBMISSION-举证期, APPEAL-上诉期, REPLY-答辩期, EXECUTION_APPLICATION-执行申请期, HEARING-开庭期, OTHER-其他
    deadline_name VARCHAR(200) NOT NULL, -- 期限名称
    base_date DATE NOT NULL, -- 基准日期（如立案日期、开庭日期、判决日期等）
    deadline_date DATE NOT NULL, -- 期限日期
    reminder_days INTEGER DEFAULT 7, -- 提前提醒天数（默认7天）
    reminder_sent BOOLEAN DEFAULT FALSE, -- 是否已发送提醒
    reminder_sent_at TIMESTAMP, -- 提醒发送时间
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 状态：ACTIVE-有效, COMPLETED-已完成, EXPIRED-已过期, CANCELLED-已取消
    completed_at TIMESTAMP, -- 完成时间
    completed_by BIGINT, -- 完成人ID
    description VARCHAR(500), -- 期限说明
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE matter_deadline IS '期限提醒表';
COMMENT ON COLUMN matter_deadline.matter_id IS '项目ID';
COMMENT ON COLUMN matter_deadline.deadline_type IS '期限类型';
COMMENT ON COLUMN matter_deadline.deadline_name IS '期限名称';
COMMENT ON COLUMN matter_deadline.base_date IS '基准日期';
COMMENT ON COLUMN matter_deadline.deadline_date IS '期限日期';
COMMENT ON COLUMN matter_deadline.reminder_days IS '提前提醒天数';
COMMENT ON COLUMN matter_deadline.reminder_sent IS '是否已发送提醒';
COMMENT ON COLUMN matter_deadline.status IS '状态';

CREATE INDEX idx_matter_deadline_matter_id ON matter_deadline(matter_id);
CREATE INDEX idx_matter_deadline_deadline_date ON matter_deadline(deadline_date);
CREATE INDEX idx_matter_deadline_status ON matter_deadline(status);
CREATE INDEX idx_matter_deadline_reminder_sent ON matter_deadline(reminder_sent);

