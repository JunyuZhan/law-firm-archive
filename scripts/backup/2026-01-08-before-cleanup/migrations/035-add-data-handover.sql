-- =============================================
-- 用户数据交接功能
-- 创建时间：2026-01-06
-- 功能：支持用户离职交接、项目移交、客户移交等场景
-- =============================================

-- 1. 数据交接记录表
CREATE TABLE IF NOT EXISTS sys_data_handover (
    id BIGSERIAL PRIMARY KEY,
    handover_no VARCHAR(50) NOT NULL UNIQUE,  -- 交接单号
    from_user_id BIGINT NOT NULL REFERENCES sys_user(id),  -- 移交人
    from_username VARCHAR(50) NOT NULL,
    to_user_id BIGINT NOT NULL REFERENCES sys_user(id),  -- 接收人
    to_username VARCHAR(50) NOT NULL,
    handover_type VARCHAR(20) NOT NULL,  -- RESIGNATION-离职交接, PROJECT-项目移交, CLIENT-客户移交, LEAD-案源移交
    handover_reason TEXT,  -- 交接原因
    status VARCHAR(20) DEFAULT 'PENDING_APPROVAL',  -- PENDING_APPROVAL-待审批, APPROVED-审批通过待执行, REJECTED-已拒绝, CONFIRMED-已确认, CANCELLED-已取消
    approval_id BIGINT,  -- 关联的审批记录ID
    
    -- 统计信息
    matter_count INT DEFAULT 0,  -- 移交项目数
    client_count INT DEFAULT 0,  -- 移交客户数
    lead_count INT DEFAULT 0,  -- 移交案源数
    task_count INT DEFAULT 0,  -- 移交任务数
    
    -- 审批信息
    submitted_by BIGINT REFERENCES sys_user(id),  -- 提交人
    submitted_at TIMESTAMP,  -- 提交时间
    confirmed_by BIGINT REFERENCES sys_user(id),  -- 确认人
    confirmed_at TIMESTAMP,  -- 确认时间
    
    remark TEXT,
    deleted BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_handover_from_user ON sys_data_handover(from_user_id);
CREATE INDEX IF NOT EXISTS idx_handover_to_user ON sys_data_handover(to_user_id);
CREATE INDEX IF NOT EXISTS idx_handover_status ON sys_data_handover(status);
CREATE INDEX IF NOT EXISTS idx_handover_type ON sys_data_handover(handover_type);
CREATE INDEX IF NOT EXISTS idx_handover_created_at ON sys_data_handover(created_at);

-- 注释
COMMENT ON TABLE sys_data_handover IS '数据交接记录表';
COMMENT ON COLUMN sys_data_handover.handover_no IS '交接单号';
COMMENT ON COLUMN sys_data_handover.from_user_id IS '移交人ID';
COMMENT ON COLUMN sys_data_handover.to_user_id IS '接收人ID';
COMMENT ON COLUMN sys_data_handover.handover_type IS '交接类型：RESIGNATION-离职交接, PROJECT-项目移交, CLIENT-客户移交, LEAD-案源移交';
COMMENT ON COLUMN sys_data_handover.status IS '状态：PENDING-待确认, CONFIRMED-已确认, CANCELLED-已取消';

-- 2. 数据交接明细表
CREATE TABLE IF NOT EXISTS sys_data_handover_detail (
    id BIGSERIAL PRIMARY KEY,
    handover_id BIGINT NOT NULL REFERENCES sys_data_handover(id),
    data_type VARCHAR(30) NOT NULL,  -- MATTER, CLIENT, LEAD, TASK, MATTER_PARTICIPANT, CONTRACT_PARTICIPANT
    data_id BIGINT NOT NULL,  -- 数据ID
    data_no VARCHAR(50),  -- 数据编号
    data_name VARCHAR(200),  -- 数据名称
    field_name VARCHAR(50) NOT NULL,  -- 变更字段：lead_lawyer_id, responsible_lawyer_id 等
    old_value VARCHAR(100),  -- 原值（用户ID）
    new_value VARCHAR(100),  -- 新值（用户ID）
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING-待处理, DONE-已完成, FAILED-失败
    error_message TEXT,  -- 错误信息
    executed_at TIMESTAMP,  -- 执行时间
    deleted BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_handover_detail_handover ON sys_data_handover_detail(handover_id);
CREATE INDEX IF NOT EXISTS idx_handover_detail_type ON sys_data_handover_detail(data_type);
CREATE INDEX IF NOT EXISTS idx_handover_detail_data ON sys_data_handover_detail(data_type, data_id);
CREATE INDEX IF NOT EXISTS idx_handover_detail_status ON sys_data_handover_detail(status);

-- 注释
COMMENT ON TABLE sys_data_handover_detail IS '数据交接明细表';
COMMENT ON COLUMN sys_data_handover_detail.data_type IS '数据类型：MATTER, CLIENT, LEAD, TASK, MATTER_PARTICIPANT, CONTRACT_PARTICIPANT';
COMMENT ON COLUMN sys_data_handover_detail.field_name IS '变更字段名';
COMMENT ON COLUMN sys_data_handover_detail.status IS '状态：PENDING-待处理, DONE-已完成, FAILED-失败';

-- 3. 添加菜单（数据交接管理 - 独立顶级菜单）
-- 图标使用 ant-design:swap-outlined 格式（与审批中心一致）
INSERT INTO sys_menu (parent_id, menu_type, name, path, component, permission, icon, sort_order, status, deleted)
SELECT 
    0,  -- 顶级菜单，不属于系统管理
    'MENU',
    '数据交接',
    '/data-handover',
    'data-handover/index',
    NULL,
    'ant-design:swap-outlined',  -- 使用 iconify 格式
    85,  -- 排序在其他业务菜单之间
    'ENABLED',
    FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/data-handover');

-- 更新现有菜单（如果是旧路径）
UPDATE sys_menu SET 
    parent_id = 0,
    path = '/data-handover',
    component = 'data-handover/index',
    icon = 'ant-design:swap-outlined',
    sort_order = 85
WHERE path = '/system/data-handover';

-- 权限按钮
INSERT INTO sys_menu (parent_id, menu_type, name, path, component, permission, icon, sort_order, status, deleted)
SELECT 
    (SELECT id FROM sys_menu WHERE path = '/data-handover' LIMIT 1),
    'BUTTON',
    '查询交接',
    NULL,
    NULL,
    'sys:handover:list',
    NULL,
    0,
    'ENABLED',
    FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'sys:handover:list');

INSERT INTO sys_menu (parent_id, menu_type, name, path, component, permission, icon, sort_order, status, deleted)
SELECT 
    (SELECT id FROM sys_menu WHERE path = '/data-handover' LIMIT 1),
    'BUTTON',
    '创建交接',
    NULL,
    NULL,
    'sys:handover:create',
    NULL,
    1,
    'ENABLED',
    FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'sys:handover:create');

INSERT INTO sys_menu (parent_id, menu_type, name, path, component, permission, icon, sort_order, status, deleted)
SELECT 
    (SELECT id FROM sys_menu WHERE path = '/data-handover' LIMIT 1),
    'BUTTON',
    '查看交接',
    NULL,
    NULL,
    'sys:handover:view',
    NULL,
    2,
    'ENABLED',
    FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'sys:handover:view');

INSERT INTO sys_menu (parent_id, menu_type, name, path, component, permission, icon, sort_order, status, deleted)
SELECT 
    (SELECT id FROM sys_menu WHERE path = '/data-handover' LIMIT 1),
    'BUTTON',
    '确认交接',
    NULL,
    NULL,
    'sys:handover:confirm',
    NULL,
    3,
    'ENABLED',
    FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'sys:handover:confirm');

INSERT INTO sys_menu (parent_id, menu_type, name, path, component, permission, icon, sort_order, status, deleted)
SELECT 
    (SELECT id FROM sys_menu WHERE path = '/data-handover' LIMIT 1),
    'BUTTON',
    '取消交接',
    NULL,
    NULL,
    'sys:handover:cancel',
    NULL,
    4,
    'ENABLED',
    FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'sys:handover:cancel');

-- 4. 为所有角色分配数据交接权限（所有人都可发起，审批控制权限）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.deleted = false
  AND (m.path = '/data-handover' OR m.permission IN ('sys:handover:list', 'sys:handover:create', 'sys:handover:view', 'sys:handover:confirm', 'sys:handover:cancel'))
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 5. 为所有角色分配用户列表查询权限（用于数据交接选择接收人）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.deleted = false AND m.permission = 'sys:user:list'
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 完成
SELECT '数据交接功能表结构创建完成' AS result;
