--
-- PostgreSQL database dump
--

\restrict 3pNKbI9XtWPtABqC7qaoW4K7WbH0nCa6SUpWKiio7ajj5Olj048pyvFg80nSEZk

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
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


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
    deleted boolean DEFAULT false
);


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
    deleted boolean DEFAULT false
);


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
    thumbnail_url character varying(1000)
);


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
    distribution_ratio numeric(5,4),
    commission_rate numeric(5,4),
    commission_amount numeric(15,2),
    compensation_type character varying(20)
);


--
-- Name: finance_commission_detail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.finance_commission_detail (
    id bigint NOT NULL,
    commission_id bigint NOT NULL,
    user_id bigint NOT NULL,
    user_name character varying(100),
    role_in_matter character varying(50),
    allocation_rate numeric(5,4) NOT NULL,
    commission_amount numeric(15,2) NOT NULL,
    tax_amount numeric(15,2),
    net_amount numeric(15,2) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);


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
    firm_retention_rate numeric(5,4) NOT NULL,
    originator_rate numeric(5,4),
    tax_rate numeric(5,4),
    management_fee_rate numeric(5,4),
    rate_tiers jsonb,
    effective_date date,
    expiry_date date,
    is_default boolean DEFAULT false,
    active boolean DEFAULT true,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    deleted boolean DEFAULT false
);


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
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false
);


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
    deleted boolean DEFAULT false
);


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
    deleted boolean DEFAULT false
);


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
    deleted boolean DEFAULT false
);


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
-- Name: sys_data_sync_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_data_sync_log (
    id bigint NOT NULL,
    source_table character varying(100) NOT NULL,
    source_id bigint NOT NULL,
    target_module character varying(50) NOT NULL,
    operation_type character varying(50) NOT NULL,
    sync_data jsonb,
    sync_status character varying(20) NOT NULL,
    error_message text,
    retry_count integer DEFAULT 0,
    synced_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


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
    deleted boolean DEFAULT false
);


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
-- Name: matter_deadline id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_deadline ALTER COLUMN id SET DEFAULT nextval('public.matter_deadline_id_seq'::regclass);


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
-- Name: sys_role id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role ALTER COLUMN id SET DEFAULT nextval('public.sys_role_id_seq'::regclass);


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
-- Name: matter_deadline matter_deadline_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matter_deadline
    ADD CONSTRAINT matter_deadline_pkey PRIMARY KEY (id);


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
-- Name: idx_document_matter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_matter ON public.doc_document USING btree (matter_id);


--
-- Name: idx_document_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_status ON public.doc_document USING btree (status);


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
-- Name: idx_matter_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matter_client_id ON public.matter USING btree (client_id);


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
-- PostgreSQL database dump complete
--

\unrestrict 3pNKbI9XtWPtABqC7qaoW4K7WbH0nCa6SUpWKiio7ajj5Olj048pyvFg80nSEZk

