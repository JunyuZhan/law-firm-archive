# AI使用量记录与费用分摊设计方案

> 文档版本：v1.0  
> 创建日期：2026-01-14  
> 状态：设计中

## 修订说明（v1.1）

- 标准化 usage 解析与降级：优先解析供应商返回的 Token 用量（OpenAI/DeepSeek/Qwen/Wenxin/Claude/Moonshot/Ollama/Local 等分别适配）；无 usage 的供应商按 TOKEN 估算或 PER_CALL 固定计费，确保计费闭环
- 记录隐私最小化：使用记录不保存完整 prompt/全文，仅保存模型、用量、费用与业务标签；错误信息脱敏并裁剪
- 成本计算与精度：单价单位为“元/千Token”，总费用四舍五入至 4 位小数；每条记录固化 charge_ratio 快照与定价来源（integration_code/model），保证账单可复现
- SQL 修正：更正 ai_usage_log 的注释列名为 charge_ratio；补充索引命名与唯一约束一致性说明，外键依赖的表需存在且具备必要索引
- 集成方式：在 LlmClient 增加返回原始响应体的调用变体，由 AiUsageRecorder 统一解析 usage 与计费，异步写库，不阻塞业务
- 配置与权限：统一 sys_config 键名为 ai.billing.* 与 ai.usage.*；权限代码统一为 ai:usage:view、ai:billing:view、ai:billing:manage；用户仅可见自身数据，管理员视图受角色控制
- 调度与稳定性：依赖 @EnableAsync 与线程池；账单生成定时任务在失败时告警并可重试；异步异常统一处理；调用端保留重试但不影响记录器一致性
- 前后端迭代策略：优先交付“我的使用/统计”与基础记录，随后上线管理员统计与账单管理，最后打通工资扣减关联

## 一、需求背景

### 1.1 业务需求

律所使用AI大模型生成法律文书，产生的API调用费用由使用人承担：
- **谁用谁付**：费用由调用AI的用户承担
- **用户透明**：用户能查看自己的使用记录和费用
- **工资扣减**：费用从工资中扣除
- **管理员可配置**：是否收费、收费比例等由管理员设置

### 1.2 现状分析

当前系统AI调用存在的问题：

```java
// LlmClient.java - 当前代码没有记录使用量
public String generate(ExternalIntegration integration, String systemPrompt, String userPrompt) {
    // 直接调用，没有记录：
    // - 谁调用的
    // - 调用了多少次
    // - 消耗了多少Token
    // - 产生了多少费用
}
```

### 1.3 功能目标

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 使用量记录 | 记录每次AI调用的用户、Token数、模型等 | P0 |
| 费用计算 | 根据Token数量和单价计算费用 | P0 |
| 用户查看 | 用户可查看自己的使用记录和统计 | P0 |
| **管理员配置** | 管理员可配置收费开关、费率等 | P0 |
| 月度账单 | 按月生成用户AI费用账单 | P1 |
| 工资集成 | 费用与工资系统对接，支持扣减 | P1 |
| 配额控制 | 可设置用户月度使用配额 | P2 |

### 1.4 管理员可配置项

管理员可以在**系统设置**中配置以下选项：

| 配置项 | 说明 | 默认值 |
|-------|------|-------|
| **是否启用收费** | 关闭则所有AI调用免费 | 开启 |
| **收费比例** | 用户承担的比例（0-100%） | 100% |
| **是否从工资扣减** | 开启则自动从工资扣除 | 开启 |
| **免费额度** | 每月免费Token数（超出才收费） | 0 |
| **单价设置** | 不同模型的计费单价 | 按模型默认 |
| **免费用户** | 指定某些用户免费（如管理员） | 无 |

---

## 二、整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端层                                   │
├─────────────────────────────────────────────────────────────────┤
│  我的AI使用          管理员统计          财务工资扣减             │
│  - 使用记录          - 全员统计          - 月度账单               │
│  - 本月费用          - 费用排行          - 批量扣减               │
│  - 配额剩余          - 模型使用分布      - 扣减记录               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      控制器层                                    │
├─────────────────────────────────────────────────────────────────┤
│  AiUsageController                                               │
│  - GET  /ai/usage/my              # 我的使用记录                 │
│  - GET  /ai/usage/my/summary      # 我的使用统计                 │
│  - GET  /ai/usage/statistics      # 管理员-全员统计              │
│  - GET  /ai/usage/billing         # 管理员-账单列表              │
│  - POST /ai/usage/billing/salary  # 生成工资扣减记录             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     应用层                                       │
├─────────────────────────────────────────────────────────────────┤
│  AiUsageAppService           AiBillingAppService                 │
│  - 记录使用                  - 计算费用                          │
│  - 查询统计                  - 生成账单                          │
│  - 配额检查                  - 工资对接                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    基础设施层                                    │
├─────────────────────────────────────────────────────────────────┤
│  AiUsageRecorder（使用量记录器）                                  │
│  - 拦截LlmClient调用                                             │
│  - 解析响应获取Token数                                           │
│  - 异步写入数据库                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、数据库设计

### 3.1 AI使用记录表

```sql
-- AI使用记录表（每次调用一条记录）
CREATE TABLE public.ai_usage_log (
    id BIGSERIAL PRIMARY KEY,
    
    -- 用户信息
    user_id BIGINT NOT NULL,                -- 调用用户ID
    user_name VARCHAR(100),                 -- 用户姓名（冗余，便于查询）
    department_id BIGINT,                   -- 所属部门
    department_name VARCHAR(100),           -- 部门名称
    
    -- AI模型信息
    integration_id BIGINT NOT NULL,         -- AI集成配置ID
    integration_code VARCHAR(50) NOT NULL,  -- 集成编码（如AI_DEEPSEEK）
    integration_name VARCHAR(100),          -- 集成名称
    model_name VARCHAR(100),                -- 具体模型名（如deepseek-chat）
    
    -- 调用信息
    request_type VARCHAR(50) NOT NULL,      -- 请求类型：DOCUMENT_GENERATE/CHAT/SUMMARY等
    business_type VARCHAR(50),              -- 业务类型：MATTER/PERSONAL
    business_id BIGINT,                     -- 业务ID（如项目ID）
    
    -- Token统计
    prompt_tokens INTEGER DEFAULT 0,        -- 输入Token数
    completion_tokens INTEGER DEFAULT 0,    -- 输出Token数
    total_tokens INTEGER DEFAULT 0,         -- 总Token数
    
    -- 费用信息
    prompt_price DECIMAL(10,6) DEFAULT 0,   -- 输入Token单价（元/千Token）
    completion_price DECIMAL(10,6) DEFAULT 0, -- 输出Token单价（元/千Token）
    total_cost DECIMAL(10,4) DEFAULT 0,     -- 总费用（元）
    
    -- 用户应付费用（根据管理员配置的收费比例计算）
    user_cost DECIMAL(10,4) DEFAULT 0,      -- 用户承担费用
    charge_ratio INTEGER DEFAULT 100,       -- 当时的收费比例（记录快照）
    
    -- 调用结果
    success BOOLEAN DEFAULT TRUE,           -- 是否成功
    error_message TEXT,                     -- 错误信息
    duration_ms INTEGER,                    -- 响应时间（毫秒）
    
    -- 元数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 索引优化
    CONSTRAINT fk_ai_usage_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 索引
CREATE INDEX idx_ai_usage_user ON public.ai_usage_log(user_id);
CREATE INDEX idx_ai_usage_time ON public.ai_usage_log(created_at);
CREATE INDEX idx_ai_usage_user_month ON public.ai_usage_log(user_id, DATE_TRUNC('month', created_at));
CREATE INDEX idx_ai_usage_dept ON public.ai_usage_log(department_id);
CREATE INDEX idx_ai_usage_model ON public.ai_usage_log(integration_code);

COMMENT ON TABLE public.ai_usage_log IS 'AI使用记录表，记录每次AI调用的详细信息';
COMMENT ON COLUMN public.ai_usage_log.charge_ratio IS '用户承担比例（百分比），100表示全额承担，50表示各承担50%';
```

### 3.2 AI计费系统配置（使用系统配置表）

通过现有的 `sys_config` 表存储AI计费相关配置，管理员可在系统设置中修改：

```sql
-- 在 sys_config 表中添加AI计费配置
INSERT INTO public.sys_config (config_key, config_value, config_name, config_type, remark) VALUES
-- 基础开关
('ai.billing.enabled', 'true', 'AI计费开关', 'BOOLEAN', '是否启用AI使用计费，关闭则所有调用免费'),
('ai.billing.charge_ratio', '100', 'AI收费比例', 'NUMBER', '用户承担费用的比例（0-100），100表示全额承担'),
('ai.billing.salary_deduction', 'true', '工资扣减开关', 'BOOLEAN', '是否从工资中自动扣减AI费用'),

-- 免费额度
('ai.billing.free_tokens', '0', '月度免费Token', 'NUMBER', '每用户每月免费Token额度，超出部分才收费'),
('ai.billing.free_amount', '0', '月度免费金额', 'NUMBER', '每用户每月免费金额（元），超出部分才收费'),

-- 配额限制
('ai.billing.max_tokens', '0', '月度Token上限', 'NUMBER', '每用户每月最多可用Token数，0表示无限制'),
('ai.billing.max_amount', '0', '月度费用上限', 'NUMBER', '每用户每月最多费用（元），0表示无限制');

COMMENT ON TABLE public.sys_config IS '系统配置表，包含AI计费等全局配置';
```

**管理员配置界面说明**：

| 配置项 | 说明 | 示例 |
|-------|------|-----|
| **AI计费开关** | 关闭后所有AI调用免费 | 开启/关闭 |
| **收费比例** | 用户承担的百分比 | 100%=全付，50%=付一半，0%=免费 |
| **工资扣减** | 是否自动从工资扣除 | 开启/关闭 |
| **免费额度** | 每月免费使用量 | 如：每月10万Token免费 |
| **使用上限** | 防止过度使用 | 如：每月最多50元 |

### 3.3 AI模型定价表

```sql
-- AI模型定价表（不同模型的单价）
CREATE TABLE public.ai_pricing_config (
    id BIGSERIAL PRIMARY KEY,
    
    integration_code VARCHAR(50) NOT NULL,  -- 集成编码（如AI_DEEPSEEK）
    model_name VARCHAR(100),                -- 模型名称（可为空，表示默认价格）
    
    -- 定价（单位：元/千Token）
    prompt_price DECIMAL(10,6) NOT NULL,    -- 输入Token单价
    completion_price DECIMAL(10,6) NOT NULL,-- 输出Token单价
    
    -- 也支持按次计费
    per_call_price DECIMAL(10,4),           -- 每次调用固定费用（可选）
    pricing_mode VARCHAR(20) DEFAULT 'TOKEN', -- 计费模式：TOKEN/PER_CALL
    
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_ai_pricing UNIQUE (integration_code, model_name)
);

-- 初始化常用模型定价（参考价格，实际以API供应商为准）
INSERT INTO public.ai_pricing_config (integration_code, model_name, prompt_price, completion_price) VALUES
-- DeepSeek（性价比高）
('AI_DEEPSEEK', 'deepseek-chat', 0.001, 0.002),
('AI_DEEPSEEK_R1', 'deepseek-reasoner', 0.004, 0.016),

-- 通义千问
('AI_QWEN', 'qwen-max', 0.012, 0.012),
('AI_QWEN', 'qwen-plus', 0.004, 0.012),

-- 智谱
('AI_ZHIPU', 'glm-4', 0.1, 0.1),
('AI_ZHIPU', 'glm-3-turbo', 0.001, 0.001),

-- 文心一言
('AI_WENXIN', 'ernie-bot-4', 0.12, 0.12),

-- Moonshot Kimi
('AI_MOONSHOT', 'moonshot-v1-8k', 0.012, 0.012),

-- OpenAI
('AI_OPENAI', 'gpt-4', 0.21, 0.42),
('AI_OPENAI', 'gpt-3.5-turbo', 0.0035, 0.0105),

-- Claude
('AI_CLAUDE', 'claude-3-opus', 0.105, 0.525),
('AI_CLAUDE', 'claude-3-sonnet', 0.021, 0.105),

-- 本地部署模型（免费）
('AI_OLLAMA', NULL, 0, 0),
('AI_DIFY', NULL, 0, 0),
('AI_LOCALAI', NULL, 0, 0);

COMMENT ON TABLE public.ai_pricing_config IS 'AI模型定价表，管理员可配置不同模型的单价';
```

### 3.3 用户AI配额表

```sql
-- 用户AI配额表（可选功能）
CREATE TABLE public.ai_user_quota (
    id BIGSERIAL PRIMARY KEY,
    
    user_id BIGINT NOT NULL UNIQUE,         -- 用户ID
    
    -- 月度配额
    monthly_token_quota BIGINT,             -- 月度Token配额（NULL表示无限制）
    monthly_cost_quota DECIMAL(10,2),       -- 月度费用配额（元，NULL表示无限制）
    
    -- 当月使用量（每月初重置）
    current_month_tokens BIGINT DEFAULT 0,  -- 当月已用Token
    current_month_cost DECIMAL(10,4) DEFAULT 0, -- 当月已产生费用
    quota_reset_date DATE,                  -- 配额重置日期
    
    -- 特殊设置
    custom_cost_ratio DECIMAL(3,2),         -- 个人自定义承担比例（覆盖默认）
    exempt_billing BOOLEAN DEFAULT FALSE,   -- 是否免计费（如管理员）
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ai_quota_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

COMMENT ON TABLE public.ai_user_quota IS '用户AI使用配额表';
```

### 3.4 月度账单表

```sql
-- AI月度账单表
CREATE TABLE public.ai_monthly_bill (
    id BIGSERIAL PRIMARY KEY,
    
    -- 账单周期
    bill_year INTEGER NOT NULL,             -- 账单年份
    bill_month INTEGER NOT NULL,            -- 账单月份（1-12）
    
    -- 用户信息
    user_id BIGINT NOT NULL,
    user_name VARCHAR(100),
    department_id BIGINT,
    department_name VARCHAR(100),
    
    -- 使用统计
    total_calls INTEGER DEFAULT 0,          -- 调用次数
    total_tokens BIGINT DEFAULT 0,          -- 总Token数
    prompt_tokens BIGINT DEFAULT 0,         -- 输入Token数
    completion_tokens BIGINT DEFAULT 0,     -- 输出Token数
    
    -- 费用明细
    total_cost DECIMAL(10,4) DEFAULT 0,     -- API总费用
    user_cost DECIMAL(10,4) DEFAULT 0,      -- 用户应付费用（按收费比例计算）
    charge_ratio INTEGER DEFAULT 100,       -- 当月收费比例（记录快照）
    
    -- 扣减状态
    deduction_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING/DEDUCTED/WAIVED
    deduction_amount DECIMAL(10,4),         -- 实际扣减金额
    deducted_at TIMESTAMP,                  -- 扣减时间
    deducted_by BIGINT,                     -- 操作人
    deduction_remark TEXT,                  -- 扣减备注
    
    -- 关联工资记录
    salary_deduction_id BIGINT,             -- 关联的工资扣减记录ID
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_ai_bill_month UNIQUE (bill_year, bill_month, user_id)
);

CREATE INDEX idx_ai_bill_user ON public.ai_monthly_bill(user_id);
CREATE INDEX idx_ai_bill_period ON public.ai_monthly_bill(bill_year, bill_month);
CREATE INDEX idx_ai_bill_status ON public.ai_monthly_bill(deduction_status);

COMMENT ON TABLE public.ai_monthly_bill IS 'AI月度账单表，按月汇总用户AI使用费用';
COMMENT ON COLUMN public.ai_monthly_bill.deduction_status IS '扣减状态：PENDING-待扣减，DEDUCTED-已扣减，WAIVED-已减免';
```

---

## 四、后端代码设计

### 4.1 目录结构

```
backend/src/main/java/com/lawfirm/
├── application/
│   └── ai/
│       ├── service/
│       │   ├── AiUsageAppService.java      # 使用量服务
│       │   └── AiBillingAppService.java    # 账单服务
│       ├── dto/
│       │   ├── AiUsageLogDTO.java          # 使用记录DTO
│       │   ├── AiUsageSummaryDTO.java      # 使用统计DTO
│       │   ├── AiMonthlyBillDTO.java       # 月度账单DTO
│       │   └── AiUsageQueryDTO.java        # 查询条件DTO
│       └── command/
│           └── DeductionCommand.java        # 扣减命令
├── domain/
│   └── ai/
│       ├── entity/
│       │   ├── AiUsageLog.java             # 使用记录实体
│       │   ├── AiPricingConfig.java        # 定价配置实体
│       │   ├── AiUserQuota.java            # 用户配额实体
│       │   └── AiMonthlyBill.java          # 月度账单实体
│       └── repository/
│           ├── AiUsageLogRepository.java
│           ├── AiPricingConfigRepository.java
│           ├── AiUserQuotaRepository.java
│           └── AiMonthlyBillRepository.java
├── infrastructure/
│   └── ai/
│       ├── LlmClient.java                  # 【修改】增加使用量记录
│       └── AiUsageRecorder.java            # 【新增】使用量记录器
└── interfaces/
    └── rest/
        └── ai/
            └── AiUsageController.java       # REST控制器
```

### 4.2 使用量记录器

```java
package com.lawfirm.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.ai.entity.AiPricingConfig;
import com.lawfirm.domain.ai.entity.AiUsageLog;
import com.lawfirm.domain.ai.entity.AiUserQuota;
import com.lawfirm.domain.ai.repository.AiPricingConfigRepository;
import com.lawfirm.domain.ai.repository.AiUsageLogRepository;
import com.lawfirm.domain.ai.repository.AiUserQuotaRepository;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * AI使用量记录器
 * 
 * 负责：
 * 1. 解析API响应获取Token数量
 * 2. 计算费用
 * 3. 异步写入数据库
 * 4. 更新用户配额
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiUsageRecorder {

    private final AiUsageLogRepository usageLogRepository;
    private final AiPricingConfigRepository pricingRepository;
    private final AiUserQuotaRepository quotaRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 记录AI使用量（异步执行，不影响主流程）
     */
    @Async
    @Transactional
    public void recordUsage(
            ExternalIntegration integration,
            String requestType,
            String businessType,
            Long businessId,
            String responseBody,
            long durationMs,
            boolean success,
            String errorMessage) {
        
        try {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                log.warn("无法记录AI使用量：用户未登录");
                return;
            }

            // 1. 解析Token数量
            TokenUsage tokenUsage = parseTokenUsage(integration.getIntegrationCode(), responseBody);
            
            // 2. 获取定价配置
            AiPricingConfig pricing = getPricing(integration.getIntegrationCode(), 
                    getModelName(integration));
            
            // 3. 计算费用
            BigDecimal totalCost = calculateCost(tokenUsage, pricing);
            BigDecimal costRatio = getCostRatio(userId, pricing);
            BigDecimal userCost = totalCost.multiply(costRatio).setScale(4, RoundingMode.HALF_UP);
            BigDecimal companyCost = totalCost.subtract(userCost);

            // 4. 获取用户信息
            User user = userRepository.findById(userId);
            
            // 5. 创建使用记录
            AiUsageLog usageLog = AiUsageLog.builder()
                    .userId(userId)
                    .userName(user != null ? user.getRealName() : null)
                    .departmentId(user != null ? user.getDepartmentId() : null)
                    .integrationId(integration.getId())
                    .integrationCode(integration.getIntegrationCode())
                    .integrationName(integration.getIntegrationName())
                    .modelName(getModelName(integration))
                    .requestType(requestType)
                    .businessType(businessType)
                    .businessId(businessId)
                    .promptTokens(tokenUsage.promptTokens)
                    .completionTokens(tokenUsage.completionTokens)
                    .totalTokens(tokenUsage.totalTokens)
                    .promptPrice(pricing != null ? pricing.getPromptPrice() : BigDecimal.ZERO)
                    .completionPrice(pricing != null ? pricing.getCompletionPrice() : BigDecimal.ZERO)
                    .totalCost(totalCost)
                    .userCost(userCost)
                    .companyCost(companyCost)
                    .costRatio(costRatio)
                    .success(success)
                    .errorMessage(errorMessage)
                    .durationMs((int) durationMs)
                    .build();
            
            usageLogRepository.save(usageLog);
            
            // 6. 更新用户配额
            updateUserQuota(userId, tokenUsage.totalTokens, userCost);
            
            log.info("AI使用记录已保存: user={}, model={}, tokens={}, cost={}元(用户承担{}元)",
                    userId, integration.getIntegrationCode(), 
                    tokenUsage.totalTokens, totalCost, userCost);
            
        } catch (Exception e) {
            log.error("记录AI使用量失败", e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 解析Token使用量
     * 不同模型的响应格式不同，需要适配
     */
    private TokenUsage parseTokenUsage(String integrationCode, String responseBody) {
        TokenUsage usage = new TokenUsage();
        
        if (responseBody == null || responseBody.isEmpty()) {
            return usage;
        }
        
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            
            // OpenAI 风格（大多数模型兼容）
            JsonNode usageNode = json.path("usage");
            if (!usageNode.isMissingNode()) {
                usage.promptTokens = usageNode.path("prompt_tokens").asInt(0);
                usage.completionTokens = usageNode.path("completion_tokens").asInt(0);
                usage.totalTokens = usageNode.path("total_tokens").asInt(
                        usage.promptTokens + usage.completionTokens);
                return usage;
            }
            
            // 通义千问格式
            JsonNode qwenUsage = json.path("usage");
            if (!qwenUsage.isMissingNode()) {
                usage.promptTokens = qwenUsage.path("input_tokens").asInt(0);
                usage.completionTokens = qwenUsage.path("output_tokens").asInt(0);
                usage.totalTokens = usage.promptTokens + usage.completionTokens;
                return usage;
            }
            
            // 文心一言格式
            if (json.has("usage")) {
                JsonNode wxUsage = json.path("usage");
                usage.promptTokens = wxUsage.path("prompt_tokens").asInt(0);
                usage.completionTokens = wxUsage.path("completion_tokens").asInt(0);
                usage.totalTokens = wxUsage.path("total_tokens").asInt(
                        usage.promptTokens + usage.completionTokens);
                return usage;
            }
            
            // 如果解析失败，估算Token数（按字符数粗略估计）
            String content = extractContent(json);
            if (content != null) {
                // 中文约1.5字符=1Token，英文约4字符=1Token，取中间值
                usage.totalTokens = content.length() / 2;
                usage.completionTokens = usage.totalTokens;
            }
            
        } catch (Exception e) {
            log.warn("解析Token使用量失败: {}", e.getMessage());
        }
        
        return usage;
    }

    /**
     * 从响应中提取内容（用于估算Token）
     */
    private String extractContent(JsonNode json) {
        // OpenAI格式
        JsonNode choices = json.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText(null);
        }
        // 其他格式
        if (json.has("result")) {
            return json.path("result").asText(null);
        }
        if (json.has("answer")) {
            return json.path("answer").asText(null);
        }
        return null;
    }

    /**
     * 获取定价配置
     */
    private AiPricingConfig getPricing(String integrationCode, String modelName) {
        // 先精确匹配模型
        AiPricingConfig config = pricingRepository.findByCodeAndModel(integrationCode, modelName);
        if (config != null) {
            return config;
        }
        // 再匹配该集成的默认配置
        return pricingRepository.findByCodeAndModel(integrationCode, null);
    }

    /**
     * 计算费用
     */
    private BigDecimal calculateCost(TokenUsage usage, AiPricingConfig pricing) {
        if (pricing == null) {
            return BigDecimal.ZERO;
        }
        
        // 按Token计费（单价是每千Token）
        BigDecimal promptCost = pricing.getPromptPrice()
                .multiply(BigDecimal.valueOf(usage.promptTokens))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        
        BigDecimal completionCost = pricing.getCompletionPrice()
                .multiply(BigDecimal.valueOf(usage.completionTokens))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        
        BigDecimal totalCost = promptCost.add(completionCost);
        
        // 如果有按次计费，加上
        if (pricing.getPerCallPrice() != null) {
            totalCost = totalCost.add(pricing.getPerCallPrice());
        }
        
        return totalCost.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 获取收费比例（从系统配置读取，管理员可修改）
     */
    private int getChargeRatio(Long userId) {
        // 1. 检查用户是否在免费名单
        AiUserQuota quota = quotaRepository.findByUserId(userId);
        if (quota != null && Boolean.TRUE.equals(quota.getExemptBilling())) {
            return 0; // 免计费用户
        }
        
        // 2. 读取管理员配置的收费比例
        String ratioStr = configService.getConfigValue("ai.billing.charge_ratio", "100");
        return Integer.parseInt(ratioStr);
    }
    
    /**
     * 计算用户应付费用
     * 公式：用户费用 = 总费用 × 收费比例%
     */
    private BigDecimal calculateUserCost(BigDecimal totalCost, int chargeRatio) {
        if (chargeRatio <= 0) return BigDecimal.ZERO;
        if (chargeRatio >= 100) return totalCost;
        
        return totalCost.multiply(BigDecimal.valueOf(chargeRatio))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /**
     * 更新用户配额
     */
    private void updateUserQuota(Long userId, int tokens, BigDecimal cost) {
        AiUserQuota quota = quotaRepository.findByUserId(userId);
        if (quota == null) {
            // 创建配额记录
            quota = new AiUserQuota();
            quota.setUserId(userId);
            quota.setQuotaResetDate(LocalDate.now().withDayOfMonth(1));
        }
        
        // 检查是否需要重置（新月份）
        LocalDate today = LocalDate.now();
        if (quota.getQuotaResetDate() == null || 
            quota.getQuotaResetDate().isBefore(today.withDayOfMonth(1))) {
            quota.setCurrentMonthTokens(0L);
            quota.setCurrentMonthCost(BigDecimal.ZERO);
            quota.setQuotaResetDate(today.withDayOfMonth(1));
        }
        
        // 累加使用量
        quota.setCurrentMonthTokens(quota.getCurrentMonthTokens() + tokens);
        quota.setCurrentMonthCost(quota.getCurrentMonthCost().add(cost));
        
        quotaRepository.save(quota);
    }

    /**
     * 从集成配置获取模型名称
     */
    private String getModelName(ExternalIntegration integration) {
        if (integration.getExtraConfig() != null) {
            Object model = integration.getExtraConfig().get("model");
            return model != null ? model.toString() : null;
        }
        return null;
    }

    /**
     * Token使用量内部类
     */
    private static class TokenUsage {
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
    }
}
```

### 4.3 修改LlmClient增加使用量记录

```java
// 在 LlmClient.java 中添加使用量记录

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final ObjectMapper objectMapper;
    private final AiUsageRecorder usageRecorder;  // 新增

    /**
     * 调用大模型生成文本（带使用量记录）
     */
    public String generate(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        return generate(integration, systemPrompt, userPrompt, "GENERAL", null, null);
    }

    /**
     * 调用大模型生成文本（完整版，支持使用量记录）
     * 
     * @param integration AI集成配置
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param requestType 请求类型（DOCUMENT_GENERATE/CHAT/SUMMARY等）
     * @param businessType 业务类型（MATTER/PERSONAL）
     * @param businessId 业务ID（如项目ID）
     */
    public String generate(ExternalIntegration integration, String systemPrompt, String userPrompt,
                          String requestType, String businessType, Long businessId) {
        String code = integration.getIntegrationCode();
        long startTime = System.currentTimeMillis();
        String responseBody = null;
        boolean success = true;
        String errorMessage = null;
        
        log.info("调用大模型: code={}, name={}, requestType={}", 
                code, integration.getIntegrationName(), requestType);
        
        try {
            // 原有的调用逻辑...
            responseBody = switch (code) {
                case "AI_OPENAI" -> callOpenAIWithResponse(integration, systemPrompt, userPrompt);
                case "AI_DEEPSEEK", "AI_DEEPSEEK_R1" -> callDeepSeekWithResponse(integration, systemPrompt, userPrompt);
                // ... 其他模型
                default -> throw new RuntimeException("不支持的 AI 模型: " + code);
            };
            
            // 解析内容
            return extractContentFromResponse(code, responseBody);
            
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
            
        } finally {
            // 异步记录使用量
            long duration = System.currentTimeMillis() - startTime;
            usageRecorder.recordUsage(
                integration, requestType, businessType, businessId,
                responseBody, duration, success, errorMessage
            );
        }
    }
    
    // 修改调用方法，返回原始响应体（用于解析Token数）
    private String callOpenAIWithResponse(ExternalIntegration integration, 
                                          String systemPrompt, String userPrompt) {
        // ... 调用逻辑，返回原始responseBody
    }
}
```

### 4.4 使用量应用服务

```java
package com.lawfirm.application.ai.service;

import com.lawfirm.application.ai.dto.*;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.ai.entity.AiUsageLog;
import com.lawfirm.domain.ai.entity.AiUserQuota;
import com.lawfirm.domain.ai.repository.AiUsageLogRepository;
import com.lawfirm.domain.ai.repository.AiUserQuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * AI使用量应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageAppService {

    private final AiUsageLogRepository usageLogRepository;
    private final AiUserQuotaRepository quotaRepository;

    /**
     * 查询我的使用记录
     */
    public PageResult<AiUsageLogDTO> getMyUsageLogs(AiUsageQueryDTO query) {
        Long userId = SecurityUtils.getUserId();
        query.setUserId(userId);
        return usageLogRepository.queryPage(query);
    }

    /**
     * 获取我的使用统计
     */
    public AiUsageSummaryDTO getMyUsageSummary(YearMonth month) {
        Long userId = SecurityUtils.getUserId();
        return getUsageSummary(userId, month);
    }

    /**
     * 获取用户使用统计
     */
    public AiUsageSummaryDTO getUsageSummary(Long userId, YearMonth month) {
        if (month == null) {
            month = YearMonth.now();
        }

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        // 查询统计数据
        Map<String, Object> stats = usageLogRepository.getSummary(userId, startDate, endDate);
        
        // 查询配额信息
        AiUserQuota quota = quotaRepository.findByUserId(userId);

        return AiUsageSummaryDTO.builder()
                .userId(userId)
                .month(month.toString())
                .totalCalls(((Number) stats.get("total_calls")).intValue())
                .totalTokens(((Number) stats.get("total_tokens")).longValue())
                .promptTokens(((Number) stats.get("prompt_tokens")).longValue())
                .completionTokens(((Number) stats.get("completion_tokens")).longValue())
                .totalCost((BigDecimal) stats.get("total_cost"))
                .userCost((BigDecimal) stats.get("user_cost"))
                .companyCost((BigDecimal) stats.get("company_cost"))
                // 配额信息
                .monthlyTokenQuota(quota != null ? quota.getMonthlyTokenQuota() : null)
                .monthlyCostQuota(quota != null ? quota.getMonthlyCostQuota() : null)
                .tokenUsagePercent(calculateUsagePercent(
                        ((Number) stats.get("total_tokens")).longValue(),
                        quota != null ? quota.getMonthlyTokenQuota() : null))
                .costUsagePercent(calculateUsagePercent(
                        (BigDecimal) stats.get("user_cost"),
                        quota != null ? quota.getMonthlyCostQuota() : null))
                .build();
    }

    /**
     * 获取按模型分组的使用统计
     */
    public List<Map<String, Object>> getUsageByModel(Long userId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return usageLogRepository.getUsageByModel(userId, startDate, endDate);
    }

    /**
     * 管理员-获取全员统计
     */
    public List<AiUsageSummaryDTO> getAllUsersSummary(YearMonth month) {
        SecurityUtils.checkRole("ADMIN", "FINANCE", "DIRECTOR");
        
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        
        return usageLogRepository.getAllUsersSummary(startDate, endDate);
    }

    /**
     * 管理员-获取部门统计
     */
    public List<Map<String, Object>> getDepartmentSummary(YearMonth month) {
        SecurityUtils.checkRole("ADMIN", "FINANCE", "DIRECTOR");
        
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        
        return usageLogRepository.getDepartmentSummary(startDate, endDate);
    }

    /**
     * 检查用户是否超配额
     */
    public boolean checkQuota(Long userId) {
        AiUserQuota quota = quotaRepository.findByUserId(userId);
        if (quota == null) {
            return true; // 无配额限制
        }

        // 检查Token配额
        if (quota.getMonthlyTokenQuota() != null && 
            quota.getCurrentMonthTokens() >= quota.getMonthlyTokenQuota()) {
            return false;
        }

        // 检查费用配额
        if (quota.getMonthlyCostQuota() != null && 
            quota.getCurrentMonthCost().compareTo(quota.getMonthlyCostQuota()) >= 0) {
            return false;
        }

        return true;
    }

    private Double calculateUsagePercent(Long used, Long quota) {
        if (quota == null || quota == 0) return null;
        return used * 100.0 / quota;
    }

    private Double calculateUsagePercent(BigDecimal used, BigDecimal quota) {
        if (quota == null || quota.compareTo(BigDecimal.ZERO) == 0) return null;
        return used.multiply(BigDecimal.valueOf(100))
                .divide(quota, 2, BigDecimal.ROUND_HALF_UP)
                .doubleValue();
    }
}
```

### 4.5 REST控制器

```java
package com.lawfirm.interfaces.rest.ai;

import com.lawfirm.application.ai.dto.*;
import com.lawfirm.application.ai.service.AiUsageAppService;
import com.lawfirm.application.ai.service.AiBillingAppService;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Tag(name = "AI使用量", description = "AI使用量记录与费用查询")
@RestController
@RequestMapping("/ai/usage")
@RequiredArgsConstructor
public class AiUsageController {

    private final AiUsageAppService usageAppService;
    private final AiBillingAppService billingAppService;

    // ==================== 个人使用量查询 ====================

    @Operation(summary = "我的使用记录", description = "查询当前用户的AI使用记录")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<AiUsageLogDTO>> getMyUsageLogs(AiUsageQueryDTO query) {
        return Result.success(usageAppService.getMyUsageLogs(query));
    }

    @Operation(summary = "我的使用统计", description = "获取当前用户的月度使用统计")
    @GetMapping("/my/summary")
    @PreAuthorize("isAuthenticated()")
    public Result<AiUsageSummaryDTO> getMyUsageSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return Result.success(usageAppService.getMyUsageSummary(month));
    }

    @Operation(summary = "我的模型使用分布", description = "获取当前用户按模型分组的使用统计")
    @GetMapping("/my/by-model")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> getMyUsageByModel(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
        if (month == null) month = YearMonth.now();
        return Result.success(usageAppService.getUsageByModel(userId, month));
    }

    // ==================== 管理员统计 ====================

    @Operation(summary = "全员使用统计", description = "管理员查看所有用户的AI使用统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('ai:usage:view', 'finance:view')")
    public Result<List<AiUsageSummaryDTO>> getAllUsersSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        if (month == null) month = YearMonth.now();
        return Result.success(usageAppService.getAllUsersSummary(month));
    }

    @Operation(summary = "部门使用统计", description = "按部门统计AI使用情况")
    @GetMapping("/statistics/department")
    @PreAuthorize("hasAnyAuthority('ai:usage:view', 'finance:view')")
    public Result<List<Map<String, Object>>> getDepartmentSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        if (month == null) month = YearMonth.now();
        return Result.success(usageAppService.getDepartmentSummary(month));
    }

    // ==================== 账单与扣减 ====================

    @Operation(summary = "月度账单列表", description = "查询指定月份的用户账单")
    @GetMapping("/billing")
    @PreAuthorize("hasAnyAuthority('finance:salary:view', 'finance:view')")
    public Result<List<AiMonthlyBillDTO>> getMonthlyBills(
            @RequestParam int year,
            @RequestParam int month) {
        return Result.success(billingAppService.getMonthlyBills(year, month));
    }

    @Operation(summary = "生成月度账单", description = "为指定月份生成所有用户的AI费用账单")
    @PostMapping("/billing/generate")
    @PreAuthorize("hasAuthority('finance:salary:manage')")
    public Result<Integer> generateMonthlyBills(
            @RequestParam int year,
            @RequestParam int month) {
        int count = billingAppService.generateMonthlyBills(year, month);
        return Result.success(count);
    }

    @Operation(summary = "关联工资扣减", description = "将AI费用账单关联到工资扣减记录")
    @PostMapping("/billing/link-salary")
    @PreAuthorize("hasAuthority('finance:salary:manage')")
    public Result<Void> linkToSalaryDeduction(@RequestBody SalaryDeductionLinkCommand command) {
        billingAppService.linkToSalaryDeduction(command);
        return Result.success();
    }

    @Operation(summary = "标记已扣减", description = "标记账单已完成工资扣减")
    @PostMapping("/billing/{id}/deduct")
    @PreAuthorize("hasAuthority('finance:salary:manage')")
    public Result<Void> markDeducted(@PathVariable Long id, @RequestParam String remark) {
        billingAppService.markDeducted(id, remark);
        return Result.success();
    }

    @Operation(summary = "减免费用", description = "减免用户的AI费用（不扣减）")
    @PostMapping("/billing/{id}/waive")
    @PreAuthorize("hasAuthority('finance:salary:manage')")
    public Result<Void> waiveBill(@PathVariable Long id, @RequestParam String reason) {
        billingAppService.waiveBill(id, reason);
        return Result.success();
    }
}
```

---

## 五、前端设计

### 5.1 我的AI使用页面

```vue
<!-- frontend/apps/web-antd/src/views/personal/ai-usage/index.vue -->
<template>
  <Page title="我的AI使用">
    <!-- 本月统计卡片 -->
    <Row :gutter="16" class="summary-cards">
      <Col :span="6">
        <Card>
          <Statistic title="本月调用次数" :value="summary.totalCalls" suffix="次" />
        </Card>
      </Col>
      <Col :span="6">
        <Card>
          <Statistic 
            title="本月消耗Token" 
            :value="summary.totalTokens" 
            :precision="0"
            suffix="tokens"
          />
          <Progress 
            v-if="summary.tokenUsagePercent" 
            :percent="summary.tokenUsagePercent" 
            :status="summary.tokenUsagePercent > 90 ? 'exception' : 'normal'"
            size="small"
          />
        </Card>
      </Col>
      <Col :span="6">
        <Card>
          <Statistic 
            title="本月费用(我承担)" 
            :value="summary.userCost" 
            :precision="2"
            prefix="¥"
          />
          <div class="cost-detail">
            <span>总费用: ¥{{ summary.totalCost }}</span>
            <span>单位承担: ¥{{ summary.companyCost }}</span>
          </div>
        </Card>
      </Col>
      <Col :span="6">
        <Card>
          <Statistic 
            title="费用配额" 
            :value="summary.monthlyCostQuota || '无限制'" 
            :precision="2"
            :prefix="summary.monthlyCostQuota ? '¥' : ''"
          />
          <Progress 
            v-if="summary.costUsagePercent" 
            :percent="summary.costUsagePercent" 
            :status="summary.costUsagePercent > 90 ? 'exception' : 'normal'"
            size="small"
          />
        </Card>
      </Col>
    </Row>

    <!-- 使用趋势图 -->
    <Card title="使用趋势" style="margin-top: 16px">
      <div ref="chartRef" style="height: 300px"></div>
    </Card>

    <!-- 模型使用分布 -->
    <Row :gutter="16" style="margin-top: 16px">
      <Col :span="12">
        <Card title="模型使用分布">
          <div ref="pieChartRef" style="height: 250px"></div>
        </Card>
      </Col>
      <Col :span="12">
        <Card title="费用明细">
          <Table 
            :columns="modelColumns" 
            :data-source="modelUsage" 
            size="small"
            :pagination="false"
          />
        </Card>
      </Col>
    </Row>

    <!-- 使用记录列表 -->
    <Card title="使用记录" style="margin-top: 16px">
      <Table
        :columns="columns"
        :data-source="usageLogs"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'success'">
            <Tag :color="record.success ? 'green' : 'red'">
              {{ record.success ? '成功' : '失败' }}
            </Tag>
          </template>
          <template v-if="column.key === 'cost'">
            <span>¥{{ record.totalCost }}</span>
            <span class="cost-breakdown">
              (我承担: ¥{{ record.userCost }})
            </span>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Page } from '@vben/common-ui';
import { Card, Row, Col, Statistic, Progress, Table, Tag } from 'ant-design-vue';
import * as echarts from 'echarts';
import { getMyUsageSummary, getMyUsageLogs, getMyUsageByModel } from '#/api/ai/usage';

// ... 实现代码
</script>
```

### 5.2 管理员统计页面

文件：`frontend/apps/web-antd/src/views/finance/ai-billing/index.vue`

提供：
- 全员AI使用统计
- 按部门统计
- 月度账单管理
- 批量导出工资扣减
- 减免操作

---

## 六、与工资系统集成

### 6.1 工资扣减流程

```
1. 月初自动生成上月账单
   └── 定时任务：每月1日凌晨运行
   
2. 财务审核账单
   ├── 查看所有用户账单
   ├── 可减免特殊情况
   └── 确认扣减金额
   
3. 关联工资扣减
   ├── 批量生成工资扣减记录
   └── 写入 hr_salary_deduction 表
   
4. 发放工资时自动扣除
   └── 工资模块读取扣减记录
```

### 6.2 扣减记录表关联

```sql
-- 在现有的工资扣减表中增加AI费用类型
INSERT INTO sys_dict_item (dict_id, label, value, sort)
SELECT id, 'AI使用费', 'AI_USAGE', 99 
FROM sys_dict WHERE code = 'salary_deduction_type';

-- AI账单关联工资扣减
ALTER TABLE public.ai_monthly_bill 
ADD COLUMN salary_deduction_id BIGINT REFERENCES hr_salary_deduction(id);
```

---

## 七、配置与部署

### 7.1 系统配置

```yaml
# application.yml
law-firm:
  ai:
    usage:
      enabled: true                    # 启用使用量记录
      async: true                      # 异步记录（推荐）
      quota-check: true                # 启用配额检查
      default-cost-ratio: 1.0          # 默认用户承担比例
    billing:
      auto-generate: true              # 自动生成月度账单
      generate-day: 1                  # 每月几号生成
      generate-hour: 2                 # 几点生成（凌晨2点）
```

### 7.2 定时任务

```java
@Scheduled(cron = "0 0 2 1 * ?")  // 每月1日凌晨2点
public void generateMonthlyBills() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    billingAppService.generateMonthlyBills(lastMonth.getYear(), lastMonth.getMonthValue());
}
```

---

## 八、权限设计

| 权限代码 | 说明 | 角色 |
|---------|------|-----|
| `ai:usage:view:my` | 查看自己的使用记录 | 所有用户 |
| `ai:usage:view` | 查看所有人的使用记录 | ADMIN, FINANCE, DIRECTOR |
| `ai:billing:view` | 查看账单 | FINANCE |
| `ai:billing:manage` | 管理账单（扣减/减免） | FINANCE |
| `ai:quota:manage` | 管理用户配额 | ADMIN |
| `ai:pricing:manage` | 管理定价配置 | ADMIN |

---

## 九、后续扩展

1. **预警通知**：用户接近配额时发送提醒
2. **费用预估**：调用前估算费用并提示用户
3. **账单导出**：支持导出Excel/PDF
4. **自助充值**：用户自行购买额度（高级功能）
5. **部门预算**：按部门设置月度预算

---

## 十、附录

### 10.1 常用模型参考定价

| 模型 | 输入价格(元/千Token) | 输出价格(元/千Token) | 生成1000字文书约 |
|-----|---------------------|---------------------|-----------------|
| DeepSeek Chat | 0.001 | 0.002 | ¥0.01 |
| DeepSeek R1 | 0.004 | 0.016 | ¥0.05 |
| 通义千问 Max | 0.012 | 0.012 | ¥0.05 |
| 智谱GLM-4 | 0.1 | 0.1 | ¥0.40 |
| GPT-4 | 0.21 | 0.42 | ¥1.50 |
| Claude 3 Opus | 0.105 | 0.525 | ¥2.00 |
| 本地部署(Ollama等) | 0 | 0 | 免费 |

*注：价格仅供参考，请以各平台官方价格为准*

### 10.2 收费比例配置示例

管理员在系统设置中配置收费比例，所有用户统一执行：

| 收费比例 | 含义 | 费用10元时用户付 |
|---------|-----|-----------------|
| **100%** | 用户全额承担 | 10元 |
| **80%** | 用户承担80% | 8元 |
| **50%** | 用户承担一半 | 5元 |
| **0%** | 单位全额承担（免费） | 0元 |

管理员可随时调整比例，调整后对新产生的费用生效。
