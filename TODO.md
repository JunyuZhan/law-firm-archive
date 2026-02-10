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
| 🔴高 | 硬编码密钥 | `DocumentController.java:76`<br>`OnlyOfficeService.java:424` | 改用环境变量 `${document.token.secret}` |
| 🔴高 | 路径遍历 | `VersionController.java:175` | 验证 upgradeId 不含 `../`、`/`、`\` |
| 🔴高 | ThreadLocal 泄漏 | `StatisticsAppService.java:87-93`<br>`ContractDataPermissionService.java:45-48` | 添加 Filter 在请求结束时调用 clearCache() |
| 🟡中 | 日志记录 Token | `DocumentController.java:1046` | 仅记录 token hash 或移除 |
| 🟡中 | IP 验证宽松 | `DocumentController.java:1086` | 使用 CIDR 验证库 |

#### 4.2 前端安全问题

| 风险 | 问题 | 位置 | 修复方案 |
|------|------|------|----------|
| 🔴高 | refreshToken 存 localStorage | `store/auth.ts:86`<br>`api/request.ts:59` | 改用 SecureLS 或 httpOnly cookie |
| 🔴高 | v-html XSS | 6 处文件 | 使用 DOMPurify 清理 HTML |
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
| 🔴高 | 数组越界风险 | `LlmClient.java:664,725,952,1387` | `get(0)` 前检查数组是否为空 |
| 🔴高 | 空指针风险 | `LlmClient.java` 多处 | `response.getBody()` 判空 |
| 🔴高 | 空指针风险 | `PayrollController.java:284-287` | `sheet` 判空后再访问属性 |
| 🟡中 | 调试代码残留 | `ContractController.java:121-125,462-467` | 移除 `System.out.println` |
| 🟡中 | 异常被吞掉 | `SysConfigController.java:288`<br>`AuthController.java:344`<br>`VersionController.java:368`<br>`MatterClientFileController.java:169`<br>`AiUsageRecorder.java:395` | 空 catch 块添加日志记录 |
| 🟡中 | 文件名编码 | `PayrollController.java:292` | 使用 `URLEncoder.encode` |

#### 5.2 前端问题

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🔴高 | 内存泄漏 | `EvidenceUploader.vue:52-61` | catch/finally 中清理 `setInterval` |
| 🔴高 | Blob URL 泄漏 | `office-preview/index.vue:415-435` | 监听窗口关闭时释放 URL |
| 🔴高 | FileReader 无错误处理 | `RichTextEditor/index.vue:136-143` | 添加 `error` 事件监听 |
| 🔴高 | 未处理 Promise | `ClientServicePanel/index.vue:776-786` | watch 回调添加错误处理 |
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
| 🟡中 | Process 无超时 | `VersionController.java:276-287,332-354` | 使用 waitFor(timeout, unit)，超时后 destroyForcibly() |

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
| 🔴高 | 轮询定时器未清理 | `system/config/index.vue:441,485,523` | onUnmounted 中调用 stopUpgradePolling() |
| 🔴高 | 异步回调在卸载后执行 | `document/list/index.vue:1536-1558` | 添加 isMounted 检查或取消标记 |
| 🔴高 | JSON.parse 无 try-catch | `crm/client/index.vue:392-393` | 包裹 try-catch，解析失败用默认值 |
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
| 🔴高 | pageNum/pageSize 可能为 null | `DataHandoverService.java:636-646` | new Page<>() 前添加 null 检查 |
| 🔴高 | getOffset() 未判空 | `DataHandoverQueryDTO.java:33-34` | 添加 null 检查和默认值 |
| 🟡中 | 未使用 PageUtils | `ScheduledReportAppService.java:95`<br>`CustomReportAppService.java:64` | 改用 PageUtils.createPage() |

#### 9.2 分页 pageSize 无上限

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | pageSize 可传超大值 | `DocumentController.java:346-347` | 添加最大值限制（如 100） |

**实施进度**：
- [x] DataHandoverService/DTO 分页参数判空（添加 getSafePageNum/getSafePageSize）
- [ ] 分页逻辑统一使用 PageUtils.createPage

**状态**：🔄 部分修复

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
| 🔴高 | 图片上传无校验 | `RichTextEditor/index.vue:136-146` | 添加文件类型、大小、扩展名校验 |
| 🟡中 | 上传类型不限制 | `EvidenceUploader.vue:95-103` | 添加 accept 属性限制文件类型 |

#### 11.2 路径遍历（Zip Slip）

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🔴高 | Zip 解压路径遍历 | `BackupAppService.java:1148-1154` | 校验 entry.getName() 不含 `../` |

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
| 🔴高 | e.getMessage() 暴露给前端 | `DocumentController.java:802,1172`<br>`VersionController.java:191`<br>`EvidenceController.java:463`<br>`ReportController.java:132` | 记录日志后返回通用错误信息 |

#### 12.2 资源清理缺失

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🔴高 | InputStream 未关闭 | `EvidenceController.java:295`<br>`TaskCommentAppService.java:268`<br>`SealApplicationAppService.java:455`<br>`FileAccessService.java:68`<br>`DocumentAppService.java:605` | 使用 try-with-resources 管理流 |

#### 12.3 事务回滚

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🟡中 | checked exception 不回滚 | 多个 @Transactional 方法 | 添加 rollbackFor = Exception.class |

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
| 🔴高 | 无权限即可修改配置 | `VersionController.java:409-411` | POST /ignore 添加 @RequirePermission |
| 🔴高 | 任意用户可读配置 | `SysConfigController.java:69-72,81-84` | GET /key/{key} 和 POST /batch 添加权限校验 |
| 🟡中 | 缺少细粒度权限 | `WorkbenchController.java:37-127` | 添加 @RequirePermission 注解 |

#### 13.2 回调接口安全

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🔴高 | OnlyOffice 回调无校验 | `DocumentController.java:767-806` | 添加签名或来源 IP 校验 |

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
| 🔴高 | 全表查询+内存分页 | `ExpenseAppService.java:142-158`<br>`ExpenseMapper.java` | SQL 添加 LIMIT/OFFSET |
| 🔴高 | 全表查询+内存分页 | `LeadAppService.java:74-89`<br>`LeadMapper.java` | SQL 添加 LIMIT/OFFSET |

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
| 🔴高 | AlertWebhook 无签名校验 | `AlertWebhookController.java:36` | 添加来源验证或签名校验 |
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
| 🔴高 | input 事件监听未清理 | `document/list/index.vue:1536` | 使用单例 input 或清理 |
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
| 🟡中 | @Transactional 未指定 rollbackFor | `DataHandoverService.java`<br>`BackupAppService.java`<br>`UserAppService.java` 等 | 添加 rollbackFor=Exception.class |

**状态**：🔄 部分修复

---

### 18. 前端竞态条件问题

**问题描述**：异步操作未处理竞态，可能导致数据被旧请求覆盖。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | watch + emit 循环 | `StateCompensationForm.vue:224` | 已使用标志位防止循环 |
| 🔴高 | watch + emit 循环 | `StructuredTemplateEditor.vue:127` | 使用标志位防止循环 |
| 🔴高 | 并发请求未去重 | `document/list/index.vue:671`<br>`matter/detail/index.vue:253` | 添加请求取消或版本号校验 |
| 🟡中 | 卸载后修改 ref | 多个组件 | 添加 isMounted 检查 |

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
| 🟡中 | MinIO 文件名未过滤 .. | `MinioPathGenerator.java:257` | 添加路径遍历过滤 |

**状态**：🔄 部分修复

---

### 22. 权限校验缺失问题

**问题描述**：部分接口缺少权限校验或资源归属校验。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| 🔴高 | OnlyOffice 回调无认证 | `DocumentController.java:768` | 添加签名校验 |
| 🔴高 | 客户文件开放接口无认证 | `ClientFileOpenController.java` | 添加 API Key 校验 |
| 🔴高 | 文档接口缺资源归属校验 | `DocumentController.java` | 校验 matter 归属 |
| ✅ | OCR 接口缺权限 | `OcrController.java` | 已添加 @RequirePermission("ocr:use") |
| 🟡中 | 批量删除无二次确认 | `ClientController.java:169`<br>`UserController.java:144` | 添加确认码机制 |

**状态**：🔄 部分修复

---

### 23. 并发与线程安全问题

**问题描述**：异步操作异常处理不足，缓存操作非原子。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | CompletableFuture 无异常处理 | `VersionController.java:184` | 已添加 exceptionally() |
| ✅ | @Async 方法无 try-catch | `ContractSyncService.java:74`<br>`DossierAutoArchiveService.java:232` | 已添加全局 try-catch |
| ✅ | 缓存返回可变引用 | `DataScopeInterceptor.java:256` | 已返回不可变副本 |
| 🟡中 | get-then-put 非原子 | `CacheDegradationService.java:79` | 使用原子操作 |

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
| 🟡中 | Token 写入日志 | `OnlyOfficeService.java:409` | 脱敏处理 |
| 🟡中 | 许可码写入日志 | `LoginLocationService.java:411` | 移除敏感参数 |
| 🟡中 | 滑块验证凭证写入日志 | `SliderCaptchaService.java:77,134,156` | 移除敏感参数 |

**状态**：🔄 高优先级已修复

---

### 26. API 响应数据暴露

**问题描述**：DTO 返回了敏感字段。

| 优先级 | 问题 | 位置 | 修复方案 |
|--------|------|------|----------|
| ✅ | LoginUser.password 可被序列化 | `LoginUser.java:39` | 已添加 @JsonIgnore |
| 🔴高 | 系统配置暴露密码 | `SysConfigAppService.java:254` | 敏感配置脱敏 |
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
| 🔴高 | N+1 循环内查询 | `PayrollAutoConfirmScheduler.java:51-61,86` | 批量查询替代循环查询 |
| 🔴高 | N+1 循环内查询 | `ApproverService.java:378-416,457-491` | 批量加载用户和部门 |
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
| 🟡中 | folder 路径遍历风险 | `DocumentController.java:381,413` | 过滤 ../ 等字符 |
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

## ✅ 已完成任务

_（完成后将任务移至此处）_

---

## 📝 备注

- 修改菜单位置时需同步更新数据库和初始化脚本
- 所有改动需在测试环境验证后再部署生产
