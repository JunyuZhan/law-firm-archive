-- 042-rename-partner-to-team-leader.sql
-- 将 PARTNER（合伙人）角色重命名为 TEAM_LEADER（团队负责人）
-- 执行日期: 2026-01-07

-- 更新角色表
UPDATE public.sys_role 
SET role_code = 'TEAM_LEADER',
    role_name = '团队负责人',
    description = '团队负责人，可查看本团队数据，负责团队业务管理',
    updated_at = NOW()
WHERE role_code = 'PARTNER';

-- 更新用户角色关联表中的角色名称缓存（如果有的话）
-- sys_user_role 表只存储 role_id，不受影响

-- 验证更新结果
-- SELECT * FROM public.sys_role WHERE id = 3;

