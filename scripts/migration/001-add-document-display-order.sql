-- 添加文档排序字段
-- 用于卷宗归档文件的排列顺序

-- 添加 display_order 字段
ALTER TABLE doc_document 
ADD COLUMN IF NOT EXISTS display_order INTEGER DEFAULT 0;

-- 添加字段注释
COMMENT ON COLUMN doc_document.display_order IS '显示排序顺序（同一目录内的排序，数值越小越靠前）';

-- 为现有文档设置默认排序（按创建时间）
UPDATE doc_document 
SET display_order = subquery.row_num
FROM (
    SELECT id, 
           ROW_NUMBER() OVER (PARTITION BY dossier_item_id ORDER BY created_at) as row_num
    FROM doc_document
) AS subquery
WHERE doc_document.id = subquery.id;

-- 创建索引以优化排序查询
CREATE INDEX IF NOT EXISTS idx_doc_document_display_order 
ON doc_document(dossier_item_id, display_order);

