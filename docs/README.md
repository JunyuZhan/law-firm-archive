# 开发者文档

本目录存放开发者参考文档，供代码开发和维护人员使用。

## 文档索引

### 开发指南

| 文档 | 说明 |
|------|------|
| [BACKEND_IMPLEMENTATION_GUIDE.md](./BACKEND_IMPLEMENTATION_GUIDE.md) | 后端实现指南 |
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
| [SECURITY_AUDIT_REPORT.md](./SECURITY_AUDIT_REPORT.md) | 安全审计报告 |
| [SECURITY_FIXES_SUMMARY.md](./SECURITY_FIXES_SUMMARY.md) | 安全修复总结 |

### 版本管理

| 文档 | 说明 |
|------|------|
| [VERSION_MANAGEMENT.md](./VERSION_MANAGEMENT.md) | 版本管理规范 |
| [VERSION_FORMAT_GUIDE.md](./VERSION_FORMAT_GUIDE.md) | 版本格式说明 |

### 其他

| 文档 | 说明 |
|------|------|
| [BUSINESS_LOGIC_REVIEW_REPORT.md](./BUSINESS_LOGIC_REVIEW_REPORT.md) | 业务逻辑审查报告 |
| [QR_CODE_VERIFICATION_DESIGN.md](./QR_CODE_VERIFICATION_DESIGN.md) | 二维码验证设计 |
| [TEST_COVERAGE_PLAN.md](./TEST_COVERAGE_PLAN.md) | 测试覆盖计划 |

## 用户手册

用户操作手册位于 `frontend/docs/`，是一个独立的 VitePress 文档站点：

```bash
cd frontend/docs
pnpm install
pnpm run dev
```

包含各模块的用户操作指南。
