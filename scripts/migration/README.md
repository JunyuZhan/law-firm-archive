# 数据库迁移脚本

此目录用于存放**增量迁移脚本**，仅在版本升级时使用。

## 当前状态

截至 2026-01-08，所有开发阶段的迁移脚本已合并到 `init-db/` 初始化脚本中。
本目录暂时为空，待正式版本发布后用于存放版本间的增量迁移。

## 命名规范

```
VX.Y.Z-描述.sql
```

例如：
- `V1.0.1-add-new-column.sql`
- `V1.1.0-add-new-feature.sql`

## 使用说明

1. 迁移脚本应该是**幂等的**（可重复执行）
2. 使用 `IF NOT EXISTS`、`ON CONFLICT DO NOTHING` 等语法
3. 执行前先在测试环境验证
4. 每个脚本需包含版本号和描述注释

## 历史备份

开发阶段的脚本备份位置：
- `scripts/backup/2026-01-05-before-consolidation/` - 早期脚本
- `scripts/backup/2026-01-08-before-cleanup/` - 整理前的脚本
