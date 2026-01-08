// 合同类型选项
export const contractTypeOptions = [
  { label: '服务合同', value: 'SERVICE' },
  { label: '常年法顾', value: 'RETAINER' },
  { label: '诉讼代理', value: 'LITIGATION' },
  { label: '非诉项目', value: 'NON_LITIGATION' },
];

// 收费方式选项
export const feeTypeOptions = [
  { label: '固定收费', value: 'FIXED' },
  { label: '计时收费', value: 'HOURLY' },
  { label: '风险代理', value: 'CONTINGENCY' },
  { label: '混合收费', value: 'MIXED' },
];

// 预览时的示例数据
export const sampleData: Record<string, string> = {
  clientName: '张三',
  clientIdNumber: '520000199001011234',
  clientAddress: '贵州省贵阳市云岩区XX路XX号',
  clientPhone: '13800138000',
  clientPostcode: '550001',
  firmName: '贵州威迪律师事务所',
  firmAddress: '贵州省毕节市七星关区开行路联邦金座10楼',
  firmPhone: '0857-8228444',
  firmPostcode: '551700',
  firmLegalPerson: '孟天明',
  firmBankName: '中国工商银行毕节分行清毕支行',
  firmBankAccount: '240 607 0209 2000 18990',
  lawyerName: '李律师',
  lawyerLicenseNo: '15200201912345678',
  matterName: '张三与李四民间借贷纠纷案',
  causeOfAction: '民间借贷纠纷',
  procedureStage: '一审',
  authorityScope: '特别授权',
  totalAmount: '50000',
  totalAmountChinese: '伍万元整',
  totalAmountFormatted: '50,000.00',
  claimAmount: '100000',
  claimAmountChinese: '壹拾万元整',
  claimAmountFormatted: '100,000.00',
  signDate: '2026年1月5日',
  effectiveDate: '2026年1月5日',
  expiryDate: '2027年1月4日',
  paymentTerms: '签订合同时一次性支付',
  contractNo: '2026民代字第0001号',
  currentYear: '2026',
  currentDate: '2026年1月5日',
  opposingParty: '李四',
  defendant: '李四',
  relatedParty: '王五（证人）',
};

// 模板列表
export const templateList = [
  { key: 'civil-standard', name: '民事委托代理合同（标准版）', type: 'LITIGATION' },
  { key: 'criminal-standard', name: '刑事委托代理合同（标准版）', type: 'LITIGATION' },
  { key: 'criminal-simple', name: '刑事委托代理合同（简版）', type: 'LITIGATION' },
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
export const defaultTemplates: Record<string, { content: string; name: string; type: string }> = {
  'civil-standard': { 
    content: civilContractStandard, 
    name: '民事委托代理合同（标准版）',
    type: 'LITIGATION'
  },
  'criminal-standard': { 
    content: criminalContractStandard, 
    name: '刑事委托代理合同（标准版）',
    type: 'LITIGATION'
  },
  'criminal-simple': { 
    content: criminalContractSimple, 
    name: '刑事委托代理合同（简版）',
    type: 'LITIGATION'
  },
  'retainer': { 
    content: retainerContract, 
    name: '常年法律顾问合同',
    type: 'RETAINER'
  },
};

