-- =====================================================
-- 律师事务所管理系统 - 合同模板模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 合同模板、参与人模板、付款计划模板等
-- =====================================================

--
-- Name: contract_participant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contract_participant (
    id bigint NOT NULL,
    contract_id bigint NOT NULL,
    user_id bigint NOT NULL,
    role character varying(20) NOT NULL,
    commission_rate numeric(5,2),
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE contract_participant; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.contract_participant IS '合同参与人表';
--
-- Name: COLUMN contract_participant.contract_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_participant.contract_id IS '合同ID';
--
-- Name: COLUMN contract_participant.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_participant.user_id IS '用户ID';
--
-- Name: COLUMN contract_participant.role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_participant.role IS '角色：LEAD-承办律师, CO_COUNSEL-协办律师, ORIGINATOR-案源人, PARALEGAL-律师助理';
--
-- Name: COLUMN contract_participant.commission_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_participant.commission_rate IS '提成比例（百分比）';
--
-- Name: contract_participant_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contract_participant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: contract_participant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contract_participant_id_seq OWNED BY public.contract_participant.id;
--
-- Name: contract_payment_schedule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contract_payment_schedule (
    id bigint NOT NULL,
    contract_id bigint NOT NULL,
    phase_name character varying(100) NOT NULL,
    amount numeric(15,2) NOT NULL,
    percentage numeric(5,2),
    planned_date date,
    actual_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE contract_payment_schedule; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.contract_payment_schedule IS '合同付款计划表';
--
-- Name: COLUMN contract_payment_schedule.contract_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.contract_id IS '合同ID';
--
-- Name: COLUMN contract_payment_schedule.phase_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.phase_name IS '阶段名称（如：签约款、一审结束、执行到位）';
--
-- Name: COLUMN contract_payment_schedule.amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.amount IS '付款金额';
--
-- Name: COLUMN contract_payment_schedule.percentage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.percentage IS '比例（风险代理时使用）';
--
-- Name: COLUMN contract_payment_schedule.planned_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.planned_date IS '计划收款日期';
--
-- Name: COLUMN contract_payment_schedule.actual_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.actual_date IS '实际收款日期';
--
-- Name: COLUMN contract_payment_schedule.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.status IS '状态：PENDING-待收, PARTIAL-部分收款, PAID-已收清, CANCELLED-已取消';
--
-- Name: contract_payment_schedule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contract_payment_schedule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: contract_payment_schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contract_payment_schedule_id_seq OWNED BY public.contract_payment_schedule.id;
--
-- Name: contract_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contract_template (
    id bigint NOT NULL,
    template_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    template_type character varying(50) NOT NULL,
    fee_type character varying(20),
    content text,
    clauses text,
    description character varying(500),
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE contract_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.contract_template IS '合同模板表';
--
-- Name: COLUMN contract_template.template_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.template_no IS '模板编号';
--
-- Name: COLUMN contract_template.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.name IS '模板名称';
--
-- Name: COLUMN contract_template.template_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.template_type IS '模板类型（合同类型）：CIVIL_PROXY-民事代理, ADMINISTRATIVE_PROXY-行政代理, CRIMINAL_DEFENSE-刑事辩护, LEGAL_COUNSEL-法律顾问, NON_LITIGATION-非诉案件, CUSTOM-自定义模板';
--
-- Name: COLUMN contract_template.fee_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.fee_type IS '默认收费方式';
--
-- Name: COLUMN contract_template.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.content IS '模板内容（支持变量替换）';
--
-- Name: COLUMN contract_template.clauses; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.clauses IS '标准条款（JSON格式）';
--
-- Name: COLUMN contract_template.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.description IS '模板说明';
--
-- Name: COLUMN contract_template.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.status IS '状态：ACTIVE-启用, INACTIVE-停用';
--
-- Name: contract_template_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contract_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: contract_template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contract_template_id_seq OWNED BY public.contract_template.id;
--
-- Name: contract_participant id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant ALTER COLUMN id SET DEFAULT nextval('public.contract_participant_id_seq'::regclass);
--
-- Name: contract_payment_schedule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_payment_schedule ALTER COLUMN id SET DEFAULT nextval('public.contract_payment_schedule_id_seq'::regclass);
--
-- Name: contract_template id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_template ALTER COLUMN id SET DEFAULT nextval('public.contract_template_id_seq'::regclass);
--
-- Name: contract_participant contract_participant_contract_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant
    ADD CONSTRAINT contract_participant_contract_id_user_id_key UNIQUE (contract_id, user_id);
--
-- Name: contract_participant contract_participant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant
    ADD CONSTRAINT contract_participant_pkey PRIMARY KEY (id);
--
-- Name: contract_payment_schedule contract_payment_schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_payment_schedule
    ADD CONSTRAINT contract_payment_schedule_pkey PRIMARY KEY (id);
--
-- Name: contract_template contract_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_template
    ADD CONSTRAINT contract_template_pkey PRIMARY KEY (id);
--
-- Name: contract_template contract_template_template_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_template
    ADD CONSTRAINT contract_template_template_no_key UNIQUE (template_no);
--
-- Name: idx_contract_participant_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_participant_contract ON public.contract_participant USING btree (contract_id);
--
-- Name: idx_contract_participant_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_participant_role ON public.contract_participant USING btree (role);
--
-- Name: idx_contract_participant_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_participant_user ON public.contract_participant USING btree (user_id);
--
-- Name: idx_contract_template_contract_type; Type: INDEX; Schema: public; Owner: -
--

--
-- Name: idx_contract_template_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_template_status ON public.contract_template USING btree (status);
--
-- Name: idx_contract_template_template_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_template_template_type ON public.contract_template USING btree (template_type);
--
-- Name: idx_payment_schedule_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_schedule_contract ON public.contract_payment_schedule USING btree (contract_id);
--
-- Name: idx_payment_schedule_planned_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_schedule_planned_date ON public.contract_payment_schedule USING btree (planned_date);
--
-- Name: idx_payment_schedule_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_schedule_status ON public.contract_payment_schedule USING btree (status);
--
-- Name: contract_participant fk_participant_contract; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant
    ADD CONSTRAINT fk_participant_contract FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id);
--
-- Name: contract_participant fk_participant_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant
    ADD CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);
--
-- Name: contract_payment_schedule fk_payment_schedule_contract; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_payment_schedule
    ADD CONSTRAINT fk_payment_schedule_contract FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id);
