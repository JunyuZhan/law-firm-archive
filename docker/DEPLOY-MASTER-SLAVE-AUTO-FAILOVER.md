# 主从自动故障切换方案

## 📋 当前方案分析

### 当前方案的特点

**数据复制方式**：
- ✅ 使用 PostgreSQL **流式复制**（Streaming Replication）
- ✅ 这是 PostgreSQL 官方推荐的 HA 方案，**不算落后**
- ✅ 数据实时同步（延迟通常 < 1秒）
- ✅ 从库可以用于只读查询（分担主库压力）

**故障切换方式**：
- ⚠️ **手动切换**（需要人工干预）
- ⚠️ 需要手动执行 `pg_promote()` 提升从库
- ⚠️ 需要手动修改应用配置

### 为什么说"不算落后"？

PostgreSQL 流式复制是**业界标准**的 HA 方案：
- ✅ 被广泛使用（包括大型互联网公司）
- ✅ 数据一致性有保障
- ✅ 性能优秀（异步复制，不影响主库性能）
- ✅ 配置相对简单

**"落后"的地方**：
- ❌ 缺少自动故障检测
- ❌ 缺少自动切换机制

---

## 🚀 自动接管方案

### 方案一：Keepalived + 虚拟IP + 自动切换脚本（推荐）⭐

**特点**：
- ✅ 自动故障检测（Keepalived 心跳检测）
- ✅ 自动切换虚拟IP
- ✅ 自动提升从库（配合脚本）
- ✅ 配置相对简单
- ✅ 成本低

**架构**：
```
虚拟IP: 192.168.50.100 (VIP)
    │
    ├── 主服务器 (Keepalived Master)
    │      ├── PostgreSQL 主库
    │      └── Backend（连接 VIP）
    │
    └── 从服务器 (Keepalived Backup)
           ├── PostgreSQL 从库
           └── Backend（连接 VIP）

主服务器故障
    ↓
Keepalived 检测到故障
    ↓
自动切换 VIP 到从服务器
    ↓
触发脚本：自动执行 pg_promote()
    ↓
自动更新应用配置
    ↓
完成切换（通常 30-60秒）
```

**实现步骤**：

#### 1. 安装 Keepalived

```bash
# Ubuntu/Debian
sudo apt-get install keepalived

# CentOS/RHEL
sudo yum install keepalived
```

#### 2. 配置 Keepalived（主服务器）

创建 `/etc/keepalived/keepalived.conf`：

```bash
vrrp_script chk_postgres {
    script "/usr/local/bin/check-postgres.sh"
    interval 2
    weight -5
    fall 3
    rise 2
}

vrrp_instance VI_1 {
    state MASTER
    interface eth0  # 根据实际网卡修改
    virtual_router_id 51
    priority 100
    advert_int 1
    authentication {
        auth_type PASS
        auth_pass your_password_here
    }
    virtual_ipaddress {
        192.168.50.100/24  # 虚拟IP
    }
    track_script {
        chk_postgres
    }
    notify_master "/usr/local/bin/postgres-master.sh"
    notify_backup "/usr/local/bin/postgres-slave.sh"
    notify_fault "/usr/local/bin/postgres-fault.sh"
}
```

#### 3. 配置 Keepalived（从服务器）

```bash
vrrp_instance VI_1 {
    state BACKUP  # 从服务器是 BACKUP
    interface eth0
    virtual_router_id 51
    priority 90  # 优先级低于主服务器
    advert_int 1
    authentication {
        auth_type PASS
        auth_pass your_password_here
    }
    virtual_ipaddress {
        192.168.50.100/24
    }
    track_script {
        chk_postgres
    }
    notify_master "/usr/local/bin/postgres-master.sh"
    notify_backup "/usr/local/bin/postgres-slave.sh"
    notify_fault "/usr/local/bin/postgres-fault.sh"
}
```

#### 4. 创建健康检查脚本

`/usr/local/bin/check-postgres.sh`：

```bash
#!/bin/bash
# PostgreSQL 健康检查脚本

# 检查 PostgreSQL 是否运行
if ! docker exec law-firm-postgres-master pg_isready -U law_admin -d law_firm > /dev/null 2>&1; then
    exit 1
fi

# 检查主从复制状态（如果是主库）
if docker exec law-firm-postgres-master psql -U law_admin -d law_firm -tAc "SELECT 1 FROM pg_stat_replication LIMIT 1" > /dev/null 2>&1; then
    exit 0
fi

exit 0
```

#### 5. 创建自动切换脚本

`/usr/local/bin/postgres-master.sh`（当服务器成为 Master 时执行）：

```bash
#!/bin/bash
# 当服务器成为 Master 时执行

LOG_FILE="/var/log/postgres-failover.log"
echo "$(date): Server became MASTER" >> "$LOG_FILE"

# 检查当前 PostgreSQL 角色
ROLE=$(docker exec law-firm-postgres-slave psql -U law_admin -d law_firm -tAc "SELECT pg_is_in_recovery()" 2>/dev/null || echo "unknown")

if [ "$ROLE" = "t" ]; then
    # 当前是从库，需要提升为主库
    echo "$(date): Promoting slave to master..." >> "$LOG_FILE"
    
    # 提升从库为主库
    docker exec law-firm-postgres-slave psql -U law_admin -d law_firm -c "SELECT pg_promote();" >> "$LOG_FILE" 2>&1
    
    # 更新应用配置
    sed -i 's/NODE_ROLE=slave/NODE_ROLE=master/' /opt/law-firm/.env
    sed -i 's/DB_HOST=postgres-slave/DB_HOST=postgres-master/' /opt/law-firm/.env
    
    # 重启应用服务
    cd /opt/law-firm
    docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile master up -d backend frontend
    
    echo "$(date): Failover completed" >> "$LOG_FILE"
else
    echo "$(date): Already master, no action needed" >> "$LOG_FILE"
fi
```

`/usr/local/bin/postgres-slave.sh`（当服务器成为 Backup 时执行）：

```bash
#!/bin/bash
# 当服务器成为 Backup 时执行

LOG_FILE="/var/log/postgres-failover.log"
echo "$(date): Server became BACKUP" >> "$LOG_FILE"
# 通常不需要操作，保持从库状态
```

**切换时间**：通常 30-60 秒

---

### 方案二：Patroni（完整HA方案）⭐⭐⭐

**特点**：
- ✅ 完整的自动故障检测和切换
- ✅ 自动提升从库
- ✅ 自动更新配置
- ✅ 支持多节点
- ⚠️ 配置复杂
- ⚠️ 需要 etcd/Consul

**架构**：
```
etcd/Consul (配置中心)
    │
    ├── 主服务器 (Patroni)
    │      └── PostgreSQL 主库
    │
    └── 从服务器 (Patroni)
           └── PostgreSQL 从库

主服务器故障
    ↓
Patroni 检测到故障（通过 etcd）
    ↓
自动选举新的主库（从库提升）
    ↓
自动更新 DCS（分布式配置存储）
    ↓
所有节点自动更新配置
    ↓
完成切换（通常 10-30秒）
```

**部署复杂度**：高（需要配置 etcd/Consul + Patroni）

---

### 方案三：Pgpool-II（连接池 + 故障转移）⭐⭐

**特点**：
- ✅ 连接池管理
- ✅ 自动故障转移
- ✅ 读写分离
- ✅ 负载均衡
- ⚠️ 需要额外组件

**架构**：
```
应用
  ↓
Pgpool-II (连接池)
  ├── PostgreSQL 主库（写）
  └── PostgreSQL 从库（读）

主库故障
    ↓
Pgpool-II 检测到故障
    ↓
自动切换写操作到从库
    ↓
自动提升从库为主库
    ↓
完成切换（通常 10-20秒）
```

---

## 📊 方案对比

| 方案 | 切换时间 | 复杂度 | 成本 | 自动化程度 | 推荐度 |
|------|---------|--------|------|-----------|--------|
| **当前方案（手动）** | 5-10分钟 | 低 | 低 | ⚠️ 低 | 小型律所 |
| **Keepalived + 脚本** | 30-60秒 | 中 | 低 | ✅✅ 高 | ⭐⭐⭐ **推荐** |
| **Patroni** | 10-30秒 | 高 | 中 | ✅✅✅ 很高 | 中型律所 |
| **Pgpool-II** | 10-20秒 | 中 | 中 | ✅✅ 高 | 需要读写分离 |

---

## 🎯 推荐实施路径

### 阶段一：当前方案（手动切换）

**适用场景**：
- 小型律所（< 50人）
- 可以接受手动切换
- 预算有限

**特点**：
- ✅ 简单，易于理解
- ✅ 成本低
- ❌ 需要人工干预

---

### 阶段二：Keepalived + 自动切换脚本（推荐升级）⭐

**适用场景**：
- 中小型律所（50-100人）
- 需要自动切换
- 有一定技术能力

**实施步骤**：

1. **安装 Keepalived**
   ```bash
   sudo apt-get install keepalived
   ```

2. **配置 Keepalived**（主从服务器分别配置）

3. **创建健康检查脚本**

4. **创建自动切换脚本**

5. **修改应用配置**
   - Backend 连接虚拟IP（192.168.50.100）而不是直接连接 postgres-master
   - 或者使用服务发现机制

**优点**：
- ✅ 自动故障检测和切换
- ✅ 配置相对简单
- ✅ 成本低
- ✅ 切换时间短（30-60秒）

**缺点**：
- ⚠️ 需要配置 Keepalived
- ⚠️ 需要编写切换脚本
- ⚠️ 需要配置虚拟IP

---

### 阶段三：Patroni（完整HA）

**适用场景**：
- 中型律所（100+人）
- 需要完整的HA方案
- 有专业运维能力

**优点**：
- ✅ 完整的自动故障转移
- ✅ 支持多节点
- ✅ 专业级HA方案

**缺点**：
- ❌ 配置复杂
- ❌ 需要 etcd/Consul
- ❌ 学习成本高

---

## 💡 关于"复制数据"的说明

### PostgreSQL 流式复制 ≠ 备份

**流式复制（Streaming Replication）**：
- ✅ 实时同步数据（延迟 < 1秒）
- ✅ 从库可以用于只读查询
- ✅ 主库故障时，从库可以快速接管
- ✅ 这是**高可用方案**，不是备份方案

**备份（Backup）**：
- 定期全量备份（pg_dump）
- 用于数据恢复
- 与流式复制是**互补关系**

**最佳实践**：
- ✅ 流式复制：用于高可用（主从）
- ✅ 定期备份：用于数据恢复（pg_dump + 归档）

---

## 🔄 自动接管流程（Keepalived方案）

### 正常状态

```
主服务器 (192.168.50.10)
  ├── Keepalived Master
  ├── PostgreSQL 主库
  ├── Backend（连接 VIP: 192.168.50.100）
  └── VIP: 192.168.50.100

从服务器 (192.168.50.11)
  ├── Keepalived Backup
  ├── PostgreSQL 从库（复制主库数据）
  └── Backend（待机）
```

### 主服务器故障

```
1. Keepalived 检测到主服务器故障（心跳超时）
   ↓
2. Keepalived 自动切换 VIP 到从服务器
   ↓
3. 触发 notify_master 脚本
   ↓
4. 脚本自动执行：
   - pg_promote() 提升从库为主库
   - 更新 .env 配置
   - 重启 backend/frontend
   ↓
5. 完成切换（30-60秒）
```

### 切换后的状态

```
从服务器 (192.168.50.11) - 现在是主服务器
  ├── Keepalived Master
  ├── PostgreSQL 主库（已提升）
  ├── Backend（连接 VIP: 192.168.50.100）
  └── VIP: 192.168.50.100

原主服务器 (192.168.50.10) - 故障
  └── （离线）
```

---

## 📝 实施建议

### 立即可用：当前方案（手动切换）

**优点**：
- ✅ 已实现，可以直接使用
- ✅ 简单，易于理解
- ✅ 成本低

**缺点**：
- ❌ 需要人工干预
- ❌ 切换时间较长（5-10分钟）

### 推荐升级：Keepalived + 自动切换脚本

**实施难度**：中等
**切换时间**：30-60秒
**成本**：低（只需安装 Keepalived）

**需要的工作**：
1. 安装 Keepalived（2台服务器）
2. 配置 Keepalived
3. 创建健康检查脚本
4. 创建自动切换脚本
5. 配置虚拟IP

---

## 🎯 总结

### 关于"落后"的问题

**答案**：不算落后
- PostgreSQL 流式复制是**业界标准**
- 被广泛使用
- 性能优秀，数据一致性有保障

**"落后"的地方**：
- 缺少自动故障检测和切换
- 可以通过 Keepalived + 脚本解决

### 关于"复制数据"的问题

**答案**：不是备份，是高可用方案
- 流式复制：实时同步，用于高可用
- 定期备份：用于数据恢复
- 两者是**互补关系**

### 关于"自动接管"的问题

**答案**：可以实现
- **推荐方案**：Keepalived + 自动切换脚本
- **切换时间**：30-60秒
- **实施难度**：中等
- **成本**：低

---

**最后更新**: 2026-01-31
