-- 为 seal_info 和 seal_application 表添加 updated_by 字段
-- 创建日期: 2026-01-07
-- 说明: BaseEntity 包含 updated_by 字段，但这些表缺少此字段

-- 为 seal_info 表添加 updated_by 字段
ALTER TABLE public.seal_info 
ADD COLUMN IF NOT EXISTS updated_by BIGINT;

COMMENT ON COLUMN public.seal_info.updated_by IS '最后更新人ID';

-- 为 seal_application 表添加 updated_by 字段
ALTER TABLE public.seal_application 
ADD COLUMN IF NOT EXISTS updated_by BIGINT;

COMMENT ON COLUMN public.seal_application.updated_by IS '最后更新人ID';

