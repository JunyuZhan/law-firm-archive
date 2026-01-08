# 律师事务所管理系统 - 数据安全指南

## 数据安全架构

```
┌─────────────────────────────────────────────────────────────┐
│                     应用层安全                               │
│  • JWT 认证 • 权限控制 • 操作日志 • 数据脱敏                 │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                     数据层安全                               │
│  • PostgreSQL 加密连接 • 密码加密存储 • 敏感数据加密         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                     存储层安全                               │
│  • 定时备份 • 异地备份 • 数据卷隔离                          │
└─────────────────────────────────────────────────────────────┘
```

## 一、定时备份

### 快速设置

```bash
# 设置每日凌晨 2:00 自动备份
./scripts/backup.sh --schedule
```

### 手动备份

```bash
# 完整备份（数据库 + 文件）
./scripts/backup.sh

# 仅备份数据库
./scripts/backup.sh db

# 仅备份文件
./scripts/backup.sh files

# 备份到指定目录
BACKUP_DIR=/mnt/nas/law-firm-backup ./scripts/backup.sh
```

### 备份策略建议

| 备份类型 | 频率 | 保留时间 | 存储位置 |
|----------|------|----------|----------|
| 本地备份 | 每日 | 30天 | 服务器本地 |
| 异地备份 | 每日 | 90天 | NAS/云存储 |
| 归档备份 | 每月 | 永久 | 离线存储 |

### 异地备份配置

```bash
# 备份到 NAS（通过 rsync）
BACKUP_DIR=/tmp/law-firm-backup ./scripts/backup.sh
rsync -avz /tmp/law-firm-backup/ user@nas-server:/backup/law-firm/

# 备份到阿里云 OSS
ossutil cp -r ./backups/ oss://your-bucket/law-firm-backup/
```

## 二、数据恢复

### 查看可用备份

```bash
./scripts/restore.sh list
```

输出示例：
```
📅 2026-01-09
   └─ 🗄️  db_020000.sql.gz (15M)
   └─ 📁 minio_020000.tar.gz (1.2G)

📅 2026-01-08
   └─ 🗄️  db_020000.sql.gz (14M)
   └─ 📁 minio_020000.tar.gz (1.1G)
```

### 恢复操作

```bash
# 完整恢复指定日期
./scripts/restore.sh full 2026-01-09

# 仅恢复数据库
./scripts/restore.sh db ./backups/2026-01-09/db_020000.sql.gz

# 仅恢复文件
./scripts/restore.sh files ./backups/2026-01-09/minio_020000.tar.gz
```

## 三、数据库安全配置

### 生产环境密码要求

```bash
# 生成强密码
openssl rand -base64 32

# 在 docker/.env 中配置
DB_PASSWORD=<生成的强密码>
JWT_SECRET=<至少64字符的随机字符串>
MINIO_SECRET_KEY=<生成的强密码>
```

### PostgreSQL 安全加固

```sql
-- 限制连接（在 pg_hba.conf 中配置）
-- 仅允许内部网络访问
host    all    all    172.16.0.0/12    md5
host    all    all    192.168.0.0/16   md5

-- 禁止外部直接访问
# 不要在 docker-compose 中暴露 5432 端口
```

## 四、文件存储安全

### MinIO 替代方案

由于 MinIO 开源版功能受限，建议考虑：

| 方案 | 优点 | 缺点 |
|------|------|------|
| **本地文件系统** | 简单可靠 | 不支持分布式 |
| **SeaweedFS** | S3兼容，开源活跃 | 需要学习 |
| **阿里云 OSS** | 稳定可靠，免运维 | 有成本 |

### 切换到本地存储

如需切换到本地文件存储，修改后端配置：

```yaml
# application-prod.yml
storage:
  type: local
  local:
    base-path: /data/law-firm/files
```

## 五、灾难恢复计划

### RTO/RPO 目标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| RPO | 24小时 | 最多丢失1天数据 |
| RTO | 4小时 | 4小时内恢复服务 |

### 恢复步骤

1. **评估情况**
   - 确定故障范围（数据库/文件/全部）
   - 确定需要恢复的时间点

2. **准备环境**
   ```bash
   # 新服务器上部署
   git clone https://github.com/JunyuZhan/law-firm.git
   cd law-firm
   cp docker/env.example docker/.env
   vim docker/.env
   ./scripts/deploy.sh
   ```

3. **恢复数据**
   ```bash
   # 复制备份文件到新服务器
   scp -r user@backup-server:/backup/law-firm/2026-01-09 ./backups/
   
   # 执行恢复
   ./scripts/restore.sh full 2026-01-09
   ```

4. **验证恢复**
   - 登录系统检查
   - 验证关键数据完整性
   - 检查文件附件是否正常

## 六、安全检查清单

### 部署前检查

- [ ] 修改所有默认密码
- [ ] JWT_SECRET 至少 64 字符
- [ ] 数据库不对外暴露端口
- [ ] 配置防火墙规则
- [ ] 启用 HTTPS（如有域名）

### 运维检查（每周）

- [ ] 检查备份是否正常执行
- [ ] 验证备份文件完整性
- [ ] 检查磁盘空间
- [ ] 查看安全日志

### 定期检查（每月）

- [ ] 测试备份恢复流程
- [ ] 更新系统和依赖
- [ ] 审计用户权限
- [ ] 检查操作日志

## 七、联系方式

如遇紧急数据问题，请联系：
- 系统管理员：[填写联系方式]
- 技术支持：[填写联系方式]

