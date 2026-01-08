-- 修复 case_type 字段注释，统一案件类型定义
-- 案件类型应为：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务
-- NON_LITIGATION 是 matter_type（项目大类），不是 case_type（案件类型）

-- 更新 dossier_template 表的 case_type 字段注释
COMMENT ON COLUMN dossier_template.case_type IS '案件类型: CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务';

-- 更新 case_library 表的 case_type 字段注释
COMMENT ON COLUMN case_library.case_type IS '案件类型: CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务';

