-- ============================================
-- 财务合同变更记录表
-- 创建时间: 2026-01-05
-- 用途: 记录律师变更合同后，财务模块需要处理的变更记录
-- ============================================

-- 财务合同变更记录表
CREATE TABLE IF NOT EXISTS finance_contract_amendment (
    id BIGSERIAL PRIMARY KEY,
    amendment_no VARCHAR(50) NOT NULL UNIQUE,
    contract_id BIGINT NOT NULL,
    amendment_type VARCHAR(50) NOT NULL,
    before_snapshot JSONB,
    after_snapshot JSONB,
    amendment_reason TEXT,
    lawyer_amended_by BIGINT,
    lawyer_amended_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING',
    finance_handled_by BIGINT,
    finance_handled_at TIMESTAMP,
    finance_remark TEXT,
    affects_payments BOOLEAN DEFAULT FALSE,
    affected_payment_ids JSONB,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_finance_contract_amendment_contract ON finance_contract_amendment(contract_id);
CREATE INDEX IF NOT EXISTS idx_finance_contract_amendment_status ON finance_contract_amendment(status);
CREATE INDEX IF NOT EXISTS idx_finance_contract_amendment_type ON finance_contract_amendment(amendment_type);

-- 添加注释
COMMENT ON TABLE finance_contract_amendment IS '财务合同变更记录表';
COMMENT ON COLUMN finance_contract_amendment.amendment_no IS '变更编号';
COMMENT ON COLUMN finance_contract_amendment.contract_id IS '合同ID';
COMMENT ON COLUMN finance_contract_amendment.amendment_type IS '变更类型：AMOUNT-金额变更, PARTICIPANT-参与人变更, SCHEDULE-付款计划变更, OTHER-其他';
COMMENT ON COLUMN finance_contract_amendment.before_snapshot IS '变更前数据快照（JSON格式）';
COMMENT ON COLUMN finance_contract_amendment.after_snapshot IS '变更后数据快照（JSON格式）';
COMMENT ON COLUMN finance_contract_amendment.amendment_reason IS '变更说明';
COMMENT ON COLUMN finance_contract_amendment.lawyer_amended_by IS '律师变更人ID';
COMMENT ON COLUMN finance_contract_amendment.lawyer_amended_at IS '律师变更时间';
COMMENT ON COLUMN finance_contract_amendment.status IS '状态：PENDING-待处理, SYNCED-已同步, IGNORED-已忽略, PARTIAL-部分同步';
COMMENT ON COLUMN finance_contract_amendment.finance_handled_by IS '财务处理人ID';
COMMENT ON COLUMN finance_contract_amendment.finance_handled_at IS '财务处理时间';
COMMENT ON COLUMN finance_contract_amendment.finance_remark IS '财务处理备注';
COMMENT ON COLUMN finance_contract_amendment.affects_payments IS '是否影响已有收款';
COMMENT ON COLUMN finance_contract_amendment.affected_payment_ids IS '受影响的收款ID列表';
