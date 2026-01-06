-- =====================================================
-- 系统配置模块数据库表结构
-- 包含：系统参数配置
-- 注意：菜单表和角色菜单关联表已在 01-schema.sql 中定义
-- =====================================================

-- -----------------------------------------------------
-- 1. 系统参数配置表 (sys_config)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,          -- 参数键
    config_value VARCHAR(2000),                       -- 参数值
    config_name VARCHAR(100) NOT NULL,                -- 参数名称
    config_type VARCHAR(20) DEFAULT 'STRING',         -- 值类型: STRING/NUMBER/BOOLEAN/JSON
    description VARCHAR(500),                         -- 描述
    is_system BOOLEAN DEFAULT FALSE,                  -- 是否系统内置
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_config IS '系统参数配置表';

-- 初始化系统配置
INSERT INTO sys_config (config_key, config_value, config_name, config_type, description, is_system) VALUES
('sys.name', '智慧律所管理系统', '系统名称', 'STRING', '系统显示名称', TRUE),
('sys.version', '1.0.0', '系统版本', 'STRING', '当前系统版本号', TRUE),
('sys.copyright', '© 2026 智慧律所', '版权信息', 'STRING', '页面底部版权信息', TRUE),
('sys.login.captcha', 'true', '登录验证码', 'BOOLEAN', '是否启用登录验证码', TRUE),
('sys.password.minLength', '8', '密码最小长度', 'NUMBER', '用户密码最小长度要求', TRUE),
('sys.session.timeout', '30', '会话超时时间', 'NUMBER', '会话超时时间(分钟)', TRUE),
('sys.upload.maxSize', '10', '上传文件大小限制', 'NUMBER', '单个文件上传大小限制(MB)', TRUE),
('sys.upload.allowTypes', 'doc,docx,pdf,xls,xlsx,jpg,png', '允许上传类型', 'STRING', '允许上传的文件类型', TRUE),
('firm.name', '', '律所名称', 'STRING', '律师事务所全称', FALSE),
('firm.address', '', '律所地址', 'STRING', '律师事务所地址', FALSE),
('firm.phone', '', '联系电话', 'STRING', '律所联系电话', FALSE),
('firm.email', '', '电子邮箱', 'STRING', '律所电子邮箱', FALSE),
('firm.license', '', '执业许可证号', 'STRING', '律所执业许可证号', FALSE),
('work.startTime', '09:00', '上班时间', 'STRING', '标准上班时间', FALSE),
('work.endTime', '18:00', '下班时间', 'STRING', '标准下班时间', FALSE)
ON CONFLICT (config_key) DO NOTHING;
