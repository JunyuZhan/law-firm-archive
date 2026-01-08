-- 047-sync-trainee-to-lawyer.sql
-- 将实习律师权限同步为与律师一致
-- 后续从模块角度清理时再设计项目权限区别
-- 执行日期: 2026-01-07

-- 将律师有但实习律师没有的权限添加给实习律师
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 9, menu_id, NOW()
FROM sys_role_menu 
WHERE role_id = 6
AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 9)
ON CONFLICT (role_id, menu_id) DO NOTHING;

