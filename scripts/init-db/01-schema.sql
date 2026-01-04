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
    
    -- 开票信息
    invoice_title VARCHAR(200),
    invoice_tax_no VARCHAR(50),
    invoice_address VARCHAR(500),
    invoice_phone VARCHAR(50),
    invoice_bank_name VARCHAR(200),
    invoice_bank_account VARCHAR(50),
    
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
COMMENT ON COLUMN crm_client.invoice_title IS '发票抬头';
COMMENT ON COLUMN crm_client.invoice_tax_no IS '纳税人识别号';
COMMENT ON COLUMN crm_client.invoice_address IS '开票地址';
COMMENT ON COLUMN crm_client.invoice_phone IS '开票电话';
COMMENT ON COLUMN crm_client.invoice_bank_name IS '开户银行';
COMMENT ON COLUMN crm_client.invoice_bank_account IS '银行账号';

-- ============================================
-- 三、案件/项目管理模块
-- ============================================

-- 案件/项目表
CREATE TABLE IF NOT EXISTS matter (
    id BIGSERIAL PRIMARY KEY,
    matter_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    matter_type VARCHAR(20) NOT NULL,
    case_type VARCHAR(30),
    cause_of_action VARCHAR(50),
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
CREATE INDEX idx_matter_case_type ON matter(case_type);

COMMENT ON TABLE matter IS '案件/项目表';
COMMENT ON COLUMN matter.matter_no IS '案件编号';
COMMENT ON COLUMN matter.name IS '案件名称';
COMMENT ON COLUMN matter.matter_type IS '项目大类：LITIGATION-诉讼案件, NON_LITIGATION-非诉项目';
COMMENT ON COLUMN matter.case_type IS '案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务';
COMMENT ON COLUMN matter.cause_of_action IS '案由代码';
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

-- 合同模板表
CREATE TABLE IF NOT EXISTS contract_template (
    id BIGSERIAL PRIMARY KEY,
    template_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    contract_type VARCHAR(20) NOT NULL,
    fee_type VARCHAR(20),
    content TEXT,
    clauses TEXT,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_contract_template_contract_type ON contract_template(contract_type);
CREATE INDEX idx_contract_template_status ON contract_template(status);

COMMENT ON TABLE contract_template IS '合同模板表';
COMMENT ON COLUMN contract_template.template_no IS '模板编号';
COMMENT ON COLUMN contract_template.name IS '模板名称';
COMMENT ON COLUMN contract_template.contract_type IS '合同类型：SERVICE-服务合同, RETAINER-常年法顾, LITIGATION-诉讼代理, NON_LITIGATION-非诉项目';
COMMENT ON COLUMN contract_template.fee_type IS '默认收费方式';
COMMENT ON COLUMN contract_template.content IS '模板内容（支持变量替换）';
COMMENT ON COLUMN contract_template.clauses IS '标准条款（JSON格式）';
COMMENT ON COLUMN contract_template.description IS '模板说明';
COMMENT ON COLUMN contract_template.status IS '状态：ACTIVE-启用, INACTIVE-停用';

-- 委托合同表
CREATE TABLE IF NOT EXISTS finance_contract (
    id BIGSERIAL PRIMARY KEY,
    contract_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    template_id BIGINT,
    client_id BIGINT NOT NULL,
    matter_id BIGINT,
    contract_type VARCHAR(20) NOT NULL,
    fee_type VARCHAR(20) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'CNY',
    sign_date DATE,
    effective_date DATE,
    expiry_date DATE,
    status VARCHAR(20) DEFAULT 'DRAFT',
    signer_id BIGINT,
    department_id BIGINT,
    content TEXT,
    file_url VARCHAR(500),
    payment_terms TEXT,
    remark TEXT,
    -- 扩展字段（合同模块完善）
    case_type VARCHAR(30),
    cause_of_action VARCHAR(50),
    trial_stage VARCHAR(50),
    claim_amount DECIMAL(15,2),
    jurisdiction_court VARCHAR(200),
    opposing_party VARCHAR(200),
    conflict_check_status VARCHAR(20) DEFAULT 'NOT_REQUIRED',
    archive_status VARCHAR(20) DEFAULT 'NOT_ARCHIVED',
    advance_travel_fee DECIMAL(15,2),
    risk_ratio DECIMAL(5,2),
    seal_record TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_finance_contract_contract_no ON finance_contract(contract_no);
CREATE INDEX idx_finance_contract_client_id ON finance_contract(client_id);
CREATE INDEX idx_finance_contract_matter_id ON finance_contract(matter_id);
CREATE INDEX idx_finance_contract_template_id ON finance_contract(template_id);
CREATE INDEX idx_finance_contract_status ON finance_contract(status);
CREATE INDEX idx_finance_contract_case_type ON finance_contract(case_type);
CREATE INDEX idx_finance_contract_trial_stage ON finance_contract(trial_stage);
CREATE INDEX idx_finance_contract_conflict_check_status ON finance_contract(conflict_check_status);

COMMENT ON TABLE finance_contract IS '委托合同表';
COMMENT ON COLUMN finance_contract.contract_no IS '合同编号';
COMMENT ON COLUMN finance_contract.template_id IS '使用的模板ID';
COMMENT ON COLUMN finance_contract.content IS '合同内容（基于模板生成）';
COMMENT ON COLUMN finance_contract.contract_type IS '合同类型：SERVICE-服务合同, RETAINER-常年法顾, LITIGATION-诉讼代理, NON_LITIGATION-非诉项目';
COMMENT ON COLUMN finance_contract.fee_type IS '收费方式：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理, MIXED-混合收费';
COMMENT ON COLUMN finance_contract.case_type IS '案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政等';
COMMENT ON COLUMN finance_contract.cause_of_action IS '案由代码';
COMMENT ON COLUMN finance_contract.status IS '合同状态：DRAFT-草稿, PENDING-待审批, ACTIVE-生效中, REJECTED-已拒绝, TERMINATED-已终止, COMPLETED-已完成, EXPIRED-已过期';
COMMENT ON COLUMN finance_contract.trial_stage IS '审理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行, NON_LITIGATION-非诉';
COMMENT ON COLUMN finance_contract.claim_amount IS '标的金额';
COMMENT ON COLUMN finance_contract.jurisdiction_court IS '管辖法院';
COMMENT ON COLUMN finance_contract.opposing_party IS '对方当事人';
COMMENT ON COLUMN finance_contract.conflict_check_status IS '利冲审查状态：PENDING-待审查, PASSED-已通过, FAILED-未通过, NOT_REQUIRED-无需审查';
COMMENT ON COLUMN finance_contract.archive_status IS '归档状态：NOT_ARCHIVED-未归档, ARCHIVED-已归档, DESTROYED-已销毁';
COMMENT ON COLUMN finance_contract.advance_travel_fee IS '预支差旅费';
COMMENT ON COLUMN finance_contract.risk_ratio IS '风险代理比例（0-100）';
COMMENT ON COLUMN finance_contract.seal_record IS '印章使用记录（JSON格式）';

-- 合同付款计划表
CREATE TABLE IF NOT EXISTS contract_payment_schedule (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    phase_name VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    percentage DECIMAL(5,2),
    planned_date DATE,
    actual_date DATE,
    status VARCHAR(20) DEFAULT 'PENDING',
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_payment_schedule_contract FOREIGN KEY (contract_id) REFERENCES finance_contract(id)
);

CREATE INDEX idx_payment_schedule_contract ON contract_payment_schedule(contract_id);
CREATE INDEX idx_payment_schedule_status ON contract_payment_schedule(status);
CREATE INDEX idx_payment_schedule_planned_date ON contract_payment_schedule(planned_date);

COMMENT ON TABLE contract_payment_schedule IS '合同付款计划表';
COMMENT ON COLUMN contract_payment_schedule.contract_id IS '合同ID';
COMMENT ON COLUMN contract_payment_schedule.phase_name IS '阶段名称（如：签约款、一审结束、执行到位）';
COMMENT ON COLUMN contract_payment_schedule.amount IS '付款金额';
COMMENT ON COLUMN contract_payment_schedule.percentage IS '比例（风险代理时使用）';
COMMENT ON COLUMN contract_payment_schedule.planned_date IS '计划收款日期';
COMMENT ON COLUMN contract_payment_schedule.actual_date IS '实际收款日期';
COMMENT ON COLUMN contract_payment_schedule.status IS '状态：PENDING-待收, PARTIAL-部分收款, PAID-已收清, CANCELLED-已取消';

-- 合同参与人表
CREATE TABLE IF NOT EXISTS contract_participant (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    commission_rate DECIMAL(5,2),
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_participant_contract FOREIGN KEY (contract_id) REFERENCES finance_contract(id),
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    UNIQUE(contract_id, user_id)
);

CREATE INDEX idx_contract_participant_contract ON contract_participant(contract_id);
CREATE INDEX idx_contract_participant_user ON contract_participant(user_id);
CREATE INDEX idx_contract_participant_role ON contract_participant(role);

COMMENT ON TABLE contract_participant IS '合同参与人表';
COMMENT ON COLUMN contract_participant.contract_id IS '合同ID';
COMMENT ON COLUMN contract_participant.user_id IS '用户ID';
COMMENT ON COLUMN contract_participant.role IS '角色：LEAD-承办律师, CO_COUNSEL-协办律师, ORIGINATOR-案源人, PARALEGAL-律师助理';
COMMENT ON COLUMN contract_participant.commission_rate IS '提成比例（百分比）';

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
    currency VARCHAR(10) DEFAULT 'CNY',
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
    currency VARCHAR(10) DEFAULT 'CNY',
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

-- ============================================
-- 五、出函/介绍信管理
-- ============================================

-- 出函模板表
CREATE TABLE IF NOT EXISTS letter_template (
    id BIGSERIAL PRIMARY KEY,
    template_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    letter_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE letter_template IS '出函模板表';
COMMENT ON COLUMN letter_template.template_no IS '模板编号';
COMMENT ON COLUMN letter_template.name IS '模板名称';
COMMENT ON COLUMN letter_template.letter_type IS '函件类型：INTRODUCTION-介绍信, MEETING-会见函, INVESTIGATION-调查函, FILE_REVIEW-阅卷函, LEGAL_OPINION-法律意见函, OTHER-其他';
COMMENT ON COLUMN letter_template.content IS '模板内容（支持变量如${lawyerName}、${matterName}、${targetUnit}等）';
COMMENT ON COLUMN letter_template.status IS '状态：ACTIVE-启用, DISABLED-停用';

-- 出函申请表
CREATE TABLE IF NOT EXISTS letter_application (
    id BIGSERIAL PRIMARY KEY,
    application_no VARCHAR(50) NOT NULL UNIQUE,
    template_id BIGINT NOT NULL,
    matter_id BIGINT NOT NULL,
    client_id BIGINT,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(50),
    department_id BIGINT,
    letter_type VARCHAR(50) NOT NULL,
    target_unit VARCHAR(200) NOT NULL,
    target_contact VARCHAR(50),
    target_phone VARCHAR(50),
    target_address VARCHAR(500),
    purpose VARCHAR(500) NOT NULL,
    lawyer_ids VARCHAR(500),
    lawyer_names VARCHAR(500),
    content TEXT,
    copies INT DEFAULT 1,
    expected_date DATE,
    status VARCHAR(20) DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    approval_comment VARCHAR(500),
    printed_by BIGINT,
    printed_at TIMESTAMP,
    received_by BIGINT,
    received_at TIMESTAMP,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_letter_application_no ON letter_application(application_no);
CREATE INDEX idx_letter_application_matter ON letter_application(matter_id);
CREATE INDEX idx_letter_application_applicant ON letter_application(applicant_id);
CREATE INDEX idx_letter_application_status ON letter_application(status);

COMMENT ON TABLE letter_application IS '出函申请表';
COMMENT ON COLUMN letter_application.application_no IS '申请编号';
COMMENT ON COLUMN letter_application.template_id IS '使用的模板ID';
COMMENT ON COLUMN letter_application.matter_id IS '关联项目ID';
COMMENT ON COLUMN letter_application.target_unit IS '接收单位（如：XX市中级人民法院）';
COMMENT ON COLUMN letter_application.purpose IS '出函事由';
COMMENT ON COLUMN letter_application.lawyer_ids IS '出函律师ID列表（逗号分隔）';
COMMENT ON COLUMN letter_application.lawyer_names IS '出函律师姓名列表';
COMMENT ON COLUMN letter_application.content IS '生成的函件内容';
COMMENT ON COLUMN letter_application.copies IS '份数';
COMMENT ON COLUMN letter_application.status IS '状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, RETURNED-已退回, PRINTED-已打印, RECEIVED-已领取, CANCELLED-已取消';
COMMENT ON COLUMN letter_application.printed_by IS '打印人（行政）';
COMMENT ON COLUMN letter_application.received_by IS '领取人（律师）';

-- ============================================
-- 初始化出函模板数据
-- ============================================

INSERT INTO letter_template (template_no, name, letter_type, content, description, status, sort_order, created_at, updated_at) VALUES
('LT000001', '律师会见介绍信', 'MEETING', 
'介绍信

${targetUnit}：

兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}（案号：${matterNo}）一案进行会见工作。

请予接洽为荷。

此致

敬礼

                                    XX律师事务所
                                    ${date}', 
'用于律师前往看守所、监狱等场所会见当事人', 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('LT000002', '律师调查介绍信', 'INVESTIGATION', 
'介绍信

${targetUnit}：

兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}（案号：${matterNo}）一案进行调查取证工作。

请予协助为荷。

此致

敬礼

                                    XX律师事务所
                                    ${date}', 
'用于律师前往相关单位调查取证', 'ACTIVE', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('LT000003', '律师阅卷介绍信', 'FILE_REVIEW', 
'介绍信

${targetUnit}：

兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}（案号：${matterNo}）一案进行阅卷工作。

请予接洽为荷。

此致

敬礼

                                    XX律师事务所
                                    ${date}', 
'用于律师前往法院、检察院阅卷', 'ACTIVE', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('LT000004', '通用介绍信', 'INTRODUCTION', 
'介绍信

${targetUnit}：

兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}一案办理相关事宜。

请予接洽为荷。

此致

敬礼

                                    XX律师事务所
                                    ${date}', 
'通用介绍信模板', 'ACTIVE', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

