--
-- PostgreSQL database dump
--

\restrict mJpbjeo5I4OCOR1mRRwfCinlU8mIYgdnfF6h7jbnSjNp5bQmbaGedgm9xZ1tOCK

-- Dumped from database version 15.15
-- Dumped by pg_dump version 15.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: dossier_template; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.dossier_template (id, name, case_type, description, is_default, created_by, created_at, updated_at, deleted) FROM stdin;
1	刑事案件卷宗模板	CRIMINAL	适用于刑事辩护案件	t	\N	2026-01-06 14:53:13.69281	2026-01-06 14:53:13.69281	f
2	民事案件卷宗模板	CIVIL	适用于民事诉讼代理案件	t	\N	2026-01-06 14:53:13.695714	2026-01-06 14:53:13.695714	f
3	法律顾问卷宗模板	LEGAL_COUNSEL	适用于常年法律顾问服务	t	\N	2026-01-06 14:53:13.696417	2026-01-06 14:53:13.696417	f
4	其他非诉讼法律事务卷宗模板	NON_LITIGATION	适用于其他非诉讼法律事务	t	\N	2026-01-06 14:53:13.696917	2026-01-06 14:53:13.696917	f
\.


--
-- Data for Name: dossier_template_item; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.dossier_template_item (id, template_id, parent_id, name, item_type, file_category, sort_order, required, description, created_at, updated_by, created_by, updated_at, deleted) FROM stdin;
1	1	0	1.收案审批表	FILE	OTHER	1	t	收案审批表	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
4	1	0	4.收费发票	FILE	OTHER	4	f	收费发票复印件	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
5	1	0	5.办案机关卷宗材料	FOLDER	EVIDENCE	5	f	阅卷笔录和卷宗材料	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
6	1	0	6.会见笔录	FOLDER	MEETING	6	f	会见当事人的笔录	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
7	1	0	7.起诉书/上诉状/抗诉书/再审申请书	FOLDER	PLEADING	7	f	诉讼文书	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
8	1	0	8.重大案件集体讨论记录	FILE	OTHER	8	f	重大案件讨论记录	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
9	1	0	9.辩护词	FILE	PLEADING	9	f	辩护词	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
10	1	0	10.裁定书/判决书	FOLDER	JUDGMENT	10	f	法院裁判文书	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
11	1	0	11.工作日志	FILE	WORKLOG	11	f	工作日志	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
12	1	0	12.结案（归档）审批表	FILE	OTHER	12	t	结案审批表	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
13	1	0	13.其他材料	FOLDER	OTHER	13	f	其他相关材料	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
14	2	0	1.收案审批表	FILE	OTHER	1	t	收案审批表	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
17	2	0	4.收费发票	FILE	OTHER	4	f	收费发票复印件	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
18	2	0	5.起诉状/上诉状/答辩状/再审申请书	FOLDER	PLEADING	5	f	诉讼文书	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
19	2	0	6.证据材料	FOLDER	EVIDENCE	6	f	证据材料和阅卷笔录	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
20	2	0	7.重大案件集体讨论记录	FILE	OTHER	7	f	重大案件讨论记录	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
21	2	0	8.代理词	FILE	PLEADING	8	f	代理词	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
22	2	0	9.判决书/裁定书/调解书	FOLDER	JUDGMENT	9	f	法院裁判文书	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
23	2	0	10.工作日志	FILE	WORKLOG	10	f	工作日志	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
25	2	0	12.其他材料	FOLDER	OTHER	12	f	其他相关材料	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
26	3	0	1.顾问合同	FILE	CONTRACT	1	t	法律顾问服务合同	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
27	3	0	2.收费发票	FILE	OTHER	2	f	收费发票复印件	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
28	3	0	3.法律事务记录	FOLDER	OTHER	3	f	办理各类法律事务的记录及相关材料	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
29	3	0	4.合同审查	FOLDER	OTHER	4	f	起草、审查的合同	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
30	3	0	5.规章制度	FOLDER	OTHER	5	f	起草、审查的规章制度	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
31	3	0	6.法律意见书	FOLDER	PLEADING	6	f	出具的法律意见书	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
32	3	0	7.律师函	FOLDER	CORRESPONDENCE	7	f	出具的律师函	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
33	3	0	8.咨询记录	FOLDER	OTHER	8	f	法律咨询记录	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
34	3	0	9.工作日志	FILE	WORKLOG	9	f	工作日志	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
35	3	0	10.工作小结	FILE	OTHER	10	f	年度工作小结	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
36	3	0	11.其他材料	FOLDER	OTHER	11	f	其他相关材料	2026-01-06 14:53:13.69659	\N	\N	2026-01-06 15:18:31.800534	f
37	4	0	1.授权委托书	FILE	CONTRACT	1	t	授权委托书	2026-01-06 14:53:13.697102	\N	\N	2026-01-06 15:18:31.800534	f
38	4	0	2.收费发票	FILE	OTHER	2	f	收费发票复印件	2026-01-06 14:53:13.697102	\N	\N	2026-01-06 15:18:31.800534	f
39	4	0	3.证据材料	FOLDER	EVIDENCE	3	f	相关证据材料	2026-01-06 14:53:13.697102	\N	\N	2026-01-06 15:18:31.800534	f
40	4	0	4.法律意见书/律师函/其他法律文书	FOLDER	PLEADING	4	f	出具的法律文书	2026-01-06 14:53:13.697102	\N	\N	2026-01-06 15:18:31.800534	f
41	4	0	5.工作小结	FILE	OTHER	5	f	工作小结	2026-01-06 14:53:13.697102	\N	\N	2026-01-06 15:18:31.800534	f
42	4	0	6.其他材料	FOLDER	OTHER	6	f	其他相关材料	2026-01-06 14:53:13.697102	\N	\N	2026-01-06 15:18:31.800534	f
15	2	0	2.授权委托书	FILE	CONTRACT	2	t	授权委托书	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
16	2	0	3.委托合同	FILE	CONTRACT	3	t	委托代理合同	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
24	2	0	11.结案（归档）审批表	FILE	OTHER	11	t	结案审批表	2026-01-06 14:53:13.695896	\N	\N	2026-01-06 15:18:31.800534	f
2	1	0	2.授权委托书	FILE	CONTRACT	2	t	授权委托书	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
3	1	0	3.委托合同	FILE	CONTRACT	3	t	委托代理合同	2026-01-06 14:53:13.694332	\N	\N	2026-01-06 15:18:31.800534	f
\.


--
-- Data for Name: sys_external_integration; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_external_integration (id, integration_code, integration_name, integration_type, description, api_url, api_key, api_secret, auth_type, extra_config, enabled, last_test_time, last_test_result, last_test_message, created_at, updated_at, created_by, updated_by, deleted) FROM stdin;
1	ARCHIVE_CITY	市档案馆	ARCHIVE	对接市档案馆档案管理系统，用于档案迁移	\N	\N	\N	API_KEY	{"timeout": 30000, "retryCount": 3}	f	\N	\N	\N	2026-01-08 04:48:25.438772	2026-01-08 04:48:25.438772	\N	\N	f
2	ARCHIVE_DISTRICT	区档案馆	ARCHIVE	对接区档案馆档案管理系统，用于档案迁移	\N	\N	\N	API_KEY	{"timeout": 30000, "retryCount": 3}	f	\N	\N	\N	2026-01-08 04:48:25.438772	2026-01-08 04:48:25.438772	\N	\N	f
3	ARCHIVE_THIRD_PARTY	第三方档案系统	ARCHIVE	对接第三方档案管理系统	\N	\N	\N	API_KEY	{"timeout": 30000, "retryCount": 3}	f	\N	\N	\N	2026-01-08 04:48:25.438772	2026-01-08 04:48:25.438772	\N	\N	f
4	AI_OPENAI	OpenAI (GPT)	AI	OpenAI GPT系列大模型，支持GPT-4、GPT-3.5等	\N	\N	\N	BEARER_TOKEN	{"model": "gpt-4", "timeout": 60000, "maxTokens": 4096, "temperature": 0.7}	f	\N	\N	\N	2026-01-08 04:48:25.438772	2026-01-08 04:48:25.438772	\N	\N	f
5	AI_CLAUDE	Anthropic (Claude)	AI	Anthropic Claude系列大模型	\N	\N	\N	BEARER_TOKEN	{"model": "claude-3-opus-20240229", "timeout": 60000, "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 04:48:25.438772	2026-01-08 04:48:25.438772	\N	\N	f
6	AI_QWEN	通义千问	AI	阿里云通义千问大模型	\N	\N	\N	API_KEY	{"model": "qwen-max", "timeout": 60000, "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 04:48:25.438772	2026-01-08 04:48:25.438772	\N	\N	f
7	AI_WENXIN	文心一言	AI	百度文心一言大模型	\N	\N	\N	API_KEY	{"model": "ernie-bot-4", "timeout": 60000, "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 04:48:25.438772	2026-01-08 04:48:25.438772	\N	\N	f
8	AI_ZHIPU	智谱清言 (GLM)	AI	智谱AI GLM系列大模型	\N	\N	\N	API_KEY	{"model": "glm-4", "timeout": 60000, "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 04:48:25.438772	2026-01-08 04:48:25.438772	\N	\N	f
10	AI_DEEPSEEK_R1	DeepSeek R1 (推理)	AI	DeepSeek R1 推理模型，推理能力强，适合复杂法律文书生成	https://api.deepseek.com/v1	sk-0e6d99fd0d7a4eee9a5840cdf25d37eb	\N	API_KEY	{"model": "deepseek-reasoner", "maxTokens": 8192}	t	2026-01-08 18:06:47.428094	SUCCESS	连接成功，响应码: 401	2026-01-08 10:06:08.771521	2026-01-08 18:06:47.428094	\N	\N	f
9	AI_DEEPSEEK	DeepSeek Chat (对话)	AI	DeepSeek大模型，支持代码生成等	https://api.deepseek.com/v1	sk-0e6d99fd0d7a4eee9a5840cdf25d37eb	\N	API_KEY	{"model": "deepseek-chat", "timeout": 60000, "maxTokens": 4096}	t	2026-01-08 16:34:30.272092	SUCCESS	连接成功，响应码: 401	2026-01-08 04:48:25.438772	2026-01-08 18:08:46.536256	\N	1	f
11	AI_MOONSHOT	Moonshot (Kimi)	AI	月之暗面 Kimi 大模型，支持超长上下文	https://api.moonshot.cn/v1	\N	\N	API_KEY	{"model": "moonshot-v1-8k", "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 10:06:08.80867	2026-01-08 10:06:08.80867	\N	\N	f
12	AI_YI	零一万物 Yi	AI	零一万物 Yi 大模型	https://api.lingyiwanwu.com/v1	\N	\N	API_KEY	{"model": "yi-34b-chat-0205", "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 10:06:08.813892	2026-01-08 10:06:08.813892	\N	\N	f
13	AI_MINIMAX	MiniMax	AI	MiniMax 大模型	https://api.minimax.chat/v1	\N	\N	API_KEY	{"model": "abab5.5-chat", "groupId": ""}	f	\N	\N	\N	2026-01-08 10:06:08.814523	2026-01-08 10:06:08.814523	\N	\N	f
14	AI_DIFY	Dify	AI	Dify 开源 LLM 应用开发平台	http://localhost:3000/v1	\N	\N	API_KEY	{"user": "law-firm-user", "apiType": "chat", "responseMode": "blocking"}	f	\N	\N	\N	2026-01-08 10:06:08.815244	2026-01-08 10:06:08.815244	\N	\N	f
15	AI_OLLAMA	Ollama	AI	Ollama 本地大模型，支持 Llama、Mistral、Qwen 等	http://localhost:11434	\N	\N	API_KEY	{"model": "llama2", "apiType": "chat"}	f	\N	\N	\N	2026-01-08 10:06:08.817134	2026-01-08 10:06:08.817134	\N	\N	f
16	AI_LOCALAI	LocalAI	AI	LocalAI - OpenAI 兼容的本地 AI 后端	http://localhost:8080/v1	\N	\N	API_KEY	{"model": "gpt-3.5-turbo", "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 10:06:08.818471	2026-01-08 10:06:08.818471	\N	\N	f
17	AI_VLLM	vLLM	AI	vLLM 高性能 LLM 推理框架	http://localhost:8000/v1	\N	\N	API_KEY	{"model": "meta-llama/Llama-2-7b-chat-hf", "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 10:06:08.819038	2026-01-08 10:06:08.819038	\N	\N	f
18	AI_XINFERENCE	Xinference	AI	Xinference 开源分布式推理框架	http://localhost:9997/v1	\N	\N	API_KEY	{"model": "qwen-chat", "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 10:06:08.81993	2026-01-08 10:06:08.81993	\N	\N	f
19	AI_ONEAPI	OneAPI	AI	OneAPI 多模型代理服务	http://localhost:3000/v1	\N	\N	API_KEY	{"model": "gpt-3.5-turbo", "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 10:06:08.820636	2026-01-08 10:06:08.820636	\N	\N	f
20	AI_OPENAI_COMPATIBLE	OpenAI 兼容 API	AI	通用 OpenAI 兼容接口	http://localhost:8080/v1	\N	\N	API_KEY	{"model": "default", "maxTokens": 4096}	f	\N	\N	\N	2026-01-08 10:06:08.8224	2026-01-08 10:06:08.8224	\N	\N	f
21	AI_CUSTOM	自定义 API	AI	自定义 API 接口，支持自定义请求格式	http://localhost:8080	\N	\N	API_KEY	{"responsePath": "choices.0.message.content", "requestTemplate": "openai"}	f	\N	\N	\N	2026-01-08 10:06:08.823428	2026-01-08 10:06:08.823428	\N	\N	f
\.


--
-- Data for Name: sys_menu; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) FROM stdin;
31	3	客户列表	/crm/client	crm/client/index	\N	ContactsOutlined	MENU	client:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.896673	2026-01-04 16:14:15.896673	\N	\N	f
32	3	利冲审查	/crm/conflict	crm/conflict/index	\N	SecurityScanOutlined	MENU	conflict:apply	2	t	ENABLED	f	t	2026-01-04 16:14:15.896673	2026-01-04 16:14:15.896673	\N	\N	f
33	3	案源管理	/crm/lead	crm/lead/index	\N	FunnelPlotOutlined	MENU	lead:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.896673	2026-01-04 16:14:15.896673	\N	\N	f
81	8	档案列表	/archive/list	archive/list/index	\N	FolderOpenOutlined	MENU	archive:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.89755	2026-01-04 16:14:15.89755	\N	\N	f
82	8	档案借阅	/archive/borrow	archive/borrow/index	\N	ExportOutlined	MENU	archive:borrow	2	t	ENABLED	f	t	2026-01-04 16:14:15.89755	2026-01-04 16:14:15.89755	\N	\N	f
174	93	会议室管理	\N	\N	\N	\N	BUTTON	admin:meeting:manage	1	t	ENABLED	f	f	2026-01-06 04:13:09.943026	2026-01-06 04:13:09.943026	\N	\N	f
175	97	加班查看	\N	\N	\N	\N	BUTTON	admin:overtime:view	1	t	ENABLED	f	f	2026-01-06 04:13:09.94419	2026-01-06 04:13:09.94419	\N	\N	f
83	8	档案迁移	/archive/destroy	archive/destroy/index	\N	DeleteOutlined	MENU	archive:migrate:apply	3	t	ENABLED	f	t	2026-01-04 16:14:15.89755	2026-01-08 03:06:03.996592	\N	\N	f
4	0	项目管理	/matter	LAYOUT	\N	FolderOutlined	DIRECTORY	\N	5	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
3	0	客户管理	/crm	LAYOUT	\N	TeamOutlined	DIRECTORY	\N	4	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
5	0	财务管理	/finance	LAYOUT	\N	MoneyCollectOutlined	DIRECTORY	\N	6	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
6	0	卷宗管理	/document	LAYOUT	\N	FileTextOutlined	DIRECTORY	\N	7	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
8	0	档案管理	/archive	LAYOUT	\N	DatabaseOutlined	DIRECTORY	\N	8	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
9	0	行政管理	/admin	LAYOUT	\N	BankOutlined	DIRECTORY	\N	9	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
10	0	人力资源	/hr	LAYOUT	\N	IdcardOutlined	DIRECTORY	\N	10	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
111	11	知识文章	/knowledge/article	knowledge/article/index	\N	FileTextOutlined	MENU	knowledge:article:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.898559	2026-01-04 16:14:15.898559	\N	\N	f
112	11	案例库	/knowledge/case	knowledge/case/index	\N	ContainerOutlined	MENU	knowledge:case:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.898559	2026-01-04 16:14:15.898559	\N	\N	f
113	11	法规库	/knowledge/law	knowledge/law/index	\N	BookOutlined	MENU	knowledge:law:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.898559	2026-01-04 16:14:15.898559	\N	\N	f
130	1	审批列表	\N	\N	\N	\N	BUTTON	approval:list	1	t	ENABLED	f	t	2026-01-05 10:00:00	2026-01-05 10:00:00	\N	\N	f
131	1	审批操作	\N	\N	\N	\N	BUTTON	approval:approve	2	t	ENABLED	f	t	2026-01-05 10:00:00	2026-01-05 10:00:00	\N	\N	f
132	45	合同审批	\N	\N	\N	\N	BUTTON	contract:approve	1	t	ENABLED	f	t	2026-01-05 10:00:00	2026-01-05 10:00:00	\N	\N	f
124	123	同步变更	\N	\N	\N	\N	BUTTON	finance:contract:amendment:sync	1	t	ENABLED	f	t	2026-01-05 03:55:33.410601	2026-01-05 03:55:33.410601	\N	\N	f
125	123	忽略变更	\N	\N	\N	\N	BUTTON	finance:contract:amendment:ignore	2	t	ENABLED	f	t	2026-01-05 03:55:33.410601	2026-01-05 03:55:33.410601	\N	\N	f
145	53	查看提成	\N	\N	\N	\N	BUTTON	finance:commission:view	5	t	ENABLED	f	t	2026-01-05 18:00:00	2026-01-05 18:00:00	\N	\N	f
146	53	提成审批	\N	\N	\N	\N	BUTTON	finance:commission:approve	6	t	ENABLED	f	t	2026-01-05 18:00:00	2026-01-05 18:00:00	\N	\N	f
147	53	提成发放	\N	\N	\N	\N	BUTTON	finance:commission:issue	7	t	ENABLED	f	t	2026-01-05 18:00:00	2026-01-05 18:00:00	\N	\N	f
150	149	日程查看	\N	\N	\N	\N	BUTTON	schedule:view	1	t	ENABLED	f	t	2026-01-06 04:02:22.668354	2026-01-06 04:02:22.668354	\N	\N	f
151	149	日程管理	\N	\N	\N	\N	BUTTON	schedule:manage	2	t	ENABLED	f	t	2026-01-06 04:02:22.668804	2026-01-06 04:02:22.668804	\N	\N	f
161	44	任务列表	\N	\N	\N	\N	BUTTON	task:list	1	t	ENABLED	f	t	2026-01-06 04:02:27.64771	2026-01-06 04:02:27.64771	\N	\N	f
162	44	任务查看	\N	\N	\N	\N	BUTTON	task:view	2	t	ENABLED	f	t	2026-01-06 04:02:27.649123	2026-01-06 04:02:27.649123	\N	\N	f
163	43	工时列表	\N	\N	\N	\N	BUTTON	timesheet:list	1	t	ENABLED	f	t	2026-01-06 04:02:27.649535	2026-01-06 04:02:27.649535	\N	\N	f
164	43	工时查看	\N	\N	\N	\N	BUTTON	timesheet:view	3	t	ENABLED	f	t	2026-01-06 04:02:27.649849	2026-01-06 04:02:27.649849	\N	\N	f
165	43	工时审批	\N	\N	\N	\N	BUTTON	timesheet:approve	4	t	ENABLED	f	t	2026-01-06 04:02:27.650148	2026-01-06 04:02:27.650148	\N	\N	f
168	99	外出查看	\N	\N	\N	\N	BUTTON	admin:goout:view	1	t	ENABLED	f	f	2026-01-06 04:13:09.939964	2026-01-06 04:13:09.939964	\N	\N	f
169	99	外出登记	\N	\N	\N	\N	BUTTON	admin:goout:register	2	t	ENABLED	f	f	2026-01-06 04:13:09.939964	2026-01-06 04:13:09.939964	\N	\N	f
170	100	会议记录查看	\N	\N	\N	\N	BUTTON	admin:meeting:view	1	t	ENABLED	f	f	2026-01-06 04:13:09.942358	2026-01-06 04:13:09.942358	\N	\N	f
171	100	会议记录管理	\N	\N	\N	\N	BUTTON	admin:meeting:record	2	t	ENABLED	f	f	2026-01-06 04:13:09.942358	2026-01-06 04:13:09.942358	\N	\N	f
172	100	会议通知	\N	\N	\N	\N	BUTTON	admin:meeting:notice	3	t	ENABLED	f	f	2026-01-06 04:13:09.942358	2026-01-06 04:13:09.942358	\N	\N	f
173	98	资产盘点操作	\N	\N	\N	\N	BUTTON	admin:asset:inventory	1	t	ENABLED	f	f	2026-01-06 04:13:09.942766	2026-01-06 04:13:09.942766	\N	\N	f
1	0	工作台	/dashboard/workspace	dashboard/workspace/index	\N	DashboardOutlined	MENU	\N	1	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
149	0	日程管理	/workbench/schedule	workbench/schedule/index	\N	CalendarOutlined	MENU	schedule:list	2	t	ENABLED	f	t	2026-01-06 04:02:22.660554	2026-01-06 04:02:22.660554	\N	\N	f
701	0	审批中心	/dashboard/approval	dashboard/approval/index	\N	ant-design:audit-outlined	MENU	approval:list	3	t	ENABLED	f	t	2026-01-05 15:01:30.641063	2026-01-05 15:01:30.641063	\N	\N	f
148	4	我的合同	/matter/my-contract	matter/my-contract/index	\N	FileTextOutlined	MENU	matter:contract:view	4	t	ENABLED	f	t	2026-01-06 01:27:18.190968	2026-01-06 01:27:18.190968	\N	\N	f
176	97	加班申请	\N	\N	\N	\N	BUTTON	admin:overtime:apply	2	t	ENABLED	f	f	2026-01-06 04:13:09.94419	2026-01-06 04:13:09.94419	\N	\N	f
177	97	加班审批	\N	\N	\N	\N	BUTTON	admin:overtime:approve	3	t	ENABLED	f	f	2026-01-06 04:13:09.94419	2026-01-06 04:13:09.94419	\N	\N	f
178	92	请假审批	\N	\N	\N	\N	BUTTON	admin:leave:approve	1	t	ENABLED	f	f	2026-01-06 04:13:09.944811	2026-01-06 04:13:09.944811	\N	\N	f
179	92	假期管理	\N	\N	\N	\N	BUTTON	admin:leave:manage	2	t	ENABLED	f	f	2026-01-06 04:13:09.944811	2026-01-06 04:13:09.944811	\N	\N	f
180	94	资产详情	\N	\N	\N	\N	BUTTON	admin:asset:detail	1	t	ENABLED	f	f	2026-01-06 04:13:09.945093	2026-01-06 04:13:09.945093	\N	\N	f
181	94	资产创建	\N	\N	\N	\N	BUTTON	admin:asset:create	2	t	ENABLED	f	f	2026-01-06 04:13:09.945093	2026-01-06 04:13:09.945093	\N	\N	f
182	94	资产编辑	\N	\N	\N	\N	BUTTON	admin:asset:edit	3	t	ENABLED	f	f	2026-01-06 04:13:09.945093	2026-01-06 04:13:09.945093	\N	\N	f
183	94	资产删除	\N	\N	\N	\N	BUTTON	admin:asset:delete	4	t	ENABLED	f	f	2026-01-06 04:13:09.945093	2026-01-06 04:13:09.945093	\N	\N	f
184	94	资产领用	\N	\N	\N	\N	BUTTON	admin:asset:receive	5	t	ENABLED	f	f	2026-01-06 04:13:09.945093	2026-01-06 04:13:09.945093	\N	\N	f
185	94	资产归还	\N	\N	\N	\N	BUTTON	admin:asset:return	6	t	ENABLED	f	f	2026-01-06 04:13:09.945093	2026-01-06 04:13:09.945093	\N	\N	f
186	94	资产报废	\N	\N	\N	\N	BUTTON	admin:asset:scrap	7	t	ENABLED	f	f	2026-01-06 04:13:09.945093	2026-01-06 04:13:09.945093	\N	\N	f
187	95	采购详情	\N	\N	\N	\N	BUTTON	admin:purchase:detail	1	t	ENABLED	f	f	2026-01-06 04:13:09.945586	2026-01-06 04:13:09.945586	\N	\N	f
188	95	采购创建	\N	\N	\N	\N	BUTTON	admin:purchase:create	2	t	ENABLED	f	f	2026-01-06 04:13:09.945586	2026-01-06 04:13:09.945586	\N	\N	f
189	95	采购编辑	\N	\N	\N	\N	BUTTON	admin:purchase:edit	3	t	ENABLED	f	f	2026-01-06 04:13:09.945586	2026-01-06 04:13:09.945586	\N	\N	f
190	95	采购审批	\N	\N	\N	\N	BUTTON	admin:purchase:approve	4	t	ENABLED	f	f	2026-01-06 04:13:09.945586	2026-01-06 04:13:09.945586	\N	\N	f
191	95	采购入库	\N	\N	\N	\N	BUTTON	admin:purchase:receive	5	t	ENABLED	f	f	2026-01-06 04:13:09.945586	2026-01-06 04:13:09.945586	\N	\N	f
192	96	供应商详情	\N	\N	\N	\N	BUTTON	admin:supplier:detail	1	t	ENABLED	f	f	2026-01-06 04:13:09.945853	2026-01-06 04:13:09.945853	\N	\N	f
193	96	供应商创建	\N	\N	\N	\N	BUTTON	admin:supplier:create	2	t	ENABLED	f	f	2026-01-06 04:13:09.945853	2026-01-06 04:13:09.945853	\N	\N	f
194	96	供应商编辑	\N	\N	\N	\N	BUTTON	admin:supplier:edit	3	t	ENABLED	f	f	2026-01-06 04:13:09.945853	2026-01-06 04:13:09.945853	\N	\N	f
195	96	供应商删除	\N	\N	\N	\N	BUTTON	admin:supplier:delete	4	t	ENABLED	f	f	2026-01-06 04:13:09.945853	2026-01-06 04:13:09.945853	\N	\N	f
196	121	合同导出	\N	\N	\N	\N	BUTTON	admin:contract:export	1	t	ENABLED	f	f	2026-01-06 04:13:09.94617	2026-01-06 04:13:09.94617	\N	\N	f
197	91	考勤记录	\N	\N	\N	\N	BUTTON	admin:attendance:record	1	t	ENABLED	f	f	2026-01-06 04:13:09.946385	2026-01-06 04:13:09.946385	\N	\N	f
201	200	报表查看	\N	\N	\N	\N	BUTTON	report:list	1	t	ENABLED	f	t	2026-01-06 14:00:00	2026-01-06 14:00:00	\N	\N	f
202	200	报表详情	\N	\N	\N	\N	BUTTON	report:detail	2	t	ENABLED	f	t	2026-01-06 14:00:00	2026-01-06 14:00:00	\N	\N	f
203	200	生成报表	\N	\N	\N	\N	BUTTON	report:generate	3	t	ENABLED	f	t	2026-01-06 14:00:00	2026-01-06 14:00:00	\N	\N	f
204	200	下载报表	\N	\N	\N	\N	BUTTON	report:download	4	t	ENABLED	f	t	2026-01-06 14:00:00	2026-01-06 14:00:00	\N	\N	f
205	200	删除报表	\N	\N	\N	\N	BUTTON	report:delete	5	t	ENABLED	f	t	2026-01-06 14:00:00	2026-01-06 14:00:00	\N	\N	f
210	107	创建工资表	\N	\N	\N	\N	BUTTON	payroll:create	1	t	ENABLED	f	t	2026-01-06 07:38:37.94416	2026-01-06 07:38:37.94416	\N	\N	f
211	107	查看工资表	\N	\N	\N	\N	BUTTON	payroll:view	2	t	ENABLED	f	t	2026-01-06 07:38:37.94416	2026-01-06 07:38:37.94416	\N	\N	f
212	107	编辑工资表	\N	\N	\N	\N	BUTTON	payroll:edit	3	t	ENABLED	f	t	2026-01-06 07:38:37.94416	2026-01-06 07:38:37.94416	\N	\N	f
213	107	提交工资表	\N	\N	\N	\N	BUTTON	payroll:submit	4	t	ENABLED	f	t	2026-01-06 07:38:37.94416	2026-01-06 07:38:37.94416	\N	\N	f
214	107	财务确认	\N	\N	\N	\N	BUTTON	payroll:finance:confirm	5	t	ENABLED	f	t	2026-01-06 07:38:37.94416	2026-01-06 07:38:37.94416	\N	\N	f
215	107	发放工资	\N	\N	\N	\N	BUTTON	payroll:issue	6	t	ENABLED	f	t	2026-01-06 07:38:37.94416	2026-01-06 07:38:37.94416	\N	\N	f
216	108	确认工资	\N	\N	\N	\N	BUTTON	payroll:confirm	1	t	ENABLED	f	t	2026-01-06 07:38:37.94416	2026-01-06 07:38:37.94416	\N	\N	f
152	2	期限列表	\N	\N	\N	\N	BUTTON	deadline:list	11	t	ENABLED	f	t	2026-01-06 04:02:22.669342	2026-01-06 11:40:43.86229	\N	\N	f
153	2	期限查看	\N	\N	\N	\N	BUTTON	deadline:view	12	t	ENABLED	f	t	2026-01-06 04:02:22.669621	2026-01-06 11:40:43.86229	\N	\N	f
154	2	期限创建	\N	\N	\N	\N	BUTTON	deadline:create	13	t	ENABLED	f	t	2026-01-06 04:02:22.669923	2026-01-06 11:40:43.86229	\N	\N	f
155	2	期限编辑	\N	\N	\N	\N	BUTTON	deadline:edit	14	t	ENABLED	f	t	2026-01-06 04:02:22.670256	2026-01-06 11:40:43.86229	\N	\N	f
200	0	报表中心	/workbench/report	workbench/report/index	\N	BarChartOutlined	MENU	report:list	12	t	ENABLED	f	t	2026-01-06 14:00:00	2026-01-06 14:00:00	\N	\N	f
23	2	部门管理	/system/dept	system/dept/index	\N	ApartmentOutlined	MENU	sys:dept:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-06 11:40:43.86229	\N	\N	f
24	2	菜单管理	/system/menu	system/menu/index	\N	MenuOutlined	MENU	sys:menu:list	4	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-06 11:40:43.86229	\N	\N	f
198	2	权限矩阵	/system/permission-matrix	system/permission-matrix/index	\N	TableOutlined	MENU	sys:role:list	5	t	ENABLED	f	t	2026-01-06 11:40:43.855721	2026-01-06 11:40:43.855721	\N	\N	f
156	2	期限删除	\N	\N	\N	\N	BUTTON	deadline:delete	15	t	ENABLED	f	t	2026-01-06 04:02:22.67069	2026-01-06 11:40:43.86229	\N	\N	f
703	207	查询交接	\N	\N	\N	\N	BUTTON	sys:handover:list	0	t	ENABLED	f	t	2026-01-06 12:36:38.137972	2026-01-06 12:36:38.137972	\N	\N	f
704	207	创建交接	\N	\N	\N	\N	BUTTON	sys:handover:create	1	t	ENABLED	f	t	2026-01-06 12:36:38.137972	2026-01-06 12:36:38.137972	\N	\N	f
705	207	查看交接	\N	\N	\N	\N	BUTTON	sys:handover:view	2	t	ENABLED	f	t	2026-01-06 12:36:38.137972	2026-01-06 12:36:38.137972	\N	\N	f
706	207	确认交接	\N	\N	\N	\N	BUTTON	sys:handover:confirm	3	t	ENABLED	f	t	2026-01-06 12:36:38.137972	2026-01-06 12:36:38.137972	\N	\N	f
707	207	取消交接	\N	\N	\N	\N	BUTTON	sys:handover:cancel	4	t	ENABLED	f	t	2026-01-06 12:36:38.137972	2026-01-06 12:36:38.137972	\N	\N	f
22	2	角色管理	/system/role	system/role/index	\N	SafetyCertificateOutlined	MENU	sys:role:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
217	41	查看项目详情	\N	\N	\N	\N	BUTTON	matter:view	1	t	ENABLED	f	f	2026-01-07 10:15:16.51803	2026-01-07 10:15:31.760307	\N	\N	f
220	41	项目查看	\N	\N	\N	\N	BUTTON	matter:view	1	t	ENABLED	f	t	2026-01-08 00:17:54.280133	2026-01-08 00:17:54.280133	\N	\N	f
221	41	项目创建	\N	\N	\N	\N	BUTTON	matter:create	2	t	ENABLED	f	t	2026-01-08 00:17:54.287088	2026-01-08 00:17:54.287088	\N	\N	f
222	41	项目编辑	\N	\N	\N	\N	BUTTON	matter:update	3	t	ENABLED	f	t	2026-01-08 00:17:54.288127	2026-01-08 00:17:54.288127	\N	\N	f
223	41	申请结案	\N	\N	\N	\N	BUTTON	matter:close	4	t	ENABLED	f	t	2026-01-08 00:17:54.290683	2026-01-08 00:17:54.290683	\N	\N	f
224	41	审批结案	\N	\N	\N	\N	BUTTON	matter:approve	5	t	ENABLED	f	t	2026-01-08 00:17:54.29157	2026-01-08 00:17:54.29157	\N	\N	f
711	62	生成文档			\N		BUTTON	doc:template:generate	4	t	ENABLED	f	f	2026-01-08 01:39:44.902905	2026-01-08 01:39:44.902905	\N	\N	f
712	62	预览模板			\N		BUTTON	doc:template:view	5	t	ENABLED	f	f	2026-01-08 01:39:44.903307	2026-01-08 01:39:44.903307	\N	\N	f
710	62	使用模板			\N		BUTTON	doc:template:use	3	t	ENABLED	f	f	2026-01-08 01:39:44.902589	2026-01-08 01:39:44.902589	\N	\N	f
708	713	查看详情			\N		BUTTON	doc:template:detail	1	t	ENABLED	f	f	2026-01-08 01:39:44.896302	2026-01-08 01:39:44.896302	\N	\N	f
709	713	管理模板			\N		BUTTON	doc:template:manage	2	t	ENABLED	f	f	2026-01-08 01:39:44.902169	2026-01-08 01:39:44.902169	\N	\N	f
721	71	入库审批	\N	\N	\N	\N	BUTTON	archive:store:approve	2	t	ENABLED	f	f	2026-01-08 02:56:33.628793	2026-01-08 02:56:33.628793	\N	\N	f
722	71	申请迁移	\N	\N	\N	\N	BUTTON	archive:migrate:apply	3	t	ENABLED	f	f	2026-01-08 02:56:33.632876	2026-01-08 02:56:33.632876	\N	\N	f
723	71	迁移审批	\N	\N	\N	\N	BUTTON	archive:migrate:approve	4	t	ENABLED	f	f	2026-01-08 02:56:33.633109	2026-01-08 02:56:33.633109	\N	\N	f
218	120	出函审批			\N	\N	BUTTON	admin:letter:approve	1	t	ENABLED	f	t	2026-01-08 03:26:38.462763	2026-01-08 03:26:38.462763	\N	\N	f
219	120	出函打印			\N	\N	BUTTON	admin:letter:print	2	t	ENABLED	f	t	2026-01-08 03:26:38.464226	2026-01-08 03:26:38.464226	\N	\N	f
11	0	知识库	/knowledge	LAYOUT	\N	BookOutlined	DIRECTORY	\N	11	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
207	0	数据交接	/data-handover	data-handover/index	\N	ant-design:swap-outlined	MENU	\N	13	t	ENABLED	f	t	2026-01-06 12:36:22.065598	2026-01-06 12:36:22.065598	\N	\N	f
2	0	系统管理	/system	LAYOUT	\N	SettingOutlined	DIRECTORY	\N	99	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
21	2	用户管理	/system/user	system/user/index	\N	UserOutlined	MENU	sys:user:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
25	2	系统配置	/system/config	system/config/index	\N	ToolOutlined	MENU	sys:config:list	6	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-06 11:40:43.86229	\N	\N	f
26	2	操作日志	/system/log	system/log/index	\N	FileSearchOutlined	MENU	sys:log:list	7	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-06 11:40:43.86229	\N	\N	f
27	2	出函模板	/system/letter-template	system/letter-template/index	\N	MailOutlined	MENU	sys:letter-template:list	10	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-06 11:40:43.86229	\N	\N	f
28	2	合同模板	/system/contract-template	system/contract-template/index	\N	FileProtectOutlined	MENU	sys:contract-template:list	11	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-06 11:40:43.86229	\N	\N	f
140	2	提成规则配置	/system/commission-config	system/commission-config/index	\N	ant-design:percentage-outlined	MENU	system:config:list	12	t	ENABLED	f	t	2026-01-05 18:00:00	2026-01-06 11:40:43.86229	\N	\N	f
159	2	外部系统集成	/system/integration	system/integration/index	\N	CloudOutlined	MENU	system:integration:list	99	t	ENABLED	f	t	2026-01-08 04:48:25.443502	2026-01-08 04:48:25.443502	\N	\N	f
41	4	项目列表	/matter/list	matter/list/index	\N	ProjectOutlined	MENU	matter:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
42	4	我的项目	/matter/my	matter/my/index	\N	SolutionOutlined	MENU	matter:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
45	4	合同管理	/matter/contract	matter/contract/index	\N	FileProtectOutlined	MENU	matter:contract:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
44	4	任务管理	/matter/task	matter/task/index	\N	CheckSquareOutlined	MENU	task:manage	5	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
43	4	工时管理	/matter/timesheet	matter/timesheet/index	\N	ClockCircleOutlined	MENU	timesheet:record	6	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
57	5	我的收款	/finance/my-payment	finance/my-payment/index	\N	WalletOutlined	MENU	finance:my:payment	1	t	ENABLED	f	t	2026-01-05 16:00:00	2026-01-05 16:00:00	\N	\N	f
58	5	我的提成	/finance/my-commission	finance/my-commission/index	\N	DollarOutlined	MENU	finance:my:commission	2	t	ENABLED	f	t	2026-01-05 16:00:00	2026-01-05 16:00:00	\N	\N	f
108	5	我的工资	/hr/payroll/my	hr/payroll/my/index	\N	WalletOutlined	MENU	payroll:my:view	3	t	ENABLED	f	t	2026-01-06 07:30:04.370342	2026-01-06 07:30:04.370342	\N	\N	f
59	5	费用报销	/finance/expense	finance/expense/index	\N	AccountBookOutlined	MENU	finance:expense:apply	4	t	ENABLED	f	t	2026-01-05 16:00:00	2026-01-05 16:00:00	\N	\N	f
51	5	合同收款概览	/finance/contract	finance/contract/index	\N	FileProtectOutlined	MENU	finance:contract:view	10	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
52	5	收款管理	/finance/payment	finance/payment/index	\N	PayCircleOutlined	MENU	finance:payment:manage	11	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
56	5	收款变更审批	/finance/payment-amendment	finance/payment-amendment/index	\N	AuditOutlined	MENU	fee:amendment:list	12	t	ENABLED	f	t	2026-01-05 02:19:31.876174	2026-01-05 02:19:31.876174	\N	\N	f
53	5	提成管理	/finance/commission	finance/commission/index	\N	PercentageOutlined	MENU	finance:commission:manage	13	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
107	5	工资管理	/hr/payroll	hr/payroll/index	\N	DollarOutlined	MENU	payroll:list	14	t	ENABLED	f	t	2026-01-06 07:30:04.370342	2026-01-06 07:30:04.370342	\N	\N	f
54	5	发票管理	/finance/invoice	finance/invoice/index	\N	ProfileOutlined	MENU	finance:invoice:manage	15	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
123	5	合同变更处理	/finance/contract-amendment	/finance/contract-amendment/index	\N	ant-design:diff-outlined	MENU	finance:contract:amendment:view	16	t	ENABLED	f	t	2026-01-05 03:55:33.410601	2026-01-05 03:55:33.410601	\N	\N	f
55	5	财务报表	/finance/report	finance/report/index	\N	BarChartOutlined	MENU	finance:report:view	20	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
714	6	我的文书	/document/my	document/my/index	\N	FileTextOutlined	MENU	doc:my:list	1	t	ENABLED	f	t	2026-01-08 02:15:45.865748	2026-01-08 02:15:45.865748	\N	\N	f
61	6	卷宗列表	/document/list	document/list/index	\N	FileOutlined	MENU	doc:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.89735	2026-01-08 01:43:37.711362	\N	\N	f
62	6	文书制作	/document/compose	document/compose/index	\N	FileAddOutlined	MENU	doc:compose	3	t	ENABLED	f	t	2026-01-04 16:14:15.89735	2026-01-08 01:43:37.700594	\N	\N	f
713	6	模板管理	/document/template	document/template/index	\N	SettingOutlined	MENU	doc:template:manage	4	t	ENABLED	f	t	2026-01-08 01:43:37.711744	2026-01-08 01:43:37.711744	\N	\N	f
91	9	考勤管理	/admin/attendance	admin/attendance/index	\N	ClockCircleOutlined	MENU	admin:attendance:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
92	9	请假管理	/admin/leave	admin/leave/index	\N	CalendarOutlined	MENU	admin:leave:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
97	9	加班管理	/admin/overtime	admin/overtime/index	\N	ClockCircleOutlined	MENU	admin:overtime:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
99	9	外出管理	/admin/go-out	admin/go-out/index	\N	ExportOutlined	MENU	admin:goout:list	4	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
93	9	会议室预约	/admin/meeting-room	admin/meeting-room/index	\N	ScheduleOutlined	MENU	admin:meeting:list	5	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
100	9	会议记录	/admin/meeting-record	admin/meeting-record/index	\N	FileTextOutlined	MENU	admin:meeting-record:list	6	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
120	9	出函管理	/admin/letter	admin/letter/index	\N	MailOutlined	MENU	admin:letter:list	10	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
63	9	印章管理	/document/seal	document/seal/index	\N	SafetyOutlined	MENU	doc:seal:list	11	t	ENABLED	f	t	2026-01-04 16:14:15.89735	2026-01-07 08:31:53.378975	\N	\N	f
64	9	用印申请	/document/seal-apply	document/seal-apply/index	\N	FormOutlined	MENU	doc:seal:apply	12	t	ENABLED	f	t	2026-01-04 16:14:15.89735	2026-01-07 08:31:53.383059	\N	\N	f
94	9	资产管理	/admin/asset	admin/asset/index	\N	ShopOutlined	MENU	admin:asset:list	20	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
98	9	资产盘点	/admin/asset-inventory	admin/asset-inventory/index	\N	FileSearchOutlined	MENU	admin:asset-inventory:list	21	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
95	9	采购管理	/admin/purchase	admin/purchase/index	\N	ShoppingCartOutlined	MENU	admin:purchase:list	22	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
96	9	供应商管理	/admin/supplier	admin/supplier/index	\N	ShopOutlined	MENU	admin:supplier:list	23	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
121	9	合同查询	/admin/contract	admin/contract/index	\N	FileSearchOutlined	MENU	admin:contract:list	30	t	ENABLED	f	t	2026-01-05 02:19:57.79315	2026-01-05 02:19:57.79315	\N	\N	f
109	10	员工档案	/hr/employee	hr/employee/index	\N	UserOutlined	MENU	hr:employee:list	1	t	ENABLED	f	t	2026-01-06 07:30:04.370342	2026-01-06 07:30:04.370342	\N	\N	f
101	10	培训管理	/hr/training	hr/training/index	\N	ReadOutlined	MENU	hr:training:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
102	10	绩效考核	/hr/performance	hr/performance/index	\N	TrophyOutlined	MENU	hr:performance:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
105	10	转正管理	/hr/regularization	hr/regularization/index	\N	CheckSquareOutlined	MENU	hr:regularization:list	4	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
103	10	晋升管理	/hr/promotion	hr/promotion/index	\N	RiseOutlined	MENU	hr:promotion:list	5	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
104	10	发展计划	/hr/development	hr/development/index	\N	AimOutlined	MENU	hr:development:list	6	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
106	10	离职管理	/hr/resignation	hr/resignation/index	\N	ExportOutlined	MENU	hr:resignation:list	7	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
\.


--
-- Data for Name: sys_role; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) FROM stdin;
2	DIRECTOR	律所主任	律所管理层，可查看全所数据，审批重要事项	ALL	ACTIVE	2	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
9	TRAINEE	实习律师	实习人员，有限权限	SELF	ACTIVE	9	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
3	TEAM_LEADER	团队负责人	团队负责人，可查看本团队数据，负责团队业务管理	DEPT_AND_CHILD	ACTIVE	3	2026-01-04 16:14:15.892579	2026-01-07 13:46:52.130375	\N	\N	f
6	LAWYER	律师	执业律师，处理案件的主要人员	SELF	ACTIVE	6	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
5	FINANCE	财务	财务人员，管理律所财务工作	ALL	ACTIVE	5	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
1	ADMIN	管理员	系统最高权限，可管理所有功能和数据	ALL	ACTIVE	1	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
8	ADMIN_STAFF	行政	行政后勤人员，管理行政事务	ALL	ACTIVE	8	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
\.


--
-- Data for Name: sys_role_menu; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_role_menu (id, role_id, menu_id, created_at) FROM stdin;
1574	3	6	2026-01-07 13:47:00.144537
1148	1	207	2026-01-06 12:36:46.493148
1149	2	207	2026-01-06 12:36:46.493148
1150	1	703	2026-01-06 12:36:46.493148
1151	2	703	2026-01-06 12:36:46.493148
1152	1	704	2026-01-06 12:36:46.493148
1153	2	704	2026-01-06 12:36:46.493148
1154	1	705	2026-01-06 12:36:46.493148
1155	2	705	2026-01-06 12:36:46.493148
1156	1	706	2026-01-06 12:36:46.493148
1157	2	706	2026-01-06 12:36:46.493148
1158	1	707	2026-01-06 12:36:46.493148
1159	2	707	2026-01-06 12:36:46.493148
1575	3	61	2026-01-07 13:47:00.149894
1577	3	8	2026-01-07 13:47:00.150649
1578	3	81	2026-01-07 13:47:00.150943
1579	3	82	2026-01-07 13:47:00.151697
1581	3	11	2026-01-07 13:47:00.153751
1582	3	111	2026-01-07 13:47:00.15413
1837	5	94	2026-01-07 14:45:54.642647
1584	3	113	2026-01-07 13:47:00.161577
1838	5	181	2026-01-07 14:45:54.642647
1585	3	200	2026-01-07 13:47:00.162324
1586	3	201	2026-01-07 13:47:00.162978
1235	6	9	2026-01-07 08:37:13.091367
1839	5	182	2026-01-07 14:45:54.642647
1237	9	9	2026-01-07 08:37:13.091367
1239	2	217	2026-01-07 10:15:31.763673
1840	5	183	2026-01-07 14:45:54.642647
1587	3	202	2026-01-07 13:47:00.163398
1242	6	217	2026-01-07 10:15:31.763673
1841	5	186	2026-01-07 14:45:54.642647
1588	3	203	2026-01-07 13:47:00.163829
1307	8	98	2026-01-07 18:46:46.060222
1308	8	99	2026-01-07 18:46:46.060222
1309	8	11	2026-01-07 18:46:46.060222
1310	8	100	2026-01-07 18:46:46.060222
1311	8	120	2026-01-07 18:46:46.060222
1312	8	121	2026-01-07 18:46:46.060222
1313	8	63	2026-01-07 18:46:46.060222
1314	8	108	2026-01-07 18:46:46.060222
1315	8	64	2026-01-07 18:46:46.060222
1316	8	207	2026-01-07 18:46:46.060222
1317	8	109	2026-01-07 18:46:46.060222
1589	3	204	2026-01-07 13:47:00.164299
1590	3	205	2026-01-07 13:47:00.165228
1864	8	28	2026-01-07 15:33:55.538518
1865	5	2	2026-01-07 15:33:55.539569
1866	5	28	2026-01-07 15:33:55.539569
1867	5	140	2026-01-07 15:33:55.539569
1961	2	721	2026-01-08 02:56:33.633347
1962	2	722	2026-01-08 02:56:33.63569
1963	2	723	2026-01-08 02:56:33.635984
1964	1	721	2026-01-08 02:56:33.636189
59	2	1	2026-01-04 16:14:15.900075
61	2	3	2026-01-04 16:14:15.900075
62	2	4	2026-01-04 16:14:15.900075
63	2	5	2026-01-04 16:14:15.900075
64	2	6	2026-01-04 16:14:15.900075
65	2	8	2026-01-04 16:14:15.900075
66	2	9	2026-01-04 16:14:15.900075
67	2	10	2026-01-04 16:14:15.900075
68	2	11	2026-01-04 16:14:15.900075
71	2	23	2026-01-04 16:14:15.900075
75	2	27	2026-01-04 16:14:15.900075
76	2	28	2026-01-04 16:14:15.900075
77	2	31	2026-01-04 16:14:15.900075
78	2	32	2026-01-04 16:14:15.900075
79	2	33	2026-01-04 16:14:15.900075
80	2	41	2026-01-04 16:14:15.900075
81	2	42	2026-01-04 16:14:15.900075
82	2	45	2026-01-04 16:14:15.900075
83	2	43	2026-01-04 16:14:15.900075
84	2	44	2026-01-04 16:14:15.900075
85	2	51	2026-01-04 16:14:15.900075
86	2	52	2026-01-04 16:14:15.900075
87	2	53	2026-01-04 16:14:15.900075
88	2	54	2026-01-04 16:14:15.900075
89	2	55	2026-01-04 16:14:15.900075
316	2	61	2026-01-05 17:00:00
318	2	63	2026-01-05 17:00:00
319	2	64	2026-01-05 17:00:00
320	2	81	2026-01-05 17:00:00
321	2	82	2026-01-05 17:00:00
323	2	91	2026-01-05 17:00:00
324	2	92	2026-01-05 17:00:00
325	2	93	2026-01-05 17:00:00
326	2	94	2026-01-05 17:00:00
327	2	95	2026-01-05 17:00:00
328	2	96	2026-01-05 17:00:00
329	2	97	2026-01-05 17:00:00
330	2	98	2026-01-05 17:00:00
331	2	99	2026-01-05 17:00:00
332	2	100	2026-01-05 17:00:00
333	2	101	2026-01-05 17:00:00
334	2	102	2026-01-05 17:00:00
335	2	103	2026-01-05 17:00:00
336	2	104	2026-01-05 17:00:00
337	2	111	2026-01-05 17:00:00
338	2	112	2026-01-05 17:00:00
339	2	113	2026-01-05 17:00:00
340	2	123	2026-01-05 17:00:00
1965	1	722	2026-01-08 02:56:33.636733
1966	1	723	2026-01-08 02:56:33.636959
114	6	1	2026-01-04 16:14:15.901047
115	6	3	2026-01-04 16:14:15.901047
116	6	4	2026-01-04 16:14:15.901047
117	6	6	2026-01-04 16:14:15.901047
118	6	8	2026-01-04 16:14:15.901047
119	6	11	2026-01-04 16:14:15.901047
120	6	31	2026-01-04 16:14:15.901047
121	6	32	2026-01-04 16:14:15.901047
122	6	33	2026-01-04 16:14:15.901047
123	6	41	2026-01-04 16:14:15.901047
124	6	42	2026-01-04 16:14:15.901047
125	6	45	2026-01-04 16:14:15.901047
126	6	43	2026-01-04 16:14:15.901047
127	6	44	2026-01-04 16:14:15.901047
128	6	61	2026-01-04 16:14:15.901047
1591	6	200	2026-01-07 13:47:44.060444
131	6	64	2026-01-04 16:14:15.901047
132	6	81	2026-01-04 16:14:15.901047
133	6	82	2026-01-04 16:14:15.901047
135	6	111	2026-01-04 16:14:15.901047
136	6	112	2026-01-04 16:14:15.901047
137	6	113	2026-01-04 16:14:15.901047
1592	6	201	2026-01-07 13:47:44.064594
1593	6	202	2026-01-07 13:47:44.064983
1594	6	203	2026-01-07 13:47:44.065269
1595	6	204	2026-01-07 13:47:44.065553
1596	6	205	2026-01-07 13:47:44.06585
1842	5	95	2026-01-07 14:45:54.697523
1843	5	189	2026-01-07 14:45:54.697523
1844	5	190	2026-01-07 14:45:54.697523
1845	5	191	2026-01-07 14:45:54.697523
1868	1	220	2026-01-08 00:17:54.292355
1869	1	221	2026-01-08 00:17:54.295108
1603	9	200	2026-01-07 13:47:44.067433
1604	9	201	2026-01-07 13:47:44.068091
151	2	105	2026-01-04 16:14:18.541755
152	2	106	2026-01-04 16:14:18.541755
1605	9	202	2026-01-07 13:47:44.068291
1606	9	203	2026-01-07 13:47:44.068755
1607	9	204	2026-01-07 13:47:44.069023
1870	1	222	2026-01-08 00:17:54.295966
157	2	120	2026-01-05 02:19:31.880543
158	2	56	2026-01-05 02:19:31.880861
1608	9	205	2026-01-07 13:47:44.069215
1871	1	223	2026-01-08 00:17:54.296795
1872	1	224	2026-01-08 00:17:54.29768
163	2	121	2026-01-05 02:19:57.79819
1873	2	220	2026-01-08 00:17:54.298891
168	2	130	2026-01-05 10:00:00
169	2	131	2026-01-05 10:00:00
170	2	132	2026-01-05 10:00:00
1874	2	221	2026-01-08 00:17:54.299588
1875	2	222	2026-01-08 00:17:54.300354
1876	2	223	2026-01-08 00:17:54.301257
174	6	5	2026-01-05 16:00:00
175	6	57	2026-01-05 16:00:00
176	6	58	2026-01-05 16:00:00
177	6	59	2026-01-05 16:00:00
1877	2	224	2026-01-08 00:17:54.303514
1878	3	220	2026-01-08 00:17:54.305211
1879	3	221	2026-01-08 00:17:54.307174
1880	3	222	2026-01-08 00:17:54.308739
182	9	5	2026-01-05 16:00:00
183	9	57	2026-01-05 16:00:00
184	9	58	2026-01-05 16:00:00
185	9	59	2026-01-05 16:00:00
1881	3	223	2026-01-08 00:17:54.309472
1882	3	224	2026-01-08 00:17:54.310188
1883	6	220	2026-01-08 00:17:54.310829
1884	6	221	2026-01-08 00:17:54.311648
1885	6	222	2026-01-08 00:17:54.312284
1886	6	223	2026-01-08 00:17:54.312933
1887	9	220	2026-01-08 00:17:54.313535
1888	9	222	2026-01-08 00:17:54.314448
1967	3	91	2026-01-08 03:26:38.443447
1968	5	91	2026-01-08 03:26:38.447877
1969	6	91	2026-01-08 03:26:38.448744
1970	9	91	2026-01-08 03:26:38.449029
1971	3	92	2026-01-08 03:26:38.449666
1972	5	92	2026-01-08 03:26:38.449886
1973	6	92	2026-01-08 03:26:38.450138
1974	9	92	2026-01-08 03:26:38.450368
1975	3	93	2026-01-08 03:26:38.450576
1976	5	93	2026-01-08 03:26:38.450775
1977	6	93	2026-01-08 03:26:38.451006
1978	9	93	2026-01-08 03:26:38.451215
1979	3	97	2026-01-08 03:26:38.451427
1980	6	97	2026-01-08 03:26:38.451604
1981	9	97	2026-01-08 03:26:38.451809
1982	3	99	2026-01-08 03:26:38.452024
1983	6	99	2026-01-08 03:26:38.452242
1984	9	99	2026-01-08 03:26:38.452437
1988	3	176	2026-01-08 03:26:38.464721
1990	6	176	2026-01-08 03:26:38.464721
1992	9	176	2026-01-08 03:26:38.464721
1995	3	177	2026-01-08 03:26:38.464721
1998	3	169	2026-01-08 03:26:38.464721
2000	6	169	2026-01-08 03:26:38.464721
2002	9	169	2026-01-08 03:26:38.464721
2005	3	178	2026-01-08 03:26:38.464721
2006	1	218	2026-01-08 03:26:38.464721
2007	2	218	2026-01-08 03:26:38.464721
2008	3	218	2026-01-08 03:26:38.464721
2009	1	219	2026-01-08 03:26:38.464721
2010	2	219	2026-01-08 03:26:38.464721
2011	8	219	2026-01-08 03:26:38.464721
1846	5	96	2026-01-07 14:45:54.753875
1847	5	193	2026-01-07 14:45:54.753875
1848	5	194	2026-01-07 14:45:54.753875
1609	5	200	2026-01-07 13:52:56.387645
1849	5	195	2026-01-07 14:45:54.753875
1889	5	130	2026-01-08 01:20:27.134532
1890	5	131	2026-01-08 01:20:27.326253
1891	6	130	2026-01-08 01:20:27.332915
1893	9	130	2026-01-08 01:20:27.334083
2012	1	159	2026-01-08 04:48:25.444156
1610	5	201	2026-01-07 13:52:56.393525
1499	5	703	2026-01-07 18:49:49.999575
1500	5	1	2026-01-07 18:49:49.999575
198	2	57	2026-01-05 16:00:00
199	2	58	2026-01-05 16:00:00
200	2	59	2026-01-05 16:00:00
1611	5	202	2026-01-07 13:52:56.393882
1612	5	203	2026-01-07 13:52:56.394145
1613	5	204	2026-01-07 13:52:56.394395
231	9	1	2026-01-05 17:00:00
232	9	3	2026-01-05 17:00:00
233	9	4	2026-01-05 17:00:00
234	9	6	2026-01-05 17:00:00
235	9	11	2026-01-05 17:00:00
236	9	31	2026-01-05 17:00:00
237	9	32	2026-01-05 17:00:00
238	9	42	2026-01-05 17:00:00
239	9	43	2026-01-05 17:00:00
240	9	44	2026-01-05 17:00:00
241	9	61	2026-01-05 17:00:00
243	9	111	2026-01-05 17:00:00
244	9	112	2026-01-05 17:00:00
245	9	113	2026-01-05 17:00:00
1505	5	81	2026-01-07 18:49:49.999575
349	2	140	2026-01-05 18:00:00
354	2	145	2026-01-05 18:00:00
355	2	146	2026-01-05 18:00:00
356	2	147	2026-01-05 18:00:00
1614	5	205	2026-01-07 13:52:56.394631
1506	5	111	2026-01-07 18:49:49.999575
1507	5	150	2026-01-07 18:49:49.999575
363	2	701	2026-01-05 15:01:30.641063
366	6	701	2026-01-05 15:01:30.641063
1510	5	168	2026-01-07 18:49:49.999575
1512	5	175	2026-01-07 18:49:49.999575
1513	5	180	2026-01-07 18:49:49.999575
371	2	148	2026-01-06 01:27:28.877809
1514	5	187	2026-01-07 18:49:49.999575
375	6	148	2026-01-06 01:27:28.877809
1515	5	192	2026-01-07 18:49:49.999575
1516	5	210	2026-01-07 18:49:49.999575
1517	5	216	2026-01-07 18:49:49.999575
1519	5	704	2026-01-07 18:49:49.999575
1523	5	82	2026-01-07 18:49:49.999575
386	2	149	2026-01-06 04:02:22.676255
387	2	150	2026-01-06 04:02:22.676973
388	2	151	2026-01-06 04:02:22.677166
1524	5	112	2026-01-07 18:49:49.999575
1525	5	149	2026-01-07 18:49:49.999575
1526	5	151	2026-01-07 18:49:49.999575
1528	5	169	2026-01-07 18:49:49.999575
1529	5	176	2026-01-07 18:49:49.999575
1530	5	188	2026-01-07 18:49:49.999575
1531	5	211	2026-01-07 18:49:49.999575
418	6	149	2026-01-06 04:02:22.683511
419	6	150	2026-01-06 04:02:22.68383
1850	5	97	2026-01-07 14:45:54.807937
1409	3	703	2026-01-07 18:49:28.743814
1410	3	1	2026-01-07 18:49:28.743814
1412	3	31	2026-01-07 18:49:28.743814
1413	3	41	2026-01-07 18:49:28.743814
1414	3	57	2026-01-07 18:49:28.743814
1415	3	130	2026-01-07 18:49:28.743814
433	2	161	2026-01-06 04:02:27.652068
434	2	162	2026-01-06 04:02:27.652299
435	2	163	2026-01-06 04:02:27.652629
436	2	164	2026-01-06 04:02:27.652819
437	2	165	2026-01-06 04:02:27.65301
1416	3	132	2026-01-07 18:49:28.743814
1417	3	150	2026-01-07 18:49:28.743814
1418	3	161	2026-01-07 18:49:28.743814
1419	3	163	2026-01-07 18:49:28.743814
452	6	161	2026-01-06 04:02:27.656649
453	6	162	2026-01-06 04:02:27.656796
454	6	163	2026-01-06 04:02:27.656966
455	6	164	2026-01-06 04:02:27.657131
1428	3	196	2026-01-07 18:49:28.743814
1429	3	210	2026-01-07 18:49:28.743814
1430	3	216	2026-01-07 18:49:28.743814
1431	3	217	2026-01-07 18:49:28.743814
1432	3	704	2026-01-07 18:49:28.743814
1433	3	32	2026-01-07 18:49:28.743814
1434	3	42	2026-01-07 18:49:28.743814
1435	3	58	2026-01-07 18:49:28.743814
1436	3	112	2026-01-07 18:49:28.743814
1437	3	131	2026-01-07 18:49:28.743814
1438	3	149	2026-01-07 18:49:28.743814
1439	3	151	2026-01-07 18:49:28.743814
1440	3	162	2026-01-07 18:49:28.743814
1447	3	211	2026-01-07 18:49:28.743814
1448	3	705	2026-01-07 18:49:28.743814
1449	3	3	2026-01-07 18:49:28.743814
1450	3	33	2026-01-07 18:49:28.743814
1451	3	45	2026-01-07 18:49:28.743814
1452	3	59	2026-01-07 18:49:28.743814
1453	3	164	2026-01-07 18:49:28.743814
494	2	174	2026-01-06 04:13:50.996359
495	2	175	2026-01-06 04:13:50.996359
496	2	168	2026-01-06 04:13:50.996359
497	2	169	2026-01-06 04:13:50.996359
498	2	170	2026-01-06 04:13:50.996359
499	2	171	2026-01-06 04:13:50.996359
500	2	172	2026-01-06 04:13:50.996359
501	2	173	2026-01-06 04:13:50.996359
502	2	176	2026-01-06 04:13:50.996359
503	2	177	2026-01-06 04:13:50.996359
504	2	178	2026-01-06 04:13:50.996359
505	2	179	2026-01-06 04:13:50.996359
506	2	180	2026-01-06 04:13:50.996359
507	2	181	2026-01-06 04:13:50.996359
508	2	182	2026-01-06 04:13:50.996359
509	2	183	2026-01-06 04:13:50.996359
510	2	184	2026-01-06 04:13:50.996359
511	2	185	2026-01-06 04:13:50.996359
512	2	186	2026-01-06 04:13:50.996359
513	2	187	2026-01-06 04:13:50.996359
514	2	188	2026-01-06 04:13:50.996359
515	2	189	2026-01-06 04:13:50.996359
516	2	190	2026-01-06 04:13:50.996359
517	2	191	2026-01-06 04:13:50.996359
518	2	192	2026-01-06 04:13:50.996359
519	2	193	2026-01-06 04:13:50.996359
520	2	194	2026-01-06 04:13:50.996359
521	2	195	2026-01-06 04:13:50.996359
522	2	196	2026-01-06 04:13:50.996359
523	2	197	2026-01-06 04:13:50.996359
1459	3	212	2026-01-07 18:49:28.743814
1460	3	706	2026-01-07 18:49:28.743814
1461	3	4	2026-01-07 18:49:28.743814
1462	3	43	2026-01-07 18:49:28.743814
1463	3	148	2026-01-07 18:49:28.743814
1464	3	165	2026-01-07 18:49:28.743814
1466	3	213	2026-01-07 18:49:28.743814
1467	3	707	2026-01-07 18:49:28.743814
1468	3	5	2026-01-07 18:49:28.743814
1469	3	44	2026-01-07 18:49:28.743814
1471	3	145	2026-01-07 18:49:28.743814
1474	3	214	2026-01-07 18:49:28.743814
1475	3	701	2026-01-07 18:49:28.743814
1476	3	56	2026-01-07 18:49:28.743814
1479	3	215	2026-01-07 18:49:28.743814
1481	3	9	2026-01-07 18:49:28.743814
1482	3	51	2026-01-07 18:49:28.743814
1483	3	52	2026-01-07 18:49:28.743814
1484	3	120	2026-01-07 18:49:28.743814
1486	3	53	2026-01-07 18:49:28.743814
1487	3	107	2026-01-07 18:49:28.743814
1490	3	54	2026-01-07 18:49:28.743814
1491	3	108	2026-01-07 18:49:28.743814
1493	3	55	2026-01-07 18:49:28.743814
1852	5	9	2026-01-07 14:45:54.864673
1853	5	99	2026-01-07 14:45:54.864673
1494	3	64	2026-01-07 18:49:28.743814
1497	3	207	2026-01-07 18:49:28.743814
1543	5	213	2026-01-07 18:49:49.999575
1544	5	707	2026-01-07 18:49:49.999575
1545	5	5	2026-01-07 18:49:49.999575
1547	5	145	2026-01-07 18:49:49.999575
1548	5	184	2026-01-07 18:49:49.999575
1549	5	214	2026-01-07 18:49:49.999575
1550	5	701	2026-01-07 18:49:49.999575
1552	5	56	2026-01-07 18:49:49.999575
1553	5	185	2026-01-07 18:49:49.999575
657	2	160	2026-01-06 14:00:00
1554	5	215	2026-01-07 18:49:49.999575
1555	5	8	2026-01-07 18:49:49.999575
1556	5	147	2026-01-07 18:49:49.999575
1557	5	11	2026-01-07 18:49:49.999575
1558	5	51	2026-01-07 18:49:49.999575
1647	6	151	2026-01-07 14:00:35.037142
707	2	200	2026-01-06 14:00:00
708	2	201	2026-01-06 14:00:00
709	2	202	2026-01-06 14:00:00
710	2	203	2026-01-06 14:00:00
711	2	204	2026-01-06 14:00:00
712	2	205	2026-01-06 14:00:00
752	2	107	2026-01-06 07:30:05.776922
756	2	108	2026-01-06 07:30:05.776922
1559	5	52	2026-01-07 18:49:49.999575
760	6	108	2026-01-06 07:30:05.776922
763	9	108	2026-01-06 07:30:05.776922
765	2	109	2026-01-06 07:30:05.776922
1707	5	57	2026-01-07 14:00:35.049064
1709	5	59	2026-01-07 14:00:35.049064
768	2	210	2026-01-06 07:38:37.94416
1732	5	160	2026-01-07 14:00:35.049064
772	2	211	2026-01-06 07:38:37.94416
776	2	212	2026-01-06 07:38:37.94416
780	2	213	2026-01-06 07:38:37.94416
784	2	214	2026-01-06 07:38:37.94416
788	2	215	2026-01-06 07:38:37.94416
792	2	216	2026-01-06 07:38:37.94416
1561	5	53	2026-01-07 18:49:49.999575
796	6	216	2026-01-06 07:38:37.94416
799	9	216	2026-01-06 07:38:37.94416
1769	9	8	2026-01-07 14:13:53.880934
1770	9	33	2026-01-07 14:13:53.880934
1771	9	41	2026-01-07 14:13:53.880934
1772	9	45	2026-01-07 14:13:53.880934
1773	9	81	2026-01-07 14:13:53.880934
1774	9	82	2026-01-07 14:13:53.880934
1776	9	701	2026-01-07 14:13:53.880934
1777	9	148	2026-01-07 14:13:53.880934
1778	9	149	2026-01-07 14:13:53.880934
801	1	109	2026-01-06 19:43:51.724722
802	1	1	2026-01-06 19:43:51.724722
803	1	21	2026-01-06 19:43:51.724722
804	1	31	2026-01-06 19:43:51.724722
805	1	41	2026-01-06 19:43:51.724722
806	1	57	2026-01-06 19:43:51.724722
807	1	61	2026-01-06 19:43:51.724722
808	1	81	2026-01-06 19:43:51.724722
809	1	91	2026-01-06 19:43:51.724722
810	1	101	2026-01-06 19:43:51.724722
811	1	111	2026-01-06 19:43:51.724722
812	1	124	2026-01-06 19:43:51.724722
813	1	150	2026-01-06 19:43:51.724722
814	1	161	2026-01-06 19:43:51.724722
815	1	163	2026-01-06 19:43:51.724722
816	1	168	2026-01-06 19:43:51.724722
817	1	170	2026-01-06 19:43:51.724722
818	1	173	2026-01-06 19:43:51.724722
819	1	174	2026-01-06 19:43:51.724722
820	1	175	2026-01-06 19:43:51.724722
821	1	178	2026-01-06 19:43:51.724722
822	1	180	2026-01-06 19:43:51.724722
823	1	187	2026-01-06 19:43:51.724722
824	1	192	2026-01-06 19:43:51.724722
825	1	196	2026-01-06 19:43:51.724722
826	1	197	2026-01-06 19:43:51.724722
827	1	201	2026-01-06 19:43:51.724722
828	1	210	2026-01-06 19:43:51.724722
829	1	216	2026-01-06 19:43:51.724722
830	1	2	2026-01-06 19:43:51.724722
831	1	22	2026-01-06 19:43:51.724722
832	1	32	2026-01-06 19:43:51.724722
833	1	42	2026-01-06 19:43:51.724722
834	1	58	2026-01-06 19:43:51.724722
836	1	82	2026-01-06 19:43:51.724722
837	1	92	2026-01-06 19:43:51.724722
838	1	102	2026-01-06 19:43:51.724722
839	1	112	2026-01-06 19:43:51.724722
840	1	125	2026-01-06 19:43:51.724722
841	1	149	2026-01-06 19:43:51.724722
842	1	151	2026-01-06 19:43:51.724722
843	1	162	2026-01-06 19:43:51.724722
844	1	169	2026-01-06 19:43:51.724722
845	1	171	2026-01-06 19:43:51.724722
846	1	176	2026-01-06 19:43:51.724722
847	1	179	2026-01-06 19:43:51.724722
848	1	181	2026-01-06 19:43:51.724722
849	1	188	2026-01-06 19:43:51.724722
850	1	193	2026-01-06 19:43:51.724722
851	1	202	2026-01-06 19:43:51.724722
852	1	211	2026-01-06 19:43:51.724722
853	1	3	2026-01-06 19:43:51.724722
854	1	33	2026-01-06 19:43:51.724722
855	1	45	2026-01-06 19:43:51.724722
856	1	59	2026-01-06 19:43:51.724722
857	1	63	2026-01-06 19:43:51.724722
858	1	83	2026-01-06 19:43:51.724722
859	1	93	2026-01-06 19:43:51.724722
860	1	103	2026-01-06 19:43:51.724722
861	1	113	2026-01-06 19:43:51.724722
862	1	164	2026-01-06 19:43:51.724722
863	1	172	2026-01-06 19:43:51.724722
864	1	177	2026-01-06 19:43:51.724722
865	1	182	2026-01-06 19:43:51.724722
866	1	189	2026-01-06 19:43:51.724722
867	1	194	2026-01-06 19:43:51.724722
868	1	198	2026-01-06 19:43:51.724722
869	1	200	2026-01-06 19:43:51.724722
870	1	203	2026-01-06 19:43:51.724722
871	1	212	2026-01-06 19:43:51.724722
872	1	4	2026-01-06 19:43:51.724722
873	1	23	2026-01-06 19:43:51.724722
874	1	43	2026-01-06 19:43:51.724722
875	1	64	2026-01-06 19:43:51.724722
876	1	94	2026-01-06 19:43:51.724722
877	1	104	2026-01-06 19:43:51.724722
878	1	148	2026-01-06 19:43:51.724722
879	1	165	2026-01-06 19:43:51.724722
880	1	183	2026-01-06 19:43:51.724722
881	1	190	2026-01-06 19:43:51.724722
882	1	195	2026-01-06 19:43:51.724722
883	1	204	2026-01-06 19:43:51.724722
884	1	213	2026-01-06 19:43:51.724722
885	1	5	2026-01-06 19:43:51.724722
886	1	24	2026-01-06 19:43:51.724722
887	1	44	2026-01-06 19:43:51.724722
888	1	95	2026-01-06 19:43:51.724722
889	1	105	2026-01-06 19:43:51.724722
890	1	145	2026-01-06 19:43:51.724722
891	1	184	2026-01-06 19:43:51.724722
892	1	191	2026-01-06 19:43:51.724722
893	1	205	2026-01-06 19:43:51.724722
894	1	214	2026-01-06 19:43:51.724722
895	1	701	2026-01-06 19:43:51.724722
896	1	6	2026-01-06 19:43:51.724722
897	1	25	2026-01-06 19:43:51.724722
898	1	56	2026-01-06 19:43:51.724722
899	1	96	2026-01-06 19:43:51.724722
900	1	106	2026-01-06 19:43:51.724722
901	1	146	2026-01-06 19:43:51.724722
902	1	185	2026-01-06 19:43:51.724722
903	1	215	2026-01-06 19:43:51.724722
904	1	8	2026-01-06 19:43:51.724722
905	1	26	2026-01-06 19:43:51.724722
906	1	97	2026-01-06 19:43:51.724722
907	1	147	2026-01-06 19:43:51.724722
908	1	186	2026-01-06 19:43:51.724722
909	1	9	2026-01-06 19:43:51.724722
910	1	27	2026-01-06 19:43:51.724722
911	1	98	2026-01-06 19:43:51.724722
912	1	10	2026-01-06 19:43:51.724722
913	1	28	2026-01-06 19:43:51.724722
914	1	99	2026-01-06 19:43:51.724722
915	1	11	2026-01-06 19:43:51.724722
916	1	51	2026-01-06 19:43:51.724722
917	1	100	2026-01-06 19:43:51.724722
918	1	52	2026-01-06 19:43:51.724722
919	1	120	2026-01-06 19:43:51.724722
920	1	152	2026-01-06 19:43:51.724722
921	1	53	2026-01-06 19:43:51.724722
922	1	107	2026-01-06 19:43:51.724722
923	1	121	2026-01-06 19:43:51.724722
924	1	153	2026-01-06 19:43:51.724722
925	1	54	2026-01-06 19:43:51.724722
926	1	108	2026-01-06 19:43:51.724722
927	1	154	2026-01-06 19:43:51.724722
928	1	55	2026-01-06 19:43:51.724722
929	1	155	2026-01-06 19:43:51.724722
930	1	156	2026-01-06 19:43:51.724722
931	1	140	2026-01-06 19:43:51.724722
932	1	123	2026-01-06 19:43:51.724722
1779	9	150	2026-01-07 14:13:53.880934
1782	9	161	2026-01-07 14:13:53.880934
1783	9	162	2026-01-07 14:13:53.880934
1784	9	163	2026-01-07 14:13:53.880934
1785	9	164	2026-01-07 14:13:53.880934
1792	9	151	2026-01-07 14:13:53.880934
1160	2	2	2026-01-06 12:48:28.721214
1854	1	130	2026-01-07 14:57:02.239275
1172	6	703	2026-01-06 13:11:49.476657
1855	1	131	2026-01-07 14:57:02.239275
1532	5	705	2026-01-07 18:49:49.999575
1175	9	703	2026-01-06 13:11:49.476657
1921	1	62	2026-01-08 01:43:37.717808
1181	6	704	2026-01-06 13:11:49.476657
1922	2	62	2026-01-08 01:43:37.719517
1923	3	62	2026-01-08 01:43:37.720195
1184	9	704	2026-01-06 13:11:49.476657
1924	6	62	2026-01-08 01:43:37.720398
1190	6	705	2026-01-06 13:11:49.476657
1925	8	62	2026-01-08 01:43:37.720595
1926	9	62	2026-01-08 01:43:37.72357
1193	9	705	2026-01-06 13:11:49.476657
1927	1	713	2026-01-08 01:43:37.723807
1199	6	706	2026-01-06 13:11:49.476657
1928	2	713	2026-01-08 01:43:37.724041
1929	8	713	2026-01-08 01:43:37.724352
1202	9	706	2026-01-06 13:11:49.476657
1930	1	710	2026-01-08 01:43:37.725449
1931	1	711	2026-01-08 01:43:37.725654
1208	6	707	2026-01-06 13:11:49.476657
1932	1	712	2026-01-08 01:43:37.725828
1536	5	113	2026-01-07 18:49:49.999575
1211	9	707	2026-01-06 13:11:49.476657
1933	2	710	2026-01-08 01:43:37.726031
1934	2	711	2026-01-08 01:43:37.7263
1217	6	207	2026-01-06 13:11:49.476657
1935	2	712	2026-01-08 01:43:37.726506
1936	3	710	2026-01-08 01:43:37.7267
1220	9	207	2026-01-06 13:11:49.476657
1538	5	212	2026-01-07 18:49:49.999575
1937	3	711	2026-01-08 01:43:37.72687
1539	5	706	2026-01-07 18:49:49.999575
1233	9	64	2026-01-07 08:34:04.734076
1230	1	217	2026-01-07 10:15:16.523696
1938	3	712	2026-01-08 01:43:37.727377
1939	6	710	2026-01-08 01:43:37.727558
1940	6	711	2026-01-08 01:43:37.727713
1941	6	712	2026-01-08 01:43:37.727886
1942	8	710	2026-01-08 01:43:37.728078
1943	8	711	2026-01-08 01:43:37.728248
1944	8	712	2026-01-08 01:43:37.728426
1238	9	217	2026-01-07 10:15:16.530622
1245	8	703	2026-01-07 18:46:46.060222
1246	8	1	2026-01-07 18:46:46.060222
1945	9	710	2026-01-08 01:43:37.728603
1946	9	711	2026-01-08 01:43:37.728772
1251	8	61	2026-01-07 18:46:46.060222
1252	8	81	2026-01-07 18:46:46.060222
1253	8	91	2026-01-07 18:46:46.060222
1254	8	111	2026-01-07 18:46:46.060222
1255	8	150	2026-01-07 18:46:46.060222
1947	9	712	2026-01-08 01:43:37.728935
1948	1	708	2026-01-08 01:43:37.729305
1258	8	168	2026-01-07 18:46:46.060222
1259	8	170	2026-01-07 18:46:46.060222
1260	8	173	2026-01-07 18:46:46.060222
1261	8	174	2026-01-07 18:46:46.060222
1262	8	175	2026-01-07 18:46:46.060222
1263	8	178	2026-01-07 18:46:46.060222
1264	8	197	2026-01-07 18:46:46.060222
1265	8	216	2026-01-07 18:46:46.060222
1949	1	709	2026-01-08 01:43:37.729457
1267	8	704	2026-01-07 18:46:46.060222
1950	2	708	2026-01-08 01:43:37.729617
1951	2	709	2026-01-08 01:43:37.72978
1272	8	82	2026-01-07 18:46:46.060222
1273	8	92	2026-01-07 18:46:46.060222
1274	8	112	2026-01-07 18:46:46.060222
1275	8	149	2026-01-07 18:46:46.060222
1276	8	151	2026-01-07 18:46:46.060222
1952	8	708	2026-01-08 01:43:37.729938
1278	8	169	2026-01-07 18:46:46.060222
1279	8	171	2026-01-07 18:46:46.060222
1280	8	176	2026-01-07 18:46:46.060222
1281	8	179	2026-01-07 18:46:46.060222
1282	8	705	2026-01-07 18:46:46.060222
1953	8	709	2026-01-08 01:43:37.73014
1286	8	59	2026-01-07 18:46:46.060222
1287	8	93	2026-01-07 18:46:46.060222
1288	8	113	2026-01-07 18:46:46.060222
1290	8	172	2026-01-07 18:46:46.060222
1291	8	177	2026-01-07 18:46:46.060222
1292	8	706	2026-01-07 18:46:46.060222
1295	8	94	2026-01-07 18:46:46.060222
1298	8	707	2026-01-07 18:46:46.060222
1299	8	5	2026-01-07 18:46:46.060222
1301	8	95	2026-01-07 18:46:46.060222
1302	8	6	2026-01-07 18:46:46.060222
1303	8	96	2026-01-07 18:46:46.060222
1304	8	8	2026-01-07 18:46:46.060222
1305	8	97	2026-01-07 18:46:46.060222
1306	8	9	2026-01-07 18:46:46.060222
1562	5	107	2026-01-07 18:49:49.999575
1564	5	54	2026-01-07 18:49:49.999575
1565	5	108	2026-01-07 18:49:49.999575
1859	8	2	2026-01-07 15:24:40.110193
1567	5	55	2026-01-07 18:49:49.999575
1954	1	714	2026-01-08 02:15:45.87159
1571	5	123	2026-01-07 18:49:49.999575
1572	5	207	2026-01-07 18:49:49.999575
1573	5	58	2026-01-07 18:49:49.999575
1808	8	27	2026-01-07 14:24:17.85559
1809	8	10	2026-01-07 14:24:17.859586
1810	8	101	2026-01-07 14:24:17.859943
1811	8	102	2026-01-07 14:24:17.860273
1812	8	103	2026-01-07 14:24:17.860554
1813	8	104	2026-01-07 14:24:17.860868
1814	8	105	2026-01-07 14:24:17.861153
1815	8	106	2026-01-07 14:24:17.861571
1816	8	701	2026-01-07 14:24:17.861853
1817	8	130	2026-01-07 14:24:17.862109
1818	8	131	2026-01-07 14:24:17.862298
1819	8	180	2026-01-07 14:24:17.862479
1820	8	181	2026-01-07 14:24:17.862693
1821	8	182	2026-01-07 14:24:17.862923
1822	8	183	2026-01-07 14:24:17.863152
1823	8	184	2026-01-07 14:24:17.863418
1824	8	185	2026-01-07 14:24:17.863619
1825	8	186	2026-01-07 14:24:17.863795
1826	8	187	2026-01-07 14:24:17.863974
1827	8	188	2026-01-07 14:24:17.864149
1828	8	189	2026-01-07 14:24:17.864339
1829	8	190	2026-01-07 14:24:17.864544
1830	8	191	2026-01-07 14:24:17.864739
1831	8	192	2026-01-07 14:24:17.864948
1832	8	193	2026-01-07 14:24:17.865149
1833	8	194	2026-01-07 14:24:17.865324
1834	8	195	2026-01-07 14:24:17.865516
1955	2	714	2026-01-08 02:15:45.873391
1956	3	714	2026-01-08 02:15:45.873732
1957	5	714	2026-01-08 02:15:45.874033
1958	6	714	2026-01-08 02:15:45.874698
1959	8	714	2026-01-08 02:15:45.87506
1960	9	714	2026-01-08 02:15:45.87563
\.


--
-- Name: dossier_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.dossier_template_id_seq', 5, false);


--
-- Name: dossier_template_item_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.dossier_template_item_id_seq', 43, false);


--
-- Name: sys_external_integration_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_external_integration_id_seq', 21, true);


--
-- Name: sys_menu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_menu_id_seq', 219, true);


--
-- Name: sys_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_role_id_seq', 9, true);


--
-- Name: sys_role_menu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_role_menu_id_seq', 2012, true);


--
-- PostgreSQL database dump complete
--

\unrestrict mJpbjeo5I4OCOR1mRRwfCinlU8mIYgdnfF6h7jbnSjNp5bQmbaGedgm9xZ1tOCK

