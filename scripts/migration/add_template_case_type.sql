-- 文档模板表添加案件类型字段
-- 用于区分不同案件类型的授权委托书等模板

-- 添加 case_type 列
ALTER TABLE public.doc_template 
ADD COLUMN IF NOT EXISTS case_type character varying(30) DEFAULT 'ALL';

-- 添加注释
COMMENT ON COLUMN public.doc_template.case_type IS '案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, ALL-通用';

-- 更新已有数据，设置默认值为 ALL
UPDATE public.doc_template 
SET case_type = 'ALL' 
WHERE case_type IS NULL;

-- 创建索引优化查询
CREATE INDEX IF NOT EXISTS idx_doc_template_type_case 
ON public.doc_template(template_type, case_type);
