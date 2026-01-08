-- 051-archive-enhancement.sql
-- 档案管理功能增强：归档流程优化、入库审批、档案迁移
-- 执行日期: 2026-01-08

-- ============================================================
-- 1. 更新档案状态枚举（添加新状态）
-- ============================================================
-- 原状态：PENDING(待入库), STORED(已入库), BORROWED(借出), DESTROYED(已销毁)
-- 新状态：PENDING(待入库), PENDING_STORE(待入库审批), STORED(已入库), 
--        BORROWED(借出), PENDING_MIGRATE(待迁移审批), MIGRATED(已迁移)

-- 添加档案迁移相关字段
ALTER TABLE archive ADD COLUMN IF NOT EXISTS migrate_date DATE;
ALTER TABLE archive ADD COLUMN IF NOT EXISTS migrate_reason TEXT;
ALTER TABLE archive ADD COLUMN IF NOT EXISTS migrate_approver_id BIGINT;
ALTER TABLE archive ADD COLUMN IF NOT EXISTS migrate_target TEXT; -- 迁移目标系统/档案馆
ALTER TABLE archive ADD COLUMN IF NOT EXISTS files_deleted BOOLEAN DEFAULT FALSE; -- 文件是否已删除

-- 添加归档数据快照字段（JSON格式，存储归档时收集的所有项目相关数据）
ALTER TABLE archive ADD COLUMN IF NOT EXISTS archive_snapshot TEXT;

-- 添加电子档案包路径
ALTER TABLE archive ADD COLUMN IF NOT EXISTS electronic_package_path TEXT;

-- ============================================================
-- 2. 更新卷宗目录模板项，确保有必填标识
-- ============================================================
-- dossier_template_item 表已有 required 字段

-- 设置民事案件模板的必填项
UPDATE dossier_template_item SET required = true 
WHERE template_id = 2 AND name IN ('2.授权委托书', '3.委托合同', '11.结案（归档）审批表');

-- 设置刑事案件模板的必填项
UPDATE dossier_template_item SET required = true 
WHERE template_id = 1 AND name LIKE '%委托%';

-- ============================================================
-- 3. 新增审批类型：档案入库审批、档案迁移审批
-- ============================================================
-- 审批类型在 workbench_approval.business_type 中定义
-- 新增类型：ARCHIVE_STORE（档案入库审批）, ARCHIVE_MIGRATE（档案迁移审批）

-- ============================================================
-- 4. 创建归档数据收集配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS archive_data_source (
    id BIGSERIAL PRIMARY KEY,
    source_name VARCHAR(100) NOT NULL,           -- 数据源名称（如：客户信息、合同信息）
    source_table VARCHAR(100) NOT NULL,          -- 源表名
    source_type VARCHAR(50) NOT NULL,            -- 类型：DOCUMENT(文档), GENERATED(生成), REFERENCE(引用)
    dossier_folder VARCHAR(200),                 -- 对应卷宗目录（如：/委托合同/）
    is_enabled BOOLEAN DEFAULT TRUE,             -- 是否启用
    is_required BOOLEAN DEFAULT FALSE,           -- 是否必须有数据
    sort_order INTEGER DEFAULT 0,                -- 排序
    description TEXT,                            -- 说明
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- 插入数据源配置
INSERT INTO archive_data_source (source_name, source_table, source_type, dossier_folder, is_enabled, is_required, sort_order, description) VALUES
-- 核心信息
('项目基本信息', 'matter', 'GENERATED', '/项目信息/', true, true, 1, '生成项目信息表PDF'),
('客户信息', 'matter_client', 'GENERATED', '/客户信息/', true, true, 2, '生成客户信息表PDF'),
('团队成员', 'matter_participant', 'GENERATED', '/项目信息/', true, false, 3, '生成团队成员表PDF'),

-- 合同与费用
('委托合同', 'finance_contract', 'DOCUMENT', '/委托合同/', true, true, 10, '关联合同文件'),
('收费记录', 'finance_fee', 'GENERATED', '/收费发票/', true, false, 11, '生成收费明细表'),
('付款记录', 'finance_payment', 'GENERATED', '/收费发票/', true, false, 12, '生成付款明细表'),
('费用报销', 'finance_expense', 'GENERATED', '/其他材料/', true, false, 13, '生成费用报销表'),
('提成记录', 'finance_commission', 'GENERATED', '/其他材料/', true, false, 14, '生成提成明细表'),

-- 工时与日志
('工时记录', 'timesheet', 'GENERATED', '/工作日志/', true, false, 20, '生成工作日志'),
('工时汇总', 'timesheet_summary', 'GENERATED', '/工作日志/', true, false, 21, '生成工时汇总表'),

-- 文档与证据
('项目文档', 'doc_document', 'DOCUMENT', NULL, true, false, 30, '按卷宗目录整理'),
('证据材料', 'evidence', 'DOCUMENT', '/证据材料/', true, false, 31, '证据材料文件'),
('证据清单', 'evidence_list', 'GENERATED', '/证据材料/', true, false, 32, '生成证据清单'),

-- 审批与用印
('审批记录', 'workbench_approval', 'GENERATED', '/审批记录/', true, false, 40, '生成审批记录表'),
('用印申请', 'seal_application', 'GENERATED', '/用印记录/', true, false, 41, '生成用印记录表'),
('律师函', 'letter_application', 'DOCUMENT', '/往来函件/', true, false, 42, '律师函文件'),

-- 其他信息
('利冲检查', 'crm_conflict_check', 'GENERATED', '/利冲检查/', true, false, 50, '生成利冲检查表'),
('期限提醒', 'matter_deadline', 'GENERATED', '/其他材料/', true, false, 51, '生成期限记录表'),
('任务记录', 'task', 'GENERATED', '/其他材料/', true, false, 52, '生成任务完成表'),
('日程安排', 'schedule', 'GENERATED', '/其他材料/', true, false, 53, '生成日程记录表'),
('质量检查', 'quality_check', 'GENERATED', '/质量检查/', true, false, 54, '生成质量检查表'),
('风险预警', 'risk_warning', 'GENERATED', '/风险记录/', true, false, 55, '生成风险记录表')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 5. 添加入库审批菜单权限
-- ============================================================
-- 档案入库审批按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (721, 71, '入库审批', NULL, NULL, NULL, NULL, 'BUTTON', 'archive:store:approve', 2, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO UPDATE SET
    name = '入库审批',
    permission = 'archive:store:approve',
    updated_at = NOW();

-- 档案迁移申请按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (722, 71, '申请迁移', NULL, NULL, NULL, NULL, 'BUTTON', 'archive:migrate:apply', 3, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO UPDATE SET
    name = '申请迁移',
    permission = 'archive:migrate:apply',
    updated_at = NOW();

-- 档案迁移审批按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES (723, 71, '迁移审批', NULL, NULL, NULL, NULL, 'BUTTON', 'archive:migrate:approve', 4, true, 'ENABLED', false, false, NOW(), NOW(), false)
ON CONFLICT (id) DO UPDATE SET
    name = '迁移审批',
    permission = 'archive:migrate:approve',
    updated_at = NOW();

-- 为主任角色添加审批权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 721, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 722, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (2, 723, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 为管理员角色添加审批权限
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 721, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 722, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id, created_at) VALUES (1, 723, NOW()) ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ============================================================
-- 6. 更新现有"销毁"相关权限名称为"迁移"
-- ============================================================
UPDATE sys_menu SET 
    name = '申请迁移',
    permission = 'archive:migrate:apply'
WHERE permission = 'archive:destroy';

-- ============================================================
-- 7. 验证结果
-- ============================================================
DO $$
DECLARE
    col_count INTEGER;
    menu_count INTEGER;
    source_count INTEGER;
BEGIN
    -- 检查新增字段
    SELECT COUNT(*) INTO col_count FROM information_schema.columns 
    WHERE table_name = 'archive' AND column_name IN ('migrate_date', 'archive_snapshot', 'electronic_package_path');
    RAISE NOTICE '档案表新增字段数: %', col_count;
    
    -- 检查新增菜单
    SELECT COUNT(*) INTO menu_count FROM sys_menu WHERE permission LIKE 'archive:%';
    RAISE NOTICE '档案相关菜单权限数: %', menu_count;
    
    -- 检查数据源配置
    SELECT COUNT(*) INTO source_count FROM archive_data_source WHERE is_enabled = true;
    RAISE NOTICE '已启用的归档数据源数: %', source_count;
END $$;

