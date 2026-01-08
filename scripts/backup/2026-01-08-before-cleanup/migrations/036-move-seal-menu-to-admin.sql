-- 将印章管理和用印申请从卷宗管理模块移动到行政管理模块
-- 创建日期: 2026-01-07

-- 更新印章管理菜单：从卷宗管理(parent_id=6)移动到行政管理(parent_id=9)
-- 行政管理模块最后一个菜单的sort_order是12，所以设置为13
UPDATE sys_menu 
SET parent_id = 9, 
    sort_order = 13,
    updated_at = NOW()
WHERE id = 63 AND name = '印章管理';

-- 更新用印申请菜单：从卷宗管理(parent_id=6)移动到行政管理(parent_id=9)
-- 设置为14，紧跟在印章管理后面
UPDATE sys_menu 
SET parent_id = 9, 
    sort_order = 14,
    updated_at = NOW()
WHERE id = 64 AND name = '用印申请';

