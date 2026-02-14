-- =====================================================
-- 档案管理系统数据库设计
-- 基于国家档案标准 DA/T 104-2024, GB/T 39784-2021
-- 以电子档案管理为核心
-- =====================================================

-- =====================================================
-- 一、系统基础表
-- =====================================================

-- 1.1 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    department VARCHAR(100),
    -- 用户类型：SYSTEM_ADMIN-系统管理员, SECURITY_ADMIN-安全保密员, AUDIT_ADMIN-安全审计员, ARCHIVIST-档案员, USER-普通用户
    user_type VARCHAR(30) DEFAULT 'USER',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),                   -- 最后登录IP地址
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.user_type IS '用户类型（三员分立）: SYSTEM_ADMIN, SECURITY_ADMIN, AUDIT_ADMIN, ARCHIVIST, USER';
COMMENT ON COLUMN sys_user.last_login_ip IS '最后登录IP地址';

-- 1.2 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- 1.3 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE(user_id, role_id)
);

-- 1.4 API Key管理表（用于Open API认证）
CREATE TABLE IF NOT EXISTS sys_api_key (
    id BIGSERIAL PRIMARY KEY,
    api_key VARCHAR(64) NOT NULL UNIQUE,
    source_name VARCHAR(100) NOT NULL,           -- 来源名称（如：律所管理系统）
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',         -- ACTIVE, DISABLED, EXPIRED
    expire_at TIMESTAMP,                          -- 过期时间（NULL表示永不过期）
    last_used_at TIMESTAMP,
    total_calls BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_api_key IS 'API Key管理表 - 用于Open API接口认证';
CREATE INDEX IF NOT EXISTS idx_api_key_key ON sys_api_key(api_key);
CREATE INDEX IF NOT EXISTS idx_api_key_status ON sys_api_key(status);

-- 1.5 安全审计日志表
CREATE TABLE IF NOT EXISTS sys_security_audit (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,             -- LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, PASSWORD_CHANGE, ACCOUNT_LOCKED, etc.
    user_id BIGINT,
    username VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent TEXT,
    details JSONB,                                -- 详细信息（JSON格式）
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_security_audit IS '安全审计日志表 - 记录登录、密码修改等安全事件';
CREATE INDEX IF NOT EXISTS idx_security_audit_event_type ON sys_security_audit(event_type);
CREATE INDEX IF NOT EXISTS idx_security_audit_user_id ON sys_security_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_security_audit_ip ON sys_security_audit(ip_address);
CREATE INDEX IF NOT EXISTS idx_security_audit_time ON sys_security_audit(event_time);

-- 1.6 IP黑名单表
CREATE TABLE IF NOT EXISTS sys_ip_blacklist (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    ip_range VARCHAR(45),                        -- IP范围（如：192.168.1.0/24）
    reason TEXT,
    blocked_until TIMESTAMP,                      -- 解封时间（NULL表示永久封禁）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT
);

COMMENT ON TABLE sys_ip_blacklist IS 'IP黑名单表 - 用于封禁恶意IP';
CREATE INDEX IF NOT EXISTS idx_ip_blacklist_ip ON sys_ip_blacklist(ip_address);

-- =====================================================
-- 二、档案分类体系表
-- =====================================================

-- 2.1 全宗表（Fonds）
CREATE TABLE IF NOT EXISTS arc_fonds (
    id BIGSERIAL PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL UNIQUE,       -- 全宗号
    fonds_name VARCHAR(200) NOT NULL,            -- 全宗名称
    fonds_type VARCHAR(50),                      -- 全宗类型
    description TEXT,                            -- 说明
    contact_person VARCHAR(100),                 -- 联系人
    contact_phone VARCHAR(50),                   -- 联系电话
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE arc_fonds IS '全宗表 - 档案的形成单位';

-- 2.2 档案分类表（Category）
CREATE TABLE IF NOT EXISTS arc_category (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES arc_category(id),
    category_code VARCHAR(50) NOT NULL,          -- 分类号
    category_name VARCHAR(200) NOT NULL,         -- 分类名称
    -- 档案门类：DOCUMENT-文书档案, SCIENCE-科技档案, ACCOUNTING-会计档案, 
    --          PERSONNEL-人事档案, SPECIAL-专业档案, AUDIOVISUAL-声像档案
    archive_type VARCHAR(50),
    level INTEGER DEFAULT 1,                     -- 层级
    sort_order INTEGER DEFAULT 0,                -- 排序
    retention_period VARCHAR(20),                -- 默认保管期限
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    full_path VARCHAR(500),                      -- 完整路径（如：01/01-01/01-01-01）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(parent_id, category_code)
);

COMMENT ON TABLE arc_category IS '档案分类表 - 树形分类体系';

-- 2.3 保管期限表
CREATE TABLE IF NOT EXISTS arc_retention_period (
    id BIGSERIAL PRIMARY KEY,
    period_code VARCHAR(20) NOT NULL UNIQUE,     -- 期限代码：PERMANENT, Y30, Y15, Y10, Y5
    period_name VARCHAR(50) NOT NULL,            -- 期限名称：永久、30年、15年、10年、5年
    period_years INTEGER,                        -- 年数（永久为NULL）
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE arc_retention_period IS '保管期限表';

-- =====================================================
-- 三、档案核心表
-- =====================================================

-- 3.1 电子档案表（核心表）
CREATE TABLE IF NOT EXISTS arc_archive (
    id BIGSERIAL PRIMARY KEY,
    
    -- ===== 档案标识 =====
    archive_no VARCHAR(100) NOT NULL UNIQUE,     -- 档案号（系统生成，唯一标识）
    
    -- ===== 分类信息 =====
    fonds_id BIGINT REFERENCES arc_fonds(id),    -- 全宗ID
    fonds_no VARCHAR(50),                        -- 全宗号（冗余）
    category_id BIGINT REFERENCES arc_category(id), -- 分类ID
    category_code VARCHAR(50),                   -- 分类号（冗余）
    archive_type VARCHAR(50) NOT NULL,           -- 档案门类
    
    -- ===== 基本信息 =====
    title VARCHAR(500) NOT NULL,                 -- 题名
    file_no VARCHAR(100),                        -- 文件编号（原文件号）
    responsibility VARCHAR(200),                 -- 责任者
    archive_date DATE,                           -- 归档日期
    document_date DATE,                          -- 文件日期
    page_count INTEGER,                          -- 页数
    pieces_count INTEGER DEFAULT 1,              -- 件数
    
    -- ===== 保管信息 =====
    retention_period VARCHAR(20) NOT NULL,       -- 保管期限代码
    retention_expire_date DATE,                  -- 保管到期日期
    security_level VARCHAR(20) DEFAULT 'INTERNAL', -- 密级：PUBLIC-公开, INTERNAL-内部, SECRET-秘密, CONFIDENTIAL-机密
    security_expire_date DATE,                   -- 解密日期
    
    -- ===== 来源信息 =====
    source_type VARCHAR(50) NOT NULL,            -- 来源类型：LAW_FIRM-律所系统, MANUAL-手动, IMPORT-导入, TRANSFER-移交
    source_system VARCHAR(100),                  -- 来源系统名称
    source_id VARCHAR(100),                      -- 来源系统ID
    source_no VARCHAR(100),                      -- 来源系统编号
    callback_url VARCHAR(500),                   -- 回调URL（处理完成后通知）
    
    -- ===== 业务关联（律所业务）=====
    case_no VARCHAR(100),                        -- 案件编号
    case_name VARCHAR(500),                      -- 案件名称
    client_name VARCHAR(200),                    -- 委托人
    lawyer_name VARCHAR(100),                    -- 主办律师
    case_close_date DATE,                        -- 结案日期
    
    -- ===== 存储信息 =====
    has_electronic BOOLEAN DEFAULT TRUE,         -- 是否有电子文件
    storage_location VARCHAR(200),               -- 存储位置描述
    total_file_size BIGINT DEFAULT 0,            -- 总文件大小（字节）
    file_count INTEGER DEFAULT 0,                -- 文件数量
    
    -- ===== 状态信息 =====
    -- DRAFT-草稿, RECEIVED-已接收, CATALOGING-整理中, STORED-已归档, 
    -- BORROWED-借出中, APPRAISAL-鉴定中, DESTROYED-已销毁
    status VARCHAR(30) DEFAULT 'RECEIVED',
    
    -- ===== 操作信息 =====
    received_at TIMESTAMP,                       -- 接收时间
    received_by BIGINT,                          -- 接收人
    cataloged_at TIMESTAMP,                      -- 著录时间
    cataloged_by BIGINT,                         -- 著录人
    archived_at TIMESTAMP,                       -- 归档时间（正式入库）
    archived_by BIGINT,                          -- 归档人
    
    -- ===== 扩展信息 =====
    keywords TEXT,                               -- 关键词（逗号分隔）
    abstract TEXT,                               -- 摘要
    remarks TEXT,                                -- 备注
    extra_data JSONB,                            -- 扩展数据（JSON格式）
    
    -- ===== 系统字段 =====
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE arc_archive IS '电子档案主表';
COMMENT ON COLUMN arc_archive.archive_no IS '档案号 - 系统生成的唯一标识';
COMMENT ON COLUMN arc_archive.security_level IS '密级：PUBLIC-公开, INTERNAL-内部, SECRET-秘密, CONFIDENTIAL-机密';
COMMENT ON COLUMN arc_archive.status IS '状态：DRAFT, RECEIVED, CATALOGING, STORED, BORROWED, APPRAISAL, DESTROYED';

-- 3.2 电子文件表
CREATE TABLE IF NOT EXISTS arc_digital_file (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL REFERENCES arc_archive(id),
    
    -- ===== 文件标识 =====
    file_no VARCHAR(50),                         -- 文件序号
    
    -- ===== 文件信息 =====
    file_name VARCHAR(500) NOT NULL,             -- 文件名
    original_name VARCHAR(500),                  -- 原始文件名
    file_extension VARCHAR(20),                  -- 扩展名
    mime_type VARCHAR(100),                      -- MIME类型
    file_size BIGINT NOT NULL,                   -- 文件大小（字节）
    
    -- ===== 存储信息 =====
    storage_path VARCHAR(1000) NOT NULL,         -- 存储路径（MinIO对象路径）
    storage_bucket VARCHAR(100),                 -- 存储桶
    
    -- ===== 格式信息 =====
    format_name VARCHAR(100),                    -- 格式名称（如PDF、Word）
    format_version VARCHAR(50),                  -- 格式版本
    is_long_term_format BOOLEAN DEFAULT FALSE,   -- 是否长期保存格式（PDF/A, OFD）
    converted_path VARCHAR(1000),                -- 转换后的长期保存格式路径
    
    -- ===== 完整性校验 =====
    hash_algorithm VARCHAR(20) DEFAULT 'SHA256', -- 哈希算法
    hash_value VARCHAR(128),                     -- 哈希值
    
    -- ===== 内容信息 =====
    ocr_status VARCHAR(20),                      -- OCR状态：NONE, PENDING, COMPLETED, FAILED
    ocr_content TEXT,                            -- OCR识别内容
    
    -- ===== 预览信息 =====
    has_preview BOOLEAN DEFAULT FALSE,           -- 是否有预览
    preview_path VARCHAR(1000),                  -- 预览文件路径
    thumbnail_path VARCHAR(1000),                -- 缩略图路径
    
    -- ===== 分类信息 =====
    file_category VARCHAR(50) DEFAULT 'MAIN',    -- 文件分类：MAIN-正文, ATTACHMENT-附件, COVER-封面, CATALOG-目录
    sort_order INTEGER DEFAULT 0,                -- 排序
    description TEXT,                            -- 描述
    
    -- ===== 来源信息 =====
    source_url VARCHAR(2000),                    -- 来源URL（外部文件）
    
    -- ===== 系统字段 =====
    upload_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    upload_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE arc_digital_file IS '电子文件表';
COMMENT ON COLUMN arc_digital_file.hash_value IS '文件哈希值 - 用于完整性校验';
COMMENT ON COLUMN arc_digital_file.is_long_term_format IS '是否为长期保存格式（PDF/A, OFD）';

-- 3.3 档案元数据表（可扩展的元数据存储）
CREATE TABLE IF NOT EXISTS arc_metadata (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL REFERENCES arc_archive(id),
    
    field_code VARCHAR(100) NOT NULL,            -- 字段代码
    field_name VARCHAR(200) NOT NULL,            -- 字段名称
    field_value TEXT,                            -- 字段值
    field_type VARCHAR(50) DEFAULT 'TEXT',       -- 字段类型：TEXT, NUMBER, DATE, BOOLEAN
    
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(archive_id, field_code)
);

COMMENT ON TABLE arc_metadata IS '档案元数据表 - 扩展元数据存储';

-- =====================================================
-- 四、业务流程表
-- =====================================================

-- 4.1 借阅申请表
CREATE TABLE IF NOT EXISTS arc_borrow_application (
    id BIGSERIAL PRIMARY KEY,
    application_no VARCHAR(50) NOT NULL UNIQUE,  -- 申请编号
    archive_id BIGINT NOT NULL REFERENCES arc_archive(id),
    archive_no VARCHAR(100),                     -- 档案号（冗余）
    archive_title VARCHAR(500),                  -- 档案题名（冗余）
    
    -- ===== 申请人信息 =====
    applicant_id BIGINT,                         -- 申请人ID
    applicant_name VARCHAR(100) NOT NULL,        -- 申请人姓名
    applicant_dept VARCHAR(100),                 -- 申请人部门
    applicant_phone VARCHAR(50),                 -- 联系电话
    apply_time TIMESTAMP,                        -- 申请时间
    
    -- ===== 借阅信息 =====
    borrow_purpose TEXT NOT NULL,                -- 借阅目的
    borrow_type VARCHAR(20) DEFAULT 'ONLINE',    -- 借阅方式：ONLINE-在线阅览, DOWNLOAD-下载, COPY-复制
    expected_return_date DATE,                   -- 预计归还日期
    actual_return_date DATE,                     -- 实际归还日期
    borrow_time TIMESTAMP,                       -- 借出时间
    renew_count INTEGER DEFAULT 0,               -- 续借次数
    
    -- ===== 审批信息 =====
    -- PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, BORROWED-借出中, RETURNED-已归还, CANCELLED-已取消
    status VARCHAR(30) DEFAULT 'PENDING',
    approver_id BIGINT,                          -- 审批人ID
    approver_name VARCHAR(100),                  -- 审批人姓名
    approve_time TIMESTAMP,                      -- 审批时间
    approve_remarks TEXT,                        -- 审批意见
    reject_reason TEXT,                          -- 拒绝原因
    return_remarks TEXT,                         -- 归还备注
    
    -- ===== 使用记录 =====
    download_count INTEGER DEFAULT 0,            -- 下载次数
    view_count INTEGER DEFAULT 0,                -- 阅览次数
    last_access_at TIMESTAMP,                    -- 最后访问时间
    
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE arc_borrow_application IS '档案借阅申请表';

-- 4.2 鉴定记录表
CREATE TABLE IF NOT EXISTS arc_appraisal_record (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL REFERENCES arc_archive(id),
    
    -- ===== 鉴定类型 =====
    appraisal_type VARCHAR(50) NOT NULL,         -- 鉴定类型：VALUE-价值鉴定, SECURITY-密级鉴定, OPEN-开放鉴定, RETENTION-期限鉴定
    
    -- ===== 鉴定信息 =====
    original_value VARCHAR(100),                 -- 原值
    new_value VARCHAR(100),                      -- 新值
    appraisal_reason TEXT,                       -- 鉴定原因
    appraisal_opinion TEXT,                      -- 鉴定意见
    
    -- ===== 审批信息 =====
    status VARCHAR(30) DEFAULT 'PENDING',        -- PENDING, APPROVED, REJECTED
    appraiser_id BIGINT,                         -- 鉴定人
    appraiser_name VARCHAR(100),
    appraised_at TIMESTAMP,
    approver_id BIGINT,                          -- 审批人
    approver_name VARCHAR(100),
    approved_at TIMESTAMP,
    approval_comment TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE arc_appraisal_record IS '档案鉴定记录表';

-- 4.3 销毁记录表
CREATE TABLE IF NOT EXISTS arc_destruction_record (
    id BIGSERIAL PRIMARY KEY,
    destruction_batch_no VARCHAR(50) NOT NULL,   -- 销毁批次号
    archive_id BIGINT NOT NULL REFERENCES arc_archive(id),
    
    -- ===== 销毁信息 =====
    destruction_reason TEXT,                     -- 销毁原因
    destruction_method VARCHAR(50),              -- 销毁方式：LOGICAL-逻辑删除, PHYSICAL-物理销毁
    
    -- ===== 审批信息 =====
    status VARCHAR(30) DEFAULT 'PENDING',        -- PENDING, APPROVED, REJECTED, EXECUTED
    proposer_id BIGINT,                          -- 提议人
    proposer_name VARCHAR(100),
    proposed_at TIMESTAMP,
    approver_id BIGINT,                          -- 审批人
    approver_name VARCHAR(100),
    approved_at TIMESTAMP,
    approval_comment TEXT,
    executor_id BIGINT,                          -- 执行人
    executor_name VARCHAR(100),
    executed_at TIMESTAMP,                       -- 销毁时间
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE arc_destruction_record IS '档案销毁记录表';

-- 4.4 存放位置表
CREATE TABLE IF NOT EXISTS archive_location (
    id BIGSERIAL PRIMARY KEY,
    location_code VARCHAR(50) NOT NULL UNIQUE,       -- 位置编码
    location_name VARCHAR(200) NOT NULL,             -- 位置名称
    room_name VARCHAR(100),                          -- 库房名称
    area VARCHAR(50),                                -- 区域
    shelf_no VARCHAR(50),                            -- 架号
    layer_no VARCHAR(50),                            -- 层号
    
    -- ===== 容量信息 =====
    total_capacity INTEGER DEFAULT 0,                -- 总容量
    used_capacity INTEGER DEFAULT 0,                 -- 已用容量
    
    -- ===== 状态 =====
    status VARCHAR(30) DEFAULT 'AVAILABLE',          -- AVAILABLE-可用, FULL-已满, DISABLED-停用
    remarks TEXT,                                    -- 备注
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE archive_location IS '存放位置表';
COMMENT ON COLUMN archive_location.status IS '状态: AVAILABLE-可用, FULL-已满, DISABLED-停用';

CREATE INDEX IF NOT EXISTS idx_location_code ON archive_location(location_code);
CREATE INDEX IF NOT EXISTS idx_location_room ON archive_location(room_name);
CREATE INDEX IF NOT EXISTS idx_location_status ON archive_location(status);

-- =====================================================
-- 五、日志审计表
-- =====================================================

-- 5.1 操作日志表
CREATE TABLE IF NOT EXISTS arc_operation_log (
    id BIGSERIAL PRIMARY KEY,
    
    -- ===== 操作对象 =====
    archive_id BIGINT,                           -- 档案ID（可为空）
    object_type VARCHAR(50) NOT NULL,            -- 对象类型：ARCHIVE, FILE, BORROW, APPRAISAL, SYSTEM
    object_id VARCHAR(100),                      -- 对象ID
    
    -- ===== 操作信息 =====
    operation_type VARCHAR(50) NOT NULL,         -- 操作类型：CREATE, UPDATE, DELETE, VIEW, DOWNLOAD, PRINT, EXPORT
    operation_desc TEXT,                         -- 操作描述
    operation_detail JSONB,                      -- 操作详情（JSON格式，记录变更前后的值）
    
    -- ===== 操作人信息 =====
    operator_id BIGINT,
    operator_name VARCHAR(100),
    operator_ip VARCHAR(50),
    operator_ua TEXT,                            -- User Agent
    
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE arc_operation_log IS '操作日志表 - 记录所有档案操作';

-- 5.2 访问日志表
CREATE TABLE IF NOT EXISTS arc_access_log (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT,
    file_id BIGINT,
    
    access_type VARCHAR(30) NOT NULL,            -- 访问类型：VIEW, DOWNLOAD, PRINT, PREVIEW, SEARCH
    access_ip VARCHAR(50),
    access_ua TEXT,
    
    user_id BIGINT,
    user_name VARCHAR(100),
    accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- ===== 搜索专用字段 =====
    search_keyword VARCHAR(500),                 -- 搜索关键词
    search_result_count INTEGER,                 -- 搜索结果数量
    duration BIGINT,                             -- 请求耗时（毫秒）
    extra_data JSONB                             -- 额外数据
);

COMMENT ON TABLE arc_access_log IS '档案访问日志表';
CREATE INDEX idx_access_log_type ON arc_access_log(access_type);
CREATE INDEX idx_access_log_user ON arc_access_log(user_id);
CREATE INDEX idx_access_log_time ON arc_access_log(accessed_at);

-- =====================================================
-- 5.2 系统配置表
-- =====================================================

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,     -- 配置键
    config_value TEXT,                           -- 配置值
    config_type VARCHAR(50) DEFAULT 'STRING',    -- 值类型：STRING, NUMBER, BOOLEAN, JSON
    config_group VARCHAR(50),                    -- 配置分组
    description VARCHAR(500),                    -- 配置描述
    editable BOOLEAN DEFAULT TRUE,               -- 是否可编辑
    sort_order INTEGER DEFAULT 0,                -- 排序
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

COMMENT ON TABLE sys_config IS '系统配置表';
COMMENT ON COLUMN sys_config.config_key IS '配置键，全局唯一';
COMMENT ON COLUMN sys_config.config_type IS '值类型：STRING-字符串, NUMBER-数字, BOOLEAN-布尔, JSON-JSON对象';
COMMENT ON COLUMN sys_config.config_group IS '配置分组：ARCHIVE_NO-档案号规则, RETENTION-保管期限, SYSTEM-系统参数';

CREATE INDEX idx_config_group ON sys_config(config_group);
CREATE INDEX idx_config_key ON sys_config(config_key);

-- =====================================================
-- 六、外部集成表
-- =====================================================

-- 6.1 外部系统配置表
CREATE TABLE IF NOT EXISTS arc_external_source (
    id BIGSERIAL PRIMARY KEY,
    source_code VARCHAR(50) NOT NULL UNIQUE,     -- 来源编码
    source_name VARCHAR(200) NOT NULL,           -- 来源名称
    source_type VARCHAR(50) NOT NULL,            -- 来源类型：LAW_FIRM, COURT, ENTERPRISE, OTHER
    description TEXT,
    
    -- ===== 接口配置 =====
    api_url VARCHAR(500),                        -- API地址
    api_key TEXT,                                -- API密钥（加密存储）
    auth_type VARCHAR(50),                       -- 认证方式
    extra_config JSONB,                          -- 扩展配置
    
    -- ===== 状态信息 =====
    enabled BOOLEAN DEFAULT FALSE,
    last_sync_at TIMESTAMP,
    last_sync_status VARCHAR(20),
    last_sync_message TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE arc_external_source IS '外部系统来源配置表';

-- 6.2 推送记录表（记录外部系统推送档案的请求）
CREATE TABLE IF NOT EXISTS arc_push_record (
    id BIGSERIAL PRIMARY KEY,
    push_batch_no VARCHAR(50) NOT NULL,          -- 推送批次号
    
    -- ===== 来源信息 =====
    source_code VARCHAR(50),                     -- 来源编码
    source_type VARCHAR(50) NOT NULL,            -- 来源类型
    source_id VARCHAR(100) NOT NULL,             -- 来源业务ID
    source_no VARCHAR(100),                      -- 来源业务编号
    
    -- ===== 档案关联 =====
    archive_id BIGINT REFERENCES arc_archive(id),-- 档案ID（成功后关联）
    archive_no VARCHAR(100),                     -- 档案号
    title VARCHAR(500),                          -- 档案标题
    
    -- ===== 推送状态 =====
    push_status VARCHAR(30) DEFAULT 'PENDING',   -- PENDING-待处理, PROCESSING-处理中, SUCCESS-成功, FAILED-失败, PARTIAL-部分成功
    file_status VARCHAR(30) DEFAULT 'PENDING',   -- 文件处理状态
    
    -- ===== 文件统计 =====
    total_files INTEGER DEFAULT 0,               -- 总文件数
    success_files INTEGER DEFAULT 0,             -- 成功文件数
    failed_files INTEGER DEFAULT 0,              -- 失败文件数
    
    -- ===== 请求信息 =====
    request_payload JSONB,                       -- 请求内容
    callback_url VARCHAR(500),                   -- 回调地址
    
    -- ===== 错误信息 =====
    error_code VARCHAR(50),                      -- 错误代码
    error_message TEXT,                          -- 错误信息
    
    -- ===== 时间信息 =====
    pushed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 推送时间
    processed_at TIMESTAMP,                      -- 处理完成时间
    
    -- ===== 系统字段 =====
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE arc_push_record IS '档案推送记录表';
CREATE INDEX IF NOT EXISTS idx_push_record_batch_no ON arc_push_record(push_batch_no);
CREATE INDEX IF NOT EXISTS idx_push_record_source ON arc_push_record(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_push_record_status ON arc_push_record(push_status);
CREATE INDEX IF NOT EXISTS idx_push_record_pushed_at ON arc_push_record(pushed_at DESC);

-- 6.3 回调记录表（记录向来源系统发起的回调）
CREATE TABLE IF NOT EXISTS arc_callback_record (
    id BIGSERIAL PRIMARY KEY,
    
    -- ===== 档案关联 =====
    archive_id BIGINT REFERENCES arc_archive(id),
    archive_no VARCHAR(100),
    push_record_id BIGINT REFERENCES arc_push_record(id), -- 关联推送记录
    
    -- ===== 回调信息 =====
    callback_url VARCHAR(500) NOT NULL,          -- 回调地址
    callback_type VARCHAR(50) DEFAULT 'ARCHIVE_RECEIVED', -- 回调类型
    
    -- ===== 状态信息 =====
    callback_status VARCHAR(30) DEFAULT 'PENDING', -- PENDING-待发送, SUCCESS-成功, FAILED-失败
    
    -- ===== 请求响应 =====
    request_body JSONB,                          -- 请求体
    response_code INTEGER,                       -- HTTP状态码
    response_body TEXT,                          -- 响应内容
    
    -- ===== 重试信息 =====
    retry_count INTEGER DEFAULT 0,               -- 已重试次数
    max_retries INTEGER DEFAULT 3,               -- 最大重试次数
    next_retry_at TIMESTAMP,                     -- 下次重试时间
    
    -- ===== 错误信息 =====
    error_message TEXT,                          -- 错误信息
    
    -- ===== 时间信息 =====
    callback_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 首次回调时间
    completed_at TIMESTAMP,                      -- 完成时间
    
    -- ===== 系统字段 =====
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE arc_callback_record IS '回调记录表';
CREATE INDEX IF NOT EXISTS idx_callback_record_archive ON arc_callback_record(archive_id);
CREATE INDEX IF NOT EXISTS idx_callback_record_push ON arc_callback_record(push_record_id);
CREATE INDEX IF NOT EXISTS idx_callback_record_status ON arc_callback_record(callback_status);
CREATE INDEX IF NOT EXISTS idx_callback_record_next_retry ON arc_callback_record(next_retry_at) WHERE callback_status = 'FAILED';

-- =====================================================
-- 七、索引创建
-- =====================================================

-- 档案表索引
CREATE INDEX IF NOT EXISTS idx_archive_archive_no ON arc_archive(archive_no);
CREATE INDEX IF NOT EXISTS idx_archive_fonds_id ON arc_archive(fonds_id);
CREATE INDEX IF NOT EXISTS idx_archive_category_id ON arc_archive(category_id);
CREATE INDEX IF NOT EXISTS idx_archive_archive_type ON arc_archive(archive_type);
CREATE INDEX IF NOT EXISTS idx_archive_status ON arc_archive(status);
CREATE INDEX IF NOT EXISTS idx_archive_source_type ON arc_archive(source_type);
CREATE INDEX IF NOT EXISTS idx_archive_source_id ON arc_archive(source_id);
CREATE INDEX IF NOT EXISTS idx_archive_case_no ON arc_archive(case_no);
CREATE INDEX IF NOT EXISTS idx_archive_retention_expire ON arc_archive(retention_expire_date);
CREATE INDEX IF NOT EXISTS idx_archive_created_at ON arc_archive(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_archive_title_gin ON arc_archive USING gin(to_tsvector('simple', title));

-- 文件表索引
CREATE INDEX IF NOT EXISTS idx_file_archive_id ON arc_digital_file(archive_id);
CREATE INDEX IF NOT EXISTS idx_file_hash ON arc_digital_file(hash_value);

-- 借阅表索引
CREATE INDEX IF NOT EXISTS idx_borrow_archive_id ON arc_borrow_application(archive_id);
CREATE INDEX IF NOT EXISTS idx_borrow_applicant_id ON arc_borrow_application(applicant_id);
CREATE INDEX IF NOT EXISTS idx_borrow_status ON arc_borrow_application(status);

-- 日志表索引
CREATE INDEX IF NOT EXISTS idx_oplog_archive_id ON arc_operation_log(archive_id);
CREATE INDEX IF NOT EXISTS idx_oplog_operated_at ON arc_operation_log(operated_at DESC);
CREATE INDEX IF NOT EXISTS idx_access_archive_id ON arc_access_log(archive_id);
CREATE INDEX IF NOT EXISTS idx_access_accessed_at ON arc_access_log(accessed_at DESC);

-- =====================================================
-- 八、初始化数据
-- =====================================================

-- 初始化保管期限
INSERT INTO arc_retention_period (period_code, period_name, period_years, sort_order) VALUES
('PERMANENT', '永久', NULL, 1),
('Y30', '30年', 30, 2),
('Y15', '15年', 15, 3),
('Y10', '10年', 10, 4),
('Y5', '5年', 5, 5)
ON CONFLICT (period_code) DO NOTHING;

-- 初始化默认全宗
INSERT INTO arc_fonds (fonds_no, fonds_name, fonds_type, description) VALUES
('QZ001', '律所档案', 'INTERNAL', '律所管理系统归档的电子档案')
ON CONFLICT (fonds_no) DO NOTHING;

-- 初始化档案分类
INSERT INTO arc_category (parent_id, category_code, category_name, archive_type, level, sort_order, retention_period, full_path) VALUES
(NULL, '01', '案件档案', 'SPECIAL', 1, 1, 'Y10', '01'),
(NULL, '02', '合同档案', 'DOCUMENT', 1, 2, 'Y10', '02'),
(NULL, '03', '行政档案', 'DOCUMENT', 1, 3, 'Y10', '03'),
(NULL, '04', '财务档案', 'ACCOUNTING', 1, 4, 'Y15', '04'),
(NULL, '05', '人事档案', 'PERSONNEL', 1, 5, 'PERMANENT', '05')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 初始化用户（三员分立 + 业务用户）
-- 默认密码均为：admin123
-- 密码BCrypt哈希（cost=10）
-- 注意：生产环境部署后请立即修改所有默认密码！
-- =====================================================

-- 1. 系统管理员（SYSTEM_ADMIN）- 负责系统配置、用户管理、系统维护
INSERT INTO sys_user (username, password, real_name, email, phone, department, user_type, status) VALUES
('admin', '$2a$10$x8fiMNGYO6phwaI8VDzo0edwHR1d60F/cSonNxs/X2sz3V2rdV6vK', '系统管理员', 'admin@archive.com', '13800000001', '信息技术部', 'SYSTEM_ADMIN', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- 2. 安全保密员（SECURITY_ADMIN）- 负责权限管理、密级管理、安全策略
INSERT INTO sys_user (username, password, real_name, email, phone, department, user_type, status) VALUES
('security', '$2a$10$x8fiMNGYO6phwaI8VDzo0edwHR1d60F/cSonNxs/X2sz3V2rdV6vK', '安全保密员', 'security@archive.com', '13800000002', '安全保密部', 'SECURITY_ADMIN', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- 3. 安全审计员（AUDIT_ADMIN）- 负责日志审计、安全审查、合规检查
INSERT INTO sys_user (username, password, real_name, email, phone, department, user_type, status) VALUES
('auditor', '$2a$10$x8fiMNGYO6phwaI8VDzo0edwHR1d60F/cSonNxs/X2sz3V2rdV6vK', '安全审计员', 'auditor@archive.com', '13800000003', '审计部', 'AUDIT_ADMIN', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- 4. 档案员（ARCHIVIST）- 核心业务人员，负责档案接收、整理、归档、借阅审批
INSERT INTO sys_user (username, password, real_name, email, phone, department, user_type, status) VALUES
('archivist1', '$2a$10$x8fiMNGYO6phwaI8VDzo0edwHR1d60F/cSonNxs/X2sz3V2rdV6vK', '张档案', 'archivist1@archive.com', '13800000004', '档案管理部', 'ARCHIVIST', 'ACTIVE'),
('archivist2', '$2a$10$x8fiMNGYO6phwaI8VDzo0edwHR1d60F/cSonNxs/X2sz3V2rdV6vK', '李档案', 'archivist2@archive.com', '13800000005', '档案管理部', 'ARCHIVIST', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- 5. 普通用户（USER）- 档案查阅人员，如律师、员工等，可申请借阅
INSERT INTO sys_user (username, password, real_name, email, phone, department, user_type, status) VALUES
('lawyer1', '$2a$10$x8fiMNGYO6phwaI8VDzo0edwHR1d60F/cSonNxs/X2sz3V2rdV6vK', '王律师', 'lawyer1@lawfirm.com', '13800000006', '诉讼部', 'USER', 'ACTIVE'),
('lawyer2', '$2a$10$x8fiMNGYO6phwaI8VDzo0edwHR1d60F/cSonNxs/X2sz3V2rdV6vK', '赵律师', 'lawyer2@lawfirm.com', '13800000007', '非诉部', 'USER', 'ACTIVE'),
('assistant', '$2a$10$x8fiMNGYO6phwaI8VDzo0edwHR1d60F/cSonNxs/X2sz3V2rdV6vK', '陈助理', 'assistant@lawfirm.com', '13800000008', '综合部', 'USER', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- 初始化律所系统来源（包含 API Key，用于对接律所管理系统）
-- 注意：生产环境部署后请在后台"来源管理"中重新生成 API Key
INSERT INTO arc_external_source (source_code, source_name, source_type, description, api_key, auth_type, enabled) VALUES
('LAW_FIRM_MAIN', '律所管理系统', 'LAW_FIRM', '接收律所管理系统推送的归档档案', 'lawfirm-archive-api-key-2026', 'API_KEY', true)
ON CONFLICT (source_code) DO UPDATE SET 
    api_key = EXCLUDED.api_key,
    enabled = EXCLUDED.enabled;

-- 初始化系统配置
INSERT INTO sys_config (config_key, config_value, config_type, config_group, description, editable, sort_order) VALUES
-- 档案号规则配置
('archive.no.prefix.DOCUMENT', 'WS', 'STRING', 'ARCHIVE_NO', '文书档案号前缀', true, 1),
('archive.no.prefix.SCIENCE', 'KJ', 'STRING', 'ARCHIVE_NO', '科技档案号前缀', true, 2),
('archive.no.prefix.ACCOUNTING', 'CW', 'STRING', 'ARCHIVE_NO', '会计档案号前缀', true, 3),
('archive.no.prefix.PERSONNEL', 'RS', 'STRING', 'ARCHIVE_NO', '人事档案号前缀', true, 4),
('archive.no.prefix.SPECIAL', 'ZY', 'STRING', 'ARCHIVE_NO', '专业档案号前缀', true, 5),
('archive.no.prefix.AUDIOVISUAL', 'SX', 'STRING', 'ARCHIVE_NO', '声像档案号前缀', true, 6),
('archive.no.prefix.DEFAULT', 'ARC', 'STRING', 'ARCHIVE_NO', '默认档案号前缀', true, 7),
('archive.no.date.format', 'yyyyMMdd', 'STRING', 'ARCHIVE_NO', '档案号日期格式', true, 8),
('archive.no.seq.digits', '4', 'NUMBER', 'ARCHIVE_NO', '档案号序号位数', true, 9),
-- 保管期限配置
('retention.default.code', 'Y10', 'STRING', 'RETENTION', '默认保管期限代码', true, 10),
('retention.warn.days', '90', 'NUMBER', 'RETENTION', '到期预警天数', true, 11),
-- 系统参数 - 文件上传
('system.upload.max.size', '104857600', 'NUMBER', 'SYSTEM', '上传文件最大大小（字节）', true, 20),
('system.upload.allowed.types', 'pdf,doc,docx,xls,xlsx,ppt,pptx,jpg,jpeg,png,gif,zip,rar,txt,ofd', 'STRING', 'SYSTEM', '允许上传的文件类型', true, 21),
-- 系统参数 - 借阅
('system.borrow.max.days', '30', 'NUMBER', 'SYSTEM', '最大借阅天数', true, 22),
('system.borrow.renew.max.times', '2', 'NUMBER', 'SYSTEM', '最大续借次数', true, 23),
('system.borrow.renew.days', '15', 'NUMBER', 'SYSTEM', '每次续借天数', true, 24),
-- 系统参数 - 搜索
('system.search.max.results', '10000', 'NUMBER', 'SYSTEM', '搜索最大结果数', true, 25),
('system.search.page.size', '20', 'NUMBER', 'SYSTEM', '默认分页大小', true, 26),
-- 系统参数 - 水印
('system.watermark.enabled', 'true', 'BOOLEAN', 'SYSTEM', '启用水印', true, 30),
('system.watermark.text', '档案管理系统', 'STRING', 'SYSTEM', '水印文字', true, 31),
('system.watermark.opacity', '0.3', 'STRING', 'SYSTEM', '水印透明度(0-1)', true, 32),
('system.watermark.font.size', '20', 'NUMBER', 'SYSTEM', '水印字体大小', true, 33),
-- 系统参数 - 回调
('system.callback.retry.max', '3', 'NUMBER', 'SYSTEM', '回调最大重试次数', true, 40),
('system.callback.retry.interval', '5000', 'NUMBER', 'SYSTEM', '回调重试间隔(毫秒)', true, 41),
('system.callback.timeout', '30000', 'NUMBER', 'SYSTEM', '回调超时时间(毫秒)', true, 42),
-- 系统参数 - 安全
('system.password.min.length', '8', 'NUMBER', 'SYSTEM', '密码最小长度', true, 50),
('system.password.require.special', 'true', 'BOOLEAN', 'SYSTEM', '密码需要特殊字符', true, 51),
('system.login.max.attempts', '5', 'NUMBER', 'SYSTEM', '登录最大尝试次数', true, 52),
('system.login.lock.minutes', '30', 'NUMBER', 'SYSTEM', '账户锁定时间(分钟)', true, 53),
-- 系统参数 - 通知
('system.notify.email.enabled', 'false', 'BOOLEAN', 'SYSTEM', '启用邮件通知', true, 60),
('system.notify.expire.days', '30', 'NUMBER', 'SYSTEM', '到期提醒提前天数', true, 61),
-- 系统参数 - 站点信息
('system.site.name', '档案管理系统', 'STRING', 'SITE', '系统名称', true, 1),
('system.site.name.en', 'Archive Management System', 'STRING', 'SITE', '系统英文名称', true, 2),
('system.site.icp', '', 'STRING', 'SITE', 'ICP备案号', true, 3),
('system.site.copyright', '© 2024 档案管理系统', 'STRING', 'SITE', '版权信息', true, 4),
('system.site.logo', '', 'STRING', 'SITE', 'Logo URL', true, 5)
ON CONFLICT (config_key) DO NOTHING;
