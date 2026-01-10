# 业务逻辑审查报告 - 第十三轮

**审查日期**: 2026-01-10
**审查人**: Claude Code
**审查范围**: 考勤管理、请假管理

---

## 执行摘要

第十三轮审查深入分析了行政管理中的考勤和请假模块，发现了**21个新问题**:
- **3个严重问题** (P0)
- **8个高优先级问题** (P1)
- **8个中优先级问题** (P2)
- **2个低优先级问题** (P3)

**最严重发现**:
1. **考勤签到/签退存在并发竞争** - 同一时刻多次签到可能创建重复记录
2. **请假申请列表DTO转换存在N+1查询** - 查询100条申请执行101次数据库查询
3. **请假余额DTO转换存在N+1查询** - 每条余额记录都查询一次请假类型

**累计问题统计**: 13轮共发现 **384个问题**

---

## 新发现问题详情

### 🔴 严重问题 (P0 - 立即修复)

#### 364. 考勤签到/签退存在并发竞争条件 ✅ 已修复

**文件**: `admin/service/AttendanceAppService.java:69-111, 117-162`

**问题描述**:
```java
@Transactional
public AttendanceDTO checkIn(CheckInCommand command) {
    Long userId = SecurityUtils.getUserId();
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();

    // ⚠️ 检查今日是否已签到（查询和插入不是原子操作）
    Attendance existing = attendanceMapper.selectByUserAndDate(userId, today);
    if (existing != null && existing.getCheckInTime() != null) {
        throw new BusinessException("今日已签到");
    }

    // ... 判断迟到状态 ...

    Attendance attendance;
    if (existing != null) {
        // 更新签到信息
        existing.setCheckInTime(now);
        // ...
        attendanceRepository.updateById(existing);
        attendance = existing;
    } else {
        // ⚠️ 创建新记录
        attendance = Attendance.builder()
                .userId(userId)
                .attendanceDate(today)
                .checkInTime(now)
                // ...
                .build();
        attendanceRepository.save(attendance);  // ⚠️ 并发时可能插入多条
    }

    return toDTO(attendance);
}
```

**并发问题**:
```
时刻1: 线程A查询 existing = null（没有记录）
时刻2: 线程B查询 existing = null（没有记录）
时刻3: 线程A创建新记录并插入 ✅
时刻4: 线程B创建新记录并插入 ✅
结果: 同一用户同一天有两条签到记录！
```

**影响场景**:
- 用户在移动端和PC端同时签到
- 网络延迟导致重复请求
- 一个用户有多条签到记录，统计混乱

**修复建议**:
```java
@Transactional
public AttendanceDTO checkIn(CheckInCommand command) {
    Long userId = SecurityUtils.getUserId();
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();

    // ✅ 方案1: 使用数据库唯一约束 + 重试
    // 在attendance表添加唯一索引: UNIQUE(user_id, attendance_date)

    String status = now.toLocalTime().isAfter(WORK_START_TIME)
            ? Attendance.STATUS_LATE
            : Attendance.STATUS_NORMAL;

    try {
        Attendance attendance = Attendance.builder()
                .userId(userId)
                .attendanceDate(today)
                .checkInTime(now)
                .checkInLocation(command.getLocation())
                .checkInDevice(command.getDevice())
                .status(status)
                .overtimeHours(BigDecimal.ZERO)
                .build();
        attendanceRepository.save(attendance);
        log.info("用户签到成功: userId={}, time={}, status={}", userId, now, status);
        return toDTO(attendance);
    } catch (DuplicateKeyException e) {
        // 唯一约束冲突，说明已签到，查询并返回
        Attendance existing = attendanceMapper.selectByUserAndDate(userId, today);
        if (existing != null && existing.getCheckInTime() != null) {
            throw new BusinessException("今日已签到");
        }
        // 如果没有checkInTime，允许更新
        existing.setCheckInTime(now);
        existing.setCheckInLocation(command.getLocation());
        existing.setCheckInDevice(command.getDevice());
        existing.setStatus(status);
        attendanceRepository.updateById(existing);
        return toDTO(existing);
    }
}

// ✅ 方案2: 使用乐观锁或悲观锁
@Transactional
public AttendanceDTO checkIn(CheckInCommand command) {
    Long userId = SecurityUtils.getUserId();
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();

    // 使用 FOR UPDATE 锁定记录
    Attendance existing = attendanceMapper.selectByUserAndDateForUpdate(userId, today);

    if (existing != null && existing.getCheckInTime() != null) {
        throw new BusinessException("今日已签到");
    }

    String status = now.toLocalTime().isAfter(WORK_START_TIME)
            ? Attendance.STATUS_LATE
            : Attendance.STATUS_NORMAL;

    if (existing != null) {
        existing.setCheckInTime(now);
        existing.setCheckInLocation(command.getLocation());
        existing.setCheckInDevice(command.getDevice());
        existing.setStatus(status);
        attendanceRepository.updateById(existing);
        return toDTO(existing);
    } else {
        Attendance attendance = Attendance.builder()
                .userId(userId)
                .attendanceDate(today)
                .checkInTime(now)
                .checkInLocation(command.getLocation())
                .checkInDevice(command.getDevice())
                .status(status)
                .overtimeHours(BigDecimal.ZERO)
                .build();
        attendanceRepository.save(attendance);
        return toDTO(attendance);
    }
}
```

#### 365. 请假申请列表DTO转换存在N+1查询 ✅ 已修复

**文件**: `admin/service/LeaveAppService.java:59-74, 290-315`

**问题描述**:
```java
public PageResult<LeaveApplicationDTO> listApplications(LeaveApplicationQueryDTO query) {
    IPage<LeaveApplication> page = leaveApplicationMapper.selectApplicationPage(...);

    List<LeaveApplicationDTO> records = page.getRecords().stream()
            .map(this::toApplicationDTO)  // ⚠️ N+1查询
            .collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
}

private LeaveApplicationDTO toApplicationDTO(LeaveApplication app) {
    LeaveApplicationDTO dto = new LeaveApplicationDTO();
    // ... 字段映射 ...

    // ⚠️ N+1查询: 查询请假类型名称
    LeaveType type = leaveTypeRepository.getById(app.getLeaveTypeId());  // 每条记录查一次
    if (type != null) {
        dto.setLeaveTypeName(type.getName());
    }

    return dto;
}
```

**性能影响**:
- 查询100条请假申请 = 1次主查询 + 100次请假类型查询 = **101次数据库查询**
- 请假类型通常只有几种（年假、病假、事假等），重复查询严重

**修复建议**:
```java
public PageResult<LeaveApplicationDTO> listApplications(LeaveApplicationQueryDTO query) {
    // 1. 查询申请列表
    IPage<LeaveApplication> page = leaveApplicationMapper.selectApplicationPage(...);
    List<LeaveApplication> applications = page.getRecords();

    if (applications.isEmpty()) {
        return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
    }

    // 2. 批量加载请假类型
    Set<Long> leaveTypeIds = applications.stream()
            .map(LeaveApplication::getLeaveTypeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, LeaveType> typeMap = leaveTypeIds.isEmpty() ? Collections.emptyMap() :
            leaveTypeRepository.listByIds(new ArrayList<>(leaveTypeIds)).stream()
                    .collect(Collectors.toMap(LeaveType::getId, t -> t));

    // 3. 转换DTO(从Map获取)
    List<LeaveApplicationDTO> records = applications.stream()
            .map(app -> toApplicationDTO(app, typeMap))
            .collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
}

private LeaveApplicationDTO toApplicationDTO(LeaveApplication app, Map<Long, LeaveType> typeMap) {
    LeaveApplicationDTO dto = new LeaveApplicationDTO();
    // ... 字段映射 ...

    // 从Map获取，避免查询
    if (app.getLeaveTypeId() != null) {
        LeaveType type = typeMap.get(app.getLeaveTypeId());
        if (type != null) {
            dto.setLeaveTypeName(type.getName());
        }
    }

    return dto;
}
```

#### 366. 请假余额列表DTO转换存在N+1查询 ✅ 已修复

**文件**: `admin/service/LeaveAppService.java:206-217, 317-334`

**问题描述**:
```java
public List<LeaveBalanceDTO> getUserBalance(Long userId, Integer year) {
    if (userId == null) {
        userId = SecurityUtils.getUserId();
    }
    if (year == null) {
        year = LocalDate.now().getYear();
    }

    return leaveBalanceMapper.selectByUserAndYear(userId, year).stream()
            .map(this::toBalanceDTO)  // ⚠️ N+1查询
            .collect(Collectors.toList());
}

private LeaveBalanceDTO toBalanceDTO(LeaveBalance balance) {
    LeaveBalanceDTO dto = new LeaveBalanceDTO();
    // ... 字段映射 ...

    // ⚠️ N+1查询: 查询请假类型名称
    LeaveType type = leaveTypeRepository.getById(balance.getLeaveTypeId());  // 每条余额查一次
    if (type != null) {
        dto.setLeaveTypeName(type.getName());
    }

    return dto;
}
```

**修复建议**: 与问题365相同，使用批量加载模式。

---

### 🟠 高优先级问题 (P1 - 本周修复)

#### 367. 请假审批扣减余额存在并发竞争 ✅ 已修复

**文件**: `admin/service/LeaveAppService.java:140-180`

**问题描述**:
```java
@Transactional
public LeaveApplicationDTO approveLeave(ApproveLeaveCommand command) {
    // ... 验证 ...

    if (command.getApproved()) {
        application.setStatus(LeaveApplication.STATUS_APPROVED);

        // 扣减假期余额
        LeaveType leaveType = leaveTypeRepository.getById(application.getLeaveTypeId());
        if (leaveType != null && leaveType.getAnnualLimit() != null) {
            int year = application.getStartTime().getYear();
            // ⚠️ 使用UPDATE语句扣减，可能并发问题
            int updated = leaveBalanceMapper.deductBalance(
                    application.getUserId(),
                    application.getLeaveTypeId(),
                    year,
                    application.getDuration()
            );
            if (updated == 0) {
                throw new BusinessException("扣减假期余额失败，余额不足");
            }
        }

        log.info("请假申请已批准: {}", application.getApplicationNo());
    }

    leaveApplicationRepository.updateById(application);  // ⚠️ 申请已更新为APPROVED
    return toApplicationDTO(application);
}
```

**问题**:
1. deductBalance可能执行：`UPDATE leave_balance SET used_days = used_days + ?, remaining_days = remaining_days - ? WHERE ... AND remaining_days >= ?`
2. 并发审批同一用户的多个申请时，可能超额扣减
3. 如果deductBalance失败抛异常，但申请状态已设置为APPROVED，回滚后状态仍是APPROVED（在内存中）

**修复建议**:
```java
@Transactional
public LeaveApplicationDTO approveLeave(ApproveLeaveCommand command) {
    Long approverId = SecurityUtils.getUserId();

    LeaveApplication application = leaveApplicationRepository.getByIdOrThrow(
            command.getApplicationId(), "请假申请不存在");

    if (!LeaveApplication.STATUS_PENDING.equals(application.getStatus())) {
        throw new BusinessException("该申请已处理");
    }

    // ✅ 先扣减余额，再更新申请状态
    if (command.getApproved()) {
        LeaveType leaveType = leaveTypeRepository.getById(application.getLeaveTypeId());
        if (leaveType != null && leaveType.getAnnualLimit() != null) {
            int year = application.getStartTime().getYear();
            int updated = leaveBalanceMapper.deductBalance(
                    application.getUserId(),
                    application.getLeaveTypeId(),
                    year,
                    application.getDuration()
            );
            if (updated == 0) {
                throw new BusinessException("扣减假期余额失败，余额不足");
            }
        }

        // 余额扣减成功后，才更新申请状态
        application.setStatus(LeaveApplication.STATUS_APPROVED);
        log.info("请假申请已批准: {}", application.getApplicationNo());
    } else {
        application.setStatus(LeaveApplication.STATUS_REJECTED);
        log.info("请假申请已拒绝: {}", application.getApplicationNo());
    }

    application.setApproverId(approverId);
    application.setApprovedAt(LocalDateTime.now());
    application.setApprovalComment(command.getComment());

    leaveApplicationRepository.updateById(application);
    return toApplicationDTO(application, Collections.emptyMap());
}
```

#### 368. 请假余额初始化使用循环插入性能差 ✅ 已修复

**文件**: `admin/service/LeaveAppService.java:222-242`

**问题描述**:
```java
@Transactional
public void initUserBalance(Long userId, Integer year) {
    List<LeaveType> types = leaveTypeMapper.selectEnabledTypes();
    for (LeaveType type : types) {  // ⚠️ 循环插入
        if (type.getAnnualLimit() != null) {
            LeaveBalance existing = leaveBalanceMapper.selectByUserTypeYear(userId, type.getId(), year);
            if (existing == null) {
                LeaveBalance balance = LeaveBalance.builder()
                        .userId(userId)
                        .leaveTypeId(type.getId())
                        .year(year)
                        .totalDays(type.getAnnualLimit())
                        .usedDays(BigDecimal.ZERO)
                        .remainingDays(type.getAnnualLimit())
                        .build();
                leaveBalanceRepository.save(balance);  // ⚠️ 每次一个INSERT
            }
        }
    }
}
```

**问题**:
- 5种请假类型 = 5次查询 + 5次INSERT
- 批量初始化100个员工 = 500次查询 + 500次INSERT
- 性能极差

**修复建议**:
```java
@Transactional
public void initUserBalance(Long userId, Integer year) {
    List<LeaveType> types = leaveTypeMapper.selectEnabledTypes();

    // ✅ 先批量查询已有余额
    List<LeaveBalance> existingBalances = leaveBalanceMapper.selectByUserAndYear(userId, year);
    Set<Long> existingTypeIds = existingBalances.stream()
            .map(LeaveBalance::getLeaveTypeId)
            .collect(Collectors.toSet());

    // ✅ 收集需要创建的余额
    List<LeaveBalance> toCreate = new ArrayList<>();
    for (LeaveType type : types) {
        if (type.getAnnualLimit() != null && !existingTypeIds.contains(type.getId())) {
            LeaveBalance balance = LeaveBalance.builder()
                    .userId(userId)
                    .leaveTypeId(type.getId())
                    .year(year)
                    .totalDays(type.getAnnualLimit())
                    .usedDays(BigDecimal.ZERO)
                    .remainingDays(type.getAnnualLimit())
                    .build();
            toCreate.add(balance);
        }
    }

    // ✅ 批量插入
    if (!toCreate.isEmpty()) {
        leaveBalanceRepository.saveBatch(toCreate);
    }

    log.info("用户假期余额初始化完成: userId={}, year={}, created={}", userId, year, toCreate.size());
}
```

#### 369. 加班时长计算跨天场景有误 ✅ 已修复

**文件**: `admin/service/AttendanceAppService.java:151-157`

**问题描述**:
```java
// 计算加班时长
if (now.toLocalTime().isAfter(WORK_END_TIME)) {
    Duration overtime = Duration.between(WORK_END_TIME, now.toLocalTime());  // ⚠️ 跨天问题
    BigDecimal overtimeHours = BigDecimal.valueOf(overtime.toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    attendance.setOvertimeHours(overtimeHours);
}
```

**问题**:
```
场景: 程序员加班到凌晨1点
- 签退时间: 2026-01-11 01:00:00
- now.toLocalTime() = 01:00
- WORK_END_TIME = 18:00
- Duration.between(18:00, 01:00) = -17小时（负数！）
- 加班时长 = -17小时
```

**修复建议**:
```java
// ✅ 计算加班时长（使用完整DateTime）
LocalDateTime workEndDateTime = LocalDateTime.of(
        attendance.getAttendanceDate(),  // 签到日期
        WORK_END_TIME                     // 下班时间
);

if (now.isAfter(workEndDateTime)) {
    Duration overtime = Duration.between(workEndDateTime, now);
    BigDecimal overtimeHours = BigDecimal.valueOf(overtime.toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    attendance.setOvertimeHours(overtimeHours);
}
```

#### 370-375. 其他高优先级问题

370. 请假时间重叠检查存在并发竞争 (LeaveAppService:98-101)
371. 工作时间硬编码不可配置 (AttendanceAppService:42-44)
372. 早退状态覆盖逻辑不完整 (AttendanceAppService:143-149)
373. 取消请假后余额未恢复 (LeaveAppService:184-201)
374. 请假附件URL未验证有效性 (LeaveAppService:127)
375. 待审批列表未分页 (LeaveAppService:247-251)

---

### 🟡 中优先级问题 (P2 - 两周内修复)

#### 376. 月度考勤统计返回Map缺少类型安全 ✅ 已修复

**文件**: `admin/service/AttendanceAppService.java:177-208`

**问题描述**:
```java
public Map<String, Object> getMonthlyStatistics(Long userId, Integer year, Integer month) {
    // ...
    Map<String, Object> result = new HashMap<>();  // ⚠️ 使用Map<String, Object>
    result.put("userId", userId);
    result.put("year", year);
    result.put("month", month);
    result.put("normalDays", 0);
    result.put("lateDays", 0);
    // ...
    return result;
}
```

**问题**:
- 使用Map返回，缺少类型安全
- 调用方需要知道key的名称和类型
- 容易拼写错误，编译期无法检查

**修复建议**:
```java
// 创建专用DTO
@Data
@Builder
public class MonthlyAttendanceStatisticsDTO {
    private Long userId;
    private Integer year;
    private Integer month;
    private Integer normalDays;
    private Integer lateDays;
    private Integer earlyDays;
    private Integer absentDays;
    private Integer leaveDays;
    private BigDecimal totalWorkHours;
    private BigDecimal totalOvertimeHours;
}

public MonthlyAttendanceStatisticsDTO getMonthlyStatistics(Long userId, Integer year, Integer month) {
    if (userId == null) {
        userId = SecurityUtils.getUserId();
    }
    LocalDate startDate = LocalDate.of(year, month, 1);
    LocalDate endDate = startDate.plusMonths(1).minusDays(1);

    MonthlyAttendanceStatisticsDTO.MonthlyAttendanceStatisticsDTOBuilder builder =
            MonthlyAttendanceStatisticsDTO.builder()
                    .userId(userId)
                    .year(year)
                    .month(month)
                    .normalDays(0)
                    .lateDays(0)
                    .earlyDays(0)
                    .absentDays(0)
                    .leaveDays(0);

    List<Object[]> stats = attendanceMapper.countMonthlyAttendance(userId, startDate, endDate);
    for (Object[] stat : stats) {
        String status = (String) stat[0];
        Integer count = ((Long) stat[1]).intValue();
        switch (status) {
            case Attendance.STATUS_NORMAL -> builder.normalDays(count);
            case Attendance.STATUS_LATE -> builder.lateDays(count);
            case Attendance.STATUS_EARLY -> builder.earlyDays(count);
            case Attendance.STATUS_ABSENT -> builder.absentDays(count);
            case Attendance.STATUS_LEAVE -> builder.leaveDays(count);
        }
    }

    return builder.build();
}
```

#### 377-383. 其他中优先级问题

377. 请假开始时间不能早于当前时间过于严格 (LeaveAppService:93-95)
378. 签到签退位置和设备信息未验证 (AttendanceAppService:91-92, 133-134)
379. 请假申请编号可能重复 (LeaveAppService:256-260)
380. 考勤状态变更无审计记录 (AttendanceAppService:缺少)
381. 请假审批无权限验证 (LeaveAppService:140)
382. 考勤记录删除功能缺失 (AttendanceAppService:无删除方法)
383. 请假类型启用/禁用状态变更无通知 (LeaveAppService:无相关功能)

---

### 🟢 低优先级问题 (P3 - 逐步优化)

#### 384-385. 代码质量问题

384. 状态名称转换方法在两个Service中重复
385. toDTO方法缺少空值保护，可能NPE

---

## 十三轮累计统计

**总计发现**: **384个问题**

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
| **总计** | **37** | **136** | **140** | **71** | **384** |

### 按类别统计

| 类别 | 问题数 | 占比 |
|------|--------|------|
| 安全问题 | 63 | 16.4% |
| 性能问题 | 97 | 25.3% |
| 数据一致性 | 61 | 15.9% |
| 业务逻辑 | 95 | 24.7% |
| 并发问题 | 28 | 7.3% |
| 代码质量 | 40 | 10.4% |

### 按严重程度统计

| 严重程度 | 问题数 | 占比 | 说明 |
|---------|--------|------|------|
| P0 严重 | 37 | 9.6% | 立即修复 |
| P1 高优先级 | 136 | 35.4% | 本周修复 |
| P2 中优先级 | 140 | 36.5% | 两周内修复 |
| P3 低优先级 | 71 | 18.5% | 逐步优化 |

---

## 本轮核心问题分析

### 1. 并发竞争问题严重

**影响模块**: 考勤、请假管理
**风险等级**: 🔴 严重

考勤和请假都存在严重并发问题:
- 签到/签退: 同一时刻多次请求可能创建重复记录
- 请假审批: 扣减余额可能超额
- 时间重叠检查: 查询和插入不是原子操作

**建议**: 使用数据库唯一约束、悲观锁或乐观锁。

### 2. N+1查询持续存在

**影响模块**: 请假管理
**风险等级**: 🔴 严重

请假申请和余额列表都存在N+1查询:
- 每条记录都查询请假类型名称
- 请假类型数量很少，重复查询严重浪费

**建议**: 使用批量加载模式。

### 3. 业务逻辑计算错误

**影响模块**: 考勤管理
**风险等级**: 🟠 高

加班时长计算:
- 跨天场景会计算出负数
- 使用LocalTime丢失日期信息
- 影响加班费计算

**建议**: 使用完整的LocalDateTime计算。

### 4. 循环操作性能差

**影响模块**: 请假管理
**风险等级**: 🟠 高

余额初始化:
- 循环查询和插入
- 批量操作性能极差

**建议**: 使用批量操作。

---

## 修复优先级建议

### 立即修复 (P0) - 今天

1. ✅ **修复考勤签到并发竞争** (问题364) - 已修复 2026-01-10
2. ✅ **优化请假申请列表N+1查询** (问题365) - 已修复 2026-01-10
3. ✅ **优化请假余额列表N+1查询** (问题366) - 已修复 2026-01-10

### 本周修复 (P1)

4. ✅ 修复请假审批并发问题 (问题367) - 已修复 2026-01-10
5. ✅ 优化余额初始化批量操作 (问题368) - 已修复 2026-01-10
6. ✅ 修复加班计算跨天问题 (问题369) - 已修复 2026-01-10
7. 修复时间重叠检查并发 (问题370) - 待处理
8. 工作时间改为可配置 (问题371) - 待处理

### 两周内修复 (P2)

9. ✅ 使用DTO替代Map返回 (问题376) - 已修复 2026-01-10
10. 完善业务数据验证 (问题377-383) - 待处理

### 逐步优化 (P3)

11. 提取公共代码，减少重复 (问题384-385)

---

## 重点建议

### 1. 并发控制标准模式

**方案1: 数据库唯一约束 + 异常处理**
```sql
-- 添加唯一索引
ALTER TABLE attendance ADD UNIQUE INDEX uk_user_date (user_id, attendance_date);
```

```java
try {
    attendanceRepository.save(attendance);
} catch (DuplicateKeyException e) {
    // 处理重复
    Attendance existing = attendanceMapper.selectByUserAndDate(userId, today);
    if (existing.getCheckInTime() != null) {
        throw new BusinessException("今日已签到");
    }
    // 允许更新
}
```

**方案2: 悲观锁**
```java
// Mapper
@Select("SELECT * FROM attendance WHERE user_id = #{userId} AND attendance_date = #{date} FOR UPDATE")
Attendance selectByUserAndDateForUpdate(@Param("userId") Long userId, @Param("date") LocalDate date);

// Service
@Transactional
public AttendanceDTO checkIn(CheckInCommand command) {
    Attendance existing = attendanceMapper.selectByUserAndDateForUpdate(userId, today);
    // 在锁保护下检查和创建
}
```

### 2. 跨天时间计算

```java
// ❌ 错误：使用LocalTime会丢失日期
Duration overtime = Duration.between(WORK_END_TIME, now.toLocalTime());

// ✅ 正确：使用完整DateTime
LocalDateTime workEndDateTime = LocalDateTime.of(attendanceDate, WORK_END_TIME);
if (now.isAfter(workEndDateTime)) {
    Duration overtime = Duration.between(workEndDateTime, now);
    // ...
}
```

### 3. 批量初始化模式

```java
// ❌ 错误：循环插入
for (Type type : types) {
    Balance balance = create(type);
    repository.save(balance);  // N次INSERT
}

// ✅ 正确：批量插入
List<Balance> toCreate = new ArrayList<>();
for (Type type : types) {
    if (!existingTypeIds.contains(type.getId())) {
        toCreate.add(create(type));
    }
}
if (!toCreate.isEmpty()) {
    repository.saveBatch(toCreate);  // 1次批量INSERT
}
```

### 4. 类型安全返回值

```java
// ❌ 错误：返回Map
public Map<String, Object> getStatistics() {
    Map<String, Object> result = new HashMap<>();
    result.put("normalDays", 10);
    return result;
}

// ✅ 正确：返回DTO
@Data
public class StatisticsDTO {
    private Integer normalDays;
    private Integer lateDays;
}

public StatisticsDTO getStatistics() {
    return StatisticsDTO.builder()
            .normalDays(10)
            .lateDays(2)
            .build();
}
```

---

## 总结

第十三轮审查发现**21个新问题**，其中**3个严重问题**需要立即修复。

**最关键的问题**:
1. 考勤签到/签退存在严重并发竞争
2. 请假申请和余额列表N+1查询
3. 加班时长计算跨天场景有误

**行动建议**:
1. 立即修复3个P0严重问题
2. 本周内修复8个P1高优先级问题
3. 统一并发控制模式
4. 使用批量加载优化N+1查询
5. 修复时间计算逻辑错误
6. 强制使用批量操作

系统考勤和请假管理模块存在多个并发安全和性能问题，建议优先修复P0和P1问题后再上线。

---

**审查完成时间**: 2026-01-10
**建议**: 已完成13轮深度审查，共发现384个问题。建议继续审查剩余模块。
