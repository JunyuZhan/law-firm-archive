# 运维手册

本手册面向系统管理员，介绍系统部署、配置、备份、监控和安全运维。

## 系统架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Nginx     │────▶│   前端      │     │   后端      │
│  (反向代理)  │     │  (Vue3)     │     │(Spring Boot)│
└─────────────┘     └─────────────┘     └─────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
        ┌─────▼─────┐                 ┌───────▼───────┐               ┌───────▼───────┐
        │ PostgreSQL │                 │     Redis     │               │     MinIO     │
        │  (数据库)   │                 │    (缓存)     │               │   (文件存储)   │
        └───────────┘                 └───────────────┘               └───────────────┘
```

## 技术栈

| 组件          | 版本   | 说明             |
| ------------- | ------ | ---------------- |
| JDK           | 17+    | 后端运行环境     |
| Node.js       | 18+    | 前端构建         |
| PostgreSQL    | 14+    | 数据库           |
| Redis         | 6+     | 缓存             |
| MinIO         | 最新版 | 文件存储         |
| Elasticsearch | 8.x    | 全文搜索（可选） |

## 部署形态一览

- 单机一键部署：适合小型律所或测试环境，使用 `./scripts/deploy.sh --quick` 一键拉起所有服务
- NAS 存储分离：应用服务器 + NAS 存储，提高数据安全性，参考 `docker/DEPLOY-NAS.md`
- Docker Swarm 分布式：多节点高可用与水平扩展，参考 `docker/DEPLOY-SWARM.md`
- MinIO 分布式存储集群：文件数据采用纠删码冗余，参考 `docker/DATA-SECURITY.md`

## 端口说明

| 服务       | 端口      | 说明     |
| ---------- | --------- | -------- |
| 前端       | 5173      | 开发环境 |
| 后端       | 5666      | API服务  |
| PostgreSQL | 5432      | 数据库   |
| Redis      | 6379      | 缓存     |
| MinIO      | 9000/9001 | 文件存储 |

## 目录

- [部署指南](/guide/ops/deployment)
- [配置说明](/guide/ops/configuration)
- [备份恢复](/guide/ops/backup)
- [监控告警](/guide/ops/monitoring)
- [故障排查](/guide/ops/troubleshooting)
- [数据安全（高级）](file:///Users/apple/Documents/Project/law-firm/docker/DATA-SECURITY.md)
