# 部署脚本使用指南

本文档介绍项目中的一键部署脚本和检查脚本的使用方法。

## 📋 脚本概览

| 脚本 | 功能 | 用法 |
|------|------|------|
| `scripts/deploy.sh` | 一键部署脚本（引导式） | `./scripts/deploy.sh` |
| `scripts/pre-deploy-check.sh` | 部署前统一检查 | `./scripts/pre-deploy-check.sh` |
| `scripts/check-production-ready.sh` | 生产环境检查（旧版） | `./scripts/check-production-ready.sh` |
| `scripts/security-check.sh` | 安全检查 | `./scripts/security-check.sh` |
| `scripts/env-start.sh` | 环境启动 | `./scripts/env-start.sh [dev\|test\|prod]` |
| `scripts/env-stop.sh` | 环境停止 | `./scripts/env-stop.sh [dev\|test\|prod]` |

---

## 🚀 一键部署

### 快速部署（推荐）

```bash
# 1. 运行部署前检查（可选但推荐）
./scripts/pre-deploy-check.sh

# 2. 一键部署（引导式）
./scripts/deploy.sh
```

部署脚本会自动：
- ✅ 检查 Docker 环境
- ✅ 创建或验证 `.env` 配置文件
- ✅ 自动生成安全密钥（首次部署）
- ✅ 运行生产环境检查
- ✅ 构建 Docker 镜像
- ✅ 启动所有服务

### 快速部署（非交互式）

```bash
# 快速单机部署，跳过引导
./scripts/deploy.sh --quick

# 快速部署并初始化示例数据
./scripts/deploy.sh --quick --with-demo

# 跳过检查直接部署
./scripts/deploy.sh --quick --skip-check
```

### 部署模式选择

部署脚本支持多种部署模式：

1. **单机部署**（推荐小型律所）
   ```bash
   ./scripts/deploy.sh --mode=standalone
   ```

2. **NAS 存储分离部署**（推荐中型律所）
   ```bash
   ./scripts/deploy.sh --mode=nas --nas-ip=192.168.1.100
   ```

3. **Docker Swarm 分布式部署**（推荐大型律所）
   ```bash
   ./scripts/deploy.sh --mode=swarm
   ```

4. **MinIO 分布式存储集群**（企业级）
   ```bash
   ./scripts/deploy.sh --mode=minio-cluster
   ```

---

## ✅ 部署前检查

### 统一检查脚本（推荐）

```bash
./scripts/pre-deploy-check.sh
```

**检查项目**：
- ✅ Docker 环境检查
- ✅ 环境变量配置检查
- ✅ 配置文件检查
- ✅ 数据库初始化脚本检查
- ✅ 备份配置检查
- ✅ 敏感信息泄露检查
- ✅ 部署文档检查
- ✅ 系统资源检查

**输出示例**：
```
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║    生产环境部署前检查                                        ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝

【1/8】检查 Docker 环境...
  ✓ Docker 已安装 (24.0.7)
  ✓ Docker Compose 已安装
  ✓ Docker 服务运行正常

【2/8】检查环境变量配置...
  ✓ 找到 .env 文件: .env
  ✓ JWT_SECRET 已配置（64 字符）
  ✓ DB_PASSWORD 已配置（16 字符）
  ...
```

### 其他检查脚本

#### 生产环境检查（旧版）

```bash
./scripts/check-production-ready.sh
```

检查 10 个关键项，包括：
- 环境变量配置
- 应用配置文件
- Docker 配置
- 数据库初始化脚本
- 备份配置
- 安全修复
- 资源管理
- 依赖版本
- 部署文档
- 敏感信息

#### 安全检查

```bash
./scripts/security-check.sh
```

专门检查安全相关配置：
- 密钥和密码配置
- 敏感文件保护
- Docker 配置安全
- SSL/TLS 配置

---

## 📝 环境变量配置

### ✅ 自动配置（推荐，一键部署脚本会自动处理）

**一键部署脚本会自动生成所有安全密钥，无需手动配置！**

```bash
# 运行一键部署脚本，会自动：
# 1. 检测 .env 文件是否存在
# 2. 如果不存在，从 docker/env.example 复制
# 3. 自动生成所有安全密钥并更新到 .env 文件
./scripts/deploy.sh
```

**自动生成的密钥包括**：
- ✅ `JWT_SECRET` - JWT 认证密钥（32字符 base64）
- ✅ `DB_PASSWORD` - 数据库密码（16字符 base64）
- ✅ `MINIO_ACCESS_KEY` - MinIO 访问密钥（随机生成）
- ✅ `MINIO_SECRET_KEY` - MinIO 秘密密钥（16字符 base64）
- ✅ `REDIS_PASSWORD` - Redis 密码（16字符 base64）
- ✅ `ONLYOFFICE_JWT_SECRET` - OnlyOffice JWT 密钥（64字符 hex）
- ✅ `OCR_API_KEY` - OCR API 密钥（64字符 hex）
- ✅ `DOCS_PASSWORD` - 文档站点密码（16字符 base64）

**首次部署时的输出示例**：
```
⚠ 未找到 .env 文件，正在自动创建...
ℹ 首次部署，自动生成安全密钥...
  ✅ JWT_SECRET
  ✅ DB_PASSWORD
  ✅ MINIO_ACCESS_KEY
  ✅ MINIO_SECRET_KEY
  ✅ REDIS_PASSWORD
  ✅ ONLYOFFICE_JWT_SECRET
  ✅ ONLYOFFICE_JWT_ENABLED=true
  ✅ OCR_API_KEY
  ✅ DOCS_PASSWORD

✓ 安全密钥已保存到 .env
⚠ 请妥善保管此文件！
```

### 📝 手动配置（可选）

如果需要手动配置或修改密钥：

```bash
# 1. 复制环境变量模板
cp docker/env.example .env

# 2. 编辑配置文件
vim .env

# 3. 修改需要的配置项
```

**注意**：如果 `.env` 文件已存在，部署脚本会：
- ✅ 检查是否有不安全的默认值
- ✅ 自动补充缺失的密钥（如 OnlyOffice JWT）
- ✅ 如果发现不安全配置，会提示是否继续

### 🔑 手动生成安全密钥（参考）

如果需要手动生成密钥（通常不需要，脚本会自动生成）：

```bash
# JWT 密钥（32字符 base64，脚本会自动生成）
openssl rand -base64 32

# 数据库密码（16字符 base64，脚本会自动生成）
openssl rand -base64 16

# OnlyOffice JWT 密钥（64字符 hex，脚本会自动生成）
openssl rand -hex 32
```

---

## 🔧 环境管理

### 启动环境

```bash
# 开发环境
./scripts/env-start.sh dev

# 开发环境（全量，包含 OnlyOffice、OCR）
./scripts/env-start.sh dev --full

# 测试环境
./scripts/env-start.sh test

# 生产环境
./scripts/env-start.sh prod
```

### 停止环境

```bash
# 停止开发环境
./scripts/env-stop.sh dev

# 停止测试环境
./scripts/env-stop.sh test

# 停止生产环境
./scripts/env-stop.sh prod
```

### 重置环境

```bash
# 重置开发环境（删除所有数据）
./scripts/env-reset.sh dev

# ⚠️ 重置生产环境（危险操作）
./scripts/env-reset.sh prod
```

---

## 📊 部署后验证

### 检查服务状态

```bash
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml ps
```

### 检查健康状态

```bash
# 后端健康检查
curl http://localhost/api/actuator/health

# 应返回: {"status":"UP"}
```

### 查看日志

```bash
# 查看所有服务日志
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml logs -f

# 查看特定服务日志
docker compose --env-file ../.env -f docker-compose.prod.yml logs -f backend
```

### 访问地址

部署成功后，可以通过以下地址访问：

- 🌐 **主应用**: http://localhost
- 📚 **文档站点**: http://localhost/docs/
- 🔧 **API 地址**: http://localhost/api
- 📦 **MinIO 控制台**: http://localhost:9001
- 📊 **Prometheus**: http://localhost:9090
- 📈 **Grafana**: http://localhost:3000

### 默认账号

**主应用账号**（密码统一为 `admin123`）：
- `admin` - 系统管理员
- `director` - 主任
- `lawyer1` - 律师
- `leader` - 团队负责人
- `finance` - 财务
- `staff` - 员工
- `trainee` - 实习生

**文档站点账号**：
- 用户名：`admin`（或查看 `.env` 中的 `DOCS_USERNAME`）
- 密码：查看 `.env` 文件中的 `DOCS_PASSWORD`

---

## 🐛 常见问题

### 1. 部署脚本找不到 .env 文件

**问题**: `未找到 .env 文件`

**解决**:
```bash
# 复制环境变量模板
cp docker/env.example .env

# 或运行部署脚本，会自动创建
./scripts/deploy.sh
```

### 2. Docker 服务未运行

**问题**: `Docker 服务未运行`

**解决**:
```bash
# Linux
sudo systemctl start docker

# macOS
open -a Docker
```

### 3. 端口被占用

**问题**: `端口已被占用`

**解决**:
```bash
# 检查端口占用
sudo lsof -i :80
sudo lsof -i :8080

# 停止占用端口的服务或修改 docker-compose.prod.yml 中的端口映射
```

### 4. 构建失败

**问题**: `Docker 镜像构建失败`

**解决**:
```bash
# 清理 Docker 缓存
docker system prune -a

# 重新构建
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml build --no-cache
```

### 5. 数据库连接失败

**问题**: `数据库连接失败`

**解决**:
```bash
# 检查数据库容器状态
docker ps | grep postgres

# 检查数据库日志
docker logs law-firm-postgres

# 验证环境变量
cat .env | grep DB_
```

---

## 📚 相关文档

- [生产环境部署检查清单](./PRODUCTION_DEPLOYMENT_CHECKLIST.md)
- [生产环境快速部署指南](./PRODUCTION_QUICK_START.md)
- [生产环境配置检查报告](./PRODUCTION_CONFIG_CHECK_REPORT.md)
- [环境配置统一说明](./ENVIRONMENT_CONFIGURATION.md)

---

## 🔗 快速链接

- **一键部署**: `./scripts/deploy.sh`
- **部署前检查**: `./scripts/pre-deploy-check.sh`
- **环境启动**: `./scripts/env-start.sh prod`
- **查看日志**: `cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml logs -f`
