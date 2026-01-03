# API接口清单补充（新增模块）

> 版本：1.1 | 更新日期：2026年1月3日
> 
> 本文档补充10-API接口清单.md中未包含的新增模块接口。

---

## 十二、行政后勤模块

### 12.1 考勤管理

**路径**: `/api/attendance`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/attendance/check-in` | 签到 | 需认证 |
| POST | `/attendance/check-out` | 签退 | 需认证 |
| GET | `/attendance/today` | 今日考勤 | 需认证 |
| GET | `/attendance/list` | 考勤记录列表 | `attendance:list` |
| GET | `/attendance/statistics` | 考勤统计 | `attendance:statistics` |

### 12.2 请假管理

**路径**: `/api/leave`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/leave/types` | 请假类型列表 | 需认证 |
| GET | `/leave/balance` | 假期余额 | 需认证 |
| GET | `/leave/list` | 请假记录列表 | `leave:list` |
| POST | `/leave` | 提交请假申请 | `leave:create` |
| GET | `/leave/{id}` | 请假详情 | `leave:list` |
| POST | `/leave/{id}/cancel` | 取消请假 | `leave:update` |
| POST | `/leave/{id}/approve` | 审批通过 | `leave:approve` |
| POST | `/leave/{id}/reject` | 审批拒绝 | `leave:approve` |

### 12.3 会议室管理

**路径**: `/api/meeting-room`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/meeting-room/list` | 会议室列表 | `meeting:list` |
| GET | `/meeting-room/{id}` | 会议室详情 | `meeting:list` |
| POST | `/meeting-room` | 创建会议室 | `meeting:create` |
| PUT | `/meeting-room/{id}` | 更新会议室 | `meeting:update` |
| DELETE | `/meeting-room/{id}` | 删除会议室 | `meeting:delete` |
| GET | `/meeting-room/bookings` | 预约列表 | `meeting:booking:list` |
| POST | `/meeting-room/book` | 预约会议室 | `meeting:booking:create` |
| POST | `/meeting-room/booking/{id}/cancel` | 取消预约 | `meeting:booking:update` |
| GET | `/meeting-room/{id}/schedule` | 会议室日程 | `meeting:list` |
| GET | `/meeting-room/available` | 可用会议室 | `meeting:list` |

### 12.4 资产管理

**路径**: `/api/asset`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/asset/list` | 资产列表 | `asset:list` |
| GET | `/asset/{id}` | 资产详情 | `asset:list` |
| POST | `/asset` | 创建资产 | `asset:create` |
| PUT | `/asset/{id}` | 更新资产 | `asset:update` |
| DELETE | `/asset/{id}` | 删除资产 | `asset:delete` |
| POST | `/asset/{id}/receive` | 资产领用 | `asset:receive` |
| POST | `/asset/{id}/return` | 资产归还 | `asset:return` |
| POST | `/asset/{id}/scrap` | 资产报废 | `asset:scrap` |
| GET | `/asset/{id}/records` | 资产记录 | `asset:list` |
| GET | `/asset/statistics` | 资产统计 | `asset:statistics` |
| GET | `/asset/categories` | 资产分类 | `asset:list` |
| POST | `/asset/inventory` | 资产盘点 | `asset:inventory` |

### 12.5 采购管理

**路径**: `/api/purchase`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/purchase/list` | 采购申请列表 | `purchase:list` |
| GET | `/purchase/{id}` | 采购申请详情 | `purchase:list` |
| POST | `/purchase` | 创建采购申请 | `purchase:create` |
| PUT | `/purchase/{id}` | 更新采购申请 | `purchase:update` |
| DELETE | `/purchase/{id}` | 删除采购申请 | `purchase:delete` |
| POST | `/purchase/{id}/submit` | 提交审批 | `purchase:submit` |
| POST | `/purchase/{id}/approve` | 审批通过 | `purchase:approve` |
| POST | `/purchase/{id}/reject` | 审批拒绝 | `purchase:approve` |
| POST | `/purchase/{id}/receive` | 入库登记 | `purchase:receive` |
| GET | `/purchase/{id}/items` | 采购明细 | `purchase:list` |
| GET | `/purchase/statistics` | 采购统计 | `purchase:statistics` |
| GET | `/purchase/pending` | 待审批列表 | `purchase:approve` |

### 12.6 供应商管理

**路径**: `/api/supplier`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/supplier/list` | 供应商列表 | `supplier:list` |
| GET | `/supplier/{id}` | 供应商详情 | `supplier:list` |
| POST | `/supplier` | 创建供应商 | `supplier:create` |
| PUT | `/supplier/{id}` | 更新供应商 | `supplier:update` |
| DELETE | `/supplier/{id}` | 删除供应商 | `supplier:delete` |
| PUT | `/supplier/{id}/status` | 更新状态 | `supplier:update` |
| GET | `/supplier/statistics` | 供应商统计 | `supplier:statistics` |

---

## 十三、人力资源模块

### 13.1 员工档案

**路径**: `/api/hr/employee`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/hr/employee/list` | 员工列表 | `hr:employee:list` |
| GET | `/hr/employee/{id}` | 员工详情 | `hr:employee:list` |
| POST | `/hr/employee` | 创建员工 | `hr:employee:create` |
| PUT | `/hr/employee/{id}` | 更新员工 | `hr:employee:update` |
| DELETE | `/hr/employee/{id}` | 删除员工 | `hr:employee:delete` |
| PUT | `/hr/employee/{id}/status` | 更新状态 | `hr:employee:update` |

### 13.2 劳动合同

**路径**: `/api/hr/contract`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/hr/contract/list` | 合同列表 | `hr:contract:list` |
| GET | `/hr/contract/{id}` | 合同详情 | `hr:contract:list` |
| POST | `/hr/contract` | 创建合同 | `hr:contract:create` |
| PUT | `/hr/contract/{id}` | 更新合同 | `hr:contract:update` |
| DELETE | `/hr/contract/{id}` | 删除合同 | `hr:contract:delete` |
| POST | `/hr/contract/{id}/renew` | 续签合同 | `hr:contract:renew` |
| GET | `/hr/contract/expiring` | 即将到期 | `hr:contract:list` |

### 13.3 培训管理

**路径**: `/api/hr/training`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/hr/training/list` | 培训列表 | `hr:training:list` |
| GET | `/hr/training/available` | 可报名培训 | 需认证 |
| GET | `/hr/training/{id}` | 培训详情 | 需认证 |
| POST | `/hr/training` | 创建培训 | `hr:training:create` |
| POST | `/hr/training/{id}/publish` | 发布培训 | `hr:training:edit` |
| POST | `/hr/training/{id}/cancel` | 取消培训 | `hr:training:edit` |
| POST | `/hr/training/{id}/enroll` | 报名培训 | 需认证 |
| POST | `/hr/training/{id}/cancel-enrollment` | 取消报名 | 需认证 |
| GET | `/hr/training/my-records` | 我的培训记录 | 需认证 |
| GET | `/hr/training/my-credits` | 我的学分统计 | 需认证 |
| GET | `/hr/training/{id}/participants` | 培训参与者 | `hr:training:list` |

### 13.4 绩效考核

**路径**: `/api/hr/performance`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/hr/performance/tasks` | 考核任务列表 | `hr:performance:list` |
| GET | `/hr/performance/tasks/{id}` | 考核任务详情 | `hr:performance:detail` |
| POST | `/hr/performance/tasks` | 创建考核任务 | `hr:performance:create` |
| POST | `/hr/performance/tasks/{id}/start` | 启动考核任务 | `hr:performance:edit` |
| POST | `/hr/performance/tasks/{id}/complete` | 完成考核任务 | `hr:performance:edit` |
| GET | `/hr/performance/tasks/{id}/statistics` | 考核任务统计 | `hr:performance:detail` |
| GET | `/hr/performance/indicators` | 考核指标列表 | `hr:performance:list` |
| POST | `/hr/performance/indicators` | 创建考核指标 | `hr:performance:create` |
| PUT | `/hr/performance/indicators/{id}` | 更新考核指标 | `hr:performance:edit` |
| DELETE | `/hr/performance/indicators/{id}` | 删除考核指标 | `hr:performance:delete` |
| POST | `/hr/performance/evaluations` | 提交绩效评价 | 需认证 |
| GET | `/hr/performance/evaluations` | 员工评价记录 | 需认证 |
| GET | `/hr/performance/evaluations/pending` | 待评价记录 | 需认证 |
| GET | `/hr/performance/evaluations/{id}` | 评价详情 | 需认证 |

### 13.5 晋升管理

**路径**: `/api/hr/promotion`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/hr/promotion/levels` | 职级列表 | `hr:level:list` |
| GET | `/hr/promotion/levels/{id}` | 职级详情 | `hr:level:view` |
| GET | `/hr/promotion/levels/category/{category}` | 按类别获取职级 | 需认证 |
| POST | `/hr/promotion/levels` | 创建职级 | `hr:level:create` |
| PUT | `/hr/promotion/levels/{id}` | 更新职级 | `hr:level:edit` |
| DELETE | `/hr/promotion/levels/{id}` | 删除职级 | `hr:level:delete` |
| POST | `/hr/promotion/levels/{id}/enable` | 启用职级 | `hr:level:edit` |
| POST | `/hr/promotion/levels/{id}/disable` | 停用职级 | `hr:level:edit` |
| GET | `/hr/promotion/applications` | 晋升申请列表 | `hr:promotion:list` |
| GET | `/hr/promotion/applications/{id}` | 晋升申请详情 | `hr:promotion:view` |
| POST | `/hr/promotion/applications` | 提交晋升申请 | `hr:promotion:apply` |
| POST | `/hr/promotion/applications/{id}/cancel` | 取消晋升申请 | `hr:promotion:apply` |
| POST | `/hr/promotion/applications/review` | 提交评审 | `hr:promotion:review` |
| POST | `/hr/promotion/applications/{id}/approve` | 审批通过 | `hr:promotion:approve` |
| POST | `/hr/promotion/applications/{id}/reject` | 审批拒绝 | `hr:promotion:approve` |
| GET | `/hr/promotion/applications/pending-count` | 待审批数量 | 需认证 |

### 13.6 发展规划

**路径**: `/api/hr/development-plans`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/hr/development-plans` | 发展规划列表 | `hr:plan:list` |
| GET | `/hr/development-plans/{id}` | 规划详情 | `hr:plan:view` |
| GET | `/hr/development-plans/my-current` | 我的当年规划 | 需认证 |
| POST | `/hr/development-plans` | 创建发展规划 | `hr:plan:create` |
| PUT | `/hr/development-plans/{id}` | 更新发展规划 | `hr:plan:edit` |
| DELETE | `/hr/development-plans/{id}` | 删除发展规划 | `hr:plan:delete` |
| POST | `/hr/development-plans/{id}/submit` | 提交规划 | `hr:plan:edit` |
| POST | `/hr/development-plans/{id}/review` | 审核规划 | `hr:plan:review` |
| POST | `/hr/development-plans/milestones/{milestoneId}/status` | 更新里程碑状态 | `hr:plan:edit` |

---

## 十四、知识库模块

### 14.1 法规库

**路径**: `/api/knowledge/law`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/knowledge/law/categories` | 法规分类树 | 需认证 |
| GET | `/knowledge/law` | 法规列表 | 需认证 |
| GET | `/knowledge/law/{id}` | 法规详情 | 需认证 |
| POST | `/knowledge/law` | 创建法规 | `knowledge:law:create` |
| PUT | `/knowledge/law/{id}` | 更新法规 | `knowledge:law:edit` |
| DELETE | `/knowledge/law/{id}` | 删除法规 | `knowledge:law:delete` |
| POST | `/knowledge/law/{id}/collect` | 收藏法规 | 需认证 |
| DELETE | `/knowledge/law/{id}/collect` | 取消收藏 | 需认证 |
| GET | `/knowledge/law/collected` | 我的收藏法规 | 需认证 |

### 14.2 案例库

**路径**: `/api/knowledge/case`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/knowledge/case/categories` | 案例分类树 | 需认证 |
| GET | `/knowledge/case` | 案例列表 | 需认证 |
| GET | `/knowledge/case/{id}` | 案例详情 | 需认证 |
| POST | `/knowledge/case` | 创建案例 | `knowledge:case:create` |
| PUT | `/knowledge/case/{id}` | 更新案例 | `knowledge:case:edit` |
| DELETE | `/knowledge/case/{id}` | 删除案例 | `knowledge:case:delete` |
| POST | `/knowledge/case/{id}/collect` | 收藏案例 | 需认证 |
| DELETE | `/knowledge/case/{id}/collect` | 取消收藏 | 需认证 |
| GET | `/knowledge/case/collected` | 我的收藏案例 | 需认证 |

### 14.3 经验分享

**路径**: `/api/knowledge/article`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/knowledge/article` | 文章列表 | 需认证 |
| GET | `/knowledge/article/{id}` | 文章详情 | 需认证 |
| POST | `/knowledge/article` | 创建文章 | 需认证 |
| PUT | `/knowledge/article/{id}` | 更新文章 | 需认证 |
| DELETE | `/knowledge/article/{id}` | 删除文章 | 需认证 |
| POST | `/knowledge/article/{id}/publish` | 发布文章 | 需认证 |
| POST | `/knowledge/article/{id}/archive` | 归档文章 | 需认证 |
| POST | `/knowledge/article/{id}/like` | 点赞文章 | 需认证 |
| GET | `/knowledge/article/my` | 我的文章 | 需认证 |

---

## 十五、工作台模块

### 15.1 个人工作台

**路径**: `/api/workbench`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/workbench/data` | 工作台数据 | 需认证 |
| GET | `/workbench/todo/summary` | 待办统计 | 需认证 |
| GET | `/workbench/todo/list` | 待办列表 | 需认证 |
| GET | `/workbench/project/summary` | 项目统计 | 需认证 |
| GET | `/workbench/project/recent` | 最近项目 | 需认证 |
| GET | `/workbench/schedule/today` | 今日日程 | 需认证 |
| GET | `/workbench/timesheet/summary` | 工时统计 | 需认证 |
| GET | `/workbench/notification/unread-count` | 未读消息数 | 需认证 |

### 15.2 审批中心

**路径**: `/api/workbench/approval`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/workbench/approval/list` | 审批记录列表 | `approval:list` |
| GET | `/workbench/approval/pending` | 待审批列表 | `approval:list` |
| GET | `/workbench/approval/my-initiated` | 我发起的审批 | `approval:list` |
| GET | `/workbench/approval/{id}` | 审批详情 | `approval:list` |
| POST | `/workbench/approval/approve` | 审批操作 | `approval:approve` |
| POST | `/workbench/approval/batch-approve` | 批量审批 | `approval:approve` |
| GET | `/workbench/approval/business` | 业务审批记录 | `approval:list` |

### 15.3 统计中心

**路径**: `/api/workbench/statistics`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/workbench/statistics/revenue` | 收入统计 | `statistics:view` |
| GET | `/workbench/statistics/matter` | 项目统计 | `statistics:view` |
| GET | `/workbench/statistics/client` | 客户统计 | `statistics:view` |
| GET | `/workbench/statistics/lawyer-performance` | 律师业绩排行 | `statistics:view` |

### 15.4 报表中心

**路径**: `/api/workbench/report`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/workbench/report/available` | 可用报表列表 | `report:list` |
| GET | `/workbench/report` | 报表记录列表 | `report:list` |
| GET | `/workbench/report/{id}` | 报表详情 | `report:detail` |
| POST | `/workbench/report/generate` | 生成报表 | `report:generate` |
| GET | `/workbench/report/{id}/download-url` | 获取下载URL | `report:download` |
| DELETE | `/workbench/report/{id}` | 删除报表 | `report:delete` |

### 15.5 报表模板

**路径**: `/api/report-templates`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/report-templates` | 模板列表 | `report:template:list` |
| GET | `/report-templates/{id}` | 模板详情 | `report:template:view` |
| POST | `/report-templates` | 创建模板 | `report:template:create` |
| PUT | `/report-templates/{id}` | 更新模板 | `report:template:edit` |
| DELETE | `/report-templates/{id}` | 删除模板 | `report:template:delete` |
| POST | `/report-templates/{id}/enable` | 启用模板 | `report:template:edit` |
| POST | `/report-templates/{id}/disable` | 停用模板 | `report:template:edit` |
| POST | `/report-templates/{id}/generate` | 根据模板生成报表 | `report:generate` |
| GET | `/report-templates/data-sources` | 可用数据源列表 | 需认证 |
| GET | `/report-templates/data-sources/{dataSource}/fields` | 数据源字段 | 需认证 |

### 15.6 定时报表

**路径**: `/api/scheduled-reports`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/scheduled-reports` | 定时任务列表 | `report:scheduled:list` |
| GET | `/scheduled-reports/{id}` | 任务详情 | `report:scheduled:view` |
| POST | `/scheduled-reports` | 创建定时任务 | `report:scheduled:create` |
| PUT | `/scheduled-reports/{id}` | 更新定时任务 | `report:scheduled:edit` |
| DELETE | `/scheduled-reports/{id}` | 删除定时任务 | `report:scheduled:delete` |
| POST | `/scheduled-reports/{id}/enable` | 启用任务 | `report:scheduled:edit` |
| POST | `/scheduled-reports/{id}/pause` | 暂停任务 | `report:scheduled:edit` |
| POST | `/scheduled-reports/{id}/execute` | 立即执行 | `report:scheduled:execute` |
| GET | `/scheduled-reports/{id}/logs` | 执行记录 | `report:scheduled:view` |

---

## 十六、OCR识别模块

**路径**: `/api/ocr`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/ocr/text` | 通用文字识别 | 需认证 |
| POST | `/ocr/text/url` | 通用文字识别(URL) | 需认证 |
| POST | `/ocr/bank-receipt` | 银行回单识别 | 需认证 |
| POST | `/ocr/bank-receipt/url` | 银行回单识别(URL) | 需认证 |
| POST | `/ocr/id-card` | 身份证识别 | 需认证 |
| POST | `/ocr/id-card/url` | 身份证识别(URL) | 需认证 |
| POST | `/ocr/business-license` | 营业执照识别 | 需认证 |
| POST | `/ocr/business-license/url` | 营业执照识别(URL) | 需认证 |

---

## 📊 新增模块接口统计

| 模块 | 接口数 | 说明 |
|------|--------|------|
| 行政后勤-考勤管理 | 5 | 签到、签退、考勤统计 |
| 行政后勤-请假管理 | 8 | 请假申请、审批、假期余额 |
| 行政后勤-会议室管理 | 10 | 会议室CRUD、预约管理 |
| 行政后勤-资产管理 | 12 | 资产CRUD、领用、归还、报废 |
| 行政后勤-采购管理 | 12 | 采购申请、审批、入库 |
| 行政后勤-供应商管理 | 7 | 供应商CRUD、状态管理 |
| 人力资源-员工档案 | 6 | 员工CRUD、状态管理 |
| 人力资源-劳动合同 | 7 | 合同CRUD、续签 |
| 人力资源-培训管理 | 11 | 培训CRUD、报名、学分 |
| 人力资源-绩效考核 | 14 | 考核任务、指标、评价 |
| 人力资源-晋升管理 | 16 | 职级管理、晋升申请、评审 |
| 人力资源-发展规划 | 9 | 规划CRUD、里程碑 |
| 知识库-法规库 | 9 | 法规CRUD、收藏 |
| 知识库-案例库 | 9 | 案例CRUD、收藏 |
| 知识库-经验分享 | 9 | 文章CRUD、发布、点赞 |
| 工作台-个人工作台 | 8 | 待办、项目、日程、工时 |
| 工作台-审批中心 | 7 | 审批列表、审批操作 |
| 工作台-统计中心 | 4 | 收入、项目、客户、业绩统计 |
| 工作台-报表中心 | 6 | 报表生成、下载 |
| 工作台-报表模板 | 10 | 模板CRUD、数据源 |
| 工作台-定时报表 | 9 | 定时任务CRUD、执行记录 |
| OCR识别 | 8 | 文字、回单、身份证、营业执照 |
| **新增合计** | **186** | **本文档新增接口** |

---

## 📝 与原清单合并统计

| 来源 | 接口数 |
|------|--------|
| 原API接口清单（10-API接口清单.md） | 145+ |
| 本补充清单新增 | 186 |
| **系统总计** | **331+** |

---

**最后更新**: 2026-01-03  
**维护人**: 后端开发团队