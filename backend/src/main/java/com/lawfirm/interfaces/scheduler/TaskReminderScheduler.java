package com.lawfirm.interfaces.scheduler;

import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.infrastructure.persistence.mapper.TaskMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务到期提醒定时调度器
 *
 * <p>功能： 1. 每天上午9点检查即将到期的任务（3天内），发送提醒通知 2. 每天上午10点检查已逾期的任务，发送逾期警告 3. 检查设置了提醒时间的任务，在指定时间发送提醒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskReminderScheduler {

  /** 任务仓储 */
  private final TaskRepository taskRepository;

  /** 任务Mapper */
  private final TaskMapper taskMapper;

  /** 通知应用服务 */
  private final NotificationAppService notificationAppService;

  /** 即将到期任务提醒 每天上午9点执行，提醒3天内到期的任务 */
  @Scheduled(cron = "0 0 9 * * ?")
  @Transactional
  public void sendUpcomingTaskReminders() {
    log.info("开始执行任务到期提醒定时任务");

    LocalDate today = LocalDate.now();
    LocalDate deadline = today.plusDays(3);

    List<Task> upcomingTasks = taskRepository.findUpcomingTasks(today, deadline);
    int sentCount = 0;

    for (Task task : upcomingTasks) {
      try {
        // 跳过没有负责人的任务
        if (task.getAssigneeId() == null) {
          continue;
        }

        // 跳过今天已经发送过提醒的任务（通过reminderSent标记）
        if (Boolean.TRUE.equals(task.getReminderSent())
            && task.getDueDate() != null
            && task.getDueDate().isAfter(today)) {
          continue;
        }

        long daysRemaining = ChronoUnit.DAYS.between(today, task.getDueDate());
        String urgency = daysRemaining <= 1 ? "【紧急】" : "";

        String message =
            String.format(
                "%s任务【%s】将于%d天后到期（%s），请及时处理！",
                urgency, task.getTitle(), daysRemaining, task.getDueDate());

        notificationAppService.sendSystemNotification(
            task.getAssigneeId(), "任务到期提醒", message, "TASK", task.getId());

        // 标记已发送提醒
        task.setReminderSent(true);
        taskRepository.updateById(task);

        sentCount++;
        log.debug(
            "发送任务到期提醒: taskId={}, assigneeId={}, daysRemaining={}",
            task.getId(),
            task.getAssigneeId(),
            daysRemaining);

      } catch (Exception e) {
        log.error("发送任务到期提醒失败: taskId={}", task.getId(), e);
      }
    }

    log.info("任务到期提醒定时任务执行完成，共发送{}条提醒", sentCount);
  }

  /** 逾期任务警告 每天上午10点执行，提醒已逾期的任务 */
  @Scheduled(cron = "0 0 10 * * ?")
  @Transactional
  public void sendOverdueTaskWarnings() {
    log.info("开始执行逾期任务警告定时任务");

    List<Task> overdueTasks = taskRepository.findOverdueTasks(LocalDate.now());
    int sentCount = 0;

    for (Task task : overdueTasks) {
      try {
        if (task.getAssigneeId() == null) {
          continue;
        }

        long daysOverdue = ChronoUnit.DAYS.between(task.getDueDate(), LocalDate.now());

        String message =
            String.format(
                "【逾期警告】任务【%s】已逾期%d天（截止日期：%s），请尽快处理！",
                task.getTitle(), daysOverdue, task.getDueDate());

        notificationAppService.sendSystemNotification(
            task.getAssigneeId(), "任务逾期警告", message, "TASK", task.getId());

        sentCount++;
        log.debug(
            "发送逾期任务警告: taskId={}, assigneeId={}, daysOverdue={}",
            task.getId(),
            task.getAssigneeId(),
            daysOverdue);

      } catch (Exception e) {
        log.error("发送逾期任务警告失败: taskId={}", task.getId(), e);
      }
    }

    log.info("逾期任务警告定时任务执行完成，共发送{}条警告", sentCount);
  }

  /** 自定义提醒时间检查 每小时执行一次，检查设置了reminderDate的任务 */
  @Scheduled(cron = "0 0 * * * ?")
  @Transactional
  public void sendScheduledReminders() {
    log.debug("开始检查自定义提醒任务");

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime oneHourAgo = now.minusHours(1);

    List<Task> tasksNeedReminder = taskMapper.selectTasksNeedReminder(oneHourAgo, now);
    int sentCount = 0;

    for (Task task : tasksNeedReminder) {
      try {
        if (task.getAssigneeId() == null) {
          continue;
        }

        String message =
            String.format(
                "任务提醒：【%s】%s",
                task.getTitle(), task.getDueDate() != null ? "，截止日期：" + task.getDueDate() : "");

        notificationAppService.sendSystemNotification(
            task.getAssigneeId(), "任务提醒", message, "TASK", task.getId());

        // 标记已发送提醒
        task.setReminderSent(true);
        taskRepository.updateById(task);

        sentCount++;
        log.debug("发送自定义任务提醒: taskId={}, assigneeId={}", task.getId(), task.getAssigneeId());

      } catch (Exception e) {
        log.error("发送自定义任务提醒失败: taskId={}", task.getId(), e);
      }
    }

    if (sentCount > 0) {
      log.info("自定义任务提醒执行完成，共发送{}条提醒", sentCount);
    }
  }
}
