-- ============================================
-- 修复财务管理模块子菜单显示问题
-- 为所有财务管理子菜单设置 visible 和 status 字段
-- ============================================

-- 更新财务管理子菜单的 visible 和 status 字段
UPDATE sys_menu 
SET visible = TRUE, status = 'ENABLED'
WHERE parent_id = 5 
  AND menu_type = 'MENU'
  AND (visible IS NULL OR status IS NULL OR status != 'ENABLED');

-- 验证更新结果
SELECT id, name, path, visible, status 
FROM sys_menu 
WHERE parent_id = 5 
ORDER BY sort_order;

