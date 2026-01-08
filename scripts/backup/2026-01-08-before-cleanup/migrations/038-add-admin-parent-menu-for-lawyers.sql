-- 为律师角色添加行政管理模块父菜单权限
-- 创建日期: 2026-01-07
-- 问题：律师角色有印章管理等子菜单权限，但缺少行政管理父菜单权限，导致前端不显示

-- 获取下一个可用的 sys_role_menu id
DO $$
DECLARE
    next_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) + 1 INTO next_id FROM sys_role_menu;
    
    -- 为合伙人（PARTNER，role_id=3）添加行政管理父菜单权限
    INSERT INTO sys_role_menu (id, role_id, menu_id, created_at)
    VALUES (next_id, 3, 9, NOW())
    ON CONFLICT (role_id, menu_id) DO NOTHING;
    
    -- 为主办律师（LEAD_LAWYER，role_id=6）添加行政管理父菜单权限
    INSERT INTO sys_role_menu (id, role_id, menu_id, created_at)
    VALUES (next_id + 1, 6, 9, NOW())
    ON CONFLICT (role_id, menu_id) DO NOTHING;
    
    -- 为协办律师（ASSOCIATE，role_id=7）添加行政管理父菜单权限
    INSERT INTO sys_role_menu (id, role_id, menu_id, created_at)
    VALUES (next_id + 2, 7, 9, NOW())
    ON CONFLICT (role_id, menu_id) DO NOTHING;
    
    -- 为实习律师（TRAINEE，role_id=9）添加行政管理父菜单权限
    INSERT INTO sys_role_menu (id, role_id, menu_id, created_at)
    VALUES (next_id + 3, 9, 9, NOW())
    ON CONFLICT (role_id, menu_id) DO NOTHING;
END $$;

