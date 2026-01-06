-- ============================================
-- 证据管理模块 - 添加文件类型和缩略图字段
-- 版本: 1.0.1
-- 创建日期: 2026-01-05
-- ============================================

-- 添加文件类型字段
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS file_type VARCHAR(50);
COMMENT ON COLUMN evidence.file_type IS '文件类型分类: image, pdf, word, excel, ppt, audio, video, other';

-- 添加缩略图URL字段
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(1000);
COMMENT ON COLUMN evidence.thumbnail_url IS '缩略图URL（仅图片文件）';

-- 更新现有数据的文件类型（根据文件名后缀）
UPDATE evidence SET file_type = 
    CASE 
        WHEN LOWER(file_name) LIKE '%.jpg' OR LOWER(file_name) LIKE '%.jpeg' OR LOWER(file_name) LIKE '%.png' 
             OR LOWER(file_name) LIKE '%.gif' OR LOWER(file_name) LIKE '%.bmp' OR LOWER(file_name) LIKE '%.webp' THEN 'image'
        WHEN LOWER(file_name) LIKE '%.pdf' THEN 'pdf'
        WHEN LOWER(file_name) LIKE '%.doc' OR LOWER(file_name) LIKE '%.docx' THEN 'word'
        WHEN LOWER(file_name) LIKE '%.xls' OR LOWER(file_name) LIKE '%.xlsx' THEN 'excel'
        WHEN LOWER(file_name) LIKE '%.ppt' OR LOWER(file_name) LIKE '%.pptx' THEN 'ppt'
        WHEN LOWER(file_name) LIKE '%.mp3' OR LOWER(file_name) LIKE '%.wav' OR LOWER(file_name) LIKE '%.m4a' OR LOWER(file_name) LIKE '%.aac' THEN 'audio'
        WHEN LOWER(file_name) LIKE '%.mp4' OR LOWER(file_name) LIKE '%.avi' OR LOWER(file_name) LIKE '%.mov' OR LOWER(file_name) LIKE '%.wmv' THEN 'video'
        ELSE 'other'
    END
WHERE file_name IS NOT NULL AND file_type IS NULL;

-- 为图片文件设置缩略图URL（使用原文件URL）
UPDATE evidence SET thumbnail_url = file_url
WHERE file_type = 'image' AND file_url IS NOT NULL AND thumbnail_url IS NULL;
