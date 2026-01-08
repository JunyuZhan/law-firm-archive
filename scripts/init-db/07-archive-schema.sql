-- =====================================================
-- 律师事务所管理系统 - 档案管理模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 档案、存放位置、借阅记录、数据源等档案相关表
-- =====================================================

--
-- Name: archive; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive (
    id bigint NOT NULL,
    archive_no character varying(50) NOT NULL,
    matter_id bigint,
    archive_name character varying(200) NOT NULL,
    archive_type character varying(20) NOT NULL,
    matter_no character varying(50),
    matter_name character varying(200),
    client_name character varying(200),
    main_lawyer_name character varying(50),
    case_close_date date,
    volume_count integer DEFAULT 1,
    page_count integer DEFAULT 0,
    catalog text,
    location_id bigint,
    box_no character varying(50),
    retention_period character varying(20) DEFAULT '10_YEARS'::character varying,
    retention_expire_date date,
    has_electronic boolean DEFAULT false,
    electronic_url character varying(500),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    stored_by bigint,
    stored_at timestamp without time zone,
    destroy_date date,
    destroy_reason text,
    destroy_approver_id bigint,
    remarks text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false,
    migrate_date date,
    migrate_reason text,
    migrate_approver_id bigint,
    migrate_target text,
    files_deleted boolean DEFAULT false,
    archive_snapshot text,
    electronic_package_path text
);
--
-- Name: TABLE archive; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive IS '档案表';
--
-- Name: COLUMN archive.archive_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive.archive_no IS '档案号';
--
-- Name: COLUMN archive.archive_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive.archive_type IS '档案类型：LITIGATION-诉讼, NON_LITIGATION-非诉, CONSULTATION-咨询';
--
-- Name: COLUMN archive.retention_period; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive.retention_period IS '保管期限：PERMANENT-永久, 30_YEARS-30年, 15_YEARS-15年, 10_YEARS-10年, 5_YEARS-5年';
--
-- Name: COLUMN archive.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive.status IS '状态：PENDING-待入库, STORED-已入库, BORROWED-借出, DESTROYED-已销毁';
--
-- Name: archive_borrow; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_borrow (
    id bigint NOT NULL,
    borrow_no character varying(50) NOT NULL,
    archive_id bigint NOT NULL,
    borrower_id bigint NOT NULL,
    borrower_name character varying(50) NOT NULL,
    department character varying(100),
    borrow_reason text,
    borrow_date date NOT NULL,
    expected_return_date date NOT NULL,
    actual_return_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approver_id bigint,
    approved_at timestamp without time zone,
    rejection_reason text,
    return_handler_id bigint,
    return_condition character varying(20),
    return_remarks text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE archive_borrow; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive_borrow IS '档案借阅表';
--
-- Name: COLUMN archive_borrow.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_borrow.status IS '状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, BORROWED-借出中, RETURNED-已归还, OVERDUE-逾期';
--
-- Name: COLUMN archive_borrow.return_condition; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_borrow.return_condition IS '归还状态：GOOD-完好, DAMAGED-损坏, LOST-遗失';
--
-- Name: archive_borrow_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_borrow_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: archive_borrow_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_borrow_id_seq OWNED BY public.archive_borrow.id;
--
-- Name: archive_data_source; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_data_source (
    id bigint NOT NULL,
    source_name character varying(100) NOT NULL,
    source_table character varying(100) NOT NULL,
    source_type character varying(50) NOT NULL,
    dossier_folder character varying(200),
    is_enabled boolean DEFAULT true,
    is_required boolean DEFAULT false,
    sort_order integer DEFAULT 0,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);
--
-- Name: archive_data_source_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_data_source_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: archive_data_source_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_data_source_id_seq OWNED BY public.archive_data_source.id;
--
-- Name: archive_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: archive_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_id_seq OWNED BY public.archive.id;
--
-- Name: archive_location; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_location (
    id bigint NOT NULL,
    location_code character varying(50) NOT NULL,
    location_name character varying(100) NOT NULL,
    room character varying(50),
    cabinet character varying(50),
    shelf character varying(50),
    "position" character varying(50),
    total_capacity integer DEFAULT 0,
    used_capacity integer DEFAULT 0,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying,
    remarks text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE archive_location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive_location IS '档案库位表';
--
-- Name: COLUMN archive_location.location_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_location.location_code IS '库位编码';
--
-- Name: COLUMN archive_location.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_location.status IS '状态：AVAILABLE-可用, FULL-已满, MAINTENANCE-维护中';
--
-- Name: archive_location_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_location_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: archive_location_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_location_id_seq OWNED BY public.archive_location.id;
--
-- Name: archive_operation_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_operation_log (
    id bigint NOT NULL,
    archive_id bigint NOT NULL,
    operation_type character varying(20) NOT NULL,
    operation_description text,
    operator_id bigint NOT NULL,
    operated_at timestamp without time zone NOT NULL,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE archive_operation_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive_operation_log IS '档案操作日志表';
--
-- Name: COLUMN archive_operation_log.operation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_operation_log.operation_type IS '操作类型：STORE-入库, BORROW-借出, RETURN-归还, TRANSFER-转移, DESTROY-销毁';
--
-- Name: archive_operation_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_operation_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: archive_operation_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_operation_log_id_seq OWNED BY public.archive_operation_log.id;
--
-- Name: archive id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive ALTER COLUMN id SET DEFAULT nextval('public.archive_id_seq'::regclass);
--
-- Name: archive_borrow id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_borrow ALTER COLUMN id SET DEFAULT nextval('public.archive_borrow_id_seq'::regclass);
--
-- Name: archive_data_source id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_data_source ALTER COLUMN id SET DEFAULT nextval('public.archive_data_source_id_seq'::regclass);
--
-- Name: archive_location id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_location ALTER COLUMN id SET DEFAULT nextval('public.archive_location_id_seq'::regclass);
--
-- Name: archive_operation_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_operation_log ALTER COLUMN id SET DEFAULT nextval('public.archive_operation_log_id_seq'::regclass);
--
-- Name: archive archive_archive_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive
    ADD CONSTRAINT archive_archive_no_key UNIQUE (archive_no);
--
-- Name: archive_borrow archive_borrow_borrow_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_borrow
    ADD CONSTRAINT archive_borrow_borrow_no_key UNIQUE (borrow_no);
--
-- Name: archive_borrow archive_borrow_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_borrow
    ADD CONSTRAINT archive_borrow_pkey PRIMARY KEY (id);
--
-- Name: archive_data_source archive_data_source_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_data_source
    ADD CONSTRAINT archive_data_source_pkey PRIMARY KEY (id);
--
-- Name: archive_location archive_location_location_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_location
    ADD CONSTRAINT archive_location_location_code_key UNIQUE (location_code);
--
-- Name: archive_location archive_location_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_location
    ADD CONSTRAINT archive_location_pkey PRIMARY KEY (id);
--
-- Name: archive_operation_log archive_operation_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_operation_log
    ADD CONSTRAINT archive_operation_log_pkey PRIMARY KEY (id);
--
-- Name: archive archive_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive
    ADD CONSTRAINT archive_pkey PRIMARY KEY (id);
--
-- Name: idx_archive_archive_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_archive_no ON public.archive USING btree (archive_no);
--
-- Name: idx_archive_borrow_archive_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_archive_id ON public.archive_borrow USING btree (archive_id);
--
-- Name: idx_archive_borrow_borrow_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_borrow_no ON public.archive_borrow USING btree (borrow_no);
--
-- Name: idx_archive_borrow_borrower_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_borrower_id ON public.archive_borrow USING btree (borrower_id);
--
-- Name: idx_archive_borrow_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_date ON public.archive_borrow USING btree (borrow_date, expected_return_date);
--
-- Name: idx_archive_borrow_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_status ON public.archive_borrow USING btree (status);
--
-- Name: idx_archive_expire_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_expire_date ON public.archive USING btree (retention_expire_date);
--
-- Name: idx_archive_location_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_location_code ON public.archive_location USING btree (location_code);
--
-- Name: idx_archive_location_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_location_id ON public.archive USING btree (location_id);
--
-- Name: idx_archive_location_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_location_status ON public.archive_location USING btree (status);
--
-- Name: idx_archive_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_matter_id ON public.archive USING btree (matter_id);
--
-- Name: idx_archive_operation_log_archive_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_operation_log_archive_id ON public.archive_operation_log USING btree (archive_id);
--
-- Name: idx_archive_operation_log_operator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_operation_log_operator ON public.archive_operation_log USING btree (operator_id);
--
-- Name: idx_archive_operation_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_operation_log_time ON public.archive_operation_log USING btree (operated_at);
--
-- Name: idx_archive_operation_log_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_operation_log_type ON public.archive_operation_log USING btree (operation_type);
--
-- Name: idx_archive_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_status ON public.archive USING btree (status);
--
-- Name: idx_archive_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_type ON public.archive USING btree (archive_type);
