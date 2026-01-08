-- =====================================================
-- 卷宗管理模块重构迁移脚本
-- 日期: 2026-01-06
-- 说明: 
--   1. 菜单名称调整：文书管理 → 卷宗管理
--   2. doc_document 表新增 file_category, folder_path 字段
--   3. evidence 表新增 document_id 字段
--   4. 新增卷宗目录模板表
-- =====================================================

-- ===========================================
-- 第一部分：菜单名称调整
-- ===========================================

-- 更新主菜单名称：文书管理 → 卷宗管理
UPDATE sys_menu SET name = '卷宗管理' WHERE id = 6 AND name = '文书管理';

-- 更新子菜单名称：文书列表 → 卷宗列表
UPDATE sys_menu SET name = '卷宗列表' WHERE id = 61 AND name = '文书列表';

-- 更新子菜单名称：文书模板 → 文档模板（保持不变，模板是通用的）
-- UPDATE sys_menu SET name = '文档模板' WHERE id = 62 AND name = '文书模板';

-- ===========================================
-- 第二部分：doc_document 表新增字段
-- ===========================================

-- 新增文件分类字段
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS file_category VARCHAR(50) DEFAULT 'OTHER';

-- 新增文件夹路径字段（用于显示卷宗目录结构）
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS folder_path VARCHAR(500);

-- 新增卷宗目录项ID（关联预制目录）
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS dossier_item_id BIGINT;

-- 添加字段注释
COMMENT ON COLUMN doc_document.file_category IS '文件分类: EVIDENCE-证据材料, PLEADING-诉讼文书, CONTRACT-合同文件, CORRESPONDENCE-往来函件, MEETING-会见记录, JUDGMENT-裁判文书, WORKLOG-工作日志, OTHER-其他';
COMMENT ON COLUMN doc_document.folder_path IS '卷宗目录路径，如: /证据材料/';
COMMENT ON COLUMN doc_document.dossier_item_id IS '关联卷宗目录项ID';

-- ===========================================
-- 第三部分：evidence 表新增 document_id 字段
-- ===========================================

-- 新增关联卷宗文件ID
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS document_id BIGINT;

-- 添加字段注释
COMMENT ON COLUMN evidence.document_id IS '关联卷宗文件ID，引用 doc_document.id';

-- ===========================================
-- 第四部分：创建卷宗目录模板表
-- ===========================================

-- 卷宗目录模板表（定义不同案件类型的标准目录结构）
CREATE TABLE IF NOT EXISTS dossier_template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                    -- 模板名称
    case_type VARCHAR(50) NOT NULL,                -- 案件类型: CRIMINAL-刑事, CIVIL-民事, LEGAL_COUNSEL-法律顾问, NON_LITIGATION-其他非诉
    description VARCHAR(500),                       -- 描述
    is_default BOOLEAN DEFAULT false,              -- 是否默认模板
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

COMMENT ON TABLE dossier_template IS '卷宗目录模板表';
COMMENT ON COLUMN dossier_template.case_type IS '案件类型: CRIMINAL-刑事, CIVIL-民事, LEGAL_COUNSEL-法律顾问, NON_LITIGATION-其他非诉';

-- 卷宗目录项表（模板中的具体目录项）
CREATE TABLE IF NOT EXISTS dossier_template_item (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,                   -- 所属模板ID
    parent_id BIGINT DEFAULT 0,                    -- 父目录ID，0表示顶级
    name VARCHAR(200) NOT NULL,                    -- 目录名称
    item_type VARCHAR(50) DEFAULT 'FOLDER',        -- 类型: FOLDER-目录, FILE-文件占位
    file_category VARCHAR(50),                     -- 对应的文件分类
    sort_order INTEGER DEFAULT 0,                  -- 排序号
    required BOOLEAN DEFAULT false,                -- 是否必需
    description VARCHAR(500),                       -- 说明
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dossier_item_template FOREIGN KEY (template_id) REFERENCES dossier_template(id)
);

COMMENT ON TABLE dossier_template_item IS '卷宗目录项表';
COMMENT ON COLUMN dossier_template_item.item_type IS '类型: FOLDER-目录, FILE-文件占位';

-- 项目卷宗目录表（实际项目的目录结构，基于模板生成，可自定义修改）
CREATE TABLE IF NOT EXISTS matter_dossier_item (
    id BIGSERIAL PRIMARY KEY,
    matter_id BIGINT NOT NULL,                     -- 项目ID
    parent_id BIGINT DEFAULT 0,                    -- 父目录ID
    name VARCHAR(200) NOT NULL,                    -- 目录名称
    item_type VARCHAR(50) DEFAULT 'FOLDER',        -- 类型
    file_category VARCHAR(50),                     -- 文件分类
    sort_order INTEGER DEFAULT 0,                  -- 排序号
    document_count INTEGER DEFAULT 0,              -- 文件数量（统计）
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_matter_dossier_matter FOREIGN KEY (matter_id) REFERENCES matter(id)
);

COMMENT ON TABLE matter_dossier_item IS '项目卷宗目录表';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_dossier_template_case_type ON dossier_template(case_type);
CREATE INDEX IF NOT EXISTS idx_dossier_template_item_template ON dossier_template_item(template_id);
CREATE INDEX IF NOT EXISTS idx_matter_dossier_item_matter ON matter_dossier_item(matter_id);
CREATE INDEX IF NOT EXISTS idx_document_dossier_item ON doc_document(dossier_item_id);
CREATE INDEX IF NOT EXISTS idx_evidence_document ON evidence(document_id);

-- ===========================================
-- 第五部分：初始化卷宗目录模板数据
-- ===========================================

-- 刑事案件模板
INSERT INTO dossier_template (id, name, case_type, description, is_default) VALUES
(1, '刑事案件卷宗模板', 'CRIMINAL', '适用于刑事辩护案件', true)
ON CONFLICT DO NOTHING;

-- 刑事案件目录项
INSERT INTO dossier_template_item (template_id, parent_id, name, item_type, file_category, sort_order, required, description) VALUES
(1, 0, '1.收案审批表', 'FILE', 'OTHER', 1, true, '收案审批表'),
(1, 0, '2.授权委托书', 'FILE', 'CONTRACT', 2, true, '授权委托书'),
(1, 0, '3.委托合同', 'FILE', 'CONTRACT', 3, true, '委托代理合同'),
(1, 0, '4.收费发票', 'FILE', 'OTHER', 4, false, '收费发票复印件'),
(1, 0, '5.办案机关卷宗材料', 'FOLDER', 'EVIDENCE', 5, false, '阅卷笔录和卷宗材料'),
(1, 0, '6.会见笔录', 'FOLDER', 'MEETING', 6, false, '会见当事人的笔录'),
(1, 0, '7.起诉书/上诉状/抗诉书/再审申请书', 'FOLDER', 'PLEADING', 7, false, '诉讼文书'),
(1, 0, '8.重大案件集体讨论记录', 'FILE', 'OTHER', 8, false, '重大案件讨论记录'),
(1, 0, '9.辩护词', 'FILE', 'PLEADING', 9, false, '辩护词'),
(1, 0, '10.裁定书/判决书', 'FOLDER', 'JUDGMENT', 10, false, '法院裁判文书'),
(1, 0, '11.工作日志', 'FILE', 'WORKLOG', 11, false, '工作日志'),
(1, 0, '12.结案（归档）审批表', 'FILE', 'OTHER', 12, true, '结案审批表'),
(1, 0, '13.其他材料', 'FOLDER', 'OTHER', 13, false, '其他相关材料')
ON CONFLICT DO NOTHING;

-- 民事案件模板
INSERT INTO dossier_template (id, name, case_type, description, is_default) VALUES
(2, '民事案件卷宗模板', 'CIVIL', '适用于民事诉讼代理案件', true)
ON CONFLICT DO NOTHING;

-- 民事案件目录项
INSERT INTO dossier_template_item (template_id, parent_id, name, item_type, file_category, sort_order, required, description) VALUES
(2, 0, '1.收案审批表', 'FILE', 'OTHER', 1, true, '收案审批表'),
(2, 0, '2.授权委托书', 'FILE', 'CONTRACT', 2, true, '授权委托书'),
(2, 0, '3.委托合同', 'FILE', 'CONTRACT', 3, true, '委托代理合同'),
(2, 0, '4.收费发票', 'FILE', 'OTHER', 4, false, '收费发票复印件'),
(2, 0, '5.起诉状/上诉状/答辩状/再审申请书', 'FOLDER', 'PLEADING', 5, false, '诉讼文书'),
(2, 0, '6.证据材料', 'FOLDER', 'EVIDENCE', 6, false, '证据材料和阅卷笔录'),
(2, 0, '7.重大案件集体讨论记录', 'FILE', 'OTHER', 7, false, '重大案件讨论记录'),
(2, 0, '8.代理词', 'FILE', 'PLEADING', 8, false, '代理词'),
(2, 0, '9.判决书/裁定书/调解书', 'FOLDER', 'JUDGMENT', 9, false, '法院裁判文书'),
(2, 0, '10.工作日志', 'FILE', 'WORKLOG', 10, false, '工作日志'),
(2, 0, '11.结案（归档）审批表', 'FILE', 'OTHER', 11, true, '结案审批表'),
(2, 0, '12.其他材料', 'FOLDER', 'OTHER', 12, false, '其他相关材料')
ON CONFLICT DO NOTHING;

-- 法律顾问模板
INSERT INTO dossier_template (id, name, case_type, description, is_default) VALUES
(3, '法律顾问卷宗模板', 'LEGAL_COUNSEL', '适用于常年法律顾问服务', true)
ON CONFLICT DO NOTHING;

-- 法律顾问目录项
INSERT INTO dossier_template_item (template_id, parent_id, name, item_type, file_category, sort_order, required, description) VALUES
(3, 0, '1.顾问合同', 'FILE', 'CONTRACT', 1, true, '法律顾问服务合同'),
(3, 0, '2.收费发票', 'FILE', 'OTHER', 2, false, '收费发票复印件'),
(3, 0, '3.法律事务记录', 'FOLDER', 'OTHER', 3, false, '办理各类法律事务的记录及相关材料'),
(3, 0, '4.合同审查', 'FOLDER', 'OTHER', 4, false, '起草、审查的合同'),
(3, 0, '5.规章制度', 'FOLDER', 'OTHER', 5, false, '起草、审查的规章制度'),
(3, 0, '6.法律意见书', 'FOLDER', 'PLEADING', 6, false, '出具的法律意见书'),
(3, 0, '7.律师函', 'FOLDER', 'CORRESPONDENCE', 7, false, '出具的律师函'),
(3, 0, '8.咨询记录', 'FOLDER', 'OTHER', 8, false, '法律咨询记录'),
(3, 0, '9.工作日志', 'FILE', 'WORKLOG', 9, false, '工作日志'),
(3, 0, '10.工作小结', 'FILE', 'OTHER', 10, false, '年度工作小结'),
(3, 0, '11.其他材料', 'FOLDER', 'OTHER', 11, false, '其他相关材料')
ON CONFLICT DO NOTHING;

-- 其他非诉讼模板
INSERT INTO dossier_template (id, name, case_type, description, is_default) VALUES
(4, '其他非诉讼法律事务卷宗模板', 'NON_LITIGATION', '适用于其他非诉讼法律事务', true)
ON CONFLICT DO NOTHING;

-- 其他非诉讼目录项
INSERT INTO dossier_template_item (template_id, parent_id, name, item_type, file_category, sort_order, required, description) VALUES
(4, 0, '1.授权委托书', 'FILE', 'CONTRACT', 1, true, '授权委托书'),
(4, 0, '2.收费发票', 'FILE', 'OTHER', 2, false, '收费发票复印件'),
(4, 0, '3.证据材料', 'FOLDER', 'EVIDENCE', 3, false, '相关证据材料'),
(4, 0, '4.法律意见书/律师函/其他法律文书', 'FOLDER', 'PLEADING', 4, false, '出具的法律文书'),
(4, 0, '5.工作小结', 'FILE', 'OTHER', 5, false, '工作小结'),
(4, 0, '6.其他材料', 'FOLDER', 'OTHER', 6, false, '其他相关材料')
ON CONFLICT DO NOTHING;

-- 重置序列
SELECT setval('dossier_template_id_seq', COALESCE((SELECT MAX(id) FROM dossier_template), 0) + 1, false);
SELECT setval('dossier_template_item_id_seq', COALESCE((SELECT MAX(id) FROM dossier_template_item), 0) + 1, false);

