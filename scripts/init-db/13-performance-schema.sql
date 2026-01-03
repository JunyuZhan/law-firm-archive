-- =====================================================
-- 绩效考核模块数据库表结构
-- 模块：M9 人力资源 - 绩效考核
-- 功能：M9-020~025 考核指标、考核任务、自评互评、上级评价、考核结果
-- =====================================================

-- 考核指标表
CREATE TABLE IF NOT EXISTS hr_performance_indicator (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                     -- 指标名称
    code VARCHAR(50),                               -- 指标编码
    category VARCHAR(20) NOT NULL,                  -- 分类：WORK-工作业绩, ABILITY-能力素质, ATTITUDE-工作态度, OTHER-其他
    description TEXT,                               -- 指标描述
    weight DECIMAL(5,2) NOT NULL,                   -- 权重（百分比）
    max_score INTEGER DEFAULT 100,                  -- 满分
    scoring_criteria TEXT,                          -- 评分标准说明
    applicable_role VARCHAR(20) DEFAULT 'ALL',      -- 适用角色：ALL-全部, LAWYER-律师, ASSISTANT-助理, ADMIN-行政
    sort_order INTEGER DEFAULT 0,                   -- 排序
    status VARCHAR(20) DEFAULT 'ACTIVE',            -- 状态：ACTIVE-启用, INACTIVE-停用
    remarks TEXT,                                   -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

-- 考核任务表
CREATE TABLE IF NOT EXISTS hr_performance_task (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,                     -- 考核任务名称
    period_type VARCHAR(20) NOT NULL,               -- 周期类型：MONTHLY-月度, QUARTERLY-季度, YEARLY-年度
    year INTEGER NOT NULL,                          -- 考核年份
    period INTEGER,                                 -- 考核周期（月/季度）
    start_date DATE,                                -- 考核开始日期
    end_date DATE,                                  -- 考核结束日期
    self_eval_deadline DATE,                        -- 自评截止日期
    peer_eval_deadline DATE,                        -- 互评截止日期
    supervisor_eval_deadline DATE,                  -- 上级评价截止日期
    status VARCHAR(20) DEFAULT 'DRAFT',             -- 状态：DRAFT-草稿, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消
    description TEXT,                               -- 考核说明
    remarks TEXT,                                   -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

-- 绩效评价表
CREATE TABLE IF NOT EXISTS hr_performance_evaluation (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,                        -- 考核任务ID
    employee_id BIGINT NOT NULL,                    -- 被考核人ID
    evaluator_id BIGINT NOT NULL,                   -- 评价人ID
    evaluation_type VARCHAR(20) NOT NULL,           -- 评价类型：SELF-自评, PEER-互评, SUPERVISOR-上级评价
    total_score DECIMAL(5,2),                       -- 总分
    grade VARCHAR(10),                              -- 等级：A-优秀, B-良好, C-合格, D-待改进, E-不合格
    comment TEXT,                                   -- 综合评语
    strengths TEXT,                                 -- 优点
    improvements TEXT,                              -- 改进建议
    evaluated_at TIMESTAMP,                         -- 评价时间
    status VARCHAR(20) DEFAULT 'PENDING',           -- 状态：PENDING-待评价, COMPLETED-已完成
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_evaluation_task FOREIGN KEY (task_id) REFERENCES hr_performance_task(id)
);

-- 绩效评分明细表
CREATE TABLE IF NOT EXISTS hr_performance_score (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL,                  -- 评价ID
    indicator_id BIGINT NOT NULL,                   -- 指标ID
    score DECIMAL(5,2),                             -- 得分
    comment TEXT,                                   -- 评语
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_score_evaluation FOREIGN KEY (evaluation_id) REFERENCES hr_performance_evaluation(id),
    CONSTRAINT fk_score_indicator FOREIGN KEY (indicator_id) REFERENCES hr_performance_indicator(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_indicator_category ON hr_performance_indicator(category);
CREATE INDEX IF NOT EXISTS idx_indicator_status ON hr_performance_indicator(status);
CREATE INDEX IF NOT EXISTS idx_task_year ON hr_performance_task(year);
CREATE INDEX IF NOT EXISTS idx_task_status ON hr_performance_task(status);
CREATE INDEX IF NOT EXISTS idx_evaluation_task ON hr_performance_evaluation(task_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_employee ON hr_performance_evaluation(employee_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_evaluator ON hr_performance_evaluation(evaluator_id);
CREATE INDEX IF NOT EXISTS idx_score_evaluation ON hr_performance_score(evaluation_id);

-- 添加表注释
COMMENT ON TABLE hr_performance_indicator IS '考核指标表';
COMMENT ON TABLE hr_performance_task IS '考核任务表';
COMMENT ON TABLE hr_performance_evaluation IS '绩效评价表';
COMMENT ON TABLE hr_performance_score IS '绩效评分明细表';

-- 插入默认考核指标
INSERT INTO hr_performance_indicator (name, code, category, description, weight, max_score, scoring_criteria, applicable_role, sort_order, status)
VALUES 
    ('案件办理质量', 'WORK_001', 'WORK', '案件办理的专业水平和结果质量', 25.00, 100, '90-100分：案件处理专业、高效，客户满意度高；80-89分：案件处理规范，基本达到预期；70-79分：案件处理一般，有待提升；60-69分：案件处理存在问题；60分以下：案件处理不合格', 'LAWYER', 1, 'ACTIVE'),
    ('工作效率', 'WORK_002', 'WORK', '工作任务完成的及时性和效率', 20.00, 100, '按时完成率90%以上得90-100分；80-89%得80-89分；70-79%得70-79分；60-69%得60-69分；60%以下得60分以下', 'ALL', 2, 'ACTIVE'),
    ('专业能力', 'ABILITY_001', 'ABILITY', '法律专业知识和技能水平', 20.00, 100, '根据专业考试成绩、培训参与度、知识更新情况综合评定', 'LAWYER', 3, 'ACTIVE'),
    ('沟通协作', 'ABILITY_002', 'ABILITY', '团队协作和沟通能力', 15.00, 100, '根据团队反馈、协作项目表现综合评定', 'ALL', 4, 'ACTIVE'),
    ('工作态度', 'ATTITUDE_001', 'ATTITUDE', '工作积极性、责任心', 10.00, 100, '根据出勤情况、工作主动性、责任心表现综合评定', 'ALL', 5, 'ACTIVE'),
    ('客户服务', 'ATTITUDE_002', 'ATTITUDE', '客户服务意识和满意度', 10.00, 100, '根据客户反馈、服务态度综合评定', 'ALL', 6, 'ACTIVE');
