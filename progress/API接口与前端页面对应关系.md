# API接口与前端页面对应关系

本文档记录所有已对接的API接口及其对应的前端页面情况。

## 已完成API对接但缺少前端页面的模块

### 1. 供应商管理 (Supplier)
- **API文件**: `frontend/apps/web-antd/src/api/admin/supplier.ts`
- **后端Controller**: `SupplierController`
- **接口路径**: `/admin/suppliers`
- **功能**: 供应商信息管理（创建、更新、删除、查询、启用/停用、统计）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/admin/supplier/index.vue`

### 2. 资产盘点 (Asset Inventory)
- **API文件**: `frontend/apps/web-antd/src/api/admin/asset-inventory.ts`
- **后端Controller**: `AssetInventoryController`
- **接口路径**: `/admin/asset-inventories`
- **功能**: 资产盘点管理（创建盘点、更新明细、完成盘点、查询进行中的盘点）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/admin/asset-inventory/index.vue`

### 3. 外出管理 (Go Out)
- **API文件**: `frontend/apps/web-antd/src/api/admin/go-out.ts`
- **后端Controller**: `GoOutController`
- **接口路径**: `/admin/go-out`
- **功能**: 外出登记和返回管理
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/admin/go-out/index.vue`

### 4. 加班管理 (Overtime)
- **API文件**: `frontend/apps/web-antd/src/api/admin/overtime.ts`
- **后端Controller**: `OvertimeController`
- **接口路径**: `/admin/overtime`
- **功能**: 加班申请和审批管理
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/admin/overtime/index.vue`

### 5. 会议通知 (Meeting Notice)
- **API文件**: `frontend/apps/web-antd/src/api/admin/meeting-notice.ts`
- **后端Controller**: `MeetingNoticeController`
- **接口路径**: `/admin/meeting-notices`
- **功能**: 会议通知发送（单个发送、批量发送即将开始的会议通知）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/admin/meeting-notice/index.vue` 或在会议管理页面中集成

### 6. 会议记录 (Meeting Record)
- **API文件**: `frontend/apps/web-antd/src/api/admin/meeting-record.ts`
- **后端Controller**: `MeetingRecordController`
- **接口路径**: `/admin/meeting-records`
- **功能**: 会议记录管理（创建、查询、根据预约创建）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/admin/meeting-record/index.vue` 或在会议管理页面中集成

### 7. 员工发展计划 (Development Plan)
- **API文件**: `frontend/apps/web-antd/src/api/hr/development-plan.ts`
- **后端Controller**: `DevelopmentPlanController`
- **接口路径**: `/hr/development-plan`
- **功能**: 个人发展规划管理（创建、更新、提交、审核、里程碑管理）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/hr/development-plan/index.vue`

### 8. 晋升管理 (Promotion)
- **API文件**: `frontend/apps/web-antd/src/api/hr/promotion.ts`
- **后端Controller**: `PromotionController`
- **接口路径**: `/hr/promotion`
- **功能**: 职级管理和晋升申请管理（职级CRUD、晋升申请、评审、审批）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/hr/promotion/index.vue`

### 9. 转正管理 (Regularization)
- **API文件**: `frontend/apps/web-antd/src/api/hr/regularization.ts`
- **后端Controller**: `RegularizationController`
- **接口路径**: `/hr/regularization`
- **功能**: 转正申请管理（创建、审批、查询）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/hr/regularization/index.vue`

### 10. 离职管理 (Resignation)
- **API文件**: `frontend/apps/web-antd/src/api/hr/resignation.ts`
- **后端Controller**: `ResignationController`
- **接口路径**: `/hr/resignation`
- **功能**: 离职申请管理（创建、审批、交接完成、查询）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/hr/resignation/index.vue`

### 11. 报表模板 (Report Template)
- **API文件**: `frontend/apps/web-antd/src/api/workbench/report-template.ts`
- **后端Controller**: `ReportTemplateController`
- **接口路径**: `/workbench/report-template`
- **功能**: 报表模板管理（创建、更新、启用/停用、根据模板生成报表、数据源管理）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/workbench/report-template/index.vue`

### 12. 定时报表 (Scheduled Report)
- **API文件**: `frontend/apps/web-antd/src/api/workbench/scheduled-report.ts`
- **后端Controller**: `ScheduledReportController`
- **接口路径**: `/workbench/scheduled-report`
- **功能**: 定时报表任务管理（创建、更新、启用/暂停、立即执行、执行记录查询）
- **前端页面状态**: ❌ 未找到对应页面
- **建议页面路径**: `views/workbench/scheduled-report/index.vue`

## 总结

### API对接完成情况
- ✅ **全部后端已开发功能的API对接已完成**
- ✅ 所有后端Controller都有对应的前端API文件
- ✅ API接口路径、参数、返回值类型都已正确对接

### 前端页面完成情况
- ✅ 大部分核心功能已有前端页面（客户、案件、文档、证据、知识库、工作台基础功能等）
- ❌ **12个新对接的模块缺少前端页面**（见上方列表）
- ❌ 这些模块的API已完全对接，但前端页面尚未开发

### 建议
1. **优先级排序**：根据业务重要性，优先开发以下模块的前端页面：
   - 高优先级：供应商管理、资产盘点、加班管理、晋升管理、转正管理、离职管理
   - 中优先级：外出管理、员工发展计划
   - 低优先级：会议通知、会议记录（可考虑集成到现有会议管理页面）、报表模板、定时报表

2. **页面开发建议**：
   - 参考现有页面的代码结构和组件使用方式
   - 使用统一的表格、表单、对话框等组件
   - 遵循现有的权限控制和操作日志记录方式

3. **API使用说明**：
   - 所有API文件已创建并导出
   - 可以在页面中直接 `import { ... } from '#/api/admin/supplier'` 等方式使用
   - API类型定义已完整，可提供良好的TypeScript类型提示

