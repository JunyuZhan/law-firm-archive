import dayjs from 'dayjs';

/**
 * 合同模板预览示例数据
 * 用于模板预览时展示规范的示例内容
 */
export const createContractSampleData = (): Record<string, string> => {
  const currentDate = dayjs();
  const currentYear = currentDate.year();
  const effectiveDate = currentDate;
  const expiryDate = currentDate.add(1, 'year').subtract(1, 'day');
  
  return {
    // ========== 委托人/客户信息 ==========
    clientName: '张三',
    clientIdNumber: '520102199001011234',
    clientAddress: '贵州省贵阳市云岩区中华中路123号',
    clientPhone: '13800138000',
    clientEmail: 'zhangsan@example.com',
    clientPostcode: '550001',
    
    // ========== 律所信息（将从系统配置动态加载）==========
    firmName: '律师事务所',
    firmAddress: '',
    firmPhone: '',
    firmPostcode: '',
    firmLegalPerson: '',
    firmBankName: '',
    firmBankAccount: '',
    
    // ========== 律师信息 ==========
    lawyerName: '王明律师',
    lawyerLicenseNo: '15200101912345678',
    
    // ========== 案件/项目信息 ==========
    matterName: '张三诉李四民间借贷纠纷案',
    causeOfAction: '民间借贷纠纷',
    procedureStage: '一审',
    trialStage: '一审',
    authorityScope: '特别授权',
    
    // ========== 金额信息 ==========
    totalAmount: '50000',
    totalAmountChinese: '伍万元整',
    totalAmountFormatted: '50,000.00',
    claimAmount: '100000',
    claimAmountChinese: '壹拾万元整',
    claimAmountFormatted: '100,000.00',
    
    // ========== 日期信息 ==========
    signDate: effectiveDate.format('YYYY年MM月DD日'),
    effectiveDate: effectiveDate.format('YYYY年MM月DD日'),
    expiryDate: expiryDate.format('YYYY年MM月DD日'),
    currentYear: String(currentYear),
    currentDate: currentDate.format('YYYY年MM月DD日'),
    
    // ========== 合同信息 ==========
    contractNo: `${currentYear}民代字第0001号`,
    paymentTerms: '签订合同时一次性支付',
    
    // ========== 其他当事人信息 ==========
    opposingParty: '李四',
    defendant: '李四',
    relatedParty: '王五（证人）',
  };
};

