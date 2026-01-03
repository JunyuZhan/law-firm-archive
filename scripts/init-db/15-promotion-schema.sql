-- =====================================================
-- 晋升发展模块数据库表设计
-- M9-030: 晋升通道
-- M9-031: 晋升申请
-- M9-032: 评审流程
-- M9-033: 发展规划
-- =====================================================

-- 1. 职级通道表
CREATE TABLE IF NOT EXISTS hr_career_level (
    id BIGSERIAL PRIMARY KEY,
    level_code VARCHAR(50) NOT NULL UNIQUE,            -- 职级编码
    level_name VARCHAR(100) NOT NULL,                  -- 职级名称
    level_order INTEGER NOT NULL,                      -- 职级顺序（数字越大级别越高）
    category VARCHAR(50) NOT NULL,                     -- 通道类别：LAWYER-律师通道, ADMIN-行政通道, TECH-技术通道
    
    description VARCHAR(500),                          -- 职级描述
    
    -- 晋升条件
    min_work_years INTEGER,                            -- 最低工作年限
    min_matter_count INTEGER,                          -- 最低案件数量
    min_revenue DECIMAL(15,2),                         -- 最低创收金额
    required_certificates TEXT,                        -- 所需证书（JSON数组）
    other_requirements TEXT,                           -- 其他要求
    
    -- 薪酬范围
    salary_min DECIMAL(15,2),                          -- 薪酬下限
    salary_max DECIMAL(15,2),                          -- 薪酬上限
    
    status VARCHAR(20) DEFAULT 'ACTIVE',               -- 状态：ACTIVE-启用, INACTIVE-停用
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

-- 2. 晋升申请表
CREATE TABLE IF NOT EXISTS hr_promotion_application (
    id BIGSERIAL PRIMARY KEY,
    application_no VARCHAR(50) NOT NULL UNIQUE,        -- 申请编号
    
    employee_id BIGINT NOT NULL,                       -- 申请人ID
    employee_name VARCHAR(50),                         -- 申请人姓名
    department_id BIGINT,                              -- 部门ID
    department_name VARCHAR(100),                      -- 部门名称
    
    current_level_id BIGINT,                           -- 当前职级ID
    current_level_name VARCHAR(100),                   -- 当前职级名称
    target_level_id BIGINT NOT NULL,                   -- 目标职级ID
    target_level_name VARCHAR(100),                    -- 目标职级名称
    
    -- 申请信息
    apply_reason TEXT,                                 -- 申请理由
    achievements TEXT,                                 -- 主要业绩
    self_evaluation TEXT,                              -- 自我评价
    attachments TEXT,                                  -- 附件列表（JSON数组）
    
    -- 审批状态
    status VARCHAR(20) DEFAULT 'PENDING',              -- 状态：PENDING-待审批, REVIEWING-评审中, APPROVED-已通过, REJECTED-已拒绝, CANCELLED-已取消
    
    -- 评审结果
    review_score DECIMAL(5,2),                         -- 评审得分
    review_result VARCHAR(20),                         -- 评审结果：PASS-通过, FAIL-不通过
    review_comment TEXT,                               -- 评审意见
    
    -- 审批信息
    approved_by BIGINT,                                -- 审批人ID
    approved_by_name VARCHAR(50),                      -- 审批人姓名
    approved_at TIMESTAMP,                             -- 审批时间
    approval_comment TEXT,                             -- 审批意见
    
    -- 生效信息
    effective_date DATE,                               -- 生效日期
    
    apply_date DATE NOT NULL,                          -- 申请日期
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_promotion_current_level FOREIGN KEY (current_level_id) REFERENCES hr_career_level(id),
    CONSTRAINT fk_promotion_target_level FOREIGN KEY (target_level_id) REFERENCES hr_career_level(id)
);

-- 3. 晋升评审记录表
CREATE TABLE IF NOT EXISTS hr_promotion_review (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,                    -- 晋升申请ID
    
    reviewer_id BIGINT NOT NULL,                       -- 评审人ID
    reviewer_name VARCHAR(50),                         -- 评审人姓名
    reviewer_role VARCHAR(50),                         -- 评审人角色：DIRECT_MANAGER-直属上级, HR-人力资源, PARTNER-合伙人, COMMITTEE-评审委员会
    
    -- 评分项（JSON格式存储各项评分）
    score_details TEXT,                                -- 评分明细
    total_score DECIMAL(5,2),                          -- 总分
    
    -- 评审意见
    review_opinion VARCHAR(20),                        -- 评审意见：APPROVE-同意, REJECT-不同意, ABSTAIN-弃权
    review_comment TEXT,                               -- 评审评语
    
    review_time TIMESTAMP,                             -- 评审时间
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_review_application FOREIGN KEY (application_id) REFERENCES hr_promotion_application(id)
);

-- 4. 个人发展规划表
CREATE TABLE IF NOT EXISTS hr_development_plan (
    id BIGSERIAL PRIMARY KEY,
    plan_no VARCHAR(50) NOT NULL UNIQUE,               -- 规划编号
    
    employee_id BIGINT NOT NULL,                       -- 员工ID
    employee_name VARCHAR(50),                         -- 员工姓名
    
    plan_year INTEGER NOT NULL,                        -- 规划年度
    plan_title VARCHAR(200),                           -- 规划标题
    
    -- 当前状态
    current_level_id BIGINT,                           -- 当前职级ID
    current_level_name VARCHAR(100),                   -- 当前职级名称
    
    -- 目标设定
    target_level_id BIGINT,                            -- 目标职级ID
    target_level_name VARCHAR(100),                    -- 目标职级名称
    target_date DATE,                                  -- 目标达成日期
    
    -- 发展目标（JSON数组）
    career_goals TEXT,                                 -- 职业目标
    skill_goals TEXT,                                  -- 技能目标
    performance_goals TEXT,                            -- 业绩目标
    
    -- 行动计划（JSON数组）
    action_plans TEXT,                                 -- 行动计划
    
    -- 所需支持
    required_training TEXT,                            -- 所需培训
    required_resources TEXT,                           -- 所需资源
    mentor_id BIGINT,                                  -- 导师ID
    mentor_name VARCHAR(50),                           -- 导师姓名
    
    -- 进度跟踪
    progress_percentage INTEGER DEFAULT 0,             -- 完成进度百分比
    progress_notes TEXT,                               -- 进度备注
    
    status VARCHAR(20) DEFAULT 'DRAFT',                -- 状态：DRAFT-草稿, ACTIVE-执行中, COMPLETED-已完成, CANCELLED-已取消
    
    -- 审核信息
    reviewed_by BIGINT,                                -- 审核人ID
    reviewed_by_name VARCHAR(50),                      -- 审核人姓名
    reviewed_at TIMESTAMP,                             -- 审核时间
    review_comment TEXT,                               -- 审核意见
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_plan_current_level FOREIGN KEY (current_level_id) REFERENCES hr_career_level(id),
    CONSTRAINT fk_plan_target_level FOREIGN KEY (target_level_id) REFERENCES hr_career_level(id)
);

-- 5. 发展规划里程碑表
CREATE TABLE IF NOT EXISTS hr_development_milestone (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,                           -- 发展规划ID
    
    milestone_name VARCHAR(200) NOT NULL,              -- 里程碑名称
    description TEXT,                                  -- 描述
    target_date DATE,                                  -- 目标日期
    
    status VARCHAR(20) DEFAULT 'PENDING',              -- 状态：PENDING-待完成, IN_PROGRESS-进行中, COMPLETED-已完成, DELAYED-已延期
    completed_date DATE,                               -- 实际完成日期
    completion_note TEXT,                              -- 完成备注
    
    sort_order INTEGER DEFAULT 0,                      -- 排序
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_milestone_plan FOREIGN KEY (plan_id) REFERENCES hr_development_plan(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_career_level_category ON hr_career_level(category);
CREATE INDEX IF NOT EXISTS idx_career_level_status ON hr_career_level(status);
CREATE INDEX IF NOT EXISTS idx_career_level_order ON hr_career_level(level_order);

CREATE INDEX IF NOT EXISTS idx_promotion_employee ON hr_promotion_application(employee_id);
CREATE INDEX IF NOT EXISTS idx_promotion_status ON hr_promotion_application(status);
CREATE INDEX IF NOT EXISTS idx_promotion_apply_date ON hr_promotion_application(apply_date);

CREATE INDEX IF NOT EXISTS idx_review_application ON hr_promotion_review(application_id);
CREATE INDEX IF NOT EXISTS idx_review_reviewer ON hr_promotion_review(reviewer_id);

CREATE INDEX IF NOT EXISTS idx_plan_employee ON hr_development_plan(employee_id);
CREATE INDEX IF NOT EXISTS idx_plan_year ON hr_development_plan(plan_year);
CREATE INDEX IF NOT EXISTS idx_plan_status ON hr_development_plan(status);

CREATE INDEX IF NOT EXISTS idx_milestone_plan ON hr_development_milestone(plan_id);

-- 添加注释
COMMENT ON TABLE hr_career_level IS '职级通道表';
COMMENT ON TABLE hr_promotion_application IS '晋升申请表';
COMMENT ON TABLE hr_promotion_review IS '晋升评审记录表';
COMMENT ON TABLE hr_development_plan IS '个人发展规划表';
COMMENT ON TABLE hr_development_milestone IS '发展规划里程碑表';

-- 插入默认职级数据
INSERT INTO hr_career_level (level_code, level_name, level_order, category, description, min_work_years, min_matter_count, min_revenue) VALUES
-- 律师通道
('L1', '实习律师', 1, 'LAWYER', '实习期律师，需在指导律师带领下工作', 0, NULL, NULL),
('L2', '初级律师', 2, 'LAWYER', '独立执业律师，可独立处理简单案件', 1, 5, 100000),
('L3', '中级律师', 3, 'LAWYER', '能独立处理复杂案件，可指导初级律师', 3, 20, 500000),
('L4', '高级律师', 4, 'LAWYER', '业务骨干，可带领团队处理重大案件', 5, 50, 1500000),
('L5', '资深律师', 5, 'LAWYER', '行业专家，具有较高知名度和影响力', 8, 100, 3000000),
('L6', '合伙人', 6, 'LAWYER', '律所合伙人，参与律所管理决策', 10, 150, 5000000),

-- 行政通道
('A1', '行政助理', 1, 'ADMIN', '行政基础岗位', 0, NULL, NULL),
('A2', '行政专员', 2, 'ADMIN', '独立负责行政事务', 1, NULL, NULL),
('A3', '行政主管', 3, 'ADMIN', '管理行政团队', 3, NULL, NULL),
('A4', '行政经理', 4, 'ADMIN', '负责行政部门管理', 5, NULL, NULL),
('A5', '行政总监', 5, 'ADMIN', '负责全所行政管理', 8, NULL, NULL);
