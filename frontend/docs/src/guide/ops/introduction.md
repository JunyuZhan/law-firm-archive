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

## 端口说明（单端口架构）

系统采用**单端口架构**，仅暴露 HTTP（80）和 HTTPS（443）端口，其他服务通过 Nginx 路径代理访问。

### 暴露的端口

| 端口 | 服务 | 说明 |
|------|------|------|
| **80** | Frontend (Nginx) | HTTP 访问 |
| **443** | Frontend (Nginx) | HTTPS 访问 |

### 通过路径访问的服务

| 路径 | 服务 | 原端口 | 说明 |
|------|------|--------|------|
| `/minio/` | MinIO API | 9000 | 文件存储 API |
| `/minio-console/` | MinIO Console | 9001 | MinIO 管理控制台 |
| `/onlyoffice/` | OnlyOffice | 80 | 文档编辑服务 |
| `/prometheus/` | Prometheus | 9090 | 监控数据收集（可选） |
| `/grafana/` | Grafana | 3000 | 监控可视化（可选） |

### 不暴露端口的服务

| 服务 | 端口 | 访问方式 | 说明 |
|------|------|---------|------|
| PostgreSQL | 5432 | Docker 内部网络 | 通过 `docker exec` 维护 |
| Redis | 6379 | Docker 内部网络 | Docker 内部访问 |

> 📖 详细说明请参考 [单端口架构](/guide/ops/single-port-architecture)

## 目录

- [部署指南](/guide/ops/deployment) - 系统部署与环境配置
- [单端口架构](/guide/ops/single-port-architecture) - 单端口架构说明与配置
- [单端口架构迁移](/guide/ops/single-port-migration) - 从多端口迁移到单端口
- [部署检查清单](/guide/ops/deployment-checklist) - 部署前检查项
- [配置说明](/guide/ops/configuration) - 环境变量与应用配置
- [数据库维护](/guide/ops/database-maintenance) - 数据库访问与维护
- [备份恢复](/guide/ops/backup) - 数据库与文件备份策略
- [监控告警](/guide/ops/monitoring) - Prometheus + Grafana 监控
- [安全运维](/guide/ops/security) - 安全架构与防护措施
- [故障排查](/guide/ops/troubleshooting) - 常见问题与解决方案
