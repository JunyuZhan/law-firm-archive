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
│   ├── src/main/java/
│   │   └── com/lawfirm/
│   │       ├── common/      # 通用组件
│   │       ├── infrastructure/ # 基础设施层
│   │       ├── domain/      # 领域层
│   │       ├── application/ # 应用层
│   │       └── interfaces/  # 接口层
│   └── src/main/resources/
├── frontend/                # 前端项目（Vben Admin）
├── docker/                  # Docker配置
├── scripts/                 # 脚本文件
│   ├── init-db/            # 数据库初始化
│   ├── backup/             # 备份脚本
│   └── deploy/             # 部署脚本
├── docs/                    # 开发文档
├── progress/                # 开发进度
└── api/                     # API文档
```

## 快速开始

### 1. 环境要求

- JDK 21+
- Node.js 18+
- pnpm 8+
- Docker & Docker Compose
- Git

### 2. 克隆项目

```bash
git clone <repository-url> law-firm
cd law-firm
```

### 3. 初始化前端项目

```bash
# 克隆 Vben Admin 到 frontend 目录
git clone https://github.com/vbenjs/vue-vben-admin.git frontend

# 安装依赖
cd frontend
pnpm install
```

### 4. 启动基础设施

```bash
cd docker
docker-compose up -d

# 验证服务状态
docker-compose ps
```

服务端口：
- PostgreSQL: localhost:5432
- Redis: localhost:6379
- MinIO API: localhost:9000
- MinIO Console: localhost:9001

### 5. 启动后端

```bash
cd backend
mvn spring-boot:run
```

访问：
- API: http://localhost:8080/api
- Swagger: http://localhost:8080/api/swagger-ui.html

### 6. 启动前端

```bash
cd frontend
pnpm dev
```

访问：http://localhost:5666

### 7. 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |

## 文档目录

| 文档 | 说明 |
|------|------|
| [00-系统功能清单](docs/00-系统功能清单.md) | 297个功能点，11个模块 |
| [01-项目启动指南](docs/01-项目启动与环境配置指南.md) | 环境搭建、配置说明 |
| [02-后端开发指南](docs/02-后端核心模块开发指南.md) | 后端架构、代码示例 |
| [03-前端开发指南](docs/03-前端页面开发指南.md) | 前端架构、组件开发 |
| [04-测试部署指南](docs/04-集成测试与部署指南.md) | 测试策略、CI/CD |
| [05-培训上线计划](docs/05-用户培训与上线计划.md) | 用户培训、上线方案 |
| [06-财务模块指南](docs/06-财务管理模块开发指南.md) | 财务业务逻辑详解 |
| [07-开发准备清单](docs/07-开发前准备工作清单.md) | 开发前准备事项 |
| [08-业务逻辑说明](docs/08-律所业务逻辑与模块关系说明.md) | 律所业务术语、流程 |

## 开发进度

查看 [progress/开发进度记录.md](progress/开发进度记录.md)

## 开发规范

### Git 提交规范

```
<type>(<scope>): <subject>

类型：
- feat: 新功能
- fix: 修复Bug
- docs: 文档更新
- style: 代码格式
- refactor: 重构
- test: 测试
- chore: 构建/工具

示例：
feat(user): 添加用户登录功能
fix(finance): 修复提成计算精度问题
```

### 分支规范

- `main`: 生产分支
- `develop`: 开发分支
- `feature/*`: 功能分支
- `bugfix/*`: 修复分支
- `release/*`: 发布分支

## License

Private - All Rights Reserved
