# 智慧律所管理系统 - 后端代码实装深度指南 (Detailed Backend Implementation Guide)

> 本文档旨在为新加入的工程师提供“显微镜级”的代码图谱。这里不讲宏大叙事，只讲类名、目录与真实的调用链。

---

## 1. 架构实装全景 (Architectural Layers)

系统通过 **SpringBoot 3.2** 构建，严格遵循 DDD 四层模型，并结合 **MyBatis-Plus** 简化持久层。

### 1.1 接口层 (Interfaces) - `com.lawfirm.interfaces`
*   **REST 控制器**: 位于 `rest` 包下，负责参数校验与响应封装。
    *   **身份验证**: `AuthController.java` (核心：`login()` 方法)。
    *   **业务示例**: `finance.FinanceCommissionController` (提成计算触发)。
*   **异常拦截**: `infrastructure.config.GlobalExceptionHandler` 负责将所有异常统一为 `Result<T>` 格式。

### 1.2 应用层 (Application) - `com.lawfirm.application`
*   **事务编排**: 在此层通过 `@Transactional` 锁定业务原子性。
*   **核心实装**:
    *   `finance.service.CommissionCalculationService`: 实现了**三层提成模型**与**比例归一化算法**。
    *   `finance.service.PaymentReconciliationService`: 实现了 **OCR 自动核销加权分值模型**。
*   **空壳风险**: `report` 包下目前仅有框架代码，复杂的分析 SQL 暂存于 `infrastructure` 的 Mapper 中。

### 1.3 领域层 (Domain) - `com.lawfirm.domain`
*   **实体 (Entity)**: 继承自 `common.base.BaseEntity`，自动享有 `id/createdAt/updatedAt/deleted` 字段。
*   **Repository 接口**: 定义领域级持久化契约，实现在 `infrastructure` 层。

### 1.4 基础设施层 (Infrastructure) - `com.lawfirm.infrastructure`
*   **持久层 (Persistence)**: 
    *   全系统含 **123 个 Mapper**，遵循 `BaseMapper` 规范。
    *   **巨型 SQL**: `StatisticsMapper.java` (30kb+) 承载了全系统大部分的数据统计逻辑，为未来报表模块的动力源。
*   **外部服务**:
    *   **MinIO**: `external.minio.MinioService` (实现文件版本与哈希去重)。
    *   **OCR**: `ocr.PaddleOcrService` (本地化深度学习 OCR 对接)。

---

## 2. 横切关注点实装 (Common Patterns)

### 2.1 安全鉴权链 (Security Chain)
*   **核心类**: `infrastructure.security.SecurityConfig`。
*   **机制**: **Stateless JWT**。
    1.  `JwtAuthenticationFilter` 拦截 header 中的 `Authorization`。
    2.  `UserDetailsServiceImpl` 缓存权限树。
    3.  通过注解 `@PreAuthorize("hasAuthority('xxx')")` 保护方法。

### 2.2 数据审计与逻辑删除
*   **基类**: `com.lawfirm.common.base.BaseEntity`。
*   **自动填充**: 使用 MyBatis-Plus `MetaObjectHandler` 自动注入 `createdBy` 和 `updatedAt`。
*   **逻辑删除**: 全部表具备 `deleted` 字段，执行 `deleteById` 时仅做软删除。

### 2.3 异常处理规范
*   业务逻辑中严禁捕获异常后返回错误串。
*   **标准做法**: `throw new BusinessException("错误描述")`。

---

## 3. 开发者执行蓝图 (Dev Workflow)

若要新增一个业务功能，请遵循以下实装路径：
1.  **DB**: 创建表，必须包含 `deleted`, `created_at` 等基类字段。
2.  **Entity**: 继承 `BaseEntity`，添加 MyBatis-Plus 注解。
3.  **Persistence**: 创建 `Mapper` 接口，若有复杂聚合查询，优先考虑在 `StatisticsMapper` 或对应的 XML 中编写。
4.  **Application**: 编写 `AppService`。在此处注入 Repository，编写核心算法，标注 `@Transactional`。
5.  **Interface**: 编写 `Controller`，使用 `Assembler` 将 Entity 转换为 DTO。

---

## 4. 关键参数校准 (Truth Calibration)

| 项 | 实装细节 | 物理位置 |
| :--- | :--- | :--- |
| **Token 过期** | 86,400,000s (24小时) | `application.yml` |
| **OCR 权重** | 40/35/15/10 (金额/款方/日期/项目) | `PaymentReconciliationService` |
| **文件存储** | MinIO Bucket: `documents` | `MinioService` |
| **工作流** | 手动修改 `status` 字段 | 各业务模块 `AppService` |

---
> 📅 **版本**: 2026-01-05 | **状态**: 深度实修版 | **维护**: Antigravity
