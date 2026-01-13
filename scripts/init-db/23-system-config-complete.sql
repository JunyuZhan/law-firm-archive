-- =====================================================
-- 系统配置完整初始化脚本
-- =====================================================
-- 说明：补充完整的系统配置项
-- 使用方法：执行此脚本添加缺失的配置项
-- =====================================================

-- 插入系统基础配置（如果不存在）
INSERT INTO public.sys_config (config_key, config_value, config_name, config_type, description, is_system, created_at, updated_at, deleted)
VALUES 
-- 系统基础信息
('sys.name', '智慧律所管理系统', '系统名称', 'STRING', '系统显示名称，用于页面标题等', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.version', '1.0.0', '系统版本', 'STRING', '当前系统版本号', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.copyright', '© 2026 智慧律所', '版权信息', 'STRING', '页面底部版权信息', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 登录安全配置
('sys.login.captcha', 'true', '登录验证码', 'BOOLEAN', '是否启用登录验证码', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.password.minLength', '8', '密码最小长度', 'NUMBER', '用户密码最小长度要求', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.password.complexity', 'true', '密码复杂度要求', 'BOOLEAN', '是否要求密码包含大小写字母、数字和特殊字符', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.session.timeout', '30', '会话超时时间', 'NUMBER', '会话超时时间(分钟)，超过此时间未操作需重新登录', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.login.maxAttempts', '5', '最大登录尝试次数', 'NUMBER', '连续登录失败超过此次数将锁定账户', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.login.lockDuration', '30', '账户锁定时长', 'NUMBER', '账户锁定后解锁时间(分钟)', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 文件上传配置
('sys.upload.maxSize', '100', '上传文件大小限制', 'NUMBER', '单个文件上传大小限制(MB)', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.upload.allowTypes', 'doc,docx,pdf,xls,xlsx,jpg,jpeg,png,gif,zip,rar', '允许上传类型', 'STRING', '允许上传的文件类型，多个用逗号分隔', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.upload.path', '/uploads', '文件上传路径', 'STRING', '文件上传存储路径', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 律所基本信息（已有，但补充完整）
('firm.name', '', '律所名称', 'STRING', '律师事务所全称，用于合同、函件等文档', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.address', '', '律所地址', 'STRING', '律师事务所详细地址', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.phone', '', '联系电话', 'STRING', '律所联系电话', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.email', '', '电子邮箱', 'STRING', '律所电子邮箱', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.license', '', '执业许可证号', 'STRING', '律所执业许可证号', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.legal.rep', '', '法定代表人', 'STRING', '律所法定代表人姓名', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.fax', '', '传真号码', 'STRING', '律所传真号码', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.website', '', '官方网站', 'STRING', '律所官方网站地址', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('firm.postcode', '', '邮政编码', 'STRING', '律所所在地邮政编码', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 工作时间配置
('work.startTime', '09:00', '上班时间', 'STRING', '标准上班时间，格式：HH:mm', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('work.endTime', '18:00', '下班时间', 'STRING', '标准下班时间，格式：HH:mm', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('work.workdays', '1,2,3,4,5', '工作日', 'STRING', '工作日设置，1-7表示周一到周日，多个用逗号分隔', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 审批流程配置
('approval.autoApprove', 'false', '自动审批', 'BOOLEAN', '是否启用自动审批（仅用于测试环境）', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('approval.reminder.enabled', 'true', '审批提醒', 'BOOLEAN', '是否启用审批提醒通知', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('approval.reminder.interval', '24', '提醒间隔', 'NUMBER', '审批提醒间隔时间(小时)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('approval.timeout.days', '7', '审批超时天数', 'NUMBER', '审批超过此天数未处理将自动提醒', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 通知配置
('notification.email.enabled', 'false', '邮件通知', 'BOOLEAN', '是否启用邮件通知', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.email.smtp.host', '', 'SMTP服务器', 'STRING', '邮件服务器地址', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.email.smtp.port', '25', 'SMTP端口', 'NUMBER', '邮件服务器端口', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.email.smtp.username', '', 'SMTP用户名', 'STRING', '邮件服务器用户名', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.email.smtp.password', '', 'SMTP密码', 'STRING', '邮件服务器密码', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.sms.enabled', 'false', '短信通知', 'BOOLEAN', '是否启用短信通知', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.sms.provider', '', '短信服务商', 'STRING', '短信服务提供商', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 财务配置
('finance.invoice.prefix', 'FP', '发票编号前缀', 'STRING', '发票编号前缀', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('finance.invoice.taxRate', '6', '默认税率', 'NUMBER', '默认税率(%)，用于发票计算', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('finance.payment.reminder.days', '7', '付款提醒天数', 'NUMBER', '合同到期前多少天提醒付款', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('finance.commission.default', '30,50,15,5', '默认提成比例', 'STRING', '默认提成比例：律所,主办,协办,辅助，用逗号分隔', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 文档配置
('document.template.path', '/templates', '模板存储路径', 'STRING', '文档模板存储路径', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('document.autoSave', 'true', '自动保存', 'BOOLEAN', '是否启用文档自动保存', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('document.autoSave.interval', '300', '自动保存间隔', 'NUMBER', '自动保存间隔时间(秒)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('document.version.keep', '10', '版本保留数量', 'NUMBER', '文档版本历史保留数量', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 档案配置
('archive.retention.years', '10', '档案保留年限', 'NUMBER', '档案保留年限(年)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('archive.autoArchive', 'false', '自动归档', 'BOOLEAN', '项目结案后是否自动归档', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('archive.cover.autoGenerate', 'true', '自动生成封面', 'BOOLEAN', '归档时是否自动生成卷宗封面', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 打印配置
('print.defaultFont', 'SimSun', '默认字体', 'STRING', '打印文档默认字体', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('print.defaultFontSize', '14', '默认字号', 'NUMBER', '打印文档默认字号(磅)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('print.margin.top', '3.7', '上边距', 'NUMBER', '打印文档上边距(厘米)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('print.margin.bottom', '3.5', '下边距', 'NUMBER', '打印文档下边距(厘米)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('print.margin.left', '2.8', '左边距', 'NUMBER', '打印文档左边距(厘米)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('print.margin.right', '2.6', '右边距', 'NUMBER', '打印文档右边距(厘米)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 数据备份配置
('backup.enabled', 'true', '启用备份', 'BOOLEAN', '是否启用数据自动备份', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('backup.interval', '24', '备份间隔', 'NUMBER', '数据备份间隔时间(小时)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('backup.keep.days', '30', '备份保留天数', 'NUMBER', '备份文件保留天数', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('backup.path', '/backups', '备份路径', 'STRING', '数据备份存储路径', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 日志配置
('log.level', 'INFO', '日志级别', 'STRING', '系统日志级别：DEBUG/INFO/WARN/ERROR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('log.keep.days', '90', '日志保留天数', 'NUMBER', '系统日志保留天数', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('log.operation.enabled', 'true', '操作日志', 'BOOLEAN', '是否启用操作日志记录', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
-- 注意：
-- 1. 律所名称统一使用 firm.name 配置，不再单独配置 system.firm.name
-- 2. letter.verify.public.url 已弃用并移除，函件验证功能通过独立客服系统实现
ON CONFLICT (config_key) DO NOTHING;

