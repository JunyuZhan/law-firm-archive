-- 测试用数据库schema
CREATE TABLE IF NOT EXISTS arc_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS arc_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS arc_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS arc_fonds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL UNIQUE,
    fonds_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS arc_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(200) NOT NULL,
    category_type VARCHAR(50),
    sort_order INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS arc_archive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    archive_no VARCHAR(50) NOT NULL UNIQUE,
    fonds_id BIGINT,
    fonds_no VARCHAR(50),
    category_id BIGINT,
    category_code VARCHAR(50),
    archive_type VARCHAR(50),
    title VARCHAR(500) NOT NULL,
    file_no VARCHAR(100),
    responsibility VARCHAR(200),
    archive_date DATE,
    document_date DATE,
    page_count INT DEFAULT 0,
    pieces_count INT DEFAULT 1,
    retention_period VARCHAR(50),
    retention_expire_date DATE,
    security_level VARCHAR(20) DEFAULT 'INTERNAL',
    security_expire_date DATE,
    source_type VARCHAR(50),
    source_system VARCHAR(100),
    source_id VARCHAR(100),
    source_no VARCHAR(100),
    case_no VARCHAR(100),
    case_name VARCHAR(200),
    client_name VARCHAR(100),
    lawyer_name VARCHAR(100),
    case_close_date DATE,
    has_electronic BOOLEAN DEFAULT TRUE,
    storage_location VARCHAR(200),
    total_file_size BIGINT DEFAULT 0,
    file_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'RECEIVED',
    received_at TIMESTAMP,
    received_by BIGINT,
    cataloged_at TIMESTAMP,
    cataloged_by BIGINT,
    archived_at TIMESTAMP,
    archived_by BIGINT,
    keywords VARCHAR(500),
    abstract TEXT,
    remarks VARCHAR(500),
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS arc_digital_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    archive_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    file_extension VARCHAR(20),
    mime_type VARCHAR(100),
    file_size BIGINT DEFAULT 0,
    hash_value VARCHAR(128),
    storage_path VARCHAR(500),
    thumbnail_path VARCHAR(500),
    preview_path VARCHAR(500),
    has_preview BOOLEAN DEFAULT FALSE,
    upload_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS arc_borrow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    archive_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(100),
    apply_reason VARCHAR(500),
    apply_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    borrow_date DATE,
    return_date DATE,
    actual_return_date DATE,
    status VARCHAR(20) DEFAULT 'PENDING',
    approver_id BIGINT,
    approve_time TIMESTAMP,
    approve_comment VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS arc_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    operation VARCHAR(100) NOT NULL,
    method VARCHAR(500),
    params TEXT,
    result TEXT,
    ip_address VARCHAR(50),
    execution_time BIGINT,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS arc_access_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    resource_type VARCHAR(50),
    resource_id BIGINT,
    action VARCHAR(50),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试用户
INSERT INTO arc_user (id, username, password, real_name, email, phone, status) 
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin@archive.com', '13800138000', 'ACTIVE');

INSERT INTO arc_user (id, username, password, real_name, email, phone, status) 
VALUES (2, 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户', 'test@archive.com', '13900139000', 'ACTIVE');

-- 插入测试角色
INSERT INTO arc_role (id, role_name, role_code, description, status) 
VALUES (1, '系统管理员', 'ROLE_ADMIN', '系统管理员拥有所有权限', 'ACTIVE');

INSERT INTO arc_role (id, role_name, role_code, description, status) 
VALUES (2, '档案管理员', 'ROLE_ARCHIVE_ADMIN', '档案管理所有权限', 'ACTIVE');

INSERT INTO arc_role (id, role_name, role_code, description, status) 
VALUES (3, '普通用户', 'ROLE_USER', '普通用户权限', 'ACTIVE');

-- 插入用户角色关联
INSERT INTO arc_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO arc_user_role (user_id, role_id) VALUES (2, 3);

-- 插入测试全宗
INSERT INTO arc_fonds (id, fonds_no, fonds_name, description, status) 
VALUES (1, 'F001', '律所档案全宗', '律师事务所档案全宗', 'ACTIVE');

-- 插入测试分类
INSERT INTO arc_category (id, parent_id, category_code, category_name, category_type, sort_order, status) 
VALUES (1, 0, '01', '诉讼档案', 'LITIGATION', 1, 'ACTIVE');

INSERT INTO arc_category (id, parent_id, category_code, category_name, category_type, sort_order, status) 
VALUES (2, 0, '02', '非诉讼档案', 'NON_LITIGATION', 2, 'ACTIVE');

INSERT INTO arc_category (id, parent_id, category_code, category_name, category_type, sort_order, status) 
VALUES (3, 1, '0101', '民事诉讼', 'CIVIL', 1, 'ACTIVE');

INSERT INTO arc_category (id, parent_id, category_code, category_name, category_type, sort_order, status) 
VALUES (4, 1, '0102', '刑事诉讼', 'CRIMINAL', 2, 'ACTIVE');

-- 插入测试档案
INSERT INTO arc_archive (id, archive_no, fonds_id, fonds_no, category_id, category_code, archive_type, title, 
    file_no, responsibility, retention_period, security_level, source_type, status, page_count, file_count)
VALUES (1, 'ARC-2026-0001', 1, 'F001', 3, '0101', 'LITIGATION', '张三诉李四合同纠纷案', 
    'CASE-2026-001', '张律师', 'Y10', 'INTERNAL', 'LAW_FIRM', 'RECEIVED', 50, 3);

INSERT INTO arc_archive (id, archive_no, fonds_id, fonds_no, category_id, category_code, archive_type, title, 
    file_no, responsibility, retention_period, security_level, source_type, status, page_count, file_count)
VALUES (2, 'ARC-2026-0002', 1, 'F001', 4, '0102', 'LITIGATION', '王五故意伤害案', 
    'CASE-2026-002', '李律师', 'Y30', 'SECRET', 'LAW_FIRM', 'STORED', 120, 5);
