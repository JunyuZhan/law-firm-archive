# 国家赔偿案件接入方案深度评估

> 评估日期: 2026-01-17
> 评估依据: 《中华人民共和国国家赔偿法》+ 实际法律程序 + 现有系统架构

---

## 一、核心发现：方案存在重大缺陷 ⚠️

### 1.1 关键问题

原方案将 **"国家赔偿"** 视为单一案件类型，但根据国家赔偿法：

> 国家赔偿分为 **行政赔偿** 和 **刑事赔偿**，两者的程序、救济途径、审理机构完全不同！

| 维度 | 行政赔偿 | 刑事赔偿 |
|------|----------|----------|
| **前置条件** | 无 | 必须 **刑事诉讼终结** |
| **复议性质** | 行政复议 | 刑事赔偿复议 |
| **最终救济** | **行政诉讼** | **法院赔偿委员会** |
| **审理程序** | 普通行政审判 | 赔偿委员会决定（非诉讼） |
| **可否调解** | 可以 | 不适用 |

### 1.2 系统设计影响

原方案设计中的 `compensation_track` 字段试图区分"行政赔偿/刑事赔偿"，但：

1. **案件类型层面混淆**：如果用同一个 `caseType=STATE_COMPENSATION`，用户选择后仍需再次选择是行政还是刑事
2. **代理阶段字典冲突**：行政赔偿和刑事赔偿的阶段完全不同
3. **文书模板冲突**：两类案件的申请书、决定书模板完全不同

---

## 二、重新设计：三种方案对比

### 方案 1：单一案件类型（原方案 B）❌ 不推荐

```
caseType = "STATE_COMPENSATION"
扩展表 compensation_track = "ADMINISTRATIVE" | "CRIMINAL"
```

**问题**：
- 用户无法直观区分行政/刑事赔偿
- 代理阶段字典需要根据 track 动态加载，复杂度高
- 统计时需要 JOIN 扩展表才能区分

---

### 方案 2：行政/刑事分别设置 ⭐ 推荐

```
行政案件 → caseType = "ADMINISTRATIVE" → businessType = "STATE_COMPENSATION"
刑事案件 → caseType = "CRIMINAL" → businessType = "STATE_COMPENSATION"
```

**优点**：
- 复用现有案件类型结构
- 行政/刑事各自的代理阶段字典已存在
- 统计时可通过 `businessType` 筛选

**问题**：
- "国家赔偿"在案件类型层面不可见
- 需要教育用户选择"行政案件+国家赔偿"或"刑事案件+国家赔偿"

---

### 方案 3：新增两个独立案件类型 ⭐⭐ 最推荐

```
caseType = "ADMINISTRATIVE_COMPENSATION"  // 行政国家赔偿
caseType = "CRIMINAL_COMPENSATION"       // 刑事国家赔偿
或者
caseType = "STATE_COMPENSATION_ADMIN"    // 行政赔偿
caseType = "STATE_COMPENSATION_CRIMINAL" // 刑事赔偿
```

**优点**：
- 用户界面清晰，可直接选择
- 各自有独立的代理阶段字典
- 统计、权限、合同编号都清晰

**缺点**：
- 开发工作量略大（多一个类型）

---

## 三、数据模型重新设计

### 3.1 扩展表字段调整

原方案设计的字段需要调整，增加 **刑事赔偿特有字段**：

| 字段名 | 原设计 | 调整建议 | 原因 |
|--------|--------|----------|------|
| `compensation_track` | 行政/刑事/司法/其他 | **保留**，但建议改为字典 | 核心区分字段 |
| `criminal_case_terminated` | - | **新增** | 刑事赔偿必填：刑事诉讼是否终结 |
| `criminal_case_id` | - | **新增** | 关联原刑事案件 ID |
| `criminal_case_no` | - | **新增** | 原刑事案件编号 |
| `compensation_committee` | - | **新增** | 赔偿委员会（刑事赔偿用） |
| `administrative_litigation` | - | **新增** | 是否提起行政赔偿诉讼 |
| `application_reason` | - | **新增** | 申请赔偿理由/事实依据 |
| `evidence_list` | - | **新增** | 证据清单（JSONB） |

### 3.2 修正后的扩展表设计

```sql
CREATE TABLE matter_state_compensation (
    matter_id BIGINT PRIMARY KEY REFERENCES matter(id) ON DELETE CASCADE,

    -- 核心区分
    compensation_track VARCHAR(50) NOT NULL,  -- 字典：ADMINISTRATIVE_COMPENSATION/CRIMINAL_COMPENSATION

    -- 赔偿义务机关
    obligor_org_name VARCHAR(255),
    obligor_org_type VARCHAR(50),             -- 字典：PUBLIC_SECURITY/PROCURATORATE/COURT/PRISON/ADMIN_ORGAN

    -- 致损行为
    case_source VARCHAR(50),                  -- 字典：违法拘留/违法强制措施/错误判决/违法查封等
    damage_description TEXT,                  -- 损害情况描述

    -- 刑事赔偿特有字段
    criminal_case_terminated BOOLEAN,         -- 刑事诉讼是否终结
    criminal_case_no VARCHAR(100),            -- 原刑事案件编号
    compensation_committee VARCHAR(255),      -- 受理的赔偿委员会

    -- 程序日期（与主表 filingDate 不冲突）
    application_date DATE,                    -- 赔偿申请日（2年时效）
    acceptance_date DATE,                     -- 受理日
    decision_date DATE,                       -- 赔偿义务机关决定日（2个月期限）
    reconsideration_date DATE,                -- 复议/复核申请日（30日期限）
    reconsideration_decision_date DATE,       -- 复议决定日
    committee_app_date DATE,                  -- 赔偿委员会申请日
    committee_decision_date DATE,             -- 赔偿委员会决定日
    admin_litigation_filing_date DATE,        -- 行政赔偿诉讼立案日
    admin_litigation_court_name VARCHAR(255), -- 行政诉讼法院

    -- 赔偿请求
    claim_amount DECIMAL(18,2),               -- 请求赔偿总额（可与主表 claimAmount 同步）
    compensation_items JSONB,                 -- 赔偿项目明细
    -- JSONB 结构示例：
    -- [
    --   {"type": "人身自由赔偿", "days": 100, "amount": 43689},
    --   {"type": "精神损害抚慰金", "amount": 50000},
    --   {"type": "财产损害赔偿", "amount": 100000}
    -- ]

    -- 决定结果
    decision_result VARCHAR(50),              -- 字典：GRANTED/DENIED/PARTIAL_GRANTED
    approved_amount DECIMAL(18,2),            -- 决定赔偿金额
    payment_status VARCHAR(50),               -- 字典：UNPAID/PAID/PARTIAL_PAID
    payment_date DATE,

    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_msc_compensation_track ON matter_state_compensation(compensation_track);
CREATE INDEX idx_msc_obligor_org ON matter_state_compensation(obligor_org_type);
CREATE INDEX idx_msc_decision_date ON matter_state_compensation(decision_date);
```

---

## 四、代理阶段字典重新设计

### 4.1 问题发现

原方案的代理阶段设计混淆了行政/刑事赔偿的不同程序：

```
原设计：
- 赔偿申请
- 赔偿审查/决定    ← 这个名称不专业
- 复议/复核        ← 行政复议和刑事复议是不同性质
- 一审             ← 刑事赔偿没有"一审"
```

### 4.2 修正设计

#### 行政赔偿代理阶段

```
dict_code: litigation_stage_admin_compensation

├── COMPENSATION_APPLICATION     -- 赔偿申请（向赔偿义务机关）
├── COMPENSATION_DECISION        -- 赔偿决定（赔偿义务机关2个月内）
├── ADMIN_RECONSIDERATION        -- 行政复议（向复议机关）
├── ADMIN_LITIGATION             -- 行政赔偿诉讼（向法院）
├── FIRST_INSTANCE               -- 一审
├── SECOND_INSTANCE              -- 二审
└── ENFORCEMENT                  -- 执行
```

#### 刑事赔偿代理阶段

```
dict_code: litigation_stage_criminal_compensation

├── CRIMINAL_TERMINATION         -- 刑事诉讼终结确认
├── COMPENSATION_APPLICATION     -- 赔偿申请（向赔偿义务机关）
├── COMPENSATION_DECISION        -- 赔偿决定（赔偿义务机关2个月内）
├── CRIMINAL_REVIEW             -- 复议（向上一级机关）
├── COMPENSATION_COMMITTEE      -- 赔偿委员会（同级法院）
├── COMMITTEE_REVIEW            -- 上级赔偿委员会
└── PAYMENT                      -- 支付赔偿金
```

---

## 五、开发方案修正

### 5.1 推荐方案（方案 3 修正版）

**采用新增两个独立案件类型**：

| 案件类型代码 | 案件类型名称 | matterType | 适用场景 |
|-------------|-------------|-----------|----------|
| `STATE_COMP_ADMIN` | 行政国家赔偿 | LITIGATION | 行政机关及其工作人员侵权 |
| `STATE_COMP_CRIMINAL` | 刑事国家赔偿 | LITIGATION | 司法机关（公安/检察/法院/监狱）刑事诉讼侵权 |

**代码映射**：

```java
// MatterConstants.java
CASE_TYPE_NAME_MAP.put("STATE_COMP_ADMIN", "行政国家赔偿");
CASE_TYPE_NAME_MAP.put("STATE_COMP_CRIMINAL", "刑事国家赔偿");
```

```typescript
// useCauseOfAction.ts
{
  value: 'STATE_COMP_ADMIN',
  label: '行政国家赔偿',
  matterType: 'LITIGATION',
  hasCause: false,
  causeType: null,
},
{
  value: 'STATE_COMP_CRIMINAL',
  label: '刑事国家赔偿',
  matterType: 'LITIGATION',
  hasCause: false,
  causeType: null,
}
```

```java
// ContractNumberGenerator.java
CASE_TYPE_CN_MAP.put("STATE_COMP_ADMIN", "行赔");
CASE_TYPE_CN_MAP.put("STATE_COMP_CRIMINAL", "刑赔");
CASE_TYPE_CODE_MAP.put("STATE_COMP_ADMIN", "XPS");
CASE_TYPE_CODE_MAP.put("STATE_COMP_CRIMINAL", "XRS");
```

### 5.2 代理阶段字典映射

```java
// 前端：useCauseOfAction.ts 或类似的映射逻辑
const STAGE_DICT_MAP = {
  'CIVIL': 'litigation_stage_civil',
  'CRIMINAL': 'litigation_stage_criminal',
  'ADMINISTRATIVE': 'litigation_stage_administrative',
  'STATE_COMP_ADMIN': 'litigation_stage_state_comp_admin',      // 新增
  'STATE_COMP_CRIMINAL': 'litigation_stage_state_comp_criminal', // 新增
  // ...
};
```

### 5.3 扩展表与案件类型关联

由于有两个案件类型，扩展表的 `compensation_track` 可以从案件类型推断：

```java
// 服务层逻辑
public MatterStateCompensation getOrInitExtension(Long matterId, String caseType) {
    String track = switch (caseType) {
        case "STATE_COMP_ADMIN" -> "ADMINISTRATIVE_COMPENSATION";
        case "STATE_COMP_CRIMINAL" -> "CRIMINAL_COMPENSATION";
        default -> throw new IllegalArgumentException("非国家赔偿案件类型");
    };
    // ...
}
```

---

## 六、实施计划（修正版）

### 阶段 1：基础数据（1 天）

| 任务 | 说明 |
|------|------|
| 新增案件类型常量 | `STATE_COMP_ADMIN`、`STATE_COMP_CRIMINAL` |
| 新增代理阶段字典 | 两个独立的字典类型 |
| 合同编号映射 | "行赔"/"XPS"、"刑赔"/"XRS" |
| 模板变量映射 | 两个类型的中文全称 |

### 阶段 2：扩展表开发（2 天）

| 任务 | 说明 |
|------|------|
| 建表 | 包含刑事赔偿特有字段 |
| 实体类 | MatterStateCompensation |
| Repository/Service | CRUD 接口 |
| Controller | RESTful API |

### 阶段 3：前端表单（2 天）

| 任务 | 说明 |
|------|------|
| 案件类型选项 | 新增两个选项 |
| 扩展信息表单 | 根据案件类型动态显示字段 |
| 案件详情展示 | 显示扩展信息 |

### 阶段 4：文书与测试（1 天）

| 任务 | 说明 |
|------|------|
| 文书变量测试 | 验证变量渲染正确 |
| 流程测试 | 完整创建-编辑-归档流程 |

**总工作量：约 6 人日**

---

## 七、与原方案的差异对比

| 项目 | 原方案 | 修正方案 | 影响 |
|------|--------|----------|------|
| 案件类型 | 1 个 | 2 个 | 更清晰，区分行政/刑事 |
| 代理阶段字典 | 1 套 | 2 套独立 | 符合实际法律程序 |
| 扩展表字段 | 8 个 | 15+ 个 | 覆盖刑事赔偿特有场景 |
| 合同编号简称 | "赔"/"PS" | "行赔"/"XPS"、"刑赔"/"XRS" | 可区分行政/刑事 |

---

## 八、最终评估

| 评估项 | 原方案 | 修正方案 |
|--------|--------|----------|
| **法律准确性** | ⚠️ 混淆行政/刑事程序 | ✅ 符合国家赔偿法 |
| **用户体验** | ⚠️ 需二次选择赔偿类型 | ✅ 直接选择案件类型 |
| **统计清晰度** | ⚠️ 需 JOIN 扩展表 | ✅ 直接按 caseType 分组 |
| **开发工作量** | 3-5 人日 | 5-6 人日 |
| **长期维护** | ⚠️ 容易混淆 | ✅ 结构清晰 |

---

## 九、建议

**推荐采用修正方案（方案 3）**：

1. 新增 `STATE_COMP_ADMIN` 和 `STATE_COMP_CRIMINAL` 两个独立案件类型
2. 扩展表增加刑事赔偿特有字段
3. 两套独立的代理阶段字典
4. 合同编号使用"行赔"/"刑赔"区分

**理由**：
- 符合国家赔偿法的法律架构
- 用户体验清晰
- 便于后续扩展和统计
- 开发成本增加不多（约 1-2 人日）

---

**Sources:**
- [中华人民共和国国家赔偿法](http://szrzyhghj.zaozhuang.gov.cn/yhyshj/wjtz/zrzybhxgbwwj/202411/t20241101_1962585.html)
- [申请国家赔偿办理流程图 - 淮南市人民检察院](https://www.huainandt.jcy.gov.cn/jwgk/gzlc/201905/t20190520_2573148.shtml)
- [行政赔偿和刑事赔偿在程序上的区别 - 萤火法务](https://www.yinghuolaw.com/knowledge/62320/)
- [普法小课堂：行政赔偿和刑事赔偿之间的异同](https://m.thepaper.cn/newsDetail_forward_10601199)
