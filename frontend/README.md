# 智慧律所管理系统 - 前端

**项目仓库**: [https://github.com/junyuzhan/law-firm](https://github.com/junyuzhan/law-firm)

基于 Vue 3 + Vite + TypeScript + Ant Design Vue 的律师事务所业务管理系统前端。

## 技术栈

- Vue 3.5+
- Vite 6
- TypeScript 5
- Ant Design Vue 4
- Pinia
- TailwindCSS

## 开发环境

```bash
# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev:antd

# 构建生产版本
pnpm build:antd

# 构建用户手册
pnpm build:docs
```

## 目录结构

```
frontend/
├── apps/web-antd/          # 主应用
│   ├── src/
│   │   ├── api/            # API 接口定义
│   │   ├── components/     # 业务组件
│   │   ├── views/          # 页面视图
│   │   ├── router/         # 路由配置
│   │   └── store/          # 状态管理
│   └── .env.*              # 环境配置
├── docs/                   # 用户手册站点
├── packages/               # 公共包
└── internal/               # 内部工具包
```

## 环境配置

- `.env` - 通用配置
- `.env.development` - 开发环境
- `.env.production` - 生产环境

## 部署

### Docker 部署

```bash
pnpm build:docker
```

### 静态部署

```bash
pnpm build:antd
# 产物在 apps/web-antd/dist/
```

## 用户手册

用户手册位于 `docs/` 目录，是独立的 VitePress 站点：

```bash
cd docs
pnpm install
pnpm run dev
```

## 相关文档

- [开发者指南](../docs/DEVELOPER_GUIDE.md)
- [前端组件指南](../docs/frontend-component-guide.md)

