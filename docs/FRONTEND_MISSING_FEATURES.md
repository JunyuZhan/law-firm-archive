# 前端缺失功能清单

> 本文档记录后端已实现但前端未完整实现的功能模块
> 
> 更新时间：2026-01-12
> 
> ⚠️ 经过菜单配置验证，部分功能可能是预留功能，非核心需求

## 概述

经过对后端 Controller、前端页面和菜单配置的详细对比分析：

- ✅ **已实现**：8个（预收款管理、系统公告、OpenAPI管理、报表模板、定时报表、质量管理、在线计时器、项目客户服务）
- **预留功能待评估**：1个（卷宗模板）

---

## ✅ 已实现

### 1. 财务 - 预收款管理 ✅ 已完成

| 后端Controller | API路径 | 功能描述 | 前端状态 |
|---|---|---|---|
| `PrepaymentController` | `/finance/prepayment` | 预收款管理 | ✅ 已实现 |

**实现内容：**
- ✅ API文件：`api/finance/prepayment.ts`
- ✅ 页面：`views/finance/prepayment/index.vue`（使用 useVbenVxeGrid）
- ✅ 菜单配置：ID 720-724
- ✅ 权限配置：管理员、律所主任、财务角色

**功能点：**
- ✅ 预收款列表查询
- ✅ 创建预收款
- ✅ 确认预收款
- ✅ 预收款退款
- ✅ 预收款详情（含核销记录）

---

### 2. 系统 - 公告管理 ✅ 已完成

| 后端Controller | API路径 | 功能描述 | 前端状态 |
|---|---|---|---|
| `AnnouncementController` | `/system/announcement` | 系统公告管理 | ✅ 已实现 |

**实现内容：**
- ✅ API文件：`api/system/announcement.ts`
- ✅ 页面：`views/system/announcement/index.vue`（使用 useVbenVxeGrid）
- ✅ 菜单配置：ID 725-729
- ✅ 权限配置：管理员、律所主任、行政角色

**功能点：**
- ✅ 公告列表查询（支持类型、状态筛选）
- ✅ 新建/编辑公告
- ✅ 发布/撤回公告
- ✅ 删除公告
- ✅ 公告详情查看

---

### 3. 系统 - OpenAPI管理 ✅ 已完成

| 后端Controller | API路径 | 功能描述 | 前端状态 |
|---|---|---|---|
| `OpenApiManageController` | `/system/openapi` | 客户门户访问令牌管理 | ✅ 已实现 |

**实现内容：**
- ✅ API文件：`api/system/openapi.ts`（已存在）
- ✅ 页面：`views/system/openapi/index.vue`
- ✅ 菜单配置：ID 735-737
- ✅ 权限配置：管理员、律所主任角色

**功能点：**
- ✅ 令牌列表查询（支持客户、状态筛选）
- ✅ 创建访问令牌
- ✅ 撤销令牌
- ✅ 令牌详情查看
- ✅ 复制门户地址

---

### 4. 工作台 - 报表模板管理 ✅ 已完成

| 后端Controller | API路径 | 功能描述 | 前端状态 |
|---|---|---|---|
| `ReportTemplateController` | `/workbench/report-template` | 自定义报表模板 | ✅ 已实现 |

**实现内容：**
- ✅ API文件：`api/workbench/report-template.ts`（已存在）
- ✅ 页面：`views/workbench/report-template/index.vue`（使用 useVbenVxeGrid）
- ✅ 菜单配置：ID 740-743
- ✅ 权限配置：管理员、律所主任角色

**功能点：**
- ✅ 模板列表查询
- ✅ 新建/编辑模板
- ✅ 启用/停用模板
- ✅ 删除模板
- ✅ 根据模板生成报表

---

### 5. 工作台 - 定时报表管理 ✅ 已完成

| 后端Controller | API路径 | 功能描述 | 前端状态 |
|---|---|---|---|
| `ScheduledReportController` | `/workbench/scheduled-report` | 定时报表任务 | ✅ 已实现 |

**实现内容：**
- ✅ API文件：`api/workbench/scheduled-report.ts`（已存在）
- ✅ 页面：`views/workbench/scheduled-report/index.vue`（使用 useVbenVxeGrid）
- ✅ 菜单配置：ID 745-749
- ✅ 权限配置：管理员、律所主任角色

**功能点：**
- ✅ 任务列表查询
- ✅ 新建/编辑任务
- ✅ 启用/暂停任务
- ✅ 立即执行任务
- ✅ 查看执行记录
- ✅ 删除任务

---

### 6. 知识库 - 质量管理 ✅ 已完成

| 后端Controller | API路径 | 功能描述 | 前端状态 |
|---|---|---|---|
| `QualityCheckController` | `/knowledge/quality-check` | 项目质量检查 | ✅ 已实现 |
| `QualityCheckStandardController` | `/knowledge/quality-standard` | 质量检查标准管理 | ✅ 已实现 |
| `QualityIssueController` | `/knowledge/quality-issue` | 问题整改管理 | ✅ 已实现 |
| `RiskWarningController` | `/knowledge/risk-warning` | 风险预警管理 | ✅ 已实现 |

**实现内容：**
- ✅ API文件：`api/knowledge/quality.ts`
- ✅ 页面：`views/knowledge/quality/index.vue`（多Tab复杂页面）
- ✅ 菜单配置：ID 750-754
- ✅ 权限配置：管理员、律所主任、团队负责人角色

**功能点：**
- ✅ 质量概览（统计卡片）
- ✅ 待处理问题列表
- ✅ 活跃预警列表
- ✅ 检查标准管理（CRUD）
- ✅ 问题详情查看与解决
- ✅ 预警确认、解决、关闭

---

### 7. 案件 - 在线计时器 ✅ 已完成

| 后端Controller | API路径 | 功能描述 | 前端状态 |
|---|---|---|---|
| `TimerController` | `/timer` | 在线计时器工具 | ✅ 已实现 |

**实现内容：**
- ✅ API文件：`api/matter/timer.ts`
- ✅ 组件：`components/timer/FloatingTimer.vue`
- ✅ 集成：`layouts/basic.vue`（全局悬浮组件）

**功能点：**
- ✅ 开始计时（选择项目）
- ✅ 暂停/继续计时
- ✅ 停止计时（自动保存为工时记录）
- ✅ 实时显示计时时长
- ✅ 最小化/展开切换

---

### 8. 案件 - 项目客户服务 ✅ 已完成

| 后端Controller | API路径 | 功能描述 | 前端状态 |
|---|---|---|---|
| `MatterClientServiceController` | `/matter/client-service` | 项目数据推送到客户服务系统 | ✅ 已实现 |

**实现内容：**
- ✅ 组件：`components/ClientServicePanel/index.vue`
- ✅ 集成：`views/matter/detail/index.vue`（项目详情 Tab）
- ✅ API复用：`api/system/openapi.ts`

**功能点：**
- ✅ 授权范围配置（项目信息、进度、律师、期限、任务、文书、费用）
- ✅ 手动推送项目数据到客户门户
- ✅ 文档文件选择推送
- ✅ 推送记录查看
- ✅ 推送统计

---

## ⚪ 预留功能（待评估）

以下功能后端已实现但菜单中未规划，可能是预留功能或内部使用功能：

### 9. 文档 - 卷宗模板管理

| 后端Controller | API路径 | 功能描述 |
|---|---|---|
| `DossierTemplateController` | `/dossier/template` | 卷宗模板配置 |

**评估意见：** 配置类功能，优先级较低。

---

## 📊 汇总统计

| 状态 | 数量 | 说明 |
|---|---|---|
| ✅ 已实现 | 8 | 预收款管理、系统公告、OpenAPI管理、报表模板、定时报表、质量管理、在线计时器、项目客户服务 |
| ⚪ 预留待评估 | 1 | 卷宗模板（后端只读API，暂不实现） |

---

## 更新记录

| 日期 | 更新内容 |
|---|---|
| 2026-01-12 | 初始版本，完成后端Controller与前端页面对比分析 |
| 2026-01-12 | ✅ 完成预收款管理功能实现（API + 页面 + 菜单 + 权限） |
| 2026-01-12 | ✅ 完成系统公告功能实现（API + 页面 + 菜单 + 权限） |
| 2026-01-12 | ✅ 完成OpenAPI管理页面实现（页面 + 菜单 + 权限） |
| 2026-01-12 | ✅ 完成报表模板管理页面实现（页面 + 菜单 + 权限） |
| 2026-01-12 | ✅ 完成定时报表管理页面实现（页面 + 菜单 + 权限） |
| 2026-01-12 | ✅ 完成质量管理模块实现（API + 页面 + 菜单 + 权限） |
| 2026-01-12 | ✅ 完成在线计时器实现（API + 悬浮组件 + 布局集成） |
| 2026-01-12 | 📝 按 frontend-component-guide.md 规范改造页面使用 useVbenVxeGrid |