# 主从部署常见问题解答

## ❓ 问题1：部署是否全部自动化？

### ✅ **现在已实现全自动化！**

使用 `deploy-master-slave.sh` 脚本可以实现**一条命令全自动部署**。

#### ✅ 全自动部署脚本：`deploy-master-slave.sh`

**主服务器部署**（一条命令）：
```bash
./scripts/ops/deploy-master-slave.sh master --init-db
```

**自动完成**：
- ✅ 创建 `.env` 文件
- ✅ 设置 `NODE_ROLE=master`
- ✅ 检测主服务器IP
- ✅ 创建 PostgreSQL 配置
- ✅ 启动所有服务
- ✅ 等待服务就绪
- ✅ 创建复制用户
- ✅ 初始化数据库（如果指定 `--init-db`）

**从服务器部署**（一条命令）：
```bash
./scripts/ops/deploy-master-slave.sh slave <主服务器IP>
```

**自动完成**：
- ✅ 创建 `.env` 文件
- ✅ 设置 `NODE_ROLE=slave`
- ✅ 配置连接主服务器
- ✅ 创建 PostgreSQL 从库配置
- ✅ 启动所有服务
- ✅ 等待服务就绪
- ✅ **自动从主服务器复制数据**（pg_basebackup）
- ✅ 配置主从复制

#### ⚠️ 仍需手动操作的部分：

1. **首次部署前**：
   - 编辑 `.env` 设置密码（脚本会创建模板）
   - 设置 `SLAVE_IP`（从服务器IP，主服务器需要）

2. **pg_hba.conf 配置**：
   - 脚本会自动生成，但建议根据实际网络限制IP范围

### 💡 使用建议：

**推荐流程**：
1. 主服务器：`./scripts/ops/deploy-master-slave.sh master --init-db`
2. 编辑 `.env` 设置密码和 `SLAVE_IP`
3. 从服务器：`./scripts/ops/deploy-master-slave.sh slave <主服务器IP>`

**就这么简单！** 🎉

---

## ❓ 问题2：数据库只读，在从服务器接管的时候，是否可以写？

### 答案：**可以写，但需要手动提升** ✅

#### 当前状态：

1. **正常运行时**：
   - 主库：✅ 可读可写
   - 从库：✅ 只读（`hot_standby=on`）

2. **主库故障后**：
   - 从库仍然是只读状态
   - **需要手动执行 `pg_promote()` 提升为主库**
   - 提升后：✅ 可读可写

#### 提升从库为主库的步骤：

```bash
# 1. 在从服务器执行提升命令
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT pg_promote();"

# 2. 更新应用配置（修改 .env）
# 将 DB_HOST 从 postgres-slave 改为 postgres-master
# 或者修改 docker-compose 配置

# 3. 重启后端服务
docker compose restart backend
```

#### ⚠️ 注意事项：

- **数据一致性**：提升前确保从库已同步最新数据
- **应用配置**：提升后必须更新应用连接配置
- **原主库恢复**：原主库恢复后需要重新配置为从库

### 💡 改进建议：

要实现**自动提升**，可以使用：
- **Patroni**：PostgreSQL 高可用管理器，自动故障检测和切换
- **Keepalived + 虚拟IP**：自动切换虚拟IP到健康的服务器
- **Pgpool-II**：连接池和自动故障转移

---

## ❓ 问题3：如何识别主服务器挂了？是否自动切换？

### 答案：**当前方案是手动切换** ⚠️

#### 当前方案的问题：

1. **没有自动故障检测**
   - 需要人工发现主服务器故障
   - 没有监控告警机制

2. **没有自动切换**
   - 需要手动执行 `pg_promote()`
   - 需要手动修改应用配置
   - 需要手动重启服务

#### 如何识别主服务器故障：

**方式1：手动检查**
```bash
# 检查主服务器是否在线
ping <主服务器IP>

# 检查 PostgreSQL 是否运行
docker exec law-firm-postgres-master psql -U law_admin -d law_firm -c "SELECT 1;"

# 检查主从复制状态
docker exec law-firm-postgres-master psql -U law_admin -d law_firm \
  -c "SELECT * FROM pg_stat_replication;"
```

**方式2：监控告警（需要配置）**
- Prometheus + Grafana 监控
- 配置告警规则，主服务器故障时发送通知

#### 手动切换流程：

```bash
# 1. 确认主服务器故障
ping <主服务器IP>  # 不通

# 2. 在从服务器提升为主库
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT pg_promote();"

# 3. 修改从服务器配置，改为主服务器模式
# 编辑 .env: NODE_ROLE=master
# 或者修改 docker-compose 配置

# 4. 重启服务
docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile master up -d
```

### 💡 实现自动切换的方案：

#### 方案1：使用 Patroni（推荐）

**特点**：
- ✅ 自动故障检测（心跳检测）
- ✅ 自动故障转移（自动提升从库）
- ✅ 自动更新配置
- ✅ 支持多节点

**架构**：
```
主服务器 (Patroni) ──心跳──> 从服务器 (Patroni)
    │                          │
    └── 故障检测 ──────────────┘
    │
    └── 自动提升从库为主库
```

**部署复杂度**：中等（需要配置 etcd/Consul）

#### 方案2：使用 Keepalived + 虚拟IP

**特点**：
- ✅ 自动故障检测
- ✅ 自动切换虚拟IP
- ⚠️ 需要手动提升数据库（配合脚本可自动化）

**架构**：
```
虚拟IP: 192.168.50.100
    │
    ├── 主服务器 (Keepalived Master)
    └── 从服务器 (Keepalived Backup)
    
主服务器故障 → Keepalived 自动切换虚拟IP到从服务器
```

**部署复杂度**：低（配置简单）

#### 方案3：使用 Pgpool-II

**特点**：
- ✅ 连接池管理
- ✅ 自动故障转移
- ✅ 读写分离
- ⚠️ 需要额外组件

**部署复杂度**：中等

---

## 📊 方案对比

| 方案 | 自动化程度 | 复杂度 | 成本 | 适用场景 |
|------|-----------|--------|------|---------|
| **当前方案（手动）** | ⚠️ 低 | 低 | 低 | 小型律所，可接受手动切换 |
| **Keepalived + 脚本** | ✅ 中 | 中 | 低 | 中小型律所，需要自动切换 |
| **Patroni** | ✅✅ 高 | 高 | 中 | 中型律所，需要完整HA |
| **Pgpool-II** | ✅✅ 高 | 中 | 中 | 需要读写分离的场景 |

---

## 🎯 推荐方案

### 阶段一：当前方案（手动切换）

**适用场景**：
- 小型律所（< 50人）
- 可以接受手动切换（通常几分钟内完成）
- 预算有限

**优点**：
- ✅ 简单，易于理解
- ✅ 成本低
- ✅ 维护简单

**缺点**：
- ❌ 需要人工干预
- ❌ 切换时间较长（5-10分钟）

### 阶段二：Keepalived + 自动切换脚本（推荐升级）

**适用场景**：
- 中小型律所（50-100人）
- 需要自动切换
- 有一定技术能力

**实现方式**：
1. 配置 Keepalived 实现虚拟IP切换
2. 编写脚本监听 Keepalived 状态变化
3. 自动执行 `pg_promote()` 和配置更新

**优点**：
- ✅ 自动故障检测和切换
- ✅ 配置相对简单
- ✅ 成本低

**缺点**：
- ⚠️ 需要配置 Keepalived
- ⚠️ 需要编写切换脚本

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

## 📝 总结

### 当前方案（手动切换）

1. **部署**：半自动化（脚本 + 手动步骤）
2. **数据库写入**：从库提升后可以写，但需要手动操作
3. **故障切换**：手动切换，需要人工干预

### 改进方向

1. **全自动化部署**：创建一键部署脚本
2. **自动故障检测**：配置监控告警
3. **自动故障切换**：使用 Keepalived + 脚本 或 Patroni

---

**最后更新**: 2026-01-31
