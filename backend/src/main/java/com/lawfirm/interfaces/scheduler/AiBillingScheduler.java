package com.lawfirm.interfaces.scheduler;

import com.lawfirm.application.ai.service.AiBillingAppService;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.notification.AlertService;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** AI账单定时任务 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiBillingScheduler {

  /** AI账单应用服务 */
  private final AiBillingAppService billingAppService;

  /** 告警服务 */
  private final AlertService alertService;

  /** 用户Mapper */
  private final UserMapper userMapper;

  /** 通知应用服务 */
  private final NotificationAppService notificationAppService;

  /** 自动生成上月账单 每月1日凌晨2点执行 */
  @Scheduled(cron = "0 0 2 1 * ?")
  public void generateLastMonthBills() {
    log.info("开始执行AI账单自动生成任务");

    try {
      YearMonth lastMonth = YearMonth.now().minusMonths(1);
      int year = lastMonth.getYear();
      int month = lastMonth.getMonthValue();

      log.info("生成{}年{}月AI费用账单", year, month);
      int count = billingAppService.generateMonthlyBills(year, month);
      log.info("AI账单自动生成任务完成，共生成{}份账单", count);

    } catch (Exception e) {
      log.error("AI账单自动生成任务执行失败", e);
      // 发送告警通知给管理员
      try {
        String stackTrace = getStackTrace(e);
        alertService.sendSystemErrorAlert(
            "AI账单自动生成任务失败", e.getMessage() != null ? e.getMessage() : "未知错误", stackTrace);
      } catch (Exception alertException) {
        log.error("发送告警通知失败", alertException);
      }
    }
  }

  /** 账单提醒（可选） 每月5日上午10点执行，提醒财务处理待扣减账单 */
  @Scheduled(cron = "0 0 10 5 * ?")
  public void remindPendingBills() {
    log.info("执行AI账单提醒任务");

    try {
      // 查询待扣减账单数量
      int pendingCount = billingAppService.countPendingBills();
      log.info("当前待扣减账单数量: {}", pendingCount);

      if (pendingCount == 0) {
        log.info("无待扣减账单，跳过提醒");
        return;
      }

      // 查询财务人员
      List<User> financeUsers = userMapper.selectFinanceUsers();
      if (financeUsers == null || financeUsers.isEmpty()) {
        log.warn("未找到财务人员，无法发送提醒");
        return;
      }

      // 发送提醒给财务人员
      List<Long> financeUserIds =
          financeUsers.stream().map(User::getId).collect(Collectors.toList());

      String title = "AI账单处理提醒";
      String content = String.format("您有 %d 份AI费用账单待处理，请及时前往【AI费用账单】页面进行处理。", pendingCount);

      notificationAppService.sendUrgentNotification(
          financeUserIds, title, content, "AI_BILLING", null);

      log.info("AI账单提醒任务完成，已通知{}位财务人员，待处理账单数: {}", financeUserIds.size(), pendingCount);

    } catch (Exception e) {
      log.error("AI账单提醒任务执行失败", e);
      // 发送告警通知给管理员
      try {
        String stackTrace = getStackTrace(e);
        alertService.sendSystemErrorAlert(
            "AI账单提醒任务失败", e.getMessage() != null ? e.getMessage() : "未知错误", stackTrace);
      } catch (Exception alertException) {
        log.error("发送告警通知失败", alertException);
      }
    }
  }

  /**
   * 获取异常堆栈信息
   *
   * @param e 异常对象
   * @return 异常堆栈信息字符串
   */
  private String getStackTrace(final Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }
}
