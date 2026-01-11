# 律师事务所管理系统 - 角色权限分析报告

> **文档版本**: 1.0.0  
> **生成日期**: 2026-01-11  
> **数据来源**: `scripts/init-db/20-system-init-data.sql`

---

## 一、系统角色概览

系统共定义了 **7个角色**，采用 RBAC（基于角色的访问控制）模型：

| 角色ID | 角色编码 | 角色名称 | 数据范围 | 描述 |
|:------:|----------|----------|----------|------|
| 1 | `ADMIN` | 管理员 | ALL | 系统最高权限，可管理所有功能和数据 |
| 2 | `DIRECTOR` | 律所主任 | ALL | 律所管理层，可查看全所数据，审批重要事项 |
| 3 | `TEAM_LEADER` | 团队负责人 | DEPT_AND_CHILD | 团队负责人，可查看本团队数据，负责团队业务管理 |
| 5 | `FINANCE` | 财务 | ALL | 财务人员，管理律所财务工作 |
| 6 | `LAWYER` | 律师 | SELF | 执业律师，处理案件的主要人员 |
| 8 | `ADMIN_STAFF` | 行政 | ALL | 行政后勤人员，管理行政事务 |
| 9 | `TRAINEE` | 实习律师 | SELF | 实习人员，有限权限 |

### 数据范围说明

| 数据范围 | 说明 |
|----------|------|
| `ALL` | 可查看全部数据 |
| `DEPT_AND_CHILD` | 可查看本部门及子部门数据 |
| `DEPT` | 仅可查看本部门数据 |
| `SELF` | 仅可查看自己的数据 |

---

## 二、完整权限列表

### 2.1 系统管理权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `sys:user:list` | 用户管理 | /system/user | MENU |
| `sys:role:list` | 角色管理 | /system/role | MENU |
| `sys:dept:list` | 部门管理 | /system/dept | MENU |
| `sys:menu:list` | 菜单管理 | /system/menu | MENU |
| `sys:dict:list` | 字典管理 | /system/dict | MENU |
| `sys:config:list` | 系统配置 | /system/config | MENU |
| `sys:log:list` | 操作日志 | /system/log | MENU |
| `system:backup:list` | 数据库备份 | /system/backup | MENU |
| `sys:letter-template:list` | 出函模板 | /system/letter-template | MENU |
| `sys:contract-template:list` | 合同模板 | /system/contract-template | MENU |
| `system:integration:list` | 外部系统集成 | /system/integration | MENU |
| `sys:handover:list` | 查询交接 | /data-handover | BUTTON |
| `sys:handover:create` | 创建交接 | /data-handover | BUTTON |
| `sys:handover:view` | 查看交接 | /data-handover | BUTTON |
| `sys:handover:confirm` | 确认交接 | /data-handover | BUTTON |
| `sys:handover:cancel` | 取消交接 | /data-handover | BUTTON |

### 2.2 客户管理权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `client:list` | 客户列表 | /crm/client | MENU |
| `client:create` | 创建客户 | - | BUTTON |
| `client:update` | 编辑客户 | - | BUTTON |
| `client:delete` | 删除客户 | - | BUTTON |
| `conflict:list` | 利冲查询 | - | BUTTON |
| `conflict:apply` | 利冲审查 | /crm/conflict | MENU |
| `lead:list` | 案源管理 | /crm/lead | MENU |

### 2.3 项目管理权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `matter:list` | 项目列表 | /matter/list | MENU |
| `matter:view` | 项目查看 | - | BUTTON |
| `matter:create` | 项目创建 | - | BUTTON |
| `matter:update` | 项目编辑 | - | BUTTON |
| `matter:close` | 申请结案 | - | BUTTON |
| `matter:approve` | 审批结案 | - | BUTTON |
| `matter:contract:list` | 合同管理 | /matter/contract | MENU |
| `matter:contract:view` | 我的合同 | /matter/my-contract | MENU |
| `contract:approve` | 合同审批 | - | BUTTON |
| `task:list` | 任务列表 | - | BUTTON |
| `task:view` | 任务查看 | - | BUTTON |
| `task:manage` | 任务管理 | /matter/task | MENU |
| `timesheet:list` | 工时列表 | - | BUTTON |
| `timesheet:view` | 工时查看 | - | BUTTON |
| `timesheet:record` | 工时管理 | /matter/timesheet | MENU |
| `timesheet:approve` | 工时审批 | - | BUTTON |

### 2.4 财务管理权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `finance:my:payment` | 我的收款 | /finance/my-payment | MENU |
| `finance:my:commission` | 我的提成 | /finance/my-commission | MENU |
| `finance:expense:apply` | 费用报销 | /finance/expense | MENU |
| `finance:contract:view` | 合同收款概览 | /finance/contract | MENU |
| `finance:payment:manage` | 收款管理 | /finance/payment | MENU |
| `fee:amendment:list` | 收款变更审批 | /finance/payment-amendment | MENU |
| `finance:commission:view` | 查看提成 | - | BUTTON |
| `finance:commission:manage` | 提成管理 | /finance/commission | MENU |
| `finance:commission:approve` | 提成审批 | - | BUTTON |
| `finance:commission:issue` | 提成发放 | - | BUTTON |
| `finance:invoice:manage` | 发票管理 | /finance/invoice | MENU |
| `finance:contract:amendment:view` | 合同变更处理 | /finance/contract-amendment | MENU |
| `finance:contract:amendment:sync` | 同步变更 | - | BUTTON |
| `finance:contract:amendment:ignore` | 忽略变更 | - | BUTTON |
| `finance:report:view` | 财务报表 | /finance/report | MENU |
| `payroll:list` | 工资管理 | /hr/payroll | MENU |
| `payroll:create` | 创建工资表 | - | BUTTON |
| `payroll:view` | 查看工资表 | - | BUTTON |
| `payroll:edit` | 编辑工资表 | - | BUTTON |
| `payroll:submit` | 提交工资表 | - | BUTTON |
| `payroll:finance:confirm` | 财务确认 | - | BUTTON |
| `payroll:issue` | 发放工资 | - | BUTTON |
| `payroll:my:view` | 我的工资 | /hr/payroll/my | MENU |
| `payroll:confirm` | 确认工资 | - | BUTTON |

### 2.5 卷宗管理权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `doc:my:list` | 我的文书 | /document/my | MENU |
| `doc:list` | 卷宗列表 | /document/list | MENU |
| `doc:compose` | 文书制作 | /document/compose | MENU |
| `doc:template:manage` | 模板管理 | /document/template | MENU |
| `doc:template:detail` | 查看详情 | - | BUTTON |
| `doc:template:use` | 使用模板 | - | BUTTON |
| `doc:template:generate` | 生成文档 | - | BUTTON |
| `doc:template:view` | 预览模板 | - | BUTTON |
| `doc:seal:list` | 印章管理 | /document/seal | MENU |
| `doc:seal:apply` | 用印申请 | /document/seal-apply | MENU |

### 2.6 档案管理权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `archive:list` | 档案列表 | /archive/list | MENU |
| `archive:borrow` | 档案借阅 | /archive/borrow | MENU |
| `archive:migrate:apply` | 档案迁移 | /archive/destroy | MENU |
| `archive:store:approve` | 入库审批 | - | BUTTON |
| `archive:migrate:approve` | 迁移审批 | - | BUTTON |

### 2.7 行政管理权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `admin:attendance:list` | 考勤管理 | /admin/attendance | MENU |
| `admin:attendance:record` | 考勤记录 | - | BUTTON |
| `admin:leave:list` | 请假管理 | /admin/leave | MENU |
| `admin:leave:approve` | 请假审批 | - | BUTTON |
| `admin:leave:manage` | 假期管理 | - | BUTTON |
| `admin:overtime:list` | 加班管理 | /admin/overtime | MENU |
| `admin:overtime:view` | 加班查看 | - | BUTTON |
| `admin:overtime:apply` | 加班申请 | - | BUTTON |
| `admin:overtime:approve` | 加班审批 | - | BUTTON |
| `admin:goout:list` | 外出管理 | /admin/go-out | MENU |
| `admin:goout:view` | 外出查看 | - | BUTTON |
| `admin:goout:register` | 外出登记 | - | BUTTON |
| `admin:meeting:list` | 会议室预约 | /admin/meeting-room | MENU |
| `admin:meeting:manage` | 会议室管理 | - | BUTTON |
| `admin:meeting-record:list` | 会议记录 | /admin/meeting-record | MENU |
| `admin:meeting:view` | 会议记录查看 | - | BUTTON |
| `admin:meeting:record` | 会议记录管理 | - | BUTTON |
| `admin:meeting:notice` | 会议通知 | - | BUTTON |
| `admin:letter:list` | 出函管理 | /admin/letter | MENU |
| `admin:letter:approve` | 出函审批 | - | BUTTON |
| `admin:letter:print` | 出函打印 | - | BUTTON |
| `admin:asset:list` | 资产管理 | /admin/asset | MENU |
| `admin:asset:detail` | 资产详情 | - | BUTTON |
| `admin:asset:create` | 资产创建 | - | BUTTON |
| `admin:asset:edit` | 资产编辑 | - | BUTTON |
| `admin:asset:delete` | 资产删除 | - | BUTTON |
| `admin:asset:receive` | 资产领用 | - | BUTTON |
| `admin:asset:return` | 资产归还 | - | BUTTON |
| `admin:asset:scrap` | 资产报废 | - | BUTTON |
| `admin:asset-inventory:list` | 资产盘点 | /admin/asset-inventory | MENU |
| `admin:asset:inventory` | 资产盘点操作 | - | BUTTON |
| `admin:purchase:list` | 采购管理 | /admin/purchase | MENU |
| `admin:purchase:detail` | 采购详情 | - | BUTTON |
| `admin:purchase:create` | 采购创建 | - | BUTTON |
| `admin:purchase:edit` | 采购编辑 | - | BUTTON |
| `admin:purchase:approve` | 采购审批 | - | BUTTON |
| `admin:purchase:receive` | 采购入库 | - | BUTTON |
| `admin:supplier:list` | 供应商管理 | /admin/supplier | MENU |
| `admin:supplier:detail` | 供应商详情 | - | BUTTON |
| `admin:supplier:create` | 供应商创建 | - | BUTTON |
| `admin:supplier:edit` | 供应商编辑 | - | BUTTON |
| `admin:supplier:delete` | 供应商删除 | - | BUTTON |
| `admin:contract:list` | 合同查询 | /admin/contract | MENU |
| `admin:contract:export` | 合同导出 | - | BUTTON |

### 2.8 人力资源权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `hr:employee:list` | 员工档案 | /hr/employee | MENU |
| `hr:training:list` | 培训管理 | /hr/training | MENU |
| `hr:training:create` | 发布培训通知 | - | BUTTON |
| `hr:training:delete` | 删除培训通知 | - | BUTTON |
| `hr:performance:list` | 绩效考核 | /hr/performance | MENU |
| `hr:regularization:list` | 转正管理 | /hr/regularization | MENU |
| `hr:promotion:list` | 晋升管理 | /hr/promotion | MENU |
| `hr:promotion:view` | 职级查看 | - | BUTTON |
| `hr:promotion:create` | 职级创建 | - | BUTTON |
| `hr:promotion:edit` | 职级编辑 | - | BUTTON |
| `hr:promotion:delete` | 职级删除 | - | BUTTON |
| `hr:promotion:approve` | 晋升审批 | - | BUTTON |
| `hr:development:list` | 发展计划 | /hr/development | MENU |
| `hr:resignation:list` | 离职管理 | /hr/resignation | MENU |

### 2.9 知识库权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `knowledge:article:list` | 知识文章 | /knowledge/article | MENU |
| `knowledge:case:list` | 案例库 | /knowledge/case | MENU |
| `knowledge:law:list` | 法规库 | /knowledge/law | MENU |

### 2.10 通用权限

| 权限标识 | 权限名称 | 菜单路径 | 类型 |
|----------|----------|----------|------|
| `approval:list` | 审批列表 | - | BUTTON |
| `approval:approve` | 审批操作 | - | BUTTON |
| `schedule:list` | 日程管理 | /workbench/schedule | MENU |
| `schedule:view` | 日程查看 | - | BUTTON |
| `schedule:manage` | 日程管理 | - | BUTTON |
| `deadline:list` | 期限列表 | - | BUTTON |
| `deadline:view` | 期限查看 | - | BUTTON |
| `deadline:create` | 期限创建 | - | BUTTON |
| `deadline:edit` | 期限编辑 | - | BUTTON |
| `deadline:delete` | 期限删除 | - | BUTTON |
| `report:list` | 报表中心 | /workbench/report | MENU |
| `report:detail` | 报表详情 | - | BUTTON |
| `report:generate` | 生成报表 | - | BUTTON |
| `report:download` | 下载报表 | - | BUTTON |
| `report:delete` | 删除报表 | - | BUTTON |

---

## 三、各角色权限矩阵

### 3.1 管理员 (ADMIN)

**数据范围**: ALL - 全部数据

**拥有权限**: 
- ✅ 全部系统管理权限（用户、角色、菜单、部门、字典、配置、日志、备份、外部集成）
- ✅ 全部客户管理权限
- ✅ 全部项目管理权限
- ✅ 全部财务管理权限
- ✅ 全部卷宗管理权限
- ✅ 全部档案管理权限
- ✅ 全部行政管理权限
- ✅ 全部人力资源权限
- ✅ 全部知识库权限
- ✅ 全部通用权限

### 3.2 律所主任 (DIRECTOR)

**数据范围**: ALL - 全部数据

**拥有权限**:
- ✅ 部门管理、字典管理、出函模板、合同模板、提成规则配置
- ✅ 全部客户管理权限
- ✅ 全部项目管理权限
- ✅ 全部财务管理权限（包含审批权限）
- ✅ 卷宗管理（查看、制作）
- ✅ 档案管理（查看、借阅、迁移审批）
- ✅ 全部行政管理权限（包含审批权限）
- ✅ 全部人力资源权限
- ✅ 知识库查看
- ✅ 审批中心、日程管理、报表中心
- ✅ 数据交接管理

**不具有权限**:
- ❌ 用户管理
- ❌ 角色管理
- ❌ 菜单管理
- ❌ 操作日志
- ❌ 数据库备份
- ❌ 外部系统集成

### 3.3 团队负责人 (TEAM_LEADER)

**数据范围**: DEPT_AND_CHILD - 本部门及子部门数据

**拥有权限**:
- ✅ 客户管理（列表、创建、编辑、删除、利冲、案源）
- ✅ 项目管理（列表、查看、创建、编辑、结案申请、结案审批）
- ✅ 合同管理（查看、审批）
- ✅ 任务管理、工时管理（含审批）
- ✅ 财务查看（我的收款、我的提成、费用报销、合同收款、收款管理、提成查看）
- ✅ 卷宗管理（列表、制作）
- ✅ 档案管理（列表、借阅）
- ✅ 行政申请（考勤、请假审批、加班申请、外出、会议室预约、出函审批）
- ✅ 知识库查看
- ✅ 日程管理、报表查看
- ✅ 数据交接管理

**不具有权限**:
- ❌ 系统管理
- ❌ 财务审批和发放权限
- ❌ 采购管理、供应商管理、资产管理
- ❌ 人力资源管理

### 3.4 财务 (FINANCE)

**数据范围**: ALL - 全部数据

**拥有权限**:
- ✅ 合同模板、提成规则配置
- ✅ 全部财务管理权限（收款、提成、发票、工资等）
- ✅ 审批相关权限
- ✅ 档案管理（列表、借阅）
- ✅ 行政申请（考勤、请假、会议室、外出、资产领用/归还）
- ✅ 采购管理（编辑、审批、入库）
- ✅ 供应商管理
- ✅ 知识库查看
- ✅ 日程管理、报表中心
- ✅ 数据交接管理

**不具有权限**:
- ❌ 系统管理（除合同模板、提成规则外）
- ❌ 客户管理
- ❌ 项目管理
- ❌ 卷宗管理
- ❌ 人力资源管理

### 3.5 律师 (LAWYER)

**数据范围**: SELF - 仅自己的数据

**拥有权限**:
- ✅ 客户管理（列表、创建、编辑、删除、利冲、案源）
- ✅ 项目管理（列表、我的项目、查看、创建、编辑、结案申请）
- ✅ 合同管理（查看）
- ✅ 任务管理、工时记录
- ✅ 财务（我的收款、我的提成、费用报销）
- ✅ 卷宗管理（我的文书、列表、制作）
- ✅ 用印申请
- ✅ 档案管理（列表、借阅）
- ✅ 行政申请（考勤、请假、加班申请、外出登记、会议室预约）
- ✅ 知识库查看
- ✅ 审批中心、日程管理、报表查看
- ✅ 数据交接（查询、创建、查看、取消）

**不具有权限**:
- ❌ 系统管理
- ❌ 财务管理和审批
- ❌ 档案迁移审批
- ❌ 资产管理、采购管理、供应商管理
- ❌ 人力资源管理
- ❌ 审批结案权限

### 3.6 行政 (ADMIN_STAFF)

**数据范围**: ALL - 全部数据

**拥有权限**:
- ✅ 系统管理（出函模板、合同模板）
- ✅ 卷宗管理（列表、制作、模板管理）
- ✅ 档案管理（列表、借阅）
- ✅ 全部行政管理权限（考勤、请假、加班、外出、会议室、会议记录、出函、资产、采购、供应商）
- ✅ 全部人力资源权限（员工档案、培训、绩效、转正、晋升、发展、离职）
- ✅ 知识库查看
- ✅ 审批中心、日程管理
- ✅ 数据交接管理
- ✅ 我的工资、费用报销

**不具有权限**:
- ❌ 用户、角色、菜单、部门管理
- ❌ 客户管理
- ❌ 项目管理
- ❌ 财务管理（除我的工资、费用报销外）
- ❌ 报表中心

### 3.7 实习律师 (TRAINEE)

**数据范围**: SELF - 仅自己的数据

**拥有权限**:
- ✅ 客户管理（列表、利冲查询）
- ✅ 项目管理（我的项目、查看）
- ✅ 合同管理（查看）
- ✅ 任务管理、工时记录
- ✅ 财务（我的收款、我的提成、费用报销）
- ✅ 卷宗管理（我的文书、列表、制作）
- ✅ 用印申请
- ✅ 档案管理（列表、借阅）
- ✅ 行政申请（考勤、请假、加班申请、外出登记、会议室预约）
- ✅ 知识库查看
- ✅ 审批中心、日程管理、报表查看
- ✅ 数据交接（查询、创建、查看、取消）

**不具有权限**:
- ❌ 系统管理
- ❌ 客户创建、编辑、删除
- ❌ 项目创建、编辑
- ❌ 案源管理
- ❌ 财务管理和审批
- ❌ 资产管理、采购管理、供应商管理
- ❌ 人力资源管理
- ❌ 任何审批权限

---

## 四、权限配置评估

### 4.1 ✅ 设计合理之处

1. **数据范围分层合理**
   - 管理层（管理员、主任、财务、行政）：ALL
   - 中层管理（团队负责人）：DEPT_AND_CHILD
   - 基层人员（律师、实习律师）：SELF
   
2. **职能分离明确**
   - 财务角色专注财务模块
   - 行政角色专注行政和人力资源模块
   - 业务人员专注业务模块

3. **审批权限层级清晰**
   - 主任：最高级审批权限
   - 团队负责人：团队级审批
   - 财务：财务相关审批
   - 行政：行政相关审批

4. **实习律师权限限制得当**
   - 只有查看和基础操作权限
   - 无创建、编辑、删除和审批权限

### 4.2 ⚠️ 潜在问题与建议

#### 问题 1: 财务角色数据范围过大
**现状**: 财务角色数据范围为 ALL  
**风险**: 可能查看到不必要的业务敏感数据  
**建议**: 考虑将财务角色的数据范围限制在财务相关模块，或创建更细粒度的数据权限控制

#### 问题 2: 行政角色数据范围过大
**现状**: 行政角色数据范围为 ALL  
**风险**: 行政人员可能查看到敏感的案件信息  
**建议**: 行政人员主要处理内部事务，可考虑限制其对案件相关模块的数据访问

#### 问题 3: 缺少高级律师/合伙人角色
**现状**: 只有普通律师角色  
**建议**: 可考虑增加以下角色：
- `SENIOR_LAWYER` - 高级律师（可审核初级律师工作）
- `PARTNER` - 合伙人（可审批重大事项）

#### 问题 4: 律师无法查看团队成员工时
**现状**: 律师数据范围为 SELF  
**建议**: 如果需要项目协作，可考虑增加"项目成员可查看同项目工时"的细粒度权限

#### 问题 5: 缺少只读审计角色
**现状**: 无专门的审计或监督角色  
**建议**: 可增加 `AUDITOR` 角色，具有全部数据的只读权限，用于合规检查

### 4.3 安全性评估

| 评估项 | 状态 | 说明 |
|--------|------|------|
| 最小权限原则 | ⚠️ 部分符合 | 财务和行政的数据范围可进一步限制 |
| 职责分离 | ✅ 符合 | 各角色职能边界清晰 |
| 审批分级 | ✅ 符合 | 审批权限按层级分配 |
| 敏感操作保护 | ✅ 符合 | 删除、审批等操作有权限控制 |
| 数据隔离 | ⚠️ 部分符合 | 基层人员数据隔离良好，管理层可考虑细化 |

---

## 五、默认用户与角色对应

| 用户名 | 真实姓名 | 角色 | 部门 | 职位 |
|--------|----------|------|------|------|
| admin | 系统管理员 | 管理员 | 行政部 | 系统管理员 |
| director | 律所主任 | 律所主任 | 诉讼部 | 律所主任 |
| lawyer1 | 张律师 | 律师 | 诉讼部 | 高级律师 |
| leader | 李团长 | 团队负责人 | 第一组 | 团队负责人 |
| finance | 王财务 | 财务 | 财务部 | 财务主管 |
| staff | 赵行政 | 行政 | 行政部 | 行政专员 |
| trainee | 陈实习 | 实习律师 | 诉讼部 | 实习律师 |

> **默认密码**: `admin123`

---

## 六、权限问题修复记录（2026-01-11）

### 发现的问题

律师创建合同时，页面会调用以下API但因权限不足而返回403错误：

| 问题API | 所需权限 | 问题原因 |
|---------|---------|---------|
| `/api/finance/contract/list` | `finance:contract:view` | 前端错误调用了财务模块的API |
| `/api/system/department/tree` | `sys:dept:list` | 应该使用公共接口 |
| `/api/finance/commission/rules` | `finance:commission:rule:list` | 应该使用公共接口 |

### 修复措施

1. **合同列表API修复**
   - 前端改用 `/matter/contract/list`（需要 `matter:contract:view` 权限，律师已有）
   - 添加 `getMatterContractList` 和 `getMatterContractStatistics` API

2. **部门树API修复**
   - 后端已存在公共接口 `/system/department/tree-public`（无需权限）
   - 前端改用 `getDepartmentTreePublic` 函数

3. **提成规则API修复**
   - 后端新增公共接口 `/finance/commission/rules/active`（无需权限）
   - 前端改用 `commissionRuleApi.getActiveRules`

### 第二批发现的问题（2026-01-11 续）

在工时管理和HR员工档案页面发现类似问题：

| 问题页面 | 问题API | 修复方案 |
|---------|---------|---------|
| 工时管理 `/matter/timesheet` | `getUserList` 需要 `sys:user:list` | 改用 `getUserSelectOptions` |
| HR员工档案 `/hr/employee` | `getUserList` 需要 `sys:user:list` | 改用 `getUserSelectOptions` |

### 修改的文件（汇总）

**后端：**
- `CommissionController.java` - 新增 `/finance/commission/rules/active` 公共接口
- `LetterController.java` - 新增 `/admin/letter/template/active` 公共接口，移除创建申请的权限限制
- `DocumentTemplateController.java` - 新增 `/document/template/active` 公共接口

**前端API：**
- `api/matter/index.ts` - 新增 `getMatterContractList`、`getMyContracts`、`getMatterContractStatistics`
- `api/admin/letter.ts` - 新增 `getActiveTemplatesPublic`
- `api/document/template.ts` - 新增 `getActiveTemplateList`

**前端页面：**
- `views/matter/contract/index.vue` - 改用 matter 模块API和公共接口
- `views/matter/list/index.vue` - 改用 `getDepartmentTreePublic`
- `views/matter/timesheet/index.vue` - 改用 `getUserSelectOptions`
- `views/matter/detail/index.vue` - 改用 `getActiveTemplatesPublic`
- `views/document/compose/components/TemplateMode.vue` - 改用 `getActiveTemplateList`
- `views/hr/employee/index.vue` - 改用 `getDepartmentTreePublic` 和 `getUserSelectOptions`
- `views/admin/asset-inventory/index.vue` - 改用 `getDepartmentTreePublic`

**注意：本次修改不涉及数据库结构或初始数据的变化，无需修改初始化脚本。**

### 第三批发现的问题（2026-01-11 续）

在项目详情页的出函申请功能发现问题：

| 问题页面 | 问题API | 修复方案 |
|---------|---------|---------|
| 项目详情 `/matter/detail` | `getActiveTemplates` 需要 `admin:letter:list` | 改用新增的 `getActiveTemplatesPublic` |

**后端修改：**
- `LetterController.java` - 新增 `/admin/letter/template/active` 公共接口

### 第四批发现的问题（2026-01-11 续）

创建出函申请需要 `admin:letter:list` 权限，但律师需要在项目详情页申请出函：

| 问题页面 | 问题API | 修复方案 |
|---------|---------|---------|
| 项目详情 `/matter/detail` | `createLetterApplication` 需要 `admin:letter:list` | 移除权限限制 |

### 第五批发现的问题（2026-01-11 续）

文书制作页面需要获取模板列表，但需要 `doc:template:list` 权限：

| 问题页面 | 问题API | 修复方案 |
|---------|---------|---------|
| 文书制作 `/document/compose` | `getTemplateList` 需要 `doc:template:list` | 新增公共接口 `/document/template/active` |

**后端修改：**
- `DocumentTemplateController.java` - 新增 `/document/template/active` 公共接口

### 第六批发现的问题（2026-01-11 续）

财务和行政等角色在使用其有权访问的页面时，页面中的下拉选项需要调用客户/项目列表API，但这些角色没有对应的权限：

| 问题页面 | 问题API | 所需权限 | 受影响角色 | 修复方案 |
|---------|---------|---------|-----------|---------|
| 收款管理 `/finance/payment` | `getClientList` | `client:list` | 财务 | 改用 `getClientSelectOptions` |
| 提成管理 `/finance/commission` | `getClientList` + `getMatterList` | `client:list` + `matter:list` | 财务 | 改用公共接口 |
| 发票管理 `/finance/invoice` | `getClientList` | `client:list` | 财务 | 改用 `getClientSelectOptions` |
| 合同收款概览 `/finance/contract` | `getClientList` + `getMatterList` | `client:list` + `matter:list` | 财务 | 改用公共接口 |
| 档案列表 `/archive/list` | `getMatterList` | `matter:list` | 财务、行政 | 改用 `getMatterSelectOptions` |
| 日程管理 `/workbench/schedule` | `getMatterList` | `matter:list` | 财务、行政 | 改用 `getMatterSelectOptions` |
| 卷宗列表 `/document/list` | `getMatterList` | `matter:list` | 行政 | 改用 `getMatterSelectOptions` |

**注意**：项目管理模块（`/matter/*`）只有拥有 `matter:list` 权限的角色才能访问，这些角色同时也有 `client:list` 权限，因此**项目管理模块应使用原API**（`getMatterList`、`getClientList`），而非公共接口。

**后端修改：**
- `ClientController.java` - 新增 `/client/select-options` 公共接口（无需 `client:list` 权限）
- `MatterController.java` - 新增 `/matter/select-options` 公共接口（无需 `matter:list` 权限）

**前端API修改：**
- `api/client/index.ts` - 新增 `getClientSelectOptions()` 函数
- `api/matter/index.ts` - 新增 `getMatterSelectOptions()` 函数

**前端页面修改（使用公共接口 - 财务/行政角色访问）：**
- `views/finance/payment/index.vue` - 改用 `getClientSelectOptions`
- `views/finance/commission/index.vue` - 改用 `getClientSelectOptions` 和 `getMatterSelectOptions`
- `views/finance/invoice/index.vue` - 改用 `getClientSelectOptions`
- `views/finance/contract/index.vue` - 改用 `getClientSelectOptions`、`getMatterSelectOptions`、`getUserSelectOptions`、`getDepartmentTreePublic`
- `views/archive/list/index.vue` - 改用 `getMatterSelectOptions`
- `views/workbench/schedule/index.vue` - 改用 `getMatterSelectOptions`
- `views/document/list/index.vue` - 改用 `getMatterSelectOptions`

**前端页面（保持原API - 律师角色访问）：**
- `views/matter/task/index.vue` - 使用 `getMatterList`（律师有 `matter:list` 权限）
- `views/matter/timesheet/index.vue` - 使用 `getMatterList`（律师有 `matter:list` 权限）
- `views/matter/list/index.vue` - 使用 `getClientList`（律师有 `client:list` 权限）
- `views/matter/contract/index.vue` - 使用 `getClientList`（律师有 `client:list` 权限）

### 已有公共接口清单（完整）

系统中已提供以下公共接口供各模块调用：

| 接口路径 | 用途 | 前端函数 |
|---------|------|---------|
| `/system/department/tree-public` | 获取部门树 | `getDepartmentTreePublic()` |
| `/system/user/select-options` | 获取用户选择列表 | `getUserSelectOptions()` |
| `/system/dict/types` | 获取字典类型 | - |
| `/system/dict/items/code/{code}` | 根据编码获取字典项 | - |
| `/system/config/key/{key}` | 获取配置项 | - |
| `/system/config/version` | 获取系统版本 | - |
| `/client/select-options` | 获取客户选择列表 | `getClientSelectOptions()` |
| `/matter/select-options` | 获取项目选择列表 | `getMatterSelectOptions()` |
| `/finance/commission/rules/active` | 获取激活的提成规则 | `commissionRuleApi.getActiveRules()` |
| `/admin/letter/template/active` | 获取出函模板列表 | `getActiveTemplatesPublic()` |
| `/admin/letter/application` (POST) | 创建出函申请 | `createLetterApplication()` |
| `/admin/letter/application/{id}` | 获取申请详情 | - |
| `/admin/letter/application/my` | 我的申请列表 | - |
| `/admin/letter/application/matter/{id}` | 项目的出函记录 | - |
| `/document/template/active` | 获取启用的文档模板 | `getActiveTemplateList()` |
| `/system/contract-template/active` | 获取启用的合同模板 | - |
| `/system/contract-template/{id}` | 获取合同模板详情 | - |
| `/knowledge/case/*` (查询) | 案例库查询 | - |
| `/client/search` | 利冲客户搜索 | 已单独配置 |

### 关键权限矩阵（决定API选择）

以下矩阵说明各角色是否拥有 `client:list` 和 `matter:list` 权限，用于判断页面应使用原API还是公共接口：

| 角色 | `client:list` | `matter:list` | 页面应使用 |
|------|--------------|---------------|-----------|
| 管理员 (ADMIN) | ✅ | ✅ | 原API |
| 主任 (DIRECTOR) | ✅ | ✅ | 原API |
| 团队负责人 (TEAM_LEADER) | ✅ | ✅ | 原API |
| 律师 (LAWYER) | ✅ | ✅ | 原API |
| 实习律师 (TRAINEE) | ✅ | ✅ | 原API |
| **财务 (FINANCE)** | ❌ | ❌ | **公共接口** |
| **行政 (ADMIN_STAFF)** | ❌ | ❌ | **公共接口** |

**使用规则**：
1. **项目管理模块** (`/matter/*`) - 只有有权限的角色能访问，使用原API
2. **财务模块** (`/finance/*`) - 财务角色使用，需要公共接口
3. **跨角色模块** (`/workbench/*`、`/archive/*`、`/document/*`) - 多种角色访问，使用公共接口更安全

---

## 七、总结

本系统的角色权限设计总体上遵循了 RBAC 模型的最佳实践，实现了：

1. **角色层级化**: 从管理员到实习律师，权限逐级递减
2. **职能模块化**: 财务、行政、业务各有专属模块
3. **数据范围控制**: 通过 data_scope 实现数据隔离
4. **操作粒度细化**: 菜单级 + 按钮级权限控制

**核心设计原则**:
- 财务和行政人员需要访问业务数据以支持其工作职责（这是合理的）
- 律师的数据权限通过 `data_scope=SELF` 限制为仅看自己的数据
- 需要全所数据的场景（如利冲检索）通过特定的公共API或专用权限解决

**改进建议优先级**:
1. ✅ 高优先级: 确保功能页面使用正确的API（已完成，见第六批修复）
2. ✅ 中优先级: 前端按钮权限控制（已完成，见第八节）
3. ~~🟡 中优先级: 考虑增加高级律师、合伙人角色~~ → **不需要**，通过一人多角色实现更灵活
4. 🟢 低优先级: 增加审计角色，细化项目级数据共享权限

**关于角色设计的结论**：
系统支持一人多角色（`sys_user_role` 多对多关联），通过角色组合即可满足各种身份需求：
- 高级律师 = 律师 + 团队负责人
- 合伙人 = 律师 + 部分主任权限
- 无需新增角色，避免角色膨胀

---

## 八、权限系统增强（2026-01-11）

### 8.1 精简公共接口返回字段

为避免公共接口暴露敏感数据，创建了精简的DTO：

**后端新增文件**：
- `ClientSimpleDTO.java` - 只包含 id, clientNo, name, clientType, status
- `MatterSimpleDTO.java` - 包含以下字段：

| 字段 | 说明 |
|------|------|
| `id` | 项目ID |
| `matterNo` | 项目编号 |
| `name` | 项目名称 |
| `matterType/matterTypeName` | 项目类型 |
| `status/statusName` | 项目状态 |
| `clientName` | 客户名称（仅名称，不暴露ID） |
| `contractNo` | 合同编号 |
| `leadLawyerName` | 承办律师名称 |

**修改的接口**：
- `/client/select-options` - 返回 `ClientSimpleDTO`（不含联系方式等敏感信息）
- `/matter/select-options` - 返回 `MatterSimpleDTO`（不含金额、对方信息等敏感数据，但包含合同编号和承办人便于识别）

### 8.2 分类报表权限

将统一的 `report:list` 权限细分为：

| 权限码 | 说明 | 包含接口 | 分配角色 |
|-------|------|---------|---------|
| `report:finance:view` | 财务报表 | 收入统计、律师业绩排行 | 管理员、主任、财务 |
| `report:matter:view` | 业务报表 | 项目统计、客户统计 | 管理员、主任、团队负责人、律师 |

**SQL脚本**: `scripts/init-db/28-report-permission-refine.sql`

**数据库菜单ID**：750（财务报表）、751（业务报表）

### 8.3 细化审批权限

将统一的 `approval:approve` 权限按业务类型细分：

| 权限码 | 说明 | 分配角色 |
|-------|------|---------|
| `approval:contract:approve` | 合同审批 | 管理员、主任、团队负责人 |
| `approval:seal:approve` | 用印审批 | 管理员、主任、行政 |
| `approval:conflict:approve` | 利冲审批 | 管理员、主任、团队负责人 |
| `approval:expense:approve` | 费用审批 | 管理员、主任、财务 |
| `approval:matter-close:approve` | 结案审批 | 管理员、主任、团队负责人 |

**SQL脚本**: `scripts/init-db/29-approval-permission-refine.sql`

**数据库菜单ID**：740-744（合同/用印/利冲/费用/结案审批）

### 8.4 前端按钮权限控制

使用已有的 `v-access:code` 指令控制按钮显示，已应用到以下关键页面：

| 页面 | 权限控制 |
|------|---------|
| 客户管理 (`crm/client`) | 新增按钮 `client:create`、删除按钮 `client:delete`、编辑链接 `client:edit` |
| 项目列表 (`matter/list`) | 新增按钮 `matter:create`、编辑链接 `matter:edit`、归档链接 `archive:create` |
| 收款管理 (`finance/payment`) | 登记收款 `fee:payment` |
| 用户管理 (`system/user`) | 新增 `user:create`、编辑 `user:edit`、删除 `user:delete`、重置密码 `user:reset-password` |

**使用方式**：
```vue
<!-- 单个权限 -->
<Button v-access:code="'client:create'" @click="handleAdd">新增</Button>

<!-- 多个权限（任一即可） -->
<Button v-access:code="['client:create', 'client:edit']" @click="handleAction">操作</Button>
```

### 8.5 执行记录

**SQL脚本执行状态**（2026-01-11）：

```bash
# 报表权限细分
docker exec -i law-firm-postgres psql -U law_admin -d law_firm_dev < scripts/init-db/28-report-permission-refine.sql
# 结果: 新增菜单 750, 751 及角色权限分配

# 审批权限细分  
docker exec -i law-firm-postgres psql -U law_admin -d law_firm_dev < scripts/init-db/29-approval-permission-refine.sql
# 结果: 新增菜单 740-744 及角色权限分配
```

**验证查询**：
```sql
SELECT id, name, permission FROM sys_menu 
WHERE permission LIKE 'report:%' OR permission LIKE 'approval:%' 
ORDER BY id;
```

**生效条件**：
- ✅ 数据库：SQL 已执行，权限记录已写入
- ✅ 后端：服务重启后新代码生效
- ✅ 前端：开发服务器自动热更新
- ⚠️ 用户：需重新登录以获取最新权限列表

---

## 九、改进效果总结

| 改进项 | 安全收益 | 体验改进 |
|--------|---------|---------|
| 精简公共接口返回字段 | 不暴露联系方式、金额等敏感信息 | 接口响应更快，数据传输量减少 |
| 项目接口含合同编号/承办人 | - | 下拉选择时能看到更多上下文 |
| 报表权限细分 | 财务/业务数据分离，普通用户看不到敏感报表 | 权限更精细，各司其职 |
| 审批权限细分 | 不同业务审批分开控制 | 职责边界更清晰 |
| 前端按钮权限控制 | 防止无权限用户尝试操作 | 无权限按钮自动隐藏，不会点击后报错 |

---

*文档最后更新: 2026-01-11*

