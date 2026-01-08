-- =====================================================
-- 律师事务所管理系统 - 财务管理模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 合同、收费、发票、支付、提成、费用等财务相关表
-- =====================================================

--
-- Name: fin_payment_amendment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fin_payment_amendment (
    id bigint NOT NULL,
    payment_id bigint NOT NULL,
    original_amount numeric(15,2) NOT NULL,
    new_amount numeric(15,2) NOT NULL,
    reason text NOT NULL,
    requested_by bigint NOT NULL,
    requested_at timestamp without time zone NOT NULL,
    approved_by bigint,
    approved_at timestamp without time zone,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    reject_reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE fin_payment_amendment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.fin_payment_amendment IS '收款变更申请表';
--
-- Name: COLUMN fin_payment_amendment.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.id IS '主键ID';
--
-- Name: COLUMN fin_payment_amendment.payment_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.payment_id IS '关联收款记录ID';
--
-- Name: COLUMN fin_payment_amendment.original_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.original_amount IS '原金额';
--
-- Name: COLUMN fin_payment_amendment.new_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.new_amount IS '新金额';
--
-- Name: COLUMN fin_payment_amendment.reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.reason IS '变更原因';
--
-- Name: COLUMN fin_payment_amendment.requested_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.requested_by IS '申请人ID';
--
-- Name: COLUMN fin_payment_amendment.requested_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.requested_at IS '申请时间';
--
-- Name: COLUMN fin_payment_amendment.approved_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.approved_by IS '审批人ID';
--
-- Name: COLUMN fin_payment_amendment.approved_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.approved_at IS '审批时间';
--
-- Name: COLUMN fin_payment_amendment.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.status IS '状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝';
--
-- Name: COLUMN fin_payment_amendment.reject_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.reject_reason IS '拒绝原因';
--
-- Name: fin_payment_amendment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fin_payment_amendment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: fin_payment_amendment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fin_payment_amendment_id_seq OWNED BY public.fin_payment_amendment.id;
--
-- Name: finance_commission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_commission (
    id bigint NOT NULL,
    commission_no character varying(50) NOT NULL,
    payment_id bigint NOT NULL,
    fee_id bigint,
    contract_id bigint,
    matter_id bigint,
    client_id bigint,
    rule_id bigint,
    rule_code character varying(50),
    payment_amount numeric(15,2),
    firm_retention numeric(15,2),
    commission_base numeric(15,2),
    tax_amount numeric(15,2),
    management_fee numeric(15,2),
    net_commission numeric(15,2),
    originator_id bigint,
    originator_commission numeric(15,2),
    status character varying(50) DEFAULT 'PENDING'::character varying,
    approved_by bigint,
    approved_at timestamp without time zone,
    paid_by bigint,
    paid_at timestamp without time zone,
    remark text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false,
    gross_amount numeric(15,2),
    cost_amount numeric(15,2),
    net_amount numeric(15,2),
    distribution_ratio numeric(5,2),
    commission_rate numeric(5,2),
    commission_amount numeric(15,2),
    compensation_type character varying(20)
);
--
-- Name: TABLE finance_commission; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_commission IS '提成记录表';
--
-- Name: COLUMN finance_commission.commission_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.commission_no IS '提成编号';
--
-- Name: COLUMN finance_commission.payment_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.payment_id IS '收款记录ID';
--
-- Name: COLUMN finance_commission.fee_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.fee_id IS '收费记录ID';
--
-- Name: COLUMN finance_commission.contract_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.contract_id IS '合同ID';
--
-- Name: COLUMN finance_commission.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.matter_id IS '案件ID';
--
-- Name: COLUMN finance_commission.client_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.client_id IS '客户ID';
--
-- Name: COLUMN finance_commission.rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.rule_id IS '提成规则ID';
--
-- Name: COLUMN finance_commission.rule_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.rule_code IS '提成规则编码';
--
-- Name: COLUMN finance_commission.payment_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.payment_amount IS '收款金额';
--
-- Name: COLUMN finance_commission.firm_retention; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.firm_retention IS '律所留存';
--
-- Name: COLUMN finance_commission.commission_base; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.commission_base IS '提成基数';
--
-- Name: COLUMN finance_commission.tax_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.tax_amount IS '税费';
--
-- Name: COLUMN finance_commission.management_fee; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.management_fee IS '管理费';
--
-- Name: COLUMN finance_commission.net_commission; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.net_commission IS '净提成';
--
-- Name: COLUMN finance_commission.originator_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.originator_id IS '案源人ID';
--
-- Name: COLUMN finance_commission.originator_commission; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.originator_commission IS '案源提成';
--
-- Name: COLUMN finance_commission.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.status IS '状态：PENDING-待审批, APPROVED-已审批, PAID-已发放, CANCELLED-已取消';
--
-- Name: COLUMN finance_commission.gross_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.gross_amount IS '毛收入';
--
-- Name: COLUMN finance_commission.cost_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.cost_amount IS '成本';
--
-- Name: COLUMN finance_commission.net_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.net_amount IS '净收入';
--
-- Name: COLUMN finance_commission.distribution_ratio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.distribution_ratio IS '分配比例';
--
-- Name: COLUMN finance_commission.commission_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.commission_rate IS '提成比例';
--
-- Name: COLUMN finance_commission.commission_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.commission_amount IS '提成金额';
--
-- Name: COLUMN finance_commission.compensation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.compensation_type IS '薪酬模式：COMMISSION-提成制, SALARIED-授薪制, HYBRID-混合制';
--
-- Name: finance_commission_detail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_commission_detail (
    id bigint NOT NULL,
    commission_id bigint NOT NULL,
    user_id bigint NOT NULL,
    user_name character varying(100),
    role_in_matter character varying(50),
    allocation_rate numeric(5,2) NOT NULL,
    commission_amount numeric(15,2) NOT NULL,
    tax_amount numeric(15,2),
    net_amount numeric(15,2) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    created_by bigint,
    updated_by bigint
);
--
-- Name: TABLE finance_commission_detail; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_commission_detail IS '提成分配明细表';
--
-- Name: COLUMN finance_commission_detail.commission_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.commission_id IS '提成记录ID';
--
-- Name: COLUMN finance_commission_detail.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.user_id IS '用户ID';
--
-- Name: COLUMN finance_commission_detail.user_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.user_name IS '用户姓名';
--
-- Name: COLUMN finance_commission_detail.role_in_matter; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.role_in_matter IS '案件角色';
--
-- Name: COLUMN finance_commission_detail.allocation_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.allocation_rate IS '分配比例';
--
-- Name: COLUMN finance_commission_detail.commission_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.commission_amount IS '提成金额';
--
-- Name: COLUMN finance_commission_detail.tax_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.tax_amount IS '税费';
--
-- Name: COLUMN finance_commission_detail.net_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.net_amount IS '净提成';
--
-- Name: finance_commission_detail_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_commission_detail_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_commission_detail_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_commission_detail_id_seq OWNED BY public.finance_commission_detail.id;
--
-- Name: finance_commission_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_commission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_commission_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_commission_id_seq OWNED BY public.finance_commission.id;
--
-- Name: finance_commission_rule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_commission_rule (
    id bigint NOT NULL,
    rule_code character varying(50) NOT NULL,
    rule_name character varying(200) NOT NULL,
    rule_type character varying(50),
    firm_rate numeric(5,2) DEFAULT 0,
    lead_lawyer_rate numeric(5,2) DEFAULT 0,
    assist_lawyer_rate numeric(5,2) DEFAULT 0,
    support_staff_rate numeric(5,2) DEFAULT 0,
    allow_modify boolean DEFAULT true,
    description text,
    is_default boolean DEFAULT false,
    active boolean DEFAULT true,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false,
    originator_rate numeric(5,2) DEFAULT 0
);
--
-- Name: TABLE finance_commission_rule; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_commission_rule IS '提成规则表';
--
-- Name: COLUMN finance_commission_rule.rule_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.rule_code IS '规则编码';
--
-- Name: COLUMN finance_commission_rule.rule_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.rule_name IS '规则名称';
--
-- Name: COLUMN finance_commission_rule.rule_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.rule_type IS '规则类型';
--
-- Name: COLUMN finance_commission_rule.firm_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.firm_rate IS '律所比例(%)，0表示不参与分配';
--
-- Name: COLUMN finance_commission_rule.lead_lawyer_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.lead_lawyer_rate IS '主办律师比例(%)，0表示不参与分配';
--
-- Name: COLUMN finance_commission_rule.assist_lawyer_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.assist_lawyer_rate IS '协办律师比例(%)，0表示无协办或不参与分配';
--
-- Name: COLUMN finance_commission_rule.support_staff_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.support_staff_rate IS '辅助人员比例(%)，0表示无辅助或不参与分配';
--
-- Name: COLUMN finance_commission_rule.allow_modify; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.allow_modify IS '律师创建合同时是否允许修改比例';
--
-- Name: COLUMN finance_commission_rule.is_default; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.is_default IS '是否默认规则';
--
-- Name: COLUMN finance_commission_rule.active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.active IS '是否启用';
--
-- Name: COLUMN finance_commission_rule.originator_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.originator_rate IS '案源人比例(%)，0表示不参与分配';
--
-- Name: finance_commission_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_commission_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_commission_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_commission_rule_id_seq OWNED BY public.finance_commission_rule.id;
--
-- Name: finance_contract; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_contract (
    id bigint NOT NULL,
    contract_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    template_id bigint,
    client_id bigint NOT NULL,
    matter_id bigint,
    contract_type character varying(20) NOT NULL,
    fee_type character varying(20) NOT NULL,
    total_amount numeric(15,2) NOT NULL,
    paid_amount numeric(15,2) DEFAULT 0,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    sign_date date,
    effective_date date,
    expiry_date date,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    signer_id bigint,
    department_id bigint,
    content text,
    file_url character varying(500),
    payment_terms text,
    remark text,
    case_type character varying(30),
    cause_of_action character varying(50),
    trial_stage character varying(50),
    claim_amount numeric(15,2),
    jurisdiction_court character varying(200),
    opposing_party character varying(200),
    conflict_check_status character varying(20) DEFAULT 'NOT_REQUIRED'::character varying,
    archive_status character varying(20) DEFAULT 'NOT_ARCHIVED'::character varying,
    advance_travel_fee numeric(15,2),
    risk_ratio numeric(5,2),
    seal_record text,
    commission_rule_id bigint,
    firm_rate numeric(5,2),
    lead_lawyer_rate numeric(5,2),
    assist_lawyer_rate numeric(5,2),
    support_staff_rate numeric(5,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    originator_rate numeric(5,2),
    case_summary text
);
--
-- Name: TABLE finance_contract; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_contract IS '委托合同表';
--
-- Name: COLUMN finance_contract.contract_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.contract_no IS '合同编号';
--
-- Name: COLUMN finance_contract.template_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.template_id IS '使用的模板ID';
--
-- Name: COLUMN finance_contract.contract_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.contract_type IS '合同类型：SERVICE-服务合同, RETAINER-常年法顾, LITIGATION-诉讼代理, NON_LITIGATION-非诉项目';
--
-- Name: COLUMN finance_contract.fee_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.fee_type IS '收费方式：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理, MIXED-混合收费';
--
-- Name: COLUMN finance_contract.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.status IS '合同状态：DRAFT-草稿, PENDING-待审批, ACTIVE-生效中, REJECTED-已拒绝, TERMINATED-已终止, COMPLETED-已完成, EXPIRED-已过期';
--
-- Name: COLUMN finance_contract.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.content IS '合同内容（基于模板生成）';
--
-- Name: COLUMN finance_contract.case_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.case_type IS '案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政等';
--
-- Name: COLUMN finance_contract.cause_of_action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.cause_of_action IS '案由代码';
--
-- Name: COLUMN finance_contract.trial_stage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.trial_stage IS '审理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行, NON_LITIGATION-非诉';
--
-- Name: COLUMN finance_contract.claim_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.claim_amount IS '标的金额';
--
-- Name: COLUMN finance_contract.jurisdiction_court; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.jurisdiction_court IS '管辖法院';
--
-- Name: COLUMN finance_contract.opposing_party; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.opposing_party IS '对方当事人';
--
-- Name: COLUMN finance_contract.conflict_check_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.conflict_check_status IS '利冲审查状态：PENDING-待审查, PASSED-已通过, FAILED-未通过, NOT_REQUIRED-无需审查';
--
-- Name: COLUMN finance_contract.archive_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.archive_status IS '归档状态：NOT_ARCHIVED-未归档, ARCHIVED-已归档, DESTROYED-已销毁';
--
-- Name: COLUMN finance_contract.advance_travel_fee; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.advance_travel_fee IS '预支差旅费';
--
-- Name: COLUMN finance_contract.risk_ratio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.risk_ratio IS '风险代理比例（0-100）';
--
-- Name: COLUMN finance_contract.seal_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.seal_record IS '印章使用记录（JSON格式）';
--
-- Name: COLUMN finance_contract.commission_rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.commission_rule_id IS '提成规则ID（选择的预设方案）';
--
-- Name: COLUMN finance_contract.firm_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.firm_rate IS '律所比例(%)';
--
-- Name: COLUMN finance_contract.lead_lawyer_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.lead_lawyer_rate IS '主办律师比例(%)';
--
-- Name: COLUMN finance_contract.assist_lawyer_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.assist_lawyer_rate IS '协办律师比例(%)';
--
-- Name: COLUMN finance_contract.support_staff_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.support_staff_rate IS '辅助人员比例(%)';
--
-- Name: COLUMN finance_contract.originator_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.originator_rate IS '案源人比例(%)';
--
-- Name: COLUMN finance_contract.case_summary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.case_summary IS '案情摘要（用于审批表）';
--
-- Name: finance_contract_amendment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_contract_amendment (
    id bigint NOT NULL,
    amendment_no character varying(50) NOT NULL,
    contract_id bigint NOT NULL,
    amendment_type character varying(50) NOT NULL,
    before_snapshot jsonb,
    after_snapshot jsonb,
    amendment_reason text,
    lawyer_amended_by bigint,
    lawyer_amended_at timestamp without time zone,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    finance_handled_by bigint,
    finance_handled_at timestamp without time zone,
    finance_remark text,
    affects_payments boolean DEFAULT false,
    affected_payment_ids jsonb,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE finance_contract_amendment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_contract_amendment IS '财务合同变更记录表';
--
-- Name: COLUMN finance_contract_amendment.amendment_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.amendment_no IS '变更编号';
--
-- Name: COLUMN finance_contract_amendment.contract_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.contract_id IS '合同ID';
--
-- Name: COLUMN finance_contract_amendment.amendment_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.amendment_type IS '变更类型：AMOUNT-金额变更, PARTICIPANT-参与人变更, SCHEDULE-付款计划变更, OTHER-其他';
--
-- Name: COLUMN finance_contract_amendment.before_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.before_snapshot IS '变更前数据快照（JSON格式）';
--
-- Name: COLUMN finance_contract_amendment.after_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.after_snapshot IS '变更后数据快照（JSON格式）';
--
-- Name: COLUMN finance_contract_amendment.amendment_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.amendment_reason IS '变更说明';
--
-- Name: COLUMN finance_contract_amendment.lawyer_amended_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.lawyer_amended_by IS '律师变更人ID';
--
-- Name: COLUMN finance_contract_amendment.lawyer_amended_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.lawyer_amended_at IS '律师变更时间';
--
-- Name: COLUMN finance_contract_amendment.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.status IS '状态：PENDING-待处理, SYNCED-已同步, IGNORED-已忽略, PARTIAL-部分同步';
--
-- Name: COLUMN finance_contract_amendment.finance_handled_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.finance_handled_by IS '财务处理人ID';
--
-- Name: COLUMN finance_contract_amendment.finance_handled_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.finance_handled_at IS '财务处理时间';
--
-- Name: COLUMN finance_contract_amendment.finance_remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.finance_remark IS '财务处理备注';
--
-- Name: COLUMN finance_contract_amendment.affects_payments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.affects_payments IS '是否影响已有收款';
--
-- Name: COLUMN finance_contract_amendment.affected_payment_ids; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.affected_payment_ids IS '受影响的收款ID列表';
--
-- Name: finance_contract_amendment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_contract_amendment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_contract_amendment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_contract_amendment_id_seq OWNED BY public.finance_contract_amendment.id;
--
-- Name: finance_contract_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_contract_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_contract_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_contract_id_seq OWNED BY public.finance_contract.id;
--
-- Name: finance_cost_allocation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_cost_allocation (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    expense_id bigint NOT NULL,
    allocated_amount numeric(15,2) NOT NULL,
    allocation_date date NOT NULL,
    allocated_by bigint NOT NULL,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);
--
-- Name: TABLE finance_cost_allocation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_cost_allocation IS '成本归集记录表';
--
-- Name: COLUMN finance_cost_allocation.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_allocation.matter_id IS '项目ID';
--
-- Name: COLUMN finance_cost_allocation.expense_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_allocation.expense_id IS '费用ID';
--
-- Name: COLUMN finance_cost_allocation.allocated_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_allocation.allocated_amount IS '归集金额';
--
-- Name: finance_cost_allocation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_cost_allocation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_cost_allocation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_cost_allocation_id_seq OWNED BY public.finance_cost_allocation.id;
--
-- Name: finance_cost_split; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_cost_split (
    id bigint NOT NULL,
    expense_id bigint NOT NULL,
    matter_id bigint NOT NULL,
    split_amount numeric(15,2) NOT NULL,
    split_ratio numeric(5,4),
    split_method character varying(50),
    split_date date NOT NULL,
    split_by bigint NOT NULL,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE finance_cost_split; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_cost_split IS '成本分摊表';
--
-- Name: COLUMN finance_cost_split.expense_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.expense_id IS '费用ID（公共费用）';
--
-- Name: COLUMN finance_cost_split.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.matter_id IS '分摊到的项目ID';
--
-- Name: COLUMN finance_cost_split.split_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.split_amount IS '分摊金额';
--
-- Name: COLUMN finance_cost_split.split_ratio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.split_ratio IS '分摊比例';
--
-- Name: COLUMN finance_cost_split.split_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.split_method IS '分摊方式：EQUAL-平均分摊, RATIO-按比例, MANUAL-手动指定';
--
-- Name: finance_cost_split_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_cost_split_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_cost_split_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_cost_split_id_seq OWNED BY public.finance_cost_split.id;
--
-- Name: finance_expense; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_expense (
    id bigint NOT NULL,
    expense_no character varying(50) NOT NULL,
    matter_id bigint,
    applicant_id bigint NOT NULL,
    expense_type character varying(50),
    expense_category character varying(50),
    expense_date date NOT NULL,
    amount numeric(15,2) NOT NULL,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    description text,
    vendor_name character varying(200),
    invoice_no character varying(100),
    invoice_url character varying(500),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approver_id bigint,
    approved_at timestamp without time zone,
    approval_comment text,
    paid_at timestamp without time zone,
    paid_by bigint,
    payment_method character varying(50),
    is_cost_allocation boolean DEFAULT false,
    allocated_to_matter_id bigint,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE finance_expense; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_expense IS '费用报销表';
--
-- Name: COLUMN finance_expense.expense_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.expense_no IS '报销单号';
--
-- Name: COLUMN finance_expense.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.matter_id IS '关联项目ID（可选）';
--
-- Name: COLUMN finance_expense.applicant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.applicant_id IS '申请人ID';
--
-- Name: COLUMN finance_expense.expense_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.expense_type IS '费用类型：TRAVEL-差旅费, MEAL-餐费, ACCOMMODATION-住宿费, TRANSPORT-交通费, MATERIAL-材料费, OTHER-其他';
--
-- Name: COLUMN finance_expense.expense_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.expense_category IS '费用分类：CASE_COST-办案成本, OFFICE_COST-办公费用, OTHER-其他';
--
-- Name: COLUMN finance_expense.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.status IS '状态：PENDING-待审批, APPROVED-已审批, REJECTED-已驳回, PAID-已支付';
--
-- Name: COLUMN finance_expense.is_cost_allocation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.is_cost_allocation IS '是否已归集到项目成本';
--
-- Name: finance_expense_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_expense_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_expense_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_expense_id_seq OWNED BY public.finance_expense.id;
--
-- Name: finance_fee; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_fee (
    id bigint NOT NULL,
    fee_no character varying(50) NOT NULL,
    contract_id bigint NOT NULL,
    matter_id bigint,
    client_id bigint NOT NULL,
    fee_type character varying(20) NOT NULL,
    amount numeric(15,2) NOT NULL,
    paid_amount numeric(15,2) DEFAULT 0,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    planned_date date NOT NULL,
    actual_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    fee_name character varying(200),
    responsible_id bigint
);
--
-- Name: TABLE finance_fee; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_fee IS '收费记录表';
--
-- Name: COLUMN finance_fee.fee_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_fee.fee_type IS '收费类型：RETAINER-预付款, PROGRESS-进度款, FINAL-尾款, OTHER-其他';
--
-- Name: COLUMN finance_fee.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_fee.status IS '状态：PENDING-待收, PARTIAL-部分收款, PAID-已收清, CANCELLED-已取消';
--
-- Name: finance_fee_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_fee_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_fee_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_fee_id_seq OWNED BY public.finance_fee.id;
--
-- Name: finance_invoice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_invoice (
    id bigint NOT NULL,
    invoice_no character varying(50) NOT NULL,
    fee_id bigint,
    contract_id bigint,
    client_id bigint NOT NULL,
    invoice_type character varying(20) NOT NULL,
    title character varying(200) NOT NULL,
    tax_no character varying(50),
    amount numeric(15,2) NOT NULL,
    tax_rate numeric(5,4) DEFAULT 0.06,
    tax_amount numeric(15,2),
    content text,
    invoice_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    applicant_id bigint,
    issuer_id bigint,
    file_url character varying(500),
    remark text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE finance_invoice; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_invoice IS '发票表';
--
-- Name: COLUMN finance_invoice.invoice_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_invoice.invoice_no IS '发票号码';
--
-- Name: COLUMN finance_invoice.invoice_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_invoice.invoice_type IS '发票类型：SPECIAL-增值税专用发票, NORMAL-增值税普通发票, ELECTRONIC-电子发票';
--
-- Name: COLUMN finance_invoice.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_invoice.status IS '状态：PENDING-待开票, ISSUED-已开票, CANCELLED-已作废, RED-已红冲';
--
-- Name: finance_invoice_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_invoice_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_invoice_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_invoice_id_seq OWNED BY public.finance_invoice.id;
--
-- Name: finance_payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_payment (
    id bigint NOT NULL,
    payment_no character varying(50) NOT NULL,
    fee_id bigint NOT NULL,
    contract_id bigint,
    matter_id bigint,
    client_id bigint NOT NULL,
    amount numeric(15,2) NOT NULL,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    payment_date date NOT NULL,
    payment_method character varying(20) NOT NULL,
    payment_account character varying(100),
    receipt_no character varying(100),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    confirmed_at timestamp without time zone,
    confirmed_by bigint,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    locked boolean DEFAULT false,
    locked_at timestamp without time zone,
    locked_by bigint
);
--
-- Name: TABLE finance_payment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_payment IS '收款记录表';
--
-- Name: COLUMN finance_payment.payment_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.payment_method IS '付款方式：BANK_TRANSFER-银行转账, CASH-现金, CHECK-支票, ALIPAY-支付宝, WECHAT-微信, OTHER-其他';
--
-- Name: COLUMN finance_payment.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.status IS '状态：PENDING-待确认, CONFIRMED-已确认, CANCELLED-已取消';
--
-- Name: COLUMN finance_payment.locked; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.locked IS '是否锁定（登记后自动锁定）';
--
-- Name: COLUMN finance_payment.locked_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.locked_at IS '锁定时间';
--
-- Name: COLUMN finance_payment.locked_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.locked_by IS '锁定人ID';
--
-- Name: finance_payment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_payment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: finance_payment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_payment_id_seq OWNED BY public.finance_payment.id;
--
-- Name: hourly_rate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hourly_rate (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    rate numeric(10,2) NOT NULL,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    effective_date date NOT NULL,
    expiry_date date,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE hourly_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hourly_rate IS '小时费率表';
--
-- Name: hourly_rate_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hourly_rate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: hourly_rate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hourly_rate_id_seq OWNED BY public.hourly_rate.id;
--
-- Name: fin_payment_amendment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment ALTER COLUMN id SET DEFAULT nextval('public.fin_payment_amendment_id_seq'::regclass);
--
-- Name: finance_commission id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission ALTER COLUMN id SET DEFAULT nextval('public.finance_commission_id_seq'::regclass);
--
-- Name: finance_commission_detail id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_detail ALTER COLUMN id SET DEFAULT nextval('public.finance_commission_detail_id_seq'::regclass);
--
-- Name: finance_commission_rule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_rule ALTER COLUMN id SET DEFAULT nextval('public.finance_commission_rule_id_seq'::regclass);
--
-- Name: finance_contract id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract ALTER COLUMN id SET DEFAULT nextval('public.finance_contract_id_seq'::regclass);
--
-- Name: finance_contract_amendment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract_amendment ALTER COLUMN id SET DEFAULT nextval('public.finance_contract_amendment_id_seq'::regclass);
--
-- Name: finance_cost_allocation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_cost_allocation ALTER COLUMN id SET DEFAULT nextval('public.finance_cost_allocation_id_seq'::regclass);
--
-- Name: finance_cost_split id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_cost_split ALTER COLUMN id SET DEFAULT nextval('public.finance_cost_split_id_seq'::regclass);
--
-- Name: finance_expense id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_expense ALTER COLUMN id SET DEFAULT nextval('public.finance_expense_id_seq'::regclass);
--
-- Name: finance_fee id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_fee ALTER COLUMN id SET DEFAULT nextval('public.finance_fee_id_seq'::regclass);
--
-- Name: finance_invoice id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_invoice ALTER COLUMN id SET DEFAULT nextval('public.finance_invoice_id_seq'::regclass);
--
-- Name: finance_payment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_payment ALTER COLUMN id SET DEFAULT nextval('public.finance_payment_id_seq'::regclass);
--
-- Name: hourly_rate id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hourly_rate ALTER COLUMN id SET DEFAULT nextval('public.hourly_rate_id_seq'::regclass);
--
-- Name: fin_payment_amendment fin_payment_amendment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment
    ADD CONSTRAINT fin_payment_amendment_pkey PRIMARY KEY (id);
--
-- Name: finance_commission finance_commission_commission_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission
    ADD CONSTRAINT finance_commission_commission_no_key UNIQUE (commission_no);
--
-- Name: finance_commission_detail finance_commission_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_detail
    ADD CONSTRAINT finance_commission_detail_pkey PRIMARY KEY (id);
--
-- Name: finance_commission finance_commission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission
    ADD CONSTRAINT finance_commission_pkey PRIMARY KEY (id);
--
-- Name: finance_commission_rule finance_commission_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_rule
    ADD CONSTRAINT finance_commission_rule_pkey PRIMARY KEY (id);
--
-- Name: finance_commission_rule finance_commission_rule_rule_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_rule
    ADD CONSTRAINT finance_commission_rule_rule_code_key UNIQUE (rule_code);
--
-- Name: finance_contract_amendment finance_contract_amendment_amendment_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract_amendment
    ADD CONSTRAINT finance_contract_amendment_amendment_no_key UNIQUE (amendment_no);
--
-- Name: finance_contract_amendment finance_contract_amendment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract_amendment
    ADD CONSTRAINT finance_contract_amendment_pkey PRIMARY KEY (id);
--
-- Name: finance_contract finance_contract_contract_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract
    ADD CONSTRAINT finance_contract_contract_no_key UNIQUE (contract_no);
--
-- Name: finance_contract finance_contract_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract
    ADD CONSTRAINT finance_contract_pkey PRIMARY KEY (id);
--
-- Name: finance_cost_allocation finance_cost_allocation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_cost_allocation
    ADD CONSTRAINT finance_cost_allocation_pkey PRIMARY KEY (id);
--
-- Name: finance_cost_split finance_cost_split_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_cost_split
    ADD CONSTRAINT finance_cost_split_pkey PRIMARY KEY (id);
--
-- Name: finance_expense finance_expense_expense_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_expense
    ADD CONSTRAINT finance_expense_expense_no_key UNIQUE (expense_no);
--
-- Name: finance_expense finance_expense_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_expense
    ADD CONSTRAINT finance_expense_pkey PRIMARY KEY (id);
--
-- Name: finance_fee finance_fee_fee_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_fee
    ADD CONSTRAINT finance_fee_fee_no_key UNIQUE (fee_no);
--
-- Name: finance_fee finance_fee_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_fee
    ADD CONSTRAINT finance_fee_pkey PRIMARY KEY (id);
--
-- Name: finance_invoice finance_invoice_invoice_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_invoice
    ADD CONSTRAINT finance_invoice_invoice_no_key UNIQUE (invoice_no);
--
-- Name: finance_invoice finance_invoice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_invoice
    ADD CONSTRAINT finance_invoice_pkey PRIMARY KEY (id);
--
-- Name: finance_payment finance_payment_payment_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_payment
    ADD CONSTRAINT finance_payment_payment_no_key UNIQUE (payment_no);
--
-- Name: finance_payment finance_payment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_payment
    ADD CONSTRAINT finance_payment_pkey PRIMARY KEY (id);
--
-- Name: hourly_rate hourly_rate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hourly_rate
    ADD CONSTRAINT hourly_rate_pkey PRIMARY KEY (id);
--
-- Name: idx_commission_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_client ON public.finance_commission USING btree (client_id);
--
-- Name: idx_commission_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_contract ON public.finance_commission USING btree (contract_id);
--
-- Name: idx_commission_detail_commission; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_detail_commission ON public.finance_commission_detail USING btree (commission_id);
--
-- Name: idx_commission_detail_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_detail_user ON public.finance_commission_detail USING btree (user_id);
--
-- Name: idx_commission_fee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_fee ON public.finance_commission USING btree (fee_id);
--
-- Name: idx_commission_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_matter ON public.finance_commission USING btree (matter_id);
--
-- Name: idx_commission_originator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_originator ON public.finance_commission USING btree (originator_id);
--
-- Name: idx_commission_payment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_payment ON public.finance_commission USING btree (payment_id);
--
-- Name: idx_commission_rule; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_rule ON public.finance_commission USING btree (rule_id);
--
-- Name: idx_commission_rule_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_rule_code ON public.finance_commission_rule USING btree (rule_code);
--
-- Name: idx_commission_rule_default; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_rule_default ON public.finance_commission_rule USING btree (is_default);
--
-- Name: idx_commission_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_status ON public.finance_commission USING btree (status);
--
-- Name: idx_finance_contract_amendment_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_amendment_contract ON public.finance_contract_amendment USING btree (contract_id);
--
-- Name: idx_finance_contract_amendment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_amendment_status ON public.finance_contract_amendment USING btree (status);
--
-- Name: idx_finance_contract_amendment_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_amendment_type ON public.finance_contract_amendment USING btree (amendment_type);
--
-- Name: idx_finance_contract_case_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_case_type ON public.finance_contract USING btree (case_type);
--
-- Name: idx_finance_contract_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_client_id ON public.finance_contract USING btree (client_id);
--
-- Name: idx_finance_contract_conflict_check_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_conflict_check_status ON public.finance_contract USING btree (conflict_check_status);
--
-- Name: idx_finance_contract_contract_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_contract_no ON public.finance_contract USING btree (contract_no);
--
-- Name: idx_finance_contract_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_matter_id ON public.finance_contract USING btree (matter_id);
--
-- Name: idx_finance_contract_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_status ON public.finance_contract USING btree (status);
--
-- Name: idx_finance_contract_template_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_template_id ON public.finance_contract USING btree (template_id);
--
-- Name: idx_finance_contract_trial_stage; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_trial_stage ON public.finance_contract USING btree (trial_stage);
--
-- Name: idx_finance_cost_allocation_expense_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_cost_allocation_expense_id ON public.finance_cost_allocation USING btree (expense_id);
--
-- Name: idx_finance_cost_allocation_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_cost_allocation_matter_id ON public.finance_cost_allocation USING btree (matter_id);
--
-- Name: idx_finance_cost_split_expense_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_cost_split_expense_id ON public.finance_cost_split USING btree (expense_id);
--
-- Name: idx_finance_cost_split_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_cost_split_matter_id ON public.finance_cost_split USING btree (matter_id);
--
-- Name: idx_finance_expense_applicant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_applicant_id ON public.finance_expense USING btree (applicant_id);
--
-- Name: idx_finance_expense_expense_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_expense_date ON public.finance_expense USING btree (expense_date);
--
-- Name: idx_finance_expense_expense_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_expense_no ON public.finance_expense USING btree (expense_no);
--
-- Name: idx_finance_expense_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_matter_id ON public.finance_expense USING btree (matter_id);
--
-- Name: idx_finance_expense_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_status ON public.finance_expense USING btree (status);
--
-- Name: idx_finance_fee_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_client_id ON public.finance_fee USING btree (client_id);
--
-- Name: idx_finance_fee_contract_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_contract_id ON public.finance_fee USING btree (contract_id);
--
-- Name: idx_finance_fee_fee_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_fee_no ON public.finance_fee USING btree (fee_no);
--
-- Name: idx_finance_fee_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_matter_id ON public.finance_fee USING btree (matter_id);
--
-- Name: idx_finance_fee_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_status ON public.finance_fee USING btree (status);
--
-- Name: idx_finance_invoice_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_client_id ON public.finance_invoice USING btree (client_id);
--
-- Name: idx_finance_invoice_contract_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_contract_id ON public.finance_invoice USING btree (contract_id);
--
-- Name: idx_finance_invoice_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_date ON public.finance_invoice USING btree (invoice_date);
--
-- Name: idx_finance_invoice_fee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_fee_id ON public.finance_invoice USING btree (fee_id);
--
-- Name: idx_finance_invoice_invoice_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_invoice_no ON public.finance_invoice USING btree (invoice_no);
--
-- Name: idx_finance_invoice_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_status ON public.finance_invoice USING btree (status);
--
-- Name: idx_finance_payment_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_client_id ON public.finance_payment USING btree (client_id);
--
-- Name: idx_finance_payment_contract_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_contract_id ON public.finance_payment USING btree (contract_id);
--
-- Name: idx_finance_payment_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_date ON public.finance_payment USING btree (payment_date);
--
-- Name: idx_finance_payment_fee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_fee_id ON public.finance_payment USING btree (fee_id);
--
-- Name: idx_finance_payment_locked; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_locked ON public.finance_payment USING btree (locked);
--
-- Name: idx_finance_payment_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_matter_id ON public.finance_payment USING btree (matter_id);
--
-- Name: idx_finance_payment_payment_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_payment_no ON public.finance_payment USING btree (payment_no);
--
-- Name: idx_finance_payment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_status ON public.finance_payment USING btree (status);
--
-- Name: idx_hourly_rate_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hourly_rate_user ON public.hourly_rate USING btree (user_id);
--
-- Name: idx_payment_amendment_payment_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_amendment_payment_id ON public.fin_payment_amendment USING btree (payment_id);
--
-- Name: idx_payment_amendment_requested_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_amendment_requested_at ON public.fin_payment_amendment USING btree (requested_at);
--
-- Name: idx_payment_amendment_requested_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_amendment_requested_by ON public.fin_payment_amendment USING btree (requested_by);
--
-- Name: idx_payment_amendment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_amendment_status ON public.fin_payment_amendment USING btree (status);
--
-- Name: fin_payment_amendment fk_amendment_approver; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment
    ADD CONSTRAINT fk_amendment_approver FOREIGN KEY (approved_by) REFERENCES public.sys_user(id);
--
-- Name: fin_payment_amendment fk_amendment_payment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment
    ADD CONSTRAINT fk_amendment_payment FOREIGN KEY (payment_id) REFERENCES public.finance_payment(id);
--
-- Name: fin_payment_amendment fk_amendment_requester; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment
    ADD CONSTRAINT fk_amendment_requester FOREIGN KEY (requested_by) REFERENCES public.sys_user(id);
--
-- Name: finance_commission_detail fk_commission_detail_commission; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_detail
    ADD CONSTRAINT fk_commission_detail_commission FOREIGN KEY (commission_id) REFERENCES public.finance_commission(id);
--
-- Name: finance_commission_detail fk_commission_detail_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_detail
    ADD CONSTRAINT fk_commission_detail_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);
--
-- Name: finance_payment fk_payment_locked_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_payment
    ADD CONSTRAINT fk_payment_locked_by FOREIGN KEY (locked_by) REFERENCES public.sys_user(id);
