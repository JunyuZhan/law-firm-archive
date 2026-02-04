-- =====================================================
-- 模板菜单位置调整
-- =====================================================
-- 日期: 2026-02-03
-- 描述: 将合同模板和出函模板从系统管理移到各自的业务模块
--   - 出函模板: 系统管理 → 行政管理
--   - 合同模板: 系统管理 → 财务管理
-- =====================================================

-- 1. 移动出函模板到行政管理 (parent_id: 2→9)
UPDATE sys_menu 
SET 
    parent_id = 9,
    path = '/admin/letter-template',
    component = 'admin/letter-template/index',
    sort_order = 20,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 27;

-- 2. 移动合同模板到财务管理 (parent_id: 2→5)
UPDATE sys_menu 
SET 
    parent_id = 5,
    path = '/finance/contract-template',
    component = 'finance/contract-template/index',
    sort_order = 20,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 28;

-- 验证菜单调整结果
SELECT id, parent_id, name, path, component 
FROM sys_menu 
WHERE id IN (27, 28);

-- =====================================================
-- 函件模板示例数据（可删除）
-- =====================================================
INSERT INTO public.letter_template (id, template_no, name, letter_type, content, description, status, sort_order, version, created_at, updated_at, created_by, deleted)
VALUES 
(1, 'LT-001', '律师介绍信（通用）', 'INTRODUCTION', 
'致：${recipientUnit}

${firmName}接受${clientName}的委托，指派${lawyerName}律师（执业证号：${licenseNo}）处理${matterDescription}相关法律事务。

现持此函前往贵单位，请予以接洽，协助办理相关事宜。

特此致函。

${firmName}
${date}

联系电话：${firmPhone}
地址：${firmAddress}', 
'通用介绍信模板，用于律师外出办事', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),

(2, 'LT-002', '会见函（看守所）', 'MEETING', 
'致：${detentionCenter}

${firmName}依法接受${clientName}（或其近亲属${familyName}）的委托，指派${lawyerName}律师（执业证号：${licenseNo}）担任犯罪嫌疑人/被告人${suspectName}涉嫌${crimeName}一案的辩护人。

根据《中华人民共和国刑事诉讼法》第三十九条之规定，辩护律师可以同在押的犯罪嫌疑人、被告人会见和通信。

现持此函及相关委托手续，申请会见犯罪嫌疑人/被告人${suspectName}，请予以安排。

${firmName}
${date}

承办律师：${lawyerName}
联系电话：${lawyerPhone}', 
'看守所会见函模板', 'ACTIVE', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),

(3, 'LT-003', '调查函（通用）', 'INVESTIGATION', 
'致：${recipientUnit}

${firmName}接受${clientName}的委托，指派${lawyerName}律师处理${matterDescription}相关法律事务。

为维护委托人的合法权益，现根据《中华人民共和国律师法》第三十五条之规定，需向贵单位调查了解以下事项：

${investigationItems}

请贵单位予以协助，提供上述信息或相关证明材料。

特此致函，恳请支持！

${firmName}
${date}

承办律师：${lawyerName}
联系电话：${lawyerPhone}', 
'通用调查函模板', 'ACTIVE', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),

(4, 'LT-004', '阅卷函（检察院/法院）', 'FILE_REVIEW', 
'致：${courtOrProcuratorate}

${firmName}依法接受委托，指派${lawyerName}律师（执业证号：${licenseNo}）担任${caseName}一案${partyName}的诉讼代理人/辩护人。

根据《中华人民共和国刑事诉讼法》/《中华人民共和国民事诉讼法》相关规定，辩护人/诉讼代理人有权查阅、摘抄、复制本案的案卷材料。

现持此函及相关委托手续，申请查阅、复制该案全部卷宗材料，请予以安排。

${firmName}
${date}

承办律师：${lawyerName}
联系电话：${lawyerPhone}', 
'检察院/法院阅卷函模板', 'ACTIVE', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false),

(5, 'LT-005', '法律意见函（通用）', 'LEGAL_OPINION', 
'致：${recipientName}

${firmName}接受您的委托，就${consultationMatter}事宜出具法律意见如下：

一、基本事实
${factDescription}

二、法律分析
${legalAnalysis}

三、法律意见
${legalOpinion}

四、风险提示
${riskWarning}

以上意见仅供参考，具体事宜建议进一步咨询或委托专业律师处理。

${firmName}
${lawyerName} 律师
${date}

【声明】本法律意见函仅基于委托人提供的材料和陈述作出，如有重要事实未予披露或与实际情况不符，可能影响本意见的准确性。', 
'法律意见函模板', 'ACTIVE', 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, false)

ON CONFLICT (id) DO UPDATE SET 
    template_no = EXCLUDED.template_no,
    name = EXCLUDED.name,
    letter_type = EXCLUDED.letter_type,
    content = EXCLUDED.content,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- 验证函件模板
SELECT id, template_no, name, letter_type FROM letter_template WHERE deleted = false;
