-- =====================================================
-- 律师事务所管理系统 - 证据管理模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 证据、证据清单、质证记录等证据相关表
-- =====================================================

--
-- Name: evidence; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evidence (
    id bigint NOT NULL,
    evidence_no character varying(50) NOT NULL,
    matter_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    evidence_type character varying(50) NOT NULL,
    source character varying(200),
    group_name character varying(100),
    sort_order integer DEFAULT 0,
    prove_purpose text,
    description text,
    is_original boolean DEFAULT false,
    original_count integer DEFAULT 0,
    copy_count integer DEFAULT 0,
    page_start integer,
    page_end integer,
    file_url character varying(1000),
    file_name character varying(500),
    file_size bigint,
    cross_exam_status character varying(20) DEFAULT 'PENDING'::character varying,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    file_type character varying(50),
    thumbnail_url character varying(1000),
    document_id bigint,
    bucket_name character varying(50) DEFAULT 'law-firm',
    storage_path character varying(500),
    physical_name character varying(1000),
    file_hash character varying(64)
);
--
-- Name: TABLE evidence; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.evidence IS '证据表';
--
-- Name: COLUMN evidence.evidence_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.evidence_type IS '证据类型: DOCUMENTARY-书证, PHYSICAL-物证, AUDIO_VISUAL-视听资料, ELECTRONIC-电子数据, WITNESS-证人证言, EXPERT-鉴定意见, INSPECTION-勘验笔录';
--
-- Name: COLUMN evidence.cross_exam_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.cross_exam_status IS '质证状态: PENDING-待质证, IN_PROGRESS-质证中, COMPLETED-已质证';
--
-- Name: COLUMN evidence.file_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.file_type IS '文件类型分类: image, pdf, word, excel, ppt, audio, video, other';
--
-- Name: COLUMN evidence.thumbnail_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.thumbnail_url IS '缩略图URL（仅图片文件）';
--
-- Name: COLUMN evidence.document_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.document_id IS '关联卷宗文件ID，引用 doc_document.id';
--
-- Name: COLUMN evidence.bucket_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.bucket_name IS 'MinIO桶名称，默认law-firm';
--
-- Name: COLUMN evidence.storage_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.storage_path IS '存储路径：evidence/M_101/2026-01/证据材料/';
--
-- Name: COLUMN evidence.physical_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.physical_name IS '物理文件名：20260127_uuid_借条.jpg（支持超长文件名，最大1000字符）';
--
-- Name: COLUMN evidence.file_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.file_hash IS '文件Hash值（SHA-256），用于去重和校验（测试阶段仅记录，不强制去重）';
--
-- Name: evidence_cross_exam; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evidence_cross_exam (
    id bigint NOT NULL,
    evidence_id bigint NOT NULL,
    exam_party character varying(50) NOT NULL,
    authenticity_opinion character varying(20),
    authenticity_reason text,
    legality_opinion character varying(20),
    legality_reason text,
    relevance_opinion character varying(20),
    relevance_reason text,
    overall_opinion text,
    court_opinion text,
    court_accepted boolean,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);
--
-- Name: TABLE evidence_cross_exam; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.evidence_cross_exam IS '质证记录表';
--
-- Name: COLUMN evidence_cross_exam.exam_party; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence_cross_exam.exam_party IS '质证方: OUR_SIDE-我方, OPPOSITE-对方, COURT-法院';
--
-- Name: COLUMN evidence_cross_exam.authenticity_opinion; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence_cross_exam.authenticity_opinion IS '真实性意见: ACCEPT-认可, PARTIAL-部分认可, REJECT-不认可';
--
-- Name: evidence_cross_exam_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.evidence_cross_exam_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: evidence_cross_exam_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.evidence_cross_exam_id_seq OWNED BY public.evidence_cross_exam.id;
--
-- Name: evidence_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.evidence_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: evidence_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.evidence_id_seq OWNED BY public.evidence.id;
--
-- Name: evidence_list; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evidence_list (
    id bigint NOT NULL,
    list_no character varying(50) NOT NULL,
    matter_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    list_type character varying(50),
    evidence_ids text,
    file_url character varying(1000),
    file_name character varying(500),
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    version integer DEFAULT 1,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE evidence_list; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.evidence_list IS '证据清单表';
--
-- Name: COLUMN evidence_list.list_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence_list.list_type IS '清单类型: SUBMISSION-提交清单, EXCHANGE-交换清单, COURT-庭审清单';
--
-- Name: evidence_list_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.evidence_list_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: evidence_list_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.evidence_list_id_seq OWNED BY public.evidence_list.id;
--
-- Name: evidence id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence ALTER COLUMN id SET DEFAULT nextval('public.evidence_id_seq'::regclass);
--
-- Name: evidence_cross_exam id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_cross_exam ALTER COLUMN id SET DEFAULT nextval('public.evidence_cross_exam_id_seq'::regclass);
--
-- Name: evidence_list id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_list ALTER COLUMN id SET DEFAULT nextval('public.evidence_list_id_seq'::regclass);
--
-- Name: evidence_cross_exam evidence_cross_exam_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_cross_exam
    ADD CONSTRAINT pk_evidence_cross_exam PRIMARY KEY (id);
--
-- Name: evidence evidence_evidence_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT uk_evidence_evidence_no UNIQUE (evidence_no);
--
-- Name: evidence_list evidence_list_list_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_list
    ADD CONSTRAINT uk_evidence_list_list_no UNIQUE (list_no);
--
-- Name: evidence_list evidence_list_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_list
    ADD CONSTRAINT pk_evidence_list PRIMARY KEY (id);
--
-- Name: evidence evidence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT pk_evidence PRIMARY KEY (id);
--
-- Name: idx_cross_exam_evidence; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cross_exam_evidence ON public.evidence_cross_exam USING btree (evidence_id);
--
-- Name: idx_evidence_document; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_document ON public.evidence USING btree (document_id);
--
-- Name: idx_evidence_list_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_list_matter ON public.evidence_list USING btree (matter_id);
--
-- Name: idx_evidence_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_matter ON public.evidence USING btree (matter_id);
--
-- Name: idx_evidence_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_status ON public.evidence USING btree (cross_exam_status);
--
-- Name: idx_evidence_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_type ON public.evidence USING btree (evidence_type);
--
-- Name: idx_evidence_file_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_file_hash ON public.evidence USING btree (file_hash) WHERE (file_hash IS NOT NULL);
--
-- Name: idx_evidence_storage_path; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_storage_path ON public.evidence USING btree (storage_path) WHERE (storage_path IS NOT NULL);
