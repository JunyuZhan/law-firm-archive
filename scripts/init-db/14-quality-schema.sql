--
-- PostgreSQL database dump
--

\restrict xgda7mfljRGs7vujZ2hsz3FNwvn08eFAdLYD4bmvaeTHmnmxc4Nec4PwEAJTHfQ

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
-- Name: quality_check; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quality_check (
    id bigint NOT NULL,
    check_no character varying(50) NOT NULL,
    matter_id bigint NOT NULL,
    checker_id bigint NOT NULL,
    check_date date NOT NULL,
    check_type character varying(20) NOT NULL,
    status character varying(20) DEFAULT 'IN_PROGRESS'::character varying,
    total_score numeric(5,2),
    qualified boolean,
    check_summary text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE quality_check; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.quality_check IS '质量检查表';


--
-- Name: COLUMN quality_check.check_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quality_check.check_type IS '检查类型：ROUTINE常规, RANDOM随机, SPECIAL专项';


--
-- Name: COLUMN quality_check.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quality_check.status IS '状态：IN_PROGRESS进行中, COMPLETED已完成';


--
-- Name: quality_check_detail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quality_check_detail (
    id bigint NOT NULL,
    check_id bigint NOT NULL,
    standard_id bigint NOT NULL,
    check_result character varying(20),
    score numeric(5,2),
    max_score numeric(5,2),
    findings text,
    suggestions text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE quality_check_detail; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.quality_check_detail IS '质量检查明细表';


--
-- Name: COLUMN quality_check_detail.check_result; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quality_check_detail.check_result IS '检查结果：PASS通过, FAIL不通过, PARTIAL部分通过';


--
-- Name: quality_check_detail_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.quality_check_detail_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: quality_check_detail_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.quality_check_detail_id_seq OWNED BY public.quality_check_detail.id;


--
-- Name: quality_check_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.quality_check_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: quality_check_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.quality_check_id_seq OWNED BY public.quality_check.id;


--
-- Name: quality_check_standard; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quality_check_standard (
    id bigint NOT NULL,
    standard_no character varying(50) NOT NULL,
    standard_name character varying(200) NOT NULL,
    category character varying(50),
    description text,
    check_items text,
    applicable_matter_types character varying(500),
    weight numeric(5,2) DEFAULT 1.0,
    enabled boolean DEFAULT true,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE quality_check_standard; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.quality_check_standard IS '质量检查标准表';


--
-- Name: COLUMN quality_check_standard.category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quality_check_standard.category IS '分类：CONTRACT合同, DOCUMENT文书, PROCEDURE程序, OTHER其他';


--
-- Name: quality_check_standard_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.quality_check_standard_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: quality_check_standard_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.quality_check_standard_id_seq OWNED BY public.quality_check_standard.id;


--
-- Name: quality_issue; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quality_issue (
    id bigint NOT NULL,
    issue_no character varying(50) NOT NULL,
    check_id bigint,
    matter_id bigint NOT NULL,
    issue_type character varying(50),
    issue_description text NOT NULL,
    responsible_user_id bigint,
    status character varying(20) DEFAULT 'OPEN'::character varying,
    priority character varying(20) DEFAULT 'MEDIUM'::character varying,
    due_date date,
    resolution text,
    resolved_at timestamp without time zone,
    resolved_by bigint,
    verified_at timestamp without time zone,
    verified_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE quality_issue; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.quality_issue IS '问题整改表';


--
-- Name: COLUMN quality_issue.issue_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quality_issue.issue_type IS '问题类型：CRITICAL严重, MAJOR重要, MINOR一般';


--
-- Name: COLUMN quality_issue.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quality_issue.status IS '状态：OPEN待整改, IN_PROGRESS整改中, RESOLVED已解决, CLOSED已关闭';


--
-- Name: quality_issue_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.quality_issue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: quality_issue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.quality_issue_id_seq OWNED BY public.quality_issue.id;


--
-- Name: risk_warning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.risk_warning (
    id bigint NOT NULL,
    warning_no character varying(50) NOT NULL,
    matter_id bigint NOT NULL,
    risk_type character varying(50) NOT NULL,
    risk_level character varying(20) NOT NULL,
    risk_description text NOT NULL,
    warning_reason text,
    suggested_action text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    acknowledged_at timestamp without time zone,
    acknowledged_by bigint,
    resolved_at timestamp without time zone,
    resolved_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE risk_warning; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.risk_warning IS '风险预警表';


--
-- Name: COLUMN risk_warning.risk_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.risk_warning.risk_type IS '风险类型：DEADLINE期限风险, QUALITY质量风险, COST成本风险, LEGAL法律风险, OTHER其他';


--
-- Name: COLUMN risk_warning.risk_level; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.risk_warning.risk_level IS '风险等级：HIGH高, MEDIUM中, LOW低';


--
-- Name: COLUMN risk_warning.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.risk_warning.status IS '状态：ACTIVE活跃, ACKNOWLEDGED已确认, RESOLVED已解决, CLOSED已关闭';


--
-- Name: risk_warning_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.risk_warning_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: risk_warning_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.risk_warning_id_seq OWNED BY public.risk_warning.id;


--
-- Name: quality_check id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_check ALTER COLUMN id SET DEFAULT nextval('public.quality_check_id_seq'::regclass);


--
-- Name: quality_check_detail id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_check_detail ALTER COLUMN id SET DEFAULT nextval('public.quality_check_detail_id_seq'::regclass);


--
-- Name: quality_check_standard id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_check_standard ALTER COLUMN id SET DEFAULT nextval('public.quality_check_standard_id_seq'::regclass);


--
-- Name: quality_issue id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_issue ALTER COLUMN id SET DEFAULT nextval('public.quality_issue_id_seq'::regclass);


--
-- Name: risk_warning id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_warning ALTER COLUMN id SET DEFAULT nextval('public.risk_warning_id_seq'::regclass);


--
-- Name: quality_check quality_check_check_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_check
    ADD CONSTRAINT quality_check_check_no_key UNIQUE (check_no);


--
-- Name: quality_check_detail quality_check_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_check_detail
    ADD CONSTRAINT quality_check_detail_pkey PRIMARY KEY (id);


--
-- Name: quality_check quality_check_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_check
    ADD CONSTRAINT quality_check_pkey PRIMARY KEY (id);


--
-- Name: quality_check_standard quality_check_standard_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_check_standard
    ADD CONSTRAINT quality_check_standard_pkey PRIMARY KEY (id);


--
-- Name: quality_check_standard quality_check_standard_standard_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_check_standard
    ADD CONSTRAINT quality_check_standard_standard_no_key UNIQUE (standard_no);


--
-- Name: quality_issue quality_issue_issue_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_issue
    ADD CONSTRAINT quality_issue_issue_no_key UNIQUE (issue_no);


--
-- Name: quality_issue quality_issue_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quality_issue
    ADD CONSTRAINT quality_issue_pkey PRIMARY KEY (id);


--
-- Name: risk_warning risk_warning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_warning
    ADD CONSTRAINT risk_warning_pkey PRIMARY KEY (id);


--
-- Name: risk_warning risk_warning_warning_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_warning
    ADD CONSTRAINT risk_warning_warning_no_key UNIQUE (warning_no);


--
-- Name: idx_quality_check_checker; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_check_checker ON public.quality_check USING btree (checker_id);


--
-- Name: idx_quality_check_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_check_date ON public.quality_check USING btree (check_date);


--
-- Name: idx_quality_check_detail_check; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_check_detail_check ON public.quality_check_detail USING btree (check_id);


--
-- Name: idx_quality_check_detail_standard; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_check_detail_standard ON public.quality_check_detail USING btree (standard_id);


--
-- Name: idx_quality_check_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_check_matter ON public.quality_check USING btree (matter_id);


--
-- Name: idx_quality_check_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_check_status ON public.quality_check USING btree (status);


--
-- Name: idx_quality_issue_check; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_issue_check ON public.quality_issue USING btree (check_id);


--
-- Name: idx_quality_issue_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_issue_matter ON public.quality_issue USING btree (matter_id);


--
-- Name: idx_quality_issue_responsible; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_issue_responsible ON public.quality_issue USING btree (responsible_user_id);


--
-- Name: idx_quality_issue_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_issue_status ON public.quality_issue USING btree (status);


--
-- Name: idx_quality_standard_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_standard_category ON public.quality_check_standard USING btree (category);


--
-- Name: idx_quality_standard_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quality_standard_enabled ON public.quality_check_standard USING btree (enabled);


--
-- Name: idx_risk_warning_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_risk_warning_level ON public.risk_warning USING btree (risk_level);


--
-- Name: idx_risk_warning_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_risk_warning_matter ON public.risk_warning USING btree (matter_id);


--
-- Name: idx_risk_warning_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_risk_warning_status ON public.risk_warning USING btree (status);


--
-- Name: idx_risk_warning_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_risk_warning_type ON public.risk_warning USING btree (risk_type);


--
-- PostgreSQL database dump complete
--

\unrestrict xgda7mfljRGs7vujZ2hsz3FNwvn08eFAdLYD4bmvaeTHmnmxc4Nec4PwEAJTHfQ

