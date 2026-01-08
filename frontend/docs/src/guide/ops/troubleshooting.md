# 故障排查

## 常见问题

### 服务无法启动

1. 检查端口占用
```bash
lsof -i :5666
```

2. 检查日志
```bash
docker logs law-firm-backend
```

3. 检查数据库连接
```bash
psql -U lawfirm -h localhost -d lawfirm
```

### 数据库连接失败

```bash
# 检查 PostgreSQL 状态
docker exec postgres pg_isready

# 检查连接
psql -U lawfirm -h localhost -d lawfirm -c "SELECT 1"
```

### Redis 连接失败

```bash
redis-cli -h localhost ping
```

### 文件上传失败

1. 检查 MinIO 服务状态
2. 检查存储空间
3. 检查文件大小限制（默认 50MB）

### 前端页面空白

1. 检查浏览器控制台错误
2. 检查 API 地址配置
3. 清除浏览器缓存

## 性能问题

### 响应慢

1. 检查数据库慢查询
```sql
SELECT * FROM pg_stat_activity WHERE state = 'active';
```

2. 检查 Redis 缓存命中率

3. 检查服务器资源使用

### 内存不足

```bash
# 查看内存使用
free -h

# 调整 JVM 参数
java -Xmx2g -jar law-firm-backend.jar
```

## 数据问题

### 重置数据库

```bash
# 执行重置脚本
./scripts/reset-db.sh
```

### 修复权限

如果权限配置出错，可以重新执行初始化脚本：

```bash
psql -U lawfirm -d lawfirm < scripts/init-db/20-system-init-data.sql
```

## 联系支持

如无法解决，请联系技术支持并提供：

1. 错误日志
2. 操作步骤
3. 系统环境信息
