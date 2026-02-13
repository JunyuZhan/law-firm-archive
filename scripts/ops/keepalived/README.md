# Keepalived 自动故障切换脚本

## 📋 概述

这个目录包含 Keepalived 自动故障切换所需的所有脚本和配置文件。

## 📁 文件说明

### 脚本文件

| 文件 | 用途 | 位置 |
|------|------|------|
| `check-postgres.sh` | PostgreSQL 健康检查脚本（Keepalived 使用） | `/usr/local/bin/check-postgres.sh` |
| `postgres-master.sh` | 切换为主服务器时执行 | `/usr/local/bin/postgres-master.sh` |
| `postgres-slave.sh` | 切换为从服务器时执行 | `/usr/local/bin/postgres-slave.sh` |
| `postgres-fault.sh` | 进入故障状态时执行 | `/usr/local/bin/postgres-fault.sh` |
| `check-role.sh` | 检查当前服务器的主从角色 | `/usr/local/bin/check-role.sh` |
| `restore-original-master.sh` | 恢复原主服务器为从服务器 | 项目目录执行 |
| `install-keepalived.sh` | 自动安装和配置 Keepalived | 项目目录执行 |

### 配置文件

| 文件 | 用途 | 位置 |
|------|------|------|
| `keepalived-master.conf.example` | 主服务器 Keepalived 配置模板 | `/etc/keepalived/keepalived.conf` |
| `keepalived-slave.conf.example` | 从服务器 Keepalived 配置模板 | `/etc/keepalived/keepalived.conf` |

## 🚀 安装步骤

### 1. 安装 Keepalived

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install keepalived

# CentOS/RHEL
sudo yum install keepalived
```

### 2. 复制脚本文件

```bash
# 在主服务器和从服务器都要执行
sudo cp check-postgres.sh /usr/local/bin/
sudo cp postgres-master.sh /usr/local/bin/
sudo cp postgres-slave.sh /usr/local/bin/
sudo cp postgres-fault.sh /usr/local/bin/

# 设置执行权限
sudo chmod +x /usr/local/bin/check-postgres.sh
sudo chmod +x /usr/local/bin/postgres-master.sh
sudo chmod +x /usr/local/bin/postgres-slave.sh
sudo chmod +x /usr/local/bin/postgres-fault.sh
```

### 3. 配置 Keepalived

#### 主服务器

```bash
# 复制配置文件
sudo cp keepalived-master.conf.example /etc/keepalived/keepalived.conf

# 编辑配置文件
sudo nano /etc/keepalived/keepalived.conf

# 修改以下参数：
# - interface: 改成你的实际网卡名称（ip addr 查看）
# - auth_pass: 改成强密码
# - virtual_ipaddress: 改成你的虚拟IP
```

#### 从服务器

```bash
# 复制配置文件
sudo cp keepalived-slave.conf.example /etc/keepalived/keepalived.conf

# 编辑配置文件
sudo nano /etc/keepalived/keepalived.conf

# 修改以下参数：
# - interface: 改成你的实际网卡名称
# - auth_pass: 必须和主服务器一样
# - virtual_ipaddress: 必须和主服务器一样
# - priority: 应该低于主服务器（例如 90）
```

### 4. 检查配置

```bash
# 检查配置文件语法
sudo keepalived -t

# 如果显示 "Configuration file is OK"，说明配置正确
```

### 5. 启动服务

```bash
# 启动 Keepalived
sudo systemctl start keepalived

# 设置开机自启
sudo systemctl enable keepalived

# 查看状态
sudo systemctl status keepalived
```

### 6. 查看日志

```bash
# Keepalived 日志
sudo journalctl -u keepalived -f

# 故障切换日志
tail -f /var/log/postgres-failover.log

# 健康检查日志
tail -f /var/log/keepalived-postgres-check.log
```

## 🔧 配置参数说明

### Keepalived 配置参数

| 参数 | 说明 | 主服务器 | 从服务器 |
|------|------|---------|---------|
| `state` | 初始状态 | `MASTER` | `BACKUP` |
| `priority` | 优先级 | `100` | `90`（更低） |
| `virtual_router_id` | 虚拟路由器ID | `51` | `51`（必须一致） |
| `auth_pass` | 认证密码 | 自定义 | 必须一致 |
| `virtual_ipaddress` | 虚拟IP | `192.168.50.100/24` | 必须一致 |

### 健康检查参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `interval` | 检查间隔（秒） | `2` |
| `weight` | 失败时优先级减分 | `-5` |
| `fall` | 连续失败次数才认为故障 | `3` |
| `rise` | 连续成功次数才认为恢复 | `2` |
| `timeout` | 脚本超时时间（秒） | `5` |

## 📊 工作流程

### 正常状态

```
主服务器（priority 100）
  ├── Keepalived MASTER
  ├── 虚拟IP绑定：192.168.50.100
  └── PostgreSQL 主库运行中

从服务器（priority 90）
  ├── Keepalived BACKUP
  └── PostgreSQL 从库运行中（复制主库数据）
```

### 主服务器故障

```
1. Keepalived 检测到主服务器心跳超时（3秒）
   ↓
2. 从服务器自动切换为 MASTER
   ↓
3. 虚拟IP自动绑定到从服务器
   ↓
4. 触发 postgres-master.sh 脚本
   ↓
5. 脚本执行：
   - 提升从库为主库（pg_promote）
   - 更新配置文件（.env）
   - 重启应用服务（backend/frontend）
   ↓
6. 切换完成（约30-60秒）
```

## ⚠️ 注意事项

1. **网络配置**
   - 确保两台服务器在同一网段
   - 确保虚拟IP没有被其他设备使用
   - 确保防火墙允许 VRRP 协议（通常端口 112）

2. **脚本路径**
   - 所有脚本必须放在 `/usr/local/bin/` 目录
   - 确保脚本有执行权限
   - 确保脚本中的路径正确（特别是项目目录）

3. **环境变量**
   - `postgres-master.sh` 脚本使用 `LAW_FIRM_PROJECT_DIR` 环境变量
   - 默认值是 `/opt/law-firm`
   - 如果项目在其他位置，需要设置环境变量或修改脚本

4. **日志文件**
   - 故障切换日志：`/var/log/postgres-failover.log`
   - 健康检查日志：`/var/log/keepalived-postgres-check.log`
   - Keepalived 日志：`journalctl -u keepalived`

5. **测试**
   - 建议先在测试环境验证
   - 可以手动停止主服务器 PostgreSQL 容器测试
   - 观察日志确认切换流程正常

## 🐛 故障排查

### 问题1：Keepalived 无法启动

```bash
# 检查配置语法
sudo keepalived -t

# 查看错误日志
sudo journalctl -u keepalived -n 50
```

### 问题2：虚拟IP 无法绑定

```bash
# 检查网卡名称是否正确
ip addr show

# 检查虚拟IP是否被占用
ping 192.168.50.100

# 检查防火墙
sudo ufw status
sudo firewall-cmd --list-all
```

### 问题3：健康检查失败

```bash
# 手动执行健康检查脚本
sudo /usr/local/bin/check-postgres.sh
echo $?  # 应该返回 0

# 查看健康检查日志
tail -f /var/log/keepalived-postgres-check.log
```

### 问题4：切换脚本执行失败

```bash
# 查看故障切换日志
tail -f /var/log/postgres-failover.log

# 手动执行切换脚本测试
sudo /usr/local/bin/postgres-master.sh
```

## 🔄 故障恢复

### 检查当前角色

```bash
# 在任何服务器执行
sudo /usr/local/bin/check-role.sh

# 或从项目目录执行
./scripts/ops/keepalived/check-role.sh
```

### 恢复原主服务器为从服务器

```bash
# 在原主服务器（已修复）执行
cd /opt/law-firm
sudo ./scripts/ops/keepalived/restore-original-master.sh <新主服务器IP>

# 例如：
sudo ./scripts/ops/keepalived/restore-original-master.sh 192.168.50.11
```

**详细说明**：详见 [故障恢复指南](./FAILOVER-RECOVERY.md)

---

## 📚 相关文档

- [快速开始指南](./QUICK-START.md) - 快速安装和使用
- [故障恢复指南](./FAILOVER-RECOVERY.md) - 故障切换后的恢复
- [主从自动故障切换方案](../docker/DEPLOY-MASTER-SLAVE-AUTO-FAILOVER.md)
- [主从服务器部署文档](../docker/DEPLOY-MASTER-SLAVE.md)
- [Keepalived 官方文档](https://www.keepalived.org/manpage.html)

---

**最后更新**: 2026-01-31
