# Docker Compose 配置示例

此目录包含特定场景的 Docker Compose 配置文件。

## 文件说明

| 文件 | 用途 |
|------|------|
| `docker-compose.env-vars.yml` | 使用环境变量方式（传统，`docker inspect` 可见密钥） |
| `docker-compose.swarm.yml` | Docker Swarm 集群部署 |
| `docker-compose.master-slave.yml` | 主从高可用架构 |

## 推荐配置

**日常使用请直接使用上级目录的配置文件：**

```bash
# 开发环境
docker compose -f docker/docker-compose.dev.yml up -d

# 生产环境（默认，安全）
docker compose -f docker/docker-compose.yml up -d
```

## 使用示例配置

```bash
# 使用环境变量方式（不推荐生产环境）
docker compose --env-file .env -f docker/examples/docker-compose.env-vars.yml up -d

# Swarm 集群部署
docker stack deploy -c docker/examples/docker-compose.swarm.yml law-firm

# 主从高可用
docker compose -f docker/examples/docker-compose.master-slave.yml up -d
```
