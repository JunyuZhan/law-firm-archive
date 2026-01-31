# 共享 MinIO 部署指南

## 📋 概述

如果服务器上运行多个应用（如 law-firm、pis 等），共享一个 MinIO 实例可以节省资源，避免运行多个 MinIO 容器。

---

## 💡 为什么共享 MinIO？

### 资源对比

| 场景 | CPU（空闲） | 内存（空闲） | 磁盘 |
|------|------------|------------|------|
| 两个独立 MinIO | ~30% | ~512MB-1GB | 两个数据卷 |
| 共享 MinIO | ~15% | ~256MB-512MB | 一个数据卷 |
| **节省** | **50%** | **50%** | **50%** |

### 优势

1. **资源节省**：CPU 和内存占用减半
2. **统一管理**：一个控制台管理所有应用
3. **数据隔离**：通过不同 bucket 实现隔离
4. **易于维护**：只需维护一个 MinIO 实例

---

## 🚀 部署方案

### 方案 1：独立部署共享 MinIO（推荐）

创建一个独立的共享 MinIO 服务，供多个应用使用。

#### 步骤 1：创建共享 MinIO 配置

```bash
cd /opt
mkdir shared-minio
cd shared-minio
```

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  shared-minio:
    image: minio/minio:RELEASE.2024-07-15T19-02-30Z
    container_name: shared-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${SHARED_MINIO_ROOT_USER:-minioadmin}
      MINIO_ROOT_PASSWORD: ${SHARED_MINIO_ROOT_PASSWORD:-changeme}
    volumes:
      - shared_minio_data:/data
    ports:
      - "9000:9000"  # API 端口
      - "9001:9001"  # 控制台
    networks:
      - shared-network
    restart: unless-stopped
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M

  minio-init:
    image: minio/mc:latest
    container_name: shared-minio-init
    depends_on:
      - shared-minio
    entrypoint: >
      /bin/sh -c "
      sleep 10;
      mc alias set myminio http://shared-minio:9000 $${SHARED_MINIO_ROOT_USER:-minioadmin} $${SHARED_MINIO_ROOT_PASSWORD:-changeme};
      mc mb --ignore-existing myminio/law-firm;
      mc mb --ignore-existing myminio/pis;
      mc anonymous set download myminio/law-firm/public;
      mc anonymous set download myminio/pis/public;
      echo 'Buckets created successfully';
      exit 0;
      "
    networks:
      - shared-network

volumes:
  shared_minio_data:
    driver: local

networks:
  shared-network:
    driver: bridge
    name: shared-network
```

#### 步骤 2：启动共享 MinIO

```bash
docker compose up -d
```

#### 步骤 3：配置应用使用共享 MinIO

在 `law-firm` 项目的 `.env` 文件中：

```bash
# 使用共享 MinIO（通过 Docker 网络）
MINIO_ENDPOINT=http://shared-minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=changeme
MINIO_BUCKET=law-firm
```

在 `pis` 项目的配置中：

```bash
MINIO_ENDPOINT=http://shared-minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=changeme
MINIO_BUCKET=pis
```

---

### 方案 2：在 law-firm 项目中共享 MinIO

如果 law-firm 项目已经部署了 MinIO，可以让其他应用连接到它。

#### 步骤 1：修改 law-firm 的 docker-compose.prod.yml

确保 MinIO 容器名称不是 `law-firm-minio`，而是 `shared-minio`：

```yaml
services:
  minio:
    container_name: shared-minio  # 改为共享名称
    # ... 其他配置
```

#### 步骤 2：创建共享网络

```bash
docker network create shared-network
```

#### 步骤 3：将 MinIO 加入共享网络

在 `docker-compose.prod.yml` 中添加：

```yaml
services:
  minio:
    networks:
      - law-firm-network
      - shared-network  # 添加到共享网络

networks:
  law-firm-network:
    driver: bridge
  shared-network:
    external: true  # 使用外部网络
```

#### 步骤 4：其他应用连接到共享网络

在 `pis` 项目的 docker-compose 中：

```yaml
services:
  pis-app:
    networks:
      - shared-network

networks:
  shared-network:
    external: true
```

---

## 🔒 安全配置

### 1. 为不同应用创建独立的访问密钥

使用 MinIO 控制台或 `mc` 命令创建：

```bash
# 为 law-firm 创建访问密钥
mc admin user add myminio law-firm-user
mc admin policy set myminio readwrite user=law-firm-user
mc admin policy attach myminio readwrite --user=law-firm-user --bucket=law-firm

# 为 pis 创建访问密钥
mc admin user add myminio pis-user
mc admin policy set myminio readwrite user=pis-user
mc admin policy attach myminio readwrite --user=pis-user --bucket=pis
```

### 2. 配置 bucket 策略

确保每个应用只能访问自己的 bucket：

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:*"],
      "Resource": ["arn:aws:s3:::law-firm/*"]
    }
  ]
}
```

---

## 📊 监控和维护

### 监控 MinIO 资源使用

```bash
# 查看容器资源使用
docker stats shared-minio

# 查看 MinIO 控制台
# 访问 http://服务器IP:9001
```

### 备份策略

共享 MinIO 的数据备份更加重要：

```bash
# 备份所有 bucket
mc mirror myminio/law-firm /backup/law-firm
mc mirror myminio/pis /backup/pis
```

---

## ⚠️ 注意事项

1. **数据隔离**：确保不同应用的 bucket 策略正确配置
2. **性能影响**：如果某个应用负载很高，可能影响其他应用
3. **清理脚本**：清理脚本不会删除 `shared-minio` 容器
4. **网络配置**：确保所有应用都能访问共享 MinIO

---

## 🔄 迁移现有数据

如果要从独立 MinIO 迁移到共享 MinIO：

```bash
# 1. 备份现有数据
mc mirror myminio-old/law-firm /backup/law-firm

# 2. 启动共享 MinIO
docker compose -f docker-compose.shared-minio.yml up -d

# 3. 恢复数据
mc mirror /backup/law-firm myminio-new/law-firm

# 4. 更新应用配置
# 修改 .env 文件中的 MINIO_ENDPOINT
```

---

## 📚 参考

- [MinIO 官方文档](https://min.io/docs/)
- [MinIO 多租户配置](https://min.io/docs/minio/kubernetes/tenant-management/)
- [Bucket 策略配置](https://min.io/docs/minio/kubernetes/tenant-management/tenant-policy-management.html)
- [配置说明](./configuration.md)
- [单端口架构](./single-port-architecture.md)

---

**最后更新**: 2026-01-31
