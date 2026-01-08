-- 调整印章管理和用印申请的权限分配
-- 创建日期: 2026-01-07
-- 规则：
-- 1. 用印申请（menu_id=64）：所有角色都可以看到和使用
-- 2. 印章管理（menu_id=63）：只有管理员、律所主任、行政人员可以管理

-- 删除非管理员角色的印章管理权限
-- 保留权限的角色：ADMIN(1), DIRECTOR(2), ADMIN_STAFF(8)
-- 需要删除的角色：PARTNER(3), FINANCE_MANAGER(4), FINANCE(5), LEAD_LAWYER(6), ASSOCIATE(7), TRAINEE(9)

DELETE FROM sys_role_menu 
WHERE menu_id = 63 
  AND role_id IN (3, 4, 5, 6, 7, 9);

-- 确保所有角色都有用印申请权限（如果还没有的话）
-- 用印申请（menu_id=64）应该对所有角色开放
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT r.id, 64, NOW()
FROM sys_role r
WHERE r.deleted = false
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = 64
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 确保管理员、律所主任、行政人员有印章管理权限（如果还没有的话）
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT r.id, 63, NOW()
FROM sys_role r
WHERE r.deleted = false
  AND r.id IN (1, 2, 8)  -- ADMIN, DIRECTOR, ADMIN_STAFF
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = r.id AND rm.menu_id = 63
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

