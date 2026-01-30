package com.lawfirm.interfaces.scheduler;

import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.domain.matter.entity.Schedule;
import com.lawfirm.infrastructure.persistence.mapper.ScheduleMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 日程提醒定时调度器
 *
 * <p>功能： 1. 每5分钟检查需要提醒的日程，根据用户设置的提前提醒时间发送通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleReminderScheduler {

  /** 日程Mapper */
  private final ScheduleMapper scheduleMapper;

  /** 通知应用服务 */
  private final NotificationAppService notificationAppService;

  /** 日程提醒 每5分钟执行一次，检查需要提醒的日程 */
  @Scheduled(cron = "0 */5 * * * ?")
  @Transactional
  public void sendScheduleReminders() {
    log.debug("开始执行日程提醒定时任务");

    List<Schedule> schedules = scheduleMapper.selectNeedReminder();
    int sentCount = 0;

    for (Schedule schedule : schedules) {
      try {
        if (schedule.getUserId() == null) {
          continue;
        }

        String typeLabel = getScheduleTypeLabel(schedule.getScheduleType());
        String location = schedule.getLocation() != null ? "，地点：" + schedule.getLocation() : "";

        String message =
            String.format("【日程提醒】您有一个%s【%s】即将开始%s", typeLabel, schedule.getTitle(), location);

        notificationAppService.sendSystemNotification(
            schedule.getUserId(), "日程提醒", message, "SCHEDULE", schedule.getId());

        // 标记已发送提醒
        schedule.setReminderSent(true);
        scheduleMapper.updateById(schedule);

        sentCount++;
        log.debug(
            "发送日程提醒: scheduleId={}, userId={}, title={}",
            schedule.getId(),
            schedule.getUserId(),
            schedule.getTitle());

      } catch (Exception e) {
        log.error("发送日程提醒失败: scheduleId={}", schedule.getId(), e);
      }
    }

    if (sentCount > 0) {
      log.info("日程提醒定时任务执行完成，共发送{}条提醒", sentCount);
    }
  }

  private String getScheduleTypeLabel(final String type) {
    if (type == null) {
      return "日程";
    }
    return switch (type) {
      case "COURT" -> "开庭";
      case "MEETING" -> "会议";
      case "DEADLINE" -> "期限";
      case "APPOINTMENT" -> "约见";
      case "OTHER" -> "日程";
      default -> "日程";
    };
  }
}
