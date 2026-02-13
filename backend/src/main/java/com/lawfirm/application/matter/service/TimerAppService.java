package com.lawfirm.application.matter.service;

import com.lawfirm.application.matter.command.CreateTimesheetCommand;
import com.lawfirm.application.matter.command.StartTimerCommand;
import com.lawfirm.application.matter.dto.TimerSessionDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.TimerSession;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.TimerSessionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在线计时器应用服务（M3-044）. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimerAppService {

  /** 每小时秒数. */
  private static final long SECONDS_PER_HOUR = 3600L;

  /** 每分钟秒数. */
  private static final long SECONDS_PER_MINUTE = 60L;

  /** 小时精度. */
  private static final int HOURS_SCALE = 2;

  /** 计时器会话仓储. */
  private final TimerSessionRepository timerSessionRepository;

  /** 项目仓储. */
  private final MatterRepository matterRepository;

  /** 工时应用服务. */
  private final TimesheetAppService timesheetAppService;

  /**
   * 开始计时.
   *
   * @param command 开始计时命令
   * @return 计时器会话DTO
   */
  @Transactional
  public TimerSessionDTO startTimer(final StartTimerCommand command) {
    Long userId = SecurityUtils.getUserId();

    // 检查是否有正在运行的计时器
    TimerSession running = timerSessionRepository.findRunningByUserId(userId);
    if (running != null) {
      throw new BusinessException("已有正在运行的计时器，请先停止或暂停");
    }

    // 验证案件存在
    Matter matter = matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");

    // 创建新的计时器会话
    TimerSession session =
        TimerSession.builder()
            .userId(userId)
            .matterId(command.getMatterId())
            .workType(command.getWorkType())
            .workContent(command.getWorkContent())
            .billable(command.getBillable() != null ? command.getBillable() : true)
            .startTime(LocalDateTime.now())
            .elapsedSeconds(0L)
            .status("RUNNING")
            .build();

    timerSessionRepository.save(session);
    log.info(
        "开始计时: userId={}, matterId={}, sessionId={}",
        userId,
        command.getMatterId(),
        session.getId());

    return toDTO(session, matter);
  }

  /**
   * 暂停计时.
   *
   * @return 计时器会话DTO
   */
  @Transactional
  public TimerSessionDTO pauseTimer() {
    Long userId = SecurityUtils.getUserId();
    TimerSession session = timerSessionRepository.findRunningByUserId(userId);

    if (session == null) {
      throw new BusinessException("没有正在运行的计时器");
    }

    LocalDateTime now = LocalDateTime.now();
    long currentElapsed = Duration.between(session.getStartTime(), now).getSeconds();

    // 如果有恢复时间，说明之前暂停过，需要加上之前的累计时间
    if (session.getResumeTime() != null) {
      currentElapsed = Duration.between(session.getResumeTime(), now).getSeconds();
    }

    session.setPauseTime(now);
    session.setElapsedSeconds(session.getElapsedSeconds() + currentElapsed);
    session.setStatus("PAUSED");
    timerSessionRepository.updateById(session);

    log.info("暂停计时: sessionId={}, elapsedSeconds={}", session.getId(), session.getElapsedSeconds());

    Matter matter = matterRepository.getByIdOrThrow(session.getMatterId(), "案件不存在");
    return toDTO(session, matter);
  }

  /**
   * 继续计时.
   *
   * @return 计时器会话DTO
   */
  @Transactional
  public TimerSessionDTO resumeTimer() {
    Long userId = SecurityUtils.getUserId();
    TimerSession session = timerSessionRepository.findPausedByUserId(userId);

    if (session == null) {
      throw new BusinessException("没有已暂停的计时器");
    }

    session.setResumeTime(LocalDateTime.now());
    session.setStatus("RUNNING");
    timerSessionRepository.updateById(session);

    log.info("继续计时: sessionId={}", session.getId());

    Matter matter = matterRepository.getByIdOrThrow(session.getMatterId(), "案件不存在");
    return toDTO(session, matter);
  }

  /** 停止计时并保存工时记录. */
  @Transactional
  public void stopTimer() {
    Long userId = SecurityUtils.getUserId();
    TimerSession session = timerSessionRepository.findRunningByUserId(userId);

    if (session == null) {
      // 尝试查找已暂停的计时器
      session = timerSessionRepository.findPausedByUserId(userId);
      if (session == null) {
        throw new BusinessException("没有正在运行的计时器");
      }
    }

    LocalDateTime now = LocalDateTime.now();
    long finalElapsed = session.getElapsedSeconds();

    // 如果正在运行，需要加上当前运行的时间
    if ("RUNNING".equals(session.getStatus())) {
      long currentElapsed;
      if (session.getResumeTime() != null) {
        currentElapsed = Duration.between(session.getResumeTime(), now).getSeconds();
      } else {
        currentElapsed = Duration.between(session.getStartTime(), now).getSeconds();
      }
      finalElapsed += currentElapsed;
    }

    // 转换为小时（保留2位小数）
    BigDecimal hours =
        BigDecimal.valueOf(finalElapsed)
            .divide(BigDecimal.valueOf(SECONDS_PER_HOUR), HOURS_SCALE, RoundingMode.HALF_UP);

    // 创建工时记录
    CreateTimesheetCommand command = new CreateTimesheetCommand();
    command.setMatterId(session.getMatterId());
    command.setWorkDate(now.toLocalDate());
    command.setHours(hours);
    command.setWorkType(session.getWorkType());
    command.setWorkContent(session.getWorkContent());
    command.setBillable(session.getBillable());

    timesheetAppService.createTimesheet(command);

    // 标记计时器会话为已停止
    session.setStatus("STOPPED");
    timerSessionRepository.updateById(session);

    log.info("停止计时并保存工时记录: sessionId={}, hours={}", session.getId(), hours);
  }

  /**
   * 获取当前计时器状态.
   *
   * @return 计时器会话DTO
   */
  public TimerSessionDTO getTimerStatus() {
    Long userId = SecurityUtils.getUserId();

    // 优先查找正在运行的计时器
    TimerSession session = timerSessionRepository.findRunningByUserId(userId);
    if (session == null) {
      // 查找已暂停的计时器
      session = timerSessionRepository.findPausedByUserId(userId);
    }

    if (session == null) {
      return null; // 没有活动的计时器
    }

    Matter matter = matterRepository.getByIdOrThrow(session.getMatterId(), "案件不存在");
    return toDTO(session, matter);
  }

  /**
   * 转换为DTO.
   *
   * @param session 计时器会话
   * @param matter 案件
   * @return DTO
   */
  private TimerSessionDTO toDTO(final TimerSession session, final Matter matter) {
    TimerSessionDTO dto = new TimerSessionDTO();
    dto.setId(session.getId());
    dto.setMatterId(session.getMatterId());
    dto.setMatterName(matter.getName());
    dto.setWorkType(session.getWorkType());
    dto.setWorkContent(session.getWorkContent());
    dto.setBillable(session.getBillable());
    dto.setStartTime(session.getStartTime());
    dto.setPauseTime(session.getPauseTime());
    dto.setResumeTime(session.getResumeTime());
    dto.setElapsedSeconds(session.getElapsedSeconds());
    dto.setStatus(session.getStatus());

    // 计算总秒数
    long totalSeconds = session.getElapsedSeconds();
    if ("RUNNING".equals(session.getStatus())) {
      LocalDateTime referenceTime =
          session.getResumeTime() != null ? session.getResumeTime() : session.getStartTime();
      totalSeconds += Duration.between(referenceTime, LocalDateTime.now()).getSeconds();
    }
    dto.setTotalSeconds(totalSeconds);

    // 格式化时间显示
    dto.setFormattedTime(formatTime(totalSeconds));

    return dto;
  }

  /**
   * 格式化时间（秒数转为 HH:mm:ss）.
   *
   * @param seconds 秒数
   * @return 格式化后的时间字符串
   */
  private String formatTime(final long seconds) {
    long hours = seconds / SECONDS_PER_HOUR;
    long minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
    long secs = seconds % SECONDS_PER_MINUTE;
    return String.format("%02d:%02d:%02d", hours, minutes, secs);
  }
}
