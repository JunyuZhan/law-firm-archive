# Keepalived 快速开始指南

> 💡 **前提条件**：主从服务器已经部署完成，PostgreSQL 主从复制正常工作。

## 🚀 快速安装（推荐）

### 方式一：使用自动安装脚本（最简单）⭐

#### 第一步：在主服务器安装 Keepalived

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 运行安装脚本
sudo ./scripts/ops/keepalived/install-keepalived.sh master 192.168.50.100 eth0

# 参数说明：
#   master          - 服务器角色
#   192.168.50.100  - 虚拟IP（改成你的虚拟IP）
#   eth0            - 网卡名称（使用 ip addr 查看，可能是 ens33、enp0s3 等）
```

**脚本会自动完成**：
- ✅ 安装 Keepalived
- ✅ 复制脚本到 `/usr/local/bin/`
- ✅ 生成配置文件
- ✅ 检查配置
- ✅ 启动服务

**安装完成后会显示**：
```
配置信息:
  角色: master
  虚拟IP: 192.168.50.100/24
  网卡: eth0
  优先级: 100
  认证密码: keepalived_password_xxxxxxxx
⚠️  重要: 这是随机生成的密码，请记录下来！
⚠️  从服务器安装时必须使用相同的密码！
```

**⚠️ 重要**：
- **必须记录显示的认证密码**（如果使用随机密码）
- 从服务器安装时必须使用**完全相同的虚拟IP和认证密码**
- 如果忘记密码，可以查看配置文件：`sudo cat /etc/keepalived/keepalived.conf | grep auth_pass`

---

#### 第二步：在从服务器安装 Keepalived

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 运行安装脚本（需要主服务器的认证密码）
sudo ./scripts/ops/keepalived/install-keepalived.sh slave 192.168.50.100 eth0 mypassword123

# 参数说明：
#   slave           - 服务器角色
#   192.168.50.100  - 虚拟IP（必须和主服务器一样）
#   eth0            - 网卡名称（使用 ip addr 查看）
#   mypassword123   - 认证密码（必须和主服务器一样）
```

**⚠️ 重要**：
- 虚拟IP必须和主服务器**完全一样**
- 认证密码必须和主服务器**完全一样**
- 如果主服务器安装时使用了随机密码，**必须使用主服务器显示的密码**
- 如果忘记主服务器的密码，可以在主服务器查看：`sudo cat /etc/keepalived/keepalived.conf | grep auth_pass`

---

### 方式二：手动安装（如果需要更多控制）

#### 第一步：在主服务器安装

```bash
# 1. 安装 Keepalived
sudo apt-get update
sudo apt-get install keepalived  # Ubuntu/Debian
# 或
sudo yum install keepalived      # CentOS/RHEL

# 2. 复制脚本
sudo cp scripts/ops/keepalived/*.sh /usr/local/bin/
sudo chmod +x /usr/local/bin/*.sh

# 3. 复制配置文件
sudo cp scripts/ops/keepalived/keepalived-master.conf.example /etc/keepalived/keepalived.conf

# 4. 编辑配置文件
sudo nano /etc/keepalived/keepalived.conf

# 修改以下参数：
#   - interface eth0              # 改成你的网卡名称
#   - auth_pass your_password     # 改成强密码（记住这个密码！）
#   - virtual_ipaddress 192.168.50.100/24  # 改成你的虚拟IP

# 5. 检查配置
sudo keepalived -t

# 6. 启动服务
sudo systemctl enable keepalived
sudo systemctl start keepalived

# 7. 查看状态
sudo systemctl status keepalived
```

#### 第二步：在从服务器安装

```bash
# 1. 安装 Keepalived（同上）
sudo apt-get update
sudo apt-get install keepalived

# 2. 复制脚本（同上）
sudo cp scripts/ops/keepalived/*.sh /usr/local/bin/
sudo chmod +x /usr/local/bin/*.sh

# 3. 复制配置文件
sudo cp scripts/ops/keepalived/keepalived-slave.conf.example /etc/keepalived/keepalived.conf

# 4. 编辑配置文件
sudo nano /etc/keepalived/keepalived.conf

# 修改以下参数（⚠️ 必须和主服务器一致）：
#   - interface eth0              # 改成你的网卡名称
#   - auth_pass your_password     # ⚠️ 必须和主服务器一样！
#   - virtual_ipaddress 192.168.50.100/24  # ⚠️ 必须和主服务器一样！
#   - priority 90                 # 必须低于主服务器（主服务器是100）

# 5. 检查配置
sudo keepalived -t

# 6. 启动服务
sudo systemctl enable keepalived
sudo systemctl start keepalived

# 7. 查看状态
sudo systemctl status keepalived
```

---

## ✅ 验证安装

### 1. 检查 Keepalived 服务状态

```bash
# 在主服务器和从服务器都执行
sudo systemctl status keepalived

# 应该看到：Active: active (running)
```

### 2. 检查虚拟IP是否绑定

```bash
# 在主服务器执行（应该看到虚拟IP）
ip addr show eth0

# 应该看到类似：
# inet 192.168.50.100/24 scope global secondary eth0

# 在从服务器执行（不应该看到虚拟IP，除非主服务器故障）
ip addr show eth0
```

### 3. 查看日志

```bash
# Keepalived 日志
sudo journalctl -u keepalived -f

# 健康检查日志
tail -f /var/log/keepalived-postgres-check.log

# 故障切换日志（如果有切换）
tail -f /var/log/postgres-failover.log
```

---

## 🧪 测试故障切换

### 测试1：停止主服务器 PostgreSQL 容器

```bash
# 在主服务器执行
docker stop law-firm-postgres-master

# 观察：
# 1. Keepalived 检测到 PostgreSQL 故障（约6秒）
# 2. 虚拟IP自动切换到从服务器（约30秒）
# 3. 从服务器自动提升为主库（约30-60秒）
# 4. 应用服务自动重启

# 查看切换日志
tail -f /var/log/postgres-failover.log
```

### 测试2：停止主服务器 Keepalived

```bash
# 在主服务器执行
sudo systemctl stop keepalived

# 观察：
# 1. 从服务器检测到主服务器心跳超时（约3秒）
# 2. 虚拟IP自动切换到从服务器（约30秒）
# 3. 从服务器自动提升为主库（约30-60秒）
```

### 测试3：恢复主服务器

```bash
# 在主服务器执行
# 1. 恢复 PostgreSQL（如果之前停止了）
docker start law-firm-postgres-master

# 2. 恢复 Keepalived
sudo systemctl start keepalived

# 观察：
# - 主服务器恢复后，虚拟IP会保持绑定在从服务器（因为从服务器现在是主服务器）
# - 如果需要切换回原主服务器，需要手动操作
```

---

## 📋 完整运行流程示例

### 场景：两台服务器已部署完成

```bash
# ============================================
# 主服务器 (192.168.50.10)
# ============================================
cd /opt/law-firm

# 1. 查看网卡名称
ip addr
# 假设网卡是 eth0

# 2. 安装 Keepalived（使用虚拟IP 192.168.50.100）
sudo ./scripts/ops/keepalived/install-keepalived.sh master 192.168.50.100 eth0

# 3. 记录显示的认证密码（如果使用随机密码）
# 或者手动设置密码：
sudo nano /etc/keepalived/keepalived.conf
# 修改 auth_pass 为你想要的密码，例如：mypassword123

# 4. 重启服务使密码生效
sudo systemctl restart keepalived

# 5. 验证虚拟IP已绑定
ip addr show eth0 | grep 192.168.50.100
# 应该看到虚拟IP

# ============================================
# 从服务器 (192.168.50.11)
# ============================================
cd /opt/law-firm

# 1. 查看网卡名称
ip addr
# 假设网卡是 eth0

# 2. 安装 Keepalived（使用相同的虚拟IP和密码）
sudo ./scripts/ops/keepalived/install-keepalived.sh slave 192.168.50.100 eth0 mypassword123

# 3. 验证服务运行
sudo systemctl status keepalived

# 4. 验证虚拟IP未绑定（主服务器正常时）
ip addr show eth0 | grep 192.168.50.100
# 不应该看到虚拟IP

# ============================================
# 测试故障切换
# ============================================
# 在主服务器停止 PostgreSQL
docker stop law-firm-postgres-master

# 等待约30-60秒，然后检查：
# 1. 从服务器是否接管了虚拟IP
ip addr show eth0 | grep 192.168.50.100  # 在从服务器执行

# 2. 从库是否提升为主库
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm -c "SELECT pg_is_in_recovery();"
# 应该返回 f（false，表示不是恢复模式，即已提升为主库）

# 3. 查看切换日志
tail -20 /var/log/postgres-failover.log  # 在从服务器执行
```

---

## ⚠️ 常见问题

### Q1: 安装脚本报错"网卡不存在"

**A**: 使用 `ip addr` 查看实际的网卡名称，可能是 `ens33`、`enp0s3`、`eth1` 等。

```bash
ip addr
# 找到类似 "2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP>" 的行
# 使用对应的网卡名称
```

### Q2: 从服务器无法连接主服务器

**A**: 检查防火墙和网络连接：

```bash
# 检查网络连通性
ping <主服务器IP>

# 检查防火墙
sudo ufw status          # Ubuntu/Debian
sudo firewall-cmd --list-all  # CentOS/RHEL

# Keepalived 需要 VRRP 协议（通常端口 112）
# 确保防火墙允许 VRRP 协议
```

### Q3: 虚拟IP无法绑定

**A**: 检查：
1. 虚拟IP是否被其他设备使用：`ping 192.168.50.100`
2. 网卡名称是否正确
3. Keepalived 配置是否正确：`sudo keepalived -t`
4. 查看日志：`sudo journalctl -u keepalived -n 50`

### Q4: 故障切换后，原主服务器恢复，会自动变成从服务器吗？

**A**: **不会自动**。需要手动恢复：

```bash
# 在原主服务器执行恢复脚本
cd /opt/law-firm
sudo ./scripts/ops/keepalived/restore-original-master.sh <新主服务器IP>

# 例如：
sudo ./scripts/ops/keepalived/restore-original-master.sh 192.168.50.11
```

**详细说明**：
- Keepalived 只会切换虚拟IP，不会自动恢复 PostgreSQL 主从关系
- 原主服务器恢复后，需要从新主服务器复制数据并重新配置为从服务器
- 详见 [故障恢复指南](./FAILOVER-RECOVERY.md)

### Q5: 时间长了，如何区分主从？

**A**: 使用角色检查脚本：

```bash
# 在任何服务器执行
sudo /usr/local/bin/check-role.sh

# 或从项目目录执行
./scripts/ops/keepalived/check-role.sh
```

**脚本会显示**：
- Keepalived 状态和角色
- PostgreSQL 容器状态
- PostgreSQL 数据库角色（主库/从库）
- 配置文件角色
- 综合判断结果

**手动检查方法**：
```bash
# 1. 检查虚拟IP（主服务器有虚拟IP）
ip addr show eth0 | grep 192.168.50.100

# 2. 检查 PostgreSQL 角色
docker exec law-firm-postgres-master psql -U law_admin -d law_firm \
  -c "SELECT pg_is_in_recovery();"
# 返回 f = 主库，返回 t = 从库
```

详见 [故障恢复指南](./FAILOVER-RECOVERY.md)

### Q5: 主服务器使用随机密码，如何获取？

**A**: 有几种方法：

```bash
# 方法1：查看配置文件
sudo cat /etc/keepalived/keepalived.conf | grep auth_pass

# 方法2：如果主服务器安装时显示了密码，直接使用

# 方法3：如果忘记了，可以重新生成配置
# 在主服务器执行：
sudo nano /etc/keepalived/keepalived.conf
# 修改 auth_pass 为你想要的密码
sudo systemctl restart keepalived
```

### Q6: 脚本文件不存在或复制失败？

**A**: 检查：

```bash
# 1. 确认在项目根目录
cd /opt/law-firm

# 2. 检查脚本文件是否存在
ls -la scripts/ops/keepalived/*.sh

# 3. 如果不存在，检查项目路径是否正确
pwd

# 4. 确认脚本目录结构
find . -name "install-keepalived.sh"
```

### Q7: Keepalived 已安装，会覆盖配置吗？

**A**: 
- 脚本会检测 Keepalived 是否已安装，如果已安装会跳过安装步骤
- **配置文件会被覆盖**，但会自动备份到 `/etc/keepalived/keepalived.conf.backup.时间戳`
- 如果需要保留现有配置，请先备份：`sudo cp /etc/keepalived/keepalived.conf /etc/keepalived/keepalived.conf.backup`

---

## 📚 相关文档

- [主从服务器部署文档](../../docker/DEPLOY-MASTER-SLAVE.md)
- [Keepalived 自动故障切换方案](../../docker/DEPLOY-MASTER-SLAVE-AUTO-FAILOVER.md)
- [Keepalived 脚本说明](./README.md)

---

**最后更新**: 2026-01-31
