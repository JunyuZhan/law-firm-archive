# 部署脚本

部署相关的脚本集合。

## 📋 脚本列表

| 脚本 | 说明 | 用法 |
|------|------|------|
| `deploy.sh` | 一键 Docker 部署（推荐） | `./scripts/deploy.sh` |
| `deploy-swarm.sh` | Docker Swarm 集群部署 | `./scripts/deploy/deploy-swarm.sh init` |
| `deploy-to-server.sh` | 快速部署到服务器 | `./scripts/deploy/deploy-to-server.sh <IP> [用户]` |
| `force-update-server.sh` | 强制更新服务器代码 | `./scripts/deploy/force-update-server.sh [IP] [用户]` |
| `pre-deploy-check.sh` | 部署前统一检查（推荐） | `./scripts/deploy/pre-deploy-check.sh` |
| `check-production-ready.sh` | 生产环境检查（旧版） | 已弃用，使用 `pre-deploy-check.sh` |

---

## 🚀 deploy.sh - 一键部署

### 快速开始

```bash
# 交互式部署（推荐首次部署）
./scripts/deploy.sh

# 快速部署（跳过交互）
./scripts/deploy.sh --quick

# 快速部署并初始化示例数据
./scripts/deploy.sh --quick --with-demo
```

### 常用参数

```bash
--quick                    # 快速部署，跳过交互
--mode=standalone          # 单机部署
--mode=nas --nas-ip=192.168.1.100  # NAS 存储分离
--mode=swarm               # Swarm 集群部署
--with-demo                # 初始化示例数据
--no-cache                 # 强制重新构建镜像
```

### 部署模式

- **单机部署**：适合小型律所或测试环境
- **NAS 存储分离**：应用服务器 + NAS 存储
- **Swarm 集群**：多节点高可用部署

---

## 🐳 deploy-swarm.sh - Swarm 集群部署

### 常用命令

```bash
# 初始化集群（Manager 节点）
./scripts/deploy/deploy-swarm.sh init

# 加入集群（Worker 节点）
./scripts/deploy/deploy-swarm.sh join <token> <manager-ip>

# 部署服务栈
./scripts/deploy/deploy-swarm.sh deploy

# 扩缩容
./scripts/deploy/deploy-swarm.sh scale backend 3

# 查看状态
./scripts/deploy/deploy-swarm.sh status

# 查看日志
./scripts/deploy/deploy-swarm.sh logs backend
```

---

## 🌐 deploy-to-server.sh - 远程部署

从本地快速部署到远程服务器。

```bash
# 基本用法
./scripts/deploy/deploy-to-server.sh <服务器IP> [用户名]

# 示例
./scripts/deploy/deploy-to-server.sh 192.168.1.100 root
```

**功能**：
- 自动检查 SSH 连接
- 使用 rsync 同步代码
- 自动检查并安装 Docker
- 自动执行部署

---

## 🔄 force-update-server.sh - 更新服务器代码

强制更新服务器代码到最新版本。

### 方式1：在服务器上运行

```bash
cd /opt/law-firm
./scripts/deploy/force-update-server.sh
```

### 方式2：从本地执行

```bash
./scripts/deploy/force-update-server.sh <服务器IP> [用户名]
```

**⚠️ 警告**：会丢弃所有本地未提交的更改！

---

## ✅ pre-deploy-check.sh - 部署前检查

部署前统一检查脚本，检查 8 个方面：

1. Docker 环境
2. 环境变量配置
3. 配置文件完整性
4. 数据库初始化脚本
5. 备份配置
6. 敏感信息泄露
7. 系统资源
8. 网络端口

```bash
./scripts/deploy/pre-deploy-check.sh
```

**输出说明**：
- ✅ 绿色：检查通过
- ⚠️ 黄色：警告（建议修复）
- ❌ 红色：错误（必须修复）

---

## 🔄 常用部署流程

### 首次部署

```bash
# 1. 部署前检查
./scripts/deploy/pre-deploy-check.sh

# 2. 一键部署
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
./scripts/deploy/deploy-to-server.sh 192.168.1.100 root
```

---

## ❓ 常见问题

**Q: 部署失败怎么办？**

```bash
# 检查部署前检查脚本的输出
./scripts/deploy/pre-deploy-check.sh

# 查看 Docker 日志
docker compose logs
```

**Q: 如何更新已部署的服务？**

```bash
# 更新代码后重新构建
docker compose up -d --build
```

**Q: 如何查看服务状态？**

```bash
# 查看所有服务状态
docker compose ps

# 查看特定服务日志
docker compose logs -f <服务名>
```

**Q: 如何停止服务？**

```bash
# 停止服务
docker compose down

# 停止并删除数据卷（谨慎使用）
docker compose down -v
```

---

## 📚 相关文档

- [主脚本说明](../README.md)
- [运维脚本说明](../ops/README.md)
- [部署检查清单](../../../docs/PRODUCTION_DEPLOYMENT_CHECKLIST.md)

---

**最后更新**: 2026-01-31
