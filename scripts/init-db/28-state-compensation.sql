-- =====================================================
-- 律师事务所管理系统 - 国家赔偿案件扩展表
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-17
-- 描述: 国家赔偿案件业务信息扩展表
--       支持行政国家赔偿(STATE_COMP_ADMIN)和刑事国家赔偿(STATE_COMP_CRIMINAL)
-- =====================================================

-- 创建序列
CREATE SEQUENCE IF NOT EXISTS matter_state_compensation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 创建国家赔偿案件业务信息表
CREATE TABLE IF NOT EXISTS public.matter_state_compensation (
    id bigint PRIMARY KEY DEFAULT nextval('matter_state_compensation_id_seq'::regclass),
    matter_id bigint NOT NULL,

    -- 赔偿义务机关
    obligor_org_name VARCHAR(255),
    obligor_org_type VARCHAR(50),

    -- 致损行为
    case_source VARCHAR(50),
    damage_description TEXT,

    -- 刑事赔偿特有字段
    criminal_case_terminated BOOLEAN,
    criminal_case_no VARCHAR(100),
    compensation_committee VARCHAR(255),

    -- 程序日期
    application_date DATE,
    acceptance_date DATE,
    decision_date DATE,
    reconsideration_date DATE,
    reconsideration_decision_date DATE,
    committee_app_date DATE,
    committee_decision_date DATE,
    admin_litigation_filing_date DATE,
    admin_litigation_court_name VARCHAR(255),

    -- 赔偿请求
    claim_amount NUMERIC(18,2),
    compensation_items JSONB,

    -- 决定结果
    decision_result VARCHAR(50),
    approved_amount NUMERIC(18,2),
    payment_status VARCHAR(50),
    payment_date DATE,

    remark TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

-- 表注释
COMMENT ON TABLE public.matter_state_compensation IS '国家赔偿案件业务信息扩展表';

-- 字段注释
COMMENT ON COLUMN public.matter_state_compensation.id IS '主键ID';
COMMENT ON COLUMN public.matter_state_compensation.matter_id IS '关联案件ID';
COMMENT ON COLUMN public.matter_state_compensation.obligor_org_name IS '赔偿义务机关名称';
COMMENT ON COLUMN public.matter_state_compensation.obligor_org_type IS '义务机关类型: PUBLIC_SECURITY-公安机关, PROCURATORATE-检察机关, COURT-审判机关, PRISON-监狱管理机关, ADMIN_ORGAN-行政机关, OTHER-其他';
COMMENT ON COLUMN public.matter_state_compensation.case_source IS '致损行为类型: ILLEGAL_DETENTION-违法拘留, ILLEGAL_COERCIVE-违法采取强制措施, WRONGFUL_CONVICT-错误判决等';
COMMENT ON COLUMN public.matter_state_compensation.damage_description IS '损害情况描述';
COMMENT ON COLUMN public.matter_state_compensation.criminal_case_terminated IS '刑事诉讼是否终结(刑事赔偿必填)';
COMMENT ON COLUMN public.matter_state_compensation.criminal_case_no IS '原刑事案件编号';
COMMENT ON COLUMN public.matter_state_compensation.compensation_committee IS '受理的赔偿委员会';
COMMENT ON COLUMN public.matter_state_compensation.application_date IS '赔偿申请日(2年时效)';
COMMENT ON COLUMN public.matter_state_compensation.acceptance_date IS '受理日';
COMMENT ON COLUMN public.matter_state_compensation.decision_date IS '赔偿义务机关决定日(2个月期限)';
COMMENT ON COLUMN public.matter_state_compensation.reconsideration_date IS '复议/复核申请日(30日期限)';
COMMENT ON COLUMN public.matter_state_compensation.reconsideration_decision_date IS '复议决定日';
COMMENT ON COLUMN public.matter_state_compensation.committee_app_date IS '赔偿委员会申请日';
COMMENT ON COLUMN public.matter_state_compensation.committee_decision_date IS '赔偿委员会决定日';
COMMENT ON COLUMN public.matter_state_compensation.admin_litigation_filing_date IS '行政赔偿诉讼立案日';
COMMENT ON COLUMN public.matter_state_compensation.admin_litigation_court_name IS '行政诉讼法院';
COMMENT ON COLUMN public.matter_state_compensation.claim_amount IS '请求赔偿总额';
COMMENT ON COLUMN public.matter_state_compensation.compensation_items IS '赔偿项目明细JSONB';
COMMENT ON COLUMN public.matter_state_compensation.decision_result IS '决定结果: GRANTED-全部支持, DENIED-不予赔偿, PARTIAL_GRANTED-部分支持';
COMMENT ON COLUMN public.matter_state_compensation.approved_amount IS '决定赔偿金额';
COMMENT ON COLUMN public.matter_state_compensation.payment_status IS '支付状态: UNPAID-未支付, PAID-已支付, PARTIAL_PAID-部分支付';
COMMENT ON COLUMN public.matter_state_compensation.payment_date IS '支付日期';

-- 创建索引
CREATE INDEX idx_msc_matter_id ON public.matter_state_compensation(matter_id);
CREATE INDEX idx_msc_obligor_org_type ON public.matter_state_compensation(obligor_org_type);
CREATE INDEX idx_msc_decision_date ON public.matter_state_compensation(decision_date);
CREATE INDEX idx_msc_deleted ON public.matter_state_compensation(deleted);

-- 创建外键约束
ALTER TABLE public.matter_state_compensation
    ADD CONSTRAINT fk_msc_matter
    FOREIGN KEY (matter_id)
    REFERENCES public.matter(id)
    ON DELETE CASCADE;

-- 添加唯一约束（一个案件只能有一条国家赔偿记录）
CREATE UNIQUE INDEX idx_msc_matter_unique ON public.matter_state_compensation(matter_id) WHERE deleted = FALSE;

-- =====================================================
-- 修改 matter 表的 case_type 字段注释，添加国家赔偿类型说明
-- =====================================================
COMMENT ON COLUMN public.matter.case_type IS '案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, STATE_COMP_ADMIN-行政国家赔偿, STATE_COMP_CRIMINAL-刑事国家赔偿, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务';

-- =====================================================
-- 国家赔偿相关字典数据
-- =====================================================
-- 注意：以下字典类型已在 27-dict-init-data.sql 中定义：
--   - 赔偿义务机关类型 (obligor_org_type) - ID=63
--   - 致损行为类型 (damage_cause_type) - ID=64
-- 此脚本不再重复定义，避免唯一约束冲突
