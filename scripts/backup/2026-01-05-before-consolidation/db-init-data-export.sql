--
-- PostgreSQL database dump
--

\restrict vgXGkMz2seFYB1kCKTn9BiFVkaLvQPFhVb5cRd94IPsZTDmsUai7rRfPBfeWJkH

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
-- Data for Name: contract_template; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.contract_template (id, template_no, name, contract_type, fee_type, content, clauses, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) FROM stdin;
1	CT-001	标准服务合同	SERVICE	FIXED	委托代理合同\n\n甲方（委托人）：${clientName}\n乙方（受托人）：${firmName}\n\n鉴于甲方因${matterDescription}事宜，委托乙方提供法律服务，经双方协商一致，订立本合同。\n\n第一条 委托事项\n甲方委托乙方就${matterName}事项提供法律服务。\n\n第二条 服务内容\n1. 提供法律咨询；\n2. 代理参与诉讼/仲裁/调解；\n3. 起草、审查法律文书；\n4. 其他双方约定的法律服务。\n\n第三条 服务费用\n本合同项下法律服务费用为人民币${totalAmount}元整。\n\n第四条 付款方式\n${paymentTerms}\n\n第五条 合同期限\n本合同自${effectiveDate}起至${expiryDate}止。\n\n第六条 双方权利义务\n（一）甲方权利义务\n1. 如实向乙方陈述案件事实，提供相关证据材料；\n2. 按约定支付法律服务费用；\n3. 配合乙方开展法律服务工作。\n\n（二）乙方权利义务\n1. 依法维护甲方合法权益；\n2. 保守甲方商业秘密和个人隐私；\n3. 及时向甲方通报案件进展情况。\n\n第七条 违约责任\n任何一方违反本合同约定，应承担相应违约责任。\n\n第八条 争议解决\n本合同履行过程中发生争议，双方协商解决；协商不成的，提交乙方所在地人民法院诉讼解决。\n\n甲方（签章）：                    乙方（签章）：\n日期：${signDate}                 日期：${signDate}	[{"title":"服务范围","content":"提供法律咨询、代理诉讼、起草审查法律文书等"},{"title":"保密条款","content":"乙方对甲方的商业秘密和个人隐私负有保密义务"},{"title":"免责条款","content":"因甲方提供虚假信息导致的损失由甲方自行承担"}]	适用于一般法律服务的标准合同模板	ACTIVE	1	2026-01-04 16:14:15.903185	2026-01-04 16:14:15.903185	\N	\N	f
2	CT-002	常年法律顾问合同	RETAINER	FIXED	常年法律顾问合同\n\n甲方（委托人）：${clientName}\n乙方（受托人）：${firmName}\n\n第一条 服务期限\n自${effectiveDate}起至${expiryDate}止，为期一年。\n\n第二条 服务内容\n1. 日常法律咨询；\n2. 合同审查与起草；\n3. 法律风险评估；\n4. 参与重大商务谈判；\n5. 代理诉讼/仲裁（另行收费）。\n\n第三条 服务费用\n年度顾问费人民币${totalAmount}元整。\n\n第四条 付款方式\n${paymentTerms}\n\n第五条 服务方式\n1. 电话、邮件咨询不限次数；\n2. 每月提供不少于${serviceHours}小时现场服务；\n3. 紧急事项24小时响应。\n\n甲方（签章）：                    乙方（签章）：\n日期：${signDate}                 日期：${signDate}	[{"title":"服务响应","content":"常规咨询24小时内响应，紧急事项即时响应"},{"title":"续约条款","content":"合同到期前30日，双方可协商续约事宜"}]	适用于企业常年法律顾问服务	ACTIVE	2	2026-01-04 16:14:15.903185	2026-01-04 16:14:15.903185	\N	\N	f
3	CT-003	民事/行政委托代理合同	LITIGATION	FIXED	委 托 代 理 合 同\n\n（${contractYear}）${contractNo}代字第${contractSeq}号\n\n委托人：${clientName}\n住所地：${clientAddress}                     电话：${clientPhone}\n受托人：${firmName}\n法定代表人（负责人）：${firmLegalRep}        职务：主任\n住所地：${firmAddress}，电话：${firmPhone}。\n\n委托人因与${opposingParty}${causeOfAction}纠纷一案，委托人与受托人就委托代理事宜，经平等协商，自愿订立本合同，以供双方共同遵守执行。\n\n一、受托人接受委托后，指派律师${lawyerNames}、律师助理${assistantNames}为委托人的${trialStage}代理人。\n\n二、代理权限为：${authorizationType}（一般代理或特别代理）。\n一般代理的权限为：拟写诉讼（仲裁）文书、起诉（立案）、应诉、调查取证、参加庭审、签收诉讼（仲裁）法律文书等诉讼活动。\n特别代理的权限（根据相应的代理阶段据实确定）为：承认、变更、放弃诉讼（仲裁）请求，决定是否调解、和解并签订调解、和解协议，提起反诉或上诉，代为申请执行并收付执行款物，以及行使与案件有关的其他诉讼权利。\n\n三、根据《律师服务收费管理办法》及相关规定，委托人应当向受托人支付的代理费大写${totalAmountCN}（¥：${totalAmount}），定于${paymentDeadline}前付清。\n委托人应当以直接汇入受托人单位银行账户或支付给持有受托人单位签章票据（收据或发票）的人员的方式付费，不得私自向受托人单位的律师、律师助理或其他工作人员支付任何费用。否则，由此产生的法律后果与受托人单位无关。\n\n四、委托人必须如实全面地向受托人陈述案情，提供证据及证据线索，并不得有下列行为：\n1、隐瞒案情；2、伪造证据；3、虚假陈述；4、利用受托人提供法律服务谋取非法利益的其他行为。\n受托人发现委托人有前列情形之一时，有权单方终止本合同的履行，所收委托人的一切费用概不予退还。\n\n五、受托人必须尽职尽责，依法维护委托人利益，并不得有下列行为：\n1、与委托人的对方当事人及其代理人恶意串通，损害委托人的利益；2、接受委托人的对方当事人及其代理人的吃请或其他贿赂；3、指派其他律师担任同一案件中对方当事人的代理人；4、泄露委托人的商业秘密或个人隐私；5、其他损害委托人合法利益的行为。\n\n六、委托人不得以下列理由解除本合同：\n1、与对方当事人自行协商、和解；2、法院主持调解结案；3、委托人自己撤诉或放弃权利；4、对方当事人撤诉或主动履行义务；5、未经法院判决应当解除合同的其他理由。\n\n七、受托人在履行本合同过程中，如出现下列情形，委托人可以解除本合同并要求受托人退费：\n1、经人民法院判决确认，受托人实施了损害委托人合法权益的行为；2、受托人无正当理由明确表示不履行代理义务；3、受托人代理义务未履行完毕之前，丧失执业资格。\n\n八、委托人不得以下列理由要求受托人退费：\n1、案件败诉；2、裁判文书确定的利益未实现或未能达到委托人的预期目的；3、案件出现本合同第六条规定的情形；4、未经法院判决受托人应当退费的其他理由。\n\n九、因履行本合同过程中引起的或本合同有关的任何争议，不能协商解决的，双方共同选择以下第${disputeResolution}种方式解决：\n1.由受托人住所地法院管辖。2.提请${arbitrationCommittee}仲裁委员会按该会仲裁规则进行仲裁。\n\n十、本合同自双方签字和盖章之日起生效。\n\n十一、本合同未尽事宜，由双方本着平等、自愿、合法的原则，协商解决。\n\n十二、本合同一式二份，双方各持一份。\n\n十三、特别约定：${specialTerms}\n\n委托人签章：                            受托人签章：\n法定代表人（负责人）：                   负责人签章：\n                           ${signDate}	[{"title":"代理权限说明","content":"一般代理：拟写诉讼文书、起诉、应诉、调查取证、参加庭审、签收法律文书。特别代理：承认、变更、放弃诉讼请求，调解和解，提起反诉或上诉，申请执行并收付执行款物"},{"title":"委托人义务","content":"如实陈述案情，提供证据，不得隐瞒案情、伪造证据、虚假陈述"},{"title":"受托人义务","content":"尽职尽责，依法维护委托人利益，保守商业秘密和个人隐私"},{"title":"费用支付","content":"应直接汇入受托人单位银行账户或支付给持有受托人单位签章票据的人员"}]	适用于民事案件和行政案件的委托代理合同，符合律师协会规范	ACTIVE	3	2026-01-04 16:14:15.903185	2026-01-04 16:14:15.903185	\N	\N	f
4	CT-004	非诉项目合同	NON_LITIGATION	HOURLY	非诉讼法律服务合同\n\n甲方（委托人）：${clientName}\n乙方（受托人）：${firmName}\n\n第一条 项目名称\n${matterName}\n\n第二条 服务内容\n${matterDescription}\n\n第三条 收费方式\n本项目采用计时收费方式：\n1. 合伙人：人民币${partnerRate}元/小时；\n2. 资深律师：人民币${seniorRate}元/小时；\n3. 律师助理：人民币${assistantRate}元/小时。\n\n第四条 费用预估\n预估总费用人民币${totalAmount}元（实际以工时记录为准）。\n\n第五条 付款方式\n${paymentTerms}\n\n甲方（签章）：                    乙方（签章）：\n日期：${signDate}                 日期：${signDate}	[{"title":"工时记录","content":"乙方应如实记录工作时间，定期向甲方提供工时报告"},{"title":"费用上限","content":"如实际费用超出预估20%，乙方应提前告知甲方"}]	适用于非诉讼项目，按工时计费	ACTIVE	4	2026-01-04 16:14:15.903185	2026-01-04 16:14:15.903185	\N	\N	f
5	CT-005	刑事案件委托代理合同	LITIGATION	FIXED	委 托 代 理 合 同\n\n（${contractYear}）刑辩字第${contractSeq}号\n\n委托人：${clientName}                           电话：${clientPhone}\n受托人：${firmName}\n法定代表人（负责人）：${firmLegalRep}        职务：主任\n住所地：${firmAddress}，电话：${firmPhone}。\n\n因${defendantName}涉嫌${criminalCharge}一案，委托人与受托人就委托辩护（代理）事宜，经平等协商，自愿订立本合同，以供双方共同遵守。\n\n一、受托人接受委托后，指派律师${lawyerNames}担任被告人（犯罪嫌疑人、受害人）${defendantName}的${defenseStage}。\nA、侦查阶段的律师；B、审查起诉阶段的辩护人；C、一审辩护人；D、二审辩护人；E、死刑复核阶段的辩护人；F、再审辩护人；\n\n二、根据《律师服务收费管理办法》及相关规定，委托人应当向受托人支付辩护费（代理）${totalAmountCN}元（￥${totalAmount}），定于${paymentDeadline}前付清。\n委托人应当以直接汇入受托人单位银行账户或支付给持有受托人单位签章票据（收据或发票）的人员的方式付费，不得私自向受托人单位的律师、律师助理或其他工作人员支付任何费用。否则，由此产生的法律后果与受托人单位无关。\n\n三、受托人必须尽职尽责，依法维护被告人（犯罪嫌疑人、受害人）的合法权益，并及时与委托方沟通联系。\n\n四、如出现被告人（犯罪嫌疑人）拒绝受托人指派的律师为其辩护（代理）的情况，除非受托人实施了损害被告人（犯罪嫌疑人）的合法权益的行为，委托人不得要求受托人退还所交费用。受托人是否实施了损害被告人合法权益的行为，须由人民法院判决确认。\n\n五、委托人不得以下列理由要求受托人退还费用：\n1、侦查部门撤销案件；2、审查起诉部门决定对犯罪嫌疑人不起诉；3、公诉机关撤回对案件的起诉；4、被告人（犯罪嫌疑人）被取保候审；5、案件的处理结果未达到或未能完全达到委托人的预期目的；6、委托人单方解除本合同；7、被告人（犯罪嫌疑人）拒绝委托人指派的律师为其辩护（代理）。8、其他非经法院判决确认退费的理由。\n\n六、因履行本合同所发生的不能协商解决的争议，由受托人住所地法院管辖。\n\n七、本合同自双方签字、盖章之日起生效。\n\n八、本合同未尽事宜，由双方本着平等、自愿、合法的原则，协商解决。\n\n九、本合同一式二份，双方各持一份。\n\n十、特别约定：${specialTerms}\n\n委托人签章：                                受托人签章：\n法定代表人（负责人）：                       负责人签章：\n${signDate}	[{"title":"辩护阶段说明","content":"A-侦查阶段律师、B-审查起诉阶段辩护人、C-一审辩护人、D-二审辩护人、E-死刑复核阶段辩护人、F-再审辩护人"},{"title":"受托人义务","content":"尽职尽责，依法维护被告人（犯罪嫌疑人、受害人）的合法权益，及时与委托方沟通联系"},{"title":"不予退费情形","content":"侦查部门撤销案件、不起诉、撤回起诉、取保候审、结果未达预期、委托人单方解除、被告人拒绝辩护等"},{"title":"费用支付","content":"应直接汇入受托人单位银行账户或支付给持有受托人单位签章票据的人员"}]	适用于刑事案件的辩护代理合同，包含侦查、审查起诉、一审、二审、死刑复核、再审等阶段	ACTIVE	5	2026-01-04 16:14:15.903185	2026-01-04 16:14:15.903185	\N	\N	f
\.


--
-- Data for Name: finance_commission_rule; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.finance_commission_rule (id, rule_code, rule_name, rule_type, firm_retention_rate, originator_rate, tax_rate, management_fee_rate, rate_tiers, effective_date, expiry_date, is_default, active, created_by, created_at, updated_at, updated_by, deleted) FROM stdin;
1	DEFAULT	默认提成规则	\N	0.3000	0.2000	0.0672	0.1500	[{"rate": 0.30, "maxAmount": 100000, "minAmount": 0}, {"rate": 0.35, "maxAmount": 500000, "minAmount": 100000}, {"rate": 0.40, "maxAmount": 1000000, "minAmount": 500000}, {"rate": 0.45, "maxAmount": null, "minAmount": 1000000}]	\N	\N	t	t	\N	2026-01-04 16:14:18.417555	2026-01-04 16:14:18.417555	\N	f
\.


--
-- Data for Name: letter_template; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.letter_template (id, template_no, name, letter_type, content, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) FROM stdin;
1	LT000001	律师会见介绍信	MEETING	介绍信\n\n${targetUnit}：\n\n兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}（案号：${matterNo}）一案进行会见工作。\n\n请予接洽为荷。\n\n此致\n\n敬礼\n\n                                    XX律师事务所\n                                    ${date}	用于律师前往看守所、监狱等场所会见当事人	ACTIVE	1	2026-01-04 16:14:15.83048	2026-01-04 16:14:15.83048	\N	\N	f
2	LT000002	律师调查介绍信	INVESTIGATION	介绍信\n\n${targetUnit}：\n\n兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}（案号：${matterNo}）一案进行调查取证工作。\n\n请予协助为荷。\n\n此致\n\n敬礼\n\n                                    XX律师事务所\n                                    ${date}	用于律师前往相关单位调查取证	ACTIVE	2	2026-01-04 16:14:15.83048	2026-01-04 16:14:15.83048	\N	\N	f
3	LT000003	律师阅卷介绍信	FILE_REVIEW	介绍信\n\n${targetUnit}：\n\n兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}（案号：${matterNo}）一案进行阅卷工作。\n\n请予接洽为荷。\n\n此致\n\n敬礼\n\n                                    XX律师事务所\n                                    ${date}	用于律师前往法院、检察院阅卷	ACTIVE	3	2026-01-04 16:14:15.83048	2026-01-04 16:14:15.83048	\N	\N	f
4	LT000004	通用介绍信	INTRODUCTION	介绍信\n\n${targetUnit}：\n\n兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}一案办理相关事宜。\n\n请予接洽为荷。\n\n此致\n\n敬礼\n\n                                    XX律师事务所\n                                    ${date}	通用介绍信模板	ACTIVE	4	2026-01-04 16:14:15.83048	2026-01-04 16:14:15.83048	\N	\N	f
\.


--
-- Data for Name: sys_department; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_department (id, name, parent_id, sort_order, leader_id, status, created_at, updated_at, created_by, updated_by, deleted) FROM stdin;
1	律师事务所	0	1	\N	ACTIVE	2026-01-04 16:14:15.893589	2026-01-04 16:14:15.893589	\N	\N	f
2	诉讼业务部	1	1	\N	ACTIVE	2026-01-04 16:14:15.893589	2026-01-04 16:14:15.893589	\N	\N	f
3	非诉业务部	1	2	\N	ACTIVE	2026-01-04 16:14:15.893589	2026-01-04 16:14:15.893589	\N	\N	f
4	知识产权部	1	3	\N	ACTIVE	2026-01-04 16:14:15.893589	2026-01-04 16:14:15.893589	\N	\N	f
5	财务部	1	4	\N	ACTIVE	2026-01-04 16:14:15.893589	2026-01-04 16:14:15.893589	\N	\N	f
6	行政部	1	5	\N	ACTIVE	2026-01-04 16:14:15.893589	2026-01-04 16:14:15.893589	\N	\N	f
\.


--
-- Data for Name: sys_menu; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_menu (id, parent_id, name, path, component, redirect, icon, menu_type, permission, sort_order, visible, status, is_external, is_cache, created_at, updated_at, created_by, updated_by, deleted) FROM stdin;
1	0	工作台	/dashboard	dashboard/index/index	\N	DashboardOutlined	MENU	\N	1	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
2	0	系统管理	/system	LAYOUT	\N	SettingOutlined	DIRECTORY	\N	2	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
3	0	客户管理	/crm	LAYOUT	\N	TeamOutlined	DIRECTORY	\N	3	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
4	0	项目管理	/matter	LAYOUT	\N	FolderOutlined	DIRECTORY	\N	4	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
5	0	财务管理	/finance	LAYOUT	\N	MoneyCollectOutlined	DIRECTORY	\N	5	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
6	0	文书管理	/document	LAYOUT	\N	FileTextOutlined	DIRECTORY	\N	6	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
8	0	档案管理	/archive	LAYOUT	\N	DatabaseOutlined	DIRECTORY	\N	7	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
9	0	行政管理	/admin	LAYOUT	\N	BankOutlined	DIRECTORY	\N	8	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
10	0	人力资源	/hr	LAYOUT	\N	IdcardOutlined	DIRECTORY	\N	9	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
11	0	知识库	/knowledge	LAYOUT	\N	BookOutlined	DIRECTORY	\N	10	t	ENABLED	f	t	2026-01-04 16:14:15.895648	2026-01-04 16:14:15.895648	\N	\N	f
21	2	用户管理	/system/user	system/user/index	\N	UserOutlined	MENU	sys:user:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
22	2	角色管理	/system/role	system/role/index	\N	SafetyCertificateOutlined	MENU	sys:role:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
23	2	部门管理	/system/dept	system/dept/index	\N	ApartmentOutlined	MENU	sys:dept:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
24	2	菜单管理	/system/menu	system/menu/index	\N	MenuOutlined	MENU	sys:menu:list	4	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
25	2	系统配置	/system/config	system/config/index	\N	ToolOutlined	MENU	sys:config:list	5	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
26	2	操作日志	/system/log	system/log/index	\N	FileSearchOutlined	MENU	sys:log:list	6	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
27	2	出函模板	/system/letter-template	system/letter-template/index	\N	MailOutlined	MENU	sys:letter-template:list	7	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
28	2	合同模板	/system/contract-template	system/contract-template/index	\N	FileProtectOutlined	MENU	sys:contract-template:list	8	t	ENABLED	f	t	2026-01-04 16:14:15.896441	2026-01-04 16:14:15.896441	\N	\N	f
31	3	客户列表	/crm/client	crm/client/index	\N	ContactsOutlined	MENU	client:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.896673	2026-01-04 16:14:15.896673	\N	\N	f
32	3	利冲审查	/crm/conflict	crm/conflict/index	\N	SecurityScanOutlined	MENU	conflict:apply	2	t	ENABLED	f	t	2026-01-04 16:14:15.896673	2026-01-04 16:14:15.896673	\N	\N	f
33	3	案源管理	/crm/lead	crm/lead/index	\N	FunnelPlotOutlined	MENU	lead:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.896673	2026-01-04 16:14:15.896673	\N	\N	f
41	4	项目列表	/matter/list	matter/list/index	\N	ProjectOutlined	MENU	matter:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
42	4	我的项目	/matter/my	matter/my/index	\N	SolutionOutlined	MENU	matter:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
45	4	合同管理	/matter/contract	matter/contract/index	\N	FileProtectOutlined	MENU	matter:contract:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
43	4	工时管理	/matter/timesheet	matter/timesheet/index	\N	ClockCircleOutlined	MENU	timesheet:record	4	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
44	4	任务管理	/matter/task	matter/task/index	\N	CheckSquareOutlined	MENU	task:manage	5	t	ENABLED	f	t	2026-01-04 16:14:15.896878	2026-01-04 16:14:15.896878	\N	\N	f
51	5	合同查看	/finance/contract	finance/contract/index	\N	FileProtectOutlined	MENU	contract:view	1	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
52	5	收款管理	/finance/payment	finance/payment/index	\N	PayCircleOutlined	MENU	finance:payment:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
53	5	提成管理	/finance/commission	finance/commission/index	\N	PercentageOutlined	MENU	finance:commission:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
54	5	发票管理	/finance/invoice	finance/invoice/index	\N	ProfileOutlined	MENU	finance:invoice:apply	4	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
55	5	财务报表	/finance/report	finance/report/index	\N	BarChartOutlined	MENU	finance:report:view	5	t	ENABLED	f	t	2026-01-04 16:14:15.897126	2026-01-04 16:14:15.897126	\N	\N	f
61	6	文书列表	/document/list	document/list/index	\N	FileOutlined	MENU	doc:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.89735	2026-01-04 16:14:15.89735	\N	\N	f
62	6	文书模板	/document/template	document/template/index	\N	FileAddOutlined	MENU	doc:template:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.89735	2026-01-04 16:14:15.89735	\N	\N	f
63	6	印章管理	/document/seal	document/seal/index	\N	SafetyOutlined	MENU	doc:seal:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.89735	2026-01-04 16:14:15.89735	\N	\N	f
64	6	用印申请	/document/seal-apply	document/seal-apply/index	\N	FormOutlined	MENU	doc:seal:apply	4	t	ENABLED	f	t	2026-01-04 16:14:15.89735	2026-01-04 16:14:15.89735	\N	\N	f
81	8	档案列表	/archive/list	archive/list/index	\N	FolderOpenOutlined	MENU	archive:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.89755	2026-01-04 16:14:15.89755	\N	\N	f
82	8	档案借阅	/archive/borrow	archive/borrow/index	\N	ExportOutlined	MENU	archive:borrow	2	t	ENABLED	f	t	2026-01-04 16:14:15.89755	2026-01-04 16:14:15.89755	\N	\N	f
83	8	档案销毁	/archive/destroy	archive/destroy/index	\N	DeleteOutlined	MENU	archive:destroy	3	t	ENABLED	f	t	2026-01-04 16:14:15.89755	2026-01-04 16:14:15.89755	\N	\N	f
91	9	考勤管理	/admin/attendance	admin/attendance/index	\N	ClockCircleOutlined	MENU	admin:attendance:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
92	9	请假管理	/admin/leave	admin/leave/index	\N	CalendarOutlined	MENU	admin:leave:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
93	9	会议室预约	/admin/meeting-room	admin/meeting-room/index	\N	ScheduleOutlined	MENU	admin:meeting:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
94	9	资产管理	/admin/asset	admin/asset/index	\N	ShopOutlined	MENU	admin:asset:list	4	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
95	9	采购管理	/admin/purchase	admin/purchase/index	\N	ShoppingCartOutlined	MENU	admin:purchase:list	5	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
96	9	供应商管理	/admin/supplier	admin/supplier/index	\N	ShopOutlined	MENU	admin:supplier:list	6	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
97	9	加班管理	/admin/overtime	admin/overtime/index	\N	ClockCircleOutlined	MENU	admin:overtime:list	7	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
98	9	资产盘点	/admin/asset-inventory	admin/asset-inventory/index	\N	FileSearchOutlined	MENU	admin:asset-inventory:list	8	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
99	9	外出管理	/admin/go-out	admin/go-out/index	\N	ExportOutlined	MENU	admin:go-out:list	9	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
100	9	会议记录	/admin/meeting-record	admin/meeting-record/index	\N	FileTextOutlined	MENU	admin:meeting-record:list	10	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
120	9	出函管理	/admin/letter	admin/letter/index	\N	MailOutlined	MENU	admin:letter:list	11	t	ENABLED	f	t	2026-01-04 16:14:15.89774	2026-01-04 16:14:15.89774	\N	\N	f
101	10	培训管理	/hr/training	hr/training/index	\N	ReadOutlined	MENU	hr:training:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
102	10	绩效考核	/hr/performance	hr/performance/index	\N	TrophyOutlined	MENU	hr:performance:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
103	10	晋升管理	/hr/promotion	hr/promotion/index	\N	RiseOutlined	MENU	hr:promotion:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
104	10	发展计划	/hr/development	hr/development/index	\N	AimOutlined	MENU	hr:development:list	4	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
105	10	转正管理	/hr/regularization	hr/regularization/index	\N	CheckSquareOutlined	MENU	hr:regularization:list	5	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
106	10	离职管理	/hr/resignation	hr/resignation/index	\N	ExportOutlined	MENU	hr:resignation:list	6	t	ENABLED	f	t	2026-01-04 16:14:15.898121	2026-01-04 16:14:15.898121	\N	\N	f
111	11	知识文章	/knowledge/article	knowledge/article/index	\N	FileTextOutlined	MENU	knowledge:article:list	1	t	ENABLED	f	t	2026-01-04 16:14:15.898559	2026-01-04 16:14:15.898559	\N	\N	f
112	11	案例库	/knowledge/case	knowledge/case/index	\N	ContainerOutlined	MENU	knowledge:case:list	2	t	ENABLED	f	t	2026-01-04 16:14:15.898559	2026-01-04 16:14:15.898559	\N	\N	f
113	11	法规库	/knowledge/law	knowledge/law/index	\N	BookOutlined	MENU	knowledge:law:list	3	t	ENABLED	f	t	2026-01-04 16:14:15.898559	2026-01-04 16:14:15.898559	\N	\N	f
56	5	收款变更审批	/finance/payment-amendment	finance/payment-amendment/index	\N	AuditOutlined	MENU	fee:amendment:list	6	t	ENABLED	f	t	2026-01-05 02:19:31.876174	2026-01-05 02:19:31.876174	\N	\N	f
121	9	合同查询	/admin/contract	admin/contract/index	\N	FileSearchOutlined	MENU	admin:contract:list	12	t	ENABLED	f	t	2026-01-05 02:19:57.79315	2026-01-05 02:19:57.79315	\N	\N	f
123	5	合同变更处理	/finance/contract-amendment	/finance/contract-amendment/index	\N	ant-design:diff-outlined	MENU	finance:contract:amendment:view	60	t	ENABLED	f	t	2026-01-05 03:55:33.410601	2026-01-05 03:55:33.410601	\N	\N	f
124	123	同步变更	\N	\N	\N	\N	BUTTON	finance:contract:amendment:sync	1	t	ENABLED	f	t	2026-01-05 03:55:33.410601	2026-01-05 03:55:33.410601	\N	\N	f
125	123	忽略变更	\N	\N	\N	\N	BUTTON	finance:contract:amendment:ignore	2	t	ENABLED	f	t	2026-01-05 03:55:33.410601	2026-01-05 03:55:33.410601	\N	\N	f
\.


--
-- Data for Name: sys_role; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_role (id, role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) FROM stdin;
1	ADMIN	超级管理员	系统最高权限，可管理所有功能和数据	ALL	ACTIVE	1	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
2	DIRECTOR	律所主任	律所管理层，可查看全所数据，审批重要事项	ALL	ACTIVE	2	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
3	PARTNER	合伙人	律所合伙人，可查看部门数据，参与重大决策	DEPT_AND_CHILD	ACTIVE	3	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
4	FINANCE_MANAGER	财务主管	财务管理负责人，管理全所财务数据	ALL	ACTIVE	4	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
5	FINANCE	财务人员	财务操作人员，处理日常财务工作	DEPT	ACTIVE	5	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
6	LEAD_LAWYER	主办律师	案件主办人，管理自己主办的项目	SELF	ACTIVE	6	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
7	ASSOCIATE	协办律师	案件协办人，参与项目协作	SELF	ACTIVE	7	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
8	ADMIN_STAFF	行政人员	行政后勤人员，管理行政事务	DEPT	ACTIVE	8	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
9	TRAINEE	实习律师	实习人员，有限权限	SELF	ACTIVE	9	2026-01-04 16:14:15.892579	2026-01-04 16:14:15.892579	\N	\N	f
\.


--
-- Data for Name: sys_role_menu; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_role_menu (id, role_id, menu_id, created_at) FROM stdin;
1	1	1	2026-01-04 16:14:15.8992
2	1	2	2026-01-04 16:14:15.8992
3	1	3	2026-01-04 16:14:15.8992
4	1	4	2026-01-04 16:14:15.8992
5	1	5	2026-01-04 16:14:15.8992
6	1	6	2026-01-04 16:14:15.8992
7	1	8	2026-01-04 16:14:15.8992
8	1	9	2026-01-04 16:14:15.8992
9	1	10	2026-01-04 16:14:15.8992
10	1	11	2026-01-04 16:14:15.8992
11	1	21	2026-01-04 16:14:15.8992
12	1	22	2026-01-04 16:14:15.8992
13	1	23	2026-01-04 16:14:15.8992
14	1	24	2026-01-04 16:14:15.8992
15	1	25	2026-01-04 16:14:15.8992
16	1	26	2026-01-04 16:14:15.8992
17	1	27	2026-01-04 16:14:15.8992
18	1	28	2026-01-04 16:14:15.8992
19	1	31	2026-01-04 16:14:15.8992
20	1	32	2026-01-04 16:14:15.8992
21	1	33	2026-01-04 16:14:15.8992
22	1	41	2026-01-04 16:14:15.8992
23	1	42	2026-01-04 16:14:15.8992
24	1	45	2026-01-04 16:14:15.8992
25	1	43	2026-01-04 16:14:15.8992
26	1	44	2026-01-04 16:14:15.8992
27	1	51	2026-01-04 16:14:15.8992
28	1	52	2026-01-04 16:14:15.8992
29	1	53	2026-01-04 16:14:15.8992
30	1	54	2026-01-04 16:14:15.8992
31	1	55	2026-01-04 16:14:15.8992
32	1	61	2026-01-04 16:14:15.8992
33	1	62	2026-01-04 16:14:15.8992
34	1	63	2026-01-04 16:14:15.8992
35	1	64	2026-01-04 16:14:15.8992
36	1	81	2026-01-04 16:14:15.8992
37	1	82	2026-01-04 16:14:15.8992
38	1	83	2026-01-04 16:14:15.8992
39	1	91	2026-01-04 16:14:15.8992
40	1	92	2026-01-04 16:14:15.8992
41	1	93	2026-01-04 16:14:15.8992
42	1	94	2026-01-04 16:14:15.8992
43	1	95	2026-01-04 16:14:15.8992
44	1	96	2026-01-04 16:14:15.8992
45	1	97	2026-01-04 16:14:15.8992
46	1	98	2026-01-04 16:14:15.8992
47	1	99	2026-01-04 16:14:15.8992
48	1	100	2026-01-04 16:14:15.8992
49	1	120	2026-01-04 16:14:15.8992
50	1	101	2026-01-04 16:14:15.8992
51	1	102	2026-01-04 16:14:15.8992
52	1	103	2026-01-04 16:14:15.8992
53	1	104	2026-01-04 16:14:15.8992
54	1	105	2026-01-04 16:14:15.8992
55	1	106	2026-01-04 16:14:15.8992
56	1	111	2026-01-04 16:14:15.8992
57	1	112	2026-01-04 16:14:15.8992
58	1	113	2026-01-04 16:14:15.8992
59	2	1	2026-01-04 16:14:15.900075
60	2	2	2026-01-04 16:14:15.900075
61	2	3	2026-01-04 16:14:15.900075
62	2	4	2026-01-04 16:14:15.900075
63	2	5	2026-01-04 16:14:15.900075
64	2	6	2026-01-04 16:14:15.900075
65	2	8	2026-01-04 16:14:15.900075
66	2	9	2026-01-04 16:14:15.900075
67	2	10	2026-01-04 16:14:15.900075
68	2	11	2026-01-04 16:14:15.900075
69	2	21	2026-01-04 16:14:15.900075
70	2	22	2026-01-04 16:14:15.900075
71	2	23	2026-01-04 16:14:15.900075
72	2	24	2026-01-04 16:14:15.900075
73	2	25	2026-01-04 16:14:15.900075
74	2	26	2026-01-04 16:14:15.900075
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
90	3	1	2026-01-04 16:14:15.900502
91	3	3	2026-01-04 16:14:15.900502
92	3	4	2026-01-04 16:14:15.900502
93	3	5	2026-01-04 16:14:15.900502
94	3	31	2026-01-04 16:14:15.900502
95	3	32	2026-01-04 16:14:15.900502
96	3	33	2026-01-04 16:14:15.900502
97	3	41	2026-01-04 16:14:15.900502
98	3	42	2026-01-04 16:14:15.900502
99	3	45	2026-01-04 16:14:15.900502
100	3	43	2026-01-04 16:14:15.900502
101	3	44	2026-01-04 16:14:15.900502
102	3	51	2026-01-04 16:14:15.900502
103	3	52	2026-01-04 16:14:15.900502
104	3	53	2026-01-04 16:14:15.900502
105	3	54	2026-01-04 16:14:15.900502
106	3	55	2026-01-04 16:14:15.900502
107	4	1	2026-01-04 16:14:15.900745
108	4	5	2026-01-04 16:14:15.900745
109	4	51	2026-01-04 16:14:15.900745
110	4	52	2026-01-04 16:14:15.900745
111	4	53	2026-01-04 16:14:15.900745
112	4	54	2026-01-04 16:14:15.900745
113	4	55	2026-01-04 16:14:15.900745
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
129	6	62	2026-01-04 16:14:15.901047
130	6	63	2026-01-04 16:14:15.901047
131	6	64	2026-01-04 16:14:15.901047
132	6	81	2026-01-04 16:14:15.901047
133	6	82	2026-01-04 16:14:15.901047
134	6	83	2026-01-04 16:14:15.901047
135	6	111	2026-01-04 16:14:15.901047
136	6	112	2026-01-04 16:14:15.901047
137	6	113	2026-01-04 16:14:15.901047
138	8	1	2026-01-04 16:14:15.902804
139	8	9	2026-01-04 16:14:15.902804
140	8	91	2026-01-04 16:14:15.902804
141	8	92	2026-01-04 16:14:15.902804
142	8	93	2026-01-04 16:14:15.902804
143	8	94	2026-01-04 16:14:15.902804
144	8	95	2026-01-04 16:14:15.902804
145	8	96	2026-01-04 16:14:15.902804
146	8	97	2026-01-04 16:14:15.902804
147	8	98	2026-01-04 16:14:15.902804
148	8	99	2026-01-04 16:14:15.902804
149	8	100	2026-01-04 16:14:15.902804
150	8	120	2026-01-04 16:14:15.902804
151	2	105	2026-01-04 16:14:18.541755
152	2	106	2026-01-04 16:14:18.541755
153	3	105	2026-01-04 16:14:18.541755
154	3	106	2026-01-04 16:14:18.541755
155	1	56	2026-01-05 02:19:31.879342
156	4	56	2026-01-05 02:19:31.880239
157	2	120	2026-01-05 02:19:31.880543
158	2	56	2026-01-05 02:19:31.880861
159	3	120	2026-01-05 02:19:31.881299
160	3	56	2026-01-05 02:19:31.881604
161	1	121	2026-01-05 02:19:57.796406
162	8	121	2026-01-05 02:19:57.797797
163	2	121	2026-01-05 02:19:57.79819
164	3	121	2026-01-05 02:19:57.798474
\.


--
-- Data for Name: sys_user; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_user (id, username, password, real_name, email, phone, avatar_url, department_id, "position", employee_no, lawyer_license_no, join_date, compensation_type, can_be_originator, status, last_login_at, last_login_ip, created_at, updated_at, created_by, updated_by, deleted) FROM stdin;
2	lawyer1	$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH	张律师	lawyer1@lawfirm.com	13800138001	\N	1	律师	EMP002	L20240001	\N	COMMISSION	t	ACTIVE	\N	\N	2026-01-05 03:13:37.697736	2026-01-05 03:13:37.697736	\N	\N	f
1	admin	$2a$10$fABtTPpIs5pNRgbd.LxXqeuW3BO5gWblCJHye4pW7A3CkvaGS178S	系统管理员	admin@lawfirm.com	13800138000	\N	1	系统管理员	\N	\N	\N	HYBRID	t	ACTIVE	2026-01-05 02:57:04.449217	0:0:0:0:0:0:0:1	2026-01-04 16:14:15.893998	2026-01-04 16:14:15.893998	\N	\N	f
\.


--
-- Data for Name: sys_user_role; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_user_role (id, user_id, role_id, created_at) FROM stdin;
1	1	1	2026-01-04 16:14:15.894473
2	2	6	2026-01-05 03:13:59.896251
\.


--
-- Name: contract_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.contract_template_id_seq', 5, true);


--
-- Name: finance_commission_rule_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.finance_commission_rule_id_seq', 1, true);


--
-- Name: letter_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.letter_template_id_seq', 4, true);


--
-- Name: sys_department_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_department_id_seq', 6, true);


--
-- Name: sys_menu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_menu_id_seq', 125, true);


--
-- Name: sys_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_role_id_seq', 9, true);


--
-- Name: sys_role_menu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_role_menu_id_seq', 164, true);


--
-- Name: sys_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_user_id_seq', 2, true);


--
-- Name: sys_user_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_user_role_id_seq', 2, true);


--
-- PostgreSQL database dump complete
--

\unrestrict vgXGkMz2seFYB1kCKTn9BiFVkaLvQPFhVb5cRd94IPsZTDmsUai7rRfPBfeWJkH

