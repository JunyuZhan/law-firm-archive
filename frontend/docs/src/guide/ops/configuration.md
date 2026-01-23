# 配置说明

## 后端配置

配置文件：`backend/src/main/resources/application.yml`

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lawfirm
    username: lawfirm
    password: your-password
```

### Redis 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: your-password
```

### MinIO 配置

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: law-firm
```

### JWT 配置

```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000 # 24小时
  refresh-expiration: 604800000 # 7天
```

## 前端配置

配置文件：`frontend/apps/web-antd/.env.production`

```env
VITE_GLOB_API_URL=/api
```

### 版本号配置

系统版本号显示在登录页右下角，配置方式如下：

**配置文件**：`frontend/apps/web-antd/package.json`

```json
{
  "version": "1.0.0"
}
```

**工作原理**：

1. 构建时 Vite 插件读取 `package.json` 中的 `version` 字段
2. 注入到环境变量 `import.meta.env.VITE_APP_VERSION`
3. 登录页面读取该变量显示版本号

**修改版本号**：

```bash
# 方式一：直接编辑 package.json
cd frontend/apps/web-antd
# 修改 "version": "1.0.1"

# 方式二：使用 npm version 命令
cd frontend/apps/web-antd
npm version patch  # 1.0.0 -> 1.0.1
npm version minor  # 1.0.0 -> 1.1.0
npm version major  # 1.0.0 -> 2.0.0
```

::: tip 提示
修改版本号后需要重新构建前端才能生效。开发模式下如果版本号未生效，会显示默认值 `1.0.0`。
:::

## 环境变量

| 变量名           | 说明           | 默认值                |
| ---------------- | -------------- | --------------------- |
| DB_HOST          | 数据库地址     | localhost             |
| DB_PORT          | 数据库端口     | 5432                  |
| DB_NAME          | 数据库名       | lawfirm               |
| DB_USER          | 数据库用户     | lawfirm               |
| DB_PASSWORD      | 数据库密码     | -                     |
| REDIS_HOST       | Redis 地址     | localhost             |
| REDIS_PORT       | Redis 端口     | 6379                  |
| MINIO_ENDPOINT   | MinIO 地址     | http://localhost:9000 |
| MINIO_ACCESS_KEY | MinIO 访问密钥 | minioadmin            |
| MINIO_SECRET_KEY | MinIO 密钥     | minioadmin            |

## 系统配置（数据库）

以下配置保存在数据库 `sys_config` 表中，可通过**系统管理 → 系统配置**页面修改。

### 安全配置

| 配置项 | 默认值 | 说明 |
|-------|--------|------|
| `security.location.enabled` | `true` | 是否启用异地登录检测 |
| `security.location.level` | `province` | 异地判断级别：`province`=省级，`city`=市级 |
| `security.location.permit-code.mode` | `fixed` | 许可码模式：`fixed`=固定码，`random`=随机码（邮件通知） |
| `security.location.permit-code.value` | `888888` | 固定许可码（仅 fixed 模式有效） |

### 邮件通知配置

| 配置项 | 默认值 | 说明 |
|-------|--------|------|
| `notification.email.enabled` | `false` | 是否启用邮件通知 |
| `notification.email.smtp.host` | - | SMTP 服务器地址 |
| `notification.email.smtp.port` | `465` | SMTP 端口 |
| `notification.email.smtp.username` | - | 发件人邮箱 |
| `notification.email.smtp.password` | - | 邮箱授权码 |
| `notification.email.admin.recipients` | - | 管理员邮箱（多个用逗号分隔） |

::: tip 异地登录许可码
- **固定码模式**：管理员设置一个固定密码，员工出差前可提前获取
- **随机码模式**：每次异地登录生成新的 6 位随机码，通过邮件发送给管理员

如需使用随机码模式，请先配置邮件通知功能。
:::

## Docker Compose 配置

配置文件：`docker/docker-compose.yml`

可以修改各服务的端口、密码等配置。
