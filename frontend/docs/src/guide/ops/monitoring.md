# 监控告警

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

## 监控指标

建议监控以下指标：

| 指标 | 阈值 | 说明 |
|------|------|------|
| CPU 使用率 | < 80% | 服务器 CPU |
| 内存使用率 | < 85% | 服务器内存 |
| 磁盘使用率 | < 90% | 存储空间 |
| 数据库连接数 | < 100 | PostgreSQL 连接 |
| API 响应时间 | < 1s | 接口响应 |
| 错误率 | < 1% | 请求错误比例 |

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

## 告警配置

可集成以下告警渠道：

- 邮件通知
- 企业微信
- 钉钉
- Slack
