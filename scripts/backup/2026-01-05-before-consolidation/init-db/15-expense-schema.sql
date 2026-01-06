-- ============================================
-- 成本与报销管理模块 - 数据库表结构
-- ============================================

-- 费用报销表
CREATE TABLE IF NOT EXISTS finance_expense (
    id BIGSERIAL PRIMARY KEY,
    expense_no VARCHAR(50) NOT NULL UNIQUE,
    
    -- 关联信息
    matter_id BIGINT,  -- 关联项目（可选，公共费用可为空）
    applicant_id BIGINT NOT NULL,  -- 申请人ID
    
    -- 费用信息
    expense_type VARCHAR(50),  -- 费用类型：TRAVEL-差旅费, MEAL-餐费, ACCOMMODATION-住宿费, TRANSPORT-交通费, MATERIAL-材料费, OTHER-其他
    expense_category VARCHAR(50),  -- 费用分类：CASE_COST-办案成本, OFFICE_COST-办公费用, OTHER-其他
    expense_date DATE NOT NULL,  -- 费用发生日期
    amount DECIMAL(15, 2) NOT NULL,  -- 费用金额
    currency VARCHAR(10) DEFAULT 'CNY',  -- 币种
    
    -- 费用详情
    description TEXT,  -- 费用说明
    vendor_name VARCHAR(200),  -- 供应商/商户名称
    invoice_no VARCHAR(100),  -- 发票号
    invoice_url VARCHAR(500),  -- 发票附件URL
    
    -- 审批信息
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING-待审批, APPROVED-已审批, REJECTED-已驳回, PAID-已支付
    approver_id BIGINT,  -- 审批人ID
    approved_at TIMESTAMP,  -- 审批时间
    approval_comment TEXT,  -- 审批意见
    
    -- 支付信息
    paid_at TIMESTAMP,  -- 支付时间
    paid_by BIGINT,  -- 支付人ID
    payment_method VARCHAR(50),  -- 支付方式：CASH-现金, BANK_TRANSFER-银行转账, ALIPAY-支付宝, WECHAT-微信
    
    -- 成本归集
    is_cost_allocation BOOLEAN DEFAULT FALSE,  -- 是否已归集到项目成本
    allocated_to_matter_id BIGINT,  -- 归集到的项目ID
    
    -- 备注
    remark TEXT,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE finance_expense IS '费用报销表';
COMMENT ON COLUMN finance_expense.expense_no IS '报销单号';
COMMENT ON COLUMN finance_expense.matter_id IS '关联项目ID（可选）';
COMMENT ON COLUMN finance_expense.applicant_id IS '申请人ID';
COMMENT ON COLUMN finance_expense.expense_type IS '费用类型：TRAVEL-差旅费, MEAL-餐费, ACCOMMODATION-住宿费, TRANSPORT-交通费, MATERIAL-材料费, OTHER-其他';
COMMENT ON COLUMN finance_expense.expense_category IS '费用分类：CASE_COST-办案成本, OFFICE_COST-办公费用, OTHER-其他';
COMMENT ON COLUMN finance_expense.status IS '状态：PENDING-待审批, APPROVED-已审批, REJECTED-已驳回, PAID-已支付';
COMMENT ON COLUMN finance_expense.is_cost_allocation IS '是否已归集到项目成本';

CREATE INDEX idx_finance_expense_matter_id ON finance_expense(matter_id);
CREATE INDEX idx_finance_expense_applicant_id ON finance_expense(applicant_id);
CREATE INDEX idx_finance_expense_status ON finance_expense(status);
CREATE INDEX idx_finance_expense_expense_date ON finance_expense(expense_date);
CREATE INDEX idx_finance_expense_expense_no ON finance_expense(expense_no);

-- 成本归集记录表（用于记录项目成本归集历史）
CREATE TABLE IF NOT EXISTS finance_cost_allocation (
    id BIGSERIAL PRIMARY KEY,
    matter_id BIGINT NOT NULL,  -- 项目ID
    expense_id BIGINT NOT NULL,  -- 费用ID
    allocated_amount DECIMAL(15, 2) NOT NULL,  -- 归集金额
    allocation_date DATE NOT NULL,  -- 归集日期
    allocated_by BIGINT NOT NULL,  -- 归集操作人
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE finance_cost_allocation IS '成本归集记录表';
COMMENT ON COLUMN finance_cost_allocation.matter_id IS '项目ID';
COMMENT ON COLUMN finance_cost_allocation.expense_id IS '费用ID';
COMMENT ON COLUMN finance_cost_allocation.allocated_amount IS '归集金额';

CREATE INDEX idx_finance_cost_allocation_matter_id ON finance_cost_allocation(matter_id);
CREATE INDEX idx_finance_cost_allocation_expense_id ON finance_cost_allocation(expense_id);

-- 成本分摊表（用于记录公共成本分摊到多个项目）
CREATE TABLE IF NOT EXISTS finance_cost_split (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL,  -- 费用ID（公共费用）
    matter_id BIGINT NOT NULL,  -- 分摊到的项目ID
    split_amount DECIMAL(15, 2) NOT NULL,  -- 分摊金额
    split_ratio DECIMAL(5, 4),  -- 分摊比例（可选，如0.25表示25%）
    split_method VARCHAR(50),  -- 分摊方式：EQUAL-平均分摊, RATIO-按比例, MANUAL-手动指定
    split_date DATE NOT NULL,  -- 分摊日期
    split_by BIGINT NOT NULL,  -- 分摊操作人
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE finance_cost_split IS '成本分摊表';
COMMENT ON COLUMN finance_cost_split.expense_id IS '费用ID（公共费用）';
COMMENT ON COLUMN finance_cost_split.matter_id IS '分摊到的项目ID';
COMMENT ON COLUMN finance_cost_split.split_amount IS '分摊金额';
COMMENT ON COLUMN finance_cost_split.split_ratio IS '分摊比例';
COMMENT ON COLUMN finance_cost_split.split_method IS '分摊方式：EQUAL-平均分摊, RATIO-按比例, MANUAL-手动指定';

CREATE INDEX idx_finance_cost_split_expense_id ON finance_cost_split(expense_id);
CREATE INDEX idx_finance_cost_split_matter_id ON finance_cost_split(matter_id);

