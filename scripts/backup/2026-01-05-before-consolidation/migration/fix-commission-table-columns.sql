-- ============================================
-- 修复 finance_commission 表结构
-- 添加 Commission 实体需要的列，移除旧列的 NOT NULL 约束
-- 创建时间: 2026-01-05
-- ============================================

-- 移除旧列的 NOT NULL 约束（因为新的计算逻辑不使用这些列）
ALTER TABLE finance_commission ALTER COLUMN payment_amount DROP NOT NULL;
ALTER TABLE finance_commission ALTER COLUMN firm_retention DROP NOT NULL;
ALTER TABLE finance_commission ALTER COLUMN commission_base DROP NOT NULL;
ALTER TABLE finance_commission ALTER COLUMN net_commission DROP NOT NULL;

-- 添加缺失的列
ALTER TABLE finance_commission ADD COLUMN IF NOT EXISTS gross_amount DECIMAL(15,2);
ALTER TABLE finance_commission ADD COLUMN IF NOT EXISTS cost_amount DECIMAL(15,2);
ALTER TABLE finance_commission ADD COLUMN IF NOT EXISTS net_amount DECIMAL(15,2);
ALTER TABLE finance_commission ADD COLUMN IF NOT EXISTS distribution_ratio DECIMAL(5,4);
ALTER TABLE finance_commission ADD COLUMN IF NOT EXISTS commission_rate DECIMAL(5,4);
ALTER TABLE finance_commission ADD COLUMN IF NOT EXISTS commission_amount DECIMAL(15,2);
ALTER TABLE finance_commission ADD COLUMN IF NOT EXISTS compensation_type VARCHAR(20);

-- 添加注释
COMMENT ON COLUMN finance_commission.gross_amount IS '毛收入';
COMMENT ON COLUMN finance_commission.cost_amount IS '成本';
COMMENT ON COLUMN finance_commission.net_amount IS '净收入';
COMMENT ON COLUMN finance_commission.distribution_ratio IS '分配比例';
COMMENT ON COLUMN finance_commission.commission_rate IS '提成比例';
COMMENT ON COLUMN finance_commission.commission_amount IS '提成金额';
COMMENT ON COLUMN finance_commission.compensation_type IS '薪酬模式：COMMISSION-提成制, SALARIED-授薪制, HYBRID-混合制';

-- 如果有旧数据，可以从现有字段迁移
UPDATE finance_commission 
SET gross_amount = payment_amount,
    net_amount = net_commission,
    commission_amount = net_commission
WHERE gross_amount IS NULL AND payment_amount IS NOT NULL;
