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

## ✅ 已完成任务

_（完成后将任务移至此处）_

---

## 📝 备注

- 修改菜单位置时需同步更新数据库和初始化脚本
- 所有改动需在测试环境验证后再部署生产
