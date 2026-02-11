# 律师事务所管理系统 - 待办事项

## 📋 待处理任务

### 1. 模板菜单位置调整

**问题描述**：合同模板和函件模板目前放在"系统管理"下，但这两个属于业务模板，不应该在系统管理中。

**当前状态**：
| 模板 | 菜单ID | 当前父菜单 | 当前路径 |
|-----|--------|-----------|---------|
| 出函模板 | 27 | 系统管理 (ID=2) | /system/letter-template |
| 合同模板 | 28 | 系统管理 (ID=2) | /system/contract-template |

**调整方案**（推荐方案一：分散到各业务模块）：
| 模板 | 目标父菜单 | 目标路径 | 理由 |
|-----|-----------|---------|------|
| 合同模板 | 财务管理 (ID=5) | /finance/contract-template | 委托合同与收费强相关 |
| 出函模板 | 行政管理 (ID=9) | /admin/letter-template | 出函审批是行政流程 |

**权限分析**（已验证）：
| 父菜单 | 有权限的角色 |
|--------|-------------|
| 财务管理 (ID=5) | 管理员(1), 律所主任(2), 团队负责人(3), 财务(5), 律师(6), 行政(8), 实习律师(9) |
| 行政管理 (ID=9) | 管理员(1), 律所主任(2), 团队负责人(3), 财务(5), 律师(6), 行政(8), 实习律师(9) |

| 模板菜单 | 当前权限 | 移动后 |
|---------|---------|-------|
| 出函模板 (ID=27) | 管理员(1), 律所主任(2), 行政(8) | ✅ 父菜单(9)包含这些角色 |
| 合同模板 (ID=28) | 管理员(1), 律所主任(2), 财务(5), 行政(8) | ✅ 父菜单(5)包含这些角色 |

**结论**：移动后权限不会丢失，sys_role_menu 记录无需修改。

**涉及改动**：
1. **数据库** - 更新 sys_menu 表（前端路由从后端菜单动态生成，无需改前端路由文件）：
   - 修改菜单 27: parent_id: 2→9, path: /system/letter-template→/admin/letter-template, component: system/...→admin/...
   - 修改菜单 28: parent_id: 2→5, path: /system/contract-template→/finance/contract-template, component: system/...→finance/...
2. **前端组件** - 移动组件目录以匹配新的 component 路径：
   - `views/system/letter-template/` → `views/admin/letter-template/` (6个文件)
   - `views/system/contract-template/` → `views/finance/contract-template/` (7个文件)
3. **初始化脚本** - 更新 `scripts/init-db/20-init-data.sql` 中的 INSERT 语句

**备选方案**（方案二：模板中心）：
- 创建新的一级菜单"模板中心"
- 缺点：改动较大，系统已成熟不建议大改

**实施进度**：
- [x] 移动前端组件目录
- [x] 更新初始化脚本 `scripts/init-db/20-init-data.sql`
- [x] 数据库迁移脚本已应用并清理
- [ ] 部署到服务器

**状态**：🔄 待部署测试

---

### 2. 函件模板示例数据

**问题描述**：函件模板表 (letter_template) 当前为空，需要添加示例数据方便用户快速上手。

**已添加的模板**：
| ID | 模板编号 | 名称 | 类型 |
|----|---------|------|------|
| 1 | LT-001 | 律师介绍信（通用） | INTRODUCTION |
| 2 | LT-002 | 会见函（看守所） | MEETING |
| 3 | LT-003 | 调查函（通用） | INVESTIGATION |
| 4 | LT-004 | 阅卷函（检察院/法院） | FILE_REVIEW |
| 5 | LT-005 | 法律意见函（通用） | LEGAL_OPINION |

**实施进度**：
- [x] 在 `scripts/init-db/20-init-data.sql` 中添加 INSERT 语句
- [x] 在迁移脚本中添加（供服务器执行）
- [ ] 部署到服务器

**状态**：🔄 待部署测试

---

### 3. 客户服务面板自动刷新功能

**问题描述**：项目详情页的"客户服务"Tab 中，访问记录、下载记录、客户文件需要手动刷新才能看到最新数据。

**需求**：实现自动刷新机制（轮询或 WebSocket）

**涉及文件**：
- `frontend/apps/web-antd/src/components/ClientServicePanel/index.vue`
- 代码中已有 TODO 注释标记

**状态**：⏳ 待实施（优先级低）

---

### 4. 安全漏洞修复（高优先级）

**问题描述**：代码审查发现多处安全漏洞，需要立即修复。

#### 4.1 后端安全问题

| 风险 | 问题 | 位置 | 修复方案 |
|------|------|------|----------|
| ✅ | 硬编码密钥 | `DocumentController.java:76`<br>`OnlyOfficeService.java:424` | 已改用 @Value 从配置注入 |
| ✅ | 路径遍历 | `VersionController.java:175` | 已添加 UUID 格式正则验证 |
| ✅ | ThreadLocal 泄漏 | `StatisticsAppService.java`<br>`ContractDataPermissionService.java` | 已添加 ThreadLocalCleanupFilter |
| 🟡中 | 日志记录 Token | `DocumentController.java:1046` | 仅记录 token hash 或移除 |
| 🟡中 | IP 验证宽松 | `DocumentController.java:1086` | 使用 CIDR 验证库 |

#### 4.2 前端安全问题

| 风险 | 问题 | 位置 | 修复方案 |
|------|------|------|----------|
| ✅ | refreshToken 存 localStorage | `store/auth.ts:86`<br>`api/request.ts:59` | 已改用 sessionStorage（更安全）|
| ✅ | v-html XSS | 6 处文件 | 已添加 sanitizeHtml 包裹 |
| 🟡中 | 路由守卫异步错误 | `router/guard.ts:106-110` | 添加 try-catch 包裹 |
| 🟡中 | 全局 Promise 错误 | `main.ts` | 添加 unhandledrejection 监听 |

**实施进度**：
- [x] 后端：硬编码密钥改为配置（使用环境变量 DOCUMENT_TOKEN_SECRET）
- [x] 后端：VersionController 路径遍历修复（添加 upgradeId 格式验证）
- [x] 后端：ThreadLocal 清理 Filter（新增 ThreadLocalCleanupFilter）
- [x] 前端：refreshToken 存储方式优化（localStorage → sessionStorage）
- [x] 前端：v-html XSS 防护（安装 DOMPurify，添加 sanitizeHtml 工具）
- [x] 前端：路由守卫错误处理（添加 try-catch 和全局错误处理）

**状态**：✅ 已修复

---

### 5. 代码质量问题修复

**问题描述**：代码审查发现多处潜在 Bug 和代码质量问题。

#### 5.1 后端问题

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 数组越界风险 | `LlmClient.java` | 已有 isEmpty() 检查后再 get(0) |
| ✅ | 空指针风险 | `LlmClient.java` 多处 | 已有 body == null 检查 |
| ✅ | 空指针风险 | `PayrollController.java:284-287` | 已有 null 检查并返回 404 |
| 🟡中 | 调试代码残留 | `ContractController.java:121-125,462-467` | 移除 `System.out.println` |
| 🟡中 | 异常被吞掉 | `SysConfigController.java:288`<br>`AuthController.java:344`<br>`VersionController.java:368`<br>`MatterClientFileController.java:169`<br>`AiUsageRecorder.java:395` | 空 catch 块添加日志记录 |
| 🟡中 | 文件名编码 | `PayrollController.java:292` | 使用 `URLEncoder.encode` |

#### 5.2 前端问题

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 内存泄漏 | `EvidenceUploader.vue:52-61` | 已在 finally 中 clearInterval |
| ✅ | Blob URL 泄漏 | `office-preview/index.vue` | 已添加定时器跟踪和 onUnmounted 清理 |
| ✅ | FileReader 无错误处理 | `RichTextEditor/index.vue` | 已添加 error 事件监听 |
| ✅ | 未处理 Promise | `ClientServicePanel/index.vue:776-786` | 已使用 Promise.allSettled 和错误日志 |
| 🟡中 | 类型安全 | `EvidenceUploader.vue:62-64` | 避免使用 `any`，定义明确类型 |
| 🟡中 | 事件监听泄漏 | `document/list/index.vue:1532-1558` | 使用单例 input 或清理监听器 |
| 🟡中 | 错误无用户提示 | `ClientServicePanel/index.vue` 多处 | 添加 `message.error` 提示 |

**实施进度**：
- [x] 后端：LlmClient 数组/空指针检查（添加数组为空检查和响应体判空）
- [x] 后端：PayrollController sheet 判空（先查询再导出，修复 NPE）
- [x] 后端：移除调试代码 System.out.println
- [ ] 后端：空 catch 块添加日志（中优先级）
- [x] 前端：EvidenceUploader setInterval 清理（移到 finally 块）
- [x] 前端：office-preview Blob URL 释放（添加窗口关闭检测）
- [x] 前端：RichTextEditor 错误处理（添加 error 事件监听）
- [x] 前端：ClientServicePanel watch 错误处理（使用 Promise.allSettled）

**状态**：✅ 高优先级已修复

---

### 6. XSS 防护补充

**问题描述**：之前修复了 3 处 v-html XSS 问题，还有 3 处遗漏。

| 文件 | 状态 |
|------|------|
| `system/config/index.vue` | ✅ 已修复 |
| `ContractPreviewModal.vue` | ✅ 已修复 |
| `document/.../PreviewModal.vue` | ✅ 已修复 |

**实施进度**：
- [x] 添加 sanitizeHtml 调用

**状态**：✅ 已修复

---

### 7. TypeScript 错误修复

**问题描述**：ClientServicePanel 存在 TypeScript 编译错误。

| 文件 | 行号 | 问题 |
|------|------|------|
| `ClientServicePanel/index.vue` | 459 | `match[1]` 可能为 undefined |

**修复方案**：使用可选链 `match?.[1]?.toLowerCase() ?? ''`

**实施进度**：
- [x] 修复 TypeScript 错误

**状态**：✅ 已修复

---

### 8. 资源泄漏与参数校验问题

**问题描述**：代码审查发现资源管理和参数校验问题。

#### 8.1 后端资源泄漏

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | InputStream 未关闭 | `MinioService.java:171` | 已使用 try-with-resources 关闭流 |
| ✅ | Workbook 未关闭 | `ExcelImportExportService.java:177` | 已使用 try-with-resources 包裹 Workbook |
| ✅ | Process 无超时 | `VersionController.java` | 已添加 waitFor(timeout) 和 destroyForcibly() |
| 🔄 | Workbook 未 try-with-resources | `ExcelReportGenerator.java` (13处) | 部分修复（generateRevenueReport），需继续修复 |
| 🟡中 | PDF 资源未 try-with-resources | `PdfReportGenerator.java` | 使用 try-finally 确保关闭 |
| ✅ | HttpURLConnection 未在异常时关闭 | `ExternalIntegrationAppService.java` | 已添加 try-finally disconnect() |

#### 8.2 后端参数校验

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | ids 未校验 | `CommissionController.java:294-298,327-330` | 已添加 @Size 限制 |
| ✅ | 批量删除无数量限制 | `UserController.BatchDeleteRequest` | 已添加 @Size(max=100) |
| ✅ | 批量删除无数量限制 | `ClientController.BatchDeleteRequest` | 已添加 @Size(max=100) |
| ✅ | 批量审批无数量限制 | `ApprovalController.BatchApproveRequest` | 已添加 @NotEmpty + @Size(max=100) |
| 🟡中 | pageSize 无上限 | `PageQuery.java:37-39` | 添加 pageSize 最大值限制 |
| 🟡中 | 日期格式未校验 | `CommissionController.java:253-256` | 使用 @DateTimeFormat 或 LocalDate |

#### 8.3 后端并发问题

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | 锁粒度过大 | `LetterAppService.java:967-970` | synchronized(this) 改为按 matterId 加锁 |

#### 8.4 前端定时器/异步清理

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 轮询定时器未清理 | `system/config/index.vue` | onUnmounted 中已调用 stopUpgradePolling() |
| ✅ | input 监听器未清理 | `document/list/index.vue:1536-1558` | 已在 change 后移除监听器 |
| ✅ | JSON.parse 无 try-catch | `crm/client/index.vue:392-393` | 已包裹 try-catch |
| ✅ | watch 异步无 catch | `matter/contract/index.vue:1208` | 已添加 .catch() |
| ✅ | FileReader addEventListener 未移除 | `RichTextEditor/index.vue:160-167` | 已改用 onload/onerror 赋值 |
| ✅ | FileReader addEventListener 未移除 | `adapter/component/index.ts:203-207` | 已改用 onload/onerror 赋值 |
| ✅ | Blob URL 未释放 | `office-preview/index.vue:414-424` | 已在窗口关闭时调用 revokeObjectURL |
| 🟡中 | searchTimer 未清理 | `MatterSelector.vue:143-154` | onUnmounted 中 clearTimeout |
| 🟡中 | setTimeout 未清理 | `finance/report/index.vue:327-332` | onBeforeUnmount 中 clearTimeout |
| 🟡中 | setTimeout 未清理 | `workbench/report/index.vue:451-454` | onBeforeUnmount 中 clearTimeout |

**实施进度**：
- [x] 后端：MinioService InputStream 关闭（使用 try-with-resources）
- [x] 后端：ExcelImportExportService Workbook 关闭（使用 try-with-resources）
- [x] 后端：CommissionController ids 校验（已添加 @Size）
- [x] 前端：system/config 轮询定时器清理（添加 onUnmounted）
- [ ] 前端：document/list 异步回调检查
- [x] 前端：crm/client JSON.parse 错误处理（添加 try-catch）

**状态**：🔄 部分修复

---

### 9. 分页与参数处理问题

**问题描述**：分页参数未统一校验，可能导致 NPE 或性能问题。

#### 9.1 分页参数 NPE 风险

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | pageNum/pageSize 可能为 null | `DataHandoverService.java` | 已添加 getSafePageNum/getSafePageSize |
| ✅ | getOffset() 未判空 | `DataHandoverQueryDTO.java` | 已添加 null 检查和默认值 |
| 🟡中 | 未使用 PageUtils | `ScheduledReportAppService.java:95`<br>`CustomReportAppService.java:64` | 改用 PageUtils.createPage() |

#### 9.2 分页 pageSize 无上限

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | pageSize 可传超大值 | `DocumentController.java:346-347` | 已添加 @Min(1) @Max(100) |
| ✅ | 日期范围未校验 | `TimesheetController.java:194-196` | 已添加 endDate >= startDate 校验 |
| ✅ | 日期范围未校验 | `OvertimeController.java:88-90` | 已添加 endDate >= startDate 校验 |
| ✅ | API 返回未安全访问 | `hr/payroll/index.vue:188` | 已改为 res?.list?.length |

**实施进度**：
- [x] DataHandoverService/DTO 分页参数判空（添加 getSafePageNum/getSafePageSize）
- [x] DocumentController pageSize 添加 @Max(100) 限制
- [x] 日期范围参数校验（TimesheetController, OvertimeController）
- [x] 前端安全访问 API 返回（payroll/index.vue）
- [ ] 分页逻辑统一使用 PageUtils.createPage

**状态**：🔄 大部分修复

---

### 10. 前端状态管理与竞态问题

**问题描述**：组件间通信和异步请求存在竞态风险。

#### 10.1 watch 无限循环风险

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | formData ↔ initialData ↔ emit 循环 | `StateCompensationForm.vue:219-241` | 已添加标志位区分来源 |
| ✅ | blocks ↔ modelValue ↔ emit 循环 | `StructuredTemplateEditor.vue:127-144` | 已添加 isUpdatingFromParent 标志 |
| ✅ | blocks ↔ modelValue ↔ emit 循环 | `PowerOfAttorneyEditor.vue:140-157` | 已添加 isUpdatingFromParent 标志 |
| ✅ | blocks ↔ modelValue ↔ emit 循环 | `StructuredLetterEditor.vue:132-149` | 已添加 isUpdatingFromParent 标志 |

#### 10.2 异步请求竞态

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | 并发调用重复请求 | `useCauseOfAction.ts:54-108` | 使用 Promise 缓存正在进行的请求 |
| 🟡中 | 快速操作覆盖数据 | `HandoverDetailModal.vue:81,121` | 使用 AbortController 或请求版本号 |

**实施进度**：
- [x] StateCompensationForm watch 循环修复
- [x] StructuredTemplateEditor watch 循环修复
- [x] PowerOfAttorneyEditor watch 循环修复
- [x] StructuredLetterEditor watch 循环修复
- [ ] useCauseOfAction 并发请求去重

**状态**：🔄 高优先级已修复

---

### 11. 文件处理安全问题

**问题描述**：文件上传、下载、解压等操作存在安全和兼容性问题。

#### 11.1 文件上传验证

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 图片上传无校验 | `RichTextEditor/index.vue:136-146` | 已添加文件类型、大小、扩展名校验 |
| 🟡中 | 上传类型不限制 | `EvidenceUploader.vue:95-103` | 添加 accept 属性限制文件类型 |

#### 11.2 路径遍历（Zip Slip）

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | Zip 解压路径遍历 | `BackupAppService.java:1148-1160` | 已有路径校验和规范化 |
| ✅ | AI 文档文件名路径遍历 | `AiDocumentService.java:295-304` | 已使用 MinioPathGenerator.sanitizeFilename |
| ✅ | 客户文件名路径遍历 | `ClientFileService.java:208-210` | 已使用 MinioPathGenerator.sanitizeFilename |
| 🟡中 | MinioService 路径参数未校验 | `MinioService.java:311` | 调用方需确保参数安全 |

#### 11.3 临时文件清理

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | 升级状态文件未删除 | `VersionController.java:175,297,357` | 升级完成后删除 /tmp/.upgrade-status-*.json |

#### 11.4 下载文件名编码

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | 中文文件名乱码 | `ClientController.java:229`<br>`EvidenceController.java:552`<br>`BackupController.java:133` | 使用 RFC 5987 的 `filename*=UTF-8''` 格式 |

**实施进度**：
- [ ] RichTextEditor 图片上传校验
- [x] BackupAppService Zip Slip 修复（添加路径验证）
- [ ] 下载文件名 RFC 5987 编码

**状态**：🔄 部分修复

---

### 12. 异常处理与信息泄露

**问题描述**：异常信息直接暴露给前端，存在安全风险。

#### 12.1 异常信息泄露

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | e.getMessage() 暴露给前端 | 多个 Controller | 已检查，仅用于日志，未返回给前端 |

#### 12.2 资源清理缺失

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | InputStream 未关闭 | `EvidenceController.java:295`<br>`DocumentAppService.java:605` | 已使用 try-with-resources |
| 🟡中 | InputStream 未关闭 | `FileAccessService.java:68` | 使用 try-with-resources 管理流 |
| ✅ | InputStream 未关闭 | `TaskCommentAppService.java`<br>`SealApplicationAppService.java` | 已使用 try-with-resources |

#### 12.3 事务回滚

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | checked exception 不回滚 | 多个 @Transactional 方法 | UserAppService/DocumentAppService 已添加 rollbackFor |

**实施进度**：
- [ ] 异常信息泄露修复
- [ ] InputStream 资源管理修复

**状态**：⏳ 待修复

---

### 13. 权限与认证安全问题

**问题描述**：部分接口权限检查缺失，存在越权风险。

#### 13.1 权限检查缺失

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 无权限即可修改配置 | `VersionController.java` | 已有 @PreAuthorize('sys:config:edit') |
| ✅ | 任意用户可读配置 | `SysConfigController.java` | 已有 @RequirePermission('sys:config:list') |
| 🟡中 | 缺少细粒度权限 | `WorkbenchController.java:37-127` | 添加 @RequirePermission 注解 |

#### 13.2 回调接口安全

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | OnlyOffice 回调无校验 | `DocumentController.java:768` | 需配合 OnlyOffice JWT 配置（复杂改动）|

#### 13.3 敏感操作

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | 删除用户无二次确认 | `UserController.java:130-136,144-150` | 考虑添加密码确认或操作确认 |
| 🟡中 | 重置密码无二次确认 | `UserController.java:158-166` | 添加操作确认机制 |

**实施进度**：
- [x] VersionController /ignore 权限修复（添加 @PreAuthorize）
- [x] SysConfigController 配置接口权限修复（添加 @RequirePermission）
- [ ] OnlyOffice 回调安全校验

**状态**：🔄 部分修复

---

### 14. 数据库与事务问题

**问题描述**：事务边界不当和批量操作性能问题。

#### 14.1 事务边界问题

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | 事务内执行 HTTP 调用 | `DataPushService.java:98-198` | 将 HTTP 调用移到事务外或改为异步 |
| 🟡中 | getOrCreateConfig 无事务 | `DataPushService.java:261-276` | 添加 @Transactional 和唯一约束处理 |

#### 14.2 批量操作性能

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | 循环内逐条 insert | `DataHandoverService.java:779-935,973` | 改为 saveBatch 批量插入 |
| 🟡中 | 循环内逐条 update | `DataHandoverService.java:501-513` | 改为批量更新 |

#### 14.3 内存分页问题

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 全表查询+内存分页 | `ExpenseMapper.java` | 已添加 LIMIT/OFFSET 和 count 方法 |
| ✅ | 全表查询+内存分页 | `LeadMapper.java` | 已添加 LIMIT/OFFSET 和 count 方法 |

**实施进度**：
- [x] ExpenseMapper SQL 分页（添加 LIMIT/OFFSET 和 count 方法）
- [x] LeadMapper SQL 分页（添加 LIMIT/OFFSET 和 count 方法）
- [ ] DataHandoverService 批量操作优化

**状态**：🔄 部分修复

---

### 15. 输入验证问题（后端）

**问题描述**：大量接口参数未做校验，存在安全风险。

#### 15.1 高风险问题

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | List 参数无大小限制 | `CommissionController.java:295,329`<br>`EvidenceController.java:177`<br>`TimesheetController.java:136` | 已添加 @Size(max=100) |
| ✅ | AlertWebhook 无签名校验 | `AlertWebhookController.java` | 已添加 Bearer Token 认证 |
| 🟡中 | @PathVariable Long 未校验正数 | 多个 Controller | 添加 @Positive 或 @Min(1) |
| 🟡中 | pageSize 无上限 | `DocumentController.java:346`<br>`NotificationController.java:44` | 添加 @Max(100) |

**实施进度**：
- [x] 批量接口 List 参数添加 @Size
- [ ] 分页参数添加 @Max 限制

**状态**：🔄 部分修复

---

### 16. 前端生命周期问题

**问题描述**：定时器和事件监听未在组件卸载时清理。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | searchTimer 未清理 | `MatterSelector.vue:151` | 已添加 onUnmounted 中 clearTimeout |
| ✅ | input 事件监听未清理 | `document/list/index.vue:1536` | 已在 change 后移除监听器 |
| 🟡中 | printWindow load 未清理 | `EvidenceListDisplay.vue:685` | 添加卸载检查 |
| ✅ | setTimeout 未清理 | `finance/report/index.vue:327`<br>`workbench/report/index.vue:451` | 已添加 onBeforeUnmount 清理 |

**实施进度**：
- [x] MatterSelector searchTimer 清理
- [x] finance/report setTimeout 清理
- [x] workbench/report setTimeout 清理

**状态**：🔄 部分修复

---

### 17. 异常信息泄露问题（后端）

**问题描述**：`e.getMessage()` 直接返回给前端，可能泄露系统内部信息。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 异常消息直接暴露 | `EvidenceController.java:335,465`<br>`DocumentController.java:328,802,1172`<br>`VersionController.java:191,367` | 已改为通用错误提示 |
| ✅ | 空 catch 块无日志 | `SysConfigController.java:290`<br>`VersionController.java:447,368`<br>`AuditFieldAspect.java:155` | 已添加 log.debug/warn |
| ✅ | @Transactional 未指定 rollbackFor | UserAppService/DocumentAppService | 已添加 rollbackFor=Exception.class |

**状态**：✅ 已完成

---

### 18. 前端竞态条件问题

**问题描述**：异步操作未处理竞态，可能导致数据被旧请求覆盖。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | watch + emit 循环 | `StateCompensationForm.vue:224` | 已使用标志位防止循环 |
| ✅ | watch + emit 循环 | `StructuredTemplateEditor.vue:127` | 已使用标志位防止循环 |
| ✅ | 并发请求未去重 | `MatterSelector.vue:loadDossierItems` | 已添加 requestId 版本号校验 |
| ✅ | 卸载后修改 ref | `office-preview/index.vue` | 已添加 isMounted 检查（loadFromBackend 和 loadDirectUrl）|
| ✅ | Modal.confirm 重复提交 | `archive/list/index.vue:466` | 已添加 isSubmitting 标志 |
| ✅ | Modal.confirm 重复提交 | `hr/payroll/index.vue:347,364` | 已添加 isSubmitting 标志 |
| 🟡中 | 并发请求未去重 | `document/list/index.vue:671`<br>`matter/detail/index.vue:253` | 需添加请求取消或版本号校验 |

**状态**：🔄 部分修复

---

### 19. SQL 注入风险

**问题描述**：动态 SQL 拼接存在潜在注入风险。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 列名未白名单校验 | `PageUtils.java:227` | 已添加列名白名单校验 |
| ✅ | DataScope 动态列名无白名单 | `DataScopeInterceptor.java` | 已添加 isValidIdentifier 校验 |
| ✅ | DataScopeHelper 列名无白名单 | `DataScopeHelper.java` | 已添加 validateColumnName 校验 |
| 🟡中 | 迁移脚本直接执行 | `MigrationAppService.java:311` | 加强脚本目录权限控制 |
| 🟢低 | wrapper.last() LIMIT | `OperationLogAppService.java:271` | maxRows 已为固定常量 |
| 🟢低 | wrapper.last() LIMIT | `ClientAppService.java:178` | safeLimit 已有范围限制 |
| 🟢低 | URL 拼接 repo 参数 | `VersionController.java:499` | 对 repo 添加格式校验 |

**状态**：🔄 部分修复

---

### 20. 前端安全问题

**问题描述**：打印功能存在 XSS 风险，document.write 未消毒。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | document.write 未消毒 | `EvidenceTableEditor.vue:1129,1144`<br>`EvidenceListDisplay.vue:660,681` | 已使用 escapeHtml 转义 |
| ✅ | ContractPreviewModal 打印未消毒 | `ContractPreviewModal.vue:278` | 已添加 sanitizeHtml |
| ✅ | PreviewModal 打印未消毒 | `letter-template/PreviewModal.vue:135` | 已添加 sanitizeHtml |
| ✅ | 卷宗封面打印未转义 | `archive/list/index.vue:740-774` | 已添加 escapeHtml |
| ✅ | 合同打印 XSS | `matter/contract/index.vue` | 已添加 escapeHtml/sanitizeHtml |
| 🟡中 | URL 跳转未白名单 | `office-preview/index.vue:57`<br>`adapter/component/index.ts:186` | 添加域名白名单校验 |

**状态**：🔄 部分修复

---

### 21. 文件上传安全问题

**问题描述**：部分上传接口缺少文件校验。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | OCR 接口无文件校验 | `OcrController.java` | 已添加 FileValidator 校验 |
| ✅ | 任务评论附件无校验 | `TaskCommentAppService.java` | 已添加 FileValidator 校验 |
| ✅ | 用印申请附件无校验 | `SealApplicationAppService.java` | 已添加 FileValidator 校验 |
| ✅ | MinIO 文件名未过滤 .. | `MinioPathGenerator.java` | 已添加路径遍历过滤 |

**状态**：🔄 部分修复

---

### 22. 权限校验缺失问题

**问题描述**：部分接口缺少权限校验或资源归属校验。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | OnlyOffice 回调无认证 | `DocumentController.java:768` | 需配合 OnlyOffice JWT 配置（复杂改动）|
| ✅ | 客户文件开放接口无认证 | `ClientFileOpenController.java` | 已添加 X-API-Key 校验 |
| 🔴高 | 文档接口缺资源归属校验 | `DocumentController.java` | 校验 matter 归属 |
| ✅ | OCR 接口缺权限 | `OcrController.java` | 已添加 @RequirePermission("ocr:use") |
| ✅ | 证据文件代理缺权限 | `EvidenceController.java:509` | 已添加 @RequirePermission("evidence:view") |
| ✅ | Content-Disposition 注入 | `EvidenceController.java:556` | 已添加文件名安全处理和 RFC 5987 编码 |
| 🟡中 | 批量删除无二次确认 | `ClientController.java:169`<br>`UserController.java:144` | 添加确认码机制 |
| 🟡中 | 案件选择接口缺权限注解 | `MatterController.java:75,112` | 添加 @RequirePermission("matter:list") |
| 🟡中 | 个人中心接口缺权限注解 | `ProfileController.java:51,68` | 添加 @PreAuthorize("isAuthenticated()") |

**状态**：🔄 部分修复

---

### 23. 并发与线程安全问题

**问题描述**：异步操作异常处理不足，缓存操作非原子。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | CompletableFuture 无异常处理 | `VersionController.java:184` | 已添加 exceptionally() |
| ✅ | @Async 方法无 try-catch | `ContractSyncService.java:74`<br>`DossierAutoArchiveService.java:232` | 已添加全局 try-catch |
| ✅ | 缓存返回可变引用 | `DataScopeInterceptor.java:256` | 已返回不可变副本 |
| ✅ | check-then-act 竞态 | `VersionController.java:448-464` | 已改为 update-first + catch DuplicateKey |
| ✅ | check-then-act 竞态 | `DataScopeInterceptor.java:149-188` | 已改为 get() + putIfAbsent() |
| ✅ | 状态变更非原子 | `CircuitBreaker.java:183-236` | 已添加 synchronized 保护状态转换 |
| 🟡中 | get-then-put 非原子 | `CacheDegradationService.java:79` | 使用原子操作 |
| 🟡中 | 编号生成竞态 | `LetterAppService.java:967-971` | 需扩展同步范围 |

**状态**：🔄 大部分修复

---

### 24. Vue watch deep 性能问题

**问题描述**：watch deep:true 监听大对象可能影响性能。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | 配置列表 deep watch | `system/config/index.vue:826` | 监听具体字段 |
| 🟡中 | 菜单树 deep watch | `MenuModal.vue:193` | 监听具体字段 |
| 🟢低 | props 初始化后未同步 | `EvidenceListDisplay.vue:99` | 添加 watch 同步 |

**状态**：⏳ 待修复（性能优化）

---

### 25. 日志敏感信息泄露

**问题描述**：操作日志记录了密码、API Key 等敏感信息。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 密码写入操作日志 | `ProfileController.java:69`<br>`UserController.java:161` | 已添加 saveParams=false |
| ✅ | API Key 写入操作日志 | `ExternalIntegrationController.java:110,126` | 已添加 saveParams=false |
| ✅ | 登录密码写入操作日志 | `AuthController.java:167` | 已添加 saveParams=false |
| ✅ | 验证码答案写入 DEBUG 日志 | `CaptchaService.java:77` | 已移除 answer 参数 |
| ✅ | 异常信息暴露给前端 | `EvidenceController.java:687,735` | 已改为通用错误提示 |
| ✅ | 许可码写入日志 | `LoginLocationService.java:411` | 已移除敏感参数 |
| 🟡中 | Token 写入日志 | `OnlyOfficeService.java:409` | 脱敏处理 |
| 🟡中 | 滑块验证凭证写入日志 | `SliderCaptchaService.java:77,134,156` | 移除敏感参数 |

**状态**：🔄 高优先级已修复

---

### 26. API 响应数据暴露

**问题描述**：DTO 返回了敏感字段。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | LoginUser.password 可被序列化 | `LoginUser.java:39` | 已添加 @JsonIgnore |
| ✅ | 系统配置暴露密码 | `SysConfigAppService.java` | 已添加敏感配置脱敏 |
| 🟡中 | 用户选项返回完整 DTO | `UserController.java:71` | 返回简化 DTO |

**状态**：🔄 部分修复

---

### 27. 日期时间处理问题

**问题描述**：日期解析和计算存在潜在问题。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | executeDayOfMonth 为 null 时 NPE | `ScheduledReportAppService.java:682` | 已添加 null 检查，默认值 1 |
| 🟡中 | Excel 导入日期格式支持不足 | `UserAppService.java:749`<br>`ClientAppService.java:762` | 添加多格式解析支持 |
| 🟢低 | parseDate 单数月/日解析 | `PaddleOcrService.java:474-481` | 已有多格式尝试，影响小 |

**状态**：🔄 高优先级已修复

---

### 28. 事件监听器清理问题

**问题描述**：printWindow 上的 load 事件监听未使用 `{ once: true }`。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | printWindow load 监听未自动移除 | `EvidenceListDisplay.vue:686` | 已添加 { once: true } |
| ✅ | printWindow load 监听未自动移除 | `EvidenceTableEditor.vue:1150` | 已添加 { once: true } |

**状态**：✅ 已修复

---

### 29. Service 层性能问题

**问题描述**：Service 层存在 N+1 查询和大集合加载问题。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | N+1 循环内查询 | `PayrollAutoConfirmScheduler.java` | 已重构为批量查询和批量更新 |
| ✅ | N+1 循环内查询 | `ApproverService.java:getMatterCloseAvailableApprovers` | 已重构为批量查询 |
| 🟡中 | N+1 循环内查询 | `ApproverService.java` 其他方法 | 需继续使用批量查询重构 |
| 🟡中 | 全量加载用户 | `ApproverService.java:337` | 添加分页或条件限制 |
| 🟡中 | 全量加载收款 | `StatisticsAppService.java:590-598` | 使用聚合查询 |
| 🟡中 | 全量加载员工 | `PayrollAppService.java:194-195,546-547` | 添加分页处理 |
| 🟡中 | 全量加载资产 | `AssetInventoryAppService.java:94` | 分批处理 |
| 🟡中 | 全量加载交接数据 | `DataHandoverService.java:777-921` | 分批处理 |
| 🟢低 | 递归查询父部门 | `DepartmentAppService.java:176-182` | 一次性加载部门树 |

**状态**：⏳ 待修复

---

### 30. 输入验证与错误处理问题

**问题描述**：参数校验和错误处理不完善。

#### 30.1 后端参数校验

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | progress 无范围限制 | `TaskController.java:155` | 已添加 @Min(0) @Max(100) |
| ✅ | groupName 无长度限制 | `EvidenceController.java:179` | 已添加 @Size(max=100) |
| ✅ | folder 路径遍历风险 | `DocumentAppService.java:buildStoragePath` | 已使用 MinioPathGenerator.sanitizeFolderName |
| ✅ | 事务方法吞异常 | `DocumentAppService.java:updateDossierItemCount` | 已重新抛出异常确保事务回滚 |
| 🟡中 | version 无格式校验 | `MigrationController.java:83` | 添加版本号格式验证 |
| 🟡中 | keyword 无长度限制 | `ClientController.java:273` | 添加 @Size(max=100) |

#### 30.2 前端错误处理

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 数组访问无边界检查 | `EvidenceListDisplay.vue:120` | 已改用可选链 |
| ✅ | 可选链使用不当 | `resignation/index.vue:409` | 已改为 option?.children?.[0] |
| ✅ | API 返回未判空 | `user/index.vue:161` | 已添加 ?? [] 和 ?? 0 |
| ✅ | API 返回未判空 | `contract/index.vue:764` | 已添加 ?? [] 和 ?? 0 |
| ✅ | 统计失败未提示用户 | `contract/index.vue:777` | 已添加 message.error |
| ✅ | Input 缺少 maxlength | `crm/client/index.vue:924-1029` | 已添加各字段 maxlength |
| ✅ | InputNumber 缺少 min/max | `hr/payroll/index.vue:1103-1106` | 已添加 :min="0" :max="9999999.99" |
| 🟡中 | Promise 无 .catch() | `CauseOfActionTab.vue:482-493` | 添加错误处理 |

**状态**：🔄 高优先级已修复

---

### 31. 代码质量改进（魔法数字和最佳实践）

**问题描述**：硬编码值和不佳的编码实践。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 魔法数字 10 | `ContractNumberGenerator.java:161,169` | 已定义 MAX_CONTRACT_NUMBER_RETRY 常量 |
| ✅ | 数组 index 赋值 | `EvidenceListManager.vue:202` | 已改用 splice() 方法 |
| 🟡中 | 硬编码超时参数 | `LlmClient.java:51-81` | 改为配置驱动 |
| 🟡中 | 硬编码路径 /tmp | `VersionController.java:175,222` | 使用环境变量 |
| 🟡中 | 重试参数硬编码 | `ContractSyncService.java:41,112` | 改为配置驱动 |

**状态**：🔄 部分修复

---

### 32. 异步操作优化问题

**问题描述**：前端异步操作存在串行执行和错误处理不完善问题。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | Promise 无 .catch() | `CauseOfActionTab.vue:482` | 已添加 .catch() 错误处理 |
| ✅ | 循环串行 await | `EvidenceListDisplay.vue:317` | 已改用 Promise.allSettled 并行 |
| ✅ | 数组 index 赋值 | `EvidenceListDisplay.vue:326,343` | 已改用 splice() 方法 |
| 🟡中 | 多个独立 await 串行 | `contract/index.vue:784-834` | 改为 Promise.all 并行 |
| 🟡中 | onMounted 串行 await | `config/index.vue:904-911` | 独立请求用 Promise.all |
| 🟡中 | 循环中重复请求 | `contract/index.vue:2277-2321` | 循环外只请求一次 |

**状态**：🔄 高优先级已修复

---

### 33. 空指针和类型安全问题

**问题描述**：Map.get() 返回值和类型断言使用不当导致的潜在错误。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | Map.get("userId") 未判空 | `FinanceContractAmendmentService.java:270` | 已添加 null 检查 |
| ✅ | Map.get("amount") 未判空 | `FinanceContractAmendmentService.java:330,377` | 已添加 null 检查 |
| ✅ | value! 非空断言错误 | `hr/contract/index.vue:700,715,730` | 已改用 value?.replace() ?? '' |
| 🟡中 | else 分支未判空 | `PdfReportGenerator.java:445,679,826` | 添加 Objects.toString() |
| 🟡中 | detail as any 类型绕过 | `contract/index.vue:2064` | 定义 DTO 接口 |

**状态**：🔄 高优先级已修复

---

### 34. equals() 空指针安全问题

**问题描述**：equals() 在可能为 null 的对象上调用导致 NPE 风险。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | clientId.equals() 可能 NPE | `PrepaymentAppService.java:275` | 已改用 Objects.equals() |
| ✅ | fromUserId.equals() 可能 NPE | `DataHandoverService.java:219` | 已改用 Objects.equals() |
| ✅ | getVersion().equals() 可能 NPE | `DocumentAppService.java:421` | 已改用 Objects.equals() |
| 🟡中 | userId.equals() 未判空 | `LoginLocationService.java:405-406` | 方法参数校验 |
| 🟡中 | sourceModule.equals() 可能 NPE | `DossierAutoArchiveService.java:777-781` | 使用 Objects.equals() |

**状态**：🔄 高优先级已修复

---

### 35. 表单处理问题

**问题描述**：前端表单重置和数值处理存在问题。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 天数/日赔偿金清空后金额未重置 | `StateCompensationForm.vue:202-206` | else 分支设 amount=0 |
| ✅ | 表单重置未清空所有字段 | `contract/index.vue:2426-2451` | 补全缺失字段 |
| ✅ | 刑事罪名级联未重置 | `contract/index.vue:2423-2424` | 重置 criminalChargeValue |
| 🟡中 | NaN 风险 | `hr/payroll/index.vue:288-302` | parseFloat 返回 NaN 未处理 |
| 🟡中 | 表单校验与后端不一致 | `UserModal.vue:57,69` | 前端未强制用户名/密码长度 |

**状态**：🔄 高优先级已修复

---

### 36. 前端内存泄漏问题

**问题描述**：事件监听器未正确清理导致内存泄漏。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 文件选择取消后监听器未移除 | `document/list/index.vue:1564` | 跟踪输入并在 onUnmounted 清理 |
| ✅ | 打印窗口 load 监听器未移除 | `office-preview/index.vue:419,477` | 使用 { once: true } |
| ✅ | OnlyOffice 脚本 load 监听器未移除 | `office-preview/index.vue:247` | 使用 { once: true } |
| 🟡中 | 打印窗口生命周期与组件不同步 | `office-preview/index.vue:464-488` | 添加 isMounted 检查 |

**状态**：🔄 高优先级已修复

---

### 37. 后端缓存问题

**问题描述**：缓存无过期或大小限制，可能导致内存增长。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | deptChildrenCache 无过期限制 | `DataScopeInterceptor.java:66-67` | 部门数量有限，已有 clearDeptCache 方法 |
| ✅ | 部门缓存清理不统一 | `CacheController.java:94-99` | 同时清理 DataScopeInterceptor 缓存 |
| 🟡中 | taskRecords 无过期限制 | `ScheduledTaskMonitor.java:25-26` | 任务类型数量有限，影响小 |
| 🟡中 | taskStatsMap 无过期限制 | `AsyncTaskMonitor.java:26` | 任务类型数量有限，影响小 |

**状态**：🔄 高优先级已修复

---

### 38. Redis 数据类型不匹配

**问题描述**：Redis 操作数据类型不一致导致读写失败。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | getTrustedLocations 用 opsForValue 读 Set | `LoginLocationService.java:138-152` | 改用 opsForSet().members() |

**状态**：✅ 已修复

---

### 39. API 错误处理不完善

**问题描述**：API 调用失败时缺少用户提示。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 首页统计加载失败无提示 | `dashboard/index/index.vue` | 统计失败次数，全部/部分失败时提示 |
| ✅ | 财务报表加载失败无提示 | `finance/report/index.vue` | 统计失败次数，全部/部分失败时提示 |
| 🟡中 | 客户列表无 try-catch | `crm/client/index.vue:242-254` | 依赖 grid 默认处理 |
| 🟡中 | 合同详情加载失败无提示 | `matter/contract/index.vue` | catch 中只有 console.error |
| ✅ | 批量审批无 loading | `workbench/approval/index.vue` | onOk 返回 Promise，失败时 throw |

**状态**：✅ 已完成

---

### 40. 文件上传安全问题

**问题描述**：文件名未清理或类型校验不完善。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 证据清单名称未 sanitize | `EvidenceListAppService.java:374-396` | 使用 MinioPathGenerator.sanitizeFilename |
| 🟡中 | FileTypeService 仅扩展名校验 | `FileTypeService.java:200-221` | 需增加 MIME 和内容校验 |
| 🟡中 | 备份导入仅扩展名校验 | `BackupAppService.java:1419-1446` | 已限制为 .sql/.dump |
| 🟢低 | OcrController 校验逻辑 | `OcrController.java` | 检查 FileValidator 注入 |

**状态**：🔄 高优先级已修复

---

### 41. 日期时间处理问题

**问题描述**：时区处理不当或格式不一致。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | LocalDateTime 被当作 UTC 转换 | `DocumentController.java:708-710` | 改用系统默认时区 |
| 🟡中 | JWT 使用 new Date() 未注入 Clock | `JwtTokenProvider.java:100-101` | 影响测试可控性 |
| 🟡中 | 时间格式不统一 | `VersionController.java` | 同一控制器格式不同 |
| 🟢低 | BackupAppService 多种格式混用 | `BackupAppService.java` | 统一为常量 |

**状态**：🔄 高优先级已修复

---

### 42. 输入验证缺失

**问题描述**：@RequestBody 缺少 @Valid 或参数无范围限制。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 付款计划/参与人/变更命令缺 @Valid | `ContractController.java` | 添加 @Valid |
| ✅ | 工资表相关命令缺 @Valid | `PayrollController.java` | 添加 @Valid |
| ✅ | year/month 参数无范围限制 | `PayrollController.java:106` | 添加 @Min/@Max |
| 🟡中 | List 参数无大小限制 | `SysConfigController/DocumentController` | 添加 @Size(max=...) |

**状态**：🔄 高优先级已修复

---

### 43. XSS 风险修复

**问题描述**：HTML 拼接未转义用户输入。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 收案审批表打印未转义 | `matter/contract/index.vue:1856-1896` | 使用 escapeHtml |
| ✅ | 卷宗封面标题未转义 | `archive/list/index.vue:613` | 使用 escapeHtml |
| ✅ | 系统名称未转义 | `archive/list/index.vue:725` | 使用 escapeHtml |
| ✅ | 工资表打印未转义 | `hr/payroll/index.vue:684-737` | 使用 escapeHtml |
| 🟡中 | URL 未做协议校验 | `document/list/index.vue` | 添加协议白名单 |

**状态**：🔄 高优先级已修复

---

### 44. 异步错误处理问题

**问题描述**：@Async/@Scheduled 方法异常处理不完善。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | findById 在 try 外导致异常时状态不更新 | `ReportAppService.java:360-364` | 移入 try 块内 |
| ✅ | catch 中 updateById 可能抛异常 | `BackupAppService.java:275-282,883-890` | 包装在 try-catch 中 |
| ✅ | @Scheduled 无 try-catch 异常会中断 | `PayrollAutoConfirmScheduler.java:38-140` | 添加全局 try-catch |
| 🟡中 | 健康检查无异常处理 | `SystemReportService.java:107-116` | 添加 try-catch |

**状态**：🔄 高优先级已修复

---

### 46. API 响应空值保护

**问题描述**：前端直接访问 API 响应字段，未做空值检查。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | res.list 直接访问 | `archive/list/index.vue:285-288,331` | 使用 res?.list ?? [] |
| ✅ | res.list 直接访问 | `document/list/index.vue:944` | 使用 res?.list ?? [] |
| 🟡中 | res.list/res.total 无检查 | `system/role/index.vue:137-138` | 添加空值保护 |
| 🟡中 | res.list/res.total 无检查 | `personal/ai-usage/index.vue:241-243` | 添加空值保护 |
| 🟡中 | res.list 访问前无检查 | `workbench/schedule/index.vue:226` | 使用 res?.list ?? [] |

**状态**：🔄 高优先级已修复

---

### 47. 后端边界条件处理

**问题描述**：除法、字符串操作等边界条件未处理。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | pageSize 为 0 导致除零 | `PageResult.java:59` | 添加 pageSize > 0 检查 |
| ✅ | heapMax 可能为 -1 或 0 | `SystemReportService.java:279,413` | 添加 heapMax > 0 检查 |
| ✅ | lastIndexOf 为 -1 时 substring 越界 | `DataScopeInterceptor.java:163-164` | 提前检查 lastDotIndex |
| 🟡中 | get(0) 依赖隐式非空假设 | `TemplateVariableService.java:101,157` | 改用更安全的写法 |

**状态**：🔄 高优先级已修复

---

### 48. 事件处理安全问题

**问题描述**：删除操作无确认、表单缺 loading 状态。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 删除会议室无确认对话框 | `hr/meeting-room/index.vue:319` | 添加 Popconfirm |
| ✅ | 上传文档弹窗无 loading | `document/list/index.vue:2717-2722` | 添加 confirm-loading |
| ✅ | 报销提交弹窗无 loading | `finance/expense/index.vue:412-417` | 添加 confirm-loading |
| 🟡中 | 备份导入无文件类型/大小限制 | `system/backup/index.vue:466-469` | 添加 beforeUpload 校验 |
| 🟡中 | 培训附件无文件类型/大小限制 | `hr/training/index.vue:471-476` | 添加 beforeUpload 校验 |

**状态**：🔄 高优先级已修复

---

### 49. 权限校验缺失

**问题描述**：部分接口缺少资源归属校验。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 文档查看/删除未校验项目权限 | `DocumentAppService.java` | 添加 validateDocumentAccess |
| ✅ | 批量下载未逐项校验权限 | `DocumentController.java:278-308` | getDocumentById 已含权限检查 |
| 🟡中 | 删除合同参与人未校验合同权限 | `ContractParticipantService.java:167` | 添加合同归属校验 |
| 🟡中 | 删除客户未校验数据权限 | `ClientAppService.java:341` | 添加数据范围校验 |

**状态**：🔄 高优先级已修复

---

### 50. SQL 安全问题

**问题描述**：LIKE 查询未转义通配符，存在注入风险。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | wrapper.last() 拼接未校验 | `OperationLogAppService.java:271` | 添加范围校验 |
| ✅ | LIKE 未转义 % 和 _ | `OperationLogAppService.java` | 添加 SqlUtils.escapeLike |
| ✅ | LIKE 未转义通配符 | `UserMapper.java:48-54` | UserAppService 添加转义 |
| ✅ | LIKE 未转义通配符 | `MatterMapper.java:39-42` | MatterAppService 添加转义 |

**状态**：🔄 高优先级已修复

---

### 51. 路由参数安全

**问题描述**：路由参数未校验可能导致错误或安全问题。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | clientId 转换可能为 NaN | `matter/list/index.vue:1042` | 添加 isNaN 检查 |
| ✅ | returnPath 开放重定向风险 | `crm/client/index.vue:390` | 校验路径以 / 开头 |
| 🟡中 | route.query 变化未 watch | `document/list/index.vue` | 添加 watch |
| 🟡中 | route.query 变化未 watch | `office-preview/index.vue` | 添加 watch |

**状态**：🔄 高优先级已修复

---

### 52. 日期时间处理问题

**问题描述**：日期解析、格式化和范围校验问题。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | new Date() 未校验有效性 | `document/list/index.vue:894-896` | 添加 isNaN 检查 |
| ✅ | formatTime 未处理 null | `workbench/schedule/index.vue:432` | 添加 null 检查和 isValid |
| ✅ | 日期范围未校验开始<结束 | `hr/contract/index.vue:381-390` | 添加日期比较校验 |
| 🟡中 | dayjs 未处理 null | `matter/detail/index.vue:1086` | 添加空值检查 |
| 🟡中 | 时间范围未校验 | `workbench/schedule/index.vue:355-366` | 添加时间比较 |

**状态**：🔄 高优先级已修复

---

### 54. 数据转换安全问题

**问题描述**：数据转换未处理 null 和异常情况。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | getTotalAmount() 可能为 null | `ContractAppService.java:1709-1713` | 添加 null 检查 |
| ✅ | Long.parseLong 无异常处理 | `DataHandoverService.java:667` | 添加 try-catch |
| ✅ | Long.parseLong 无异常处理 | `ScheduledReportAppService.java:391,754` | 添加 try-catch |
| ✅ | Map.get 结果转 Number 无检查 | `DocAccessLogService.java:122` | 添加 instanceof 检查 |
| 🟡中 | 报表参数解析未用工具类 | `ReportAppService.java:1163` | 使用 ReportParameterUtils |

**状态**：🔄 高优先级已修复

---

### 55. 表单验证缺失

**问题描述**：表单未使用 Form 校验或缺少格式校验。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | 劳动合同表单无 Form 校验 | `hr/contract/index.vue` | 添加 rules 和 validate |
| 🟡中 | 离职表单无 Form 校验 | `hr/resignation/index.vue` | 添加 rules 和 validate |
| 🟡中 | 联系电话/邮箱无格式校验 | `crm/client/index.vue` | 添加格式验证规则 |
| 🟡中 | 邮箱/手机号无格式校验 | `system/user/components/UserModal.vue` | 添加格式验证规则 |
| 🟡中 | 会议室表单无 Form 校验 | `hr/meeting-room/index.vue` | 添加 rules 和 validate |

**状态**：⏳ 待修复

---

### 56. 前端性能和循环风险

**问题描述**：watch 重复监听、深度监听等性能问题。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 同一变量被两个 watch 监听 | `matter/list/index.vue:261-289` | 合并为单个 watch |
| 🟡中 | 直接修改 route.query | `document/list/index.vue:1879-1886` | 使用 router.replace |
| 🟡中 | watch deep:true 频繁更新 | `system/config/index.vue:819-827` | 改用浅比较 |
| 🟡中 | 模板中直接 filter | `StructuredLetterEditor.vue:409-412` | 改用 computed |
| 🟡中 | watch 循环依赖风险 | `StateCompensationForm.vue:224-241` | 优化数据流 |

**状态**：🔄 高优先级已修复

---

### 57. 后端事务管理问题

**问题描述**：事务内异常处理和长事务问题。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | try-catch 吞异常不回滚 | `DossierAutoArchiveService.java:298-301` | 重新抛出异常 |
| 🟡中 | 事务内 HTTP 调用 | `ClientFileService.java:201-203` | 事务外执行网络 IO |
| 🟡中 | 事务内 LLM API 调用 | `AiDocumentService.java:98-101` | 事务外调用 API |
| 🟡中 | 批量操作长事务 | `ClientFileService.java:299-316` | 拆分为多个小事务 |

**状态**：⏳ 待修复（架构改动较大）

---

### 58. 可访问性问题

**问题描述**：键盘无法访问、缺少 aria-label 等可访问性问题。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 验证码图片键盘无法刷新 | `login.vue:327-334` | 包裹在 button 中 |
| ✅ | 图标按钮无 aria-label | `archive/list/index.vue:1075-1078` | 添加 aria-label |
| ✅ | 图标按钮无 aria-label | `document/list/index.vue:2516-2518` | 添加 aria-label |
| 🟡中 | 快捷操作 div 无法聚焦 | `dashboard/index/index.vue:383-420` | 改用 button 或添加 tabindex |
| 🟡中 | 输入框缺少 label | `login.vue:320-326` | 添加 aria-label |

**状态**：🔄 高优先级已修复

---

### 59. 异常信息泄露

**问题描述**：异常消息暴露给客户端或存入数据库。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 异常消息返回客户端 | `ClientFileService.java:373` | 使用通用错误文案 |
| ✅ | 异常消息存入同步状态 | `ClientFileService.java:274` | 使用通用错误文案 |
| ✅ | finally 可能覆盖原异常 | `BackupAppService.java:630-633,1214-1217` | try-catch 包裹 |
| 🟡中 | 异常消息存入交接记录 | `DataHandoverService.java:510` | 使用业务化错误文案 |

**状态**：🔄 高优先级已修复

---

### 53. 日志记录问题

**问题描述**：异常日志缺少堆栈信息，不利于排查。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | JWT 认证失败无堆栈 | `JwtAuthenticationFilter.java:101` | 添加异常参数 |
| ✅ | 性能日志无堆栈 | `LogUtils.java:52,74` | 添加异常参数 |
| ✅ | 文件删除异常无堆栈 | `ClientFileService.java:456` | 添加异常参数 |
| 🟡中 | 身份证 URL 可能含敏感信息 | `OcrAppService.java:100` | 脱敏处理 |
| 🟡中 | 循环内记录日志 | `BackupAppService.java:536-558` | 改为汇总日志 |

**状态**：🔄 高优先级已修复

---

### 45. 前端组件卸载时状态更新

**问题描述**：异步操作完成后未检查组件是否仍挂载。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 首页数据加载无 isMounted 检查 | `dashboard/index/index.vue` | 添加 isMounted 跟踪 |
| 🟡中 | 工作台数据加载无检查 | `dashboard/workspace/index.vue` | 添加 isMounted 跟踪 |
| 🟡中 | 文档列表加载无检查 | `document/list/index.vue` | 添加 isMounted 跟踪 |
| 🟡中 | 档案列表加载无检查 | `archive/list/index.vue` | 添加 isMounted 跟踪 |

**状态**：🔄 高优先级已修复

---

### 60. 数据验证与安全配置问题

**问题描述**：URL参数未校验、API开放接口缺认证保护。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | documentId 未校验即用于 API 请求 | `office-preview/index.vue:61-62` | 添加正整数校验 |
| ✅ | URL 参数未校验即加载（SSRF 风险） | `office-preview/index.vue:65-66` | 添加 http/https 协议校验 |
| ✅ | 打印时 documentId 未校验 | `office-preview/index.vue:410-464` | 添加正整数校验 |
| ✅ | archive/list matterId 未校验 > 0 | `archive/list/index.vue:872-873` | 添加正整数校验 |
| ✅ | matter/list id 未校验 > 0 | `matter/list/index.vue:1061-1062` | 添加正整数校验 |
| ✅ | returnQuery JSON 解析未校验结构 | `crm/client/index.vue:393-401` | 添加白名单校验防原型污染 |
| ✅ | 开放接口无 API Key 时直接放行 | `ClientFileOpenController.java:52-59` | 未配置时拒绝请求 |
| 🟡中 | CORS 配置过于宽松 | `WebMvcConfig.java:39` | 生产环境限定可信域名 |
| 🟡中 | 静态 token 密钥硬编码 | `DocumentController.java:83` | 应使用配置注入 |
| 🟡中 | JWT/OnlyOffice 默认密钥风险 | `application.yml:89,145` | 生产环境强制配置 |

**状态**：🔄 高优先级已修复

---

### 61. 并发和线程安全问题

**问题描述**：限流竞态、ThreadLocal 泄漏、静态变量可变性。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 限流 check-then-act 竞态 | `RateLimitAspect.java:47-61` | 使用原子 increment 操作 |
| ✅ | ReportAppService ThreadLocal 未清理 | `ThreadLocalCleanupFilter.java` | 添加 clearCache 调用 |
| ✅ | staticTokenSecret 可变静态变量 | `DocumentController.java:83` | 改为 final 常量 |
| ✅ | 部门缓存 check-then-act | `DataScopeInterceptor.java:298-321` | 使用 computeIfAbsent |
| 🟡中 | @Async 依赖 SecurityContext | `AiUsageRecorder.java:80-99` | 应在主线程获取用户 |

**状态**：🔄 高优先级已修复

---

### 62. 前端内存泄漏问题

**问题描述**：setTimeout 未在组件卸载时清理。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 打印 setTimeout 未清理 | `matter/contract/index.vue:1927` | 添加 activeTimers 跟踪 |
| ✅ | goBack setTimeout 未检查挂载状态 | `office-preview/index.vue:543` | 添加 isMounted 检查 |
| ✅ | emit success setTimeout 未检查 | `ConfigModal.vue:124` | 添加 isMounted 检查 |
| ✅ | setValues setTimeout 未检查 | `ConfigModal.vue:145` | 添加 isMounted 检查 |
| ✅ | emit success setTimeout 未检查 | `CauseModal.vue:229` | 添加 isMounted 检查 |

**状态**：✅ 已完成

---

### 63. 后端资源未正确关闭

**问题描述**：InputStream、Process 流未正确关闭。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | file.getInputStream() 未关闭 | `FileAccessService.java:68` | 使用 try-with-resources |
| ✅ | Process 流未消费/关闭 | `BackupAppService.java:369` | 添加 drainStream 和 destroy |
| 🟡中 | docker cp Process 流未关闭 | `BackupAppService.java:347,680,940` | 需统一 Process 执行工具类 |
| 🟡中 | 匿名 Process 流未关闭 | `BackupAppService.java:356,684,1249,1274` | 需重构 |

**状态**：🔄 高优先级已修复

---

### 64. 前端错误处理缺失

**问题描述**：API 调用失败后无用户反馈。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | 滑块令牌获取失败无反馈 | `login.vue:82-84` | 添加 message.warning |
| ✅ | 验证码获取失败无反馈 | `login.vue:94-96` | 添加 message.warning |
| ✅ | 会话列表加载失败空 catch | `SessionTab.vue:82-83` | 添加 message.error |
| ✅ | 登录日志加载失败空 catch | `LoginLogTab.vue:74-76` | 添加 message.error |
| ✅ | 登录日志详情失败无提示 | `LoginLogTab.vue:86-88` | 添加 message.warning |
| ✅ | 日程页多个加载仅 console | `schedule/index.vue` | 添加 message.error |
| ✅ | 案由详情加载仅 console | `CauseOfActionTab.vue:495` | 添加 message.error |

**状态**：✅ 已完成

---

## ✅ 已完成任务

_（完成后将任务移至此处）_

---

## 📝 备注

- 修改菜单位置时需同步更新数据库和初始化脚本
- 所有改动需在测试环境验证后再部署生产
