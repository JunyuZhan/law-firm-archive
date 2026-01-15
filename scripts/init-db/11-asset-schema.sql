-- =====================================================
-- 律师事务所管理系统 - 资产盘点模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 资产盘点、盘点明细等资产扩展表
-- =====================================================

--
-- Name: asset_inventory; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asset_inventory (
    id bigint NOT NULL,
    inventory_no character varying(50) NOT NULL,
    inventory_date date NOT NULL,
    inventory_type character varying(20) NOT NULL,
    department_id bigint,
    location character varying(200),
    status character varying(20) DEFAULT 'IN_PROGRESS'::character varying,
    total_count integer DEFAULT 0,
    actual_count integer DEFAULT 0,
    surplus_count integer DEFAULT 0,
    shortage_count integer DEFAULT 0,
    remark character varying(500),
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);
--
-- Name: TABLE asset_inventory; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.asset_inventory IS '资产盘点表';
--
-- Name: COLUMN asset_inventory.inventory_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.asset_inventory.inventory_type IS '盘点类型: FULL全盘/PARTIAL抽盘';
--
-- Name: COLUMN asset_inventory.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.asset_inventory.status IS '状态: IN_PROGRESS进行中/COMPLETED已完成';
--
-- Name: asset_inventory_detail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asset_inventory_detail (
    id bigint NOT NULL,
    inventory_id bigint NOT NULL,
    asset_id bigint NOT NULL,
    expected_status character varying(20),
    actual_status character varying(20),
    expected_location character varying(200),
    actual_location character varying(200),
    expected_user_id bigint,
    actual_user_id bigint,
    discrepancy_type character varying(20),
    discrepancy_desc character varying(500),
    remark character varying(500),
    version integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);
--
-- Name: TABLE asset_inventory_detail; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.asset_inventory_detail IS '资产盘点明细表';
--
-- Name: COLUMN asset_inventory_detail.discrepancy_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.asset_inventory_detail.discrepancy_type IS '差异类型: NORMAL正常/SURPLUS盘盈/SHORTAGE盘亏/LOCATION位置不符/STATUS状态不符';
--
-- Name: asset_inventory_detail_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.asset_inventory_detail_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: asset_inventory_detail_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.asset_inventory_detail_id_seq OWNED BY public.asset_inventory_detail.id;
--
-- Name: asset_inventory_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.asset_inventory_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: asset_inventory_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.asset_inventory_id_seq OWNED BY public.asset_inventory.id;
--
-- Name: asset_inventory id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_inventory ALTER COLUMN id SET DEFAULT nextval('public.asset_inventory_id_seq'::regclass);
--
-- Name: asset_inventory_detail id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_inventory_detail ALTER COLUMN id SET DEFAULT nextval('public.asset_inventory_detail_id_seq'::regclass);
--
-- Name: asset_inventory_detail asset_inventory_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_inventory_detail
    ADD CONSTRAINT asset_inventory_detail_pkey PRIMARY KEY (id);
--
-- Name: asset_inventory asset_inventory_inventory_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_inventory
    ADD CONSTRAINT asset_inventory_inventory_no_key UNIQUE (inventory_no);
--
-- Name: asset_inventory asset_inventory_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_inventory
    ADD CONSTRAINT asset_inventory_pkey PRIMARY KEY (id);
--
-- Name: idx_asset_inventory_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_inventory_date ON public.asset_inventory USING btree (inventory_date);
--
-- Name: idx_asset_inventory_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_inventory_status ON public.asset_inventory USING btree (status);
--
-- Name: idx_inventory_detail_asset; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_detail_asset ON public.asset_inventory_detail USING btree (asset_id);
--
-- Name: idx_inventory_detail_inventory; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_detail_inventory ON public.asset_inventory_detail USING btree (inventory_id);
