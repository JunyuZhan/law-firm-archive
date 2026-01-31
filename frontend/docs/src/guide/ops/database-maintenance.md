# 数据库维护

## 概述

在单端口架构下，数据库端口（5432）不暴露到公网，这是生产环境的标准安全实践。本文档说明如何在不暴露端口的情况下维护数据库。

## 为什么数据库端口不暴露？

### ✅ 安全性

- **降低攻击面**：数据库端口不暴露到公网，避免直接攻击
- **防止未授权访问**：即使防火墙配置错误，数据库也不会暴露
- **符合安全最佳实践**：这是生产环境的标准做法

### ✅ 不影响维护

- **通过 Docker 命令访问**：使用 `docker exec` 可以直接访问数据库容器
- **SSH 隧道支持**：需要远程访问时可以使用 SSH 隧道
- **备份脚本正常**：所有备份脚本都使用 `docker exec`，不受影响

## 访问方式

### 方法 1：服务器上直接访问（推荐）

#### 进入数据库交互式命令行

```bash
# 进入 PostgreSQL 容器
docker exec -it law-firm-postgres psql -U law_admin -d law_firm
```

#### 执行 SQL 命令

```bash
# 执行单条 SQL 命令
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT version();"

# 执行 SQL 文件
docker exec -i law-firm-postgres psql -U law_admin -d law_firm < script.sql
```

#### 查看数据库信息

```bash
# 查看数据库列表
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "\l"

# 查看表列表
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "\dt"

# 查看表结构
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "\d table_name"
```

### 方法 2：SSH 隧道（远程访问）

如果需要从本地电脑使用数据库客户端（如 DBeaver、pgAdmin）连接远程数据库：

#### 建立 SSH 隧道

```bash
# 建立 SSH 隧道（将本地 5432 端口映射到服务器的 5432 端口）
ssh -L 5432:localhost:5432 user@your-server-ip

# 或者使用不同的本地端口
ssh -L 15432:localhost:5432 user@your-server-ip
```

#### 连接数据库

建立隧道后，在本地数据库客户端中：

- **主机**: `localhost`（或 `127.0.0.1`）
- **端口**: `5432`（或你指定的本地端口，如 `15432`）
- **数据库**: `law_firm`
- **用户名**: `law_admin`
- **密码**: `.env` 文件中的 `DB_PASSWORD`

## 备份和恢复

### 自动备份

系统提供了自动备份脚本：

```bash
# 数据库自动备份（推荐）
./scripts/db-auto-backup.sh

# 完整备份（数据库 + MinIO）
./scripts/backup.sh
```

这些脚本都使用 `docker exec` 命令，不需要暴露端口。

### 手动备份

#### 备份数据库

```bash
# 备份整个数据库
docker exec law-firm-postgres pg_dump -U law_admin law_firm > backup_$(date +%Y%m%d).sql

# 备份特定表
docker exec law-firm-postgres pg_dump -U law_admin -t table_name law_firm > table_backup.sql

# 压缩备份
docker exec law-firm-postgres pg_dump -U law_admin law_firm | gzip > backup_$(date +%Y%m%d).sql.gz
```

#### 恢复数据库

```bash
# 恢复数据库（会覆盖现有数据）
docker exec -i law-firm-postgres psql -U law_admin -d law_firm < backup.sql

# 或者使用恢复脚本
./scripts/restore.sh backup.sql
```

### 导出数据

#### 导出为 CSV

```bash
# 导出表数据为 CSV
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "\COPY table_name TO '/tmp/table_name.csv' CSV HEADER"

# 从容器复制到主机
docker cp law-firm-postgres:/tmp/table_name.csv ./
```

#### 导出为 SQL

```bash
# 导出表结构和数据
docker exec law-firm-postgres pg_dump -U law_admin -t table_name law_firm > table_name.sql

# 只导出表结构（不含数据）
docker exec law-firm-postgres pg_dump -U law_admin -t table_name -s law_firm > table_structure.sql
```

## 性能监控

### 查看数据库连接数

```bash
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT count(*) FROM pg_stat_activity;"
```

### 查看慢查询

```bash
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT pid, now() - pg_stat_activity.query_start AS duration, query FROM pg_stat_activity WHERE state = 'active' AND now() - pg_stat_activity.query_start > interval '5 seconds';"
```

### 查看数据库大小

```bash
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT pg_size_pretty(pg_database_size('law_firm'));"
```

## 维护操作

### 清理连接

```bash
# 终止所有活动连接（谨慎使用）
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'law_firm' AND pid <> pg_backend_pid();"
```

### 重建索引

```bash
# 重建所有索引
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "REINDEX DATABASE law_firm;"

# 重建特定表的索引
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "REINDEX TABLE table_name;"
```

### 更新统计信息

```bash
# 更新所有表的统计信息
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "ANALYZE;"
```

## 常见问题

### Q: 为什么不能直接连接数据库端口？

A: 这是安全最佳实践。数据库端口不暴露可以：
- 降低被攻击的风险
- 防止未授权访问
- 符合生产环境安全标准

### Q: 备份脚本还能用吗？

A: 可以！所有备份脚本都使用 `docker exec` 命令，不依赖端口暴露。参考 `scripts/db-auto-backup.sh` 和 `scripts/backup.sh`。

### Q: 如何从本地访问远程数据库？

A: 使用 SSH 隧道。参考上面的"方法 2：SSH 隧道"部分。

### Q: 数据库维护会影响服务吗？

A: 大部分维护操作（如查询、备份）不会影响服务。但以下操作可能需要停止服务：
- 数据库版本升级
- 大规模数据迁移
- 表结构重大变更

## 相关文档

- [单端口架构](/guide/ops/single-port-architecture)
- [备份恢复](/guide/ops/backup)
- [故障排查](/guide/ops/troubleshooting)

---

**最后更新**: 2026-01-31
