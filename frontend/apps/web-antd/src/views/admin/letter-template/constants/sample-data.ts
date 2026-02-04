import dayjs from 'dayjs';

/**
 * 出函模板预览示例数据
 * 用于模板预览时展示规范的示例内容
 * 符合《党政机关公文格式》国家标准（GB/T 9704-2012）
 */
export const createLetterSampleData = (): Record<string, string> => {
  const currentDate = dayjs();
  const currentYear = currentDate.year();

  return {
    // ========== 项目/案件信息 ==========
    matterName: '张三诉李四民间借贷纠纷案',
    matterNo: `M${currentYear}001234`,
    causeOfAction: '民间借贷纠纷',
    caseType: '民事',
    trialStage: '一审',
    procedureStage: '一审',

    // ========== 委托人/客户信息 ==========
    clientName: '张三',
    clientRole: 'PLAINTIFF', // 诉讼地位代码（原告）
    clientRoleName: '原告', // 诉讼地位名称（原告、被告、申请人等）
    clientIdNumber: '520102199001011234',
    clientAddress: '贵州省贵阳市云岩区中华中路123号',
    clientPhone: '13800138000',
    clientEmail: 'zhangsan@example.com',
    clientPostcode: '550001',
    legalRepresentative: '张三',
    creditCode: '',

    // ========== 对方当事人信息 ==========
    opposingParty: '李四',
    opposingPartyRole: 'DEFENDANT', // 对方诉讼地位代码（被告）
    opposingPartyRoleName: '被告', // 对方诉讼地位名称（原告、被告、申请人等）
    opposingLawyerName: '赵律师',
    opposingLawyerFirm: '某某律师事务所',

    // ========== 律师/律所信息 ==========
    lawyerNames: '王明律师、刘强律师',
    lawyerLicenseNo: '15200101912345678',
    firmName: '律师事务所',
    firmAddress: '',
    firmPhone: '',
    firmLicense: '',
    firmLegalPerson: '',

    // ========== 其他可能用到的字段 ==========
    matterType: '诉讼案件',
    businessType: '民事诉讼',

    // ========== 案件相关信息 ==========
    claimAmount: '100,000.00',
    jurisdictionCourt: '贵阳市云岩区人民法院',

    // ========== 函件信息（公文格式）==========
    // 收文单位：使用规范的全称，顶格书写
    targetUnit: '贵阳市云岩区人民法院',
    targetAddress: '贵州省贵阳市云岩区中华中路456号',
    // 函件编号：格式为"律所简称+年份+序号"，如"WD-HJ-2026-0001"
    letterNo: `WD-HJ-${currentYear}-0001`,

    // ========== 日期信息（公文格式）==========
    // 日期格式：YYYY年MM月DD日，如"2026年01月15日"
    date: currentDate.format('YYYY年MM月DD日'),
    currentYear: String(currentYear),
    currentDate: currentDate.format('YYYY年MM月DD日'),

    // ========== 合同信息（如适用）==========
    contractNo: `${currentYear}民代字第0001号`,
  };
};
