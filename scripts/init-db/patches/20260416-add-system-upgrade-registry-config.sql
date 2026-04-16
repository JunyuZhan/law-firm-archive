-- 已有环境增量：镜像升级检测相关 sys_config（与全新安装 02-schema-consolidated 保持一致）
INSERT INTO sys_config (config_key, config_value, config_type, config_group, description, editable, sort_order) VALUES
('system.upgrade.registry_base_url', 'https://hub.albertzhan.top', 'STRING', 'SYSTEM', '私有镜像仓库根地址（用于检测更新）', true, 80),
('system.upgrade.backend_repository', 'law-firm-archive/backend', 'STRING', 'SYSTEM', '后端镜像仓库路径（不含主机名）', true, 81),
('system.upgrade.frontend_repository', 'law-firm-archive/frontend', 'STRING', 'SYSTEM', '前端镜像仓库路径（不含主机名）', true, 82),
('system.upgrade.registry_username', '', 'STRING', 'SYSTEM', '仓库只读账号（可选，也可使用环境变量 REGISTRY_USERNAME）', true, 83),
('system.upgrade.registry_password', '', 'STRING', 'SYSTEM', '仓库只读密码（可选，也可使用环境变量 REGISTRY_PASSWORD）', true, 84)
ON CONFLICT (config_key) DO NOTHING;
