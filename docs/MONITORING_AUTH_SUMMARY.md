# 监控服务认证说明

## 📊 认证情况总结

### Prometheus（监控数据收集）

| 项目 | 值 |
|------|-----|
| **是否需要密码** | ❌ **不需要** |
| **认证方式** | 无认证（默认） |
| **访问方式** | 直接访问，无需登录 |
| **安全建议** | 如需认证，通过 Nginx 反向代理添加基本认证 |

**说明**：
- ✅ Prometheus 本身不提供认证功能
- ✅ 可以直接访问，无需密码
- ⚠️ 如需保护，建议：
  - 关闭 Cloudflare 代理（仅内网访问）
  - 或通过 Nginx 添加基本认证
  - 或使用 Cloudflare Access 保护

---

### Grafana（监控可视化面板）

| 项目 | 值 |
|------|-----|
| **是否需要密码** | ✅ **需要** |
| **默认用户名** | `admin` |
| **默认密码** | `admin`（如果未设置 `GRAFANA_PASSWORD`） |
| **认证方式** | Grafana 内置认证 |
| **安全警告** | 使用默认密码会显示安全警告 |

**说明**：
- ✅ Grafana 默认需要密码
- ✅ 如果 `.env` 文件中没有设置 `GRAFANA_PASSWORD`，默认密码是 `admin`
- ⚠️ 使用默认密码 `admin` 会显示警告："Continuing to use the default password exposes you to security risks"
- ✅ 可以通过 `.env` 文件设置 `GRAFANA_PASSWORD` 来修改密码

---

## 🔐 认证配置对比

### Prometheus

**当前配置**：
```yaml
# docker-compose.prod.yml
prometheus:
  # 无认证配置
  # 直接访问，无需密码
```

**访问**：
- ✅ 直接访问：`http://prometheus.albertzhan.top`
- ✅ 无需登录

**如需添加认证**（可选）：
```nginx
# 通过 Nginx 添加基本认证
location /prometheus/ {
    proxy_pass http://prometheus:9090/;
    auth_basic "Prometheus Access";
    auth_basic_user_file /etc/nginx/.htpasswd;
}
```

### Grafana

**当前配置**：
```yaml
# docker-compose.prod.yml
grafana:
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-admin}
```

**访问**：
- ✅ 需要登录
- ✅ 用户名：`admin`
- ✅ 密码：`.env` 文件中的 `GRAFANA_PASSWORD`（默认：`admin`）

**设置密码**：
```bash
# 在 .env 文件中设置
GRAFANA_PASSWORD=your-strong-password-here
```

---

## 📋 快速参考

### Prometheus

```
访问：http://prometheus.albertzhan.top
登录：无需
密码：无需
```

### Grafana

```
访问：http://grafana.albertzhan.top
登录：需要
用户名：admin
密码：.env 文件中的 GRAFANA_PASSWORD（默认：admin）
```

---

## ⚠️ 安全建议

### Prometheus

1. **推荐**：关闭 Cloudflare 代理，仅内网访问
2. **备选**：通过 Nginx 添加基本认证
3. **备选**：使用 Cloudflare Access 保护

### Grafana

1. **必须**：修改默认密码 `admin`
2. **推荐**：在 `.env` 文件中设置强密码
3. **推荐**：关闭 Cloudflare 代理，仅内网访问
4. **备选**：使用 Cloudflare Access 保护

---

## 🔗 相关文档

- [监控服务登录指南](./MONITORING_LOGIN_GUIDE.md)
- [添加 Grafana 密码](./ADD_GRAFANA_PASSWORD.md)
