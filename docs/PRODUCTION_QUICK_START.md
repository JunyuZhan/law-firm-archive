# 生产环境快速部署指南

## 🚀 快速开始

### 1. 运行检查脚本

部署前，先运行检查脚本确保配置正确：

```bash
./scripts/check-production-ready.sh
```

如果检查通过，可以继续部署；如果有错误，请先修复。

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp docker/env.example docker/.env

# 编辑配置文件（必须修改密码和密钥！）
vim docker/.env
```

**必须修改的配置**：
- `JWT_SECRET` - 生成命令：`openssl rand -base64 64`
- `DB_PASSWORD` - 强密码（16位以上）
- `MINIO_SECRET_KEY` - 修改默认密钥

### 3. 一键部署

```bash
./scripts/deploy.sh
```

部署脚本会自动：
1. ✅ 检查 Docker 环境
2. ✅ 验证配置文件
3. ✅ 运行生产环境检查
4. ✅ 构建并启动所有服务

---

## 📋 详细检查清单

完整的检查清单请参考：[生产环境部署检查清单](./PRODUCTION_DEPLOYMENT_CHECKLIST.md)

### 关键检查项（必须完成）

#### 🔴 安全配置（最高优先级）

- [ ] JWT 密钥已设置为强随机值（至少64字符）
- [ ] 数据库密码已设置为强密码（16位以上）
- [ ] MinIO 访问密钥已修改（不是默认的 minioadmin）
- [ ] Swagger UI 已禁用（`SWAGGER_ENABLED=false`）
- [ ] HTTPS 已配置（如需要）

#### 🟡 应用配置

- [ ] 日志级别已设置为 `info`（生产环境）
- [ ] 数据库连接池大小已调整
- [ ] 文件上传大小限制已配置

#### 🟢 数据库

- [ ] 数据库已初始化
- [ ] 默认管理员密码已修改
- [ ] 自动备份已配置

#### 🔵 监控和备份

- [ ] 监控系统已配置
- [ ] 告警规则已设置
- [ ] 备份策略已配置并测试

---

## 🔧 部署后验证

### 1. 检查服务状态

```bash
cd docker
docker compose -f docker-compose.prod.yml ps
```

所有服务状态应为 `Up`。

### 2. 检查健康状态

```bash
curl http://localhost/api/actuator/health
```

应返回 `{"status":"UP"}`。

### 3. 测试登录

访问前端地址，使用默认管理员账户登录，确认：
- [ ] 登录功能正常
- [ ] 验证码功能正常
- [ ] 权限控制正常

### 4. 检查日志

```bash
# 查看后端日志
docker compose -f docker-compose.prod.yml logs -f backend

# 查看所有服务日志
docker compose -f docker-compose.prod.yml logs -f
```

确认日志中无错误信息。

---

## 🆘 常见问题

### 问题1：服务启动失败

**检查**：
1. 查看日志：`docker compose -f docker-compose.prod.yml logs backend`
2. 检查环境变量是否正确
3. 检查数据库连接是否正常

### 问题2：无法访问前端

**检查**：
1. 确认 Nginx 服务已启动
2. 检查端口是否被占用：`netstat -tulpn | grep 80`
3. 检查防火墙规则

### 问题3：数据库连接失败

**检查**：
1. 确认数据库服务已启动
2. 检查数据库密码是否正确
3. 检查网络连接：`docker exec -it law-firm-backend ping postgres`

---

## 📚 相关文档

- [详细部署指南](../DEPLOY.md)
- [Docker 部署指南](../docker/DEPLOY.md)
- [安全审计报告](./SECURITY_AUDIT_REPORT.md)
- [安全修复总结](./SECURITY_FIXES_SUMMARY.md)

---

## ⚠️ 重要提醒

1. **部署前必须完成所有 🔴 高优先级检查项**
2. **生产环境不要使用默认密码和密钥**
3. **定期备份数据库和文件**
4. **监控系统运行状态**
5. **保持依赖库更新，及时修复安全漏洞**

