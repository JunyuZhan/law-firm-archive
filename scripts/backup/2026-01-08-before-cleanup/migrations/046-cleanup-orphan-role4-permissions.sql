-- 046-cleanup-orphan-role4-permissions.sql
-- 清理孤儿权限数据：role_id=4 在 sys_role 表中不存在
-- 执行日期: 2026-01-08

-- 首先检查 role_id=4 是否真的不存在
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.sys_role WHERE id = 4 AND deleted = false) THEN
        RAISE NOTICE 'role_id=4 不存在，开始清理孤儿权限数据...';
        
        -- 删除 role_id=4 的所有权限分配记录
        DELETE FROM public.sys_role_menu WHERE role_id = 4;
        
        RAISE NOTICE '已清理 role_id=4 的所有孤儿权限数据';
    ELSE
        RAISE NOTICE 'role_id=4 存在，无需清理';
    END IF;
END $$;

-- 同时检查是否有其他孤儿权限（引用了不存在的角色）
-- 仅报告，不自动删除
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM public.sys_role_menu rm
    LEFT JOIN public.sys_role r ON rm.role_id = r.id AND r.deleted = false
    WHERE r.id IS NULL;
    
    IF orphan_count > 0 THEN
        RAISE NOTICE '警告：还存在 % 条引用了不存在角色的孤儿权限记录', orphan_count;
    ELSE
        RAISE NOTICE '所有权限记录都引用了有效的角色';
    END IF;
END $$;

