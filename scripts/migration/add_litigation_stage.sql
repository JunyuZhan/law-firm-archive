-- =====================================================
-- 迁移脚本：添加代理阶段字段
-- =====================================================
-- 日期: 2026-01-12
-- 描述: 为matter表添加litigation_stage字段，记录案件的代理阶段
-- =====================================================

-- 添加代理阶段字段
ALTER TABLE public.matter 
ADD COLUMN IF NOT EXISTS litigation_stage character varying(30);

-- 添加字段注释
COMMENT ON COLUMN public.matter.litigation_stage IS '代理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行, ARBITRATION-仲裁, CONSULTATION-咨询, ALL_STAGES-全阶段';

-- 可选：为诉讼案件设置默认值（一审）
-- UPDATE public.matter 
-- SET litigation_stage = 'FIRST_INSTANCE' 
-- WHERE matter_type = 'LITIGATION' AND litigation_stage IS NULL;
