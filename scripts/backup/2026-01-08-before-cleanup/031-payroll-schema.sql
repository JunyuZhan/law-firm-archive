--
-- 工资管理模块数据库表结构
-- 创建时间：2026-01-06
--

-- ============================================
-- 1. 工资表（hr_payroll_sheet）
-- ============================================
CREATE TABLE IF NOT EXISTS public.hr_payroll_sheet (
    id BIGSERIAL PRIMARY KEY,
    payroll_no VARCHAR(50) NOT NULL UNIQUE,
    payroll_year INTEGER NOT NULL,
    payroll_month INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    total_employees INTEGER DEFAULT 0,
    total_gross_amount NUMERIC(15,2) DEFAULT 0,
    total_deduction_amount NUMERIC(15,2) DEFAULT 0,
    total_net_amount NUMERIC(15,2) DEFAULT 0,
    confirmed_count INTEGER DEFAULT 0,
    submitted_at TIMESTAMP,
    submitted_by BIGINT,
    finance_confirmed_at TIMESTAMP,
    finance_confirmed_by BIGINT,
    issued_at TIMESTAMP,
    issued_by BIGINT,
    payment_method VARCHAR(50),
    payment_voucher_url VARCHAR(500),
    remark VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(payroll_year, payroll_month, deleted)
);

COMMENT ON TABLE public.hr_payroll_sheet IS '工资表';
COMMENT ON COLUMN public.hr_payroll_sheet.payroll_no IS '工资表编号（格式：PAY-YYYYMM）';
COMMENT ON COLUMN public.hr_payroll_sheet.payroll_year IS '工资年份';
COMMENT ON COLUMN public.hr_payroll_sheet.payroll_month IS '工资月份（1-12）';
COMMENT ON COLUMN public.hr_payroll_sheet.status IS '状态：DRAFT-草稿, PENDING_CONFIRM-待确认, CONFIRMED-已确认, FINANCE_CONFIRMED-财务已确认, ISSUED-已发放';
COMMENT ON COLUMN public.hr_payroll_sheet.total_employees IS '总人数';
COMMENT ON COLUMN public.hr_payroll_sheet.total_gross_amount IS '应发工资总额';
COMMENT ON COLUMN public.hr_payroll_sheet.total_deduction_amount IS '扣减总额';
COMMENT ON COLUMN public.hr_payroll_sheet.total_net_amount IS '实发工资总额';
COMMENT ON COLUMN public.hr_payroll_sheet.confirmed_count IS '已确认人数';
COMMENT ON COLUMN public.hr_payroll_sheet.submitted_at IS '提交时间';
COMMENT ON COLUMN public.hr_payroll_sheet.submitted_by IS '提交人ID';
COMMENT ON COLUMN public.hr_payroll_sheet.finance_confirmed_at IS '财务确认时间';
COMMENT ON COLUMN public.hr_payroll_sheet.finance_confirmed_by IS '财务确认人ID';
COMMENT ON COLUMN public.hr_payroll_sheet.issued_at IS '发放时间';
COMMENT ON COLUMN public.hr_payroll_sheet.issued_by IS '发放人ID';
COMMENT ON COLUMN public.hr_payroll_sheet.payment_method IS '发放方式：BANK_TRANSFER-银行转账, CASH-现金, OTHER-其他';
COMMENT ON COLUMN public.hr_payroll_sheet.payment_voucher_url IS '发放凭证URL';
COMMENT ON COLUMN public.hr_payroll_sheet.remark IS '备注';

-- ============================================
-- 2. 工资明细表（hr_payroll_item）
-- ============================================
CREATE TABLE IF NOT EXISTS public.hr_payroll_item (
    id BIGSERIAL PRIMARY KEY,
    payroll_sheet_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    user_id BIGINT,
    employee_no VARCHAR(50),
    employee_name VARCHAR(50),
    gross_amount NUMERIC(15,2) DEFAULT 0,
    deduction_amount NUMERIC(15,2) DEFAULT 0,
    net_amount NUMERIC(15,2) DEFAULT 0,
    confirm_status VARCHAR(20) DEFAULT 'PENDING',
    confirmed_at TIMESTAMP,
    confirm_comment VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (payroll_sheet_id) REFERENCES public.hr_payroll_sheet(id),
    FOREIGN KEY (employee_id) REFERENCES public.hr_employee(id),
    UNIQUE(payroll_sheet_id, employee_id, deleted)
);

COMMENT ON TABLE public.hr_payroll_item IS '工资明细表';
COMMENT ON COLUMN public.hr_payroll_item.payroll_sheet_id IS '工资表ID';
COMMENT ON COLUMN public.hr_payroll_item.employee_id IS '员工ID（关联hr_employee.id）';
COMMENT ON COLUMN public.hr_payroll_item.user_id IS '用户ID（关联sys_user.id，冗余字段便于查询）';
COMMENT ON COLUMN public.hr_payroll_item.employee_no IS '工号（冗余字段）';
COMMENT ON COLUMN public.hr_payroll_item.employee_name IS '员工姓名（冗余字段）';
COMMENT ON COLUMN public.hr_payroll_item.gross_amount IS '应发工资（基本工资+提成+绩效+其他）';
COMMENT ON COLUMN public.hr_payroll_item.deduction_amount IS '扣减总额（税+社保+公积金+其他）';
COMMENT ON COLUMN public.hr_payroll_item.net_amount IS '实发工资（应发-扣减）';
COMMENT ON COLUMN public.hr_payroll_item.confirm_status IS '确认状态：PENDING-待确认, CONFIRMED-已确认, REJECTED-已拒绝';
COMMENT ON COLUMN public.hr_payroll_item.confirmed_at IS '确认时间';
COMMENT ON COLUMN public.hr_payroll_item.confirm_comment IS '确认意见（拒绝时填写原因）';

-- ============================================
-- 3. 工资收入项表（hr_payroll_income）
-- ============================================
CREATE TABLE IF NOT EXISTS public.hr_payroll_income (
    id BIGSERIAL PRIMARY KEY,
    payroll_item_id BIGINT NOT NULL,
    income_type VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    remark VARCHAR(500),
    source_type VARCHAR(50),
    source_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (payroll_item_id) REFERENCES public.hr_payroll_item(id) ON DELETE CASCADE
);

COMMENT ON TABLE public.hr_payroll_income IS '工资收入项表';
COMMENT ON COLUMN public.hr_payroll_income.payroll_item_id IS '工资明细ID';
COMMENT ON COLUMN public.hr_payroll_income.income_type IS '收入类型：BASE_SALARY-基本工资, COMMISSION-提成, PERFORMANCE_BONUS-绩效奖金, OTHER_ALLOWANCE-其他津贴';
COMMENT ON COLUMN public.hr_payroll_income.amount IS '金额';
COMMENT ON COLUMN public.hr_payroll_income.remark IS '备注';
COMMENT ON COLUMN public.hr_payroll_income.source_type IS '数据来源：AUTO-自动汇总, MANUAL-手动输入, IMPORT-导入';
COMMENT ON COLUMN public.hr_payroll_income.source_id IS '来源ID（如提成记录ID、合同ID等）';

-- ============================================
-- 4. 工资扣减项表（hr_payroll_deduction）
-- ============================================
CREATE TABLE IF NOT EXISTS public.hr_payroll_deduction (
    id BIGSERIAL PRIMARY KEY,
    payroll_item_id BIGINT NOT NULL,
    deduction_type VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    remark VARCHAR(500),
    source_type VARCHAR(50) DEFAULT 'MANUAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (payroll_item_id) REFERENCES public.hr_payroll_item(id) ON DELETE CASCADE
);

COMMENT ON TABLE public.hr_payroll_deduction IS '工资扣减项表';
COMMENT ON COLUMN public.hr_payroll_deduction.payroll_item_id IS '工资明细ID';
COMMENT ON COLUMN public.hr_payroll_deduction.deduction_type IS '扣减类型：INCOME_TAX-个人所得税, SOCIAL_INSURANCE-社保个人部分, HOUSING_FUND-公积金个人部分, OTHER_DEDUCTION-其他扣款';
COMMENT ON COLUMN public.hr_payroll_deduction.amount IS '金额';
COMMENT ON COLUMN public.hr_payroll_deduction.remark IS '备注';
COMMENT ON COLUMN public.hr_payroll_deduction.source_type IS '数据来源：AUTO-自动计算, MANUAL-手动输入, IMPORT-导入';

-- ============================================
-- 5. 创建索引
-- ============================================
CREATE INDEX idx_payroll_sheet_year_month ON public.hr_payroll_sheet(payroll_year, payroll_month);
CREATE INDEX idx_payroll_sheet_status ON public.hr_payroll_sheet(status);
CREATE INDEX idx_payroll_item_sheet_id ON public.hr_payroll_item(payroll_sheet_id);
CREATE INDEX idx_payroll_item_employee_id ON public.hr_payroll_item(employee_id);
CREATE INDEX idx_payroll_item_user_id ON public.hr_payroll_item(user_id);
CREATE INDEX idx_payroll_income_item_id ON public.hr_payroll_income(payroll_item_id);
CREATE INDEX idx_payroll_deduction_item_id ON public.hr_payroll_deduction(payroll_item_id);

-- ============================================
-- 6. 创建序列
-- ============================================
CREATE SEQUENCE IF NOT EXISTS public.hr_payroll_sheet_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS public.hr_payroll_item_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS public.hr_payroll_income_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS public.hr_payroll_deduction_id_seq START WITH 1 INCREMENT BY 1;

-- ============================================
-- 7. 设置序列所有者
-- ============================================
ALTER SEQUENCE public.hr_payroll_sheet_id_seq OWNED BY public.hr_payroll_sheet.id;
ALTER SEQUENCE public.hr_payroll_item_id_seq OWNED BY public.hr_payroll_item.id;
ALTER SEQUENCE public.hr_payroll_income_id_seq OWNED BY public.hr_payroll_income.id;
ALTER SEQUENCE public.hr_payroll_deduction_id_seq OWNED BY public.hr_payroll_deduction.id;

-- ============================================
-- 8. 设置默认值
-- ============================================
ALTER TABLE public.hr_payroll_sheet ALTER COLUMN id SET DEFAULT nextval('public.hr_payroll_sheet_id_seq');
ALTER TABLE public.hr_payroll_item ALTER COLUMN id SET DEFAULT nextval('public.hr_payroll_item_id_seq');
ALTER TABLE public.hr_payroll_income ALTER COLUMN id SET DEFAULT nextval('public.hr_payroll_income_id_seq');
ALTER TABLE public.hr_payroll_deduction ALTER COLUMN id SET DEFAULT nextval('public.hr_payroll_deduction_id_seq');

