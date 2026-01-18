# Grafana 密码问题解决指南

## 🔍 问题：`.env` 文件中没有 `GRAFANA_PASSWORD`

如果执行 `cat .env | grep GRAFANA_PASSWORD` 没有显示密码，说明：

1. **`.env` 文件中没有设置 `GRAFANA_PASSWORD`**
2. **Grafana 使用默认密码：`admin`**

---

## ✅ 解决方案

### 方法一：使用默认密码登录

**默认登录信息**：
- 用户名：`admin`
- 密码：`admin`

⚠️ **首次登录后会要求修改密码**

### 方法二：查看容器中的实际密码

```bash
# 查看 Grafana 容器的环境变量
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD
```

如果没有设置，会显示默认值或空值。

### 方法三：设置密码（推荐）

#### 步骤 1: 编辑 `.env` 文件

```bash
cd /opt/law-firm
nano .env
```

#### 步骤 2: 添加或修改 `GRAFANA_PASSWORD`

```bash
# 在文件末尾添加（如果不存在）
GRAFANA_PASSWORD=your-strong-password-here

# 或修改现有值（如果存在但为空）
GRAFANA_PASSWORD=your-strong-password-here
```

#### 步骤 3: 重启 Grafana 容器

```bash
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana
```

#### 步骤 4: 验证

```bash
# 查看环境变量
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD

# 应该显示：
# GF_SECURITY_ADMIN_PASSWORD=your-strong-password-here
```

---

## 🔐 重置 Grafana 密码

如果忘记了密码，可以通过以下方式重置：

### 方法一：通过 Grafana CLI（推荐）

```bash
# 进入 Grafana 容器
docker exec -it law-firm-grafana bash

# 重置密码
grafana-cli admin reset-admin-password your-new-password

# 退出容器
exit
```

### 方法二：通过环境变量重置

```bash
# 1. 编辑 .env 文件
cd /opt/law-firm
nano .env

# 2. 设置新密码
GRAFANA_PASSWORD=your-new-password-here

# 3. 重启 Grafana
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana
```

### 方法三：删除数据卷重新初始化（⚠️ 会丢失所有配置）

```bash
# ⚠️ 警告：这会删除所有 Grafana 配置和仪表板

# 1. 停止 Grafana
docker stop law-firm-grafana

# 2. 删除数据卷
docker volume rm grafana_data

# 3. 重新启动（会使用新的密码）
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml up -d grafana
```

---

## 📋 快速检查清单

### 检查当前密码配置

```bash
# 1. 检查 .env 文件
cd /opt/law-firm
cat .env | grep GRAFANA_PASSWORD

# 2. 检查容器环境变量
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD

# 3. 检查容器是否运行
docker ps | grep grafana
```

### 如果没有设置密码

**使用默认密码登录**：
- 用户名：`admin`
- 密码：`admin`

然后：
1. 登录后立即修改密码
2. 或在 `.env` 文件中设置 `GRAFANA_PASSWORD`

---

## 🚀 一键设置密码脚本

创建脚本快速设置密码：

```bash
# 创建脚本
cat > /opt/law-firm/set-grafana-password.sh << 'EOF'
#!/bin/bash
# 设置 Grafana 密码

if [ -z "$1" ]; then
    echo "用法: $0 <新密码>"
    exit 1
fi

NEW_PASSWORD="$1"
ENV_FILE="/opt/law-firm/.env"

# 检查 .env 文件是否存在
if [ ! -f "$ENV_FILE" ]; then
    echo "错误: .env 文件不存在"
    exit 1
fi

# 检查是否已有 GRAFANA_PASSWORD
if grep -q "^GRAFANA_PASSWORD=" "$ENV_FILE"; then
    # 更新现有密码
    sed -i "s|^GRAFANA_PASSWORD=.*|GRAFANA_PASSWORD=$NEW_PASSWORD|" "$ENV_FILE"
    echo "✓ 已更新 GRAFANA_PASSWORD"
else
    # 添加新密码
    echo "GRAFANA_PASSWORD=$NEW_PASSWORD" >> "$ENV_FILE"
    echo "✓ 已添加 GRAFANA_PASSWORD"
fi

# 重启 Grafana
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana

echo "✓ Grafana 已重启，新密码已生效"
echo "用户名: admin"
echo "密码: $NEW_PASSWORD"
EOF

# 设置执行权限
chmod +x /opt/law-firm/set-grafana-password.sh

# 使用示例
# ./set-grafana-password.sh your-new-password
```

---

## 📝 总结

### 当前情况

如果 `cat .env | grep GRAFANA_PASSWORD` 没有显示：
- ✅ **默认密码是 `admin`**
- ✅ **可以直接使用 `admin/admin` 登录**
- ⚠️ **建议设置强密码**

### 推荐操作

1. **立即登录**（使用默认密码）
   ```
   用户名: admin
   密码: admin
   ```

2. **设置强密码**
   ```bash
   # 编辑 .env 文件
   cd /opt/law-firm
   nano .env
   
   # 添加或修改
   GRAFANA_PASSWORD=your-strong-password-here
   
   # 重启 Grafana
   cd docker
   docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana
   ```

3. **验证**
   ```bash
   # 查看环境变量
   docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD
   ```

---

## 🔗 相关文档

- [监控服务登录指南](./MONITORING_LOGIN_GUIDE.md)
- [生产环境配置检查报告](./PRODUCTION_CONFIG_CHECK_REPORT.md)
