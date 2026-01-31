# 单端口架构迁移实施指南

> ⚠️ **重要提示**：本文档用于将系统从多端口暴露迁移到单端口（80/443）架构。  
> 系统已成熟，需要谨慎操作，特别是 OnlyOffice、MinIO 和前后端的数据通信。

## 📋 目录

1. [迁移概述](#迁移概述)
2. [关键通信链路分析](#关键通信链路分析)
3. [实施前检查清单](#实施前检查清单)
4. [详细实施步骤](#详细实施步骤)
5. [配置变更清单](#配置变更清单)
6. [测试验证步骤](#测试验证步骤)
7. [回滚方案](#回滚方案)
8. [常见问题排查](#常见问题排查)

---

## 1. 迁移概述

### 1.1 目标

- **只暴露 80/443 端口**（HTTP/HTTPS）
- **移除其他端口映射**：9000, 9001, 8088, 9090, 3000
- **所有服务通过 Nginx 路径访问**：
  - `/minio/` → MinIO API
  - `/minio-console/` → MinIO Console（新增）
  - `/onlyoffice/` → OnlyOffice（已存在）
  - `/prometheus/` → Prometheus（可选）
  - `/grafana/` → Grafana（可选）

### 1.2 影响范围

| 服务 | 当前端口 | 新路径 | 影响级别 |
|------|---------|--------|---------|
| MinIO API | 9000 | `/minio/` | ⚠️ **高**（已实现，需验证） |
| MinIO Console | 9001 | `/minio-console/` | ⚠️ **高**（新增，需配置） |
| OnlyOffice | 8088 | `/onlyoffice/` | ⚠️ **高**（已实现，需验证） |
| Prometheus | 9090 | `/prometheus/` | ⚡ 中（可选） |
| Grafana | 3000 | `/grafana/` | ⚡ 中（可选） |

---

## 2. 关键通信链路分析

### 2.1 OnlyOffice 文档编辑流程

```
┌─────────────────────────────────────────────────────────────┐
│ 用户浏览器                                                    │
└─────────────────────────────────────────────────────────────┘
         │
         │ HTTP/HTTPS: /onlyoffice/
         ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend Nginx (frontend:8080)                              │
│ location /onlyoffice/ {                                     │
│   proxy_pass http://onlyoffice:80/;  ← Docker内部网络       │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
         │
         │ Docker网络: onlyoffice:80
         ▼
┌─────────────────────────────────────────────────────────────┐
│ OnlyOffice Container (onlyoffice:80)                        │
│                                                             │
│ 1. 获取文档文件                                              │
│    → http://minio:9000/...  ← Docker内部网络               │
│                                                             │
│ 2. 保存文档回调                                              │
│    → http://backend:8080/api/document/{id}/callback        │
│       ← Docker内部网络                                      │
└─────────────────────────────────────────────────────────────┘
         │
         │ Docker网络通信
         ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend Container (backend:8080)                            │
│                                                             │
│ - ONLYOFFICE_CALLBACK_URL=http://backend:8080/api         │
│ - FILE_SERVER_URL=http://minio:9000                        │
│ - ONLYOFFICE_URL=/onlyoffice  ← 浏览器访问路径             │
└─────────────────────────────────────────────────────────────┘
```

**关键配置点：**
- ✅ `ONLYOFFICE_CALLBACK_URL`：Docker 内部地址，**不需要修改**
- ✅ `FILE_SERVER_URL`：Docker 内部地址，**不需要修改**
- ✅ `ONLYOFFICE_URL`：浏览器访问路径，**已经是路径形式，不需要修改**
- ✅ Nginx `/onlyoffice/` 代理：**已存在，需要验证**

### 2.2 MinIO 文件存储流程

```
┌─────────────────────────────────────────────────────────────┐
│ 用户浏览器                                                    │
└─────────────────────────────────────────────────────────────┘
         │
         │ HTTP/HTTPS: /minio/law-firm/...
         ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend Nginx (frontend:8080)                              │
│ location /minio/ {                                           │
│   proxy_pass http://minio:9000/;  ← Docker内部网络          │
│   proxy_set_header Host minio:9000;  ← 关键！保持签名      │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
         │
         │ Docker网络: minio:9000
         ▼
┌─────────────────────────────────────────────────────────────┐
│ MinIO Container (minio:9000)                               │
└─────────────────────────────────────────────────────────────┘
         │
         │ Docker网络: backend → minio
         ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend Container (backend:8080)                            │
│                                                             │
│ - MINIO_ENDPOINT=http://minio:9000  ← Docker内部地址        │
│ - MINIO_BROWSER_ENDPOINT=/minio  ← 浏览器访问路径（需改）  │
└─────────────────────────────────────────────────────────────┘
```

**关键配置点：**
- ✅ `MINIO_ENDPOINT`：Docker 内部地址，**不需要修改**
- ⚠️ `MINIO_BROWSER_ENDPOINT`：**需要改为 `/minio`**（相对路径）
- ✅ Nginx `/minio/` 代理：**已存在，需要验证 Host 头设置**

### 2.3 MinIO Console 管理界面

```
┌─────────────────────────────────────────────────────────────┐
│ 用户浏览器                                                    │
└─────────────────────────────────────────────────────────────┘
         │
         │ HTTP/HTTPS: /minio-console/
         ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend Nginx (frontend:8080)                              │
│ location /minio-console/ {                                   │
│   proxy_pass http://minio:9001/;  ← Docker内部网络         │
│   # WebSocket 支持（Console 需要）                           │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
         │
         │ Docker网络: minio:9001
         ▼
┌─────────────────────────────────────────────────────────────┐
│ MinIO Container (minio:9001)                                │
│ command: server /data --console-address ":9001"             │
└─────────────────────────────────────────────────────────────┘
```

**关键配置点：**
- ⚠️ Nginx `/minio-console/` 代理：**需要新增**（当前不存在）
- ⚠️ WebSocket 支持：**必须配置**（Console 使用 WebSocket）
- ✅ MinIO Console 容器内部监听 9001 端口（已配置）

---

## 3. 关键验证点（必须确认）

在开始迁移前，**必须确认以下关键点**：

### 3.1 后端代码验证（✅ 已验证，无需修改）

- ✅ **MinIOService.getBrowserAccessibleUrl()** 已支持相对路径 `/minio`
  - 代码位置：`backend/src/main/java/com/lawfirm/infrastructure/external/minio/MinioService.java:223-226`
  - 逻辑：当 `browserEndpoint.equals("/minio")` 时，自动替换为 `/minio/`

- ✅ **OnlyOffice 文件访问** 使用 Docker 内部地址
  - `buildFileUrlForDocument()` 使用 `http://backend:8080/api`（Docker 内部）
  - `buildFileUrl()` 使用 `http://minio:9000`（Docker 内部）
  - **不依赖端口映射**

### 3.2 Nginx 配置验证

- ✅ **`/minio/` location** 已存在且配置正确
  - `proxy_set_header Host minio:9000;` ✅ 已配置（关键！）
  - `proxy_buffering off;` ✅ 已配置

- ✅ **`/onlyoffice/` location** 已存在且配置正确
  - `proxy_set_header Authorization $http_authorization;` ✅ 已配置（关键！）
  - WebSocket 支持 ✅ 已配置

- ❌ **`/minio-console/` location** **不存在**，需要添加

### 3.3 环境变量验证

- ⚠️ **MINIO_BROWSER_ENDPOINT** 当前值：`http://your-ip:9000`（需要改为 `/minio`）
- ✅ **ONLYOFFICE_URL** 可以是 `/onlyoffice` 或完整 URL（不需要修改）
- ✅ **ONLYOFFICE_CALLBACK_URL** 值：`http://backend:8080/api`（Docker 内部，正确）
- ✅ **FILE_SERVER_URL** 值：`http://law-firm-minio:9000`（Docker 内部，正确）

### 3.4 Docker Compose 验证

- ⚠️ **MinIO ports** 第 169-171 行：需要移除
- ⚠️ **OnlyOffice ports** 第 246-247 行：需要移除
- ⚠️ **Prometheus ports** 第 282-283 行：需要移除（如果启用）
- ⚠️ **Grafana ports** 第 295-296 行：需要移除（如果启用）

---

## 4. 实施前检查清单

### 3.1 备份清单

- [ ] **数据库备份**
  ```bash
  docker exec law-firm-postgres pg_dump -U law_admin law_firm > backup_$(date +%Y%m%d_%H%M%S).sql
  ```

- [ ] **配置文件备份**
  ```bash
  cp docker/docker-compose.prod.yml docker/docker-compose.prod.yml.backup
  cp frontend/scripts/deploy/nginx-ssl.conf frontend/scripts/deploy/nginx-ssl.conf.backup
  cp .env .env.backup
  ```

- [ ] **当前端口映射记录**
  ```bash
  docker ps --format "table {{.Names}}\t{{.Ports}}" > ports_before_migration.txt
  ```

### 3.2 环境检查

- [ ] **确认当前 Nginx 配置**
  ```bash
  docker exec law-firm-frontend cat /etc/nginx/nginx.conf | grep -A 10 "location /minio"
  docker exec law-firm-frontend cat /etc/nginx/nginx.conf | grep -A 10 "location /onlyoffice"
  ```

- [ ] **测试当前路径代理**
  ```bash
  # 测试 MinIO 路径代理
  curl -I http://localhost/minio/law-firm/
  
  # 测试 OnlyOffice 路径代理
  curl -I http://localhost/onlyoffice/healthcheck
  ```

- [ ] **记录当前环境变量**
  ```bash
  docker exec law-firm-backend env | grep -E "MINIO|ONLYOFFICE|FILE_SERVER" > env_before.txt
  ```

### 3.3 依赖关系检查

- [ ] **OnlyOffice 文档编辑功能正常**
  - [ ] 可以打开文档
  - [ ] 可以编辑文档
  - [ ] 可以保存文档

- [ ] **MinIO 文件访问正常**
  - [ ] 文件上传正常
  - [ ] 文件下载正常
  - [ ] 缩略图显示正常

- [ ] **MinIO Console 访问正常**
  - [ ] 可以登录控制台
  - [ ] 可以查看存储桶
  - [ ] 可以管理文件

---

## 5. 详细实施步骤

### 步骤 1：更新 Nginx 配置

**文件：** `frontend/scripts/deploy/nginx-ssl.conf`

#### 1.1 验证现有 MinIO 代理配置

检查 `/minio/` location 块是否正确：

```nginx
# MinIO 对象存储代理（解决 HTTPS 页面加载 HTTP 资源问题）
location /minio/ {
  proxy_pass http://minio:9000/;
  # 关键：保持 MinIO 期望的 Host，否则预签名验证会失败
  proxy_set_header Host minio:9000;
  proxy_set_header X-Real-IP $remote_addr;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
  
  # 禁用缓冲，支持大文件
  proxy_buffering off;
  proxy_request_buffering off;
  
  # 超时设置
  proxy_connect_timeout 60s;
  proxy_send_timeout 600s;
  proxy_read_timeout 600s;
  
  # 文件大小限制
  client_max_body_size 500M;
}
```

**验证点：**
- ✅ `proxy_set_header Host minio:9000;` **必须存在**（预签名 URL 验证需要）
- ✅ `proxy_buffering off;` **必须存在**（大文件传输需要）

#### 1.2 验证现有 OnlyOffice 代理配置

检查 `/onlyoffice/` location 块是否正确：

```nginx
# OnlyOffice 文档编辑服务代理
location /onlyoffice/ {
  proxy_pass http://onlyoffice:80/;
  proxy_set_header Host $host;
  proxy_set_header X-Real-IP $remote_addr;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
  proxy_set_header X-Forwarded-Host $host;
  # 关键：传递 Authorization header（OnlyOffice JWT 验证需要）
  proxy_set_header Authorization $http_authorization;
  
  # CORS 头（解决跨域问题）
  add_header Access-Control-Allow-Origin * always;
  add_header Access-Control-Allow-Methods "GET, POST, OPTIONS, PUT, DELETE" always;
  add_header Access-Control-Allow-Headers "Authorization, Content-Type, X-Requested-With" always;
  add_header Access-Control-Allow-Credentials true always;
  
  # 处理 OPTIONS 预检请求
  if ($request_method = 'OPTIONS') {
    add_header Access-Control-Allow-Origin * always;
    add_header Access-Control-Allow-Methods "GET, POST, OPTIONS, PUT, DELETE" always;
    add_header Access-Control-Allow-Headers "Authorization, Content-Type, X-Requested-With" always;
    add_header Access-Control-Max-Age 1728000;
    add_header Content-Type 'text/plain charset=UTF-8';
    add_header Content-Length 0;
    return 204;
  }
  
  # WebSocket 支持（OnlyOffice 实时协作需要）
  proxy_http_version 1.1;
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "upgrade";
  
  # 超时设置
  proxy_connect_timeout 60s;
  proxy_send_timeout 600s;
  proxy_read_timeout 600s;
  
  # 文件大小限制
  client_max_body_size 100M;
}
```

**验证点：**
- ✅ `proxy_set_header Authorization $http_authorization;` **必须存在**（JWT 验证）
- ✅ WebSocket 配置 **必须存在**（实时协作）

#### 1.3 添加 MinIO Console 代理配置

**重要：** 需要在 **HTTP server 块（8080）** 和 **HTTPS server 块（8443）** 中都添加。

在 HTTP server 块（第 200 行之前）添加：

```nginx
    # MinIO Console 管理界面代理
    location /minio-console/ {
      proxy_pass http://minio:9001/;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      
      # WebSocket 支持（Console 需要 WebSocket）
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
      
      # 超时设置
      proxy_connect_timeout 60s;
      proxy_send_timeout 600s;
      proxy_read_timeout 600s;
      
      # 禁用缓冲
      proxy_buffering off;
      
      # 文件大小限制
      client_max_body_size 100M;
    }
```

在 HTTPS server 块（第 370 行之前，OnlyOffice location 之后）也添加相同的配置。

**注意：**
- ⚠️ 路径必须以 `/` 结尾：`/minio-console/`
- ⚠️ `proxy_pass` 必须以 `/` 结尾：`http://minio:9001/`
- ⚠️ **必须配置 WebSocket**（Console 使用 WebSocket 通信）
- ⚠️ **必须在两个 server 块中都添加**（HTTP 和 HTTPS）

#### 1.4 添加 Prometheus 代理配置（可选）

```nginx
# Prometheus 监控服务代理
location /prometheus/ {
  proxy_pass http://prometheus:9090/;
  proxy_set_header Host $host;
  proxy_set_header X-Real-IP $remote_addr;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
  
  # 超时设置
  proxy_connect_timeout 60s;
  proxy_send_timeout 600s;
  proxy_read_timeout 600s;
}
```

#### 1.5 添加 Grafana 代理配置（可选）

```nginx
# Grafana 可视化服务代理
location /grafana/ {
  proxy_pass http://grafana:3000/;
  proxy_set_header Host $host;
  proxy_set_header X-Real-IP $remote_addr;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
  
  # WebSocket 支持
  proxy_http_version 1.1;
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "upgrade";
  
  # 超时设置
  proxy_connect_timeout 60s;
  proxy_send_timeout 600s;
  proxy_read_timeout 600s;
}
```

### 步骤 2：更新 docker-compose.prod.yml

**文件：** `docker/docker-compose.prod.yml`

#### 2.1 移除 MinIO 端口映射

**文件位置：** `docker/docker-compose.prod.yml` 第 169-171 行

```yaml
minio:
  # ... 其他配置保持不变 ...
  # 移除以下端口映射（注释掉或删除）：
  # ports:
  #   - "9000:9000"  # API 端口（浏览器访问缩略图需要）
  #   - "9001:9001"  # 控制台
  # 容器内部仍然监听 9000 和 9001，只是不暴露到宿主机
```

**修改后应该是：**
```yaml
minio:
  image: minio/minio:${MINIO_VERSION:-RELEASE.2024-07-15T19-02-30Z}
  container_name: law-firm-minio
  # ... 其他配置 ...
  # ports:  # ← 删除或注释掉整个 ports 块
  #   - "9000:9000"
  #   - "9001:9001"
  networks:
    - law-firm-network
```

#### 2.2 移除 OnlyOffice 端口映射

**文件位置：** `docker/docker-compose.prod.yml` 第 246-247 行

```yaml
onlyoffice:
  # ... 其他配置保持不变 ...
  # 移除以下端口映射（注释掉或删除）：
  # ports:
  #   - "8088:80"
  # 容器内部仍然监听 80，只是不暴露到宿主机
```

**修改后应该是：**
```yaml
onlyoffice:
  profiles: ["docs"]
  build:
    context: ./onlyoffice
    dockerfile: Dockerfile
  # ... 其他配置 ...
  # ports:  # ← 删除或注释掉整个 ports 块
  #   - "8088:80"
  networks:
    - law-firm-network
```

#### 2.3 移除 Prometheus 端口映射（如果存在）

**文件位置：** `docker/docker-compose.prod.yml` 第 282-283 行

```yaml
prometheus:
  # ... 其他配置保持不变 ...
  # 移除以下端口映射（注释掉或删除）：
  # ports:
  #   - "9090:9090"
```

**修改后应该是：**
```yaml
prometheus:
  profiles: ["monitoring"]
  image: prom/prometheus:latest
  # ... 其他配置 ...
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
    - '--web.external-url=/prometheus/'  # ← 添加此参数（如果启用）
    # ... 其他参数 ...
  # ports:  # ← 删除或注释掉整个 ports 块
  #   - "9090:9090"
  networks:
    - law-firm-network
```

#### 2.4 移除 Grafana 端口映射（如果存在）

**文件位置：** `docker/docker-compose.prod.yml` 第 295-296 行

```yaml
grafana:
  # ... 其他配置保持不变 ...
  # 移除以下端口映射（注释掉或删除）：
  # ports:
  #   - "3000:3000"
```

**修改后应该是：**
```yaml
grafana:
  profiles: ["monitoring"]
  image: grafana/grafana:latest
  # ... 其他配置 ...
  environment:
    - GF_SERVER_ROOT_URL=/grafana/  # ← 添加此环境变量（如果启用）
    - GF_SERVER_SERVE_FROM_SUB_PATH=true  # ← 添加此环境变量（如果启用）
    - GF_SECURITY_ADMIN_PASSWORD=admin
    # ... 其他环境变量 ...
  # ports:  # ← 删除或注释掉整个 ports 块
  #   - "3000:3000"
  networks:
    - law-firm-network
```

#### 2.5 更新 Prometheus 配置（如果启用）

```yaml
prometheus:
  # ... 其他配置 ...
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
    - '--web.external-url=/prometheus/'  # ← 添加此参数
    - '--web.console.libraries=/usr/share/prometheus/console_libraries'
    - '--web.console.templates=/usr/share/prometheus/consoles'
```

#### 2.6 更新 Grafana 配置（如果启用）

```yaml
grafana:
  # ... 其他配置 ...
  environment:
    - GF_SERVER_ROOT_URL=/grafana/  # ← 添加此环境变量
    - GF_SERVER_SERVE_FROM_SUB_PATH=true  # ← 添加此环境变量
    - GF_SECURITY_ADMIN_PASSWORD=admin
    - GF_USERS_ALLOW_SIGN_UP=false
    - GF_DEFAULT_LANGUAGE=zh-Hans
```

### 步骤 3：更新环境变量配置

**文件：** `.env` 或 `docker/env.example`

#### 3.1 更新 MINIO_BROWSER_ENDPOINT

```bash
# 之前（绝对路径，直接暴露端口）：
# MINIO_BROWSER_ENDPOINT=http://your-ip:9000
# 或
# MINIO_BROWSER_ENDPOINT=http://192.168.50.10:9000

# 之后（相对路径，通过 Nginx 代理）：
MINIO_BROWSER_ENDPOINT=/minio
```

**关键点：**
- ⚠️ 使用相对路径 `/minio`，**不要包含协议和域名**
- ⚠️ **不要使用** `http://your-domain/minio`（必须是 `/minio`）
- ✅ 后端代码 `MinioService.getBrowserAccessibleUrl()` 已经支持此逻辑：
  ```java
  if (browserEndpoint != null && browserEndpoint.equals("/minio")) {
      presignedUrl = presignedUrl.replace(endpoint + "/", "/minio/");
  }
  ```

#### 3.2 验证 ONLYOFFICE_URL（可以是路径或完整URL）

```bash
# 选项1：相对路径（推荐，自动适配 HTTP/HTTPS）
ONLYOFFICE_URL=/onlyoffice

# 选项2：完整 URL（如果使用域名）
ONLYOFFICE_URL=http://your-domain/onlyoffice
# 或
ONLYOFFICE_URL=https://your-domain/onlyoffice
```

**关键点：**
- ✅ 相对路径 `/onlyoffice` **推荐使用**（自动适配 HTTP/HTTPS）
- ✅ 完整 URL 也可以使用，但需要确保协议正确（HTTP/HTTPS）
- ✅ 后端代码会直接使用此值，**不需要修改**

#### 3.3 验证其他环境变量（不需要修改）

```bash
# 这些是 Docker 内部地址，不需要修改：
MINIO_ENDPOINT=http://minio:9000
MINIO_EXTERNAL_ENDPOINT=http://minio:9000
FILE_SERVER_URL=http://law-firm-minio:9000  # 注意：docker-compose 中使用 law-firm-minio
ONLYOFFICE_CALLBACK_URL=http://backend:8080/api
```

**关键点：**
- ✅ 这些都是 Docker 内部通信地址，**绝对不要修改**
- ✅ 它们不依赖端口映射，通过 Docker 网络直接通信
- ⚠️ **特别注意**：`FILE_SERVER_URL` 在 docker-compose.prod.yml 中设置为 `http://law-firm-minio:9000`（容器名是 `law-firm-minio`，不是 `minio`）
- ✅ OnlyOffice 通过 `FILE_SERVER_URL` 访问 MinIO 文件，使用 Docker 内部地址

### 步骤 4：验证后端代码逻辑

**文件：** `backend/src/main/java/com/lawfirm/infrastructure/external/minio/MinioService.java`

#### 4.1 检查 getBrowserAccessibleUrl 方法

✅ **已验证：** 代码已经支持相对路径模式（第 223-226 行）：

```java
// 代码已存在，无需修改：
if (browserEndpoint != null && browserEndpoint.equals("/minio")) {
    // 使用相对路径模式：将 http://minio:9000/ 替换为 /minio/
    presignedUrl = presignedUrl.replace(endpoint + "/", "/minio/");
    log.debug("使用相对路径模式: {} -> {}", endpoint, "/minio");
}
```

**结论：** ✅ **不需要修改后端代码**，已经支持相对路径模式。

#### 4.2 验证 OnlyOffice 文件访问逻辑

✅ **已验证：** OnlyOffice 使用后端代理接口 `/file-proxy`，通过 Docker 内部地址访问：
- `buildFileUrlForDocument()` 方法使用 `http://backend:8080/api`（Docker 内部地址）
- `buildFileUrl()` 方法使用 `http://minio:9000`（Docker 内部地址）
- **不依赖端口映射**，完全通过 Docker 网络通信

**结论：** ✅ **OnlyOffice 配置不需要修改**，已经正确使用 Docker 内部地址。

---

## 6. 配置变更清单

### 5.1 必须修改的文件

| 文件 | 修改内容 | 优先级 |
|------|---------|--------|
| `frontend/scripts/deploy/nginx-ssl.conf` | 添加 `/minio-console/` location | ⚠️ **高** |
| `docker/docker-compose.prod.yml` | 移除端口映射 | ⚠️ **高** |
| `.env` | 更新 `MINIO_BROWSER_ENDPOINT=/minio` | ⚠️ **高** |

### 5.2 可选修改的文件

| 文件 | 修改内容 | 优先级 |
|------|---------|--------|
| `frontend/scripts/deploy/nginx-ssl.conf` | 添加 `/prometheus/` location | ⚡ 中 |
| `frontend/scripts/deploy/nginx-ssl.conf` | 添加 `/grafana/` location | ⚡ 中 |
| `docker/docker-compose.prod.yml` | Prometheus `--web.external-url` | ⚡ 中 |
| `docker/docker-compose.prod.yml` | Grafana `GF_SERVER_ROOT_URL` | ⚡ 中 |

### 5.3 不需要修改的配置

| 配置项 | 原因 |
|--------|------|
| `MINIO_ENDPOINT` | Docker 内部地址，不依赖端口映射 |
| `MINIO_EXTERNAL_ENDPOINT` | Docker 内部地址，不依赖端口映射 |
| `FILE_SERVER_URL` | Docker 内部地址，不依赖端口映射 |
| `ONLYOFFICE_CALLBACK_URL` | Docker 内部地址，不依赖端口映射 |
| `ONLYOFFICE_URL` | 已经是路径形式，不需要修改 |

---

## 7. 测试验证步骤

### 6.1 部署前测试（在开发环境）

```bash
# 1. 构建新的前端镜像（包含更新的 Nginx 配置）
cd frontend
docker build -f scripts/deploy/Dockerfile -t law-firm-frontend:test .

# 2. 测试 Nginx 配置语法
docker run --rm law-firm-frontend:test nginx -t

# 3. 启动测试环境
cd ../docker
docker compose -f docker-compose.prod.yml up -d

# 4. 等待服务启动
sleep 30

# 5. 检查容器状态
docker ps
```

### 6.2 功能测试清单

#### 6.2.1 MinIO API 测试

```bash
# 测试 MinIO API 路径代理
curl -I http://localhost/minio/law-firm/

# 预期结果：200 OK
# 检查响应头中是否有正确的 Content-Type
```

**浏览器测试：**
- [ ] 打开 `http://your-domain/minio/law-firm/`（应该显示文件列表或 403）
- [ ] 检查浏览器控制台，确认没有 Mixed Content 错误

#### 6.2.2 MinIO Console 测试

```bash
# 测试 MinIO Console 路径代理
curl -I http://localhost/minio-console/

# 预期结果：200 OK 或 302 Redirect（到登录页）
```

**浏览器测试：**
- [ ] 打开 `http://your-domain/minio-console/`
- [ ] 应该显示 MinIO Console 登录页面
- [ ] 登录后可以正常使用控制台
- [ ] WebSocket 连接正常（检查浏览器 Network 标签）

#### 6.2.3 OnlyOffice 测试

```bash
# 测试 OnlyOffice 健康检查
curl http://localhost/onlyoffice/healthcheck

# 预期结果：{"status": "ok"} 或类似响应
```

**浏览器测试：**
- [ ] 打开文档预览页面
- [ ] 点击"编辑"按钮
- [ ] OnlyOffice 编辑器应该正常加载
- [ ] 可以编辑文档
- [ ] 可以保存文档
- [ ] 检查浏览器控制台，确认没有错误

#### 6.2.4 文件上传下载测试

- [ ] **文件上传**：上传一个文档，确认成功
- [ ] **文件下载**：下载文件，确认正常
- [ ] **缩略图显示**：查看文档列表，缩略图应该正常显示
- [ ] **预签名 URL**：检查生成的 URL 格式是否为 `/minio/...`

#### 6.2.5 容器间通信测试

```bash
# 进入后端容器
docker exec -it law-firm-backend sh

# 测试访问 MinIO（Docker 内部网络）
curl http://minio:9000/minio/health/live

# 测试访问 OnlyOffice（Docker 内部网络）
curl http://onlyoffice:80/healthcheck

# 退出容器
exit
```

**预期结果：** 所有测试都应该成功（不依赖端口映射）

### 6.3 端口验证

```bash
# 检查端口映射（应该只有 80 和 443）
docker ps --format "table {{.Names}}\t{{.Ports}}" | grep -E "80|443|9000|9001|8088|9090|3000"

# 预期结果：
# law-firm-frontend  0.0.0.0:80->8080/tcp, 0.0.0.0:443->8443/tcp
# 其他容器不应该有端口映射
```

### 6.4 日志检查

```bash
# 检查 Nginx 日志
docker logs law-firm-frontend | tail -50

# 检查后端日志（关注 MinIO 和 OnlyOffice 相关）
docker logs law-firm-backend | grep -E "MinIO|OnlyOffice" | tail -50

# 检查 MinIO 日志
docker logs law-firm-minio | tail -50

# 检查 OnlyOffice 日志
docker logs onlyoffice | tail -50
```

---

## 8. 回滚方案

如果迁移后出现问题，可以快速回滚：

### 7.1 快速回滚步骤

```bash
# 1. 停止当前服务
cd docker
docker compose -f docker-compose.prod.yml down

# 2. 恢复配置文件
cp docker/docker-compose.prod.yml.backup docker/docker-compose.prod.yml
cp frontend/scripts/deploy/nginx-ssl.conf.backup frontend/scripts/deploy/nginx-ssl.conf
cp .env.backup .env

# 3. 恢复端口映射（手动编辑 docker-compose.prod.yml）
# 取消注释端口映射部分

# 4. 恢复环境变量
# MINIO_BROWSER_ENDPOINT=http://your-ip:9000

# 5. 重新启动服务
docker compose -f docker-compose.prod.yml up -d

# 6. 验证服务正常
docker ps
curl http://localhost:9001  # MinIO Console
curl http://localhost:8088/healthcheck  # OnlyOffice
```

### 7.2 部分回滚（只回滚某个服务）

如果只有某个服务有问题，可以只恢复该服务的端口映射：

```yaml
# 例如：只恢复 MinIO Console 端口映射
minio:
  ports:
    - "9001:9001"  # 临时恢复，用于排查问题
```

---

## 9. 常见问题排查

### 8.1 MinIO 预签名 URL 验证失败

**症状：**
- 文件无法访问
- 返回 403 Forbidden
- 日志显示签名验证失败

**原因：**
- Nginx `proxy_set_header Host` 配置错误
- `MINIO_BROWSER_ENDPOINT` 配置错误

**解决方案：**
```nginx
# 确保 Nginx 配置正确：
location /minio/ {
  proxy_pass http://minio:9000/;
  proxy_set_header Host minio:9000;  # ← 必须保持为 minio:9000
  # ...
}
```

```bash
# 确保环境变量正确：
MINIO_BROWSER_ENDPOINT=/minio  # ← 相对路径
```

### 8.2 OnlyOffice 无法加载文档

**症状：**
- OnlyOffice 编辑器显示错误
- 无法获取文档文件
- 浏览器控制台显示 CORS 错误

**原因：**
- OnlyOffice 无法访问 MinIO 文件
- `FILE_SERVER_URL` 配置错误
- Nginx CORS 配置缺失

**解决方案：**
```bash
# 检查环境变量：
docker exec law-firm-backend env | grep FILE_SERVER_URL
# 应该显示：FILE_SERVER_URL=http://minio:9000

# 检查 OnlyOffice 能否访问 MinIO：
docker exec onlyoffice curl http://minio:9000/minio/health/live
```

### 8.3 MinIO Console WebSocket 连接失败

**症状：**
- Console 页面加载但无法操作
- 浏览器控制台显示 WebSocket 错误

**原因：**
- Nginx 未配置 WebSocket 支持

**解决方案：**
```nginx
# 确保配置了 WebSocket：
location /minio-console/ {
  proxy_pass http://minio:9001/;
  proxy_http_version 1.1;
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "upgrade";
  # ...
}
```

### 8.4 Grafana 子路径访问问题

**症状：**
- Grafana 页面加载但资源 404
- 样式丢失

**原因：**
- Grafana 未配置子路径模式

**解决方案：**
```yaml
grafana:
  environment:
    - GF_SERVER_ROOT_URL=/grafana/
    - GF_SERVER_SERVE_FROM_SUB_PATH=true
```

### 8.5 路径冲突问题

**症状：**
- 某些路径无法访问
- 前端路由被代理拦截

**原因：**
- Nginx location 顺序问题
- 路径匹配冲突

**解决方案：**
```nginx
# location 顺序很重要，更具体的路径应该在前：
location /minio-console/ { ... }  # 更具体
location /minio/ { ... }          # 更通用

# 前端路由应该放在最后：
location / {
  try_files $uri $uri/ /index.html;
}
```

---

## 10. 迁移后检查清单

### 9.1 功能检查

- [ ] **文档编辑功能**
  - [ ] 可以打开文档
  - [ ] 可以编辑文档
  - [ ] 可以保存文档
  - [ ] 保存后文件正确更新

- [ ] **文件管理功能**
  - [ ] 文件上传正常
  - [ ] 文件下载正常
  - [ ] 缩略图显示正常
  - [ ] 文件预览正常

- [ ] **MinIO Console**
  - [ ] 可以访问控制台
  - [ ] 可以登录
  - [ ] 可以管理文件
  - [ ] WebSocket 连接正常

### 9.2 性能检查

- [ ] **响应时间**
  - [ ] 文件上传速度正常
  - [ ] 文件下载速度正常
  - [ ] 页面加载速度正常

- [ ] **资源使用**
  - [ ] Nginx CPU 使用率正常
  - [ ] 内存使用率正常

### 9.3 安全检查

- [ ] **端口扫描**
  ```bash
  # 扫描服务器，确认只有 80 和 443 开放
  nmap -p 80,443,9000,9001,8088,9090,3000 your-server-ip
  ```

- [ ] **访问控制**
  - [ ] MinIO Console 需要认证
  - [ ] Prometheus/Grafana 需要认证（如果暴露）

---

## 11. 后续优化建议

### 10.1 添加访问控制

```nginx
# MinIO Console 添加基本认证
location /minio-console/ {
  auth_basic "MinIO Console";
  auth_basic_user_file /etc/nginx/.htpasswd;
  # ...
}

# Prometheus 添加 IP 白名单
location /prometheus/ {
  allow 192.168.1.0/24;
  deny all;
  # ...
}
```

### 10.2 监控和告警

- [ ] 监控 Nginx 健康状态
- [ ] 监控各服务路径访问情况
- [ ] 设置异常告警

### 10.3 文档更新

- [ ] 更新部署文档
- [ ] 更新用户手册（访问地址）
- [ ] 更新运维手册

---

## 12. 联系和支持

如果在迁移过程中遇到问题：

1. **查看日志**：`docker logs <container-name>`
2. **检查配置**：对比本文档的配置要求
3. **回滚操作**：参考第 7 节回滚方案
4. **联系团队**：记录问题详情和错误日志

---

## 附录：关键配置参考

### ⚠️ 重要提醒

在实施迁移前，请**务必**：
1. ✅ 阅读并理解所有关键通信链路（第 2 节）
2. ✅ 确认所有关键验证点（第 3 节）
3. ✅ 完成实施前检查清单（第 4 节）
4. ✅ 按照详细步骤逐步实施（第 5 节）
5. ✅ 完成所有测试验证（第 7 节）

**如果任何一步出现问题，立即停止并参考回滚方案（第 8 节）。**

### A.1 完整的 Nginx location 配置顺序

```nginx
# 1. 精确匹配（优先级最高）
location = /sw.js { ... }
location = /docs { return 301 /docs/; }

# 2. 前缀匹配（按具体程度排序）
location /minio-console/ { ... }  # 更具体
location /minio/ { ... }          # 更通用
location /onlyoffice/ { ... }
location /prometheus/ { ... }
location /grafana/ { ... }
location /docs/ { ... }
location /api/ { ... }

# 3. 通用匹配（最后）
location / { ... }
```

### A.2 环境变量配置参考

```bash
# MinIO 配置
MINIO_ENDPOINT=http://minio:9000
MINIO_EXTERNAL_ENDPOINT=http://minio:9000
MINIO_BROWSER_ENDPOINT=/minio  # ← 相对路径
MINIO_ACCESS_KEY=your-access-key
MINIO_SECRET_KEY=your-secret-key

# OnlyOffice 配置
ONLYOFFICE_URL=/onlyoffice  # ← 相对路径或完整 URL
ONLYOFFICE_CALLBACK_URL=http://backend:8080/api  # ← Docker 内部地址
FILE_SERVER_URL=http://minio:9000  # ← Docker 内部地址
ONLYOFFICE_JWT_ENABLED=true
ONLYOFFICE_JWT_SECRET=your-jwt-secret
```

---

**文档版本：** 1.0  
**最后更新：** 2026-01-31  
**维护者：** DevOps Team
