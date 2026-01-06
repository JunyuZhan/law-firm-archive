-- ============================================
-- 智慧律所管理系统 - 文书印章模块数据库脚本
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- ============================================
-- 五、文书管理模块
-- ============================================

-- 文档分类表
CREATE TABLE IF NOT EXISTS doc_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE doc_category IS '文档分类表';
COMMENT ON COLUMN doc_category.name IS '分类名称';
COMMENT ON COLUMN doc_category.parent_id IS '父分类ID，0表示顶级';

-- 文档表
CREATE TABLE IF NOT EXISTS doc_document (
    id BIGSERIAL PRIMARY KEY,
    doc_no VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    
    -- 关联信息
    category_id BIGINT,
    matter_id BIGINT,
    
    -- 文件信息
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),
    mime_type VARCHAR(100),
    
    -- 版本信息
    version INT DEFAULT 1,
    is_latest BOOLEAN DEFAULT TRUE,
    parent_doc_id BIGINT,
    
    -- 安全级别
    security_level VARCHAR(20) DEFAULT 'INTERNAL',
    
    -- 文档阶段（关联案件阶段）
    stage VARCHAR(50),
    
    -- 标签（JSON数组）
    tags JSONB,
    
    -- 描述
    description TEXT,
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE doc_document IS '文档表';
COMMENT ON COLUMN doc_document.doc_no IS '文档编号';
COMMENT ON COLUMN doc_document.security_level IS '安全级别: PUBLIC-公开, INTERNAL-内部, CONFIDENTIAL-机密, TOP_SECRET-绝密';
COMMENT ON COLUMN doc_document.status IS '状态: ACTIVE-正常, ARCHIVED-已归档, DELETED-已删除';

-- 文档版本历史表
CREATE TABLE IF NOT EXISTS doc_version (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version INT NOT NULL,
    
    -- 文件信息
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT,
    
    -- 变更说明
    change_note VARCHAR(1000),
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(document_id, version)
);

COMMENT ON TABLE doc_version IS '文档版本历史表';

-- 文档访问日志表
CREATE TABLE IF NOT EXISTS doc_access_log (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    -- 操作类型
    action_type VARCHAR(20) NOT NULL,
    
    -- 访问信息
    ip_address VARCHAR(50),
    user_agent VARCHAR(2000),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE doc_access_log IS '文档访问日志表';
COMMENT ON COLUMN doc_access_log.action_type IS '操作类型: VIEW-查看, DOWNLOAD-下载, PRINT-打印, EDIT-编辑';

-- 文档模板表
CREATE TABLE IF NOT EXISTS doc_template (
    id BIGSERIAL PRIMARY KEY,
    template_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    
    -- 分类
    category_id BIGINT,
    template_type VARCHAR(50),
    
    -- 文件信息
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT,
    
    -- 变量定义（JSON）
    variables JSONB,
    
    -- 描述
    description TEXT,
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    -- 使用次数
    use_count INT DEFAULT 0,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE doc_template IS '文档模板表';
COMMENT ON COLUMN doc_template.template_type IS '模板类型: CONTRACT-合同, LEGAL_OPINION-法律意见书, POWER_OF_ATTORNEY-授权委托书, COMPLAINT-起诉状, DEFENSE-答辩状, OTHER-其他';
COMMENT ON COLUMN doc_template.variables IS '模板变量定义，如 [{"name":"clientName","label":"客户名称","type":"text"}]';

-- ============================================
-- 六、印章管理模块
-- ============================================

-- 印章表
CREATE TABLE IF NOT EXISTS seal_info (
    id BIGSERIAL PRIMARY KEY,
    seal_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    
    -- 印章类型
    seal_type VARCHAR(50) NOT NULL,
    
    -- 保管信息
    keeper_id BIGINT NOT NULL,
    keeper_name VARCHAR(50),
    
    -- 印章图片
    image_url VARCHAR(500),
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    -- 描述
    description TEXT,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE seal_info IS '印章表';
COMMENT ON COLUMN seal_info.seal_type IS '印章类型: OFFICIAL-公章, CONTRACT-合同章, FINANCE-财务章, LEGAL-法人章, OTHER-其他';
COMMENT ON COLUMN seal_info.status IS '状态: ACTIVE-在用, DISABLED-停用, LOST-遗失, DESTROYED-销毁';

-- 用印申请表
CREATE TABLE IF NOT EXISTS seal_application (
    id BIGSERIAL PRIMARY KEY,
    application_no VARCHAR(50) NOT NULL UNIQUE,
    
    -- 申请人
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(50),
    department_id BIGINT,
    
    -- 印章信息
    seal_id BIGINT NOT NULL,
    seal_name VARCHAR(100),
    
    -- 关联项目
    matter_id BIGINT,
    matter_name VARCHAR(500),
    
    -- 用印信息
    document_name VARCHAR(500) NOT NULL,
    document_type VARCHAR(50),
    copies INT DEFAULT 1,
    use_purpose TEXT,
    
    -- 用印日期
    expected_use_date DATE,
    actual_use_date DATE,
    
    -- 审批状态
    status VARCHAR(20) DEFAULT 'PENDING',
    
    -- 审批信息
    approved_by BIGINT,
    approved_at TIMESTAMP,
    approval_comment TEXT,
    
    -- 用印登记
    used_by BIGINT,
    used_at TIMESTAMP,
    use_remark TEXT,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE seal_application IS '用印申请表';
COMMENT ON COLUMN seal_application.status IS '状态: PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, USED-已用印, CANCELLED-已取消';

-- 用印记录表（历史记录）
CREATE TABLE IF NOT EXISTS seal_usage_record (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    seal_id BIGINT NOT NULL,
    
    -- 用印信息
    document_name VARCHAR(500) NOT NULL,
    copies INT DEFAULT 1,
    
    -- 用印人
    used_by BIGINT NOT NULL,
    used_at TIMESTAMP NOT NULL,
    
    -- 备注
    remark TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE seal_usage_record IS '用印记录表';

-- ============================================
-- 索引
-- ============================================

CREATE INDEX IF NOT EXISTS idx_doc_category_parent ON doc_category(parent_id);
CREATE INDEX IF NOT EXISTS idx_document_category ON doc_document(category_id);
CREATE INDEX IF NOT EXISTS idx_document_matter ON doc_document(matter_id);
CREATE INDEX IF NOT EXISTS idx_document_status ON doc_document(status);
CREATE INDEX IF NOT EXISTS idx_doc_version_document ON doc_version(document_id);
CREATE INDEX IF NOT EXISTS idx_doc_access_document ON doc_access_log(document_id);
CREATE INDEX IF NOT EXISTS idx_doc_access_user ON doc_access_log(user_id);
CREATE INDEX IF NOT EXISTS idx_template_category ON doc_template(category_id);
CREATE INDEX IF NOT EXISTS idx_seal_keeper ON seal_info(keeper_id);
CREATE INDEX IF NOT EXISTS idx_seal_app_applicant ON seal_application(applicant_id);
CREATE INDEX IF NOT EXISTS idx_seal_app_seal ON seal_application(seal_id);
CREATE INDEX IF NOT EXISTS idx_seal_app_status ON seal_application(status);
CREATE INDEX IF NOT EXISTS idx_seal_usage_seal ON seal_usage_record(seal_id);

-- ============================================
-- 初始数据
-- ============================================

-- 文档分类初始数据
INSERT INTO doc_category (name, parent_id, sort_order, description) VALUES
('案件材料', 0, 1, '案件相关的所有材料'),
('起诉材料', 1, 1, '起诉状、证据等'),
('答辩材料', 1, 2, '答辩状、反诉状等'),
('庭审材料', 1, 3, '庭审笔录、代理词等'),
('判决文书', 1, 4, '判决书、裁定书等'),
('合同文件', 0, 2, '各类合同文件'),
('委托合同', 6, 1, '委托代理合同'),
('服务合同', 6, 2, '法律服务合同'),
('行政文件', 0, 3, '行政管理文件'),
('内部制度', 9, 1, '内部管理制度'),
('会议纪要', 9, 2, '会议纪要'),
('模板文件', 0, 4, '各类模板文件')
ON CONFLICT DO NOTHING;
