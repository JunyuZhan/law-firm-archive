-- =====================================================
-- 律师事务所管理系统 - 文档管理模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 文档、版本、分类、模板、卷宗目录等文档相关表
-- =====================================================

--
-- Name: doc_access_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.doc_access_log (
    id bigint NOT NULL,
    document_id bigint NOT NULL,
    user_id bigint NOT NULL,
    action_type character varying(20) NOT NULL,
    ip_address character varying(50),
    user_agent character varying(2000),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);
--
-- Name: TABLE doc_access_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.doc_access_log IS '文档访问日志表';
--
-- Name: COLUMN doc_access_log.action_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_access_log.action_type IS '操作类型: VIEW-查看, DOWNLOAD-下载, PRINT-打印, EDIT-编辑';
--
-- Name: doc_access_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.doc_access_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: doc_access_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.doc_access_log_id_seq OWNED BY public.doc_access_log.id;
--
-- Name: doc_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.doc_category (
    id bigint NOT NULL,
    name character varying(200) NOT NULL,
    parent_id bigint DEFAULT 0,
    sort_order integer DEFAULT 0,
    description character varying(500),
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE doc_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.doc_category IS '文档分类表';
--
-- Name: COLUMN doc_category.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_category.name IS '分类名称';
--
-- Name: COLUMN doc_category.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_category.parent_id IS '父分类ID，0表示顶级';
--
-- Name: doc_category_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.doc_category_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: doc_category_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.doc_category_id_seq OWNED BY public.doc_category.id;
--
-- Name: doc_document; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.doc_document (
    id bigint NOT NULL,
    doc_no character varying(50) NOT NULL,
    title character varying(500) NOT NULL,
    category_id bigint,
    matter_id bigint,
    file_name character varying(500) NOT NULL,
    file_path character varying(1000) NOT NULL,
    file_size bigint,
    file_type character varying(50),
    mime_type character varying(100),
    version integer DEFAULT 1,
    is_latest boolean DEFAULT true,
    parent_doc_id bigint,
    security_level character varying(20) DEFAULT 'INTERNAL'::character varying,
    stage character varying(50),
    tags jsonb,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    updated_by bigint,
    file_category character varying(50) DEFAULT 'OTHER'::character varying,
    folder_path character varying(500),
    dossier_item_id bigint,
    ai_generated boolean DEFAULT false,
    display_order integer DEFAULT 0,
    thumbnail_url character varying(500),
    bucket_name character varying(50) DEFAULT 'law-firm',
    storage_path character varying(500),
    physical_name character varying(1000),
    file_hash character varying(64)
);
--
-- Name: TABLE doc_document; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.doc_document IS '文档表';
--
-- Name: COLUMN doc_document.doc_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.doc_no IS '文档编号';
--
-- Name: COLUMN doc_document.security_level; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.security_level IS '安全级别: PUBLIC-公开, INTERNAL-内部, CONFIDENTIAL-机密, TOP_SECRET-绝密';
--
-- Name: COLUMN doc_document.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.status IS '状态: ACTIVE-正常, ARCHIVED-已归档, DELETED-已删除';
--
-- Name: COLUMN doc_document.file_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.file_category IS '文件分类: EVIDENCE-证据材料, PLEADING-诉讼文书, CONTRACT-合同文件, CORRESPONDENCE-往来函件, MEETING-会见记录, JUDGMENT-裁判文书, WORKLOG-工作日志, OTHER-其他';
--
-- Name: COLUMN doc_document.folder_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.folder_path IS '卷宗目录路径，如: /证据材料/';
--
-- Name: COLUMN doc_document.dossier_item_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.dossier_item_id IS '关联卷宗目录项ID';
--
-- Name: COLUMN doc_document.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.display_order IS '显示排序顺序（同一目录内的排序，数值越小越靠前）';
--
-- Name: COLUMN doc_document.thumbnail_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.thumbnail_url IS '缩略图URL（图片和PDF文件）';
--
-- Name: COLUMN doc_document.bucket_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.bucket_name IS 'MinIO桶名称，默认law-firm';
--
-- Name: COLUMN doc_document.storage_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.storage_path IS '存储路径：matters/M_101/2026-01/诉讼文书/';
--
-- Name: COLUMN doc_document.physical_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.physical_name IS '物理文件名：20260127_uuid_合同.pdf（支持超长文件名，最大1000字符）';
--
-- Name: COLUMN doc_document.file_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_document.file_hash IS '文件Hash值（SHA-256），用于去重和校验（测试阶段仅记录，不强制去重）';
--
-- Name: doc_document_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.doc_document_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: doc_document_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.doc_document_id_seq OWNED BY public.doc_document.id;
--
-- Name: doc_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.doc_template (
    id bigint NOT NULL,
    template_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    category_id bigint,
    template_type character varying(50),
    case_type character varying(30) DEFAULT 'ALL',
    file_name character varying(500) NOT NULL,
    file_path character varying(1000) NOT NULL,
    file_size bigint,
    variables text,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    use_count integer DEFAULT 0,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    updated_by bigint,
    content text
);
--
-- Name: TABLE doc_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.doc_template IS '文档模板表';
--
-- Name: COLUMN doc_template.template_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_template.template_type IS '模板类型: CONTRACT-合同, LEGAL_OPINION-法律意见书, POWER_OF_ATTORNEY-授权委托书, COMPLAINT-起诉状, DEFENSE-答辩状, OTHER-其他';
--
-- Name: COLUMN doc_template.case_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_template.case_type IS '案件类型: CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, ALL-通用';
--
-- Name: COLUMN doc_template.variables; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_template.variables IS '模板变量定义，逗号分隔的变量名，如: 客户名称,项目名称,承办律师';
--
-- Name: doc_template_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.doc_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: doc_template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.doc_template_id_seq OWNED BY public.doc_template.id;
--
-- Name: doc_version; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.doc_version (
    id bigint NOT NULL,
    document_id bigint NOT NULL,
    version integer NOT NULL,
    file_name character varying(500) NOT NULL,
    file_path character varying(1000) NOT NULL,
    file_size bigint,
    change_note character varying(1000),
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);
--
-- Name: TABLE doc_version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.doc_version IS '文档版本历史表';
--
-- Name: doc_version_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.doc_version_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: doc_version_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.doc_version_id_seq OWNED BY public.doc_version.id;
--
-- Name: dossier_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dossier_template (
    id bigint NOT NULL,
    name character varying(200) NOT NULL,
    case_type character varying(50) NOT NULL,
    description character varying(500),
    is_default boolean DEFAULT false,
    version integer DEFAULT 1,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE dossier_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.dossier_template IS '卷宗目录模板表';
--
-- Name: COLUMN dossier_template.case_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.dossier_template.case_type IS '案件类型: CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务';
--
-- Name: dossier_template_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dossier_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: dossier_template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dossier_template_id_seq OWNED BY public.dossier_template.id;
--
-- Name: dossier_template_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dossier_template_item (
    id bigint NOT NULL,
    template_id bigint NOT NULL,
    parent_id bigint DEFAULT 0,
    name character varying(200) NOT NULL,
    item_type character varying(50) DEFAULT 'FOLDER'::character varying,
    file_category character varying(50),
    sort_order integer DEFAULT 0,
    required boolean DEFAULT false,
    description character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    created_by bigint,
    updated_at timestamp without time zone DEFAULT now(),
    deleted boolean DEFAULT false
);
--
-- Name: TABLE dossier_template_item; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.dossier_template_item IS '卷宗目录项表';
--
-- Name: COLUMN dossier_template_item.item_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.dossier_template_item.item_type IS '类型: FOLDER-目录, FILE-文件占位';
--
-- Name: dossier_template_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dossier_template_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: dossier_template_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dossier_template_item_id_seq OWNED BY public.dossier_template_item.id;
--
-- Name: doc_access_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_access_log ALTER COLUMN id SET DEFAULT nextval('public.doc_access_log_id_seq'::regclass);
--
-- Name: doc_category id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_category ALTER COLUMN id SET DEFAULT nextval('public.doc_category_id_seq'::regclass);
--
-- Name: doc_document id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_document ALTER COLUMN id SET DEFAULT nextval('public.doc_document_id_seq'::regclass);
--
-- Name: doc_template id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_template ALTER COLUMN id SET DEFAULT nextval('public.doc_template_id_seq'::regclass);
--
-- Name: doc_version id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_version ALTER COLUMN id SET DEFAULT nextval('public.doc_version_id_seq'::regclass);
--
-- Name: dossier_template id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossier_template ALTER COLUMN id SET DEFAULT nextval('public.dossier_template_id_seq'::regclass);
--
-- Name: dossier_template_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossier_template_item ALTER COLUMN id SET DEFAULT nextval('public.dossier_template_item_id_seq'::regclass);
--
-- Name: doc_access_log doc_access_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_access_log
    ADD CONSTRAINT pk_doc_access_log PRIMARY KEY (id);
--
-- Name: doc_category doc_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_category
    ADD CONSTRAINT pk_doc_category PRIMARY KEY (id);
--
-- Name: doc_document doc_document_doc_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_document
    ADD CONSTRAINT uk_doc_document_doc_no UNIQUE (doc_no);
--
-- Name: doc_document doc_document_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_document
    ADD CONSTRAINT pk_doc_document PRIMARY KEY (id);
--
-- Name: doc_template doc_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_template
    ADD CONSTRAINT pk_doc_template PRIMARY KEY (id);
--
-- Name: doc_template doc_template_template_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_template
    ADD CONSTRAINT uk_doc_template_template_no UNIQUE (template_no);
--
-- Name: doc_version doc_version_document_id_version_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_version
    ADD CONSTRAINT uk_doc_version_document_id_version UNIQUE (document_id, version);
--
-- Name: doc_version doc_version_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_version
    ADD CONSTRAINT pk_doc_version PRIMARY KEY (id);
--
-- Name: dossier_template_item dossier_template_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossier_template_item
    ADD CONSTRAINT pk_dossier_template_item PRIMARY KEY (id);
--
-- Name: dossier_template dossier_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossier_template
    ADD CONSTRAINT pk_dossier_template PRIMARY KEY (id);
--
-- Name: idx_doc_access_document; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doc_access_document ON public.doc_access_log USING btree (document_id);
--
-- Name: idx_doc_access_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doc_access_user ON public.doc_access_log USING btree (user_id);
--
-- Name: idx_doc_category_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doc_category_parent ON public.doc_category USING btree (parent_id);
--
-- Name: idx_doc_version_document; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doc_version_document ON public.doc_version USING btree (document_id);
--
-- Name: idx_document_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_category ON public.doc_document USING btree (category_id);
--
-- Name: idx_document_dossier_item; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_dossier_item ON public.doc_document USING btree (dossier_item_id);
--
-- Name: idx_document_display_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doc_document_display_order ON public.doc_document USING btree (dossier_item_id, display_order);
--
-- Name: idx_document_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_matter ON public.doc_document USING btree (matter_id);
--
-- Name: idx_document_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_status ON public.doc_document USING btree (status);
--
-- Name: idx_dossier_template_case_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dossier_template_case_type ON public.dossier_template USING btree (case_type);
--
-- Name: idx_dossier_template_item_template; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dossier_template_item_template ON public.dossier_template_item USING btree (template_id);
--
-- Name: idx_template_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_template_category ON public.doc_template USING btree (category_id);
--
-- Name: idx_doc_document_file_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doc_document_file_hash ON public.doc_document USING btree (file_hash) WHERE (file_hash IS NOT NULL);
--
-- Name: idx_doc_document_storage_path; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doc_document_storage_path ON public.doc_document USING btree (storage_path) WHERE (storage_path IS NOT NULL);
--
-- Name: dossier_template_item fk_dossier_item_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossier_template_item
    ADD CONSTRAINT fk_dossier_item_template FOREIGN KEY (template_id) REFERENCES public.dossier_template(id);
