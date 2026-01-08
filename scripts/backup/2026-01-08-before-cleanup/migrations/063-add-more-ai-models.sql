-- 添加更多 AI 大模型配置
-- 包括云端服务和本地部署方案

-- 云端大模型

-- Moonshot (Kimi) - 月之暗面
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    'Moonshot (Kimi)', 'AI_MOONSHOT', 'AI', 'https://api.moonshot.cn/v1', '',
    false, '月之暗面 Kimi 大模型，支持超长上下文',
    '{"model": "moonshot-v1-8k", "maxTokens": 4096}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- Yi (零一万物)
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    '零一万物 Yi', 'AI_YI', 'AI', 'https://api.lingyiwanwu.com/v1', '',
    false, '零一万物 Yi 大模型',
    '{"model": "yi-34b-chat-0205", "maxTokens": 4096}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- MiniMax
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    'MiniMax', 'AI_MINIMAX', 'AI', 'https://api.minimax.chat/v1', '',
    false, 'MiniMax 大模型',
    '{"model": "abab5.5-chat", "groupId": ""}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- 本地部署/私有化大模型

-- Dify
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    'Dify', 'AI_DIFY', 'AI', 'http://localhost:3000/v1', '',
    false, 'Dify 开源 LLM 应用开发平台，支持多种大模型编排',
    '{"apiType": "chat", "user": "law-firm-user", "responseMode": "blocking"}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- Ollama
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    'Ollama', 'AI_OLLAMA', 'AI', 'http://localhost:11434', '',
    false, 'Ollama 本地大模型运行工具，支持 Llama、Mistral、Gemma 等',
    '{"model": "llama2", "apiType": "chat", "temperature": 0.7}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- LocalAI
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    'LocalAI', 'AI_LOCALAI', 'AI', 'http://localhost:8080/v1', '',
    false, 'LocalAI - OpenAI 兼容的本地 AI 后端',
    '{"model": "gpt-3.5-turbo", "maxTokens": 4096}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- vLLM
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    'vLLM', 'AI_VLLM', 'AI', 'http://localhost:8000/v1', '',
    false, 'vLLM 高性能 LLM 推理和服务框架',
    '{"model": "meta-llama/Llama-2-7b-chat-hf", "maxTokens": 4096}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- Xinference
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    'Xinference', 'AI_XINFERENCE', 'AI', 'http://localhost:9997/v1', '',
    false, 'Xinference 开源分布式推理框架',
    '{"model": "qwen-chat", "maxTokens": 4096}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- OneAPI
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    'OneAPI', 'AI_ONEAPI', 'AI', 'http://localhost:3000/v1', '',
    false, 'OneAPI 多模型代理服务，统一管理多个 AI 模型',
    '{"model": "gpt-3.5-turbo", "maxTokens": 4096}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- OpenAI 兼容 API (通用)
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    'OpenAI 兼容 API', 'AI_OPENAI_COMPATIBLE', 'AI', 'http://localhost:8080/v1', '',
    false, '通用 OpenAI 兼容 API 接口，适用于任何兼容 OpenAI 格式的服务',
    '{"model": "default", "maxTokens": 4096}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- 自定义 API
INSERT INTO sys_external_integration (
    integration_name, integration_code, integration_type, api_url, api_key,
    enabled, description, extra_config, create_time, update_time
) VALUES (
    '自定义 API', 'AI_CUSTOM', 'AI', 'http://localhost:8080', '',
    false, '自定义 API 接口，支持自定义请求格式和响应解析',
    '{"requestTemplate": "openai", "responsePath": "choices.0.message.content", "endpoint": "chat/completions"}'::jsonb,
    NOW(), NOW()
) ON CONFLICT (integration_code) DO NOTHING;

-- 添加说明注释
COMMENT ON TABLE sys_external_integration IS '外部系统集成配置表，支持多种 AI 大模型：
云端服务：OpenAI、Claude、通义千问、文心一言、智谱、DeepSeek、Moonshot、Yi、MiniMax
本地部署：Dify、Ollama、LocalAI、vLLM、Xinference、OneAPI、OpenAI 兼容 API、自定义 API';

