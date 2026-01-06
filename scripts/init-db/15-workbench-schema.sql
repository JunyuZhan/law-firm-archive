--
-- PostgreSQL database dump
--

\restrict ADhr8lYo4Ttha3So4y1A8AQev1XngIsc83sSGanosHko1EydPbnwS3n7AC6Pj2Q

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
-- Name: workbench_approval; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workbench_approval (
    id bigint NOT NULL,
    approval_no character varying(50) NOT NULL,
    business_type character varying(50) NOT NULL,
    business_id bigint NOT NULL,
    business_no character varying(50),
    business_title character varying(200),
    applicant_id bigint NOT NULL,
    applicant_name character varying(100),
    approver_id bigint NOT NULL,
    approver_name character varying(100),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    comment text,
    approved_at timestamp without time zone,
    priority character varying(20) DEFAULT 'MEDIUM'::character varying,
    urgency character varying(20) DEFAULT 'NORMAL'::character varying,
    business_snapshot text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE workbench_approval; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.workbench_approval IS '审批记录表';


--
-- Name: COLUMN workbench_approval.approval_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.approval_no IS '审批编号';


--
-- Name: COLUMN workbench_approval.business_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.business_type IS '业务类型：CONTRACT-合同, SEAL_APPLICATION-用印申请, CONFLICT_CHECK-利冲检查, EXPENSE-费用报销, etc.';


--
-- Name: COLUMN workbench_approval.business_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.business_id IS '业务ID（关联具体业务表的主键）';


--
-- Name: COLUMN workbench_approval.business_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.business_no IS '业务编号（如合同编号、用印申请编号等）';


--
-- Name: COLUMN workbench_approval.business_title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.business_title IS '业务标题';


--
-- Name: COLUMN workbench_approval.applicant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.applicant_id IS '发起人ID';


--
-- Name: COLUMN workbench_approval.applicant_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.applicant_name IS '发起人姓名';


--
-- Name: COLUMN workbench_approval.approver_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.approver_id IS '审批人ID';


--
-- Name: COLUMN workbench_approval.approver_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.approver_name IS '审批人姓名';


--
-- Name: COLUMN workbench_approval.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.status IS '审批状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝, CANCELLED-已取消';


--
-- Name: COLUMN workbench_approval.comment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.comment IS '审批意见';


--
-- Name: COLUMN workbench_approval.approved_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.approved_at IS '审批时间';


--
-- Name: COLUMN workbench_approval.priority; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.priority IS '优先级：HIGH-高, MEDIUM-中, LOW-低';


--
-- Name: COLUMN workbench_approval.urgency; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.urgency IS '紧急程度：URGENT-紧急, NORMAL-普通';


--
-- Name: COLUMN workbench_approval.business_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.workbench_approval.business_snapshot IS '业务数据快照（JSON格式，保存审批时的业务数据）';


--
-- Name: workbench_approval_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.workbench_approval_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: workbench_approval_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.workbench_approval_id_seq OWNED BY public.workbench_approval.id;


--
-- Name: workbench_report_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workbench_report_template (
    id bigint NOT NULL,
    template_no character varying(50) NOT NULL,
    template_name character varying(100) NOT NULL,
    description character varying(500),
    data_source character varying(50) NOT NULL,
    field_config text NOT NULL,
    filter_config text,
    group_config text,
    sort_config text,
    aggregate_config text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    is_system boolean DEFAULT false,
    created_by bigint,
    created_by_name character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE workbench_report_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.workbench_report_template IS '自定义报表模板表';


--
-- Name: workbench_report_template_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.workbench_report_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: workbench_report_template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.workbench_report_template_id_seq OWNED BY public.workbench_report_template.id;


--
-- Name: workbench_scheduled_report; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workbench_scheduled_report (
    id bigint NOT NULL,
    task_no character varying(50) NOT NULL,
    task_name character varying(100) NOT NULL,
    description character varying(500),
    template_id bigint NOT NULL,
    schedule_type character varying(20) NOT NULL,
    cron_expression character varying(100),
    execute_time character varying(10),
    execute_day_of_week integer,
    execute_day_of_month integer,
    report_parameters text,
    output_format character varying(20) DEFAULT 'EXCEL'::character varying,
    notify_enabled boolean DEFAULT false,
    notify_emails text,
    notify_user_ids text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    last_execute_time timestamp without time zone,
    last_execute_status character varying(20),
    next_execute_time timestamp without time zone,
    total_execute_count integer DEFAULT 0,
    success_count integer DEFAULT 0,
    fail_count integer DEFAULT 0,
    created_by bigint,
    created_by_name character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE workbench_scheduled_report; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.workbench_scheduled_report IS '定时报表任务表';


--
-- Name: workbench_scheduled_report_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.workbench_scheduled_report_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: workbench_scheduled_report_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.workbench_scheduled_report_id_seq OWNED BY public.workbench_scheduled_report.id;


--
-- Name: workbench_scheduled_report_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workbench_scheduled_report_log (
    id bigint NOT NULL,
    task_id bigint NOT NULL,
    task_no character varying(50),
    execute_time timestamp without time zone NOT NULL,
    status character varying(20) NOT NULL,
    report_id bigint,
    file_url character varying(500),
    file_size bigint,
    duration_ms bigint,
    error_message text,
    notify_status character varying(20),
    notify_result text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE workbench_scheduled_report_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.workbench_scheduled_report_log IS '定时报表执行记录表';


--
-- Name: workbench_scheduled_report_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.workbench_scheduled_report_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: workbench_scheduled_report_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.workbench_scheduled_report_log_id_seq OWNED BY public.workbench_scheduled_report_log.id;


--
-- Name: workbench_approval id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_approval ALTER COLUMN id SET DEFAULT nextval('public.workbench_approval_id_seq'::regclass);


--
-- Name: workbench_report_template id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_report_template ALTER COLUMN id SET DEFAULT nextval('public.workbench_report_template_id_seq'::regclass);


--
-- Name: workbench_scheduled_report id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_scheduled_report ALTER COLUMN id SET DEFAULT nextval('public.workbench_scheduled_report_id_seq'::regclass);


--
-- Name: workbench_scheduled_report_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_scheduled_report_log ALTER COLUMN id SET DEFAULT nextval('public.workbench_scheduled_report_log_id_seq'::regclass);


--
-- Name: workbench_approval workbench_approval_approval_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_approval
    ADD CONSTRAINT workbench_approval_approval_no_key UNIQUE (approval_no);


--
-- Name: workbench_approval workbench_approval_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_approval
    ADD CONSTRAINT workbench_approval_pkey PRIMARY KEY (id);


--
-- Name: workbench_report_template workbench_report_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_report_template
    ADD CONSTRAINT workbench_report_template_pkey PRIMARY KEY (id);


--
-- Name: workbench_report_template workbench_report_template_template_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_report_template
    ADD CONSTRAINT workbench_report_template_template_no_key UNIQUE (template_no);


--
-- Name: workbench_scheduled_report_log workbench_scheduled_report_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_scheduled_report_log
    ADD CONSTRAINT workbench_scheduled_report_log_pkey PRIMARY KEY (id);


--
-- Name: workbench_scheduled_report workbench_scheduled_report_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_scheduled_report
    ADD CONSTRAINT workbench_scheduled_report_pkey PRIMARY KEY (id);


--
-- Name: workbench_scheduled_report workbench_scheduled_report_task_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_scheduled_report
    ADD CONSTRAINT workbench_scheduled_report_task_no_key UNIQUE (task_no);


--
-- Name: idx_report_template_created_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_report_template_created_by ON public.workbench_report_template USING btree (created_by);


--
-- Name: idx_report_template_data_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_report_template_data_source ON public.workbench_report_template USING btree (data_source);


--
-- Name: idx_report_template_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_report_template_status ON public.workbench_report_template USING btree (status);


--
-- Name: idx_scheduled_log_execute_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scheduled_log_execute_time ON public.workbench_scheduled_report_log USING btree (execute_time);


--
-- Name: idx_scheduled_log_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scheduled_log_status ON public.workbench_scheduled_report_log USING btree (status);


--
-- Name: idx_scheduled_log_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scheduled_log_task ON public.workbench_scheduled_report_log USING btree (task_id);


--
-- Name: idx_scheduled_report_next_execute; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scheduled_report_next_execute ON public.workbench_scheduled_report USING btree (next_execute_time);


--
-- Name: idx_scheduled_report_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scheduled_report_status ON public.workbench_scheduled_report USING btree (status);


--
-- Name: idx_scheduled_report_template; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scheduled_report_template ON public.workbench_scheduled_report USING btree (template_id);


--
-- Name: idx_workbench_approval_applicant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workbench_approval_applicant ON public.workbench_approval USING btree (applicant_id);


--
-- Name: idx_workbench_approval_approver; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workbench_approval_approver ON public.workbench_approval USING btree (approver_id, status);


--
-- Name: idx_workbench_approval_business; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workbench_approval_business ON public.workbench_approval USING btree (business_type, business_id);


--
-- Name: idx_workbench_approval_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workbench_approval_created_at ON public.workbench_approval USING btree (created_at DESC);


--
-- Name: idx_workbench_approval_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workbench_approval_status ON public.workbench_approval USING btree (status);


--
-- Name: workbench_scheduled_report_log fk_scheduled_log_task; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_scheduled_report_log
    ADD CONSTRAINT fk_scheduled_log_task FOREIGN KEY (task_id) REFERENCES public.workbench_scheduled_report(id);


--
-- Name: workbench_scheduled_report fk_scheduled_report_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workbench_scheduled_report
    ADD CONSTRAINT fk_scheduled_report_template FOREIGN KEY (template_id) REFERENCES public.workbench_report_template(id);


--
-- PostgreSQL database dump complete
--

\unrestrict ADhr8lYo4Ttha3So4y1A8AQev1XngIsc83sSGanosHko1EydPbnwS3n7AC6Pj2Q

