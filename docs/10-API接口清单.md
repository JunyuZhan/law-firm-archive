# 智慧律所管理系统 - API接口清单

> 版本：1.0 | 更新日期：2026年1月3日
> 
> 本文档汇总所有后端API接口，方便前端开发对接。

---

## 📋 目录

1. [认证授权](#一认证授权)
2. [系统管理](#二系统管理)
3. [客户管理](#三客户管理)
4. [案件管理](#四案件管理)
5. [合同管理](#五合同管理)
6. [财务收费](#六财务收费)
7. [文档管理](#七文档管理)
8. [证据管理](#八证据管理)
9. [档案管理](#九档案管理)
10. [工时管理](#十工时管理)
11. [任务与日程](#十一任务与日程)

---

## 一、认证授权

**基础路径**: `/api/auth`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/auth/login` | 用户登录 | 公开 |
| POST | `/auth/refresh` | 刷新Token | 公开 |
| POST | `/auth/logout` | 用户登出 | 需认证 |
| GET | `/auth/info` | 获取当前用户信息 | 需认证 |

---

## 二、系统管理

**基础路径**: `/api/system`

### 2.1 用户管理

**路径**: `/api/system/user`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/system/user` | 分页查询用户列表 | `sys:user:list` |
| GET | `/system/user/{id}` | 获取用户详情 | `sys:user:list` |
| POST | `/system/user` | 创建用户 | `sys:user:create` |
| PUT | `/system/user` | 更新用户 | `sys:user:update` |
| DELETE | `/system/user/{id}` | 删除用户 | `sys:user:delete` |
| PATCH | `/system/user/{id}/reset-password` | 重置密码 | `sys:user:resetPassword` |
| PATCH | `/system/user/{id}/status/{status}` | 更新用户状态 | `sys:user:updateStatus` |

---

## 三、客户管理

**基础路径**: `/api/client`

### 3.1 客户管理

**路径**: `/api/client`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/client/list` | 分页查询客户列表 | `client:list` |
| GET | `/client/{id}` | 获取客户详情 | `client:list` |
| POST | `/client` | 创建客户 | `client:create` |
| PUT | `/client` | 更新客户 | `client:update` |
| DELETE | `/client/{id}` | 删除客户 | `client:delete` |
| DELETE | `/client/batch` | 批量删除客户 | `client:delete` |
| PUT | `/client/{id}/status` | 修改客户状态 | `client:update` |
| POST | `/client/{id}/convert` | 潜在客户转正式 | `client:update` |

### 3.2 利冲检查

**路径**: `/api/client/conflict-check`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/client/conflict-check/list` | 分页查询利冲检查 | `conflict:list` |
| GET | `/client/conflict-check/{id}` | 获取利冲检查详情 | `conflict:list` |
| POST | `/client/conflict-check` | 创建利冲检查 | `conflict:create` |
| POST | `/client/conflict-check/{id}/approve` | 审核通过（豁免） | `conflict:approve` |
| POST | `/client/conflict-check/{id}/reject` | 审核拒绝 | `conflict:approve` |

---

## 四、案件管理

**基础路径**: `/api/matter`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/matter/list` | 分页查询案件列表 | `matter:list` |
| GET | `/matter/my` | 查询我的案件 | 需认证 |
| GET | `/matter/{id}` | 获取案件详情 | `matter:list` |
| POST | `/matter` | 创建案件 | `matter:create` |
| PUT | `/matter` | 更新案件 | `matter:update` |
| DELETE | `/matter/{id}` | 删除案件 | `matter:delete` |
| PUT | `/matter/{id}/status` | 修改案件状态 | `matter:update` |
| POST | `/matter/{id}/participant` | 添加团队成员 | `matter:update` |
| DELETE | `/matter/{id}/participant/{userId}` | 移除团队成员 | `matter:update` |

---

## 五、合同管理

**基础路径**: `/api/finance/contract`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/finance/contract/list` | 分页查询合同列表 | `contract:list` |
| GET | `/finance/contract/{id}` | 获取合同详情 | `contract:list` |
| POST | `/finance/contract` | 创建合同 | `contract:create` |
| PUT | `/finance/contract` | 更新合同 | `contract:update` |
| DELETE | `/finance/contract/{id}` | 删除合同 | `contract:delete` |
| POST | `/finance/contract/{id}/submit` | 提交审批 | `contract:submit` |
| POST | `/finance/contract/{id}/approve` | 审批通过 | `contract:approve` |
| POST | `/finance/contract/{id}/reject` | 审批拒绝 | `contract:approve` |
| POST | `/finance/contract/{id}/terminate` | 终止合同 | `contract:terminate` |
| POST | `/finance/contract/{id}/complete` | 完成合同 | `contract:complete` |

---

## 六、财务收费

**基础路径**: `/api/finance`

### 6.1 收费管理

**路径**: `/api/finance/fee`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/finance/fee/list` | 分页查询收费记录 | `fee:list` |
| GET | `/finance/fee/{id}` | 获取收费详情 | `fee:list` |
| POST | `/finance/fee` | 创建收费记录 | `fee:create` |
| POST | `/finance/fee/payment` | 创建收款记录 | `fee:payment` |
| POST | `/finance/fee/payment/{id}/confirm` | 确认收款 | `fee:confirm` |
| POST | `/finance/fee/payment/{id}/cancel` | 取消收款 | `fee:payment` |

### 6.2 发票管理

**路径**: `/api/finance/invoice`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/finance/invoice/list` | 分页查询发票 | `invoice:list` |
| GET | `/finance/invoice/{id}` | 获取发票详情 | `invoice:list` |
| POST | `/finance/invoice/apply` | 申请开票 | `invoice:apply` |
| POST | `/finance/invoice/{id}/issue` | 开具发票 | `invoice:issue` |
| POST | `/finance/invoice/{id}/cancel` | 作废发票 | `invoice:cancel` |

---

## 七、文档管理

**基础路径**: `/api/document`

### 7.1 文档管理

**路径**: `/api/document`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/document/list` | 分页查询文档 | `document:list` |
| GET | `/document/{id}` | 获取文档详情 | `document:list` |
| POST | `/document` | 创建文档 | `document:create` |
| PUT | `/document` | 更新文档 | `document:update` |
| DELETE | `/document/{id}` | 删除文档 | `document:delete` |
| POST | `/document/{id}/upload-version` | 上传新版本 | `document:upload` |
| GET | `/document/{id}/versions` | 获取版本列表 | `document:list` |
| GET | `/document/{id}/download/{versionId}` | 下载文档 | `document:download` |

### 7.2 文档分类

**路径**: `/api/document/category`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/document/category/tree` | 获取分类树 | `document:category:list` |
| POST | `/document/category` | 创建分类 | `document:category:create` |
| PUT | `/document/category/{id}` | 更新分类 | `document:category:update` |
| DELETE | `/document/category/{id}` | 删除分类 | `document:category:delete` |

### 7.3 文档模板

**路径**: `/api/document/template`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/document/template/list` | 分页查询模板 | `document:template:list` |
| GET | `/document/template/{id}` | 获取模板详情 | `document:template:list` |
| POST | `/document/template` | 创建模板 | `document:template:create` |
| PUT | `/document/template/{id}` | 更新模板 | `document:template:update` |
| DELETE | `/document/template/{id}` | 删除模板 | `document:template:delete` |
| POST | `/document/template/{id}/generate` | 生成文档 | `document:template:generate` |

### 7.4 印章管理

**路径**: `/api/document/seal`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/document/seal/list` | 分页查询印章 | `seal:list` |
| GET | `/document/seal/{id}` | 获取印章详情 | `seal:list` |
| POST | `/document/seal` | 创建印章 | `seal:create` |
| PUT | `/document/seal/{id}` | 更新印章 | `seal:update` |
| DELETE | `/document/seal/{id}` | 删除印章 | `seal:delete` |
| PUT | `/document/seal/{id}/status` | 更新印章状态 | `seal:update` |

### 7.5 用印申请

**路径**: `/api/document/seal-application`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/document/seal-application/list` | 分页查询申请 | `seal:application:list` |
| GET | `/document/seal-application/{id}` | 获取申请详情 | `seal:application:list` |
| POST | `/document/seal-application` | 创建申请 | `seal:application:create` |
| PUT | `/document/seal-application/{id}` | 更新申请 | `seal:application:update` |
| DELETE | `/document/seal-application/{id}` | 删除申请 | `seal:application:delete` |
| POST | `/document/seal-application/{id}/approve` | 审批通过 | `seal:application:approve` |
| POST | `/document/seal-application/{id}/reject` | 审批拒绝 | `seal:application:approve` |
| POST | `/document/seal-application/{id}/use` | 用印登记 | `seal:application:use` |

---

## 八、证据管理

**基础路径**: `/api/evidence`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/evidence/list` | 分页查询证据 | `evidence:list` |
| GET | `/evidence/{id}` | 获取证据详情 | `evidence:list` |
| POST | `/evidence` | 添加证据 | `evidence:create` |
| PUT | `/evidence` | 更新证据 | `evidence:update` |
| DELETE | `/evidence/{id}` | 删除证据 | `evidence:delete` |
| POST | `/evidence/batch-adjust-group` | 批量调整分组 | `evidence:update` |
| POST | `/evidence/{id}/cross-exam` | 添加质证记录 | `evidence:crossExam` |
| POST | `/evidence/{id}/complete` | 完成质证 | `evidence:crossExam` |
| GET | `/evidence/{id}/cross-exams` | 获取质证记录 | `evidence:list` |
| POST | `/evidence/generate-list` | 生成证据清单 | `evidence:generate` |

---

## 九、档案管理

**基础路径**: `/api/archive`

### 9.1 档案管理

**路径**: `/api/archive`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/archive/list` | 分页查询档案 | `archive:list` |
| GET | `/archive/{id}` | 获取档案详情 | `archive:list` |
| GET | `/archive/pending-matters` | 获取待归档案件 | `archive:create` |
| POST | `/archive` | 创建档案 | `archive:create` |
| POST | `/archive/store` | 档案入库 | `archive:store` |

### 9.2 档案借阅

**路径**: `/api/archive/borrow`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/archive/borrow/list` | 分页查询借阅记录 | `archive:borrow:list` |
| POST | `/archive/borrow` | 创建借阅申请 | `archive:borrow:create` |
| POST | `/archive/borrow/{id}/approve` | 审批通过 | `archive:borrow:approve` |
| POST | `/archive/borrow/{id}/reject` | 审批拒绝 | `archive:borrow:approve` |
| POST | `/archive/borrow/{id}/confirm` | 确认借出 | `archive:borrow:confirm` |
| POST | `/archive/borrow/return` | 归还档案 | `archive:borrow:return` |
| GET | `/archive/borrow/overdue` | 获取逾期借阅 | `archive:borrow:list` |

### 9.3 档案库位

**路径**: `/api/archive/location`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/archive/location/list` | 查询所有库位 | `archive:location:list` |
| GET | `/archive/location/available` | 查询可用库位 | `archive:location:list` |
| GET | `/archive/location/{id}` | 获取库位详情 | `archive:location:list` |
| POST | `/archive/location` | 创建库位 | `archive:location:create` |
| PUT | `/archive/location/{id}` | 更新库位 | `archive:location:update` |

---

## 十、工时管理

**基础路径**: `/api/timesheet`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/timesheet/list` | 分页查询工时记录 | `timesheet:list` |
| GET | `/timesheet/{id}` | 获取工时详情 | `timesheet:list` |
| POST | `/timesheet` | 创建工时记录 | `timesheet:create` |
| PUT | `/timesheet` | 更新工时记录 | `timesheet:update` |
| DELETE | `/timesheet/{id}` | 删除工时记录 | `timesheet:delete` |
| POST | `/timesheet/{id}/submit` | 提交工时 | `timesheet:submit` |
| POST | `/timesheet/{id}/approve` | 审批通过 | `timesheet:approve` |
| POST | `/timesheet/{id}/reject` | 审批拒绝 | `timesheet:approve` |
| GET | `/timesheet/summary` | 工时汇总统计 | `timesheet:summary` |
| GET | `/timesheet/my` | 我的工时记录 | 需认证 |
| GET | `/timesheet/rate/list` | 查询小时费率 | `timesheet:rate:list` |
| POST | `/timesheet/rate` | 设置小时费率 | `timesheet:rate:create` |
| PUT | `/timesheet/rate/{id}` | 更新小时费率 | `timesheet:rate:update` |
| DELETE | `/timesheet/rate/{id}` | 删除小时费率 | `timesheet:rate:delete` |

---

## 十一、任务与日程

**基础路径**: `/api`

### 11.1 任务管理

**路径**: `/api/task`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/task/list` | 分页查询任务 | `task:list` |
| GET | `/task/{id}` | 获取任务详情 | `task:list` |
| POST | `/task` | 创建任务 | `task:create` |
| PUT | `/task` | 更新任务 | `task:update` |
| DELETE | `/task/{id}` | 删除任务 | `task:delete` |
| PUT | `/task/{id}/status` | 更新任务状态 | `task:update` |
| PUT | `/task/{id}/progress` | 更新任务进度 | `task:update` |
| POST | `/task/{id}/assign` | 分配任务 | `task:assign` |
| POST | `/task/{id}/comment` | 添加评论 | `task:comment` |
| GET | `/task/{id}/comments` | 获取任务评论 | `task:list` |
| GET | `/task/my` | 我的任务 | 需认证 |
| GET | `/task/overdue` | 逾期任务 | `task:list` |

### 11.2 日程管理

**路径**: `/api/schedule`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/schedule/list` | 分页查询日程 | `schedule:list` |
| GET | `/schedule/{id}` | 获取日程详情 | `schedule:list` |
| GET | `/schedule/date/{date}` | 按日期查询日程 | `schedule:list` |
| POST | `/schedule` | 创建日程 | `schedule:create` |
| PUT | `/schedule` | 更新日程 | `schedule:update` |
| DELETE | `/schedule/{id}` | 删除日程 | `schedule:delete` |
| POST | `/schedule/{id}/cancel` | 取消日程 | `schedule:update` |
| GET | `/schedule/my` | 我的日程 | 需认证 |

---

## 📊 接口统计

| 模块 | 接口数 | 说明 |
|------|--------|------|
| 认证授权 | 4 | 登录、刷新、登出、用户信息 |
| 系统管理 | 7 | 用户管理 |
| 客户管理 | 12 | 客户CRUD、利冲检查 |
| 案件管理 | 8 | 案件CRUD、团队成员 |
| 合同管理 | 10 | 合同CRUD、审批流程 |
| 财务收费 | 11 | 收费、收款、发票 |
| 文档管理 | 32 | 文档、分类、模板、印章、用印 |
| 证据管理 | 10 | 证据登记、质证记录 |
| 档案管理 | 17 | 档案整理、入库、借阅、库位 |
| 工时管理 | 14 | 工时记录、审批、统计、费率 |
| 任务与日程 | 20 | 任务管理、日程管理 |
| **本清单合计** | **145** | **基础模块** |

> 📌 **注意**：更多模块接口请参阅 [11-API接口清单补充.md](./11-API接口清单补充.md)，包含行政后勤、人力资源、知识库、工作台、OCR识别等186个新增接口。
> 
> **系统总接口数：331+**

---

## 🔐 权限说明

### 权限命名规范

格式：`{模块}:{资源}:{操作}`

- **模块**：client, matter, contract, fee, invoice, document, evidence, archive, timesheet, task, schedule, sys
- **资源**：list, create, update, delete, approve, submit, etc.
- **操作**：根据业务需求定义

### 常用权限

| 权限 | 说明 |
|------|------|
| `{module}:list` | 查询列表 |
| `{module}:create` | 创建 |
| `{module}:update` | 更新 |
| `{module}:delete` | 删除 |
| `{module}:approve` | 审批 |

---

## 📝 接口规范

### 统一响应格式

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": {},
  "timestamp": 1767415000000
}
```

### 分页响应格式

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": {
    "total": 100,
    "list": [],
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  },
  "timestamp": 1767415000000
}
```

### 错误响应格式

```json
{
  "success": false,
  "code": "400",
  "message": "错误信息",
  "timestamp": 1767415000000
}
```

---

## 🔗 API文档访问

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/api/v3/api-docs.yaml

---

## 📌 注意事项

1. **认证方式**：所有接口（除公开接口外）需要在请求头中携带 JWT Token
   ```
   Authorization: Bearer {token}
   ```

2. **请求格式**：Content-Type: `application/json`

3. **日期格式**：`yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss`

4. **分页参数**：
   - `pageNum`: 页码（从1开始）
   - `pageSize`: 每页数量

5. **数据范围**：系统支持数据权限控制，用户只能查看有权限的数据

---

**最后更新**: 2026-01-03  
**维护人**: 后端开发团队

