# 档案管理系统 CI/CD 与分发链路

本文只针对 `law-firm-archive` 项目，约定代码开发、镜像构建、内网推送与分发中心发布的标准流程。

## 1. 目标链路

```text
Mac 开发机
  -> Git 仓库（git.albertzhan.top）
  -> Forgejo Runner（myu）
  -> 内网镜像仓库 192.168.50.5:5050
  -> Dist Center（install.albertzhan.top）
  -> 测试机 / 客户机安装
```

约束：

- 镜像构建与推送只走内网仓库 `192.168.50.5:5050`
- 对外安装脚本默认下发公网可拉取仓库地址 `hub.albertzhan.top`
- 分发中心托管 `install.sh`、`update.sh`、`versions/latest.json` 与 compose 资产

## 2. 仓库工作流

- [.forgejo/workflows/ci.yml](/Users/apple/Documents/Project/law-firm-archive/.forgejo/workflows/ci.yml)
  普通推送时执行后端测试、前端构建、Compose 校验
- [.forgejo/workflows/release-dist.yml](/Users/apple/Documents/Project/law-firm-archive/.forgejo/workflows/release-dist.yml)
  tag 发布时构建镜像、推送私库、同步 Dist Center 并重启分发中心

推荐发布方式：

1. 在业务仓库打版本 tag，例如 `v0.1.0`
2. 推送 tag 到 Git 仓库
3. 等待 `release-dist` 工作流完成
4. 在 `install.albertzhan.top/projects/law-firm-archive/versions/latest.json` 验证版本更新

## 3. Runner 所需变量

建议在 Forgejo Actions 中配置：

- `vars.DIST_ROOT=/opt/dist-center`
- `vars.DIST_PROJECT=law-firm-archive`
- `vars.DEFAULT_REGISTRY=hub.albertzhan.top/law-firm-archive/`
- `vars.INTERNAL_REGISTRY=192.168.50.5:5050/law-firm-archive/`
- `vars.REGISTRY_HOST=192.168.50.5:5050`
- `secrets.REGISTRY_USERNAME`
- `secrets.REGISTRY_PASSWORD`

如果未配置，工作流会使用上述值作为默认回退。

## 4. Dist Center 同步内容

发布脚本会把以下资产同步到 `dist-center`：

- `docker/docker-compose.registry.yml` -> `assets/compose/law-firm-archive/docker-compose.yml`
- `docker/.env.registry.example` -> `assets/compose/law-firm-archive/.env.example`
- `docker/secrets/init-secrets.sh`
- `scripts/init-db/02-schema-consolidated.sql`

版本描述由 [scripts/release/update-dist-version.sh](/Users/apple/Documents/Project/law-firm-archive/scripts/release/update-dist-version.sh) 更新，关键字段包括：

- `version`
- `descriptor_version`
- `app_version`
- `default_registry`
- `internal_registry`
- `env.APP_VERSION`
- `env.APP_REGISTRY`

## 5. 本地手工发布兜底

如果 CI 临时不可用，仍可在 Linux 构建机手工执行：

```bash
cp docker/.env.registry.example docker/.env.registry
# 填好 REGISTRY_USERNAME / REGISTRY_PASSWORD / APP_VERSION
bash scripts/build-and-push-on-linux.sh
bash scripts/release/sync-dist-center.sh
bash scripts/release/update-dist-version.sh --version v0.1.0 --app-version v0.1.0 --snapshot
```

手工发布后，需要在分发中心目录执行：

```bash
docker compose -f /opt/dist-center/docker-compose.yml up --build -d
```
