# 数据库维护操作说明（不暴露端口也能维护）

## ✅ 重要澄清

**不暴露端口 ≠ 无法维护数据库**

实际上，**不暴露端口的情况下，维护数据库更方便、更安全！**

---

## 🔍 两种维护方式对比

### 方式 1：暴露端口（不推荐）

```yaml
# docker-compose.prod.yml
postgres:
  ports:
    - "5432:5432"  # ← 暴露端口
```

**维护方式：**
```bash
# 从外部电脑连接数据库
psql -h your-server-ip -p 5432 -U law_admin -d law_firm
```

**问题：**
- ❌ 安全风险高（任何人都可以尝试连接）
- ❌ 需要配置防火墙规则
- ❌ 需要配置 SSL 证书

### 方式 2：不暴露端口（推荐，当前方案）

```yaml
# docker-compose.prod.yml
postgres:
  # ports:  # ← 不暴露端口
```

**维护方式：**
```bash
# 在服务器上直接执行（不需要暴露端口）
docker exec law-firm-postgres pg_dump -U law_admin -d law_firm > backup.sql
```

**优点：**
- ✅ 更安全（数据库不暴露到公网）
- ✅ 操作简单（在服务器上直接执行）
- ✅ 性能更好（本地操作，无网络延迟）

---

## 🛠️ 实际维护操作（不暴露端口）

### 1. 数据库备份

**项目已有备份脚本：** `scripts/db-auto-backup.sh`

```bash
# 方式1：使用备份脚本（推荐）
cd /path/to/law-firm
./scripts/db-auto-backup.sh          # 执行备份
./scripts/db-auto-backup.sh --schedule  # 设置定时备份

# 方式2：手动备份
docker exec law-firm-postgres pg_dump -U law_admin -d law_firm \
  --format=plain --no-owner --no-acl | gzip > backup.sql.gz

# 方式3：完整备份（数据库 + 文件）
./scripts/backup.sh                  # 备份数据库和 MinIO 文件
```

**关键点：**
- ✅ 在**服务器上**执行，不需要暴露端口
- ✅ 使用 `docker exec` 直接访问容器内的数据库
- ✅ 备份文件保存在服务器上

### 2. 数据库导出

```bash
# 导出整个数据库
docker exec law-firm-postgres pg_dump -U law_admin -d law_firm > export.sql

# 导出特定表
docker exec law-firm-postgres pg_dump -U law_admin -d law_firm \
  -t table_name > table_export.sql

# 导出为 CSV（通过 psql）
docker exec law-firm-postgres psql -U law_admin -d law_firm \
  -c "COPY (SELECT * FROM users) TO STDOUT WITH CSV HEADER" > users.csv
```

### 3. 数据库恢复

**项目已有恢复脚本：** `scripts/restore.sh`

```bash
# 方式1：使用恢复脚本
cd /path/to/law-firm
./scripts/restore.sh backup.sql

# 方式2：手动恢复
docker exec -i law-firm-postgres psql -U law_admin -d law_firm < backup.sql

# 方式3：从压缩文件恢复
gunzip < backup.sql.gz | docker exec -i law-firm-postgres psql -U law_admin -d law_firm
```

### 4. 数据库查询和管理

```bash
# 方式1：直接执行 SQL
docker exec law-firm-postgres psql -U law_admin -d law_firm \
  -c "SELECT COUNT(*) FROM users;"

# 方式2：进入数据库交互式命令行
docker exec -it law-firm-postgres psql -U law_admin -d law_firm

# 方式3：执行 SQL 文件
docker exec -i law-firm-postgres psql -U law_admin -d law_firm < query.sql
```

### 5. 数据库迁移

**项目已有迁移脚本：** `scripts/migration/`

```bash
# 执行迁移脚本
docker exec -i law-firm-postgres psql -U law_admin -d law_firm < migration.sql
```

---

## 📋 常见维护场景

### 场景 1：日常备份（自动）

**频率：** 每天自动执行

**操作：**
```bash
# 设置定时备份（一次设置，自动执行）
cd /path/to/law-firm
./scripts/db-auto-backup.sh --schedule

# 查看备份状态
./scripts/db-auto-backup.sh --status
```

**说明：**
- ✅ 自动执行，无需人工干预
- ✅ 在服务器上执行，不需要暴露端口
- ✅ 备份文件保存在服务器上

### 场景 2：手动备份（紧急情况）

**频率：** 需要时执行

**操作：**
```bash
# SSH 到服务器
ssh user@your-server

# 执行备份
cd /path/to/law-firm
./scripts/backup.sh db

# 或手动备份
docker exec law-firm-postgres pg_dump -U law_admin -d law_firm \
  | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

**时间：** 几秒钟完成

### 场景 3：数据导出（给客户或审计）

**频率：** 偶尔需要

**操作：**
```bash
# SSH 到服务器
ssh user@your-server

# 导出数据
docker exec law-firm-postgres pg_dump -U law_admin -d law_firm \
  --format=plain > export.sql

# 下载到本地（如果需要）
scp user@your-server:/path/to/export.sql ./
```

**说明：**
- ✅ 在服务器上导出，不需要暴露端口
- ✅ 通过 SSH 下载文件，安全可靠

### 场景 4：数据恢复（故障恢复）

**频率：** 很少发生

**操作：**
```bash
# SSH 到服务器
ssh user@your-server

# 恢复数据
cd /path/to/law-firm
./scripts/restore.sh backup.sql

# 或手动恢复
docker exec -i law-firm-postgres psql -U law_admin -d law_firm < backup.sql
```

**说明：**
- ✅ 在服务器上恢复，不需要暴露端口
- ✅ 使用项目提供的脚本，操作简单

### 场景 5：查看数据库状态（故障排查）

**频率：** 需要时执行

**操作：**
```bash
# SSH 到服务器
ssh user@your-server

# 查看数据库状态
docker exec law-firm-postgres psql -U law_admin -d law_firm \
  -c "SELECT pg_size_pretty(pg_database_size('law_firm'));"

# 查看连接数
docker exec law-firm-postgres psql -U law_admin -d law_firm \
  -c "SELECT count(*) FROM pg_stat_activity;"

# 查看表大小
docker exec law-firm-postgres psql -U law_admin -d law_firm \
  -c "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size FROM pg_tables WHERE schemaname = 'public' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
```

---

## 🔄 如果需要图形界面工具（DBeaver、Navicat）

### 使用 SSH 隧道（推荐）

**步骤：**

1. **建立 SSH 隧道**
   ```bash
   # 在本地电脑执行
   ssh -L 5432:localhost:5432 user@your-server-ip
   ```

2. **配置数据库工具**
   - 主机：`localhost`
   - 端口：`5432`
   - 用户名：`law_admin`
   - 密码：`your-db-password`
   - 数据库：`law_firm`

3. **连接数据库**
   - 就像数据库在本地一样
   - 可以执行所有操作（查询、导出、备份等）

**优点：**
- ✅ 使用图形界面，操作方便
- ✅ 数据库不暴露端口，安全
- ✅ 所有操作通过 SSH 加密传输

---

## 📊 对比总结

| 操作 | 不暴露端口（当前方案） | 暴露端口 |
|------|---------------------|---------|
| **备份** | ✅ `docker exec ... pg_dump` | ⚠️ 需要外部连接 |
| **导出** | ✅ `docker exec ... pg_dump` | ⚠️ 需要外部连接 |
| **恢复** | ✅ `docker exec ... psql < file` | ⚠️ 需要外部连接 |
| **查询** | ✅ `docker exec ... psql -c` | ⚠️ 需要外部连接 |
| **安全性** | ✅ 高（不暴露） | ❌ 低（暴露端口） |
| **便利性** | ✅ 高（服务器上直接操作） | ⚠️ 中（需要网络连接） |

---

## ✅ 结论

### 不暴露端口的情况下，完全可以维护数据库！

**所有维护操作都可以在服务器上完成：**

1. ✅ **备份**：使用 `scripts/db-auto-backup.sh` 或 `docker exec pg_dump`
2. ✅ **导出**：使用 `docker exec pg_dump`
3. ✅ **恢复**：使用 `scripts/restore.sh` 或 `docker exec psql`
4. ✅ **查询**：使用 `docker exec psql`
5. ✅ **管理**：使用图形工具 + SSH 隧道

### 为什么更推荐不暴露端口？

1. ✅ **更安全**：数据库不暴露到公网
2. ✅ **更方便**：在服务器上直接操作，无需网络连接
3. ✅ **更快速**：本地操作，无网络延迟
4. ✅ **更标准**：这是生产环境的标准做法

### 项目已提供的工具

- ✅ `scripts/db-auto-backup.sh` - 自动备份脚本
- ✅ `scripts/backup.sh` - 完整备份脚本
- ✅ `scripts/restore.sh` - 恢复脚本
- ✅ `scripts/migration/` - 迁移脚本

**所有工具都设计为在服务器上运行，不需要暴露数据库端口！**

---

## 🎯 最终答案

**不暴露端口，完全可以维护数据库！**

- ✅ 备份：使用项目提供的脚本或 `docker exec pg_dump`
- ✅ 导出：使用 `docker exec pg_dump`
- ✅ 恢复：使用项目提供的脚本或 `docker exec psql`
- ✅ 查询：使用 `docker exec psql` 或图形工具 + SSH 隧道

**所有操作都在服务器上完成，不需要暴露端口！**
