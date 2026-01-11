-- =====================================================
-- 律师事务所管理系统 - 模板初始化数据
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-08
-- 描述: 卷宗模板、文档模板、合同模板、职级、提成规则等
-- =====================================================

--
-- Data for Name: contract_template; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.contract_template VALUES (1, 'CT-001', '标准服务合同', 'SERVICE', 'FIXED', '委托代理合同

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
INSERT INTO public.contract_template VALUES (2, 'CT-002', '常年法律顾问合同', 'RETAINER', 'FIXED', '常年法律顾问合同

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
INSERT INTO public.contract_template VALUES (3, 'CT-003', '民事/行政委托代理合同', 'LITIGATION', 'FIXED', '委 托 代 理 合 同

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
INSERT INTO public.contract_template VALUES (4, 'CT-004', '非诉项目合同', 'NON_LITIGATION', 'HOURLY', '非诉讼法律服务合同

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
INSERT INTO public.contract_template VALUES (5, 'CT-005', '刑事案件委托代理合同', 'LITIGATION', 'FIXED', '委 托 代 理 合 同

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
-- Data for Name: doc_template; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.doc_template VALUES (1, 'TPL-2026-001', '律师函模板', NULL, 'WORD', '律师函模板.docx', '/templates/律师函模板.docx', 10240, '客户名称,客户地址,律所名称,项目编号,项目名称,用户名,今日,事实描述,法律分析,律师意见,截止日期', '通用律师函模板，适用于各类律师函件', 'ACTIVE', 2, 1, '2026-01-08 02:21:42.37542', '2026-01-08 02:21:42.37542', false, NULL, '律 师 函

致：${client.name}
地址：${client.address}

发函单位：${firm.name}
函件编号：${matter.no}
发函日期：${date.today}

关于 ${matter.name} 事宜

尊敬的 ${client.name}：

本律师事务所接受委托，就 ${matter.name} 事宜，特向贵方发送本律师函。

一、事实概述
（请在此填写事实描述）

二、法律分析
（请在此填写法律分析）

三、律师意见
（请在此填写律师意见）

请贵方在收到本函后予以书面答复。

此致

${firm.name}
承办律师：${lawyer.name}
日期：${date.today}');
INSERT INTO public.doc_template VALUES (3, 'TPL-2026-003', '法律意见书模板', NULL, 'WORD', '法律意见书模板.docx', '/templates/法律意见书模板.docx', 12288, '客户名称,律所名称,项目编号,项目名称,用户名,今日,基本事实,法律依据,法律分析,法律意见,风险提示', '法律意见书标准模板', 'ACTIVE', 0, 1, '2026-01-08 02:21:42.37542', '2026-01-08 02:21:42.37542', false, NULL, '法 律 意 见 书

编号：${matter.no}
日期：${date.today}

致：${client.name}

关于 ${matter.name} 的法律意见书

${firm.name} 接受 ${client.name} 的委托，就 ${matter.name} 相关法律问题出具本法律意见书。

一、基本情况
（请在此填写基本情况）

二、相关法律法规
（请在此填写相关法律法规）

三、法律分析
（请在此填写法律分析）

四、法律意见
（请在此填写法律意见）

五、风险提示
（请在此填写风险提示）

本法律意见书仅供 ${client.name} 参考使用。

${firm.name}
承办律师：${lawyer.name}
${date.today}');
INSERT INTO public.doc_template VALUES (4, 'TPL-2026-004', '民事起诉状模板', NULL, 'WORD', '民事起诉状模板.docx', '/templates/民事起诉状模板.docx', 11264, '客户名称,客户地址,客户电话,律所名称,用户名,今日,被告名称,被告地址,被告电话,案由,诉讼请求,事实与理由,法院名称,副本份数,证据数量', '民事起诉状标准模板', 'ACTIVE', 0, 1, '2026-01-08 02:21:42.37542', '2026-01-08 02:21:42.37542', false, NULL, '民 事 起 诉 状

原告：${client.name}
住所地：${client.address}
联系电话：${client.phone}

被告：（被告信息）
住所地：
联系电话：

案由：${matter.name}

诉讼请求：
（请在此填写诉讼请求）

事实与理由：
（请在此填写事实与理由）

综上所述，原告的诉讼请求事实清楚，证据确凿，法律依据充分。为维护原告的合法权益，特向贵院提起诉讼。

此致
人民法院

具状人：${client.name}
代理人：${lawyer.name}
${firm.name}

${date.today}');
INSERT INTO public.doc_template VALUES (2, 'TPL-2026-002', '委托代理合同模板', NULL, 'WORD', '委托代理合同模板.docx', '/templates/委托代理合同模板.docx', 15360, '客户名称,客户电话,客户地址,律所名称,律所地址,项目编号,项目名称,用户名,今日,服务范围,代理权限,费用条款,付款方式,权利义务', '委托代理合同标准模板', 'ACTIVE', 0, 1, '2026-01-08 02:21:42.37542', '2026-01-08 10:32:42.32417', true, 1, '委 托 代 理 合 同

合同编号：${matter.no}

甲方（委托人）：${client.name}
联系电话：${client.phone}
联系地址：${client.address}

乙方（受托人）：${firm.name}
地址：${firm.address}

鉴于甲方因 ${matter.name} 事宜，需要聘请律师提供法律服务，经双方协商一致，签订本合同。

第一条 委托事项
甲方委托乙方就上述事宜提供法律服务。

第二条 代理权限
（一般代理/特别授权）

第三条 律师费用
双方协商确定。

第四条 合同期限
本合同自双方签字盖章之日起生效，至本委托事项办理完毕时终止。

甲方（签章）：                    乙方（签章）：${firm.name}
日期：                           承办律师：${lawyer.name}
                                日期：${date.today}');
--
-- Data for Name: dossier_template; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.dossier_template VALUES (1, '刑事案件卷宗模板', 'CRIMINAL', '适用于刑事辩护案件', true, NULL, '2026-01-06 14:53:13.69281', '2026-01-06 14:53:13.69281', false);
INSERT INTO public.dossier_template VALUES (2, '民事案件卷宗模板', 'CIVIL', '适用于民事诉讼代理案件', true, NULL, '2026-01-06 14:53:13.695714', '2026-01-06 14:53:13.695714', false);
INSERT INTO public.dossier_template VALUES (3, '法律顾问卷宗模板', 'LEGAL_COUNSEL', '适用于常年法律顾问服务', true, NULL, '2026-01-06 14:53:13.696417', '2026-01-06 14:53:13.696417', false);
INSERT INTO public.dossier_template VALUES (4, '其他非诉讼法律事务卷宗模板', 'NON_LITIGATION', '适用于其他非诉讼法律事务', true, NULL, '2026-01-06 14:53:13.696917', '2026-01-06 14:53:13.696917', false);
--
-- Data for Name: dossier_template_item; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.dossier_template_item VALUES (1, 1, 0, '1.收案审批表', 'FILE', 'OTHER', 1, true, '收案审批表', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (4, 1, 0, '4.收费发票', 'FILE', 'OTHER', 4, false, '收费发票复印件', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (5, 1, 0, '5.办案机关卷宗材料', 'FOLDER', 'EVIDENCE', 5, false, '阅卷笔录和卷宗材料', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (6, 1, 0, '6.会见笔录', 'FOLDER', 'MEETING', 6, false, '会见当事人的笔录', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (7, 1, 0, '7.起诉书/上诉状/抗诉书/再审申请书', 'FOLDER', 'PLEADING', 7, false, '诉讼文书', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (8, 1, 0, '8.重大案件集体讨论记录', 'FILE', 'OTHER', 8, false, '重大案件讨论记录', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (9, 1, 0, '9.辩护词', 'FILE', 'PLEADING', 9, false, '辩护词', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (10, 1, 0, '10.裁定书/判决书', 'FOLDER', 'JUDGMENT', 10, false, '法院裁判文书', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (11, 1, 0, '11.工作日志', 'FILE', 'WORKLOG', 11, false, '工作日志', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (12, 1, 0, '12.结案（归档）审批表', 'FILE', 'OTHER', 12, true, '结案审批表', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (13, 1, 0, '13.其他材料', 'FOLDER', 'OTHER', 13, false, '其他相关材料', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (14, 2, 0, '1.收案审批表', 'FILE', 'OTHER', 1, true, '收案审批表', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (17, 2, 0, '4.收费发票', 'FILE', 'OTHER', 4, false, '收费发票复印件', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (18, 2, 0, '5.起诉状/上诉状/答辩状/再审申请书', 'FOLDER', 'PLEADING', 5, false, '诉讼文书', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (19, 2, 0, '6.证据材料', 'FOLDER', 'EVIDENCE', 6, false, '证据材料和阅卷笔录', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (20, 2, 0, '7.重大案件集体讨论记录', 'FILE', 'OTHER', 7, false, '重大案件讨论记录', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (21, 2, 0, '8.代理词', 'FILE', 'PLEADING', 8, false, '代理词', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (22, 2, 0, '9.判决书/裁定书/调解书', 'FOLDER', 'JUDGMENT', 9, false, '法院裁判文书', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (23, 2, 0, '10.工作日志', 'FILE', 'WORKLOG', 10, false, '工作日志', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (25, 2, 0, '12.其他材料', 'FOLDER', 'OTHER', 12, false, '其他相关材料', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (26, 3, 0, '1.顾问合同', 'FILE', 'CONTRACT', 1, true, '法律顾问服务合同', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (27, 3, 0, '2.收费发票', 'FILE', 'OTHER', 2, false, '收费发票复印件', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (28, 3, 0, '3.法律事务记录', 'FOLDER', 'OTHER', 3, false, '办理各类法律事务的记录及相关材料', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (29, 3, 0, '4.合同审查', 'FOLDER', 'OTHER', 4, false, '起草、审查的合同', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (30, 3, 0, '5.规章制度', 'FOLDER', 'OTHER', 5, false, '起草、审查的规章制度', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (31, 3, 0, '6.法律意见书', 'FOLDER', 'PLEADING', 6, false, '出具的法律意见书', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (32, 3, 0, '7.律师函', 'FOLDER', 'CORRESPONDENCE', 7, false, '出具的律师函', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (33, 3, 0, '8.咨询记录', 'FOLDER', 'OTHER', 8, false, '法律咨询记录', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (34, 3, 0, '9.工作日志', 'FILE', 'WORKLOG', 9, false, '工作日志', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (35, 3, 0, '10.工作小结', 'FILE', 'OTHER', 10, false, '年度工作小结', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (36, 3, 0, '11.其他材料', 'FOLDER', 'OTHER', 11, false, '其他相关材料', '2026-01-06 14:53:13.69659', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (37, 4, 0, '1.授权委托书', 'FILE', 'CONTRACT', 1, true, '授权委托书', '2026-01-06 14:53:13.697102', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (38, 4, 0, '2.收费发票', 'FILE', 'OTHER', 2, false, '收费发票复印件', '2026-01-06 14:53:13.697102', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (39, 4, 0, '3.证据材料', 'FOLDER', 'EVIDENCE', 3, false, '相关证据材料', '2026-01-06 14:53:13.697102', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (40, 4, 0, '4.法律意见书/律师函/其他法律文书', 'FOLDER', 'PLEADING', 4, false, '出具的法律文书', '2026-01-06 14:53:13.697102', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (41, 4, 0, '5.工作小结', 'FILE', 'OTHER', 5, false, '工作小结', '2026-01-06 14:53:13.697102', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (42, 4, 0, '6.其他材料', 'FOLDER', 'OTHER', 6, false, '其他相关材料', '2026-01-06 14:53:13.697102', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (15, 2, 0, '2.授权委托书', 'FILE', 'CONTRACT', 2, true, '授权委托书', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (16, 2, 0, '3.委托合同', 'FILE', 'CONTRACT', 3, true, '委托代理合同', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (24, 2, 0, '11.结案（归档）审批表', 'FILE', 'OTHER', 11, true, '结案审批表', '2026-01-06 14:53:13.695896', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (2, 1, 0, '2.授权委托书', 'FILE', 'CONTRACT', 2, true, '授权委托书', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
INSERT INTO public.dossier_template_item VALUES (3, 1, 0, '3.委托合同', 'FILE', 'CONTRACT', 3, true, '委托代理合同', '2026-01-06 14:53:13.694332', NULL, NULL, '2026-01-06 15:18:31.800534', false);
--
-- Data for Name: finance_commission_rule; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.finance_commission_rule VALUES (1, 'STANDARD', '标准方案', NULL, 30.00, 50.00, 15.00, 5.00, true, '常规案件：律所30%、主办50%、协办15%、辅助5%，可在创建合同时修改', true, true, NULL, '2026-01-05 09:22:48.158237', '2026-01-05 09:22:48.158237', NULL, false, 0.00);
INSERT INTO public.finance_commission_rule VALUES (2, 'SOLO', '独立办案', NULL, 30.00, 70.00, 0.00, 0.00, true, '无协办和辅助：律所30%、主办70%', false, true, NULL, '2026-01-05 09:22:48.158237', '2026-01-05 09:22:48.158237', NULL, false, 0.00);
INSERT INTO public.finance_commission_rule VALUES (3, 'NO_FIRM', '律所不提成', NULL, 0.00, 70.00, 20.00, 10.00, true, '律所不参与分配（如特殊约定案件）', false, true, NULL, '2026-01-05 09:22:48.158237', '2026-01-05 09:22:48.158237', NULL, false, 0.00);
INSERT INTO public.finance_commission_rule VALUES (4, 'TEAM_EQUAL', '团队均分', NULL, 20.00, 40.00, 30.00, 10.00, true, '团队项目，主办协办比例接近', false, true, NULL, '2026-01-05 09:22:48.158237', '2026-01-05 09:22:48.158237', NULL, false, 0.00);
INSERT INTO public.finance_commission_rule VALUES (5, 'RETAINER', '顾问项目', NULL, 40.00, 60.00, 0.00, 0.00, true, '常年法律顾问，律所比例较高', false, true, NULL, '2026-01-05 09:22:48.158237', '2026-01-05 09:22:48.158237', NULL, false, 0.00);
INSERT INTO public.finance_commission_rule VALUES (6, 'CUSTOM', '自定义方案', NULL, 0.00, 0.00, 0.00, 0.00, true, '完全自定义，所有比例手动填写', false, true, NULL, '2026-01-05 09:22:48.158237', '2026-01-05 09:22:48.158237', NULL, false, 0.00);
--
-- Data for Name: hr_career_level; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.hr_career_level (level_code, level_name, level_order, category, description, min_work_years, min_matter_count, min_revenue, status, created_at, updated_at, deleted) VALUES
-- 律师通道
('L1', '实习律师', 1, 'LAWYER', '实习期律师，需在指导律师带领下工作', 0, NULL, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('L2', '初级律师', 2, 'LAWYER', '独立执业律师，可独立处理简单案件', 1, 5, 100000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('L3', '中级律师', 3, 'LAWYER', '能独立处理复杂案件，可指导初级律师', 3, 20, 500000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('L4', '高级律师', 4, 'LAWYER', '业务骨干，可带领团队处理重大案件', 5, 50, 1500000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('L5', '资深律师', 5, 'LAWYER', '行业专家，具有较高知名度和影响力', 8, 100, 3000000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('L6', '合伙人', 6, 'LAWYER', '律所合伙人，参与律所管理决策', 10, 150, 5000000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
-- 行政通道
('A1', '行政助理', 1, 'ADMIN', '行政基础岗位', 0, NULL, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('A2', '行政专员', 2, 'ADMIN', '独立负责行政事务', 1, NULL, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('A3', '行政主管', 3, 'ADMIN', '管理行政团队', 3, NULL, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('A4', '行政经理', 4, 'ADMIN', '负责行政部门管理', 5, NULL, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('A5', '行政总监', 5, 'ADMIN', '负责全所行政管理', 8, NULL, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (level_code) DO NOTHING;

--
-- Data for Name: workbench_report_template; Type: TABLE DATA; Schema: public; Owner: -
--

--
-- Name: contract_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: doc_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: dossier_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: dossier_template_item_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: finance_commission_rule_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: hr_career_level_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

--
-- Name: workbench_report_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--
