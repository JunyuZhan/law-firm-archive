-- 更新律所基本信息配置（请修改为实际律所信息后执行）
-- 执行方式：在数据库管理工具（如 DBeaver、Navicat 等）中执行此 SQL

UPDATE sys_config SET config_value = '贵州XX律师事务所' WHERE config_key = 'firm.name';
UPDATE sys_config SET config_value = '贵州省贵阳市云岩区中华中路XX号XX大厦X层' WHERE config_key = 'firm.address';
UPDATE sys_config SET config_value = '0851-8888XXXX' WHERE config_key = 'firm.phone';
UPDATE sys_config SET config_value = '张主任' WHERE config_key = 'firm.legal.rep';
UPDATE sys_config SET config_value = '黔司律所证字第0001号' WHERE config_key = 'firm.license';
UPDATE sys_config SET config_value = '550001' WHERE config_key = 'firm.postcode';
UPDATE sys_config SET config_value = '0851-8888XXXX' WHERE config_key = 'firm.fax';
UPDATE sys_config SET config_value = 'contact@lawfirm.com' WHERE config_key = 'firm.email';
UPDATE sys_config SET config_value = 'www.lawfirm.com' WHERE config_key = 'firm.website';

-- 验证更新结果
SELECT config_key, config_value, config_name FROM sys_config WHERE config_key LIKE 'firm.%' ORDER BY config_key;
