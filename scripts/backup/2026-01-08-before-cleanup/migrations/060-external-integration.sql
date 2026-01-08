-- 外部系统集成配置表
-- 用于管理档案馆API、AI大模型API等外部系统的连接配置

CREATE TABLE IF NOT EXISTS sys_external_integration (
    id BIGSERIAL PRIMARY KEY,
    
    -- 基本信息
    integration_code VARCHAR(50) NOT NULL UNIQUE,  -- 集成编码，如：ARCHIVE_CITY, AI_OPENAI
    integration_name VARCHAR(100) NOT NULL,         -- 集成名称，如：市档案馆、OpenAI
    integration_type VARCHAR(50) NOT NULL,          -- 类型：ARCHIVE-档案系统, AI-AI大模型, OTHER-其他
    description TEXT,                               -- 描述
    
    -- API配置
    api_url VARCHAR(500),                           -- API基础地址
    api_key VARCHAR(500),                           -- API密钥
    api_secret VARCHAR(500),                        -- API密钥（加密存储）
    auth_type VARCHAR(50) DEFAULT 'API_KEY',        -- 认证方式：API_KEY, BEARER_TOKEN, BASIC, OAUTH2
    
    -- 额外配置（JSON格式，存储特定系统的配置）
    extra_config JSONB,                             -- 如：模型名称、超时时间、最大重试次数等
    
    -- 状态
    enabled BOOLEAN DEFAULT false,                  -- 是否启用
    last_test_time TIMESTAMP,                       -- 最后测试时间
    last_test_result VARCHAR(20),                   -- 最后测试结果：SUCCESS, FAILED
    last_test_message TEXT,                         -- 最后测试消息
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT false
);

-- 创建索引
CREATE INDEX idx_external_integration_type ON sys_external_integration(integration_type);
CREATE INDEX idx_external_integration_enabled ON sys_external_integration(enabled);

-- 添加注释
COMMENT ON TABLE sys_external_integration IS '外部系统集成配置表';
COMMENT ON COLUMN sys_external_integration.integration_code IS '集成编码，唯一标识';
COMMENT ON COLUMN sys_external_integration.integration_type IS '类型：ARCHIVE-档案系统, AI-AI大模型, OTHER-其他';
COMMENT ON COLUMN sys_external_integration.auth_type IS '认证方式：API_KEY, BEARER_TOKEN, BASIC, OAUTH2';
COMMENT ON COLUMN sys_external_integration.extra_config IS 'JSON格式的额外配置';

-- 插入预置的集成配置
INSERT INTO sys_external_integration (integration_code, integration_name, integration_type, description, auth_type, extra_config, enabled)
VALUES 
    -- 档案系统
    ('ARCHIVE_CITY', '市档案馆', 'ARCHIVE', '对接市档案馆档案管理系统，用于档案迁移', 'API_KEY', 
     '{"timeout": 30000, "retryCount": 3}', false),
    ('ARCHIVE_DISTRICT', '区档案馆', 'ARCHIVE', '对接区档案馆档案管理系统，用于档案迁移', 'API_KEY',
     '{"timeout": 30000, "retryCount": 3}', false),
    ('ARCHIVE_THIRD_PARTY', '第三方档案系统', 'ARCHIVE', '对接第三方档案管理系统', 'API_KEY',
     '{"timeout": 30000, "retryCount": 3}', false),
    
    -- AI大模型
    ('AI_OPENAI', 'OpenAI (GPT)', 'AI', 'OpenAI GPT系列大模型，支持GPT-4、GPT-3.5等', 'BEARER_TOKEN',
     '{"model": "gpt-4", "maxTokens": 4096, "temperature": 0.7, "timeout": 60000}', false),
    ('AI_CLAUDE', 'Anthropic (Claude)', 'AI', 'Anthropic Claude系列大模型', 'BEARER_TOKEN',
     '{"model": "claude-3-opus-20240229", "maxTokens": 4096, "timeout": 60000}', false),
    ('AI_QWEN', '通义千问', 'AI', '阿里云通义千问大模型', 'API_KEY',
     '{"model": "qwen-max", "maxTokens": 4096, "timeout": 60000}', false),
    ('AI_WENXIN', '文心一言', 'AI', '百度文心一言大模型', 'API_KEY',
     '{"model": "ernie-bot-4", "maxTokens": 4096, "timeout": 60000}', false),
    ('AI_ZHIPU', '智谱清言 (GLM)', 'AI', '智谱AI GLM系列大模型', 'API_KEY',
     '{"model": "glm-4", "maxTokens": 4096, "timeout": 60000}', false),
    ('AI_DEEPSEEK', 'DeepSeek', 'AI', 'DeepSeek大模型，支持代码生成等', 'BEARER_TOKEN',
     '{"model": "deepseek-chat", "maxTokens": 4096, "timeout": 60000}', false)
ON CONFLICT (integration_code) DO NOTHING;

-- 添加菜单（parent_id=2 是系统管理）
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, deleted)
VALUES 
    (159, 2, '外部系统集成', '/system/integration', 'system/integration/index', NULL, 'CloudOutlined', 'MENU', 'system:integration:list', 99, true, 'ENABLED', false, true, NOW(), NOW(), false)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    permission = EXCLUDED.permission,
    sort_order = EXCLUDED.sort_order;

-- 为管理员角色添加菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 159 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 159);

