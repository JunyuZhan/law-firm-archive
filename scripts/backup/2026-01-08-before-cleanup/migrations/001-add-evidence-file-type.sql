-- ============================================
-- 迁移脚本: 添加 evidence 表缺失的字段
-- 版本: 001
-- 创建日期: 2026-01-05
-- ============================================

-- 添加 file_type 列
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS file_type VARCHAR(50);

-- 添加 thumbnail_url 列
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(1000);

-- 添加注释
COMMENT ON COLUMN evidence.file_type IS '文件类型分类: image, pdf, word, excel, ppt, audio, video, other';
COMMENT ON COLUMN evidence.thumbnail_url IS '缩略图URL（仅图片文件）';
