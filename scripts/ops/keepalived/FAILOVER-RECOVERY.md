# 故障切换后的恢复指南

## 📋 问题说明

### 场景：故障切换后的状态

```
初始状态：
  主服务器 (192.168.50.10) - Keepalived MASTER + PostgreSQL 主库
  从服务器 (192.168.50.11) - Keepalived BACKUP + PostgreSQL 从库

主服务器故障后：
  主服务器 (192.168.50.10) - 故障（离线）
  从服务器 (192.168.50.11) - Keepalived MASTER + PostgreSQL 主库（已提升）+ 虚拟IP

原主服务器修好后：
  主服务器 (192.168.50.10) - 恢复在线，但数据可能过期
  从服务器 (192.168.50.11) - 仍然是主服务器（Keepalived MASTER + PostgreSQL 主库）
```

### 关键问题

1. **原主服务器不会自动变成从服务器**
   - Keepalived 只会切换虚拟IP，不会自动恢复主从关系
   - PostgreSQL 主从复制需要手动重新配置

2. **如何识别当前主从角色**
   - 时间长了，可能忘记哪台是主服务器
   - 需要工具来识别当前角色

---

## 🔍 如何识别当前主从角色

### 方法一：使用检查脚本（推荐）⭐

```bash
# 在任何服务器执行
sudo /usr/local/bin/check-role.sh

# 或者从项目目录执行
./scripts/ops/keepalived/check-role.sh
```

**脚本会显示**：
- Keepalived 状态和角色
- PostgreSQL 容器状态
- PostgreSQL 数据库角色（主库/从库）
- 配置文件角色
- 综合判断结果

### 方法二：手动检查

#### 检查 Keepalived 角色

```bash
# 检查虚拟IP绑定（主服务器会有虚拟IP）
ip addr show eth0 | grep 192.168.50.100

# 如果看到虚拟IP，说明是当前主服务器
# 如果没有，说明是从服务器
```

#### 检查 PostgreSQL 角色

```bash
# 检查主库容器
docker exec law-firm-postgres-master psql -U law_admin -d law_firm \
  -c "SELECT pg_is_in_recovery();"
# 返回 f = 主库（可写）
# 返回 t = 从库（只读）

# 检查从库容器
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT pg_is_in_recovery();"
# 返回 f = 已提升为主库
# 返回 t = 仍然是从库
```

#### 检查配置文件

```bash
# 查看配置角色
grep "^NODE_ROLE=" /opt/law-firm/.env

# 查看主服务器IP配置
grep "^MASTER_IP=" /opt/law-firm/.env
```

---

## 🔄 原主服务器恢复后的处理

### 情况一：保持当前状态（推荐）⭐

**如果从服务器已经稳定运行为主服务器，可以保持现状**：

```bash
# 1. 原主服务器（现在是备用）重新配置为从服务器
# 在原主服务器执行
cd /opt/law-firm
./scripts/ops/keepalived/restore-original-master.sh <新主服务器IP>

# 例如：
./scripts/ops/keepalived/restore-original-master.sh 192.168.50.11
```

**脚本会自动完成**：
- ✅ 停止当前 PostgreSQL 容器
- ✅ 备份现有数据
- ✅ 从新主服务器复制数据
- ✅ 配置为从服务器
- ✅ 启动从库服务

### 情况二：切换回原主服务器（复杂）

**如果需要切换回原主服务器，需要手动操作**：

```bash
# ⚠️ 警告：此操作复杂，需要仔细规划

# 1. 在新主服务器（原从服务器）停止写入
# 2. 等待数据同步完成
# 3. 在原主服务器重新配置为主服务器
# 4. 在新主服务器重新配置为从服务器
# 5. 切换 Keepalived 虚拟IP
```

**建议**：除非有特殊需求，否则保持当前状态更简单。

---

## 📝 详细恢复步骤

### 步骤1：确认当前状态

```bash
# 在两台服务器都执行
./scripts/ops/keepalived/check-role.sh

# 确认：
# - 哪台服务器是当前主服务器（有虚拟IP）
# - 哪台服务器是当前从服务器（无虚拟IP）
```

### 步骤2：原主服务器恢复为从服务器

```bash
# 在原主服务器（现在是备用）执行
cd /opt/law-firm

# 1. 确认新主服务器IP（当前运行主库的服务器）
# 例如：192.168.50.11

# 2. 运行恢复脚本
sudo ./scripts/ops/keepalived/restore-original-master.sh 192.168.50.11

# 脚本会：
# - 备份现有数据
# - 从新主服务器复制数据
# - 配置为从服务器
# - 启动从库服务
```

### 步骤3：验证恢复

```bash
# 在原主服务器执行
# 1. 检查从库状态
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT pg_is_in_recovery();"
# 应该返回 t（true，表示是从库）

# 2. 检查复制状态
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT * FROM pg_stat_wal_receiver;"
# 应该看到复制连接信息

# 3. 检查 Keepalived 状态
sudo systemctl status keepalived
# 应该显示 BACKUP 状态
```

### 步骤4：更新 Keepalived 配置（可选）

**如果原主服务器的 Keepalived 优先级需要调整**：

```bash
# 在原主服务器执行
sudo nano /etc/keepalived/keepalived.conf

# 确保：
# - state BACKUP（从服务器）
# - priority 90（低于新主服务器的100）
# - auth_pass 和新主服务器一致
# - virtual_ipaddress 和新主服务器一致

# 重启 Keepalived
sudo systemctl restart keepalived
```

---

## ⚠️ 重要注意事项

### 1. 数据一致性

- **恢复前**：确保新主服务器的数据是最新的
- **恢复时**：脚本会自动从新主服务器复制数据
- **恢复后**：验证数据是否同步

### 2. Keepalived 配置

- **虚拟IP**：保持绑定在新主服务器（当前主服务器）
- **优先级**：原主服务器应该设置为 90（低于新主服务器的 100）
- **认证密码**：必须和新主服务器一致

### 3. 应用配置

- **Backend 连接**：应该连接虚拟IP，而不是直接连接服务器IP
- **如果使用虚拟IP**：无需修改应用配置
- **如果直接连接IP**：需要更新为新主服务器IP

### 4. 时间长了如何区分

**定期检查**：
```bash
# 每月执行一次角色检查
./scripts/ops/keepalived/check-role.sh > /var/log/role-check-$(date +%Y%m).log
```

**记录文档**：
- 记录每次故障切换的时间
- 记录当前主从服务器IP
- 记录虚拟IP地址

---

## 🔄 完整恢复流程示例

### 场景：原主服务器修好后恢复

```bash
# ============================================
# 当前状态（故障切换后）
# ============================================
# 原主服务器 (192.168.50.10) - 已修复，但数据过期
# 新主服务器 (192.168.50.11) - 当前主服务器（Keepalived MASTER + PostgreSQL 主库）

# ============================================
# 步骤1：确认当前状态
# ============================================
# 在新主服务器执行
cd /opt/law-firm
./scripts/ops/keepalived/check-role.sh
# 确认：当前是主服务器

# ============================================
# 步骤2：原主服务器恢复为从服务器
# ============================================
# 在原主服务器执行
cd /opt/law-firm
sudo ./scripts/ops/keepalived/restore-original-master.sh 192.168.50.11

# ============================================
# 步骤3：验证恢复
# ============================================
# 在原主服务器执行
docker exec law-firm-postgres-slave psql -U law_admin -d law_firm \
  -c "SELECT pg_is_in_recovery();"
# 应该返回 t（从库）

# ============================================
# 步骤4：更新 Keepalived（如果需要）
# ============================================
# 在原主服务器执行
sudo nano /etc/keepalived/keepalived.conf
# 确保 priority 90（低于新主服务器的 100）
sudo systemctl restart keepalived

# ============================================
# 最终状态
# ============================================
# 新主服务器 (192.168.50.11) - Keepalived MASTER + PostgreSQL 主库 + 虚拟IP
# 原主服务器 (192.168.50.10) - Keepalived BACKUP + PostgreSQL 从库
```

---

## 📊 主从角色识别表

| 检查项 | 主服务器 | 从服务器 |
|--------|---------|---------|
| **Keepalived 状态** | MASTER | BACKUP |
| **虚拟IP绑定** | ✅ 已绑定 | ❌ 未绑定 |
| **PostgreSQL 角色** | 主库（可写） | 从库（只读） |
| **pg_is_in_recovery()** | f（false） | t（true） |
| **配置文件 NODE_ROLE** | master | slave |
| **可以写入数据** | ✅ 是 | ❌ 否 |

---

## 🎯 最佳实践

### 1. 定期检查角色

```bash
# 创建定时任务，每月检查一次
# 编辑 crontab
crontab -e

# 添加：
0 0 1 * * /usr/local/bin/check-role.sh >> /var/log/role-check.log 2>&1
```

### 2. 记录故障切换

```bash
# 创建故障切换记录文件
echo "$(date): Failover occurred, new master: 192.168.50.11" >> /var/log/failover-history.log
```

### 3. 监控主从状态

```bash
# 使用监控工具（Prometheus + Grafana）
# 监控指标：
# - Keepalived 状态
# - PostgreSQL 主从角色
# - 复制延迟
# - 虚拟IP绑定状态
```

---

## 📚 相关文档

- [快速开始指南](./QUICK-START.md)
- [Keepalived 脚本说明](./README.md)
- [主从服务器部署文档](../../docker/DEPLOY-MASTER-SLAVE.md)

---

**最后更新**: 2026-01-31
