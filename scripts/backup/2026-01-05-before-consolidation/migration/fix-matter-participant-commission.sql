-- 迁移脚本：修复 matter_participant 表中的提成相关字段
-- 问题：commission_rate 为空导致提成计算时分配比例为0
-- 执行方式: docker exec -i law-postgres psql -U law_admin -d law_firm_dev < scripts/migration/fix-matter-participant-commission.sql

-- 1. 为主办律师(LEAD)设置默认提成比例 100%（如果只有一个参与人）
-- 如果有多个参与人，主办60%，协办40%
UPDATE matter_participant
SET commission_rate = 100.00
WHERE role = 'LEAD' 
  AND commission_rate IS NULL
  AND deleted = false;

-- 2. 为协办律师(CO_COUNSEL)设置默认提成比例
UPDATE matter_participant
SET commission_rate = 40.00
WHERE role = 'CO_COUNSEL' 
  AND commission_rate IS NULL
  AND deleted = false;

-- 3. 为助理(ASSISTANT)设置默认提成比例
UPDATE matter_participant
SET commission_rate = 10.00
WHERE role = 'ASSISTANT' 
  AND commission_rate IS NULL
  AND deleted = false;

-- 4. 如果主办律师同时是案源人，设置 is_originator = true
-- 这里假设第一个参与人是案源人（实际业务中应该在创建时指定）
UPDATE matter_participant mp
SET is_originator = true
WHERE mp.id IN (
    SELECT MIN(id) 
    FROM matter_participant 
    WHERE deleted = false 
    GROUP BY matter_id
)
AND is_originator IS NULL OR is_originator = false;

-- 5. 验证修复结果
SELECT 'matter_participant 修复结果:' AS info;
SELECT id, matter_id, user_id, role, is_originator, commission_rate 
FROM matter_participant 
WHERE deleted = false
ORDER BY matter_id, id;

-- 6. 检查用户的薪酬类型（影响提成资格）
SELECT 'sys_user 薪酬类型:' AS info;
SELECT id, username, real_name, compensation_type 
FROM sys_user 
WHERE deleted = false;
