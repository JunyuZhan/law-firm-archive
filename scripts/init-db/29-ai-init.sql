-- =====================================================
-- 律师事务所管理系统 - AI使用量计费模块（整合脚本）
-- =====================================================
-- 版本: 1.1.0
-- 日期: 2026-01-17
-- 描述: AI使用量记录、定价配置、用户配额、月度账单
--       菜单权限、字典数据等完整初始化
-- =====================================================

-- =====================================================
-- 第一部分：表结构定义
-- =====================================================

-- 1. AI使用记录表（每次调用一条记录）
CREATE TABLE IF NOT EXISTS public.ai_usage_log (
    id BIGSERIAL,
    
    -- 用户信息
    user_id BIGINT NOT NULL,
    user_name VARCHAR(50),
    department_id BIGINT,
    department_name VARCHAR(50),
    
    -- AI模型信息
    integration_id BIGINT NOT NULL,
    integration_code VARCHAR(50) NOT NULL,
    integration_name VARCHAR(100),
    model_name VARCHAR(100),
    
    -- 调用信息
    request_type VARCHAR(50) NOT NULL,
    business_type VARCHAR(50),
    business_id BIGINT,
    
    -- Token统计
    prompt_tokens INTEGER DEFAULT 0,
    completion_tokens INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    
    -- 费用信息（单价：元/千Token，费用：元）
    prompt_price DECIMAL(10,6) DEFAULT 0,
    completion_price DECIMAL(10,6) DEFAULT 0,
    total_cost DECIMAL(10,4) DEFAULT 0,
    user_cost DECIMAL(10,4) DEFAULT 0,
    charge_ratio INTEGER DEFAULT 100,
    
    -- 调用结果
    success BOOLEAN DEFAULT TRUE,
    error_message VARCHAR(500),
    duration_ms INTEGER,
    
    -- 元数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_ai_usage_user ON public.ai_usage_log(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_time ON public.ai_usage_log(created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_user_month ON public.ai_usage_log(user_id, DATE_TRUNC('month', created_at));
CREATE INDEX IF NOT EXISTS idx_ai_usage_dept ON public.ai_usage_log(department_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_model ON public.ai_usage_log(integration_code);
CREATE INDEX IF NOT EXISTS idx_ai_usage_request_type ON public.ai_usage_log(request_type);

COMMENT ON TABLE public.ai_usage_log IS 'AI使用记录表，记录每次AI调用的详细信息';
COMMENT ON COLUMN public.ai_usage_log.charge_ratio IS '用户承担比例（百分比），100表示全额承担';

-- 2. AI模型定价表
CREATE TABLE IF NOT EXISTS public.ai_pricing_config (
    id BIGSERIAL,
    integration_code VARCHAR(50) NOT NULL,
    model_name VARCHAR(100),
    prompt_price DECIMAL(10,6) NOT NULL DEFAULT 0,
    completion_price DECIMAL(10,6) NOT NULL DEFAULT 0,
    per_call_price DECIMAL(10,4),
    pricing_mode VARCHAR(20) DEFAULT 'TOKEN',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_pricing ON public.ai_pricing_config(integration_code, COALESCE(model_name, '')) WHERE deleted = FALSE;
COMMENT ON TABLE public.ai_pricing_config IS 'AI模型定价表';

-- 3. 用户AI配额表
CREATE TABLE IF NOT EXISTS public.ai_user_quota (
    id BIGSERIAL,
    user_id BIGINT NOT NULL,
    monthly_token_quota BIGINT,
    monthly_cost_quota DECIMAL(10,2),
    current_month_tokens BIGINT DEFAULT 0,
    current_month_cost DECIMAL(10,4) DEFAULT 0,
    quota_reset_date DATE,
    custom_charge_ratio INTEGER,
    exempt_billing BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_ai_quota_user UNIQUE (user_id)
);

COMMENT ON TABLE public.ai_user_quota IS '用户AI使用配额表';

-- 4. AI月度账单表
CREATE TABLE IF NOT EXISTS public.ai_monthly_bill (
    id BIGSERIAL,
    bill_year INTEGER NOT NULL,
    bill_month INTEGER NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(50),
    department_id BIGINT,
    department_name VARCHAR(50),
    total_calls INTEGER DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    prompt_tokens BIGINT DEFAULT 0,
    completion_tokens BIGINT DEFAULT 0,
    total_cost DECIMAL(10,4) DEFAULT 0,
    user_cost DECIMAL(10,4) DEFAULT 0,
    charge_ratio INTEGER DEFAULT 100,
    deduction_status VARCHAR(20) DEFAULT 'PENDING',
    deduction_amount DECIMAL(10,4),
    deducted_at TIMESTAMP,
    deducted_by BIGINT,
    deduction_remark TEXT,
    payroll_deduction_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_bill_month ON public.ai_monthly_bill(bill_year, bill_month, user_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_bill_user ON public.ai_monthly_bill(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_bill_period ON public.ai_monthly_bill(bill_year, bill_month);
CREATE INDEX IF NOT EXISTS idx_ai_bill_status ON public.ai_monthly_bill(deduction_status);
COMMENT ON TABLE public.ai_monthly_bill IS 'AI月度账单表';

-- =====================================================
-- 第二部分：菜单配置
-- =====================================================

-- 我的AI使用（隐藏菜单，通过个人中心访问）
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, menu_type, permission, sort_order, status, visible, created_at, updated_at, deleted)
VALUES (5000, 0, '我的AI使用', '/personal/ai-usage', 'personal/ai-usage/index', 'laptop-outlined', 'MENU', 'ai:usage:view', 99, 'ENABLED', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET parent_id = 0, visible = false;

-- AI费用账单（财务管理下）
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, menu_type, permission, sort_order, status, visible, created_at, updated_at, deleted)
VALUES (5100, 5, 'AI费用账单', '/finance/ai-billing', 'finance/ai-billing/index', 'MoneyCollectOutlined', 'MENU', 'ai:billing:view', 60, 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, path = EXCLUDED.path, icon = EXCLUDED.icon;

-- 注：AI计费配置已整合到系统配置页面的Tab中，不再需要单独菜单

-- 角色菜单关联
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 5000), (1, 5100) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 5100 FROM sys_role r WHERE r.role_name IN ('财务', '律所主任')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = 5100);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 5000 FROM sys_role r WHERE r.role_name IN ('律师', '实习律师', '管理员', '律所主任', '财务', '团队负责人', '行政')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = 5000);

-- =====================================================
-- 第三部分：字典数据
-- =====================================================

-- AI请求类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (70, 'AI请求类型', 'ai_request_type', 'AI调用的请求类型分类', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted) VALUES
(700, 70, '文书生成', 'DOCUMENT_GENERATE', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(701, 70, '智能问答', 'CHAT', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(702, 70, '内容摘要', 'SUMMARY', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(703, 70, '文本润色', 'POLISH', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(704, 70, '法规检索', 'LAW_SEARCH', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(705, 70, '案例分析', 'CASE_ANALYSIS', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(706, 70, '证据整理', 'EVIDENCE_ORGANIZE', 7, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(707, 70, '翻译', 'TRANSLATION', 8, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(708, 70, '其他', 'OTHER', 99, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value;

-- AI业务类型
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (71, 'AI业务类型', 'ai_business_type', 'AI调用的业务类型分类', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted) VALUES
(710, 71, '案件相关', 'MATTER', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(711, 71, '个人使用', 'PERSONAL', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value;

-- AI扣减状态
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (72, 'AI扣减状态', 'ai_deduction_status', 'AI费用账单扣减状态', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, css_class, created_at, updated_at, deleted) VALUES
(720, 72, '待扣减', 'PENDING', 1, 'ENABLED', 'warning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(721, 72, '已扣减', 'DEDUCTED', 2, 'ENABLED', 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(722, 72, '已减免', 'WAIVED', 3, 'ENABLED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value;

-- AI集成编码
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (73, 'AI集成编码', 'ai_integration_code', 'AI模型集成供应商编码', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted) VALUES
(730, 73, 'DeepSeek', 'AI_DEEPSEEK', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(731, 73, 'DeepSeek R1', 'AI_DEEPSEEK_R1', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(732, 73, '通义千问', 'AI_QWEN', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(733, 73, '智谱GLM', 'AI_ZHIPU', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(734, 73, '文心一言', 'AI_WENXIN', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(735, 73, 'Kimi', 'AI_MOONSHOT', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(736, 73, 'OpenAI', 'AI_OPENAI', 7, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(737, 73, 'Claude', 'AI_CLAUDE', 8, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(738, 73, 'Ollama本地', 'AI_OLLAMA', 90, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(739, 73, 'Dify', 'AI_DIFY', 91, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(740, 73, 'LocalAI', 'AI_LOCALAI', 92, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value;

-- AI计费模式
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (74, 'AI计费模式', 'ai_pricing_mode', 'AI模型计费模式', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted) VALUES
(745, 74, '按Token计费', 'TOKEN', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(746, 74, '按次计费', 'PER_CALL', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value;

-- 工资扣减类型 - AI使用费
INSERT INTO sys_dict_item (dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
SELECT (SELECT id FROM sys_dict_type WHERE code = 'salary_deduction_type'), 'AI使用费', 'AI_USAGE', 100, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE EXISTS (SELECT 1 FROM sys_dict_type WHERE code = 'salary_deduction_type')
AND NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE value = 'AI_USAGE' AND dict_type_id = (SELECT id FROM sys_dict_type WHERE code = 'salary_deduction_type'));

-- =====================================================
-- 第四部分：系统配置
-- =====================================================

INSERT INTO sys_config (config_key, config_value, config_name, config_type, description, created_at, updated_at, deleted) VALUES
('ai.billing.enabled', 'false', 'AI计费开关', 'BOOLEAN', '是否启用AI使用计费', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('ai.billing.charge_ratio', '100', 'AI收费比例', 'NUMBER', '用户承担费用的比例（0-100）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('ai.billing.salary_deduction', 'false', '工资扣减开关', 'BOOLEAN', '是否从工资中自动扣减', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('ai.billing.free_tokens', '100000', '月度免费Token', 'NUMBER', '每用户每月免费Token额度（默认10万）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('ai.billing.free_amount', '10', '月度免费金额', 'NUMBER', '每用户每月免费金额（元，默认10）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('ai.billing.max_tokens', '0', '月度Token上限', 'NUMBER', '每用户每月最多可用Token数，0表示无限制', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('ai.billing.max_amount', '0', '月度费用上限', 'NUMBER', '每用户每月最多费用（元），0表示无限制', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (config_key) DO NOTHING;

-- =====================================================
-- 第五部分：模型定价初始化
-- =====================================================

INSERT INTO public.ai_pricing_config (integration_code, model_name, prompt_price, completion_price, pricing_mode) VALUES
-- DeepSeek
('AI_DEEPSEEK', 'deepseek-chat', 0.001, 0.002, 'TOKEN'),
('AI_DEEPSEEK_R1', 'deepseek-reasoner', 0.004, 0.016, 'TOKEN'),
-- 通义千问
('AI_QWEN', 'qwen-max', 0.012, 0.012, 'TOKEN'),
('AI_QWEN', 'qwen-plus', 0.004, 0.012, 'TOKEN'),
('AI_QWEN', 'qwen-turbo', 0.002, 0.006, 'TOKEN'),
-- 智谱
('AI_ZHIPU', 'glm-4', 0.1, 0.1, 'TOKEN'),
('AI_ZHIPU', 'glm-3-turbo', 0.001, 0.001, 'TOKEN'),
-- 文心一言
('AI_WENXIN', 'ernie-bot-4', 0.12, 0.12, 'TOKEN'),
('AI_WENXIN', 'ernie-bot', 0.012, 0.012, 'TOKEN'),
-- Moonshot
('AI_MOONSHOT', 'moonshot-v1-8k', 0.012, 0.012, 'TOKEN'),
('AI_MOONSHOT', 'moonshot-v1-32k', 0.024, 0.024, 'TOKEN'),
-- OpenAI
('AI_OPENAI', 'gpt-4', 0.21, 0.42, 'TOKEN'),
('AI_OPENAI', 'gpt-4-turbo', 0.07, 0.21, 'TOKEN'),
('AI_OPENAI', 'gpt-3.5-turbo', 0.0035, 0.0105, 'TOKEN'),
-- Claude
('AI_CLAUDE', 'claude-3-opus', 0.105, 0.525, 'TOKEN'),
('AI_CLAUDE', 'claude-3-sonnet', 0.021, 0.105, 'TOKEN'),
('AI_CLAUDE', 'claude-3-haiku', 0.00175, 0.00875, 'TOKEN'),
-- 本地部署（免费）
('AI_OLLAMA', NULL, 0, 0, 'TOKEN'),
('AI_DIFY', NULL, 0, 0, 'TOKEN'),
('AI_LOCALAI', NULL, 0, 0, 'TOKEN')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 添加主键约束（显式命名）
-- =====================================================
ALTER TABLE ONLY public.ai_usage_log
    ADD CONSTRAINT pk_ai_usage_log PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_pricing_config
    ADD CONSTRAINT pk_ai_pricing_config PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_user_quota
    ADD CONSTRAINT pk_ai_user_quota PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_monthly_bill
    ADD CONSTRAINT pk_ai_monthly_bill PRIMARY KEY (id);

-- =====================================================
-- 完成
-- =====================================================
