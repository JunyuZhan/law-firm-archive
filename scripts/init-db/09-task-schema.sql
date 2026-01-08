-- =====================================================
-- 律师事务所管理系统 - 任务管理模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 任务、任务评论等任务相关表
-- =====================================================

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
    deleted boolean DEFAULT false,
    review_status character varying(20),
    review_comment text,
    reviewed_at timestamp without time zone,
    reviewed_by bigint
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
-- Name: COLUMN task.review_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.task.review_status IS '验收状态: PENDING_REVIEW-待验收, APPROVED-已通过, REJECTED-已退回';
--
-- Name: COLUMN task.review_comment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.task.review_comment IS '验收意见（退回时填写）';
--
-- Name: COLUMN task.reviewed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.task.reviewed_at IS '验收时间';
--
-- Name: COLUMN task.reviewed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.task.reviewed_by IS '验收人ID';
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
-- Name: task id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task ALTER COLUMN id SET DEFAULT nextval('public.task_id_seq'::regclass);
--
-- Name: task_comment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_comment ALTER COLUMN id SET DEFAULT nextval('public.task_comment_id_seq'::regclass);
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
