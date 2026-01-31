# 开发者文档

本目录存放开发者参考文档。

## 文档索引

### 开发指南

| 文档 | 说明 |
|------|------|
| [BACKEND_IMPLEMENTATION_GUIDE.md](./BACKEND_IMPLEMENTATION_GUIDE.md) | 后端架构和代码实现指南 |
| [frontend-component-guide.md](./frontend-component-guide.md) | 前端组件使用指南 |
| [UTILITIES_GUIDE.md](./UTILITIES_GUIDE.md) | 工具类使用指南 |
| [VERSION_MANAGEMENT.md](./VERSION_MANAGEMENT.md) | 版本号管理说明 |

### 部署运维

> ⚠️ **重要**：部署运维相关文档已整合到前端文档站点，请访问 `frontend/docs/` 查看完整的运维手册。

> 📦 **已归档文档**：部分已整合的文档已移动到 `docs/archive/` 目录，保留作为历史记录。

| 文档 | 说明 |
|------|------|
| [ocr-setup-guide.md](./ocr-setup-guide.md) | OCR 服务配置指南 |
| [SHARED_MINIO_GUIDE.md](./SHARED_MINIO_GUIDE.md) | 共享 MinIO 配置指南 |
| [PROMETHEUS_CHINESE_GUIDE.md](./PROMETHEUS_CHINESE_GUIDE.md) | Prometheus 中文使用指南 |

### 安全相关

| 文档 | 说明 |
|------|------|
| [SECURITY_AUDIT_REPORT.md](./SECURITY_AUDIT_REPORT.md) | 安全审计报告与部署检查清单 |

### 功能设计

| 文档 | 说明 |
|------|------|
| [QR_CODE_VERIFICATION_DESIGN.md](./QR_CODE_VERIFICATION_DESIGN.md) | 函件二维码防伪验证系统设计 |
| [AI_USAGE_BILLING_DESIGN.md](./AI_USAGE_BILLING_DESIGN.md) | AI 使用计费设计 |

### 系统评估

| 文档 | 说明 |
|------|------|
| [ASSESSMENT_REPORTS.md](./ASSESSMENT_REPORTS.md) | 系统评估报告汇总 |
| [DATABASE_ASSESSMENT_REPORT.md](./DATABASE_ASSESSMENT_REPORT.md) | 数据库现状评估报告 |
| [PERFORMANCE_OPTIMIZATION_REPORT.md](./PERFORMANCE_OPTIMIZATION_REPORT.md) | 性能优化报告 |
| [TEST_COVERAGE_REPORT.md](./TEST_COVERAGE_REPORT.md) | 测试覆盖率报告 |
| [TEST_REPORT.md](./TEST_REPORT.md) | 生产环境测试报告 |
| [ROLE_PERMISSION_ANALYSIS.md](./ROLE_PERMISSION_ANALYSIS.md) | 角色权限分析报告 |
| [FINAL_PROJECT_COMPLETION_REPORT.md](./FINAL_PROJECT_COMPLETION_REPORT.md) | 数据库标准化项目完成报告 |

### 集成设计

| 文档 | 说明 |
|------|------|
| [CUSTOMER_SERVICE_INTEGRATION_GUIDE.md](./CUSTOMER_SERVICE_INTEGRATION_GUIDE.md) | 客户服务系统对接文档 |
| [FREE_API_INTEGRATION_DESIGN.md](./FREE_API_INTEGRATION_DESIGN.md) | 免费 API 集成设计方案 |
| [TIANYANCHA_INTEGRATION_DESIGN.md](./TIANYANCHA_INTEGRATION_DESIGN.md) | 天眼查 API 集成技术设计方案 |
| [STATE_COMPENSATION_DEEP_REVIEW.md](./STATE_COMPENSATION_DEEP_REVIEW.md) | 国家赔偿案件接入方案深度评估 |

### 未来计划

| 文档 | 说明 |
|------|------|
| [MOBILE_ADAPTATION_PLAN.md](./MOBILE_ADAPTATION_PLAN.md) | 移动端适配改造计划 |
| [PWA_INTEGRATION_PLAN.md](./PWA_INTEGRATION_PLAN.md) | PWA 集成方案 |
| [PWA_SECURITY_PERFORMANCE_ASSESSMENT.md](./PWA_SECURITY_PERFORMANCE_ASSESSMENT.md) | PWA 安全与性能评估报告 |

### 法律参考资料

| 文档 | 说明 |
|------|------|
| [民事案由规定2025.md](./民事案由规定2025.md) | 民事案件案由规定 |
| [刑事案件罪名汇总.md](./刑事案件罪名汇总.md) | 刑事案件罪名汇总 |
| [行政案件案由规定.md](./行政案件案由规定.md) | 行政案件案由规定 |
| [国家赔偿案件接入方案.md](./国家赔偿案件接入方案.md) | 国家赔偿案件接入方案 |

## Docker 部署文档

Docker 相关部署文档位于 `docker/` 目录：

| 文档 | 说明 |
|------|------|
| [DEPLOY.md](../docker/DEPLOY.md) | Docker 部署指南 |
| [DEPLOY-SWARM.md](../docker/DEPLOY-SWARM.md) | Docker Swarm 集群部署 |
| [DEPLOY-NAS.md](../docker/DEPLOY-NAS.md) | NAS 存储部署方案 |
| [DATA-SECURITY.md](../docker/DATA-SECURITY.md) | 数据安全指南 |
| [SSL 配置指南](../docker/ssl/README.md) | SSL 证书配置说明 |

## 用户手册

用户操作手册位于 `frontend/docs/`，是 VitePress 文档站点：

```bash
cd frontend/docs
pnpm install
pnpm run dev
```

## 脚本文档

脚本使用说明位于 `scripts/README.md`，包含：

- 部署脚本
- 备份恢复脚本
- 测试脚本
- 数据库初始化脚本
