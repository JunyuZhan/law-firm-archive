# 部署脚本

部署相关的脚本集合，提供多种部署方式和部署前检查功能。

## 📋 脚本列表

| 脚本 | 说明 | 适用场景 |
|------|------|----------|
| `deploy.sh` | 一键 Docker 部署（推荐） | 单机部署、NAS存储分离、Swarm集群 |
| `deploy-swarm.sh` | Docker Swarm 集群部署 | 多节点高可用部署 |
| `deploy-to-server.sh` | 快速部署到服务器 | 从本地部署到远程服务器 |
| `force-update-server.sh` | 强制更新服务器代码 | 服务器代码同步 |
| `pre-deploy-check.sh` | 部署前统一检查（推荐） | 部署前环境检查 |
| `check-production-ready.sh` | 生产环境检查（旧版） | 生产环境检查（已弃用） |

---

## 🚀 deploy.sh - 一键部署

**推荐使用**：最常用的部署脚本，支持多种部署模式。

### 功能特点

- ✅ 引导式交互部署
- ✅ 支持单机、NAS、Swarm 多种模式
- ✅ 自动环境检查
- ✅ 自动构建 Docker 镜像
- ✅ 可选初始化示例数据
- ✅ 支持快速部署模式

### 使用方法

#### 方式1：交互式部署（推荐首次部署）

```bash
# 从项目根目录执行
./scripts/deploy.sh

# 或从 scripts/deploy/ 目录执行
cd scripts/deploy
./deploy.sh
```

脚本会引导你选择：
- 部署模式（单机/NAS/Swarm）
- 是否初始化示例数据
- 其他配置选项

#### 方式2：快速部署（跳过交互）

```bash
# 快速部署（单机模式，跳过所有交互）
./scripts/deploy.sh --quick

# 快速部署并初始化示例数据
./scripts/deploy.sh --quick --with-demo

# 指定部署模式
./scripts/deploy.sh --mode=standalone
./scripts/deploy.sh --mode=nas --nas-ip=192.168.1.100
./scripts/deploy.sh --mode=swarm
```

### 参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| `--quick` | 快速部署，跳过交互 | `./scripts/deploy.sh --quick` |
| `--mode=<模式>` | 指定部署模式 | `--mode=standalone` |
| `--nas-ip=<IP>` | NAS 服务器 IP | `--nas-ip=192.168.1.100` |
| `--nas-path=<路径>` | NAS 挂载路径 | `--nas-path=/mnt/nas` |
| `--with-demo` | 初始化示例数据 | `./scripts/deploy.sh --quick --with-demo` |
| `--no-cache` | 强制重新构建镜像 | `./scripts/deploy.sh --no-cache` |
| `--skip-check` | 跳过部署前检查 | `./scripts/deploy.sh --skip-check` |

### 部署模式

#### 1. 单机部署（standalone）

适合小型律所或测试环境，所有服务运行在一台服务器上。

```bash
./scripts/deploy.sh --mode=standalone
```

#### 2. NAS 存储分离（nas）

应用服务器 + NAS 存储，提高数据安全性。

```bash
./scripts/deploy.sh --mode=nas --nas-ip=192.168.1.100
```

#### 3. Docker Swarm 集群（swarm）

多节点高可用与水平扩展。

```bash
./scripts/deploy.sh --mode=swarm
```

### 部署流程

1. **环境检查**：检查 Docker、环境变量、配置文件等
2. **构建镜像**：构建前端、后端、OCR 等 Docker 镜像
3. **启动服务**：启动所有 Docker 容器
4. **等待就绪**：等待服务启动完成
5. **初始化数据**（可选）：初始化示例数据

### 注意事项

- ⚠️ 首次部署前需要配置 `.env` 文件
- ⚠️ 确保 Docker 和 Docker Compose 已安装
- ⚠️ 确保有足够的磁盘空间（建议至少 20GB）
- ⚠️ 生产环境部署前建议先运行 `pre-deploy-check.sh`

---

## 🐳 deploy-swarm.sh - Swarm 集群部署

Docker Swarm 分布式部署脚本，支持多节点高可用。

### 使用方法

#### 1. 初始化 Swarm 集群（Manager 节点）

```bash
# 在 Manager 节点上执行
./scripts/deploy/deploy-swarm.sh init
```

#### 2. 加入集群（Worker 节点）

```bash
# 在 Worker 节点上执行
./scripts/deploy/deploy-swarm.sh join <token> <manager-ip>

# 示例
./scripts/deploy/deploy-swarm.sh join SWMTKN-1-xxx 192.168.1.100
```

#### 3. 部署服务栈

```bash
# 在 Manager 节点上执行
./scripts/deploy/deploy-swarm.sh deploy
```

#### 4. 扩缩容

```bash
# 扩展后端服务到 3 个副本
./scripts/deploy/deploy-swarm.sh scale backend 3

# 扩展前端服务到 2 个副本
./scripts/deploy/deploy-swarm.sh scale frontend 2
```

#### 5. 查看状态

```bash
# 查看服务状态
./scripts/deploy/deploy-swarm.sh status

# 查看特定服务日志
./scripts/deploy/deploy-swarm.sh logs backend
```

#### 6. 更新服务

```bash
# 更新服务（重新构建镜像并部署）
./scripts/deploy/deploy-swarm.sh update
```

#### 7. 删除服务栈

```bash
# 删除整个服务栈
./scripts/deploy/deploy-swarm.sh remove
```

### 命令列表

| 命令 | 说明 | 执行节点 |
|------|------|----------|
| `init` | 初始化 Swarm 集群 | Manager |
| `join <token> <ip>` | 加入集群 | Worker |
| `deploy` | 部署服务栈 | Manager |
| `update` | 更新服务 | Manager |
| `scale <service> <replicas>` | 扩缩容 | Manager |
| `status` | 查看状态 | Manager |
| `logs <service>` | 查看日志 | Manager |
| `remove` | 删除服务栈 | Manager |

### 注意事项

- ⚠️ Manager 节点需要至少 3 个（生产环境推荐）
- ⚠️ 所有节点需要网络互通
- ⚠️ 确保所有节点已安装 Docker
- ⚠️ 建议使用负载均衡器（如 Nginx）作为入口

---

## 🌐 deploy-to-server.sh - 快速部署到服务器

从本地快速部署到远程服务器，使用 rsync 同步代码。

### 使用方法

```bash
# 基本用法
./scripts/deploy/deploy-to-server.sh <服务器IP> [用户名]

# 示例
./scripts/deploy/deploy-to-server.sh 192.168.1.100 root
./scripts/deploy/deploy-to-server.sh 192.168.1.100 admin
```

### 功能特点

- ✅ 自动检查 SSH 连接
- ✅ 使用 rsync 同步代码（排除 node_modules、.git 等）
- ✅ 自动检查并安装 Docker
- ✅ 自动配置环境变量
- ✅ 自动配置 SSL 证书
- ✅ 自动执行部署

### 部署流程

1. **检查 SSH 连接**：验证能否连接到服务器
2. **上传代码**：使用 rsync 同步代码到服务器
3. **检查 Docker**：检查并安装 Docker（如需要）
4. **配置环境**：创建 `.env` 文件（如不存在）
5. **配置 SSL**：配置 SSL 证书（如存在）
6. **执行部署**：在服务器上执行部署

### 同步排除项

脚本会自动排除以下文件和目录：
- `node_modules/`
- `.git/`
- `backend/target/`
- `frontend/dist/`
- `frontend/node_modules/`
- `.env`
- `docker/.env`
- `*.log`

### 注意事项

- ⚠️ 需要配置 SSH 密钥认证（推荐）
- ⚠️ 确保服务器可以访问 GitHub（用于拉取代码）
- ⚠️ 首次部署后需要手动编辑 `.env` 文件配置密码
- ⚠️ 建议在服务器上配置防火墙规则

---

## 🔄 force-update-server.sh - 强制更新服务器代码

强制更新服务器代码，使用 git pull 同步最新代码。

### 使用方法

#### 方式1：在服务器上直接运行（推荐）

```bash
# SSH 到服务器
ssh user@your-server-ip

# 进入项目目录
cd /opt/law-firm

# 执行更新
./scripts/deploy/force-update-server.sh
```

#### 方式2：从本地SSH到服务器执行

```bash
# 从本地执行
./scripts/deploy/force-update-server.sh <服务器IP> [用户名]

# 示例
./scripts/deploy/force-update-server.sh 192.168.1.100 root

# 指定项目路径
./scripts/deploy/force-update-server.sh 192.168.1.100 root /opt/law-firm
```

### 功能特点

- ✅ 检查当前 Git 状态
- ✅ 检测未提交的更改（会警告）
- ✅ 获取远程更新
- ✅ 强制重置到远程分支
- ✅ 可选清理未跟踪的文件

### 执行流程

1. **检查状态**：显示当前分支和提交信息
2. **检测更改**：检测是否有未提交的更改
3. **获取更新**：执行 `git fetch origin`
4. **显示差异**：显示本地和远程的差异
5. **强制重置**：执行 `git reset --hard origin/main`
6. **清理文件**（可选）：清理未跟踪的文件

### ⚠️ 重要警告

**此脚本会强制重置到远程分支，丢弃所有本地未提交的更改！**

执行前请确保：
- ✅ 已备份重要数据
- ✅ 已提交或暂存重要更改
- ✅ 了解重置操作的影响

### 常见使用场景

#### 场景1：代码更新后重新部署

```bash
# 1. 更新代码
./scripts/deploy/force-update-server.sh 192.168.1.100 root

# 2. 重新构建并启动
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml up -d --build
```

#### 场景2：回滚到最新版本

```bash
# 如果服务器代码被意外修改，可以强制回滚
./scripts/deploy/force-update-server.sh
```

---

## ✅ pre-deploy-check.sh - 部署前检查

**推荐使用**：部署前统一检查脚本，整合所有检查功能。

### 使用方法

```bash
# 从项目根目录执行
./scripts/deploy/pre-deploy-check.sh

# 或从 scripts/deploy/ 目录执行
cd scripts/deploy
./pre-deploy-check.sh
```

### 检查项

脚本会检查以下 8 个方面：

1. **Docker 环境检查**
   - Docker 是否安装
   - Docker Compose 是否安装
   - Docker 服务是否运行

2. **环境变量配置**
   - `.env` 文件是否存在
   - 必需的环境变量是否配置
   - 密码是否已修改（不是默认值）

3. **配置文件完整性**
   - `docker-compose.prod.yml` 是否存在
   - Nginx 配置文件是否存在
   - SSL 证书配置（如启用）

4. **数据库初始化脚本**
   - SQL 初始化脚本是否存在
   - SQL 文件数量统计

5. **备份配置**
   - 备份脚本是否存在
   - 备份目录是否可写

6. **敏感信息泄露**
   - 检查配置文件中是否有敏感信息
   - 检查是否有硬编码的密码

7. **系统资源**
   - 磁盘空间是否充足
   - 内存是否充足

8. **网络端口**
   - 端口是否被占用
   - 防火墙配置检查

### 输出说明

- ✅ **绿色**：检查通过
- ⚠️ **黄色**：警告（可以继续，但建议修复）
- ❌ **红色**：错误（必须修复后才能部署）

### 检查结果

- **全部通过**：可以部署生产环境
- **有警告**：建议修复后再部署
- **有错误**：必须修复后才能部署

### 示例输出

```
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║    律师事务所管理系统 - 部署前检查                              ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝

【1/8】检查 Docker 环境...
✓ Docker 已安装: Docker version 24.0.0
✓ Docker Compose 已安装: Docker Compose version v2.20.0
✓ Docker 服务运行正常

【2/8】检查环境变量配置...
✓ .env 文件存在
✓ 必需的环境变量已配置
⚠ 部分密码仍使用默认值，建议修改

...

✓ 所有检查通过，可以部署生产环境

下一步：
  运行部署脚本: ./scripts/deploy.sh 或 ./scripts/deploy/deploy.sh
```

---

## 📝 check-production-ready.sh - 生产环境检查（旧版）

**已弃用**：建议使用 `pre-deploy-check.sh` 替代。

此脚本是旧版的生产环境检查脚本，功能已被 `pre-deploy-check.sh` 整合。

---

## 🔄 完整部署流程

### 首次部署

```bash
# 1. 部署前检查
./scripts/deploy/pre-deploy-check.sh

# 2. 一键部署
./scripts/deploy.sh

# 或快速部署
./scripts/deploy.sh --quick
```

### 更新部署

```bash
# 1. 更新代码（在服务器上）
cd /opt/law-firm
./scripts/deploy/force-update-server.sh

# 2. 重新构建并启动
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml up -d --build
```

### 远程部署

```bash
# 从本地部署到远程服务器
./scripts/deploy/deploy-to-server.sh 192.168.1.100 root
```

---

## ❓ 常见问题

### Q: 部署失败怎么办？

A: 
1. 检查部署前检查脚本的输出
2. 查看 Docker 日志：`docker compose logs`
3. 检查环境变量配置
4. 确保有足够的磁盘空间和内存

### Q: 如何更新已部署的服务？

A: 
```bash
# 方式1: 更新代码后重新构建
cd /opt/law-firm
./scripts/deploy/force-update-server.sh
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml up -d --build

# 方式2: 使用部署脚本（会重新构建）
./scripts/deploy.sh --quick
```

### Q: 如何查看服务状态？

A:
```bash
# 查看所有服务状态
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml ps

# 查看特定服务日志
docker compose --env-file ../.env -f docker-compose.prod.yml logs -f backend
```

### Q: 如何停止服务？

A:
```bash
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml down

# 停止并删除数据卷（谨慎使用）
docker compose --env-file ../.env -f docker-compose.prod.yml down -v
```

### Q: 部署后无法访问怎么办？

A:
1. 检查服务是否正常运行：`docker compose ps`
2. 检查端口是否被占用：`netstat -tulpn | grep 80`
3. 检查防火墙规则
4. 查看 Nginx 日志：`docker compose logs frontend`

---

## 📚 相关文档

- [主脚本说明](../README.md)
- [运维脚本说明](../ops/README.md)
- [部署检查清单](../../../docs/PRODUCTION_DEPLOYMENT_CHECKLIST.md)
- [单端口架构说明](../../../frontend/docs/src/guide/ops/single-port-architecture.md)

---

**最后更新**: 2026-01-31
