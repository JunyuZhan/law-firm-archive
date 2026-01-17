# 监控告警

本章节介绍如何检查系统健康状态，并通过 Prometheus + Alertmanager 实现监控与告警。

## 健康检查

### 后端健康检查

```bash
curl http://localhost:5666/actuator/health
```

### 数据库检查

```bash
docker exec postgres pg_isready -U lawfirm
```

### Redis 检查

```bash
redis-cli ping
```

## 日志查看

### 后端日志

```bash
# 实时查看
tail -f logs/law-firm.log

# Docker 环境
docker logs -f law-firm-backend
```

### Nginx 日志

```bash
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

## 监控体系（Prometheus）

系统内置了 Prometheus 配置文件 `docker/prometheus/prometheus.yml`，默认抓取以下指标：

- Prometheus 自身：`localhost:9090`
- 后端服务：`backend:8080/api/actuator/prometheus`
- Redis：`redis-exporter:9121`
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
