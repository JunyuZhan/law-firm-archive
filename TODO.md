# 律师事务所管理系统 - 待办事项

> 完整历史记录和已修复问题详情请查看 [TODO-ARCHIVED.md](./TODO-ARCHIVED.md)

## 📊 问题统计汇总（2026-02-12 更新）

| 优先级 | 总数 | 已修复 | 待处理 |
|--------|------|--------|--------|
| 🔴高 | 5 | 5 | 0 |
| 🟡中 | 27 | 25 | 2 |
| 🟢低 | 13 | - | 13（可暂缓）|

---

## 🔴 高优先级待处理

无

---

## 🟡 中优先级待处理

无需立即处理的性能优化项，详见低优先级列表。

---

## 🟢 低优先级（可暂缓）

| 任务 | 描述 | 备注 |
|------|------|------|
| 71 | ApprovalAppService 批量审批事件发布时机 | 已有事务保护 |
| 74 | 缓存使用不足 | 性能优化建议 |
| 78 | MatterAppService 详情页N+1查询 | 单条记录影响小 |
| 24 | Vue watch deep 性能问题 | 当前数据量可接受 |
| 其他 | 见 TODO-ARCHIVED.md 中标记为 🟢低 的任务 | |

---

## 🔄 部分修复（中优先级剩余项）

以下任务已完成主要修复，仅剩少量低优先级子项：

| 任务 | 描述 | 剩余项 |
|------|------|--------|
| 8 | 资源泄漏与参数校验 | ExcelReportGenerator 部分方法 |
| 13 | 权限与认证安全 | OnlyOffice 回调校验（复杂改动）|
| 14 | 数据库与事务 | DataHandoverService 批量优化 |
| 18 | 前端竞态条件 | 2处并发请求去重 |

---

## ⏳ 待部署

以下任务代码已完成，等待部署到生产环境：

1. **任务1**：模板菜单位置调整（出函模板/合同模板）
2. **任务2**：函件模板示例数据

---

## ✅ 已修复摘要（详情见 TODO-ARCHIVED.md）

### 高优先级（5/5 完成）
- ~~任务65~~：全量数据加载 → SQL过滤/LIMIT 1
- ~~任务82~~：SQL注入防护 → Mapper.xml 使用 `#{}`
- ~~任务84~~：数据库连接安全 → 日志无密码泄露
- ~~任务92~~：通知已读权限 → 添加权限校验
- ~~任务86/94~~：定时任务无分布式锁 → 创建 `DistributedLockService`，所有14个定时任务添加分布式锁保护

### 中优先级（25/27 完成）
- ~~任务66~~：竞态条件 → DuplicateKeyException 处理
- ~~任务72~~：默认密钥 → SecurityConfigValidator 启动检查
- ~~任务76~~：DocumentAppService 循环更新 → `listByIds` + `updateBatchById` 批量处理
- ~~任务79~~：ExpenseAppService N+1查询 → `batchConvertToCostAllocationDTO/SplitDTO` 批量加载
- ~~任务80~~：审计日志 → @OperationLog 注解和切面
- ~~任务83~~：异步任务 → AsyncConfig 线程池配置
- ~~任务87-91~~：多服务N+1优化
- ~~任务95-100~~：Timesheet/Contract/CaseStudy/QualityCheck/ScheduledReport/Payroll N+1优化

### 安全问题（任务4）
- ✅ 硬编码密钥 → @Value 配置注入
- ✅ 路径遍历 → UUID 格式验证
- ✅ ThreadLocal 泄漏 → ThreadLocalCleanupFilter
- ✅ refreshToken 存储 → sessionStorage
- ✅ v-html XSS → sanitizeHtml 防护

### 代码质量（任务5-27）
- ✅ 数组越界/空指针检查
- ✅ 资源泄漏修复（InputStream/Workbook）
- ✅ 批量接口参数限制（@Size）
- ✅ 日志敏感信息脱敏
- ✅ 前端定时器/事件监听清理
