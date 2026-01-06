# 脚本备份说明

备份时间：2026-01-05
备份原因：整合所有初始化脚本和迁移脚本为统一的初始化脚本

## 目录结构

```
2026-01-05-before-consolidation/
├── init-db/          # 原始初始化脚本（01-35）
├── migration/        # 迁移脚本
├── fix-menu-table.sql
├── reset-db.sh
└── README.md
```

## 备份的脚本清单

### init-db/ 目录
- 01-schema.sql ~ 35-workbench-approval-schema.sql

### migration/ 目录
- 各种迁移和修复脚本

## 恢复方法

如需恢复，将对应目录的文件复制回 scripts/ 目录即可。
