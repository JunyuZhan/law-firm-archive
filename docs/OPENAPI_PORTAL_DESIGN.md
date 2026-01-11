# 客户服务 OpenAPI 设计方案

## 一、设计背景

为满足以下需求，设计了客户服务 OpenAPI 方案：
1. 向客户推送项目信息和进度（在律师操作下）
2. 保护内部系统安全，与客户访问层完全隔离
3. 支持函件、合同等业务对象的二维码真伪验证
4. 为独立的"客户服务系统"提供数据推送接口

## 二、系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                         律所管理系统（内部）                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    项目详情页 - 客户服务 Tab                   │   │
│  │                                                              │   │
│  │  推送设置：                        推送统计：                 │   │
│  │  ☑ 项目基本信息                    累计推送: 5 次             │   │
│  │  ☑ 项目进度                        最近推送: 2026-01-11       │   │
│  │  ☑ 承办律师                                                  │   │
│  │  ☑ 关键期限                                                  │   │
│  │  ☐ 办理事项                                                  │   │
│  │  ☐ 费用信息                                                  │   │
│  │                                                              │   │
│  │  [📤 推送到客户服务系统]           [保存配置]                 │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     数据推送服务                              │   │
│  │  • 组装项目数据（脱敏处理）                                   │   │
│  │  • 调用客户服务系统 API                                       │   │
│  │  • 记录推送日志                                               │   │
│  └─────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────│───────────────────────────────────┘
                                   │
                                   │ HTTP POST /api/matter/receive
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      客户服务系统（独立部署）                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     接收推送数据                              │   │
│  │  • 存储项目信息                                               │   │
│  │  • 生成访问链接                                               │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     通知客户                                  │   │
│  │  • 发送短信通知                                               │   │
│  │  • 公众号消息推送                                             │   │
│  │  • 邮件通知                                                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     客户查看                                  │   │
│  │  • 点击链接访问                                               │   │
│  │  • 查看项目进度                                               │   │
│  │  • 查看承办律师                                               │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## 三、数据库设计

### 1. 推送记录表 (`openapi_push_record`)

记录每次数据推送的详细信息。

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键 |
| matter_id | BIGINT | 项目ID |
| client_id | BIGINT | 客户ID |
| push_type | VARCHAR(20) | 推送类型: MANUAL/AUTO/UPDATE |
| scopes | VARCHAR(500) | 推送范围（逗号分隔） |
| data_snapshot | JSONB | 推送的数据快照（脱敏后） |
| external_id | VARCHAR(100) | 客户服务系统返回的ID |
| external_url | VARCHAR(500) | 客户访问链接 |
| status | VARCHAR(20) | 状态: PENDING/SUCCESS/FAILED |
| error_message | TEXT | 错误信息 |
| expires_at | TIMESTAMP | 数据有效期 |

### 2. 推送配置表 (`openapi_push_config`)

项目级别的推送设置。

| 字段 | 类型 | 说明 |
|-----|------|------|
| matter_id | BIGINT | 项目ID（唯一） |
| client_id | BIGINT | 客户ID |
| enabled | BOOLEAN | 是否启用 |
| scopes | VARCHAR(500) | 默认推送范围 |
| auto_push_on_update | BOOLEAN | 项目更新时自动推送 |
| valid_days | INTEGER | 数据有效期（天） |

## 四、API 接口设计

### 律师端接口（项目详情页使用）

| 接口 | 方法 | 权限 | 说明 |
|-----|------|------|------|
| `/matter/client-service/push` | POST | matter:clientService:create | 推送数据到客户服务系统 |
| `/matter/client-service/records` | GET | matter:clientService:list | 获取推送记录列表 |
| `/matter/client-service/records/{id}` | GET | matter:clientService:list | 获取推送记录详情 |
| `/matter/client-service/latest` | GET | matter:clientService:list | 获取最近一次成功推送 |
| `/matter/client-service/config` | GET | matter:clientService:list | 获取推送配置 |
| `/matter/client-service/config` | PUT | matter:clientService:create | 更新推送配置 |
| `/matter/client-service/statistics` | GET | matter:clientService:list | 获取推送统计 |
| `/matter/client-service/scopes` | GET | matter:clientService:list | 获取可推送范围选项 |

### 推送请求示例

```json
POST /matter/client-service/push
{
  "matterId": 123,
  "clientId": 456,
  "scopes": ["MATTER_INFO", "MATTER_PROGRESS", "LAWYER_INFO", "DEADLINE_INFO"],
  "validDays": 30
}
```

### 推送响应示例

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "matterId": 123,
    "matterName": "张三诉李四借款纠纷案",
    "clientId": 456,
    "clientName": "张三",
    "pushType": "MANUAL",
    "scopes": ["MATTER_INFO", "MATTER_PROGRESS", "LAWYER_INFO", "DEADLINE_INFO"],
    "status": "SUCCESS",
    "externalId": "cs_123456",
    "externalUrl": "https://cs.example.com/view/abc123",
    "expiresAt": "2026-02-11T00:00:00",
    "createdAt": "2026-01-11T10:30:00"
  }
}
```

## 五、推送数据范围（Scope）

| 范围 | 说明 | 包含信息 |
|-----|------|---------|
| `MATTER_INFO` | 项目基本信息 | 项目名称、编号、类型、状态 |
| `MATTER_PROGRESS` | 项目进度 | 当前阶段、整体进度、最近更新 |
| `LAWYER_INFO` | 承办律师 | 团队成员姓名、角色、联系方式（脱敏） |
| `DEADLINE_INFO` | 关键期限 | 诉讼时效、举证期限、开庭时间 |
| `TASK_LIST` | 办理事项 | 任务标题、状态、进度 |
| `DOCUMENT_LIST` | 文书材料 | 文档名称列表（不含内容） |
| `FEE_INFO` | 费用信息 | 合同金额、已收款、待收款 |

## 六、客户服务系统接口规范

本系统调用客户服务系统时，需要客户服务系统实现以下接口：

### 接收推送数据

```
POST /api/matter/receive
Content-Type: application/json
Authorization: Bearer {api_key}

{
  "clientId": 456,
  "clientName": "张三",
  "matterData": {
    "matterId": 123,
    "matterName": "张三诉李四借款纠纷案",
    "matterNo": "M202601001",
    "status": "ACTIVE",
    "statusName": "进行中",
    "progress": 60,
    "currentPhase": "TRIAL",
    "currentPhaseName": "审理阶段",
    "lawyerList": [...],
    "deadlineList": [...],
    ...
  },
  "validDays": 30,
  "scopes": ["MATTER_INFO", "MATTER_PROGRESS", "LAWYER_INFO"]
}

响应:
{
  "code": 200,
  "data": {
    "id": "cs_123456",           // 客户服务系统的数据ID
    "accessUrl": "https://..."   // 客户访问链接
  }
}
```

## 七、使用流程

### 律师操作

1. 进入「项目管理 → 项目列表」
2. 点击项目进入详情页
3. 切换到「客户服务」Tab
4. 选择要推送的数据内容
5. 点击「推送到客户服务系统」
6. 系统将数据发送到客户服务系统

### 客户收到通知

1. 客户服务系统收到数据后
2. 自动通过短信/公众号/邮件通知客户
3. 客户点击链接查看项目信息

### 功能特点

- 数据脱敏：手机号、邮箱等敏感信息自动脱敏
- 推送记录：完整记录每次推送，支持审计
- 自动推送：可配置项目更新时自动推送
- 有效期控制：数据在客户服务系统中有有效期

## 八、外部系统配置

在「系统管理 → 外部系统集成」中配置客户服务系统：

| 配置项 | 说明 | 示例值 |
|-------|------|--------|
| 集成名称 | 客户服务系统 | - |
| 集成类型 | CLIENT_SERVICE | - |
| API地址 | 客户服务系统接口地址 | https://cs.example.com/api |
| API密钥 | 认证密钥 | sk-xxx |
| 是否启用 | 启用后才能推送 | true |

## 九、文件清单

### 后端文件
```
backend/src/main/java/com/lawfirm/
├── domain/openapi/entity/
│   ├── PushRecord.java              # 推送记录实体
│   └── PushConfig.java              # 推送配置实体
├── infrastructure/persistence/mapper/openapi/
│   ├── PushRecordMapper.java        # 推送记录 Mapper
│   └── PushConfigMapper.java        # 推送配置 Mapper
├── application/openapi/
│   ├── dto/
│   │   ├── PushRequest.java         # 推送请求
│   │   ├── PushRecordDTO.java       # 推送记录 DTO
│   │   └── PushConfigDTO.java       # 推送配置 DTO
│   └── service/
│       ├── DataPushService.java     # 数据推送服务
│       └── PortalDataService.java   # 数据组装服务
└── interfaces/rest/matter/
    └── MatterClientServiceController.java  # 客户服务接口
```

### 前端文件
```
frontend/apps/web-antd/src/
├── api/system/openapi.ts              # API 接口
├── components/ClientServicePanel/     # 客户服务面板
│   ├── index.vue                      # 推送管理界面
│   └── index.ts                       # 导出
└── views/matter/detail/index.vue      # 项目详情页（客户服务 Tab）
```

### 数据库脚本
```
scripts/init-db/32-openapi-schema.sql  # 数据库表结构
```

## 十、安全措施

1. **数据脱敏**：推送前自动脱敏敏感信息
2. **权限控制**：需要 `matter:clientService:*` 权限
3. **审计日志**：完整记录推送操作
4. **有效期**：数据在客户服务系统中有时效性
5. **系统隔离**：客户只能访问客户服务系统，无法访问内部系统

## 十一、后续扩展

### 客户服务系统（需另行开发）
- 独立部署的轻应用
- 接收推送数据并存储
- 多渠道通知客户（短信、公众号、邮件）
- 提供客户友好的访问界面
- 支持移动端访问

### 自动推送
- 项目状态变更时自动推送
- 关键期限到期前提醒
- 定时同步最新进度
