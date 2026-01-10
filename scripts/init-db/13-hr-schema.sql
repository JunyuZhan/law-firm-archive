-- =====================================================
-- 律师事务所管理系统 - 人力资源模块
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 员工、考勤、培训、薪酬、绩效、晋升等HR相关表
-- =====================================================

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
-- Name: attendance id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance ALTER COLUMN id SET DEFAULT nextval('public.attendance_id_seq'::regclass);
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
-- Name: overtime_application id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overtime_application ALTER COLUMN id SET DEFAULT nextval('public.overtime_application_id_seq'::regclass);
--
-- Name: attendance attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_pkey PRIMARY KEY (id);
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
-- Name: attendance uk_attendance_user_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT uk_attendance_user_date UNIQUE (user_id, attendance_date);
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
-- Name: idx_milestone_plan; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_milestone_plan ON public.hr_development_milestone USING btree (plan_id);
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
-- Name: idx_review_application; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_review_application ON public.hr_promotion_review USING btree (application_id);
--
-- Name: idx_review_reviewer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_review_reviewer ON public.hr_promotion_review USING btree (reviewer_id);
--
-- Name: idx_score_evaluation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_score_evaluation ON public.hr_performance_score USING btree (evaluation_id);
--
-- Name: idx_task_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_task_year ON public.hr_performance_task USING btree (year);
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
-- Name: hr_performance_evaluation fk_evaluation_task; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_performance_evaluation
    ADD CONSTRAINT fk_evaluation_task FOREIGN KEY (task_id) REFERENCES public.hr_performance_task(id);
--
-- Name: hr_development_milestone fk_milestone_plan; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_development_milestone
    ADD CONSTRAINT fk_milestone_plan FOREIGN KEY (plan_id) REFERENCES public.hr_development_plan(id);
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
-- Name: hr_promotion_review fk_review_application; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_promotion_review
    ADD CONSTRAINT fk_review_application FOREIGN KEY (application_id) REFERENCES public.hr_promotion_application(id);
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

-- =====================================================
-- 请假管理相关表（补充）
-- =====================================================
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
-- Name: leave_balance uk_leave_balance; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.leave_balance
    ADD CONSTRAINT uk_leave_balance UNIQUE (user_id, leave_type_id, year);


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
-- PostgreSQL database dump complete
--

