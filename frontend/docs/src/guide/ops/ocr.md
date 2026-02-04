# OCR 服务配置指南

## 📋 概述

系统默认使用 PaddleOCR 服务进行 OCR 文字识别，支持身份证识别、营业执照识别等功能。

---

## 🚀 快速开始

### 步骤 1：启动 OCR 服务

```bash
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml up -d paddle-ocr
```

### 步骤 2：检查 OCR 服务状态

```bash
# 检查容器是否运行
docker ps | grep paddle-ocr

# 检查服务健康状态
curl http://localhost:8001/health
```

### 步骤 3：重启后端服务

确保后端服务已启动，OCR 功能即可正常使用。

---

## ⚙️ 配置说明

### 默认配置

**系统默认使用 PaddleOCR 服务**，不再使用 Mock 模拟服务。

**配置位置**：`backend/src/main/resources/application.yml`

```yaml
ocr:
  provider: paddle # 默认使用 PaddleOCR
  paddle:
    url: http://localhost:8001
```

**或者通过环境变量**：

```bash
export OCR_PROVIDER=paddle
export OCR_SERVICE_URL=http://localhost:8001
```

### 开发环境配置

**文件**：`backend/src/main/resources/application.yml`

```yaml
ocr:
  provider: ${OCR_PROVIDER:paddle} # paddle(默认) | mock(测试用)
  paddle:
    url: ${OCR_SERVICE_URL:http://localhost:8001}
  timeout: 30000
```

### 生产环境配置

**文件**：`backend/src/main/resources/application-prod.yml`

```yaml
ocr:
  provider: paddle
  paddle:
    url: ${OCR_SERVICE_URL:http://paddle-ocr:8000}
```

---

## 🔄 切换服务

### 使用 PaddleOCR 服务（默认）

```bash
# 1. 启动 OCR 服务
cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml up -d paddle-ocr

# 2. 确保配置为 paddle（默认）
# 无需额外配置，系统默认使用 PaddleOCR

# 3. 重启后端服务
```

### 切换到 Mock 服务（仅用于测试）

如果需要使用 Mock 服务进行测试，可以设置：

**通过环境变量**：

```bash
export OCR_PROVIDER=mock
```

**或修改配置文件**：

```yaml
ocr:
  provider: mock # 仅用于测试
```

Mock 服务的特点：

- ✅ 无需启动 OCR 容器
- ✅ 响应速度快
- ❌ 返回固定数据（"张三"、"北京某某科技有限公司"等）

---

## ✅ 验证 OCR 服务是否正常工作

### 1. 检查 OCR 服务日志

```bash
docker logs law-firm-ocr
```

### 2. 测试 OCR 接口

```bash
# 测试健康检查
curl http://localhost:8001/health

# 测试通用文字识别（需要上传图片）
curl -X POST http://localhost:8001/ocr/general \
  -F "file=@/path/to/image.jpg"
```

### 3. 在前端测试

1. 打开"客户管理"页面
2. 点击"新增客户"
3. 点击"身份证识别"或"营业执照识别"
4. 上传真实的身份证或营业执照图片
5. 检查识别结果是否为真实数据

---

## 🐛 常见问题

### Q1: OCR 服务启动失败？

**原因**：可能是 ARM 架构（Apple Silicon）兼容性问题

**解决**：

- 检查 Docker 镜像是否支持 ARM64
- 查看 OCR 容器日志：`docker logs law-firm-ocr`
- 首次启动需要下载模型，可能需要几分钟

### Q2: OCR 识别很慢？

**原因**：

- 首次识别需要下载模型（约 500MB）
- CPU 模式识别速度较慢

**解决**：

- 等待首次识别完成（模型会缓存）
- 后续识别会快很多
- 如需更快速度，可配置 GPU 模式（需要 NVIDIA GPU）

### Q3: 识别结果不准确？

**原因**：

- 图片质量差（模糊、倾斜、光线不足）
- 图片格式不支持

**解决**：

- 使用清晰、正面的图片
- 支持格式：JPG、PNG、GIF
- 建议图片大小：1-5MB

### Q4: OCR 识别结果总是固定的"张三"、"北京某某科技有限公司"等？

**说明**：

1. **OCR 服务未启动**：请先启动 `paddle-ocr` 容器
2. **使用了 Mock 服务**：检查配置是否为 `ocr.provider: mock`

---

## ⚠️ 注意事项

1. **首次启动**：OCR 服务首次启动需要下载模型文件（约 500MB），请耐心等待
2. **内存要求**：OCR 服务建议至少 2GB 可用内存
3. **网络要求**：首次启动需要网络下载模型
4. **默认配置**：系统默认使用 PaddleOCR 服务，请确保 OCR 服务容器正在运行
5. **服务未启动**：如果 OCR 服务未启动，OCR 调用会失败，请检查容器状态

---

## 📚 相关文档

- [配置说明](./configuration.md)
- [故障排查](./troubleshooting.md)
- [部署指南](./deployment.md)

---

**最后更新**: 2026-01-31
