-- 客户标签表
CREATE TABLE IF NOT EXISTS crm_client_tag (
    id BIGSERIAL PRIMARY KEY,
    tag_name VARCHAR(50) NOT NULL UNIQUE,
    tag_color VARCHAR(20) DEFAULT '#1890ff',
    description VARCHAR(200),
    sort_order INT DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE crm_client_tag IS '客户标签表';
COMMENT ON COLUMN crm_client_tag.tag_name IS '标签名称';
COMMENT ON COLUMN crm_client_tag.tag_color IS '标签颜色（十六进制）';
COMMENT ON COLUMN crm_client_tag.description IS '标签描述';
COMMENT ON COLUMN crm_client_tag.sort_order IS '排序顺序';

CREATE INDEX idx_crm_client_tag_name ON crm_client_tag(tag_name);
CREATE INDEX idx_crm_client_tag_deleted ON crm_client_tag(deleted);

-- 客户标签关联表
CREATE TABLE IF NOT EXISTS crm_client_tag_relation (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(client_id, tag_id)
);

COMMENT ON TABLE crm_client_tag_relation IS '客户标签关联表';
COMMENT ON COLUMN crm_client_tag_relation.client_id IS '客户ID';
COMMENT ON COLUMN crm_client_tag_relation.tag_id IS '标签ID';

CREATE INDEX idx_crm_client_tag_relation_client_id ON crm_client_tag_relation(client_id);
CREATE INDEX idx_crm_client_tag_relation_tag_id ON crm_client_tag_relation(tag_id);

