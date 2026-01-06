-- 客户联系记录表
CREATE TABLE IF NOT EXISTS crm_client_contact_record (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    contact_id BIGINT, -- 联系人ID（可选，如果指定了具体联系人）
    contact_person VARCHAR(100), -- 联系人姓名（如果未指定contact_id）
    contact_method VARCHAR(20) NOT NULL, -- PHONE-电话, EMAIL-邮件, MEETING-会面, VISIT-拜访, OTHER-其他
    contact_date TIMESTAMP NOT NULL, -- 联系时间
    contact_duration INT, -- 联系时长（分钟）
    contact_location VARCHAR(200), -- 联系地点
    contact_content TEXT, -- 联系内容
    contact_result VARCHAR(500), -- 联系结果
    next_follow_up_date DATE, -- 下次跟进日期
    follow_up_reminder BOOLEAN DEFAULT FALSE, -- 是否设置提醒
    created_by BIGINT NOT NULL, -- 记录人
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE crm_client_contact_record IS '客户联系记录表';
COMMENT ON COLUMN crm_client_contact_record.client_id IS '客户ID';
COMMENT ON COLUMN crm_client_contact_record.contact_id IS '联系人ID';
COMMENT ON COLUMN crm_client_contact_record.contact_person IS '联系人姓名';
COMMENT ON COLUMN crm_client_contact_record.contact_method IS '联系方式：PHONE-电话, EMAIL-邮件, MEETING-会面, VISIT-拜访, OTHER-其他';
COMMENT ON COLUMN crm_client_contact_record.contact_date IS '联系时间';
COMMENT ON COLUMN crm_client_contact_record.contact_duration IS '联系时长（分钟）';
COMMENT ON COLUMN crm_client_contact_record.contact_location IS '联系地点';
COMMENT ON COLUMN crm_client_contact_record.contact_content IS '联系内容';
COMMENT ON COLUMN crm_client_contact_record.contact_result IS '联系结果';
COMMENT ON COLUMN crm_client_contact_record.next_follow_up_date IS '下次跟进日期';
COMMENT ON COLUMN crm_client_contact_record.follow_up_reminder IS '是否设置提醒';
COMMENT ON COLUMN crm_client_contact_record.created_by IS '记录人';

CREATE INDEX idx_crm_client_contact_record_client_id ON crm_client_contact_record(client_id);
CREATE INDEX idx_crm_client_contact_record_contact_id ON crm_client_contact_record(contact_id);
CREATE INDEX idx_crm_client_contact_record_contact_date ON crm_client_contact_record(contact_date);
CREATE INDEX idx_crm_client_contact_record_next_follow_up_date ON crm_client_contact_record(next_follow_up_date);

