-- =====================================================
-- 系统扩展模块数据库表结构
-- 包含：消息通知、数据字典、操作日志
-- =====================================================

-- -----------------------------------------------------
-- 1. 系统通知表 (sys_notification)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,                      -- 通知标题
    content TEXT NOT NULL,                            -- 通知内容
    type VARCHAR(30) NOT NULL,                        -- 类型: SYSTEM系统/APPROVAL审批/TASK任务/REMINDER提醒
    sender_id BIGINT,                                 -- 发送者ID(系统通知为空)
    receiver_id BIGINT NOT NULL,                      -- 接收者ID
    is_read BOOLEAN DEFAULT FALSE,                    -- 是否已读
    read_at TIMESTAMP,                                -- 阅读时间
    business_type VARCHAR(50),                        -- 关联业务类型
    business_id BIGINT,                               -- 关联业务ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_notification_receiver ON sys_notification(receiver_id);
CREATE INDEX idx_notification_type ON sys_notification(type);
CREATE INDEX idx_notification_read ON sys_notification(is_read);
CREATE INDEX idx_notification_created ON sys_notification(created_at);

COMMENT ON TABLE sys_notification IS '系统通知表';
COMMENT ON COLUMN sys_notification.type IS '类型: SYSTEM系统/APPROVAL审批/TASK任务/REMINDER提醒';

-- -----------------------------------------------------
-- 2. 数据字典类型表 (sys_dict_type)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                       -- 字典名称
    code VARCHAR(50) NOT NULL UNIQUE,                 -- 字典编码
    description VARCHAR(500),                         -- 描述
    status VARCHAR(20) DEFAULT 'ENABLED',             -- 状态: ENABLED启用/DISABLED禁用
    is_system BOOLEAN DEFAULT FALSE,                  -- 是否系统内置
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_dict_type IS '数据字典类型表';

-- 初始化字典类型
INSERT INTO sys_dict_type (name, code, description, is_system) VALUES
('客户类型', 'client_type', '客户类型：个人/企业/政府机关', TRUE),
('客户级别', 'client_level', '客户级别：A/B/C', TRUE),
('案件类型', 'matter_type', '案件业务类型', TRUE),
('案件状态', 'matter_status', '案件状态', TRUE),
('合同状态', 'contract_status', '合同状态', TRUE),
('收费方式', 'fee_type', '收费方式：固定/计时/风险/混合', TRUE),
('证据类型', 'evidence_type', '证据类型', TRUE),
('审批状态', 'approval_status', '审批状态', TRUE),
('请假类型', 'leave_type', '请假类型', TRUE);

-- -----------------------------------------------------
-- 3. 数据字典项表 (sys_dict_item)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGSERIAL PRIMARY KEY,
    dict_type_id BIGINT NOT NULL,                     -- 字典类型ID
    label VARCHAR(100) NOT NULL,                      -- 显示标签
    value VARCHAR(100) NOT NULL,                      -- 字典值
    description VARCHAR(500),                         -- 描述
    sort_order INT DEFAULT 0,                         -- 排序
    status VARCHAR(20) DEFAULT 'ENABLED',             -- 状态
    css_class VARCHAR(50),                            -- 样式类名
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_dict_item UNIQUE (dict_type_id, value)
);

CREATE INDEX idx_dict_item_type ON sys_dict_item(dict_type_id);

COMMENT ON TABLE sys_dict_item IS '数据字典项表';

-- 初始化字典项
INSERT INTO sys_dict_item (dict_type_id, label, value, sort_order) VALUES
-- 客户类型
((SELECT id FROM sys_dict_type WHERE code = 'client_type'), '个人', 'INDIVIDUAL', 1),
((SELECT id FROM sys_dict_type WHERE code = 'client_type'), '企业', 'ENTERPRISE', 2),
((SELECT id FROM sys_dict_type WHERE code = 'client_type'), '政府机关', 'GOVERNMENT', 3),
((SELECT id FROM sys_dict_type WHERE code = 'client_type'), '其他', 'OTHER', 4),
-- 客户级别
((SELECT id FROM sys_dict_type WHERE code = 'client_level'), '重要客户', 'A', 1),
((SELECT id FROM sys_dict_type WHERE code = 'client_level'), '普通客户', 'B', 2),
((SELECT id FROM sys_dict_type WHERE code = 'client_level'), '一般客户', 'C', 3),
-- 案件类型
((SELECT id FROM sys_dict_type WHERE code = 'matter_type'), '民事诉讼', 'CIVIL_LITIGATION', 1),
((SELECT id FROM sys_dict_type WHERE code = 'matter_type'), '刑事辩护', 'CRIMINAL_DEFENSE', 2),
((SELECT id FROM sys_dict_type WHERE code = 'matter_type'), '行政诉讼', 'ADMINISTRATIVE', 3),
((SELECT id FROM sys_dict_type WHERE code = 'matter_type'), '非诉业务', 'NON_LITIGATION', 4),
((SELECT id FROM sys_dict_type WHERE code = 'matter_type'), '常年法律顾问', 'LEGAL_COUNSEL', 5),
-- 案件状态
((SELECT id FROM sys_dict_type WHERE code = 'matter_status'), '待立案', 'PENDING', 1),
((SELECT id FROM sys_dict_type WHERE code = 'matter_status'), '进行中', 'IN_PROGRESS', 2),
((SELECT id FROM sys_dict_type WHERE code = 'matter_status'), '已结案', 'CLOSED', 3),
((SELECT id FROM sys_dict_type WHERE code = 'matter_status'), '已归档', 'ARCHIVED', 4),
-- 收费方式
((SELECT id FROM sys_dict_type WHERE code = 'fee_type'), '固定收费', 'FIXED', 1),
((SELECT id FROM sys_dict_type WHERE code = 'fee_type'), '计时收费', 'HOURLY', 2),
((SELECT id FROM sys_dict_type WHERE code = 'fee_type'), '风险代理', 'CONTINGENCY', 3),
((SELECT id FROM sys_dict_type WHERE code = 'fee_type'), '混合收费', 'MIXED', 4),
-- 证据类型
((SELECT id FROM sys_dict_type WHERE code = 'evidence_type'), '书证', 'DOCUMENTARY', 1),
((SELECT id FROM sys_dict_type WHERE code = 'evidence_type'), '物证', 'PHYSICAL', 2),
((SELECT id FROM sys_dict_type WHERE code = 'evidence_type'), '视听资料', 'AUDIOVISUAL', 3),
((SELECT id FROM sys_dict_type WHERE code = 'evidence_type'), '电子数据', 'ELECTRONIC', 4),
((SELECT id FROM sys_dict_type WHERE code = 'evidence_type'), '证人证言', 'TESTIMONY', 5),
((SELECT id FROM sys_dict_type WHERE code = 'evidence_type'), '鉴定意见', 'EXPERT_OPINION', 6);

-- -----------------------------------------------------
-- 4. 操作日志表 (sys_operation_log)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,                                   -- 操作用户ID
    user_name VARCHAR(50),                            -- 操作用户名
    module VARCHAR(50),                               -- 操作模块
    operation_type VARCHAR(50),                       -- 操作类型
    description VARCHAR(500),                         -- 操作描述
    method VARCHAR(200),                              -- 请求方法
    request_url VARCHAR(500),                         -- 请求URL
    request_method VARCHAR(10),                       -- 请求方式
    request_params TEXT,                              -- 请求参数
    response_result TEXT,                             -- 响应结果
    ip_address VARCHAR(50),                           -- IP地址
    user_agent VARCHAR(2000),                         -- 用户代理
    execution_time BIGINT,                            -- 执行时长(ms)
    status VARCHAR(20) DEFAULT 'SUCCESS',             -- 状态: SUCCESS成功/FAIL失败
    error_message TEXT,                               -- 错误信息
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_operation_log_user ON sys_operation_log(user_id);
CREATE INDEX idx_operation_log_module ON sys_operation_log(module);
CREATE INDEX idx_operation_log_created ON sys_operation_log(created_at);

COMMENT ON TABLE sys_operation_log IS '操作日志表';

-- -----------------------------------------------------
-- 5. 系统公告表 (sys_announcement)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_announcement (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,                      -- 公告标题
    content TEXT NOT NULL,                            -- 公告内容
    type VARCHAR(30) DEFAULT 'NOTICE',                -- 类型: NOTICE通知/ANNOUNCEMENT公告/WARNING警告
    priority INT DEFAULT 0,                           -- 优先级(越大越高)
    status VARCHAR(20) DEFAULT 'DRAFT',               -- 状态: DRAFT草稿/PUBLISHED已发布/EXPIRED已过期
    publish_time TIMESTAMP,                           -- 发布时间
    expire_time TIMESTAMP,                            -- 过期时间
    is_top BOOLEAN DEFAULT FALSE,                     -- 是否置顶
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_announcement_status ON sys_announcement(status);
CREATE INDEX idx_announcement_publish ON sys_announcement(publish_time);

COMMENT ON TABLE sys_announcement IS '系统公告表';
