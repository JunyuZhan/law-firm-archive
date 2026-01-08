# 律师事务所管理系统 - 数据安全指南

## 数据存储架构

```
┌─────────────────────────────────────────────────────────────┐
│                     数据分布                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PostgreSQL (结构化数据)           MinIO (文件数据)          │
│  ├─ 用户/权限                      ├─ 合同文件               │
│  ├─ 客户信息                       ├─ 证据材料               │
│  ├─ 项目/案件                      ├─ 卷宗文档               │
│  ├─ 审批流程                       ├─ 附件/模板              │
│  └─ 操作日志                       └─ 图片/扫描件            │
│                                                             │
│  📊 数据量小，定时备份即可          📁 核心资产，需要冗余存储   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 一、MinIO 分布式部署（推荐）

对于文件存储，推荐使用 MinIO 分布式模式，提供数据冗余。

### 部署模式选择

| 模式 | 服务器数量 | 数据冗余 | 适用场景 |
|------|------------|----------|----------|
| 单节点 | 1 台 | ❌ 无 | 开发/测试 |
| 分布式 | 2-4 台 | ✅ 有 | **生产环境推荐** |
| 云存储 | 0 台 | ✅ 有 | 免运维 |

### 分布式部署（4节点）

**最小硬件要求：**
- 2-4 台服务器
- 每台至少 1 块独立磁盘用于 MinIO

```bash
# 1. 在每台服务器上初始化 Docker Swarm
# 主节点
docker swarm init --advertise-addr 192.168.50.10

# 从节点加入（使用主节点返回的 token）
docker swarm join --token <token> 192.168.50.10:2377

# 2. 部署 MinIO 集群
docker stack deploy -c docker/docker-compose.minio-cluster.yml law-firm-minio
```

### 单服务器 4 驱动器模式

如果只有 1 台服务器，可以使用 4 块磁盘：

```bash
# 挂载 4 块磁盘
mkdir -p /mnt/disk1 /mnt/disk2 /mnt/disk3 /mnt/disk4

# 修改 docker-compose 使用本地路径
volumes:
  - /mnt/disk1:/data  # minio1
  - /mnt/disk2:/data  # minio2
  - /mnt/disk3:/data  # minio3
  - /mnt/disk4:/data  # minio4
```

### 云存储替代方案

如果不想自建 MinIO，可以使用云存储：

| 服务商 | 产品 | 特点 |
|--------|------|------|
| 阿里云 | OSS | S3 兼容，国内首选 |
| 腾讯云 | COS | S3 兼容 |
| AWS | S3 | 全球化 |

后端配置（`application-prod.yml`）：
```yaml
minio:
  endpoint: https://oss-cn-hangzhou.aliyuncs.com
  access-key: ${ALIYUN_ACCESS_KEY}
  secret-key: ${ALIYUN_SECRET_KEY}
  bucket-name: law-firm-files
```

## 二、数据库自动备份

数据库数据量相对较小，使用定时备份即可。

### 快速设置

```bash
# 设置每日自动备份（凌晨 3:00）
./scripts/db-auto-backup.sh --schedule

# 查看备份状态
./scripts/db-auto-backup.sh --status
```

### 备份策略

```
📦 备份保留策略
├── daily/      # 每日备份，保留 7 天
├── weekly/     # 每周备份（周日），保留 4 周
└── monthly/    # 每月备份（1日），保留 12 个月
```

### 手动备份

```bash
# 智能备份（自动判断日/周/月）
./scripts/db-auto-backup.sh

# 指定类型
./scripts/db-auto-backup.sh daily
./scripts/db-auto-backup.sh weekly
./scripts/db-auto-backup.sh monthly

# 备份到指定目录
BACKUP_DIR=/mnt/nas/law-firm/db ./scripts/db-auto-backup.sh
```

### 同步到远程存储

编辑 `db-auto-backup.sh`，启用远程备份：

```bash
REMOTE_BACKUP_ENABLED=true
REMOTE_PATH="user@nas:/backup/law-firm/db/"
```

或使用 cron 定时同步：

```bash
# 每天 4 点同步到 NAS
0 4 * * * rsync -az /path/to/backups/ user@nas:/backup/law-firm/
```

## 三、数据恢复

### 恢复数据库

```bash
# 查看可用备份
ls -la backups/db/daily/

# 恢复最新备份
./scripts/restore.sh db backups/db/daily/2026-01-09_030000.sql.gz
```

### MinIO 数据恢复

分布式 MinIO 自动处理数据冗余，如单节点故障：

```bash
# 查看集群状态
docker exec law-firm-minio1 mc admin info local

# 修复数据
docker exec law-firm-minio1 mc admin heal local
```

## 四、安全配置清单

### 部署前

- [ ] 使用强密码（至少 32 位随机字符串）
- [ ] 数据库不暴露外部端口
- [ ] MinIO 开启访问日志
- [ ] 配置防火墙规则

### 运维检查

**每日：**
- 检查备份是否成功执行

**每周：**
- 验证备份文件完整性
- 检查磁盘空间

**每月：**
- 测试恢复流程
- 审计用户权限

## 五、架构对比

### 单机部署（适合小型律所）

```
┌─────────────────────────────────────┐
│           单台服务器                 │
│  ┌─────────┐  ┌─────────────────┐   │
│  │ App     │  │ PostgreSQL      │   │
│  │ Backend │  │ (自动备份到NAS) │   │
│  │ Frontend│  └─────────────────┘   │
│  └─────────┘  ┌─────────────────┐   │
│               │ MinIO (单节点)  │   │
│               │ + NAS 备份      │   │
│               └─────────────────┘   │
└─────────────────────────────────────┘
```

### 分布式部署（适合中型律所）

```
┌──────────────┐  ┌──────────────┐
│   服务器 1    │  │   服务器 2    │
│  ┌────────┐  │  │  ┌────────┐  │
│  │ App    │  │  │  │ MinIO2 │  │
│  │ MinIO1 │  │  │  │ MinIO4 │  │
│  │ PG主   │  │  │  │ PG从   │  │
│  └────────┘  │  │  └────────┘  │
└──────────────┘  └──────────────┘
        │                 │
        └────── NAS ──────┘
              (备份)
```

## 六、常见问题

### Q: MinIO 和 PostgreSQL 哪个更重要？

**A:** MinIO 存储的文件（合同、证据、卷宗）是律所的核心资产，一旦丢失无法恢复。PostgreSQL 存储的元数据可以重建，但会造成业务中断。两者都很重要，但文件数据更难恢复。

### Q: 需要分布式部署 PostgreSQL 吗？

**A:** 对于律所系统（用户量 < 100），单节点 PostgreSQL + 定时备份足够。分布式数据库增加运维复杂度，收益有限。

### Q: MinIO 开源版功能受限怎么办？

**A:** 可选方案：
1. 使用 MinIO 分布式版（本方案）
2. 切换到 SeaweedFS（开源活跃）
3. 使用云存储（阿里云 OSS）

### Q: 如何验证备份有效？

```bash
# 定期测试恢复
# 在测试环境恢复备份并验证
./scripts/restore.sh db backups/db/daily/latest.sql.gz
# 登录系统验证数据
```
