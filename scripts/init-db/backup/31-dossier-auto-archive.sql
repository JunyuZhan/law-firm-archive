-- =====================================================
-- 律师事务所管理系统 - 卷宗自动归档功能
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-11
-- 描述: 为文档表添加来源追踪字段，支持卷宗材料自动归档
-- =====================================================

-- 添加文档来源类型字段
ALTER TABLE public.doc_document ADD COLUMN IF NOT EXISTS source_type VARCHAR(20);
COMMENT ON COLUMN public.doc_document.source_type IS '文档来源类型: SYSTEM_GENERATED-系统自动生成, SYSTEM_LINKED-系统自动关联, USER_UPLOADED-用户上传, SIGNED_VERSION-签字版本';

-- 添加来源数据ID字段
ALTER TABLE public.doc_document ADD COLUMN IF NOT EXISTS source_id BIGINT;
COMMENT ON COLUMN public.doc_document.source_id IS '来源数据ID（如合同ID、审批ID等）';

-- 添加来源模块字段
ALTER TABLE public.doc_document ADD COLUMN IF NOT EXISTS source_module VARCHAR(50);
COMMENT ON COLUMN public.doc_document.source_module IS '来源模块: CONTRACT-合同, APPROVAL-审批, INVOICE-发票, MATTER-项目';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_doc_document_source_type ON public.doc_document (source_type);
CREATE INDEX IF NOT EXISTS idx_doc_document_source ON public.doc_document (source_module, source_id);

-- 更新已有文档的 source_type 为 USER_UPLOADED（向后兼容）
UPDATE public.doc_document SET source_type = 'USER_UPLOADED' WHERE source_type IS NULL;

