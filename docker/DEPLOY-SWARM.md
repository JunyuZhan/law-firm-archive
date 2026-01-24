# 律师事务所管理系统 - Docker Swarm 分布式部署指南

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        负载均衡 (Swarm Ingress)                  │
│                           端口: 80                               │
└─────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
          ┌─────────────────┐         ┌─────────────────┐
          │ Frontend × 2    │         │ Frontend × 2    │
          │ (Node 1)        │         │ (Node 2)        │
          └─────────────────┘         └─────────────────┘
                    │                           │
                    └─────────────┬─────────────┘
                                  ▼
          ┌─────────────────┐         ┌─────────────────┐
          │ Backend × 2     │         │ Backend × 2     │
          │ (Node 1)        │         │ (Node 2)        │
          └─────────────────┘         └─────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
          ┌─────────────────┐         ┌─────────────────┐
          │ PostgreSQL      │         │ Redis           │
          │ (Manager Only)  │         │ (Manager Only)  │
          └─────────────────┘         └─────────────────┘
```

## 前置要求

### 硬件要求

| 节点类型 | 最低配置 | 推荐配置 |
|----------|----------|----------|
| Manager 节点 | 2核 4GB | 4核 8GB |
| Worker 节点 | 2核 4GB | 4核 8GB |

### 软件要求

- Docker 24.0+
- 所有节点网络互通
- 开放端口：
  - 2377/tcp - 集群管理
  - 7946/tcp+udp - 节点通信
  - 4789/udp - Overlay 网络
  - 80/tcp - Web 访问
  - 9001/tcp - MinIO 控制台

## 快速部署

### 步骤 1: Manager 节点初始化

在第一台服务器（如 192.168.50.10）上运行：

```bash
# 克隆代码
git clone https://github.com/JunyuZhan/law-firm.git
cd law-firm

# 配置环境变量
cp docker/env.example docker/.env
vim docker/.env  # 修改密码

# 初始化 Swarm 集群
./scripts/deploy-swarm.sh init
```

初始化后会显示 Worker 加入命令，类似：
```
docker swarm join --token SWMTKN-1-xxx... 192.168.50.10:2377
```

### 步骤 2: 添加 Worker 节点

在其他服务器上运行：

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 加入集群（使用 Manager 节点显示的命令）
docker swarm join --token SWMTKN-1-xxx... 192.168.50.10:2377
```

### 步骤 3: 部署服务

回到 Manager 节点：

```bash
./scripts/deploy-swarm.sh deploy
```

## 常用命令

### 查看集群状态

```bash
./scripts/deploy-swarm.sh status
```

### 扩缩容

```bash
# 将后端扩展到 4 个副本
./scripts/deploy-swarm.sh scale backend 4

# 将前端扩展到 3 个副本
./scripts/deploy-swarm.sh scale frontend 3
```

### 查看日志

```bash
./scripts/deploy-swarm.sh logs backend
./scripts/deploy-swarm.sh logs frontend
```

### 更新服务

```bash
# 重新构建并更新所有服务
./scripts/deploy-swarm.sh update
```

### 删除服务栈

```bash
./scripts/deploy-swarm.sh remove
```

## SSL 证书配置（HTTPS）

### 证书存放位置

将 SSL 证书文件放在 `docker/ssl/` 目录下：

```bash
docker/ssl/
├── fullchain.pem    # 完整证书链
└── privkey.pem      # 私钥文件
```

详细说明请参考：[docker/ssl/README.md](../docker/ssl/README.md)

### 配置 HTTPS

1. **放置证书文件**
   ```bash
   # 将证书文件复制到 docker/ssl/ 目录
   cp /path/to/your/fullchain.pem docker/ssl/fullchain.pem
   cp /path/to/your/privkey.pem docker/ssl/privkey.pem
   
   # 设置正确的文件权限
   chmod 644 docker/ssl/fullchain.pem
   chmod 600 docker/ssl/privkey.pem
   ```

2. **修改 docker-compose.swarm.yml**
   
   在 `frontend` 服务中添加 volume 挂载和端口映射：
   ```yaml
   frontend:
     volumes:
       - ./ssl:/etc/nginx/ssl:ro
     ports:
       - "80:8080"   # HTTP（重定向到 HTTPS）
       - "443:8443"  # HTTPS
   ```

3. **使用支持 HTTPS 的 Nginx 配置**
   
   将 `frontend/scripts/deploy/nginx.conf` 替换为 `nginx-ssl.conf.example` 的内容，或参考该文件修改现有配置。

## 访问服务

部署完成后，可以通过任意节点 IP 访问：

| 服务 | 地址 |
|------|------|
| 系统首页（HTTP） | `http://<任意节点IP>` |
| 系统首页（HTTPS） | `https://<任意节点IP>`（需配置SSL证书） |
| MinIO 控制台 | `http://<Manager节点IP>:9001` |

## 高可用配置

### 多 Manager 节点（推荐）

生产环境建议 3 个 Manager 节点：

```bash
# 在 Manager 节点获取 Manager 加入令牌
docker swarm join-token manager

# 在新节点上运行显示的命令
docker swarm join --token SWMTKN-1-xxx... 192.168.50.10:2377
```

### 数据库高可用

当前配置使用单节点 PostgreSQL。如需高可用，建议：

1. **外部数据库服务**（推荐）
   - 阿里云 RDS / 腾讯云 MySQL
   - 自建 PostgreSQL 主从集群

2. **修改配置**
   ```yaml
   # 移除 postgres 服务，修改 backend 环境变量
   environment:
     - DB_HOST=your-external-db-host
   ```

## 故障排除

### 服务无法启动

```bash
# 查看服务详情
docker service ps law-firm_backend --no-trunc

# 查看日志
docker service logs law-firm_backend
```

### 节点离线

```bash
# 查看节点状态
docker node ls

# 将节点设为维护模式
docker node update --availability drain <node-id>

# 恢复节点
docker node update --availability active <node-id>
```

### 网络问题

```bash
# 检查 overlay 网络
docker network ls
docker network inspect law-firm_law-firm-network
```

## 监控建议

生产环境建议添加监控：

- **Portainer** - Docker 可视化管理
- **Prometheus + Grafana** - 指标监控
- **ELK Stack** - 日志收集分析

```bash
# 快速部署 Portainer
docker service create \
  --name portainer \
  --publish 9000:9000 \
  --constraint 'node.role == manager' \
  --mount type=bind,src=/var/run/docker.sock,dst=/var/run/docker.sock \
  portainer/portainer-ce
```

