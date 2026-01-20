# 智慧律所管理系统

面向中小型律师事务所（20-50人）的一体化业务管理平台。

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | PostgreSQL | 15.x |
| 缓存 | Redis | 7.x |
| 对象存储 | MinIO | latest |
| 前端框架 | Vue 3 + Vben Admin | 3.x |
| UI组件 | Ant Design Vue | 4.x |

## 项目结构

```
law-firm/
├── backend/                 # 后端项目（Spring Boot）
│   └── src/main/java/com/lawfirm/
│       ├── common/          # 通用组件（工具类、常量、注解）
│       ├── infrastructure/  # 基础设施层（配置、安全、缓存）
│       ├── domain/          # 领域层（实体、仓储）
│       ├── application/     # 应用层（服务、DTO）
│       └── interfaces/      # 接口层（Controller、Scheduler）
├── frontend/                # 前端项目（Vben Admin）
│   └── apps/web-antd/       # 主应用
├── docker/                  # Docker 配置
├── scripts/                 # 脚本文件
│   ├── init-db/             # 数据库初始化脚本
│   ├── jmeter/              # 压力测试脚本
│   └── migration/           # 版本迁移脚本
└── docs/                    # 开发文档
```

## 快速开始

### 环境要求

- JDK 21+
- Node.js 20+
- pnpm 9+
- Docker & Docker Compose

### 一键部署

```bash
# ⚠️ 如果仓库是私有仓库，服务器部署前请先配置 SSH 密钥

# 方式一：服务器上还没有代码（首次配置）
# ⚠️ 注意：私有仓库无法直接从 GitHub 下载脚本，需要先上传脚本到服务器

# 方法 A: 上传脚本到服务器（推荐）
# 1. 在本地电脑上传脚本（替换 root 为你的实际用户名，如 ubuntu, admin）
scp scripts/init-github-ssh.sh root@192.168.50.10:/tmp/
# 2. SSH 登录服务器并运行
ssh root@192.168.50.10
bash /tmp/init-github-ssh.sh

# 方法 B: 手动配置（最简单，无需脚本）
# 1. SSH 登录服务器（替换 root 为你的实际用户名）
ssh root@192.168.50.10
# 2. 生成 SSH 密钥
ssh-keygen -t ed25519 -C "deploy@law-firm" -f ~/.ssh/id_ed25519_deploy -N ""
# 3. 查看公钥并复制
cat ~/.ssh/id_ed25519_deploy.pub
# 4. 将公钥添加到 GitHub: https://github.com/junyuzhan/law-firm/settings/keys
# 5. 配置 SSH
cat >> ~/.ssh/config << EOF
Host github.com
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519_deploy
    IdentitiesOnly yes
EOF
chmod 600 ~/.ssh/config
# 6. 测试连接
ssh -T git@github.com
# 7. 克隆代码
git clone git@github.com:junyuzhan/law-firm.git /opt/law-firm
cd /opt/law-firm

# 方式二：服务器上已有代码
# 直接运行配置脚本
cd /opt/law-firm
./scripts/setup-github-ssh.sh

# 详细配置说明请参考: docs/GITHUB_PRIVATE_REPO_SETUP.md

# 配置环境变量
cd docker
cp env.example .env
# 编辑 .env 设置密码

# 启动服务
cd ..
bash scripts/deploy.sh
```

### 账号与密码

部署完成后，系统会自动生成安全密钥并保存到 `.env` 文件中。

#### 查看密码

```bash
# 在项目目录中查看所有密码
cat .env | grep -E "PASSWORD|SECRET"
```

#### 系统账号

| 服务 | 账号 | 密码 | 说明 |
|------|------|------|------|
| **主应用** | admin | admin123 | 系统管理员 |
| **主应用** | director | admin123 | 主任律师 |
| **主应用** | lawyer1 | admin123 | 普通律师 |
| **主应用** | leader | admin123 | 部门负责人 |
| **主应用** | finance | admin123 | 财务人员 |
| **主应用** | staff | admin123 | 行政人员 |
| **主应用** | trainee | admin123 | 实习律师 |
| **文档站点** | admin | `.env` 中的 `DOCS_PASSWORD` | 文档站点登录 |
| **MinIO 控制台** | `.env` 中的 `MINIO_ACCESS_KEY` | `.env` 中的 `MINIO_SECRET_KEY` | 对象存储管理 |
| **Grafana** | admin | `.env` 中的 `GRAFANA_PASSWORD` | 监控面板 |

#### 密钥说明

| 配置项 | 用途 | 修改影响 |
|--------|------|----------|
| `JWT_SECRET` | 用户登录令牌签名 | 修改后所有用户需重新登录 |
| `DB_PASSWORD` | PostgreSQL 数据库密码 | 首次部署后不要修改 |
| `REDIS_PASSWORD` | Redis 缓存密码 | 修改后需重启服务 |
| `MINIO_ACCESS_KEY/SECRET_KEY` | 对象存储访问密钥 | 修改后需重启服务 |
| `ONLYOFFICE_JWT_SECRET` | OnlyOffice 文档编辑验证 | 需与容器配置一致 |

> ⚠️ **安全提示**：`.env` 文件包含敏感信息，请妥善保管，不要提交到 Git 仓库。

### 开发环境

#### 1. 启动基础设施

```bash
# 方式一：使用统一环境管理脚本（推荐）
./scripts/env-start.sh dev

# 方式二：启动全量开发环境（包括 OnlyOffice、OCR 等）
./scripts/env-start.sh dev --full

# 方式三：手动启动
cd docker
docker compose -f docker-compose.dev.yml up -d
# 或使用全量配置
docker compose -f docker-compose.dev-full.yml up -d
```

#### 2. 初始化数据库

```bash
# 使用重置脚本（推荐，会自动检测容器）
./scripts/reset-db.sh --dev

# 或手动初始化
cd scripts/init-db
./init-database.sh.manual --drop
```

#### 3. 停止和重置

```bash
# 停止开发环境
./scripts/env-stop.sh dev

# 重置开发环境（删除所有数据并重新初始化）
./scripts/env-reset.sh dev
```

### 测试环境

```bash
# 启动测试环境
./scripts/env-start.sh test

# 初始化数据库
./scripts/reset-db.sh --test

# 停止测试环境
./scripts/env-stop.sh test

# 重置测试环境
./scripts/env-reset.sh test
```

> 📖 详细环境配置说明请参考 [环境配置文档](docs/ENVIRONMENT_CONFIGURATION.md)

#### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：http://localhost:8080/api

#### 4. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
# 选择 @vben/web-antd
```

前端地址：http://localhost:5173（Vite 默认端口）

### 默认账号

| 用户名 | 密码 | 角色 | 姓名 |
|--------|------|------|------|
| admin | admin123 | 管理员 | 系统管理员 |
| director | admin123 | 律所主任 | 律所主任 |
| lawyer1 | admin123 | 律师 | 张律师 |
| leader | admin123 | 团队负责人 | 李团长 |
| finance | admin123 | 财务 | 王财务 |
| staff | admin123 | 行政 | 赵行政 |
| trainee | admin123 | 实习律师 | 陈实习 |

## 功能模块

| 模块 | 说明 |
|------|------|
| 客户管理 | 客户信息、线索跟进、利冲检查 |
| 项目管理 | 案件/项目、任务、期限、文档 |
| 财务管理 | 合同、收费、发票、提成、费用 |
| 文档管理 | 文档存储、版本控制、卷宗归档 |
| 证据管理 | 证据清单、质证记录 |
| 工时管理 | 工时记录、计时器、汇总统计 |
| 知识库 | 案例库、法规库、学习笔记 |
| 人力资源 | 员工、考勤、培训、薪酬、绩效 |
| 行政管理 | 印章、会议室、外出、采购 |
| 系统管理 | 用户、角色、权限、配置、日志 |

## 文档

| 文档 | 说明 |
|------|------|
| [Docker 部署指南](./docker/DEPLOY.md) | 生产环境部署说明 |
| [GitHub 私有仓库配置](./docs/GITHUB_PRIVATE_REPO_SETUP.md) | 服务器部署私有仓库配置指南 |
| [开发文档](./docs/README.md) | 开发者参考文档 |
| [用户手册](./frontend/docs/) | 用户操作手册（VitePress） |

### 文档站点

基于 VitePress 构建的文档站点，包含用户手册、运维手册和 API 文档。

#### 启动文档站点

```bash
cd frontend
pnpm docs:dev
```

访问地址：http://localhost:6173

#### 访问权限

| 文档类型 | 路径 | 是否需要登录 |
|---------|------|-------------|
| 首页 | `/` | ❌ 不需要 |
| 用户手册 | `/guide/user/` | ❌ 不需要 |
| 运维手册 | `/guide/ops/` | ✅ 需要登录 |
| API 文档 | `/guide/api/` | ✅ 需要登录 |

#### 登录凭证配置

**生产环境（一键部署）：** 密码在首次部署时自动生成，保存在项目根目录的 `.env` 文件中：
- `DOCS_USERNAME` - 登录用户名（默认：admin）
- `DOCS_PASSWORD` - 登录密码（自动生成）

**开发环境：** 通过 `frontend/docs/.env.development` 配置：
- `VITE_DOCS_USERNAME` - 登录用户名（默认：admin）
- `VITE_DOCS_PASSWORD` - 登录密码（默认：lawfirm@2026）
- `查看密码`  - cat .env | grep DOCS_PASSWORD

#### 从主应用跳转

用户可以从主应用（http://localhost:5173）的用户头像菜单中点击「用户手册」直接跳转到文档站点，系统会自动携带登录 token，无需再次登录即可查看运维手册和 API 文档。

## 开发规范

### Git 提交规范

```
<type>(<scope>): <subject>

类型：feat/fix/docs/style/refactor/test/chore
示例：feat(finance): 添加发票管理功能
```

## License

Private - All Rights Reserved
