# 档案管理系统

> 分支：`feature/archive-management-system`

独立部署的电子档案管理系统，用于接收律所管理系统归档的电子档案，提供专业的档案全生命周期管理服务。

## 项目结构

```
archive-system/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── docker/           # Docker 配置
├── scripts/          # 数据库脚本
├── README.md         # 项目说明
└── TODO.md           # 开发计划
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

## 文档

- [开发计划](archive-system/TODO.md)
- [项目详情](archive-system/README.md)
