# 脚本和文档清理整合计划

## 📋 清理目标

整理和整合项目中的脚本和文档，去除冗余，统一文档位置。

---

## 🔧 脚本清理

### 1. 已弃用的脚本

| 脚本 | 状态 | 替代方案 | 建议操作 |
|------|------|----------|----------|
| `scripts/deploy/check-production-ready.sh` | ⚠️ 已弃用 | `pre-deploy-check.sh` | **删除**或移动到 `archive/` 目录 |

**理由**：
- 已被 `pre-deploy-check.sh` 完全替代
- 功能更全面，检查项更多
- 文档中已标记为"旧版"

**操作建议**：
```bash
# 选项1：直接删除
rm scripts/deploy/check-production-ready.sh

# 选项2：移动到归档目录（保留历史）
mkdir -p scripts/archive
mv scripts/deploy/check-production-ready.sh scripts/archive/
```

---

## 📚 文档整合

### 1. 单端口架构相关文档（已整合到前端文档站点）

以下文档已整合到 `frontend/docs/src/guide/ops/`，可以删除或归档：

| 文档 | 状态 | 新位置 | 建议操作 |
|------|------|--------|----------|
| `docs/FINAL-CONFIGURATION-SUMMARY.md` | ✅ 已整合 | `frontend/docs/src/guide/ops/single-port-architecture.md` | **删除**或归档 |
| `docs/SINGLE-PORT-MIGRATION-COMPLETED.md` | ✅ 已整合 | `frontend/docs/src/guide/ops/single-port-migration.md` | **删除**或归档 |
| `docs/SINGLE-PORT-MIGRATION-GUIDE.md` | ✅ 已整合 | `frontend/docs/src/guide/ops/single-port-migration.md` | **删除**或归档 |
| `docs/PORT-EXPOSURE-ANALYSIS.md` | ✅ 已整合 | `frontend/docs/src/guide/ops/single-port-architecture.md` | **删除**或归档 |

**理由**：
- 内容已整合到前端文档站点
- 前端文档站点是用户主要访问的地方
- 避免文档分散，维护困难

### 2. 数据库相关文档（已整合到前端文档站点）

| 文档 | 状态 | 新位置 | 建议操作 |
|------|------|--------|----------|
| `docs/DATABASE-MAINTENANCE-EXPLAINED.md` | ✅ 已整合 | `frontend/docs/src/guide/ops/database-maintenance.md` | **删除**或归档 |
| `docs/DATABASE-ACCESS-EXPLAINED.md` | ✅ 已整合 | `frontend/docs/src/guide/ops/database-maintenance.md` | **删除**或归档 |

**理由**：
- 内容已整合到前端文档站点
- 避免重复维护

### 3. OnlyOffice 相关文档

| 文档 | 状态 | 建议操作 |
|------|------|----------|
| `docs/ONLYOFFICE-MINIO-CONFIGURATION-CHECK.md` | ⚠️ 可整合 | 整合到 `frontend/docs/src/guide/ops/onlyoffice.md` |
| `docs/ONLYOFFICE-MINIO-INTEGRATION-TEST.md` | ⚠️ 可整合 | 整合到 `frontend/docs/src/guide/ops/onlyoffice.md` |

**理由**：
- 内容与前端文档站点的 OnlyOffice 配置文档重复
- 建议整合到统一位置

### 4. Docker 部署文档

| 文档 | 状态 | 建议操作 |
|------|------|----------|
| `docker/服务器部署指南.md` | ⚠️ 可整合 | 整合到 `frontend/docs/src/guide/ops/deployment.md` |

**理由**：
- 与前端文档站点的部署指南内容重复
- 建议统一到前端文档站点

---

## 📁 建议的文档结构

### 保留在 `docs/` 目录的文档

**开发指南类**（开发者参考）：
- `BACKEND_IMPLEMENTATION_GUIDE.md`
- `frontend-component-guide.md`
- `UTILITIES_GUIDE.md`
- `VERSION_MANAGEMENT.md`

**设计文档类**（功能设计）：
- `QR_CODE_VERIFICATION_DESIGN.md`
- `AI_USAGE_BILLING_DESIGN.md`
- `FREE_API_INTEGRATION_DESIGN.md`
- `TIANYANCHA_INTEGRATION_DESIGN.md`

**评估报告类**（项目评估）：
- `ASSESSMENT_REPORTS.md`
- `DATABASE_ASSESSMENT_REPORT.md`
- `PERFORMANCE_OPTIMIZATION_REPORT.md`
- `TEST_COVERAGE_REPORT.md`
- `TEST_REPORT.md`
- `SECURITY_AUDIT_REPORT.md`

**计划文档类**（未来计划）：
- `MOBILE_ADAPTATION_PLAN.md`
- `PWA_INTEGRATION_PLAN.md`
- `PWA_SECURITY_PERFORMANCE_ASSESSMENT.md`

**法律参考资料**：
- `民事案由规定2025.md`
- `刑事案件罪名汇总.md`
- `行政案件案由规定.md`
- `国家赔偿案件接入方案.md`

### 整合到前端文档站点的文档

**运维手册类**（用户/运维人员参考）：
- ✅ 单端口架构相关（已完成）
- ✅ 数据库维护相关（已完成）
- ⚠️ OnlyOffice 配置（待整合）
- ⚠️ Docker 部署指南（待整合）

---

## 🗂️ 建议创建归档目录

```bash
# 创建归档目录
mkdir -p docs/archive
mkdir -p scripts/archive

# 移动已整合的文档
mv docs/FINAL-CONFIGURATION-SUMMARY.md docs/archive/
mv docs/SINGLE-PORT-MIGRATION-COMPLETED.md docs/archive/
mv docs/SINGLE-PORT-MIGRATION-GUIDE.md docs/archive/
mv docs/PORT-EXPOSURE-ANALYSIS.md docs/archive/
mv docs/DATABASE-MAINTENANCE-EXPLAINED.md docs/archive/
mv docs/DATABASE-ACCESS-EXPLAINED.md docs/archive/

# 移动已弃用的脚本
mv scripts/deploy/check-production-ready.sh scripts/archive/
```

---

## ✅ 执行步骤

### 阶段1：整合 OnlyOffice 文档

1. 将 `docs/ONLYOFFICE-MINIO-CONFIGURATION-CHECK.md` 内容整合到 `frontend/docs/src/guide/ops/onlyoffice.md`
2. 将 `docs/ONLYOFFICE-MINIO-INTEGRATION-TEST.md` 内容整合到 `frontend/docs/src/guide/ops/onlyoffice.md`
3. 移动原文档到 `docs/archive/`

### 阶段2：整合 Docker 部署文档

1. 将 `docker/服务器部署指南.md` 内容整合到 `frontend/docs/src/guide/ops/deployment.md`
2. 移动原文档到 `docs/archive/`

### 阶段3：清理已整合文档

1. 移动单端口架构相关文档到 `docs/archive/`
2. 移动数据库相关文档到 `docs/archive/`
3. 更新 `docs/README.md`，添加归档说明

### 阶段4：清理已弃用脚本

1. 移动 `check-production-ready.sh` 到 `scripts/archive/`
2. 更新 `scripts/README.md`，移除相关说明

---

## 📝 注意事项

1. **不要直接删除**：建议先移动到 `archive/` 目录，保留历史记录
2. **更新引用**：清理后需要更新所有文档中的链接引用
3. **更新 README**：更新 `docs/README.md` 和 `scripts/README.md`
4. **Git 提交**：分阶段提交，便于回滚

---

**最后更新**: 2026-01-31
