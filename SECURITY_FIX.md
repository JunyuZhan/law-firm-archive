# Security Notes

本文件收敛为安全要点摘要，避免在仓库根目录保留过长的问题排查记录。

## Baseline

- 管理接口使用 JWT Bearer Token
- 开放接口使用来源系统 API Key
- 环境变量、私有密钥、本地缓存和编译产物不进入版本控制
- 外部回调与文件下载地址应使用可审计、可控的公网或受控内网地址

## Recommended References

- [安全说明](./docs/SECURITY.md)
- [API 对接指南](./docs/API%E5%AF%B9%E6%8E%A5%E6%8C%87%E5%8D%97.md)

## Maintenance Principle

安全文档应描述当前有效策略，不保留冗长的历史修复过程作为对外主文档。
