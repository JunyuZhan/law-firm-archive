-- =====================================================
-- 律师事务所管理系统 - 知识库模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 文章、案例、法规、收藏、学习笔记等知识相关表
-- =====================================================

--
-- Name: article_collection; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.article_collection (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    article_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE article_collection; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.article_collection IS '文章收藏表';
--
-- Name: article_collection_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.article_collection_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: article_collection_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.article_collection_id_seq OWNED BY public.article_collection.id;
--
-- Name: article_comment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.article_comment (
    id bigint NOT NULL,
    article_id bigint NOT NULL,
    user_id bigint NOT NULL,
    parent_id bigint,
    content text NOT NULL,
    like_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE article_comment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.article_comment IS '文章评论表';
--
-- Name: article_comment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.article_comment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: article_comment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.article_comment_id_seq OWNED BY public.article_comment.id;
--
-- Name: case_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.case_category (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    parent_id bigint DEFAULT 0,
    level integer DEFAULT 1,
    sort_order integer DEFAULT 0,
    description character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE case_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.case_category IS '案例分类表';
--
-- Name: case_category_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.case_category_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: case_category_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.case_category_id_seq OWNED BY public.case_category.id;
--
-- Name: case_library; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.case_library (
    id bigint NOT NULL,
    title character varying(500) NOT NULL,
    category_id bigint NOT NULL,
    case_number character varying(100),
    court_name character varying(200),
    judge_date date,
    case_type character varying(50),
    cause_of_action character varying(200),
    trial_procedure character varying(50),
    plaintiff character varying(500),
    defendant character varying(500),
    case_summary text,
    court_opinion text,
    judgment_result text,
    case_significance character varying(2000),
    keywords character varying(500),
    source character varying(50) DEFAULT 'EXTERNAL'::character varying,
    matter_id bigint,
    attachment_url character varying(500),
    view_count integer DEFAULT 0,
    collect_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE case_library; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.case_library IS '案例库表';
--
-- Name: COLUMN case_library.case_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.case_library.case_type IS '案件类型: CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务';
--
-- Name: COLUMN case_library.source; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.case_library.source IS '来源: EXTERNAL外部/INTERNAL内部';
--
-- Name: case_library_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.case_library_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: case_library_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.case_library_id_seq OWNED BY public.case_library.id;
--
-- Name: case_study_note; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.case_study_note (
    id bigint NOT NULL,
    case_id bigint NOT NULL,
    user_id bigint NOT NULL,
    note_content text,
    key_points text,
    personal_insights text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE case_study_note; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.case_study_note IS '案例学习笔记表';
--
-- Name: case_study_note_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.case_study_note_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: case_study_note_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.case_study_note_id_seq OWNED BY public.case_study_note.id;
--
-- Name: knowledge_article; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.knowledge_article (
    id bigint NOT NULL,
    title character varying(200) NOT NULL,
    category character varying(50),
    content text NOT NULL,
    summary character varying(500),
    author_id bigint NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    tags character varying(500),
    view_count integer DEFAULT 0,
    like_count integer DEFAULT 0,
    comment_count integer DEFAULT 0,
    published_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE knowledge_article; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.knowledge_article IS '经验文章表';
--
-- Name: COLUMN knowledge_article.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.knowledge_article.status IS '状态: DRAFT草稿/PUBLISHED已发布/ARCHIVED已归档';
--
-- Name: knowledge_article_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.knowledge_article_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: knowledge_article_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.knowledge_article_id_seq OWNED BY public.knowledge_article.id;
--
-- Name: knowledge_collection; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.knowledge_collection (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    target_type character varying(20) NOT NULL,
    target_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE knowledge_collection; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.knowledge_collection IS '知识收藏表';
--
-- Name: COLUMN knowledge_collection.target_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.knowledge_collection.target_type IS '目标类型: LAW法规/CASE案例';
--
-- Name: knowledge_collection_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.knowledge_collection_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: knowledge_collection_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.knowledge_collection_id_seq OWNED BY public.knowledge_collection.id;
--
-- Name: law_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.law_category (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    parent_id bigint DEFAULT 0,
    level integer DEFAULT 1,
    sort_order integer DEFAULT 0,
    description character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE law_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.law_category IS '法规分类表';
--
-- Name: law_category_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.law_category_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: law_category_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.law_category_id_seq OWNED BY public.law_category.id;
--
-- Name: law_regulation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.law_regulation (
    id bigint NOT NULL,
    title character varying(500) NOT NULL,
    category_id bigint NOT NULL,
    doc_number character varying(100),
    issuing_authority character varying(200),
    issue_date date,
    effective_date date,
    expiry_date date,
    status character varying(20) DEFAULT 'EFFECTIVE'::character varying,
    content text,
    summary character varying(2000),
    keywords character varying(500),
    source character varying(200),
    attachment_url character varying(500),
    view_count integer DEFAULT 0,
    collect_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE law_regulation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.law_regulation IS '法规表';
--
-- Name: COLUMN law_regulation.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.law_regulation.status IS '状态: EFFECTIVE有效/AMENDED已修订/REPEALED已废止';
--
-- Name: law_regulation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.law_regulation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: law_regulation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.law_regulation_id_seq OWNED BY public.law_regulation.id;
--
-- Name: article_collection id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.article_collection ALTER COLUMN id SET DEFAULT nextval('public.article_collection_id_seq'::regclass);
--
-- Name: article_comment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.article_comment ALTER COLUMN id SET DEFAULT nextval('public.article_comment_id_seq'::regclass);
--
-- Name: case_category id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.case_category ALTER COLUMN id SET DEFAULT nextval('public.case_category_id_seq'::regclass);
--
-- Name: case_library id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.case_library ALTER COLUMN id SET DEFAULT nextval('public.case_library_id_seq'::regclass);
--
-- Name: case_study_note id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.case_study_note ALTER COLUMN id SET DEFAULT nextval('public.case_study_note_id_seq'::regclass);
--
-- Name: knowledge_article id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_article ALTER COLUMN id SET DEFAULT nextval('public.knowledge_article_id_seq'::regclass);
--
-- Name: knowledge_collection id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_collection ALTER COLUMN id SET DEFAULT nextval('public.knowledge_collection_id_seq'::regclass);
--
-- Name: law_category id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.law_category ALTER COLUMN id SET DEFAULT nextval('public.law_category_id_seq'::regclass);
--
-- Name: law_regulation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.law_regulation ALTER COLUMN id SET DEFAULT nextval('public.law_regulation_id_seq'::regclass);
--
-- Name: article_collection article_collection_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.article_collection
    ADD CONSTRAINT article_collection_pkey PRIMARY KEY (id);
--
-- Name: article_comment article_comment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.article_comment
    ADD CONSTRAINT article_comment_pkey PRIMARY KEY (id);
--
-- Name: case_category case_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.case_category
    ADD CONSTRAINT case_category_pkey PRIMARY KEY (id);
--
-- Name: case_library case_library_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.case_library
    ADD CONSTRAINT case_library_pkey PRIMARY KEY (id);
--
-- Name: case_study_note case_study_note_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.case_study_note
    ADD CONSTRAINT case_study_note_pkey PRIMARY KEY (id);
--
-- Name: knowledge_article knowledge_article_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_article
    ADD CONSTRAINT knowledge_article_pkey PRIMARY KEY (id);
--
-- Name: knowledge_collection knowledge_collection_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_collection
    ADD CONSTRAINT knowledge_collection_pkey PRIMARY KEY (id);
--
-- Name: law_category law_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.law_category
    ADD CONSTRAINT law_category_pkey PRIMARY KEY (id);
--
-- Name: law_regulation law_regulation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.law_regulation
    ADD CONSTRAINT law_regulation_pkey PRIMARY KEY (id);
--
-- Name: article_collection uk_article_collection; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.article_collection
    ADD CONSTRAINT uk_article_collection UNIQUE (user_id, article_id);
--
-- Name: case_study_note uk_case_study_note; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.case_study_note
    ADD CONSTRAINT uk_case_study_note UNIQUE (case_id, user_id);
--
-- Name: knowledge_collection uk_knowledge_collection; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_collection
    ADD CONSTRAINT uk_knowledge_collection UNIQUE (user_id, target_type, target_id);
--
-- Name: idx_article_collection_article; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_article_collection_article ON public.article_collection USING btree (article_id);
--
-- Name: idx_article_collection_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_article_collection_user ON public.article_collection USING btree (user_id);
--
-- Name: idx_article_comment_article; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_article_comment_article ON public.article_comment USING btree (article_id);
--
-- Name: idx_article_comment_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_article_comment_parent ON public.article_comment USING btree (parent_id);
--
-- Name: idx_article_comment_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_article_comment_user ON public.article_comment USING btree (user_id);
--
-- Name: idx_case_category_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_case_category_parent ON public.case_category USING btree (parent_id);
--
-- Name: idx_case_library_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_case_library_category ON public.case_library USING btree (category_id);
--
-- Name: idx_case_library_court; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_case_library_court ON public.case_library USING btree (court_name);
--
-- Name: idx_case_library_judge_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_case_library_judge_date ON public.case_library USING btree (judge_date);
--
-- Name: idx_case_library_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_case_library_source ON public.case_library USING btree (source);
--
-- Name: idx_case_study_note_case; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_case_study_note_case ON public.case_study_note USING btree (case_id);
--
-- Name: idx_case_study_note_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_case_study_note_user ON public.case_study_note USING btree (user_id);
--
-- Name: idx_knowledge_article_author; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_knowledge_article_author ON public.knowledge_article USING btree (author_id);
--
-- Name: idx_knowledge_article_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_knowledge_article_category ON public.knowledge_article USING btree (category);
--
-- Name: idx_knowledge_article_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_knowledge_article_status ON public.knowledge_article USING btree (status);
--
-- Name: idx_knowledge_collection_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_knowledge_collection_target ON public.knowledge_collection USING btree (target_type, target_id);
--
-- Name: idx_knowledge_collection_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_knowledge_collection_user ON public.knowledge_collection USING btree (user_id);
--
-- Name: idx_knowledge_collection_user_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_knowledge_collection_user_target ON public.knowledge_collection USING btree (user_id, target_type, target_id);
--
-- Name: idx_law_category_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_law_category_parent ON public.law_category USING btree (parent_id);
--
-- Name: idx_law_regulation_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_law_regulation_category ON public.law_regulation USING btree (category_id);
--
-- Name: idx_law_regulation_issue_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_law_regulation_issue_date ON public.law_regulation USING btree (issue_date);
--
-- Name: idx_law_regulation_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_law_regulation_status ON public.law_regulation USING btree (status);
