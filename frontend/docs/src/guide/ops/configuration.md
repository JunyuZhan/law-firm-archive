# 配置说明

## 📋 环境变量配置

### 文件位置

项目使用项目根目录的 `.env` 文件进行配置：

```bash
# 1. 复制模板文件
cp env.example .env

# 2. 编辑配置
vim .env
```

### 必须修改的配置

#### 1. 数据库密码

```bash
# 生成强密码
DB_PASSWORD=$(openssl rand -base64 24)
```

#### 2. JWT 密钥

```bash
# 生成至少 64 字符的密钥
JWT_SECRET=$(openssl rand -base64 64)
```

#### 3. MinIO 密钥

```bash
# 不能使用默认的 minioadmin
MINIO_ACCESS_KEY=$(openssl rand -base64 24)
MINIO_SECRET_KEY=$(openssl rand -base64 24)
```

#### 4. OnlyOffice JWT 密钥

```bash
# 生成密钥
ONLYOFFICE_JWT_SECRET=$(openssl rand -base64 64)
```

### 重要配置项

#### OnlyOffice 外部访问地址

```bash
# 如果使用 IP 访问
ONLYOFFICE_EXTERNAL_ACCESS_URL=http://192.168.50.10

# 如果使用域名访问
ONLYOFFICE_EXTERNAL_ACCESS_URL=http://oa.example.com
```

**作用**：OnlyOffice 容器通过 Nginx 代理访问文件，而不是直接访问 Docker 内部地址。

#### MinIO 外部端点

```bash
# 如果配置了外部访问地址，缩略图等资源会使用外部地址
MINIO_EXTERNAL_ENDPOINT=http://minio:9000
```

---

## 🔧 后端配置

配置文件：`backend/src/main/resources/application.yml`

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:law_firm}
    username: ${DB_USERNAME:law_admin}
    password: ${DB_PASSWORD}
```

### Redis 配置

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
```

### MinIO 配置

```yaml
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket: ${MINIO_BUCKET:law-firm}
```

### JWT 配置

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000 # 24小时
  refresh-expiration: 604800000 # 7天
```

---

## 🎨 前端配置

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

::: tip 提示修改版本号后需要重新构建前端才能生效。开发模式下如果版本号未生效，会显示默认值 `1.0.0`。:::

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
| --- | --- | --- |
| `security.location.enabled` | `true` | 是否启用异地登录检测 |
| `security.location.level` | `province` | 异地判断级别：`province`=省级，`city`=市级 |
| `security.location.permit-code.mode` | `fixed` | 许可码模式：`fixed`=固定码，`random`=随机码（邮件通知） |
| `security.location.permit-code.value` | `888888` | 固定许可码（仅 fixed 模式有效） |

### 邮件通知配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `notification.email.enabled` | `false` | 是否启用邮件通知 |
| `notification.email.smtp.host` | - | SMTP 服务器地址 |
| `notification.email.smtp.port` | `465` | SMTP 端口 |
| `notification.email.smtp.username` | - | 发件人邮箱 |
| `notification.email.smtp.password` | - | 邮箱授权码 |
| `notification.email.admin.recipients` | - | 管理员邮箱（多个用逗号分隔） |

::: tip 异地登录许可码

- **固定码模式**：管理员设置一个固定密码，员工出差前可提前获取
- **随机码模式**：每次异地登录生成新的 6 位随机码，通过邮件发送给管理员

如需使用随机码模式，请先配置邮件通知功能。:::

### 客户服务系统对接配置

如需与客户服务系统对接，需要在**两个系统**中分别配置。

#### 律所管理系统配置

在**系统管理 → 系统配置**页面配置以下项目：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `client-service.callback.ip-whitelist-enabled` | `true` | 是否启用回调 IP 白名单验证 |
| `client-service.callback.ip-whitelist` | `127.0.0.1,localhost` | 允许回调的客户服务系统 IP，多个用逗号分隔，支持 CIDR 格式 |

还需要在**系统管理 → 外部系统集成**中配置客户服务系统：

| 配置项 | 说明 |
| --- | --- |
| API 地址 | 客户服务系统的 API 地址，如 `http://192.168.1.100:8081/api` |
| API Key | 从客户服务系统管理后台获取的密钥 |
| 启用状态 | 配置完成后需启用 |

#### 客户服务系统配置

在客户服务系统的**系统配置**页面配置以下项目：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `callback.enabled` | `true` | 是否启用律所系统回调 |
| `callback.law-firm-url` | - | 律所管理系统的回调地址，如 `http://192.168.1.50:8080/api` |

还需要在客户服务系统的**API 密钥管理**页面为律所系统创建 API Key。

::: warning 配置顺序
1. 先在**客户服务系统**创建 API Key
2. 将 API Key 填写到**律所系统**的外部系统集成配置
3. 在**律所系统**配置 IP 白名单（填写客户服务系统的 IP）
4. 在**客户服务系统**配置回调地址（填写律所系统的地址）
5. 测试推送功能是否正常
:::

::: tip IP 白名单格式
支持以下格式：
- 单个 IP：`192.168.1.100`
- 多个 IP：`192.168.1.100,192.168.1.101`
- CIDR 网段：`192.168.1.0/24`
- 混合格式：`192.168.1.100,10.0.0.0/8`
:::

## Docker Compose 配置

配置文件：`docker/docker-compose.yml`

可以修改各服务的端口、密码等配置。
