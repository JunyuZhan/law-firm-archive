# GitHub Actions 工作流说明

## 📋 工作流列表

### 1. `build-and-lint.yml` - 构建与 Lint 工作流

**功能**：
- ✅ 前端：安装依赖 → Lint → TypeCheck → 构建
- ✅ 后端：编译 → 测试 → 打包 JAR
- ✅ 质量门禁：所有检查通过后触发
- ✅ 自动提交：可选，在成功后可自动提交并推送

**触发条件**：
- `push` 到 `main`、`develop`、`feature/**` 分支
- `pull_request` 到 `main`、`develop` 分支
- 手动触发（`workflow_dispatch`）

**使用方式**：

#### 方式1：自动触发（推送到 main/develop）
```bash
git push origin main
# 或
git push origin develop
```
工作流会自动运行，如果所有检查通过，会自动提交并推送（如果有更改）。

#### 方式2：手动触发
1. 在 GitHub 仓库页面，点击 **Actions** 标签
2. 选择 **Build and Lint** 工作流
3. 点击 **Run workflow**
4. 选择分支和是否自动提交
5. 点击 **Run workflow** 按钮

#### 方式3：PR 检查
创建 Pull Request 到 `main` 或 `develop` 分支时，会自动运行检查（不会自动提交）。

## 🔧 工作流步骤详解

### 前端构建 (`frontend-build`)
1. **Checkout code** - 检出代码
2. **Setup pnpm** - 设置 pnpm 10
3. **Setup Node.js** - 设置 Node.js 20
4. **Install dependencies** - 安装依赖（使用 frozen-lockfile）
5. **Run Lint** - 运行 ESLint + Stylelint + Prettier
6. **Run TypeCheck** - 运行 TypeScript 类型检查
7. **Build frontend** - 构建前端（使用 8GB 内存）
8. **Upload artifacts** - 上传构建产物

### 后端构建 (`backend-build`)
1. **Checkout code** - 检出代码
2. **Setup Java** - 设置 Java 21 (Temurin)
3. **Build with Maven (compile)** - 编译代码（跳过测试）
4. **Run Tests** - 运行单元测试（使用 H2 数据库）
5. **Package JAR** - 打包生产环境 JAR
6. **Upload artifacts** - 上传 JAR 和测试报告

### 质量门禁 (`quality-gate`)
- 检查前端和后端构建是否都成功
- 如果任一失败，整个工作流标记为失败

### 自动提交 (`auto-commit`)
**触发条件**：
- ✅ 质量门禁通过
- ✅ 手动触发且选择了 `auto_commit`
- ✅ 推送到 `main`/`develop` 分支（非 PR）

**行为**：
- 检查是否有未提交的更改（排除构建产物）
- 如果有更改，自动提交并推送
- 提交信息包含构建和测试状态

## 📦 构建产物

工作流会保存以下构建产物（保留 7 天）：
- `frontend-dist`: 前端构建产物 (`frontend/apps/web-antd/dist`)
- `backend-jar`: 后端 JAR 文件 (`backend/target/*.jar`)
- `backend-test-results`: 后端测试报告

## ⚙️ 环境变量

- `TZ`: Asia/Shanghai
- `CI`: true
- `NODE_OPTIONS`: `--max-old-space-size=8192` (前端构建)
- `SPRING_PROFILES_ACTIVE`: `test` (测试) / `prod` (打包)

## 🔒 权限要求

- `auto-commit` job 需要 `contents: write` 权限
- 使用 `GITHUB_TOKEN` 进行提交和推送

## 📝 注意事项

1. **自动提交**：仅在 `main`/`develop` 分支或手动触发时启用
2. **构建产物**：不会提交构建产物（dist、target 等）
3. **跳过 CI**：自动提交的消息包含 `[skip ci]`，避免循环触发
4. **并发控制**：同一分支的多个工作流会取消进行中的任务

## 🐛 故障排查

### 前端 Lint 失败
```bash
# 本地运行 lint
cd frontend
pnpm lint
```

### 后端编译失败
```bash
# 本地编译
cd backend
mvn clean compile
```

### 测试失败
```bash
# 本地运行测试
cd backend
mvn test
```

### 自动提交未触发
- 检查是否在 `main`/`develop` 分支
- 检查是否有未提交的更改（排除构建产物）
- 检查工作流权限设置

## 🔗 相关文档

- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [pnpm 文档](https://pnpm.io/)
- [Maven 文档](https://maven.apache.org/)
