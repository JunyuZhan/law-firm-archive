# 脚本说明

本目录包含系统部署、运维、测试相关的脚本。

## 部署脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `deploy.sh` | 一键 Docker 部署 | `./deploy.sh` |
| `deploy-swarm.sh` | Docker Swarm 集群部署 | `./deploy-swarm.sh init` |

### deploy.sh

```bash
# 一键部署（自动构建并启动所有服务）
./scripts/deploy.sh
```

### deploy-swarm.sh

```bash
# 初始化 Swarm 集群（Manager 节点）
./scripts/deploy-swarm.sh init

# 部署服务
./scripts/deploy-swarm.sh deploy

# 扩缩容
./scripts/deploy-swarm.sh scale backend 4
```

## 数据库脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `reset-db.sh` | 重置数据库（开发用） | `./reset-db.sh` |
| `init-db/init-database.sh` | 初始化数据库 | `./init-db/init-database.sh` |

### reset-db.sh

```bash
# 重置数据库（会删除所有数据！）
./scripts/reset-db.sh

# 强制重置（跳过确认）
./scripts/reset-db.sh --force
```

## 备份恢复脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `backup.sh` | 完整备份（数据库 + 文件） | `./backup.sh` |
| `db-auto-backup.sh` | 数据库自动备份 | `./db-auto-backup.sh --schedule` |
| `restore.sh` | 数据恢复 | `./restore.sh list` |

### backup.sh

```bash
# 完整备份（数据库 + MinIO 文件）
./scripts/backup.sh

# 仅备份数据库
./scripts/backup.sh db

# 仅备份文件
./scripts/backup.sh files
```

### db-auto-backup.sh

```bash
# 执行备份
./scripts/db-auto-backup.sh

# 设置定时任务（每日凌晨 3 点）
./scripts/db-auto-backup.sh --schedule

# 查看备份状态
./scripts/db-auto-backup.sh --status
```

### restore.sh

```bash
# 列出可用备份
./scripts/restore.sh list

# 恢复数据库
./scripts/restore.sh db backups/db/daily/2026-01-09_030000.sql.gz

# 完整恢复
./scripts/restore.sh full 2026-01-09
```

## 检查脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `check-production-ready.sh` | 生产环境检查 | `./check-production-ready.sh` |

### check-production-ready.sh

```bash
# 检查系统是否可以部署到生产环境
./scripts/check-production-ready.sh
```

## 测试脚本

| 脚本 | 说明 |
|------|------|
| `test/api-test.sh` | API 接口测试 |
| `jmeter/` | JMeter 压力测试 |

### test/api-test.sh

```bash
# 运行 API 测试
./scripts/test/api-test.sh
```

### jmeter/

```bash
# 运行登录压力测试
./scripts/jmeter/run-all-tests.sh login

# 运行全部压力测试
./scripts/jmeter/run-all-tests.sh all
```

## 目录结构

```
scripts/
├── deploy.sh                 # 一键部署
├── deploy-swarm.sh           # Swarm 部署
├── backup.sh                 # 完整备份
├── db-auto-backup.sh         # 数据库自动备份
├── restore.sh                # 数据恢复
├── reset-db.sh               # 数据库重置
├── check-production-ready.sh # 生产检查
├── init-db/                  # 数据库初始化脚本
│   ├── init-database.sh      # 初始化脚本
│   └── *.sql                 # SQL 脚本
├── jmeter/                   # 压力测试
│   ├── run-all-tests.sh      # 运行脚本
│   └── *.jmx                 # 测试计划
├── test/                     # API 测试
│   └── api-test.sh           # 测试脚本
├── migration/                # 版本迁移脚本
└── backup/                   # 备份文件目录
```

