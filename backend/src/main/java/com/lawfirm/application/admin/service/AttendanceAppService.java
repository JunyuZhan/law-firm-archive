package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CheckInCommand;
import com.lawfirm.application.admin.dto.AttendanceDTO;
import com.lawfirm.application.admin.dto.AttendanceQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.Attendance;
import com.lawfirm.domain.admin.repository.AttendanceRepository;
import com.lawfirm.infrastructure.persistence.mapper.AttendanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lawfirm.application.admin.dto.MonthlyAttendanceStatisticsDTO;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 考勤应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceAppService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;

    // 标准上班时间
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    // 标准下班时间
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    /**
     * 分页查询考勤记录
     */
    public PageResult<AttendanceDTO> listAttendance(AttendanceQueryDTO query) {
        IPage<Attendance> page = attendanceMapper.selectAttendancePage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getUserId(),
                query.getStartDate(),
                query.getEndDate(),
                query.getStatus()
        );

        List<AttendanceDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 签到
     * ✅ 修复：使用数据库唯一约束+重试机制处理并发
     * 需要在数据库添加唯一索引: UNIQUE(user_id, attendance_date)
     */
    @Transactional
    public AttendanceDTO checkIn(CheckInCommand command) {
        Long userId = SecurityUtils.getUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 判断是否迟到
        String status = Attendance.STATUS_NORMAL;
        if (now.toLocalTime().isAfter(WORK_START_TIME)) {
            status = Attendance.STATUS_LATE;
        }

        try {
            // ✅ 先尝试直接创建新记录，依赖数据库唯一约束防止重复
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
            // ✅ 唯一约束冲突，说明已有记录，查询并处理
            Attendance existing = attendanceMapper.selectByUserAndDate(userId, today);
            if (existing != null && existing.getCheckInTime() != null) {
                throw new BusinessException("今日已签到");
            }
            // 如果记录存在但没有签到时间（预创建的记录），允许更新
            if (existing != null) {
                existing.setCheckInTime(now);
                existing.setCheckInLocation(command.getLocation());
                existing.setCheckInDevice(command.getDevice());
                existing.setStatus(status);
                attendanceRepository.updateById(existing);
                log.info("用户签到成功(更新): userId={}, time={}, status={}", userId, now, status);
                return toDTO(existing);
            }
            throw new BusinessException("签到失败，请重试");
        }
    }

    /**
     * 签退
     */
    @Transactional
    public AttendanceDTO checkOut(CheckInCommand command) {
        Long userId = SecurityUtils.getUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 检查今日是否已签到
        Attendance attendance = attendanceMapper.selectByUserAndDate(userId, today);
        if (attendance == null || attendance.getCheckInTime() == null) {
            throw new BusinessException("请先签到");
        }
        if (attendance.getCheckOutTime() != null) {
            throw new BusinessException("今日已签退");
        }

        // 更新签退信息
        attendance.setCheckOutTime(now);
        attendance.setCheckOutLocation(command.getLocation());
        attendance.setCheckOutDevice(command.getDevice());

        // 计算工作时长
        Duration duration = Duration.between(attendance.getCheckInTime(), now);
        BigDecimal workHours = BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        attendance.setWorkHours(workHours);

        // 判断是否早退
        if (now.toLocalTime().isBefore(WORK_END_TIME)) {
            if (Attendance.STATUS_LATE.equals(attendance.getStatus())) {
                // 既迟到又早退，保持迟到状态
            } else {
                attendance.setStatus(Attendance.STATUS_EARLY);
            }
        }

        // ✅ 修复：计算加班时长（使用完整DateTime，支持跨天加班）
        LocalDateTime workEndDateTime = LocalDateTime.of(attendance.getAttendanceDate(), WORK_END_TIME);
        if (now.isAfter(workEndDateTime)) {
            Duration overtime = Duration.between(workEndDateTime, now);
            BigDecimal overtimeHours = BigDecimal.valueOf(overtime.toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            attendance.setOvertimeHours(overtimeHours);
        }

        attendanceRepository.updateById(attendance);
        log.info("用户签退成功: userId={}, time={}, workHours={}", userId, now, workHours);
        return toDTO(attendance);
    }

    /**
     * 获取今日考勤
     */
    public AttendanceDTO getTodayAttendance() {
        Long userId = SecurityUtils.getUserId();
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceMapper.selectByUserAndDate(userId, today);
        return attendance != null ? toDTO(attendance) : null;
    }

    /**
     * 获取月度考勤统计
     * ✅ 修复：使用类型安全的DTO替代Map返回
     */
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
            Long count = (Long) stat[1];
            switch (status) {
                case Attendance.STATUS_NORMAL -> builder.normalDays(count.intValue());
                case Attendance.STATUS_LATE -> builder.lateDays(count.intValue());
                case Attendance.STATUS_EARLY -> builder.earlyDays(count.intValue());
                case Attendance.STATUS_ABSENT -> builder.absentDays(count.intValue());
                case Attendance.STATUS_LEAVE -> builder.leaveDays(count.intValue());
            }
        }

        return builder.build();
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case Attendance.STATUS_NORMAL -> "正常";
            case Attendance.STATUS_LATE -> "迟到";
            case Attendance.STATUS_EARLY -> "早退";
            case Attendance.STATUS_ABSENT -> "缺勤";
            case Attendance.STATUS_LEAVE -> "请假";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private AttendanceDTO toDTO(Attendance attendance) {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setId(attendance.getId());
        dto.setUserId(attendance.getUserId());
        dto.setAttendanceDate(attendance.getAttendanceDate());
        dto.setCheckInTime(attendance.getCheckInTime());
        dto.setCheckOutTime(attendance.getCheckOutTime());
        dto.setCheckInLocation(attendance.getCheckInLocation());
        dto.setCheckOutLocation(attendance.getCheckOutLocation());
        dto.setStatus(attendance.getStatus());
        dto.setStatusName(getStatusName(attendance.getStatus()));
        dto.setWorkHours(attendance.getWorkHours());
        dto.setOvertimeHours(attendance.getOvertimeHours());
        dto.setRemark(attendance.getRemark());
        dto.setCreatedAt(attendance.getCreatedAt());
        return dto;
    }
}
