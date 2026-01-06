-- ============================================
-- 提成管理模块数据库表结构
-- 创建时间: 2026-01-03
-- ============================================

-- 提成规则表
CREATE TABLE IF NOT EXISTS finance_commission_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50),
    firm_retention_rate DECIMAL(5,4) NOT NULL,
    originator_rate DECIMAL(5,4),
    tax_rate DECIMAL(5,4),
    management_fee_rate DECIMAL(5,4),
    rate_tiers JSONB,
    effective_date DATE,
    expiry_date DATE,
    is_default BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE finance_commission_rule IS '提成规则表';
COMMENT ON COLUMN finance_commission_rule.rule_code IS '规则编码';
COMMENT ON COLUMN finance_commission_rule.rule_name IS '规则名称';
COMMENT ON COLUMN finance_commission_rule.rule_type IS '规则类型';
COMMENT ON COLUMN finance_commission_rule.firm_retention_rate IS '律所留存比例';
COMMENT ON COLUMN finance_commission_rule.originator_rate IS '案源提成比例';
COMMENT ON COLUMN finance_commission_rule.tax_rate IS '税费率';
COMMENT ON COLUMN finance_commission_rule.management_fee_rate IS '管理费率';
COMMENT ON COLUMN finance_commission_rule.rate_tiers IS '阶梯费率（JSON格式）';
COMMENT ON COLUMN finance_commission_rule.effective_date IS '生效日期';
COMMENT ON COLUMN finance_commission_rule.expiry_date IS '失效日期';
COMMENT ON COLUMN finance_commission_rule.is_default IS '是否默认规则';
COMMENT ON COLUMN finance_commission_rule.active IS '是否启用';

-- 提成记录表
CREATE TABLE IF NOT EXISTS finance_commission (
    id BIGSERIAL PRIMARY KEY,
    commission_no VARCHAR(50) NOT NULL UNIQUE,
    payment_id BIGINT NOT NULL,
    fee_id BIGINT,
    contract_id BIGINT,
    matter_id BIGINT,
    client_id BIGINT,
    rule_id BIGINT,
    rule_code VARCHAR(50),
    -- 金额信息（原有字段）
    payment_amount DECIMAL(15,2),
    firm_retention DECIMAL(15,2),
    commission_base DECIMAL(15,2),
    tax_amount DECIMAL(15,2),
    management_fee DECIMAL(15,2),
    net_commission DECIMAL(15,2),
    -- 金额信息（新增字段，用于三层分配模型）
    gross_amount DECIMAL(15,2),
    cost_amount DECIMAL(15,2),
    net_amount DECIMAL(15,2),
    distribution_ratio DECIMAL(5,4),
    commission_rate DECIMAL(5,4),
    commission_amount DECIMAL(15,2),
    compensation_type VARCHAR(20),
    -- 分配信息
    originator_id BIGINT,
    originator_commission DECIMAL(15,2),
    -- 状态
    status VARCHAR(50) DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    paid_by BIGINT,
    paid_at TIMESTAMP,
    -- 备注
    remark TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE finance_commission IS '提成记录表';
COMMENT ON COLUMN finance_commission.commission_no IS '提成编号';
COMMENT ON COLUMN finance_commission.payment_id IS '收款记录ID';
COMMENT ON COLUMN finance_commission.fee_id IS '收费记录ID';
COMMENT ON COLUMN finance_commission.contract_id IS '合同ID';
COMMENT ON COLUMN finance_commission.matter_id IS '案件ID';
COMMENT ON COLUMN finance_commission.client_id IS '客户ID';
COMMENT ON COLUMN finance_commission.rule_id IS '提成规则ID';
COMMENT ON COLUMN finance_commission.rule_code IS '提成规则编码';
COMMENT ON COLUMN finance_commission.payment_amount IS '收款金额';
COMMENT ON COLUMN finance_commission.firm_retention IS '律所留存';
COMMENT ON COLUMN finance_commission.commission_base IS '提成基数';
COMMENT ON COLUMN finance_commission.tax_amount IS '税费';
COMMENT ON COLUMN finance_commission.management_fee IS '管理费';
COMMENT ON COLUMN finance_commission.net_commission IS '净提成';
COMMENT ON COLUMN finance_commission.originator_id IS '案源人ID';
COMMENT ON COLUMN finance_commission.originator_commission IS '案源提成';
COMMENT ON COLUMN finance_commission.status IS '状态：PENDING-待审批, APPROVED-已审批, PAID-已发放, CANCELLED-已取消';

-- 提成分配明细表
CREATE TABLE IF NOT EXISTS finance_commission_detail (
    id BIGSERIAL PRIMARY KEY,
    commission_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(100),
    role_in_matter VARCHAR(50),
    allocation_rate DECIMAL(5,4) NOT NULL,
    commission_amount DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2),
    net_amount DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_commission_detail_commission FOREIGN KEY (commission_id) REFERENCES finance_commission(id),
    CONSTRAINT fk_commission_detail_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

COMMENT ON TABLE finance_commission_detail IS '提成分配明细表';
COMMENT ON COLUMN finance_commission_detail.commission_id IS '提成记录ID';
COMMENT ON COLUMN finance_commission_detail.user_id IS '用户ID';
COMMENT ON COLUMN finance_commission_detail.user_name IS '用户姓名';
COMMENT ON COLUMN finance_commission_detail.role_in_matter IS '案件角色';
COMMENT ON COLUMN finance_commission_detail.allocation_rate IS '分配比例';
COMMENT ON COLUMN finance_commission_detail.commission_amount IS '提成金额';
COMMENT ON COLUMN finance_commission_detail.tax_amount IS '税费';
COMMENT ON COLUMN finance_commission_detail.net_amount IS '净提成';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_commission_payment ON finance_commission(payment_id);
CREATE INDEX IF NOT EXISTS idx_commission_fee ON finance_commission(fee_id);
CREATE INDEX IF NOT EXISTS idx_commission_contract ON finance_commission(contract_id);
CREATE INDEX IF NOT EXISTS idx_commission_matter ON finance_commission(matter_id);
CREATE INDEX IF NOT EXISTS idx_commission_client ON finance_commission(client_id);
CREATE INDEX IF NOT EXISTS idx_commission_rule ON finance_commission(rule_id);
CREATE INDEX IF NOT EXISTS idx_commission_status ON finance_commission(status);
CREATE INDEX IF NOT EXISTS idx_commission_originator ON finance_commission(originator_id);
CREATE INDEX IF NOT EXISTS idx_commission_detail_commission ON finance_commission_detail(commission_id);
CREATE INDEX IF NOT EXISTS idx_commission_detail_user ON finance_commission_detail(user_id);
CREATE INDEX IF NOT EXISTS idx_commission_rule_code ON finance_commission_rule(rule_code);
CREATE INDEX IF NOT EXISTS idx_commission_rule_default ON finance_commission_rule(is_default);

