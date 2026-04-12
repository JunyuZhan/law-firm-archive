# Deployment Notes

本文件保留为精简部署说明，详细流程以正式交付文档为准。

## Recommended Path

- 使用 Docker Compose 作为标准部署方式
- 使用版本号明确的镜像标签，而不是 `latest`
- 部署前完成数据库、对象存储和配置备份
- 部署后执行固定冒烟测试并留痕

## Canonical Documents

- [部署与升级手册](./docs/deployment-upgrade-guide.md)
- [发布前验收清单](./docs/release-checklist.md)
- [部署后冒烟测试](./docs/deployment-smoke-test.md)

## Rule

业务管理员负责系统内配置；程序部署、升级、回滚和故障恢复由运维管理员执行。
