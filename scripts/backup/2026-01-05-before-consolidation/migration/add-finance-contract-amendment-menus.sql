-- ============================================
-- 财务合同变更管理菜单和权限
-- 创建时间: 2026-01-05
-- ============================================

-- 获取财务管理菜单ID
DO $$
DECLARE
    v_finance_menu_id BIGINT;
    v_amendment_menu_id BIGINT;
BEGIN
    -- 查找财务管理菜单
    SELECT id INTO v_finance_menu_id FROM sys_menu WHERE path = '/finance' AND deleted = false LIMIT 1;
    
    IF v_finance_menu_id IS NULL THEN
        RAISE NOTICE '财务管理菜单不存在，跳过添加子菜单';
        RETURN;
    END IF;

    -- 检查是否已存在合同变更菜单
    SELECT id INTO v_amendment_menu_id FROM sys_menu WHERE path = '/finance/contract-amendment' AND deleted = false LIMIT 1;
    
    IF v_amendment_menu_id IS NULL THEN
        -- 添加合同变更管理菜单
        INSERT INTO sys_menu (parent_id, name, path, component, icon, menu_type, permission, sort_order, visible, status, created_at, updated_at)
        VALUES (v_finance_menu_id, '合同变更处理', '/finance/contract-amendment', '/finance/contract-amendment/index', 'ant-design:diff-outlined', 'MENU', 'finance:contract:amendment:view', 60, true, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id INTO v_amendment_menu_id;

        -- 添加按钮权限
        INSERT INTO sys_menu (parent_id, name, path, component, icon, menu_type, permission, sort_order, visible, status, created_at, updated_at)
        VALUES 
            (v_amendment_menu_id, '同步变更', NULL, NULL, NULL, 'BUTTON', 'finance:contract:amendment:sync', 1, true, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (v_amendment_menu_id, '忽略变更', NULL, NULL, NULL, 'BUTTON', 'finance:contract:amendment:ignore', 2, true, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

        RAISE NOTICE '已添加合同变更管理菜单';
    ELSE
        RAISE NOTICE '合同变更管理菜单已存在，跳过';
    END IF;
END $$;

-- 为财务角色分配权限
DO $$
DECLARE
    v_finance_role_id BIGINT;
    v_menu_id BIGINT;
BEGIN
    -- 查找财务角色
    SELECT id INTO v_finance_role_id FROM sys_role WHERE role_code = 'finance' AND deleted = false LIMIT 1;
    
    IF v_finance_role_id IS NULL THEN
        RAISE NOTICE '财务角色不存在，跳过权限分配';
        RETURN;
    END IF;

    -- 分配合同变更管理菜单权限
    FOR v_menu_id IN 
        SELECT id FROM sys_menu 
        WHERE (path = '/finance/contract-amendment' OR permission LIKE 'finance:contract:amendment:%') 
        AND deleted = false
    LOOP
        INSERT INTO sys_role_menu (role_id, menu_id, created_at)
        VALUES (v_finance_role_id, v_menu_id, CURRENT_TIMESTAMP)
        ON CONFLICT (role_id, menu_id) DO NOTHING;
    END LOOP;

    RAISE NOTICE '已为财务角色分配合同变更管理权限';
END $$;
