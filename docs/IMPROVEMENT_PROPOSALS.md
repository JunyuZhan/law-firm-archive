# 智慧律所管理系统 - 架构改进建议

> **文档版本**：2.0  
> **创建日期**：2026-01-10  
> **更新日期**：2026-01-10（深度检查更新）  
> **参考项目**：law-firm-infra（成熟多模块架构）

---

## 概述

本文档基于对 `law-firm-infra` 项目的**深度分析**（特别是 common 层的 9 个子模块），结合当前项目已完成的功能，提出系统性的改进建议。

### 参考项目 common 层模块结构

```
law-firm-common/
├── common-cache/      # 缓存服务（含限流、防重、熔断、降级、预热）
├── common-core/       # 核心基础（异常、常量、配置、通用结果）
├── common-data/       # 数据层（分页查询、数据源切换、实体基类）
├── common-log/        # 日志服务（操作日志注解、MDC追踪、日志切面）
├── common-message/    # 消息服务
├── common-security/   # 安全服务（上下文、加密、脱敏、权限注解）
├── common-test/       # 测试基础设施（基类、配置）
├── common-util/       # 通用工具类（断言、日期、加密、文件、ID生成）
└── common-web/        # Web层（XSS过滤、分页请求、响应工具）
```

### 核心发现

| 能力 | 当前项目 | 参考项目 | 差距 |
|------|----------|----------|------|
| 缓存抽象 | 直接 RedisTemplate | CacheService + 熔断降级 | ⚠️ 高 |
| 限流防重 | 硬编码在 AuthService | @RateLimiter + @RepeatSubmit | ⚠️ 高 |
| 安全上下文 | 直接依赖 Spring Security | 抽象 SecurityContext | ⚠️ 中 |
| 数据脱敏 | 无 | SensitiveDataService | ⚠️ 高 |
| 请求追踪 | 无 | MDC TraceId Filter | ⚠️ 高 |
| XSS 防护 | 无 | XssFilter | ⚠️ 高 |
| 存储策略 | 硬编码 MinIO | 策略模式（多云支持） | ⚠️ 中 |
| 测试覆盖 | 4 文件 (0.4%) | 78 文件 (3.2%) | ⚠️ 高 |

---

## 一、对比分析

### 1.1 项目规模对比

| 项目 | 模块数量 | 源码文件数 | 测试覆盖 |
|------|----------|-----------|----------|
| 当前项目 (law-firm) | 单体应用 | ~250 个 | 较低 |
| 参考项目 (law-firm-infra) | 8+ 个独立模块 | ~1,000+ 个 | 较高（每模块有测试） |

### 1.2 架构层次对比

**当前项目结构：**
```
backend/
├── common/           # 通用组件（简单）
├── domain/           # 领域层
├── application/      # 应用层
├── infrastructure/   # 基础设施层
└── interfaces/       # 接口层
```

**参考项目结构：**
```
law-firm-infra/
├── law-firm-common/          # 通用模块（8个子模块）
│   ├── common-core/          # 核心抽象
│   ├── common-util/          # 工具类
│   ├── common-data/          # 数据访问
│   ├── common-web/           # Web支持
│   ├── common-security/      # 安全框架
│   ├── common-cache/         # 缓存框架
│   ├── common-log/           # 日志框架
│   ├── common-message/       # 消息框架
│   └── common-test/          # 测试框架
├── law-firm-core/            # 核心业务
└── law-firm-modules/         # 业务模块
```

---

## 二、Common 层深度分析与改进建议

### 2.1 响应结果标准化

**参考项目亮点：**
- `CommonResult<T>` 提供丰富的静态工厂方法
- 同时支持 `data` 和 `result` 字段，兼容 vue-vben-admin
- 使用 `ResultCode` 枚举统一管理错误码

**当前项目现状：**
```java
// 当前：Result.java 使用 String 类型的 code
public static <T> Result<T> error(String code, String message) {
    return new Result<>(false, code, message, null);
}
```

**改进建议：**
```java
// 建议：使用枚举统一管理响应码
@Getter
public enum ResultCode {
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    // 业务错误码
    VALIDATION_ERROR(600, "参数验证错误"),
    BUSINESS_ERROR(601, "业务逻辑错误"),
    CONCURRENT_ERROR(602, "并发操作错误"),
    RATE_LIMIT_ERROR(603, "请求频率超限");
    
    private final int code;
    private final String message;
}
```

**工作量：** 1 天
**优先级：** 中

---

### 2.2 工具类体系完善

**参考项目 common-util 模块亮点：**

| 工具类 | 功能 | 当前项目是否具备 |
|--------|------|-----------------|
| `Assert` | 断言验证工具 | ❌ 缺失 |
| `CryptoUtils` | AES-GCM 加密 | ❌ 缺失 |
| `DateUtils` | 日期时间处理 | ❌ 缺失 |
| `JsonUtils` | JSON 序列化 | ⚠️ 使用 fastjson2 |
| `HttpUtils` | HTTP 客户端 | ❌ 缺失 |
| `FileValidator` | 文件安全验证 | ❌ 缺失 |
| `IdGenerator` | 雪花ID生成 | ❌ 缺失 |
| `ExcelWriter/Reader` | Excel 处理 | ✅ 有 ExcelImportExportService |
| `ServletUtils` | Web工具 | ✅ 有，但较简单 |
| `BeanUtils` | 对象拷贝 | ❌ 缺失 |

**高优先级改进：**

#### 2.2.1 添加 Assert 断言工具

```java
package com.lawfirm.common.util;

/**
 * 断言工具类 - 用于参数校验，快速失败
 */
public final class Assert {
    
    private Assert() {}
    
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }
    
    public static void notEmpty(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(message);
        }
    }
    
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(message);
        }
    }
    
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(message);
        }
    }
    
    public static void inRange(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw new BusinessException(message);
        }
    }
}
```

**工作量：** 0.5 天

#### 2.2.2 添加 FileValidator 文件安全验证

```java
package com.lawfirm.common.util;

/**
 * 文件安全验证器
 * - 验证文件扩展名（白名单 + 黑名单）
 * - 验证 MIME 类型
 * - 验证文件签名（魔数），防止文件伪装
 */
public class FileValidator {
    
    // 允许的文件扩展名
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "jpg", "jpeg", "png", "gif", "zip", "txt", "csv"
    );
    
    // 禁止的可执行文件
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
        "exe", "dll", "bat", "sh", "ps1", "vbs", "js", "jar"
    );
    
    // 文件签名（魔数）映射
    private static final Map<String, List<byte[]>> FILE_SIGNATURES = Map.of(
        "pdf", List.of(new byte[]{0x25, 0x50, 0x44, 0x46}), // %PDF
        "png", List.of(new byte[]{(byte)0x89, 0x50, 0x4E, 0x47}),
        // ... 更多签名
    );
    
    public static ValidationResult validate(MultipartFile file) {
        // 1. 验证文件大小
        // 2. 验证扩展名
        // 3. 验证 MIME 类型
        // 4. 验证文件签名
    }
}
```

**工作量：** 1 天
**优先级：** 高（安全相关）

#### 2.2.3 添加 DateUtils 日期工具

```java
package com.lawfirm.common.util;

/**
 * 日期时间工具类
 */
public class DateUtils {
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    public static String formatDate(LocalDate date) { ... }
    public static String formatDateTime(LocalDateTime dateTime) { ... }
    public static LocalDate parseDate(String dateStr) { ... }
    public static LocalDateTime parseDateTime(String dateTimeStr) { ... }
    public static LocalDate getFirstDayOfMonth(LocalDate date) { ... }
    public static LocalDate getLastDayOfMonth(LocalDate date) { ... }
    public static long getDaysBetween(LocalDate start, LocalDate end) { ... }
    public static boolean isWeekend(LocalDate date) { ... }
}
```

**工作量：** 0.5 天

---

### 2.3 日志体系增强

**参考项目 common-log 模块亮点：**

1. **`@OperationLog` 注解增强**
   - 支持 `async` 异步记录
   - 支持 `excludeFields` 排除敏感字段
   - 支持 `logStackTrace` 控制异常堆栈

2. **MDC Trace ID 链路追踪**
   - 自动生成 traceId
   - 支持从请求头传递
   - 响应头返回 traceId

3. **日志切面增强**
   - 可配置的日志级别
   - 路径排除规则
   - 异步日志执行器

**当前项目现状：**
```java
// 当前 @OperationLog 较简单
public @interface OperationLog {
    String module();
    String action();
    boolean saveParams() default true;
    boolean saveResult() default false;
}
```

**改进建议：**

#### 2.3.1 增强 OperationLog 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    String module();
    String action();
    
    /** 是否异步记录 */
    boolean async() default true;
    
    /** 是否记录请求参数 */
    boolean logParams() default true;
    
    /** 是否记录返回值 */
    boolean logResult() default false;
    
    /** 是否记录异常堆栈 */
    boolean logStackTrace() default true;
    
    /** 需要排除的敏感字段 */
    String[] excludeFields() default {"password", "token", "secret"};
}
```

**工作量：** 0.5 天

#### 2.3.2 添加 MDC 链路追踪

```java
package com.lawfirm.infrastructure.filter;

/**
 * MDC 日志追踪过滤器
 * 每个请求自动生成 traceId，用于日志关联
 */
public class MdcTraceIdFilter extends OncePerRequestFilter {
    
    private static final String TRACE_ID = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        try {
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            
            MDC.put(TRACE_ID, traceId);
            response.addHeader(TRACE_ID_HEADER, traceId);
            
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
```

**logback 配置更新：**
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
```

**工作量：** 0.5 天
**优先级：** 高（运维排查必备）

---

### 2.4 缓存体系增强

**参考项目 common-cache 模块亮点：**

1. **统一缓存服务接口 `CacheService`**
   - 支持 String、Hash、List、Set 操作
   - 支持过期时间设置
   - 支持批量操作

2. **声明式注解**
   - `@RateLimiter` 限流注解
   - `@RepeatSubmit` 防重提交注解
   - `@SimpleCache` 简单缓存注解
   - `@CacheWarmUp` 缓存预热注解

3. **熔断降级服务 `CircuitBreakerService`**
   - 熔断器状态：CLOSED、OPEN、HALF_OPEN
   - 自动统计失败率
   - 防止缓存雪崩

**当前项目现状：**
- 有 Redis 配置
- 缺乏统一的缓存服务接口
- 缺乏限流、防重等注解

**改进建议：**

#### 2.4.1 添加 @RateLimiter 限流注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    /** 限流 key */
    String key() default "";
    
    /** 速率（每个时间间隔允许的请求数） */
    long rate() default 10;
    
    /** 时间间隔（秒） */
    long interval() default 1;
    
    /** 限流提示语 */
    String message() default "请求过于频繁，请稍后重试";
}
```

**工作量：** 1 天
**优先级：** 高（防止接口滥用）

#### 2.4.2 添加 @RepeatSubmit 防重提交注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {
    /** 间隔时间（默认3秒内不允许重复提交） */
    long interval() default 3;
    
    /** 时间单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /** 提示消息 */
    String message() default "请勿重复提交";
}
```

**工作量：** 0.5 天
**优先级：** 高（防止重复操作）

---

### 2.5 安全体系增强

**参考项目 common-security 模块亮点：**

1. **声明式权限注解**
   - `@RequiresPermissions` 权限校验
   - `@RequiresRoles` 角色校验
   - 支持 AND/OR 逻辑组合

2. **独立的安全上下文**
   - `SecurityContext` 接口
   - `SecurityContextHolder` 持有者
   - 线程安全的用户信息存储

3. **加密服务**
   - `CryptoService` 加解密服务
   - `EncryptionService` 敏感数据加密
   - `SensitiveDataService` 脱敏服务

**当前项目现状：**
- 有 `@RequirePermission` 注解
- 有 `SecurityUtils` 工具类
- 缺乏完善的加密服务

**改进建议：**

#### 2.5.1 增强权限注解

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /** 需要的权限 */
    String[] value();
    
    /** 多权限逻辑关系 */
    Logical logical() default Logical.AND;
}

public enum Logical {
    AND,  // 必须同时拥有所有权限
    OR    // 拥有任一权限即可
}
```

**工作量：** 0.5 天

#### 2.5.2 添加加密服务

```java
package com.lawfirm.common.util;

/**
 * 加密工具类
 * 使用 AES-GCM 模式（比 ECB 更安全）
 */
public class CryptoUtils {
    
    /** AES-GCM 加密 */
    public static String encryptAESGCM(String content, String key) { ... }
    
    /** AES-GCM 解密 */
    public static String decryptAESGCM(String encrypted, String key) { ... }
    
    /** RSA 加密 */
    public static String rsaEncrypt(String content, PublicKey publicKey) { ... }
    
    /** RSA 解密 */
    public static String rsaDecrypt(String encrypted, PrivateKey privateKey) { ... }
    
    /** 生成 RSA 密钥对 */
    public static KeyPair generateKeyPair() { ... }
}
```

**工作量：** 1 天

---

### 2.6 Web 层增强

**参考项目 common-web 模块亮点：**

1. **XSS 过滤器**
   - 自动过滤请求参数中的 XSS 攻击代码
   - 支持路径排除配置

2. **统一分页请求**
   - `PageRequest` 基类
   - 支持 `keyword`、`startTime`、`endTime`、`orderField`、`orderDirection`

3. **响应工具类**
   - `ResponseUtils` 统一响应处理
   - `WebUtils` Web 工具方法

**当前项目改进建议：**

#### 2.6.1 添加 XSS 过滤器

```java
package com.lawfirm.infrastructure.filter;

/**
 * XSS 过滤器
 * 自动过滤请求参数中的潜在 XSS 攻击代码
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter {
    
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
        "/health", "/doc.html", "/swagger-ui", "/v3/api-docs"
    );
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        
        if (EXCLUDE_PATHS.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(new XssHttpServletRequestWrapper(httpRequest), response);
        }
    }
    
    private static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
        @Override
        public String getParameter(String name) {
            return cleanXss(super.getParameter(name));
        }
        
        @Override
        public String getHeader(String name) {
            return cleanXss(super.getHeader(name));
        }
        
        private String cleanXss(String value) {
            if (value == null) return null;
            return HtmlUtil.escape(value); // 使用 hutool
        }
    }
}
```

**工作量：** 0.5 天
**优先级：** 高（安全相关）

---

## 三、改进优先级总览

### 3.1 高优先级（安全/运维必备）

| 改进项 | 工作量 | 描述 |
|--------|--------|------|
| FileValidator 文件验证 | 1 天 | 防止恶意文件上传 |
| XssFilter 过滤器 | 0.5 天 | 防止 XSS 攻击 |
| MDC 链路追踪 | 0.5 天 | 日志关联，问题排查 |
| @RateLimiter 限流 | 1 天 | 防止接口滥用 |
| @RepeatSubmit 防重 | 0.5 天 | 防止重复提交 |

**总计：** 3.5 天

### 3.2 中优先级（代码质量）

| 改进项 | 工作量 | 描述 |
|--------|--------|------|
| ResultCode 枚举 | 1 天 | 统一错误码管理 |
| Assert 断言工具 | 0.5 天 | 参数校验快速失败 |
| DateUtils 日期工具 | 0.5 天 | 统一日期处理 |
| CryptoUtils 加密工具 | 1 天 | 安全加密能力 |
| OperationLog 增强 | 0.5 天 | 日志功能增强 |

**总计：** 3.5 天

### 3.3 低优先级（长期优化）

| 改进项 | 工作量 | 描述 |
|--------|--------|------|
| CacheService 接口 | 2 天 | 统一缓存服务 |
| CircuitBreaker 熔断 | 2 天 | 防止雪崩 |
| 权限注解增强 | 0.5 天 | AND/OR 逻辑支持 |
| JsonUtils 统一 | 1 天 | 替换 fastjson2 |
| HttpUtils 工具 | 1 天 | HTTP 客户端封装 |

**总计：** 6.5 天

---

## 四、实施建议

### 4.1 第一阶段：安全加固（1周）

1. 添加 `FileValidator` 文件安全验证
2. 添加 `XssFilter` 过滤器
3. 添加 MDC 链路追踪
4. 添加 `@RateLimiter` 限流注解

### 4.2 第二阶段：工具完善（1周）

1. 添加 `Assert` 断言工具
2. 添加 `DateUtils` 日期工具
3. 添加 `CryptoUtils` 加密工具
4. 增强 `@OperationLog` 注解
5. 添加 `@RepeatSubmit` 防重注解

### 4.3 第三阶段：架构优化（长期）

1. 统一 `ResultCode` 错误码
2. 抽取 `CacheService` 接口
3. 添加熔断降级能力
4. 考虑模块化拆分

---

## 五、参考代码示例

### 5.1 完整的 Assert 工具类

```java
package com.lawfirm.common.util;

import com.lawfirm.common.exception.BusinessException;
import java.util.Collection;
import java.util.Map;

/**
 * 断言工具类
 * 用于参数校验，快速失败
 */
public final class Assert {
    
    private Assert() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }
    
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }
    
    public static void isNull(Object object, String message) {
        if (object != null) {
            throw new BusinessException(message);
        }
    }
    
    public static void notEmpty(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(message);
        }
    }
    
    public static void notBlank(String str, String message) {
        if (str == null || str.isBlank()) {
            throw new BusinessException(message);
        }
    }
    
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(message);
        }
    }
    
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(message);
        }
    }
    
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new BusinessException(message);
        }
    }
    
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(message);
        }
    }
    
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(message);
        }
    }
    
    public static void equals(Object obj1, Object obj2, String message) {
        if (!java.util.Objects.equals(obj1, obj2)) {
            throw new BusinessException(message);
        }
    }
    
    public static void inRange(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw new BusinessException(message);
        }
    }
    
    public static void lengthInRange(String str, int min, int max, String message) {
        if (str == null || str.length() < min || str.length() > max) {
            throw new BusinessException(message);
        }
    }
}
```

### 5.2 使用示例

```java
// 使用 Assert 进行参数校验
public void createUser(UserDTO userDTO) {
    Assert.notNull(userDTO, "用户信息不能为空");
    Assert.notEmpty(userDTO.getUsername(), "用户名不能为空");
    Assert.lengthInRange(userDTO.getUsername(), 2, 20, "用户名长度必须在2-20之间");
    Assert.notEmpty(userDTO.getPassword(), "密码不能为空");
    Assert.isTrue(userDTO.getAge() >= 0, "年龄不能为负数");
    
    // 业务逻辑...
}

// 使用 @RateLimiter 限流
@RateLimiter(rate = 10, interval = 60, message = "登录过于频繁")
public Result<LoginVO> login(LoginDTO loginDTO) {
    // 登录逻辑...
}

// 使用 @RepeatSubmit 防重
@RepeatSubmit(interval = 5, message = "请勿重复提交订单")
public Result<OrderVO> createOrder(OrderDTO orderDTO) {
    // 创建订单逻辑...
}
```

---

## 六、深度检查补充发现（2026-01-10）

基于对 `law-firm-infra` 项目 common 层的**深度检查**，以下是新发现的重要改进点：

### 6.1 缓存服务完整体系

**参考项目实现**：`common-cache` 模块不仅有 `CacheService`，还有：

#### 6.1.1 熔断器服务 `CircuitBreakerService`

```java
/**
 * 熔断器服务接口
 * 当缓存服务连续失败达到阈值时，自动熔断，避免雪崩
 */
public interface CircuitBreakerService {
    
    /** 执行操作（带熔断保护） */
    <T> T execute(String operation, OperationSupplier<T> supplier) throws CircuitBreakerOpenException;
    
    /** 记录成功/失败 */
    void recordSuccess(String operation);
    void recordFailure(String operation);
    
    /** 检查是否允许执行 */
    boolean isAllowed(String operation);
    
    /** 获取/重置熔断器状态 */
    CircuitBreakerState getState(String operation);  // CLOSED, OPEN, HALF_OPEN
    void reset(String operation);
    
    /** 获取统计信息 */
    CircuitBreakerStatistics getStatistics(String operation);
}
```

#### 6.1.2 缓存降级服务 `CacheDegradationService`

```java
/**
 * 缓存降级服务
 * 当缓存服务不可用时，提供降级方案
 */
public interface CacheDegradationService {
    
    /** 获取缓存（带降级策略） */
    <T> T getWithFallback(String key, FallbackSupplier<T> fallbackSupplier);
    
    /** 设置缓存（带失败回调） */
    <T> boolean setWithFallback(String key, T value, FailureCallback onFailure);
    
    /** 检查缓存服务是否可用 */
    boolean isCacheAvailable();
    
    /** 获取降级统计信息（命中率、降级率等） */
    DegradationStatistics getStatistics();
}
```

#### 6.1.3 缓存预热注解 `@CacheWarmUp`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheWarmUp {
    String keyPrefix() default "";      // 缓存键前缀
    boolean useMethodName() default true;   // 是否使用方法名作为键
    boolean useParams() default true;       // 是否使用参数作为键
}
```

**工作量：** 3 天
**优先级：** 中（提升系统稳定性）

---

### 6.2 数据脱敏服务 `SensitiveDataService`

**参考项目实现**：`common-security/crypto/SensitiveDataService.java`

```java
/**
 * 数据脱敏服务接口
 * 提供通用的数据脱敏操作
 */
public interface SensitiveDataService {
    
    /** 手机号脱敏：138****1234 */
    String maskPhoneNumber(String phoneNumber);
    
    /** 身份证号脱敏：110101****0011 */
    String maskIdCard(String idCard);
    
    /** 银行卡号脱敏：****1234 */
    String maskBankCard(String bankCard);
    
    /** 邮箱脱敏：t***t@example.com */
    String maskEmail(String email);
    
    /** 姓名脱敏：张** */
    String maskName(String name);
    
    /** 地址脱敏：北京市*** */
    String maskAddress(String address);
    
    /** 案件号脱敏 */
    String maskCaseNumber(String caseNumber);
    
    /** 合同编号脱敏 */
    String maskContractNumber(String contractNumber);
    
    /** 金额脱敏 */
    String maskAmount(BigDecimal amount);
    
    /** 自定义脱敏 */
    String mask(String text, int prefixLength, int suffixLength, char maskChar);
    
    /** 批量脱敏 Map 中的指定字段 */
    Map<String, Object> maskMap(Map<String, Object> dataMap, String[] sensitiveKeys);
}
```

**应用场景**：
- 操作日志记录时脱敏敏感信息
- API 响应中脱敏客户信息
- 导出数据时脱敏处理

**工作量：** 1 天
**优先级：** 高（合规要求）

---

### 6.3 安全上下文抽象 `SecurityContext`

**当前问题**：`SecurityUtils` 直接依赖 Spring Security，测试困难。

**当前代码**（SecurityUtils.java）：
```java
public static LoginUser getLoginUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // 直接依赖 Spring Security...
}
```

**参考项目实现**：`common-security/context/`

```java
/**
 * 安全上下文接口
 */
public interface SecurityContext {
    Authentication getAuthentication();
    Authorization getAuthorization();
    Long getCurrentUserId();
    String getCurrentUsername();
    Long getCurrentTenantId();  // 多租户支持
}

/**
 * 安全上下文持有者
 */
public class SecurityContextHolder {
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();
    
    public static SecurityContext getContext() { ... }
    public static void setContext(SecurityContext context) { ... }
    public static void clearContext() { ... }
    
    /** 检查当前用户是否拥有指定权限 */
    public static boolean hasPermission(String permission) { ... }
    
    /** 获取当前用户ID */
    public static Long getCurrentUserId() { ... }
}
```

**收益**：
- 便于单元测试（可 Mock）
- 解耦 Spring Security
- 支持多种认证方式
- 支持多租户

**工作量：** 1 天
**优先级：** 中

---

### 6.4 存储策略模式

**当前问题**：硬编码 MinIO，不支持其他存储服务。

**参考项目实现**：`core-storage/strategy/`

```java
/**
 * 存储策略接口
 */
public interface StorageStrategy {
    StorageTypeEnum getStorageType();
    void initialize();
    
    boolean createBucket(String bucketName, boolean isPublic);
    boolean bucketExists(String bucketName);
    boolean removeBucket(String bucketName);
    
    boolean uploadFile(StorageBucket bucket, FileObject fileObject, InputStream inputStream);
    InputStream getObject(StorageBucket bucket, String objectName);
    boolean removeObject(StorageBucket bucket, String objectName);
    String generatePresignedUrl(StorageBucket bucket, String objectName, Integer expireSeconds);
}

/**
 * 存储上下文（策略管理器）
 */
public class StorageContext {
    private final List<StorageStrategy> strategies;
    private final Map<StorageTypeEnum, StorageStrategy> strategyCache = new ConcurrentHashMap<>();
    
    public StorageStrategy getStrategy(StorageTypeEnum storageType) { ... }
    public boolean isSupported(StorageTypeEnum storageType) { ... }
}

// 已有的策略实现
├── LocalStorageStrategy.java      // 本地存储
├── MinIOStorageStrategy.java      // MinIO
├── AliyunOssStorageStrategy.java  // 阿里云 OSS
└── TencentCosStorageStrategy.java // 腾讯云 COS
```

**收益**：
- 支持多云部署
- 便于客户定制
- 统一存储接口
- 易于扩展

**工作量：** 3 天
**优先级：** 中

---

### 6.5 消息门面模式 `MessageFacade`

**参考项目实现**：`core-message/facade/MessageFacade.java`

```java
/**
 * 消息门面类
 * 提供统一的消息发送接口
 */
public class MessageFacade {
    
    private final MessageSender messageSender;
    
    /** 发送通知 */
    public void sendNotify(BaseNotify notify, List<String> receivers, NotifyChannelEnum channel);
    
    /** 发送案件消息 */
    public void sendCaseMessage(CaseMessage message, Long caseId, List<String> receivers);
    
    /** 发送系统消息 */
    public void sendSystemMessage(SystemMessage message, Integer type, List<String> receivers);
    
    /** 获取/删除消息 */
    public BaseMessage getMessage(String messageId);
    public void deleteMessage(String messageId);
}
```

**支持的消息渠道**：
- EmailMessageChannel（邮件）
- SmsNotificationService（短信）
- WebSocketNotificationService（WebSocket 推送）
- InternalNotificationService（站内消息）

**工作量：** 2 天
**优先级：** 中

---

### 6.6 测试基础设施

**参考项目实现**：`common-test/`

```java
/**
 * 单元测试基类
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@Transactional
public abstract class BaseTest {
    protected abstract void beforeTest();
    protected abstract void afterTest();
    protected abstract void clearTestData();
}

/**
 * 集成测试基类
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected void clearDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0;");
        jdbcTemplate.query("SHOW TABLES", (rs) -> {
            while (rs.next()) {
                jdbcTemplate.execute("TRUNCATE TABLE " + rs.getString(1));
            }
            return null;
        });
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1;");
    }

    protected void setupTestData() {
        // 插入测试数据
    }
}

/**
 * 安全测试配置
 */
@TestConfiguration
public class SecurityTestConfig {
    // 测试环境安全配置
}
```

**当前项目测试文件**：仅 4 个（0.4%）
**参考项目测试文件**：78 个（3.2%）

**工作量：** 2 天
**优先级：** 高（提升代码质量）

---

### 6.7 加密服务 `EncryptionService`

**参考项目实现**：`common-security/crypto/EncryptionService.java`

```java
/**
 * 加密服务接口
 */
public interface EncryptionService {
    
    /** 加密/解密 */
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
    
    /** 密码哈希 */
    String hashPassword(String password);
    boolean matchesPassword(String rawPassword, String encodedPassword);
    
    /** 数据签名 */
    String sign(String data);
    boolean verifySignature(String data, String signature);
}
```

**工作量：** 1 天
**优先级：** 中

---

## 七、更新后的改进优先级总览

### 7.1 高优先级（P0）

| 改进项 | 工作量 | 描述 | 状态 |
|--------|--------|------|------|
| FileValidator 文件验证 | 1 天 | 防止恶意文件上传 | 待实施 |
| XssFilter 过滤器 | 0.5 天 | 防止 XSS 攻击 | 待实施 |
| MDC 链路追踪 | 0.5 天 | 日志关联，问题排查 | 待实施 |
| @RateLimiter 限流 | 1 天 | 防止接口滥用 | 待实施 |
| @RepeatSubmit 防重 | 0.5 天 | 防止重复提交 | 待实施 |
| SensitiveDataService 脱敏 | 1 天 | 合规要求 | 待实施 |
| 测试基础设施 | 2 天 | 提升代码质量 | 待实施 |

**小计：** 6.5 天

### 7.2 中优先级（P1）

| 改进项 | 工作量 | 描述 | 状态 |
|--------|--------|------|------|
| CacheService 接口 | 2 天 | 统一缓存服务 | 待实施 |
| 熔断降级服务 | 3 天 | 系统稳定性 | 待实施 |
| SecurityContext 抽象 | 1 天 | 解耦 Spring Security | 待实施 |
| 存储策略模式 | 3 天 | 多云支持 | 待实施 |
| 消息门面 | 2 天 | 统一消息发送 | 待实施 |
| EncryptionService | 1 天 | 统一加密服务 | 待实施 |

**小计：** 12 天

### 7.3 低优先级（P2）

| 改进项 | 工作量 | 描述 | 状态 |
|--------|--------|------|------|
| ResultCode 枚举 | 1 天 | 统一错误码管理 | 待实施 |
| Assert 断言工具 | 0.5 天 | 参数校验快速失败 | 待实施 |
| DateUtils 日期工具 | 0.5 天 | 统一日期处理 | 待实施 |
| OperationLog 增强 | 0.5 天 | 日志功能增强 | 待实施 |
| 权限注解增强 | 0.5 天 | AND/OR 逻辑支持 | 待实施 |

**小计：** 3 天

---

## 八、总结

通过对 `law-firm-infra` 项目 common 层的**深度分析**，我们识别了以下主要差距：

### 8.1 功能层面差距

1. **缓存体系不完善**：缺少熔断、降级、预热能力
2. **安全能力不足**：缺少数据脱敏、加密服务、安全上下文抽象
3. **存储能力单一**：硬编码 MinIO，无法支持多云
4. **消息能力分散**：邮件服务独立，无统一消息门面
5. **测试基础设施薄弱**：仅 4 个测试文件

### 8.2 架构层面差距

1. **抽象不够彻底**：SecurityUtils 直接依赖 Spring Security
2. **扩展性不足**：存储、消息等服务无策略模式
3. **可测试性差**：难以 Mock 安全上下文

### 8.3 改进建议

1. **第一阶段（1-2 周）**：实施高优先级改进，提升系统安全性
2. **第二阶段（2-3 周）**：实施中优先级改进，完善架构抽象
3. **第三阶段（长期）**：逐步优化，考虑模块化拆分

**总预估工作量**：21.5 天（约 4-5 周）

---

*文档版本：2.0*  
*更新日期：2026-01-10*  
*深度检查：common-cache、common-security、common-log、common-test、core-storage、core-message*
