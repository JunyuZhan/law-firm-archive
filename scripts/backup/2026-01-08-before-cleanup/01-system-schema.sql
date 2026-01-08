--
-- PostgreSQL database dump
--

\restrict ZyuYf2gBrszZHvoxjNaQV6TEakWx1quBANX7wtv60eVMf1cgXi5OlGid6UzzP4r

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
-- Name: sys_dict_item uk_dict_item; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_dict_item
    ADD CONSTRAINT uk_dict_item UNIQUE (dict_type_id, value);


--
-- Name: idx_announcement_publish; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_announcement_publish ON public.sys_announcement USING btree (publish_time);


--
-- Name: idx_announcement_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_announcement_status ON public.sys_announcement USING btree (status);


--
-- Name: idx_dict_item_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dict_item_type ON public.sys_dict_item USING btree (dict_type_id);


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
-- Name: idx_menu_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_menu_parent ON public.sys_menu USING btree (parent_id);


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
-- Name: idx_role_menu_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_menu_role ON public.sys_role_menu USING btree (role_id);


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
-- Name: idx_user_role_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_role_user ON public.sys_user_role USING btree (user_id);


--
-- Name: sys_export_log fk_export_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_export_log
    ADD CONSTRAINT fk_export_user FOREIGN KEY (exported_by) REFERENCES public.sys_user(id);


--
-- PostgreSQL database dump complete
--

\unrestrict ZyuYf2gBrszZHvoxjNaQV6TEakWx1quBANX7wtv60eVMf1cgXi5OlGid6UzzP4r

