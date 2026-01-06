-- ============================================
-- 智慧律所管理系统 - 发票管理模块数据库脚本
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 发票表
CREATE TABLE IF NOT EXISTS finance_invoice (
    id BIGSERIAL PRIMARY KEY,
    invoice_no VARCHAR(50) NOT NULL UNIQUE,
    
    -- 关联信息
    fee_id BIGINT,
    contract_id BIGINT,
    client_id BIGINT NOT NULL,
    
    -- 发票信息
    invoice_type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    tax_no VARCHAR(50),
    amount DECIMAL(15, 2) NOT NULL,
    tax_rate DECIMAL(5, 4) DEFAULT 0.06,
    tax_amount DECIMAL(15, 2),
    content TEXT,
    invoice_date DATE,
    
    -- 状态信息
    status VARCHAR(20) DEFAULT 'PENDING',
    
    -- 申请人/开票人
    applicant_id BIGINT,
    issuer_id BIGINT,
    
    -- 文件
    file_url VARCHAR(500),
    
    -- 备注
    remark TEXT,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_finance_invoice_invoice_no ON finance_invoice(invoice_no);
CREATE INDEX idx_finance_invoice_client_id ON finance_invoice(client_id);
CREATE INDEX idx_finance_invoice_contract_id ON finance_invoice(contract_id);
CREATE INDEX idx_finance_invoice_fee_id ON finance_invoice(fee_id);
CREATE INDEX idx_finance_invoice_status ON finance_invoice(status);
CREATE INDEX idx_finance_invoice_date ON finance_invoice(invoice_date);

COMMENT ON TABLE finance_invoice IS '发票表';
COMMENT ON COLUMN finance_invoice.invoice_no IS '发票号码';
COMMENT ON COLUMN finance_invoice.invoice_type IS '发票类型：SPECIAL-增值税专用发票, NORMAL-增值税普通发票, ELECTRONIC-电子发票';
COMMENT ON COLUMN finance_invoice.status IS '状态：PENDING-待开票, ISSUED-已开票, CANCELLED-已作废, RED-已红冲';

