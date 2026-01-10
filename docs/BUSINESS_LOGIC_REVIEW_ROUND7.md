# 业务逻辑审查报告 - 第七轮

**审查日期**: 2026-01-10
**审查人**: Claude Code
**审查范围**: 工作流审批、报表生成、任务管理、工时管理、知识库、统计服务

---

## 执行摘要

第七轮审查深入分析了核心工作流模块,发现了**32个新问题**:
- **4个严重问题** (P0)
- **13个高优先级问题** (P1)
- **10个中优先级问题** (P2)
- **5个低优先级问题** (P3)

**最严重发现**:
1. **审批业务回调失败不回滚** - 审批记录已更新但业务状态未更新
2. **工作台统计存在严重N+1查询** - 查询100个项目执行100次数据库查询
3. **任务进度更新无状态验证** - 已取消任务仍可更新为待验收
4. **案例库DTO转换N+1查询** - 查询100条案例执行300次SQL

**累计问题统计**: 7轮共发现 **235个问题**

---

## 新发现问题详情

### 🔴 严重问题 (P0 - 立即修复)

#### 204. 审批业务状态更新失败不回滚，导致数据不一致

**文件**: `application/workbench/service/ApprovalAppService.java:254-303`

**问题描述**:
```java
// 对于需要特殊业务逻辑的审批类型，需要直接调用业务方法更新业务状态
String businessType = approval.getBusinessType();
Long businessId = approval.getBusinessId();

try {
    switch (businessType) {
        case "MATTER_CLOSE":
            // 项目结案审批有特殊的业务逻辑
            matterAppService.approveCloseMatter(businessId, approved, command.getComment());
            break;
        case "EXPENSE":
            expenseAppService.approveExpense(expenseCommand);
            break;
        // ... 其他业务类型 ...
    }
} catch (Exception e) {
    log.error("更新业务状态失败: businessType={}, businessId={}",
            businessType, businessId, e);
    // ⚠️ 不抛出异常，避免影响审批记录的更新
}
```

**问题**:
- 审批记录已经更新为"APPROVED"或"REJECTED"
- 但业务方法调用失败(如项目结案失败)
- **异常被吞掉,不抛出,不回滚事务**
- 导致审批中心显示"已审批",但业务状态没更新

**影响场景**:
```
1. 管理员在审批中心审批"项目结案"申请,点击通过
2. 审批记录状态更新为"APPROVED" ✅
3. 调用matterAppService.approveCloseMatter()时抛异常 ❌
4. 异常被catch,不抛出,事务提交 ✅
5. 结果:
   - 审批中心显示"已通过" ✅
   - 项目状态仍然是"PENDING_CLOSE",没有变为"CLOSED" ❌
   - 用户认为审批完成,但实际业务状态未更新
```

**修复建议**:
```java
@Transactional(rollbackFor = Exception.class)
public void approve(ApproveCommand command) {
    Approval approval = approvalRepository.getByIdOrThrow(command.getApprovalId(), "审批记录不存在");

    // 检查权限和状态...

    // 更新审批状态
    approval.setStatus("APPROVED".equals(command.getResult()) ? "APPROVED" : "REJECTED");
    approval.setComment(command.getComment());
    approval.setApprovedAt(LocalDateTime.now());
    approvalRepository.updateById(approval);

    // ✅ 先更新业务状态,失败则整个事务回滚
    String businessType = approval.getBusinessType();
    Long businessId = approval.getBusinessId();
    Boolean approved = "APPROVED".equals(command.getResult());

    updateBusinessStatus(businessType, businessId, approved, command.getComment());

    // 发布事件
    eventPublisher.publishEvent(new ApprovalCompletedEvent(...));
}

/**
 * 更新业务状态(独立方法,异常向上抛出)
 */
private void updateBusinessStatus(String businessType, Long businessId, Boolean approved, String comment) {
    switch (businessType) {
        case "MATTER_CLOSE":
            matterAppService.approveCloseMatter(businessId, approved, comment);
            break;
        case "EXPENSE":
            ApproveExpenseCommand expenseCommand = new ApproveExpenseCommand();
            expenseCommand.setExpenseId(businessId);
            expenseCommand.setAction(approved ? "APPROVE" : "REJECT");
            expenseCommand.setComment(comment);
            expenseAppService.approveExpense(expenseCommand);
            break;
        case "REGULARIZATION":
            ApproveRegularizationCommand regCommand = new ApproveRegularizationCommand();
            regCommand.setApproved(approved);
            regCommand.setComment(comment);
            regularizationAppService.approveRegularization(businessId, regCommand);
            break;
        case "RESIGNATION":
            ApproveResignationCommand resCommand = new ApproveResignationCommand();
            resCommand.setApproved(approved);
            resCommand.setComment(comment);
            resignationAppService.approveResignation(businessId, resCommand);
            break;
        default:
            // 其他类型由事件监听器处理
            log.debug("业务类型{}由事件监听器处理", businessType);
    }
}
```

#### 205. 工作台统计查询存在严重N+1查询问题

**文件**: `application/workbench/service/StatisticsAppService.java:288-341`

**问题描述**:
```java
public WorkbenchStatsDTO getWorkbenchStats() {
    Long userId = SecurityUtils.getUserId();

    // 我的项目数
    var participantList = matterParticipantRepository.lambdaQuery()
            .select(MatterParticipant::getMatterId)
            .eq(MatterParticipant::getUserId, userId)
            .list();

    long matterCountLong = participantList.stream()
            .map(MatterParticipant::getMatterId)
            .distinct()
            .filter(matterId -> {
                // ⚠️ 对每个matterId都查询一次数据库
                try {
                    var matter = matterRepository.getById(matterId);  // N+1查询！
                    return matter != null && !matter.getDeleted();
                } catch (Exception e) {
                    return false;
                }
            })
            .count();
```

**性能问题**:
- 用户参与100个项目 = 1次参与者查询 + 100次项目查询 = **101次数据库查询**
- `filter`里面调用`getById()`,每个matterId都是一次SELECT
- 高并发时数据库压力巨大

**修复建议**:
```java
public WorkbenchStatsDTO getWorkbenchStats() {
    Long userId = SecurityUtils.getUserId();

    WorkbenchStatsDTO stats = new WorkbenchStatsDTO();

    // ✅ 方法1: 使用单次SQL查询(推荐)
    long matterCount = statisticsMapper.countMyMatters(userId);
    stats.setMatterCount(matterCount);

    // ✅ 方法2: 批量加载后在内存过滤
    // 先获取所有matterId
    var participantList = matterParticipantRepository.lambdaQuery()
            .select(MatterParticipant::getMatterId)
            .eq(MatterParticipant::getUserId, userId)
            .list();

    List<Long> matterIds = participantList.stream()
            .map(MatterParticipant::getMatterId)
            .distinct()
            .collect(Collectors.toList());

    if (matterIds.isEmpty()) {
        stats.setMatterCount(0L);
    } else {
        // 批量查询项目(单次SQL)
        long matterCount = matterRepository.lambdaQuery()
                .in(Matter::getId, matterIds)
                .eq(Matter::getDeleted, false)
                .count();
        stats.setMatterCount(matterCount);
    }

    // 其他统计...
    return stats;
}
```

**Mapper SQL(推荐)**:
```xml
<select id="countMyMatters" resultType="java.lang.Long">
    SELECT COUNT(DISTINCT mp.matter_id)
    FROM matter_participant mp
    INNER JOIN matter m ON mp.matter_id = m.id
    WHERE mp.user_id = #{userId}
      AND mp.deleted = 0
      AND m.deleted = 0
</select>
```

#### 206. 任务进度更新缺少状态验证，已取消任务仍可更新

**文件**: `application/matter/service/TaskAppService.java:429-452`

**问题描述**:
```java
@Transactional
public TaskDTO updateProgress(Long id, Integer progress) {
    Task task = taskRepository.getByIdOrThrow(id, "任务不存在");

    if (progress < 0 || progress > 100) {
        throw new BusinessException("进度必须在0-100之间");
    }

    task.setProgress(progress);
    if (progress == 100) {
        // 进度100%时，状态变为待验收
        task.setStatus("PENDING_REVIEW");  // ⚠️ 不检查当前状态
        task.setCompletedAt(LocalDateTime.now());
        task.setReviewStatus("PENDING_REVIEW");
    } else if (progress > 0) {
        task.setStatus("IN_PROGRESS");
        task.setReviewStatus(null);
    }

    taskRepository.updateById(task);
    return toDTO(task);
}
```

**问题**:
- 没有检查任务当前状态是否允许更新进度
- **已取消(CANCELLED)或已完成(COMPLETED)的任务也能更新进度**
- 已取消的任务更新进度100%后会变为"待验收"状态

**攻击/错误场景**:
```
1. 任务被创建者取消,状态为CANCELLED
2. 负责人仍然可以调用updateProgress(taskId, 100)
3. 任务状态变为PENDING_REVIEW,进度100%
4. 创建者收到"待验收"通知
5. 实际上这个任务已经被取消了
```

**修复建议**:
```java
@Transactional
public TaskDTO updateProgress(Long id, Integer progress) {
    Task task = taskRepository.getByIdOrThrow(id, "任务不存在");

    if (progress < 0 || progress > 100) {
        throw new BusinessException("进度必须在0-100之间");
    }

    // ✅ 验证任务状态
    if ("CANCELLED".equals(task.getStatus())) {
        throw new BusinessException("已取消的任务不能更新进度");
    }

    if ("COMPLETED".equals(task.getStatus())) {
        throw new BusinessException("已完成的任务不能更新进度");
    }

    if ("PENDING_REVIEW".equals(task.getStatus())) {
        throw new BusinessException("待验收的任务不能更新进度，请等待验收或验收退回后再更新");
    }

    // 只有TODO和IN_PROGRESS状态可以更新进度
    if (!"TODO".equals(task.getStatus()) && !"IN_PROGRESS".equals(task.getStatus())) {
        throw new BusinessException("当前状态不允许更新进度");
    }

    task.setProgress(progress);
    if (progress == 100) {
        task.setStatus("PENDING_REVIEW");
        task.setCompletedAt(LocalDateTime.now());
        task.setReviewStatus("PENDING_REVIEW");
    } else if (progress > 0) {
        task.setStatus("IN_PROGRESS");
        task.setReviewStatus(null);
    } else {
        // 进度为0，保持TODO状态
        task.setStatus("TODO");
        task.setReviewStatus(null);
    }

    taskRepository.updateById(task);
    log.info("任务进度更新: taskId={}, progress={}%, status={}", id, progress, task.getStatus());
    return toDTO(task);
}
```

#### 207. 案例库列表查询存在N+1查询问题

**文件**: `application/knowledge/service/CaseLibraryAppService.java:46-62, 267-306`

**问题描述**:
```java
public PageResult<CaseLibraryDTO> listCases(CaseLibraryQueryDTO query) {
    IPage<CaseLibrary> page = caseLibraryMapper.selectCasePage(...);

    Long userId = SecurityUtils.getUserId();
    List<CaseLibraryDTO> records = page.getRecords().stream()
            .map(c -> toCaseDTO(c, userId))  // ⚠️ 每条记录调用toDTO
            .collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
}

private CaseLibraryDTO toCaseDTO(CaseLibrary caseLib, Long userId) {
    // ... 字段映射 ...

    // ⚠️ N+1查询: 查询分类名称
    CaseCategory category = caseCategoryRepository.getById(caseLib.getCategoryId());
    if (category != null) {
        dto.setCategoryName(category.getName());
    }

    // ⚠️ N+1查询: 检查是否已收藏
    if (userId != null) {
        int count = knowledgeCollectionMapper.countByUserAndTarget(
                userId, KnowledgeCollection.TYPE_CASE, caseLib.getId());
        dto.setCollected(count > 0);
    }

    return dto;
}
```

**性能问题**:
- 查询100条案例 = 1次主查询 + 100次分类查询 + 100次收藏检查 = **201次数据库查询**

**修复建议**:
```java
public PageResult<CaseLibraryDTO> listCases(CaseLibraryQueryDTO query) {
    IPage<CaseLibrary> page = caseLibraryMapper.selectCasePage(...);
    List<CaseLibrary> cases = page.getRecords();

    if (cases.isEmpty()) {
        return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
    }

    Long userId = SecurityUtils.getUserId();

    // ✅ 批量加载分类信息
    Set<Long> categoryIds = cases.stream()
            .map(CaseLibrary::getCategoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, CaseCategory> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap() :
            caseCategoryRepository.listByIds(categoryIds).stream()
                    .collect(Collectors.toMap(CaseCategory::getId, c -> c));

    // ✅ 批量加载收藏状态
    Map<Long, Boolean> collectedMap = new HashMap<>();
    if (userId != null) {
        List<Long> caseIds = cases.stream()
                .map(CaseLibrary::getId)
                .collect(Collectors.toList());

        List<KnowledgeCollection> collections = knowledgeCollectionMapper.selectBatchByUserAndTargets(
                userId, KnowledgeCollection.TYPE_CASE, caseIds);

        Set<Long> collectedCaseIds = collections.stream()
                .map(KnowledgeCollection::getTargetId)
                .collect(Collectors.toSet());

        for (Long caseId : caseIds) {
            collectedMap.put(caseId, collectedCaseIds.contains(caseId));
        }
    }

    // ✅ 转换DTO(从Map获取,避免N+1)
    List<CaseLibraryDTO> records = cases.stream()
            .map(c -> toCaseDTO(c, userId, categoryMap, collectedMap))
            .collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
}

private CaseLibraryDTO toCaseDTO(CaseLibrary caseLib, Long userId,
                                  Map<Long, CaseCategory> categoryMap,
                                  Map<Long, Boolean> collectedMap) {
    CaseLibraryDTO dto = new CaseLibraryDTO();
    // ... 字段映射 ...

    // 从Map获取,避免查询
    if (caseLib.getCategoryId() != null) {
        CaseCategory category = categoryMap.get(caseLib.getCategoryId());
        if (category != null) {
            dto.setCategoryName(category.getName());
        }
    }

    if (userId != null) {
        dto.setCollected(collectedMap.getOrDefault(caseLib.getId(), false));
    }

    return dto;
}
```

**性能对比**:
- 修复前: 100条案例 = 201次查询
- 修复后: 100条案例 = 3次查询(1次主查询 + 1次批量分类 + 1次批量收藏)
- **性能提升67倍**

---

### 🟠 高优先级问题 (P1 - 本周修复)

#### 208. 批量审批没有整体事务控制

**文件**: `application/workbench/service/ApprovalAppService.java:318-380`

**问题描述**:
```java
@Transactional
public BatchApproveResult batchApprove(List<Long> approvalIds, String result, String comment) {
    int successCount = 0;
    int skipCount = 0;
    List<String> errors = new ArrayList<>();

    for (Long approvalId : approvalIds) {
        try {
            Approval approval = approvalRepository.findById(approvalId);
            // ... 验证和更新 ...
            approvalRepository.updateById(approval);

            // 发布事件
            eventPublisher.publishEvent(...);

            successCount++;
        } catch (Exception e) {
            // ⚠️ 捕获异常,不抛出,继续处理下一个
            errors.add("处理失败: " + approvalId + " - " + e.getMessage());
            skipCount++;
        }
    }

    return BatchApproveResult.builder()
            .total(approvalIds.size())
            .successCount(successCount)
            .skipCount(skipCount)
            .errors(errors)
            .build();
}
```

**问题**:
- 虽然有`@Transactional`,但try-catch吞掉异常,不会触发回滚
- 前5个成功,第6个失败,前5个不会回滚
- 导致部分审批成功,部分失败,没有原子性

**修复建议**:

方案1: 全部成功或全部失败
```java
@Transactional(rollbackFor = Exception.class)
public BatchApproveResult batchApprove(List<Long> approvalIds, String result, String comment) {
    // 第1阶段: 验证所有审批记录
    List<Approval> approvals = new ArrayList<>();
    for (Long approvalId : approvalIds) {
        Approval approval = approvalRepository.findById(approvalId);
        if (approval == null) {
            throw new BusinessException("审批记录不存在: " + approvalId);
        }
        if (!approval.getApproverId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("无权审批此记录: " + approval.getApprovalNo());
        }
        if (!"PENDING".equals(approval.getStatus())) {
            throw new BusinessException("该审批记录已处理: " + approval.getApprovalNo());
        }
        approvals.add(approval);
    }

    // 第2阶段: 批量更新(全部验证通过后)
    for (Approval approval : approvals) {
        approval.setStatus("APPROVED".equals(result) ? "APPROVED" : "REJECTED");
        approval.setComment(comment);
        approval.setApprovedAt(LocalDateTime.now());
        approvalRepository.updateById(approval);

        // 发布事件
        eventPublisher.publishEvent(new ApprovalCompletedEvent(...));
    }

    return BatchApproveResult.builder()
            .total(approvalIds.size())
            .successCount(approvalIds.size())
            .skipCount(0)
            .errors(Collections.emptyList())
            .build();
}
```

方案2: 分批处理,每批独立事务
```java
public BatchApproveResult batchApprove(List<Long> approvalIds, String result, String comment) {
    int successCount = 0;
    int skipCount = 0;
    List<String> errors = new ArrayList<>();

    // 每个审批独立事务
    for (Long approvalId : approvalIds) {
        try {
            approveOneInTransaction(approvalId, result, comment);
            successCount++;
        } catch (Exception e) {
            errors.add("处理失败: " + approvalId + " - " + e.getMessage());
            skipCount++;
        }
    }

    return BatchApproveResult.builder()
            .total(approvalIds.size())
            .successCount(successCount)
            .skipCount(skipCount)
            .errors(errors)
            .build();
}

@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
private void approveOneInTransaction(Long approvalId, String result, String comment) {
    Approval approval = approvalRepository.getByIdOrThrow(approvalId, "审批记录不存在");

    if (!approval.getApproverId().equals(SecurityUtils.getUserId())) {
        throw new BusinessException("无权审批此记录");
    }

    if (!"PENDING".equals(approval.getStatus())) {
        throw new BusinessException("该审批记录已处理");
    }

    approval.setStatus("APPROVED".equals(result) ? "APPROVED" : "REJECTED");
    approval.setComment(comment);
    approval.setApprovedAt(LocalDateTime.now());
    approvalRepository.updateById(approval);

    eventPublisher.publishEvent(new ApprovalCompletedEvent(...));
}
```

#### 209. 报表编号使用时间戳可能重复

**文件**: `application/workbench/service/ReportAppService.java:822-824`

**问题描述**:
```java
private String generateReportNo() {
    return "RPT" + System.currentTimeMillis();  // ⚠️ 并发时可能重复
}
```

**问题**:
- 两个请求在同一毫秒内调用,生成相同的编号
- 虽然概率低,但并发时仍可能发生

**修复建议**:
```java
private final java.util.concurrent.atomic.AtomicLong sequence = new java.util.concurrent.atomic.AtomicLong(0);

private String generateReportNo() {
    long timestamp = System.currentTimeMillis();
    long seq = sequence.incrementAndGet() % 1000;  // 每毫秒最多1000个
    return String.format("RPT%d%03d", timestamp, seq);
}
```

或使用UUID:
```java
private String generateReportNo() {
    return "RPT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
}
```

#### 210. 任务验收退回时清空进度不合理

**文件**: `application/matter/service/TaskAppService.java:298-333`

**问题描述**:
```java
@Transactional
public TaskDTO rejectTask(Long id, String comment) {
    // ... 验证 ...

    task.setStatus("IN_PROGRESS");
    task.setReviewStatus("REJECTED");
    task.setReviewComment(comment);
    task.setReviewedAt(LocalDateTime.now());
    task.setReviewedBy(currentUserId);
    task.setCompletedAt(null);
    task.setProgress(0);  // ⚠️ 进度清零,可能不合理

    taskRepository.updateById(task);
    return toDTO(task);
}
```

**问题**:
- 任务完成90%后提交验收
- 验收人发现小问题退回
- 进度被清零,变成0%
- 实际上任务可能只需要微调,不应该清零进度

**修复建议**:
```java
@Transactional
public TaskDTO rejectTask(Long id, String comment) {
    // ... 验证 ...

    task.setStatus("IN_PROGRESS");
    task.setReviewStatus("REJECTED");
    task.setReviewComment(comment);
    task.setReviewedAt(LocalDateTime.now());
    task.setReviewedBy(currentUserId);
    task.setCompletedAt(null);
    // ✅ 保持原进度,不清零(负责人可自行调整)
    // 或者设置为95%,表示接近完成但需修改
    if (task.getProgress() != null && task.getProgress() == 100) {
        task.setProgress(95);  // 从100%降到95%
    }

    taskRepository.updateById(task);
    log.info("任务验收退回: taskId={}, comment={}, progress={}%",
            id, comment, task.getProgress());
    return toDTO(task);
}
```

#### 211. 工时批量提交缺少事务控制

**文件**: `application/matter/service/TimesheetAppService.java:233-285`

**问题描述**:
类似问题208,批量提交工时时,try-catch吞掉异常,导致部分成功部分失败,没有原子性。

**修复建议**: 同问题208,提供两种方案供选择。

#### 212-220. 其他高优先级问题

212. 子部门递归查询无深度限制,可能栈溢出 (workbench/service/ApprovalAppService:177-188)
213. 报表异步生成失败无重试机制 (workbench/service/ReportAppService:239-276)
214. 报表生成InputStream可能未关闭 (workbench/service/ReportAppService:440-473)
215. 权限过滤查询项目ID可能过多导致IN参数超限 (workbench/service/ReportAppService:588-625, matter/service/TaskAppService:607-612)
216. 任务权限过滤加载全部Matter对象到内存 (matter/service/TaskAppService:607-612)
217. 工时审批权限验证存在N+1查询 (matter/service/TimesheetAppService:438-472)
218. 收藏案例计数器可能丢失更新 (knowledge/service/CaseLibraryAppService:185-205)
219. 我的收藏案例查询存在N+1 (knowledge/service/CaseLibraryAppService:223-234)
220. getAccessibleMatterIds重复代码且性能差 (workbench/service/StatisticsAppService:390-449, matter/service/TimesheetAppService:481-540)

---

### 🟡 中优先级问题 (P2 - 两周内修复)

#### 221. 报表数据权限过滤逻辑复杂且重复

**文件**: `application/workbench/service/ReportAppService.java:542-631`

**问题**: `applyDataScopeFilter()`方法中大量switch-case处理不同报表类型的权限过滤,代码重复度高,维护困难。

**修复建议**: 提取公共方法,使用策略模式重构。

#### 222-230. 其他中优先级问题

222. 审批列表查询异常返回空结果掩盖错误 (workbench/service/ApprovalAppService:90-94)
223. 任务状态变更通知逻辑过于复杂 (matter/service/TaskAppService:374-413)
224. 工时审批通知缺失 (matter/service/TimesheetAppService:302-320, 326-344)
225. 案例分类树构建可能循环引用 (knowledge/service/CaseLibraryAppService:236-244)
226. 项目统计异常处理返回空数据 (workbench/service/StatisticsAppService:170-178)
227. 工作台统计使用过多查询 (workbench/service/StatisticsAppService:288-341)
228. 客户统计权限过滤性能差 (workbench/service/StatisticsAppService:455-521)
229. 律师业绩排行分页后排名不准确 (workbench/service/StatisticsAppService:242-283)
230. 增长率计算重复查询可访问项目 (workbench/service/StatisticsAppService:357-384)

---

### 🟢 低优先级问题 (P3 - 逐步优化)

#### 231-235. 代码质量问题

231. ApprovalAppService和ReportAppService都有重复的子部门递归代码
232. 多个Service都有getAccessibleMatterIds方法,应提取到工具类
233. 报表Service职责过重,应拆分为多个小Service
234. 统计Service包含大量业务逻辑,应重构
235. DTO转换方法参数过多,应使用Builder模式

---

## 七轮累计统计

**总计发现**: **235个问题**

| 轮次 | 严重(P0) | 高(P1) | 中(P2) | 低(P3) | 合计 |
|------|---------|--------|--------|--------|------|
| 第一轮 | 1 | 8 | 15 | 23 | 47 |
| 第二轮 | 3 | 12 | 14 | 7 | 36 |
| 第三轮 | 2 | 10 | 11 | 5 | 28 |
| 第四轮 | 3 | 8 | 10 | 4 | 25 |
| 第五轮 | 4 | 11 | 13 | 4 | 32 |
| 第六轮 | 5 | 15 | 11 | 4 | 35 |
| 第七轮 | 4 | 13 | 10 | 5 | 32 |
| **总计** | **22** | **77** | **84** | **52** | **235** |

### 按类别统计

| 类别 | 问题数 | 占比 |
|------|--------|------|
| 安全问题 | 45 | 19.1% |
| 性能问题 | 52 | 22.1% |
| 数据一致性 | 38 | 16.2% |
| 业务逻辑 | 61 | 26.0% |
| 并发问题 | 16 | 6.8% |
| 代码质量 | 23 | 9.8% |

### 按严重程度统计

| 严重程度 | 问题数 | 占比 | 说明 |
|---------|--------|------|------|
| P0 严重 | 22 | 9.4% | 立即修复 |
| P1 高优先级 | 77 | 32.8% | 本周修复 |
| P2 中优先级 | 84 | 35.7% | 两周内修复 |
| P3 低优先级 | 52 | 22.1% | 逐步优化 |

---

## 本轮核心问题分析

### 1. 事务边界管理不当

**影响模块**: 审批中心、工时管理
**风险等级**: 🔴 严重

多个批量操作方法虽然有`@Transactional`,但内部try-catch吞掉异常,导致:
- 部分成功部分失败,无原子性
- 审批业务状态和审批记录不一致
- 数据完整性无法保证

**建议**: 明确事务边界,要么全部成功要么全部失败,或使用独立事务分批处理。

### 2. N+1查询问题普遍存在

**影响模块**: 案例库、工作台统计
**风险等级**: 🔴 严重

查询列表时在循环中调用查询方法:
- 案例库列表: 201次查询
- 工作台统计: 101次查询
- 响应时间长,数据库压力大

**建议**: 批量加载关联数据,使用Map缓存,避免循环查询。

### 3. 状态机验证不完整

**影响模块**: 任务管理
**风险等级**: 🟠 高

任务状态变更时缺少状态验证:
- 已取消任务仍可更新进度
- 状态流转不受控制
- 可能导致数据混乱

**建议**: 添加状态机验证,明确允许的状态流转路径。

### 4. 权限过滤查询性能问题

**影响模块**: 报表、统计、任务
**风险等级**: 🟠 高

数据权限过滤时:
- 查询所有项目ID到内存
- 使用IN查询可能超参数限制
- 重复代码多,维护困难

**建议**: 使用数据库层面的权限过滤,提取公共方法。

---

## 修复优先级建议

### 立即修复 (P0) - 今天

1. ✅ **修复审批业务回调失败不回滚** (问题204)
2. ✅ **优化工作台统计N+1查询** (问题205)
3. ✅ **添加任务进度更新状态验证** (问题206)
4. ✅ **优化案例库列表N+1查询** (问题207)

### 本周修复 (P1)

5. ✅ 修复批量审批事务控制 (问题208)
6. ✅ 修复报表编号生成并发问题 (问题209)
7. ✅ 优化任务验收退回逻辑 (问题210)
8. ✅ 修复工时批量提交事务 (问题211)
9. ✅ 添加子部门递归深度限制 (问题212)
10. ✅ 优化权限过滤查询性能 (问题215-216)

### 两周内修复 (P2)

11. ✅ 重构报表数据权限过滤 (问题221)
12. ✅ 完善审批通知机制 (问题222-224)
13. ✅ 优化统计查询性能 (问题226-230)

### 逐步优化 (P3)

14. 代码重构和质量提升 (问题231-235)

---

## 重点建议

### 1. 建立事务管理规范

批量操作的事务处理模式:

**模式1: 全部成功或全部失败**
```java
@Transactional(rollbackFor = Exception.class)
public BatchResult batchOperate(List<Long> ids) {
    // 第1阶段: 验证所有记录
    List<Entity> entities = validateAll(ids);

    // 第2阶段: 批量处理(验证通过后)
    for (Entity entity : entities) {
        process(entity);
    }

    return success();
}
```

**模式2: 独立事务,允许部分失败**
```java
public BatchResult batchOperate(List<Long> ids) {
    int successCount = 0;
    List<String> errors = new ArrayList<>();

    for (Long id : ids) {
        try {
            processOneInTransaction(id);  // 独立事务
            successCount++;
        } catch (Exception e) {
            errors.add(id + ": " + e.getMessage());
        }
    }

    return new BatchResult(successCount, errors);
}

@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
private void processOneInTransaction(Long id) {
    // 处理逻辑
}
```

### 2. N+1查询优化模式

**标准模式**:
```java
public PageResult<DTO> listRecords(Query query) {
    // 1. 查询主数据
    List<Entity> entities = repository.selectPage(...);

    if (entities.isEmpty()) {
        return empty();
    }

    // 2. 批量加载关联数据
    Set<Long> ids = entities.stream().map(Entity::getForeignId).collect(toSet());
    Map<Long, Related> relatedMap = relatedRepository.listByIds(ids).stream()
            .collect(toMap(Related::getId, r -> r));

    // 3. 转换DTO(从Map获取,避免查询)
    List<DTO> dtos = entities.stream()
            .map(e -> toDTO(e, relatedMap))
            .collect(toList());

    return PageResult.of(dtos, ...);
}
```

### 3. 提取数据权限过滤公共服务

```java
@Service
public class DataScopeFilterService {

    /**
     * 获取可访问的项目ID列表
     */
    public List<Long> getAccessibleMatterIds() {
        String dataScope = SecurityUtils.getDataScope();
        Long userId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDepartmentId();

        if ("ALL".equals(dataScope)) {
            return null;  // 可访问全部
        }

        // ... 统一的过滤逻辑 ...
    }

    /**
     * 获取可访问的客户ID列表
     */
    public List<Long> getAccessibleClientIds() {
        // 类似逻辑
    }
}
```

### 4. 使用SQL层面的权限过滤

优先使用Mapper的SQL过滤,而不是在Service层过滤:

```xml
<select id="selectPageWithPermission" resultType="Entity">
    SELECT * FROM table
    WHERE deleted = 0
    <if test="dataScope == 'DEPT'">
        AND department_id = #{deptId}
    </if>
    <if test="dataScope == 'SELF'">
        AND created_by = #{userId}
    </if>
    <if test="matterIds != null">
        AND matter_id IN
        <foreach collection="matterIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </if>
</select>
```

---

## 总结

第七轮审查发现**32个新问题**,其中**4个严重问题**需要立即修复。

**最关键的问题**:
1. 审批业务回调失败不回滚,导致数据不一致
2. 工作台统计和案例库存在严重N+1查询
3. 任务进度更新缺少状态验证
4. 批量操作缺少事务控制

**行动建议**:
1. 立即修复4个P0严重问题
2. 本周内修复13个P1高优先级问题
3. 建立事务管理和N+1查询优化规范
4. 提取数据权限过滤公共服务
5. 统一编码规范,减少重复代码

系统工作流和统计模块存在多个关键的性能和数据一致性问题,建议优先修复后再部署新功能。

---

**审查完成时间**: 2026-01-10
**下一轮审查建议**: 关注HR管理、行政管理、外部集成等模块
