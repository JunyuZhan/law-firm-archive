-- =====================================================
-- 自定义报表模块数据库表设计
-- M11-023: 自定义报表配置
-- M11-024: 定时报表
-- =====================================================

-- 1. 自定义报表模板表
CREATE TABLE IF NOT EXISTS workbench_report_template (
    id BIGSERIAL PRIMARY KEY,
    template_no VARCHAR(50) NOT NULL UNIQUE,           -- 模板编号
    template_name VARCHAR(100) NOT NULL,               -- 模板名称
    description VARCHAR(500),                          -- 模板描述
    data_source VARCHAR(50) NOT NULL,                  -- 数据源：MATTER-案件, CLIENT-客户, FINANCE-财务, TIMESHEET-工时, EMPLOYEE-员工
    
    -- 字段配置（JSON数组）
    -- 格式: [{"field": "client_name", "label": "客户名称", "type": "string", "visible": true, "sortable": true}]
    field_config TEXT NOT NULL,
    
    -- 筛选条件配置（JSON数组）
    -- 格式: [{"field": "status", "label": "状态", "type": "select", "options": ["ACTIVE", "CLOSED"]}]
    filter_config TEXT,
    
    -- 分组配置（JSON数组）
    -- 格式: [{"field": "matter_type", "label": "案件类型"}]
    group_config TEXT,
    
    -- 排序配置（JSON数组）
    -- 格式: [{"field": "created_at", "direction": "DESC"}]
    sort_config TEXT,
    
    -- 聚合配置（JSON数组）
    -- 格式: [{"field": "amount", "function": "SUM", "label": "总金额"}]
    aggregate_config TEXT,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',               -- 状态：ACTIVE-启用, INACTIVE-停用
    is_system BOOLEAN DEFAULT FALSE,                   -- 是否系统内置模板
    created_by BIGINT,
    created_by_name VARCHAR(50),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- 2. 定时报表任务表
CREATE TABLE IF NOT EXISTS workbench_scheduled_report (
    id BIGSERIAL PRIMARY KEY,
    task_no VARCHAR(50) NOT NULL UNIQUE,               -- 任务编号
    task_name VARCHAR(100) NOT NULL,                   -- 任务名称
    description VARCHAR(500),                          -- 任务描述
    
    template_id BIGINT NOT NULL,                       -- 关联的报表模板ID
    
    -- 定时配置
    schedule_type VARCHAR(20) NOT NULL,                -- 调度类型：DAILY-每日, WEEKLY-每周, MONTHLY-每月, CRON-自定义
    cron_expression VARCHAR(100),                      -- Cron表达式（schedule_type=CRON时使用）
    execute_time VARCHAR(10),                          -- 执行时间 HH:mm（DAILY/WEEKLY/MONTHLY时使用）
    execute_day_of_week INTEGER,                       -- 执行星期几 1-7（WEEKLY时使用）
    execute_day_of_month INTEGER,                      -- 执行日期 1-31（MONTHLY时使用）
    
    -- 报表参数（JSON格式，用于筛选条件的默认值）
    report_parameters TEXT,
    
    -- 输出配置
    output_format VARCHAR(20) DEFAULT 'EXCEL',         -- 输出格式：EXCEL, PDF
    
    -- 通知配置
    notify_enabled BOOLEAN DEFAULT FALSE,              -- 是否启用通知
    notify_emails TEXT,                                -- 通知邮箱列表（逗号分隔）
    notify_user_ids TEXT,                              -- 通知用户ID列表（逗号分隔）
    
    status VARCHAR(20) DEFAULT 'ACTIVE',               -- 状态：ACTIVE-启用, PAUSED-暂停, INACTIVE-停用
    
    -- 执行统计
    last_execute_time TIMESTAMP,                       -- 上次执行时间
    last_execute_status VARCHAR(20),                   -- 上次执行状态：SUCCESS, FAILED
    next_execute_time TIMESTAMP,                       -- 下次执行时间
    total_execute_count INTEGER DEFAULT 0,             -- 总执行次数
    success_count INTEGER DEFAULT 0,                   -- 成功次数
    fail_count INTEGER DEFAULT 0,                      -- 失败次数
    
    created_by BIGINT,
    created_by_name VARCHAR(50),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_scheduled_report_template FOREIGN KEY (template_id) REFERENCES workbench_report_template(id)
);

-- 3. 定时报表执行记录表
CREATE TABLE IF NOT EXISTS workbench_scheduled_report_log (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,                           -- 定时任务ID
    task_no VARCHAR(50),                               -- 任务编号
    
    execute_time TIMESTAMP NOT NULL,                   -- 执行时间
    status VARCHAR(20) NOT NULL,                       -- 执行状态：RUNNING-执行中, SUCCESS-成功, FAILED-失败
    
    -- 执行结果
    report_id BIGINT,                                  -- 生成的报表ID
    file_url VARCHAR(500),                             -- 文件URL
    file_size BIGINT,                                  -- 文件大小
    
    -- 执行信息
    duration_ms BIGINT,                                -- 执行耗时（毫秒）
    error_message TEXT,                                -- 错误信息
    
    -- 通知结果
    notify_status VARCHAR(20),                         -- 通知状态：SENT-已发送, FAILED-发送失败, SKIPPED-跳过
    notify_result TEXT,                                -- 通知结果详情
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_scheduled_log_task FOREIGN KEY (task_id) REFERENCES workbench_scheduled_report(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_report_template_status ON workbench_report_template(status);
CREATE INDEX IF NOT EXISTS idx_report_template_data_source ON workbench_report_template(data_source);
CREATE INDEX IF NOT EXISTS idx_report_template_created_by ON workbench_report_template(created_by);

CREATE INDEX IF NOT EXISTS idx_scheduled_report_status ON workbench_scheduled_report(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_report_template ON workbench_scheduled_report(template_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_report_next_execute ON workbench_scheduled_report(next_execute_time);

CREATE INDEX IF NOT EXISTS idx_scheduled_log_task ON workbench_scheduled_report_log(task_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_log_execute_time ON workbench_scheduled_report_log(execute_time);
CREATE INDEX IF NOT EXISTS idx_scheduled_log_status ON workbench_scheduled_report_log(status);

-- 添加注释
COMMENT ON TABLE workbench_report_template IS '自定义报表模板表';
COMMENT ON TABLE workbench_scheduled_report IS '定时报表任务表';
COMMENT ON TABLE workbench_scheduled_report_log IS '定时报表执行记录表';

-- 插入系统内置报表模板
INSERT INTO workbench_report_template (template_no, template_name, description, data_source, field_config, filter_config, sort_config, is_system, created_by_name) VALUES
-- 案件统计报表模板
('TPL_MATTER_STAT', '案件统计报表', '按案件类型、状态统计案件数量和金额', 'MATTER',
 '[{"field":"matter_no","label":"案件编号","type":"string","visible":true},{"field":"matter_name","label":"案件名称","type":"string","visible":true},{"field":"matter_type","label":"案件类型","type":"string","visible":true},{"field":"status","label":"状态","type":"string","visible":true},{"field":"client_name","label":"客户名称","type":"string","visible":true},{"field":"lawyer_name","label":"主办律师","type":"string","visible":true},{"field":"contract_amount","label":"合同金额","type":"decimal","visible":true},{"field":"received_amount","label":"已收金额","type":"decimal","visible":true},{"field":"created_at","label":"创建时间","type":"datetime","visible":true}]',
 '[{"field":"status","label":"状态","type":"select","options":["PENDING","ACTIVE","CLOSED","ARCHIVED"]},{"field":"matter_type","label":"案件类型","type":"select"},{"field":"created_at","label":"创建时间","type":"dateRange"}]',
 '[{"field":"created_at","direction":"DESC"}]',
 TRUE, '系统'),

-- 客户分析报表模板
('TPL_CLIENT_ANALYSIS', '客户分析报表', '分析客户来源、类型、合作情况', 'CLIENT',
 '[{"field":"client_no","label":"客户编号","type":"string","visible":true},{"field":"name","label":"客户名称","type":"string","visible":true},{"field":"client_type","label":"客户类型","type":"string","visible":true},{"field":"industry","label":"所属行业","type":"string","visible":true},{"field":"source","label":"客户来源","type":"string","visible":true},{"field":"level","label":"客户级别","type":"string","visible":true},{"field":"status","label":"状态","type":"string","visible":true},{"field":"matter_count","label":"案件数","type":"integer","visible":true},{"field":"total_amount","label":"累计金额","type":"decimal","visible":true},{"field":"first_cooperation_date","label":"首次合作","type":"date","visible":true}]',
 '[{"field":"client_type","label":"客户类型","type":"select","options":["INDIVIDUAL","ENTERPRISE","GOVERNMENT","OTHER"]},{"field":"level","label":"客户级别","type":"select","options":["A","B","C"]},{"field":"status","label":"状态","type":"select","options":["POTENTIAL","ACTIVE","INACTIVE","BLACKLIST"]}]',
 '[{"field":"total_amount","direction":"DESC"}]',
 TRUE, '系统'),

-- 收入明细报表模板
('TPL_REVENUE_DETAIL', '收入明细报表', '详细记录每笔收款信息', 'FINANCE',
 '[{"field":"payment_no","label":"收款编号","type":"string","visible":true},{"field":"matter_name","label":"案件名称","type":"string","visible":true},{"field":"client_name","label":"客户名称","type":"string","visible":true},{"field":"amount","label":"收款金额","type":"decimal","visible":true},{"field":"payment_method","label":"收款方式","type":"string","visible":true},{"field":"payment_date","label":"收款日期","type":"date","visible":true},{"field":"payer_name","label":"付款人","type":"string","visible":true},{"field":"remark","label":"备注","type":"string","visible":true}]',
 '[{"field":"payment_date","label":"收款日期","type":"dateRange"},{"field":"payment_method","label":"收款方式","type":"select"}]',
 '[{"field":"payment_date","direction":"DESC"}]',
 TRUE, '系统'),

-- 工时统计报表模板
('TPL_TIMESHEET_STAT', '工时统计报表', '统计律师工时记录', 'TIMESHEET',
 '[{"field":"lawyer_name","label":"律师姓名","type":"string","visible":true},{"field":"matter_name","label":"案件名称","type":"string","visible":true},{"field":"work_date","label":"工作日期","type":"date","visible":true},{"field":"hours","label":"工时(小时)","type":"decimal","visible":true},{"field":"hourly_rate","label":"小时费率","type":"decimal","visible":true},{"field":"amount","label":"工时金额","type":"decimal","visible":true},{"field":"work_content","label":"工作内容","type":"string","visible":true},{"field":"status","label":"状态","type":"string","visible":true}]',
 '[{"field":"work_date","label":"工作日期","type":"dateRange"},{"field":"status","label":"状态","type":"select","options":["DRAFT","SUBMITTED","APPROVED","REJECTED"]}]',
 '[{"field":"work_date","direction":"DESC"}]',
 TRUE, '系统'),

-- 员工信息报表模板
('TPL_EMPLOYEE_INFO', '员工信息报表', '员工基本信息统计', 'EMPLOYEE',
 '[{"field":"employee_no","label":"员工编号","type":"string","visible":true},{"field":"name","label":"姓名","type":"string","visible":true},{"field":"department_name","label":"部门","type":"string","visible":true},{"field":"position","label":"职位","type":"string","visible":true},{"field":"phone","label":"联系电话","type":"string","visible":true},{"field":"email","label":"邮箱","type":"string","visible":true},{"field":"join_date","label":"入职日期","type":"date","visible":true},{"field":"status","label":"状态","type":"string","visible":true}]',
 '[{"field":"department_id","label":"部门","type":"select"},{"field":"status","label":"状态","type":"select","options":["ACTIVE","INACTIVE"]}]',
 '[{"field":"join_date","direction":"DESC"}]',
 TRUE, '系统');
