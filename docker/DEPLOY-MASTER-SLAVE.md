# 主从服务器部署方案

> 💡 **最经济实惠的高可用方案**：两台服务器，每台运行全部容器，实现主从高可用。

## 📋 架构概述

### 架构图

```
┌─────────────────────────────────────────────────────────┐
│                   负载均衡 (Nginx)                        │
│             域名: law-firm.example.com                   │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  主服务器     │  │  从服务器     │  │  可选：NAS   │
│ <主服务器IP> │  │ <从服务器IP> │  │  (共享存储)  │
│              │  │              │  │              │
│ Frontend     │  │ Frontend     │  │              │
│ Backend      │  │ Backend      │  │              │
│ PostgreSQL主 │  │ PostgreSQL从 │  │              │
│ Redis主      │  │ Redis从      │  │              │
│ MinIO节点1   │  │ MinIO节点2   │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
        │                 │                 │
        └─────────────────┼─────────────────┘
                          ▼
                  数据同步和备份
```

### 核心特点

- ✅ **经济实惠**：只需 2 台服务器
- ✅ **高可用**：一台故障，另一台继续服务
- ✅ **数据安全**：数据库主从复制，数据不丢失
- ✅ **负载分担**：两台服务器分担用户请求
- ✅ **简单维护**：使用标准 Docker Compose，无需复杂编排

---

## 🎯 部署方案

### 方案一：标准主从（推荐）

**特点**：
- 主服务器：运行所有服务（主库）
- 从服务器：运行所有服务（从库）
- 数据库：PostgreSQL 主从复制
- Redis：主从复制
- MinIO：分布式模式（2节点）
- 负载均衡：Nginx upstream

**适用场景**：
- 中小型律所（20-100人）
- 需要高可用但预算有限
- 内网部署

---

### 方案二：主从 + 共享存储

**特点**：
- 主从服务器配置同上
- 文件存储：MinIO 使用共享存储（NFS/NAS）
- 数据库：PostgreSQL 主从复制
- Redis：主从复制

**适用场景**：
- 已有 NAS 设备
- 需要集中管理文件存储
- 文件量大，需要集中备份

---

## 📦 服务器要求

### 主服务器（Master）

| 项目 | 要求 |
|------|------|
| CPU | 4核+ |
| 内存 | 8GB+ |
| 磁盘 | 100GB+ SSD |
| 网络 | 千兆网卡 |
| 系统 | Ubuntu 22.04 / CentOS 8+ |

### 从服务器（Slave）

| 项目 | 要求 |
|------|------|
| CPU | 4核+ |
| 内存 | 8GB+ |
| 磁盘 | 100GB+ SSD |
| 网络 | 千兆网卡 |
| 系统 | Ubuntu 22.04 / CentOS 8+ |

### 网络要求

- 两台服务器在同一内网（推荐）
- 主从服务器之间网络延迟 < 10ms
- 防火墙开放端口：
  - 主服务器：80, 443, 5432（PostgreSQL 主从复制）
  - 从服务器：80, 443, 5432（PostgreSQL 主从复制）

---

## 🚀 部署步骤

### 🎯 方式一：全自动部署（推荐）✅

**只需一条命令，自动完成所有配置和部署！**

#### 第一步：部署主服务器

```bash
# 1. 克隆代码到主服务器
git clone git@github.com:junyuzhan/law-firm.git /opt/law-firm
cd /opt/law-firm

# 2. 全自动部署（一条命令搞定）
./scripts/ops/deploy-master-slave.sh master --init-db
```

**脚本会自动完成**：
- ✅ 创建 `.env` 文件
- ✅ 配置主服务器环境变量
- ✅ 创建 PostgreSQL 主库配置
- ✅ 启动所有服务
- ✅ 创建复制用户
- ✅ 初始化数据库（如果指定 `--init-db`）

#### 第二步：部署从服务器

```bash
# 1. 克隆代码到从服务器
git clone git@github.com:junyuzhan/law-firm.git /opt/law-firm
cd /opt/law-firm

# 2. 全自动部署（一条命令搞定）
./scripts/ops/deploy-master-slave.sh slave <主服务器IP>
```

**脚本会自动完成**：
- ✅ 创建 `.env` 文件
- ✅ 配置从服务器环境变量
- ✅ 创建 PostgreSQL 从库配置
- ✅ 启动所有服务
- ✅ 从主服务器自动复制数据（pg_basebackup）
- ✅ 配置主从复制

---

### 📝 方式二：手动部署（可选）

如果需要手动控制每个步骤，可以参考以下流程：

#### 第一步：准备主服务器

```bash
# 1. 克隆代码到主服务器
git clone git@github.com:junyuzhan/law-firm.git /opt/law-firm
cd /opt/law-firm

# 2. 使用配置脚本
./scripts/ops/setup-master-slave.sh master

# 3. 启动主服务器服务
docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile master up -d

# 4. 初始化数据库（仅在主服务器执行一次）
./scripts/ops/reset-db.sh --prod
```

### 第二步：配置 PostgreSQL 主从复制

#### 在主服务器配置主库

```bash
# 1. 进入 PostgreSQL 容器
docker exec -it law-firm-postgres-master bash

# 2. 编辑 postgresql.conf
echo "wal_level = replica" >> /var/lib/postgresql/data/postgresql.conf
echo "max_wal_senders = 3" >> /var/lib/postgresql/data/postgresql.conf
echo "max_replication_slots = 3" >> /var/lib/postgresql/data/postgresql.conf

# 3. 编辑 pg_hba.conf（允许从服务器复制）
# ⚠️ 注意：请将 <从服务器IP> 替换为实际的从服务器IP地址
echo "host replication replicator <从服务器IP>/32 md5" >> /var/lib/postgresql/data/pg_hba.conf
# 或者使用配置脚本自动生成：./scripts/ops/setup-master-slave.sh master

# 4. 重启 PostgreSQL
exit
docker restart law-firm-postgres-master

# 5. 创建复制用户
docker exec -it law-firm-postgres-master psql -U law_admin -d law_firm
CREATE USER replicator WITH REPLICATION PASSWORD 'replicator_password';
\q
```

#### 在从服务器配置从库（手动方式）

```bash
# 1. 使用配置脚本
./scripts/ops/setup-master-slave.sh slave <主服务器IP>

# 2. 停止从服务器 PostgreSQL（如果已启动）
docker stop law-firm-postgres-slave

# 3. 从主服务器复制数据（PostgreSQL 15 方式）
# ⚠️ 注意：请将 <主服务器IP> 替换为实际的主服务器IP地址
docker run --rm -v law-firm-master-slave_postgres_slave_data:/data \
  -e PGHOST=<主服务器IP> -e PGPORT=5432 -e PGUSER=replicator \
  -e PGPASSWORD=replicator_password \
  postgres:15-alpine pg_basebackup -D /data -R -X stream -P -U replicator

# 4. 启动从服务器服务
docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile slave up -d
```

> 💡 **提示**：使用全自动部署脚本 `deploy-master-slave.sh` 可以自动完成以上所有步骤！

### 第三步：配置 Redis 主从

Redis 主从配置会自动通过环境变量完成，无需手动配置。

### 第四步：配置 MinIO 分布式

MinIO 会自动配置为分布式模式（2节点），数据会自动同步。

### 第五步：配置负载均衡（Nginx）

在主服务器或独立服务器上配置 Nginx 负载均衡：

```nginx
# ⚠️ 注意：请将 <主服务器IP> 和 <从服务器IP> 替换为实际的IP地址
upstream backend_servers {
    server <主服务器IP>:8080 weight=2 max_fails=3 fail_timeout=30s;
    server <从服务器IP>:8080 weight=1 max_fails=3 fail_timeout=30s backup;
    keepalive 32;
}

upstream frontend_servers {
    server <主服务器IP>:80 weight=2 max_fails=3 fail_timeout=30s;
    server <从服务器IP>:80 weight=1 max_fails=3 fail_timeout=30s backup;
    keepalive 32;
}

server {
    listen 80;
    server_name law-firm.example.com;

    location / {
        proxy_pass http://frontend_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://backend_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 🔧 配置文件说明

### docker-compose.master-slave.yml

主从部署的 Docker Compose 配置文件，包含：
- `--profile master`：主服务器配置
- `--profile slave`：从服务器配置

### 环境变量配置

在项目根目录的 `.env` 文件中添加（如果不存在，先运行 `cp env.example .env`）：

```bash
# 主从服务器配置（使用配置脚本会自动设置）
# ⚠️ 注意：请将 <主服务器IP> 和 <从服务器IP> 替换为实际的IP地址
NODE_ROLE=master  # 或 slave
MASTER_IP=<主服务器IP>  # 主服务器会自动检测，从服务器需要手动设置
SLAVE_IP=<从服务器IP>   # 需要手动设置

# PostgreSQL 主从复制
POSTGRES_REPLICATION_USER=replicator
POSTGRES_REPLICATION_PASSWORD=replicator_password  # 请修改为强密码

# Redis 主从配置（从服务器需要设置）
REDIS_MASTER_HOST=<主服务器IP>  # 仅在从服务器需要设置
REDIS_MASTER_PORT=6379

# MinIO 分布式配置（可选）
MINIO_NODE1_HOST=<主服务器IP>
MINIO_NODE2_HOST=<从服务器IP>
```

> 💡 **提示**：使用配置脚本 `./scripts/ops/setup-master-slave.sh` 可以自动设置大部分配置。

---

## 📊 监控和维护

### 检查主从复制状态

```bash
# 在主服务器检查
docker exec law-firm-postgres-master psql -U law_admin -d law_firm \
  -c "SELECT * FROM pg_stat_replication;"

# 在从服务器检查
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT * FROM pg_stat_wal_receiver;"
```

### 手动切换主从

如果主服务器故障，需要手动将从服务器提升为主服务器：

```bash
# 1. 确认主服务器故障
ping <主服务器IP>  # 检查网络连通性
docker exec law-firm-postgres-master psql -U law_admin -d law_firm -c "SELECT 1;"  # 检查数据库

# 2. 在从服务器提升为主库（从库变为可写）
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT pg_promote();"

# 3. 更新从服务器配置，改为主服务器模式
# 编辑项目根目录 .env 文件：
# NODE_ROLE=master  # 改为 master
# DB_HOST=postgres-master  # 改为 postgres-master（或保持 postgres-slave，但需要修改 docker-compose）

# 4. 重启从服务器服务（以主服务器模式运行）
docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile master up -d

# 5. 验证新主库可以写入
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "CREATE TABLE test_write AS SELECT 1; DROP TABLE test_write;"
```

> ⚠️ **注意**：当前方案为手动切换。要实现自动切换，请参考 [常见问题文档](./DEPLOY-MASTER-SLAVE-FAQ.md) 中的自动切换方案。

### 第六步：配置 Keepalived 自动故障切换（可选）⭐

> 💡 **推荐**：配置 Keepalived 后，主服务器故障时可以在 30-60 秒内自动切换到从服务器，无需人工干预。

#### 快速安装（使用脚本）

```bash
# 在主服务器和从服务器都要执行
cd /opt/law-firm

# 1. 安装 Keepalived
sudo apt-get update
sudo apt-get install keepalived  # Ubuntu/Debian
# 或
sudo yum install keepalived      # CentOS/RHEL

# 2. 复制脚本到系统目录
sudo cp scripts/ops/keepalived/*.sh /usr/local/bin/
sudo chmod +x /usr/local/bin/*.sh

# 3. 配置 Keepalived（主服务器）
sudo cp scripts/ops/keepalived/keepalived-master.conf.example /etc/keepalived/keepalived.conf
sudo nano /etc/keepalived/keepalived.conf
# 修改：interface（网卡名称）、auth_pass（密码）、virtual_ipaddress（虚拟IP）

# 4. 配置 Keepalived（从服务器）
sudo cp scripts/ops/keepalived/keepalived-slave.conf.example /etc/keepalived/keepalived.conf
sudo nano /etc/keepalived/keepalived.conf
# 修改：interface（网卡名称）、auth_pass（必须和主服务器一样）、virtual_ipaddress（必须和主服务器一样）、priority（90，低于主服务器）

# 5. 检查配置
sudo keepalived -t

# 6. 启动服务
sudo systemctl enable keepalived
sudo systemctl start keepalived

# 7. 查看状态
sudo systemctl status keepalived
```

#### 详细配置说明

详见 [Keepalived 自动故障切换方案](./DEPLOY-MASTER-SLAVE-AUTO-FAILOVER.md) 和 [Keepalived 脚本说明](../scripts/ops/keepalived/README.md)。

**关键配置项**：
- **虚拟IP（VIP）**：选择一个未被使用的IP地址（例如 `192.168.50.100`）
- **网卡名称**：使用 `ip addr` 查看实际网卡名称（通常是 `eth0` 或 `ens33`）
- **认证密码**：主从服务器必须使用相同的密码
- **优先级**：主服务器 `100`，从服务器 `90`（数字越大越优先）

**验证**：
```bash
# 查看虚拟IP是否绑定
ip addr show eth0  # 主服务器应该看到虚拟IP

# 查看 Keepalived 日志
sudo journalctl -u keepalived -f

# 查看故障切换日志
tail -f /var/log/postgres-failover.log
```

### 数据备份

```bash
# 在主服务器执行备份
./scripts/ops/backup.sh

# 备份会自动包含：
# - PostgreSQL 数据库备份
# - MinIO 文件备份
```

---

## ⚠️ 注意事项

1. **数据库主从延迟**：主从复制有延迟（通常 < 1秒），读操作建议使用主库
2. **MinIO 分布式**：2节点 MinIO 分布式模式，允许 1 个节点故障
3. **Redis 主从**：从服务器只读，写操作必须到主服务器
4. **负载均衡**：建议使用 Nginx 或 HAProxy 做负载均衡
5. **监控告警**：建议配置 Prometheus + Grafana 监控
6. **故障切换**：当前方案为**手动切换**，主服务器故障时需要手动提升从服务器（详见 [常见问题](./DEPLOY-MASTER-SLAVE-FAQ.md)）

## ❓ 常见问题

### Q1: 部署是否全部自动化？
**A**: ✅ **已实现全自动化**！使用 `deploy-master-slave.sh` 脚本，一条命令完成所有部署步骤。详见 [常见问题文档](./DEPLOY-MASTER-SLAVE-FAQ.md#问题1部署是否全部自动化)

### Q2: 从服务器数据库是只读的，接管后可以写吗？
**A**: **可以写**，但需要手动执行 `pg_promote()` 将从库提升为主库。提升后就可以正常读写了。详见 [常见问题文档](./DEPLOY-MASTER-SLAVE-FAQ.md#问题2数据库只读在从服务器接管的时候是否可以写)

### Q3: 如何识别主服务器挂了？是否自动切换？
**A**: 
- **手动切换**：当前基础方案为手动切换，需要人工发现故障并执行切换。
- **自动切换**：推荐配置 **Keepalived + 自动切换脚本**（切换时间 30-60秒）。详见：
  - [自动故障切换方案](./DEPLOY-MASTER-SLAVE-AUTO-FAILOVER.md)
  - [Keepalived 脚本说明](../scripts/ops/keepalived/README.md)
  - 部署步骤中的"第六步：配置 Keepalived 自动故障切换"

### Q4: 这种方式是不是很落后？是备份吗？
**A**: **不算落后**。PostgreSQL 流式复制是业界标准的 HA 方案，被广泛使用。它不是备份方案，而是高可用方案（实时同步数据）。备份应该使用定期 pg_dump。详见 [自动故障切换方案](./DEPLOY-MASTER-SLAVE-AUTO-FAILOVER.md)

---

## 🎯 优势对比

| 方案 | 成本 | 可用性 | 复杂度 | 适用场景 |
|------|------|--------|--------|---------|
| **单机部署** | 低 | 99% | 低 | 小型律所 |
| **主从部署** | 中 | 99.9% | 中 | 中小型律所 ✅ |
| **Swarm 多节点** | 高 | 99.9% | 高 | 中型律所 |
| **Kubernetes** | 很高 | 99.99% | 很高 | 大型律所 |

---

## 📚 相关文档

- [高可用架构详解](../docs/HA_ARCHITECTURE_EXPLAINED.md)
- [Docker Swarm 部署](./DEPLOY-SWARM.md)
- [数据安全指南](./DATA-SECURITY.md)
- [备份恢复脚本](../scripts/ops/README.md)

---

**最后更新**: 2026-01-31
