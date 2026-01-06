-- 迁移脚本：将 admin 用户的薪酬类型改为混合制（保底工资+提成）
-- 这样 admin 既能拿固定工资，也能参与项目提成
-- 执行方式: docker exec -i law-postgres psql -U law_admin -d law_firm_dev < scripts/migration/update-admin-compensation-type.sql

-- 更新 admin 用户的薪酬类型为 HYBRID（混合制）
UPDATE sys_user 
SET compensation_type = 'HYBRID'
WHERE username = 'admin';

-- 验证结果
SELECT id, username, real_name, compensation_type,
       CASE compensation_type
           WHEN 'SALARIED' THEN '纯授薪制（不参与提成）'
           WHEN 'COMMISSION' THEN '纯提成制（无底薪）'
           WHEN 'HYBRID' THEN '混合制（保底工资+提成）'
       END AS compensation_desc
FROM sys_user 
WHERE deleted = false;
