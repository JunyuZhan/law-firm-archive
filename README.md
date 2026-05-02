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
├── VERSION     产品版本（单一来源）；修改后运行 scripts/sync-version.sh 同步 pom 与前端 package.json
├── backend/    Spring Boot service
├── frontend/   Vue 3 console
├── docker/     Compose, Nginx and deployment templates
├── docs/       Integration, security and delivery documentation
└── scripts/    Bootstrap and delivery scripts
```

产品版本与界面展示：`VERSION` 与 Docker 构建参数 `APP_VERSION`（未设置时回退到 `VERSION`）、后端 `runtime-info`、前端构建注入一致。发布镜像时请让 `APP_VERSION` 与私有库 tag 对齐。

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

### Dist Center（标准安装入口）

**分发中心**的定位是：**一键部署**——用固定 URL 提供 **`install.sh`（首次安装）** 与 **`update.sh`（升级）**，客户机 `curl … | bash` 即可拉取公共脚本、版本描述 **`versions/latest.json`**、Compose 与初始化资产并完成拉镜像 / 起服务。与 **`law-firm-archive` 仓库同级**的 **`dist-center`** 仓库实现该站点（slug 示例：`law-firm-archive`）。运行中的本系统还可**可选地**配置 **`latest.json` 的完整 URL**，在后台「系统信息」与分发中心公布的镜像标签对齐（见环境变量说明）。**对外出售时**：应由客户或集成商部署**其自有域名**上的分发站（或镜像 dist-center 资产到客户内网），避免成品默认依赖某一特定公网域名。

若由你们托管官方分发站，目标机可使用与分发站域名一致的安装命令（默认安装根目录 **`/opt/law-firm-archive`** 以 `manifest.json` 为准），例如：

```bash
curl -fsSL https://<分发站域名>/projects/law-firm-archive/install.sh | sudo bash
```

分发站会下发与仓库中 **`docker/docker-compose.yml`** 对齐的全栈 Compose、环境模板及 **`scripts/init-db/02-schema-consolidated.sql`**。发版时需将 **`elasticsearch` / `backend` / `frontend`** 镜像推到 `latest.json` 所声明的仓库前缀，并同步更新客户可见分发站上的 **`latest.json`**。需要后台「与分发中心比对版本」时，在客户环境配置 **`DIST_CENTER_LATEST_JSON_URL`** 指向其 **`versions/latest.json`**（见 **`docker/.env.example`** 注释）。

**源码、测试机构建、私有库、对外部署（不要混用）**

- **源代码**只从 **GitHub** 拉取。私有仓库 **192.168.50.5:5050** 等只存**已构建镜像**，不提供源码。
- **构建 / 测试专用机（例如 myu）**：在**完整克隆**的仓库里（如 `/root/src/law-firm-archive`），进入 **`docker/`**，使用 **`docker compose up -d --build`** 从源码构建并起容器（必要时 `down -v` 做干净库）。这里的 backend、frontend、Elasticsearch 都是 **`build:` 本地 Dockerfile**，**不是**从私有库 `pull`。测通、冒烟通过后，再在仓库根目录执行 **`scripts/build-and-push-on-linux.sh`**（配置好 `docker/.env` 且已 `docker login`），把**同一套已验证构建**推到私有库，供对外使用。
- **客户或生产环境**：设置 **`REGISTRY_PREFIX`**（如 `hub.albertzhan.top/`）和 **`APP_VERSION`**，使用 **`docker compose up -d`** 从私有库拉取已在测试机发布好的镜像运行。
- **切勿**在 myu 这类测试机上设 `REGISTRY_PREFIX` 拉私有库镜像当「最新源码测试」——那样跑的是旧制品，不是当前 GitHub `main`。

- [部署与升级手册](./docs/deployment-upgrade-guide.md)
- [发布前验收清单](./docs/release-checklist.md)
- [部署后冒烟测试](./docs/deployment-smoke-test.md)
- [测试台账](./docs/test-ledger.md)
- [缺陷台账](./docs/defect-ledger.md)

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
