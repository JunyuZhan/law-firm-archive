# 开发者文档

本目录存放开发者参考文档，供代码开发和维护人员使用。

## 文档索引

| 文档 | 说明 |
|------|------|
| [DEVELOPER_GUIDE.md](./DEVELOPER_GUIDE.md) | 项目架构与开发百科全书 |
| [BACKEND_IMPLEMENTATION_GUIDE.md](./BACKEND_IMPLEMENTATION_GUIDE.md) | 后端实现指南 |
| [frontend-component-guide.md](./frontend-component-guide.md) | 前端组件使用指南 |

## 用户手册

用户操作手册位于 `frontend/docs/`，是一个独立的 VitePress 文档站点：

```bash
cd frontend/docs
pnpm install
pnpm run dev
```

包含：
- 用户操作指南（各模块使用说明）
- API 接口文档
- 运维部署文档
