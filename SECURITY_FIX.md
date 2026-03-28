# 安全配置修复 - 403错误解决方案

## 问题描述

前端在加载站点配置时遇到403权限错误：
```
GET http://localhost:3001/api/configs/group/SITE
Status: 403 Forbidden
Error: 加载站点配置失败
```

## 问题原因

Spring Security配置中的路径匹配错误。

### 原配置
```java
.requestMatchers("/api/configs/group/**").permitAll()
```

### 实际情况
- **application.yml** 配置了 `context-path: /api`
- **Controller** 路径是 `@RequestMapping("/configs")`
- **实际完整路径**: `/api/configs/group/{group}`
- **Spring Security匹配路径**: 不包含context-path，应该是 `/configs/group/**`

## 解决方案

修改 `SecurityConfig.java` 中的路径匹配规则：

```java
// 修改前
.requestMatchers("/api/configs/group/**").permitAll()

// 修改后
.requestMatchers("/configs/group/**").permitAll()
```

## 技术说明

### Spring Security路径匹配规则

Spring Security的 `requestMatchers()` 方法匹配的是**相对于context-path之后的路径**，而不是完整的URL路径。

#### 示例说明

假设配置：
```yaml
server:
  servlet:
    context-path: /api
```

Controller：
```java
@RestController
@RequestMapping("/configs")
public class ConfigController {
    @GetMapping("/group/{group}")
    public Result<List<SysConfig>> getByGroup(@PathVariable String group) {
        // ...
    }
}
```

那么：
- **完整URL**: `http://localhost:8090/api/configs/group/SITE`
- **Spring Security应匹配**: `/configs/group/**`
- **错误匹配**: `/api/configs/group/**` ❌

### 为什么会这样？

Spring Security的过滤器链在Servlet容器处理context-path之后才执行，所以它看到的路径已经去掉了context-path前缀。

```
完整请求流程：
1. 浏览器请求: http://localhost:8090/api/configs/group/SITE
2. Servlet容器处理context-path: /api
3. Spring Security看到的路径: /configs/group/SITE
4. Controller处理: /configs/group/SITE
```

## 验证修复

### 1. 编译项目
```bash
cd backend
mvn clean compile
```

### 2. 启动后端服务
```bash
mvn spring-boot:run
```

### 3. 测试接口
```bash
# 应该返回200 OK
curl http://localhost:8090/api/configs/group/SITE
```

### 4. 启动前端
前端应该能够成功加载站点配置，不再出现403错误。

## 相关配置文件

### 1. application.yml
```yaml
server:
  port: 8090
  servlet:
    context-path: /api  # 所有接口都有/api前缀
```

### 2. SecurityConfig.java
```java
.authorizeHttpRequests(auth -> auth
    // 公开的配置接口（登录前需要加载站点配置）
    .requestMatchers("/configs/group/**").permitAll()  // 注意：不包含/api
    // 其他配置...
)
```

### 3. ConfigController.java
```java
@RestController
@RequestMapping("/configs")  // 注意：不包含/api
public class ConfigController {
    @GetMapping("/group/{group}")
    public Result<List<SysConfig>> getByGroup(@PathVariable String group) {
        // ...
    }
}
```

## 其他需要注意的路径

检查SecurityConfig中的其他路径配置，确保都不包含context-path：

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/open/**").permitAll()           // ✅ 正确
    .requestMatchers("/auth/**").permitAll()           // ✅ 正确
    .requestMatchers("/configs/group/**").permitAll()  // ✅ 正确（已修复）
    .requestMatchers("/retention-periods/**").permitAll() // ✅ 正确
    // ...
)
```

## 常见错误模式

### ❌ 错误示例1：重复context-path
```java
// 错误：在SecurityConfig中包含了context-path
.requestMatchers("/api/configs/**").permitAll()
```

### ❌ 错误示例2：Controller中包含context-path
```java
// 错误：在Controller中包含了context-path
@RequestMapping("/api/configs")
```

### ✅ 正确示例
```java
// SecurityConfig
.requestMatchers("/configs/**").permitAll()

// Controller
@RequestMapping("/configs")
```

## 测试清单

- [x] 编译通过
- [ ] 后端启动成功
- [ ] 接口可以访问（curl测试）
- [ ] 前端可以加载站点配置
- [ ] 登录功能正常
- [ ] 其他公开接口正常

## 相关文档

- [Spring Security Reference - Request Matchers](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
- [Spring Boot - Context Path Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.server.server.servlet.context-path)

## 总结

这是一个典型的Spring Security配置问题，关键点是理解：
1. **context-path** 是Servlet容器级别的配置
2. **Spring Security** 的路径匹配在context-path之后
3. **Controller** 的路径映射也在context-path之后

记住：**SecurityConfig和Controller中的路径都不应该包含context-path！**
