-- ============================================
-- 智慧律所管理系统 - 数据库初始化脚本
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================
-- 一、系统管理模块
-- ============================================

-- 部门表
CREATE TABLE IF NOT EXISTS sys_department (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    leader_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_department IS '部门表';
COMMENT ON COLUMN sys_department.name IS '部门名称';
COMMENT ON COLUMN sys_department.parent_id IS '父部门ID';
COMMENT ON COLUMN sys_department.sort_order IS '排序';
COMMENT ON COLUMN sys_department.leader_id IS '部门负责人';
COMMENT ON COLUMN sys_department.status IS '状态';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    department_id BIGINT,
    position VARCHAR(50),
    employee_no VARCHAR(50),
    lawyer_license_no VARCHAR(50),
    join_date DATE,
    
    -- 薪酬模式（重要字段）
    compensation_type VARCHAR(20) DEFAULT 'COMMISSION',
    can_be_originator BOOLEAN DEFAULT TRUE,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码';
COMMENT ON COLUMN sys_user.real_name IS '真实姓名';
COMMENT ON COLUMN sys_user.compensation_type IS '薪酬模式: COMMISSION-提成制, SALARIED-授薪制, HYBRID-混合制';
COMMENT ON COLUMN sys_user.can_be_originator IS '是否可作为案源人';
COMMENT ON COLUMN sys_user.status IS '状态: ACTIVE, INACTIVE, LOCKED';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    data_scope VARCHAR(20) DEFAULT 'SELF',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.role_code IS '角色编码';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.data_scope IS '数据范围: ALL, DEPT, DEPT_AND_CHILD, SELF';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';

-- 菜单/权限表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(200),
    component VARCHAR(200),
    redirect VARCHAR(200),
    icon VARCHAR(100),
    menu_type VARCHAR(20) NOT NULL,
    permission VARCHAR(200),
    sort_order INT DEFAULT 0,
    visible BOOLEAN DEFAULT TRUE,
    status VARCHAR(20) DEFAULT 'ENABLED',
    is_external BOOLEAN DEFAULT FALSE,
    is_cache BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_menu_parent ON sys_menu(parent_id);

COMMENT ON TABLE sys_menu IS '菜单/权限表';
COMMENT ON COLUMN sys_menu.menu_type IS '类型: DIRECTORY-目录, MENU-菜单, BUTTON-按钮';
COMMENT ON COLUMN sys_menu.permission IS '权限标识';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    real_name VARCHAR(50),
    login_ip VARCHAR(50),
    login_location VARCHAR(200),
    user_agent VARCHAR(2000),
    browser VARCHAR(100),
    os VARCHAR(100),
    device_type VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    message VARCHAR(2000),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_login_log IS '登录日志表';
COMMENT ON COLUMN sys_login_log.user_id IS '用户ID';
COMMENT ON COLUMN sys_login_log.username IS '用户名';
COMMENT ON COLUMN sys_login_log.real_name IS '真实姓名';
COMMENT ON COLUMN sys_login_log.login_ip IS '登录IP';
COMMENT ON COLUMN sys_login_log.login_location IS '登录地点';
COMMENT ON COLUMN sys_login_log.user_agent IS '用户代理';
COMMENT ON COLUMN sys_login_log.browser IS '浏览器';
COMMENT ON COLUMN sys_login_log.os IS '操作系统';
COMMENT ON COLUMN sys_login_log.device_type IS '设备类型: PC, MOBILE, TABLET';
COMMENT ON COLUMN sys_login_log.status IS '状态: SUCCESS, FAILURE';
COMMENT ON COLUMN sys_login_log.message IS '登录结果消息';
COMMENT ON COLUMN sys_login_log.login_time IS '登录时间';
COMMENT ON COLUMN sys_login_log.logout_time IS '登出时间';

CREATE INDEX idx_sys_login_log_user_id ON sys_login_log(user_id);
CREATE INDEX idx_sys_login_log_username ON sys_login_log(username);
CREATE INDEX idx_sys_login_log_login_time ON sys_login_log(login_time);
CREATE INDEX idx_sys_login_log_status ON sys_login_log(status);

-- ============================================
-- 二、客户管理模块
-- ============================================

-- 客户表
CREATE TABLE IF NOT EXISTS crm_client (
    id BIGSERIAL PRIMARY KEY,
    client_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    client_type VARCHAR(20) NOT NULL,
    credit_code VARCHAR(50),
    id_card VARCHAR(50),
    legal_representative VARCHAR(100),
    registered_address VARCHAR(500),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    industry VARCHAR(100),
    source VARCHAR(50),
    level VARCHAR(20) DEFAULT 'B',
    category VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'POTENTIAL',
    originator_id BIGINT,
    responsible_lawyer_id BIGINT,
    first_cooperation_date DATE,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_crm_client_client_no ON crm_client(client_no);
CREATE INDEX idx_crm_client_name ON crm_client(name);
CREATE INDEX idx_crm_client_type ON crm_client(client_type);
CREATE INDEX idx_crm_client_status ON crm_client(status);
CREATE INDEX idx_crm_client_responsible_lawyer ON crm_client(responsible_lawyer_id);

COMMENT ON TABLE crm_client IS '客户表';
COMMENT ON COLUMN crm_client.client_no IS '客户编号';
COMMENT ON COLUMN crm_client.name IS '客户名称';
COMMENT ON COLUMN crm_client.client_type IS '客户类型：INDIVIDUAL-个人, ENTERPRISE-企业, GOVERNMENT-政府机关, OTHER-其他';
COMMENT ON COLUMN crm_client.credit_code IS '统一社会信用代码（企业）';
COMMENT ON COLUMN crm_client.id_card IS '身份证号（个人）';
COMMENT ON COLUMN crm_client.status IS '状态：POTENTIAL-潜在, ACTIVE-正式, INACTIVE-休眠, BLACKLIST-黑名单';
COMMENT ON COLUMN crm_client.level IS '客户级别：A-重要, B-普通, C-一般';
COMMENT ON COLUMN crm_client.category IS '客户分类：VIP-重要客户, NORMAL-普通客户, POTENTIAL-潜在客户';

-- ============================================
-- 三、案件/项目管理模块
-- ============================================

-- 案件/项目表
CREATE TABLE IF NOT EXISTS matter (
    id BIGSERIAL PRIMARY KEY,
    matter_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    matter_type VARCHAR(20) NOT NULL,
    business_type VARCHAR(50),
    client_id BIGINT NOT NULL,
    opposing_party VARCHAR(200),
    opposing_lawyer_name VARCHAR(100),
    opposing_lawyer_license_no VARCHAR(50),
    opposing_lawyer_firm VARCHAR(200),
    opposing_lawyer_phone VARCHAR(20),
    opposing_lawyer_email VARCHAR(100),
    description TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    originator_id BIGINT,
    lead_lawyer_id BIGINT,
    department_id BIGINT,
    fee_type VARCHAR(20),
    estimated_fee DECIMAL(15,2),
    actual_fee DECIMAL(15,2),
    filing_date DATE,
    expected_end_date DATE,
    actual_end_date DATE,
    claim_amount DECIMAL(15,2),
    outcome TEXT,
    contract_id BIGINT,
    remark TEXT,
    conflict_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_matter_matter_no ON matter(matter_no);
CREATE INDEX idx_matter_name ON matter(name);
CREATE INDEX idx_matter_client_id ON matter(client_id);
CREATE INDEX idx_matter_status ON matter(status);
CREATE INDEX idx_matter_lead_lawyer ON matter(lead_lawyer_id);
CREATE INDEX idx_matter_type ON matter(matter_type);

COMMENT ON TABLE matter IS '案件/项目表';
COMMENT ON COLUMN matter.matter_no IS '案件编号';
COMMENT ON COLUMN matter.name IS '案件名称';
COMMENT ON COLUMN matter.matter_type IS '案件类型：LITIGATION-诉讼, NON_LITIGATION-非诉';
COMMENT ON COLUMN matter.status IS '状态：DRAFT-草稿, PENDING-待审批, ACTIVE-进行中, SUSPENDED-暂停, CLOSED-结案, ARCHIVED-归档';
COMMENT ON COLUMN matter.fee_type IS '收费方式：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理, MIXED-混合收费';
COMMENT ON COLUMN matter.conflict_status IS '利冲检查状态：PENDING-待检查, PASSED-已通过, FAILED-未通过';

-- 案件参与人表
CREATE TABLE IF NOT EXISTS matter_participant (
    id BIGSERIAL PRIMARY KEY,
    matter_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    commission_rate DECIMAL(5,2),
    is_originator BOOLEAN DEFAULT FALSE,
    join_date DATE,
    exit_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(matter_id, user_id)
);

CREATE INDEX idx_matter_participant_matter_id ON matter_participant(matter_id);
CREATE INDEX idx_matter_participant_user_id ON matter_participant(user_id);
CREATE INDEX idx_matter_participant_status ON matter_participant(status);

COMMENT ON TABLE matter_participant IS '案件参与人表';
COMMENT ON COLUMN matter_participant.role IS '角色：LEAD-主办律师, CO_COUNSEL-协办律师, PARALEGAL-律师助理, TRAINEE-实习律师';
COMMENT ON COLUMN matter_participant.commission_rate IS '提成比例（百分比）';
COMMENT ON COLUMN matter_participant.is_originator IS '是否案源人';

-- ============================================
-- 四、财务管理模块
-- ============================================

-- 委托合同表
CREATE TABLE IF NOT EXISTS finance_contract (
    id BIGSERIAL PRIMARY KEY,
    contract_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    client_id BIGINT NOT NULL,
    matter_id BIGINT,
    contract_type VARCHAR(20) NOT NULL,
    fee_type VARCHAR(20) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'CNY',
    sign_date DATE NOT NULL,
    effective_date DATE,
    expiry_date DATE,
    status VARCHAR(20) DEFAULT 'DRAFT',
    signer_id BIGINT,
    department_id BIGINT,
    file_url VARCHAR(500),
    payment_terms TEXT,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_finance_contract_contract_no ON finance_contract(contract_no);
CREATE INDEX idx_finance_contract_client_id ON finance_contract(client_id);
CREATE INDEX idx_finance_contract_matter_id ON finance_contract(matter_id);
CREATE INDEX idx_finance_contract_status ON finance_contract(status);

COMMENT ON TABLE finance_contract IS '委托合同表';
COMMENT ON COLUMN finance_contract.contract_no IS '合同编号';
COMMENT ON COLUMN finance_contract.contract_type IS '合同类型：SERVICE-服务合同, RETAINER-常年法顾, LITIGATION-诉讼代理, NON_LITIGATION-非诉项目';
COMMENT ON COLUMN finance_contract.fee_type IS '收费方式：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理, MIXED-混合收费';
COMMENT ON COLUMN finance_contract.status IS '合同状态：DRAFT-草稿, PENDING-待审批, ACTIVE-生效中, REJECTED-已拒绝, TERMINATED-已终止, COMPLETED-已完成, EXPIRED-已过期';

-- 收费记录表
CREATE TABLE IF NOT EXISTS finance_fee (
    id BIGSERIAL PRIMARY KEY,
    fee_no VARCHAR(50) NOT NULL UNIQUE,
    contract_id BIGINT NOT NULL,
    matter_id BIGINT,
    client_id BIGINT NOT NULL,
    fee_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) DEFAULT 0,
    planned_date DATE NOT NULL,
    actual_date DATE,
    status VARCHAR(20) DEFAULT 'PENDING',
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_finance_fee_fee_no ON finance_fee(fee_no);
CREATE INDEX idx_finance_fee_contract_id ON finance_fee(contract_id);
CREATE INDEX idx_finance_fee_matter_id ON finance_fee(matter_id);
CREATE INDEX idx_finance_fee_client_id ON finance_fee(client_id);
CREATE INDEX idx_finance_fee_status ON finance_fee(status);

COMMENT ON TABLE finance_fee IS '收费记录表';
COMMENT ON COLUMN finance_fee.fee_type IS '收费类型：RETAINER-预付款, PROGRESS-进度款, FINAL-尾款, OTHER-其他';
COMMENT ON COLUMN finance_fee.status IS '状态：PENDING-待收, PARTIAL-部分收款, PAID-已收清, CANCELLED-已取消';

-- 收款记录表
CREATE TABLE IF NOT EXISTS finance_payment (
    id BIGSERIAL PRIMARY KEY,
    payment_no VARCHAR(50) NOT NULL UNIQUE,
    fee_id BIGINT NOT NULL,
    contract_id BIGINT,
    matter_id BIGINT,
    client_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_account VARCHAR(100),
    receipt_no VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING',
    confirmed_at TIMESTAMP,
    confirmed_by BIGINT,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_finance_payment_payment_no ON finance_payment(payment_no);
CREATE INDEX idx_finance_payment_fee_id ON finance_payment(fee_id);
CREATE INDEX idx_finance_payment_contract_id ON finance_payment(contract_id);
CREATE INDEX idx_finance_payment_matter_id ON finance_payment(matter_id);
CREATE INDEX idx_finance_payment_client_id ON finance_payment(client_id);
CREATE INDEX idx_finance_payment_status ON finance_payment(status);
CREATE INDEX idx_finance_payment_date ON finance_payment(payment_date);

COMMENT ON TABLE finance_payment IS '收款记录表';
COMMENT ON COLUMN finance_payment.payment_method IS '付款方式：BANK_TRANSFER-银行转账, CASH-现金, CHECK-支票, ALIPAY-支付宝, WECHAT-微信, OTHER-其他';
COMMENT ON COLUMN finance_payment.status IS '状态：PENDING-待确认, CONFIRMED-已确认, CANCELLED-已取消';
