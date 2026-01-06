# 智慧律所管理系统 - 开发者核心全书 (Developer Encyclopedia)

> 版本：V1.0 | 状态：单一事实来源 (Single Source of Truth)

---

## 第一部分：业务灵魂与核心算法

### 1.1 薪酬与角色模型 (HR & Finance)
系统将人员身份拆分为“项目角色”与“薪酬模式”两个独立维度。

#### 1.1.1 项目分配比例归一化算法
当办案团队中同时存在**提成制**与**授薪制**律师时，系统按以下逻辑锁定分配基数：
- **逻辑**：仅提成制律师参与三层分配，授薪律师份额自动归零并回流律所。
- **公式**：`实际分配比例 = 个人原定比例 / 所有提成制律师原定比例之和`。
- **算例**：原定 主办60%(提成) + 协办A 30%(授薪) + 协办B 10%(提成)。
  - 最终提成：主办得 `60/(60+10) = 85.7%`，协办B得 `10/(60+10) = 14.3%`。

#### 1.1.2 财务核算：三层分配模型
确认收款后，系统按以下顺序层层扣减：
1.  **第一层：律所留存** (Gross * FirmRatio)
2.  **第二层：案源提成** ( (Gross - 留存) * OriginatorRatio )
3.  **第三层：办案提成** ( (Gross - 留存 - 案源) * PracticeRatio )

### 1.2 OCR 智能核销算法 (Reconciliation)
系统通过银行回单图片自动匹配待收账款，采用加权分值模型：

| 匹配指标 | 权重 | 判定规则 |
| :--- | :--- | :--- |
| **金额匹配** | 40% | 误差 5% 内得 0.95 分；完全一致得满分。 |
| **付款方匹配** | 35% | 客户名称、法定代表人或联系人包含匹配关键字符。 |
| **时间匹配** | 15% | 收款日期与计划日期差异：当日(100%)，3天内(90%)，7天内(70%)。 |
| **项目状态** | 10% | 关联项目处于 `ACTIVE` 状态加 0.1 分。 |

---

## 第二部分：系统架构与技术标准

### 2.1 架构分层规范
严格遵循 **Domain-Driven Design (DDD)** 思路：

```text
com.lawfirm.
├── interfaces/      # 接口层 (REST API / Security 鉴权拦截)
├── application/     # 应用层 (用例编排 / 事务控制 / 提成计算编排)
├── domain/          # 领域层 (实体 Entity / 核心业务方法 / 提成归一化逻辑)
└── infrastructure/  # 基础设施层 (数据映射器 / MinIO / OCR 实现)
```

### 2.2 核心技术参数
- **存储**: MinIO (Bucket: `documents`, `/documents/{matter_id}/` 路。
- **安全**: JWT Token 鉴权，过期时间 24 小时 (`jwt.expiration=86400000`)。
- **审批**: 当前采用手动状态机，规划中将引入 Flowable (`flowable_` 数据库前缀)。

---

## 第三部分：关键数据契约 (Data Contract)

### 3.1 支付计划 JSON 结构
合同文件中的 `payment_schedule` 字段需符合以下格式：
```json
{
  "totalAmount": 100000.00,
  "paymentSchedule": [
    { "stage": "预付款", "amount": 30000.00, "status": "PENDING" },
    { "stage": "进展款", "amount": 70000.00, "status": "PENDING" }
  ]
}
```

### 3.2 统一响应格式
```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1672531200000
}
```

---

## 第四部分：开发者执行准则
1.  **代码位置**: 提成计算主逻辑位于 `application/finance/service/CommissionCalculationService.java`。
2.  **异常处理**: 业务异常统一抛出 `BusinessException`。
3.  **审计日志**: `DocAccessLogService` 虽已实现，但在业务入口的挂载尚不完整，需优先对齐该标准。
