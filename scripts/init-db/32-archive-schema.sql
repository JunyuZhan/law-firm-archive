-- ============================================
-- 智慧律所管理系统 - 档案管理模块数据库脚本
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 档案库位表
CREATE TABLE IF NOT EXISTS archive_location (
    id BIGSERIAL PRIMARY KEY,
    location_code VARCHAR(50) NOT NULL UNIQUE,
    location_name VARCHAR(100) NOT NULL,
    room VARCHAR(50),
    cabinet VARCHAR(50),
    shelf VARCHAR(50),
    position VARCHAR(50),
    total_capacity INT DEFAULT 0,
    used_capacity INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    remarks TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_archive_location_code ON archive_location(location_code);
CREATE INDEX idx_archive_location_status ON archive_location(status);

COMMENT ON TABLE archive_location IS '档案库位表';
COMMENT ON COLUMN archive_location.location_code IS '库位编码';
COMMENT ON COLUMN archive_location.status IS '状态：AVAILABLE-可用, FULL-已满, MAINTENANCE-维护中';

-- 档案表
CREATE TABLE IF NOT EXISTS archive (
    id BIGSERIAL PRIMARY KEY,
    archive_no VARCHAR(50) NOT NULL UNIQUE,
    matter_id BIGINT,
    archive_name VARCHAR(200) NOT NULL,
    archive_type VARCHAR(20) NOT NULL,
    
    -- 冗余字段（从案件表同步）
    matter_no VARCHAR(50),
    matter_name VARCHAR(200),
    client_name VARCHAR(200),
    main_lawyer_name VARCHAR(50),
    case_close_date DATE,
    
    -- 档案信息
    volume_count INT DEFAULT 1,
    page_count INT DEFAULT 0,
    catalog TEXT,
    
    -- 库位信息
    location_id BIGINT,
    box_no VARCHAR(50),
    
    -- 保管期限
    retention_period VARCHAR(20) DEFAULT '10_YEARS',
    retention_expire_date DATE,
    
    -- 电子档案
    has_electronic BOOLEAN DEFAULT FALSE,
    electronic_url VARCHAR(500),
    
    -- 状态
    status VARCHAR(20) DEFAULT 'PENDING',
    
    -- 入库信息
    stored_by BIGINT,
    stored_at TIMESTAMP,
    
    -- 销毁信息
    destroy_date DATE,
    destroy_reason TEXT,
    destroy_approver_id BIGINT,
    
    -- 备注
    remarks TEXT,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_archive_archive_no ON archive(archive_no);
CREATE INDEX idx_archive_matter_id ON archive(matter_id);
CREATE INDEX idx_archive_location_id ON archive(location_id);
CREATE INDEX idx_archive_status ON archive(status);
CREATE INDEX idx_archive_type ON archive(archive_type);
CREATE INDEX idx_archive_expire_date ON archive(retention_expire_date);

COMMENT ON TABLE archive IS '档案表';
COMMENT ON COLUMN archive.archive_no IS '档案号';
COMMENT ON COLUMN archive.archive_type IS '档案类型：LITIGATION-诉讼, NON_LITIGATION-非诉, CONSULTATION-咨询';
COMMENT ON COLUMN archive.retention_period IS '保管期限：PERMANENT-永久, 30_YEARS-30年, 15_YEARS-15年, 10_YEARS-10年, 5_YEARS-5年';
COMMENT ON COLUMN archive.status IS '状态：PENDING-待入库, STORED-已入库, BORROWED-借出, DESTROYED-已销毁';

-- 档案借阅表
CREATE TABLE IF NOT EXISTS archive_borrow (
    id BIGSERIAL PRIMARY KEY,
    borrow_no VARCHAR(50) NOT NULL UNIQUE,
    archive_id BIGINT NOT NULL,
    borrower_id BIGINT NOT NULL,
    borrower_name VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    borrow_reason TEXT,
    borrow_date DATE NOT NULL,
    expected_return_date DATE NOT NULL,
    actual_return_date DATE,
    status VARCHAR(20) DEFAULT 'PENDING',
    approver_id BIGINT,
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    return_handler_id BIGINT,
    return_condition VARCHAR(20),
    return_remarks TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_archive_borrow_borrow_no ON archive_borrow(borrow_no);
CREATE INDEX idx_archive_borrow_archive_id ON archive_borrow(archive_id);
CREATE INDEX idx_archive_borrow_borrower_id ON archive_borrow(borrower_id);
CREATE INDEX idx_archive_borrow_status ON archive_borrow(status);
CREATE INDEX idx_archive_borrow_date ON archive_borrow(borrow_date, expected_return_date);

COMMENT ON TABLE archive_borrow IS '档案借阅表';
COMMENT ON COLUMN archive_borrow.status IS '状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, BORROWED-借出中, RETURNED-已归还, OVERDUE-逾期';
COMMENT ON COLUMN archive_borrow.return_condition IS '归还状态：GOOD-完好, DAMAGED-损坏, LOST-遗失';

-- 档案操作日志表
CREATE TABLE IF NOT EXISTS archive_operation_log (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    operation_description TEXT,
    operator_id BIGINT NOT NULL,
    operated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_archive_operation_log_archive_id ON archive_operation_log(archive_id);
CREATE INDEX idx_archive_operation_log_type ON archive_operation_log(operation_type);
CREATE INDEX idx_archive_operation_log_operator ON archive_operation_log(operator_id);
CREATE INDEX idx_archive_operation_log_time ON archive_operation_log(operated_at);

COMMENT ON TABLE archive_operation_log IS '档案操作日志表';
COMMENT ON COLUMN archive_operation_log.operation_type IS '操作类型：STORE-入库, BORROW-借出, RETURN-归还, TRANSFER-转移, DESTROY-销毁';

