package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateScheduleCommand;
import com.lawfirm.application.matter.dto.ScheduleDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Schedule;
import com.lawfirm.domain.matter.repository.ScheduleRepository;
import com.lawfirm.infrastructure.persistence.mapper.ScheduleMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 日程应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleAppService {

  /** 日程仓储. */
  private final ScheduleRepository scheduleRepository;

  /** 日程Mapper. */
  private final ScheduleMapper scheduleMapper;

  /**
   * 查询日程 数据权限：仅限用户自己创建的日程，与角色无关
   *
   * @param userId 用户ID
   * @param matterId 案件ID
   * @param scheduleType 日程类型
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 日程列表
   */
  public List<ScheduleDTO> listSchedules(
      final Long userId,
      final Long matterId,
      final String scheduleType,
      final LocalDateTime startTime,
      final LocalDateTime endTime,
      final int pageNum,
      final int pageSize) {
    // 日程管理仅限用户自己的数据，忽略传入的userId参数
    Long currentUserId = SecurityUtils.getUserId();

    IPage<Schedule> page =
        scheduleMapper.selectSchedulePage(
            new Page<>(pageNum, pageSize),
            currentUserId,
            matterId,
            scheduleType,
            startTime,
            endTime);
    return page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 创建日程
   *
   * @param command 创建日程命令
   * @return 日程DTO
   */
  @Transactional
  public ScheduleDTO createSchedule(final CreateScheduleCommand command) {
    if (command.getEndTime().isBefore(command.getStartTime())) {
      throw new BusinessException("结束时间不能早于开始时间");
    }

    Long userId = SecurityUtils.getUserId();

    Schedule schedule =
        Schedule.builder()
            .matterId(command.getMatterId())
            .userId(userId)
            .title(command.getTitle())
            .description(command.getDescription())
            .location(command.getLocation())
            .scheduleType(command.getScheduleType())
            .startTime(command.getStartTime())
            .endTime(command.getEndTime())
            .allDay(command.getAllDay() != null ? command.getAllDay() : false)
            .reminderMinutes(command.getReminderMinutes())
            .recurrenceRule(command.getRecurrenceRule())
            .status("ACTIVE")
            .build();

    scheduleRepository.save(schedule);
    log.info("日程创建成功: {}", schedule.getTitle());
    return toDTO(schedule);
  }

  /**
   * 获取日程详情 仅能查看自己创建的日程
   *
   * @param id 日程ID
   * @return 日程DTO
   */
  public ScheduleDTO getScheduleById(final Long id) {
    Schedule schedule = scheduleRepository.getByIdOrThrow(id, "日程不存在");
    checkScheduleOwnership(schedule);
    return toDTO(schedule);
  }

  /**
   * 检查日程所有权
   *
   * @param schedule 日程实体
   * @throws BusinessException 如果无权操作
   */
  private void checkScheduleOwnership(final Schedule schedule) {
    Long currentUserId = SecurityUtils.getUserId();
    if (!schedule.getUserId().equals(currentUserId)) {
      throw new BusinessException("无权操作此日程");
    }
  }

  /**
   * 更新日程 仅能更新自己创建的日程
   *
   * @param id 日程ID
   * @param title 标题
   * @param description 描述
   * @param location 地点
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param reminderMinutes 提醒时间（分钟）
   * @return 更新后的日程DTO
   */
  @Transactional
  public ScheduleDTO updateSchedule(
      final Long id,
      final String title,
      final String description,
      final String location,
      final LocalDateTime startTime,
      final LocalDateTime endTime,
      final Integer reminderMinutes) {
    Schedule schedule = scheduleRepository.getByIdOrThrow(id, "日程不存在");
    checkScheduleOwnership(schedule);

    if (StringUtils.hasText(title)) {
      schedule.setTitle(title);
    }
    if (description != null) {
      schedule.setDescription(description);
    }
    if (location != null) {
      schedule.setLocation(location);
    }
    if (startTime != null) {
      schedule.setStartTime(startTime);
    }
    if (endTime != null) {
      schedule.setEndTime(endTime);
    }
    if (reminderMinutes != null) {
      schedule.setReminderMinutes(reminderMinutes);
    }

    scheduleRepository.updateById(schedule);
    log.info("日程更新成功: {}", schedule.getTitle());
    return toDTO(schedule);
  }

  /**
   * 删除日程 仅能删除自己创建的日程
   *
   * @param id 日程ID
   */
  @Transactional
  public void deleteSchedule(final Long id) {
    Schedule schedule = scheduleRepository.getByIdOrThrow(id, "日程不存在");
    checkScheduleOwnership(schedule);
    scheduleRepository.removeById(id);
    log.info("日程删除成功: {}", schedule.getTitle());
  }

  /**
   * 取消日程 仅能取消自己创建的日程
   *
   * @param id 日程ID
   */
  @Transactional
  public void cancelSchedule(final Long id) {
    Schedule schedule = scheduleRepository.getByIdOrThrow(id, "日程不存在");
    checkScheduleOwnership(schedule);
    schedule.setStatus("CANCELLED");
    scheduleRepository.updateById(schedule);
    log.info("日程已取消: {}", schedule.getTitle());
  }

  /**
   * 获取用户某天的日程
   *
   * @param userId 用户ID
   * @param date 日期
   * @return 日程列表
   */
  public List<ScheduleDTO> getSchedulesByDate(final Long userId, final LocalDate date) {
    List<Schedule> schedules = scheduleRepository.findByUserAndDate(userId, date);
    return schedules.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取我今天的日程
   *
   * @return 日程列表
   */
  public List<ScheduleDTO> getMyTodaySchedules() {
    Long userId = SecurityUtils.getUserId();
    return getSchedulesByDate(userId, LocalDate.now());
  }

  /**
   * 获取我近期的日程（未来N天）
   *
   * @param days 天数
   * @param limit 限制数量
   * @return 日程列表
   */
  public List<ScheduleDTO> getMyUpcomingSchedules(final int days, final int limit) {
    Long userId = SecurityUtils.getUserId();
    LocalDateTime endTime = LocalDateTime.now().plusDays(days);
    List<Schedule> schedules = scheduleMapper.selectUpcomingSchedules(userId, endTime, limit);
    return schedules.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取日程类型名称
   *
   * @param type 日程类型代码
   * @return 日程类型名称
   */
  private String getScheduleTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "COURT" -> "开庭";
      case "MEETING" -> "会议";
      case "DEADLINE" -> "期限";
      case "APPOINTMENT" -> "约见";
      case "OTHER" -> "其他";
      default -> type;
    };
  }

  /**
   * Entity 转 DTO
   *
   * @param schedule 日程实体
   * @return 日程DTO
   */
  private ScheduleDTO toDTO(final Schedule schedule) {
    ScheduleDTO dto = new ScheduleDTO();
    dto.setId(schedule.getId());
    dto.setMatterId(schedule.getMatterId());
    dto.setUserId(schedule.getUserId());
    dto.setTitle(schedule.getTitle());
    dto.setDescription(schedule.getDescription());
    dto.setLocation(schedule.getLocation());
    dto.setScheduleType(schedule.getScheduleType());
    dto.setScheduleTypeName(getScheduleTypeName(schedule.getScheduleType()));
    dto.setStartTime(schedule.getStartTime());
    dto.setEndTime(schedule.getEndTime());
    dto.setAllDay(schedule.getAllDay());
    dto.setReminderMinutes(schedule.getReminderMinutes());
    dto.setReminderSent(schedule.getReminderSent());
    dto.setRecurrenceRule(schedule.getRecurrenceRule());
    dto.setStatus(schedule.getStatus());
    dto.setCreatedAt(schedule.getCreatedAt());
    dto.setUpdatedAt(schedule.getUpdatedAt());
    return dto;
  }
}
