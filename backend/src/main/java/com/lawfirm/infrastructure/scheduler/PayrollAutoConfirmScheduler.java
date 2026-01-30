package com.lawfirm.infrastructure.scheduler;

import com.lawfirm.domain.hr.entity.PayrollItem;
import com.lawfirm.domain.hr.entity.PayrollSheet;
import com.lawfirm.domain.hr.repository.PayrollItemRepository;
import com.lawfirm.domain.hr.repository.PayrollSheetRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工资表自动确认定时任务.
 *
 * <p>检查超过自动确认截止时间未确认的工资明细，自动确认
 *
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollAutoConfirmScheduler {

  /** 工资表仓储. */
  private final PayrollSheetRepository payrollSheetRepository;

  /** 工资明细仓储. */
  private final PayrollItemRepository payrollItemRepository;

  /** 每天凌晨2点执行自动确认任务. */
  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public void autoConfirmPayrollItems() {
    log.info("开始执行工资表自动确认任务...");

    LocalDateTime now = LocalDateTime.now();

    // 查询所有待确认状态的工资表
    List<PayrollSheet> sheets =
        payrollSheetRepository
            .lambdaQuery()
            .eq(PayrollSheet::getStatus, "PENDING_CONFIRM")
            .eq(PayrollSheet::getDeleted, false)
            .list();

    int totalAutoConfirmed = 0;

    for (PayrollSheet sheet : sheets) {
      try {
        // 查询该工资表中所有待确认的工资明细
        List<PayrollItem> pendingItems =
            payrollItemRepository
                .lambdaQuery()
                .eq(PayrollItem::getPayrollSheetId, sheet.getId())
                .eq(PayrollItem::getConfirmStatus, "PENDING")
                .eq(PayrollItem::getDeleted, false)
                .list();

        if (pendingItems.isEmpty()) {
          continue;
        }

        // 自动确认超过确认截止时间的工资明细
        // 每个员工有独立的确认截止时间，如果没有设置则使用工资表的autoConfirmDeadline
        for (PayrollItem item : pendingItems) {
          LocalDateTime confirmDeadline = item.getConfirmDeadline();
          if (confirmDeadline == null) {
            // 如果没有设置员工单独的确认截止时间，使用工资表的默认截止时间
            confirmDeadline = sheet.getAutoConfirmDeadline();
          }

          // 如果确认截止时间已过，自动确认
          if (confirmDeadline != null && now.isAfter(confirmDeadline)) {
            item.setConfirmStatus("CONFIRMED");
            item.setConfirmedAt(now);
            item.setConfirmComment("系统自动确认（超过截止时间未确认）");
            payrollItemRepository.updateById(item);
            totalAutoConfirmed++;
          }
        }

        // 重新计算已确认人数
        List<PayrollItem> allItems = payrollItemRepository.findByPayrollSheetId(sheet.getId());
        long confirmedCount =
            allItems.stream().filter(i -> "CONFIRMED".equals(i.getConfirmStatus())).count();

        sheet.setConfirmedCount((int) confirmedCount);

        // 如果所有员工都已确认，自动更新工资表状态为已确认
        if (confirmedCount == allItems.size() && allItems.size() > 0) {
          sheet.setStatus("CONFIRMED");
        }

        payrollSheetRepository.updateById(sheet);

        log.info(
            "自动确认工资表: payrollNo={}, autoConfirmedCount={}",
            sheet.getPayrollNo(),
            pendingItems.size());

      } catch (Exception e) {
        log.error("自动确认工资表失败: payrollNo={}", sheet.getPayrollNo(), e);
      }
    }

    log.info("工资表自动确认任务完成，共自动确认 {} 条工资明细", totalAutoConfirmed);
  }
}
