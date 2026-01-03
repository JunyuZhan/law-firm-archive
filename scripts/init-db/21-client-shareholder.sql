-- 客户股东信息表
CREATE TABLE IF NOT EXISTS crm_client_shareholder (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    shareholder_name VARCHAR(100) NOT NULL,
    shareholder_type VARCHAR(20), -- INDIVIDUAL-个人, ENTERPRISE-企业
    id_card VARCHAR(50), -- 个人股东身份证号
    credit_code VARCHAR(50), -- 企业股东统一社会信用代码
    shareholding_ratio DECIMAL(5,2), -- 持股比例（百分比）
    investment_amount DECIMAL(15,2), -- 投资金额
    investment_date DATE, -- 投资日期
    position VARCHAR(50), -- 职务（如：董事长、总经理等）
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE crm_client_shareholder IS '客户股东信息表';
COMMENT ON COLUMN crm_client_shareholder.client_id IS '客户ID';
COMMENT ON COLUMN crm_client_shareholder.shareholder_name IS '股东名称';
COMMENT ON COLUMN crm_client_shareholder.shareholder_type IS '股东类型：INDIVIDUAL-个人, ENTERPRISE-企业';
COMMENT ON COLUMN crm_client_shareholder.id_card IS '个人股东身份证号';
COMMENT ON COLUMN crm_client_shareholder.credit_code IS '企业股东统一社会信用代码';
COMMENT ON COLUMN crm_client_shareholder.shareholding_ratio IS '持股比例（百分比）';
COMMENT ON COLUMN crm_client_shareholder.investment_amount IS '投资金额';
COMMENT ON COLUMN crm_client_shareholder.investment_date IS '投资日期';
COMMENT ON COLUMN crm_client_shareholder.position IS '职务';

CREATE INDEX idx_crm_client_shareholder_client_id ON crm_client_shareholder(client_id);
CREATE INDEX idx_crm_client_shareholder_name ON crm_client_shareholder(shareholder_name);

