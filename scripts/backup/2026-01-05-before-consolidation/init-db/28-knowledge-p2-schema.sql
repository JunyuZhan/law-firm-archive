-- =====================================================
-- 知识库模块P2功能数据库表结构
-- 模块：M10 知识库
-- 包含：案例学习、文章评论、质量管理等
-- =====================================================

-- -----------------------------------------------------
-- 1. 案例学习笔记表 (case_study_note) - M10-013
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS case_study_note (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,                            -- 案例ID
    user_id BIGINT NOT NULL,                            -- 学习人ID
    note_content TEXT,                                  -- 学习笔记内容
    key_points TEXT,                                    -- 关键要点
    personal_insights TEXT,                             -- 个人见解
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_case_study_note UNIQUE (case_id, user_id)
);

CREATE INDEX idx_case_study_note_case ON case_study_note(case_id);
CREATE INDEX idx_case_study_note_user ON case_study_note(user_id);

COMMENT ON TABLE case_study_note IS '案例学习笔记表';

-- -----------------------------------------------------
-- 2. 文章评论表 (article_comment) - M10-022
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS article_comment (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,                         -- 文章ID
    user_id BIGINT NOT NULL,                            -- 评论人ID
    parent_id BIGINT,                                  -- 父评论ID（用于回复）
    content TEXT NOT NULL,                              -- 评论内容
    like_count INT DEFAULT 0,                          -- 点赞数
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_article_comment_article ON article_comment(article_id);
CREATE INDEX idx_article_comment_user ON article_comment(user_id);
CREATE INDEX idx_article_comment_parent ON article_comment(parent_id);

COMMENT ON TABLE article_comment IS '文章评论表';

-- -----------------------------------------------------
-- 3. 文章收藏表 (article_collection) - M10-023
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS article_collection (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,                            -- 用户ID
    article_id BIGINT NOT NULL,                         -- 文章ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_article_collection UNIQUE (user_id, article_id)
);

CREATE INDEX idx_article_collection_user ON article_collection(user_id);
CREATE INDEX idx_article_collection_article ON article_collection(article_id);

COMMENT ON TABLE article_collection IS '文章收藏表';

-- -----------------------------------------------------
-- 4. 质量检查标准表 (quality_check_standard) - M10-030
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS quality_check_standard (
    id BIGSERIAL PRIMARY KEY,
    standard_no VARCHAR(50) NOT NULL UNIQUE,            -- 标准编号
    standard_name VARCHAR(200) NOT NULL,                 -- 标准名称
    category VARCHAR(50),                               -- 分类：CONTRACT合同, DOCUMENT文书, PROCEDURE程序, OTHER其他
    description TEXT,                                   -- 标准描述
    check_items TEXT,                                   -- 检查项（JSON格式）
    applicable_matter_types VARCHAR(500),               -- 适用案件类型
    weight DECIMAL(5,2) DEFAULT 1.0,                    -- 权重
    enabled BOOLEAN DEFAULT TRUE,                       -- 是否启用
    sort_order INT DEFAULT 0,                           -- 排序
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_quality_standard_category ON quality_check_standard(category);
CREATE INDEX idx_quality_standard_enabled ON quality_check_standard(enabled);

COMMENT ON TABLE quality_check_standard IS '质量检查标准表';
COMMENT ON COLUMN quality_check_standard.category IS '分类：CONTRACT合同, DOCUMENT文书, PROCEDURE程序, OTHER其他';

-- -----------------------------------------------------
-- 5. 质量检查表 (quality_check) - M10-031
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS quality_check (
    id BIGSERIAL PRIMARY KEY,
    check_no VARCHAR(50) NOT NULL UNIQUE,               -- 检查编号
    matter_id BIGINT NOT NULL,                          -- 项目ID
    checker_id BIGINT NOT NULL,                         -- 检查人ID
    check_date DATE NOT NULL,                           -- 检查日期
    check_type VARCHAR(20) NOT NULL,                    -- 检查类型：ROUTINE常规, RANDOM随机, SPECIAL专项
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',          -- 状态：IN_PROGRESS进行中, COMPLETED已完成
    total_score DECIMAL(5,2),                           -- 总分
    qualified BOOLEAN,                                  -- 是否合格
    check_summary TEXT,                                 -- 检查总结
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_quality_check_matter ON quality_check(matter_id);
CREATE INDEX idx_quality_check_checker ON quality_check(checker_id);
CREATE INDEX idx_quality_check_date ON quality_check(check_date);
CREATE INDEX idx_quality_check_status ON quality_check(status);

COMMENT ON TABLE quality_check IS '质量检查表';
COMMENT ON COLUMN quality_check.check_type IS '检查类型：ROUTINE常规, RANDOM随机, SPECIAL专项';
COMMENT ON COLUMN quality_check.status IS '状态：IN_PROGRESS进行中, COMPLETED已完成';

-- -----------------------------------------------------
-- 6. 质量检查明细表 (quality_check_detail) - M10-031
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS quality_check_detail (
    id BIGSERIAL PRIMARY KEY,
    check_id BIGINT NOT NULL,                           -- 检查ID
    standard_id BIGINT NOT NULL,                        -- 标准ID
    check_result VARCHAR(20),                           -- 检查结果：PASS通过, FAIL不通过, PARTIAL部分通过
    score DECIMAL(5,2),                                 -- 得分
    max_score DECIMAL(5,2),                            -- 满分
    findings TEXT,                                      -- 发现的问题
    suggestions TEXT,                                  -- 改进建议
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quality_check_detail_check ON quality_check_detail(check_id);
CREATE INDEX idx_quality_check_detail_standard ON quality_check_detail(standard_id);

COMMENT ON TABLE quality_check_detail IS '质量检查明细表';
COMMENT ON COLUMN quality_check_detail.check_result IS '检查结果：PASS通过, FAIL不通过, PARTIAL部分通过';

-- -----------------------------------------------------
-- 7. 问题整改表 (quality_issue) - M10-032
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS quality_issue (
    id BIGSERIAL PRIMARY KEY,
    issue_no VARCHAR(50) NOT NULL UNIQUE,               -- 问题编号
    check_id BIGINT,                                    -- 关联检查ID
    matter_id BIGINT NOT NULL,                          -- 项目ID
    issue_type VARCHAR(50),                             -- 问题类型：CRITICAL严重, MAJOR重要, MINOR一般
    issue_description TEXT NOT NULL,                   -- 问题描述
    responsible_user_id BIGINT,                        -- 责任人ID
    status VARCHAR(20) DEFAULT 'OPEN',                 -- 状态：OPEN待整改, IN_PROGRESS整改中, RESOLVED已解决, CLOSED已关闭
    priority VARCHAR(20) DEFAULT 'MEDIUM',             -- 优先级：HIGH高, MEDIUM中, LOW低
    due_date DATE,                                     -- 整改期限
    resolution TEXT,                                    -- 整改措施
    resolved_at TIMESTAMP,                             -- 解决时间
    resolved_by BIGINT,                                -- 解决人ID
    verified_at TIMESTAMP,                             -- 验证时间
    verified_by BIGINT,                                -- 验证人ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_quality_issue_matter ON quality_issue(matter_id);
CREATE INDEX idx_quality_issue_check ON quality_issue(check_id);
CREATE INDEX idx_quality_issue_status ON quality_issue(status);
CREATE INDEX idx_quality_issue_responsible ON quality_issue(responsible_user_id);

COMMENT ON TABLE quality_issue IS '问题整改表';
COMMENT ON COLUMN quality_issue.issue_type IS '问题类型：CRITICAL严重, MAJOR重要, MINOR一般';
COMMENT ON COLUMN quality_issue.status IS '状态：OPEN待整改, IN_PROGRESS整改中, RESOLVED已解决, CLOSED已关闭';

-- -----------------------------------------------------
-- 8. 风险预警表 (risk_warning) - M10-033
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS risk_warning (
    id BIGSERIAL PRIMARY KEY,
    warning_no VARCHAR(50) NOT NULL UNIQUE,             -- 预警编号
    matter_id BIGINT NOT NULL,                          -- 项目ID
    risk_type VARCHAR(50) NOT NULL,                    -- 风险类型：DEADLINE期限风险, QUALITY质量风险, COST成本风险, LEGAL法律风险, OTHER其他
    risk_level VARCHAR(20) NOT NULL,                   -- 风险等级：HIGH高, MEDIUM中, LOW低
    risk_description TEXT NOT NULL,                     -- 风险描述
    warning_reason TEXT,                                -- 预警原因
    suggested_action TEXT,                              -- 建议措施
    status VARCHAR(20) DEFAULT 'ACTIVE',               -- 状态：ACTIVE活跃, ACKNOWLEDGED已确认, RESOLVED已解决, CLOSED已关闭
    acknowledged_at TIMESTAMP,                         -- 确认时间
    acknowledged_by BIGINT,                            -- 确认人ID
    resolved_at TIMESTAMP,                             -- 解决时间
    resolved_by BIGINT,                                -- 解决人ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_risk_warning_matter ON risk_warning(matter_id);
CREATE INDEX idx_risk_warning_type ON risk_warning(risk_type);
CREATE INDEX idx_risk_warning_level ON risk_warning(risk_level);
CREATE INDEX idx_risk_warning_status ON risk_warning(status);

COMMENT ON TABLE risk_warning IS '风险预警表';
COMMENT ON COLUMN risk_warning.risk_type IS '风险类型：DEADLINE期限风险, QUALITY质量风险, COST成本风险, LEGAL法律风险, OTHER其他';
COMMENT ON COLUMN risk_warning.risk_level IS '风险等级：HIGH高, MEDIUM中, LOW低';
COMMENT ON COLUMN risk_warning.status IS '状态：ACTIVE活跃, ACKNOWLEDGED已确认, RESOLVED已解决, CLOSED已关闭';

