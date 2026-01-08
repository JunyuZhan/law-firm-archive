-- 调整一级菜单顺序，按照律所业务流程合理排序
-- 执行时间: 2026-01-08

BEGIN;

-- ==================== 一级菜单排序调整 ====================
-- 业务逻辑顺序：工作台 -> 日程/审批 -> 业务核心(客户/项目/财务) -> 支撑(卷宗/档案/行政/人力) -> 知识/报表 -> 系统管理

-- 1. 工作台 - 首页入口，保持第一位
UPDATE sys_menu SET sort_order = 1 WHERE id = 1 AND name = '工作台';

-- 2. 日程管理 - 个人工作日程，高频使用
UPDATE sys_menu SET sort_order = 2 WHERE id = 149 AND name = '日程管理';

-- 3. 审批中心 - 待办审批，需要及时处理
UPDATE sys_menu SET sort_order = 3 WHERE id = 701 AND name = '审批中心';

-- 4. 客户管理 - 业务源头
UPDATE sys_menu SET sort_order = 4 WHERE id = 3 AND name = '客户管理';

-- 5. 项目管理 - 核心业务
UPDATE sys_menu SET sort_order = 5 WHERE id = 4 AND name = '项目管理';

-- 6. 财务管理 - 财务相关
UPDATE sys_menu SET sort_order = 6 WHERE id = 5 AND name = '财务管理';

-- 7. 卷宗管理 - 文书文档
UPDATE sys_menu SET sort_order = 7 WHERE id = 6 AND name = '卷宗管理';

-- 8. 档案管理 - 归档管理
UPDATE sys_menu SET sort_order = 8 WHERE id = 8 AND name = '档案管理';

-- 9. 行政管理 - 行政事务
UPDATE sys_menu SET sort_order = 9 WHERE id = 9 AND name = '行政管理';

-- 10. 人力资源 - 人事管理
UPDATE sys_menu SET sort_order = 10 WHERE id = 10 AND name = '人力资源';

-- 11. 知识库 - 知识沉淀
UPDATE sys_menu SET sort_order = 11 WHERE id = 11 AND name = '知识库';

-- 12. 报表中心 - 统计分析，偏管理层使用
UPDATE sys_menu SET sort_order = 12 WHERE id = 200 AND name = '报表中心';

-- 13. 数据交接 - 特殊功能，低频使用
UPDATE sys_menu SET sort_order = 13 WHERE id = 207 AND name = '数据交接';

-- 14. 系统管理 - 系统配置，放最后（仅管理员使用）
UPDATE sys_menu SET sort_order = 99 WHERE id = 2 AND name = '系统管理';


-- ==================== 二级菜单排序调整 ====================

-- 系统管理子菜单（id=2）顺序调整
UPDATE sys_menu SET sort_order = 1 WHERE parent_id = 2 AND name = '用户管理';
UPDATE sys_menu SET sort_order = 2 WHERE parent_id = 2 AND name = '角色管理';
UPDATE sys_menu SET sort_order = 3 WHERE parent_id = 2 AND name = '部门管理';
UPDATE sys_menu SET sort_order = 4 WHERE parent_id = 2 AND name = '菜单管理';
UPDATE sys_menu SET sort_order = 5 WHERE parent_id = 2 AND name = '权限矩阵';
UPDATE sys_menu SET sort_order = 6 WHERE parent_id = 2 AND name = '系统配置';
UPDATE sys_menu SET sort_order = 7 WHERE parent_id = 2 AND name = '操作日志';
UPDATE sys_menu SET sort_order = 10 WHERE parent_id = 2 AND name = '出函模板';
UPDATE sys_menu SET sort_order = 11 WHERE parent_id = 2 AND name = '合同模板';
UPDATE sys_menu SET sort_order = 12 WHERE parent_id = 2 AND name = '提成规则配置';
UPDATE sys_menu SET sort_order = 99 WHERE parent_id = 2 AND name = '外部系统集成';

-- 项目管理子菜单（id=4）顺序调整
UPDATE sys_menu SET sort_order = 1 WHERE parent_id = 4 AND name = '项目列表';
UPDATE sys_menu SET sort_order = 2 WHERE parent_id = 4 AND name = '我的项目';
UPDATE sys_menu SET sort_order = 3 WHERE parent_id = 4 AND name = '合同管理';
UPDATE sys_menu SET sort_order = 4 WHERE parent_id = 4 AND name = '我的合同';
UPDATE sys_menu SET sort_order = 5 WHERE parent_id = 4 AND name = '任务管理';
UPDATE sys_menu SET sort_order = 6 WHERE parent_id = 4 AND name = '工时管理';

-- 财务管理子菜单（id=5）顺序调整 - 按照个人->管理的顺序
UPDATE sys_menu SET sort_order = 1 WHERE parent_id = 5 AND name = '我的收款';
UPDATE sys_menu SET sort_order = 2 WHERE parent_id = 5 AND name = '我的提成';
UPDATE sys_menu SET sort_order = 3 WHERE parent_id = 5 AND name = '我的工资';
UPDATE sys_menu SET sort_order = 4 WHERE parent_id = 5 AND name = '费用报销';
UPDATE sys_menu SET sort_order = 10 WHERE parent_id = 5 AND name = '合同收款概览';
UPDATE sys_menu SET sort_order = 11 WHERE parent_id = 5 AND name = '收款管理';
UPDATE sys_menu SET sort_order = 12 WHERE parent_id = 5 AND name = '收款变更审批';
UPDATE sys_menu SET sort_order = 13 WHERE parent_id = 5 AND name = '提成管理';
UPDATE sys_menu SET sort_order = 14 WHERE parent_id = 5 AND name = '工资管理';
UPDATE sys_menu SET sort_order = 15 WHERE parent_id = 5 AND name = '发票管理';
UPDATE sys_menu SET sort_order = 16 WHERE parent_id = 5 AND name = '合同变更处理';
UPDATE sys_menu SET sort_order = 20 WHERE parent_id = 5 AND name = '财务报表';

-- 卷宗管理子菜单（id=6）顺序调整
UPDATE sys_menu SET sort_order = 1 WHERE parent_id = 6 AND name = '我的文书';
UPDATE sys_menu SET sort_order = 2 WHERE parent_id = 6 AND name = '卷宗列表';
UPDATE sys_menu SET sort_order = 3 WHERE parent_id = 6 AND name = '文书制作';
UPDATE sys_menu SET sort_order = 4 WHERE parent_id = 6 AND name = '模板管理';

-- 行政管理子菜单（id=9）顺序调整 - 按使用频率
UPDATE sys_menu SET sort_order = 1 WHERE parent_id = 9 AND name = '考勤管理';
UPDATE sys_menu SET sort_order = 2 WHERE parent_id = 9 AND name = '请假管理';
UPDATE sys_menu SET sort_order = 3 WHERE parent_id = 9 AND name = '加班管理';
UPDATE sys_menu SET sort_order = 4 WHERE parent_id = 9 AND name = '外出管理';
UPDATE sys_menu SET sort_order = 5 WHERE parent_id = 9 AND name = '会议室预约';
UPDATE sys_menu SET sort_order = 6 WHERE parent_id = 9 AND name = '会议记录';
UPDATE sys_menu SET sort_order = 10 WHERE parent_id = 9 AND name = '出函管理';
UPDATE sys_menu SET sort_order = 11 WHERE parent_id = 9 AND name = '印章管理';
UPDATE sys_menu SET sort_order = 12 WHERE parent_id = 9 AND name = '用印申请';
UPDATE sys_menu SET sort_order = 20 WHERE parent_id = 9 AND name = '资产管理';
UPDATE sys_menu SET sort_order = 21 WHERE parent_id = 9 AND name = '资产盘点';
UPDATE sys_menu SET sort_order = 22 WHERE parent_id = 9 AND name = '采购管理';
UPDATE sys_menu SET sort_order = 23 WHERE parent_id = 9 AND name = '供应商管理';
UPDATE sys_menu SET sort_order = 30 WHERE parent_id = 9 AND name = '合同查询';

-- 人力资源子菜单（id=10）顺序调整
UPDATE sys_menu SET sort_order = 1 WHERE parent_id = 10 AND name = '员工档案';
UPDATE sys_menu SET sort_order = 2 WHERE parent_id = 10 AND name = '培训管理';
UPDATE sys_menu SET sort_order = 3 WHERE parent_id = 10 AND name = '绩效考核';
UPDATE sys_menu SET sort_order = 4 WHERE parent_id = 10 AND name = '转正管理';
UPDATE sys_menu SET sort_order = 5 WHERE parent_id = 10 AND name = '晋升管理';
UPDATE sys_menu SET sort_order = 6 WHERE parent_id = 10 AND name = '发展计划';
UPDATE sys_menu SET sort_order = 7 WHERE parent_id = 10 AND name = '离职管理';

COMMIT;

-- 验证调整结果
SELECT id, parent_id, name, path, sort_order 
FROM sys_menu 
WHERE parent_id = 0 
ORDER BY sort_order, id;

