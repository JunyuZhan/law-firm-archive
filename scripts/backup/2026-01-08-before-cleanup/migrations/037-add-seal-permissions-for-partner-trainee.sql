-- 为合伙人（PARTNER）和实习律师（TRAINEE）添加印章管理和用印申请权限
-- 创建日期: 2026-01-07

-- 获取下一个可用的 sys_role_menu id
-- 先查询当前最大值
DO $$
DECLARE
    next_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) + 1 INTO next_id FROM sys_role_menu;
    
    -- 为合伙人（PARTNER，role_id=3）添加印章管理权限
    INSERT INTO sys_role_menu (id, role_id, menu_id, created_at)
    VALUES (next_id, 3, 63, NOW())
    ON CONFLICT (role_id, menu_id) DO NOTHING;
    
    -- 为合伙人（PARTNER，role_id=3）添加用印申请权限
    INSERT INTO sys_role_menu (id, role_id, menu_id, created_at)
    VALUES (next_id + 1, 3, 64, NOW())
    ON CONFLICT (role_id, menu_id) DO NOTHING;
    
    -- 为实习律师（TRAINEE，role_id=9）添加印章管理权限（查看权限）
    INSERT INTO sys_role_menu (id, role_id, menu_id, created_at)
    VALUES (next_id + 2, 9, 63, NOW())
    ON CONFLICT (role_id, menu_id) DO NOTHING;
    
    -- 为实习律师（TRAINEE，role_id=9）添加用印申请权限
    INSERT INTO sys_role_menu (id, role_id, menu_id, created_at)
    VALUES (next_id + 3, 9, 64, NOW())
    ON CONFLICT (role_id, menu_id) DO NOTHING;
END $$;

