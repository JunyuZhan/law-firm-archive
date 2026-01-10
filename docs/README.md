# 开发者文档

本目录存放开发者参考文档。

## 文档索引

### 开发指南

| 文档 | 说明 |
|------|------|
| [BACKEND_IMPLEMENTATION_GUIDE.md](./BACKEND_IMPLEMENTATION_GUIDE.md) | 后端架构和代码实现指南 |
| [frontend-component-guide.md](./frontend-component-guide.md) | 前端组件使用指南 |
| [UTILITIES_GUIDE.md](./UTILITIES_GUIDE.md) | 工具类使用指南 |

### 部署运维

| 文档 | 说明 |
|------|------|
| [PRODUCTION_DEPLOYMENT_CHECKLIST.md](./PRODUCTION_DEPLOYMENT_CHECKLIST.md) | 生产环境部署检查清单 |
| [PRODUCTION_QUICK_START.md](./PRODUCTION_QUICK_START.md) | 快速部署指南 |
| [ocr-setup-guide.md](./ocr-setup-guide.md) | OCR 服务配置指南 |

### 安全相关

| 文档 | 说明 |
|------|------|
| [SECURITY_AUDIT_REPORT.md](./SECURITY_AUDIT_REPORT.md) | 安全审计报告与部署检查清单 |

### 功能设计

| 文档 | 说明 |
|------|------|
| [QR_CODE_VERIFICATION_DESIGN.md](./QR_CODE_VERIFICATION_DESIGN.md) | 函件二维码防伪验证系统设计 |
| [VERSION_MANAGEMENT.md](./VERSION_MANAGEMENT.md) | 版本号管理说明 |

## Docker 部署文档

Docker 相关部署文档位于 `docker/` 目录：

| 文档 | 说明 |
|------|------|
| [DEPLOY.md](../docker/DEPLOY.md) | Docker 部署指南 |
| [DEPLOY-SWARM.md](../docker/DEPLOY-SWARM.md) | Docker Swarm 集群部署 |
| [DEPLOY-NAS.md](../docker/DEPLOY-NAS.md) | NAS 存储部署方案 |
| [DATA-SECURITY.md](../docker/DATA-SECURITY.md) | 数据安全指南 |

## 用户手册

用户操作手册位于 `frontend/docs/`，是 VitePress 文档站点：

```bash
cd frontend/docs
pnpm install
pnpm run dev
```
