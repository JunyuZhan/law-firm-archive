-- ============================================
-- 迁移脚本：添加代理阶段字典数据
-- ============================================
-- 日期: 2026-01-12
-- 描述: 为不同案件类型添加代理阶段字典
-- ============================================

-- ==================== 代理阶段字典 ====================

-- 51. 民事案件代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (51, '民事案件代理阶段', 'litigation_stage_civil', '民事案件的诉讼代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(510, 51, '一审', 'FIRST_INSTANCE', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(511, 51, '二审', 'SECOND_INSTANCE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(512, 51, '再审', 'RETRIAL', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(513, 51, '执行', 'EXECUTION', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 52. 刑事案件代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (52, '刑事案件代理阶段', 'litigation_stage_criminal', '刑事案件的诉讼代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(520, 52, '侦查阶段', 'INVESTIGATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(521, 52, '审查起诉', 'PROSECUTION_REVIEW', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(522, 52, '一审', 'FIRST_INSTANCE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(523, 52, '二审', 'SECOND_INSTANCE', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(524, 52, '死刑复核', 'DEATH_PENALTY_REVIEW', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(525, 52, '再审', 'RETRIAL', 6, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 53. 行政案件代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (53, '行政案件代理阶段', 'litigation_stage_administrative', '行政案件的诉讼代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(530, 53, '行政复议', 'ADMINISTRATIVE_RECONSIDERATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(531, 53, '一审', 'FIRST_INSTANCE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(532, 53, '二审', 'SECOND_INSTANCE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(533, 53, '再审', 'RETRIAL', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 54. 劳动仲裁代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (54, '劳动仲裁代理阶段', 'litigation_stage_labor_arbitration', '劳动仲裁的代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(540, 54, '仲裁阶段', 'ARBITRATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(541, 54, '一审', 'FIRST_INSTANCE', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(542, 54, '二审', 'SECOND_INSTANCE', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(543, 54, '再审', 'RETRIAL', 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(544, 54, '执行', 'EXECUTION', 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 55. 商事仲裁代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (55, '商事仲裁代理阶段', 'litigation_stage_commercial_arbitration', '商事仲裁的代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(550, 55, '仲裁阶段', 'ARBITRATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(551, 55, '执行', 'EXECUTION', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 56. 执行案件代理阶段
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (56, '执行案件代理阶段', 'litigation_stage_enforcement', '执行案件的代理阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(560, 56, '执行阶段', 'EXECUTION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(561, 56, '执行异议', 'EXECUTION_OBJECTION', 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(562, 56, '执行复议', 'EXECUTION_REVIEW', 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- 57. 非诉项目代理阶段（默认）
INSERT INTO sys_dict_type (id, name, code, description, status, is_system, created_at, updated_at, deleted)
VALUES (57, '非诉项目服务阶段', 'litigation_stage_default', '非诉项目的服务阶段', 'ENABLED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description;

INSERT INTO sys_dict_item (id, dict_type_id, label, value, sort_order, status, created_at, updated_at, deleted)
VALUES 
(570, 57, '非诉服务', 'NON_LITIGATION', 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, value = EXCLUDED.value, sort_order = EXCLUDED.sort_order;

-- ==================== 更新序列 ====================

SELECT setval('sys_dict_type_id_seq', GREATEST((SELECT MAX(id) FROM sys_dict_type), 60));
SELECT setval('sys_dict_item_id_seq', GREATEST((SELECT MAX(id) FROM sys_dict_item), 600));

-- ============================================
-- 字典编码与案件类型对照表
-- ============================================
-- 案件类型         | 字典编码
-- ----------------|-----------------------------------
-- CIVIL           | litigation_stage_civil
-- CRIMINAL        | litigation_stage_criminal
-- ADMINISTRATIVE  | litigation_stage_administrative
-- LABOR_ARBITRATION | litigation_stage_labor_arbitration
-- COMMERCIAL_ARBITRATION | litigation_stage_commercial_arbitration
-- ENFORCEMENT     | litigation_stage_enforcement
-- 其他（非诉）    | litigation_stage_default
-- ============================================
