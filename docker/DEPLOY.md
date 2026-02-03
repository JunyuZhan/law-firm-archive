# 律师事务所管理系统 - 生产环境部署指南

> 📖 **分布式部署**：如需多节点集群部署，请参阅 [DEPLOY-SWARM.md](./DEPLOY-SWARM.md)

## 系统分支说明

本项目包含两个独立的系统，分别在不同的 Git 分支：

| 分支 | 系统 | 说明 | 部署目录 |
|------|------|------|----------|
| `main` | 律所管理系统 | 律所内部使用的管理系统 | 项目根目录 |
| `feature/client-service-system` | 客户服务系统 | 面向客户的门户网站 | `client-service/` |

### 首次部署（克隆仓库）

```bash
# 部署律所管理系统（默认 main 分支）
git clone https://github.com/JunyuZhan/law-firm.git
cd law-firm
./scripts/deploy/deploy.sh

# 部署客户服务系统（指定分支）
git clone -b feature/client-service-system https://github.com/JunyuZhan/law-firm.git client-service
cd client-service/client-service
./deploy.sh
```

### 升级更新（已有仓库）

```bash
# 升级律所管理系统（推荐：使用升级脚本，自动备份）
cd law-firm
./scripts/ops/upgrade.sh

# 或手动升级
git pull origin main
./scripts/deploy/deploy.sh --quick

# 升级客户服务系统
cd client-service/client-service
git pull origin feature/client-service-system
./deploy.sh --quick
```

升级脚本选项：
- `./scripts/ops/upgrade.sh` - 交互式升级（带备份确认）
- `./scripts/ops/upgrade.sh --quick` - 快速升级（自动备份，跳过确认）
- `./scripts/ops/upgrade.sh --check` - 仅检查更新，不执行

> ⚠️ **注意**：两个系统是独立部署的，可以部署在同一台服务器的不同端口，或部署在不同服务器上。

## 前置要求

- Docker 24.0+
- Docker Compose 2.20+
- 至少 4GB RAM
- 20GB 磁盘空间

## 一键部署

```bash
# 在项目根目录执行
./scripts/deploy.sh
```

脚本会自动完成：
- ✅ 首次部署自动生成安全密钥（JWT、数据库密码、MinIO密钥）
- ✅ 构建前端应用（主应用 + 文档站点）
- ✅ 构建后端服务
- ✅ 启动所有 Docker 容器
- ✅ 初始化数据库和示例数据

## 手动部署（可选）

如果需要手动控制部署过程：

```bash
cd docker

# 1. 复制环境变量模板（首次部署）
cp env.example .env

# 2. 构建并启动
docker compose -f docker-compose.prod.yml up -d --build

# 3. 查看日志
docker compose -f docker-compose.prod.yml logs -f
```

## 服务列表

| 服务 | 容器名称 | 端口 | 说明 |
|------|----------|------|------|
| frontend | law-firm-frontend | 80 | 前端 + Nginx（包含主应用和文档站点） |
| backend | law-firm-backend | - | 后端 API（内部） |
| postgres | law-firm-postgres | - | PostgreSQL 数据库 |
| redis | law-firm-redis | - | Redis 缓存 |
| minio | law-firm-minio | 9001 | 对象存储控制台 |
| paddle-ocr | law-firm-ocr | - | OCR 服务 |
| onlyoffice | law-firm-onlyoffice | - | 文档预览服务 |

## 访问地址

部署完成后，可通过以下地址访问：

| 站点 | 地址 | 说明 |
|------|------|------|
| 主应用 | `http://localhost/` | 律师事务所管理系统主界面 |
| 文档站点 | `http://localhost/docs/` | 系统使用文档和 API 文档 |
| MinIO 控制台 | `http://localhost:9001/` | 对象存储管理界面 |

## 默认账号

所有账号密码统一为：`admin123`

| 用户名 | 角色 |
|--------|------|
| admin | 管理员 |
| director | 律所主任 |
| lawyer1 | 律师 |
| leader | 团队负责人 |
| finance | 财务人员 |
| staff | 行政人员 |
| trainee | 实习律师 |

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

