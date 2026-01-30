package com.lawfirm.interfaces.scheduler;

import com.lawfirm.application.workbench.service.ScheduledReportAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时报表调度器 每分钟检查一次待执行的定时报表任务 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledReportScheduler {

  /** 定时报表应用服务 */
  private final ScheduledReportAppService scheduledReportAppService;

  /** 每分钟执行一次，检查并执行待处理的定时报表任务 */
  @Scheduled(cron = "0 * * * * ?")
  public void executePendingTasks() {
    log.debug("开始检查待执行的定时报表任务...");
    try {
      scheduledReportAppService.executePendingTasks();
    } catch (Exception e) {
      log.error("执行定时报表任务调度失败", e);
    }
  }
}
