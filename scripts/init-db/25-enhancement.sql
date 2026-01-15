-- =====================================================
-- 数据库增强脚本（整合版）
-- =====================================================
-- 版本: 2.0.0
-- 日期: 2026-01-15
-- 描述: 整合所有数据库增强功能
-- 说明: 此脚本包含以下增强：
--   - version 字段（乐观锁）
--   - 字典管理菜单
--   - 报表权限细化
--   - 审批权限细化
--   - 卷宗自动归档功能
-- =====================================================


-- =====================================================
-- 合并自: 25-add-version-column.sql
-- =====================================================

-- =====================================================
-- 律师事务所管理系统 - 添加乐观锁版本列
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-10
-- 描述: 为所有业务表添加 version 列，用于乐观锁并发控制
-- 说明: BaseEntity 基类包含 version 字段，所有继承该基类的实体
--       对应的表都需要有 version 列
-- =====================================================

-- 批量为所有缺少 version 列的表添加该列
DO $$
DECLARE
    tbl_name TEXT;
BEGIN
    FOR tbl_name IN 
        SELECT t.table_name
        FROM information_schema.tables t
        WHERE t.table_schema = 'public' 
        AND t.table_type = 'BASE TABLE'
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.columns c 
            WHERE c.table_schema = t.table_schema 
            AND c.table_name = t.table_name 
            AND c.column_name = 'version'
        )
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN version INTEGER DEFAULT 1', tbl_name);
        EXECUTE format('UPDATE %I SET version = 1 WHERE version IS NULL', tbl_name);
        RAISE NOTICE 'Added version column to table: %', tbl_name;
    END LOOP;
END $$;

-- 添加注释
COMMENT ON COLUMN public.sys_user.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sys_role.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sys_menu.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sys_department.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sys_config.version IS '乐观锁版本号';
COMMENT ON COLUMN public.crm_client.version IS '乐观锁版本号';
COMMENT ON COLUMN public.matter.version IS '乐观锁版本号';
COMMENT ON COLUMN public.finance_contract.version IS '乐观锁版本号';
COMMENT ON COLUMN public.finance_payment.version IS '乐观锁版本号';
COMMENT ON COLUMN public.finance_invoice.version IS '乐观锁版本号';
COMMENT ON COLUMN public.workbench_approval.version IS '乐观锁版本号';


-- =====================================================
-- 合并自: 26-add-dict-menu.sql
-- =====================================================

-- ============================================
-- 添加字典管理菜单
-- ============================================
-- 说明：在系统管理模块下添加字典管理菜单项
-- 执行时间：2026-01-10
-- ============================================

-- 插入字典管理菜单（系统管理下，ID=30，排序号=5，放在菜单管理之后）
INSERT INTO public.sys_menu (
    id, 
    parent_id, 
    name, 
    path, 
    component, 
    redirect, 
    icon, 
    menu_type, 
    permission, 
    sort_order, 
    visible, 
    status, 
    is_external, 
    is_cache, 
    created_at, 
    updated_at, 
    created_by, 
    updated_by, 
    deleted
) VALUES (
    30, 
    2, 
    '字典管理', 
    '/system/dict', 
    'system/dict/index', 
    NULL, 
    'BookOutlined', 
    'MENU', 
    'sys:dict:list', 
    5, 
    true, 
    'ENABLED', 
    false, 
    true, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP, 
    NULL, 
    NULL, 
    false
) ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    name = EXCLUDED.name,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    icon = EXCLUDED.icon,
    menu_type = EXCLUDED.menu_type,
    permission = EXCLUDED.permission,
    sort_order = EXCLUDED.sort_order,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

-- 为管理员角色分配字典管理菜单权限
-- 注意：role_id=1 是管理员角色（ADMIN），role_id=2 是律所主任（DIRECTOR）
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT r.id, 30, CURRENT_TIMESTAMP
FROM sys_role r
WHERE r.role_code IN ('ADMIN', 'DIRECTOR')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = 30
);

-- 更新菜单序列（如果需要）
SELECT setval('sys_menu_id_seq', GREATEST((SELECT MAX(id) FROM sys_menu), 30));


-- =====================================================
-- 合并自: 28-report-permission-refine.sql
-- =====================================================

-- =====================================================
-- 报表权限细化
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-11
-- 描述: 将报表权限按业务类型细分，区分财务报表和业务报表
-- =====================================================

-- 添加新的报表权限菜单
-- menu_id 使用 750-759 范围避免冲突
INSERT INTO public.sys_menu VALUES (750, 200, '财务报表查看', NULL, NULL, NULL, NULL, 'BUTTON', 'report:finance:view', 10, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (751, 200, '业务报表查看', NULL, NULL, NULL, NULL, 'BUTTON', 'report:matter:view', 11, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;

-- 为管理员角色(1)分配所有报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 750, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 751, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为主任角色(2)分配所有报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 750, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 751, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为团队负责人角色(3)分配业务报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 3, 751, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为财务角色(5)分配财务报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 5, 750, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为律师角色(6)分配业务报表权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 6, 751, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 说明：
-- report:finance:view - 财务相关报表（收入统计、律师业绩排行）
--   - 管理员、主任、财务 可查看
-- report:matter:view - 业务相关报表（项目统计、客户统计）
--   - 管理员、主任、团队负责人、律师 可查看


-- =====================================================
-- 合并自: 29-approval-permission-refine.sql
-- =====================================================

-- =====================================================
-- 审批权限细化
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-11
-- 描述: 将审批权限按业务类型细分
-- =====================================================

-- 添加按业务类型细分的审批权限
-- menu_id 使用 740-749 范围避免冲突
-- parent_id = 149（审批中心菜单）

INSERT INTO public.sys_menu VALUES (740, 149, '合同审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:contract:approve', 10, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (741, 149, '用印审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:seal:approve', 11, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (742, 149, '利冲审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:conflict:approve', 12, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (743, 149, '费用审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:expense:approve', 13, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.sys_menu VALUES (744, 149, '结案审批', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:matter-close:approve', 14, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;

-- 为管理员角色(1)分配所有审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 740, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 741, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 742, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 743, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 1, 744, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为主任角色(2)分配所有审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 740, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 741, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 742, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 743, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 2, 744, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;

-- 为团队负责人角色(3)分配团队相关审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 3, 740, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 合同审批
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 3, 742, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 利冲审批
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 3, 744, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 结案审批

-- 为财务角色(5)分配财务相关审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 5, 743, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 费用审批

-- 为行政角色(8)分配行政相关审批权限
INSERT INTO public.sys_role_menu VALUES (DEFAULT, 8, 741, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;  -- 用印审批

-- 说明：
-- approval:contract:approve - 合同审批权限
-- approval:seal:approve - 用印申请审批权限
-- approval:conflict:approve - 利冲检查审批权限
-- approval:expense:approve - 费用报销审批权限
-- approval:matter-close:approve - 项目结案审批权限
--
-- 注意：审批权限是「能否审批」的权限，不是「能否发起」的权限
-- 发起审批的权限由各业务模块自己的权限控制
--
-- 角色权限分配逻辑：
-- - 管理员、主任：所有审批权限
-- - 团队负责人：合同、利冲、结案（与业务相关）
-- - 财务：费用审批
-- - 行政：用印审批


-- =====================================================
-- 合并自: 31-dossier-auto-archive.sql
-- =====================================================

-- =====================================================
-- 律师事务所管理系统 - 卷宗自动归档功能
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-11
-- 描述: 为文档表添加来源追踪字段，支持卷宗材料自动归档
-- =====================================================

-- 添加文档来源类型字段
ALTER TABLE public.doc_document ADD COLUMN IF NOT EXISTS source_type VARCHAR(20);
COMMENT ON COLUMN public.doc_document.source_type IS '文档来源类型: SYSTEM_GENERATED-系统自动生成, SYSTEM_LINKED-系统自动关联, USER_UPLOADED-用户上传, SIGNED_VERSION-签字版本';

-- 添加来源数据ID字段
ALTER TABLE public.doc_document ADD COLUMN IF NOT EXISTS source_id BIGINT;
COMMENT ON COLUMN public.doc_document.source_id IS '来源数据ID（如合同ID、审批ID等）';

-- 添加来源模块字段
ALTER TABLE public.doc_document ADD COLUMN IF NOT EXISTS source_module VARCHAR(50);
COMMENT ON COLUMN public.doc_document.source_module IS '来源模块: CONTRACT-合同, APPROVAL-审批, INVOICE-发票, MATTER-项目';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_doc_document_source_type ON public.doc_document (source_type);
CREATE INDEX IF NOT EXISTS idx_doc_document_source ON public.doc_document (source_module, source_id);

-- 更新已有文档的 source_type 为 USER_UPLOADED（向后兼容）
UPDATE public.doc_document SET source_type = 'USER_UPLOADED' WHERE source_type IS NULL;


-- =====================================================
-- 完成标记
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '数据库增强完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已添加:';
    RAISE NOTICE '  - version 字段（乐观锁）';
    RAISE NOTICE '  - 字典管理菜单';
    RAISE NOTICE '  - 报表权限细化';
    RAISE NOTICE '  - 审批权限细化';
    RAISE NOTICE '  - 卷宗自动归档功能';
END $$;

