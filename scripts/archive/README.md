# 归档脚本说明

本目录存放已弃用或已替代的脚本。

## 📋 归档脚本列表

### 已弃用的脚本

- `check-production-ready.sh` - 生产环境检查脚本（旧版）
  - **状态**: 已弃用
  - **替代方案**: `scripts/deploy/pre-deploy-check.sh`
  - **原因**: `pre-deploy-check.sh` 功能更全面，检查项更多

## 📚 使用最新脚本

请使用最新的脚本：

- 部署前检查：`./scripts/deploy/pre-deploy-check.sh`
- 一键部署：`./scripts/deploy.sh`

---

**归档时间**: 2026-01-31
