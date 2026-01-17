package com.lawfirm.interfaces.scheduler;

import com.lawfirm.application.ai.service.AiBillingAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * AI账单定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiBillingScheduler {

    private final AiBillingAppService billingAppService;

    /**
     * 自动生成上月账单
     * 每月1日凌晨2点执行
     */
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
            // TODO: 发送告警通知
        }
    }

    /**
     * 账单提醒（可选）
     * 每月5日上午10点执行，提醒财务处理待扣减账单
     */
    @Scheduled(cron = "0 0 10 5 * ?")
    public void remindPendingBills() {
        log.info("执行AI账单提醒任务");

        try {
            // TODO: 查询待扣减账单数量
            // TODO: 发送提醒给财务人员

            log.info("AI账单提醒任务完成");

        } catch (Exception e) {
            log.error("AI账单提醒任务执行失败", e);
        }
    }
}
