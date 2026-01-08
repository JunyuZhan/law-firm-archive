-- =====================================================
-- 律师事务所管理系统 - PostgreSQL 扩展
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 创建必要的 PostgreSQL 扩展
-- =====================================================

-- 文本相似度搜索扩展（用于模糊搜索）
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;
COMMENT ON EXTENSION pg_trgm IS '文本相似度测量和基于三元组的索引搜索';

-- UUID 生成扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
COMMENT ON EXTENSION "uuid-ossp" IS '生成通用唯一标识符(UUID)';
