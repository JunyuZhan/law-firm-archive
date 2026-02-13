# 档案管理系统

独立部署的档案管理系统，用于接收律所管理系统的归档档案，并提供完整的档案管理服务。

## 功能概述

### 核心功能
- **档案接收**：接收律所管理系统推送的归档档案
- **档案入库**：档案物理入库管理，支持位置分配
- **档案检索**：多维度档案检索查询
- **档案借阅**：借阅申请、审批、归还管理
- **统计分析**：档案统计报表

### 扩展功能
- **多来源收集**：支持接收多个外部系统的档案
- **来源配置**：可配置不同的档案来源系统
- **批量导入**：支持档案批量导入

## 技术架构

### 后端
- **框架**：Spring Boot 3.2.2
- **数据库**：PostgreSQL 16
- **ORM**：MyBatis-Plus 3.5.5
- **缓存**：Redis 7
- **存储**：MinIO
- **API文档**：Knife4j/OpenAPI 3

### 前端
- **框架**：Vue 3.4
- **UI组件**：Element Plus 2.5
- **构建工具**：Vite 5
- **状态管理**：Pinia
- **路由**：Vue Router 4

## 项目结构

```
archive-system/
├── backend/                    # 后端项目
│   ├── src/main/java/com/archivesystem/
│   │   ├── config/            # 配置类
│   │   ├── controller/        # 控制器
│   │   ├── service/           # 服务层
│   │   ├── repository/        # 数据访问层
│   │   ├── entity/            # 实体类
│   │   ├── dto/               # 数据传输对象
│   │   └── common/            # 公共类
│   └── src/main/resources/    # 配置文件
├── frontend/                   # 前端项目
│   └── src/
│       ├── api/               # API调用
│       ├── components/        # 组件
│       ├── views/             # 页面
│       ├── router/            # 路由
│       └── store/             # 状态管理
├── docker/                     # Docker配置
│   ├── Dockerfile
│   └── docker-compose.yml
└── scripts/                    # 脚本
    └── init-db/               # 数据库初始化
```

## 快速开始

### 使用Docker Compose（推荐）

```bash
cd docker
docker-compose up -d
```

服务启动后：
- 后端API：http://localhost:8090/api
- API文档：http://localhost:8090/api/doc.html
- 前端：http://localhost:3001
- MinIO控制台：http://localhost:9003

### 本地开发

#### 后端
```bash
cd backend
mvn spring-boot:run
```

#### 前端
```bash
cd frontend
npm install
npm run dev
```

## API接口

### 开放接口（供外部系统调用）

#### 接收律所档案
```
POST /api/open/law-firm/archive/receive
Content-Type: application/json
Authorization: Bearer {api-key}

{
  "sourceId": "123",
  "sourceNo": "CASE-2026-001",
  "archiveName": "张三诉李四合同纠纷案",
  "archiveType": "LITIGATION",
  "clientName": "张三",
  "responsiblePerson": "王律师",
  "caseCloseDate": "2026-01-15",
  "retentionPeriod": "10_YEARS",
  "volumeCount": 2,
  "files": [
    {
      "fileName": "起诉状.pdf",
      "fileType": "application/pdf",
      "downloadUrl": "http://..."
    }
  ]
}
```

#### 健康检查
```
GET /api/open/health
```

### 管理接口

- `GET /api/archives` - 档案列表
- `GET /api/archives/{id}` - 档案详情
- `POST /api/archives/receive` - 接收档案
- `POST /api/archives/{id}/store` - 档案入库
- `GET /api/archives/statistics` - 档案统计

## 与律所系统对接

### 对接方式
律所管理系统通过HTTP API将归档档案推送到本系统：

1. 在律所系统配置档案管理系统的API地址和密钥
2. 律所系统在档案迁移时调用本系统的接收接口
3. 本系统接收档案数据并存储

### 数据格式
详见API文档：http://localhost:8090/api/doc.html

## 部署说明

### 端口配置
本系统使用以下端口（避免与律所主系统冲突）：
- 后端API：8090
- PostgreSQL：5433
- Redis：6380
- MinIO：9002（API）、9003（控制台）
- 前端：3001

### 环境变量
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/archive_system
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
MINIO_ENDPOINT=http://localhost:9002
```

## 分支管理

本系统作为独立分支 `feature/archive-management-system` 开发，与以下分支并存且永不合并：
- `main` - 律所管理系统主分支
- `feature/client-service-system` - 客户服务系统分支

## License

私有项目，未经授权不得使用。
