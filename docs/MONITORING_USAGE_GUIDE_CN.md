# 监控服务使用指南（中文）

## 📊 Prometheus 和 Grafana 中文使用指南

---

## 🌐 设置中文界面

### Grafana 设置中文

**详细设置步骤请参考**：[Grafana 中文界面设置指南](./GRAFANA_CHINESE_SETUP.md)

**快速步骤**：

1. **登录 Grafana**
   - 访问：`http://grafana.albertzhan.top` 或 `http://your-server-ip:3000`
   - 用户名：`admin`
   - 密码：`.env` 文件中的 `GRAFANA_PASSWORD`

2. **切换到中文**
   - 点击左下角用户图标（或右上角头像）
   - 选择 **"Preferences"**（偏好设置）
   - 在 **"Language"**（语言）下拉菜单中选择 **"中文（简体）"** 或 **"中文（繁体）"**
   - 点击 **"Save"**（保存）

3. **界面会立即切换为中文**

**提示**：生产环境已配置默认中文（`GF_DEFAULT_LANGUAGE=zh-Hans`），重启容器后新用户将默认使用中文界面。

### Prometheus 设置中文

⚠️ **Prometheus 本身不支持中文界面**，但可以通过以下方式：
- 使用浏览器翻译功能（Chrome/Edge 右键 → 翻译）
- 或主要使用 Grafana 查看（推荐，支持中文）

---

## 📈 Prometheus 基本使用

### 访问地址

```
http://prometheus.albertzhan.top
```

### 主要功能

#### 1. 查询指标（Graph）

**位置**：顶部导航栏 → **"Graph"**

**常用查询**：

```promql
# 查看所有服务是否运行
up

# 查看后端服务状态
up{job="law-firm-backend"}

# 查看 JVM 内存使用
jvm_memory_used_bytes{application="law-firm-management"}

# 查看 HTTP 请求总数
http_server_requests_seconds_count

# 查看数据库连接池使用情况
hikari_connections_active
```

**操作步骤**：
1. 在搜索框输入查询语句
2. 点击 **"Execute"**（执行）
3. 查看图表或表格结果

#### 2. 查看监控目标（Targets）

**位置**：顶部导航栏 → **"Status"** → **"Targets"**

**功能**：
- 查看所有被监控的服务
- 检查服务是否正常（绿色 = 正常，红色 = 异常）

**应该看到**：
- `law-firm-backend` - 后端服务监控

#### 3. 查看配置（Configuration）

**位置**：顶部导航栏 → **"Status"** → **"Configuration"**

**功能**：
- 查看 Prometheus 的配置文件
- 检查监控目标配置

---

## 📊 Grafana 基本使用

### 访问地址

```
http://grafana.albertzhan.top
```

### 主要功能

#### 1. 查看仪表板（Dashboard）

**位置**：左侧菜单 → **"Dashboards"**（仪表板）→ **"Browse"**（浏览）

**操作**：
- 点击仪表板名称打开
- 查看各种监控图表

**预配置的仪表板**：
- Spring Boot Overview - Spring Boot 应用监控

#### 2. 创建自定义仪表板

**步骤**：
1. 点击左侧菜单 **"+"** → **"Create"**（创建）→ **"Dashboard"**（仪表板）
2. 点击 **"Add visualization"**（添加可视化）
3. 选择数据源：**Prometheus**
4. 输入 PromQL 查询语句
5. 选择图表类型（折线图、柱状图等）
6. 点击 **"Apply"**（应用）

#### 3. 配置数据源

**位置**：左侧菜单 → **"Configuration"**（配置）→ **"Data Sources"**（数据源）

**预配置的数据源**：
- Prometheus - 已自动配置，指向 `http://backend:8080/api/actuator/prometheus`

**如果需要手动配置**：
1. 点击 **"Add data source"**（添加数据源）
2. 选择 **"Prometheus"**
3. URL 填写：`http://prometheus:9090`
4. 点击 **"Save & Test"**（保存并测试）

---

## 🔍 常用监控指标

### 后端服务监控

#### 1. 服务健康状态

**Prometheus 查询**：
```promql
up{job="law-firm-backend"}
```

**说明**：
- `1` = 服务正常
- `0` = 服务异常

#### 2. JVM 内存使用

**Prometheus 查询**：
```promql
# 已使用内存（MB）
jvm_memory_used_bytes{application="law-firm-management"} / 1024 / 1024

# 最大内存（MB）
jvm_memory_max_bytes{application="law-firm-management"} / 1024 / 1024
```

#### 3. HTTP 请求统计

**Prometheus 查询**：
```promql
# 请求总数
http_server_requests_seconds_count

# 请求速率（每秒）
rate(http_server_requests_seconds_count[5m])
```

#### 4. 数据库连接池

**Prometheus 查询**：
```promql
# 活跃连接数
hikari_connections_active

# 空闲连接数
hikari_connections_idle

# 总连接数
hikari_connections
```

#### 5. 响应时间

**Prometheus 查询**：
```promql
# 平均响应时间（秒）
http_server_requests_seconds_sum / http_server_requests_seconds_count

# P95 响应时间
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

---

## 📋 常用操作步骤

### 在 Grafana 中查看后端服务状态

1. **登录 Grafana**
   ```
   http://grafana.albertzhan.top
   用户名: admin
   密码: .env 文件中的 GRAFANA_PASSWORD
   ```

2. **切换到中文**
   - 用户图标 → Preferences → Language → 中文（简体）

3. **查看仪表板**
   - 左侧菜单 → Dashboards → Browse
   - 点击 "Spring Boot Overview"

4. **查看关键指标**
   - JVM 内存使用
   - HTTP 请求统计
   - 数据库连接池状态

### 在 Prometheus 中查询指标

1. **访问 Prometheus**
   ```
   http://prometheus.albertzhan.top
   ```

2. **查询服务状态**
   - 在搜索框输入：`up{job="law-firm-backend"}`
   - 点击 "Execute"
   - 查看结果（1 = 正常，0 = 异常）

3. **查看图表**
   - 点击 "Graph" 标签
   - 查看历史趋势

---

## 🎯 常用监控场景

### 场景 1：检查服务是否正常运行

**Prometheus**：
```promql
up{job="law-firm-backend"}
```
结果应该是 `1`（正常）

**Grafana**：
- 查看仪表板中的服务状态面板
- 绿色 = 正常，红色 = 异常

### 场景 2：检查内存使用情况

**Prometheus**：
```promql
jvm_memory_used_bytes{application="law-firm-management"} / 1024 / 1024
```
查看已使用内存（MB）

**Grafana**：
- 查看 JVM 内存图表
- 关注内存使用趋势

### 场景 3：检查 API 性能

**Prometheus**：
```promql
rate(http_server_requests_seconds_count[5m])
```
查看请求速率

**Grafana**：
- 查看 HTTP 请求统计图表
- 关注响应时间

### 场景 4：检查数据库连接

**Prometheus**：
```promql
hikari_connections_active
```
查看活跃连接数

**Grafana**：
- 查看数据库连接池图表

---

## 🔧 故障排查

### 问题 1：Prometheus 显示服务为 Down

**检查步骤**：
```bash
# 1. 检查后端服务是否运行
docker ps | grep backend

# 2. 检查健康检查端点
curl http://localhost/api/actuator/health

# 3. 检查 Prometheus 配置
cat docker/prometheus/prometheus.yml
```

### 问题 2：Grafana 无法连接 Prometheus

**检查步骤**：
```bash
# 1. 检查 Prometheus 是否运行
docker ps | grep prometheus

# 2. 检查 Prometheus 是否可访问
curl http://localhost:9090

# 3. 检查 Grafana 数据源配置
# 登录 Grafana → Configuration → Data Sources
```

### 问题 3：看不到监控数据

**可能原因**：
1. 后端服务未启用 Actuator
2. Prometheus 端点未暴露
3. 数据源配置错误

**解决方案**：
```bash
# 检查后端 Actuator 配置
curl http://localhost/api/actuator/prometheus

# 应该返回 Prometheus 格式的指标数据
```

---

## 📚 常用 PromQL 查询示例

### 服务健康

```promql
# 所有服务状态
up

# 后端服务状态
up{job="law-firm-backend"}
```

### 内存监控

```promql
# 已使用内存（MB）
jvm_memory_used_bytes{application="law-firm-management"} / 1024 / 1024

# 内存使用率（%）
(jvm_memory_used_bytes{application="law-firm-management"} / jvm_memory_max_bytes{application="law-firm-management"}) * 100
```

### CPU 监控

```promql
# CPU 使用率
system_cpu_usage

# 进程 CPU 使用率
process_cpu_usage
```

### HTTP 请求监控

```promql
# 请求总数
http_server_requests_seconds_count

# 请求速率（每秒）
rate(http_server_requests_seconds_count[5m])

# 平均响应时间（秒）
http_server_requests_seconds_sum / http_server_requests_seconds_count
```

### 数据库监控

```promql
# 活跃连接数
hikari_connections_active

# 连接池使用率
(hikari_connections_active / hikari_connections_max) * 100
```

---

## 🎨 Grafana 仪表板创建示例

### 创建内存监控面板

1. **创建新仪表板**
   - 点击 **"+"** → **"Create"** → **"Dashboard"**

2. **添加面板**
   - 点击 **"Add visualization"**
   - 选择数据源：**Prometheus**

3. **配置查询**
   ```
   查询语句：jvm_memory_used_bytes{application="law-firm-management"} / 1024 / 1024
   图例：{{area}} - 已使用内存 (MB)
   ```

4. **选择图表类型**
   - 选择 **"Time series"**（时间序列）
   - 或 **"Stat"**（统计）

5. **保存**
   - 点击 **"Apply"**（应用）
   - 点击 **"Save dashboard"**（保存仪表板）

---

## 📖 中文术语对照

### Prometheus 界面

| 英文 | 中文 |
|------|------|
| Graph | 图表 |
| Status | 状态 |
| Targets | 目标 |
| Configuration | 配置 |
| Alerts | 告警 |
| Execute | 执行 |
| Graph | 图表 |
| Table | 表格 |

### Grafana 界面

| 英文 | 中文 |
|------|------|
| Dashboard | 仪表板 |
| Data Sources | 数据源 |
| Users | 用户 |
| Teams | 团队 |
| Plugins | 插件 |
| Preferences | 偏好设置 |
| Sign out | 退出登录 |
| Create | 创建 |
| Browse | 浏览 |
| Search | 搜索 |
| Add visualization | 添加可视化 |
| Time series | 时间序列 |
| Stat | 统计 |
| Table | 表格 |
| Gauge | 仪表盘 |
| Bar chart | 柱状图 |
| Pie chart | 饼图 |

---

## 🔗 相关文档

- [Grafana 中文界面设置指南](./GRAFANA_CHINESE_SETUP.md) - 详细的中文设置步骤
- [监控服务登录指南](./MONITORING_LOGIN_GUIDE.md)
- [监控认证说明](./MONITORING_AUTH_SUMMARY.md)
- [生产环境容器清单](./PRODUCTION_CONTAINERS.md)
