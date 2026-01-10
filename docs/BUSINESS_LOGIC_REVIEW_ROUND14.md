# 业务逻辑审查报告 - 第十四轮

**审查日期**: 2026-01-10
**审查人**: Claude Code
**审查范围**: 加班管理、外出登记管理

---

## 执行摘要

第十四轮审查深入分析了行政管理中的加班和外出登记模块，发现了**18个新问题**:
- **2个严重问题** (P0)
- **6个高优先级问题** (P1)
- **8个中优先级问题** (P2)
- **2个低优先级问题** (P3)

**最严重发现**:
1. **加班申请列表DTO转换存在N+1查询** - 查询100条申请执行201次数据库查询
2. **外出记录列表DTO转换存在N+1查询** - 每条记录都查询一次用户信息

**累计问题统计**: 14轮共发现 **402个问题**

---

## 新发现问题详情

### 🔴 严重问题 (P0 - 立即修复)

#### 386. 加班申请列表DTO转换存在N+1查询 ✅ 已修复

**文件**: `admin/service/OvertimeAppService.java:104-108, 113-117, 135-169`

**问题描述**:
```java
public List<OvertimeApplicationDTO> getMyApplications() {
    Long userId = SecurityUtils.getUserId();
    List<OvertimeApplication> applications = overtimeMapper.selectByUserId(userId);
    return applications.stream().map(this::toDTO).collect(Collectors.toList());  // ⚠️ N+1查询
}

public List<OvertimeApplicationDTO> getApplicationsByDateRange(LocalDate startDate, LocalDate endDate) {
    Long userId = SecurityUtils.getUserId();
    List<OvertimeApplication> applications = overtimeMapper.selectByDateRange(userId, startDate, endDate);
    return applications.stream().map(this::toDTO).collect(Collectors.toList());  // ⚠️ N+1查询
}

private OvertimeApplicationDTO toDTO(OvertimeApplication application) {
    OvertimeApplicationDTO dto = new OvertimeApplicationDTO();
    // ... 字段映射 ...

    // ⚠️ N+1查询: 查询申请人名称
    if (application.getUserId() != null) {
        User user = userRepository.findById(application.getUserId());  // 每条记录查一次
        if (user != null) {
            dto.setUserName(user.getRealName());
        }
    }
    // ⚠️ N+1查询: 查询审批人名称
    if (application.getApproverId() != null) {
        User approver = userRepository.findById(application.getApproverId());  // 每条记录查一次
        if (approver != null) {
            dto.setApproverName(approver.getRealName());
        }
    }

    return dto;
}
```

**性能影响**:
- 查询100条加班申请 = 1次主查询 + 100次申请人查询 + 100次审批人查询 = **201次数据库查询**
- 用户和审批人经常重复，重复查询严重浪费

**修复建议**:
```java
public List<OvertimeApplicationDTO> getMyApplications() {
    Long userId = SecurityUtils.getUserId();
    List<OvertimeApplication> applications = overtimeMapper.selectByUserId(userId);

    if (applications.isEmpty()) {
        return Collections.emptyList();
    }

    return convertToDTOs(applications);
}

public List<OvertimeApplicationDTO> getApplicationsByDateRange(LocalDate startDate, LocalDate endDate) {
    Long userId = SecurityUtils.getUserId();
    List<OvertimeApplication> applications = overtimeMapper.selectByDateRange(userId, startDate, endDate);

    if (applications.isEmpty()) {
        return Collections.emptyList();
    }

    return convertToDTOs(applications);
}

// ✅ 批量转换方法
private List<OvertimeApplicationDTO> convertToDTOs(List<OvertimeApplication> applications) {
    // 批量加载用户信息（申请人和审批人）
    Set<Long> userIds = new HashSet<>();
    applications.forEach(app -> {
        if (app.getUserId() != null) userIds.add(app.getUserId());
        if (app.getApproverId() != null) userIds.add(app.getApproverId());
    });

    Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
            userRepository.listByIds(new ArrayList<>(userIds)).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

    // 转换DTO(从Map获取)
    return applications.stream()
            .map(app -> toDTO(app, userMap))
            .collect(Collectors.toList());
}

private OvertimeApplicationDTO toDTO(OvertimeApplication application, Map<Long, User> userMap) {
    OvertimeApplicationDTO dto = new OvertimeApplicationDTO();
    // ... 字段映射 ...

    // 从Map获取，避免查询
    if (application.getUserId() != null) {
        User user = userMap.get(application.getUserId());
        if (user != null) {
            dto.setUserName(user.getRealName());
        }
    }
    if (application.getApproverId() != null) {
        User approver = userMap.get(application.getApproverId());
        if (approver != null) {
            dto.setApproverName(approver.getRealName());
        }
    }

    return dto;
}
```

**性能对比**:
- 修复前: 100条申请 = 201次查询
- 修复后: 100条申请 = 2次查询(1次主查询 + 1次批量用户查询)
- **性能提升100倍**

#### 387. 外出记录列表DTO转换存在N+1查询 ✅ 已修复

**文件**: `admin/service/GoOutAppService.java:93-97, 102-106, 111-115, 132-157`

**问题描述**:
```java
public List<GoOutRecordDTO> getMyRecords() {
    Long userId = SecurityUtils.getUserId();
    List<GoOutRecord> records = goOutMapper.selectByUserId(userId);
    return records.stream().map(this::toDTO).collect(Collectors.toList());  // ⚠️ N+1查询
}

private GoOutRecordDTO toDTO(GoOutRecord record) {
    GoOutRecordDTO dto = new GoOutRecordDTO();
    // ... 字段映射 ...

    // ⚠️ N+1查询: 查询用户名称
    if (record.getUserId() != null) {
        User user = userRepository.findById(record.getUserId());  // 每条记录查一次
        if (user != null) {
            dto.setUserName(user.getRealName());
        }
    }

    return dto;
}
```

**性能影响**:
- 查询100条外出记录 = 1次主查询 + 100次用户查询 = **101次数据库查询**

**修复建议**: 使用与问题386相同的批量加载模式。

---

### 🟠 高优先级问题 (P1 - 本周修复)

#### 388. 外出登记返回没有权限验证 ✅ 已修复

**文件**: `admin/service/GoOutAppService.java:72-88`

**问题描述**:
```java
@Transactional
public GoOutRecordDTO registerReturn(Long id) {
    GoOutRecord record = goOutRepository.getByIdOrThrow(id, "外出记录不存在");

    if (!GoOutRecord.STATUS_OUT.equals(record.getStatus())) {
        throw new BusinessException("该记录不是外出中状态");
    }

    // ⚠️ 没有验证当前用户是否是外出人本人
    // 任何人都可以登记他人返回！

    record.setActualReturnTime(LocalDateTime.now());
    record.setStatus(GoOutRecord.STATUS_RETURNED);
    record.setUpdatedBy(SecurityUtils.getUserId());
    record.setUpdatedAt(LocalDateTime.now());
    goOutRepository.updateById(record);

    return toDTO(record);
}
```

**安全风险**:
```
场景1: 恶意操作
1. 员工A外出办事，登记外出
2. 员工B（恶意）调用registerReturn(A的记录ID)
3. 系统成功登记返回，A的记录变为"已返回"
4. 实际上A还在外面，但系统显示已返回
5. 考勤和管理混乱

场景2: 误操作
1. 管理员查看外出记录
2. 不小心点击"登记返回"按钮
3. 他人的外出记录被误登记返回
```

**修复建议**:
```java
@Transactional
public GoOutRecordDTO registerReturn(Long id) {
    GoOutRecord record = goOutRepository.getByIdOrThrow(id, "外出记录不存在");

    if (!GoOutRecord.STATUS_OUT.equals(record.getStatus())) {
        throw new BusinessException("该记录不是外出中状态");
    }

    Long currentUserId = SecurityUtils.getUserId();

    // ✅ 验证权限：只能登记自己的返回，除非是管理员
    if (!record.getUserId().equals(currentUserId)) {
        if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("HR_MANAGER")) {
            throw new BusinessException("权限不足：只能登记自己的返回");
        }
        log.warn("管理员代登记返回: recordNo={}, operator={}, user={}",
                 record.getRecordNo(), currentUserId, record.getUserId());
    }

    record.setActualReturnTime(LocalDateTime.now());
    record.setStatus(GoOutRecord.STATUS_RETURNED);
    record.setUpdatedBy(currentUserId);
    record.setUpdatedAt(LocalDateTime.now());
    goOutRepository.updateById(record);

    log.info("外出返回登记成功: recordNo={}, user={}", record.getRecordNo(), record.getUserId());
    return toDTO(record, Collections.emptyMap());
}
```

#### 389. 加班审批没有权限验证 ✅ 已修复

**文件**: `admin/service/OvertimeAppService.java:81-99`

**问题描述**:
```java
@Transactional
public OvertimeApplicationDTO approveOvertime(Long id, boolean approved, String comment) {
    OvertimeApplication application = overtimeRepository.getByIdOrThrow(id, "加班申请不存在");

    if (!OvertimeApplication.STATUS_PENDING.equals(application.getStatus())) {
        throw new BusinessException("只有待审批的申请可以审批");
    }

    // ⚠️ 没有验证审批人权限
    // 任何人都可以审批加班申请！

    application.setStatus(approved ? OvertimeApplication.STATUS_APPROVED : OvertimeApplication.STATUS_REJECTED);
    application.setApproverId(SecurityUtils.getUserId());
    // ...
}
```

**问题**:
- 没有检查审批人是否有权限
- 普通员工可以审批自己或他人的加班
- 应该只有HR或部门主管才能审批

**修复建议**:
```java
@Transactional
public OvertimeApplicationDTO approveOvertime(Long id, boolean approved, String comment) {
    OvertimeApplication application = overtimeRepository.getByIdOrThrow(id, "加班申请不存在");

    if (!OvertimeApplication.STATUS_PENDING.equals(application.getStatus())) {
        throw new BusinessException("只有待审批的申请可以审批");
    }

    Long approverId = SecurityUtils.getUserId();

    // ✅ 验证审批权限
    if (!SecurityUtils.hasRole("ADMIN") &&
        !SecurityUtils.hasRole("HR_MANAGER") &&
        !SecurityUtils.hasRole("DEPT_MANAGER")) {
        throw new BusinessException("权限不足：只有管理员或部门主管才能审批加班申请");
    }

    // ✅ 防止自己审批自己
    if (application.getUserId().equals(approverId)) {
        throw new BusinessException("不能审批自己的加班申请");
    }

    application.setStatus(approved ? OvertimeApplication.STATUS_APPROVED : OvertimeApplication.STATUS_REJECTED);
    application.setApproverId(approverId);
    application.setApprovedAt(LocalDateTime.now());
    application.setApprovalComment(comment);
    application.setUpdatedBy(approverId);
    application.setUpdatedAt(LocalDateTime.now());
    overtimeRepository.updateById(application);

    log.info("加班申请审批完成: applicationNo={}, approved={}, approver={}",
             application.getApplicationNo(), approved, approverId);
    return toDTO(application, Collections.emptyMap());
}
```

#### 390. 外出登记检查未返回记录存在并发竞争 ✅ 已修复

**文件**: `admin/service/GoOutAppService.java:38-67`

**问题描述**:
```java
@Transactional
public GoOutRecordDTO registerGoOut(GoOutCommand command) {
    Long userId = SecurityUtils.getUserId();

    // ⚠️ 检查是否有未返回的外出记录（查询和插入不是原子操作）
    List<GoOutRecord> currentOut = goOutMapper.selectCurrentOut(userId);
    if (!currentOut.isEmpty()) {
        throw new BusinessException("您还有未返回的外出记录，请先登记返回");
    }

    // ... 生成编号 ...

    GoOutRecord record = GoOutRecord.builder()...build();
    goOutRepository.save(record);  // ⚠️ 并发时可能插入多条

    return toDTO(record);
}
```

**并发问题**:
```
时刻1: 线程A查询 currentOut = [] (没有未返回记录)
时刻2: 线程B查询 currentOut = [] (没有未返回记录)
时刻3: 线程A创建外出记录并插入 ✅
时刻4: 线程B创建外出记录并插入 ✅
结果: 同一用户有两条外出中的记录！
```

**修复建议**:
```java
@Transactional
public GoOutRecordDTO registerGoOut(GoOutCommand command) {
    Long userId = SecurityUtils.getUserId();

    // ✅ 方案1: 使用数据库唯一约束
    // 在go_out_record表添加条件唯一索引: UNIQUE(user_id, status) WHERE status = 'OUT'

    // ✅ 方案2: 使用悲观锁
    // 先锁定用户记录，再检查
    List<GoOutRecord> currentOut = goOutMapper.selectCurrentOutForUpdate(userId);
    if (!currentOut.isEmpty()) {
        throw new BusinessException("您还有未返回的外出记录，请先登记返回");
    }

    // 在锁保护下创建记录
    String recordNo = generateRecordNo();
    GoOutRecord record = GoOutRecord.builder()
            .recordNo(recordNo)
            .userId(userId)
            .outTime(command.getOutTime())
            .expectedReturnTime(command.getExpectedReturnTime())
            .location(command.getLocation())
            .reason(command.getReason())
            .companions(command.getCompanions())
            .status(GoOutRecord.STATUS_OUT)
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .build();

    goOutRepository.save(record);
    log.info("外出登记成功: recordNo={}, userId={}", recordNo, userId);
    return toDTO(record, Collections.emptyMap());
}
```

#### 391-393. 其他高优先级问题

391. ✅ 加班时长没有合理性验证 (OvertimeAppService:52-54) - 已修复
392. ✅ 外出时间可能在预计返回时间之后 (GoOutAppService:无验证) - 已修复
393. 加班申请没有取消功能 (OvertimeAppService:缺少方法) - 待处理

---

### 🟡 中优先级问题 (P2 - 两周内修复)

#### 394. 加班日期和时间可能不一致 ✅ 已修复

**文件**: `admin/service/OvertimeAppService.java:42-76`

**问题描述**:
```java
@Transactional
public OvertimeApplicationDTO applyOvertime(ApplyOvertimeCommand command) {
    Long userId = SecurityUtils.getUserId();
    LocalDate today = LocalDate.now();

    // 验证时间
    if (command.getStartTime().isAfter(command.getEndTime())) {
        throw new BusinessException("开始时间不能晚于结束时间");
    }

    // ⚠️ 没有验证startTime和endTime的日期是否等于overtimeDate
    // 可能出现: overtimeDate=2026-01-10, startTime=2026-01-11 18:00

    OvertimeApplication application = OvertimeApplication.builder()
            .overtimeDate(command.getOvertimeDate())  // 加班日期
            .startTime(command.getStartTime())        // 开始时间（含日期）
            .endTime(command.getEndTime())            // 结束时间（含日期）
            // ...
            .build();
}
```

**问题**:
- 加班日期字段和时间字段可能不一致
- 导致统计混乱

**修复建议**:
```java
@Transactional
public OvertimeApplicationDTO applyOvertime(ApplyOvertimeCommand command) {
    Long userId = SecurityUtils.getUserId();

    // 验证时间
    if (command.getStartTime().isAfter(command.getEndTime())) {
        throw new BusinessException("开始时间不能晚于结束时间");
    }

    // ✅ 验证日期一致性
    LocalDate startDate = command.getStartTime().toLocalDate();
    LocalDate endDate = command.getEndTime().toLocalDate();

    if (!startDate.equals(command.getOvertimeDate())) {
        throw new BusinessException("开始时间的日期必须等于加班日期");
    }

    // 允许跨天加班，但需要提示
    if (!startDate.equals(endDate)) {
        log.warn("跨天加班申请: userId={}, startDate={}, endDate={}",
                 userId, startDate, endDate);
        // 或者直接拒绝跨天
        // throw new BusinessException("不支持跨天加班，请分别申请");
    }

    // 计算加班时长
    Duration duration = Duration.between(command.getStartTime(), command.getEndTime());
    BigDecimal overtimeHours = BigDecimal.valueOf(duration.toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

    // ✅ 验证加班时长合理性（不超过12小时）
    if (overtimeHours.compareTo(BigDecimal.valueOf(12)) > 0) {
        throw new BusinessException("单次加班时长不能超过12小时");
    }

    // ...
}
```

#### 395-401. 其他中优先级问题

395. 加班开始时间可能在过去 (OvertimeAppService:无验证)
396. 外出预计返回时间可能在过去 (GoOutAppService:无验证)
397. 编号生成虽然用UUID但仍可优化 (OvertimeAppService:119-123, GoOutAppService:117-121)
398. 加班审批无通知功能 (OvertimeAppService:无通知)
399. 外出超时未返回无提醒 (GoOutAppService:无定时任务)
400. 加班统计功能缺失 (OvertimeAppService:无统计方法)
401. 外出记录无审批流程 (GoOutAppService:直接登记)

---

### 🟢 低优先级问题 (P3 - 逐步优化)

#### 402-403. 代码质量问题

402. 状态名称转换方法重复，应提取常量类
403. toDTO方法缺少Map参数的重载版本

---

## 十四轮累计统计

**总计发现**: **402个问题**

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
| **总计** | **39** | **142** | **148** | **73** | **402** |

### 按类别统计

| 类别 | 问题数 | 占比 |
|------|--------|------|
| 安全问题 | 66 | 16.4% |
| 性能问题 | 101 | 25.1% |
| 数据一致性 | 64 | 15.9% |
| 业务逻辑 | 99 | 24.6% |
| 并发问题 | 30 | 7.5% |
| 代码质量 | 42 | 10.4% |

### 按严重程度统计

| 严重程度 | 问题数 | 占比 | 说明 |
|---------|--------|------|------|
| P0 严重 | 39 | 9.7% | 立即修复 |
| P1 高优先级 | 142 | 35.3% | 本周修复 |
| P2 中优先级 | 148 | 36.8% | 两周内修复 |
| P3 低优先级 | 73 | 18.2% | 逐步优化 |

---

## 本轮核心问题分析

### 1. N+1查询问题持续普遍存在

**影响模块**: 加班、外出管理
**风险等级**: 🔴 严重

所有列表查询都存在N+1查询:
- 加班申请列表: 201次查询
- 外出记录列表: 101次查询
- 系统性能瓶颈

**建议**: 立即使用批量加载模式优化所有列表查询。

### 2. 权限验证严重缺失

**影响模块**: 加班、外出管理
**风险等级**: 🟠 高

关键操作没有权限验证:
- 任何人都可以审批加班申请
- 任何人都可以登记他人返回
- 可能导致管理混乱

**建议**: 添加严格的权限验证机制。

### 3. 并发安全问题

**影响模块**: 外出管理
**风险等级**: 🟠 高

外出登记检查:
- 查询和插入不是原子操作
- 并发时可能创建多条外出中记录
- 导致数据混乱

**建议**: 使用数据库唯一约束或悲观锁。

### 4. 业务数据验证不完整

**影响模块**: 加班、外出管理
**风险等级**: 🟡 中

缺少必要的业务验证:
- 加班时长没有上限
- 时间和日期可能不一致
- 允许申请过去的时间

**建议**: 完善业务规则验证。

---

## 修复优先级建议

### 立即修复 (P0) - 今天

1. ✅ **优化加班申请列表N+1查询** (问题386) - 已修复 2026-01-10
2. ✅ **优化外出记录列表N+1查询** (问题387) - 已修复 2026-01-10

### 本周修复 (P1)

3. ✅ 添加外出返回权限验证 (问题388) - 已修复 2026-01-10
4. ✅ 添加加班审批权限验证 (问题389) - 已修复 2026-01-10
5. ✅ 修复外出登记并发问题 (问题390) - 已修复 2026-01-10
6. ✅ 完善业务验证逻辑 (问题391-392) - 已修复 2026-01-10
7. 加班申请取消功能 (问题393) - 待处理

### 两周内修复 (P2)

8. ✅ 添加日期时间一致性验证 (问题394) - 已修复 2026-01-10
9. 完善其他业务数据验证 (问题395-401) - 待处理

### 逐步优化 (P3)

9. 提取公共代码，减少重复 (问题402-403)

---

## 重点建议

### 1. 统一N+1查询优化

**所有列表查询必须使用批量加载**:
```java
// 标准模式
private List<DTO> convertToDTOs(List<Entity> entities) {
    if (entities.isEmpty()) {
        return Collections.emptyList();
    }

    // 批量加载所有关联用户
    Set<Long> userIds = collectUserIds(entities);
    Map<Long, User> userMap = batchLoadUsers(userIds);

    // 转换DTO(从Map获取)
    return entities.stream()
            .map(e -> toDTO(e, userMap))
            .collect(toList());
}
```

### 2. 权限验证标准模式

```java
// 审批权限验证
private void validateApprovalPermission(Application application) {
    Long approverId = SecurityUtils.getUserId();

    // 检查角色权限
    if (!SecurityUtils.hasRole("ADMIN") &&
        !SecurityUtils.hasRole("HR_MANAGER") &&
        !SecurityUtils.hasRole("DEPT_MANAGER")) {
        throw new BusinessException("权限不足：只有管理员或主管才能审批");
    }

    // 防止自己审批自己
    if (application.getUserId().equals(approverId)) {
        throw new BusinessException("不能审批自己的申请");
    }
}

// 操作权限验证
private void validateOperationPermission(Record record) {
    Long currentUserId = SecurityUtils.getUserId();

    // 只能操作自己的记录，除非是管理员
    if (!record.getUserId().equals(currentUserId)) {
        if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("HR_MANAGER")) {
            throw new BusinessException("权限不足：只能操作自己的记录");
        }
        log.warn("管理员代操作: recordId={}, operator={}, owner={}",
                 record.getId(), currentUserId, record.getUserId());
    }
}
```

### 3. 业务数据验证

```java
@Transactional
public ApplicationDTO applyOvertime(ApplyOvertimeCommand command) {
    // ✅ 时间合理性验证
    if (command.getStartTime().isAfter(command.getEndTime())) {
        throw new BusinessException("开始时间不能晚于结束时间");
    }

    // ✅ 日期一致性验证
    LocalDate startDate = command.getStartTime().toLocalDate();
    if (!startDate.equals(command.getOvertimeDate())) {
        throw new BusinessException("开始时间的日期必须等于加班日期");
    }

    // ✅ 时长上限验证
    Duration duration = Duration.between(command.getStartTime(), command.getEndTime());
    BigDecimal hours = BigDecimal.valueOf(duration.toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

    if (hours.compareTo(BigDecimal.valueOf(12)) > 0) {
        throw new BusinessException("单次加班时长不能超过12小时");
    }

    // ...
}
```

### 4. 并发控制

```java
// 方案1: 数据库唯一约束
CREATE UNIQUE INDEX uk_user_status_out
ON go_out_record(user_id, status)
WHERE status = 'OUT';

// 方案2: 悲观锁
@Select("SELECT * FROM go_out_record WHERE user_id = #{userId} AND status = 'OUT' FOR UPDATE")
List<GoOutRecord> selectCurrentOutForUpdate(@Param("userId") Long userId);
```

---

## 总结

第十四轮审查发现**18个新问题**，其中**2个严重问题**需要立即修复。

**最关键的问题**:
1. 加班申请和外出记录列表N+1查询严重
2. 关键操作缺少权限验证
3. 外出登记存在并发竞争

**行动建议**:
1. 立即修复2个P0严重问题
2. 本周内修复6个P1高优先级问题
3. 统一N+1查询优化模式
4. 建立权限验证框架
5. 完善业务数据验证
6. 修复并发安全问题

系统加班和外出管理模块存在多个性能和安全问题，建议优先修复P0和P1问题后再上线。

---

**审查完成时间**: 2026-01-10
**建议**: 已完成14轮深度审查，共发现402个问题。建议继续审查剩余模块。
