# 业务逻辑审查报告 - 第二十二轮

**审查日期**: 2026-01-10
**审查人**: Claude Code
**审查范围**: 工作台 - 工作台数据、统计分析、报表生成

---

## 执行摘要

第二十二轮审查深入分析了工作台业务模块,发现了**25个新问题**:
- **2个严重问题** (P0)
- **14个高优先级问题** (P1)
- **8个中优先级问题** (P2)
- **1个低优先级问题** (P3)

**最严重发现**:
1. ~~**报表查询和下载缺少权限验证** - 任何人都可以查看和下载他人的报表~~ ✅ 已修复
2. ~~**律师业绩排行存在N+1查询** - 循环查询每个律师的提成数据~~ ✅ 已修复

**累计问题统计**: 22轮共发现 **576个问题**

---

## 🎉 修复进度 (2026-01-10)

| 问题编号 | 问题描述 | 状态 |
|---------|---------|------|
| 552 | 报表查询和下载缺少权限验证 | ✅ 已修复 |
| 553 | 律师业绩排行存在N+1查询 | ✅ 已修复 |
| 554 | 获取可访问项目ID时重复查询 | ✅ 已修复 |
| 555 | 异常处理过于宽泛返回空数据 | ✅ 已修复 |
| 556 | applyDataScopeFilter权限过滤查询次数过多 | ✅ 已修复 |
| 557 | 删除报表记录没有权限验证 | ✅ 已修复 |
| 558 | getAccessibleClientIds重复查询逻辑 | ✅ 已修复 |
| 561 | listReports查询没有严格权限验证 | ✅ 已修复 |
| 568 | getAvailableReports返回Map缺少类型安全 | ✅ 已修复 |

**修复统计**: 已修复 **9个问题** (2个P0 + 6个P1 + 1个P2)

---

## 新发现问题详情

### 🔴 严重问题 (P0 - 立即修复)

#### 552. ✅ [已修复] 报表查询和下载缺少权限验证

**文件**: `workbench/service/ReportAppService.java:193-195, 337-356`

**问题描述**:
```java
public ReportDTO getReportById(Long id) {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");
    return toDTO(report);
    // ⚠️ 没有权限验证，任何人都可以查看任何报表
}

public String getReportDownloadUrl(Long id) throws Exception {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");

    if (!"COMPLETED".equals(report.getStatus())) {
        throw new BusinessException("报表尚未生成完成");
    }

    // ⚠️ 没有权限验证，任何人都可以下载任何报表文件

    // 从URL提取对象名称
    String objectName = minioService.extractObjectName(report.getFileUrl());
    if (objectName == null) {
        throw new BusinessException("无效的文件URL");
    }

    // 生成预签名URL（有效期1小时）
    return minioService.getPresignedUrl(objectName, 3600);
}
```

**问题**:
1. **任何人都可以查看报表记录** - 包括报表类型、参数等敏感信息
2. **任何人都可以下载报表文件** - 可能包含财务、业绩等敏感数据
3. **没有检查报表所属人** - 用户A可以下载用户B的报表

**严重后果**:
- 普通律师可以查看财务报表、利润分析等敏感数据
- 员工可以查看他人的业绩排行
- 数据权限控制完全失效

**修复建议**:
```java
public ReportDTO getReportById(Long id) {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");

    // ✅ 权限验证：只能查看自己生成的报表，或有ALL权限
    Long currentUserId = SecurityUtils.getUserId();
    if (!report.getGeneratedBy().equals(currentUserId)) {
        String dataScope = SecurityUtils.getDataScope();
        if (!"ALL".equals(dataScope)) {
            throw new BusinessException("权限不足：只能查看自己生成的报表");
        }
        log.warn("跨用户查看报表: reportId={}, owner={}, viewer={}",
                id, report.getGeneratedBy(), currentUserId);
    }

    return toDTO(report);
}

public String getReportDownloadUrl(Long id) throws Exception {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");

    // ✅ 权限验证
    Long currentUserId = SecurityUtils.getUserId();
    if (!report.getGeneratedBy().equals(currentUserId)) {
        String dataScope = SecurityUtils.getDataScope();
        if (!"ALL".equals(dataScope)) {
            throw new BusinessException("权限不足：只能下载自己生成的报表");
        }
        log.warn("跨用户下载报表: reportId={}, owner={}, downloader={}",
                id, report.getGeneratedBy(), currentUserId);
    }

    if (!"COMPLETED".equals(report.getStatus())) {
        throw new BusinessException("报表尚未生成完成");
    }

    if (report.getFileUrl() == null || report.getFileUrl().isEmpty()) {
        throw new BusinessException("报表文件不存在");
    }

    // 从URL提取对象名称
    String objectName = minioService.extractObjectName(report.getFileUrl());
    if (objectName == null) {
        throw new BusinessException("无效的文件URL");
    }

    // ✅ 记录下载审计
    log.info("下载报表: reportNo={}, type={}, downloader={}",
            report.getReportNo(), report.getReportType(), currentUserId);

    // 生成预签名URL（有效期1小时）
    return minioService.getPresignedUrl(objectName, 3600);
}
```

#### 553. ✅ [已修复] 律师业绩排行存在N+1查询

**文件**: `workbench/service/StatisticsAppService.java:243-283`

**问题描述**:
```java
public List<StatisticsDTO.LawyerPerformance> getLawyerPerformanceRanking(Integer limit) {
    // 根据用户权限过滤数据
    String dataScope = com.lawfirm.common.util.SecurityUtils.getDataScope();
    Long currentUserId = com.lawfirm.common.util.SecurityUtils.getUserId();
    Long deptId = com.lawfirm.common.util.SecurityUtils.getDepartmentId();

    // 获取可访问的项目ID列表
    List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);

    List<Map<String, Object>> rankings = statisticsMapper.getLawyerPerformanceRanking(limit, accessibleMatterIds);

    List<StatisticsDTO.LawyerPerformance> result = new ArrayList<>();
    int rank = 1;
    for (Map<String, Object> item : rankings) {
        if (item == null) continue;
        Object lawyerIdObj = item.get("lawyer_id");
        if (lawyerIdObj == null) continue;
        Long lawyerId = ((Number) lawyerIdObj).longValue();
        StatisticsDTO.LawyerPerformance performance = new StatisticsDTO.LawyerPerformance();
        performance.setLawyerId(lawyerId);
        performance.setLawyerName((String) item.get("lawyer_name"));
        Object matterCountObj = item.get("matter_count");
        performance.setMatterCount(matterCountObj != null ? ((Number) matterCountObj).longValue() : 0L);
        Object revenueObj = item.get("total_revenue");
        performance.setRevenue(revenueObj != null ? new BigDecimal(revenueObj.toString()) : BigDecimal.ZERO);

        // ⚠️ N+1查询: 循环查询每个律师的提成
        BigDecimal commission = commissionRepository.sumCommissionByUserId(lawyerId);
        performance.setCommission(commission != null ? commission : BigDecimal.ZERO);

        // 从工时表统计工时（已审批的工时）
        BigDecimal totalHours = (BigDecimal) item.get("total_hours");
        performance.setHours(totalHours != null ? totalHours.doubleValue() : 0.0);

        performance.setRank(rank++);
        result.add(performance);
    }

    log.info("获取律师业绩排行: count={}, dataScope={}", result.size(), dataScope);
    return result;
}
```

**性能影响**:
- 查询10个律师的业绩 = 1次主查询 + 10次提成查询 = **11次数据库查询**

**修复建议**:
```java
public List<StatisticsDTO.LawyerPerformance> getLawyerPerformanceRanking(Integer limit) {
    // 根据用户权限过滤数据
    String dataScope = com.lawfirm.common.util.SecurityUtils.getDataScope();
    Long currentUserId = com.lawfirm.common.util.SecurityUtils.getUserId();
    Long deptId = com.lawfirm.common.util.SecurityUtils.getDepartmentId();

    // 获取可访问的项目ID列表
    List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);

    List<Map<String, Object>> rankings = statisticsMapper.getLawyerPerformanceRanking(limit, accessibleMatterIds);

    if (rankings.isEmpty()) {
        return new ArrayList<>();
    }

    // ✅ 批量加载所有律师的提成数据
    Set<Long> lawyerIds = rankings.stream()
            .map(item -> item.get("lawyer_id"))
            .filter(Objects::nonNull)
            .map(id -> ((Number) id).longValue())
            .collect(Collectors.toSet());

    Map<Long, BigDecimal> commissionMap = lawyerIds.isEmpty() ? Collections.emptyMap() :
            commissionRepository.sumCommissionByUserIds(new ArrayList<>(lawyerIds));

    // 转换DTO（从Map获取）
    List<StatisticsDTO.LawyerPerformance> result = new ArrayList<>();
    int rank = 1;
    for (Map<String, Object> item : rankings) {
        if (item == null) continue;
        Object lawyerIdObj = item.get("lawyer_id");
        if (lawyerIdObj == null) continue;
        Long lawyerId = ((Number) lawyerIdObj).longValue();

        StatisticsDTO.LawyerPerformance performance = new StatisticsDTO.LawyerPerformance();
        performance.setLawyerId(lawyerId);
        performance.setLawyerName((String) item.get("lawyer_name"));
        Object matterCountObj = item.get("matter_count");
        performance.setMatterCount(matterCountObj != null ? ((Number) matterCountObj).longValue() : 0L);
        Object revenueObj = item.get("total_revenue");
        performance.setRevenue(revenueObj != null ? new BigDecimal(revenueObj.toString()) : BigDecimal.ZERO);

        // ✅ 从Map获取提成，避免查询
        BigDecimal commission = commissionMap.get(lawyerId);
        performance.setCommission(commission != null ? commission : BigDecimal.ZERO);

        BigDecimal totalHours = (BigDecimal) item.get("total_hours");
        performance.setHours(totalHours != null ? totalHours.doubleValue() : 0.0);

        performance.setRank(rank++);
        result.add(performance);
    }

    log.info("获取律师业绩排行: count={}, dataScope={}", result.size(), dataScope);
    return result;
}

// Repository中添加批量查询方法:
public Map<Long, BigDecimal> sumCommissionByUserIds(List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
        return Collections.emptyMap();
    }

    List<Map<String, Object>> results = commissionMapper.sumCommissionGroupByUserId(userIds);

    return results.stream()
            .collect(Collectors.toMap(
                    item -> ((Number) item.get("user_id")).longValue(),
                    item -> (BigDecimal) item.get("total_commission")
            ));
}
```

**性能对比**:
- 修复前: 10个律师 = 11次查询
- 修复后: 10个律师 = 2次查询(1次主查询 + 1次批量提成)
- **性能提升5.5倍**

---

### 🟠 高优先级问题 (P1 - 本周修复)

#### 554. ✅ [已修复] 获取可访问项目ID时重复查询

**文件**: `workbench/service/StatisticsAppService.java:387-446`

**问题描述**:
```java
private List<Long> getAccessibleMatterIds(String dataScope, Long currentUserId, Long deptId) {
    if ("ALL".equals(dataScope)) {
        return null; // null表示可以访问所有项目
    }

    List<Long> matterIds = new ArrayList<>();

    if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
        // 部门及下级部门：查询本部门及下级部门的项目
        // TODO: 需要实现部门递归查询  // ⚠️ 递归未实现
        matterIds = matterRepository.lambdaQuery()  // ⚠️ 第1次查询
                .select(com.lawfirm.domain.matter.entity.Matter::getId)
                .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                .eq(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptId)
                .list()
                .stream()
                .map(com.lawfirm.domain.matter.entity.Matter::getId)
                .collect(Collectors.toList());
    } else if ("DEPT".equals(dataScope) && deptId != null) {
        // 本部门：查询本部门的项目
        matterIds = matterRepository.lambdaQuery()  // ⚠️ 第2次查询
                .select(com.lawfirm.domain.matter.entity.Matter::getId)
                .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                .eq(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptId)
                .list()
                .stream()
                .map(com.lawfirm.domain.matter.entity.Matter::getId)
                .collect(Collectors.toList());
    } else {
        // SELF：只查看自己负责的项目或参与的项目
        // 查询自己负责的项目
        List<Long> leadMatterIds = matterRepository.lambdaQuery()  // ⚠️ 第3次查询
                .select(com.lawfirm.domain.matter.entity.Matter::getId)
                .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                .eq(com.lawfirm.domain.matter.entity.Matter::getLeadLawyerId, currentUserId)
                .list()
                .stream()
                .map(com.lawfirm.domain.matter.entity.Matter::getId)
                .collect(Collectors.toList());

        // 查询自己参与的项目
        var participantList = matterParticipantRepository.lambdaQuery()  // ⚠️ 第4次查询
                .select(com.lawfirm.domain.matter.entity.MatterParticipant::getMatterId)
                .eq(com.lawfirm.domain.matter.entity.MatterParticipant::getUserId, currentUserId)
                .eq(com.lawfirm.domain.matter.entity.MatterParticipant::getDeleted, false)
                .list();

        List<Long> participantMatterIds = participantList.stream()
                .map(com.lawfirm.domain.matter.entity.MatterParticipant::getMatterId)
                .distinct()
                .collect(Collectors.toList());

        // 合并去重
        matterIds.addAll(leadMatterIds);
        matterIds.addAll(participantMatterIds);
        matterIds = matterIds.stream().distinct().collect(Collectors.toList());
    }

    return matterIds.isEmpty() ? Collections.emptyList() : matterIds;
}
```

**问题**:
1. **每次调用都查询数据库** - 方法被多次调用（getRevenueStats、getMatterStats、getLawyerPerformanceRanking、calculateGrowthRate）
2. **同一个请求中重复查询** - 例如getRevenueStats中调用3次getAccessibleMatterIds
3. **DEPT_AND_CHILD的递归查询未实现** - 代码中有TODO

**修复建议**:
```java
// ✅ 使用ThreadLocal缓存（同一请求内）
private static final ThreadLocal<Map<String, List<Long>>> MATTER_IDS_CACHE = new ThreadLocal<>();

private List<Long> getAccessibleMatterIds(String dataScope, Long currentUserId, Long deptId) {
    // ✅ 检查缓存
    String cacheKey = dataScope + "_" + currentUserId + "_" + deptId;
    Map<String, List<Long>> cache = MATTER_IDS_CACHE.get();
    if (cache != null && cache.containsKey(cacheKey)) {
        return cache.get(cacheKey);
    }

    if ("ALL".equals(dataScope)) {
        return null; // null表示可以访问所有项目
    }

    List<Long> matterIds = calculateAccessibleMatterIds(dataScope, currentUserId, deptId);

    // ✅ 放入缓存
    if (cache == null) {
        cache = new HashMap<>();
        MATTER_IDS_CACHE.set(cache);
    }
    cache.put(cacheKey, matterIds);

    return matterIds;
}

private List<Long> calculateAccessibleMatterIds(String dataScope, Long currentUserId, Long deptId) {
    // 原有逻辑...
}

// ✅ 请求结束后清理缓存（可在Filter或Interceptor中调用）
public static void clearMatterIdsCache() {
    MATTER_IDS_CACHE.remove();
}
```

#### 555. ✅ [已修复] 异常处理过于宽泛返回空数据

**文件**: `workbench/service/WorkbenchAppService.java:41-65, 70-97`

**问题描述**:
```java
public WorkbenchDTO getWorkbenchData() {
    try {
        Long userId = SecurityUtils.getCurrentUserId();

        return WorkbenchDTO.builder()
                .todoSummary(getTodoSummary(userId))
                .projectSummary(getProjectSummary(userId))
                .timesheetSummary(getTimesheetSummary(userId))
                .todoItems(getTodoItems(userId))
                .todaySchedules(getTodaySchedules(userId))
                .recentProjects(getRecentProjects(userId))
                .build();
    } catch (Exception e) {  // ⚠️ 捕获所有异常
        log.error("获取工作台数据失败", e);
        // 返回空数据，避免500错误  // ⚠️ 隐藏了真实错误
        return WorkbenchDTO.builder()
                .todoSummary(TodoSummary.builder()...build())
                .projectSummary(ProjectSummary.builder()...build())
                // ...
                .build();
    }
}

public TodoSummary getTodoSummary(Long userId) {
    try {
        // 待审批数量
        int pendingApproval = approvalRepository.countPendingByApproverId(userId);
        // ...
    } catch (Exception e) {  // ⚠️ 捕获所有异常
        log.error("获取待办事项统计失败: userId={}", userId, e);
        // 返回空统计，避免500错误  // ⚠️ 隐藏了真实错误
        return TodoSummary.builder()
                .pendingApproval(0)
                // ...
                .build();
    }
}
```

**问题**:
1. **捕获所有异常** - 包括数据库连接失败等严重问题
2. **返回空数据隐藏错误** - 用户看到0数据,但实际是系统故障
3. **无法区分真正的空数据和错误** - 影响问题诊断

**修复建议**:
```java
public WorkbenchDTO getWorkbenchData() {
    Long userId = SecurityUtils.getCurrentUserId();

    // ✅ 不捕获异常,让Spring统一处理
    // 如果真的需要容错,只捕获特定的非致命异常
    WorkbenchDTO.WorkbenchDTOBuilder builder = WorkbenchDTO.builder();

    try {
        builder.todoSummary(getTodoSummary(userId));
    } catch (Exception e) {
        log.error("获取待办事项统计失败", e);
        builder.todoSummary(TodoSummary.builder()
                .pendingApproval(0).pendingTask(0).overdueTask(0).total(0)
                .error("数据加载失败")
                .build());
    }

    try {
        builder.projectSummary(getProjectSummary(userId));
    } catch (Exception e) {
        log.error("获取项目统计失败", e);
        builder.projectSummary(ProjectSummary.builder()
                .inProgress(0).pendingApproval(0).closed(0).total(0)
                .error("数据加载失败")
                .build());
    }

    // ... 其他部分类似处理 ...

    return builder.build();
}
```

#### 556. ✅ [已修复] applyDataScopeFilter中权限过滤查询数据库次数过多

**文件**: `workbench/service/ReportAppService.java:542-631`

**问题描述**:
```java
private void applyDataScopeFilter(Map<String, Object> parameters, String reportType) {
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    // ...

    switch (reportType) {
        // ...
        case "LAWYER_PERFORMANCE" -> {
            // 律师业绩报表：根据数据范围过滤项目
            if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
                List<Long> deptIds = getAllChildDepartmentIds(deptId);  // ⚠️ 递归查询所有子部门
                deptIds.add(deptId);
                // 查询符合条件的项目ID列表
                List<Long> matterIds = matterRepository.lambdaQuery()  // ⚠️ 查询项目
                        .select(com.lawfirm.domain.matter.entity.Matter::getId)
                        .in(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptIds)
                        .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                        .list()
                        .stream()
                        .map(com.lawfirm.domain.matter.entity.Matter::getId)
                        .collect(Collectors.toList());
                parameters.put("matterIds", matterIds);
            } else if ("DEPT".equals(dataScope) && deptId != null) {
                List<Long> matterIds = matterRepository.lambdaQuery()  // ⚠️ 又一次查询项目
                        .select(com.lawfirm.domain.matter.entity.Matter::getId)
                        .eq(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptId)
                        // ...
            } else if ("SELF".equals(dataScope)) {
                List<Long> leadMatterIds = matterRepository.lambdaQuery()  // ⚠️ 再一次查询项目
                        .select(com.lawfirm.domain.matter.entity.Matter::getId)
                        // ...
            }
        }
        // ...
    }
}

private List<Long> getAllChildDepartmentIds(Long parentId) {
    List<Long> result = new ArrayList<>();
    collectChildDeptIds(parentId, result);
    return result;
}

private void collectChildDeptIds(Long parentId, List<Long> result) {
    try {
        List<Long> childIds = approvalMapper.selectChildDeptIds(parentId);  // ⚠️ 递归查询每一层
        if (childIds != null && !childIds.isEmpty()) {
            result.addAll(childIds);
            for (Long childId : childIds) {
                collectChildDeptIds(childId, result);  // ⚠️ 递归调用，每层都查数据库
            }
        }
    } catch (Exception e) {
        log.warn("查询子部门失败: parentId={}", parentId, e);
    }
}
```

**问题**:
1. **递归查询子部门** - 每层都查一次数据库，3层部门=3次查询
2. **每个报表类型都查项目** - 代码重复
3. **可以改为一次性查询所有部门的树形结构**

**修复建议**:
```java
// ✅ 使用CTE或递归SQL一次性查询所有子部门
private List<Long> getAllChildDepartmentIds(Long parentId) {
    // 使用PostgreSQL的递归查询
    return approvalMapper.selectAllDescendantDeptIds(parentId);
}

// Mapper中:
@Select("WITH RECURSIVE dept_tree AS (" +
        "  SELECT id FROM department WHERE parent_id = #{parentId} AND deleted = 0" +
        "  UNION ALL" +
        "  SELECT d.id FROM department d" +
        "  INNER JOIN dept_tree dt ON d.parent_id = dt.id" +
        "  WHERE d.deleted = 0" +
        ") SELECT id FROM dept_tree")
List<Long> selectAllDescendantDeptIds(@Param("parentId") Long parentId);

// ✅ 或使用缓存避免重复查询
private static final Map<Long, List<Long>> DEPT_CHILDREN_CACHE = new ConcurrentHashMap<>();

private List<Long> getAllChildDepartmentIds(Long parentId) {
    return DEPT_CHILDREN_CACHE.computeIfAbsent(parentId, this::queryChildDeptIds);
}
```

#### 557. ✅ [已修复] 删除报表记录没有权限验证

**文件**: `workbench/service/ReportAppService.java:362-379`

**问题**: 任何人都可以删除任何报表记录。

**修复建议**: 添加权限验证，只允许报表创建者或管理员删除。

#### 558-567. 其他高优先级问题

558. ✅ [已修复] getAccessibleClientIds重复查询逻辑 (StatisticsAppService:452-518)
559. 待办事项查询硬编码limit为5 (WorkbenchAppService:145, 161)
560. calculateGrowthRate重复调用getAccessibleMatterIds (StatisticsAppService:354-381) - ThreadLocal缓存已解决
561. ✅ [已修复] listReports查询没有严格权限验证 (ReportAppService:174-188)
562. generateReportFile中报表编号重复生成 (ReportAppService:463)
563. 报表异步生成失败只记录日志 (ReportAppService:268-276)
564. collectChildDeptIds捕获所有异常只记录warn (ReportAppService:642-653) - 已改为递归CTE查询
565. 获取客户统计时accessibleClientIds为null会查所有 (StatisticsAppService:199-205)
566. ✅ [已修复] 部门递归查询未实现 (StatisticsAppService:108, 395, 463) - 添加了selectAllDescendantDeptIds
567. 报表参数转JSON失败只记录warn (ReportAppService:425-435)

---

### 🟡 中优先级问题 (P2 - 两周内修复)

#### 568. ✅ [已修复] getAvailableReports返回Map缺少类型安全

**文件**: `workbench/service/ReportAppService.java:61-169`

**问题描述**:
```java
public List<Map<String, Object>> getAvailableReports() {
    List<Map<String, Object>> reports = new ArrayList<>();  // ⚠️ 返回Map，缺少类型安全

    // 收入报表
    Map<String, Object> revenueReport = new HashMap<>();
    revenueReport.put("type", "REVENUE");
    revenueReport.put("name", "收入报表");
    revenueReport.put("description", "统计收入情况，支持按时间、客户、案件等维度");
    revenueReport.put("formats", List.of("EXCEL", "PDF"));
    reports.add(revenueReport);

    // ... 重复10多次类似代码

    return reports;
}
```

**问题**: 返回Map缺少类型安全，应该定义DTO。

**修复建议**:
```java
@Data
@Builder
public class AvailableReportDTO {
    private String type;
    private String name;
    private String description;
    private List<String> formats;
}

public List<AvailableReportDTO> getAvailableReports() {
    List<AvailableReportDTO> reports = new ArrayList<>();

    reports.add(AvailableReportDTO.builder()
            .type("REVENUE")
            .name("收入报表")
            .description("统计收入情况，支持按时间、客户、案件等维度")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // ...

    return reports;
}
```

#### 569. 报表编号生成使用AtomicLong可能重复

**文件**: `workbench/service/ReportAppService.java:820-831`

**问题描述**:
```java
// 用于生成唯一报表编号的原子计数器
private static final java.util.concurrent.atomic.AtomicLong reportSequence =
        new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());

private String generateReportNo() {
    String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    long seq = reportSequence.incrementAndGet() % 10000;  // ⚠️ 取模可能重复
    return String.format("RPT%s%04d", date, seq);
}
```

**问题**:
- 服务重启后从新的时间戳开始
- 多实例部署时可能重复
- 取模10000后循环,同一天生成超过10000个报表会重复

**修复建议**: 使用数据库序列或雪花算法。

#### 570-575. 其他中优先级问题

570. submitReportGeneration和generateReport方法重复逻辑 (ReportAppService:202-324)
571. 查询报表数据switch-case过长 (ReportAppService:487-533)
572. 状态名称转换使用switch表达式 (ReportAppService:410-417)
573. 报表类型名称转换使用switch表达式 (ReportAppService:836-854)
574. 获取工作台数据没有权限验证 (WorkbenchAppService:41-65)
575. 删除报表文件失败只记录warn (ReportAppService:367-374)

---

### 🟢 低优先级问题 (P3 - 逐步优化)

#### 576. 代码重复：多个query*Data方法结构相似

**文件**: `workbench/service/ReportAppService.java:657-892`

**问题**: queryRevenueData、queryMatterData等方法结构相似,可以抽取公共逻辑。

---

## 二十二轮累计统计

**总计发现**: **576个问题**

| 轮次 | 严重(P0) | 高(P1) | 中(P2) | 低(P3) | 合计 |
|------|---------|--------|--------|--------|------|
| 第一轮 | 1 | 8 | 15 | 23 | 47 |
| 第二轮 | 3 | 12 | 14 | 7 | 36 |
| 第三轮 | 2 | 10 | 11 | 5 | 28 |
| 第四轮 | 3 | 8 | 10 | 4 | 25 |
| 第五轮 | 4 | 11 | 13 | 4 | 32 |
| 第六轮 | 5 | 15 | 11 | 4 | 35 |
| 第七轮 | 4 | 13 | 10 | 5 | 32 |
| 第八轮 | 3 | 11 | 10 | 4 | 28 |
| 第九轮 | 2 | 10 | 10 | 4 | 26 |
| 第十轮 | 2 | 8 | 9 | 3 | 22 |
| 第十一轮 | 3 | 12 | 10 | 3 | 28 |
| 第十二轮 | 2 | 10 | 9 | 3 | 24 |
| 第十三轮 | 3 | 8 | 8 | 2 | 21 |
| 第十四轮 | 2 | 6 | 8 | 2 | 18 |
| 第十五轮 | 3 | 8 | 9 | 2 | 22 |
| 第十六轮 | 3 | 9 | 10 | 2 | 24 |
| 第十七轮 | 1 | 8 | 10 | 2 | 21 |
| 第十八轮 | 1 | 7 | 9 | 2 | 19 |
| 第十九轮 | 0 | 8 | 8 | 2 | 18 |
| 第二十轮 | 3 | 11 | 6 | 2 | 22 |
| 第二十一轮 | 3 | 12 | 7 | 1 | 23 |
| 第二十二轮 | 2 | 14 | 8 | 1 | 25 |
| **总计** | **55** | **219** | **215** | **87** | **576** |

### 按类别统计

| 类别 | 问题数 | 占比 |
|------|--------|------|
| 安全问题 | 124 | 21.5% |
| 性能问题 | 137 | 23.8% |
| 数据一致性 | 86 | 14.9% |
| 业务逻辑 | 136 | 23.6% |
| 并发问题 | 34 | 5.9% |
| 代码质量 | 59 | 10.2% |

### 按严重程度统计

| 严重程度 | 问题数 | 占比 | 说明 |
|---------|--------|------|------|
| P0 严重 | 55 | 9.5% | 立即修复 |
| P1 高优先级 | 219 | 38.0% | 本周修复 |
| P2 中优先级 | 215 | 37.3% | 两周内修复 |
| P3 低优先级 | 87 | 15.1% | 逐步优化 |

---

## 本轮核心问题分析

### 1. 权限验证严重缺失

**影响模块**: 报表管理
**风险等级**: 🔴 严重

报表查询和下载缺少权限验证:
- 任何人都可以查看任何报表记录
- 任何人都可以下载任何报表文件
- 财务、业绩等敏感数据可能泄露

**建议**: 立即添加权限验证，只允许报表创建者或管理员访问。

### 2. N+1查询问题持续存在

**影响模块**: 统计分析
**风险等级**: 🔴 严重

律师业绩排行存在N+1查询:
- 循环查询每个律师的提成数据
- 10个律师 = 11次查询

**建议**: 使用批量查询优化。

### 3. 权限过滤查询性能差

**影响模块**: 统计分析、报表生成
**风险等级**: 🟠 高

多处权限过滤查询数据库次数过多:
- getAccessibleMatterIds每次调用都查数据库
- collectChildDeptIds递归查询每一层
- applyDataScopeFilter中每个报表类型都查项目

**建议**: 使用缓存和批量查询优化。

### 4. 异常处理过于宽泛

**影响模块**: 工作台数据
**风险等级**: 🟠 高

多处捕获所有异常返回空数据:
- 隐藏了真实错误
- 无法区分真正的空数据和错误
- 影响问题诊断

**建议**: 只捕获特定的非致命异常，或添加错误标识。

---

## 修复优先级建议

### 立即修复 (P0) - 今天

1. **添加报表查询和下载权限验证** (问题552)
2. **优化律师业绩排行N+1查询** (问题553)

### 本周修复 (P1)

3. 优化getAccessibleMatterIds重复查询 (问题554)
4. 改进异常处理机制 (问题555)
5. 优化applyDataScopeFilter权限过滤查询 (问题556)
6. 添加删除报表权限验证 (问题557)
7. 完善其他高优先级问题 (问题558-567)

### 两周内修复 (P2)

8. 改进返回类型安全 (问题568)
9. 修复报表编号生成 (问题569)
10. 完善其他业务逻辑 (问题570-575)

### 逐步优化 (P3)

11. 提取公共代码,减少重复 (问题576)

---

## 重点建议

### 1. 报表权限验证

```java
// ✅ 统一权限验证
private void validateReportAccess(Report report) {
    Long currentUserId = SecurityUtils.getUserId();
    if (!report.getGeneratedBy().equals(currentUserId)) {
        String dataScope = SecurityUtils.getDataScope();
        if (!"ALL".equals(dataScope)) {
            throw new BusinessException("权限不足：只能访问自己生成的报表");
        }
    }
}

public ReportDTO getReportById(Long id) {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");
    validateReportAccess(report);
    return toDTO(report);
}

public String getReportDownloadUrl(Long id) throws Exception {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");
    validateReportAccess(report);
    // ... 生成下载URL ...
}
```

### 2. N+1查询优化标准

```java
// ✅ 批量加载模式
public List<DTO> getList() {
    // 1. 主查询
    List<Entity> entities = repository.query();

    // 2. 批量加载关联数据
    Set<Long> foreignIds = collectIds(entities);
    Map<Long, Related> relatedMap = batchLoad(foreignIds);

    // 3. 转换DTO（从Map获取）
    return entities.stream()
            .map(e -> toDTO(e, relatedMap))
            .collect(toList());
}
```

### 3. 权限过滤查询优化

```java
// ✅ 使用ThreadLocal缓存（同一请求内）
private static final ThreadLocal<Map<String, List<Long>>> CACHE = new ThreadLocal<>();

private List<Long> getAccessibleMatterIds(String dataScope, Long userId, Long deptId) {
    String cacheKey = dataScope + "_" + userId + "_" + deptId;
    Map<String, List<Long>> cache = CACHE.get();

    if (cache != null && cache.containsKey(cacheKey)) {
        return cache.get(cacheKey);
    }

    List<Long> matterIds = calculateMatterIds(dataScope, userId, deptId);

    if (cache == null) {
        cache = new HashMap<>();
        CACHE.set(cache);
    }
    cache.put(cacheKey, matterIds);

    return matterIds;
}

// 请求结束后清理
public static void clearCache() {
    CACHE.remove();
}
```

### 4. 递归查询优化

```java
// ❌ 错误: 递归调用每层查数据库
private void collectChildDeptIds(Long parentId, List<Long> result) {
    List<Long> childIds = mapper.selectChildDeptIds(parentId);  // 查询
    result.addAll(childIds);
    for (Long childId : childIds) {
        collectChildDeptIds(childId, result);  // 递归，又查询
    }
}

// ✅ 正确: 使用递归SQL一次性查询
@Select("WITH RECURSIVE dept_tree AS (" +
        "  SELECT id FROM department WHERE parent_id = #{parentId} AND deleted = 0" +
        "  UNION ALL" +
        "  SELECT d.id FROM department d" +
        "  INNER JOIN dept_tree dt ON d.parent_id = dt.id" +
        "  WHERE d.deleted = 0" +
        ") SELECT id FROM dept_tree")
List<Long> selectAllDescendantDeptIds(@Param("parentId") Long parentId);
```

---

## 总结

第二十二轮审查发现**25个新问题**,其中**2个严重问题**需要立即修复。

**最关键的问题**:
1. ~~报表查询和下载缺少权限验证~~ ✅ 已修复
2. ~~律师业绩排行存在N+1查询~~ ✅ 已修复
3. ~~权限过滤查询数据库次数过多~~ ✅ 已修复
4. ~~异常处理过于宽泛隐藏错误~~ ✅ 已修复

**行动建议**:
1. ~~立即修复2个P0严重问题~~ ✅ 已完成
2. ~~本周内修复14个P1高优先级问题~~ 部分完成 (6/14)
3. ~~统一报表权限验证机制~~ ✅ 已完成
4. ~~优化N+1查询和权限过滤~~ ✅ 已完成
5. ~~改进异常处理策略~~ ✅ 已完成
6. ~~使用缓存减少重复查询~~ ✅ 已完成

~~系统工作台和报表模块存在严重的权限验证缺失和性能问题,**特别是报表权限验证问题极其严重**,建议立即修复P0问题后再允许生产使用。~~

✅ **已修复核心问题**: 报表权限验证、N+1查询、缓存优化、递归查询优化等关键问题已修复。

---

**审查完成时间**: 2026-01-10
**修复完成时间**: 2026-01-10
**修复内容**:
- ✅ 添加报表查询/下载/删除权限验证 (validateReportAccess)
- ✅ 优化律师业绩排行N+1查询 (批量查询提成)
- ✅ 添加ThreadLocal缓存优化重复查询
- ✅ 添加递归CTE查询子部门 (selectAllDescendantDeptIds)
- ✅ 改进异常处理为分块处理
- ✅ 改进返回类型安全 (AvailableReportDTO)
- ✅ 添加listReports严格权限验证

**建议**: 已完成22轮深度审查,共发现576个问题。本轮发现的9个关键问题已修复。建议继续审查剩余模块并修复剩余的16个未处理问题。
