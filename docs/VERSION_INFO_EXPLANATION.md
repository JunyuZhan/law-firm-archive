# 版本信息字段说明

## 📋 当前版本信息包含的字段

### 1. **应用版本号** (version)
- **用途**：标识当前部署的应用版本
- **格式**：`主版本号.次版本号.修订号`（如：1.0.0）
- **来源**：`pom.xml` 中的 `<version>` 标签
- **重要性**：⭐⭐⭐⭐⭐ 核心字段，用于版本识别和问题追踪

### 2. **构建时间** (buildTime)
- **用途**：记录应用打包构建的时间点
- **格式**：`yyyy-MM-dd HH:mm:ss`（如：2026-01-08 10:30:45）
- **来源**：Maven 构建时自动生成的时间戳
- **重要性**：⭐⭐⭐⭐ 用于判断部署的代码版本和排查问题

### 3. **Git 提交 ID** (gitCommit)
- **用途**：关联到具体的代码提交，便于追溯代码变更
- **格式**：Git commit hash 的短格式（如：a1b2c3d）
- **来源**：`git-commit-id-plugin` 在构建时获取
- **重要性**：⭐⭐⭐⭐⭐ 用于精确定位代码版本，支持回滚和问题排查

### 4. **运行环境** (profile)
- **用途**：标识当前运行的环境（开发/测试/生产）
- **格式**：Spring Profile 名称（如：dev, test, prod）
- **来源**：`spring.profiles.active` 配置
- **重要性**：⭐⭐⭐⭐ 用于区分不同环境的配置和行为

### 5. **Java 版本** (javaVersion)
- **用途**：记录运行时的 JVM 版本
- **格式**：Java 版本号（如：21.0.1）
- **来源**：`System.getProperty("java.version")`
- **重要性**：⭐⭐⭐ 用于排查兼容性问题

### 6. **Java 供应商** (javaVendor)
- **用途**：记录 JVM 供应商（如：Oracle, Eclipse Adoptium, OpenJDK）
- **格式**：供应商名称
- **来源**：`System.getProperty("java.vendor")`
- **重要性**：⭐⭐ 用于区分不同的 JVM 实现

### 7. **操作系统名称** (osName)
- **用途**：记录运行的操作系统
- **格式**：操作系统名称（如：Linux, Windows, macOS）
- **来源**：`System.getProperty("os.name")`
- **重要性**：⭐⭐⭐ 用于排查平台相关问题

### 8. **操作系统版本** (osVersion)
- **用途**：记录操作系统的具体版本
- **格式**：操作系统版本号
- **来源**：`System.getProperty("os.version")`
- **重要性**：⭐⭐ 用于详细的环境信息

### 9. **服务器时间** (serverTime)
- **用途**：显示服务器当前时间
- **格式**：`yyyy-MM-dd HH:mm:ss`
- **来源**：运行时获取的系统时间
- **重要性**：⭐⭐⭐ 用于时间同步检查和日志时间对比

---

## 🤔 为什么选择这些字段？

### 核心原则

1. **问题排查**：当系统出现问题时，需要快速定位：
   - 哪个版本？（版本号）
   - 什么时候构建的？（构建时间）
   - 对应哪次代码提交？（Git 提交）
   - 运行在什么环境？（运行环境）

2. **环境一致性**：确保不同环境的信息可对比：
   - Java 版本是否一致？
   - 操作系统是否相同？
   - 环境配置是否正确？

3. **运维管理**：便于运维人员：
   - 确认部署版本
   - 检查环境配置
   - 追踪问题根源

---

## 💡 可以考虑添加的字段

根据实际需求，可以考虑添加以下字段：

### 1. **数据库版本**
```java
// 可以通过 JDBC 查询
SELECT version();  // PostgreSQL
```
- **用途**：确认数据库版本兼容性
- **重要性**：⭐⭐⭐

### 2. **前端版本**
```typescript
// 从前端 package.json 读取
const frontendVersion = import.meta.env.VITE_APP_VERSION;
```
- **用途**：前后端版本对应关系
- **重要性**：⭐⭐⭐

### 3. **依赖版本**
- Spring Boot 版本
- PostgreSQL JDBC 驱动版本
- Redis 客户端版本
- **用途**：排查依赖冲突
- **重要性**：⭐⭐

### 4. **服务器信息**
- CPU 核心数
- 内存大小
- 磁盘空间
- **用途**：性能分析和容量规划
- **重要性**：⭐⭐

### 5. **启动参数**
- JVM 参数（-Xmx, -Xms）
- 环境变量
- **用途**：配置验证
- **重要性**：⭐⭐

### 6. **服务状态**
- 运行时长（uptime）
- 健康检查状态
- **用途**：监控和告警
- **重要性**：⭐⭐⭐

---

## 🔧 如何自定义版本信息

### 添加新字段

#### 1. 后端添加字段

编辑 `SysConfigController.java`：

```java
@GetMapping("/version")
public Result<Map<String, Object>> getVersionInfo() {
    // ... 现有代码 ...
    
    // 添加新字段
    info.put("databaseVersion", getDatabaseVersion());
    info.put("frontendVersion", "1.0.0"); // 或从配置读取
    
    return Result.success(info);
}

private String getDatabaseVersion() {
    try {
        return jdbcTemplate.queryForObject("SELECT version()", String.class);
    } catch (Exception e) {
        return "unknown";
    }
}
```

#### 2. 前端更新类型定义

编辑 `api/system/index.ts`：

```typescript
export function getVersionInfo() {
  return requestClient.get<{
    version: string;
    buildTime: string;
    gitCommit: string;
    profile: string;
    javaVersion: string;
    javaVendor: string;
    osName: string;
    osVersion: string;
    serverTime: string;
    databaseVersion?: string;  // 新增字段
    frontendVersion?: string;  // 新增字段
  }>('/system/config/version');
}
```

#### 3. 前端更新显示

编辑 `views/system/config/index.vue`，在版本信息页面添加新字段的显示。

---

## 📊 字段优先级建议

### 高优先级（必须）
- ✅ 应用版本号
- ✅ Git 提交 ID
- ✅ 构建时间
- ✅ 运行环境

### 中优先级（推荐）
- ✅ Java 版本
- ✅ 操作系统信息
- ⭐ 数据库版本
- ⭐ 前端版本
- ⭐ 服务器时间

### 低优先级（可选）
- ⭐ 依赖版本
- ⭐ 服务器资源信息
- ⭐ JVM 参数

---

## 🎯 总结

当前版本信息字段的选择基于以下考虑：

1. **最小化原则**：只包含最必要的信息，避免信息过载
2. **实用性**：每个字段都有明确的用途和价值
3. **可扩展性**：可以根据实际需求添加新字段
4. **标准化**：遵循常见的版本信息展示惯例

如果您觉得需要添加或删除某些字段，可以根据实际运维需求进行调整。

