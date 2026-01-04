-- ============================================
-- 智慧律所管理系统 - 证据管理模块数据库脚本
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 证据表
CREATE TABLE IF NOT EXISTS evidence (
    id BIGSERIAL PRIMARY KEY,
    evidence_no VARCHAR(50) NOT NULL UNIQUE,
    matter_id BIGINT NOT NULL,
    
    -- 证据基本信息
    name VARCHAR(500) NOT NULL,
    evidence_type VARCHAR(50) NOT NULL,
    source VARCHAR(200),
    
    -- 证据分组和排序
    group_name VARCHAR(100),
    sort_order INT DEFAULT 0,
    
    -- 证据内容
    prove_purpose TEXT,
    description TEXT,
    
    -- 原件信息
    is_original BOOLEAN DEFAULT FALSE,
    original_count INT DEFAULT 0,
    copy_count INT DEFAULT 0,
    page_start INT,
    page_end INT,
    
    -- 文件信息
    file_url VARCHAR(1000),
    file_name VARCHAR(500),
    file_size BIGINT,
    
    -- 质证状态
    cross_exam_status VARCHAR(20) DEFAULT 'PENDING',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE evidence IS '证据表';
COMMENT ON COLUMN evidence.evidence_type IS '证据类型: DOCUMENTARY-书证, PHYSICAL-物证, AUDIO_VISUAL-视听资料, ELECTRONIC-电子数据, WITNESS-证人证言, EXPERT-鉴定意见, INSPECTION-勘验笔录';
COMMENT ON COLUMN evidence.cross_exam_status IS '质证状态: PENDING-待质证, IN_PROGRESS-质证中, COMPLETED-已质证';

-- 质证记录表
CREATE TABLE IF NOT EXISTS evidence_cross_exam (
    id BIGSERIAL PRIMARY KEY,
    evidence_id BIGINT NOT NULL,
    
    -- 质证方
    exam_party VARCHAR(50) NOT NULL,
    
    -- 三性意见
    authenticity_opinion VARCHAR(20),
    authenticity_reason TEXT,
    
    legality_opinion VARCHAR(20),
    legality_reason TEXT,
    
    relevance_opinion VARCHAR(20),
    relevance_reason TEXT,
    
    -- 综合意见
    overall_opinion TEXT,
    
    -- 法院认定（如有）
    court_opinion TEXT,
    court_accepted BOOLEAN,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE evidence_cross_exam IS '质证记录表';
COMMENT ON COLUMN evidence_cross_exam.exam_party IS '质证方: OUR_SIDE-我方, OPPOSITE-对方, COURT-法院';
COMMENT ON COLUMN evidence_cross_exam.authenticity_opinion IS '真实性意见: ACCEPT-认可, PARTIAL-部分认可, REJECT-不认可';

-- 证据清单表
CREATE TABLE IF NOT EXISTS evidence_list (
    id BIGSERIAL PRIMARY KEY,
    list_no VARCHAR(50) NOT NULL UNIQUE,
    matter_id BIGINT NOT NULL,
    
    -- 清单信息
    name VARCHAR(200) NOT NULL,
    list_type VARCHAR(50),
    
    -- 包含的证据ID列表
    evidence_ids JSONB,
    
    -- 生成的文件
    file_url VARCHAR(1000),
    file_name VARCHAR(500),
    
    -- 状态
    status VARCHAR(20) DEFAULT 'DRAFT',
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE evidence_list IS '证据清单表';
COMMENT ON COLUMN evidence_list.list_type IS '清单类型: SUBMISSION-提交清单, EXCHANGE-交换清单, COURT-庭审清单';

-- 索引
CREATE INDEX IF NOT EXISTS idx_evidence_matter ON evidence(matter_id);
CREATE INDEX IF NOT EXISTS idx_evidence_type ON evidence(evidence_type);
CREATE INDEX IF NOT EXISTS idx_evidence_status ON evidence(cross_exam_status);
CREATE INDEX IF NOT EXISTS idx_cross_exam_evidence ON evidence_cross_exam(evidence_id);
CREATE INDEX IF NOT EXISTS idx_evidence_list_matter ON evidence_list(matter_id);
