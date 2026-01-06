-- ============================================
-- 案源管理模块 - 数据库表结构
-- ============================================

-- 案源线索表
CREATE TABLE IF NOT EXISTS crm_lead (
    id BIGSERIAL PRIMARY KEY,
    lead_no VARCHAR(50) NOT NULL UNIQUE,
    
    -- 案源基本信息
    lead_name VARCHAR(200) NOT NULL,
    lead_type VARCHAR(20),  -- INDIVIDUAL-个人, ENTERPRISE-企业
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    
    -- 案源来源
    source_channel VARCHAR(50),  -- 来源渠道：REFERRAL-转介绍, WEBSITE-网站, AD-广告等
    source_detail VARCHAR(500),  -- 来源详情
    
    -- 案源状态
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING-待跟进, FOLLOWING-跟进中, CONVERTED-已转化, ABANDONED-已放弃
    priority VARCHAR(20) DEFAULT 'NORMAL',  -- HIGH-高, NORMAL-中, LOW-低
    
    -- 业务信息
    business_type VARCHAR(50),  -- 业务类型：LITIGATION-诉讼, NON_LITIGATION-非诉
    estimated_amount DECIMAL(15, 2),  -- 预估金额
    description TEXT,  -- 案源描述
    
    -- 跟进信息
    last_follow_time TIMESTAMP,  -- 最后跟进时间
    next_follow_time TIMESTAMP,  -- 下次跟进时间
    follow_count INT DEFAULT 0,  -- 跟进次数
    
    -- 转化信息
    converted_at TIMESTAMP,  -- 转化时间
    converted_to_client_id BIGINT,  -- 转化后的客户ID
    converted_to_matter_id BIGINT,  -- 转化后的项目ID
    
    -- 分配信息
    originator_id BIGINT,  -- 案源人ID
    responsible_user_id BIGINT,  -- 负责跟进人ID
    
    -- 备注
    remark TEXT,
    
    -- 审计字段
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE crm_lead IS '案源线索表';
COMMENT ON COLUMN crm_lead.lead_no IS '案源编号';
COMMENT ON COLUMN crm_lead.lead_name IS '案源名称';
COMMENT ON COLUMN crm_lead.lead_type IS '案源类型：INDIVIDUAL-个人, ENTERPRISE-企业';
COMMENT ON COLUMN crm_lead.source_channel IS '来源渠道';
COMMENT ON COLUMN crm_lead.status IS '状态：PENDING-待跟进, FOLLOWING-跟进中, CONVERTED-已转化, ABANDONED-已放弃';
COMMENT ON COLUMN crm_lead.priority IS '优先级：HIGH-高, NORMAL-中, LOW-低';
COMMENT ON COLUMN crm_lead.converted_to_client_id IS '转化后的客户ID';
COMMENT ON COLUMN crm_lead.converted_to_matter_id IS '转化后的项目ID';

CREATE INDEX idx_crm_lead_status ON crm_lead(status);
CREATE INDEX idx_crm_lead_originator_id ON crm_lead(originator_id);
CREATE INDEX idx_crm_lead_responsible_user_id ON crm_lead(responsible_user_id);
CREATE INDEX idx_crm_lead_created_at ON crm_lead(created_at);

-- 案源跟进记录表
CREATE TABLE IF NOT EXISTS crm_lead_follow_up (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL,
    
    -- 跟进信息
    follow_type VARCHAR(20),  -- PHONE-电话, EMAIL-邮件, VISIT-拜访, MEETING-会议, OTHER-其他
    follow_content TEXT,  -- 跟进内容
    follow_result VARCHAR(20),  -- POSITIVE-积极, NEUTRAL-中性, NEGATIVE-消极
    
    -- 下次跟进
    next_follow_time TIMESTAMP,  -- 下次跟进时间
    next_follow_plan TEXT,  -- 下次跟进计划
    
    -- 跟进人
    follow_user_id BIGINT NOT NULL,  -- 跟进人ID
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT
);

COMMENT ON TABLE crm_lead_follow_up IS '案源跟进记录表';
COMMENT ON COLUMN crm_lead_follow_up.lead_id IS '案源ID';
COMMENT ON COLUMN crm_lead_follow_up.follow_type IS '跟进方式：PHONE-电话, EMAIL-邮件, VISIT-拜访, MEETING-会议, OTHER-其他';
COMMENT ON COLUMN crm_lead_follow_up.follow_result IS '跟进结果：POSITIVE-积极, NEUTRAL-中性, NEGATIVE-消极';

CREATE INDEX idx_crm_lead_follow_up_lead_id ON crm_lead_follow_up(lead_id);
CREATE INDEX idx_crm_lead_follow_up_created_at ON crm_lead_follow_up(created_at);

