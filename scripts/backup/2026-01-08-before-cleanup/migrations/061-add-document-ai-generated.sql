-- 添加 AI 生成标记字段
-- 用于标记文档是否由 AI 大模型生成

ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS ai_generated BOOLEAN DEFAULT false;

COMMENT ON COLUMN doc_document.ai_generated IS '是否由 AI 生成';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_doc_document_ai_generated ON doc_document(ai_generated);

