-- ============================================================
-- 晋升管理权限按钮迁移脚本
-- 执行日期: 2026-01-09
-- 说明: 为晋升管理(hr:promotion)添加子权限按钮
-- ============================================================

-- 晋升管理菜单ID为103，添加子权限按钮
-- 查找可用的ID范围
-- 假设从200开始使用

-- 添加晋升管理的权限按钮 (使用730-739范围的ID)
INSERT INTO public.sys_menu (id, parent_id, title, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, keep_alive, show_in_tab, created_at, updated_at, created_by, updated_by, deleted)
VALUES 
-- 职级查看
(730, 103, '职级查看', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:view', 1, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
-- 职级创建
(731, 103, '职级创建', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:create', 2, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
-- 职级编辑
(732, 103, '职级编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:edit', 3, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
-- 职级删除
(733, 103, '职级删除', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:delete', 4, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
-- 晋升审批
(734, 103, '晋升审批', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:approve', 5, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

-- 将权限分配给超级管理员角色(role_id = 1)
INSERT INTO public.sys_role_menu (role_id, menu_id)
SELECT 1, id FROM public.sys_menu WHERE id IN (730, 731, 732, 733, 734)
ON CONFLICT DO NOTHING;

-- 更新菜单序列以避免冲突
SELECT setval('public.sys_menu_id_seq', GREATEST((SELECT MAX(id) FROM public.sys_menu), 740), true);

-- 验证插入结果
-- SELECT id, parent_id, title, permission FROM public.sys_menu WHERE parent_id = 103;

