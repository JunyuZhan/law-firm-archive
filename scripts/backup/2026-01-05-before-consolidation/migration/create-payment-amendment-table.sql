-- 创建收款变更申请表
-- 用于已锁定收款记录的变更申请和审批流程
-- Requirements: 3.5

CREATE TABLE IF NOT EXISTS fin_payment_amendment (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    original_amount DECIMAL(15,2) NOT NULL,
    new_amount DECIMAL(15,2) NOT NULL,
    reason TEXT NOT NULL,
    requested_by BIGINT NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_amendment_payment FOREIGN KEY (payment_id) 
        REFERENCES finance_payment(id),
    CONSTRAINT fk_amendment_requester FOREIGN KEY (requested_by) 
        REFERENCES sys_user(id),
    CONSTRAINT fk_amendment_approver FOREIGN KEY (approved_by) 
        REFERENCES sys_user(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_payment_amendment_payment_id ON fin_payment_amendment(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_amendment_status ON fin_payment_amendment(status);
CREATE INDEX IF NOT EXISTS idx_payment_amendment_requested_by ON fin_payment_amendment(requested_by);
CREATE INDEX IF NOT EXISTS idx_payment_amendment_requested_at ON fin_payment_amendment(requested_at);

-- 添加注释
COMMENT ON TABLE fin_payment_amendment IS '收款变更申请表';
COMMENT ON COLUMN fin_payment_amendment.id IS '主键ID';
COMMENT ON COLUMN fin_payment_amendment.payment_id IS '关联收款记录ID';
COMMENT ON COLUMN fin_payment_amendment.original_amount IS '原金额';
COMMENT ON COLUMN fin_payment_amendment.new_amount IS '新金额';
COMMENT ON COLUMN fin_payment_amendment.reason IS '变更原因';
COMMENT ON COLUMN fin_payment_amendment.requested_by IS '申请人ID';
COMMENT ON COLUMN fin_payment_amendment.requested_at IS '申请时间';
COMMENT ON COLUMN fin_payment_amendment.approved_by IS '审批人ID';
COMMENT ON COLUMN fin_payment_amendment.approved_at IS '审批时间';
COMMENT ON COLUMN fin_payment_amendment.status IS '状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝';
COMMENT ON COLUMN fin_payment_amendment.reject_reason IS '拒绝原因';
