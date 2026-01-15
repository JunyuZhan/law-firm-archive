-- =============================================
-- 案由/罪名数据表
-- =============================================

-- 案由类型枚举: CIVIL(民事), CRIMINAL(刑事), ADMIN(行政)
CREATE TABLE IF NOT EXISTS sys_cause_of_action (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,                    -- 案由代码，如 '14', '8.1'
    name VARCHAR(200) NOT NULL,                   -- 案由名称
    cause_type VARCHAR(20) NOT NULL,              -- 类型: CIVIL, CRIMINAL, ADMIN
    category_code VARCHAR(20),                    -- 所属大类代码，如 'P2'
    category_name VARCHAR(100),                   -- 所属大类名称，如 '婚姻家庭纠纷'
    parent_code VARCHAR(20),                      -- 父级案由代码（用于子案由）
    level INT DEFAULT 1,                          -- 层级: 1=一级案由, 2=二级案由
    sort_order INT DEFAULT 0,                     -- 排序号
    is_active BOOLEAN DEFAULT TRUE,               -- 是否启用
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(code, cause_type)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_cause_type ON sys_cause_of_action(cause_type);
CREATE INDEX IF NOT EXISTS idx_cause_code ON sys_cause_of_action(code);
CREATE INDEX IF NOT EXISTS idx_cause_category ON sys_cause_of_action(category_code);
CREATE INDEX IF NOT EXISTS idx_cause_parent ON sys_cause_of_action(parent_code);
CREATE INDEX IF NOT EXISTS idx_cause_active ON sys_cause_of_action(is_active);

COMMENT ON TABLE sys_cause_of_action IS '案由/罪名数据表';
COMMENT ON COLUMN sys_cause_of_action.code IS '案由代码';
COMMENT ON COLUMN sys_cause_of_action.name IS '案由名称';
COMMENT ON COLUMN sys_cause_of_action.cause_type IS '类型: CIVIL-民事, CRIMINAL-刑事, ADMIN-行政';
COMMENT ON COLUMN sys_cause_of_action.category_code IS '所属大类代码';
COMMENT ON COLUMN sys_cause_of_action.category_name IS '所属大类名称';
COMMENT ON COLUMN sys_cause_of_action.parent_code IS '父级案由代码';
COMMENT ON COLUMN sys_cause_of_action.level IS '层级';
COMMENT ON COLUMN sys_cause_of_action.sort_order IS '排序号';
COMMENT ON COLUMN sys_cause_of_action.is_active IS '是否启用';

-- =============================================
-- 插入民事案由数据 (部分常用案由示例)
-- =============================================

-- 一、人格权纠纷
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('1', '生命权、身体权、健康权纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 1),
('2', '姓名权纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 2),
('3', '名称权纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 3),
('4', '肖像权纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 4),
('5', '声音保护纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 5),
('6', '名誉权纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 6),
('7', '荣誉权纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 7),
('8', '隐私权、个人信息保护纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 8),
('8.1', '隐私权纠纷', 'CIVIL', 'P1', '人格权纠纷', 2, 81) ON CONFLICT DO NOTHING,
('8.2', '个人信息保护纠纷', 'CIVIL', 'P1', '人格权纠纷', 2, 82) ON CONFLICT DO NOTHING,
('9', '婚姻自主权纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 9),
('10', '人身自由权纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 10),
('11', '一般人格权纠纷', 'CIVIL', 'P1', '人格权纠纷', 1, 11),
('11.1', '平等就业权纠纷', 'CIVIL', 'P1', '人格权纠纷', 2, 111) ON CONFLICT DO NOTHING;

-- 二、婚姻家庭纠纷
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('12', '婚约财产纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 12),
('13', '婚内夫妻财产分割纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 13),
('14', '离婚纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 14),
('15', '离婚后财产纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 15),
('16', '离婚后损害责任纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 16),
('17', '婚姻无效纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 17),
('18', '撤销婚姻纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 18),
('19', '夫妻财产约定纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 19),
('20', '同居关系纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 20),
('20.1', '同居关系析产纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 2, 201) ON CONFLICT DO NOTHING,
('20.2', '同居关系子女抚养纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 2, 202) ON CONFLICT DO NOTHING,
('21', '亲子关系纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 21),
('21.1', '确认亲子关系纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 2, 211) ON CONFLICT DO NOTHING,
('21.2', '否认亲子关系纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 2, 212) ON CONFLICT DO NOTHING,
('22', '抚养纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 22),
('22.1', '抚养费纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 2, 221) ON CONFLICT DO NOTHING,
('22.2', '变更抚养关系纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 2, 222) ON CONFLICT DO NOTHING,
('23', '扶养纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 23),
('24', '赡养纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 24),
('25', '收养关系纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 25),
('26', '监护权纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 26),
('27', '探望权纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 27),
('28', '分家析产纠纷', 'CIVIL', 'P2', '婚姻家庭纠纷', 1, 28) ON CONFLICT DO NOTHING;

-- 三、继承纠纷
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('29', '法定继承纠纷', 'CIVIL', 'P3', '继承纠纷', 1, 29),
('29.1', '转继承纠纷', 'CIVIL', 'P3', '继承纠纷', 2, 291) ON CONFLICT DO NOTHING,
('29.2', '代位继承纠纷', 'CIVIL', 'P3', '继承纠纷', 2, 292) ON CONFLICT DO NOTHING,
('30', '遗嘱继承纠纷', 'CIVIL', 'P3', '继承纠纷', 1, 30),
('31', '被继承人债务清偿纠纷', 'CIVIL', 'P3', '继承纠纷', 1, 31),
('32', '遗赠纠纷', 'CIVIL', 'P3', '继承纠纷', 1, 32),
('33', '遗赠扶养协议纠纷', 'CIVIL', 'P3', '继承纠纷', 1, 33),
('34', '非遗产继承人分配遗产纠纷', 'CIVIL', 'P3', '继承纠纷', 1, 34),
('35', '遗产管理纠纷', 'CIVIL', 'P3', '继承纠纷', 1, 35) ON CONFLICT DO NOTHING;

-- 十、合同纠纷（常用）
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('78', '缔约过失责任纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 78),
('79', '预约合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 79),
('80', '确认合同效力纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 80),
('81', '债权人代位权纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 81),
('82', '债权人撤销权纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 82),
('88', '买卖合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 88),
('88.1', '分期付款买卖合同纠纷', 'CIVIL', 'P10', '合同纠纷', 2, 881) ON CONFLICT DO NOTHING,
('88.7', '国际货物买卖合同纠纷', 'CIVIL', 'P10', '合同纠纷', 2, 887) ON CONFLICT DO NOTHING,
('96', '房屋买卖合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 96),
('96.1', '商品房预约合同纠纷', 'CIVIL', 'P10', '合同纠纷', 2, 961) ON CONFLICT DO NOTHING,
('96.2', '商品房预售合同纠纷', 'CIVIL', 'P10', '合同纠纷', 2, 962) ON CONFLICT DO NOTHING,
('96.3', '商品房销售合同纠纷', 'CIVIL', 'P10', '合同纠纷', 2, 963) ON CONFLICT DO NOTHING,
('108', '借款合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 108),
('108.1', '金融借款合同纠纷', 'CIVIL', 'P10', '合同纠纷', 2, 1081) ON CONFLICT DO NOTHING,
('108.3', '民间借贷纠纷', 'CIVIL', 'P10', '合同纠纷', 2, 1083) ON CONFLICT DO NOTHING,
('109', '保证合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 109),
('116', '租赁合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 116),
('116.2', '房屋租赁合同纠纷', 'CIVIL', 'P10', '合同纠纷', 2, 1162) ON CONFLICT DO NOTHING,
('117', '融资租赁合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 117),
('120', '建设工程合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 120),
('120.3', '建设工程施工合同纠纷', 'CIVIL', 'P10', '合同纠纷', 2, 1203) ON CONFLICT DO NOTHING,
('124', '委托合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 124),
('126', '物业服务合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 126),
('142', '服务合同纠纷', 'CIVIL', 'P10', '合同纠纷', 1, 142) ON CONFLICT DO NOTHING;

-- 十九、劳动争议
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('205', '劳动合同纠纷', 'CIVIL', 'P19', '劳动争议', 1, 205),
('205.1', '确认劳动关系纠纷', 'CIVIL', 'P19', '劳动争议', 2, 2051) ON CONFLICT DO NOTHING,
('205.5', '追索劳动报酬纠纷', 'CIVIL', 'P19', '劳动争议', 2, 2055) ON CONFLICT DO NOTHING,
('205.6', '经济补偿金纠纷', 'CIVIL', 'P19', '劳动争议', 2, 2056) ON CONFLICT DO NOTHING,
('205.7', '竞业限制纠纷', 'CIVIL', 'P19', '劳动争议', 2, 2057) ON CONFLICT DO NOTHING,
('206', '社会保险纠纷', 'CIVIL', 'P19', '劳动争议', 1, 206),
('206.2', '工伤保险待遇纠纷', 'CIVIL', 'P19', '劳动争议', 2, 2062) ON CONFLICT DO NOTHING,
('207', '福利待遇纠纷', 'CIVIL', 'P19', '劳动争议', 1, 207) ON CONFLICT DO NOTHING;

-- 二十四、与公司有关的纠纷
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('290', '股东资格确认纠纷', 'CIVIL', 'P24', '与公司有关的纠纷', 1, 290),
('293', '股东出资纠纷', 'CIVIL', 'P24', '与公司有关的纠纷', 1, 293),
('296', '股东知情权纠纷', 'CIVIL', 'P24', '与公司有关的纠纷', 1, 296),
('298', '股权转让纠纷', 'CIVIL', 'P24', '与公司有关的纠纷', 1, 298),
('299', '公司决议纠纷', 'CIVIL', 'P24', '与公司有关的纠纷', 1, 299),
('304', '公司盈余分配纠纷', 'CIVIL', 'P24', '与公司有关的纠纷', 1, 304),
('305', '损害股东利益责任纠纷', 'CIVIL', 'P24', '与公司有关的纠纷', 1, 305),
('306', '损害公司利益责任纠纷', 'CIVIL', 'P24', '与公司有关的纠纷', 1, 306),
('315', '公司解散纠纷', 'CIVIL', 'P24', '与公司有关的纠纷', 1, 315) ON CONFLICT DO NOTHING;

-- 三十五、侵权责任纠纷
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('402', '监护人责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 402),
('403', '用人单位责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 403),
('406', '提供劳务者受害责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 406),
('407', '违反安全保障义务责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 407),
('410', '产品责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 410),
('411', '机动车交通事故责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 411),
('413', '医疗损害责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 413),
('414', '环境污染责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 414),
('419', '饲养动物损害责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 419),
('420', '建筑物和物件损害责任纠纷', 'CIVIL', 'P35', '侵权责任纠纷', 1, 420) ON CONFLICT DO NOTHING;

-- =============================================
-- 插入刑事罪名数据（常用罪名）
-- =============================================

-- 危害国家安全罪
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C101', '背叛国家罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 101),
('C102', '分裂国家罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 102),
('C103', '煽动分裂国家罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 103),
('C110', '间谍罪', 'CRIMINAL', 'C1', '危害国家安全罪', 1, 110) ON CONFLICT DO NOTHING;

-- 危害公共安全罪
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C114', '放火罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 114),
('C115', '决水罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 115),
('C116', '爆炸罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 116),
('C117', '投放危险物质罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 117),
('C122', '劫持航空器罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 122),
('C133', '交通肇事罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 133),
('C133A', '危险驾驶罪', 'CRIMINAL', 'C2', '危害公共安全罪', 1, 1331) ON CONFLICT DO NOTHING;

-- 破坏社会主义市场经济秩序罪
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C140', '生产、销售伪劣产品罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 140),
('C141', '生产、销售假药罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 141),
('C158', '虚报注册资本罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 158),
('C159', '虚假出资、抽逃出资罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 159),
('C160', '欺诈发行证券罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 160),
('C175', '高利转贷罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 175),
('C176', '非法吸收公众存款罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 176),
('C192', '集资诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 192),
('C193', '贷款诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 193),
('C194', '票据诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 194),
('C196', '信用卡诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 196),
('C201', '逃税罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 201),
('C213', '假冒注册商标罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 213),
('C217', '侵犯著作权罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 217),
('C219', '侵犯商业秘密罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 219),
('C224', '合同诈骗罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 224),
('C225', '非法经营罪', 'CRIMINAL', 'C3', '破坏社会主义市场经济秩序罪', 1, 225) ON CONFLICT DO NOTHING;

-- 侵犯公民人身权利、民主权利罪
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C232', '故意杀人罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 232),
('C233', '过失致人死亡罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 233),
('C234', '故意伤害罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 234),
('C235', '过失致人重伤罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 235),
('C236', '强奸罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 236),
('C238', '非法拘禁罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 238),
('C239', '绑架罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 239),
('C240', '拐卖妇女、儿童罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 240),
('C245', '非法搜查罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 245),
('C246', '侮辱罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 246),
('C246A', '诽谤罪', 'CRIMINAL', 'C4', '侵犯公民人身权利、民主权利罪', 1, 2461) ON CONFLICT DO NOTHING;

-- 侵犯财产罪
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C263', '抢劫罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 263),
('C264', '盗窃罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 264),
('C266', '诈骗罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 266),
('C267', '抢夺罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 267),
('C270', '侵占罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 270),
('C271', '职务侵占罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 271),
('C272', '挪用资金罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 272),
('C274', '敲诈勒索罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 274),
('C275', '故意毁坏财物罪', 'CRIMINAL', 'C5', '侵犯财产罪', 1, 275) ON CONFLICT DO NOTHING;

-- 妨害社会管理秩序罪
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C277', '妨害公务罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 277),
('C291', '聚众扰乱公共场所秩序罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 291),
('C292', '聚众斗殴罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 292),
('C293', '寻衅滋事罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 293),
('C303', '赌博罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 303),
('C303A', '开设赌场罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 3031),
('C305', '伪证罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 305),
('C307', '帮助毁灭、伪造证据罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 307),
('C310', '窝藏、包庇罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 310),
('C312', '掩饰、隐瞒犯罪所得罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 312),
('C347', '走私、贩卖、运输、制造毒品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 347),
('C348', '非法持有毒品罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 348),
('C354', '容留他人吸毒罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 354),
('C358', '组织卖淫罪', 'CRIMINAL', 'C6', '妨害社会管理秩序罪', 1, 358) ON CONFLICT DO NOTHING;

-- 贪污贿赂罪
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C382', '贪污罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 382),
('C383', '挪用公款罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 383),
('C385', '受贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 385),
('C387', '单位受贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 387),
('C389', '行贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 389),
('C390', '对单位行贿罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 390),
('C391', '介绍贿赂罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 391),
('C395', '巨额财产来源不明罪', 'CRIMINAL', 'C8', '贪污贿赂罪', 1, 395) ON CONFLICT DO NOTHING;

-- 渎职罪
INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
('C397', '滥用职权罪', 'CRIMINAL', 'C9', '渎职罪', 1, 397),
('C398', '玩忽职守罪', 'CRIMINAL', 'C9', '渎职罪', 1, 398),
('C399', '徇私枉法罪', 'CRIMINAL', 'C9', '渎职罪', 1, 399) ON CONFLICT DO NOTHING;

-- =============================================
-- 插入行政案由数据（常用）
-- =============================================

INSERT INTO sys_cause_of_action (code, name, cause_type, category_code, category_name, level, sort_order) VALUES
-- 行政处罚
('A1', '行政处罚', 'ADMIN', 'A1', '行政处罚', 1, 1),
('A1.1', '警告', 'ADMIN', 'A1', '行政处罚', 2, 11),
('A1.2', '罚款', 'ADMIN', 'A1', '行政处罚', 2, 12),
('A1.3', '没收违法所得', 'ADMIN', 'A1', '行政处罚', 2, 13),
('A1.4', '没收非法财物', 'ADMIN', 'A1', '行政处罚', 2, 14),
('A1.5', '责令停产停业', 'ADMIN', 'A1', '行政处罚', 2, 15),
('A1.6', '暂扣许可证件', 'ADMIN', 'A1', '行政处罚', 2, 16),
('A1.7', '吊销许可证件', 'ADMIN', 'A1', '行政处罚', 2, 17),
('A1.8', '行政拘留', 'ADMIN', 'A1', '行政处罚', 2, 18),

-- 行政许可
('A2', '行政许可', 'ADMIN', 'A2', '行政许可', 1, 2),
('A2.1', '准予行政许可', 'ADMIN', 'A2', '行政许可', 2, 21),
('A2.2', '不予行政许可', 'ADMIN', 'A2', '行政许可', 2, 22),
('A2.3', '变更行政许可', 'ADMIN', 'A2', '行政许可', 2, 23),
('A2.4', '延续行政许可', 'ADMIN', 'A2', '行政许可', 2, 24),
('A2.5', '撤销行政许可', 'ADMIN', 'A2', '行政许可', 2, 25),
('A2.6', '注销行政许可', 'ADMIN', 'A2', '行政许可', 2, 26),

-- 行政强制
('A3', '行政强制', 'ADMIN', 'A3', '行政强制', 1, 3),
('A3.1', '查封', 'ADMIN', 'A3', '行政强制', 2, 31),
('A3.2', '扣押', 'ADMIN', 'A3', '行政强制', 2, 32),
('A3.3', '冻结', 'ADMIN', 'A3', '行政强制', 2, 33),
('A3.4', '强制拆除', 'ADMIN', 'A3', '行政强制', 2, 34),
('A3.5', '强制执行', 'ADMIN', 'A3', '行政强制', 2, 35),

-- 行政征收
('A4', '行政征收', 'ADMIN', 'A4', '行政征收', 1, 4),
('A4.1', '税收征收', 'ADMIN', 'A4', '行政征收', 2, 41),
('A4.2', '行政收费', 'ADMIN', 'A4', '行政征收', 2, 42),
('A4.3', '社会保险费征收', 'ADMIN', 'A4', '行政征收', 2, 43),

-- 行政征用
('A5', '行政征用', 'ADMIN', 'A5', '行政征用', 1, 5),

-- 行政确认
('A6', '行政确认', 'ADMIN', 'A6', '行政确认', 1, 6),
('A6.1', '权属确认', 'ADMIN', 'A6', '行政确认', 2, 61),
('A6.2', '资质确认', 'ADMIN', 'A6', '行政确认', 2, 62),
('A6.3', '身份确认', 'ADMIN', 'A6', '行政确认', 2, 63),

-- 行政给付
('A7', '行政给付', 'ADMIN', 'A7', '行政给付', 1, 7),

-- 行政裁决
('A8', '行政裁决', 'ADMIN', 'A8', '行政裁决', 1, 8),

-- 行政补偿
('A9', '行政补偿', 'ADMIN', 'A9', '行政补偿', 1, 9),

-- 行政赔偿
('A10', '行政赔偿', 'ADMIN', 'A10', '行政赔偿', 1, 10),

-- 政府信息公开
('A11', '政府信息公开', 'ADMIN', 'A11', '政府信息公开', 1, 11),

-- 行政复议
('A12', '行政复议', 'ADMIN', 'A12', '行政复议', 1, 12),
('A12.1', '维持原行政行为', 'ADMIN', 'A12', '行政复议', 2, 121),
('A12.2', '撤销原行政行为', 'ADMIN', 'A12', '行政复议', 2, 122),
('A12.3', '变更原行政行为', 'ADMIN', 'A12', '行政复议', 2, 123),
('A12.4', '确认违法', 'ADMIN', 'A12', '行政复议', 2, 124),
('A12.5', '驳回复议申请', 'ADMIN', 'A12', '行政复议', 2, 125),

-- 行政协议
('A13', '行政协议', 'ADMIN', 'A13', '行政协议', 1, 13),
('A13.1', '政府特许经营协议', 'ADMIN', 'A13', '行政协议', 2, 131),
('A13.2', '土地房屋征收补偿协议', 'ADMIN', 'A13', '行政协议', 2, 132),
('A13.3', '国有土地使用权出让协议', 'ADMIN', 'A13', '行政协议', 2, 133) ON CONFLICT DO NOTHING;

-- =============================================
-- 创建案由查询函数
-- =============================================

-- 根据代码和类型获取案由名称
CREATE OR REPLACE FUNCTION get_cause_name(p_code VARCHAR, p_type VARCHAR DEFAULT 'CIVIL')
RETURNS VARCHAR AS $$
DECLARE
    v_name VARCHAR;
BEGIN
    SELECT name INTO v_name
    FROM sys_cause_of_action
    WHERE code = p_code AND cause_type = p_type AND is_active = TRUE
    LIMIT 1;
    
    RETURN COALESCE(v_name, p_code);
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_cause_name IS '根据案由代码获取案由名称';
