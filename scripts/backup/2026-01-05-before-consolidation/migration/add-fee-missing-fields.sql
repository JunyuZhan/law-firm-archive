-- 迁移脚本：为 finance_fee 表添加缺失的字段
-- 执行方式: docker exec -i law-postgres psql -U law_admin -d law_firm_dev < scripts/migration/add-fee-missing-fields.sql

-- 添加收费项目名称字段
ALTER TABLE finance_fee ADD COLUMN IF NOT EXISTS fee_name VARCHAR(200);
COMMENT ON COLUMN finance_fee.fee_name IS '收费项目名称';

-- 添加负责人ID字段
ALTER TABLE finance_fee ADD COLUMN IF NOT EXISTS responsible_id BIGINT;
COMMENT ON COLUMN finance_fee.responsible_id IS '负责人ID';

-- 确认修改
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'finance_fee' 
ORDER BY ordinal_position;
