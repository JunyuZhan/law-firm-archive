# 监控服务登录指南

## 📊 Prometheus 和 Grafana 登录说明

---

## 🔍 Prometheus 监控

### 访问地址

```
http://prometheus.albertzhan.top
或
http://your-server-ip:9090
```

### 登录方式

**Prometheus 默认没有认证**，可以直接访问 ✅

- ✅ **无需登录**：直接打开即可查看监控数据
- ✅ **无需密码**：默认配置下无需认证
- ✅ **这是正常的**：Prometheus 本身不提供认证功能，如需认证可通过 Nginx 反向代理添加

### 主要功能

1. **查询指标**
   - 在顶部搜索框输入 PromQL 查询语句
   - 例如：`up`、`rate(http_requests_total[5m])`

2. **查看目标**
   - 访问：`http://prometheus.albertzhan.top/targets`
   - 查看所有监控目标的状态

3. **查看图表**
   - 访问：`http://prometheus.albertzhan.top/graph`
   - 使用 PromQL 查询并可视化数据

### 安全建议

⚠️ **生产环境建议**：
- 如果通过公网访问，建议添加认证
- 或使用 Cloudflare Access 保护
- 或仅内网访问（关闭 Cloudflare 代理）

---

## 📈 Grafana 监控面板

### 访问地址

```
http://grafana.albertzhan.top
或
http://your-server-ip:3000
```

### 登录信息

**默认账号**：
- **用户名**: `admin`
- **密码**: 查看 `.env` 文件中的 `GRAFANA_PASSWORD`

**如果 `.env` 文件中没有设置 `GRAFANA_PASSWORD`**：
- 默认密码是：**`admin`**
- ⚠️ Grafana 会显示警告："Continuing to use the default password exposes you to security risks"

### 密码配置

#### 方式一：查看环境变量

```bash
# 在服务器上查看
cd /opt/law-firm
cat .env | grep GRAFANA_PASSWORD
```

#### 方式二：查看容器环境变量

```bash
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD
```

#### 方式三：如果没有设置

如果 `.env` 文件中没有设置 `GRAFANA_PASSWORD`：
- 默认密码是：**`admin`**
- ⚠️ **可以使用默认密码登录**，但会显示安全警告
- ⚠️ **首次登录后会要求修改密码**

### 登录步骤

1. **打开 Grafana**
   ```
   http://grafana.albertzhan.top
   ```

2. **输入登录信息**
   - 用户名：`admin`
   - 密码：`.env` 文件中的 `GRAFANA_PASSWORD`（或默认 `admin`）

3. **首次登录**
   - 如果使用默认密码 `admin`，会提示修改密码
   - 建议设置强密码

### 主要功能

1. **查看仪表板**
   - 登录后可以看到预配置的仪表板
   - 查看系统性能指标

2. **数据源配置**
   - 已自动配置 Prometheus 数据源
   - 访问：Configuration → Data Sources

3. **创建自定义仪表板**
   - 可以创建自定义监控面板
   - 访问：Create → Dashboard

### 修改密码

#### 方式一：通过环境变量（推荐）

```bash
# 1. 编辑 .env 文件
cd /opt/law-firm
nano .env

# 2. 设置 GRAFANA_PASSWORD
GRAFANA_PASSWORD=your-strong-password-here

# 3. 重启 Grafana 容器
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana
```

#### 方式二：通过 Grafana Web 界面

1. 登录 Grafana
2. 点击右上角用户图标 → Profile
3. 修改密码并保存

#### 方式三：通过 Grafana CLI

```bash
# 进入 Grafana 容器
docker exec -it law-firm-grafana bash

# 使用 grafana-cli 重置密码
grafana-cli admin reset-admin-password your-new-password
```

---

## 🔐 安全配置建议

### Prometheus 安全

#### 添加基本认证（可选）

如果需要为 Prometheus 添加认证，可以：

1. **使用 Nginx 反向代理添加认证**

```nginx
# 在 Nginx 配置中添加
location /prometheus/ {
    proxy_pass http://prometheus:9090/;
    
    # 基本认证
    auth_basic "Prometheus Access";
    auth_basic_user_file /etc/nginx/.htpasswd;
}
```

2. **创建密码文件**

```bash
# 安装 htpasswd
sudo apt-get install apache2-utils

# 创建密码文件
sudo htpasswd -c /etc/nginx/.htpasswd prometheus
# 输入密码
```

### Grafana 安全

#### 当前配置

- ✅ **已禁用用户注册**：`GF_USERS_ALLOW_SIGN_UP=false`
- ✅ **密码通过环境变量配置**：`GF_SECURITY_ADMIN_PASSWORD`
- ⚠️ **建议修改默认密码**

#### 安全建议

1. **修改默认密码**
   ```bash
   # 在 .env 文件中设置强密码
   GRAFANA_PASSWORD=your-strong-password-here
   ```

2. **限制访问**
   - 关闭 Cloudflare 代理（仅内网访问）
   - 或使用 Cloudflare Access 保护

3. **定期更新密码**
   - 建议每 3-6 个月更新一次密码

---

## 📋 快速参考

### Prometheus

| 项目 | 值 |
|------|-----|
| **访问地址** | `http://prometheus.albertzhan.top` |
| **用户名** | 无需 |
| **密码** | 无需 |
| **默认端口** | 9090 |

### Grafana

| 项目 | 值 |
|------|-----|
| **访问地址** | `http://grafana.albertzhan.top` |
| **用户名** | `admin` |
| **密码** | `.env` 文件中的 `GRAFANA_PASSWORD`（默认：`admin`） |
| **默认端口** | 3000 |

---

## 🔧 常见问题

### 1. 忘记 Grafana 密码

**解决方案**：

```bash
# 方法一：查看环境变量
cat /opt/law-firm/.env | grep GRAFANA_PASSWORD

# 方法二：重置密码
docker exec -it law-firm-grafana grafana-cli admin reset-admin-password new-password

# 方法三：修改环境变量后重启
# 编辑 .env 文件，设置 GRAFANA_PASSWORD，然后重启容器
```

### 2. Prometheus 无法访问

**检查步骤**：

```bash
# 1. 检查容器是否运行
docker ps | grep prometheus

# 2. 检查端口是否监听
netstat -tlnp | grep 9090

# 3. 检查防火墙
sudo ufw status

# 4. 检查 FRPC 配置
# 确认 prometheus.albertzhan.top 已配置
```

### 3. Grafana 登录失败

**检查步骤**：

```bash
# 1. 检查容器日志
docker logs law-firm-grafana

# 2. 检查环境变量
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD

# 3. 重置密码
docker exec -it law-firm-grafana grafana-cli admin reset-admin-password admin
```

---

## 📚 相关文档

- [FRPC 配置示例](./FRPC_CONFIG_EXAMPLE.md)
- [生产环境容器清单](./PRODUCTION_CONTAINERS.md)
- [部署脚本使用指南](./DEPLOYMENT_SCRIPTS_GUIDE.md)
