# 部署脚本总结

## ✅ 一键部署和检查脚本已就绪

项目已配置完整的一键部署和检查脚本，可以安全、便捷地部署到生产环境。

---

## 🚀 快速开始

### 方式一：一键部署（推荐）

```bash
# 1. 运行部署前检查（推荐）
./scripts/pre-deploy-check.sh

# 2. 一键部署（引导式）
./scripts/deploy.sh
```

### 方式二：快速部署（非交互）

```bash
# 快速单机部署
./scripts/deploy.sh --quick

# 快速部署并初始化示例数据
./scripts/deploy.sh --quick --with-demo
```

---

## 📋 可用脚本列表

### 1. 部署脚本

| 脚本 | 功能 | 用法 |
|------|------|------|
| `scripts/deploy.sh` | **一键部署脚本**（引导式，推荐） | `./scripts/deploy.sh` |
| `scripts/deploy-swarm.sh` | Docker Swarm 分布式部署 | `./scripts/deploy-swarm.sh` |

**特性**：
- ✅ 自动检查 Docker 环境
- ✅ 自动创建/验证 `.env` 配置文件
- ✅ 自动生成安全密钥（首次部署）
- ✅ 自动运行生产环境检查
- ✅ 支持多种部署模式（单机/NAS/Swarm/MinIO集群）
- ✅ 支持初始化示例数据

### 2. 检查脚本

| 脚本 | 功能 | 用法 |
|------|------|------|
| `scripts/pre-deploy-check.sh` | **统一部署前检查**（推荐） | `./scripts/pre-deploy-check.sh` |
| `scripts/check-production-ready.sh` | 生产环境检查（旧版） | `./scripts/check-production-ready.sh` |
| `scripts/security-check.sh` | 安全检查 | `./scripts/security-check.sh` |

**检查项**：
- ✅ Docker 环境检查
- ✅ 环境变量配置检查
- ✅ 配置文件检查
- ✅ 数据库初始化脚本检查
- ✅ 备份配置检查
- ✅ 敏感信息泄露检查
- ✅ 部署文档检查
- ✅ 系统资源检查

### 3. 环境管理脚本

| 脚本 | 功能 | 用法 |
|------|------|------|
| `scripts/env-start.sh` | 启动环境 | `./scripts/env-start.sh [dev\|test\|prod]` |
| `scripts/env-stop.sh` | 停止环境 | `./scripts/env-stop.sh [dev\|test\|prod]` |
| `scripts/env-reset.sh` | 重置环境 | `./scripts/env-reset.sh [dev\|test\|prod]` |

**支持的环境**：
- `dev` - 开发环境
- `test` - 测试环境
- `prod` - 生产环境

### 4. 其他脚本

| 脚本 | 功能 | 用法 |
|------|------|------|
| `scripts/backup.sh` | 数据库备份 | `./scripts/backup.sh` |
| `scripts/db-auto-backup.sh` | 自动备份 | `./scripts/db-auto-backup.sh` |
| `scripts/reset-db.sh` | 重置数据库 | `./scripts/reset-db.sh` |
| `scripts/init-demo-data.sh` | 初始化示例数据 | `./scripts/init-demo-data.sh` |

---

## 📝 部署流程

### 标准部署流程

```bash
# 1. 克隆项目
git clone https://github.com/JunyuZhan/law-firm.git
cd law-firm

# 2. 运行部署前检查（推荐）
./scripts/pre-deploy-check.sh

# 3. 一键部署
./scripts/deploy.sh

# 4. 等待服务启动（约1-2分钟）

# 5. 验证部署
curl http://localhost/api/actuator/health
# 应返回: {"status":"UP"}
```

### 快速部署流程

```bash
# 1. 克隆项目
git clone https://github.com/JunyuZhan/law-firm.git
cd law-firm

# 2. 快速部署（自动创建配置）
./scripts/deploy.sh --quick --with-demo

# 3. 访问 http://localhost
```

---

## 🔧 环境变量配置

### ✅ 自动配置（推荐，一键部署脚本会自动生成）

**一键部署脚本会自动生成所有安全密钥，无需手动配置！**

```bash
# 运行一键部署脚本，会自动：
# 1. 检测 .env 文件是否存在
# 2. 如果不存在，从 docker/env.example 复制
# 3. 自动生成所有安全密钥并更新到 .env 文件
./scripts/deploy.sh
```

**自动生成的密钥**：
- ✅ `JWT_SECRET` - JWT 认证密钥
- ✅ `DB_PASSWORD` - 数据库密码
- ✅ `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` - MinIO 密钥
- ✅ `REDIS_PASSWORD` - Redis 密码
- ✅ `ONLYOFFICE_JWT_SECRET` - OnlyOffice JWT 密钥
- ✅ `OCR_API_KEY` - OCR API 密钥
- ✅ `DOCS_PASSWORD` - 文档站点密码

**首次部署时会看到**：
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
```

**注意**：如果 `.env` 文件已存在，部署脚本会：
- ✅ 检查是否有不安全的默认值
- ✅ 自动补充缺失的密钥
- ✅ 如果发现不安全配置，会提示是否继续

---

## ✅ 部署后验证

### 1. 检查服务状态

```bash
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml ps
```

所有服务状态应为 `Up`。

### 2. 检查健康状态

```bash
curl http://localhost/api/actuator/health
# 应返回: {"status":"UP"}
```

### 3. 访问地址

- 🌐 **主应用**: http://localhost
- 📚 **文档站点**: http://localhost/docs/
- 🔧 **API 地址**: http://localhost/api
- 📦 **MinIO 控制台**: http://localhost:9001
- 📊 **Prometheus**: http://localhost:9090
- 📈 **Grafana**: http://localhost:3000

### 4. 默认账号

**主应用账号**（密码统一为 `admin123`）：
- `admin` - 系统管理员
- `director` - 主任
- `lawyer1` - 律师

**文档站点账号**：
- 用户名：`admin`（或查看 `.env` 中的 `DOCS_USERNAME`）
- 密码：查看 `.env` 文件中的 `DOCS_PASSWORD`

---

## 📚 相关文档

- [部署脚本使用指南](./DEPLOYMENT_SCRIPTS_GUIDE.md) - 详细的脚本使用说明
- [生产环境部署检查清单](./PRODUCTION_DEPLOYMENT_CHECKLIST.md) - 完整的检查清单
- [生产环境快速部署指南](./PRODUCTION_QUICK_START.md) - 快速部署指南
- [生产环境配置检查报告](./PRODUCTION_CONFIG_CHECK_REPORT.md) - 配置检查报告
- [环境配置统一说明](./ENVIRONMENT_CONFIGURATION.md) - 环境配置说明

---

## 🎯 总结

### ✅ 已完成的配置

1. **一键部署脚本** (`scripts/deploy.sh`)
   - ✅ 引导式部署界面
   - ✅ 自动环境检查
   - ✅ 自动密钥生成
   - ✅ 支持多种部署模式

2. **统一检查脚本** (`scripts/pre-deploy-check.sh`)
   - ✅ 8 项全面检查
   - ✅ Docker 环境检查
   - ✅ 配置验证
   - ✅ 安全检查

3. **环境变量模板** (`docker/env.example`)
   - ✅ 完整的配置模板
   - ✅ 详细的注释说明
   - ✅ 安全建议

4. **文档完善**
   - ✅ 部署脚本使用指南
   - ✅ 生产环境检查清单
   - ✅ 配置检查报告

### 🚀 使用建议

1. **首次部署**：使用 `./scripts/deploy.sh`（引导式）
2. **快速部署**：使用 `./scripts/deploy.sh --quick`
3. **部署前检查**：运行 `./scripts/pre-deploy-check.sh`
4. **环境管理**：使用 `scripts/env-*.sh` 脚本

---

## 🔗 快速链接

- **一键部署**: `./scripts/deploy.sh`
- **部署前检查**: `./scripts/pre-deploy-check.sh`
- **环境启动**: `./scripts/env-start.sh prod`
- **查看日志**: `cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml logs -f`
