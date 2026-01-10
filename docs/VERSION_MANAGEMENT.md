# 版本号管理说明

## 📌 概述

系统已实现完整的版本号管理机制，包括：
- 应用启动时显示版本信息
- API 接口查询版本信息
- 前端页面显示版本信息
- 日志中包含版本信息

## 🔧 版本号格式

版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范：

```
主版本号.次版本号.修订号[-SNAPSHOT]
```

示例：
- `1.0.0` - 正式版本
- `1.0.0-SNAPSHOT` - 开发版本
- `1.1.0` - 次版本更新
- `2.0.0` - 主版本更新

## 📍 版本号位置

### 1. Maven 项目配置

版本号定义在 `backend/pom.xml`：

```xml
<version>1.0.0-SNAPSHOT</version>
```

### 2. 版本信息文件

构建时会自动生成 `version.properties` 文件，包含：
- `version` - 应用版本号
- `build.time` - 构建时间
- `git.commit.id.abbrev` - Git 提交 ID（短格式）

### 3. Git 提交信息

构建时会自动生成 `git.properties` 文件（由 `git-commit-id-plugin` 生成），包含完整的 Git 信息。

## 🚀 使用方式

### 后端

#### 1. 应用启动时显示版本信息

应用启动时会自动在日志和控制台输出版本信息：

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                                                                               ║
║                   智慧律所管理系统启动成功                                      ║
║              Law Firm Management System Started Successfully                  ║
║                                                                               ║
║   版本号 (Version)    : 1.0.0                                                ║
║   构建时间 (Build Time): 2026-01-08 10:30:45                                  ║
║   Git 提交 (Commit)    : a1b2c3d                                              ║
║   运行环境 (Profile)   : prod                                                 ║
║   启动时间 (Start Time): 2026-01-08 10:35:20                                  ║
║                                                                               ║
╚═══════════════════════════════════════════════════════════════════════════════╝
```

#### 2. 通过 API 查询版本信息

```bash
GET /api/system/config/version
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "version": "1.0.0",
    "buildTime": "2026-01-08 10:30:45",
    "gitCommit": "a1b2c3d",
    "profile": "prod",
    "javaVersion": "21.0.1",
    "javaVendor": "Eclipse Adoptium",
    "osName": "Linux",
    "osVersion": "5.4.0",
    "serverTime": "2026-01-08 10:35:20"
  }
}
```

#### 3. 代码中获取版本信息

```java
import com.lawfirm.common.util.VersionUtils;

// 获取版本号
String version = VersionUtils.getVersion();

// 获取构建时间
String buildTime = VersionUtils.getBuildTime();

// 获取 Git 提交 ID
String gitCommit = VersionUtils.getGitCommit();

// 获取完整版本信息
VersionUtils.VersionInfo info = VersionUtils.getVersionInfo();
```

### 前端

#### 1. 通过 API 获取版本信息

```typescript
import { getVersionInfo } from '@/api/system';

const versionInfo = await getVersionInfo();
console.log(versionInfo.version); // 1.0.0
```

#### 2. 在系统配置页面查看

访问：**系统管理 > 系统配置 > 版本信息**

页面会显示：
- 应用版本号
- 构建时间
- Git 提交 ID
- 运行环境
- Java 版本信息
- 操作系统信息
- 服务器时间

## 🔨 构建配置

### Maven 插件配置

`pom.xml` 中已配置以下插件：

1. **git-commit-id-plugin** - 生成 Git 提交信息
2. **maven-resources-plugin** - 处理版本信息文件

### 构建时自动生成

执行构建命令时，会自动生成版本信息：

```bash
# 开发环境构建
mvn clean package

# 生产环境构建
mvn clean package -Pprod
```

构建完成后，`version.properties` 和 `git.properties` 会被打包到 JAR 文件中。

## 📝 更新版本号

### 1. 更新 Maven 版本号

编辑 `backend/pom.xml`：

```xml
<version>1.1.0</version>  <!-- 更新版本号 -->
```

### 2. 提交代码

```bash
git add backend/pom.xml
git commit -m "chore: 更新版本号到 1.1.0"
git tag v1.1.0  # 可选：创建 Git 标签
```

### 3. 重新构建

```bash
mvn clean package -Pprod
```

## 🔍 查看版本信息

### 方式1：查看启动日志

应用启动时，版本信息会输出到日志和控制台。

### 方式2：通过 API 查询

```bash
curl http://localhost:8080/api/system/config/version
```

### 方式3：前端页面查看

登录系统后，访问：**系统管理 > 系统配置 > 版本信息**

### 方式4：查看 JAR 文件内容

```bash
# 解压 JAR 文件
unzip -q law-firm-*.jar

# 查看版本信息文件
cat BOOT-INF/classes/version.properties
cat BOOT-INF/classes/git.properties
```

## ⚠️ 注意事项

1. **版本号格式**：遵循语义化版本规范，不要随意修改格式
2. **Git 仓库**：确保项目在 Git 仓库中，否则 Git 信息可能无法获取
3. **构建环境**：版本信息在构建时生成，运行时无法修改
4. **生产环境**：部署前确认版本号正确，避免版本混乱

## 📚 相关文件

- `backend/pom.xml` - Maven 项目配置，定义版本号
- `backend/src/main/resources/version.properties` - 版本信息模板文件
- `backend/src/main/java/com/lawfirm/common/util/VersionUtils.java` - 版本信息工具类
- `backend/src/main/java/com/lawfirm/infrastructure/config/VersionInfoRunner.java` - 启动时显示版本信息
- `backend/src/main/java/com/lawfirm/interfaces/rest/system/SysConfigController.java` - 版本信息 API 接口
- `frontend/apps/web-antd/src/views/system/config/index.vue` - 前端版本信息页面

## 🔗 参考资源

- [语义化版本 2.0.0](https://semver.org/lang/zh-CN/)
- [Maven 版本管理最佳实践](https://maven.apache.org/guides/getting-started/index.html)
- [Git Commit ID Plugin](https://github.com/git-commit-id/git-commit-id-maven-plugin)

