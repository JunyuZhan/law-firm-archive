-- =====================================================
-- 企业变更历史记录表结构（M2-014）
-- 模块：M2 客户管理
-- =====================================================

-- -----------------------------------------------------
-- 企业变更历史表 (crm_client_change_history)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_client_change_history (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,                          -- 客户ID
    change_type VARCHAR(50) NOT NULL,                  -- 变更类型：NAME-名称, REGISTERED_CAPITAL-注册资本, LEGAL_REPRESENTATIVE-法定代表人, ADDRESS-地址, BUSINESS_SCOPE-经营范围, SHAREHOLDER-股东, OTHER-其他
    change_date DATE NOT NULL,                         -- 变更日期
    before_value TEXT,                                  -- 变更前值
    after_value TEXT,                                  -- 变更后值
    change_description TEXT,                            -- 变更描述
    registration_authority VARCHAR(200),               -- 登记机关
    registration_number VARCHAR(100),                  -- 登记编号
    attachment_url VARCHAR(500),                        -- 附件URL（变更通知书等）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_client_change_client ON crm_client_change_history(client_id);
CREATE INDEX idx_client_change_type ON crm_client_change_history(change_type);
CREATE INDEX idx_client_change_date ON crm_client_change_history(change_date);

COMMENT ON TABLE crm_client_change_history IS '企业变更历史记录表';
COMMENT ON COLUMN crm_client_change_history.change_type IS '变更类型：NAME-名称, REGISTERED_CAPITAL-注册资本, LEGAL_REPRESENTATIVE-法定代表人, ADDRESS-地址, BUSINESS_SCOPE-经营范围, SHAREHOLDER-股东, OTHER-其他';

