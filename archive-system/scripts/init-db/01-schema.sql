-- =====================================================
-- 档案管理系统数据库初始化脚本
-- 数据库: archive_system
-- 版本: 1.0.0
-- =====================================================

-- 创建数据库（如果不存在）
-- CREATE DATABASE archive_system WITH ENCODING 'UTF8';

-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    department VARCHAR(100),
    role VARCHAR(20) DEFAULT 'USER',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.role IS '角色: ADMIN-管理员, ARCHIVIST-档案员, USER-普通用户';
COMMENT ON COLUMN sys_user.status IS '状态: ACTIVE-正常, DISABLED-禁用';

-- 2. 档案存放位置表
CREATE TABLE IF NOT EXISTS archive_location (
    id BIGSERIAL PRIMARY KEY,
    location_code VARCHAR(50) NOT NULL UNIQUE,
    location_name VARCHAR(100) NOT NULL,
    room_name VARCHAR(100),
    area VARCHAR(50),
    shelf_no VARCHAR(20),
    layer_no VARCHAR(20),
    total_capacity INTEGER DEFAULT 100,
    used_capacity INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE archive_location IS '档案存放位置表';
COMMENT ON COLUMN archive_location.status IS '状态: AVAILABLE-可用, FULL-已满, DISABLED-停用';

-- 3. 档案来源配置表
CREATE TABLE IF NOT EXISTS archive_source (
    id BIGSERIAL PRIMARY KEY,
    source_code VARCHAR(50) NOT NULL UNIQUE,
    source_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    description TEXT,
    api_url VARCHAR(500),
    api_key TEXT,
    api_secret TEXT,
    auth_type VARCHAR(50),
    extra_config JSONB,
    enabled BOOLEAN DEFAULT FALSE,
    last_sync_at TIMESTAMP,
    last_sync_result VARCHAR(20),
    last_sync_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE archive_source IS '档案来源配置表';
COMMENT ON COLUMN archive_source.source_type IS '来源类型: LAW_FIRM-律所系统, COURT-法院系统, ENTERPRISE-企业系统, OTHER-其他';

-- 4. 档案主表
CREATE TABLE IF NOT EXISTS archive (
    id BIGSERIAL PRIMARY KEY,
    archive_no VARCHAR(50) NOT NULL UNIQUE,
    archive_name VARCHAR(255) NOT NULL,
    archive_type VARCHAR(50),
    category VARCHAR(50),
    description TEXT,
    
    -- 来源信息
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(100),
    source_no VARCHAR(100),
    source_name VARCHAR(255),
    source_snapshot JSONB,
    
    -- 关联信息
    client_name VARCHAR(255),
    responsible_person VARCHAR(100),
    case_close_date DATE,
    
    -- 物理信息
    volume_count INTEGER DEFAULT 1,
    page_count INTEGER,
    catalog TEXT,
    location_id BIGINT REFERENCES archive_location(id),
    box_no VARCHAR(50),
    has_electronic BOOLEAN DEFAULT FALSE,
    
    -- 保管信息
    retention_period VARCHAR(20) DEFAULT '10_YEARS',
    retention_expire_date DATE,
    
    -- 状态信息
    status VARCHAR(20) DEFAULT 'RECEIVED',
    stored_by BIGINT REFERENCES sys_user(id),
    stored_at TIMESTAMP,
    received_at TIMESTAMP,
    remarks TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE archive IS '档案主表';
COMMENT ON COLUMN archive.archive_type IS '档案类型: LITIGATION-诉讼, NON_LITIGATION-非诉, CONSULTATION-咨询, OTHER-其他';
COMMENT ON COLUMN archive.category IS '分类: CASE-案件档案, CONTRACT-合同档案, PERSONNEL-人事档案, FINANCE-财务档案, OTHER-其他';
COMMENT ON COLUMN archive.source_type IS '来源类型: LAW_FIRM-律所系统, MANUAL-手动录入, IMPORT-批量导入, EXTERNAL-外部系统';
COMMENT ON COLUMN archive.retention_period IS '保管期限: PERMANENT-永久, 30_YEARS, 15_YEARS, 10_YEARS, 5_YEARS';
COMMENT ON COLUMN archive.status IS '状态: RECEIVED-已接收, PENDING-待入库, STORED-已入库, BORROWED-借出中, PENDING_DESTROY-待销毁, DESTROYED-已销毁';

-- 5. 档案文件表
CREATE TABLE IF NOT EXISTS archive_file (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL REFERENCES archive(id),
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255),
    file_type VARCHAR(100),
    file_size BIGINT,
    storage_path VARCHAR(500),
    category VARCHAR(50) DEFAULT 'DOCUMENT',
    sort_order INTEGER DEFAULT 0,
    description TEXT,
    source_url VARCHAR(1000),
    file_md5 VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE archive_file IS '档案文件表';
COMMENT ON COLUMN archive_file.category IS '分类: COVER-封面, CATALOG-目录, DOCUMENT-文档, ATTACHMENT-附件';

-- 6. 档案借阅表
CREATE TABLE IF NOT EXISTS archive_borrow (
    id BIGSERIAL PRIMARY KEY,
    borrow_no VARCHAR(50) NOT NULL UNIQUE,
    archive_id BIGINT NOT NULL REFERENCES archive(id),
    borrower_id BIGINT REFERENCES sys_user(id),
    borrower_name VARCHAR(100) NOT NULL,
    borrower_dept VARCHAR(100),
    borrower_contact VARCHAR(50),
    borrow_reason TEXT,
    expected_return_date DATE,
    actual_return_date DATE,
    status VARCHAR(20) DEFAULT 'PENDING',
    approver_id BIGINT REFERENCES sys_user(id),
    approver_name VARCHAR(100),
    approved_at TIMESTAMP,
    approval_comment TEXT,
    return_condition TEXT,
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE archive_borrow IS '档案借阅表';
COMMENT ON COLUMN archive_borrow.status IS '状态: PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, BORROWED-借出中, RETURNED-已归还, OVERDUE-已逾期';

-- 7. 档案操作日志表
CREATE TABLE IF NOT EXISTS archive_operation_log (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL REFERENCES archive(id),
    operation_type VARCHAR(50) NOT NULL,
    operation_description TEXT,
    operator_id BIGINT REFERENCES sys_user(id),
    operator_name VARCHAR(100),
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    extra_data JSONB
);

COMMENT ON TABLE archive_operation_log IS '档案操作日志表';

-- =====================================================
-- 创建索引
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_archive_archive_no ON archive(archive_no);
CREATE INDEX IF NOT EXISTS idx_archive_source_type ON archive(source_type);
CREATE INDEX IF NOT EXISTS idx_archive_source_id ON archive(source_id);
CREATE INDEX IF NOT EXISTS idx_archive_status ON archive(status);
CREATE INDEX IF NOT EXISTS idx_archive_location_id ON archive(location_id);
CREATE INDEX IF NOT EXISTS idx_archive_created_at ON archive(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_archive_retention_expire ON archive(retention_expire_date);

CREATE INDEX IF NOT EXISTS idx_archive_file_archive_id ON archive_file(archive_id);
CREATE INDEX IF NOT EXISTS idx_archive_file_md5 ON archive_file(file_md5);

CREATE INDEX IF NOT EXISTS idx_archive_borrow_archive_id ON archive_borrow(archive_id);
CREATE INDEX IF NOT EXISTS idx_archive_borrow_borrower_id ON archive_borrow(borrower_id);
CREATE INDEX IF NOT EXISTS idx_archive_borrow_status ON archive_borrow(status);

CREATE INDEX IF NOT EXISTS idx_operation_log_archive_id ON archive_operation_log(archive_id);
CREATE INDEX IF NOT EXISTS idx_operation_log_operated_at ON archive_operation_log(operated_at DESC);

-- =====================================================
-- 初始化数据
-- =====================================================

-- 初始化管理员用户（密码: admin123，实际使用时需要BCrypt加密）
INSERT INTO sys_user (username, password, real_name, role, status) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6zX32', '系统管理员', 'ADMIN', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- 初始化默认存放位置
INSERT INTO archive_location (location_code, location_name, room_name, area, shelf_no, total_capacity) VALUES
('LOC-A01-01', 'A区1号架第1层', '档案室A区', 'A区', 'A01', 50),
('LOC-A01-02', 'A区1号架第2层', '档案室A区', 'A区', 'A01', 50),
('LOC-A01-03', 'A区1号架第3层', '档案室A区', 'A区', 'A01', 50),
('LOC-A02-01', 'A区2号架第1层', '档案室A区', 'A区', 'A02', 50),
('LOC-B01-01', 'B区1号架第1层', '档案室B区', 'B区', 'B01', 100)
ON CONFLICT (location_code) DO NOTHING;

-- 初始化律所系统来源配置
INSERT INTO archive_source (source_code, source_name, source_type, description, auth_type, enabled) VALUES
('LAW_FIRM_MAIN', '律所管理系统（主系统）', 'LAW_FIRM', '接收律所管理系统推送的归档档案', 'API_KEY', true)
ON CONFLICT (source_code) DO NOTHING;
