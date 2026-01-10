# 工具类使用指南

> **创建日期**：2026-01-10  
> **目的**：确保团队正确使用已实现的工具类，避免遗忘

---

## 📋 工具清单速查

| 工具 | 用途 | 使用场景 | 是否必须使用 |
|------|------|----------|-------------|
| `FileValidator` | 文件上传验证 | 所有文件上传接口 | ✅ 必须 |
| `IpUtils` | 获取真实IP | 登录、操作日志 | ✅ 必须 |
| `XssFilter` | XSS过滤 | 自动生效（Filter） | ✅ 自动 |
| `TraceIdFilter` | 请求追踪 | 自动生效（Filter） | ✅ 自动 |
| `DeviceFingerprintUtils` | 设备识别 | 登录日志 | 🟡 推荐 |
| `FieldChangeUtils` | 字段变更对比 | 审计日志 | 🟡 推荐 |
| `Assert` | 参数校验 | Service层入参 | 🟡 推荐 |
| `SensitiveUtils` | 数据脱敏 | 日志输出、API返回 | ✅ 必须 |
| `@RateLimiter` | 接口限流 | 敏感接口 | 🟡 推荐 |
| `@RepeatSubmit` | 防重复提交 | 表单提交接口 | 🟡 推荐 |
| `CompressUtils` | 文件压缩 | 批量下载打包 | 🟡 推荐 |

---

## 1. FileValidator - 文件上传验证

### 为什么要用？
防止恶意文件上传，包括：
- 可执行文件（.exe, .sh, .php）
- 伪装文件（把 .exe 改成 .jpg）
- 超大文件攻击

### 在哪里使用？
**所有文件上传接口**都必须调用！

### 使用示例

```java
import com.lawfirm.common.util.FileValidator;
import com.lawfirm.common.util.FileValidator.ValidationResult;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @PostMapping("/upload")
    public Result<FileDTO> upload(@RequestParam MultipartFile file) {
        // ⚠️ 必须：在处理文件前先验证
        ValidationResult result = FileValidator.validate(file);
        if (!result.isValid()) {
            throw new BusinessException(result.getErrorMessage());
        }
        
        // 验证通过，继续处理上传...
        return fileService.upload(file);
    }
    
    @PostMapping("/batch-upload")
    public Result<List<FileDTO>> batchUpload(@RequestParam List<MultipartFile> files) {
        // 批量上传时，每个文件都要验证
        for (MultipartFile file : files) {
            ValidationResult result = FileValidator.validate(file);
            if (!result.isValid()) {
                throw new BusinessException("文件 " + file.getOriginalFilename() 
                    + " 验证失败: " + result.getErrorMessage());
            }
        }
        
        return fileService.batchUpload(files);
    }
}
```

### 需要改造的接口清单

| 模块 | 接口 | 文件 | 状态 |
|------|------|------|------|
| 合同 | 合同附件上传 | `ContractController` | ⏳ 待改造 |
| 项目 | 项目文档上传 | `MatterController` | ⏳ 待改造 |
| 用户 | 头像上传 | `UserController` | ⏳ 待改造 |
| 系统 | 通用文件上传 | `FileController` | ⏳ 待改造 |

---

## 2. IpUtils - IP地址工具

### 为什么要用？
- `request.getRemoteAddr()` 在有代理时获取的是代理IP
- 需要正确获取用户真实IP用于：登录日志、操作审计、限流

### 使用示例

```java
import com.lawfirm.common.util.IpUtils;

@Service
public class LoginService {

    public LoginResult login(LoginCommand cmd, HttpServletRequest request) {
        // ✅ 正确获取真实IP
        String realIp = IpUtils.getIpAddr(request);
        
        // 记录登录日志
        LoginLog log = new LoginLog();
        log.setIp(realIp);
        log.setLocation(IpUtils.getRealAddressByIP(realIp));  // 获取IP地理位置
        log.setIsInternal(IpUtils.isInternalIP(realIp));      // 判断是否内网
        
        // ... 登录逻辑
    }
}
```

### 需要改造的地方

| 位置 | 当前实现 | 改造方式 |
|------|----------|----------|
| 登录日志 | `request.getRemoteAddr()` | 改为 `IpUtils.getIpAddr(request)` |
| 操作日志切面 | 同上 | 同上 |
| 限流服务 | 同上 | 同上 |

---

## 3. XssFilter - XSS过滤器

### 为什么要用？
防止跨站脚本攻击（XSS），自动过滤请求中的危险脚本。

### 如何生效？
需要注册为 Spring Bean，**自动生效**，无需手动调用。

### 配置方式

```java
import com.lawfirm.infrastructure.filter.XssFilter;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(1);
        return registration;
    }
}
```

### 排除路径
以下路径自动排除（不进行XSS过滤）：
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/doc.html`
- `/actuator/**`

---

## 4. TraceIdFilter - 请求追踪

### 为什么要用？
- 每个请求生成唯一的 traceId
- 方便在日志中追踪整个请求链路
- 出问题时可以用 traceId 搜索所有相关日志

### 如何生效？
注册为 Spring Bean 后**自动生效**。

### 配置方式

```java
import com.lawfirm.infrastructure.filter.TraceIdFilter;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        registration.setOrder(0);  // 最高优先级
        return registration;
    }
}
```

### 配置 logback 输出 traceId

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- 添加 %X{traceId} -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

### 在异步任务中使用

```java
// 主线程
String traceId = TraceIdFilter.getTraceId();

// 异步任务中
CompletableFuture.runAsync(() -> {
    TraceIdFilter.setTraceId(traceId);  // 传递 traceId
    try {
        // 执行任务
    } finally {
        TraceIdFilter.clearTraceId();
    }
});
```

---

## 5. DeviceFingerprintUtils - 设备指纹

### 为什么要用？
- 识别用户设备类型（手机/电脑/平板）
- 识别浏览器和操作系统
- 生成设备指纹用于安全审计（异地登录检测）

### 使用示例

```java
import com.lawfirm.common.util.DeviceFingerprintUtils;
import com.lawfirm.common.util.DeviceFingerprintUtils.DeviceInfo;
import com.lawfirm.common.util.DeviceFingerprintUtils.DeviceType;

@Service
public class LoginLogService {

    public void recordLoginLog(HttpServletRequest request, Long userId) {
        // 解析设备信息
        DeviceInfo device = DeviceFingerprintUtils.parseDeviceInfo(request);
        
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setDeviceType(device.getDeviceType().getDescription());  // "桌面端"/"移动端"/"平板"
        log.setBrowser(device.getFullBrowser());   // "Chrome 120.0.6099"
        log.setOs(device.getFullOS());             // "Windows 10/11"
        log.setFingerprint(DeviceFingerprintUtils.generateFingerprint(request));  // 32位MD5
        
        loginLogRepository.save(log);
    }
    
    // 检测异常登录（设备指纹变化）
    public boolean isAbnormalLogin(Long userId, HttpServletRequest request) {
        String currentFingerprint = DeviceFingerprintUtils.generateFingerprint(request);
        String lastFingerprint = getLastLoginFingerprint(userId);
        
        return lastFingerprint != null && !lastFingerprint.equals(currentFingerprint);
    }
}
```

---

## 6. FieldChangeUtils - 字段变更对比

### 为什么要用？
- 记录"改了什么"，而不只是"改了"
- 审计日志更有价值

### 使用示例

```java
import com.lawfirm.common.util.FieldChangeUtils;
import com.lawfirm.common.util.FieldChangeUtils.FieldChange;

@Service
public class MatterService {

    public void updateMatter(UpdateMatterCommand cmd) {
        // 1. 查询原数据
        Matter oldMatter = matterRepository.findById(cmd.getId());
        
        // 2. 执行更新
        Matter newMatter = updateFields(oldMatter, cmd);
        matterRepository.save(newMatter);
        
        // 3. 记录变更
        List<FieldChange> changes = FieldChangeUtils.compare(oldMatter, newMatter);
        if (!changes.isEmpty()) {
            String changeLog = FieldChangeUtils.formatChanges(changes);
            // 输出: "status: [ACTIVE] -> [CLOSED]; leadLawyerId: [1] -> [2]"
            auditLogService.log("更新项目", cmd.getId(), changeLog);
        }
    }
}
```

### 只对比指定字段

```java
// 只对比关心的字段
Set<String> fields = Set.of("status", "leadLawyerId", "actualFee");
List<FieldChange> changes = FieldChangeUtils.compare(oldMatter, newMatter, fields);
```

---

## 7. Assert - 参数校验

### 为什么要用？
- 代码更简洁
- 统一的校验失败处理（抛出 BusinessException）

### 使用示例

```java
import com.lawfirm.common.util.Assert;

@Service
public class ContractService {

    public ContractDTO createContract(CreateContractCommand cmd) {
        // ⚠️ 入参校验
        Assert.notNull(cmd, "请求参数不能为空");
        Assert.notBlank(cmd.getContractNo(), "合同编号不能为空");
        Assert.notBlank(cmd.getTitle(), "合同标题不能为空");
        Assert.notNull(cmd.getClientId(), "客户ID不能为空");
        Assert.greaterThan(cmd.getAmount(), BigDecimal.ZERO, "合同金额必须大于0");
        Assert.maxLength(cmd.getTitle(), 200, "合同标题不能超过200字");
        
        // 校验通过，执行业务逻辑...
    }
}
```

### 可用的校验方法

| 方法 | 说明 |
|------|------|
| `notNull(obj, msg)` | 对象不能为null |
| `notBlank(str, msg)` | 字符串不能为空或空白 |
| `notEmpty(collection, msg)` | 集合不能为空 |
| `isTrue(expr, msg)` | 表达式必须为true |
| `isFalse(expr, msg)` | 表达式必须为false |
| `greaterThan(num, min, msg)` | 数值必须大于 |
| `lessThan(num, max, msg)` | 数值必须小于 |
| `inRange(num, min, max, msg)` | 数值在范围内 |
| `maxLength(str, max, msg)` | 字符串最大长度 |
| `matches(str, regex, msg)` | 字符串匹配正则 |

---

## 8. SensitiveUtils - 数据脱敏

### 为什么要用？
- 日志中不能输出明文手机号、身份证
- API 返回时部分信息需要脱敏

### 使用示例

```java
import com.lawfirm.common.util.SensitiveUtils;

@Service
public class UserService {

    public void logUserAction(User user, String action) {
        // ✅ 日志输出时脱敏
        log.info("用户操作: {}, 姓名: {}, 手机: {}, 身份证: {}", 
            action,
            SensitiveUtils.maskName(user.getName()),      // 张**
            SensitiveUtils.maskPhone(user.getPhone()),    // 138****1234
            SensitiveUtils.maskIdCard(user.getIdCard())); // 110101****0011
    }
}

@RestController
public class ClientController {

    @GetMapping("/clients/{id}")
    public Result<ClientVO> getClient(@PathVariable Long id) {
        Client client = clientService.getById(id);
        
        ClientVO vo = new ClientVO();
        vo.setName(client.getName());
        // ✅ API返回时脱敏
        vo.setPhone(SensitiveUtils.maskPhone(client.getPhone()));
        vo.setEmail(SensitiveUtils.maskEmail(client.getEmail()));  // t***@example.com
        
        return Result.success(vo);
    }
}
```

### 可用的脱敏方法

| 方法 | 示例输入 | 示例输出 |
|------|----------|----------|
| `maskPhone` | 13812345678 | 138****5678 |
| `maskIdCard` | 110101199001011234 | 110101****1234 |
| `maskName` | 张三 | 张* |
| `maskEmail` | test@example.com | t***@example.com |
| `maskBankCard` | 6222021234567890123 | 6222****0123 |
| `maskAddress` | 北京市朝阳区xxx街道 | 北京市朝阳区*** |
| `mask(str, front, end)` | 自定义前后保留位数 | - |

---

## 9. @RateLimiter - 接口限流

### 为什么要用？
- 防止接口被恶意刷
- 保护系统资源

### 使用示例

```java
import com.lawfirm.common.annotation.RateLimiter;
import com.lawfirm.common.annotation.RateLimiter.LimitType;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 登录接口：每个IP每15分钟最多10次
    @RateLimiter(
        key = "'login:ip:' + #ip",
        rate = 10,
        rateInterval = 15,
        rateIntervalUnit = TimeUnit.MINUTES,
        message = "登录尝试过于频繁，请15分钟后再试"
    )
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginCommand cmd, 
                                     @RequestParam String ip) {
        return authService.login(cmd);
    }
    
    // 发送验证码：每个手机号每分钟最多1次
    @RateLimiter(
        key = "'sms:' + #phone",
        rate = 1,
        rateInterval = 1,
        rateIntervalUnit = TimeUnit.MINUTES,
        message = "验证码发送过于频繁"
    )
    @PostMapping("/send-sms")
    public Result<Void> sendSms(@RequestParam String phone) {
        return smsService.send(phone);
    }
    
    // 按用户限流
    @RateLimiter(
        limitType = LimitType.USER,
        rate = 100,
        rateInterval = 1,
        rateIntervalUnit = TimeUnit.HOURS
    )
    @PostMapping("/export")
    public Result<String> exportData() {
        return exportService.export();
    }
}
```

### 参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `key` | 限流键（支持SpEL） | 方法签名 |
| `rate` | 时间窗口内最大请求数 | 10 |
| `rateInterval` | 时间窗口 | 60 |
| `rateIntervalUnit` | 时间单位 | SECONDS |
| `limitType` | 限流类型（DEFAULT/IP/USER） | DEFAULT |
| `message` | 超限提示消息 | 请求过于频繁 |

---

## 10. @RepeatSubmit - 防重复提交

### 为什么要用？
- 防止用户快速点击多次
- 防止网络抖动导致重复请求

### 使用示例

```java
import com.lawfirm.common.annotation.RepeatSubmit;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    // 创建合同：5秒内不允许重复提交
    @RepeatSubmit(interval = 5, message = "请勿重复创建合同")
    @PostMapping
    public Result<ContractDTO> create(@RequestBody CreateContractCommand cmd) {
        return contractService.create(cmd);
    }
    
    // 付款：10秒内不允许重复提交（金融操作更严格）
    @RepeatSubmit(interval = 10, timeUnit = TimeUnit.SECONDS, message = "支付处理中，请勿重复操作")
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id, @RequestBody PayCommand cmd) {
        return paymentService.pay(id, cmd);
    }
}
```

### 需要添加的接口

| 模块 | 接口 | 建议间隔 |
|------|------|----------|
| 客户 | 创建客户 | 3秒 |
| 合同 | 创建合同 | 5秒 |
| 项目 | 创建项目 | 5秒 |
| 财务 | 收款/付款 | 10秒 |
| 审批 | 提交审批 | 3秒 |

---

## 🔧 集成检查清单

### Filter 配置（一次性）

- [ ] 创建 `FilterConfig.java` 配置类
- [ ] 注册 `XssFilter`
- [ ] 注册 `TraceIdFilter`
- [ ] 更新 `logback-spring.xml` 添加 traceId

### 登录模块改造

- [ ] 使用 `IpUtils.getIpAddr()` 获取真实IP
- [ ] 使用 `DeviceFingerprintUtils` 记录设备信息
- [ ] 使用 `@RateLimiter` 替换硬编码限流

### 文件上传改造

- [ ] 合同附件上传添加 `FileValidator.validate()`
- [ ] 项目文档上传添加 `FileValidator.validate()`
- [ ] 头像上传添加 `FileValidator.validate()`

### 敏感数据处理

- [ ] 日志输出使用 `SensitiveUtils` 脱敏
- [ ] API 返回敏感字段使用脱敏

### 表单提交接口

- [ ] 创建类接口添加 `@RepeatSubmit`
- [ ] 支付类接口添加 `@RepeatSubmit`

---

## ❓ 常见问题

### Q: Filter 没生效怎么办？
A: 检查是否创建了 `FilterConfig` 配置类并注册了 Filter

### Q: @RateLimiter 没生效怎么办？
A: 检查是否启用了 AOP（`@EnableAspectJAutoProxy`），以及 Redis 是否正常

### Q: 如何测试这些工具？
A: 
```java
// 单元测试示例
@Test
void testFileValidator() {
    MockMultipartFile file = new MockMultipartFile(
        "test.exe", "test.exe", "application/octet-stream", new byte[10]
    );
    ValidationResult result = FileValidator.validate(file);
    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("禁止"));
}
```

---

## 11. CompressUtils - 压缩工具

### 为什么要用？
提供统一的文件压缩/解压功能：
- 批量下载文件打包成 ZIP
- 大文件 GZIP 压缩传输
- 安全解压（防止 Zip Slip 漏洞）

### 使用场景
- 批量导出文档打包下载
- 案件材料批量下载
- 备份文件压缩

### 使用示例

```java
import com.lawfirm.common.util.CompressUtils;

// 1. 批量文件打包下载（内存方式）
@GetMapping("/batch-download")
public void batchDownload(@RequestParam List<Long> ids, HttpServletResponse response) throws IOException {
    Map<String, byte[]> files = new HashMap<>();
    for (Long id : ids) {
        Document doc = documentService.getById(id);
        byte[] content = storageService.download(doc.getPath());
        files.put(doc.getFileName(), content);
    }
    
    byte[] zipData = CompressUtils.zipDataToBytes(files);
    
    response.setContentType("application/zip");
    response.setHeader("Content-Disposition", "attachment; filename=documents.zip");
    response.getOutputStream().write(zipData);
}

// 2. 流式压缩（适合大文件）
@GetMapping("/export-large")
public void exportLarge(HttpServletResponse response) throws IOException {
    Map<String, InputStream> streams = new HashMap<>();
    streams.put("file1.pdf", storageService.getInputStream("path1"));
    streams.put("file2.pdf", storageService.getInputStream("path2"));
    
    response.setContentType("application/zip");
    response.setHeader("Content-Disposition", "attachment; filename=export.zip");
    CompressUtils.zipStreams(streams, response.getOutputStream());
}

// 3. 压缩目录
File sourceDir = new File("/data/backup/2026-01-10");
File zipFile = new File("/data/archive/backup-2026-01-10.zip");
CompressUtils.zipDirectory(sourceDir, zipFile);

// 4. 解压 ZIP 文件
File zipFile = new File("/data/upload/import.zip");
File targetDir = new File("/data/temp/import");
CompressUtils.unzip(zipFile, targetDir);

// 5. GZIP 压缩（适合文本数据）
String jsonData = objectMapper.writeValueAsString(largeObject);
byte[] compressed = CompressUtils.gzip(jsonData);
// 解压
String original = CompressUtils.ungzipToString(compressed);

// 6. 检查是否为 ZIP 文件
if (CompressUtils.isZipFile(uploadedFile)) {
    // 处理 ZIP 文件
}

// 7. 列出 ZIP 内容
List<String> entries = CompressUtils.listZipEntries(zipFile);
```

### API 速查

| 方法 | 用途 | 说明 |
|------|------|------|
| `zipFile(source, zip)` | 压缩单个文件 | - |
| `zipFiles(files, zip)` | 压缩多个文件 | - |
| `zipDirectory(dir, zip)` | 压缩目录 | 保留目录结构 |
| `zipData(map, zip)` | 压缩内存数据到文件 | Map<文件名, 字节数组> |
| `zipDataToBytes(map)` | 压缩内存数据到字节数组 | 适合直接返回下载 |
| `zipStreams(streams, out)` | 流式压缩 | 适合大文件 |
| `unzip(zip, dir)` | 解压到目录 | 防 Zip Slip |
| `gzip(data)` | GZIP 压缩 | 适合文本 |
| `ungzip(data)` | GZIP 解压 | - |
| `isZipFile(file)` | 检查是否为 ZIP | 检查魔数 |
| `listZipEntries(zip)` | 列出 ZIP 内容 | - |

---

*最后更新：2026-01-10*

