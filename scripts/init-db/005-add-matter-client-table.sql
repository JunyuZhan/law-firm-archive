-- 迁移脚本：添加matter_client关联表，支持一个项目关联多个客户
-- 日期：2026-01-06

-- 创建matter_client关联表
CREATE TABLE IF NOT EXISTS public.matter_client (
    id bigserial PRIMARY KEY,
    matter_id bigint NOT NULL,
    client_id bigint NOT NULL,
    client_role character varying(50) DEFAULT 'PLAINTIFF',  -- 客户角色：PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人
    is_primary boolean DEFAULT false,  -- 是否主要客户（用于显示和结算）
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    CONSTRAINT fk_matter_client_matter FOREIGN KEY (matter_id) REFERENCES public.matter(id),
    CONSTRAINT fk_matter_client_client FOREIGN KEY (client_id) REFERENCES public.crm_client(id)
);

-- 添加注释
COMMENT ON TABLE public.matter_client IS '项目-客户关联表（支持多客户）';
COMMENT ON COLUMN public.matter_client.matter_id IS '项目ID';
COMMENT ON COLUMN public.matter_client.client_id IS '客户ID';
COMMENT ON COLUMN public.matter_client.client_role IS '客户角色：PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人';
COMMENT ON COLUMN public.matter_client.is_primary IS '是否主要客户（用于显示和结算）';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_matter_client_matter_id ON public.matter_client(matter_id);
CREATE INDEX IF NOT EXISTS idx_matter_client_client_id ON public.matter_client(client_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_matter_client_unique ON public.matter_client(matter_id, client_id) WHERE deleted = false;

-- 迁移现有数据：将matter表的client_id迁移到matter_client表
INSERT INTO public.matter_client (matter_id, client_id, is_primary, created_at, updated_at)
SELECT id, client_id, true, created_at, updated_at
FROM public.matter 
WHERE client_id IS NOT NULL AND deleted = false
ON CONFLICT DO NOTHING;

-- 注意：暂时保留matter.client_id字段，便于向后兼容
-- 后续可以考虑删除该字段

