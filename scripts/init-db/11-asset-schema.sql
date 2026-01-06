--
-- PostgreSQL database dump
--

\restrict hB7cMMVxcOsE3W30gw9LWf0IkDMToT4ARrM2Tk2VOVjycPyFY7cxSf3xqlmoHIp

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
-- Name: asset_inventory id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_inventory ALTER COLUMN id SET DEFAULT nextval('public.asset_inventory_id_seq'::regclass);


--
-- Name: asset_inventory_detail id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_inventory_detail ALTER COLUMN id SET DEFAULT nextval('public.asset_inventory_detail_id_seq'::regclass);


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
-- Name: idx_asset_inventory_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_inventory_date ON public.asset_inventory USING btree (inventory_date);


--
-- Name: idx_asset_inventory_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_inventory_status ON public.asset_inventory USING btree (status);


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
-- Name: idx_inventory_detail_asset; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_detail_asset ON public.asset_inventory_detail USING btree (asset_id);


--
-- Name: idx_inventory_detail_inventory; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_detail_inventory ON public.asset_inventory_detail USING btree (inventory_id);


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


--
-- PostgreSQL database dump complete
--

\unrestrict hB7cMMVxcOsE3W30gw9LWf0IkDMToT4ARrM2Tk2VOVjycPyFY7cxSf3xqlmoHIp

