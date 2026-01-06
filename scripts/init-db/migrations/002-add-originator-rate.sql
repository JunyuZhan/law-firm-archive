-- 迁移脚本：添加案源人比例字段
-- 日期：2026-01-05
-- 说明：为 finance_contract 和 finance_commission_rule 表添加 originator_rate 字段

-- 1. 为 finance_commission_rule 表添加 originator_rate 字段
ALTER TABLE public.finance_commission_rule 
ADD COLUMN IF NOT EXISTS originator_rate numeric(5,2) DEFAULT 0;

COMMENT ON COLUMN public.finance_commission_rule.originator_rate IS '案源人比例(%)，0表示不参与分配';

-- 2. 为 finance_contract 表添加 originator_rate 字段
ALTER TABLE public.finance_contract 
ADD COLUMN IF NOT EXISTS originator_rate numeric(5,2);

COMMENT ON COLUMN public.finance_contract.originator_rate IS '案源人比例(%)';

-- 3. 更新现有数据：将 originator_rate 设置为默认值 0
UPDATE public.finance_commission_rule 
SET originator_rate = 0 
WHERE originator_rate IS NULL;

UPDATE public.finance_contract 
SET originator_rate = 0 
WHERE originator_rate IS NULL;

