# 系统安全审计报告

## 审计时间
2026年1月

## 审计范围
- 后端 Spring Boot 应用
- 前端 Vue.js 应用
- 配置文件
- 数据库访问层
- 认证授权机制
- 文件上传处理
- API 接口安全

---

## 安全评估结果

### ✅ 已实施的安全措施

#### 1. SQL注入防护 ✅
- **状态**: 良好
- **说明**: 
  - 使用 MyBatis 参数化查询（`#{}`）
  - 所有 SQL 查询都使用 `@Param` 注解绑定参数
  - 未发现字符串拼接 SQL 的情况
- **示例代码**:
  ```java
  @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = false")
  User selectByUsername(@Param("username") String username);
  ```

#### 2. 密码安全 ✅
- **状态**: 良好
- **说明**:
  - 使用 BCrypt 密码加密算法
  - 密码存储前进行哈希处理
  - 支持密码修改时的旧密码验证
- **配置位置**: `SecurityConfig.java`

#### 3. 文件上传安全 ✅
- **状态**: 良好
- **说明**:
  - 文件类型白名单验证（`FileTypeService`）
  - 文件大小限制（100MB）
  - 文件扩展名检查
  - 支持的文件类型：图片、PDF、Office文档、音频、视频
- **限制**:
  - 最大文件大小: 100MB
  - 仅允许预定义的文件类型

#### 4. 权限验证机制 ✅
- **状态**: 良好
- **说明**:
  - 基于 JWT 的认证机制
  - 使用 `@RequirePermission` 注解进行权限控制
  - 管理员权限检查（`SecurityUtils.isAdmin()`）
  - 数据权限范围控制（ALL、DEPT_AND_CHILD、DEPT、SELF）
- **实现位置**: `PermissionAspect.java`

#### 5. 输入验证 ✅
- **状态**: 良好
- **说明**:
  - 使用 Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`)
  - 全局异常处理器统一处理验证错误
  - 参数校验失败时返回友好的错误信息

#### 6. 路径遍历防护 ✅
- **状态**: 良好
- **说明**:
  - 使用 `Paths.get().toAbsolutePath().normalize()` 规范化路径
  - 备份文件路径处理中使用了路径规范化
  - 防止 `../` 等路径遍历攻击

---

## ⚠️ 发现的安全问题

### 🔴 高风险问题

#### 1. JWT密钥默认值过弱
- **风险等级**: 🔴 高
- **位置**: `application.yml` 第75行
- **问题**:
  ```yaml
  jwt:
    secret: ${JWT_SECRET:your-256-bit-secret-key-here-change-in-production}
  ```
- **影响**: 
  - 如果生产环境未设置 `JWT_SECRET` 环境变量，将使用弱密钥
  - 攻击者可能伪造 JWT Token，获得未授权访问
- **建议**:
  1. **立即行动**: 在生产环境必须设置强随机密钥（至少256位）
  2. 使用环境变量或密钥管理服务（如 Vault、AWS Secrets Manager）
  3. 定期轮换 JWT 密钥
  4. 密钥生成示例:
     ```bash
     # 生成256位随机密钥
     openssl rand -base64 32
     ```

#### 2. 数据库密码默认值
- **风险等级**: 🔴 高
- **位置**: `application.yml` 第22行
- **问题**:
  ```yaml
  password: ${DB_PASSWORD:dev_password_123}
  ```
- **影响**: 
  - 开发环境默认密码过于简单
  - 如果生产环境未设置 `DB_PASSWORD`，将使用弱密码
- **建议**:
  1. **立即行动**: 生产环境必须设置强密码
  2. 使用环境变量管理敏感配置
  3. 不同环境使用不同的密码策略

#### 3. MinIO访问密钥默认值
- **风险等级**: 🔴 高
- **位置**: `application.yml` 第86-87行
- **问题**:
  ```yaml
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  ```
- **影响**: 
  - 默认密钥为 `minioadmin/minioadmin`，这是 MinIO 的默认值
  - 攻击者可能直接访问文件存储服务
- **建议**:
  1. **立即行动**: 生产环境必须修改 MinIO 默认密钥
  2. 使用强随机密钥
  3. 定期轮换访问密钥

---

### 🟡 中等风险问题

#### 4. Swagger UI 在生产环境暴露
- **风险等级**: 🟡 中
- **位置**: `SecurityConfig.java` 第72-74行
- **问题**:
  ```java
  .requestMatchers(
      "/swagger-ui/**",
      "/swagger-ui.html",
      "/v3/api-docs/**",
  ).permitAll()
  ```
- **影响**: 
  - API 文档暴露给未授权用户
  - 可能泄露接口结构、参数格式等信息
- **建议**:
  1. 生产环境禁用 Swagger UI，或仅允许内网访问
  2. 使用 Spring Profile 控制:
     ```java
     @Profile({"dev", "test"})
     public class SwaggerConfig { ... }
     ```
  3. 添加 IP 白名单限制

#### 5. XSS 跨站脚本攻击风险
- **风险等级**: 🟡 中
- **位置**: 
  - `RichTextEditor` 组件（前端）
  - 富文本内容直接插入 HTML
- **问题**:
  - 富文本编辑器允许用户输入 HTML 内容
  - 如果未对输出进行转义，可能导致 XSS 攻击
- **影响**: 
  - 恶意脚本可能窃取用户 Cookie、Token
  - 可能进行钓鱼攻击
- **建议**:
  1. 前端输出时使用 HTML 转义（如 `v-html` 需要谨慎使用）
  2. 后端对富文本内容进行 HTML 标签白名单过滤
  3. 使用专业的富文本编辑器（如已使用的 wangeditor），并配置 XSS 过滤
  4. 设置 Content Security Policy (CSP) 响应头

#### 6. 错误信息可能泄露敏感信息
- **风险等级**: 🟡 中
- **位置**: 多处异常处理代码
- **问题**:
  - 部分异常处理直接返回 `e.getMessage()`
  - 可能泄露数据库结构、文件路径等敏感信息
- **示例**:
  ```java
  throw new BusinessException("文件上传失败: " + e.getMessage());
  ```
- **影响**: 
  - 可能泄露系统内部结构
  - 可能泄露文件路径、数据库错误信息
- **建议**:
  1. 统一异常处理，避免直接返回异常消息
  2. 生产环境仅返回通用错误信息
  3. 详细错误信息仅记录在日志中
  4. 已实施的改进: `GlobalExceptionHandler` 已统一处理，但部分业务代码仍需优化

#### 7. CSRF 防护已禁用
- **风险等级**: 🟡 中（可接受）
- **位置**: `SecurityConfig.java` 第57行
- **问题**:
  ```java
  .csrf(AbstractHttpConfigurer::disable)
  ```
- **说明**: 
  - 使用 JWT Token 的 RESTful API 通常不需要 CSRF 防护
  - 这是常见做法，但需要注意 Token 存储安全
- **建议**:
  1. 确保 Token 存储在 `httpOnly` Cookie 或 `localStorage`（当前实现）
  2. 前端避免将 Token 暴露在 URL 中
  3. 实施 Token 刷新机制（已实施）

---

### 🟢 低风险问题

#### 8. 日志可能记录敏感信息
- **风险等级**: 🟢 低
- **位置**: `OperationLogAspect.java`
- **问题**:
  - 操作日志可能记录请求参数，包含敏感信息（密码、Token等）
- **建议**:
  1. 对敏感字段进行脱敏处理
  2. 密码字段不应记录到日志
  3. 已实施: `saveParams()` 可控制是否保存参数

#### 9. JWT Token 过期时间较长
- **风险等级**: 🟢 低
- **位置**: `application.yml` 第76-77行
- **问题**:
  ```yaml
  expiration: 86400000  # 24小时
  refresh-expiration: 604800000  # 7天
  ```
- **建议**:
  1. 根据业务需求调整 Token 过期时间
  2. 考虑实施 Token 撤销机制（已部分实施：Redis 缓存 Token）

---

## 📋 安全建议总结

### 立即修复（高优先级）

1. **生产环境配置检查清单**:
   - [ ] 设置强随机 `JWT_SECRET`（至少256位）
   - [ ] 设置强 `DB_PASSWORD`
   - [ ] 修改 MinIO 默认访问密钥
   - [ ] 禁用或限制 Swagger UI 访问
   - [ ] 检查所有环境变量是否已正确设置

2. **代码改进**:
   - [ ] 优化异常处理，避免泄露敏感信息
   - [ ] 实施 XSS 防护（HTML 转义/过滤）
   - [ ] 添加敏感字段日志脱敏

### 中期改进（中优先级）

1. **安全加固**:
   - [ ] 实施 Content Security Policy (CSP)
   - [ ] 添加 API 速率限制（Rate Limiting）
   - [ ] 实施登录失败次数限制
   - [ ] 添加操作审计日志

2. **监控和告警**:
   - [ ] 实施安全事件监控
   - [ ] 异常登录检测
   - [ ] 敏感操作告警

### 长期优化（低优先级）

1. **安全最佳实践**:
   - [ ] 定期安全审计
   - [ ] 依赖库漏洞扫描
   - [ ] 渗透测试
   - [ ] 安全培训

---

## 🔒 安全配置检查清单

### 生产环境部署前检查

- [ ] 所有默认密码已修改
- [ ] JWT 密钥已设置为强随机值
- [ ] 数据库密码已设置为强密码
- [ ] MinIO 访问密钥已修改
- [ ] Swagger UI 已禁用或限制访问
- [ ] 环境变量已正确配置
- [ ] HTTPS 已启用
- [ ] 防火墙规则已配置
- [ ] 日志级别已调整为生产级别
- [ ] 敏感信息已从代码中移除

---

## 📚 参考资源

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security 最佳实践](https://spring.io/guides/topicals/spring-security-architecture)
- [JWT 安全最佳实践](https://datatracker.ietf.org/doc/html/rfc8725)

---

## 结论

系统整体安全架构良好，已实施多项安全措施：
- ✅ SQL注入防护完善
- ✅ 密码加密使用 BCrypt
- ✅ 文件上传有类型和大小限制
- ✅ 权限验证机制完善

**主要风险集中在配置安全**：
- 🔴 生产环境必须修改所有默认密钥和密码
- 🟡 建议禁用或限制 Swagger UI
- 🟡 建议加强 XSS 防护和错误信息处理

**建议优先级**：
1. **立即**: 修复配置安全问题（JWT密钥、数据库密码、MinIO密钥）
2. **短期**: 优化异常处理和 XSS 防护
3. **长期**: 实施安全监控和定期审计

