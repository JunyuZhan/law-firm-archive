# 天眼查API集成技术设计方案

> 文档版本：v1.0  
> 创建日期：2026-01-14  
> 状态：设计中

## 一、概述

### 1.1 背景

律所管理系统需要对接天眼查API，实现以下核心功能：
1. **企业客户一键录入**：输入企业名称自动填充工商信息
2. **利益冲突检查增强**：通过股权穿透发现隐藏关联关系
3. **客户尽职调查**：查询企业司法风险、失信记录等
4. **对方当事人调查**：了解对方企业背景和财产线索

### 1.2 天眼查API简介

- 官方文档：https://open.tianyancha.com
- 认证方式：Token认证（Header: Authorization）
- 计费模式：按次计费，不同接口价格不同
- 数据更新：工商数据T+1更新，司法数据准实时

### 1.3 功能优先级

| 优先级 | 功能 | 价值 | 开发量 |
|-------|-----|------|-------|
| **P0** | 利冲检查增强 | ⭐⭐⭐⭐⭐ | 中 |
| **P0** | 企业客户一键录入 | ⭐⭐⭐⭐ | 小 |
| **P1** | 客户风险画像 | ⭐⭐⭐⭐ | 中 |
| **P1** | 股东信息同步 | ⭐⭐⭐ | 小 |
| **P2** | 对方当事人调查 | ⭐⭐⭐ | 中 |
| **P2** | 裁判文书关联 | ⭐⭐⭐ | 中 |
| **P3** | 企业变更监控 | ⭐⭐ | 大 |

---

## 二、整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端层 (Vue)                              │
├─────────────────────────────────────────────────────────────────┤
│  客户管理页面      利冲检查页面      项目详情页面      尽调报告页面   │
│  - 企业查询按钮    - 深度关联检查    - 对方信息查询    - 风险画像     │
│  - 自动填充表单    - 股权穿透展示    - 财产线索       - 一键生成报告  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      接口层 (Controller)                         │
├─────────────────────────────────────────────────────────────────┤
│  TianyanchaController                                            │
│  - POST /tianyancha/search          # 企业搜索                   │
│  - GET  /tianyancha/company/{id}    # 企业详情                   │
│  - GET  /tianyancha/shareholders    # 股东穿透                   │
│  - GET  /tianyancha/judicial        # 司法风险                   │
│  - POST /tianyancha/conflict-check  # 增强利冲检查                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     应用层 (Application)                         │
├─────────────────────────────────────────────────────────────────┤
│  TianyanchaAppService                                            │
│  - 企业信息查询与缓存                                             │
│  - 利冲检查增强（调用 ConflictCheckAppService）                   │
│  - 客户自动填充                                                   │
│  - 尽调报告生成                                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    基础设施层 (Infrastructure)                    │
├─────────────────────────────────────────────────────────────────┤
│  TianyanchaClient                                                │
│  - HTTP 调用封装                                                  │
│  - Token 认证                                                     │
│  - 限流与重试                                                     │
│  - 响应解析                                                       │
├─────────────────────────────────────────────────────────────────┤
│  缓存层 (Redis + Database)                                       │
│  - Redis: 热点数据缓存 (24h TTL)                                  │
│  - Database: 持久化存储，用于离线分析                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     天眼查开放平台 API                            │
│                   https://open.tianyancha.com                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、数据库设计

### 3.1 外部集成配置（复用现有表）

在 `sys_external_integration` 表中添加天眼查配置：

```sql
-- 新增集成类型
-- ExternalIntegration.java 中添加: TYPE_ENTERPRISE_INFO = "ENTERPRISE_INFO"

INSERT INTO public.sys_external_integration (
    integration_code, 
    integration_name, 
    integration_type, 
    description,
    api_url,
    auth_type,
    extra_config,
    enabled
) VALUES (
    'TIANYANCHA',
    '天眼查',
    'ENTERPRISE_INFO',
    '天眼查企业信息查询API，用于客户录入、利冲检查、尽职调查等场景',
    'https://open.tianyancha.com',
    'API_KEY',
    '{
        "version": "v3",
        "dailyQuota": 1000,
        "cacheHours": 24,
        "retryCount": 3,
        "timeout": 10000
    }',
    false
);
```

### 3.2 企业信息缓存表（新建）

```sql
-- 企业信息缓存表（减少API调用次数，支持离线分析）
CREATE TABLE public.enterprise_info_cache (
    id BIGSERIAL PRIMARY KEY,
    
    -- 企业标识
    credit_code VARCHAR(50),           -- 统一社会信用代码
    company_name VARCHAR(200) NOT NULL, -- 企业名称
    tyc_id VARCHAR(50),                -- 天眼查企业ID
    
    -- 基本工商信息
    legal_representative VARCHAR(100), -- 法定代表人
    registered_capital VARCHAR(50),    -- 注册资本
    establishment_date DATE,           -- 成立日期
    business_status VARCHAR(50),       -- 经营状态（存续/注销/吊销等）
    company_type VARCHAR(100),         -- 企业类型
    registered_address TEXT,           -- 注册地址
    business_scope TEXT,               -- 经营范围
    
    -- 联系信息
    phone VARCHAR(50),
    email VARCHAR(100),
    website VARCHAR(200),
    
    -- 股东信息（JSON格式）
    shareholders JSONB,
    -- 示例：[{"name":"张三","type":"PERSON","percent":"60%"},{"name":"XX公司","type":"COMPANY","percent":"40%"}]
    
    -- 实际控制人（JSON格式）
    actual_controllers JSONB,
    -- 示例：["张三","李四"]
    
    -- 对外投资（JSON格式）
    investments JSONB,
    -- 示例：[{"name":"子公司A","percent":"100%"},{"name":"参股公司B","percent":"30%"}]
    
    -- 风险摘要信息
    risk_level VARCHAR(20),            -- 风险等级: LOW/MEDIUM/HIGH
    lawsuit_count INTEGER DEFAULT 0,   -- 涉诉案件数
    executed_count INTEGER DEFAULT 0,  -- 被执行记录数
    dishonest_count INTEGER DEFAULT 0, -- 失信记录数
    abnormal_count INTEGER DEFAULT 0,  -- 经营异常数
    
    -- 元数据
    data_source VARCHAR(50) DEFAULT 'TIANYANCHA',
    fetched_at TIMESTAMP NOT NULL,     -- 数据获取时间
    expires_at TIMESTAMP NOT NULL,     -- 缓存过期时间
    raw_response JSONB,                -- 原始响应（用于排查问题）
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT uk_enterprise_credit_code UNIQUE (credit_code),
    CONSTRAINT uk_enterprise_tyc_id UNIQUE (tyc_id)
);

-- 索引
CREATE INDEX idx_enterprise_cache_name ON public.enterprise_info_cache(company_name);
CREATE INDEX idx_enterprise_cache_expires ON public.enterprise_info_cache(expires_at);
CREATE INDEX idx_enterprise_cache_legal_rep ON public.enterprise_info_cache(legal_representative);

COMMENT ON TABLE public.enterprise_info_cache IS '企业信息缓存表，存储天眼查查询结果，减少API调用';
COMMENT ON COLUMN public.enterprise_info_cache.shareholders IS '股东信息JSON数组';
COMMENT ON COLUMN public.enterprise_info_cache.actual_controllers IS '实际控制人JSON数组';
COMMENT ON COLUMN public.enterprise_info_cache.risk_level IS '风险等级：LOW-低风险，MEDIUM-中风险，HIGH-高风险';
```

### 3.3 企业关联关系表（用于利冲检查股权穿透）

```sql
-- 企业关联关系表（支持股权穿透分析）
CREATE TABLE public.enterprise_relation (
    id BIGSERIAL PRIMARY KEY,
    
    -- 主体企业
    source_credit_code VARCHAR(50),
    source_company_name VARCHAR(200) NOT NULL,
    
    -- 关联企业/自然人
    target_credit_code VARCHAR(50),    -- 企业则填信用代码，自然人为空
    target_name VARCHAR(200) NOT NULL, -- 企业名称或自然人姓名
    target_type VARCHAR(20) NOT NULL,  -- COMPANY/PERSON
    
    -- 关联关系类型
    relation_type VARCHAR(50) NOT NULL,
    -- SHAREHOLDER: 股东
    -- INVESTMENT: 对外投资
    -- LEGAL_REP: 法定代表人
    -- ACTUAL_CONTROLLER: 实际控制人
    -- EXECUTIVE: 高管
    -- HISTORICAL_SHAREHOLDER: 历史股东
    
    relation_detail VARCHAR(200),       -- 详细信息，如：持股比例、职位等
    
    -- 层级（用于穿透查询）
    depth INTEGER DEFAULT 1,            -- 关联层级，1=直接关联，2=二级关联...
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT uk_enterprise_relation UNIQUE (source_credit_code, target_name, relation_type)
);

CREATE INDEX idx_relation_source ON public.enterprise_relation(source_credit_code);
CREATE INDEX idx_relation_target ON public.enterprise_relation(target_name);
CREATE INDEX idx_relation_type ON public.enterprise_relation(relation_type);

COMMENT ON TABLE public.enterprise_relation IS '企业关联关系表，用于利冲检查的股权穿透分析';
COMMENT ON COLUMN public.enterprise_relation.depth IS '关联层级：1=直接关联（如直接股东），2=二级关联（如股东的股东）';
```

### 3.4 API调用记录表（用于统计和计费）

```sql
-- 天眼查API调用记录（用于统计、计费、问题排查）
CREATE TABLE public.tianyancha_api_log (
    id BIGSERIAL PRIMARY KEY,
    
    -- 调用信息
    api_name VARCHAR(100) NOT NULL,    -- 接口名称
    api_url VARCHAR(500),              -- 完整URL
    request_params JSONB,              -- 请求参数
    
    -- 响应信息
    response_code INTEGER,             -- HTTP状态码
    error_code VARCHAR(50),            -- 天眼查错误码
    error_message TEXT,                -- 错误信息
    success BOOLEAN DEFAULT FALSE,
    
    -- 业务关联
    business_type VARCHAR(50),         -- 业务类型：CLIENT_CREATE/CONFLICT_CHECK/DUE_DILIGENCE
    business_id BIGINT,                -- 业务ID（如客户ID、利冲检查ID）
    
    -- 调用者
    user_id BIGINT,
    user_name VARCHAR(100),
    
    -- 性能
    duration_ms INTEGER,               -- 响应时间（毫秒）
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tyc_log_api ON public.tianyancha_api_log(api_name);
CREATE INDEX idx_tyc_log_time ON public.tianyancha_api_log(created_at);
CREATE INDEX idx_tyc_log_user ON public.tianyancha_api_log(user_id);

COMMENT ON TABLE public.tianyancha_api_log IS '天眼查API调用日志，用于统计分析和问题排查';
```

---

## 四、后端代码设计

### 4.1 目录结构

```
backend/src/main/java/com/lawfirm/
├── application/
│   └── tianyancha/
│       ├── service/
│       │   └── TianyanchaAppService.java      # 应用服务
│       ├── dto/
│       │   ├── EnterpriseInfoDTO.java         # 企业信息DTO
│       │   ├── EnterpriseSearchResultDTO.java # 搜索结果DTO
│       │   ├── EnterpriseRiskDTO.java         # 风险信息DTO
│       │   ├── ShareholderDTO.java            # 股东DTO
│       │   ├── EnhancedConflictCheckResultDTO.java  # 增强利冲结果
│       │   └── ConflictItemDTO.java           # 冲突项DTO
│       └── command/
│           └── EnhancedConflictCheckCommand.java
├── domain/
│   └── tianyancha/
│       ├── entity/
│       │   ├── EnterpriseInfoCache.java       # 企业信息缓存实体
│       │   ├── EnterpriseRelation.java        # 企业关联关系实体
│       │   └── TianyanchaApiLog.java          # API调用日志实体
│       └── repository/
│           ├── EnterpriseInfoCacheRepository.java
│           ├── EnterpriseRelationRepository.java
│           └── TianyanchaApiLogRepository.java
├── infrastructure/
│   └── external/
│       └── tianyancha/
│           ├── TianyanchaClient.java          # API客户端
│           ├── TianyanchaConfig.java          # 配置类
│           └── dto/                           # 天眼查原始响应DTO
│               ├── TycCompanyInfo.java
│               ├── TycSearchResult.java
│               ├── TycShareholderList.java
│               ├── TycInvestmentList.java
│               ├── TycLawsuitList.java
│               ├── TycExecutedList.java
│               ├── TycDishonestList.java
│               └── TycActualControllerList.java
└── interfaces/
    └── rest/
        └── tianyancha/
            └── TianyanchaController.java      # REST控制器
```

### 4.2 集成类型常量扩展

```java
// 文件：ExternalIntegration.java
// 添加新的集成类型常量

public class ExternalIntegration extends BaseEntity {
    // ... 现有代码 ...
    
    // ===== 新增集成类型 =====
    /** 企业信息查询服务（天眼查、企查查等） */
    public static final String TYPE_ENTERPRISE_INFO = "ENTERPRISE_INFO";
}
```

### 4.3 天眼查API客户端

```java
package com.lawfirm.infrastructure.external.tianyancha;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.tianyancha.entity.TianyanchaApiLog;
import com.lawfirm.domain.tianyancha.repository.TianyanchaApiLogRepository;
import com.lawfirm.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 天眼查API客户端
 * 
 * 官方文档：https://open.tianyancha.com/open/1116
 * 
 * 主要接口：
 * - 企业搜索：/services/open/search/2.0
 * - 企业基本信息：/services/open/ic/baseinfo/normal
 * - 股东信息：/services/open/ic/holder/2.0
 * - 对外投资：/services/open/ic/inverst/2.0
 * - 实际控制人：/services/open/ic/actualControl/2.0
 * - 法律诉讼：/services/open/jr/lawSuit/2.0
 * - 被执行人：/services/open/jr/zhixinginfo/2.0
 * - 失信被执行人：/services/open/jr/dishonest/2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TianyanchaClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TianyanchaApiLogRepository apiLogRepository;

    // ==================== 工商信息接口 ====================

    /**
     * 企业关键字搜索
     * API: /services/open/search/2.0
     * 费用：约0.1元/次
     */
    public TycSearchResult searchCompany(ExternalIntegration config, String keyword, int pageNum, int pageSize) {
        String apiName = "search";
        String url = buildUrl(config, "/services/open/search/2.0", 
                "word", keyword, "pageNum", String.valueOf(pageNum), "pageSize", String.valueOf(pageSize));
        
        return callApi(config, apiName, url, TycSearchResult.class, "SEARCH", null);
    }

    /**
     * 企业基本信息
     * API: /services/open/ic/baseinfo/normal
     * 费用：约0.2元/次
     */
    public TycCompanyInfo getCompanyInfo(ExternalIntegration config, String companyName) {
        String apiName = "baseinfo";
        String url = buildUrl(config, "/services/open/ic/baseinfo/normal", "name", companyName);
        
        return callApi(config, apiName, url, TycCompanyInfo.class, "CLIENT_CREATE", null);
    }

    /**
     * 企业股东信息
     * API: /services/open/ic/holder/2.0
     * 费用：约0.15元/次
     */
    public TycShareholderList getShareholders(ExternalIntegration config, String companyId, int pageNum, int pageSize) {
        String apiName = "holder";
        String url = buildUrl(config, "/services/open/ic/holder/2.0",
                "id", companyId, "pageNum", String.valueOf(pageNum), "pageSize", String.valueOf(pageSize));
        
        return callApi(config, apiName, url, TycShareholderList.class, "CONFLICT_CHECK", null);
    }

    /**
     * 企业对外投资
     * API: /services/open/ic/inverst/2.0
     * 费用：约0.15元/次
     */
    public TycInvestmentList getInvestments(ExternalIntegration config, String companyId, int pageNum, int pageSize) {
        String apiName = "investment";
        String url = buildUrl(config, "/services/open/ic/inverst/2.0",
                "id", companyId, "pageNum", String.valueOf(pageNum), "pageSize", String.valueOf(pageSize));
        
        return callApi(config, apiName, url, TycInvestmentList.class, "CONFLICT_CHECK", null);
    }

    /**
     * 实际控制人
     * API: /services/open/ic/actualControl/2.0
     * 费用：约0.3元/次
     */
    public TycActualControllerList getActualControllers(ExternalIntegration config, String companyId) {
        String apiName = "actualControl";
        String url = buildUrl(config, "/services/open/ic/actualControl/2.0", "id", companyId);
        
        return callApi(config, apiName, url, TycActualControllerList.class, "CONFLICT_CHECK", null);
    }

    // ==================== 司法风险接口 ====================

    /**
     * 法律诉讼
     * API: /services/open/jr/lawSuit/2.0
     * 费用：约0.2元/次
     */
    public TycLawsuitList getLawsuits(ExternalIntegration config, String companyName, int pageNum, int pageSize) {
        String apiName = "lawSuit";
        String url = buildUrl(config, "/services/open/jr/lawSuit/2.0",
                "name", companyName, "pageNum", String.valueOf(pageNum), "pageSize", String.valueOf(pageSize));
        
        return callApi(config, apiName, url, TycLawsuitList.class, "DUE_DILIGENCE", null);
    }

    /**
     * 被执行人信息
     * API: /services/open/jr/zhixinginfo/2.0
     * 费用：约0.15元/次
     */
    public TycExecutedList getExecutedInfo(ExternalIntegration config, String companyName, int pageNum, int pageSize) {
        String apiName = "zhixinginfo";
        String url = buildUrl(config, "/services/open/jr/zhixinginfo/2.0",
                "name", companyName, "pageNum", String.valueOf(pageNum), "pageSize", String.valueOf(pageSize));
        
        return callApi(config, apiName, url, TycExecutedList.class, "DUE_DILIGENCE", null);
    }

    /**
     * 失信被执行人
     * API: /services/open/jr/dishonest/2.0
     * 费用：约0.15元/次
     */
    public TycDishonestList getDishonestInfo(ExternalIntegration config, String companyName, int pageNum, int pageSize) {
        String apiName = "dishonest";
        String url = buildUrl(config, "/services/open/jr/dishonest/2.0",
                "name", companyName, "pageNum", String.valueOf(pageNum), "pageSize", String.valueOf(pageSize));
        
        return callApi(config, apiName, url, TycDishonestList.class, "DUE_DILIGENCE", null);
    }

    // ==================== 通用方法 ====================

    /**
     * 通用API调用方法
     */
    private <T> T callApi(ExternalIntegration config, String apiName, String url, 
                          Class<T> responseType, String businessType, Long businessId) {
        long startTime = System.currentTimeMillis();
        TianyanchaApiLog apiLog = new TianyanchaApiLog();
        apiLog.setApiName(apiName);
        apiLog.setApiUrl(url);
        apiLog.setBusinessType(businessType);
        apiLog.setBusinessId(businessId);
        apiLog.setUserId(SecurityUtils.getUserId());
        apiLog.setUserName(SecurityUtils.getUsername());
        
        try {
            HttpHeaders headers = buildHeaders(config);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            
            apiLog.setResponseCode(response.getStatusCode().value());
            
            T result = parseResponse(response.getBody(), responseType, apiLog);
            
            apiLog.setSuccess(true);
            return result;
            
        } catch (Exception e) {
            apiLog.setSuccess(false);
            apiLog.setErrorMessage(e.getMessage());
            log.error("天眼查API调用失败: api={}, error={}", apiName, e.getMessage());
            throw new BusinessException("天眼查查询失败: " + e.getMessage());
            
        } finally {
            apiLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            try {
                apiLogRepository.save(apiLog);
            } catch (Exception e) {
                log.warn("保存API日志失败", e);
            }
        }
    }

    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders(ExternalIntegration config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", config.getApiKey());
        return headers;
    }

    /**
     * 构建URL
     */
    private String buildUrl(ExternalIntegration config, String path, String... params) {
        StringBuilder url = new StringBuilder(config.getApiUrl());
        url.append(path);
        
        if (params.length > 0) {
            url.append("?");
            for (int i = 0; i < params.length; i += 2) {
                if (i > 0) url.append("&");
                url.append(params[i]).append("=")
                   .append(URLEncoder.encode(params[i + 1], StandardCharsets.UTF_8));
            }
        }
        
        return url.toString();
    }

    /**
     * 解析响应
     */
    private <T> T parseResponse(String body, Class<T> clazz, TianyanchaApiLog apiLog) {
        try {
            JsonNode root = objectMapper.readTree(body);
            
            // 天眼查返回格式：{"error_code": 0, "reason": "ok", "result": {...}}
            String errorCode = root.path("error_code").asText();
            apiLog.setErrorCode(errorCode);
            
            if (!"0".equals(errorCode)) {
                String reason = root.path("reason").asText();
                apiLog.setErrorMessage(reason);
                log.error("天眼查API返回错误: errorCode={}, reason={}", errorCode, reason);
                throw new BusinessException("天眼查查询失败: " + reason);
            }
            
            JsonNode result = root.path("result");
            return objectMapper.treeToValue(result, clazz);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析天眼查响应失败", e);
            throw new BusinessException("解析天眼查响应失败: " + e.getMessage());
        }
    }
}
```

### 4.4 天眼查原始响应DTO

```java
// 文件：TycCompanyInfo.java
package com.lawfirm.infrastructure.external.tianyancha.dto;

import lombok.Data;

/**
 * 天眼查企业基本信息响应
 */
@Data
public class TycCompanyInfo {
    private String id;                    // 天眼查企业ID
    private String name;                  // 企业名称
    private String creditCode;            // 统一社会信用代码
    private String legalPersonName;       // 法定代表人
    private String regCapital;            // 注册资本
    private String estiblishTime;         // 成立日期（时间戳）
    private String regStatus;             // 经营状态
    private String companyOrgType;        // 企业类型
    private String regLocation;           // 注册地址
    private String businessScope;         // 经营范围
    private String phoneNumber;           // 联系电话
    private String email;                 // 邮箱
    private String websiteList;           // 网站
    private String industry;              // 所属行业
    private String staffNumRange;         // 人员规模
}

// 文件：TycSearchResult.java
@Data
public class TycSearchResult {
    private Integer total;
    private List<TycCompanyBrief> items;
    
    @Data
    public static class TycCompanyBrief {
        private String id;
        private String name;
        private String creditCode;
        private String legalPersonName;
        private String regStatus;
        private String estiblishTime;
    }
}

// 文件：TycShareholderList.java
@Data
public class TycShareholderList {
    private Integer total;
    private List<TycShareholder> items;
    
    @Data
    public static class TycShareholder {
        private String id;
        private String name;           // 股东名称
        private Integer type;          // 1-公司 2-人
        private String capital;        // 认缴出资额
        private String percent;        // 持股比例（如 "30.00%"）
    }
}

// 文件：TycLawsuitList.java
@Data
public class TycLawsuitList {
    private Integer total;
    private List<TycLawsuit> items;
    
    @Data
    public static class TycLawsuit {
        private String id;
        private String title;          // 案件名称
        private String caseno;         // 案号
        private String casereason;     // 案由
        private String court;          // 审理法院
        private String judgetime;      // 判决日期
        private String plaintiffs;     // 原告（可能是JSON数组字符串）
        private String defendants;     // 被告（可能是JSON数组字符串）
    }
}

// 其他DTO类似...
```

### 4.5 应用服务

```java
package com.lawfirm.application.tianyancha.service;

import com.lawfirm.application.tianyancha.dto.*;
import com.lawfirm.application.client.command.CreateClientCommand;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.tianyancha.entity.EnterpriseInfoCache;
import com.lawfirm.domain.tianyancha.entity.EnterpriseRelation;
import com.lawfirm.domain.tianyancha.repository.EnterpriseInfoCacheRepository;
import com.lawfirm.domain.tianyancha.repository.EnterpriseRelationRepository;
import com.lawfirm.application.system.service.ExternalIntegrationAppService;
import com.lawfirm.infrastructure.external.tianyancha.TianyanchaClient;
import com.lawfirm.infrastructure.external.tianyancha.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 天眼查应用服务
 * 
 * 提供以下核心功能：
 * 1. 企业信息查询（带缓存）
 * 2. 利冲检查增强（股权穿透）
 * 3. 客户信息自动填充
 * 4. 企业风险评估
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TianyanchaAppService {

    private final TianyanchaClient tianyanchaClient;
    private final ExternalIntegrationAppService integrationService;
    private final EnterpriseInfoCacheRepository cacheRepository;
    private final EnterpriseRelationRepository relationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "tyc:company:";
    private static final Duration CACHE_DURATION = Duration.ofHours(24);

    // ==================== 配置获取 ====================

    /**
     * 获取天眼查集成配置
     */
    private ExternalIntegration getConfig() {
        ExternalIntegration config = integrationService.getFirstEnabledIntegrationByType(
            ExternalIntegration.TYPE_ENTERPRISE_INFO
        );
        if (config == null) {
            throw new BusinessException("天眼查服务未配置或未启用，请在系统设置-外部集成中配置");
        }
        return integrationService.getIntegrationWithDecryptedKeys(config.getId());
    }

    /**
     * 检查天眼查服务是否可用
     */
    public boolean isServiceAvailable() {
        try {
            ExternalIntegration config = integrationService.getFirstEnabledIntegrationByType(
                ExternalIntegration.TYPE_ENTERPRISE_INFO
            );
            return config != null && config.getEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 企业信息查询 ====================

    /**
     * 企业搜索（用于下拉选择）
     */
    public EnterpriseSearchResultDTO searchCompany(String keyword, int pageNum, int pageSize) {
        ExternalIntegration config = getConfig();
        TycSearchResult result = tianyanchaClient.searchCompany(config, keyword, pageNum, pageSize);
        return convertSearchResult(result);
    }

    /**
     * 获取企业详细信息（带多级缓存）
     * 
     * 缓存策略：
     * 1. Redis缓存（24小时TTL）- 热点数据
     * 2. 数据库缓存 - 持久化，用于离线分析
     */
    public EnterpriseInfoDTO getCompanyInfo(String companyName) {
        // 1. 查Redis缓存
        String cacheKey = CACHE_PREFIX + companyName;
        EnterpriseInfoDTO cached = (EnterpriseInfoDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Redis缓存命中: {}", companyName);
            return cached;
        }

        // 2. 查数据库缓存（未过期的）
        EnterpriseInfoCache dbCache = cacheRepository.findByCompanyNameAndNotExpired(companyName, LocalDateTime.now());
        if (dbCache != null) {
            log.debug("数据库缓存命中: {}", companyName);
            EnterpriseInfoDTO dto = convertFromCache(dbCache);
            // 回填Redis
            redisTemplate.opsForValue().set(cacheKey, dto, CACHE_DURATION);
            return dto;
        }

        // 3. 调用天眼查API
        ExternalIntegration config = getConfig();
        
        TycCompanyInfo info = tianyanchaClient.getCompanyInfo(config, companyName);
        TycShareholderList shareholders = tianyanchaClient.getShareholders(config, info.getId(), 1, 50);
        TycActualControllerList controllers = tianyanchaClient.getActualControllers(config, info.getId());
        TycInvestmentList investments = tianyanchaClient.getInvestments(config, info.getId(), 1, 50);
        
        // 4. 转换DTO
        EnterpriseInfoDTO dto = convertCompanyInfo(info, shareholders, controllers, investments);
        
        // 5. 写入缓存
        redisTemplate.opsForValue().set(cacheKey, dto, CACHE_DURATION);
        saveToDatabase(dto, info, shareholders, controllers, investments);
        
        return dto;
    }

    /**
     * 获取企业风险信息
     */
    public EnterpriseRiskDTO getCompanyRisk(String companyName) {
        ExternalIntegration config = getConfig();
        
        // 并行查询多个风险维度
        TycLawsuitList lawsuits = tianyanchaClient.getLawsuits(config, companyName, 1, 20);
        TycExecutedList executed = tianyanchaClient.getExecutedInfo(config, companyName, 1, 20);
        TycDishonestList dishonest = tianyanchaClient.getDishonestInfo(config, companyName, 1, 20);
        
        return EnterpriseRiskDTO.builder()
                .companyName(companyName)
                .lawsuitCount(lawsuits.getTotal())
                .lawsuits(convertLawsuits(lawsuits.getItems()))
                .executedCount(executed.getTotal())
                .executedRecords(convertExecuted(executed.getItems()))
                .dishonestCount(dishonest.getTotal())
                .dishonestRecords(convertDishonest(dishonest.getItems()))
                .riskLevel(calculateRiskLevel(lawsuits.getTotal(), executed.getTotal(), dishonest.getTotal()))
                .build();
    }

    // ==================== 利冲检查增强 ====================

    /**
     * 增强利冲检查：通过天眼查进行深度关联分析
     * 
     * 检查维度：
     * 1. 股东交叉 - 双方是否有共同股东
     * 2. 实际控制人 - 双方是否由同一人控制
     * 3. 法定代表人 - 双方法人是否相同
     * 4. 投资关系 - 是否存在投资/被投资关系
     * 5. 历史诉讼 - 双方是否有历史诉讼关系
     */
    public EnhancedConflictCheckResultDTO enhancedConflictCheck(
            String clientName, 
            String opposingPartyName,
            boolean deepCheck) {
        
        EnhancedConflictCheckResultDTO result = new EnhancedConflictCheckResultDTO();
        result.setClientName(clientName);
        result.setOpposingPartyName(opposingPartyName);
        result.setCheckTime(LocalDateTime.now());
        
        List<ConflictItemDTO> conflicts = new ArrayList<>();
        
        // 1. 获取双方企业信息
        EnterpriseInfoDTO clientInfo = null;
        EnterpriseInfoDTO opposingInfo = null;
        
        try {
            clientInfo = getCompanyInfo(clientName);
            result.setClientInfo(clientInfo);
        } catch (Exception e) {
            log.warn("查询委托人企业信息失败: {} - {}", clientName, e.getMessage());
            result.setClientQueryError("未找到企业信息或查询失败");
        }
        
        try {
            opposingInfo = getCompanyInfo(opposingPartyName);
            result.setOpposingPartyInfo(opposingInfo);
        } catch (Exception e) {
            log.warn("查询对方当事人企业信息失败: {} - {}", opposingPartyName, e.getMessage());
            result.setOpposingQueryError("未找到企业信息或查询失败");
        }
        
        // 如果双方都查询到了企业信息，进行关联检查
        if (clientInfo != null && opposingInfo != null) {
            // 2. 检查法定代表人
            if (clientInfo.getLegalRepresentative() != null && 
                clientInfo.getLegalRepresentative().equals(opposingInfo.getLegalRepresentative())) {
                conflicts.add(ConflictItemDTO.builder()
                        .conflictType("LEGAL_REP")
                        .severity("HIGH")
                        .description("【严重冲突】双方法定代表人相同：" + clientInfo.getLegalRepresentative())
                        .relatedEntity(clientInfo.getLegalRepresentative())
                        .build());
            }
            
            // 3. 检查股东交叉
            conflicts.addAll(checkShareholderConflict(clientInfo, opposingInfo));
            
            // 4. 检查实际控制人
            conflicts.addAll(checkActualControllerConflict(clientInfo, opposingInfo));
            
            // 5. 检查投资关系
            conflicts.addAll(checkInvestmentRelation(clientInfo, opposingInfo));
        }
        
        // 6. 深度检查：历史诉讼关系
        if (deepCheck) {
            conflicts.addAll(checkHistoricalLitigation(clientName, opposingPartyName));
        }
        
        result.setConflicts(conflicts);
        result.setHasConflict(!conflicts.isEmpty());
        result.setRiskLevel(calculateConflictRiskLevel(conflicts));
        result.setRecommendation(generateRecommendation(conflicts));
        
        log.info("增强利冲检查完成: 委托人={}, 对方={}, 发现冲突数={}", 
                clientName, opposingPartyName, conflicts.size());
        
        return result;
    }

    /**
     * 检查股东交叉
     */
    private List<ConflictItemDTO> checkShareholderConflict(
            EnterpriseInfoDTO client, EnterpriseInfoDTO opposing) {
        
        List<ConflictItemDTO> conflicts = new ArrayList<>();
        
        if (client.getShareholders() == null || opposing.getShareholders() == null) {
            return conflicts;
        }
        
        // 构建委托人股东名称集合
        Map<String, ShareholderDTO> clientShareholderMap = client.getShareholders().stream()
                .collect(Collectors.toMap(ShareholderDTO::getName, s -> s, (a, b) -> a));
        
        // 检查对方股东是否与委托人股东重叠
        for (ShareholderDTO opposingSh : opposing.getShareholders()) {
            ShareholderDTO clientSh = clientShareholderMap.get(opposingSh.getName());
            if (clientSh != null) {
                String severity = "MEDIUM";
                // 如果是大股东（持股>20%），提升严重程度
                if (parsePercent(opposingSh.getPercent()) > 20 || parsePercent(clientSh.getPercent()) > 20) {
                    severity = "HIGH";
                }
                
                conflicts.add(ConflictItemDTO.builder()
                        .conflictType("SHAREHOLDER_CROSS")
                        .severity(severity)
                        .description(String.format("共同股东「%s」：在委托人处持股%s，在对方处持股%s",
                                opposingSh.getName(), clientSh.getPercent(), opposingSh.getPercent()))
                        .relatedEntity(opposingSh.getName())
                        .build());
            }
        }
        
        return conflicts;
    }

    /**
     * 检查实际控制人冲突
     */
    private List<ConflictItemDTO> checkActualControllerConflict(
            EnterpriseInfoDTO client, EnterpriseInfoDTO opposing) {
        
        List<ConflictItemDTO> conflicts = new ArrayList<>();
        
        if (client.getActualControllers() == null || opposing.getActualControllers() == null) {
            return conflicts;
        }
        
        Set<String> clientControllers = new HashSet<>(client.getActualControllers());
        
        for (String opposingController : opposing.getActualControllers()) {
            if (clientControllers.contains(opposingController)) {
                conflicts.add(ConflictItemDTO.builder()
                        .conflictType("ACTUAL_CONTROLLER")
                        .severity("HIGH")
                        .description("【严重冲突】双方存在共同实际控制人：" + opposingController)
                        .relatedEntity(opposingController)
                        .build());
            }
        }
        
        return conflicts;
    }

    /**
     * 检查投资关系
     */
    private List<ConflictItemDTO> checkInvestmentRelation(
            EnterpriseInfoDTO client, EnterpriseInfoDTO opposing) {
        
        List<ConflictItemDTO> conflicts = new ArrayList<>();
        
        // 检查委托人是否投资了对方
        if (client.getInvestments() != null) {
            for (InvestmentDTO inv : client.getInvestments()) {
                if (inv.getCompanyName().contains(opposing.getCompanyName()) ||
                    opposing.getCompanyName().contains(inv.getCompanyName())) {
                    conflicts.add(ConflictItemDTO.builder()
                            .conflictType("INVESTMENT")
                            .severity("HIGH")
                            .description("委托人投资了对方当事人：持股" + inv.getPercent())
                            .relatedEntity(inv.getCompanyName())
                            .build());
                }
            }
        }
        
        // 检查对方是否投资了委托人
        if (opposing.getInvestments() != null) {
            for (InvestmentDTO inv : opposing.getInvestments()) {
                if (inv.getCompanyName().contains(client.getCompanyName()) ||
                    client.getCompanyName().contains(inv.getCompanyName())) {
                    conflicts.add(ConflictItemDTO.builder()
                            .conflictType("INVESTMENT")
                            .severity("HIGH")
                            .description("对方当事人投资了委托人：持股" + inv.getPercent())
                            .relatedEntity(inv.getCompanyName())
                            .build());
                }
            }
        }
        
        return conflicts;
    }

    /**
     * 检查历史诉讼关系
     */
    private List<ConflictItemDTO> checkHistoricalLitigation(String clientName, String opposingName) {
        List<ConflictItemDTO> conflicts = new ArrayList<>();
        
        try {
            ExternalIntegration config = getConfig();
            TycLawsuitList lawsuits = tianyanchaClient.getLawsuits(config, clientName, 1, 100);
            
            for (TycLawsuit lawsuit : lawsuits.getItems()) {
                // 检查对方是否出现在案件的原告或被告中
                boolean inPlaintiffs = lawsuit.getPlaintiffs() != null && 
                                       lawsuit.getPlaintiffs().contains(opposingName);
                boolean inDefendants = lawsuit.getDefendants() != null && 
                                       lawsuit.getDefendants().contains(opposingName);
                
                if (inPlaintiffs || inDefendants) {
                    String role = inPlaintiffs ? "原告" : "被告";
                    conflicts.add(ConflictItemDTO.builder()
                            .conflictType("HISTORICAL_LITIGATION")
                            .severity("MEDIUM")
                            .description(String.format("历史诉讼：%s（案号：%s，对方作为%s）",
                                    lawsuit.getTitle(), lawsuit.getCaseno(), role))
                            .relatedEntity(lawsuit.getCaseno())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("查询历史诉讼失败: {}", e.getMessage());
        }
        
        return conflicts;
    }

    // ==================== 客户自动填充 ====================

    /**
     * 将天眼查数据填充到客户创建命令
     */
    public CreateClientCommand fillClientFromTianyancha(String companyName) {
        EnterpriseInfoDTO info = getCompanyInfo(companyName);
        
        CreateClientCommand command = new CreateClientCommand();
        command.setName(info.getCompanyName());
        command.setClientType("ENTERPRISE");
        command.setCreditCode(info.getCreditCode());
        command.setLegalRepresentative(info.getLegalRepresentative());
        command.setRegisteredCapital(info.getRegisteredCapital());
        command.setEstablishmentDate(info.getEstablishmentDate());
        command.setAddress(info.getRegisteredAddress());
        command.setBusinessScope(info.getBusinessScope());
        command.setPhone(info.getPhone());
        command.setEmail(info.getEmail());
        
        // 风险提示
        if (info.getBusinessStatus() != null && !"存续".equals(info.getBusinessStatus())) {
            command.setRemark("【注意】该企业状态为：" + info.getBusinessStatus());
        }
        
        return command;
    }

    // ==================== 辅助方法 ====================

    private double parsePercent(String percent) {
        if (percent == null) return 0;
        try {
            return Double.parseDouble(percent.replace("%", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String calculateRiskLevel(Integer lawsuits, Integer executed, Integer dishonest) {
        int score = 0;
        if (lawsuits != null) score += Math.min(lawsuits * 2, 20);
        if (executed != null) score += Math.min(executed * 5, 30);
        if (dishonest != null) score += dishonest * 20;
        
        if (score >= 50 || (dishonest != null && dishonest > 0)) return "HIGH";
        if (score >= 20) return "MEDIUM";
        return "LOW";
    }

    private String calculateConflictRiskLevel(List<ConflictItemDTO> conflicts) {
        if (conflicts.isEmpty()) return "NONE";
        
        long highCount = conflicts.stream().filter(c -> "HIGH".equals(c.getSeverity())).count();
        if (highCount > 0) return "HIGH";
        
        long mediumCount = conflicts.stream().filter(c -> "MEDIUM".equals(c.getSeverity())).count();
        if (mediumCount > 0) return "MEDIUM";
        
        return "LOW";
    }

    private String generateRecommendation(List<ConflictItemDTO> conflicts) {
        if (conflicts.isEmpty()) {
            return "未发现明显利益冲突，建议继续进行常规利冲检查流程。";
        }
        
        long highCount = conflicts.stream().filter(c -> "HIGH".equals(c.getSeverity())).count();
        if (highCount > 0) {
            return "发现严重利益冲突！建议拒绝该委托或提交合伙人会议审议。";
        }
        
        return "发现潜在利益冲突，建议进一步核实后提交主管合伙人审批。";
    }

    // ... 数据转换方法省略，实际实现时需要补充 ...
}
```

### 4.6 REST控制器

```java
package com.lawfirm.interfaces.rest.tianyancha;

import com.lawfirm.application.tianyancha.dto.*;
import com.lawfirm.application.tianyancha.service.TianyanchaAppService;
import com.lawfirm.application.client.command.CreateClientCommand;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "天眼查集成", description = "企业信息查询、利冲检查增强、尽职调查")
@RestController
@RequestMapping("/tianyancha")
@RequiredArgsConstructor
public class TianyanchaController {

    private final TianyanchaAppService tianyanchaService;

    @Operation(summary = "检查服务状态", description = "检查天眼查服务是否已配置并启用")
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> checkStatus() {
        return Result.success(tianyanchaService.isServiceAvailable());
    }

    @Operation(summary = "搜索企业", description = "根据关键词搜索企业，用于下拉选择")
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public Result<EnterpriseSearchResultDTO> searchCompany(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(tianyanchaService.searchCompany(keyword, pageNum, pageSize));
    }

    @Operation(summary = "获取企业详情", description = "获取企业详细信息，包括股东、实控人等")
    @GetMapping("/company")
    @PreAuthorize("isAuthenticated()")
    public Result<EnterpriseInfoDTO> getCompanyInfo(
            @Parameter(description = "企业名称") @RequestParam String name) {
        return Result.success(tianyanchaService.getCompanyInfo(name));
    }

    @Operation(summary = "获取企业风险信息", description = "获取企业涉诉、被执行、失信等风险信息")
    @GetMapping("/risk")
    @PreAuthorize("isAuthenticated()")
    public Result<EnterpriseRiskDTO> getCompanyRisk(
            @Parameter(description = "企业名称") @RequestParam String name) {
        return Result.success(tianyanchaService.getCompanyRisk(name));
    }

    @Operation(summary = "增强利冲检查", description = "通过天眼查进行深度关联分析，发现隐藏的利益冲突")
    @PostMapping("/conflict-check")
    @PreAuthorize("hasAnyAuthority('conflict:create', 'conflict:check')")
    public Result<EnhancedConflictCheckResultDTO> enhancedConflictCheck(
            @RequestBody EnhancedConflictCheckCommand command) {
        return Result.success(tianyanchaService.enhancedConflictCheck(
                command.getClientName(),
                command.getOpposingPartyName(),
                command.isDeepCheck()
        ));
    }

    @Operation(summary = "自动填充客户信息", description = "根据企业名称从天眼查获取信息，自动填充客户创建表单")
    @GetMapping("/fill-client")
    @PreAuthorize("hasAuthority('client:create')")
    public Result<CreateClientCommand> fillClientFromTianyancha(
            @Parameter(description = "企业名称") @RequestParam String companyName) {
        return Result.success(tianyanchaService.fillClientFromTianyancha(companyName));
    }
}
```

---

## 五、前端设计

### 5.1 API接口定义

文件：`frontend/apps/web-antd/src/api/tianyancha/index.ts`

```typescript
import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface EnterpriseSearchResult {
  total: number;
  items: EnterpriseSearchItem[];
}

export interface EnterpriseSearchItem {
  id: string;
  name: string;
  creditCode: string;
  legalPersonName: string;
  regStatus: string;
  establishTime: string;
}

export interface EnterpriseInfo {
  companyName: string;
  creditCode: string;
  tycId: string;
  legalRepresentative: string;
  registeredCapital: string;
  establishmentDate: string;
  businessStatus: string;
  companyType: string;
  registeredAddress: string;
  businessScope: string;
  phone: string;
  email: string;
  website: string;
  shareholders: Shareholder[];
  actualControllers: string[];
  investments: Investment[];
}

export interface Shareholder {
  name: string;
  type: 'COMPANY' | 'PERSON';
  percent: string;
  capital: string;
}

export interface Investment {
  companyName: string;
  creditCode: string;
  percent: string;
  regStatus: string;
}

export interface EnterpriseRisk {
  companyName: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  lawsuitCount: number;
  executedCount: number;
  dishonestCount: number;
  lawsuits: LawsuitRecord[];
  executedRecords: ExecutedRecord[];
  dishonestRecords: DishonestRecord[];
}

export interface LawsuitRecord {
  title: string;
  caseNo: string;
  caseReason: string;
  court: string;
  judgeTime: string;
}

export interface ExecutedRecord {
  caseCode: string;
  execCourtName: string;
  execMoney: string;
  caseCreateTime: string;
}

export interface DishonestRecord {
  caseCode: string;
  courtName: string;
  performance: string;
  disruptTypeName: string;
  publishDate: string;
}

export interface EnhancedConflictCheckResult {
  clientName: string;
  opposingPartyName: string;
  hasConflict: boolean;
  riskLevel: 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH';
  conflicts: ConflictItem[];
  clientInfo?: EnterpriseInfo;
  opposingPartyInfo?: EnterpriseInfo;
  clientQueryError?: string;
  opposingQueryError?: string;
  recommendation: string;
  checkTime: string;
}

export interface ConflictItem {
  conflictType: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  description: string;
  relatedEntity?: string;
}

export interface EnhancedConflictCheckCommand {
  clientName: string;
  opposingPartyName: string;
  deepCheck?: boolean;
}

// ==================== API方法 ====================

/** 检查天眼查服务状态 */
export function checkTianyanchaStatus() {
  return requestClient.get<boolean>('/tianyancha/status');
}

/** 搜索企业 */
export function searchCompany(keyword: string, pageNum = 1, pageSize = 10) {
  return requestClient.get<EnterpriseSearchResult>('/tianyancha/search', {
    params: { keyword, pageNum, pageSize },
  });
}

/** 获取企业详情 */
export function getCompanyInfo(name: string) {
  return requestClient.get<EnterpriseInfo>('/tianyancha/company', {
    params: { name },
  });
}

/** 获取企业风险信息 */
export function getCompanyRisk(name: string) {
  return requestClient.get<EnterpriseRisk>('/tianyancha/risk', {
    params: { name },
  });
}

/** 增强利冲检查 */
export function enhancedConflictCheck(data: EnhancedConflictCheckCommand) {
  return requestClient.post<EnhancedConflictCheckResult>('/tianyancha/conflict-check', data);
}

/** 从天眼查填充客户信息 */
export function fillClientFromTianyancha(companyName: string) {
  return requestClient.get('/tianyancha/fill-client', {
    params: { companyName },
  });
}
```

### 5.2 企业搜索组件

文件：`frontend/apps/web-antd/src/components/EnterpriseSearch/index.vue`

```vue
<template>
  <div class="enterprise-search">
    <!-- 搜索框 -->
    <Select
      v-model:value="selectedCompany"
      show-search
      :placeholder="placeholder"
      :filter-option="false"
      :loading="loading"
      :options="options"
      :disabled="!serviceAvailable"
      @search="handleSearch"
      @change="handleChange"
      style="width: 100%"
    >
      <template #notFoundContent>
        <div v-if="loading" style="text-align: center; padding: 8px">
          <Spin size="small" />
          <span style="margin-left: 8px">搜索中...</span>
        </div>
        <Empty v-else :description="searchKeyword ? '未找到匹配企业' : '请输入企业名称搜索'" />
      </template>
      
      <template #option="{ label, creditCode, legalPerson, status }">
        <div class="company-option">
          <div class="company-name">{{ label }}</div>
          <div class="company-meta">
            <span class="credit-code">{{ creditCode }}</span>
            <span class="legal-person">法人：{{ legalPerson }}</span>
            <Tag :color="status === '存续' ? 'green' : 'orange'" size="small">
              {{ status }}
            </Tag>
          </div>
        </div>
      </template>
    </Select>
    
    <!-- 服务不可用提示 -->
    <Alert
      v-if="!serviceAvailable && showServiceAlert"
      type="warning"
      message="天眼查服务未配置"
      description="请在系统设置-外部集成中配置天眼查API"
      show-icon
      closable
      style="margin-top: 8px"
      @close="showServiceAlert = false"
    />
    
    <!-- 企业详情预览 -->
    <Card v-if="companyInfo && showPreview" class="company-preview" size="small">
      <template #title>
        <Space>
          <span>{{ companyInfo.companyName }}</span>
          <Tag :color="statusColor">{{ companyInfo.businessStatus }}</Tag>
        </Space>
      </template>
      <template #extra>
        <Button type="link" size="small" @click="showPreview = false">收起</Button>
      </template>
      
      <Descriptions :column="2" size="small">
        <Descriptions.Item label="统一社会信用代码">
          {{ companyInfo.creditCode }}
        </Descriptions.Item>
        <Descriptions.Item label="法定代表人">
          {{ companyInfo.legalRepresentative }}
        </Descriptions.Item>
        <Descriptions.Item label="注册资本">
          {{ companyInfo.registeredCapital }}
        </Descriptions.Item>
        <Descriptions.Item label="成立日期">
          {{ companyInfo.establishmentDate }}
        </Descriptions.Item>
        <Descriptions.Item label="注册地址" :span="2">
          {{ companyInfo.registeredAddress }}
        </Descriptions.Item>
      </Descriptions>
      
      <!-- 风险提示 -->
      <Alert
        v-if="riskInfo"
        :type="riskAlertType"
        :message="riskMessage"
        show-icon
        style="margin-top: 12px"
      >
        <template #description v-if="riskInfo.riskLevel !== 'LOW'">
          <Space direction="vertical" size="small">
            <span v-if="riskInfo.lawsuitCount > 0">📋 涉诉案件：{{ riskInfo.lawsuitCount }}条</span>
            <span v-if="riskInfo.executedCount > 0">⚠️ 被执行记录：{{ riskInfo.executedCount }}条</span>
            <span v-if="riskInfo.dishonestCount > 0">🚫 失信记录：{{ riskInfo.dishonestCount }}条</span>
          </Space>
        </template>
      </Alert>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Select, Tag, Card, Descriptions, Alert, Button, Space, Spin, Empty } from 'ant-design-vue';
import { debounce } from 'lodash-es';
import { 
  searchCompany, 
  getCompanyInfo, 
  getCompanyRisk, 
  checkTianyanchaStatus,
  type EnterpriseInfo, 
  type EnterpriseRisk 
} from '#/api/tianyancha';

interface Props {
  placeholder?: string;
  autoFetchRisk?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '输入企业名称搜索（天眼查）',
  autoFetchRisk: true,
});

const emit = defineEmits<{
  (e: 'select', info: EnterpriseInfo): void;
  (e: 'change', companyName: string | undefined): void;
}>();

// 状态
const loading = ref(false);
const serviceAvailable = ref(true);
const showServiceAlert = ref(false);
const selectedCompany = ref<string>();
const searchKeyword = ref('');
const options = ref<any[]>([]);
const companyInfo = ref<EnterpriseInfo | null>(null);
const riskInfo = ref<EnterpriseRisk | null>(null);
const showPreview = ref(true);

// 计算属性
const statusColor = computed(() => {
  if (!companyInfo.value) return 'default';
  return companyInfo.value.businessStatus === '存续' ? 'green' : 'red';
});

const riskAlertType = computed(() => {
  if (!riskInfo.value) return 'info';
  switch (riskInfo.value.riskLevel) {
    case 'HIGH': return 'error';
    case 'MEDIUM': return 'warning';
    default: return 'success';
  }
});

const riskMessage = computed(() => {
  if (!riskInfo.value) return '';
  switch (riskInfo.value.riskLevel) {
    case 'HIGH': return '⚠️ 高风险企业';
    case 'MEDIUM': return '⚡ 存在风险提示';
    default: return '✅ 暂无明显风险';
  }
});

// 检查服务状态
onMounted(async () => {
  try {
    serviceAvailable.value = await checkTianyanchaStatus();
    if (!serviceAvailable.value) {
      showServiceAlert.value = true;
    }
  } catch {
    serviceAvailable.value = false;
    showServiceAlert.value = true;
  }
});

// 搜索企业
const handleSearch = debounce(async (keyword: string) => {
  searchKeyword.value = keyword;
  
  if (!keyword || keyword.length < 2) {
    options.value = [];
    return;
  }
  
  loading.value = true;
  try {
    const result = await searchCompany(keyword);
    options.value = result.items.map(item => ({
      value: item.name,
      label: item.name,
      creditCode: item.creditCode,
      legalPerson: item.legalPersonName,
      status: item.regStatus,
    }));
  } catch (e: any) {
    console.error('搜索企业失败', e);
  } finally {
    loading.value = false;
  }
}, 300);

// 选择企业
const handleChange = async (companyName: string | undefined) => {
  emit('change', companyName);
  
  if (!companyName) {
    companyInfo.value = null;
    riskInfo.value = null;
    return;
  }
  
  loading.value = true;
  showPreview.value = true;
  
  try {
    // 获取企业详情
    const info = await getCompanyInfo(companyName);
    companyInfo.value = info;
    emit('select', info);
    
    // 获取风险信息
    if (props.autoFetchRisk) {
      const risk = await getCompanyRisk(companyName);
      riskInfo.value = risk;
    }
  } catch (e: any) {
    console.error('获取企业信息失败', e);
  } finally {
    loading.value = false;
  }
};

// 暴露方法
defineExpose({
  getCompanyInfo: () => companyInfo.value,
  getRiskInfo: () => riskInfo.value,
  clear: () => {
    selectedCompany.value = undefined;
    companyInfo.value = null;
    riskInfo.value = null;
  },
});
</script>

<style scoped>
.company-option {
  padding: 4px 0;
}
.company-name {
  font-weight: 500;
  color: #1a1a1a;
}
.company-meta {
  font-size: 12px;
  color: #666;
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 2px;
}
.credit-code {
  font-family: monospace;
}
.company-preview {
  margin-top: 12px;
}
</style>
```

### 5.3 页面集成示例

#### 客户创建页面集成

```vue
<!-- 在客户创建表单中添加天眼查搜索 -->
<FormItem label="企业名称" name="name" v-if="formData.clientType === 'ENTERPRISE'">
  <EnterpriseSearch
    @select="handleEnterpriseSelect"
    placeholder="输入企业名称，从天眼查自动填充信息"
  />
</FormItem>

<script setup>
import EnterpriseSearch from '#/components/EnterpriseSearch/index.vue';

function handleEnterpriseSelect(info) {
  // 自动填充表单
  formData.name = info.companyName;
  formData.creditCode = info.creditCode;
  formData.legalRepresentative = info.legalRepresentative;
  formData.registeredCapital = info.registeredCapital;
  formData.establishmentDate = info.establishmentDate;
  formData.address = info.registeredAddress;
  formData.phone = info.phone;
  formData.email = info.email;
  
  message.success('已自动填充企业信息');
}
</script>
```

#### 利冲检查页面集成

详见单独的增强利冲检查组件设计。

---

## 六、配置与部署

### 6.1 系统配置

在系统设置-外部集成页面配置天眼查：

1. 访问 **系统设置 > 外部集成**
2. 找到"天眼查"配置项（或点击"新建"创建）
3. 填写配置：
   - API地址：`https://open.tianyancha.com`
   - API密钥：从天眼查开放平台获取的Token
4. 点击"测试连接"验证配置
5. 启用集成

### 6.2 天眼查开放平台配置

1. 注册天眼查开放平台账号：https://open.tianyancha.com
2. 创建应用，获取Token
3. 根据需求购买API调用次数
4. 将Token配置到系统中

### 6.3 费用预估

| 接口 | 单价（参考） | 每月预估调用 | 月费用 |
|-----|------------|------------|-------|
| 企业搜索 | ¥0.1/次 | 500次 | ¥50 |
| 企业基本信息 | ¥0.2/次 | 300次 | ¥60 |
| 股东信息 | ¥0.15/次 | 200次 | ¥30 |
| 实际控制人 | ¥0.3/次 | 100次 | ¥30 |
| 法律诉讼 | ¥0.2/次 | 100次 | ¥20 |
| 被执行人 | ¥0.15/次 | 50次 | ¥7.5 |
| 失信被执行人 | ¥0.15/次 | 50次 | ¥7.5 |
| **合计** | | | **约¥200-300/月** |

*注：实际价格以天眼查官方为准，此处仅为参考*

---

## 七、后续扩展

### 7.1 可扩展功能

1. **企业变更监控**：定期检查关注企业的工商变更、诉讼新增
2. **批量尽调报告**：一键生成多个企业的尽调报告PDF
3. **关联图谱可视化**：以图形方式展示企业股权关系
4. **个人信息查询**：对接天眼查个人信息接口（需额外授权）

### 7.2 其他数据源扩展

同样的架构可以扩展对接：
- **企查查**：https://open.qcc.com
- **启信宝**：https://api.qixin.com
- **裁判文书网**（如获得授权）

通过 `ExternalIntegration.TYPE_ENTERPRISE_INFO` 类型可以配置多个数据源，系统自动选择启用的进行查询。

---

## 八、附录

### 8.1 天眼查API错误码

| 错误码 | 说明 | 处理方式 |
|-------|-----|---------|
| 0 | 成功 | - |
| 300000 | 参数错误 | 检查请求参数 |
| 300001 | Token无效 | 检查API密钥配置 |
| 300002 | 权限不足 | 联系天眼查开通权限 |
| 300003 | 余额不足 | 充值 |
| 300004 | 频率超限 | 增加请求间隔 |

### 8.2 相关文档

- [天眼查开放平台文档](https://open.tianyancha.com/open/1116)
- [项目外部集成设计](./BACKEND_IMPLEMENTATION_GUIDE.md)
- [利冲检查模块说明](./CLIENT_MODULE_TEST_REPORT.md)
