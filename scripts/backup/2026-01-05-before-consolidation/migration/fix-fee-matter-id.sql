-- 迁移脚本：修复 finance_fee 表中缺失的 matter_id
-- 问题：早期创建的 Fee 记录没有正确关联 matter_id，导致提成计算失败
-- 执行方式: docker exec -i law-postgres psql -U law_admin -d law_firm_dev < scripts/migration/fix-fee-matter-id.sql

-- 1. 通过 contract_id 关联更新 matter_id
UPDATE finance_fee f
SET matter_id = c.matter_id
FROM finance_contract c
WHERE f.contract_id = c.id
  AND f.matter_id IS NULL
  AND c.matter_id IS NOT NULL;

-- 2. 同样修复 finance_payment 表中的 matter_id
UPDATE finance_payment p
SET matter_id = f.matter_id
FROM finance_fee f
WHERE p.fee_id = f.id
  AND p.matter_id IS NULL
  AND f.matter_id IS NOT NULL;

-- 3. 验证修复结果
SELECT 'finance_fee 修复结果:' AS info;
SELECT id, fee_no, contract_id, matter_id, amount 
FROM finance_fee 
ORDER BY id;

SELECT 'finance_payment 修复结果:' AS info;
SELECT id, payment_no, fee_id, matter_id, amount, status 
FROM finance_payment 
ORDER BY id;

-- 4. 检查是否还有未关联的记录
SELECT 'finance_fee 中仍缺失 matter_id 的记录:' AS info;
SELECT id, fee_no, contract_id, matter_id 
FROM finance_fee 
WHERE matter_id IS NULL;

SELECT 'finance_payment 中仍缺失 matter_id 的记录:' AS info;
SELECT id, payment_no, fee_id, matter_id 
FROM finance_payment 
WHERE matter_id IS NULL;
