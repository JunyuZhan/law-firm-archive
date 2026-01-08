# 律师事务所管理系统 - 生产环境部署指南

> 📖 **分布式部署**：如需多节点集群部署，请参阅 [DEPLOY-SWARM.md](./DEPLOY-SWARM.md)

## 前置要求

- Docker 24.0+
- Docker Compose 2.20+
- 至少 4GB RAM
- 20GB 磁盘空间

## 一键部署（推荐）

```bash
# 1. 配置环境变量
cp docker/env.example docker/.env
vim docker/.env  # 修改密码和密钥

# 2. 运行部署脚本
./scripts/deploy.sh
```

## 手动部署

### 1. 配置环境变量

```bash
cd docker
cp env.example .env
```

编辑 `.env` 文件，设置以下必需的环境变量：

| 变量 | 说明 | 示例 |
|------|------|------|
| `DB_PASSWORD` | 数据库密码 | `SecurePassword123!` |
| `JWT_SECRET` | JWT 密钥（至少64字符） | `openssl rand -base64 64` 生成 |
| `MINIO_SECRET_KEY` | MinIO 密钥 | `MinioSecureKey123!` |

### 2. 启动服务

```bash
# 构建并启动所有服务
docker compose -f docker-compose.prod.yml up -d --build

# 查看日志
docker compose -f docker-compose.prod.yml logs -f
```

### 3. 验证部署

```bash
# 检查服务状态
docker compose -f docker-compose.prod.yml ps

# 检查后端健康状态
curl http://localhost/api/actuator/health
```

## 服务列表

| 服务 | 容器名称 | 端口 | 说明 |
|------|----------|------|------|
| frontend | law-firm-frontend | 80 | 前端 + Nginx |
| backend | law-firm-backend | - | 后端 API（内部） |
| postgres | law-firm-postgres | - | PostgreSQL 数据库 |
| redis | law-firm-redis | - | Redis 缓存 |
| minio | law-firm-minio | 9001 | 对象存储控制台 |
| paddle-ocr | law-firm-ocr | - | OCR 服务 |
| onlyoffice | law-firm-onlyoffice | - | 文档预览服务 |

## 常用命令

```bash
# 停止所有服务
docker compose -f docker-compose.prod.yml down

# 重启单个服务
docker compose -f docker-compose.prod.yml restart backend

# 查看日志
docker compose -f docker-compose.prod.yml logs -f backend

# 进入容器
docker exec -it law-firm-backend sh

# 数据库备份
docker exec law-firm-postgres pg_dump -U law_admin law_firm > backup.sql

# 数据库恢复
docker exec -i law-firm-postgres psql -U law_admin law_firm < backup.sql
```

## 数据持久化

以下目录需要备份：

- `postgres_data` - 数据库数据
- `redis_data` - Redis 数据
- `minio_data` - 文件存储
- `onlyoffice_data` - OnlyOffice 数据

## 故障排除

### 后端无法连接数据库

```bash
# 检查数据库容器状态
docker logs law-firm-postgres

# 测试连接
docker exec law-firm-postgres pg_isready -U law_admin -d law_firm
```

### 前端 API 调用失败

```bash
# 检查 Nginx 日志
docker logs law-firm-frontend

# 检查后端健康状态
docker exec law-firm-frontend curl http://backend:8080/api/actuator/health
```

## 安全建议

1. 修改所有默认密码
2. 配置防火墙，仅开放必要端口（80, 443）
3. 启用 HTTPS（使用反向代理如 Traefik 或外部负载均衡器）
4. 定期备份数据库
5. 监控日志和资源使用情况

