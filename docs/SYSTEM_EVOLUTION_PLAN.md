# 系统演进规划 - 从当前走向成熟

> **文档版本**：1.0  
> **创建日期**：2026-01-10  
> **目的**：客观分析两个项目的真实差距，制定务实的演进路线

---

## 一、客观对比：真实情况

### 1.1 数据对比

| 指标 | law-firm（当前项目） | law-firm-infra（参考项目） | 分析 |
|------|---------------------|---------------------------|------|
| Java 源文件 | 1,000 个 | 2,434 个 | 参考项目代码量是我们的 2.4 倍 |
| 测试文件 | 4 个 | 72 个 | 差距最大，需重点改进 |
| Service 实现 | 86 个 | 171 个 | 业务逻辑量相当（考虑重复） |
| 模块数量 | 1 个（单体） | 50+ 个 | 模块化程度差异大 |

### 1.2 两个项目的定位差异

**law-firm-infra 是什么？**
- 一个**基础设施框架**（infra = infrastructure）
- 定位：被多个律所系统复用的底层框架
- 特点：高度抽象、策略模式、多云支持、模块化设计
- 适用：需要部署到不同环境、不同客户的产品化软件

**law-firm 是什么？**
- 一个**面向特定律所的业务系统**
- 定位：解决单一律所的具体业务问题
- 特点：业务逻辑完整、快速迭代、紧贴需求
- 适用：自用或少量客户的定制系统

### 1.3 law-firm-infra 的"好"在哪里？

| 优点 | 具体表现 | 对我们的启发 |
|------|----------|-------------|
| **通用抽象层** | CacheService、StorageStrategy、MessageFacade | 便于切换实现、提高可测试性 |
| **注解驱动** | @RateLimiter、@RepeatSubmit、@SimpleCache | 减少样板代码、声明式编程 |
| **熔断降级** | CircuitBreakerService、CacheDegradationService | 提高系统容错性 |
| **安全抽象** | SecurityContext、SensitiveDataService | 解耦框架、数据脱敏 |
| **完整测试** | 72 个测试文件 | 代码质量保障 |
| **工作流集成** | Flowable、BPMN 定义 | 复杂审批流程支持 |

### 1.4 law-firm-infra 的"过度设计"在哪里？

| 过度设计 | 具体表现 | 分析 |
|----------|----------|------|
| **模块过度拆分** | common-cache、common-util 等 9 个 common 模块 | 单一项目不需要这么多模块 |
| **策略过度抽象** | 4 种存储策略实现 | 如果只用 MinIO，不需要策略模式 |
| **消息过度复杂** | RocketMQ + RabbitMQ + WebSocket + Email | 过多渠道增加维护成本 |
| **配置过度灵活** | 每个模块大量配置属性 | 增加理解和维护成本 |

### 1.5 law-firm（当前项目）的优势

| 优势 | 具体表现 | 价值 |
|------|----------|------|
| **业务逻辑完整** | MatterAppService 1260 行，功能完善 | 真实可用 |
| **DDD 分层清晰** | domain/application/infrastructure/interfaces | 架构合理 |
| **数据权限完善** | SELF/DEPT/DEPT_AND_CHILD/ALL 四级 | 细粒度控制 |
| **通知系统完整** | 项目状态变更自动通知 | 用户体验好 |
| **审批流程简洁** | ApprovalService 简单有效 | 易理解维护 |
| **与前端契合** | 返回格式符合 vue-vben-admin | 开发效率高 |

---

## 二、真实差距分析

### 2.1 必须弥补的差距（安全/稳定性）

| 差距 | 当前实现 | 风险 | 优先级 |
|------|----------|------|--------|
| **XSS 防护** | 无 | 安全漏洞 | P0 |
| **请求追踪** | 无 TraceId | 问题排查困难 | P0 |
| **限流机制** | 硬编码 60 行 | 难复用、难测试 | P0 |
| **文件验证** | 不完整 | 恶意上传风险 | P0 |
| **测试覆盖** | 0.4% | 质量无保障 | P0 |

### 2.2 值得借鉴的设计（可选改进）

| 设计 | 当前实现 | 参考项目 | 建议 |
|------|----------|----------|------|
| 缓存抽象 | 直接 RedisTemplate | CacheService 接口 | 可考虑，便于测试 |
| 安全上下文 | SecurityUtils 静态方法 | SecurityContextHolder | 可考虑，便于测试 |
| 数据脱敏 | 无 | SensitiveDataService | 建议实现，合规需要 |
| 存储策略 | 硬编码 MinIO | StorageStrategy | 暂不需要，除非多云 |

### 2.3 不需要模仿的设计

| 设计 | 原因 |
|------|------|
| 50+ 模块拆分 | 单一项目不需要，增加复杂度 |
| 多消息中间件支持 | 邮件 + 站内通知已足够 |
| Flowable 工作流 | 现有审批服务已满足需求 |
| 多云存储策略 | 当前只用 MinIO |

---

## 三、务实的演进路线

### 3.1 第一阶段：安全加固（1-2 周）

**目标**：弥补安全和运维短板

#### 3.1.1 添加 XssFilter

```java
// 文件：infrastructure/filter/XssFilter.java
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
            chain.doFilter(new XssRequestWrapper(httpRequest), response);
        }
    }
}
```

**工作量**：0.5 天

#### 3.1.2 添加 MDC 请求追踪

```java
// 文件：infrastructure/filter/TraceIdFilter.java
public class TraceIdFilter extends OncePerRequestFilter {
    
    private static final String TRACE_ID = "traceId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) {
        try {
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            MDC.put(TRACE_ID, traceId);
            response.addHeader("X-Trace-Id", traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
```

**工作量**：0.5 天

#### 3.1.3 抽取限流注解

将 AuthService 中的 60 行限流代码抽取为可复用注解：

```java
// 文件：common/annotation/RateLimiter.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {
    String key() default "";           // 限流键，支持 SpEL
    int rate() default 10;             // 允许的请求数
    int interval() default 60;         // 时间窗口（秒）
    String message() default "请求过于频繁";
}

// 使用示例
@RateLimiter(key = "'login:ip:' + #ip", rate = 10, interval = 900)
public LoginResult login(String username, String password, String ip, String userAgent) {
    // 核心登录逻辑，不再包含限流代码
}
```

**工作量**：1 天

#### 3.1.4 添加核心测试

```java
// 文件：test/java/com/lawfirm/application/matter/MatterAppServiceTest.java
@SpringBootTest
@Transactional
class MatterAppServiceTest {
    
    @Autowired
    private MatterAppService matterAppService;
    
    @Test
    void testCreateMatter_Success() { ... }
    
    @Test
    void testCreateMatter_WithoutContract_ShouldFail() { ... }
    
    @Test
    void testChangeStatus_FromActiveToClose() { ... }
    
    @Test
    void testDataPermission_SelfScope() { ... }
}
```

**工作量**：3-5 天（核心模块测试覆盖到 20%）

### 3.2 第二阶段：代码质量（2-3 周）

**目标**：提升可维护性

#### 3.2.1 添加 Assert 工具类

```java
// 文件：common/util/Assert.java
public final class Assert {
    
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }
    
    public static void notEmpty(String str, String message) {
        if (str == null || str.isBlank()) {
            throw new BusinessException(message);
        }
    }
    
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(message);
        }
    }
}
```

**工作量**：0.5 天

#### 3.2.2 添加数据脱敏服务

```java
// 文件：common/util/SensitiveUtils.java
public class SensitiveUtils {
    
    /** 手机号脱敏：138****1234 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    /** 身份证脱敏：110101****0011 */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) return idCard;
        return idCard.substring(0, 6) + "****" + idCard.substring(idCard.length() - 4);
    }
    
    /** 姓名脱敏：张** */
    public static String maskName(String name) {
        if (name == null || name.length() == 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
```

**工作量**：0.5 天

#### 3.2.3 增强操作日志

```java
// 文件：common/annotation/OperationLog.java（增强）
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String module();
    String action();
    boolean saveParams() default true;
    boolean saveResult() default false;
    boolean async() default true;           // 新增：异步记录
    String[] excludeFields() default {"password", "token"}; // 新增：排除敏感字段
}
```

**工作量**：0.5 天

#### 3.2.4 添加防重提交注解

```java
// 文件：common/annotation/RepeatSubmit.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {
    long interval() default 3;              // 间隔时间（秒）
    String message() default "请勿重复提交";
}

// 使用示例
@RepeatSubmit(interval = 5)
@PostMapping("/create")
public Result<ContractDTO> createContract(@RequestBody CreateContractCommand cmd) {
    return Result.success(contractAppService.create(cmd));
}
```

**工作量**：0.5 天

### 3.3 第三阶段：可选优化（长期）

根据实际需求决定是否实施：

| 优化项 | 触发条件 | 工作量 |
|--------|----------|--------|
| CacheService 抽象 | 需要换缓存实现或提高测试覆盖率 | 2 天 |
| 存储策略模式 | 需要支持阿里云/腾讯云存储 | 3 天 |
| 模块化拆分 | 团队扩大、需要微服务 | 10+ 天 |
| Flowable 工作流 | 审批流程变得非常复杂 | 5+ 天 |

---

## 四、什么是"成熟系统"？

### 4.1 成熟系统的核心特征

| 特征 | 描述 | 我们的现状 | 目标 |
|------|------|-----------|------|
| **功能完整** | 满足业务需求 | ✅ 完整 | 保持 |
| **安全可靠** | 无明显漏洞 | ⚠️ 需加固 | 第一阶段完成 |
| **可追踪** | 问题可定位 | ⚠️ 缺 TraceId | 第一阶段完成 |
| **可测试** | 核心逻辑有测试 | ❌ 覆盖率 0.4% | 提升到 20% |
| **可维护** | 代码规范清晰 | ✅ 基本满足 | 保持 |
| **可扩展** | 新功能易添加 | ✅ 分层清晰 | 保持 |

### 4.2 不要追求的"伪成熟"

| 伪成熟 | 原因 |
|--------|------|
| 模块数量多 | 模块多不等于好，反而增加复杂度 |
| 策略模式多 | 不需要切换的场景，策略模式是过度设计 |
| 代码量大 | 代码量大不等于功能强大 |
| 依赖多 | 引入 Flowable、RocketMQ 等会增加运维成本 |

---

## 五、演进时间线

```
当前 ─────────── 1-2周 ─────────── 3-5周 ─────────── 长期
  │                 │                  │               │
  │   第一阶段       │    第二阶段       │   第三阶段      │
  │   安全加固       │    代码质量       │   可选优化      │
  │                 │                  │               │
  │ ✓ XssFilter     │ ✓ Assert        │ ? CacheService │
  │ ✓ TraceId       │ ✓ 脱敏服务       │ ? 存储策略     │
  │ ✓ @RateLimiter  │ ✓ 日志增强       │ ? 模块拆分     │
  │ ✓ 核心测试       │ ✓ @RepeatSubmit │ ? 工作流       │
  │                 │                  │               │
  └─────────────────┴──────────────────┴───────────────┘
      工作量：1-2周       工作量：1周         按需决定
```

---

## 六、总结

### 6.1 核心结论

1. **law-firm-infra 是框架**，我们是业务系统，定位不同，不需要完全模仿
2. **必须弥补的差距**：XSS 防护、请求追踪、限流注解、测试覆盖
3. **值得借鉴的设计**：脱敏服务、Assert 工具、日志增强
4. **不需要模仿的设计**：50 模块拆分、多云存储策略、复杂消息中间件

### 6.2 行动建议

1. **立即执行**（1-2 周）：完成安全加固四项
2. **短期执行**（1 周）：完成代码质量四项
3. **按需决定**：CacheService、存储策略等根据实际需求决定

### 6.3 成熟度目标

| 阶段 | 成熟度 | 标志 |
|------|--------|------|
| 当前 | 60% | 功能完整但安全/测试不足 |
| 第一阶段后 | 75% | 安全加固完成，核心测试覆盖 |
| 第二阶段后 | 85% | 代码质量提升，可维护性增强 |
| 第三阶段后 | 90%+ | 根据需求有针对性优化 |

---

## 七、我们没想到的实用功能（需补充）

经过深入检查参考项目，发现以下实用功能是我们当前系统没有的：

### 7.1 🔴 安全相关（高优先级）

#### 7.1.1 文件上传验证（FileValidator）

**我们的问题**：上传文件只做了简单检查，可能被恶意文件绕过。

**参考项目的实现**：
- ✅ 白名单验证（允许的扩展名）
- ✅ 黑名单验证（禁止的可执行文件）
- ✅ MIME 类型验证
- ✅ 文件魔数签名验证（防止伪装文件）

```java
// 需要补充的文件验证工具
// 位置：common/util/FileValidator.java
public class FileValidator {
    
    // 禁止的可执行文件扩展名
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
        "exe", "dll", "so", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar"
    );
    
    // 文件魔数签名映射
    private static final Map<String, byte[]> FILE_SIGNATURES = Map.of(
        "pdf", new byte[]{0x25, 0x50, 0x44, 0x46},     // %PDF
        "png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47}, // PNG
        "zip", new byte[]{0x50, 0x4B, 0x03, 0x04}      // PK..
    );
    
    public static ValidationResult validate(MultipartFile file) {
        // 1. 检查禁止的扩展名
        // 2. 验证 MIME 类型
        // 3. 验证文件魔数签名，防止伪装
    }
}
```

**工作量**：0.5 天

#### 7.1.2 IP 工具增强（IpUtils）

**我们的问题**：获取真实 IP 不完整，可能被代理头欺骗。

**参考项目的实现**：
- ✅ 检查多个代理 Header（X-Forwarded-For、Proxy-Client-IP 等）
- ✅ 处理多级代理的逗号分隔
- ✅ 判断内网/公网 IP
- ✅ 集成 ip2region 获取 IP 地理位置

```java
// 需要补充的 IP 工具
// 位置：common/util/IpUtils.java
public class IpUtils {
    
    private static final String[] HEADERS = {
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_CLIENT_IP"
    };
    
    public static String getIpAddr(HttpServletRequest request) {
        // 依次检查多个代理头
    }
    
    public static boolean isInternalIP(String ip) {
        // 判断 10.x、172.16-31.x、192.168.x
    }
    
    public static String getRealAddressByIP(String ip) {
        // 使用 ip2region 获取地理位置
    }
}
```

**工作量**：0.5 天

### 7.2 🟠 审计日志相关（中优先级）

#### 7.2.1 设备指纹（DeviceFingerprintUtils）

**我们的问题**：登录日志只记录 IP，无法识别设备。

**参考项目的实现**：
- ✅ 生成设备指纹（User-Agent + Accept-Language + 时区等）
- ✅ 判断设备类型（MOBILE/TABLET/DESKTOP）
- ✅ 提取浏览器信息（Chrome/Firefox/Safari）
- ✅ 提取操作系统信息（Windows/macOS/Linux）

```java
// 需要补充的设备指纹工具
// 位置：common/util/DeviceFingerprintUtils.java
public class DeviceFingerprintUtils {
    
    public static String generateFingerprint(HttpServletRequest request) {
        // 组合 User-Agent、Accept-Language、时区等生成 MD5
    }
    
    public static String determineDeviceType(HttpServletRequest request) {
        // 返回 MOBILE, TABLET, DESKTOP
    }
    
    public static String extractBrowser(HttpServletRequest request) {
        // 返回 Chrome, Firefox, Safari 等
    }
    
    public static String extractOS(HttpServletRequest request) {
        // 返回 Windows, macOS, Linux, Android, iOS
    }
}
```

**工作量**：0.5 天

#### 7.2.2 字段变更审计（FieldChangeUtils）

**我们的问题**：更新操作只记录"修改了某条记录"，不知道改了什么。

**参考项目的实现**：
- ✅ 比较两个对象指定字段的变更
- ✅ 返回变更前后的值
- ✅ 支持 @AuditIgnore 注解排除敏感字段

```java
// 需要补充的字段变更工具
// 位置：common/util/FieldChangeUtils.java
public class FieldChangeUtils {
    
    /**
     * 比较字段变更
     * @return Map<字段名, [旧值, 新值]>
     */
    public static Map<String, Object[]> compareFields(Object before, Object after, String... fields) {
        // 反射对比字段值
    }
}

// 使用示例
Map<String, Object[]> changes = FieldChangeUtils.compareFields(
    oldMatter, newMatter, "name", "status", "leadLawyerId"
);
// 返回：{"status": ["ACTIVE", "CLOSED"], "leadLawyerId": [1, 2]}
```

**工作量**：0.5 天

#### 7.2.3 字段级审计注解（@AuditField）

**我们的问题**：没有字段级别的审计追踪。

```java
// 需要补充的字段审计注解
// 位置：common/annotation/AuditField.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditField {
    String[] fields();              // 需要审计的字段
    boolean async() default true;   // 是否异步
    String description() default "";
}

// 使用示例
@AuditField(fields = {"status", "leadLawyerId", "actualFee"})
public void updateMatter(UpdateMatterCommand cmd) { ... }
```

**工作量**：1 天

### 7.3 🟡 容错与稳定性（中优先级）

#### 7.3.1 熔断器（CircuitBreaker）

**我们的问题**：Redis 挂了整个系统就挂了，没有降级机制。

**参考项目的实现**：
- ✅ 三种状态：CLOSED（正常）→ OPEN（熔断）→ HALF_OPEN（试探）
- ✅ 失败次数达到阈值自动熔断
- ✅ 熔断超时后自动半开试探
- ✅ 半开成功次数达到阈值自动恢复

```java
// 需要补充的熔断器
// 位置：common/resilience/CircuitBreaker.java
public class CircuitBreaker {
    
    private static final int FAILURE_THRESHOLD = 5;  // 失败阈值
    private static final long TIMEOUT_MS = 60000;    // 熔断 60 秒
    
    public <T> T execute(String operation, Supplier<T> supplier) {
        if (!isAllowed()) {
            throw new CircuitBreakerOpenException();
        }
        try {
            T result = supplier.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }
}
```

**工作量**：1 天

#### 7.3.2 缓存预热（@CacheWarmUp）

**我们的问题**：系统重启后缓存冷启动，前几个请求慢。

```java
// 需要补充的缓存预热注解
// 位置：common/annotation/CacheWarmUp.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheWarmUp {
    String keyPrefix() default "";
}

// 使用示例 - 系统启动时自动加载热点数据
@CacheWarmUp(keyPrefix = "dict:")
public List<DictItem> loadAllDictItems() { ... }
```

**工作量**：0.5 天

### 7.4 🟢 实用工具类（低优先级但有用）

#### 7.4.1 图片处理（ImageUtils）

**我们的问题**：上传图片需要缩略图、水印，目前手工处理。

**参考项目的实现**：
- ✅ 图片缩放（指定尺寸或比例）
- ✅ 图片压缩（调整质量）
- ✅ 图片裁剪
- ✅ 添加文字水印
- ✅ 添加图片水印
- ✅ 格式转换

```java
// 需要补充的图片工具
// 位置：common/util/ImageUtils.java
public class ImageUtils {
    public static void scale(File src, File dest, int width, int height);
    public static void compress(File src, File dest, float quality);
    public static void addWatermark(File src, File dest, String text, Color color);
}
```

**工作量**：0.5 天（使用 Thumbnailator 库）

#### 7.4.2 压缩工具（CompressUtils）

**我们的问题**：批量下载文件需要打包，目前没有统一工具。

```java
// 需要补充的压缩工具
// 位置：common/util/CompressUtils.java
public class CompressUtils {
    public static void zip(String srcPath, String destPath);
    public static File unzip(String zipPath, String destPath);
    public static byte[] gzip(String content);
}
```

**工作量**：0.25 天（使用 Hutool ZipUtil）

#### 7.4.3 数据库备份工具（DatabaseBackupUtil）

**我们的问题**：备份用 shell 脚本，Java 程序无法控制。

```java
// 需要补充的数据库备份工具
// 位置：common/util/DatabaseBackupUtil.java
public class DatabaseBackupUtil {
    public String backup(String host, String port, String user, String pwd, 
                        String database, String backupPath) {
        // 调用 mysqldump 或 pg_dump
    }
}
```

**工作量**：0.5 天

#### 7.4.4 数字签名工具（SignatureUtils）

**我们的问题**：合同签名、电子印章需要数字签名，目前没有。

```java
// 需要补充的数字签名工具
// 位置：common/util/crypto/SignatureUtils.java
public class SignatureUtils {
    public static byte[] sign(byte[] data, PrivateKey privateKey);
    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey);
}
```

**工作量**：0.5 天

---

## 八、补充功能优先级排序

| 优先级 | 功能 | 理由 | 工作量 |
|--------|------|------|--------|
| **P0** | FileValidator（文件验证） | 安全漏洞风险 | 0.5 天 |
| **P0** | IpUtils 增强 | 防止 IP 欺骗 | 0.5 天 |
| **P1** | DeviceFingerprintUtils | 登录安全审计 | 0.5 天 |
| **P1** | FieldChangeUtils | 审计日志完整性 | 0.5 天 |
| **P1** | CircuitBreaker | 系统稳定性 | 1 天 |
| **P2** | @AuditField 注解 | 精细化审计 | 1 天 |
| **P2** | @CacheWarmUp | 启动性能 | 0.5 天 |
| **P3** | ImageUtils | 图片处理 | 0.5 天 |
| **P3** | CompressUtils | 批量下载 | 0.25 天 |
| **P3** | DatabaseBackupUtil | 程序控制备份 | 0.5 天 |
| **P3** | SignatureUtils | 电子签名 | 0.5 天 |

**总工作量**：约 6 天

---

## 九、立即行动清单

### 本周必须完成（P0，安全相关）

1. **FileValidator** - 防止恶意文件上传
2. **IpUtils 增强** - 正确获取真实 IP
3. **XssFilter** - 防止 XSS 攻击
4. **TraceIdFilter** - 请求追踪

### 下周完成（P1，审计与稳定性）

5. **DeviceFingerprintUtils** - 设备指纹
6. **FieldChangeUtils** - 字段变更记录
7. **CircuitBreaker** - 熔断降级

### 后续完成（P2-P3，锦上添花）

8. @AuditField 注解
9. @CacheWarmUp 注解
10. ImageUtils、CompressUtils 等

---

*真正的成熟不是代码量的堆砌，而是在满足业务需求的前提下，保持代码简洁、安全可靠、易于维护。*

