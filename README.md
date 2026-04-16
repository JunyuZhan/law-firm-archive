# Law Firm Archive

面向律师事务所场景的电子档案平台，负责接收业务系统归档数据，完成档案接收、检索、借阅、鉴定、销毁、统计与运维管理。

## Overview

- Backend: Spring Boot 3, MyBatis-Plus, PostgreSQL, Redis, MinIO
- Frontend: Vue 3, Vite, Pinia, Element Plus
- Open API: `/api/open/**`
- Console API: `/api/**`
- Default Local API: `http://localhost:8090/api`
- Default Local Web: `http://localhost:3001`

## What It Solves

- 统一接收来自管理系统或其他业务系统的归档数据
- 管理电子档案全生命周期，包括保存、检索、借阅和销毁
- 通过来源系统、开放接口和推送记录建立可追踪的对接链路
- 提供适合交付场景的部署、升级、回滚与验收资料

## Core Capabilities

- 档案接收：开放接口接收档案元数据与文件地址
- 档案管理：列表、详情、检索、补录、状态流转
- 借阅利用：申请、审批、电子借阅链接、访问记录
- 基础数据：全宗、分类、保管期限、位置、来源系统
- 系统治理：用户、角色、配置、日志、统计、推送记录
- 备份恢复：备份目标、恢复流程、维护模式与台账支持

## Architecture

```text
External Systems
        |
        v
  Open API Gateway
        |
        v
 Archive Backend  ---- PostgreSQL / Redis / RabbitMQ / Elasticsearch / MinIO
        |
        v
 Archive Console
```

## Repository Layout

```text
.
├── backend/    Spring Boot service
├── frontend/   Vue 3 console
├── docker/     Compose, Nginx and deployment templates
├── docs/       Integration, security and delivery documentation
└── scripts/    Bootstrap and delivery scripts
```

## Quick Start

### Docker

```bash
cp docker/.env.example docker/.env
cd docker
docker compose up -d
```

访问入口：

- Web: `http://localhost:3001`
- API: `http://localhost:8090/api`
- Swagger: `http://localhost:8090/api/doc.html`

### Local Development

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

## Integration

对接管理系统或其他业务系统时，重点关注三项：

- 在后台“来源管理”中创建来源并分配 API Key
- 通过 `POST /api/open/archive/receive` 推送归档数据
- 按需配置 `callbackUrl` 以接收处理结果通知

专题文档：

- [API 对接指南](./docs/API%E5%AF%B9%E6%8E%A5%E6%8C%87%E5%8D%97.md)
- [外部业务系统接入指南](./docs/%E6%A1%A3%E6%A1%88%E7%B3%BB%E7%BB%9F%E5%AF%B9%E6%8E%A5%E6%8C%87%E5%8D%97.md)

## Delivery

面向正式交付时，建议统一使用容器化部署与版本化镜像。

**源码、测试机构建、私有库、对外部署（不要混用）**

- **源代码**只从 **GitHub** 拉取。私有仓库 **192.168.50.5:5050** 等只存**已构建镜像**，不提供源码。
- **构建 / 测试专用机（例如 myu）**：在**完整克隆**的仓库里（如 `/root/src/law-firm-archive`），进入 **`docker/`**，使用 **`docker-compose.yml`**：`docker compose build` 再从源码起容器 `docker compose up -d`（必要时 `down -v` 做干净库）。这里的 backend、frontend、Elasticsearch 都是 **`build:` 本地 Dockerfile**，**不是**从私有库 `pull`。测通、冒烟通过后，再在仓库根目录执行 **`scripts/build-and-push-on-linux.sh`**（配置好 `docker/.env.registry` 且已 `docker login`），把**同一套已验证构建**推到私有库，供对外使用。
- **客户或生产环境**：才使用 **`docker-compose.registry.yml`**，配置 **`REGISTRY_PULL` / `APP_VERSION`**，**从私有库拉取**已在测试机发布好的镜像运行。
- **切勿**在 myu 这类测试机上用 `docker-compose.registry.yml` 拉私有库镜像当「最新源码测试」——那样跑的是旧制品，不是当前 GitHub `main`。

- [部署与升级手册](./docs/deployment-upgrade-guide.md)
- [发布前验收清单](./docs/release-checklist.md)
- [部署后冒烟测试](./docs/deployment-smoke-test.md)
- [测试台账](./docs/test-ledger.md)

## Security

- 管理接口默认使用 JWT Bearer Token
- 开放接口按来源系统 API Key 鉴权
- `.env`、本地索引、代理辅助目录、编译产物不纳入版本控制

更多说明见：

- [安全说明](./docs/SECURITY.md)

## Project Notes

- 仓库文档以当前 `main` 分支代码行为为准
- 开源仓库默认只保留源码、必要文档和示例配置
- 本地构建产物、代理辅助配置和私有环境信息不应提交
