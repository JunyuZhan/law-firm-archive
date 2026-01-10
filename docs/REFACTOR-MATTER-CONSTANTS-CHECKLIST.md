# 项目/案件常量统一重构检查清单

## ✅ 重构完成检查

### 1. 常量类创建
- [x] 创建 `MatterConstants.java`
- [x] 包含项目大类映射（2个）
- [x] 包含案件类型映射（14个）
- [x] 包含项目状态映射（7个）
- [x] 提供静态方法：`getMatterTypeName()`, `getCaseTypeName()`, `getMatterStatusName()`

### 2. MatterAppService.java
- [x] 添加导入：`import com.lawfirm.common.constant.MatterConstants;`
- [x] 删除 `getMatterTypeName()` 方法
- [x] 删除 `getCaseTypeName()` 方法
- [x] 删除 `getStatusName()` 方法
- [x] 删除 `getMatterStatusName()` 方法
- [x] 替换 `toDTO()` 中的调用（3处）
- [x] 替换 `changeStatus()` 中的调用（1处）
- [x] 替换 `generateReport()` 中的调用（1处）
- [x] 编译通过

### 3. MatterContextCollector.java
- [x] 添加导入：`import com.lawfirm.common.constant.MatterConstants;`
- [x] 删除 `getMatterTypeName()` 方法
- [x] 删除 `getCaseTypeName()` 方法
- [x] 删除 `getStatusName()` 方法
- [x] 替换 `collectMatterContext()` 中的调用（3处）
- [x] 编译通过

### 4. ContractAppService.java
- [x] 添加导入：`import com.lawfirm.common.constant.MatterConstants;`
- [x] 删除 `getCaseTypeName()` 方法
- [x] 替换 `toDTO()` 中的调用（1处）
- [x] 替换 `toPrintDTO()` 中的调用（1处）
- [x] 编译通过

### 5. ContractNumberGenerator.java
- [x] 添加导入：`import com.lawfirm.common.constant.MatterConstants;`
- [x] 删除 `getCaseTypeName()` 方法
- [x] 替换 `previewContractNumber()` 中的调用（2处）
- [x] 替换 `getCaseTypeOptions()` 中的调用（1处）
- [x] 编译通过

### 6. AdminContractQueryService.java
- [x] 添加导入：`import com.lawfirm.common.constant.MatterConstants;`
- [x] 删除 `getCaseTypeName()` 方法（原定义不完整）
- [x] 替换 `toDTO()` 中的调用（1处）
- [x] 编译通过

### 7. 验证检查
- [x] 所有文件编译通过
- [x] 无编译错误
- [x] 无遗留的私有方法
- [x] 所有调用点已替换为 MatterConstants

## 📋 待测试功能清单

### 项目相关功能
- [ ] 创建项目时，项目大类名称显示正确
- [ ] 创建项目时，案件类型名称显示正确
- [ ] 查询项目列表时，类型和状态名称显示正确
- [ ] 修改项目状态时，状态名称显示正确
- [ ] 项目详情页显示的类型和状态名称正确
- [ ] 项目结案报告中的类型名称正确

### 合同相关功能
- [ ] 创建合同时，案件类型名称显示正确
- [ ] 合同列表查询时，案件类型名称显示正确
- [ ] 合同打印时，案件类型名称显示正确
- [ ] 合同编号预览时，案件类型名称显示正确

### 文档相关功能
- [ ] AI文书生成时，项目上下文信息中的类型和状态名称正确
- [ ] 卷宗目录初始化时，模板选择正确

### 其他功能
- [ ] 合同查询（AdminContractQueryService）时，案件类型名称显示正确
- [ ] 所有新增的案件类型（DUE_DILIGENCE, CONTRACT_REVIEW, LEGAL_OPINION等）都能正确显示名称

## 🔍 代码质量检查

- [x] 无重复代码
- [x] 命名统一规范
- [x] 常量类结构清晰
- [x] 方法注释完整
- [x] 向后兼容性保证

## 📝 文档更新

- [x] 创建重构方案文档
- [x] 创建重构总结文档
- [x] 创建检查清单文档
- [ ] 更新API文档（如需要）
- [ ] 更新开发指南（如需要）

