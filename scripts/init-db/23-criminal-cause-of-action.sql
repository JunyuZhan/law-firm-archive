-- =============================================
-- 插入刑事罪名数据（完整数据，共483个罪名）
-- 根据《中华人民共和国刑法》及司法解释整理
-- =============================================

-- 第一章 危害国家安全罪（12个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C101', '背叛国家罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 101),
('C102', '分裂国家罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 102),
('C103', '煽动分裂国家罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 103),
('C104', '武装叛乱、暴乱罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 104),
('C105', '颠覆国家政权罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 105),
('C106', '煽动颠覆国家政权罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 106),
('C107', '资助危害国家安全犯罪活动罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 107),
('C108', '投敌叛变罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 108),
('C109', '叛逃罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 109),
('C110', '间谍罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 110),
('C111', '为境外窃取、剌探、收买、非法提供国家秘密、情报罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 111),
('C112', '资敌罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 112)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第二章 危害公共安全罪（54个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C201', '放火罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 201),
('C202', '决水罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 202),
('C203', '爆炸罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 203),
('C204', '投放危险物质罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 204),
('C205', '以危险方法危害公共安全罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 205),
('C206', '失火罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 206),
('C207', '过失决水罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 207),
('C208', '过失爆炸罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 208),
('C209', '过失投放危险物质罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 209),
('C210', '过失以危险方法危害公共安全罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 210),
('C211', '破坏交通工具罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 211),
('C212', '破坏交通设施罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 212),
('C213', '破坏电力设备罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 213),
('C214', '破坏易燃易爆设备罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 214),
('C215', '过失损坏交通工具罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 215),
('C216', '过失损坏交通设施罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 216),
('C217', '过失损坏电力设备罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 217),
('C218', '过失损坏易燃易爆设备罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 218),
('C219', '组织、领导、参加恐怖组织罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 219),
('C220', '帮助恐怖活动罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 220),
('C221', '准备实施恐怖活动罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 221),
('C222', '宣扬恐怖主义、极端主义、煽动实施恐怖活动罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 222),
('C223', '利用极端主义破坏法律实施罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 223),
('C224', '强制穿戴宣扬恐怖主义、极端主义服饰、标志罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 224),
('C225', '非法持有宣扬恐怖主义、极端主义物品罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 225),
('C226', '劫持航空器罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 226),
('C227', '劫持船只、汽车罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 227),
('C228', '暴力危及飞行安全罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 228),
('C229', '破坏广播电视设施、公用电信设施罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 229),
('C230', '过失损坏广播电视设施、公用电信设施罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 230),
('C231', '非法制造、买卖、运输、邮寄、储存枪支、弹药、爆炸物罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 231),
('C232', '非法制造、买卖、运输、储存危险物质罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 232),
('C233', '违规制造、销售枪支罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 233),
('C234', '盗窃、抢夺枪支、弹药、爆炸物、危险物质罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 234),
('C235', '抢劫枪支、弹药、爆炸物、危险物质罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 235),
('C236', '非法持有、私藏枪支、弹药罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 236),
('C237', '非法出租、出借枪支罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 237),
('C238', '丢失枪支不报罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 238),
('C239', '非法携带枪支、弹药、管制刀具、危险物品危及公共安全罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 239),
('C240', '重大飞行事故罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 240),
('C241', '铁路运营安全事故罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 241),
('C242', '交通肇事罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 242),
('C243', '危险驾驶罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 243),
('C244', '妨害安全驾驶罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 244),
('C245', '重大责任事故罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 245),
('C246', '强令、组织他人违章冒险作业罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 246),
('C247', '危险作业罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 247),
('C248', '重大劳动安全事故罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 248),
('C249', '大型群众性活动重大安全事故罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 249),
('C250', '危险物品肇事罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 250),
('C251', '工程重大安全事故罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 251),
('C252', '教育设施重大安全事故罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 252),
('C253', '消防责任事故罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 253),
('C254', '不报、谎报安全事故罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 254)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第三章 破坏社会主义市场经济秩序罪（110个罪名）
-- 第一节 生产、销售伪劣商品罪（9个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C301', '生产、销售伪劣产品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 301),
('C302', '生产、销售、提供假药罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 302),
('C303', '生产、销售、提供劣药罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 303),
('C304', '妨害药品管理罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 304),
('C305', '生产、销售不符合安全标准的食品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 305),
('C306', '生产、销售有毒、有害食品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 306),
('C307', '生产、销售不符合标准的医用器材罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 307),
('C308', '生产、销售不符合安全标准的产品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 308),
('C309', '生产、销售伪劣农药、兽药、化肥、种子罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 309),
('C310', '生产、销售不符合卫生标准的化妆品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 310)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第二节 走私罪（10个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C311', '走私武器、弹药罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 311),
('C312', '走私核材料罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 312),
('C313', '走私假币罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 313),
('C314', '走私文物罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 314),
('C315', '走私贵重金属罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 315),
('C316', '走私珍贵动物、珍贵动物制品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 316),
('C317', '走私国家禁止进出口的货物、物品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 317),
('C318', '走私淫秽物品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 318),
('C319', '走私普通货物、物品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 319),
('C320', '走私废物罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 320)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第三节 妨害对公司、企业的管理秩序罪（17个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C321', '虚报注册资本罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 321),
('C322', '虚假出资、抽逃出资罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 322),
('C323', '欺诈发行证券罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 323),
('C324', '违规披露、不披露重要信息罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 324),
('C325', '妨害清算罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 325),
('C326', '虚假破产罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 326),
('C327', '隐匿、故意销毁会计凭证、会计帐簿、财务会计报告罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 327),
('C328', '非国家工作人员受贿罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 328),
('C329', '对非国家工作人员行贿罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 329),
('C330', '对外国公职人员、国际公共组织官员行贿罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 330),
('C331', '非法经营同类营业罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 331),
('C332', '为亲友非法牟利罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 332),
('C333', '签订、履行合同失职被骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 333),
('C334', '国有公司、企业、事业单位人员失职罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 334),
('C335', '国有公司、企业、事业单位人员滥用职权罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 335),
('C336', '徇私舞弊低价折股、出售公司、企业资产罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 336),
('C337', '背信损害上市公司利益罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 337)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第四节 破坏金融管理秩序罪（30个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C338', '伪造货币罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 338),
('C339', '出售、购买、运输假币罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 339),
('C340', '金融工作人员购买假币、以假币换取货币罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 340),
('C341', '持有、使用假币罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 341),
('C342', '变造货币罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 342),
('C343', '擅自设立金融机构罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 343),
('C344', '伪造、变造、转让金融机构经营许可证、批准文件罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 344),
('C345', '高利转贷罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 345),
('C346', '骗取贷款、票据承兑、金融票证罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 346),
('C347', '非法吸收公众存款罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 347),
('C348', '伪造、变造金融票证罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 348),
('C349', '妨害信用卡管理罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 349),
('C350', '窃取、收买、非法提供信用卡信息罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 350),
('C351', '伪造、变造国家有价证券罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 351),
('C352', '伪造、变造股票、公司、企业债券罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 352),
('C353', '擅自发行股票、公司、企业债券罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 353),
('C354', '内幕交易、泄露内幕信息罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 354),
('C355', '利用未公开信息交易罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 355),
('C356', '编造并传播证券、期货交易虚假信息罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 356),
('C357', '诱骗投资者买卖证券、期货合约罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 357),
('C358', '操纵证券、期货市场罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 358),
('C359', '背信运用受托财产罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 359),
('C360', '违法运用资金罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 360),
('C361', '违法发放贷款罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 361),
('C362', '吸收客户资金不入账罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 362),
('C363', '违规出具金融票证罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 363),
('C364', '对违法票据承兑、付款、保证罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 364),
('C365', '骗购外汇罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 365),
('C366', '逃汇罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 366),
('C367', '洗钱罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 367)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第五节 金融诈骗罪（8个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C368', '集资诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 368),
('C369', '贷款诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 369),
('C370', '票据诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 370),
('C371', '金融凭证诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 371),
('C372', '信用证诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 372),
('C373', '信用卡诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 373),
('C374', '有价证券诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 374),
('C375', '保险诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 375)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第六节 危害税收征管罪（14个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C376', '逃税罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 376),
('C377', '抗税罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 377),
('C378', '逃避追缴欠税罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 378),
('C379', '骗取出口退税罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 379),
('C380', '虚开增值税专用发票、用于骗取出口退税、抵扣税款发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 380),
('C381', '虚开发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 381),
('C382', '伪造、出售伪造的增值税专用发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 382),
('C383', '非法出售增值税专用发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 383),
('C384', '非法购买增值税专用发票、购买伪造的增值税专用发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 384),
('C385', '非法制造、出售非法制造的用于骗取出口退税、抵扣税款发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 385),
('C386', '非法制造、出售非法制造的发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 386),
('C387', '非法出售用于骗取出口退税、抵扣税款发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 387),
('C388', '非法出售发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 388),
('C389', '持有伪造的发票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 389)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第七节 侵犯知识产权罪（8个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C390', '假冒注册商标罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 390),
('C391', '销售假冒注册商标的商品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 391),
('C392', '非法制造、销售非法制造的注册商标标识罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 392),
('C393', '假冒专利罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 393),
('C394', '侵犯著作权罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 394),
('C395', '销售侵权复制品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 395),
('C396', '侵犯商业秘密罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 396),
('C397', '为境外窃取、刺探、收买、非法提供商业秘密罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 397)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第八节 扰乱市场秩序罪（13个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C398', '损害商业信誉、商品声誉罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 398),
('C399', '虚假广告罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 399),
('C400', '串通投标罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 400),
('C401', '合同诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 401),
('C402', '组织、领导传销活动罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 402),
('C403', '非法经营罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 403),
('C404', '强迫交易罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 404),
('C405', '伪造、倒卖伪造的有价票证罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 405),
('C406', '倒卖车票、船票罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 406),
('C407', '非法转让、倒卖土地使用权罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 407),
('C408', '提供虚假证明文件罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 408),
('C409', '出具证明文件重大失实罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 409),
('C410', '逃避商检罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 410)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第四章 侵犯公民人身权利、民主权利罪（45个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C411', '故意杀人罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 411),
('C412', '过失致人死亡罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 412),
('C413', '故意伤害罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 413),
('C414', '组织出卖人体器官罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 414),
('C415', '过失致人重伤罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 415),
('C416', '强奸罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 416),
('C417', '负有照护职责人员性侵罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 417),
('C418', '强制猥亵、侮辱罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 418),
('C419', '猥亵儿童罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 419),
('C420', '非法拘禁罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 420),
('C421', '绑架罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 421),
('C422', '拐卖妇女、儿童罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 422),
('C423', '收买被拐卖的妇女、儿童罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 423),
('C424', '聚众阻碍解救被收买的妇女、儿童罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 424),
('C425', '诬告陷害罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 425),
('C426', '强迫劳动罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 426),
('C427', '雇用童工从事危重劳动罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 427),
('C428', '非法搜查罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 428),
('C429', '非法侵入住宅罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 429),
('C430', '侮辱罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 430),
('C431', '诽谤罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 431),
('C432', '刑讯逼供罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 432),
('C433', '暴力取证罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 433),
('C434', '虐待被监管人罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 434),
('C435', '煽动民族仇恨、民族歧视罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 435),
('C436', '出版歧视、侮辱少数民族作品罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 436),
('C437', '非法剥夺公民宗教信仰自由罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 437),
('C438', '侵犯少数民族风俗习惯罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 438),
('C439', '侵犯通信自由罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 439),
('C440', '私自开拆、隐匿、毁弃邮件、电报罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 440),
('C441', '侵犯公民个人信息罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 441),
('C442', '报复陷害罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 442),
('C443', '打击报复会计、统计人员罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 443),
('C444', '破坏选举罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 444),
('C445', '暴力干涉婚姻自由罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 445),
('C446', '重婚罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 446),
('C447', '破坏军婚罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 447),
('C448', '虐待罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 448),
('C449', '虐待被监护、看护人罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 449),
('C450', '遗弃罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 450),
('C451', '拐骗儿童罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 451),
('C452', '组织残疾人、儿童乞讨罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 452),
('C453', '组织未成年人进行违反治安管理活动罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 453)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第五章 侵犯财产罪（13个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C454', '抢劫罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 454),
('C455', '盗窃罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 455),
('C456', '诈骗罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 456),
('C457', '抢夺罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 457),
('C458', '聚众哄抢罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 458),
('C459', '侵占罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 459),
('C460', '职务侵占罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 460),
('C461', '挪用资金罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 461),
('C462', '挪用特定款物罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 462),
('C463', '敲诈勒索罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 463),
('C464', '故意毁坏财物罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 464),
('C465', '破坏生产经营罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 465),
('C466', '拒不支付劳动报酬罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 466)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第六章 妨害社会管理秩序罪（146个罪名）
-- 第一节 扰乱公共秩序罪（56个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C467', '妨害公务罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 467),
('C468', '袭警罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 468),
('C469', '煽动暴力抗拒法律实施罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 469),
('C470', '招摇撞骗罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 470),
('C471', '伪造、变造、买卖国家机关公文、证件、印章罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 471),
('C472', '盗窃、抢夺、毁灭国家机关公文、证件、印章罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 472),
('C473', '伪造公司、企业、事业单位、人民团体印章罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 473),
('C474', '伪造、变造、买卖身份证件罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 474),
('C475', '使用虚假身份证件、盗用身份证件罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 475),
('C476', '冒名顶替罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 476),
('C477', '非法生产、买卖警用装备罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 477),
('C478', '非法获取国家秘密罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 478),
('C479', '非法持有国家绝密、机密文件、资料、物品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 479),
('C480', '非法生产、销售专用间谍器材、窃听、窃照专用器材罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 480),
('C481', '非法使用窃听、窃照专用器材罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 481),
('C482', '组织考试作弊罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 482),
('C483', '非法出售、提供试题、答案罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 483),
('C484', '代替考试罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 484),
('C485', '非法侵入计算机信息系统罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 485),
('C486', '非法获取计算机信息系统数据、非法控制计算机信息系统罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 486),
('C487', '提供侵入、非法控制计算机信息系统程序、工具罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 487),
('C488', '破坏计算机信息系统罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 488),
('C489', '拒不履行信息网络安全管理义务罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 489),
('C490', '非法利用信息网络罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 490),
('C491', '帮助信息网络犯罪活动罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 491),
('C492', '扰乱无线电通讯管理秩序罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 492),
('C493', '聚众扰乱社会秩序罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 493),
('C494', '聚众冲击国家机关罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 494),
('C495', '扰乱国家机关工作秩序罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 495),
('C496', '组织、资助非法聚集罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 496),
('C497', '聚众扰乱公共场所秩序、交通秩序罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 497),
('C498', '投放虚假危险物质罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 498),
('C499', '编造、故意传播虚假恐怖信息罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 499),
('C500', '编造、故意传播虚假信息罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 500),
('C501', '高空抛物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 501),
('C502', '聚众斗殴罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 502),
('C503', '寻衅滋事罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 503),
('C504', '催收非法债务罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 504),
('C505', '组织、领导、参加黑社会性质组织罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 505),
('C506', '入境发展黑社会组织罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 506),
('C507', '包庇、纵容黑社会性质组织罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 507),
('C508', '传授犯罪方法罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 508),
('C509', '非法集会、游行、示威罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 509),
('C510', '非法携带武器、管制刀具、爆炸物参加集会、游行、示威罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 510),
('C511', '破坏集会、游行、示威罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 511),
('C512', '侮辱国旗、国徽、国歌罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 512),
('C513', '侵害英雄烈士名誉、荣誉罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 513),
('C514', '组织、利用会道门、邪教组织、利用迷信破坏法律实施罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 514),
('C515', '组织、利用会道门、邪教组织、利用迷信致人重伤、死亡罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 515),
('C516', '聚众淫乱罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 516),
('C517', '引诱未成年人聚众淫乱罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 517),
('C518', '盗窃、侮辱、故意毁坏尸体、尸骨、骨灰罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 518),
('C519', '赌博罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 519),
('C520', '开设赌场罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 520),
('C521', '组织参与国（境）外赌博罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 521),
('C522', '故意延误投递邮件罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 522)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第二节 妨害司法罪（20个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C523', '伪证罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 523),
('C524', '辩护人、诉讼代理人毁灭证据、伪造证据、妨害作证罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 524),
('C525', '妨害作证罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 525),
('C526', '帮助毁灭、伪造证据罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 526),
('C527', '虚假诉讼罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 527),
('C528', '打击报复证人罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 528),
('C529', '泄露不应公开的案件信息罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 529),
('C530', '披露、报道不应公开的案件信息罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 530),
('C531', '扰乱法庭秩序罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 531),
('C532', '窝藏、包庇罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 532),
('C533', '拒绝提供间谍犯罪、恐怖主义犯罪、极端主义犯罪证据罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 533),
('C534', '掩饰、隐瞒犯罪所得、犯罪所得收益罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 534),
('C535', '拒不执行判决、裁定罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 535),
('C536', '非法处置查封、扣押、冻结的财产罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 536),
('C537', '破坏监管秩序罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 537),
('C538', '脱逃罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 538),
('C539', '劫夺被押解人员罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 539),
('C540', '组织越狱罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 540),
('C541', '暴动越狱罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 541),
('C542', '聚众持械劫狱罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 542)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第三节 妨害国（边）境管理罪（8个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C543', '组织他人偷越国（边）境罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 543),
('C544', '骗取出境证件罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 544),
('C545', '提供伪造、变造的出入境证件罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 545),
('C546', '出售出入境证件罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 546),
('C547', '运送他人偷越国（边）境罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 547),
('C548', '偷越国（边）境罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 548),
('C549', '破坏界碑、界桩罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 549),
('C550', '破坏永久性测量标志罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 550)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第四节 妨害文物管理罪（10个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C551', '故意损毁文物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 551),
('C552', '故意损毁名胜古迹罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 552),
('C553', '过失损毁文物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 553),
('C554', '非法向外国人出售、赠送珍贵文物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 554),
('C555', '倒卖文物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 555),
('C556', '非法出售、私赠文物藏品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 556),
('C557', '盗掘古文化遗址、古墓葬罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 557),
('C558', '盗掘古人类化石、古脊椎动物化石罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 558),
('C559', '抢夺、窃取国有档案罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 559),
('C560', '擅自出卖、转让国有档案罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 560)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第五节 危害公共卫生罪（13个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C561', '妨害传染病防治罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 561),
('C562', '传染病菌种、毒种扩散罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 562),
('C563', '妨害国境卫生检疫罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 563),
('C564', '非法组织卖血罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 564),
('C565', '强迫卖血罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 565),
('C566', '非法采集、供应血液、制作、供应血液制品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 566),
('C567', '采集、供应血液、制作、供应血液制品事故罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 567),
('C568', '非法采集人类遗传资源、走私人类遗传资源材料罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 568),
('C569', '医疗事故罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 569),
('C570', '非法行医罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 570),
('C571', '非法进行节育手术罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 571),
('C572', '非法植入基因编辑、克隆胚胎罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 572),
('C573', '妨害动植物防疫、检疫罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 573)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第六节 破坏环境资源保护罪（16个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C574', '污染环境罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 574),
('C575', '非法处置进口的固体废物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 575),
('C576', '擅自进口固体废物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 576),
('C577', '非法捕捞水产品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 577),
('C578', '危害珍贵、濒危野生动物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 578),
('C579', '非法狩猎罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 579),
('C580', '非法猎捕、收购、运输、出售陆生野生动物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 580),
('C581', '非法占用农用地罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 581),
('C582', '破坏自然保护地罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 582),
('C583', '非法采矿罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 583),
('C584', '破坏性采矿罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 584),
('C585', '危害国家重点保护植物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 585),
('C586', '非法引进、释放、丢弃外来入侵物种罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 586),
('C587', '盗伐林木罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 587),
('C588', '滥伐林木罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 588),
('C589', '非法收购、运输盗伐、滥伐的林木罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 589)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第七节 走私、贩卖、运输、制造毒品罪（12个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C590', '走私、贩卖、运输、制造毒品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 590),
('C591', '非法持有毒品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 591),
('C592', '包庇毒品犯罪分子罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 592),
('C593', '窝藏、转移、隐瞒毒品、毒赃罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 593),
('C594', '非法生产、买卖、运输制毒物品、走私制毒物品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 594),
('C595', '非法种植毒品原植物罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 595),
('C596', '非法买卖、运输、携带、持有毒品原植物种子、幼苗罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 596),
('C597', '引诱、教唆、欺骗他人吸毒罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 597),
('C598', '强迫他人吸毒罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 598),
('C599', '容留他人吸毒罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 599),
('C600', '非法提供麻醉药品、精神药品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 600),
('C601', '妨害兴奋剂管理罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 601)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第八节 组织、强迫、引诱、容留、介绍卖淫罪（6个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C602', '组织卖淫罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 602),
('C603', '强迫卖淫罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 603),
('C604', '协助组织卖淫罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 604),
('C605', '引诱、容留、介绍卖淫罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 605),
('C606', '引诱幼女卖淫罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 606),
('C607', '传播性病罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 607)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第九节 制作、贩卖、传播淫秽物品罪（5个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C608', '制作、复制、出版、贩卖、传播淫秽物品牟利罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 608),
('C609', '为他人提供书号出版淫秽书刊罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 609),
('C610', '传播淫秽物品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 610),
('C611', '组织播放淫秽音像制品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 611),
('C612', '组织淫秽表演罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 612)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第七章 危害国防利益罪（23个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C613', '阻碍军人执行职务罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 613),
('C614', '阻碍军事行动罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 614),
('C615', '破坏武器装备、军事设施、军事通信罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 615),
('C616', '过失损坏武器装备、军事设施、军事通信罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 616),
('C617', '故意提供不合格武器装备、军事设施罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 617),
('C618', '过失提供不合格武器装备、军事设施罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 618),
('C619', '聚众冲击军事禁区罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 619),
('C620', '聚众扰乱军事管理区秩序罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 620),
('C621', '冒充军人招摇撞骗罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 621),
('C622', '煽动军人逃离部队罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 622),
('C623', '雇用逃离部队军人罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 623),
('C624', '接送不合格兵员罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 624),
('C625', '伪造、变造、买卖武装部队公文、证件、印章罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 625),
('C626', '盗窃、抢夺武装部队公文、证件、印章罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 626),
('C627', '非法生产、买卖武装部队制式服装罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 627),
('C628', '伪造、盗窃、买卖、非法提供、非法使用武装部队专用标志罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 628),
('C629', '战时拒绝、逃避征召、军事训练罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 629),
('C630', '战时拒绝、逃避服役罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 630),
('C631', '战时故意提供虚假敌情罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 631),
('C632', '战时造谣扰乱军心罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 632),
('C633', '战时窝藏逃离部队军人罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 633),
('C634', '战时拒绝、故意延误军事订货罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 634),
('C635', '战时拒绝军事征收、征用罪', 'CRIMINAL', 'C7', '危害国防利益罪', 1, 635)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第八章 贪污贿赂罪（14个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C636', '贪污罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 636),
('C637', '挪用公款罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 637),
('C638', '受贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 638),
('C639', '单位受贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 639),
('C640', '利用影响力受贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 640),
('C641', '行贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 641),
('C642', '对有影响力的人行贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 642),
('C643', '对单位行贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 643),
('C644', '介绍贿赂罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 644),
('C645', '单位行贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 645),
('C646', '巨额财产来源不明罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 646),
('C647', '隐瞒境外存款罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 647),
('C648', '私分国有资产罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 648),
('C649', '私分罚没财物罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 649)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第九章 渎职罪（37个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C650', '滥用职权罪', 'CRIMINAL', 'C9', '渎职罪', 1, 650),
('C651', '玩忽职守罪', 'CRIMINAL', 'C9', '渎职罪', 1, 651),
('C652', '故意泄露国家秘密罪', 'CRIMINAL', 'C9', '渎职罪', 1, 652),
('C653', '过失泄露国家秘密罪', 'CRIMINAL', 'C9', '渎职罪', 1, 653),
('C654', '徇私枉法罪', 'CRIMINAL', 'C9', '渎职罪', 1, 654),
('C655', '民事、行政枉法裁判罪', 'CRIMINAL', 'C9', '渎职罪', 1, 655),
('C656', '执行判决、裁定失职罪', 'CRIMINAL', 'C9', '渎职罪', 1, 656),
('C657', '执行判决、裁定滥用职权罪', 'CRIMINAL', 'C9', '渎职罪', 1, 657),
('C658', '枉法仲裁罪', 'CRIMINAL', 'C9', '渎职罪', 1, 658),
('C659', '私放在押人员罪', 'CRIMINAL', 'C9', '渎职罪', 1, 659),
('C660', '失职致使在押人员脱逃罪', 'CRIMINAL', 'C9', '渎职罪', 1, 660),
('C661', '徇私舞弊减刑、假释、暂予监外执行罪', 'CRIMINAL', 'C9', '渎职罪', 1, 661),
('C662', '徇私舞弊不移交刑事案件罪', 'CRIMINAL', 'C9', '渎职罪', 1, 662),
('C663', '滥用管理公司、证券职权罪', 'CRIMINAL', 'C9', '渎职罪', 1, 663),
('C664', '徇私舞弊不征、少征税款罪', 'CRIMINAL', 'C9', '渎职罪', 1, 664),
('C665', '徇私舞弊发售发票、抵扣税款、出口退税罪', 'CRIMINAL', 'C9', '渎职罪', 1, 665),
('C666', '违法提供出口退税凭证罪', 'CRIMINAL', 'C9', '渎职罪', 1, 666),
('C667', '国家机关工作人员签订、履行合同失职被骗罪', 'CRIMINAL', 'C9', '渎职罪', 1, 667),
('C668', '违法发放林木采伐许可证罪', 'CRIMINAL', 'C9', '渎职罪', 1, 668),
('C669', '环境监管失职罪', 'CRIMINAL', 'C9', '渎职罪', 1, 669),
('C670', '食品、药品监管渎职罪', 'CRIMINAL', 'C9', '渎职罪', 1, 670),
('C671', '传染病防治失职罪', 'CRIMINAL', 'C9', '渎职罪', 1, 671),
('C672', '非法批准征收、征用、占用土地罪', 'CRIMINAL', 'C9', '渎职罪', 1, 672),
('C673', '非法低价出让国有土地使用权罪', 'CRIMINAL', 'C9', '渎职罪', 1, 673),
('C674', '放纵走私罪', 'CRIMINAL', 'C9', '渎职罪', 1, 674),
('C675', '商检徇私舞弊罪', 'CRIMINAL', 'C9', '渎职罪', 1, 675),
('C676', '商检失职罪', 'CRIMINAL', 'C9', '渎职罪', 1, 676),
('C677', '动植物检疫徇私舞弊罪', 'CRIMINAL', 'C9', '渎职罪', 1, 677),
('C678', '动植物检疫失职罪', 'CRIMINAL', 'C9', '渎职罪', 1, 678),
('C679', '放纵制售伪劣商品犯罪行为罪', 'CRIMINAL', 'C9', '渎职罪', 1, 679),
('C680', '办理偷越国（边）境人员出入境证件罪', 'CRIMINAL', 'C9', '渎职罪', 1, 680),
('C681', '放行偷越国（边）境人员罪', 'CRIMINAL', 'C9', '渎职罪', 1, 681),
('C682', '不解救被拐卖、绑架妇女、儿童罪', 'CRIMINAL', 'C9', '渎职罪', 1, 682),
('C683', '阻碍解救被拐卖、绑架妇女、儿童罪', 'CRIMINAL', 'C9', '渎职罪', 1, 683),
('C684', '帮助犯罪分子逃避处罚罪', 'CRIMINAL', 'C9', '渎职罪', 1, 684),
('C685', '招收公务员、学生徇私舞弊罪', 'CRIMINAL', 'C9', '渎职罪', 1, 685),
('C686', '失职造成珍贵文物损毁、流失罪', 'CRIMINAL', 'C9', '渎职罪', 1, 686)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order;

-- 第十章 军人违反职责罪（31个罪名）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C687', '战时违抗命令罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 687),
('C688', '隐瞒、谎报军情罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 688),
('C689', '拒传、假传军令罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 689),
('C690', '投降罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 690),
('C691', '战时临阵脱逃罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 691),
('C692', '擅离、玩忽军事职守罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 692),
('C693', '阻碍执行军事职务罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 693),
('C694', '指使部属违反职责罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 694),
('C695', '违令作战消极罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 695),
('C696', '拒不救援友邻部队罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 696),
('C697', '军人叛逃罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 697),
('C698', '非法获取军事秘密罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 698),
('C699', '为境外窃取、剌探、收买、非法提供军事秘密罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 699),
('C700', '故意泄露军事秘密罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 700),
('C701', '过失泄露军事秘密罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 701),
('C702', '战时造谣惑众罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 702),
('C703', '战时自伤罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 703),
('C704', '逃离部队罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 704),
('C705', '武器装备肇事罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 705),
('C706', '擅自改变武器装备编配用途罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 706),
('C707', '盗窃、抢夺武器装备、军用物资罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 707),
('C708', '非法出卖、转让武器装备罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 708),
('C709', '遗弃武器装备罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 709),
('C710', '遗失武器装备罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 710),
('C711', '擅自出卖、转让军队房地产罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 711),
('C712', '虐待部属罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 712),
('C713', '遗弃伤病军人罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 713),
('C714', '战时拒不救治伤病军人罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 714),
('C715', '战时残害居民、掠夺居民财物罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 715),
('C716', '私放俘虏罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 716),
('C717', '虐待俘虏罪', 'CRIMINAL', 'C10', '军人违反职责罪', 1, 717)
ON CONFLICT (code, cause_type) DO UPDATE SET name = EXCLUDED.name, category_code = EXCLUDED.category_code, category_name = EXCLUDED.category_name, sort_order = EXCLUDED.sort_order; 