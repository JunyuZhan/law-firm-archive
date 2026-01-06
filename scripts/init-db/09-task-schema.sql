--
-- PostgreSQL database dump
--

\restrict EwKcbxnyhcoip5oi11SsZbcUpwhVhoGdnyluhpQg8377NnIcH9AEH4DTUTKSIb3

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
-- Name: schedule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schedule (
    id bigint NOT NULL,
    matter_id bigint,
    user_id bigint NOT NULL,
    title character varying(500) NOT NULL,
    description text,
    location character varying(500),
    schedule_type character varying(50) NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    all_day boolean DEFAULT false,
    reminder_minutes integer,
    reminder_sent boolean DEFAULT false,
    recurrence_rule character varying(200),
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE schedule; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.schedule IS '日程表';


--
-- Name: COLUMN schedule.schedule_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.schedule.schedule_type IS '日程类型: COURT-开庭, MEETING-会议, DEADLINE-期限, APPOINTMENT-约见, OTHER-其他';


--
-- Name: schedule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.schedule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.schedule_id_seq OWNED BY public.schedule.id;


--
-- Name: task; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task (
    id bigint NOT NULL,
    task_no character varying(50) NOT NULL,
    matter_id bigint NOT NULL,
    parent_id bigint,
    title character varying(500) NOT NULL,
    description text,
    priority character varying(20) DEFAULT 'MEDIUM'::character varying,
    assignee_id bigint,
    assignee_name character varying(50),
    start_date date,
    due_date date,
    completed_at timestamp without time zone,
    status character varying(20) DEFAULT 'TODO'::character varying,
    progress integer DEFAULT 0,
    reminder_date timestamp without time zone,
    reminder_sent boolean DEFAULT false,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE task; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.task IS '任务表';


--
-- Name: COLUMN task.priority; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.task.priority IS '优先级: HIGH-高, MEDIUM-中, LOW-低';


--
-- Name: COLUMN task.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.task.status IS '状态: TODO-待办, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消';


--
-- Name: task_comment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_comment (
    id bigint NOT NULL,
    task_id bigint NOT NULL,
    content text NOT NULL,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    attachments jsonb,
    mentioned_user_ids jsonb
);


--
-- Name: TABLE task_comment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.task_comment IS '任务评论表';


--
-- Name: COLUMN task_comment.attachments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.task_comment.attachments IS '附件列表（JSON格式，存储文件URL列表）';


--
-- Name: COLUMN task_comment.mentioned_user_ids; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.task_comment.mentioned_user_ids IS '@提醒的用户ID列表（JSON格式）';


--
-- Name: task_comment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_comment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_comment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.task_comment_id_seq OWNED BY public.task_comment.id;


--
-- Name: task_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.task_id_seq OWNED BY public.task.id;


--
-- Name: schedule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedule ALTER COLUMN id SET DEFAULT nextval('public.schedule_id_seq'::regclass);


--
-- Name: task id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task ALTER COLUMN id SET DEFAULT nextval('public.task_id_seq'::regclass);


--
-- Name: task_comment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_comment ALTER COLUMN id SET DEFAULT nextval('public.task_comment_id_seq'::regclass);


--
-- Name: schedule schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedule
    ADD CONSTRAINT schedule_pkey PRIMARY KEY (id);


--
-- Name: task_comment task_comment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_comment
    ADD CONSTRAINT task_comment_pkey PRIMARY KEY (id);


--
-- Name: task task_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_pkey PRIMARY KEY (id);


--
-- Name: task task_task_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_task_no_key UNIQUE (task_no);


--
-- Name: idx_schedule_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_schedule_matter ON public.schedule USING btree (matter_id);


--
-- Name: idx_schedule_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_schedule_time ON public.schedule USING btree (start_time, end_time);


--
-- Name: idx_schedule_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_schedule_user ON public.schedule USING btree (user_id);


--
-- Name: idx_task_assignee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_task_assignee ON public.task USING btree (assignee_id);


--
-- Name: idx_task_comment_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_task_comment_created_at ON public.task_comment USING btree (created_at);


--
-- Name: idx_task_comment_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_task_comment_task ON public.task_comment USING btree (task_id);


--
-- Name: idx_task_comment_task_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_task_comment_task_id ON public.task_comment USING btree (task_id);


--
-- Name: idx_task_due_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_task_due_date ON public.task USING btree (due_date);


--
-- Name: idx_task_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_task_matter ON public.task USING btree (matter_id);


--
-- Name: idx_task_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_task_status ON public.task USING btree (status);


--
-- PostgreSQL database dump complete
--

\unrestrict EwKcbxnyhcoip5oi11SsZbcUpwhVhoGdnyluhpQg8377NnIcH9AEH4DTUTKSIb3

