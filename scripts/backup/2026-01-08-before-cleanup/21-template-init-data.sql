--
-- PostgreSQL database dump
--

\restrict VxCjnC31E3SiHC7Ss4NgFpPSVS5rroOYC6tuEVJrFyf8SDm75QA3ALpHmCu53wY

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

INSERT INTO public.contract_template (id, template_no, name, contract_type, fee_type, content, clauses, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (1, 'CT-001', '标准服务合同', 'SERVICE', 'FIXED', '委托代理合同

甲方（委托人）：${clientName}
乙方（受托人）：${firmName}

鉴于甲方因${matterDescription}事宜，委托乙方提供法律服务，经双方协商一致，订立本合同。

第一条 委托事项
甲方委托乙方就${matterName}事项提供法律服务。

第二条 服务内容
1. 提供法律咨询；
2. 代理参与诉讼/仲裁/调解；
3. 起草、审查法律文书；
4. 其他双方约定的法律服务。

第三条 服务费用
本合同项下法律服务费用为人民币${totalAmount}元整。

第四条 付款方式
${paymentTerms}

第五条 合同期限
本合同自${effectiveDate}起至${expiryDate}止。

第六条 双方权利义务
（一）甲方权利义务
1. 如实向乙方陈述案件事实，提供相关证据材料；
2. 按约定支付法律服务费用；
3. 配合乙方开展法律服务工作。

（二）乙方权利义务
1. 依法维护甲方合法权益；
2. 保守甲方商业秘密和个人隐私；
3. 及时向甲方通报案件进展情况。

第七条 违约责任
任何一方违反本合同约定，应承担相应违约责任。

第八条 争议解决
本合同履行过程中发生争议，双方协商解决；协商不成的，提交乙方所在地人民法院诉讼解决。

甲方（签章）：                    乙方（签章）：
日期：${signDate}                 日期：${signDate}', '[{"title":"服务范围","content":"提供法律咨询、代理诉讼、起草审查法律文书等"},{"title":"保密条款","content":"乙方对甲方的商业秘密和个人隐私负有保密义务"},{"title":"免责条款","content":"因甲方提供虚假信息导致的损失由甲方自行承担"}]', '适用于一般法律服务的标准合同模板', 'ACTIVE', 1, '2026-01-04 16:14:15.903185', '2026-01-04 16:14:15.903185', NULL, NULL, false);
INSERT INTO public.contract_template (id, template_no, name, contract_type, fee_type, content, clauses, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (2, 'CT-002', '常年法律顾问合同', 'RETAINER', 'FIXED', '常年法律顾问合同

甲方（委托人）：${clientName}
乙方（受托人）：${firmName}

第一条 服务期限
自${effectiveDate}起至${expiryDate}止，为期一年。

第二条 服务内容
1. 日常法律咨询；
2. 合同审查与起草；
3. 法律风险评估；
4. 参与重大商务谈判；
5. 代理诉讼/仲裁（另行收费）。

第三条 服务费用
年度顾问费人民币${totalAmount}元整。

第四条 付款方式
${paymentTerms}

第五条 服务方式
1. 电话、邮件咨询不限次数；
2. 每月提供不少于${serviceHours}小时现场服务；
3. 紧急事项24小时响应。

甲方（签章）：                    乙方（签章）：
日期：${signDate}                 日期：${signDate}', '[{"title":"服务响应","content":"常规咨询24小时内响应，紧急事项即时响应"},{"title":"续约条款","content":"合同到期前30日，双方可协商续约事宜"}]', '适用于企业常年法律顾问服务', 'ACTIVE', 2, '2026-01-04 16:14:15.903185', '2026-01-04 16:14:15.903185', NULL, NULL, false);
INSERT INTO public.contract_template (id, template_no, name, contract_type, fee_type, content, clauses, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (3, 'CT-003', '民事/行政委托代理合同', 'LITIGATION', 'FIXED', '委 托 代 理 合 同

（${contractYear}）${contractNo}代字第${contractSeq}号

委托人：${clientName}
住所地：${clientAddress}                     电话：${clientPhone}
受托人：${firmName}
法定代表人（负责人）：${firmLegalRep}        职务：主任
住所地：${firmAddress}，电话：${firmPhone}。

委托人因与${opposingParty}${causeOfAction}纠纷一案，委托人与受托人就委托代理事宜，经平等协商，自愿订立本合同，以供双方共同遵守执行。

一、受托人接受委托后，指派律师${lawyerNames}、律师助理${assistantNames}为委托人的${trialStage}代理人。

二、代理权限为：${authorizationType}（一般代理或特别代理）。
一般代理的权限为：拟写诉讼（仲裁）文书、起诉（立案）、应诉、调查取证、参加庭审、签收诉讼（仲裁）法律文书等诉讼活动。
特别代理的权限（根据相应的代理阶段据实确定）为：承认、变更、放弃诉讼（仲裁）请求，决定是否调解、和解并签订调解、和解协议，提起反诉或上诉，代为申请执行并收付执行款物，以及行使与案件有关的其他诉讼权利。

三、根据《律师服务收费管理办法》及相关规定，委托人应当向受托人支付的代理费大写${totalAmountCN}（¥：${totalAmount}），定于${paymentDeadline}前付清。
委托人应当以直接汇入受托人单位银行账户或支付给持有受托人单位签章票据（收据或发票）的人员的方式付费，不得私自向受托人单位的律师、律师助理或其他工作人员支付任何费用。否则，由此产生的法律后果与受托人单位无关。

四、委托人必须如实全面地向受托人陈述案情，提供证据及证据线索，并不得有下列行为：
1、隐瞒案情；2、伪造证据；3、虚假陈述；4、利用受托人提供法律服务谋取非法利益的其他行为。
受托人发现委托人有前列情形之一时，有权单方终止本合同的履行，所收委托人的一切费用概不予退还。

五、受托人必须尽职尽责，依法维护委托人利益，并不得有下列行为：
1、与委托人的对方当事人及其代理人恶意串通，损害委托人的利益；2、接受委托人的对方当事人及其代理人的吃请或其他贿赂；3、指派其他律师担任同一案件中对方当事人的代理人；4、泄露委托人的商业秘密或个人隐私；5、其他损害委托人合法利益的行为。

六、委托人不得以下列理由解除本合同：
1、与对方当事人自行协商、和解；2、法院主持调解结案；3、委托人自己撤诉或放弃权利；4、对方当事人撤诉或主动履行义务；5、未经法院判决应当解除合同的其他理由。

七、受托人在履行本合同过程中，如出现下列情形，委托人可以解除本合同并要求受托人退费：
1、经人民法院判决确认，受托人实施了损害委托人合法权益的行为；2、受托人无正当理由明确表示不履行代理义务；3、受托人代理义务未履行完毕之前，丧失执业资格。

八、委托人不得以下列理由要求受托人退费：
1、案件败诉；2、裁判文书确定的利益未实现或未能达到委托人的预期目的；3、案件出现本合同第六条规定的情形；4、未经法院判决受托人应当退费的其他理由。

九、因履行本合同过程中引起的或本合同有关的任何争议，不能协商解决的，双方共同选择以下第${disputeResolution}种方式解决：
1.由受托人住所地法院管辖。2.提请${arbitrationCommittee}仲裁委员会按该会仲裁规则进行仲裁。

十、本合同自双方签字和盖章之日起生效。

十一、本合同未尽事宜，由双方本着平等、自愿、合法的原则，协商解决。

十二、本合同一式二份，双方各持一份。

十三、特别约定：${specialTerms}

委托人签章：                            受托人签章：
法定代表人（负责人）：                   负责人签章：
                           ${signDate}', '[{"title":"代理权限说明","content":"一般代理：拟写诉讼文书、起诉、应诉、调查取证、参加庭审、签收法律文书。特别代理：承认、变更、放弃诉讼请求，调解和解，提起反诉或上诉，申请执行并收付执行款物"},{"title":"委托人义务","content":"如实陈述案情，提供证据，不得隐瞒案情、伪造证据、虚假陈述"},{"title":"受托人义务","content":"尽职尽责，依法维护委托人利益，保守商业秘密和个人隐私"},{"title":"费用支付","content":"应直接汇入受托人单位银行账户或支付给持有受托人单位签章票据的人员"}]', '适用于民事案件和行政案件的委托代理合同，符合律师协会规范', 'ACTIVE', 3, '2026-01-04 16:14:15.903185', '2026-01-04 16:14:15.903185', NULL, NULL, false);
INSERT INTO public.contract_template (id, template_no, name, contract_type, fee_type, content, clauses, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (4, 'CT-004', '非诉项目合同', 'NON_LITIGATION', 'HOURLY', '非诉讼法律服务合同

甲方（委托人）：${clientName}
乙方（受托人）：${firmName}

第一条 项目名称
${matterName}

第二条 服务内容
${matterDescription}

第三条 收费方式
本项目采用计时收费方式：
1. 合伙人：人民币${partnerRate}元/小时；
2. 资深律师：人民币${seniorRate}元/小时；
3. 律师助理：人民币${assistantRate}元/小时。

第四条 费用预估
预估总费用人民币${totalAmount}元（实际以工时记录为准）。

第五条 付款方式
${paymentTerms}

甲方（签章）：                    乙方（签章）：
日期：${signDate}                 日期：${signDate}', '[{"title":"工时记录","content":"乙方应如实记录工作时间，定期向甲方提供工时报告"},{"title":"费用上限","content":"如实际费用超出预估20%，乙方应提前告知甲方"}]', '适用于非诉讼项目，按工时计费', 'ACTIVE', 4, '2026-01-04 16:14:15.903185', '2026-01-04 16:14:15.903185', NULL, NULL, false);
INSERT INTO public.contract_template (id, template_no, name, contract_type, fee_type, content, clauses, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (5, 'CT-005', '刑事案件委托代理合同', 'LITIGATION', 'FIXED', '委 托 代 理 合 同

（${contractYear}）刑辩字第${contractSeq}号

委托人：${clientName}                           电话：${clientPhone}
受托人：${firmName}
法定代表人（负责人）：${firmLegalRep}        职务：主任
住所地：${firmAddress}，电话：${firmPhone}。

因${defendantName}涉嫌${criminalCharge}一案，委托人与受托人就委托辩护（代理）事宜，经平等协商，自愿订立本合同，以供双方共同遵守。

一、受托人接受委托后，指派律师${lawyerNames}担任被告人（犯罪嫌疑人、受害人）${defendantName}的${defenseStage}。
A、侦查阶段的律师；B、审查起诉阶段的辩护人；C、一审辩护人；D、二审辩护人；E、死刑复核阶段的辩护人；F、再审辩护人；

二、根据《律师服务收费管理办法》及相关规定，委托人应当向受托人支付辩护费（代理）${totalAmountCN}元（￥${totalAmount}），定于${paymentDeadline}前付清。
委托人应当以直接汇入受托人单位银行账户或支付给持有受托人单位签章票据（收据或发票）的人员的方式付费，不得私自向受托人单位的律师、律师助理或其他工作人员支付任何费用。否则，由此产生的法律后果与受托人单位无关。

三、受托人必须尽职尽责，依法维护被告人（犯罪嫌疑人、受害人）的合法权益，并及时与委托方沟通联系。

四、如出现被告人（犯罪嫌疑人）拒绝受托人指派的律师为其辩护（代理）的情况，除非受托人实施了损害被告人（犯罪嫌疑人）的合法权益的行为，委托人不得要求受托人退还所交费用。受托人是否实施了损害被告人合法权益的行为，须由人民法院判决确认。

五、委托人不得以下列理由要求受托人退还费用：
1、侦查部门撤销案件；2、审查起诉部门决定对犯罪嫌疑人不起诉；3、公诉机关撤回对案件的起诉；4、被告人（犯罪嫌疑人）被取保候审；5、案件的处理结果未达到或未能完全达到委托人的预期目的；6、委托人单方解除本合同；7、被告人（犯罪嫌疑人）拒绝委托人指派的律师为其辩护（代理）。8、其他非经法院判决确认退费的理由。

六、因履行本合同所发生的不能协商解决的争议，由受托人住所地法院管辖。

七、本合同自双方签字、盖章之日起生效。

八、本合同未尽事宜，由双方本着平等、自愿、合法的原则，协商解决。

九、本合同一式二份，双方各持一份。

十、特别约定：${specialTerms}

委托人签章：                                受托人签章：
法定代表人（负责人）：                       负责人签章：
${signDate}', '[{"title":"辩护阶段说明","content":"A-侦查阶段律师、B-审查起诉阶段辩护人、C-一审辩护人、D-二审辩护人、E-死刑复核阶段辩护人、F-再审辩护人"},{"title":"受托人义务","content":"尽职尽责，依法维护被告人（犯罪嫌疑人、受害人）的合法权益，及时与委托方沟通联系"},{"title":"不予退费情形","content":"侦查部门撤销案件、不起诉、撤回起诉、取保候审、结果未达预期、委托人单方解除、被告人拒绝辩护等"},{"title":"费用支付","content":"应直接汇入受托人单位银行账户或支付给持有受托人单位签章票据的人员"}]', '适用于刑事案件的辩护代理合同，包含侦查、审查起诉、一审、二审、死刑复核、再审等阶段', 'ACTIVE', 5, '2026-01-04 16:14:15.903185', '2026-01-04 16:14:15.903185', NULL, NULL, false);


--
-- Data for Name: finance_commission_rule; Type: TABLE DATA; Schema: public; Owner: -
-- 提成预设方案（模板），比例可以为0，律师创建合同时可修改
--

INSERT INTO public.finance_commission_rule (id, rule_code, rule_name, rule_type, firm_rate, lead_lawyer_rate, assist_lawyer_rate, support_staff_rate, allow_modify, description, is_default, active, created_by, created_at, updated_at, updated_by, deleted) VALUES 
(1, 'STANDARD', '标准方案', NULL, 30.00, 50.00, 15.00, 5.00, true, '常规案件：律所30%、主办50%、协办15%、辅助5%，可在创建合同时修改', true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, false),
(2, 'SOLO', '独立办案', NULL, 30.00, 70.00, 0.00, 0.00, true, '无协办和辅助：律所30%、主办70%', false, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, false),
(3, 'NO_FIRM', '律所不提成', NULL, 0.00, 70.00, 20.00, 10.00, true, '律所不参与分配（如特殊约定案件）', false, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, false),
(4, 'TEAM_EQUAL', '团队均分', NULL, 20.00, 40.00, 30.00, 10.00, true, '团队项目，主办协办比例接近', false, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, false),
(5, 'RETAINER', '顾问项目', NULL, 40.00, 60.00, 0.00, 0.00, true, '常年法律顾问，律所比例较高', false, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, false),
(6, 'CUSTOM', '自定义方案', NULL, 0.00, 0.00, 0.00, 0.00, true, '完全自定义，所有比例手动填写', false, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, false);


--
-- Data for Name: letter_template; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.letter_template (id, template_no, name, letter_type, content, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (1, 'LT000001', '律师会见介绍信', 'MEETING', '介绍信

${targetUnit}：

兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}（案号：${matterNo}）一案进行会见工作。

请予接洽为荷。

此致

敬礼

                                    XX律师事务所
                                    ${date}', '用于律师前往看守所、监狱等场所会见当事人', 'ACTIVE', 1, '2026-01-04 16:14:15.83048', '2026-01-04 16:14:15.83048', NULL, NULL, false);
INSERT INTO public.letter_template (id, template_no, name, letter_type, content, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (2, 'LT000002', '律师调查介绍信', 'INVESTIGATION', '介绍信

${targetUnit}：

兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}（案号：${matterNo}）一案进行调查取证工作。

请予协助为荷。

此致

敬礼

                                    XX律师事务所
                                    ${date}', '用于律师前往相关单位调查取证', 'ACTIVE', 2, '2026-01-04 16:14:15.83048', '2026-01-04 16:14:15.83048', NULL, NULL, false);
INSERT INTO public.letter_template (id, template_no, name, letter_type, content, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (3, 'LT000003', '律师阅卷介绍信', 'FILE_REVIEW', '介绍信

${targetUnit}：

兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}（案号：${matterNo}）一案进行阅卷工作。

请予接洽为荷。

此致

敬礼

                                    XX律师事务所
                                    ${date}', '用于律师前往法院、检察院阅卷', 'ACTIVE', 3, '2026-01-04 16:14:15.83048', '2026-01-04 16:14:15.83048', NULL, NULL, false);
INSERT INTO public.letter_template (id, template_no, name, letter_type, content, description, status, sort_order, created_at, updated_at, created_by, updated_by, deleted) VALUES (4, 'LT000004', '通用介绍信', 'INTRODUCTION', '介绍信

${targetUnit}：

兹介绍我所律师${lawyerNames}前往贵单位，就${matterName}一案办理相关事宜。

请予接洽为荷。

此致

敬礼

                                    XX律师事务所
                                    ${date}', '通用介绍信模板', 'ACTIVE', 4, '2026-01-04 16:14:15.83048', '2026-01-04 16:14:15.83048', NULL, NULL, false);


--
-- Name: contract_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.contract_template_id_seq', 5, true);


--
-- Name: finance_commission_rule_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.finance_commission_rule_id_seq', 6, true);


--
-- Name: letter_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.letter_template_id_seq', 4, true);


--
-- PostgreSQL database dump complete
--

\unrestrict VxCjnC31E3SiHC7Ss4NgFpPSVS5rroOYC6tuEVJrFyf8SDm75QA3ALpHmCu53wY

