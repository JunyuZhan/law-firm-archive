-- 迁移脚本：添加提成制律师测试用户
-- 目的：测试提成计算功能（授薪制律师不参与提成）
-- 执行方式: docker exec -i law-postgres psql -U law_admin -d law_firm_dev < scripts/migration/add-commission-test-user.sql

-- 1. 创建一个提成制律师用户（如果不存在）
INSERT INTO sys_user (
    username, password, real_name, email, phone, 
    department_id, position, employee_no, lawyer_license_no,
    compensation_type, status, deleted
)
SELECT 
    'lawyer1', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', -- 密码: 123456
    '张律师',
    'lawyer1@lawfirm.com',
    '13800138001',
    1,
    '律师',
    'EMP002',
    'L20240001',
    'COMMISSION',  -- 提成制
    'ACTIVE',
    false
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'lawyer1');

-- 2. 获取新用户ID并分配角色
DO $$
DECLARE
    v_user_id BIGINT;
    v_role_id BIGINT;
BEGIN
    SELECT id INTO v_user_id FROM sys_user WHERE username = 'lawyer1';
    SELECT id INTO v_role_id FROM sys_role WHERE role_code = 'LEAD_LAWYER' LIMIT 1;
    
    IF v_user_id IS NOT NULL AND v_role_id IS NOT NULL THEN
        -- 3. 分配律师角色
        INSERT INTO sys_user_role (user_id, role_id)
        SELECT v_user_id, v_role_id
        WHERE NOT EXISTS (
            SELECT 1 FROM sys_user_role 
            WHERE user_id = v_user_id AND role_id = v_role_id
        );
        
        -- 4. 将新律师添加为案件参与人（替换或新增）
        UPDATE matter_participant 
        SET user_id = v_user_id, 
            commission_rate = 100.00,
            is_originator = true
        WHERE matter_id IN (1, 2) AND role = 'LEAD';
        
        RAISE NOTICE '已将律师 % (ID: %) 设置为案件参与人', 'lawyer1', v_user_id;
    ELSE
        RAISE NOTICE '用户或角色不存在';
    END IF;
END $$;

-- 5. 验证结果
SELECT '新用户信息:' AS info;
SELECT id, username, real_name, compensation_type, status 
FROM sys_user 
WHERE username = 'lawyer1';

SELECT '案件参与人:' AS info;
SELECT mp.id, mp.matter_id, mp.user_id, u.real_name, u.compensation_type, mp.role, mp.is_originator, mp.commission_rate
FROM matter_participant mp
JOIN sys_user u ON mp.user_id = u.id
WHERE mp.deleted = false
ORDER BY mp.matter_id;
