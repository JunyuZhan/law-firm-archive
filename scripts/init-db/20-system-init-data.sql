-- =====================================================
-- 律师事务所管理系统 - 系统初始化数据
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 菜单、角色、权限、字典、配置、AI模型等系统初始数据
-- =====================================================

--
-- Data for Name: sys_config; Type: TABLE DATA; Schema: public; Owner: -
--

-- 系统配置初始化（使用 ON CONFLICT 避免重复插入）
INSERT INTO public.sys_config (config_key, config_value, config_name, config_type, description, is_system, created_at, updated_at, deleted)
VALUES 
-- 系统基础信息
('sys.name', '智慧律所管理系统', '系统名称', 'STRING', '系统显示名称，用于页面标题等', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.version', '1.0.0', '系统版本', 'STRING', '当前系统版本号', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.copyright', '© 2026 智慧律所', '版权信息', 'STRING', '页面底部版权信息', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 系统维护模式配置
('sys.maintenance.enabled', 'false', '维护模式开关', 'BOOLEAN', '是否启用系统维护模式，启用后将阻止非管理员用户访问', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('sys.maintenance.message', '系统正在维护中，预计维护时间：30分钟，请稍后再试', '维护提示信息', 'STRING', '维护模式下显示给用户的提示信息', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

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

-- 合同编号配置
('contract.number.prefix', 'HT', '合同编号前缀', 'STRING', '合同编号的前缀，用于 {PREFIX} 变量', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('contract.number.pattern', '{YEAR}{CASE_TYPE}代字第{SEQUENCE_YEAR}号', '合同编号规则', 'STRING', '合同编号生成规则，支持多种变量组合。示例：2026民代字第0001号', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('contract.number.sequence.length', '4', '序号长度', 'STRING', '序号部分的长度(1-10)，不足位数前面补0', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 律所基本信息
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

-- 邮件通知配置
('notification.email.enabled', 'false', '邮件通知开关', 'BOOLEAN', '是否启用邮件通知功能', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.email.smtp.host', '', 'SMTP服务器', 'STRING', '邮件服务器地址，如 smtp.qq.com、smtp.163.com', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.email.smtp.port', '465', 'SMTP端口', 'NUMBER', '邮件服务器端口，SSL用465，TLS用587', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.email.smtp.username', '', 'SMTP用户名', 'STRING', '发件人邮箱地址', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.email.smtp.password', '', 'SMTP密码', 'STRING', '邮箱授权码（非登录密码）', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.email.admin.recipients', '', '告警接收邮箱', 'STRING', '接收系统告警的管理员邮箱，多个用逗号分隔', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 告警通知配置
('notification.alert.login.failure', 'true', '登录失败告警', 'BOOLEAN', '登录失败次数过多时发送告警', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.alert.account.locked', 'true', '账户锁定告警', 'BOOLEAN', '账户被锁定时发送告警', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.alert.system.error', 'true', '系统错误告警', 'BOOLEAN', '系统发生错误时发送告警', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.alert.disk.space', 'true', '磁盘空间告警', 'BOOLEAN', '磁盘空间不足时发送告警', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.alert.backup.failure', 'true', '备份失败告警', 'BOOLEAN', '备份失败时发送告警', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 定时报告配置
('notification.report.daily.enabled', 'false', '每日报告', 'BOOLEAN', '是否启用每日系统运行报告（每天早上8点）', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('notification.report.weekly.enabled', 'false', '每周报告', 'BOOLEAN', '是否启用每周系统运行报告（每周一早上9点）', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

-- 短信通知配置（预留）
('notification.sms.enabled', 'false', '短信通知', 'BOOLEAN', '是否启用短信通知（暂未实现）', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
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
ON CONFLICT (config_key) DO NOTHING;
--
-- Data for Name: sys_dict_item; Type: TABLE DATA; Schema: public; Owner: -
--

--
-- Data for Name: sys_dict_type; Type: TABLE DATA; Schema: public; Owner: -
--

--
-- Data for Name: sys_external_integration; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_external_integration VALUES (1, 'ARCHIVE_CITY', '市档案馆', 'ARCHIVE', '对接市档案馆档案管理系统，用于档案迁移', NULL, NULL, NULL, 'API_KEY', '{"timeout": 30000, "retryCount": 3}', false, NULL, NULL, NULL, '2026-01-08 04:48:25.438772', '2026-01-08 04:48:25.438772', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (2, 'ARCHIVE_DISTRICT', '区档案馆', 'ARCHIVE', '对接区档案馆档案管理系统，用于档案迁移', NULL, NULL, NULL, 'API_KEY', '{"timeout": 30000, "retryCount": 3}', false, NULL, NULL, NULL, '2026-01-08 04:48:25.438772', '2026-01-08 04:48:25.438772', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (3, 'ARCHIVE_THIRD_PARTY', '第三方档案系统', 'ARCHIVE', '对接第三方档案管理系统', NULL, NULL, NULL, 'API_KEY', '{"timeout": 30000, "retryCount": 3}', false, NULL, NULL, NULL, '2026-01-08 04:48:25.438772', '2026-01-08 04:48:25.438772', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (4, 'AI_OPENAI', 'OpenAI (GPT)', 'AI', 'OpenAI GPT系列大模型，支持GPT-4、GPT-3.5等', NULL, NULL, NULL, 'BEARER_TOKEN', '{"model": "gpt-4", "timeout": 60000, "maxTokens": 4096, "temperature": 0.7}', false, NULL, NULL, NULL, '2026-01-08 04:48:25.438772', '2026-01-08 04:48:25.438772', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (5, 'AI_CLAUDE', 'Anthropic (Claude)', 'AI', 'Anthropic Claude系列大模型', NULL, NULL, NULL, 'BEARER_TOKEN', '{"model": "claude-3-opus-20240229", "timeout": 60000, "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 04:48:25.438772', '2026-01-08 04:48:25.438772', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (6, 'AI_QWEN', '通义千问', 'AI', '阿里云通义千问大模型', NULL, NULL, NULL, 'API_KEY', '{"model": "qwen-max", "timeout": 60000, "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 04:48:25.438772', '2026-01-08 04:48:25.438772', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (7, 'AI_WENXIN', '文心一言', 'AI', '百度文心一言大模型', NULL, NULL, NULL, 'API_KEY', '{"model": "ernie-bot-4", "timeout": 60000, "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 04:48:25.438772', '2026-01-08 04:48:25.438772', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (8, 'AI_ZHIPU', '智谱清言 (GLM)', 'AI', '智谱AI GLM系列大模型', NULL, NULL, NULL, 'API_KEY', '{"model": "glm-4", "timeout": 60000, "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 04:48:25.438772', '2026-01-08 04:48:25.438772', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (10, 'AI_DEEPSEEK_R1', 'DeepSeek R1 (推理)', 'AI', 'DeepSeek R1 推理模型，推理能力强，适合复杂法律文书生成', 'https://api.deepseek.com/v1', 'sk-0e6d99fd0d7a4eee9a5840cdf25d37eb', NULL, 'API_KEY', '{"model": "deepseek-reasoner", "maxTokens": 8192}', true, '2026-01-08 18:06:47.428094', 'SUCCESS', '连接成功，响应码: 401', '2026-01-08 10:06:08.771521', '2026-01-08 18:06:47.428094', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (9, 'AI_DEEPSEEK', 'DeepSeek Chat (对话)', 'AI', 'DeepSeek大模型，支持代码生成等', 'https://api.deepseek.com/v1', 'sk-0e6d99fd0d7a4eee9a5840cdf25d37eb', NULL, 'API_KEY', '{"model": "deepseek-chat", "timeout": 60000, "maxTokens": 4096}', true, '2026-01-08 16:34:30.272092', 'SUCCESS', '连接成功，响应码: 401', '2026-01-08 04:48:25.438772', '2026-01-08 18:08:46.536256', NULL, 1, false);
INSERT INTO public.sys_external_integration VALUES (11, 'AI_MOONSHOT', 'Moonshot (Kimi)', 'AI', '月之暗面 Kimi 大模型，支持超长上下文', 'https://api.moonshot.cn/v1', NULL, NULL, 'API_KEY', '{"model": "moonshot-v1-8k", "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.80867', '2026-01-08 10:06:08.80867', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (12, 'AI_YI', '零一万物 Yi', 'AI', '零一万物 Yi 大模型', 'https://api.lingyiwanwu.com/v1', NULL, NULL, 'API_KEY', '{"model": "yi-34b-chat-0205", "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.813892', '2026-01-08 10:06:08.813892', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (13, 'AI_MINIMAX', 'MiniMax', 'AI', 'MiniMax 大模型', 'https://api.minimax.chat/v1', NULL, NULL, 'API_KEY', '{"model": "abab5.5-chat", "groupId": ""}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.814523', '2026-01-08 10:06:08.814523', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (14, 'AI_DIFY', 'Dify', 'AI', 'Dify 开源 LLM 应用开发平台', 'http://localhost:3000/v1', NULL, NULL, 'API_KEY', '{"user": "law-firm-user", "apiType": "chat", "responseMode": "blocking"}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.815244', '2026-01-08 10:06:08.815244', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (15, 'AI_OLLAMA', 'Ollama', 'AI', 'Ollama 本地大模型，支持 Llama、Mistral、Qwen 等', 'http://localhost:11434', NULL, NULL, 'API_KEY', '{"model": "llama2", "apiType": "chat"}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.817134', '2026-01-08 10:06:08.817134', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (16, 'AI_LOCALAI', 'LocalAI', 'AI', 'LocalAI - OpenAI 兼容的本地 AI 后端', 'http://localhost:8080/v1', NULL, NULL, 'API_KEY', '{"model": "gpt-3.5-turbo", "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.818471', '2026-01-08 10:06:08.818471', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (17, 'AI_VLLM', 'vLLM', 'AI', 'vLLM 高性能 LLM 推理框架', 'http://localhost:8000/v1', NULL, NULL, 'API_KEY', '{"model": "meta-llama/Llama-2-7b-chat-hf", "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.819038', '2026-01-08 10:06:08.819038', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (18, 'AI_XINFERENCE', 'Xinference', 'AI', 'Xinference 开源分布式推理框架', 'http://localhost:9997/v1', NULL, NULL, 'API_KEY', '{"model": "qwen-chat", "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.81993', '2026-01-08 10:06:08.81993', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (19, 'AI_ONEAPI', 'OneAPI', 'AI', 'OneAPI 多模型代理服务', 'http://localhost:3000/v1', NULL, NULL, 'API_KEY', '{"model": "gpt-3.5-turbo", "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.820636', '2026-01-08 10:06:08.820636', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (20, 'AI_OPENAI_COMPATIBLE', 'OpenAI 兼容 API', 'AI', '通用 OpenAI 兼容接口', 'http://localhost:8080/v1', NULL, NULL, 'API_KEY', '{"model": "default", "maxTokens": 4096}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.8224', '2026-01-08 10:06:08.8224', NULL, NULL, false);
INSERT INTO public.sys_external_integration VALUES (21, 'AI_CUSTOM', '自定义 API', 'AI', '自定义 API 接口，支持自定义请求格式', 'http://localhost:8080', NULL, NULL, 'API_KEY', '{"responsePath": "choices.0.message.content", "requestTemplate": "openai"}', false, NULL, NULL, NULL, '2026-01-08 10:06:08.823428', '2026-01-08 10:06:08.823428', NULL, NULL, false);
--
-- Data for Name: sys_menu; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_menu VALUES (31, 3, '客户列表', '/crm/client', 'crm/client/index', NULL, 'ContactsOutlined', 'MENU', 'client:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896673', '2026-01-04 16:14:15.896673', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (32, 3, '利冲审查', '/crm/conflict', 'crm/conflict/index', NULL, 'SecurityScanOutlined', 'MENU', 'conflict:apply', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896673', '2026-01-04 16:14:15.896673', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (33, 3, '案源管理', '/crm/lead', 'crm/lead/index', NULL, 'FunnelPlotOutlined', 'MENU', 'lead:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896673', '2026-01-04 16:14:15.896673', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (81, 8, '档案列表', '/archive/list', 'archive/list/index', NULL, 'FolderOpenOutlined', 'MENU', 'archive:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89755', '2026-01-04 16:14:15.89755', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (82, 8, '档案借阅', '/archive/borrow', 'archive/borrow/index', NULL, 'ExportOutlined', 'MENU', 'archive:borrow', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89755', '2026-01-04 16:14:15.89755', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (174, 93, '会议室管理', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:meeting:manage', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.943026', '2026-01-06 04:13:09.943026', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (175, 97, '加班查看', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:overtime:view', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.94419', '2026-01-06 04:13:09.94419', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (83, 8, '档案迁移', '/archive/destroy', 'archive/destroy/index', NULL, 'DeleteOutlined', 'MENU', 'archive:migrate:apply', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89755', '2026-01-08 03:06:03.996592', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (4, 0, '项目管理', '/matter', 'LAYOUT', NULL, 'FolderOutlined', 'DIRECTORY', NULL, 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (3, 0, '客户管理', '/crm', 'LAYOUT', NULL, 'TeamOutlined', 'DIRECTORY', NULL, 4, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (5, 0, '财务管理', '/finance', 'LAYOUT', NULL, 'MoneyCollectOutlined', 'DIRECTORY', NULL, 6, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (6, 0, '卷宗管理', '/document', 'LAYOUT', NULL, 'FileTextOutlined', 'DIRECTORY', NULL, 7, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (8, 0, '档案管理', '/archive', 'LAYOUT', NULL, 'DatabaseOutlined', 'DIRECTORY', NULL, 8, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (9, 0, '行政管理', '/admin', 'LAYOUT', NULL, 'BankOutlined', 'DIRECTORY', NULL, 9, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (10, 0, '人力资源', '/hr', 'LAYOUT', NULL, 'IdcardOutlined', 'DIRECTORY', NULL, 10, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (111, 11, '知识文章', '/knowledge/article', 'knowledge/article/index', NULL, 'FileTextOutlined', 'MENU', 'knowledge:article:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898559', '2026-01-04 16:14:15.898559', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (112, 11, '案例库', '/knowledge/case', 'knowledge/case/index', NULL, 'ContainerOutlined', 'MENU', 'knowledge:case:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898559', '2026-01-04 16:14:15.898559', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (113, 11, '法规库', '/knowledge/law', 'knowledge/law/index', NULL, 'BookOutlined', 'MENU', 'knowledge:law:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898559', '2026-01-04 16:14:15.898559', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (130, 1, '审批列表', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:list', 1, true, 'ENABLED', false, true, '2026-01-05 10:00:00', '2026-01-05 10:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (131, 1, '审批操作', NULL, NULL, NULL, NULL, 'BUTTON', 'approval:approve', 2, true, 'ENABLED', false, true, '2026-01-05 10:00:00', '2026-01-05 10:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (132, 45, '合同审批', NULL, NULL, NULL, NULL, 'BUTTON', 'contract:approve', 1, true, 'ENABLED', false, true, '2026-01-05 10:00:00', '2026-01-05 10:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (124, 123, '同步变更', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:contract:amendment:sync', 1, true, 'ENABLED', false, true, '2026-01-05 03:55:33.410601', '2026-01-05 03:55:33.410601', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (125, 123, '忽略变更', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:contract:amendment:ignore', 2, true, 'ENABLED', false, true, '2026-01-05 03:55:33.410601', '2026-01-05 03:55:33.410601', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (145, 53, '查看提成', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:commission:view', 5, true, 'ENABLED', false, true, '2026-01-05 18:00:00', '2026-01-05 18:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (146, 53, '提成审批', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:commission:approve', 6, true, 'ENABLED', false, true, '2026-01-05 18:00:00', '2026-01-05 18:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (147, 53, '提成发放', NULL, NULL, NULL, NULL, 'BUTTON', 'finance:commission:issue', 7, true, 'ENABLED', false, true, '2026-01-05 18:00:00', '2026-01-05 18:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (150, 149, '日程查看', NULL, NULL, NULL, NULL, 'BUTTON', 'schedule:view', 1, true, 'ENABLED', false, true, '2026-01-06 04:02:22.668354', '2026-01-06 04:02:22.668354', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (151, 149, '日程管理', NULL, NULL, NULL, NULL, 'BUTTON', 'schedule:manage', 2, true, 'ENABLED', false, true, '2026-01-06 04:02:22.668804', '2026-01-06 04:02:22.668804', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (161, 44, '任务列表', NULL, NULL, NULL, NULL, 'BUTTON', 'task:list', 1, true, 'ENABLED', false, true, '2026-01-06 04:02:27.64771', '2026-01-06 04:02:27.64771', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (162, 44, '任务查看', NULL, NULL, NULL, NULL, 'BUTTON', 'task:view', 2, true, 'ENABLED', false, true, '2026-01-06 04:02:27.649123', '2026-01-06 04:02:27.649123', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (163, 43, '工时列表', NULL, NULL, NULL, NULL, 'BUTTON', 'timesheet:list', 1, true, 'ENABLED', false, true, '2026-01-06 04:02:27.649535', '2026-01-06 04:02:27.649535', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (164, 43, '工时查看', NULL, NULL, NULL, NULL, 'BUTTON', 'timesheet:view', 3, true, 'ENABLED', false, true, '2026-01-06 04:02:27.649849', '2026-01-06 04:02:27.649849', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (165, 43, '工时审批', NULL, NULL, NULL, NULL, 'BUTTON', 'timesheet:approve', 4, true, 'ENABLED', false, true, '2026-01-06 04:02:27.650148', '2026-01-06 04:02:27.650148', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (168, 99, '外出查看', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:goout:view', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.939964', '2026-01-06 04:13:09.939964', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (169, 99, '外出登记', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:goout:register', 2, true, 'ENABLED', false, false, '2026-01-06 04:13:09.939964', '2026-01-06 04:13:09.939964', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (170, 100, '会议记录查看', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:meeting:view', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.942358', '2026-01-06 04:13:09.942358', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (171, 100, '会议记录管理', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:meeting:record', 2, true, 'ENABLED', false, false, '2026-01-06 04:13:09.942358', '2026-01-06 04:13:09.942358', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (172, 100, '会议通知', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:meeting:notice', 3, true, 'ENABLED', false, false, '2026-01-06 04:13:09.942358', '2026-01-06 04:13:09.942358', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (173, 98, '资产盘点操作', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:inventory', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.942766', '2026-01-06 04:13:09.942766', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (1, 0, '工作台', '/dashboard/workspace', 'dashboard/workspace/index', NULL, 'DashboardOutlined', 'MENU', NULL, 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (149, 0, '日程管理', '/workbench/schedule', 'workbench/schedule/index', NULL, 'CalendarOutlined', 'MENU', 'schedule:list', 2, true, 'ENABLED', false, true, '2026-01-06 04:02:22.660554', '2026-01-06 04:02:22.660554', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (701, 0, '审批中心', '/dashboard/approval', 'dashboard/approval/index', NULL, 'ant-design:audit-outlined', 'MENU', 'approval:list', 3, true, 'ENABLED', false, true, '2026-01-05 15:01:30.641063', '2026-01-05 15:01:30.641063', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (148, 4, '我的合同', '/matter/my-contract', 'matter/my-contract/index', NULL, 'FileTextOutlined', 'MENU', 'matter:contract:view', 4, true, 'ENABLED', false, true, '2026-01-06 01:27:18.190968', '2026-01-06 01:27:18.190968', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (176, 97, '加班申请', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:overtime:apply', 2, true, 'ENABLED', false, false, '2026-01-06 04:13:09.94419', '2026-01-06 04:13:09.94419', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (177, 97, '加班审批', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:overtime:approve', 3, true, 'ENABLED', false, false, '2026-01-06 04:13:09.94419', '2026-01-06 04:13:09.94419', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (178, 92, '请假审批', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:leave:approve', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.944811', '2026-01-06 04:13:09.944811', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (179, 92, '假期管理', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:leave:manage', 2, true, 'ENABLED', false, false, '2026-01-06 04:13:09.944811', '2026-01-06 04:13:09.944811', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (180, 94, '资产详情', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:detail', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945093', '2026-01-06 04:13:09.945093', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (181, 94, '资产创建', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:create', 2, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945093', '2026-01-06 04:13:09.945093', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (182, 94, '资产编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:edit', 3, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945093', '2026-01-06 04:13:09.945093', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (183, 94, '资产删除', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:delete', 4, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945093', '2026-01-06 04:13:09.945093', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (184, 94, '资产领用', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:receive', 5, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945093', '2026-01-06 04:13:09.945093', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (185, 94, '资产归还', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:return', 6, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945093', '2026-01-06 04:13:09.945093', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (186, 94, '资产报废', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:asset:scrap', 7, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945093', '2026-01-06 04:13:09.945093', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (187, 95, '采购详情', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:detail', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945586', '2026-01-06 04:13:09.945586', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (188, 95, '采购创建', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:create', 2, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945586', '2026-01-06 04:13:09.945586', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (189, 95, '采购编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:edit', 3, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945586', '2026-01-06 04:13:09.945586', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (190, 95, '采购审批', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:approve', 4, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945586', '2026-01-06 04:13:09.945586', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (191, 95, '采购入库', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:purchase:receive', 5, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945586', '2026-01-06 04:13:09.945586', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (192, 96, '供应商详情', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:supplier:detail', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945853', '2026-01-06 04:13:09.945853', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (193, 96, '供应商创建', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:supplier:create', 2, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945853', '2026-01-06 04:13:09.945853', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (194, 96, '供应商编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:supplier:edit', 3, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945853', '2026-01-06 04:13:09.945853', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (195, 96, '供应商删除', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:supplier:delete', 4, true, 'ENABLED', false, false, '2026-01-06 04:13:09.945853', '2026-01-06 04:13:09.945853', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (196, 121, '合同导出', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:contract:export', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.94617', '2026-01-06 04:13:09.94617', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (197, 91, '考勤记录', NULL, NULL, NULL, NULL, 'BUTTON', 'admin:attendance:record', 1, true, 'ENABLED', false, false, '2026-01-06 04:13:09.946385', '2026-01-06 04:13:09.946385', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (201, 200, '报表查看', NULL, NULL, NULL, NULL, 'BUTTON', 'report:list', 1, true, 'ENABLED', false, true, '2026-01-06 14:00:00', '2026-01-06 14:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (202, 200, '报表详情', NULL, NULL, NULL, NULL, 'BUTTON', 'report:detail', 2, true, 'ENABLED', false, true, '2026-01-06 14:00:00', '2026-01-06 14:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (203, 200, '生成报表', NULL, NULL, NULL, NULL, 'BUTTON', 'report:generate', 3, true, 'ENABLED', false, true, '2026-01-06 14:00:00', '2026-01-06 14:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (204, 200, '下载报表', NULL, NULL, NULL, NULL, 'BUTTON', 'report:download', 4, true, 'ENABLED', false, true, '2026-01-06 14:00:00', '2026-01-06 14:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (205, 200, '删除报表', NULL, NULL, NULL, NULL, 'BUTTON', 'report:delete', 5, true, 'ENABLED', false, true, '2026-01-06 14:00:00', '2026-01-06 14:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (210, 107, '创建工资表', NULL, NULL, NULL, NULL, 'BUTTON', 'payroll:create', 1, true, 'ENABLED', false, true, '2026-01-06 07:38:37.94416', '2026-01-06 07:38:37.94416', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (211, 107, '查看工资表', NULL, NULL, NULL, NULL, 'BUTTON', 'payroll:view', 2, true, 'ENABLED', false, true, '2026-01-06 07:38:37.94416', '2026-01-06 07:38:37.94416', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (212, 107, '编辑工资表', NULL, NULL, NULL, NULL, 'BUTTON', 'payroll:edit', 3, true, 'ENABLED', false, true, '2026-01-06 07:38:37.94416', '2026-01-06 07:38:37.94416', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (213, 107, '提交工资表', NULL, NULL, NULL, NULL, 'BUTTON', 'payroll:submit', 4, true, 'ENABLED', false, true, '2026-01-06 07:38:37.94416', '2026-01-06 07:38:37.94416', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (214, 107, '财务确认', NULL, NULL, NULL, NULL, 'BUTTON', 'payroll:finance:confirm', 5, true, 'ENABLED', false, true, '2026-01-06 07:38:37.94416', '2026-01-06 07:38:37.94416', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (215, 107, '发放工资', NULL, NULL, NULL, NULL, 'BUTTON', 'payroll:issue', 6, true, 'ENABLED', false, true, '2026-01-06 07:38:37.94416', '2026-01-06 07:38:37.94416', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (216, 108, '确认工资', NULL, NULL, NULL, NULL, 'BUTTON', 'payroll:confirm', 1, true, 'ENABLED', false, true, '2026-01-06 07:38:37.94416', '2026-01-06 07:38:37.94416', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (152, 2, '期限列表', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:list', 11, true, 'ENABLED', false, true, '2026-01-06 04:02:22.669342', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (153, 2, '期限查看', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:view', 12, true, 'ENABLED', false, true, '2026-01-06 04:02:22.669621', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (154, 2, '期限创建', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:create', 13, true, 'ENABLED', false, true, '2026-01-06 04:02:22.669923', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (155, 2, '期限编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:edit', 14, true, 'ENABLED', false, true, '2026-01-06 04:02:22.670256', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (200, 0, '报表中心', '/workbench/report', 'workbench/report/index', NULL, 'BarChartOutlined', 'MENU', 'report:list', 12, true, 'ENABLED', false, true, '2026-01-06 14:00:00', '2026-01-06 14:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (23, 2, '部门管理', '/system/dept', 'system/dept/index', NULL, 'ApartmentOutlined', 'MENU', 'sys:dept:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (24, 2, '菜单管理', '/system/menu', 'system/menu/index', NULL, 'MenuOutlined', 'MENU', 'sys:menu:list', 4, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (30, 2, '字典管理', '/system/dict', 'system/dict/index', NULL, 'BookOutlined', 'MENU', 'sys:dict:list', 5, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (198, 2, '权限矩阵', '/system/permission-matrix', 'system/permission-matrix/index', NULL, 'TableOutlined', 'MENU', 'sys:role:list', 6, true, 'ENABLED', false, true, '2026-01-06 11:40:43.855721', '2026-01-06 11:40:43.855721', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (156, 2, '期限删除', NULL, NULL, NULL, NULL, 'BUTTON', 'deadline:delete', 15, true, 'ENABLED', false, true, '2026-01-06 04:02:22.67069', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (703, 207, '查询交接', NULL, NULL, NULL, NULL, 'BUTTON', 'sys:handover:list', 0, true, 'ENABLED', false, true, '2026-01-06 12:36:38.137972', '2026-01-06 12:36:38.137972', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (704, 207, '创建交接', NULL, NULL, NULL, NULL, 'BUTTON', 'sys:handover:create', 1, true, 'ENABLED', false, true, '2026-01-06 12:36:38.137972', '2026-01-06 12:36:38.137972', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (705, 207, '查看交接', NULL, NULL, NULL, NULL, 'BUTTON', 'sys:handover:view', 2, true, 'ENABLED', false, true, '2026-01-06 12:36:38.137972', '2026-01-06 12:36:38.137972', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (706, 207, '确认交接', NULL, NULL, NULL, NULL, 'BUTTON', 'sys:handover:confirm', 3, true, 'ENABLED', false, true, '2026-01-06 12:36:38.137972', '2026-01-06 12:36:38.137972', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (707, 207, '取消交接', NULL, NULL, NULL, NULL, 'BUTTON', 'sys:handover:cancel', 4, true, 'ENABLED', false, true, '2026-01-06 12:36:38.137972', '2026-01-06 12:36:38.137972', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (22, 2, '角色管理', '/system/role', 'system/role/index', NULL, 'SafetyCertificateOutlined', 'MENU', 'sys:role:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (217, 41, '查看项目详情', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:view', 1, true, 'ENABLED', false, false, '2026-01-07 10:15:16.51803', '2026-01-07 10:15:31.760307', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (220, 41, '项目查看', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:view', 1, true, 'ENABLED', false, true, '2026-01-08 00:17:54.280133', '2026-01-08 00:17:54.280133', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (221, 41, '项目创建', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:create', 2, true, 'ENABLED', false, true, '2026-01-08 00:17:54.287088', '2026-01-08 00:17:54.287088', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (222, 41, '项目编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:update', 3, true, 'ENABLED', false, true, '2026-01-08 00:17:54.288127', '2026-01-08 00:17:54.288127', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (223, 41, '申请结案', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:close', 4, true, 'ENABLED', false, true, '2026-01-08 00:17:54.290683', '2026-01-08 00:17:54.290683', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (224, 41, '审批结案', NULL, NULL, NULL, NULL, 'BUTTON', 'matter:approve', 5, true, 'ENABLED', false, true, '2026-01-08 00:17:54.29157', '2026-01-08 00:17:54.29157', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (711, 62, '生成文档', '', '', NULL, '', 'BUTTON', 'doc:template:generate', 4, true, 'ENABLED', false, false, '2026-01-08 01:39:44.902905', '2026-01-08 01:39:44.902905', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (712, 62, '预览模板', '', '', NULL, '', 'BUTTON', 'doc:template:view', 5, true, 'ENABLED', false, false, '2026-01-08 01:39:44.903307', '2026-01-08 01:39:44.903307', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (710, 62, '使用模板', '', '', NULL, '', 'BUTTON', 'doc:template:use', 3, true, 'ENABLED', false, false, '2026-01-08 01:39:44.902589', '2026-01-08 01:39:44.902589', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (708, 713, '查看详情', '', '', NULL, '', 'BUTTON', 'doc:template:detail', 1, true, 'ENABLED', false, false, '2026-01-08 01:39:44.896302', '2026-01-08 01:39:44.896302', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (709, 713, '管理模板', '', '', NULL, '', 'BUTTON', 'doc:template:manage', 2, true, 'ENABLED', false, false, '2026-01-08 01:39:44.902169', '2026-01-08 01:39:44.902169', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (721, 71, '入库审批', NULL, NULL, NULL, NULL, 'BUTTON', 'archive:store:approve', 2, true, 'ENABLED', false, false, '2026-01-08 02:56:33.628793', '2026-01-08 02:56:33.628793', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (722, 71, '申请迁移', NULL, NULL, NULL, NULL, 'BUTTON', 'archive:migrate:apply', 3, true, 'ENABLED', false, false, '2026-01-08 02:56:33.632876', '2026-01-08 02:56:33.632876', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (723, 71, '迁移审批', NULL, NULL, NULL, NULL, 'BUTTON', 'archive:migrate:approve', 4, true, 'ENABLED', false, false, '2026-01-08 02:56:33.633109', '2026-01-08 02:56:33.633109', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (218, 120, '出函审批', '', '', NULL, NULL, 'BUTTON', 'admin:letter:approve', 1, true, 'ENABLED', false, true, '2026-01-08 03:26:38.462763', '2026-01-08 03:26:38.462763', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (219, 120, '出函打印', '', '', NULL, NULL, 'BUTTON', 'admin:letter:print', 2, true, 'ENABLED', false, true, '2026-01-08 03:26:38.464226', '2026-01-08 03:26:38.464226', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (11, 0, '知识库', '/knowledge', 'LAYOUT', NULL, 'BookOutlined', 'DIRECTORY', NULL, 11, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (207, 0, '数据交接', '/data-handover', 'data-handover/index', NULL, 'ant-design:swap-outlined', 'MENU', NULL, 13, true, 'ENABLED', false, true, '2026-01-06 12:36:22.065598', '2026-01-06 12:36:22.065598', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (2, 0, '系统管理', '/system', 'LAYOUT', NULL, 'SettingOutlined', 'DIRECTORY', NULL, 99, true, 'ENABLED', false, true, '2026-01-04 16:14:15.895648', '2026-01-04 16:14:15.895648', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (21, 2, '用户管理', '/system/user', 'system/user/index', NULL, 'UserOutlined', 'MENU', 'sys:user:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-04 16:14:15.896441', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (25, 2, '系统配置', '/system/config', 'system/config/index', NULL, 'ToolOutlined', 'MENU', 'sys:config:list', 7, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (26, 2, '操作日志', '/system/log', 'system/log/index', NULL, 'FileSearchOutlined', 'MENU', 'sys:log:list', 8, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (29, 2, '数据库备份', '/system/backup', 'system/backup/index', NULL, 'DatabaseOutlined', 'MENU', 'system:backup:list', 9, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (27, 2, '出函模板', '/system/letter-template', 'system/letter-template/index', NULL, 'MailOutlined', 'MENU', 'sys:letter-template:list', 10, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (28, 2, '合同模板', '/system/contract-template', 'system/contract-template/index', NULL, 'FileProtectOutlined', 'MENU', 'sys:contract-template:list', 11, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896441', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (140, 2, '提成规则配置', '/system/commission-config', 'system/commission-config/index', NULL, 'ant-design:percentage-outlined', 'MENU', 'system:config:list', 12, true, 'ENABLED', false, true, '2026-01-05 18:00:00', '2026-01-06 11:40:43.86229', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (159, 2, '外部系统集成', '/system/integration', 'system/integration/index', NULL, 'CloudOutlined', 'MENU', 'system:integration:list', 99, true, 'ENABLED', false, true, '2026-01-08 04:48:25.443502', '2026-01-08 04:48:25.443502', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (41, 4, '项目列表', '/matter/list', 'matter/list/index', NULL, 'ProjectOutlined', 'MENU', 'matter:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (42, 4, '我的项目', '/matter/my', 'matter/my/index', NULL, 'SolutionOutlined', 'MENU', 'matter:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (45, 4, '合同管理', '/matter/contract', 'matter/contract/index', NULL, 'FileProtectOutlined', 'MENU', 'matter:contract:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (44, 4, '任务管理', '/matter/task', 'matter/task/index', NULL, 'CheckSquareOutlined', 'MENU', 'task:manage', 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (43, 4, '工时管理', '/matter/timesheet', 'matter/timesheet/index', NULL, 'ClockCircleOutlined', 'MENU', 'timesheet:record', 6, true, 'ENABLED', false, true, '2026-01-04 16:14:15.896878', '2026-01-04 16:14:15.896878', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (57, 5, '我的收款', '/finance/my-payment', 'finance/my-payment/index', NULL, 'WalletOutlined', 'MENU', 'finance:my:payment', 1, true, 'ENABLED', false, true, '2026-01-05 16:00:00', '2026-01-05 16:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (58, 5, '我的提成', '/finance/my-commission', 'finance/my-commission/index', NULL, 'DollarOutlined', 'MENU', 'finance:my:commission', 2, true, 'ENABLED', false, true, '2026-01-05 16:00:00', '2026-01-05 16:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (108, 5, '我的工资', '/hr/payroll/my', 'hr/payroll/my/index', NULL, 'WalletOutlined', 'MENU', 'payroll:my:view', 3, true, 'ENABLED', false, true, '2026-01-06 07:30:04.370342', '2026-01-06 07:30:04.370342', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (59, 5, '费用报销', '/finance/expense', 'finance/expense/index', NULL, 'AccountBookOutlined', 'MENU', 'finance:expense:apply', 4, true, 'ENABLED', false, true, '2026-01-05 16:00:00', '2026-01-05 16:00:00', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (51, 5, '合同收款概览', '/finance/contract', 'finance/contract/index', NULL, 'FileProtectOutlined', 'MENU', 'finance:contract:view', 10, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (52, 5, '收款管理', '/finance/payment', 'finance/payment/index', NULL, 'PayCircleOutlined', 'MENU', 'finance:payment:manage', 11, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (56, 5, '收款变更审批', '/finance/payment-amendment', 'finance/payment-amendment/index', NULL, 'AuditOutlined', 'MENU', 'fee:amendment:list', 12, true, 'ENABLED', false, true, '2026-01-05 02:19:31.876174', '2026-01-05 02:19:31.876174', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (53, 5, '提成管理', '/finance/commission', 'finance/commission/index', NULL, 'PercentageOutlined', 'MENU', 'finance:commission:manage', 13, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (107, 5, '工资管理', '/hr/payroll', 'hr/payroll/index', NULL, 'DollarOutlined', 'MENU', 'payroll:list', 14, true, 'ENABLED', false, true, '2026-01-06 07:30:04.370342', '2026-01-06 07:30:04.370342', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (54, 5, '发票管理', '/finance/invoice', 'finance/invoice/index', NULL, 'ProfileOutlined', 'MENU', 'finance:invoice:manage', 15, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (123, 5, '合同变更处理', '/finance/contract-amendment', '/finance/contract-amendment/index', NULL, 'ant-design:diff-outlined', 'MENU', 'finance:contract:amendment:view', 16, true, 'ENABLED', false, true, '2026-01-05 03:55:33.410601', '2026-01-05 03:55:33.410601', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (55, 5, '财务报表', '/finance/report', 'finance/report/index', NULL, 'BarChartOutlined', 'MENU', 'finance:report:view', 20, true, 'ENABLED', false, true, '2026-01-04 16:14:15.897126', '2026-01-04 16:14:15.897126', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (714, 6, '我的文书', '/document/my', 'document/my/index', NULL, 'FileTextOutlined', 'MENU', 'doc:my:list', 1, true, 'ENABLED', false, true, '2026-01-08 02:15:45.865748', '2026-01-08 02:15:45.865748', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (61, 6, '卷宗列表', '/document/list', 'document/list/index', NULL, 'FileOutlined', 'MENU', 'doc:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89735', '2026-01-08 01:43:37.711362', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (62, 6, '文书制作', '/document/compose', 'document/compose/index', NULL, 'FileAddOutlined', 'MENU', 'doc:compose', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89735', '2026-01-08 01:43:37.700594', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (713, 6, '模板管理', '/document/template', 'document/template/index', NULL, 'SettingOutlined', 'MENU', 'doc:template:manage', 4, true, 'ENABLED', false, true, '2026-01-08 01:43:37.711744', '2026-01-08 01:43:37.711744', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (91, 9, '考勤管理', '/admin/attendance', 'admin/attendance/index', NULL, 'ClockCircleOutlined', 'MENU', 'admin:attendance:list', 1, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (92, 9, '请假管理', '/admin/leave', 'admin/leave/index', NULL, 'CalendarOutlined', 'MENU', 'admin:leave:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (97, 9, '加班管理', '/admin/overtime', 'admin/overtime/index', NULL, 'ClockCircleOutlined', 'MENU', 'admin:overtime:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (99, 9, '外出管理', '/admin/go-out', 'admin/go-out/index', NULL, 'ExportOutlined', 'MENU', 'admin:goout:list', 4, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (93, 9, '会议室预约', '/admin/meeting-room', 'admin/meeting-room/index', NULL, 'ScheduleOutlined', 'MENU', 'admin:meeting:list', 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (100, 9, '会议记录', '/admin/meeting-record', 'admin/meeting-record/index', NULL, 'FileTextOutlined', 'MENU', 'admin:meeting-record:list', 6, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (120, 9, '出函管理', '/admin/letter', 'admin/letter/index', NULL, 'MailOutlined', 'MENU', 'admin:letter:list', 10, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (63, 9, '印章管理', '/document/seal', 'document/seal/index', NULL, 'SafetyOutlined', 'MENU', 'doc:seal:list', 11, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89735', '2026-01-07 08:31:53.378975', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (64, 9, '用印申请', '/document/seal-apply', 'document/seal-apply/index', NULL, 'FormOutlined', 'MENU', 'doc:seal:apply', 12, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89735', '2026-01-07 08:31:53.383059', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (94, 9, '资产管理', '/admin/asset', 'admin/asset/index', NULL, 'ShopOutlined', 'MENU', 'admin:asset:list', 20, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (98, 9, '资产盘点', '/admin/asset-inventory', 'admin/asset-inventory/index', NULL, 'FileSearchOutlined', 'MENU', 'admin:asset-inventory:list', 21, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (95, 9, '采购管理', '/admin/purchase', 'admin/purchase/index', NULL, 'ShoppingCartOutlined', 'MENU', 'admin:purchase:list', 22, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (96, 9, '供应商管理', '/admin/supplier', 'admin/supplier/index', NULL, 'ShopOutlined', 'MENU', 'admin:supplier:list', 23, true, 'ENABLED', false, true, '2026-01-04 16:14:15.89774', '2026-01-04 16:14:15.89774', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (121, 9, '合同查询', '/admin/contract', 'admin/contract/index', NULL, 'FileSearchOutlined', 'MENU', 'admin:contract:list', 30, true, 'ENABLED', false, true, '2026-01-05 02:19:57.79315', '2026-01-05 02:19:57.79315', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (109, 10, '员工档案', '/hr/employee', 'hr/employee/index', NULL, 'UserOutlined', 'MENU', 'hr:employee:list', 1, true, 'ENABLED', false, true, '2026-01-06 07:30:04.370342', '2026-01-06 07:30:04.370342', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (101, 10, '培训管理', '/hr/training', 'hr/training/index', NULL, 'ReadOutlined', 'MENU', 'hr:training:list', 2, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (102, 10, '绩效考核', '/hr/performance', 'hr/performance/index', NULL, 'TrophyOutlined', 'MENU', 'hr:performance:list', 3, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (105, 10, '转正管理', '/hr/regularization', 'hr/regularization/index', NULL, 'CheckSquareOutlined', 'MENU', 'hr:regularization:list', 4, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (103, 10, '晋升管理', '/hr/promotion', 'hr/promotion/index', NULL, 'RiseOutlined', 'MENU', 'hr:promotion:list', 5, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (104, 10, '发展计划', '/hr/development', 'hr/development/index', NULL, 'AimOutlined', 'MENU', 'hr:development:list', 6, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
INSERT INTO public.sys_menu VALUES (106, 10, '离职管理', '/hr/resignation', 'hr/resignation/index', NULL, 'ExportOutlined', 'MENU', 'hr:resignation:list', 7, true, 'ENABLED', false, true, '2026-01-04 16:14:15.898121', '2026-01-04 16:14:15.898121', NULL, NULL, false);
--
-- Data for Name: sys_role; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_role VALUES (2, 'DIRECTOR', '律所主任', '律所管理层，可查看全所数据，审批重要事项', 'ALL', 'ACTIVE', 2, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role VALUES (9, 'TRAINEE', '实习律师', '实习人员，有限权限', 'SELF', 'ACTIVE', 9, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role VALUES (3, 'TEAM_LEADER', '团队负责人', '团队负责人，可查看本团队数据，负责团队业务管理', 'DEPT_AND_CHILD', 'ACTIVE', 3, '2026-01-04 16:14:15.892579', '2026-01-07 13:46:52.130375', NULL, NULL, false);
INSERT INTO public.sys_role VALUES (6, 'LAWYER', '律师', '执业律师，处理案件的主要人员', 'SELF', 'ACTIVE', 6, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role VALUES (5, 'FINANCE', '财务', '财务人员，管理律所财务工作', 'ALL', 'ACTIVE', 5, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role VALUES (1, 'ADMIN', '管理员', '系统最高权限，可管理所有功能和数据', 'ALL', 'ACTIVE', 1, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
INSERT INTO public.sys_role VALUES (8, 'ADMIN_STAFF', '行政', '行政后勤人员，管理行政事务', 'ALL', 'ACTIVE', 8, '2026-01-04 16:14:15.892579', '2026-01-04 16:14:15.892579', NULL, NULL, false);
--
-- Data for Name: sys_role_menu; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_role_menu VALUES (1574, 3, 6, '2026-01-07 13:47:00.144537');
INSERT INTO public.sys_role_menu VALUES (1148, 1, 207, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1149, 2, 207, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1150, 1, 703, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1151, 2, 703, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1152, 1, 704, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1153, 2, 704, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1154, 1, 705, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1155, 2, 705, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1156, 1, 706, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1157, 2, 706, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1158, 1, 707, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1159, 2, 707, '2026-01-06 12:36:46.493148');
INSERT INTO public.sys_role_menu VALUES (1575, 3, 61, '2026-01-07 13:47:00.149894');
INSERT INTO public.sys_role_menu VALUES (1577, 3, 8, '2026-01-07 13:47:00.150649');
INSERT INTO public.sys_role_menu VALUES (1578, 3, 81, '2026-01-07 13:47:00.150943');
INSERT INTO public.sys_role_menu VALUES (1579, 3, 82, '2026-01-07 13:47:00.151697');
INSERT INTO public.sys_role_menu VALUES (1581, 3, 11, '2026-01-07 13:47:00.153751');
INSERT INTO public.sys_role_menu VALUES (1582, 3, 111, '2026-01-07 13:47:00.15413');
INSERT INTO public.sys_role_menu VALUES (1837, 5, 94, '2026-01-07 14:45:54.642647');
INSERT INTO public.sys_role_menu VALUES (1584, 3, 113, '2026-01-07 13:47:00.161577');
INSERT INTO public.sys_role_menu VALUES (1838, 5, 181, '2026-01-07 14:45:54.642647');
INSERT INTO public.sys_role_menu VALUES (1585, 3, 200, '2026-01-07 13:47:00.162324');
INSERT INTO public.sys_role_menu VALUES (1586, 3, 201, '2026-01-07 13:47:00.162978');
INSERT INTO public.sys_role_menu VALUES (1235, 6, 9, '2026-01-07 08:37:13.091367');
INSERT INTO public.sys_role_menu VALUES (1839, 5, 182, '2026-01-07 14:45:54.642647');
INSERT INTO public.sys_role_menu VALUES (1237, 9, 9, '2026-01-07 08:37:13.091367');
INSERT INTO public.sys_role_menu VALUES (1239, 2, 217, '2026-01-07 10:15:31.763673');
INSERT INTO public.sys_role_menu VALUES (1840, 5, 183, '2026-01-07 14:45:54.642647');
INSERT INTO public.sys_role_menu VALUES (1587, 3, 202, '2026-01-07 13:47:00.163398');
INSERT INTO public.sys_role_menu VALUES (1242, 6, 217, '2026-01-07 10:15:31.763673');
INSERT INTO public.sys_role_menu VALUES (1841, 5, 186, '2026-01-07 14:45:54.642647');
INSERT INTO public.sys_role_menu VALUES (1588, 3, 203, '2026-01-07 13:47:00.163829');
INSERT INTO public.sys_role_menu VALUES (1307, 8, 98, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1308, 8, 99, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1309, 8, 11, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1310, 8, 100, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1311, 8, 120, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1312, 8, 121, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1313, 8, 63, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1314, 8, 108, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1315, 8, 64, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1316, 8, 207, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1317, 8, 109, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1589, 3, 204, '2026-01-07 13:47:00.164299');
INSERT INTO public.sys_role_menu VALUES (1590, 3, 205, '2026-01-07 13:47:00.165228');
INSERT INTO public.sys_role_menu VALUES (1864, 8, 28, '2026-01-07 15:33:55.538518');
INSERT INTO public.sys_role_menu VALUES (1865, 5, 2, '2026-01-07 15:33:55.539569');
INSERT INTO public.sys_role_menu VALUES (1866, 5, 28, '2026-01-07 15:33:55.539569');
INSERT INTO public.sys_role_menu VALUES (1867, 5, 140, '2026-01-07 15:33:55.539569');
INSERT INTO public.sys_role_menu VALUES (1961, 2, 721, '2026-01-08 02:56:33.633347');
INSERT INTO public.sys_role_menu VALUES (1962, 2, 722, '2026-01-08 02:56:33.63569');
INSERT INTO public.sys_role_menu VALUES (1963, 2, 723, '2026-01-08 02:56:33.635984');
INSERT INTO public.sys_role_menu VALUES (1964, 1, 721, '2026-01-08 02:56:33.636189');
INSERT INTO public.sys_role_menu VALUES (59, 2, 1, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (61, 2, 3, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (62, 2, 4, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (63, 2, 5, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (64, 2, 6, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (65, 2, 8, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (66, 2, 9, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (67, 2, 10, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (68, 2, 11, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (71, 2, 23, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (72, 2, 30, CURRENT_TIMESTAMP);
INSERT INTO public.sys_role_menu VALUES (75, 2, 27, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (76, 2, 28, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (77, 2, 31, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (78, 2, 32, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (79, 2, 33, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (80, 2, 41, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (81, 2, 42, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (82, 2, 45, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (83, 2, 43, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (84, 2, 44, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (85, 2, 51, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (86, 2, 52, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (87, 2, 53, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (88, 2, 54, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (89, 2, 55, '2026-01-04 16:14:15.900075');
INSERT INTO public.sys_role_menu VALUES (316, 2, 61, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (318, 2, 63, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (319, 2, 64, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (320, 2, 81, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (321, 2, 82, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (323, 2, 91, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (324, 2, 92, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (325, 2, 93, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (326, 2, 94, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (327, 2, 95, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (328, 2, 96, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (329, 2, 97, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (330, 2, 98, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (331, 2, 99, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (332, 2, 100, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (333, 2, 101, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (334, 2, 102, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (335, 2, 103, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (336, 2, 104, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (337, 2, 111, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (338, 2, 112, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (339, 2, 113, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (340, 2, 123, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (1965, 1, 722, '2026-01-08 02:56:33.636733');
INSERT INTO public.sys_role_menu VALUES (1966, 1, 723, '2026-01-08 02:56:33.636959');
INSERT INTO public.sys_role_menu VALUES (114, 6, 1, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (115, 6, 3, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (116, 6, 4, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (117, 6, 6, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (118, 6, 8, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (119, 6, 11, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (120, 6, 31, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (121, 6, 32, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (122, 6, 33, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (123, 6, 41, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (124, 6, 42, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (125, 6, 45, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (126, 6, 43, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (127, 6, 44, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (128, 6, 61, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (1591, 6, 200, '2026-01-07 13:47:44.060444');
INSERT INTO public.sys_role_menu VALUES (131, 6, 64, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (132, 6, 81, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (133, 6, 82, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (135, 6, 111, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (136, 6, 112, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (137, 6, 113, '2026-01-04 16:14:15.901047');
INSERT INTO public.sys_role_menu VALUES (1592, 6, 201, '2026-01-07 13:47:44.064594');
INSERT INTO public.sys_role_menu VALUES (1593, 6, 202, '2026-01-07 13:47:44.064983');
INSERT INTO public.sys_role_menu VALUES (1594, 6, 203, '2026-01-07 13:47:44.065269');
INSERT INTO public.sys_role_menu VALUES (1595, 6, 204, '2026-01-07 13:47:44.065553');
INSERT INTO public.sys_role_menu VALUES (1596, 6, 205, '2026-01-07 13:47:44.06585');
INSERT INTO public.sys_role_menu VALUES (1842, 5, 95, '2026-01-07 14:45:54.697523');
INSERT INTO public.sys_role_menu VALUES (1843, 5, 189, '2026-01-07 14:45:54.697523');
INSERT INTO public.sys_role_menu VALUES (1844, 5, 190, '2026-01-07 14:45:54.697523');
INSERT INTO public.sys_role_menu VALUES (1845, 5, 191, '2026-01-07 14:45:54.697523');
INSERT INTO public.sys_role_menu VALUES (1868, 1, 220, '2026-01-08 00:17:54.292355');
INSERT INTO public.sys_role_menu VALUES (1869, 1, 221, '2026-01-08 00:17:54.295108');
INSERT INTO public.sys_role_menu VALUES (1603, 9, 200, '2026-01-07 13:47:44.067433');
INSERT INTO public.sys_role_menu VALUES (1604, 9, 201, '2026-01-07 13:47:44.068091');
INSERT INTO public.sys_role_menu VALUES (151, 2, 105, '2026-01-04 16:14:18.541755');
INSERT INTO public.sys_role_menu VALUES (152, 2, 106, '2026-01-04 16:14:18.541755');
INSERT INTO public.sys_role_menu VALUES (1605, 9, 202, '2026-01-07 13:47:44.068291');
INSERT INTO public.sys_role_menu VALUES (1606, 9, 203, '2026-01-07 13:47:44.068755');
INSERT INTO public.sys_role_menu VALUES (1607, 9, 204, '2026-01-07 13:47:44.069023');
INSERT INTO public.sys_role_menu VALUES (1870, 1, 222, '2026-01-08 00:17:54.295966');
INSERT INTO public.sys_role_menu VALUES (157, 2, 120, '2026-01-05 02:19:31.880543');
INSERT INTO public.sys_role_menu VALUES (158, 2, 56, '2026-01-05 02:19:31.880861');
INSERT INTO public.sys_role_menu VALUES (1608, 9, 205, '2026-01-07 13:47:44.069215');
INSERT INTO public.sys_role_menu VALUES (1871, 1, 223, '2026-01-08 00:17:54.296795');
INSERT INTO public.sys_role_menu VALUES (1872, 1, 224, '2026-01-08 00:17:54.29768');
INSERT INTO public.sys_role_menu VALUES (163, 2, 121, '2026-01-05 02:19:57.79819');
INSERT INTO public.sys_role_menu VALUES (1873, 2, 220, '2026-01-08 00:17:54.298891');
INSERT INTO public.sys_role_menu VALUES (168, 2, 130, '2026-01-05 10:00:00');
INSERT INTO public.sys_role_menu VALUES (169, 2, 131, '2026-01-05 10:00:00');
INSERT INTO public.sys_role_menu VALUES (170, 2, 132, '2026-01-05 10:00:00');
INSERT INTO public.sys_role_menu VALUES (1874, 2, 221, '2026-01-08 00:17:54.299588');
INSERT INTO public.sys_role_menu VALUES (1875, 2, 222, '2026-01-08 00:17:54.300354');
INSERT INTO public.sys_role_menu VALUES (1876, 2, 223, '2026-01-08 00:17:54.301257');
INSERT INTO public.sys_role_menu VALUES (174, 6, 5, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (175, 6, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (176, 6, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (177, 6, 59, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (1877, 2, 224, '2026-01-08 00:17:54.303514');
INSERT INTO public.sys_role_menu VALUES (1878, 3, 220, '2026-01-08 00:17:54.305211');
INSERT INTO public.sys_role_menu VALUES (1879, 3, 221, '2026-01-08 00:17:54.307174');
INSERT INTO public.sys_role_menu VALUES (1880, 3, 222, '2026-01-08 00:17:54.308739');
INSERT INTO public.sys_role_menu VALUES (182, 9, 5, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (183, 9, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (184, 9, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (185, 9, 59, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (1881, 3, 223, '2026-01-08 00:17:54.309472');
INSERT INTO public.sys_role_menu VALUES (1882, 3, 224, '2026-01-08 00:17:54.310188');
INSERT INTO public.sys_role_menu VALUES (1883, 6, 220, '2026-01-08 00:17:54.310829');
INSERT INTO public.sys_role_menu VALUES (1884, 6, 221, '2026-01-08 00:17:54.311648');
INSERT INTO public.sys_role_menu VALUES (1885, 6, 222, '2026-01-08 00:17:54.312284');
INSERT INTO public.sys_role_menu VALUES (1886, 6, 223, '2026-01-08 00:17:54.312933');
INSERT INTO public.sys_role_menu VALUES (1887, 9, 220, '2026-01-08 00:17:54.313535');
INSERT INTO public.sys_role_menu VALUES (1888, 9, 222, '2026-01-08 00:17:54.314448');
INSERT INTO public.sys_role_menu VALUES (1967, 3, 91, '2026-01-08 03:26:38.443447');
INSERT INTO public.sys_role_menu VALUES (1968, 5, 91, '2026-01-08 03:26:38.447877');
INSERT INTO public.sys_role_menu VALUES (1969, 6, 91, '2026-01-08 03:26:38.448744');
INSERT INTO public.sys_role_menu VALUES (1970, 9, 91, '2026-01-08 03:26:38.449029');
INSERT INTO public.sys_role_menu VALUES (1971, 3, 92, '2026-01-08 03:26:38.449666');
INSERT INTO public.sys_role_menu VALUES (1972, 5, 92, '2026-01-08 03:26:38.449886');
INSERT INTO public.sys_role_menu VALUES (1973, 6, 92, '2026-01-08 03:26:38.450138');
INSERT INTO public.sys_role_menu VALUES (1974, 9, 92, '2026-01-08 03:26:38.450368');
INSERT INTO public.sys_role_menu VALUES (1975, 3, 93, '2026-01-08 03:26:38.450576');
INSERT INTO public.sys_role_menu VALUES (1976, 5, 93, '2026-01-08 03:26:38.450775');
INSERT INTO public.sys_role_menu VALUES (1977, 6, 93, '2026-01-08 03:26:38.451006');
INSERT INTO public.sys_role_menu VALUES (1978, 9, 93, '2026-01-08 03:26:38.451215');
INSERT INTO public.sys_role_menu VALUES (1979, 3, 97, '2026-01-08 03:26:38.451427');
INSERT INTO public.sys_role_menu VALUES (1980, 6, 97, '2026-01-08 03:26:38.451604');
INSERT INTO public.sys_role_menu VALUES (1981, 9, 97, '2026-01-08 03:26:38.451809');
INSERT INTO public.sys_role_menu VALUES (1982, 3, 99, '2026-01-08 03:26:38.452024');
INSERT INTO public.sys_role_menu VALUES (1983, 6, 99, '2026-01-08 03:26:38.452242');
INSERT INTO public.sys_role_menu VALUES (1984, 9, 99, '2026-01-08 03:26:38.452437');
INSERT INTO public.sys_role_menu VALUES (1988, 3, 176, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (1990, 6, 176, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (1992, 9, 176, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (1995, 3, 177, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (1998, 3, 169, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (2000, 6, 169, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (2002, 9, 169, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (2005, 3, 178, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (2006, 1, 218, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (2007, 2, 218, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (2008, 3, 218, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (2009, 1, 219, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (2010, 2, 219, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (2011, 8, 219, '2026-01-08 03:26:38.464721');
INSERT INTO public.sys_role_menu VALUES (1846, 5, 96, '2026-01-07 14:45:54.753875');
INSERT INTO public.sys_role_menu VALUES (1847, 5, 193, '2026-01-07 14:45:54.753875');
INSERT INTO public.sys_role_menu VALUES (1848, 5, 194, '2026-01-07 14:45:54.753875');
INSERT INTO public.sys_role_menu VALUES (1609, 5, 200, '2026-01-07 13:52:56.387645');
INSERT INTO public.sys_role_menu VALUES (1849, 5, 195, '2026-01-07 14:45:54.753875');
INSERT INTO public.sys_role_menu VALUES (1889, 5, 130, '2026-01-08 01:20:27.134532');
INSERT INTO public.sys_role_menu VALUES (1890, 5, 131, '2026-01-08 01:20:27.326253');
INSERT INTO public.sys_role_menu VALUES (1891, 6, 130, '2026-01-08 01:20:27.332915');
INSERT INTO public.sys_role_menu VALUES (1893, 9, 130, '2026-01-08 01:20:27.334083');
INSERT INTO public.sys_role_menu VALUES (2012, 1, 159, '2026-01-08 04:48:25.444156');
INSERT INTO public.sys_role_menu VALUES (1610, 5, 201, '2026-01-07 13:52:56.393525');
INSERT INTO public.sys_role_menu VALUES (1499, 5, 703, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1500, 5, 1, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (198, 2, 57, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (199, 2, 58, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (200, 2, 59, '2026-01-05 16:00:00');
INSERT INTO public.sys_role_menu VALUES (1611, 5, 202, '2026-01-07 13:52:56.393882');
INSERT INTO public.sys_role_menu VALUES (1612, 5, 203, '2026-01-07 13:52:56.394145');
INSERT INTO public.sys_role_menu VALUES (1613, 5, 204, '2026-01-07 13:52:56.394395');
INSERT INTO public.sys_role_menu VALUES (231, 9, 1, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (232, 9, 3, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (233, 9, 4, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (234, 9, 6, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (235, 9, 11, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (236, 9, 31, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (237, 9, 32, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (238, 9, 42, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (239, 9, 43, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (240, 9, 44, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (241, 9, 61, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (243, 9, 111, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (244, 9, 112, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (245, 9, 113, '2026-01-05 17:00:00');
INSERT INTO public.sys_role_menu VALUES (1505, 5, 81, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (349, 2, 140, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu VALUES (354, 2, 145, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu VALUES (355, 2, 146, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu VALUES (356, 2, 147, '2026-01-05 18:00:00');
INSERT INTO public.sys_role_menu VALUES (1614, 5, 205, '2026-01-07 13:52:56.394631');
INSERT INTO public.sys_role_menu VALUES (1506, 5, 111, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1507, 5, 150, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (363, 2, 701, '2026-01-05 15:01:30.641063');
INSERT INTO public.sys_role_menu VALUES (366, 6, 701, '2026-01-05 15:01:30.641063');
INSERT INTO public.sys_role_menu VALUES (1510, 5, 168, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1512, 5, 175, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1513, 5, 180, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (371, 2, 148, '2026-01-06 01:27:28.877809');
INSERT INTO public.sys_role_menu VALUES (1514, 5, 187, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (375, 6, 148, '2026-01-06 01:27:28.877809');
INSERT INTO public.sys_role_menu VALUES (1515, 5, 192, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1516, 5, 210, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1517, 5, 216, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1519, 5, 704, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1523, 5, 82, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (386, 2, 149, '2026-01-06 04:02:22.676255');
INSERT INTO public.sys_role_menu VALUES (387, 2, 150, '2026-01-06 04:02:22.676973');
INSERT INTO public.sys_role_menu VALUES (388, 2, 151, '2026-01-06 04:02:22.677166');
INSERT INTO public.sys_role_menu VALUES (1524, 5, 112, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1525, 5, 149, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1526, 5, 151, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1528, 5, 169, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1529, 5, 176, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1530, 5, 188, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1531, 5, 211, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (418, 6, 149, '2026-01-06 04:02:22.683511');
INSERT INTO public.sys_role_menu VALUES (419, 6, 150, '2026-01-06 04:02:22.68383');
INSERT INTO public.sys_role_menu VALUES (1850, 5, 97, '2026-01-07 14:45:54.807937');
INSERT INTO public.sys_role_menu VALUES (1409, 3, 703, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1410, 3, 1, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1412, 3, 31, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1413, 3, 41, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1414, 3, 57, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1415, 3, 130, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (433, 2, 161, '2026-01-06 04:02:27.652068');
INSERT INTO public.sys_role_menu VALUES (434, 2, 162, '2026-01-06 04:02:27.652299');
INSERT INTO public.sys_role_menu VALUES (435, 2, 163, '2026-01-06 04:02:27.652629');
INSERT INTO public.sys_role_menu VALUES (436, 2, 164, '2026-01-06 04:02:27.652819');
INSERT INTO public.sys_role_menu VALUES (437, 2, 165, '2026-01-06 04:02:27.65301');
INSERT INTO public.sys_role_menu VALUES (1416, 3, 132, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1417, 3, 150, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1418, 3, 161, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1419, 3, 163, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (452, 6, 161, '2026-01-06 04:02:27.656649');
INSERT INTO public.sys_role_menu VALUES (453, 6, 162, '2026-01-06 04:02:27.656796');
INSERT INTO public.sys_role_menu VALUES (454, 6, 163, '2026-01-06 04:02:27.656966');
INSERT INTO public.sys_role_menu VALUES (455, 6, 164, '2026-01-06 04:02:27.657131');
INSERT INTO public.sys_role_menu VALUES (1428, 3, 196, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1429, 3, 210, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1430, 3, 216, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1431, 3, 217, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1432, 3, 704, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1433, 3, 32, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1434, 3, 42, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1435, 3, 58, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1436, 3, 112, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1437, 3, 131, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1438, 3, 149, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1439, 3, 151, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1440, 3, 162, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1447, 3, 211, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1448, 3, 705, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1449, 3, 3, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1450, 3, 33, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1451, 3, 45, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1452, 3, 59, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1453, 3, 164, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (494, 2, 174, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (495, 2, 175, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (496, 2, 168, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (497, 2, 169, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (498, 2, 170, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (499, 2, 171, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (500, 2, 172, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (501, 2, 173, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (502, 2, 176, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (503, 2, 177, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (504, 2, 178, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (505, 2, 179, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (506, 2, 180, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (507, 2, 181, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (508, 2, 182, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (509, 2, 183, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (510, 2, 184, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (511, 2, 185, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (512, 2, 186, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (513, 2, 187, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (514, 2, 188, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (515, 2, 189, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (516, 2, 190, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (517, 2, 191, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (518, 2, 192, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (519, 2, 193, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (520, 2, 194, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (521, 2, 195, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (522, 2, 196, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (523, 2, 197, '2026-01-06 04:13:50.996359');
INSERT INTO public.sys_role_menu VALUES (1459, 3, 212, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1460, 3, 706, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1461, 3, 4, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1462, 3, 43, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1463, 3, 148, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1464, 3, 165, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1466, 3, 213, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1467, 3, 707, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1468, 3, 5, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1469, 3, 44, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1471, 3, 145, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1474, 3, 214, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1475, 3, 701, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1476, 3, 56, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1479, 3, 215, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1481, 3, 9, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1482, 3, 51, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1483, 3, 52, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1484, 3, 120, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1486, 3, 53, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1487, 3, 107, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1490, 3, 54, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1491, 3, 108, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1493, 3, 55, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1852, 5, 9, '2026-01-07 14:45:54.864673');
INSERT INTO public.sys_role_menu VALUES (1853, 5, 99, '2026-01-07 14:45:54.864673');
INSERT INTO public.sys_role_menu VALUES (1494, 3, 64, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1497, 3, 207, '2026-01-07 18:49:28.743814');
INSERT INTO public.sys_role_menu VALUES (1543, 5, 213, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1544, 5, 707, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1545, 5, 5, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1547, 5, 145, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1548, 5, 184, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1549, 5, 214, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1550, 5, 701, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1552, 5, 56, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1553, 5, 185, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (657, 2, 160, '2026-01-06 14:00:00');
INSERT INTO public.sys_role_menu VALUES (1554, 5, 215, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1555, 5, 8, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1556, 5, 147, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1557, 5, 11, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1558, 5, 51, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1647, 6, 151, '2026-01-07 14:00:35.037142');
INSERT INTO public.sys_role_menu VALUES (707, 2, 200, '2026-01-06 14:00:00');
INSERT INTO public.sys_role_menu VALUES (708, 2, 201, '2026-01-06 14:00:00');
INSERT INTO public.sys_role_menu VALUES (709, 2, 202, '2026-01-06 14:00:00');
INSERT INTO public.sys_role_menu VALUES (710, 2, 203, '2026-01-06 14:00:00');
INSERT INTO public.sys_role_menu VALUES (711, 2, 204, '2026-01-06 14:00:00');
INSERT INTO public.sys_role_menu VALUES (712, 2, 205, '2026-01-06 14:00:00');
INSERT INTO public.sys_role_menu VALUES (752, 2, 107, '2026-01-06 07:30:05.776922');
INSERT INTO public.sys_role_menu VALUES (756, 2, 108, '2026-01-06 07:30:05.776922');
INSERT INTO public.sys_role_menu VALUES (1559, 5, 52, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (760, 6, 108, '2026-01-06 07:30:05.776922');
INSERT INTO public.sys_role_menu VALUES (763, 9, 108, '2026-01-06 07:30:05.776922');
INSERT INTO public.sys_role_menu VALUES (765, 2, 109, '2026-01-06 07:30:05.776922');
INSERT INTO public.sys_role_menu VALUES (1707, 5, 57, '2026-01-07 14:00:35.049064');
INSERT INTO public.sys_role_menu VALUES (1709, 5, 59, '2026-01-07 14:00:35.049064');
INSERT INTO public.sys_role_menu VALUES (768, 2, 210, '2026-01-06 07:38:37.94416');
INSERT INTO public.sys_role_menu VALUES (1732, 5, 160, '2026-01-07 14:00:35.049064');
INSERT INTO public.sys_role_menu VALUES (772, 2, 211, '2026-01-06 07:38:37.94416');
INSERT INTO public.sys_role_menu VALUES (776, 2, 212, '2026-01-06 07:38:37.94416');
INSERT INTO public.sys_role_menu VALUES (780, 2, 213, '2026-01-06 07:38:37.94416');
INSERT INTO public.sys_role_menu VALUES (784, 2, 214, '2026-01-06 07:38:37.94416');
INSERT INTO public.sys_role_menu VALUES (788, 2, 215, '2026-01-06 07:38:37.94416');
INSERT INTO public.sys_role_menu VALUES (792, 2, 216, '2026-01-06 07:38:37.94416');
INSERT INTO public.sys_role_menu VALUES (1561, 5, 53, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (796, 6, 216, '2026-01-06 07:38:37.94416');
INSERT INTO public.sys_role_menu VALUES (799, 9, 216, '2026-01-06 07:38:37.94416');
INSERT INTO public.sys_role_menu VALUES (1769, 9, 8, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1770, 9, 33, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1771, 9, 41, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1772, 9, 45, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1773, 9, 81, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1774, 9, 82, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1776, 9, 701, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1777, 9, 148, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1778, 9, 149, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (801, 1, 109, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (802, 1, 1, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (803, 1, 21, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (804, 1, 31, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (805, 1, 41, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (806, 1, 57, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (807, 1, 61, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (808, 1, 81, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (809, 1, 91, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (810, 1, 101, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (811, 1, 111, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (812, 1, 124, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (813, 1, 150, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (814, 1, 161, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (815, 1, 163, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (816, 1, 168, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (817, 1, 170, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (818, 1, 173, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (819, 1, 174, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (820, 1, 175, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (821, 1, 178, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (822, 1, 180, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (823, 1, 187, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (824, 1, 192, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (825, 1, 196, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (826, 1, 197, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (827, 1, 201, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (828, 1, 210, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (829, 1, 216, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (830, 1, 2, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (831, 1, 22, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (832, 1, 32, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (833, 1, 42, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (834, 1, 58, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (836, 1, 82, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (837, 1, 92, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (838, 1, 102, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (839, 1, 112, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (840, 1, 125, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (841, 1, 149, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (842, 1, 151, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (843, 1, 162, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (844, 1, 169, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (845, 1, 171, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (846, 1, 176, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (847, 1, 179, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (848, 1, 181, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (849, 1, 188, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (850, 1, 193, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (851, 1, 202, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (852, 1, 211, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (853, 1, 3, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (854, 1, 33, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (855, 1, 45, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (856, 1, 59, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (857, 1, 63, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (858, 1, 83, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (859, 1, 93, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (860, 1, 103, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (861, 1, 113, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (862, 1, 164, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (863, 1, 172, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (864, 1, 177, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (865, 1, 182, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (866, 1, 189, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (867, 1, 194, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (868, 1, 198, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (869, 1, 200, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (870, 1, 203, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (871, 1, 212, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (872, 1, 4, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (873, 1, 23, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (874, 1, 43, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (875, 1, 64, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (876, 1, 94, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (877, 1, 104, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (878, 1, 148, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (879, 1, 165, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (880, 1, 183, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (881, 1, 190, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (882, 1, 195, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (883, 1, 204, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (884, 1, 213, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (885, 1, 5, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (886, 1, 24, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (887, 1, 44, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (888, 1, 95, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (889, 1, 105, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (890, 1, 145, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (891, 1, 184, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (892, 1, 191, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (893, 1, 205, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (894, 1, 214, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (895, 1, 701, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (896, 1, 6, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (897, 1, 25, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (898, 1, 56, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (899, 1, 96, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (900, 1, 106, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (901, 1, 146, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (902, 1, 185, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (903, 1, 215, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (904, 1, 8, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (905, 1, 26, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (906, 1, 97, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (907, 1, 147, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (908, 1, 186, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (909, 1, 9, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (910, 1, 27, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (911, 1, 98, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (912, 1, 10, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (913, 1, 28, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (914, 1, 99, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (915, 1, 11, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (916, 1, 51, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (917, 1, 100, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (918, 1, 52, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (919, 1, 120, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (920, 1, 152, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (921, 1, 53, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (922, 1, 107, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (923, 1, 121, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (924, 1, 153, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (925, 1, 54, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (926, 1, 108, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (927, 1, 154, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (928, 1, 55, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (929, 1, 155, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (930, 1, 156, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (931, 1, 140, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (932, 1, 123, '2026-01-06 19:43:51.724722');
INSERT INTO public.sys_role_menu VALUES (1779, 9, 150, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1782, 9, 161, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1783, 9, 162, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1784, 9, 163, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1785, 9, 164, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1792, 9, 151, '2026-01-07 14:13:53.880934');
INSERT INTO public.sys_role_menu VALUES (1160, 2, 2, '2026-01-06 12:48:28.721214');
INSERT INTO public.sys_role_menu VALUES (1854, 1, 130, '2026-01-07 14:57:02.239275');
INSERT INTO public.sys_role_menu VALUES (1172, 6, 703, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1855, 1, 131, '2026-01-07 14:57:02.239275');
INSERT INTO public.sys_role_menu VALUES (1532, 5, 705, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1175, 9, 703, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1921, 1, 62, '2026-01-08 01:43:37.717808');
INSERT INTO public.sys_role_menu VALUES (1181, 6, 704, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1922, 2, 62, '2026-01-08 01:43:37.719517');
INSERT INTO public.sys_role_menu VALUES (1923, 3, 62, '2026-01-08 01:43:37.720195');
INSERT INTO public.sys_role_menu VALUES (1184, 9, 704, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1924, 6, 62, '2026-01-08 01:43:37.720398');
INSERT INTO public.sys_role_menu VALUES (1190, 6, 705, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1925, 8, 62, '2026-01-08 01:43:37.720595');
INSERT INTO public.sys_role_menu VALUES (1926, 9, 62, '2026-01-08 01:43:37.72357');
INSERT INTO public.sys_role_menu VALUES (1193, 9, 705, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1927, 1, 713, '2026-01-08 01:43:37.723807');
INSERT INTO public.sys_role_menu VALUES (1199, 6, 706, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1928, 2, 713, '2026-01-08 01:43:37.724041');
INSERT INTO public.sys_role_menu VALUES (1929, 8, 713, '2026-01-08 01:43:37.724352');
INSERT INTO public.sys_role_menu VALUES (1202, 9, 706, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1930, 1, 710, '2026-01-08 01:43:37.725449');
INSERT INTO public.sys_role_menu VALUES (1931, 1, 711, '2026-01-08 01:43:37.725654');
INSERT INTO public.sys_role_menu VALUES (1208, 6, 707, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1932, 1, 712, '2026-01-08 01:43:37.725828');
INSERT INTO public.sys_role_menu VALUES (1536, 5, 113, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1211, 9, 707, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1933, 2, 710, '2026-01-08 01:43:37.726031');
INSERT INTO public.sys_role_menu VALUES (1934, 2, 711, '2026-01-08 01:43:37.7263');
INSERT INTO public.sys_role_menu VALUES (1217, 6, 207, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1935, 2, 712, '2026-01-08 01:43:37.726506');
INSERT INTO public.sys_role_menu VALUES (1936, 3, 710, '2026-01-08 01:43:37.7267');
INSERT INTO public.sys_role_menu VALUES (1220, 9, 207, '2026-01-06 13:11:49.476657');
INSERT INTO public.sys_role_menu VALUES (1538, 5, 212, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1937, 3, 711, '2026-01-08 01:43:37.72687');
INSERT INTO public.sys_role_menu VALUES (1539, 5, 706, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1233, 9, 64, '2026-01-07 08:34:04.734076');
INSERT INTO public.sys_role_menu VALUES (1230, 1, 217, '2026-01-07 10:15:16.523696');
INSERT INTO public.sys_role_menu VALUES (1938, 3, 712, '2026-01-08 01:43:37.727377');
INSERT INTO public.sys_role_menu VALUES (1939, 6, 710, '2026-01-08 01:43:37.727558');
INSERT INTO public.sys_role_menu VALUES (1940, 6, 711, '2026-01-08 01:43:37.727713');
INSERT INTO public.sys_role_menu VALUES (1941, 6, 712, '2026-01-08 01:43:37.727886');
INSERT INTO public.sys_role_menu VALUES (1942, 8, 710, '2026-01-08 01:43:37.728078');
INSERT INTO public.sys_role_menu VALUES (1943, 8, 711, '2026-01-08 01:43:37.728248');
INSERT INTO public.sys_role_menu VALUES (1944, 8, 712, '2026-01-08 01:43:37.728426');
INSERT INTO public.sys_role_menu VALUES (1238, 9, 217, '2026-01-07 10:15:16.530622');
INSERT INTO public.sys_role_menu VALUES (1245, 8, 703, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1246, 8, 1, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1945, 9, 710, '2026-01-08 01:43:37.728603');
INSERT INTO public.sys_role_menu VALUES (1946, 9, 711, '2026-01-08 01:43:37.728772');
INSERT INTO public.sys_role_menu VALUES (1251, 8, 61, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1252, 8, 81, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1253, 8, 91, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1254, 8, 111, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1255, 8, 150, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1947, 9, 712, '2026-01-08 01:43:37.728935');
INSERT INTO public.sys_role_menu VALUES (1948, 1, 708, '2026-01-08 01:43:37.729305');
INSERT INTO public.sys_role_menu VALUES (1258, 8, 168, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1259, 8, 170, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1260, 8, 173, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1261, 8, 174, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1262, 8, 175, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1263, 8, 178, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1264, 8, 197, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1265, 8, 216, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1949, 1, 709, '2026-01-08 01:43:37.729457');
INSERT INTO public.sys_role_menu VALUES (1267, 8, 704, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1950, 2, 708, '2026-01-08 01:43:37.729617');
INSERT INTO public.sys_role_menu VALUES (1951, 2, 709, '2026-01-08 01:43:37.72978');
INSERT INTO public.sys_role_menu VALUES (1272, 8, 82, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1273, 8, 92, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1274, 8, 112, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1275, 8, 149, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1276, 8, 151, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1952, 8, 708, '2026-01-08 01:43:37.729938');
INSERT INTO public.sys_role_menu VALUES (1278, 8, 169, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1279, 8, 171, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1280, 8, 176, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1281, 8, 179, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1282, 8, 705, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1953, 8, 709, '2026-01-08 01:43:37.73014');
INSERT INTO public.sys_role_menu VALUES (1286, 8, 59, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1287, 8, 93, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1288, 8, 113, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1290, 8, 172, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1291, 8, 177, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1292, 8, 706, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1295, 8, 94, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1298, 8, 707, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1299, 8, 5, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1301, 8, 95, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1302, 8, 6, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1303, 8, 96, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1304, 8, 8, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1305, 8, 97, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1306, 8, 9, '2026-01-07 18:46:46.060222');
INSERT INTO public.sys_role_menu VALUES (1562, 5, 107, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1564, 5, 54, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1565, 5, 108, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1859, 8, 2, '2026-01-07 15:24:40.110193');
INSERT INTO public.sys_role_menu VALUES (1567, 5, 55, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1954, 1, 714, '2026-01-08 02:15:45.87159');
INSERT INTO public.sys_role_menu VALUES (1571, 5, 123, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1572, 5, 207, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1573, 5, 58, '2026-01-07 18:49:49.999575');
INSERT INTO public.sys_role_menu VALUES (1808, 8, 27, '2026-01-07 14:24:17.85559');
INSERT INTO public.sys_role_menu VALUES (1809, 8, 10, '2026-01-07 14:24:17.859586');
INSERT INTO public.sys_role_menu VALUES (1810, 8, 101, '2026-01-07 14:24:17.859943');
INSERT INTO public.sys_role_menu VALUES (1811, 8, 102, '2026-01-07 14:24:17.860273');
INSERT INTO public.sys_role_menu VALUES (1812, 8, 103, '2026-01-07 14:24:17.860554');
INSERT INTO public.sys_role_menu VALUES (1813, 8, 104, '2026-01-07 14:24:17.860868');
INSERT INTO public.sys_role_menu VALUES (1814, 8, 105, '2026-01-07 14:24:17.861153');
INSERT INTO public.sys_role_menu VALUES (1815, 8, 106, '2026-01-07 14:24:17.861571');
INSERT INTO public.sys_role_menu VALUES (1816, 8, 701, '2026-01-07 14:24:17.861853');
INSERT INTO public.sys_role_menu VALUES (1817, 8, 130, '2026-01-07 14:24:17.862109');
INSERT INTO public.sys_role_menu VALUES (1818, 8, 131, '2026-01-07 14:24:17.862298');
INSERT INTO public.sys_role_menu VALUES (1819, 8, 180, '2026-01-07 14:24:17.862479');
INSERT INTO public.sys_role_menu VALUES (1820, 8, 181, '2026-01-07 14:24:17.862693');
INSERT INTO public.sys_role_menu VALUES (1821, 8, 182, '2026-01-07 14:24:17.862923');
INSERT INTO public.sys_role_menu VALUES (1822, 8, 183, '2026-01-07 14:24:17.863152');
INSERT INTO public.sys_role_menu VALUES (1823, 8, 184, '2026-01-07 14:24:17.863418');
INSERT INTO public.sys_role_menu VALUES (1824, 8, 185, '2026-01-07 14:24:17.863619');
INSERT INTO public.sys_role_menu VALUES (1825, 8, 186, '2026-01-07 14:24:17.863795');
INSERT INTO public.sys_role_menu VALUES (1826, 8, 187, '2026-01-07 14:24:17.863974');
INSERT INTO public.sys_role_menu VALUES (1827, 8, 188, '2026-01-07 14:24:17.864149');
INSERT INTO public.sys_role_menu VALUES (1828, 8, 189, '2026-01-07 14:24:17.864339');
INSERT INTO public.sys_role_menu VALUES (1829, 8, 190, '2026-01-07 14:24:17.864544');
INSERT INTO public.sys_role_menu VALUES (1830, 8, 191, '2026-01-07 14:24:17.864739');
INSERT INTO public.sys_role_menu VALUES (1831, 8, 192, '2026-01-07 14:24:17.864948');
INSERT INTO public.sys_role_menu VALUES (1832, 8, 193, '2026-01-07 14:24:17.865149');
INSERT INTO public.sys_role_menu VALUES (1833, 8, 194, '2026-01-07 14:24:17.865324');
INSERT INTO public.sys_role_menu VALUES (1834, 8, 195, '2026-01-07 14:24:17.865516');
INSERT INTO public.sys_role_menu VALUES (1955, 2, 714, '2026-01-08 02:15:45.873391');
INSERT INTO public.sys_role_menu VALUES (1956, 3, 714, '2026-01-08 02:15:45.873732');
INSERT INTO public.sys_role_menu VALUES (1957, 5, 714, '2026-01-08 02:15:45.874033');
INSERT INTO public.sys_role_menu VALUES (1958, 6, 714, '2026-01-08 02:15:45.874698');
INSERT INTO public.sys_role_menu VALUES (1959, 8, 714, '2026-01-08 02:15:45.87506');
INSERT INTO public.sys_role_menu VALUES (1960, 9, 714, '2026-01-08 02:15:45.87563');
INSERT INTO public.sys_role_menu VALUES (1970, 8, 196, '2026-01-08 15:35:00');

-- =====================================================
-- HR模块菜单权限补充（2026-01-09）
-- =====================================================

-- 培训管理权限按钮（代码中实际使用）
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted)
VALUES 
(1011, 101, '发布培训通知', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:training:create', 1, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
(1012, 101, '删除培训通知', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:training:delete', 2, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
(1013, 101, '查看完成情况', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:training:list', 3, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    permission = EXCLUDED.permission,
    updated_at = CURRENT_TIMESTAMP;

-- 晋升管理权限按钮（代码中实际使用）
INSERT INTO public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted)
VALUES 
(730, 103, '职级查看', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:view', 1, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
(731, 103, '职级创建', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:create', 2, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
(732, 103, '职级编辑', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:edit', 3, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
(733, 103, '职级删除', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:delete', 4, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false),
(734, 103, '晋升审批', NULL, NULL, NULL, NULL, 'BUTTON', 'hr:promotion:approve', 5, true, 'ENABLED', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

-- 为管理员角色分配培训管理和晋升管理权限
INSERT INTO public.sys_role_menu (role_id, menu_id)
SELECT 1, id FROM public.sys_menu WHERE id IN (101, 1011, 1012, 1013, 103, 730, 731, 732, 733, 734)
ON CONFLICT DO NOTHING;

-- 为律所主任角色分配培训管理和晋升管理权限
INSERT INTO public.sys_role_menu (role_id, menu_id)
SELECT 2, id FROM public.sys_menu WHERE id IN (101, 1011, 1012, 1013, 103, 730, 731, 732, 733, 734)
ON CONFLICT DO NOTHING;

-- 为行政角色分配培训管理权限
INSERT INTO public.sys_role_menu (role_id, menu_id)
SELECT 8, id FROM public.sys_menu WHERE id IN (101, 1011, 1012, 1013)
ON CONFLICT DO NOTHING;

-- 为其他角色（团队负责人、律师、实习律师）添加培训菜单查看权限
INSERT INTO public.sys_role_menu (role_id, menu_id)
SELECT r.id, 101 FROM public.sys_role r 
WHERE r.id IN (3, 6, 9)
AND NOT EXISTS (SELECT 1 FROM public.sys_role_menu WHERE role_id = r.id AND menu_id = 101);

-- 为管理员角色分配数据库备份菜单权限
INSERT INTO public.sys_role_menu (role_id, menu_id)
SELECT 1, id FROM public.sys_menu WHERE id = 29
ON CONFLICT (role_id, menu_id) DO NOTHING;

--
-- Name: sys_config_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: sys_dict_item_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: sys_dict_type_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: sys_external_integration_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: sys_menu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: sys_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: sys_role_menu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

-- =====================================================
-- 默认用户数据
-- =====================================================
-- 默认密码: admin123
-- BCrypt 哈希 (cost=12): $2a$12$VwbtDL2dik9LtPNlLf7qqu64ZSlS67cedlG/eQXm0mGK7sONamcLO
-- =====================================================

-- 插入默认用户
INSERT INTO public.sys_user (id, username, password, real_name, email, phone, status, deleted, created_at, updated_at, version)
VALUES 
(1, 'admin', '$2a$12$VwbtDL2dik9LtPNlLf7qqu64ZSlS67cedlG/eQXm0mGK7sONamcLO', '系统管理员', 'admin@lawfirm.com', '13800000001', 'ACTIVE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'director', '$2a$12$VwbtDL2dik9LtPNlLf7qqu64ZSlS67cedlG/eQXm0mGK7sONamcLO', '律所主任', 'director@lawfirm.com', '13800000002', 'ACTIVE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'lawyer1', '$2a$12$VwbtDL2dik9LtPNlLf7qqu64ZSlS67cedlG/eQXm0mGK7sONamcLO', '张律师', 'lawyer1@lawfirm.com', '13800000003', 'ACTIVE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 设置用户ID序列
SELECT setval('sys_user_id_seq', COALESCE((SELECT MAX(id) FROM sys_user), 0) + 1, false);

-- 插入用户角色关联
INSERT INTO public.sys_user_role (user_id, role_id)
VALUES 
(1, 1),  -- admin -> 管理员角色
(2, 2),  -- director -> 主任角色
(3, 6)   -- lawyer1 -> 律师角色
ON CONFLICT (user_id, role_id) DO NOTHING;
