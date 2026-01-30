package com.lawfirm.interfaces.scheduler;

import com.lawfirm.infrastructure.external.holiday.HolidayService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 节假日数据同步定时任务
 *
 * <p>功能： 1. 应用启动时检查并同步当年和下一年的数据 2. 每年1月1日自动同步新年份的数据 3. 每月1日检查数据完整性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HolidaySyncScheduler {

  /** 节假日服务 */
  private final HolidayService holidayService;

  /** 应用启动时同步节假日数据 */
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    try {
      int currentYear = LocalDate.now().getYear();
      int nextYear = currentYear + 1;

      // 检查当年数据
      if (!holidayService.isYearSynced(currentYear)) {
        log.info("应用启动：同步{}年节假日数据", currentYear);
        holidayService.syncYearHolidays(currentYear);
      }

      // 检查下一年数据（下半年开始同步）
      if (LocalDate.now().getMonthValue() >= 6 && !holidayService.isYearSynced(nextYear)) {
        log.info("应用启动：同步{}年节假日数据", nextYear);
        holidayService.syncYearHolidays(nextYear);
      }

      log.info("节假日数据同步检查完成");
    } catch (Exception e) {
      log.error("应用启动时同步节假日数据失败", e);
    }
  }

  /** 每年1月1日凌晨1点同步节假日数据 同步当年和下一年的数据 */
  @Scheduled(cron = "0 0 1 1 1 ?")
  public void syncYearlyHolidays() {
    int currentYear = LocalDate.now().getYear();
    int nextYear = currentYear + 1;

    log.info("定时任务：开始同步节假日数据（{}年、{}年）", currentYear, nextYear);

    try {
      int count1 = holidayService.syncYearHolidays(currentYear);
      int count2 = holidayService.syncYearHolidays(nextYear);

      log.info("节假日数据同步完成: {}年{}条, {}年{}条", currentYear, count1, nextYear, count2);
    } catch (Exception e) {
      log.error("节假日数据同步失败", e);
    }
  }

  /** 每月1日凌晨2点检查并补充缺失数据 */
  @Scheduled(cron = "0 0 2 1 * ?")
  public void checkAndSyncMissingData() {
    int currentYear = LocalDate.now().getYear();

    log.debug("定时任务：检查节假日数据完整性");

    try {
      // 确保当年数据完整
      if (!holidayService.isYearSynced(currentYear)) {
        log.info("补充{}年节假日数据", currentYear);
        holidayService.syncYearHolidays(currentYear);
      }

      // 下半年开始同步下一年数据
      if (LocalDate.now().getMonthValue() >= 6) {
        int nextYear = currentYear + 1;
        if (!holidayService.isYearSynced(nextYear)) {
          log.info("补充{}年节假日数据", nextYear);
          holidayService.syncYearHolidays(nextYear);
        }
      }
    } catch (Exception e) {
      log.error("补充节假日数据失败", e);
    }
  }
}
