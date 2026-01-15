-- =====================================================
-- 律师事务所管理系统 - 卷宗自动归档模板
-- =====================================================
-- 版本: 1.1.0
-- 日期: 2026-01-12
-- 描述: 授权委托书自动归档文档模板
-- 
-- 说明：
-- - 收案审批表：直接使用审批记录数据生成，不需要模板
-- - 委托合同：合同创建时已从模板加载内容，归档时生成快照
-- - 授权委托书：需要模板支持定制，是唯一需要模板的自动归档文档
-- =====================================================

-- 删除旧的不需要的模板（如果存在）
DELETE FROM public.doc_template WHERE template_type IN ('APPROVAL_FORM', 'CONTRACT') AND id IN (101, 103);

-- 授权委托书模板（自动归档专用）
INSERT INTO public.doc_template (id, template_no, name, category_id, template_type, file_name, file_path, file_size, variables, description, status, use_count, version, created_at, updated_at, deleted, content)
VALUES (
    102,
    'TPL-AUTO-002',
    '授权委托书（自动归档）',
    NULL,
    'POWER_OF_ATTORNEY',
    '授权委托书.pdf',
    '/templates/auto/授权委托书.pdf',
    0,
    '客户名称,身份标识,身份号码,客户电话,客户地址,项目名称,案件类型,承办律师,律师执业证号,律所名称,律所地址,代理权限类型,代理权限范围表,审理阶段,当前日',
    '授权委托书模板，用于项目创建时自动生成。支持一般代理和特别代理，可自定义代理权限范围。',
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
    name = EXCLUDED.name,
    content = EXCLUDED.content,
    variables = EXCLUDED.variables,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- 更新序列
SELECT setval('doc_template_id_seq', GREATEST(102, (SELECT MAX(id) FROM doc_template)), true);
