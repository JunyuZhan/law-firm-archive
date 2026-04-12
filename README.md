# 档案管理系统

独立部署的档案管理系统，用于接收外部业务系统推送的归档档案，并提供档案接收、检索、借阅、鉴定、销毁、统计与系统管理能力。

## 当前状态

- 后端：Spring Boot 3.2.2 + MyBatis-Plus + PostgreSQL + Redis + MinIO
- 前端：Vue 3 + Vite + Pinia + Element Plus
- 开放接口：`/api/open/**`
- 管理接口：`/api/**`
- 本地后端默认端口：`8090`
- Docker 前端入口：`http://localhost:3001`

## 核心能力

- 档案接收：支持外部系统通过开放接口推送档案及电子文件元数据
- 档案管理：档案创建、更新、分页查询、全文检索、补充上传、状态更新
- 借阅管理：借阅申请、审批、电子借阅链接生成、公开访问与下载记录
- 鉴定与销毁：鉴定流程、销毁申请与执行
- 基础数据：全宗、分类、保管期限、存放位置、来源系统
- 系统能力：用户、角色、系统配置、操作日志、推送记录、统计报表

## 目录结构

```text
.
├── backend/                    # Spring Boot 后端
├── frontend/                   # Vue 3 前端
├── docker/                     # Docker / Nginx / Compose 配置
├── docs/                       # 对接与安全文档
├── scripts/                    # 初始化与辅助脚本
├── AGENTS.md                   # 仓库级代理说明
└── CLAUDE.md                   # Claude/GitNexus 说明
```

## 运行方式

### 1. Docker Compose 本地构建

```bash
cp docker/.env.example docker/.env
cd docker
docker compose up -d
```

默认访问地址：

- 前端：`http://localhost:3001`
- 后端 API：`http://localhost:8090/api`
- Swagger / Knife4j：`http://localhost:8090/api/doc.html`
- Actuator Health：`http://localhost:8090/api/actuator/health`
- MinIO 控制台：`http://localhost:9003`
- RabbitMQ 管理台：`http://localhost:15672`

说明：

- Docker 前端通过 Nginx 代理 `/api/` 到后端
- Docker 前端通过 Nginx 代理 `/storage/` 到 MinIO
- Compose 会同时启动 PostgreSQL、Redis、RabbitMQ、Elasticsearch、MinIO、后端、前端
- 首次启动前请编辑 `docker/.env`，不要直接使用示例中的占位密钥

### 2. 私有仓库一键拉取部署

公网拉取地址使用 `hub.albertzhan.top`，仓库内置了镜像版 Compose 与一键脚本。

```bash
cp docker/.env.registry.example docker/.env.registry
./scripts/pull-from-registry.sh
```

默认会执行：

```bash
docker compose --env-file docker/.env.registry -f docker/docker-compose.registry.yml pull
docker compose --env-file docker/.env.registry -f docker/docker-compose.registry.yml up -d
```

镜像命名：

- `hub.albertzhan.top/law-firm-archive/backend:v0.1.2`
- `hub.albertzhan.top/law-firm-archive/frontend:v0.1.2`
- `hub.albertzhan.top/law-firm-archive/elasticsearch:v0.1.2`

说明：

- Docker CLI 没有 `docker hub.pull` 子命令，标准命令是 `docker pull`
- 如果要整套一键更新，直接运行 `./scripts/pull-from-registry.sh`

### 3. 私有仓库内网推送

内网推送地址使用 `192.168.50.5:5050`。

```bash
cp docker/.env.registry.example docker/.env.registry
./scripts/push-images.sh
```

脚本会构建并推送：

- `192.168.50.5:5050/law-firm-archive/backend:${APP_VERSION}`
- `192.168.50.5:5050/law-firm-archive/frontend:${APP_VERSION}`
- `192.168.50.5:5050/law-firm-archive/elasticsearch:${APP_VERSION}`
- `192.168.50.5:5050/law-firm-archive/backend:${APP_COMMIT_SHA}`
- `192.168.50.5:5050/law-firm-archive/frontend:${APP_COMMIT_SHA}`
- `192.168.50.5:5050/law-firm-archive/elasticsearch:${APP_COMMIT_SHA}`

建议：

- `APP_VERSION` 使用可读发布号，例如 `v0.1.2`
- `APP_COMMIT_SHA` 使用 Git 提交短哈希，例如 `7cc18a52`
- `APP_BUILD_TIME` 建议使用 ISO 时间，例如 `2026-03-30T22:30:00+08:00`
- 部署时 Compose 优先使用 `APP_VERSION`

### 4. 本地开发

先准备依赖服务。后端开发默认使用这些本地地址：

- PostgreSQL：`localhost:5433`
- Redis：`localhost:6380`
- MinIO：`http://localhost:9002`
- RabbitMQ：`localhost:5672`
- Elasticsearch：`http://localhost:9200`

后端：

```bash
export SPRING_DATASOURCE_PASSWORD=your-db-password
export SPRING_RABBITMQ_PASSWORD=your-rabbitmq-password
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=your-minio-secret
export JWT_SECRET=your-jwt-secret-at-least-32-characters
cd backend
mvn spring-boot:run
```

如果你是通过 `docker compose` 起的本地依赖，以上值应与 `docker/.env` 中保持一致。

前端：

```bash
cd frontend
npm install
npm run dev
```

前端开发环境默认通过 `VITE_API_PROXY_TARGET=http://localhost:8090` 代理后端。

## 安全与认证

### 管理接口

- 登录接口：`POST /api/auth/login`
- 刷新令牌：`POST /api/auth/refresh`
- 当前用户：`GET /api/auth/me`
- 登出：`POST /api/auth/logout`
- 管理接口默认使用 JWT Bearer Token

### 开放接口

开放接口路径位于 `/api/open/**`，其中：

- 需要 `X-API-Key`：
  - `POST /api/open/archive/receive`
  - `POST /api/open/borrow/apply`
- 不需要 `X-API-Key`：
  - `GET /api/open/health`
  - `GET /api/open/borrow/access/{token}`
  - `POST /api/open/borrow/access/{token}/download/{fileId}`

来源系统 API Key 由后台“来源管理”维护。

## 关键接口概览

### 开放接口

- `POST /api/open/archive/receive`：接收档案
- `POST /api/open/borrow/apply`：申请电子借阅链接
- `GET /api/open/borrow/access/{token}`：公开访问借阅内容
- `POST /api/open/borrow/access/{token}/download/{fileId}`：记录下载
- `GET /api/open/health`：健康检查

### 管理接口

- `GET /api/archives`：档案列表
- `GET /api/archives/{id}`：档案详情
- `GET /api/archives/search`：全文检索
- `POST /api/archives`：创建档案
- `PUT /api/archives/{id}`：更新档案
- `POST /api/archives/{id}/supplement`：补充上传
- `PUT /api/archives/{id}/status`：更新状态
- `GET /api/statistics/**`：统计报表
- `GET /api/sources`：来源系统管理
- `GET /api/users`、`GET /api/roles`：用户和角色管理

## 测试与构建

后端测试：

```bash
cd backend
mvn test
```

后端打包：

```bash
cd backend
mvn clean package -DskipTests
```

前端构建：

```bash
cd frontend
npm install
npm run build
```

说明：

- 当前仓库后端测试已可在本机跑通

## 交付文档

- [部署与升级手册](/Users/apple/Documents/Project/law-firm-archive/docs/deployment-upgrade-guide.md)
- [发布前验收清单](/Users/apple/Documents/Project/law-firm-archive/docs/release-checklist.md)
- [部署后冒烟测试流程](/Users/apple/Documents/Project/law-firm-archive/docs/deployment-smoke-test.md)
- [测试执行台账](/Users/apple/Documents/Project/law-firm-archive/docs/test-ledger.md)
- [备份与恢复中心设计方案](/Users/apple/Documents/Project/law-firm-archive/docs/backup-recovery-design.md)
- Maven 测试阶段会生成 JaCoCo 覆盖率报告

## 文档索引

- [API 对接指南](./docs/API%E5%AF%B9%E6%8E%A5%E6%8C%87%E5%8D%97.md)
- [集成测试指南](./docs/integration-test.md)
- [律所系统对接指南](./docs/%E6%A1%A3%E6%A1%88%E7%B3%BB%E7%BB%9F%E5%AF%B9%E6%8E%A5%E6%8C%87%E5%8D%97.md)
- [安全说明](./docs/SECURITY.md)

## 仓库约定

- `.claude/` 这类本地代理 / IDE 工作流配置不作为项目必需内容，默认不纳入版本控制
- `.gitnexus/` 为本地分析索引目录，不提交
- 仓库内文档应以当前代码行为为准，不以历史分支命名或旧部署端口为准
