-- =====================================================
-- 知识库模块数据库表结构
-- 模块：M10 知识库
-- 包含：法规库、案例库、经验分享
-- =====================================================

-- -----------------------------------------------------
-- 1. 法规分类表 (law_category)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS law_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                       -- 分类名称
    parent_id BIGINT DEFAULT 0,                       -- 父分类ID
    level INT DEFAULT 1,                              -- 层级
    sort_order INT DEFAULT 0,                         -- 排序
    description VARCHAR(500),                         -- 描述
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_law_category_parent ON law_category(parent_id);

COMMENT ON TABLE law_category IS '法规分类表';

-- 初始化法规分类
INSERT INTO law_category (name, parent_id, level, sort_order) VALUES
('宪法', 0, 1, 1),
('法律', 0, 1, 2),
('行政法规', 0, 1, 3),
('地方性法规', 0, 1, 4),
('司法解释', 0, 1, 5),
('部门规章', 0, 1, 6);

-- -----------------------------------------------------
-- 2. 法规表 (law_regulation)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS law_regulation (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,                      -- 法规标题
    category_id BIGINT NOT NULL,                      -- 分类ID
    doc_number VARCHAR(100),                          -- 文号
    issuing_authority VARCHAR(200),                   -- 发布机关
    issue_date DATE,                                  -- 发布日期
    effective_date DATE,                              -- 生效日期
    expiry_date DATE,                                 -- 失效日期
    status VARCHAR(20) DEFAULT 'EFFECTIVE',           -- 状态: EFFECTIVE有效/AMENDED已修订/REPEALED已废止
    content TEXT,                                     -- 法规内容
    summary VARCHAR(2000),                            -- 摘要
    keywords VARCHAR(500),                            -- 关键词
    source VARCHAR(200),                              -- 来源
    attachment_url VARCHAR(500),                      -- 附件URL
    view_count INT DEFAULT 0,                         -- 浏览次数
    collect_count INT DEFAULT 0,                      -- 收藏次数
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_law_regulation_category ON law_regulation(category_id);
CREATE INDEX idx_law_regulation_status ON law_regulation(status);
CREATE INDEX idx_law_regulation_issue_date ON law_regulation(issue_date);

COMMENT ON TABLE law_regulation IS '法规表';
COMMENT ON COLUMN law_regulation.status IS '状态: EFFECTIVE有效/AMENDED已修订/REPEALED已废止';

-- -----------------------------------------------------
-- 3. 案例分类表 (case_category)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS case_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                       -- 分类名称
    parent_id BIGINT DEFAULT 0,                       -- 父分类ID
    level INT DEFAULT 1,                              -- 层级
    sort_order INT DEFAULT 0,                         -- 排序
    description VARCHAR(500),                         -- 描述
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_case_category_parent ON case_category(parent_id);

COMMENT ON TABLE case_category IS '案例分类表';

-- 初始化案例分类
INSERT INTO case_category (name, parent_id, level, sort_order) VALUES
('民事案例', 0, 1, 1),
('刑事案例', 0, 1, 2),
('行政案例', 0, 1, 3),
('知识产权案例', 0, 1, 4),
('劳动争议案例', 0, 1, 5),
('公司商事案例', 0, 1, 6);

-- -----------------------------------------------------
-- 4. 案例库表 (case_library)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS case_library (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,                      -- 案例标题
    category_id BIGINT NOT NULL,                      -- 分类ID
    case_number VARCHAR(100),                         -- 案号
    court_name VARCHAR(200),                          -- 审理法院
    judge_date DATE,                                  -- 裁判日期
    case_type VARCHAR(50),                            -- 案件类型
    cause_of_action VARCHAR(200),                     -- 案由
    trial_procedure VARCHAR(50),                      -- 审理程序(一审/二审/再审)
    plaintiff VARCHAR(500),                           -- 原告/上诉人
    defendant VARCHAR(500),                           -- 被告/被上诉人
    case_summary TEXT,                                -- 案情摘要
    court_opinion TEXT,                               -- 法院观点
    judgment_result TEXT,                             -- 裁判结果
    case_significance VARCHAR(2000),                  -- 案例意义
    keywords VARCHAR(500),                            -- 关键词
    source VARCHAR(50) DEFAULT 'EXTERNAL',            -- 来源: EXTERNAL外部/INTERNAL内部
    matter_id BIGINT,                                 -- 关联案件ID(内部案例)
    attachment_url VARCHAR(500),                      -- 附件URL
    view_count INT DEFAULT 0,                         -- 浏览次数
    collect_count INT DEFAULT 0,                      -- 收藏次数
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_case_library_category ON case_library(category_id);
CREATE INDEX idx_case_library_court ON case_library(court_name);
CREATE INDEX idx_case_library_judge_date ON case_library(judge_date);
CREATE INDEX idx_case_library_source ON case_library(source);

COMMENT ON TABLE case_library IS '案例库表';
COMMENT ON COLUMN case_library.source IS '来源: EXTERNAL外部/INTERNAL内部';

-- -----------------------------------------------------
-- 5. 知识收藏表 (knowledge_collection)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge_collection (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,                          -- 用户ID
    target_type VARCHAR(20) NOT NULL,                 -- 目标类型: LAW法规/CASE案例
    target_id BIGINT NOT NULL,                        -- 目标ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_knowledge_collection UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_knowledge_collection_user ON knowledge_collection(user_id);
CREATE INDEX idx_knowledge_collection_target ON knowledge_collection(target_type, target_id);

COMMENT ON TABLE knowledge_collection IS '知识收藏表';
COMMENT ON COLUMN knowledge_collection.target_type IS '目标类型: LAW法规/CASE案例';

-- -----------------------------------------------------
-- 6. 经验文章表 (knowledge_article)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge_article (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,                      -- 文章标题
    category VARCHAR(50),                             -- 分类
    content TEXT NOT NULL,                            -- 文章内容
    summary VARCHAR(500),                             -- 摘要
    author_id BIGINT NOT NULL,                        -- 作者ID
    status VARCHAR(20) DEFAULT 'DRAFT',               -- 状态: DRAFT草稿/PUBLISHED已发布/ARCHIVED已归档
    tags VARCHAR(500),                                -- 标签
    view_count INT DEFAULT 0,                         -- 浏览次数
    like_count INT DEFAULT 0,                         -- 点赞次数
    comment_count INT DEFAULT 0,                      -- 评论次数
    published_at TIMESTAMP,                           -- 发布时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_knowledge_article_author ON knowledge_article(author_id);
CREATE INDEX idx_knowledge_article_status ON knowledge_article(status);
CREATE INDEX idx_knowledge_article_category ON knowledge_article(category);

COMMENT ON TABLE knowledge_article IS '经验文章表';
COMMENT ON COLUMN knowledge_article.status IS '状态: DRAFT草稿/PUBLISHED已发布/ARCHIVED已归档';
