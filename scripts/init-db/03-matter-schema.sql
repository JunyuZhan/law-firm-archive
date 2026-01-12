-- =====================================================
-- 律师事务所管理系统 - 项目管理模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 项目/案件、参与人、期限、客户关联等项目相关表
-- =====================================================

--
-- Name: matter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter (
    id bigint NOT NULL,
    matter_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    matter_type character varying(20) NOT NULL,
    case_type character varying(30),
    litigation_stage character varying(30),
    cause_of_action character varying(50),
    business_type character varying(50),
    client_id bigint NOT NULL,
    opposing_party character varying(200),
    opposing_lawyer_name character varying(100),
    opposing_lawyer_license_no character varying(50),
    opposing_lawyer_firm character varying(200),
    opposing_lawyer_phone character varying(20),
    opposing_lawyer_email character varying(100),
    description text,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    originator_id bigint,
    lead_lawyer_id bigint,
    department_id bigint,
    fee_type character varying(20),
    estimated_fee numeric(15,2),
    actual_fee numeric(15,2),
    filing_date date,
    expected_end_date date,
    actual_end_date date,
    claim_amount numeric(15,2),
    outcome text,
    contract_id bigint,
    remark text,
    conflict_status character varying(20),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE matter; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter IS '案件/项目表';
--
-- Name: COLUMN matter.matter_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.matter_no IS '案件编号';
--
-- Name: COLUMN matter.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.name IS '案件名称';
--
-- Name: COLUMN matter.matter_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.matter_type IS '项目大类：LITIGATION-诉讼案件, NON_LITIGATION-非诉项目';
--
-- Name: COLUMN matter.case_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.case_type IS '案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务';
--
-- Name: COLUMN matter.litigation_stage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.litigation_stage IS '代理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行, ARBITRATION-仲裁, CONSULTATION-咨询, ALL_STAGES-全阶段';
--
-- Name: COLUMN matter.cause_of_action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.cause_of_action IS '案由代码';
--
-- Name: COLUMN matter.opposing_lawyer_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_name IS '对方律师姓名';
--
-- Name: COLUMN matter.opposing_lawyer_license_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_license_no IS '对方律师执业证号';
--
-- Name: COLUMN matter.opposing_lawyer_firm; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_firm IS '对方律师所在律所';
--
-- Name: COLUMN matter.opposing_lawyer_phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_phone IS '对方律师联系电话';
--
-- Name: COLUMN matter.opposing_lawyer_email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_email IS '对方律师邮箱';
--
-- Name: COLUMN matter.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.status IS '状态：DRAFT-草稿, PENDING-待审批, ACTIVE-进行中, SUSPENDED-暂停, CLOSED-结案, ARCHIVED-归档';
--
-- Name: COLUMN matter.fee_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.fee_type IS '收费方式：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理, MIXED-混合收费';
--
-- Name: COLUMN matter.conflict_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.conflict_status IS '利冲检查状态：PENDING-待检查, PASSED-已通过, FAILED-未通过';
--
-- Name: matter_client; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter_client (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    client_id bigint NOT NULL,
    client_role character varying(50) DEFAULT 'PLAINTIFF'::character varying,
    is_primary boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE matter_client; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter_client IS '项目-客户关联表（支持多客户）';
--
-- Name: COLUMN matter_client.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_client.matter_id IS '项目ID';
--
-- Name: COLUMN matter_client.client_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_client.client_id IS '客户ID';
--
-- Name: COLUMN matter_client.client_role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_client.client_role IS '客户角色：PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人';
--
-- Name: COLUMN matter_client.is_primary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_client.is_primary IS '是否主要客户（用于显示和结算）';
--
-- Name: matter_client_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_client_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: matter_client_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_client_id_seq OWNED BY public.matter_client.id;
--
-- Name: matter_deadline; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter_deadline (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    deadline_type character varying(50) NOT NULL,
    deadline_name character varying(200) NOT NULL,
    base_date date NOT NULL,
    deadline_date date NOT NULL,
    reminder_days integer DEFAULT 7,
    reminder_sent boolean DEFAULT false,
    reminder_sent_at timestamp without time zone,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    completed_at timestamp without time zone,
    completed_by bigint,
    description character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE matter_deadline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter_deadline IS '期限提醒表';
--
-- Name: COLUMN matter_deadline.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.matter_id IS '项目ID';
--
-- Name: COLUMN matter_deadline.deadline_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.deadline_type IS '期限类型';
--
-- Name: COLUMN matter_deadline.deadline_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.deadline_name IS '期限名称';
--
-- Name: COLUMN matter_deadline.base_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.base_date IS '基准日期';
--
-- Name: COLUMN matter_deadline.deadline_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.deadline_date IS '期限日期';
--
-- Name: COLUMN matter_deadline.reminder_days; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.reminder_days IS '提前提醒天数';
--
-- Name: COLUMN matter_deadline.reminder_sent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.reminder_sent IS '是否已发送提醒';
--
-- Name: COLUMN matter_deadline.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.status IS '状态';
--
-- Name: matter_deadline_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_deadline_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: matter_deadline_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_deadline_id_seq OWNED BY public.matter_deadline.id;
--
-- Name: matter_dossier_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter_dossier_item (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    parent_id bigint DEFAULT 0,
    name character varying(200) NOT NULL,
    item_type character varying(50) DEFAULT 'FOLDER'::character varying,
    file_category character varying(50),
    sort_order integer DEFAULT 0,
    document_count integer DEFAULT 0,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    updated_by bigint
);
--
-- Name: TABLE matter_dossier_item; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter_dossier_item IS '项目卷宗目录表';
--
-- Name: matter_dossier_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_dossier_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: matter_dossier_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_dossier_item_id_seq OWNED BY public.matter_dossier_item.id;
--
-- Name: matter_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: matter_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_id_seq OWNED BY public.matter.id;
--
-- Name: matter_participant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter_participant (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    user_id bigint NOT NULL,
    role character varying(20) NOT NULL,
    commission_rate numeric(5,2),
    is_originator boolean DEFAULT false,
    join_date date,
    exit_date date,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE matter_participant; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter_participant IS '案件参与人表';
--
-- Name: COLUMN matter_participant.role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_participant.role IS '角色：LEAD-主办律师, CO_COUNSEL-协办律师, PARALEGAL-律师助理, TRAINEE-实习律师';
--
-- Name: COLUMN matter_participant.commission_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_participant.commission_rate IS '提成比例（百分比）';
--
-- Name: COLUMN matter_participant.is_originator; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_participant.is_originator IS '是否案源人';
--
-- Name: matter_participant_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_participant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: matter_participant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_participant_id_seq OWNED BY public.matter_participant.id;
--
-- Name: matter id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter ALTER COLUMN id SET DEFAULT nextval('public.matter_id_seq'::regclass);
--
-- Name: matter_client id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_client ALTER COLUMN id SET DEFAULT nextval('public.matter_client_id_seq'::regclass);
--
-- Name: matter_deadline id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_deadline ALTER COLUMN id SET DEFAULT nextval('public.matter_deadline_id_seq'::regclass);
--
-- Name: matter_dossier_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_dossier_item ALTER COLUMN id SET DEFAULT nextval('public.matter_dossier_item_id_seq'::regclass);
--
-- Name: matter_participant id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_participant ALTER COLUMN id SET DEFAULT nextval('public.matter_participant_id_seq'::regclass);
--
-- Name: matter_client matter_client_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_client
    ADD CONSTRAINT matter_client_pkey PRIMARY KEY (id);
--
-- Name: matter_deadline matter_deadline_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_deadline
    ADD CONSTRAINT matter_deadline_pkey PRIMARY KEY (id);
--
-- Name: matter_dossier_item matter_dossier_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_dossier_item
    ADD CONSTRAINT matter_dossier_item_pkey PRIMARY KEY (id);
--
-- Name: matter matter_matter_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter
    ADD CONSTRAINT matter_matter_no_key UNIQUE (matter_no);
--
-- Name: matter_participant matter_participant_matter_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_participant
    ADD CONSTRAINT matter_participant_matter_id_user_id_key UNIQUE (matter_id, user_id);
--
-- Name: matter_participant matter_participant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_participant
    ADD CONSTRAINT matter_participant_pkey PRIMARY KEY (id);
--
-- Name: matter matter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter
    ADD CONSTRAINT matter_pkey PRIMARY KEY (id);
--
-- Name: idx_matter_case_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_case_type ON public.matter USING btree (case_type);
--
-- Name: idx_matter_client_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_client_client_id ON public.matter_client USING btree (client_id);
--
-- Name: idx_matter_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_client_id ON public.matter USING btree (client_id);
--
-- Name: idx_matter_client_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_client_matter_id ON public.matter_client USING btree (matter_id);
--
-- Name: idx_matter_client_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_matter_client_unique ON public.matter_client USING btree (matter_id, client_id) WHERE (deleted = false);
--
-- Name: idx_matter_deadline_deadline_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_deadline_deadline_date ON public.matter_deadline USING btree (deadline_date);
--
-- Name: idx_matter_deadline_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_deadline_matter_id ON public.matter_deadline USING btree (matter_id);
--
-- Name: idx_matter_deadline_reminder_sent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_deadline_reminder_sent ON public.matter_deadline USING btree (reminder_sent);
--
-- Name: idx_matter_deadline_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_deadline_status ON public.matter_deadline USING btree (status);
--
-- Name: idx_matter_dossier_item_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_dossier_item_matter ON public.matter_dossier_item USING btree (matter_id);
--
-- Name: idx_matter_lead_lawyer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_lead_lawyer ON public.matter USING btree (lead_lawyer_id);
--
-- Name: idx_matter_matter_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_matter_no ON public.matter USING btree (matter_no);
--
-- Name: idx_matter_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_name ON public.matter USING btree (name);
--
-- Name: idx_matter_opposing_lawyer_firm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_opposing_lawyer_firm ON public.matter USING btree (opposing_lawyer_firm);
--
-- Name: idx_matter_opposing_lawyer_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_opposing_lawyer_name ON public.matter USING btree (opposing_lawyer_name);
--
-- Name: idx_matter_participant_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_participant_matter_id ON public.matter_participant USING btree (matter_id);
--
-- Name: idx_matter_participant_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_participant_status ON public.matter_participant USING btree (status);
--
-- Name: idx_matter_participant_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_participant_user_id ON public.matter_participant USING btree (user_id);
--
-- Name: idx_matter_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_status ON public.matter USING btree (status);
--
-- Name: idx_matter_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_type ON public.matter USING btree (matter_type);
--
-- Name: matter_client fk_matter_client_client; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_client
    ADD CONSTRAINT fk_matter_client_client FOREIGN KEY (client_id) REFERENCES public.crm_client(id);
--
-- Name: matter_client fk_matter_client_matter; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_client
    ADD CONSTRAINT fk_matter_client_matter FOREIGN KEY (matter_id) REFERENCES public.matter(id);
--
-- Name: matter_dossier_item fk_matter_dossier_matter; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_dossier_item
    ADD CONSTRAINT fk_matter_dossier_matter FOREIGN KEY (matter_id) REFERENCES public.matter(id);
