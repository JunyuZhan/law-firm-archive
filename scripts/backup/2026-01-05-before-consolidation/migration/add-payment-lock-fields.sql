-- 扩展 finance_payment 表，增加锁定字段
-- 用于实现收款登记后数据锁定功能
-- Requirements: 3.2

-- 添加锁定相关字段
ALTER TABLE finance_payment ADD COLUMN IF NOT EXISTS locked BOOLEAN DEFAULT FALSE;
ALTER TABLE finance_payment ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP;
ALTER TABLE finance_payment ADD COLUMN IF NOT EXISTS locked_by BIGINT;

-- 添加外键约束（锁定人关联用户表）
ALTER TABLE finance_payment 
    ADD CONSTRAINT fk_payment_locked_by 
    FOREIGN KEY (locked_by) REFERENCES sys_user(id);

-- 添加索引以优化查询
CREATE INDEX IF NOT EXISTS idx_finance_payment_locked ON finance_payment(locked);

-- 添加注释
COMMENT ON COLUMN finance_payment.locked IS '是否锁定（登记后自动锁定）';
COMMENT ON COLUMN finance_payment.locked_at IS '锁定时间';
COMMENT ON COLUMN finance_payment.locked_by IS '锁定人ID';
