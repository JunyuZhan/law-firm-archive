# 部署后冒烟测试流程

本文档用于律师事务所电子档案系统首次部署、升级后复测或客户交付验收，目标是验证核心链路是否可用：

- 入库
- 保存
- 借阅

## 1. 测试目标

本轮冒烟测试覆盖以下主链路：

1. 基础服务可用
2. 管理员与业务用户可登录
3. 开放接口可接收入库请求
4. 档案员可查看档案、补充电子文件、更新归档状态
5. 普通用户可申请借阅
6. 档案员可审批、借出并生成电子借阅链接
7. 借阅公开访问、预览/下载链路可用

## 2. 测试环境

按实际环境填写，建议至少记录：

- 前端入口
- 后端入口
- 部署目录
- 当前部署版本
- 镜像标签
- Git 提交号

## 3. 初始化账号

首次部署建议使用整合脚本 [02-schema-consolidated.sql](/Users/apple/Documents/Project/law-firm-archive/scripts/init-db/02-schema-consolidated.sql) 初始化数据库。

如当前环境沿用默认测试账号，可按以下口径验证：

- 系统管理员：`admin / admin123`
- 档案员：`archivist1 / admin123`
- 普通用户：`lawyer1 / admin123`

默认密码仅用于测试环境，生产环境必须立即修改。

## 4. 初始化开放接口凭证

初始化来源：

- 来源编码：`LAW_FIRM_MAIN`
- 默认 API Key：`lawfirm-archive-api-key-2026`

该 Key 仅建议用于测试环境冒烟，不建议在正式客户环境长期保留。

## 5. 冒烟主流程

### 5.1 基础健康检查

验证项：

- `GET /api/actuator/health` 返回 `UP`
- 前端首页可返回 `200`
- 域名 `https://arc.albertzhan.top` 可打开

### 5.2 登录与鉴权

验证项：

- `admin` 登录成功，拿到 `accessToken`
- `archivist1` 登录成功，拿到 `accessToken`
- `lawyer1` 登录成功，拿到 `accessToken`
- `GET /api/auth/me` 与当前登录用户匹配

### 5.3 开放入库

使用 `X-API-Key` 调用：

- `GET /api/open/health`
- `POST /api/open/archive/receive`

建议创建 1 条测试档案，字段至少包含：

- `sourceType=LAW_FIRM`
- `sourceId`
- `sourceNo`
- `title`
- `archiveType=DOCUMENT`
- `retentionPeriod=Y10`
- `caseNo`
- `caseName`
- `clientName`
- `lawyerName`

验收点：

- 接口返回成功
- 返回 `archiveId` 与 `archiveNo`
- 后台列表中能查到该档案

### 5.4 保存与电子文件补充

由 `archivist1` 执行：

1. 查询刚创建的档案详情
2. 上传 1 个电子文件到该档案
3. 执行正式归档，将档案状态更新为 `STORED`
4. 再次查询档案详情

验收点：

- 档案详情能看到电子文件
- 文件数量大于 0
- 档案状态为 `STORED`
- 档案具备电子利用前提
- 未归档档案不会被错误允许借阅

### 5.5 普通借阅申请

由 `lawyer1` 执行：

1. 调用 `GET /api/borrows/check/{archiveId}`
2. 提交 `POST /api/borrows/apply`

建议借阅方式：

- `ONLINE`

验收点：

- 借阅可用性检查返回 `available=true`
- 借阅申请提交成功
- 返回借阅申请 `id`

### 5.6 审批与借出

由 `archivist1` 执行：

1. 查询待审批列表
2. 审批通过
3. 借出
4. 生成电子借阅链接

验收点：

- 申请状态从 `PENDING` 变为 `APPROVED`
- 借出后状态进入 `BORROWED`
- 成功生成 `accessToken` 或访问链接

### 5.7 公开访问与电子利用

使用生成的借阅链接执行：

1. `GET /api/open/borrow/access/{token}`
2. 如有文件，获取预览链接
3. 如允许下载，记录下载或获取下载链接

验收点：

- 借阅公开访问返回 `valid=true`
- 返回档案信息与文件列表
- 预览链接可生成
- 下载允许时下载链接可生成或下载记录成功

## 6. 通过标准

以下条件同时满足，视为本轮部署通过：

- 全部容器健康
- 登录链路正常
- 开放入库成功
- 电子文件成功保存并可在档案详情中看到
- 借阅申请、审批、借出链路正常
- 电子借阅公开访问成功

## 7. 建议留痕

执行测试后建议同步记录到测试台账，字段至少包含：

- 客户名称
- 环境名称
- 测试时间
- 部署版本
- 镜像标签
- 提交号
- 测试档案号
- 借阅申请号
- 借阅链接号
- 是否通过
- 是否触发回滚
