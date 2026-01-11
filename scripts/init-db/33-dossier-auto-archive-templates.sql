-- =====================================================
-- 律师事务所管理系统 - 卷宗自动归档模板
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-12
-- 描述: 收案审批表、授权委托书等自动归档文档模板
-- =====================================================

-- 收案审批表模板
INSERT INTO public.doc_template (id, template_no, name, category_id, template_type, file_name, file_path, file_size, variables, description, status, use_count, version, created_at, updated_at, deleted, content)
VALUES (
    101,
    'TPL-AUTO-001',
    '收案审批表',
    NULL,
    'APPROVAL_FORM',
    '收案审批表.pdf',
    '/templates/auto/收案审批表.pdf',
    0,
    '["contract.no", "contract.signDate", "client.name", "client.phone", "client.address", "matter.name", "matter.caseType", "matter.description", "contract.totalAmount", "contract.feeType", "approval.status", "approval.approverName", "approval.approvedAt", "approval.comment", "lawyer.name", "date.today"]',
    '收案审批表模板，用于记录案件收案审批信息，支持自定义格式',
    'ACTIVE',
    0,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    false,
    '                          收 案 审 批 表

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

合同编号：${contract.no}                     申请日期：${contract.signDate}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                          【委托人信息】

委托人名称：${client.name}
联系电话：${client.phone}
联系地址：${client.address}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                          【案件信息】

项目名称：${matter.name}
案件类型：${matter.caseTypeName}

案情简介：
${matter.description}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                          【收费信息】

律师费用：人民币 ${contract.totalAmount} 元
收费方式：${contract.feeTypeName}
付款条款：${contract.paymentTerms}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                          【审批意见】

审批状态：${approval.statusName}
审 批 人：${approval.approverName}
审批时间：${approval.approvedAt}
审批意见：${approval.comment}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                          【签字确认】

申请人签字：________________          审批人签字：________________

日    期：________________          日    期：________________

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

承办律师：${lawyer.name}              生成日期：${date.today}

【本表由系统自动生成，以签字盖章版本为准】
')
ON CONFLICT (id) DO UPDATE SET
    content = EXCLUDED.content,
    variables = EXCLUDED.variables,
    updated_at = CURRENT_TIMESTAMP;

-- 授权委托书模板
INSERT INTO public.doc_template (id, template_no, name, category_id, template_type, file_name, file_path, file_size, variables, description, status, use_count, version, created_at, updated_at, deleted, content)
VALUES (
    102,
    'TPL-AUTO-002',
    '授权委托书',
    NULL,
    'POWER_OF_ATTORNEY',
    '授权委托书.pdf',
    '/templates/auto/授权委托书.pdf',
    0,
    '["client.name", "client.type", "client.idNumber", "client.phone", "client.address", "matter.name", "matter.caseTypeName", "lawyer.name", "lawyer.licenseNo", "firm.name", "firm.address", "authorizationType", "authorizationScope", "trialStage", "date.today"]',
    '授权委托书模板，支持一般代理和特别代理，可自定义代理权限范围',
    'ACTIVE',
    0,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    false,
    '                          授 权 委 托 书


                          【委托人信息】

委托人：${client.name}
${client.idLabel}：${client.idNumber}
联系电话：${client.phone}
住所地址：${client.address}


                          【受托人信息】

受托人：${firm.name}
承办律师：${lawyer.name}
执业证号：${lawyer.licenseNo}
律所地址：${firm.address}


                          【委托事项】

    本人因 ${matter.name}（${matter.caseTypeName}）一案，特委托上述受托人
作为本人的诉讼代理人。

代理阶段：${trialStage}


                          【代理权限】

代理权限类型：${authorizationType}

${authorizationScope}


                          【委托期限】

本委托书自签署之日起至本案${trialStage}结案止。


                          【签字确认】

委托人（签章）：________________

日    期：    年  月  日


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

生成日期：${date.today}
【本授权委托书由系统自动生成，以签字盖章版本为准】
')
ON CONFLICT (id) DO UPDATE SET
    content = EXCLUDED.content,
    variables = EXCLUDED.variables,
    updated_at = CURRENT_TIMESTAMP;

-- 委托合同模板（用于自动归档）
INSERT INTO public.doc_template (id, template_no, name, category_id, template_type, file_name, file_path, file_size, variables, description, status, use_count, version, created_at, updated_at, deleted, content)
VALUES (
    103,
    'TPL-AUTO-003',
    '委托代理合同（自动归档）',
    NULL,
    'CONTRACT',
    '委托代理合同.pdf',
    '/templates/auto/委托代理合同.pdf',
    0,
    '["contract.no", "contract.year", "client.name", "client.phone", "client.address", "firm.name", "firm.address", "matter.name", "matter.opposingParty", "matter.causeOfAction", "lawyer.name", "assistantNames", "trialStage", "authorizationType", "contract.totalAmount", "contract.totalAmountCN", "contract.paymentTerms", "specialTerms", "contract.signDate", "date.today"]',
    '委托代理合同模板，用于自动归档，内容与合同模块一致',
    'ACTIVE',
    0,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    false,
    '                     委 托 代 理 合 同

（${contract.year}）${contract.no}

委托人：${client.name}
住所地：${client.address}                     电话：${client.phone}
受托人：${firm.name}
住所地：${firm.address}

委托人因与${matter.opposingParty}${matter.causeOfAction}纠纷一案，委托人与受托人就委托代理事宜，经平等协商，自愿订立本合同，以供双方共同遵守执行。

一、受托人接受委托后，指派律师${lawyer.name}${assistantNames}为委托人的${trialStage}代理人。

二、代理权限为：${authorizationType}（一般代理或特别代理）。
一般代理的权限为：拟写诉讼（仲裁）文书、起诉（立案）、应诉、调查取证、参加庭审、签收诉讼（仲裁）法律文书等诉讼活动。
特别代理的权限（根据相应的代理阶段据实确定）为：承认、变更、放弃诉讼（仲裁）请求，决定是否调解、和解并签订调解、和解协议，提起反诉或上诉，代为申请执行并收付执行款物，以及行使与案件有关的其他诉讼权利。

三、根据《律师服务收费管理办法》及相关规定，委托人应当向受托人支付的代理费大写${contract.totalAmountCN}（¥：${contract.totalAmount}），定于合同签订后付清。
委托人应当以直接汇入受托人单位银行账户或支付给持有受托人单位签章票据（收据或发票）的人员的方式付费，不得私自向受托人单位的律师、律师助理或其他工作人员支付任何费用。否则，由此产生的法律后果与受托人单位无关。

四、委托人必须如实全面地向受托人陈述案情，提供证据及证据线索，并不得有下列行为：
1、隐瞒案情；2、伪造证据；3、虚假陈述；4、利用受托人提供法律服务谋取非法利益的其他行为。
受托人发现委托人有前列情形之一时，有权单方终止本合同的履行，所收委托人的一切费用概不予退还。

五、受托人必须尽职尽责，依法维护委托人利益，并不得有下列行为：
1、与委托人的对方当事人及其代理人恶意串通，损害委托人的利益；2、接受委托人的对方当事人及其代理人的吃请或其他贿赂；3、指派其他律师担任同一案件中对方当事人的代理人；4、泄露委托人的商业秘密或个人隐私；5、其他损害委托人合法利益的行为。

六、本合同自双方签字和盖章之日起生效。

七、本合同未尽事宜，由双方本着平等、自愿、合法的原则，协商解决。

八、本合同一式二份，双方各持一份。

九、特别约定：${specialTerms}

委托人签章：                            受托人签章：
法定代表人（负责人）：                   负责人签章：
                           ${contract.signDate}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
生成日期：${date.today}
【本合同由系统自动生成，以签字盖章版本为准】
')
ON CONFLICT (id) DO UPDATE SET
    content = EXCLUDED.content,
    variables = EXCLUDED.variables,
    updated_at = CURRENT_TIMESTAMP;

-- 更新序列
SELECT setval('doc_template_id_seq', GREATEST(103, (SELECT MAX(id) FROM doc_template)), true);

