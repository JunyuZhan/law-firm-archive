--
-- PostgreSQL database dump
--

\restrict mBRRYHjX525wPEz4MxcgEf3GD31MP7I9DCPJoqag6KOecJX78m7CdchdQkRsSqy

-- Dumped from database version 15.15
-- Dumped by pg_dump version 15.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

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
    name character varying(100) NOT NULL,
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
    updated_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
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
    file_name character varying(500) NOT NULL,
    file_path character varying(1000) NOT NULL,
    file_size bigint,
    variables jsonb,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    use_count integer DEFAULT 0,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
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
-- Name: COLUMN doc_template.variables; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.doc_template.variables IS '模板变量定义，如 [{"name":"clientName","label":"客户名称","type":"text"}]';


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
-- Name: doc_access_log doc_access_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_access_log
    ADD CONSTRAINT doc_access_log_pkey PRIMARY KEY (id);


--
-- Name: doc_category doc_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_category
    ADD CONSTRAINT doc_category_pkey PRIMARY KEY (id);


--
-- Name: doc_document doc_document_doc_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_document
    ADD CONSTRAINT doc_document_doc_no_key UNIQUE (doc_no);


--
-- Name: doc_document doc_document_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_document
    ADD CONSTRAINT doc_document_pkey PRIMARY KEY (id);


--
-- Name: doc_template doc_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_template
    ADD CONSTRAINT doc_template_pkey PRIMARY KEY (id);


--
-- Name: doc_template doc_template_template_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_template
    ADD CONSTRAINT doc_template_template_no_key UNIQUE (template_no);


--
-- Name: doc_version doc_version_document_id_version_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_version
    ADD CONSTRAINT doc_version_document_id_version_key UNIQUE (document_id, version);


--
-- Name: doc_version doc_version_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_version
    ADD CONSTRAINT doc_version_pkey PRIMARY KEY (id);


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
-- Name: idx_document_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_matter ON public.doc_document USING btree (matter_id);


--
-- Name: idx_document_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_status ON public.doc_document USING btree (status);


--
-- Name: idx_template_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_template_category ON public.doc_template USING btree (category_id);


--
-- PostgreSQL database dump complete
--

\unrestrict mBRRYHjX525wPEz4MxcgEf3GD31MP7I9DCPJoqag6KOecJX78m7CdchdQkRsSqy

