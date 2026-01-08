--
-- PostgreSQL database dump
--

\restrict cKSUodcA4otatYEdk1WWcqVpE1tIfjuPJvBkXEqWXeQufvov8NlYuwYzJYy1YOc

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

--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


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
-- Name: archive; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive (
    id bigint NOT NULL,
    archive_no character varying(50) NOT NULL,
    matter_id bigint,
    archive_name character varying(200) NOT NULL,
    archive_type character varying(20) NOT NULL,
    matter_no character varying(50),
    matter_name character varying(200),
    client_name character varying(200),
    main_lawyer_name character varying(50),
    case_close_date date,
    volume_count integer DEFAULT 1,
    page_count integer DEFAULT 0,
    catalog text,
    location_id bigint,
    box_no character varying(50),
    retention_period character varying(20) DEFAULT '10_YEARS'::character varying,
    retention_expire_date date,
    has_electronic boolean DEFAULT false,
    electronic_url character varying(500),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    stored_by bigint,
    stored_at timestamp without time zone,
    destroy_date date,
    destroy_reason text,
    destroy_approver_id bigint,
    remarks text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false,
    migrate_date date,
    migrate_reason text,
    migrate_approver_id bigint,
    migrate_target text,
    files_deleted boolean DEFAULT false,
    archive_snapshot text,
    electronic_package_path text
);


--
-- Name: TABLE archive; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive IS '档案表';


--
-- Name: COLUMN archive.archive_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive.archive_no IS '档案号';


--
-- Name: COLUMN archive.archive_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive.archive_type IS '档案类型：LITIGATION-诉讼, NON_LITIGATION-非诉, CONSULTATION-咨询';


--
-- Name: COLUMN archive.retention_period; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive.retention_period IS '保管期限：PERMANENT-永久, 30_YEARS-30年, 15_YEARS-15年, 10_YEARS-10年, 5_YEARS-5年';


--
-- Name: COLUMN archive.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive.status IS '状态：PENDING-待入库, STORED-已入库, BORROWED-借出, DESTROYED-已销毁';


--
-- Name: archive_borrow; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_borrow (
    id bigint NOT NULL,
    borrow_no character varying(50) NOT NULL,
    archive_id bigint NOT NULL,
    borrower_id bigint NOT NULL,
    borrower_name character varying(50) NOT NULL,
    department character varying(100),
    borrow_reason text,
    borrow_date date NOT NULL,
    expected_return_date date NOT NULL,
    actual_return_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approver_id bigint,
    approved_at timestamp without time zone,
    rejection_reason text,
    return_handler_id bigint,
    return_condition character varying(20),
    return_remarks text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE archive_borrow; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive_borrow IS '档案借阅表';


--
-- Name: COLUMN archive_borrow.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_borrow.status IS '状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, BORROWED-借出中, RETURNED-已归还, OVERDUE-逾期';


--
-- Name: COLUMN archive_borrow.return_condition; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_borrow.return_condition IS '归还状态：GOOD-完好, DAMAGED-损坏, LOST-遗失';


--
-- Name: archive_borrow_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_borrow_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: archive_borrow_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_borrow_id_seq OWNED BY public.archive_borrow.id;


--
-- Name: archive_data_source; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_data_source (
    id bigint NOT NULL,
    source_name character varying(100) NOT NULL,
    source_table character varying(100) NOT NULL,
    source_type character varying(50) NOT NULL,
    dossier_folder character varying(200),
    is_enabled boolean DEFAULT true,
    is_required boolean DEFAULT false,
    sort_order integer DEFAULT 0,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: archive_data_source_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_data_source_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: archive_data_source_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_data_source_id_seq OWNED BY public.archive_data_source.id;


--
-- Name: archive_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: archive_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_id_seq OWNED BY public.archive.id;


--
-- Name: archive_location; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_location (
    id bigint NOT NULL,
    location_code character varying(50) NOT NULL,
    location_name character varying(100) NOT NULL,
    room character varying(50),
    cabinet character varying(50),
    shelf character varying(50),
    "position" character varying(50),
    total_capacity integer DEFAULT 0,
    used_capacity integer DEFAULT 0,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying,
    remarks text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE archive_location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive_location IS '档案库位表';


--
-- Name: COLUMN archive_location.location_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_location.location_code IS '库位编码';


--
-- Name: COLUMN archive_location.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_location.status IS '状态：AVAILABLE-可用, FULL-已满, MAINTENANCE-维护中';


--
-- Name: archive_location_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_location_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: archive_location_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_location_id_seq OWNED BY public.archive_location.id;


--
-- Name: archive_operation_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_operation_log (
    id bigint NOT NULL,
    archive_id bigint NOT NULL,
    operation_type character varying(20) NOT NULL,
    operation_description text,
    operator_id bigint NOT NULL,
    operated_at timestamp without time zone NOT NULL,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE archive_operation_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive_operation_log IS '档案操作日志表';


--
-- Name: COLUMN archive_operation_log.operation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_operation_log.operation_type IS '操作类型：STORE-入库, BORROW-借出, RETURN-归还, TRANSFER-转移, DESTROY-销毁';


--
-- Name: archive_operation_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_operation_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: archive_operation_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_operation_log_id_seq OWNED BY public.archive_operation_log.id;


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
-- Name: attendance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attendance (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    attendance_date date NOT NULL,
    check_in_time timestamp without time zone,
    check_out_time timestamp without time zone,
    check_in_location character varying(200),
    check_out_location character varying(200),
    check_in_device character varying(100),
    check_out_device character varying(100),
    status character varying(20) DEFAULT 'NORMAL'::character varying,
    work_hours numeric(4,2),
    overtime_hours numeric(4,2) DEFAULT 0,
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE attendance; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.attendance IS '考勤记录表';


--
-- Name: COLUMN attendance.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.attendance.status IS '状态: NORMAL正常/LATE迟到/EARLY早退/ABSENT缺勤/LEAVE请假';


--
-- Name: attendance_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.attendance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: attendance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.attendance_id_seq OWNED BY public.attendance.id;


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
-- Name: contract_participant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contract_participant (
    id bigint NOT NULL,
    contract_id bigint NOT NULL,
    user_id bigint NOT NULL,
    role character varying(20) NOT NULL,
    commission_rate numeric(5,2),
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE contract_participant; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.contract_participant IS '合同参与人表';


--
-- Name: COLUMN contract_participant.contract_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_participant.contract_id IS '合同ID';


--
-- Name: COLUMN contract_participant.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_participant.user_id IS '用户ID';


--
-- Name: COLUMN contract_participant.role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_participant.role IS '角色：LEAD-承办律师, CO_COUNSEL-协办律师, ORIGINATOR-案源人, PARALEGAL-律师助理';


--
-- Name: COLUMN contract_participant.commission_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_participant.commission_rate IS '提成比例（百分比）';


--
-- Name: contract_participant_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contract_participant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: contract_participant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contract_participant_id_seq OWNED BY public.contract_participant.id;


--
-- Name: contract_payment_schedule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contract_payment_schedule (
    id bigint NOT NULL,
    contract_id bigint NOT NULL,
    phase_name character varying(100) NOT NULL,
    amount numeric(15,2) NOT NULL,
    percentage numeric(5,2),
    planned_date date,
    actual_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE contract_payment_schedule; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.contract_payment_schedule IS '合同付款计划表';


--
-- Name: COLUMN contract_payment_schedule.contract_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.contract_id IS '合同ID';


--
-- Name: COLUMN contract_payment_schedule.phase_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.phase_name IS '阶段名称（如：签约款、一审结束、执行到位）';


--
-- Name: COLUMN contract_payment_schedule.amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.amount IS '付款金额';


--
-- Name: COLUMN contract_payment_schedule.percentage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.percentage IS '比例（风险代理时使用）';


--
-- Name: COLUMN contract_payment_schedule.planned_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.planned_date IS '计划收款日期';


--
-- Name: COLUMN contract_payment_schedule.actual_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.actual_date IS '实际收款日期';


--
-- Name: COLUMN contract_payment_schedule.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_payment_schedule.status IS '状态：PENDING-待收, PARTIAL-部分收款, PAID-已收清, CANCELLED-已取消';


--
-- Name: contract_payment_schedule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contract_payment_schedule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: contract_payment_schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contract_payment_schedule_id_seq OWNED BY public.contract_payment_schedule.id;


--
-- Name: contract_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contract_template (
    id bigint NOT NULL,
    template_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    contract_type character varying(20) NOT NULL,
    fee_type character varying(20),
    content text,
    clauses text,
    description character varying(500),
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE contract_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.contract_template IS '合同模板表';


--
-- Name: COLUMN contract_template.template_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.template_no IS '模板编号';


--
-- Name: COLUMN contract_template.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.name IS '模板名称';


--
-- Name: COLUMN contract_template.contract_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.contract_type IS '合同类型：SERVICE-服务合同, RETAINER-常年法顾, LITIGATION-诉讼代理, NON_LITIGATION-非诉项目';


--
-- Name: COLUMN contract_template.fee_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.fee_type IS '默认收费方式';


--
-- Name: COLUMN contract_template.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.content IS '模板内容（支持变量替换）';


--
-- Name: COLUMN contract_template.clauses; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.clauses IS '标准条款（JSON格式）';


--
-- Name: COLUMN contract_template.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.description IS '模板说明';


--
-- Name: COLUMN contract_template.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.contract_template.status IS '状态：ACTIVE-启用, INACTIVE-停用';


--
-- Name: contract_template_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contract_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: contract_template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contract_template_id_seq OWNED BY public.contract_template.id;


--
-- Name: crm_client; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_client (
    id bigint NOT NULL,
    client_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    client_type character varying(20) NOT NULL,
    credit_code character varying(50),
    id_card character varying(50),
    legal_representative character varying(100),
    registered_address character varying(500),
    contact_person character varying(100),
    contact_phone character varying(20),
    contact_email character varying(100),
    industry character varying(100),
    source character varying(50),
    level character varying(20) DEFAULT 'B'::character varying,
    category character varying(20) DEFAULT 'NORMAL'::character varying,
    status character varying(20) DEFAULT 'POTENTIAL'::character varying,
    originator_id bigint,
    responsible_lawyer_id bigint,
    first_cooperation_date date,
    invoice_title character varying(200),
    invoice_tax_no character varying(50),
    invoice_address character varying(500),
    invoice_phone character varying(50),
    invoice_bank_name character varying(200),
    invoice_bank_account character varying(50),
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE crm_client; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_client IS '客户表';


--
-- Name: COLUMN crm_client.client_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.client_no IS '客户编号';


--
-- Name: COLUMN crm_client.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.name IS '客户名称';


--
-- Name: COLUMN crm_client.client_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.client_type IS '客户类型：INDIVIDUAL-个人, ENTERPRISE-企业, GOVERNMENT-政府机关, OTHER-其他';


--
-- Name: COLUMN crm_client.credit_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.credit_code IS '统一社会信用代码（企业）';


--
-- Name: COLUMN crm_client.id_card; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.id_card IS '身份证号（个人）';


--
-- Name: COLUMN crm_client.level; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.level IS '客户级别：A-重要, B-普通, C-一般';


--
-- Name: COLUMN crm_client.category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.category IS '客户分类: VIP-重要客户, NORMAL-普通客户, POTENTIAL-潜在客户';


--
-- Name: COLUMN crm_client.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.status IS '状态：POTENTIAL-潜在, ACTIVE-正式, INACTIVE-休眠, BLACKLIST-黑名单';


--
-- Name: COLUMN crm_client.invoice_title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.invoice_title IS '发票抬头';


--
-- Name: COLUMN crm_client.invoice_tax_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.invoice_tax_no IS '纳税人识别号';


--
-- Name: COLUMN crm_client.invoice_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.invoice_address IS '开票地址';


--
-- Name: COLUMN crm_client.invoice_phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.invoice_phone IS '开票电话';


--
-- Name: COLUMN crm_client.invoice_bank_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.invoice_bank_name IS '开户银行';


--
-- Name: COLUMN crm_client.invoice_bank_account; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client.invoice_bank_account IS '银行账号';


--
-- Name: crm_client_change_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_client_change_history (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    change_type character varying(50) NOT NULL,
    change_date date NOT NULL,
    before_value text,
    after_value text,
    change_description text,
    registration_authority character varying(200),
    registration_number character varying(100),
    attachment_url character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE crm_client_change_history; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_client_change_history IS '企业变更历史记录表';


--
-- Name: COLUMN crm_client_change_history.change_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_change_history.change_type IS '变更类型：NAME-名称, REGISTERED_CAPITAL-注册资本, LEGAL_REPRESENTATIVE-法定代表人, ADDRESS-地址, BUSINESS_SCOPE-经营范围, SHAREHOLDER-股东, OTHER-其他';


--
-- Name: crm_client_change_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_client_change_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_client_change_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_client_change_history_id_seq OWNED BY public.crm_client_change_history.id;


--
-- Name: crm_client_contact_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_client_contact_record (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    contact_id bigint,
    contact_person character varying(100),
    contact_method character varying(20) NOT NULL,
    contact_date timestamp without time zone NOT NULL,
    contact_duration integer,
    contact_location character varying(200),
    contact_content text,
    contact_result character varying(500),
    next_follow_up_date date,
    follow_up_reminder boolean DEFAULT false,
    created_by bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE crm_client_contact_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_client_contact_record IS '客户联系记录表';


--
-- Name: COLUMN crm_client_contact_record.client_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.client_id IS '客户ID';


--
-- Name: COLUMN crm_client_contact_record.contact_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.contact_id IS '联系人ID';


--
-- Name: COLUMN crm_client_contact_record.contact_person; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.contact_person IS '联系人姓名';


--
-- Name: COLUMN crm_client_contact_record.contact_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.contact_method IS '联系方式：PHONE-电话, EMAIL-邮件, MEETING-会面, VISIT-拜访, OTHER-其他';


--
-- Name: COLUMN crm_client_contact_record.contact_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.contact_date IS '联系时间';


--
-- Name: COLUMN crm_client_contact_record.contact_duration; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.contact_duration IS '联系时长（分钟）';


--
-- Name: COLUMN crm_client_contact_record.contact_location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.contact_location IS '联系地点';


--
-- Name: COLUMN crm_client_contact_record.contact_content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.contact_content IS '联系内容';


--
-- Name: COLUMN crm_client_contact_record.contact_result; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.contact_result IS '联系结果';


--
-- Name: COLUMN crm_client_contact_record.next_follow_up_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.next_follow_up_date IS '下次跟进日期';


--
-- Name: COLUMN crm_client_contact_record.follow_up_reminder; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.follow_up_reminder IS '是否设置提醒';


--
-- Name: COLUMN crm_client_contact_record.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_contact_record.created_by IS '记录人';


--
-- Name: crm_client_contact_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_client_contact_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_client_contact_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_client_contact_record_id_seq OWNED BY public.crm_client_contact_record.id;


--
-- Name: crm_client_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_client_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_client_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_client_id_seq OWNED BY public.crm_client.id;


--
-- Name: crm_client_related_company; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_client_related_company (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    related_company_name character varying(200) NOT NULL,
    related_company_type character varying(20) NOT NULL,
    credit_code character varying(50),
    registered_address character varying(500),
    legal_representative character varying(100),
    relationship_description character varying(500),
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE crm_client_related_company; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_client_related_company IS '客户关联企业表';


--
-- Name: COLUMN crm_client_related_company.client_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_related_company.client_id IS '客户ID';


--
-- Name: COLUMN crm_client_related_company.related_company_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_related_company.related_company_name IS '关联企业名称';


--
-- Name: COLUMN crm_client_related_company.related_company_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_related_company.related_company_type IS '关联类型：PARENT-母公司, SUBSIDIARY-子公司, AFFILIATE-关联公司';


--
-- Name: COLUMN crm_client_related_company.credit_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_related_company.credit_code IS '统一社会信用代码';


--
-- Name: COLUMN crm_client_related_company.registered_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_related_company.registered_address IS '注册地址';


--
-- Name: COLUMN crm_client_related_company.legal_representative; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_related_company.legal_representative IS '法定代表人';


--
-- Name: COLUMN crm_client_related_company.relationship_description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_related_company.relationship_description IS '关联关系描述';


--
-- Name: crm_client_related_company_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_client_related_company_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_client_related_company_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_client_related_company_id_seq OWNED BY public.crm_client_related_company.id;


--
-- Name: crm_client_shareholder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_client_shareholder (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    shareholder_name character varying(100) NOT NULL,
    shareholder_type character varying(20),
    id_card character varying(50),
    credit_code character varying(50),
    shareholding_ratio numeric(5,2),
    investment_amount numeric(15,2),
    investment_date date,
    "position" character varying(50),
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE crm_client_shareholder; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_client_shareholder IS '客户股东信息表';


--
-- Name: COLUMN crm_client_shareholder.client_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_shareholder.client_id IS '客户ID';


--
-- Name: COLUMN crm_client_shareholder.shareholder_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_shareholder.shareholder_name IS '股东名称';


--
-- Name: COLUMN crm_client_shareholder.shareholder_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_shareholder.shareholder_type IS '股东类型：INDIVIDUAL-个人, ENTERPRISE-企业';


--
-- Name: COLUMN crm_client_shareholder.id_card; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_shareholder.id_card IS '个人股东身份证号';


--
-- Name: COLUMN crm_client_shareholder.credit_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_shareholder.credit_code IS '企业股东统一社会信用代码';


--
-- Name: COLUMN crm_client_shareholder.shareholding_ratio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_shareholder.shareholding_ratio IS '持股比例（百分比）';


--
-- Name: COLUMN crm_client_shareholder.investment_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_shareholder.investment_amount IS '投资金额';


--
-- Name: COLUMN crm_client_shareholder.investment_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_shareholder.investment_date IS '投资日期';


--
-- Name: COLUMN crm_client_shareholder."position"; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_shareholder."position" IS '职务';


--
-- Name: crm_client_shareholder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_client_shareholder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_client_shareholder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_client_shareholder_id_seq OWNED BY public.crm_client_shareholder.id;


--
-- Name: crm_client_tag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_client_tag (
    id bigint NOT NULL,
    tag_name character varying(50) NOT NULL,
    tag_color character varying(20) DEFAULT '#1890ff'::character varying,
    description character varying(200),
    sort_order integer DEFAULT 0,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE crm_client_tag; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_client_tag IS '客户标签表';


--
-- Name: COLUMN crm_client_tag.tag_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_tag.tag_name IS '标签名称';


--
-- Name: COLUMN crm_client_tag.tag_color; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_tag.tag_color IS '标签颜色（十六进制）';


--
-- Name: COLUMN crm_client_tag.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_tag.description IS '标签描述';


--
-- Name: COLUMN crm_client_tag.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_tag.sort_order IS '排序顺序';


--
-- Name: crm_client_tag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_client_tag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_client_tag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_client_tag_id_seq OWNED BY public.crm_client_tag.id;


--
-- Name: crm_client_tag_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_client_tag_relation (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    tag_id bigint NOT NULL,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE crm_client_tag_relation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_client_tag_relation IS '客户标签关联表';


--
-- Name: COLUMN crm_client_tag_relation.client_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_tag_relation.client_id IS '客户ID';


--
-- Name: COLUMN crm_client_tag_relation.tag_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_client_tag_relation.tag_id IS '标签ID';


--
-- Name: crm_client_tag_relation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_client_tag_relation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_client_tag_relation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_client_tag_relation_id_seq OWNED BY public.crm_client_tag_relation.id;


--
-- Name: crm_conflict_check; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_conflict_check (
    id bigint NOT NULL,
    check_no character varying(50) NOT NULL,
    check_type character varying(20) NOT NULL,
    client_id bigint,
    matter_id bigint,
    client_name character varying(200),
    opposing_party character varying(200),
    related_parties text,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    result_description text,
    conflict_details text,
    applicant_id bigint,
    reviewer_id bigint,
    reviewed_at timestamp without time zone,
    review_comment text,
    remark text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE crm_conflict_check; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_conflict_check IS '利冲检查表';


--
-- Name: COLUMN crm_conflict_check.check_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_conflict_check.check_no IS '检查编号';


--
-- Name: COLUMN crm_conflict_check.check_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_conflict_check.check_type IS '检查类型：NEW_CLIENT-新客户, NEW_MATTER-新案件, MANUAL-手动检查';


--
-- Name: COLUMN crm_conflict_check.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_conflict_check.status IS '检查状态：PENDING-待检查, CHECKING-检查中, PASSED-通过, CONFLICT-存在冲突, EXEMPTION_PENDING-豁免待审批, WAIVED-已豁免, REJECTED-已拒绝';


--
-- Name: crm_conflict_check_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_conflict_check_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_conflict_check_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_conflict_check_id_seq OWNED BY public.crm_conflict_check.id;


--
-- Name: crm_conflict_check_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_conflict_check_item (
    id bigint NOT NULL,
    check_id bigint NOT NULL,
    party_name character varying(200) NOT NULL,
    party_type character varying(20) NOT NULL,
    id_number character varying(50),
    has_conflict boolean DEFAULT false,
    conflict_detail text,
    related_matter_id bigint,
    related_client_id bigint,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE crm_conflict_check_item; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_conflict_check_item IS '利冲检查项表';


--
-- Name: COLUMN crm_conflict_check_item.party_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_conflict_check_item.party_type IS '当事人类型：CLIENT-委托人, OPPOSING-对方当事人, RELATED-关联方';


--
-- Name: crm_conflict_check_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_conflict_check_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_conflict_check_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_conflict_check_item_id_seq OWNED BY public.crm_conflict_check_item.id;


--
-- Name: crm_lead; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_lead (
    id bigint NOT NULL,
    lead_no character varying(50) NOT NULL,
    lead_name character varying(200) NOT NULL,
    lead_type character varying(20),
    contact_name character varying(50),
    contact_phone character varying(20),
    contact_email character varying(100),
    source_channel character varying(50),
    source_detail character varying(500),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    priority character varying(20) DEFAULT 'NORMAL'::character varying,
    business_type character varying(50),
    estimated_amount numeric(15,2),
    description text,
    last_follow_time timestamp without time zone,
    next_follow_time timestamp without time zone,
    follow_count integer DEFAULT 0,
    converted_at timestamp without time zone,
    converted_to_client_id bigint,
    converted_to_matter_id bigint,
    originator_id bigint,
    responsible_user_id bigint,
    remark text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE crm_lead; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_lead IS '案源线索表';


--
-- Name: COLUMN crm_lead.lead_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead.lead_no IS '案源编号';


--
-- Name: COLUMN crm_lead.lead_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead.lead_name IS '案源名称';


--
-- Name: COLUMN crm_lead.lead_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead.lead_type IS '案源类型：INDIVIDUAL-个人, ENTERPRISE-企业';


--
-- Name: COLUMN crm_lead.source_channel; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead.source_channel IS '来源渠道';


--
-- Name: COLUMN crm_lead.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead.status IS '状态：PENDING-待跟进, FOLLOWING-跟进中, CONVERTED-已转化, ABANDONED-已放弃';


--
-- Name: COLUMN crm_lead.priority; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead.priority IS '优先级：HIGH-高, NORMAL-中, LOW-低';


--
-- Name: COLUMN crm_lead.converted_to_client_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead.converted_to_client_id IS '转化后的客户ID';


--
-- Name: COLUMN crm_lead.converted_to_matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead.converted_to_matter_id IS '转化后的项目ID';


--
-- Name: crm_lead_follow_up; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_lead_follow_up (
    id bigint NOT NULL,
    lead_id bigint NOT NULL,
    follow_type character varying(20),
    follow_content text,
    follow_result character varying(20),
    next_follow_time timestamp without time zone,
    next_follow_plan text,
    follow_user_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint
);


--
-- Name: TABLE crm_lead_follow_up; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.crm_lead_follow_up IS '案源跟进记录表';


--
-- Name: COLUMN crm_lead_follow_up.lead_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead_follow_up.lead_id IS '案源ID';


--
-- Name: COLUMN crm_lead_follow_up.follow_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead_follow_up.follow_type IS '跟进方式：PHONE-电话, EMAIL-邮件, VISIT-拜访, MEETING-会议, OTHER-其他';


--
-- Name: COLUMN crm_lead_follow_up.follow_result; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.crm_lead_follow_up.follow_result IS '跟进结果：POSITIVE-积极, NEUTRAL-中性, NEGATIVE-消极';


--
-- Name: crm_lead_follow_up_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_lead_follow_up_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_lead_follow_up_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_lead_follow_up_id_seq OWNED BY public.crm_lead_follow_up.id;


--
-- Name: crm_lead_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.crm_lead_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: crm_lead_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.crm_lead_id_seq OWNED BY public.crm_lead.id;


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
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    updated_by bigint,
    file_category character varying(50) DEFAULT 'OTHER'::character varying,
    folder_path character varying(500),
    dossier_item_id bigint,
    ai_generated boolean DEFAULT false
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
-- Name: dossier_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dossier_template (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    case_type character varying(50) NOT NULL,
    description character varying(500),
    is_default boolean DEFAULT false,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
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
-- Name: evidence; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evidence (
    id bigint NOT NULL,
    evidence_no character varying(50) NOT NULL,
    matter_id bigint NOT NULL,
    name character varying(500) NOT NULL,
    evidence_type character varying(50) NOT NULL,
    source character varying(200),
    group_name character varying(100),
    sort_order integer DEFAULT 0,
    prove_purpose text,
    description text,
    is_original boolean DEFAULT false,
    original_count integer DEFAULT 0,
    copy_count integer DEFAULT 0,
    page_start integer,
    page_end integer,
    file_url character varying(1000),
    file_name character varying(500),
    file_size bigint,
    cross_exam_status character varying(20) DEFAULT 'PENDING'::character varying,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    file_type character varying(50),
    thumbnail_url character varying(1000),
    document_id bigint
);


--
-- Name: TABLE evidence; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.evidence IS '证据表';


--
-- Name: COLUMN evidence.evidence_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.evidence_type IS '证据类型: DOCUMENTARY-书证, PHYSICAL-物证, AUDIO_VISUAL-视听资料, ELECTRONIC-电子数据, WITNESS-证人证言, EXPERT-鉴定意见, INSPECTION-勘验笔录';


--
-- Name: COLUMN evidence.cross_exam_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.cross_exam_status IS '质证状态: PENDING-待质证, IN_PROGRESS-质证中, COMPLETED-已质证';


--
-- Name: COLUMN evidence.file_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.file_type IS '文件类型分类: image, pdf, word, excel, ppt, audio, video, other';


--
-- Name: COLUMN evidence.thumbnail_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.thumbnail_url IS '缩略图URL（仅图片文件）';


--
-- Name: COLUMN evidence.document_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence.document_id IS '关联卷宗文件ID，引用 doc_document.id';


--
-- Name: evidence_cross_exam; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evidence_cross_exam (
    id bigint NOT NULL,
    evidence_id bigint NOT NULL,
    exam_party character varying(50) NOT NULL,
    authenticity_opinion character varying(20),
    authenticity_reason text,
    legality_opinion character varying(20),
    legality_reason text,
    relevance_opinion character varying(20),
    relevance_reason text,
    overall_opinion text,
    court_opinion text,
    court_accepted boolean,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE evidence_cross_exam; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.evidence_cross_exam IS '质证记录表';


--
-- Name: COLUMN evidence_cross_exam.exam_party; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence_cross_exam.exam_party IS '质证方: OUR_SIDE-我方, OPPOSITE-对方, COURT-法院';


--
-- Name: COLUMN evidence_cross_exam.authenticity_opinion; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence_cross_exam.authenticity_opinion IS '真实性意见: ACCEPT-认可, PARTIAL-部分认可, REJECT-不认可';


--
-- Name: evidence_cross_exam_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.evidence_cross_exam_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: evidence_cross_exam_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.evidence_cross_exam_id_seq OWNED BY public.evidence_cross_exam.id;


--
-- Name: evidence_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.evidence_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: evidence_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.evidence_id_seq OWNED BY public.evidence.id;


--
-- Name: evidence_list; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evidence_list (
    id bigint NOT NULL,
    list_no character varying(50) NOT NULL,
    matter_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    list_type character varying(50),
    evidence_ids jsonb,
    file_url character varying(1000),
    file_name character varying(500),
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE evidence_list; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.evidence_list IS '证据清单表';


--
-- Name: COLUMN evidence_list.list_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evidence_list.list_type IS '清单类型: SUBMISSION-提交清单, EXCHANGE-交换清单, COURT-庭审清单';


--
-- Name: evidence_list_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.evidence_list_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: evidence_list_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.evidence_list_id_seq OWNED BY public.evidence_list.id;


--
-- Name: fin_payment_amendment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fin_payment_amendment (
    id bigint NOT NULL,
    payment_id bigint NOT NULL,
    original_amount numeric(15,2) NOT NULL,
    new_amount numeric(15,2) NOT NULL,
    reason text NOT NULL,
    requested_by bigint NOT NULL,
    requested_at timestamp without time zone NOT NULL,
    approved_by bigint,
    approved_at timestamp without time zone,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    reject_reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE fin_payment_amendment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.fin_payment_amendment IS '收款变更申请表';


--
-- Name: COLUMN fin_payment_amendment.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.id IS '主键ID';


--
-- Name: COLUMN fin_payment_amendment.payment_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.payment_id IS '关联收款记录ID';


--
-- Name: COLUMN fin_payment_amendment.original_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.original_amount IS '原金额';


--
-- Name: COLUMN fin_payment_amendment.new_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.new_amount IS '新金额';


--
-- Name: COLUMN fin_payment_amendment.reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.reason IS '变更原因';


--
-- Name: COLUMN fin_payment_amendment.requested_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.requested_by IS '申请人ID';


--
-- Name: COLUMN fin_payment_amendment.requested_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.requested_at IS '申请时间';


--
-- Name: COLUMN fin_payment_amendment.approved_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.approved_by IS '审批人ID';


--
-- Name: COLUMN fin_payment_amendment.approved_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.approved_at IS '审批时间';


--
-- Name: COLUMN fin_payment_amendment.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.status IS '状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝';


--
-- Name: COLUMN fin_payment_amendment.reject_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fin_payment_amendment.reject_reason IS '拒绝原因';


--
-- Name: fin_payment_amendment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fin_payment_amendment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fin_payment_amendment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fin_payment_amendment_id_seq OWNED BY public.fin_payment_amendment.id;


--
-- Name: finance_commission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_commission (
    id bigint NOT NULL,
    commission_no character varying(50) NOT NULL,
    payment_id bigint NOT NULL,
    fee_id bigint,
    contract_id bigint,
    matter_id bigint,
    client_id bigint,
    rule_id bigint,
    rule_code character varying(50),
    payment_amount numeric(15,2),
    firm_retention numeric(15,2),
    commission_base numeric(15,2),
    tax_amount numeric(15,2),
    management_fee numeric(15,2),
    net_commission numeric(15,2),
    originator_id bigint,
    originator_commission numeric(15,2),
    status character varying(50) DEFAULT 'PENDING'::character varying,
    approved_by bigint,
    approved_at timestamp without time zone,
    paid_by bigint,
    paid_at timestamp without time zone,
    remark text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false,
    gross_amount numeric(15,2),
    cost_amount numeric(15,2),
    net_amount numeric(15,2),
    distribution_ratio numeric(5,2),
    commission_rate numeric(5,2),
    commission_amount numeric(15,2),
    compensation_type character varying(20)
);


--
-- Name: TABLE finance_commission; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_commission IS '提成记录表';


--
-- Name: COLUMN finance_commission.commission_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.commission_no IS '提成编号';


--
-- Name: COLUMN finance_commission.payment_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.payment_id IS '收款记录ID';


--
-- Name: COLUMN finance_commission.fee_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.fee_id IS '收费记录ID';


--
-- Name: COLUMN finance_commission.contract_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.contract_id IS '合同ID';


--
-- Name: COLUMN finance_commission.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.matter_id IS '案件ID';


--
-- Name: COLUMN finance_commission.client_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.client_id IS '客户ID';


--
-- Name: COLUMN finance_commission.rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.rule_id IS '提成规则ID';


--
-- Name: COLUMN finance_commission.rule_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.rule_code IS '提成规则编码';


--
-- Name: COLUMN finance_commission.payment_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.payment_amount IS '收款金额';


--
-- Name: COLUMN finance_commission.firm_retention; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.firm_retention IS '律所留存';


--
-- Name: COLUMN finance_commission.commission_base; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.commission_base IS '提成基数';


--
-- Name: COLUMN finance_commission.tax_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.tax_amount IS '税费';


--
-- Name: COLUMN finance_commission.management_fee; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.management_fee IS '管理费';


--
-- Name: COLUMN finance_commission.net_commission; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.net_commission IS '净提成';


--
-- Name: COLUMN finance_commission.originator_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.originator_id IS '案源人ID';


--
-- Name: COLUMN finance_commission.originator_commission; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.originator_commission IS '案源提成';


--
-- Name: COLUMN finance_commission.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.status IS '状态：PENDING-待审批, APPROVED-已审批, PAID-已发放, CANCELLED-已取消';


--
-- Name: COLUMN finance_commission.gross_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.gross_amount IS '毛收入';


--
-- Name: COLUMN finance_commission.cost_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.cost_amount IS '成本';


--
-- Name: COLUMN finance_commission.net_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.net_amount IS '净收入';


--
-- Name: COLUMN finance_commission.distribution_ratio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.distribution_ratio IS '分配比例';


--
-- Name: COLUMN finance_commission.commission_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.commission_rate IS '提成比例';


--
-- Name: COLUMN finance_commission.commission_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.commission_amount IS '提成金额';


--
-- Name: COLUMN finance_commission.compensation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission.compensation_type IS '薪酬模式：COMMISSION-提成制, SALARIED-授薪制, HYBRID-混合制';


--
-- Name: finance_commission_detail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_commission_detail (
    id bigint NOT NULL,
    commission_id bigint NOT NULL,
    user_id bigint NOT NULL,
    user_name character varying(100),
    role_in_matter character varying(50),
    allocation_rate numeric(5,2) NOT NULL,
    commission_amount numeric(15,2) NOT NULL,
    tax_amount numeric(15,2),
    net_amount numeric(15,2) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    created_by bigint,
    updated_by bigint
);


--
-- Name: TABLE finance_commission_detail; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_commission_detail IS '提成分配明细表';


--
-- Name: COLUMN finance_commission_detail.commission_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.commission_id IS '提成记录ID';


--
-- Name: COLUMN finance_commission_detail.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.user_id IS '用户ID';


--
-- Name: COLUMN finance_commission_detail.user_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.user_name IS '用户姓名';


--
-- Name: COLUMN finance_commission_detail.role_in_matter; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.role_in_matter IS '案件角色';


--
-- Name: COLUMN finance_commission_detail.allocation_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.allocation_rate IS '分配比例';


--
-- Name: COLUMN finance_commission_detail.commission_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.commission_amount IS '提成金额';


--
-- Name: COLUMN finance_commission_detail.tax_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.tax_amount IS '税费';


--
-- Name: COLUMN finance_commission_detail.net_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_detail.net_amount IS '净提成';


--
-- Name: finance_commission_detail_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_commission_detail_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_commission_detail_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_commission_detail_id_seq OWNED BY public.finance_commission_detail.id;


--
-- Name: finance_commission_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_commission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_commission_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_commission_id_seq OWNED BY public.finance_commission.id;


--
-- Name: finance_commission_rule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_commission_rule (
    id bigint NOT NULL,
    rule_code character varying(50) NOT NULL,
    rule_name character varying(200) NOT NULL,
    rule_type character varying(50),
    firm_rate numeric(5,2) DEFAULT 0,
    lead_lawyer_rate numeric(5,2) DEFAULT 0,
    assist_lawyer_rate numeric(5,2) DEFAULT 0,
    support_staff_rate numeric(5,2) DEFAULT 0,
    allow_modify boolean DEFAULT true,
    description text,
    is_default boolean DEFAULT false,
    active boolean DEFAULT true,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false,
    originator_rate numeric(5,2) DEFAULT 0
);


--
-- Name: TABLE finance_commission_rule; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_commission_rule IS '提成规则表';


--
-- Name: COLUMN finance_commission_rule.rule_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.rule_code IS '规则编码';


--
-- Name: COLUMN finance_commission_rule.rule_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.rule_name IS '规则名称';


--
-- Name: COLUMN finance_commission_rule.rule_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.rule_type IS '规则类型';


--
-- Name: COLUMN finance_commission_rule.firm_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.firm_rate IS '律所比例(%)，0表示不参与分配';


--
-- Name: COLUMN finance_commission_rule.lead_lawyer_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.lead_lawyer_rate IS '主办律师比例(%)，0表示不参与分配';


--
-- Name: COLUMN finance_commission_rule.assist_lawyer_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.assist_lawyer_rate IS '协办律师比例(%)，0表示无协办或不参与分配';


--
-- Name: COLUMN finance_commission_rule.support_staff_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.support_staff_rate IS '辅助人员比例(%)，0表示无辅助或不参与分配';


--
-- Name: COLUMN finance_commission_rule.allow_modify; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.allow_modify IS '律师创建合同时是否允许修改比例';


--
-- Name: COLUMN finance_commission_rule.is_default; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.is_default IS '是否默认规则';


--
-- Name: COLUMN finance_commission_rule.active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.active IS '是否启用';


--
-- Name: COLUMN finance_commission_rule.originator_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_commission_rule.originator_rate IS '案源人比例(%)，0表示不参与分配';


--
-- Name: finance_commission_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_commission_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_commission_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_commission_rule_id_seq OWNED BY public.finance_commission_rule.id;


--
-- Name: finance_contract; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_contract (
    id bigint NOT NULL,
    contract_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    template_id bigint,
    client_id bigint NOT NULL,
    matter_id bigint,
    contract_type character varying(20) NOT NULL,
    fee_type character varying(20) NOT NULL,
    total_amount numeric(15,2) NOT NULL,
    paid_amount numeric(15,2) DEFAULT 0,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    sign_date date,
    effective_date date,
    expiry_date date,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    signer_id bigint,
    department_id bigint,
    content text,
    file_url character varying(500),
    payment_terms text,
    remark text,
    case_type character varying(30),
    cause_of_action character varying(50),
    trial_stage character varying(50),
    claim_amount numeric(15,2),
    jurisdiction_court character varying(200),
    opposing_party character varying(200),
    conflict_check_status character varying(20) DEFAULT 'NOT_REQUIRED'::character varying,
    archive_status character varying(20) DEFAULT 'NOT_ARCHIVED'::character varying,
    advance_travel_fee numeric(15,2),
    risk_ratio numeric(5,2),
    seal_record text,
    commission_rule_id bigint,
    firm_rate numeric(5,2),
    lead_lawyer_rate numeric(5,2),
    assist_lawyer_rate numeric(5,2),
    support_staff_rate numeric(5,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    originator_rate numeric(5,2),
    case_summary text
);


--
-- Name: TABLE finance_contract; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_contract IS '委托合同表';


--
-- Name: COLUMN finance_contract.contract_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.contract_no IS '合同编号';


--
-- Name: COLUMN finance_contract.template_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.template_id IS '使用的模板ID';


--
-- Name: COLUMN finance_contract.contract_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.contract_type IS '合同类型：SERVICE-服务合同, RETAINER-常年法顾, LITIGATION-诉讼代理, NON_LITIGATION-非诉项目';


--
-- Name: COLUMN finance_contract.fee_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.fee_type IS '收费方式：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理, MIXED-混合收费';


--
-- Name: COLUMN finance_contract.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.status IS '合同状态：DRAFT-草稿, PENDING-待审批, ACTIVE-生效中, REJECTED-已拒绝, TERMINATED-已终止, COMPLETED-已完成, EXPIRED-已过期';


--
-- Name: COLUMN finance_contract.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.content IS '合同内容（基于模板生成）';


--
-- Name: COLUMN finance_contract.case_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.case_type IS '案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政等';


--
-- Name: COLUMN finance_contract.cause_of_action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.cause_of_action IS '案由代码';


--
-- Name: COLUMN finance_contract.trial_stage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.trial_stage IS '审理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行, NON_LITIGATION-非诉';


--
-- Name: COLUMN finance_contract.claim_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.claim_amount IS '标的金额';


--
-- Name: COLUMN finance_contract.jurisdiction_court; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.jurisdiction_court IS '管辖法院';


--
-- Name: COLUMN finance_contract.opposing_party; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.opposing_party IS '对方当事人';


--
-- Name: COLUMN finance_contract.conflict_check_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.conflict_check_status IS '利冲审查状态：PENDING-待审查, PASSED-已通过, FAILED-未通过, NOT_REQUIRED-无需审查';


--
-- Name: COLUMN finance_contract.archive_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.archive_status IS '归档状态：NOT_ARCHIVED-未归档, ARCHIVED-已归档, DESTROYED-已销毁';


--
-- Name: COLUMN finance_contract.advance_travel_fee; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.advance_travel_fee IS '预支差旅费';


--
-- Name: COLUMN finance_contract.risk_ratio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.risk_ratio IS '风险代理比例（0-100）';


--
-- Name: COLUMN finance_contract.seal_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.seal_record IS '印章使用记录（JSON格式）';


--
-- Name: COLUMN finance_contract.commission_rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.commission_rule_id IS '提成规则ID（选择的预设方案）';


--
-- Name: COLUMN finance_contract.firm_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.firm_rate IS '律所比例(%)';


--
-- Name: COLUMN finance_contract.lead_lawyer_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.lead_lawyer_rate IS '主办律师比例(%)';


--
-- Name: COLUMN finance_contract.assist_lawyer_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.assist_lawyer_rate IS '协办律师比例(%)';


--
-- Name: COLUMN finance_contract.support_staff_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.support_staff_rate IS '辅助人员比例(%)';


--
-- Name: COLUMN finance_contract.originator_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.originator_rate IS '案源人比例(%)';


--
-- Name: COLUMN finance_contract.case_summary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract.case_summary IS '案情摘要（用于审批表）';


--
-- Name: finance_contract_amendment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_contract_amendment (
    id bigint NOT NULL,
    amendment_no character varying(50) NOT NULL,
    contract_id bigint NOT NULL,
    amendment_type character varying(50) NOT NULL,
    before_snapshot jsonb,
    after_snapshot jsonb,
    amendment_reason text,
    lawyer_amended_by bigint,
    lawyer_amended_at timestamp without time zone,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    finance_handled_by bigint,
    finance_handled_at timestamp without time zone,
    finance_remark text,
    affects_payments boolean DEFAULT false,
    affected_payment_ids jsonb,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE finance_contract_amendment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_contract_amendment IS '财务合同变更记录表';


--
-- Name: COLUMN finance_contract_amendment.amendment_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.amendment_no IS '变更编号';


--
-- Name: COLUMN finance_contract_amendment.contract_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.contract_id IS '合同ID';


--
-- Name: COLUMN finance_contract_amendment.amendment_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.amendment_type IS '变更类型：AMOUNT-金额变更, PARTICIPANT-参与人变更, SCHEDULE-付款计划变更, OTHER-其他';


--
-- Name: COLUMN finance_contract_amendment.before_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.before_snapshot IS '变更前数据快照（JSON格式）';


--
-- Name: COLUMN finance_contract_amendment.after_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.after_snapshot IS '变更后数据快照（JSON格式）';


--
-- Name: COLUMN finance_contract_amendment.amendment_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.amendment_reason IS '变更说明';


--
-- Name: COLUMN finance_contract_amendment.lawyer_amended_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.lawyer_amended_by IS '律师变更人ID';


--
-- Name: COLUMN finance_contract_amendment.lawyer_amended_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.lawyer_amended_at IS '律师变更时间';


--
-- Name: COLUMN finance_contract_amendment.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.status IS '状态：PENDING-待处理, SYNCED-已同步, IGNORED-已忽略, PARTIAL-部分同步';


--
-- Name: COLUMN finance_contract_amendment.finance_handled_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.finance_handled_by IS '财务处理人ID';


--
-- Name: COLUMN finance_contract_amendment.finance_handled_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.finance_handled_at IS '财务处理时间';


--
-- Name: COLUMN finance_contract_amendment.finance_remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.finance_remark IS '财务处理备注';


--
-- Name: COLUMN finance_contract_amendment.affects_payments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.affects_payments IS '是否影响已有收款';


--
-- Name: COLUMN finance_contract_amendment.affected_payment_ids; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_contract_amendment.affected_payment_ids IS '受影响的收款ID列表';


--
-- Name: finance_contract_amendment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_contract_amendment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_contract_amendment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_contract_amendment_id_seq OWNED BY public.finance_contract_amendment.id;


--
-- Name: finance_contract_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_contract_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_contract_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_contract_id_seq OWNED BY public.finance_contract.id;


--
-- Name: finance_cost_allocation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_cost_allocation (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    expense_id bigint NOT NULL,
    allocated_amount numeric(15,2) NOT NULL,
    allocation_date date NOT NULL,
    allocated_by bigint NOT NULL,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE finance_cost_allocation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_cost_allocation IS '成本归集记录表';


--
-- Name: COLUMN finance_cost_allocation.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_allocation.matter_id IS '项目ID';


--
-- Name: COLUMN finance_cost_allocation.expense_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_allocation.expense_id IS '费用ID';


--
-- Name: COLUMN finance_cost_allocation.allocated_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_allocation.allocated_amount IS '归集金额';


--
-- Name: finance_cost_allocation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_cost_allocation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_cost_allocation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_cost_allocation_id_seq OWNED BY public.finance_cost_allocation.id;


--
-- Name: finance_cost_split; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_cost_split (
    id bigint NOT NULL,
    expense_id bigint NOT NULL,
    matter_id bigint NOT NULL,
    split_amount numeric(15,2) NOT NULL,
    split_ratio numeric(5,4),
    split_method character varying(50),
    split_date date NOT NULL,
    split_by bigint NOT NULL,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE finance_cost_split; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_cost_split IS '成本分摊表';


--
-- Name: COLUMN finance_cost_split.expense_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.expense_id IS '费用ID（公共费用）';


--
-- Name: COLUMN finance_cost_split.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.matter_id IS '分摊到的项目ID';


--
-- Name: COLUMN finance_cost_split.split_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.split_amount IS '分摊金额';


--
-- Name: COLUMN finance_cost_split.split_ratio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.split_ratio IS '分摊比例';


--
-- Name: COLUMN finance_cost_split.split_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_cost_split.split_method IS '分摊方式：EQUAL-平均分摊, RATIO-按比例, MANUAL-手动指定';


--
-- Name: finance_cost_split_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_cost_split_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_cost_split_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_cost_split_id_seq OWNED BY public.finance_cost_split.id;


--
-- Name: finance_expense; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_expense (
    id bigint NOT NULL,
    expense_no character varying(50) NOT NULL,
    matter_id bigint,
    applicant_id bigint NOT NULL,
    expense_type character varying(50),
    expense_category character varying(50),
    expense_date date NOT NULL,
    amount numeric(15,2) NOT NULL,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    description text,
    vendor_name character varying(200),
    invoice_no character varying(100),
    invoice_url character varying(500),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approver_id bigint,
    approved_at timestamp without time zone,
    approval_comment text,
    paid_at timestamp without time zone,
    paid_by bigint,
    payment_method character varying(50),
    is_cost_allocation boolean DEFAULT false,
    allocated_to_matter_id bigint,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE finance_expense; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_expense IS '费用报销表';


--
-- Name: COLUMN finance_expense.expense_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.expense_no IS '报销单号';


--
-- Name: COLUMN finance_expense.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.matter_id IS '关联项目ID（可选）';


--
-- Name: COLUMN finance_expense.applicant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.applicant_id IS '申请人ID';


--
-- Name: COLUMN finance_expense.expense_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.expense_type IS '费用类型：TRAVEL-差旅费, MEAL-餐费, ACCOMMODATION-住宿费, TRANSPORT-交通费, MATERIAL-材料费, OTHER-其他';


--
-- Name: COLUMN finance_expense.expense_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.expense_category IS '费用分类：CASE_COST-办案成本, OFFICE_COST-办公费用, OTHER-其他';


--
-- Name: COLUMN finance_expense.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.status IS '状态：PENDING-待审批, APPROVED-已审批, REJECTED-已驳回, PAID-已支付';


--
-- Name: COLUMN finance_expense.is_cost_allocation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_expense.is_cost_allocation IS '是否已归集到项目成本';


--
-- Name: finance_expense_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_expense_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_expense_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_expense_id_seq OWNED BY public.finance_expense.id;


--
-- Name: finance_fee; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_fee (
    id bigint NOT NULL,
    fee_no character varying(50) NOT NULL,
    contract_id bigint NOT NULL,
    matter_id bigint,
    client_id bigint NOT NULL,
    fee_type character varying(20) NOT NULL,
    amount numeric(15,2) NOT NULL,
    paid_amount numeric(15,2) DEFAULT 0,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    planned_date date NOT NULL,
    actual_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    fee_name character varying(200),
    responsible_id bigint
);


--
-- Name: TABLE finance_fee; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_fee IS '收费记录表';


--
-- Name: COLUMN finance_fee.fee_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_fee.fee_type IS '收费类型：RETAINER-预付款, PROGRESS-进度款, FINAL-尾款, OTHER-其他';


--
-- Name: COLUMN finance_fee.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_fee.status IS '状态：PENDING-待收, PARTIAL-部分收款, PAID-已收清, CANCELLED-已取消';


--
-- Name: finance_fee_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_fee_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_fee_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_fee_id_seq OWNED BY public.finance_fee.id;


--
-- Name: finance_invoice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_invoice (
    id bigint NOT NULL,
    invoice_no character varying(50) NOT NULL,
    fee_id bigint,
    contract_id bigint,
    client_id bigint NOT NULL,
    invoice_type character varying(20) NOT NULL,
    title character varying(200) NOT NULL,
    tax_no character varying(50),
    amount numeric(15,2) NOT NULL,
    tax_rate numeric(5,4) DEFAULT 0.06,
    tax_amount numeric(15,2),
    content text,
    invoice_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    applicant_id bigint,
    issuer_id bigint,
    file_url character varying(500),
    remark text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE finance_invoice; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_invoice IS '发票表';


--
-- Name: COLUMN finance_invoice.invoice_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_invoice.invoice_no IS '发票号码';


--
-- Name: COLUMN finance_invoice.invoice_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_invoice.invoice_type IS '发票类型：SPECIAL-增值税专用发票, NORMAL-增值税普通发票, ELECTRONIC-电子发票';


--
-- Name: COLUMN finance_invoice.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_invoice.status IS '状态：PENDING-待开票, ISSUED-已开票, CANCELLED-已作废, RED-已红冲';


--
-- Name: finance_invoice_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_invoice_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_invoice_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_invoice_id_seq OWNED BY public.finance_invoice.id;


--
-- Name: finance_payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_payment (
    id bigint NOT NULL,
    payment_no character varying(50) NOT NULL,
    fee_id bigint NOT NULL,
    contract_id bigint,
    matter_id bigint,
    client_id bigint NOT NULL,
    amount numeric(15,2) NOT NULL,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    payment_date date NOT NULL,
    payment_method character varying(20) NOT NULL,
    payment_account character varying(100),
    receipt_no character varying(100),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    confirmed_at timestamp without time zone,
    confirmed_by bigint,
    remark text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    locked boolean DEFAULT false,
    locked_at timestamp without time zone,
    locked_by bigint
);


--
-- Name: TABLE finance_payment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.finance_payment IS '收款记录表';


--
-- Name: COLUMN finance_payment.payment_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.payment_method IS '付款方式：BANK_TRANSFER-银行转账, CASH-现金, CHECK-支票, ALIPAY-支付宝, WECHAT-微信, OTHER-其他';


--
-- Name: COLUMN finance_payment.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.status IS '状态：PENDING-待确认, CONFIRMED-已确认, CANCELLED-已取消';


--
-- Name: COLUMN finance_payment.locked; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.locked IS '是否锁定（登记后自动锁定）';


--
-- Name: COLUMN finance_payment.locked_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.locked_at IS '锁定时间';


--
-- Name: COLUMN finance_payment.locked_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.finance_payment.locked_by IS '锁定人ID';


--
-- Name: finance_payment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.finance_payment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: finance_payment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.finance_payment_id_seq OWNED BY public.finance_payment.id;


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
-- Name: hourly_rate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hourly_rate (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    rate numeric(10,2) NOT NULL,
    currency character varying(10) DEFAULT 'CNY'::character varying,
    effective_date date NOT NULL,
    expiry_date date,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hourly_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hourly_rate IS '小时费率表';


--
-- Name: hourly_rate_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hourly_rate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hourly_rate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hourly_rate_id_seq OWNED BY public.hourly_rate.id;


--
-- Name: hr_career_level; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_career_level (
    id bigint NOT NULL,
    level_code character varying(50) NOT NULL,
    level_name character varying(100) NOT NULL,
    level_order integer NOT NULL,
    category character varying(50) NOT NULL,
    description character varying(500),
    min_work_years integer,
    min_matter_count integer,
    min_revenue numeric(15,2),
    required_certificates text,
    other_requirements text,
    salary_min numeric(15,2),
    salary_max numeric(15,2),
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_career_level; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_career_level IS '职级通道表';


--
-- Name: hr_career_level_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_career_level_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_career_level_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_career_level_id_seq OWNED BY public.hr_career_level.id;


--
-- Name: hr_development_milestone; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_development_milestone (
    id bigint NOT NULL,
    plan_id bigint NOT NULL,
    milestone_name character varying(200) NOT NULL,
    description text,
    target_date date,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    completed_date date,
    completion_note text,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE hr_development_milestone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_development_milestone IS '发展规划里程碑表';


--
-- Name: hr_development_milestone_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_development_milestone_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_development_milestone_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_development_milestone_id_seq OWNED BY public.hr_development_milestone.id;


--
-- Name: hr_development_plan; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_development_plan (
    id bigint NOT NULL,
    plan_no character varying(50) NOT NULL,
    employee_id bigint NOT NULL,
    employee_name character varying(50),
    plan_year integer NOT NULL,
    plan_title character varying(200),
    current_level_id bigint,
    current_level_name character varying(100),
    target_level_id bigint,
    target_level_name character varying(100),
    target_date date,
    career_goals text,
    skill_goals text,
    performance_goals text,
    action_plans text,
    required_training text,
    required_resources text,
    mentor_id bigint,
    mentor_name character varying(50),
    progress_percentage integer DEFAULT 0,
    progress_notes text,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    reviewed_by bigint,
    reviewed_by_name character varying(50),
    reviewed_at timestamp without time zone,
    review_comment text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_development_plan; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_development_plan IS '个人发展规划表';


--
-- Name: hr_development_plan_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_development_plan_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_development_plan_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_development_plan_id_seq OWNED BY public.hr_development_plan.id;


--
-- Name: hr_employee; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_employee (
    id bigint NOT NULL,
    user_id bigint,
    employee_no character varying(50),
    gender character varying(20),
    birth_date date,
    id_card character varying(18),
    nationality character varying(50),
    native_place character varying(100),
    political_status character varying(50),
    education character varying(50),
    major character varying(100),
    graduation_school character varying(200),
    graduation_date date,
    emergency_contact character varying(50),
    emergency_phone character varying(20),
    address character varying(500),
    lawyer_license_no character varying(50),
    license_issue_date date,
    license_expire_date date,
    license_status character varying(20),
    practice_area character varying(200),
    practice_years integer,
    "position" character varying(50),
    level character varying(50),
    entry_date date,
    probation_end_date date,
    regular_date date,
    resignation_date date,
    resignation_reason character varying(500),
    work_status character varying(20) DEFAULT 'ACTIVE'::character varying,
    remark character varying(1000),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_employee; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_employee IS '员工档案表';


--
-- Name: COLUMN hr_employee.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_employee.user_id IS '关联用户ID';


--
-- Name: COLUMN hr_employee.employee_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_employee.employee_no IS '工号';


--
-- Name: COLUMN hr_employee.work_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_employee.work_status IS '工作状态: ACTIVE-在职, PROBATION-试用, RESIGNED-离职, RETIRED-退休';


--
-- Name: hr_employee_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_employee_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_employee_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_employee_id_seq OWNED BY public.hr_employee.id;


--
-- Name: hr_payroll_deduction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_payroll_deduction (
    id bigint NOT NULL,
    payroll_item_id bigint NOT NULL,
    deduction_type character varying(50) NOT NULL,
    amount numeric(15,2) NOT NULL,
    remark character varying(500),
    source_type character varying(50) DEFAULT 'MANUAL'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_payroll_deduction; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_payroll_deduction IS '工资扣减项表';


--
-- Name: COLUMN hr_payroll_deduction.payroll_item_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_deduction.payroll_item_id IS '工资明细ID';


--
-- Name: COLUMN hr_payroll_deduction.deduction_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_deduction.deduction_type IS '扣减类型：INCOME_TAX-个人所得税, SOCIAL_INSURANCE-社保个人部分, HOUSING_FUND-公积金个人部分, OTHER_DEDUCTION-其他扣款';


--
-- Name: COLUMN hr_payroll_deduction.amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_deduction.amount IS '金额';


--
-- Name: COLUMN hr_payroll_deduction.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_deduction.remark IS '备注';


--
-- Name: COLUMN hr_payroll_deduction.source_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_deduction.source_type IS '数据来源：AUTO-自动计算, MANUAL-手动输入, IMPORT-导入';


--
-- Name: hr_payroll_deduction_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_payroll_deduction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_payroll_deduction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_payroll_deduction_id_seq OWNED BY public.hr_payroll_deduction.id;


--
-- Name: hr_payroll_income; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_payroll_income (
    id bigint NOT NULL,
    payroll_item_id bigint NOT NULL,
    income_type character varying(50) NOT NULL,
    amount numeric(15,2) NOT NULL,
    remark character varying(500),
    source_type character varying(50),
    source_id bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_payroll_income; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_payroll_income IS '工资收入项表';


--
-- Name: COLUMN hr_payroll_income.payroll_item_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_income.payroll_item_id IS '工资明细ID';


--
-- Name: COLUMN hr_payroll_income.income_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_income.income_type IS '收入类型：BASE_SALARY-基本工资, COMMISSION-提成, PERFORMANCE_BONUS-绩效奖金, OTHER_ALLOWANCE-其他津贴';


--
-- Name: COLUMN hr_payroll_income.amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_income.amount IS '金额';


--
-- Name: COLUMN hr_payroll_income.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_income.remark IS '备注';


--
-- Name: COLUMN hr_payroll_income.source_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_income.source_type IS '数据来源：AUTO-自动汇总, MANUAL-手动输入, IMPORT-导入';


--
-- Name: COLUMN hr_payroll_income.source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_income.source_id IS '来源ID（如提成记录ID、合同ID等）';


--
-- Name: hr_payroll_income_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_payroll_income_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_payroll_income_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_payroll_income_id_seq OWNED BY public.hr_payroll_income.id;


--
-- Name: hr_payroll_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_payroll_item (
    id bigint NOT NULL,
    payroll_sheet_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    user_id bigint,
    employee_no character varying(50),
    employee_name character varying(50),
    gross_amount numeric(15,2) DEFAULT 0,
    deduction_amount numeric(15,2) DEFAULT 0,
    net_amount numeric(15,2) DEFAULT 0,
    confirm_status character varying(20) DEFAULT 'PENDING'::character varying,
    confirmed_at timestamp without time zone,
    confirm_comment character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    confirm_deadline timestamp without time zone
);


--
-- Name: TABLE hr_payroll_item; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_payroll_item IS '工资明细表';


--
-- Name: COLUMN hr_payroll_item.payroll_sheet_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.payroll_sheet_id IS '工资表ID';


--
-- Name: COLUMN hr_payroll_item.employee_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.employee_id IS '员工ID（关联hr_employee.id）';


--
-- Name: COLUMN hr_payroll_item.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.user_id IS '用户ID（关联sys_user.id，冗余字段便于查询）';


--
-- Name: COLUMN hr_payroll_item.employee_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.employee_no IS '工号（冗余字段）';


--
-- Name: COLUMN hr_payroll_item.employee_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.employee_name IS '员工姓名（冗余字段）';


--
-- Name: COLUMN hr_payroll_item.gross_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.gross_amount IS '应发工资（基本工资+提成+绩效+其他）';


--
-- Name: COLUMN hr_payroll_item.deduction_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.deduction_amount IS '扣减总额（税+社保+公积金+其他）';


--
-- Name: COLUMN hr_payroll_item.net_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.net_amount IS '实发工资（应发-扣减）';


--
-- Name: COLUMN hr_payroll_item.confirm_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.confirm_status IS '确认状态：PENDING-待确认, CONFIRMED-已确认, REJECTED-已拒绝';


--
-- Name: COLUMN hr_payroll_item.confirmed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.confirmed_at IS '确认时间';


--
-- Name: COLUMN hr_payroll_item.confirm_comment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.confirm_comment IS '确认意见（确认时可为空，拒绝时必须填写理由）';


--
-- Name: COLUMN hr_payroll_item.confirm_deadline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_item.confirm_deadline IS '确认截止时间（超过此时间未确认将自动确认，如果为空则使用工资表的autoConfirmDeadline）';


--
-- Name: hr_payroll_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_payroll_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_payroll_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_payroll_item_id_seq OWNED BY public.hr_payroll_item.id;


--
-- Name: hr_payroll_sheet; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_payroll_sheet (
    id bigint NOT NULL,
    payroll_no character varying(50) NOT NULL,
    payroll_year integer NOT NULL,
    payroll_month integer NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    total_employees integer DEFAULT 0,
    total_gross_amount numeric(15,2) DEFAULT 0,
    total_deduction_amount numeric(15,2) DEFAULT 0,
    total_net_amount numeric(15,2) DEFAULT 0,
    confirmed_count integer DEFAULT 0,
    submitted_at timestamp without time zone,
    submitted_by bigint,
    finance_confirmed_at timestamp without time zone,
    finance_confirmed_by bigint,
    issued_at timestamp without time zone,
    issued_by bigint,
    payment_method character varying(50),
    payment_voucher_url character varying(500),
    remark character varying(1000),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    auto_confirm_deadline timestamp without time zone,
    approver_id bigint,
    approved_at timestamp without time zone,
    approved_by bigint,
    approval_comment text
);


--
-- Name: TABLE hr_payroll_sheet; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_payroll_sheet IS '工资表';


--
-- Name: COLUMN hr_payroll_sheet.payroll_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.payroll_no IS '工资表编号（格式：PAY-YYYYMM）';


--
-- Name: COLUMN hr_payroll_sheet.payroll_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.payroll_year IS '工资年份';


--
-- Name: COLUMN hr_payroll_sheet.payroll_month; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.payroll_month IS '工资月份（1-12）';


--
-- Name: COLUMN hr_payroll_sheet.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.status IS '状态：DRAFT-草稿, PENDING_CONFIRM-待确认, CONFIRMED-已确认, PENDING_APPROVAL-待审批, APPROVED-已审批, REJECTED-已拒绝, ISSUED-已发放';


--
-- Name: COLUMN hr_payroll_sheet.total_employees; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.total_employees IS '总人数';


--
-- Name: COLUMN hr_payroll_sheet.total_gross_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.total_gross_amount IS '应发工资总额';


--
-- Name: COLUMN hr_payroll_sheet.total_deduction_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.total_deduction_amount IS '扣减总额';


--
-- Name: COLUMN hr_payroll_sheet.total_net_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.total_net_amount IS '实发工资总额';


--
-- Name: COLUMN hr_payroll_sheet.confirmed_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.confirmed_count IS '已确认人数';


--
-- Name: COLUMN hr_payroll_sheet.submitted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.submitted_at IS '提交时间';


--
-- Name: COLUMN hr_payroll_sheet.submitted_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.submitted_by IS '提交人ID';


--
-- Name: COLUMN hr_payroll_sheet.finance_confirmed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.finance_confirmed_at IS '财务确认时间';


--
-- Name: COLUMN hr_payroll_sheet.finance_confirmed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.finance_confirmed_by IS '财务确认人ID';


--
-- Name: COLUMN hr_payroll_sheet.issued_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.issued_at IS '发放时间';


--
-- Name: COLUMN hr_payroll_sheet.issued_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.issued_by IS '发放人ID';


--
-- Name: COLUMN hr_payroll_sheet.payment_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.payment_method IS '发放方式：BANK_TRANSFER-银行转账, CASH-现金, OTHER-其他';


--
-- Name: COLUMN hr_payroll_sheet.payment_voucher_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.payment_voucher_url IS '发放凭证URL';


--
-- Name: COLUMN hr_payroll_sheet.remark; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.remark IS '备注';


--
-- Name: COLUMN hr_payroll_sheet.auto_confirm_deadline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.auto_confirm_deadline IS '自动确认截止时间（超过此时间未确认的工资明细将自动确认）';


--
-- Name: COLUMN hr_payroll_sheet.approver_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.approver_id IS '审批人ID（主任或合伙人）';


--
-- Name: COLUMN hr_payroll_sheet.approved_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.approved_at IS '审批时间';


--
-- Name: COLUMN hr_payroll_sheet.approved_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.approved_by IS '审批人ID';


--
-- Name: COLUMN hr_payroll_sheet.approval_comment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_payroll_sheet.approval_comment IS '审批意见';


--
-- Name: hr_payroll_sheet_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_payroll_sheet_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_payroll_sheet_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_payroll_sheet_id_seq OWNED BY public.hr_payroll_sheet.id;


--
-- Name: hr_performance_evaluation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_performance_evaluation (
    id bigint NOT NULL,
    task_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    evaluator_id bigint NOT NULL,
    evaluation_type character varying(20) NOT NULL,
    total_score numeric(5,2),
    grade character varying(10),
    comment text,
    strengths text,
    improvements text,
    evaluated_at timestamp without time zone,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_performance_evaluation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_performance_evaluation IS '绩效评价表';


--
-- Name: hr_performance_evaluation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_performance_evaluation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_performance_evaluation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_performance_evaluation_id_seq OWNED BY public.hr_performance_evaluation.id;


--
-- Name: hr_performance_indicator; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_performance_indicator (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50),
    category character varying(20) NOT NULL,
    description text,
    weight numeric(5,2) NOT NULL,
    max_score integer DEFAULT 100,
    scoring_criteria text,
    applicable_role character varying(20) DEFAULT 'ALL'::character varying,
    sort_order integer DEFAULT 0,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    remarks text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_performance_indicator; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_performance_indicator IS '考核指标表';


--
-- Name: hr_performance_indicator_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_performance_indicator_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_performance_indicator_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_performance_indicator_id_seq OWNED BY public.hr_performance_indicator.id;


--
-- Name: hr_performance_score; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_performance_score (
    id bigint NOT NULL,
    evaluation_id bigint NOT NULL,
    indicator_id bigint NOT NULL,
    score numeric(5,2),
    comment text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_performance_score; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_performance_score IS '绩效评分明细表';


--
-- Name: hr_performance_score_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_performance_score_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_performance_score_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_performance_score_id_seq OWNED BY public.hr_performance_score.id;


--
-- Name: hr_performance_task; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_performance_task (
    id bigint NOT NULL,
    name character varying(200) NOT NULL,
    period_type character varying(20) NOT NULL,
    year integer NOT NULL,
    period integer,
    start_date date,
    end_date date,
    self_eval_deadline date,
    peer_eval_deadline date,
    supervisor_eval_deadline date,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    description text,
    remarks text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_performance_task; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_performance_task IS '考核任务表';


--
-- Name: hr_performance_task_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_performance_task_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_performance_task_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_performance_task_id_seq OWNED BY public.hr_performance_task.id;


--
-- Name: hr_promotion_application; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_promotion_application (
    id bigint NOT NULL,
    application_no character varying(50) NOT NULL,
    employee_id bigint NOT NULL,
    employee_name character varying(50),
    department_id bigint,
    department_name character varying(100),
    current_level_id bigint,
    current_level_name character varying(100),
    target_level_id bigint NOT NULL,
    target_level_name character varying(100),
    apply_reason text,
    achievements text,
    self_evaluation text,
    attachments text,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    review_score numeric(5,2),
    review_result character varying(20),
    review_comment text,
    approved_by bigint,
    approved_by_name character varying(50),
    approved_at timestamp without time zone,
    approval_comment text,
    effective_date date,
    apply_date date NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_promotion_application; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_promotion_application IS '晋升申请表';


--
-- Name: hr_promotion_application_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_promotion_application_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_promotion_application_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_promotion_application_id_seq OWNED BY public.hr_promotion_application.id;


--
-- Name: hr_promotion_review; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_promotion_review (
    id bigint NOT NULL,
    application_id bigint NOT NULL,
    reviewer_id bigint NOT NULL,
    reviewer_name character varying(50),
    reviewer_role character varying(50),
    score_details text,
    total_score numeric(5,2),
    review_opinion character varying(20),
    review_comment text,
    review_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE hr_promotion_review; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_promotion_review IS '晋升评审记录表';


--
-- Name: hr_promotion_review_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_promotion_review_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_promotion_review_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_promotion_review_id_seq OWNED BY public.hr_promotion_review.id;


--
-- Name: hr_regularization; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_regularization (
    id bigint NOT NULL,
    employee_id bigint NOT NULL,
    application_no character varying(30) NOT NULL,
    probation_start_date date,
    probation_end_date date,
    application_date date NOT NULL,
    expected_regular_date date,
    self_evaluation text,
    supervisor_evaluation text,
    hr_evaluation text,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approver_id bigint,
    approved_date date,
    comment character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_regularization; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_regularization IS '转正申请表';


--
-- Name: COLUMN hr_regularization.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_regularization.status IS '状态: PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝';


--
-- Name: hr_regularization_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_regularization_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_regularization_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_regularization_id_seq OWNED BY public.hr_regularization.id;


--
-- Name: hr_resignation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_resignation (
    id bigint NOT NULL,
    employee_id bigint NOT NULL,
    application_no character varying(30) NOT NULL,
    resignation_type character varying(20),
    resignation_date date NOT NULL,
    last_work_date date,
    reason character varying(1000),
    handover_person_id bigint,
    handover_status character varying(20) DEFAULT 'PENDING'::character varying,
    handover_note text,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approver_id bigint,
    approved_date date,
    comment character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_resignation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_resignation IS '离职申请表';


--
-- Name: COLUMN hr_resignation.resignation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_resignation.resignation_type IS '离职类型: VOLUNTARY-主动离职, DISMISSED-辞退, RETIREMENT-退休, CONTRACT_EXPIRED-合同到期';


--
-- Name: COLUMN hr_resignation.handover_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_resignation.handover_status IS '交接状态: PENDING-待交接, IN_PROGRESS-交接中, COMPLETED-已完成';


--
-- Name: COLUMN hr_resignation.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_resignation.status IS '状态: PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝, COMPLETED-已完成';


--
-- Name: hr_resignation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_resignation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_resignation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_resignation_id_seq OWNED BY public.hr_resignation.id;


--
-- Name: hr_training; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_training (
    id bigint NOT NULL,
    title character varying(200) NOT NULL,
    training_type character varying(50) NOT NULL,
    category character varying(50),
    description text,
    trainer character varying(100),
    location character varying(200),
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    duration integer,
    credits integer DEFAULT 0,
    max_participants integer,
    current_participants integer DEFAULT 0,
    enroll_deadline date,
    status character varying(50) DEFAULT 'DRAFT'::character varying,
    materials_url character varying(500),
    remarks text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_training; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_training IS '培训计划表';


--
-- Name: COLUMN hr_training.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.title IS '培训标题';


--
-- Name: COLUMN hr_training.training_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.training_type IS '培训类型：INTERNAL-内部培训, EXTERNAL-外部培训, ONLINE-在线培训';


--
-- Name: COLUMN hr_training.category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.category IS '培训分类：LAW-法律知识, SKILL-业务技能, MANAGEMENT-管理能力, OTHER-其他';


--
-- Name: COLUMN hr_training.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.description IS '培训描述';


--
-- Name: COLUMN hr_training.trainer; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.trainer IS '讲师/培训机构';


--
-- Name: COLUMN hr_training.location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.location IS '培训地点';


--
-- Name: COLUMN hr_training.start_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.start_time IS '开始时间';


--
-- Name: COLUMN hr_training.end_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.end_time IS '结束时间';


--
-- Name: COLUMN hr_training.duration; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.duration IS '培训时长（小时）';


--
-- Name: COLUMN hr_training.credits; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.credits IS '学分';


--
-- Name: COLUMN hr_training.max_participants; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.max_participants IS '最大参与人数';


--
-- Name: COLUMN hr_training.current_participants; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.current_participants IS '当前报名人数';


--
-- Name: COLUMN hr_training.enroll_deadline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.enroll_deadline IS '报名截止日期';


--
-- Name: COLUMN hr_training.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.status IS '状态：DRAFT-草稿, PUBLISHED-已发布, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消';


--
-- Name: COLUMN hr_training.materials_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.materials_url IS '培训材料URL';


--
-- Name: COLUMN hr_training.remarks; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.remarks IS '备注';


--
-- Name: COLUMN hr_training.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training.created_by IS '创建人';


--
-- Name: hr_training_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_training_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_training_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_training_id_seq OWNED BY public.hr_training.id;


--
-- Name: hr_training_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_training_record (
    id bigint NOT NULL,
    training_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    enroll_time timestamp without time zone,
    check_in_time timestamp without time zone,
    check_out_time timestamp without time zone,
    actual_duration numeric(5,2),
    status character varying(50) DEFAULT 'ENROLLED'::character varying,
    score numeric(5,2),
    passed boolean,
    earned_credits integer DEFAULT 0,
    feedback text,
    rating integer,
    certificate_url character varying(500),
    remarks text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE hr_training_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.hr_training_record IS '培训记录表';


--
-- Name: COLUMN hr_training_record.training_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.training_id IS '培训ID';


--
-- Name: COLUMN hr_training_record.employee_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.employee_id IS '员工ID';


--
-- Name: COLUMN hr_training_record.enroll_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.enroll_time IS '报名时间';


--
-- Name: COLUMN hr_training_record.check_in_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.check_in_time IS '签到时间';


--
-- Name: COLUMN hr_training_record.check_out_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.check_out_time IS '签退时间';


--
-- Name: COLUMN hr_training_record.actual_duration; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.actual_duration IS '实际参与时长（小时）';


--
-- Name: COLUMN hr_training_record.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.status IS '状态：ENROLLED-已报名, ATTENDED-已参加, ABSENT-缺席, CANCELLED-已取消';


--
-- Name: COLUMN hr_training_record.score; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.score IS '考核成绩';


--
-- Name: COLUMN hr_training_record.passed; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.passed IS '是否通过';


--
-- Name: COLUMN hr_training_record.earned_credits; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.earned_credits IS '获得学分';


--
-- Name: COLUMN hr_training_record.feedback; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.feedback IS '培训反馈/评价';


--
-- Name: COLUMN hr_training_record.rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.rating IS '评分（1-5）';


--
-- Name: COLUMN hr_training_record.certificate_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.certificate_url IS '证书URL';


--
-- Name: COLUMN hr_training_record.remarks; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.hr_training_record.remarks IS '备注';


--
-- Name: hr_training_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hr_training_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hr_training_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.hr_training_record_id_seq OWNED BY public.hr_training_record.id;


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
-- Name: leave_application; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.leave_application (
    id bigint NOT NULL,
    application_no character varying(30) NOT NULL,
    user_id bigint NOT NULL,
    leave_type_id bigint NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    duration numeric(5,1) NOT NULL,
    reason character varying(500) NOT NULL,
    attachment_url character varying(500),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approver_id bigint,
    approved_at timestamp without time zone,
    approval_comment character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE leave_application; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.leave_application IS '请假申请表';


--
-- Name: COLUMN leave_application.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.leave_application.status IS '状态: PENDING待审批/APPROVED已批准/REJECTED已拒绝/CANCELLED已取消';


--
-- Name: leave_application_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.leave_application_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: leave_application_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.leave_application_id_seq OWNED BY public.leave_application.id;


--
-- Name: leave_balance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.leave_balance (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    leave_type_id bigint NOT NULL,
    year integer NOT NULL,
    total_days numeric(5,1) DEFAULT 0 NOT NULL,
    used_days numeric(5,1) DEFAULT 0 NOT NULL,
    remaining_days numeric(5,1) DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE leave_balance; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.leave_balance IS '假期余额表';


--
-- Name: leave_balance_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.leave_balance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: leave_balance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.leave_balance_id_seq OWNED BY public.leave_balance.id;


--
-- Name: leave_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.leave_type (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    code character varying(30) NOT NULL,
    paid boolean DEFAULT true,
    annual_limit numeric(5,1),
    need_approval boolean DEFAULT true,
    description character varying(200),
    sort_order integer DEFAULT 0,
    enabled boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE leave_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.leave_type IS '请假类型表';


--
-- Name: leave_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.leave_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: leave_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.leave_type_id_seq OWNED BY public.leave_type.id;


--
-- Name: letter_application; Type: TABLE; Schema: public; Owner: -
--

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
-- Name: matter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter (
    id bigint NOT NULL,
    matter_no character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    matter_type character varying(20) NOT NULL,
    case_type character varying(30),
    cause_of_action character varying(50),
    business_type character varying(50),
    client_id bigint NOT NULL,
    opposing_party character varying(200),
    opposing_lawyer_name character varying(100),
    opposing_lawyer_license_no character varying(50),
    opposing_lawyer_firm character varying(200),
    opposing_lawyer_phone character varying(20),
    opposing_lawyer_email character varying(100),
    description text,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    originator_id bigint,
    lead_lawyer_id bigint,
    department_id bigint,
    fee_type character varying(20),
    estimated_fee numeric(15,2),
    actual_fee numeric(15,2),
    filing_date date,
    expected_end_date date,
    actual_end_date date,
    claim_amount numeric(15,2),
    outcome text,
    contract_id bigint,
    remark text,
    conflict_status character varying(20),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE matter; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter IS '案件/项目表';


--
-- Name: COLUMN matter.matter_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.matter_no IS '案件编号';


--
-- Name: COLUMN matter.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.name IS '案件名称';


--
-- Name: COLUMN matter.matter_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.matter_type IS '项目大类：LITIGATION-诉讼案件, NON_LITIGATION-非诉项目';


--
-- Name: COLUMN matter.case_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.case_type IS '案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务';


--
-- Name: COLUMN matter.cause_of_action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.cause_of_action IS '案由代码';


--
-- Name: COLUMN matter.opposing_lawyer_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_name IS '对方律师姓名';


--
-- Name: COLUMN matter.opposing_lawyer_license_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_license_no IS '对方律师执业证号';


--
-- Name: COLUMN matter.opposing_lawyer_firm; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_firm IS '对方律师所在律所';


--
-- Name: COLUMN matter.opposing_lawyer_phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_phone IS '对方律师联系电话';


--
-- Name: COLUMN matter.opposing_lawyer_email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.opposing_lawyer_email IS '对方律师邮箱';


--
-- Name: COLUMN matter.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.status IS '状态：DRAFT-草稿, PENDING-待审批, ACTIVE-进行中, SUSPENDED-暂停, CLOSED-结案, ARCHIVED-归档';


--
-- Name: COLUMN matter.fee_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.fee_type IS '收费方式：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理, MIXED-混合收费';


--
-- Name: COLUMN matter.conflict_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter.conflict_status IS '利冲检查状态：PENDING-待检查, PASSED-已通过, FAILED-未通过';


--
-- Name: matter_client; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter_client (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    client_id bigint NOT NULL,
    client_role character varying(50) DEFAULT 'PLAINTIFF'::character varying,
    is_primary boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE matter_client; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter_client IS '项目-客户关联表（支持多客户）';


--
-- Name: COLUMN matter_client.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_client.matter_id IS '项目ID';


--
-- Name: COLUMN matter_client.client_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_client.client_id IS '客户ID';


--
-- Name: COLUMN matter_client.client_role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_client.client_role IS '客户角色：PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人';


--
-- Name: COLUMN matter_client.is_primary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_client.is_primary IS '是否主要客户（用于显示和结算）';


--
-- Name: matter_client_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_client_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: matter_client_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_client_id_seq OWNED BY public.matter_client.id;


--
-- Name: matter_deadline; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter_deadline (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    deadline_type character varying(50) NOT NULL,
    deadline_name character varying(200) NOT NULL,
    base_date date NOT NULL,
    deadline_date date NOT NULL,
    reminder_days integer DEFAULT 7,
    reminder_sent boolean DEFAULT false,
    reminder_sent_at timestamp without time zone,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    completed_at timestamp without time zone,
    completed_by bigint,
    description character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE matter_deadline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter_deadline IS '期限提醒表';


--
-- Name: COLUMN matter_deadline.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.matter_id IS '项目ID';


--
-- Name: COLUMN matter_deadline.deadline_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.deadline_type IS '期限类型';


--
-- Name: COLUMN matter_deadline.deadline_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.deadline_name IS '期限名称';


--
-- Name: COLUMN matter_deadline.base_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.base_date IS '基准日期';


--
-- Name: COLUMN matter_deadline.deadline_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.deadline_date IS '期限日期';


--
-- Name: COLUMN matter_deadline.reminder_days; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.reminder_days IS '提前提醒天数';


--
-- Name: COLUMN matter_deadline.reminder_sent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.reminder_sent IS '是否已发送提醒';


--
-- Name: COLUMN matter_deadline.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_deadline.status IS '状态';


--
-- Name: matter_deadline_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_deadline_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: matter_deadline_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_deadline_id_seq OWNED BY public.matter_deadline.id;


--
-- Name: matter_dossier_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter_dossier_item (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    parent_id bigint DEFAULT 0,
    name character varying(200) NOT NULL,
    item_type character varying(50) DEFAULT 'FOLDER'::character varying,
    file_category character varying(50),
    sort_order integer DEFAULT 0,
    document_count integer DEFAULT 0,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false,
    updated_by bigint
);


--
-- Name: TABLE matter_dossier_item; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter_dossier_item IS '项目卷宗目录表';


--
-- Name: matter_dossier_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_dossier_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: matter_dossier_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_dossier_item_id_seq OWNED BY public.matter_dossier_item.id;


--
-- Name: matter_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: matter_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_id_seq OWNED BY public.matter.id;


--
-- Name: matter_participant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matter_participant (
    id bigint NOT NULL,
    matter_id bigint NOT NULL,
    user_id bigint NOT NULL,
    role character varying(20) NOT NULL,
    commission_rate numeric(5,2),
    is_originator boolean DEFAULT false,
    join_date date,
    exit_date date,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE matter_participant; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matter_participant IS '案件参与人表';


--
-- Name: COLUMN matter_participant.role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_participant.role IS '角色：LEAD-主办律师, CO_COUNSEL-协办律师, PARALEGAL-律师助理, TRAINEE-实习律师';


--
-- Name: COLUMN matter_participant.commission_rate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_participant.commission_rate IS '提成比例（百分比）';


--
-- Name: COLUMN matter_participant.is_originator; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matter_participant.is_originator IS '是否案源人';


--
-- Name: matter_participant_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matter_participant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: matter_participant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matter_participant_id_seq OWNED BY public.matter_participant.id;


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
-- Name: overtime_application; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.overtime_application (
    id bigint NOT NULL,
    application_no character varying(50) NOT NULL,
    user_id bigint NOT NULL,
    overtime_date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    overtime_hours numeric(4,2),
    reason character varying(500),
    work_content character varying(1000),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approver_id bigint,
    approved_at timestamp without time zone,
    approval_comment character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE overtime_application; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.overtime_application IS '加班申请表';


--
-- Name: COLUMN overtime_application.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.overtime_application.status IS '状态: PENDING待审批/APPROVED已批准/REJECTED已拒绝';


--
-- Name: overtime_application_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.overtime_application_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: overtime_application_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.overtime_application_id_seq OWNED BY public.overtime_application.id;


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
-- Name: sys_announcement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_announcement (
    id bigint NOT NULL,
    title character varying(200) NOT NULL,
    content text NOT NULL,
    type character varying(30) DEFAULT 'NOTICE'::character varying,
    priority integer DEFAULT 0,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    publish_time timestamp without time zone,
    expire_time timestamp without time zone,
    is_top boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_announcement; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_announcement IS '系统公告表';


--
-- Name: sys_announcement_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_announcement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_announcement_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_announcement_id_seq OWNED BY public.sys_announcement.id;


--
-- Name: sys_backup; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_backup (
    id bigint NOT NULL,
    backup_no character varying(50) NOT NULL,
    backup_type character varying(20) NOT NULL,
    backup_name character varying(200) NOT NULL,
    backup_path character varying(500) NOT NULL,
    file_size bigint,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    backup_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    restore_time timestamp without time zone,
    description text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_backup; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_backup IS '系统备份记录表';


--
-- Name: COLUMN sys_backup.backup_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_backup.backup_no IS '备份编号';


--
-- Name: COLUMN sys_backup.backup_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_backup.backup_type IS '备份类型: FULL-全量, INCREMENTAL-增量, DATABASE-数据库, FILE-文件';


--
-- Name: COLUMN sys_backup.backup_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_backup.backup_name IS '备份名称';


--
-- Name: COLUMN sys_backup.backup_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_backup.backup_path IS '备份文件路径';


--
-- Name: COLUMN sys_backup.file_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_backup.file_size IS '文件大小（字节）';


--
-- Name: COLUMN sys_backup.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_backup.status IS '状态: PENDING-进行中, SUCCESS-成功, FAILED-失败';


--
-- Name: COLUMN sys_backup.backup_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_backup.backup_time IS '备份时间';


--
-- Name: COLUMN sys_backup.restore_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_backup.restore_time IS '恢复时间';


--
-- Name: COLUMN sys_backup.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_backup.description IS '备份说明';


--
-- Name: sys_backup_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_backup_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_backup_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_backup_id_seq OWNED BY public.sys_backup.id;


--
-- Name: sys_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_config (
    id bigint NOT NULL,
    config_key character varying(100) NOT NULL,
    config_value character varying(2000),
    config_name character varying(100) NOT NULL,
    config_type character varying(20) DEFAULT 'STRING'::character varying,
    description character varying(500),
    is_system boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_config; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_config IS '系统参数配置表';


--
-- Name: sys_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_config_id_seq OWNED BY public.sys_config.id;


--
-- Name: sys_data_handover; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_data_handover (
    id bigint NOT NULL,
    handover_no character varying(50) NOT NULL,
    from_user_id bigint NOT NULL,
    from_username character varying(50) NOT NULL,
    to_user_id bigint NOT NULL,
    to_username character varying(50) NOT NULL,
    handover_type character varying(20) NOT NULL,
    handover_reason text,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    matter_count integer DEFAULT 0,
    client_count integer DEFAULT 0,
    lead_count integer DEFAULT 0,
    task_count integer DEFAULT 0,
    submitted_by bigint,
    submitted_at timestamp without time zone,
    confirmed_by bigint,
    confirmed_at timestamp without time zone,
    remark text,
    deleted boolean DEFAULT false,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    approval_id bigint
);


--
-- Name: TABLE sys_data_handover; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_data_handover IS '数据交接记录表';


--
-- Name: COLUMN sys_data_handover.handover_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_handover.handover_no IS '交接单号';


--
-- Name: COLUMN sys_data_handover.from_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_handover.from_user_id IS '移交人ID';


--
-- Name: COLUMN sys_data_handover.to_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_handover.to_user_id IS '接收人ID';


--
-- Name: COLUMN sys_data_handover.handover_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_handover.handover_type IS '交接类型：RESIGNATION-离职交接, PROJECT-项目移交, CLIENT-客户移交, LEAD-案源移交';


--
-- Name: COLUMN sys_data_handover.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_handover.status IS '状态：PENDING_APPROVAL-待审批, APPROVED-审批通过待执行, REJECTED-已拒绝, CONFIRMED-已确认, CANCELLED-已取消';


--
-- Name: sys_data_handover_detail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_data_handover_detail (
    id bigint NOT NULL,
    handover_id bigint NOT NULL,
    data_type character varying(30) NOT NULL,
    data_id bigint NOT NULL,
    data_no character varying(50),
    data_name character varying(200),
    field_name character varying(50) NOT NULL,
    old_value character varying(100),
    new_value character varying(100),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    error_message text,
    executed_at timestamp without time zone,
    deleted boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint
);


--
-- Name: TABLE sys_data_handover_detail; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_data_handover_detail IS '数据交接明细表';


--
-- Name: COLUMN sys_data_handover_detail.data_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_handover_detail.data_type IS '数据类型：MATTER, CLIENT, LEAD, TASK, MATTER_PARTICIPANT, CONTRACT_PARTICIPANT';


--
-- Name: COLUMN sys_data_handover_detail.field_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_handover_detail.field_name IS '变更字段名';


--
-- Name: COLUMN sys_data_handover_detail.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_handover_detail.status IS '状态：PENDING-待处理, DONE-已完成, FAILED-失败';


--
-- Name: sys_data_handover_detail_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_data_handover_detail_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_data_handover_detail_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_data_handover_detail_id_seq OWNED BY public.sys_data_handover_detail.id;


--
-- Name: sys_data_handover_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_data_handover_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_data_handover_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_data_handover_id_seq OWNED BY public.sys_data_handover.id;


--
-- Name: sys_data_sync_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_data_sync_log (
    id bigint NOT NULL,
    source_table character varying(100) NOT NULL,
    source_id bigint NOT NULL,
    target_module character varying(50) NOT NULL,
    operation_type character varying(50) NOT NULL,
    sync_data text,
    sync_status character varying(20) NOT NULL,
    error_message text,
    retry_count integer DEFAULT 0,
    synced_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_data_sync_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_data_sync_log IS '数据同步日志表';


--
-- Name: COLUMN sys_data_sync_log.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.id IS '主键ID';


--
-- Name: COLUMN sys_data_sync_log.source_table; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.source_table IS '源表名';


--
-- Name: COLUMN sys_data_sync_log.source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.source_id IS '源记录ID';


--
-- Name: COLUMN sys_data_sync_log.target_module; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.target_module IS '目标模块：FINANCE-财务模块, ADMIN-行政模块';


--
-- Name: COLUMN sys_data_sync_log.operation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.operation_type IS '操作类型：CREATE-创建, UPDATE-更新, DELETE-删除';


--
-- Name: COLUMN sys_data_sync_log.sync_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.sync_data IS '同步的数据内容（JSON格式）';


--
-- Name: COLUMN sys_data_sync_log.sync_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.sync_status IS '同步状态：SUCCESS-成功, FAILED-失败, PENDING-待处理';


--
-- Name: COLUMN sys_data_sync_log.error_message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.error_message IS '错误信息';


--
-- Name: COLUMN sys_data_sync_log.retry_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.retry_count IS '重试次数';


--
-- Name: COLUMN sys_data_sync_log.synced_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_data_sync_log.synced_at IS '同步时间';


--
-- Name: sys_data_sync_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_data_sync_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_data_sync_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_data_sync_log_id_seq OWNED BY public.sys_data_sync_log.id;


--
-- Name: sys_department; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_department (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    parent_id bigint DEFAULT 0,
    sort_order integer DEFAULT 0,
    leader_id bigint,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_department; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_department IS '部门表';


--
-- Name: COLUMN sys_department.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_department.name IS '部门名称';


--
-- Name: COLUMN sys_department.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_department.parent_id IS '父部门ID';


--
-- Name: COLUMN sys_department.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_department.sort_order IS '排序';


--
-- Name: COLUMN sys_department.leader_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_department.leader_id IS '部门负责人';


--
-- Name: COLUMN sys_department.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_department.status IS '状态';


--
-- Name: sys_department_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_department_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_department_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_department_id_seq OWNED BY public.sys_department.id;


--
-- Name: sys_dict_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_dict_item (
    id bigint NOT NULL,
    dict_type_id bigint NOT NULL,
    label character varying(100) NOT NULL,
    value character varying(100) NOT NULL,
    description character varying(500),
    sort_order integer DEFAULT 0,
    status character varying(20) DEFAULT 'ENABLED'::character varying,
    css_class character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_dict_item; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_dict_item IS '数据字典项表';


--
-- Name: sys_dict_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_dict_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_dict_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_dict_item_id_seq OWNED BY public.sys_dict_item.id;


--
-- Name: sys_dict_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_dict_type (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50) NOT NULL,
    description character varying(500),
    status character varying(20) DEFAULT 'ENABLED'::character varying,
    is_system boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_dict_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_dict_type IS '数据字典类型表';


--
-- Name: sys_dict_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_dict_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_dict_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_dict_type_id_seq OWNED BY public.sys_dict_type.id;


--
-- Name: sys_export_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_export_log (
    id bigint NOT NULL,
    export_type character varying(50) NOT NULL,
    export_params jsonb,
    record_count integer,
    exported_by bigint NOT NULL,
    exported_at timestamp without time zone NOT NULL,
    file_name character varying(200),
    file_size bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_export_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_export_log IS '导出日志表';


--
-- Name: COLUMN sys_export_log.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_export_log.id IS '主键ID';


--
-- Name: COLUMN sys_export_log.export_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_export_log.export_type IS '导出类型：JUDICIAL_FILING-司法局报备, CONTRACT_LIST-合同列表等';


--
-- Name: COLUMN sys_export_log.export_params; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_export_log.export_params IS '导出参数（JSON格式，如年月、筛选条件等）';


--
-- Name: COLUMN sys_export_log.record_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_export_log.record_count IS '导出记录数';


--
-- Name: COLUMN sys_export_log.exported_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_export_log.exported_by IS '导出人ID';


--
-- Name: COLUMN sys_export_log.exported_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_export_log.exported_at IS '导出时间';


--
-- Name: COLUMN sys_export_log.file_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_export_log.file_name IS '导出文件名';


--
-- Name: COLUMN sys_export_log.file_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_export_log.file_size IS '文件大小（字节）';


--
-- Name: sys_export_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_export_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_export_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_export_log_id_seq OWNED BY public.sys_export_log.id;


--
-- Name: sys_external_integration; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_external_integration (
    id bigint NOT NULL,
    integration_code character varying(50) NOT NULL,
    integration_name character varying(100) NOT NULL,
    integration_type character varying(50) NOT NULL,
    description text,
    api_url character varying(500),
    api_key character varying(500),
    api_secret character varying(500),
    auth_type character varying(50) DEFAULT 'API_KEY'::character varying,
    extra_config jsonb,
    enabled boolean DEFAULT false,
    last_test_time timestamp without time zone,
    last_test_result character varying(20),
    last_test_message text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_external_integration; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_external_integration IS '外部系统集成配置表';


--
-- Name: COLUMN sys_external_integration.integration_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_external_integration.integration_code IS '集成编码，唯一标识';


--
-- Name: COLUMN sys_external_integration.integration_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_external_integration.integration_type IS '类型：ARCHIVE-档案系统, AI-AI大模型, OTHER-其他';


--
-- Name: COLUMN sys_external_integration.auth_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_external_integration.auth_type IS '认证方式：API_KEY, BEARER_TOKEN, BASIC, OAUTH2';


--
-- Name: COLUMN sys_external_integration.extra_config; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_external_integration.extra_config IS 'JSON格式的额外配置';


--
-- Name: sys_external_integration_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_external_integration_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_external_integration_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_external_integration_id_seq OWNED BY public.sys_external_integration.id;


--
-- Name: sys_login_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_login_log (
    id bigint NOT NULL,
    user_id bigint,
    username character varying(50),
    real_name character varying(50),
    login_ip character varying(50),
    login_location character varying(200),
    user_agent character varying(2000),
    browser character varying(100),
    os character varying(100),
    device_type character varying(20),
    status character varying(20) NOT NULL,
    message character varying(2000),
    login_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    logout_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_login_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_login_log IS '登录日志表';


--
-- Name: COLUMN sys_login_log.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.user_id IS '用户ID';


--
-- Name: COLUMN sys_login_log.username; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.username IS '用户名';


--
-- Name: COLUMN sys_login_log.real_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.real_name IS '真实姓名';


--
-- Name: COLUMN sys_login_log.login_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.login_ip IS '登录IP';


--
-- Name: COLUMN sys_login_log.login_location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.login_location IS '登录地点';


--
-- Name: COLUMN sys_login_log.user_agent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.user_agent IS '用户代理';


--
-- Name: COLUMN sys_login_log.browser; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.browser IS '浏览器';


--
-- Name: COLUMN sys_login_log.os; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.os IS '操作系统';


--
-- Name: COLUMN sys_login_log.device_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.device_type IS '设备类型: PC, MOBILE, TABLET';


--
-- Name: COLUMN sys_login_log.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.status IS '状态: SUCCESS, FAILURE';


--
-- Name: COLUMN sys_login_log.message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.message IS '登录结果消息';


--
-- Name: COLUMN sys_login_log.login_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.login_time IS '登录时间';


--
-- Name: COLUMN sys_login_log.logout_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.logout_time IS '登出时间';


--
-- Name: COLUMN sys_login_log.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.updated_at IS '更新时间';


--
-- Name: COLUMN sys_login_log.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.created_by IS '创建人ID';


--
-- Name: COLUMN sys_login_log.updated_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.updated_by IS '更新人ID';


--
-- Name: COLUMN sys_login_log.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_login_log.deleted IS '是否删除';


--
-- Name: sys_login_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_login_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_login_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_login_log_id_seq OWNED BY public.sys_login_log.id;


--
-- Name: sys_menu; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_menu (
    id bigint NOT NULL,
    parent_id bigint DEFAULT 0,
    name character varying(100) NOT NULL,
    path character varying(200),
    component character varying(200),
    redirect character varying(200),
    icon character varying(100),
    menu_type character varying(20) NOT NULL,
    permission character varying(200),
    sort_order integer DEFAULT 0,
    visible boolean DEFAULT true,
    status character varying(20) DEFAULT 'ENABLED'::character varying,
    is_external boolean DEFAULT false,
    is_cache boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_menu; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_menu IS '菜单/权限表';


--
-- Name: COLUMN sys_menu.menu_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.menu_type IS '类型: DIRECTORY-目录, MENU-菜单, BUTTON-按钮';


--
-- Name: COLUMN sys_menu.permission; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_menu.permission IS '权限标识';


--
-- Name: sys_menu_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_menu_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_menu_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_menu_id_seq OWNED BY public.sys_menu.id;


--
-- Name: sys_notification; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_notification (
    id bigint NOT NULL,
    title character varying(200) NOT NULL,
    content text NOT NULL,
    type character varying(30) NOT NULL,
    sender_id bigint,
    receiver_id bigint NOT NULL,
    is_read boolean DEFAULT false,
    read_at timestamp without time zone,
    business_type character varying(50),
    business_id bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_notification; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_notification IS '系统通知表';


--
-- Name: COLUMN sys_notification.type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_notification.type IS '类型: SYSTEM系统/APPROVAL审批/TASK任务/REMINDER提醒';


--
-- Name: sys_notification_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_notification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_notification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_notification_id_seq OWNED BY public.sys_notification.id;


--
-- Name: sys_operation_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_operation_log (
    id bigint NOT NULL,
    user_id bigint,
    user_name character varying(50),
    module character varying(50),
    operation_type character varying(50),
    description character varying(500),
    method character varying(200),
    request_url character varying(500),
    request_method character varying(10),
    request_params text,
    response_result text,
    ip_address character varying(50),
    user_agent character varying(2000),
    execution_time bigint,
    status character varying(20) DEFAULT 'SUCCESS'::character varying,
    error_message text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_operation_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_operation_log IS '操作日志表';


--
-- Name: sys_operation_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_operation_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_operation_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_operation_log_id_seq OWNED BY public.sys_operation_log.id;


--
-- Name: sys_permission_change_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_permission_change_log (
    id bigint NOT NULL,
    role_id bigint NOT NULL,
    role_code character varying(50) NOT NULL,
    change_type character varying(20) NOT NULL,
    permission_code character varying(100) NOT NULL,
    permission_name character varying(100),
    change_reason text,
    changed_by bigint,
    changed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_permission_change_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_permission_change_log IS '权限变更历史表';


--
-- Name: COLUMN sys_permission_change_log.role_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission_change_log.role_id IS '角色ID';


--
-- Name: COLUMN sys_permission_change_log.role_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission_change_log.role_code IS '角色代码';


--
-- Name: COLUMN sys_permission_change_log.change_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission_change_log.change_type IS '变更类型：ADD-新增权限, REMOVE-移除权限';


--
-- Name: COLUMN sys_permission_change_log.permission_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission_change_log.permission_code IS '权限代码';


--
-- Name: COLUMN sys_permission_change_log.permission_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission_change_log.permission_name IS '权限名称';


--
-- Name: COLUMN sys_permission_change_log.change_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission_change_log.change_reason IS '变更原因';


--
-- Name: COLUMN sys_permission_change_log.changed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission_change_log.changed_by IS '变更人ID';


--
-- Name: COLUMN sys_permission_change_log.changed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission_change_log.changed_at IS '变更时间';


--
-- Name: sys_permission_change_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_permission_change_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_permission_change_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_permission_change_log_id_seq OWNED BY public.sys_permission_change_log.id;


--
-- Name: sys_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_role (
    id bigint NOT NULL,
    role_code character varying(50) NOT NULL,
    role_name character varying(100) NOT NULL,
    description character varying(500),
    data_scope character varying(20) DEFAULT 'SELF'::character varying,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_role IS '角色表';


--
-- Name: COLUMN sys_role.role_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.role_code IS '角色编码';


--
-- Name: COLUMN sys_role.role_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.role_name IS '角色名称';


--
-- Name: COLUMN sys_role.data_scope; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.data_scope IS '数据范围: ALL, DEPT, DEPT_AND_CHILD, SELF';


--
-- Name: sys_role_change_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_role_change_log (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    username character varying(50) NOT NULL,
    old_role_ids text,
    old_role_codes text,
    new_role_ids text,
    new_role_codes text,
    change_type character varying(20) NOT NULL,
    change_reason text,
    pending_business_count integer DEFAULT 0,
    changed_by bigint,
    changed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_role_change_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_role_change_log IS '角色变更历史表';


--
-- Name: COLUMN sys_role_change_log.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.user_id IS '用户ID';


--
-- Name: COLUMN sys_role_change_log.username; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.username IS '用户名';


--
-- Name: COLUMN sys_role_change_log.old_role_ids; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.old_role_ids IS '旧角色ID列表（JSON格式）';


--
-- Name: COLUMN sys_role_change_log.old_role_codes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.old_role_codes IS '旧角色代码列表（JSON格式）';


--
-- Name: COLUMN sys_role_change_log.new_role_ids; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.new_role_ids IS '新角色ID列表（JSON格式）';


--
-- Name: COLUMN sys_role_change_log.new_role_codes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.new_role_codes IS '新角色代码列表（JSON格式）';


--
-- Name: COLUMN sys_role_change_log.change_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.change_type IS '变更类型：UPGRADE-权限扩大, DOWNGRADE-权限缩小, TRANSFER-跨部门/跨角色';


--
-- Name: COLUMN sys_role_change_log.change_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.change_reason IS '变更原因';


--
-- Name: COLUMN sys_role_change_log.pending_business_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.pending_business_count IS '待处理业务数量';


--
-- Name: COLUMN sys_role_change_log.changed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.changed_by IS '变更人ID';


--
-- Name: COLUMN sys_role_change_log.changed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_change_log.changed_at IS '变更时间';


--
-- Name: sys_role_change_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_role_change_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_role_change_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_role_change_log_id_seq OWNED BY public.sys_role_change_log.id;


--
-- Name: sys_role_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_role_id_seq OWNED BY public.sys_role.id;


--
-- Name: sys_role_menu; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_role_menu (
    id bigint NOT NULL,
    role_id bigint NOT NULL,
    menu_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_role_menu; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_role_menu IS '角色菜单关联表';


--
-- Name: COLUMN sys_role_menu.role_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.role_id IS '角色ID';


--
-- Name: COLUMN sys_role_menu.menu_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role_menu.menu_id IS '菜单ID';


--
-- Name: sys_role_menu_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_role_menu_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_role_menu_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_role_menu_id_seq OWNED BY public.sys_role_menu.id;


--
-- Name: sys_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(200) NOT NULL,
    real_name character varying(50),
    email character varying(100),
    phone character varying(20),
    avatar_url character varying(500),
    department_id bigint,
    "position" character varying(50),
    employee_no character varying(50),
    lawyer_license_no character varying(50),
    join_date date,
    compensation_type character varying(20) DEFAULT 'COMMISSION'::character varying,
    can_be_originator boolean DEFAULT true,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    last_login_at timestamp without time zone,
    last_login_ip character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_user; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_user IS '用户表';


--
-- Name: COLUMN sys_user.username; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.username IS '用户名';


--
-- Name: COLUMN sys_user.password; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.password IS '密码';


--
-- Name: COLUMN sys_user.real_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.real_name IS '真实姓名';


--
-- Name: COLUMN sys_user.compensation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.compensation_type IS '薪酬模式: COMMISSION-提成制, SALARIED-授薪制, HYBRID-混合制';


--
-- Name: COLUMN sys_user.can_be_originator; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.can_be_originator IS '是否可作为案源人';


--
-- Name: COLUMN sys_user.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user.status IS '状态: ACTIVE, INACTIVE, LOCKED';


--
-- Name: sys_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_user_id_seq OWNED BY public.sys_user.id;


--
-- Name: sys_user_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user_role (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_user_role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_user_role IS '用户角色关联表';


--
-- Name: COLUMN sys_user_role.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.user_id IS '用户ID';


--
-- Name: COLUMN sys_user_role.role_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_role.role_id IS '角色ID';


--
-- Name: sys_user_role_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_user_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_user_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_user_role_id_seq OWNED BY public.sys_user_role.id;


--
-- Name: sys_user_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user_session (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    username character varying(50) NOT NULL,
    token character varying(2000) NOT NULL,
    refresh_token character varying(2000),
    ip_address character varying(50),
    user_agent character varying(2000),
    device_type character varying(50),
    browser character varying(100),
    os character varying(100),
    location character varying(200),
    login_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_access_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    expire_time timestamp without time zone NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    is_current boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE sys_user_session; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_user_session IS '用户会话表';


--
-- Name: COLUMN sys_user_session.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_session.user_id IS '用户ID';


--
-- Name: COLUMN sys_user_session.token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_session.token IS 'JWT访问令牌';


--
-- Name: COLUMN sys_user_session.refresh_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_session.refresh_token IS '刷新令牌';


--
-- Name: COLUMN sys_user_session.ip_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_session.ip_address IS '登录IP地址';


--
-- Name: COLUMN sys_user_session.user_agent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_session.user_agent IS '用户代理';


--
-- Name: COLUMN sys_user_session.device_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_session.device_type IS '设备类型';


--
-- Name: COLUMN sys_user_session.expire_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_session.expire_time IS '过期时间';


--
-- Name: COLUMN sys_user_session.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_session.status IS '会话状态';


--
-- Name: COLUMN sys_user_session.is_current; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_session.is_current IS '是否为当前会话';


--
-- Name: sys_user_session_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_user_session_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_user_session_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_user_session_id_seq OWNED BY public.sys_user_session.id;


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
-- Name: timer_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.timer_session (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    matter_id bigint NOT NULL,
    work_type character varying(50),
    work_content text,
    billable boolean DEFAULT true,
    start_time timestamp without time zone NOT NULL,
    pause_time timestamp without time zone,
    resume_time timestamp without time zone,
    elapsed_seconds bigint DEFAULT 0,
    status character varying(20) DEFAULT 'RUNNING'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE timer_session; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.timer_session IS '计时器会话表';


--
-- Name: COLUMN timer_session.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.user_id IS '用户ID';


--
-- Name: COLUMN timer_session.matter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.matter_id IS '案件ID';


--
-- Name: COLUMN timer_session.work_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.work_type IS '工作类型';


--
-- Name: COLUMN timer_session.work_content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.work_content IS '工作内容';


--
-- Name: COLUMN timer_session.billable; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.billable IS '是否计费';


--
-- Name: COLUMN timer_session.start_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.start_time IS '开始时间';


--
-- Name: COLUMN timer_session.pause_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.pause_time IS '暂停时间';


--
-- Name: COLUMN timer_session.resume_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.resume_time IS '恢复时间';


--
-- Name: COLUMN timer_session.elapsed_seconds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.elapsed_seconds IS '已累计的秒数';


--
-- Name: COLUMN timer_session.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timer_session.status IS '状态: RUNNING-运行中, PAUSED-已暂停, STOPPED-已停止';


--
-- Name: timer_session_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.timer_session_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: timer_session_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.timer_session_id_seq OWNED BY public.timer_session.id;


--
-- Name: timesheet; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.timesheet (
    id bigint NOT NULL,
    timesheet_no character varying(50) NOT NULL,
    matter_id bigint NOT NULL,
    user_id bigint NOT NULL,
    work_date date NOT NULL,
    hours numeric(5,2) NOT NULL,
    work_type character varying(50),
    work_content text NOT NULL,
    billable boolean DEFAULT true,
    hourly_rate numeric(10,2),
    amount numeric(15,2),
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    submitted_at timestamp without time zone,
    approved_by bigint,
    approved_at timestamp without time zone,
    approval_comment text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


--
-- Name: TABLE timesheet; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.timesheet IS '工时记录表';


--
-- Name: COLUMN timesheet.work_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timesheet.work_type IS '工作类型: RESEARCH-法律研究, DRAFTING-文书起草, MEETING-会议, COURT-出庭, NEGOTIATION-谈判, COMMUNICATION-沟通, TRAVEL-差旅, OTHER-其他';


--
-- Name: COLUMN timesheet.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.timesheet.status IS '状态: DRAFT-草稿, SUBMITTED-已提交, APPROVED-已批准, REJECTED-已拒绝';


--
-- Name: timesheet_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.timesheet_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: timesheet_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.timesheet_id_seq OWNED BY public.timesheet.id;


--
-- Name: timesheet_summary; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.timesheet_summary (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    matter_id bigint,
    year integer NOT NULL,
    month integer NOT NULL,
    total_hours numeric(10,2) DEFAULT 0,
    billable_hours numeric(10,2) DEFAULT 0,
    non_billable_hours numeric(10,2) DEFAULT 0,
    total_amount numeric(15,2) DEFAULT 0,
    draft_count integer DEFAULT 0,
    submitted_count integer DEFAULT 0,
    approved_count integer DEFAULT 0,
    rejected_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE timesheet_summary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.timesheet_summary IS '工时汇总表';


--
-- Name: timesheet_summary_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.timesheet_summary_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: timesheet_summary_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.timesheet_summary_id_seq OWNED BY public.timesheet_summary.id;


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
-- Name: archive id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive ALTER COLUMN id SET DEFAULT nextval('public.archive_id_seq'::regclass);


--
-- Name: archive_borrow id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_borrow ALTER COLUMN id SET DEFAULT nextval('public.archive_borrow_id_seq'::regclass);


--
-- Name: archive_data_source id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_data_source ALTER COLUMN id SET DEFAULT nextval('public.archive_data_source_id_seq'::regclass);


--
-- Name: archive_location id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_location ALTER COLUMN id SET DEFAULT nextval('public.archive_location_id_seq'::regclass);


--
-- Name: archive_operation_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_operation_log ALTER COLUMN id SET DEFAULT nextval('public.archive_operation_log_id_seq'::regclass);


--
-- Name: article_collection id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.article_collection ALTER COLUMN id SET DEFAULT nextval('public.article_collection_id_seq'::regclass);


--
-- Name: article_comment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.article_comment ALTER COLUMN id SET DEFAULT nextval('public.article_comment_id_seq'::regclass);


--
-- Name: asset_inventory id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_inventory ALTER COLUMN id SET DEFAULT nextval('public.asset_inventory_id_seq'::regclass);


--
-- Name: asset_inventory_detail id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_inventory_detail ALTER COLUMN id SET DEFAULT nextval('public.asset_inventory_detail_id_seq'::regclass);


--
-- Name: attendance id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance ALTER COLUMN id SET DEFAULT nextval('public.attendance_id_seq'::regclass);


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
-- Name: contract_participant id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant ALTER COLUMN id SET DEFAULT nextval('public.contract_participant_id_seq'::regclass);


--
-- Name: contract_payment_schedule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_payment_schedule ALTER COLUMN id SET DEFAULT nextval('public.contract_payment_schedule_id_seq'::regclass);


--
-- Name: contract_template id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_template ALTER COLUMN id SET DEFAULT nextval('public.contract_template_id_seq'::regclass);


--
-- Name: crm_client id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client ALTER COLUMN id SET DEFAULT nextval('public.crm_client_id_seq'::regclass);


--
-- Name: crm_client_change_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_change_history ALTER COLUMN id SET DEFAULT nextval('public.crm_client_change_history_id_seq'::regclass);


--
-- Name: crm_client_contact_record id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_contact_record ALTER COLUMN id SET DEFAULT nextval('public.crm_client_contact_record_id_seq'::regclass);


--
-- Name: crm_client_related_company id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_related_company ALTER COLUMN id SET DEFAULT nextval('public.crm_client_related_company_id_seq'::regclass);


--
-- Name: crm_client_shareholder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_shareholder ALTER COLUMN id SET DEFAULT nextval('public.crm_client_shareholder_id_seq'::regclass);


--
-- Name: crm_client_tag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_tag ALTER COLUMN id SET DEFAULT nextval('public.crm_client_tag_id_seq'::regclass);


--
-- Name: crm_client_tag_relation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_tag_relation ALTER COLUMN id SET DEFAULT nextval('public.crm_client_tag_relation_id_seq'::regclass);


--
-- Name: crm_conflict_check id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_conflict_check ALTER COLUMN id SET DEFAULT nextval('public.crm_conflict_check_id_seq'::regclass);


--
-- Name: crm_conflict_check_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_conflict_check_item ALTER COLUMN id SET DEFAULT nextval('public.crm_conflict_check_item_id_seq'::regclass);


--
-- Name: crm_lead id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_lead ALTER COLUMN id SET DEFAULT nextval('public.crm_lead_id_seq'::regclass);


--
-- Name: crm_lead_follow_up id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_lead_follow_up ALTER COLUMN id SET DEFAULT nextval('public.crm_lead_follow_up_id_seq'::regclass);


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
-- Name: evidence id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence ALTER COLUMN id SET DEFAULT nextval('public.evidence_id_seq'::regclass);


--
-- Name: evidence_cross_exam id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_cross_exam ALTER COLUMN id SET DEFAULT nextval('public.evidence_cross_exam_id_seq'::regclass);


--
-- Name: evidence_list id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_list ALTER COLUMN id SET DEFAULT nextval('public.evidence_list_id_seq'::regclass);


--
-- Name: fin_payment_amendment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment ALTER COLUMN id SET DEFAULT nextval('public.fin_payment_amendment_id_seq'::regclass);


--
-- Name: finance_commission id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission ALTER COLUMN id SET DEFAULT nextval('public.finance_commission_id_seq'::regclass);


--
-- Name: finance_commission_detail id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_detail ALTER COLUMN id SET DEFAULT nextval('public.finance_commission_detail_id_seq'::regclass);


--
-- Name: finance_commission_rule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_rule ALTER COLUMN id SET DEFAULT nextval('public.finance_commission_rule_id_seq'::regclass);


--
-- Name: finance_contract id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract ALTER COLUMN id SET DEFAULT nextval('public.finance_contract_id_seq'::regclass);


--
-- Name: finance_contract_amendment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract_amendment ALTER COLUMN id SET DEFAULT nextval('public.finance_contract_amendment_id_seq'::regclass);


--
-- Name: finance_cost_allocation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_cost_allocation ALTER COLUMN id SET DEFAULT nextval('public.finance_cost_allocation_id_seq'::regclass);


--
-- Name: finance_cost_split id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_cost_split ALTER COLUMN id SET DEFAULT nextval('public.finance_cost_split_id_seq'::regclass);


--
-- Name: finance_expense id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_expense ALTER COLUMN id SET DEFAULT nextval('public.finance_expense_id_seq'::regclass);


--
-- Name: finance_fee id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_fee ALTER COLUMN id SET DEFAULT nextval('public.finance_fee_id_seq'::regclass);


--
-- Name: finance_invoice id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_invoice ALTER COLUMN id SET DEFAULT nextval('public.finance_invoice_id_seq'::regclass);


--
-- Name: finance_payment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_payment ALTER COLUMN id SET DEFAULT nextval('public.finance_payment_id_seq'::regclass);


--
-- Name: go_out_record id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.go_out_record ALTER COLUMN id SET DEFAULT nextval('public.go_out_record_id_seq'::regclass);


--
-- Name: hourly_rate id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hourly_rate ALTER COLUMN id SET DEFAULT nextval('public.hourly_rate_id_seq'::regclass);


--
-- Name: hr_career_level id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_career_level ALTER COLUMN id SET DEFAULT nextval('public.hr_career_level_id_seq'::regclass);


--
-- Name: hr_development_milestone id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_development_milestone ALTER COLUMN id SET DEFAULT nextval('public.hr_development_milestone_id_seq'::regclass);


--
-- Name: hr_development_plan id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_development_plan ALTER COLUMN id SET DEFAULT nextval('public.hr_development_plan_id_seq'::regclass);


--
-- Name: hr_employee id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee ALTER COLUMN id SET DEFAULT nextval('public.hr_employee_id_seq'::regclass);


--
-- Name: hr_payroll_deduction id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_deduction ALTER COLUMN id SET DEFAULT nextval('public.hr_payroll_deduction_id_seq'::regclass);


--
-- Name: hr_payroll_income id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_income ALTER COLUMN id SET DEFAULT nextval('public.hr_payroll_income_id_seq'::regclass);


--
-- Name: hr_payroll_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_item ALTER COLUMN id SET DEFAULT nextval('public.hr_payroll_item_id_seq'::regclass);


--
-- Name: hr_payroll_sheet id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_sheet ALTER COLUMN id SET DEFAULT nextval('public.hr_payroll_sheet_id_seq'::regclass);


--
-- Name: hr_performance_evaluation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_evaluation ALTER COLUMN id SET DEFAULT nextval('public.hr_performance_evaluation_id_seq'::regclass);


--
-- Name: hr_performance_indicator id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_indicator ALTER COLUMN id SET DEFAULT nextval('public.hr_performance_indicator_id_seq'::regclass);


--
-- Name: hr_performance_score id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_score ALTER COLUMN id SET DEFAULT nextval('public.hr_performance_score_id_seq'::regclass);


--
-- Name: hr_performance_task id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_task ALTER COLUMN id SET DEFAULT nextval('public.hr_performance_task_id_seq'::regclass);


--
-- Name: hr_promotion_application id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_promotion_application ALTER COLUMN id SET DEFAULT nextval('public.hr_promotion_application_id_seq'::regclass);


--
-- Name: hr_promotion_review id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_promotion_review ALTER COLUMN id SET DEFAULT nextval('public.hr_promotion_review_id_seq'::regclass);


--
-- Name: hr_regularization id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_regularization ALTER COLUMN id SET DEFAULT nextval('public.hr_regularization_id_seq'::regclass);


--
-- Name: hr_resignation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_resignation ALTER COLUMN id SET DEFAULT nextval('public.hr_resignation_id_seq'::regclass);


--
-- Name: hr_training id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_training ALTER COLUMN id SET DEFAULT nextval('public.hr_training_id_seq'::regclass);


--
-- Name: hr_training_record id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_training_record ALTER COLUMN id SET DEFAULT nextval('public.hr_training_record_id_seq'::regclass);


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
-- Name: leave_application id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_application ALTER COLUMN id SET DEFAULT nextval('public.leave_application_id_seq'::regclass);


--
-- Name: leave_balance id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_balance ALTER COLUMN id SET DEFAULT nextval('public.leave_balance_id_seq'::regclass);


--
-- Name: leave_type id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_type ALTER COLUMN id SET DEFAULT nextval('public.leave_type_id_seq'::regclass);


--
-- Name: letter_application id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.letter_application ALTER COLUMN id SET DEFAULT nextval('public.letter_application_id_seq'::regclass);


--
-- Name: letter_template id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.letter_template ALTER COLUMN id SET DEFAULT nextval('public.letter_template_id_seq'::regclass);


--
-- Name: matter id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter ALTER COLUMN id SET DEFAULT nextval('public.matter_id_seq'::regclass);


--
-- Name: matter_client id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_client ALTER COLUMN id SET DEFAULT nextval('public.matter_client_id_seq'::regclass);


--
-- Name: matter_deadline id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_deadline ALTER COLUMN id SET DEFAULT nextval('public.matter_deadline_id_seq'::regclass);


--
-- Name: matter_dossier_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_dossier_item ALTER COLUMN id SET DEFAULT nextval('public.matter_dossier_item_id_seq'::regclass);


--
-- Name: matter_participant id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_participant ALTER COLUMN id SET DEFAULT nextval('public.matter_participant_id_seq'::regclass);


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
-- Name: overtime_application id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overtime_application ALTER COLUMN id SET DEFAULT nextval('public.overtime_application_id_seq'::regclass);


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
-- Name: schedule id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedule ALTER COLUMN id SET DEFAULT nextval('public.schedule_id_seq'::regclass);


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
-- Name: sys_announcement id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_announcement ALTER COLUMN id SET DEFAULT nextval('public.sys_announcement_id_seq'::regclass);


--
-- Name: sys_backup id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_backup ALTER COLUMN id SET DEFAULT nextval('public.sys_backup_id_seq'::regclass);


--
-- Name: sys_config id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_config ALTER COLUMN id SET DEFAULT nextval('public.sys_config_id_seq'::regclass);


--
-- Name: sys_data_handover id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover ALTER COLUMN id SET DEFAULT nextval('public.sys_data_handover_id_seq'::regclass);


--
-- Name: sys_data_handover_detail id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover_detail ALTER COLUMN id SET DEFAULT nextval('public.sys_data_handover_detail_id_seq'::regclass);


--
-- Name: sys_data_sync_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_sync_log ALTER COLUMN id SET DEFAULT nextval('public.sys_data_sync_log_id_seq'::regclass);


--
-- Name: sys_department id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_department ALTER COLUMN id SET DEFAULT nextval('public.sys_department_id_seq'::regclass);


--
-- Name: sys_dict_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dict_item ALTER COLUMN id SET DEFAULT nextval('public.sys_dict_item_id_seq'::regclass);


--
-- Name: sys_dict_type id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dict_type ALTER COLUMN id SET DEFAULT nextval('public.sys_dict_type_id_seq'::regclass);


--
-- Name: sys_export_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_export_log ALTER COLUMN id SET DEFAULT nextval('public.sys_export_log_id_seq'::regclass);


--
-- Name: sys_external_integration id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_external_integration ALTER COLUMN id SET DEFAULT nextval('public.sys_external_integration_id_seq'::regclass);


--
-- Name: sys_login_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_login_log ALTER COLUMN id SET DEFAULT nextval('public.sys_login_log_id_seq'::regclass);


--
-- Name: sys_menu id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_menu ALTER COLUMN id SET DEFAULT nextval('public.sys_menu_id_seq'::regclass);


--
-- Name: sys_notification id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_notification ALTER COLUMN id SET DEFAULT nextval('public.sys_notification_id_seq'::regclass);


--
-- Name: sys_operation_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_operation_log ALTER COLUMN id SET DEFAULT nextval('public.sys_operation_log_id_seq'::regclass);


--
-- Name: sys_permission_change_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_permission_change_log ALTER COLUMN id SET DEFAULT nextval('public.sys_permission_change_log_id_seq'::regclass);


--
-- Name: sys_role id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role ALTER COLUMN id SET DEFAULT nextval('public.sys_role_id_seq'::regclass);


--
-- Name: sys_role_change_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_change_log ALTER COLUMN id SET DEFAULT nextval('public.sys_role_change_log_id_seq'::regclass);


--
-- Name: sys_role_menu id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_menu ALTER COLUMN id SET DEFAULT nextval('public.sys_role_menu_id_seq'::regclass);


--
-- Name: sys_user id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user ALTER COLUMN id SET DEFAULT nextval('public.sys_user_id_seq'::regclass);


--
-- Name: sys_user_role id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_role ALTER COLUMN id SET DEFAULT nextval('public.sys_user_role_id_seq'::regclass);


--
-- Name: sys_user_session id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_session ALTER COLUMN id SET DEFAULT nextval('public.sys_user_session_id_seq'::regclass);


--
-- Name: task id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task ALTER COLUMN id SET DEFAULT nextval('public.task_id_seq'::regclass);


--
-- Name: task_comment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_comment ALTER COLUMN id SET DEFAULT nextval('public.task_comment_id_seq'::regclass);


--
-- Name: timer_session id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timer_session ALTER COLUMN id SET DEFAULT nextval('public.timer_session_id_seq'::regclass);


--
-- Name: timesheet id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet ALTER COLUMN id SET DEFAULT nextval('public.timesheet_id_seq'::regclass);


--
-- Name: timesheet_summary id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet_summary ALTER COLUMN id SET DEFAULT nextval('public.timesheet_summary_id_seq'::regclass);


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
-- Name: archive archive_archive_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive
    ADD CONSTRAINT archive_archive_no_key UNIQUE (archive_no);


--
-- Name: archive_borrow archive_borrow_borrow_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_borrow
    ADD CONSTRAINT archive_borrow_borrow_no_key UNIQUE (borrow_no);


--
-- Name: archive_borrow archive_borrow_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_borrow
    ADD CONSTRAINT archive_borrow_pkey PRIMARY KEY (id);


--
-- Name: archive_data_source archive_data_source_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_data_source
    ADD CONSTRAINT archive_data_source_pkey PRIMARY KEY (id);


--
-- Name: archive_location archive_location_location_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_location
    ADD CONSTRAINT archive_location_location_code_key UNIQUE (location_code);


--
-- Name: archive_location archive_location_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_location
    ADD CONSTRAINT archive_location_pkey PRIMARY KEY (id);


--
-- Name: archive_operation_log archive_operation_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_operation_log
    ADD CONSTRAINT archive_operation_log_pkey PRIMARY KEY (id);


--
-- Name: archive archive_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive
    ADD CONSTRAINT archive_pkey PRIMARY KEY (id);


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
-- Name: attendance attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_pkey PRIMARY KEY (id);


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
-- Name: contract_participant contract_participant_contract_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant
    ADD CONSTRAINT contract_participant_contract_id_user_id_key UNIQUE (contract_id, user_id);


--
-- Name: contract_participant contract_participant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant
    ADD CONSTRAINT contract_participant_pkey PRIMARY KEY (id);


--
-- Name: contract_payment_schedule contract_payment_schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_payment_schedule
    ADD CONSTRAINT contract_payment_schedule_pkey PRIMARY KEY (id);


--
-- Name: contract_template contract_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_template
    ADD CONSTRAINT contract_template_pkey PRIMARY KEY (id);


--
-- Name: contract_template contract_template_template_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_template
    ADD CONSTRAINT contract_template_template_no_key UNIQUE (template_no);


--
-- Name: crm_client_change_history crm_client_change_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_change_history
    ADD CONSTRAINT crm_client_change_history_pkey PRIMARY KEY (id);


--
-- Name: crm_client crm_client_client_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client
    ADD CONSTRAINT crm_client_client_no_key UNIQUE (client_no);


--
-- Name: crm_client_contact_record crm_client_contact_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_contact_record
    ADD CONSTRAINT crm_client_contact_record_pkey PRIMARY KEY (id);


--
-- Name: crm_client crm_client_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client
    ADD CONSTRAINT crm_client_pkey PRIMARY KEY (id);


--
-- Name: crm_client_related_company crm_client_related_company_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_related_company
    ADD CONSTRAINT crm_client_related_company_pkey PRIMARY KEY (id);


--
-- Name: crm_client_shareholder crm_client_shareholder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_shareholder
    ADD CONSTRAINT crm_client_shareholder_pkey PRIMARY KEY (id);


--
-- Name: crm_client_tag crm_client_tag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_tag
    ADD CONSTRAINT crm_client_tag_pkey PRIMARY KEY (id);


--
-- Name: crm_client_tag_relation crm_client_tag_relation_client_id_tag_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_tag_relation
    ADD CONSTRAINT crm_client_tag_relation_client_id_tag_id_key UNIQUE (client_id, tag_id);


--
-- Name: crm_client_tag_relation crm_client_tag_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_tag_relation
    ADD CONSTRAINT crm_client_tag_relation_pkey PRIMARY KEY (id);


--
-- Name: crm_client_tag crm_client_tag_tag_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_client_tag
    ADD CONSTRAINT crm_client_tag_tag_name_key UNIQUE (tag_name);


--
-- Name: crm_conflict_check crm_conflict_check_check_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_conflict_check
    ADD CONSTRAINT crm_conflict_check_check_no_key UNIQUE (check_no);


--
-- Name: crm_conflict_check_item crm_conflict_check_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_conflict_check_item
    ADD CONSTRAINT crm_conflict_check_item_pkey PRIMARY KEY (id);


--
-- Name: crm_conflict_check crm_conflict_check_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_conflict_check
    ADD CONSTRAINT crm_conflict_check_pkey PRIMARY KEY (id);


--
-- Name: crm_lead_follow_up crm_lead_follow_up_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_lead_follow_up
    ADD CONSTRAINT crm_lead_follow_up_pkey PRIMARY KEY (id);


--
-- Name: crm_lead crm_lead_lead_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_lead
    ADD CONSTRAINT crm_lead_lead_no_key UNIQUE (lead_no);


--
-- Name: crm_lead crm_lead_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_lead
    ADD CONSTRAINT crm_lead_pkey PRIMARY KEY (id);


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
-- Name: dossier_template_item dossier_template_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossier_template_item
    ADD CONSTRAINT dossier_template_item_pkey PRIMARY KEY (id);


--
-- Name: dossier_template dossier_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossier_template
    ADD CONSTRAINT dossier_template_pkey PRIMARY KEY (id);


--
-- Name: evidence_cross_exam evidence_cross_exam_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_cross_exam
    ADD CONSTRAINT evidence_cross_exam_pkey PRIMARY KEY (id);


--
-- Name: evidence evidence_evidence_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_evidence_no_key UNIQUE (evidence_no);


--
-- Name: evidence_list evidence_list_list_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_list
    ADD CONSTRAINT evidence_list_list_no_key UNIQUE (list_no);


--
-- Name: evidence_list evidence_list_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence_list
    ADD CONSTRAINT evidence_list_pkey PRIMARY KEY (id);


--
-- Name: evidence evidence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_pkey PRIMARY KEY (id);


--
-- Name: fin_payment_amendment fin_payment_amendment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment
    ADD CONSTRAINT fin_payment_amendment_pkey PRIMARY KEY (id);


--
-- Name: finance_commission finance_commission_commission_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission
    ADD CONSTRAINT finance_commission_commission_no_key UNIQUE (commission_no);


--
-- Name: finance_commission_detail finance_commission_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_detail
    ADD CONSTRAINT finance_commission_detail_pkey PRIMARY KEY (id);


--
-- Name: finance_commission finance_commission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission
    ADD CONSTRAINT finance_commission_pkey PRIMARY KEY (id);


--
-- Name: finance_commission_rule finance_commission_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_rule
    ADD CONSTRAINT finance_commission_rule_pkey PRIMARY KEY (id);


--
-- Name: finance_commission_rule finance_commission_rule_rule_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_rule
    ADD CONSTRAINT finance_commission_rule_rule_code_key UNIQUE (rule_code);


--
-- Name: finance_contract_amendment finance_contract_amendment_amendment_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract_amendment
    ADD CONSTRAINT finance_contract_amendment_amendment_no_key UNIQUE (amendment_no);


--
-- Name: finance_contract_amendment finance_contract_amendment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract_amendment
    ADD CONSTRAINT finance_contract_amendment_pkey PRIMARY KEY (id);


--
-- Name: finance_contract finance_contract_contract_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract
    ADD CONSTRAINT finance_contract_contract_no_key UNIQUE (contract_no);


--
-- Name: finance_contract finance_contract_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_contract
    ADD CONSTRAINT finance_contract_pkey PRIMARY KEY (id);


--
-- Name: finance_cost_allocation finance_cost_allocation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_cost_allocation
    ADD CONSTRAINT finance_cost_allocation_pkey PRIMARY KEY (id);


--
-- Name: finance_cost_split finance_cost_split_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_cost_split
    ADD CONSTRAINT finance_cost_split_pkey PRIMARY KEY (id);


--
-- Name: finance_expense finance_expense_expense_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_expense
    ADD CONSTRAINT finance_expense_expense_no_key UNIQUE (expense_no);


--
-- Name: finance_expense finance_expense_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_expense
    ADD CONSTRAINT finance_expense_pkey PRIMARY KEY (id);


--
-- Name: finance_fee finance_fee_fee_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_fee
    ADD CONSTRAINT finance_fee_fee_no_key UNIQUE (fee_no);


--
-- Name: finance_fee finance_fee_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_fee
    ADD CONSTRAINT finance_fee_pkey PRIMARY KEY (id);


--
-- Name: finance_invoice finance_invoice_invoice_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_invoice
    ADD CONSTRAINT finance_invoice_invoice_no_key UNIQUE (invoice_no);


--
-- Name: finance_invoice finance_invoice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_invoice
    ADD CONSTRAINT finance_invoice_pkey PRIMARY KEY (id);


--
-- Name: finance_payment finance_payment_payment_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_payment
    ADD CONSTRAINT finance_payment_payment_no_key UNIQUE (payment_no);


--
-- Name: finance_payment finance_payment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_payment
    ADD CONSTRAINT finance_payment_pkey PRIMARY KEY (id);


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
-- Name: hourly_rate hourly_rate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hourly_rate
    ADD CONSTRAINT hourly_rate_pkey PRIMARY KEY (id);


--
-- Name: hr_career_level hr_career_level_level_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_career_level
    ADD CONSTRAINT hr_career_level_level_code_key UNIQUE (level_code);


--
-- Name: hr_career_level hr_career_level_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_career_level
    ADD CONSTRAINT hr_career_level_pkey PRIMARY KEY (id);


--
-- Name: hr_development_milestone hr_development_milestone_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_development_milestone
    ADD CONSTRAINT hr_development_milestone_pkey PRIMARY KEY (id);


--
-- Name: hr_development_plan hr_development_plan_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_development_plan
    ADD CONSTRAINT hr_development_plan_pkey PRIMARY KEY (id);


--
-- Name: hr_development_plan hr_development_plan_plan_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_development_plan
    ADD CONSTRAINT hr_development_plan_plan_no_key UNIQUE (plan_no);


--
-- Name: hr_employee hr_employee_employee_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee
    ADD CONSTRAINT hr_employee_employee_no_key UNIQUE (employee_no);


--
-- Name: hr_employee hr_employee_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee
    ADD CONSTRAINT hr_employee_pkey PRIMARY KEY (id);


--
-- Name: hr_payroll_deduction hr_payroll_deduction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_deduction
    ADD CONSTRAINT hr_payroll_deduction_pkey PRIMARY KEY (id);


--
-- Name: hr_payroll_income hr_payroll_income_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_income
    ADD CONSTRAINT hr_payroll_income_pkey PRIMARY KEY (id);


--
-- Name: hr_payroll_item hr_payroll_item_payroll_sheet_id_employee_id_deleted_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_item
    ADD CONSTRAINT hr_payroll_item_payroll_sheet_id_employee_id_deleted_key UNIQUE (payroll_sheet_id, employee_id, deleted);


--
-- Name: hr_payroll_item hr_payroll_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_item
    ADD CONSTRAINT hr_payroll_item_pkey PRIMARY KEY (id);


--
-- Name: hr_payroll_sheet hr_payroll_sheet_payroll_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_sheet
    ADD CONSTRAINT hr_payroll_sheet_payroll_no_key UNIQUE (payroll_no);


--
-- Name: hr_payroll_sheet hr_payroll_sheet_payroll_year_payroll_month_deleted_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_sheet
    ADD CONSTRAINT hr_payroll_sheet_payroll_year_payroll_month_deleted_key UNIQUE (payroll_year, payroll_month, deleted);


--
-- Name: hr_payroll_sheet hr_payroll_sheet_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_sheet
    ADD CONSTRAINT hr_payroll_sheet_pkey PRIMARY KEY (id);


--
-- Name: hr_performance_evaluation hr_performance_evaluation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_evaluation
    ADD CONSTRAINT hr_performance_evaluation_pkey PRIMARY KEY (id);


--
-- Name: hr_performance_indicator hr_performance_indicator_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_indicator
    ADD CONSTRAINT hr_performance_indicator_pkey PRIMARY KEY (id);


--
-- Name: hr_performance_score hr_performance_score_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_score
    ADD CONSTRAINT hr_performance_score_pkey PRIMARY KEY (id);


--
-- Name: hr_performance_task hr_performance_task_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_task
    ADD CONSTRAINT hr_performance_task_pkey PRIMARY KEY (id);


--
-- Name: hr_promotion_application hr_promotion_application_application_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_promotion_application
    ADD CONSTRAINT hr_promotion_application_application_no_key UNIQUE (application_no);


--
-- Name: hr_promotion_application hr_promotion_application_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_promotion_application
    ADD CONSTRAINT hr_promotion_application_pkey PRIMARY KEY (id);


--
-- Name: hr_promotion_review hr_promotion_review_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_promotion_review
    ADD CONSTRAINT hr_promotion_review_pkey PRIMARY KEY (id);


--
-- Name: hr_regularization hr_regularization_application_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_regularization
    ADD CONSTRAINT hr_regularization_application_no_key UNIQUE (application_no);


--
-- Name: hr_regularization hr_regularization_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_regularization
    ADD CONSTRAINT hr_regularization_pkey PRIMARY KEY (id);


--
-- Name: hr_resignation hr_resignation_application_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_resignation
    ADD CONSTRAINT hr_resignation_application_no_key UNIQUE (application_no);


--
-- Name: hr_resignation hr_resignation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_resignation
    ADD CONSTRAINT hr_resignation_pkey PRIMARY KEY (id);


--
-- Name: hr_training hr_training_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_training
    ADD CONSTRAINT hr_training_pkey PRIMARY KEY (id);


--
-- Name: hr_training_record hr_training_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_training_record
    ADD CONSTRAINT hr_training_record_pkey PRIMARY KEY (id);


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
-- Name: leave_application leave_application_application_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_application
    ADD CONSTRAINT leave_application_application_no_key UNIQUE (application_no);


--
-- Name: leave_application leave_application_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_application
    ADD CONSTRAINT leave_application_pkey PRIMARY KEY (id);


--
-- Name: leave_balance leave_balance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_balance
    ADD CONSTRAINT leave_balance_pkey PRIMARY KEY (id);


--
-- Name: leave_type leave_type_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_type
    ADD CONSTRAINT leave_type_code_key UNIQUE (code);


--
-- Name: leave_type leave_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_type
    ADD CONSTRAINT leave_type_pkey PRIMARY KEY (id);


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
-- Name: matter_client matter_client_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_client
    ADD CONSTRAINT matter_client_pkey PRIMARY KEY (id);


--
-- Name: matter_deadline matter_deadline_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_deadline
    ADD CONSTRAINT matter_deadline_pkey PRIMARY KEY (id);


--
-- Name: matter_dossier_item matter_dossier_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_dossier_item
    ADD CONSTRAINT matter_dossier_item_pkey PRIMARY KEY (id);


--
-- Name: matter matter_matter_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter
    ADD CONSTRAINT matter_matter_no_key UNIQUE (matter_no);


--
-- Name: matter_participant matter_participant_matter_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_participant
    ADD CONSTRAINT matter_participant_matter_id_user_id_key UNIQUE (matter_id, user_id);


--
-- Name: matter_participant matter_participant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_participant
    ADD CONSTRAINT matter_participant_pkey PRIMARY KEY (id);


--
-- Name: matter matter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter
    ADD CONSTRAINT matter_pkey PRIMARY KEY (id);


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
-- Name: overtime_application overtime_application_application_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overtime_application
    ADD CONSTRAINT overtime_application_application_no_key UNIQUE (application_no);


--
-- Name: overtime_application overtime_application_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overtime_application
    ADD CONSTRAINT overtime_application_pkey PRIMARY KEY (id);


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
-- Name: schedule schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedule
    ADD CONSTRAINT schedule_pkey PRIMARY KEY (id);


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
-- Name: sys_announcement sys_announcement_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_announcement
    ADD CONSTRAINT sys_announcement_pkey PRIMARY KEY (id);


--
-- Name: sys_backup sys_backup_backup_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_backup
    ADD CONSTRAINT sys_backup_backup_no_key UNIQUE (backup_no);


--
-- Name: sys_backup sys_backup_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_backup
    ADD CONSTRAINT sys_backup_pkey PRIMARY KEY (id);


--
-- Name: sys_config sys_config_config_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_config
    ADD CONSTRAINT sys_config_config_key_key UNIQUE (config_key);


--
-- Name: sys_config sys_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_config
    ADD CONSTRAINT sys_config_pkey PRIMARY KEY (id);


--
-- Name: sys_data_handover_detail sys_data_handover_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover_detail
    ADD CONSTRAINT sys_data_handover_detail_pkey PRIMARY KEY (id);


--
-- Name: sys_data_handover sys_data_handover_handover_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover
    ADD CONSTRAINT sys_data_handover_handover_no_key UNIQUE (handover_no);


--
-- Name: sys_data_handover sys_data_handover_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover
    ADD CONSTRAINT sys_data_handover_pkey PRIMARY KEY (id);


--
-- Name: sys_data_sync_log sys_data_sync_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_sync_log
    ADD CONSTRAINT sys_data_sync_log_pkey PRIMARY KEY (id);


--
-- Name: sys_department sys_department_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_department
    ADD CONSTRAINT sys_department_pkey PRIMARY KEY (id);


--
-- Name: sys_dict_item sys_dict_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dict_item
    ADD CONSTRAINT sys_dict_item_pkey PRIMARY KEY (id);


--
-- Name: sys_dict_type sys_dict_type_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dict_type
    ADD CONSTRAINT sys_dict_type_code_key UNIQUE (code);


--
-- Name: sys_dict_type sys_dict_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dict_type
    ADD CONSTRAINT sys_dict_type_pkey PRIMARY KEY (id);


--
-- Name: sys_export_log sys_export_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_export_log
    ADD CONSTRAINT sys_export_log_pkey PRIMARY KEY (id);


--
-- Name: sys_external_integration sys_external_integration_integration_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_external_integration
    ADD CONSTRAINT sys_external_integration_integration_code_key UNIQUE (integration_code);


--
-- Name: sys_external_integration sys_external_integration_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_external_integration
    ADD CONSTRAINT sys_external_integration_pkey PRIMARY KEY (id);


--
-- Name: sys_login_log sys_login_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_login_log
    ADD CONSTRAINT sys_login_log_pkey PRIMARY KEY (id);


--
-- Name: sys_menu sys_menu_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_menu
    ADD CONSTRAINT sys_menu_pkey PRIMARY KEY (id);


--
-- Name: sys_notification sys_notification_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_notification
    ADD CONSTRAINT sys_notification_pkey PRIMARY KEY (id);


--
-- Name: sys_operation_log sys_operation_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_operation_log
    ADD CONSTRAINT sys_operation_log_pkey PRIMARY KEY (id);


--
-- Name: sys_permission_change_log sys_permission_change_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_permission_change_log
    ADD CONSTRAINT sys_permission_change_log_pkey PRIMARY KEY (id);


--
-- Name: sys_role_change_log sys_role_change_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_change_log
    ADD CONSTRAINT sys_role_change_log_pkey PRIMARY KEY (id);


--
-- Name: sys_role_menu sys_role_menu_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_menu
    ADD CONSTRAINT sys_role_menu_pkey PRIMARY KEY (id);


--
-- Name: sys_role_menu sys_role_menu_role_id_menu_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_menu
    ADD CONSTRAINT sys_role_menu_role_id_menu_id_key UNIQUE (role_id, menu_id);


--
-- Name: sys_role sys_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_pkey PRIMARY KEY (id);


--
-- Name: sys_role sys_role_role_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_role_code_key UNIQUE (role_code);


--
-- Name: sys_user sys_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_pkey PRIMARY KEY (id);


--
-- Name: sys_user_role sys_user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_pkey PRIMARY KEY (id);


--
-- Name: sys_user_role sys_user_role_user_id_role_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_user_id_role_id_key UNIQUE (user_id, role_id);


--
-- Name: sys_user_session sys_user_session_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_session
    ADD CONSTRAINT sys_user_session_pkey PRIMARY KEY (id);


--
-- Name: sys_user sys_user_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_username_key UNIQUE (username);


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
-- Name: timer_session timer_session_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timer_session
    ADD CONSTRAINT timer_session_pkey PRIMARY KEY (id);


--
-- Name: timesheet timesheet_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet
    ADD CONSTRAINT timesheet_pkey PRIMARY KEY (id);


--
-- Name: timesheet_summary timesheet_summary_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet_summary
    ADD CONSTRAINT timesheet_summary_pkey PRIMARY KEY (id);


--
-- Name: timesheet_summary timesheet_summary_user_id_matter_id_year_month_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet_summary
    ADD CONSTRAINT timesheet_summary_user_id_matter_id_year_month_key UNIQUE (user_id, matter_id, year, month);


--
-- Name: timesheet timesheet_timesheet_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timesheet
    ADD CONSTRAINT timesheet_timesheet_no_key UNIQUE (timesheet_no);


--
-- Name: article_collection uk_article_collection; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.article_collection
    ADD CONSTRAINT uk_article_collection UNIQUE (user_id, article_id);


--
-- Name: attendance uk_attendance_user_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT uk_attendance_user_date UNIQUE (user_id, attendance_date);


--
-- Name: case_study_note uk_case_study_note; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.case_study_note
    ADD CONSTRAINT uk_case_study_note UNIQUE (case_id, user_id);


--
-- Name: sys_dict_item uk_dict_item; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dict_item
    ADD CONSTRAINT uk_dict_item UNIQUE (dict_type_id, value);


--
-- Name: knowledge_collection uk_knowledge_collection; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_collection
    ADD CONSTRAINT uk_knowledge_collection UNIQUE (user_id, target_type, target_id);


--
-- Name: leave_balance uk_leave_balance; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_balance
    ADD CONSTRAINT uk_leave_balance UNIQUE (user_id, leave_type_id, year);


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
-- Name: idx_announcement_publish; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_announcement_publish ON public.sys_announcement USING btree (publish_time);


--
-- Name: idx_announcement_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_announcement_status ON public.sys_announcement USING btree (status);


--
-- Name: idx_archive_archive_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_archive_no ON public.archive USING btree (archive_no);


--
-- Name: idx_archive_borrow_archive_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_archive_id ON public.archive_borrow USING btree (archive_id);


--
-- Name: idx_archive_borrow_borrow_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_borrow_no ON public.archive_borrow USING btree (borrow_no);


--
-- Name: idx_archive_borrow_borrower_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_borrower_id ON public.archive_borrow USING btree (borrower_id);


--
-- Name: idx_archive_borrow_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_date ON public.archive_borrow USING btree (borrow_date, expected_return_date);


--
-- Name: idx_archive_borrow_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_borrow_status ON public.archive_borrow USING btree (status);


--
-- Name: idx_archive_expire_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_expire_date ON public.archive USING btree (retention_expire_date);


--
-- Name: idx_archive_location_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_location_code ON public.archive_location USING btree (location_code);


--
-- Name: idx_archive_location_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_location_id ON public.archive USING btree (location_id);


--
-- Name: idx_archive_location_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_location_status ON public.archive_location USING btree (status);


--
-- Name: idx_archive_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_matter_id ON public.archive USING btree (matter_id);


--
-- Name: idx_archive_operation_log_archive_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_operation_log_archive_id ON public.archive_operation_log USING btree (archive_id);


--
-- Name: idx_archive_operation_log_operator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_operation_log_operator ON public.archive_operation_log USING btree (operator_id);


--
-- Name: idx_archive_operation_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_operation_log_time ON public.archive_operation_log USING btree (operated_at);


--
-- Name: idx_archive_operation_log_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_operation_log_type ON public.archive_operation_log USING btree (operation_type);


--
-- Name: idx_archive_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_status ON public.archive USING btree (status);


--
-- Name: idx_archive_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_type ON public.archive USING btree (archive_type);


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
-- Name: idx_attendance_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_date ON public.attendance USING btree (attendance_date);


--
-- Name: idx_attendance_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_status ON public.attendance USING btree (status);


--
-- Name: idx_attendance_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_user ON public.attendance USING btree (user_id);


--
-- Name: idx_career_level_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_career_level_category ON public.hr_career_level USING btree (category);


--
-- Name: idx_career_level_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_career_level_order ON public.hr_career_level USING btree (level_order);


--
-- Name: idx_career_level_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_career_level_status ON public.hr_career_level USING btree (status);


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
-- Name: idx_client_change_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_change_client ON public.crm_client_change_history USING btree (client_id);


--
-- Name: idx_client_change_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_change_date ON public.crm_client_change_history USING btree (change_date);


--
-- Name: idx_client_change_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_change_type ON public.crm_client_change_history USING btree (change_type);


--
-- Name: idx_commission_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_client ON public.finance_commission USING btree (client_id);


--
-- Name: idx_commission_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_contract ON public.finance_commission USING btree (contract_id);


--
-- Name: idx_commission_detail_commission; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_detail_commission ON public.finance_commission_detail USING btree (commission_id);


--
-- Name: idx_commission_detail_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_detail_user ON public.finance_commission_detail USING btree (user_id);


--
-- Name: idx_commission_fee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_fee ON public.finance_commission USING btree (fee_id);


--
-- Name: idx_commission_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_matter ON public.finance_commission USING btree (matter_id);


--
-- Name: idx_commission_originator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_originator ON public.finance_commission USING btree (originator_id);


--
-- Name: idx_commission_payment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_payment ON public.finance_commission USING btree (payment_id);


--
-- Name: idx_commission_rule; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_rule ON public.finance_commission USING btree (rule_id);


--
-- Name: idx_commission_rule_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_rule_code ON public.finance_commission_rule USING btree (rule_code);


--
-- Name: idx_commission_rule_default; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_rule_default ON public.finance_commission_rule USING btree (is_default);


--
-- Name: idx_commission_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_commission_status ON public.finance_commission USING btree (status);


--
-- Name: idx_contract_participant_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_participant_contract ON public.contract_participant USING btree (contract_id);


--
-- Name: idx_contract_participant_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_participant_role ON public.contract_participant USING btree (role);


--
-- Name: idx_contract_participant_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_participant_user ON public.contract_participant USING btree (user_id);


--
-- Name: idx_contract_template_contract_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_template_contract_type ON public.contract_template USING btree (contract_type);


--
-- Name: idx_contract_template_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_template_status ON public.contract_template USING btree (status);


--
-- Name: idx_crm_client_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_category ON public.crm_client USING btree (category);


--
-- Name: idx_crm_client_client_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_client_no ON public.crm_client USING btree (client_no);


--
-- Name: idx_crm_client_contact_record_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_contact_record_client_id ON public.crm_client_contact_record USING btree (client_id);


--
-- Name: idx_crm_client_contact_record_contact_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_contact_record_contact_date ON public.crm_client_contact_record USING btree (contact_date);


--
-- Name: idx_crm_client_contact_record_contact_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_contact_record_contact_id ON public.crm_client_contact_record USING btree (contact_id);


--
-- Name: idx_crm_client_contact_record_next_follow_up_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_contact_record_next_follow_up_date ON public.crm_client_contact_record USING btree (next_follow_up_date);


--
-- Name: idx_crm_client_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_name ON public.crm_client USING btree (name);


--
-- Name: idx_crm_client_related_company_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_related_company_client_id ON public.crm_client_related_company USING btree (client_id);


--
-- Name: idx_crm_client_related_company_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_related_company_name ON public.crm_client_related_company USING btree (related_company_name);


--
-- Name: idx_crm_client_related_company_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_related_company_type ON public.crm_client_related_company USING btree (related_company_type);


--
-- Name: idx_crm_client_responsible_lawyer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_responsible_lawyer ON public.crm_client USING btree (responsible_lawyer_id);


--
-- Name: idx_crm_client_shareholder_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_shareholder_client_id ON public.crm_client_shareholder USING btree (client_id);


--
-- Name: idx_crm_client_shareholder_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_shareholder_name ON public.crm_client_shareholder USING btree (shareholder_name);


--
-- Name: idx_crm_client_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_status ON public.crm_client USING btree (status);


--
-- Name: idx_crm_client_tag_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_tag_deleted ON public.crm_client_tag USING btree (deleted);


--
-- Name: idx_crm_client_tag_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_tag_name ON public.crm_client_tag USING btree (tag_name);


--
-- Name: idx_crm_client_tag_relation_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_tag_relation_client_id ON public.crm_client_tag_relation USING btree (client_id);


--
-- Name: idx_crm_client_tag_relation_tag_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_tag_relation_tag_id ON public.crm_client_tag_relation USING btree (tag_id);


--
-- Name: idx_crm_client_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_client_type ON public.crm_client USING btree (client_type);


--
-- Name: idx_crm_conflict_check_applicant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_conflict_check_applicant_id ON public.crm_conflict_check USING btree (applicant_id);


--
-- Name: idx_crm_conflict_check_check_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_conflict_check_check_no ON public.crm_conflict_check USING btree (check_no);


--
-- Name: idx_crm_conflict_check_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_conflict_check_client_id ON public.crm_conflict_check USING btree (client_id);


--
-- Name: idx_crm_conflict_check_item_check_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_conflict_check_item_check_id ON public.crm_conflict_check_item USING btree (check_id);


--
-- Name: idx_crm_conflict_check_item_has_conflict; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_conflict_check_item_has_conflict ON public.crm_conflict_check_item USING btree (has_conflict);


--
-- Name: idx_crm_conflict_check_item_party_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_conflict_check_item_party_name ON public.crm_conflict_check_item USING btree (party_name);


--
-- Name: idx_crm_conflict_check_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_conflict_check_matter_id ON public.crm_conflict_check USING btree (matter_id);


--
-- Name: idx_crm_conflict_check_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_conflict_check_status ON public.crm_conflict_check USING btree (status);


--
-- Name: idx_crm_lead_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_lead_created_at ON public.crm_lead USING btree (created_at);


--
-- Name: idx_crm_lead_follow_up_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_lead_follow_up_created_at ON public.crm_lead_follow_up USING btree (created_at);


--
-- Name: idx_crm_lead_follow_up_lead_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_lead_follow_up_lead_id ON public.crm_lead_follow_up USING btree (lead_id);


--
-- Name: idx_crm_lead_originator_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_lead_originator_id ON public.crm_lead USING btree (originator_id);


--
-- Name: idx_crm_lead_responsible_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_lead_responsible_user_id ON public.crm_lead USING btree (responsible_user_id);


--
-- Name: idx_crm_lead_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_lead_status ON public.crm_lead USING btree (status);


--
-- Name: idx_cross_exam_evidence; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cross_exam_evidence ON public.evidence_cross_exam USING btree (evidence_id);


--
-- Name: idx_dict_item_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dict_item_type ON public.sys_dict_item USING btree (dict_type_id);


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
-- Name: idx_evaluation_employee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evaluation_employee ON public.hr_performance_evaluation USING btree (employee_id);


--
-- Name: idx_evaluation_evaluator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evaluation_evaluator ON public.hr_performance_evaluation USING btree (evaluator_id);


--
-- Name: idx_evaluation_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evaluation_task ON public.hr_performance_evaluation USING btree (task_id);


--
-- Name: idx_evidence_document; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_document ON public.evidence USING btree (document_id);


--
-- Name: idx_evidence_list_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_list_matter ON public.evidence_list USING btree (matter_id);


--
-- Name: idx_evidence_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_matter ON public.evidence USING btree (matter_id);


--
-- Name: idx_evidence_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_status ON public.evidence USING btree (cross_exam_status);


--
-- Name: idx_evidence_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evidence_type ON public.evidence USING btree (evidence_type);


--
-- Name: idx_export_log_exported_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_export_log_exported_at ON public.sys_export_log USING btree (exported_at);


--
-- Name: idx_export_log_exported_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_export_log_exported_by ON public.sys_export_log USING btree (exported_by);


--
-- Name: idx_export_log_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_export_log_type ON public.sys_export_log USING btree (export_type);


--
-- Name: idx_external_integration_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_external_integration_enabled ON public.sys_external_integration USING btree (enabled);


--
-- Name: idx_external_integration_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_external_integration_type ON public.sys_external_integration USING btree (integration_type);


--
-- Name: idx_finance_contract_amendment_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_amendment_contract ON public.finance_contract_amendment USING btree (contract_id);


--
-- Name: idx_finance_contract_amendment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_amendment_status ON public.finance_contract_amendment USING btree (status);


--
-- Name: idx_finance_contract_amendment_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_amendment_type ON public.finance_contract_amendment USING btree (amendment_type);


--
-- Name: idx_finance_contract_case_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_case_type ON public.finance_contract USING btree (case_type);


--
-- Name: idx_finance_contract_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_client_id ON public.finance_contract USING btree (client_id);


--
-- Name: idx_finance_contract_conflict_check_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_conflict_check_status ON public.finance_contract USING btree (conflict_check_status);


--
-- Name: idx_finance_contract_contract_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_contract_no ON public.finance_contract USING btree (contract_no);


--
-- Name: idx_finance_contract_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_matter_id ON public.finance_contract USING btree (matter_id);


--
-- Name: idx_finance_contract_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_status ON public.finance_contract USING btree (status);


--
-- Name: idx_finance_contract_template_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_template_id ON public.finance_contract USING btree (template_id);


--
-- Name: idx_finance_contract_trial_stage; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_contract_trial_stage ON public.finance_contract USING btree (trial_stage);


--
-- Name: idx_finance_cost_allocation_expense_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_cost_allocation_expense_id ON public.finance_cost_allocation USING btree (expense_id);


--
-- Name: idx_finance_cost_allocation_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_cost_allocation_matter_id ON public.finance_cost_allocation USING btree (matter_id);


--
-- Name: idx_finance_cost_split_expense_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_cost_split_expense_id ON public.finance_cost_split USING btree (expense_id);


--
-- Name: idx_finance_cost_split_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_cost_split_matter_id ON public.finance_cost_split USING btree (matter_id);


--
-- Name: idx_finance_expense_applicant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_applicant_id ON public.finance_expense USING btree (applicant_id);


--
-- Name: idx_finance_expense_expense_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_expense_date ON public.finance_expense USING btree (expense_date);


--
-- Name: idx_finance_expense_expense_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_expense_no ON public.finance_expense USING btree (expense_no);


--
-- Name: idx_finance_expense_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_matter_id ON public.finance_expense USING btree (matter_id);


--
-- Name: idx_finance_expense_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_expense_status ON public.finance_expense USING btree (status);


--
-- Name: idx_finance_fee_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_client_id ON public.finance_fee USING btree (client_id);


--
-- Name: idx_finance_fee_contract_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_contract_id ON public.finance_fee USING btree (contract_id);


--
-- Name: idx_finance_fee_fee_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_fee_no ON public.finance_fee USING btree (fee_no);


--
-- Name: idx_finance_fee_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_matter_id ON public.finance_fee USING btree (matter_id);


--
-- Name: idx_finance_fee_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_fee_status ON public.finance_fee USING btree (status);


--
-- Name: idx_finance_invoice_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_client_id ON public.finance_invoice USING btree (client_id);


--
-- Name: idx_finance_invoice_contract_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_contract_id ON public.finance_invoice USING btree (contract_id);


--
-- Name: idx_finance_invoice_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_date ON public.finance_invoice USING btree (invoice_date);


--
-- Name: idx_finance_invoice_fee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_fee_id ON public.finance_invoice USING btree (fee_id);


--
-- Name: idx_finance_invoice_invoice_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_invoice_no ON public.finance_invoice USING btree (invoice_no);


--
-- Name: idx_finance_invoice_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_invoice_status ON public.finance_invoice USING btree (status);


--
-- Name: idx_finance_payment_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_client_id ON public.finance_payment USING btree (client_id);


--
-- Name: idx_finance_payment_contract_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_contract_id ON public.finance_payment USING btree (contract_id);


--
-- Name: idx_finance_payment_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_date ON public.finance_payment USING btree (payment_date);


--
-- Name: idx_finance_payment_fee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_fee_id ON public.finance_payment USING btree (fee_id);


--
-- Name: idx_finance_payment_locked; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_locked ON public.finance_payment USING btree (locked);


--
-- Name: idx_finance_payment_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_matter_id ON public.finance_payment USING btree (matter_id);


--
-- Name: idx_finance_payment_payment_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_payment_no ON public.finance_payment USING btree (payment_no);


--
-- Name: idx_finance_payment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_finance_payment_status ON public.finance_payment USING btree (status);


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
-- Name: idx_handover_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_created_at ON public.sys_data_handover USING btree (created_at);


--
-- Name: idx_handover_detail_data; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_detail_data ON public.sys_data_handover_detail USING btree (data_type, data_id);


--
-- Name: idx_handover_detail_handover; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_detail_handover ON public.sys_data_handover_detail USING btree (handover_id);


--
-- Name: idx_handover_detail_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_detail_status ON public.sys_data_handover_detail USING btree (status);


--
-- Name: idx_handover_detail_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_detail_type ON public.sys_data_handover_detail USING btree (data_type);


--
-- Name: idx_handover_from_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_from_user ON public.sys_data_handover USING btree (from_user_id);


--
-- Name: idx_handover_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_status ON public.sys_data_handover USING btree (status);


--
-- Name: idx_handover_to_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_to_user ON public.sys_data_handover USING btree (to_user_id);


--
-- Name: idx_handover_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_type ON public.sys_data_handover USING btree (handover_type);


--
-- Name: idx_hourly_rate_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hourly_rate_user ON public.hourly_rate USING btree (user_id);


--
-- Name: idx_hr_employee_employee_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_employee_employee_no ON public.hr_employee USING btree (employee_no);


--
-- Name: idx_hr_employee_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_employee_user_id ON public.hr_employee USING btree (user_id);


--
-- Name: idx_hr_employee_work_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_employee_work_status ON public.hr_employee USING btree (work_status);


--
-- Name: idx_hr_regularization_application_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_regularization_application_date ON public.hr_regularization USING btree (application_date);


--
-- Name: idx_hr_regularization_application_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_regularization_application_no ON public.hr_regularization USING btree (application_no);


--
-- Name: idx_hr_regularization_employee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_regularization_employee_id ON public.hr_regularization USING btree (employee_id);


--
-- Name: idx_hr_regularization_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_regularization_status ON public.hr_regularization USING btree (status);


--
-- Name: idx_hr_resignation_application_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_resignation_application_no ON public.hr_resignation USING btree (application_no);


--
-- Name: idx_hr_resignation_employee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_resignation_employee_id ON public.hr_resignation USING btree (employee_id);


--
-- Name: idx_hr_resignation_handover_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_resignation_handover_status ON public.hr_resignation USING btree (handover_status);


--
-- Name: idx_hr_resignation_resignation_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_resignation_resignation_date ON public.hr_resignation USING btree (resignation_date);


--
-- Name: idx_hr_resignation_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_resignation_status ON public.hr_resignation USING btree (status);


--
-- Name: idx_indicator_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_indicator_category ON public.hr_performance_indicator USING btree (category);


--
-- Name: idx_indicator_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_indicator_status ON public.hr_performance_indicator USING btree (status);


--
-- Name: idx_inventory_detail_asset; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_detail_asset ON public.asset_inventory_detail USING btree (asset_id);


--
-- Name: idx_inventory_detail_inventory; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_detail_inventory ON public.asset_inventory_detail USING btree (inventory_id);


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


--
-- Name: idx_leave_app_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_leave_app_status ON public.leave_application USING btree (status);


--
-- Name: idx_leave_app_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_leave_app_time ON public.leave_application USING btree (start_time, end_time);


--
-- Name: idx_leave_app_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_leave_app_type ON public.leave_application USING btree (leave_type_id);


--
-- Name: idx_leave_app_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_leave_app_user ON public.leave_application USING btree (user_id);


--
-- Name: idx_leave_balance_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_leave_balance_user ON public.leave_balance USING btree (user_id);


--
-- Name: idx_leave_balance_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_leave_balance_year ON public.leave_balance USING btree (year);


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
-- Name: idx_matter_case_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_case_type ON public.matter USING btree (case_type);


--
-- Name: idx_matter_client_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_client_client_id ON public.matter_client USING btree (client_id);


--
-- Name: idx_matter_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_client_id ON public.matter USING btree (client_id);


--
-- Name: idx_matter_client_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_client_matter_id ON public.matter_client USING btree (matter_id);


--
-- Name: idx_matter_client_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_matter_client_unique ON public.matter_client USING btree (matter_id, client_id) WHERE (deleted = false);


--
-- Name: idx_matter_deadline_deadline_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_deadline_deadline_date ON public.matter_deadline USING btree (deadline_date);


--
-- Name: idx_matter_deadline_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_deadline_matter_id ON public.matter_deadline USING btree (matter_id);


--
-- Name: idx_matter_deadline_reminder_sent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_deadline_reminder_sent ON public.matter_deadline USING btree (reminder_sent);


--
-- Name: idx_matter_deadline_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_deadline_status ON public.matter_deadline USING btree (status);


--
-- Name: idx_matter_dossier_item_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_dossier_item_matter ON public.matter_dossier_item USING btree (matter_id);


--
-- Name: idx_matter_lead_lawyer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_lead_lawyer ON public.matter USING btree (lead_lawyer_id);


--
-- Name: idx_matter_matter_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_matter_no ON public.matter USING btree (matter_no);


--
-- Name: idx_matter_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_name ON public.matter USING btree (name);


--
-- Name: idx_matter_opposing_lawyer_firm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_opposing_lawyer_firm ON public.matter USING btree (opposing_lawyer_firm);


--
-- Name: idx_matter_opposing_lawyer_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_opposing_lawyer_name ON public.matter USING btree (opposing_lawyer_name);


--
-- Name: idx_matter_participant_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_participant_matter_id ON public.matter_participant USING btree (matter_id);


--
-- Name: idx_matter_participant_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_participant_status ON public.matter_participant USING btree (status);


--
-- Name: idx_matter_participant_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_participant_user_id ON public.matter_participant USING btree (user_id);


--
-- Name: idx_matter_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_status ON public.matter USING btree (status);


--
-- Name: idx_matter_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_type ON public.matter USING btree (matter_type);


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
-- Name: idx_menu_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_menu_parent ON public.sys_menu USING btree (parent_id);


--
-- Name: idx_milestone_plan; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_milestone_plan ON public.hr_development_milestone USING btree (plan_id);


--
-- Name: idx_notification_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_created ON public.sys_notification USING btree (created_at);


--
-- Name: idx_notification_read; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_read ON public.sys_notification USING btree (is_read);


--
-- Name: idx_notification_receiver; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_receiver ON public.sys_notification USING btree (receiver_id);


--
-- Name: idx_notification_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_type ON public.sys_notification USING btree (type);


--
-- Name: idx_operation_log_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_operation_log_created ON public.sys_operation_log USING btree (created_at);


--
-- Name: idx_operation_log_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_operation_log_module ON public.sys_operation_log USING btree (module);


--
-- Name: idx_operation_log_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_operation_log_user ON public.sys_operation_log USING btree (user_id);


--
-- Name: idx_overtime_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_overtime_date ON public.overtime_application USING btree (overtime_date);


--
-- Name: idx_overtime_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_overtime_status ON public.overtime_application USING btree (status);


--
-- Name: idx_overtime_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_overtime_user ON public.overtime_application USING btree (user_id);


--
-- Name: idx_payment_amendment_payment_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_amendment_payment_id ON public.fin_payment_amendment USING btree (payment_id);


--
-- Name: idx_payment_amendment_requested_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_amendment_requested_at ON public.fin_payment_amendment USING btree (requested_at);


--
-- Name: idx_payment_amendment_requested_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_amendment_requested_by ON public.fin_payment_amendment USING btree (requested_by);


--
-- Name: idx_payment_amendment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_amendment_status ON public.fin_payment_amendment USING btree (status);


--
-- Name: idx_payment_schedule_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_schedule_contract ON public.contract_payment_schedule USING btree (contract_id);


--
-- Name: idx_payment_schedule_planned_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_schedule_planned_date ON public.contract_payment_schedule USING btree (planned_date);


--
-- Name: idx_payment_schedule_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_schedule_status ON public.contract_payment_schedule USING btree (status);


--
-- Name: idx_payroll_deduction_item_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_deduction_item_id ON public.hr_payroll_deduction USING btree (payroll_item_id);


--
-- Name: idx_payroll_income_item_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_income_item_id ON public.hr_payroll_income USING btree (payroll_item_id);


--
-- Name: idx_payroll_item_employee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_item_employee_id ON public.hr_payroll_item USING btree (employee_id);


--
-- Name: idx_payroll_item_sheet_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_item_sheet_id ON public.hr_payroll_item USING btree (payroll_sheet_id);


--
-- Name: idx_payroll_item_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_item_user_id ON public.hr_payroll_item USING btree (user_id);


--
-- Name: idx_payroll_sheet_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_sheet_status ON public.hr_payroll_sheet USING btree (status);


--
-- Name: idx_payroll_sheet_year_month; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_sheet_year_month ON public.hr_payroll_sheet USING btree (payroll_year, payroll_month);


--
-- Name: idx_permission_log_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permission_log_code ON public.sys_permission_change_log USING btree (permission_code);


--
-- Name: idx_permission_log_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permission_log_role ON public.sys_permission_change_log USING btree (role_id);


--
-- Name: idx_permission_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permission_log_time ON public.sys_permission_change_log USING btree (changed_at);


--
-- Name: idx_permission_log_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permission_log_type ON public.sys_permission_change_log USING btree (change_type);


--
-- Name: idx_plan_employee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_plan_employee ON public.hr_development_plan USING btree (employee_id);


--
-- Name: idx_plan_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_plan_status ON public.hr_development_plan USING btree (status);


--
-- Name: idx_plan_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_plan_year ON public.hr_development_plan USING btree (plan_year);


--
-- Name: idx_promotion_apply_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_apply_date ON public.hr_promotion_application USING btree (apply_date);


--
-- Name: idx_promotion_employee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_employee ON public.hr_promotion_application USING btree (employee_id);


--
-- Name: idx_promotion_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_status ON public.hr_promotion_application USING btree (status);


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
-- Name: idx_review_application; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_review_application ON public.hr_promotion_review USING btree (application_id);


--
-- Name: idx_review_reviewer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_review_reviewer ON public.hr_promotion_review USING btree (reviewer_id);


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
-- Name: idx_role_change_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_change_time ON public.sys_role_change_log USING btree (changed_at);


--
-- Name: idx_role_change_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_change_type ON public.sys_role_change_log USING btree (change_type);


--
-- Name: idx_role_change_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_change_user ON public.sys_role_change_log USING btree (user_id);


--
-- Name: idx_role_menu_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_menu_role ON public.sys_role_menu USING btree (role_id);


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
-- Name: idx_score_evaluation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_score_evaluation ON public.hr_performance_score USING btree (evaluation_id);


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
-- Name: idx_summary_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_summary_period ON public.timesheet_summary USING btree (year, month);


--
-- Name: idx_summary_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_summary_user ON public.timesheet_summary USING btree (user_id);


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
-- Name: idx_sync_log_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_log_source ON public.sys_data_sync_log USING btree (source_table, source_id);


--
-- Name: idx_sync_log_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_log_status ON public.sys_data_sync_log USING btree (sync_status);


--
-- Name: idx_sync_log_synced_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_log_synced_at ON public.sys_data_sync_log USING btree (synced_at);


--
-- Name: idx_sync_log_target_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_log_target_module ON public.sys_data_sync_log USING btree (target_module);


--
-- Name: idx_sys_backup_backup_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_backup_backup_time ON public.sys_backup USING btree (backup_time);


--
-- Name: idx_sys_backup_backup_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_backup_backup_type ON public.sys_backup USING btree (backup_type);


--
-- Name: idx_sys_backup_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_backup_status ON public.sys_backup USING btree (status);


--
-- Name: idx_sys_login_log_login_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_login_log_login_time ON public.sys_login_log USING btree (login_time);


--
-- Name: idx_sys_login_log_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_login_log_status ON public.sys_login_log USING btree (status);


--
-- Name: idx_sys_login_log_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_login_log_user_id ON public.sys_login_log USING btree (user_id);


--
-- Name: idx_sys_login_log_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_login_log_username ON public.sys_login_log USING btree (username);


--
-- Name: idx_sys_user_session_expire_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_user_session_expire_time ON public.sys_user_session USING btree (expire_time);


--
-- Name: idx_sys_user_session_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_user_session_status ON public.sys_user_session USING btree (status);


--
-- Name: idx_sys_user_session_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_user_session_token ON public.sys_user_session USING btree (token);


--
-- Name: idx_sys_user_session_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_user_session_user_id ON public.sys_user_session USING btree (user_id);


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
-- Name: idx_task_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_task_year ON public.hr_performance_task USING btree (year);


--
-- Name: idx_template_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_template_category ON public.doc_template USING btree (category_id);


--
-- Name: idx_timer_session_matter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timer_session_matter_id ON public.timer_session USING btree (matter_id);


--
-- Name: idx_timer_session_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timer_session_status ON public.timer_session USING btree (status);


--
-- Name: idx_timer_session_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timer_session_user_id ON public.timer_session USING btree (user_id);


--
-- Name: idx_timesheet_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_date ON public.timesheet USING btree (work_date);


--
-- Name: idx_timesheet_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_matter ON public.timesheet USING btree (matter_id);


--
-- Name: idx_timesheet_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_status ON public.timesheet USING btree (status);


--
-- Name: idx_timesheet_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_user ON public.timesheet USING btree (user_id);


--
-- Name: idx_training_record_employee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_training_record_employee ON public.hr_training_record USING btree (employee_id);


--
-- Name: idx_training_record_training; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_training_record_training ON public.hr_training_record USING btree (training_id);


--
-- Name: idx_training_start_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_training_start_time ON public.hr_training USING btree (start_time);


--
-- Name: idx_training_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_training_status ON public.hr_training USING btree (status);


--
-- Name: idx_user_role_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_role_user ON public.sys_user_role USING btree (user_id);


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
-- Name: fin_payment_amendment fk_amendment_approver; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment
    ADD CONSTRAINT fk_amendment_approver FOREIGN KEY (approved_by) REFERENCES public.sys_user(id);


--
-- Name: fin_payment_amendment fk_amendment_payment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment
    ADD CONSTRAINT fk_amendment_payment FOREIGN KEY (payment_id) REFERENCES public.finance_payment(id);


--
-- Name: fin_payment_amendment fk_amendment_requester; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_amendment
    ADD CONSTRAINT fk_amendment_requester FOREIGN KEY (requested_by) REFERENCES public.sys_user(id);


--
-- Name: admin_asset_record fk_asset_record_asset; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_asset_record
    ADD CONSTRAINT fk_asset_record_asset FOREIGN KEY (asset_id) REFERENCES public.admin_asset(id);


--
-- Name: finance_commission_detail fk_commission_detail_commission; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_detail
    ADD CONSTRAINT fk_commission_detail_commission FOREIGN KEY (commission_id) REFERENCES public.finance_commission(id);


--
-- Name: finance_commission_detail fk_commission_detail_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_commission_detail
    ADD CONSTRAINT fk_commission_detail_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- Name: dossier_template_item fk_dossier_item_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossier_template_item
    ADD CONSTRAINT fk_dossier_item_template FOREIGN KEY (template_id) REFERENCES public.dossier_template(id);


--
-- Name: hr_performance_evaluation fk_evaluation_task; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_evaluation
    ADD CONSTRAINT fk_evaluation_task FOREIGN KEY (task_id) REFERENCES public.hr_performance_task(id);


--
-- Name: sys_export_log fk_export_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_export_log
    ADD CONSTRAINT fk_export_user FOREIGN KEY (exported_by) REFERENCES public.sys_user(id);


--
-- Name: matter_client fk_matter_client_client; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_client
    ADD CONSTRAINT fk_matter_client_client FOREIGN KEY (client_id) REFERENCES public.crm_client(id);


--
-- Name: matter_client fk_matter_client_matter; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_client
    ADD CONSTRAINT fk_matter_client_matter FOREIGN KEY (matter_id) REFERENCES public.matter(id);


--
-- Name: matter_dossier_item fk_matter_dossier_matter; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_dossier_item
    ADD CONSTRAINT fk_matter_dossier_matter FOREIGN KEY (matter_id) REFERENCES public.matter(id);


--
-- Name: hr_development_milestone fk_milestone_plan; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_development_milestone
    ADD CONSTRAINT fk_milestone_plan FOREIGN KEY (plan_id) REFERENCES public.hr_development_plan(id);


--
-- Name: contract_participant fk_participant_contract; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant
    ADD CONSTRAINT fk_participant_contract FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id);


--
-- Name: contract_participant fk_participant_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_participant
    ADD CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- Name: finance_payment fk_payment_locked_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.finance_payment
    ADD CONSTRAINT fk_payment_locked_by FOREIGN KEY (locked_by) REFERENCES public.sys_user(id);


--
-- Name: contract_payment_schedule fk_payment_schedule_contract; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_payment_schedule
    ADD CONSTRAINT fk_payment_schedule_contract FOREIGN KEY (contract_id) REFERENCES public.finance_contract(id);


--
-- Name: hr_development_plan fk_plan_current_level; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_development_plan
    ADD CONSTRAINT fk_plan_current_level FOREIGN KEY (current_level_id) REFERENCES public.hr_career_level(id);


--
-- Name: hr_development_plan fk_plan_target_level; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_development_plan
    ADD CONSTRAINT fk_plan_target_level FOREIGN KEY (target_level_id) REFERENCES public.hr_career_level(id);


--
-- Name: hr_promotion_application fk_promotion_current_level; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_promotion_application
    ADD CONSTRAINT fk_promotion_current_level FOREIGN KEY (current_level_id) REFERENCES public.hr_career_level(id);


--
-- Name: hr_promotion_application fk_promotion_target_level; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_promotion_application
    ADD CONSTRAINT fk_promotion_target_level FOREIGN KEY (target_level_id) REFERENCES public.hr_career_level(id);


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
-- Name: hr_promotion_review fk_review_application; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_promotion_review
    ADD CONSTRAINT fk_review_application FOREIGN KEY (application_id) REFERENCES public.hr_promotion_application(id);


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
-- Name: hr_performance_score fk_score_evaluation; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_score
    ADD CONSTRAINT fk_score_evaluation FOREIGN KEY (evaluation_id) REFERENCES public.hr_performance_evaluation(id);


--
-- Name: hr_performance_score fk_score_indicator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_score
    ADD CONSTRAINT fk_score_indicator FOREIGN KEY (indicator_id) REFERENCES public.hr_performance_indicator(id);


--
-- Name: hr_training_record fk_training_record_employee; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_training_record
    ADD CONSTRAINT fk_training_record_employee FOREIGN KEY (employee_id) REFERENCES public.sys_user(id);


--
-- Name: hr_training_record fk_training_record_training; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_training_record
    ADD CONSTRAINT fk_training_record_training FOREIGN KEY (training_id) REFERENCES public.hr_training(id);


--
-- Name: hr_payroll_deduction hr_payroll_deduction_payroll_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_deduction
    ADD CONSTRAINT hr_payroll_deduction_payroll_item_id_fkey FOREIGN KEY (payroll_item_id) REFERENCES public.hr_payroll_item(id) ON DELETE CASCADE;


--
-- Name: hr_payroll_income hr_payroll_income_payroll_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_income
    ADD CONSTRAINT hr_payroll_income_payroll_item_id_fkey FOREIGN KEY (payroll_item_id) REFERENCES public.hr_payroll_item(id) ON DELETE CASCADE;


--
-- Name: hr_payroll_item hr_payroll_item_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_item
    ADD CONSTRAINT hr_payroll_item_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.hr_employee(id);


--
-- Name: hr_payroll_item hr_payroll_item_payroll_sheet_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_payroll_item
    ADD CONSTRAINT hr_payroll_item_payroll_sheet_id_fkey FOREIGN KEY (payroll_sheet_id) REFERENCES public.hr_payroll_sheet(id);


--
-- Name: sys_data_handover sys_data_handover_confirmed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover
    ADD CONSTRAINT sys_data_handover_confirmed_by_fkey FOREIGN KEY (confirmed_by) REFERENCES public.sys_user(id);


--
-- Name: sys_data_handover_detail sys_data_handover_detail_handover_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover_detail
    ADD CONSTRAINT sys_data_handover_detail_handover_id_fkey FOREIGN KEY (handover_id) REFERENCES public.sys_data_handover(id);


--
-- Name: sys_data_handover sys_data_handover_from_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover
    ADD CONSTRAINT sys_data_handover_from_user_id_fkey FOREIGN KEY (from_user_id) REFERENCES public.sys_user(id);


--
-- Name: sys_data_handover sys_data_handover_submitted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover
    ADD CONSTRAINT sys_data_handover_submitted_by_fkey FOREIGN KEY (submitted_by) REFERENCES public.sys_user(id);


--
-- Name: sys_data_handover sys_data_handover_to_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_data_handover
    ADD CONSTRAINT sys_data_handover_to_user_id_fkey FOREIGN KEY (to_user_id) REFERENCES public.sys_user(id);


--
-- Name: sys_permission_change_log sys_permission_change_log_changed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_permission_change_log
    ADD CONSTRAINT sys_permission_change_log_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES public.sys_user(id);


--
-- Name: sys_permission_change_log sys_permission_change_log_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_permission_change_log
    ADD CONSTRAINT sys_permission_change_log_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.sys_role(id);


--
-- Name: sys_role_change_log sys_role_change_log_changed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_change_log
    ADD CONSTRAINT sys_role_change_log_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES public.sys_user(id);


--
-- Name: sys_role_change_log sys_role_change_log_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_change_log
    ADD CONSTRAINT sys_role_change_log_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- PostgreSQL database dump complete
--

\unrestrict cKSUodcA4otatYEdk1WWcqVpE1tIfjuPJvBkXEqWXeQufvov8NlYuwYzJYy1YOc

