-- 客户关联企业表
CREATE TABLE IF NOT EXISTS crm_client_related_company (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    related_company_name VARCHAR(200) NOT NULL,
    related_company_type VARCHAR(20) NOT NULL, -- PARENT-母公司, SUBSIDIARY-子公司, AFFILIATE-关联公司
    credit_code VARCHAR(50), -- 统一社会信用代码
    registered_address VARCHAR(500), -- 注册地址
    legal_representative VARCHAR(100), -- 法定代表人
    relationship_description VARCHAR(500), -- 关联关系描述
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE crm_client_related_company IS '客户关联企业表';
COMMENT ON COLUMN crm_client_related_company.client_id IS '客户ID';
COMMENT ON COLUMN crm_client_related_company.related_company_name IS '关联企业名称';
COMMENT ON COLUMN crm_client_related_company.related_company_type IS '关联类型：PARENT-母公司, SUBSIDIARY-子公司, AFFILIATE-关联公司';
COMMENT ON COLUMN crm_client_related_company.credit_code IS '统一社会信用代码';
COMMENT ON COLUMN crm_client_related_company.registered_address IS '注册地址';
COMMENT ON COLUMN crm_client_related_company.legal_representative IS '法定代表人';
COMMENT ON COLUMN crm_client_related_company.relationship_description IS '关联关系描述';

CREATE INDEX idx_crm_client_related_company_client_id ON crm_client_related_company(client_id);
CREATE INDEX idx_crm_client_related_company_name ON crm_client_related_company(related_company_name);
CREATE INDEX idx_crm_client_related_company_type ON crm_client_related_company(related_company_type);

