-- =====================================================
-- 律师事务所管理系统 - 行政管理模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 印章、会议室、外出、采购、资产等行政相关表
-- =====================================================

--
-- Name: admin_asset; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_asset (
    id bigint NOT NULL,
    asset_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    category character varying(50) NOT NULL,
    brand character varying(100),
    model character varying(100),
    specification character varying(500),
    serial_number character varying(100),
    purchase_date date,
    purchase_price numeric(12,2),
    supplier character varying(200),
    warranty_expire_date date,
    useful_life integer,
    location character varying(200),
    current_user_id bigint,
    department_id bigint,
    status character varying(20) DEFAULT 'IDLE'::character varying NOT NULL,
    image_url character varying(500),
    remarks text,
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE admin_asset; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_asset IS '资产信息表';
--
-- Name: COLUMN admin_asset.asset_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_asset.asset_no IS '资产编号';
--
-- Name: COLUMN admin_asset.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_asset.name IS '资产名称';
--
-- Name: COLUMN admin_asset.category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_asset.category IS '资产类别：OFFICE-办公设备, IT-IT设备, FURNITURE-家具, VEHICLE-车辆, OTHER-其他';
--
-- Name: COLUMN admin_asset.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_asset.status IS '状态：IDLE-闲置, IN_USE-使用中, MAINTENANCE-维修中, SCRAPPED-已报废';
--
-- Name: admin_asset_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_asset_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: admin_asset_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_asset_id_seq OWNED BY public.admin_asset.id;
--
-- Name: admin_asset_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_asset_record (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    record_type character varying(20) NOT NULL,
    operator_id bigint NOT NULL,
    from_user_id bigint,
    to_user_id bigint,
    operate_date date NOT NULL,
    expected_return_date date,
    actual_return_date date,
    reason text,
    maintenance_cost numeric(10,2),
    approval_status character varying(20) DEFAULT 'PENDING'::character varying,
    approver_id bigint,
    approval_comment text,
    remarks text,
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE admin_asset_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_asset_record IS '资产操作记录表';
--
-- Name: COLUMN admin_asset_record.record_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_asset_record.record_type IS '记录类型：RECEIVE-领用, RETURN-归还, TRANSFER-转移, MAINTENANCE-维修, SCRAP-报废';
--
-- Name: COLUMN admin_asset_record.approval_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_asset_record.approval_status IS '审批状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝';
--
-- Name: admin_asset_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_asset_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: admin_asset_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_asset_record_id_seq OWNED BY public.admin_asset_record.id;
--
-- Name: admin_purchase_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_purchase_item (
    id bigint NOT NULL,
    request_id bigint NOT NULL,
    item_name character varying(200) NOT NULL,
    specification character varying(500),
    unit character varying(20),
    quantity integer DEFAULT 1 NOT NULL,
    estimated_price numeric(10,2),
    actual_price numeric(10,2),
    estimated_amount numeric(12,2),
    actual_amount numeric(12,2),
    received_quantity integer DEFAULT 0,
    remarks text,
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE admin_purchase_item; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_purchase_item IS '采购明细表';
--
-- Name: admin_purchase_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_purchase_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: admin_purchase_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_purchase_item_id_seq OWNED BY public.admin_purchase_item.id;
--
-- Name: admin_purchase_receive; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_purchase_receive (
    id bigint NOT NULL,
    receive_no character varying(50) NOT NULL,
    request_id bigint NOT NULL,
    item_id bigint NOT NULL,
    quantity integer NOT NULL,
    receive_date date NOT NULL,
    receiver_id bigint NOT NULL,
    location character varying(200),
    convert_to_asset boolean DEFAULT false,
    asset_id bigint,
    remarks text,
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE admin_purchase_receive; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_purchase_receive IS '采购入库记录表';
--
-- Name: admin_purchase_receive_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_purchase_receive_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: admin_purchase_receive_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_purchase_receive_id_seq OWNED BY public.admin_purchase_receive.id;
--
-- Name: admin_purchase_request; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_purchase_request (
    id bigint NOT NULL,
    request_no character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    applicant_id bigint NOT NULL,
    department_id bigint,
    purchase_type character varying(20) NOT NULL,
    estimated_amount numeric(12,2),
    actual_amount numeric(12,2),
    expected_date date,
    reason text,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    approver_id bigint,
    approval_date date,
    approval_comment text,
    supplier_id bigint,
    remarks text,
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE admin_purchase_request; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_purchase_request IS '采购申请表';
--
-- Name: admin_purchase_request_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_purchase_request_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: admin_purchase_request_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_purchase_request_id_seq OWNED BY public.admin_purchase_request.id;
--
-- Name: admin_supplier; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_supplier (
    id bigint NOT NULL,
    supplier_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    supplier_type character varying(20) NOT NULL,
    contact_person character varying(50),
    contact_phone character varying(20),
    contact_email character varying(100),
    address character varying(500),
    credit_code character varying(50),
    bank_name character varying(100),
    bank_account character varying(50),
    supply_scope text,
    rating character varying(10) DEFAULT 'B'::character varying,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    remarks text,
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE admin_supplier; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_supplier IS '供应商信息表';
--
-- Name: admin_supplier_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_supplier_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: admin_supplier_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_supplier_id_seq OWNED BY public.admin_supplier.id;
--
-- Name: go_out_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.go_out_record (
    id bigint NOT NULL,
    record_no character varying(50) NOT NULL,
    user_id bigint NOT NULL,
    out_time timestamp without time zone NOT NULL,
    expected_return_time timestamp without time zone,
    actual_return_time timestamp without time zone,
    location character varying(200),
    reason character varying(500) NOT NULL,
    companions character varying(500),
    status character varying(20) DEFAULT 'OUT'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE go_out_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.go_out_record IS '外出登记表';
--
-- Name: COLUMN go_out_record.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.go_out_record.status IS '状态: OUT外出中/RETURNED已返回';
--
-- Name: go_out_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.go_out_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: go_out_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.go_out_record_id_seq OWNED BY public.go_out_record.id;
--
-- Name: meeting_booking; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meeting_booking (
    id bigint NOT NULL,
    booking_no character varying(30) NOT NULL,
    room_id bigint NOT NULL,
    title character varying(200) NOT NULL,
    organizer_id bigint NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    attendees character varying(1000),
    description character varying(500),
    status character varying(20) DEFAULT 'BOOKED'::character varying,
    reminder_sent boolean DEFAULT false,
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE meeting_booking; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.meeting_booking IS '会议预约表';
--
-- Name: COLUMN meeting_booking.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.meeting_booking.status IS '状态: BOOKED已预约/IN_PROGRESS进行中/COMPLETED已完成/CANCELLED已取消';
--
-- Name: meeting_booking_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meeting_booking_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: meeting_booking_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meeting_booking_id_seq OWNED BY public.meeting_booking.id;
--
-- Name: meeting_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meeting_record (
    id bigint NOT NULL,
    record_no character varying(50) NOT NULL,
    booking_id bigint,
    room_id bigint,
    title character varying(200) NOT NULL,
    meeting_date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    organizer_id bigint NOT NULL,
    attendees character varying(1000),
    content text,
    decisions text,
    action_items text,
    attachment_url character varying(500),
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE meeting_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.meeting_record IS '会议记录表';
--
-- Name: meeting_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meeting_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: meeting_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meeting_record_id_seq OWNED BY public.meeting_record.id;
--
-- Name: meeting_room; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meeting_room (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(30) NOT NULL,
    location character varying(200),
    capacity integer NOT NULL,
    equipment character varying(500),
    description character varying(500),
    status character varying(20) DEFAULT 'AVAILABLE'::character varying,
    enabled boolean DEFAULT true,
    sort_order integer DEFAULT 0,
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE meeting_room; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.meeting_room IS '会议室表';
--
-- Name: COLUMN meeting_room.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.meeting_room.status IS '状态: AVAILABLE可用/OCCUPIED占用/MAINTENANCE维护中';
--
-- Name: meeting_room_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meeting_room_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: meeting_room_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meeting_room_id_seq OWNED BY public.meeting_room.id;
--
-- Name: seal_application; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.seal_application (
    id bigint NOT NULL,
    application_no character varying(50) NOT NULL,
    applicant_id bigint NOT NULL,
    applicant_name character varying(50),
    department_id bigint,
    seal_id bigint NOT NULL,
    seal_name character varying(100),
    matter_id bigint,
    matter_name character varying(500),
    document_name character varying(500) NOT NULL,
    document_type character varying(50),
    copies integer DEFAULT 1,
    use_purpose text,
    expected_use_date date,
    actual_use_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approved_by bigint,
    approved_at timestamp without time zone,
    approval_comment text,
    used_by bigint,
    used_at timestamp without time zone,
    use_remark text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    updated_by bigint
);
--
-- Name: TABLE seal_application; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.seal_application IS '用印申请表';
--
-- Name: COLUMN seal_application.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.seal_application.status IS '状态: PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, USED-已用印, CANCELLED-已取消';
--
-- Name: COLUMN seal_application.updated_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.seal_application.updated_by IS '最后更新人ID';
--
-- Name: seal_application_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.seal_application_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: seal_application_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.seal_application_id_seq OWNED BY public.seal_application.id;
--
-- Name: seal_info; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.seal_info (
    id bigint NOT NULL,
    seal_no character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    seal_type character varying(50) NOT NULL,
    keeper_id bigint NOT NULL,
    keeper_name character varying(50),
    image_url character varying(500),
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    description text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    updated_by bigint
);
--
-- Name: TABLE seal_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.seal_info IS '印章表';
--
-- Name: COLUMN seal_info.seal_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.seal_info.seal_type IS '印章类型: OFFICIAL-公章, CONTRACT-合同章, FINANCE-财务章, LEGAL-法人章, OTHER-其他';
--
-- Name: COLUMN seal_info.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.seal_info.status IS '状态: ACTIVE-在用, DISABLED-停用, LOST-遗失, DESTROYED-销毁';
--
-- Name: COLUMN seal_info.updated_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.seal_info.updated_by IS '最后更新人ID';
--
-- Name: seal_info_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.seal_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: seal_info_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.seal_info_id_seq OWNED BY public.seal_info.id;
--
-- Name: seal_usage_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.seal_usage_record (
    id bigint NOT NULL,
    application_id bigint NOT NULL,
    seal_id bigint NOT NULL,
    document_name character varying(500) NOT NULL,
    copies integer DEFAULT 1,
    used_by bigint NOT NULL,
    used_at timestamp without time zone NOT NULL,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);
--
-- Name: TABLE seal_usage_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.seal_usage_record IS '用印记录表';
--
-- Name: seal_usage_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.seal_usage_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: seal_usage_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.seal_usage_record_id_seq OWNED BY public.seal_usage_record.id;
--
-- Name: admin_asset id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_asset ALTER COLUMN id SET DEFAULT nextval('public.admin_asset_id_seq'::regclass);
--
-- Name: admin_asset_record id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_asset_record ALTER COLUMN id SET DEFAULT nextval('public.admin_asset_record_id_seq'::regclass);
--
-- Name: admin_purchase_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_item ALTER COLUMN id SET DEFAULT nextval('public.admin_purchase_item_id_seq'::regclass);
--
-- Name: admin_purchase_receive id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_receive ALTER COLUMN id SET DEFAULT nextval('public.admin_purchase_receive_id_seq'::regclass);
--
-- Name: admin_purchase_request id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_request ALTER COLUMN id SET DEFAULT nextval('public.admin_purchase_request_id_seq'::regclass);
--
-- Name: admin_supplier id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_supplier ALTER COLUMN id SET DEFAULT nextval('public.admin_supplier_id_seq'::regclass);
--
-- Name: go_out_record id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.go_out_record ALTER COLUMN id SET DEFAULT nextval('public.go_out_record_id_seq'::regclass);
--
-- Name: meeting_booking id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meeting_booking ALTER COLUMN id SET DEFAULT nextval('public.meeting_booking_id_seq'::regclass);
--
-- Name: meeting_record id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meeting_record ALTER COLUMN id SET DEFAULT nextval('public.meeting_record_id_seq'::regclass);
--
-- Name: meeting_room id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meeting_room ALTER COLUMN id SET DEFAULT nextval('public.meeting_room_id_seq'::regclass);
--
-- Name: seal_application id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seal_application ALTER COLUMN id SET DEFAULT nextval('public.seal_application_id_seq'::regclass);
--
-- Name: seal_info id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seal_info ALTER COLUMN id SET DEFAULT nextval('public.seal_info_id_seq'::regclass);
--
-- Name: seal_usage_record id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seal_usage_record ALTER COLUMN id SET DEFAULT nextval('public.seal_usage_record_id_seq'::regclass);
--
-- Name: admin_asset admin_asset_asset_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_asset
    ADD CONSTRAINT admin_asset_asset_no_key UNIQUE (asset_no);
--
-- Name: admin_asset admin_asset_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_asset
    ADD CONSTRAINT admin_asset_pkey PRIMARY KEY (id);
--
-- Name: admin_asset_record admin_asset_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_asset_record
    ADD CONSTRAINT admin_asset_record_pkey PRIMARY KEY (id);
--
-- Name: admin_purchase_item admin_purchase_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_item
    ADD CONSTRAINT admin_purchase_item_pkey PRIMARY KEY (id);
--
-- Name: admin_purchase_receive admin_purchase_receive_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_receive
    ADD CONSTRAINT admin_purchase_receive_pkey PRIMARY KEY (id);
--
-- Name: admin_purchase_receive admin_purchase_receive_receive_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_receive
    ADD CONSTRAINT admin_purchase_receive_receive_no_key UNIQUE (receive_no);
--
-- Name: admin_purchase_request admin_purchase_request_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_request
    ADD CONSTRAINT admin_purchase_request_pkey PRIMARY KEY (id);
--
-- Name: admin_purchase_request admin_purchase_request_request_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_request
    ADD CONSTRAINT admin_purchase_request_request_no_key UNIQUE (request_no);
--
-- Name: admin_supplier admin_supplier_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_supplier
    ADD CONSTRAINT admin_supplier_pkey PRIMARY KEY (id);
--
-- Name: admin_supplier admin_supplier_supplier_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_supplier
    ADD CONSTRAINT admin_supplier_supplier_no_key UNIQUE (supplier_no);
--
-- Name: go_out_record go_out_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.go_out_record
    ADD CONSTRAINT go_out_record_pkey PRIMARY KEY (id);
--
-- Name: go_out_record go_out_record_record_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.go_out_record
    ADD CONSTRAINT go_out_record_record_no_key UNIQUE (record_no);
--
-- Name: meeting_booking meeting_booking_booking_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meeting_booking
    ADD CONSTRAINT meeting_booking_booking_no_key UNIQUE (booking_no);
--
-- Name: meeting_booking meeting_booking_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meeting_booking
    ADD CONSTRAINT meeting_booking_pkey PRIMARY KEY (id);
--
-- Name: meeting_record meeting_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meeting_record
    ADD CONSTRAINT meeting_record_pkey PRIMARY KEY (id);
--
-- Name: meeting_record meeting_record_record_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meeting_record
    ADD CONSTRAINT meeting_record_record_no_key UNIQUE (record_no);
--
-- Name: meeting_room meeting_room_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meeting_room
    ADD CONSTRAINT meeting_room_code_key UNIQUE (code);
--
-- Name: meeting_room meeting_room_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meeting_room
    ADD CONSTRAINT meeting_room_pkey PRIMARY KEY (id);
--
-- Name: seal_application seal_application_application_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seal_application
    ADD CONSTRAINT seal_application_application_no_key UNIQUE (application_no);
--
-- Name: seal_application seal_application_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seal_application
    ADD CONSTRAINT seal_application_pkey PRIMARY KEY (id);
--
-- Name: seal_info seal_info_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seal_info
    ADD CONSTRAINT seal_info_pkey PRIMARY KEY (id);
--
-- Name: seal_info seal_info_seal_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seal_info
    ADD CONSTRAINT seal_info_seal_no_key UNIQUE (seal_no);
--
-- Name: seal_usage_record seal_usage_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seal_usage_record
    ADD CONSTRAINT seal_usage_record_pkey PRIMARY KEY (id);
--
-- Name: idx_asset_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_category ON public.admin_asset USING btree (category);
--
-- Name: idx_asset_current_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_current_user ON public.admin_asset USING btree (current_user_id);
--
-- Name: idx_asset_department; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_department ON public.admin_asset USING btree (department_id);
--
-- Name: idx_asset_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_no ON public.admin_asset USING btree (asset_no);
--
-- Name: idx_asset_record_asset; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_record_asset ON public.admin_asset_record USING btree (asset_id);
--
-- Name: idx_asset_record_operator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_record_operator ON public.admin_asset_record USING btree (operator_id);
--
-- Name: idx_asset_record_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_record_type ON public.admin_asset_record USING btree (record_type);
--
-- Name: idx_asset_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_status ON public.admin_asset USING btree (status);
--
-- Name: idx_go_out_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_go_out_status ON public.go_out_record USING btree (status);
--
-- Name: idx_go_out_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_go_out_time ON public.go_out_record USING btree (out_time);
--
-- Name: idx_go_out_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_go_out_user ON public.go_out_record USING btree (user_id);
--
-- Name: idx_meeting_booking_organizer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meeting_booking_organizer ON public.meeting_booking USING btree (organizer_id);
--
-- Name: idx_meeting_booking_room; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meeting_booking_room ON public.meeting_booking USING btree (room_id);
--
-- Name: idx_meeting_booking_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meeting_booking_status ON public.meeting_booking USING btree (status);
--
-- Name: idx_meeting_booking_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meeting_booking_time ON public.meeting_booking USING btree (start_time, end_time);
--
-- Name: idx_meeting_record_booking; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meeting_record_booking ON public.meeting_record USING btree (booking_id);
--
-- Name: idx_meeting_record_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meeting_record_date ON public.meeting_record USING btree (meeting_date);
--
-- Name: idx_meeting_record_room; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meeting_record_room ON public.meeting_record USING btree (room_id);
--
-- Name: idx_purchase_item_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_item_request ON public.admin_purchase_item USING btree (request_id);
--
-- Name: idx_purchase_receive_item; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_receive_item ON public.admin_purchase_receive USING btree (item_id);
--
-- Name: idx_purchase_receive_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_receive_request ON public.admin_purchase_receive USING btree (request_id);
--
-- Name: idx_purchase_request_applicant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_request_applicant ON public.admin_purchase_request USING btree (applicant_id);
--
-- Name: idx_purchase_request_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_request_no ON public.admin_purchase_request USING btree (request_no);
--
-- Name: idx_purchase_request_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_request_status ON public.admin_purchase_request USING btree (status);
--
-- Name: idx_seal_app_applicant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_seal_app_applicant ON public.seal_application USING btree (applicant_id);
--
-- Name: idx_seal_app_seal; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_seal_app_seal ON public.seal_application USING btree (seal_id);
--
-- Name: idx_seal_app_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_seal_app_status ON public.seal_application USING btree (status);
--
-- Name: idx_seal_keeper; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_seal_keeper ON public.seal_info USING btree (keeper_id);
--
-- Name: idx_seal_usage_seal; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_seal_usage_seal ON public.seal_usage_record USING btree (seal_id);
--
-- Name: idx_supplier_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_supplier_no ON public.admin_supplier USING btree (supplier_no);
--
-- Name: idx_supplier_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_supplier_status ON public.admin_supplier USING btree (status);
--
-- Name: idx_supplier_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_supplier_type ON public.admin_supplier USING btree (supplier_type);
--
-- Name: admin_asset_record fk_asset_record_asset; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_asset_record
    ADD CONSTRAINT fk_asset_record_asset FOREIGN KEY (asset_id) REFERENCES public.admin_asset(id);
--
-- Name: admin_purchase_item fk_purchase_item_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_item
    ADD CONSTRAINT fk_purchase_item_request FOREIGN KEY (request_id) REFERENCES public.admin_purchase_request(id);
--
-- Name: admin_purchase_receive fk_purchase_receive_item; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_receive
    ADD CONSTRAINT fk_purchase_receive_item FOREIGN KEY (item_id) REFERENCES public.admin_purchase_item(id);
--
-- Name: admin_purchase_receive fk_purchase_receive_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_purchase_receive
    ADD CONSTRAINT fk_purchase_receive_request FOREIGN KEY (request_id) REFERENCES public.admin_purchase_request(id);

-- =====================================================
-- 介绍信管理相关表（补充）
-- =====================================================
CREATE TABLE public.letter_application (
    id bigint NOT NULL,
    application_no character varying(50) NOT NULL,
    template_id bigint NOT NULL,
    matter_id bigint NOT NULL,
    client_id bigint,
    applicant_id bigint NOT NULL,
    applicant_name character varying(50),
    department_id bigint,
    letter_type character varying(50) NOT NULL,
    target_unit character varying(200) NOT NULL,
    target_contact character varying(50),
    target_phone character varying(50),
    target_address character varying(500),
    purpose character varying(500) NOT NULL,
    lawyer_ids character varying(500),
    lawyer_names character varying(500),
    content text,
    copies integer DEFAULT 1,
    expected_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approved_by bigint,
    approved_at timestamp without time zone,
    approval_comment character varying(500),
    printed_by bigint,
    printed_at timestamp without time zone,
    received_by bigint,
    received_at timestamp without time zone,
    remark character varying(500),
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    assigned_approver_id bigint,
    approval_id bigint
);


--
-- Name: TABLE letter_application; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.letter_application IS '出函申请表';


--
-- Name: COLUMN letter_application.application_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.application_no IS '申请编号';


--
-- Name: COLUMN letter_application.template_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.template_id IS '使用的模板ID';


--
-- Name: COLUMN letter_application.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.matter_id IS '关联项目ID';


--
-- Name: COLUMN letter_application.target_unit; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.target_unit IS '接收单位（如：XX市中级人民法院）';


--
-- Name: COLUMN letter_application.purpose; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.purpose IS '出函事由';


--
-- Name: COLUMN letter_application.lawyer_ids; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.lawyer_ids IS '出函律师ID列表（逗号分隔）';


--
-- Name: COLUMN letter_application.lawyer_names; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.lawyer_names IS '出函律师姓名列表';


--
-- Name: COLUMN letter_application.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.content IS '生成的函件内容';


--
-- Name: COLUMN letter_application.copies; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.copies IS '份数';


--
-- Name: COLUMN letter_application.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.status IS '状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, RETURNED-已退回, PRINTED-已打印, RECEIVED-已领取, CANCELLED-已取消';


--
-- Name: COLUMN letter_application.printed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.printed_by IS '打印人（行政）';


--
-- Name: COLUMN letter_application.received_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.received_by IS '领取人（律师）';


--
-- Name: COLUMN letter_application.assigned_approver_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.assigned_approver_id IS '指定审批人ID（申请时选择）';


--
-- Name: COLUMN letter_application.approval_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_application.approval_id IS '审批中心审批记录ID（关联 workbench_approval 表）';


--
-- Name: letter_application_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.letter_application_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: letter_application_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.letter_application_id_seq OWNED BY public.letter_application.id;


--
-- Name: letter_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.letter_template (
    id bigint NOT NULL,
    template_no character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    letter_type character varying(50) NOT NULL,
    content text NOT NULL,
    description character varying(500),
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    sort_order integer DEFAULT 0,
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE letter_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.letter_template IS '出函模板表';


--
-- Name: COLUMN letter_template.template_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_template.template_no IS '模板编号';


--
-- Name: COLUMN letter_template.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_template.name IS '模板名称';


--
-- Name: COLUMN letter_template.letter_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_template.letter_type IS '函件类型：INTRODUCTION-介绍信, MEETING-会见函, INVESTIGATION-调查函, FILE_REVIEW-阅卷函, LEGAL_OPINION-法律意见函, OTHER-其他';


--
-- Name: COLUMN letter_template.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_template.content IS '模板内容（支持变量如${lawyerName}、${matterName}、${targetUnit}等）';


--
-- Name: COLUMN letter_template.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.letter_template.status IS '状态：ACTIVE-启用, DISABLED-停用';


--
-- Name: letter_template_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.letter_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: letter_template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.letter_template_id_seq OWNED BY public.letter_template.id;


--
-- Name: letter_application id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.letter_application ALTER COLUMN id SET DEFAULT nextval('public.letter_application_id_seq'::regclass);


--
-- Name: letter_template id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.letter_template ALTER COLUMN id SET DEFAULT nextval('public.letter_template_id_seq'::regclass);


--
-- Name: letter_application letter_application_application_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.letter_application
    ADD CONSTRAINT letter_application_application_no_key UNIQUE (application_no);


--
-- Name: letter_application letter_application_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.letter_application
    ADD CONSTRAINT letter_application_pkey PRIMARY KEY (id);


--
-- Name: letter_template letter_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.letter_template
    ADD CONSTRAINT letter_template_pkey PRIMARY KEY (id);


--
-- Name: letter_template letter_template_template_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.letter_template
    ADD CONSTRAINT letter_template_template_no_key UNIQUE (template_no);


--
-- Name: idx_letter_application_applicant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_letter_application_applicant ON public.letter_application USING btree (applicant_id);


--
-- Name: idx_letter_application_approval_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_letter_application_approval_id ON public.letter_application USING btree (approval_id);


--
-- Name: idx_letter_application_assigned_approver; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_letter_application_assigned_approver ON public.letter_application USING btree (assigned_approver_id);


--
-- Name: idx_letter_application_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_letter_application_matter ON public.letter_application USING btree (matter_id);


--
-- Name: idx_letter_application_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_letter_application_no ON public.letter_application USING btree (application_no);


--
-- Name: idx_letter_application_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_letter_application_status ON public.letter_application USING btree (status);


--
-- PostgreSQL database dump complete
--

