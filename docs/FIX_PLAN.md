# 系统修复计划

## 制定日期
2026-01-15

## 计划版本
v1.0

---

## 一、问题评估总结

### 必须修复（影响核心功能或安全）

| ID | 问题 | 影响 | 优先级 | 预计工时 |
|----|------|------|--------|---------|
| P1-001 | 工资权限检查缺失 | 安全风险 | P0 | 2h |
| P1-002 | 合同变更同步逻辑未实现 | 财务数据准确性 | P0 | 4h |

### 建议修复（影响用户体验）

| ID | 问题 | 影响 | 优先级 | 预计工时 |
|----|------|------|--------|---------|
| P2-001 | 合同组件过大(4329行) | 维护困难 | P1 | 8h |
| P2-002 | @提醒通知未实现 | 协作效率 | P1 | 6h |
| P2-003 | 定时报表通知不完整 | 决策支持 | P2 | 4h |
| P2-004 | 客户冲突检查不完整 | 业务风险 | P1 | 6h |
| P2-005 | 案由管理前端缺失 | 功能不完整 | P2 | 4h |

### 可延后（不影响当前使用）

| ID | 问题 | 影响 | 优先级 | 预计工时 |
|----|------|------|--------|---------|
| P3-001 | console.log未清理 | 生产环境性能 | P3 | 2h |
| P3-002 | 定时器未清理 | 内存泄漏风险 | P3 | 1h |
| P3-003 | 缺少虚拟滚动 | 大数据量性能 | P3 | 6h |
| P3-004 | 财务佣金规则未实现 | 功能不完整 | P3 | 8h |
| P3-005 | 节假日未集成业务流程 | 期限计算准确性 | P3 | 4h |
| P3-006 | 企业微信未完全集成 | 通知渠道单一 | P3 | 6h |

---

## 二、修复计划

### 第一阶段：安全修复（P0 - 立即执行）

#### P1-001 工资权限检查缺失

**文件**: `backend/src/main/java/com/lawfirm/application/hr/service/PayrollAppService.java`

**问题描述**:
- 第213行：updatePayrollItem方法缺少权限检查
- 工资数据敏感，需要严格的权限控制

**修复方案**:
```java
// 在方法上添加权限注解
@PreAuthorize("hasAuthority('hr:payroll:edit')")
public PayrollDTO updatePayrollItem(Long id, UpdatePayrollItemCommand command) {
    // 检查是否有权限修改该员工工资
    checkPayrollEditPermission(id);
    // ... 现有代码
}
```

**验收标准**:
- [ ] 非授权用户无法修改工资数据
- [ ] 测试用例通过
- [ ] 代码审查通过

---

#### P1-002 合同变更同步逻辑未实现

**文件**: `backend/src/main/java/com/lawfirm/application/finance/service/FinanceContractAmendmentService.java`

**问题描述**:
- 第280行：部分同步逻辑未实现
- 合同变更后，已收款的付款计划未同步更新

**修复方案**:
```java
// 实现部分同步逻辑
private void syncPaymentPlanAfterAmendment(FinanceContract contract, ContractAmendment amendment) {
    // 1. 获取所有付款计划
    List<PaymentPlan> plans = paymentPlanRepository.findByContractId(contract.getId());

    // 2. 对已收款的计划保持不变，未收款的按新金额调整
    for (PaymentPlan plan : plans) {
        if (plan.getStatus() == PaymentStatus.PAID) {
            continue; // 已收款的不变
        }
        // 调整未收款计划的金额
        adjustPlanAmount(plan, amendment.getNewAmount());
    }
}
```

**验收标准**:
- [ ] 合同变更后，未收款的付款计划金额正确更新
- [ ] 已收款的记录保持不变
- [ ] 测试用例通过

---

### 第二阶段：功能完善（P1 - 本周内完成）

#### P2-004 客户冲突检查不完整

**文件**:
- `backend/src/main/java/com/lawfirm/application/client/service/ConflictCheckAppService.java`
- `frontend/apps/web-antd/src/api/client/conflict.ts`

**问题描述**:
- 前端有冲突检查界面，但后端只实现简单搜索
- 缺少真正的冲突检测算法

**修复方案**:
1. 后端实现完整冲突检测逻辑
2. 支持按客户名称、身份证号、企业信用代码检测
3. 记录冲突检查历史

**验收标准**:
- [ ] 能准确识别重复客户
- [ ] 冲突检查结果可追溯
- [ ] 前后端联调通过

---

#### P2-005 案由管理前端缺失

**新增文件**:
- `frontend/apps/web-antd/src/views/system/cause-of-action/index.vue`
- `frontend/apps/web-antd/src/api/system/cause-of-action.ts`

**问题描述**:
- 后端已实现案由管理Controller
- 前端缺少对应的管理界面

**修复方案**:
1. 创建案由管理页面
2. 实现案由的CRUD操作
3. 添加案由分类管理

**验收标准**:
- [ ] 案由管理页面可访问
- [ ] 可增删改查案由
- [ ] 与后端API对接正确

---

#### P2-002 @提醒通知未实现

**文件**: `backend/src/main/java/com/lawfirm/application/matter/service/TaskCommentAppService.java`

**问题描述**:
- 第54行：@提醒通知功能未实现
- 影响团队协作效率

**修复方案**:
1. 实现@提醒通知服务
2. 支持站内信通知
3. 记录通知发送状态

**验收标准**:
- [ ] @用户后能收到通知
- [ ] 通知内容正确
- [ ] 通知状态可追踪

---

### 第三阶段：代码优化（P2 - 本月内完成）

#### P2-001 合同组件过大(4329行)

**文件**: `frontend/apps/web-antd/src/views/matter/contract/index.vue`

**问题描述**:
- 单个组件文件过大，难以维护
- 建议拆分为多个子组件

**修复方案**:
```
matter/contract/
├── index.vue          # 主页面
├── components/
│   ├── ContractList.vue      # 合同列表
│   ├── ContractForm.vue      # 合同表单
│   ├── ContractDetail.vue    # 合同详情
│   ├── ContractAmendment.vue # 合同变更
│   └── PaymentPlan.vue       # 付款计划
```

**验收标准**:
- [ ] 组件拆分完成
- [ ] 功能保持不变
- [ ] 代码可读性提升

---

#### P2-003 定时报表通知不完整

**文件**: `backend/src/main/java/com/lawfirm/application/workbench/service/ScheduledReportAppService.java`

**问题描述**:
- 定时报表生成后，通知功能未完全实现
- 只有日志记录，缺少实际通知

**修复方案**:
1. 实现邮件通知功能
2. 添加通知状态跟踪
3. 实现通知失败重试机制

**验收标准**:
- [ ] 定时报表生成后能发送邮件通知
- [ ] 通知状态可查询
- [ ] 通知失败能重试

---

### 第四阶段：性能优化（P3 - 持续进行）

#### P3-001 console.log未清理

**范围**: 所有前端文件

**修复方案**:
1. 全局搜索console.log
2. 替换为专业日志系统或移除
3. 添加ESLint规则预防

**验收标准**:
- [ ] 生产环境无console.log
- [ ] 开发环境保留必要日志

---

#### P3-002 定时器未清理

**文件**: `frontend/apps/web-antd/src/layouts/basic.vue:343`

**修复方案**:
```typescript
// 添加清理逻辑
onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
});
```

**验收标准**:
- [ ] 组件销毁时清理定时器
- [ ] 无内存泄漏

---

#### P3-003 缺少虚拟滚动

**范围**: 大数据量列表（案件、客户、文档）

**修复方案**:
1. 使用vxe-table的虚拟滚动
2. 或使用vue-virtual-scroller
3. 实现分页加载优化

**验收标准**:
- [ ] 1000+数据滚动流畅
- [ ] 内存占用合理

---

## 三、实施时间表

### 本周（Week 1）- 已完成

| 优先级 | 任务 | 负责人 | 状态 |
|--------|------|--------|------|
| P0 | P1-001 工资权限检查 | Claude | ✅ 已完成 |
| P0 | P1-002 合同变更同步 | Claude | ✅ 已完成 |
| P1 | P2-004 客户冲突检查 | Claude | ✅ 已完成 |
| P1 | P2-005 案由管理前端 | Claude | ✅ 已完成 |
| P1 | P2-002 @提醒通知 | Claude | ✅ 已完成 |
| P1 | P2-001 合同组件拆分 | Claude | ✅ 已完成（独立组件+工具函数） |
| P2 | P2-003 定时报表通知 | Claude | ✅ 已完成 |

### 持续进行 - 已完成

| 优先级 | 任务 | 负责人 | 状态 |
|--------|------|--------|------|
| P3 | P3-001 console.log清理 | Claude | ✅ 已完成 |
| P3 | P3-002 定时器清理 | Claude | ✅ 已完成 |
| P3 | P3-003 虚拟滚动 | Claude | ✅ 已完成 |
| P3 | P3-004 佣金规则 | Claude | ✅ 已完成 |
| P3 | P3-005 节假日集成 | Claude | ✅ 已完成 |
| P3 | P3-006 企业微信集成 | Claude | ✅ 已完成 |

---

## 四、验收标准

### 安全修复
- [ ] 权限检查测试用例100%通过
- [ ] 安全审查通过

### 功能完善
- [ ] 新功能可用性测试通过
- [ ] 用户体验验收通过

### 代码质量
- [ ] 代码审查通过
- [ ] 单元测试覆盖率>80%
- [ ] ESLint检查无错误

---

## 五、风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 修改权限控制可能影响现有功能 | 高 | 充分测试，灰度发布 |
| 组件拆分可能引入新bug | 中 | 保留原代码备份，逐步迁移 |
| 新增功能可能增加系统复杂度 | 低 | 遵循现有架构规范 |

---

## 六、备注

- 所有修复需要经过测试环境验证
- 重大修改需要代码审查
- 修复完成后更新本文档状态

---

**计划制定人**: Claude Opus 4.5
**制定日期**: 2026-01-15
**最后更新**: 2026-01-15（全部任务已完成 ✅）
