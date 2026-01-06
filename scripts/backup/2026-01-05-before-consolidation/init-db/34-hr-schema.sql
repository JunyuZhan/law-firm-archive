-- =====================================================
-- 人力资源模块数据库表结构
-- 模块：M9 人力资源
-- 包含：员工档案、转正管理、离职管理
-- =====================================================

-- -----------------------------------------------------
-- 1. 员工档案表 (hr_employee)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_employee (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,                              -- 关联用户ID
    employee_no VARCHAR(50) UNIQUE,              -- 工号
    gender VARCHAR(20),                         -- 性别: MALE-男, FEMALE-女
    birth_date DATE,                            -- 出生日期
    id_card VARCHAR(18),                        -- 身份证号
    nationality VARCHAR(50),                    -- 国籍
    native_place VARCHAR(100),                  -- 籍贯
    political_status VARCHAR(50),               -- 政治面貌
    education VARCHAR(50),                      -- 学历
    major VARCHAR(100),                         -- 专业
    graduation_school VARCHAR(200),             -- 毕业院校
    graduation_date DATE,                       -- 毕业日期
    emergency_contact VARCHAR(50),              -- 紧急联系人
    emergency_phone VARCHAR(20),                -- 紧急联系电话
    address VARCHAR(500),                       -- 家庭住址
    lawyer_license_no VARCHAR(50),              -- 律师执业证号
    license_issue_date DATE,                    -- 执业证发证日期
    license_expire_date DATE,                   -- 执业证到期日期
    license_status VARCHAR(20),                 -- 执业证状态: VALID-有效, EXPIRED-已过期, SUSPENDED-已暂停
    practice_area VARCHAR(200),                 -- 执业领域
    practice_years INT,                         -- 执业年限
    position VARCHAR(50),                      -- 职位
    level VARCHAR(50),                         -- 职级
    entry_date DATE,                           -- 入职日期
    probation_end_date DATE,                   -- 试用期结束日期
    regular_date DATE,                         -- 转正日期
    resignation_date DATE,                     -- 离职日期
    resignation_reason VARCHAR(500),           -- 离职原因
    work_status VARCHAR(20) DEFAULT 'ACTIVE',  -- 工作状态: ACTIVE-在职, PROBATION-试用, RESIGNED-离职, RETIRED-退休
    remark VARCHAR(1000),                      -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_hr_employee_user_id ON hr_employee(user_id);
CREATE INDEX idx_hr_employee_employee_no ON hr_employee(employee_no);
CREATE INDEX idx_hr_employee_work_status ON hr_employee(work_status);

COMMENT ON TABLE hr_employee IS '员工档案表';
COMMENT ON COLUMN hr_employee.user_id IS '关联用户ID';
COMMENT ON COLUMN hr_employee.employee_no IS '工号';
COMMENT ON COLUMN hr_employee.work_status IS '工作状态: ACTIVE-在职, PROBATION-试用, RESIGNED-离职, RETIRED-退休';

-- -----------------------------------------------------
-- 2. 转正申请表 (hr_regularization)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_regularization (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,                 -- 员工ID
    application_no VARCHAR(30) NOT NULL UNIQUE, -- 申请编号
    probation_start_date DATE,                  -- 试用期开始日期
    probation_end_date DATE,                    -- 试用期结束日期
    application_date DATE NOT NULL,             -- 申请日期
    expected_regular_date DATE,                 -- 预计转正日期
    self_evaluation TEXT,                       -- 自我评价
    supervisor_evaluation TEXT,                 -- 上级评价
    hr_evaluation TEXT,                         -- HR评价
    status VARCHAR(20) DEFAULT 'PENDING',     -- 状态: PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝
    approver_id BIGINT,                         -- 审批人ID
    approved_date DATE,                         -- 审批日期
    comment VARCHAR(500),                       -- 审批意见
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_hr_regularization_employee_id ON hr_regularization(employee_id);
CREATE INDEX idx_hr_regularization_application_no ON hr_regularization(application_no);
CREATE INDEX idx_hr_regularization_status ON hr_regularization(status);
CREATE INDEX idx_hr_regularization_application_date ON hr_regularization(application_date);

COMMENT ON TABLE hr_regularization IS '转正申请表';
COMMENT ON COLUMN hr_regularization.status IS '状态: PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝';

-- -----------------------------------------------------
-- 3. 离职申请表 (hr_resignation)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_resignation (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,                -- 员工ID
    application_no VARCHAR(30) NOT NULL UNIQUE, -- 申请编号
    resignation_type VARCHAR(20),               -- 离职类型: VOLUNTARY-主动离职, DISMISSED-辞退, RETIREMENT-退休, CONTRACT_EXPIRED-合同到期
    resignation_date DATE NOT NULL,            -- 申请日期
    last_work_date DATE,                        -- 最后工作日
    reason VARCHAR(1000),                       -- 离职原因
    handover_person_id BIGINT,                  -- 交接人ID
    handover_status VARCHAR(20) DEFAULT 'PENDING', -- 交接状态: PENDING-待交接, IN_PROGRESS-交接中, COMPLETED-已完成
    handover_note TEXT,                         -- 交接说明
    status VARCHAR(20) DEFAULT 'PENDING',       -- 状态: PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝, COMPLETED-已完成
    approver_id BIGINT,                         -- 审批人ID
    approved_date DATE,                         -- 审批日期
    comment VARCHAR(500),                       -- 审批意见
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_hr_resignation_employee_id ON hr_resignation(employee_id);
CREATE INDEX idx_hr_resignation_application_no ON hr_resignation(application_no);
CREATE INDEX idx_hr_resignation_status ON hr_resignation(status);
CREATE INDEX idx_hr_resignation_resignation_date ON hr_resignation(resignation_date);
CREATE INDEX idx_hr_resignation_handover_status ON hr_resignation(handover_status);

COMMENT ON TABLE hr_resignation IS '离职申请表';
COMMENT ON COLUMN hr_resignation.resignation_type IS '离职类型: VOLUNTARY-主动离职, DISMISSED-辞退, RETIREMENT-退休, CONTRACT_EXPIRED-合同到期';
COMMENT ON COLUMN hr_resignation.handover_status IS '交接状态: PENDING-待交接, IN_PROGRESS-交接中, COMPLETED-已完成';
COMMENT ON COLUMN hr_resignation.status IS '状态: PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝, COMPLETED-已完成';

-- 外键约束（可选，根据实际需求决定是否添加）
-- ALTER TABLE hr_regularization ADD CONSTRAINT fk_regularization_employee 
--     FOREIGN KEY (employee_id) REFERENCES hr_employee(id);
-- ALTER TABLE hr_resignation ADD CONSTRAINT fk_resignation_employee 
--     FOREIGN KEY (employee_id) REFERENCES hr_employee(id);

COMMIT;

