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
# 克隆项目
git clone https://github.com/JunyuZhan/law-firm.git
cd law-firm

# 配置环境变量
cd docker
cp env.example .env
# 编辑 .env 设置密码

# 启动服务
cd ..
./scripts/deploy.sh
```

### 开发环境

#### 1. 启动基础设施

```bash
cd docker
docker compose up -d postgres redis minio
```

#### 2. 初始化数据库

```bash
cd scripts/init-db
./init-database.sh
```

#### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：http://localhost:8080

#### 4. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
# 选择 @vben/web-antd
```

前端地址：http://localhost:5666

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| director | lawyer123 | 律所主任 |
| lawyer1 | lawyer123 | 律师 |

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
| [部署指南](./DEPLOY.md) | 生产环境部署说明 |
| [开发文档](./docs/README.md) | 开发者参考文档 |
| [用户手册](./frontend/docs/) | 用户操作手册（VitePress） |

## 开发规范

### Git 提交规范

```
<type>(<scope>): <subject>

类型：feat/fix/docs/style/refactor/test/chore
示例：feat(finance): 添加发票管理功能
```

## License

Private - All Rights Reserved
