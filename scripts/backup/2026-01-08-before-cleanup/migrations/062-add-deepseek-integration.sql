-- 添加 DeepSeek 大模型集成配置
-- deepseek-reasoner (R1) 是推理模型，适合复杂法律文书生成
-- deepseek-chat 是对话模型，速度更快但推理能力较弱

-- DeepSeek R1 (推理模型)
INSERT INTO sys_external_integration (integration_code, integration_name, integration_type, api_url, description, auth_type, extra_config, enabled, create_time, update_time)
VALUES 
    ('AI_DEEPSEEK_R1', 'DeepSeek R1 (推理)', 'AI', 'https://api.deepseek.com/v1', 
     'DeepSeek R1 推理模型，推理能力强，适合复杂法律文书生成，响应较慢', 'BEARER_TOKEN',
     '{"model": "deepseek-reasoner", "maxTokens": 8192}', false, NOW(), NOW())
ON CONFLICT (integration_code) DO NOTHING;

-- DeepSeek Chat (对话模型)
INSERT INTO sys_external_integration (integration_code, integration_name, integration_type, api_url, description, auth_type, extra_config, enabled, create_time, update_time)
VALUES 
    ('AI_DEEPSEEK', 'DeepSeek Chat (对话)', 'AI', 'https://api.deepseek.com/v1', 
     'DeepSeek Chat 对话模型，响应快速，适合简单文书生成', 'BEARER_TOKEN',
     '{"model": "deepseek-chat", "maxTokens": 4096}', false, NOW(), NOW())
ON CONFLICT (integration_code) DO NOTHING;

