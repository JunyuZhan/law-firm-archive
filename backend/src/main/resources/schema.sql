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

-- 国家赔偿案件业务信息表
DROP TABLE IF EXISTS matter_state_compensation CASCADE;
CREATE TABLE matter_state_compensation (
    id BIGINT PRIMARY KEY,
    matter_id BIGINT NOT NULL,
    -- 赔偿义务机关
    obligor_org_name VARCHAR(200),
    obligor_org_type VARCHAR(50),
    -- 致损行为
    case_source VARCHAR(50),
    damage_description TEXT,
    -- 刑事赔偿特有字段
    criminal_case_terminated BOOLEAN,
    criminal_case_no VARCHAR(100),
    compensation_committee VARCHAR(200),
    -- 程序日期
    application_date DATE,
    acceptance_date DATE,
    decision_date DATE,
    reconsideration_date DATE,
    reconsideration_decision_date DATE,
    committee_app_date DATE,
    committee_decision_date DATE,
    -- 行政赔偿特有字段
    admin_litigation_filing_date DATE,
    admin_litigation_court_name VARCHAR(200),
    -- 赔偿请求
    claim_amount DECIMAL(18,2),
    compensation_items TEXT,
    -- 决定结果
    decision_result VARCHAR(50),
    approved_amount DECIMAL(18,2),
    payment_status VARCHAR(50),
    payment_date DATE,
    remark TEXT,
    -- 审计字段
    deleted BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
