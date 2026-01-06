# 数据库迁移脚本

此目录用于存放增量迁移脚本。

## 命名规范

```
YYYY-MM-DD-描述.sql
```

例如：
- `2026-01-06-add-new-feature.sql`
- `2026-01-07-fix-column-type.sql`

## 注意事项

1. 迁移脚本应该是幂等的（可重复执行）
2. 使用 `IF NOT EXISTS` 或 `ON CONFLICT DO NOTHING` 等语法
3. 执行前先在测试环境验证

## 历史脚本

2026-01-05 之前的迁移脚本已整合到 `init-db/` 目录，原始文件备份在：
`scripts/backup/2026-01-05-before-consolidation/migration/`
