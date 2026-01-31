# 数据安全说明：原主服务器恢复的风险和防护

## ⚠️ 关键问题

### 问题：原主服务器数据过期，贸然接入是否会导致数据回滚或删除？

**答案：使用脚本是安全的，但手动操作有风险！**

---

## 🔒 脚本的安全机制

### `restore-original-master.sh` 脚本的保护措施

#### 1. 数据备份（步骤3）

```bash
# 脚本会自动备份现有数据
BACKUP_DIR="/tmp/postgres-backup-$(date +%Y%m%d_%H%M%S)"
# 备份到 /tmp/postgres-backup-YYYYMMDD_HHMMSS/
```

**保护**：
- ✅ 清空前先备份
- ✅ 备份位置明确（`/tmp/postgres-backup-时间戳/`）
- ✅ 如果恢复失败，可以从备份恢复

#### 2. 数据清空（步骤4）

```bash
# 清空数据目录
docker run --rm -v "$VOLUME_NAME":/data alpine sh -c "rm -rf /data/*"
```

**保护**：
- ✅ 先备份，再清空
- ✅ 清空后从新主服务器复制最新数据
- ✅ 不会保留过期数据

#### 3. 从新主服务器复制（步骤5）

```bash
# 从新主服务器复制最新数据
pg_basebackup -D /data -R -X stream -P -U replicator
```

**保护**：
- ✅ 使用 `pg_basebackup` 从新主服务器复制
- ✅ 确保数据是最新的
- ✅ 不会使用过期数据

---

## ⚠️ 手动操作的风险

### 风险场景1：直接启动原主服务器

```bash
# ❌ 危险操作！
# 如果直接启动原主服务器的 PostgreSQL，可能会：
docker start law-firm-postgres-master

# 风险：
# 1. 原主服务器可能认为自己是主库
# 2. 如果配置错误，可能尝试写入
# 3. 如果数据过期，可能导致数据不一致
```

**后果**：
- ⚠️ 数据冲突
- ⚠️ 数据不一致
- ⚠️ 可能导致数据丢失

### 风险场景2：错误配置主从关系

```bash
# ❌ 危险操作！
# 如果错误地将原主服务器配置为主库，新主服务器配置为从库：

# 原主服务器（数据过期）
NODE_ROLE=master  # ❌ 错误！

# 新主服务器（数据最新）
NODE_ROLE=slave   # ❌ 错误！

# 后果：
# - 原主服务器的过期数据会覆盖新主服务器的最新数据
# - 导致数据回滚到故障前的状态
# - 故障切换后的所有数据都会丢失！
```

---

## 🛡️ 防护措施

### 措施1：使用恢复脚本（推荐）⭐

```bash
# ✅ 安全操作
./scripts/ops/keepalived/restore-original-master.sh <新主服务器IP>

# 脚本会：
# 1. 备份现有数据
# 2. 清空数据目录
# 3. 从新主服务器复制最新数据
# 4. 配置为从服务器
```

### 措施2：数据时间戳验证（增强保护）

在恢复前，可以手动检查数据时间戳：

```bash
# 检查原主服务器数据时间戳
docker run --rm -v law-firm-master-slave_postgres_master_data:/data \
  postgres:15-alpine \
  psql -U law_admin -d law_firm \
  -c "SELECT pg_last_xact_replay_timestamp();" 2>/dev/null || echo "无法连接"

# 检查新主服务器数据时间戳
docker run --rm --network host postgres:15-alpine \
  psql -h <新主服务器IP> -U law_admin -d law_firm \
  -c "SELECT pg_last_xact_replay_timestamp();"

# 比较时间戳，确保新主服务器的数据更新
```

### 措施3：防止误操作的保护

**当前脚本的保护**：
- ✅ 需要手动确认（`yes/no`）
- ✅ 明确警告会删除数据
- ✅ 自动备份数据

**建议增强**：
- 添加数据时间戳比较
- 添加数据大小验证
- 添加二次确认

---

## 📊 数据安全对比

| 操作方式 | 数据备份 | 数据清空 | 数据来源 | 安全性 |
|---------|---------|---------|---------|--------|
| **使用恢复脚本** | ✅ 自动备份 | ✅ 先备份再清空 | ✅ 从新主服务器复制 | ✅✅✅ **安全** |
| **手动操作（正确）** | ⚠️ 需要手动备份 | ⚠️ 需要手动清空 | ✅ 从新主服务器复制 | ✅✅ 较安全 |
| **手动操作（错误）** | ❌ 无备份 | ❌ 不清空 | ❌ 使用过期数据 | ❌❌❌ **危险** |

---

## 🔍 PostgreSQL 主从复制的数据安全机制

### PostgreSQL 的保护机制

#### 1. 从库只读保护

```sql
-- 从库默认是只读的
SELECT pg_is_in_recovery();  -- 返回 t（true）

-- 如果尝试写入，会报错：
ERROR: cannot execute INSERT in a read-only transaction
```

**保护**：
- ✅ 从库无法写入，防止数据冲突
- ✅ 只能从主库复制数据

#### 2. WAL（Write-Ahead Log）机制

```
主库写入 → WAL 日志 → 从库复制 WAL → 从库应用
```

**保护**：
- ✅ 数据复制基于 WAL，确保一致性
- ✅ 从库只能应用主库的 WAL，不能自己生成

#### 3. 时间线（Timeline）保护

```
主库时间线: 1
从库时间线: 1（跟随主库）

如果从库提升为主库：
  新主库时间线: 2

如果旧主库恢复并尝试连接：
  - 旧主库时间线: 1
  - 新主库时间线: 2
  - PostgreSQL 会拒绝连接（时间线不匹配）
```

**保护**：
- ✅ 防止过期数据覆盖新数据
- ✅ 时间线不匹配时会报错

---

## ⚠️ 潜在风险场景

### 场景1：原主服务器数据比新主服务器新（极罕见）

**可能情况**：
- 故障切换后，新主服务器写入了一些数据
- 但原主服务器在故障前也有未同步的数据
- 如果原主服务器数据时间戳更新

**风险**：
- ⚠️ 如果错误地将原主服务器设为主库，会丢失新主服务器的数据

**防护**：
- ✅ 使用恢复脚本（会从新主服务器复制，忽略原主服务器数据）
- ✅ 检查数据时间戳
- ✅ 确认新主服务器是当前主库

### 场景2：配置错误导致双主库

**可能情况**：
- 原主服务器恢复后，错误配置为主库
- 新主服务器仍然是主库
- 两个主库同时写入

**风险**：
- ⚠️ 数据冲突
- ⚠️ 数据不一致
- ⚠️ 可能导致数据损坏

**防护**：
- ✅ 使用恢复脚本（自动配置为从库）
- ✅ 检查主从状态（`check-role.sh`）
- ✅ 确保只有一个主库

### 场景3：网络分区导致的数据分叉

**可能情况**：
- 主从服务器网络断开
- 从服务器提升为主库
- 原主服务器恢复后，两个主库都有写入

**风险**：
- ⚠️ 数据分叉
- ⚠️ 需要手动合并数据

**防护**：
- ✅ 监控网络状态
- ✅ 使用 Keepalived 检测故障
- ✅ 恢复时从新主服务器复制数据

---

## ✅ 安全恢复流程

### 推荐流程（使用脚本）

```bash
# 1. 检查当前状态
./scripts/ops/keepalived/check-role.sh

# 2. 确认新主服务器（当前主库）
# 在新主服务器执行：
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT pg_is_in_recovery();"
# 应该返回 f（false，表示是主库）

# 3. 在原主服务器执行恢复脚本
cd /opt/law-firm
sudo ./scripts/ops/keepalived/restore-original-master.sh <新主服务器IP>

# 脚本会自动：
# - 备份数据
# - 清空数据目录
# - 从新主服务器复制最新数据
# - 配置为从服务器
```

### 手动流程（需要谨慎）

```bash
# ⚠️ 警告：手动操作有风险，建议使用脚本！

# 1. 备份原主服务器数据
docker run --rm -v law-firm-master-slave_postgres_master_data:/data \
  -v /tmp/backup:/backup alpine tar czf /backup/backup.tar.gz -C /data .

# 2. 停止 PostgreSQL
docker stop law-firm-postgres-master

# 3. 清空数据目录
docker run --rm -v law-firm-master-slave_postgres_master_data:/data \
  alpine sh -c "rm -rf /data/*"

# 4. 从新主服务器复制数据
docker run --rm \
  -v law-firm-master-slave_postgres_slave_data:/data \
  -e PGHOST=<新主服务器IP> \
  -e PGPORT=5432 \
  -e PGUSER=replicator \
  -e PGPASSWORD=replicator_password \
  postgres:15-alpine \
  pg_basebackup -D /data -R -X stream -P -U replicator

# 5. 更新配置为从服务器
# 编辑 .env: NODE_ROLE=slave

# 6. 启动从库服务
docker compose --env-file .env -f docker/docker-compose.master-slave.yml \
  --profile slave up -d postgres-slave
```

---

## 🎯 最佳实践

### 1. 始终使用恢复脚本

```bash
# ✅ 推荐
./scripts/ops/keepalived/restore-original-master.sh <新主服务器IP>

# ❌ 不推荐手动操作
```

### 2. 恢复前验证

```bash
# 1. 确认新主服务器是当前主库
./scripts/ops/keepalived/check-role.sh  # 在新主服务器执行

# 2. 检查数据时间戳（可选）
# 确保新主服务器的数据更新

# 3. 备份重要数据（额外保险）
./scripts/ops/backup.sh  # 在新主服务器执行
```

### 3. 恢复后验证

```bash
# 1. 检查从库状态
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT pg_is_in_recovery();"
# 应该返回 t（true，表示是从库）

# 2. 检查复制状态
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT * FROM pg_stat_wal_receiver;"
# 应该看到复制连接信息

# 3. 检查数据是否同步
# 在新主服务器写入测试数据
# 在从服务器查询，应该能看到
```

---

## 📋 数据安全检查清单

### 恢复前检查

- [ ] 确认新主服务器是当前主库（`check-role.sh`）
- [ ] 确认新主服务器数据是最新的
- [ ] 备份新主服务器数据（额外保险）
- [ ] 确认网络连接正常

### 恢复时检查

- [ ] 使用恢复脚本（不要手动操作）
- [ ] 确认备份已创建
- [ ] 确认数据已清空
- [ ] 确认从新主服务器复制数据成功

### 恢复后检查

- [ ] 检查从库状态（`pg_is_in_recovery()` 返回 `t`）
- [ ] 检查复制状态（`pg_stat_wal_receiver` 有数据）
- [ ] 验证数据同步（写入测试数据）
- [ ] 检查日志（确认无错误）

---

## ⚠️ 重要警告

### 1. 不要直接启动原主服务器

```bash
# ❌ 危险！
docker start law-firm-postgres-master
# 可能导致数据冲突或数据丢失
```

### 2. 不要错误配置主从关系

```bash
# ❌ 危险！
# 原主服务器：NODE_ROLE=master  # 错误！
# 新主服务器：NODE_ROLE=slave    # 错误！

# 会导致数据回滚！
```

### 3. 不要跳过数据复制步骤

```bash
# ❌ 危险！
# 直接启动从库，不清空数据，不复制数据
# 可能导致数据不一致
```

---

## 🔒 总结

### 使用脚本是安全的 ✅

- ✅ 自动备份数据
- ✅ 先备份再清空
- ✅ 从新主服务器复制最新数据
- ✅ 自动配置为从服务器

### 手动操作有风险 ⚠️

- ⚠️ 可能使用过期数据
- ⚠️ 可能导致数据回滚
- ⚠️ 可能导致数据丢失

### 推荐做法

1. **始终使用恢复脚本**
2. **恢复前验证新主服务器状态**
3. **恢复后验证数据同步**
4. **定期备份数据**

---

**最后更新**: 2026-01-31
