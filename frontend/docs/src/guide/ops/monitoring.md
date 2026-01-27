# 监控告警

本章节介绍如何检查系统健康状态，并通过 Prometheus + Grafana 实现监控与告警。

---

## 📊 监控服务概览

系统使用 **Prometheus** 收集监控数据，**Grafana** 进行可视化展示。

### 访问地址

- **Prometheus**: `http://your-server-ip:9090` 或 `http://prometheus.example.com`
- **Grafana**: `http://your-server-ip:3000` 或 `http://grafana.example.com`

### 登录信息

**Prometheus**：
- ✅ **无需登录**：直接访问即可查看监控数据
- ⚠️ **安全建议**：如需保护，通过 Nginx 反向代理添加基本认证

**Grafana**：
- 用户名：`admin`
- 密码：`.env` 文件中的 `GRAFANA_PASSWORD`，如果没有设置则默认为 `admin`
- ⚠️ **首次登录后会要求修改密码**

---

## 🔍 健康检查

### 后端健康检查

```bash
curl http://localhost:5666/actuator/health
# 或
curl http://localhost/api/actuator/health
```

### 数据库检查

```bash
# Docker 环境
docker exec law-firm-postgres pg_isready -U law_admin -d law_firm

# 本地环境
pg_isready -U law_admin -d law_firm
```

### Redis 检查

```bash
# Docker 环境
docker exec law-firm-redis redis-cli ping

# 本地环境
redis-cli ping
```

---

## 📝 日志查看

### 后端日志

```bash
# Docker 环境
docker logs -f law-firm-backend

# 本地环境
tail -f logs/law-firm.log
```

### Nginx 日志

```bash
# Docker 环境
docker logs -f law-firm-nginx

# 本地环境
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

---

## 📊 Prometheus 监控

### 配置说明

系统内置了 Prometheus 配置文件 `docker/prometheus/prometheus.yml`，默认抓取以下指标：

- Prometheus 自身：`localhost:9090`
- 后端服务：`backend:8080/api/actuator/prometheus`
- Redis：`redis-exporter:9121`

### Prometheus 使用指南

**Prometheus 界面是英文的**，可以使用以下方法：

**方法一：使用浏览器翻译**（最简单）
- Chrome/Edge：右键 → "翻译为中文"
- 界面会自动翻译为中文

**方法二：使用 Grafana**（推荐）
- Grafana 支持中文界面
- 可以从 Prometheus 读取数据并以更友好的方式展示
- 详见下面的 Grafana 部分

### Prometheus 查询示例

```promql
# 检查服务是否在线
up

# 查询 HTTP 请求速率
rate(http_requests_total[5m])

# 查询数据库连接数
pg_stat_database_numbackends
```

---

## 📈 Grafana 监控

### 设置中文界面

1. **登录 Grafana**
   - 访问：`http://your-server-ip:3000`
   - 用户名：`admin`
   - 密码：`.env` 文件中的 `GRAFANA_PASSWORD`

2. **切换到中文**
   - 点击左下角用户图标（或右上角头像）
   - 选择 **"Preferences"**（偏好设置）
   - 找到 **"Language"**（语言）选项
   - 选择 **"中文（简体）"**
   - 点击 **"Save"**（保存）

详细步骤请参考：[Grafana 配置指南](./grafana.md)

### 数据源配置

Grafana 默认已配置 Prometheus 数据源：
- **名称**：Prometheus
- **URL**：`http://prometheus:9090`
- **访问**：Server（服务器模式）

### 仪表板

系统已预置监控仪表板，包括：
- 系统概览
- 应用性能
- 数据库监控
- Redis 监控

---

## 🚨 告警配置

### Alertmanager 配置

系统使用 Alertmanager 进行告警管理，配置文件位于 `docker/alertmanager/alertmanager.yml`。

### 告警规则

告警规则定义在 `docker/prometheus/alerts.yml`，包括：
- 服务下线告警
- 高 CPU 使用率告警
- 高内存使用率告警
- 数据库连接数告警

---

## 📚 相关文档

- [Grafana 配置指南](./grafana.md)
- [故障排查](./troubleshooting.md)
- [配置说明](./configuration.md)

---

**最后更新**: 2026-01-27
- PostgreSQL：`postgres-exporter:9187`
- MinIO 集群：`minio:9000/minio/v2/metrics/cluster`
- Node Exporter：`node-exporter:9100`

在生产部署中启动监控栈后，可访问：

- Prometheus：`http://<服务器IP>:9090`
- Grafana（如有部署）：`http://<服务器IP>:3000`

## 监控指标

建议监控以下指标：

| 指标         | 阈值     | 说明                  |
| ------------ | -------- | --------------------- |
| CPU 使用率   | < 80%    | 服务器 CPU            |
| 内存使用率   | < 85%    | 服务器内存            |
| 磁盘使用率   | < 90%    | 存储空间              |
| 数据库连接数 | < 100    | PostgreSQL 连接数     |
| API 响应时间 | P95 < 1s | 接口 95 分位耗时      |
| API 错误率   | < 1%     | HTTP 5xx/4xx 比例     |
| 登录失败次数 | 阈值自定 | 暴力破解/异常登录     |
| 队列长度     | 阈值自定 | 如有异步任务/消息队列 |

## 常用命令

```bash
# 查看服务状态
docker-compose ps

# 重启服务
docker-compose restart backend

# 查看资源使用
docker stats

# 查看磁盘使用
df -h

# 查看内存使用
free -h
```

## 告警配置（Alertmanager + 后端 Webhook）

Alertmanager 配置文件位于 `docker/alertmanager/alertmanager.yml`，通过 Webhook 调用后端 API 发送告警：

- Webhook 地址：`http://backend:8080/api/system/alert/webhook`
- 后端从数据库读取告警通知配置（邮件、企业微信、钉钉等）

告警路由示例：

- 严重告警（`severity=critical`）：立即通知，重复间隔 1 小时
- 安全相关告警（登录失败过多、帐号锁定、异常访问等）：单独路由，优先处理
- 一般警告（`severity=warning`）：聚合后定期通知

在系统前端「系统管理 → 系统配置」中，配置：

- 邮件服务器（SMTP）
- 告警接收邮箱
- 企业微信/钉钉 等渠道的 Webhook

完成后，即可实现「Prometheus → Alertmanager → 后端 Webhook → 邮件/IM」的完整告警链路。
