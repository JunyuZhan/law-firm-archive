# 备份恢复

## 数据库备份

### 手动备份

```bash
pg_dump -U lawfirm -h localhost lawfirm > backup_$(date +%Y%m%d).sql
```

### 定时备份

创建 cron 任务：

```bash
# 每天凌晨2点备份
0 2 * * * pg_dump -U lawfirm lawfirm | gzip > /backup/db_$(date +\%Y\%m\%d).sql.gz
```

### Docker 环境备份

```bash
docker exec postgres pg_dump -U lawfirm lawfirm > backup.sql
```

## 文件备份

### MinIO 文件备份

```bash
# 使用 mc 客户端
mc mirror minio/law-firm /backup/files/
```

### 增量同步

```bash
mc mirror --overwrite minio/law-firm /backup/files/
```

## 恢复数据

### 恢复数据库

```bash
psql -U lawfirm -h localhost lawfirm < backup.sql
```

### Docker 环境恢复

```bash
docker exec -i postgres psql -U lawfirm lawfirm < backup.sql
```

### 恢复文件

```bash
mc mirror /backup/files/ minio/law-firm
```

## 备份策略建议

| 类型 | 频率 | 保留周期 |
|------|------|----------|
| 数据库全量 | 每日 | 30天 |
| 数据库增量 | 每小时 | 7天 |
| 文件增量 | 每日 | 30天 |
| 异地备份 | 每周 | 90天 |
