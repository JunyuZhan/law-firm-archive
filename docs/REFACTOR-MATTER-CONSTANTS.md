# 项目/案件常量统一重构方案

## 一、重构目标

统一后端代码中项目大类、案件类型、项目状态的名称映射，消除重复定义和不一致问题，提高代码可维护性。

## 二、现状分析

### 2.1 问题清单

#### 问题1：重复定义
以下 Service 类中都定义了相同的名称映射方法：
- `MatterAppService.getMatterTypeName()` - 完整定义
- `MatterAppService.getCaseTypeName()` - 完整定义（12个类型）
- `MatterAppService.getStatusName()` - 完整定义（7个状态）
- `MatterContextCollector.getMatterTypeName()` - 完整定义
- `MatterContextCollector.getCaseTypeName()` - **缺少3个类型**（DUE_DILIGENCE, CONTRACT_REVIEW, LEGAL_OPINION）
- `MatterContextCollector.getStatusName()` - **缺少1个状态**（PENDING_CLOSE）
- `ContractAppService.getCaseTypeName()` - **缺少3个类型**
- `ContractNumberGenerator.getCaseTypeName()` - **缺少5个类型**
- `AdminContractQueryService.getCaseTypeName()` - **定义不完整且命名不一致**

#### 问题2：命名不一致
- "知识产权案件" vs "知识产权"（部分地方简化）
- "已结案" vs "结案"
- "已暂停" vs "暂停"
- "已归档" vs "归档"

#### 问题3：类型缺失
以下类型在部分 Service 中缺失：
- `DUE_DILIGENCE`（尽职调查）
- `CONTRACT_REVIEW`（合同审查）
- `LEGAL_OPINION`（法律意见）
- `LABOR_ARBITRATION`（劳动仲裁）
- `COMMERCIAL_ARBITRATION`（商事仲裁）

### 2.2 需要重构的文件清单

#### 核心文件（必须重构）
1. `MatterAppService.java` - 项目核心服务
   - 删除：`getMatterTypeName()`, `getCaseTypeName()`, `getStatusName()`
   - 替换为：`MatterConstants.getMatterTypeName()`, `MatterConstants.getCaseTypeName()`, `MatterConstants.getMatterStatusName()`

2. `MatterContextCollector.java` - 项目上下文收集器
   - 删除：`getMatterTypeName()`, `getCaseTypeName()`, `getStatusName()`
   - 替换为：`MatterConstants` 对应方法

3. `ContractAppService.java` - 合同服务
   - 删除：`getCaseTypeName()`（仅用于项目相关）
   - 替换为：`MatterConstants.getCaseTypeName()`

4. `ContractNumberGenerator.java` - 合同编号生成器
   - 删除：`getCaseTypeName()`
   - 替换为：`MatterConstants.getCaseTypeName()`

5. `AdminContractQueryService.java` - 合同查询服务
   - 删除：`getCaseTypeName()`（当前定义不完整）
   - 替换为：`MatterConstants.getCaseTypeName()`

#### 注意事项
- **不重构**：其他模块的状态名称映射（如 Document、Task、Contract 等），因为它们可能有不同的状态值
- **仅重构**：与 Matter（项目/案件）相关的类型和状态映射

## 三、重构方案

### 3.1 统一常量类设计

已创建 `MatterConstants.java`，包含：
- `MATTER_TYPE_NAME_MAP` - 项目大类名称映射
- `CASE_TYPE_NAME_MAP` - 案件类型名称映射（13个完整类型）
- `MATTER_STATUS_NAME_MAP` - 项目状态名称映射（7个完整状态）
- 静态方法：`getMatterTypeName()`, `getCaseTypeName()`, `getMatterStatusName()`

### 3.2 统一命名规范

#### 项目大类
- `LITIGATION` → `诉讼案件`
- `NON_LITIGATION` → `非诉项目`

#### 案件类型（统一使用完整名称）
- `CIVIL` → `民事案件`
- `CRIMINAL` → `刑事案件`
- `ADMINISTRATIVE` → `行政案件`
- `BANKRUPTCY` → `破产案件`
- `IP` → `知识产权案件`（统一，不使用简化版）
- `ARBITRATION` → `仲裁案件`
- `COMMERCIAL_ARBITRATION` → `商事仲裁`
- `LABOR_ARBITRATION` → `劳动仲裁`
- `ENFORCEMENT` → `执行案件`
- `LEGAL_COUNSEL` → `法律顾问`
- `SPECIAL_SERVICE` → `专项服务`
- `DUE_DILIGENCE` → `尽职调查`
- `CONTRACT_REVIEW` → `合同审查`
- `LEGAL_OPINION` → `法律意见`

#### 项目状态（统一使用完整名称）
- `DRAFT` → `草稿`
- `PENDING` → `待审批`
- `ACTIVE` → `进行中`
- `SUSPENDED` → `已暂停`（统一，不使用简化版）
- `PENDING_CLOSE` → `待审批结案`
- `CLOSED` → `已结案`（统一，不使用简化版）
- `ARCHIVED` → `已归档`（统一，不使用简化版）

## 四、重构步骤

### 步骤1：验证常量类完整性
- [x] 创建 `MatterConstants.java`
- [x] 确认包含所有13个案件类型
- [x] 确认包含所有7个项目状态
- [x] 确认包含2个项目大类

### 步骤2：重构 MatterAppService
**文件**：`backend/src/main/java/com/lawfirm/application/matter/service/MatterAppService.java`

**操作**：
1. 在文件顶部添加导入：`import com.lawfirm.common.constant.MatterConstants;`
2. 删除以下私有方法：
   - `getMatterTypeName(String type)` (第677-684行)
   - `getCaseTypeName(String type)` (第689-706行)
   - `getStatusName(String status)` (第711-723行)
3. 替换所有调用：
   - `getMatterTypeName(...)` → `MatterConstants.getMatterTypeName(...)`
   - `getCaseTypeName(...)` → `MatterConstants.getCaseTypeName(...)`
   - `getStatusName(...)` → `MatterConstants.getMatterStatusName(...)`
4. 注意：`getMatterStatusName()` 方法名已改为 `getMatterStatusName()` 以避免与其他模块的状态方法冲突

**影响范围**：
- `toDTO()` 方法中的名称设置
- `changeStatus()` 方法中的状态名称获取
- `generateReport()` 方法中的报告生成

### 步骤3：重构 MatterContextCollector
**文件**：`backend/src/main/java/com/lawfirm/application/document/service/MatterContextCollector.java`

**操作**：
1. 添加导入：`import com.lawfirm.common.constant.MatterConstants;`
2. 删除以下私有方法：
   - `getMatterTypeName(String type)` (第307-314行)
   - `getCaseTypeName(String type)` (第316-330行)
   - `getStatusName(String status)` (第332-343行)
3. 替换所有调用：
   - `getMatterTypeName(...)` → `MatterConstants.getMatterTypeName(...)`
   - `getCaseTypeName(...)` → `MatterConstants.getCaseTypeName(...)`
   - `getStatusName(...)` → `MatterConstants.getMatterStatusName(...)`

**影响范围**：
- `collectMatterContext()` 方法中的项目信息收集

### 步骤4：重构 ContractAppService
**文件**：`backend/src/main/java/com/lawfirm/application/finance/service/ContractAppService.java`

**操作**：
1. 添加导入：`import com.lawfirm.common.constant.MatterConstants;`
2. 删除私有方法：`getCaseTypeName(String type)` (第1316-1330行)
3. 替换调用：
   - `getCaseTypeName(...)` → `MatterConstants.getCaseTypeName(...)`

**影响范围**：
- `toDTO()` 方法中的案件类型名称设置
- `toPrintDTO()` 方法中的打印DTO转换

### 步骤5：重构 ContractNumberGenerator
**文件**：`backend/src/main/java/com/lawfirm/application/finance/service/ContractNumberGenerator.java`

**操作**：
1. 添加导入：`import com.lawfirm.common.constant.MatterConstants;`
2. 删除私有方法：`getCaseTypeName(String caseType)` (第365-378行)
3. 替换调用：
   - `getCaseTypeName(...)` → `MatterConstants.getCaseTypeName(...)`

**影响范围**：
- `getCaseTypeOptions()` 方法中的选项生成
- `previewContractNumber()` 方法中的预览生成

### 步骤6：重构 AdminContractQueryService
**文件**：`backend/src/main/java/com/lawfirm/application/admin/service/AdminContractQueryService.java`

**操作**：
1. 添加导入：`import com.lawfirm.common.constant.MatterConstants;`
2. 删除私有方法：`getCaseTypeName(String caseType)` (第209-218行)
3. 替换调用：
   - `getCaseTypeName(...)` → `MatterConstants.getCaseTypeName(...)`

**影响范围**：
- `toDTO()` 方法中的案件类型名称设置

### 步骤7：验证和测试
1. 编译检查：确保所有文件编译通过
2. 单元测试：运行相关单元测试
3. 集成测试：测试项目创建、查询、状态变更等功能
4. 回归测试：确保现有功能不受影响

## 五、注意事项

### 5.1 方法命名
- 项目状态方法命名为 `getMatterStatusName()`，避免与其他模块的 `getStatusName()` 冲突
- 其他模块（Document、Task、Contract等）的状态映射保持独立，不在此次重构范围内

### 5.2 向后兼容
- 常量类的方法返回原值（如果找不到映射），确保向后兼容
- 不会影响数据库存储的值，只影响显示名称

### 5.3 特殊处理
- `ContractNumberGenerator` 中的 `CASE_TYPE_CN_MAP` 和 `CASE_TYPE_CODE_MAP` 保持不变（用于编号生成，不是名称映射）
- `MatterAppService` 中的 `getCaseTypeCode()` 方法保持不变（用于编号生成）

## 六、预期收益

1. **代码质量提升**：消除重复代码，提高可维护性
2. **一致性保证**：所有地方使用统一的名称映射
3. **易于扩展**：新增类型只需在常量类中添加一次
4. **减少错误**：避免因复制粘贴导致的不一致问题

## 七、风险评估

### 低风险
- 仅影响显示名称，不影响业务逻辑
- 方法签名保持一致（都是 String → String）
- 有完整的测试覆盖

### 注意事项
- 确保所有调用点都已替换
- 注意区分项目状态和其他模块状态
- 测试时重点关注名称显示是否正确

## 八、执行计划

1. **准备阶段**（已完成）
   - [x] 创建 `MatterConstants.java`
   - [x] 编写重构方案文档

2. **执行阶段**（已完成）
   - [x] 重构 MatterAppService
   - [x] 重构 MatterContextCollector
   - [x] 重构 ContractAppService
   - [x] 重构 ContractNumberGenerator
   - [x] 重构 AdminContractQueryService

3. **验证阶段**（部分完成）
   - [x] 编译检查（通过）
   - [ ] 单元测试（待执行）
   - [ ] 集成测试（待执行）
   - [ ] 代码审查（待执行）

## 九、回滚方案

如果重构后出现问题，可以：
1. 恢复各 Service 中的私有方法
2. 保持常量类不变（不影响其他代码）
3. 逐步迁移，不强制一次性完成

