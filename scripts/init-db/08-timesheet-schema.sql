-- =====================================================
-- 律师事务所管理系统 - 工时管理模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 工时记录、计时器会话、工时汇总等工时相关表
-- =====================================================

--
-- Name: timer_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.timer_session (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    matter_id bigint NOT NULL,
    work_type character varying(50),
    work_content text,
    billable boolean DEFAULT true,
    start_time timestamp without time zone NOT NULL,
    pause_time timestamp without time zone,
    resume_time timestamp without time zone,
    elapsed_seconds bigint DEFAULT 0,
    status character varying(20) DEFAULT 'RUNNING'::character varying,
    version integer DEFAULT 1,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE timer_session; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.timer_session IS '计时器会话表';
--
-- Name: COLUMN timer_session.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.user_id IS '用户ID';
--
-- Name: COLUMN timer_session.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.matter_id IS '案件ID';
--
-- Name: COLUMN timer_session.work_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.work_type IS '工作类型';
--
-- Name: COLUMN timer_session.work_content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.work_content IS '工作内容';
--
-- Name: COLUMN timer_session.billable; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.billable IS '是否计费';
--
-- Name: COLUMN timer_session.start_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.start_time IS '开始时间';
--
-- Name: COLUMN timer_session.pause_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.pause_time IS '暂停时间';
--
-- Name: COLUMN timer_session.resume_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.resume_time IS '恢复时间';
--
-- Name: COLUMN timer_session.elapsed_seconds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.elapsed_seconds IS '已累计的秒数';
--
-- Name: COLUMN timer_session.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.status IS '状态: RUNNING-运行中, PAUSED-已暂停, STOPPED-已停止';
--
-- Name: timer_session_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.timer_session_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: timer_session_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.timer_session_id_seq OWNED BY public.timer_session.id;
--
-- Name: timesheet; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.timesheet (
    id bigint NOT NULL,
    timesheet_no character varying(50) NOT NULL,
    matter_id bigint NOT NULL,
    user_id bigint NOT NULL,
    work_date date NOT NULL,
    hours numeric(5,2) NOT NULL,
    work_type character varying(50),
    work_content text,
    billable boolean DEFAULT true,
    hourly_rate numeric(10,2),
    amount numeric(15,2),
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    submitted_at timestamp without time zone,
    approved_by bigint,
    approved_at timestamp without time zone,
    approval_comment text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE timesheet; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.timesheet IS '工时记录表';
--
-- Name: COLUMN timesheet.work_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timesheet.work_type IS '工作类型: RESEARCH-法律研究, DRAFTING-文书起草, MEETING-会议, COURT-出庭, NEGOTIATION-谈判, COMMUNICATION-沟通, TRAVEL-差旅, OTHER-其他';
--
-- Name: COLUMN timesheet.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timesheet.status IS '状态: DRAFT-草稿, SUBMITTED-已提交, APPROVED-已批准, REJECTED-已拒绝';
--
-- Name: timesheet_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.timesheet_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: timesheet_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.timesheet_id_seq OWNED BY public.timesheet.id;
--
-- Name: timesheet_summary; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.timesheet_summary (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    matter_id bigint,
    year integer NOT NULL,
    month integer NOT NULL,
    total_hours numeric(10,2) DEFAULT 0,
    billable_hours numeric(10,2) DEFAULT 0,
    non_billable_hours numeric(10,2) DEFAULT 0,
    total_amount numeric(15,2) DEFAULT 0,
    draft_count integer DEFAULT 0,
    submitted_count integer DEFAULT 0,
    approved_count integer DEFAULT 0,
    rejected_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);
--
-- Name: TABLE timesheet_summary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.timesheet_summary IS '工时汇总表';
--
-- Name: timesheet_summary_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.timesheet_summary_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: timesheet_summary_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.timesheet_summary_id_seq OWNED BY public.timesheet_summary.id;
--
-- Name: timer_session id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timer_session ALTER COLUMN id SET DEFAULT nextval('public.timer_session_id_seq'::regclass);
--
-- Name: timesheet id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet ALTER COLUMN id SET DEFAULT nextval('public.timesheet_id_seq'::regclass);
--
-- Name: timesheet_summary id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet_summary ALTER COLUMN id SET DEFAULT nextval('public.timesheet_summary_id_seq'::regclass);
--
-- Name: timer_session timer_session_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timer_session
    ADD CONSTRAINT pk_timer_session PRIMARY KEY (id);
--
-- Name: timesheet timesheet_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet
    ADD CONSTRAINT pk_timesheet PRIMARY KEY (id);
--
-- Name: timesheet_summary timesheet_summary_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet_summary
    ADD CONSTRAINT pk_timesheet_summary PRIMARY KEY (id);
--
-- Name: timesheet_summary timesheet_summary_user_id_matter_id_year_month_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet_summary
    ADD CONSTRAINT uk_timesheet_summary_user_id_matter_id_year_month UNIQUE (user_id, matter_id, year, month);
--
-- Name: timesheet timesheet_timesheet_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet
    ADD CONSTRAINT uk_timesheet_timesheet_no UNIQUE (timesheet_no);
--
-- Name: idx_summary_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_summary_period ON public.timesheet_summary USING btree (year, month);
--
-- Name: idx_summary_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_summary_user ON public.timesheet_summary USING btree (user_id);
--
-- Name: idx_timer_session_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timer_session_matter_id ON public.timer_session USING btree (matter_id);
--
-- Name: idx_timer_session_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timer_session_status ON public.timer_session USING btree (status);
--
-- Name: idx_timer_session_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timer_session_user_id ON public.timer_session USING btree (user_id);
--
-- Name: idx_timesheet_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_date ON public.timesheet USING btree (work_date);
--
-- Name: idx_timesheet_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_matter ON public.timesheet USING btree (matter_id);
--
-- Name: idx_timesheet_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_status ON public.timesheet USING btree (status);
--
-- Name: idx_timesheet_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_user ON public.timesheet USING btree (user_id);
