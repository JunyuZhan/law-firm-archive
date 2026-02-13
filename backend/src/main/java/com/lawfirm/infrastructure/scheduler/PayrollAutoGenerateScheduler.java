package com.lawfirm.infrastructure.scheduler;

import com.lawfirm.application.hr.command.CreatePayrollSheetCommand;
import com.lawfirm.application.hr.service.PayrollAppService;
import com.lawfirm.domain.hr.repository.PayrollSheetRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工资表自动生成定时任务.
 *
 * <p>每月1日0时自动生成当月工资表
 *
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollAutoGenerateScheduler {

  /** 工资应用服务. */
  private final PayrollAppService payrollAppService;

  /** 工资表仓储. */
  private final PayrollSheetRepository payrollSheetRepository;

  /**
   * 每月1日0时执行自动生成工资表任务.
   *
   * <p>cron表达式：0 0 0 1 * ? （每月1日0时0分0秒） 已禁用：不再自动生成工资表，员工自动显示在工资管理列表中
   */
  // @Scheduled(cron = "0 0 0 1 * ?")
  @Transactional
  public void autoGeneratePayrollSheet() {
    log.info("开始执行工资表自动生成任务...");

    LocalDate now = LocalDate.now();
    int year = now.getYear();
    int month = now.getMonthValue();

    try {
      // 检查是否已存在该年月的工资表
      boolean exists = payrollSheetRepository.findByYearAndMonth(year, month).isPresent();

      if (exists) {
        log.info("{}年{}月的工资表已存在，跳过自动生成", year, month);
        return;
      }

      // 创建工资表命令
      CreatePayrollSheetCommand command = new CreatePayrollSheetCommand();
      command.setPayrollYear(year);
      command.setPayrollMonth(month);
      // 设置自动确认截止时间为下月1日0时
      command.setAutoConfirmDeadline(LocalDateTime.of(year, month, 1, 0, 0).plusMonths(1));

      // 创建工资表（会自动为所有在职员工生成工资明细）
      payrollAppService.createPayrollSheet(command);

      log.info("自动生成工资表成功: {}-{}", year, month);

    } catch (Exception e) {
      log.error("自动生成工资表失败: {}-{}", year, month, e);
    }
  }
}
