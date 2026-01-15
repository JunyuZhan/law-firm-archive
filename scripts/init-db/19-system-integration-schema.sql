-- =====================================================
-- 免费API集成相关表
-- 包含：节假日缓存、用户企业微信绑定
-- =====================================================

-- 1. 节假日缓存表
CREATE TABLE IF NOT EXISTS public.sys_holiday_cache (
    id BIGSERIAL PRIMARY KEY,
    
    -- 日期信息
    date DATE NOT NULL,                  -- 日期
    year INTEGER NOT NULL,               -- 年份
    month INTEGER NOT NULL,              -- 月份
    
    -- 类型信息
    day_type INTEGER NOT NULL,           -- 0=工作日, 1=周末, 2=法定节假日, 3=调休工作日
    day_type_name VARCHAR(20),           -- 类型名称
    
    -- 节假日信息
    holiday_name VARCHAR(50),            -- 节假日名称（如"春节"）
    is_off BOOLEAN NOT NULL,             -- 是否休息日
    
    -- 元数据
    data_source VARCHAR(20) DEFAULT 'TIMOR',
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_holiday_date UNIQUE (date)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_holiday_year_month ON public.sys_holiday_cache(year, month);
CREATE INDEX IF NOT EXISTS idx_holiday_is_off ON public.sys_holiday_cache(is_off);

COMMENT ON TABLE public.sys_holiday_cache IS '节假日缓存表，存储节假日信息用于工作日计算';
COMMENT ON COLUMN public.sys_holiday_cache.day_type IS '日期类型：0=工作日,1=周末,2=法定节假日,3=调休工作日';
COMMENT ON COLUMN public.sys_holiday_cache.is_off IS '是否休息日（周末+节假日为true，调休工作日为false）';


-- 2. 用户企业微信绑定表
CREATE TABLE IF NOT EXISTS public.sys_user_wecom (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,             -- 系统用户ID
    wecom_userid VARCHAR(100),           -- 企业微信UserId（用于@）
    wecom_mobile VARCHAR(20),            -- 企业微信绑定手机号（备用）
    enabled BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_wecom_user UNIQUE (user_id),
    CONSTRAINT fk_user_wecom_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

COMMENT ON TABLE public.sys_user_wecom IS '用户企业微信绑定表，用于消息推送时@指定用户';


-- 3. 在 sys_external_integration 表中添加企业微信机器人配置
INSERT INTO public.sys_external_integration (
    id,
    integration_code,
    integration_name,
    integration_type,
    description,
    api_url,
    api_key,
    auth_type,
    extra_config,
    enabled,
    deleted
) VALUES (
    100,
    'WECOM_BOT',
    '企业微信机器人',
    'NOTIFICATION',
    '企业微信群机器人，用于推送系统通知到企业微信群。在企业微信群中添加机器人后获取Webhook地址。',
    '',
    '',
    'WEBHOOK',
    '{"mentionAll": false, "enabledTypes": ["TASK", "SCHEDULE", "APPROVAL", "CONTRACT", "REMINDER"]}',
    false,
    false
) ON CONFLICT (integration_code) DO NOTHING;
