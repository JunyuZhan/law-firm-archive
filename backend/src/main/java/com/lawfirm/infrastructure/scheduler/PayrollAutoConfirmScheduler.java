package com.lawfirm.infrastructure.scheduler;

import com.lawfirm.domain.hr.entity.PayrollItem;
import com.lawfirm.domain.hr.entity.PayrollSheet;
import com.lawfirm.domain.hr.repository.PayrollItemRepository;
import com.lawfirm.domain.hr.repository.PayrollSheetRepository;
import com.lawfirm.infrastructure.lock.DistributedLockService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

  /** 分布式锁服务. */
  private final DistributedLockService distributedLockService;

  /** 锁过期时间（秒）：10分钟 */
  private static final long LOCK_EXPIRE_SECONDS = 600;

  /** 每天凌晨2点执行自动确认任务. */
  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public void autoConfirmPayrollItems() {
    if (!distributedLockService.trySchedulerLock(
        "PayrollAutoConfirmScheduler", LOCK_EXPIRE_SECONDS)) {
      return;
    }
    try {
      doAutoConfirmPayrollItems();
    } finally {
      distributedLockService.unlockScheduler("PayrollAutoConfirmScheduler");
    }
  }

  /** 执行自动确认任务. */
  private void doAutoConfirmPayrollItems() {
    try {
      log.info("开始执行工资表自动确认任务...");

      LocalDateTime now = LocalDateTime.now();

      // 查询所有待确认状态的工资表
      List<PayrollSheet> sheets =
          payrollSheetRepository
              .lambdaQuery()
              .eq(PayrollSheet::getStatus, "PENDING_CONFIRM")
              .eq(PayrollSheet::getDeleted, false)
              .list();

      if (sheets.isEmpty()) {
        log.info("没有待确认的工资表");
        return;
      }

      // 批量获取所有工资表的ID
      List<Long> sheetIds = sheets.stream().map(PayrollSheet::getId).collect(Collectors.toList());

      // 一次性查询所有待确认的工资明细（避免N+1查询）
      List<PayrollItem> allPendingItems =
          payrollItemRepository
              .lambdaQuery()
              .in(PayrollItem::getPayrollSheetId, sheetIds)
              .eq(PayrollItem::getConfirmStatus, "PENDING")
              .eq(PayrollItem::getDeleted, false)
              .list();

      // 按工资表ID分组
      Map<Long, List<PayrollItem>> itemsBySheetId =
          allPendingItems.stream().collect(Collectors.groupingBy(PayrollItem::getPayrollSheetId));

      // 收集需要更新的工资明细
      List<PayrollItem> itemsToUpdate = new ArrayList<>();
      int totalAutoConfirmed = 0;

      for (PayrollSheet sheet : sheets) {
        List<PayrollItem> pendingItems = itemsBySheetId.getOrDefault(sheet.getId(), List.of());
        if (pendingItems.isEmpty()) {
          continue;
        }

        // 自动确认超过确认截止时间的工资明细
        for (PayrollItem item : pendingItems) {
          LocalDateTime confirmDeadline = item.getConfirmDeadline();
          if (confirmDeadline == null) {
            confirmDeadline = sheet.getAutoConfirmDeadline();
          }

          if (confirmDeadline != null && now.isAfter(confirmDeadline)) {
            item.setConfirmStatus("CONFIRMED");
            item.setConfirmedAt(now);
            item.setConfirmComment("系统自动确认（超过截止时间未确认）");
            itemsToUpdate.add(item);
            totalAutoConfirmed++;
          }
        }
      }

      // 批量更新工资明细
      if (!itemsToUpdate.isEmpty()) {
        payrollItemRepository.updateBatchById(itemsToUpdate);
      }

      // 一次性查询所有工资明细用于统计（避免N+1查询）
      List<PayrollItem> allItems =
          payrollItemRepository
              .lambdaQuery()
              .in(PayrollItem::getPayrollSheetId, sheetIds)
              .eq(PayrollItem::getDeleted, false)
              .list();

      Map<Long, List<PayrollItem>> allItemsBySheetId =
          allItems.stream().collect(Collectors.groupingBy(PayrollItem::getPayrollSheetId));

      // 更新工资表统计
      List<PayrollSheet> sheetsToUpdate = new ArrayList<>();
      for (PayrollSheet sheet : sheets) {
        List<PayrollItem> sheetItems = allItemsBySheetId.getOrDefault(sheet.getId(), List.of());
        long confirmedCount =
            sheetItems.stream().filter(i -> "CONFIRMED".equals(i.getConfirmStatus())).count();

        sheet.setConfirmedCount((int) confirmedCount);

        if (confirmedCount == sheetItems.size() && !sheetItems.isEmpty()) {
          sheet.setStatus("CONFIRMED");
        }
        sheetsToUpdate.add(sheet);
      }

      // 批量更新工资表
      if (!sheetsToUpdate.isEmpty()) {
        payrollSheetRepository.updateBatchById(sheetsToUpdate);
      }

      log.info("工资表自动确认任务完成，共自动确认 {} 条工资明细", totalAutoConfirmed);
    } catch (Exception e) {
      log.error("工资表自动确认任务执行失败", e);
      // 不重新抛出异常，避免影响调度器后续任务执行
    }
  }
}
