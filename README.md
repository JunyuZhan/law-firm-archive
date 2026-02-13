# 档案管理系统

> 分支：`feature/archive-management-system`

独立部署的电子档案管理系统，用于接收律所管理系统归档的电子档案，提供专业的档案全生命周期管理服务。

## 项目结构

```
archive-system/
├── backend/             # 后端服务 (Spring Boot 3 + JDK 21)
├── frontend/            # 前端应用 (Vue 3 + Element Plus)
├── docker/              # Docker 配置
├── scripts/             # 数据库脚本
└── docs/                # 对接文档
```

## 快速开始

```bash
cd archive-system/docker
docker-compose up -d
```

- 后端：http://localhost:8090
- 前端：http://localhost:3001
- API 文档：http://localhost:8090/api/doc.html

## 分支说明

本仓库采用多分支独立开发模式，各分支独立部署、永不合并：

| 分支 | 说明 | 端口 |
|------|------|------|
| `main` | 律所管理系统 | 8080/3000 |
| `feature/client-service-system` | 客户服务系统 | 8081/3001 |
| `feature/archive-management-system` | 档案管理系统（当前） | 8090/3001 |

## 系统对接

档案管理系统通过 API 接收律所主系统推送的归档档案：

```
律所管理系统 (main)  ──POST /api/open/archive/receive──>  档案管理系统 (本分支)
                     <────── 回调通知 ──────
```

详见：[对接指南](archive-system/docs/档案系统对接指南.md)

## 文档

- [对接指南](archive-system/docs/档案系统对接指南.md)
- [API 文档](archive-system/docs/API对接指南.md)
- [项目详情](archive-system/README.md)
