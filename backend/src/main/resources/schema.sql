-- 基础表结构初始化 (H2 PostgreSQL 兼容模式)

DROP TABLE IF EXISTS matter CASCADE;
CREATE TABLE matter (
    id BIGINT PRIMARY KEY,
    matter_no VARCHAR(50),
    name VARCHAR(200),
    matter_type VARCHAR(50),
    case_type VARCHAR(50),
    litigation_stage VARCHAR(50),
    cause_of_action VARCHAR(50),
    business_type VARCHAR(50),
    client_id BIGINT,
    opposing_party VARCHAR(200),
    opposing_lawyer_name VARCHAR(100),
    opposing_lawyer_license_no VARCHAR(100),
    opposing_lawyer_firm VARCHAR(200),
    opposing_lawyer_phone VARCHAR(50),
    opposing_lawyer_email VARCHAR(100),
    description TEXT,
    status VARCHAR(50),
    originator_id BIGINT,
    lead_lawyer_id BIGINT,
    department_id BIGINT,
    fee_type VARCHAR(50),
    estimated_fee DECIMAL(18,2),
    actual_fee DECIMAL(18,2),
    filing_date DATE,
    expected_end_date DATE,
    actual_end_date DATE,
    claim_amount DECIMAL(18,2),
    outcome TEXT,
    contract_id BIGINT,
    remark TEXT,
    conflict_status VARCHAR(50),
    deleted BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

DROP TABLE IF EXISTS matter_participant CASCADE;
CREATE TABLE matter_participant (
    id BIGINT PRIMARY KEY,
    matter_id BIGINT,
    user_id BIGINT,
    role VARCHAR(50),
    commission_rate DECIMAL(5,2),
    is_originator BOOLEAN,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    deleted BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
