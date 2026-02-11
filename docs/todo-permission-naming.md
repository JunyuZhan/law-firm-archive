# 权限码命名规范统一任务

> 创建时间：2026-02-11
> 状态：✅ **已完成**

## 目标

统一项目中权限码的命名规范，解决以下问题：
1. `sys:` vs `system:` 前缀混用
2. `edit` vs `update` 动词混用

## 命名规范

```
格式: {模块}:{资源}:{操作}

模块前缀（统一使用）:
- sys:      系统管理（用户、角色、菜单、部门、配置、字典、日志）
- client:   客户管理
- matter:   案件管理
- finance:  财务管理
- doc:      文档管理
- admin:    行政管理
- hr:       人力资源
- knowledge: 知识库
- archive:  档案管理
- approval: 审批
- report:   报表

操作动词（统一使用）:
- list      查看列表
- view      查看详情
- create    新建
- update    编辑/更新（不用 edit）
- delete    删除
- export    导出
- import    导入
- approve   审批
- manage    管理
```

## 任务清单

### 阶段1：分析现状
- [x] 1.1 统计后端使用 `system:` 前缀的权限码
- [x] 1.2 统计后端使用 `:edit` 动词的权限码
- [x] 1.3 统计 sys_menu 中需要修改的记录

### 阶段2：修改后端 Controller
- [x] 2.1 修改使用 `system:` 前缀的 Controller
- [x] 2.2 修改使用 `:edit` 动词的 Controller

### 阶段3：修改数据库初始化脚本
- [x] 3.1 修改 20-init-data.sql 中的权限码
- [x] 3.2 修改其他 SQL 脚本中的权限码

### 阶段4：修改前端
- [x] 4.1 修改前端使用的权限码（如有遗漏）

### 阶段5：验证
- [x] 5.1 重新初始化数据库
- [x] 5.2 启动后端验证
- [x] 5.3 启动前端验证

## 修改记录

### A. `system:` → `sys:` (6个文件)

| 原权限码 | 新权限码 | 文件 |
|---------|---------|------|
| `system:backup:*` | `sys:backup:*` | BackupController.java |
| `system:log:*` | `sys:log:*` | OperationLogController.java |
| `system:integration:*` | `sys:integration:*` | ExternalIntegrationController.java |
| `system:loginlog:*` | `sys:loginlog:*` | LoginLogController.java |
| `system:config:manage` | `sys:config:manage` | HolidayController.java |
| `system:cause:*` | `sys:cause:*` | CauseOfActionController.java |

### B. `:edit` → `:update` (20+个文件)

| 原权限码 | 新权限码 | 文件 |
|---------|---------|------|
| `sys:role:edit` | `sys:role:update` | RoleController.java |
| `sys:dept:edit` | `sys:dept:update` | DepartmentController.java |
| `sys:dict:edit` | `sys:dict:update` | DictController.java |
| `sys:announcement:edit` | `sys:announcement:update` | AnnouncementController.java |
| `system:integration:edit` | `sys:integration:update` | ExternalIntegrationController.java |
| `matter:contract:edit` | `matter:contract:update` | ContractController.java (matter) |
| `doc:edit` | `doc:update` | DocumentController.java, MatterDossierController.java |
| `evidence:edit` | `evidence:update` | EvidenceController.java, EvidenceListController.java |
| `deadline:edit` | `deadline:update` | DeadlineController.java |
| `report:template:edit` | `report:template:update` | ReportTemplateController.java |
| `report:scheduled:edit` | `report:scheduled:update` | ScheduledReportController.java |
| `hr:promotion:edit` | `hr:promotion:update` | PromotionController.java |
| `hr:performance:edit` | `hr:performance:update` | PerformanceController.java |
| `hr:development:edit` | `hr:development:update` | DevelopmentPlanController.java |
| `payroll:edit` | `payroll:update` | PayrollController.java |
| `admin:supplier:edit` | `admin:supplier:update` | SupplierController.java |
| `admin:purchase:edit` | `admin:purchase:update` | PurchaseController.java |
| `admin:asset:edit` | `admin:asset:update` | AssetController.java |
| `knowledge:quality:edit` | `knowledge:quality:update` | QualityCheckStandardController.java 等 |
| `knowledge:case:edit` | `knowledge:case:update` | CaseLibraryController.java |
| `knowledge:law:edit` | `knowledge:law:update` | LawRegulationController.java |
