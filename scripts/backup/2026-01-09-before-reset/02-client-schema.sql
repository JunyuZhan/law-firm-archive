-- =====================================================
-- 律师事务所管理系统 - 客户管理模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 客户信息、线索、跟进记录、利冲检查等CRM相关表
-- =====================================================

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
