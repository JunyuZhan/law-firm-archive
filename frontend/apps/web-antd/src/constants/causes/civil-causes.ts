/**
 * 民事案由
 * 根据《民事案件案由规定》（2025年12月修正）
 * 共514个案由
 */
import type { CauseCategory } from './types';

/** 一、人格权纠纷 */
export const CIVIL_PART1_PERSONALITY: CauseCategory = {
  code: 'P1',
  name: '人格权纠纷',
  causes: [
    { code: '1', name: '生命权、身体权、健康权纠纷' },
    { code: '2', name: '姓名权纠纷' },
    { code: '3', name: '名称权纠纷' },
    { code: '4', name: '肖像权纠纷' },
    { code: '5', name: '声音保护纠纷' },
    { code: '6', name: '名誉权纠纷' },
    { code: '7', name: '荣誉权纠纷' },
    {
      code: '8',
      name: '隐私权、个人信息保护纠纷',
      children: [
        { code: '8.1', name: '隐私权纠纷' },
        { code: '8.2', name: '个人信息保护纠纷' },
      ],
    },
    { code: '9', name: '婚姻自主权纠纷' },
    { code: '10', name: '人身自由权纠纷' },
    {
      code: '11',
      name: '一般人格权纠纷',
      children: [{ code: '11.1', name: '平等就业权纠纷' }],
    },
  ],
};

/** 二、婚姻家庭纠纷 */
export const CIVIL_PART2_MARRIAGE: CauseCategory = {
  code: 'P2',
  name: '婚姻家庭纠纷',
  causes: [
    { code: '12', name: '婚约财产纠纷' },
    { code: '13', name: '婚内夫妻财产分割纠纷' },
    { code: '14', name: '离婚纠纷' },
    { code: '15', name: '离婚后财产纠纷' },
    { code: '16', name: '离婚后损害责任纠纷' },
    { code: '17', name: '婚姻无效纠纷' },
    { code: '18', name: '撤销婚姻纠纷' },
    { code: '19', name: '夫妻财产约定纠纷' },
    {
      code: '20',
      name: '同居关系纠纷',
      children: [
        { code: '20.1', name: '同居关系析产纠纷' },
        { code: '20.2', name: '同居关系子女抚养纠纷' },
      ],
    },
    {
      code: '21',
      name: '亲子关系纠纷',
      children: [
        { code: '21.1', name: '确认亲子关系纠纷' },
        { code: '21.2', name: '否认亲子关系纠纷' },
      ],
    },
    {
      code: '22',
      name: '抚养纠纷',
      children: [
        { code: '22.1', name: '抚养费纠纷' },
        { code: '22.2', name: '变更抚养关系纠纷' },
      ],
    },
    {
      code: '23',
      name: '扶养纠纷',
      children: [
        { code: '23.1', name: '扶养费纠纷' },
        { code: '23.2', name: '变更扶养关系纠纷' },
      ],
    },
    {
      code: '24',
      name: '赡养纠纷',
      children: [
        { code: '24.1', name: '赡养费纠纷' },
        { code: '24.2', name: '变更赡养关系纠纷' },
      ],
    },
    {
      code: '25',
      name: '收养关系纠纷',
      children: [
        { code: '25.1', name: '确认收养关系纠纷' },
        { code: '25.2', name: '解除收养关系纠纷' },
      ],
    },
    { code: '26', name: '监护权纠纷' },
    { code: '27', name: '探望权纠纷' },
    { code: '28', name: '分家析产纠纷' },
  ],
};

/** 三、继承纠纷 */
export const CIVIL_PART3_INHERITANCE: CauseCategory = {
  code: 'P3',
  name: '继承纠纷',
  causes: [
    {
      code: '29',
      name: '法定继承纠纷',
      children: [
        { code: '29.1', name: '转继承纠纷' },
        { code: '29.2', name: '代位继承纠纷' },
      ],
    },
    { code: '30', name: '遗嘱继承纠纷' },
    { code: '31', name: '被继承人债务清偿纠纷' },
    { code: '32', name: '遗赠纠纷' },
    { code: '33', name: '遗赠扶养协议纠纷' },
    { code: '34', name: '非遗产继承人分配遗产纠纷' },
    {
      code: '35',
      name: '遗产管理纠纷',
      children: [
        { code: '35.1', name: '遗产管理人责任纠纷' },
        { code: '35.2', name: '遗产管理人报酬纠纷' },
      ],
    },
  ],
};

/** 四、不动产登记纠纷 */
export const CIVIL_PART4_PROPERTY_REGISTRATION: CauseCategory = {
  code: 'P4',
  name: '不动产登记纠纷',
  causes: [
    { code: '36', name: '异议登记不当损害责任纠纷' },
    { code: '37', name: '虚假登记损害责任纠纷' },
  ],
};

/** 五、物权保护纠纷 */
export const CIVIL_PART5_PROPERTY_PROTECTION: CauseCategory = {
  code: 'P5',
  name: '物权保护纠纷',
  causes: [
    {
      code: '38',
      name: '物权确认纠纷',
      children: [
        { code: '38.1', name: '所有权确认纠纷' },
        { code: '38.2', name: '用益物权确认纠纷' },
        { code: '38.3', name: '担保物权确认纠纷' },
      ],
    },
    { code: '39', name: '返还原物纠纷' },
    { code: '40', name: '排除妨害纠纷' },
    { code: '41', name: '消除危险纠纷' },
    { code: '42', name: '修理、重作、更换纠纷' },
    { code: '43', name: '恢复原状纠纷' },
    { code: '44', name: '财产损害赔偿纠纷' },
  ],
};


/** 六、所有权纠纷 */
export const CIVIL_PART6_OWNERSHIP: CauseCategory = {
  code: 'P6',
  name: '所有权纠纷',
  causes: [
    { code: '45', name: '侵害集体经济组织成员权益纠纷' },
    { code: '46', name: '侵害集体经济组织权益纠纷' },
    {
      code: '47',
      name: '建筑物区分所有权纠纷',
      children: [
        { code: '47.1', name: '业主专有权纠纷' },
        { code: '47.2', name: '业主共有权纠纷' },
        { code: '47.3', name: '车位纠纷' },
        { code: '47.4', name: '车库纠纷' },
      ],
    },
    { code: '48', name: '业主撤销权纠纷' },
    { code: '49', name: '业主知情权纠纷' },
    { code: '50', name: '遗失物返还纠纷' },
    { code: '51', name: '漂流物返还纠纷' },
    { code: '52', name: '埋藏物返还纠纷' },
    { code: '53', name: '隐藏物返还纠纷' },
    { code: '54', name: '添附物归属纠纷' },
    {
      code: '55',
      name: '相邻关系纠纷',
      children: [
        { code: '55.1', name: '相邻用水、排水纠纷' },
        { code: '55.2', name: '相邻通行纠纷' },
        { code: '55.3', name: '相邻土地、建筑物利用关系纠纷' },
        { code: '55.4', name: '相邻通风纠纷' },
        { code: '55.5', name: '相邻采光、日照纠纷' },
        { code: '55.6', name: '相邻污染侵害纠纷' },
        { code: '55.7', name: '相邻损害防免关系纠纷' },
      ],
    },
    {
      code: '56',
      name: '共有纠纷',
      children: [
        { code: '56.1', name: '共有权确认纠纷' },
        { code: '56.2', name: '共有物分割纠纷' },
        { code: '56.3', name: '共有人优先购买权纠纷' },
        { code: '56.4', name: '债权人代位析产纠纷' },
      ],
    },
    { code: '57', name: '自然资源资产损害赔偿纠纷' },
  ],
};

/** 七、用益物权纠纷 */
export const CIVIL_PART7_USUFRUCT: CauseCategory = {
  code: 'P7',
  name: '用益物权纠纷',
  causes: [
    { code: '58', name: '海域使用权纠纷' },
    { code: '59', name: '探矿权纠纷' },
    { code: '60', name: '采矿权纠纷' },
    { code: '61', name: '矿产资源压覆补偿纠纷' },
    { code: '62', name: '取水权纠纷' },
    { code: '63', name: '养殖权纠纷' },
    { code: '64', name: '捕捞权纠纷' },
    {
      code: '65',
      name: '土地承包经营权纠纷',
      children: [
        { code: '65.1', name: '土地承包经营权确认纠纷' },
        { code: '65.2', name: '承包地征收补偿费用分配纠纷' },
        { code: '65.3', name: '土地承包经营权继承纠纷' },
      ],
    },
    { code: '66', name: '土地经营权纠纷' },
    { code: '67', name: '建设用地使用权纠纷' },
    { code: '68', name: '宅基地使用权纠纷' },
    { code: '69', name: '居住权纠纷' },
    { code: '70', name: '地役权纠纷' },
  ],
};

/** 八、担保物权纠纷 */
export const CIVIL_PART8_SECURITY: CauseCategory = {
  code: 'P8',
  name: '担保物权纠纷',
  causes: [
    {
      code: '71',
      name: '抵押权纠纷',
      children: [
        { code: '71.1', name: '建筑物和其他土地附着物抵押权纠纷' },
        { code: '71.2', name: '在建建筑物抵押权纠纷' },
        { code: '71.3', name: '建设用地使用权抵押权纠纷' },
        { code: '71.4', name: '土地经营权抵押权纠纷' },
        { code: '71.5', name: '探矿权抵押权纠纷' },
        { code: '71.6', name: '采矿权抵押权纠纷' },
        { code: '71.7', name: '海域使用权抵押权纠纷' },
        { code: '71.8', name: '动产抵押权纠纷' },
        { code: '71.9', name: '在建船舶、航空器抵押权纠纷' },
        { code: '71.10', name: '动产浮动抵押权纠纷' },
        { code: '71.11', name: '最高额抵押权纠纷' },
      ],
    },
    {
      code: '72',
      name: '质权纠纷',
      children: [
        { code: '72.1', name: '动产质权纠纷' },
        { code: '72.2', name: '转质权纠纷' },
        { code: '72.3', name: '最高额质权纠纷' },
        { code: '72.4', name: '票据质权纠纷' },
        { code: '72.5', name: '债券质权纠纷' },
        { code: '72.6', name: '存单质权纠纷' },
        { code: '72.7', name: '仓单质权纠纷' },
        { code: '72.8', name: '提单质权纠纷' },
        { code: '72.9', name: '股权质权纠纷' },
        { code: '72.10', name: '基金份额质权纠纷' },
        { code: '72.11', name: '知识产权质权纠纷' },
        { code: '72.12', name: '应收账款质权纠纷' },
        { code: '72.13', name: '环境资源相关权利质权纠纷' },
      ],
    },
    { code: '73', name: '留置权纠纷' },
  ],
};

/** 九、占有保护纠纷 */
export const CIVIL_PART9_POSSESSION: CauseCategory = {
  code: 'P9',
  name: '占有保护纠纷',
  causes: [
    { code: '74', name: '占有物返还纠纷' },
    { code: '75', name: '占有排除妨害纠纷' },
    { code: '76', name: '占有消除危险纠纷' },
    { code: '77', name: '占有物损害赔偿纠纷' },
  ],
};


/** 十、合同纠纷 */
export const CIVIL_PART10_CONTRACT: CauseCategory = {
  code: 'P10',
  name: '合同纠纷',
  causes: [
    { code: '78', name: '缔约过失责任纠纷' },
    { code: '79', name: '预约合同纠纷' },
    {
      code: '80',
      name: '确认合同效力纠纷',
      children: [
        { code: '80.1', name: '确认合同有效纠纷' },
        { code: '80.2', name: '确认合同无效纠纷' },
      ],
    },
    { code: '81', name: '债权人代位权纠纷' },
    { code: '82', name: '债权人撤销权纠纷' },
    { code: '83', name: '债权转让合同纠纷' },
    { code: '84', name: '债务转移合同纠纷' },
    { code: '85', name: '债权债务概括转移合同纠纷' },
    { code: '86', name: '债务加入纠纷' },
    { code: '87', name: '悬赏广告纠纷' },
    {
      code: '88',
      name: '买卖合同纠纷',
      children: [
        { code: '88.1', name: '分期付款买卖合同纠纷' },
        { code: '88.2', name: '凭样品买卖合同纠纷' },
        { code: '88.3', name: '试用买卖合同纠纷' },
        { code: '88.4', name: '所有权保留买卖合同纠纷' },
        { code: '88.5', name: '招标投标买卖合同纠纷' },
        { code: '88.6', name: '互易纠纷' },
        { code: '88.7', name: '国际货物买卖合同纠纷' },
        { code: '88.8', name: '信息网络买卖合同纠纷' },
      ],
    },
    { code: '89', name: '拍卖合同纠纷' },
    {
      code: '90',
      name: '建设用地使用权合同纠纷',
      children: [
        { code: '90.1', name: '建设用地使用权出让合同纠纷' },
        { code: '90.2', name: '建设用地使用权转让合同纠纷' },
      ],
    },
    {
      code: '91',
      name: '临时用地合同纠纷',
      children: [{ code: '91.1', name: '勘查、开采矿产临时用地合同纠纷' }],
    },
    { code: '92', name: '探矿权转让合同纠纷' },
    { code: '93', name: '采矿权转让合同纠纷' },
    { code: '94', name: '矿产资源压覆补偿合同纠纷' },
    {
      code: '95',
      name: '房地产开发经营合同纠纷',
      children: [
        { code: '95.1', name: '委托代建合同纠纷' },
        { code: '95.2', name: '合资、合作开发房地产合同纠纷' },
        { code: '95.3', name: '项目转让合同纠纷' },
      ],
    },
    {
      code: '96',
      name: '房屋买卖合同纠纷',
      children: [
        { code: '96.1', name: '商品房预约合同纠纷' },
        { code: '96.2', name: '商品房预售合同纠纷' },
        { code: '96.3', name: '商品房销售合同纠纷' },
        { code: '96.4', name: '商品房委托代理销售合同纠纷' },
        { code: '96.5', name: '经济适用房转让合同纠纷' },
        { code: '96.6', name: '农村房屋买卖合同纠纷' },
      ],
    },
    { code: '97', name: '民事主体间房屋拆迁补偿合同纠纷' },
    { code: '98', name: '供用电合同纠纷' },
    { code: '99', name: '供用水合同纠纷' },
    { code: '100', name: '供用气合同纠纷' },
    { code: '101', name: '供用热力合同纠纷' },
    { code: '102', name: '排污权交易纠纷' },
    { code: '103', name: '用能权交易纠纷' },
    { code: '104', name: '用水权交易纠纷' },
    { code: '105', name: '碳排放权交易纠纷' },
    { code: '106', name: '碳汇交易纠纷' },
    {
      code: '107',
      name: '赠与合同纠纷',
      children: [
        { code: '107.1', name: '公益事业捐赠合同纠纷' },
        { code: '107.2', name: '附义务赠与合同纠纷' },
      ],
    },
    {
      code: '108',
      name: '借款合同纠纷',
      children: [
        { code: '108.1', name: '金融借款合同纠纷' },
        { code: '108.2', name: '同业拆借纠纷' },
        { code: '108.3', name: '民间借贷纠纷' },
        { code: '108.4', name: '小额借款合同纠纷' },
        { code: '108.5', name: '金融不良债权转让合同纠纷' },
        { code: '108.6', name: '金融不良债权追偿纠纷' },
      ],
    },
    { code: '109', name: '保证合同纠纷' },
    { code: '110', name: '抵押合同纠纷' },
    { code: '111', name: '质押合同纠纷' },
    { code: '112', name: '定金合同纠纷' },
    { code: '113', name: '进出口押汇纠纷' },
    { code: '114', name: '储蓄存款合同纠纷' },
    {
      code: '115',
      name: '银行卡纠纷',
      children: [
        { code: '115.1', name: '借记卡纠纷' },
        { code: '115.2', name: '信用卡纠纷' },
      ],
    },
    {
      code: '116',
      name: '租赁合同纠纷',
      children: [
        { code: '116.1', name: '土地租赁合同纠纷' },
        { code: '116.2', name: '房屋租赁合同纠纷' },
        { code: '116.3', name: '车辆租赁合同纠纷' },
        { code: '116.4', name: '建筑设备租赁合同纠纷' },
      ],
    },
    { code: '117', name: '融资租赁合同纠纷' },
    {
      code: '118',
      name: '保理合同纠纷',
      children: [
        { code: '118.1', name: '有追索权保理纠纷' },
        { code: '118.2', name: '无追索权保理纠纷' },
        { code: '118.3', name: '多重保理纠纷' },
      ],
    },
    {
      code: '119',
      name: '承揽合同纠纷',
      children: [
        { code: '119.1', name: '加工合同纠纷' },
        { code: '119.2', name: '定作合同纠纷' },
        { code: '119.3', name: '修理合同纠纷' },
        { code: '119.4', name: '复制合同纠纷' },
        { code: '119.5', name: '测试合同纠纷' },
        { code: '119.6', name: '检验合同纠纷' },
        { code: '119.7', name: '铁路机车、车辆建造合同纠纷' },
      ],
    },
    {
      code: '120',
      name: '建设工程合同纠纷',
      children: [
        { code: '120.1', name: '建设工程勘察合同纠纷' },
        { code: '120.2', name: '建设工程设计合同纠纷' },
        { code: '120.3', name: '建设工程施工合同纠纷' },
        { code: '120.4', name: '建设工程价款优先受偿权纠纷' },
        { code: '120.5', name: '建设工程分包合同纠纷' },
        { code: '120.6', name: '建设工程监理合同纠纷' },
        { code: '120.7', name: '装饰装修合同纠纷' },
        { code: '120.8', name: '铁路修建合同纠纷' },
        { code: '120.9', name: '农村建房施工合同纠纷' },
        { code: '120.10', name: '矿山建设工程合同纠纷' },
        { code: '120.11', name: '电、水、气、热力工程合同纠纷' },
        { code: '120.12', name: '生态环境保护工程合同纠纷' },
      ],
    },
    {
      code: '121',
      name: '运输合同纠纷',
      children: [
        { code: '121.1', name: '公路旅客运输合同纠纷' },
        { code: '121.2', name: '公路货物运输合同纠纷' },
        { code: '121.3', name: '水路旅客运输合同纠纷' },
        { code: '121.4', name: '水路货物运输合同纠纷' },
        { code: '121.5', name: '航空旅客运输合同纠纷' },
        { code: '121.6', name: '航空货物运输合同纠纷' },
        { code: '121.7', name: '出租汽车运输合同纠纷' },
        { code: '121.8', name: '管道运输合同纠纷' },
        { code: '121.9', name: '城市公交运输合同纠纷' },
        { code: '121.10', name: '联合运输合同纠纷' },
        { code: '121.11', name: '多式联运合同纠纷' },
        { code: '121.12', name: '铁路货物运输合同纠纷' },
        { code: '121.13', name: '铁路旅客运输合同纠纷' },
        { code: '121.14', name: '铁路行李运输合同纠纷' },
        { code: '121.15', name: '铁路包裹运输合同纠纷' },
        { code: '121.16', name: '国际铁路联运合同纠纷' },
        { code: '121.17', name: '国际航空运输合同纠纷' },
        { code: '121.18', name: '国际公路运输合同纠纷' },
      ],
    },
    { code: '122', name: '保管合同纠纷' },
    { code: '123', name: '仓储合同纠纷' },
    {
      code: '124',
      name: '委托合同纠纷',
      children: [
        { code: '124.1', name: '进出口代理合同纠纷' },
        { code: '124.2', name: '货运代理合同纠纷' },
        { code: '124.3', name: '民用航空运输销售代理合同纠纷' },
        { code: '124.4', name: '诉讼、仲裁、人民调解代理合同纠纷' },
        { code: '124.5', name: '销售代理合同纠纷' },
      ],
    },
    {
      code: '125',
      name: '委托理财合同纠纷',
      children: [
        { code: '125.1', name: '金融委托理财合同纠纷' },
        { code: '125.2', name: '民间委托理财合同纠纷' },
      ],
    },
    { code: '126', name: '物业服务合同纠纷' },
    { code: '127', name: '行纪合同纠纷' },
    { code: '128', name: '中介合同纠纷' },
    { code: '129', name: '补偿贸易纠纷' },
    { code: '130', name: '借用合同纠纷' },
    { code: '131', name: '典当纠纷' },
    { code: '132', name: '合伙合同纠纷' },
    { code: '133', name: '种植、养殖回收合同纠纷' },
    { code: '134', name: '彩票、奖券纠纷' },
    { code: '135', name: '中外合作勘探开发自然资源合同纠纷' },
    { code: '136', name: '农业承包合同纠纷' },
    { code: '137', name: '林业承包合同纠纷' },
    { code: '138', name: '渔业承包合同纠纷' },
    { code: '139', name: '牧业承包合同纠纷' },
    {
      code: '140',
      name: '土地承包经营权合同纠纷',
      children: [
        { code: '140.1', name: '土地承包经营权转让合同纠纷' },
        { code: '140.2', name: '土地承包经营权互换合同纠纷' },
        { code: '140.3', name: '土地经营权入股合同纠纷' },
        { code: '140.4', name: '土地经营权抵押合同纠纷' },
        { code: '140.5', name: '土地经营权出租合同纠纷' },
      ],
    },
    { code: '141', name: '居住权合同纠纷' },
    {
      code: '142',
      name: '服务合同纠纷',
      children: [
        { code: '142.1', name: '电信服务合同纠纷' },
        { code: '142.2', name: '邮政服务合同纠纷' },
        { code: '142.3', name: '快递服务合同纠纷' },
        { code: '142.4', name: '医疗服务合同纠纷' },
        { code: '142.5', name: '法律服务合同纠纷' },
        { code: '142.6', name: '旅游合同纠纷' },
        { code: '142.7', name: '房地产咨询合同纠纷' },
        { code: '142.8', name: '房地产价格评估合同纠纷' },
        { code: '142.9', name: '旅店服务合同纠纷' },
        { code: '142.10', name: '财会服务合同纠纷' },
        { code: '142.11', name: '餐饮服务合同纠纷' },
        { code: '142.12', name: '娱乐服务合同纠纷' },
        { code: '142.13', name: '有线电视服务合同纠纷' },
        { code: '142.14', name: '网络服务合同纠纷' },
        { code: '142.15', name: '教育培训合同纠纷' },
        { code: '142.16', name: '家政服务合同纠纷' },
        { code: '142.17', name: '庆典服务合同纠纷' },
        { code: '142.18', name: '殡葬服务合同纠纷' },
        { code: '142.19', name: '农业技术服务合同纠纷' },
        { code: '142.20', name: '农机作业服务合同纠纷' },
        { code: '142.21', name: '保安服务合同纠纷' },
        { code: '142.22', name: '银行结算合同纠纷' },
        { code: '142.23', name: '养老服务合同纠纷' },
        { code: '142.24', name: '环境资源服务合同纠纷' },
      ],
    },
    { code: '143', name: '演出合同纠纷' },
    {
      code: '144',
      name: '劳务合同纠纷',
      children: [{ code: '144.1', name: '超龄劳动者劳务合同纠纷' }],
    },
    { code: '145', name: '广告合同纠纷' },
    { code: '146', name: '展览合同纠纷' },
    { code: '147', name: '追偿权纠纷' },
  ],
};

/** 十一、不当得利纠纷 */
export const CIVIL_PART11_UNJUST_ENRICHMENT: CauseCategory = {
  code: 'P11',
  name: '不当得利纠纷',
  causes: [{ code: '148', name: '不当得利纠纷' }],
};

/** 十二、无因管理纠纷 */
export const CIVIL_PART12_NEGOTIORUM_GESTIO: CauseCategory = {
  code: 'P12',
  name: '无因管理纠纷',
  causes: [{ code: '149', name: '无因管理纠纷' }],
};


/** 十三、知识产权合同纠纷 */
export const CIVIL_PART13_IP_CONTRACT: CauseCategory = {
  code: 'P13',
  name: '知识产权合同纠纷',
  causes: [
    {
      code: '150',
      name: '著作权合同纠纷',
      children: [
        { code: '150.1', name: '委托创作合同纠纷' },
        { code: '150.2', name: '合作创作合同纠纷' },
        { code: '150.3', name: '著作权转让合同纠纷' },
        { code: '150.4', name: '著作权许可使用合同纠纷' },
        { code: '150.5', name: '出版合同纠纷' },
        { code: '150.6', name: '表演合同纠纷' },
        { code: '150.7', name: '音像制品制作合同纠纷' },
        { code: '150.8', name: '广播电视播放合同纠纷' },
        { code: '150.9', name: '邻接权转让合同纠纷' },
        { code: '150.10', name: '邻接权许可使用合同纠纷' },
        { code: '150.11', name: '计算机软件开发合同纠纷' },
        { code: '150.12', name: '计算机软件著作权转让合同纠纷' },
        { code: '150.13', name: '计算机软件著作权许可使用合同纠纷' },
      ],
    },
    {
      code: '151',
      name: '商标合同纠纷',
      children: [
        { code: '151.1', name: '商标权转让合同纠纷' },
        { code: '151.2', name: '商标使用许可合同纠纷' },
        { code: '151.3', name: '商标代理合同纠纷' },
      ],
    },
    {
      code: '152',
      name: '发明专利合同纠纷',
      children: [
        { code: '152.1', name: '发明专利申请权转让合同纠纷' },
        { code: '152.2', name: '发明专利权转让合同纠纷' },
        { code: '152.3', name: '发明专利实施许可合同纠纷' },
        { code: '152.4', name: '发明专利代理合同纠纷' },
        { code: '152.5', name: '发明专利开放许可纠纷' },
      ],
    },
    {
      code: '153',
      name: '实用新型专利合同纠纷',
      children: [
        { code: '153.1', name: '实用新型专利申请权转让合同纠纷' },
        { code: '153.2', name: '实用新型专利权转让合同纠纷' },
        { code: '153.3', name: '实用新型专利实施许可合同纠纷' },
        { code: '153.4', name: '实用新型专利代理合同纠纷' },
        { code: '153.5', name: '实用新型专利开放许可纠纷' },
      ],
    },
    {
      code: '154',
      name: '外观设计专利合同纠纷',
      children: [
        { code: '154.1', name: '外观设计专利申请权转让合同纠纷' },
        { code: '154.2', name: '外观设计专利权转让合同纠纷' },
        { code: '154.3', name: '外观设计专利实施许可合同纠纷' },
        { code: '154.4', name: '外观设计专利代理合同纠纷' },
        { code: '154.5', name: '外观设计专利开放许可纠纷' },
      ],
    },
    {
      code: '155',
      name: '植物新品种合同纠纷',
      children: [
        { code: '155.1', name: '植物新品种育种合同纠纷' },
        { code: '155.2', name: '植物新品种申请权转让合同纠纷' },
        { code: '155.3', name: '植物新品种权转让合同纠纷' },
        { code: '155.4', name: '植物新品种实施许可合同纠纷' },
      ],
    },
    {
      code: '156',
      name: '集成电路布图设计合同纠纷',
      children: [
        { code: '156.1', name: '集成电路布图设计创作合同纠纷' },
        { code: '156.2', name: '集成电路布图设计专有权转让合同纠纷' },
        { code: '156.3', name: '集成电路布图设计许可使用合同纠纷' },
      ],
    },
    {
      code: '157',
      name: '商业秘密合同纠纷',
      children: [
        { code: '157.1', name: '技术秘密让与合同纠纷' },
        { code: '157.2', name: '技术秘密许可使用合同纠纷' },
        { code: '157.3', name: '经营秘密让与合同纠纷' },
        { code: '157.4', name: '经营秘密许可使用合同纠纷' },
      ],
    },
    {
      code: '158',
      name: '技术合同纠纷',
      children: [
        { code: '158.1', name: '技术委托开发合同纠纷' },
        { code: '158.2', name: '技术合作开发合同纠纷' },
        { code: '158.3', name: '技术转化合同纠纷' },
        { code: '158.4', name: '技术转让合同纠纷' },
        { code: '158.5', name: '技术许可合同纠纷' },
        { code: '158.6', name: '技术咨询合同纠纷' },
        { code: '158.7', name: '技术服务合同纠纷' },
        { code: '158.8', name: '技术培训合同纠纷' },
        { code: '158.9', name: '技术中介合同纠纷' },
        { code: '158.10', name: '技术进口合同纠纷' },
        { code: '158.11', name: '技术出口合同纠纷' },
        { code: '158.12', name: '职务技术成果完成人奖励、报酬纠纷' },
        { code: '158.13', name: '技术成果完成人署名权、荣誉权、奖励权纠纷' },
      ],
    },
    { code: '159', name: '特许经营合同纠纷' },
    {
      code: '160',
      name: '企业名称（商号）合同纠纷',
      children: [
        { code: '160.1', name: '企业名称（商号）转让合同纠纷' },
        { code: '160.2', name: '企业名称（商号）使用合同纠纷' },
      ],
    },
    { code: '161', name: '特殊标志合同纠纷' },
    {
      code: '162',
      name: '网络域名合同纠纷',
      children: [
        { code: '162.1', name: '网络域名注册合同纠纷' },
        { code: '162.2', name: '网络域名转让合同纠纷' },
        { code: '162.3', name: '网络域名许可使用合同纠纷' },
      ],
    },
    { code: '163', name: '知识产权质押合同纠纷' },
  ],
};

/** 十四、知识产权权属、侵权纠纷 */
export const CIVIL_PART14_IP_INFRINGEMENT: CauseCategory = {
  code: 'P14',
  name: '知识产权权属、侵权纠纷',
  causes: [
    {
      code: '164',
      name: '著作权权属、侵权纠纷',
      children: [
        { code: '164.1', name: '著作权权属纠纷' },
        { code: '164.2', name: '侵害作品发表权纠纷' },
        { code: '164.3', name: '侵害作品署名权纠纷' },
        { code: '164.4', name: '侵害作品修改权纠纷' },
        { code: '164.5', name: '侵害保护作品完整权纠纷' },
        { code: '164.6', name: '侵害作品复制权纠纷' },
        { code: '164.7', name: '侵害作品发行权纠纷' },
        { code: '164.8', name: '侵害作品出租权纠纷' },
        { code: '164.9', name: '侵害作品展览权纠纷' },
        { code: '164.10', name: '侵害作品表演权纠纷' },
        { code: '164.11', name: '侵害作品放映权纠纷' },
        { code: '164.12', name: '侵害作品广播权纠纷' },
        { code: '164.13', name: '侵害作品信息网络传播权纠纷' },
        { code: '164.14', name: '侵害作品摄制权纠纷' },
        { code: '164.15', name: '侵害作品改编权纠纷' },
        { code: '164.16', name: '侵害作品翻译权纠纷' },
        { code: '164.17', name: '侵害作品汇编权纠纷' },
        { code: '164.18', name: '侵害其他著作财产权纠纷' },
        { code: '164.19', name: '出版者权权属纠纷' },
        { code: '164.20', name: '表演者权权属纠纷' },
        { code: '164.21', name: '录音录像制作者权权属纠纷' },
        { code: '164.22', name: '广播组织权权属纠纷' },
        { code: '164.23', name: '侵害出版者权纠纷' },
        { code: '164.24', name: '侵害表演者权纠纷' },
        { code: '164.25', name: '侵害录音录像制作者权纠纷' },
        { code: '164.26', name: '侵害广播组织权纠纷' },
        { code: '164.27', name: '计算机软件著作权权属纠纷' },
        { code: '164.28', name: '侵害计算机软件著作权纠纷' },
      ],
    },
    {
      code: '165',
      name: '商标权权属、侵权纠纷',
      children: [
        { code: '165.1', name: '商标权权属纠纷' },
        { code: '165.2', name: '侵害商标权纠纷' },
      ],
    },
    { code: '166', name: '地理标志侵权纠纷' },
    {
      code: '167',
      name: '发明专利权权属、侵权纠纷',
      children: [
        { code: '167.1', name: '发明专利申请权权属纠纷' },
        { code: '167.2', name: '发明专利权权属纠纷' },
        { code: '167.3', name: '侵害发明专利权纠纷' },
        { code: '167.4', name: '假冒发明专利纠纷' },
        { code: '167.5', name: '发明专利临时保护期使用费纠纷' },
        { code: '167.6', name: '发明专利职务发明人奖励、报酬纠纷' },
        { code: '167.7', name: '发明专利发明人署名权纠纷' },
      ],
    },
    {
      code: '168',
      name: '实用新型专利权权属、侵权纠纷',
      children: [
        { code: '168.1', name: '实用新型专利申请权权属纠纷' },
        { code: '168.2', name: '实用新型专利权权属纠纷' },
        { code: '168.3', name: '侵害实用新型专利权纠纷' },
        { code: '168.4', name: '假冒实用新型专利纠纷' },
        { code: '168.5', name: '实用新型专利职务发明人奖励、报酬纠纷' },
        { code: '168.6', name: '实用新型专利发明人署名权纠纷' },
      ],
    },
    {
      code: '169',
      name: '外观设计专利权权属、侵权纠纷',
      children: [
        { code: '169.1', name: '外观设计专利申请权权属纠纷' },
        { code: '169.2', name: '外观设计专利权权属纠纷' },
        { code: '169.3', name: '侵害外观设计专利权纠纷' },
        { code: '169.4', name: '假冒外观设计专利纠纷' },
        { code: '169.5', name: '外观设计专利职务设计人奖励、报酬纠纷' },
        { code: '169.6', name: '外观设计专利设计人署名权纠纷' },
      ],
    },
    {
      code: '170',
      name: '标准必要专利纠纷',
      children: [
        { code: '170.1', name: '侵害标准必要专利权纠纷' },
        { code: '170.2', name: '标准必要专利许可纠纷' },
      ],
    },
    {
      code: '171',
      name: '植物新品种权权属、侵权纠纷',
      children: [
        { code: '171.1', name: '植物新品种申请权权属纠纷' },
        { code: '171.2', name: '植物新品种权权属纠纷' },
        { code: '171.3', name: '侵害植物新品种权纠纷' },
        { code: '171.4', name: '假冒植物新品种纠纷' },
        { code: '171.5', name: '植物新品种临时保护期使用费纠纷' },
        { code: '171.6', name: '植物新品种培育人署名权纠纷' },
        { code: '171.7', name: '确认实质性派生品种纠纷' },
      ],
    },
    {
      code: '172',
      name: '集成电路布图设计专有权权属、侵权纠纷',
      children: [
        { code: '172.1', name: '集成电路布图设计专有权权属纠纷' },
        { code: '172.2', name: '侵害集成电路布图设计专有权纠纷' },
        { code: '172.3', name: '集成电路布图设计专有权创作者署名权纠纷' },
      ],
    },
    { code: '173', name: '侵害企业名称（商号）权纠纷' },
    { code: '174', name: '侵害特殊标志专有权纠纷' },
    {
      code: '175',
      name: '网络域名权属、侵权纠纷',
      children: [
        { code: '175.1', name: '网络域名权属纠纷' },
        { code: '175.2', name: '侵害网络域名纠纷' },
      ],
    },
    { code: '176', name: '发现权纠纷' },
    { code: '177', name: '发明权纠纷' },
    { code: '178', name: '其他科技成果权纠纷' },
    {
      code: '179',
      name: '确认不侵害知识产权纠纷',
      children: [
        { code: '179.1', name: '确认不侵害发明专利权纠纷' },
        { code: '179.2', name: '确认不侵害实用新型专利权纠纷' },
        { code: '179.3', name: '确认不侵害外观设计专利权纠纷' },
        { code: '179.4', name: '确认不侵害商标权纠纷' },
        { code: '179.5', name: '确认不侵害著作权纠纷' },
        { code: '179.6', name: '确认不侵害植物新品种权纠纷' },
        { code: '179.7', name: '确认不侵害集成电路布图设计专有权纠纷' },
        { code: '179.8', name: '确认不侵害计算机软件著作权纠纷' },
        { code: '179.9', name: '确认不侵害技术秘密纠纷' },
        { code: '179.10', name: '确认不侵害经营秘密纠纷' },
      ],
    },
    { code: '180', name: '药品专利链接纠纷' },
    {
      code: '181',
      name: '因申请知识产权临时措施损害责任纠纷',
      children: [
        { code: '181.1', name: '因申请停止侵害著作权损害责任纠纷' },
        { code: '181.2', name: '因申请停止侵害注册商标专用权损害责任纠纷' },
        { code: '181.3', name: '因申请停止侵害发明专利权损害责任纠纷' },
        { code: '181.4', name: '因申请停止侵害实用新型专利权损害责任纠纷' },
        { code: '181.5', name: '因申请停止侵害外观设计专利权损害责任纠纷' },
        { code: '181.6', name: '因申请停止侵害植物新品种权损害责任纠纷' },
        { code: '181.7', name: '因申请停止侵害计算机软件著作权损害责任纠纷' },
        { code: '181.8', name: '因申请停止侵害集成电路布图设计专有权损害责任纠纷' },
        { code: '181.9', name: '因申请海关知识产权保护措施损害责任纠纷' },
        { code: '181.10', name: '因申请停止侵害技术秘密损害责任纠纷' },
        { code: '181.11', name: '因申请停止侵害经营秘密损害责任纠纷' },
      ],
    },
    { code: '182', name: '因恶意提起知识产权诉讼损害责任纠纷' },
    {
      code: '183',
      name: '专利权宣告无效后返还费用纠纷',
      children: [
        { code: '183.1', name: '发明专利权宣告无效后返还费用纠纷' },
        { code: '183.2', name: '实用新型专利权宣告无效后返还费用纠纷' },
        { code: '183.3', name: '外观设计专利权宣告无效后返还费用纠纷' },
      ],
    },
    { code: '184', name: '植物新品种权宣告无效后返还费用纠纷' },
    { code: '185', name: '集成电路布图设计专有权撤销后返还费用纠纷' },
  ],
};

/** 十五、不正当竞争纠纷 */
export const CIVIL_PART15_UNFAIR_COMPETITION: CauseCategory = {
  code: 'P15',
  name: '不正当竞争纠纷',
  causes: [
    {
      code: '186',
      name: '仿冒纠纷',
      children: [
        { code: '186.1', name: '擅自使用与他人有一定影响的商品名称、包装、装潢等相同或者近似的标识纠纷' },
        { code: '186.2', name: '擅自使用他人有一定影响的名称、姓名纠纷' },
        { code: '186.3', name: '擅自使用他人有一定影响的网络活动标识纠纷' },
      ],
    },
    { code: '187', name: '商业贿赂不正当竞争纠纷' },
    { code: '188', name: '虚假宣传纠纷' },
    {
      code: '189',
      name: '侵害商业秘密纠纷',
      children: [
        { code: '189.1', name: '侵害技术秘密纠纷' },
        { code: '189.2', name: '侵害经营秘密纠纷' },
      ],
    },
    { code: '190', name: '不正当有奖销售纠纷' },
    { code: '191', name: '商业诋毁纠纷' },
    { code: '192', name: '串通投标不正当竞争纠纷' },
    {
      code: '193',
      name: '网络不正当竞争纠纷',
      children: [
        { code: '193.1', name: '妨碍、破坏合法网络产品或者服务纠纷' },
        { code: '193.2', name: '不正当获取、使用数据纠纷' },
        { code: '193.3', name: '滥用平台规则恶意交易纠纷' },
      ],
    },
    { code: '194', name: '平台经营者强制低价销售纠纷' },
    { code: '195', name: '滥用优势地位拖欠中小企业账款纠纷' },
  ],
};

/** 十六、垄断纠纷 */
export const CIVIL_PART16_MONOPOLY: CauseCategory = {
  code: 'P16',
  name: '垄断纠纷',
  causes: [
    {
      code: '196',
      name: '垄断协议纠纷',
      children: [
        { code: '196.1', name: '横向垄断协议纠纷' },
        { code: '196.2', name: '纵向垄断协议纠纷' },
        { code: '196.3', name: '组织、帮助达成垄断协议纠纷' },
      ],
    },
    {
      code: '197',
      name: '滥用市场支配地位纠纷',
      children: [
        { code: '197.1', name: '不公平价格纠纷' },
        { code: '197.2', name: '低于成本销售纠纷' },
        { code: '197.3', name: '拒绝交易纠纷' },
        { code: '197.4', name: '限定交易纠纷' },
        { code: '197.5', name: '搭售、附加其他不合理交易条件纠纷' },
        { code: '197.6', name: '差别待遇纠纷' },
        { code: '197.7', name: '其他滥用市场支配地位纠纷' },
      ],
    },
    { code: '198', name: '经营者集中纠纷' },
  ],
};


/** 十七、数据纠纷 */
export const CIVIL_PART17_DATA: CauseCategory = {
  code: 'P17',
  name: '数据纠纷',
  causes: [
    { code: '199', name: '数据权属纠纷' },
    { code: '200', name: '数据合同纠纷' },
    { code: '201', name: '侵害数据权益纠纷' },
  ],
};

/** 十八、网络虚拟财产纠纷 */
export const CIVIL_PART18_VIRTUAL_PROPERTY: CauseCategory = {
  code: 'P18',
  name: '网络虚拟财产纠纷',
  causes: [
    { code: '202', name: '网络虚拟财产权属纠纷' },
    { code: '203', name: '网络虚拟财产合同纠纷' },
    { code: '204', name: '侵害网络虚拟财产权益纠纷' },
  ],
};

/** 十九、劳动争议 - 用于劳动仲裁 */
export const CIVIL_PART19_LABOR: CauseCategory = {
  code: 'P19',
  name: '劳动争议',
  causes: [
    {
      code: '205',
      name: '劳动合同纠纷',
      children: [
        { code: '205.1', name: '确认劳动关系纠纷' },
        { code: '205.2', name: '集体合同纠纷' },
        { code: '205.3', name: '劳务派遣合同纠纷' },
        { code: '205.4', name: '非全日制用工纠纷' },
        { code: '205.5', name: '追索劳动报酬纠纷' },
        { code: '205.6', name: '经济补偿金纠纷' },
        { code: '205.7', name: '竞业限制纠纷' },
      ],
    },
    {
      code: '206',
      name: '社会保险纠纷',
      children: [
        { code: '206.1', name: '养老保险待遇纠纷' },
        { code: '206.2', name: '工伤保险待遇纠纷' },
        { code: '206.3', name: '医疗保险待遇纠纷' },
        { code: '206.4', name: '生育保险待遇纠纷' },
        { code: '206.5', name: '失业保险待遇纠纷' },
      ],
    },
    { code: '207', name: '福利待遇纠纷' },
    { code: '208', name: '超龄劳动者用工纠纷' },
    { code: '209', name: '承包人、被挂靠人用工主体责任纠纷' },
  ],
};

/** 二十、人事争议 */
export const CIVIL_PART20_PERSONNEL: CauseCategory = {
  code: 'P20',
  name: '人事争议',
  causes: [
    { code: '210', name: '聘用合同纠纷' },
    { code: '211', name: '聘任合同纠纷' },
    { code: '212', name: '辞职纠纷' },
    { code: '213', name: '辞退纠纷' },
  ],
};

/** 二十一、新就业形态用工纠纷 */
export const CIVIL_PART21_NEW_EMPLOYMENT: CauseCategory = {
  code: 'P21',
  name: '新就业形态用工纠纷',
  causes: [
    {
      code: '214',
      name: '新就业形态用工合同纠纷',
      children: [
        { code: '214.1', name: '新就业形态确认劳动关系纠纷' },
        { code: '214.2', name: '新就业形态追索劳动报酬纠纷' },
      ],
    },
    {
      code: '215',
      name: '新就业形态社会保险纠纷',
      children: [{ code: '215.1', name: '新就业形态工伤保险待遇纠纷' }],
    },
    { code: '216', name: '新就业形态职业伤害保障纠纷' },
  ],
};

/** 二十二、海事海商纠纷 */
export const CIVIL_PART22_MARITIME: CauseCategory = {
  code: 'P22',
  name: '海事海商纠纷',
  causes: [
    { code: '217', name: '船舶碰撞损害责任纠纷' },
    { code: '218', name: '船舶触碰损害责任纠纷' },
    { code: '219', name: '船舶损坏空中设施、水下设施损害责任纠纷' },
    { code: '220', name: '船舶污染损害责任纠纷' },
    { code: '221', name: '海上、通海水域污染损害责任纠纷' },
    { code: '222', name: '海上、通海水域养殖损害责任纠纷' },
    { code: '223', name: '海上、通海水域财产损害责任纠纷' },
    { code: '224', name: '海上、通海水域人身损害责任纠纷' },
    { code: '225', name: '非法留置船舶、船载货物、船用燃油、船用物料损害责任纠纷' },
    { code: '226', name: '船舶部件、物料产品责任纠纷' },
    { code: '227', name: '海运物流服务合同纠纷' },
    { code: '228', name: '海上、通海水域货物运输合同纠纷' },
    { code: '229', name: '海上、通海水域旅客运输合同纠纷' },
    { code: '230', name: '海上、通海水域行李运输合同纠纷' },
    { code: '231', name: '船舶经营管理合同纠纷' },
    { code: '232', name: '船舶买卖合同纠纷' },
    {
      code: '233',
      name: '船舶工程合同纠纷',
      children: [
        { code: '233.1', name: '船舶建造合同纠纷' },
        { code: '233.2', name: '船舶修理合同纠纷' },
        { code: '233.3', name: '船舶改建合同纠纷' },
        { code: '233.4', name: '船舶拆解合同纠纷' },
      ],
    },
    { code: '234', name: '船舶设计合同纠纷' },
    { code: '235', name: '船员劳务合同纠纷' },
    { code: '236', name: '船员劳动合同纠纷' },
    { code: '237', name: '船员劳务派遣合同纠纷' },
    { code: '238', name: '船员服务合同纠纷' },
    { code: '239', name: '船舶抵押合同纠纷' },
    {
      code: '240',
      name: '船舶租用合同纠纷',
      children: [
        { code: '240.1', name: '定期租船合同纠纷' },
        { code: '240.2', name: '光船租赁合同纠纷' },
        { code: '240.3', name: '航次租船合同纠纷' },
      ],
    },
    { code: '241', name: '船舶融资租赁合同纠纷' },
    { code: '242', name: '海上、通海水域运输船舶承包合同纠纷' },
    { code: '243', name: '渔船承包合同纠纷' },
    { code: '244', name: '船舶属具租赁合同纠纷' },
    { code: '245', name: '船舶属具保管合同纠纷' },
    { code: '246', name: '海运集装箱租赁合同纠纷' },
    { code: '247', name: '海运集装箱保管合同纠纷' },
    { code: '248', name: '港口货物保管合同纠纷' },
    { code: '249', name: '船舶代理合同纠纷' },
    { code: '250', name: '海上、通海水域货运代理合同纠纷' },
    { code: '251', name: '航运经纪合同纠纷' },
    { code: '252', name: '理货合同纠纷' },
    { code: '253', name: '船舶物料和备品供应合同纠纷' },
    { code: '254', name: '海难救助合同纠纷' },
    { code: '255', name: '海上、通海水域打捞合同纠纷' },
    { code: '256', name: '海上、通海水域拖航合同纠纷' },
    { code: '257', name: '海上、通海水域保险合同纠纷' },
    { code: '258', name: '海上、通海水域保赔合同纠纷' },
    { code: '259', name: '海上、通海水域运输联营合同纠纷' },
    { code: '260', name: '船舶营运借款合同纠纷' },
    { code: '261', name: '海事担保合同纠纷' },
    { code: '262', name: '航道、港口疏浚合同纠纷' },
    { code: '263', name: '船坞、码头建造合同纠纷' },
    { code: '264', name: '港口、码头租赁合同纠纷' },
    { code: '265', name: '船舶检验合同纠纷' },
    { code: '266', name: '海事请求担保纠纷' },
    { code: '267', name: '海上、通海水域运输重大责任事故责任纠纷' },
    { code: '268', name: '港口作业重大责任事故责任纠纷' },
    { code: '269', name: '港口作业纠纷' },
    { code: '270', name: '共同海损纠纷' },
    { code: '271', name: '海洋开发利用纠纷' },
    {
      code: '272',
      name: '船舶物权纠纷',
      children: [
        { code: '272.1', name: '船舶所有权纠纷' },
        { code: '272.2', name: '船舶抵押权纠纷' },
        { code: '272.3', name: '船舶优先权纠纷' },
        { code: '272.4', name: '船舶留置权纠纷' },
      ],
    },
    { code: '273', name: '船舶投资经营纠纷' },
    { code: '274', name: '海运欺诈纠纷' },
    { code: '275', name: '海事债权确权纠纷' },
  ],
};


/** 二十三、与企业有关的纠纷 */
export const CIVIL_PART23_ENTERPRISE: CauseCategory = {
  code: 'P23',
  name: '与企业有关的纠纷',
  causes: [
    { code: '276', name: '企业出资人权益确认纠纷' },
    { code: '277', name: '侵害企业出资人权益纠纷' },
    { code: '278', name: '企业公司制改造合同纠纷' },
    { code: '279', name: '企业股份合作制改造合同纠纷' },
    { code: '280', name: '企业债权转股权合同纠纷' },
    { code: '281', name: '企业分立合同纠纷' },
    { code: '282', name: '企业租赁经营合同纠纷' },
    { code: '283', name: '企业出售合同纠纷' },
    { code: '284', name: '挂靠经营合同纠纷' },
    { code: '285', name: '企业兼并合同纠纷' },
    { code: '286', name: '联营合同纠纷' },
    {
      code: '287',
      name: '企业承包经营合同纠纷',
      children: [
        { code: '287.1', name: '中外合资经营企业承包经营合同纠纷' },
        { code: '287.2', name: '中外合作经营企业承包经营合同纠纷' },
        { code: '287.3', name: '外商独资企业承包经营合同纠纷' },
        { code: '287.4', name: '乡镇企业承包经营合同纠纷' },
      ],
    },
    { code: '288', name: '中外合资经营企业合同纠纷' },
    { code: '289', name: '中外合作经营企业合同纠纷' },
  ],
};

/** 二十四、与公司有关的纠纷 */
export const CIVIL_PART24_COMPANY: CauseCategory = {
  code: 'P24',
  name: '与公司有关的纠纷',
  causes: [
    { code: '290', name: '股东资格确认纠纷' },
    { code: '291', name: '股东名册记载纠纷' },
    {
      code: '292',
      name: '请求变更公司登记纠纷',
      children: [{ code: '292.1', name: '涤除公司登记（备案）纠纷' }],
    },
    {
      code: '293',
      name: '股东出资纠纷',
      children: [
        { code: '293.1', name: '股东未全面履行出资义务纠纷' },
        { code: '293.2', name: '股东抽逃出资纠纷' },
        { code: '293.3', name: '股东出资加速到期纠纷' },
      ],
    },
    { code: '294', name: '股东失权纠纷' },
    { code: '295', name: '新增资本认购纠纷' },
    { code: '296', name: '股东知情权纠纷' },
    { code: '297', name: '请求公司收购股权、股份纠纷' },
    { code: '298', name: '股权转让纠纷' },
    {
      code: '299',
      name: '公司决议纠纷',
      children: [
        { code: '299.1', name: '公司决议效力确认纠纷' },
        { code: '299.2', name: '公司决议撤销纠纷' },
        { code: '299.3', name: '公司决议不成立确认纠纷' },
      ],
    },
    { code: '300', name: '董事、监事、高级管理人员解任纠纷' },
    { code: '301', name: '公司设立纠纷' },
    { code: '302', name: '公司证照返还纠纷' },
    { code: '303', name: '设立人责任纠纷' },
    { code: '304', name: '公司盈余分配纠纷' },
    { code: '305', name: '损害股东利益责任纠纷' },
    { code: '306', name: '损害公司利益责任纠纷' },
    { code: '307', name: '损害公司债权人利益责任纠纷' },
    { code: '308', name: '公司债券持有人会议决议效力纠纷' },
    { code: '309', name: '债券受托管理人损害债券持有人利益纠纷' },
    { code: '310', name: '公司关联交易损害责任纠纷' },
    { code: '311', name: '公司合并纠纷' },
    { code: '312', name: '公司分立纠纷' },
    { code: '313', name: '公司减资纠纷' },
    { code: '314', name: '公司增资纠纷' },
    { code: '315', name: '公司解散纠纷' },
    {
      code: '316',
      name: '公司清算责任纠纷',
      children: [
        { code: '316.1', name: '清算义务人责任纠纷' },
        { code: '316.2', name: '清算组成员责任纠纷' },
      ],
    },
    { code: '317', name: '上市公司收购纠纷' },
  ],
};

/** 二十五、合伙企业纠纷 */
export const CIVIL_PART25_PARTNERSHIP: CauseCategory = {
  code: 'P25',
  name: '合伙企业纠纷',
  causes: [
    { code: '318', name: '入伙纠纷' },
    { code: '319', name: '退伙纠纷' },
    { code: '320', name: '合伙企业财产份额转让纠纷' },
  ],
};

/** 二十六、与农民专业合作社有关的纠纷 */
export const CIVIL_PART26_COOPERATIVE: CauseCategory = {
  code: 'P26',
  name: '与农民专业合作社有关的纠纷',
  causes: [
    { code: '321', name: '农民专业合作社成员资格确认纠纷' },
    { code: '322', name: '农民专业合作社成员出资纠纷' },
    { code: '323', name: '农民专业合作社合并纠纷' },
    { code: '324', name: '农民专业合作社分立纠纷' },
    { code: '325', name: '农民专业合作社解散纠纷' },
    { code: '326', name: '农民专业合作社清算纠纷' },
  ],
};

/** 二十七、与破产有关的纠纷 */
export const CIVIL_PART27_BANKRUPTCY: CauseCategory = {
  code: 'P27',
  name: '与破产有关的纠纷',
  causes: [
    { code: '327', name: '请求撤销个别清偿行为纠纷' },
    { code: '328', name: '请求确认债务人行为无效纠纷' },
    { code: '329', name: '对外追收债权纠纷' },
    { code: '330', name: '追收未缴出资纠纷' },
    { code: '331', name: '追收抽逃出资纠纷' },
    { code: '332', name: '追收非正常收入纠纷' },
    {
      code: '333',
      name: '破产债权确认纠纷',
      children: [
        { code: '333.1', name: '优先破产债权确认纠纷' },
        { code: '333.2', name: '职工破产债权确认纠纷' },
        { code: '333.3', name: '普通破产债权确认纠纷' },
      ],
    },
    {
      code: '334',
      name: '取回权纠纷',
      children: [
        { code: '334.1', name: '一般取回权纠纷' },
        { code: '334.2', name: '出卖人取回权纠纷' },
      ],
    },
    { code: '335', name: '破产抵销权纠纷' },
    { code: '336', name: '别除权纠纷' },
    { code: '337', name: '破产撤销权纠纷' },
    { code: '338', name: '损害债务人利益赔偿纠纷' },
    { code: '339', name: '管理人责任纠纷' },
  ],
};

/** 二十八、证券纠纷 */
export const CIVIL_PART28_SECURITIES: CauseCategory = {
  code: 'P28',
  name: '证券纠纷',
  causes: [
    {
      code: '340',
      name: '证券权利确认纠纷',
      children: [
        { code: '340.1', name: '股票权利确认纠纷' },
        { code: '340.2', name: '公司债券权利确认纠纷' },
        { code: '340.3', name: '国债权利确认纠纷' },
        { code: '340.4', name: '证券投资基金权利确认纠纷' },
      ],
    },
    {
      code: '341',
      name: '证券交易合同纠纷',
      children: [
        { code: '341.1', name: '股票交易纠纷' },
        { code: '341.2', name: '公司债券交易纠纷' },
        { code: '341.3', name: '国债交易纠纷' },
        { code: '341.4', name: '证券投资基金交易纠纷' },
      ],
    },
    { code: '342', name: '金融衍生品种交易纠纷' },
    {
      code: '343',
      name: '证券承销合同纠纷',
      children: [
        { code: '343.1', name: '证券代销合同纠纷' },
        { code: '343.2', name: '证券包销合同纠纷' },
      ],
    },
    { code: '344', name: '证券投资咨询纠纷' },
    { code: '345', name: '证券资信评级服务合同纠纷' },
    {
      code: '346',
      name: '证券回购合同纠纷',
      children: [
        { code: '346.1', name: '股票回购合同纠纷' },
        { code: '346.2', name: '国债回购合同纠纷' },
        { code: '346.3', name: '公司债券回购合同纠纷' },
        { code: '346.4', name: '证券投资基金回购合同纠纷' },
        { code: '346.5', name: '质押式证券回购纠纷' },
      ],
    },
    { code: '347', name: '证券上市合同纠纷' },
    { code: '348', name: '证券交易代理合同纠纷' },
    { code: '349', name: '证券上市保荐合同纠纷' },
    {
      code: '350',
      name: '证券发行纠纷',
      children: [
        { code: '350.1', name: '证券认购纠纷' },
        { code: '350.2', name: '证券发行失败纠纷' },
      ],
    },
    { code: '351', name: '证券返还纠纷' },
    {
      code: '352',
      name: '证券欺诈责任纠纷',
      children: [
        { code: '352.1', name: '证券内幕交易责任纠纷' },
        { code: '352.2', name: '操纵证券交易市场责任纠纷' },
        { code: '352.3', name: '证券虚假陈述责任纠纷' },
        { code: '352.4', name: '欺诈客户责任纠纷' },
      ],
    },
    { code: '353', name: '证券托管纠纷' },
    { code: '354', name: '证券登记、存管、结算纠纷' },
    { code: '355', name: '融资融券交易纠纷' },
    { code: '356', name: '客户交易结算资金纠纷' },
  ],
};

/** 二十九、期货交易纠纷 */
export const CIVIL_PART29_FUTURES: CauseCategory = {
  code: 'P29',
  name: '期货交易纠纷',
  causes: [
    { code: '357', name: '期货经纪合同纠纷' },
    { code: '358', name: '期货透支交易纠纷' },
    { code: '359', name: '期货强行平仓纠纷' },
    { code: '360', name: '期货实物交割纠纷' },
    { code: '361', name: '期货保证合约纠纷' },
    { code: '362', name: '期货交易代理合同纠纷' },
    { code: '363', name: '侵占期货交易保证金纠纷' },
    { code: '364', name: '期货欺诈责任纠纷' },
    { code: '365', name: '操纵期货交易市场责任纠纷' },
    { code: '366', name: '期货内幕交易责任纠纷' },
    { code: '367', name: '期货虚假信息责任纠纷' },
  ],
};

/** 三十、信托纠纷 */
export const CIVIL_PART30_TRUST: CauseCategory = {
  code: 'P30',
  name: '信托纠纷',
  causes: [
    { code: '368', name: '民事信托纠纷' },
    { code: '369', name: '营业信托纠纷' },
    { code: '370', name: '公益信托纠纷' },
  ],
};

/** 三十一、保险纠纷 */
export const CIVIL_PART31_INSURANCE: CauseCategory = {
  code: 'P31',
  name: '保险纠纷',
  causes: [
    {
      code: '371',
      name: '财产保险合同纠纷',
      children: [
        { code: '371.1', name: '财产损失保险合同纠纷' },
        { code: '371.2', name: '责任保险合同纠纷' },
        { code: '371.3', name: '信用保险合同纠纷' },
        { code: '371.4', name: '保证保险合同纠纷' },
        { code: '371.5', name: '保险人代位求偿权纠纷' },
      ],
    },
    {
      code: '372',
      name: '人身保险合同纠纷',
      children: [
        { code: '372.1', name: '人寿保险合同纠纷' },
        { code: '372.2', name: '意外伤害保险合同纠纷' },
        { code: '372.3', name: '健康保险合同纠纷' },
      ],
    },
    { code: '373', name: '再保险合同纠纷' },
    { code: '374', name: '保险经纪合同纠纷' },
    { code: '375', name: '保险代理合同纠纷' },
    { code: '376', name: '进出口信用保险合同纠纷' },
    { code: '377', name: '保险费纠纷' },
  ],
};

/** 三十二、票据纠纷 */
export const CIVIL_PART32_NEGOTIABLE: CauseCategory = {
  code: 'P32',
  name: '票据纠纷',
  causes: [
    { code: '378', name: '票据付款请求权纠纷' },
    { code: '379', name: '票据追索权纠纷' },
    { code: '380', name: '票据交付请求权纠纷' },
    { code: '381', name: '票据返还请求权纠纷' },
    { code: '382', name: '票据损害责任纠纷' },
    { code: '383', name: '票据利益返还请求权纠纷' },
    { code: '384', name: '汇票回单签发请求权纠纷' },
    { code: '385', name: '票据保证纠纷' },
    { code: '386', name: '确认票据无效纠纷' },
    { code: '387', name: '票据代理纠纷' },
    { code: '388', name: '票据回购纠纷' },
  ],
};

/** 三十三、信用证纠纷 */
export const CIVIL_PART33_LETTER_OF_CREDIT: CauseCategory = {
  code: 'P33',
  name: '信用证纠纷',
  causes: [
    { code: '389', name: '委托开立信用证纠纷' },
    { code: '390', name: '信用证开证纠纷' },
    { code: '391', name: '信用证议付纠纷' },
    { code: '392', name: '信用证欺诈纠纷' },
    { code: '393', name: '信用证融资纠纷' },
    { code: '394', name: '信用证转让纠纷' },
  ],
};

/** 三十四、独立保函纠纷 */
export const CIVIL_PART34_GUARANTEE: CauseCategory = {
  code: 'P34',
  name: '独立保函纠纷',
  causes: [
    { code: '395', name: '独立保函开立纠纷' },
    { code: '396', name: '独立保函付款纠纷' },
    { code: '397', name: '独立保函追偿纠纷' },
    { code: '398', name: '独立保函欺诈纠纷' },
    { code: '399', name: '独立保函转让纠纷' },
    { code: '400', name: '独立保函通知纠纷' },
    { code: '401', name: '独立保函撤销纠纷' },
  ],
};


/** 三十五、侵权责任纠纷 */
export const CIVIL_PART35_TORT: CauseCategory = {
  code: 'P35',
  name: '侵权责任纠纷',
  causes: [
    { code: '402', name: '监护人责任纠纷' },
    { code: '403', name: '用人单位责任纠纷' },
    { code: '404', name: '劳务派遣工作人员侵权责任纠纷' },
    { code: '405', name: '提供劳务者致害责任纠纷' },
    { code: '406', name: '提供劳务者受害责任纠纷' },
    {
      code: '407',
      name: '违反安全保障义务责任纠纷',
      children: [
        { code: '407.1', name: '经营场所、公共场所的经营者、管理者责任纠纷' },
        { code: '407.2', name: '群众性活动组织者责任纠纷' },
      ],
    },
    { code: '408', name: '教育机构责任纠纷' },
    { code: '409', name: '性骚扰损害责任纠纷' },
    {
      code: '410',
      name: '产品责任纠纷',
      children: [
        { code: '410.1', name: '产品生产者责任纠纷' },
        { code: '410.2', name: '产品销售者责任纠纷' },
        { code: '410.3', name: '产品运输者责任纠纷' },
        { code: '410.4', name: '产品仓储者责任纠纷' },
      ],
    },
    {
      code: '411',
      name: '机动车交通事故责任纠纷',
      children: [{ code: '411.1', name: '智能网联汽车交通事故责任纠纷' }],
    },
    { code: '412', name: '非机动车交通事故责任纠纷' },
    {
      code: '413',
      name: '医疗损害责任纠纷',
      children: [
        { code: '413.1', name: '医疗诊疗责任纠纷' },
        { code: '413.2', name: '医疗产品责任纠纷' },
      ],
    },
    {
      code: '414',
      name: '环境污染责任纠纷',
      children: [
        { code: '414.1', name: '大气污染责任纠纷' },
        { code: '414.2', name: '水污染责任纠纷' },
        { code: '414.3', name: '土壤污染责任纠纷' },
        { code: '414.4', name: '电子废物污染责任纠纷' },
        { code: '414.5', name: '固体废物污染责任纠纷' },
        { code: '414.6', name: '噪声污染责任纠纷' },
        { code: '414.7', name: '光污染责任纠纷' },
        { code: '414.8', name: '放射性污染责任纠纷' },
      ],
    },
    { code: '415', name: '生态破坏责任纠纷' },
    { code: '416', name: '越界勘查、开采矿产资源责任纠纷' },
    { code: '417', name: '矿产资源压覆侵权责任纠纷' },
    {
      code: '418',
      name: '高度危险责任纠纷',
      children: [
        { code: '418.1', name: '民用核设施、核材料损害责任纠纷' },
        { code: '418.2', name: '民用航空器损害责任纠纷' },
        { code: '418.3', name: '占有、使用高度危险物损害责任纠纷' },
        { code: '418.4', name: '高度危险活动损害责任纠纷' },
        { code: '418.5', name: '遗失、抛弃高度危险物损害责任纠纷' },
        { code: '418.6', name: '非法占有高度危险物损害责任纠纷' },
      ],
    },
    { code: '419', name: '饲养动物损害责任纠纷' },
    {
      code: '420',
      name: '建筑物和物件损害责任纠纷',
      children: [
        { code: '420.1', name: '物件脱落、坠落损害责任纠纷' },
        { code: '420.2', name: '建筑物、构筑物倒塌、塌陷损害责任纠纷' },
        { code: '420.3', name: '高空抛物、坠物损害责任纠纷' },
        { code: '420.4', name: '堆放物倒塌、滚落、滑落损害责任纠纷' },
        { code: '420.5', name: '公共道路妨碍通行损害责任纠纷' },
        { code: '420.6', name: '林木折断、倾倒、果实坠落损害责任纠纷' },
        { code: '420.7', name: '地面施工、地下设施损害责任纠纷' },
      ],
    },
    { code: '421', name: '触电人身损害责任纠纷' },
    { code: '422', name: '义务帮工人受害责任纠纷' },
    { code: '423', name: '见义勇为人受害责任纠纷' },
    { code: '424', name: '防卫过当损害责任纠纷' },
    { code: '425', name: '紧急避险损害责任纠纷' },
    { code: '426', name: '驻香港、澳门特别行政区军人执行职务侵权责任纠纷' },
    {
      code: '427',
      name: '铁路运输损害责任纠纷',
      children: [
        { code: '427.1', name: '铁路运输人身损害责任纠纷' },
        { code: '427.2', name: '铁路运输财产损害责任纠纷' },
      ],
    },
    {
      code: '428',
      name: '水上运输损害责任纠纷',
      children: [
        { code: '428.1', name: '水上运输人身损害责任纠纷' },
        { code: '428.2', name: '水上运输财产损害责任纠纷' },
      ],
    },
    {
      code: '429',
      name: '航空运输损害责任纠纷',
      children: [
        { code: '429.1', name: '航空运输人身损害责任纠纷' },
        { code: '429.2', name: '航空运输财产损害责任纠纷' },
      ],
    },
    { code: '430', name: '公证损害责任纠纷' },
    { code: '431', name: '因申请财产保全损害责任纠纷' },
    { code: '432', name: '因申请行为保全损害责任纠纷' },
    { code: '433', name: '因申请证据保全损害责任纠纷' },
    { code: '434', name: '因申请先予执行损害责任纠纷' },
  ],
};

/** 三十六、选民资格案件 */
export const CIVIL_PART36_VOTER: CauseCategory = {
  code: 'P36',
  name: '选民资格案件',
  causes: [{ code: '435', name: '申请确定选民资格' }],
};

/** 三十七、宣告失踪、宣告死亡案件 */
export const CIVIL_PART37_MISSING_DEATH: CauseCategory = {
  code: 'P37',
  name: '宣告失踪、宣告死亡案件',
  causes: [
    { code: '436', name: '申请宣告自然人失踪' },
    { code: '437', name: '申请撤销宣告失踪判决' },
    { code: '438', name: '申请为失踪人财产指定、变更代管人' },
    { code: '439', name: '申请宣告自然人死亡' },
    { code: '440', name: '申请撤销宣告自然人死亡判决' },
  ],
};

/** 三十八、认定自然人无民事行为能力、限制民事行为能力案件 */
export const CIVIL_PART38_CAPACITY: CauseCategory = {
  code: 'P38',
  name: '认定自然人无民事行为能力、限制民事行为能力案件',
  causes: [
    { code: '441', name: '申请宣告自然人无民事行为能力' },
    { code: '442', name: '申请宣告自然人限制民事行为能力' },
    { code: '443', name: '申请宣告自然人恢复限制民事行为能力' },
    { code: '444', name: '申请宣告自然人恢复完全民事行为能力' },
  ],
};

/** 三十九、指定遗产管理人案件 */
export const CIVIL_PART39_ESTATE_MANAGER: CauseCategory = {
  code: 'P39',
  name: '指定遗产管理人案件',
  causes: [{ code: '445', name: '申请指定遗产管理人' }],
};

/** 四十、认定财产无主案件 */
export const CIVIL_PART40_OWNERLESS: CauseCategory = {
  code: 'P40',
  name: '认定财产无主案件',
  causes: [
    { code: '446', name: '申请认定财产无主' },
    { code: '447', name: '申请撤销认定财产无主判决' },
  ],
};

/** 四十一、确认调解协议案件 */
export const CIVIL_PART41_MEDIATION: CauseCategory = {
  code: 'P41',
  name: '确认调解协议案件',
  causes: [
    { code: '448', name: '申请司法确认调解协议' },
    { code: '449', name: '申请撤销确认调解协议裁定' },
  ],
};

/** 四十二、确认生态环境损害赔偿协议案件 */
export const CIVIL_PART42_ECO_DAMAGE: CauseCategory = {
  code: 'P42',
  name: '确认生态环境损害赔偿协议案件',
  causes: [{ code: '450', name: '申请司法确认生态环境损害赔偿协议' }],
};

/** 四十三、实现担保物权案件 */
export const CIVIL_PART43_SECURITY_REALIZATION: CauseCategory = {
  code: 'P43',
  name: '实现担保物权案件',
  causes: [
    { code: '451', name: '申请实现担保物权' },
    { code: '452', name: '申请撤销准许实现担保物权裁定' },
  ],
};

/** 四十四、监护权特别程序案件 */
export const CIVIL_PART44_GUARDIANSHIP: CauseCategory = {
  code: 'P44',
  name: '监护权特别程序案件',
  causes: [
    { code: '453', name: '申请确定监护人' },
    { code: '454', name: '申请指定监护人' },
    { code: '455', name: '申请变更监护人' },
    { code: '456', name: '申请撤销监护人资格' },
    { code: '457', name: '申请恢复监护人资格' },
  ],
};

/** 四十五、督促程序案件 */
export const CIVIL_PART45_PAYMENT_ORDER: CauseCategory = {
  code: 'P45',
  name: '督促程序案件',
  causes: [{ code: '458', name: '申请支付令' }],
};

/** 四十六、公示催告程序案件 */
export const CIVIL_PART46_PUBLIC_SUMMONS: CauseCategory = {
  code: 'P46',
  name: '公示催告程序案件',
  causes: [{ code: '459', name: '申请公示催告' }],
};

/** 四十七、公司清算案件 */
export const CIVIL_PART47_COMPANY_LIQUIDATION: CauseCategory = {
  code: 'P47',
  name: '公司清算案件',
  causes: [{ code: '460', name: '申请公司清算' }],
};

/** 四十八、破产程序案件 */
export const CIVIL_PART48_BANKRUPTCY_PROCEDURE: CauseCategory = {
  code: 'P48',
  name: '破产程序案件',
  causes: [
    { code: '461', name: '申请破产清算' },
    { code: '462', name: '申请破产重整' },
    { code: '463', name: '申请破产和解' },
    { code: '464', name: '申请对破产财产追加分配' },
  ],
};

/** 四十九、申请诉前停止侵害知识产权案件 */
export const CIVIL_PART49_IP_INJUNCTION: CauseCategory = {
  code: 'P49',
  name: '申请诉前停止侵害知识产权案件',
  causes: [
    { code: '465', name: '申请诉前停止侵害发明专利权' },
    { code: '466', name: '申请诉前停止侵害实用新型专利权' },
    { code: '467', name: '申请诉前停止侵害外观设计专利权' },
    { code: '468', name: '申请诉前停止侵害注册商标专用权' },
    { code: '469', name: '申请诉前停止侵害著作权' },
    { code: '470', name: '申请诉前停止侵害植物新品种权' },
    { code: '471', name: '申请诉前停止侵害计算机软件著作权' },
    { code: '472', name: '申请诉前停止侵害集成电路布图设计专有权' },
    { code: '473', name: '申请诉前停止侵害技术秘密' },
    { code: '474', name: '申请诉前停止侵害经营秘密' },
  ],
};

/** 五十、申请保全案件 */
export const CIVIL_PART50_PRESERVATION: CauseCategory = {
  code: 'P50',
  name: '申请保全案件',
  causes: [
    { code: '475', name: '申请诉前财产保全' },
    { code: '476', name: '申请诉前行为保全' },
    { code: '477', name: '申请诉前证据保全' },
    { code: '478', name: '申请执行前财产保全' },
    { code: '479', name: '申请中止支付信用证项下款项' },
    { code: '480', name: '申请中止支付保函项下款项' },
  ],
};

/** 五十一、申请人身安全保护令案件 */
export const CIVIL_PART51_PROTECTION_ORDER: CauseCategory = {
  code: 'P51',
  name: '申请人身安全保护令案件',
  causes: [{ code: '481', name: '申请人身安全保护令' }],
};

/** 五十二、申请人格权侵害禁令案件 */
export const CIVIL_PART52_PERSONALITY_INJUNCTION: CauseCategory = {
  code: 'P52',
  name: '申请人格权侵害禁令案件',
  causes: [{ code: '482', name: '申请人格权侵害禁令' }],
};

/** 五十三、仲裁司法审查案件 */
export const CIVIL_PART53_ARBITRATION_REVIEW: CauseCategory = {
  code: 'P53',
  name: '仲裁司法审查案件',
  causes: [
    { code: '483', name: '申请确认仲裁协议效力' },
    { code: '484', name: '申请撤销仲裁裁决' },
    { code: '485', name: '申请仲裁前财产保全' },
    { code: '486', name: '申请仲裁前行为保全' },
    { code: '487', name: '申请仲裁前证据保全' },
    { code: '488', name: '仲裁程序中的财产保全' },
    { code: '489', name: '仲裁程序中的行为保全' },
    { code: '490', name: '仲裁程序中的证据保全' },
  ],
};

/** 五十四、海事诉讼特别程序案件 */
export const CIVIL_PART54_MARITIME_PROCEDURE: CauseCategory = {
  code: 'P54',
  name: '海事诉讼特别程序案件',
  causes: [
    {
      code: '491',
      name: '申请海事请求保全',
      children: [
        { code: '491.1', name: '申请扣押船舶' },
        { code: '491.2', name: '申请拍卖扣押船舶' },
        { code: '491.3', name: '申请扣押船载货物' },
        { code: '491.4', name: '申请拍卖扣押船载货物' },
        { code: '491.5', name: '申请扣押船用燃油及船用物料' },
        { code: '491.6', name: '申请拍卖扣押船用燃油及船用物料' },
      ],
    },
    { code: '492', name: '申请海事支付令' },
    { code: '493', name: '申请海事强制令' },
    { code: '494', name: '申请海事证据保全' },
    { code: '495', name: '申请设立海事赔偿责任限制基金' },
    { code: '496', name: '申请船舶优先权催告' },
    { code: '497', name: '申请海事债权登记与受偿' },
  ],
};

/** 五十五、申请承认与执行法院判决、仲裁裁决案件 */
export const CIVIL_PART55_RECOGNITION: CauseCategory = {
  code: 'P55',
  name: '申请承认与执行法院判决、仲裁裁决案件',
  causes: [
    {
      code: '498',
      name: '申请认可和执行港澳台地区法院判决、仲裁裁决',
      children: [
        { code: '498.1', name: '申请认可和执行香港特别行政区法院民事判决' },
        { code: '498.2', name: '申请认可和执行香港特别行政区仲裁裁决' },
        { code: '498.3', name: '申请认可和执行澳门特别行政区法院民事判决' },
        { code: '498.4', name: '申请认可和执行澳门特别行政区仲裁裁决' },
        { code: '498.5', name: '申请认可和执行台湾地区法院民事判决' },
        { code: '498.6', name: '申请认可和执行台湾地区仲裁裁决' },
      ],
    },
    {
      code: '499',
      name: '申请承认和执行外国法院判决、仲裁裁决',
      children: [
        { code: '499.1', name: '申请承认和执行外国法院民事判决、裁定' },
        { code: '499.2', name: '申请承认和执行外国仲裁裁决' },
      ],
    },
  ],
};

/** 五十六、与宣告失踪、宣告死亡案件有关的纠纷 */
export const CIVIL_PART56_MISSING_DEATH_RELATED: CauseCategory = {
  code: 'P56',
  name: '与宣告失踪、宣告死亡案件有关的纠纷',
  causes: [
    { code: '500', name: '失踪人债务支付纠纷' },
    { code: '501', name: '被撤销死亡宣告人请求返还财产纠纷' },
  ],
};

/** 五十七、公益诉讼 */
export const CIVIL_PART57_PUBLIC_INTEREST: CauseCategory = {
  code: 'P57',
  name: '公益诉讼',
  causes: [
    {
      code: '502',
      name: '生态环境保护民事公益诉讼',
      children: [
        { code: '502.1', name: '环境污染民事公益诉讼' },
        { code: '502.2', name: '生态破坏民事公益诉讼' },
        { code: '502.3', name: '生态环境损害赔偿诉讼' },
      ],
    },
    { code: '503', name: '文物和文化遗产保护民事公益诉讼' },
    { code: '504', name: '安全生产民事公益诉讼' },
    { code: '505', name: '英雄烈士保护民事公益诉讼' },
    { code: '506', name: '军人权益保护民事公益诉讼' },
    { code: '507', name: '未成年人保护民事公益诉讼' },
    { code: '508', name: '消费者权益保护民事公益诉讼' },
    { code: '509', name: '个人信息保护民事公益诉讼' },
    { code: '510', name: '妇女权益保障民事公益诉讼' },
  ],
};

/** 五十八、第三人撤销之诉 */
export const CIVIL_PART58_THIRD_PARTY: CauseCategory = {
  code: 'P58',
  name: '第三人撤销之诉',
  causes: [{ code: '511', name: '第三人撤销之诉' }],
};

/** 五十九、执行程序中的异议之诉 */
export const CIVIL_PART59_EXECUTION_OBJECTION: CauseCategory = {
  code: 'P59',
  name: '执行程序中的异议之诉',
  causes: [
    {
      code: '512',
      name: '执行异议之诉',
      children: [
        { code: '512.1', name: '案外人执行异议之诉' },
        { code: '512.2', name: '申请执行人执行异议之诉' },
      ],
    },
    { code: '513', name: '追加、变更被执行人异议之诉' },
    { code: '514', name: '执行分配方案异议之诉' },
  ],
};


/** 劳动争议 - 用于劳动仲裁案由选择 */
export const CIVIL_PART6_LABOR = CIVIL_PART19_LABOR;

/** 所有民事案由 */
export const CIVIL_CAUSES: CauseCategory[] = [
  CIVIL_PART1_PERSONALITY,
  CIVIL_PART2_MARRIAGE,
  CIVIL_PART3_INHERITANCE,
  CIVIL_PART4_PROPERTY_REGISTRATION,
  CIVIL_PART5_PROPERTY_PROTECTION,
  CIVIL_PART6_OWNERSHIP,
  CIVIL_PART7_USUFRUCT,
  CIVIL_PART8_SECURITY,
  CIVIL_PART9_POSSESSION,
  CIVIL_PART10_CONTRACT,
  CIVIL_PART11_UNJUST_ENRICHMENT,
  CIVIL_PART12_NEGOTIORUM_GESTIO,
  CIVIL_PART13_IP_CONTRACT,
  CIVIL_PART14_IP_INFRINGEMENT,
  CIVIL_PART15_UNFAIR_COMPETITION,
  CIVIL_PART16_MONOPOLY,
  CIVIL_PART17_DATA,
  CIVIL_PART18_VIRTUAL_PROPERTY,
  CIVIL_PART19_LABOR,
  CIVIL_PART20_PERSONNEL,
  CIVIL_PART21_NEW_EMPLOYMENT,
  CIVIL_PART22_MARITIME,
  CIVIL_PART23_ENTERPRISE,
  CIVIL_PART24_COMPANY,
  CIVIL_PART25_PARTNERSHIP,
  CIVIL_PART26_COOPERATIVE,
  CIVIL_PART27_BANKRUPTCY,
  CIVIL_PART28_SECURITIES,
  CIVIL_PART29_FUTURES,
  CIVIL_PART30_TRUST,
  CIVIL_PART31_INSURANCE,
  CIVIL_PART32_NEGOTIABLE,
  CIVIL_PART33_LETTER_OF_CREDIT,
  CIVIL_PART34_GUARANTEE,
  CIVIL_PART35_TORT,
  CIVIL_PART36_VOTER,
  CIVIL_PART37_MISSING_DEATH,
  CIVIL_PART38_CAPACITY,
  CIVIL_PART39_ESTATE_MANAGER,
  CIVIL_PART40_OWNERLESS,
  CIVIL_PART41_MEDIATION,
  CIVIL_PART42_ECO_DAMAGE,
  CIVIL_PART43_SECURITY_REALIZATION,
  CIVIL_PART44_GUARDIANSHIP,
  CIVIL_PART45_PAYMENT_ORDER,
  CIVIL_PART46_PUBLIC_SUMMONS,
  CIVIL_PART47_COMPANY_LIQUIDATION,
  CIVIL_PART48_BANKRUPTCY_PROCEDURE,
  CIVIL_PART49_IP_INJUNCTION,
  CIVIL_PART50_PRESERVATION,
  CIVIL_PART51_PROTECTION_ORDER,
  CIVIL_PART52_PERSONALITY_INJUNCTION,
  CIVIL_PART53_ARBITRATION_REVIEW,
  CIVIL_PART54_MARITIME_PROCEDURE,
  CIVIL_PART55_RECOGNITION,
  CIVIL_PART56_MISSING_DEATH_RELATED,
  CIVIL_PART57_PUBLIC_INTEREST,
  CIVIL_PART58_THIRD_PARTY,
  CIVIL_PART59_EXECUTION_OBJECTION,
];
