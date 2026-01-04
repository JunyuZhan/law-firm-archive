-- 预收款表
CREATE TABLE IF NOT EXISTS finance_prepayment (
    id BIGSERIAL PRIMARY KEY,
    prepayment_no VARCHAR(32) NOT NULL UNIQUE,
    client_id BIGINT NOT NULL,
    contract_id BIGINT,
    matter_id BIGINT,
    amount DECIMAL(18,2) NOT NULL,
    used_amount DECIMAL(18,2) DEFAULT 0,
    remaining_amount DECIMAL(18,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'CNY',
    receipt_date DATE NOT NULL,
    payment_method VARCHAR(20),
    bank_account VARCHAR(100),
    transaction_no VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confirmer_id BIGINT,
    confirmed_at TIMESTAMP,
    purpose VARCHAR(500),
    remark VARCHAR(500),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- 预收款核销记录表
CREATE TABLE IF NOT EXISTS finance_prepayment_usage (
    id BIGSERIAL PRIMARY KEY,
    prepayment_id BIGINT NOT NULL,
    fee_id BIGINT NOT NULL,
    matter_id BIGINT,
    amount DECIMAL(18,2) NOT NULL,
    usage_time TIMESTAMP NOT NULL,
    operator_id BIGINT,
    remark VARCHAR(500),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_prepayment_client_id ON finance_prepayment(client_id);
CREATE INDEX IF NOT EXISTS idx_prepayment_contract_id ON finance_prepayment(contract_id);
CREATE INDEX IF NOT EXISTS idx_prepayment_matter_id ON finance_prepayment(matter_id);
CREATE INDEX IF NOT EXISTS idx_prepayment_status ON finance_prepayment(status);
CREATE INDEX IF NOT EXISTS idx_prepayment_usage_prepayment_id ON finance_prepayment_usage(prepayment_id);
CREATE INDEX IF NOT EXISTS idx_prepayment_usage_fee_id ON finance_prepayment_usage(fee_id);

-- 注释
COMMENT ON TABLE finance_prepayment IS '预收款表';
COMMENT ON COLUMN finance_prepayment.prepayment_no IS '预收款编号';
COMMENT ON COLUMN finance_prepayment.client_id IS '客户ID';
COMMENT ON COLUMN finance_prepayment.contract_id IS '合同ID';
COMMENT ON COLUMN finance_prepayment.matter_id IS '项目ID';
COMMENT ON COLUMN finance_prepayment.amount IS '预收款金额';
COMMENT ON COLUMN finance_prepayment.used_amount IS '已核销金额';
COMMENT ON COLUMN finance_prepayment.remaining_amount IS '剩余金额';
COMMENT ON COLUMN finance_prepayment.status IS '状态：PENDING-待确认, ACTIVE-有效, USED-已用完, REFUNDED-已退款, CANCELLED-已取消';

COMMENT ON TABLE finance_prepayment_usage IS '预收款核销记录表';
COMMENT ON COLUMN finance_prepayment_usage.prepayment_id IS '预收款ID';
COMMENT ON COLUMN finance_prepayment_usage.fee_id IS '收费记录ID';
COMMENT ON COLUMN finance_prepayment_usage.amount IS '核销金额';
COMMENT ON COLUMN finance_prepayment_usage.usage_time IS '核销时间';
