// 模板类型选项（合同类型）
export const templateTypeOptions = [
  { label: '民事代理', value: 'CIVIL_PROXY' },
  { label: '行政代理', value: 'ADMINISTRATIVE_PROXY' },
  { label: '刑事辩护', value: 'CRIMINAL_DEFENSE' },
  { label: '行政国家赔偿', value: 'STATE_COMP_ADMIN' },
  { label: '刑事国家赔偿', value: 'STATE_COMP_CRIMINAL' },
  { label: '法律顾问', value: 'LEGAL_COUNSEL' },
  { label: '非诉案件', value: 'NON_LITIGATION' },
  { label: '自定义模板', value: 'CUSTOM' },
];

// 向后兼容：保留 contractTypeOptions 作为别名
export const contractTypeOptions = templateTypeOptions;

// 收费方式选项
export const feeTypeOptions = [
  { label: '固定收费', value: 'FIXED' },
  { label: '计时收费', value: 'HOURLY' },
  { label: '风险代理', value: 'CONTINGENCY' },
  { label: '混合收费', value: 'MIXED' },
];

// 注意：此文件中的 sampleData 已废弃，请使用 constants/sample-data.ts 中的 createContractSampleData()
// 保留此导出仅用于向后兼容，新代码请使用 createContractSampleData()
// @deprecated 请使用 createContractSampleData() 替代
export const sampleData: Record<string, string> = {};

// 模板列表
export const templateList = [
  {
    key: 'civil-standard',
    name: '民事委托代理合同（标准版）',
    type: 'LITIGATION',
  },
  {
    key: 'criminal-standard',
    name: '刑事委托代理合同（标准版）',
    type: 'LITIGATION',
  },
  {
    key: 'criminal-simple',
    name: '刑事委托代理合同（简版）',
    type: 'LITIGATION',
  },
  { key: 'retainer', name: '常年法律顾问合同', type: 'RETAINER' },
];

// 民事委托代理合同（标准版）
const civilContractStandard = `
<h2 style="text-align: center; letter-spacing: 0.3em;">民事委托合同</h2>
<p style="text-align: center;">合同编号：\${contractNo}</p>

<table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
  <tr>
    <td style="width: 25%; padding: 8px; border: 1px solid #000;"><strong>委托人（甲方）</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${clientName}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>住所地</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${clientAddress}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>公民身份号码</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${clientIdNumber}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>联系电话</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${clientPhone}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>受托人（乙方）</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${firmName}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>负责人</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${firmLegalPerson}</td>
  </tr>
</table>

<p>甲、乙双方根据《中华人民共和国律师法》《中华人民共和国民法典》及其他有关法律规定，经协商一致，订立本合同，以资共同遵守：</p>

<h3>第一条 通知和联系</h3>
<p><strong>1. 甲方通讯地址及联系方法</strong></p>
<p>通讯地址：\${clientAddress}　　邮政编码：\${clientPostcode}</p>
<p>联系人：\${clientName}　　联系电话：\${clientPhone}</p>

<p><strong>2. 乙方通讯地址及联系方法</strong></p>
<p>通讯地址：\${firmAddress}　　邮政编码：\${firmPostcode}</p>
<p>联系电话：\${firmPhone}</p>

<h3>第二条 委托事项</h3>
<p>1. 当事人及案由：<strong>\${matterName}</strong>（案由：\${causeOfAction}）</p>
<p>2. 代理程序：<strong>\${procedureStage}</strong></p>

<h3>第三条 委托权限</h3>
<p>乙方代理权限为：<strong>\${authorityScope}</strong></p>

<h3>第四条 承办律师</h3>
<p>乙方指派律师<strong>\${lawyerName}</strong>（执业证号：\${lawyerLicenseNo}）为甲方代理人。</p>

<h3>第五条 法律服务费</h3>
<p>甲方应向乙方支付代理费人民币<strong>\${totalAmount}</strong>元（大写：\${totalAmountChinese}）。</p>
<p>付款方式：\${paymentTerms}</p>

<h3>第六条 乙方开户银行及账号</h3>
<p>户名：\${firmName}</p>
<p>开户银行：\${firmBankName}</p>
<p>银行账号：\${firmBankAccount}</p>

<p style="margin-top: 3em;">&nbsp;</p>

<table style="width: 100%; border: none;">
  <tr>
    <td style="width: 50%; vertical-align: top; border: none;">
      <p><strong>甲方（签章）：</strong></p>
      <p>&nbsp;</p>
    </td>
    <td style="width: 50%; vertical-align: top; border: none;">
      <p><strong>乙方（签章）：</strong>\${firmName}</p>
      <p><strong>负责人：</strong>\${firmLegalPerson}</p>
    </td>
  </tr>
</table>

<p style="text-align: center; margin-top: 2em;"><strong>签订时间：</strong>\${signDate}</p>
`;

// 刑事委托代理合同（标准版）
const criminalContractStandard = `
<h2 style="text-align: center; letter-spacing: 0.3em;">刑事委托合同</h2>
<p style="text-align: center;">合同编号：\${contractNo}</p>

<table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
  <tr>
    <td style="width: 25%; padding: 8px; border: 1px solid #000;"><strong>委托人（甲方）</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${clientName}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>住所地</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${clientAddress}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>受托人（乙方）</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${firmName}</td>
  </tr>
</table>

<p>甲、乙双方就委托辩护事宜，经协商一致，订立本合同：</p>

<h3>第一条 委托事项</h3>
<p>1. 当事人及案由：<strong>\${defendant}</strong>涉嫌<strong>\${causeOfAction}</strong>一案</p>
<p>2. 代理程序：<strong>\${procedureStage}</strong></p>

<h3>第二条 承办律师</h3>
<p>乙方指派律师<strong>\${lawyerName}</strong>（执业证号：\${lawyerLicenseNo}）为甲方提供辩护服务。</p>

<h3>第三条 律师服务费</h3>
<p>甲方应向乙方支付辩护费人民币<strong>\${totalAmount}</strong>元（大写：\${totalAmountChinese}）。</p>
<p>付款方式：\${paymentTerms}</p>

<p style="margin-top: 3em;">&nbsp;</p>

<table style="width: 100%; border: none;">
  <tr>
    <td style="width: 50%; vertical-align: top; border: none;">
      <p><strong>甲方（签章）：</strong></p>
    </td>
    <td style="width: 50%; vertical-align: top; border: none;">
      <p><strong>乙方（签章）：</strong>\${firmName}</p>
    </td>
  </tr>
</table>

<p style="text-align: center; margin-top: 2em;"><strong>签订时间：</strong>\${signDate}</p>
`;

// 刑事委托代理合同（简版）
const criminalContractSimple = `
<h2 style="text-align: center; letter-spacing: 0.3em;">委托代理合同</h2>
<p style="text-align: center;">合同编号：\${contractNo}</p>

<p><strong>委托人：</strong>\${clientName}　　<strong>电话：</strong>\${clientPhone}</p>
<p><strong>受托人：</strong>\${firmName}</p>

<p>因<strong>\${defendant}</strong>涉嫌<strong>\${causeOfAction}</strong>一案，双方就委托辩护事宜订立本合同。</p>

<p><strong>一、</strong>受托人指派律师<strong>\${lawyerName}</strong>担任\${defendant}的<strong>\${procedureStage}</strong>辩护人。</p>

<p><strong>二、</strong>委托人应向受托人支付辩护费人民币<strong>\${totalAmount}</strong>元（大写：\${totalAmountChinese}）。</p>

<p style="margin-top: 3em;">&nbsp;</p>

<table style="width: 100%; border: none;">
  <tr>
    <td style="width: 50%; border: none;"><strong>委托人签章：</strong></td>
    <td style="width: 50%; border: none;"><strong>受托人签章：</strong>\${firmName}</td>
  </tr>
</table>

<p style="text-align: center; margin-top: 2em;">\${signDate}</p>
`;

// 常年法律顾问合同
const retainerContract = `
<h2 style="text-align: center; letter-spacing: 0.5em;">常年法律顾问合同</h2>
<p style="text-align: center;">合同编号：\${contractNo}</p>

<table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
  <tr>
    <td style="width: 15%; padding: 8px; border: 1px solid #000;"><strong>甲方</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${clientName}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>地址</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${clientAddress}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>乙方</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${firmName}</td>
  </tr>
  <tr>
    <td style="padding: 8px; border: 1px solid #000;"><strong>地址</strong></td>
    <td style="padding: 8px; border: 1px solid #000;">\${firmAddress}</td>
  </tr>
</table>

<p>甲方聘请乙方担任常年法律顾问，经协商达成如下协议：</p>

<h3>第一条 服务内容</h3>
<p>乙方为甲方提供法律咨询、合同审核、重大决策法律意见等服务。</p>

<h3>第二条 服务期限</h3>
<p>本合同有效期为<strong>壹年</strong>，自<strong>\${effectiveDate}</strong>起至<strong>\${expiryDate}</strong>止。</p>

<h3>第三条 顾问费用</h3>
<p>年度顾问费为人民币<strong>\${totalAmount}</strong>元（大写：\${totalAmountChinese}）。</p>
<p>付款方式：\${paymentTerms}</p>

<p style="margin-top: 3em;">&nbsp;</p>

<table style="width: 100%; border: none;">
  <tr>
    <td style="width: 50%; vertical-align: top; border: none;">
      <p><strong>甲方（盖章）：</strong></p>
      <p><strong>日期：</strong></p>
    </td>
    <td style="width: 50%; vertical-align: top; border: none;">
      <p><strong>乙方（盖章）：</strong>\${firmName}</p>
      <p><strong>日期：</strong>\${signDate}</p>
    </td>
  </tr>
</table>
`;

// 默认模板集合
export const defaultTemplates: Record<
  string,
  { content: string; name: string; type: string }
> = {
  'civil-standard': {
    content: civilContractStandard,
    name: '民事委托代理合同（标准版）',
    type: 'LITIGATION',
  },
  'criminal-standard': {
    content: criminalContractStandard,
    name: '刑事委托代理合同（标准版）',
    type: 'LITIGATION',
  },
  'criminal-simple': {
    content: criminalContractSimple,
    name: '刑事委托代理合同（简版）',
    type: 'LITIGATION',
  },
  retainer: {
    content: retainerContract,
    name: '常年法律顾问合同',
    type: 'RETAINER',
  },
};
