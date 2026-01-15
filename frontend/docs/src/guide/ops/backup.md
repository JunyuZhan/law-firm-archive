# 备份恢复

本章节介绍如何使用系统自带脚本和 MinIO 工具完成数据库、文件的备份与恢复。

## 数据库备份

### 一键自动备份脚本（推荐）

系统提供了 PostgreSQL 自动备份脚本：

```bash
./scripts/db-auto-backup.sh          # 智能备份（日/周/月）
./scripts/db-auto-backup.sh --schedule   # 设置每日定时备份
./scripts/db-auto-backup.sh --status     # 查看备份状态
```

智能备份策略：

- 每天执行：生成「每日备份」
- 每周日：额外生成「每周备份」
- 每月 1 日：额外生成「每月备份」
- 自动按保留策略清理历史备份（默认：每日7天、每周4周、每月12个月）

备份目录结构（默认在项目根目录 `backups/db`）：

```text
backups/db/
  ├── daily/      # 每日备份
  ├── weekly/     # 每周备份
  └── monthly/    # 每月备份
```

如需修改备份目录，可以设置环境变量：

```bash
BACKUP_DIR=/mnt/nas/law-firm/db ./scripts/db-auto-backup.sh
```

### 手动备份（非 Docker 环境）

```bash
pg_dump -U lawfirm -h localhost lawfirm > backup_$(date +%Y%m%d).sql
```

### Docker 环境手动备份

```bash
docker exec law-firm-postgres pg_dump -U law_admin law_firm > backup.sql
```

## 文件备份（MinIO）

文件数据（合同、证据、卷宗等）存储在 MinIO，对律所来说是最核心的资产。

### 使用 mc 客户端备份

```bash
# 同步 MinIO bucket 到本地备份目录
mc mirror minio/law-firm /backup/files/
```

### 增量同步/定期备份

```bash
mc mirror --overwrite minio/law-firm /backup/files/
```

可以结合 cron 定时执行：

```bash
# 每天 4 点增量同步到 NAS
0 4 * * * mc mirror --overwrite minio/law-firm /backup/files/
```

更多分布式与云存储方案，参考项目根目录 `docker/DATA-SECURITY.md`。

## 恢复数据

### 恢复数据库

从自动备份目录恢复示例：

```bash
# 查看可用备份
ls -la backups/db/daily/

# 恢复某次备份（本机环境）
psql -U lawfirm -h localhost lawfirm < backups/db/daily/2026-01-09_030000.sql

# Docker 环境恢复
docker exec -i law-firm-postgres psql -U law_admin law_firm < backups/db/daily/2026-01-09_030000.sql
```

也可以使用脚本 `./scripts/restore.sh`（参见 `docker/DATA-SECURITY.md` 中示例）。

### 恢复文件

```bash
mc mirror /backup/files/ minio/law-firm
```

## 备份策略建议

推荐策略（与自动备份脚本保持一致）：

| 类型       | 频率   | 保留周期 |
| ---------- | ------ | -------- |
| 数据库每日 | 每日   | 7 天     |
| 数据库每周 | 每周日 | 4 周     |
| 数据库每月 | 每月 1 日 | 12 个月 |
| 文件增量   | 每日   | 30 天    |
| 异地备份   | 每周   | ≥ 90 天  |

实际生产中建议：

- 数据库和文件备份同时落地到本地磁盘 + NAS/对象存储
- 定期在测试环境执行「恢复演练」，验证备份可用性
