-- 046-simplify-roles.sql
-- 简化系统角色：从9个减少到7个
-- 执行日期: 2026-01-07

-- ============================================
-- 一、合并律师角色：LEAD_LAWYER + ASSOCIATE → LAWYER
-- ============================================

-- 1. 重命名 LEAD_LAWYER 为 LAWYER
UPDATE sys_role SET 
    role_code = 'LAWYER', 
    role_name = '律师',
    description = '执业律师，处理案件的主要人员'
WHERE id = 6 AND role_code = 'LEAD_LAWYER';

-- 2. 将 ASSOCIATE 用户迁移到 LAWYER
UPDATE sys_user_role SET role_id = 6 WHERE role_id = 7;

-- 3. 将 ASSOCIATE 的权限合并到 LAWYER（如果 LAWYER 没有的话）
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 6, menu_id, NOW()
FROM sys_role_menu 
WHERE role_id = 7
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 4. 删除 ASSOCIATE 角色的权限
DELETE FROM sys_role_menu WHERE role_id = 7;

-- 5. 删除 ASSOCIATE 角色
DELETE FROM sys_role WHERE id = 7 AND role_code = 'ASSOCIATE';

-- ============================================
-- 二、合并财务角色：FINANCE_MANAGER + FINANCE → FINANCE
-- ============================================

-- 1. 更新 FINANCE 角色信息（继承 FINANCE_MANAGER 的 ALL 数据范围）
UPDATE sys_role SET 
    role_name = '财务',
    data_scope = 'ALL',
    description = '财务人员，管理律所财务工作'
WHERE id = 5 AND role_code = 'FINANCE';

-- 2. 将 FINANCE_MANAGER 用户迁移到 FINANCE
UPDATE sys_user_role SET role_id = 5 WHERE role_id = 4;

-- 3. 将 FINANCE_MANAGER 的权限合并到 FINANCE
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 5, menu_id, NOW()
FROM sys_role_menu 
WHERE role_id = 4
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 4. 删除 FINANCE_MANAGER 角色的权限
DELETE FROM sys_role_menu WHERE role_id = 4;

-- 5. 删除 FINANCE_MANAGER 角色
DELETE FROM sys_role WHERE id = 4 AND role_code = 'FINANCE_MANAGER';

-- ============================================
-- 三、更新其他角色名称（统一简洁）
-- ============================================

UPDATE sys_role SET role_name = '管理员' WHERE id = 1 AND role_code = 'ADMIN';
UPDATE sys_role SET role_name = '行政' WHERE id = 8 AND role_code = 'ADMIN_STAFF';

