-- =====================================================
-- 律所配置补丁脚本
-- =====================================================
-- 说明：为现有数据库添加律所相关配置项
-- 使用方法：如果数据库中缺少律所配置，执行此脚本
-- =====================================================

-- 插入律所相关配置（如果不存在）
INSERT INTO public.sys_config (config_key, config_value, config_name, config_type, description, is_system, created_at, updated_at, deleted)
VALUES 
('firm.name', '', '律所名称', 'STRING', '律师事务所全称，用于合同、函件等文档', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.address', '', '律所地址', 'STRING', '律师事务所地址', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.phone', '', '联系电话', 'STRING', '律所联系电话', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.email', '', '电子邮箱', 'STRING', '律所电子邮箱', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.license', '', '执业许可证号', 'STRING', '律所执业许可证号', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (config_key) DO NOTHING;

