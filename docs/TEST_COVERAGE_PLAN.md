# 智慧律所管理系统 - 测试覆盖实施计划

> 文档版本：1.0  
> 创建日期：2026-01-10  
> 最后更新：2026-01-10

---

## 一、概述

### 1.1 目标

建立完善的自动化测试体系，提高代码质量和系统可靠性，目标将整体测试覆盖率从当前的 **0.4%** 提升至 **60%** 以上。

### 1.2 当前状态

| 指标 | 数值 |
|------|------|
| 后端源码文件 | 1,000 个 Java 文件 |
| 现有测试文件 | 4 个 |
| 当前覆盖率 | ≈ 0.4% |
| 目标覆盖率 | ≥ 60% |

### 1.3 模块分布

| 层次 | 文件数 | 职责描述 |
|------|--------|----------|
| `application` | 435 | 应用服务层（业务逻辑编排） |
| `domain` | 272 | 领域层（实体、仓储接口、领域服务） |
| `infrastructure` | 173 | 基础设施层（持久化、外部服务集成） |
| `interfaces` | 100 | 接口层（REST Controller） |
| `common` | 19 | 公共工具类 |

### 1.4 测试技术栈

- **JUnit 5** - 单元测试框架
- **Mockito** - Mock 框架
- **AssertJ** - 断言库
- **TestContainers** - 集成测试容器（PostgreSQL）
- **jqwik** - 属性测试框架

---

## 二、测试分层策略

```
┌─────────────────────────────────────────────────────────┐
│              E2E Tests (端到端测试)                      │
│              关键业务流程，数量少，运行慢                   │
├─────────────────────────────────────────────────────────┤
│           Integration Tests (集成测试)                   │
│           Controller + Service + Database               │
│           验证组件协作，使用 TestContainers              │
├─────────────────────────────────────────────────────────┤
│              Unit Tests (单元测试)                       │
│              Service + Utils，数量多，运行快              │
│              使用 Mockito 隔离依赖                       │
└─────────────────────────────────────────────────────────┘
```

### 测试类型说明

| 类型 | 目的 | 速度 | 占比 |
|------|------|------|------|
| 单元测试 | 验证单个类/方法的逻辑正确性 | 快（毫秒级） | 70% |
| 集成测试 | 验证多个组件协作正确性 | 中（秒级） | 25% |
| 端到端测试 | 验证完整业务流程 | 慢（分钟级） | 5% |

---

## 三、实施阶段计划

### 3.1 第一阶段：核心安全模块 🔴

**优先级：最高**  
**目标覆盖率：80%+**  
**预计工时：3-4 天**  
**预计测试文件：15 个**

#### 测试范围

| 子模块 | 测试类 | 测试内容 | 类型 |
|--------|--------|----------|------|
| **认证服务** | `AuthServiceTest` | 登录验证、Token生成、刷新Token、登出 | 单元 |
| | `AuthControllerTest` | 登录接口、验证码校验、参数验证 | 集成 |
| **JWT** | `JwtTokenProviderTest` | Token生成、解析、过期处理、签名验证 | 单元 |
| **用户详情** | `UserDetailsServiceImplTest` | 用户加载、状态检查、权限加载 | 单元 |
| **验证码** | `CaptchaServiceTest` | 验证码生成、存储、校验、过期处理 | 单元 |
| **用户管理** | `UserAppServiceTest` | 用户CRUD、密码加密、状态变更 | 单元 |
| | `UserControllerTest` | 用户接口、权限校验 | 集成 |
| **角色管理** | `RoleAppServiceTest` | 角色CRUD、权限分配、角色继承 | 单元 |
| **权限缓存** | `PermissionCacheServiceTest` | 权限缓存、刷新、失效 | 单元 |
| **会话管理** | `SessionAppServiceTest` | 会话创建、查询、踢出、过期 | 单元 |
| **登录日志** | `LoginLogServiceTest` | 日志记录、失败次数统计、锁定判断 | 单元 |
| **维护模式** | `MaintenanceModeInterceptorTest` | 拦截逻辑、管理员放行 | 单元 |

#### 关键测试用例示例

```java
// AuthServiceTest 关键用例
- 正确的用户名密码应该登录成功
- 错误的密码应该登录失败并记录日志
- 连续失败5次应该锁定账户
- 被锁定的账户应该无法登录
- 过期的Token应该验证失败
- 刷新Token应该返回新的访问Token
```

---

### 3.2 第二阶段：核心业务模块 🟠

**优先级：高**  
**目标覆盖率：70%+**  
**预计工时：5-7 天**  
**预计测试文件：25 个**

#### 案件管理模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `MatterAppServiceTest` | 案件CRUD、状态流转、团队分配、数据权限 | 单元 |
| `MatterControllerTest` | 案件接口、参数验证、权限控制 | 集成 |
| `TaskAppServiceTest` | 任务创建、分配、状态变更、关联案件 | 单元 |
| `DeadlineAppServiceTest` | 期限创建、提醒触发、到期处理 | 单元 |
| `TimesheetAppServiceTest` | 工时记录、统计汇总 | 单元 |
| `ScheduleAppServiceTest` | 日程创建、冲突检测 | 单元 |

#### 客户管理模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `ClientAppServiceTest` | 客户CRUD、标签管理、关联公司 | 单元 |
| `ConflictCheckAppServiceTest` | 利益冲突检测逻辑、规则匹配 | 单元 |
| `ContactAppServiceTest` | 联系人CRUD、关联客户 | 单元 |
| `LeadAppServiceTest` | 商机管理、跟进记录、转化客户 | 单元 |
| `ClientControllerTest` | 客户接口、搜索、分页 | 集成 |

#### 文档管理模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `DocumentAppServiceTest` | 文档上传、下载、版本控制、权限 | 单元 |
| `MinioServiceTest` | 文件存储、下载、删除、URL生成 | 集成 |
| `ThumbnailServiceTest` | 缩略图生成、格式支持 | 单元 |
| `OnlyOfficeServiceTest` | 编辑URL生成、回调处理 | 单元 |
| `DocumentControllerTest` | 文档接口、上传下载 | 集成 |

#### 合同管理模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `ContractAppServiceTest` | 合同CRUD、状态流转、关联案件 | 单元 |
| `ContractTemplateAppServiceTest` | 模板CRUD、变量替换 | 单元 |
| `ContractControllerTest` | 合同接口、审批流程 | 集成 |

---

### 3.3 第三阶段：财务与人事模块 🟡

**优先级：中**  
**目标覆盖率：60%+**  
**预计工时：4-5 天**  
**预计测试文件：20 个**

#### 财务管理模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `FeeAppServiceTest` | 收费记录CRUD、关联合同 | 单元 |
| `InvoiceAppServiceTest` | 发票开具、红冲、统计 | 单元 |
| `PrepaymentAppServiceTest` | 预付款管理、抵扣 | 单元 |
| `ExpenseAppServiceTest` | 费用报销、审批、统计 | 单元 |
| `CommissionAppServiceTest` | 提成计算、分配规则 | 单元 |
| `PayrollAppServiceTest` | 工资计算、发放、历史 | 单元 |

#### 人事管理模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `EmployeeAppServiceTest` | 员工CRUD、入职离职 | 单元 |
| `AttendanceAppServiceTest` | 考勤打卡、统计、异常 | 单元 |
| `LeaveAppServiceTest` | 请假申请、审批、余额 | 单元 |
| `OvertimeAppServiceTest` | 加班申请、审批、统计 | 单元 |
| `PerformanceAppServiceTest` | 绩效考核、评分、汇总 | 单元 |
| `TrainingAppServiceTest` | 培训管理、报名 | 单元 |

---

### 3.4 第四阶段：辅助功能模块 🟢

**优先级：低**  
**目标覆盖率：50%+**  
**预计工时：3-4 天**  
**预计测试文件：15 个**

#### 系统管理模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `SysConfigAppServiceTest` | 配置CRUD、缓存刷新 | 单元 |
| `BackupAppServiceTest` | 备份创建、恢复、下载 | 单元 |
| `MigrationAppServiceTest` | 迁移脚本扫描、执行 | 单元 |
| `OperationLogAppServiceTest` | 日志记录、查询、导出 | 单元 |
| `DepartmentAppServiceTest` | 部门CRUD、树形结构 | 单元 |
| `DictAppServiceTest` | 字典管理 | 单元 |

#### 通知服务模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `EmailServiceTest` | 邮件发送、模板渲染 | 单元 |
| `AlertServiceTest` | 告警触发、内容生成 | 单元 |
| `SystemReportServiceTest` | 报告生成、定时任务 | 单元 |
| `NotificationAppServiceTest` | 站内通知 | 单元 |

#### 知识库模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `KnowledgeArticleAppServiceTest` | 知识文章CRUD | 单元 |
| `CaseLibraryAppServiceTest` | 案例库管理 | 单元 |

#### 行政管理模块

| 测试类 | 测试内容 | 类型 |
|--------|----------|------|
| `AssetAppServiceTest` | 资产管理 | 单元 |
| `MeetingRoomAppServiceTest` | 会议室预订 | 单元 |

---

## 四、测试基础设施

### 4.1 目录结构

```
backend/src/test/java/com/lawfirm/
├── base/                              # 测试基类
│   ├── BaseUnitTest.java              # 单元测试基类
│   ├── BaseIntegrationTest.java       # 集成测试基类
│   └── BaseControllerTest.java        # Controller测试基类
├── config/                            # 测试配置
│   ├── TestSecurityConfig.java        # 安全配置
│   └── TestContainersConfig.java      # TestContainers配置
├── fixtures/                          # 测试数据固件
│   ├── UserFixtures.java
│   ├── MatterFixtures.java
│   ├── ClientFixtures.java
│   ├── DocumentFixtures.java
│   └── ...
├── application/                       # 应用层测试
│   ├── system/
│   │   └── service/
│   │       ├── AuthServiceTest.java
│   │       ├── UserAppServiceTest.java
│   │       └── ...
│   ├── matter/
│   ├── client/
│   └── ...
├── infrastructure/                    # 基础设施层测试
│   ├── security/
│   ├── external/
│   └── ...
└── interfaces/                        # 接口层测试
    └── rest/
        ├── AuthControllerTest.java
        └── ...
```

### 4.2 测试基类设计

#### BaseUnitTest.java

```java
/**
 * 单元测试基类
 * - 使用 Mockito 进行依赖隔离
 * - 不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
    // 通用Mock设置和工具方法
}
```

#### BaseIntegrationTest.java

```java
/**
 * 集成测试基类
 * - 使用 TestContainers 启动 PostgreSQL
 * - 启动完整 Spring 容器
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

#### BaseControllerTest.java

```java
/**
 * Controller测试基类
 * - 使用 MockMvc 测试 REST 接口
 * - 模拟安全上下文
 */
@WebMvcTest
@AutoConfigureMockMvc
public abstract class BaseControllerTest {
    
    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected String asJsonString(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }
}
```

### 4.3 测试配置文件

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///test_db
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  
  data:
    redis:
      host: localhost
      port: 6379
  
  jpa:
    hibernate:
      ddl-auto: create-drop

# 禁用不需要的功能
minio:
  enabled: false

onlyoffice:
  enabled: false
```

---

## 五、测试规范

### 5.1 命名规范

| 类型 | 命名格式 | 示例 |
|------|----------|------|
| 测试类 | `{被测类}Test` | `AuthServiceTest` |
| 测试方法 | `should{预期行为}_when{条件}` | `shouldReturnToken_whenCredentialsValid` |
| 测试数据 | `{模块}Fixtures` | `UserFixtures` |

### 5.2 测试方法结构

```java
@Test
@DisplayName("有效凭证应该返回Token")
void shouldReturnToken_whenCredentialsValid() {
    // Given（准备）
    String username = "admin";
    String password = "password123";
    
    // When（执行）
    LoginResult result = authService.login(username, password, "127.0.0.1", "Chrome");
    
    // Then（验证）
    assertThat(result).isNotNull();
    assertThat(result.getAccessToken()).isNotEmpty();
    assertThat(result.getRefreshToken()).isNotEmpty();
}
```

### 5.3 Mock 使用规范

```java
// ✅ 推荐：明确指定Mock行为
when(userRepository.findByUsername("admin"))
    .thenReturn(Optional.of(testUser));

// ❌ 避免：过度Mock
when(userRepository.findByUsername(any()))
    .thenReturn(Optional.of(testUser));
```

---

## 六、覆盖率目标

### 6.1 分阶段目标

| 阶段 | 完成后整体覆盖率 | 核心模块覆盖率 |
|------|-----------------|---------------|
| 基础设施 | 5% | - |
| 第一阶段 | 20% | 80% |
| 第二阶段 | 40% | 75% |
| 第三阶段 | 55% | 70% |
| 第四阶段 | 65% | 65% |

### 6.2 覆盖率工具

使用 **JaCoCo** 生成覆盖率报告：

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

运行覆盖率报告：

```bash
mvn clean test jacoco:report
# 报告位置：target/site/jacoco/index.html
```

---

## 七、执行计划

### 7.1 时间线

| 周次 | 任务 | 交付物 |
|------|------|--------|
| 第1周 | 测试基础设施搭建 | 基类、配置、Fixtures |
| 第1-2周 | 第一阶段实施 | 安全模块测试 15 个 |
| 第2-3周 | 第二阶段实施 | 核心业务测试 25 个 |
| 第3-4周 | 第三阶段实施 | 财务HR测试 20 个 |
| 第4周 | 第四阶段实施 | 辅助功能测试 15 个 |

### 7.2 里程碑

| 里程碑 | 完成标准 | 预计日期 |
|--------|----------|----------|
| M1 | 测试基础设施就绪 | 第1周末 |
| M2 | 核心安全模块覆盖率 80% | 第2周末 |
| M3 | 核心业务模块覆盖率 70% | 第3周末 |
| M4 | 整体覆盖率 60% | 第4周末 |

---

## 八、持续集成

### 8.1 CI 配置建议

```yaml
# .github/workflows/test.yml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: test_db
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
      
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Tests
        run: mvn clean test
      
      - name: Generate Coverage Report
        run: mvn jacoco:report
      
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
```

### 8.2 质量门禁

| 指标 | 阈值 | 说明 |
|------|------|------|
| 测试通过率 | 100% | 所有测试必须通过 |
| 新代码覆盖率 | ≥ 80% | 新增代码的测试覆盖 |
| 整体覆盖率 | ≥ 60% | 最终目标 |

---

## 九、附录

### A. 现有测试文件

```
backend/src/test/java/com/lawfirm/
├── common/constant/
│   └── MatterConstantsTest.java
├── application/evidence/service/
│   └── EvidencePermissionPropertyTest.java
└── infrastructure/external/
    ├── document/
    │   └── EvidenceListDocumentPropertyTest.java
    └── file/
        └── FileTypeServicePropertyTest.java
```

### B. 测试依赖

```xml
<!-- 已在 pom.xml 中配置 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.2</version>
    <scope>test</scope>
</dependency>
```

---

## 十、版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0 | 2026-01-10 | AI Assistant | 初始版本 |

---

*本文档将随着测试实施进度持续更新。*

