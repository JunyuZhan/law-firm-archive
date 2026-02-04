-- =====================================================
-- 模板菜单位置调整
-- =====================================================
-- 日期: 2026-02-03
-- 描述: 将合同模板和出函模板从系统管理移到各自的业务模块
--   - 出函模板: 系统管理 → 行政管理
--   - 合同模板: 系统管理 → 财务管理
-- =====================================================

-- 1. 移动出函模板到行政管理 (parent_id: 2→9)
UPDATE sys_menu 
SET 
    parent_id = 9,
    path = '/admin/letter-template',
    component = 'admin/letter-template/index',
    sort_order = 20,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 27;

-- 2. 移动合同模板到财务管理 (parent_id: 2→5)
UPDATE sys_menu 
SET 
    parent_id = 5,
    path = '/finance/contract-template',
    component = 'finance/contract-template/index',
    sort_order = 20,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 28;

-- 验证结果
SELECT id, parent_id, name, path, component 
FROM sys_menu 
WHERE id IN (27, 28);
