# 档案管理系统

> 分支：`feature/archive-management-system`

独立部署的电子档案管理系统，用于接收律所管理系统归档的电子档案，提供专业的档案全生命周期管理服务。

## 项目结构

```
├── archive-system/          # 📦 档案管理系统（开发中）
│   ├── backend/             # 后端服务
│   ├── frontend/            # 前端应用
│   ├── docker/              # Docker配置
│   ├── scripts/             # 数据库脚本
│   └── TODO.md              # 开发计划
│
├── backend/                 # 📚 主系统后端（仅参考）
└── scripts/                 # 📚 主系统脚本（仅参考）
    └── init-db/             # 数据库初始化脚本
        ├── 07-archive-schema.sql   # 主系统档案模块（重要参考）
        └── ...
```

## 快速开始

```bash
cd archive-system/docker
docker-compose up -d
```

## 分支说明

本仓库采用多分支独立开发模式，各分支独立部署、永不合并：

| 分支 | 说明 | 端口 |
|------|------|------|
| `main` | 律所管理系统 | 8080/3000 |
| `feature/client-service-system` | 客户服务系统 | 8081/3001 |
| `feature/archive-management-system` | 档案管理系统（当前） | 8090/3002 |

## 系统对接

档案管理系统通过 API 接收律所主系统推送的归档档案：

```
律所管理系统 (main)  --[HTTP API]--> 档案管理系统 (本分支)
     ↓
POST /api/open/law-firm/archive/receive
```

## 文档

- [开发计划](archive-system/TODO.md)
- [项目详情](archive-system/README.md)
